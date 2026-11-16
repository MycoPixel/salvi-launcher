package com.salvador.salvitolauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toBitmap
import com.salvador.salvitolauncher.ui.theme.SalvitoLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

enum class AppIconTheme {
    DEFAULT,
    DARK,
    TINTED
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val category: String,
    val badgeCount: Int = 0
)

val wallpapers = listOf(
    // iOS Classic Deep Blue
    listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFF1B2B48)),
    // iOS Sunset Purple / Orange
    listOf(Color(0xFF4A154B), Color(0xFF572568), Color(0xFFE26D5C)),
    // iOS Emerald Dark
    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
    // iOS Midnight Dark
    listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF0D1117))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SalvitoLauncherTheme {
                var wallpaperIndex by remember { mutableIntStateOf(0) }
                val currentGradient = wallpapers[wallpaperIndex % wallpapers.size]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = currentGradient))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    wallpaperIndex = (wallpaperIndex + 1) % wallpapers.size
                                }
                            )
                        }
                ) {
                    IOSHomeScreenContent()
                }
            }
        }
    }
}

@Composable
fun IOSHomeScreenContent() {
    // Intercept back gesture gracefully using AndroidX BackHandler
    BackHandler {
        // Home screen replacement intercepts back press
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val columnsCount = if (isTablet) 6 else 4

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var appInfoTarget by remember { mutableStateOf<AppInfo?>(null) }
    var editingAppLabelTarget by remember { mutableStateOf<AppInfo?>(null) }
    var newLabelText by remember { mutableStateOf("") }

    var isWiggleMode by remember { mutableStateOf(false) }
    var isControlCenterVisible by remember { mutableStateOf(false) }
    var isLockScreenVisible by remember { mutableStateOf(false) }
    var isSiriVisible by remember { mutableStateOf(false) }
    var isLauncherSettingsVisible by remember { mutableStateOf(false) }
    var isSpanish by remember { mutableStateOf(true) }

    var selectedCategoryFolder by remember { mutableStateOf<Pair<String, List<AppInfo>>?>(null) }
    var iconTheme by remember { mutableStateOf(AppIconTheme.DEFAULT) }

    var hiddenPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customAppLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        allApps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            resolveInfos.map {
                val origLabel = it.loadLabel(pm).toString()
                val pkg = it.activityInfo.packageName
                val category = getAppCategory(context, pkg, origLabel, isSpanish)
                val badge = getAppBadgeCount(pkg)
                AppInfo(
                    packageName = pkg,
                    label = origLabel,
                    icon = it.loadIcon(pm),
                    category = category,
                    badgeCount = badge
                )
            }.sortedBy { it.label }
        }
    }

    val processedApps = remember(allApps, customAppLabels) {
        allApps.map { app ->
            val custom = customAppLabels[app.packageName]
            if (custom != null) app.copy(label = custom) else app
        }
    }

    val visibleApps = remember(processedApps, hiddenPackages) {
        processedApps.filter { !hiddenPackages.contains(it.packageName) }
    }

    val filteredApps = remember(visibleApps, searchQuery) {
        if (searchQuery.isBlank()) visibleApps
        else visibleApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    val dockCount = if (isTablet) 6 else 4
    val dockApps = remember(visibleApps, dockCount) {
        if (visibleApps.size >= dockCount) visibleApps.take(dockCount) else visibleApps
    }

    val gridApps = remember(filteredApps, dockApps, searchQuery) {
        if (searchQuery.isBlank() && filteredApps.size >= dockCount) {
            filteredApps.drop(dockCount)
        } else {
            filteredApps
        }
    }

    val pageSize = if (isTablet) 24 else 16
    val homeGridPages = remember(gridApps, pageSize) {
        gridApps.chunked(pageSize)
    }

    val totalPageCount = remember(homeGridPages) {
        (homeGridPages.size + 1).coerceAtLeast(2)
    }

    val pagerState = rememberPagerState(pageCount = { totalPageCount })

    val infiniteTransition = rememberInfiniteTransition(label = "wiggle")
    val wiggleRotation by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle_rot"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isWiggleMode) isWiggleMode = false
                    },
                    onLongPress = {
                        isLauncherSettingsVisible = true
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            StatusBarController(
                onControlCenterToggle = { isControlCenterVisible = !isControlCenterVisible },
                onLockScreenToggle = { isLockScreenVisible = true }
            )

            DynamicIslandPill(isSpanish = isSpanish)

            IOSSmartStackWidgetHeader(
                isSpanish = isSpanish,
                isTablet = isTablet,
                onIconThemeToggle = {
                    iconTheme = when (iconTheme) {
                        AppIconTheme.DEFAULT -> AppIconTheme.DARK
                        AppIconTheme.DARK -> AppIconTheme.TINTED
                        AppIconTheme.TINTED -> AppIconTheme.DEFAULT
                    }
                },
                currentThemeName = iconTheme.name
            )

            SpotlightSearch(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onSiriClick = { isSiriVisible = true },
                isSpanish = isSpanish
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    if (pageIndex < homeGridPages.size) {
                        val pageApps = homeGridPages.getOrElse(pageIndex) { emptyList() }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnsCount),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isTablet) 32.dp else 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            userScrollEnabled = false
                        ) {
                            items(pageApps, key = { it.packageName }) { app ->
                                AppItem(
                                    app = app,
                                    iconTheme = iconTheme,
                                    isWiggle = isWiggleMode,
                                    wiggleAngle = wiggleRotation,
                                    onLongClick = {
                                        isWiggleMode = true
                                        appInfoTarget = app
                                    },
                                    onDeleteClick = {
                                        appInfoTarget = app
                                    }
                                )
                            }
                        }
                    } else {
                        AppLibraryPage(
                            apps = processedApps,
                            columnsCount = if (isTablet) 3 else 2,
                            isSpanish = isSpanish,
                            onFolderClick = { category, appsInCategory ->
                                selectedCategoryFolder = Pair(category, appsInCategory)
                            }
                        )
                    }
                }
            }

            if (searchQuery.isBlank()) {
                PageIndicatorDots(
                    pageCount = totalPageCount,
                    currentPage = pagerState.currentPage
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (searchQuery.isBlank() && dockApps.isNotEmpty()) {
                IOSDock(
                    dockApps = dockApps,
                    iconTheme = iconTheme,
                    isWiggle = isWiggleMode,
                    wiggleAngle = wiggleRotation,
                    isTablet = isTablet,
                    onAppLongClick = {
                        isWiggleMode = true
                        appInfoTarget = it
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = selectedCategoryFolder != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedCategoryFolder?.let { (category, appsInFolder) ->
                IOSCategoryFolderOverlay(
                    categoryName = category,
                    apps = appsInFolder,
                    iconTheme = iconTheme,
                    isTablet = isTablet,
                    onDismiss = { selectedCategoryFolder = null },
                    onAppLongClick = {
                        isWiggleMode = true
                        appInfoTarget = it
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = isControlCenterVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            IOSControlCenterOverlay(
                isSpanish = isSpanish,
                onDismiss = { isControlCenterVisible = false }
            )
        }

        AnimatedVisibility(
            visible = isLockScreenVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IOSLockScreenOverlay(
                isSpanish = isSpanish,
                onUnlock = { isLockScreenVisible = false }
            )
        }

        AnimatedVisibility(
            visible = isSiriVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SiriOrbOverlay(
                isSpanish = isSpanish,
                onDismiss = { isSiriVisible = false }
            )
        }
    }

    if (appInfoTarget != null) {
        val target = appInfoTarget!!
        AlertDialog(
            onDismissRequest = { appInfoTarget = null },
            title = { Text(target.label) },
            text = { Text(if (isSpanish) "Elige una opción para ${target.label}:" else "Choose an option for ${target.label}:") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${target.packageName}".toUri()
                        }
                        context.startActivity(intent)
                        appInfoTarget = null
                    }) {
                        Text(if (isSpanish) "Información de la App" else "App Info")
                    }
                    TextButton(onClick = {
                        editingAppLabelTarget = target
                        newLabelText = target.label
                        appInfoTarget = null
                    }) {
                        Text(if (isSpanish) "Renombrar App" else "Rename App")
                    }
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = {
                        hiddenPackages = hiddenPackages + target.packageName
                        appInfoTarget = null
                    }) {
                        Text(if (isSpanish) "Ocultar App" else "Hide App", color = Color.Gray)
                    }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = "package:${target.packageName}".toUri()
                        }
                        context.startActivity(intent)
                        appInfoTarget = null
                    }) {
                        Text(if (isSpanish) "Desinstalar" else "Uninstall", color = Color.Red)
                    }
                }
            }
        )
    }

    if (editingAppLabelTarget != null) {
        val target = editingAppLabelTarget!!
        AlertDialog(
            onDismissRequest = { editingAppLabelTarget = null },
            title = { Text(if (isSpanish) "Renombrar ${target.label}" else "Rename ${target.label}") },
            text = {
                OutlinedTextField(
                    value = newLabelText,
                    onValueChange = { newLabelText = it },
                    singleLine = true,
                    label = { Text(if (isSpanish) "Nombre de la App" else "App Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newLabelText.isNotBlank()) {
                        customAppLabels = customAppLabels + (target.packageName to newLabelText)
                    }
                    editingAppLabelTarget = null
                }) {
                    Text(if (isSpanish) "Guardar" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAppLabelTarget = null }) {
                    Text(if (isSpanish) "Cancelar" else "Cancel")
                }
            }
        )
    }

    if (isLauncherSettingsVisible) {
        AlertDialog(
            onDismissRequest = { isLauncherSettingsVisible = false },
            title = { Text(if (isSpanish) "Ajustes de SalvitoLauncher" else "SalvitoLauncher Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSpanish) "Idioma: Español" else "Language: English")
                        Switch(
                            checked = isSpanish,
                            onCheckedChange = { isSpanish = it }
                        )
                    }

                    if (hiddenPackages.isNotEmpty()) {
                        Text(
                            text = if (isSpanish) "Apps ocultas (${hiddenPackages.size}):" else "Hidden Apps (${hiddenPackages.size}):",
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { hiddenPackages = emptySet() }) {
                            Text(if (isSpanish) "Mostrar todas las apps" else "Unhide All Apps", color = Color(0xFF007AFF))
                        }
                    }

                    if (customAppLabels.isNotEmpty()) {
                        TextButton(onClick = { customAppLabels = emptyMap() }) {
                            Text(if (isSpanish) "Restablecer nombres de apps" else "Reset App Names", color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isLauncherSettingsVisible = false }) {
                    Text(if (isSpanish) "Cerrar" else "Close")
                }
            }
        )
    }
}

