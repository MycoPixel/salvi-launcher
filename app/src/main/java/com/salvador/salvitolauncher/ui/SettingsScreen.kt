package com.salvador.salvitolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salvador.salvitolauncher.data.AppIconTheme
import com.salvador.salvitolauncher.data.AppInfo
import com.salvador.salvitolauncher.data.IconPackManager
import com.salvador.salvitolauncher.data.LauncherSettings
import com.salvador.salvitolauncher.data.WallpaperMode
import com.salvador.salvitolauncher.system.BadgeState
import com.salvador.salvitolauncher.system.LockAccessibilityService
import com.salvador.salvitolauncher.system.openAccessibilitySettings
import com.salvador.salvitolauncher.system.openAndroidSettings
import com.salvador.salvitolauncher.system.openDefaultLauncherSettings
import com.salvador.salvitolauncher.system.openNotificationAccessSettings

private val Panel = Color(0xFF2C2C2E)
private val Accent = Color(0xFF0A84FF)

/** Pantalla de ajustes a pantalla completa (reemplaza al antiguo diálogo con 3 opciones). */
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    allApps: List<AppInfo>,
    onPickPhoto: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val es = settings.isSpanish

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(es, "Ajustes", "Settings"),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) {
                Text(tr(es, "Listo", "Done"), color = Accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ------------------------------------------------------------ fondo
            SectionTitle(tr(es, "Fondo de pantalla", "Wallpaper"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WallpaperPresets.forEachIndexed { index, preset ->
                    WallpaperTile(
                        label = tr(es, preset.nameEs, preset.nameEn),
                        selected = settings.wallpaperMode == WallpaperMode.GRADIENT && settings.gradientIndex == index,
                        brush = Brush.verticalGradient(preset.colors),
                        onClick = { settings.selectGradient(index) }
                    )
                }
                WallpaperTile(
                    label = tr(es, "Sistema", "System"),
                    selected = settings.wallpaperMode == WallpaperMode.SYSTEM,
                    brush = Brush.verticalGradient(listOf(Color(0xFF636366), Color(0xFF48484A))),
                    icon = "🖼️",
                    onClick = { settings.selectSystemWallpaper() }
                )
                WallpaperTile(
                    label = tr(es, "Mi foto", "My photo"),
                    selected = settings.wallpaperMode == WallpaperMode.CUSTOM,
                    brush = Brush.verticalGradient(listOf(Color(0xFF3A3A3C), Color(0xFF2C2C2E))),
                    icon = "＋",
                    onClick = onPickPhoto
                )
            }

            // ------------------------------------------------------------ iconos
            SectionTitle(tr(es, "Estilo de iconos", "Icon style"))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip(tr(es, "Normal", "Default"), settings.iconTheme == AppIconTheme.DEFAULT, Modifier.weight(1f)) {
                        settings.updateIconTheme(AppIconTheme.DEFAULT)
                    }
                    ThemeChip(tr(es, "Oscuro", "Dark"), settings.iconTheme == AppIconTheme.DARK, Modifier.weight(1f)) {
                        settings.updateIconTheme(AppIconTheme.DARK)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip(tr(es, "Tintado", "Tinted"), settings.iconTheme == AppIconTheme.TINTED, Modifier.weight(1f)) {
                        settings.updateIconTheme(AppIconTheme.TINTED)
                    }
                    ThemeChip("Liquid Glass", settings.iconTheme == AppIconTheme.LIQUID_GLASS, Modifier.weight(1f)) {
                        settings.updateIconTheme(AppIconTheme.LIQUID_GLASS)
                    }
                }

                if (settings.iconTheme == AppIconTheme.TINTED) {
                    val colors = listOf(
                        0xFF3B82F6L to "Azul",
                        0xFF8B5CF6L to "Púrpura",
                        0xFFEC4899L to "Rosa",
                        0xFFEF4444L to "Rojo",
                        0xFFF97316L to "Naranja",
                        0xFF10B981L to "Verde",
                        0xFF06B6D4L to "Cian",
                        0xFFEAB308L to "Dorado"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.forEach { (colorValue, _) ->
                            val selected = settings.customTintColor == colorValue
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(colorValue))
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { settings.updateCustomTintColor(colorValue) }
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------------ paquete de iconos (Play Store)
            SectionTitle(tr(es, "Paquete de iconos (Play Store)", "Icon pack (Play Store)"))
            val iconPacks = remember(context) { IconPackManager.getAvailableIconPacks(context) }
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settings.updateIconPackPackage(null) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(es, "Predeterminado (Sistema)", "Default (System)"),
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(
                        selected = settings.iconPackPackage == null,
                        onClick = { settings.updateIconPackPackage(null) },
                        colors = RadioButtonDefaults.colors(selectedColor = Accent)
                    )
                }

                iconPacks.forEach { pack ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settings.updateIconPackPackage(pack.packageName) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pack.icon != null) {
                            Image(
                                bitmap = pack.icon,
                                contentDescription = pack.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = pack.label,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = settings.iconPackPackage == pack.packageName,
                            onClick = { settings.updateIconPackPackage(pack.packageName) },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent)
                        )
                    }
                }

                ActionRow(tr(es, "Descargar paquetes de iconos de Play Store", "Download icon packs from Play Store")) {
                    IconPackManager.openPlayStoreIconPacks(context)
                }
            }

            // ------------------------------------------------------------ general
            SectionTitle(tr(es, "General", "General"))
            SettingsCard {
                SwitchRow(tr(es, "Español", "Spanish"), es) { settings.updateSpanish(it) }
                SwitchRow(tr(es, "Mostrar nombres de apps", "Show app names"), settings.showLabels) {
                    settings.updateShowLabels(it)
                }
                SwitchRow(tr(es, "Iconos grandes (iOS 18)", "Large icons (iOS 18)"), settings.useLargeIcons) {
                    settings.updateUseLargeIcons(it)
                }
                SwitchRow(tr(es, "Isla dinámica", "Dynamic Island"), settings.showIsland) {
                    settings.updateShowIsland(it)
                }
                SwitchRow(tr(es, "Números de notificaciones", "Notification badges"), settings.showBadges) {
                    settings.updateShowBadges(it)
                    if (it && !BadgeState.connected) openNotificationAccessSettings(context)
                }
                if (settings.showBadges && !BadgeState.connected) {
                    ActionRow(tr(es, "Conceder acceso a notificaciones", "Grant notification access")) {
                        openNotificationAccessSettings(context)
                    }
                }
                if (LockAccessibilityService.isSupported) {
                    SwitchRow(tr(es, "Doble toque para bloquear", "Double tap to lock"), settings.doubleTapLock) {
                        settings.updateDoubleTapLock(it)
                        if (it && !LockAccessibilityService.running) openAccessibilitySettings(context)
                    }
                    if (settings.doubleTapLock && !LockAccessibilityService.running) {
                        ActionRow(tr(es, "Activar el servicio de bloqueo", "Turn on the lock service")) {
                            openAccessibilitySettings(context)
                        }
                    }
                }
            }

            // ------------------------------------------------------------ apps bloqueadas con huella
            if (settings.lockedPackages.isNotEmpty()) {
                SectionTitle(tr(es, "Apps protegidas con Huella / PIN", "Apps locked with Biometrics / PIN"))
                SettingsCard {
                    settings.lockedPackages.sorted().forEach { pkg ->
                        val label = allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔒  $label",
                                color = Color.White,
                                fontSize = 15.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { settings.unlockApp(pkg) }) {
                                Text(tr(es, "Desbloquear", "Unlock"), color = Accent)
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------ apps ocultas
            if (settings.hiddenPackages.isNotEmpty()) {
                SectionTitle(tr(es, "Apps quitadas de la pantalla de inicio", "Apps removed from Home Screen"))
                SettingsCard {
                    settings.hiddenPackages.sorted().forEach { pkg ->
                        val label = allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 15.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { settings.unhide(pkg) }) {
                                Text(tr(es, "Mostrar", "Show"), color = Accent)
                            }
                        }
                    }
                    TextButton(
                        onClick = { settings.unhideAll() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(tr(es, "Mostrar todas", "Show all"), color = Accent)
                    }
                }
            }

            if (settings.customLabels.isNotEmpty()) {
                SettingsCard {
                    TextButton(
                        onClick = { settings.resetLabels() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = tr(
                                es,
                                "Restablecer nombres de apps (${settings.customLabels.size})",
                                "Reset app names (${settings.customLabels.size})"
                            ),
                            color = Accent
                        )
                    }
                }
            }

            // ------------------------------------------------------------ sistema
            SectionTitle(tr(es, "Sistema", "System"))
            SettingsCard {
                ActionRow(tr(es, "Establecer como launcher predeterminado", "Set as default launcher")) {
                    openDefaultLauncherSettings(context)
                }
                ActionRow(tr(es, "Ajustes de Android", "Android settings")) {
                    openAndroidSettings(context)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Accent,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else Panel)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WallpaperTile(
    label: String,
    selected: Boolean,
    brush: Brush,
    onClick: () -> Unit,
    icon: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(brush)
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) Text(text = icon, fontSize = 22.sp, color = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.width(68.dp),
            textAlign = TextAlign.Center
        )
    }
}
