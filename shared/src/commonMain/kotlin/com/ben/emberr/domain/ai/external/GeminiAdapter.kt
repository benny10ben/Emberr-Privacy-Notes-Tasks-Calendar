package com.ben.emberr.domain.ai.external

import com.ben.emberr.domain.ai.chat.ChatTurn
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiGenerationConfig(
    val maxOutputTokens: Int
)

@Serializable
private data class GeminiRequest(
    @SerialName("system_instruction") val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
private data class GeminiStreamChunk(
    val candidates: List<GeminiCandidate> = emptyList()
)

class GeminiAdapter : ChatCompletionAdapter {

    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChatCompletion(
        httpClient: HttpClient,
        config: ExternalAiProviderConfig,
        providerDisplayName: String,
        systemPrompt: String,
        userQuestion: String,
        contextBlock: String,
        conversationHistory: List<ChatTurn>,
        maxOutputTokens: Int
    ): Flow<String> = flow {
        val contents = buildList {
            conversationHistory.forEach { turn ->
                add(GeminiContent(role = "user", parts = listOf(GeminiPart(turn.userMessage))))
                add(GeminiContent(role = "model", parts = listOf(GeminiPart(turn.assistantMessage))))
            }
            add(GeminiContent(role = "user", parts = listOf(GeminiPart(userQuestion))))
        }

        val requestBody = json.encodeToString(
            GeminiRequest.serializer(),
            GeminiRequest(
                systemInstruction = GeminiContent(parts = listOf(GeminiPart("$systemPrompt\n\n$contextBlock"))),
                contents = contents,
                generationConfig = GeminiGenerationConfig(maxOutputTokens = maxOutputTokens)
            )
        )

        httpClient.preparePost("$API_BASE_URL/${config.model}:streamGenerateContent") {
            parameter("alt", "sse")
            parameter("key", config.apiKey)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.execute { response ->
            if (response.status.value !in 200..299) {
                throwForFailedResponse(response, providerDisplayName)
            }

            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data:")) continue

                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty()) continue

                val chunk = try {
                    json.decodeFromString(GeminiStreamChunk.serializer(), payload)
                } catch (cause: SerializationException) {
                    continue
                }

                val text = chunk.candidates.firstOrNull()?.content?.parts?.joinToString("") { it.text }
                if (!text.isNullOrEmpty()) emit(text)
            }
        }
    }

    private companion object {
        const val API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}
