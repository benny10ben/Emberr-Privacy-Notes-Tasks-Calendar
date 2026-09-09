package com.ben.emberr.domain.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WidgetCalendarDateBus {
    private val _requestedDates = MutableSharedFlow<String>(replay = 1)
    val requestedDates = _requestedDates.asSharedFlow()

    fun requestDate(dateString: String) {
        _requestedDates.tryEmit(dateString)
    }

    fun consumeRequestedDate() {
        _requestedDates.resetReplayCache()
    }
}
