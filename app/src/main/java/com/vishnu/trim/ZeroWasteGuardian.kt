package com.vishnu.trim

object ZeroWasteGuardian {
    
    // Maps services to their functional "Utility"
    private val utilityMap = mapOf(
        "netflix" to "Premium Video",
        "hulu" to "Premium Video",
        "disney+" to "Premium Video",
        "hbo max" to "Premium Video",
        "spotify" to "Music Streaming",
        "youtube premium" to "Music Streaming",
        "apple music" to "Music Streaming",
        "canva" to "Design Tools",
        "adobe" to "Design Tools"
    )

    fun identifyRedundancies(subs: List<Subscription>): List<RedundancyGroup> {
        return subs.groupBy { sub -> 
            utilityMap.entries.find { sub.name.lowercase().contains(it.key) }?.value ?: "Other" 
        }
        .filter { it.key != "Other" && it.value.size > 1 }
        .map { (utility, overlappingSubs) ->
            RedundancyGroup(
                utility = utility,
                services = overlappingSubs.map { it.name },
                potentialMonthlySavings = overlappingSubs.sortedByDescending { it.cost }.drop(1).sumOf { it.cost }
            )
        }
    }
}

data class RedundancyGroup(
    val utility: String,
    val services: List<String>,
    val potentialMonthlySavings: Double
)
