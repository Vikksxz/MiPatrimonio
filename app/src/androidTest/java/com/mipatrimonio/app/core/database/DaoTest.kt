package com.mipatrimonio.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.BudgetEntity
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.database.entity.TagEntity
import com.mipatrimonio.app.core.database.entity.TransactionTagCrossRef
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.RecurrencePeriod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DaoTest {

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
    fun transactionDaoInsertAndQuery() = runBlocking {
        val dao = db.transactionDao()
        val epoch = LocalDate.of(2026, 11, 3).toEpochDay()
        val tx = TransactionEntity(
            type = TransactionType.CASH,
            amountCents = 1234,
            amountBaseCents = 1234,
            date = epoch,
            merchant = "Mercadona",
            createdAt = System.currentTimeMillis()
        )
        val id = dao.insert(tx)
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(1234, all[0].amountCents)
        assertEquals("Mercadona", all[0].merchant)
    }

    @Test
    fun transactionDaoDeleteCardAndNullify() = runBlocking {
        val cardDao = db.cardDao()
        val txDao = db.transactionDao()
        val card = CardEntity(bank = "Test", kind = CardKind.DEBIT, alias = "A", theme = "cyan", createdAt = System.currentTimeMillis())
        val cardId = cardDao.insert(card)
        val epoch = LocalDate.of(2026, 11, 3).toEpochDay()
        val tx = TransactionEntity(type = TransactionType.CARD, amountCents = 1000, amountBaseCents = 1000, date = epoch, merchant = "Test", cardId = cardId, createdAt = System.currentTimeMillis())
        txDao.insert(tx)
        txDao.deleteCardAndNullify(cardId)
        val read = txDao.observeAll().first().find { it.merchant == "Test" }
        assertNotNull(read)
        assertNull(read!!.cardId)
    }

    @Test
    fun categoryDaoReassignOnDelete() = runBlocking {
        val catDao = db.categoryDao()
        val txDao = db.transactionDao()
        val cat1 = CategoryEntity(name = "Cat1", colorHex = "#FF0000", icon = "icon", isIncome = false)
        val cat2 = CategoryEntity(name = "Cat2", colorHex = "#00FF00", icon = "icon", isIncome = false)
        val id1 = catDao.insert(cat1)
        val id2 = catDao.insert(cat2)
        val epoch = LocalDate.of(2026, 11, 3).toEpochDay()
        val tx = TransactionEntity(type = TransactionType.CASH, amountCents = 1000, amountBaseCents = 1000, date = epoch, merchant = "Test", categoryId = id1, createdAt = System.currentTimeMillis())
        txDao.insert(tx)
        catDao.deleteAndReassign(id1, id2)
        val read = txDao.observeAll().first().find { it.merchant == "Test" }
        assertEquals(id2, read!!.categoryId)
    }

    @Test
    fun savingsDaoDeletePlanWithContributions() = runBlocking {
        val savingsDao = db.savingsDao()
        val plan = SavingsPlanEntity(
            name = "Viaje",
            targetCents = 100000,
            monthlyQuotaCents = 10000,
            priority = 1,
            isActive = true,
            initialEstimatedMonths = 10,
            createdAt = System.currentTimeMillis()
        )
        val planId = savingsDao.insertPlan(plan)
        val contrib = PlanContributionEntity(planId = planId, amountCents = 5000, date = LocalDate.of(2026, 11, 1).toEpochDay())
        savingsDao.insertContribution(contrib)
        savingsDao.deletePlanWithContributions(planId)
        val plans = savingsDao.observePlans().first()
        assertTrue(plans.isEmpty())
        val contribs = savingsDao.observeContributions(planId).first()
        assertTrue(contribs.isEmpty())
    }

    @Test
    fun savingsDaoSumContributedWithNegative() = runBlocking {
        val savingsDao = db.savingsDao()
        val plan = SavingsPlanEntity(
            name = "Test",
            targetCents = 10000,
            monthlyQuotaCents = 1000,
            priority = 1,
            isActive = true,
            initialEstimatedMonths = 10,
            createdAt = System.currentTimeMillis()
        )
        val planId = savingsDao.insertPlan(plan)
        savingsDao.insertContribution(PlanContributionEntity(planId = planId, amountCents = 5000, date = LocalDate.of(2026, 1, 1).toEpochDay()))
        savingsDao.insertContribution(PlanContributionEntity(planId = planId, amountCents = -2000, date = LocalDate.of(2026, 2, 1).toEpochDay()))
        val sum = savingsDao.sumContributed(planId).first()
        assertEquals(3000L, sum!!)
    }

    @Test
    fun recurringDaoGetDue() = runBlocking {
        val dao = db.recurringDao()
        val today = LocalDate.now().toEpochDay()
        val rec = RecurringEntity(
            name = "Suscripción",
            type = TransactionType.SUBSCRIPTION,
            amountCents = 999,
            period = RecurrencePeriod.MONTHLY,
            nextDate = today,
            isSubscription = true,
            active = true
        )
        dao.insert(rec)
        val due = dao.getDue(today)
        assertEquals(1, due.size)
        assertEquals("Suscripción", due[0].name)
    }

    @Test
    fun budgetDaoObserveWithCategory() = runBlocking {
        val catDao = db.categoryDao()
        val budgetDao = db.budgetDao()
        val cat = CategoryEntity(name = "TestCat", colorHex = "#FF0000", icon = "icon", isIncome = false)
        val catId = catDao.insert(cat)
        val budget = BudgetEntity(categoryId = catId, limitCents = 50000)
        budgetDao.upsert(budget)
        val withCat = budgetDao.observeWithCategory().first()
        assertEquals(1, withCat.size)
        assertEquals("TestCat", withCat[0].category?.name)
    }

    @Test
    fun documentDaoObserveExpiringBefore() = runBlocking {
        val dao = db.documentDao()
        val future = LocalDate.now().plusDays(10).toEpochDay()
        val doc = DocumentEntity(
            kind = DocumentKind.WARRANTY,
            title = "Garantía TV",
            filePath = "/path",
            mimeType = "application/pdf",
            date = LocalDate.now().toEpochDay(),
            expiresAt = future,
            createdAt = System.currentTimeMillis()
        )
        dao.insert(doc)
        val expiring = dao.observeExpiringBefore(LocalDate.now().plusDays(15).toEpochDay()).first()
        assertEquals(1, expiring.size)
    }

    @Test
    fun tagDaoCrossRef() = runBlocking {
        val tagDao = db.tagDao()
        val tag = TagEntity(name = "tag1")
        val tagId = tagDao.insert(tag)
        val crossRef = TransactionTagCrossRef(transactionId = 1, tagId = tagId)
        tagDao.addCrossRef(crossRef)
        val tags = tagDao.observeTagsForTransaction(1).first()
        assertEquals(1, tags.size)
        assertEquals("tag1", tags[0].name)
        tagDao.removeCrossRef(crossRef)
        val after = tagDao.observeTagsForTransaction(1).first()
        assertTrue(after.isEmpty())
    }
}