package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mipatrimonio.app.core.database.entity.PersonEntity

@Dao
interface PersonDao {
    @Query("SELECT * FROM people")
    suspend fun getAll(): List<PersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PersonEntity>): List<Long>

    @Query("DELETE FROM people")
    suspend fun deleteAll()
}
