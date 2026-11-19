#!/usr/bin/env python3
"""Sichtbare Minutenprüfung eines Befehls, der nach dem 60-s-Zeitlimit weiterläuft (Franks Regel vom 15.11.2026).

Aufruf (im Vordergrund, Beschreibung „Minute N: Lauf prüfen“; Windows: python -X utf8):
    python3 ~/proggs/Werkzeuge/lauf-waechter/minute.py <ausgabedatei> <minute>

Beobachtet höchstens 40 s (bleibt so selbst unter dem 60-s-Limit) und endet sofort, wenn der Lauf fertig ist. Danach eine Zeile:
  FERTIG       – Claude Code hat „[exited with code N]“ geschrieben bzw. kein Prozess hält die Ausgabe mehr offen
  LÄUFT        – arbeitet (CPU-Zeit steigt oder neue Ausgabe), keine Fehlermeldung
  WARTET       – absichtliche Pause: sleep, Ja/Nein-Dialog (osascript, unter Windows das Fenster „OpenLauncher aktualisieren“
                 bzw. „Spracheingabe aktualisieren“), Overlay-Frage. NIE beenden, Frank entscheidet gerade.
  ÜBERTRÄGT    – adb push/install mit kaum CPU: netzgebunden, kein Hänger
  FEHLER       – eindeutige Fehlermeldung in der neuen Ausgabe (Zeile wird gezeigt)
  HÄNGER?      – keine CPU-Zeit und keine neue Ausgabe im ganzen Fenster: Prozesse ansehen, Ursache beheben
  STILL        – keine neue Ausgabe und keine Prozessdaten (bzw. unter Windows nur die Shell selbst, z. B. Start-Sleep im
                 Skript): Prozesse von Hand prüfen
  DATEI FEHLT  – Pfad falsch oder noch nicht angelegt
CPU zählt als kumulierte CPU-Zeit (ps -o time, unter Windows Win32_Process Kernel- + User-Zeit), nicht als Durchschnitt;
Gradle: der Daemon arbeitet, nicht der Client.
Windows: Den Lauf findet das Skript über den Dateinamen in der Befehlszeile (PowerShell-Werkzeug) bzw. als einzige Shell
von Claude Code, die mit der Ausgabedatei entstand (Bash-Werkzeug); Git-Bash-exec verknüpft Gits ps -e (WINPID).
Lange Befehle nie in `| tail`/`| head` leiten: Die Pipe hält die Ausgabe bis zum Ende zurück, dann sieht diese Prüfung nichts.
"""
import base64, os, re, shutil, subprocess, sys, time

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

FENSTER = 40
TAKT = 5
# Nur eindeutige Fehler (Groß-/Kleinschreibung zählt); „0 failed“, Warnungen mit „Exception“ und INSTALL_FAILED_MISSING_SPLIT
# (Jarvis-Installationsschutz, kein Fehler) zählen nicht.
FEHLER = re.compile(r'(^e: |^FAILURE: |BUILD FAILED|^Traceback \(most recent call last\)|^fatal: |command not found|'
                    r'Exception in thread|^error: |^Error: |INSTALL_FAILED_(?!MISSING_SPLIT)|JARVIS_INSTALL_STATUS=fehler|'
                    r'APK_UPDATE_STATUS=fehler)', re.M)
DAEMON = re.compile(r'GradleDaemon|KotlinCompileDaemon|kotlin-daemon', re.I)
WARTEN = re.compile(r'^(/bin/)?sleep \d|osascript|display dialog|zenity|Start-Sleep|timeout(\.exe)? /t|InstallFrage', re.I)
ENDE = re.compile(r'^\[exited with code (-?\d+)\]', re.M)

