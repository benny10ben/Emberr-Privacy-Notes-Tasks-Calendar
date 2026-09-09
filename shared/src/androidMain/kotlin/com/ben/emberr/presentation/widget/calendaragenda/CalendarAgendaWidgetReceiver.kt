// The manifest-registered receiver that hands CalendarAgendaWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.calendaragenda

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CalendarAgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarAgendaWidget()
}
