// The manifest-registered receiver that hands NoteWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.note

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteWidget()
}
