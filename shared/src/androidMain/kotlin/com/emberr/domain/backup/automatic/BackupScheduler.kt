package com.emberr.domain.backup.automatic

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.emberr.data.local.prefs.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BackupScheduler(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            combine(
                settingsManager.autoBackupEnabledFlow,
                settingsManager.backupFrequencyFlow,
                settingsManager.backupTimeFlow,
                settingsManager.backupDayFlow
            ) { enabled, freq, time, day ->
                BackupConfig(enabled, freq, time, day)
            }.distinctUntilChanged().collect { config ->
                if (config.enabled) {
                    scheduleBackup(config.freq, config.time, config.day, forceReplace = false)
                } else {
                    cancelBackup()
                }
            }
        }
    }

    fun rescheduleNow(frequency: String, timeString: String, dayString: String) {
        scheduleBackup(frequency, timeString, dayString, forceReplace = true)
    }

    fun scheduleBackup(frequency: String, timeString: String, dayString: String, forceReplace: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        val delayMillis = calculateDelay(frequency, timeString, dayString)
        val policy = if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag("EmberrAutoBackupTag")
            .build()

        workManager.enqueueUniqueWork("EmberrAutoBackup", policy, workRequest)
    }

    fun cancelBackup() {
        WorkManager.getInstance(context).cancelUniqueWork("EmberrAutoBackup")
    }

    private fun calculateDelay(frequency: String, timeString: String, dayString: String): Long {
        val parts = timeString.split(":")
        val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 2
        val targetMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (frequency == "Weekly") {
            val targetDay = when (dayString.lowercase()) {
                "monday" -> Calendar.MONDAY
                "tuesday" -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday" -> Calendar.THURSDAY
                "friday" -> Calendar.FRIDAY
                "saturday" -> Calendar.SATURDAY
                else -> Calendar.SUNDAY
            }
            target.set(Calendar.DAY_OF_WEEK, targetDay)
        }

        if (target.before(now)) {
            if (frequency == "Weekly") {
                target.add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return target.timeInMillis - now.timeInMillis
    }
}

data class BackupConfig(
    val enabled: Boolean,
    val freq: String,
    val time: String,
    val day: String
)
