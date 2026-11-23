"""Jarvis-Mac-Brücke: holt Programmieraufträge vom Jarvis-Relay ab und startet dafür am Mac eine Claude-Code-Sitzung
mit Remote Control. Frank sieht sie im Terminal und in der Claude-App unter Code. Gegenstück zu jarvis-pc-bruecke.ps1.

Ablauf: Dauerschleife GET https://<Server>/pc/auftrag (Long-Polling, bis 25 s). Solange das Skript fragt, gilt der Mac
beim Relay als da. Die Mac-Brücke meldet sich mit X-Jarvis-Rang: ersatz (nur der Name): Seit Franks Wahl vom 25.10.2026 hat
der Mac Vorrang, der Windows-PC bekommt Opus-Aufträge nur, wenn der Mac nicht abfragt; Sol 6.2 geht zuerst an den PC. Ist keiner da, nimmt Jarvis
die Cloud. Start: LaunchAgent de.frank.jarvis.pc-bruecke (einrichten.sh). Zugang aus ~/SK/Jarvis/relay.properties.

Sitzung: Profil Minimal, Schnellmodus, Claude Opus 5.5 (1M), Effort High, wie unter Windows. Nennt Frank in Jarvis ein
Modell aus dem OpenLauncher-Angebot (Auftrag mit modell=katalog), startet die Brücke genau das: slug, anbieter, effort,
profil, modus und cli (claude, codex, opencode, kimi, antigravity). Das können auch die Windows-Brücke und der Server (X-Jarvis-Katalog: 1).
Anmeldung: Mit jeder Abfrage meldet die Brücke den lokalen Anmeldestand von Claude Code (X-Jarvis-Anmeldung: lokal,
abgemeldet oder unbekannt, dazu die Prüfzeit). Grundlage ist `claude auth status` im Profilordner, höchstens alle fünf
Minuten und frisch vor jedem Start; gelesen wird nur „loggedIn“, nie Konto oder Token. „lokal“ heißt nur: Anmeldedaten
liegen vor. Ob sie gelten, zeigt erst eine Sitzung (der Hook meldet „Login expired“ sofort). Abgemeldet startet keine Sitzung.
Sol 6.2 (seit 13.11.2026): mit X-Jarvis-Sol: 1 springt der Mac ein, wenn der Windows-PC fehlt; gestartet über den Katalog-Weg
(gpt-6.2-sol im Codex CLI oder OpenCode). Beim Auftragsstart baut Werkzeuge/vorwaermen die Auftrags-App vor (wie am PC).
Victron: Ein Faden scannt jede Minute die Solarbatterie per Bluetooth (jarvis_victron.py) und meldet sie an den Relay,
danach den SwitchBot-CO2-Monitor (jarvis_switchbot.py).
Mac-Update-Zentrale: Aufträge des Handys (prüfen, unkritische oder kritische Updates installieren) über /pc/mac-befehl an die
Updater-Zentrale macOS (Dateien in ~/Library/Application Support/UpdaterZentrale/fern), ihr Stand geht an POST /pc/updatezentrale.
Programmier-CLI aufräumen: Auf Franks Ja (Mac-Zentrale) oder geplant (Schalter und Intervall 1–24 h in
~/Library/Application Support/JarvisPcBruecke/cli-aufraeumen.json) beendet jarvis_cli_aufraeumen.py nur eindeutig seit mehr als
einer Stunde inaktive CLI-Sitzungen; Stand und Liste gehen mit POST /pc/updatezentrale („cli“) ans Handy.
Tenda: Einmal je Stunde Funkwerte, Modus und Ping des Heimanschlusses (jarvis_tenda.py, POST /pc/tenda); Aufträge des Handys über /pc/mac-befehl.
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
import shutil
import socket
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

from jarvis_review import antigravity_review_zeile


PROFIL = "minimal"
MODUS = "schnell"
MODELL = "claude-opus-5-5[1m]"
MODELL_SOL = "gpt-6.2-sol"  # wie $ModellSol in jarvis-pc-bruecke.ps1
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


def sol_als_katalog(auftrag: dict) -> dict:
    """Sol 6.2 wie am Windows-PC (gpt-6.2-sol, Effort High, Profil Minimal, Schnellmodus, Codex CLI oder auf Franks Wahl
    OpenCode), am Mac über den Katalog-Weg des OpenLaunchers. Der Name bleibt „Sol 6.2“."""
    cli = "opencode" if str(auftrag.get("cli") or "") == "opencode" else "codex"
    return {**auftrag, "modell": "katalog", "cli": cli, "slug": MODELL_SOL, "anbieter": "openai", "effort": EFFORT,
            "profil": PROFIL, "modus": MODUS, "name": "Sol 6.2"}


def rufe(adresse: str, token: str, sekunden: int, rumpf: dict | None = None) -> tuple[int, bytes]:
    anfrage = urllib.request.Request(adresse, method="POST" if rumpf is not None else "GET",
                                     data=json.dumps(rumpf, ensure_ascii=False).encode("utf-8") if rumpf is not None else None,
                                     headers={"X-Jarvis-Token": token, "X-Jarvis-Rechner": RECHNER, "X-Jarvis-Rang": RANG,
                                              "X-Jarvis-Anmeldung": ANMELDUNG["stand"], "X-Jarvis-Anmeldung-Zeit": str(int(ANMELDUNG["zeit"])),
                                              # Diese Brücke kennt Aufträge mit frei gewähltem Modell (modell=katalog).
                                              "X-Jarvis-Katalog": "1",
                                              # Und Sol 6.2 (über Codex CLI oder OpenCode): Der Relay gibt ihn her, wenn der Windows-PC fehlt.
                                              **({"X-Jarvis-Sol": "1"} if sol_clis() else {}),
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


_SOL = {"zeit": 0.0, "clis": set()}
SOL_PRUEFEN_S = 600


def sol_clis(frisch: bool = False) -> set:
    """Womit dieser Mac Sol 6.2 starten kann: „codex“ (installiert und angemeldet) und/oder „opencode“ (installiert), nur mit dem
    Katalog-Weg des OpenLaunchers (ab 1.24.45). Höchstens alle 10 Minuten neu geprüft (nie beim Import): Eine Anmeldung, ein
    Abmelden oder ein Update gilt so ohne Neustart der Brücke. Leer = kein X-Jarvis-Sol, das Handy nimmt dann Codex Cloud."""
    if not frisch and time.time() - _SOL["zeit"] < SOL_PRUEFEN_S:
        return _SOL["clis"]
    clis = set()
    try:
        if openlauncher_kann_jarvis(OPENLAUNCHER_KATALOG_AB):
            pfad = ":".join([str(HOME / ".local" / "bin"), str(HOME / ".opencode" / "bin"), "/opt/homebrew/bin", "/usr/local/bin",
                             os.environ.get("PATH", "")])
            codex = shutil.which("codex", path=pfad)
            if codex:
                aus = subprocess.run([codex, "login", "status"], capture_output=True, text=True, timeout=20, stdin=subprocess.DEVNULL)
                if aus.returncode == 0 and "logged in" in (aus.stdout + aus.stderr).lower():
                    clis.add("codex")
            if shutil.which("opencode", path=pfad):
                clis.add("opencode")
    except Exception:
        pass
    _SOL.update(zeit=time.time(), clis=clis)
    return clis


def katalog_cli(auftrag: dict) -> str:
    """Das CLI eines Katalog-Auftrags; bei Opus/Sol leer."""
    return str(auftrag.get("cli") or "claude") if auftrag.get("modell") == "katalog" else ""


def jarvis_hooks() -> dict:
    """Dieselben Hooks wie OpenLauncherService.JarvisHooks() (Windows) bzw. OpenLauncherMac, nur für den Notweg. Den Warter vor
    jedem Werkzeug liefern bei allen dreien die Profile (nachwarter in Profiles/ClaudeCode*/…/settings.json)."""
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
        # Den Warter vor jedem Werkzeug bringt das Profil mit (nachwarter, auch der Notweg setzt CLAUDE_CONFIG_DIR aufs Profil): hier nicht doppelt.
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
        # Ohne Rückfragen: Die Sitzung meldet selbst über jarvis_hook.py melde. Antigravity hat zusätzlich Hooks (jarvis_agy_hook.py):
        # Nachträge nach dem nächsten Werkzeug, Zwischentexte und Schritte gehen wie bei Claude Code an Jarvis.
        kopf = ("Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); Frank verfolgt die Schritte in Jarvis.\n"
                f'MELDEN (statt programmier_fortschritt, das es hier nicht gibt): python3 "{HOOK}" melde <laeuft|fertig|fehler> "<ein Satz>"')
        unklar1 = "Nicht eindeutig: die wahrscheinlichste App nehmen und das in der ersten Meldung nennen."
        unklar2 = "2. Niemand antwortet in dieser Sitzung: Unklarheiten selbst sinnvoll entscheiden und in der Meldung nennen, nie auf eine Antwort warten."
        review = ("REVIEW: Ja (vor dem Abschluss den gesamten Diff gründlich selbst auf Fehler prüfen und Befunde beheben)\n"
                  if auftrag.get("review") else "REVIEW: Nein\n")
        if cli == "antigravity":
            review = antigravity_review_zeile(auftrag)
    return f"""{kopf}

