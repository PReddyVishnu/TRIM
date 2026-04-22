package com.vishnu.trim

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object CancellationHelper {
    
    // The "Hidden" URLs companies don't want you to find easily
    private val cancelUrls = mapOf(
        "netflix" to "https://www.netflix.com/cancelplan",
        "spotify" to "https://www.spotify.com/account/cancel/",
        "amazon prime" to "https://www.amazon.com/mc/pipelines/cancellation/v2",
        "disney+" to "https://www.disneyplus.com/account/subscription",
        "hulu" to "https://secure.hulu.com/account/cancel",
        "apple music" to "https://music.apple.com/account/subscriptions",
        "adobe" to "https://account.adobe.com/plans",
        "canva" to "https://www.canva.com/settings/billing-and-teams"
    )

    fun openCancellationPage(context: Context, serviceName: String) {
        // Make it lowercase to ensure it matches our map
        val queryName = serviceName.lowercase().trim()
        
        // Find the URL, or default to a Google search on how to cancel it
        val url = cancelUrls.entries.firstOrNull { queryName.contains(it.key) }?.value 
            ?: "https://www.google.com/search?q=how+to+cancel+$queryName+subscription"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }
}
