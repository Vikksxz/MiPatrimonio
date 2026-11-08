package com.mipatrimonio.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mipatrimonio.app.R

/** Canales de notificación (§12). Idempotente. */
object AppNotificationChannels {
    const val BUDGETS = "budgets"
    const val SAVINGS = "savings"
    const val SUBSCRIPTIONS = "subscriptions"
    const val REMINDERS = "reminders"
    const val REPORTS = "reports"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(BUDGETS, context.getString(R.string.channel_budgets), NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(SAVINGS, context.getString(R.string.channel_savings), NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(SUBSCRIPTIONS, context.getString(R.string.channel_subscriptions), NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(REPORTS, context.getString(R.string.channel_reports), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
