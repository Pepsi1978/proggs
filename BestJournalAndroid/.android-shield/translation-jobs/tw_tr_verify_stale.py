# -*- coding: utf-8 -*-
import os, re
app = os.path.expanduser('~/proggs/BestJournalAndroid')
tr_file = os.path.join(app, 'app/src/main/res/values-tr/strings.xml')
with open(tr_file, encoding='utf-8') as f:
    content = f.read()

for k in ['consent_card3_title','paywall_feature_profiles','settings_revoke_confirm_title','settings_revoke_subtitle','onboarding_premium_feature_noads','paywall_from_per_day','retro_benefit_weekly']:
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(k), content, re.DOTALL)
    print(f"[{k}] :: {m.group(1) if m else '<<MISSING>>'!r}")

# Show context of "KI" substring matches
print("\n=== 'KI' substring contexts ===")
for m in re.finditer(r'.{8}KI.{8}', content):
    print(repr(m.group(0)))
