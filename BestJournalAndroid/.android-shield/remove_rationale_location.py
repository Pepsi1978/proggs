#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""E1-Cleanup: Entfernt den toten String permission_rationale_location aus allen
values*/strings.xml. Der String wird nirgends im Code referenziert (verifiziert per
grep) — er war die Begruendung fuer die jetzt entfernte ACCESS_COARSE_LOCATION-Permission.
Reines Zeilen-Entfernen, KEINE Uebersetzung."""
import glob, os, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

base = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
pattern = re.compile(r'[ \t]*<string name="permission_rationale_location">.*?</string>\s*\n', re.DOTALL)
changed = 0
for path in sorted(glob.glob(os.path.join(base, 'values*', 'strings.xml'))):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new = pattern.sub('', content)
    if new != content:
        # atomares Schreiben
        tmp = path + '.tmp'
        with open(tmp, 'w', encoding='utf-8', newline='\n') as f:
            f.write(new)
        os.replace(tmp, path)
        changed += 1
        print(f'  bereinigt: {os.path.basename(os.path.dirname(path))}')
print(f'\nFertig: {changed} strings.xml bereinigt (permission_rationale_location entfernt).')
