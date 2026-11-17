package com.salvador.salvitolauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.salvador.salvitolauncher.data.LauncherSettings
import com.salvador.salvitolauncher.ui.LauncherHome
import com.salvador.salvitolauncher.ui.theme.SalvitoLauncherTheme

class MainActivity : ComponentActivity() {

    /** Sube cada vez que se pulsa Home con el launcher ya abierto (singleTask => onNewIntent). */
    private var homeSignal by mutableIntStateOf(0)

    private lateinit var settings: LauncherSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Iconos claros en la barra de estado/navegación: los fondos del launcher son oscuros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        settings = LauncherSettings(this)

        setContent {
            SalvitoLauncherTheme {
                LauncherHome(settings = settings, homeSignal = homeSignal)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) homeSignal++
    }
}
