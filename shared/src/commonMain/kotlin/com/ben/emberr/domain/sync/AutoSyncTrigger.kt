package com.ben.emberr.domain.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A global event bus to trigger background syncs when the user modifies data.
 */
object AutoSyncTrigger {
    private val _syncRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val syncRequests = _syncRequests.asSharedFlow()

    fun requestSync() {
        _syncRequests.tryEmit(Unit)
    }
}