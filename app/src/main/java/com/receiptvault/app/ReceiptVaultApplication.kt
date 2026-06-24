package com.receiptvault.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.receiptvault.app.worker.LicenseCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application entry point.
 *
 * - [HiltAndroidApp] bootstraps the Hilt dependency graph for the whole process.
 * - Implements [Configuration.Provider] so WorkManager is initialized on demand with a
 *   [HiltWorkerFactory], allowing workers to receive injected dependencies. The default
 *   WorkManager initializer is removed in the manifest.
 * - Loads the SQLCipher native library once, before any encrypted database is opened.
 */
@HiltAndroidApp
class ReceiptVaultApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        val request = PeriodicWorkRequestBuilder<LicenseCheckWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "license_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
