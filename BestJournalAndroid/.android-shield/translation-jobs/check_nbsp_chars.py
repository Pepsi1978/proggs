# -*- coding: utf-8 -*-
"""Prueft ob die NBSP/NNBSP-Zeichen in fr-data.json wirklich U+00A0 / U+202F sind."""
import json, os

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/fr-data.json')

d = json.load(open(DATA, encoding='utf-8'))  # raises if invalid JSON
print('json_valid=YES')

checks = {
    'settings_revoke_confirm_title': [0x202F],   # contrat<NNBSP>?
    'settings_revoke_subtitle': [0x00A0],        # §<NBSP>356a
    'settings_revoke_confirm_body': [0x00A0],    # «<NBSP>...<NBSP>» and §<NBSP>16
}
for key, expected_cps in checks.items():
    val = d['strings'][key]
    cps_present = set(ord(c) for c in val if ord(c) in (0x00A0, 0x202F))
    print('%s: codepoints_present=%s (hex=%s)' % (
        key, sorted(cps_present), [hex(c) for c in sorted(cps_present)]))

# count guillemets and ensure NBSP adjacency in revoke_confirm_body
body = d['strings']['settings_revoke_confirm_body']
import re
# « should be followed by U+00A0 ; U+00A0 before »
ok_open = ('« ' in body)
ok_close = (' »' in body)
print('guillemet_open_nbsp=%s guillemet_close_nbsp=%s' % (ok_open, ok_close))
title = d['strings']['settings_revoke_confirm_title']
print('title_has_nnbsp_before_q=%s' % (' ?' in title))
