"""Bindet Jarvis und Second Brain in die CLIs des Servers ein, wie an den Rechnern. Läuft bei jeder Installation und mit jarvis-clis.timer.

Die Jarvis-Adresse enthält ein Geheimnis und kommt nur aus ~/SK, nie ins Repo. Second Brain ist nur im Tunnel erreichbar und braucht deshalb keine Anmeldung.
Geschrieben wird nur, was fehlt oder sich geändert hat: Laufende Sitzungen lesen dieselben Dateien. OpenCode bekommt beides schon über opencode-setup.
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import launcher  # noqa: E402

HOME = Path.home()
JARVIS_URL_DATEI = HOME / "SK" / "Jarvis" / "plugin-url.txt"
SECOND_BRAIN_URL = "http://10.8.0.1:8001/mcp"
CLAUDE_PROFILE = ("minimal", "standard", "strict")


def server() -> dict[str, str]:
    jarvis = JARVIS_URL_DATEI.read_text("utf-8").strip() if JARVIS_URL_DATEI.exists() else ""
    return {**({"jarvis": jarvis} if jarvis else {}), "second-brain": SECOND_BRAIN_URL}


def claude(adressen: dict[str, str]) -> None:
    for profil in CLAUDE_PROFILE:
        pfad = launcher.claude_ordner(profil) / ".claude.json"
        try:
            daten = json.loads(launcher._lies(pfad) or "{}")
        except ValueError:
            continue  # Gerade unlesbar: nie darüberschreiben, der nächste Lauf holt es nach.
        eintraege = daten.setdefault("mcpServers", {})
        neu = {n: {"type": "http", "url": u} for n, u in adressen.items() if eintraege.get(n, {}).get("url") != u}
        if neu:
            eintraege.update(neu)
            pfad.parent.mkdir(parents=True, exist_ok=True)
            launcher._schreib(pfad, json.dumps(daten, ensure_ascii=False, indent=2), 0o600)


def codex(adressen: dict[str, str]) -> None:
    konfig = launcher.codex_zuhause() / "config.toml"
    text = launcher._lies(konfig)
    fehlt = [n for n in adressen if f"[mcp_servers.{n}]" not in text]
    if fehlt:
        launcher._schreib(konfig, text.rstrip("\n") + "".join(f'\n\n[mcp_servers.{n}]\nurl = "{adressen[n]}"\n' for n in fehlt), 0o600)
    konfig.chmod(0o600)  # Die Jarvis-Adresse ist ein Geheimnis.


def kimi(adressen: dict[str, str]) -> None:
    pfad = HOME / ".kimi-code" / "mcp.json"
    try:
        daten = json.loads(launcher._lies(pfad) or "{}")
    except ValueError:
        return
    eintraege = daten.setdefault("mcpServers", {})
    neu = {n: {"transport": "http", "url": u, "enabled": True, "deferred": False, "startupTimeoutMs": 30000, "toolTimeoutMs": 120000}
           for n, u in adressen.items() if eintraege.get(n, {}).get("url") != u}
    if neu:
        eintraege.update(neu)
        pfad.parent.mkdir(parents=True, exist_ok=True)
        launcher._schreib(pfad, json.dumps(daten, ensure_ascii=False, indent=2), 0o600)


def antigravity(adressen: dict[str, str]) -> None:
    agy = shutil.which("agy") or str(HOME / ".agy" / "bin" / "agy")
    if not Path(agy).exists():
        return
    vorhanden = subprocess.run([agy, "mcp", "list"], capture_output=True, text=True, timeout=30, stdin=subprocess.DEVNULL).stdout
    for name, url in adressen.items():
        if name not in vorhanden:
            subprocess.run([agy, "mcp", "add", "--type", "http", name, url], capture_output=True, timeout=30, stdin=subprocess.DEVNULL)


def anbinden() -> None:
    adressen = server()
    for schritt in (claude, codex, kimi, antigravity):
        try:
            schritt(adressen)
        except Exception as fehler:
            # Eine CLI darf die anderen und den Update-Lauf nicht aufhalten. Die Fehlermeldung nennt nie die Adresse.
            print(f"MCP-Anbindung {schritt.__name__}: {type(fehler).__name__}", file=sys.stderr)


if __name__ == "__main__":
    anbinden()
