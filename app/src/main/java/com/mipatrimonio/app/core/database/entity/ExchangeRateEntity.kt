package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currency: String,
    val rateToBase: Double,
    val updatedAt: Long
)