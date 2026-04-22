package com.vishnu.trim

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object CancellationManager {
    
    // Deep links to the exact cancellation "Step 1" pages
    private val cancellationDirectory = mapOf(
        "netflix" to "https://www.netflix.com/cancelplan",
        "spotify" to "https://www.spotify.com/account/subscription/cancel/",
        "amazon prime" to "https://www.amazon.com/mc/pipelines/cancellation/v2",
        "disney+" to "https://www.disneyplus.com/account/subscription",
        "hulu" to "https://secure.hulu.com/account/cancel",
        "youtube premium" to "https://www.youtube.com/paid_memberships",
        "apple music" to "https://music.apple.com/account/subscriptions",
        "adobe" to "https://account.adobe.com/plans",
        "canva" to "https://www.canva.com/settings/billing-and-teams",
        "duolingo" to "https://www.duolingo.com/settings/plus"
    )

    fun launchKillSwitch(context: Context, serviceName: String) {
        val query = serviceName.lowercase().trim()
        
        // Find the direct link or fall back to a specific Google Search
        val url = cancellationDirectory.entries.find { query.contains(it.key) }?.value 
            ?: "https://www.google.com/search?q=how+to+cancel+${Uri.encode(serviceName)}+subscription"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No browser found to launch Kill Switch", Toast.LENGTH_SHORT).show()
        }
    }
}
