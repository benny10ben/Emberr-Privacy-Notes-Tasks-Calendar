package com.ben.emberr.domain.selfhost.sync

import kotlinx.coroutines.flow.StateFlow

expect class SelfHostSyncScheduler {
    val isSyncActive: StateFlow<Boolean>
    val syncError: StateFlow<String?>
    fun scheduleDailySync()
    fun scheduleDeferredSyncAfterAppClose()
    fun scheduleMediaSync()
    fun syncNow()
    fun cancelAll()
}
