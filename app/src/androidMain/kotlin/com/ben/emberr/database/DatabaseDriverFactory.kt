package com.ben.emberr.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emberr.database.EmberrDatabase
import net.sqlcipher.database.SupportFactory

class DatabaseDriverFactory(private val context: Context, private val passphrase: ByteArray) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = EmberrDatabase.Schema,
            context = context,
            name = "emberr_ai_index.db",
            factory = SupportFactory(passphrase)
        )
    }
}
