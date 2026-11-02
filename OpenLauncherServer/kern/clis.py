"""Programmier-Software auf dem Hostinger-Server: Versionen lesen, aktualisieren, Anmeldestände prüfen. Täglich um 4:30 Uhr Berliner Zeit wird alles aktualisiert;
stündlich holt der Timer nach, was übersprungen wurde oder scheiterte (jarvis-clis.timer, Merker „offen“).

Eine CLI, die gerade läuft, wird nicht angefasst. Ergebnis: ~/.openlauncher/cli-status.json, angezeigt im Reiter System der Konsole; jedes Programm wird sofort
gespeichert, damit ein Abbruch nichts verliert. Update-Wege wie in UpdaterZentrale-macOS/programs.json. Aufrufe: taeglich (Timer), aktualisieren (Knopf), fehlende
(installieren.sh), ohne Angabe nur der Status.
"""
from __future__ import annotations

import fcntl
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from contextlib import contextmanager
from pathlib import Path
from typing import Callable, Iterator

sys.path.insert(0, str(Path(__file__).resolve().parent))
import launcher  # noqa: E402

HOME = Path.home()
KERN = Path(__file__).resolve().parent
STATUS = launcher.ZUSTAND / "cli-status.json"
STATUS_SPERRE = launcher.ZUSTAND / "cli-status.sperre"
LAUF_SPERRE = launcher.ZUSTAND / "cli-update.sperre"
PAKETE_WARTEN_S = 1800
UMGEBUNG = dict(os.environ, PATH=":".join([f"{HOME}/{p}" for p in launcher.PFADE] + [os.environ.get("PATH", "")]),
                CLAUDE_CONFIG_DIR=str(launcher.claude_ordner("minimal")), CODEX_HOME=str(launcher.CODEX_ZUHAUSE), NPM_CONFIG_PREFIX=f"{HOME}/.local")
NPM = "npm install -g"
KIMI_NPM = f"{NPM} --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest"


def npm_update(paket: str) -> str:
    """Nur bei neuer Version installieren: Ein Lauf ohne Änderung kostet so kaum etwas, und das Zeitlimit trifft npm nicht mitten im Austausch."""
    return (f"""[ "$(npm ls -g {paket} --depth=0 --json 2>/dev/null | jq -r '.dependencies["{paket}"].version // empty')" = "$(npm view {paket} version 2>/dev/null)" ] """
            f"|| {NPM} {paket}@latest")


def werkzeug(name: str) -> str:
    return f"bash {KERN}/werkzeug_aktualisieren.sh {name}"


