package com.ben.emberr.presentation.sync

import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID

actual fun getLocalNetworkIp(): String {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: continue
                    if (sAddr.contains('.')) {
                        return sAddr
                    }
                }
            }
        }
    } catch (e: Exception) {
        return "127.0.0.1"
    }
    return "127.0.0.1"
}

actual fun generateSecureToken(): String {
    return UUID.randomUUID().toString()
}