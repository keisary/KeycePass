@echo off
title KeycePass
setlocal

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
set "APP_DIR=%~dp0"

echo [KeycePass] Lancement de l'application...
start "" "%JAVA_HOME%\bin\javaw.exe" ^
    -Dskiko.renderApi=SOFTWARE ^
    -Dskiko.window.api=gdi ^
    -Dskiko.enableVsync=false ^
    -cp "%APP_DIR%build\libs\*;%APP_DIR%shared\build\libs\*;%USERPROFILE%\.gradle\caches\modules-2\files-2.1\*" ^
    com.ak.keycepass.desktop.MainKt

echo [KeycePass] Fenetre lancee.
