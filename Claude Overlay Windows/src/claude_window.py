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
WS_EX_TOOLWINDOW = 0x00000080
GWL_EXSTYLE = -20

user32 = ctypes.windll.user32

# Callback-Typ global definieren (verhindert Garbage Collection)
WNDENUMPROC = ctypes.WINFUNCTYPE(
    ctypes.wintypes.BOOL,
    ctypes.wintypes.HWND,
    ctypes.wintypes.LPARAM,
)


def is_claude_running(settings: Settings) -> bool:
    """Prueft, ob ein Claude-Prozess laeuft."""
    names = set(settings.claude_process_names)
    for proc in psutil.process_iter(["name"]):
        name = (proc.info.get("name") or "").lower()
        for candidate in names:
            if candidate and candidate in name:
                return True
    return False


def _get_claude_pids(settings: Settings) -> set[int]:
    """Gibt alle PIDs von Claude-Prozessen zurueck."""
    pids = set()
    names = set(settings.claude_process_names)
    for proc in psutil.process_iter(["name", "pid"]):
        name = (proc.info.get("name") or "").lower()
        for candidate in names:
            if candidate and candidate in name:
                pids.add(proc.info["pid"])
    return pids


def _find_claude_hwnd(overlay_hwnd: int | None = None,
                      settings: Settings | None = None) -> int | None:
    """Findet das Claude Desktop Hauptfenster.

    Strategie 1: Suche nach sichtbaren Fenstern mit 'Claude' im Titel.
    Strategie 2: Suche nach Fenstern die zum Claude.exe Prozess gehoeren.
    """
    results = []
    EXCLUDED_TITLES = {"mic overlay"}

    # Alle sichtbaren Top-Level-Fenster sammeln
    def _enum_callback(hwnd, _lparam):
        if not user32.IsWindowVisible(hwnd):
            return True

        # Eigenes Overlay ausschliessen
        if overlay_hwnd and hwnd == overlay_hwnd:
            return True

        # Toolwindows ausschliessen
        ex_style = user32.GetWindowLongW(hwnd, GWL_EXSTYLE)
        if ex_style & WS_EX_TOOLWINDOW:
            return True

        # Fenstertitel lesen
        length = user32.GetWindowTextLengthW(hwnd)
        if length == 0:
            return True

        buf = ctypes.create_unicode_buffer(length + 1)
        user32.GetWindowTextW(hwnd, buf, length + 1)
        title = buf.value

        if title.lower() in EXCLUDED_TITLES:
            return True

        # PID des Fensters ermitteln
        pid = ctypes.wintypes.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))

        results.append((hwnd, title, pid.value))
        return True

    cb = WNDENUMPROC(_enum_callback)
    user32.EnumWindows(cb, 0)

    # Strategie 1: Titel enthaelt "claude"
    for hwnd, title, pid in results:
        if "claude" in title.lower():
            log.info("Claude-Fenster gefunden (Titel): hwnd=%s title=%r pid=%s",
                     hwnd, title, pid)
            return hwnd

    # Strategie 2: Fenster gehoert zu einem Claude-Prozess
    if settings:
        claude_pids = _get_claude_pids(settings)
        if claude_pids:
            log.info("Claude PIDs gefunden: %s", claude_pids)
            # Suche das groesste sichtbare Fenster eines Claude-Prozesses
            for hwnd, title, pid in results:
                if pid in claude_pids and title:
                    log.info("Claude-Fenster gefunden (PID): hwnd=%s title=%r pid=%s",
                             hwnd, title, pid)
                    return hwnd

    # Debug: Alle Fenster loggen wenn nichts gefunden
    log.warning("Claude-Fenster nicht gefunden. Sichtbare Fenster:")
    for hwnd, title, pid in results[:20]:
        log.warning("  hwnd=%s pid=%s title=%r", hwnd, pid, title)

    return None


def _activate_window(hwnd: int) -> None:
    """Bringt ein Fenster zuverlaessig in den Vordergrund."""
    if user32.IsIconic(hwnd):
        user32.ShowWindow(hwnd, SW_RESTORE)
    user32.SetForegroundWindow(hwnd)


def _get_shell():
    """Erstellt WScript.Shell mit COM-Initialisierung fuer Background-Threads."""
    pythoncom.CoInitialize()
    return win32com.client.Dispatch("WScript.Shell")


def insert_text_into_claude(text: str, overlay_hwnd: int | None = None,
                            settings: Settings | None = None, **_kwargs) -> None:
    """Fuegt Text an der aktuellen Cursorposition in Claude ein."""
    if not text.strip():
        return

    hwnd = _find_claude_hwnd(overlay_hwnd, settings)
    if not hwnd:
        # Fallback: AppActivate versuchen
        try:
            shell = _get_shell()
            if shell.AppActivate("Claude"):
                log.info("Claude via AppActivate gefunden (Fallback)")
                pyperclip.copy(text)
                time.sleep(0.3)
                shell.SendKeys("^v")
                return
        except Exception as exc:
            log.warning("AppActivate Fallback fehlgeschlagen: %s", exc)
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


def clear_claude_input(overlay_hwnd: int | None = None,
                       settings: Settings | None = None, **_kwargs) -> None:
    """Leert das Eingabefeld in Claude (Ctrl+A, dann Backspace)."""
    hwnd = _find_claude_hwnd(overlay_hwnd, settings)
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
