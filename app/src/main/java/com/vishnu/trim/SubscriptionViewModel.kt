package com.vishnu.trim

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).subscriptionDao()
    private val workManager = WorkManager.getInstance(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    val allSubscriptions: Flow<List<Subscription>> = dao.getAllSubscriptions()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    val filteredSubscriptions = combine(allSubscriptions, searchQuery, selectedFilter) { subs, query, filter ->
        subs.filter { sub ->
            val matchesQuery = sub.name.contains(query, ignoreCase = true)
            val matchesFilter = filter == "All" || sub.category == filter
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChange(newFilter: String) {
        _selectedFilter.value = newFilter
    }

    val monthlyProjection: Flow<Double> = allSubscriptions.map { subs ->
        subs.sumOf { sub ->
            if (sub.isYearly) sub.cost / 12 else sub.cost
        }
    }

    val yearlyProjection: Flow<Double> = monthlyProjection.map { it * 12 }

    val categorySpending: Flow<Map<String, Double>> = allSubscriptions.map { subs ->
        subs.groupBy { it.category }.mapValues { entry ->
            entry.value.sumOf { if (it.isYearly) it.cost / 12 else it.cost }
        }
    }

    suspend fun getSubscriptionById(id: Int): Subscription? {
        return dao.getSubscriptionById(id)
    }

    fun addSubscription(name: String, cost: String, billingDate: String, isYearly: Boolean, category: String) {
        viewModelScope.launch {
            val subscription = Subscription(
                name = name,
                cost = cost.toDoubleOrNull() ?: 0.0,
                billingDate = billingDate,
                isYearly = isYearly,
                category = category
            )
            val id = dao.insertSubscription(subscription)
            val subWithId = subscription.copy(id = id.toInt())
            scheduleNotification(subWithId)
            backupToCloud(subWithId)
        }
    }

    fun updateSubscription(id: Int, name: String, cost: String, billingDate: String, isYearly: Boolean, category: String) {
        viewModelScope.launch {
            val subscription = Subscription(
                id = id,
                name = name,
                cost = cost.toDoubleOrNull() ?: 0.0,
                billingDate = billingDate,
                isYearly = isYearly,
                category = category
            )
            dao.insertSubscription(subscription)
            scheduleNotification(subscription)
            backupToCloud(subscription)
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            dao.deleteSubscription(subscription.id)
            cancelNotification(subscription.id)
            removeFromCloud(subscription)
        }
    }

    private fun backupToCloud(sub: Subscription) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("subscriptions").document(sub.id.toString())
            .set(sub)
    }

    private fun removeFromCloud(sub: Subscription) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("subscriptions").document(sub.id.toString())
            .delete()
    }

    fun restoreFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("subscriptions")
            .get()
            .addOnSuccessListener { documents ->
                viewModelScope.launch {
                    for (document in documents) {
                        val sub = document.toObject(Subscription::class.java)
                        dao.insertSubscription(sub)
                        scheduleNotification(sub)
                    }
                }
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

    fun triggerTestNotification(name: String) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES) // Fire in 60 seconds
            .setInputData(workDataOf("sub_name" to "$name (Test)"))
            .build()
        workManager.enqueue(workRequest)
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
