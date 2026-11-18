package com.salvador.salvitolauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.salvador.salvitolauncher.data.AppCategory
import com.salvador.salvitolauncher.data.AppEntry
import com.salvador.salvitolauncher.data.AppIconTheme
import com.salvador.salvitolauncher.data.AppInfo
import com.salvador.salvitolauncher.data.AppRepository
import com.salvador.salvitolauncher.data.FolderEntry
import com.salvador.salvitolauncher.data.HomeLayoutOps
import com.salvador.salvitolauncher.data.LauncherSettings
import com.salvador.salvitolauncher.data.WallpaperStorage
import com.salvador.salvitolauncher.system.AppShortcuts
import com.salvador.salvitolauncher.system.BadgeState
import com.salvador.salvitolauncher.system.BiometricHelper
import com.salvador.salvitolauncher.system.LockAccessibilityService
import com.salvador.salvitolauncher.system.WidgetEnvironment
import com.salvador.salvitolauncher.system.launchApp
import com.salvador.salvitolauncher.system.openAppInfo
import com.salvador.salvitolauncher.system.openNotificationAccessSettings
import com.salvador.salvitolauncher.system.openVoiceAssistant
import com.salvador.salvitolauncher.system.rememberBatteryState
import com.salvador.salvitolauncher.system.uninstallApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pantalla principal del launcher.
 *
 * Páginas del pager: 0 = widgets, 1..n = inicio, última = Biblioteca de apps.
 *
 * @param homeSignal sube cada vez que el usuario pulsa Home estando ya en el launcher
 *                   (vuelve a la primera página del inicio y cierra overlays, como iOS).
 */
