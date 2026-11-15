#!/usr/bin/env python3
"""Sichtbare Minutenprüfung eines Befehls, der nach dem 60-s-Zeitlimit im Hintergrund weiterläuft (Franks Regel vom 15.11.2026).

Aufruf (im Vordergrund, Beschreibung „Minute N: Lauf prüfen“):
    python3 ~/proggs/Werkzeuge/lauf-waechter/minute.py <ausgabedatei> <minute>

Beobachtet höchstens 50 s und endet sofort, wenn der Lauf fertig ist. Danach eine Zeile Einschätzung:
  FERTIG    – kein Prozess schreibt mehr in die Ausgabe
  LÄUFT     – arbeitet (CPU oder neue Ausgabe), keine Fehlermeldung
  FEHLER    – Fehlermeldung in der neuen Ausgabe (Zeile wird gezeigt)
  WARTET    – absichtliche Pause im Lauf (sleep, z. B. gewählte Installationszeit), kein Fehler
  HÄNGER?   – kein CPU-Verbrauch und keine neue Ausgabe im ganzen Fenster: Prozesse ansehen, Ursache beheben
Gradle: Der Client tut nichts, der Daemon arbeitet; dessen CPU zählt mit (GradleDaemon, KotlinCompileDaemon).
Unter Windows ohne ps/lsof nur Ausgabe und Fehlermeldungen. Lange Befehle nie in `| tail`/`| head` leiten: Die Pipe hält
die Ausgabe bis zum Ende zurück, dann sieht diese Prüfung nichts (15.11.2026: „Gewählt: in 5 Minuten“ war unsichtbar).
"""
import os, re, subprocess, sys, time

FENSTER = 50
FEHLER = re.compile(r'(^e: |error:|\bERROR\b|FAILURE|FAILED|BUILD FAILED|Exception|Traceback|fatal:|No such file|command not found|Permission denied)', re.I)
DAEMON = re.compile(r'GradleDaemon|KotlinCompileDaemon|kotlin-daemon', re.I)


def ps():
    try:
        out = subprocess.run(['ps', '-A', '-o', 'pid=,ppid=,pcpu=,etime=,command='], capture_output=True, text=True, timeout=10).stdout
    except Exception:
        return None
    zeilen = []
    for z in out.splitlines():
        t = z.split(None, 4)
        if len(t) == 5:
            try:
                zeilen.append((int(t[0]), int(t[1]), float(t[2].replace(',', '.')), t[3], t[4]))
            except ValueError:
                pass
    return zeilen


def schreiber(datei):
    """PIDs, die die Ausgabedatei offen halten (lsof am Mac, fuser unter Linux)."""
    for befehl in (['lsof', '-t', datei], ['fuser', datei]):
        try:
            r = subprocess.run(befehl, capture_output=True, text=True, timeout=10)
            pids = [int(x) for x in re.findall(r'\d+', r.stdout)]
            if r.returncode in (0, 1):
                return pids
        except Exception:
            continue
    return None


def baum(alle, wurzeln):
    kinder = {}
    for pid, ppid, *_ in alle:
        kinder.setdefault(ppid, []).append(pid)
    gesehen, offen = set(), list(wurzeln)
    while offen:
        p = offen.pop()
        if p in gesehen:
            continue
        gesehen.add(p)
        offen += kinder.get(p, [])
    return [z for z in alle if z[0] in gesehen]


def kurz(cmd, n=70):
    cmd = re.sub(r'\s+', ' ', cmd)
    return cmd if len(cmd) <= n else cmd[:n - 1] + '…'


def main():
    if len(sys.argv) < 3:
        print("Aufruf: minute.py <ausgabedatei> <minute>")
        return
    datei, minute = sys.argv[1], sys.argv[2]
    start = time.time()
    groesse0 = os.path.getsize(datei) if os.path.exists(datei) else 0
    cpu_max, fertig, prozesse = 0.0, False, []
    while time.time() - start < FENSTER:
        pids = schreiber(datei) if os.name != 'nt' else None
        if pids is not None and not pids:
            fertig = True
            break
        alle = ps() if os.name != 'nt' else None
        if alle and pids:
            prozesse = [z for z in baum(alle, pids) if 'minute.py' not in z[4]]
            cpu = sum(z[2] for z in prozesse)
            if any('gradle' in z[4].lower() for z in prozesse):
                cpu += sum(z[2] for z in alle if DAEMON.search(z[4]))
            cpu_max = max(cpu_max, cpu)
        time.sleep(5)
    dauer = int(time.time() - start)
    neu = ''
    if os.path.exists(datei):
        with open(datei, 'rb') as f:
            f.seek(groesse0)
            neu = f.read().decode('utf-8', 'replace')
    zeilen = [z for z in neu.splitlines() if z.strip()]
    fehler = [z for z in zeilen if FEHLER.search(z)]
    letzte = zeilen[-1] if zeilen else ''
    if fertig:
        stand = 'FERTIG – der Lauf ist beendet, Ergebnis jetzt auswerten'
    elif fehler:
        stand = 'FEHLER – Fehlermeldung in der Ausgabe: ' + kurz(fehler[-1], 120)
    elif any(re.match(r'(/bin/)?sleep \d', z[4]) for z in prozesse):
        s = next(z for z in prozesse if re.match(r'(/bin/)?sleep \d', z[4]))
        stand = 'WARTET – absichtliche Pause im Lauf (%s, läuft seit %s), kein Fehler' % (kurz(s[4], 30), s[3])
    elif cpu_max < 1.0 and not zeilen and prozesse:
        stand = 'HÄNGER? – kein CPU-Verbrauch und keine neue Ausgabe seit %d s: Prozesse ansehen und Ursache beheben' % dauer
    else:
        stand = 'LÄUFT – arbeitet fehlerfrei weiter (CPU bis %d %%, %d neue Zeilen), nächste Prüfung in Minute %d' % (
            cpu_max, len(zeilen), int(minute) + 1 if minute.isdigit() else 0)
    print('Minute %s · %s' % (minute, stand))
    if letzte:
        print('  Letzte Ausgabe: ' + kurz(letzte, 140))
    if prozesse and not fertig:
        for z in sorted(prozesse, key=lambda z: -z[2])[:4]:
            print('  Prozess %d · %s %% CPU · läuft %s · %s' % (z[0], z[2], z[3], kurz(z[4])))


if __name__ == '__main__':
    main()
