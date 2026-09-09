package com.ben.emberr.domain.ai.external

import com.ben.emberr.domain.ai.chat.ChatTurn
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    @SerialName("max_tokens") val maxTokens: Int,
    val stream: Boolean = true
)

@Serializable
private data class ChatMessagePayload(
    val role: String,
    val content: String
)

@Serializable
private data class ChatCompletionChunk(
    val choices: List<ChunkChoice> = emptyList()
)

@Serializable
private data class ChunkChoice(
    val delta: ChunkDelta = ChunkDelta()
)

@Serializable
private data class ChunkDelta(
    val content: String? = null
)

class OpenAiCompatibleAdapter : ChatCompletionAdapter {

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
        val messages = buildList {
            add(ChatMessagePayload(role = "system", content = "$systemPrompt\n\n$contextBlock"))
            conversationHistory.forEach { turn ->
                add(ChatMessagePayload(role = "user", content = turn.userMessage))
                add(ChatMessagePayload(role = "assistant", content = turn.assistantMessage))
            }
            add(ChatMessagePayload(role = "user", content = userQuestion))
        }

        val requestBody = json.encodeToString(
            ChatCompletionRequest.serializer(),
            ChatCompletionRequest(model = config.model, messages = messages, maxTokens = maxOutputTokens)
        )

        httpClient.preparePost(resolveEndpoint(config)) {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
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
                if (payload == "[DONE]") break

                val chunk = try {
                    json.decodeFromString(ChatCompletionChunk.serializer(), payload)
                } catch (cause: SerializationException) {
                    continue
                }

                val delta = chunk.choices.firstOrNull()?.delta?.content
                if (!delta.isNullOrEmpty()) emit(delta)
            }
        }
    }

    private fun resolveEndpoint(config: ExternalAiProviderConfig): String {
        val base = config.baseUrl?.trimEnd('/') ?: DEFAULT_OPENAI_BASE_URL
        return "$base/chat/completions"
    }

    private companion object {
        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"
    }
}
