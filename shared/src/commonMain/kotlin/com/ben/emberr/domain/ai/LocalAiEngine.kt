package com.ben.emberr.domain.ai

import com.ben.emberr.domain.ai.chat.ChatTurn
import com.ben.emberr.domain.ai.external.AiSettingsRepository
import com.ben.emberr.domain.ai.models.ModelFileNames
import com.ben.emberr.domain.ai.models.modelFileExists
import com.ben.emberr.domain.ai.models.resolveModelPath
import com.llamatik.library.platform.LlamaBridge
import com.llamatik.library.platform.GenStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalAiEngine(
    private val aiSettingsRepository: AiSettingsRepository
) : AiGenerationEngine {

    private val embedderFileName = ModelFileNames.EMBEDDER

    val unsupportedHardwareReason: String? =
        (detectLocalAiSupport() as? LocalAiSupport.Unsupported)?.reason

    private val nativeMutex = Mutex()
    private var loadedModel: LoadedModel = LoadedModel.NONE
    private var loadedGeneratorFileName: String? = null

    private enum class LoadedModel { NONE, EMBEDDER, GENERATOR }

    // Embedding
    suspend fun generateEmbeddings(texts: List<String>): List<List<Float>> =
        nativeMutex.withLock {
            withContext(Dispatchers.Default) {
                if (texts.isEmpty()) return@withContext emptyList()

                ensureEmbedderLoaded()
                texts.map { LlamaBridge.embed(it).toList() }
            }
        }

    suspend fun generateEmbedding(text: String): List<Float> =
        generateEmbeddings(listOf(text)).firstOrNull() ?: emptyList()

    // Generation (streaming)
    override fun generateResponseStream(
        systemPrompt: String,
        userQuestion: String,
        contextBlock: String,
        conversationHistory: List<ChatTurn>
    ): Flow<String> = callbackFlow {
        failWhenHardwareCannotRunLocalAi()

        launch(Dispatchers.IO) {
            nativeMutex.withLock {
                ensureGeneratorLoaded()

                val historyMessages = conversationHistory.flatMap { turn ->
                    listOf("user" to turn.userMessage, "assistant" to turn.assistantMessage)
                }

                val formattedPrompt = LlamaBridge.applyChatTemplate(
                    messages = listOf("system" to "$systemPrompt\n\n$contextBlock") + historyMessages + listOf("user" to userQuestion),
                    addAssistantPrefix = true
                ) ?: "$systemPrompt\n\n$contextBlock\n\nUser: $userQuestion\n\nAssistant:"

                LlamaBridge.generateStream(
                    prompt = formattedPrompt,
                    callback = object : GenStream {
                        override fun onDelta(text: String) { trySend(text) }
                        override fun onComplete()           { close() }
                        override fun onError(message: String) { close(Exception(message)) }
                    }
                )
            }
        }

        awaitClose { LlamaBridge.nativeCancelGenerate() }
    }.flowOn(Dispatchers.Default)

    suspend fun shutdown() = nativeMutex.withLock {
        if (loadedModel != LoadedModel.NONE) {
            LlamaBridge.shutdown()
            loadedModel = LoadedModel.NONE
            loadedGeneratorFileName = null
        }
    }

    // Private helpers
    private fun failWhenHardwareCannotRunLocalAi() {
        val reason = unsupportedHardwareReason ?: return
        throw LocalAiUnsupportedException(reason)
    }

    private fun ensureEmbedderLoaded() {
        failWhenHardwareCannotRunLocalAi()
        if (loadedModel == LoadedModel.EMBEDDER) return

        val embedderPath = resolveModelPath(embedderFileName)
        if (!modelFileExists(embedderPath)) {
            throw IllegalStateException("Embedding model not found at $embedderPath")
        }

        if (loadedModel == LoadedModel.GENERATOR) {
            LlamaBridge.shutdown()
            loadedModel = LoadedModel.NONE
            loadedGeneratorFileName = null
        }

        val loaded = LlamaBridge.initEmbedModel(embedderPath)
        if (!loaded) throw IllegalStateException(
            "Failed to load native embedding model. Check memory constraints or model corruption."
        )
        loadedModel = LoadedModel.EMBEDDER
    }

    private suspend fun ensureGeneratorLoaded() {
        failWhenHardwareCannotRunLocalAi()
        val generatorFileName = aiSettingsRepository.getSelectedLocalModelFileName()
        if (generatorFileName.isBlank()) {
            throw IllegalStateException("No local model is installed or selected.")
        }

        if (loadedModel == LoadedModel.GENERATOR && loadedGeneratorFileName == generatorFileName) return

        val generatorPath = resolveModelPath(generatorFileName)
        if (!modelFileExists(generatorPath)) {
            throw IllegalStateException("Generator model not found at $generatorPath")
        }

        // If any model is already loaded (embedder, or a different generator file), free it first
        if (loadedModel != LoadedModel.NONE) {
            LlamaBridge.shutdown()
            loadedModel = LoadedModel.NONE
            loadedGeneratorFileName = null
        }

        val configuredContextLength = aiSettingsRepository.localContextLength.first()
        val configuredMaxOutputTokens = aiSettingsRepository.maxOutputTokens.first()
        val safeMaxOutputTokens = configuredMaxOutputTokens.coerceIn(
            MIN_OUTPUT_TOKENS,
            configuredContextLength / 2
        )

        LlamaBridge.updateGenerateParams(
            temperature    = 0.3f,
            maxTokens      = safeMaxOutputTokens,
            topP           = 0.95f,
            topK           = 40,
            repeatPenalty  = 1.1f,
            contextLength  = configuredContextLength,
            numThreads     = 6,
            useMmap        = true,
            flashAttention = true,
            batchSize      = 512,
            gpuLayers      = 0
        )

        val loaded = LlamaBridge.initGenerateModel(generatorPath)
        if (!loaded) throw IllegalStateException(
            "Failed to load native generator model. The file may not be a valid or supported GGUF model."
        )
        loadedModel = LoadedModel.GENERATOR
        loadedGeneratorFileName = generatorFileName
    }

    suspend fun checkGeneratorFinetuneType(): String? = nativeMutex.withLock {
        withContext(Dispatchers.Default) {
            ensureGeneratorLoaded()
            LlamaBridge.getModelFinetuneType()
        }
    }

    suspend fun warmUpGenerator() = nativeMutex.withLock {
        withContext(Dispatchers.Default) {
            ensureGeneratorLoaded()
        }
    }

    override suspend fun isModelAvailable(): Boolean =
        isEmbeddingModelAvailable() && isGeneratorModelAvailable()

    fun isEmbeddingModelAvailable(): Boolean {
        if (unsupportedHardwareReason != null) return false
        return try {
            modelFileExists(resolveModelPath(embedderFileName))
        } catch (e: Exception) {
            false
        }
    }

    fun isGeneratorModelAvailable(): Boolean {
        if (unsupportedHardwareReason != null) return false
        return try {
            val fileName = aiSettingsRepository.getSelectedLocalModelFileName()
            fileName.isNotBlank() && modelFileExists(resolveModelPath(fileName))
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        const val MIN_OUTPUT_TOKENS = 128
    }
}