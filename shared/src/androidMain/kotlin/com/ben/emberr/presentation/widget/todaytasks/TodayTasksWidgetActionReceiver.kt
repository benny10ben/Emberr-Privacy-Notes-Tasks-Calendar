// Handles the checkbox taps on this widget, ticking a task off and redrawing every task widget.
package com.ben.emberr.presentation.widget.todaytasks

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ben.emberr.presentation.widget.WidgetLog
import com.ben.emberr.presentation.widget.applyTaskCompletion
import com.ben.emberr.presentation.widget.refreshEveryTaskWidget
import com.ben.emberr.presentation.widget.taskBlockIdExtra
import com.ben.emberr.presentation.widget.taskIsCheckedExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodayTasksWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != toggleTodayTaskAction) return

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val blockId = intent.getStringExtra(taskBlockIdExtra) ?: return
        val isChecked = intent.getBooleanExtra(taskIsCheckedExtra, false)
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                applyTaskCompletion(blockId, isChecked)
                refreshEveryTaskWidget(applicationContext)
            } catch (cause: Exception) {
                WidgetLog.e("Could not tick off task $blockId", cause)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
