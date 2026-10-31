"""OpenLauncher Server: dieselbe Startlogik wie OpenLauncherMac, ohne Oberfläche, für den Hostinger-Server.

Liest dieselben Dateien wie Mac und Windows (models.json, model-defaults.json, Profiles/WorkModes, Profiltexte) und startet jede
CLI in einer eigenen tmux-Sitzung. Vorlage: OpenLauncherMac/OpenLauncher/Services/OpenLauncherService.swift und
InstructionProfileService.swift. Benutzt von der Brücke (bruecke/server_bruecke.py) und der Konsole (web/app.py).

Aufruf von Hand: python3 launcher.py starten --cli claude --modell 'claude-opus-5-5[1m]'
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import subprocess
import sys
import time
from pathlib import Path

HOME = Path.home()
PROGGS = HOME / "proggs"
ZUSTAND = HOME / ".openlauncher"
SITZUNGEN = ZUSTAND / "sitzungen"
SKRIPTE = ZUSTAND / "start"
PROFILE = PROGGS / "OpenLauncher" / "Profiles"
HOOK = PROGGS / "Jarvis" / "werkzeuge" / "pc-bruecke" / "jarvis_hook.py"
ZUSATZ = PROGGS / "OpenLauncherServer" / "server-zusatz.md"
RECHNER = "Hostinger-Server"
TMUX = ["tmux", "-L", "jarvis"]
PROFIL_IDS = ("minimal", "standard", "strict")
MODUS_IDS = ("frei", "schnell", "normal", "gruendlich")
CLIS = ("claude", "codex", "opencode", "kimi", "antigravity")
EFFORTS = {"claude": ["low", "medium", "high", "xhigh", "max"], "codex": ["minimal", "low", "medium", "high", "xhigh"],
           "opencode": ["none", "minimal", "low", "medium", "high", "xhigh", "max"], "kimi": ["low", "high", "max"],
           "antigravity": ["low", "medium", "high", "xhigh", "max"]}
# 2 vCPU, 8 GB, nebenbei Werft Studio und Second Brain: mehr vertragen die Sitzungen nicht (OpenLauncherServer/PLAN.md).
MAX_ARBEITEND = 2
MAX_OFFEN = 4
ARBEITET_S = 180  # so lange nach der letzten Ausgabe gilt eine Sitzung als arbeitend
UNTAETIG_ZU_S = 12 * 3600  # danach schließt der Launcher eine untätige CLI (länger wartet auch kein Warter auf Nachträge)


def _lies(pfad: Path) -> str:
    try:
        return pfad.read_text(encoding="utf-8")
    except OSError:
        return ""


def _schreib(pfad: Path, text: str) -> None:
    pfad.parent.mkdir(parents=True, exist_ok=True)
    neu = pfad.with_name(pfad.name + f".{os.getpid()}.neu")
    neu.write_text(text, encoding="utf-8")
    os.replace(neu, pfad)


def _uebersetzt(text: str) -> str:
    """Die Profiltexte sind für den Mac geschrieben; der Pfadanteil ist das Einzige, was hier anders ist."""
    return text.replace("/Users/frank", str(HOME))


def pruefe_wahl(profil: str, modus: str) -> None:
    if profil not in PROFIL_IDS:
        raise ValueError(f"Unbekanntes Profil: {profil}")
    if modus not in MODUS_IDS:
        raise ValueError(f"Unbekannter Modus: {modus}")


def regeltext(quelle: Path, modus: str) -> str:
    """Profil, dann die Server-Regeln (sie gehen dem Profil vor), dann der Modus, wie composeClaudeContext am Mac."""
    teile = [_uebersetzt(_lies(quelle)).strip(), _uebersetzt(_lies(ZUSATZ)).strip(), _lies(PROFILE / "WorkModes" / f"{modus}.md").strip()]
    return "\n\n".join(t for t in teile if t) + "\n"


# ---------------------------------------------------------------- Repo

def repo_abgleich() -> None:
    """Holt den neuesten Stand, solange das geht. Die AGENTS.md schreibt jeder Start neu, deshalb erst zurück auf den Repo-Stand."""
    git = ["git", "-C", str(PROGGS)]
    subprocess.run(git + ["update-index", "--no-skip-worktree", "AGENTS.md"], capture_output=True, timeout=20)
    subprocess.run(git + ["checkout", "--", "AGENTS.md"], capture_output=True, timeout=20)
    subprocess.run(git + ["pull", "--ff-only", "--quiet"], capture_output=True, timeout=120, stdin=subprocess.DEVNULL)
    subprocess.run(git + ["update-index", "--skip-worktree", "AGENTS.md"], capture_output=True, timeout=20)


# ---------------------------------------------------------------- Claude Code

def _skills_verlinken(ziel: Path) -> None:
    quelle = PROFILE / "ClaudeCode" / "standard" / "skills"
    ziel.parent.mkdir(parents=True, exist_ok=True)
    if ziel.is_symlink() or not ziel.exists():
        if ziel.is_symlink():
            ziel.unlink()
        ziel.symlink_to(quelle)


def globale_skills() -> None:
    _skills_verlinken(HOME / ".claude" / "skills")
    _skills_verlinken(HOME / ".agents" / "skills")


def claude_ordner(profil: str) -> Path:
    return ZUSTAND / "claude" / profil


def claude_vorbereiten(profil: str, modus: str) -> Path:
    """Gegenstück zu ensureClaudeConfigDir. Der Ordner liegt außerhalb des Repos: Alles darin entsteht aus den Mac-Profilen."""
    pruefe_wahl(profil, modus)
    ordner = claude_ordner(profil)
    ordner.mkdir(parents=True, exist_ok=True)
    mac = PROFILE / "ClaudeCodeMac" / profil
    _schreib(ordner / "CLAUDE.md", regeltext(PROFILE / "ClaudeCodeMac" / "sources" / f"{profil}.md", modus))
    einstellungen = json.loads(_lies(mac / "settings.json") or "{}")
    # Der Login-Abgleich der Profile spricht den macOS-Schlüsselbund an; hier erledigt ihn _login_abgleich.
    for art, eintraege in list((einstellungen.get("hooks") or {}).items()):
        einstellungen["hooks"][art] = [e for e in eintraege if not any("claude-login-sync" in h.get("command", "") for h in e.get("hooks", []))]
    einstellungen.pop("_comment", None)
    _schreib(ordner / "settings.json", _uebersetzt(json.dumps(einstellungen, ensure_ascii=False, indent=2)))
    for teil in ("agents", "commands", "rules"):
        link, quelle = ordner / teil, mac / teil
        if quelle.is_dir() and not link.exists():
            link.symlink_to(quelle)
    _skills_verlinken(ordner / "skills")
    globale_skills()
    _login_abgleich(ordner)
    _erststart_setzen(ordner)
    return ordner


def _ablauf(pfad: Path) -> int:
    try:
        return int(json.loads(_lies(pfad))["claudeAiOauth"]["expiresAt"])
    except (ValueError, KeyError, TypeError):
        return -1


def _login_abgleich(ziel: Path) -> None:
    """Jedes Profil hat einen eigenen Ordner, die Anmeldung gilt aber für alle: den jüngsten Login in das gewählte Profil spiegeln.
    Auf Linux liegt er als Datei .credentials.json im Ordner."""
    kandidaten = sorted((claude_ordner(p) / ".credentials.json" for p in PROFIL_IDS), key=_ablauf)
    bester = kandidaten[-1]
    datei = ziel / ".credentials.json"
    if _ablauf(bester) > _ablauf(datei):
        _schreib(datei, _lies(bester))
        datei.chmod(0o600)


def _erststart_setzen(ordner: Path) -> None:
    """Ohne diese Angaben bliebe eine unbeaufsichtigte Sitzung bei Einrichtung und Vertrauensfrage stehen."""
    pfad = ordner / ".claude.json"
    try:
        daten = json.loads(_lies(pfad) or "{}")
    except ValueError:
        daten = {}
    daten.update(hasCompletedOnboarding=True, bypassPermissionsModeAccepted=True, theme="dark")
    daten.setdefault("projects", {}).setdefault(str(PROGGS), {}).update(hasTrustDialogAccepted=True, hasCompletedProjectOnboarding=True)
    _schreib(pfad, json.dumps(daten, ensure_ascii=False, indent=2))
    pfad.chmod(0o600)


def claude_anmeldung(profil: str = "minimal") -> str:
    """lokal, abgemeldet oder unbekannt, wie die Mac-Brücke; gelesen wird nur „loggedIn“."""
    ordner = claude_ordner(profil)
    try:
        ordner.mkdir(parents=True, exist_ok=True)
        _login_abgleich(ordner)
        aus = subprocess.run([str(HOME / ".local" / "bin" / "claude"), "auth", "status", "--json"], env=dict(os.environ, CLAUDE_CONFIG_DIR=str(ordner)),
                             capture_output=True, text=True, timeout=15, stdin=subprocess.DEVNULL).stdout
        angemeldet = json.loads(aus or "{}").get("loggedIn")
    except Exception:
        return "unbekannt"
    return "lokal" if angemeldet is True else "abgemeldet" if angemeldet is False else "unbekannt"


def _jarvis_hooks() -> dict:
    """Dieselben Hooks wie OpenLauncherService.jarvisHooksNode am Mac."""
    befehl = f'python3 "{HOOK}"'

    def eintrag(matcher, art, timeout, hintergrund=False):
        hook = {"type": "command", "command": f"{befehl} {art}", "timeout": timeout}
        if hintergrund:
            hook["async"] = True
        e = {"hooks": [hook]}
        if matcher is not None:
            e["matcher"] = matcher
        return e

    warter = {"hooks": [{"type": "command", "command": f"{befehl} warte", "timeout": 43200, "asyncRewake": True,
                         "rewakeSummary": "Nachtrag von Frank über Jarvis"}]}
    return {"PostToolUse": [eintrag("*", "post", 15, True)], "PostToolUseFailure": [eintrag("Bash|PowerShell", "post", 15, True)],
            "PreToolUse": [eintrag("AskUserQuestion", "frage", 660), eintrag("*", "puls", 10, True)],
            "Stop": [eintrag(None, "stop", 660), warter], "StopFailure": [eintrag(None, "fehlschlag", 30)], "SessionStart": [warter]}


# ---------------------------------------------------------------- Codex, Kimi, Antigravity, OpenCode

def agents_aktivieren(profil: str, modus: str) -> None:
    """Gegenstück zu activateCodexProjectAgents. Die Datei ist versioniert: skip-worktree hält die Server-Fassung aus jedem Commit heraus."""
    pruefe_wahl(profil, modus)
    _schreib(PROGGS / "AGENTS.md", regeltext(PROFILE / "OpenCodeMac" / profil / "AGENTS.md", modus))
    subprocess.run(["git", "-C", str(PROGGS), "update-index", "--skip-worktree", "AGENTS.md"], capture_output=True, timeout=20)


def codex_zuhause() -> Path:
    """Eigenes Zuhause wie am Mac: keine globale AGENTS.md, keine Plugins, keine MCP-Server. Die Anmeldung liegt ebenfalls hier."""
    zuhause = ZUSTAND / "codex-home"
    zuhause.mkdir(parents=True, exist_ok=True)
    _schreib(zuhause / "AGENTS.md", "")
    konfig = zuhause / "config.toml"
    if not konfig.exists():
        _schreib(konfig, f'# Von OpenLauncher Server angelegt. Codex ergänzt hier selbst seine Vertrauensstufen.\n\n[projects."{PROGGS}"]\ntrust_level = "trusted"\n')
    globale_skills()
    return zuhause


def opencode_plugin() -> None:
    quelle, ziel = PROGGS / "opencode-setup" / "plugins" / "jarvis-auftrag.js", HOME / ".config" / "opencode" / "plugins" / "jarvis-auftrag.js"
    if quelle.exists() and _lies(ziel) != _lies(quelle):
        _schreib(ziel, _lies(quelle))


def _kimi_effort(alias: str, effort: str) -> None:
    pfad = HOME / ".kimi-code" / "config.toml"
    zeilen, abschnitt = _lies(pfad).split("\n"), ""
    for i, zeile in enumerate(zeilen):
        t = zeile.strip()
        if t.startswith("["):
            abschnitt = t
            continue
        schluessel = t.split("=", 1)[0].strip()
        if (abschnitt == "[thinking]" and schluessel == "effort") or (abschnitt == f'[models."{alias}"]' and schluessel == "default_effort"):
            zeilen[i] = f'{schluessel} = "{effort}"'
    if pfad.exists():
        _schreib(pfad, "\n".join(zeilen))


# ---------------------------------------------------------------- Startskripte

KOPF = """#!/bin/bash
# Von OpenLauncher Server erzeugtes Startskript.
export TZ=Europe/Berlin LANG=C.UTF-8 LC_ALL=C.UTF-8 TERM=xterm-256color COLORTERM=truecolor
export JARVIS_SERVER=1 JARVIS_RECHNER={rechner}
export PATH="$HOME/.local/bin:$HOME/.local/node/bin:$HOME/.kimi-code/bin:$HOME/.agy/bin:$HOME/android-sdk/platform-tools:$HOME/android-sdk/cmdline-tools/latest/bin:$PATH"
export ANDROID_HOME="$HOME/android-sdk" ANDROID_SDK_ROOT="$HOME/android-sdk"
export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
unset NO_COLOR FORCE_COLOR CLICOLOR CLICOLOR_FORCE AI_AGENT GIT_TERMINAL_PROMPT JARVIS_AUFTRAG_ID JARVIS_CLI JARVIS_ELTERN_PID
printf '\\033]0;%s\\a' {titel}
cd {ordner} || exit 1
{auftrag_id}
PROMPT=""
[ -n {prompt_datei} ] && [ -f {prompt_datei} ] && PROMPT="$(cat {prompt_datei})"
"""

# Nach dem Ende bleibt die Sitzung als Shell offen, wie der Terminal-Tab am Mac. Die Konsole zeigt sie dann als „beendet“.
# Die Merkdatei sagt der Konsole, dass die CLI beendet ist: tmux nennt auch während des Laufs „bash“ (Skript ohne Job-Steuerung).
FUSS = """
: > "$HOME/.openlauncher/sitzungen/{name}.fertig"
echo
echo "[OpenLauncher] Die CLI ist beendet. Diese Sitzung lässt sich in der Konsole schließen."
exec bash -l
"""


def _claude_rumpf(modell: str, effort: str, profil: str, modus: str, name: str, nummer: str, prompt_datei: str) -> str:
    ordner = claude_vorbereiten(profil, modus)
    einstellungen = SKRIPTE / f"settings-{time.time_ns()}.json"
    daten = {"model": modell, "effortLevel": effort}
    if nummer:
        daten["hooks"] = _jarvis_hooks()
    _schreib(einstellungen, json.dumps(daten, ensure_ascii=False))
    q = shlex.quote
    argumente = f"--dangerously-skip-permissions --settings {q(str(einstellungen))} --model {q(modell)}"
    if effort:
        argumente += f" --effort {q(effort)}"
    if nummer:
        argumente += f" --remote-control {q(name)}"
    return f'''export CLAUDE_CONFIG_DIR={q(str(ordner))}
if [ -n "$PROMPT" ]; then claude {argumente} "$PROMPT"; else claude {argumente}; fi
'''


def _codex_rumpf(modell: str, effort: str, profil: str, modus: str) -> str:
    agents_aktivieren(profil, modus)
    zuhause = codex_zuhause()
    q = shlex.quote
    stufe = ""
    if modell.lower().endswith("-fast"):
        modell, stufe = modell[:-5], " -c 'service_tier=\"priority\"'"
    argumente = f"--dangerously-bypass-approvals-and-sandbox -C {q(str(PROGGS))} -m {q(modell)} -c 'project_doc_max_bytes=1048576'{stufe}"
    if effort in ("low", "medium", "high", "xhigh", "max", "ultra"):
        argumente += " -c " + q(f'model_reasoning_effort="{effort}"')
    return f'''export CODEX_HOME={q(str(zuhause))}
if [ -n "$PROMPT" ]; then codex {argumente} "$PROMPT"; else codex {argumente}; fi
'''


def _opencode_rumpf(modell: str, anbieter: str, effort: str, profil: str, modus: str) -> str:
    agents_aktivieren(profil, modus)
    opencode_plugin()
    globale_skills()
    q = shlex.quote
    kennung = modell if "/" in modell else f"{anbieter}/{modell}"
    # Wie patchModelVariantState am Mac: Die Oberfläche von OpenCode kennt --variant nicht, die Stufe steht in ihrem Zustand.
    if effort and effort != "none":
        zustand = HOME / ".local" / "state" / "opencode" / "model.json"
        try:
            daten = json.loads(_lies(zustand) or "{}")
        except ValueError:
            daten = {}
        daten.setdefault("variant", {})[kennung] = effort
        _schreib(zustand, json.dumps(daten, ensure_ascii=False))
    return f'''export OPENCODE_DISABLE_CLAUDE_CODE_PROMPT=1 OPENLAUNCHER_MODEL={q(kennung)} OPENLAUNCHER_SOURCE=OpenLauncher OPENLAUNCHER_WORK_MODE={q(modus)}
mkdir -p "$HOME/.local/share/opencode/log/stderr"
FEHLER="$HOME/.local/share/opencode/log/stderr/opencode-stderr-$(date +%Y%m%d-%H%M%S)-$$.log"
if [ -n "$PROMPT" ]; then opencode -m {q(kennung)} --prompt "$PROMPT" 2>"$FEHLER"; else opencode -m {q(kennung)} 2>"$FEHLER"; fi
[ -s "$FEHLER" ] && echo "[OpenLauncher] Fehlerausgaben dieses Laufs: $FEHLER" || rm -f "$FEHLER"
'''


def _kimi_rumpf(modell: str, effort: str, profil: str, modus: str) -> str:
    agents_aktivieren(profil, modus)
    alias = f"kimi-code/{modell}"
    if effort:
        _kimi_effort(alias, effort)
    q = shlex.quote
    # Kimi kennt keine erste Nachricht für die Oberfläche, nur -p (ein Durchlauf); danach geht dieselbe Sitzung interaktiv weiter.
    return f'''if [ -n "$PROMPT" ]; then kimi --auto -m {q(alias)} -p "$PROMPT"; kimi --auto -m {q(alias)} -c; else kimi --auto -m {q(alias)}; fi
'''


def _agy_rumpf(modell: str, effort: str, profil: str, modus: str) -> str:
    agents_aktivieren(profil, modus)
    q = shlex.quote
    stufe = f" --effort {q(effort)}" if effort else ""
    return f'''unset GEMINI_API_KEY
if [ -n "$PROMPT" ]; then agy --model {q(modell)}{stufe} --dangerously-skip-permissions -i "$PROMPT"; else agy --model {q(modell)}{stufe} --dangerously-skip-permissions; fi
'''


def _sitzungsname(titel: str) -> str:
    kurz = re.sub(r"[^a-z0-9]+", "-", titel.lower()).strip("-")[:30] or "sitzung"
    return f"{kurz}-{time.strftime('%H%M%S')}"


def _tmux_starten(name: str, skript: Path) -> None:
    ergebnis = subprocess.run(TMUX + ["new-session", "-d", "-s", name, "-x", "120", "-y", "40", "-c", str(PROGGS), f"bash {shlex.quote(str(skript))}"],
                              capture_output=True, text=True, timeout=20)
    if ergebnis.returncode != 0:
        raise RuntimeError(f"tmux konnte die Sitzung nicht starten: {ergebnis.stderr.strip()[:200]}")


def starten(cli: str, modell: str, anbieter: str = "", effort: str = "high", profil: str = "minimal", modus: str = "schnell",
            titel: str = "", prompt: str = "", nummer: str = "") -> str:
    """Startet eine CLI und liefert den Namen der tmux-Sitzung."""
    if cli not in CLIS:
        raise ValueError(f"Unbekanntes CLI: {cli}")
    pruefe_wahl(profil, modus)
    if nummer and not re.fullmatch(r"[A-Za-z0-9]{1,20}", nummer):
        raise ValueError("Ungültige Auftragsnummer")
    aufraeumen()
    repo_abgleich()
    titel = titel or f"{cli}: {modell}"
    name = _sitzungsname(titel)
    SKRIPTE.mkdir(parents=True, exist_ok=True)
    prompt_datei = ""
    if prompt:
        prompt_datei = str(SKRIPTE / f"auftrag-{name}.txt")
        _schreib(Path(prompt_datei), prompt)
    if cli == "claude":
        rumpf = _claude_rumpf(modell, effort, profil, modus, titel, nummer, prompt_datei)
    elif cli == "codex":
        rumpf = _codex_rumpf(modell, effort, profil, modus)
    elif cli == "opencode":
        rumpf = _opencode_rumpf(modell, anbieter, effort, profil, modus)
    elif cli == "kimi":
        rumpf = _kimi_rumpf(modell, effort, profil, modus)
    else:
        rumpf = _agy_rumpf(modell, effort, profil, modus)
    q = shlex.quote
    kopf = KOPF.format(rechner=q(RECHNER), titel=q(titel), ordner=q(str(PROGGS)), prompt_datei=q(prompt_datei),
                       auftrag_id=f"export JARVIS_AUFTRAG_ID={q(nummer)}" if nummer else "")
    skript = SKRIPTE / f"start-{name}.sh"
    _schreib(skript, kopf + rumpf + FUSS.format(name=name))
    skript.chmod(0o700)
    _schreib(SITZUNGEN / f"{name}.json", json.dumps({"name": name, "titel": titel, "cli": cli, "modell": modell, "anbieter": anbieter, "effort": effort,
                                                     "profil": profil, "modus": modus, "auftrag": nummer, "beginn": int(time.time())}, ensure_ascii=False))
    _tmux_starten(name, skript)
    return name


def befehl_starten(titel: str, befehl: str, cli: str = "") -> str:
    """Eine freie Sitzung (Shell, Anmeldung): gleiche Umgebung wie die CLIs, aber ohne Profil."""
    name = _sitzungsname(titel)
    q = shlex.quote
    kopf = KOPF.format(rechner=q(RECHNER), titel=q(titel), ordner=q(str(PROGGS)), prompt_datei="''", auftrag_id="")
    skript = SKRIPTE / f"start-{name}.sh"
    _schreib(skript, kopf + befehl + "\n" + FUSS.format(name=name))
    skript.chmod(0o700)
    _schreib(SITZUNGEN / f"{name}.json", json.dumps({"name": name, "titel": titel, "cli": cli, "frei": True, "beginn": int(time.time())}, ensure_ascii=False))
    _tmux_starten(name, skript)
    return name


# ---------------------------------------------------------------- Sitzungen

def sitzungen() -> list:
    """Alle Sitzungen mit Stand: arbeitet (gibt gerade aus), wartet (CLI offen, still) oder beendet (nur noch die Shell)."""
    aus = subprocess.run(TMUX + ["list-panes", "-a", "-F", "#{session_name}\t#{pane_current_command}\t#{window_activity}\t#{session_attached}"],
                         capture_output=True, text=True, timeout=10).stdout
    jetzt = time.time()
    liste = []
    for zeile in aus.splitlines():
        teile = zeile.split("\t")
        if len(teile) < 4 or teile[0] == "basis":
            continue
        name, befehl, aktiv = teile[0], teile[1], int(teile[2] or 0)
        try:
            meta = json.loads(_lies(SITZUNGEN / f"{name}.json") or "{}")
        except ValueError:
            meta = {}
        still = int(jetzt - aktiv)
        stand = "beendet" if (SITZUNGEN / f"{name}.fertig").exists() else "arbeitet" if still < ARBEITET_S else "wartet"
        liste.append({**meta, "name": name, "titel": meta.get("titel") or name, "stand": stand, "still_s": still, "befehl": befehl,
                      "angezeigt": teile[3] != "0"})
    return sorted(liste, key=lambda s: -s.get("beginn", 0))


def schliessen(name: str) -> None:
    """Beendet die Sitzung samt CLI und räumt ihre Dateien weg."""
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,80}", name) or name == "basis":
        raise ValueError("Ungültiger Sitzungsname")
    subprocess.run(TMUX + ["kill-session", "-t", f"={name}"], capture_output=True, timeout=10)
    for datei in (SITZUNGEN / f"{name}.json", SITZUNGEN / f"{name}.fertig", SKRIPTE / f"start-{name}.sh", SKRIPTE / f"auftrag-{name}.txt"):
        datei.unlink(missing_ok=True)


def aufraeumen() -> None:
    """Schließt Beendete nach einer Stunde und untätige CLIs nach UNTAETIG_ZU_S. Eine gerade angezeigte Sitzung bleibt immer offen."""
    for s in sitzungen():
        if s["angezeigt"]:
            continue
        if (s["stand"] == "beendet" and s["still_s"] > 3600) or (s["stand"] == "wartet" and s["still_s"] > UNTAETIG_ZU_S):
            schliessen(s["name"])
    grenze = time.time() - 14 * 86400
    for datei in SKRIPTE.glob("*") if SKRIPTE.exists() else []:
        if datei.stat().st_mtime < grenze:
            datei.unlink(missing_ok=True)


def belegt() -> bool:
    """Der Server nimmt keinen weiteren Auftrag: zu viele arbeiten oder zu viele sind offen, ohne dass eine schließbar wäre."""
    alle = [s for s in sitzungen() if not s.get("frei") and s["stand"] != "beendet"]
    if sum(s["stand"] == "arbeitet" for s in alle) >= MAX_ARBEITEND:
        return True
    return len(alle) >= MAX_OFFEN


def platz_schaffen() -> None:
    """Vor einem neuen Auftrag: die am längsten untätige CLI schließen, wenn sonst MAX_OFFEN erreicht wäre, und Beendete wegräumen."""
    alle = sitzungen()
    for s in alle:
        if s["stand"] == "beendet" and not s["angezeigt"]:
            schliessen(s["name"])
    offen = [s for s in alle if not s.get("frei") and s["stand"] == "wartet" and not s["angezeigt"]]
    arbeitend = [s for s in alle if not s.get("frei") and s["stand"] == "arbeitet"]
    if len(offen) + len(arbeitend) >= MAX_OFFEN and offen:
        schliessen(max(offen, key=lambda s: s["still_s"])["name"])


def katalog() -> dict:
    """Modelle, Standards und Wahlmöglichkeiten für den Reiter OpenLauncher, gleiche Regeln wie OpenLauncherKatalog.kt in Jarvis."""
    modelle = []
    try:
        gruppen = json.loads(_lies(PROGGS / "OpenLauncher" / "models.json") or "[]")
        standards = json.loads(_lies(PROGGS / "OpenLauncher" / "model-defaults.json") or "{}")
    except ValueError:
        gruppen, standards = [], {}
    for gruppe in gruppen:
        anbieter = str(gruppe.get("ProviderId") or gruppe.get("Id") or "").lower()
        verborgen = set(gruppe.get("HiddenModelSlugs") or [])
        for m in gruppe.get("Models") or []:
            slug = m.get("Slug", "")
            if not slug or m.get("IsHidden") or slug in verborgen:
                continue
            standard = standards.get(f"{anbieter}/{slug}") or {}
            modelle.append({"slug": slug, "name": m.get("DisplayName") or slug, "anbieter": anbieter, "gruppe": gruppe.get("Title") or anbieter,
                            "clis": clis_fuer(anbieter), "profil": standard.get("ProfileId") or "minimal", "modus": standard.get("WorkModeId") or "schnell",
                            "effort": standard.get("ThinkingValue") or "high", "cli": standard.get("CliTargetId") or ""})
    return {"modelle": modelle, "efforts": EFFORTS, "profile": list(PROFIL_IDS), "modi": list(MODUS_IDS)}


def clis_fuer(anbieter: str) -> list:
    if anbieter == "anthropic":
        return ["claude"]
    if anbieter == "openai":
        return ["codex", "opencode"]
    if anbieter in ("moonshot", "kimi-code", "kimi", "moonshotai"):
        return ["kimi", "opencode"]
    if anbieter in ("google", "gemini", "antigravity"):
        return ["antigravity", "opencode"]
    return ["opencode"]


def main() -> int:
    teil = argparse.ArgumentParser(description="OpenLauncher Server")
    unter = teil.add_subparsers(dest="aktion", required=True)
    s = unter.add_parser("starten")
    s.add_argument("--cli", default="claude")
    s.add_argument("--modell", default="claude-opus-5-5[1m]")
    s.add_argument("--anbieter", default="")
    s.add_argument("--effort", default="high")
    s.add_argument("--profil", default="minimal")
    s.add_argument("--modus", default="schnell")
    unter.add_parser("liste")
    unter.add_parser("abgleich")
    z = unter.add_parser("schliessen")
    z.add_argument("name")
    a = teil.parse_args()
    if a.aktion == "starten":
        print(starten(a.cli, a.modell, a.anbieter, a.effort, a.profil, a.modus))
    elif a.aktion == "liste":
        print(json.dumps(sitzungen(), ensure_ascii=False, indent=1))
    elif a.aktion == "abgleich":
        repo_abgleich()
    else:
        schliessen(a.name)
    return 0


if __name__ == "__main__":
    sys.exit(main())
