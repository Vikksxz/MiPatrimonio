package com.mipatrimonio.app.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.money.Money

object BudgetNotifier {

    fun notify80(context: Context, budgetId: Long, categoryName: String, spentCents: Long, limitCents: Long) {
        if (!canNotify(context)) return
        val text = context.getString(R.string.budget_notify_80, categoryName, Money.format(spentCents), Money.format(limitCents))
        post(context, (budgetId.toInt() * 17) + 3, context.getString(R.string.channel_budgets), text)
    }

    fun notify100(context: Context, budgetId: Long, categoryName: String, spentCents: Long, limitCents: Long) {
        if (!canNotify(context)) return
        val text = context.getString(R.string.budget_notify_100, categoryName, Money.format(spentCents), Money.format(limitCents))
        post(context, (budgetId.toInt() * 17) + 4, context.getString(R.string.channel_budgets), text)
    }

    fun notifyTest(context: Context) {
        if (!canNotify(context)) return
        post(context, 99002, context.getString(R.string.channel_budgets), context.getString(R.string.notifications_test_budget_body))
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return true
    }

    private fun post(context: Context, id: Int, title: String, text: String) {
        AppNotificationChannels.createAll(context)
        val n = NotificationCompat.Builder(context, AppNotificationChannels.BUDGETS)
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
