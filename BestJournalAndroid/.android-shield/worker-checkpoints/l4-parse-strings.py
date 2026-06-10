# -*- coding: utf-8 -*-
"""L4: Parse DE + ta/te/th strings.xml, extrahiere die 95 Legal-Keys.
   Schreibt l4-legal-pairs.json mit {key: {de, ta, te, th}}.
"""
import json, os, re
import xml.etree.ElementTree as ET

app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')

with open(os.path.join(base, 'legal-keys-2026-06-10.json'), 'r', encoding='utf-8') as f:
    legal = json.load(f)
legal_keys_raw = legal['legalCriticalKeys']

# Normalisiere Keys: manche enthalten " / " (Hinweise auf Gruppen) oder "*"
legal_keys = set()
for k in legal_keys_raw:
    # Bei "a / b" beide Teile aufnehmen; bei "*" -> Prefix
    for part in re.split(r'\s*/\s*', k):
        part = part.strip()
        if not part:
            continue
        legal_keys.add(part)

def parse_strings(path):
    """Gibt dict key->text (mit raw inner XML fuer string-Tags) zurueck."""
    out = {}
    if not os.path.exists(path):
        return out, f"FILE-MISSING:{path}"
    try:
        tree = ET.parse(path)
    except Exception as e:
        return out, f"XML-PARSE-ERROR:{e}"
    root = tree.getroot()
    for el in root:
        if el.tag != 'string':
            continue
        name = el.get('name')
        if name is None:
            continue
        # Voller Textinhalt inkl. evtl. inner markup
        # itertext sammelt allen Text; fuer Korruptionscheck reicht das
        txt = ''.join(el.itertext())
        out[name] = txt
    return out, None

files = {
    'de': os.path.join(app, 'values', 'strings.xml'),
    'ta': os.path.join(app, 'values-ta', 'strings.xml'),
    'te': os.path.join(app, 'values-te', 'strings.xml'),
    'th': os.path.join(app, 'values-th', 'strings.xml'),
}

parsed = {}
errors = {}
for lang, path in files.items():
    d, err = parse_strings(path)
    parsed[lang] = d
    if err:
        errors[lang] = err
    print(f"{lang}: {len(d)} strings  err={err}")

# Welche legal-keys existieren ueberhaupt in DE?
de = parsed['de']
present = sorted(k for k in legal_keys if k in de)
absent = sorted(k for k in legal_keys if k not in de)
print(f"\nLegal-Keys in DE vorhanden: {len(present)} / {len(legal_keys)} (normalisiert)")
if absent:
    print(f"NICHT in DE gefunden ({len(absent)}): {absent}")

# Paare bauen
pairs = {}
for k in present:
    pairs[k] = {
        'de': de.get(k, ''),
        'ta': parsed['ta'].get(k, '__MISSING__'),
        'te': parsed['te'].get(k, '__MISSING__'),
        'th': parsed['th'].get(k, '__MISSING__'),
    }

out_path = os.path.join(base, 'worker-checkpoints', 'l4-legal-pairs.json')
tmp = out_path + '.tmp'
with open(tmp, 'w', encoding='utf-8') as f:
    json.dump({'pairs': pairs, 'errors': errors, 'absentInDe': absent}, f, ensure_ascii=False, indent=1)
os.replace(tmp, out_path)
print(f"\nGeschrieben: {out_path}  ({len(pairs)} Legal-Paare)")
