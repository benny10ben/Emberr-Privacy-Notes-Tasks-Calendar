package com.ben.emberr.domain.sync

import com.ben.emberr.data.local.prefs.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncPairingState(private val settingsManager: SettingsManager) {
    private val _isPaired = MutableStateFlow(settingsManager.isSyncPairingConfirmed())
    val isPaired = _isPaired.asStateFlow()

    fun markPaired() {
        settingsManager.saveSyncPairingConfirmed(true)
        _isPaired.value = true
    }

    fun unpairLocally() {
        settingsManager.clearSyncPairing()
        _isPaired.value = false
    }
}
