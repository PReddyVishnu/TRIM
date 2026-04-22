package com.vishnu.trim

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import android.content.Context
import androidx.glance.appwidget.updateAll
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

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions = _subscriptions.asStateFlow()

    private val _categoryTotals = MutableStateFlow<Map<String, Float>>(emptyMap())
    val categoryTotals: StateFlow<Map<String, Float>> = _categoryTotals

    private val _totalTrimmed = MutableStateFlow(0.0)
    val totalTrimmed: StateFlow<Double> = _totalTrimmed

    fun fetchStats() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("profile").document("stats")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && snapshot.exists()) {
                    val total = snapshot.getDouble("totalTrimmed") ?: 0.0
                    _totalTrimmed.value = total
                }
            }
    }

    fun fetchSubscriptions() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("subscriptions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ViewModel", "Firestore Listen Failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val subsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Subscription::class.java)
                    }
                    _subscriptions.value = subsList
                    calculateCategoryTotals(subsList)

                    // NEW: Sync daily burn to the Home Screen Widget
                    viewModelScope.launch {
                        val totalMonthly = subsList.sumOf { if (it.isYearly) it.cost / 12 else it.cost }
                        syncDailyBurnToWidget(getApplication<android.app.Application>().applicationContext, totalMonthly)
                    }
                }
            }
    }

    fun calculateCategoryTotals(subsList: List<Subscription>) {
        val totals = mutableMapOf<String, Float>()
        subsList.forEach { sub ->
            val currentTotal = totals[sub.category] ?: 0f
            totals[sub.category] = currentTotal + sub.cost.toFloat()
        }
        _categoryTotals.value = totals
    }

    fun deleteSubscription(sub: Subscription) {
        val uid = auth.currentUser?.uid ?: return
        
        // Step A: Add the price to the user's 'Total Trimmed' stats
        val statsRef = firestore.collection("users").document(uid).collection("profile").document("stats")
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(statsRef)
            val currentTotal = if (snapshot.exists()) snapshot.getDouble("totalTrimmed") ?: 0.0 else 0.0
            transaction.set(statsRef, hashMapOf("totalTrimmed" to currentTotal + sub.cost))
        }

        viewModelScope.launch {
            dao.deleteSubscription(sub.id)
            cancelNotification(sub.id)
            
            // Step B: Actually delete the subscription from Firestore
            firestore.collection("users").document(uid)
                .collection("subscriptions").document(sub.id.toString())
                .delete()
                .addOnFailureListener { e ->
                    Log.e("ViewModel", "Error deleting document", e)
                }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    val filteredSubscriptions = combine(subscriptions, searchQuery, selectedFilter) { subs, query, filter ->
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

    val monthlyProjection: Flow<Double> = subscriptions.map { subs ->
        subs.sumOf { sub ->
            if (sub.isYearly) sub.cost / 12 else sub.cost
        }
    }

    val yearlyProjection: Flow<Double> = monthlyProjection.map { it * 12 }

    val dailyBurn: Flow<Double> = monthlyProjection.map { it / 30.0 }
    
    val fiveYearProjection: Flow<Double> = yearlyProjection.map { it * 5 }

    val projectedWealth30Years: StateFlow<Double> = monthlyProjection.map { monthlyTotal ->
        val annualReturn = 0.08 // 8% average market return
        val months = 30 * 12
        val monthlyRate = annualReturn / 12
        
        // Compound Interest Formula for monthly contributions
        if (monthlyRate > 0) {
            monthlyTotal * ((Math.pow(1 + monthlyRate, months.toDouble()) - 1) / monthlyRate)
        } else {
            monthlyTotal * months.toDouble()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpending: Flow<Map<String, Double>> = subscriptions.map { subs ->
        subs.groupBy { it.category }.mapValues { entry ->
            entry.value.sumOf { if (it.isYearly) it.cost / 12 else it.cost }
        }
    }

    val highestSpendingCategory: Flow<Pair<String, Double>?> = categorySpending.map { 
        it.maxByOrNull { entry -> entry.value }?.toPair()
    }

    val redundancyAlerts: StateFlow<List<RedundancyGroup>> = subscriptions.map { subsList ->
        ZeroWasteGuardian.identifyRedundancies(subsList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptionHealthScore: StateFlow<Int> = subscriptions.map { subsList ->
        if (subsList.isEmpty()) return@map 100
        
        var score = 100
        val redundancies = ZeroWasteGuardian.identifyRedundancies(subsList)
        
        // Penalize for redundancies
        score -= (redundancies.size * 15)
        
        // Penalize for high "Daily Burn" (relative metric)
        val monthly = subsList.sumOf { if (it.isYearly) it.cost / 12 else it.cost }
        if (monthly > 100) score -= 10
        if (monthly > 250) score -= 20
        
        // Penalize for "Dead Weight" (if usage permission is granted)
        if (usageTracker.hasUsagePermission()) {
            val deadWeightCount = subsList.count { sub ->
                val pkg = AppDirectory.getPackageName(sub.name)
                pkg != null && usageTracker.getHoursUsedInLast30Days(pkg) < 1.0
            }
            score -= (deadWeightCount * 10)
        }
        
        score.coerceIn(0, 100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    private val usageTracker = UsageTracker(application)
    private val _deadWeightApps = MutableStateFlow<List<String>>(emptyList())
    val deadWeightApps: StateFlow<List<String>> = _deadWeightApps.asStateFlow()

    // NEW: Ghost Mode States
    var isGhostModeEnabled by mutableStateOf(false)
    var isVaultUnlocked by mutableStateOf(false)

    fun toggleGhostMode(enabled: Boolean) {
        isGhostModeEnabled = enabled
        if (enabled) isVaultUnlocked = false // Immediately lock it if they turn it on
    }

    fun checkDeadWeight() {
        if (!usageTracker.hasUsagePermission()) return

        viewModelScope.launch {
            val deadWeight = mutableListOf<String>()
            subscriptions.value.forEach { sub ->
                val packageName = AppDirectory.getPackageName(sub.name)
                if (packageName != null) {
                    val hours = usageTracker.getHoursUsedInLast30Days(packageName)
                    if (hours < 1.0) {
                        deadWeight.add(sub.name)
                    }
                }
            }
            _deadWeightApps.value = deadWeight
        }
    }

    fun hasUsagePermission(): Boolean = usageTracker.hasUsagePermission()
    fun requestUsagePermission() = usageTracker.requestPermission()

    suspend fun getSubscriptionById(id: Int): Subscription? {
        return dao.getSubscriptionById(id)
    }

    fun addSubscription(
        name: String, 
        cost: String, 
        billingDate: String, 
        isYearly: Boolean, 
        category: String,
        isFreeTrial: Boolean = false,
        trialDays: String = "7",
        isGhostProtected: Boolean = false
    ) {
        viewModelScope.launch {
            val trialDaysLong = trialDays.toLongOrNull() ?: 7L
            val trialEndDate = if (isFreeTrial) {
                System.currentTimeMillis() + (trialDaysLong * 24 * 60 * 60 * 1000)
            } else null

            val newCost = cost.toDoubleOrNull() ?: 0.0
            val subscription = Subscription(
                name = name,
                cost = newCost,
                previousCost = newCost,
                billingDate = billingDate,
                isYearly = isYearly,
                category = category,
                isFreeTrial = isFreeTrial,
                trialEndDate = trialEndDate,
                isGhostProtected = isGhostProtected
            )
            val id = dao.insertSubscription(subscription)
            val subWithId = subscription.copy(id = id.toInt())
            
            // Record initial price snapshot
            dao.insertPriceSnapshot(PriceSnapshot(subscriptionName = name, price = newCost))
            
            scheduleNotification(subWithId)
            backupToCloud(subWithId)
        }
    }

    fun updateSubscription(
        id: Int, 
        name: String, 
        cost: String, 
        billingDate: String, 
        isYearly: Boolean, 
        category: String,
        isFreeTrial: Boolean = false,
        trialDays: String = "7",
        isGhostProtected: Boolean = false
    ) {
        viewModelScope.launch {
            val oldSub = dao.getSubscriptionById(id)
            val newCost = cost.toDoubleOrNull() ?: 0.0
            
            if (oldSub != null && oldSub.cost != newCost) {
                // Price changed, record snapshot
                dao.insertPriceSnapshot(PriceSnapshot(subscriptionName = name, price = newCost))
                
                if (newCost > oldSub.cost) {
                    // Trigger Price Hike Notification
                    NotificationHelper(getApplication()).showNotification(
                        "Price Hike Alert: $name",
                        "Your subscription cost increased from $${oldSub.cost} to $$newCost.",
                        id
                    )
                }
            }

            val trialDaysLong = trialDays.toLongOrNull() ?: 7L
            val trialEndDate = if (isFreeTrial) {
                System.currentTimeMillis() + (trialDaysLong * 24 * 60 * 60 * 1000)
            } else null

            val subscription = Subscription(
                id = id,
                name = name,
                cost = newCost,
                previousCost = oldSub?.cost ?: newCost,
                billingDate = billingDate,
                isYearly = isYearly,
                category = category,
                isFreeTrial = isFreeTrial,
                trialEndDate = trialEndDate,
                isGhostProtected = isGhostProtected
            )
            dao.insertSubscription(subscription)
            scheduleNotification(subscription)
            backupToCloud(subscription)
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
                        sub?.let {
                            dao.insertSubscription(it)
                            scheduleNotification(it)
                        }
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

    fun cancelNotification(subId: Int) {
        workManager.cancelAllWorkByTag(subId.toString())
    }

    fun triggerGmailScan(context: Context, userEmail: String) {
        viewModelScope.launch {
            val scanner = GmailScanner(context)
            // This will run in the background
            val receiptsFound = scanner.scanForSubscriptions(userEmail)
            
            if (receiptsFound.isNotEmpty()) {
                println("Magic! We found ${receiptsFound.size} potential subscriptions.")
                // TODO: Navigate to a "Confirm Imports" screen 
            }
        }
    }

    // Add this function to save the daily burn locally and tell the widget to update
    suspend fun syncDailyBurnToWidget(context: android.content.Context, totalMonthlySpend: Double) {
        try {
            val dailyBurn = totalMonthlySpend / 30.0
            val sharedPrefs = context.getSharedPreferences("trim_widget_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putFloat("daily_burn", dailyBurn.toFloat()).apply()
            
            // Force the widget to refresh immediately
            DailyBurnWidget().updateAll(context)
        } catch (e: Exception) {
            // Glance might not be initialized yet or background updates limited
            Log.e("WidgetSync", "Failed to update widget", e)
        }
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
