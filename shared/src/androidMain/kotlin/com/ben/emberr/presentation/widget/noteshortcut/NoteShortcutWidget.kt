// The home screen widget itself: draws one note's icon and name as a shortcut that opens it.
package com.ben.emberr.presentation.widget.noteshortcut

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ben.emberr.MainActivity
import com.ben.emberr.presentation.widget.WidgetLog
import com.ben.emberr.presentation.widget.primaryTextColor
import com.ben.emberr.presentation.widget.secondaryTextColor
import com.ben.emberr.presentation.widget.surfaceColor
import com.ben.emberr.presentation.widget.widgetNoteIdExtra
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private val pillHeight = 66.dp
private val pillCornerRadius = 33.dp

class NoteShortcutWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val storedNoteId = readShortcutNoteId(context, id)?.takeIf { it.isNotBlank() }

        val content = storedNoteId?.let { noteId ->
            loadAndCacheShortcut(context, id, noteId) ?: readCachedShortcut(context, id)
        }

        val appWidgetId = resolveAppWidgetId(context, id)

        provideContent {
            if (storedNoteId == null) {
                NoteShortcutSetupPrompt(context = context, appWidgetId = appWidgetId)
            } else {
                NoteShortcutWidgetBody(context = context, content = content)
            }
        }
    }

    private suspend fun loadAndCacheShortcut(
        context: Context,
        id: GlanceId,
        noteId: String
    ): NoteShortcutWidgetContent? {
        val contentReader = runCatching { get<NoteShortcutWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce(noteId) ?: return null

        if (freshContent != readCachedShortcut(context, id)) {
            writeCachedShortcut(context, id, freshContent)
        }
        return freshContent
    }

    private suspend fun resolveAppWidgetId(context: Context, id: GlanceId): Int =
        try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (cause: Exception) {
            WidgetLog.e("Could not resolve the widget id", cause)
            INVALID_APPWIDGET_ID
        }
}

@Composable
internal fun NoteShortcutWidgetBody(context: Context, content: NoteShortcutWidgetContent?) {
    if (content == null) {
        ShortcutSurface(tapModifier = GlanceModifier) {
            Text(
                text = "Open Emberr to load this note",
                maxLines = 1,
                style = TextStyle(color = secondaryTextColor, fontSize = 14.sp)
            )
        }
        return
    }

    ShortcutSurface(
        tapModifier = GlanceModifier.clickable(
            actionStartActivity(openNoteIntent(context, content.noteId))
        )
    ) {
        if (content.icon != null) {
            Text(text = content.icon, style = TextStyle(color = primaryTextColor, fontSize = 22.sp))
            Spacer(modifier = GlanceModifier.width(12.dp))
        }

        Text(
            text = content.title,
            maxLines = 1,
            style = TextStyle(
                color = primaryTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun NoteShortcutSetupPrompt(context: Context, appWidgetId: Int) {
    ShortcutSurface(
        tapModifier = GlanceModifier.clickable(
            actionStartActivity(openPickerIntent(context, appWidgetId))
        )
    ) {
        Text(
            text = "Tap to choose a note",
            maxLines = 1,
            style = TextStyle(color = secondaryTextColor, fontSize = 14.sp)
        )
    }
}

@Composable
private fun ShortcutSurface(tapModifier: GlanceModifier, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(pillHeight)
                .background(surfaceColor)
                .cornerRadius(pillCornerRadius)
                .padding(horizontal = 20.dp)
                .then(tapModifier),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            content()
        }
    }
}

private fun openNoteIntent(context: Context, noteId: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://note/$noteId".toUri())
        .putExtra(widgetNoteIdExtra, noteId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun openPickerIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, NoteShortcutWidgetPickerActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
