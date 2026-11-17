package com.salvador.salvitolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salvador.salvitolauncher.system.canWriteSettings
import com.salvador.salvitolauncher.system.getBrightnessFraction
import com.salvador.salvitolauncher.system.getVolumeFraction
import com.salvador.salvitolauncher.system.isAirplaneOn
import com.salvador.salvitolauncher.system.isBluetoothOn
import com.salvador.salvitolauncher.system.isWifiOn
import com.salvador.salvitolauncher.system.openAirplaneSettings
import com.salvador.salvitolauncher.system.openAndroidSettings
import com.salvador.salvitolauncher.system.openBluetoothSettings
import com.salvador.salvitolauncher.system.openInternetSettings
import com.salvador.salvitolauncher.system.rememberTorchState
import com.salvador.salvitolauncher.system.requestWriteSettings
import com.salvador.salvitolauncher.system.setBrightnessFraction
import com.salvador.salvitolauncher.system.setTorch
import com.salvador.salvitolauncher.system.setVolumeFraction

private val ControlBlue = Color(0xFF0A84FF)

/**
 * Centro de Control. Linterna, volumen y brillo son reales. Wi-Fi, Bluetooth y modo avión
 * no se pueden activar desde una app en Android moderno, así que abren su panel del sistema
 * (y muestran el estado actual).
 */
@Composable
fun ControlCenterOverlay(
    es: Boolean,
    onOpenLauncherSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val torchOn by rememberTorchState()
    val wifiOn = remember { isWifiOn(context) }
    val bluetoothOn = remember { isBluetoothOn(context) }
    val airplaneOn = remember { isAirplaneOn(context) }
    val canWrite = remember { canWriteSettings(context) }
    var brightness by remember { mutableFloatStateOf(getBrightnessFraction(context)) }
    var volume by remember { mutableFloatStateOf(getVolumeFraction(context)) }

    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, amount -> if (amount < -14f) onDismiss() }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF2C2C2E).copy(alpha = 0.82f))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = tr(es, "Centro de Control", "Control Center"),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlTile("📶", "Wi-Fi", wifiOn) {
                    openInternetSettings(context)
                    onDismiss()
                }
                ControlTile("ᛒ", "Bluetooth", bluetoothOn) {
                    openBluetoothSettings(context)
                    onDismiss()
                }
                ControlTile("🔦", tr(es, "Linterna", "Torch"), torchOn) {
                    setTorch(context, !torchOn)
                }
                ControlTile("✈️", tr(es, "Avión", "Airplane"), airplaneOn) {
                    openAirplaneSettings(context)
                    onDismiss()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tr(es, "Brillo", "Brightness"),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        if (canWrite) setBrightnessFraction(context, it)
                    },
                    onValueChangeFinished = {
                        // Cambiar el brillo del sistema requiere un permiso especial que se concede en Ajustes.
                        if (!canWrite) {
                            requestWriteSettings(context)
                            onDismiss()
                        }
                    },
                    colors = sliderColors
                )
                Text(
                    text = tr(es, "Volumen", "Volume"),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        setVolumeFraction(context, it)
                    },
                    colors = sliderColors
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onOpenLauncherSettings) {
                    Text(
                        text = tr(es, "Ajustes del launcher", "Launcher settings"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = {
                    openAndroidSettings(context)
                    onDismiss()
                }) {
                    Text(
                        text = tr(es, "Ajustes de Android", "Android settings"),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlTile(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) ControlBlue else Color.White.copy(alpha = 0.25f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 21.sp, color = Color.White)
        }
        Text(text = label, color = Color.White, fontSize = 11.sp, maxLines = 1)
    }
}