# Windows: Befehlszeilen tragen den vollen Pfad („C:\…\usr\bin\sleep.exe" 80), Dialoge laufen im Prozess der Shell selbst
# (MessageBoxTimeoutW in update-launcher.ps1 / rebuild-overlay.ps1) und sind nur am Fenstertitel erkennbar.
WARTEN_WIN = re.compile(r'(^|[\\/"\s])sleep(\.exe)?"?\s+\d|Start-Sleep|timeout(\.exe)?"?\s+/t|InstallFrage', re.I)
DIALOG_TITEL = re.compile(r'OpenLauncher aktualisieren|Spracheingabe aktualisieren')
# jarvis-install.ps1 wartet im eigenen Prozess (Start-Sleep), sichtbar nur an seiner letzten Ausgabezeile.
WARTE_AUSGABE = re.compile(r'^(Overlay geöffnet, warte auf Antwort|Gewählt: in [1-5] Minute|Eine andere Jarvis-Installation läuft, warte)')
# Die Hüllen von Claude Code tragen den ganzen Befehlstext (bash -c "… eval '…'") bzw. gar keinen (pwsh mit Skript aus der
# Umgebung): Für Befehlszeilen-Muster zählen sie nicht, sonst wäre jeder Befehl mit „Start-Sleep“ irgendwo darin „WARTET“.
HUELLE = re.compile(r'shell-snapshots[\\/]snapshot-|CLAUDE_CODE_SHELL_LAUNCHER_SCRIPT|__claudeCodeScript', re.I)
SHELLS = ('bash.exe', 'sh.exe', 'pwsh.exe', 'powershell.exe', 'cmd.exe')
CLAUDE = ('claude.exe', 'node.exe')
WIN_ABFRAGE_S = 8


def zeit_sek(t):
    """ps-Zeit [[dd-]hh:]mm:ss[.cc] in Sekunden."""
    tage = 0
    if '-' in t:
        d, t = t.split('-', 1)
        tage = int(d)
    teile = [float(x.replace(',', '.')) for x in t.split(':')]
    s = 0.0
    for x in teile:
        s = s * 60 + x
    return tage * 86400 + s


def ps():
    try:
        out = subprocess.run(['ps', '-A', '-o', 'pid=,ppid=,time=,etime=,command='], capture_output=True, text=True, timeout=5).stdout
    except Exception:
        return None
    zeilen = []
    for z in out.splitlines():
        t = z.split(None, 4)
        if len(t) == 5:
            try:
                zeilen.append((int(t[0]), int(t[1]), zeit_sek(t[2]), t[3], t[4]))
            except ValueError:
                pass
    return zeilen


def schreiber(datei):
    """PIDs, die die Ausgabedatei offen halten (lsof am Mac, fuser unter Linux); None = unbekannt."""
    for befehl in (['lsof', '-t', datei], ['fuser', datei]):
        try:
            r = subprocess.run(befehl, capture_output=True, text=True, timeout=5)
        except Exception:
            continue
        pids = [int(x) for x in re.findall(r'\d+', r.stdout)]
        if r.returncode == 0 or (r.returncode == 1 and not r.stderr.strip()):
            return pids
    return None


# --- Windows: Prozessdaten über Win32_Process (pwsh, sonst Windows PowerShell) ---
WIN_SKRIPT = r'''
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$ErrorActionPreference = 'SilentlyContinue'
$w = @{}
foreach ($p in Get-Process) { if ($p.MainWindowTitle) { $w[[int]$p.Id] = $p.MainWindowTitle } }
foreach ($p in Get-CimInstance Win32_Process) {
    $s = 0
    if ($p.CreationDate) { $s = ([DateTimeOffset]$p.CreationDate).ToUnixTimeMilliseconds() / 1000 }
    $c = ([double]$p.KernelModeTime + [double]$p.UserModeTime) / 1e7
    $l = if ($p.CommandLine) { $p.CommandLine } else { $p.Name }
    $t = "$($w[[int]$p.ProcessId])"
    (($p.ProcessId, $p.ParentProcessId, $c, $s, $p.Name, $t, $l) -join "`t") -replace '[\r\n]+', ' '
}
'''


def etime(sek):
    """Laufzeit im ps-Format [[dd-]hh:]mm:ss."""
    sek = max(0, int(sek))
    t, sek = divmod(sek, 86400)
    h, rest = divmod(sek, 3600)
    m, s = divmod(rest, 60)
    if t:
        return '%d-%02d:%02d:%02d' % (t, h, m, s)
    if h:
        return '%02d:%02d:%02d' % (h, m, s)
    return '%02d:%02d' % (m, s)


def win_ps():
    """[(pid, ppid, cpu_s, etime, befehl, name, start_epoch, fenstertitel)] oder None."""
    exe = shutil.which('pwsh') or shutil.which('powershell')
    if not exe:
        return None
    kodiert = base64.b64encode(WIN_SKRIPT.encode('utf-16-le')).decode('ascii')
    try:
        r = subprocess.run([exe, '-NoProfile', '-NonInteractive', '-EncodedCommand', kodiert],
                           capture_output=True, timeout=WIN_ABFRAGE_S)
    except Exception:
        return None
    jetzt = time.time()
    zeilen = []
    for z in r.stdout.decode('utf-8', 'replace').splitlines():
        t = z.split('\t', 6)
        if len(t) != 7:
            continue
        try:
            start = float(t[3].replace(',', '.'))
            zeilen.append((int(t[0]), int(t[1]), float(t[2].replace(',', '.')), etime(jetzt - start) if start else '?',
                           t[6].strip() or t[4], t[4].lower(), start, t[5].strip()))
        except ValueError:
            pass
    return zeilen or None


