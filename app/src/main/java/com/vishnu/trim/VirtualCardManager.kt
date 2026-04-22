package com.vishnu.trim

import android.content.Context
import android.content.Intent
import android.net.Uri

object VirtualCardManager {
    
    // Direct link to Privacy.com's card creation flow
    private const val PRIVACY_WEB_URL = "https://privacy.com/virtual-card"

    fun launchCardCreator(context: Context, subscriptionName: String, monthlyLimit: Double) {
        // We craft a URL that helps the user understand the intent
        // In a full API integration, we'd pass the limit here.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_WEB_URL))
        context.startActivity(intent)
    }
}
