package com.ben.emberr.domain.ai

import com.ben.emberr.data.local.prefs.SettingsManager
import com.ben.emberr.data.local.prefs.SyncConstants
import com.ben.emberr.domain.ai.external.AiSettingsRepository
import com.ben.emberr.domain.ai.external.ExternalAiProvider
import com.ben.emberr.domain.ai.external.SecureAiKeyStorage
import com.ben.emberr.domain.ai.models.ModelDownloadScheduler
import com.ben.emberr.domain.ai.models.ModelFileNames
import com.ben.emberr.domain.ai.models.cleanupPendingModelDeletions
import com.ben.emberr.domain.ai.models.clearCachedExpectedModelSize
import com.ben.emberr.domain.ai.models.purgeModelFileCompletely
import com.emberr.database.EmberrDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiPurgeReport(
    val bytesFreed: Long,
    val undeletablePaths: List<String>,
    val survivingPaths: List<String>
) {
    val isFullySuccessful: Boolean get() = undeletablePaths.isEmpty() && survivingPaths.isEmpty()
}

class DisableAiFeaturesUseCase(
    private val database: EmberrDatabase,
    private val settingsManager: SettingsManager,
    private val aiSettingsRepository: AiSettingsRepository,
    private val secureAiKeyStorage: SecureAiKeyStorage,
    private val localAiEngine: LocalAiEngine,
    private val modelDownloadScheduler: ModelDownloadScheduler
) {
    suspend fun execute(): AiPurgeReport {
        settingsManager.saveAiFeaturesDisabled(true)

        runCatching { modelDownloadScheduler.cancelEmbeddingModelDownload() }
        runCatching { modelDownloadScheduler.cancelGeneratorModelDownload() }

        runCatching { localAiEngine.shutdown() }

        ExternalAiProvider.entries.forEach { provider ->
            runCatching { aiSettingsRepository.deleteProviderConfig(provider) }
        }
        runCatching { secureAiKeyStorage.clearAll() }

        return withContext(Dispatchers.IO) {
            val installedFileNames = runCatching { aiSettingsRepository.getInstalledLocalModels().map { it.fileName } }
                .getOrDefault(emptyList())

            var bytesFreed = 0L
            val undeletablePaths = mutableListOf<String>()
            val survivingPaths = mutableListOf<String>()

            (installedFileNames + ModelFileNames.GENERATOR + ModelFileNames.EMBEDDER)
                .distinct()
                .forEach { fileName ->
                    val outcome = runCatching { purgeModelFileCompletely(fileName) }.getOrNull()
                    if (outcome != null) {
                        bytesFreed += outcome.bytesFreed
                        undeletablePaths += outcome.undeletablePaths
                        survivingPaths += outcome.survivingPaths
                    } else {
                        undeletablePaths += fileName
                    }
                    runCatching { clearCachedExpectedModelSize(fileName) }
                }

            runCatching { cleanupPendingModelDeletions() }

            settingsManager.saveInstalledLocalModelsJson(SyncConstants.DEFAULT_INSTALLED_LOCAL_MODELS_JSON)
            settingsManager.saveSelectedLocalModelFileName("")
            settingsManager.saveAiGenerationMode(SyncConstants.DEFAULT_AI_GENERATION_MODE)

            database.vectorStoreQueries.deleteAllBlocks()

            AiPurgeReport(bytesFreed, undeletablePaths, survivingPaths)
        }
    }

    fun enableAiFeatures() {
        settingsManager.saveAiFeaturesDisabled(false)
    }
}
