"""Bindet Jarvis und Second Brain in die CLIs des Servers ein, wie an den Rechnern. Läuft beim Start jeder Sitzung, bei der Installation und mit jarvis-clis.timer.

Die Jarvis-Adresse enthält ein Geheimnis: Sie kommt nur aus ~/SK, nie ins Repo, in keine Meldung und auf keine Befehlszeile. Second Brain ist nur im Tunnel
erreichbar und braucht keine Anmeldung. Geschrieben wird nur, was fehlt oder sich geändert hat, denn laufende Sitzungen lesen und schreiben dieselben Dateien.
Eine unlesbare, leere oder ungültige Datei bleibt unberührt und wird gemeldet. OpenCode bekommt beide Server über opencode-setup; hier wird das nur geprüft.
"""
from __future__ import annotations

import json
import re
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


def server() -> dict[str, str]:
    """Bricht ab, wenn die Datei da, aber unlesbar ist. Fehlt sie oder taugt ihr Inhalt nicht, fehlt nur Jarvis; das meldet anbinden()."""
    jarvis = JARVIS_URL_DATEI.read_text("utf-8").strip() if JARVIS_URL_DATEI.exists() else ""
    return {**({"jarvis": jarvis} if GUELTIGE_URL.fullmatch(jarvis) else {}), "second-brain": SECOND_BRAIN_URL}


def _zeitstempel(pfad: Path) -> int | None:
    return pfad.stat().st_mtime_ns if pfad.exists() else None


def _schreib_wenn_unveraendert(pfad: Path, text: str, gelesen: int | None) -> None:
    """Schreibt die CLI die Datei gerade selbst, gewinnt sie; der nächste Lauf trägt nach."""
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
    """Direkt in die Datei von agy: `agy mcp add` hätte die geheime Adresse auf der Befehlszeile, sichtbar für jeden Prozess."""
    _json_eintragen(HOME / ".gemini" / "config" / "mcp_config.json", adressen, lambda url: {"disabled": False, "serverUrl": url})


def _codex_block_ersetzen(text: str, name: str, block: str) -> str:
    kopf = f"[mcp_servers.{name}]"
    zeilen = text.split("\n")
    start = next((i for i, z in enumerate(zeilen) if z.strip() == kopf), None)
    if start is None:
        return text.rstrip("\n") + "\n\n" + block
    ende = start + 1
    while ende < len(zeilen) and not (zeilen[ende].startswith("[") and not zeilen[ende].startswith(f"[mcp_servers.{name}.")):
        ende += 1
    return "\n".join(zeilen[:start] + block.rstrip("\n").split("\n") + [""] + zeilen[ende:])


def codex(adressen: dict[str, str]) -> None:
    konfig = launcher.ZUSTAND / "codex-home" / "config.toml"
    if not konfig.exists():
        launcher.codex_zuhause()  # Nur wenn nötig: Es baut auch Skill-Verknüpfungen neu, die laufende Sitzungen lesen.
    gelesen = _zeitstempel(konfig)
    text = konfig.read_text("utf-8")
    vorhanden = tomllib.loads(text).get("mcp_servers", {})
    neu = text
    for name, url in adressen.items():
        soll = {"startup_timeout_sec": CODEX_STARTZEIT_S, "tool_timeout_sec": CODEX_WERKZEUGZEIT_S, "url": url}
        if any(vorhanden.get(name, {}).get(k) != v for k, v in soll.items()):
            neu = _codex_block_ersetzen(neu, name, f"[mcp_servers.{name}]\n" + f"startup_timeout_sec = {CODEX_STARTZEIT_S}\ntool_timeout_sec = {CODEX_WERKZEUGZEIT_S}\nurl = \"{url}\"\n")
    if neu != text:
        tomllib.loads(neu)  # Lieber nichts schreiben als eine Datei, mit der Codex nicht mehr startet.
        _schreib_wenn_unveraendert(konfig, neu, gelesen)
    konfig.chmod(0o600)


def opencode(adressen: dict[str, str]) -> None:
    text = launcher._lies(HOME / ".config" / "opencode" / "opencode.jsonc")
    fehlt = [name for name in adressen if f'"{name}"' not in text]
    if fehlt:
        raise LookupError("fehlt in opencode.jsonc: " + ", ".join(fehlt))


def anbinden() -> list[str]:
    """Eine CLI hält die anderen nie auf. Meldungen nennen nur Schritt und Fehlerart, nie den Inhalt einer Datei."""
    fehler: list[str] = []
    try:
        adressen = server()
    except Exception as ausnahme:
        adressen = {"second-brain": SECOND_BRAIN_URL}
        fehler.append(f"Jarvis-Adresse: {type(ausnahme).__name__}")
    else:
        if "jarvis" not in adressen:
            fehler.append("Jarvis-Adresse fehlt oder ist ungültig: ~/SK/Jarvis/plugin-url.txt")
    schritte: list[tuple[str, Callable[[], None]]] = [(f"Claude {p}", lambda p=p: claude(p, adressen)) for p in launcher.PROFIL_IDS]
    schritte += [("Codex", lambda: codex(adressen)), ("Kimi", lambda: kimi(adressen)), ("Antigravity", lambda: antigravity(adressen)), ("OpenCode", lambda: opencode(adressen))]
    for name, schritt in schritte:
        try:
            schritt()
        except LookupError as ausnahme:
            fehler.append(f"{name}: {ausnahme}")
        except Exception as ausnahme:
            fehler.append(f"{name}: {type(ausnahme).__name__}")
    for meldung in fehler:
        print(f"MCP-Anbindung {meldung}", file=sys.stderr)
    return fehler


if __name__ == "__main__":
    sys.exit(1 if anbinden() else 0)
