package com.ben.emberr.domain.backup.automatic

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ben.emberr.data.local.prefs.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val settingsManager: SettingsManager,
    private val backupNotifier: BackupNotifier,
    private val backupExporter: BackupSnapshotExporter,
    private val backupScheduler: BackupScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val isEnabled = settingsManager.autoBackupEnabledFlow.first()
            if (!isEnabled) return@withContext Result.success()

            val uriString = settingsManager.backupDirectoryUriFlow.first()
            if (uriString.isNullOrBlank()) {
                backupNotifier.showBackupFailedNotification(
                    "Backup Failed",
                    "Auto-backup is enabled, but no folder is selected. Please check your settings."
                )
                return@withContext Result.failure()
            }

            val treeUri = Uri.parse(uriString)
            val pickedDir = DocumentFile.fromTreeUri(applicationContext, treeUri)

            if (pickedDir == null || !pickedDir.canWrite()) {
                settingsManager.saveAutoBackupEnabled(false)
                backupNotifier.showBackupFailedNotification(
                    "Backup Folder Missing",
                    "Emberr lost access to your backup folder. Auto-backups have been paused."
                )
                return@withContext Result.failure()
            }

            enforceRetentionPolicy(pickedDir, keepCount = 3)

            val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "EmberrBackup_$timeStamp.emberr"

            val newBackupFile = pickedDir.createFile("application/zip", fileName)
                ?: throw java.io.IOException("Failed to create file. Storage might be full.")

            try {
                backupExporter.exportToZip(newBackupFile.uri)
            } catch (e: Exception) {
                newBackupFile.delete()
                throw e
            }
            backupNotifier.showBackupSuccessNotification(fileName)

            Log.d("BackupWorker", "Background backup completed successfully: $fileName")

            scheduleNextRun()
            return@withContext Result.success()

        } catch (e: java.io.IOException) {
            e.printStackTrace()
            backupNotifier.showBackupFailedNotification(
                "Storage Full",
                "Your automated backup failed because the device is out of storage space."
            )
            return@withContext Result.failure()

        } catch (e: SecurityException) {
            e.printStackTrace()
            backupNotifier.showBackupFailedNotification(
                "Permission Denied",
                "Emberr doesn't have permission to write to your backup folder."
            )
            return@withContext Result.failure()

        } catch (e: Exception) {
            e.printStackTrace()
            backupNotifier.showBackupFailedNotification(
                "Backup Failed",
                "Something went wrong while creating your backup: ${e.message}"
            )
            return@withContext Result.failure()
        }
    }

    private suspend fun scheduleNextRun() {
        val isEnabled = settingsManager.autoBackupEnabledFlow.first()
        if (isEnabled) {
            val freq = settingsManager.backupFrequencyFlow.first()
            val time = settingsManager.backupTimeFlow.first()
            val day = settingsManager.backupDayFlow.first()
            backupScheduler.scheduleBackup(freq, time, day, forceReplace = true)
        }
    }

    private fun enforceRetentionPolicy(dir: DocumentFile, keepCount: Int) {
        try {
            val existingBackups = dir.listFiles()
                .filter { it.name?.contains("EmberrBackup_") == true }
                .sortedByDescending { it.lastModified() }

            if (existingBackups.size >= keepCount) {
                val backupsToDelete = existingBackups.drop(keepCount - 1)
                backupsToDelete.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
