package com.ben.emberr.domain.sync

sealed interface SyncServerStatus {
    data object Starting : SyncServerStatus
    data class Running(val port: Int) : SyncServerStatus
    data class Unavailable(val port: Int, val reason: String) : SyncServerStatus
}
