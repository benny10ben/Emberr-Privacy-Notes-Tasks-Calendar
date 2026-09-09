package com.emberr.domain.ai.models

expect object ModelDownloadLog {
    fun e(message: String, throwable: Throwable? = null)
}
