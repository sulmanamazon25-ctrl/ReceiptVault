package com.receiptvault.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.receiptvault.app.presentation.navigation.ScanEntryNavHost
import com.receiptvault.app.presentation.theme.ReceiptVaultTheme
import com.receiptvault.app.scanner.session.BatchScanSession
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Lightweight scan-only entry: shortcuts, widget, tile, and share-import.
 * Finishes after save so the user returns to their previous app.
 */
@AndroidEntryPoint
class ScanEntryActivity : FragmentActivity() {

    @Inject
    lateinit var batchScanSession: BatchScanSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val batch = intent.getBooleanExtra(EXTRA_BATCH, false)
        val importUri = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
        setContent {
            ReceiptVaultTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    ScanEntryNavHost(
                        navController = navController,
                        batchScanSession = batchScanSession,
                        startBatch = batch,
                        importUri = importUri,
                        onFinished = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_BATCH = "extra_batch"
    }
}
