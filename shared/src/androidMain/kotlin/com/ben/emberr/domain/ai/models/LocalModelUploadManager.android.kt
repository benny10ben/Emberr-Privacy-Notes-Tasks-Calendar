package com.ben.emberr.domain.ai.models

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual class LocalModelUploadManager(private val context: Context) {

    actual suspend fun resolveDisplayName(pickedPath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.query(Uri.parse(pickedPath), null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex == -1 || !cursor.moveToFirst()) return@use null
                    cursor.getString(nameIndex)
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun resolveFileSize(pickedPath: String): Long {
        return try {
            context.contentResolver.query(Uri.parse(pickedPath), null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex == -1 || !cursor.moveToFirst()) return -1L
                if (cursor.isNull(sizeIndex)) -1L else cursor.getLong(sizeIndex)
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    actual fun copyPickedFileToModelPath(pickedPath: String, destinationFileName: String): Flow<ModelDownloadProgress> =
        channelFlow {
            withContext(Dispatchers.IO) {
                try {
                    val totalBytes = resolveFileSize(pickedPath)
                    val destinationPath = resolveModelPath(destinationFileName)
                    val tempFile = File("$destinationPath.part")
                    tempFile.parentFile?.mkdirs()

                    val inputStream = context.contentResolver.openInputStream(Uri.parse(pickedPath))
                        ?: throw IllegalStateException("Couldn't open the picked file.")

                    inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            copyWithProgress(input, output, totalBytes)
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
        input: java.io.InputStream,
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
