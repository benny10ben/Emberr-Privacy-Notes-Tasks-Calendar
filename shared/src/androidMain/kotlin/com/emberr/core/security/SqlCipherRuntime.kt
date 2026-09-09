package com.emberr.core.security

import net.zetetic.database.sqlcipher.SQLiteGlobal

object SqlCipherRuntime {

    private const val maxDatabaseConnections = 2

    private var isNativeLibraryReady = false

    @Synchronized
    fun loadNativeLibraryAndLimitConnections() {
        if (isNativeLibraryReady) return
        System.loadLibrary("sqlcipher")
        SQLiteGlobal.setWALConnectionPoolSize(maxDatabaseConnections)
        isNativeLibraryReady = true
    }

    fun asRawKey(passphrase: ByteArray): ByteArray {
        val hexKey = passphrase.joinToString("") { keyByte -> "%02x".format(keyByte.toInt() and 0xFF) }
        return "x'$hexKey'".encodeToByteArray()
    }
}