def msys_kanten():
    """Git Bash startet ein Programm per exec als neuen Windows-Prozess, dessen Elternprozess schon weg ist: Gits ps -e kennt
    die Verwandtschaft über die msys-PID und liefert die Windows-PID (WINPID) dazu. Ergebnis: [(eltern_winpid, kind_winpid)]."""
    kandidaten = []
    for p in (shutil.which('ps'), shutil.which('git')):
        if p and 'git' in p.lower():
            wurzel = os.path.dirname(os.path.dirname(p))
            kandidaten += [p, os.path.join(wurzel, 'usr', 'bin', 'ps.exe')]
    kandidaten.append(r'C:\Program Files\Git\usr\bin\ps.exe')
    exe = next((k for k in kandidaten if k and k.lower().endswith('ps.exe') and os.path.isfile(k)), None)
    if not exe:
        return []
    try:
        out = subprocess.run([exe, '-e'], capture_output=True, text=True, timeout=5).stdout
    except Exception:
        return []
    msys = {}
    for z in out.splitlines()[1:]:
        t = z.split()
        if t and not t[0].isdigit():
            t = t[1:]  # Statusbuchstabe (I, S, O) vor der PID
        try:
            msys[int(t[0])] = (int(t[1]), int(t[3]))
        except (IndexError, ValueError):
            pass
    return [(msys[eltern][1], winpid) for eltern, winpid in msys.values() if eltern in msys]


def eigene_kette(alle, kanten=()):
    """Vorfahren dieses Skripts, nächste zuerst; über Windows-Eltern UND msys-exec-Kanten (Git Bash)."""
    eltern = {}
    for z in alle:
        eltern.setdefault(z[0], []).append(z[1])
    for e, k in kanten:
        eltern.setdefault(k, []).append(e)
    kette, offen = [], [os.getpid()]
    while offen:
        p = offen.pop(0)
        if p not in kette:
            kette.append(p)
            offen += eltern.get(p, [])
    return kette


def win_wurzeln(alle, datei, kanten=()):
    """Prozesse, die den Lauf tragen: exakt über den Dateinamen in der Befehlszeile (PowerShell-Werkzeug: cmd.exe … > datei),
    sonst die EINZIGE Shell von Claude Code, die mit der Ausgabedatei entstand (Bash-Werkzeug). Mehrdeutig: keine."""
    vorfahren = eigene_kette(alle, kanten)
    kette = set(vorfahren)
    name = os.path.basename(datei).lower()
    genau = [z[0] for z in alle if name in z[4].lower() and z[0] not in kette and 'minute.py' not in z[4]]
    if genau:
        return genau
    namen = {z[0]: z[5] for z in alle}
    claude = next((p for p in vorfahren if namen.get(p) in CLAUDE), None)
    if claude is None:
        return []
    try:
        st = os.stat(datei)
        entstanden = getattr(st, 'st_birthtime', None) or st.st_ctime  # Windows: st_ctime = Erstellzeit
    except OSError:
        return []
    kandidaten = [z[0] for z in alle if z[1] == claude and z[0] not in kette and z[5] in SHELLS and z[6]
                  and abs(z[6] - entstanden) <= 5]
    return kandidaten if len(kandidaten) == 1 else []


def baum(alle, wurzeln, kanten=()):
    kinder = {}
    for z in alle:
        kinder.setdefault(z[1], []).append(z[0])
    for eltern, kind in kanten:
        kinder.setdefault(eltern, []).append(kind)
    gesehen, offen = set(), list(wurzeln)
    while offen:
        p = offen.pop()
        if p not in gesehen:
            gesehen.add(p)
            offen += kinder.get(p, [])
    return [z for z in alle if z[0] in gesehen and 'minute.py' not in z[4]]


def cpu_proben(alle, prozesse):
    """{pid: kumulierte CPU-Zeit} der Prozesse des Laufs, bei Gradle samt Daemons."""
    d = {z[0]: z[2] for z in prozesse}
    if any('gradle' in z[4].lower() for z in prozesse):
        d.update({z[0]: z[2] for z in alle if DAEMON.search(z[4])})
    return d


