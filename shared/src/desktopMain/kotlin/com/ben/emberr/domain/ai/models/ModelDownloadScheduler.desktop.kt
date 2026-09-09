package com.ben.emberr.domain.ai.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class ModelDownloadScheduler(private val modelDownloadManager: ModelDownloadManager) {
    actual fun scheduleEmbeddingModelDownload(): Flow<ModelDownloadProgress> =
        modelDownloadManager.downloadEmbeddingModel()

    actual fun scheduleGeneratorModelDownload(): Flow<ModelDownloadProgress> =
        modelDownloadManager.downloadGeneratorModel()

    actual fun observeEmbeddingModelDownload(): Flow<ModelDownloadProgress> = emptyFlow()

    actual fun observeGeneratorModelDownload(): Flow<ModelDownloadProgress> = emptyFlow()

    actual fun cancelEmbeddingModelDownload() {}

    actual fun cancelGeneratorModelDownload() {}

    actual suspend fun isEmbeddingDownloadRunning(): Boolean = false

    actual suspend fun isGeneratorDownloadRunning(): Boolean = false
}
