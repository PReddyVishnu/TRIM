package com.vishnu.trim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment

import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun ManagerScreen(
    viewModel: SubscriptionViewModel = viewModel()
) {
    val subs by viewModel.subscriptions.collectAsState()
    val context = LocalContext.current
    val userEmail = "preddyvishnu497@gmail.com" // Update with dynamic email from Auth if possible
    
    var negotiationSub by remember { mutableStateOf<Subscription?>(null) }
    val clipboardManager = LocalClipboardManager.current

    // This triggers the fetch function as soon as the screen opens
    LaunchedEffect(Unit) {
        viewModel.fetchSubscriptions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp)
    ) {
        Text(
            text = "Active Subscriptions",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFont
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.triggerGmailScan(context, userEmail) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Email, 
                contentDescription = "Scan", 
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Auto-Scan Gmail for Receipts", color = Color.Black, fontFamily = InterFont)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(subs) { sub ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFF121212), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sub.name, 
                            color = Color.White, 
                            fontSize = 18.sp, 
                            fontFamily = InterFont,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "$${sub.cost}", 
                            color = if (sub.cost > sub.previousCost) Color(0xFFE50914) else Color.White, 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFont
                        )
                        
                        if (sub.cost > sub.previousCost) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE50914).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "PRICE HIKE", 
                                    color = Color(0xFFE50914), 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Black,
                                    fontFamily = InterFont
                                )
                            }
                        }
                        
                        if (sub.isGhostProtected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock, 
                                    contentDescription = "Ghost Protected",
                                    tint = Color(0xFFBB86FC),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "GHOST PROTECTED", 
                                    color = Color(0xFFBB86FC), 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFont
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        IconButton(
                            onClick = { viewModel.deleteSubscription(sub) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = "Delete", 
                                tint = Color.DarkGray
                            )
                        }
                    }

                    // NEW: The Card-Reward Optimizer Badge
                    val rewardTip = RewardOptimizer.getOptimizationTip(sub.category, sub.name)
                    
                    if (rewardTip != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF8E1).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star, 
                                contentDescription = "Reward", 
                                tint = Color(0xFFFFD54F), // Premium Gold
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${rewardTip.message} for ${rewardTip.potentialYield}",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFont
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { CancellationHelper.openCancellationPage(context, sub.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Trim It (Cancel Online)", color = Color.LightGray, fontSize = 12.sp, fontFamily = InterFont)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { negotiationSub = sub },
                        modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f), // Trust Blue
                            contentColor = Color(0xFF64B5F6)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("NEGOTIATE PRICE", fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFont)
                        }
                    }

                    Button(
                        onClick = { CancellationManager.launchKillSwitch(context, sub.name) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE50914).copy(alpha = 0.1f),
                            contentColor = Color(0xFFE50914)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "KILL SWITCH (UNSUBSCRIBE)", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = InterFont
                            )
                        }
                    }
                }
            }
        }
    }

    // Place this Dialog at the bottom of your ManagerScreen (outside the LazyColumn)
    negotiationSub?.let { sub ->
        val script = ScriptEngine.generateScript(sub.name, sub.cost)

        AlertDialog(
            onDismissRequest = { negotiationSub = null },
            containerColor = Color(0xFF1E1E1E),
            title = { 
                Text("Retention Script: ${sub.name}", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InterFont) 
            },
            text = {
                Column {
                    Text(
                        "Send this message to their live chat support. These specific phrases are designed to trigger retention discounts.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontFamily = InterFont
                    )
                    Text(
                        text = script,
                        color = Color(0xFF64B5F6),
                        fontSize = 14.sp,
                        fontFamily = InterFont,
                        modifier = Modifier
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        clipboardManager.setText(AnnotatedString(script))
                        negotiationSub = null // Close dialog
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Copy to Clipboard", color = Color.White, fontFamily = InterFont)
                }
            },
            dismissButton = {
                TextButton(onClick = { negotiationSub = null }) {
                    Text("Cancel", color = Color.Gray, fontFamily = InterFont)
                }
            }
        )
    }
}
