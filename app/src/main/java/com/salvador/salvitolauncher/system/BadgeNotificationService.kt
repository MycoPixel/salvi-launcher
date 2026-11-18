package com.salvador.salvitolauncher.system

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap

data class NotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val icon: Bitmap?,
    val contentIntent: PendingIntent?,
    val postTime: Long
)

/** Estado compartido entre el servicio y la UI. */
object BadgeState {
    var counts: Map<String, Int> by mutableStateOf(emptyMap())
    var items: List<NotificationItem> by mutableStateOf(emptyList())
    var connected: Boolean by mutableStateOf(false)
    var instance: BadgeNotificationService? = null
}

/**
 * Lee las notificaciones activas para mostrar el número en cada icono y permitir interactuar con ellas.
 */
class BadgeNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        BadgeState.connected = true
        BadgeState.instance = this
        refresh()
    }

    override fun onListenerDisconnected() {
        BadgeState.connected = false
        BadgeState.instance = null
        BadgeState.counts = emptyMap()
        BadgeState.items = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val active: Array<StatusBarNotification>? = try {
            activeNotifications
        } catch (_: SecurityException) {
            null
        }
        if (active == null) return

        val validNotifications = active.filter {
            !it.isOngoing && (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
        }

        BadgeState.counts = validNotifications
            .groupingBy { it.packageName }
            .eachCount()

        val pm = packageManager
        BadgeState.items = validNotifications.mapNotNull { sbn ->
            val notification = sbn.notification ?: return@mapNotNull null
            val extras = notification.extras ?: return@mapNotNull null

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                ?: return@mapNotNull null

            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                ?: ""

            val iconBitmap = try {
                val iconDrawable = sbn.notification.smallIcon?.loadDrawable(this)
                    ?: pm.getApplicationIcon(sbn.packageName)
                iconDrawable.toBitmap(64, 64)
            } catch (_: Exception) {
                null
            }

            NotificationItem(
                key = sbn.key,
                packageName = sbn.packageName,
                title = title,
                text = text,
                icon = iconBitmap,
                contentIntent = notification.contentIntent,
                postTime = sbn.postTime
            )
        }.sortedByDescending { it.postTime }
    }
}

fun openNotificationItem(item: NotificationItem, context: android.content.Context) {
    if (item.contentIntent != null) {
        try {
            item.contentIntent.send()
        } catch (_: Exception) {
            launchApp(context, item.packageName)
        }
    } else {
        launchApp(context, item.packageName)
    }
}

fun dismissNotificationItem(key: String) {
    try {
        BadgeState.instance?.cancelNotification(key)
    } catch (_: Exception) {
    }
}

fun clearAllNotifications() {
    try {
        BadgeState.instance?.cancelAllNotifications()
    } catch (_: Exception) {
    }
}
