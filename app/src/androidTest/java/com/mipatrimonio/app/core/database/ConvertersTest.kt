package com.mipatrimonio.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.RecurrencePeriod
import com.mipatrimonio.app.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ConvertersTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun localDateToEpochDayAndBack() = runBlocking {
        val date = LocalDate.of(2026, 11, 3)
        val epoch = date.toEpochDay()
        val dao = db.transactionDao()
        val tx = com.mipatrimonio.app.core.database.entity.TransactionEntity(
            type = TransactionType.CASH,
            amountCents = 1000,
            amountBaseCents = 1000,
            date = epoch,
            merchant = "Test",
            createdAt = System.currentTimeMillis()
        )
        val id = dao.insert(tx)
        val read = dao.observeAll().first().find { it.id == id }
        assertNotNull(read)
        assertEquals(date, LocalDate.ofEpochDay(read!!.date))
    }

    @Test
    fun enumRoundTripViaTransaction() = runBlocking {
        val dao = db.transactionDao()
        val epoch = LocalDate.of(2026, 11, 3).toEpochDay()
        val tx = com.mipatrimonio.app.core.database.entity.TransactionEntity(
            type = TransactionType.SUBSCRIPTION,
            incomeKind = IncomeKind.OTHER,
            amountCents = 5000,
            amountBaseCents = 5000,
            date = epoch,
            merchant = "Netflix",
            createdAt = System.currentTimeMillis()
        )
        val id = dao.insert(tx)
        val read = dao.observeAll().first().find { it.id == id }
        assertNotNull(read)
        assertEquals(TransactionType.SUBSCRIPTION, read!!.type)
        assertEquals(IncomeKind.OTHER, read.incomeKind)
    }

    @Test
    fun cardKindRoundTrip() = runBlocking {
        val dao = db.cardDao()
        val card = com.mipatrimonio.app.core.database.entity.CardEntity(
            bank = "TestBank",
            kind = CardKind.CREDIT,
            alias = "Test",
            theme = "cyan",
            createdAt = System.currentTimeMillis()
        )
        val id = dao.insert(card)
        val read = dao.getById(id)
        assertNotNull(read)
        assertEquals(CardKind.CREDIT, read!!.kind)
    }

    @Test
    fun documentKindRoundTrip() = runBlocking {
        val dao = db.documentDao()
        val doc = com.mipatrimonio.app.core.database.entity.DocumentEntity(
            kind = DocumentKind.INVOICE,
            title = "Factura",
            filePath = "/path",
            mimeType = "application/pdf",
            date = LocalDate.of(2026, 1, 1).toEpochDay(),
            createdAt = System.currentTimeMillis()
        )
        val id = dao.insert(doc)
        val read = dao.getById(id)
        assertNotNull(read)
        assertEquals(DocumentKind.INVOICE, read!!.kind)
    }

    @Test
    fun recurrencePeriodRoundTrip() = runBlocking {
        val dao = db.recurringDao()
        val rec = com.mipatrimonio.app.core.database.entity.RecurringEntity(
            name = "Test",
            type = TransactionType.CARD,
            amountCents = 1000,
            period = RecurrencePeriod.QUARTERLY,
            nextDate = LocalDate.of(2026, 12, 1).toEpochDay(),
            isSubscription = false,
            active = true
        )
        val id = dao.insert(rec)
        val read = dao.getById(id)
        assertNotNull(read)
        assertEquals(RecurrencePeriod.QUARTERLY, read!!.period)
    }
}