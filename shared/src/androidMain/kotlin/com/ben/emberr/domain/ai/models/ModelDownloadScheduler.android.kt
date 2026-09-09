package com.ben.emberr.domain.ai.models

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile

actual class ModelDownloadScheduler(private val context: Context) {

    actual fun scheduleEmbeddingModelDownload(): Flow<ModelDownloadProgress> {
        enqueue(ModelFileNames.EMBEDDER, UNIQUE_WORK_EMBEDDER)
        return observe(UNIQUE_WORK_EMBEDDER)
    }

    actual fun scheduleGeneratorModelDownload(): Flow<ModelDownloadProgress> {
        enqueue(ModelFileNames.GENERATOR, UNIQUE_WORK_GENERATOR)
        return observe(UNIQUE_WORK_GENERATOR)
    }

    actual fun observeEmbeddingModelDownload(): Flow<ModelDownloadProgress> = observe(UNIQUE_WORK_EMBEDDER)

    actual fun observeGeneratorModelDownload(): Flow<ModelDownloadProgress> = observe(UNIQUE_WORK_GENERATOR)

    actual fun cancelEmbeddingModelDownload() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_EMBEDDER)
    }

    actual fun cancelGeneratorModelDownload() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_GENERATOR)
    }

    actual suspend fun isEmbeddingDownloadRunning(): Boolean = isRunning(UNIQUE_WORK_EMBEDDER)

    actual suspend fun isGeneratorDownloadRunning(): Boolean = isRunning(UNIQUE_WORK_GENERATOR)

    private suspend fun isRunning(uniqueWorkName: String): Boolean {
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(uniqueWorkName).first()
        val info = infos.firstOrNull() ?: return false
        return !info.state.isFinished
    }

    private fun enqueue(fileName: String, uniqueWorkName: String) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(ModelDownloadWorker.KEY_FILE_NAME to fileName))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun observe(uniqueWorkName: String): Flow<ModelDownloadProgress> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(uniqueWorkName)
            .map { infos -> infos.firstOrNull() }
            .filterNotNull()
            .transformWhile { info ->
                val progress = info.toModelDownloadProgress()
                if (progress != null) emit(progress)
                !info.state.isFinished
            }

    private fun WorkInfo.toModelDownloadProgress(): ModelDownloadProgress? = when (state) {
        WorkInfo.State.SUCCEEDED -> ModelDownloadProgress.Completed
        WorkInfo.State.FAILED -> {
            val reportedMessage = outputData.getString(ModelDownloadWorker.KEY_ERROR_MESSAGE)
            if (reportedMessage == null) {
                ModelDownloadLog.e("Work failed before the worker ran, so no reason was reported")
            }
            ModelDownloadProgress.Failed(
                reportedMessage ?: "Couldn't download the model — check your internet connection and try again."
            )
        }
        WorkInfo.State.CANCELLED -> ModelDownloadProgress.Failed("Download was cancelled.")
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> null
        else -> {
            val fraction = progress.getFloat(ModelDownloadWorker.KEY_PROGRESS_FRACTION, -1f)
            if (fraction < 0f) null else ModelDownloadProgress.Downloading(fraction)
        }
    }

    private companion object {
        const val UNIQUE_WORK_EMBEDDER = "model_download_embedder"
        const val UNIQUE_WORK_GENERATOR = "model_download_generator"
    }
}
