package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mipatrimonio.app.core.database.entity.SharedShareEntity

@Dao
interface SharedShareDao {
    @Query("SELECT * FROM shared_shares")
    suspend fun getAll(): List<SharedShareEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SharedShareEntity>)

    @Query("DELETE FROM shared_shares")
    suspend fun deleteAll()
}
