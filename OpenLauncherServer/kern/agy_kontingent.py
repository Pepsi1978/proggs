"""Antigravity-Limits: führt `agy -p /usage` aus und liefert die Werte. Die Server-Konsole gibt sie über /api/antigravity-limits ans Handy.

Das Handy kann agy nicht selbst ausführen und die Seite gemini.google.com/usage zeigt ein anderes Kontingent. Jeder Aufruf ist ein kurzer
eigener Prozess, der nach der Ausgabe endet und keine Sitzung anlegt (geprüft am 31.10.2026 an den Ordnern conversations und brain).
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
from datetime import datetime
from pathlib import Path

import launcher

FENSTER = {"Woche": 7 * 86400, "Fuenf": 5 * 3600}
GRUPPEN = {"Gemini Models": "Gemini-Modelle", "Claude and GPT models": "Claude und GPT"}
ANSI = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]")


def agy_pfad() -> str:
    pfade = [str(Path.home() / p) for p in launcher.PFADE] + [os.environ.get("PATH", "")]
    return shutil.which("agy", path=os.pathsep.join(pfade)) or "agy"


def auswerten(ausgabe: str) -> list[dict]:
    """Eine Zeile je Limit, mit Tabulatoren: Gruppe, Art, Rest in Prozent, Zurücksetzen (UTC)."""
    limits = []
    for zeile in ANSI.sub("", ausgabe).splitlines():
        teile = [t.strip() for t in zeile.split("\t")]
        if len(teile) != 4 or not teile[2].endswith("%"):
            continue
        try:
            rest = float(teile[2].rstrip("%"))
            reset = int(datetime.fromisoformat(teile[3].replace("Z", "+00:00")).timestamp())
        except ValueError:
            continue
        art = teile[1].lower()
        fuenf = re.search(r"(five|5)[ -]?hour", art) is not None
        # Eine unbekannte Art nicht als Woche ausgeben: Name und Fenster wären falsch.
        if not fuenf and "week" not in art:
            continue
        name = f"{GRUPPEN.get(teile[0], teile[0])}: {'5-Stunden-Limit' if fuenf else 'Wochenlimit'}"
        limits.append({"name": name, "genutzt": round(100.0 - rest, 1), "fenster": FENSTER["Fuenf" if fuenf else "Woche"], "reset": reset})
    return limits


def lesen() -> list[dict]:
    # Ohne API-Schlüssel, wie beim Start der CLI: Es zählt das Abo.
    umgebung = {k: v for k, v in os.environ.items() if k != "GEMINI_API_KEY"}
    r = subprocess.run([agy_pfad(), "-p", "/usage"], stdin=subprocess.DEVNULL, capture_output=True, text=True, timeout=25, env=umgebung)
    limits = auswerten(r.stdout)
    if not limits:
        raise RuntimeError(f"agy nennt keine Limits (Code {r.returncode}): {(r.stdout + r.stderr).strip()[:200]}")
    return limits
