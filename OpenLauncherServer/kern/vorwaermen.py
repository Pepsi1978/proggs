"""Vorwärmen der Android-Apps auf dem Hostinger-Server.

Ein Bau auf dem Server ist nur dann schnell, wenn der build-Ordner der App schon zum aktuellen Stand passt: Dann übersetzt
Gradle nur, was der Auftrag selbst ändert. Dieses Skript baut deshalb jede App, die Frank auf dem Handy hat, im Hintergrund
nach, sobald sich ihr Ordner im Repo geändert hat (Auslöser: Repo-Abgleich alle 5 Minuten), und beim Start eines
Programmierauftrags sofort die genannte App (dann wird auch der Gradle-Daemon warm, während die Sitzung noch liest).

Regeln:
- Immer nur ein Bau: Jeder Bau hält ~/.openlauncher/vorwaermen/gradle-bau.lock. Sitzungsbauten warten über den Bau-Hook auf
  dieselbe Sperre und legen die Datei pause an; dann hört das Vorwärmen nach der laufenden App auf. Ein laufender Bau wird nie
  abgebrochen (ein abgebrochener Client nimmt den Daemon mit).
- Apps mit offenen Änderungen im Arbeitsbaum (eine Sitzung arbeitet daran) werden übersprungen.
- Normale Priorität: nice würde an die gemeinsam genutzten Daemons vererbt und auch Sitzungsbauten bremsen.

Aufrufe: vorwaermen.py start [--app ORDNER] [--immer]   (löst sich vom Aufrufer, kehrt sofort zurück)
         vorwaermen.py lauf  [--app ORDNER] [--immer]   (arbeitet im Vordergrund)
         vorwaermen.py stand                             (zeigt, was vorgewärmt ist)
"""
import argparse, fcntl, json, os, re, shlex, subprocess, sys, time
from pathlib import Path

PROGGS = Path.home() / "proggs"
ORDNER = Path.home() / ".openlauncher" / "vorwaermen"
STAND = ORDNER / "stand.json"
PAKETE = ORDNER / "pakete.txt"
LOG = ORDNER / "log.txt"
BAU_SPERRE = ORDNER / "gradle-bau.lock"
LAUF_SPERRE = ORDNER / "lauf.lock"
PAUSE = ORDNER / "pause"
HANDY = "10.8.0.7:5555"
# Werden von hier nie installiert (fehlende Schlüssel oder nur am Windows-PC), Vorwärmen wäre nur Last.
AUSSCHLUSS = {"GenialerWeckerAndroid", "BestJournalAndroid", "EntropieReductor"}
# Namen aus Jarvis-Aufträgen, die nicht dem Ordner entsprechen.
NAMEN = {"geniale aufgaben": "Aufgaben", "genialer wecker": "GenialerWeckerAndroid", "geniale ideen": "GenialeIdeen",
         "stack-labor": "StackLabor", "stack labor": "StackLabor", "entropie reductor": "EntropieReductor"}
PAUSE_SEKUNDEN = 20 * 60
# Der Timer-Dienst startet ohne Franks Shell-Profil: dieselbe Umgebung wie in den Sitzungen setzen (~/.profile).
SDK = str(Path.home() / "android-sdk")
os.environ.setdefault("ANDROID_HOME", SDK)
os.environ.setdefault("ANDROID_SDK_ROOT", SDK)
os.environ.setdefault("TZ", "Europe/Berlin")
os.environ["PATH"] = f"{SDK}/platform-tools:{Path.home()}/.local/bin:" + os.environ.get("PATH", "/usr/bin:/bin")
VERSUCHE = 2  # so oft wird derselbe Stand nach einem Fehlschlag erneut gebaut


def log(text: str) -> None:
    ORDNER.mkdir(parents=True, exist_ok=True)
    with open(LOG, "a", encoding="utf-8") as f:
        f.write(time.strftime("%d.%m.%Y %H:%M:%S ") + text + "\n")
    # Log klein halten.
    if LOG.stat().st_size > 400_000:
        zeilen = LOG.read_text(encoding="utf-8").splitlines()[-1500:]
        LOG.write_text("\n".join(zeilen) + "\n", encoding="utf-8")


def git(*args: str) -> str:
    return subprocess.run(["git", "-C", str(PROGGS), *args], capture_output=True, text=True, timeout=120, stdin=subprocess.DEVNULL).stdout.strip()


def lies_stand() -> dict:
    try:
        return json.loads(STAND.read_text(encoding="utf-8"))
    except Exception:
        return {}


def schreib_stand(stand: dict) -> None:
    neu = STAND.with_suffix(".neu")
    neu.write_text(json.dumps(stand, ensure_ascii=False, indent=1), encoding="utf-8")
    neu.replace(STAND)


