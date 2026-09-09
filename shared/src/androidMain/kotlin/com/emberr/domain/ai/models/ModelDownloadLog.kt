package com.emberr.domain.ai.models

import android.util.Log

actual object ModelDownloadLog {
    private const val TAG = "EmberrModelDownload"

    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}
