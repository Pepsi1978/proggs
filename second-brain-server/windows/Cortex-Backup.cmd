@echo off
REM Cortex-Backup.cmd — DEIN BACKUP-KNOPF.
REM Doppelklick (oder Stream-Deck "Open/System: Cortex-Backup.cmd") -> zieht ein komplettes,
REM server-unabhaengiges Backup des zweiten Gehirns auf diesen PC. Einrichtung: BACKUP-RESTORE-README.md.
title Cortex Komplett-Backup
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0cortex-full-backup.ps1"
