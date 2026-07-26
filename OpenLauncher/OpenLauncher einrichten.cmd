@echo off
setlocal

rem Ein-Klick-Einrichtung: Doppelklick genuegt, kein Rechtsklick, keine PowerShell-Kenntnisse.
rem
rem Warum eine .cmd und nicht direkt das .ps1: Ein Doppelklick auf eine .ps1-Datei OEFFNET sie im
rem Editor, statt sie auszufuehren, und "Mit PowerShell ausfuehren" scheitert auf vielen Rechnern an
rem der Execution-Policy. Beides umgeht dieser Starter mit -ExecutionPolicy Bypass.
rem
rem Die eigentliche Arbeit macht release-und-verknuepfung.ps1: Profile aus dem alten Ordner
rem uebernehmen, pruefen, alten Ordner entfernen, Release bauen, Desktop-Verknuepfung anlegen.

title OpenLauncher einrichten

set "SKRIPT=%~dp0release-und-verknuepfung.ps1"
if not exist "%SKRIPT%" (
    echo.
    echo FEHLER: Die Datei release-und-verknuepfung.ps1 liegt nicht neben dieser Datei.
    echo Erwartet wurde: "%SKRIPT%"
    echo.
    echo Bitte diese Datei im Ordner OpenLauncher belassen und erneut doppelklicken.
    echo.
    pause
    exit /b 1
)

rem pwsh (PowerShell 7) bevorzugen, sonst das mitgelieferte Windows PowerShell nutzen.
where pwsh >nul 2>&1
if %ERRORLEVEL% equ 0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%SKRIPT%"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SKRIPT%"
)

set "ERGEBNIS=%ERRORLEVEL%"
echo.
if %ERGEBNIS% neq 0 (
    echo Die Einrichtung wurde NICHT abgeschlossen. Die Meldung oben nennt den Grund.
) else (
    echo Fertig. Die Verknuepfung "OpenLauncher" liegt jetzt auf dem Desktop.
)
echo.
rem Fenster offen halten: Nach einem Doppelklick schliesst sich die Konsole sonst sofort und
rem die Meldungen - gerade die im Fehlerfall - waeren nicht lesbar.
pause
exit /b %ERGEBNIS%
