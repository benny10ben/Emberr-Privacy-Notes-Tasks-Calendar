package com.ben.emberr.presentation.sync

import java.util.UUID

import java.net.NetworkInterface

actual fun getLocalNetworkIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val name = networkInterface.name.lowercase()
            if (name.startsWith("docker") || name.startsWith("br-") || name.startsWith("vnet") || name.startsWith("virbr")) {
                continue
            }

            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address.isSiteLocalAddress && !address.hostAddress.contains(":")) {
                    return address.hostAddress
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "127.0.0.1" // Fallback
}

actual fun generateSecureToken(): String {
    return UUID.randomUUID().toString()
}