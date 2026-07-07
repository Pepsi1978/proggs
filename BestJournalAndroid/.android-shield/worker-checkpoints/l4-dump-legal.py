# -*- coding: utf-8 -*-
"""L4: Dump Legal-Paare gruppenweise zur semantischen Pruefung. Batch-Argument."""
import json, os, sys
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'worker-checkpoints', 'l4-legal-pairs.json'), 'r', encoding='utf-8') as f:
    pairs = json.load(f)['pairs']

group = sys.argv[1] if len(sys.argv) > 1 else 'churn'
prefixes = {
    'churn': ['churn_'],
    'consent': ['consent_'],
    'paywall': ['paywall_'],
    'onboarding': ['onboarding_'],
    'settings': ['settings_'],
    'misc': ['permission_', 'privacy_gate_', 'entry_', 'follow_up_', 'prompt_', 'retro_',
             'dashboard_', 'journal_', 'profile_', 'transcription_', 'urgency_', 'legend_',
             'ai_improved', 'ai_limit_yearly', 'ai_prompt_', 'dev_seed_'],
}
keys = sorted(k for k in pairs if any(k.startswith(p) for p in prefixes.get(group, [group])))
print(f"### GRUPPE '{group}' — {len(keys)} Keys\n")
for k in keys:
    p = pairs[k]
    print(f"@@ {k}")
    for lang in ['de', 'ta', 'te', 'th']:
        v = p[lang].replace('\n', '\\n')
        if len(v) > 220:
            v = v[:220] + ' …[+]'
        print(f"  {lang}: {v}")
    print()
