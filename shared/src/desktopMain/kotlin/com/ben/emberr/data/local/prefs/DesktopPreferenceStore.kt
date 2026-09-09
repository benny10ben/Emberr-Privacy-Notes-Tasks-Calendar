package com.ben.emberr.data.local.prefs

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.prefs.Preferences

class DesktopPreferenceStore(
    storageDirectory: File,
    legacyNodeName: String
) {

    private val settingsFile = File(storageDirectory, "settings.properties")
    private val temporaryFile = File(storageDirectory, "settings.properties.tmp")
    private val values = Properties()
    private val writeLock = Any()

    init {
        runCatching { storageDirectory.mkdirs() }

        if (settingsFile.exists()) {
            loadFromDisk()
        } else {
            adoptLegacyPreferences(legacyNodeName)
        }
    }

    fun get(key: String, defaultValue: String): String = values.getProperty(key) ?: defaultValue

    fun getOrNull(key: String): String? = values.getProperty(key)

    fun put(key: String, value: String) {
        values.setProperty(key, value)
        persist()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue

    fun putBoolean(key: String, value: Boolean) = put(key, value.toString())

    fun getInt(key: String, defaultValue: Int): Int =
        values.getProperty(key)?.toIntOrNull() ?: defaultValue

    fun putInt(key: String, value: Int) = put(key, value.toString())

    fun getLong(key: String, defaultValue: Long): Long =
        values.getProperty(key)?.toLongOrNull() ?: defaultValue

    fun putLong(key: String, value: Long) = put(key, value.toString())

    fun getFloat(key: String, defaultValue: Float): Float =
        values.getProperty(key)?.toFloatOrNull() ?: defaultValue

    fun putFloat(key: String, value: Float) = put(key, value.toString())

    fun remove(key: String) {
        values.remove(key)
        persist()
    }

    private fun loadFromDisk() {
        runCatching {
            settingsFile.inputStream().use { stream -> values.load(stream) }
        }
    }

    private fun adoptLegacyPreferences(legacyNodeName: String) {
        runCatching {
            val legacyRoot = Preferences.userRoot()
            if (!legacyRoot.nodeExists(legacyNodeName)) return@runCatching

            val legacyNode = legacyRoot.node(legacyNodeName)
            legacyNode.keys().forEach { key ->
                legacyNode.get(key, null)?.let { value -> values.setProperty(key, value) }
            }

            legacyNode.removeNode()
            legacyRoot.flush()
        }

        persist()
    }

    private fun persist() {
        synchronized(writeLock) {
            runCatching {
                temporaryFile.outputStream().use { stream ->
                    values.store(stream, "Emberr desktop settings")
                }
                Files.move(
                    temporaryFile.toPath(),
                    settingsFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }
}
