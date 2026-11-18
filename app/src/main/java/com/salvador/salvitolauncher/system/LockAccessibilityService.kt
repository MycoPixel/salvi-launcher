package com.salvador.salvitolauncher.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Servicio de accesibilidad mínimo: no lee la pantalla ni recibe eventos, solo existe para poder
 * bloquear el teléfono (Android no permite hacerlo de otra forma sin ser administrador del
 * dispositivo). Requiere Android 9 o superior.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        running = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        running = false
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private var instance: LockAccessibilityService? = null

        /** true mientras el usuario tenga el servicio activado en Accesibilidad. */
        var running: Boolean by mutableStateOf(false)
            private set

        val isSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        fun lockScreen(): Boolean {
            if (!isSupported) return false
            return instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true
        }
    }
}
