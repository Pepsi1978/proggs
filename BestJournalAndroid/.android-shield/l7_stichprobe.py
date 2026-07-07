# -*- coding: utf-8 -*-
"""15 random strings per lang + script-purity checks.
pl: presence of ł/ż/ź/ą/ę. uk: Cyrillic-only, detect Russian-only letters (ё, ъ, ы, э).
in: latin, no German umlauts leaking."""
import json, os, re, random

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'l7-parsed-strings.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)

de = data['de']
random.seed(77)

# Russian-specific letters NOT used in Ukrainian: ы, э, ъ, ё
RU_ONLY = set('ыэъёЫЭЪЁ')
# Ukrainian-specific letters: і, ї, є, ґ
UA_SPECIFIC = set('іїєґІЇЄҐ')
PL_SPECIAL = set('łżźąęćńóśŁŻŹĄĘĆŃÓŚ')
CYRILLIC = re.compile(r'[Ѐ-ӿ]')
LATIN = re.compile(r'[A-Za-z]')

def sample_keys(lang, n=15):
    keys = [k for k in data[lang].keys() if k in de and len(data[lang][k]) > 12]
    return random.sample(keys, min(n, len(keys)))

for lang in ['nl', 'pl', 'uk', 'in']:
    print(f"\n{'='*70}\n{lang.upper()} — 15 random strings\n{'='*70}")
    for k in sample_keys(lang):
        v = data[lang][k]
        sv = v if len(v) < 110 else v[:110] + '…'
        print(f"  {k}: {sv}")

# Script-purity over the WHOLE file
print(f"\n\n{'#'*70}\nSCRIPT-PURITY (whole file)\n{'#'*70}")

# pl: how many strings contain pl special chars
pl_with_special = sum(1 for v in data['pl'].values() if PL_SPECIAL & set(v))
print(f"pl: strings containing ł/ż/ź/ą/ę/ć/ń/ó/ś = {pl_with_special} / {len(data['pl'])}")

# uk: detect any Russian-only letters
uk_ru_hits = []
for k, v in data['uk'].items():
    found = RU_ONLY & set(v)
    if found:
        uk_ru_hits.append((k, ''.join(sorted(found)), v[:80]))
print(f"uk: strings with Russian-only letters (ы/э/ъ/ё) = {len(uk_ru_hits)}")
for k, ch, snip in uk_ru_hits[:25]:
    print(f"    {k} [{ch}]: {snip}")
# uk: confirm Ukrainian-specific letters present somewhere
uk_ua_count = sum(1 for v in data['uk'].values() if UA_SPECIFIC & set(v))
print(f"uk: strings with Ukrainian-specific letters (і/ї/є/ґ) = {uk_ua_count} / {len(data['uk'])}")

# in: detect German umlauts leaking (ä ö ü ß) in non-prompt strings
UMLAUT = set('äöüßÄÖÜ')
in_umlaut = [(k, v[:80]) for k, v in data['in'].items() if UMLAUT & set(v)]
print(f"in: strings with German umlauts (ä/ö/ü/ß) = {len(in_umlaut)}")
for k, snip in in_umlaut[:15]:
    print(f"    {k}: {snip}")
