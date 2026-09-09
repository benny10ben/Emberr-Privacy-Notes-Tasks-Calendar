package com.ben.emberr.domain.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WidgetCalendarEventBus {
    private val _requestedEvents = MutableSharedFlow<String>(replay = 1)
    val requestedEvents = _requestedEvents.asSharedFlow()

    fun requestEvent(blockId: String) {
        _requestedEvents.tryEmit(blockId)
    }

    fun consumeRequestedEvent() {
        _requestedEvents.resetReplayCache()
    }
}
