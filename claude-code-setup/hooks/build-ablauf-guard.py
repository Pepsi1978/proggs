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
# 4) Warte-Schleife, die nie endet: until/while mit pgrep -f "<muster>", das im eigenen Befehl steht. pgrep findet die
#    eigene Shell, der Bau gilt ewig als laufend (gemessen 15.11.2026: 20 Minuten Warten auf einen fertigen Jarvis-Bau).
# 6) Gestaffelte Zeitlimits (Franks Regel vom 15.11.2026): höchstens 60 s im Vordergrund. Für Bash setzt das Claude Code selbst
#    (BASH_DEFAULT_TIMEOUT_MS/BASH_MAX_TIMEOUT_MS = 60000 in allen Profil-settings.json); dieser Hook senkt es nur für das
#    PowerShell-Werkzeug (Bypass-Modus) und für die Befehle, die er ohnehin umschreibt (Vorwärmen). Läuft ein Befehl länger, schiebt
#    Claude Code ihn in den Hintergrund (Ausnahme: Befehle, die mit sleep beginnen, werden abgebrochen; die bleiben hier ungedeckelt).
#    Danach jede Minute sichtbar prüfen: Werkzeuge/lauf-waechter/minute.py. Der Hinweis geht per additionalContext an das Modell.
#
# Blockt per permissionDecision=deny + exit 0 (claude-hooks.md 1.6), stdin per Python statt jq.
# Fail-open: jeder Fehler -> kein Block. Notaus: leere Datei ~/.claude/build-ablauf-guard-disable.flag (wirkt 2 Stunden ab Anlegen).
import json, os, re, subprocess, sys, time

_START = time.monotonic()
FRIST_S = 8  # alle Unterprozesse zusammen; der Hook hat laut settings.json 10 s


def frist(n):
    """Zeitlimit für den nächsten Unterprozess: höchstens n, zusammen höchstens FRIST_S. Ist kaum Zeit übrig: Abbruch (fail-open)."""
    rest = FRIST_S - (time.monotonic() - _START)
    if rest < 0.5:
        raise TimeoutError('Hook-Frist aufgebraucht')
    return min(n, rest)


NOTAUS = os.path.join(os.path.expanduser('~'), '.claude', 'build-ablauf-guard-disable.flag')
NOTAUS_ZEILE = ("Notaus bei Fehlalarm: leere Datei " + NOTAUS.replace('\\', '/') + " anlegen (wirkt 2 Stunden)."
                ).replace('{', '{{').replace('}', '}}')


def notaus_aktiv():
    """Flag gilt nur 2 Stunden ab Anlegen/Ändern, damit ein vergessenes Flag den Hook nicht dauerhaft abschaltet.
    Der alte Ort im TEMP zählt weiter, mit derselben Frist."""
    tmp = os.environ.get('TEMP') or os.environ.get('TMPDIR') or '/tmp'
    for p in (NOTAUS, os.path.join(tmp, 'build-ablauf-guard-disable.flag')):
        try:
            if time.time() - os.path.getmtime(p) < 7200:
                return True
        except OSError:
            pass
    return False


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
    + NOTAUS_ZEILE
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
    + NOTAUS_ZEILE
)


SCHLEIFE = re.compile(r'\b(until|while|for)\b')
HINWEIS_WARTEN = (
    "STOPP - diese Warte-Schleife arbeitet falsch (endet nie bzw. sofort).\n\n"
    "pgrep -f \"{muster}\" findet die eigene Shell (das Muster steht woertlich im Befehl), der Prozess gilt also immer als laufend.\n\n"
    "Stattdessen: Den Bau direkt starten (bash gradlew assembleDebug -q). Wird er in den Hintergrund verschoben, meldet sich das\n"
    "Werkzeug beim Ende selbst (Server: mit run_in_background, M26). Danach jede Minute sichtbar pruefen:\n"
    "Werkzeuge/lauf-waechter/minute.py <ausgabedatei> <minute>. Sonst auf das Ergebnis pruefen (APK neuer als der Start), nie auf\n"
    "Prozessnamen, oder den Klammer-Trick nutzen: pgrep -f \"[g]radlew assembleDebug\".\n\n"
    + NOTAUS_ZEILE
)


ZEITLIMIT_MS = 60000
_MINUTE = os.path.join(os.path.expanduser('~'), 'proggs', 'Werkzeuge', 'lauf-waechter', 'minute.py').replace('\\', '/')
_PY = ('python -X utf8 "' if os.name == 'nt' else 'python3 "') + _MINUTE + '"'
HINWEIS_ZEIT = ("Gestaffeltes Zeitlimit (60 s): Ist der Befehl dann nicht fertig, läuft er weiter. Jede weitere Minute sichtbar prüfen: "
                + _PY + " <ausgabedatei> <minute> "
                "(Beschreibung 'Minute N: Lauf prüfen'), danach ein Satz Einschätzung für Frank. WARTET (Dialog, Overlay, sleep) nie beenden; "
                "bei FEHLER oder HÄNGER sofort eingreifen.")


def mit_zeitlimit(d, neu):
    """Senkt das Vordergrund-Zeitlimit auf 60 s. Liefert None (nichts geändert), 'gesetzt' (kein eigenes timeout, Wert nur
    ergänzt) oder 'gesenkt' (ausdrücklich höheres timeout herabgesetzt). Befehle, die mit sleep beginnen, bleiben
    unverändert: Claude Code schiebt sie beim Zeitlimit nicht in den Hintergrund, sondern bricht sie ab."""
    if d.get('permission_mode') != 'bypassPermissions' or neu.get('run_in_background'):
        return None
    if re.match(r'\s*(sleep|Start-Sleep)\b', neu.get('command') or '', re.I):
        return None
    alt = neu.get('timeout')
    if isinstance(alt, (int, float)) and alt <= ZEITLIMIT_MS:
        return None
    neu['timeout'] = ZEITLIMIT_MS
    return 'gesenkt' if isinstance(alt, (int, float)) else 'gesetzt'


def freigeben(d, neu, grund):
    aus = {"hookEventName": "PreToolUse", "permissionDecision": "allow", "permissionDecisionReason": grund, "updatedInput": neu}
    # Hinweis nur, wenn ein ausdrücklich höheres timeout gesenkt wurde (Kontextkosten; Regel 24 steht ohnehin im Profil).
    if mit_zeitlimit(d, neu) == 'gesenkt':
        aus["additionalContext"] = HINWEIS_ZEIT  # permissionDecisionReason sieht bei "allow" nur der Nutzer, nicht das Modell
    print(json.dumps({"hookSpecificOutput": aus}))


# Heredoc-Anfang; '<<<' (Here-String) zählt nicht.
HEREDOC_START = re.compile(r"(?<!<)<<(?!<)(-?)\s*['\"]?([A-Za-z_]\w*)['\"]?([^\n]*)\n")


def heredoc_koerper(cmd):
    """(start, ende, zeilen) jedes Heredoc-Inhalts: von der Zeile nach dem Marker bis einschließlich Endmarker (bzw. Befehlsende)."""
    spannen, pos = [], 0
    while True:
        m = HEREDOC_START.search(cmd, pos)
        if not m:
            return spannen
        ende = re.compile(r'(?m)^' + (r'\t*' if m.group(1) else '') + re.escape(m.group(2)) + r'[ \t]*$').search(cmd, m.end())
        stop = ende.end() if ende else len(cmd)
        inhalt = cmd[m.end():ende.start() if ende else len(cmd)]
        spannen.append((m.end(), stop, inhalt.count('\n') + (0 if inhalt.endswith('\n') or not inhalt else 1)))
        pos = stop


AUSGEFUEHRT = re.compile(r'(?:\s-c|\s-Command|\beval|\bssh\s+[^\s;&|]+(?:\s+-\S+)*)\s*$', re.I)
BEFEHLSSTELLE = re.compile(r'(?:^|[;&|\n({])\s*(?:&\s*|(?:bash|sh)\s+)?$')


