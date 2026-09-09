package com.ben.emberr.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "self_host_deleted_api_configs")
data class SelfHostDeletedApiConfigEntity(
    @PrimaryKey val provider: String,
    val deletedAt: Long
)
