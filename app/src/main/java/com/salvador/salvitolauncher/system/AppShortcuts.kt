package com.salvador.salvitolauncher.system

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppShortcutItem(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val shortcutInfo: ShortcutInfo
)

object AppShortcuts {

    fun getShortcuts(context: Context, packageName: String): List<AppShortcutItem> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return emptyList()

        return try {
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            }
            val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
            val density = context.resources.displayMetrics.densityDpi

            shortcuts.take(4).mapNotNull { shortcut ->
                val label = (shortcut.shortLabel ?: shortcut.longLabel ?: "").toString()
                if (label.isBlank()) return@mapNotNull null

                val iconDrawable = try {
                    launcherApps.getShortcutIconDrawable(shortcut, density)
                } catch (_: Exception) {
                    null
                }

                val bitmap = try {
                    iconDrawable?.toBitmap(64, 64)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }

                AppShortcutItem(
                    id = shortcut.id,
                    packageName = shortcut.`package`,
                    label = label,
                    icon = bitmap,
                    shortcutInfo = shortcut
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchShortcut(context: Context, shortcutItem: AppShortcutItem): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return false

        return try {
            launcherApps.startShortcut(
                shortcutItem.packageName,
                shortcutItem.id,
                null,
                null,
                Process.myUserHandle()
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
