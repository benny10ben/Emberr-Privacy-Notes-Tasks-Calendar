package com.ben.emberr.domain.util

expect object ImageClipboard {
    suspend fun copyImageToClipboard(filePath: String): Boolean
}