# „installieren“ fehlt, wenn derselbe Befehl auch installiert. „true“ heißt: kommt aus dem Paketarchiv (jarvis-powershell.timer), hier zählt nur der Stand.
PROGRAMME = {
    # Node zuerst: Die npm-Programme danach laufen so schon mit der neuen Fassung. 900 s, denn beim Wechsel der Hauptversion wird auch Kimi neu installiert.
    "node": {"name": "Node.js (LTS)", "befehl": "node", "anmeldung": "", "stand": "", "sekunden": 900, "update": f"bash {KERN}/node_aktualisieren.sh"},
    "claude": {"name": "Claude Code", "befehl": "claude", "update": "claude update", "anmeldung": "claude auth login",
               "stand": "claude auth status --json", "installieren": "curl -fsSL https://claude.ai/install.sh | bash"},
    "codex": {"name": "Codex CLI", "befehl": "codex", "update": npm_update("@openai/codex"), "anmeldung": "codex login --device-auth", "stand": "codex login status"},
    "opencode": {"name": "OpenCode", "befehl": "opencode", "update": npm_update("opencode-ai"), "anmeldung": "opencode auth login", "stand": "opencode auth list"},
    "kimi": {"name": "Kimi Code", "befehl": "kimi", "update": f"kimi update --yes || {KIMI_NPM}", "anmeldung": "kimi login", "stand": "", "installieren": KIMI_NPM},
    "antigravity": {"name": "Antigravity CLI", "befehl": "agy", "update": "agy update", "anmeldung": "agy", "stand": "",
                    "installieren": "curl -fsSL https://antigravity.google/cli/install.sh | bash"},
    "chrome-devtools-mcp": {"name": "Browser-Steuerung", "befehl": "chrome-devtools-mcp", "update": npm_update("chrome-devtools-mcp"), "anmeldung": "", "stand": ""},
    "gh": {"name": "GitHub CLI", "befehl": "gh", "update": "true", "anmeldung": "gh auth login -h github.com -p https -w", "stand": "gh auth token"},
    # Werkzeuge wie in UpdateZentrale/programs.json. Die Firebase-Anmeldung bleibt Franks Entscheidung, darum hier kein Anmelde-Knopf.
    "pnpm": {"name": "pnpm", "befehl": "pnpm", "update": npm_update("pnpm"), "anmeldung": "", "stand": ""},
    # Die Version nennt die Platform Tools, denn sdkmanager --version zeigt nur die eigene. build-tools 36.0.0 (apksigner) bleibt beim Update daneben liegen.
    "android-sdk": {"name": "Android SDK", "befehl": "sdkmanager", "sekunden": 900, "anmeldung": "", "stand": "", "installieren": "true",
                    "version_befehl": "sed -n 's/^Pkg.Revision=//p' $HOME/android-sdk/platform-tools/source.properties",
                    "update": "yes | sdkmanager --licenses >/dev/null 2>&1; sdkmanager --update"},
    "trivy": {"name": "Trivy", "befehl": "trivy", "update": werkzeug("trivy"), "anmeldung": "", "stand": ""},
    "lazygit": {"name": "lazygit", "befehl": "lazygit", "update": werkzeug("lazygit"), "anmeldung": "", "stand": ""},
    "rclone": {"name": "Rclone", "befehl": "rclone", "update": werkzeug("rclone"), "anmeldung": "", "stand": ""},
    "bun": {"name": "Bun", "befehl": "bun", "update": "bun upgrade", "anmeldung": "", "stand": "",
            "installieren": "curl -fsSL https://bun.com/install | SHELL=/bin/sh BUN_INSTALL=$HOME/.local bash"},
    "deno": {"name": "Deno", "befehl": "deno", "update": "deno upgrade", "anmeldung": "", "stand": "",
             "installieren": "curl -fsSL https://deno.land/install.sh | DENO_INSTALL=$HOME/.local sh -s -- --yes --no-modify-path"},
    "cmake": {"name": "CMake", "befehl": "cmake", "update": "true", "anmeldung": "", "stand": ""},
    "ninja": {"name": "Ninja", "befehl": "ninja", "update": "true", "anmeldung": "", "stand": ""},
    "uv": {"name": "uv", "befehl": "uv", "update": "uv self update", "anmeldung": "", "stand": "", "installieren": "curl -LsSf https://astral.sh/uv/install.sh | UV_NO_MODIFY_PATH=1 sh"},
    "typescript": {"name": "TypeScript", "befehl": "tsc", "update": npm_update("typescript"), "anmeldung": "", "stand": ""},
    "tsx": {"name": "tsx", "befehl": "tsx", "update": npm_update("tsx"), "anmeldung": "", "stand": ""},
    "biome": {"name": "Biome", "befehl": "biome", "update": npm_update("@biomejs/biome"), "anmeldung": "", "stand": ""},
    "firebase": {"name": "Firebase CLI", "befehl": "firebase", "update": npm_update("firebase-tools"), "sekunden": 900, "anmeldung": "", "stand": ""},
}
MCP_EINTRAG = "mcp-anbindung"


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


def _vorhanden(p: dict) -> bool:
    return shutil.which(p["befehl"], path=UMGEBUNG["PATH"]) is not None


def version(p: dict) -> str:
    code, aus = _lauf(p.get("version_befehl", f"{p['befehl']} --version"), 30)
    if code != 0:
        return ""
    m = re.search(r"\d+\.\d+[\w.\-]*", aus)
    return m.group(0) if m else aus[:40]


