package com.ben.emberr.domain.media

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Mirrors AutoSyncTrigger's shape - a global event bus so deep editor code (which has no direct
// dependency on LocalMediaGarbageCollector) can ask for a cleanup pass without needing it threaded
// through every ViewModel constructor. The actual collector debounces this at the app entry point,
// so rapid successive block deletions (e.g. a multi-select delete) collapse into one scan instead
// of one per block.
object LocalMediaGcTrigger {
    private val _cleanupRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cleanupRequests = _cleanupRequests.asSharedFlow()

    fun requestCleanup() {
        _cleanupRequests.tryEmit(Unit)
    }
}
