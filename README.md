# 📱 SalvitoLauncher

**SalvitoLauncher** is a custom Android home screen replacement built using **Kotlin** and **Jetpack Compose**. It accurately mimics the visual aesthetic, fluid interactions, and core features of **iOS (iPhone & iPad)** on Android devices.

---

## ✨ Features

- 📱 **iOS Home Screen & Paged Grid**:
  - Horizontal page swiping (`HorizontalPager`) with dynamic page indicator dots (`• • o •`).
  - Adaptive layout: **4 columns** on phones (16 apps/page) and **6 columns** on tablets & foldables (24 apps/page).
- 🏝️ **Dynamic Island Pill**:
  - Top-center animated dark pill shape. Tapping expands it to show live charging state (`🔋 88%`) or music playback.
- ⚡ **Spotlight Search Bar**:
  - Search bar with live filtering of installed apps and integrated microphone icon.
- 🔒 **iOS Lock Screen Overlay**:
  - Authentic lock screen featuring bold clock typography, date display, Flashlight & Camera quick action buttons, and tap/swipe to unlock.
- 🎛️ **iOS Control Center**:
  - Tapping status bar icons opens an iOS Control Center with Wi-Fi, Bluetooth, Flashlight, Airplane Mode toggles, plus Brightness and Volume sliders.
- 🎨 **iOS 18 App Icon Themes**:
  - Cycle between **Default**, **Dark Icons**, and **Tinted Monochromatic Glass Icons** themes.
- 🔄 **Smart Stack Widget Carousel**:
  - Vertical swiping or tapping on the main header widget cycles between 4 live stack cards (*Date/Clock*, *Battery & Storage*, *Reminders*, *Favorite Quote*).
- 📂 **App Library & Category Folders**:
  - Right-most page features the iOS App Library categorized into *Games*, *Social*, *Entertainment*, *Productivity*, and *Utilities*.
  - Tapping a category opens a full-screen frosted glass folder modal (`Modifier.blur`).
- 💫 **Jiggle Mode (Edit Mode)**:
  - Long-pressing an app icon triggers the wobble animation with a delete (`-`) badge on each icon.
- 🎨 **Wallpaper Switcher**:
  - Double-tapping anywhere on the desktop background cycles between 4 iOS gradient themes (*Classic Deep Blue*, *Sunset Purple*, *Emerald Dark*, *Midnight Dark*).
- ✏️ **App Hiding & Custom Label Editing**:
  - Options to **Rename Apps** or **Hide Apps** from the primary grid via the long-press menu.
- 🇪🇸 **Bilingual Support (Español & English)**:
  - Full localization support toggleable from Launcher Settings.
- ⚙️ **Launcher Settings**:
  - Long-pressing wallpaper background opens Launcher Settings to unhide apps, reset custom app names, or switch language.

---

## 🛠️ Project Requirements

- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 37 (Android 15)
- **Language**: Kotlin 2.x
- **UI Toolkit**: Jetpack Compose with Material 3 & Compose BOM

---

## 🚀 Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/SalvitoLauncher.git
   cd SalvitoLauncher
   ```

2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on device/emulator**:
   ```bash
   ./gradlew installDebug
   ```

4. **Set as Default Launcher**:
   - Press the **Home** button on your Android device.
   - Select **SalvitoLauncher** and tap **Always**.

---

## 📂 Project Structure

```
SalvitoLauncher/
├── app/
│   ├── src/main/
│   │   ├── java/com/salvador/salvitolauncher/
│   │   │   └── MainActivity.kt       # Entire Compose UI & Launcher logic
│   │   └── AndroidManifest.xml       # Launcher intent filters & QUERY_ALL_PACKAGES
│   └── build.gradle.kts              # App dependencies & SDK versions
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
