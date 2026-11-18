package com.salvador.salvitolauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Gestor de paquetes de iconos (Icon Packs) de la Play Store.
 * Soporta los formatos estándar de launchers (Nova, ADW, Apex, Go, Solo, etc.).
 */
object IconPackManager {

    private const val ICON_PX = 192

    private val ICON_PACK_INTENTS = listOf(
        "com.novalauncher.THEME",
        "org.adw.launcher.THEMES",
        "com.dlto.atom.launcher.THEME",
        "com.gau.go.launcherex.theme",
        "com.solo.launcher.FREE_THEME",
        "com.fede.launcher.THEME_ICONPACK",
        "com.teslacoilsw.launcher.THEME",
        "com.anddoes.launcher.THEME"
    )

    private data class AppFilterData(
        val iconMap: Map<String, String>, // key: "pkg/activity" o "pkg", value: drawable name
        val iconBacks: List<String>,
        val iconMask: String?,
        val iconUpon: String?,
        val scale: Float
    )

    @Volatile
    private var cachedPackPackage: String? = null

    @Volatile
    private var cachedAppFilter: AppFilterData? = null

    @Volatile
    private var cachedResources: Resources? = null

    /**
     * Busca en el sistema todos los paquetes de iconos instalados desde la Play Store o externas.
     */
    fun getAvailableIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val iconPacks = mutableMapOf<String, IconPackInfo>()

