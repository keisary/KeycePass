@echo off
title KeycePass

echo =========================================
echo   KeycePass - Administration des Presences
echo =========================================
echo.

echo Lancement (backend + interface)...
call gradlew.bat :desktopApp:run

echo.
echo App fermee. Double-cliquez pour relancer.
pause >nul
