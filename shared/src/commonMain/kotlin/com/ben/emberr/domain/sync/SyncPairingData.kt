package com.ben.emberr.domain.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncPairingData(
    val ipAddress: String,
    val port: Int,
    val authToken: String,
    val encryptionKey: String,

)