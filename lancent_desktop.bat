@echo off
title KeycePass Lancement
cd /d "C:\Users\jules\Desktop\stack\controle-presence\KeycePass"

echo =========================================
echo   KeycePass - Application de Presence
echo =========================================
echo.
echo Demarrage en cours - patientez 15-30s...
echo.

REM Utiliser javaw pour pas de console
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"

REM Lancer via le script genere par Gradle
call "desktopApp\build\install\desktopApp\bin\desktopApp.bat"

echo.
echo App fermee. Appuyez sur une touche pour quitter.
pause >nul
