package com.vishnu.trim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vishnu.trim.ui.theme.TRIMTheme
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Netflix-Style Dark Theme Colors
val BackgroundBlack = Color(0xFF000000)
val CardDark = Color(0xFF121212)
val NeonRed = Color(0xFFE50914)
val GradientStart = Color(0xFFFF7B7B)

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFont = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = GoogleFont("Inter"),
        fontProvider = provider
    )
)

val premiumRedGradient = Brush.verticalGradient(
    colors = listOf(GradientStart, NeonRed)
)

class MainActivity : FragmentActivity() {
    private val viewModel: SubscriptionViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val workRequest = PeriodicWorkRequestBuilder<TrimWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "TrimDailyCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        checkNotificationPermission()

        setContent {
            TRIMTheme {
                TrimApp(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock the vault when the user leaves the app
        viewModel.isVaultUnlocked = false
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun TrimApp(viewModel: SubscriptionViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val startScreen = if (authManager.isUserSignedIn()) "dashboard" else "login"

    NavHost(navController = navController, startDestination = startScreen) {
        composable("login") {
            LoginScreen(
                onSignInClick = {
                    coroutineScope.launch {
                        val success = authManager.signInWithGoogle()
                        if (success) {
                            viewModel.restoreFromCloud()
                            Toast.makeText(context, "Subscriptions restored from cloud!", Toast.LENGTH_SHORT).show()
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                },
                onEmailSignIn = { email, password ->
                    coroutineScope.launch {
                        val success = authManager.signInWithEmail(email, password)
                        if (success) {
                            viewModel.restoreFromCloud()
                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Login failed. Check your email/password.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        composable("dashboard") {
            TrimDashboard(
                viewModel = viewModel,
                navController = navController,
                onAddClick = { navController.navigate("add_edit_subscription/-1") },
                onEditClick = { id -> navController.navigate("add_edit_subscription/$id") }
            )
        }
        composable("analytics") {
            TrimDashboard(
                viewModel = viewModel,
                navController = navController,
                onAddClick = { navController.navigate("add_edit_subscription/-1") },
                onEditClick = { id -> navController.navigate("add_edit_subscription/$id") },
                initialContent = "analytics"
            )
        }
        composable("manager") {
            TrimDashboard(
                viewModel = viewModel,
                navController = navController,
                onAddClick = { navController.navigate("add_edit_subscription/-1") },
                onEditClick = { id -> navController.navigate("add_edit_subscription/$id") },
                initialContent = "manager"
            )
        }
        composable(
            "add_edit_subscription/{subId}",
            arguments = listOf(navArgument("subId") { type = NavType.IntType })
        ) { backStackEntry ->
            val subId = backStackEntry.arguments?.getInt("subId") ?: -1
            AddSubscriptionScreen(
                subId = subId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun TrimDashboard(
    viewModel: SubscriptionViewModel, 
    navController: NavHostController,
    onAddClick: () -> Unit, 
    onEditClick: (Int) -> Unit,
    initialContent: String = "dashboard"
) {
    val subscriptions by viewModel.filteredSubscriptions.collectAsState(initial = emptyList())
    val allSubscriptions by viewModel.allSubscriptions.collectAsState(initial = emptyList())
    val monthlySpend by viewModel.monthlyProjection.collectAsState(initial = 0.0)
    val yearlySpend by viewModel.yearlyProjection.collectAsState(initial = 0.0)
    val categorySpend by viewModel.categorySpending.collectAsState(initial = emptyMap())
    
    val totalTrimmed by viewModel.totalTrimmed.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val categories = listOf("All", "Entertainment", "Utilities", "Health", "Work", "Food")

    LaunchedEffect(Unit) {
        viewModel.fetchStats()
        viewModel.fetchSubscriptions()
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            NavigationBar(
                containerColor = CardDark,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonRed,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = NeonRed,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    selected = currentRoute == "analytics",
                    onClick = {
                        navController.navigate("analytics") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonRed,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = NeonRed,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Manager") },
                    label = { Text("Manager") },
                    selected = currentRoute == "manager",
                    onClick = {
                        navController.navigate("manager") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonRed,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = NeonRed,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = NeonRed
            ) {
                Text("+", color = Color.White, fontSize = 28.sp)
            }
        },
        containerColor = BackgroundBlack
    ) { padding ->
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: initialContent

        Box(modifier = Modifier.padding(padding)) {
            when (currentRoute) {
                "analytics" -> AnalyticsScreen(viewModel = viewModel)
                "manager" -> ManagerScreen(viewModel = viewModel)
                else -> {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

                        // Header: Total Spend
                        Text("Total Monthly Spend", color = Color.Gray, fontSize = 16.sp, fontFamily = InterFont)
                        Text(
                            text = "$${String.format("%.2f", monthlySpend)}",
                            style = TextStyle(
                                brush = premiumRedGradient,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFont
                            )
                        )

                        ProjectionCard(monthly = monthlySpend, yearly = yearlySpend, totalTrimmed = totalTrimmed)

                        if (categorySpend.isNotEmpty()) {
                            CategoryBreakdown(categorySpend)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (allSubscriptions.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Your vault is empty.",
                                    color = Color.Gray,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFont
                                )
                                Text(
                                    text = "Tap the + to start trimming costs.",
                                    color = Color.DarkGray,
                                    fontSize = 14.sp,
                                    fontFamily = InterFont
                                )
                            }
                        } else {
                            // Search Bar
                            SearchBar(query = searchQuery, onQueryChange = { viewModel.onSearchQueryChange(it) })

                            // Filter Chips
                            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                items(categories) { filter ->
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { viewModel.onFilterChange(filter) },
                                        label = { Text(filter) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonRed,
                                            labelColor = Color.Gray,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Subscriptions List Title
                            Text(
                                "Active Subscriptions",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFont
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (subscriptions.isEmpty()) {
                                Text(
                                    "No results found.",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 16.dp),
                                    fontFamily = InterFont
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(subscriptions) { subscription ->
                                        SubscriptionCard(
                                            subscription = subscription,
                                            onDelete = { viewModel.deleteSubscription(subscription) },
                                            onClick = { onEditClick(subscription.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search...", color = Color.Gray, fontFamily = InterFont) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonRed,
            unfocusedBorderColor = Color.DarkGray,
            cursorColor = NeonRed,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
        singleLine = true
    )
}

@Composable
fun CategoryBreakdown(categorySpend: Map<String, Double>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Top Categories", color = Color.Gray, fontSize = 14.sp, fontFamily = InterFont)
        Spacer(modifier = Modifier.height(8.dp))
        categorySpend.forEach { (category, amount) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category, color = Color.White, fontSize = 14.sp, fontFamily = InterFont)
                Text("$${String.format("%.2f", amount)}", color = Color.Gray, fontSize = 14.sp, fontFamily = InterFont)
            }
        }
    }
}

@Composable
fun ProjectionCard(monthly: Double, yearly: Double, totalTrimmed: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spending Projections", color = Color.Gray, fontSize = 14.sp, fontFamily = InterFont)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Monthly Avg", color = Color.Gray, fontSize = 12.sp, fontFamily = InterFont)
                    Text("$${String.format("%.2f", monthly)}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
                }
                
                // Vertical Divider
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.DarkGray))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Yearly Total", color = Color.Gray, fontSize = 12.sp, fontFamily = InterFont)
                    Text("$${String.format("%.2f", yearly)}", color = NeonRed, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
                }

                // Vertical Divider
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.DarkGray))

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Trimmed", color = Color.Gray, fontSize = 12.sp, fontFamily = InterFont)
                    Text("$${String.format("%.2f", totalTrimmed)}", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(subscription: Subscription, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subscription.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${subscription.category} • Renews ${subscription.billingDate} ${if (subscription.isYearly) "(Yearly)" else "(Monthly)"}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = InterFont
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$${String.format("%.2f", subscription.cost)}",
                    color = NeonRed,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFont
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionScreen(
    subId: Int,
    viewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val isEditing = subId != -1
    
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var isYearly by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Entertainment") }
    var isFreeTrial by remember { mutableStateOf(false) }
    var trialDays by remember { mutableStateOf("7") }
    var isGhostProtected by remember { mutableStateOf(false) }
    
    val categories = listOf("Entertainment", "Utilities", "Health", "Work", "Food")
    
    var nameError by remember { mutableStateOf(false) }
    var costError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    LaunchedEffect(subId) {
        if (isEditing) {
            val existingSub = viewModel.getSubscriptionById(subId)
            existingSub?.let {
                name = it.name
                cost = it.cost.toString()
                date = it.billingDate
                isYearly = it.isYearly
                category = it.category
                isFreeTrial = it.isFreeTrial
                isGhostProtected = it.isGhostProtected
            }
        }
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            date = "$dayOfMonth/${month + 1}/$year"
            dateError = false
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        containerColor = BackgroundBlack,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditing) stringResource(R.string.edit_subscription) else stringResource(R.string.add_subscription), 
                        color = Color.White 
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = NeonRed
                ),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = NeonRed)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.subscription_name_hint), color = Color.Gray) },
                isError = nameError,
                supportingText = { if (nameError) Text("Name is required") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonRed,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = NeonRed
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Cost Input
            OutlinedTextField(
                value = cost,
                onValueChange = { 
                    if (it.isEmpty() || it.toDoubleOrNull() != null) {
                        cost = it
                        costError = false
                    }
                },
                label = { Text(stringResource(R.string.cost_hint), color = Color.Gray) },
                isError = costError,
                supportingText = { if (costError) Text("Please enter a valid amount") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonRed,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = NeonRed
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Date Input (Clickable to show DatePicker)
            OutlinedTextField(
                value = date,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.billing_date_hint), color = Color.Gray) },
                isError = dateError,
                supportingText = { if (dateError) Text("Billing date is required") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .clickable { datePickerDialog.show() },
                enabled = false, // Use disabled but with custom colors to avoid standard keyboard
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if (dateError) NeonRed else Color.DarkGray,
                    disabledTextColor = Color.White,
                    disabledLabelColor = Color.Gray,
                    disabledSupportingTextColor = NeonRed
                )
            )

            // Billing Cycle Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Yearly Billing?", color = Color.White, fontSize = 16.sp, fontFamily = InterFont)
                Switch(
                    checked = isYearly,
                    onCheckedChange = { isYearly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonRed,
                        checkedTrackColor = NeonRed.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }

            // Free Trial Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Is this a Free Trial?", color = Color.White, fontSize = 16.sp, fontFamily = InterFont)
                Switch(
                    checked = isFreeTrial,
                    onCheckedChange = { isFreeTrial = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonRed,
                        checkedTrackColor = NeonRed.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }

            if (isFreeTrial) {
                OutlinedTextField(
                    value = trialDays,
                    onValueChange = { trialDays = it },
                    label = { Text("Trial length (days)", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonRed,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Ghost Card Protection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isGhostProtected,
                            onCheckedChange = { isGhostProtected = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFBB86FC))
                        )
                        Text("Protect with Ghost Card", color = Color.LightGray, fontSize = 14.sp, fontFamily = InterFont)
                    }
                }

                if (isGhostProtected) {
                    Button(
                        onClick = { VirtualCardManager.launchCardCreator(context, name, cost.toDoubleOrNull() ?: 0.0) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock, 
                            contentDescription = null, 
                            tint = Color(0xFFBB86FC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Burner Card", color = Color(0xFFBB86FC), fontFamily = InterFont)
                    }
                }
            }

            // Category Selection
            Text(
                "Category",
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = InterFont,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonRed,
                            labelColor = Color.Gray,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Save Button
            Button(
                onClick = { 
                    nameError = name.isBlank()
                    costError = cost.isBlank() || cost.toDoubleOrNull() == null
                    dateError = date.isBlank()

                    if (!nameError && !costError && !dateError) {
                        if (isEditing) {
                            viewModel.updateSubscription(subId, name, cost, date, isYearly, category, isFreeTrial, trialDays, isGhostProtected)
                        } else {
                            viewModel.addSubscription(name, cost, date, isYearly, category, isFreeTrial, trialDays, isGhostProtected)
                        }
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.save_button), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
