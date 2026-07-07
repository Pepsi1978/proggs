# -*- coding: utf-8 -*-
import re, os
APP = os.path.expanduser('~/proggs/BestJournalAndroid')
F = os.path.join(APP, 'app/src/main/res/values-ja/strings.xml')
with open(F, 'r', encoding='utf-8') as fh:
    c = fh.read()
for k in ['settings_delete_account_subtitle', 'paywall_monthly_note',
          'settings_revoke_subtitle', 'consent_toggle_do_not_sell_body',
          'datetime_months_relative']:
    m = re.search(r'<string name="' + k + r'">(.*?)</string>', c, re.S) or \
        re.search(r'<plurals name="' + k + r'">(.*?)</plurals>', c, re.S)
    body = m.group(1) if m else 'NOTFOUND'
    has_real_nl = '\n' in body if k != 'datetime_months_relative' else False
    has_literal = '\\n' in body
    print('###', k, '| real_newline_in_body:', has_real_nl, '| literal_backslash_n:', has_literal)
    print('   ', body[:120].replace('\n', '<<NL>>'))
# do_not_sell must NOT claim selling; revoke_subtitle must mention 356a
print('--- 356a present:', '356a' in c)
print('--- google kept (subtitle):', 'Googleアカウント自体は残ります' in c)
print('--- year/2035 not invented in do_not_sell:', not re.search(r'20[0-9][0-9]年', c.split('consent_toggle_do_not_sell_body')[1].split('</string>')[0]) if 'consent_toggle_do_not_sell_body' in c else 'n/a')
