package com.ben.emberr.domain.backup

import com.ben.emberr.data.local.room.AppDatabase
import java.io.File

fun exportPlainSqliteCopy(appDatabase: AppDatabase, destination: File) {
    val liveDb = appDatabase.openHelper.writableDatabase
    liveDb.execSQL("ATTACH DATABASE '${destination.absolutePath}' AS plaintext KEY ''")
    try {
        liveDb.query("SELECT sqlcipher_export('plaintext')").use { it.moveToFirst() }
    } finally {
        liveDb.execSQL("DETACH DATABASE plaintext")
    }
}