APP: {app}
{review}AUFTRAG:
{text}

VORGEHEN
1. App im Repo finden. Bekannt: Geniale Aufgaben = Aufgaben, Genialer Wecker = GenialerWecker (die Store-Fassung GenialerWeckerAndroid nur, wenn Frank sie ausdrücklich meint), Geniale Ideen = GenialeIdeen, Entropie Reductor = EntropieReductor, Jarvis = Jarvis, UpdateStation = UpdateStation. {unklar1}
{unklar2}
3. Nach den Profilregeln vollständig abschließen: bauen, Version, Commit, Push, Installation per WLAN (die App Jarvis nie mit nacktem adb install, sondern immer über Werkzeuge/jarvis-install, das vorher per Overlay nach dem Zeitpunkt fragt), bei Android-Apps Skill apk-update. Abschlussmeldung auf Deutsch.
"""


def vorbau(app: str) -> None:
    """Auftrags-App sofort vorbauen (wärmt den Gradle-Daemon, während die Sitzung noch liest), wie am PC und am Server. Erst nach
    dem Start der Sitzung, damit ein abgelehnter oder gescheiterter Auftrag keinen Bau auslöst; Sitzungsbauten warten per Hook darauf.
    vorwaermen.py wartet selbst 25 s auf den Repo-Abgleich des OpenLaunchers und setzt Java und SDK selbst."""
    try:
        vorwaermen = PROGGS / "Werkzeuge" / "vorwaermen" / "vorwaermen.py"
        if vorwaermen.is_file() and (PROGGS / app / "app" / "build.gradle.kts").is_file():
            subprocess.Popen([sys.executable, str(vorwaermen), "lauf", "--app", app], cwd=str(PROGGS),
                             stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
    except Exception as fehler:
        schreib(f"Vorwärmen: {fehler}")


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
        import jarvis_anhaenge
        zugang = lies_zugang()
        auftrag = {**auftrag, "auftrag": (auftrag.get("auftrag") or "") + jarvis_anhaenge.fuer_auftrag(auftrag, zugang["host"], zugang["token"], cli in ("", "claude") and mit_hooks)}
    # Handy und Tablet sofort prüfen und nach einem Geräte-Neustart wieder verbinden (Wachhund, im Hintergrund);
    # nur bei Android-Apps (gradle-Projekt im Repo).
    try:
        if (PROGGS / app / "app" / "build.gradle.kts").is_file():
            subprocess.Popen(["bash", str(PROGGS / "Werkzeuge" / "adb-wlan" / "adb-wlan.sh"), "--leise", "--geraet", "alle"],
                             stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
    except Exception:
        pass
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
            vorbau(app)
            return f"{name} (Start unbestätigt: der OpenLauncher war nach 30 Sekunden noch nicht fertig)"
        if code != 0:
            raise RuntimeError(f"Der OpenLauncher konnte die Sitzung nicht starten (Code {code}, siehe OpenLauncher-Log).")
        vorbau(app)
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
    vorbau(app)
    return name


RECHNER = rechnername()


def lies_rang() -> str:
    """Platz beim Relay. Standard „ersatz“; eine lokale Einstellung ~/Library/Application Support/JarvisPcBruecke/rechner.json
    ({"rang": "..."}, geschrieben von einrichten.sh --rang ...) kann ihn ändern, wie rechner.json der Windows-Brücke.
    Erlaubt sind nur „ersatz“ und „power“ (der Relay lehnt unbekannte Ränge ab). Fehlt oder taugt sie nicht, bleibt es bei
    „ersatz“, laut im Protokoll."""
    datei = HOME / "Library" / "Application Support" / "JarvisPcBruecke" / "rechner.json"
    try:
        rang = str(json.loads(datei.read_text(encoding="utf-8-sig")).get("rang") or "").strip().lower()
    except FileNotFoundError:
        return "ersatz"
    except Exception as fehler:
        schreib(f"rechner.json unlesbar ({fehler}), Rang ersatz")
        return "ersatz"
    if rang in RAENGE:
        return rang
    schreib(f"ACHTUNG: Rang '{rang}' in rechner.json unbekannt (erlaubt: {', '.join(sorted(RAENGE))}), Rang ersatz")
    return "ersatz"


RAENGE = {"ersatz", "power"}


RANG = lies_rang()
# Ein Bluetooth-Scan zur Zeit: Victron, SwitchBot im Takt und SwitchBot auf Knopfdruck (/geraet/switchbot-jetzt) wechseln sich ab.
BLUETOOTH = threading.Lock()
SWITCHBOT_SOFORT = threading.Event()
SWITCHBOT_ZULETZT = [0.0]   # Ende des letzten Scans mit Meldung (Takt oder Knopfdruck), damit keiner gleich danach doppelt scannt
SWITCHBOT_ERSPART_S = 15    # so frisch gemeldet, erspart sich der Knopfdruck einen eigenen Scan (der Relay hat die Werte schon)


def switchbot_lesen(schnell: bool = False) -> int | None:
    """Ein Lauf von jarvis_switchbot.py (schnell: bis 30 s, endet 1 s nach dem CO2-Monitor, frühestens nach 8 s; für den
    Aktualisieren-Knopf am Handy, meldet auch „nichts gehört“). Rückgabe: Exit-Code; None = beim Knopfdruck erspart, weil eben gemeldet."""
    skript = Path(__file__).resolve().with_name("jarvis_switchbot.py")
    sb_log = ORDNER / "switchbot.log"
    with BLUETOOTH:
        if schnell and time.time() - SWITCHBOT_ZULETZT[0] < SWITCHBOT_ERSPART_S:
            return None
        if sb_log.exists() and sb_log.stat().st_size > 200_000:
            sb_log.write_text("", "utf-8")
        with open(sb_log, "a", encoding="utf-8") as fehlerlog:
            # Scan bis 30 s + 5 s Nachlauf + Meldung bis 15 s + Start: 65 s reichen sicher.
            code = subprocess.run([sys.executable, str(skript)] + (["schnell"] if schnell else []), cwd=str(skript.parent), stdin=subprocess.DEVNULL,
                                  stdout=subprocess.DEVNULL, stderr=fehlerlog, timeout=65).returncode
        if code == 0:
            SWITCHBOT_ZULETZT[0] = time.time()
        return code


def switchbot_sofort_faden() -> None:
    """Liest auf Zuruf des Handys sofort (Frank, 16.11.2026: Aktualisieren soll die Werte des Macs in Echtzeit holen).
    Zurufe während des Scans (der Relay wiederholt seinen Befehl) gehen im laufenden Scan auf: erst danach wieder hinhören."""
    while True:
        SWITCHBOT_SOFORT.wait()
        try:
            code = switchbot_lesen(schnell=True)
            schreib("SwitchBot auf Knopfdruck: eben gemeldet, kein eigener Scan" if code is None else f"SwitchBot auf Knopfdruck gelesen (Exit {code})")
        except Exception as fehler:
            schreib(f"SwitchBot auf Knopfdruck: {fehler}")
        finally:
            SWITCHBOT_SOFORT.clear()


LICHT_LAUF = {"prozess": None}
LICHT_SPERRE = threading.Lock()


def licht_schalten(befehle: dict, kennung: str = "") -> None:
    """LED-Streifen auf Zuruf des Handys (POST /geraet/licht) per Bluetooth schalten (jarvis_licht.py) und das Ergebnis an
    POST /pc/licht melden. Bewusst ohne die BLUETOOTH-Sperre: Licht soll sofort gehen, nicht erst nach einem Victron-Scan."""
    skript = Path(__file__).resolve().with_name("jarvis_licht.py")
    try:
        # Nur ein Licht-Lauf zur Zeit: Ein neuer Befehl beendet den laufenden, sonst könnte ein veralteter Auftrag zuletzt schreiben.
        # Leere Befehle (nur „_namen“): laufenden Lauf abbrechen, kein neuer (das Handy stellt gerade live ein).
        abbrechen = not any(isinstance(v, list) and v for k, v in befehle.items() if not k.startswith("_"))
        with LICHT_SPERRE:
            alt = LICHT_LAUF["prozess"]
            if kennung and kennung == LICHT_LAUF.get("kennung") and alt is not None and alt.poll() is None:
                return  # derselbe Auftrag kam erneut (verlorene Antwort am Relay): der Lauf ist schon unterwegs
            if alt is not None and alt.poll() is None:
                alt.kill()
            LICHT_LAUF["kennung"] = kennung
            prozess = None if abbrechen else subprocess.Popen([sys.executable, str(skript), json.dumps(befehle)], cwd=str(skript.parent),
                                                              stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            LICHT_LAUF["prozess"] = prozess
        if prozess is not None and kennung:
            try:
                zugang = lies_zugang()
                rufe(f"https://{zugang['host']}/pc/licht", zugang["token"], 10, {"angenommen": True, "kennung": kennung})
            except Exception as fehler:
                schreib(f"Licht annehmen: {fehler}")
        if prozess is None:
            zugang = lies_zugang()
            rufe(f"https://{zugang['host']}/pc/licht", zugang["token"], 15, {"geschaltet": [], "gesehen": [], "abgebrochen": True, "kennung": kennung})
            schreib("Licht: laufender Auftrag abgebrochen (Handy stellt live ein)")
            return
        try:
            ausgabe, fehlertext = prozess.communicate(timeout=30)
        except subprocess.TimeoutExpired:
            prozess.kill()
            ausgabe, fehlertext = prozess.communicate()
        if prozess.returncode in (-9, -15) and LICHT_LAUF["prozess"] is not prozess:
            return  # von einem neueren Befehl abgelöst: nichts melden
        lauf = subprocess.CompletedProcess(prozess.args, prozess.returncode, ausgabe, fehlertext)
        ergebnis = json.loads(lauf.stdout.strip().splitlines()[-1]) if lauf.returncode == 0 and lauf.stdout.strip() else \
            {"geschaltet": [], "gesehen": [], "fehler": (lauf.stderr.strip() or f"Exit {lauf.returncode}")[-300:]}
    except Exception as fehler:
        ergebnis = {"geschaltet": [], "gesehen": [], "fehler": str(fehler)[:300]}
    schreib(f"Licht: {len(ergebnis.get('geschaltet', []))} von {len(ergebnis.get('gesehen', []))} geschaltet" +
            (f" ({ergebnis['fehler']})" if ergebnis.get("fehler") else ""))
    try:
        zugang = lies_zugang()
        rufe(f"https://{zugang['host']}/pc/licht", zugang["token"], 15, {**ergebnis, "kennung": kennung})
    except Exception as fehler:
        schreib(f"Licht melden: {fehler}")


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
            if befehl.get("befehl") == "licht":
                threading.Thread(target=licht_schalten, args=(befehl.get("befehle") or {}, str(befehl.get("kennung") or "")), daemon=True).start()
            elif befehl.get("befehl") == "tenda":
                threading.Thread(target=tenda_befehl, args=(befehl,), daemon=True).start()
            elif befehl.get("befehl") == "updatezentrale":
                threading.Thread(target=uz_befehl, args=(befehl,), daemon=True).start()
            elif befehl.get("befehl") == "switchbot_lesen":
                SWITCHBOT_SOFORT.set()   # eigener Faden: der Scan darf die Abfrage nie aufhalten
            elif befehl.get("befehl") == "jarvis_starten":
                argumente = ["/usr/bin/open"] + ([] if befehl.get("vorn") else ["-g"]) + ["-b", "de.frank.jarvismac"]
                ergebnis = subprocess.run(argumente, capture_output=True, text=True, timeout=20, stdin=subprocess.DEVNULL)
                schreib(f"Jarvis Mac OS gestartet (vorn={bool(befehl.get('vorn'))})" if ergebnis.returncode == 0
                        else f"Jarvis Mac OS nicht gestartet: {ergebnis.stderr.strip()[:200]}")
        except Exception as fehler:
            schreib(f"Mac-Befehl: {fehler}")
            time.sleep(pause)
            pause = min(pause * 2, 30)


def victron_faden() -> None:
    """Victron-Anlage (Batterie, Laderegler) per Bluetooth wie die PC-Brücke unter Windows: jede Minute ein kurzer Scan (bis 12 s),
    der Stand geht an den Relay (jarvis_victron.py, POST /pc/victron). Nur mit ~/SK/Victron/victron.json; eigener Prozess, damit
    ein hängender Bluetooth-Stack die Brücke nie aufhält. Fehler stehen in ~/Library/Logs/JarvisPcBruecke/victron.log."""
    skript = Path(__file__).resolve().with_name("jarvis_victron.py")
    fehlte = False
    switchbot_gemeldet = 0.0
    while True:
        beginn = time.time()
        if (HOME / "SK" / "Victron" / "victron.json").is_file():
            fehlte = False
            try:
                # Fehler vor dem eigenen Protokoll (fehlendes bleak, Importfehler) landen so trotzdem in victron.log.
                log_datei = ORDNER / "victron.log"
                if log_datei.exists() and log_datei.stat().st_size > 200_000:
                    log_datei.write_text("", "utf-8")  # wie protokoll() im Skript: nie unbegrenzt wachsen
                with BLUETOOTH, open(log_datei, "a", encoding="utf-8") as fehlerlog:
                    subprocess.run([sys.executable, str(skript)], cwd=str(skript.parent), stdin=subprocess.DEVNULL,
                                   stdout=subprocess.DEVNULL, stderr=fehlerlog, timeout=50)
            except Exception as fehler:
                schreib(f"Victron: {fehler}")
        elif not fehlte:
            fehlte = True
            schreib("Victron: ~/SK/Victron/victron.json fehlt, die Solarbatterie wird vom Mac nicht gemeldet")
        # Danach, nie gleichzeitig (ein Bluetooth-Scan zur Zeit): SwitchBot-CO2-Monitor, den der Hub Mini nicht in die Cloud bringt
        # (jarvis_switchbot.py, POST /pc/switchbot). Alle fünf Minuten (so oft misst er höchstens), ohne Empfang in der nächsten Minute erneut.
        # Ein Scan auf Knopfdruck zählt mit: danach nicht gleich noch einmal.
        if time.time() - max(switchbot_gemeldet, SWITCHBOT_ZULETZT[0]) >= 300:
            try:
                # Nur „nichts gehört“ (Exit 3) lohnt einen neuen Scan in der nächsten Minute; andere Fehler (Zugang, Relay) behebt ein Scan nicht.
                if switchbot_lesen() != 3:
                    switchbot_gemeldet = time.time()
            except Exception as fehler:
                switchbot_gemeldet = time.time()
                schreib(f"SwitchBot: {fehler}")
        time.sleep(max(5, 60 - (time.time() - beginn)))


# ---------------------------------------------------------------- Mac-Update-Zentrale (Updater-Zentrale macOS)

UZ_ORDNER = HOME / "Library" / "Application Support" / "UpdaterZentrale" / "fern"
UZ_BUNDLE = "de.frank.UpdaterZentrale"
UZ_GESEHEN: list = []
UZ_SPERRE = threading.Lock()
UZ_SOFORT = threading.Event()


def uz_melden(rumpf: dict, versuche: int = 1) -> bool:
    for i in range(versuche):
        try:
            zugang = lies_zugang()
            code, _ = rufe(f"https://{zugang['host']}/pc/updatezentrale", zugang["token"], 15, rumpf)
            if code == 200:
                return True
        except Exception:
            pass
        if i + 1 < versuche:
            time.sleep(3)
    return False


def uz_stand() -> tuple[dict | None, bool]:
    """Letzter Stand der App und ob sie gerade lebt (Lebenszeichen alle 5 s, Prozess vorhanden)."""
    try:
        stand = json.loads((UZ_ORDNER / "stand.json").read_text("utf-8"))
    except Exception:
        return None, False
    lebt = False
    try:
        os.kill(int(stand.get("pid") or 0), 0)
        lebt = time.time() - float(stand.get("zeit") or 0) < 20
    except Exception:
        pass
    return stand, lebt


def uz_befehl(befehl: dict) -> None:
    """Auftrag des Handys: Empfang sofort bestätigen (der Relay gibt sonst erneut aus), jede Kennung nur einmal, App bei Bedarf
    im Hintergrund starten (sie prüft dann von selbst), Auftrag als Datei ablegen. Die App weist doppelte Installationen selbst ab."""
    kennung, art = str(befehl.get("kennung") or ""), str(befehl.get("art") or "")
    with UZ_SPERRE:
        if kennung in UZ_GESEHEN:
            uz_melden({"kennung": kennung, "empfangen": True})
            return
        UZ_GESEHEN.append(kennung)
        del UZ_GESEHEN[:-50]
    uz_melden({"kennung": kennung, "empfangen": True}, versuche=3)
    if art in ("cli_aufraeumen", "cli_einstellung"):
        cli_befehl(kennung, art, befehl)
        return
    if art not in ("pruefen", "unkritisch", "kritisch"):
        uz_melden({"kennung": kennung, "fehler": "Diese Mac-Brücke kennt den Auftrag nicht; bitte die Brücke aktualisieren."}, versuche=3)
        return
    try:
        (UZ_ORDNER / "auftraege").mkdir(parents=True, exist_ok=True)
        _, lebt = uz_stand()
        if not lebt:
            ergebnis = subprocess.run(["/usr/bin/open", "-g", "-b", UZ_BUNDLE], capture_output=True, text=True, timeout=20, stdin=subprocess.DEVNULL)
            schreib("Updater-Zentrale gestartet" if ergebnis.returncode == 0 else f"Updater-Zentrale nicht gestartet: {ergebnis.stderr.strip()[:200]}")
            if ergebnis.returncode != 0:
                uz_melden({"kennung": kennung, "fehler": "Die Update-Zentrale ließ sich am Mac nicht starten."}, versuche=3)
                return
        zwischen = UZ_ORDNER / "auftraege" / f".{kennung}.neu"
        zwischen.write_text(json.dumps({"kennung": kennung, "art": art}), "utf-8")
        os.replace(zwischen, UZ_ORDNER / "auftraege" / f"{kennung}.json")
        schreib(f"Update-Zentrale: Auftrag {art} ({kennung})")
    except Exception as fehler:
        schreib(f"Update-Zentrale: {fehler}")
        uz_melden({"kennung": kennung, "fehler": f"Auftrag nicht abgelegt ({type(fehler).__name__})."}, versuche=3)
    UZ_SOFORT.set()


def uz_faden() -> None:
    """Reicht den Stand der App an den Relay weiter: bei jeder Änderung (höchstens alle 2 s), sonst alle 30 s während eines
    Laufs und alle 10 Minuten in Ruhe. Beendet sich die App, einmal mit app=false."""
    zuletzt, zuletzt_zeit = "", 0.0
    while True:
        UZ_SOFORT.wait(2)
        UZ_SOFORT.clear()
        try:
            stand, lebt = uz_stand()
            if stand is None:
                continue
            vergleich = json.dumps({k: v for k, v in stand.items() if k != "zeit"}, sort_keys=True) + str(lebt)
            laeuft = lebt and (stand.get("laeuft") or (stand.get("vorgang") or {}).get("phase") == "laeuft")
            alter = time.time() - zuletzt_zeit
            if vergleich != zuletzt or alter >= (30 if laeuft else 600):
                if uz_melden({"stand": stand, "app": lebt}):
                    zuletzt, zuletzt_zeit = vergleich, time.time()
        except Exception as fehler:
            schreib(f"Update-Zentrale melden: {fehler}")
            time.sleep(10)


# ---------------------------------------------------------------- Programmier-CLI aufräumen (jarvis_cli_aufraeumen.py)

CLI_STAND: dict = {"laeuft": False, "letzter": None, "sitzungen": None, "laeufe": 0, "kennung": None}


def cli_melden() -> bool:
    """Stand an den Relay. „kennung“ = zuletzt umgesetzte Einstellung: daran erkennt das Handy die Bestätigung (ohne Uhrvergleich)."""
    import jarvis_cli_aufraeumen as ca
    e = ca.lies_einstellung()
    stand = {"automatik": e["automatik"], "stunden": e["stunden"], "naechster": e.get("naechster") if e["automatik"] else None,
             "laeuft": CLI_STAND["laeuft"], "letzter": e.get("letzter"), "sitzungen": CLI_STAND["sitzungen"],
             "kennung": CLI_STAND["kennung"], "zeit": time.time()}
    return uz_melden({"cli": stand}, versuche=3)


def cli_lauf(art: str, kennung: str = "") -> None:
    """Ein Aufräumen (von Hand nach Franks Ja oder geplant); nie zwei gleichzeitig (Sperre im Modul)."""
    import jarvis_cli_aufraeumen as ca
    CLI_STAND["laeuft"] = True
    CLI_STAND["laeufe"] += 1
    cli_melden()
    # Lebenszeichen alle vier Minuten (Review 21.11.2026): Die Karte hält einen Stand nach zehn Minuten für veraltet, zeigte bei
    # einem langen Aufräumen „läuft nicht“ und gab den Knopf frei, obwohl der zweite Auftrag dann abgewiesen wurde.
    puls_ende = threading.Event()

    def puls() -> None:
        while not puls_ende.wait(240):
            try:
                cli_melden()
            except Exception:
                pass
    puls_faden = threading.Thread(target=puls, daemon=True)
    puls_faden.start()
    try:
        try:
            ergebnis = ca.aufraeumen(art)
            schreib(f"CLI-Aufräumen ({art}): {ergebnis['text']}")
            CLI_STAND["sitzungen"] = ergebnis["offen"]
        except Exception as fehler:
            schreib(f"CLI-Aufräumen: {type(fehler).__name__}: {fehler}")
            ergebnis = {"art": art, "zeit": time.time(), "text": f"Aufräumen fehlgeschlagen ({type(fehler).__name__}), nichts weiter beendet.",
                        "beendet": [], "offen": CLI_STAND["sitzungen"] or [], "fehler": True}
            if kennung:
                uz_melden({"kennung": kennung, "fehler": ergebnis["text"]}, versuche=3)
        with ca.EINSTELLUNG_SPERRE:
            e = ca.lies_einstellung()
            e["letzter"] = ergebnis
            # Auch nach einem Fehler erst im nächsten Intervall erneut, nie im Minutentakt.
            if art == "auto" and e["automatik"]:
                e["naechster"] = time.time() + e["stunden"] * 3600
            ca.schreib_einstellung(e)
    except Exception as fehler:
        schreib(f"CLI-Aufräumen (Abschluss): {type(fehler).__name__}: {fehler}")
    finally:
        puls_ende.set()
        CLI_STAND["laeuft"] = False
        # Ein Puls mitten im Senden (bis zu drei Versuche) käme sonst mit „läuft“ nach der Schlussmeldung an.
        puls_faden.join(timeout=150)
        cli_melden()


def cli_befehl(kennung: str, art: str, befehl: dict) -> None:
    import jarvis_cli_aufraeumen as ca
    if art == "cli_aufraeumen":
        with UZ_SPERRE:
            besetzt = CLI_STAND["laeuft"]
            CLI_STAND["laeuft"] = True
        if besetzt:
            uz_melden({"kennung": kennung, "fehler": "Ein Aufräumen läuft bereits."}, versuche=3)
            return
        threading.Thread(target=cli_lauf, args=("hand", kennung), daemon=True).start()
        return
    try:
        automatik = befehl.get("automatik")
        stunden = befehl.get("stunden")
        ca.setze_einstellung(automatik if isinstance(automatik, bool) else None, stunden if stunden is not None else None)
        schreib(f"CLI-Aufräumen: Automatik {automatik}, Intervall {stunden}")
        CLI_STAND["kennung"] = kennung
    except Exception as fehler:
        uz_melden({"kennung": kennung, "fehler": f"Einstellung nicht gespeichert ({fehler})."}, versuche=3)
    cli_melden()


def cli_faden() -> None:
    """Geplantes Aufräumen nur bei eingeschalteter Automatik (nach Ruhezustand höchstens ein nachgeholter Lauf), dazu alle
    fünf Minuten die Liste der offenen Sitzungen für die Karte."""
    import jarvis_cli_aufraeumen as ca
    liste_zeit = 0.0
    while True:
        try:
            with UZ_SPERRE:
                los = not CLI_STAND["laeuft"] and ca.faellig(ca.lies_einstellung(), time.time())
                if los:
                    CLI_STAND["laeuft"] = True
            if los:
                cli_lauf("auto")
                liste_zeit = time.time()
            elif not CLI_STAND["laeuft"] and time.time() - liste_zeit >= 300:
                liste_zeit = time.time()
                nr = CLI_STAND["laeufe"]
                liste = [ca.kurz(x) for x in ca.sitzungen()]
                # Hat inzwischen ein Aufräumen begonnen, ist diese Messung älter als sein Ergebnis: verwerfen.
                if CLI_STAND["laeufe"] == nr and not CLI_STAND["laeuft"]:
                    CLI_STAND["sitzungen"] = liste
                    cli_melden()
        except Exception as fehler:
            schreib(f"CLI-Aufräumen (Zeitplan): {type(fehler).__name__}: {fehler}")
        time.sleep(60)


# ---------------------------------------------------------------- Tenda-Mobilfunkrouter (jarvis_tenda.py)

def tenda_melden(rumpf: dict, versuche: int = 1) -> bool:
    """POST /pc/tenda. Nach einem Moduswechsel oder Neustart ist das Internet kurz weg: dann bis zu fünf Minuten nachliefern."""
    for i in range(versuche):
        try:
            zugang = lies_zugang()
            code, _ = rufe(f"https://{zugang['host']}/pc/tenda", zugang["token"], 15, rumpf)
            if code == 200:
                return True
        except Exception:
            pass
        if i + 1 < versuche:
            time.sleep(10)
    return False


TENDA_GESEHEN: list = []
TENDA_SPERRE = threading.Lock()
TENDA_SCHALTET = [0]   # laufende Schreibaufträge: solange kein Neustart der Brücke bei neuer Fassung (sonst bliebe etwa Connect halb getrennt)


def tenda_befehl(befehl: dict) -> None:
    """Auftrag des Handys über den Relay: lesen (frische Momentabfrage), zugang (Hash aus dem Einrichtungsdialog, nie protokolliert),
    modus/verbinden/neustart (je genau ein Schreibvorgang, danach Zustand prüfen)."""
    import jarvis_tenda
    kennung, art, wert = str(befehl.get("kennung") or ""), str(befehl.get("art") or ""), str(befehl.get("wert") or "")
    # Der Relay gibt einen unquittierten Auftrag nach 8 s erneut aus: jede Kennung nur einmal ausführen, Empfang sofort bestätigen.
    schreibend = art in ("modus", "verbinden", "neustart")
    with TENDA_SPERRE:
        if kennung in TENDA_GESEHEN:
            tenda_melden({"kennung": kennung, "empfangen": True})
            return
        TENDA_GESEHEN.append(kennung)
        del TENDA_GESEHEN[:-50]
        # Ab hier bis zur Ergebnismeldung keine Selbst-Neustarts der Brücke (unter der Sperre, nicht atomar sonst).
        if schreibend:
            TENDA_SCHALTET[0] += 1
    try:
        tenda_melden({"kennung": kennung, "empfangen": True})
        if art == "zugang":
            jarvis_tenda.zugang_speichern(wert)
            schreib("Tenda: neuer Zugang gespeichert")
            art = "lesen"
        if art == "lesen":
            stand = jarvis_tenda.lesen()
            tenda_melden({"kennung": kennung, "ergebnis": "fertig", "stand": stand})
            return
        tenda_melden({"kennung": kennung, "angenommen": True})
        schreib(f"Tenda: {art} {wert}".strip())
        ergebnis = jarvis_tenda.aktion(art, wert)
        schreib(f"Tenda: {art} → {ergebnis.get('ergebnis')}")
        tenda_melden({"kennung": kennung, "aktion": art, "wert": wert, **ergebnis}, versuche=30)
    except jarvis_tenda.TendaFehler as fehler:
        # Nach „angenommen“ wartet das Handy auf ein Aktionsergebnis: dann mit „aktion“ melden, sonst sähe es keins.
        tenda_melden({"kennung": kennung, "ergebnis": "fehler", "text": str(fehler), "art": fehler.art, **({"aktion": art} if schreibend else {})},
                     versuche=30 if schreibend else 1)
    except Exception as fehler:
        schreib(f"Tenda: {type(fehler).__name__}")
        tenda_melden({"kennung": kennung, "ergebnis": "fehler", "text": f"Router nicht erreichbar ({type(fehler).__name__}).",
                      **({"aktion": art} if schreibend else {})}, versuche=30 if schreibend else 1)
    finally:
        if schreibend:
            with TENDA_SPERRE:
                TENDA_SCHALTET[0] -= 1


def tenda_faden() -> None:
    """Einmal je Stunde Funkwerte, Modus und Ping des Heimanschlusses an den Relay (Startkarte, Live-Gedächtnis, Mobilfunk-Abruf)."""
    import jarvis_tenda
    # Nach einem Neustart der Brücke (jede neue Fassung) nicht sofort neu lesen: erst eine Stunde nach dem letzten Abruf.
    zuletzt = ORDNER / "tenda-zuletzt.txt"
    try:
        time.sleep(max(60, 3600 - (time.time() - float(zuletzt.read_text()))))
    except Exception:
        time.sleep(60)
    while True:
        beginn = time.time()
        try:
            zuletzt.write_text(str(beginn))
        except Exception:
            pass
        if jarvis_tenda.zugang_lesen().get("hash"):
            try:
                tenda_melden({"stand": jarvis_tenda.lesen()})
            except jarvis_tenda.TendaFehler as fehler:
                tenda_melden({"fehler": str(fehler), "art": fehler.art, "zeit": int(time.time())})
            except Exception as fehler:
                tenda_melden({"fehler": f"Router nicht erreichbar ({type(fehler).__name__}).", "zeit": int(time.time())})
        # Stündlich genügt (Franks Wunsch vom 20.11.2026): Frisch liest ohnehin jeder Tipp auf Aktualisieren und jede Frage.
        time.sleep(max(30, 3600 - (time.time() - beginn)))


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
    schreib(f"Brücke gestartet ({RECHNER}, Rang {RANG})")
    raeume_auf()
    threading.Thread(target=jarvis_mac_faden, daemon=True).start()
    threading.Thread(target=victron_faden, daemon=True).start()
    threading.Thread(target=switchbot_sofort_faden, daemon=True).start()
    threading.Thread(target=tenda_faden, daemon=True).start()
    threading.Thread(target=uz_faden, daemon=True).start()
    threading.Thread(target=cli_faden, daemon=True).start()
    pause = 2
    # Am Mac direkt gestartete Claude-Code-Sitzungen ohne Hooks liest jarvis_lokal.py für Jarvis mit (eigener Prozess).
    scanner = Path(__file__).resolve().with_name("jarvis_lokal.py")
    letzter_scan = 0.0
    # Neue Fassung (git pull): beenden, launchd (KeepAlive) startet die neue.
    eigener_stand = Path(__file__).stat().st_mtime
    # Auch eine neue Fassung von jarvis_tenda.py: Der Faden hält das Modul im Speicher.
    tenda_datei = Path(__file__).resolve().with_name("jarvis_tenda.py")
    tenda_stand = tenda_datei.stat().st_mtime if tenda_datei.exists() else 0
    while True:
        try:
            if (Path(__file__).stat().st_mtime != eigener_stand or (tenda_datei.exists() and tenda_datei.stat().st_mtime != tenda_stand)) \
                    and not TENDA_SCHALTET[0]:
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
                # Sol 6.2: über den Katalog-Weg (Codex CLI oder OpenCode); ohne passendes CLI klar ablehnen, nie still als Opus.
                if auftrag.get("modell") == "sol":
                    auftrag = sol_als_katalog(auftrag)
                    if auftrag["cli"] not in sol_clis(frisch=True):
                        grund = "nicht angemeldet (codex login)" if auftrag["cli"] == "codex" and shutil.which("codex", path=frische_umgebung()["PATH"]) \
                            else "nicht installiert"
                        raise RuntimeError(f"Sol 6.2 im {auftrag['cli']} CLI geht auf {RECHNER} nicht: {grund}.")
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
