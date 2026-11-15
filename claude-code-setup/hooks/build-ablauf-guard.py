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
# 5) Komfort-Aliase aus ~/.zshrc im Shell-Schnappschuss (ls->eza, cat->bat): "ls -t <datei>" wurde zu "eza -t <datei>",
#    eza wartete auf die offene Standardeingabe (15.11.2026, 10 Minuten bis zum Timeout). Ursache behoben in ~/.zshrc (Aliase nur
#    im echten Terminal); für Mac-Sitzungen mit altem Schnappschuss bekommen Bash-Befehle mit ls/cat/tree bis 22.11.2026 im
#    Bypass-Modus "unalias …; eval '<befehl>'" (ll bleibt). Danach Block 5) löschen (Rücknahme M35). bugs/werkzeuge/agenten-shell-aliase.md
#
# 6) Gestaffelte Zeitlimits (Franks Regel vom 15.11.2026): höchstens 60 s im Vordergrund. Für Bash setzt das Claude Code selbst
#    (BASH_DEFAULT_TIMEOUT_MS/BASH_MAX_TIMEOUT_MS = 60000 in allen Profil-settings.json); dieser Hook senkt es nur für das
#    PowerShell-Werkzeug (Bypass-Modus) und für die Befehle, die er ohnehin umschreibt (Vorwärmen). Läuft ein Befehl länger, schiebt
#    Claude Code ihn in den Hintergrund (Ausnahme: Befehle, die mit sleep beginnen, werden abgebrochen; die bleiben hier ungedeckelt).
#    Danach jede Minute sichtbar prüfen: Werkzeuge/lauf-waechter/minute.py. Der Hinweis geht per additionalContext an das Modell.
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


SCHLEIFE = re.compile(r'\b(until|while|for)\b')
ALIAS_BEFEHL = re.compile(r'(?:^|[;&|(\n`{]|\$\(|\b(?:do|then|else|xargs|time)\s)\s*(ls|cat|tree)(?=[\s|;&)]|$)')
ALIAS_BIS = (2026, 11, 22)
HINWEIS_WARTEN = (
    "STOPP - diese Warte-Schleife arbeitet falsch (endet nie bzw. sofort).\n\n"
    "pgrep -f \"{muster}\" findet die eigene Shell (das Muster steht woertlich im Befehl), der Prozess gilt also immer als laufend.\n\n"
    "Stattdessen: Den Bau direkt starten (bash gradlew assembleDebug -q). Wird er in den Hintergrund verschoben, meldet sich das\n"
    "Werkzeug beim Ende selbst (Server: mit run_in_background, M26). Danach jede Minute sichtbar pruefen:\n"
    "Werkzeuge/lauf-waechter/minute.py <ausgabedatei> <minute>. Sonst auf das Ergebnis pruefen (APK neuer als der Start), nie auf\n"
    "Prozessnamen, oder den Klammer-Trick nutzen: pgrep -f \"[g]radlew assembleDebug\".\n\n"
    "Notaus bei Fehlalarm: leere Datei build-ablauf-guard-disable.flag im TEMP anlegen."
)


ZEITLIMIT_MS = 60000
_PY = 'python -X utf8 "%USERPROFILE%/proggs' if os.name == 'nt' else 'python3 ~/proggs'
HINWEIS_ZEIT = ("Gestaffeltes Zeitlimit (60 s): Ist der Befehl dann nicht fertig, läuft er weiter. Jede weitere Minute sichtbar prüfen: "
                + _PY + "/Werkzeuge/lauf-waechter/minute.py" + ('"' if os.name == 'nt' else '') + " <ausgabedatei> <minute> "
                "(Beschreibung 'Minute N: Lauf prüfen'), danach ein Satz Einschätzung für Frank. WARTET (Dialog, Overlay, sleep) nie beenden; "
                "bei FEHLER oder HÄNGER sofort eingreifen.")


def mit_zeitlimit(d, neu):
    """Senkt das Vordergrund-Zeitlimit auf 60 s; liefert True, wenn etwas geändert wurde. Befehle, die mit sleep beginnen, bleiben
    unverändert: Claude Code schiebt sie beim Zeitlimit nicht in den Hintergrund, sondern bricht sie ab."""
    if d.get('permission_mode') != 'bypassPermissions' or neu.get('run_in_background'):
        return False
    if re.match(r'\s*(sleep|Start-Sleep)\b', neu.get('command') or '', re.I):
        return False
    alt = neu.get('timeout')
    if isinstance(alt, (int, float)) and alt <= ZEITLIMIT_MS:
        return False
    neu['timeout'] = ZEITLIMIT_MS
    return True


