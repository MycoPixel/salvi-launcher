package com.salvador.salvitolauncher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Ajustes del launcher. Cada propiedad es un estado de Compose (la UI se actualiza sola)
 * y se guarda en SharedPreferences, así sobrevive a reinicios.
 */
class LauncherSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    var isSpanish by mutableStateOf(prefs.getBoolean(KEY_SPANISH, Locale.getDefault().language == "es"))
        private set

    var iconTheme by mutableStateOf(readEnum(KEY_ICON_THEME, AppIconTheme.DEFAULT))
        private set

    var wallpaperMode by mutableStateOf(readEnum(KEY_WALLPAPER_MODE, WallpaperMode.GRADIENT))
        private set

    var gradientIndex by mutableIntStateOf(prefs.getInt(KEY_GRADIENT, 0))
        private set

    /** Sube cada vez que se guarda una foto nueva, para que la UI la vuelva a cargar. */
    var wallpaperVersion by mutableIntStateOf(prefs.getInt(KEY_WALLPAPER_VERSION, 0))
        private set

    var showLabels by mutableStateOf(prefs.getBoolean(KEY_SHOW_LABELS, true))
        private set

    var showIsland by mutableStateOf(prefs.getBoolean(KEY_SHOW_ISLAND, true))
        private set

    var hiddenPackages: Set<String> by mutableStateOf(
        prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
    )
        private set

    var customLabels: Map<String, String> by mutableStateOf(readLabels())
        private set

    /** null = todavía no se eligió un dock (se calcula con apps por defecto la primera vez). */
    var dockPackages: List<String>? by mutableStateOf(readDock())
        private set

    /** null = todavía no se movió nada: las apps van por orden alfabético. */
    var homeLayout: List<LayoutEntry>? by mutableStateOf(
        prefs.getString(KEY_LAYOUT, null)?.let { HomeLayoutOps.fromJson(it) }
    )
        private set

    var widgetIds: List<Int> by mutableStateOf(readWidgetIds())
        private set

    var showBadges by mutableStateOf(prefs.getBoolean(KEY_BADGES, true))
        private set

    var doubleTapLock by mutableStateOf(prefs.getBoolean(KEY_DOUBLE_TAP_LOCK, false))
        private set

    fun updateHomeLayout(layout: List<LayoutEntry>) {
        homeLayout = layout
        prefs.edit { putString(KEY_LAYOUT, HomeLayoutOps.toJson(layout)) }
    }

    fun addWidget(appWidgetId: Int) {
        widgetIds = widgetIds + appWidgetId
        saveWidgets()
    }

    fun removeWidget(appWidgetId: Int) {
        widgetIds = widgetIds - appWidgetId
        saveWidgets()
    }

    fun updateShowBadges(value: Boolean) {
        showBadges = value
        prefs.edit { putBoolean(KEY_BADGES, value) }
    }

    fun updateDoubleTapLock(value: Boolean) {
        doubleTapLock = value
        prefs.edit { putBoolean(KEY_DOUBLE_TAP_LOCK, value) }
    }

    private fun saveWidgets() {
        prefs.edit { putString(KEY_WIDGETS, JSONArray(widgetIds).toString()) }
    }

    private fun readWidgetIds(): List<Int> {
        val raw = prefs.getString(KEY_WIDGETS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { array.getInt(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun updateSpanish(value: Boolean) {
        isSpanish = value
        prefs.edit { putBoolean(KEY_SPANISH, value) }
    }

    fun updateIconTheme(value: AppIconTheme) {
        iconTheme = value
        prefs.edit { putString(KEY_ICON_THEME, value.name) }
    }

    fun selectGradient(index: Int) {
        wallpaperMode = WallpaperMode.GRADIENT
        gradientIndex = index
        prefs.edit {
            putString(KEY_WALLPAPER_MODE, WallpaperMode.GRADIENT.name)
            putInt(KEY_GRADIENT, index)
        }
    }

    fun selectSystemWallpaper() {
        wallpaperMode = WallpaperMode.SYSTEM
        prefs.edit { putString(KEY_WALLPAPER_MODE, WallpaperMode.SYSTEM.name) }
    }

    fun selectCustomWallpaper() {
        wallpaperMode = WallpaperMode.CUSTOM
        wallpaperVersion += 1
        prefs.edit {
            putString(KEY_WALLPAPER_MODE, WallpaperMode.CUSTOM.name)
            putInt(KEY_WALLPAPER_VERSION, wallpaperVersion)
        }
    }

    fun updateShowLabels(value: Boolean) {
        showLabels = value
        prefs.edit { putBoolean(KEY_SHOW_LABELS, value) }
    }

    fun updateShowIsland(value: Boolean) {
        showIsland = value
        prefs.edit { putBoolean(KEY_SHOW_ISLAND, value) }
    }

    fun hide(packageName: String) {
        hiddenPackages = hiddenPackages + packageName
        prefs.edit { putStringSet(KEY_HIDDEN, hiddenPackages.toSet()) }
    }

    fun unhide(packageName: String) {
        hiddenPackages = hiddenPackages - packageName
        prefs.edit { putStringSet(KEY_HIDDEN, hiddenPackages.toSet()) }
    }

    fun unhideAll() {
        hiddenPackages = emptySet()
        prefs.edit { putStringSet(KEY_HIDDEN, emptySet()) }
    }

    fun rename(packageName: String, label: String) {
        customLabels = customLabels + (packageName to label)
        saveLabels()
    }

    fun resetLabels() {
        customLabels = emptyMap()
        saveLabels()
    }

    fun updateDock(packages: List<String>) {
        dockPackages = packages
        prefs.edit { putString(KEY_DOCK, JSONArray(packages).toString()) }
    }

    private fun saveLabels() {
        prefs.edit { putString(KEY_CUSTOM_LABELS, JSONObject(customLabels).toString()) }
    }

    private fun readLabels(): Map<String, String> {
        val raw = prefs.getString(KEY_CUSTOM_LABELS, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getString(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun readDock(): List<String>? {
        val raw = prefs.getString(KEY_DOCK, null) ?: return null
        return try {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        } catch (_: Exception) {
            null
        }
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T {
        return try {
            enumValueOf<T>(prefs.getString(key, default.name) ?: default.name)
        } catch (_: IllegalArgumentException) {
            default
        }
    }

    private companion object {
        const val KEY_SPANISH = "spanish"
        const val KEY_ICON_THEME = "icon_theme"
        const val KEY_WALLPAPER_MODE = "wallpaper_mode"
        const val KEY_GRADIENT = "gradient_index"
        const val KEY_WALLPAPER_VERSION = "wallpaper_version"
        const val KEY_SHOW_LABELS = "show_labels"
        const val KEY_SHOW_ISLAND = "show_island"
        const val KEY_HIDDEN = "hidden_packages"
        const val KEY_CUSTOM_LABELS = "custom_labels"
        const val KEY_DOCK = "dock_packages"
        const val KEY_LAYOUT = "home_layout"
        const val KEY_WIDGETS = "widget_ids"
        const val KEY_BADGES = "show_badges"
        const val KEY_DOUBLE_TAP_LOCK = "double_tap_lock"
    }
}
