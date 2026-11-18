package com.salvador.salvitolauncher.system

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo

/** Identificador del "host" de widgets de este launcher (cualquier número fijo sirve). */
const val WIDGET_HOST_ID = 1024

/**
 * Lo que la UI necesita para alojar widgets de otras apps. Lo crea la Activity porque la
 * pantalla de configuración de un widget devuelve su resultado a la Activity.
 *
 * @param startConfigure abre la pantalla de configuración del widget y avisa si el usuario aceptó.
 */
class WidgetEnvironment(
    val host: AppWidgetHost,
    val manager: AppWidgetManager,
    val startConfigure: (appWidgetId: Int, onResult: (Boolean) -> Unit) -> Unit
)

/** Widgets instalados que se pueden poner en una pantalla de inicio. */
fun installedWidgetProviders(manager: AppWidgetManager): List<AppWidgetProviderInfo> =
    manager.installedProviders.filter {
        (it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0
    }
