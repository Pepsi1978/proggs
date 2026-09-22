@echo off
chcp 65001 >nul
cd /d "%~dp0"
rem Suno Archiv: ALLE eigenen Songs nach C:\Suno Archiv (nicht nur Daumen hoch).
rem Eigener Port, damit es neben Suno Backup laufen kann.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0downloader.ps1" "C:\Suno Archiv" --alle --port 8788 %*
