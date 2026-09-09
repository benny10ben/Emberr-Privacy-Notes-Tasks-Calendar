package com.emberr.core.security

class AndroidSyncEncryptionManagerTest : SyncEncryptionManagerContract() {
    override fun createEncryptionManager(): SyncEncryptionManager = AesGcmEncryptionManager()
}
