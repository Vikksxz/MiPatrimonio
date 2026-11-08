package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Resumen del mes natural para la pantalla Inicio (MiPatrimonio.md §7.1).
 * Todos los importes están en céntimos (`Long`); el signo lo da el tipo.
 */
data class MonthlySummary(
    val month: YearMonth,
    val incomeCents: Long = 0L,
    val cashCents: Long = 0L,
    val cardCents: Long = 0L,
    val subscriptionCents: Long = 0L,
    val savingsCents: Long = 0L,
    val recent: List<TransactionEntity> = emptyList(),
) {
    /** Gastos del mes: metálico + tarjeta + suscripciones. */
    val expenseCents: Long get() = cashCents + cardCents + subscriptionCents

    /** Disponible = ingresos − gastos − aportado al ahorro. */
    val availableCents: Long get() = incomeCents - expenseCents - savingsCents
}

/**
 * Caso de uso `GetMonthlySummary` (T1.9): calcula el resumen del mes actual y
 * los últimos movimientos. Las aportaciones a planes de ahorro se suman en T3.1.
 */
class GetMonthlySummary @Inject constructor(
    private val transactionDao: TransactionDao,
) {

    operator fun invoke(month: YearMonth = YearMonth.now()): Flow<MonthlySummary> {
        val fromEpochDay = month.atDay(1).toEpochDay()
        val toEpochDayExclusive = month.plusMonths(1).atDay(1).toEpochDay()
        return combine(
            transactionDao.observeByMonth(fromEpochDay, toEpochDayExclusive),
            transactionDao.observeRecent(RECENT_LIMIT),
        ) { monthTransactions, recent ->
            build(month, monthTransactions, recent)
        }
    }

    companion object {

        /** §8.1: la pantalla Inicio muestra los 4 últimos movimientos. */
        const val RECENT_LIMIT = 4

        /** Aportaciones a planes de ahorro: 0 hasta T3.1. */
        const val SAVINGS_CENTS = 0L

        /**
         * Porcentaje mínimo que ocupa un segmento de la barra aunque su importe
         * sea 0 (§7.1). Con tres segmentos el mínimo total es 18 %.
         */
        const val MIN_SEGMENT_SHARE = 0.06f

        /** Función pura: suma por tipo, descarta lo que no sea del mes y recorta los recientes. */
        fun build(
            month: YearMonth,
            transactions: List<TransactionEntity>,
            recent: List<TransactionEntity>,
            savingsCents: Long = SAVINGS_CENTS,
        ): MonthlySummary {
            var income = 0L
            var cash = 0L
            var card = 0L
            var subscription = 0L

            transactions.forEach { transaction ->
                if (YearMonth.from(LocalDate.ofEpochDay(transaction.date)) != month) return@forEach
                when (transaction.type) {
                    TransactionType.INCOME -> income += transaction.amountBaseCents
                    TransactionType.CASH -> cash += transaction.amountBaseCents
                    TransactionType.CARD -> card += transaction.amountBaseCents
                    TransactionType.SUBSCRIPTION -> subscription += transaction.amountBaseCents
                }
            }

            return MonthlySummary(
                month = month,
                incomeCents = income,
                cashCents = cash,
                cardCents = card,
                subscriptionCents = subscription,
                savingsCents = savingsCents,
                recent = recent.take(RECENT_LIMIT),
            )
        }

        /**
         * Pesos de la barra por tipo (§7.1): proporcionales al gasto de metálico,
         * tarjeta y suscripciones, con [MIN_SEGMENT_SHARE] fijo para cada uno aunque
         * su importe sea 0. Siempre suman 1 y nunca son negativos.
         *
         * Es lógica de presentación, pero §7.1 pide las reglas como funciones puras
         * en `domain/usecase`; así se puede probar sin Compose.
         */
        fun segmentWeights(
            cashCents: Long,
            cardCents: Long,
            subscriptionCents: Long,
        ): List<Float> {
            val cash = cashCents.coerceAtLeast(0L)
            val card = cardCents.coerceAtLeast(0L)
            val subscription = subscriptionCents.coerceAtLeast(0L)
            val total = cash + card + subscription
            val flexible = 1f - SEGMENT_COUNT * MIN_SEGMENT_SHARE

            fun weight(amount: Long): Float {
                val share = if (total > 0L) {
                    amount.toDouble() / total.toDouble()
                } else {
                    1.0 / SEGMENT_COUNT
                }
                return MIN_SEGMENT_SHARE + flexible * share.toFloat()
            }

            return listOf(weight(cash), weight(card), weight(subscription))
        }

        private const val SEGMENT_COUNT = 3
    }
}
