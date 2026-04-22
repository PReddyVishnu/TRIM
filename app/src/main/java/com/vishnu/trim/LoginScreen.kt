package com.vishnu.trim

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onSignInClick: () -> Unit,
    onEmailSignIn: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showEmailFields by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF000000) // True Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spacer to push content down slightly
            Spacer(modifier = Modifier.weight(1f))

            // The Typographic Branding
            Text(
                text = "TRIM",
                color = Color(0xFFE50914), // Neon Red
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = InterFont
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Cut the costs. Keep the control.",
                color = Color.Gray,
                fontSize = 16.sp,
                fontFamily = InterFont
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!showEmailFields) {
                // Google Sign-In Button
                Button(
                    onClick = onSignInClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF121212), // Dark Card Gray
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Continue with Google", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFont
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // The Bypass Option
                TextButton(onClick = { showEmailFields = true }) {
                    Text(
                        text = "Use Email / Test Login", 
                        color = Color.Gray,
                        fontFamily = InterFont
                    )
                }
            } else {
                // The Backdoor Fields
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.Gray, fontFamily = InterFont) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.Gray, fontFamily = InterFont) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onEmailSignIn(email, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Sign In",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFont
                    )
                }
                TextButton(onClick = { showEmailFields = false }) {
                    Text(
                        text = "Back to Google", 
                        color = Color.Gray,
                        fontFamily = InterFont
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
