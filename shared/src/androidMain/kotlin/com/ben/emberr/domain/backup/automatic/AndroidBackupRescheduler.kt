package com.ben.emberr.domain.backup.automatic

class AndroidBackupRescheduler(
    private val backupScheduler: BackupScheduler
) : BackupRescheduler {
    override fun rescheduleNow(frequency: String, time: String, day: String) {
        backupScheduler.rescheduleNow(frequency, time, day)
    }
    override fun cancel() {
        backupScheduler.cancelBackup()
    }
}
