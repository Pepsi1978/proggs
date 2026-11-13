"""Vorwärmen der Android-Apps am Windows-PC (später auch am Mac), Gegenstück zu OpenLauncherServer/kern/vorwaermen.py.

Problem: Git bringt nur Quelltext. Hat Frank auf dem Server, dem Mac oder in der Cloud viele Commits gemacht, passt der
build-Ordner am PC nicht mehr, und der erste Bau einer Sitzung übersetzt fast alles neu, dazu kalter Gradle-Daemon.
Dieses Skript holt deshalb im Hintergrund den neuen Stand (nur Vorspulen wie der OpenLauncher, nie ein echter Merge)
und baut danach jede App nach, die am Handy oder Tablet installiert ist und deren Ordner sich geändert hat.

Regeln (siehe best-practices/schneller-programmieren/android-server-bau.md):
- Nie einen laufenden Bau abbrechen, nie Daemons beenden, keine niedrigere Priorität (vererbt sich an den geteilten Daemon).
- Läuft gerade irgendein Gradle-Bau (eine Sitzung baut), setzt das Vorwärmen aus; Apps mit offenen Änderungen und frisch
  committete Apps (eine Sitzung baut vermutlich gleich) werden übersprungen.
- Im Akkubetrieb kein Vorwärmen.

Aufrufe: vorwaermen.py lauf  [--app ORDNER] [--immer]   (Aufgabenplanung alle 10 Minuten, Anmeldung)
         vorwaermen.py start [--app ORDNER] [--immer]   (löst sich vom Aufrufer, z. B. PC-Brücke beim Auftragsstart)
         vorwaermen.py stand                             (zeigt, was vorgewärmt ist)
"""
import argparse, json, os, re, subprocess, sys, time
from pathlib import Path

WINDOWS = os.name == "nt"
PROGGS = Path.home() / "proggs"
ORDNER = Path.home() / ".openlauncher" / "vorwaermen"
STAND = ORDNER / "stand.json"
PAKETE = ORDNER / "pakete.txt"
LOG = ORDNER / "log.txt"
BAU_SPERRE = ORDNER / "gradle-bau.lock"
LAUF_SPERRE = ORDNER / "lauf.lock"
PAUSE = ORDNER / "pause"
PAUSE_SEKUNDEN = 20 * 60
RUHE_NACH_COMMIT = 10 * 60  # frisch committet = eine Sitzung hier baut/installiert vermutlich gerade selbst
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


def run(befehl, **kw):
    kw.setdefault("capture_output", True)
    kw.setdefault("text", True)
    kw.setdefault("stdin", subprocess.DEVNULL)
    kw.setdefault("encoding", "utf-8")
    kw.setdefault("errors", "replace")
    return subprocess.run(befehl, creationflags=OHNE_FENSTER, **kw)


def git(*args: str, timeout=120) -> str:
    try:
        return run(["git", "-C", str(PROGGS), *args], timeout=timeout).stdout.strip()
    except Exception:
        return ""


class Sperre:
    """Dateisperre für Windows (msvcrt) und Mac/Linux (fcntl). warten=False: sofort aufgeben, wenn belegt."""

    def __init__(self, pfad: Path, warten: bool):
        self.pfad, self.warten, self.f = pfad, warten, None

    def __enter__(self):
        ORDNER.mkdir(parents=True, exist_ok=True)
        self.f = open(self.pfad, "a+")
        while True:
            try:
                if WINDOWS:
                    self.f.seek(0)
                    msvcrt.locking(self.f.fileno(), msvcrt.LK_NBLCK, 1)
                else:
                    fcntl.flock(self.f, fcntl.LOCK_EX | fcntl.LOCK_NB)
                return True
            except OSError:
                if not self.warten:
                    self.f.close()
                    self.f = None
                    return False
                time.sleep(5)

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


def schreib_stand(stand: dict) -> None:
    neu = STAND.with_suffix(".neu")
    neu.write_text(json.dumps(stand, ensure_ascii=False, indent=1), encoding="utf-8")
    neu.replace(STAND)


