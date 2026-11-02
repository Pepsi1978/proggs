"""Bindet Jarvis und Second Brain in die CLIs des Servers ein, wie an den Rechnern. Läuft beim Start einer Sitzung für deren CLI, bei der Installation und mit jarvis-clis.timer für alle.

Die Jarvis-Adresse enthält ein Geheimnis: Sie kommt nur aus ~/SK, nie ins Repo, in keine Meldung und auf keine Befehlszeile. Second Brain ist nur im Tunnel
erreichbar und braucht keine Anmeldung. Geschrieben wird nur, was fehlt oder sich geändert hat, denn laufende Sitzungen lesen und schreiben dieselben Dateien.
Eine unlesbare, leere oder ungültige Datei bleibt unberührt und wird gemeldet (Reiter System über clis.py). OpenCode bekommt beide Server über opencode-setup; hier wird das nur geprüft.
"""
from __future__ import annotations

import json
import re
import shutil
import sys
import tomllib
from pathlib import Path
from typing import Callable

sys.path.insert(0, str(Path(__file__).resolve().parent))
import launcher  # noqa: E402

HOME = Path.home()
JARVIS_URL_DATEI = HOME / "SK" / "Jarvis" / "plugin-url.txt"
SECOND_BRAIN_URL = "http://10.8.0.1:8001/mcp"
GUELTIGE_URL = re.compile(r"https?://[^\s\"'\\]+")
CODEX_STARTZEIT_S = 30  # wie Jarvis/werkzeuge/plugin-adresse-setzen.sh an den Rechnern
CODEX_WERKZEUGZEIT_S = 120


class Fehlt(Exception):
    """Ein erwarteter Eintrag fehlt. Ihr Text ist selbst formuliert und darf in die Meldung; andere Ausnahmen könnten Dateiinhalte tragen."""


def server() -> dict[str, str]:
    """Bricht ab, wenn die Datei da, aber unlesbar ist. Fehlt sie oder taugt ihr Inhalt nicht, fehlt nur Jarvis; das meldet anbinden()."""
    jarvis = JARVIS_URL_DATEI.read_text("utf-8").strip() if JARVIS_URL_DATEI.exists() else ""
    return {**({"jarvis": jarvis} if GUELTIGE_URL.fullmatch(jarvis) else {}), "second-brain": SECOND_BRAIN_URL}


def _zeitstempel(pfad: Path) -> int | None:
    return pfad.stat().st_mtime_ns if pfad.exists() else None


def _schreib_wenn_unveraendert(pfad: Path, text: str, gelesen: int | None) -> None:
    """Hat die CLI die Datei seit dem Lesen geändert, gewinnt sie, und der nächste Lauf trägt nach. Das verkleinert das Fenster für einen Zusammenstoß, schließt es aber nicht ganz."""
    if _zeitstempel(pfad) == gelesen:
        launcher._schreib(pfad, text, 0o600)


def _json_eintragen(pfad: Path, adressen: dict[str, str], eintrag: Callable[[str], dict]) -> None:
    gelesen = _zeitstempel(pfad)
    daten = json.loads(pfad.read_text("utf-8")) if gelesen is not None else {}
    eintraege = daten.setdefault("mcpServers", {}) if isinstance(daten, dict) else None
    if not isinstance(eintraege, dict):
        raise ValueError("kein mcpServers-Objekt")
    aenderung = False
    for name, url in adressen.items():
        alt = eintraege.get(name) if isinstance(eintraege.get(name), dict) else {}
        neu = {**alt, **eintrag(url)}
        if neu != alt:
            eintraege[name] = neu
            aenderung = True
    if aenderung:
        _schreib_wenn_unveraendert(pfad, json.dumps(daten, ensure_ascii=False, indent=2), gelesen)
    if pfad.exists():
        pfad.chmod(0o600)


def claude(profil: str, adressen: dict[str, str]) -> None:
    _json_eintragen(launcher.claude_ordner(profil) / ".claude.json", adressen, lambda url: {"type": "http", "url": url})


def kimi(adressen: dict[str, str]) -> None:
    _json_eintragen(HOME / ".kimi-code" / "mcp.json", adressen,
                    lambda url: {"transport": "http", "url": url, "enabled": True, "deferred": False, "startupTimeoutMs": 30000, "toolTimeoutMs": 120000})


def antigravity(adressen: dict[str, str]) -> None:
    """Direkt in die Datei, die `agy mcp add` selbst schreibt und `agy mcp list` liest: Der Befehl hätte die geheime Adresse auf der Befehlszeile, sichtbar für jeden Prozess."""
    if shutil.which("agy", path=":".join(f"{HOME}/{p}" for p in launcher.PFADE)):
        _json_eintragen(HOME / ".gemini" / "config" / "mcp_config.json", adressen, lambda url: {"disabled": False, "serverUrl": url})


