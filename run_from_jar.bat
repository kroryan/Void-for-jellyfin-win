@echo off
cd /d "%~dp0"

echo Starting Void for Jellyfin...
echo.

REM Check if JAVA_HOME is set
if "%JAVA_HOME%"=="" (
    echo WARNING: JAVA_HOME not set. Using system default Java.
    echo.
)

REM Run the compiled JAR directly
gradlew.bat :desktop:run

if errorlevel 1 (
    echo.
    echo Error running application. Press any key to exit...
    pause >nul
)
