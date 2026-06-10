@echo off
title KeycePass (Console)

echo =========================================
echo   KeycePass - Presence / Badging Desktop
echo =========================================
echo.

echo Lancement (mode console - logs visibles)...
echo.
call gradlew.bat :desktopApp:run

echo.
echo App fermee.
pause
