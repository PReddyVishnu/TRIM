package com.vishnu.trim

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TrimWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.failure()

        try {
            // Fetch user's subscriptions
            val snapshot = db.collection("users").document(userId)
                .collection("subscriptions").get().await()

            val notificationHelper = NotificationHelper(applicationContext)
            val currentTime = System.currentTimeMillis()

            snapshot.documents.forEachIndexed { index, doc ->
                val name = doc.getString("name") ?: "Subscription"
                val price = doc.getDouble("cost") ?: 0.0
                val isFreeTrial = doc.getBoolean("isFreeTrial") ?: false
                val trialEndDate = doc.getLong("trialEndDate")

                if (isFreeTrial && trialEndDate != null) {
                    // FREE TRIAL LOGIC
                    val timeLeftMillis = trialEndDate - currentTime
                    val hoursLeft = timeLeftMillis / (1000 * 60 * 60)

                    // If there are less than 48 hours left, SOUND THE ALARM
                    if (hoursLeft in 0..48) { 
                        notificationHelper.showNotification(
                            title = "🚨 URGENT: $name Trial Ending!",
                            message = "You will be charged $$price very soon. Open TRIM to cancel it now.",
                            notificationId = index + 1000 // Offset ID to avoid overlapping notifications
                        )
                    }
                } else {
                    // YOUR EXISTING STANDARD 7-DAY RENEWAL LOGIC
                    val addedAt = doc.getLong("addedAt") ?: currentTime
                    val timeSinceAdded = currentTime - addedAt
                    val daysSinceAdded = timeSinceAdded / (1000 * 60 * 60 * 24)

                    // If we are at day 23 of a 30 day cycle (7 days left)
                    if (daysSinceAdded % 30 in 23L..24L) { 
                        notificationHelper.showNotification(
                            title = "Upcoming Charge: $name",
                            message = "Your card will be charged $$price in 7 days. Trim it?",
                            notificationId = index
                        )
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
