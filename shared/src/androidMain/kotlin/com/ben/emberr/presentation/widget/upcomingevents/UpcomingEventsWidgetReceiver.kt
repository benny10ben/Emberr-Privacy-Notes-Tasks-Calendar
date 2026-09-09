// The manifest-registered receiver that hands UpcomingEventsWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.upcomingevents

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class UpcomingEventsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingEventsWidget()
}
