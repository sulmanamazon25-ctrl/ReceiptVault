package com.receiptvault.app.presentation.navigation

/**
 * Type-safe destination catalog. Routes with arguments expose a `createRoute(...)` helper so
 * call sites never assemble route strings by hand.
 */
sealed class Screen(val route: String) {

    data object Home : Screen("home")

    data object Folders : Screen("folders")

    data object Search : Screen("search")

    data object Settings : Screen("settings")

    data object Subscription : Screen("subscription")

    data object SmartScan : Screen("smart_scan?batch={batch}&append={append}") {
        const val ARG_BATCH = "batch"
        const val ARG_APPEND = "append"
        fun createRoute(batch: Boolean = false, append: Boolean = false): String =
            "smart_scan?batch=$batch&append=$append"
    }

    data object ScanReview : Screen("scan_review/{imagePath}") {
        const val ARG_IMAGE_PATH = "imagePath"
        fun createRoute(imagePath: String): String =
            "scan_review/${java.net.URLEncoder.encode(imagePath, Charsets.UTF_8.name())}"
    }

    data object BatchReview : Screen("batch_review")

    data object ReceiptDetail : Screen("receipt_detail/{receiptId}") {
        const val ARG_RECEIPT_ID = "receiptId"
        fun createRoute(receiptId: Long): String = "receipt_detail/$receiptId"
    }

    data object AddEditReceipt : Screen("add_edit_receipt?receiptId={receiptId}") {
        const val ARG_RECEIPT_ID = "receiptId"
        const val NO_RECEIPT = -1L
        fun createRoute(receiptId: Long = NO_RECEIPT): String =
            "add_edit_receipt?receiptId=$receiptId"
    }
}
