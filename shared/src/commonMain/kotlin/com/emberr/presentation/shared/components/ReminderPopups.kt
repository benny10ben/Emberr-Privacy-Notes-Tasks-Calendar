@file:OptIn(ExperimentalMaterial3Api::class)

package com.emberr.presentation.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.LocalEmberrFontStyle
import com.emberr.ui.theme.fontFamilyFor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.bell_off2
import emberr.shared.generated.resources.calendar_add3
import emberr.shared.generated.resources.calendar_day
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.history2
import emberr.shared.generated.resources.history3
import emberr.shared.generated.resources.moon_cloud
import emberr.shared.generated.resources.sofa
import emberr.shared.generated.resources.square_arrows_right
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.util.Calendar
import kotlin.math.abs

private val DesktopMenuWidth = 240.dp
@Composable
fun ReminderPresetMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPresetSelected: (Long) -> Unit,
    onCustomSelected: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(DesktopMenuWidth)
        ) {
            DropdownMenuItem(
                text = { Text("Later today", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.calendar_day), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.LATER_TODAY)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            DropdownMenuItem(
                text = { Text("Tomorrow", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.calendar_day), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.TOMORROW)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            DropdownMenuItem(
                text = { Text("This weekend", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.sofa), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.THIS_WEEKEND)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            DropdownMenuItem(
                text = { Text("Next week", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.square_arrows_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.NEXT_WEEK)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
            DropdownMenuItem(
                text = { Text("Custom date...", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.calendar_add3), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onCustomSelected(); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            if (onRemove != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
                DropdownMenuItem(
                    text = { Text("Remove reminder", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.bell_off2),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = { onRemove(); onDismiss() },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
                )
            }
        }
    } else {
        EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Date") {
          CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                PresetSheetItem(painterResource(Res.drawable.calendar_day), "Later today") {
                    onPresetSelected(
                        getDatePresetTime(DatePresetType.LATER_TODAY)
                    ); onDismiss()
                }
                PresetSheetItem(painterResource(Res.drawable.calendar_day), "Tomorrow") {
                    onPresetSelected(
                        getDatePresetTime(DatePresetType.TOMORROW)
                    ); onDismiss()
                }
                PresetSheetItem(painterResource(Res.drawable.sofa), "This weekend") {
                    onPresetSelected(
                        getDatePresetTime(DatePresetType.THIS_WEEKEND)
                    ); onDismiss()
                }
                PresetSheetItem(painterResource(Res.drawable.square_arrows_right), "Next week") {
                    onPresetSelected(
                        getDatePresetTime(DatePresetType.NEXT_WEEK)
                    ); onDismiss()
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                PresetSheetItem(
                    painterResource(Res.drawable.calendar_add3),
                    "Custom date..."
                ) { onCustomSelected(); onDismiss() }
                if (onRemove != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    PresetSheetItem(
                        painterResource(Res.drawable.bell_off2),
                        "Remove reminder",
                        isDestructive = true
                    ) { onRemove(); onDismiss() }
                }
                EmberrButtonPrimary(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
          }
        }
    }
}

@Composable
fun TimePresetMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPresetSelected: (Long) -> Unit,
    onCustomSelected: () -> Unit
) {
    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(DesktopMenuWidth)
        ) {
            DropdownMenuItem(
                text = { Text("In 15 mins", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.history2), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_15_MINS)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            DropdownMenuItem(
                text = { Text("In 1 hour", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.history2), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_1_HOUR)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            DropdownMenuItem(
                text = { Text("In 3 hours", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.history3), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_3_HOURS)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            DropdownMenuItem(
                text = { Text("This evening", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.moon_cloud), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.THIS_EVENING)); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
            DropdownMenuItem(
                text = { Text("Custom time...", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painter = painterResource(Res.drawable.clock_circle), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                onClick = { onCustomSelected(); onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            )
        }
    } else {
        EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Time") {
          CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                PresetSheetItem(painterResource(Res.drawable.history2), "In 15 mins") {
                    onPresetSelected(
                        getTimePreset(
                            TimePresetType.IN_15_MINS
                        )
                    ); onDismiss()
                }
                PresetSheetItem(painterResource(Res.drawable.history2), "In 1 hour") {
                    onPresetSelected(
                        getTimePreset(TimePresetType.IN_1_HOUR)
                    ); onDismiss()
                }
                PresetSheetItem(painterResource(Res.drawable.history3), "In 3 hours") {
                    onPresetSelected(
                        getTimePreset(TimePresetType.IN_3_HOURS)
                    ); onDismiss()
                }
                PresetSheetItem(painterResource(Res.drawable.moon_cloud), "This evening") {
                    onPresetSelected(
                        getTimePreset(TimePresetType.THIS_EVENING)
                    ); onDismiss()
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                PresetSheetItem(
                    painterResource(Res.drawable.clock_circle),
                    "Custom time..."
                ) { onCustomSelected(); onDismiss() }
                EmberrButtonPrimary(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
          }
        }
    }
}

@Composable
private fun PresetSheetItem(icon: Painter, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}
// Custom date picker
private val MonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private data class CalendarDate(val year: Int, val month: Int, val day: Int) // month is 0-11

private fun CalendarDate.toMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(year, month, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun Long.toCalendarDate(): CalendarDate {
    val cal = Calendar.getInstance().apply { timeInMillis = this@toCalendarDate }
    return CalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
}

private fun daysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun firstWeekdayOfMonth(year: Int, month: Int): Int {
    // 0 = Sunday ... 6 = Saturday
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)
    return cal.get(Calendar.DAY_OF_WEEK) - 1
}

private class CustomDatePickerState(initialTimestamp: Long?) {
    private val initial = (initialTimestamp ?: System.currentTimeMillis()).toCalendarDate()

    var selectedDate by mutableStateOf(initial)
        private set
    var viewYear by mutableIntStateOf(initial.year)
        private set
    var viewMonth by mutableIntStateOf(initial.month)
        private set

    val selectedMillis: Long get() = selectedDate.toMillis()

    fun selectDay(day: Int) {
        selectedDate = CalendarDate(viewYear, viewMonth, day)
    }

    fun goToPreviousMonth() {
        if (viewMonth == 0) {
            viewMonth = 11
            viewYear -= 1
        } else {
            viewMonth -= 1
        }
    }

    fun goToNextMonth() {
        if (viewMonth == 11) {
            viewMonth = 0
            viewYear += 1
        } else {
            viewMonth += 1
        }
    }

    fun setMonthYear(month: Int, year: Int) {
        viewMonth = month
        viewYear = year
    }
}

@Composable
private fun rememberCustomDatePickerState(initialTimestamp: Long?): CustomDatePickerState {
    return remember { CustomDatePickerState(initialTimestamp) }
}

@Composable
private fun CustomCalendarHeader(
    viewYear: Int,
    viewMonth: Int,
    onHeaderClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onHeaderClick)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${MonthNames[viewMonth]} $viewYear",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Choose month or year",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.weight(1f).padding(end = 2.dp))
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
            Icon(painterResource(Res.drawable.chevron_left), contentDescription = "Previous month")
        }
        IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp).padding(end = 2.dp)) {
            Icon(painterResource(Res.drawable.chevron_right), contentDescription = "Next month")
        }
    }
}

@Composable
private fun CalendarWeekdayRow() {
    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        weekdays.forEach { label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CustomCalendarGrid(
    viewYear: Int,
    viewMonth: Int,
    selectedDate: CalendarDate,
    onDaySelected: (Int) -> Unit
) {
    val today = remember { System.currentTimeMillis().toCalendarDate() }
    val cells = remember(viewYear, viewMonth) {
        val daysCount = daysInMonth(viewYear, viewMonth)
        val startOffset = firstWeekdayOfMonth(viewYear, viewMonth)
        buildList {
            repeat(startOffset) { add(null) }
            for (day in 1..daysCount) add(day)
            while (size % 7 != 0) add(null)
        }.chunked(7)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        cells.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val isSelected = selectedDate.year == viewYear &&
                                    selectedDate.month == viewMonth &&
                                    selectedDate.day == day
                            val isToday = today.year == viewYear &&
                                    today.month == viewMonth &&
                                    today.day == day

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        when {
                                            isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                            isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                            else -> Modifier
                                        }
                                    )
                                    .clickable { onDaySelected(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthYearPickerContent(
    initialMonth: Int,
    initialYear: Int,
    onBack: () -> Unit,
    onConfirm: (month: Int, year: Int) -> Unit
) {
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    val years = remember { ((initialYear - 100)..(initialYear + 100)).map { it.toString() } }
    val pickerItemHeight = if (isDesktopPlatform) 40.dp else 44.dp

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                items = MonthNames,
                selectedIndex = selectedMonth,
                onItemSelected = { selectedMonth = it },
                itemHeight = pickerItemHeight,
                itemWidth = if (isDesktopPlatform) 108.dp else 124.dp,
                selectedSize = 18f,
                unselectedSize = 14f
            )
            Spacer(modifier = Modifier.width(12.dp))
            WheelPicker(
                items = years,
                selectedIndex = years.indexOf(selectedYear.toString()).coerceAtLeast(0),
                onItemSelected = { selectedYear = years[it].toInt() },
                itemHeight = pickerItemHeight
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmberrButtonSecondary(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
            EmberrButtonPrimary(
                text = "Save",
                onClick = { onConfirm(selectedMonth, selectedYear) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MinimalDatePickerDialog(
    expanded: Boolean = true,
    initialTimestamp: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val pickerState = rememberCustomDatePickerState(initialTimestamp)
    var showMonthYearPicker by remember { mutableStateOf(false) }

    // Always land back on the calendar view the next time this dialog is opened.
    LaunchedEffect(expanded) {
        if (!expanded) showMonthYearPicker = false
    }

    val calendarContent = @Composable {
        Column(modifier = Modifier.fillMaxWidth()) {
            CustomCalendarHeader(
                viewYear = pickerState.viewYear,
                viewMonth = pickerState.viewMonth,
                onHeaderClick = { showMonthYearPicker = true },
                onPreviousMonth = { pickerState.goToPreviousMonth() },
                onNextMonth = { pickerState.goToNextMonth() }
            )
            CalendarWeekdayRow()
            CustomCalendarGrid(
                viewYear = pickerState.viewYear,
                viewMonth = pickerState.viewMonth,
                selectedDate = pickerState.selectedDate,
                onDaySelected = { day -> pickerState.selectDay(day) }
            )
        }
    }

    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            Column(modifier = Modifier.width(300.dp).wrapContentHeight().padding(bottom = 12.dp)) {
                if (showMonthYearPicker) {
                    MonthYearPickerContent(
                        initialMonth = pickerState.viewMonth,
                        initialYear = pickerState.viewYear,
                        onBack = { showMonthYearPicker = false },
                        onConfirm = { month, year ->
                            pickerState.setMonthYear(month, year)
                            showMonthYearPicker = false
                        }
                    )
                } else {
                    calendarContent()

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EmberrButtonSecondary(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                        EmberrButtonPrimary(
                            text = "Save",
                            onClick = { onConfirm(pickerState.selectedMillis); onDismiss() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    } else {
        // the month/year picker is its own stacked EmberrBottomSheet, so opening it slides a
        // fresh sheet in on top instead of swapping this sheet's content in place
        EmberrBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            title = "Select Date"
        ) {
          CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
          ) {
            calendarContent()

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmberrButtonSecondary(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                EmberrButtonPrimary(
                    text = "Save",
                    onClick = { onConfirm(pickerState.selectedMillis); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }
          }
        }

        EmberrBottomSheet(
            expanded = showMonthYearPicker,
            onDismiss = { showMonthYearPicker = false },
            title = "Month & Year"
        ) {
          CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
          ) {
            MonthYearPickerContent(
                initialMonth = pickerState.viewMonth,
                initialYear = pickerState.viewYear,
                onBack = { showMonthYearPicker = false },
                onConfirm = { month, year ->
                    pickerState.setMonthYear(month, year)
                    showMonthYearPicker = false
                }
            )
          }
        }
    }
}

@Composable
fun MinimalTimePickerDialog(
    expanded: Boolean = true,
    initialTimestamp: Long?,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val cal = Calendar.getInstance().apply { if (initialTimestamp != null) timeInMillis = initialTimestamp }
    val initialHour24 = cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = cal.get(Calendar.MINUTE)
    var isAm by remember { mutableStateOf(initialHour24 < 12) }
    var hour by remember { mutableIntStateOf(if (initialHour24 % 12 == 0) 12 else initialHour24 % 12) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    val pickerItemHeight = if (isDesktopPlatform) 40.dp else 44.dp

    val content = @Composable {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = listOf("AM", "PM"),
                    selectedIndex = if (isAm) 0 else 1,
                    onItemSelected = { isAm = (it == 0) },
                    itemHeight = pickerItemHeight
                )
                Spacer(Modifier.width(16.dp))
                WheelPicker(
                    items = (1..12).map { it.toString().padStart(2, '0') },
                    selectedIndex = hour - 1,
                    onItemSelected = { hour = it + 1 },
                    itemHeight = pickerItemHeight
                )
                Text(
                    text = ":",
                    fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
                    fontSize = 22.sp,
                    modifier = Modifier.padding(horizontal = 6.dp).offset(y = (-4).dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                WheelPicker(
                    items = (0..59).map { it.toString().padStart(2, '0') },
                    selectedIndex = minute,
                    onItemSelected = { minute = it },
                    itemHeight = pickerItemHeight
                )
            }
        }
    }

    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            Column(modifier = Modifier.width(280.dp).wrapContentHeight()) {
                content()

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmberrButtonSecondary(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    EmberrButtonPrimary(text = "Save", onClick = { val finalHour = when { isAm && hour == 12 -> 0; !isAm && hour < 12 -> hour + 12; else -> hour }; onConfirm(finalHour, minute); onDismiss() }, modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Select Time") {
          CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
          ) {
            content()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmberrButtonSecondary(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                EmberrButtonPrimary(
                    text = "Save",
                    onClick = {
                        val finalHour = when {
                            isAm && hour == 12 -> 0
                            !isAm && hour < 12 -> hour + 12
                            else -> hour
                        }
                        onConfirm(finalHour, minute)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
          }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    selectedSize: Float = 22f,
    unselectedSize: Float = 16f,
    itemHeight: Dp = 44.dp,
    itemWidth: Dp = if (isDesktopPlatform) 48.dp else 64.dp
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf -1
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            val closestItem = visibleItemsInfo.minByOrNull { abs((it.offset + (it.size / 2)) - viewportCenter) }
            (closestItem?.index ?: 1) - 1
        }
    }

    LaunchedEffect(centerIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centerIndex in items.indices) {
            onItemSelected(centerIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = Modifier
            .height(itemHeight * 3)
            .width(itemWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(Modifier.height(itemHeight)) }
        items(items.size) { index ->
            val isSelected = centerIndex == index
            val animatedFontSize by animateFloatAsState(
                targetValue = if (isSelected) selectedSize else unselectedSize,
                animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                label = "fontSize"
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                animationSpec = tween(150),
                label = "color"
            )

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        coroutineScope.launch { listState.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
                    fontSize = animatedFontSize.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = animatedColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        item { Spacer(Modifier.height(itemHeight)) }
    }
}

private enum class DatePresetType { LATER_TODAY, TOMORROW, THIS_WEEKEND, NEXT_WEEK }
private enum class TimePresetType { IN_15_MINS, IN_1_HOUR, IN_3_HOURS, THIS_EVENING }

private fun getDatePresetTime(type: DatePresetType): Long {
    val cal = Calendar.getInstance()
    when (type) {
        DatePresetType.LATER_TODAY -> cal.add(Calendar.HOUR_OF_DAY, 4)
        DatePresetType.TOMORROW -> { cal.add(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0) }
        DatePresetType.THIS_WEEKEND -> { while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) { cal.add(Calendar.DATE, 1) }; cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0) }
        DatePresetType.NEXT_WEEK -> { do { cal.add(Calendar.DATE, 1) } while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0) }
    }
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun getTimePreset(type: TimePresetType): Long {
    val cal = Calendar.getInstance()
    when (type) {
        TimePresetType.IN_15_MINS -> cal.add(Calendar.MINUTE, 15)
        TimePresetType.IN_1_HOUR -> cal.add(Calendar.HOUR_OF_DAY, 1)
        TimePresetType.IN_3_HOURS -> cal.add(Calendar.HOUR_OF_DAY, 3)
        TimePresetType.THIS_EVENING -> { cal.set(Calendar.HOUR_OF_DAY, 18); cal.set(Calendar.MINUTE, 0) }
    }
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}