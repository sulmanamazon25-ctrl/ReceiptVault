package com.receiptvault.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.receiptvault.app.presentation.screens.AddEditReceiptScreen
import com.receiptvault.app.presentation.screens.BatchReviewScreen
import com.receiptvault.app.presentation.screens.FoldersScreen
import com.receiptvault.app.presentation.screens.HomeScreen
import com.receiptvault.app.presentation.screens.ReceiptDetailScreen
import com.receiptvault.app.presentation.screens.ScanReviewScreen
import com.receiptvault.app.presentation.screens.SearchScreen
import com.receiptvault.app.presentation.screens.SettingsScreen
import com.receiptvault.app.presentation.screens.SmartScanScreen
import com.receiptvault.app.presentation.screens.SubscriptionScreen
import com.receiptvault.app.presentation.viewmodel.SmartScanViewModel
import com.receiptvault.app.scanner.session.BatchScanSession

@Composable
fun AppNavHost(
    navController: NavHostController,
    batchScanSession: BatchScanSession,
    startDestination: String = Screen.Home.route
) {
    val openPro = { navController.navigate(Screen.Subscription.route) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onSmartScan = { navController.navigate(Screen.SmartScan.createRoute()) },
                onAddReceipt = { navController.navigate(Screen.AddEditReceipt.createRoute()) },
                onOpenReceipt = { id -> navController.navigate(Screen.ReceiptDetail.createRoute(id)) },
                onOpenFolders = { navController.navigate(Screen.Folders.route) },
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenSubscription = openPro
            )
        }

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
            val append = backStackEntry.arguments?.getBoolean(Screen.SmartScan.ARG_APPEND) == true
            val batch = backStackEntry.arguments?.getBoolean(Screen.SmartScan.ARG_BATCH) == true
            LaunchedEffect(batch) {
                if (batch) viewModel.updateBatchMode(true)
            }
            SmartScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanComplete = { path ->
                    if (append) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("append_page_path", path)
                        navController.popBackStack()
                    } else if (viewModel.batchMode) {
                        // stay on scanner
                    } else {
                        navController.navigate(Screen.ScanReview.createRoute(path))
                    }
                },
                onBatchDone = { navController.navigate(Screen.BatchReview.route) },
                onOpenSubscription = openPro,
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.ScanReview.route,
            arguments = listOf(
                navArgument(Screen.ScanReview.ARG_IMAGE_PATH) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val appendPath by backStackEntry.savedStateHandle
                .getStateFlow<String?>("append_page_path", null)
                .collectAsStateWithLifecycle()
            ScanReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { id ->
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                    navController.navigate(Screen.ReceiptDetail.createRoute(id))
                },
                onOpenSubscription = openPro,
                onAddPage = {
                    navController.navigate(Screen.SmartScan.createRoute(append = true))
                },
                appendPagePath = appendPath,
                onAppendConsumed = {
                    backStackEntry.savedStateHandle.remove<String>("append_page_path")
                }
            )
        }

        composable(Screen.BatchReview.route) {
            BatchReviewScreen(
                batchScanSession = batchScanSession,
                onNavigateBack = { navController.popBackStack() },
                onReviewItem = { path ->
                    navController.navigate(Screen.ScanReview.createRoute(path))
                },
                onClear = { batchScanSession.clear() }
            )
        }

        composable(
            route = Screen.AddEditReceipt.route,
            arguments = listOf(
                navArgument(Screen.AddEditReceipt.ARG_RECEIPT_ID) {
                    type = NavType.LongType
                    defaultValue = Screen.AddEditReceipt.NO_RECEIPT
                }
            )
        ) {
            AddEditReceiptScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenSubscription = openPro
            )
        }

        composable(
            route = Screen.ReceiptDetail.route,
            arguments = listOf(
                navArgument(Screen.ReceiptDetail.ARG_RECEIPT_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            ReceiptDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(Screen.AddEditReceipt.createRoute(id))
                },
                onOpenSubscription = openPro
            )
        }

        composable(Screen.Folders.route) {
            FoldersScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenSubscription = openPro
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenReceipt = { id -> navController.navigate(Screen.ReceiptDetail.createRoute(id)) },
                onOpenSubscription = openPro
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenSubscription = openPro
            )
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
