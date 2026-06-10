# -*- coding: utf-8 -*-
"""
Apply pt-rBR translations to values-pt-rBR/strings.xml for finale Phase 3.
- Replaces existing <string> values for the 34 keys
- Inserts paywall_monthly_note (NEW) before </resources> via rfind
- Replaces the 2 <plurals> blocks (one/other)
Target file touched ONLY via Python (FIN-048). Atomic write.
"""
import os, re, json, sys, tempfile, html

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(APP, 'app/src/main/res/values-pt-rBR/strings.xml')
DATA = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/ptbr-data.json')

with open(DATA, 'r', encoding='utf-8') as f:
    data = json.load(f)
strings = data['strings']
plurals = data['plurals']

with open(TARGET, 'r', encoding='utf-8') as f:
    content = f.read()

def xml_escape(text):
    # Escape XML-significant chars. Apostrophes & double quotes use Android backslash escaping.
    # text already contains literal \n (backslash-n) as two chars — keep them.
    out = text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    return out

report = {'replaced': [], 'inserted': [], 'plurals_replaced': [], 'not_found': [], 'errors': []}

# --- Replace / insert string keys ---
for key, val in strings.items():
    esc = xml_escape(val)
    # Match the whole <string name="key" ...>...</string> (DOTALL, non-greedy)
    pat = re.compile(r'(<string\b[^>]*\bname="' + re.escape(key) + r'"[^>]*>)(.*?)(</string>)', re.DOTALL)
    m = pat.search(content)
    if m:
        # Preserve opening/closing tag attributes exactly; replace only inner text
        new_block = m.group(1) + esc + m.group(3)
        content = content[:m.start()] + new_block + content[m.end():]
        report['replaced'].append(key)
    else:
        # NEW key: insert before last </resources>
        idx = content.rfind('</resources>')
        if idx == -1:
            report['errors'].append('no </resources> for ' + key)
            continue
        new_line = '    <string name="' + key + '">' + esc + '</string>\n'
        content = content[:idx] + new_line + content[idx:]
        report['inserted'].append(key)

# --- Replace plurals ---
for key, forms in plurals.items():
    pat = re.compile(r'<plurals\b[^>]*\bname="' + re.escape(key) + r'"[^>]*>.*?</plurals>', re.DOTALL)
    m = pat.search(content)
    items = []
    for q in ('zero', 'one', 'two', 'few', 'many', 'other'):
        if q in forms:
            items.append('        <item quantity="' + q + '">' + xml_escape(forms[q]) + '</item>')
    new_block = '<plurals name="' + key + '">\n' + '\n'.join(items) + '\n    </plurals>'
    if m:
        content = content[:m.start()] + new_block + content[m.end():]
        report['plurals_replaced'].append(key)
    else:
        idx = content.rfind('</resources>')
        content = content[:idx] + '    ' + new_block + '\n' + content[idx:]
        report['plurals_replaced'].append(key + '(inserted)')

# --- Atomic write ---
dir_name = os.path.dirname(TARGET)
with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp', delete=False, encoding='utf-8', newline='\n') as tmp:
    tmp.write(content)
    tmp_path = tmp.name
os.replace(tmp_path, TARGET)

print(json.dumps(report, ensure_ascii=False, indent=1))
