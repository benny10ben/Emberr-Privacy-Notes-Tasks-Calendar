package com.ben.emberr.domain.theme

import java.io.IOException
import java.util.concurrent.TimeUnit

fun resolveLinuxSystemIsDark(): Boolean? {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    if (!osName.contains("linux")) return null

    return readColorSchemeFromDesktopPortal() ?: readColorSchemeFromGnomeSettings()
}

private fun runCommandWithTimeout(vararg command: String): String? {
    return try {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        val finishedInTime = process.waitFor(1500, TimeUnit.MILLISECONDS)
        if (!finishedInTime) {
            process.destroyForcibly()
            null
        } else {
            process.inputStream.bufferedReader().readText().trim()
        }
    } catch (cause: IOException) {
        null
    }
}

private fun readColorSchemeFromDesktopPortal(): Boolean? {
    val output = runCommandWithTimeout(
        "gdbus", "call", "--session",
        "--dest", "org.freedesktop.portal.Desktop",
        "--object-path", "/org/freedesktop/portal/desktop",
        "--method", "org.freedesktop.portal.Settings.Read",
        "org.freedesktop.appearance", "color-scheme"
    ) ?: return null

    return when {
        output.contains("uint32 1") -> true
        output.contains("uint32 2") -> false
        else -> null
    }
}

private fun readColorSchemeFromGnomeSettings(): Boolean? {
    val colorScheme = runCommandWithTimeout("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
    if (colorScheme != null && colorScheme.contains("prefer-dark")) return true
    if (colorScheme != null && colorScheme.contains("prefer-light")) return false

    val gtkTheme = runCommandWithTimeout("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
        ?: return null
    return gtkTheme.lowercase().contains("dark")
}
