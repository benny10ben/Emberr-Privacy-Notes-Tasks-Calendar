package com.ben.emberr.domain.sync

expect object LanSyncLog {
    fun d(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
