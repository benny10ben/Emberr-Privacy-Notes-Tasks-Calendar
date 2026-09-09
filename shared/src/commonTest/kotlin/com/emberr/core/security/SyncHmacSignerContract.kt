package com.emberr.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

abstract class SyncHmacSignerContract {

    abstract fun createSigner(): SyncHmacSigner

    private val secret = "pairing-secret"

    @Test
    fun aSignatureMatchesTheOneTheOtherPlatformWouldProduce() {
        val signer = createSigner()

        assertEquals(
            CryptoGoldenFixtures.EXPECTED_SIGNATURE,
            signer.sign(
                path = CryptoGoldenFixtures.SIGNED_PATH,
                timestampMillis = CryptoGoldenFixtures.SIGNED_TIMESTAMP_MILLIS,
                secretKey = CryptoGoldenFixtures.SIGNING_SECRET
            )
        )
    }

    @Test
    fun signingTheSameRequestTwiceGivesTheSameSignature() {
        val signer = createSigner()

        assertEquals(
            signer.sign("/sync/notes", 1_000L, secret),
            signer.sign("/sync/notes", 1_000L, secret)
        )
    }

    @Test
    fun aSignatureIsAlwaysSixtyFourLowercaseHexCharacters() {
        val signer = createSigner()

        val signature = signer.sign("/sync/notes", 1_000L, secret)

        assertEquals(64, signature.length)
        assertTrue(signature.all { it in "0123456789abcdef" }, "not lowercase hex: $signature")
    }

    @Test
    fun aDifferentPathProducesADifferentSignature() {
        val signer = createSigner()

        assertNotEquals(
            signer.sign("/sync/notes", 1_000L, secret),
            signer.sign("/sync/daily", 1_000L, secret)
        )
    }

    @Test
    fun aDifferentTimestampProducesADifferentSignature() {
        val signer = createSigner()

        assertNotEquals(
            signer.sign("/sync/notes", 1_000L, secret),
            signer.sign("/sync/notes", 1_001L, secret)
        )
    }

    @Test
    fun aDifferentSecretProducesADifferentSignature() {
        val signer = createSigner()

        assertNotEquals(
            signer.sign("/sync/notes", 1_000L, secret),
            signer.sign("/sync/notes", 1_000L, "a-different-secret")
        )
    }

    @Test
    fun anySecretLengthIsAcceptedBecauseItIsHashedFirst() {
        val signer = createSigner()

        assertEquals(64, signer.sign("/sync/notes", 1_000L, "").length)
        assertEquals(64, signer.sign("/sync/notes", 1_000L, "x").length)
        assertEquals(64, signer.sign("/sync/notes", 1_000L, "y".repeat(500)).length)
    }

    @Test
    fun aPathContainingAColonStillProducesItsOwnSignature() {
        val signer = createSigner()

        assertNotEquals(
            signer.sign("/a", 12L, secret),
            signer.sign("/a:1", 2L, secret)
        )
    }
}
