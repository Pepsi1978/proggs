@echo off
chcp 65001 >nul
title SunoDownload - Suno-Bibliothek herunterladen
cd /d "%~dp0"

echo.
echo  Der Download startet. Die Songs landen in  C:\Sono Backup
echo.

node suno-download.ts %*

echo.
echo  Fenster kann jetzt geschlossen werden.
pause
