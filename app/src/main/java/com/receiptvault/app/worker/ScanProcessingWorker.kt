package com.receiptvault.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.receiptvault.app.data.storage.ImageStorage
import com.receiptvault.app.scanner.ScannerProcessor
import com.receiptvault.app.scanner.session.ScanJobQueue
import com.receiptvault.app.scanner.session.ScanJobStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class ScanProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scannerProcessor: ScannerProcessor,
    private val imageStorage: ImageStorage,
    private val scanJobQueue: ScanJobQueue
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val rawPath = inputData.getString(KEY_RAW_PATH) ?: return Result.failure()
        val enhance = inputData.getBoolean(KEY_ENHANCE, true)

        scanJobQueue.update(jobId) { it.copy(status = ScanJobStatus.PROCESSING) }
        return runCatching {
            val out = imageStorage.createScanFile()
            val processed = scannerProcessor.processCapture(rawPath, out, enhance)
            scanJobQueue.update(jobId) {
                it.copy(status = ScanJobStatus.DONE, outputPath = processed.outputPath)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { e ->
                scanJobQueue.update(jobId) {
                    it.copy(status = ScanJobStatus.FAILED, error = e.message)
                }
                Result.failure()
            }
        )
    }

    companion object {
        private const val KEY_JOB_ID = "job_id"
        private const val KEY_RAW_PATH = "raw_path"
        private const val KEY_ENHANCE = "enhance"

        fun enqueue(context: Context, jobId: String, rawPath: String, enhance: Boolean) {
            val data = Data.Builder()
                .putString(KEY_JOB_ID, jobId)
                .putString(KEY_RAW_PATH, rawPath)
                .putBoolean(KEY_ENHANCE, enhance)
                .build()
            val request = OneTimeWorkRequestBuilder<ScanProcessingWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
