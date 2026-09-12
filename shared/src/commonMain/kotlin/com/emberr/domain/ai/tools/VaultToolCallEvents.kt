// In-memory event bus that announces read-only vault tool calls, for chat transparency.

package com.emberr.domain.ai.tools

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class VaultToolCallSummary(val toolName: String, val description: String)

class VaultToolCallEvents {

    private val _calls = MutableSharedFlow<VaultToolCallSummary>(extraBufferCapacity = 16)
    val calls: SharedFlow<VaultToolCallSummary> = _calls

    suspend fun notifyCalled(summary: VaultToolCallSummary) {
        _calls.emit(summary)
    }
}
