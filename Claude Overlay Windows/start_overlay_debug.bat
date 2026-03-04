@echo off
setlocal
cd /d "%~dp0"
python "%~dp0src\overlay_app.py"
endlocal
