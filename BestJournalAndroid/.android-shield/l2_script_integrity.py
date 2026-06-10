# -*- coding: utf-8 -*-
import json, os, re, xml.etree.ElementTree as ET
os.environ['PYTHONIOENCODING'] = 'utf-8'
base = os.path.expanduser('~/proggs/BestJournalAndroid')
RES = os.path.join(base, 'app/src/main/res')

# Unicode block ranges
def in_range(ch, lo, hi):
    return lo <= ord(ch) <= hi

DEVANAGARI = (0x0900, 0x097F)   # hi, mr
BENGALI = (0x0980, 0x09FF)      # bn
LATIN_LETTERS = re.compile(r'[A-Za-z]')

# Allowed latin tokens (brand/tech terms legitimately kept in latin)
ALLOWED_LATIN = {
    'AI', 'KI', 'PREMIUM', 'Premium', 'Pro', 'PDF', 'Groq', 'Gemini', 'Google',
    'Drive', 'TTS', 'AGB', 'DSGVO', 'OK', 'App', 'BestJournal', 'Whoop', 'Oura',
    'Strava', 'Polar', 'Garmin', 'Fitbit', 'plus', 'Markdown', 'JSON', 'URL',
    'Android', 'iOS', 'GB', 'MB', 'KB', 'min', 'Min', 'a', 'b', 'c', 'x',
    'HTML', 'XML', 'API', 'ID', 'UI', 'GPS', 'WLAN', 'Wi', 'Fi', 'WiFi',
    'Email', 'E', 'Mail', 'SMS', 'Bluetooth', 'USB', 'Wear', 'OS', 'Cloud',
    'CSV', 'TXT', 'Apple', 'Health', 'Connect', 'Zepp', 'Samsung', 'pt', 'br',
}

def load_strings(path):
    tree = ET.parse(path)
    root = tree.getroot()
    out = {}
    plurals = {}
    arrays = {}
    for el in root:
        name = el.get('name')
        if el.tag == 'string':
            txt = ET.tostring(el, encoding='unicode')
            inner = re.sub(r'^<string[^>]*>', '', txt)
            inner = re.sub(r'</string>\s*$', '', inner).strip()
            out[name] = inner
        elif el.tag == 'plurals':
            for item in el:
                q = item.get('quantity')
                t = (item.text or '')
                plurals['%s[%s]' % (name, q)] = t
        elif el.tag == 'string-array':
            for i, item in enumerate(el):
                arrays['%s[%d]' % (name, i)] = (item.text or '')
    return out, plurals, arrays

SCRIPT = {'bn': BENGALI, 'hi': DEVANAGARI, 'mr': DEVANAGARI}

report = {}
for L in ['bn', 'hi', 'mr']:
    path = os.path.join(RES, 'values-%s/strings.xml' % L)
    strings, plurals, arrays = load_strings(path)
    allv = dict(strings)
    allv.update(plurals)
    allv.update(arrays)
    lo, hi_ = SCRIPT[L]
    res = {
        'totalStrings': len(strings), 'totalPlurals': len(plurals), 'totalArrays': len(arrays),
        'replacementChar': [],     # U+FFFD
        'emptyStrings': [],
        'noTargetScript': [],      # value has NO chars in target script (and not pure-symbol/number)
        'latinFragments': [],      # latin word-fragments not in allowlist embedded in native text
        'controlChars': [],        # stray control chars
        'halantDangling': [],      # virama (्/্) at end of a word cluster oddly
    }
    VIRAMA = '्' if L != 'bn' else '্'
    for k, v in allv.items():
        if v is None:
            continue
        # 1. replacement char
        if '�' in v:
            res['replacementChar'].append(k)
        # 2. empty (after stripping placeholders/whitespace)
        stripped = re.sub(r'%\d*\$?[sd]|%%|\s|<[^>]+>|&\w+;', '', v).strip()
        if stripped == '' and v.strip() == '':
            res['emptyStrings'].append(k)
        # 3. no target script char — only flag if there ARE letters but none in target script
        has_target = any(in_range(ch, lo, hi_) for ch in v)
        has_latin_letter = bool(LATIN_LETTERS.search(v))
        if not has_target and has_latin_letter:
            # Could be legitimately all-latin (brand). Record for classification.
            res['noTargetScript'].append({'key': k, 'val': v[:80]})
        # 4. latin fragments embedded inside native text
        if has_target and has_latin_letter:
            latin_tokens = re.findall(r'[A-Za-z]+', v)
            suspicious = [t for t in latin_tokens if t not in ALLOWED_LATIN and len(t) >= 2]
            if suspicious:
                res['latinFragments'].append({'key': k, 'tokens': suspicious, 'val': v[:90]})
        # 5. control chars (excluding tab/newline which appear as \n literal anyway)
        ctrl = [hex(ord(ch)) for ch in v if ord(ch) < 0x20 and ch not in '\t\n\r']
        if ctrl:
            res['controlChars'].append({'key': k, 'ctrl': ctrl})
        # 6. dangling virama at very end of string (corruption symptom)
        if v.rstrip().endswith(VIRAMA):
            res['halantDangling'].append(k)
    # Summaries
    print('=' * 60)
    print('LANG', L, '| strings=%d plurals=%d arrays=%d' % (len(strings), len(plurals), len(arrays)))
    print('  U+FFFD replacement chars:', len(res['replacementChar']), res['replacementChar'][:10])
    print('  empty strings:', len(res['emptyStrings']), res['emptyStrings'][:10])
    print('  control chars:', len(res['controlChars']), res['controlChars'][:5])
    print('  dangling virama:', len(res['halantDangling']), res['halantDangling'][:5])
    print('  NO target-script (latin-only, needs classify):', len(res['noTargetScript']))
    for x in res['noTargetScript'][:40]:
        print('       ', x['key'], '=', x['val'])
    print('  latin fragments embedded (suspicious tokens):', len(res['latinFragments']))
    report[L] = res

with open(os.path.join(base, '.android-shield', 'l2_script_integrity.json'), 'w', encoding='utf-8') as f:
    json.dump(report, f, ensure_ascii=False, indent=1)
print('\nWROTE l2_script_integrity.json')
