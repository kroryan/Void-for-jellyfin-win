@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cls
echo ╔══════════════════════════════════════════════════════════════╗
echo ║       Void for Jellyfin - Herramienta de Diagnóstico       ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

cd /d "%~dp0"

echo [1/7] Verificando Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo   ❌ Java no está instalado o no está en PATH
    echo   📥 Instala Java 17+ desde: https://adoptium.net/
    echo.
    pause
    exit /b 1
) else (
    for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%i
    echo   ✅ Java encontrado: !JAVA_VER!
)
echo.

echo [2/7] Verificando VLC...
if not exist "C:\Program Files\VideoLAN\VLC\vlc.exe" (
    if not exist "C:\Program Files (x86)\VideoLAN\VLC\vlc.exe" (
        echo   ❌ VLC no está instalado
        echo   📥 Instala VLC 64-bit desde: https://www.videolan.org/vlc/
        echo.
        pause
        exit /b 1
    ) else (
        echo   ✅ VLC encontrado en Program Files (x86)
    )
) else (
    echo   ✅ VLC encontrado en Program Files
)
echo.

echo [3/7] Verificando Gradle...
if not exist "gradlew.bat" (
    echo   ❌ gradlew.bat no encontrado
    echo   📂 Ejecuta este script desde la carpeta del proyecto
    echo.
    pause
    exit /b 1
) else (
    echo   ✅ Gradle wrapper encontrado
)
echo.

echo [4/7] Limpiando builds anteriores...
call gradlew.bat :desktop:clean >nul 2>&1
if errorlevel 1 (
    echo   ⚠️  Advertencia: clean falló (puede ser normal si hay archivos en uso)
) else (
    echo   ✅ Limpieza completada
)
echo.

echo [5/7] Compilando aplicación...
echo   Esto puede tardar varios minutos...
echo.
call gradlew.bat :desktop:compileKotlin
if errorlevel 1 (
    echo   ❌ Error de compilación
    echo.
    pause
    exit /b 1
) else (
    echo   ✅ Compilación exitosa
)
echo.

echo [6/7] Creando instalador .exe...
echo   Esto puede tardar varios minutos...
echo.
call gradlew.bat :desktop:packageExe
if errorlevel 1 (
    echo   ❌ Error al crear el instalador
    echo.
    echo   Si el error es "Unable to delete directory":
    echo   1. Cierra Void for Jellyfin si está abierto
    echo   2. Cierra el instalador si está abierto
    echo   3. Reinicia el ordenador si persiste
    echo.
    pause
    exit /b 1
) else (
    echo   ✅ Instalador creado exitosamente
)
echo.

echo [7/7] Verificando instalador...
if exist "desktop\build\compose\binaries\main\exe\Void for Jellyfin-0.2.6.exe" (
    for %%A in ("desktop\build\compose\binaries\main\exe\Void for Jellyfin-0.2.6.exe") do set SIZE=%%~zA
    set /size SIZE=!SIZE! / 1024 / 1024
    echo   ✅ Instalador creado: !SIZE! MB
    echo   📂 Ubicación: desktop\build\compose\binaries\main\exe\
    echo.
    echo ════════════════════════════════════════════════════════════
    echo   ✨ ¡LISTO! El instalador está listo para usar
    echo ════════════════════════════════════════════════════════════
    echo.
    echo   Para instalar:
    echo   1. Ve a: desktop\build\compose\binaries\main\exe\
    echo   2. Doble clic en "Void for Jellyfin-0.2.6.exe"
    echo   3. Sigue las instrucciones del instalador
    echo.
    echo   Alternativa: ejecuta START_VOID.bat para usar sin instalar
    echo.
) else (
    echo   ❌ No se encontró el instalador
    echo.
    pause
    exit /b 1
)

pause
