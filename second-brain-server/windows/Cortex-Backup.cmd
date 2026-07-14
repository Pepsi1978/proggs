@echo off
REM Cortex-Backup.cmd — DEIN BACKUP-KNOPF.
REM Doppelklick (oder Stream-Deck "Open/System: Cortex-Backup.cmd") -> zieht ein komplettes,
REM server-unabhaengiges Backup des zweiten Gehirns auf diesen PC. Einrichtung: BACKUP-RESTORE-README.md.
title Cortex Komplett-Backup
REM pwsh (PowerShell 7+) bevorzugen, Fallback auf Windows PowerShell 5.1. Bewusst per if/else statt
REM "where pwsh && pwsh ... || powershell ..." — bei der &&/||-Kette wuerde ein NICHT-0 Exit des
REM Skripts (z.B. Abbruch via Die) faelschlich AUCH noch powershell starten (Doppel-Ausfuehrung).
where pwsh >nul 2>&1
if %errorlevel%==0 (
  pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0cortex-full-backup.ps1"
) else (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0cortex-full-backup.ps1"
)
