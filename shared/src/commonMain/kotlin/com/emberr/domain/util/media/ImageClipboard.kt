package com.emberr.domain.util.media

expect object ImageClipboard {
    suspend fun copyImageToClipboard(filePath: String): Boolean
}
