package com.vishnu.trim

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).subscriptionDao()
    private val workManager = WorkManager.getInstance(application)
    val allSubscriptions: Flow<List<Subscription>> = dao.getAllSubscriptions()

    suspend fun getSubscriptionById(id: Int): Subscription? {
        return dao.getSubscriptionById(id)
    }

    fun addSubscription(name: String, cost: String, billingDate: String) {
        viewModelScope.launch {
            val subscription = Subscription(
                name = name,
                cost = cost.toDoubleOrNull() ?: 0.0,
                billingDate = billingDate
            )
            val id = dao.insertSubscription(subscription)
            scheduleNotification(subscription.copy(id = id.toInt()))
        }
    }

    fun updateSubscription(id: Int, name: String, cost: String, billingDate: String) {
        viewModelScope.launch {
            val subscription = Subscription(
                id = id,
                name = name,
                cost = cost.toDoubleOrNull() ?: 0.0,
                billingDate = billingDate
            )
            dao.insertSubscription(subscription)
            scheduleNotification(subscription)
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            dao.deleteSubscription(subscription.id)
            cancelNotification(subscription.id)
        }
    }

    fun scheduleNotification(sub: Subscription) {
        val delayInMinutes = calculateDelay(sub.billingDate)

        if (delayInMinutes > 0) {
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf("sub_name" to sub.name))
                .addTag(sub.id.toString())
                .build()

            workManager.enqueue(workRequest)
        }
    }

    private fun cancelNotification(subId: Int) {
        workManager.cancelAllWorkByTag(subId.toString())
    }

    private fun calculateDelay(billingDate: String): Long {
        return try {
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val date = sdf.parse(billingDate)
            val renewalTime = Calendar.getInstance().apply {
                if (date != null) {
                    time = date
                }
            }
            
            // Notification point: 48 hours before renewal
            val notificationPoint = renewalTime.timeInMillis - TimeUnit.HOURS.toMillis(48)
            val currentTime = System.currentTimeMillis()
            
            val delayMillis = notificationPoint - currentTime
            TimeUnit.MILLISECONDS.toMinutes(delayMillis)
        } catch (e: Exception) {
            -1L
        }
    }
}
