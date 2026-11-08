package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.feature.widget.WidgetUpdater
import javax.inject.Inject

/**
 * Caso de uso `DeleteTransaction` (T1.8): borra un movimiento desde la lista
 * (deslizar) o desde la hoja de edición.
 */
class DeleteTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val widgetUpdater: WidgetUpdater,
) {

    suspend operator fun invoke(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
        widgetUpdater.update()
    }
}

/**
 * Caso de uso `RestoreTransaction` (T1.8): deshace un borrado reinsertando el
 * movimiento con su id original.
 */
class RestoreTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val widgetUpdater: WidgetUpdater,
) {

    suspend operator fun invoke(transaction: TransactionEntity) {
        transactionDao.restore(transaction)
        widgetUpdater.update()
    }
}
