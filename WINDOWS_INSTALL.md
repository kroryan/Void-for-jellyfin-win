# Void for Jellyfin - Windows Desktop

## 🚀 Cómo ejecutar la aplicación

### Opción 1: Script launcher (RECOMENDADO)
Doble clic en **`START_VOID.bat`** - esto iniciará la aplicación directamente.

### Opción 2: Desde Gradle
```bash
# En una terminal/cmd en la carpeta del proyecto
gradlew.bat :desktop:run
```

### Opción 3: Instalador .exe
El instalador `desktop/build/compose/binaries/main/exe/Void for Jellyfin-0.2.6.exe` puede dar problemas en algunos sistemas.

**Si el .exe se cierra inmediatamente:**
- Es un problema conocido con el empaquetado en algunos sistemas Windows
- Usa `START_VOID.bat` en su lugar
- O ejecuta `gradlew.bat :desktop:run` desde una terminal

## 📋 Requisitos previos

1. **VLC Media Player (64-bit)** - https://www.videolan.org/vlc/
2. **Java 17+** - https://adoptium.net/ (opcional, viene bundled en la app)

## ⚙️ Configuración inicial

1. Inicia la aplicación con `START_VOID.bat`
2. Introduce la URL de tu servidor Jellyfin (ej: `http://192.168.1.100:8096`)
3. Introduce tu usuario y contraseña
4. ¡Disfruta!

## 🎮 Características

- ✅ Búsqueda de películas y series
- ✅ Favoritos
- ✅ Navegación por temporadas y episodios
- ✅ Reproducción con VLC
- ✅ Pantalla completa (F11)
- ✅ Interfaz 30% más grande
- ✅ Tema oscuro cinemático

## 🔧 Solución de problemas

**La app no se inicia:**
- Verifica que VLC está instalado (versión 64-bit)
- Verifica que tu servidor Jellyfin es accesible desde el navegador
- Ejecuta desde una terminal para ver mensajes de error

**Problemas de reproducción:**
- VLC debe estar instalado en la ruta por defecto
- Verifica que el video funciona en el cliente web de Jellyfin

**Pantalla blanca o se cierra:**
- Ejecuta `START_VOID.bat` para ver los mensajes de error
- Revisa que Java esté instalado correctamente

## 📝 Notas técnicas

- La aplicación está construida con Compose Multiplatform
- Usa VLCJ para reproducción de video
- Las credenciales se guardan en `%USERPROFILE%\.void-jellyfin\prefs.json`
- Para salir: Cierra la ventana o pulsa el botón × en la barra de título personalizada
