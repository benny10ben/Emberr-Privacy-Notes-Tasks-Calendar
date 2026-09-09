package com.emberr.domain.util

object LocalNetworkHostValidator {

    fun isLocalNetworkHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        if (host.contains(':')) return isLocalIpv6Host(host)

        val octets = host.split(".").map { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it == null || it !in 0..255 }) return false
        val (first, second) = octets[0]!! to octets[1]!!

        return when {
            first == 127 -> true
            first == 10 -> true
            first == 192 && second == 168 -> true
            first == 172 && second in 16..31 -> true
            else -> false
        }
    }

    private fun isLocalIpv6Host(host: String): Boolean {
        val groups = expandIpv6Groups(host) ?: return false

        val isLoopback = groups.subList(0, 7).all { it == 0 } && groups[7] == 1
        val isLinkLocal = groups[0] in 0xFE80..0xFEBF
        val isUniqueLocal = groups[0] in 0xFC00..0xFDFF

        return isLoopback || isLinkLocal || isUniqueLocal
    }

    private fun expandIpv6Groups(host: String): List<Int>? {
        val address = host.removePrefix("[").removeSuffix("]")

        val compressionParts = address.split("::")
        if (compressionParts.size > 2) return null
        val hasCompression = compressionParts.size == 2

        val headGroups = compressionParts[0].split(":").filter { it.isNotEmpty() }
        val tailGroups = if (hasCompression) compressionParts[1].split(":").filter { it.isNotEmpty() } else emptyList()

        if (!hasCompression && headGroups.size != 8) return null
        val missingGroupCount = 8 - headGroups.size - tailGroups.size
        if (hasCompression && missingGroupCount < 0) return null

        val allGroups = headGroups + List(if (hasCompression) missingGroupCount else 0) { "0" } + tailGroups
        if (allGroups.size != 8) return null

        return allGroups.map { group -> group.toIntOrNull(16)?.takeIf { it in 0..0xFFFF } ?: return null }
    }
}
