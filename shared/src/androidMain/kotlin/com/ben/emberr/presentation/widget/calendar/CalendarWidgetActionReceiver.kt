// Handles the chevron taps, moving the widget one month back or forward.
package com.ben.emberr.presentation.widget.calendar

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.ben.emberr.presentation.widget.WidgetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalendarWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val monthsToAdd = when (intent.action) {
            showPreviousMonthAction -> -1
            showNextMonthAction -> 1
            else -> return
        }

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        val applicationContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(appWidgetId)
                val nextMonth = shiftMonth(readShownMonth(applicationContext, glanceId), monthsToAdd)

                writeShownMonth(applicationContext, glanceId, nextMonth)
                refreshCalendarWidget(applicationContext, glanceId)
            } catch (cause: Exception) {
                WidgetLog.e("Could not change the shown month", cause)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
