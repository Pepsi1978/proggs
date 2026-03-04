from __future__ import annotations

import time
from typing import Optional

import psutil
import pythoncom
import pyperclip
import win32com.client

from config import Settings


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
    """Erstellt WScript.Shell mit COM-Initialisierung fuer Background-Threads."""
    pythoncom.CoInitialize()
    return win32com.client.Dispatch("WScript.Shell")


def insert_text_into_claude(text: str, **_kwargs) -> None:
    """Fuegt Text an der aktuellen Cursorposition in Claude ein."""
    if not text.strip():
        return

    try:
        shell = _get_shell()
        pyperclip.copy(text)

        if not shell.AppActivate("Claude"):
            raise RuntimeError("Claude-Fenster wurde nicht gefunden.")

        time.sleep(0.3)
        shell.SendKeys("^v")
    finally:
        pythoncom.CoUninitialize()


def clear_claude_input(**_kwargs) -> None:
    """Leert das Eingabefeld in Claude (Ctrl+A, dann Backspace)."""
    try:
        shell = _get_shell()

        if not shell.AppActivate("Claude"):
            raise RuntimeError("Claude-Fenster wurde nicht gefunden.")

        time.sleep(0.3)
        shell.SendKeys("^a")
        time.sleep(0.05)
        shell.SendKeys("{BACKSPACE}")
    finally:
        pythoncom.CoUninitialize()
