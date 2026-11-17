package com.salvador.salvitolauncher.system

import android.app.AlarmManager
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.util.Locale
import kotlin.math.roundToInt

// ---------------------------------------------------------------- abrir cosas

fun safeStart(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        true
    } catch (_: Exception) {
        false
    }
}

fun launchApp(context: Context, packageName: String): Boolean {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
    return safeStart(context, intent)
}

fun openAppInfo(context: Context, packageName: String): Boolean =
    safeStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))

/** Requiere el permiso REQUEST_DELETE_PACKAGES (declarado en el manifest). */
fun uninstallApp(context: Context, packageName: String): Boolean =
    safeStart(context, Intent(Intent.ACTION_DELETE, "package:$packageName".toUri()))

fun openVoiceAssistant(context: Context): Boolean =
    safeStart(context, Intent(Intent.ACTION_VOICE_COMMAND)) ||
        safeStart(context, Intent(Intent.ACTION_WEB_SEARCH))

fun openInternetSettings(context: Context): Boolean {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    } else {
        Intent(Settings.ACTION_WIFI_SETTINGS)
    }
    return safeStart(context, intent)
}

fun openBluetoothSettings(context: Context): Boolean =
    safeStart(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

fun openAirplaneSettings(context: Context): Boolean =
    safeStart(context, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))

fun openAndroidSettings(context: Context): Boolean =
    safeStart(context, Intent(Settings.ACTION_SETTINGS))

fun openDefaultLauncherSettings(context: Context): Boolean =
    safeStart(context, Intent(Settings.ACTION_HOME_SETTINGS))

// ---------------------------------------------------------------- batería

data class BatteryState(val percent: Int, val charging: Boolean)

private fun parseBattery(intent: Intent?): BatteryState {
    if (intent == null) return BatteryState(0, false)
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryState(percent, charging)
}

@Composable
fun rememberBatteryState(): State<BatteryState> {
    val context = LocalContext.current
    val state = remember {
        // ACTION_BATTERY_CHANGED es "sticky": registrar sin receptor devuelve el valor actual.
        mutableStateOf(
            parseBattery(context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                state.value = parseBattery(intent)
            }
        }
        // Es un broadcast protegido del sistema, solo el sistema puede enviarlo.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

// ---------------------------------------------------------------- linterna

@Composable
fun rememberTorchState(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                state.value = enabled
            }
        }
        manager?.registerTorchCallback(callback, null)
        onDispose { manager?.unregisterTorchCallback(callback) }
    }
    return state
}

fun setTorch(context: Context, enabled: Boolean): Boolean {
    return try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        manager.setTorchMode(cameraId, enabled)
        true
    } catch (_: Exception) {
        false
    }
}

// ---------------------------------------------------------------- conectividad (solo lectura)

fun isWifiOn(context: Context): Boolean = try {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    wifi?.isWifiEnabled == true
} catch (_: Exception) {
    false
}

fun isBluetoothOn(context: Context): Boolean = try {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    manager?.adapter?.isEnabled == true
} catch (_: Exception) {
    false
}

fun isAirplaneOn(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

// ---------------------------------------------------------------- volumen y brillo

fun getVolumeFraction(context: Context): Float {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    return if (max > 0) audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max else 0f
}

fun setVolumeFraction(context: Context, fraction: Float) {
    try {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (fraction * max).roundToInt().coerceIn(0, max),
            0
        )
    } catch (_: SecurityException) {
        // algunos modos "No molestar" restringen el volumen
    }
}

fun canWriteSettings(context: Context): Boolean = Settings.System.canWrite(context)

fun getBrightnessFraction(context: Context): Float = try {
    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
} catch (_: Settings.SettingNotFoundException) {
    0.5f
}

fun setBrightnessFraction(context: Context, fraction: Float) {
    if (!canWriteSettings(context)) return
    try {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            (fraction * 255).roundToInt().coerceIn(1, 255)
        )
    } catch (_: SecurityException) {
        // sin permiso WRITE_SETTINGS
    }
}

fun requestWriteSettings(context: Context): Boolean =
    safeStart(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()))

// ---------------------------------------------------------------- almacenamiento y alarma

/** (libre, total) en bytes de la partición de datos. */
fun storageInfo(): Pair<Long, Long> {
    val stat = StatFs(Environment.getDataDirectory().path)
    return stat.availableBytes to stat.totalBytes
}

fun formatGigabytes(bytes: Long): String =
    String.format(Locale.getDefault(), "%.0f GB", bytes / 1_073_741_824.0)

fun nextAlarmMillis(context: Context): Long? {
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    return alarms?.nextAlarmClock?.triggerTime
}
