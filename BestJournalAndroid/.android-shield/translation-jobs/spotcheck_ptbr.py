# -*- coding: utf-8 -*-
import os, re
import xml.etree.ElementTree as ET
TARGET = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-pt-rBR/strings.xml')
tree = ET.parse(TARGET); root = tree.getroot()
d = {s.get('name'): (s.text or '') for s in root.findall('string')}

# PT-PT contamination check (should be absent in BR)
pt_pt_terms = ['utilizador', 'aplicação', 'guardar', 'definições', 'palavra-passe', 'telemóvel', 'transferir', 'ecrã']
all_text = ' '.join(d.values()).lower()
contam = [t for t in pt_pt_terms if t in all_text]
print('PT-PT contamination terms found:', contam if contam else 'NONE (good)')

# legal spot-checks
checks = {
  'settings_delete_account_subtitle': ['conta do Google em si permanece'],
  'settings_delete_account_confirm_body': ['conta do Google em si permanece', 'não a sua conta do Google'],
  'settings_revoke_subtitle': ['356a', 'BGB'],
  'consent_toggle_do_not_sell_body': ['Califórnia'],
}
for k, needles in checks.items():
    v = d.get(k, '')
    for n in needles:
        print(k, '| contains', repr(n), '->', n in v)

# do_not_sell must NOT contain an invented year like 2026
print('do_not_sell contains "2026"?:', '2026' in d.get('consent_toggle_do_not_sell_body',''))

# você-form present
print('você present in delete_subtitle context?:', 'permanece' in d.get('settings_delete_account_subtitle',''))
