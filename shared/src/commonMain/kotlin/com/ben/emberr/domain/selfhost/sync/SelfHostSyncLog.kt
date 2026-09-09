package com.ben.emberr.domain.selfhost.sync

expect object SelfHostSyncLog {
    fun d(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
