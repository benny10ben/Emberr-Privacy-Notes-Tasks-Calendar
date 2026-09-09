package com.ben.emberr.presentation.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ben.emberr.domain.model.RecurrenceEditScope
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

// Reusable host for opening the same EventEditorSheet used by CalendarScreen from anywhere a
// checkbox's reminder can be tapped (Daily/Note screens' three-dot menu) - avoids re-wiring the
// sheet's state + recurrence scope chooser in every screen that needs it.
@Composable
fun EventEditorSheetHost(
    targetBlockId: String?,
    targetOccurrenceDate: String? = null,
    onDismissTarget: () -> Unit,
    calendarViewModel: CalendarViewModel = koinViewModel()
) {
    val categories by calendarViewModel.categories.collectAsState()
    var eventEditorState by remember { mutableStateOf<EventEditorState?>(null) }
    var pendingScopeAction by remember { mutableStateOf<((RecurrenceEditScope) -> Unit)?>(null) }

    // Collected continuously (not a one-shot LaunchedEffect + Flow.first()) so there's no window
    // where a recomposition can observe targetBlockId having changed before the lookup's first
    // emission has landed - collectAsState just always reflects whatever the flow has emitted so
    // far, recomputing from scratch whenever the key changes.
    val resolvedEvent: CalendarEvent? by remember(targetBlockId, targetOccurrenceDate) {
        if (targetBlockId == null) flowOf(null) else calendarViewModel.eventForBlock(targetBlockId, targetOccurrenceDate)
    }.collectAsState(initial = null)

    LaunchedEffect(targetBlockId, resolvedEvent?.blockId) {
        if (targetBlockId == null) {
            eventEditorState = null
            return@LaunchedEffect
        }
        // Only (re)initialize the editable snapshot when a genuinely different event just
        // resolved - once the sheet is open for a given block, further emissions from the same
        // underlying flow (e.g. a background sync) must not clobber in-progress local edits.
        if (eventEditorState?.original?.blockId == targetBlockId) return@LaunchedEffect
        val event = resolvedEvent ?: return@LaunchedEffect
        val dt = Instant.fromEpochMilliseconds(event.reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        eventEditorState = EventEditorState(
            original = event,
            name = event.text,
            date = dt.date,
            hour = dt.hour,
            minute = dt.minute,
            categoryId = event.categoryId,
            durationMinutes = event.durationMinutes,
            url = event.url.orEmpty(),
            description = event.description.orEmpty(),
            recurrenceRule = event.recurrenceRule
        )
    }

    val dismiss: () -> Unit = {
        eventEditorState = null
        onDismissTarget()
    }

    EventEditorSheet(
        state = eventEditorState,
        categories = categories,
        onNameChange = { name -> eventEditorState = eventEditorState?.copy(name = name) },
        onDateChange = { date -> eventEditorState = eventEditorState?.copy(date = date) },
        onTimeChange = { hour, minute -> eventEditorState = eventEditorState?.copy(hour = hour, minute = minute) },
        onDurationChange = { minutes -> eventEditorState = eventEditorState?.copy(durationMinutes = minutes) },
        onCategoryChange = { categoryId -> eventEditorState = eventEditorState?.copy(categoryId = categoryId) },
        onUrlChange = { url -> eventEditorState = eventEditorState?.copy(url = url) },
        onDescriptionChange = { description -> eventEditorState = eventEditorState?.copy(description = description) },
        onRecurrenceChange = { rule -> eventEditorState = eventEditorState?.copy(recurrenceRule = rule) },
        onEditClick = {
            val recurring = eventEditorState?.original?.recurrenceRule != null
            if (recurring) {
                pendingScopeAction = { scope -> eventEditorState = eventEditorState?.copy(isEditing = true, editScope = scope) }
            } else {
                eventEditorState = eventEditorState?.copy(isEditing = true)
            }
        },
        onSave = {
            eventEditorState?.let { state ->
                calendarViewModel.saveEvent(
                    original = state.original,
                    dateString = state.date.toString(),
                    timestamp = state.toEpochMillis(),
                    name = state.name,
                    categoryId = state.categoryId,
                    durationMinutes = state.durationMinutes,
                    url = state.url,
                    description = state.description,
                    recurrenceRule = state.recurrenceRule,
                    editScope = state.editScope
                )
            }
            dismiss()
        },
        onDelete = eventEditorState?.original?.let { original ->
            {
                if (original.recurrenceRule != null) {
                    pendingScopeAction = { scope ->
                        calendarViewModel.deleteEvent(original, scope)
                        dismiss()
                    }
                } else {
                    calendarViewModel.deleteEvent(original)
                    dismiss()
                }
            }
        },
        onDismiss = dismiss
    )

    pendingScopeAction?.let { runWithScope ->
        RecurrenceScopeChooser(
            onDismiss = { pendingScopeAction = null },
            onScopeSelected = { scope ->
                pendingScopeAction = null
                runWithScope(scope)
            }
        )
    }
}
