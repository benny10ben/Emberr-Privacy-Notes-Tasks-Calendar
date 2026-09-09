package com.ben.emberr.domain.ai.external

expect class SecureAiKeyStorage {
    fun getConfig(provider: ExternalAiProvider): ExternalAiProviderConfig?
    fun saveConfig(provider: ExternalAiProvider, config: ExternalAiProviderConfig)
    fun clearConfig(provider: ExternalAiProvider)
    fun clearAll()
}
