package com.mipatrimonio.app.core.backup

import com.mipatrimonio.app.core.database.entity.BudgetEntity
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.database.entity.ExchangeRateEntity
import com.mipatrimonio.app.core.database.entity.PersonEntity
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import com.mipatrimonio.app.core.database.entity.SharedShareEntity
import com.mipatrimonio.app.core.database.entity.TagEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.database.entity.TransactionTagCrossRef
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.RecurrencePeriod
import com.mipatrimonio.app.core.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject

/** Snapshot de todas las tablas para backup (ids originales incluidos para remapeo al restaurar). */
data class BackupSnapshot(
    val categories: List<CategoryEntity>,
    val cards: List<CardEntity>,
    val transactions: List<TransactionEntity>,
    val tags: List<TagEntity>,
    val transactionTags: List<TransactionTagCrossRef>,
    val recurring: List<RecurringEntity>,
    val savingsPlans: List<SavingsPlanEntity>,
    val planContributions: List<PlanContributionEntity>,
    val budgets: List<BudgetEntity>,
    val documents: List<DocumentEntity>,
    val exchangeRates: List<ExchangeRateEntity>,
    val people: List<PersonEntity>,
    val sharedExpenses: List<SharedExpenseEntity>,
    val sharedShares: List<SharedShareEntity>,
)

object BackupSerializer {

    fun snapshotToJson(s: BackupSnapshot): String {
        val root = JSONObject()
        root.put("categories", JSONArray().apply { s.categories.forEach { put(categoryToJson(it)) } })
        root.put("cards", JSONArray().apply { s.cards.forEach { put(cardToJson(it)) } })
        root.put("transactions", JSONArray().apply { s.transactions.forEach { put(txToJson(it)) } })
        root.put("tags", JSONArray().apply { s.tags.forEach { put(tagToJson(it)) } })
        root.put("transactionTags", JSONArray().apply { s.transactionTags.forEach { put(txTagToJson(it)) } })
        root.put("recurring", JSONArray().apply { s.recurring.forEach { put(recurringToJson(it)) } })
        root.put("savingsPlans", JSONArray().apply { s.savingsPlans.forEach { put(planToJson(it)) } })
        root.put("planContributions", JSONArray().apply { s.planContributions.forEach { put(contribToJson(it)) } })
        root.put("budgets", JSONArray().apply { s.budgets.forEach { put(budgetToJson(it)) } })
        root.put("documents", JSONArray().apply { s.documents.forEach { put(docToJson(it)) } })
        root.put("exchangeRates", JSONArray().apply { s.exchangeRates.forEach { put(rateToJson(it)) } })
        root.put("people", JSONArray().apply { s.people.forEach { put(personToJson(it)) } })
        root.put("sharedExpenses", JSONArray().apply { s.sharedExpenses.forEach { put(sharedExpenseToJson(it)) } })
        root.put("sharedShares", JSONArray().apply { s.sharedShares.forEach { put(sharedShareToJson(it)) } })
        return root.toString()
    }

    fun snapshotFromJson(json: String): BackupSnapshot {
        val root = JSONObject(json)
        return BackupSnapshot(
            categories = root.optJSONArray("categories").toList { categoryFromJson(it) },
            cards = root.optJSONArray("cards").toList { cardFromJson(it) },
            transactions = root.optJSONArray("transactions").toList { txFromJson(it) },
            tags = root.optJSONArray("tags").toList { tagFromJson(it) },
            transactionTags = root.optJSONArray("transactionTags").toList { txTagFromJson(it) },
            recurring = root.optJSONArray("recurring").toList { recurringFromJson(it) },
            savingsPlans = root.optJSONArray("savingsPlans").toList { planFromJson(it) },
            planContributions = root.optJSONArray("planContributions").toList { contribFromJson(it) },
            budgets = root.optJSONArray("budgets").toList { budgetFromJson(it) },
            documents = root.optJSONArray("documents").toList { docFromJson(it) },
            exchangeRates = root.optJSONArray("exchangeRates").toList { rateFromJson(it) },
            people = root.optJSONArray("people").toList { personFromJson(it) },
            sharedExpenses = root.optJSONArray("sharedExpenses").toList { sharedExpenseFromJson(it) },
            sharedShares = root.optJSONArray("sharedShares").toList { sharedShareFromJson(it) },
        )
    }

    fun metaToJson(schemaVersion: Int, dateIso: String, appVersion: String): String =
        JSONObject().apply {
            put("schemaVersion", schemaVersion)
            put("date", dateIso)
            put("appVersion", appVersion)
        }.toString()

    fun schemaVersionFromMeta(json: String): Int = JSONObject(json).optInt("schemaVersion", -1)

