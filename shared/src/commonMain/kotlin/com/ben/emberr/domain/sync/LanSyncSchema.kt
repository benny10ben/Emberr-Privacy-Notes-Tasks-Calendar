package com.ben.emberr.domain.sync

const val LAN_SYNC_SCHEMA_VERSION = 1

const val OLDEST_SUPPORTED_LAN_SYNC_SCHEMA_VERSION = 1

fun isSupportedLanSyncSchemaVersion(peerSchemaVersion: Int): Boolean =
    peerSchemaVersion in OLDEST_SUPPORTED_LAN_SYNC_SCHEMA_VERSION..LAN_SYNC_SCHEMA_VERSION

class LanSyncSchemaMismatchException(message: String) : Exception(message)
