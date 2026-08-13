#!/usr/bin/env python3
# emulator-start-guard: blockiert den NACKTEN Start des Android-Emulators
# (emulator[.exe] -avd <name>) im Bash- und PowerShell-Tool und verweist auf die
# Werkzeugkette Werkzeuge/fold8-emulator/Start-Fold8.ps1.
#
# WARUM: Der Android-Emulator kennt weder Startgroesse noch Startposition
# (-scale abgeschafft, -window-size nur Fuchsia, emulator-user.ini wird beim
# Beenden ueberschrieben). Nackt gestartet landet das Fenster irgendwo - gemessen
# am 13.08.2026: x=1019, y=-648, also 648 px oberhalb des Bildschirms und damit
# nicht mehr greifbar. Siehe bugs/android/emulator-foldable.md #18 und #18b.
# Die Kette setzt Originalgroesse (1:1 wie das echte Fold 8), zentriert das
# Fenster und haelt es per Waechter beim Drehen/Klappen nach.
#
# WICHTIG (claude-hooks.md 1.6): NICHT exit 2 zum Blocken -> permissionDecision=deny
# + exit 0. Bei exit 2 wuerde der JSON-Output ignoriert (1.3).
# stdin NIE mit jq parsen (claude-hooks.md 16.2) - daher Python.
# Fail-open: jeder Fehler -> kein Block (nie die Arbeit blockieren wegen Hook-Bug).
# Notaus: leere Datei emulator-start-guard-disable.flag im TEMP -> nie blockieren.
import json, sys, os, re

# "emulator" bzw. "emulator.exe" als eigenes Wort - auch am Ende eines Pfades
# oder in Anfuehrungszeichen (C:\...\emulator\emulator.exe, /opt/.../emulator).
# $ & ( = muessen mit in die Zeichenklasse: in PowerShell steht der Pfad meist in
# einer Variablen ("& $emulator -avd Fold8"), und genau dieser Fall rutschte beim
# ersten Test durch den Guard.
VOR = r'(?:^|[\\/\s"\'$&(=])'
EMU = re.compile(VOR + r'emulator(?:\.exe)?["\']?(?:\s|$)', re.IGNORECASE)
# Alte Kurzform: emulator @Fold8
AT_FORM = re.compile(VOR + r'emulator(?:\.exe)?["\']?\s+@\w', re.IGNORECASE)

HINWEIS = (
    "STOPP - der Emulator darf nicht nackt gestartet werden.\n\n"
    "Der Android-Emulator kennt weder Startgroesse noch Startposition. Nackt "
    "gestartet landet das Fenster ausserhalb des Bildschirms (gemessen: 648 px "
    "oberhalb des oberen Rands - die Titelleiste ist dann nicht mehr greifbar) "
    "und die App wird nicht in Geraetegroesse dargestellt. "
    "Siehe bugs/android/emulator-foldable.md #18 und #18b.\n\n"
    "Nimm stattdessen die Werkzeugkette:\n"
    "  pwsh -File ~/proggs/Werkzeuge/fold8-emulator/Start-Fold8.ps1 -Projekt <Projektname>\n\n"
    "Sie baut die App, installiert sie, startet sie, stellt das Fenster auf "
    "Originalgroesse (1:1 wie das echte Fold 8), zentriert es und haelt es per "
    "Waechter beim Drehen und Klappen nach.\n"
    "  -Innen      grosses Innendisplay (Standard ist zugeklappt/Cover)\n"
    "  -OhneBauen  vorhandene APK nehmen\n"
    "  -Zoom 1.5   groesser als das echte Geraet\n\n"
    "Notaus bei Fehlalarm: leere Datei emulator-start-guard-disable.flag im TEMP anlegen."
)


def main():
    try:
        d = json.load(sys.stdin)
    except Exception:
        return
    tool = d.get('tool_name', '') or ''
    if tool not in ('Bash', 'PowerShell'):
        return
    ti = d.get('tool_input') or {}
    cmd = ti.get('command') or ''
    if not cmd:
        return

    # Der richtige Weg laeuft selbst ueber emulator.exe - niemals blockieren.
    if 'start-fold8' in cmd.lower():
        return

    startet_emulator = (EMU.search(cmd) and re.search(r'-avd\b', cmd, re.IGNORECASE)) \
        or AT_FORM.search(cmd)
    if not startet_emulator:
        return

    tmp = os.environ.get('TEMP') or os.environ.get('TMPDIR') or '/tmp'
    if os.path.exists(os.path.join(tmp, 'emulator-start-guard-disable.flag')):
        return

    out = {"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                  "permissionDecision": "deny",
                                  "permissionDecisionReason": HINWEIS}}
    sys.stdout.write(json.dumps(out))


if __name__ == '__main__':
    try:
        main()
    except Exception:
        pass
    sys.exit(0)
