package com.emberr.domain.selfhost.crypto

import com.emberr.domain.selfhost.webdav.SelfHostServerCredentials

expect class SecureSyncKeyStorage {
    fun saveEncryptionKey(key: ByteArray)
    fun getEncryptionKey(): ByteArray?
    fun saveServerCredentials(credentials: SelfHostServerCredentials)
    fun getServerCredentials(): SelfHostServerCredentials?
    fun clearAll()
}