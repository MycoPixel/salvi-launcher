package com.salvador.salvitolauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salvador.salvitolauncher.system.BatteryState

@Composable
fun StandByModeOverlay(
    visible: Boolean,
    es: Boolean,
    battery: BatteryState,
    now: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF1C1C1E),
                            Color(0xFF000000)
                        )
                    )
                )
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reloj gigante
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatClock(context, now),
                        color = Color(0xFFFF3B30),
                        fontSize = 86.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDay(now, es).uppercase(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Info lateral (batería / estado)
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "StandBy",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = (if (battery.charging) "⚡ " else "🔋 ") + "${battery.percent}%",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tr(es, "Toca para salir", "Tap to exit"),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
