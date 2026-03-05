from __future__ import annotations

import ctypes
import ctypes.wintypes
import logging
import subprocess
import time
from typing import Optional

import psutil

from config import Settings

log = logging.getLogger(__name__)

# Fuer subprocess: kein sichtbares Konsolenfenster oeffnen
CREATE_NO_WINDOW = 0x08000000

# --- Win32 API ---
_user32 = ctypes.windll.user32

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


# ---------------------------------------------------------------------------
# Tastatur-Simulation
# ---------------------------------------------------------------------------

def _send_key_combo(*vk_codes: int) -> None:
    """Sendet eine Tastenkombination via SendInput (z.B. Ctrl+V)."""
    inputs = []

    for vk in vk_codes:
        inp = INPUT()
        inp.type = INPUT_KEYBOARD
        inp._input.ki.wVk = vk
        inp._input.ki.dwFlags = 0
        inp._input.ki.time = 0
        inp._input.ki.dwExtraInfo = ctypes.pointer(ctypes.c_ulong(0))
        inputs.append(inp)

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


def _send_keys_hidden(keys: str) -> None:
    """Sendet Tasten via PowerShell SendKeys (ohne sichtbares Konsolenfenster).

    Nutzt System.Windows.Forms.SendKeys, das einen Journaling-Hook verwendet
    und dadurch auch in Electron-Apps (Claude Desktop) funktioniert.
    """
    ps_cmd = (
        "Add-Type -AssemblyName System.Windows.Forms; "
        f"[System.Windows.Forms.SendKeys]::SendWait('{keys}')"
    )
    subprocess.run(
        ["powershell", "-NoProfile", "-Command", ps_cmd],
        capture_output=True,
        timeout=5,
        creationflags=CREATE_NO_WINDOW,
    )


# ---------------------------------------------------------------------------
# Clipboard
# ---------------------------------------------------------------------------

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


# ---------------------------------------------------------------------------
# Fenster-Verwaltung
# ---------------------------------------------------------------------------

def get_foreground_window() -> int | None:
    """Gibt das aktuell aktive Fenster (HWND) zurueck."""
    try:
        hwnd = _user32.GetForegroundWindow()
        if hwnd and hwnd != 0:
            log.info("GetForegroundWindow: %s", hwnd)
            return hwnd
    except Exception:
        pass
    return None


def _find_claude_hwnd() -> int | None:
    """Sucht das Claude Desktop Fenster via EnumWindows."""
    result = []

    @ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.wintypes.HWND, ctypes.wintypes.LPARAM)
    def _callback(hwnd, _lparam):
        if _user32.IsWindowVisible(hwnd):
            length = _user32.GetWindowTextLengthW(hwnd)
            if length > 0:
                buf = ctypes.create_unicode_buffer(length + 1)
                _user32.GetWindowTextW(hwnd, buf, length + 1)
                if "claude" in buf.value.lower():
                    result.append(hwnd)
        return True

    _user32.EnumWindows(_callback, 0)
    if result:
        log.info("Claude-Fenster gefunden: HWND=%s", result[0])
        return result[0]
    return None


def _activate_window(hwnd: int) -> bool:
    """Aktiviert ein Fenster per SetForegroundWindow."""
    try:
        if _user32.IsIconic(hwnd):
            _user32.ShowWindow(hwnd, 9)  # SW_RESTORE
        result = _user32.SetForegroundWindow(hwnd)
        log.info("SetForegroundWindow(%s) = %s", hwnd, result)
        return bool(result)
    except Exception as exc:
        log.warning("_activate_window fehlgeschlagen: %s", exc)
        return False


def is_claude_running(settings: Settings) -> bool:
    """Prueft, ob die Claude Desktop App laeuft.

    Verwendet einen exakten Vergleich des Prozessnamens, damit
    Browser-Prozesse (z.B. Chrome mit einem Claude-Tab) nicht
    faelschlicherweise als Claude Desktop erkannt werden.
    """
    names = set(settings.claude_process_names)
    for proc in psutil.process_iter(["name"]):
        proc_name = (proc.info.get("name") or "").lower()
        if proc_name in names:
            return True
    return False


# ---------------------------------------------------------------------------
# Einfuegen / Leeren
# ---------------------------------------------------------------------------

def _activate_target(target_hwnd: int | None = None) -> bool:
    """Aktiviert das Zielfenster. Versucht HWND, dann Claude-Suche, dann Alt+Tab."""
    # 1. Gespeichertes HWND verwenden
    if target_hwnd:
        if _activate_window(target_hwnd):
            time.sleep(0.3)
            return True
        log.warning("Gespeichertes HWND %s konnte nicht aktiviert werden", target_hwnd)

    # 2. Claude-Fenster suchen
    claude_hwnd = _find_claude_hwnd()
    if claude_hwnd:
        if _activate_window(claude_hwnd):
            time.sleep(0.3)
            return True
        log.warning("Claude HWND %s konnte nicht aktiviert werden", claude_hwnd)

    # 3. Fallback: Alt+Tab
    log.info("Fallback: Alt+Tab")
    _send_key_combo(VK_MENU, VK_TAB)
    time.sleep(0.8)
    return True


def paste_text(text: str, target_hwnd: int | None = None, tk_root=None, **_kwargs) -> None:
    """Fuegt Text per Clipboard + Fensteraktivierung + Ctrl+V ein."""
    if not text.strip():
        return

    # Clipboard setzen
    clipboard_ok = _set_clipboard_text(text)

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

    # Zielfenster aktivieren
    _activate_target(target_hwnd)

    # Ctrl+V senden (PowerShell SendKeys fuer Electron-Kompatibilitaet)
    try:
        _send_keys_hidden("^v")
    except Exception as exc:
        log.warning("PowerShell Ctrl+V fehlgeschlagen, Fallback SendInput: %s", exc)
        _send_key_combo(VK_CONTROL, VK_V)
    time.sleep(0.3)

    log.info("Text eingefuegt (%d Zeichen)", len(text))


def clear_input(target_hwnd: int | None = None, tk_root=None, **_kwargs) -> None:
    """Leert das Eingabefeld (Ctrl+A, Backspace)."""
    _activate_target(target_hwnd)

    try:
        _send_keys_hidden("^a")
        time.sleep(0.05)
        _send_keys_hidden("{BACKSPACE}")
    except Exception:
        _send_key_combo(VK_CONTROL, VK_A)
        time.sleep(0.05)
        _send_key_combo(VK_BACK)
