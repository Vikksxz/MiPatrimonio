package com.mipatrimonio.app.feature.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mipatrimonio.app.core.security.AppLockManager
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareReceiverActivity : FragmentActivity() {

    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings.collect { settings ->
                    if (settings.hideContent) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        val uri = extractStreamUri(intent)
        val source = resolveSourceLabel()

        setContent {
            MiPatrimonioTheme {
                ShareGate(
                    incomingUri = uri,
                    sourceLabel = source,
                    onDone = { finish() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { appLockManager.onForeground() }
    }

    override fun onStop() {
        if (!isChangingConfigurations) appLockManager.onBackground()
        super.onStop()
    }

    private fun extractStreamUri(intent: Intent?): Uri? {
        if (intent == null) return null
        val fromExtra = when (intent.action) {
            Intent.ACTION_SEND -> getParcelableUri(intent, Intent.EXTRA_STREAM)
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                list?.firstOrNull()
            }
            else -> null
        }
        if (fromExtra != null) return fromExtra
        // Fallback: algunas apps (p. ej. CamScanner) envían el contenido en ClipData.
        return intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    private fun getParcelableUri(intent: Intent, key: String): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(key)
        }

    private fun resolveSourceLabel(): String {
        val ref = referrer?.host ?: callingPackage ?: ""
        return when {
            ref.contains("camscanner", true) -> "CamScanner"
            ref.isNotBlank() -> ref
            else -> "CamScanner"
        }
    }
}
