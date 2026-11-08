package com.mipatrimonio.app.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.core.security.LockScreen
import com.mipatrimonio.app.core.security.LockViewModel
import com.mipatrimonio.app.core.ui.theme.NeonColor

/**
 * Puerta de entrada (T1.10): mientras se leen los ajustes de bloqueo no se pinta
 * nada sensible; si la app está bloqueada se muestra [LockScreen].
 */
@Composable
fun AppGate(
    modifier: Modifier = Modifier,
    openAdd: Boolean = false,
    onAddConsumed: () -> Unit = {},
    viewModel: LockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        !state.ready -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(NeonColor.bg),
        )

        state.locked -> LockScreen(
            modifier = modifier,
            onAuthStarted = viewModel::onAuthStarted,
            onAuthFinished = viewModel::onAuthFinished,
            onUnlocked = viewModel::unlock,
        )

        else -> AppRoot(openAdd = openAdd, onAddConsumed = onAddConsumed)
    }
}
