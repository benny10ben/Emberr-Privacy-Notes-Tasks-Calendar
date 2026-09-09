package com.ben.emberr.domain.selfhost.crypto

interface KeyDerivationManager {
    fun deriveAesKey(passphrase: CharArray, salt: ByteArray): ByteArray
    fun generateSalt(): ByteArray
    fun generatePassphrase(): String
}