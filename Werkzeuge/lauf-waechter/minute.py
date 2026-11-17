#!/usr/bin/env python3
"""Sichtbare Minutenprüfung eines Befehls, der nach dem 60-s-Zeitlimit weiterläuft (Franks Regel vom 15.11.2026).

Aufruf (im Vordergrund, Beschreibung „Minute N: Lauf prüfen“; Windows: python -X utf8):
    python3 ~/proggs/Werkzeuge/lauf-waechter/minute.py <ausgabedatei> <minute>

Beobachtet höchstens 40 s (bleibt so selbst unter dem 60-s-Limit) und endet sofort, wenn der Lauf fertig ist. Danach eine Zeile:
  FERTIG       – Claude Code hat „[exited with code N]“ geschrieben bzw. kein Prozess hält die Ausgabe mehr offen
  LÄUFT        – arbeitet (CPU-Zeit steigt oder neue Ausgabe), keine Fehlermeldung
  WARTET       – absichtliche Pause: sleep, Ja/Nein-Dialog (osascript), Overlay-Frage. NIE beenden, Frank entscheidet gerade.
  FEHLER       – eindeutige Fehlermeldung in der neuen Ausgabe (Zeile wird gezeigt)
  HÄNGER?      – keine CPU-Zeit und keine neue Ausgabe im ganzen Fenster: Prozesse ansehen, Ursache beheben
  STILL        – (nur ohne Prozessdaten, z. B. Windows) keine neue Ausgabe: Prozesse von Hand prüfen
  DATEI FEHLT  – Pfad falsch oder noch nicht angelegt
CPU zählt als kumulierte CPU-Zeit (ps -o time), nicht als Durchschnitt; Gradle: der Daemon arbeitet, nicht der Client.
Lange Befehle nie in `| tail`/`| head` leiten: Die Pipe hält die Ausgabe bis zum Ende zurück, dann sieht diese Prüfung nichts.
"""
import os, re, subprocess, sys, time

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


def baum(alle, wurzeln):
    kinder = {}
    for z in alle:
        kinder.setdefault(z[1], []).append(z[0])
    gesehen, offen = set(), list(wurzeln)
    while offen:
        p = offen.pop()
        if p not in gesehen:
            gesehen.add(p)
            offen += kinder.get(p, [])
    return [z for z in alle if z[0] in gesehen and 'minute.py' not in z[4]]


def cpu_zeit(alle, prozesse):
    s = sum(z[2] for z in prozesse)
    if any('gradle' in z[4].lower() for z in prozesse):
        s += sum(z[2] for z in alle if DAEMON.search(z[4]))
    return s


def kurz(cmd, n=70):
    cmd = re.sub(r'\s+', ' ', cmd)
    return cmd if len(cmd) <= n else cmd[:n - 1] + '…'


# adb-Übertragung (push/install) ist netzgebunden und braucht kaum CPU: kein Hänger (Jarvis-Installation, 17.11.2026).
UEBERTRAGUNG = re.compile(r'\badb\b.*\b(push|install|install-multiple|sync)\b')


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
    cpu0 = cpu1 = None
    fertig, prozesse = False, []
    while True:
        if ENDE.search(lies_ab(datei, max(0, os.path.getsize(datei) - 200))):
            fertig = True
            break
        pids = schreiber(datei) if posix else None
        if pids is not None and not pids:
            fertig = True
            break
        alle = ps() if posix else None
        if alle and pids:
            prozesse = baum(alle, pids)
            c = cpu_zeit(alle, prozesse)
            cpu0 = c if cpu0 is None else cpu0
            cpu1 = c
        rest = FENSTER - (time.time() - start)
        if rest <= 0:
            break
        time.sleep(min(TAKT, rest))
    dauer = int(time.time() - start)
    neu = lies_ab(datei, groesse0)
    zeilen = [z for z in neu.splitlines() if z.strip()]
    fehler = [z for z in zeilen if FEHLER.search(z)]
    letzte = zeilen[-1] if zeilen else ''
    arbeit = (cpu1 - cpu0) if cpu0 is not None else None
    wartend = [z for z in prozesse if WARTEN.search(z[4])]
    ende = ENDE.search(neu) or ENDE.search(lies_ab(datei, max(0, os.path.getsize(datei) - 200)))
    if fertig:
        stand = 'FERTIG – der Lauf ist beendet%s, Ergebnis jetzt auswerten' % (' (Exit %s)' % ende.group(1) if ende else '')
    elif fehler:
        stand = 'FEHLER – Fehlermeldung in der Ausgabe: ' + kurz(fehler[-1], 120)
    elif wartend:
        stand = 'WARTET – absichtliche Pause (%s, seit %s), kein Fehler, nicht beenden' % (kurz(wartend[0][4], 40), wartend[0][3])
    elif arbeit is None and not zeilen:
        stand = 'STILL – keine neue Ausgabe seit %d s, Prozessdaten fehlen hier: Prozesse von Hand prüfen' % dauer
    elif arbeit is not None and arbeit < 0.2 and not zeilen and any(UEBERTRAGUNG.search(z[4]) for z in prozesse):
        stand = 'ÜBERTRÄGT – adb lädt über WLAN/Tunnel (kaum CPU, kein Hänger), nicht beenden, nächste Prüfung in Minute %s' % (int(minute) + 1 if minute.isdigit() else '?')
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
