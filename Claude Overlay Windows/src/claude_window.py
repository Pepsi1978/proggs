from __future__ import annotations

import ctypes
import ctypes.wintypes
import logging
import time
from typing import Optional

import psutil
import pythoncom
import pyperclip
import win32com.client

from config import Settings

log = logging.getLogger(__name__)

# Win32 API Konstanten
SW_RESTORE = 9
GW_OWNER = 4
WS_EX_TOOLWINDOW = 0x00000080
WS_EX_APPWINDOW = 0x00040000
GWL_EXSTYLE = -20

user32 = ctypes.windll.user32


def is_claude_running(settings: Settings) -> bool:
    """Prueft, ob ein Claude-Prozess laeuft."""
    names = set(settings.claude_process_names)
    for proc in psutil.process_iter(["name"]):
        name = (proc.info.get("name") or "").lower()
        for candidate in names:
            if candidate and candidate in name:
                return True
    return False


def _find_claude_hwnd(overlay_hwnd: int | None = None) -> int | None:
    """Findet das Claude Desktop Hauptfenster per win32 EnumWindows.

    Sucht nach sichtbaren Top-Level-Fenstern deren Titel 'Claude' enthaelt,
    aber schliesst das eigene Overlay-Fenster aus.
    """
    found_hwnd = None
    EXCLUDED_TITLES = {"mic overlay"}

    def _enum_callback(hwnd, _lparam):
        nonlocal found_hwnd

        # Nur sichtbare Fenster
        if not user32.IsWindowVisible(hwnd):
            return True

        # Eigenes Overlay-Fenster ausschliessen
        if overlay_hwnd and hwnd == overlay_hwnd:
            return True

        # Fenstertitel lesen
        length = user32.GetWindowTextLengthW(hwnd)
        if length == 0:
            return True

        buf = ctypes.create_unicode_buffer(length + 1)
        user32.GetWindowTextW(hwnd, buf, length + 1)
        title = buf.value.lower()

        # Overlay-Titel ausschliessen
        if title in EXCLUDED_TITLES:
            return True

        # Pruefen ob es ein Claude-Fenster ist
        if "claude" in title:
            # Toolwindows (kleine Popups) ausschliessen
            ex_style = user32.GetWindowLongW(hwnd, GWL_EXSTYLE)
            if ex_style & WS_EX_TOOLWINDOW:
                return True

            found_hwnd = hwnd
            return False  # Gefunden, Enumeration stoppen

        return True

    WNDENUMPROC = ctypes.WINFUNCTYPE(
        ctypes.wintypes.BOOL,
        ctypes.wintypes.HWND,
        ctypes.wintypes.LPARAM,
    )
    user32.EnumWindows(WNDENUMPROC(_enum_callback), 0)
    return found_hwnd


def _activate_window(hwnd: int) -> None:
    """Bringt ein Fenster zuverlaessig in den Vordergrund."""
    # Minimiertes Fenster wiederherstellen
    if user32.IsIconic(hwnd):
        user32.ShowWindow(hwnd, SW_RESTORE)

    user32.SetForegroundWindow(hwnd)


def _get_shell():
    """Erstellt WScript.Shell mit COM-Initialisierung fuer Background-Threads."""
    pythoncom.CoInitialize()
    return win32com.client.Dispatch("WScript.Shell")


def insert_text_into_claude(text: str, overlay_hwnd: int | None = None, **_kwargs) -> None:
    """Fuegt Text an der aktuellen Cursorposition in Claude ein."""
    if not text.strip():
        return

    hwnd = _find_claude_hwnd(overlay_hwnd)
    if not hwnd:
        # Fallback: AppActivate versuchen
        try:
            shell = _get_shell()
            if shell.AppActivate("Claude"):
                pyperclip.copy(text)
                time.sleep(0.3)
                shell.SendKeys("^v")
                return
        except Exception:
            pass
        finally:
            try:
                pythoncom.CoUninitialize()
            except Exception:
                pass
        raise RuntimeError("Claude-Fenster wurde nicht gefunden.")

    pyperclip.copy(text)
    _activate_window(hwnd)
    time.sleep(0.3)

    try:
        shell = _get_shell()
        shell.SendKeys("^v")
    finally:
        try:
            pythoncom.CoUninitialize()
        except Exception:
            pass


def clear_claude_input(overlay_hwnd: int | None = None, **_kwargs) -> None:
    """Leert das Eingabefeld in Claude (Ctrl+A, dann Backspace)."""
    hwnd = _find_claude_hwnd(overlay_hwnd)
    if not hwnd:
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
        raise RuntimeError("Claude-Fenster wurde nicht gefunden.")

    _activate_window(hwnd)
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
