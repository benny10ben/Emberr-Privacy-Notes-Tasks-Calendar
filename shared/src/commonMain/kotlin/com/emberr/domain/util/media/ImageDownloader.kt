package com.emberr.domain.util.media

interface ImageDownloader {
    suspend fun downloadImage(sourceFilePath: String, displayName: String): Boolean
}
