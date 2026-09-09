package com.ben.emberr.domain.selfhost.sync

import kotlinx.serialization.Serializable

@Serializable
data class ApiConfigSyncEntry(
    val provider: String,
    val apiKey: String,
    val model: String,
    val baseUrl: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
