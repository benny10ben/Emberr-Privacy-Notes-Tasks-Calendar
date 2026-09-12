package com.emberr.domain.ai.chat

import com.emberr.domain.ai.tools.VaultPendingWrite
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val pendingVaultWrite: VaultPendingWrite? = null,
    val toolCallSummary: String? = null
)
