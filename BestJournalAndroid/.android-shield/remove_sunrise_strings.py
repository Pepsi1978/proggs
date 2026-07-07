#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Sonnenstand-Cleanup: entfernt die toten Strings settings_sunrise_sunset und
settings_sunrise_sunset_desc aus allen values*/strings.xml (UI-Toggle wurde entfernt,
Strings nirgends mehr referenziert). Reines Zeilen-Entfernen, keine Uebersetzung."""
import glob, os, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

base = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
keys = ['settings_sunrise_sunset', 'settings_sunrise_sunset_desc']
pats = [re.compile(r'[ \t]*<string name="' + re.escape(k) + r'">.*?</string>\s*\n', re.DOTALL) for k in keys]
changed = 0
for path in sorted(glob.glob(os.path.join(base, 'values*', 'strings.xml'))):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new = content
    for p in pats:
        new = p.sub('', new)
    if new != content:
        tmp = path + '.tmp'
        with open(tmp, 'w', encoding='utf-8', newline='\n') as f:
            f.write(new)
        os.replace(tmp, path)
        changed += 1
        print(f'  bereinigt: {os.path.basename(os.path.dirname(path))}')
print(f'\nFertig: {changed} strings.xml bereinigt (settings_sunrise_sunset[_desc] entfernt).')
