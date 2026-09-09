package com.ben.emberr.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopImageDownloader : ImageDownloader {

    private val downloadsDir = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }

    override suspend fun downloadImage(sourceFilePath: String, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourceFilePath)
                if (!sourceFile.exists()) return@withContext false

                var destFile = File(downloadsDir, displayName)
                if (destFile.exists()) {
                    val baseName = destFile.nameWithoutExtension
                    val extension = destFile.extension
                    var counter = 1
                    while (destFile.exists()) {
                        val candidateName = if (extension.isNotEmpty()) "$baseName ($counter).$extension" else "$baseName ($counter)"
                        destFile = File(downloadsDir, candidateName)
                        counter++
                    }
                }

                sourceFile.copyTo(destFile, overwrite = false)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}
