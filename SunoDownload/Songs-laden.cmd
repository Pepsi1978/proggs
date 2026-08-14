@echo off
chcp 65001 >nul
title SunoDownload - Suno-Bibliothek herunterladen
cd /d "%~dp0"

echo.
echo  Der Download startet. Es oeffnet sich gleich ein Chrome-Fenster.
echo  Melde dich dort einmal bei Suno an - danach laeuft alles von allein.
echo.

node suno-download.ts %*

echo.
echo  Fenster kann jetzt geschlossen werden.
pause
