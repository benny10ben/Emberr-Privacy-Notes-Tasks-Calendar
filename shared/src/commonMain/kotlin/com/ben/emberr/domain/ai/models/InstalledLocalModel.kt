package com.ben.emberr.domain.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class InstalledLocalModel(
    val fileName: String,
    val displayName: String,
    val isBundledDefault: Boolean,
    val installedAt: Long
)