        for (action in ICON_PACK_INTENTS) {
            val intent = Intent(action)
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg != context.packageName && !iconPacks.containsKey(pkg)) {
                    val label = info.loadLabel(pm).toString()
                    val iconDrawable = info.loadIcon(pm)
                    val iconBitmap = try {
                        iconDrawable.toBitmap(96, 96).asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                    iconPacks[pkg] = IconPackInfo(
                        packageName = pkg,
                        label = label,
                        icon = iconBitmap
                    )
                }
            }
        }

        return iconPacks.values.sortedBy { it.label.lowercase() }
    }

    /**
     * Carga el icono de una app usando el paquete de iconos seleccionado.
     * Si el paquete de iconos no contiene un icono específico para la app, aplica máscara/fondo si el pack lo soporta.
     * Retorna null si no se pudo cargar ningún icono del pack (para usar el por defecto del sistema).
     */
    fun loadAppIcon(
        context: Context,
        iconPackPackage: String?,
        appPackageName: String,
        pm: PackageManager,
        defaultDrawable: Drawable
    ): ImageBitmap? {
        if (iconPackPackage.isNull_or_blank()) return null
        val packPackage = iconPackPackage!!

        val filter = getOrLoadAppFilter(context, packPackage) ?: return null
        val res = cachedResources ?: return null

        // 1. Buscar correspondencia en el appfilter por nombre de paquete
        val drawableName = filter.iconMap[appPackageName]
            ?: filter.iconMap.entries.firstOrNull { it.key.startsWith("$appPackageName/") }?.value

        if (drawableName != null) {
            val resId = res.getIdentifier(drawableName, "drawable", packPackage)
            if (resId != 0) {
                try {
                    val drawable = ResourcesCompat.getDrawable(res, resId, null)
                    if (drawable != null) {
                        return drawable.toBitmap(ICON_PX, ICON_PX).asImageBitmap()
                    }
                } catch (_: Exception) {
                }
            }
        }

        // 2. Si no hay icono directo pero el pack provee fondos/máscaras (iconback, iconmask, etc.), generar icono adaptado
        if (filter.iconBacks.isNotEmpty() || filter.iconMask != null || filter.iconUpon != null) {
            val masked = composeMaskedIcon(res, packPackage, filter, defaultDrawable)
            if (masked != null) {
                return masked.asImageBitmap()
            }
        }

        return null
    }

    /**
     * Abre Google Play Store para buscar paquetes de iconos.
     */
    fun openPlayStoreIconPacks(context: Context) {
        val marketUri = Uri.parse("market://search?q=icon%20pack&c=apps")
        val webUri = Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps")
        val intent = Intent(Intent.ACTION_VIEW, marketUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    private fun getOrLoadAppFilter(context: Context, iconPackPackage: String): AppFilterData? {
        if (cachedPackPackage == iconPackPackage && cachedAppFilter != null) {
            return cachedAppFilter
        }

        val pm = context.packageManager
        val res = try {
            pm.getResourcesForApplication(iconPackPackage)
        } catch (_: Exception) {
            return null
        }

        cachedResources = res
        val appFilter = parseAppFilter(res, iconPackPackage)
        cachedPackPackage = iconPackPackage
        cachedAppFilter = appFilter
        return appFilter
    }

    private fun parseAppFilter(res: Resources, packageName: String): AppFilterData {
        val iconMap = mutableMapOf<String, String>()
        val iconBacks = mutableListOf<String>()
        var iconMask: String? = null
        var iconUpon: String? = null
        var scale = 1.0f

        var inputStream: InputStream? = null
        try {
            inputStream = res.assets.open("appfilter.xml")
        } catch (_: Exception) {
            // Intentar desde recurso XML
            val resId = res.getIdentifier("appfilter", "xml", packageName)
            if (resId != 0) {
                try {
                    val xmlParser = res.getXml(resId)
                    parseXmlParser(xmlParser, iconMap, iconBacks, { iconMask = it }, { iconUpon = it }, { scale = it })
                    return AppFilterData(iconMap, iconBacks, iconMask, iconUpon, scale)
                } catch (_: Exception) {
                }
            }
        }

        if (inputStream != null) {
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(inputStream, "UTF-8")
                parseXmlParser(parser, iconMap, iconBacks, { iconMask = it }, { iconUpon = it }, { scale = it })
            } catch (_: Exception) {
            } finally {
                try { inputStream.close() } catch (_: Exception) {}
            }
        }

        return AppFilterData(iconMap, iconBacks, iconMask, iconUpon, scale)
    }

    private fun parseXmlParser(
        parser: XmlPullParser,
        iconMap: MutableMap<String, String>,
        iconBacks: MutableList<String>,
        setIconMask: (String?) -> Unit,
        setIconUpon: (String?) -> Unit,
        setScale: (Float) -> Unit
    ) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val name = parser.name?.lowercase()
                when (name) {
                    "item" -> {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (!component.isNull_or_blank() && !drawable.isNull_or_blank()) {
                            val cleanComp = cleanComponentString(component)
                            iconMap[cleanComp] = drawable
                        }
                    }
                    "iconback" -> {
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            if (attrName.startsWith("img")) {
                                val value = parser.getAttributeValue(i)
                                if (!value.isNull_or_blank()) iconBacks.add(value)
                            }
                        }
                    }
                    "iconmask" -> {
                        val value = parser.getAttributeValue(null, "img1")
                        if (!value.isNull_or_blank()) setIconMask(value)
                    }
                    "iconupon" -> {
                        val value = parser.getAttributeValue(null, "img1")
                        if (!value.isNull_or_blank()) setIconUpon(value)
                    }
                    "scale" -> {
                        val factor = parser.getAttributeValue(null, "factor")
                        factor?.toFloatOrNull()?.let { setScale(it) }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun cleanComponentString(raw: String): String {
        // Ejemplo raw: "ComponentInfo{com.whatsapp/com.whatsapp.Main}" -> "com.whatsapp/com.whatsapp.Main"
        val start = raw.indexOf('{')
        val end = raw.indexOf('}')
        val content = if (start != -1 && end != -1 && end > start) {
            raw.substring(start + 1, end)
        } else {
            raw
        }
        val pkg = content.substringBefore('/')
        return pkg
    }

    private fun composeMaskedIcon(
        res: Resources,
        packageName: String,
        filter: AppFilterData,
        defaultDrawable: Drawable
    ): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Dibujar icono de fondo (iconback)
            if (filter.iconBacks.isNotEmpty()) {
                val backName = filter.iconBacks.random()
                val backResId = res.getIdentifier(backName, "drawable", packageName)
                if (backResId != 0) {
                    val backDrawable = ResourcesCompat.getDrawable(res, backResId, null)
                    backDrawable?.setBounds(0, 0, ICON_PX, ICON_PX)
                    backDrawable?.draw(canvas)
                }
            }

            // 2. Escalar y dibujar icono original
            val origBitmap = defaultDrawable.toBitmap(ICON_PX, ICON_PX)
            val scaleFactor = filter.scale.coerceIn(0.2f, 1.0f)
            val scaledWidth = (ICON_PX * scaleFactor).toInt()
            val scaledHeight = (ICON_PX * scaleFactor).toInt()
            val left = (ICON_PX - scaledWidth) / 2
            val top = (ICON_PX - scaledHeight) / 2

            val scaledBitmap = Bitmap.createScaledBitmap(origBitmap, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)

            // 3. Aplicar máscara (iconmask)
            if (filter.iconMask != null) {
                val maskResId = res.getIdentifier(filter.iconMask, "drawable", packageName)
                if (maskResId != 0) {
                    val maskDrawable = ResourcesCompat.getDrawable(res, maskResId, null)
                    if (maskDrawable != null) {
                        val maskBitmap = maskDrawable.toBitmap(ICON_PX, ICON_PX)
                        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                        }
                        canvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)
                    }
                }
            }

            // 4. Dibujar sobrecapa (iconupon)
            if (filter.iconUpon != null) {
                val uponResId = res.getIdentifier(filter.iconUpon, "drawable", packageName)
                if (uponResId != 0) {
                    val uponDrawable = ResourcesCompat.getDrawable(res, uponResId, null)
                    uponDrawable?.setBounds(0, 0, ICON_PX, ICON_PX)
                    uponDrawable?.draw(canvas)
                }
            }

            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
