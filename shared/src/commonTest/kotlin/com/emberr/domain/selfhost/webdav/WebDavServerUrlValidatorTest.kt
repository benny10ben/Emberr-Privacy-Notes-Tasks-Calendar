package com.emberr.domain.selfhost.webdav

import kotlin.test.Test
import kotlin.test.assertFailsWith

class WebDavServerUrlValidatorTest {

    private fun assertAccepted(serverUrl: String) {
        WebDavServerUrlValidator.validate(serverUrl)
    }

    private fun assertRefused(serverUrl: String) {
        assertFailsWith<WebDavConfigurationException>("$serverUrl should have been refused") {
            WebDavServerUrlValidator.validate(serverUrl)
        }
    }

    @Test
    fun secureAddressesAreAlwaysAccepted() {
        assertAccepted("https://example.com")
        assertAccepted("https://cloud.example.com/remote.php/dav")
        assertAccepted("https://example.com:8443/dav")
        assertAccepted("https://192.168.1.5:8443")
    }

    @Test
    fun plainHttpIsAcceptedOnTheLocalNetwork() {
        assertAccepted("http://localhost:8080")
        assertAccepted("http://127.0.0.1")
        assertAccepted("http://192.168.1.5:8080/dav")
        assertAccepted("http://10.0.0.5")
        assertAccepted("http://172.16.0.1")
    }

    @Test
    fun plainHttpIsRefusedForAnythingOnThePublicInternet() {
        assertRefused("http://example.com")
        assertRefused("http://cloud.example.com/remote.php/dav")
        assertRefused("http://8.8.8.8")
        assertRefused("http://172.32.0.1")
        assertRefused("http://192.169.0.1")
    }

    @Test
    fun theRefusalMessageExplainsWhyPlainHttpWasRejected() {
        val failure = assertFailsWith<WebDavConfigurationException> {
            WebDavServerUrlValidator.validate("http://example.com")
        }

        assertContainsAll(failure.message.orEmpty(), "https://", "http://", "local network")
    }

    @Test
    fun theValidatorAgreesWithTheSharedLocalNetworkCheck() {
        assertAccepted("http://192.168.1.5")
        assertRefused("http://192.169.1.5")
    }

    private fun assertContainsAll(text: String, vararg expectedFragments: String) {
        expectedFragments.forEach { fragment ->
            kotlin.test.assertTrue(fragment in text, "'$fragment' missing from: $text")
        }
    }
}
