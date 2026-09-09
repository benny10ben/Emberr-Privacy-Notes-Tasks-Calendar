package com.emberr.domain.ai

import com.emberr.domain.ai.chat.ChatTurn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptBudgetTest {

    private fun textOfTokenSize(tokens: Int): String = "x".repeat(tokens * 4)

    private fun turnOfTokenSize(tokens: Int, label: String): ChatTurn {
        val halfTokens = tokens / 2
        return ChatTurn(
            userMessage = label + "u".repeat(halfTokens * 4 - label.length),
            assistantMessage = "a".repeat(halfTokens * 4)
        )
    }

    @Test
    fun anEmptyStringCostsNoTokens() {
        assertEquals(0, TokenEstimator.estimateTokens(""))
    }

    @Test
    fun tokensAreEstimatedAtFourCharactersEachAndAlwaysRoundedUp() {
        assertEquals(1, TokenEstimator.estimateTokens("a"))
        assertEquals(1, TokenEstimator.estimateTokens("abcd"))
        assertEquals(2, TokenEstimator.estimateTokens("abcde"))
        assertEquals(25, TokenEstimator.estimateTokens("x".repeat(100)))
    }

    @Test
    fun everythingFitsWhenTheContextWindowIsGenerous() {
        val plan = PromptBudgetPlanner.plan(
            totalContextTokens = 10_000,
            outputReservationTokens = 500,
            systemPrompt = textOfTokenSize(10),
            userQuestion = textOfTokenSize(10),
            candidateHistory = listOf(turnOfTokenSize(20, "first"), turnOfTokenSize(20, "second")),
            candidateChunks = listOf(textOfTokenSize(50), textOfTokenSize(50))
        )

        assertEquals(2, plan.contextChunks.size)
        assertEquals(2, plan.history.size)
    }

    @Test
    fun theOutputReservationCanNeverEatMoreThanHalfTheContextWindow() {
        val plan = PromptBudgetPlanner.plan(
            totalContextTokens = 100,
            outputReservationTokens = 10_000,
            systemPrompt = "",
            userQuestion = "",
            candidateHistory = emptyList(),
            candidateChunks = listOf(textOfTokenSize(50), textOfTokenSize(10))
        )

        assertEquals(listOf(textOfTokenSize(50)), plan.contextChunks)
    }

    @Test
    fun anOversizedChunkIsSkippedButSmallerOnesAfterItStillFit() {
        val oversizedChunk = textOfTokenSize(1_000)
        val smallChunk = textOfTokenSize(5)

        val plan = PromptBudgetPlanner.plan(
            totalContextTokens = 100,
            outputReservationTokens = 0,
            systemPrompt = "",
            userQuestion = "",
            candidateHistory = emptyList(),
            candidateChunks = listOf(oversizedChunk, smallChunk)
        )

        assertEquals(listOf(smallChunk), plan.contextChunks)
    }

    @Test
    fun historyStopsAtTheFirstTurnThatWillNotFitInsteadOfSkippingIt() {
        val oldestAndOversized = turnOfTokenSize(200, "oldest")
        val middle = turnOfTokenSize(10, "middle")
        val newest = turnOfTokenSize(10, "newest")

        val plan = PromptBudgetPlanner.plan(
            totalContextTokens = 100,
            outputReservationTokens = 0,
            systemPrompt = "",
            userQuestion = "",
            candidateHistory = listOf(oldestAndOversized, middle, newest),
            candidateChunks = emptyList()
        )

        assertEquals(listOf(middle, newest), plan.history)
    }

    @Test
    fun theNewestTurnsAreKeptAndHandedBackInTheOrderTheyHappened() {
        val oldest = turnOfTokenSize(10, "oldest")
        val middle = turnOfTokenSize(10, "middle")
        val newest = turnOfTokenSize(10, "newest")

        val plan = PromptBudgetPlanner.plan(
            totalContextTokens = 25,
            outputReservationTokens = 0,
            systemPrompt = "",
            userQuestion = "",
            candidateHistory = listOf(oldest, middle, newest),
            candidateChunks = emptyList()
        )

        assertEquals(listOf(middle, newest), plan.history)
    }

    @Test
    fun contextChunksAreOfferedTheBudgetBeforeTheChatHistory() {
        val chunk = textOfTokenSize(20)
        val turn = turnOfTokenSize(20, "turn")

        val plan = PromptBudgetPlanner.plan(
            totalContextTokens = 30,
            outputReservationTokens = 0,
            systemPrompt = "",
            userQuestion = "",
            candidateHistory = listOf(turn),
            candidateChunks = listOf(chunk)
        )

        assertEquals(listOf(chunk), plan.contextChunks)
        assertTrue(plan.history.isEmpty())
    }

    @Test
    fun nothingIsIncludedWhenTheQuestionAloneAlreadyFillsTheWindow() {
        val plan = PromptBudgetPlanner.plan(
            totalContextTokens = 10,
            outputReservationTokens = 0,
            systemPrompt = textOfTokenSize(100),
            userQuestion = textOfTokenSize(100),
            candidateHistory = listOf(turnOfTokenSize(2, "turn")),
            candidateChunks = listOf(textOfTokenSize(1))
        )

        assertTrue(plan.contextChunks.isEmpty())
        assertTrue(plan.history.isEmpty())
    }

    @Test
    fun theExternalContextWindowStaysAtTheSizeTheAdaptersWereBuiltFor() {
        assertEquals(32_000, AiContextWindows.EXTERNAL_TOKENS)
    }
}
