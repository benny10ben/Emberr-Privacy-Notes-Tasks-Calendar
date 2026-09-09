package com.emberr.core.security

class AndroidSyncHmacSignerTest : SyncHmacSignerContract() {
    override fun createSigner(): SyncHmacSigner = HmacSha256Signer()
}
