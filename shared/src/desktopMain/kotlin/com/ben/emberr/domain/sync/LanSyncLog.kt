package com.ben.emberr.domain.sync

actual object LanSyncLog {
    private const val TAG = "EmberrLanSync"

    actual fun d(message: String) {
        println("[$TAG] $message")
    }

    actual fun e(message: String, throwable: Throwable?) {
        println("[$TAG] $message")
        throwable?.printStackTrace()
    }
}
