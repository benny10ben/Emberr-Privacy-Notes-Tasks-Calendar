package com.ben.emberr.domain.selfhost.sync

actual object SelfHostSyncLog {
    private const val TAG = "EmberrSyncEngine"

    actual fun d(message: String) {
        println("[$TAG] $message")
    }

    actual fun e(message: String, throwable: Throwable?) {
        println("[$TAG] $message")
        throwable?.printStackTrace()
    }
}