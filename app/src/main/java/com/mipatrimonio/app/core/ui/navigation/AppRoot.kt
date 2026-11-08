package com.mipatrimonio.app.core.ui.navigation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.mipatrimonio.app.core.ui.components.NeonTabBar
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.feature.add.AddMenuSheet
import com.mipatrimonio.app.feature.add.AddTransactionViewModel
import com.mipatrimonio.app.feature.add.ReceiptScanDraft
import com.mipatrimonio.app.feature.add.ReviewSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val OCR_MAX_DIMENSION = 1600

/**
 * Raíz de la app (T1.3, T2.5 escaneo): barra inferior neón, NavHost y flujo de alta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(addViewModel: AddTransactionViewModel = hiltViewModel()) {
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
    var scanDraft by remember { mutableStateOf<ReceiptScanDraft?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    fun closeAddFlow() {
        val draft = scanDraft
        showAddSheet = false
        addMode = null
        scanDraft = null
        scanError = null
        scanning = false
        // Libera bitmaps del borrador (el ticket cifrado en disco se crea solo al confirmar).
        draft?.thumbnail?.takeUnless { it.isRecycled }?.recycle()
        draft?.sourceBitmap?.takeUnless { it.isRecycled }?.recycle()
    }

    val scanPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
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
        scanDraft = null
        scope.launch {
            val result = runCatching {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeBitmapForOcr(context, uri) ?: error("decode")
                }
                val draft = addViewModel.scanReceipt(bitmap)
                // La imagen original se conserva como sourceBitmap del borrador (se cifra al guardar).
                draft
            }
            result.onSuccess { draft ->
                scanDraft = draft
                scanning = false
            }.onFailure {
                scanning = false
                scanError = context.getString(R.string.scan_failed)
            }
        }
    }

    Scaffold(
        containerColor = NeonColor.bg,
        bottomBar = {
            NeonTabBar(
                selectedRoute = selectedRoute,
                onTabSelected = { route ->
                    if (route != selectedRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onAddClick = {
                    addMode = null
                    scanDraft = null
                    scanError = null
                    scanning = false
                    showAddSheet = true
                },
            )
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
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
                scanning || (addMode == "SCAN" && scanDraft == null && scanError == null) -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(color = NeonColor.neonCyan)
                            Text(
                                text = stringResource(R.string.scan_processing),
                                color = NeonColor.muted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                scanError != null && addMode == "SCAN" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = scanError ?: "",
                            color = NeonColor.expenseRed,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.add_sheet_scan_hint),
                            color = NeonColor.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                addMode == null -> {
                    AddMenuSheet(
                        onScan = {
                            showAddSheet = false
                            addMode = "SCAN"
                            scanError = null
                            scanDraft = null
                            scanPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onManualExpense = { addMode = TransactionType.CASH.name },
                        onIncome = { addMode = TransactionType.INCOME.name },
                    )
                }

                addMode == "SCAN" && scanDraft != null -> {
                    ReviewSheet(
                        initialType = TransactionType.CASH,
                        scanDraft = scanDraft,
                        viewModel = addViewModel,
                        onSaved = {
                            if (showAddSheet) {
                                closeAddFlow()
                                navController.navigate(Routes.TRANSACTIONS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onDismiss = { closeAddFlow() },
                    )
                }

                else -> {
                    val type = runCatching { TransactionType.valueOf(addMode!!) }
                        .getOrDefault(TransactionType.CASH)
                    ReviewSheet(
                        initialType = type,
                        viewModel = addViewModel,
                        onSaved = {
                            if (showAddSheet) {
                                closeAddFlow()
                                navController.navigate(Routes.TRANSACTIONS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onDismiss = { closeAddFlow() },
                    )
                }
            }
        }
    }
}

private fun decodeBitmapForOcr(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, OCR_MAX_DIMENSION)
    }
    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    } ?: return null

    val oriented = applyExifOrientation(context, uri, decoded)
    return scaleDown(oriented, OCR_MAX_DIMENSION)
}

private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    while (width / (sampleSize * 2) >= maxDimension || height / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
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
