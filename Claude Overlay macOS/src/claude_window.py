from __future__ import annotations

import logging
import subprocess
import time
from typing import Optional

from config import Settings

log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# AppleScript-Helfer
# ---------------------------------------------------------------------------

def _run_applescript(script: str, timeout: int = 5) -> Optional[str]:
    """Fuehrt ein AppleScript aus und gibt die Ausgabe zurueck."""
    try:
        result = subprocess.run(
            ["osascript", "-e", script],
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        if result.returncode == 0:
            return result.stdout.strip()
        log.warning("AppleScript Fehler: %s", result.stderr.strip())
        return None
    except subprocess.TimeoutExpired:
        log.warning("AppleScript Timeout")
        return None
    except Exception as exc:
        log.warning("AppleScript Fehler: %s", exc)
        return None


# ---------------------------------------------------------------------------
# Prozess-Erkennung
# ---------------------------------------------------------------------------

_CLAUDE_BUNDLE_ID = "com.anthropic.claudefordesktop"


def is_claude_running(settings: Settings) -> bool:
    """Prueft, ob die Claude Desktop App laeuft.

    Verwendet den Bundle-Identifier (com.anthropic.claudefordesktop), um
    ausschliesslich die native Claude Desktop App zu erkennen.
    Chrome-Tabs, PWAs oder andere Prozesse mit 'Claude' im Namen
    werden zuverlaessig ignoriert.
    """
    # 1. Primaer: AppleScript – holt alle Bundle-IDs und prueft ob unsere dabei ist
    check_script = (
        'tell application "System Events" to '
        '(bundle identifier of every application process) '
        f'contains "{_CLAUDE_BUNDLE_ID}"'
    )
    result = _run_applescript(check_script, timeout=3)
    if result == "true":
        return True

    # 2. Fallback: lsappinfo (listet laufende Apps mit Bundle-ID)
    try:
        proc = subprocess.run(
            ["lsappinfo", "find", f"bundleid={_CLAUDE_BUNDLE_ID}"],
            capture_output=True,
            text=True,
            timeout=3,
        )
        if proc.returncode == 0 and proc.stdout.strip() and "ASN:" in proc.stdout:
            return True
    except Exception:
        pass

    return False


# ---------------------------------------------------------------------------
# Fenster-Verwaltung (macOS via AppleScript)
# ---------------------------------------------------------------------------

def get_frontmost_app() -> Optional[str]:
    """Gibt den Namen der aktuell aktiven App zurueck."""
    script = (
        'tell application "System Events" to get name of first '
        'application process whose frontmost is true'
    )
    return _run_applescript(script)


def _activate_claude() -> bool:
    """Aktiviert das Claude-Fenster via AppleScript."""
    script = 'tell application "Claude" to activate'
    result = _run_applescript(script, timeout=3)
    if result is not None:
        time.sleep(0.3)
        return True
    return False


def _find_and_activate_claude() -> bool:
    """Versucht Claude Desktop zu finden und zu aktivieren.

    Nutzt den Bundle-Identifier, damit nur die native App erkannt wird.
    """
    check_script = (
        'tell application "System Events" to '
        '(bundle identifier of every application process) '
        f'contains "{_CLAUDE_BUNDLE_ID}"'
    )
    result = _run_applescript(check_script)
    if result == "true":
        return _activate_claude()
    return False


# ---------------------------------------------------------------------------
# Clipboard (macOS: pbcopy/pbpaste)
# ---------------------------------------------------------------------------

def _set_clipboard_text(text: str) -> bool:
    """Setzt den Clipboard-Text via pbcopy."""
    try:
        process = subprocess.Popen(
            ["pbcopy"],
            stdin=subprocess.PIPE,
        )
        process.communicate(text.encode("utf-8"), timeout=3)
        if process.returncode == 0:
            log.info("Clipboard gesetzt: %d Zeichen", len(text))
            return True
        log.warning("pbcopy fehlgeschlagen: returncode=%d", process.returncode)
        return False
    except Exception as exc:
        log.warning("pbcopy Fehler: %s", exc)
        return False


# ---------------------------------------------------------------------------
# Tastatursimulation (macOS via AppleScript + System Events)
# ---------------------------------------------------------------------------

def _send_keystroke(key: str, using: str = "command down") -> bool:
    """Sendet einen Tastendruck via AppleScript System Events."""
    script = (
        f'tell application "System Events" to keystroke "{key}" using {using}'
    )
    result = _run_applescript(script, timeout=3)
    return result is not None


def _send_key_code(code: int, using: str = "") -> bool:
    """Sendet einen Key-Code via AppleScript System Events."""
    if using:
        script = (
            f'tell application "System Events" to key code {code} using {using}'
        )
    else:
        script = (
            f'tell application "System Events" to key code {code}'
        )
    result = _run_applescript(script, timeout=3)
    return result is not None


# ---------------------------------------------------------------------------
# Einfuegen / Leeren
# ---------------------------------------------------------------------------

def _activate_target(target_app: str | None = None) -> bool:
    """Aktiviert das Zielfenster. Versucht gespeicherte App, dann Claude-Suche."""
    # 1. Gespeicherte App verwenden
    if target_app and target_app.lower() != "python":
        script = f'tell application "{target_app}" to activate'
        result = _run_applescript(script, timeout=3)
        if result is not None:
            time.sleep(0.3)
            return True
        log.warning("App '%s' konnte nicht aktiviert werden", target_app)

    # 2. Claude direkt aktivieren
    if _find_and_activate_claude():
        return True

    log.warning("Kein Zielfenster gefunden")
    return False


def _verify_clipboard(expected: str) -> bool:
    """Prueft ob der Clipboard-Inhalt dem erwarteten Text entspricht."""
    try:
        result = subprocess.run(
            ["pbpaste"],
            capture_output=True,
            text=True,
            timeout=3,
        )
        if result.returncode == 0:
            return result.stdout == expected
    except Exception:
        pass
    return False


def paste_text(text: str, target_app: str | None = None, tk_root=None, **_kwargs) -> None:
    """Fuegt Text per Clipboard + Fensteraktivierung + Cmd+V ein."""
    if not text.strip():
        return

    # Clipboard setzen (mit Retry)
    clipboard_ok = False
    for attempt in range(3):
        if _set_clipboard_text(text) and _verify_clipboard(text):
            clipboard_ok = True
            break
        log.warning("Clipboard-Versuch %d fehlgeschlagen, wiederhole...", attempt + 1)
        time.sleep(0.1)

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

    # Zielfenster aktivieren (mit Retry)
    activated = False
    for attempt in range(3):
        if _activate_target(target_app):
            activated = True
            break
        log.warning("Aktivierung Versuch %d fehlgeschlagen", attempt + 1)
        time.sleep(0.3)

    if not activated:
        log.error("Zielfenster konnte nicht aktiviert werden – Text im Clipboard")
        return

    # Kurz warten bis Fenster bereit ist
    time.sleep(0.15)

    # Cmd+V senden (mit Retry)
    for attempt in range(2):
        if _send_keystroke("v", "command down"):
            break
        log.warning("Cmd+V Versuch %d fehlgeschlagen, wiederhole...", attempt + 1)
        time.sleep(0.3)

    time.sleep(0.2)
    log.info("Text eingefuegt (%d Zeichen)", len(text))


def clear_input(target_app: str | None = None, **_kwargs) -> None:
    """Leert das Eingabefeld (Cmd+A, Backspace)."""
    _activate_target(target_app)

    # Cmd+A (alles auswaehlen)
    _send_keystroke("a", "command down")
    time.sleep(0.05)
    # Backspace (Key Code 51)
    _send_key_code(51)
