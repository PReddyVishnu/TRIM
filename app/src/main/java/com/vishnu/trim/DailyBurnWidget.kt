package com.vishnu.trim

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class DailyBurnWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        
        // Read the latest saved value
        val sharedPrefs = context.getSharedPreferences("trim_widget_prefs", Context.MODE_PRIVATE)
        val dailyBurn = sharedPrefs.getFloat("daily_burn", 0f).toDouble()

        // The Threshold Logic: Red if burning > $5/day, Green if optimized
        val isHighBurn = dailyBurn > 5.0
        val bgColor = if (isHighBurn) Color(0xFFE50914) else Color(0xFF1B5E20)
        val titleText = if (isHighBurn) "HIGH BURN RATE" else "OPTIMIZED"

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titleText,
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "$${String.format("%.2f", dailyBurn)} / day",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(top = 8.dp)
                )
                Text(
                    text = "in subscriptions",
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                        fontSize = 10.sp
                    ),
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }
        }
    }
}
