package com.vishnu.trim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.FontRequests
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.vishnu.trim.ui.theme.TRIMTheme
import java.util.Calendar

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

class MainActivity : ComponentActivity() {
    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TRIMTheme {
                TrimApp(viewModel)
            }
        }
    }
}

@Composable
fun TrimApp(viewModel: SubscriptionViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            TrimDashboard(
                viewModel = viewModel,
                onAddClick = { navController.navigate("add_edit_subscription/-1") },
                onEditClick = { id -> navController.navigate("add_edit_subscription/$id") }
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
fun TrimDashboard(viewModel: SubscriptionViewModel, onAddClick: () -> Unit, onEditClick: (Int) -> Unit) {
    val subscriptions by viewModel.allSubscriptions.collectAsState(initial = emptyList())
    val totalCost = subscriptions.sumOf { it.cost }

    Scaffold(
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {

            // Header: Total Spend
            Text("Total Monthly Spend", color = Color.Gray, fontSize = 16.sp, fontFamily = InterFont)
            Text(
                text = "$${String.format("%.2f", totalCost)}",
                style = TextStyle(
                    brush = premiumRedGradient,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFont
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Subscriptions List Title
            Text("Active Subscriptions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
            Spacer(modifier = Modifier.height(16.dp))

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
                Text("Renews ${subscription.billingDate}", color = Color.Gray, fontSize = 14.sp, fontFamily = InterFont)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$${String.format("%.2f", subscription.cost)}/mo", color = NeonRed, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
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

            // Save Button
            Button(
                onClick = { 
                    nameError = name.isBlank()
                    costError = cost.isBlank() || cost.toDoubleOrNull() == null
                    dateError = date.isBlank()

                    if (!nameError && !costError && !dateError) {
                        if (isEditing) {
                            viewModel.updateSubscription(subId, name, cost, date)
                        } else {
                            viewModel.addSubscription(name, cost, date)
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
