#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
L3 GU-KORRUPTIONS-VOLLPRUEFUNG (read-only).
Scannt ALLE gu-Strings + Plurals + String-Arrays auf Korruptions-Signaturen.
Hintergrund: 2026 Vorfall (153 Strings durch Platzhalter-Symbole zerstoert, Fix #1288).
Diese Pruefung verifiziert, dass gu jetzt sauber ist.
"""
import re, os, sys, json
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
GU = os.path.join(ROOT, 'app/src/main/res/values-gu/strings.xml')

# --- Unicode ranges ---
GUJ_LO, GUJ_HI = 0x0A80, 0x0AFF          # Gujarati block
HALANT = 0x0ACD                          # virama
def is_guj(ch): return GUJ_LO <= ord(ch) <= GUJ_HI
def is_latin(ch): return ('a' <= ch <= 'z') or ('A' <= ch <= 'Z')

# Corruption signatures
REPLACEMENT = '�'                                   # replacement char
GEOMETRIC = re.compile(r'[■-◿]')               # geometric shapes (■ ▲ ● etc.)
BOXDRAW   = re.compile(r'[─-╿]')               # box drawing
BLOCKELEM = re.compile(r'[▀-▟]')               # block elements
PLACEHOLDER_REPEAT = re.compile(r'([■-◿─-▟�\?])\1{2,}')  # ??? or ■■■

# Strings that legitimately have low Gujarati ratio (skip ratio check)
SKIP_RATIO_PREFIX = ('json_key_', 'json_value_')
# Format/markup tokens we strip before ratio analysis
FMT_TOKEN = re.compile(r'%\d*\$?[a-z]|%[a-z]|\\n|\\t|\\u[0-9A-Fa-f]{4}|&#?\w+;|<[^>]+>')
DIGIT_PUNCT = re.compile(r'[\s0-9\.,:;!\?\-—–…/()\[\]{}€$%#@*+=_"\'`~|\\<>°•·„“”‚‘’]')

def analyze_text(raw):
    """Return dict of signals for a decoded text value."""
    sig = {}
    if raw is None:
        sig['empty'] = True
        return sig
    if raw.strip() == '':
        sig['empty'] = True
        return sig
    if REPLACEMENT in raw:
        sig['replacement_char'] = raw.count(REPLACEMENT)
    g = GEOMETRIC.findall(raw)
    if g:
        sig['geometric_shapes'] = ''.join(sorted(set(g)))
    b = BOXDRAW.findall(raw)
    if b:
        sig['box_drawing'] = ''.join(sorted(set(b)))
    be = BLOCKELEM.findall(raw)
    if be:
        sig['block_elements'] = ''.join(sorted(set(be)))
    pr = PLACEHOLDER_REPEAT.findall(raw)
    if pr:
        sig['repeat_symbols'] = pr
    # truncation: ends with halant (dangling virama -> cut mid-cluster)
    if raw and ord(raw.rstrip()[-1]) == HALANT:
        sig['ends_with_halant'] = True
    return sig

def guj_ratio(raw):
    """Fraction of letters that are Gujarati, after removing markup/format/punct/digits."""
    # remove markup & format tokens & punctuation & digits & whitespace
    t = FMT_TOKEN.sub('', raw)
    t = DIGIT_PUNCT.sub('', t)
    letters = [c for c in t if c.isalpha() or is_guj(c)]
    if not letters:
        return None  # nothing to judge (pure number/symbol value)
    guj = sum(1 for c in letters if is_guj(c))
    return guj / len(letters), len(letters)

def latin_fragment(raw):
    """Detect a run of >=4 consecutive Latin letters embedded among Gujarati."""
    # strip format tokens / markup first so xliff/placeholders don't trip it
    t = FMT_TOKEN.sub(' ', raw)
    runs = re.findall(r'[A-Za-z]{4,}', t)
    has_guj = any(is_guj(c) for c in t)
    if has_guj and runs:
        return runs
    return None

def parse_file(path):
    """Parse strings.xml, return list of (kind, name, sub, text)."""
    tree = ET.parse(path)
    root = tree.getroot()
    out = []
    for el in root:
        name = el.get('name')
        tag = el.tag
        if tag == 'string':
            # full inner text including markup -> reconstruct
            txt = inner_text(el)
            out.append(('string', name, None, txt))
        elif tag == 'plurals':
            for it in el.findall('item'):
                q = it.get('quantity')
                out.append(('plural', name, q, inner_text(it)))
        elif tag == 'string-array':
            for i, it in enumerate(el.findall('item')):
                out.append(('array', name, str(i), inner_text(it)))
    return out

def inner_text(el):
    """Reconstruct full inner text incl. nested markup (xliff:g etc.)."""
    parts = []
    if el.text:
        parts.append(el.text)
    for child in el:
        # include child markup as serialized
        parts.append(ET.tostring(child, encoding='unicode'))
        if child.tail:
            parts.append(child.tail)
    return ''.join(parts)

def main():
    items = parse_file(GU)
    strings = [x for x in items if x[0] == 'string']
    plurals = [x for x in items if x[0] == 'plural']
    arrays  = [x for x in items if x[0] == 'array']
    print("PARSED gu: strings=%d plural-items=%d array-items=%d (total nodes=%d)"
          % (len(strings), len(plurals), len(arrays), len(items)))

    suspicious = []
    low_ratio = []
    for kind, name, sub, txt in items:
        keyid = name if sub is None else f"{name}[{sub}]"
        sig = analyze_text(txt)
        if sig:
            reasons = []
            for k, v in sig.items():
                reasons.append(f"{k}={v}")
            suspicious.append({'key': keyid, 'kind': kind,
                               'reason': '; '.join(reasons),
                               'sample': (txt or '')[:60]})
        # Latin fragment among Gujarati
        lf = latin_fragment(txt or '')
        if lf:
            # filter known acceptable brand/tech tokens
            acceptable = {'Firebase','Google','Gemini','Groq','Drive','Whisper','OpenAI',
                          'AGB','PDF','URL','API','iOS','Android','GmbH','EU','USA','UK',
                          'DSGVO','App','Journal','Best','Entropie','TTS','KI','AI'}
            unknown = [r for r in lf if r not in acceptable]
            if unknown:
                suspicious.append({'key': keyid, 'kind': kind,
                                   'reason': f"latin_fragment={unknown}",
                                   'sample': (txt or '')[:60]})
        # Gujarati ratio (skip json_/value tokens & pure-symbol values)
        if not name.startswith(SKIP_RATIO_PREFIX):
            r = guj_ratio(txt or '')
            if r is not None:
                ratio, nletters = r
                if ratio < 0.30 and nletters >= 3:
                    low_ratio.append({'key': keyid, 'kind': kind,
                                      'ratio': round(ratio, 2), 'letters': nletters,
                                      'sample': (txt or '')[:60]})

    print("\n=== CORRUPTION SIGNATURES (FFFD/geometric/box/block/repeat/empty/halant) ===")
    if not suspicious:
        print("  NONE")
    else:
        for s in suspicious:
            print(f"  [{s['kind']}] {s['key']}: {s['reason']}")
            print(f"        sample: {s['sample']!r}")

    print("\n=== LOW GUJARATI RATIO (<30%%, >=3 letters, excl json_/value tokens) ===")
    if not low_ratio:
        print("  NONE")
    else:
        for s in low_ratio:
            print(f"  [{s['kind']}] {s['key']}: ratio={s['ratio']} letters={s['letters']}")
            print(f"        sample: {s['sample']!r}")

    total_scanned = len(items)
    n_susp = len(suspicious) + len(low_ratio)
    verdict = "sauber" if n_susp == 0 else f"{n_susp} Funde"
    print("\nVERDICT: scanned=%d suspicious=%d -> %s" % (total_scanned, n_susp, verdict))

    # dump machine-readable for output stage
    res = {'scanned': total_scanned,
           'stringNodes': len(strings), 'pluralItems': len(plurals), 'arrayItems': len(arrays),
           'corruptionSignatures': suspicious,
           'lowGujaratiRatio': low_ratio,
           'verdict': verdict}
    outp = os.path.join(ROOT, '.android-shield/l3-gu-corruption-result.json')
    with open(outp, 'w', encoding='utf-8') as f:
        json.dump(res, f, ensure_ascii=False, indent=1)
    print("WROTE", outp)

if __name__ == '__main__':
    main()
