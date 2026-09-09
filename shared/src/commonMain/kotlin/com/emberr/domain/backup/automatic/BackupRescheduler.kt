package com.emberr.domain.backup.automatic

interface BackupRescheduler {
    fun rescheduleNow(frequency: String, time: String, day: String)
    fun cancel()
}
