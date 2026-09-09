package com.emberr.presentation.shared

import kotlinx.coroutines.CompletableDeferred

object FirstContentRenderSignal {

    private val firstContentRendered = CompletableDeferred<Unit>()

    fun reportContentRendered() {
        firstContentRendered.complete(Unit)
    }

    suspend fun awaitFirstContent() {
        firstContentRendered.await()
    }
}
