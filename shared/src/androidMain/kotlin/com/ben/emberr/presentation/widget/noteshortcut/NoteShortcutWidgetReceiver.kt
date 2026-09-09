// The manifest-registered receiver that hands NoteShortcutWidget to the Android widget framework.
package com.ben.emberr.presentation.widget.noteshortcut

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NoteShortcutWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteShortcutWidget()
}
