@echo off

chcp 65001 > nul
echo =======================================================================
echo  Lancement des Tests Unitaires KeycePass (:shared)
echo =======================================================================
echo.
echo Ce script va compiler le module partagé et executer les 37 tests unitaires.
echo Une fois termine, le rapport detaille s'ouvrira dans votre navigateur.
echo.
echo Lancement en cours...
echo.

call .\gradlew.bat :shared:jvmTest

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo -----------------------------------------------------------------------
    echo  [ERREUR] Certains tests ont echoue ou une erreur est survenue !
    echo -----------------------------------------------------------------------
) else (
    echo.
    echo -----------------------------------------------------------------------
    echo  [SUCCES] Tous les tests unitaires sont passes avec succes !
    echo -----------------------------------------------------------------------
)

echo.
echo Ouverture du rapport HTML dans votre navigateur par defaut...
start "" "shared\build\reports\tests\jvmTest\index.html"
echo.
pause
