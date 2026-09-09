package com.ben.emberr.domain.ai.external

import com.ben.emberr.domain.ai.chat.ChatTurn
import io.ktor.client.HttpClient
import io.ktor.client.request.header
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
private data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
private data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>,
    val stream: Boolean = true
)

@Serializable
private data class AnthropicStreamEvent(
    val type: String,
    val delta: AnthropicDelta? = null
)

@Serializable
private data class AnthropicDelta(
    val type: String? = null,
    val text: String? = null
)

class AnthropicAdapter : ChatCompletionAdapter {

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
            conversationHistory.forEach { turn ->
                add(AnthropicMessage(role = "user", content = turn.userMessage))
                add(AnthropicMessage(role = "assistant", content = turn.assistantMessage))
            }
            add(AnthropicMessage(role = "user", content = userQuestion))
        }

        val requestBody = json.encodeToString(
            AnthropicRequest.serializer(),
            AnthropicRequest(
                model = config.model,
                maxTokens = maxOutputTokens,
                system = "$systemPrompt\n\n$contextBlock",
                messages = messages
            )
        )

        httpClient.preparePost(API_ENDPOINT) {
            header("x-api-key", config.apiKey)
            header("anthropic-version", ANTHROPIC_VERSION)
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

                val event = try {
                    json.decodeFromString(AnthropicStreamEvent.serializer(), payload)
                } catch (cause: SerializationException) {
                    continue
                }

                if (event.type == "content_block_delta" && event.delta?.type == "text_delta") {
                    val text = event.delta.text
                    if (!text.isNullOrEmpty()) emit(text)
                } else if (event.type == "message_stop") {
                    break
                }
            }
        }
    }

    private companion object {
        const val API_ENDPOINT = "https://api.anthropic.com/v1/messages"
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
