"""Jarvis-Mac-Brücke: holt Programmieraufträge vom Jarvis-Relay ab und startet dafür am Mac eine Claude-Code-Sitzung
mit Remote Control. Frank sieht sie im Terminal und in der Claude-App unter Code. Gegenstück zu jarvis-pc-bruecke.ps1.

Ablauf: Dauerschleife GET https://<Server>/pc/auftrag (Long-Polling, bis 25 s). Solange das Skript fragt, gilt der Mac
beim Relay als da. Die Mac-Brücke meldet sich mit X-Jarvis-Rang: ersatz (nur der Name): Seit Franks Wahl vom 25.10.2026 hat
der Mac Vorrang, der Windows-PC bekommt Opus-Aufträge nur, wenn der Mac nicht abfragt; Sol 6.1 bleibt beim PC. Ist keiner da, nimmt Jarvis
die Cloud. Start: LaunchAgent de.frank.jarvis.pc-bruecke (einrichten.sh). Zugang aus ~/SK/Jarvis/relay.properties.

Sitzung: Profil Minimal, Schnellmodus, Claude Opus 5.5 (1M), Effort High, wie unter Windows. Nennt Frank in Jarvis ein
Modell aus dem OpenLauncher-Angebot (Auftrag mit modell=katalog), startet die Brücke genau das: slug, anbieter, effort,
profil, modus und cli (claude, codex, opencode, kimi, antigravity). Das kann nur der Mac (der Relay gibt solche Aufträge nur ihm).
Anmeldung: Mit jeder Abfrage meldet die Brücke den lokalen Anmeldestand von Claude Code (X-Jarvis-Anmeldung: lokal,
abgemeldet oder unbekannt, dazu die Prüfzeit). Grundlage ist `claude auth status` im Profilordner, höchstens alle fünf
Minuten und frisch vor jedem Start; gelesen wird nur „loggedIn“, nie Konto oder Token. „lokal“ heißt nur: Anmeldedaten
liegen vor. Ob sie gelten, zeigt erst eine Sitzung (der Hook meldet „Login expired“ sofort). Abgemeldet startet keine Sitzung.
Jarvis Mac OS: Ein eigener Faden fragt /pc/mac-befehl ab und startet die Mac-App, wenn das Handy es verlangt oder Post für sie hat.
Protokoll: ~/Library/Logs/JarvisPcBruecke/bruecke.log
"""
from __future__ import annotations

import fcntl
import json
import os
import plistlib
import re
import shlex
import socket
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

import jarvis_anhaenge

PROFIL = "minimal"
MODUS = "schnell"
MODELL = "claude-opus-5-5[1m]"
EFFORT = "high"
HOME = Path.home()
PROGGS = HOME / "proggs"
ORDNER = HOME / "Library" / "Logs" / "JarvisPcBruecke"
PROTOKOLL = ORDNER / "bruecke.log"
TEMP = Path(os.environ.get("TMPDIR", "/tmp")) / "JarvisPcBruecke"
OPENLAUNCHER = Path("/Applications/OpenLauncher.app")
OPENLAUNCHER_AB = (1, 24, 39)  # erste Mac-Fassung mit --jarvis-auftrag
OPENLAUNCHER_KATALOG_AB = (1, 24, 45)  # erste Mac-Fassung mit --cli und --anbieter (jedes Modell aus dem OpenLauncher-Angebot)
HOOK = PROGGS / "Jarvis" / "werkzeuge" / "pc-bruecke" / "jarvis_hook.py"
CONFIG_DIR = PROGGS / "OpenLauncher" / "Profiles" / "ClaudeCodeMac" / PROFIL
LOGIN_SYNC = PROGGS / "OpenLauncher" / "Profiles" / "hooks" / "claude-login-sync.py"
ANMELDUNG_ALLE_S = 300


def schreib(text: str) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        if PROTOKOLL.exists() and PROTOKOLL.stat().st_size > 1024 * 1024:
            os.replace(PROTOKOLL, PROTOKOLL.with_name("bruecke.log.alt"))
        with PROTOKOLL.open("a", encoding="utf-8") as f:
            f.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} {text}\n")
    except Exception:
        pass


