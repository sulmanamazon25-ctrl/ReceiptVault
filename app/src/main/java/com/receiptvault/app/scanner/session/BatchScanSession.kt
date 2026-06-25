package com.receiptvault.app.scanner.session

import com.receiptvault.app.domain.model.ScanResult
import javax.inject.Inject
import javax.inject.Singleton

data class BatchScanItem(
    val imagePath: String,
    val scanResult: ScanResult? = null
)

@Singleton
class BatchScanSession @Inject constructor() {
    val items: MutableList<BatchScanItem> = mutableListOf()

    fun clear() = items.clear()

    fun add(path: String) {
        items.add(BatchScanItem(imagePath = path))
    }

    fun isActive(): Boolean = items.isNotEmpty()
}