def akkubetrieb() -> bool:
    if not WINDOWS:
        aus = run(["pmset", "-g", "batt"], timeout=10).stdout if sys.platform == "darwin" else ""
        return "Battery Power" in aus
    try:
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
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or \
        str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk" if WINDOWS else Path.home() / "Library" / "Android" / "sdk")
    kandidat = Path(sdk) / "platform-tools" / ("adb.exe" if WINDOWS else "adb")
    return str(kandidat) if kandidat.is_file() else "adb"


def geraete_pakete() -> set:
    """Installierte Pakete auf allen gerade verbundenen Geräten (Handy, Tablet); sonst die zuletzt gemerkte Liste."""
    pakete = set()
    try:
        liste = run([adb(), "devices"], timeout=15).stdout.splitlines()
        for z in liste[1:]:
            teile = z.split()
            if len(teile) >= 2 and teile[1] == "device":
                aus = run([adb(), "-s", teile[0], "shell", "pm", "list", "packages", "-3"], timeout=20).stdout
                pakete |= {p.split(":", 1)[1].strip() for p in aus.splitlines() if p.startswith("package:")}
    except Exception:
        pass
    alt = set()
    try:
        alt = set(PAKETE.read_text(encoding="utf-8").split())
    except OSError:
        pass
    if pakete:
        ORDNER.mkdir(parents=True, exist_ok=True)
        PAKETE.write_text("\n".join(sorted(pakete | alt)) + "\n", encoding="utf-8")
        return pakete | alt
    return alt


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
        elif int(git("log", "-1", "--format=%ct", "--", d.name) or 0) < grenze:
            continue
        apps.append(d.name)
    zeit = {a: int(git("log", "-1", "--format=%ct", "--", a) or 0) for a in apps}
    return sorted(apps, key=lambda a: (a != "Jarvis", -zeit[a]))


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


def java_befehlszeilen() -> list:
    if WINDOWS:
        aus = run(["powershell", "-NoProfile", "-NonInteractive", "-Command",
                   "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | ForEach-Object { $_.CommandLine }"], timeout=60).stdout
    else:
        aus = run(["ps", "-u", str(os.getuid()), "-o", "args="], timeout=20).stdout
    return [z for z in aus.splitlines() if z.strip()]


def fremder_bau_laeuft() -> bool:
    """Läuft ein Gradle-Client (gradlew einer Sitzung, apk-update)? Erkennbar am Wrapper-Jar in der Befehlszeile."""
    return any("gradle-wrapper.jar" in z or "GradleWrapperMain" in z for z in java_befehlszeilen())


def pausiert() -> bool:
    try:
        return time.time() - PAUSE.stat().st_mtime < PAUSE_SEKUNDEN
    except OSError:
        return False


def repo_vorspulen() -> None:
    """Wie OpenLauncher/Services/RepoSync.cs: fetch + merge --ff-only. Nie ein echter Merge, das Vorspulen wird nie abgebrochen."""
    gitdir = PROGGS / ".git"
    if any((gitdir / n).exists() for n in ("index.lock", "rebase-merge", "rebase-apply", "MERGE_HEAD")):
        return
    vorher = git("rev-parse", "HEAD")
    if run(["git", "-C", str(PROGGS), "fetch", "--quiet"], timeout=120).returncode != 0:
        return
    ergebnis = run(["git", "-C", str(PROGGS), "merge", "--ff-only", "--quiet", "@{u}"])  # ohne Zeitlimit: ein Abbruch hinterließe index.lock
    nachher = git("rev-parse", "HEAD")
    if ergebnis.returncode == 0 and nachher != vorher:
        anzahl = git("rev-list", "--count", f"{vorher}..{nachher}")
        log(f"Repo vorgespult: {anzahl} neue Commits")


