package com.ben.emberr.domain.sync

import com.ben.emberr.domain.model.DatabaseBlock
import com.ben.emberr.domain.model.NoteBlock
import com.ben.emberr.domain.model.NoteContent

object NoteMergeHelper {

    fun mergeNoteContent(
        localContent: NoteContent?,
        localUpdatedAt: Long,
        remoteContent: NoteContent,
        remoteUpdatedAt: Long
    ): NoteContent {
        if (localContent == null) return remoteContent

        // Determines base content using last-write-wins (strictly greater timestamp).
        // On exact timestamp ties, local content is preserved.
        val remoteWins = remoteUpdatedAt > localUpdatedAt
        val baseContent  = if (remoteWins) remoteContent else localContent
        val otherContent = if (remoteWins) localContent  else remoteContent
        val baseIds = baseContent.blocks.mapTo(HashSet()) { it.id }
        val otherById = otherContent.blocks.associateBy { it.id }
        val mergedTree = rebuildTree(baseContent.blocks, otherById)
        val otherOnly = otherContent.blocks.filter { it.id !in baseIds }

        val result = mergedTree.toMutableList()
        if (otherOnly.isNotEmpty()) {
            val otherOrder = otherContent.blocks.map { it.id }
            otherOnly.forEach { block ->
                val idx = otherOrder.indexOf(block.id)
                val precedingId = otherOrder.subList(0, idx)
                    .lastOrNull { id -> result.any { it.id == id } }
                val insertAfter = if (precedingId != null) {
                    result.indexOfFirst { it.id == precedingId }
                } else -1
                result.add(insertAfter + 1, block)
            }
        }

        return NoteContent(blocks = result.distinctBy { it.id })
    }

    /**
     * Rebuilds the block tree:
     *  - DatabaseBlock: performs field-level merging with its corresponding twin.
     *  - Other blocks: applies pure last-write-wins based on updatedAt timestamps.
     */
    private fun rebuildTree(
        baseBlocks: List<NoteBlock>,
        otherById: Map<String, NoteBlock>
    ): List<NoteBlock> = baseBlocks.map { baseBlock ->
        when (baseBlock) {
            is DatabaseBlock -> {
                val twin = otherById[baseBlock.id] as? DatabaseBlock
                mergeDatabase(twin, baseBlock)
            }
            else -> {
                val other = otherById[baseBlock.id]
                when {
                    other == null -> baseBlock
                    other.updatedAt > baseBlock.updatedAt -> other
                    else -> baseBlock
                }
            }
        }
    }

    private fun mergeDatabase(
        localBlock: DatabaseBlock?,
        remoteBlock: DatabaseBlock
    ): DatabaseBlock {
        if (localBlock == null) return remoteBlock

        val remoteBlockWins = remoteBlock.updatedAt > localBlock.updatedAt

        val localColMap  = localBlock.columns.associateBy  { it.id }
        val remoteColMap = remoteBlock.columns.associateBy { it.id }
        // Orders columns according to the winning block, preserving additional columns from the losing side.
        val baseColumns  = if (remoteBlockWins) remoteBlock.columns else localBlock.columns
        val otherColumns = if (remoteBlockWins) localBlock.columns  else remoteBlock.columns
        val allColIds    = (baseColumns.map { it.id } + otherColumns.map { it.id }).distinct()

        val mergedColumns = allColIds.mapNotNull { id ->
            val localCol  = localColMap[id]
            val remoteCol = remoteColMap[id]
            when {
                localCol != null && remoteCol != null -> {
                    val winnerCol = if (remoteCol.updatedAt > localCol.updatedAt) remoteCol else localCol
                    val loserCol  = if (remoteCol.updatedAt > localCol.updatedAt) localCol else remoteCol
                    winnerCol.copy(
                        isDeleted       = localCol.isDeleted || remoteCol.isDeleted,
                        aggregationType = winnerCol.aggregationType ?: loserCol.aggregationType,
                        currencySymbol  = winnerCol.currencySymbol  ?: loserCol.currencySymbol,
                        isFormulaCurrency = winnerCol.isFormulaCurrency || loserCol.isFormulaCurrency
                    )
                }
                else -> localCol ?: remoteCol
            }
        }.toMutableList()

        val localRowMap  = localBlock.rows.associateBy  { it.id }
        val remoteRowMap = remoteBlock.rows.associateBy { it.id }
        // Orders rows according to the winning block.
        val baseRows     = if (remoteBlockWins) remoteBlock.rows else localBlock.rows
        val otherRows    = if (remoteBlockWins) localBlock.rows  else remoteBlock.rows
        val allRowIds    = (baseRows.map { it.id } + otherRows.map { it.id }).distinct()

        val mergedRows = allRowIds.mapNotNull { id ->
            val localRow  = localRowMap[id]
            val remoteRow = remoteRowMap[id]
            when {
                localRow != null && remoteRow != null -> {
                    val winnerRow = if (remoteRow.updatedAt > localRow.updatedAt) remoteRow else localRow
                    val mergedCells = if (remoteRow.updatedAt > localRow.updatedAt) {
                        localRow.cells + remoteRow.cells
                    } else {
                        remoteRow.cells + localRow.cells
                    }
                    winnerRow.copy(
                        cells    = mergedCells,
                        isDeleted = localRow.isDeleted || remoteRow.isDeleted
                    )
                }
                else -> localRow ?: remoteRow
            }
        }

        val winnerBlock = if (remoteBlockWins) remoteBlock else localBlock
        return winnerBlock.copy(
            columns   = mergedColumns,
            rows      = mergedRows,
            updatedAt = maxOf(localBlock.updatedAt, remoteBlock.updatedAt)
        )
    }
}