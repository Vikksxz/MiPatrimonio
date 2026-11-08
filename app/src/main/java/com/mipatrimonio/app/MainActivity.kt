package com.mipatrimonio.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mipatrimonio.app.core.security.AppLockManager
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.core.ui.navigation.AppGate
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var addRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAddIntent(intent)

        // "Ocultar contenido en recientes y capturas" (§14), activado por defecto.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings.collect { settings ->
                    applySecureFlag(settings.hideContent)
                }
            }
        }

        setContent {
            MiPatrimonioTheme {
                AppGate(
                    openAdd = addRequested,
                    onAddConsumed = { addRequested = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAddIntent(intent)
    }

    private fun handleAddIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data?.scheme == "mipatrimonio" && intent.data?.host == "add") {
            addRequested = true
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { appLockManager.onForeground() }
    }

    override fun onStop() {
        // Una rotación no cuenta como segundo plano.
        if (!isChangingConfigurations) {
            appLockManager.onBackground()
        }
        super.onStop()
    }

    private fun applySecureFlag(secure: Boolean) {
        if (secure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