def gradlew(app: str) -> list:
    workers = str(max(2, (os.cpu_count() or 4) // 2))  # gilt nur für diesen Bau, nicht für den Daemon
    basis = ["cmd", "/c", "gradlew.bat"] if WINDOWS else ["bash", "gradlew"]
    return basis + ["assembleDebug", "-q", f"--max-workers={workers}"]


def baue(app: str, hash_: str, stand: dict) -> bool:
    if git("lfs", "ls-files", "-I", f"{app}/**", timeout=60):
        run(["git", "-C", str(PROGGS), "lfs", "pull", f"--include={app}/**"], timeout=1800)
    with Sperre(BAU_SPERRE, warten=True):
        hash_ = git("rev-parse", f"HEAD:{app}") or hash_
        if stand.get(app) == hash_ and not ARGS.immer:
            return True
        beginn = time.time()
        try:
            e = run(gradlew(app), cwd=PROGGS / app, timeout=3600)
            ok, fehler = e.returncode == 0, (e.stderr or e.stdout or "").strip()[-400:]
        except subprocess.TimeoutExpired:
            ok, fehler = False, "nach 60 Minuten nicht fertig"
        dauer = int(time.time() - beginn)
    stand = lies_stand()
    if ok:
        stand[app] = hash_
        stand.get("_fehler", {}).pop(app, None)
        stand.get("_versuche", {}).pop(app, None)
        schreib_stand(stand)
        log(f"{app}: {'Auftragsstart-Vorbau' if ARGS.immer else 'vorgewärmt'} in {dauer} s")
    else:
        v = stand.setdefault("_versuche", {})
        v[app] = [hash_, (v.get(app, ["", 0])[1] + 1) if v.get(app, ["", 0])[0] == hash_ else 1]
        if v[app][1] >= VERSUCHE:
            stand[app] = hash_
        stand.setdefault("_fehler", {})[app] = fehler
        schreib_stand(stand)
        log(f"{app}: Bau fehlgeschlagen nach {dauer} s: {fehler.splitlines()[-1] if fehler else '?'}")
    return ok


def daemon_warm() -> None:
    """Kein Gradle-Daemon aktiv (Rechnerstart, JDK-Update): per Jarvis-Bau einen starten. Die Lebensdauer (24 h Leerlauf)
    steht in ~/.gradle/gradle.properties (einrichten.ps1)."""
    if not (PROGGS / "Jarvis" / "gradlew").is_file():
        return
    if any("GradleDaemon" in z for z in java_befehlszeilen()):
        return
    log("Kein Gradle-Daemon aktiv, starte ihn mit einem Jarvis-Bau")
    ARGS.immer = True
    baue("Jarvis", git("rev-parse", "HEAD:Jarvis"), lies_stand())
    ARGS.immer = False


def lauf() -> int:
    einzeln = LAUF_SPERRE if not ARGS.app else ORDNER / f"lauf-{ARGS.app}.lock"
    with Sperre(einzeln, warten=False) as frei:
        if not frei:
            return 0
        if not ARGS.app:
            if akkubetrieb():
                return 0
            repo_vorspulen()
            if pausiert() or fremder_bau_laeuft():
                return 0
        apps = [ARGS.app] if ARGS.app else alle_apps()
        stand = lies_stand()
        jetzt = time.time()
        for app in apps:
            hash_ = git("rev-parse", f"HEAD:{app}")
            if not hash_ or (stand.get(app) == hash_ and not ARGS.immer):
                continue
            if not ARGS.app:
                if git("status", "--porcelain", "--", app):
                    continue  # eine Sitzung arbeitet daran
                if jetzt - int(git("log", "-1", "--format=%ct", "--", app) or 0) < RUHE_NACH_COMMIT:
                    continue  # frisch committet: die Sitzung baut und installiert gleich selbst
                if pausiert() or fremder_bau_laeuft():
                    log("Pause: eine Sitzung baut, Vorwärmen setzt beim nächsten Lauf fort")
                    return 0
            baue(app, hash_, stand)
            stand = lies_stand()
        if not ARGS.app and not pausiert() and not fremder_bau_laeuft():
            daemon_warm()
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
    p.add_argument("aktion", choices=["start", "lauf", "stand"])
    p.add_argument("--app", default="")
    p.add_argument("--immer", action="store_true", help="auch bauen, wenn der Stand schon vorgewärmt ist (wärmt den Daemon)")
    ARGS = p.parse_args()
    if ARGS.app:
        ARGS.app = ordner_zu(ARGS.app)
        if not ARGS.app:
            sys.exit(0)
    try:
        sys.exit({"start": start, "lauf": lauf, "stand": zeige}[ARGS.aktion]())
    except Exception as fehler:  # Hintergrundlauf: nie still sterben
        log(f"Fehler: {fehler!r}")
        sys.exit(1)
