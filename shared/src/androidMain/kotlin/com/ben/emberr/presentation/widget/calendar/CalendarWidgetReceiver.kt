// The manifest-registered receiver that hands CalendarWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.calendar

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}
