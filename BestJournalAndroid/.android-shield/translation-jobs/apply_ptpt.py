# -*- coding: utf-8 -*-
"""Apply pt-rPT translations from ptpt-data.json into values-pt-rPT/strings.xml.
In the JSON, \\n means literal backslash-n (XML newline escape) and \\u201e/\\u201c are
guillemet-style quotes used in the German source. Idempotent: replace existing
<string>/<plurals>, rfind-insert new keys."""
import os, re, json, time

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-pt-rPT/strings.xml')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/ptpt-data.json')
CP_DIR = os.path.join(ROOT, '.android-shield/translation-jobs/worker-checkpoints')
CP = os.path.join(CP_DIR, 'tw-ptpt.jsonl')
os.makedirs(CP_DIR, exist_ok=True)

def ck(stage, **kw):
    rec = {"ts": time.strftime('%Y-%m-%dT%H:%M:%S'), "stage": stage}
    rec.update(kw)
    with open(CP, 'a', encoding='utf-8') as f:
        f.write(json.dumps(rec, ensure_ascii=False) + '\n')

def main():
    with open(DATA, 'r', encoding='utf-8') as f:
        data = json.load(f)
    STRINGS = data['strings']
    PLURALS = data['plurals']
    ck("start", strings=len(STRINGS), plurals=len(PLURALS))

    with open(TARGET, 'r', encoding='utf-8') as f:
        content = f.read()
    orig = content

    applied, inserted, replaced_plurals = [], [], []

    for key, val in STRINGS.items():
        pat = re.compile(r'<string name="' + re.escape(key) + r'"[^>]*>.*?</string>', re.DOTALL)
        new_el = '<string name="' + key + '">' + val + '</string>'
        if pat.search(content):
            content = pat.sub(lambda m: new_el, content, count=1)
            applied.append(key)
        else:
            idx = content.rfind('</resources>')
            if idx == -1:
                raise RuntimeError("kein </resources> gefunden")
            content = content[:idx] + '    ' + new_el + '\n' + content[idx:]
            inserted.append(key)

    for key, forms in PLURALS.items():
        items = ''.join('\n        <item quantity="%s">%s</item>' % (q, t) for q, t in forms.items())
        block = '<plurals name="%s">%s\n    </plurals>' % (key, items)
        pat = re.compile(r'<plurals name="' + re.escape(key) + r'">.*?</plurals>', re.DOTALL)
        if pat.search(content):
            content = pat.sub(lambda m: block, content, count=1)
            replaced_plurals.append(key)
        else:
            raise RuntimeError("plurals %s nicht gefunden" % key)

    if content != orig:
        with open(TARGET, 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)

    ck("written", applied=len(applied), inserted=inserted, plurals=replaced_plurals)
    print("APPLIED(replace):", len(applied))
    print("INSERTED(new):", inserted)
    print("PLURALS:", replaced_plurals)

if __name__ == '__main__':
    main()
