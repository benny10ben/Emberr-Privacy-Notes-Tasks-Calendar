package com.ben.emberr.domain.ai.models

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun writeChannelToModelFile(
    channel: ByteReadChannel,
    destinationPath: String,
    append: Boolean,
    onBytesWritten: suspend (bytesWrittenThisSession: Long) -> Unit
) = withContext(Dispatchers.IO) {
    val tempFile = File("$destinationPath.part")
    tempFile.parentFile?.mkdirs()
    if (!append && tempFile.exists()) tempFile.delete()

    val buffer = ByteArray(64 * 1024)
    var bytesThisSession = 0L
    FileOutputStream(tempFile, append).use { output ->
        while (true) {
            val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
            if (bytesRead == -1) break
            output.write(buffer, 0, bytesRead)
            bytesThisSession += bytesRead
            onBytesWritten(bytesThisSession)
        }
    }

    val destinationFile = File(destinationPath)
    if (destinationFile.exists()) destinationFile.delete()
    if (!tempFile.renameTo(destinationFile)) {
        throw IllegalStateException("Failed to finalize downloaded model file at $destinationPath")
    }
}

actual fun partialModelFileSize(destinationPath: String): Long {
    val tempFile = File("$destinationPath.part")
    return if (tempFile.exists()) tempFile.length() else 0L
}