def in_benutzung(schluessel: str, p: dict, alle: list) -> bool:
    """Eine CLI nie unter einer laufenden Sitzung ersetzen. Die Sitzungen nennen ihr CLI; der Prozesstest findet auch die in einem freien Terminal."""
    if any(s.get("cli") == schluessel and s["stand"] != "beendet" for s in alle):
        return True
    # Klammern: Der Text steht auch in der Befehlszeile des bash -c um pgrep. Ohne sie fände sich die Prüfung selbst, und die CLI gälte immer als laufend.
    # Das Android SDK wartet auf Bauten und laufende Installationen, die seine Werkzeuge gerade benutzen.
    muster = {"codex": "[@]openai/codex/", "opencode": "[o]pencode-ai/", "kimi": "[@]moonshot-ai/kimi-code/",
              "android-sdk": "[G]radleDaemon|[K]otlinCompileDaemon|[a]db .*install"}.get(schluessel)
    code, _ = _lauf(f"pgrep -u \"$(id -u)\" -f '(^|/){p['befehl']}( |$)" + (f"|{muster}" if muster else "") + "' >/dev/null", 10)
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


def _status_aendern(aendern: Callable[[dict], None]) -> dict:
    """Timer, Knopf und die Auffrischung der Konsole schreiben dieselbe Datei. Unter der Sperre frisch lesen und nur Eigenes ändern, sonst löschte einer die Merker des anderen."""
    STATUS_SPERRE.parent.mkdir(parents=True, exist_ok=True)
    with open(STATUS_SPERRE, "a") as sperre:
        fcntl.flock(sperre, fcntl.LOCK_EX)
        daten = _stand()
        aendern(daten)
        launcher._schreib(STATUS, json.dumps(daten, ensure_ascii=False, indent=1))
        return daten


def status_schreiben(aktualisieren: bool, auswahl: Callable[[str, dict, dict], bool] | None = None) -> dict:
    alt = _stand().get("programme", {})
    gelesen = {}
    for schluessel, p in PROGRAMME.items():
        bisher = alt.get(schluessel, {})
        da = _vorhanden(p)
        anstehend = aktualisieren and (auswahl is None or auswahl(schluessel, p, bisher))
        # Beim Auffrischen des Reiters System reicht für Werkzeuge ohne Anmeldung der letzte Stand: Jede Abfrage startet einen Prozess, auch neben einem Android-Bau.
        vorher = bisher.get("version", "") if da and not anstehend and not p["anmeldung"] else ""
        vorher = (vorher or version(p)) if da else ""
        eintrag = {"name": p["name"], "version": vorher, "zuletzt": bisher.get("zuletzt", ""), "meldung": bisher.get("meldung", ""), "offen": bisher.get("offen", False)}
        if anstehend:
            jetzt = time.strftime("%d.%m.%Y %H:%M")
            # Sitzungen je Programm neu lesen: Ein Lauf dauert Minuten, und Jarvis kann währenddessen eine starten.
            if da and in_benutzung(schluessel, p, launcher.sitzungen()):
                eintrag["meldung"] = f"{jetzt}: läuft gerade, beim nächsten Lauf"
                eintrag["offen"] = True
            else:
                code, aus = _lauf(p["update"] if da else p.get("installieren", p["update"]), p.get("sekunden", 300))
                eintrag["version"] = version(p) if _vorhanden(p) else ""
                eintrag["zuletzt"] = jetzt
                # Pakete aus dem Archiv installiert dieser Lauf nie: Stündliches Wiederholen änderte daran nichts.
                eintrag["offen"] = code != 0 or (not eintrag["version"] and p.get("installieren", p["update"]) != "true")
                if code != 0:
                    eintrag["meldung"] = f"{jetzt}: fehlgeschlagen: {aus[-200:]}"
                elif not eintrag["version"]:
                    eintrag["meldung"] = f"{jetzt}: fehlgeschlagen: nicht installiert"
                elif eintrag["version"] != vorher:
                    eintrag["meldung"] = f"{jetzt}: aktualisiert von {vorher or ('nichts' if not da else 'unbekannt')} auf {eintrag['version']}"
                else:
                    eintrag["meldung"] = f"{jetzt}: aktuell"
        eintrag["anmeldung"] = anmeldung(schluessel, p) if eintrag["version"] else "fehlt"
        eintrag["anmeldbar"] = bool(p["anmeldung"])
        gelesen[schluessel] = eintrag
        if anstehend:
            _status_aendern(lambda d, s=schluessel, e=eintrag: d.setdefault("programme", {}).__setitem__(s, e))
    code, pwsh = _lauf("pwsh --version", 20)

    def abschliessen(daten: dict) -> None:
        programme = daten.setdefault("programme", {})
        for schluessel, eintrag in gelesen.items():
            # Meldung und Merker eines anderen Laufs bleiben; hier zählt nur, was gerade gelesen wurde.
            ziel = programme.setdefault(schluessel, eintrag)
            ziel.update({k: eintrag[k] for k in ("name", "version", "anmeldung", "anmeldbar")})
        for schluessel in [k for k in programme if k not in PROGRAMME and k != MCP_EINTRAG]:
            del programme[schluessel]
        daten.update(powershell=pwsh if code == 0 else "", geprueft=time.strftime("%d.%m.%Y %H:%M"))
    return _status_aendern(abschliessen)


def _warte_auf_pakete() -> None:
    """Beide Timer starten um 4:30 Uhr. Die Systempakete zuerst: Sonst ersetzte apt jq oder das JDK unter einem laufenden Update, und sdkmanager sähe aus wie ein laufendes Java."""
    time.sleep(5)
    ende = time.monotonic() + PAKETE_WARTEN_S
    while time.monotonic() < ende:
        if subprocess.run(["systemctl", "is-active", "jarvis-powershell.service"], capture_output=True, text=True).stdout.strip() not in ("active", "activating"):
            return
        time.sleep(15)


def tageslauf() -> None:
    """Der erste Lauf eines Tages ab 4:30 Uhr aktualisiert alles, auch wenn der Server um 4:30 Uhr aus war. Danach holt jede Stunde nur das Offene nach.
    Der Tag wird vorab mit allen Programmen als offen vermerkt: Endet der Lauf vorzeitig, macht der nächste genau beim Rest weiter statt wieder von vorn."""
    stand = _stand()
    heute = time.strftime("%Y-%m-%d")
    faellig = stand.get("tageslauf") != heute and time.strftime("%H:%M") >= "04:30"
    if not faellig and not any(e.get("offen") for e in stand.get("programme", {}).values()):
        return
    _warte_auf_pakete()
    if faellig:
        def vormerken(daten: dict) -> None:
            daten["tageslauf"] = heute
            for schluessel, p in PROGRAMME.items():
                daten.setdefault("programme", {}).setdefault(schluessel, {"name": p["name"], "version": "", "anmeldung": ""})["offen"] = True
        _status_aendern(vormerken)
    status_schreiben(True, lambda _s, _p, bisher: bisher.get("offen", False))


def mcp_melden(fehler: list[str]) -> None:
    """Im Reiter System sichtbar, denn das Journal liest niemand. Die Liste enthält keine Adressen."""
    _status_aendern(lambda d: d.setdefault("programme", {}).__setitem__(MCP_EINTRAG, {
        "name": "MCP-Anbindung", "version": "Fehler" if fehler else "in Ordnung", "zuletzt": time.strftime("%d.%m.%Y %H:%M"), "meldung": "; ".join(fehler),
        "offen": False, "anmeldung": "keine", "anmeldbar": False}))


@contextmanager
def _lauf_sperre() -> Iterator[bool]:
    """Immer nur ein Update-Lauf: Zwei npm-Installationen oder Node-Tausche überlagerten sich sonst. Die Kindprozesse erben die Sperre nicht."""
    LAUF_SPERRE.parent.mkdir(parents=True, exist_ok=True)
    with open(LAUF_SPERRE, "a") as sperre:
        try:
            fcntl.flock(sperre, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError:
            yield False
            return
        yield True


if __name__ == "__main__":
    os.environ["TZ"] = "Europe/Berlin"
    time.tzset()
    modus = sys.argv[1] if len(sys.argv) > 1 else ""
    if modus not in ("taeglich", "aktualisieren", "fehlende"):
        print(json.dumps(status_schreiben(False), ensure_ascii=False, indent=1))
        sys.exit(0)
    with _lauf_sperre() as frei:
        if not frei:
            print("Es läuft schon ein Update.")
            sys.exit(0)
        if modus == "fehlende":
            status_schreiben(True, lambda _s, p, _b: not _vorhanden(p))
            sys.exit(0)
        # Vor den Updates: Ein Abbruch oder das Dienst-Zeitlimit darf die Anbindung nicht mitreißen.
        import mcp_anbindung
        mcp_melden(mcp_anbindung.anbinden())
        if modus == "taeglich":
            tageslauf()
        else:
            print(json.dumps(status_schreiben(True), ensure_ascii=False, indent=1))
