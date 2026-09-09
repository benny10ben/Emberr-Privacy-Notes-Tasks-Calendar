package com.emberr.core.security

class DesktopSyncHmacSignerTest : SyncHmacSignerContract() {
    override fun createSigner(): SyncHmacSigner = HmacSha256Signer()
}
