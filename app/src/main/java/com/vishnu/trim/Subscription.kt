package com.vishnu.trim

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val cost: Double,
    val billingDate: String,
    val isYearly: Boolean = false
)