@Composable
fun DynamicIslandPill(isSpanish: Boolean) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(if (isExpanded) 240.dp else 120.dp)
                .height(if (isExpanded) 42.dp else 26.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎵 iOS Chill Beats", color = Color.White, fontSize = 12.sp)
                    Text(text = "🔋 88%", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                    Text(
                        text = if (isSpanish) "Cargando..." else "Charging...",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IOSSmartStackWidgetHeader(
    isSpanish: Boolean,
    isTablet: Boolean,
    onIconThemeToggle: () -> Unit,
    currentThemeName: String
) {
    var stackIndex by remember { mutableIntStateOf(0) }
    val dateString = remember(isSpanish) {
        val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.getDefault()
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", locale)
        sdf.format(Date())
    }

    val widgetWidth = if (isTablet) 240.dp else 165.dp

    val draggableState = rememberDraggableState { delta ->
        if (delta > 20) {
            stackIndex = (stackIndex + 1) % 4
        } else if (delta < -20) {
            stackIndex = if (stackIndex - 1 < 0) 3 else stackIndex - 1
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(widgetWidth)
                .height(85.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .draggable(state = draggableState, orientation = Orientation.Vertical)
                .clickable { stackIndex = (stackIndex + 1) % 4 }
                .padding(12.dp)
        ) {
            when (stackIndex) {
                0 -> {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = dateString.uppercase(Locale.getDefault()),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Salvito",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                1 -> {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = if (isSpanish) "BATERÍA Y ALMACENAMIENTO" else "BATTERY & STORAGE",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "🔋 88%", color = Color.White, fontSize = 14.sp)
                            Text(text = "💾 64GB", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                2 -> {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = if (isSpanish) "RECORDATORIOS" else "REMINDERS",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSpanish) "• Reunión 2 PM\n• Comprar Café" else "• Team Sync 2 PM\n• Buy Coffee",
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
                else -> {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = if (isSpanish) "FRASE FAVORITA" else "FAVORITE QUOTE",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\"Think Different\"",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .width(widgetWidth)
                .height(85.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onIconThemeToggle() }
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "${if (isSpanish) "TEMA" else "THEME"}: $currentThemeName",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "22°",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = "☀️",
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IOSControlCenterOverlay(isSpanish: Boolean, onDismiss: () -> Unit) {
    var wifiOn by remember { mutableStateOf(true) }
    var bluetoothOn by remember { mutableStateOf(true) }
    var flashlightOn by remember { mutableStateOf(false) }
    var airplaneOn by remember { mutableStateOf(false) }

    var brightness by remember { mutableFloatStateOf(0.7f) }
    var volume by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(25.dp)
                } else {
                    Modifier
                }
            )
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                .width(340.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isSpanish) "Centro de Control" else "Control Center",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlCenterToggle(
                    label = "Wi-Fi",
                    iconText = "📶",
                    isActive = wifiOn,
                    onClick = { wifiOn = !wifiOn }
                )
                ControlCenterToggle(
                    label = "Bluetooth",
                    iconText = "ᛒ",
                    isActive = bluetoothOn,
                    onClick = { bluetoothOn = !bluetoothOn }
                )
                ControlCenterToggle(
                    label = if (isSpanish) "Linterna" else "Flashlight",
                    iconText = "🔦",
                    isActive = flashlightOn,
                    onClick = { flashlightOn = !flashlightOn }
                )
                ControlCenterToggle(
                    label = if (isSpanish) "Avión" else "Airplane",
                    iconText = "✈️",
                    isActive = airplaneOn,
                    onClick = { airplaneOn = !airplaneOn }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = if (isSpanish) "Brillo" else "Brightness", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Text(text = if (isSpanish) "Volumen" else "Volume", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }

            TextButton(onClick = onDismiss) {
                Text(if (isSpanish) "Cerrar Centro de Control" else "Close Control Center", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IOSLockScreenOverlay(isSpanish: Boolean, onUnlock: () -> Unit) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember(isSpanish) { mutableStateOf("") }

    LaunchedEffect(isSpanish) {
        val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.getDefault()
        val timeSdf = SimpleDateFormat("h:mm", Locale.getDefault())
        val dateSdf = SimpleDateFormat("EEEE, d 'de' MMMM", locale)
        while (true) {
            val now = Date()
            currentTime = timeSdf.format(now)
            val rawDate = dateSdf.format(now)
            currentDate = rawDate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            delay(1000.milliseconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onUnlock() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🔒", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDate,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentTime.ifEmpty { "9:41" },
                    color = Color.White,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔦", fontSize = 22.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📷", fontSize = 22.sp)
                    }
                }

                Text(
                    text = if (isSpanish) "Toca para desbloquear" else "Tap to unlock",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SiriOrbOverlay(isSpanish: Boolean, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "siri")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "siri_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            Text(
                text = if (isSpanish) "¿En qué puedo ayudarte?" else "How can I help you?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF2D55),
                                Color(0xFF5856D6),
                                Color(0xFF007AFF),
                                Color(0xFF4CD964)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun ControlCenterToggle(
    label: String,
    iconText: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color(0xFF007AFF) else Color.White.copy(alpha = 0.25f)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconText, fontSize = 20.sp, color = Color.White)
        }
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun AppLibraryPage(
    apps: List<AppInfo>,
    columnsCount: Int,
    isSpanish: Boolean,
    onFolderClick: (String, List<AppInfo>) -> Unit
) {
    val categories = remember(apps) {
        apps.groupBy { it.category }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = if (isSpanish) "Biblioteca de Apps" else "App Library",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories.keys.toList()) { category ->
                val categoryApps = categories[category] ?: emptyList()
                IOSLibraryFolderCard(
                    categoryName = category,
                    apps = categoryApps,
                    onClick = { onFolderClick(category, categoryApps) }
                )
            }
        }
    }
}

@Composable
fun IOSLibraryFolderCard(
    categoryName: String,
    apps: List<AppInfo>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(apps.take(4)) { app ->
                    val bitmap = remember(app.icon) {
                        try {
                            app.icon.toBitmap(width = 80, height = 80).asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            Text(
                text = categoryName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IOSCategoryFolderOverlay(
    categoryName: String,
    apps: List<AppInfo>,
    iconTheme: AppIconTheme,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(20.dp)
                } else {
                    Modifier
                }
            )
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(if (isTablet) 480.dp else 320.dp)
                .height(if (isTablet) 450.dp else 380.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = categoryName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTablet) 4 else 3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppItem(
                            app = app,
                            iconTheme = iconTheme,
                            isWiggle = false,
                            wiggleAngle = 0f,
                            onLongClick = { onAppLongClick(app) },
                            onDeleteClick = { onAppLongClick(app) }
                        )
                    }
                }
            }
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
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
fun StatusBarController(
    onControlCenterToggle: () -> Unit,
    onLockScreenToggle: () -> Unit
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("h:mm", Locale.getDefault())
            currentTime = sdf.format(Date())
            delay(1000.milliseconds)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentTime.ifEmpty { "9:41" },
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onLockScreenToggle() }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onControlCenterToggle() }
        ) {
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 10.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 10.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 11.dp)
                    .background(Color.Transparent, RoundedCornerShape(3.dp))
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.85f)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun IOSDock(
    dockApps: List<AppInfo>,
    iconTheme: AppIconTheme,
    isWiggle: Boolean,
    wiggleAngle: Float,
    isTablet: Boolean,
    onAppLongClick: (AppInfo) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 64.dp else 24.dp)
            .height(if (isTablet) 100.dp else 90.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.forEachIndexed { index, app ->
                val angle = if (isWiggle) (if (index % 2 == 0) wiggleAngle else -wiggleAngle) else 0f
                DockAppItem(
                    app = app,
                    iconTheme = iconTheme,
                    rotationAngle = angle,
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@Composable
fun DockAppItem(
    app: AppInfo,
    iconTheme: AppIconTheme,
    rotationAngle: Float,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "dock_scale")

    Box(
        modifier = Modifier
            .rotate(rotationAngle)
            .scale(scale)
            .size(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                },
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = remember(app.icon) {
            try {
                app.icon.toBitmap(width = 120, height = 120).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }

        if (bitmap != null) {
            val filter = when (iconTheme) {
                AppIconTheme.DARK -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.2f) })
                AppIconTheme.TINTED -> ColorFilter.tint(Color(0xFF3B82F6))
                else -> null
            }
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                colorFilter = filter,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        if (app.badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (app.badgeCount > 9) "9+" else app.badgeCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    iconTheme: AppIconTheme,
    isWiggle: Boolean,
    wiggleAngle: Float,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "app_scale")

    val rotation = if (isWiggle) wiggleAngle else 0f

    Box(
        modifier = Modifier
            .scale(scale)
            .rotate(rotation)
            .width(72.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    },
                    onLongClick = onLongClick
                )
                .fillMaxWidth()
        ) {
            val bitmap = remember(app.icon) {
                try {
                    app.icon.toBitmap(width = 144, height = 144).asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }

            val containerColor = when (iconTheme) {
                AppIconTheme.DARK -> Color(0xFF1E1E1E)
                AppIconTheme.TINTED -> Color(0xFF3B82F6).copy(alpha = 0.35f)
                else -> Color.White.copy(alpha = 0.12f)
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    val filter = when (iconTheme) {
                        AppIconTheme.DARK -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.25f) })
                        AppIconTheme.TINTED -> ColorFilter.tint(Color.White)
                        else -> null
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        colorFilter = filter,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = app.label,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (app.badgeCount > 0 && !isWiggle) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (app.badgeCount > 9) "9+" else app.badgeCount.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isWiggle) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.9f))
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpotlightSearch(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSiriClick: () -> Unit,
    isSpanish: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
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
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = if (isSpanish) "Buscar" else "Search",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "🎙️",
                fontSize = 16.sp,
                modifier = Modifier.clickable { onSiriClick() }
            )
        }
    }
}

fun getAppCategory(context: Context, packageName: String, label: String, isSpanish: Boolean): String {
    return try {
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> if (isSpanish) "Juegos" else "Games"
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE -> if (isSpanish) "Entretenimiento" else "Entertainment"
                ApplicationInfo.CATEGORY_SOCIAL,
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Social"
                ApplicationInfo.CATEGORY_MAPS,
                ApplicationInfo.CATEGORY_NEWS,
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> if (isSpanish) "Productividad" else "Productivity"
                else -> inferCategoryFromLabel(label, packageName, isSpanish)
            }
        } else {
            inferCategoryFromLabel(label, packageName, isSpanish)
        }
    } catch (_: Exception) {
        inferCategoryFromLabel(label, packageName, isSpanish)
    }
}

fun inferCategoryFromLabel(label: String, packageName: String, isSpanish: Boolean): String {
    val l = label.lowercase()
    val p = packageName.lowercase()
    return when {
        l.contains("game") || p.contains("game") || l.contains("juego") -> if (isSpanish) "Juegos" else "Games"
        l.contains("chat") || l.contains("message") || l.contains("social") || l.contains("phone") || l.contains("contact") || p.contains("whatsapp") || p.contains("telegram") -> "Social"
        l.contains("music") || l.contains("video") || l.contains("youtube") || l.contains("media") || l.contains("camera") || l.contains("photo") || l.contains("gallery") -> if (isSpanish) "Entretenimiento" else "Entertainment"
        l.contains("setting") || l.contains("clock") || l.contains("calculator") || l.contains("file") || l.contains("calendar") || l.contains("browser") || l.contains("chrome") -> if (isSpanish) "Productividad" else "Productivity"
        else -> if (isSpanish) "Utilidades" else "Utilities"
    }
}

fun getAppBadgeCount(packageName: String): Int {
    val p = packageName.lowercase()
    return when {
        p.contains("messaging") || p.contains("mms") || p.contains("whatsapp") -> 3
        p.contains("dialer") || p.contains("phone") -> 1
        p.contains("mail") || p.contains("gmail") -> 7
        else -> 0
    }
}
