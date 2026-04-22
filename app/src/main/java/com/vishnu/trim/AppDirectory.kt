package com.vishnu.trim

object AppDirectory {
    // Maps the user's plain-text subscription name to the Android Package ID
    val packageMap = mapOf(
        "netflix" to "com.netflix.mediaclient",
        "spotify" to "com.spotify.music",
        "disney+" to "com.disney.disneyplus",
        "hulu" to "com.hulu.plus",
        "amazon prime" to "com.amazon.avod.thirdpartyclient", // Prime Video
        "duolingo" to "com.duolingo",
        "youtube" to "com.google.android.youtube",
        "hbo max" to "com.hbo.hbonow",
        "crunchyroll" to "com.crunchyroll.crunchyroid"
    )

    fun getPackageName(serviceName: String): String? {
        val query = serviceName.lowercase().trim()
        return packageMap.entries.firstOrNull { query.contains(it.key) }?.value
    }
}
