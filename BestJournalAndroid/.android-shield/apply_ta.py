# -*- coding: utf-8 -*-
"""finale Phase 3 — Translation-Worker ta. Loads ta-data.json (no Python-literal
escaping pitfalls), replaces 35 string values + 2 plurals (German-stale) in
values-ta/strings.xml, inserts NEW key paywall_monthly_note via rfind (FIN-040).
Edits ONLY values-ta/strings.xml via Python (FIN-048). Placeholders preserved."""
import os, re, json, tempfile
os.chdir(os.path.expanduser('~/proggs/BestJournalAndroid'))
TARGET = 'app/src/main/res/values-ta/strings.xml'
DATA = '.android-shield/ta-data.json'

data = json.load(open(DATA, encoding='utf-8'))
TA = data['strings']
PLURALS = data['plurals']


def xml_escape_value(s):
    # Android string value escaping. Note: \\n in JSON arrives here as a literal
    # backslash+n (2 chars), which is what we want in the XML (Android \n line break).
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace("'", "\\'")
    s = s.replace('"', '\\"')
    return s


with open(TARGET, 'r', encoding='utf-8') as f:
    content = f.read()

applied = []

# 1) Replace existing <string name="key">...</string> values in place; NEW keys -> rfind insert.
for key, val in TA.items():
    esc = xml_escape_value(val)
    pat = re.compile(r'(<string name="' + re.escape(key) + r'"[^>]*>).*?(</string>)', re.DOTALL)
    if pat.search(content):
        content = pat.sub(lambda m: m.group(1) + esc + m.group(2), content, count=1)
        applied.append(key)
    else:
        new_line = '    <string name="%s">%s</string>' % (key, esc)
        idx = content.rfind('</resources>')
        if idx == -1:
            raise RuntimeError('no </resources>')
        content = content[:idx] + new_line + '\n' + content[idx:]
        applied.append(key + '(NEW)')

# 2) Replace plurals blocks (ta uses one+other).
for key, forms in PLURALS.items():
    items = '\n'.join(
        '        <item quantity="%s">%s</item>' % (q, xml_escape_value(forms[q]))
        for q in ('one', 'other')
    )
    block = '    <plurals name="%s">\n%s\n    </plurals>' % (key, items)
    pat = re.compile(r'<plurals name="' + re.escape(key) + r'">.*?</plurals>', re.DOTALL)
    if pat.search(content):
        content = pat.sub(lambda m: block, content, count=1)
        applied.append('PLURAL:' + key)
    else:
        idx = content.rfind('</resources>')
        content = content[:idx] + block + '\n' + content[idx:]
        applied.append('PLURAL:' + key + '(NEW)')

dir_name = os.path.dirname(TARGET)
with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp', delete=False,
                                 encoding='utf-8', newline='\n') as tmp:
    tmp.write(content)
    tmp_path = tmp.name
os.replace(tmp_path, TARGET)

print('APPLIED', len(applied))
for a in applied:
    print(a)
