package com.salvador.salvitolauncher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salvador.salvitolauncher.data.AppIconTheme
import com.salvador.salvitolauncher.data.AppInfo
import kotlin.math.abs

private val DarkIconFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.55f, 0f, 0f, 0f, 0f,
            0f, 0.55f, 0f, 0f, 0f,
            0f, 0f, 0.55f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/** Pasa el icono a escala de grises y lo tiñe de azul (estilo iOS "tinted"). */
private val TintedIconFilter = run {
    val lr = 0.2126f
    val lg = 0.7152f
    val lb = 0.0722f
    val tr = 0.62f * 1.3f
    val tg = 0.78f * 1.3f
    val tb = 1.0f * 1.3f
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                lr * tr, lg * tr, lb * tr, 0f, 0f,
                lr * tg, lg * tg, lb * tg, 0f, 0f,
                lr * tb, lg * tb, lb * tb, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
}

fun iconColorFilter(theme: AppIconTheme): ColorFilter? = when (theme) {
    AppIconTheme.DEFAULT -> null
    AppIconTheme.DARK -> DarkIconFilter
    AppIconTheme.TINTED -> TintedIconFilter
}

private fun iconTileColor(theme: AppIconTheme): Color = when (theme) {
    AppIconTheme.DEFAULT -> Color.White.copy(alpha = 0.12f)
    AppIconTheme.DARK -> Color(0xFF1C1C1E)
    AppIconTheme.TINTED -> Color(0xFF3B82F6).copy(alpha = 0.35f)
}

/**
 * Icono de app con nombre opcional. En modo edición (wiggle) tiembla y muestra el botón "−".
 * La rotación se lee dentro de graphicsLayer, así que animar no recompone la pantalla.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    iconTheme: AppIconTheme,
    showLabel: Boolean,
    wiggle: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 60.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "press")

    // cada icono tiembla con fase y velocidad distintas, como en iOS
    val seed = remember(app.packageName) { app.packageName.hashCode() }
    val wiggleAngle: State<Float>? = if (wiggle) {
        rememberInfiniteTransition(label = "wiggle").animateFloat(
            initialValue = if (seed % 2 == 0) -2f else 2f,
            targetValue = if (seed % 2 == 0) 2f else -2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 100 + abs(seed % 40), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wiggleAngle"
        )
    } else {
        null
    }

    Box(
        modifier = modifier
            .width(iconSize + 16.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                rotationZ = wiggleAngle?.value ?: 0f
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { if (!wiggle) onClick() },
                    onLongClick = onLongClick
                )
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(percent = 22))
                    .background(iconTileColor(iconTheme))
            ) {
                val bitmap = app.icon
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        colorFilter = iconColorFilter(iconTheme),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = app.label,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        shadow = Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 4f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (wiggle) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 2.dp, y = (-4).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8E8E93))
                    .clickable(onClick = onRemoveClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
