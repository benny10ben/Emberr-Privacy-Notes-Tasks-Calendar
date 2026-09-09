package com.ben.emberr.domain.sync

import com.ben.emberr.domain.selfhost.sync.SelfHostSyncEngine

// Coordinates media download retries across both LAN sync and self-host (WebDAV) sync mechanisms.
// Triggers retries for both engines so the UI doesn't need to track which mechanism owns a file.
class MediaRetryCoordinator(
    private val syncRepository: SyncRepository,
    private val selfHostSyncEngine: SelfHostSyncEngine
) {
    fun retryMediaDownload(fileName: String) {
        syncRepository.retryMediaDownload(fileName)
        selfHostSyncEngine.retryMediaDownload(fileName)
    }
}