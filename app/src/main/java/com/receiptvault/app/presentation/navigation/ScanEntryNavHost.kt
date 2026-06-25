package com.receiptvault.app.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.receiptvault.app.presentation.screens.ScanReviewScreen
import com.receiptvault.app.presentation.screens.SmartScanScreen
import com.receiptvault.app.presentation.viewmodel.ScanEntryViewModel
import com.receiptvault.app.presentation.viewmodel.SmartScanViewModel
import com.receiptvault.app.scanner.session.BatchScanSession

@Composable
fun ScanEntryNavHost(
    navController: NavHostController,
    batchScanSession: BatchScanSession,
    startBatch: Boolean,
    importUri: Uri?,
    onFinished: () -> Unit
) {
    val entryViewModel: ScanEntryViewModel = hiltViewModel()

    LaunchedEffect(importUri) {
        importUri?.let { uri ->
            entryViewModel.importAndReview(uri) { path ->
                navController.navigate(Screen.ScanReview.createRoute(path))
            }
        }
    }

    LaunchedEffect(startBatch) {
        if (startBatch) {
            navController.navigate(Screen.SmartScan.createRoute(batch = true))
        }
    }

    val start = if (importUri != null) Screen.SmartScan.route else Screen.SmartScan.route

    NavHost(navController = navController, startDestination = start) {
        composable(
            route = Screen.SmartScan.route,
            arguments = listOf(
                navArgument(Screen.SmartScan.ARG_BATCH) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Screen.SmartScan.ARG_APPEND) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val viewModel: SmartScanViewModel = hiltViewModel()
            val batch = backStackEntry.arguments?.getBoolean(Screen.SmartScan.ARG_BATCH) == true
                || startBatch
            LaunchedEffect(batch) {
                if (batch) viewModel.updateBatchMode(true)
            }
            SmartScanScreen(
                onNavigateBack = onFinished,
                onScanComplete = { path ->
                    if (viewModel.batchMode) {
                        // stay on scanner
                    } else {
                        navController.navigate(Screen.ScanReview.createRoute(path))
                    }
                },
                onBatchDone = { navController.navigate(Screen.BatchReview.route) },
                onOpenSubscription = { /* quick scan: finish to main app */ onFinished() },
                viewModel = viewModel
            )
        }
        composable(
            route = Screen.ScanReview.route,
            arguments = listOf(navArgument(Screen.ScanReview.ARG_IMAGE_PATH) { type = NavType.StringType })
        ) {
            ScanReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { onFinished() },
                onOpenSubscription = onFinished,
                quickExit = true
            )
        }
        composable(Screen.BatchReview.route) {
            com.receiptvault.app.presentation.screens.BatchReviewScreen(
                batchScanSession = batchScanSession,
                onNavigateBack = { navController.popBackStack() },
                onReviewItem = { path -> navController.navigate(Screen.ScanReview.createRoute(path)) },
                onClear = { batchScanSession.clear() }
            )
        }
    }
}