# Nur IPv4, wie curl -4 unter Windows: getaddrinfo liefert sonst zuerst IPv6, und die Verbindung hängt bis zum Zeitlimit
# (bugs/desktop/dotnet-httpclient-ipv6-haenger.md).
_original = socket.getaddrinfo


def _nur_ipv4(host, port, family=0, *args, **kwargs):
    return _original(host, port, socket.AF_INET, *args, **kwargs)


socket.getaddrinfo = _nur_ipv4


def lies_zugang() -> dict:
    datei = HOME / "SK" / "Jarvis" / "relay.properties"
    werte = {}
    for zeile in datei.read_text(encoding="utf-8").splitlines():
        m = re.match(r"^\s*([^#=]+?)\s*=\s*(.*)$", zeile)
        if m:
            werte[m.group(1)] = m.group(2).strip()
    if not werte.get("host") or not werte.get("token"):
        raise RuntimeError(f"host oder token fehlt in {datei}")
    return werte


def rechnername() -> str:
    try:
        return subprocess.run(["scutil", "--get", "ComputerName"], capture_output=True, text=True, timeout=5).stdout.strip() or "Mac"
    except Exception:
        return socket.gethostname() or "Mac"


def rufe(adresse: str, token: str, sekunden: int, rumpf: dict | None = None) -> tuple[int, bytes]:
    anfrage = urllib.request.Request(adresse, method="POST" if rumpf is not None else "GET",
                                     data=json.dumps(rumpf, ensure_ascii=False).encode("utf-8") if rumpf is not None else None,
                                     headers={"X-Jarvis-Token": token, "X-Jarvis-Rechner": RECHNER, "X-Jarvis-Rang": "ersatz",
                                              "X-Jarvis-Anmeldung": ANMELDUNG["stand"], "X-Jarvis-Anmeldung-Zeit": str(int(ANMELDUNG["zeit"])),
                                              # Diese Brücke kennt Aufträge mit frei gewähltem Modell (modell=katalog).
                                              "X-Jarvis-Katalog": "1",
                                              "Content-Type": "application/json; charset=utf-8"})
    try:
        with urllib.request.urlopen(anfrage, timeout=sekunden) as antwort:
            return antwort.status, antwort.read()
    except urllib.error.HTTPError as fehler:
        return fehler.code, b""


def frische_umgebung() -> dict:
    """
    Nur für den Notweg (der OpenLauncher lädt die Login-Umgebung im Startskript selbst). Die Brücke läuft tagelang und
    behielte sonst die Umgebung von ihrem Start (unter Windows zeigte JAVA_HOME nach einem JDK-Update auf einen gelöschten
    Ordner). Darum PATH, JAVA_HOME und die Android-Variablen frisch aus der Shell lesen, samt ~/.zshrc (dort setzt Frank
    ANDROID_HOME, JAVA_HOME und die build-tools; `zsh -l` allein liest sie nicht). launchd startet mit sehr kurzem PATH:
    Homebrew, ~/.local/bin, platform-tools und der Ordner von claude kommen dazu.
    """
    umgebung = dict(os.environ)
    namen = ("PATH", "JAVA_HOME", "ANDROID_HOME", "ANDROID_SDK_ROOT", "ANDROID_NDK_HOME")
    werte = {}
    try:
        skript = "source ~/.zshrc >/dev/null 2>&1; " + "; ".join(f'printf "%s\\0" "${n}"' for n in namen)
        aus = subprocess.run(["/bin/zsh", "-lc", skript], capture_output=True, text=True, timeout=20,
                             stdin=subprocess.DEVNULL).stdout.split("\0")
        werte = {n: w for n, w in zip(namen, aus) if w}
    except Exception:
        pass
    pfad, java = werte.get("PATH", ""), werte.get("JAVA_HOME", "")
    for n in ("ANDROID_HOME", "ANDROID_SDK_ROOT", "ANDROID_NDK_HOME"):
        if werte.get(n):
            umgebung[n] = werte[n]
    sdk = umgebung.get("ANDROID_HOME") or "/opt/homebrew/share/android-commandlinetools"
    teile = [t for t in pfad.split(":") if t] + ["/opt/homebrew/bin", "/opt/homebrew/sbin", str(HOME / ".local" / "bin"),
                                                  f"{sdk}/platform-tools", "/usr/local/bin", "/usr/bin", "/bin", "/usr/sbin", "/sbin"]
    claude = finde_claude(teile)
    if claude:
        teile.insert(0, str(Path(claude).parent))
    umgebung["PATH"] = ":".join(dict.fromkeys(teile))
    try:
        java_home = subprocess.run(["/usr/libexec/java_home"], capture_output=True, text=True, timeout=10)
        kandidat = java_home.stdout.strip() if java_home.returncode == 0 else ""
    except Exception:
        kandidat = ""
    for wahl in (kandidat, java.strip(), "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home",
                 "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"):
        if wahl and Path(wahl, "bin", "java").exists():
            umgebung["JAVA_HOME"] = wahl
            break
    umgebung.setdefault("ANDROID_HOME", sdk)
    return umgebung


