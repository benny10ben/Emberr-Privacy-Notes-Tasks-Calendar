// Common-code contract for running vault tool calls and applying a confirmed write.

package com.emberr.domain.ai.tools

interface VaultToolRunner {
    suspend fun run(toolName: String, arguments: Map<String, String>): VaultToolResult
    suspend fun applyPendingWrite(write: VaultPendingWrite): VaultToolResult
}
