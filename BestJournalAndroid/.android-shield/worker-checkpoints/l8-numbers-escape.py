# -*- coding: utf-8 -*-
"""Numbers check for ai_limits_dialog_body + newline-escape integrity for rerank_system."""
import json, os, sys, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from l8_lib import parse_strings  # noqa

root = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
# Read raw to inspect actual escaping (not the unescaped-for-comparison)
def raw_string(path, name):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    m = re.search(r'<string\s+name="'+re.escape(name)+r'"[^>]*>(.*?)</string>', content, re.DOTALL)
    return m.group(1) if m else None

de_p = os.path.join(root, 'values/strings.xml')
br_p = os.path.join(root, 'values-pt-rBR/strings.xml')
pt_p = os.path.join(root, 'values-pt-rPT/strings.xml')

# --- NUMBERS CHECK ai_limits_dialog_body ---
mandated = ['150','600','30','101','151','50','5']
print("=== ai_limits_dialog_body NUMBERS ===")
for lang, p in [('DE',de_p),('BR',br_p),('PT',pt_p)]:
    raw = raw_string(p, 'ai_limits_dialog_body')
    if raw is None:
        print(f"  {lang}: MISSING"); continue
    # count occurrences of each mandated number as standalone token
    nums = re.findall(r'\d+', raw)
    counts = {n: nums.count(n) for n in mandated}
    # how many times 150 appears (DE has 4x)
    print(f"  {lang}: len={len(raw)} | 150x{counts['150']} 600x{counts['600']} 30x{counts['30']} 101x{counts['101']} 151x{counts['151']} 50x{counts['50']} 5x{counts['5']}")
    # full number sequence
    print(f"       all-nums-seq: {nums}")

# --- NEWLINE ESCAPE INTEGRITY rerank_system ---
print("\n=== rerank_system NEWLINE ESCAPE ===")
for lang, p in [('DE',de_p),('BR',br_p),('PT',pt_p)]:
    raw = raw_string(p, 'ai_prompt_rerank_system')
    if raw is None:
        print(f"  {lang}: MISSING"); continue
    literal_newlines = raw.count('\n')          # actual newline chars in XML
    escaped_nn = raw.count('\\n')               # backslash-n sequences
    print(f"  {lang}: len={len(raw)} | actual-newline-chars={literal_newlines} | '\\\\n'-sequences={escaped_nn}")

# --- check a few BR/PT differentiation samples for legal keys that are identical BR==PT ---
print("\n=== consent_toggle_do_not_sell_body identical BR==PT? ===")
de_s,_,_ = parse_strings(de_p); br_s,_,_ = parse_strings(br_p); pt_s,_,_ = parse_strings(pt_p)
k='consent_toggle_do_not_sell_body'
print(f"  BR==PT: {br_s[k].strip()==pt_s[k].strip()}")
# '2026' invented year present?
for lang,d in [('BR',br_s),('PT',pt_s),('DE',de_s)]:
    print(f"  {lang} has '2026': {'2026' in d[k]}; mentions 'verkauft/vende/sell-clause': {'nirgendwo' in d[k] or 'vende' in d[k].lower() or 'vender' in d[k].lower()}")