def finde_claude(pfade: list[str]) -> str | None:
    for kandidat in [str(HOME / ".local" / "bin" / "claude")] + [str(Path(p) / "claude") for p in pfade]:
        if os.path.isfile(kandidat) and os.access(kandidat, os.X_OK):
            return kandidat
    return None


ANMELDUNG = {"stand": "unbekannt", "zeit": 0.0}


def _auth_status() -> str:
    claude = finde_claude(os.environ.get("PATH", "").split(":") + ["/opt/homebrew/bin", "/usr/local/bin"])
    if not claude:
        return "unbekannt"
    ergebnis = subprocess.run([claude, "auth", "status", "--json"], env=dict(os.environ, CLAUDE_CONFIG_DIR=str(CONFIG_DIR)),
                              capture_output=True, text=True, timeout=10, stdin=subprocess.DEVNULL)
    angemeldet = json.loads(ergebnis.stdout or "{}").get("loggedIn")
    return "lokal" if angemeldet is True else "abgemeldet" if angemeldet is False else "unbekannt"


def pruefe_anmeldung(frisch: bool = False) -> dict:
    """
    Lokaler Anmeldestand von Claude Code im Profilordner (siehe Kopf). Abgemeldet: erst der Login-Abgleich, den auch jeder
    Start ausführt (holt einen gültigen Login aus einem anderen Profil), dann noch einmal prüfen. Jeder Fehler = unbekannt.
    """
    if not frisch and time.time() - ANMELDUNG["zeit"] < ANMELDUNG_ALLE_S:
        return ANMELDUNG
    try:
        stand = _auth_status()
        if stand == "abgemeldet" and LOGIN_SYNC.exists():
            subprocess.run(["python3", str(LOGIN_SYNC), "--startup"], env=dict(os.environ, CLAUDE_CONFIG_DIR=str(CONFIG_DIR)),
                           capture_output=True, timeout=15, stdin=subprocess.DEVNULL)
            stand = _auth_status()
    except Exception as fehler:
        schreib(f"Anmeldung nicht prüfbar: {type(fehler).__name__}")
        stand = "unbekannt"
    if stand != ANMELDUNG["stand"]:
        schreib(f"Anmeldung: {stand}")
    ANMELDUNG.update(stand=stand, zeit=time.time())
    return ANMELDUNG


_PRUEFER: threading.Thread | None = None


def pruefe_anmeldung_nebenher() -> None:
    """
    Die regelmäßige Prüfung (mit Login-Abgleich bis etwa 25 s) in einem eigenen Faden: Die Abfrageschleife läuft weiter, sonst
    gälte der Mac beim Relay nach 3 s als „holt nicht ab“ und ein Auftrag ginge trotz Mac-Vorrang an den Windows-PC.
    """
    global _PRUEFER
    if time.time() - ANMELDUNG["zeit"] < ANMELDUNG_ALLE_S or (_PRUEFER is not None and _PRUEFER.is_alive()):
        return
    _PRUEFER = threading.Thread(target=pruefe_anmeldung, daemon=True)
    _PRUEFER.start()


