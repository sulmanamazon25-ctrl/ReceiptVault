package com.receiptvault.app.scanner.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ScanJobStatus { QUEUED, PROCESSING, DONE, FAILED }

data class ScanJob(
    val id: String,
    val rawPath: String,
    var status: ScanJobStatus = ScanJobStatus.QUEUED,
    var outputPath: String? = null,
    var error: String? = null
)

@Singleton
class ScanJobQueue @Inject constructor() {
    private val _jobs = MutableStateFlow<List<ScanJob>>(emptyList())
    val jobs: StateFlow<List<ScanJob>> = _jobs.asStateFlow()

    val pendingCount: Int get() = _jobs.value.count {
        it.status == ScanJobStatus.QUEUED || it.status == ScanJobStatus.PROCESSING
    }

    fun enqueue(rawPath: String): ScanJob {
        val job = ScanJob(id = "scan_${System.currentTimeMillis()}", rawPath = rawPath)
        _jobs.value = _jobs.value + job
        return job
    }

    fun update(jobId: String, block: (ScanJob) -> ScanJob) {
        _jobs.value = _jobs.value.map { if (it.id == jobId) block(it) else it }
    }

    fun clearCompleted() {
        _jobs.value = _jobs.value.filter { it.status != ScanJobStatus.DONE }
    }
}
