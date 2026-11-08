package com.mipatrimonio.app.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun update() {
        scope.launch {
            runCatching { MiPatrimonioWidget().updateAll(context) }
        }
    }

    companion object {
        fun updateNow(context: Context) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                runCatching { MiPatrimonioWidget().updateAll(context.applicationContext) }
            }
        }
    }
}
