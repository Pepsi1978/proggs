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

set +e

raw=$(cat 2>/dev/null)
[ -z "$raw" ] && exit 0

# stdin per python3 parsen (NIE jq — §16.2: Control-Chars brechen jq → stiller Guard-Bypass).
command_str=$(printf '%s' "$raw" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('command', ''))
except Exception:
    pass
" 2>/dev/null)

[ -z "$command_str" ] && exit 0

# 1. Der vorgeschriebene Weg selbst und der bewusste Notausstieg laufen immer durch.
printf '%s' "$command_str" | grep -qiE 'update-launcher|guard-aus' && exit 0

# 2. Nur Befehle, die den Launcher ueberhaupt betreffen.
printf '%s' "$command_str" | grep -qi 'OpenLauncher' || exit 0

# 3. Die drei Handgriffe, die update-launcher.ps1 ersetzt. Lesende Cmdlets stehen bewusst NICHT
#    hier: Get-Process/Get-Item auf die EXE bleiben erlaubt (Versionspruefung nach dem Deploy).
treffer=""
printf '%s' "$command_str" | grep -qiE 'dotnet[[:space:]]+(build|publish|msbuild)' && treffer="Bauen"
printf '%s' "$command_str" | grep -qiE 'Stop-Process|taskkill|CloseMainWindow|\.Kill\(\)' && treffer="${treffer:+$treffer + }Beenden"
printf '%s' "$command_str" | grep -qiE 'Start-Process|Invoke-Item' && treffer="${treffer:+$treffer + }Starten"

[ -z "$treffer" ] && exit 0

# Strikt spec-konformes JSON (§16.1: non-spec hookSpecificOutput crasht die ganze Session).
TREFFER="$treffer" python3 <<'PYEOF'
import json, os

grund = f"""Blockiert: {os.environ['TREFFER']} des OpenLaunchers von Hand.

Nimm stattdessen den vorgeschriebenen Weg:
    pwsh ~/proggs/OpenLauncher/update-launcher.ps1

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

exit 0
