from __future__ import annotations

import logging
import subprocess
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

    Verwendet mehrere Strategien:
    1. pywinauto findwindows (eigene C-Level Enumeration)
    2. win32gui.EnumWindows (Python-Callback)
    3. PID-basierte Suche ueber Claude-Prozesse
    """
    EXCLUDED_TITLES = {"mic overlay", ""}

    # --- Strategie 1: pywinauto (robusteste Methode) ---
    try:
        from pywinauto import findwindows, handleprops
        all_hwnds = findwindows.find_windows(visible_only=True)
        log.info("pywinauto fand %d sichtbare Fenster", len(all_hwnds))

        for hwnd in all_hwnds:
            if overlay_hwnd and hwnd == overlay_hwnd:
                continue
            try:
                title = handleprops.text(hwnd)
            except Exception:
                continue
            if not title or title.lower() in EXCLUDED_TITLES:
                continue

            if "claude" in title.lower():
                log.info("Claude-Fenster gefunden (pywinauto/Titel): hwnd=%s title=%r", hwnd, title)
                return hwnd

        # PID-basierte Suche
        if settings:
            claude_pids = _get_claude_pids(settings)
            if claude_pids:
                log.info("Claude PIDs: %s", claude_pids)
                for hwnd in all_hwnds:
                    if overlay_hwnd and hwnd == overlay_hwnd:
                        continue
                    try:
                        pid = handleprops.processid(hwnd)
                        title = handleprops.text(hwnd)
                    except Exception:
                        continue
                    if pid in claude_pids and title and title.lower() not in EXCLUDED_TITLES:
                        log.info("Claude-Fenster gefunden (pywinauto/PID): hwnd=%s title=%r pid=%s",
                                 hwnd, title, pid)
                        return hwnd

        # Debug
        log.warning("pywinauto: Claude nicht gefunden. Top 20 Fenster:")
        for hwnd in all_hwnds[:20]:
            try:
                title = handleprops.text(hwnd)
                pid = handleprops.processid(hwnd)
                log.warning("  hwnd=%s pid=%s title=%r", hwnd, pid, title)
            except Exception:
                pass

    except Exception as exc:
        log.warning("pywinauto fehlgeschlagen: %s", exc)

    # --- Strategie 2: win32gui.EnumWindows (Fallback) ---
    try:
        results = []

        def _enum_cb(hwnd, _):
            try:
                if not win32gui.IsWindowVisible(hwnd):
                    return True
                if overlay_hwnd and hwnd == overlay_hwnd:
                    return True
                title = win32gui.GetWindowText(hwnd)
                if title and title.lower() not in EXCLUDED_TITLES:
                    results.append((hwnd, title))
            except Exception:
                pass
            return True

        win32gui.EnumWindows(_enum_cb, None)
        log.info("win32gui.EnumWindows fand %d Fenster", len(results))

        for hwnd, title in results:
            if "claude" in title.lower():
                log.info("Claude-Fenster gefunden (win32gui): hwnd=%s title=%r", hwnd, title)
                return hwnd

    except Exception as exc:
        log.warning("win32gui.EnumWindows fehlgeschlagen: %s", exc)

    # --- Strategie 3: PowerShell als letzter Fallback ---
    try:
        ps_cmd = (
            "Get-Process | Where-Object {$_.MainWindowHandle -ne 0 -and "
            "$_.MainWindowTitle -like '*Claude*'} | "
            "Select-Object -First 1 -ExpandProperty MainWindowHandle"
        )
        result = subprocess.run(
            ["powershell", "-NoProfile", "-Command", ps_cmd],
            capture_output=True, text=True, timeout=5,
        )
        hwnd_str = result.stdout.strip()
        if hwnd_str and hwnd_str.isdigit():
            hwnd = int(hwnd_str)
            log.info("Claude-Fenster gefunden (PowerShell): hwnd=%s", hwnd)
            return hwnd
        else:
            log.warning("PowerShell: Kein Claude-Fenster gefunden. stdout=%r", hwnd_str)
    except Exception as exc:
        log.warning("PowerShell Fallback fehlgeschlagen: %s", exc)

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


def insert_text_into_claude(text: str, overlay_hwnd: int | None = None,
                            claude_hwnd: int | None = None,
                            settings: Settings | None = None, **_kwargs) -> None:
    """Fuegt Text an der aktuellen Cursorposition in Claude ein."""
    if not text.strip():
        return

    hwnd = claude_hwnd or _find_claude_hwnd(overlay_hwnd, settings)
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
                       claude_hwnd: int | None = None,
                       settings: Settings | None = None, **_kwargs) -> None:
    """Leert das Eingabefeld in Claude (Ctrl+A, dann Backspace)."""
    hwnd = claude_hwnd or _find_claude_hwnd(overlay_hwnd, settings)
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
