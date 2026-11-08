package com.mipatrimonio.app.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mipatrimonio.app.R

object CashReminderNotifier {

    fun notify(context: Context) {
        if (!canNotify(context)) return
        AppNotificationChannels.createAll(context)
        val n = NotificationCompat.Builder(context, AppNotificationChannels.REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.channel_reminders))
            .setContentText(context.getString(R.string.cash_reminder_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.cash_reminder_text)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(99003, n)
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return true
    }
}
