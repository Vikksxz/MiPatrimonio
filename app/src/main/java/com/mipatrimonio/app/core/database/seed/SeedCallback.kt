package com.mipatrimonio.app.core.database.seed

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Callback para sembrar categorías en la primera creación de la BD.
 * Inserta las 12 categorías con nombres traducidos usando Context.
 */
class SeedCallback(private val context: Context) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            // Verificar si ya hay categorías
            val count = db.query("SELECT COUNT(*) FROM categories").use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }
            if (count == 0L) {
                val categories = DefaultCategories.ALL.map { seed ->
                    CategoryEntity(
                        name = context.getString(seed.nameRes),
                        colorHex = seed.colorHex,
                        icon = seed.icon,
                        isIncome = seed.isIncome,
                        isFixed = seed.isFixed,
                        isDefault = seed.isDefault
                    )
                }
                // Insertar en transacción
                db.beginTransaction()
                try {
                    val stmt = db.compileStatement(
                        "INSERT INTO categories (name, colorHex, icon, isIncome, isFixed, isDefault) VALUES (?, ?, ?, ?, ?, ?)"
                    )
                    categories.forEach { cat ->
                        stmt.bindString(1, cat.name)
                        stmt.bindString(2, cat.colorHex)
                        stmt.bindString(3, cat.icon)
                        stmt.bindLong(4, if (cat.isIncome) 1 else 0)
                        stmt.bindLong(5, if (cat.isFixed) 1 else 0)
                        stmt.bindLong(6, if (cat.isDefault) 1 else 0)
                        stmt.executeInsert()
                        stmt.clearBindings()
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}