package com.ben.emberr.domain.util

import kotlinx.coroutines.sync.Mutex

// Prevents local note saves from running concurrently with background sync operations,
// ensuring a sync never reads or modifies a note while it is mid-update.
object SyncCoordinator {
    val mutex = Mutex()
}

// Executes a unit of work (such as reconciling a single note) if SyncCoordinator.mutex is available.
// If the lock is busy, it skips the work immediately instead of waiting, allowing other items
// in the sync pass to continue. Skipped items will be retried during the next sync cycle.
suspend fun <T> withSyncCoordinatorOrSkip(block: suspend () -> T): T? {
    if (!SyncCoordinator.mutex.tryLock()) return null
    return try {
        block()
    } finally {
        SyncCoordinator.mutex.unlock()
    }
}