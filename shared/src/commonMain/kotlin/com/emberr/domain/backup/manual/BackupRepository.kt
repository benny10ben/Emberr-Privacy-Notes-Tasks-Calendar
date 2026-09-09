package com.emberr.domain.backup.manual

interface BackupRepository {

    suspend fun createBackupData(): EmberrBackupData

    suspend fun restoreBackup(backupData: EmberrBackupData)
}
