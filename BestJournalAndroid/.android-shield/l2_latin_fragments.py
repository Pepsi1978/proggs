# -*- coding: utf-8 -*-
import json, os, re
from collections import Counter
os.environ['PYTHONIOENCODING'] = 'utf-8'
shield = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
rep = json.load(open(os.path.join(shield, 'l2_script_integrity.json'), encoding='utf-8'))

# German-looking words (with umlauts or common German tokens) are red flags inside native text
GERMAN_MARKERS = re.compile(r'[äöüÄÖÜß]')
# Common German function words that, if embedded, indicate untranslated German fragment
GERMAN_WORDS = {'und', 'oder', 'der', 'die', 'das', 'für', 'mit', 'von', 'pro', 'bis',
                'dein', 'deine', 'wird', 'sich', 'auch', 'nicht', 'kein', 'keine',
                'pause', 'Tag', 'Woche', 'Monat', 'Jahr', 'Anfrage', 'Anfragen',
                'verlaengert', 'Abo', 'Kuendigung', 'Widerruf', 'Profil', 'Analyse',
                'Eintraege', 'Eintrag', 'Verbesserung', 'Verbesserungen', 'plus'}

for L in ['bn', 'hi', 'mr']:
    print('=' * 60)
    print('LANG', L)
    frags = rep[L]['latinFragments']
    # Aggregate token frequency
    tokctr = Counter()
    suspicious_entries = []
    for fr in frags:
        toks = fr['tokens']
        tokctr.update(toks)
        val = fr['val']
        # Flag if German umlaut present OR German function word present
        if GERMAN_MARKERS.search(val) or any(t in GERMAN_WORDS for t in toks):
            suspicious_entries.append(fr)
    print('  Top 30 latin tokens:', tokctr.most_common(30))
    print('  GERMAN-LOOKING embedded entries (%d):' % len(suspicious_entries))
    for e in suspicious_entries[:40]:
        print('     ', e['key'], '| tokens=', e['tokens'][:6], '|', e['val'])
