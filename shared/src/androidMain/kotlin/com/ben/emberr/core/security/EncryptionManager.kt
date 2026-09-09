package com.ben.emberr.core.security

import android.content.Context
import java.io.File
import java.security.SecureRandom

object EncryptionManager {

    fun getDatabasePassphrase(context: Context): ByteArray {
        val applicationContext = context.applicationContext
        val secretCipher = AndroidSecretCipher(applicationContext)
        val passphraseFile = File(applicationContext.filesDir, PASSPHRASE_FILE_NAME)

        if (passphraseFile.exists()) {
            return decryptPassphraseFile(secretCipher, passphraseFile)
        }

        val freshPassphrase = ByteArray(PASSPHRASE_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        storePassphrase(secretCipher, passphraseFile, freshPassphrase)
        return freshPassphrase
    }

    private fun decryptPassphraseFile(secretCipher: AndroidSecretCipher, passphraseFile: File): ByteArray =
        try {
            secretCipher.decrypt(passphraseFile.readBytes(), PASSPHRASE_PURPOSE_LABEL)
        } catch (cause: Exception) {
            throw IllegalStateException(
                "The stored database passphrase could not be decrypted. Replacing it would leave every existing note unreadable.",
                cause
            )
        }

    private fun storePassphrase(
        secretCipher: AndroidSecretCipher,
        passphraseFile: File,
        passphrase: ByteArray
    ) {
        val partiallyWrittenFile = File(passphraseFile.parentFile, "$PASSPHRASE_FILE_NAME.tmp")
        try {
            partiallyWrittenFile.writeBytes(secretCipher.encrypt(passphrase, PASSPHRASE_PURPOSE_LABEL))
            if (!partiallyWrittenFile.renameTo(passphraseFile)) {
                throw IllegalStateException("Unable to move the database passphrase into its final location.")
            }
        } finally {
            partiallyWrittenFile.delete()
        }

        val readBackPassphrase = decryptPassphraseFile(secretCipher, passphraseFile)
        if (!readBackPassphrase.contentEquals(passphrase)) {
            throw IllegalStateException("The database passphrase did not survive being written to disk.")
        }
    }

    private const val PASSPHRASE_FILE_NAME = "emberr_db_key.bin"
    private const val PASSPHRASE_PURPOSE_LABEL = "emberr_database_passphrase"
    private const val PASSPHRASE_SIZE_BYTES = 32
}
