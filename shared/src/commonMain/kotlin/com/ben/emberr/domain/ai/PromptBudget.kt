package com.ben.emberr.domain.ai

import com.ben.emberr.domain.ai.chat.ChatTurn
import kotlin.math.ceil

object AiContextWindows {
    const val EXTERNAL_TOKENS = 32_000
}

object TokenEstimator {
    private const val CHARACTERS_PER_TOKEN = 4.0

    fun estimateTokens(text: String): Int =
        ceil(text.length / CHARACTERS_PER_TOKEN).toInt()
}

data class PlannedPrompt(
    val history: List<ChatTurn>,
    val contextChunks: List<String>
)

object PromptBudgetPlanner {

    fun plan(
        totalContextTokens: Int,
        outputReservationTokens: Int,
        systemPrompt: String,
        userQuestion: String,
        candidateHistory: List<ChatTurn>,
        candidateChunks: List<String>
    ): PlannedPrompt {
        val safeOutputReservation = outputReservationTokens.coerceAtMost(totalContextTokens / 2)
        val fixedTokens = TokenEstimator.estimateTokens(systemPrompt) +
            TokenEstimator.estimateTokens(userQuestion) +
            safeOutputReservation

        var remainingTokens = (totalContextTokens - fixedTokens).coerceAtLeast(0)

        val selectedChunks = mutableListOf<String>()
        for (chunk in candidateChunks) {
            val chunkTokens = TokenEstimator.estimateTokens(chunk)
            if (chunkTokens > remainingTokens) continue
            selectedChunks.add(chunk)
            remainingTokens -= chunkTokens
        }

        val selectedHistoryNewestFirst = mutableListOf<ChatTurn>()
        for (turn in candidateHistory.asReversed()) {
            val turnTokens = TokenEstimator.estimateTokens(turn.userMessage) +
                TokenEstimator.estimateTokens(turn.assistantMessage)
            if (turnTokens > remainingTokens) break
            selectedHistoryNewestFirst.add(turn)
            remainingTokens -= turnTokens
        }

        return PlannedPrompt(
            history = selectedHistoryNewestFirst.asReversed(),
            contextChunks = selectedChunks
        )
    }
}
