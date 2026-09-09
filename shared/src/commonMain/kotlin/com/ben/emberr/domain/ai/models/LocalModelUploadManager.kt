package com.ben.emberr.domain.ai.models

import kotlinx.coroutines.flow.Flow

expect class LocalModelUploadManager {
    suspend fun resolveDisplayName(pickedPath: String): String?
    fun copyPickedFileToModelPath(pickedPath: String, destinationFileName: String): Flow<ModelDownloadProgress>
}
