import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-outputs')

def dump(path, wanted_ids):
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    # findings may be nested under various keys; search recursively for dicts with findingId/id
    results = []
    def walk(o):
        if isinstance(o, dict):
            fid = o.get('findingId') or o.get('id')
            if fid in wanted_ids:
                results.append(o)
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)
    walk(data)
    return results

print('==== UWG (B6,B7,B8,F1,F2) ====')
for r in dump(os.path.join(base, 'phase1b-recht-uwg.json'), {'B6','B7','B8','F1','F2','B5'}):
    print('ID:', r.get('findingId') or r.get('id'))
    print('  currentText:', repr(r.get('currentText') or r.get('current') or r.get('text')))
    print('  key:', r.get('key') or r.get('stringKey') or r.get('resourceKey'))
    print('  line:', r.get('line') or r.get('lineHint'))
    sf = r.get('suggestedFixes') or r.get('suggestedFix') or r.get('fixes')
    print('  suggestedFixes:', repr(sf))
    print()

print('==== HWG-AI (A5,A6) ====')
for r in dump(os.path.join(base, 'phase1b-recht-hwg-ai.json'), {'A5','A6'}):
    print('ID:', r.get('findingId') or r.get('id'))
    print('  currentText:', repr(r.get('currentText') or r.get('current') or r.get('text')))
    print('  key:', r.get('key') or r.get('stringKey') or r.get('resourceKey'))
    print('  line:', r.get('line') or r.get('lineHint'))
    sf = r.get('suggestedFixes') or r.get('suggestedFix') or r.get('fixes')
    print('  suggestedFixes:', repr(sf))
    print()
