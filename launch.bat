@echo off
title KeycePass

echo =========================================
echo   KeycePass - Presence / Badging Desktop
echo =========================================
echo.

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
set "LIB_DIR=C:\Users\jules\Desktop\stack\controle-presence\KeycePass\desktopApp\build\install\desktopApp\lib"
set "MAIN=com.ak.keycepass.desktop.MainKt"

echo Lancement...
"%JAVA_HOME%\bin\javaw" -Dskiko.renderApi=SOFTWARE_FAST -classpath "%LIB_DIR%\*" %MAIN%

echo.
echo App fermee.
pause >nul
