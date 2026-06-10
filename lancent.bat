@echo off
title KeycePass
cd /d "C:\Users\jules\Desktop\stack\controle-presence\KeycePass"

echo ============================================
echo   KeycePass - Lancement direct (Gradle)
echo ============================================
echo.
echo Demarrage du serveur et de l'interface...
echo.

REM Lancer directement avec javaw via Gradle - pas de daemon
set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot

call "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java" ^
    -Dskiko.renderApi=SOFTWARE_FAST ^
    -cp desktopApp\build\libs\desktopApp-1.0.0.jar;shared\build\libs\shared-jvm.jar ^
    com.ak.keycepass.desktop.MainKt

pause