    private inline fun <T> JSONArray?.toList(map: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) optJSONObject(i)?.let { out.add(map(it)) }
        return out
    }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) optLong(key) else null
    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) optString(key) else null

    private fun categoryToJson(e: CategoryEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("colorHex", e.colorHex); put("icon", e.icon)
        put("isIncome", e.isIncome); put("isFixed", e.isFixed); put("isDefault", e.isDefault)
    }
    private fun categoryFromJson(o: JSONObject) = CategoryEntity(
        id = o.optLong("id"), name = o.optString("name"), colorHex = o.optString("colorHex", "#6FE3FF"),
        icon = o.optString("icon", ""), isIncome = o.optBoolean("isIncome"),
        isFixed = o.optBoolean("isFixed"), isDefault = o.optBoolean("isDefault"),
    )

    private fun cardToJson(e: CardEntity) = JSONObject().apply {
        put("id", e.id); put("bank", e.bank); put("variant", e.variant); put("kind", e.kind.name)
        put("alias", e.alias); put("theme", e.theme); put("photoPath", e.photoPath); put("createdAt", e.createdAt)
    }
    private fun cardFromJson(o: JSONObject) = CardEntity(
        id = o.optLong("id"), bank = o.optString("bank"), variant = o.optString("variant"),
        kind = runCatching { CardKind.valueOf(o.optString("kind")) }.getOrDefault(CardKind.DEBIT),
        alias = o.optString("alias"), theme = o.optString("theme", "cyan"),
        photoPath = o.optStringOrNull("photoPath"), createdAt = o.optLong("createdAt"),
    )

    private fun txToJson(e: TransactionEntity) = JSONObject().apply {
        put("id", e.id); put("type", e.type.name); put("incomeKind", e.incomeKind?.name)
        put("amountCents", e.amountCents); put("currency", e.currency); put("amountBaseCents", e.amountBaseCents)
        put("date", e.date); put("merchant", e.merchant); put("note", e.note)
        put("categoryId", e.categoryId); put("cardId", e.cardId); put("receiptPath", e.receiptPath)
        put("recurringId", e.recurringId); put("sharedExpenseId", e.sharedExpenseId); put("createdAt", e.createdAt)
    }
    private fun txFromJson(o: JSONObject) = TransactionEntity(
        id = o.optLong("id"),
        type = runCatching { TransactionType.valueOf(o.optString("type")) }.getOrDefault(TransactionType.CASH),
        incomeKind = o.optStringOrNull("incomeKind")?.let { runCatching { IncomeKind.valueOf(it) }.getOrNull() },
        amountCents = o.optLong("amountCents"), currency = o.optString("currency", "EUR"),
        amountBaseCents = o.optLong("amountBaseCents"), date = o.optLong("date"),
        merchant = o.optString("merchant"), note = o.optString("note"),
        categoryId = o.optLongOrNull("categoryId"), cardId = o.optLongOrNull("cardId"),
        receiptPath = o.optStringOrNull("receiptPath"), recurringId = o.optLongOrNull("recurringId"),
        sharedExpenseId = o.optLongOrNull("sharedExpenseId"), createdAt = o.optLong("createdAt"),
    )

    private fun tagToJson(e: TagEntity) = JSONObject().apply { put("id", e.id); put("name", e.name) }
    private fun tagFromJson(o: JSONObject) = TagEntity(id = o.optLong("id"), name = o.optString("name"))

    private fun txTagToJson(e: TransactionTagCrossRef) = JSONObject().apply {
        put("transactionId", e.transactionId); put("tagId", e.tagId)
    }
    private fun txTagFromJson(o: JSONObject) = TransactionTagCrossRef(
        transactionId = o.optLong("transactionId"), tagId = o.optLong("tagId"),
    )

    private fun recurringToJson(e: RecurringEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("type", e.type.name); put("amountCents", e.amountCents)
        put("period", e.period.name); put("nextDate", e.nextDate); put("cardId", e.cardId)
        put("categoryId", e.categoryId); put("isSubscription", e.isSubscription)
        put("remindDaysBefore", e.remindDaysBefore); put("active", e.active)
    }
    private fun recurringFromJson(o: JSONObject) = RecurringEntity(
        id = o.optLong("id"), name = o.optString("name"),
        type = runCatching { TransactionType.valueOf(o.optString("type")) }.getOrDefault(TransactionType.SUBSCRIPTION),
        amountCents = o.optLong("amountCents"),
        period = runCatching { RecurrencePeriod.valueOf(o.optString("period")) }.getOrDefault(RecurrencePeriod.MONTHLY),
        nextDate = o.optLong("nextDate"), cardId = o.optLongOrNull("cardId"), categoryId = o.optLongOrNull("categoryId"),
        isSubscription = o.optBoolean("isSubscription", true), remindDaysBefore = o.optInt("remindDaysBefore", 2),
        active = o.optBoolean("active", true),
    )

    private fun planToJson(e: SavingsPlanEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("targetCents", e.targetCents)
        put("monthlyQuotaCents", e.monthlyQuotaCents); put("priority", e.priority)
        put("isActive", e.isActive); put("initialEstimatedMonths", e.initialEstimatedMonths)
        put("lastNotifiedState", e.lastNotifiedState); put("lastNotifiedMonths", e.lastNotifiedMonths)
        put("createdAt", e.createdAt)
    }
    private fun planFromJson(o: JSONObject) = SavingsPlanEntity(
        id = o.optLong("id"), name = o.optString("name"), targetCents = o.optLong("targetCents"),
        monthlyQuotaCents = o.optLong("monthlyQuotaCents"), priority = o.optInt("priority"),
        isActive = o.optBoolean("isActive"), initialEstimatedMonths = o.optInt("initialEstimatedMonths"),
        lastNotifiedState = o.optString("lastNotifiedState", "NONE"),
        lastNotifiedMonths = o.optInt("lastNotifiedMonths", -1), createdAt = o.optLong("createdAt"),
    )

    private fun contribToJson(e: PlanContributionEntity) = JSONObject().apply {
        put("id", e.id); put("planId", e.planId); put("amountCents", e.amountCents); put("date", e.date)
    }
    private fun contribFromJson(o: JSONObject) = PlanContributionEntity(
        id = o.optLong("id"), planId = o.optLong("planId"),
        amountCents = o.optLong("amountCents"), date = o.optLong("date"),
    )

    private fun budgetToJson(e: BudgetEntity) = JSONObject().apply {
        put("id", e.id); put("categoryId", e.categoryId); put("limitCents", e.limitCents)
        put("notified80Month", e.notified80Month); put("notified100Month", e.notified100Month)
    }
    private fun budgetFromJson(o: JSONObject) = BudgetEntity(
        id = o.optLong("id"), categoryId = o.optLong("categoryId"), limitCents = o.optLong("limitCents"),
        notified80Month = o.optInt("notified80Month", -1), notified100Month = o.optInt("notified100Month", -1),
    )

    private fun docToJson(e: DocumentEntity) = JSONObject().apply {
        put("id", e.id); put("kind", e.kind.name); put("title", e.title)
        put("filePath", e.filePath); put("mimeType", e.mimeType); put("date", e.date)
        put("expiresAt", e.expiresAt); put("createdAt", e.createdAt)
    }
    private fun docFromJson(o: JSONObject) = DocumentEntity(
        id = o.optLong("id"),
        kind = runCatching { DocumentKind.valueOf(o.optString("kind")) }.getOrDefault(DocumentKind.OTHER),
        title = o.optString("title"), filePath = o.optString("filePath"),
        mimeType = o.optString("mimeType", "application/octet-stream"),
        date = o.optLong("date"), expiresAt = o.optLongOrNull("expiresAt"),
        createdAt = o.optLong("createdAt"),
    )

    private fun rateToJson(e: ExchangeRateEntity) = JSONObject().apply {
        put("currency", e.currency); put("rateToBase", e.rateToBase); put("updatedAt", e.updatedAt)
    }
    private fun rateFromJson(o: JSONObject) = ExchangeRateEntity(
        currency = o.optString("currency"), rateToBase = o.optDouble("rateToBase"), updatedAt = o.optLong("updatedAt"),
    )

    private fun personToJson(e: PersonEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("isMe", e.isMe)
    }
    private fun personFromJson(o: JSONObject) = PersonEntity(
        id = o.optLong("id"), name = o.optString("name"), isMe = o.optBoolean("isMe"),
    )

    private fun sharedExpenseToJson(e: SharedExpenseEntity) = JSONObject().apply {
        put("id", e.id); put("title", e.title); put("totalCents", e.totalCents)
        put("paidByPersonId", e.paidByPersonId); put("date", e.date)
    }
    private fun sharedExpenseFromJson(o: JSONObject) = SharedExpenseEntity(
        id = o.optLong("id"), title = o.optString("title"), totalCents = o.optLong("totalCents"),
        paidByPersonId = o.optLong("paidByPersonId"), date = o.optLong("date"),
    )

    private fun sharedShareToJson(e: SharedShareEntity) = JSONObject().apply {
        put("sharedExpenseId", e.sharedExpenseId); put("personId", e.personId)
        put("shareCents", e.shareCents); put("settled", e.settled)
    }
    private fun sharedShareFromJson(o: JSONObject) = SharedShareEntity(
        sharedExpenseId = o.optLong("sharedExpenseId"), personId = o.optLong("personId"),
        shareCents = o.optLong("shareCents"), settled = o.optBoolean("settled"),
    )
}
