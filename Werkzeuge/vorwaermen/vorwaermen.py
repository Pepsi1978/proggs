"""Vorwärmen der Android-Apps am Windows-PC (später auch am Mac), Gegenstück zu OpenLauncherServer/kern/vorwaermen.py.

Problem: Git bringt nur Quelltext. Hat Frank auf dem Server, dem Mac oder in der Cloud viele Commits gemacht, passt der
build-Ordner am PC nicht mehr, und der erste Bau einer Sitzung übersetzt fast alles neu, dazu kalter Gradle-Daemon.
Dieses Skript holt deshalb im Hintergrund den neuen Stand (nur Vorspulen wie der OpenLauncher, nie ein echter Merge)
und baut danach jede App nach, die am Handy oder Tablet installiert ist und deren Ordner sich geändert hat.

Regeln (siehe best-practices/schneller-programmieren/android-server-bau.md, Abschnitt 8):
- Nie einen laufenden Bau abbrechen, nie Daemons beenden, keine niedrigere Priorität (vererbt sich an den geteilten Daemon).
- Sitzungsbauten rufen vorher `vorwaermen.py warte` auf (Bau-Hook für Claude Code, Skill apk-update): Das wartet auf einen
  laufenden Vorbau und legt die Datei pause an, damit 20 Minuten kein neuer Vorbau beginnt.
- Läuft trotzdem ein fremder Gradle-Bau, setzt das Vorwärmen aus; Apps mit offenen Änderungen und frisch committete Apps
  (die Sitzung baut gleich selbst) werden übersprungen. Im Akkubetrieb kein Vorwärmen.
- Ausgaben nie über Pipes: Ein dabei gestarteter Gradle-Daemon oder adb-Server erbte die Pipe, und der Lauf hinge.
- Dauerhaft warm bleibt nur der Daemon mit den Jarvis-Einstellungen; andere Apps bauen ihren Ordner mit --no-daemon vor
  (am PC hat fast jede App eigene jvmargs, sonst blieben mehrere 2-4-GB-Daemons einen Tag lang liegen).

Aufrufe: vorwaermen.py lauf  [--app ORDNER] [--immer]   (Aufgabenplanung alle 10 Minuten und bei der Anmeldung;
                                                         PC-Brücke beim Auftragsstart per Start-Process)
         vorwaermen.py start [--app ORDNER] [--immer]   (löst sich vom Aufrufer, für Aufrufe aus Agenten-Shells)
         vorwaermen.py warte                             (vor einem Sitzungsbau)
         vorwaermen.py stand                             (zeigt, was vorgewärmt ist)
"""
import argparse, json, os, re, subprocess, sys, tempfile, time
from pathlib import Path

WINDOWS = os.name == "nt"
PROGGS = Path.home() / "proggs"
ORDNER = Path.home() / ".openlauncher" / "vorwaermen"
STAND = ORDNER / "stand.json"
STAND_SPERRE = ORDNER / "stand.lock"
PAKETE = ORDNER / "pakete.json"
LOG = ORDNER / "log.txt"
BAU_SPERRE = ORDNER / "gradle-bau.lock"
LAUF_SPERRE = ORDNER / "lauf.lock"
PAUSE = ORDNER / "pause"
PAUSE_SEKUNDEN = 20 * 60
RUHE_NACH_COMMIT = 10 * 60
VERSUCHE = 2
NAMEN = {"geniale aufgaben": "Aufgaben", "genialer wecker": "GenialerWeckerAndroid", "geniale ideen": "GenialeIdeen",
         "stack-labor": "StackLabor", "stack labor": "StackLabor", "entropie reductor": "EntropieReductor"}
OHNE_FENSTER = 0x08000000 if WINDOWS else 0  # CREATE_NO_WINDOW

if WINDOWS:
    import msvcrt
else:
    import fcntl


def log(text: str) -> None:
    ORDNER.mkdir(parents=True, exist_ok=True)
    with open(LOG, "a", encoding="utf-8") as f:
        f.write(time.strftime("%d.%m.%Y %H:%M:%S ") + text + "\n")
    if LOG.stat().st_size > 400_000:
        zeilen = LOG.read_text(encoding="utf-8").splitlines()[-1500:]
        LOG.write_text("\n".join(zeilen) + "\n", encoding="utf-8")


class Ergebnis:
    def __init__(self, code: int, text: str):
        self.returncode, self.stdout = code, text


