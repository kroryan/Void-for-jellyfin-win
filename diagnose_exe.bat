@echo off
setlocal enabledelayedexpansion

echo ============================================
echo Diagnosing Void for Jellyfin EXE
echo ============================================
echo.

cd /d "%~dp0desktop\build\compose\binaries\main\exe"

echo Running exe with full output capture...
echo.

"Void for Jellyfin-0.2.6.exe" > output.log 2>&1
set EXIT_CODE=%errorlevel%

echo Exit code: %EXIT_CODE%
echo.
echo === Output ===
type output.log
echo.
echo === End Output ===

echo.
pause
