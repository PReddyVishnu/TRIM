package com.vishnu.trim

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

class UsageTracker(private val context: Context) {

    // 1. Check if the user has actually given us the special permission
    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // 2. Send them to the settings screen if they haven't
    fun requestPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // 3. The Engine: Calculate total hours used in the last 30 days
    fun getHoursUsedInLast30Days(packageName: String): Double {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (30L * 24 * 60 * 60 * 1000) // 30 days ago

        // Queries the aggregated stats
        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        val packageStats = stats[packageName]
        return if (packageStats != null) {
            // Convert milliseconds to hours
            packageStats.totalTimeInForeground / (1000.0 * 60.0 * 60.0)
        } else {
            0.0 // App not used at all
        }
    }
}
