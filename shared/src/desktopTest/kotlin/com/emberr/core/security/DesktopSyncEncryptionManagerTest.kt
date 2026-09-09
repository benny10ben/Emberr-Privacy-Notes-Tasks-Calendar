package com.emberr.core.security

class DesktopSyncEncryptionManagerTest : SyncEncryptionManagerContract() {
    override fun createEncryptionManager(): SyncEncryptionManager = AesGcmEncryptionManager()
}
