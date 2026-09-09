package com.ben.emberr.domain.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidImageDownloader(private val context: Context) : ImageDownloader {

    override suspend fun downloadImage(sourceFilePath: String, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) return@withContext false

            val mimeType = when (sourceFile.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Emberr")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false

            try {
                val written = resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input -> input.copyTo(output) }
                    true
                } ?: false

                if (!written) {
                    resolver.delete(uri, null, null)
                    return@withContext false
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(uri, null, null)
                false
            }
        }
}
