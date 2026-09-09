package com.ben.emberr.domain.util

import kotlinx.coroutines.flow.MutableStateFlow

enum class WidgetComposeRequest { NEW_TASK, NEW_NOTE, NEW_EVENT }

object WidgetComposeRequestBus {
    private val pendingRequest = MutableStateFlow<WidgetComposeRequest?>(null)

    fun request(request: WidgetComposeRequest) {
        pendingRequest.value = request
    }

    fun consume(request: WidgetComposeRequest): Boolean =
        pendingRequest.compareAndSet(request, null)
}
