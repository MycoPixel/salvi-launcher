package com.salvador.salvitolauncher.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.salvador.salvitolauncher.data.LauncherSettings
import com.salvador.salvitolauncher.system.WidgetEnvironment
import com.salvador.salvitolauncher.system.installedWidgetProviders

/**
 * Página a la izquierda del inicio con widgets reales de otras apps (reloj, clima, calendario…).
 * Se agregan con "+" y se quitan en modo edición con el botón "−".
 */
@Composable
fun WidgetsPage(
    settings: LauncherSettings,
    env: WidgetEnvironment,
    es: Boolean,
    wiggle: Boolean
) {
    var showPicker by remember { mutableStateOf(false) }
    var pendingBind by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }

    // Quita de la lista los widgets que ya no existen (p. ej. la app se desinstaló).
    LaunchedEffect(settings.widgetIds) {
        settings.widgetIds.forEach { id ->
            if (env.manager.getAppWidgetInfo(id) == null) {
                env.host.deleteAppWidgetId(id)
                settings.removeWidget(id)
            }
        }
    }

    // Último paso: si el widget tiene pantalla de configuración se abre; si no, se agrega directo.
    fun finishAdd(id: Int, info: AppWidgetProviderInfo) {
        if (info.configure != null) {
            env.startConfigure(id) { accepted ->
                if (accepted) settings.addWidget(id) else env.host.deleteAppWidgetId(id)
            }
        } else {
            settings.addWidget(id)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pending = pendingBind
        pendingBind = null
        if (pending != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                finishAdd(pending.first, pending.second)
            } else {
                env.host.deleteAppWidgetId(pending.first)
            }
        }
    }

    fun beginAdd(info: AppWidgetProviderInfo) {
        val id = env.host.allocateAppWidgetId()
        val allowed = try {
            env.manager.bindAppWidgetIdIfAllowed(id, info.provider)
        } catch (_: Exception) {
            false
        }
        if (allowed) {
            finishAdd(id, info)
        } else {
            // Primera vez: Android pide al usuario permiso para que el launcher use ese widget.
            pendingBind = id to info
            try {
                bindLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                )
            } catch (_: Exception) {
                pendingBind = null
                env.host.deleteAppWidgetId(id)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Widgets",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable { showPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (settings.widgetIds.isEmpty()) {
                Text(
                    text = tr(
                        es,
                        "Toca + para agregar widgets de tus apps.",
                        "Tap + to add widgets from your apps."
                    ),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            settings.widgetIds.forEach { id ->
                key(id) {
                    WidgetItem(
                        env = env,
                        appWidgetId = id,
                        wiggle = wiggle,
                        onRemove = {
                            env.host.deleteAppWidgetId(id)
                            settings.removeWidget(id)
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPicker) {
        WidgetPickerDialog(
            env = env,
            es = es,
            onPick = { info ->
                showPicker = false
                beginAdd(info)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun WidgetItem(
    env: WidgetEnvironment,
    appWidgetId: Int,
    wiggle: Boolean,
    onRemove: () -> Unit
) {
    val info = remember(appWidgetId) { env.manager.getAppWidgetInfo(appWidgetId) } ?: return
    val density = LocalDensity.current
    val minWidthDp = with(density) { info.minWidth.toDp().value.toInt() }
    val minHeightDp = with(density) { info.minHeight.toDp().value.toInt() }
    val height = with(density) { info.minHeight.toDp() }.coerceIn(96.dp, 360.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(22.dp))
    ) {
        AndroidView(
            factory = { context -> env.host.createView(context, appWidgetId, info) },
            update = { view ->
                view.updateAppWidgetSize(null, minWidthDp, minHeightDp, minWidthDp, minHeightDp)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (wiggle) {
            // capa transparente: en modo edición el widget no recibe toques
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { } }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8E8E93))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "−", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WidgetPickerDialog(
    env: WidgetEnvironment,
    es: Boolean,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val providers = remember {
        val pm = context.packageManager
        installedWidgetProviders(env.manager).sortedBy { (it.loadLabel(pm) ?: "").lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(es, "Agregar widget", "Add widget")) },
        text = {
            if (providers.isEmpty()) {
                Text(tr(es, "No hay widgets disponibles.", "No widgets available."))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(providers) { info ->
                        WidgetProviderRow(info = info, onClick = { onPick(info) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr(es, "Cancelar", "Cancel")) }
        }
    )
}

@Composable
private fun WidgetProviderRow(info: AppWidgetProviderInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val label = remember(info) { info.loadLabel(context.packageManager) ?: "" }
    // el icono se decodifica solo cuando la fila se ve (la lista es perezosa)
    val icon = remember(info) {
        try {
            info.loadIcon(context, context.resources.displayMetrics.densityDpi)
                ?.toBitmap(96, 96)
                ?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
