package com.mipatrimonio.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                AppGate()
            }
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
