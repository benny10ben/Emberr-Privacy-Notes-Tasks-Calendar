package com.emberr.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emberr.core.security.SqlCipherRuntime
import com.emberr.database.EmberrDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class DatabaseDriverFactory(private val context: Context, private val passphrase: ByteArray) {
    fun createDriver(): SqlDriver {
        SqlCipherRuntime.loadNativeLibraryAndLimitConnections()
        return AndroidSqliteDriver(
            schema = EmberrDatabase.Schema,
            context = context,
            name = "emberr_ai_index.db",
            factory = SupportOpenHelperFactory(SqlCipherRuntime.asRawKey(passphrase))
        )
    }
}
