package com.vishnu.trim

object RewardOptimizer {
    
    data class RewardTip(
        val message: String,
        val potentialYield: String
    )

    fun getOptimizationTip(category: String, subName: String): RewardTip? {
        val normalizedCategory = category.lowercase().trim()
        val normalizedName = subName.lowercase().trim()

        // Check for specific big-ticket items first
        if (normalizedName.contains("amazon prime")) {
            return RewardTip("Use Amazon Prime Store Card", "5% Back")
        }

        // Fall back to category-based routing
        return when {
            normalizedCategory.contains("entertainment") || 
            normalizedCategory.contains("streaming") -> {
                RewardTip("Use a 'Streaming' Multiplier Card", "Up to 6% Back")
            }
            normalizedCategory.contains("utilities") || 
            normalizedCategory.contains("telecom") -> {
                RewardTip("Use a 'Telecom/Utilities' Card", "Up to 5% Back")
            }
            normalizedCategory.contains("food") || 
            normalizedCategory.contains("delivery") -> {
                RewardTip("Use a 'Dining/Grocery' Card", "Up to 4% Back")
            }
            normalizedCategory.contains("software") || 
            normalizedCategory.contains("business") -> {
                RewardTip("Use a 'Business Services' Card", "Up to 5% Back")
            }
            else -> null // No specific high-yield tip available
        }
    }
}
