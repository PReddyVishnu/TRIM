package com.vishnu.trim

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionName: String,
    val price: Double,
    val currency: String = "$",
    val timestamp: Long = System.currentTimeMillis()
)
