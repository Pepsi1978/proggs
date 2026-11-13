"""Vorwärmen der Android-Apps am Windows-PC und am Mac (Einrichtung einrichten.ps1 bzw. einrichten.sh), Gegenstück zu OpenLauncherServer/kern/vorwaermen.py.

Problem: Git bringt nur Quelltext. Hat Frank auf dem Server, dem Mac oder in der Cloud viele Commits gemacht, passt der
build-Ordner am PC nicht mehr, und der erste Bau einer Sitzung übersetzt fast alles neu, dazu kalter Gradle-Daemon.
Dieses Skript holt deshalb im Hintergrund den neuen Stand (nur Vorspulen wie der OpenLauncher, nie ein echter Merge),
baut jede App nach, die am Handy oder Tablet installiert ist und deren Ordner sich geändert hat, und hält einen
Gradle-Daemon mit den Jarvis-Einstellungen warm.

Leitlinien (Abschnitt 8 in best-practices/schneller-programmieren/android-server-bau.md):
- Sitzungen haben immer Vorrang, in jeder CLI: Startet irgendwo ein Gradle-Bau, weicht ein laufender Vorbau binnen 2 s aus.
  Vorbauten laufen mit Wegwerf-Daemon (--no-daemon) und belegen nie den warmen Daemon, den nur `gradlew help` startet.
  Sitzungen warten nie aufeinander.
- Wenig Dauerlast: Der Leerlauf-Takt (alle 5 min) braucht nur git und eine Prozessliste über die Windows-API; adb fragt
  höchstens stündlich nach den installierten Apps, die Java-Befehlszeilen (PowerShell) nur, wenn der Daemon fehlt.
- Vorbau mit halber Kernzahl, keine niedrigere Priorität (vererbt sich an den geteilten Daemon), kein Vorbau bei Akku
  oder Spiel. 24 h Leerlauf gilt nur für den vorgewärmten Daemon (Kommandozeile), Sitzungs-Daemons behalten 3 h.
- Ausgaben nie über Pipes: Ein dabei gestarteter Gradle-Daemon oder adb-Server erbte die Pipe, und der Lauf hinge.
- git nur mit GIT_OPTIONAL_LOCKS=0: git status nimmt sonst kurz index.lock und stört Commits der Sitzungen.

Aufrufe: vorwaermen.py lauf  [--app ORDNER] [--immer]   (Aufgabenplanung; PC-Brücke beim Auftragsstart mit --app)
         vorwaermen.py warte                             (vor einem Sitzungsbau: Hook, apk-update)
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
DAEMON = ORDNER / "daemon.json"
LOG = ORDNER / "log.txt"
BAU_SPERRE = ORDNER / "gradle-bau.lock"
LAUF_SPERRE = ORDNER / "lauf.lock"
PAUSE = ORDNER / "pause"
PAUSE_SEKUNDEN = 20 * 60
RUHE_NACH_COMMIT = 10 * 60
VERSUCHE = 2
DAEMON_LEBEN = 24 * 3600
LEERLAUF_24H = "-Dorg.gradle.daemon.idletimeout=86400000"
NAMEN = {"geniale aufgaben": "Aufgaben", "genialer wecker": "GenialerWeckerAndroid", "geniale ideen": "GenialeIdeen",
         "stack-labor": "StackLabor", "stack labor": "StackLabor", "entropie reductor": "EntropieReductor"}
OHNE_FENSTER = 0x08000000 if WINDOWS else 0  # CREATE_NO_WINDOW
SHELLS = {"cmd.exe", "bash.exe", "sh.exe", "powershell.exe", "pwsh.exe"}
# Steam-Apps, die keine Spiele sind und dauerhaft laufen können (Wallpaper Engine).
STEAM_KEIN_SPIEL = {0, 431960}
# cod.exe, cod22-cod.exe, cod24.exe …, aber nie codex.exe (Codex CLI).
SPIEL_EXE = re.compile(r"^(cod|cod\d\d\S*|modernwarfare\S*|blackops\S*|warzone\S*)\.exe$", re.I)
AUSGEWICHEN = -2
ENV = dict(os.environ, GIT_OPTIONAL_LOCKS="0")
if sys.platform == "darwin":
    # Gleiche Java und SDK wie die Sitzungen, sonst passte der warme Daemon nicht (Gradle vergleicht den Java-Pfad). Der
    # Cellar-Pfad aus /usr/libexec/java_home bricht nach jedem brew upgrade; launchd bringt nur einen kurzen PATH mit.
    _JAVA = "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    if (not ENV.get("JAVA_HOME") or "/Cellar/" in ENV["JAVA_HOME"]) and os.path.isdir(_JAVA):
        ENV["JAVA_HOME"] = _JAVA
    _SDK = ENV.get("ANDROID_HOME") or "/opt/homebrew/share/android-commandlinetools"
    ENV.setdefault("ANDROID_HOME", _SDK)
    ENV.setdefault("ANDROID_SDK_ROOT", _SDK)
    ENV["PATH"] = ":".join(dict.fromkeys([f"{ENV.get('JAVA_HOME', _JAVA)}/bin", f"{_SDK}/platform-tools", "/opt/homebrew/bin",
                                          *ENV.get("PATH", "/usr/bin:/bin:/usr/sbin:/sbin").split(":")]))

if WINDOWS:
    import ctypes, msvcrt
    from ctypes import wintypes
else:
    import fcntl


def log(text: str) -> None:
    ORDNER.mkdir(parents=True, exist_ok=True)
    with open(LOG, "a", encoding="utf-8") as f:
        f.write(time.strftime("%d.%m.%Y %H:%M:%S ") + text + "\n")
    if LOG.stat().st_size > 400_000:
        zeilen = LOG.read_text(encoding="utf-8").splitlines()[-1500:]
        LOG.write_text("\n".join(zeilen) + "\n", encoding="utf-8")


# ---------------------------------------------------------------- Prozesse (billig über die Windows-API, ohne PowerShell)

def prozesse() -> list:
    """(pid, Eltern-pid, Programmname klein) aller Prozesse."""
    if not WINDOWS:
        aus = run(["ps", "-A", "-o", "pid=,ppid=,comm="], timeout=20).stdout
        return [(int(t[0]), int(t[1]), os.path.basename(t[2]).lower()) for t in (z.split(None, 2) for z in aus.splitlines())
                if len(t) == 3 and t[0].isdigit() and t[1].isdigit()]

    class EINTRAG(ctypes.Structure):
        _fields_ = [("dwSize", wintypes.DWORD), ("cntUsage", wintypes.DWORD), ("th32ProcessID", wintypes.DWORD),
                    ("th32DefaultHeapID", ctypes.c_size_t), ("th32ModuleID", wintypes.DWORD), ("cntThreads", wintypes.DWORD),
                    ("th32ParentProcessID", wintypes.DWORD), ("pcPriClassBase", ctypes.c_long), ("dwFlags", wintypes.DWORD),
                    ("szExeFile", ctypes.c_wchar * 260)]
    k32 = ctypes.windll.kernel32
    k32.CreateToolhelp32Snapshot.restype = wintypes.HANDLE
    schnapp = k32.CreateToolhelp32Snapshot(0x2, 0)
    liste, e = [], EINTRAG()
    e.dwSize = ctypes.sizeof(EINTRAG)
    try:
        ok = k32.Process32FirstW(schnapp, ctypes.byref(e))
        while ok:
            liste.append((e.th32ProcessID, e.th32ParentProcessID, e.szExeFile.lower()))
            ok = k32.Process32NextW(schnapp, ctypes.byref(e))
    finally:
        k32.CloseHandle(schnapp)
    return liste


_BEKANNT = {}  # pid → ist Gradle-Client (laut Befehlszeile); einmal je pid abgefragt


def gradle_clients(liste=None) -> list:
    """(pid, Eltern-pid) laufender Gradle-Clients. Vorauswahl billig über die Prozessliste (java unter einer Shell; Daemons
    sind Kinder eines Clients, Kotlin-Daemons Kinder eines Daemons, Android Studio startet nie über eine Shell), bestätigt über
    die Befehlszeile (gradle-wrapper.jar), aber nur für neue Kandidaten: apksigner.bat & Co. bremsen sonst den Vorbau aus."""
    liste = liste if liste is not None else prozesse()
    name = {pid: exe for pid, _, exe in liste}
    if WINDOWS:
        kandidaten = [(pid, ppid) for pid, ppid, exe in liste if exe == "java.exe" and name.get(ppid, "") in SHELLS]
    else:
        # Mac/Linux: gradlew endet mit `exec java`, der Client ersetzt also die Shell und hängt direkt unter dem Aufrufer
        # (zsh der CLI, pwsh von apk-update, node, python). Darum jede java-JVM als Kandidat; ps ist hier billig.
        kandidaten = [(pid, ppid) for pid, ppid, exe in liste if exe == "java"]
    neu = [pid for pid, _ in kandidaten if pid not in _BEKANNT]
    if neu:
        zeilen = befehlszeilen(neu)
        if zeilen is None:
            return kandidaten  # Abfrage gescheitert: lieber ausweichen als einer Sitzung im Weg stehen
        for pid in neu:
            z = zeilen.get(pid)
            if z is not None:
                _BEKANNT[pid] = "gradle-wrapper.jar" in z or "GradleWrapperMain" in z
    return [(pid, ppid) for pid, ppid in kandidaten if _BEKANNT.get(pid, True)]


def befehlszeilen(pids: list):
    """pid → Befehlszeile, nur für die genannten pids; None, wenn die Abfrage scheiterte."""
    if WINDOWS:
        filter_ = " OR ".join(f"ProcessId={p}" for p in pids)
        e = run(["powershell", "-NoProfile", "-NonInteractive", "-Command",
                 f"Get-CimInstance Win32_Process -Filter '{filter_}' | ForEach-Object {{ '{{0}}|{{1}}' -f $_.ProcessId, $_.CommandLine }}"], timeout=60)
    else:
        e = run(["ps", "-o", "pid=,args=", "-p", ",".join(map(str, pids))], timeout=20)
    if e.returncode != 0:
        return None
    aus = {}
    for z in e.stdout.splitlines():
        t = z.split("|", 1) if WINDOWS else z.strip().split(None, 1)
        if len(t) == 2 and t[0].strip().isdigit():
            aus[int(t[0])] = t[1]
    return aus


def fremd(client: tuple, eigene_shell: int) -> bool:
    """Nicht der eigene Vorbau: Unter Windows hängt der Client unter cmd (eigene_shell), am Mac ist er wegen `exec java`
    dieselbe pid wie die gestartete bash."""
    pid, ppid = client
    return not eigene_shell or (ppid != eigene_shell and pid != eigene_shell)


def fremder_bau_laeuft(eigene_shell: int = 0) -> bool:
    return any(fremd(c, eigene_shell) for c in gradle_clients())


def spiel_laeuft(liste=None) -> bool:
    """Steam-Spiel aktiv (Steam setzt RunningAppID) oder ein Call-of-Duty-Prozess: dann volle Leistung fürs Spiel."""
    if not WINDOWS:
        return False
    try:
        import winreg
        with winreg.OpenKey(winreg.HKEY_CURRENT_USER, r"Software\Valve\Steam") as k:
            if int(winreg.QueryValueEx(k, "RunningAppID")[0] or 0) not in STEAM_KEIN_SPIEL:
                return True
    except (OSError, ValueError):
        pass
    liste = liste if liste is not None else prozesse()
    return any(SPIEL_EXE.match(exe) for _, _, exe in liste)


def akkubetrieb() -> bool:
    try:
        if not WINDOWS:
            return sys.platform == "darwin" and "Battery Power" in run(["pmset", "-g", "batt"], timeout=10).stdout

        class Status(ctypes.Structure):
            _fields_ = [("ACLineStatus", ctypes.c_byte), ("BatteryFlag", ctypes.c_byte), ("BatteryLifePercent", ctypes.c_byte),
                        ("SystemStatusFlag", ctypes.c_byte), ("BatteryLifeTime", ctypes.c_ulong), ("BatteryFullLifeTime", ctypes.c_ulong)]
        s = Status()
        ctypes.windll.kernel32.GetSystemPowerStatus(ctypes.byref(s))
        return s.ACLineStatus == 0
    except Exception:
        return False


# ---------------------------------------------------------------- Ausführen, Sperren, Stand

class Ergebnis:
    def __init__(self, code: int, text: str):
        self.returncode, self.stdout = code, text


def run(befehl, timeout=None, cwd=None, ausweichen=None) -> Ergebnis:
    """Startet ohne Pipes (Ausgabe in eine Temp-Datei), nie eine Ausnahme: Fehler und Zeitlimit kommen als Code -1 zurück.
    ausweichen(shell_pid): alle 2 s gefragt; True → nur der Gradle-Client wird beendet, Code AUSGEWICHEN."""
    with tempfile.TemporaryFile() as aus:
        try:
            p = subprocess.Popen(befehl, cwd=cwd, env=ENV, stdin=subprocess.DEVNULL, stdout=aus, stderr=subprocess.STDOUT,
                                 creationflags=OHNE_FENSTER)
        except OSError as fehler:
            return Ergebnis(-1, str(fehler))
        ende = None if timeout is None else time.time() + timeout
        code = None
        while code is None:
            try:
                code = p.wait(timeout=2 if ausweichen else timeout)
            except subprocess.TimeoutExpired:
                weichen = bool(ausweichen and ausweichen(p.pid))
                if weichen or (ende is not None and time.time() >= ende):
                    if ausweichen:
                        beende_client(p.pid)  # Gradle: zuerst den Client, sonst baute er nach dem Ende von cmd verwaist weiter
                        try:
                            p.wait(timeout=30)
                        except subprocess.TimeoutExpired:
                            pass
                    if p.poll() is None:
                        p.kill()
                        p.wait()
                    code = AUSGEWICHEN if weichen else -1
        aus.seek(0)
        return Ergebnis(code, aus.read().decode("utf-8", "replace"))


def beende_client(shell: int) -> None:
    """Beendet die Gradle-Client-JVM unter der Shell, nie den Daemon (ein Baum-Kill, taskkill /T, träfe ihn mit)."""
    for pid, ppid in gradle_clients():
        if ppid == shell or pid == shell:
            try:
                os.kill(pid, 15)
            except OSError:
                pass


def git(*args: str, timeout=120):
    """Ausgabe von git; None bei Fehler oder Zeitlimit (nie als „leer“ werten)."""
    e = run(["git", "-C", str(PROGGS), *args], timeout=timeout)
    return e.stdout.strip() if e.returncode == 0 else None


def sauber(app: str) -> bool:
    return git("status", "--porcelain", "--", app) == ""


class Sperre:
    """Dateisperre für Windows (msvcrt) und Mac/Linux (fcntl). warten: Sekunden (None = unbegrenzt, 0 = sofort aufgeben)."""

    def __init__(self, pfad: Path, warten=None, takt=1.0):
        self.pfad, self.warten, self.takt, self.f = pfad, warten, takt, None

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
                time.sleep(self.takt)

    def __exit__(self, *a):
        if self.f:
            try:
                if WINDOWS:
                    self.f.seek(0)
                    msvcrt.locking(self.f.fileno(), msvcrt.LK_UNLCK, 1)
            except OSError:
                pass
            self.f.close()


def lies_json(pfad: Path) -> dict:
    try:
        return json.loads(pfad.read_text(encoding="utf-8"))
    except Exception:
        return {}


def schreib_json(pfad: Path, daten: dict) -> None:
    ORDNER.mkdir(parents=True, exist_ok=True)
    neu = pfad.with_name(f"{pfad.stem}.{os.getpid()}.neu")
    neu.write_text(json.dumps(daten, ensure_ascii=False, indent=1), encoding="utf-8")
    for _ in range(10):
        try:
            neu.replace(pfad)
            return
        except PermissionError:  # Windows: ein Leser hält die Datei gerade offen
            time.sleep(0.5)
    neu.unlink(missing_ok=True)


def aendere_stand(aenderung) -> None:
    """Lesen, ändern, schreiben unter einer eigenen Sperre: Brücken-Lauf und Takt-Lauf dürfen sich nicht überschreiben."""
    with Sperre(STAND_SPERRE, warten=60, takt=0.2) as frei:
        if not frei:
            log("stand.json gesperrt, Ergebnis nicht gemerkt (wird beim nächsten Lauf neu geprüft)")
            return
        stand = lies_json(STAND)
        aenderung(stand)
        schreib_json(STAND, stand)


def pausiert() -> bool:
    try:
        return time.time() - PAUSE.stat().st_mtime < PAUSE_SEKUNDEN
    except OSError:
        return False


# ---------------------------------------------------------------- Apps

def adb() -> str:
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        if WINDOWS:
            sdk = str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk")
        else:
            sdk = next((k for k in ("/opt/homebrew/share/android-commandlinetools", str(Path.home() / "Library" / "Android" / "sdk"))
                        if (Path(k) / "platform-tools" / "adb").is_file()), "")
    kandidat = Path(sdk) / "platform-tools" / ("adb.exe" if WINDOWS else "adb")
    return str(kandidat) if kandidat.is_file() else "adb"


def geraete_pakete() -> set:
    """Installierte Pakete je Gerät (Seriennummer), höchstens stündlich frisch gelesen (weckt sonst Handy und Tablet alle
    5 Minuten). Für nicht verbundene Geräte gilt ihre letzte Liste; deinstallierte Apps fallen beim nächsten Lesen heraus."""
    je_geraet = lies_json(PAKETE)
    try:
        frisch = time.time() - PAKETE.stat().st_mtime < 3600
    except OSError:
        frisch = False
    if not frisch:
        for z in run([adb(), "devices"], timeout=20).stdout.splitlines()[1:]:
            teile = z.split()
            if len(teile) < 2 or teile[1] != "device":
                continue
            seriell = run([adb(), "-s", teile[0], "shell", "getprop", "ro.serialno"], timeout=15).stdout.strip() or teile[0]
            e = run([adb(), "-s", teile[0], "shell", "pm", "list", "packages", "-3"], timeout=20)
            pakete = sorted({p.split(":", 1)[1].strip() for p in e.stdout.splitlines() if p.startswith("package:")})
            if e.returncode == 0 and pakete:
                je_geraet[seriell] = pakete
        schreib_json(PAKETE, je_geraet)  # auch ohne Gerät: Zeitstempel erneuern, damit adb erst in einer Stunde wieder fragt
    return {p for liste in je_geraet.values() if isinstance(liste, list) for p in liste}


def app_id(app: str) -> str:
    try:
        text = (PROGGS / app / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    except OSError:
        return ""
    m = re.search(r'applicationId\s*=\s*"([^"]+)"', text)
    return m.group(1) if m else ""


def baum_hashes() -> dict:
    """Ordner → Baum-Hash in HEAD, alles in einem git-Aufruf."""
    aus = git("ls-tree", "HEAD") or ""
    return {t[3]: t[2] for t in (z.split(None, 3) for z in aus.splitlines()) if len(t) == 4 and t[1] == "tree"}


def alle_apps() -> list:
    """Android-Apps im Repo, die auf Handy oder Tablet installiert sind (ohne bekannte Paketliste: alle mit Commit in den
    letzten 14 Tagen)."""
    pakete = geraete_pakete()
    apps = []
    for d in sorted(PROGGS.iterdir()):
        if not (d / "gradlew").is_file() or not (d / "app" / "build.gradle.kts").is_file():
            continue
        if pakete:
            kennung = app_id(d.name)
            if not kennung or not any(p == kennung or p.startswith(kennung + ".") for p in pakete):
                continue
        elif commitzeit(d.name) < time.time() - 14 * 86400:
            continue
        apps.append(d.name)
    return apps


_COMMITZEIT = {}


def commitzeit(app: str) -> int:
    if app not in _COMMITZEIT:
        _COMMITZEIT[app] = int(git("log", "-1", "--format=%ct", "--", app) or 0)
    return _COMMITZEIT[app]


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


def jvmargs(app: str) -> str:
    try:
        text = (PROGGS / app / "gradle.properties").read_text(encoding="utf-8")
    except OSError:
        return ""
    m = re.search(r"^\s*org\.gradle\.jvmargs\s*=(.*)$", text, re.M)
    return " ".join(m.group(1).split()) if m else ""


def jarvis_gruppe(app: str) -> bool:
    """Teilt sich die App den vorgewärmten Daemon (gleiche jvmargs wie Jarvis)?"""
    return app == "Jarvis" or jvmargs(app) == jvmargs("Jarvis")


# ---------------------------------------------------------------- Bauen

def repo_vorspulen() -> None:
    """Wie OpenLauncher/Services/RepoSync.cs: fetch (darf abbrechen) + merge --ff-only (nie abbrechen, sonst bleibt index.lock)."""
    gitdir = PROGGS / ".git"
    if any((gitdir / n).exists() for n in ("index.lock", "rebase-merge", "rebase-apply", "MERGE_HEAD")):
        return
    vorher = git("rev-parse", "HEAD")
    if run(["git", "-C", str(PROGGS), "fetch", "--quiet"], timeout=300).returncode != 0:
        return
    if vorher == git("rev-parse", "@{u}"):
        return  # nichts Neues: kein merge, kein Indexzugriff neben laufenden Sitzungen
    if run(["git", "-C", str(PROGGS), "merge", "--ff-only", "--quiet", "@{u}"]).returncode == 0:
        nachher = git("rev-parse", "HEAD")
        if nachher and nachher != vorher:
            _COMMITZEIT.clear()
            log(f"Repo vorgespult: {git('rev-list', '--count', f'{vorher}..{nachher}')} neue Commits")


def gradle_basis(ordner: Path) -> list:
    # Absoluter Pfad: Agenten-Shells setzen NoDefaultCurrentDirectoryInExePath=1, dann findet cmd gradlew.bat im cwd nicht.
    return ["cmd", "/c", str(ordner / "gradlew.bat")] if WINDOWS else ["bash", str(ordner / "gradlew")]


def gradlew(app: str) -> list:
    # Vorbau immer mit Wegwerf-Daemon: Er belegt nie den warmen Daemon der Sitzungen, lässt keinen weiteren 2-4-GB-Daemon
    # liegen, und Ausweichen kostet nichts. (Gemessen 13.11.2026: Bricht man einem Daemon einen laufenden Bau ab, per Client-
    # Ende oder Strg+C, beendet er sich oft selbst; ein Vorbau im gemeinsamen Daemon nähme den Sitzungen also den warmen Daemon.)
    workers = str(max(2, (os.cpu_count() or 4) // 2))  # halbe Kernzahl, gilt nur für diesen Bau
    return gradle_basis(PROGGS / app) + ["assembleDebug", "-q", f"--max-workers={workers}", "--no-daemon"]


class Ausweichen:
    """Während eines Vorbaus: Sitzungsbau (pause frisch oder fremder Gradle-Client, jede CLI) oder Spielstart hat Vorrang.
    Alle 2 s geprüft, über die Windows-API (Millisekunden, keine PowerShell)."""

    def __init__(self):
        self.beginn = time.time()

    def __call__(self, eigene_shell: int) -> bool:
        try:
            if PAUSE.stat().st_mtime > self.beginn - 5:
                return True
        except OSError:
            pass
        liste = prozesse()
        return any(fremd(c, eigene_shell) for c in gradle_clients(liste)) or spiel_laeuft(liste)


def baue(app: str):
    """True/False = Bau ok/fehlgeschlagen, None = nicht gebaut (Sitzung oder Spiel hat Vorrang)."""
    if git("lfs", "ls-files", "-I", f"{app}/**", timeout=60):
        run(["git", "-C", str(PROGGS), "lfs", "pull", f"--include={app}/**"])  # ohne Zeitlimit: ein Abbruch ließe index.lock liegen
    with Sperre(BAU_SPERRE):
        # Erst nach dem Warten prüfen: Inzwischen kann eine Sitzung bauen oder ein anderer Lauf die App gebaut haben.
        if pausiert() or fremder_bau_laeuft() or spiel_laeuft():
            return None
        hash_ = baum_hashes().get(app, "")
        if not hash_ or (lies_json(STAND).get(app) == hash_ and not ARGS.immer):
            return True
        ist_sauber = sauber(app)
        beginn = time.time()
        e = run(gradlew(app), cwd=PROGGS / app, timeout=3600, ausweichen=Ausweichen())
        dauer = int(time.time() - beginn)
        if e.returncode == AUSGEWICHEN:
            # Selbst pausieren: Sonst begänne der nächste Takt denselben Vorbau neben einem Bau ohne Hook (Codex, OpenCode …)
            # kalt von vorn und bräche ihn wieder ab, endlos.
            PAUSE.touch()
            log(f"{app}: Vorbau nach {dauer} s abgebrochen, eine Sitzung oder ein Spiel hat Vorrang (20 min Pause)")
            return None
        ok, fehler = e.returncode == 0, e.stdout.strip()[-400:] or ("nach 60 Minuten abgebrochen" if e.returncode == -1 else "")
    if not ist_sauber:
        # Offener Arbeitsstand (Auftragsstart): Der Bau wärmt, passt aber nicht genau zu HEAD, also nichts merken.
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


# ---------------------------------------------------------------- Daemon

def java_daemons():
    """(pid, Sekunden seit Start, ist Jarvis-Gruppe) aller Gradle-Daemons; teuer (PowerShell), daher nur bei Bedarf.
    None, wenn die Abfrage scheiterte."""
    if WINDOWS:
        e = run(["powershell", "-NoProfile", "-NonInteractive", "-Command",
                 "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | ForEach-Object { "
                 "'{0}|{1}|{2}' -f $_.ProcessId, [int]((Get-Date) - $_.CreationDate).TotalSeconds, $_.CommandLine }"], timeout=60)
    else:
        # macOS kennt etimes nicht, nur etime ([[TT-]hh:]mm:ss); Linux kann beides.
        e = run(["ps", "-u", str(os.getuid()), "-o", "pid=,etime=,args="], timeout=20)
    if e.returncode != 0:
        return None
    ziel = set(jvmargs("Jarvis").split())
    liste = []
    for z in e.stdout.splitlines():
        t = z.split("|", 2) if WINDOWS else z.split(None, 2)
        alter = t[1].strip() if len(t) == 3 else ""
        if not WINDOWS:
            alter = str(sekunden_aus_etime(alter))
        if len(t) == 3 and t[0].strip().isdigit() and alter.isdigit() and "GradleDaemon" in t[2]:
            schalter = {s for s in t[2].split() if s.startswith(("-X", "-Dfile."))}
            liste.append((int(t[0]), int(alter), schalter == ziel))
    return liste


def sekunden_aus_etime(text: str) -> int:
    """ps etime „[[TT-]hh:]mm:ss“ in Sekunden; -1, wenn unlesbar."""
    try:
        tage, _, rest = text.rpartition("-")
        teile = [int(x) for x in rest.split(":")]
        while len(teile) < 3:
            teile.insert(0, 0)
        return (int(tage) if tage else 0) * 86400 + teile[0] * 3600 + teile[1] * 60 + teile[2]
    except ValueError:
        return -1


def daemon_app() -> str:
    """App, deren `gradlew help` den gemeinsamen Daemon startet: Jarvis, sonst eine App mit denselben jvmargs."""
    if (PROGGS / "Jarvis" / "gradlew").is_file():
        return "Jarvis"
    for d in sorted(PROGGS.iterdir()):
        if (d / "gradlew").is_file() and (d / "app" / "build.gradle.kts").is_file() and jarvis_gruppe(d.name):
            return d.name
    return ""


def wecke_daemon(app: str) -> None:
    """Startet den gemeinsamen Daemon mit `gradlew help` (konfiguriert das Projekt, lädt AGP und Kotlin-Plugin, baut nichts;
    am PC um 15-30 s). Nur er bekommt 24 h Leerlauf, Sitzungs-Daemons behalten Gradles 3 h."""
    beginn = time.time()
    e = run(gradle_basis(PROGGS / app) + ["help", "-q", LEERLAUF_24H], cwd=PROGGS / app, timeout=600)
    log(f"Gradle-Daemon mit {app} gestartet in {int(time.time() - beginn)} s" + ("" if e.returncode == 0 else " (help fehlgeschlagen)"))


def daemon_pflege() -> None:
    """Wie am Server (Frank, 13.11.2026): Ein Daemon mit den Jarvis-Einstellungen läuft ab dem Windows-Start rund um die Uhr.
    Fehlt er, startet ihn ein Bau; ist er älter als 24 Stunden und gerade IDLE, wird genau er beendet und frisch gestartet
    (nie --stop: das träfe auch die Daemons laufender Sitzungen). Gemerkt wird er in daemon.json, damit der Takt nur prüft,
    ob die pid noch lebt, statt jedes Mal die teure Befehlszeilen-Abfrage zu starten."""
    gemerkt = lies_json(DAEMON)
    lebende = {pid for pid, _, exe in prozesse() if exe in ("java.exe", "java")}
    if gemerkt.get("pid") in lebende and time.time() - gemerkt.get("seit", 0) < DAEMON_LEBEN:
        return
    daemons = java_daemons()
    if daemons is None:
        return
    jarvis = sorted((alter, pid) for pid, alter, gruppe in daemons if gruppe)
    if jarvis and jarvis[0][0] < DAEMON_LEBEN:
        alter, pid = jarvis[0]
        schreib_json(DAEMON, {"pid": pid, "seit": time.time() - alter})
        return
    app = daemon_app()
    if not app:
        return
    if jarvis:
        alter, pid = jarvis[0]
        status = run(gradle_basis(PROGGS / app) + ["--status", "-q"], cwd=PROGGS / app, timeout=60).stdout
        if not re.search(rf"^\s*{pid}\s+IDLE\b", status, re.M):
            return  # baut gerade: beim nächsten Takt wieder
        with Sperre(BAU_SPERRE, warten=0) as frei:
            if not frei or fremder_bau_laeuft():
                return
            try:
                os.kill(pid, 15)
            except OSError:
                pass
        log("Gradle-Daemon nach 24 Stunden beendet, starte ihn frisch")
    wecke_daemon(app)
    neu =[(alter, pid) for pid, alter, gruppe in (java_daemons() or []) if gruppe]
    if neu:
        alter, pid = min(neu)
        schreib_json(DAEMON, {"pid": pid, "seit": time.time() - alter})


# ---------------------------------------------------------------- Abläufe

def lauf() -> int:
    einzeln = LAUF_SPERRE if not ARGS.app else ORDNER / f"lauf-{ARGS.app}.lock"
    with Sperre(einzeln, warten=0) as frei:
        if not frei or spiel_laeuft():
            return 0
        if akkubetrieb():
            log_gedrosselt("Aussetzen: Akkubetrieb, kein Vorspulen und kein Vorbau, bis das Netzteil steckt")
            return 0
        if ARGS.app:
            # Auftragsstart: Der OpenLauncher gleicht gleich danach das Repo ab (bis 20 s). Erst danach bauen, sonst übersetzte
            # der Vorbau den alten Stand und das Vorspulen änderte die Dateien mitten im Bau.
            time.sleep(25)
            if not fremder_bau_laeuft() and not pausiert():
                daemon_pflege()  # warmer Daemon für den ersten Sitzungsbau, falls er fehlt
                baue(ARGS.app)
            return 0
        if pausiert() or fremder_bau_laeuft():
            log_gedrosselt("Aussetzen: eine Sitzung baut (oder Pause nach ihrem Bau), weder Repo vorspulen noch bauen")
            return 0
        daemon_pflege()  # zuerst: warmer Daemon direkt nach dem Windows-Start, nicht erst nach allen Vorbauten
        repo_vorspulen()
        stand = lies_json(STAND)
        hashes = baum_hashes()
        offen = [a for a in alle_apps() if hashes.get(a) and stand.get(a) != hashes[a]]
        for app in sorted(offen, key=lambda a: (a != "Jarvis", -commitzeit(a))):
            if not sauber(app):
                continue  # eine Sitzung arbeitet daran
            if time.time() - commitzeit(app) < RUHE_NACH_COMMIT:
                continue  # frisch committet: die Sitzung baut und installiert gleich selbst
            if akkubetrieb() or baue(app) is None:
                log("Pause: Akku, eine Sitzung baut oder ein Spiel läuft, Vorwärmen setzt später fort")
                return 0
    return 0


def log_gedrosselt(text: str) -> None:
    """Höchstens einmal je Stunde ins Log, damit ein dauerhaftes Aussetzen sichtbar ist, ohne das Log zu fluten."""
    marke = ORDNER / "aussetzen.log-marke"
    try:
        if time.time() - marke.stat().st_mtime < 3600:
            return
    except OSError:
        pass
    ORDNER.mkdir(parents=True, exist_ok=True)
    marke.touch()
    log(text)


def warte() -> int:
    """Vor einem Sitzungsbau: Vorwärmen 20 Minuten pausieren. Ein laufender Vorbau sieht das binnen 2 s und weicht aus;
    gewartet wird nur, bis er die Sperre freigibt (sein Daemon ist dann frei), höchstens 2 Minuten.
    Sitzungen warten nie aufeinander: Mehrere Sitzungsbauten laufen gleichzeitig."""
    ORDNER.mkdir(parents=True, exist_ok=True)
    PAUSE.touch()
    beginn = time.time()
    with Sperre(BAU_SPERRE, warten=120, takt=0.3) as frei:
        gewartet = int(time.time() - beginn)
        if gewartet > 2:
            print(f"VORWAERMEN: {gewartet} s auf einen laufenden Vorbau gewartet" + ("" if frei else ", baue trotzdem"))
    return 0


def zeige() -> int:
    stand, hashes = lies_json(STAND), baum_hashes()
    for app in alle_apps():
        fehler = stand.get("_fehler", {}).get(app, "")
        aktuell = stand.get(app) == hashes.get(app)
        grund = "" if aktuell else ("  (offene Änderungen im Ordner, wird übersprungen)" if not sauber(app) else "  (kommt beim nächsten Takt)")
        print(f"{app:24} {'aktuell' if aktuell else 'offen'}{grund}" + (f"  FEHLER {fehler[-80:]!r}" if fehler and aktuell else ""))
    gemerkt = lies_json(DAEMON)
    if gemerkt:
        print(f"Warmer Daemon: pid {gemerkt.get('pid')}, seit {time.strftime('%d.%m. %H:%M', time.localtime(gemerkt.get('seit', 0)))}")
    return 0


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("aktion", choices=["lauf", "warte", "stand"])
    p.add_argument("--app", default="")
    p.add_argument("--immer", action="store_true", help="auch bauen, wenn der Stand schon vorgewärmt ist (wärmt den Daemon)")
    ARGS = p.parse_args()
    if ARGS.app:
        ARGS.app = ordner_zu(ARGS.app)
        if not ARGS.app:
            sys.exit(0)
    try:
        sys.exit({"lauf": lauf, "warte": warte, "stand": zeige}[ARGS.aktion]())
    except Exception as fehler:  # Hintergrundlauf: nie still sterben; warte darf einen Sitzungsbau nie verhindern
        log(f"Fehler ({ARGS.aktion}): {fehler!r}")
        sys.exit(0 if ARGS.aktion == "warte" else 1)
