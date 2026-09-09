package com.ben.emberr.domain.media

import android.util.Log

actual object LocalMediaGcLog {
    private const val TAG = "EmberrLocalMediaGc"

    actual fun d(message: String) {
        Log.d(TAG, message)
    }

    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}
