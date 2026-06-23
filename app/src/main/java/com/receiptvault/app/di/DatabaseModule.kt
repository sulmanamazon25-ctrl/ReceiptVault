package com.receiptvault.app.di

import android.content.Context
import androidx.room.Room
import com.receiptvault.app.data.database.ReceiptVaultDatabase
import com.receiptvault.app.data.database.dao.FolderDao
import com.receiptvault.app.data.database.dao.ReceiptDao
import com.receiptvault.app.data.database.dao.SubscriptionDao
import com.receiptvault.app.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * Builds the encrypted Room database. The SQLCipher [SupportOpenHelperFactory] is keyed with
 * the passphrase supplied by [DatabaseKeyProvider]; the native SQLCipher library is loaded in
 * the Application before any database access occurs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider
    ): ReceiptVaultDatabase {
        val passphrase = keyProvider.getOrCreatePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(
            context,
            ReceiptVaultDatabase::class.java,
            ReceiptVaultDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .build()
    }

    @Provides
    fun provideReceiptDao(database: ReceiptVaultDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideFolderDao(database: ReceiptVaultDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideSubscriptionDao(database: ReceiptVaultDatabase): SubscriptionDao =
        database.subscriptionDao()
}
