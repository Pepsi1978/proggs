# -*- coding: utf-8 -*-
import re
f = 'app/src/main/res/values-th/strings.xml'
s = open(f, encoding='utf-8').read()
for key in ['settings_revoke_confirm_body', 'ai_prompt_custom_intro',
            'consent_toggle_do_not_sell_body', 'settings_delete_account_confirm_body',
            'ai_prompt_custom_rules']:
    m = re.search(r'<string name="%s"(?:\s+[^>]*?)?>(.*?)</string>' % re.escape(key), s, re.DOTALL)
    if m:
        v = m.group(1)
        print('=== ' + key + ' ===')
        print(repr(v[:260]))
        print('contains &quot;:', '&quot;' in v,
              '| contains backslash+doublequote:', ('\\' + '"') in v,
              '| contains raw doublequote:', '"' in v,
              '| contains backslash-n:', '\\n' in v)
        print()
