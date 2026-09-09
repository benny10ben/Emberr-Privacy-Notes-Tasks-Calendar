package com.emberr.domain.util

import com.emberr.domain.model.PendingShare
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ShareEventBus {
    private val _pendingShare = MutableSharedFlow<PendingShare>(replay = 1)
    val pendingShare = _pendingShare.asSharedFlow()

    fun postPendingShare(share: PendingShare) {
        _pendingShare.tryEmit(share)
    }

    fun consumePendingShare() {
        _pendingShare.resetReplayCache()
    }
}
