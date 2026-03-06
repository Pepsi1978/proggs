from __future__ import annotations

import logging
import subprocess
import time
from typing import Optional

import psutil

from config import Settings

log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# AppleScript-Ausfuehrung
# ---------------------------------------------------------------------------

def _run_applescript(script: str, timeout: int = 10) -> str:
    """Fuehrt ein AppleScript aus und gibt die Ausgabe zurueck."""
    try:
        result = subprocess.run(
            ["osascript", "-e", script],
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        if result.returncode != 0 and result.stderr.strip():
            log.warning("AppleScript Fehler: %s", result.stderr.strip())
        return result.stdout.strip()
    except subprocess.TimeoutExpired:
        log.warning("AppleScript Timeout nach %ds", timeout)
        return ""
    except Exception as exc:
        log.warning("AppleScript fehlgeschlagen: %s", exc)
        return ""


# ---------------------------------------------------------------------------
# Fenster-Verwaltung (macOS)
# ---------------------------------------------------------------------------

def get_foreground_app_name() -> str:
    """Gibt den Namen der aktuell aktiven App zurueck."""
    script = (
        'tell application "System Events" to get name of first '
        "application process whose frontmost is true"
    )
    return _run_applescript(script)


def is_target_app_frontmost(settings: Settings | None = None) -> bool:
    """Prueft, ob eine Ziel-App (Claude/Codex) im Vordergrund ist."""
    app_name = get_foreground_app_name().lower()
    if not app_name:
        return False

    target_names = ["claude", "codex"]
    if settings:
        target_names = settings.overlay_target_process_names

    for name in target_names:
        if name in app_name or app_name in name:
            return True
    return False


def is_claude_running(settings: Settings) -> bool:
    """Prueft, ob die Claude Desktop App oder Codex Desktop App laeuft.

    Verwendet einen Vergleich des Prozessnamens, damit
    Browser-Prozesse (z.B. Chrome mit einem Claude-Tab) nicht
    faelschlicherweise als Ziel-App erkannt werden.
    """
    names = set(settings.overlay_target_process_names)
    for proc in psutil.process_iter(["name"]):
        proc_name = (proc.info.get("name") or "").lower()
        for target in names:
            if target in proc_name or proc_name in target:
                return True
    return False


def _activate_claude(settings: Settings | None = None) -> bool:
    """Aktiviert das Claude-Fenster via AppleScript."""
    # Versuche verschiedene App-Namen
    app_names = ["Claude", "Claude Desktop", "Codex", "Codex Desktop"]
    if settings:
        app_names = [
            name.title() for name in settings.overlay_target_process_names
        ]

    for app_name in app_names:
        # Pruefe ob die App laeuft
        check_script = (
            'tell application "System Events" to '
            f'(name of processes) contains "{app_name}"'
        )
        result = _run_applescript(check_script)
        if result.lower() == "true":
            activate_script = f'tell application "{app_name}" to activate'
            _run_applescript(activate_script)
            time.sleep(0.3)
            log.info("App aktiviert: %s", app_name)
            return True

    log.warning("Keine Ziel-App gefunden zum Aktivieren")
    return False


# ---------------------------------------------------------------------------
# Clipboard (macOS)
# ---------------------------------------------------------------------------

def _set_clipboard_text(text: str) -> bool:
    """Setzt den Clipboard-Text via pbcopy."""
    try:
        process = subprocess.Popen(
            ["pbcopy"],
            stdin=subprocess.PIPE,
        )
        process.communicate(input=text.encode("utf-8"))
        if process.returncode == 0:
            log.info("Clipboard gesetzt: %d Zeichen", len(text))
            return True
        log.warning("pbcopy fehlgeschlagen (Code %d)", process.returncode)
        return False
    except Exception as exc:
        log.warning("Clipboard setzen fehlgeschlagen: %s", exc)
        return False


# ---------------------------------------------------------------------------
# Tastatur-Simulation (macOS via AppleScript)
# ---------------------------------------------------------------------------

def _send_keystroke(key: str, modifiers: str = "") -> None:
    """Sendet einen Tastendruck via AppleScript.

    Args:
        key: Der Tastendruck (z.B. "v", "a")
        modifiers: Modifikatoren (z.B. "command down", "command down, shift down")
    """
    if modifiers:
        script = (
            'tell application "System Events" to keystroke '
            f'"{key}" using {{{modifiers}}}'
        )
    else:
        script = (
            'tell application "System Events" to keystroke '
            f'"{key}"'
        )
    _run_applescript(script)


def _send_key_code(code: int, modifiers: str = "") -> None:
    """Sendet einen Key Code via AppleScript (fuer Sondertasten wie Backspace).

    Args:
        code: Der Key Code (z.B. 51 fuer Backspace/Delete)
        modifiers: Modifikatoren (z.B. "command down")
    """
    if modifiers:
        script = (
            'tell application "System Events" to key code '
            f'{code} using {{{modifiers}}}'
        )
    else:
        script = (
            'tell application "System Events" to key code '
            f'{code}'
        )
    _run_applescript(script)


# ---------------------------------------------------------------------------
# Einfuegen / Leeren
# ---------------------------------------------------------------------------

def paste_text(
    text: str,
    settings: Settings | None = None,
    tk_root=None,
    **_kwargs,
) -> None:
    """Fuegt Text per Clipboard + Cmd+V ein."""
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
    _activate_claude(settings)

    # Cmd+V senden
    _send_keystroke("v", "command down")
    time.sleep(0.3)

    log.info("Text eingefuegt (%d Zeichen)", len(text))


def clear_input(settings: Settings | None = None, **_kwargs) -> None:
    """Leert das Eingabefeld (Cmd+A, Backspace)."""
    _activate_claude(settings)

    # Cmd+A (alles auswaehlen)
    _send_keystroke("a", "command down")
    time.sleep(0.05)

    # Backspace (Key Code 51)
    _send_key_code(51)