def openlauncher_kann_jarvis(ab: tuple = OPENLAUNCHER_AB) -> bool:
    try:
        with (OPENLAUNCHER / "Contents" / "Info.plist").open("rb") as f:
            version = plistlib.load(f).get("CFBundleShortVersionString", "0")
        return tuple(int(x) for x in version.split(".")[:3]) >= ab
    except Exception:
        return False


def katalog_cli(auftrag: dict) -> str:
    """Das CLI eines Katalog-Auftrags; bei Opus/Sol leer."""
    return str(auftrag.get("cli") or "claude") if auftrag.get("modell") == "katalog" else ""


def jarvis_hooks() -> dict:
    """Dieselben Hooks wie OpenLauncherService.JarvisHooks() (Windows) bzw. OpenLauncherMac, nur für den Notweg."""
    befehl = f'python3 "{HOOK}"'

    def eintrag(matcher, art, timeout, hintergrund=False):
        hook = {"type": "command", "command": f"{befehl} {art}", "timeout": timeout}
        if hintergrund:
            hook["async"] = True
        e = {"hooks": [hook]}
        if matcher is not None:
            e["matcher"] = matcher
        return e

    warter = {"hooks": [{"type": "command", "command": f"{befehl} warte", "timeout": 43200,
                         "asyncRewake": True, "rewakeSummary": "Nachtrag von Frank über Jarvis"}]}
    return {
        "PostToolUse": [eintrag("*", "post", 15, True)],
        "PostToolUseFailure": [eintrag("Bash|PowerShell", "post", 15, True)],
        "PreToolUse": [eintrag("AskUserQuestion", "frage", 660), eintrag("*", "puls", 10, True)],
        "Stop": [eintrag(None, "stop", 660), warter],
        "StopFailure": [eintrag(None, "fehlschlag", 30)],
        "SessionStart": [warter],
    }


def raeume_auf() -> None:
    """Aufträge, Startskripte und Hook-Zustände blieben sonst für immer liegen, samt dem vollen Auftragstext in TMPDIR.
    14 Tage: Kein Warter lebt länger als 12 h, und so alte Sitzungen laufen nicht mehr."""
    grenze = time.time() - 14 * 86400
    for ordner, muster in ((TEMP, ("auftrag-*.txt", "start-*.command", "settings-*.json")),
                           (ORDNER, ("hook-*.json", "hook-*.json.*.tmp", "hook-*.sperre", "hook-*.lock", "puls-*.txt", "warter-*.pid",
                                     "lokal-*.hook", "jarvis-*.sitzung", "entfernt-*"))):
        for m in muster:
            for datei in ordner.glob(m):
                try:
                    if datei.is_file() and datei.stat().st_mtime < grenze:
                        datei.unlink()
                except OSError:
                    pass


