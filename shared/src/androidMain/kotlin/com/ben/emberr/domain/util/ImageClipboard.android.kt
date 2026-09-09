package com.ben.emberr.domain.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.File

actual object ImageClipboard {
    actual suspend fun copyImageToClipboard(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false

            val context = KoinPlatform.getKoin().get<Context>()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val clip = ClipData.newUri(context.contentResolver, "Image", uri)

            withContext(Dispatchers.Main) {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(clip)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
