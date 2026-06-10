#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Sonnenstand-Cleanup in der Datenschutzerklaerung: entfernt Abschnitt 3.4
(Ungefaehrer Standort / ACCESS_COARSE_LOCATION, war fuer das entfernte Sonnenstand-
Feature) und renummeriert 3.5->3.4, 3.6->3.5. Schreibt das identische Ergebnis in
App-Asset UND Online-docs (garantierter Sync). Sprachunabhaengig ueber h3-Marker."""
import os, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

ROOT = os.path.expanduser('~/proggs')
files = {
    'de': ('BestJournalAndroid/app/src/main/assets/legal/de/DATENSCHUTZ.html', 'docs/bestjournal/privacy-de.html'),
    'en': ('BestJournalAndroid/app/src/main/assets/legal/en/PRIVACY.html',     'docs/bestjournal/privacy-en.html'),
    'ko': ('BestJournalAndroid/app/src/main/assets/legal/ko/PRIVACY.html',     'docs/bestjournal/privacy-ko.html'),
}
# Block von <h3>3.4 ...</h3>... bis unmittelbar vor <h3>3.5 ...
block_re = re.compile(r'<h3>3\.4 .*?(?=<h3>3\.5 )', re.DOTALL)

for lang, (apprel, onlinerel) in files.items():
    appf = os.path.join(ROOT, apprel)
    content = open(appf, encoding='utf-8').read()
    if '<h3>3.4 ' not in content or '<h3>3.5 ' not in content:
        print(f'  {lang}: WARN — 3.4/3.5-Marker nicht gefunden, uebersprungen')
        continue
    new = block_re.sub('', content)
    new = new.replace('<h3>3.5 ', '<h3>3.4 ').replace('<h3>3.6 ', '<h3>3.5 ')
    had_loc = 'ACCESS_COARSE_LOCATION' in content
    still_loc = 'ACCESS_COARSE_LOCATION' in new
    for target in (appf, os.path.join(ROOT, onlinerel)):
        tmp = target + '.tmp'
        with open(tmp, 'w', encoding='utf-8', newline='\n') as f:
            f.write(new)
        os.replace(tmp, target)
    print(f'  {lang}: 3.4 entfernt (COARSE vorher={had_loc}, nachher={still_loc}), App+Online geschrieben')

print('\nFertig. Sonnenstand-/Standort-Abschnitt aus DSE entfernt (App + Online).')
