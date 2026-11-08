package com.mipatrimonio.app.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mipatrimonio.app.R
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object SavingsNotifier {

    fun notifyAhead(context: Context, planId: Long, planName: String, months: Int, target: YearMonth) {
        if (!canNotify(context)) return
        val monthLabel = target.month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")) + " " + target.year
        val text = context.getString(R.string.savings_notify_ahead, planName, months, monthLabel)
        post(context, AppNotificationChannels.SAVINGS, (planId.toInt() * 17) + 1, context.getString(R.string.channel_savings), text)
    }

    fun notifyBehind(context: Context, planId: Long, planName: String) {
        if (!canNotify(context)) return
        val text = context.getString(R.string.savings_notify_behind, planName)
        post(context, AppNotificationChannels.SAVINGS, (planId.toInt() * 17) + 2, context.getString(R.string.channel_savings), text)
    }

    fun notifyTest(context: Context) {
        if (!canNotify(context)) return
        post(context, AppNotificationChannels.SAVINGS, 99001, context.getString(R.string.channel_savings), context.getString(R.string.notifications_test_savings_body))
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return true
    }

    private fun post(context: Context, channel: String, id: Int, title: String, text: String) {
        AppNotificationChannels.createAll(context)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
