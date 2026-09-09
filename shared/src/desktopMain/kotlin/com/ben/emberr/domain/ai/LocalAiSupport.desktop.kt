package com.ben.emberr.domain.ai

private const val BUNDLED_OPERATING_SYSTEM_KEYWORD = "linux"

private val BUNDLED_PROCESSOR_NAMES = setOf("amd64", "x86_64", "x86-64")

actual fun detectLocalAiSupport(): LocalAiSupport {
    val operatingSystemName = System.getProperty("os.name").orEmpty().lowercase()
    val processorName = System.getProperty("os.arch").orEmpty().lowercase()

    return when {
        !operatingSystemName.contains(BUNDLED_OPERATING_SYSTEM_KEYWORD) -> LocalAiSupport.Unsupported(
            "The on-device AI engine is only bundled for Linux desktops. Connect a cloud provider with your own API key instead."
        )

        processorName !in BUNDLED_PROCESSOR_NAMES -> LocalAiSupport.Unsupported(
            "The on-device AI engine needs a 64-bit Intel or AMD processor. This computer reports \"$processorName\", which has no bundled engine. Connect a cloud provider with your own API key instead."
        )

        else -> LocalAiSupport.Supported
    }
}
