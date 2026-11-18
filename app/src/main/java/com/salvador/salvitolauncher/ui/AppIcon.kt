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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
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

private fun createTintedFilter(colorLong: Long): ColorFilter {
    val color = Color(colorLong)
    val r = color.red * 1.3f
    val g = color.green * 1.3f
    val b = color.blue * 1.3f
    val lr = 0.2126f
    val lg = 0.7152f
    val lb = 0.0722f
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                lr * r, lg * r, lb * r, 0f, 0f,
                lr * g, lg * g, lb * g, 0f, 0f,
                lr * b, lg * b, lb * b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
}

private val LiquidGlassIconFilter = run {
    val lr = 0.2126f
    val lg = 0.7152f
    val lb = 0.0722f
    val tr = 1.05f
    val tg = 1.10f
    val tb = 1.20f
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                lr * tr, lg * tr, lb * tr, 0f, 12f,
                lr * tg, lg * tg, lb * tg, 0f, 12f,
                lr * tb, lg * tb, lb * tb, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
}

fun iconColorFilter(theme: AppIconTheme, tintColor: Long = 0xFF3B82F6L): ColorFilter? = when (theme) {
    AppIconTheme.DEFAULT -> null
    AppIconTheme.DARK -> DarkIconFilter
    AppIconTheme.TINTED -> createTintedFilter(tintColor)
    AppIconTheme.LIQUID_GLASS -> LiquidGlassIconFilter
}

private fun iconTileColor(theme: AppIconTheme, tintColor: Long = 0xFF3B82F6L): Color = when (theme) {
    AppIconTheme.DEFAULT -> Color.White.copy(alpha = 0.12f)
    AppIconTheme.DARK -> Color(0xFF1C1C1E)
    AppIconTheme.TINTED -> Color(tintColor).copy(alpha = 0.35f)
    AppIconTheme.LIQUID_GLASS -> Color.White.copy(alpha = 0.25f)
}

/**
 * Estructura común de un icono del inicio (app o carpeta): se achica al presionar, tiembla en
 * modo edición con el botón "−", muestra un badge de notificaciones y el nombre debajo.
 * La rotación se lee dentro de graphicsLayer, así que animar no recompone la pantalla.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconScaffold(
    seedKey: String,
    label: String,
    showLabel: Boolean,
    wiggle: Boolean,
    badge: Int,
    isLocked: Boolean,
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier,
    tile: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "press")

    // cada icono tiembla con fase y velocidad distintas, como en iOS
    val seed = remember(seedKey) { seedKey.hashCode() }
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
                // En modo edición no hay clics: así el gesto de arrastre del padre recibe el toque.
                .then(
                    if (wiggle) {
                        Modifier
                    } else {
                        Modifier.combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick,
                            onLongClick = onLongClick
                        )
                    }
                )
        ) {
            Box(modifier = Modifier.size(iconSize)) { tile() }

            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
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
        } else if (isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 2.dp, y = (-4).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.70f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    fontSize = 10.sp
                )
            }
        } else if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = (-4).dp)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge > 99) "99+" else badge.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/** Icono de una app. */
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
    iconSize: Dp = 60.dp,
    badge: Int = 0,
    isLocked: Boolean = false,
    customTintColor: Long = 0xFF3B82F6L
) {
    IconScaffold(
        seedKey = app.packageName,
        label = app.label,
        showLabel = showLabel,
        wiggle = wiggle,
        badge = badge,
        isLocked = isLocked,
        iconSize = iconSize,
        onClick = onClick,
        onLongClick = onLongClick,
        onRemoveClick = onRemoveClick,
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(percent = 22)
        val glassModifier = if (iconTheme == AppIconTheme.LIQUID_GLASS) {
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.20f)
                        )
                    ),
                    shape = shape
                )
        } else {
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(iconTileColor(iconTheme, customTintColor))
        }

        Box(modifier = glassModifier) {
            val bitmap = app.icon
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    colorFilter = iconColorFilter(iconTheme, customTintColor),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (iconTheme == AppIconTheme.LIQUID_GLASS) {
                // Brillo superior líquido (glossy liquid sheen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.46f)
                        .clip(RoundedCornerShape(topStartPercent = 22, topEndPercent = 22))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.02f)
                                )
                            )
                        )
                )
            }
        }
    }
}

/** Carpeta creada por el usuario: un cuadro con las primeras 9 apps en miniatura. */
@Composable
fun FolderTile(
    name: String,
    apps: List<AppInfo>,
    iconTheme: AppIconTheme,
    showLabel: Boolean,
    wiggle: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 60.dp,
    badge: Int = 0,
    customTintColor: Long = 0xFF3B82F6L
) {
    IconScaffold(
        seedKey = "folder:$name",
        label = name,
        showLabel = showLabel,
        wiggle = wiggle,
        badge = badge,
        isLocked = false,
        iconSize = iconSize,
        onClick = onClick,
        onLongClick = onLongClick,
        onRemoveClick = onRemoveClick,
        modifier = modifier
    ) {
        val gap = 2.dp
        val mini = (iconSize - gap * 4) / 3
        val shape = RoundedCornerShape(percent = 22)
        val containerModifier = if (iconTheme == AppIconTheme.LIQUID_GLASS) {
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.White.copy(alpha = 0.18f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.90f),
                            Color.White.copy(alpha = 0.25f)
                        )
                    ),
                    shape = shape
                )
                .padding(gap + 1.dp)
        } else {
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color.White.copy(alpha = 0.28f))
                .padding(gap + 1.dp)
        }

        Column(
            modifier = containerModifier,
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            apps.take(9).chunked(3).forEach { rowApps ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowApps.forEach { app ->
                        Box(
                            modifier = Modifier
                                .size(mini)
                                .clip(RoundedCornerShape(percent = 24))
                        ) {
                            val bitmap = app.icon
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    colorFilter = iconColorFilter(iconTheme, customTintColor),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
