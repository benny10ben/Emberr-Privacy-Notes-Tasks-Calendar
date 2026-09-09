package com.ben.emberr.domain.ai.external

import kotlinx.serialization.Serializable

@Serializable
data class ExternalAiProviderConfig(
    val apiKey: String,
    val model: String,
    val baseUrl: String? = null,
    val updatedAt: Long = 0L
)
