package com.salvador.salvitolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.salvador.salvitolauncher.data.LauncherSettings
import com.salvador.salvitolauncher.data.WallpaperMode

class WallpaperPreset(val nameEs: String, val nameEn: String, val colors: List<Color>)

val WallpaperPresets = listOf(
    WallpaperPreset("Azul", "Blue", listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFF1B2B48))),
    WallpaperPreset("Atardecer", "Sunset", listOf(Color(0xFF4A154B), Color(0xFF572568), Color(0xFFE26D5C))),
    WallpaperPreset("Esmeralda", "Emerald", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
    WallpaperPreset("Medianoche", "Midnight", listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF0D1117))),
    WallpaperPreset("Aurora", "Aurora", listOf(Color(0xFF134E5E), Color(0xFF2B7A78), Color(0xFF71B280))),
    WallpaperPreset("Grafito", "Graphite", listOf(Color(0xFF232526), Color(0xFF414345), Color(0xFF18191A)))
)

/**
 * Fondo de pantalla con efecto Parallax al deslizar páginas.
 */
@Composable
fun WallpaperLayer(
    settings: LauncherSettings,
    customImage: ImageBitmap?,
    pageOffset: Float = 0f
) {
    val gradient = WallpaperPresets[settings.gradientIndex.coerceIn(0, WallpaperPresets.lastIndex)].colors
    val parallaxModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            translationX = -pageOffset * 32f
            scaleX = 1.08f
            scaleY = 1.08f
        }

    when (settings.wallpaperMode) {
        WallpaperMode.GRADIENT ->
            Box(parallaxModifier.background(Brush.verticalGradient(gradient)))

        WallpaperMode.CUSTOM ->
            if (customImage != null) {
                Box(parallaxModifier) {
                    Image(
                        bitmap = customImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // velo suave para que se lean los nombres de las apps
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
                }
            } else {
                Box(parallaxModifier.background(Brush.verticalGradient(gradient)))
            }

        WallpaperMode.SYSTEM ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
    }
}
