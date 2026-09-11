// Event bus the repository shouts into, so it can announce a change without knowing a vault exists.

package com.emberr.domain.vault

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface VaultMirrorRequest {
    data class SingleNote(val noteId: String) : VaultMirrorRequest
    data object Everything : VaultMirrorRequest
}

// Only desktop listens. On Android nothing subscribes and the requests are dropped.
object VaultMirrorTrigger {

    private val _requests = MutableSharedFlow<VaultMirrorRequest>(extraBufferCapacity = 64)
    val requests = _requests.asSharedFlow()

    fun requestNoteRefresh(noteId: String) {
        _requests.tryEmit(VaultMirrorRequest.SingleNote(noteId))
    }

    fun requestFullRefresh() {
        _requests.tryEmit(VaultMirrorRequest.Everything)
    }
}
