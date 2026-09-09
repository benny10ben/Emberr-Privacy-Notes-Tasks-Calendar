// The manifest-registered receiver that hands TodayTasksWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.todaytasks

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()
}
