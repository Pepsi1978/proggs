from __future__ import annotations

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
    """Gibt das aktuell aktive Fenster (HWND) zurueck via PowerShell."""
    try:
        result = subprocess.run(
            ["powershell", "-NoProfile", "-Command",
             "Add-Type -TypeDefinition 'using System;using System.Runtime.InteropServices;"
             "public class WinAPI{[DllImport(\"user32.dll\")]"
             "public static extern IntPtr GetForegroundWindow();}'; "
             "[WinAPI]::GetForegroundWindow().ToInt64()"],
            capture_output=True, text=True, timeout=3,
        )
        hwnd_str = result.stdout.strip()
        if hwnd_str and hwnd_str.isdigit() and int(hwnd_str) != 0:
            return int(hwnd_str)
    except Exception:
        pass
    return None


def _activate_and_paste(hwnd: int | None, keys: str = "^v") -> bool:
    """Aktiviert ein Fenster per PowerShell und sendet Tasten."""
    if not hwnd:
        return False
    try:
        ps_script = (
            "Add-Type -TypeDefinition '"
            "using System;using System.Runtime.InteropServices;"
            "public class WinAPI{"
            "[DllImport(\"user32.dll\")] public static extern bool SetForegroundWindow(IntPtr hWnd);"
            "[DllImport(\"user32.dll\")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);"
            "[DllImport(\"user32.dll\")] public static extern bool IsIconic(IntPtr hWnd);"
            "}';"
            f"$h = [IntPtr]{hwnd};"
            "if([WinAPI]::IsIconic($h)){[WinAPI]::ShowWindow($h, 9)};"
            "[WinAPI]::SetForegroundWindow($h)"
        )
        result = subprocess.run(
            ["powershell", "-NoProfile", "-Command", ps_script],
            capture_output=True, text=True, timeout=3,
        )
        if "True" in result.stdout:
            time.sleep(0.3)
            # SendKeys via COM
            try:
                pythoncom.CoInitialize()
                shell = win32com.client.Dispatch("WScript.Shell")
                shell.SendKeys(keys)
                return True
            finally:
                try:
                    pythoncom.CoUninitialize()
                except Exception:
                    pass
        else:
            log.warning("SetForegroundWindow fehlgeschlagen fuer hwnd=%s: %s",
                        hwnd, result.stdout.strip())
    except Exception as exc:
        log.warning("_activate_and_paste fehlgeschlagen: %s", exc)
    return False


def paste_text(text: str, target_hwnd: int | None = None, **_kwargs) -> None:
    """Fuegt Text per Clipboard + Ctrl+V in das Ziel-Fenster ein."""
    if not text.strip():
        return

    pyperclip.copy(text)

    if target_hwnd and _activate_and_paste(target_hwnd, "^v"):
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
        if _activate_and_paste(target_hwnd, "^a"):
            time.sleep(0.05)
            try:
                pythoncom.CoInitialize()
                shell = win32com.client.Dispatch("WScript.Shell")
                shell.SendKeys("{BACKSPACE}")
            finally:
                try:
                    pythoncom.CoUninitialize()
                except Exception:
                    pass
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
