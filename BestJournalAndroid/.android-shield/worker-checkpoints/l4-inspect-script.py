# -*- coding: utf-8 -*-
"""L4: Zeige die Detail-Befunde aus l4-script-report.json zur Klassifikation."""
import json, os
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'worker-checkpoints', 'l4-script-report.json'), 'r', encoding='utf-8') as f:
    rep = json.load(f)

for lang in ['ta', 'te', 'th']:
    r = rep[lang]
    print(f"\n########## {lang} ##########")
    print(f"--- emptyStrings ({len(r['emptyStrings'])}): {r['emptyStrings']}")
    print(f"--- lowScriptRatio ({len(r['lowScriptRatio'])}):")
    for it in r['lowScriptRatio']:
        print(f"    [{it['key']}] ratio={it['ratio']} tgt={it['target']}/{it['letters']}  | {it['value']}")
    print(f"--- noTargetScriptButHasText ({len(r['noTargetScriptButHasText'])}):")
    for it in r['noTargetScriptButHasText']:
        print(f"    [{it['key']}]  | {it['value']}")