def auftragstext(auftrag: dict) -> str:
    """Die erste Nachricht der Sitzung. Der Auftrag geht zeilengenau hinein: keine Umbrüche ergänzen oder entfernen (Absätze,
    Listen, Codeblöcke und „ ; “-Trenner bleiben, wie Jarvis sie geschickt hat; test_auftragstext.py). Nur CRLF/CR werden LF:
    Ein nacktes CR setzt im Terminal den Cursor an den Zeilenanfang."""
    # Ohne REVIEW-Zeile (Franks Wahl vom 22.10.2026): Claude Code fragt am Ende selbst, ob ein Review laufen soll.
    app = auftrag.get("app") or ""
    text = (auftrag.get("auftrag") or "").replace("\r\n", "\n").replace("\r", "\n")
    cli = katalog_cli(auftrag)
    kopf = "Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); Frank verfolgt die Sitzung per Remote Control."
    unklar1 = "Nicht eindeutig: Frank mit AskUserQuestion fragen und die Kandidaten nennen."
    unklar2 = "2. Wesentliche Unklarheiten mit AskUserQuestion klären, Kleinigkeiten selbst entscheiden."
    review = ""
    if cli == "opencode":
        # OpenCode: Rückfragen über das Werkzeug question; das Plugin jarvis-auftrag.js bringt sie aufs Handy und die Antwort zurück.
        kopf = "Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); Frank verfolgt die Schritte in Jarvis."
        unklar1 = "Nicht eindeutig: Frank mit dem Werkzeug question fragen und die Kandidaten nennen."
        unklar2 = "2. Wesentliche Unklarheiten mit dem Werkzeug question klären, Kleinigkeiten selbst entscheiden."
    elif cli in ("codex", "kimi", "antigravity"):
        # Ohne Hooks und ohne Rückfragen (wie Sol im Codex CLI am Windows-PC): Die Sitzung meldet selbst über jarvis_hook.py melde.
        kopf = ("Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); Frank verfolgt die Schritte in Jarvis.\n"
                f'MELDEN (statt programmier_fortschritt, das es hier nicht gibt): python3 "{HOOK}" melde <laeuft|fertig|fehler> "<ein Satz>"')
        unklar1 = "Nicht eindeutig: die wahrscheinlichste App nehmen und das in der ersten Meldung nennen."
        unklar2 = "2. Niemand antwortet in dieser Sitzung: Unklarheiten selbst sinnvoll entscheiden und in der Meldung nennen, nie auf eine Antwort warten."
        review = ("REVIEW: Ja (vor dem Abschluss den gesamten Diff gründlich selbst auf Fehler prüfen und Befunde beheben)\n"
                  if auftrag.get("review") else "REVIEW: Nein\n")
    return f"""{kopf}

APP: {app}
{review}AUFTRAG:
{text}

VORGEHEN
1. App im Repo finden. Bekannt: Geniale Aufgaben = Aufgaben, Genialer Wecker = GenialerWeckerAndroid, Geniale Ideen = GenialeIdeen, Entropie Reductor = EntropieReductor, Jarvis = Jarvis, UpdateStation = UpdateStation. {unklar1}
{unklar2}
3. Nach den Profilregeln vollständig abschließen: bauen, Version, Commit, Push, Installation per WLAN (die App Jarvis nie mit nacktem adb install, sondern immer über Werkzeuge/jarvis-install, das vorher per Overlay nach dem Zeitpunkt fragt), bei Android-Apps Skill apk-update. Abschlussmeldung auf Deutsch.
"""


