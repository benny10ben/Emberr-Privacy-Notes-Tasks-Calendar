// Proves a store really works by writing, reading back and deleting a throwaway value.
package com.emberr.core.security.secrets

import java.security.SecureRandom
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SecretBackendProbe(private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS) {

    private val randomSource = SecureRandom()

    fun isBackendUsable(backend: SecretBackend): Boolean = withTimeout(false) {
        val canaryValue = freshCanaryValue()
        backend.writeSecret(PROBE_SERVICE, PROBE_ACCOUNT, canaryValue)
        val valueReadBack = backend.readSecret(PROBE_SERVICE, PROBE_ACCOUNT)
        runCatching { backend.removeSecret(PROBE_SERVICE, PROBE_ACCOUNT) }
        valueReadBack == canaryValue
    }

    fun <T> withTimeout(valueOnFailure: T, action: () -> T): T {
        val probeExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, PROBE_THREAD_NAME).apply { isDaemon = true }
        }
        return try {
            probeExecutor.submit(Callable { action() }).get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (cause: Exception) {
            valueOnFailure
        } finally {
            probeExecutor.shutdownNow()
        }
    }

    private fun freshCanaryValue(): String {
        val randomBytes = ByteArray(CANARY_BYTE_COUNT)
        randomSource.nextBytes(randomBytes)
        return randomBytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 5L
        const val PROBE_SERVICE = "EmberrKeyringProbe"
        const val PROBE_ACCOUNT = "availability_canary"
        const val PROBE_THREAD_NAME = "emberr-secret-backend-probe"
        const val CANARY_BYTE_COUNT = 16
    }
}
