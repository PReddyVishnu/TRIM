package com.vishnu.trim

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData

@Composable
fun AnalyticsScreen(
    viewModel: SubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    // SECURITY INTERCEPTOR
    if (viewModel.isGhostModeEnabled && !viewModel.isVaultUnlocked) {
        // THE LOCKED STATE
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF000000)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFE50914), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ghost Mode Active", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (BiometricAuthenticator.canAuthenticate(context)) {
                        BiometricAuthenticator.prompt(
                            context = context,
                            onSuccess = { viewModel.isVaultUnlocked = true },
                            onError = { /* Handle error or show toast */ }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212))
            ) {
                Text("Tap to Unlock", color = Color.White, fontFamily = InterFont)
            }
        }
        return // Stop the rest of the screen from rendering!
    }

    val subs by viewModel.subscriptions.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val monthlySpend by viewModel.monthlyProjection.collectAsState(initial = 0.0)
    val yearlySpend by viewModel.yearlyProjection.collectAsState(initial = 0.0)
    val fiveYearSpend by viewModel.fiveYearProjection.collectAsState(initial = 0.0)
    val dailyBurn by viewModel.dailyBurn.collectAsState(initial = 0.0)
    val highestCategory by viewModel.highestSpendingCategory.collectAsState(initial = null)
    val wealthPivot by viewModel.projectedWealth30Years.collectAsState()
    val deadWeightApps by viewModel.deadWeightApps.collectAsState()
    val healthScore by viewModel.subscriptionHealthScore.collectAsState()
    val hasUsagePermission = viewModel.hasUsagePermission()

    LaunchedEffect(Unit) {
        viewModel.checkDeadWeight()
    }

    // Pre-defined colors for the pie slices
    val sliceColors = listOf(NeonRed, Color.White, Color.Gray, Color.DarkGray)

    // Convert our Map<String, Float> into PieChartData
    val pieChartData = PieChartData(
        slices = categoryTotals.entries.mapIndexed { index, entry ->
            PieChartData.Slice(
                entry.key, // Category Name
                entry.value, // Total Price
                sliceColors[index % sliceColors.size] // Assign a color
            )
        },
        plotType = PlotType.Pie
    )

    val pieChartConfig = PieChartConfig(
        isAnimationEnable = true,
        showSliceLabels = false,
        animationDuration = 1000,
        backgroundColor = BackgroundBlack,
        sliceLabelTextColor = Color.Black,
        isSumVisible = true,
        sumUnit = "$"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ghost Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .background(Color(0xFF121212), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock, 
                    contentDescription = null, 
                    tint = if (viewModel.isGhostModeEnabled) Color(0xFFBB86FC) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "GHOST MODE", 
                        color = if (viewModel.isGhostModeEnabled) Color(0xFFBB86FC) else Color.Gray,
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontFamily = InterFont
                    )
                    Text(
                        text = if (viewModel.isGhostModeEnabled) "Vault Armed" else "Data Visible",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        fontFamily = InterFont
                    )
                }
            }
            
            Switch(
                checked = viewModel.isGhostModeEnabled,
                onCheckedChange = { viewModel.toggleGhostMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFBB86FC),
                    checkedTrackColor = Color(0xFFBB86FC).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }

        Text(
            text = "Analytics",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
            fontFamily = InterFont
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (categoryTotals.isNotEmpty()) {
            PieChart(
                modifier = Modifier
                    .width(250.dp)
                    .height(250.dp),
                pieChartData = pieChartData,
                pieChartConfig = pieChartConfig
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Insight Card
            highestCategory?.let { (category, amount) ->
                InsightCard(
                    title = "Spending Leak",
                    message = "You're spending $${String.format("%.2f", amount)} on $category. Consider trimming this first.",
                    color = NeonRed
                )
            }
        } else {
            Box(modifier = Modifier.height(250.dp), contentAlignment = Alignment.Center) {
                Text("Add subscriptions to see analytics.", color = Color.Gray, fontFamily = InterFont)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Impact Summary",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
            fontFamily = InterFont
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        ProjectionRow("Daily Burn", dailyBurn, Color.Gray)
        ProjectionRow("Monthly Total", monthlySpend, Color.White)
        ProjectionRow("1-Year Projection", yearlySpend, NeonRed)
        ProjectionRow("5-Year Forecast", fiveYearSpend, Color(0xFFFFD700)) // Gold for 5 year

        Spacer(modifier = Modifier.height(32.dp))
        
        // The "Shock Factor"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "The Cost of Inaction",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontFamily = InterFont
                )
                Text(
                    "In 5 years, these 'small' payments will total $${String.format("%,.2f", fiveYearSpend)}.",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFont,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "That's enough for a luxury vacation or a down payment on a car.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontFamily = InterFont,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        if (wealthPivot > 0) {
            WealthPivotCard(projectedWealth = wealthPivot)
            Spacer(modifier = Modifier.height(32.dp))
        }

        // NEW: Subscription Health Score
        HealthScoreCard(score = healthScore)
        Spacer(modifier = Modifier.height(32.dp))

        // NEW: Zero-Waste Overlap Alerts
        val redundancies = ZeroWasteGuardian.identifyRedundancies(subs)
        if (redundancies.isNotEmpty()) {
            Text(
                text = "Redundancy Alerts",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont,
                modifier = Modifier.align(Alignment.Start).padding(vertical = 16.dp)
            )
            
            redundancies.forEach { group ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF331A00)), // Dark Orange
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "OVERLAP: ${group.utility}", 
                                color = Color(0xFFFF9800), 
                                fontWeight = FontWeight.Black, 
                                fontSize = 12.sp,
                                fontFamily = InterFont
                            )
                        }
                        Text(
                            "You're paying for ${group.services.joinToString(" & ")}. Do you need both?",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = InterFont,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            "Potential Savings: $${String.format("%.2f", group.potentialMonthlySavings)}/mo",
                            color = Color(0xFF4CAF50), // Success Green
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFont
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!hasUsagePermission) {
            DeadWeightUnlockCard(onUnlock = { viewModel.requestUsagePermission() })
        } else if (deadWeightApps.isNotEmpty()) {
            DeadWeightAlertCard(apps = deadWeightApps)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun HealthScoreCard(score: Int) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50) // Green
        score >= 50 -> Color(0xFFFF9800) // Orange
        else -> NeonRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SUBSCRIPTION HEALTH", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFont)
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(100.dp),
                    color = scoreColor,
                    strokeWidth = 8.dp,
                    trackColor = Color.DarkGray
                )
                Text(
                    text = score.toString(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFont
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    score >= 80 -> "Excellent! Your finances are lean."
                    score >= 50 -> "Good, but there's room to trim."
                    else -> "Warning: High financial leakage detected."
                },
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = InterFont
            )
        }
    }
}