def freigeben(d, neu, grund):
    aus = {"hookEventName": "PreToolUse", "permissionDecision": "allow", "permissionDecisionReason": grund, "updatedInput": neu}
    if mit_zeitlimit(d, neu):
        aus["additionalContext"] = HINWEIS_ZEIT  # permissionDecisionReason sieht bei "allow" nur der Nutzer, nicht das Modell
    print(json.dumps({"hookSpecificOutput": aus}))


def heredoc_filter(cmd, weg):
    """Gemeinsamer Heredoc-Parser: entfernt den Inhalt jedes Heredocs, für dessen ganze Zeile (vor und nach << , also auch
    "cat <<EOF | bash") weg(zeile) wahr ist. Endmarker: bei <<- mit führenden Tabs, sonst genau am Zeilenanfang."""
    teile, rest = [], cmd
    while True:
        m = re.search(r"<<(-?)\s*['\"]?([A-Za-z_]\w*)['\"]?([^\n]*)\n", rest)
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
    for m in PKILL.finditer(cmd):
        if m.group(1) != 'pgrep' or not re.search(r'\s-\w*f|--full', m.group(2) or ''):
            continue
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


def ohne_aliase(cmd):
    """ls/cat/tree/ll als Befehl -> Aliase dieser Shell vorher aufheben (wirkt auch in Sitzungen mit altem Schnappschuss)."""
    import datetime
    if sys.platform != 'darwin' or datetime.date.today() > datetime.date(*ALIAS_BIS) or cmd.lstrip().startswith('unalias ls cat tree') \
            or not ALIAS_BEFEHL.search(nur_shell(cmd)):
        return cmd
    # Aliase werden beim Einlesen ersetzt: Ein unalias im selben Befehl wirkte nicht mehr. Deshalb der eigentliche Befehl erst
    # über ein inneres eval, das nach dem unalias eingelesen wird.
    import shlex
    return 'unalias ls cat tree 2>/dev/null; eval ' + shlex.quote(cmd)


HEREDOC = re.compile(r"<<-?\s*['\"]?[A-Za-z_]+['\"]?")


def verweigern(grund):
    sys.stdout.write(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                 "permissionDecision": "deny", "permissionDecisionReason": grund}}))


def pfad(p, basis):
    p = os.path.expandvars(os.path.expanduser(p.strip()))
    return os.path.normpath(p if os.path.isabs(p) else os.path.join(basis, p))


def selbstmord(cmd):
    cmd = ohne_heredoc(cmd)
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
    r = subprocess.run(['git', '-C', basis, 'log', '-1', '--format=%H', '--', log], capture_output=True, text=True, timeout=5)
    bump = r.stdout.strip()
    if r.returncode != 0 or not bump:
        return False
    r = subprocess.run(['git', '-C', basis, 'reflog', '-n', '200', '--format=%H %gs'], capture_output=True, text=True, timeout=5)
    return any(z.startswith(bump + ' ') and re.match(r'(commit|rebase[^:]*\((pick|continue|reword|edit)\))', z[len(bump) + 1:])
               for z in r.stdout.splitlines())


def ohne_heredoc(cmd):
    """Heredoc-Inhalte entfernen, die cat/tee nur in eine Datei schreiben: Dieser Text wird nicht ausgeführt.
    Heredocs an python/bash/sh bleiben (sie laufen, und ihr Text steht in der Befehlszeile der Shell); "cat <<EOF | bash" auch."""
    return heredoc_filter(cmd, lambda z: bool(re.search(r'\b(cat|tee)\b', z)) and not SHELL_ZEILE.search(z))


def bau_ohne_bump(cmd, cwd):
    cmd = ohne_heredoc(cmd)  # Skripttext, den cat/tee nur in eine Datei schreibt, ist kein Bau
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
                           capture_output=True, text=True, timeout=5)
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


def adb_ziel_fehlt(cmd):
    geraete = None
    for m in ADB_INSTALL.finditer(cmd):
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
            r = subprocess.run([adb, 'devices'], capture_output=True, text=True, timeout=6)
            if r.returncode != 0:
                return None
            geraete = [z.split()[0] for z in r.stdout.splitlines()[1:] if len(z.split()) >= 2 and z.split()[1] == 'device']
        if m.group(1) not in geraete:
            return m.group(1), ', '.join(geraete) or 'kein Gerät'
    return None