def run(befehl, timeout=None, cwd=None) -> Ergebnis:
    """Startet ohne Pipes: Ausgabe in eine Temp-Datei. Erbt ein Langläufer (Gradle-Daemon, adb-Server) das Handle, blockiert
    nichts, und ein Zeitlimit beendet nur diesen Prozess. Nie eine Ausnahme: Fehler und Zeitlimit kommen als Code -1 zurück."""
    with tempfile.TemporaryFile() as aus:
        try:
            p = subprocess.Popen(befehl, cwd=cwd, stdin=subprocess.DEVNULL, stdout=aus, stderr=subprocess.STDOUT, creationflags=OHNE_FENSTER)
        except OSError as fehler:
            return Ergebnis(-1, str(fehler))
        try:
            code = p.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            p.kill()
            p.wait()
            code = -1
        aus.seek(0)
        return Ergebnis(code, aus.read().decode("utf-8", "replace"))


def git(*args: str, timeout=120) -> str:
    e = run(["git", "-C", str(PROGGS), *args], timeout=timeout)
    return e.stdout.strip() if e.returncode == 0 else ""


class Sperre:
    """Dateisperre für Windows (msvcrt) und Mac/Linux (fcntl). warten: Sekunden (None = unbegrenzt, 0 = sofort aufgeben)."""

    def __init__(self, pfad: Path, warten=None):
        self.pfad, self.warten, self.f = pfad, warten, None

    def __enter__(self):
        ORDNER.mkdir(parents=True, exist_ok=True)
        self.f = open(self.pfad, "a+")
        ende = None if self.warten is None else time.time() + self.warten
        while True:
            try:
                if WINDOWS:
                    self.f.seek(0)
                    msvcrt.locking(self.f.fileno(), msvcrt.LK_NBLCK, 1)
                else:
                    fcntl.flock(self.f, fcntl.LOCK_EX | fcntl.LOCK_NB)
                return True
            except OSError:
                if ende is not None and time.time() >= ende:
                    self.f.close()
                    self.f = None
                    return False
                time.sleep(1 if self.pfad == STAND_SPERRE else 5)

    def __exit__(self, *a):
        if self.f:
            try:
                if WINDOWS:
                    self.f.seek(0)
                    msvcrt.locking(self.f.fileno(), msvcrt.LK_UNLCK, 1)
            except OSError:
                pass
            self.f.close()


def lies_stand() -> dict:
    try:
        return json.loads(STAND.read_text(encoding="utf-8"))
    except Exception:
        return {}


def aendere_stand(aenderung) -> None:
    """Lesen, ändern, schreiben unter einer eigenen Sperre: Brücken-Lauf und Takt-Lauf dürfen sich nicht überschreiben."""
    with Sperre(STAND_SPERRE, warten=60):
        stand = lies_stand()
        aenderung(stand)
        neu = STAND.with_name(f"stand.{os.getpid()}.neu")
        neu.write_text(json.dumps(stand, ensure_ascii=False, indent=1), encoding="utf-8")
        for _ in range(10):
            try:
                neu.replace(STAND)
                return
            except PermissionError:  # Windows: ein Leser hält die Datei gerade offen
                time.sleep(0.5)
        neu.unlink(missing_ok=True)


def akkubetrieb() -> bool:
    try:
        if not WINDOWS:
            return sys.platform == "darwin" and "Battery Power" in run(["pmset", "-g", "batt"], timeout=10).stdout
        import ctypes

        class Status(ctypes.Structure):
            _fields_ = [("ACLineStatus", ctypes.c_byte), ("BatteryFlag", ctypes.c_byte), ("BatteryLifePercent", ctypes.c_byte),
                        ("SystemStatusFlag", ctypes.c_byte), ("BatteryLifeTime", ctypes.c_ulong), ("BatteryFullLifeTime", ctypes.c_ulong)]
        s = Status()
        ctypes.windll.kernel32.GetSystemPowerStatus(ctypes.byref(s))
        return s.ACLineStatus == 0
    except Exception:
        return False


def adb() -> str:
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        sdk = str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk") if WINDOWS else str(Path.home() / "Library" / "Android" / "sdk")
    kandidat = Path(sdk) / "platform-tools" / ("adb.exe" if WINDOWS else "adb")
    return str(kandidat) if kandidat.is_file() else "adb"


