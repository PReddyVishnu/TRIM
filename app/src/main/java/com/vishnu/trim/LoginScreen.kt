package com.vishnu.trim

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onSignInClick: () -> Unit
) {
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
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
