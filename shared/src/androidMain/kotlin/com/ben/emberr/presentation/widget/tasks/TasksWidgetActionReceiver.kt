// Handles the widget's own taps - switching between open and completed tasks, and ticking a task off.
package com.ben.emberr.presentation.widget.tasks

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.ben.emberr.presentation.widget.WidgetLog
import com.ben.emberr.presentation.widget.applyTaskCompletion
import com.ben.emberr.presentation.widget.refreshEveryTaskWidget
import com.ben.emberr.presentation.widget.taskBlockIdExtra
import com.ben.emberr.presentation.widget.taskIsCheckedExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TasksWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val action = intent.action ?: return
        val blockId = intent.getStringExtra(taskBlockIdExtra)
        val isChecked = intent.getBooleanExtra(taskIsCheckedExtra, false)
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (action) {
                    toggleCompletedViewAction -> switchCompletedView(applicationContext, appWidgetId)
                    toggleTaskAction -> if (blockId != null) {
                        markTask(applicationContext, blockId, isChecked)
                    }
                }
            } catch (cause: Exception) {
                WidgetLog.e("Could not handle the widget tap", cause)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun switchCompletedView(context: Context, appWidgetId: Int) {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        writeShowingCompleted(context, glanceId, !readShowingCompleted(context, glanceId))
        refreshTaskWidget(context, glanceId)
    }

    private suspend fun markTask(context: Context, blockId: String, isChecked: Boolean) {
        applyTaskCompletion(blockId, isChecked)
        refreshEveryTaskWidget(context)
    }
}