def maske(cmd, powershell=False):
    """Text in Anführungszeichen längenerhaltend durch '_' ersetzen: nur zum Finden von Stellen; Muster und Pfade immer aus dem
    Original lesen (gleiche Offsets). Bleibt sichtbar: ausgeführter Text (bash -c "…", pwsh -Command "…", eval, ssh host '…'),
    ein Programmpfad in Anführungszeichen an Befehlsstelle ("./gradlew" …) und Heredoc-Inhalte (Apostrophe dort kippen sonst die Paarung)."""
    text = re.compile(r'"[^"]*"|\'[^\']*\'' if powershell else r'"(?:[^"\\]|\\.)*"|\'[^\']*\'', re.S)

    def ersatz(m, versatz):
        s, davor = m.group(0), cmd[:versatz + m.start()]
        if AUSGEFUEHRT.search(davor):
            return s
        if re.search(r'gradlew(?:\.bat)?$', s[1:-1], re.I) and BEFEHLSSTELLE.search(davor):
            return s
        return s[0] + re.sub(r'[^\n]', '_', s[1:-1]) + s[-1]

    teile, pos = [], 0
    for start, stop, _ in heredoc_koerper(cmd) + [(len(cmd), len(cmd), 0)]:
        stueck = cmd[pos:start]
        teile.append(text.sub(lambda m, v=pos: ersatz(m, v), stueck))
        teile.append(cmd[start:stop])
        pos = stop
    return ''.join(teile)


def in_text(maskiert, i):
    """True, wenn die Stelle i im Original innerhalb ausgeblendeten Texts liegt."""
    return 0 <= i < len(maskiert) and maskiert[i] == '_'


def heredoc_filter(cmd, weg):
    """Gemeinsamer Heredoc-Parser: entfernt den Inhalt jedes Heredocs, für dessen ganze Zeile (vor und nach << , also auch
    "cat <<EOF | bash") weg(zeile) wahr ist. Endmarker: bei <<- mit führenden Tabs, sonst genau am Zeilenanfang."""
    teile, rest = [], cmd
    while True:
        m = HEREDOC_START.search(rest)
        if not m:
            return ''.join(teile) + rest
        ende = re.search(r'(?m)^' + (r'\t*' if m.group(1) else '') + re.escape(m.group(2)) + r'[ \t]*$', rest[m.end():])
        zeile = rest[rest.rfind('\n', 0, m.start()) + 1:m.start()] + ' ' + m.group(3)
        teile.append(rest[:m.end()])
        if not ende:
            return ''.join(teile) + rest[m.end():]
        if not weg(zeile):
            teile.append(rest[m.end():m.end() + ende.end()])
        rest = rest[m.end() + ende.end():]


SHELL_ZEILE = re.compile(r'(?:^|[\s|;&(])(?:bash|sh|zsh)(?:\s|$)')


def nur_shell(cmd):
    """Nur Heredocs behalten, die an eine Shell gehen; python-, node-, cat-Heredocs sind Daten (Fehlalarm vom 15.11.2026)."""
    return heredoc_filter(cmd, lambda z: not SHELL_ZEILE.search(z))


def ohne_kommentar_und_text(cmd):
    """Text in Anführungszeichen ausblenden (Commit-Nachrichten, echo), damit dort kein while/for als Schleife zählt."""
    return re.sub(r'"(?:[^"\\]|\\.)*"|\'[^\']*\'', '""', cmd)


def warte_schleife(cmd):
    """pgrep -f in einer until/while-Schleife (auch mehrzeilig), dessen Muster im eigenen Befehl steht; gleicher Parser wie selbstmord()."""
    cmd = nur_shell(cmd)
    maskiert = maske(cmd)
    for m in PKILL.finditer(cmd):
        if m.group(1) != 'pgrep' or not re.search(r'\s-\w*f|--full', m.group(2) or ''):
            continue
        if in_text(maskiert, m.start(1)):
            continue  # pgrep nur als Text (Commit-Nachricht, echo, grep-Muster)
        muster = m.group(3) or m.group(4) or m.group(5) or ''
        # Klammer-Trick, Anker (^…, …$) und Variablen passen nicht auf den eigenen Befehlstext.
        if not muster or '[' in muster or muster.startswith('^') or muster.endswith('$') or muster.startswith('$'):
            continue
        davor = ohne_kommentar_und_text(cmd[:m.start()])
        # Nur eine noch offene Schleife zählt: alles bis zum letzten "done" davor ist abgeschlossen.
        letzte = [x.end() for x in re.finditer(r'\bdone\b', davor)]
        if SCHLEIFE.search(davor[letzte[-1] if letzte else 0:]):
            return muster
    return None


def verweigern(grund):
    sys.stdout.write(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                 "permissionDecision": "deny", "permissionDecisionReason": grund}}))


def pfad(p, basis):
    # PowerShell $env:NAME -> ${NAME}, $HOME -> ~ (unter Windows meist keine Umgebungsvariable), dann wie gewohnt auflösen.
    p = re.sub(r'\$env:(\w+)', r'${\1}', p.strip(), flags=re.IGNORECASE)
    p = re.sub(r'^\$(?:HOME\b|\{HOME\})', '~', p)
    p = os.path.expandvars(os.path.expanduser(p))
    if os.name == 'nt':
        p = re.sub(r'^/([A-Za-z])(?:/|$)', lambda m: m.group(1).upper() + ':/', p)  # Git Bash: /c/Users/… -> C:/Users/…
    return os.path.normpath(p if os.path.isabs(p) else os.path.join(basis, p))


def selbstmord(cmd, powershell=False):
    cmd = ohne_heredoc(cmd)
    # Nur Aufrufe außerhalb von Text zählen (git commit -m "… pkill -f gradle …", grep "pkill -f x" beenden nichts).
    maskiert = maske(cmd, powershell)
    for m in PKILL.finditer(cmd):
        schalter = m.group(2) or ''
        if not re.search(r'\s-\w*f|--full', schalter) or in_text(maskiert, m.start(1)):
            continue
        muster = m.group(3) or m.group(4) or m.group(5) or ''
        if m.group(1) == 'pgrep' and not re.search(r'\bkill\b|xargs', maskiert):
            continue  # reines Nachsehen beendet nichts
        # Klammer-Trick, Anker (^…, …$) und Variablen passen nicht auf den eigenen Befehlstext ("bash -c …").
        if not muster or '[' in muster or muster.startswith('^') or muster.endswith('$') or muster.startswith('$'):
            continue
        return muster, m.group(1)
    for m in WIN_CMDLINE.finditer(cmd):
        if in_text(maskiert, m.start()):
            continue
        if re.search(r'Stop-Process|Terminate|taskkill|\.Kill\(', maskiert, re.IGNORECASE) and '[' not in m.group(1):
            return m.group(1), 'der CommandLine-Filter'
    return None


GIT_PULL = re.compile(r'(?:^|[;&|\n({]\s*)git(?:\s+-[Cc]\s+(?:"[^"]*"|\'[^\']*\'|\S+))*\s+pull\b')


def eigener_bump_ungebaut(basis, log):
    import glob
    zeiten = []
    for a in glob.glob(os.path.join(basis, 'app', 'build', 'outputs', 'apk', '**', '*.apk'), recursive=True):
        try:
            zeiten.append(os.path.getmtime(a))
        except OSError:
            pass  # parallel gelöscht (clean): zählt wie fehlend
    if not zeiten or os.path.getmtime(log) <= max(zeiten):
        return False
    # Eigener Commit, auch schon gepusht (Sitzung a56e3dee: commit, pull --rebase, push, dann Bau): Er steht im Reflog als
    # "commit:" bzw. nach dem Rebase als "rebase (pick):". Ein per pull/merge geholter Bump steht dort nur als Fast-Forward.
    r = subprocess.run(['git', '-C', basis, 'log', '-1', '--format=%H', '--', log], capture_output=True, text=True,
                       stdin=subprocess.DEVNULL, timeout=frist(3))
    bump = r.stdout.strip()
    if r.returncode != 0 or not bump:
        return False
    r = subprocess.run(['git', '-C', basis, 'reflog', '-n', '200', '--format=%H %gs'], capture_output=True, text=True,
                       stdin=subprocess.DEVNULL, timeout=frist(3))
    return any(z.startswith(bump + ' ') and re.match(r'(commit|rebase[^:]*\((pick|continue|reword|edit)\))', z[len(bump) + 1:])
               for z in r.stdout.splitlines())


