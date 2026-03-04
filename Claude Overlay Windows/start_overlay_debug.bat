@echo off
cd /d "%~dp0"
call .venv\Scripts\activate.bat
echo === Watcher Debug-Modus ===
echo Druecke Ctrl+C zum Beenden.
echo.
python src\watcher.py
pause
