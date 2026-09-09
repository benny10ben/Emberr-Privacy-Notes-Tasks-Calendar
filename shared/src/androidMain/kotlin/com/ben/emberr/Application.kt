package com.ben.emberr

import android.app.Application
import android.content.SharedPreferences
import com.ben.emberr.data.local.room.AppDatabase
import com.ben.emberr.di.androidModule
import com.ben.emberr.di.sharedModule
import com.ben.emberr.domain.ai.LocalAiEngine
import com.ben.emberr.domain.selfhost.sync.SelfHostSyncScheduler
import com.ben.emberr.presentation.reminders.ReminderRescheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import java.util.concurrent.atomic.AtomicBoolean

class EmberrApplication : Application() {
    companion object {
        @Volatile
        var isReady = false
    }

    private val hasStartedAiWarmUp = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EmberrApplication)
            workManagerFactory()
            modules(sharedModule, androidModule)
        }

        getKoin().get<SelfHostSyncScheduler>()

        CoroutineScope(Dispatchers.IO).launch {
            getKoin().get<AppDatabase>()
            getKoin().get<SharedPreferences>()
            isReady = true
            getKoin().get<com.ben.emberr.presentation.widget.note.NoteWidgetCoordinator>().start()
            getKoin().get<com.ben.emberr.presentation.widget.tasks.TasksWidgetCoordinator>().start()
            getKoin().get<com.ben.emberr.presentation.widget.notelist.NoteListWidgetCoordinator>().start()
            getKoin().get<com.ben.emberr.presentation.widget.noteshortcut.NoteShortcutWidgetCoordinator>().start()
            getKoin().get<com.ben.emberr.presentation.widget.todaytasks.TodayTasksWidgetCoordinator>().start()
            getKoin().get<com.ben.emberr.presentation.widget.calendar.CalendarWidgetCoordinator>().start()
            getKoin().get<com.ben.emberr.presentation.widget.calendaragenda.CalendarAgendaWidgetCoordinator>().start()
            getKoin().get<com.ben.emberr.presentation.widget.upcomingevents.UpcomingEventsWidgetCoordinator>().start()
            getKoin().get<ReminderRescheduler>().rescheduleUpcomingReminders()
        }

    }

    fun warmUpAiEngineOnce() {
        if (!hasStartedAiWarmUp.compareAndSet(false, true)) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (getKoin().get<com.ben.emberr.data.local.prefs.SettingsManager>().isAiFeaturesDisabled()) return@launch
                getKoin().get<LocalAiEngine>().warmUpGenerator()
            } catch (e: Exception) {
                // Silent — if pre-warm fails, first query just pays the load cost
            }
        }
    }
}