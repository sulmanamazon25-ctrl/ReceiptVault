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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE receipts ADD COLUMN documentType TEXT NOT NULL DEFAULT 'RECEIPT'")
            db.execSQL("ALTER TABLE receipts ADD COLUMN ocrText TEXT")
            db.execSQL("ALTER TABLE receipts ADD COLUMN scanConfidence REAL")
            db.execSQL("ALTER TABLE receipts ADD COLUMN extraPagesJson TEXT")
            db.execSQL("ALTER TABLE receipts ADD COLUMN parsedFieldsJson TEXT")
            db.execSQL("ALTER TABLE receipts ADD COLUMN isSensitive INTEGER NOT NULL DEFAULT 0")
        }
    }
}
