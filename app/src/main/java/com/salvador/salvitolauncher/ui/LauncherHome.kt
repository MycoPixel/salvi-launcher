package com.salvador.salvitolauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.salvador.salvitolauncher.data.AppCategory
import com.salvador.salvitolauncher.data.AppInfo
import com.salvador.salvitolauncher.data.AppRepository
import com.salvador.salvitolauncher.data.LauncherSettings
import com.salvador.salvitolauncher.data.WallpaperStorage
import com.salvador.salvitolauncher.system.launchApp
import com.salvador.salvitolauncher.system.openAppInfo
import com.salvador.salvitolauncher.system.openVoiceAssistant
import com.salvador.salvitolauncher.system.rememberBatteryState
import com.salvador.salvitolauncher.system.uninstallApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla principal del launcher.
 *
 * @param homeSignal sube cada vez que el usuario pulsa Home estando ya en el launcher
 *                   (vuelve a la primera página y cierra overlays, como iOS).
 */
@Composable
fun LauncherHome(settings: LauncherSettings, homeSignal: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val es = settings.isSpanish

    val isTablet = with(density) { windowInfo.containerSize.width.toDp() } >= 600.dp
    val columns = if (isTablet) 6 else 4
    val dockSize = if (isTablet) 6 else 4
    val iconSize = if (isTablet) 68.dp else 60.dp

    val battery by rememberBatteryState()
    val now by rememberNow()

    // ------------------------------------------------------------------ apps instaladas

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var packageTick by remember { mutableIntStateOf(0) }

    // Recarga la lista cuando se instala, actualiza o desinstala una app.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                packageTick++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        // Broadcasts protegidos del sistema: solo el sistema puede enviarlos.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(packageTick) {
        val loaded = withContext(Dispatchers.IO) { AppRepository.loadApps(context) }
        allApps = loaded
        if (settings.dockPackages == null) {
            settings.updateDock(withContext(Dispatchers.IO) { AppRepository.defaultDock(context, loaded) })
        }
    }

    // ------------------------------------------------------------------ fondo personalizado

    var customWallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(settings.wallpaperVersion) {
        customWallpaper = withContext(Dispatchers.IO) { WallpaperStorage.load(context) }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { WallpaperStorage.save(context, uri) }
                if (saved) settings.selectCustomWallpaper()
            }
        }
    }

    // ------------------------------------------------------------------ estado de la UI

    var wiggle by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var openFolder by remember { mutableStateOf<AppCategory?>(null) }
    var controlCenterOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var backgroundMenuOpen by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var removeApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameText by remember { mutableStateOf("") }

    // ------------------------------------------------------------------ listas derivadas

    val labels = settings.customLabels
    val hidden = settings.hiddenPackages
    val dockPackages = settings.dockPackages ?: emptyList()

    val apps = remember(allApps, labels) {
        allApps
            .map { app -> labels[app.packageName]?.let { app.copy(label = it) } ?: app }
            .sortedBy { it.label.lowercase() }
    }
    val dockApps = remember(apps, dockPackages, hidden, dockSize) {
        val byPackage = apps.associateBy { it.packageName }
        dockPackages
            .filter { it !in hidden }
            .mapNotNull { byPackage[it] }
            .take(dockSize)
    }
    val homeApps = remember(apps, hidden, dockApps) {
        val inDock = dockApps.map { it.packageName }.toSet()
        apps.filter { it.packageName !in hidden && it.packageName !in inDock }
    }
    val searching = query.isNotBlank()
    val searchResults = remember(apps, query) {
        if (query.isBlank()) emptyList()
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val folderApps = remember(apps, openFolder) {
        openFolder?.let { category -> apps.filter { it.category == category } } ?: emptyList()
    }

    // ------------------------------------------------------------------ paginación adaptable

    // Cuántas filas caben realmente en el espacio disponible (antes eran 4 fijas y se cortaban).
    var gridAreaHeightPx by remember { mutableIntStateOf(0) }
    val rows = if (gridAreaHeightPx == 0) {
        4
    } else {
        ((with(density) { gridAreaHeightPx.toDp() } + 20.dp) / 104.dp).toInt().coerceIn(2, 6)
    }
    val pageSize = rows * columns
    val homePages = remember(homeApps, pageSize) { homeApps.chunked(pageSize) }
    val pageCount = homePages.size.coerceAtLeast(1) + 1 // +1 = Biblioteca de apps
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // ------------------------------------------------------------------ acciones

    val openApp: (AppInfo) -> Unit = { app ->
        if (launchApp(context, app.packageName)) {
            query = ""
            openFolder = null
        }
    }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            wiggle = false
            query = ""
            openFolder = null
            controlCenterOpen = false
            settingsOpen = false
            backgroundMenuOpen = false
            menuApp = null
            removeApp = null
            renameApp = null
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler {
        when {
            menuApp != null -> menuApp = null
            removeApp != null -> removeApp = null
            renameApp != null -> renameApp = null
            backgroundMenuOpen -> backgroundMenuOpen = false
            settingsOpen -> settingsOpen = false
            controlCenterOpen -> controlCenterOpen = false
            openFolder != null -> openFolder = null
            wiggle -> wiggle = false
            query.isNotEmpty() -> query = ""
            pagerState.currentPage != 0 -> scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    // El blur se aplica a la capa de ATRÁS (fondo + inicio). Antes se aplicaba al propio overlay,
    // que se desenfocaba a sí mismo y por eso las carpetas "no se veían".
    val overlayActive = openFolder != null || controlCenterOpen || backgroundMenuOpen ||
        menuApp != null || removeApp != null
    val blurRadius by animateDpAsState(if (overlayActive) 28.dp else 0.dp, label = "blur")

    Box(Modifier.fillMaxSize()) {

        // ============================================================== capa base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
        ) {
            WallpaperLayer(settings, customWallpaper)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (wiggle) wiggle = false },
                            onLongPress = { if (!wiggle) backgroundMenuOpen = true }
                        )
                    }
            ) {
                TopBar(
                    es = es,
                    showIsland = settings.showIsland,
                    wiggle = wiggle,
                    battery = battery,
                    onDone = { wiggle = false },
                    onOpenControlCenter = { controlCenterOpen = true }
                )

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeWidgets(es = es, battery = battery, now = now)
                    SpotlightSearch(
                        query = query,
                        onQueryChange = { query = it },
                        onVoiceClick = { openVoiceAssistant(context) },
                        onSearch = { searchResults.firstOrNull()?.let(openApp) },
                        es = es
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { gridAreaHeightPx = it.height }
                ) {
                    if (searching) {
                        SearchResultsGrid(
                            results = searchResults,
                            columns = columns,
                            es = es,
                            iconSize = iconSize,
                            iconTheme = settings.iconTheme,
                            onLaunch = openApp,
                            onLongClick = { menuApp = it }
                        )
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            if (page == pageCount - 1) {
                                AppLibraryPage(
                                    apps = apps,
                                    columns = if (isTablet) 3 else 2,
                                    es = es,
                                    onFolderClick = { openFolder = it }
                                )
                            } else {
                                HomeGridPage(
                                    pageApps = homePages.getOrElse(page) { emptyList() },
                                    columns = columns,
                                    isTablet = isTablet,
                                    iconSize = iconSize,
                                    iconTheme = settings.iconTheme,
                                    showLabels = settings.showLabels,
                                    wiggle = wiggle,
                                    onLaunch = openApp,
                                    onLongClick = { menuApp = it },
                                    onRemoveClick = { removeApp = it }
                                )
                            }
                        }
                    }
                }

                if (!searching) {
                    PageIndicatorDots(pageCount = pageCount, currentPage = pagerState.currentPage)
                    Spacer(Modifier.height(6.dp))
                    if (dockApps.isNotEmpty()) {
                        Dock(
                            apps = dockApps,
                            iconTheme = settings.iconTheme,
                            wiggle = wiggle,
                            isTablet = isTablet,
                            onLaunch = openApp,
                            onLongClick = { menuApp = it },
                            onRemoveClick = { removeApp = it }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        // ============================================================== overlays (sin blur propio)

        if (openFolder != null) {
            FolderOverlay(
                title = categoryName(openFolder!!, es),
                apps = folderApps,
                iconTheme = settings.iconTheme,
                isTablet = isTablet,
                wiggle = wiggle,
                onLaunch = openApp,
                onLongClick = { menuApp = it },
                onRemoveClick = { removeApp = it },
                onDismiss = { openFolder = null }
            )
        }

        AnimatedVisibility(
            visible = controlCenterOpen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            ControlCenterOverlay(
                es = es,
                onOpenLauncherSettings = {
                    controlCenterOpen = false
                    settingsOpen = true
                },
                onDismiss = { controlCenterOpen = false }
            )
        }

        // menú de long-press sobre una app
        menuApp?.let { app ->
            val inDock = app.packageName in dockPackages
            val isHidden = app.packageName in hidden
            val items = buildList {
                add(MenuItem(tr(es, "Editar pantalla de inicio", "Edit Home Screen")) {
                    wiggle = true
                    menuApp = null
                })
                add(MenuItem(tr(es, "Información de la app", "App Info")) {
                    openAppInfo(context, app.packageName)
                    menuApp = null
                })
                add(MenuItem(tr(es, "Renombrar", "Rename")) {
                    renameText = app.label
                    renameApp = app
                    menuApp = null
                })
                if (inDock) {
                    add(MenuItem(tr(es, "Quitar del dock", "Remove from Dock")) {
                        settings.updateDock(dockPackages - app.packageName)
                        menuApp = null
                    })
                } else if (dockPackages.size < dockSize) {
                    add(MenuItem(tr(es, "Añadir al dock", "Add to Dock")) {
                        settings.unhide(app.packageName)
                        settings.updateDock(dockPackages + app.packageName)
                        menuApp = null
                    })
                }
                if (isHidden) {
                    add(MenuItem(tr(es, "Mostrar en pantalla de inicio", "Show on Home Screen")) {
                        settings.unhide(app.packageName)
                        menuApp = null
                    })
                } else if (!inDock) {
                    add(MenuItem(tr(es, "Quitar de la pantalla de inicio", "Remove from Home Screen")) {
                        settings.hide(app.packageName)
                        menuApp = null
                    })
                }
                add(MenuItem(tr(es, "Desinstalar", "Uninstall"), destructive = true) {
                    uninstallApp(context, app.packageName)
                    menuApp = null
                })
            }
            GlassMenu(title = app.label, items = items, onDismiss = { menuApp = null })
        }

        // botón "−" en modo edición
        removeApp?.let { app ->
            val inDock = app.packageName in dockPackages
            GlassMenu(
                title = app.label,
                items = listOf(
                    MenuItem(
                        if (inDock) tr(es, "Quitar del dock", "Remove from Dock")
                        else tr(es, "Quitar de la pantalla de inicio", "Remove from Home Screen")
                    ) {
                        if (inDock) settings.updateDock(dockPackages - app.packageName)
                        else settings.hide(app.packageName)
                        removeApp = null
                    },
                    MenuItem(tr(es, "Desinstalar", "Uninstall"), destructive = true) {
                        uninstallApp(context, app.packageName)
                        removeApp = null
                    },
                    MenuItem(tr(es, "Cancelar", "Cancel")) { removeApp = null }
                ),
                onDismiss = { removeApp = null }
            )
        }

        // long-press sobre el fondo
        if (backgroundMenuOpen) {
            GlassMenu(
                title = null,
                items = listOf(
                    MenuItem(tr(es, "Editar pantalla de inicio", "Edit Home Screen")) {
                        wiggle = true
                        backgroundMenuOpen = false
                    },
                    MenuItem(tr(es, "Fondo de pantalla y ajustes", "Wallpaper & Settings")) {
                        backgroundMenuOpen = false
                        settingsOpen = true
                    },
                    MenuItem(tr(es, "Cancelar", "Cancel")) { backgroundMenuOpen = false }
                ),
                onDismiss = { backgroundMenuOpen = false }
            )
        }

        if (settingsOpen) {
            SettingsScreen(
                settings = settings,
                allApps = apps,
                onPickPhoto = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClose = { settingsOpen = false }
            )
        }
    }

    // ------------------------------------------------------------------ renombrar

    renameApp?.let { app ->
        AlertDialog(
            onDismissRequest = { renameApp = null },
            title = { Text(tr(es, "Renombrar ${app.label}", "Rename ${app.label}")) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(tr(es, "Nombre de la app", "App name")) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) settings.rename(app.packageName, renameText.trim())
                    renameApp = null
                }) { Text(tr(es, "Guardar", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { renameApp = null }) { Text(tr(es, "Cancelar", "Cancel")) }
            }
        )
    }
}

// ====================================================================== páginas

@Composable
private fun HomeGridPage(
    pageApps: List<AppInfo>,
    columns: Int,
    isTablet: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    iconTheme: com.salvador.salvitolauncher.data.AppIconTheme,
    showLabels: Boolean,
    wiggle: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    onRemoveClick: (AppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 32.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(pageApps, key = { _, app -> app.packageName }) { _, app ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                AppIcon(
                    app = app,
                    iconTheme = iconTheme,
                    showLabel = showLabels,
                    wiggle = wiggle,
                    iconSize = iconSize,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongClick(app) },
                    onRemoveClick = { onRemoveClick(app) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsGrid(
    results: List<AppInfo>,
    columns: Int,
    es: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    iconTheme: com.salvador.salvitolauncher.data.AppIconTheme,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit
) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(
                text = tr(es, "Sin resultados", "No results"),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(results, key = { _, app -> app.packageName }) { _, app ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                AppIcon(
                    app = app,
                    iconTheme = iconTheme,
                    showLabel = true,
                    wiggle = false,
                    iconSize = iconSize,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongClick(app) },
                    onRemoveClick = {}
                )
            }
        }
    }
}
