package com.ben.emberr.domain.ai

import android.os.Build

private val BUNDLED_ANDROID_ABIS = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

actual fun detectLocalAiSupport(): LocalAiSupport {
    val deviceAbis = Build.SUPPORTED_ABIS?.toList().orEmpty()

    if (deviceAbis.any { it in BUNDLED_ANDROID_ABIS }) return LocalAiSupport.Supported

    val reportedAbi = deviceAbis.firstOrNull() ?: "unknown"
    return LocalAiSupport.Unsupported(
        "The on-device AI engine has no build for this device's processor ($reportedAbi). Connect a cloud provider with your own API key instead."
    )
}
