#!/usr/bin/env python3
import glob, os, re

keys = set([
 "settings_premium_feature_5_perspectives_desc","onboarding_subtitle","profile_insight_long",
 "onboarding_premium_feature_patterns","paywall_feature_patterns","paywall_subscribe_button",
 "legend_insight_deep","profile_insight_desc","profile_entropy_long","dashboard_weekly_review_upsell_body",
 "retro_monthly_p3","onboarding_how_step4_desc","onboarding_free_trial","retro_yearly_p2","paywall_free_note",
 "paywall_most_popular","settings_delete_account_force_local","settings_premium_cancelled_desc",
 "onboarding_try_premium","paywall_yearly_note","churn_exclusive_offer","privacy_gate_gemini_body"])

bs = chr(92)            # backslash
apos = chr(39)          # '
line_re = re.compile(r'(<string name="([^"]+)">)(.*)(</string>)')
# unescapter Apostroph = ' der nicht direkt von einem backslash vorangestellt ist
unescaped = re.compile(r'(?<!' + re.escape(bs) + r')' + re.escape(apos))

fixed = {}
for f in sorted(glob.glob('app/src/main/res/values-*/strings.xml')):
    lang = os.path.basename(os.path.dirname(f))
    with open(f, encoding='utf-8') as fh:
        lines = fh.readlines()
    changed = False
    for i, line in enumerate(lines):
        m = line_re.search(line)
        if not m:
            continue
        key, val = m.group(2), m.group(3)
        if key not in keys:
            continue
        quoted = val.startswith('"') and val.endswith('"')
        if quoted:
            continue
        new_val = unescaped.sub(bs + apos, val)
        if new_val != val:
            lines[i] = m.group(1) + new_val + m.group(4) + "\n"
            fixed.setdefault(lang, []).append(key)
            changed = True
    if changed:
        tmp = f + '.tmp'
        with open(tmp, 'w', encoding='utf-8', newline='\n') as fh:
            fh.writelines(lines)
        os.replace(tmp, f)

if not fixed:
    print("Keine unescapten Apostrophe in den 22 Keys gefunden.")
else:
    for lang, ks in fixed.items():
        print(f"{lang}: {len(ks)} Apostroph-Fixes -> {', '.join(ks)}")
