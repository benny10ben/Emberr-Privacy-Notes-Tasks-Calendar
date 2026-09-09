// Watches the note list so a renamed note updates its shortcut while the app runs.
package com.ben.emberr.presentation.widget.noteshortcut

import android.content.Context
import com.ben.emberr.presentation.widget.WidgetLog
import com.ben.emberr.presentation.widget.WidgetNoteSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val shortcutSettleDelayMillis = 150L

class NoteShortcutWidgetCoordinator(
    private val context: Context,
    private val noteSource: WidgetNoteSource
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    fun start() {
        coordinatorScope.launch {
            try {
                noteSource.observeSelectableNotes()
                    .distinctUntilChanged()
                    .debounce(shortcutSettleDelayMillis)
                    .collect { refreshNoteShortcutWidgets(context) }
            } catch (cause: Exception) {
                WidgetLog.e("Stopped watching notes for the shortcut widgets", cause)
            }
        }
    }
}
