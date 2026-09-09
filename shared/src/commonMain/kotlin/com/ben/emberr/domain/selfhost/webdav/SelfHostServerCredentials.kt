package com.ben.emberr.domain.selfhost.webdav

import kotlinx.serialization.Serializable

@Serializable
data class SelfHostServerCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)