# -*- coding: utf-8 -*-
"""Post-verification for pt-rPT (FIN-051c + FIN-049 + PT-PT quality)."""
import os, re, json, xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-pt-rPT/strings.xml')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/ptpt-data.json')
DEREF = os.path.join(ROOT, '.android-shield/translation-jobs/de-reference.json')

data = json.load(open(DATA, encoding='utf-8'))
deref = json.load(open(DEREF, encoding='utf-8'))
STRINGS = data['strings']; PLURALS = data['plurals']
de_s = deref['strings']; de_p = deref['plurals']

errors, warns = [], []

# 1) XML valid
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
    print("XML valid: OK")
except Exception as e:
    errors.append("XML parse FAILED: %s" % e)
    root = None

# Build maps from parsed XML
xml_strings, xml_plurals = {}, {}
if root is not None:
    for el in root.findall('string'):
        # full inner text incl. nested
        xml_strings[el.get('name')] = ''.join(el.itertext())
    for pl in root.findall('plurals'):
        forms = {it.get('quantity'): ''.join(it.itertext()) for it in pl.findall('item')}
        xml_plurals[pl.get('name')] = forms

# 2) all keys present
for k in STRINGS:
    if k not in xml_strings:
        errors.append("MISSING string key in XML: %s" % k)
for k in PLURALS:
    if k not in xml_plurals:
        errors.append("MISSING plurals key in XML: %s" % k)

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')

def args_of(s):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', s))

for k, ptval in STRINGS.items():
    if k not in xml_strings:
        continue
    xv = xml_strings[k]
    dev = de_s.get(k, '')
    # 3) not identical to DE (decode \n etc for compare not needed; compare raw vs raw)
    # de_s stores literal \\n as in source; XML itext returns literal backslash-n too. Compare directly.
    de_plain = dev.replace('\\n', '').replace('\\"', '"').strip()
    pt_plain = xv.replace('\\n', '').replace('\\"', '"').strip()
    if pt_plain and pt_plain == de_plain:
        errors.append("IDENTICAL to DE: %s" % k)
    # 4) format args identical (compare against DE source decoded the same way)
    # de source uses %1$s etc; xml itext also. Need to compare placeholder sets.
    de_args = args_of(dev)
    pt_args = args_of(xv)
    if de_args != pt_args:
        errors.append("FORMAT-ARG mismatch %s: de=%s pt=%s" % (k, de_args, pt_args))
    # 5) no bare % (after removing valid tokens)
    unguarded = fmt.sub('', xv).count('%')
    if unguarded != 0:
        errors.append("UNGUARDED %% in %s (%d)" % (k, unguarded))

# plurals checks
for k, forms in PLURALS.items():
    if k not in xml_plurals:
        continue
    for q, t in forms.items():
        xv = xml_plurals[k].get(q, '')
        de_t = de_p.get(k, {}).get(q, '')
        if args_of(xv) != args_of(de_t):
            errors.append("PLURAL fmt mismatch %s/%s" % (k, q))
        if xv.strip() == de_t.strip():
            errors.append("PLURAL identical to DE %s/%s: %r" % (k, q, xv))
        if fmt.sub('', xv).count('%') != 0:
            errors.append("PLURAL unguarded %% %s/%s" % (k, q))

# 6) PT-BR leakage check on my values only
all_my = list(xml_strings[k] for k in STRINGS if k in xml_strings)
all_my += [v for k in PLURALS if k in xml_plurals for v in xml_plurals[k].values()]
joined = '\n'.join(all_my)
br_terms = [r'\busuário', r'\busuario', r'\baplicativo', r'\bsalvar', r'\bsalvando',
            r'\bconfigurações\b', r'\bsenha\b', r'\bbaixar', r'\bcelular', r'\barquivo',
            r'\bexcluir', r'\btela\b', r'\bvocê', r'\bcompartilh', r'\bassinatura',
            r'ando\.\.\.', r'endo\.\.\.', r'indo\.\.\.']
for t in br_terms:
    if re.search(t, joined, re.IGNORECASE):
        warns.append("possible PT-BR term: %s" % t)

# 7) apostrophe escaping (run validator separately too); here flag unescaped ' in non-quoted strings
# Android: a literal ' must be \'. Our XML inner text: look for ' not preceded by backslash.
for k in STRINGS:
    if k not in xml_strings: continue
    xv = xml_strings[k]
    # find ' that is not \'
    for m in re.finditer(r"(?<!\\)'", xv):
        warns.append("possible unescaped apostrophe in %s near: ...%s..." % (k, xv[max(0,m.start()-12):m.start()+5]))
        break

print("ERRORS: %d" % len(errors))
for e in errors: print("  ✗", e)
print("WARNINGS: %d" % len(warns))
for w in warns: print("  ⚠", w)
print("RESULT:", "PASS" if not errors else "FAIL")
