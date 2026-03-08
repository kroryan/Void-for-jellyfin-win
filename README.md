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

### Build from source
```bash
./gradlew :desktop:run          # Run in dev mode
./gradlew :desktop:packageExe   # Build installer (.exe)
./gradlew :desktop:packageMsi   # Build MSI package
```

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

### Installation
1. Go to the [Releases](../../releases) page
2. Download the latest `.apk`
3. Enable *Install from Unknown Sources* in Android settings
4. Install the APK and launch Void

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
