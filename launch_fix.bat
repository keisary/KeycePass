@echo off
title KeycePass

setlocal EnableDelayedExpansion

echo =========================================
echo   KeycePass - Presence / Badging Desktop
echo =========================================
echo Construction du classpath (83 jars)...
echo.

REM Chemins
set "APP_HOME=C:\Users\jules\Desktop\stack\controle-presence\KeycePass\desktopApp\build\install\desktopApp"
set "LIB_DIR=%APP_HOME%\lib"
set "MAIN_CLASS=com.ak.keycepass.desktop.MainKt"

REM Construire le classpath avec 8.3 chemins (plus courts)
set "CP="
for %%f in ("%LIB_DIR%\*.jar") do (
    set "CP=!CP!%%f;"
)

REM Fichier reponse Java - ordre correct (JVM opts AVANT classpath)
echo -Dskiko.renderApi=SOFTWARE_FAST > "%TEMP%\keycepass_args.txt"
echo -classpath >> "%TEMP%\keycepass_args.txt"
echo !CP! >> "%TEMP%\keycepass_args.txt"
echo %MAIN_CLASS% >> "%TEMP%\keycepass_args.txt"

echo Lancement de l'application...
echo.

REM javaw = pas de console (fenetre graphique seule)
"C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\javaw" @"%TEMP%\keycepass_args.txt"

echo.
echo App fermee. Appuyez sur une touche.
pause >nul
endlocal
