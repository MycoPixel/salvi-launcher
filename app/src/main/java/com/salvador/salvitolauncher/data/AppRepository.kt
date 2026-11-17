package com.salvador.salvitolauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri

object AppRepository {

    private const val ICON_PX = 192

    /** Debe llamarse desde un hilo de fondo (convierte todos los iconos a bitmap). */
    fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            // Una app puede exponer varias actividades launcher; sin esto las claves de la grilla se duplican y crashea.
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                AppInfo(
                    packageName = pkg,
                    label = label,
                    icon = drawableToBitmap(info.loadIcon(pm)),
                    category = categorize(context, pkg, label)
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Teléfono, mensajes, navegador y cámara: las apps que el sistema tiene como predeterminadas. */
    fun defaultDock(context: Context, apps: List<AppInfo>): List<String> {
        val pm = context.packageManager
        val installed = apps.map { it.packageName }.toSet()
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, "smsto:".toUri()),
            Intent(Intent.ACTION_VIEW, "https://".toUri()),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        )
        return intents
            .mapNotNull { intent ->
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    .map { it.activityInfo.packageName }
                    .firstOrNull { it in installed }
            }
            .distinct()
    }

    private fun drawableToBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            drawable.toBitmap(ICON_PX, ICON_PX).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun categorize(context: Context, packageName: String, label: String): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                when (info.category) {
                    ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                    ApplicationInfo.CATEGORY_AUDIO,
                    ApplicationInfo.CATEGORY_VIDEO,
                    ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.ENTERTAINMENT
                    ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                    ApplicationInfo.CATEGORY_NEWS,
                    ApplicationInfo.CATEGORY_MAPS,
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                }
            } catch (_: Exception) {
                // cae a la inferencia por nombre
            }
        }
        return inferCategory(label, packageName)
    }

    private fun inferCategory(label: String, packageName: String): AppCategory {
        val text = "$label $packageName".lowercase()
        fun has(vararg keywords: String) = keywords.any { text.contains(it) }

        return when {
            has("game", "juego") -> AppCategory.GAMES
            has(
                "whatsapp", "telegram", "message", "sms", "mms", "chat", "contact", "dialer",
                "phone", "instagram", "facebook", "twitter", "tiktok", "snapchat", "discord", "social"
            ) -> AppCategory.SOCIAL
            has(
                "music", "video", "youtube", "spotify", "netflix", "camera", "photo",
                "gallery", "media", "podcast", "player"
            ) -> AppCategory.ENTERTAINMENT
            has(
                "calendar", "clock", "calculator", "browser", "chrome", "mail", "docs",
                "drive", "office", "note", "files", "maps"
            ) -> AppCategory.PRODUCTIVITY
            else -> AppCategory.UTILITIES
        }
    }
}
