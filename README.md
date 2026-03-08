<div align="center">

<img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/Fugaz%20One_void.png" alt="Void Logo" width="160" height="160">

**A modern, minimal Jellyfin client — for Android and Windows**

Built entirely with **Kotlin**, **Jetpack Compose** and **Compose Multiplatform**

[![Android API](https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg?style=flat&logo=android)](https://android-arsenal.com/api?level=26)
[![Windows](https://img.shields.io/badge/Windows-10%2F11-blue.svg?style=flat&logo=windows)](https://github.com/hritwikjohri/Void-for-jellyfin-win/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.0-green.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

</div>

---

## 🖥️ Windows Desktop Client *(new)*

A native Windows desktop app built with **Compose Multiplatform**, sharing the same Jellyfin API layer as the Android app.

### Features
- **Custom dark title bar** — borderless window with minimize / maximize / close controls
- **Library browsing** — full grid view of your Jellyfin libraries and media items
- **Media detail view** — backdrop, overview, cast, genres, ratings, mark played / favorite
- **Embedded video player** — powered by [VLC](https://www.videolan.org/vlc/) via VLCJ
  - Seek bar, play/pause, ±10 s skip, mute
  - **Fullscreen mode** — F11 or the ⛶ button (Escape to exit)
  - Controls auto-hide on mouse inactivity; persistent back button always visible
- **Persistent sessions** — credentials stored in `~/.void-jellyfin/prefs.json`

### Requirements
- Windows 10 / 11 (64-bit)
- **[VLC Media Player](https://www.videolan.org/vlc/)** installed (for video playback)
- Java 17+ (bundled in the installer)

### Installation
1. Download `Void for Jellyfin-0.2.6.exe` from the [Releases](../../releases) page
2. Run the installer
3. Launch **Void for Jellyfin** from the Start menu
4. Enter your Jellyfin server URL and sign in

### Build from source (Windows)

> Step-by-step — no experience needed.

**Prerequisites (install these first):**

1. **Git** — [git-scm.com](https://git-scm.com/download/win) → download and install with default settings
2. **JDK 17** — [adoptium.net](https://adoptium.net/) → download **Temurin 17 LTS**, run the `.msi` installer, make sure to tick "Set JAVA_HOME"
3. **VLC Media Player (64-bit)** — [videolan.org/vlc](https://www.videolan.org/vlc/) → required at runtime for video playback
4. *(Optional — to build the installer)* **WiX Toolset 3.x** — [wixtoolset.org](https://wixtoolset.org/) → required only if you want to produce `.msi`/`.exe` installers

**Clone and run:**

```bat
:: Open a Command Prompt (cmd) or PowerShell window
git clone https://github.com/hritwikjohri/Void-for-jellyfin-win.git
cd Void-for-jellyfin-win

:: Run the app directly (no install needed)
gradlew.bat :desktop:run
```

**Build the installer (.exe):**

```bat
:: Produces the installer at:
::   desktop\build\compose\binaries\main\exe\Void for Jellyfin-0.2.6.exe
gradlew.bat :desktop:packageExe
```

**Build the MSI package:**

```bat
:: Produces an MSI at:
::   desktop\build\compose\binaries\main\msi\Void for Jellyfin-0.2.6.msi
gradlew.bat :desktop:packageMsi
```

> On first run Gradle will download all dependencies (~500 MB). This is normal. Subsequent builds are fast.

---

## 📱 Android App

### ✨ Features

#### 🎯 Core Functionality
- **Jellyfin Integration** — Full server authentication and user management
- **Library Browsing** — Explore your media collection with intuitive navigation
- **Advanced Search** — Find your content quickly with comprehensive search
- **Detailed Media Views** — Rich metadata display with cast, crew, and plot information

#### 🎵 Media Playback
- **MPV Player** — High-quality video playback with excellent format support
- **ExoPlayer Support** — Alternative playback engine for broader compatibility
- **Picture-in-Picture** — Continue watching while using other apps
- **Theme Song Support** — Immersive experience with background audio

#### 📱 Modern UI/UX
- **Material 3 Design** — Beautiful, adaptive UI following Google's design principles
- **Dynamic Themes** — Colors that adapt to your content and system preferences
- **Ambient Backgrounds** — Stunning visual effects that enhance your viewing experience
- **Responsive Layout** — Optimized for phones, tablets, and various screen sizes

#### 🔄 Advanced Features
- **Offline Downloads** — Download content for offline viewing with progress notifications
- **Client-side Watchlist** — Keep track of your favourite content
- **Multiple Quality Options** — Auto, 4K, 1080p, 720p, 480p, 360p
- **Subtitle Support** — Full subtitle support with customisable sizing
- **Background Sync** — Automatic content synchronisation

---

## 📸 Screenshots

<div align="center">

| Splash | Server Setup | Login |
|--------|--------------|-------|
| <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/02_Splash.png?raw=true" alt="Splash" width="200" height="400"> | <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/03_Server.png?raw=true" alt="Server Setup" width="200" height="400"> | <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/04_Login.png?raw=true" alt="Login" width="200" height="400"> |

| Home | Library | Series |
|------|---------|--------|
| <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/05_Home.png?raw=true" alt="Home" width="200" height="400"> | <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/06_Library.png?raw=true" alt="Library" width="200" height="400"> | <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/09_Series.png?raw=true" alt="Series" width="200" height="400"> |

| Category | Search | Season |
|----------|--------|--------|
| <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/07_Category.png?raw=true" alt="Category" width="200" height="400"> | <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/08_Search.png?raw=true" alt="Search" width="200" height="400"> | <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/10_Season.png?raw=true" alt="Season" width="200" height="400"> |

</div>

---

## 🚀 Getting Started — Android

### Prerequisites
- Android device running **API 26+** (Android 8.0)
- A running **Jellyfin server** (version 10.8+)

### Installation (APK)
1. Go to the [Releases](../../releases) page
2. Download the latest `.apk`
3. On your Android phone go to **Settings → Apps → Special app access → Install unknown apps**, find your browser or file manager, and enable it
4. Open the downloaded `.apk` and tap **Install**
5. Launch **Void** and enter your Jellyfin server URL

### Build from source (Android)

> Step-by-step — no experience needed.

**Prerequisites (install these first):**

1. **Git** — [git-scm.com](https://git-scm.com/download/win)
2. **Android Studio** (latest stable) — [developer.android.com/studio](https://developer.android.com/studio) → install with default settings, let it download the Android SDK automatically
3. **JDK 17** — comes bundled with Android Studio; nothing extra needed

**Clone and open in Android Studio:**

```bat
git clone https://github.com/hritwikjohri/Void-for-jellyfin-win.git
```

1. Open **Android Studio**
2. Click **File → Open** and select the cloned `Void-for-jellyfin-win` folder
3. Wait for Gradle sync to finish (first time takes a few minutes)
4. Connect your Android phone via USB **or** start an emulator (AVD Manager → create a device with API 26+)
5. Press the green **▶ Run** button — the app will install and launch automatically

**Build a release APK:**

1. In Android Studio: **Build → Generate Signed App Bundle / APK**
2. Choose **APK** → create or use an existing keystore → follow the wizard
3. The APK will be in `app/release/app-release.apk`

Or via command line:

```bat
gradlew.bat :app:assembleRelease
:: Output: app\build\outputs\apk\release\app-release.apk
```

---

## 🛠️ Tech Stack

### Android
| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Async | Coroutines + Flow |
| Media | MPV-Android + ExoPlayer (Media3) |
| Networking | Retrofit + OkHttp |
| Storage | Room + DataStore |
| Images | Coil |

### Windows Desktop
| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Compose Multiplatform 1.7.0 + Material 3 |
| Networking | Retrofit + OkHttp (shared) |
| Images | Coil 3 (multiplatform) |
| Video | VLCJ 4.8.3 (embedded VLC) |
| Storage | JSON file (`~/.void-jellyfin/`) |

---

## 📄 License

```
GPL V3 — https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/LICENSE
```

---

## 🙏 Acknowledgments

- **[Jellyfin Project](https://jellyfin.org/)** — For the amazing open-source media server
- **[MPV](https://mpv.io/) & AndroidX Media3** — For excellent media playback
- **[@nitanmarcel](https://github.com/nitanmarcel)** — For the mpv-compose library
- **[VideoLAN / VLC](https://www.videolan.org/)** — For the VLCJ desktop player
- **[JetBrains](https://www.jetbrains.com/)** — For Compose Multiplatform

---

<div align="center">

## ☕ Support the Project

If you find Void useful, consider buying me a coffee!

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-yellow?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/hritwikjohri)

**Your support helps maintain and improve Void for everyone! 🚀**

---

## 🤝 Collaborators

| [Hritwik Johri](https://github.com/hritwikjohri) | [KHazard](https://github.com/khazard) |
|:---:|:---:|
| Lead Developer | Contributor |

---

**Made with ❤️ by the Void Team**

[⭐ Star this repository](../../stargazers) · [🐛 Report Bug](../../issues) · [💡 Request Feature](../../issues)

</div>
