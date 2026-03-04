from __future__ import annotations

import logging
import time
from typing import Optional

import psutil
import pythoncom
import pyperclip
import win32com.client
import win32gui
import win32con

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
    """Gibt das aktuell aktive Fenster (HWND) zurueck."""
    try:
        hwnd = win32gui.GetForegroundWindow()
        if hwnd and hwnd != 0:
            return hwnd
    except Exception:
        pass
    return None


def _activate_window(hwnd: int) -> None:
    """Bringt ein Fenster zuverlaessig in den Vordergrund."""
    try:
        if win32gui.IsIconic(hwnd):
            win32gui.ShowWindow(hwnd, win32con.SW_RESTORE)
        win32gui.SetForegroundWindow(hwnd)
    except Exception:
        try:
            win32gui.ShowWindow(hwnd, win32con.SW_SHOW)
            win32gui.BringWindowToTop(hwnd)
        except Exception as exc:
            log.warning("Fenster konnte nicht aktiviert werden: %s", exc)


def _get_shell():
    """Erstellt WScript.Shell mit COM-Initialisierung fuer Background-Threads."""
    pythoncom.CoInitialize()
    return win32com.client.Dispatch("WScript.Shell")


def paste_text(text: str, target_hwnd: int | None = None, **_kwargs) -> None:
    """Fuegt Text per Clipboard + Ctrl+V in das Ziel-Fenster ein.

    Wenn target_hwnd angegeben, wird dieses Fenster aktiviert.
    Sonst wird AppActivate('Claude') als Fallback versucht.
    """
    if not text.strip():
        return

    pyperclip.copy(text)

    if target_hwnd:
        _activate_window(target_hwnd)
        time.sleep(0.3)
        try:
            shell = _get_shell()
            shell.SendKeys("^v")
        finally:
            try:
                pythoncom.CoUninitialize()
            except Exception:
                pass
        return

    # Fallback: AppActivate
    try:
        shell = _get_shell()
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
        _activate_window(target_hwnd)
        time.sleep(0.3)
        try:
            shell = _get_shell()
            shell.SendKeys("^a")
            time.sleep(0.05)
            shell.SendKeys("{BACKSPACE}")
        finally:
            try:
                pythoncom.CoUninitialize()
            except Exception:
                pass
        return

    # Fallback
    try:
        shell = _get_shell()
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


# Abwaertskompatible Aliase
def insert_text_into_claude(text: str, claude_hwnd: int | None = None, **kwargs) -> None:
    paste_text(text, target_hwnd=claude_hwnd, **kwargs)


def clear_claude_input(claude_hwnd: int | None = None, **kwargs) -> None:
    clear_input(target_hwnd=claude_hwnd, **kwargs)
