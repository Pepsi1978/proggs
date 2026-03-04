from __future__ import annotations

import ctypes
import logging
import subprocess
import time
from typing import Optional

import psutil
import pythoncom
import pyperclip
import win32com.client

from config import Settings

log = logging.getLogger(__name__)

# Direkte ctypes-Referenz auf user32.dll
_user32 = ctypes.windll.user32


def is_claude_running(settings: Settings) -> bool:
    """Prueft, ob ein Claude-Prozess laeuft."""
    names = set(settings.claude_process_names)
    for proc in psutil.process_iter(["name"]):
        name = (proc.info.get("name") or "").lower()
        for candidate in names:
            if candidate and candidate in name:
                return True
    return False


def get_foreground_window() -> int | None:
    """Gibt das aktuell aktive Fenster (HWND) zurueck."""
    try:
        hwnd = _user32.GetForegroundWindow()
        if hwnd and hwnd != 0:
            return hwnd
    except Exception:
        pass
    return None


def _activate_window(hwnd: int) -> bool:
    """Aktiviert ein Fenster per ctypes user32."""
    try:
        if _user32.IsIconic(hwnd):
            _user32.ShowWindow(hwnd, 9)  # SW_RESTORE
        result = _user32.SetForegroundWindow(hwnd)
        log.info("SetForegroundWindow(%s) = %s", hwnd, result)
        return bool(result)
    except Exception as exc:
        log.warning("_activate_window fehlgeschlagen: %s", exc)
        return False


def _send_keys(keys: str) -> None:
    """Sendet Tasten via WScript.Shell COM."""
    try:
        pythoncom.CoInitialize()
        shell = win32com.client.Dispatch("WScript.Shell")
        shell.SendKeys(keys)
    finally:
        try:
            pythoncom.CoUninitialize()
        except Exception:
            pass


def paste_text(text: str, target_hwnd: int | None = None, **_kwargs) -> None:
    """Fuegt Text per Clipboard + Ctrl+V in das Ziel-Fenster ein."""
    if not text.strip():
        return

    pyperclip.copy(text)

    if target_hwnd:
        if _activate_window(target_hwnd):
            time.sleep(0.3)
            _send_keys("^v")
            return

    # Fallback: AppActivate
    try:
        pythoncom.CoInitialize()
        shell = win32com.client.Dispatch("WScript.Shell")
        if shell.AppActivate("Claude"):
            log.info("Claude via AppActivate gefunden")
            time.sleep(0.3)
            shell.SendKeys("^v")
            return
    except Exception as exc:
        log.warning("AppActivate fehlgeschlagen: %s", exc)
    finally:
        try:
            pythoncom.CoUninitialize()
        except Exception:
            pass
    raise RuntimeError("Ziel-Fenster konnte nicht aktiviert werden.")


def clear_input(target_hwnd: int | None = None, **_kwargs) -> None:
    """Leert das Eingabefeld (Ctrl+A, dann Backspace)."""
    if target_hwnd:
        if _activate_window(target_hwnd):
            time.sleep(0.3)
            _send_keys("^a")
            time.sleep(0.05)
            _send_keys("{BACKSPACE}")
            return

    # Fallback
    try:
        pythoncom.CoInitialize()
        shell = win32com.client.Dispatch("WScript.Shell")
        if shell.AppActivate("Claude"):
            time.sleep(0.3)
            shell.SendKeys("^a")
            time.sleep(0.05)
            shell.SendKeys("{BACKSPACE}")
            return
    except Exception:
        pass
    finally:
        try:
            pythoncom.CoUninitialize()
        except Exception:
            pass
    raise RuntimeError("Ziel-Fenster konnte nicht aktiviert werden.")
