@echo off
setlocal

set "WRAPPER=%USERPROFILE%\.codex\hooks\codex-git-wrapper.ps1"
if not exist "%WRAPPER%" set "WRAPPER=%~dp0..\hooks\codex-git-wrapper.ps1"

pwsh -NoProfile -ExecutionPolicy Bypass -File "%WRAPPER%" %*
exit /b %ERRORLEVEL%