def cpu_merken(erste, letzte, probe, daemons=()):
    """Zuwachs je Prozess statt Summe der gerade lebenden: Endet ein Schritt und startet der nächste, fiele die Summe
    (gemessen am Mac 19.11.2026: adb push nacheinander, „-0.0 s CPU“), und ein fleißiger Lauf aus kurzen Schritten ohne
    Ausgabe hieße „HÄNGER?“. Ein Prozess, der erst nach der ersten Probe auftaucht, zählt ab 0."""
    if erste is None:
        erste = dict(probe)
    else:
        for pid, cpu in probe.items():
            # Ein Gradle-/Kotlin-Daemon läuft oft seit Stunden: erst ab seinem ersten Auftauchen zählen (Review 19.11.2026).
            erste.setdefault(pid, cpu if pid in daemons else 0.0)
    letzte.update(probe)
    return erste, sum(letzte[p] - erste.get(p, 0.0) for p in letzte)


def kurz(cmd, n=70):
    cmd = re.sub(r'\s+', ' ', cmd)
    return cmd if len(cmd) <= n else cmd[:n - 1] + '…'


# adb-Übertragung (push/install) ist netzgebunden und braucht kaum CPU: kein Hänger (Jarvis-Installation, 17.11.2026).
UEBERTRAGUNG = re.compile(r'\badb\b.*\b(push|install|install-multiple|sync)\b')
# Unter den Installationsskripten (eigene Fristen) ist jeder adb-Schritt geräte- bzw. netzgebunden, auch adb shell
# (sha256sum, Delta am Handy, pm install-write): gemessen am Mac 18.11.2026, Sitzung e9a405d7, „HÄNGER?“ mitten in jarvis-install.
ADB_SKRIPT = re.compile(r'jarvis-install\.(sh|ps1)|auf-beide\.sh|delta-push|apk-update', re.I)
ADB = re.compile(r'(^|[\\/\s"])adb(\.exe)?"?\s', re.I)


def uebertraegt(befehle):
    return any(UEBERTRAGUNG.search(b) for b in befehle) or (
        any(ADB.search(b) for b in befehle) and any(ADB_SKRIPT.search(b) for b in befehle))


def lies_ab(datei, ab):
    with open(datei, 'rb') as f:
        f.seek(ab)
        return f.read().decode('utf-8', 'replace')


