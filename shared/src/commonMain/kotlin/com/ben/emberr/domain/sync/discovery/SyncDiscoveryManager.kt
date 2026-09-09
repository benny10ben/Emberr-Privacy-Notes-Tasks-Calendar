package com.ben.emberr.domain.sync.discovery

import kotlinx.coroutines.flow.StateFlow

data class DiscoveredDevice(
    val deviceName: String,
    val ipAddress: String,
    val port: Int
)

interface SyncDiscoveryManager {
    // For the Phone: A flow of laptops it finds on the Wi-Fi
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>

    // For the Laptop: Start shouting your existence to the network
    fun startBroadcasting(port: Int, deviceName: String)
    fun stopBroadcasting()

    // For the Phone: Start listening for laptops
    fun startScanning()
    fun stopScanning()
}