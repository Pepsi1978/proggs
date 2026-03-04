from __future__ import annotations

import ctypes
import ctypes.wintypes
import logging
import time
from typing import Optional

import psutil

from config import Settings

log = logging.getLogger(__name__)

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


def _set_clipboard_text(text: str) -> bool:
    """Setzt den Clipboard-Text direkt via Win32 API."""
    CF_UNICODETEXT = 13
    kernel32 = ctypes.windll.kernel32

    if not _user32.OpenClipboard(0):
        log.warning("OpenClipboard fehlgeschlagen")
        return False

    try:
        _user32.EmptyClipboard()

        data = text.encode("utf-16-le") + b"\x00\x00"
        h_mem = kernel32.GlobalAlloc(0x0042, len(data))
        if not h_mem:
            return False

        ptr = kernel32.GlobalLock(h_mem)
        if not ptr:
            kernel32.GlobalFree(h_mem)
            return False

        ctypes.memmove(ptr, data, len(data))
        kernel32.GlobalUnlock(h_mem)

        result = _user32.SetClipboardData(CF_UNICODETEXT, h_mem)
        if not result:
            kernel32.GlobalFree(h_mem)
            return False

        return True
    finally:
        _user32.CloseClipboard()


def paste_text(text: str, tk_root=None, **_kwargs) -> None:
    """Kopiert Text in die Zwischenablage."""
    if not text.strip():
        return

    clipboard_ok = _set_clipboard_text(text)

    if not clipboard_ok and tk_root:
        try:
            tk_root.clipboard_clear()
            tk_root.clipboard_append(text)
            tk_root.update()
            clipboard_ok = True
        except Exception as exc:
            log.warning("Tkinter Clipboard fehlgeschlagen: %s", exc)

    if not clipboard_ok:
        raise RuntimeError("Clipboard konnte nicht gesetzt werden.")

    log.info("Text in Zwischenablage (%d Zeichen)", len(text))


def clear_input(**_kwargs) -> None:
    """Leert die Zwischenablage."""
    if _user32.OpenClipboard(0):
        _user32.EmptyClipboard()
        _user32.CloseClipboard()
