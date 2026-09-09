package com.ben.emberr.domain.ai

import com.ben.emberr.domain.ai.chat.ChatTurn
import kotlinx.coroutines.flow.Flow

interface AiGenerationEngine {
    suspend fun isModelAvailable(): Boolean
    fun generateResponseStream(
        systemPrompt: String,
        userQuestion: String,
        contextBlock: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String>
}
