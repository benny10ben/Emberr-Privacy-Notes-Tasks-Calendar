package com.emberr.domain.util

import java.awt.Desktop
import java.net.URI
import java.util.concurrent.TimeUnit

private const val SECONDS_TO_WAIT_FOR_A_LAUNCHER = 6L

private val BundledRuntimeVariablesThatBreakOtherApps = listOf(
    "LD_LIBRARY_PATH",
    "LD_PRELOAD",
    "DYLD_LIBRARY_PATH",
    "GTK_PATH",
    "GTK_DATA_PREFIX",
    "GTK_EXE_PREFIX",
    "GTK_IM_MODULE_FILE",
    "GIO_MODULE_DIR",
    "GDK_PIXBUF_MODULE_FILE",
    "GSETTINGS_SCHEMA_DIR",
    "JAVA_HOME",
    "_JAVA_OPTIONS"
)

private fun launchersFor(url: String): List<List<String>> {
    val operatingSystem = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        operatingSystem.contains("win") -> listOf(
            listOf("rundll32", "url.dll,FileProtocolHandler", url)
        )
        operatingSystem.contains("mac") -> listOf(
            listOf("open", url)
        )
        else -> listOf(
            listOf("gio", "open", url),
            listOf("xdg-open", url),
            listOf("x-www-browser", url)
        )
    }
}

private fun runLauncher(command: List<String>): Boolean = try {
    val builder = ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)

    builder.environment().keys.removeAll(BundledRuntimeVariablesThatBreakOtherApps.toSet())

    val process = builder.start()
    val finishedInTime = process.waitFor(SECONDS_TO_WAIT_FOR_A_LAUNCHER, TimeUnit.SECONDS)
    if (finishedInTime) process.exitValue() == 0 else true
} catch (_: Exception) {
    false
}

private fun openWithFirstWorkingLauncher(url: String) {
    if (launchersFor(url).any { runLauncher(it) }) return

    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (_: Exception) {
        showNativeToast("could not open link")
    }
}

actual fun openLinkInRunningBrowser(url: String): Boolean {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false

    Thread { openWithFirstWorkingLauncher(url) }
        .apply {
            isDaemon = true
            name = "emberr-link-launcher"
        }
        .start()

    return true
}
