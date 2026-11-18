package com.salvador.salvitolauncher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salvador.salvitolauncher.data.AppIconTheme
import com.salvador.salvitolauncher.data.AppInfo
import kotlin.math.abs
import kotlin.math.floor

/** Alto de cada fila del inicio: icono, nombre y el espacio hasta la fila siguiente. */
val HomeCellHeight: Dp = 104.dp

/** Margen horizontal de la grilla. */
fun homeGridPadding(isTablet: Boolean): Dp = if (isTablet) 32.dp else 12.dp

/** Lo que se dibuja en una celda del inicio. La clave coincide con la de [com.salvador.salvitolauncher.data.LayoutEntry]. */
sealed interface HomeCell {
    val key: String
}

class AppCell(val app: AppInfo) : HomeCell {
    override val key: String = "app:${app.packageName}"
}

class FolderCell(val id: String, val name: String, val apps: List<AppInfo>) : HomeCell {
    override val key: String = "folder:$id"
}

/** Resultado de ubicar el dedo sobre la grilla: qué celda es y si se suelta "encima" para fusionar. */
class DropTarget(val indexOnPage: Int, val merge: Boolean)

/**
 * Traduce la posición del dedo (relativa al área del pager) a una celda de la página.
 * Si el dedo cae cerca del centro de un icono ocupado se considera "fusionar" (crear/entrar a
 * una carpeta); en cualquier otro punto de la celda se considera "reordenar".
 * Un índice igual a [itemCount] significa "al final de la página".
 */
fun computeDropTarget(
    pointer: Offset,
    areaWidth: Float,
    columns: Int,
    horizontalPadding: Float,
    cellHeight: Float,
    iconSize: Float,
    itemCount: Int
): DropTarget {
    val cellWidth = (areaWidth - 2f * horizontalPadding) / columns
    if (cellWidth <= 0f || cellHeight <= 0f) return DropTarget(itemCount, false)

    val col = floor((pointer.x - horizontalPadding) / cellWidth).toInt().coerceIn(0, columns - 1)
    val row = floor(pointer.y / cellHeight).toInt().coerceAtLeast(0)
    val index = (row * columns + col).coerceAtMost(itemCount)

    val merge = if (index < itemCount) {
        val centerX = horizontalPadding + (col + 0.5f) * cellWidth
        val centerY = row * cellHeight + iconSize / 2f
        abs(pointer.x - centerX) < iconSize * 0.4f && abs(pointer.y - centerY) < iconSize * 0.4f
    } else {
        false
    }
    return DropTarget(index, merge)
}

/**
 * Una página del inicio. Las celdas tienen tamaño fijo (columnas iguales y [HomeCellHeight] de alto),
 * así que la posición de cada una es calculable y se puede resolver dónde cae un arrastre.
 *
 * El arrastre solo está activo en modo edición. Esta función solo avisa (inicio, movimiento, fin);
 * quien decide qué hacer al soltar es [LauncherHome].
 */
