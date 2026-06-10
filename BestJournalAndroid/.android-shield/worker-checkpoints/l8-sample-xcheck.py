# -*- coding: utf-8 -*-
"""Random sample 15/lang + cross-contamination check + remaining stale keys + key-name check."""
import json, os, sys, re, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from l8_lib import parse_strings  # noqa

root = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
de_s, de_p, de_a = parse_strings(os.path.join(root, 'values/strings.xml'))
br_s, br_p, br_a = parse_strings(os.path.join(root, 'values-pt-rBR/strings.xml'))
pt_s, pt_p, pt_a = parse_strings(os.path.join(root, 'values-pt-rPT/strings.xml'))

# --- cross-contamination: where does 'time' appear in pt-PT? where 'eliminar' in pt-BR? ---
print("=== 'time' in pt-rPT (BR-marker suspect) ===")
for k,v in pt_s.items():
    if re.search(r'\btime\b', v.lower()):
        print(f"  {k}: {v[:80]}")
print("=== 'eliminar' in pt-rBR (PT-marker suspect) ===")
for k,v in br_s.items():
    if 'eliminar' in v.lower():
        print(f"  {k}: {v[:90]}")

# --- key name check: onboarding_feature_secure* ---
print("\n=== onboarding_feature_secure* keys present ===")
for k in sorted(de_s):
    if 'onboarding_feature_secure' in k:
        print(f"  DE has: {k} = {de_s[k][:60]}")
        print(f"     BR: {br_s.get(k,'[MISSING]')[:60]}")
        print(f"     PT: {pt_s.get(k,'[MISSING]')[:60]}")

# --- remaining stale-pattern keys (gemini, crisis, consent cards, gemini gate) ---
print("\n=== REMAINING STALE/LEGAL KEYS ===")
more = ['privacy_gate_gemini_body','settings_crisis_dialog_body','consent_card1_body','consent_card3_title','consent_confirmation','permission_rationale_microphone','dashboard_gemini_unavailable','paywall_legal_hint','onboarding_free_after_trial']
for k in more:
    de=de_s.get(k); br=br_s.get(k); pt=pt_s.get(k)
    print(f"\n  [{k}]")
    print(f"   DE({len(de) if de else 0}): {de[:140] if de else '[MISSING]'}")
    print(f"   BR({len(br) if br else 0}): {br[:140] if br else '[MISSING]'}")
    print(f"   PT({len(pt) if pt else 0}): {pt[:140] if pt else '[MISSING]'}")

# --- random sample 15 per lang (seeded for reproducibility) ---
random.seed(808)
all_keys = sorted([k for k in de_s if k in br_s and k in pt_s and not k.startswith('dev_seed')])
sample = random.sample(all_keys, 15)
out = {'br': {}, 'pt': {}}
print("\n=== RANDOM SAMPLE 15 ===")
for k in sample:
    out['br'][k] = {'de': de_s[k], 'br': br_s[k]}
    out['pt'][k] = {'de': de_s[k], 'pt': pt_s[k]}
    print(f"\n  [{k}]")
    print(f"   DE: {de_s[k][:90]}")
    print(f"   BR: {br_s[k][:90]}")
    print(f"   PT: {pt_s[k][:90]}")
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base,'l8-sample.json'),'w',encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=1)
