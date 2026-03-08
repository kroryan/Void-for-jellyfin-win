@echo off
echo Attempting to force clean build directory...

cd /d "%~dp0"

REM Try to delete using various methods
echo Closing any Java processes...
taskkill /F /IM java.exe /T 2>nul
taskkill /F /IM javaw.exe /T 2>nul

echo Waiting for processes to terminate...
timeout /t 2 /nobreak >nul

echo Attempting to clean...
rmdir /S /Q "desktop\build\compose\binaries\main\exe" 2>nul

if exist "desktop\build\compose\binaries\main\exe" (
    echo.
    echo ❌ Cannot delete directory. Files may be in use.
    echo.
    echo Please:
    echo 1. Close Void for Jellyfin if running
    echo 2. Close any file explorers showing the exe
    echo 3. Run this script as administrator
    echo 4. Or restart your computer
    echo.
    pause
    exit /b 1
) else (
    echo ✅ Build directory cleaned successfully
    echo.
    echo You can now run: gradlew.bat :desktop:packageExe
    echo.
    pause
)