def ohne_heredoc(cmd):
    """Heredoc-Inhalte entfernen, die cat/tee nur in eine Datei schreiben: Dieser Text wird nicht ausgeführt.
    Heredocs an python/bash/sh bleiben (sie laufen, und ihr Text steht in der Befehlszeile der Shell); "cat <<EOF | bash" auch."""
    return heredoc_filter(cmd, lambda z: bool(re.search(r'\b(cat|tee)\b', z)) and not SHELL_ZEILE.search(z))


def bau_ohne_bump(cmd, cwd, powershell=False):
    cmd = ohne_heredoc(cmd)  # Skripttext, den cat/tee nur in eine Datei schreibt, ist kein Bau
    # Gesucht wird im maskierten Befehl (grep "gradlew assembleDebug", Commit-Nachrichten sind kein Bau); Pfade, -p und
    # alles davor kommen aus dem Original (gleiche Offsets).
    for m in BAU.finditer(maske(cmd, powershell)):
        if not BAU_AUFGABE.search(m.group(2)):
            continue
        m = BAU.match(cmd, m.start()) or m
        davor = cmd[:m.start()]
        # Derselbe Befehl schreibt den Versionslog vor dem Bau (Bump und Bau in einem Aufruf): kein Bau ohne Bump.
        # Nur echtes Schreiben zählt; bloßes Lesen (cat, Get-Content) vor dem Bau bleibt geblockt.
        if 'versionslog.json' in davor and SCHREIBEN.search(davor):
            continue
        # Das Werkzeug Werkzeuge/versionslog/eintrag.py hängt den Eintrag selbst an (M13).
        if re.search(r'versionslog[\\/]+eintrag\.py', davor, re.IGNORECASE):
            continue
        # git pull vor dem Bau im selben Befehl: Der Pull bringt den Bump einer parallelen Sitzung erst während des Befehls (M23).
        # Nur ein echter git-pull-Aufruf am Befehlsanfang zählt, nicht "pull" in einer Commit-Nachricht.
        if GIT_PULL.search(davor):
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
                           capture_output=True, text=True, stdin=subprocess.DEVNULL, timeout=frist(4))
        if r.returncode != 0:
            continue
        zeilen = [z[3:].strip().strip('"') for z in r.stdout.splitlines() if len(z) > 3]
        if not zeilen:
            continue
        if any(z.replace('\\', '/').endswith('assets/versionslog.json') for z in zeilen):
            continue
        # Bump in diesem Repo committet und noch nicht gebaut (Versionslog jünger als die neueste APK): Der Bau bekommt die
        # neue versionCode; offene Dateien stammen dann meist von einer parallelen Sitzung (12.11.2026, Sitzung a56e3dee, M20).
        # Ein per git pull/merge geholter fremder Bump zählt nicht, und fehlt jede APK (frischer Worktree, nach clean), gilt
        # die Grundregel: erst bumpen, dann bauen. Grenze: Den Commit einer parallelen Sitzung im selben Repo erkennt der
        # Reflog nicht als fremd.
        if eigener_bump_ungebaut(basis, log):
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
    "Steht dort kein WLAN-Gerät: erst Werkzeuge/adb-wlan/" + ('adb-wlan.ps1' if os.name == 'nt' else 'adb-wlan.sh')
    + " ausführen (Regel 18).\n\n"
    + NOTAUS_ZEILE
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


