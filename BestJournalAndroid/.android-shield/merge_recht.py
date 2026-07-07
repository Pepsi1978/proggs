#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Merge aller recht-frag-*.json zu recht-report.json mit Dedup + FIN-025-IDs."""
import json, os, glob, datetime

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')

def risk_norm(r):
    if not r:
        return 'low'
    r = str(r).lower()
    if 'high' in r or 'hoch' in r or '\U0001f7e5' in r or 'kritisch' in r:
        return 'high'
    if 'medium' in r or 'mittel' in r or '\U0001f7e7' in r:
        return 'medium'
    return 'low'

# FIN-025 Kategorie-Praefixe
def cat_prefix(cat):
    c = (cat or '').lower()
    if 'hwg' in c or 'heil' in c or 'gesund' in c:
        return 'A'
    if 'uwg' in c or 'werb' in c or 'irrefuehr' in c or 'irrefuhr' in c:
        return 'B'
    if 'dsgvo' in c or 'datenschutz' in c or 'consent' in c or 'ttdsg' in c:
        return 'C'
    if 'bgb' in c or 'widerruf' in c or 'abo' in c or 'transparenz' in c or 'vertrag' in c:
        return 'D'
    if 'play' in c or 'data-safety' in c or 'datasafety' in c or 'data safety' in c:
        return 'E'
    if 'dark' in c:
        return 'F'
    if 'missing' in c or 'doc' in c or 'url' in c or 'version' in c or 'marken' in c:
        return 'G'
    return 'Z'

all_findings = []
sources = {}
for fp in sorted(glob.glob(os.path.join(base, 'recht-frag-*.json'))):
    name = os.path.basename(fp)
    try:
        d = json.load(open(fp, encoding='utf-8'))
    except Exception as e:
        sources[name] = f'PARSE-ERROR: {e}'
        continue
    fnds = d.get('findings', [])
    sources[name] = len(fnds)
    for f in fnds:
        f['_source'] = name
        all_findings.append(f)

# Dedup nach (key, risk, cat-prefix) bzw. (problem-prefix) wenn kein key
seen = {}
deduped = []
for f in all_findings:
    key = f.get('key') or ''
    cat = f.get('category', '')
    risk = risk_norm(f.get('riskLevel'))
    pfx = cat_prefix(cat)
    if key:
        dedup_key = (key, pfx)
    else:
        prob = (f.get('problem') or f.get('summary') or '')[:50]
        langs = ','.join(sorted(f.get('affectedLangs', []) or []))[:30]
        dedup_key = (pfx, prob, langs)
    if dedup_key in seen:
        # Merge: hoeheres Risiko + zweite Quelle vermerken
        ex = seen[dedup_key]
        if risk == 'high' and risk_norm(ex.get('riskLevel')) != 'high':
            ex['riskLevel'] = '\U0001f7e5 high'
        ex.setdefault('_alsoFoundBy', []).append(f.get('_source'))
        continue
    seen[dedup_key] = f
    deduped.append(f)

# FIN-025-IDs vergeben
counters = {}
for f in sorted(deduped, key=lambda x: {'high':0,'medium':1,'low':2}[risk_norm(x.get('riskLevel'))]):
    pfx = cat_prefix(f.get('category',''))
    counters[pfx] = counters.get(pfx, 0) + 1
    f['catId'] = f'{pfx}{counters[pfx]}'
    f['riskNorm'] = risk_norm(f.get('riskLevel'))

by_risk = {'high': [], 'medium': [], 'low': []}
for f in deduped:
    by_risk[f['riskNorm']].append(f)

report = {
    'mode': 'default',
    'audit_date': datetime.datetime.now().isoformat(),
    'auditedLanguages': 27,
    'scan_note': 'Phase 1B Layer-Split (Worker-Crashes umgangen). String-Vollabdeckung Z.1-1508 durch 4 Buckets + Self-Scan 785-965.',
    'sources': sources,
    'openFindingsCount': len(deduped),
    'countsByRisk': {k: len(v) for k, v in by_risk.items()},
    'findings': deduped,
}
tmp = os.path.join(base, 'recht-report.json.tmp')
with open(tmp, 'w', encoding='utf-8') as fh:
    json.dump(report, fh, indent=2, ensure_ascii=False)
os.replace(tmp, os.path.join(base, 'recht-report.json'))

print('=== QUELLEN ===')
for k, v in sources.items():
    print(f'  {k}: {v} findings')
print(f'\n=== KONSOLIDIERT (nach Dedup): {len(deduped)} findings ===')
print(f'  HIGH:   {len(by_risk["high"])}')
print(f'  MEDIUM: {len(by_risk["medium"])}')
print(f'  LOW:    {len(by_risk["low"])}')
print('\n=== HIGH-FINDINGS (Karte-fuer-Karte) ===')
for f in by_risk['high']:
    k = f.get('key') or (','.join(f.get('affectedLangs', [])[:5]) if f.get('affectedLangs') else '-')
    print(f'  [{f["catId"]}] {f.get("category","?")} | {k} | L{f.get("line","-")} | {(f.get("problem") or f.get("summary") or "")[:90]}')
