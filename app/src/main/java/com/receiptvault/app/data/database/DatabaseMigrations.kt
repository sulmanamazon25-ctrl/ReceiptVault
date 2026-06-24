package com.receiptvault.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS license_cache (
                    id INTEGER NOT NULL PRIMARY KEY,
                    licenseKeyId TEXT NOT NULL,
                    tier TEXT NOT NULL,
                    token TEXT NOT NULL,
                    tokenExpiresAt INTEGER NOT NULL,
                    lastValidatedAt INTEGER NOT NULL,
                    deviceHash TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "ALTER TABLE subscriptions ADD COLUMN source TEXT NOT NULL DEFAULT 'PLAY'"
            )
        }
    }
}
