package com.mipatrimonio.app.core.backup

import android.content.Context
import androidx.room.withTransaction
import com.mipatrimonio.app.core.database.AppDatabase
import com.mipatrimonio.app.core.database.dao.BudgetDao
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.dao.ExchangeRateDao
import com.mipatrimonio.app.core.database.dao.PersonDao
import com.mipatrimonio.app.core.database.dao.RecurringDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.SharedExpenseDao
import com.mipatrimonio.app.core.database.dao.SharedShareDao
import com.mipatrimonio.app.core.database.dao.TagDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.security.CryptoManager
import com.mipatrimonio.app.core.storage.EncryptedDocumentStore
import com.mipatrimonio.app.core.storage.ReceiptImageStore
import com.mipatrimonio.app.feature.wallet.CardPhotoStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
    private val recurringDao: RecurringDao,
    private val savingsDao: SavingsDao,
    private val budgetDao: BudgetDao,
    private val documentDao: DocumentDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val personDao: PersonDao,
    private val sharedExpenseDao: SharedExpenseDao,
    private val sharedShareDao: SharedShareDao,
    private val cryptoManager: CryptoManager,
    private val documentStore: EncryptedDocumentStore,
    private val receiptStore: ReceiptImageStore,
) {

    @Throws(IOException::class)
    suspend fun exportToStream(password: CharArray, output: OutputStream) {
        require(password.size >= MIN_PASSWORD_LENGTH) { "PASSWORD_TOO_SHORT" }
        val snapshot = readSnapshot()
        val workDir = tempDir("backup-export")
        val zipFile = File(workDir, "payload.zip")
        try {
            writeZip(zipFile, snapshot)
            FileInputStream(zipFile).use { input ->
                BackupCrypto.encrypt(input, output, password)
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    @Throws(IOException::class)
    suspend fun importFromStream(password: CharArray, input: InputStream) {
        require(password.isNotEmpty()) { "PASSWORD_EMPTY" }
        val workDir = tempDir("backup-import")
        val zipFile = File(workDir, "payload.zip")
        try {
            FileOutputStream(zipFile).use { output ->
                BackupCrypto.decrypt(input, output, password)
            }
            val extracted = File(workDir, "extracted").also { it.mkdirs() }
            unzip(zipFile, extracted)
            val metaText = File(extracted, "meta.json").readText(Charsets.UTF_8)
            val schema = BackupSerializer.schemaVersionFromMeta(metaText)
            if (schema != BackupFormat.SCHEMA_VERSION && schema != db.openHelper.readableDatabase.version) {
                // Accept current DB version or backup format version 1
                if (schema != 1) throw IOException("SCHEMA_INCOMPATIBLE")
            }
            val dataText = File(extracted, "data.json").readText(Charsets.UTF_8)
            val snapshot = BackupSerializer.snapshotFromJson(dataText)
            restoreSnapshot(snapshot, File(extracted, "files"))
        } finally {
            workDir.deleteRecursively()
        }
    }

    fun defaultFileName(): String {
        val ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
            .withZone(ZoneOffset.UTC).format(Instant.now())
        return "mipatrimonio-$ts.${BackupFormat.EXTENSION}"
    }

    private suspend fun readSnapshot(): BackupSnapshot = BackupSnapshot(
        categories = categoryDao.getAll(),
        cards = cardDao.getAll(),
        transactions = transactionDao.getAll(),
        tags = tagDao.getAll(),
        transactionTags = tagDao.getAllCrossRefs(),
        recurring = recurringDao.getAll(),
        savingsPlans = savingsDao.getAllPlans(),
        planContributions = savingsDao.getAllContributions(),
        budgets = budgetDao.getAll(),
        documents = documentDao.getAll(),
        exchangeRates = exchangeRateDao.getAll(),
        people = personDao.getAll(),
        sharedExpenses = sharedExpenseDao.getAll(),
        sharedShares = sharedShareDao.getAll(),
    )

    @Throws(IOException::class)
    private fun writeZip(zipFile: File, snapshot: BackupSnapshot) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            putTextEntry(zos, "meta.json", BackupSerializer.metaToJson(
                schemaVersion = BackupFormat.SCHEMA_VERSION,
                dateIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                appVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                },
            ))
            putTextEntry(zos, "data.json", BackupSerializer.snapshotToJson(snapshot))

            snapshot.documents.forEach { doc ->
                if (doc.filePath.isNotBlank()) {
                    addKeystoreFile(zos, "files/${doc.filePath}", doc.filePath) { documentStore.openDecrypted(doc.filePath) }
                }
            }
            snapshot.transactions.forEach { tx ->
                val rp = tx.receiptPath
                if (!rp.isNullOrBlank()) {
                    addKeystoreFile(zos, "files/$rp", rp) {
                        cryptoManager.decrypt(FileInputStream(File(context.filesDir, rp)))
                    }
                }
            }
            snapshot.cards.forEach { card ->
                val pp = card.photoPath
                if (!pp.isNullOrBlank()) {
                    val f = File(pp)
                    if (f.isFile) {
                        val entryName = "files/card_photos/${f.name}"
                        zos.putNextEntry(ZipEntry(entryName))
                        FileInputStream(f).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    private fun putTextEntry(zos: ZipOutputStream, name: String, text: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(text.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun addKeystoreFile(zos: ZipOutputStream, entryName: String, relativePath: String, opener: () -> InputStream) {
        try {
            val file = File(context.filesDir, relativePath)
            if (!file.isFile) return
            zos.putNextEntry(ZipEntry(entryName))
            opener().use { it.copyTo(zos) }
            zos.closeEntry()
        } catch (_: Exception) {
            // Si un archivo no se puede leer, omitirlo para no bloquear todo el backup
        }
    }

    @Throws(IOException::class)
    private fun unzip(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            val base = destDir.canonicalFile
            while (entry != null) {
                val out = File(destDir, entry.name).canonicalFile
                if (!out.path.startsWith(base.path)) throw IOException("Entrada ZIP inválida")
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private suspend fun restoreSnapshot(snapshot: BackupSnapshot, filesDir: File) {
        // Preparar directorios de archivos (reemplazo total)
        runCatching { File(context.filesDir, "documents").deleteRecursively() }
        runCatching { File(context.filesDir, "receipts").deleteRecursively() }
        runCatching { File(context.noBackupFilesDir, "card_photos").deleteRecursively() }
        File(context.filesDir, "documents").mkdirs()
        File(context.filesDir, "receipts").mkdirs()
        File(context.noBackupFilesDir, "card_photos").mkdirs()
        restoreInTransaction(snapshot, filesDir)
    }

    private suspend fun restoreInTransaction(snapshot: BackupSnapshot, filesDir: File) {
        db.withTransaction {
            db.clearAllTables()

            // Categories
            val catMap = mutableMapOf<Long, Long>()
            snapshot.categories.forEach { old ->
                val newId = categoryDao.insert(old.copy(id = 0))
                catMap[old.id] = newId
            }
            // Cards (with photos)
            val cardMap = mutableMapOf<Long, Long>()
            snapshot.cards.forEach { old ->
                val newPhoto = restoreCardPhoto(old.photoPath, filesDir)
                val newId = cardDao.insert(old.copy(id = 0, photoPath = newPhoto))
                cardMap[old.id] = newId
            }
            // Tags
            val tagMap = mutableMapOf<Long, Long>()
            snapshot.tags.forEach { old ->
                val newId = tagDao.insert(old.copy(id = 0))
                tagMap[old.id] = newId
            }
            // People
            val personMap = mutableMapOf<Long, Long>()
            snapshot.people.forEach { old ->
                val newId = personDao.insertAll(listOf(old.copy(id = 0))).first()
                personMap[old.id] = newId
            }
            // Exchange rates (PK = currency)
            if (snapshot.exchangeRates.isNotEmpty()) exchangeRateDao.insertAll(snapshot.exchangeRates)

            // Recurring
            val recMap = mutableMapOf<Long, Long>()
            snapshot.recurring.forEach { old ->
                val mapped = old.copy(
                    id = 0,
                    cardId = old.cardId?.let { cardMap[it] },
                    categoryId = old.categoryId?.let { catMap[it] },
                )
                recMap[old.id] = recurringDao.insert(mapped)
            }
            // Savings plans & contributions
            val planMap = mutableMapOf<Long, Long>()
            snapshot.savingsPlans.forEach { old ->
                planMap[old.id] = savingsDao.insertPlan(old.copy(id = 0))
            }
            if (snapshot.planContributions.isNotEmpty()) {
                savingsDao.insertAllContributions(snapshot.planContributions.mapNotNull { c ->
                    val pid = planMap[c.planId] ?: return@mapNotNull null
                    c.copy(id = 0, planId = pid)
                })
            }
            // Documents (re-encrypt to Keystore)
            snapshot.documents.forEach { old ->
                val newPath = restoreKeystoreFile(old.filePath, filesDir, "documents") { bytes ->
                    documentStore.save(java.io.ByteArrayInputStream(bytes))
                }
                if (newPath != null) {
                    documentDao.insert(old.copy(id = 0, filePath = newPath))
                }
            }
            // Shared expenses & shares
            val seMap = mutableMapOf<Long, Long>()
            snapshot.sharedExpenses.forEach { old ->
                val payer = personMap[old.paidByPersonId] ?: old.paidByPersonId
                seMap[old.id] = sharedExpenseDao.insertAll(listOf(old.copy(id = 0, paidByPersonId = payer))).first()
            }
            if (snapshot.sharedShares.isNotEmpty()) {
                sharedShareDao.insertAll(snapshot.sharedShares.mapNotNull { s ->
                    val se = seMap[s.sharedExpenseId] ?: return@mapNotNull null
                    val p = personMap[s.personId] ?: return@mapNotNull null
                    s.copy(sharedExpenseId = se, personId = p)
                })
            }
            // Transactions
            val txMap = mutableMapOf<Long, Long>()
            snapshot.transactions.forEach { old ->
                val newReceipt = restoreKeystoreFile(old.receiptPath, filesDir, "receipts") { bytes ->
                    receiptStore.saveBytes(bytes)
                }
                val mapped = old.copy(
                    id = 0,
                    categoryId = old.categoryId?.let { catMap[it] },
                    cardId = old.cardId?.let { cardMap[it] },
                    recurringId = old.recurringId?.let { recMap[it] },
                    sharedExpenseId = old.sharedExpenseId?.let { seMap[it] },
                    receiptPath = newReceipt,
                )
                txMap[old.id] = transactionDao.insert(mapped)
            }
            // Transaction tags
            if (snapshot.transactionTags.isNotEmpty()) {
                tagDao.addCrossRefs(snapshot.transactionTags.mapNotNull { ref ->
                    val tx = txMap[ref.transactionId] ?: return@mapNotNull null
                    val tag = tagMap[ref.tagId] ?: return@mapNotNull null
                    com.mipatrimonio.app.core.database.entity.TransactionTagCrossRef(tx, tag)
                })
            }
            // Budgets
            if (snapshot.budgets.isNotEmpty()) {
                budgetDao.insertAll(snapshot.budgets.mapNotNull { b ->
                    val cat = catMap[b.categoryId] ?: return@mapNotNull null
                    b.copy(id = 0, categoryId = cat)
                })
            }
        }
    }

    private fun restoreCardPhoto(oldPath: String?, filesDir: File): String? {
        if (oldPath.isNullOrBlank()) return null
        val name = File(oldPath).name
        val src = File(filesDir, "card_photos/$name")
        if (!src.isFile) return null
        return try {
            CardPhotoStore.saveBytes(context, src.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    private fun restoreKeystoreFile(oldPath: String?, filesDir: File, dir: String, saver: (ByteArray) -> String): String? {
        if (oldPath.isNullOrBlank()) return null
        val name = File(oldPath).name
        val src = File(filesDir, "$dir/$name")
        if (!src.isFile) return null
        return try {
            saver(src.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    private fun tempDir(prefix: String): File =
        File(context.cacheDir, "$prefix-${System.currentTimeMillis()}").also { it.mkdirs() }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
