"""CLIs auf dem Hostinger-Server: Versionen lesen, aktualisieren, Anmeldestände prüfen. Läuft alle 3 Stunden (jarvis-clis.timer).

Eine CLI, die gerade läuft, wird nicht angefasst; sie kommt beim nächsten Lauf dran. Ergebnis: ~/.openlauncher/cli-status.json,
angezeigt im Reiter System der Konsole. Update-Wege wie in UpdaterZentrale-macOS/programs.json.
"""
from __future__ import annotations

import json
import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import launcher  # noqa: E402

HOME = Path.home()
STATUS = launcher.ZUSTAND / "cli-status.json"
UMGEBUNG = dict(os.environ, PATH=":".join([f"{HOME}/{p}" for p in launcher.PFADE] + [os.environ.get("PATH", "")]),
                CLAUDE_CONFIG_DIR=str(launcher.claude_ordner("minimal")), CODEX_HOME=str(launcher.ZUSTAND / "codex-home"), NPM_CONFIG_PREFIX=f"{HOME}/.local")
NPM = "npm install -g"
PROGRAMME = {
    "claude": {"name": "Claude Code", "befehl": "claude", "update": "claude update", "anmeldung": "claude auth login",
               "stand": "claude auth status --json", "installieren": "curl -fsSL https://claude.ai/install.sh | bash"},
    "codex": {"name": "Codex CLI", "befehl": "codex", "update": f"{NPM} @openai/codex@latest", "anmeldung": "codex login --device-auth",
              "stand": "codex login status", "installieren": f"{NPM} @openai/codex@latest"},
    "opencode": {"name": "OpenCode", "befehl": "opencode", "update": f"{NPM} opencode-ai@latest", "anmeldung": "opencode auth login",
                 "stand": "opencode auth list", "installieren": f"{NPM} opencode-ai@latest"},
    "kimi": {"name": "Kimi Code", "befehl": "kimi", "update": f"kimi update --yes || {NPM} --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest",
             "anmeldung": "kimi login", "stand": "", "installieren": f"{NPM} --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest"},
    "antigravity": {"name": "Antigravity CLI", "befehl": "agy", "update": "agy update", "anmeldung": "agy", "stand": "",
                    "installieren": "curl -fsSL https://antigravity.google/cli/install.sh | bash"},
    "chrome-devtools-mcp": {"name": "Browser-Steuerung", "befehl": "chrome-devtools-mcp", "update": f"{NPM} chrome-devtools-mcp@latest", "anmeldung": "", "stand": "true",
                            "installieren": f"{NPM} chrome-devtools-mcp@latest"},
    # gh kommt aus dem Paketarchiv (jarvis-powershell.timer); hier zählen Version und Anmeldung.
    "gh": {"name": "GitHub CLI", "befehl": "gh", "update": "true", "anmeldung": "gh auth login -h github.com -p https -w", "stand": "gh auth status", "installieren": "true"},
}


def _lauf(befehl: str, sekunden: int = 300) -> tuple[int, str]:
    """Eigene Prozessgruppe: Sonst hielte ein hängender Enkel (curl | bash, npm) die Ausgabe offen, und das Zeitlimit griffe nicht."""
    p = subprocess.Popen(["bash", "-c", befehl], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=UMGEBUNG,
                         stdin=subprocess.DEVNULL, start_new_session=True)
    try:
        aus, _ = p.communicate(timeout=sekunden)
        return p.returncode, (aus or "").strip()
    except subprocess.TimeoutExpired:
        os.killpg(p.pid, signal.SIGKILL)
        p.communicate()
        return 124, "Zeitlimit überschritten"


def version(p: dict) -> str:
    code, aus = _lauf(f"{p['befehl']} --version", 30)
    if code != 0:
        return ""
    m = re.search(r"\d+\.\d+[\w.\-]*", aus)
    return m.group(0) if m else aus[:40]


def in_benutzung(schluessel: str, p: dict, alle: list) -> bool:
    """Eine CLI nie unter einer laufenden Sitzung ersetzen. Die Sitzungen nennen ihr CLI; der Prozesstest findet auch die in einem freien Terminal."""
    if any(s.get("cli") == schluessel and s["stand"] != "beendet" for s in alle):
        return True
    muster = {"codex": "@openai/codex", "opencode": "opencode-ai", "kimi": "@moonshot-ai/kimi-code"}.get(schluessel)
    code, _ = _lauf(f"pgrep -u \"$(id -u)\" -f '(^|/){p['befehl']}( |$)" + (f"|{muster}/" if muster else "") + "' >/dev/null", 10)
    return code == 0


def anmeldung(schluessel: str, p: dict) -> str:
    if schluessel == "claude":
        return launcher.claude_anmeldung()
    if p["stand"]:
        code, aus = _lauf(p["stand"], 20)
        if code == 0 and not re.search(r"not logged in|no credentials|0 credentials", aus, re.I):
            return "angemeldet"
        return "abgemeldet"
    pfade = {"kimi": HOME / ".kimi-code", "antigravity": HOME / ".agy"}
    return "unbekannt" if pfade.get(schluessel, HOME / "-").exists() else "abgemeldet"


def status_schreiben(aktualisieren: bool) -> dict:
    alt = {}
    try:
        alt = json.loads(STATUS.read_text("utf-8")).get("programme", {})
    except (OSError, ValueError):
        pass
    programme = {}
    alle = launcher.sitzungen()
    for schluessel, p in PROGRAMME.items():
        vorher = version(p)
        eintrag = {"name": p["name"], "version": vorher, "zuletzt": alt.get(schluessel, {}).get("zuletzt", ""), "meldung": alt.get(schluessel, {}).get("meldung", "")}
        if aktualisieren:
            jetzt = time.strftime("%d.%m.%Y %H:%M")
            if in_benutzung(schluessel, p, alle):
                eintrag["meldung"] = f"{jetzt}: läuft gerade, beim nächsten Lauf"
            else:
                code, aus = _lauf(p["update"] if vorher else p["installieren"])
                eintrag["version"] = version(p)
                eintrag["zuletzt"] = jetzt
                if code != 0:
                    eintrag["meldung"] = f"{jetzt}: fehlgeschlagen: {aus[-200:]}"
                elif eintrag["version"] != vorher:
                    eintrag["meldung"] = f"{jetzt}: aktualisiert von {vorher or 'nichts'} auf {eintrag['version']}"
                else:
                    eintrag["meldung"] = f"{jetzt}: aktuell"
        eintrag["anmeldung"] = anmeldung(schluessel, p) if eintrag["version"] else "fehlt"
        programme[schluessel] = eintrag
    code, pwsh = _lauf("pwsh --version", 20)
    daten = {"programme": programme, "powershell": pwsh if code == 0 else "", "geprueft": time.strftime("%d.%m.%Y %H:%M")}
    launcher._schreib(STATUS, json.dumps(daten, ensure_ascii=False, indent=1))
    return daten


if __name__ == "__main__":
    os.environ["TZ"] = "Europe/Berlin"
    time.tzset()
    print(json.dumps(status_schreiben("aktualisieren" in sys.argv), ensure_ascii=False, indent=1))
