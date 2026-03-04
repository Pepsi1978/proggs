from __future__ import annotations

import time
from typing import Optional

import psutil
import pyperclip
from pywinauto import Desktop
from pywinauto.controls.hwndwrapper import HwndWrapper
from pywinauto.keyboard import send_keys

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


def _find_claude_window() -> Optional[HwndWrapper]:
    """Sucht das Claude-Desktop-Fenster ueber den Fenstertitel."""
    desktop = Desktop(backend="uia")
    windows = desktop.windows()

    for window in windows:
        title = (window.window_text() or "").lower()
        if "claude" in title:
            return window

    return None


def _focus_input(window: HwndWrapper) -> None:
    """Versucht, das Eingabefeld im Claude-Fenster zu fokussieren."""
    try:
        edits = window.descendants(control_type="Edit")
        if edits:
            edits[-1].set_focus()
            return
    except Exception:
        pass

    window.set_focus()


def insert_text_into_claude(text: str) -> None:
    """Fuegt Text in das Claude-Eingabefeld ein (ueber Zwischenablage + Ctrl+V)."""
    if not text.strip():
        return

    window = _find_claude_window()
    if window is None:
        raise RuntimeError("Claude-Fenster wurde nicht gefunden.")

    window.set_focus()
    time.sleep(0.15)
    _focus_input(window)
    time.sleep(0.1)

    pyperclip.copy(text)
    send_keys("^v")


def clear_claude_input() -> None:
    """Leert das Eingabefeld in Claude (Ctrl+A, dann Backspace)."""
    window = _find_claude_window()
    if window is None:
        raise RuntimeError("Claude-Fenster wurde nicht gefunden.")

    window.set_focus()
    time.sleep(0.15)
    _focus_input(window)
    time.sleep(0.1)

    send_keys("^a")
    time.sleep(0.05)
    send_keys("{BACKSPACE}")
