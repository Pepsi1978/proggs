"""CLIs auf dem Hostinger-Server: Versionen lesen, aktualisieren, Anmeldestände prüfen. Täglich um 4:30 Uhr Berliner Zeit wird alles aktualisiert; stündlich holt der Timer nach, was übersprungen wurde oder scheiterte (jarvis-clis.timer).

Eine CLI, die gerade läuft, wird nicht angefasst; sie kommt beim nächsten Lauf dran (Merker „offen“). Ergebnis: ~/.openlauncher/cli-status.json,
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
KERN = Path(__file__).resolve().parent
STATUS = launcher.ZUSTAND / "cli-status.json"
UMGEBUNG = dict(os.environ, PATH=":".join([f"{HOME}/{p}" for p in launcher.PFADE] + [os.environ.get("PATH", "")]),
                CLAUDE_CONFIG_DIR=str(launcher.claude_ordner("minimal")), CODEX_HOME=str(launcher.ZUSTAND / "codex-home"), NPM_CONFIG_PREFIX=f"{HOME}/.local")
NPM = "npm install -g"


def npm_update(paket: str) -> str:
    """Nur bei neuer Version installieren: Ein Lauf ohne Änderung kostet so kaum etwas, und das Zeitlimit trifft npm nicht mitten im Austausch."""
    return (f"""[ "$(npm ls -g {paket} --depth=0 --json 2>/dev/null | jq -r '.dependencies["{paket}"].version // empty')" = "$(npm view {paket} version 2>/dev/null)" ] """
            f"|| {NPM} {paket}@latest")


PROGRAMME = {
    # Node zuerst: Die npm-Programme danach laufen so schon mit der neuen Fassung.
    "node": {"name": "Node.js (LTS)", "befehl": "node", "anmeldung": "", "stand": "", "update": f"bash {KERN}/node_aktualisieren.sh", "installieren": f"bash {KERN}/node_aktualisieren.sh"},
    "claude": {"name": "Claude Code", "befehl": "claude", "update": "claude update", "anmeldung": "claude auth login",
               "stand": "claude auth status --json", "installieren": "curl -fsSL https://claude.ai/install.sh | bash"},
    "codex": {"name": "Codex CLI", "befehl": "codex", "update": npm_update("@openai/codex"), "anmeldung": "codex login --device-auth",
              "stand": "codex login status", "installieren": f"{NPM} @openai/codex@latest"},
    "opencode": {"name": "OpenCode", "befehl": "opencode", "update": npm_update("opencode-ai"), "anmeldung": "opencode auth login",
                 "stand": "opencode auth list", "installieren": f"{NPM} opencode-ai@latest"},
    "kimi": {"name": "Kimi Code", "befehl": "kimi", "update": f"kimi update --yes || {NPM} --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest",
             "anmeldung": "kimi login", "stand": "", "installieren": f"{NPM} --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest"},
    "antigravity": {"name": "Antigravity CLI", "befehl": "agy", "update": "agy update", "anmeldung": "agy", "stand": "",
                    "installieren": "curl -fsSL https://antigravity.google/cli/install.sh | bash"},
    "chrome-devtools-mcp": {"name": "Browser-Steuerung", "befehl": "chrome-devtools-mcp", "update": npm_update("chrome-devtools-mcp"), "anmeldung": "", "stand": "",
                            "installieren": f"{NPM} chrome-devtools-mcp@latest"},
    # gh kommt aus dem Paketarchiv (jarvis-powershell.timer); hier zählen Version und Anmeldung.
    "gh": {"name": "GitHub CLI", "befehl": "gh", "update": "true", "anmeldung": "gh auth login -h github.com -p https -w", "stand": "gh auth token", "installieren": "true"},
    # Werkzeuge wie in UpdateZentrale/programs.json. Die Firebase-Anmeldung bleibt Franks Entscheidung, darum hier kein Anmelde-Knopf.
    "pnpm": {"name": "pnpm", "befehl": "pnpm", "update": npm_update("pnpm"), "anmeldung": "", "stand": "", "installieren": f"{NPM} pnpm@latest"},
    # Nicht neben einem Gradle-Bau: Er liest die Werkzeuge, die hier ersetzt würden. build-tools 36.0.0 (apksigner) bleibt daneben liegen.
    "android-sdk": {"name": "Android SDK", "befehl": "sdkmanager", "sekunden": 900, "anmeldung": "", "stand": "", "installieren": "true",
                    "update": "if pgrep -f '[G]radleDaemon' >/dev/null; then echo 'Gradle-Bau läuft'; exit 1; fi; yes | sdkmanager --licenses >/dev/null 2>&1; sdkmanager --update"},
    "trivy": {"name": "Trivy", "befehl": "trivy", "update": f"bash {KERN}/werkzeug_aktualisieren.sh trivy", "anmeldung": "", "stand": "", "installieren": f"bash {KERN}/werkzeug_aktualisieren.sh trivy"},
    "lazygit": {"name": "lazygit", "befehl": "lazygit", "update": f"bash {KERN}/werkzeug_aktualisieren.sh lazygit", "anmeldung": "", "stand": "", "installieren": f"bash {KERN}/werkzeug_aktualisieren.sh lazygit"},
    "rclone": {"name": "Rclone", "befehl": "rclone", "update": f"bash {KERN}/werkzeug_aktualisieren.sh rclone", "anmeldung": "", "stand": "", "installieren": f"bash {KERN}/werkzeug_aktualisieren.sh rclone"},
    "bun": {"name": "Bun", "befehl": "bun", "update": "bun upgrade", "anmeldung": "", "stand": "",
            "installieren": "curl -fsSL https://bun.com/install | SHELL=/bin/sh BUN_INSTALL=$HOME/.local bash"},
    "deno": {"name": "Deno", "befehl": "deno", "update": "deno upgrade", "anmeldung": "", "stand": "",
             "installieren": "curl -fsSL https://deno.land/install.sh | DENO_INSTALL=$HOME/.local sh -s -- --yes --no-modify-path"},
    # CMake und Ninja kommen aus dem Paketarchiv (jarvis-powershell.timer); hier zählt die Version.
    "cmake": {"name": "CMake", "befehl": "cmake", "update": "true", "anmeldung": "", "stand": "", "installieren": "true"},
    "ninja": {"name": "Ninja", "befehl": "ninja", "update": "true", "anmeldung": "", "stand": "", "installieren": "true"},
    "uv": {"name": "uv", "befehl": "uv", "update": "uv self update", "anmeldung": "", "stand": "", "installieren": "curl -LsSf https://astral.sh/uv/install.sh | UV_NO_MODIFY_PATH=1 sh"},
    "typescript": {"name": "TypeScript", "befehl": "tsc", "update": npm_update("typescript"), "anmeldung": "", "stand": "", "installieren": f"{NPM} typescript@latest"},
    "tsx": {"name": "tsx", "befehl": "tsx", "update": npm_update("tsx"), "anmeldung": "", "stand": "", "installieren": f"{NPM} tsx@latest"},
    "biome": {"name": "Biome", "befehl": "biome", "update": npm_update("@biomejs/biome"), "anmeldung": "", "stand": "", "installieren": f"{NPM} @biomejs/biome@latest"},
    "firebase": {"name": "Firebase CLI", "befehl": "firebase", "update": npm_update("firebase-tools"), "sekunden": 900, "anmeldung": "", "stand": "", "installieren": f"{NPM} firebase-tools@latest"},
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
    # Klammern: Der Text steht auch in der Befehlszeile des bash -c um pgrep. Ohne sie fände sich die Prüfung selbst, und die CLI gälte immer als laufend.
    muster = {"codex": "[@]openai/codex", "opencode": "[o]pencode-ai", "kimi": "[@]moonshot-ai/kimi-code"}.get(schluessel)
    code, _ = _lauf(f"pgrep -u \"$(id -u)\" -f '(^|/){p['befehl']}( |$)" + (f"|{muster}/" if muster else "") + "' >/dev/null", 10)
    return code == 0


def anmeldung(schluessel: str, p: dict) -> str:
    if not p["anmeldung"]:
        return "keine"
    if schluessel == "claude":
        return launcher.claude_anmeldung()
    if p["stand"]:
        code, aus = _lauf(p["stand"], 20)
        if code == 0 and not re.search(r"not logged in|no credentials|0 credentials", aus, re.I):
            return "angemeldet"
        return "abgemeldet"
    pfade = {"kimi": HOME / ".kimi-code", "antigravity": HOME / ".agy"}
    return "unbekannt" if pfade[schluessel].exists() else "abgemeldet"


def _stand() -> dict:
    try:
        return json.loads(STATUS.read_text("utf-8"))
    except (OSError, ValueError):
        return {}


def status_schreiben(aktualisieren: bool, nur_offene: bool = False, tageslauf: str = "") -> dict:
    stand = _stand()
    alt = stand.get("programme", {})
    programme = {}
    alle = launcher.sitzungen()
    for schluessel, p in PROGRAMME.items():
        # Beim Auffrischen des Reiters System reicht für Werkzeuge ohne Anmeldung der letzte Stand: Jede Abfrage startet einen Prozess, auch neben einem Android-Bau.
        anstehend = aktualisieren and (not nur_offene or alt.get(schluessel, {}).get("offen", False))
        vorher = alt.get(schluessel, {}).get("version", "") if not anstehend and not p["anmeldung"] else ""
        vorher = vorher or version(p)
        eintrag = {"name": p["name"], "version": vorher, "zuletzt": alt.get(schluessel, {}).get("zuletzt", ""), "meldung": alt.get(schluessel, {}).get("meldung", ""), "offen": alt.get(schluessel, {}).get("offen", False)}
        if anstehend:
            jetzt = time.strftime("%d.%m.%Y %H:%M")
            if in_benutzung(schluessel, p, alle):
                eintrag["meldung"] = f"{jetzt}: läuft gerade, beim nächsten Lauf"
                eintrag["offen"] = True
            else:
                code, aus = _lauf(p["update"] if vorher else p["installieren"], p.get("sekunden", 300))
                eintrag["version"] = version(p)
                eintrag["zuletzt"] = jetzt
                eintrag["offen"] = code != 0 or not eintrag["version"]
                if code != 0:
                    eintrag["meldung"] = f"{jetzt}: fehlgeschlagen: {aus[-200:]}"
                elif not eintrag["version"]:
                    eintrag["meldung"] = f"{jetzt}: fehlgeschlagen: nicht installiert"
                elif eintrag["version"] != vorher:
                    eintrag["meldung"] = f"{jetzt}: aktualisiert von {vorher or 'nichts'} auf {eintrag['version']}"
                else:
                    eintrag["meldung"] = f"{jetzt}: aktuell"
        eintrag["anmeldung"] = anmeldung(schluessel, p) if eintrag["version"] else "fehlt"
        eintrag["anmeldbar"] = bool(p["anmeldung"])
        programme[schluessel] = eintrag
    code, pwsh = _lauf("pwsh --version", 20)
    daten = {"programme": programme, "powershell": pwsh if code == 0 else "", "geprueft": time.strftime("%d.%m.%Y %H:%M"), "tageslauf": tageslauf or stand.get("tageslauf", "")}
    launcher._schreib(STATUS, json.dumps(daten, ensure_ascii=False, indent=1))
    return daten


def tageslauf() -> None:
    """Der erste Lauf eines Tages ab 4:30 Uhr aktualisiert alles, auch wenn der Server um 4:30 Uhr aus war. Danach holt jede Stunde nur das Offene nach."""
    stand = _stand()
    heute = time.strftime("%Y-%m-%d")
    if stand.get("tageslauf") != heute and time.strftime("%H:%M") >= "04:30":
        status_schreiben(True, tageslauf=heute)
    elif any(e.get("offen") for e in stand.get("programme", {}).values()):
        status_schreiben(True, nur_offene=True)


if __name__ == "__main__":
    os.environ["TZ"] = "Europe/Berlin"
    time.tzset()
    # Vor den Updates: Ein Abbruch oder das Dienst-Zeitlimit darf die Anbindung nicht mitreißen.
    import mcp_anbindung
    mcp_anbindung.anbinden()
    if "taeglich" in sys.argv:
        tageslauf()
    else:
        print(json.dumps(status_schreiben("aktualisieren" in sys.argv), ensure_ascii=False, indent=1))
