package com.ben.emberr.data.local.room

import androidx.room.RoomDatabase

/**
 * Tells the KMP compiler: "Expect each platform to provide a way to build this database."
 */
expect fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase