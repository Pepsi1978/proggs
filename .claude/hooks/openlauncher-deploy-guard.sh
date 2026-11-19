#!/usr/bin/env bash
# PreToolUse-Waechter: erzwingt den vorgeschriebenen Deploy-Weg fuer den OpenLauncher.
# bash-Fassung von openlauncher-deploy-guard.ps1 (Cross-Platform-Pflicht, identische Logik).
# Laeuft auf macOS/Linux nativ und auf Windows via Git Bash.
#
# Hintergrund: Vor Einfuehrung von update-launcher.ps1 wurde mehrfach die ALTE EXE gestartet und
# als neue Version gemeldet. Das Skript prueft Build-Exit-Code, prueft dass eine neue Datei
# entstand und erkennt einen sofortigen Absturz der neuen Version.
#
# Notausstieg: enthaelt der Befehl den Marker "guard-aus", laeuft er durch.
#
# Hook-Protokoll: exit 0 immer (Block erfolgt ueber permissionDecision:"deny" im JSON — §1.6:
# exit 2 greift bei Write/Edit nicht zuverlaessig, deny greift fuer alle Tools).
# ROBUSTNESS: Fail-open — unlesbare Eingabe darf die Arbeit nie blockieren.
#
# Seit dem Review (19.11.2026): Die Pruefung laeuft in python3 (wie die .ps1: Schreibweise egal, Zustandsordner,
# Profile und OpenLauncherServer vorher entfernt, Text in Anfuehrungszeichen kein Handgriff, update-launcher nur als
# echter Skriptaufruf). Unter Darwin zusaetzlich die Mac-Handgriffe (swift build/xcodebuild, killall/pkill, open …app),
# Hinweis plattformabhaengig (Mac: OpenLauncherMac/update-launcher.sh, Regel 13).

set +e

raw=$(cat 2>/dev/null)
[ -z "$raw" ] && exit 0

# stdin per python3 parsen (NIE jq — §16.2: Control-Chars brechen jq → stiller Guard-Bypass).
# Strikt spec-konformes JSON (§16.1: non-spec hookSpecificOutput crasht die ganze Session).
# read statt $(cat <<…): Klammern im Python-Text bringen bash 3.2 (macOS) in $(…) aus dem Tritt.
IFS= read -r -d '' code <<'PYEOF'
import json, os, re, sys

try:
    d = json.load(sys.stdin)
    command = (d.get('tool_input') or {}).get('command') or ''
except Exception:
    sys.exit(0)
if not command.strip():
    sys.exit(0)
mac = os.environ.get('SYSTEM') == 'Darwin'

# 1. Der vorgeschriebene Weg selbst (echter Skriptaufruf) und der bewusste Notausstieg laufen immer durch.
SKRIPT_AUFRUF = r'(?:^|[;&|\n(]\s*|-File\s+|&\s*|\b(?:pwsh|powershell|bash|sh|zsh)(?:\s+-\S+)*\s+)["\']?[^\s;&|"\']*update-launcher\.(?:ps1|sh)\b'
if re.search(SKRIPT_AUFRUF, command, re.I) or re.search(r'guard-aus', command, re.I):
    sys.exit(0)

# 2. Nur Befehle, die den Launcher ueberhaupt betreffen (Schreibweise egal); Zustandsordner, Profile und Server vorher entfernen.
bezug = re.sub(r'\.openlauncher[\\/]', '', command, flags=re.I)
bezug = re.sub(r'OpenLauncher[\\/]+Profiles\b', '', bezug, flags=re.I)
bezug = re.sub(r'OpenLauncherServer', '', bezug, flags=re.I)
if not re.search(r'OpenLauncher', bezug, re.I):
    sys.exit(0)


# Text in Anfuehrungszeichen ist kein Handgriff (Commit-Nachrichten, grep-Muster); ausgefuehrter Text (-c "…", -Command "…") schon.
def ohne_text(m):
    if re.search(r'(?:\s-c|\s-Command|\beval)\s*$', command[:m.start()], re.I):
        return m.group(0)
    return '""'


ohne = re.sub(r'"(?:[^"\\]|\\.)*"|\'[^\']*\'', ohne_text, command)

# 3. Die Handgriffe, die das Update-Skript ersetzt. Lesende Befehle stehen bewusst NICHT hier (Versionspruefung nach dem Deploy).
verbote = [
    (r'dotnet\s+(build|publish|msbuild)', 'Bauen'),
    (r'Stop-Process|taskkill|CloseMainWindow|\.Kill\(\)', 'Beenden'),
    (r'Start-Process|Invoke-Item', 'Starten'),
]
if mac:
    # Am Ziel verankert: nur Befehle, die den Mac-Launcher selbst bauen, beenden oder starten (kein nacktes "open").
    ml = r'["\']?[^\s;&|"\']*OpenLauncherMac'
    verbote += [
        (r'(?:\bswift\s+build\b|\bxcodebuild\b)', 'Bauen'),
        (r'\b(?:killall|pkill)(?:\s+-\S+)*\s+' + ml, 'Beenden'),
        (r'\bopen\s+(?:-\S+\s+)*' + ml + r'[^\s;&|"\']*\.app\b|\bopen\s+(?:-\S+\s+)*-a\s+["\']?OpenLauncherMac\b', 'Starten'),
    ]
treffer = []
for muster, handgriff in verbote:
    if handgriff not in treffer and re.search(muster, ohne, re.I):
        treffer.append(handgriff)
# swift build/xcodebuild gelten nur, wenn der Befehl den Mac-Launcher betrifft (Pfad OpenLauncherMac im Befehl).
if mac and 'Bauen' in treffer and not re.search(r'dotnet\s+(build|publish|msbuild)', ohne, re.I) \
        and not re.search(r'OpenLauncherMac', command, re.I):
    treffer.remove('Bauen')
if not treffer:
    sys.exit(0)

weg = ('bash /Users/frank/proggs/OpenLauncherMac/update-launcher.sh' if mac
       else 'pwsh C:\\Users\\barwa\\proggs\\OpenLauncher\\update-launcher.ps1')
grund = f"""Blockiert: {' + '.join(treffer)} des OpenLaunchers von Hand.

Nimm stattdessen den vorgeschriebenen Weg:
    {weg}

Warum: Vor diesem Skript wurde mehrfach die ALTE EXE gestartet und als neue Version gemeldet.
Das Skript prueft den Build-Exit-Code, prueft dass eine neue Datei entstand und erkennt einen
sofortigen Absturz der neuen Version. Es fragt Frank per Dialog und schliesst den Launcher
sauber statt ihn abzuschiessen. Siehe OpenLauncher/SETUP.md, Schritt 5 und 6.

Nach dem Deploy die laufende Version auslesen und die Nummer nennen (Get-Process ist erlaubt)."""

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": grund,
    }
}, ensure_ascii=False))
PYEOF
printf '%s' "$raw" | SYSTEM="$(uname -s 2>/dev/null)" python3 -c "$code" 2>/dev/null

exit 0
