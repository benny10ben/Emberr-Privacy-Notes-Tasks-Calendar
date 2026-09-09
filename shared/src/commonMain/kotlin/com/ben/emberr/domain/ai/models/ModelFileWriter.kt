package com.ben.emberr.domain.ai.models

import io.ktor.utils.io.ByteReadChannel

expect suspend fun writeChannelToModelFile(
    channel: ByteReadChannel,
    destinationPath: String,
    append: Boolean,
    onBytesWritten: suspend (bytesWrittenThisSession: Long) -> Unit
)

expect fun partialModelFileSize(destinationPath: String): Long
