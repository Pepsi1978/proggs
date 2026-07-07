# -*- coding: utf-8 -*-
"""L4: Vergleiche Format-Specifier (%1$s, %d, %% etc.) DE vs ta/te/th ueber ALLE Strings.
   Findet Strings wo Ziel MEHR/WENIGER/ANDERE Format-Args hat als DE -> Crash-Risiko."""
import os, re
import xml.etree.ElementTree as ET
app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')

def parse(sub):
    out = {}
    for el in ET.parse(os.path.join(app, sub, 'strings.xml')).getroot():
        if el.tag=='string' and el.get('name'):
            out[el.get('name')] = ''.join(el.itertext())
    return out

# Echte Format-Args: %[argpos$]flags...conversion  (s,d,f,e,g,x ...). %% ist literal (kein arg).
ARG = re.compile(r'%(\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdfeEgGxXoc]')
def args(t):
    # Entferne %% (escaped percent) zuerst
    t2 = t.replace('%%', '')
    return sorted(ARG.findall(t2) if False else [m.group(0) for m in ARG.finditer(t2)])

de = parse('values')
report = {}
for lang in ['ta','te','th']:
    tgt = parse(f'values-{lang}')
    issues = []
    for k, dv in de.items():
        if k not in tgt: continue
        da = args(dv)
        ta_ = args(tgt[k])
        if sorted(da) != sorted(ta_):
            issues.append({'key': k, 'de_args': da, 'tgt_args': ta_,
                           'de': dv[:70], 'tgt': tgt[k][:90]})
    report[lang] = issues
    print(f"\n=== {lang}: {len(issues)} Format-Arg-Abweichungen ===")
    for it in issues:
        print(f"  @@ {it['key']}  DE={it['de_args']}  {lang}={it['tgt_args']}")
        print(f"     de:  {it['de']}")
        print(f"     {lang}: {it['tgt']}")
