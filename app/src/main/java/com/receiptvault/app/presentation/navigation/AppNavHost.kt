package com.receiptvault.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.receiptvault.app.presentation.screens.AddEditReceiptScreen
import com.receiptvault.app.presentation.screens.FoldersScreen
import com.receiptvault.app.presentation.screens.HomeScreen
import com.receiptvault.app.presentation.screens.ReceiptDetailScreen
import com.receiptvault.app.presentation.screens.SearchScreen
import com.receiptvault.app.presentation.screens.SettingsScreen
import com.receiptvault.app.presentation.screens.SubscriptionScreen

/** The app's navigation graph. */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddReceipt = { navController.navigate(Screen.AddEditReceipt.createRoute()) },
                onOpenReceipt = { id -> navController.navigate(Screen.ReceiptDetail.createRoute(id)) },
                onOpenFolders = { navController.navigate(Screen.Folders.route) },
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
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
                onNavigateBack = { navController.popBackStack() }
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
                }
            )
        }

        composable(Screen.Folders.route) {
            FoldersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenReceipt = { id -> navController.navigate(Screen.ReceiptDetail.createRoute(id)) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenSubscription = { navController.navigate(Screen.Subscription.route) }
            )
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
