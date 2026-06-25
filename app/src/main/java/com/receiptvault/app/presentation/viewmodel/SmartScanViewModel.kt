package com.receiptvault.app.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.data.storage.ImageStorage
import com.receiptvault.app.domain.repository.SubscriptionRepository
import com.receiptvault.app.scanner.capture.AutoCaptureController
import com.receiptvault.app.scanner.session.BatchScanSession
import com.receiptvault.app.scanner.session.ScanJobQueue
import com.receiptvault.app.scanner.session.ScanJobStatus
import com.receiptvault.app.worker.ScanProcessingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SmartScanViewModel @Inject constructor(
    private val imageStorage: ImageStorage,
    private val autoCaptureController: AutoCaptureController,
    private val batchScanSession: BatchScanSession,
    private val scanJobQueue: ScanJobQueue,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val isPro = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    var isProcessing by mutableStateOf(false)
        private set

    var batchMode by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var pendingAutoCapture by mutableStateOf(false)
        private set

    private var pendingRawPath: String? = null

    fun updateBatchMode(enabled: Boolean) {
        batchMode = enabled
        if (!enabled) batchScanSession.clear()
    }

    fun resetAutoCapture() {
        autoCaptureController.reset()
        pendingAutoCapture = false
    }

    fun onFrameAnalyzed(luminanceHash: Int, sharpness: Float) {
        if (isProcessing || !isPro.value) return
        if (autoCaptureController.onFrame(luminanceHash, sharpness, enabled = true)) {
            pendingAutoCapture = true
        }
    }

    fun consumeAutoCapture() {
        pendingAutoCapture = false
    }

    fun prepareCaptureFile(): java.io.File {
        val file = imageStorage.createImageFile()
        pendingRawPath = file.absolutePath
        return file
    }

    fun prepareCapture(): android.net.Uri =
        imageStorage.uriForFile(prepareCaptureFile())

    fun onCaptureSuccess(context: android.content.Context, onComplete: (String) -> Unit) {
        val raw = pendingRawPath ?: return
        pendingRawPath = null
        processRawCapture(context, raw, onComplete)
    }

    fun onCaptureCancelled() {
        pendingRawPath?.let { File(it).delete() }
        pendingRawPath = null
    }

    fun processRawCapture(context: android.content.Context, rawPath: String, onComplete: (String) -> Unit) {
        if (!batchMode && isProcessing) return
        errorMessage = null
        val applyEnhance = isPro.value
        val job = scanJobQueue.enqueue(rawPath)
        ScanProcessingWorker.enqueue(context, job.id, rawPath, applyEnhance)
        if (!batchMode) isProcessing = true
        viewModelScope.launch {
            scanJobQueue.jobs.first { jobs ->
                val j = jobs.find { it.id == job.id } ?: return@first false
                j.status == ScanJobStatus.DONE || j.status == ScanJobStatus.FAILED
            }
            val finished = scanJobQueue.jobs.value.first { it.id == job.id }
            when (finished.status) {
                ScanJobStatus.DONE -> {
                    finished.outputPath?.let { path ->
                        if (batchMode && isPro.value) {
                            batchScanSession.add(path)
                        } else {
                            onComplete(path)
                        }
                    }
                }
                ScanJobStatus.FAILED -> errorMessage = finished.error ?: "Scan failed"
                else -> {}
            }
            if (!batchMode) isProcessing = false
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
