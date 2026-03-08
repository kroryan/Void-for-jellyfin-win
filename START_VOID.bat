@echo off
title Void for Jellyfin
color 0A

echo ============================================
echo    Void for Jellyfin - Launcher
echo ============================================
echo.
echo Starting Void for Jellyfin...
echo.

cd /d "%~dp0"

REM Check if Gradle wrapper exists
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found!
    echo Please run this script from the project directory.
    echo.
    pause
    exit /b 1
)

REM Run the desktop application
echo Launching desktop application...
echo.
gradlew.bat :desktop:run

if errorlevel 1 (
    echo.
    echo ============================================
    echo Error occurred while starting application
    echo ============================================
    echo.
    echo Possible issues:
    echo   1. VLC not installed - Install VLC 64-bit from videolan.org
    echo   2. Java not found - Install Java 17+ from adoptium.net
    echo   3. Server URL not configured
    echo.
)

pause
