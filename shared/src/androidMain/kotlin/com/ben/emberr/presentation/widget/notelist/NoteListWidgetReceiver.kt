// The manifest-registered receiver that hands NoteListWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.notelist

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NoteListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteListWidget()
}
