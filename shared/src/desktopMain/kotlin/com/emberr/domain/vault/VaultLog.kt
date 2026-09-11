// Console logging for everything the vault mirror does.

package com.emberr.domain.vault

object VaultLog {

    private const val LOG_TAG = "EmberrVault"

    fun d(message: String) {
        println("$LOG_TAG: $message")
    }

    fun e(message: String) {
        System.err.println("$LOG_TAG: $message")
    }
}
