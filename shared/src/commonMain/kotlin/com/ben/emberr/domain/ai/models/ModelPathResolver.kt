package com.ben.emberr.domain.ai.models

/**
 * Resolves the absolute path to a model file on the current platform.
 *
 * Android (dev):    /data/data/com.ben.emberr/files/<fileName>
 *                   (uploaded manually via Device File Explorer)
 *
 * Android (prod):   Will point to Context.filesDir once OTA download lands.
 *
 * Desktop (dev):    ~/.emberr/models/<fileName>
 *                   (copied manually during development)
 *
 * Desktop (prod):   Same ~/.emberr/models/ path, populated by OTA downloader.
 *                   No code change needed — just the download logic fills it.
 */
expect fun resolveModelPath(fileName: String): String
expect fun modelFileExists(path: String): Boolean