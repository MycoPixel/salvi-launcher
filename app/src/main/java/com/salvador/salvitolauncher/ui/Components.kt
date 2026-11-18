package com.salvador.salvitolauncher.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salvador.salvitolauncher.data.AppIconTheme
import com.salvador.salvitolauncher.data.AppInfo
import com.salvador.salvitolauncher.system.BatteryState
import com.salvador.salvitolauncher.system.formatGigabytes
import com.salvador.salvitolauncher.system.nextAlarmMillis
import com.salvador.salvitolauncher.system.storageInfo

// ======================================================================= menú contextual

class MenuItem(
    val label: String,
    val icon: ImageBitmap? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/** Menú contextual estilo iOS: tarjeta oscura sobre un velo. Tocar fuera lo cierra. */
@Composable
fun GlassMenu(title: String?, items: List<MenuItem>, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF2C2C2E).copy(alpha = 0.95f))
                // consume los toques para que no lleguen al velo y cierren el menú
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            if (title != null) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
            items.forEachIndexed { index, item ->
                if (index > 0 || title != null) MenuSeparator()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.icon != null) {
                        Image(
                            bitmap = item.icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = item.label,
                        color = if (item.destructive) Color(0xFFFF453A) else Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuSeparator() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.15f))
    )
}

// ======================================================================= barra superior

@Composable
fun TopBar(
    es: Boolean,
    showIsland: Boolean,
    wiggle: Boolean,
    battery: BatteryState,
    onDone: () -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenNotificationCenter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 20.dp)
            // deslizar hacia abajo: desde la derecha abre Centro de Control, centro/izquierda abre Notificaciones
            .pointerInput(Unit) {
                var startX = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset -> startX = offset.x },
                    onVerticalDrag = { _, amount ->
                        if (amount > 14f) {
                            if (startX > size.width * 0.65f) {
                                onOpenControlCenter()
                            } else {
                                onOpenNotificationCenter()
                            }
                        }
                    }
                )
            }
    ) {
        if (wiggle) {
            Text(
                text = tr(es, "Listo", "Done"),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable(onClick = onDone)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        if (showIsland) {
            DynamicIsland(
                es = es,
                battery = battery,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        BatteryCapsule(
            battery = battery,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenControlCenter)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DynamicIsland(es: Boolean, battery: BatteryState, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(if (expanded) 200.dp else 104.dp, label = "islandWidth")
    val height by animateDpAsState(if (expanded) 36.dp else 26.dp, label = "islandHeight")

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .clickable { expanded = !expanded },
        contentAlignment = Alignment.Center
    ) {
        val bolt = if (battery.charging) "⚡ " else ""
        val text = if (expanded) {
            val status = if (battery.charging) tr(es, "Cargando", "Charging") else tr(es, "En uso", "On battery")
            "🔋 ${battery.percent}%  ·  $status"
        } else {
            "$bolt${battery.percent}%"
        }
        Text(text = text, color = Color.White, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun BatteryCapsule(battery: BatteryState, modifier: Modifier = Modifier) {
    val fillColor = when {
        battery.charging -> Color(0xFF34C759)
        battery.percent <= 20 -> Color(0xFFFF3B30)
        else -> Color.White
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "${battery.percent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 12.dp)
                .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                .padding(1.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((battery.percent / 100f).coerceIn(0.06f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(fillColor)
            )
        }
    }
}

// ======================================================================= widgets

@Composable
private fun WidgetCard(
    modifier: Modifier = Modifier,
    gesture: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .then(gesture)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
private fun WidgetLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.75f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Dos widgets con datos reales: una pila (fecha/hora y próxima alarma, se cambia tocando o
 * deslizando) y batería + almacenamiento.
 */
@Composable
fun HomeWidgets(es: Boolean, battery: BatteryState, now: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var stackIndex by remember { mutableIntStateOf(0) }
    val stackSize = 2
    val alarm = remember(now) { nextAlarmMillis(context) }
    val storage = remember { storageInfo() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WidgetCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            gesture = Modifier
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total > 40f) stackIndex = (stackIndex + stackSize - 1) % stackSize
                            else if (total < -40f) stackIndex = (stackIndex + 1) % stackSize
                        },
                        onDragCancel = { total = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            total += amount
                        }
                    )
                }
                .clickable { stackIndex = (stackIndex + 1) % stackSize }
        ) {
            if (stackIndex == 0) {
                WidgetLabel(formatDay(now, es))
                Text(
                    text = formatClock(context, now),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
            } else {
                WidgetLabel(tr(es, "Próxima alarma", "Next alarm"))
                Text(
                    text = alarm?.let { formatAlarm(it, es) } ?: tr(es, "Sin alarmas", "No alarms"),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        WidgetCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            WidgetLabel(tr(es, "Batería", "Battery"))
            Text(
                text = (if (battery.charging) "⚡ " else "") + "${battery.percent}%",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "💾 " + tr(
                    es,
                    "${formatGigabytes(storage.first)} libres",
                    "${formatGigabytes(storage.first)} free"
                ),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

// ======================================================================= búsqueda y puntos

@Composable
fun SpotlightSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSearch: () -> Unit,
    es: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = size.minDimension / 2.5f,
                center = Offset(center.x - 2f, center.y - 2f),
                style = Stroke(width = 3f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(center.x + 1f, center.y + 1f),
                end = Offset(size.width, size.height),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = tr(es, "Buscar apps", "Search apps"),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
        if (query.isNotEmpty()) {
            Text(
                text = "✕",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            Text(
                text = "🎙️",
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onVoiceClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun PageIndicatorDots(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color.White.copy(alpha = 0.4f))
            )
        }
    }
}

// ======================================================================= dock

@Composable
fun Dock(
    apps: List<AppInfo>,
    iconTheme: AppIconTheme,
    wiggle: Boolean,
    isTablet: Boolean,
    badgeCounts: Map<String, Int>,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    onRemoveClick: (AppInfo) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 64.dp else 16.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                AppIcon(
                    app = app,
                    iconTheme = iconTheme,
                    showLabel = false,
                    wiggle = wiggle,
                    badge = badgeCounts[app.packageName] ?: 0,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongClick(app) },
                    onRemoveClick = { onRemoveClick(app) }
                )
            }
        }
    }
}
