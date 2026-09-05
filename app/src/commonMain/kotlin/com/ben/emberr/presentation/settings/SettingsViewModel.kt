package com.ben.emberr.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.emberr.data.local.prefs.SettingsManager
import com.ben.emberr.data.worker.BackupRescheduler
import com.ben.emberr.domain.ai.AiPurgeReport
import com.ben.emberr.domain.ai.DisableAiFeaturesUseCase
import com.ben.emberr.domain.model.backup.EmberrBackupData
import com.ben.emberr.domain.repository.BackupRepository
import com.ben.emberr.domain.repository.NoteRepository
import com.ben.emberr.domain.util.SyncEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val noteRepository: NoteRepository,
    private val settingsManager: SettingsManager,
    private val backupRescheduler: BackupRescheduler,
    private val disableAiFeaturesUseCase: DisableAiFeaturesUseCase,
    private val appScope: CoroutineScope
) : ViewModel() {

    // A safe JSON parser that won't crash if future app versions add new fields
    private val safeJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val autoBackupEnabled: StateFlow<Boolean> = settingsManager.autoBackupEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val backupFrequency: StateFlow<String> = settingsManager.backupFrequencyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Daily"
    )

    val backupDirectoryUri: StateFlow<String?> = settingsManager.backupDirectoryUriFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setAutoBackupEnabled(enabled: Boolean) {
        settingsManager.saveAutoBackupEnabled(enabled)
    }

    suspend fun getBackupJson(): String {
        val backupData = backupRepository.createBackupData()
        return safeJson.encodeToString(backupData)
    }

    fun setBackupDirectory(uriString: String) {
        settingsManager.saveBackupDirectory(uriString)
    }

    val backupTime: StateFlow<String> = settingsManager.backupTimeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "02:00"
    )

    val backupDay: StateFlow<String> = settingsManager.backupDayFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Sunday"
    )

    fun saveBackupSchedule(frequency: String, time: String, day: String) {
        settingsManager.saveBackupFrequency(frequency)
        settingsManager.saveBackupTime(time)
        settingsManager.saveBackupDay(day)
        backupRescheduler.rescheduleNow(frequency, time, day)
    }

    val themePreference: StateFlow<String> = settingsManager.themePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.ben.emberr.data.local.prefs.SyncConstants.DEFAULT_THEME_PREFERENCE
    )

    fun setThemePreference(preference: String) {
        settingsManager.saveThemePreference(preference)
    }

    val fontSizePreference: StateFlow<String> = settingsManager.fontSizePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.ben.emberr.data.local.prefs.SyncConstants.DEFAULT_FONT_SIZE_PREFERENCE
    )

    fun setFontSizePreference(preference: String) {
        settingsManager.saveFontSizePreference(preference)
    }

    val fontStylePreference: StateFlow<String> = settingsManager.fontStylePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.ben.emberr.data.local.prefs.SyncConstants.DEFAULT_FONT_STYLE_PREFERENCE
    )

    fun setFontStylePreference(preference: String) {
        settingsManager.saveFontStylePreference(preference)
    }

    val subNoteOpenMode: StateFlow<String> = settingsManager.subNoteOpenModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.ben.emberr.data.local.prefs.SyncConstants.DEFAULT_SUBNOTE_OPEN_MODE
    )

    fun setSubNoteOpenMode(mode: String) {
        settingsManager.saveSubNoteOpenMode(mode)
    }

    val showScrollbar: StateFlow<Boolean> = settingsManager.showScrollbarFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isShowScrollbarEnabled()
    )

    fun setShowScrollbar(enabled: Boolean) {
        settingsManager.saveShowScrollbar(enabled)
    }

    val aiFeaturesDisabled: StateFlow<Boolean> = settingsManager.aiFeaturesDisabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isAiFeaturesDisabled()
    )

    private val _isPurgingAiData = MutableStateFlow(false)
    val isPurgingAiData: StateFlow<Boolean> = _isPurgingAiData.asStateFlow()

    private val _aiPurgeResultMessage = MutableStateFlow<String?>(null)
    val aiPurgeResultMessage: StateFlow<String?> = _aiPurgeResultMessage.asStateFlow()

    fun consumeAiPurgeResultMessage() {
        _aiPurgeResultMessage.value = null
    }

    fun setAiFeaturesDisabled(disabled: Boolean) {
        if (!disabled) {
            disableAiFeaturesUseCase.enableAiFeatures()
            return
        }
        if (_isPurgingAiData.value) return

        _isPurgingAiData.value = true
        appScope.launch {
            try {
                val report = disableAiFeaturesUseCase.execute()
                _aiPurgeResultMessage.value = describePurgeResult(report)
            } catch (e: Exception) {
                _aiPurgeResultMessage.value = "Couldn't finish removing AI data: ${e.message}"
            } finally {
                _isPurgingAiData.value = false
            }
        }
    }

    private fun describePurgeResult(report: AiPurgeReport): String {
        val blockedPaths = report.undeletablePaths + report.survivingPaths
        return when {
            blockedPaths.isNotEmpty() ->
                "Freed ${formatByteSize(report.bytesFreed)}, but ${blockedPaths.size} file(s) could not be removed: " +
                    blockedPaths.joinToString(", ") { it.substringAfterLast('/') }
            report.bytesFreed > 0L -> "Freed ${formatByteSize(report.bytesFreed)} of model files."
            else -> "No model files were on disk to remove."
        }
    }

    private fun formatByteSize(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> "${(bytes / 100_000_000L) / 10.0} GB"
        bytes >= 1_000_000L -> "${bytes / 1_000_000L} MB"
        bytes >= 1_000L -> "${bytes / 1_000L} KB"
        else -> "$bytes B"
    }

    /**
     * Parses the JSON backup and merges it into the local database.
     */
    suspend fun mergeBackupJson(jsonString: String) {
        val backupData = safeJson.decodeFromString<EmberrBackupData>(jsonString)

        // FUTURE MIGRATION CHECK:
        // If backupData.version > 1, pass it through a migration mapper here
        // before handing it to the repository to ensure old data structures
        // are correctly converted to the newest schema.

        /** val migratedData = when (backupData.version) {
        1 -> runMigrationV1toV2(backupData) // Manually re-map the old structure to the new one
        2 -> backupData
        else -> backupData
        } */

        backupRepository.restoreBackup(backupData)

        // Wipe the stale memory caches so the next read hits the raw Room DB!
        noteRepository.clearCaches()

        // Tell the UI an import just finished so it can reload immediately
        kotlinx.coroutines.delay(100.milliseconds) // Brief pause to ensure DB transactions settle
        SyncEventBus.emitSyncCompleted("import_complete")
    }
}