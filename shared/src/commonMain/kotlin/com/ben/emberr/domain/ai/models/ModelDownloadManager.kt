package com.ben.emberr.domain.ai.models

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

sealed interface ModelDownloadProgress {
    data class Downloading(val fraction: Float) : ModelDownloadProgress
    data object Completed : ModelDownloadProgress
    data class Failed(val message: String) : ModelDownloadProgress
    data object Paused : ModelDownloadProgress
}

class ModelDownloadManager {

    private val httpClient by lazy {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
        }
    }

    fun downloadEmbeddingModel(): Flow<ModelDownloadProgress> =
        downloadModel(ModelFileNames.EMBEDDER, EMBEDDER_DOWNLOAD_URL)

    fun downloadGeneratorModel(): Flow<ModelDownloadProgress> =
        downloadModel(ModelFileNames.GENERATOR, GENERATOR_DOWNLOAD_URL)

    private fun downloadModel(fileName: String, url: String): Flow<ModelDownloadProgress> = channelFlow {
        try {
            val destinationPath = resolveModelPath(fileName)
            val alreadyDownloadedBytes = partialModelFileSize(destinationPath)

            httpClient.prepareGet(url) {
                if (alreadyDownloadedBytes > 0) {
                    header(HttpHeaders.Range, "bytes=$alreadyDownloadedBytes-")
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw IllegalStateException("Server responded with HTTP ${response.status.value}")
                }

                val isResuming = alreadyDownloadedBytes > 0 && response.status == HttpStatusCode.PartialContent
                val startOffset = if (isResuming) alreadyDownloadedBytes else 0L
                val remainingBytes = response.contentLength() ?: -1L
                val totalBytes = if (remainingBytes >= 0) startOffset + remainingBytes else -1L
                if (totalBytes > 0) cacheExpectedModelSize(fileName, totalBytes)

                writeChannelToModelFile(response.bodyAsChannel(), destinationPath, append = isResuming) { bytesWrittenThisSession ->
                    reportProgress(startOffset + bytesWrittenThisSession, totalBytes)
                }
            }
            clearCachedExpectedModelSize(fileName)
            send(ModelDownloadProgress.Completed)
        } catch (cause: HttpRequestTimeoutException) {
            ModelDownloadLog.e("Timed out downloading $fileName", cause)
            send(ModelDownloadProgress.Failed("The download timed out. Check your connection and try again."))
        } catch (cause: Exception) {
            ModelDownloadLog.e("Failed downloading $fileName", cause)
            send(ModelDownloadProgress.Failed("Couldn't download the model — check your internet connection and try again."))
        }
    }

    private suspend fun ProducerScope<ModelDownloadProgress>.reportProgress(bytesWritten: Long, totalBytes: Long) {
        val fraction = if (totalBytes > 0) (bytesWritten.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
        send(ModelDownloadProgress.Downloading(fraction))
    }

    private companion object {
        const val EMBEDDER_DOWNLOAD_URL =
            "https://huggingface.co/nomic-ai/nomic-embed-text-v1.5-GGUF/resolve/main/nomic-embed-text-v1.5.Q8_0.gguf"
        const val GENERATOR_DOWNLOAD_URL =
            "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q8_0.gguf"
        const val CONNECT_TIMEOUT_MS = 30_000L
        const val REQUEST_TIMEOUT_MS = 900_000L
        const val SOCKET_TIMEOUT_MS = 60_000L
    }
}
