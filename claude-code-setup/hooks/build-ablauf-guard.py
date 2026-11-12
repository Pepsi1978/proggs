#!/usr/bin/env python3
# build-ablauf-guard: PreToolUse-Blocker fuer drei Fehler im Programmierablauf (Bash und PowerShell); 3) siehe ADB_INSTALL.
#
# 1) Android-Bau VOR dem Versions-Bump: gradlew assemble*/install*/bundle* in einer App mit
#    app/src/main/assets/versionslog.json, deren Arbeitskopie Aenderungen hat, der Versionslog aber
#    nicht. Folge: Die APK traegt die alte versionCode, der Bau muss abgebrochen und wiederholt werden.
#    Rebuilds ohne Aenderungen oder nach dem Bump laufen frei durch.
# 2) Selbstmord der Shell: pkill -f / pgrep -f (bzw. Win32_Process-CommandLine-Filter) mit einem
#    Muster, das als Text auch im eigenen Befehl steht. Das Tool laeuft als "bash -c '<befehl>'",
#    also trifft das Muster die ausfuehrende Shell mit (gemessen 08.11.2026: Exit 144, Sitzung 3429c693).
#
# Blockt per permissionDecision=deny + exit 0 (claude-hooks.md 1.6), stdin per Python statt jq.
# Fail-open: jeder Fehler -> kein Block. Notaus: leere Datei build-ablauf-guard-disable.flag im TEMP.
import json, os, re, subprocess, sys

BAU = re.compile(r'(?:^|[\s"\'&;(/\\])((?:[^\s"\';&|]*[/\\])?gradlew(?:\.bat)?)["\']?\s+([^;&|\n]*)', re.IGNORECASE)
BAU_AUFGABE = re.compile(r'(?:^|\s|:)(assemble|install|bundle)\w*', re.IGNORECASE)
CD = re.compile(r'(?:^|[;&|(\s])(?:cd|pushd|Set-Location|Push-Location|sl)\s+(?:-Path\s+|-LiteralPath\s+)?(?:"([^"]+)"|\'([^\']+)\'|([^\s;&|)]+))', re.IGNORECASE)
# Schreibende Befehle (PowerShell, .NET, Shell, Python); Umleitung zählt nur direkt auf den Versionslog, nicht 2>&1.
SCHREIBEN = re.compile(r'WriteAllText|WriteAllLines|AppendAllText|Set-Content|Add-Content|Out-File|\bsed\s+-i|\btee\b|write_text|json\.dump|open\([^)]*,\s*["\']w'
                       r'|(?<![\d&>-])>>?\s*["\']?[^\s;&|"\']*versionslog\.json', re.IGNORECASE)
PROJEKT = re.compile(r'(?:-p|--project-dir)[\s=]+(?:"([^"]+)"|\'([^\']+)\'|(\S+))')
# pkill/pgrep mit -f (auch kombiniert wie -af, -9f) und dem Muster dahinter
PKILL = re.compile(r'\b(pkill|pgrep)\b((?:\s+-[\w-]+(?:\s+\d+)?)*)\s+(?:"([^"]+)"|\'([^\']+)\'|(\S+))')
WIN_CMDLINE = re.compile(r'CommandLine\s+-(?:like|match)\s+["\']([^"\']+)["\']', re.IGNORECASE)

HINWEIS_BUMP = (
    "STOPP - erst die Version bumpen, dann bauen.\n\n"
    "In {app} gibt es Aenderungen, aber {log} ist noch unveraendert. Ein Bau jetzt "
    "bekaeme die alte versionCode und muesste nach dem Bump wiederholt werden.\n\n"
    "Reihenfolge: Code aendern -> Versionslog-Eintrag anhaengen (versionCode +1, Systemzeit per "
    "Befehl) -> bauen -> committen -> pushen -> installieren.\n"
    "Geaenderte Dateien: {dateien}\n\n"
    "Notaus bei Fehlalarm: leere Datei build-ablauf-guard-disable.flag im TEMP anlegen."
)
HINWEIS_KILL = (
    "STOPP - dieses Beenden trifft die eigene Shell.\n\n"
    "Das Muster \"{muster}\" steht woertlich in deinem Befehl. Das Werkzeug laeuft als "
    "'bash -c <befehl>' bzw. eigener PowerShell-Prozess, deshalb findet {werkzeug} auch die "
    "ausfuehrende Shell und beendet sie mit (Exit 144, der Rest des Befehls laeuft nie).\n\n"
    "Stattdessen:\n"
    "  Gradle-Bau beenden:  ./gradlew --stop   (Windows: .\\gradlew.bat --stop)\n"
    "  Sonst Klammer-Trick: pkill -f \"[g]radlew assembleDebug\"  (das Muster passt dann nicht auf sich selbst)\n"
    "  Oder gezielt per PID: kill <pid> aus einem vorherigen, eigenen pgrep-Aufruf.\n"
    "Und Kill nie mit weiteren Schritten in denselben Befehl packen.\n\n"
    "Notaus bei Fehlalarm: leere Datei build-ablauf-guard-disable.flag im TEMP anlegen."
)


