# -*- coding: utf-8 -*-
import os, re
target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-ur/strings.xml')
with open(target, 'r', encoding='utf-8') as f:
    content = f.read()

# how is \n stored? check a multiline string
m = re.search(r'<string name="consent_toggle_do_not_sell_body">(.*?)</string>', content, re.DOTALL)
if m:
    v = m.group(1)
    print("do_not_sell raw repr (first 200):", repr(v[:200]))
    print("contains literal backslash-n:", '\\n' in v)
    print("contains real newline:", '\n' in v)

# placeholder style
for k in ["paywall_from_per_day","ai_prompt_custom_intro","ai_prompt_goals_rules"]:
    m = re.search(rf'<string name="{k}">(.*?)</string>', content, re.DOTALL)
    if m:
        v = m.group(1)
        ph = re.findall(r'%\d\$[sd]|%[sd]', v)
        print(f"[{k}] placeholders found: {ph}")

# plural format
for p in ["datetime_months_relative","datetime_years_relative"]:
    m = re.search(rf'<plurals name="{p}">(.*?)</plurals>', content, re.DOTALL)
    if m:
        print(f"--- {p} ---")
        print(m.group(1).strip())

# header / encoding line
print("=== HEAD ===")
print(content[:200])
# count total resources
print("total <string ", content.count('<string '))
