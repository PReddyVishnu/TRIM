package com.vishnu.trim

data class OverlapResult(
    val utilityCategory: String,
    val overlappingSubs: List<String>,
    val potentialSavings: Double
)

object OverlapDetector {
    // Maps the service to its core utility
    private val utilityMap = mapOf(
        "spotify" to "Music Streaming",
        "youtube premium" to "Music Streaming",
        "apple music" to "Music Streaming",
        "netflix" to "General Entertainment",
        "hulu" to "General Entertainment",
        "disney+" to "Kids & Family Animation",
        "crunchyroll" to "Kids & Family Animation",
        "adobe creative cloud" to "Content Creation",
        "canva pro" to "Content Creation",
        "toon boom" to "Content Creation"
    )

    fun findOverlaps(subs: List<Subscription>): List<OverlapResult> {
        val overlaps = mutableListOf<OverlapResult>()
        
        // Group user's active subscriptions by our internal utility map
        val groupedByUtility = subs.groupBy { sub ->
            val queryName = sub.name.lowercase().trim()
            utilityMap.entries.firstOrNull { queryName.contains(it.key) }?.value ?: "Unknown"
        }.filterKeys { it != "Unknown" }

        // If a category has more than 1 subscription, it's a leak.
        groupedByUtility.forEach { (category, subList) ->
            if (subList.size > 1) {
                // Calculate savings by assuming they keep the most expensive one and trim the rest
                val sortedByPrice = subList.sortedByDescending { it.cost }
                val redundantSubs = sortedByPrice.drop(1) 
                val savings = redundantSubs.sumOf { it.cost }

                overlaps.add(
                    OverlapResult(
                        utilityCategory = category,
                        overlappingSubs = subList.map { it.name },
                        potentialSavings = savings
                    )
                )
            }
        }
        return overlaps
    }
}
