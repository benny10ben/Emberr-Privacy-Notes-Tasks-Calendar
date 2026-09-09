package com.ben.emberr.domain.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WidgetNavigationBus {
    private val _requestedRoutes = MutableSharedFlow<String>(replay = 1)
    val requestedRoutes = _requestedRoutes.asSharedFlow()

    fun requestRoute(route: String) {
        _requestedRoutes.tryEmit(route)
    }

    fun consumeRequestedRoute() {
        _requestedRoutes.resetReplayCache()
    }
}
