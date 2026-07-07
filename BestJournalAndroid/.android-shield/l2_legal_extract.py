# -*- coding: utf-8 -*-
import json, os, re, xml.etree.ElementTree as ET
os.environ['PYTHONIOENCODING'] = 'utf-8'
base = os.path.expanduser('~/proggs/BestJournalAndroid')
shield = os.path.join(base, '.android-shield')
RES = os.path.join(base, 'app/src/main/res')

LEGAL = json.load(open(os.path.join(shield, 'legal-keys-2026-06-10.json'), encoding='utf-8'))['legalCriticalKeys']

# Normalise legal keys: split "a / b" variants, drop wildcard markers
def normalise(keys):
    out = []
    for k in keys:
        if '/' in k:
            parts = [p.strip() for p in k.split('/')]
            for p in parts:
                out.append(p)
        else:
            out.append(k.strip())
    # Expand: drop entries with '*' into prefix markers (handled separately)
    return out

NORM = normalise(LEGAL)

def load_strings(path):
    """Return dict key -> raw inner text (string elements only)."""
    if not os.path.exists(path):
        return {}
    try:
        tree = ET.parse(path)
    except Exception as e:
        return {'__PARSE_ERROR__': str(e)}
    root = tree.getroot()
    out = {}
    for el in root:
        if el.tag == 'string':
            name = el.get('name')
            # Reconstruct inner text incl. xliff tags
            txt = ET.tostring(el, encoding='unicode')
            # strip outer <string ...> ... </string>
            inner = re.sub(r'^<string[^>]*>', '', txt)
            inner = re.sub(r'</string>\s*$', '', inner)
            out[name] = inner.strip()
    return out

de = load_strings(os.path.join(RES, 'values/strings.xml'))
langs = {L: load_strings(os.path.join(RES, 'values-%s/strings.xml' % L)) for L in ['bn', 'hi', 'mr']}

# Which legal keys are concrete (exist in DE), which are wildcards
concrete = [k for k in NORM if '*' not in k]
wildcards = [k for k in NORM if '*' in k]

# Resolve wildcard prefixes against DE keys
wild_resolved = {}
for w in wildcards:
    prefix = w.replace('*', '')
    matches = sorted([k for k in de if k.startswith(prefix)])
    wild_resolved[w] = matches

print('Concrete legal keys:', len(concrete))
print('Wildcard markers:', wildcards)
for w, ms in wild_resolved.items():
    print('  ', w, '->', len(ms), 'matches:', ms[:8])

# Keys present in DE
present = [k for k in concrete if k in de]
absent = [k for k in concrete if k not in de]
print('Present in DE:', len(present), '| Absent from DE:', absent)

# Dump full data to a JSON for batch processing
out = {'present': present, 'absent_from_de': absent, 'wildcards': wild_resolved,
       'de': {k: de.get(k) for k in present},
       'bn': {k: langs['bn'].get(k) for k in present},
       'hi': {k: langs['hi'].get(k) for k in present},
       'mr': {k: langs['mr'].get(k) for k in present}}
with open(os.path.join(shield, 'l2_legal_pairs.json'), 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=1)
print('WROTE l2_legal_pairs.json with', len(present), 'present keys')
