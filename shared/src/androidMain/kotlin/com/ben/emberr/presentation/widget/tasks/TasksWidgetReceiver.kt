// The manifest-registered receiver that hands TasksWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.tasks

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()
}
