// Handles the chevron taps, moving the widget one month back or forward.
package com.ben.emberr.presentation.widget.calendaragenda

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

class CalendarAgendaWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val monthsToAdd = when (intent.action) {
            showPreviousAgendaMonthAction -> -1
            showNextAgendaMonthAction -> 1
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
                val nextMonth = shiftAgendaMonth(
                    readAgendaShownMonth(applicationContext, glanceId),
                    monthsToAdd
                )

                writeAgendaShownMonth(applicationContext, glanceId, nextMonth)
                refreshCalendarAgendaWidget(applicationContext, glanceId)
            } catch (cause: Exception) {
                WidgetLog.e("Could not change the shown month", cause)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