HEREDOC = re.compile(r"<<-?\s*['\"]?[A-Za-z_]+['\"]?")


def verweigern(grund):
    sys.stdout.write(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                 "permissionDecision": "deny", "permissionDecisionReason": grund}}))


def pfad(p, basis):
    p = os.path.expandvars(os.path.expanduser(p.strip()))
    return os.path.normpath(p if os.path.isabs(p) else os.path.join(basis, p))


def selbstmord(cmd):
    for m in PKILL.finditer(cmd):
        schalter = m.group(2) or ''
        if not re.search(r'\s-\w*f|--full', schalter):
            continue
        muster = m.group(3) or m.group(4) or m.group(5) or ''
        if m.group(1) == 'pgrep' and not re.search(r'\bkill\b|xargs', cmd):
            continue  # reines Nachsehen beendet nichts
        if '[' in muster:
            continue  # Klammer-Trick: passt nicht auf den eigenen Befehlstext
        return muster, m.group(1)
    for m in WIN_CMDLINE.finditer(cmd):
        if re.search(r'Stop-Process|Terminate|taskkill|\.Kill\(', cmd, re.IGNORECASE) and '[' not in m.group(1):
            return m.group(1), 'der CommandLine-Filter'
    return None


def bau_ohne_bump(cmd, cwd):
    for m in BAU.finditer(cmd):
        if not BAU_AUFGABE.search(m.group(2)):
            continue
        davor = cmd[:m.start()]
        # Derselbe Befehl schreibt den Versionslog vor dem Bau (Bump und Bau in einem Aufruf): kein Bau ohne Bump.
        # Nur echtes Schreiben zählt; bloßes Lesen (cat, Get-Content) vor dem Bau bleibt geblockt.
        if 'versionslog.json' in davor and SCHREIBEN.search(davor):
            continue
        # Das Werkzeug Werkzeuge/versionslog/eintrag.py hängt den Eintrag selbst an (M13).
        if re.search(r'versionslog[\\/]+eintrag\.py', davor, re.IGNORECASE):
            continue
        # git pull vor dem Bau im selben Befehl: Der Pull bringt den Bump einer parallelen Sitzung erst während des Befehls (M23).
        if re.search(r'\bgit\b[^;&|\n]*\bpull\b', davor):
            continue
        basis = cwd
        for c in CD.finditer(davor):
            basis = pfad(c.group(1) or c.group(2) or c.group(3), basis)
        p = PROJEKT.search(m.group(2))
        if p:
            basis = pfad(p.group(1) or p.group(2) or p.group(3), basis)
        elif re.search(r'[/\\]', m.group(1)):
            basis = os.path.dirname(pfad(m.group(1), basis))
        log = os.path.join(basis, 'app', 'src', 'main', 'assets', 'versionslog.json')
        if not os.path.isfile(log):
            continue
        r = subprocess.run(['git', '-C', basis, 'status', '--porcelain', '--', '.'],
                           capture_output=True, text=True, timeout=5)
        if r.returncode != 0:
            continue
        zeilen = [z[3:].strip().strip('"') for z in r.stdout.splitlines() if len(z) > 3]
        if not zeilen:
            continue
        if any(z.replace('\\', '/').endswith('assets/versionslog.json') for z in zeilen):
            continue
        # Bump schon committet und noch nicht gebaut (Versionslog jünger als die letzte APK): Der Bau bekommt die neue
        # versionCode; offene Dateien stammen dann meist von einer parallelen Sitzung (12.11.2026, Sitzung a56e3dee, M20).
        apk = os.path.join(basis, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
        if not os.path.isfile(apk) or os.path.getmtime(log) > os.path.getmtime(apk):
            continue
        return os.path.basename(basis), zeilen
    return None


# 3) adb-Installation auf eine geratene Adresse: "adb -s <ziel> install", obwohl <ziel> gerade nicht in
#    "adb devices" steht (09.11.2026, Sitzung 406cc1b7: Tunnel 10.8.0.7 angenommen, verbunden war 192.168.0.197).
#    Kostet nur bei Installationsbefehlen ein "adb devices" (~30 ms), nie einen Extra-Durchlauf bei richtiger Adresse.
ADB_INSTALL = re.compile(r'\badb(?:\.exe)?["\']?\s+(?:[^;&|\n]*?\s)?-s\s+["\']?([\w.:-]+)["\']?\s+(?:[^;&|\n]*?\s)?install(?:-multiple)?\b', re.IGNORECASE)
HINWEIS_ADB = (
    "STOPP - {ziel} ist gerade nicht verbunden.\n\n"
    "adb devices meldet: {geraete}.\n"
    "Installiere mit genau der Adresse, die dort steht (adb -s <Adresse> install ...). "
    "Steht dort kein WLAN-Gerät: erst Werkzeuge/adb-wlan/adb-wlan.ps1 ausführen (Regel 18).\n\n"
    "Notaus bei Fehlalarm: leere Datei build-ablauf-guard-disable.flag im TEMP anlegen."
)


def adb_pfad():
    import shutil
    kandidaten = [os.environ.get('ADB'), shutil.which('adb')]
    for sdk in (os.environ.get('ANDROID_HOME'), os.environ.get('ANDROID_SDK_ROOT'),
                os.path.join(os.environ.get('LOCALAPPDATA') or '', 'Android', 'Sdk'),
                os.path.expanduser('~/Library/Android/sdk'), os.path.expanduser('~/Android/Sdk')):
        if sdk:
            kandidaten.append(os.path.join(sdk, 'platform-tools', 'adb.exe' if os.name == 'nt' else 'adb'))
    return next((k for k in kandidaten if k and os.path.isfile(k)), None)


def adb_ziel_fehlt(cmd):
    m = ADB_INSTALL.search(cmd)
    if not m or re.search(r'[$%`{]', m.group(1)):
        return None  # keine Installation oder Adresse aus Variable: nicht prüfbar
    # adb devices in demselben Befehl davor: die Ausgabe sieht das Modell erst danach, das schützt also nicht.
    adb = adb_pfad()
    if not adb:
        return None
    r = subprocess.run([adb, 'devices'], capture_output=True, text=True, timeout=6)
    if r.returncode != 0:
        return None
    geraete = [z.split()[0] for z in r.stdout.splitlines()[1:] if len(z.split()) >= 2 and z.split()[1] == 'device']
    if m.group(1) in geraete:
        return None
    return m.group(1), ', '.join(geraete) or 'kein Gerät'


def main():
    try:
        d = json.load(sys.stdin)
    except Exception:
        return
    if (d.get('tool_name') or '') not in ('Bash', 'PowerShell'):
        return
    cmd = (d.get('tool_input') or {}).get('command') or ''
    if not cmd:
        return
    tmp = os.environ.get('TEMP') or os.environ.get('TMPDIR') or '/tmp'
    if os.path.exists(os.path.join(tmp, 'build-ablauf-guard-disable.flag')):
        return
    try:
        k = selbstmord(cmd)
        if k:
            verweigern(HINWEIS_KILL.format(muster=k[0], werkzeug=k[1]))
            return
    except Exception:
        pass
    try:
        b = bau_ohne_bump(cmd, d.get('cwd') or os.getcwd())
        if b:
            app, dateien = b
            liste = ', '.join(dateien[:6]) + (f' (+{len(dateien) - 6})' if len(dateien) > 6 else '')
            verweigern(HINWEIS_BUMP.format(app=app, log='app/src/main/assets/versionslog.json', dateien=liste))
            return
    except Exception:
        pass
    try:
        # Windows-Bash: Heredocs über 30 Zeilen scheitern oft mit "unexpected EOF" (Maßnahme M1, Almanach python-windows §10.1).
        if os.name == 'nt' and d.get('tool_name') == 'Bash' and HEREDOC.search(cmd) and cmd.count('\n') >= 30:
            verweigern("STOPP – langes Bash-Heredoc (>30 Zeilen) scheitert unter Windows oft mit 'unexpected EOF'.\n"
                       "Skript per Write in den Scratchpad legen und dann mit python/pwsh <pfad> ausführen.")
            return
    except Exception:
        pass
    try:
        a = adb_ziel_fehlt(cmd)
        if a:
            verweigern(HINWEIS_ADB.format(ziel=a[0], geraete=a[1]))
    except Exception:
        pass


if __name__ == '__main__':
    main()
