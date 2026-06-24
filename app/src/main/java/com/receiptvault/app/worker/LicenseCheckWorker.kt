package com.receiptvault.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.receiptvault.app.domain.repository.LicenseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LicenseCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val licenseRepository: LicenseRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val license = licenseRepository.getLicense() ?: return Result.success()
        return licenseRepository.validateOnline()
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() }
            )
    }
}
