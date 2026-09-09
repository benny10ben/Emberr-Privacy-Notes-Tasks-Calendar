// Reads and writes each widget's saved note name, and pushes a freshly drawn shortcut to the home screen.
package com.ben.emberr.presentation.widget.noteshortcut

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.ben.emberr.presentation.widget.WidgetLog
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext

private val cacheJson = Json { ignoreUnknownKeys = true }

private const val fallbackWidgetWidthDp = 180
private const val fallbackWidgetHeightDp = 60

fun decodeCachedShortcut(rawCache: String?): NoteShortcutWidgetContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<NoteShortcutWidgetContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable shortcut cache", cause)
        null
    }
}

suspend fun readShortcutNoteId(context: Context, glanceId: GlanceId): String? =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[shortcutNoteIdKey]
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the shortcut's note id", cause)
        null
    }

suspend fun readCachedShortcut(context: Context, glanceId: GlanceId): NoteShortcutWidgetContent? =
    try {
        decodeCachedShortcut(
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedShortcutKey]
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the shortcut cache", cause)
        null
    }

suspend fun writeCachedShortcut(context: Context, glanceId: GlanceId, content: NoteShortcutWidgetContent) {
    try {
        val encoded = cacheJson.encodeToString(NoteShortcutWidgetContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[cachedShortcutKey] = encoded }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the shortcut cache", cause)
    }
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
suspend fun pushRenderedShortcut(context: Context, glanceId: GlanceId, content: NoteShortcutWidgetContent) {
    try {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val widgetManager = AppWidgetManager.getInstance(context)
        val options = widgetManager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            .takeIf { it > 0 } ?: fallbackWidgetWidthDp
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            .takeIf { it > 0 } ?: fallbackWidgetHeightDp

        val rendered = GlanceRemoteViews().compose(
            context = context,
            size = DpSize(widthDp.dp, heightDp.dp)
        ) {
            NoteShortcutWidgetBody(context = context, content = content)
        }

        widgetManager.updateAppWidget(appWidgetId, rendered.remoteViews)
    } catch (cause: Exception) {
        WidgetLog.e("Could not push the rendered shortcut", cause)
    }
}

suspend fun refreshNoteShortcutWidget(context: Context, glanceId: GlanceId) {
    try {
        val noteId = readShortcutNoteId(context, glanceId)?.takeIf { it.isNotBlank() } ?: return
        val contentReader = GlobalContext.get().get<NoteShortcutWidgetContentReader>()
        val freshContent = contentReader.readContentOnce(noteId) ?: return

        if (freshContent == readCachedShortcut(context, glanceId)) return

        writeCachedShortcut(context, glanceId, freshContent)
        pushRenderedShortcut(context, glanceId, freshContent)
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh a shortcut widget", cause)
    }
}

suspend fun refreshNoteShortcutWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(NoteShortcutWidget::class.java)
            .forEach { glanceId -> refreshNoteShortcutWidget(context, glanceId) }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the shortcut widgets", cause)
    }
}
