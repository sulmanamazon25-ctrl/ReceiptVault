package com.receiptvault.app.di

import com.receiptvault.app.data.repository.FolderRepositoryImpl
import com.receiptvault.app.data.repository.ReceiptRepositoryImpl
import com.receiptvault.app.data.repository.SubscriptionRepositoryImpl
import com.receiptvault.app.domain.repository.FolderRepository
import com.receiptvault.app.domain.repository.ReceiptRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository interfaces to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): ReceiptRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository
}
