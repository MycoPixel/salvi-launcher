# 📱 SalvitoLauncher

Launcher de Android hecho con **Kotlin** y **Jetpack Compose** que imita el aspecto del home de iOS:
páginas con grilla de iconos, dock, Biblioteca de apps, modo edición con "jiggle", Centro de Control y
widgets. Todo lo que se muestra es real: no hay datos de relleno.

---

## ✨ Funciones

- **Pantalla de inicio paginada**: grilla que se adapta al alto disponible (ya no se cortan filas),
  4 columnas en teléfono y 6 en tablet, con indicador de páginas.
- **Dock configurable**: por defecto usa tus apps de teléfono, mensajes, navegador y cámara. Se cambia
  con *mantener presionado → Añadir / Quitar del dock*.
- **Biblioteca de apps**: última página, apps agrupadas por categoría. Al tocar una carpeta, el fondo
  se desenfoca (blur real en Android 12+) y se abre la carpeta encima.
- **Fondo de pantalla**: 6 degradados, **el fondo del sistema**, o **una foto de tu galería**
  (Ajustes → Fondo de pantalla).
- **Ajustes** (mantener presionado en el fondo): fondo, estilo de iconos (normal / oscuro / tintado),
  idioma, mostrar nombres, isla dinámica, apps ocultas, nombres personalizados y atajo para fijar el
  launcher como predeterminado. Todo se guarda y sobrevive a reinicios.
- **Modo edición** (jiggle): botón "−" para quitar de la pantalla de inicio o desinstalar, botón *Listo*.
- **Menú contextual** por app: info, renombrar, dock, quitar de inicio, desinstalar.
- **Centro de Control** (tocar la batería o deslizar hacia abajo desde la barra superior):
  - Linterna, volumen y brillo funcionan de verdad.
  - Wi-Fi, Bluetooth y modo avión muestran su estado y abren el panel del sistema (Android no deja
    que una app los active directamente).
  - El brillo pide el permiso *Modificar ajustes del sistema* la primera vez.
- **Widgets reales**: fecha/hora y próxima alarma (se cambia tocando o deslizando), batería y
  almacenamiento libre.
- **Arrastrar y carpetas propias**: en modo edición arrastra iconos para reordenarlos (cambian de
  página al llegar al borde) o suéltalos encima de otro para crear una carpeta; las carpetas se
  renombran y se disuelven desde su menú.
- **Widgets de Android reales**: página a la izquierda del inicio, botón "+" para elegir widgets de
  tus apps (reloj, clima, calendario…); se quitan con "−" en modo edición.
- **Números de notificaciones** en los iconos (Ajustes → activar; requiere acceso a notificaciones).
- **Gestos**: deslizar hacia abajo en el inicio abre la búsqueda; doble toque bloquea la pantalla
  (requiere activar el servicio de accesibilidad "Salvito Launcher", Android 9+).
- **Búsqueda** de apps con resultados en vivo; el micrófono abre el asistente de voz del teléfono.
- **Pulsar Home** estando en el launcher vuelve a la primera página y cierra lo que esté abierto.
- La lista de apps se actualiza sola al instalar o desinstalar.

## 🛠️ Requisitos

- Min SDK 24 · Target/Compile SDK 37
- Kotlin 2.x, Jetpack Compose (Material 3, BOM `2026.02.01`)

## 🚀 Compilar y ejecutar

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Luego pulsa **Home** y elige **SalvitoLauncher → Siempre** (o *Ajustes → Establecer como launcher
predeterminado*).

## 📂 Estructura

```
app/src/main/java/com/salvador/salvitolauncher/
├── MainActivity.kt            # Activity, barras del sistema, señal de "Home"
├── data/
│   ├── Models.kt              # AppInfo, AppCategory, AppIconTheme, WallpaperMode
│   ├── LauncherSettings.kt    # Ajustes persistentes (SharedPreferences + estado Compose)
│   ├── AppRepository.kt       # Carga de apps, categorías, dock por defecto
│   └── WallpaperStorage.kt    # Foto de fondo guardada en almacenamiento privado
├── system/
│   └── SystemControls.kt      # Linterna, batería, volumen, brillo, alarma, intents
└── ui/
    ├── LauncherHome.kt        # Pantalla principal y overlays
    ├── AppIcon.kt             # Icono de app (jiggle, filtros de tema)
    ├── Components.kt          # Barra superior, widgets, búsqueda, dock, menú contextual
    ├── ControlCenter.kt       # Centro de Control
    ├── Library.kt             # Biblioteca de apps y carpeta abierta
    ├── SettingsScreen.kt      # Ajustes
    ├── Wallpaper.kt           # Fondos
    └── Formatters.kt          # Textos, fechas e idioma
```

## 📄 Licencia

Proyecto de código abierto bajo licencia MIT.
