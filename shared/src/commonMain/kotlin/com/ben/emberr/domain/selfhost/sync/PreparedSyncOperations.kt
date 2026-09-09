package com.ben.emberr.domain.selfhost.sync

import com.ben.emberr.data.local.room.NoteBlockEntity
import com.ben.emberr.data.local.room.NoteMetadataEntity
import com.ben.emberr.domain.selfhost.translation.BlockTombstone
import com.ben.emberr.domain.selfhost.translation.EmbeddedBlockPayload

data class PreparedSyncOperations(
    val metadataUpsert: NoteMetadataEntity,
    val blockUpserts: List<NoteBlockEntity>,
    val blockDeletions: List<BlockTombstone>,
    val embeddedBlocks: List<EmbeddedBlockPayload> = emptyList()
)