def app_id(app: str) -> str:
    try:
        text = (PROGGS / app / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    except OSError:
        return ""
    m = re.search(r'applicationId\s*=\s*"([^"]+)"', text)
    return m.group(1) if m else ""


def handy_pakete() -> set:
    """Installierte Pakete am Handy; ist es gerade nicht erreichbar, gilt die zuletzt gemerkte Liste."""
    try:
        aus = subprocess.run(["adb", "-s", HANDY, "shell", "pm", "list", "packages", "-3"], capture_output=True, text=True, timeout=20)
        pakete = {z.split(":", 1)[1].strip() for z in aus.stdout.splitlines() if z.startswith("package:")}
        if aus.returncode == 0 and pakete:
            ORDNER.mkdir(parents=True, exist_ok=True)
            PAKETE.write_text("\n".join(sorted(pakete)) + "\n", encoding="utf-8")
            return pakete
    except Exception:
        pass
    try:
        return set(PAKETE.read_text(encoding="utf-8").split())
    except OSError:
        return set()


def alle_apps() -> list:
    """Android-Apps im Repo, die Frank auf dem Handy hat; Jarvis zuerst, danach die zuletzt geänderten."""
    pakete = handy_pakete()
    apps = []
    for d in sorted(PROGGS.iterdir()):
        if d.name in AUSSCHLUSS or not (d / "gradlew").is_file() or not (d / "app" / "build.gradle.kts").is_file():
            continue
        kennung = app_id(d.name)
        # Debug-Varianten hängen teils ein Suffix an (z. B. .debug); ohne Paketliste wird jede App vorgewärmt.
        if pakete and not any(p == kennung or p.startswith(kennung + ".") for p in pakete):
            continue
        apps.append(d.name)
    zeit = {a: int(git("log", "-1", "--format=%ct", "--", a) or 0) for a in apps}
    return sorted(apps, key=lambda a: (a != "Jarvis", -zeit[a]))


def ordner_zu(name: str) -> str:
    n = name.strip()
    if (PROGGS / n / "gradlew").is_file():
        return n
    if n.lower() in NAMEN:
        return NAMEN[n.lower()]
    kompakt = re.sub(r"[^a-z0-9]", "", n.lower())
    for d in PROGGS.iterdir():
        if (d / "gradlew").is_file() and d.name.lower() == kompakt:
            return d.name
    return ""


def pausiert() -> bool:
    try:
        return time.time() - PAUSE.stat().st_mtime < PAUSE_SEKUNDEN
    except OSError:
        return False


def baue(app: str, hash_: str, stand: dict) -> bool:
    if subprocess.run(["git", "-C", str(PROGGS), "lfs", "ls-files", "-I", f"{app}/**"], capture_output=True, text=True, timeout=60).stdout.strip():
        subprocess.run(["git", "-C", str(PROGGS), "lfs", "pull", f"--include={app}/**"], capture_output=True, timeout=1800)
    # Ohne nice/ionice: Der Gradle- und der Kotlin-Daemon erben die Priorität, und die Sitzungsbauten nutzen dieselben Daemons
    # (jarvis darf sie nicht wieder anheben). Begrenzt ist der Bau ohnehin über workers.max=2 in ~/.gradle/gradle.properties.
    befehl = ["bash", "gradlew", "assembleDebug", "-q"]
    with open(BAU_SPERRE, "a+") as sperre:
        beginn = time.time()
        fcntl.flock(sperre, fcntl.LOCK_EX)  # wartet, falls eine Sitzung gerade baut
        warte = time.time() - beginn
        # Während des Wartens kann sich der Stand geändert haben (neuer Abgleich, Sitzung hat committet).
        hash_ = git("rev-parse", f"HEAD:{app}") or hash_
        if stand.get(app) == hash_ and not ARGS.immer:
            return True
        beginn = time.time()
        try:
            ergebnis = subprocess.run(befehl, cwd=PROGGS / app, capture_output=True, text=True, timeout=3600, stdin=subprocess.DEVNULL)
            ok = ergebnis.returncode == 0
            fehler = (ergebnis.stderr or ergebnis.stdout).strip()[-400:]
        except subprocess.TimeoutExpired:
            ok, fehler = False, "nach 60 Minuten nicht fertig"
        dauer = int(time.time() - beginn)
    if ok:
        stand[app] = hash_
        stand.get("_fehler", {}).pop(app, None)
        stand.get("_versuche", {}).pop(app, None)
        schreib_stand(stand)
        log(f"{app}: {'Auftragsstart-Vorbau' if ARGS.immer else 'vorgewärmt'} in {dauer} s" + (f" (vorher {int(warte)} s auf Sitzungsbau gewartet)" if warte > 2 else ""))
    else:
        # Fehlschlag: erneut versuchen (z. B. Netz weg), aber höchstens VERSUCHE-mal je Stand, damit ein kaputter Stand nicht alle 5 Minuten baut.
        v = stand.setdefault("_versuche", {})
        v[app] = [hash_, (v.get(app, ["", 0])[1] + 1) if v.get(app, ["", 0])[0] == hash_ else 1]
        if v[app][1] >= VERSUCHE:
            stand[app] = hash_
        stand.setdefault("_fehler", {})[app] = fehler
        schreib_stand(stand)
        log(f"{app}: Bau fehlgeschlagen nach {dauer} s: {fehler.splitlines()[-1] if fehler else '?'}")
    return ok


def lauf() -> int:
    ORDNER.mkdir(parents=True, exist_ok=True)
    einzeln = LAUF_SPERRE if not ARGS.app else ORDNER / f"lauf-{ARGS.app}.lock"
    with open(einzeln, "a+") as s:
        try:
            fcntl.flock(s, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return 0  # läuft schon
        apps = [ARGS.app] if ARGS.app else alle_apps()
        stand = lies_stand()
        for app in apps:
            if not ARGS.app and pausiert():
                log("Pause: eine Sitzung baut, Vorwärmen setzt beim nächsten Abgleich fort")
                return 0
            hash_ = git("rev-parse", f"HEAD:{app}")
            if not hash_ or (stand.get(app) == hash_ and not ARGS.immer):
                continue
            # Eine Sitzung arbeitet gerade an dieser App: ihren halbfertigen Stand nicht bauen (beim Auftragsstart ist er noch sauber).
            if git("status", "--porcelain", "--", app) and not ARGS.app:
                continue
            baue(app, hash_, stand)
            stand = lies_stand()
            time.sleep(3)  # einer wartenden Sitzung die Sperre lassen
        if not ARGS.app and not pausiert():
            daemon_pflege(lies_stand())
    return 0


def gradle_daemons() -> list:
    """(pid, Sekunden seit Start) aller Gradle-Daemons dieses Benutzers."""
    aus = subprocess.run(["ps", "-u", str(os.getuid()), "-o", "pid=,etimes=,args="], capture_output=True, text=True).stdout
    return [(int(z.split()[0]), int(z.split()[1])) for z in aus.splitlines() if "org.gradle.launcher.daemon.bootstrap.GradleDaemon" in z]


def daemon_pflege(stand: dict) -> None:
    """Hält den Gradle-Daemon rund um die Uhr warm: fehlt er, wird er per Jarvis-Bau gestartet; ist er älter als 24 Stunden,
    wird er unter der Bau-Sperre beendet und frisch gestartet (Frank, 13.11.2026: lange Laufzeiten sollen nicht verwittern)."""
    daemons = gradle_daemons()
    alt = [p for p, sek in daemons if sek > 24 * 3600]
    if daemons and not alt:
        return
    if alt:
        with open(BAU_SPERRE, "a+") as sperre:
            fcntl.flock(sperre, fcntl.LOCK_EX)
            subprocess.run(["bash", "gradlew", "--stop", "-q"], cwd=PROGGS / "Jarvis", capture_output=True, timeout=120)
            aus = subprocess.run(["ps", "-u", str(os.getuid()), "-o", "pid=,args="], capture_output=True, text=True).stdout
            for z in aus.splitlines():
                if "org.jetbrains.kotlin.daemon.KotlinCompileDaemon" in z:
                    try:
                        os.kill(int(z.split()[0]), 15)
                    except OSError:
                        pass
        log("Gradle-Daemon nach 24 Stunden beendet, starte ihn frisch")
    else:
        log("Kein Gradle-Daemon aktiv, starte ihn")
    ARGS.immer = True
    baue("Jarvis", git("rev-parse", "HEAD:Jarvis"), stand)
    ARGS.immer = False


def start() -> int:
    """Startet einen Lauf losgelöst im tmux-Server (überlebt das Ende des Repo-Dienstes und der Brücke)."""
    befehl = [sys.executable, str(Path(__file__).resolve()), "lauf"] + (["--app", ARGS.app] if ARGS.app else []) + (["--immer"] if ARGS.immer else [])
    zeile = " ".join(shlex.quote(t) for t in befehl) + " >/dev/null 2>&1"
    if subprocess.run(["tmux", "-L", "jarvis", "run-shell", "-b", zeile], capture_output=True, timeout=20).returncode != 0:
        subprocess.Popen(["setsid", "nohup", "bash", "-c", zeile], stdin=subprocess.DEVNULL, start_new_session=True)
    return 0


def zeige() -> int:
    stand = lies_stand()
    for app in alle_apps():
        h = git("rev-parse", f"HEAD:{app}")
        print(f"{app:22} {'aktuell' if stand.get(app) == h else 'offen'}" + (f"  FEHLER {stand['_fehler'][app][-80:]!r}" if app in stand.get("_fehler", {}) and stand.get(app) == h else ""))
    return 0


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("aktion", choices=["start", "lauf", "stand"])
    p.add_argument("--app", default="")
    p.add_argument("--immer", action="store_true", help="auch bauen, wenn der Stand schon vorgewärmt ist (wärmt den Gradle-Daemon)")
    ARGS = p.parse_args()
    if ARGS.app:
        ARGS.app = ordner_zu(ARGS.app)
        if not ARGS.app or ARGS.app in AUSSCHLUSS:
            sys.exit(0)
    sys.exit({"start": start, "lauf": lauf, "stand": zeige}[ARGS.aktion]())
