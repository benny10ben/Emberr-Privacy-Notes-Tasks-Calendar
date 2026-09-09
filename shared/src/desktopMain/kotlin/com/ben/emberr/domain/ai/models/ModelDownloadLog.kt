package com.ben.emberr.domain.ai.models

actual object ModelDownloadLog {
    private const val TAG = "EmberrModelDownload"

    actual fun e(message: String, throwable: Throwable?) {
        println("[$TAG] $message")
        throwable?.printStackTrace()
    }
}
