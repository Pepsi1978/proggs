from __future__ import annotations

import logging
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


def _get_shell():
    """Erstellt WScript.Shell mit COM-Initialisierung."""
    pythoncom.CoInitialize()
    return win32com.client.Dispatch("WScript.Shell")


def _send_keys(keys: str) -> None:
    """Sendet Tasten via WScript.Shell COM."""
    try:
        shell = _get_shell()
        shell.SendKeys(keys)
    finally:
        try:
            pythoncom.CoUninitialize()
        except Exception:
            pass


def paste_text(text: str, **_kwargs) -> None:
    """Fuegt Text per Clipboard + Alt+Tab + Ctrl+V ein.

    Wechselt zum vorherigen Fenster (Alt+Tab) und fuegt dort ein.
    """
    if not text.strip():
        return

    pyperclip.copy(text)

    # Alt+Tab zum vorherigen Fenster (Claude)
    _send_keys("%{TAB}")
    time.sleep(0.5)

    # Ctrl+V einfuegen
    _send_keys("^v")
    log.info("Text eingefuegt (%d Zeichen)", len(text))


def clear_input(**_kwargs) -> None:
    """Leert das Eingabefeld (Alt+Tab, Ctrl+A, Backspace)."""
    _send_keys("%{TAB}")
    time.sleep(0.5)
    _send_keys("^a")
    time.sleep(0.05)
    _send_keys("{BACKSPACE}")
