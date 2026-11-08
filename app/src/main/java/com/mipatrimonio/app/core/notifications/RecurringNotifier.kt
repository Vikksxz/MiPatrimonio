package com.mipatrimonio.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.money.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object RecurringNotifier {
    const val CHANNEL_SUBSCRIPTIONS = "subscriptions"
    private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_SUBSCRIPTIONS,
                context.getString(R.string.channel_subscriptions),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            nm.createNotificationChannel(ch)
        }
    }

    fun notifyUpcoming(context: Context, rule: RecurringEntity, nextDate: LocalDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val text = context.getString(
            R.string.recurring_reminder_text,
            rule.name,
            Money.format(rule.amountCents),
            nextDate.format(dateFmt),
        )
        val n = NotificationCompat.Builder(context, CHANNEL_SUBSCRIPTIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.recurring_reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((rule.id.toInt() * 31) + nextDate.toEpochDay().toInt(), n)
    }
}