def _codex_block_setzen(text: str, name: str, soll: dict) -> str:
    """Ändert im Abschnitt nur die eigenen Schlüssel. Andere Schlüssel, Kommentare und Untertabellen wie Werkzeug-Freigaben bleiben stehen."""
    zeilen_soll = {k: f"{k} = {json.dumps(v)}" for k, v in soll.items()}
    kopf = re.compile(rf'^\s*\[\s*mcp_servers\s*\.\s*"?{re.escape(name)}"?\s*\]\s*(#.*)?$')
    zeilen = text.split("\n")
    start = next((i for i, z in enumerate(zeilen) if kopf.match(z)), None)
    if start is None:
        return text.rstrip("\n") + f"\n\n[mcp_servers.{name}]\n" + "\n".join(zeilen_soll.values()) + "\n"
    ende = start + 1
    while ende < len(zeilen) and not zeilen[ende].lstrip().startswith("["):
        ende += 1
    abschnitt = zeilen[start + 1:ende]
    for i, zeile in enumerate(abschnitt):
        schluessel = re.match(r"\s*([A-Za-z0-9_-]+)\s*=", zeile)
        if schluessel and schluessel[1] in zeilen_soll:
            abschnitt[i] = zeilen_soll.pop(schluessel[1])
    return "\n".join(zeilen[:start + 1] + list(zeilen_soll.values()) + abschnitt + zeilen[ende:])


def codex(adressen: dict[str, str]) -> None:
    konfig = launcher.CODEX_ZUHAUSE / "config.toml"
    if not konfig.exists():
        launcher.codex_zuhause()  # Nur wenn nötig: Es baut auch Skill-Verknüpfungen neu, die laufende Sitzungen lesen.
    gelesen = _zeitstempel(konfig)
    text = konfig.read_text("utf-8")
    vorhanden = tomllib.loads(text).get("mcp_servers", {})
    neu = text
    for name, url in adressen.items():
        soll = {"startup_timeout_sec": CODEX_STARTZEIT_S, "tool_timeout_sec": CODEX_WERKZEUGZEIT_S, "url": url}
        if any(vorhanden.get(name, {}).get(k) != v for k, v in soll.items()):
            neu = _codex_block_setzen(neu, name, soll)
    if neu != text:
        tomllib.loads(neu)  # Lieber nichts schreiben als eine Datei, mit der Codex nicht mehr startet.
        _schreib_wenn_unveraendert(konfig, neu, gelesen)
    konfig.chmod(0o600)


def opencode(adressen: dict[str, str]) -> None:
    text = launcher._lies(HOME / ".config" / "opencode" / "opencode.jsonc")
    fehlt = [name for name in adressen if f'"{name}"' not in text]
    if fehlt:
        raise Fehlt("fehlt in opencode.jsonc: " + ", ".join(fehlt))


def anbinden(cli: str | None = None, profil: str | None = None) -> list[str]:
    """Ohne Angabe alle CLIs, sonst nur die eine, die gerade startet. Eine CLI hält die anderen nie auf. Meldungen nennen nur Schritt und Fehlerart, nie den Inhalt einer Datei."""
    fehler: list[str] = []
    try:
        adressen = server()
    except Exception as ausnahme:
        adressen = {"second-brain": SECOND_BRAIN_URL}
        fehler.append(f"Jarvis-Adresse: {type(ausnahme).__name__}")
    else:
        if "jarvis" not in adressen:
            fehler.append("Jarvis-Adresse fehlt oder ist ungültig: ~/SK/Jarvis/plugin-url.txt")
    profile = (profil,) if profil else launcher.PROFIL_IDS
    schritte: dict[str, list[tuple[str, Callable[[], None]]]] = {
        "claude": [(f"Claude {p}", lambda p=p: claude(p, adressen)) for p in profile],
        "codex": [("Codex", lambda: codex(adressen))],
        "kimi": [("Kimi", lambda: kimi(adressen))],
        "antigravity": [("Antigravity", lambda: antigravity(adressen))],
        "opencode": [("OpenCode", lambda: opencode(adressen))],
    }
    for gruppe, liste in schritte.items():
        if cli and gruppe != cli:
            continue
        for name, schritt in liste:
            try:
                schritt()
            except Fehlt as ausnahme:
                fehler.append(f"{name}: {ausnahme}")
            except Exception as ausnahme:
                fehler.append(f"{name}: {type(ausnahme).__name__}")
    for meldung in fehler:
        print(f"MCP-Anbindung {meldung}", file=sys.stderr)
    return fehler


if __name__ == "__main__":
    sys.exit(1 if anbinden() else 0)
