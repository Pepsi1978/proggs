@echo off
setlocal
cd /d "%~dp0"
start "ClaudeOverlayWatcher" pythonw "%~dp0src\watcher.py"
endlocal