@Composable
fun DeadWeightUnlockCard(onUnlock: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("UNCOVER DEAD WEIGHT", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFont)
            Text(
                "TRIM can detect which paid apps you aren't actually using. Unlock this 'magic' feature now.",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = InterFont,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enable Usage Tracking", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeadWeightAlertCard(apps: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DEAD WEIGHT DETECTED", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFont)
            }
            Text(
                "You are paying for the following apps but haven't used them in 30 days:",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = InterFont,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            apps.forEach { app ->
                Text("• $app", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "Cancel these to save $${apps.size * 15} / month immediately.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun WealthPivotCard(projectedWealth: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)), // Deep Success Green
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("THE WEALTH PIVOT", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFont)
            Text(
                "If you invested your current monthly spend instead...",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontFamily = InterFont
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$${String.format("%,.0f", projectedWealth)}",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont
            )
            Text(
                "estimated in 30 years (8% avg. return)",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = InterFont
            )
        }
    }
}

@Composable
fun ProjectionRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 16.sp, fontFamily = InterFont)
        Text(
            text = "$${String.format("%.2f", amount)}",
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFont
        )
    }
}

@Composable
fun InsightCard(title: String, message: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, color = Color.White, fontSize = 14.sp, fontFamily = InterFont)
        }
    }
}
