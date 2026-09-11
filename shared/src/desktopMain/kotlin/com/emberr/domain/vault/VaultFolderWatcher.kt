// Watches the vault folder and reports which markdown files have changed.

package com.emberr.domain.vault

import java.io.File
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.TimeUnit

private const val POLL_TIMEOUT_SECONDS = 1L

// Java's watcher only reports directories it was told about, so each one is registered by hand.
class VaultFolderWatcher(private val vaultRootDirectory: File) {

    private var watchService: WatchService? = null
    private val watchedDirectories = mutableSetOf<String>()

    fun start() {
        if (watchService != null) return
        val service = FileSystems.getDefault().newWatchService()
        watchService = service
        registerDirectoryTree(vaultRootDirectory, service)
    }

    fun stop() {
        try {
            watchService?.close()
        } catch (_: IOException) {
        }
        watchService = null
        watchedDirectories.clear()
    }

    // Waits up to a second, then returns every markdown file touched. The timeout is what lets
    // the caller notice it was cancelled.
    fun awaitChangedMarkdownFiles(): Set<File> {
        val service = watchService ?: return emptySet()
        val changedFiles = mutableSetOf<File>()

        val firstKey = try {
            service.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: ClosedWatchServiceException) {
            return emptySet()
        } catch (_: InterruptedException) {
            return emptySet()
        } ?: return emptySet()

        var key: WatchKey? = firstKey
        while (key != null) {
            collectFromKey(key, service, changedFiles)
            if (!key.reset()) watchedDirectories.remove((key.watchable() as Path).toFile().absolutePath)
            key = service.poll()
        }
        return changedFiles
    }

    private fun collectFromKey(key: WatchKey, service: WatchService, changedFiles: MutableSet<File>) {
        val watchedDirectory = (key.watchable() as Path).toFile()

        for (event in key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue
            val relativePath = event.context() as? Path ?: continue
            val changedFile = File(watchedDirectory, relativePath.toString())

            if (changedFile.isDirectory) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    registerDirectoryTree(changedFile, service)
                    changedFile.walkTopDown()
                        .filter { it.isFile && VaultPaths.isImportableMarkdownFile(it.name) }
                        .forEach { changedFiles.add(it) }
                }
                continue
            }

            if (VaultPaths.isImportableMarkdownFile(changedFile.name)) changedFiles.add(changedFile)
        }
    }

    private fun registerDirectoryTree(directory: File, service: WatchService) {
        if (!directory.isDirectory) return

        directory.walkTopDown()
            .filter { it.isDirectory }
            .forEach { subDirectory ->
                if (!watchedDirectories.add(subDirectory.absolutePath)) return@forEach
                try {
                    subDirectory.toPath().register(
                        service,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                    )
                } catch (cause: IOException) {
                    watchedDirectories.remove(subDirectory.absolutePath)
                    VaultLog.e("Cannot watch ${subDirectory.path}: ${cause.message}")
                }
            }
    }
}
