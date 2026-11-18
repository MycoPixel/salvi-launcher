package com.salvador.salvitolauncher.system

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Estado compartido entre el servicio y la UI: cuántas notificaciones tiene cada app. */
object BadgeState {
    var counts: Map<String, Int> by mutableStateOf(emptyMap())
    var connected: Boolean by mutableStateOf(false)
}

/**
 * Lee las notificaciones activas para mostrar el número en cada icono. El usuario tiene que
 * concederle "Acceso a notificaciones" al launcher en los ajustes de Android.
 */
class BadgeNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        BadgeState.connected = true
        refresh()
    }

    override fun onListenerDisconnected() {
        BadgeState.connected = false
        BadgeState.counts = emptyMap()
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

        // Se ignoran las notificaciones fijas (música, servicios en primer plano) y los resúmenes
        // de grupo, que duplicarían el conteo.
        BadgeState.counts = active
            .filter { !it.isOngoing && (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .groupingBy { it.packageName }
            .eachCount()
    }
}
