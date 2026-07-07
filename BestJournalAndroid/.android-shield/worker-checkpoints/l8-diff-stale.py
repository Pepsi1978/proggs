# -*- coding: utf-8 -*-
"""BR/PT differentiation + stale-pattern targeted dump."""
import json, os, sys, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from l8_lib import parse_strings  # noqa

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
root = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')

de_s, de_p, de_a = parse_strings(os.path.join(root, 'values/strings.xml'))
br_s, br_p, br_a = parse_strings(os.path.join(root, 'values-pt-rBR/strings.xml'))
pt_s, pt_p, pt_a = parse_strings(os.path.join(root, 'values-pt-rPT/strings.xml'))

# --- BR/PT differentiation: count identical values across all shared keys ---
shared = [k for k in br_s if k in pt_s]
identical_brpt = [k for k in shared if br_s[k].strip() == pt_s[k].strip()]
n_shared = len(shared)
n_ident = len(identical_brpt)
pct = round(100.0 * n_ident / n_shared, 1) if n_shared else 0
print(f"=== BR/PT DIFFERENTIATION ===")
print(f"Shared keys: {n_shared}; identical BR==PT: {n_ident} ({pct}%)")

# Variant markers: BR-specific vs PT-specific spellings
# PT(pt-PT) often: gerir, ecra, registo, conta(same), aceder, eliminar/apagar; pt-BR: gerenciar, tela, registro, acessar, excluir/deletar
br_markers = ['gerenciar', 'gerencie', 'tela', 'registro', 'acessar', 'acesse', 'excluir', 'deletar', 'cancele', 'usuario', 'usuária', 'time', 'aplicativo', 'tela inicial', 'celular']
pt_markers = ['gerir', 'gira', 'ecra', 'registo', 'aceder', 'eliminar', 'apagar', 'utilizador', 'aplicacao', 'telemovel', 'ecran']
def count_marker_hits(strings, markers):
    hits = {}
    blob = ' '.join(v.lower() for v in strings.values())
    # strip accents crudely for matching ecra/ecra
    for m in markers:
        c = blob.count(m)
        if c:
            hits[m] = c
    return hits
br_hits_in_br = count_marker_hits(br_s, br_markers)
pt_hits_in_pt = count_marker_hits(pt_s, pt_markers)
# cross: does BR contain pt-PT markers (would indicate PT text in BR) and vice versa
pt_markers_in_br = count_marker_hits(br_s, pt_markers)
br_markers_in_pt = count_marker_hits(pt_s, br_markers)
print(f"\nBR-spezifische Marker IN pt-rBR: {br_hits_in_br}")
print(f"PT-spezifische Marker IN pt-rPT: {pt_hits_in_pt}")
print(f"PT-Marker faelschlich IN pt-rBR (Verdacht): {pt_markers_in_br}")
print(f"BR-Marker faelschlich IN pt-rPT (Verdacht): {br_markers_in_pt}")

# Sample 20 legal keys where BR and PT differ vs where identical
with open(os.path.join(base, 'legal-keys-2026-06-10.json'), 'r', encoding='utf-8') as f:
    legal_raw = json.load(f)['legalCriticalKeys']
legal_keys = []
for k in legal_raw:
    if '/' in k:
        for p in k.split('/'):
            p=p.strip()
            if p and '*' not in p: legal_keys.append(p)
    else:
        legal_keys.append(k.strip())
legal_in = [k for k in legal_keys if k in br_s and k in pt_s]
legal_ident_brpt = [k for k in legal_in if br_s[k].strip()==pt_s[k].strip()]
print(f"\nLegal keys shared BR/PT: {len(legal_in)}; identical BR==PT: {len(legal_ident_brpt)} ({round(100*len(legal_ident_brpt)/len(legal_in),1)}%)")
print(f"Legal keys IDENTICAL BR==PT: {legal_ident_brpt}")

# --- STALE-PATTERN targeted keys ---
stale_keys = [
    'settings_delete_account_subtitle','settings_delete_account_confirm_body',
    'settings_revoke_subtitle','settings_revoke_confirm_title','settings_revoke_confirm_body',
    'consent_toggle_do_not_sell_body',
    'privacy_gate_tts_title','privacy_gate_tts_body','privacy_gate_groq_body',
    'ai_prompt_rerank_system','ai_prompt_rerank_actions_header','ai_prompt_rerank_entries_header',
    'ai_prompt_rerank_user_focus_header','ai_prompt_rerank_user',
    'ai_limits_dialog_body',
    'settings_revoke_email_body','settings_revoke_subtitle',
]
stale_dump = {}
for k in stale_keys:
    de = de_s.get(k); br = br_s.get(k); pt = pt_s.get(k)
    stale_dump[k] = {'de': de, 'br': br, 'pt': pt,
                     'br_eq_de': (de is not None and br is not None and br.strip()==de.strip()),
                     'pt_eq_de': (de is not None and pt is not None and pt.strip()==de.strip())}
# plurals: datetime_months_relative, datetime_years_relative
for pk in ['datetime_months_relative','datetime_years_relative']:
    stale_dump[pk] = {'de': de_p.get(pk), 'br': br_p.get(pk), 'pt': pt_p.get(pk)}
with open(os.path.join(base, 'l8-stale.json'), 'w', encoding='utf-8') as f:
    json.dump(stale_dump, f, ensure_ascii=False, indent=1)
print(f"\nStale-pattern keys dumped: {len(stale_dump)} -> l8-stale.json")

# write differentiation result
diff_result = {
    'sharedKeys': n_shared, 'identicalBrPt': n_ident, 'identicalPct': pct,
    'legalShared': len(legal_in), 'legalIdenticalBrPt': len(legal_ident_brpt),
    'legalIdenticalPct': round(100*len(legal_ident_brpt)/len(legal_in),1) if legal_in else 0,
    'legalIdenticalKeys': legal_ident_brpt,
    'brMarkersInBr': br_hits_in_br, 'ptMarkersInPt': pt_hits_in_pt,
    'ptMarkersInBr': pt_markers_in_br, 'brMarkersInPt': br_markers_in_pt,
}
with open(os.path.join(base, 'l8-diff.json'), 'w', encoding='utf-8') as f:
    json.dump(diff_result, f, ensure_ascii=False, indent=1)
