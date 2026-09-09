package com.ben.emberr.domain.ai.models

expect fun softDeleteModelFile(fileName: String): Boolean
expect fun restoreModelFile(fileName: String): Boolean
expect fun hasPendingModelDeletion(fileName: String): Boolean
expect fun cleanupPendingModelDeletions()

data class ModelPurgeOutcome(
    val bytesFreed: Long,
    val undeletablePaths: List<String>,
    val survivingPaths: List<String>
)

expect fun purgeModelFileCompletely(fileName: String): ModelPurgeOutcome
