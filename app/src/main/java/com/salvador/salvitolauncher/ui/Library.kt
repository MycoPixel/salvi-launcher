package com.salvador.salvitolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salvador.salvitolauncher.data.AppCategory
import com.salvador.salvitolauncher.data.AppIconTheme
import com.salvador.salvitolauncher.data.AppInfo

/** Última página: las apps agrupadas por categoría, como la Biblioteca de apps de iOS. */
@Composable
fun AppLibraryPage(
    apps: List<AppInfo>,
    columns: Int,
    es: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onFolderClick: (AppCategory) -> Unit
) {
    val groups = remember(apps) {
        AppCategory.entries
            .map { category -> category to apps.filter { it.category == category } }
            .filter { it.second.isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = tr(es, "Biblioteca de apps", "App Library"),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups, key = { it.first.name }) { (category, categoryApps) ->
                LibraryFolderCard(
                    name = categoryName(category, es),
                    apps = categoryApps,
                    onLaunchApp = onLaunchApp,
                    onClick = { onFolderClick(category) }
                )
            }
        }
    }
}

@Composable
private fun LibraryFolderCard(
    name: String,
    apps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            apps.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { app ->
                        MiniIcon(app = app, onClick = { onLaunchApp(app) })
                    }
                }
            }
        }
        Text(
            text = name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniIcon(app: AppInfo, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(percent = 22))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
    ) {
        val bitmap = app.icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Carpeta abierta. Ya NO se desenfoca a sí misma: la pantalla de inicio de atrás es la que se
 * desenfoca (ver LauncherHome), y esto solo dibuja el velo oscuro y la tarjeta con las apps.
 */
@Composable
fun FolderOverlay(
    title: String,
    apps: List<AppInfo>,
    iconTheme: AppIconTheme,
    isTablet: Boolean,
    wiggle: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    onRemoveClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val columns = if (isTablet) 4 else 3
    val rows = (apps.size + columns - 1) / columns
    val gridHeight: Dp = (104.dp * rows).coerceAtMost(420.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = if (isTablet) 520.dp else 360.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF3A3A3C).copy(alpha = 0.60f))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        AppIcon(
                            app = app,
                            iconTheme = iconTheme,
                            showLabel = true,
                            wiggle = wiggle,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongClick(app) },
                            onRemoveClick = { onRemoveClick(app) }
                        )
                    }
                }
            }
        }
    }
}
