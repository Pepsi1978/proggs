@echo off
cd /d "%~dp0"
call .venv\Scripts\activate.bat
pythonw src\overlay_app.py
