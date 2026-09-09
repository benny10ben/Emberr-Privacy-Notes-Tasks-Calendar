package com.ben.emberr.domain.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AiEventBus {
    private val _indexRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val indexRequest = _indexRequest.asSharedFlow()

    var activeNoteId: String? = null

    fun requestImmediateIndex() {
        _indexRequest.tryEmit(Unit)
    }

    private val _indexComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val indexComplete = _indexComplete.asSharedFlow()

    fun notifyIndexComplete() {
        _indexComplete.tryEmit(Unit)
    }
}