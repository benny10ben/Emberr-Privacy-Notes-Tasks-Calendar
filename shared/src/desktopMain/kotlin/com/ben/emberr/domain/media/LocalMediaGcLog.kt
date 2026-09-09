package com.ben.emberr.domain.media

actual object LocalMediaGcLog {
    private const val TAG = "EmberrLocalMediaGc"

    actual fun d(message: String) {
        println("[$TAG] $message")
    }

    actual fun e(message: String, throwable: Throwable?) {
        println("[$TAG] $message")
        throwable?.printStackTrace()
    }
}