GRADLE_BAU = re.compile(r'(^|[\s;&|(])(\./|bash\s+)?gradlew(\.bat)?\s+[^\n;&|]*\b(assemble|bundle|build|install)\w*', re.I)


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
        w = warte_schleife(cmd)
        if w:
            verweigern(HINWEIS_WARTEN.format(muster=w))
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
        # Hostinger-Server (Linux): Gradle-Bauten dauern oft über 10 Minuten; im Vordergrund bricht das Zeitlimit sie ab
        # (Maßnahme M26). Nur im Hintergrund starten.
        if sys.platform.startswith('linux') and d.get('tool_name') == 'Bash' and GRADLE_BAU.search(cmd) \
                and not (d.get('tool_input') or {}).get('run_in_background'):
            verweigern("STOPP – Gradle-Bau auf dem Server im Vordergrund: dauert oft über 10 Minuten, das Zeitlimit bricht ihn ab.\n"
                       "Denselben Befehl ohne timeout mit run_in_background: true starten und auf die Meldung warten.")
            return
    except Exception:
        pass
    try:
        a = adb_ziel_fehlt(cmd)
        if a:
            verweigern(HINWEIS_ADB.format(ziel=a[0], geraete=a[1]))
            return
    except Exception:
        pass
    try:
        # Server: Sitzungsbau wartet auf einen laufenden Vorwärm-Bau (eine gemeinsame Sperre, nie zwei Daemons) und pausiert das
        # Vorwärmen (OpenLauncherServer/kern/vorwaermen.py). Nur ergänzen, nie den Befehl selbst ändern.
        if sys.platform.startswith('linux') and d.get('tool_name') == 'Bash' and GRADLE_BAU.search(cmd) and 'gradle-bau.lock' not in cmd \
                and os.path.isdir(os.path.expanduser('~/.openlauncher/vorwaermen')):
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
        teile = [t.strip() for t in re.split(r'[;\n]|&&|\|\|', cmd) if t.strip()]
        nur_bau = bool(teile) and all(re.match(r'(cd|set-location|push-location|pop-location)\b', t, re.I) or re.match(
            r'(&\s*)?([A-Za-z_]\w*=\S+\s+)*((bash|sh)\s+)?(\.[\\/])?gradlew(\.bat)?\s+[^|&]*\b(assemble|bundle|build|install|compile|test)\w*[^|&]*$', t, re.I)
            for t in teile) and any('gradlew' in t.lower() for t in teile)
        # Mac: derselbe Weg mit python3 (Werkzeugs/vorwaermen/einrichten.sh); flock gibt es am Mac nicht.
        mac = sys.platform == 'darwin'
        if (os.name == 'nt' or mac) and d.get('tool_name') in ('Bash', 'PowerShell') and nur_bau and os.path.isfile(skript):
            neu = dict(d.get('tool_input') or {})
            neu['command'] = ('python3 "' if mac else 'python "') + skript.replace('\\', '/') + '" warte; ' + cmd
            freigeben(d, neu, ("Mac" if mac else "PC") + ": wartet auf laufendes Vorwärmen")
            return
    except Exception:
        pass
    try:
        # "allow" gibt den ganzen Befehl frei: nur dort, wo ohnehin alles frei ist (Bypass); sonst schützt ~/.zshrc (nur echtes Terminal).
        neu = dict(d.get('tool_input') or {})
        grund = []
        if d.get('tool_name') == 'Bash' and d.get('permission_mode') == 'bypassPermissions':
            neu_cmd = ohne_aliase(cmd)
            if neu_cmd != cmd:
                neu['command'] = neu_cmd
                grund.append("Komfort-Aliase (eza, bat) aufgehoben")
        # Bash: Zeitlimit kommt aus BASH_MAX_TIMEOUT_MS (settings.json), kein pauschales "allow" je Befehl. PowerShell: hier deckeln.
        if grund or (d.get('tool_name') == 'PowerShell' and mit_zeitlimit(d, dict(neu))):
            freigeben(d, neu, " · ".join(grund) or "Gestaffeltes Zeitlimit")
    except Exception:
        pass


if __name__ == '__main__':
    main()
