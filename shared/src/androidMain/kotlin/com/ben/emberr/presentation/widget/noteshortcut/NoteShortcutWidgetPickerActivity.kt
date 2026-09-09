// The setup screen opened when the widget is added, for choosing which note it links to.
package com.ben.emberr.presentation.widget.noteshortcut

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.ben.emberr.presentation.widget.WidgetLog
import com.ben.emberr.presentation.widget.WidgetNoteChooser
import com.ben.emberr.presentation.widget.WidgetNoteSource
import com.ben.emberr.presentation.widget.WidgetSetupNotice
import com.ben.emberr.ui.theme.EmberrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get

class NoteShortcutWidgetPickerActivity : ComponentActivity() {

    private val noteSource: WidgetNoteSource? by lazy {
        runCatching { get<WidgetNoteSource>() }.getOrNull()
    }

    private val contentReader: NoteShortcutWidgetContentReader? by lazy {
        runCatching { get<NoteShortcutWidgetContentReader>() }.getOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED, resultIntentFor(appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            EmberrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val source = noteSource
                    if (source == null) {
                        WidgetSetupNotice("Open the app once before adding a widget")
                    } else {
                        val notesFlow = remember { source.observeSelectableNotes() }
                        val notes by notesFlow.collectAsState(initial = emptyList())

                        WidgetNoteChooser(
                            notes = notes,
                            onNoteChosen = { noteId -> applySelection(appWidgetId, noteId) }
                        )
                    }
                }
            }
        }
    }

    private fun applySelection(appWidgetId: Int, noteId: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val glanceId = GlanceAppWidgetManager(this@NoteShortcutWidgetPickerActivity)
                    .getGlanceIdBy(appWidgetId)

                updateAppWidgetState(
                    context = this@NoteShortcutWidgetPickerActivity,
                    definition = PreferencesGlanceStateDefinition,
                    glanceId = glanceId
                ) { preferences ->
                    preferences.toMutablePreferences().apply { this[shortcutNoteIdKey] = noteId }
                }

                contentReader?.readContentOnce(noteId)?.let { content ->
                    writeCachedShortcut(this@NoteShortcutWidgetPickerActivity, glanceId, content)
                    pushRenderedShortcut(this@NoteShortcutWidgetPickerActivity, glanceId, content)
                }

                NoteShortcutWidget().update(this@NoteShortcutWidgetPickerActivity, glanceId)
                withContext(Dispatchers.Main) { setResult(RESULT_OK, resultIntentFor(appWidgetId)) }
            } catch (cause: Exception) {
                WidgetLog.e("Could not attach note $noteId to the shortcut widget", cause)
                withContext(Dispatchers.Main) {
                    setResult(RESULT_CANCELED, resultIntentFor(appWidgetId))
                }
            }
            withContext(Dispatchers.Main) { finish() }
        }
    }

    private fun resultIntentFor(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