@Composable
fun HomeGridPage(
    cells: List<HomeCell>,
    columns: Int,
    isTablet: Boolean,
    iconSize: Dp,
    iconTheme: AppIconTheme,
    showLabels: Boolean,
    wiggle: Boolean,
    badgeCounts: Map<String, Int>,
    draggingKey: String?,
    mergeTargetKey: String?,
    onOpenApp: (AppInfo) -> Unit,
    onOpenFolder: (FolderCell) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFolderLongClick: (FolderCell) -> Unit,
    onAppRemove: (AppInfo) -> Unit,
    onFolderRemove: (FolderCell) -> Unit,
    onDragStart: (cell: HomeCell, indexOnPage: Int, pointerInCell: Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val rows = (cells.size + columns - 1) / columns
    val currentSwipeDown by rememberUpdatedState(onSwipeDown)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = homeGridPadding(isTablet))
            // Deslizar hacia abajo sobre el inicio abre la búsqueda (solo fuera del modo edición).
            .pointerInput(wiggle) {
                if (!wiggle) {
                    val threshold = 72.dp.toPx()
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > threshold) currentSwipeDown() },
                        onDragCancel = { total = 0f },
                        onVerticalDrag = { _, amount -> total += amount }
                    )
                }
            }
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeCellHeight)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    val cell = cells.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (cell != null) {
                            HomeCellView(
                                cell = cell,
                                index = index,
                                iconSize = iconSize,
                                iconTheme = iconTheme,
                                showLabels = showLabels,
                                wiggle = wiggle,
                                badgeCounts = badgeCounts,
                                isDragged = draggingKey == cell.key,
                                isMergeTarget = mergeTargetKey == cell.key,
                                onOpenApp = onOpenApp,
                                onOpenFolder = onOpenFolder,
                                onAppLongClick = onAppLongClick,
                                onFolderLongClick = onFolderLongClick,
                                onAppRemove = onAppRemove,
                                onFolderRemove = onFolderRemove,
                                onDragStart = onDragStart,
                                onDragMove = onDragMove,
                                onDragEnd = onDragEnd
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCellView(
    cell: HomeCell,
    index: Int,
    iconSize: Dp,
    iconTheme: AppIconTheme,
    showLabels: Boolean,
    wiggle: Boolean,
    badgeCounts: Map<String, Int>,
    isDragged: Boolean,
    isMergeTarget: Boolean,
    onOpenApp: (AppInfo) -> Unit,
    onOpenFolder: (FolderCell) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFolderLongClick: (FolderCell) -> Unit,
    onAppRemove: (AppInfo) -> Unit,
    onFolderRemove: (FolderCell) -> Unit,
    onDragStart: (HomeCell, Int, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    // El gesto se crea una sola vez; con rememberUpdatedState siempre llama a la versión más nueva.
    val currentStart by rememberUpdatedState(onDragStart)
    val currentMove by rememberUpdatedState(onDragMove)
    val currentEnd by rememberUpdatedState(onDragEnd)

    val dragModifier = Modifier.dragToReorder(
        enabled = wiggle,
        key = cell.key to index,
        onStart = { pointerInCell -> currentStart(cell, index, pointerInCell) },
        onMove = { delta -> currentMove(delta) },
        onEnd = { currentEnd() }
    )

    Box(
        modifier = dragModifier.graphicsLayer {
            // la celda de origen se oculta mientras el icono "flota" bajo el dedo
            alpha = if (isDragged) 0f else 1f
            val scale = if (isMergeTarget) 1.15f else 1f
            scaleX = scale
            scaleY = scale
        }
    ) {
        when (cell) {
            is AppCell -> AppIcon(
                app = cell.app,
                iconTheme = iconTheme,
                showLabel = showLabels,
                wiggle = wiggle,
                iconSize = iconSize,
                badge = badgeCounts[cell.app.packageName] ?: 0,
                onClick = { onOpenApp(cell.app) },
                onLongClick = { onAppLongClick(cell.app) },
                onRemoveClick = { onAppRemove(cell.app) }
            )

            is FolderCell -> FolderTile(
                name = cell.name,
                apps = cell.apps,
                iconTheme = iconTheme,
                showLabel = showLabels,
                wiggle = wiggle,
                iconSize = iconSize,
                badge = cell.apps.sumOf { badgeCounts[it.packageName] ?: 0 },
                onClick = { onOpenFolder(cell) },
                onLongClick = { onFolderLongClick(cell) },
                onRemoveClick = { onFolderRemove(cell) }
            )
        }
    }
}

/**
 * Arrastre inmediato (sin mantener presionado) cuando [enabled]. [onStart] recibe la posición del
 * dedo relativa al centro horizontal y al borde superior del contenido de la celda.
 */
private fun Modifier.dragToReorder(
    enabled: Boolean,
    key: Any,
    onStart: (pointerInCell: Offset) -> Unit,
    onMove: (delta: Offset) -> Unit,
    onEnd: () -> Unit
): Modifier {
    if (!enabled) return this
    return this.pointerInput(key) {
        detectDragGestures(
            onDragStart = { offset -> onStart(Offset(offset.x - size.width / 2f, offset.y)) },
            onDragEnd = { onEnd() },
            onDragCancel = { onEnd() },
            onDrag = { change, amount ->
                change.consume()
                onMove(amount)
            }
        )
    }
}