@Composable
fun LauncherHome(settings: LauncherSettings, homeSignal: Int, widgets: WidgetEnvironment) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    val es = settings.isSpanish

    val isTablet = with(density) { windowInfo.containerSize.width.toDp() } >= 600.dp
    val columns = if (isTablet) 6 else 4
    val dockSize = if (isTablet) 6 else 4
    val baseIconSize = if (isTablet) 68.dp else 60.dp
    val iconSize = if (settings.useLargeIcons) baseIconSize + 10.dp else baseIconSize
    val showLabels = if (settings.useLargeIcons) false else settings.showLabels

    val battery by rememberBatteryState()
    val now by rememberNow()
    val badgeCounts: Map<String, Int> = if (settings.showBadges) BadgeState.counts else emptyMap()

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

    LaunchedEffect(packageTick, settings.iconPackPackage) {
        val loaded = withContext(Dispatchers.IO) { AppRepository.loadApps(context, settings.iconPackPackage) }
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
    // "cat:<CATEGORIA>" = carpeta de la Biblioteca, "user:<id>" = carpeta creada por el usuario
    var openFolderKey by remember { mutableStateOf<String?>(null) }
    var controlCenterOpen by remember { mutableStateOf(false) }
    var notificationCenterOpen by remember { mutableStateOf(false) }
    var standByOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var backgroundMenuOpen by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var removeApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    var menuFolder by remember { mutableStateOf<FolderCell?>(null) }
    var renameFolder by remember { mutableStateOf<FolderCell?>(null) }
    var renameFolderText by remember { mutableStateOf("") }

    // arrastre de iconos (modo edición)
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) } // relativo al área del pager
    var mergeTargetKey by remember { mutableStateOf<String?>(null) }
    var areaWidthPx by remember { mutableIntStateOf(0) }

    // ------------------------------------------------------------------ listas derivadas

    val labels = settings.customLabels
    val hidden = settings.hiddenPackages
    val dockPackages = settings.dockPackages ?: emptyList()

    val apps = remember(allApps, labels) {
        allApps
            .map { app -> labels[app.packageName]?.let { app.copy(label = it) } ?: app }
            .sortedBy { it.label.lowercase() }
    }
    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val dockApps = remember(apps, dockPackages, hidden, dockSize) {
        dockPackages
            .filter { it !in hidden }
            .mapNotNull { byPackage[it] }
            .take(dockSize)
    }
    val homeApps = remember(apps, hidden, dockApps) {
        val inDock = dockApps.map { it.packageName }.toSet()
        apps.filter { it.packageName !in hidden && it.packageName !in inDock }
    }

    // Orden del inicio: lo guardado + las apps nuevas al final. Se persiste solo cuando el usuario cambia algo.
    val layout = remember(settings.homeLayout, homeApps) {
        HomeLayoutOps.reconcile(settings.homeLayout, homeApps)
    }
    val cells = remember(layout, byPackage) {
        layout.mapNotNull { entry ->
            when (entry) {
                is AppEntry -> byPackage[entry.packageName]?.let { AppCell(it) }
                is FolderEntry -> FolderCell(entry.id, entry.name, entry.packages.mapNotNull { byPackage[it] })
            }
        }
    }

    val searching = query.isNotBlank()
    val searchResults = remember(apps, query) {
        if (query.isBlank()) emptyList()
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    val openFolderInfo: Pair<String, List<AppInfo>>? = remember(openFolderKey, apps, layout, byPackage, es) {
        val key = openFolderKey
        when {
            key == null -> null
            key.startsWith("cat:") ->
                AppCategory.entries.firstOrNull { "cat:${it.name}" == key }
                    ?.let { category -> categoryName(category, es) to apps.filter { it.category == category } }
            key.startsWith("user:") ->
                layout.filterIsInstance<FolderEntry>().firstOrNull { "user:${it.id}" == key }
                    ?.let { folder -> folder.name to folder.packages.mapNotNull { byPackage[it] } }
            else -> null
        }
    }

    // ------------------------------------------------------------------ paginación adaptable

    // Cuántas filas caben realmente en el espacio disponible (antes eran 4 fijas y se cortaban).
    var gridAreaHeightPx by remember { mutableIntStateOf(0) }
    val rows = if (gridAreaHeightPx == 0) {
        4
    } else {
        ((with(density) { gridAreaHeightPx.toDp() } + 20.dp) / HomeCellHeight).toInt().coerceIn(2, 6)
    }
    val pageSize = rows * columns
    val cellPages = remember(cells, pageSize) { cells.chunked(pageSize) }
    val homePageCount = cellPages.size.coerceAtLeast(1)
    val pageCount = homePageCount + 2 // + página de widgets (izquierda) + Biblioteca (derecha)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { pageCount })

    // ------------------------------------------------------------------ arrastrar y soltar

    val hPadPx = with(density) { homeGridPadding(isTablet).toPx() }
    val cellHeightPx = with(density) { HomeCellHeight.toPx() }
    val iconPx = with(density) { iconSize.toPx() }
    val edgePx = with(density) { 36.dp.toPx() }
    val floatingWidthPx = with(density) { (iconSize + 16.dp).toPx() }

    fun startDrag(cell: HomeCell, indexOnPage: Int, pointerInCell: Offset) {
        val cellWidth = (areaWidthPx - 2f * hPadPx) / columns
        val row = indexOnPage / columns
        val col = indexOnPage % columns
        dragPointer = Offset(
            hPadPx + (col + 0.5f) * cellWidth + pointerInCell.x,
            row * cellHeightPx + pointerInCell.y
        )
        draggingKey = cell.key
        mergeTargetKey = null
    }

    fun currentDropTarget(): DropTarget? {
        val pageCells = cellPages.getOrNull(pagerState.currentPage - 1) ?: return null
        return computeDropTarget(
            pointer = dragPointer,
            areaWidth = areaWidthPx.toFloat(),
            columns = columns,
            horizontalPadding = hPadPx,
            cellHeight = cellHeightPx,
            iconSize = iconPx,
            itemCount = pageCells.size
        )
    }

    fun moveDrag(delta: Offset) {
        dragPointer += delta
        val key = draggingKey ?: return
        val pageCells = cellPages.getOrNull(pagerState.currentPage - 1)
        val target = currentDropTarget()
        mergeTargetKey = if (
            pageCells != null && target != null && target.merge &&
            key.startsWith("app:") && target.indexOnPage < pageCells.size
        ) {
            pageCells[target.indexOnPage].key.takeIf { it != key }
        } else {
            null
        }
    }

    fun endDrag() {
        val key = draggingKey
        val target = currentDropTarget()
        val settled = abs(pagerState.currentPageOffsetFraction) < 0.1f
        if (key != null && target != null && settled) {
            val mergeKey = mergeTargetKey
            val updated = if (target.merge && mergeKey != null && key.startsWith("app:")) {
                val targetApp = (cells.firstOrNull { it.key == mergeKey } as? AppCell)?.app
                HomeLayoutOps.mergeInto(
                    layout = layout,
                    draggedKey = key,
                    targetKey = mergeKey,
                    newFolderName = targetApp?.let { categoryName(it.category, es) } ?: tr(es, "Carpeta", "Folder"),
                    newFolderId = UUID.randomUUID().toString().take(8)
                )
            } else {
                HomeLayoutOps.move(layout, key, (pagerState.currentPage - 1) * pageSize + target.indexOnPage)
            }
            settings.updateHomeLayout(updated)
        }
        draggingKey = null
        mergeTargetKey = null
    }

    // Al mantener el dedo en el borde izquierdo/derecho mientras se arrastra, cambia de página.
    val edgeDirection by remember(edgePx) {
        derivedStateOf {
            when {
                draggingKey == null -> 0
                dragPointer.x < edgePx -> -1
                dragPointer.x > areaWidthPx - edgePx -> 1
                else -> 0
            }
        }
    }
    LaunchedEffect(edgeDirection, pagerState.currentPage) {
        if (edgeDirection != 0) {
            delay(700)
            val target = pagerState.currentPage + edgeDirection
            if (target in 1..homePageCount) pagerState.animateScrollToPage(target)
        }
    }

    // ------------------------------------------------------------------ acciones

    val openApp: (AppInfo) -> Unit = { app ->
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (app.packageName in settings.lockedPackages && activity != null) {
            BiometricHelper.authenticate(
                activity = activity,
                title = tr(es, "Abrir ${app.label}", "Open ${app.label}"),
                subtitle = tr(es, "Se requiere autenticación para abrir esta app", "Authentication required to open app"),
                onSuccess = {
                    if (launchApp(context, app.packageName)) {
                        query = ""
                        openFolderKey = null
                    }
                }
            )
        } else {
            if (launchApp(context, app.packageName)) {
                query = ""
                openFolderKey = null
            }
        }
    }

    val focusSearch: () -> Unit = {
        runCatching { searchFocus.requestFocus() }
        keyboard?.show()
    }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            wiggle = false
            query = ""
            openFolderKey = null
            controlCenterOpen = false
            notificationCenterOpen = false
            settingsOpen = false
            backgroundMenuOpen = false
            menuApp = null
            removeApp = null
            renameApp = null
            menuFolder = null
            renameFolder = null
            draggingKey = null
            mergeTargetKey = null
            pagerState.animateScrollToPage(1)
        }
    }

    BackHandler {
        when {
            menuApp != null -> menuApp = null
            removeApp != null -> removeApp = null
            renameApp != null -> renameApp = null
            menuFolder != null -> menuFolder = null
            renameFolder != null -> renameFolder = null
            backgroundMenuOpen -> backgroundMenuOpen = false
            settingsOpen -> settingsOpen = false
            controlCenterOpen -> controlCenterOpen = false
            notificationCenterOpen -> notificationCenterOpen = false
            standByOpen -> standByOpen = false
            openFolderKey != null -> openFolderKey = null
            wiggle -> wiggle = false
            query.isNotEmpty() -> query = ""
            pagerState.currentPage != 1 -> scope.launch { pagerState.animateScrollToPage(1) }
        }
    }

    val pageOffset by remember {
        derivedStateOf {
            (pagerState.currentPage - 1) + pagerState.currentPageOffsetFraction
        }
    }

    // El blur se aplica a la capa de ATRÁS (fondo + inicio). Antes se aplicaba al propio overlay,
    // que se desenfocaba a sí mismo y por eso las carpetas "no se veían".
    val overlayActive = openFolderInfo != null || controlCenterOpen || notificationCenterOpen || backgroundMenuOpen ||
        standByOpen || menuApp != null || removeApp != null || menuFolder != null
    val blurRadius by animateDpAsState(if (overlayActive) 28.dp else 0.dp, label = "blur")

    Box(Modifier.fillMaxSize()) {

        // ============================================================== capa base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
        ) {
            WallpaperLayer(settings, customWallpaper, pageOffset)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (wiggle) wiggle = false },
                            onDoubleTap = {
                                if (settings.doubleTapLock && !wiggle && !LockAccessibilityService.lockScreen()) {
                                    Toast.makeText(
                                        context,
                                        tr(
                                            settings.isSpanish,
                                            "Activa el servicio de bloqueo en Ajustes",
                                            "Turn on the lock service in Settings"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
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
                    onOpenControlCenter = { controlCenterOpen = true },
                    onOpenNotificationCenter = { notificationCenterOpen = true }
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
                        es = es,
                        focusRequester = searchFocus
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged {
                            gridAreaHeightPx = it.height
                            areaWidthPx = it.width
                        }
                ) {
                    if (searching) {
                        SearchResultsGrid(
                            query = query,
                            results = searchResults,
                            columns = columns,
                            es = es,
                            iconSize = iconSize,
                            iconTheme = settings.iconTheme,
                            badgeCounts = badgeCounts,
                            onLaunch = openApp,
                            onLongClick = { menuApp = it }
                        )
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = draggingKey == null,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when {
                                page == 0 -> WidgetsPage(
                                    settings = settings,
                                    env = widgets,
                                    es = es,
                                    wiggle = wiggle
                                )

                                page == pageCount - 1 -> AppLibraryPage(
                                    apps = apps,
                                    columns = if (isTablet) 3 else 2,
                                    es = es,
                                    onLaunchApp = openApp,
                                    onFolderClick = { openFolderKey = "cat:${it.name}" }
                                )

                                else -> HomeGridPage(
                                    cells = cellPages.getOrElse(page - 1) { emptyList() },
                                    columns = columns,
                                    isTablet = isTablet,
                                    iconSize = iconSize,
                                    iconTheme = settings.iconTheme,
                                    showLabels = showLabels,
                                    wiggle = wiggle,
                                    badgeCounts = badgeCounts,
                                    lockedPackages = settings.lockedPackages,
                                    customTintColor = settings.customTintColor,
                                    draggingKey = draggingKey,
                                    mergeTargetKey = mergeTargetKey,
                                    onOpenApp = openApp,
                                    onOpenFolder = { openFolderKey = "user:${it.id}" },
                                    onAppLongClick = { menuApp = it },
                                    onFolderLongClick = { menuFolder = it },
                                    onAppRemove = { removeApp = it },
                                    onFolderRemove = { menuFolder = it },
                                    onDragStart = { cell, index, pointer -> startDrag(cell, index, pointer) },
                                    onDragMove = { delta -> moveDrag(delta) },
                                    onDragEnd = { endDrag() },
                                    onSwipeDown = focusSearch
                                )
                            }
                        }

                        // Icono que "flota" bajo el dedo mientras se arrastra.
                        val dragged = draggingKey?.let { key -> cells.firstOrNull { it.key == key } }
                        if (dragged != null) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (dragPointer.x - floatingWidthPx / 2f).roundToInt(),
                                            (dragPointer.y - iconPx / 2f).roundToInt()
                                        )
                                    }
                                    .graphicsLayer {
                                        scaleX = 1.1f
                                        scaleY = 1.1f
                                        alpha = 0.92f
                                    }
                            ) {
                                when (dragged) {
                                    is AppCell -> AppIcon(
                                        app = dragged.app,
                                        iconTheme = settings.iconTheme,
                                        showLabel = settings.showLabels,
                                        wiggle = false,
                                        iconSize = iconSize,
                                        onClick = {},
                                        onLongClick = {},
                                        onRemoveClick = {}
                                    )

                                    is FolderCell -> FolderTile(
                                        name = dragged.name,
                                        apps = dragged.apps,
                                        iconTheme = settings.iconTheme,
                                        showLabel = settings.showLabels,
                                        wiggle = false,
                                        iconSize = iconSize,
                                        onClick = {},
                                        onLongClick = {},
                                        onRemoveClick = {}
                                    )
                                }
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
                            badgeCounts = badgeCounts,
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

        openFolderInfo?.let { (title, folderApps) ->
            FolderOverlay(
                title = title,
                apps = folderApps,
                iconTheme = settings.iconTheme,
                isTablet = isTablet,
                wiggle = wiggle,
                onLaunch = openApp,
                onLongClick = { menuApp = it },
                onRemoveClick = { removeApp = it },
                onDismiss = { openFolderKey = null }
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

        NotificationCenterOverlay(
            visible = notificationCenterOpen,
            es = es,
            onDismiss = { notificationCenterOpen = false },
            onOpenNotificationAccess = { openNotificationAccessSettings(context) }
        )

        // menú de long-press sobre una app
        menuApp?.let { app ->
            val inDock = app.packageName in dockPackages
            val isHidden = app.packageName in hidden
            val isLocked = app.packageName in settings.lockedPackages
            val inUserFolder = layout.any { it is FolderEntry && app.packageName in it.packages }
            val shortcuts = remember(app.packageName) { AppShortcuts.getShortcuts(context, app.packageName) }

            val items = buildList {
                shortcuts.forEach { shortcut ->
                    add(MenuItem(shortcut.label, icon = shortcut.icon) {
                        menuApp = null
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (isLocked && activity != null) {
                            BiometricHelper.authenticate(
                                activity,
                                tr(es, "Ejecutar acceso directo", "Run shortcut"),
                                onSuccess = { AppShortcuts.launchShortcut(context, shortcut) }
                            )
                        } else {
                            AppShortcuts.launchShortcut(context, shortcut)
                        }
                    })
                }
                if (isLocked) {
                    add(MenuItem(tr(es, "Desbloquear app", "Unlock App")) {
                        settings.unlockApp(app.packageName)
                        menuApp = null
                    })
                } else {
                    add(MenuItem(tr(es, "Bloquear con Huella/PIN", "Lock with Biometrics")) {
                        settings.lockApp(app.packageName)
                        menuApp = null
                    })
                }
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
                if (inUserFolder) {
                    add(MenuItem(tr(es, "Sacar de la carpeta", "Remove from Folder")) {
                        settings.updateHomeLayout(HomeLayoutOps.removeFromFolder(layout, app.packageName))
                        menuApp = null
                    })
                }
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

        // botón "−" sobre una app en modo edición
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

        // long-press (o "−") sobre una carpeta del usuario
        menuFolder?.let { folder ->
            GlassMenu(
                title = folder.name,
                items = listOf(
                    MenuItem(tr(es, "Renombrar carpeta", "Rename Folder")) {
                        renameFolderText = folder.name
                        renameFolder = folder
                        menuFolder = null
                    },
                    MenuItem(tr(es, "Editar pantalla de inicio", "Edit Home Screen")) {
                        wiggle = true
                        menuFolder = null
                    },
                    MenuItem(tr(es, "Disolver carpeta", "Dissolve Folder"), destructive = true) {
                        settings.updateHomeLayout(HomeLayoutOps.dissolve(layout, folder.id))
                        menuFolder = null
                    },
                    MenuItem(tr(es, "Cancelar", "Cancel")) { menuFolder = null }
                ),
                onDismiss = { menuFolder = null }
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
                    MenuItem(tr(es, "Modo StandBy (iOS 17)", "StandBy Mode (iOS 17)")) {
                        backgroundMenuOpen = false
                        standByOpen = true
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

        StandByModeOverlay(
            visible = standByOpen,
            es = es,
            battery = battery,
            now = now,
            onDismiss = { standByOpen = false }
        )

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

    renameFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { renameFolder = null },
            title = { Text(tr(es, "Renombrar carpeta", "Rename folder")) },
            text = {
                OutlinedTextField(
                    value = renameFolderText,
                    onValueChange = { renameFolderText = it },
                    singleLine = true,
                    label = { Text(tr(es, "Nombre de la carpeta", "Folder name")) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameFolderText.isNotBlank()) {
                        settings.updateHomeLayout(
                            HomeLayoutOps.renameFolder(layout, folder.id, renameFolderText.trim())
                        )
                    }
                    renameFolder = null
                }) { Text(tr(es, "Guardar", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { renameFolder = null }) { Text(tr(es, "Cancelar", "Cancel")) }
            }
        )
    }
}

// ====================================================================== búsqueda

@Composable
private fun SearchResultsGrid(
    query: String,
    results: List<AppInfo>,
    columns: Int,
    es: Boolean,
    iconSize: Dp,
    iconTheme: AppIconTheme,
    badgeCounts: Map<String, Int>,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val mathResult = remember(query) { evaluateMathExpression(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (mathResult != null) {
            val formattedResult = if (mathResult % 1.0 == 0.0) mathResult.toLong().toString() else mathResult.toString()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(Color(0xFF2C2C2E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Text(
                    text = tr(es, "Calculadora", "Calculator"),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "= $formattedResult",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (results.isEmpty() && mathResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr(es, "Sin resultados para '$query'", "No results for '$query'"),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = {
                        val webIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}".toUri())
                        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(webIntent) }
                    }) {
                        Text(
                            text = tr(es, "🔍 Buscar en Google", "🔍 Search on Google"),
                            color = Color(0xFF0A84FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (results.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.weight(1f),
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
                            badge = badgeCounts[app.packageName] ?: 0,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongClick(app) },
                            onRemoveClick = {}
                        )
                    }
                }
            }
        }
    }
}
