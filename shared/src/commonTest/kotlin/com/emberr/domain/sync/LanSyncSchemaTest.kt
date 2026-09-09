package com.emberr.domain.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanSyncSchemaTest {

    @Test
    fun ourOwnSchemaVersionIsAlwaysSupported() {
        assertTrue(isSupportedLanSyncSchemaVersion(LAN_SYNC_SCHEMA_VERSION))
    }

    @Test
    fun theOldestSchemaVersionWeStillSpeakIsSupported() {
        assertTrue(isSupportedLanSyncSchemaVersion(OLDEST_SUPPORTED_LAN_SYNC_SCHEMA_VERSION))
    }

    @Test
    fun aPeerOlderThanOurOldestSupportedVersionIsRefused() {
        assertFalse(isSupportedLanSyncSchemaVersion(OLDEST_SUPPORTED_LAN_SYNC_SCHEMA_VERSION - 1))
        assertFalse(isSupportedLanSyncSchemaVersion(0))
        assertFalse(isSupportedLanSyncSchemaVersion(-1))
    }

    @Test
    fun aPeerNewerThanUsIsRefusedBecauseWeCannotReadItYet() {
        assertFalse(isSupportedLanSyncSchemaVersion(LAN_SYNC_SCHEMA_VERSION + 1))
        assertFalse(isSupportedLanSyncSchemaVersion(99))
    }

    @Test
    fun theSupportedRangeNeverRunsBackwards() {
        assertTrue(OLDEST_SUPPORTED_LAN_SYNC_SCHEMA_VERSION <= LAN_SYNC_SCHEMA_VERSION)
    }
}
