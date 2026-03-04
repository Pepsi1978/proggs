from __future__ import annotations

import ctypes
import ctypes.wintypes
import logging
import struct
import time
from typing import Optional

import psutil

from config import Settings

log = logging.getLogger(__name__)

# --- ctypes Strukturen fuer SendInput ---
INPUT_KEYBOARD = 1
KEYEVENTF_KEYUP = 0x0002
VK_CONTROL = 0x11
VK_MENU = 0x12  # Alt
VK_TAB = 0x09
VK_V = 0x56
VK_A = 0x41
VK_BACK = 0x08


class KEYBDINPUT(ctypes.Structure):
    _fields_ = [
        ("wVk", ctypes.wintypes.WORD),
        ("wScan", ctypes.wintypes.WORD),
        ("dwFlags", ctypes.wintypes.DWORD),
        ("time", ctypes.wintypes.DWORD),
        ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong)),
    ]


class INPUT(ctypes.Structure):
    class _INPUT(ctypes.Union):
        _fields_ = [("ki", KEYBDINPUT)]
    _fields_ = [
        ("type", ctypes.wintypes.DWORD),
        ("_input", _INPUT),
    ]


_user32 = ctypes.windll.user32


def _send_key_combo(*vk_codes: int) -> None:
    """Sendet eine Tastenkombination via SendInput (z.B. Ctrl+V)."""
    inputs = []

    # Tasten druecken
    for vk in vk_codes:
        inp = INPUT()
        inp.type = INPUT_KEYBOARD
        inp._input.ki.wVk = vk
        inp._input.ki.dwFlags = 0
        inp._input.ki.time = 0
        inp._input.ki.dwExtraInfo = ctypes.pointer(ctypes.c_ulong(0))
        inputs.append(inp)

    # Tasten loslassen (umgekehrte Reihenfolge)
    for vk in reversed(vk_codes):
        inp = INPUT()
        inp.type = INPUT_KEYBOARD
        inp._input.ki.wVk = vk
        inp._input.ki.dwFlags = KEYEVENTF_KEYUP
        inp._input.ki.time = 0
        inp._input.ki.dwExtraInfo = ctypes.pointer(ctypes.c_ulong(0))
        inputs.append(inp)

    arr = (INPUT * len(inputs))(*inputs)
    sent = _user32.SendInput(len(inputs), arr, ctypes.sizeof(INPUT))
    log.info("SendInput: %d/%d Events gesendet", sent, len(inputs))


def _set_clipboard_text(text: str) -> bool:
    """Setzt den Clipboard-Text direkt via Win32 API."""
    CF_UNICODETEXT = 13
    kernel32 = ctypes.windll.kernel32

    if not _user32.OpenClipboard(0):
        log.warning("OpenClipboard fehlgeschlagen")
        return False

    try:
        _user32.EmptyClipboard()

        # Text als Wide-String (UTF-16LE) + Null-Terminator
        data = text.encode("utf-16-le") + b"\x00\x00"
        h_mem = kernel32.GlobalAlloc(0x0042, len(data))  # GMEM_MOVEABLE | GMEM_ZEROINIT
        if not h_mem:
            log.warning("GlobalAlloc fehlgeschlagen")
            return False

        ptr = kernel32.GlobalLock(h_mem)
        if not ptr:
            kernel32.GlobalFree(h_mem)
            return False

        ctypes.memmove(ptr, data, len(data))
        kernel32.GlobalUnlock(h_mem)

        result = _user32.SetClipboardData(CF_UNICODETEXT, h_mem)
        if not result:
            log.warning("SetClipboardData fehlgeschlagen")
            kernel32.GlobalFree(h_mem)
            return False

        log.info("Clipboard gesetzt: %d Zeichen", len(text))
        return True
    finally:
        _user32.CloseClipboard()


def is_claude_running(settings: Settings) -> bool:
    """Prueft, ob ein Claude-Prozess laeuft."""
    names = set(settings.claude_process_names)
    for proc in psutil.process_iter(["name"]):
        name = (proc.info.get("name") or "").lower()
        for candidate in names:
            if candidate and candidate in name:
                return True
    return False


def paste_text(text: str, tk_root=None, **_kwargs) -> None:
    """Fuegt Text per Clipboard + Alt+Tab + Ctrl+V ein."""
    if not text.strip():
        return

    # Clipboard setzen (direkt via Win32 API)
    clipboard_ok = _set_clipboard_text(text)

    # Fallback: Tkinter Clipboard
    if not clipboard_ok and tk_root:
        try:
            tk_root.clipboard_clear()
            tk_root.clipboard_append(text)
            tk_root.update()
            clipboard_ok = True
            log.info("Clipboard via Tkinter gesetzt")
        except Exception as exc:
            log.warning("Tkinter Clipboard fehlgeschlagen: %s", exc)

    if not clipboard_ok:
        raise RuntimeError("Clipboard konnte nicht gesetzt werden.")

    # Alt+Tab zum vorherigen Fenster
    _send_key_combo(VK_MENU, VK_TAB)
    time.sleep(0.5)

    # Ctrl+V einfuegen
    _send_key_combo(VK_CONTROL, VK_V)
    log.info("Text eingefuegt (%d Zeichen)", len(text))


def clear_input(tk_root=None, **_kwargs) -> None:
    """Leert das Eingabefeld (Alt+Tab, Ctrl+A, Backspace)."""
    _send_key_combo(VK_MENU, VK_TAB)
    time.sleep(0.5)
    _send_key_combo(VK_CONTROL, VK_A)
    time.sleep(0.05)
    _send_key_combo(VK_BACK)