# Das WLAN-Werkzeug wird wirklich ausgeführt (nicht nur geraete.json daneben gelesen).
ADB_WLAN_LAUF = re.compile(r'(?:-File\s+|\bbash\s+|&\s*|^|[;&|\n(]\s*)["\']?[^\s;&|"\']*adb-wlan\.(?:ps1|sh)\b', re.IGNORECASE)


def in_ssh_argument(davor):
    """True, wenn die Stelle am Ende von [davor] zum Befehl eines vorausgehenden ssh gehört: zwischen dem letzten ssh
    und der Stelle kein ; & | oder Zeilenumbruch außerhalb von Anführungszeichen (ssh host 'a; adb …' zählt, ssh x; adb … nicht)."""
    treffer = list(re.finditer(r'(?:^|[;&|\s(])ssh\s', davor))
    if not treffer:
        return False
    quote = None
    for ch in davor[treffer[-1].end():]:
        if quote:
            if ch == quote:
                quote = None
        elif ch in ('"', "'"):
            quote = ch
        elif ch in ';&|\n':
            return False
    return True


def adb_ziel_fehlt(cmd, powershell=False):
    geraete = None
    # Wie bei Bau und Kill: Heredoc-Daten (cat > skript.sh <<EOF) und Text in Anführungszeichen (Commit-Nachricht, echo) sind
    # keine Installation (Fehlalarm am Mac, 19.11.2026: Testdatei per Heredoc geblockt). ssh 'adb …' bleibt sichtbar (s. u.).
    cmd = ohne_heredoc(cmd)
    maskiert = maske(cmd, powershell)
    for m in ADB_INSTALL.finditer(cmd):
        if in_text(maskiert, m.start()):
            continue
        if re.search(r'[$%`{]', m.group(1)):
            continue  # Adresse aus Variable: nicht prüfbar
        # adb devices in demselben Befehl davor: die Ausgabe sieht das Modell erst danach, das schützt also nicht.
        # adb connect auf genau diese Adresse oder ein Lauf des WLAN-Werkzeugs davor verbinden erst im Befehl: nicht prüfbar.
        davor = cmd[:m.start()]
        # Läuft auf einem anderen Rechner (ssh, z. B. Hostinger-Server): das lokale "adb devices" sagt darüber nichts.
        if in_ssh_argument(davor):
            continue
        if ADB_WLAN_LAUF.search(davor) or re.search(r'\bconnect\s+["\']?' + re.escape(m.group(1)) + r'(?![\w.:])', davor):
            continue
        if geraete is None:
            adb = adb_pfad()
            if not adb:
                return None
            # Ausgabe in eine Datei statt eine Pipe: Startet "adb devices" den adb-Server neu, erbt der die Handles und hielte
            # eine Pipe bis zum Zeitlimit offen. Eingabe ins Leere.
            import tempfile
            with tempfile.TemporaryFile() as f:
                r = subprocess.run([adb, 'devices'], stdin=subprocess.DEVNULL, stdout=f, stderr=subprocess.DEVNULL, timeout=frist(5))
                f.seek(0)
                ausgabe = f.read().decode('utf-8', 'replace')
            if r.returncode != 0:
                return None
            geraete = [z.split()[0] for z in ausgabe.splitlines() if len(z.split()) >= 2 and z.split()[1] == 'device']
        if m.group(1) not in geraete:
            return m.group(1), ', '.join(geraete) or 'kein Gerät'
    return None


GRADLE_BAU = re.compile(r'(^|[\s;&|("\'])(\./|bash\s+)?gradlew(\.bat)?["\']?\s+[^\n;&|]*\b(assemble|bundle|build|install)\w*', re.I)
# Harmlose Umleitungen in einem Bau-Teilstück (2>&1, > datei, 2>/dev/null) gelten auf dem Server (nur Bypass) noch als reiner Bau.
UMLEITUNG = re.compile(r'\s\d*>&\d+|\s\d*>>?\s*[^\s|&;<>]+')


