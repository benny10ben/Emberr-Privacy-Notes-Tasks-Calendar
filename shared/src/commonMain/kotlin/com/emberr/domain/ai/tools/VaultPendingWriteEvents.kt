// In-memory event bus that announces AI-proposed vault writes as they happen.

package com.emberr.domain.ai.tools

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class VaultPendingWriteEvents {

    private val _proposed = MutableSharedFlow<VaultPendingWrite>(extraBufferCapacity = 8)
    val proposed: SharedFlow<VaultPendingWrite> = _proposed

    suspend fun notifyProposed(write: VaultPendingWrite) {
        _proposed.emit(write)
    }
}
