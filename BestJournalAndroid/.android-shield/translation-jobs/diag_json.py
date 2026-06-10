import os, json
DATA = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/ptbr-data.json')
with open(DATA, 'r', encoding='utf-8') as f:
    raw = f.read()
# Locate error position from json
try:
    json.loads(raw)
    print('OK')
except json.JSONDecodeError as e:
    print('ERR at line', e.lineno, 'col', e.colno, 'char', e.pos)
    # show context around the byte
    start = max(0, e.pos - 60)
    end = min(len(raw), e.pos + 60)
    print('CONTEXT:', repr(raw[start:end]))
