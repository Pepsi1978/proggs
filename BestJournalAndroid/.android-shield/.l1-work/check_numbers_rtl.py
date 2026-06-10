# -*- coding: utf-8 -*-
import json, os, re

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, '.l1-work', 'legal-pairs.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)

REQUIRED = ['150', '600', '30', '101', '151', '50', '5']
# Eastern Arabic-Indic digits (Arabic) and Extended (Persian/Urdu)
ARABIC_INDIC = '٠١٢٣٤٥٦٧٨٩'      # U+0660..0669
EXT_ARABIC = '۰۱۲۳۴۵۶۷۸۹'       # U+06F0..06F9 (Persian/Urdu)

def normalize_digits(s):
    """Map eastern-arabic + extended-arabic digits to western for matching."""
    out = []
    for ch in s:
        if ch in ARABIC_INDIC:
            out.append(str(ARABIC_INDIC.index(ch)))
        elif ch in EXT_ARABIC:
            out.append(str(EXT_ARABIC.index(ch)))
        else:
            out.append(ch)
    return ''.join(out)

def digit_system(s):
    has_w = bool(re.search(r'[0-9]', s))
    has_ai = any(c in ARABIC_INDIC for c in s)
    has_ext = any(c in EXT_ARABIC for c in s)
    sysn = []
    if has_w: sysn.append('western')
    if has_ai: sysn.append('eastern-arabic')
    if has_ext: sysn.append('extended-arabic')
    return '+'.join(sysn) if sysn else 'none'

print('=== ai_limits_dialog_body NUMBER CHECK ===')
for lang in ['ar', 'ur', 'tr']:
    p = data['langs'][lang].get('ai_limits_dialog_body')
    if not p or p.get('loc') is None:
        print(f'{lang}: MISSING'); continue
    loc = p['loc']
    norm = normalize_digits(loc)
    ds = digit_system(loc)
    # count each required number as a standalone token (avoid 150 matching inside 1500)
    results = {}
    for num in REQUIRED:
        # find as a whole number (not part of a longer digit run)
        found = re.search(r'(?<![0-9])' + num + r'(?![0-9])', norm) is not None
        results[num] = found
    all_present = all(results.values())
    print(f'{lang}: digitSystem={ds}, allPresent={all_present}')
    missing = [n for n in REQUIRED if not results[n]]
    if missing:
        print(f'   MISSING NUMBERS: {missing}')
    # show all digit-runs in normalized form for sanity
    runs = re.findall(r'[0-9]+', norm)
    print(f'   digit-runs(normalized): {runs}')

print()
print('=== RTL PLACEHOLDER / xliff CHECK (ar, ur legal keys) ===')
PLACEHOLDER = re.compile(r'%\d+\$[sdf]|%[sdf]')
XLIFF = re.compile(r'<g\b')
RLM = '‏'; LRM = '‎'; RLE='‫'; PDF='‬'
for lang in ['ar', 'ur']:
    pairs = data['langs'][lang]
    issues = []
    for k in sorted(pairs):
        p = pairs[k]
        loc = p.get('loc')
        de = p.get('de')
        if loc is None or de is None:
            continue
        de_ph = PLACEHOLDER.findall(de)
        loc_ph = PLACEHOLDER.findall(loc)
        # placeholder count mismatch
        if sorted(de_ph) != sorted(loc_ph):
            issues.append((k, 'placeholder-set-mismatch', f'de={de_ph} loc={loc_ph}'))
        # xliff tag count mismatch
        if de.count('<g') != loc.count('<g'):
            issues.append((k, 'xliff-count-mismatch', f'de={de.count("<g")} loc={loc.count("<g")}'))
        # Latin placeholder at very start of an RTL string without RLM/RLE marker
        stripped = loc.lstrip()
        if (stripped.startswith('%') and not loc.startswith(RLM) and not loc.startswith(RLE)):
            # only note if it's a real format placeholder
            if PLACEHOLDER.match(stripped):
                issues.append((k, 'latin-placeholder-at-start-no-bidi-marker', stripped[:30]))
    print(f'--- {lang}: {len(issues)} placeholder/xliff/bidi notes ---')
    for k, typ, det in issues:
        print(f'   [{k}] {typ}: {det}')
