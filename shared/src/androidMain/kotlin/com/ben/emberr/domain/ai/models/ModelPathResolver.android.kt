package com.ben.emberr.domain.ai.models

import android.content.Context
import org.koin.mp.KoinPlatform

actual fun resolveModelPath(fileName: String): String {
    val context = KoinPlatform.getKoin().get<Context>()
    return java.io.File(context.filesDir, fileName).absolutePath
}

actual fun modelFileExists(path: String): Boolean {
    val f = java.io.File(path)
    return f.exists() && f.length() > 0
}