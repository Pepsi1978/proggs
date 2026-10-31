"""Antigravity-Limits fürs Handy: führt alle drei Stunden `agy -p /usage` aus und meldet die Werte per MCP (`kontingente_melden`).

Das Handy kann agy nicht selbst ausführen und die Seite gemini.google.com/usage zeigt ein anderes Kontingent. Hauptquelle ist deshalb
der Server (läuft rund um die Uhr), der Mac springt ein, solange der Server nichts meldet. Jeder Aufruf ist ein kurzer eigener Prozess,
der nach der Ausgabe endet und keine Sitzung anlegt (geprüft am 31.10.2026 an den Ordnern conversations und brain).
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import time
import urllib.request
from datetime import datetime
from pathlib import Path

TAKT_S = 3 * 3600
NACH_FEHLER_S = 30 * 60
FENSTER = {"Woche": 7 * 86400, "Fuenf": 5 * 3600}
GRUPPEN = {"Gemini Models": "Gemini-Modelle", "Claude and GPT models": "Claude und GPT"}
ANSI = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]")


def agy_pfad() -> str:
    for p in (Path.home() / ".local/bin/agy", Path.home() / ".agy/bin/agy"):
        if p.exists():
            return str(p)
    return "agy"


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
        fuenf = "five hour" in teile[1].lower()
        name = f"{GRUPPEN.get(teile[0], teile[0])}: {'5-Stunden-Limit' if fuenf else 'Wochenlimit'}"
        limits.append({"name": name, "genutzt": round(100.0 - rest, 1), "fenster": FENSTER["Fuenf" if fuenf else "Woche"], "reset": reset})
    return limits


def lesen() -> list[dict]:
    # Ohne API-Schlüssel, wie beim Start der CLI: Es zählt das Abo.
    umgebung = {k: v for k, v in os.environ.items() if k != "GEMINI_API_KEY"}
    r = subprocess.run([agy_pfad(), "-p", "/usage"], stdin=subprocess.DEVNULL, capture_output=True, text=True, timeout=90, env=umgebung)
    limits = auswerten(r.stdout)
    if not limits:
        raise RuntimeError(f"agy nennt keine Limits (Code {r.returncode}): {(r.stdout + r.stderr).strip()[:200]}")
    return limits


def melden(quelle: str, limits: list[dict]) -> None:
    adresse = (Path.home() / "SK" / "Jarvis" / "plugin-url.txt").read_text(encoding="utf-8").strip()
    rumpf = {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "kontingente_melden", "arguments": {
        "quelle": quelle, "limits": json.dumps(limits, ensure_ascii=False)}}}
    anfrage = urllib.request.Request(adresse, data=json.dumps(rumpf).encode("utf-8"), method="POST", headers={
        "Content-Type": "application/json", "Accept": "application/json, text/event-stream", "User-Agent": f"JarvisBruecke/{quelle}"})
    with urllib.request.urlopen(anfrage, timeout=85) as antwort:
        text = antwort.read().decode("utf-8", "replace")
    if '"isError":true' in text.replace(" ", "") or '"error"' in text:
        raise RuntimeError(f"Das Handy lehnt die Meldung ab: {text[:200]}")


def faden(quelle: str, schreib) -> None:
    """Endlosschleife für einen eigenen Faden der Brücke. Fehler kommen einmal je Wortlaut ins Protokoll."""
    letzter = ""
    while True:
        try:
            melden(quelle, lesen())
            if letzter != "ok":
                schreib(f"Antigravity-Limits gemeldet ({quelle})")
            letzter, pause = "ok", TAKT_S
        except Exception as fehler:
            if str(fehler) != letzter:
                schreib(f"Antigravity-Limits nicht gemeldet: {fehler}")
            letzter, pause = str(fehler), NACH_FEHLER_S
        time.sleep(pause)