def main():
    if len(sys.argv) < 3:
        print("Aufruf: minute.py <ausgabedatei> <minute>")
        return
    datei, minute = sys.argv[1], sys.argv[2]
    if not os.path.exists(datei):
        print('Minute %s · DATEI FEHLT – %s gibt es nicht (Pfad prüfen)' % (minute, datei))
        return
    start = time.time()
    groesse0 = os.path.getsize(datei)
    posix = os.name != 'nt'
    erste, letzte, zuwachs = None, {}, None
    proben = 0
    wurzeln, kanten = [], []
    fertig, prozesse = False, []
    while True:
        if ENDE.search(lies_ab(datei, max(0, os.path.getsize(datei) - 200))):
            fertig = True
            break
        if posix:
            pids = schreiber(datei)
            if pids is not None and not pids:
                fertig = True
                break
            alle = ps()
            if alle and pids:
                prozesse = baum(alle, pids)
                erste, zuwachs = cpu_merken(erste, letzte, cpu_proben(alle, prozesse), {z[0] for z in alle if DAEMON.search(z[4])})
        else:
            # FERTIG kommt unter Windows nur aus der Markierung „[exited with code N]“ (beide Werkzeuge schreiben sie).
            alle = win_ps()
            if alle:
                kanten = msys_kanten()
                if not wurzeln:
                    wurzeln = win_wurzeln(alle, datei, kanten)
                lebend = {z[0] for z in alle}
                if wurzeln and any(w in lebend for w in wurzeln):
                    prozesse = baum(alle, wurzeln, kanten)
                    erste, zuwachs = cpu_merken(erste, letzte, cpu_proben(alle, prozesse), {z[0] for z in alle if DAEMON.search(z[4])})
                    proben += 1
        rest = FENSTER - (time.time() - start)
        if rest <= 1:
            break
        time.sleep(min(TAKT, rest))
    dauer = int(time.time() - start)
    neu = lies_ab(datei, groesse0)
    zeilen = [z for z in neu.splitlines() if z.strip()]
    fehler = [z for z in zeilen if FEHLER.search(z)]
    letzte = zeilen[-1] if zeilen else ''
    ende = ENDE.search(neu) or ENDE.search(lies_ab(datei, max(0, os.path.getsize(datei) - 200)))
    if posix:
        arbeit = zuwachs
        # Die zsh-Hülle (zsh -c "source …snapshot… && eval '<ganzer Befehl>'") trägt den ganzen Befehlstext: Zählte sie
        # mit, wäre jeder Befehl mit „osascript“ oder „adb … install“ irgendwo darin WARTET bzw. ÜBERTRÄGT, auch während
        # ein Gradle-Bau davor hängt (gemessen am Mac 19.11.2026). Ihre CPU-Zeit zählt weiter.
        echte = [z for z in prozesse if not HUELLE.search(z[4])]
        wartend = [(z[4], z[3]) for z in echte if WARTEN.search(z[4])]
        befehle = [z[4] for z in echte]
        nur_huelle = False
    else:
        # Erst zwei Proben ergeben einen Zuwachs; eine einzige hieße 0 s CPU und damit ein falsches „HÄNGER?“.
        arbeit = zuwachs if proben >= 2 else None
        echte = [z for z in prozesse if not HUELLE.search(z[4]) and z[5] != 'conhost.exe']
        befehle = [z[4] for z in echte]
        wartend = [(z[4], z[3]) for z in echte if WARTEN_WIN.search(z[4])]
        wartend += [('Dialog „%s“' % z[7], z[3]) for z in prozesse if DIALOG_TITEL.search(z[7])]
        nur_huelle = bool(prozesse) and not echte
    # jarvis-install wartet auf die Overlay-Antwort mit kurzen adb-Abfragen und sleep 0.2: Welcher Prozess gerade getroffen
    # wird, ist Zufall; sicher erkennbar ist das Warten nur an seiner letzten Ausgabezeile (Windows und Mac gleich).
    schluss = [z for z in lies_ab(datei, max(0, os.path.getsize(datei) - 4000)).splitlines() if z.strip()]
    if schluss and WARTE_AUSGABE.search(schluss[-1].strip()):
        wartend.append((schluss[-1].strip(), '%d s' % dauer if not zeilen else 'gerade'))
    if fertig:
        stand = 'FERTIG – der Lauf ist beendet%s, Ergebnis jetzt auswerten' % (' (Exit %s)' % ende.group(1) if ende else '')
    elif fehler:
        stand = 'FEHLER – Fehlermeldung in der Ausgabe: ' + kurz(fehler[-1], 120)
    elif wartend:
        stand = 'WARTET – absichtliche Pause (%s, seit %s), kein Fehler, nicht beenden' % (kurz(wartend[0][0], 40), wartend[0][1])
    elif arbeit is None and not zeilen:
        stand = 'STILL – keine neue Ausgabe seit %d s, Prozessdaten fehlen hier: Prozesse von Hand prüfen' % dauer
    elif arbeit is not None and arbeit < 0.2 and not zeilen and uebertraegt(befehle):
        stand = 'ÜBERTRÄGT – adb lädt über WLAN/Tunnel (kaum CPU, kein Hänger), nicht beenden, nächste Prüfung in Minute %s' % (int(minute) + 1 if minute.isdigit() else '?')
    elif arbeit is not None and arbeit < 0.2 and not zeilen and nur_huelle:
        # Nur die Shell selbst läuft (PowerShell-Skript im Prozess, z. B. Start-Sleep oder ein Warten auf Eingabe): von außen
        # nicht unterscheidbar, deshalb kein „HÄNGER?“ (das hieße beenden).
        stand = 'STILL – keine neue Ausgabe und keine CPU-Zeit seit %d s, nur die Shell selbst läuft (z. B. Start-Sleep im Skript): Prozesse von Hand prüfen' % dauer
    elif arbeit is not None and arbeit < 0.2 and not zeilen:
        stand = 'HÄNGER? – keine CPU-Zeit und keine neue Ausgabe seit %d s: Prozesse ansehen und Ursache beheben' % dauer
    else:
        stand = 'LÄUFT – arbeitet fehlerfrei weiter (%s%d neue Zeilen), nächste Prüfung in Minute %s' % (
            ('%.1f s CPU, ' % arbeit) if arbeit is not None else '', len(zeilen), int(minute) + 1 if minute.isdigit() else '?')
    print('Minute %s · %s' % (minute, stand))
    if letzte:
        print('  Letzte Ausgabe: ' + kurz(letzte, 140))
    if prozesse and not fertig:
        for z in prozesse[-4:]:
            print('  Prozess %d · läuft %s · %s' % (z[0], z[3], kurz(z[4])))


if __name__ == '__main__':
    main()
