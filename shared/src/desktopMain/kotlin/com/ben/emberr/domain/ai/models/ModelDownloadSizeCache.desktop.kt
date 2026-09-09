package com.ben.emberr.domain.ai.models

import java.io.File

private fun sizeCachePath(fileName: String) = "${resolveModelPath(fileName)}.size"

actual fun cacheExpectedModelSize(fileName: String, totalBytes: Long) {
    File(sizeCachePath(fileName)).writeText(totalBytes.toString())
}

actual fun getCachedExpectedModelSize(fileName: String): Long? {
    val file = File(sizeCachePath(fileName))
    if (!file.exists()) return null
    return file.readText().trim().toLongOrNull()
}

actual fun clearCachedExpectedModelSize(fileName: String) {
    File(sizeCachePath(fileName)).delete()
}
