package com.ben.emberr.domain.ai.models

import kotlinx.coroutines.flow.Flow

expect class ModelDownloadScheduler {
    fun scheduleEmbeddingModelDownload(): Flow<ModelDownloadProgress>
    fun scheduleGeneratorModelDownload(): Flow<ModelDownloadProgress>
    fun observeEmbeddingModelDownload(): Flow<ModelDownloadProgress>
    fun observeGeneratorModelDownload(): Flow<ModelDownloadProgress>
    fun cancelEmbeddingModelDownload()
    fun cancelGeneratorModelDownload()
    suspend fun isEmbeddingDownloadRunning(): Boolean
    suspend fun isGeneratorDownloadRunning(): Boolean
}
