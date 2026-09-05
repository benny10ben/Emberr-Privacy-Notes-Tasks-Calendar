package com.ben.emberr.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ben.emberr.domain.sync.SyncPairingData
import com.ben.emberr.domain.util.AppPermission
import com.ben.emberr.domain.util.isDesktopPlatform
import com.ben.emberr.domain.util.rememberAppPermissionCoordinator
import com.ben.emberr.presentation.shared.stableStatusBarsPadding
import com.ben.emberr.presentation.shared.components.EmberrBottomSheet
import com.ben.emberr.presentation.shared.components.EmberrButtonPrimary
import com.ben.emberr.presentation.shared.components.EmberrButtonSecondary
import com.ben.emberr.presentation.shared.components.SelectedOptionBackground
import com.ben.emberr.presentation.shared.components.TopBarIconButton
import com.ben.emberr.presentation.sync.SyncPairingDialog
import com.ben.emberr.presentation.sync.SyncScannerDialog
import com.ben.emberr.presentation.sync.SyncViewModel
import com.ben.emberr.domain.util.showNativeToast
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.ben.emberr.ui.theme.FontStylePreference
import com.ben.emberr.ui.theme.ThemePreference
import com.ben.emberr.ui.theme.fontFamilyFor
import emberr.app.generated.resources.Res
import emberr.app.generated.resources.astroid
import emberr.app.generated.resources.badge_plus
import emberr.app.generated.resources.badge_question_mark
import emberr.app.generated.resources.calendar_clock
import emberr.app.generated.resources.chevron_left
import emberr.app.generated.resources.chevron_right
import emberr.app.generated.resources.file_down
import emberr.app.generated.resources.folder_input
import emberr.app.generated.resources.folder_sync
import emberr.app.generated.resources.info
import emberr.app.generated.resources.palette
import emberr.app.generated.resources.qr_code
import emberr.app.generated.resources.refresh_cw
import emberr.app.generated.resources.scan_line
import emberr.app.generated.resources.shield_alert
import emberr.app.generated.resources.sidebar
import emberr.app.generated.resources.timer_reset
import emberr.app.generated.resources.triangle_alert
import com.ben.emberr.presentation.shared.SubNoteOpenMode
import com.ben.emberr.presentation.shared.components.EmberrBlur
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onImportClick: () -> Unit = {},
    onExportReady: (String) -> Unit = {},
    onRequestBackupFolder: () -> Unit = {},
    onNavigateToSelfHostSetup: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
    syncViewModel: SyncViewModel = koinViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var showImportExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val isPaired by syncViewModel.isPaired.collectAsState()
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    var showPairingDialog by remember { mutableStateOf(false) }
    var activePairingData by remember { mutableStateOf<SyncPairingData?>(null) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showUnpairConfirmation by remember { mutableStateOf(false) }

    // Backup States
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val backupFrequency by viewModel.backupFrequency.collectAsState()
    val backupDirectoryUri by viewModel.backupDirectoryUri.collectAsState()

    val backupTime by viewModel.backupTime.collectAsState()
    val backupDay by viewModel.backupDay.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    val themePreference by viewModel.themePreference.collectAsState()
    var showThemeSheet by remember { mutableStateOf(false) }

    val fontSizePreference by viewModel.fontSizePreference.collectAsState()
    val fontStylePreference by viewModel.fontStylePreference.collectAsState()
    var showFontStyleSheet by remember { mutableStateOf(false) }

    val subNoteOpenMode by viewModel.subNoteOpenMode.collectAsState()
    var showSubNoteOpenModeSheet by remember { mutableStateOf(false) }

    val showScrollbar by viewModel.showScrollbar.collectAsState()

    val aiFeaturesDisabled by viewModel.aiFeaturesDisabled.collectAsState()
    val isPurgingAiData by viewModel.isPurgingAiData.collectAsState()
    val aiPurgeResultMessage by viewModel.aiPurgeResultMessage.collectAsState()
    var showDisableAiConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(aiPurgeResultMessage) {
        aiPurgeResultMessage?.let { message ->
            showNativeToast(message)
            viewModel.consumeAiPurgeResultMessage()
        }
    }

    LaunchedEffect(syncStatus) {
        if (syncStatus != "Idle" && syncStatus != "Syncing...") {
            showNativeToast(syncStatus)
            syncViewModel.resetSyncStatus()
        }
    }

    val HazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = HazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(top = topBarHeightDp + 8.dp, bottom = 48.dp)
        ) {
            if (!isDesktopPlatform) {
                item {
                    SettingsGroup(title = "Reminders") {
                        val permissionCoordinator = rememberAppPermissionCoordinator()
                        val isUnrestricted = permissionCoordinator
                            .isGranted(AppPermission.UnrestrictedBackground)

                        SettingsActionRow(
                            icon = painterResource(Res.drawable.timer_reset),
                            title = "Unrestricted Background",
                            trailingLabel = if (isUnrestricted) "On" else "Off",
                            onClick = {
                                permissionCoordinator.request(AppPermission.UnrestrictedBackground)
                            }
                        )
                    }
                }
            }

            item {
                SettingsGroup(title = "Data & Storage") {
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.file_down),
                        title = "Import / Export",
                        onClick = { showImportExportSheet = true }
                    )

                    if (!isDesktopPlatform) {
                        SettingsToggleRow(
                            icon = painterResource(Res.drawable.folder_sync),
                            title = "Automatic Backups",
                            isChecked = autoBackupEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (backupDirectoryUri == null) {
                                        onRequestBackupFolder()
                                    } else {
                                        viewModel.setAutoBackupEnabled(true)
                                    }
                                } else {
                                    viewModel.setAutoBackupEnabled(false)
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = autoBackupEnabled,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                            ) {
                                SettingsDivider()

                                val frequencies = listOf("Hourly", "Daily", "Weekly")
                                frequencies.forEach { freq ->
                                    SettingsSelectionRow(
                                        title = freq,
                                        isSelected = backupFrequency == freq,
                                        onClick = {
                                            viewModel.saveBackupSchedule(
                                                freq,
                                                backupTime,
                                                backupDay
                                            )
                                        }
                                    )
                                }

                                if (backupFrequency != "Hourly") {
                                    SettingsDivider()

                                    if (backupFrequency == "Weekly") {
                                        SettingsActionRow(
                                            icon = painterResource(Res.drawable.calendar_clock),
                                            title = "Backup Day",
                                            trailingLabel = backupDay,
                                            onClick = { showDayPicker = true }
                                        )
                                    }

                                    SettingsActionRow(
                                        icon = painterResource(Res.drawable.timer_reset),
                                        title = "Backup Time",
                                        trailingLabel = backupTime,
                                        onClick = { showTimePicker = true }
                                    )
                                }

                                SettingsDivider()
                                SettingsActionRow(
                                    icon = painterResource(Res.drawable.folder_input),
                                    title = "Backup Location",
                                    trailingLabel = "Change",
                                    onClick = { onRequestBackupFolder() }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsGroup(title = "Sync & Backup") {
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.refresh_cw),
                        title = "Self-Host",
                        onClick = onNavigateToSelfHostSetup
                    )
                }
            }

            item {
                SettingsGroup(title = "LAN Sync") {
                    if (isPaired) {
                        if (isDesktopPlatform) {
                            Text(
                                text = "Paired. This desktop syncs automatically whenever your phone connects over LAN.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp)
                            )
                        } else {
                            SettingsActionRow(
                                icon = painterResource(Res.drawable.refresh_cw),
                                title = "Sync Now",
                                trailingLabel = syncStatus,
                                onClick = { syncViewModel.triggerManualSync() }
                            )
                        }
                        SettingsDivider()
                        SettingsActionRow(
                            icon = rememberVectorPainter(Icons.Default.LinkOff),
                            title = if (isDesktopPlatform) "Unpair from Mobile Device" else "Unpair from Desktop",
                            isDestructive = true,
                            onClick = { showUnpairConfirmation = true }
                        )
                    } else if (isDesktopPlatform) {
                        SettingsActionRow(
                            icon = painterResource(Res.drawable.qr_code),
                            title = "Pair Mobile Device",
                            onClick = {
                                activePairingData = syncViewModel.generatePairingData()
                                showPairingDialog = true
                            }
                        )
                    } else {
                        SettingsActionRow(
                            icon = painterResource(Res.drawable.scan_line),
                            title = "Pair with Desktop",
                            onClick = { showScannerDialog = true }
                        )
                    }
                }
            }

            item {
                SettingsGroup(title = "Appearance") {
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.palette),
                        title = "Theme",
                        trailingLabel = runCatching { ThemePreference.valueOf(themePreference) }
                            .getOrDefault(ThemePreference.SYSTEM).displayName,
                        onClick = { showThemeSheet = true }
                    )
                    SettingsDivider()
                    SettingsFontSizeSliderRow(
                        fontSizePreference = fontSizePreference,
                        onFontSizeChange = { viewModel.setFontSizePreference(it) }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.palette),
                        title = "Font Style",
                        trailingLabel = runCatching { FontStylePreference.valueOf(fontStylePreference) }
                            .getOrDefault(FontStylePreference.POPPINS).displayName,
                        onClick = { showFontStyleSheet = true }
                    )

                    if (isDesktopPlatform) {
                        SettingsDivider()
                        SettingsActionRow(
                            icon = painterResource(Res.drawable.sidebar),
                            title = "Subnote Opening",
                            trailingLabel = runCatching { SubNoteOpenMode.valueOf(subNoteOpenMode) }
                                .getOrDefault(SubNoteOpenMode.SIDE_PANEL).displayName,
                            onClick = { showSubNoteOpenModeSheet = true }
                        )

                        SettingsDivider()
                        SettingsToggleRow(
                            icon = painterResource(Res.drawable.sidebar),
                            title = "Show Scrollbar",
                            isChecked = showScrollbar,
                            onCheckedChange = { viewModel.setShowScrollbar(it) }
                        )

                        Text(
                            text = "Shows scrollbars in the sidebar, editor, note lists, tables and databases. Scrolling with the wheel or trackpad works either way.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 64.dp, end = 14.dp, bottom = 14.dp)
                        )
                    }
                }
            }

            item {
                SettingsGroup(title = "AI") {
                    SettingsToggleRow(
                        icon = painterResource(Res.drawable.astroid),
                        title = "Turn Off AI Features",
                        isChecked = aiFeaturesDisabled,
                        onCheckedChange = { isChecked ->
                            if (isPurgingAiData) return@SettingsToggleRow
                            if (isChecked) {
                                showDisableAiConfirmation = true
                            } else {
                                viewModel.setAiFeaturesDisabled(false)
                            }
                        }
                    )

                    Text(
                        text = when {
                            isPurgingAiData -> "Removing models, embeddings and saved keys…"
                            aiFeaturesDisabled -> "AI is off. The assistant button is hidden, notes are no longer indexed, and all model files, embeddings and API keys have been removed. Saved chats are kept."
                            else -> "Hides the AI assistant, stops note indexing, and permanently deletes downloaded models, the search index and stored API keys to free up storage. Saved chats are kept."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 64.dp, end = 14.dp, bottom = 14.dp)
                    )
                }
            }

            item {
                SettingsGroup(title = "Need Help?") {
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.badge_question_mark),
                        title = "FAQ",
                        onClick = {}
                    )
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.badge_plus),
                        title = "What's New",
                        onClick = {}
                    )
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.shield_alert),
                        title = "Privacy Policy",
                        onClick = {}
                    )
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.info),
                        title = "About Emberr",
                        trailingLabel = "v1.0.0",
                        onClick = {}
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.07f))
                        .clickable {}
                        .padding(horizontal = 14.dp, vertical = 15.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(Res.drawable.triangle_alert),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Clear All Data",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painterResource(Res.drawable.chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
        ) {
            SettingsTopBar(onNavigateBack = onNavigateBack, hazeState = HazeState)
        }

        if (showImportExportSheet) {
            EmberrBottomSheet(
                expanded = true,
                onDismiss = { showImportExportSheet = false },
                title = "Import / Export",
                subtitle = "Backup your data securely to a local file, or restore a previous backup."
            ) { closeAnd ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmberrButtonPrimary(
                        text = if (isExporting) "Preparing..." else "Export",
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val json = viewModel.getBackupJson()
                                        showImportExportSheet = false
                                        isExporting = false
                                        onExportReady(json)
                                    } catch (e: Exception) {
                                        isExporting = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    EmberrButtonSecondary(
                        text = "Import",
                        onClick = {
                            showImportExportSheet = false
                            onImportClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    EmberrButtonPrimary(
                        text = "Close",
                        onClick = { closeAnd { showImportExportSheet = false } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        // Convert "HH:mm" into a dummy timestamp so the picker initializes at the correct time
        val timeParts = backupTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 2
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
        }

        com.ben.emberr.presentation.shared.components.MinimalTimePickerDialog(
            expanded = showTimePicker,
            initialTimestamp = cal.timeInMillis,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                val formattedTime = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                viewModel.saveBackupSchedule(backupFrequency, formattedTime, backupDay)
            }
        )
    }

    if (showDayPicker) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        var selectedIndex by remember { mutableIntStateOf(days.indexOf(backupDay).coerceAtLeast(0)) }

        val wheelContent = @Composable {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.ben.emberr.presentation.shared.components.WheelPicker(
                    items = days,
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it },
                    itemHeight = if (isDesktopPlatform) 40.dp else 44.dp
                )
            }
        }

        if (isDesktopPlatform) {
            com.ben.emberr.presentation.shared.components.EmberrDesktopMenu(
                expanded = showDayPicker,
                onDismissRequest = { showDayPicker = false }
            ) {
                Column(modifier = Modifier.width(280.dp).wrapContentHeight()) {
                    wheelContent()
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EmberrButtonSecondary(text = "Cancel", onClick = { showDayPicker = false }, modifier = Modifier.weight(1f))
                        EmberrButtonPrimary(text = "Save", onClick = { viewModel.saveBackupSchedule(backupFrequency, backupTime, days[selectedIndex]); showDayPicker = false }, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            EmberrBottomSheet(
                expanded = showDayPicker,
                onDismiss = { showDayPicker = false },
                title = "Select Backup Day"
            ) {
                wheelContent()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmberrButtonSecondary(text = "Cancel", onClick = { showDayPicker = false }, modifier = Modifier.weight(1f))
                    EmberrButtonPrimary(text = "Save", onClick = { viewModel.saveBackupSchedule(backupFrequency, backupTime, days[selectedIndex]); showDayPicker = false }, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showThemeSheet) {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = { showThemeSheet = false },
            title = "Theme",
            contentHorizontalPadding = 0.dp
        ) {
            val selectedTheme = runCatching { ThemePreference.valueOf(themePreference) }
                .getOrDefault(ThemePreference.SYSTEM)

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                ThemePreference.entries.forEachIndexed { index, option ->
                    val isSelectedTheme = option == selectedTheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelectedTheme) SelectedOptionBackground else Color.Transparent
                            )
                            .clickable {
                                viewModel.setThemePreference(option.name)
                                showThemeSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelectedTheme)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (index != ThemePreference.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }

                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { showThemeSheet = false },
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }

    if (showFontStyleSheet) {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = { showFontStyleSheet = false },
            title = "Font Style",
            contentHorizontalPadding = 0.dp
        ) {
            val selectedFontStyle = runCatching { FontStylePreference.valueOf(fontStylePreference) }
                .getOrDefault(FontStylePreference.POPPINS)

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                FontStylePreference.entries.forEachIndexed { index, option ->
                    val isSelectedFontStyle = option == selectedFontStyle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelectedFontStyle) SelectedOptionBackground else Color.Transparent
                            )
                            .clickable {
                                viewModel.setFontStylePreference(option.name)
                                showFontStyleSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.displayName,
                            fontFamily = fontFamilyFor(option),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelectedFontStyle)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (index != FontStylePreference.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }

                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { showFontStyleSheet = false },
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }

    if (showSubNoteOpenModeSheet) {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = { showSubNoteOpenModeSheet = false },
            title = "Subnote Opening",
            subtitle = "Choose how notes linked inside another note (subnotes) open on desktop.",
            contentHorizontalPadding = 0.dp
        ) {
            val selectedMode = runCatching { SubNoteOpenMode.valueOf(subNoteOpenMode) }
                .getOrDefault(SubNoteOpenMode.SIDE_PANEL)

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                SubNoteOpenMode.entries.forEachIndexed { index, option ->
                    val isSelectedMode = option == selectedMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelectedMode) SelectedOptionBackground else Color.Transparent
                            )
                            .clickable {
                                viewModel.setSubNoteOpenMode(option.name)
                                showSubNoteOpenModeSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelectedMode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (index != SubNoteOpenMode.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }

                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { showSubNoteOpenModeSheet = false },
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }

    if (showDisableAiConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisableAiConfirmation = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = "Turn Off AI Features?",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This permanently deletes the downloaded embedding and language models, the note search index, and every saved provider API key. Your notes and saved chats are untouched. Turning AI back on later means downloading and re-indexing everything again.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmberrButtonSecondary(
                        text = "Cancel",
                        onClick = { showDisableAiConfirmation = false },
                        modifier = Modifier.weight(1f)
                    )
                    EmberrButtonPrimary(
                        text = "Turn Off",
                        onClick = {
                            showDisableAiConfirmation = false
                            viewModel.setAiFeaturesDisabled(true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        )
    }

    if (showPairingDialog && activePairingData != null) {
        SyncPairingDialog(
            pairingData = activePairingData,
            onDismiss = { showPairingDialog = false }
        )
    }

    if (showScannerDialog) {
        SyncScannerDialog(
            onDismiss = { showScannerDialog = false },
            onScanned = { pairingData ->
                showScannerDialog = false
                syncViewModel.applyScannedPairing(pairingData)
            }
        )
    }

    if (showUnpairConfirmation) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirmation = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = "Unpair Device?",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (isDesktopPlatform) {
                        "This removes the LAN sync credentials stored on this desktop. Your phone won't be notified automatically - unpair from this desktop on your phone as well, or it will keep trying to reach it."
                    } else {
                        "This removes the LAN sync credentials stored on this device. Keep the desktop app open so it can be notified - if you can't, unpair from Desktop manually as well, or it will keep thinking it's still paired."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                if (isDesktopPlatform) {
                    EmberrButtonPrimary(
                        text = "Unpair",
                        onClick = {
                            showUnpairConfirmation = false
                            syncViewModel.unpair()
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EmberrButtonSecondary(
                            text = "Cancel",
                            onClick = { showUnpairConfirmation = false },
                            modifier = Modifier.weight(1f)
                        )
                        EmberrButtonPrimary(
                            text = "Unpair",
                            onClick = {
                                showUnpairConfirmation = false
                                syncViewModel.unpair()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            dismissButton = if (isDesktopPlatform) {
                {
                    EmberrButtonSecondary(
                        text = "Cancel",
                        onClick = { showUnpairConfirmation = false }
                    )
                }
            } else null
        )
    }
}

@Composable
fun SettingsSelectionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SelectedOptionBackground else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 12.dp)
            .padding(start = 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit, hazeState: HazeState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDesktopPlatform) Modifier else Modifier.stableStatusBarsPadding())
            .padding(
                top = if (isDesktopPlatform) 16.dp else 10.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        TopBarIconButton(
            icon = painterResource(Res.drawable.chevron_left),
            contentDescription = "Back",
            bgColor = Color.Transparent,
            tint = MaterialTheme.colorScheme.primary,
            hazeState = hazeState,
            hazeStyle = EmberrBlur.Regular,
            onClick = onNavigateBack
        )

        Text(
            text = "Settings",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: Painter,
    title: String,
    trailingLabel: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(contentColor.copy(alpha = if (isDestructive) 0.12f else 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = if (isDestructive) 1f else 0.75f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isDestructive) FontWeight.Medium else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )

        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (!isDestructive) {
            Icon(
                painterResource(Res.drawable.chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: Painter,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

private val FontSizeSteps = listOf(
    com.ben.emberr.ui.theme.FontSizePreference.SMALL.name to "Small",
    com.ben.emberr.ui.theme.FontSizePreference.DEFAULT.name to "Default",
    com.ben.emberr.ui.theme.FontSizePreference.LARGE.name to "Large"
)

@Composable
fun SettingsFontSizeSliderRow(
    fontSizePreference: String,
    onFontSizeChange: (String) -> Unit
) {
    val selectedIndex = FontSizeSteps.indexOfFirst { it.first == fontSizePreference }
        .coerceIn(0, FontSizeSteps.lastIndex)
    var dragPosition by remember(fontSizePreference) { mutableFloatStateOf(selectedIndex.toFloat()) }
    val displayedIndex = dragPosition.roundToInt().coerceIn(0, FontSizeSteps.lastIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(Res.drawable.palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "Font Size",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = FontSizeSteps[displayedIndex].second,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        Slider(
            value = dragPosition,
            onValueChange = { dragPosition = it },
            onValueChangeFinished = {
                val snappedIndex = dragPosition.roundToInt().coerceIn(0, FontSizeSteps.lastIndex)
                dragPosition = snappedIndex.toFloat()
                onFontSizeChange(FontSizeSteps[snappedIndex].first)
            },
            valueRange = 0f..(FontSizeSteps.lastIndex).toFloat(),
            steps = FontSizeSteps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 50.dp)
        )
    }
}