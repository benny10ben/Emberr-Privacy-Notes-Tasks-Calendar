package com.ben.emberr.domain.ai.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual class LocalModelUploadManager {

    actual suspend fun resolveDisplayName(pickedPath: String): String? =
        withContext(Dispatchers.IO) { File(pickedPath).name.ifBlank { null } }

    actual fun copyPickedFileToModelPath(pickedPath: String, destinationFileName: String): Flow<ModelDownloadProgress> =
        channelFlow {
            withContext(Dispatchers.IO) {
                try {
                    val sourceFile = File(pickedPath)
                    if (!sourceFile.exists()) throw IllegalStateException("The picked file no longer exists.")

                    val destinationPath = resolveModelPath(destinationFileName)
                    val tempFile = File("$destinationPath.part")
                    tempFile.parentFile?.mkdirs()

                    FileInputStream(sourceFile).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            copyWithProgress(input, output, sourceFile.length())
                        }
                    }

                    val destinationFile = File(destinationPath)
                    if (destinationFile.exists()) destinationFile.delete()
                    if (!tempFile.renameTo(destinationFile)) {
                        throw IllegalStateException("Couldn't finalize the copied file.")
                    }

                    send(ModelDownloadProgress.Completed)
                } catch (e: Exception) {
                    send(ModelDownloadProgress.Failed("Couldn't use that file. Make sure it's a valid GGUF model."))
                }
            }
        }

    private suspend fun ProducerScope<ModelDownloadProgress>.copyWithProgress(
        input: FileInputStream,
        output: FileOutputStream,
        totalBytes: Long
    ) {
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        var bytesCopied = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            bytesCopied += read
            val fraction = if (totalBytes > 0) (bytesCopied.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
            send(ModelDownloadProgress.Downloading(fraction))
        }
    }

    private companion object {
        const val COPY_BUFFER_SIZE = 256 * 1024
    }
}
