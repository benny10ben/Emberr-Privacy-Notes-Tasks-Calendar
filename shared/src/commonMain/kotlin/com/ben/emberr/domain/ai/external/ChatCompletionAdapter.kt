package com.ben.emberr.domain.ai.external

import com.ben.emberr.domain.ai.chat.ChatTurn
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

interface ChatCompletionAdapter {
    fun streamChatCompletion(
        httpClient: HttpClient,
        config: ExternalAiProviderConfig,
        providerDisplayName: String,
        systemPrompt: String,
        userQuestion: String,
        contextBlock: String,
        conversationHistory: List<ChatTurn>,
        maxOutputTokens: Int
    ): Flow<String>
}
