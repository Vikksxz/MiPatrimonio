package com.mipatrimonio.app.core.security

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor

/**
 * Pantalla de bloqueo (§14). Pide huella o credencial del dispositivo al abrir la
 * app y tras el tiempo configurado en segundo plano.
 */
@Composable
fun LockScreen(
    modifier: Modifier = Modifier,
    onAuthStarted: () -> Unit = {},
    onAuthFinished: () -> Unit = {},
    onUnlocked: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val promptInfo = remember(context) { buildPromptInfo(context) }
    val authenticatorAvailable = remember(context) { canAuthenticate(context) }

    val currentOnAuthStarted by rememberUpdatedState(onAuthStarted)
    val currentOnAuthFinished by rememberUpdatedState(onAuthFinished)
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)

    var error by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    val prompt = remember(activity, executor) {
        activity?.let { host ->
            BiometricPrompt(
                host,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        error = false
                        currentOnAuthFinished()
                        currentOnUnlocked()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        currentOnAuthFinished()
                        error = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED,
                            -> false

                            else -> true
                        }
                    }

                    override fun onAuthenticationFailed() {
                        // El prompt sigue abierto: se avisa sin cerrar la pantalla.
                        error = true
                    }
                },
            )
        }
    }

    LaunchedEffect(authenticatorAvailable, attempt) {
        if (authenticatorAvailable && prompt != null) {
            currentOnAuthStarted()
            prompt.authenticate(promptInfo)
        }
    }

    LockContent(
        modifier = modifier,
        unavailable = !authenticatorAvailable,
        showError = error,
        onUnlockClick = { attempt++ },
        onContinueClick = onUnlocked,
    )
}

@Composable
private fun LockContent(
    modifier: Modifier = Modifier,
    unavailable: Boolean = false,
    showError: Boolean = false,
    onUnlockClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonColor.bg)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(NeonColor.neonCyan, NeonColor.neonViolet),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                // Decorativo: el texto de al lado explica el estado.
                contentDescription = null,
                tint = NeonColor.bg,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.app_name),
            color = NeonColor.ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (unavailable) R.string.lock_unavailable_title else R.string.lock_locked,
            ),
            color = NeonColor.ink,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (unavailable) R.string.lock_unavailable_message else R.string.lock_message,
            ),
            color = NeonColor.muted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (showError) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.lock_error),
                color = NeonColor.expenseRed,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        NeonButton(
            onClick = if (unavailable) onContinueClick else onUnlockClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            NeonButtonText(
                text = stringResource(
                    if (unavailable) R.string.lock_continue else R.string.lock_unlock,
                ),
            )
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

private fun canAuthenticate(context: Context): Boolean {
    val manager = BiometricManager.from(context)
    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
    } else {
        @Suppress("DEPRECATION")
        manager.canAuthenticate()
    }
    return result == BiometricManager.BIOMETRIC_SUCCESS ||
        result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
}

private fun buildPromptInfo(context: Context): BiometricPrompt.PromptInfo {
    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.lock_biometric_title))
        .setSubtitle(context.getString(R.string.lock_biometric_subtitle))

    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )

        else -> {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
    }
    return builder.build()
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun LockContentPreview() {
    MiPatrimonioTheme {
        LockContent()
    }
}
