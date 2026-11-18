package com.salvador.salvitolauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.salvador.salvitolauncher.data.LauncherSettings
import com.salvador.salvitolauncher.system.WIDGET_HOST_ID
import com.salvador.salvitolauncher.system.WidgetEnvironment
import com.salvador.salvitolauncher.ui.LauncherHome
import com.salvador.salvitolauncher.ui.theme.SalvitoLauncherTheme

class MainActivity : FragmentActivity() {

    /** Sube cada vez que se pulsa Home con el launcher ya abierto (singleTask => onNewIntent). */
    private var homeSignal by mutableIntStateOf(0)

    private lateinit var settings: LauncherSettings
    private lateinit var widgetEnvironment: WidgetEnvironment

    /** Quién espera el resultado de la pantalla de configuración de un widget. */
    private var configureCallback: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Iconos claros en la barra de estado/navegación: los fondos del launcher son oscuros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        settings = LauncherSettings(this)

        val host = AppWidgetHost(applicationContext, WIDGET_HOST_ID)
        widgetEnvironment = WidgetEnvironment(
            host = host,
            manager = AppWidgetManager.getInstance(this),
            startConfigure = { appWidgetId, onResult -> startWidgetConfigure(host, appWidgetId, onResult) }
        )

        setContent {
            SalvitoLauncherTheme {
                LauncherHome(settings = settings, homeSignal = homeSignal, widgets = widgetEnvironment)
            }
        }
    }

    // El host debe "escuchar" mientras el launcher está visible para que los widgets se actualicen.
    override fun onStart() {
        super.onStart()
        try {
            widgetEnvironment.host.startListening()
        } catch (_: Exception) {
            // algunos dispositivos fallan si no hay proveedores; los widgets simplemente no se actualizan
        }
    }

    override fun onStop() {
        try {
            widgetEnvironment.host.stopListening()
        } catch (_: Exception) {
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) homeSignal++
    }

    private fun startWidgetConfigure(host: AppWidgetHost, appWidgetId: Int, onResult: (Boolean) -> Unit) {
        configureCallback = onResult
        try {
            host.startAppWidgetConfigureActivityForResult(
                this,
                appWidgetId,
                0,
                REQUEST_CONFIGURE_WIDGET,
                null
            )
        } catch (_: Exception) {
            configureCallback = null
            onResult(false)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            val callback = configureCallback
            configureCallback = null
            callback?.invoke(resultCode == RESULT_OK)
        }
    }

    private companion object {
        const val REQUEST_CONFIGURE_WIDGET = 4242
    }
}
