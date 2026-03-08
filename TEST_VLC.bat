@echo off
echo ========================================
echo Advanced VLC Diagnostics
echo ========================================
echo.

REM Find VLC location
set "VLC_DIR="
if exist "C:\Program Files\VideoLAN\VLC\libvlc.dll" (
    set "VLC_DIR=C:\Program Files\VideoLAN\VLC"
)
if exist "C:\Program Files (x86)\VideoLAN\VLC\libvlc.dll" (
    set "VLC_DIR=C:\Program Files (x86)\VideoLAN\VLC"
)

if "%VLC_DIR%"=="" (
    echo [ERROR] VLC not found in standard locations
    goto :end
)

echo Found VLC at: %VLC_DIR%
echo.

REM Check architecture
echo Checking VLC architecture...
filever.exe "%VLC_DIR%\vlc.exe" >nul 2>&1
if %errorlevel% equ 0 (
    echo Using filever to check architecture...
) else (
    echo filever not available, checking file size...
)

echo.
echo Checking required files:
for %%F in (libvlc.dll libvlccore.dll vlc.exe) do (
    if exist "%VLC_DIR%\%%F" (
        echo [OK] %%F
    ) else (
        echo [MISSING] %%F
    )
)

echo.
echo Checking plugins directory:
if exist "%VLC_DIR%\plugins" (
    echo [OK] plugins directory exists
    dir "%VLC_DIR%\plugins" | find /c /v ""
) else (
    echo [MISSING] plugins directory
)

echo.
echo ========================================
echo Testing VLC Direct Launch
echo ========================================
echo.
echo Trying to launch VLC with --help to test if it works...
"%VLC_DIR%\vlc.exe" --help >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] VLC executable works
) else (
    echo [ERROR] VLC executable failed with error code %errorlevel%
)

echo.
echo ========================================
echo Recommendations
echo ========================================
echo.
echo If VLCJ cannot load VLC even though it's installed:
echo.
echo 1. Make sure you have 64-bit VLC (not 32-bit)
echo    - Download from: https://www.videolan.org/vlc/
echo    - During install, choose "64-bit installer"
echo.
echo 2. Add VLC to your system PATH:
echo    - Press Win+R, type: sysdm.cpl
echo    - Advanced ^> Environment Variables
echo    - Add "%VLC_DIR%" to PATH
echo.
echo 3. Restart Void for Jellyfin after making changes
echo.

:end
pause