def nur_bau(cmd, umleitung=False):
    """Jedes Teilstück ist cd/Set-Location/Push-Location/Pop-Location oder ein gradlew-Bau, mindestens ein gradlew: Dann darf
    "allow" den ganzen Befehl freigeben, weil nichts anderes darin steht."""
    teile = [t.strip() for t in re.split(r'[;\n]|&&|\|\|', cmd) if t.strip()]
    return bool(teile) and all(re.match(r'(cd|set-location|push-location|pop-location)\b', t, re.I) or re.match(
        r'(&\s*)?([A-Za-z_]\w*=\S+\s+)*((bash|sh)\s+)?(\.[\\/])?gradlew(\.bat)?\s+[^|&]*\b(assemble|bundle|build|install|compile|test)\w*[^|&]*$',
        UMLEITUNG.sub(' ', t) if umleitung else t, re.I)
        for t in teile) and any('gradlew' in t.lower() for t in teile)


def langes_heredoc(cmd, grenze=30):
    """True, wenn ein einzelner Heredoc-Inhalt mehr als [grenze] Zeilen hat (nicht der ganze Befehl, kein '<<<')."""
    return any(zeilen > grenze for _, _, zeilen in heredoc_koerper(cmd))


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
    if notaus_aktiv():
        return
    ps = d.get('tool_name') == 'PowerShell'
    bypass = d.get('permission_mode') == 'bypassPermissions'
    try:
        k = selbstmord(cmd, ps)
        if k:
            verweigern(HINWEIS_KILL.format(muster=k[0], werkzeug=k[1]))
            return
    except Exception:
        pass
    try:
        w = warte_schleife(cmd)
        if w:
            verweigern(HINWEIS_WARTEN.format(muster=w))
            return
    except Exception:
        pass
    try:
        b = bau_ohne_bump(cmd, d.get('cwd') or os.getcwd(), ps)
        if b:
            app, dateien = b
            liste = ', '.join(dateien[:6]) + (f' (+{len(dateien) - 6})' if len(dateien) > 6 else '')
            verweigern(HINWEIS_BUMP.format(app=app, log='app/src/main/assets/versionslog.json', dateien=liste))
            return
    except Exception:
        pass
    try:
        # Windows-Bash: Heredocs über 30 Zeilen scheitern oft mit "unexpected EOF" (Maßnahme M1, Almanach python-windows §10.1).
        if os.name == 'nt' and d.get('tool_name') == 'Bash' and langes_heredoc(cmd):
            verweigern("STOPP – langes Bash-Heredoc (>30 Zeilen) scheitert unter Windows oft mit 'unexpected EOF'.\n"
                       "Skript per Write in den Scratchpad legen und dann mit python/pwsh <pfad> ausführen.")
            return
    except Exception:
        pass
    try:
        # Hostinger-Server (Linux): Gradle-Bauten dauern oft über 10 Minuten; im Vordergrund bricht das Zeitlimit sie ab
        # (Maßnahme M26). Nur im Hintergrund starten.
        if sys.platform.startswith('linux') and d.get('tool_name') == 'Bash' and GRADLE_BAU.search(maske(cmd)) \
                and not (d.get('tool_input') or {}).get('run_in_background'):
            verweigern("STOPP – Gradle-Bau auf dem Server im Vordergrund: dauert oft über 10 Minuten, das Zeitlimit bricht ihn ab.\n"
                       "Denselben Befehl ohne timeout mit run_in_background: true starten und auf die Meldung warten.")
            return
    except Exception:
        pass
    try:
        a = adb_ziel_fehlt(cmd, ps)
        if a:
            verweigern(HINWEIS_ADB.format(ziel=a[0], geraete=a[1]))
            return
    except Exception:
        pass
    try:
        # Server: Sitzungsbau wartet auf einen laufenden Vorwärm-Bau (eine gemeinsame Sperre, nie zwei Daemons) und pausiert das
        # Vorwärmen (OpenLauncherServer/kern/vorwaermen.py). Nur ergänzen, nie den Befehl selbst ändern.
        # "allow" gibt den ganzen Befehl frei: nur bei reinem Bau und nur im Bypass (dort ist ohnehin alles frei). Sonst bleibt der
        # Befehl unverändert (kein "ask": das hielte unbeaufsichtigte Sitzungen an).
        if sys.platform.startswith('linux') and d.get('tool_name') == 'Bash' and bypass and nur_bau(cmd, True) \
                and 'gradle-bau.lock' not in cmd and os.path.isdir(os.path.expanduser('~/.openlauncher/vorwaermen')):
            neu = dict(d.get('tool_input') or {})
            # Sperre über einen offenen Deskriptor halten, bis die Shell endet (flock … true gäbe sie vor dem Bau frei).
            neu['command'] = ('touch ~/.openlauncher/vorwaermen/pause; exec 9>>~/.openlauncher/vorwaermen/gradle-bau.lock; flock -w 3600 9; ' + cmd)
            freigeben(d, neu, "Server: wartet auf laufendes Vorwärmen")
            return
    except Exception:
        pass
    try:
        # Windows-PC: dasselbe über Werkzeuge/vorwaermen/vorwaermen.py warte (pausiert das Vorwärmen, wartet kurz, bis ein
        # Vorbau ausgewichen ist). Nur bei reinen Bau-Befehlen (jedes Teilstück cd/Set-Location/Push-Location oder gradlew),
        # weil "allow" den ganzen Befehl freigibt. Gemischte Befehle bleiben unverändert; der Vorbau erkennt den Client trotzdem.
        skript = os.path.join(os.path.expanduser('~'), 'proggs', 'Werkzeuge', 'vorwaermen', 'vorwaermen.py')
        # Mac: derselbe Weg mit python3 (Werkzeugs/vorwaermen/einrichten.sh); flock gibt es am Mac nicht.
        mac = sys.platform == 'darwin'
        if (os.name == 'nt' or mac) and d.get('tool_name') in ('Bash', 'PowerShell') and nur_bau(cmd) and os.path.isfile(skript):
            neu = dict(d.get('tool_input') or {})
            neu['command'] = ('python3 "' if mac else 'python "') + skript.replace('\\', '/') + '" warte; ' + cmd
            freigeben(d, neu, ("Mac" if mac else "PC") + ": wartet auf laufendes Vorwärmen")
            return
    except Exception:
        pass
    try:
        # Bash: Zeitlimit kommt aus BASH_MAX_TIMEOUT_MS (settings.json), kein pauschales "allow" je Befehl. PowerShell: hier deckeln.
        # (Die befristete Alias-Umschreibung für Mac, M35, ist seit 19.11.2026 entfernt: ~/.zshrc setzt die Aliase nur im echten
        # Terminal, kein Mac-Schnappschuss enthält sie noch.)
        neu = dict(d.get('tool_input') or {})
        if ps and mit_zeitlimit(d, dict(neu)):
            freigeben(d, neu, "Gestaffeltes Zeitlimit")
        elif ps and not bypass and not neu.get('run_in_background') and isinstance(neu.get('timeout'), (int, float)) \
                and neu['timeout'] > ZEITLIMIT_MS and not re.match(r'\s*(sleep|Start-Sleep)\b', cmd, re.I):
            # Außerhalb des Bypass wirkt updatedInput nur mit "allow" (gäbe den Befehl ungeprüft frei) oder "ask" (Rückfrage auch
            # bei erlaubten Befehlen, hält unbeaufsichtigte Sitzungen an). Deshalb nur der Hinweis, das timeout bleibt.
            print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "additionalContext":
                              "Zeitlimit nicht gesenkt (kein Bypass-Modus): Nächste Befehle ohne timeout über 60000 starten. " + HINWEIS_ZEIT}}))
    except Exception:
        pass


if __name__ == '__main__':
    main()
