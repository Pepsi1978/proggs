@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem Fast Windows launcher for the Git multi-session PreToolUse hook.
rem It avoids starting PowerShell for shell commands that cannot touch Git.

if not defined TEMP set "TEMP=%USERPROFILE%\AppData\Local\Temp"
set "HOOK=%~dp0codex-git-multi-session-lock.ps1"
if not exist "%HOOK%" set "HOOK=%USERPROFILE%\.codex\hooks\codex-git-multi-session-lock.ps1"

set "INPUT_FILE=%TEMP%\codex-git-hook-%RANDOM%-%RANDOM%.json"

more > "%INPUT_FILE%" 2>nul
if errorlevel 1 (
    del "%INPUT_FILE%" >nul 2>nul
    exit /b 0
)

findstr /l /i /c:"git " /c:"git.exe" /c:"git.cmd" "%INPUT_FILE%" >nul 2>nul
if errorlevel 1 (
    del "%INPUT_FILE%" >nul 2>nul
    exit /b 0
)

type "%INPUT_FILE%" | pwsh -NoProfile -ExecutionPolicy Bypass -File "%HOOK%"
set "HOOK_EXIT=%ERRORLEVEL%"
del "%INPUT_FILE%" >nul 2>nul
exit /b %HOOK_EXIT%
