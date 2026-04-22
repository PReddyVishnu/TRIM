package com.vishnu.trim

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GmailScanner(private val context: Context) {

    private val client = OkHttpClient()

    // We search for recent emails containing these keywords
    private val query = "subject:(receipt OR invoice OR subscription) newer_than:30d"

    suspend fun scanForSubscriptions(accountEmail: String): List<String> = withContext(Dispatchers.IO) {
        val foundItems = mutableListOf<String>()
        try {
            // 1. Silently get the OAuth token for Gmail
            val scope = "oauth2:https://www.googleapis.com/auth/gmail.readonly"
            val token = GoogleAuthUtil.getToken(context, accountEmail, scope)

            // 2. Ask Gmail for a list of message IDs matching our query
            val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=$query"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.has("messages")) {
                    val messages = json.getJSONArray("messages")
                    
                    // 3. For MVP, we'll just pull the IDs and log them. 
                    // To get the actual prices, you would do a 2nd API call for each ID here.
                    for (i in 0 until Math.min(messages.length(), 5)) {
                        val msgId = messages.getJSONObject(i).getString("id")
                        Log.d("TRIM_GMAIL", "Found Receipt Email ID: $msgId")
                        foundItems.add(msgId)
                    }
                }
            } else {
                Log.e("TRIM_GMAIL", "API Failed: $responseBody")
            }
        } catch (e: Exception) {
            Log.e("TRIM_GMAIL", "Error scanning: ${e.message}")
        }
        return@withContext foundItems
    }
}
