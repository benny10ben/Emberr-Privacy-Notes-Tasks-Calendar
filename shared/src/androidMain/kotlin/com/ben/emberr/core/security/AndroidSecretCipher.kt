package com.ben.emberr.core.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class AndroidSecretCipher(applicationContext: Context) {

    private val aead: Aead

    init {
        AeadConfig.register()
        aead = AndroidKeysetManager.Builder()
            .withSharedPref(applicationContext, KEYSET_NAME, KEYSET_STORAGE_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get(AES_256_GCM_TEMPLATE_NAME))
            .withMasterKeyUri(ANDROID_KEYSTORE_MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    fun encrypt(plainBytes: ByteArray, purposeLabel: String): ByteArray =
        aead.encrypt(plainBytes, purposeLabel.encodeToByteArray())

    fun decrypt(encryptedBytes: ByteArray, purposeLabel: String): ByteArray =
        aead.decrypt(encryptedBytes, purposeLabel.encodeToByteArray())

    private companion object {
        const val KEYSET_NAME = "emberr_secret_keyset"
        const val KEYSET_STORAGE_FILE_NAME = "emberr_tink_keyset"
        const val AES_256_GCM_TEMPLATE_NAME = "AES256_GCM"
        const val ANDROID_KEYSTORE_MASTER_KEY_URI = "android-keystore://emberr_tink_master_key"
    }
}