def geraete_pakete() -> set:
    """Installierte Pakete je Gerät (Seriennummer). Verbundene Geräte werden frisch gelesen, für die übrigen gilt ihre letzte
    Liste. So fällt eine deinstallierte App heraus, sobald das Gerät wieder gelesen wird."""
    try:
        je_geraet = json.loads(PAKETE.read_text(encoding="utf-8"))
    except Exception:
        je_geraet = {}
    for z in run([adb(), "devices"], timeout=20).stdout.splitlines()[1:]:
        teile = z.split()
        if len(teile) < 2 or teile[1] != "device":
            continue
        seriell = run([adb(), "-s", teile[0], "shell", "getprop", "ro.serialno"], timeout=15).stdout.strip() or teile[0]
        e = run([adb(), "-s", teile[0], "shell", "pm", "list", "packages", "-3"], timeout=20)
        pakete = sorted({p.split(":", 1)[1].strip() for p in e.stdout.splitlines() if p.startswith("package:")})
        if e.returncode == 0 and pakete:
            je_geraet[seriell] = pakete
    if je_geraet:
        ORDNER.mkdir(parents=True, exist_ok=True)
        PAKETE.write_text(json.dumps(je_geraet, indent=1), encoding="utf-8")
    return {p for liste in je_geraet.values() for p in liste}


_COMMITZEIT = {}


def commitzeit(app: str) -> int:
    if app not in _COMMITZEIT:
        _COMMITZEIT[app] = int(git("log", "-1", "--format=%ct", "--", app) or 0)
    return _COMMITZEIT[app]


