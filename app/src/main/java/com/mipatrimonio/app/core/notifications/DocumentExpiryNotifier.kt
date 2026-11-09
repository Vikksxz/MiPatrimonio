package com.mipatrimonio.app.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mipatrimonio.app.MainActivity
import com.mipatrimonio.app.R

/** Notificación de documentos próximos a vencer (T4.8). */
object DocumentExpiryNotifier {
    fun notify(context: Context, documentId: Long, title: String, expiresLabel: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            (40_000L + documentId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, AppNotificationChannels.DOCUMENTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.document_expiry_notification_title))
            .setContentText(context.getString(R.string.document_expiry_notification_text, title, expiresLabel))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.document_expiry_notification_text, title, expiresLabel)))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((50_000L + documentId).toInt(), n)
    }
}
