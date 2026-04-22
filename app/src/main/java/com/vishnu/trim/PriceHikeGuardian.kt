package com.vishnu.trim

object PriceHikeGuardian {
    
    fun detectHike(oldPrice: Double, newPrice: Double): PriceStatus {
        return when {
            newPrice > oldPrice -> PriceStatus.HIKE
            newPrice < oldPrice -> PriceStatus.DROP
            else -> PriceStatus.STABLE
        }
    }

    enum class PriceStatus { HIKE, DROP, STABLE }
}
