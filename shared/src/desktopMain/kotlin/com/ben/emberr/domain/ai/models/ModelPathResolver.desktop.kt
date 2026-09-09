package com.ben.emberr.domain.ai.models

import java.io.File

actual fun resolveModelPath(fileName: String): String {
    val modelsDir = File(System.getProperty("user.home"), ".emberr/models")
    modelsDir.mkdirs()
    return File(modelsDir, fileName).absolutePath
}
actual fun modelFileExists(path: String): Boolean {
    val f = File(path)
    return f.exists() && f.length() > 0
}