# -*- coding: utf-8 -*-
"""L4: Rigoroser Zahlen-Check fuer ai_limits_dialog_body.
   Erwartet: 150, 600, 30, 101, 151, 50, 5 (alle EXAKT vorhanden).
   Prueft auch auf lokalisierte Ziffern (Tamil U+0BE6-EF, Telugu U+0C66-6F, Thai U+0E50-59)."""
import json, os, re
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'worker-checkpoints', 'l4-legal-pairs.json'), 'r', encoding='utf-8') as f:
    pairs = json.load(f)['pairs']

expected = ['150', '600', '30', '101', '151', '50', '5']
# Lokalisierte Ziffern-Bloecke
LOCAL_DIGITS = {
    'ta': range(0x0BE6, 0x0BF0),
    'te': range(0x0C66, 0x0C70),
    'th': range(0x0E50, 0x0E5A),
}

key = 'ai_limits_dialog_body'
p = pairs[key]
result = {}
for lang in ['de', 'ta', 'te', 'th']:
    v = p[lang]
    # Westliche Zahlen (als Token mit Wortgrenzen-naeherung)
    western = re.findall(r'\d+', v)
    found = {num: (num in western) for num in expected}
    # zaehle Vorkommen jeder erwarteten Zahl
    counts = {num: len(re.findall(r'(?<!\d)' + num + r'(?!\d)', v)) for num in expected}
    # Lokalisierte Ziffern?
    local = []
    if lang in LOCAL_DIGITS:
        for ch in v:
            if ord(ch) in LOCAL_DIGITS[lang]:
                local.append(ch)
    result[lang] = {
        'allExpectedPresent': all(found.values()),
        'missing': [n for n in expected if not found[n]],
        'counts': counts,
        'localizedDigitsFound': len(local),
        'localizedDigitSample': ''.join(local[:10]),
        'allWesternNumbers': western,
    }
    print(f"=== {lang} ===")
    print(f"  allExpectedPresent: {result[lang]['allExpectedPresent']}  missing: {result[lang]['missing']}")
    print(f"  counts: {counts}")
    print(f"  localizedDigits: {len(local)}  western nums: {western}")

# Vergleich der westlichen Zahlen-Multimenge gegen DE
de_nums = sorted(re.findall(r'\d+', p['de']))
print("\n--- Zahlen-Multimengen-Vergleich gegen DE ---")
print(f"  de: {de_nums}")
for lang in ['ta', 'te', 'th']:
    ln = sorted(re.findall(r'\d+', p[lang]))
    same = (ln == de_nums)
    print(f"  {lang}: {'IDENTISCH' if same else 'ABWEICHUNG'}  {ln}")
    result[lang]['numbersMatchDe'] = same

out = os.path.join(base, 'worker-checkpoints', 'l4-numbers-result.json')
with open(out, 'w', encoding='utf-8') as f:
    json.dump(result, f, ensure_ascii=False, indent=1)
print("\nGeschrieben:", out)
