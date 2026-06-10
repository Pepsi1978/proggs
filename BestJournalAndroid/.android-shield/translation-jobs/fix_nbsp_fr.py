# -*- coding: utf-8 -*-
"""Ersetzt die literalen \\u00A0 / \\u202F Escape-Strings in fr-data.json durch
echte NBSP / NNBSP Zeichen (explizite Codepoints), damit Android sie korrekt
rendert (nicht als Text). Danach muss apply_fr.py erneut laufen.
Idempotent: zaehlt nur die noch vorhandenen Escape-Strings."""
import json, os

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/fr-data.json')

with open(DATA, 'r', encoding='utf-8') as f:
    raw = f.read()

NBSP = chr(0x00A0)    # U+00A0 regular non-breaking space
NNBSP = chr(0x202F)   # U+202F narrow non-breaking space

before_nbsp = raw.count('\\u00A0')
before_nnbsp = raw.count('\\u202F')

raw = raw.replace('\\u00A0', NBSP)
raw = raw.replace('\\u202F', NNBSP)

with open(DATA, 'w', encoding='utf-8', newline='\n') as f:
    f.write(raw)

with open(DATA, 'r', encoding='utf-8') as f:
    obj = json.load(f)

# confirm real codepoints now present
sample = obj['strings']['settings_revoke_confirm_title']
print('replaced_nbsp=%d replaced_nnbsp=%d' % (before_nbsp, before_nnbsp))
print('has_U202F_in_title=%s' % (chr(0x202F) in sample))
print('json_valid=YES')
