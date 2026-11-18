package com.salvador.salvitolauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salvador.salvitolauncher.system.BadgeState
import com.salvador.salvitolauncher.system.NotificationItem
import com.salvador.salvitolauncher.system.clearAllNotifications
import com.salvador.salvitolauncher.system.dismissNotificationItem
import com.salvador.salvitolauncher.system.openNotificationItem

private val NotificationCardBg = Color(0xFF2C2C2E).copy(alpha = 0.85f)
private val AccentBlue = Color(0xFF0A84FF)

@Composable
fun NotificationCenterOverlay(
    visible: Boolean,
    es: Boolean,
    onDismiss: () -> Unit,
    onOpenNotificationAccess: () -> Unit
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
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
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(es, "Centro de Notificaciones", "Notification Center"),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (BadgeState.items.isNotEmpty()) {
                        TextButton(onClick = { clearAllNotifications() }) {
                            Text(
                                text = tr(es, "Borrar todas", "Clear all"),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = tr(es, "Listo", "Done"),
                            color = AccentBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!BadgeState.connected) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(NotificationCardBg)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = tr(es, "Acceso a notificaciones no concedido", "Notification access not granted"),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tr(es, "Toca aquí para permitir que el launcher lea las notificaciones.", "Tap here to allow launcher notification access."),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = tr(es, "Conceder permiso", "Grant permission"),
                                color = AccentBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(onClick = onOpenNotificationAccess)
                            )
                        }
                    } else if (BadgeState.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(es, "No hay notificaciones", "No notifications"),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        BadgeState.items.forEach { item ->
                            NotificationCardItem(
                                item = item,
                                onClick = {
                                    openNotificationItem(item, context)
                                    onDismiss()
                                },
                                onDismissItem = {
                                    dismissNotificationItem(item.key)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    item: NotificationItem,
    onClick: () -> Unit,
    onDismissItem: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NotificationCardBg)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                Image(
                    bitmap = item.icon.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = item.packageName.substringAfterLast('.').capitalize(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✕",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismissItem)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (item.text.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.text,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
