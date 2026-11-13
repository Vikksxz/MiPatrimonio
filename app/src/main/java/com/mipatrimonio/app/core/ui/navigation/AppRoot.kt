package com.mipatrimonio.app.core.ui.navigation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.ocr.DocumentClassifier
import com.mipatrimonio.app.core.ocr.SuggestedDocumentKind
import com.mipatrimonio.app.core.ui.components.NeonTabBar
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.feature.add.AddMenuSheet
import com.mipatrimonio.app.feature.add.AddTransactionViewModel
import com.mipatrimonio.app.feature.add.PayrollDraft
import com.mipatrimonio.app.feature.add.ReceiptScanDraft
import com.mipatrimonio.app.feature.add.ReviewSheet
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val OCR_MAX_DIMENSION = 1600

/**
 * Raíz de la app (T1.3, T2.5/T2.6+ escaneo/importación): barra inferior neón, NavHost y flujo de alta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    openAdd: Boolean = false,
    onAddConsumed: () -> Unit = {},
    addViewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedRoute = Routes.topLevel.firstOrNull { route ->
        currentDestination?.hierarchy?.any { it.route == route } == true
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var addMode by rememberSaveable { mutableStateOf<String?>(null) } // CASH, INCOME, SCAN
    var receiptDraft by remember { mutableStateOf<ReceiptScanDraft?>(null) }
    var payrollDraft by remember { mutableStateOf<PayrollDraft?>(null) }
    var pendingPayrollDraft by remember { mutableStateOf<PayrollDraft?>(null) }
    var showDuplicateWarning by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    fun clearDrafts() {
        receiptDraft?.thumbnail?.takeUnless { it.isRecycled }?.recycle()
        receiptDraft?.sourceBitmap?.takeUnless { it.isRecycled }?.recycle()
        receiptDraft = null
        payrollDraft?.thumbnail?.takeUnless { it.isRecycled }?.recycle()
        payrollDraft = null
        pendingPayrollDraft = null
        showDuplicateWarning = false
    }

    LaunchedEffect(openAdd) {
        if (openAdd) {
            addMode = null
            scanError = null
            scanning = false
            clearDrafts()
            showAddSheet = true
            onAddConsumed()
        }
    }

    fun closeAddFlow() {
        showAddSheet = false
        addMode = null
        scanError = null
        scanning = false
        clearDrafts()
    }

    fun openPayrollWithDuplicateCheck(draft: PayrollDraft) {
        scope.launch {
            val title = draft.displayName.removeSuffix(".pdf").removeSuffix(".PDF")
            val dups = runCatching { addViewModel.findPayrollDuplicates(draft.date, title) }.getOrDefault(emptyList())
            if (dups.isNotEmpty()) {
                pendingPayrollDraft = draft
                showDuplicateWarning = true
            } else {
                payrollDraft = draft
            }
        }
    }

    val scanPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) {
            addMode = null
            scanError = null
            scanning = false
            return@rememberLauncherForActivityResult
        }
        showAddSheet = true
        addMode = "SCAN"
        scanning = true
        scanError = null
        clearDrafts()
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { processScannedUri(context, uri, addViewModel) }
            }
            result.onSuccess { outcome ->
                scanning = false
                when (outcome) {
                    is ScanOutcome.Receipt -> receiptDraft = outcome.draft
                    is ScanOutcome.Payroll -> openPayrollWithDuplicateCheck(outcome.draft)
                }
            }.onFailure {
                scanning = false
                scanError = context.getString(R.string.scan_failed)
            }
        }
    }

    val openAddSheet = {
        addMode = null
        scanError = null
        scanning = false
        clearDrafts()
        showAddSheet = true
    }

    Scaffold(
        containerColor = NeonColor.bg,
        bottomBar = {
            NeonTabBar(
                selectedRoute = selectedRoute,
                onTabSelected = { route ->
                    if (route != selectedRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onAddClick = openAddSheet,
            )
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onAdd = openAddSheet,
        )
    }

    if (showDuplicateWarning && pendingPayrollDraft != null) {
        AlertDialog(
            onDismissRequest = {
                pendingPayrollDraft?.thumbnail?.takeUnless { it.isRecycled }?.recycle()
                pendingPayrollDraft = null
                showDuplicateWarning = false
                closeAddFlow()
            },
            containerColor = NeonColor.panel,
            title = { Text(stringResource(R.string.duplicate_payroll_title), color = NeonColor.ink) },
            text = { Text(stringResource(R.string.duplicate_payroll_message), color = NeonColor.muted) },
            confirmButton = {
                TextButton(onClick = {
                    payrollDraft = pendingPayrollDraft
                    pendingPayrollDraft = null
                    showDuplicateWarning = false
                }) { Text(stringResource(R.string.duplicate_continue), color = NeonColor.neonCyan) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPayrollDraft?.thumbnail?.takeUnless { it.isRecycled }?.recycle()
                    pendingPayrollDraft = null
                    showDuplicateWarning = false
                    closeAddFlow()
                }) { Text(stringResource(R.string.common_cancel), color = NeonColor.muted) }
            },
        )
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { closeAddFlow() },
            sheetState = sheetState,
            containerColor = NeonColor.panel,
            contentColor = NeonColor.ink,
            scrimColor = NeonColor.bg.copy(alpha = 0.72f),
        ) {
            when {
                scanning || (addMode == "SCAN" && receiptDraft == null && payrollDraft == null && scanError == null) -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = NeonColor.neonCyan)
                            Text(stringResource(R.string.scan_processing), color = NeonColor.muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                scanError != null && addMode == "SCAN" -> {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(scanError ?: "", color = NeonColor.expenseRed, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.add_sheet_scan_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                addMode == null -> {
                    AddMenuSheet(
                        onScan = {
                            showAddSheet = false
                            addMode = "SCAN"
                            scanError = null
                            clearDrafts()
                            scanPicker.launch(arrayOf("image/*", "application/pdf"))
                        },
                        onManualExpense = { addMode = TransactionType.CASH.name },
                        onIncome = { addMode = TransactionType.INCOME.name },
                    )
                }
                addMode == "SCAN" && payrollDraft != null -> {
                    ReviewSheet(
                        initialType = TransactionType.INCOME,
                        payrollDraft = payrollDraft,
                        viewModel = addViewModel,
                        onSaved = { if (showAddSheet) { closeAddFlow(); navigateToTransactions(navController) } },
                        onDismiss = { closeAddFlow() },
                    )
                }
                addMode == "SCAN" && receiptDraft != null -> {
                    ReviewSheet(
                        initialType = TransactionType.CASH,
                        scanDraft = receiptDraft,
                        viewModel = addViewModel,
                        onSaved = { if (showAddSheet) { closeAddFlow(); navigateToTransactions(navController) } },
                        onDismiss = { closeAddFlow() },
                    )
                }
                else -> {
                    val type = runCatching { TransactionType.valueOf(addMode!!) }.getOrDefault(TransactionType.CASH)
                    ReviewSheet(
                        initialType = type,
                        viewModel = addViewModel,
                        onSaved = { if (showAddSheet) { closeAddFlow(); navigateToTransactions(navController) } },
                        onDismiss = { closeAddFlow() },
                    )
                }
            }
        }
    }
}

private fun navigateToTransactions(navController: androidx.navigation.NavHostController) {
    navController.navigate(Routes.TRANSACTIONS) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private sealed interface ScanOutcome {
    data class Receipt(val draft: ReceiptScanDraft) : ScanOutcome
    data class Payroll(val draft: PayrollDraft) : ScanOutcome
}

private suspend fun processScannedUri(
    context: Context,
    uri: Uri,
    viewModel: AddTransactionViewModel,
): ScanOutcome {
    val mime = context.contentResolver.getType(uri)
    val name = queryDisplayName(context, uri) ?: "documento"
    val isPdf = mime == "application/pdf" || name.endsWith(".pdf", true)
    return if (isPdf) {
        val temp = copyToTemp(context, uri, suffix = ".pdf")
        val rendered = com.mipatrimonio.app.core.ocr.PdfBitmapRenderer.renderFirstPage(temp)
        val lines = viewModel.recognizeLines(rendered)
        val classification = DocumentClassifier.classify(lines)
        if (classification.suggested == SuggestedDocumentKind.PAYROLL || classification.payrollScore >= 2) {
            val thumb = renderThumbnail(rendered)
            rendered.recycle()
            ScanOutcome.Payroll(
                PayrollDraft(
                    sourceFilePath = temp.absolutePath,
                    mimeType = "application/pdf",
                    displayName = name,
                    amountCents = classification.suggestedNetCents,
                    date = classification.suggestedDate ?: LocalDate.now(),
                    thumbnail = thumb,
                )
            )
        } else {
            // Ticket PDF: render de nuevo como fuente (rendered se usó para OCR; scanReceipt crea su thumbnail).
            rendered.recycle()
            val bmp = com.mipatrimonio.app.core.ocr.PdfBitmapRenderer.renderFirstPage(temp)
            runCatching { temp.delete() }
            ScanOutcome.Receipt(viewModel.scanReceipt(bmp))
        }
    } else {
        val bmp = decodeBitmapForOcr(context, uri) ?: error("decode")
        val lines = viewModel.recognizeLines(bmp)
        val classification = DocumentClassifier.classify(lines)
        if (classification.suggested == SuggestedDocumentKind.PAYROLL) {
            val suffix = when {
                name.endsWith(".png", true) -> ".png"
                else -> ".jpg"
            }
            val temp = copyToTemp(context, uri, suffix = suffix)
            val thumb = renderThumbnail(bmp)
            bmp.recycle()
            ScanOutcome.Payroll(
                PayrollDraft(
                    sourceFilePath = temp.absolutePath,
                    mimeType = mime ?: "image/jpeg",
                    displayName = name,
                    amountCents = classification.suggestedNetCents,
                    date = classification.suggestedDate ?: LocalDate.now(),
                    thumbnail = thumb,
                )
            )
        } else {
            ScanOutcome.Receipt(viewModel.scanReceipt(bmp))
        }
    }
}

private fun renderThumbnail(source: Bitmap): Bitmap {
    val thumbSide = 480
    val scale = minOf(
        thumbSide.toFloat() / source.width.coerceAtLeast(1),
        thumbSide.toFloat() / source.height.coerceAtLeast(1),
        1f,
    )
    return Bitmap.createScaledBitmap(
        source,
        (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1),
        true,
    )
}

private fun copyToTemp(context: Context, uri: Uri, suffix: String): File {
    val dir = File(context.cacheDir, "scan_inbox").also { it.mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}$suffix")
    context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
        ?: error("open")
    return file
}

private fun queryDisplayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
} catch (_: Exception) { null }

private fun decodeBitmapForOcr(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, OCR_MAX_DIMENSION) }
    val decoded = context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, options) } ?: return null
    val oriented = applyExifOrientation(context, uri, decoded)
    return scaleDown(oriented, OCR_MAX_DIMENSION)
}

private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    while (width / (sampleSize * 2) >= maxDimension || height / (sampleSize * 2) >= maxDimension) sampleSize *= 2
    return sampleSize
}

private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}

private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val longestSide = maxOf(bitmap.width, bitmap.height)
    if (longestSide <= maxDimension) return bitmap
    val ratio = maxDimension.toFloat() / longestSide
    val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
    if (scaled !== bitmap) bitmap.recycle()
    return scaled
}
