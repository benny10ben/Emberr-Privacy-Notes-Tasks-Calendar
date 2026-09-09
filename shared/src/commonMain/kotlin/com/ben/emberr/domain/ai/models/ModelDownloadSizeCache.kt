package com.ben.emberr.domain.ai.models

expect fun cacheExpectedModelSize(fileName: String, totalBytes: Long)
expect fun getCachedExpectedModelSize(fileName: String): Long?
expect fun clearCachedExpectedModelSize(fileName: String)

fun hasResumableDownload(fileName: String): Boolean =
    partialModelFileSize(resolveModelPath(fileName)) > 0

fun resumeProgressFraction(fileName: String): Float {
    val partialBytes = partialModelFileSize(resolveModelPath(fileName))
    if (partialBytes <= 0) return 0f
    val totalBytes = getCachedExpectedModelSize(fileName) ?: return 0f
    if (totalBytes <= 0) return 0f
    return (partialBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}
