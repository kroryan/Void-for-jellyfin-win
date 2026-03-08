@echo off
echo ========================================
echo VLC Diagnostic Tool for Void Jellyfin
echo ========================================
echo.

echo Checking VLC installation...
echo.

REM Check if VLC is in PATH
where vlc >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] VLC found in PATH:
    where vlc
    echo.
) else (
    echo [X] VLC not found in PATH
    echo.
)

REM Check standard installation paths
set "VLC_PATHS=C:\Program Files\VideoLAN\VLC C:\Program Files (x86)\VideoLAN\VLC"

for %%P in (%VLC_PATHS%) do (
    if exist "%%P" (
        echo [OK] Found VLC directory: %%P
        if exist "%%P\vlc.exe" (
            echo     - vlc.exe: OK
        ) else (
            echo     - vlc.exe: MISSING
        )
        if exist "%%P\plugins" (
            echo     - plugins\: OK
        ) else (
            echo     - plugins\: MISSING
        )
        if exist "%%P\libvlc.dll" (
            echo     - libvlc.dll: OK
        ) else (
            echo     - libvlc.dll: MISSING
        )
        if exist "%%P\libvlccore.dll" (
            echo     - libvlccore.dll: OK
        ) else (
            echo     - libvlccore.dll: MISSING
        )
        echo.
    ) else (
        echo [X] VLC directory not found: %%P
        echo.
    )
)

REM Check if 64-bit VLC
echo Checking VLC architecture...
if exist "C:\Program Files\VideoLAN\VLC\vlc.exe" (
    echo Found VLC in Program Files (likely 64-bit) - GOOD
) else if exist "C:\Program Files (x86)\VideoLAN\VLC\vlc.exe" (
    echo Found VLC in Program Files (x86) (32-bit) - PROBLEM!
    echo.
    echo WARNING: You have 32-bit VLC installed.
    echo This app requires 64-bit VLC to work properly.
    echo Please download 64-bit VLC from: https://www.videolan.org/vlc/
    echo.
)

REM Check Java architecture
echo.
echo Checking Java architecture...
java -version 2>&1 | findstr /i "64-bit"
if %errorlevel% equ 0 (
    echo Java is 64-bit - GOOD
) else (
    echo Java might be 32-bit - could cause issues with VLC
)

echo.
echo ========================================
echo Summary:
echo ========================================
echo.
echo VLC requirements for Void Jellyfin:
echo - VLC 64-bit version
echo - Installed in: C:\Program Files\VideoLAN\VLC
echo - All DLL files present (libvlc.dll, libvlccore.dll)
echo - Plugins folder present
echo.
echo If VLC is not found:
echo 1. Download VLC 64-bit from: https://www.videolan.org/vlc/
echo 2. Install it to the default location
echo 3. Restart Void Jellyfin
echo.
echo ========================================
pause
