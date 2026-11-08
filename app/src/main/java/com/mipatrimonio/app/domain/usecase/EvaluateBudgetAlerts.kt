package com.mipatrimonio.app.domain.usecase

import android.content.Context
import com.mipatrimonio.app.core.database.dao.BudgetDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.notifications.BudgetNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import javax.inject.Inject

/** Evalúa presupuestos del mes y envía avisos 80%/100% una vez por mes/umbral (§7.4, §12). */
class EvaluateBudgetAlerts @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
) {
    suspend fun run(now: YearMonth = YearMonth.now()) {
        val from = now.atDay(1).toEpochDay()
        val to = now.plusMonths(1).atDay(1).toEpochDay()
        val txs = transactionDao.getByMonth(from, to)
        val spentByCategory = mutableMapOf<Long, Long>()
        txs.forEach { tx ->
            val catId = tx.categoryId ?: return@forEach
            if (tx.type == TransactionType.INCOME) return@forEach
            spentByCategory[catId] = (spentByCategory[catId] ?: 0L) + tx.amountBaseCents
        }
        val budgets = budgetDao.getAllWithCategory()
        for (bwc in budgets) {
            val spent = spentByCategory[bwc.budget.categoryId] ?: 0L
            val st = BudgetThresholds.evaluate(
                spentCents = spent,
                limitCents = bwc.budget.limitCents,
                yearMonth = now,
                notified80Month = bwc.budget.notified80Month,
                notified100Month = bwc.budget.notified100Month,
            )
            if (st.newNotified80Month != bwc.budget.notified80Month || st.newNotified100Month != bwc.budget.notified100Month) {
                budgetDao.updateNotifiedMonths(bwc.budget.id, st.newNotified80Month, st.newNotified100Month)
            }
            val catName = bwc.category?.name ?: ""
            // Priorizar 100%; avisar solo en el primer cruce del mes (§7.4).
            when {
                st.notify100 -> BudgetNotifier.notify100(context, bwc.budget.id, catName, spent, bwc.budget.limitCents)
                st.notify80 -> BudgetNotifier.notify80(context, bwc.budget.id, catName, spent, bwc.budget.limitCents)
            }
        }
    }
}
