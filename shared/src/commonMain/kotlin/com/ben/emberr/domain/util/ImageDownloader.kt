package com.ben.emberr.domain.util

interface ImageDownloader {
    suspend fun downloadImage(sourceFilePath: String, displayName: String): Boolean
}
