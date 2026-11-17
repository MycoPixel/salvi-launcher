package com.salvador.salvitolauncher.data

import androidx.compose.ui.graphics.ImageBitmap

enum class AppIconTheme { DEFAULT, DARK, TINTED }

enum class AppCategory { GAMES, SOCIAL, ENTERTAINMENT, PRODUCTIVITY, UTILITIES }

enum class WallpaperMode { GRADIENT, SYSTEM, CUSTOM }

/**
 * Una app instalada. El icono se convierte a bitmap una sola vez al cargar (en un hilo de fondo)
 * para no hacerlo durante la composición.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val category: AppCategory
)