def app_id(app: str) -> str:
    try:
        text = (PROGGS / app / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    except OSError:
        return ""
    m = re.search(r'applicationId\s*=\s*"([^"]+)"', text)
    return m.group(1) if m else ""


def alle_apps() -> list:
    """Android-Apps im Repo, die auf Handy oder Tablet installiert sind; Jarvis zuerst, dann die zuletzt geänderten.
    Ohne bekannte Paketliste: nur Apps, die in den letzten 14 Tagen geändert wurden."""
    pakete = geraete_pakete()
    grenze = time.time() - 14 * 86400
    apps = []
    for d in sorted(PROGGS.iterdir()):
        if not (d / "gradlew").is_file() or not (d / "app" / "build.gradle.kts").is_file():
            continue
        if pakete:
            kennung = app_id(d.name)
            if not kennung or not any(p == kennung or p.startswith(kennung + ".") for p in pakete):
                continue
        elif commitzeit(d.name) < grenze:
            continue
        apps.append(d.name)
    return sorted(apps, key=lambda a: (a != "Jarvis", -commitzeit(a)))


def ordner_zu(name: str) -> str:
    n = name.strip()
    if n and (PROGGS / n / "gradlew").is_file():
        return n
    if n.lower() in NAMEN:
        return NAMEN[n.lower()]
    kompakt = re.sub(r"[^a-z0-9]", "", n.lower())
    for d in PROGGS.iterdir():
        if (d / "gradlew").is_file() and d.name.lower() == kompakt:
            return d.name
    return ""


def java_befehlszeilen():
    """Befehlszeilen aller Java-Prozesse; None, wenn die Abfrage scheiterte (dann gilt: lieber nicht bauen)."""
    if WINDOWS:
        e = run(["powershell", "-NoProfile", "-NonInteractive", "-Command",
                 "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | ForEach-Object { $_.CommandLine }"], timeout=60)
    else:
        e = run(["ps", "-u", str(os.getuid()), "-o", "args="], timeout=20)
    return [z for z in e.stdout.splitlines() if z.strip()] if e.returncode == 0 else None


def fremder_bau_laeuft() -> bool:
    """Läuft ein Gradle-Client (gradlew einer Sitzung, apk-update)? Erkennbar am Wrapper in der Befehlszeile. Bauten aus
    Android Studio (Tooling-API) sieht das nicht; die Sitzungen am PC bauen aber über gradlew."""
    zeilen = java_befehlszeilen()
    return zeilen is None or any("gradle-wrapper.jar" in z or "GradleWrapperMain" in z for z in zeilen)


def pausiert() -> bool:
    try:
        return time.time() - PAUSE.stat().st_mtime < PAUSE_SEKUNDEN
    except OSError:
        return False


def repo_vorspulen() -> None:
    """Wie OpenLauncher/Services/RepoSync.cs: fetch (darf abbrechen) + merge --ff-only (nie abbrechen, sonst bleibt index.lock)."""
    gitdir = PROGGS / ".git"
    if any((gitdir / n).exists() for n in ("index.lock", "rebase-merge", "rebase-apply", "MERGE_HEAD")):
        return
    vorher = git("rev-parse", "HEAD")
    if run(["git", "-C", str(PROGGS), "fetch", "--quiet"], timeout=300).returncode != 0:
        return
    if run(["git", "-C", str(PROGGS), "merge", "--ff-only", "--quiet", "@{u}"]).returncode == 0:
        nachher = git("rev-parse", "HEAD")
        if nachher and nachher != vorher:
            _COMMITZEIT.clear()
            log(f"Repo vorgespult: {git('rev-list', '--count', f'{vorher}..{nachher}')} neue Commits")


def jvmargs(app: str) -> str:
    try:
        text = (PROGGS / app / "gradle.properties").read_text(encoding="utf-8")
    except OSError:
        return ""
    m = re.search(r"^\s*org\.gradle\.jvmargs\s*=(.*)$", text, re.M)
    return " ".join(m.group(1).split()) if m else ""


def gradlew(app: str) -> list:
    workers = str(max(2, (os.cpu_count() or 4) // 2))  # gilt nur für diesen Bau, nicht für den Daemon
    befehl = (["cmd", "/c", "gradlew.bat"] if WINDOWS else ["bash", "gradlew"]) + ["assembleDebug", "-q", f"--max-workers={workers}"]
    # Dauerhaft warm bleibt nur der Daemon mit den Jarvis-Einstellungen; beim Auftragsstart (--immer) auch der der App.
    if not ARGS.immer and app != "Jarvis" and jvmargs(app) != jvmargs("Jarvis"):
        befehl.append("--no-daemon")
    return befehl


def baue(app: str) -> bool:
    if git("lfs", "ls-files", "-I", f"{app}/**", timeout=60):
        run(["git", "-C", str(PROGGS), "lfs", "pull", f"--include={app}/**"])  # ohne Zeitlimit: ein Abbruch ließe index.lock liegen
    with Sperre(BAU_SPERRE):
        # Erst nach dem Warten den Stand lesen: Ein anderer Lauf kann die App inzwischen gebaut haben.
        hash_ = git("rev-parse", f"HEAD:{app}")
        if not hash_ or (lies_stand().get(app) == hash_ and not ARGS.immer):
            return True
        sauber = not git("status", "--porcelain", "--", app)
        beginn = time.time()
        e = run(gradlew(app), cwd=PROGGS / app, timeout=3600)
        ok, fehler = e.returncode == 0, e.stdout.strip()[-400:] or ("nach 60 Minuten abgebrochen" if e.returncode == -1 else "")
        dauer = int(time.time() - beginn)
    if not sauber:
        # Halb geänderter Arbeitsbaum (Auftragsstart): Der Bau wärmt, passt aber nicht genau zu HEAD, also nichts merken.
        log(f"{app}: Vorbau auf offenem Arbeitsstand in {dauer} s ({'ok' if ok else 'fehlgeschlagen'})")
        return ok

    def merke(stand):
        if ok:
            stand[app] = hash_
            stand.get("_fehler", {}).pop(app, None)
            stand.get("_versuche", {}).pop(app, None)
        else:
            v = stand.setdefault("_versuche", {})
            alt = v.get(app, ["", 0])
            v[app] = [hash_, alt[1] + 1 if alt[0] == hash_ else 1]
            if v[app][1] >= VERSUCHE:
                stand[app] = hash_
            stand.setdefault("_fehler", {})[app] = fehler
    aendere_stand(merke)
    if ok:
        log(f"{app}: {'Auftragsstart-Vorbau' if ARGS.immer else 'vorgewärmt'} in {dauer} s")
    else:
        log(f"{app}: Bau fehlgeschlagen nach {dauer} s: {fehler.splitlines()[-1] if fehler else '?'}")
    return ok


def daemon_warm() -> None:
    """Kein Gradle-Daemon aktiv (Rechnerstart, JDK-Update): per Jarvis-Bau einen starten. Lebensdauer 24 h Leerlauf steht in
    ~/.gradle/gradle.properties (einrichten.ps1)."""
    if not (PROGGS / "Jarvis" / "gradlew").is_file() or git("status", "--porcelain", "--", "Jarvis"):
        return
    if time.time() - commitzeit("Jarvis") < RUHE_NACH_COMMIT:
        return
    zeilen = java_befehlszeilen()
    if zeilen is None or any("GradleDaemon" in z for z in zeilen):
        return
    log("Kein Gradle-Daemon aktiv, starte ihn mit einem Jarvis-Bau")
    ARGS.immer = True
    baue("Jarvis")
    ARGS.immer = False


def lauf() -> int:
    einzeln = LAUF_SPERRE if not ARGS.app else ORDNER / f"lauf-{ARGS.app}.lock"
    with Sperre(einzeln, warten=0) as frei:
        if not frei or akkubetrieb():
            return 0
        if ARGS.app:
            baue(ARGS.app)
            return 0
        repo_vorspulen()
        stand = lies_stand()
        jetzt = time.time()
        for app in alle_apps():
            hash_ = git("rev-parse", f"HEAD:{app}")
            if not hash_ or stand.get(app) == hash_:
                continue
            if git("status", "--porcelain", "--", app):
                continue  # eine Sitzung arbeitet daran
            if jetzt - commitzeit(app) < RUHE_NACH_COMMIT:
                continue  # frisch committet: die Sitzung baut und installiert gleich selbst
            if pausiert() or fremder_bau_laeuft():
                log("Pause: eine Sitzung baut, Vorwärmen setzt beim nächsten Lauf fort")
                return 0
            baue(app)
            stand = lies_stand()
        if not pausiert() and not fremder_bau_laeuft():
            daemon_warm()
    return 0


def warte() -> int:
    """Vor einem Sitzungsbau: Vorwärmen 20 Minuten pausieren und auf einen laufenden Vorbau warten (höchstens 60 Minuten).
    Gibt die Sperre danach frei; der Sitzungsbau läuft über denselben Daemon."""
    ORDNER.mkdir(parents=True, exist_ok=True)
    PAUSE.touch()
    beginn = time.time()
    with Sperre(BAU_SPERRE, warten=3600) as frei:
        gewartet = int(time.time() - beginn)
        if gewartet > 2:
            print(f"VORWAERMEN: {gewartet} s auf einen laufenden Vorbau gewartet" + ("" if frei else ", baue trotzdem"))
    return 0


def start() -> int:
    """Startet einen Lauf losgelöst, ohne Handles des Aufrufers zu erben (sonst wartet ein Agent bis in sein Zeitlimit)."""
    py = Path(sys.executable)
    if WINDOWS and (py.parent / "pythonw.exe").is_file():
        py = py.parent / "pythonw.exe"
    befehl = [str(py), str(Path(__file__).resolve()), "lauf"] + (["--app", ARGS.app] if ARGS.app else []) + (["--immer"] if ARGS.immer else [])
    if WINDOWS:
        subprocess.Popen(befehl, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, close_fds=True,
                         creationflags=0x00000008 | 0x00000200 | OHNE_FENSTER)  # DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP
    else:
        subprocess.Popen(befehl, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
    return 0


def zeige() -> int:
    stand = lies_stand()
    for app in alle_apps():
        h = git("rev-parse", f"HEAD:{app}")
        fehler = stand.get("_fehler", {}).get(app, "")
        print(f"{app:24} {'aktuell' if stand.get(app) == h else 'offen'}" + (f"  FEHLER {fehler[-80:]!r}" if fehler and stand.get(app) == h else ""))
    return 0


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("aktion", choices=["start", "lauf", "warte", "stand"])
    p.add_argument("--app", default="")
    p.add_argument("--immer", action="store_true", help="auch bauen, wenn der Stand schon vorgewärmt ist (wärmt den Daemon)")
    ARGS = p.parse_args()
    if ARGS.app:
        ARGS.app = ordner_zu(ARGS.app)
        if not ARGS.app:
            sys.exit(0)
    try:
        sys.exit({"start": start, "lauf": lauf, "warte": warte, "stand": zeige}[ARGS.aktion]())
    except Exception as fehler:  # Hintergrundlauf: nie still sterben; warte darf einen Sitzungsbau nie verhindern
        log(f"Fehler ({ARGS.aktion}): {fehler!r}")
        sys.exit(0 if ARGS.aktion == "warte" else 1)
