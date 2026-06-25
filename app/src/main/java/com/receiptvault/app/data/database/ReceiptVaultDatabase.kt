package com.receiptvault.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.receiptvault.app.data.database.dao.FolderDao
import com.receiptvault.app.data.database.dao.LicenseCacheDao
import com.receiptvault.app.data.database.dao.ReceiptDao
import com.receiptvault.app.data.database.dao.SubscriptionDao
import com.receiptvault.app.data.database.entity.FolderEntity
import com.receiptvault.app.data.database.entity.LicenseCacheEntity
import com.receiptvault.app.data.database.entity.ReceiptEntity
import com.receiptvault.app.data.database.entity.SubscriptionEntity

/**
 * The encrypted Room database. SQLCipher is wired in via the open-helper factory provided
 * by [com.receiptvault.app.di.DatabaseModule]; this class itself stays storage-agnostic.
 */
@Database(
    entities = [
        ReceiptEntity::class,
        FolderEntity::class,
        SubscriptionEntity::class,
        LicenseCacheEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ReceiptVaultDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao

    abstract fun folderDao(): FolderDao

    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun licenseCacheDao(): LicenseCacheDao

    companion object {
        const val DATABASE_NAME = "receiptvault.db"
    }
}
