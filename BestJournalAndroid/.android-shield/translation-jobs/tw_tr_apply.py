# -*- coding: utf-8 -*-
import os, re, json, subprocess

app = os.path.expanduser('~/proggs/BestJournalAndroid')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
tr_file = os.path.join(app, 'app/src/main/res/values-tr/strings.xml')

data = json.load(open(os.path.join(base, 'tr-data.json'), encoding='utf-8'))
jp = json.load(open(os.path.join(base, 'job-plan.json'), encoding='utf-8'))
tr = jp['languages']['tr']
strings = tr['strings']
plurals = tr['plurals']
legend_keys = tr['special'][0].split(': ', 1)[1].split(',')

with open(tr_file, 'r', encoding='utf-8') as f:
    content = f.read()

applied = []
new_keys = []
skipped = []

# ---- 1. Replace existing strings (use lambda to avoid backslash interpretation) ----
for key in strings:
    if key == 'paywall_monthly_note':
        continue  # handled as NEW insert
    val = data[key]
    pat = re.compile(r'(<string name="%s">)(.*?)(</string>)' % re.escape(key), re.DOTALL)
    if not pat.search(content):
        print(f"WARN: {key} not found for replace")
        skipped.append(key)
        continue
    content = pat.sub(lambda m: m.group(1) + val + m.group(3), content, count=1)
    applied.append(key)

# ---- 2. NEW insert: paywall_monthly_note (pre-check via grep + rfind) ----
nk = 'paywall_monthly_note'
existing = subprocess.run(['grep', '-c', f'name="{nk}"', tr_file],
                          capture_output=True, text=True).stdout.strip()
if int(existing or '0') > 0:
    skipped.append(nk + '(already-present)')
else:
    new_string = '    <string name="%s">%s</string>\n' % (nk, data[nk])
    idx = content.rfind('</resources>')
    if idx == -1:
        raise RuntimeError("no </resources> in tr file")
    content = content[:idx] + new_string + content[idx:]
    new_keys.append(nk)
    applied.append(nk)

# ---- 3. Replace plurals (Turkish: no noun change after numbers; both quantities same noun) ----
pl_repl = {
    'datetime_months_relative': '        <item quantity="one">%1$d ay önce</item>\n        <item quantity="other">%1$d ay önce</item>',
    'datetime_years_relative':  '        <item quantity="one">%1$d yıl önce</item>\n        <item quantity="other">%1$d yıl önce</item>',
}
plurals_done = 0
for key in plurals:
    rep = pl_repl[key]
    pat = re.compile(r'(<plurals name="%s">)(.*?)(</plurals>)' % re.escape(key), re.DOTALL)
    if not pat.search(content):
        print(f"WARN: plural {key} not found")
        continue
    content = pat.sub(lambda m: m.group(1) + '\n' + rep + '\n    ' + m.group(3), content, count=1)
    plurals_done += 1

# ---- 4. SPECIAL TASK: legend %% -> % (text otherwise unchanged) ----
legend_fixed = 0
for key in legend_keys:
    pat = re.compile(r'(<string name="%s">)(.*?)(</string>)' % re.escape(key), re.DOTALL)
    m = pat.search(content)
    if not m:
        print(f"WARN: legend {key} not found")
        continue
    inner = m.group(2)
    if '%%' in inner:
        new_inner = inner.replace('%%', '%')
        content = content[:m.start()] + m.group(1) + new_inner + m.group(3) + content[m.end():]
        legend_fixed += 1

with open(tr_file, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

print("APPLIED:", len(applied))
print("NEW_KEYS:", new_keys)
print("PLURALS_DONE:", plurals_done)
print("LEGEND_FIXED:", legend_fixed)
print("SKIPPED:", skipped)
