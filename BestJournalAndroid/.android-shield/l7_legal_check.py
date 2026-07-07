# -*- coding: utf-8 -*-
"""95 legal-keys deep check across nl/pl/uk/in. Batched by index range."""
import json, os, re, sys

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'l7-parsed-strings.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)
with open(os.path.join(base, 'legal-keys-2026-06-10.json'), 'r', encoding='utf-8') as f:
    legal = json.load(f)['legalCriticalKeys']

# Clean keys: some entries are "a / b" composite notes -> split & keep real keys
real_keys = []
for k in legal:
    if ' / ' in k:
        for part in k.split(' / '):
            part = part.strip()
            # strip trailing wildcards/notes
            if part.endswith('*'):
                continue
            real_keys.append(part)
    elif k.endswith('*'):
        continue
    else:
        real_keys.append(k)
# de-dup preserve order
seen = set(); keys = []
for k in real_keys:
    if k not in seen:
        seen.add(k); keys.append(k)

de = data['de']
NUM = re.compile(r'\d+')

def numbers(s):
    return NUM.findall(s)

# args
FMT = re.compile(r'%(\d+\$)?[sdfx]')
def args(s):
    return [m.group(0) for m in FMT.finditer(s)]

start = int(sys.argv[1]) if len(sys.argv) > 1 else 0
end = int(sys.argv[2]) if len(sys.argv) > 2 else len(keys)
langs = ['nl', 'pl', 'uk', 'in']

print(f"TOTAL real legal keys: {len(keys)}  (showing {start}..{end})")
for key in keys[start:end]:
    dev = de.get(key)
    if dev is None:
        print(f"\n### {key}  -- !! NOT IN DE (key note/wildcard) skip")
        continue
    de_args = args(dev)
    de_nums = numbers(dev)
    print(f"\n### {key}")
    short = dev if len(dev) < 200 else dev[:200] + " …"
    print(f"  DE[{len(dev)}] args={de_args} nums={de_nums}: {short}")
    for lang in langs:
        lv = data[lang].get(key)
        if lv is None:
            print(f"   {lang}: <<MISSING>>")
            continue
        la = args(lv)
        ln = numbers(lv)
        flag = ""
        if la != de_args:
            flag += " ⚠ARGS"
        if set(ln) != set(de_nums):
            flag += " ⚠NUMS"
        if lv == dev:
            flag += " ⚠IDENTICAL"
        ls = lv if len(lv) < 180 else lv[:180] + " …"
        print(f"   {lang}[{len(lv)}]{flag}: {ls}")
