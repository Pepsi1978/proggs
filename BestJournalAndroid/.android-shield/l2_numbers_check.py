# -*- coding: utf-8 -*-
import json, os, re
os.environ['PYTHONIOENCODING'] = 'utf-8'
shield = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
d = json.load(open(os.path.join(shield, 'l2_legal_pairs.json'), encoding='utf-8'))

KEY = 'ai_limits_dialog_body'
REQUIRED = ['150', '600', '30', '101', '151', '50', '5']

# Devanagari digit map (used by hi, mr) and Bengali digit map (bn)
DEV = {'०':'0','१':'1','२':'2','३':'3','४':'4','५':'5','६':'6','७':'7','८':'8','९':'9'}
BEN = {'০':'0','১':'1','২':'2','৩':'3','৪':'4','৫':'5','৬':'6','৭':'7','৮':'8','৯':'9'}

def to_western(s, table):
    return ''.join(table.get(ch, ch) for ch in s)

def find_numbers(text):
    """Return list of all number tokens after digit normalisation (both western + native)."""
    # Normalise both Devanagari and Bengali digits to western for matching
    norm = to_western(to_western(text, DEV), BEN)
    return re.findall(r'\d+', norm)

for L in ['bn', 'hi', 'mr']:
    txt = d[L].get(KEY) or ''
    nums = find_numbers(txt)
    # Count native vs western digits present in original
    has_dev = any(ch in DEV for ch in txt)
    has_ben = any(ch in BEN for ch in txt)
    has_west = bool(re.search(r'[0-9]', txt))
    print('=' * 50)
    print('LANG', L, '| numeral system: western=%s devanagari=%s bengali=%s' % (has_west, has_dev, has_ben))
    print('  all number tokens (normalised):', nums)
    present = {}
    for req in REQUIRED:
        # count occurrences of this exact number token
        cnt = nums.count(req)
        present[req] = cnt
    print('  REQUIRED token presence:', present)
    missing = [r for r in REQUIRED if present[r] == 0]
    print('  MISSING required numbers:', missing if missing else 'NONE — all present')