def starte_sitzung(auftrag: dict) -> str:
    app = re.sub(r"[^\w ._-]", "", str(auftrag.get("app", ""))).strip() or "App"
    name = f"Jarvis: {app}"
    cli = katalog_cli(auftrag)
    if cli:
        modellname = re.sub(r"[^\w ._()-]", "", str(auftrag.get("name") or auftrag.get("slug") or "")).strip()
        name += f" ({modellname}" + ("" if cli == "claude" else f", {cli}") + ")"
    nummer = str(auftrag.get("nummer", ""))
    mit_hooks = bool(re.fullmatch(r"[A-Za-z0-9]{1,20}", nummer))
    if auftrag.get("anhaenge"):
        # Vor dem Start vollständig auf dem Rechner. Die Bilder fügt der SessionStart-Hook von Claude Code als [Image #n] ein.
        zugang = lies_zugang()
        auftrag = {**auftrag, "auftrag": (auftrag.get("auftrag") or "") + jarvis_anhaenge.fuer_auftrag(auftrag, zugang["host"], zugang["token"], cli in ("", "claude") and mit_hooks)}
    text = auftragstext(auftrag)
    stempel = time.strftime("%Y%m%d-%H%M%S")
    TEMP.mkdir(parents=True, exist_ok=True)
    raeume_auf()
    auftrag_datei = TEMP / f"auftrag-{stempel}.txt"
    auftrag_datei.write_text(text, encoding="utf-8")

    # Normalfall: über den OpenLauncherMac, genau wie der Startknopf (Profil, aktive CLAUDE.md mit Schnellmodus, Anmeldung,
    # Terminal-Farbe), dazu Remote Control mit Namen und der Auftrag als erste Nachricht. Ab OpenLauncherMac 1.24.39.
    # Frei gewähltes Modell: genau mit Franks Angaben (die App hat sie gegen das OpenLauncher-Angebot geprüft), nie still Opus.
    # Claude Code mit Modell, Effort, Profil und Modus kann schon jede Fassung mit --jarvis-auftrag (--cli und --anbieter ignoriert
    # sie); Codex, OpenCode, Kimi und Antigravity erst ab 1.24.45.
    if cli not in ("", "claude") and not openlauncher_kann_jarvis(OPENLAUNCHER_KATALOG_AB):
        raise RuntimeError(f"{cli} braucht am Mac OpenLauncher ab 1.24.45; bitte den OpenLauncher über das Updatescript aktualisieren.")
    if openlauncher_kann_jarvis():
        argumente = [str(OPENLAUNCHER / "Contents" / "MacOS" / "OpenLauncher"), "--jarvis-auftrag", str(auftrag_datei), "--name", name,
                     "--profil", str(auftrag.get("profil") or PROFIL) if cli else PROFIL,
                     "--modus", str(auftrag.get("modus") or MODUS) if cli else MODUS,
                     "--modell", str(auftrag.get("slug") or MODELL) if cli else MODELL,
                     "--effort", str(auftrag.get("effort") or EFFORT) if cli else EFFORT]
        if cli:
            argumente += ["--cli", cli, "--anbieter", str(auftrag.get("anbieter") or "")]
        # Mit der Nummer bekommt die Sitzung die Jarvis-Hooks: Fortschritt und Rückfragen erscheinen im Jarvis-Chat.
        if mit_hooks:
            argumente += ["--auftrag-id", nummer]
        prozess = subprocess.Popen(argumente, cwd=str(PROGGS), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        # Spätestens nach 30 s quittieren: Der Relay wartet 45 s. Kein Fehler melden, wenn der OpenLauncher nur langsam ist:
        # Mit ok=false spränge das Handy auf die Cloud aus, und zwei Sitzungen arbeiteten am selben Repo.
        try:
            code = prozess.wait(30)
        except subprocess.TimeoutExpired:
            schreib("OpenLauncher nach 30 s noch nicht fertig, Start unbestätigt")
            return f"{name} (Start unbestätigt: der OpenLauncher war nach 30 Sekunden noch nicht fertig)"
        if code != 0:
            raise RuntimeError(f"Der OpenLauncher konnte die Sitzung nicht starten (Code {code}, siehe OpenLauncher-Log).")
        return name

    # Notweg für einen älteren OpenLauncherMac: direkt in Terminal.app starten (Profil-Ordner, Modell, Effort, Hooks wie
    # oben; die aktive CLAUDE.md und damit der Modus bleiben so, wie der OpenLauncher sie zuletzt geschrieben hat).
    # Der Notweg kennt nur Opus: ein frei gewähltes Modell nie still als Opus starten.
    if cli:
        raise RuntimeError("Für ein frei gewähltes Modell braucht der Mac einen aktuellen OpenLauncher; bitte über das Updatescript aktualisieren.")
    schreib("OpenLauncher ohne --jarvis-auftrag, starte direkt")
    umgebung = frische_umgebung()
    claude = finde_claude(umgebung["PATH"].split(":"))
    if not claude:
        raise RuntimeError("Keine Claude-Code-Installation gefunden.")
    config_dir = PROGGS / "OpenLauncher" / "Profiles" / "ClaudeCodeMac" / PROFIL
    einstellungen = TEMP / f"settings-{stempel}.json"
    daten = {"model": MODELL, "effortLevel": EFFORT}
    if mit_hooks:
        daten["hooks"] = jarvis_hooks()
    einstellungen.write_text(json.dumps(daten, ensure_ascii=False), encoding="utf-8")
    start = TEMP / f"start-{stempel}.command"
    q = shlex.quote
    start.write_text(f"""#!/bin/zsh
printf '\\033]0;%s\\a' {q(name)}
cd {q(str(PROGGS))} || exit 1
export PATH={q(umgebung['PATH'])}
{chr(10).join(f"export {n}={q(umgebung[n])}" for n in ("JAVA_HOME", "ANDROID_HOME", "ANDROID_SDK_ROOT", "ANDROID_NDK_HOME") if umgebung.get(n))}
export CLAUDE_CONFIG_DIR={q(str(config_dir))}
{f"export JARVIS_AUFTRAG_ID={q(nummer)}" if mit_hooks else ''}
SYNC="$HOME/proggs/OpenLauncher/Profiles/hooks/claude-login-sync.py"
[ -f "$SYNC" ] && python3 "$SYNC" --startup >/dev/null 2>&1
{q(claude)} --dangerously-skip-permissions --settings {q(str(einstellungen))} --model {q(MODELL)} --effort {q(EFFORT)} --remote-control {q(name)} "$(cat {q(str(auftrag_datei))})"
exec /bin/zsh -l
""", encoding="utf-8")
    start.chmod(0o755)
    ergebnis = subprocess.run(["/usr/bin/open", "-a", "Terminal", str(start)], capture_output=True, text=True, timeout=20)
    if ergebnis.returncode != 0:
        raise RuntimeError(f"Terminal.app ließ sich nicht öffnen: {ergebnis.stderr.strip()[:200]}")
    return name


RECHNER = rechnername()


def jarvis_mac_faden() -> None:
    """Startet Jarvis Mac OS auf Zuruf des Relays. Eigener Faden: Der Start darf die Abfrage der Programmieraufträge nie aufhalten."""
    pause = 2
    while True:
        try:
            zugang = lies_zugang()
            code, roh = rufe(f"https://{zugang['host']}/pc/mac-befehl", zugang["token"], 40)
            pause = 2
            if code == 204:
                continue
            if code != 200:
                # Älterer Relay ohne diese Route: nur selten nachfragen.
                time.sleep(60 if code == 404 else 10)
                continue
            befehl = json.loads(roh.decode("utf-8"))
            if befehl.get("befehl") == "jarvis_starten":
                argumente = ["/usr/bin/open"] + ([] if befehl.get("vorn") else ["-g"]) + ["-b", "de.frank.jarvismac"]
                ergebnis = subprocess.run(argumente, capture_output=True, text=True, timeout=20, stdin=subprocess.DEVNULL)
                schreib(f"Jarvis Mac OS gestartet (vorn={bool(befehl.get('vorn'))})" if ergebnis.returncode == 0
                        else f"Jarvis Mac OS nicht gestartet: {ergebnis.stderr.strip()[:200]}")
        except Exception as fehler:
            schreib(f"Mac-Befehl: {fehler}")
            time.sleep(pause)
            pause = min(pause * 2, 30)


class AnmeldungFehlt(Exception):
    pass


def main() -> int:
    ORDNER.mkdir(parents=True, exist_ok=True)
    # Nur eine Brücke je Benutzer: launchd startet bei KeepAlive neu, ein zweiter Start endet sofort.
    sperre = open(ORDNER / "bruecke.sperre", "w")
    try:
        fcntl.flock(sperre, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        return 0
    schreib(f"Brücke gestartet ({RECHNER}, Rang ersatz)")
    raeume_auf()
    threading.Thread(target=jarvis_mac_faden, daemon=True).start()
    pause = 2
    # Am Mac direkt gestartete Claude-Code-Sitzungen ohne Hooks liest jarvis_lokal.py für Jarvis mit (eigener Prozess).
    scanner = Path(__file__).resolve().with_name("jarvis_lokal.py")
    letzter_scan = 0.0
    # Neue Fassung (git pull): beenden, launchd (KeepAlive) startet die neue.
    eigener_stand = Path(__file__).stat().st_mtime
    while True:
        try:
            if Path(__file__).stat().st_mtime != eigener_stand:
                schreib("Neue Fassung, Neustart")
                return 0
            if time.time() - letzter_scan >= 30:
                letzter_scan = time.time()
                try:
                    subprocess.Popen([sys.executable, str(scanner), "scan"], stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                                     stderr=subprocess.DEVNULL)
                except Exception as fehler:
                    schreib(f"Scanner nicht gestartet: {fehler}")
            zugang = lies_zugang()
            pruefe_anmeldung_nebenher()
            code, roh = rufe(f"https://{zugang['host']}/pc/auftrag", zugang["token"], 40)
            pause = 2
            if code == 204:
                continue
            if code != 200:
                raise RuntimeError(f"HTTP {code}")
            auftrag = json.loads(roh.decode("utf-8"))
            schreib(f"Auftrag für {auftrag.get('app')}")
            try:
                # Sol 6.1 (Codex CLI) startet der OpenLauncherMac noch nicht über --jarvis-auftrag: klar ablehnen, nie still als Opus.
                if auftrag.get("modell") == "sol":
                    raise RuntimeError(f"Sol 6.1 geht bisher nur am Windows-PC, nicht auf {RECHNER}. Mit Opus 5.5 starten oder den Windows-PC einschalten.")
                # Ohne Anmeldung keine Sitzung: Sie bliebe sofort bei „Please run /login“ stehen. Das Handy weicht dann
                # nicht in die Cloud aus (anmeldung_fehlt), sondern sagt Frank, was zu tun ist. Codex, OpenCode, Kimi und
                # Antigravity haben eigene Anmeldungen: Dort zählt die von Claude Code nicht.
                if katalog_cli(auftrag) in ("", "claude") and pruefe_anmeldung(frisch=True)["stand"] == "abgemeldet":
                    raise AnmeldungFehlt(f"Claude Code auf {RECHNER} ist nicht angemeldet. Claude Code auf dem Rechner öffnen und /login ausführen, "
                                         f"danach den Auftrag neu senden.")
                name = starte_sitzung(auftrag)
                quittung = {"id": auftrag.get("id"), "ok": True, "text": f"Sitzung '{name}' gestartet"}
                schreib(f"Sitzung '{name}' gestartet")
            except AnmeldungFehlt as fehler:
                quittung = {"id": auftrag.get("id"), "ok": False, "anmeldung_fehlt": True, "text": str(fehler)}
                schreib("Nicht gestartet: nicht angemeldet")
            except Exception as fehler:
                quittung = {"id": auftrag.get("id"), "ok": False, "text": f"Start fehlgeschlagen: {fehler}"}
                schreib(quittung["text"])
            # Ohne Quittung kennt das Handy die Sitzung nicht und verwirft ihre Meldungen: kurz wiederholen.
            for versuch in range(1, 4):
                try:
                    quittung_code, _ = rufe(f"https://{zugang['host']}/pc/quittung", zugang["token"], 20, quittung)
                    # Auch HTTP-Fehler (etwa 502 beim Neustart des Relays) sind ein Fehlversuch.
                    if quittung_code >= 300:
                        raise RuntimeError(f"HTTP {quittung_code}")
                    break
                except Exception as fehler:
                    schreib(f"Quittung, Versuch {versuch}: {fehler}")
                    if versuch < 3:
                        time.sleep(2)
        except FileNotFoundError as fehler:
            # Zugang fehlt (~/SK/Jarvis nicht vom PC kopiert): selten nachsehen statt das Protokoll zu füllen.
            schreib(f"Zugang fehlt: {fehler.filename}")
            time.sleep(60)
        except Exception as fehler:
            schreib(f"Fehler: {fehler}")
            time.sleep(pause)
            pause = min(pause * 2, 10)


if __name__ == "__main__":
    sys.exit(main())
