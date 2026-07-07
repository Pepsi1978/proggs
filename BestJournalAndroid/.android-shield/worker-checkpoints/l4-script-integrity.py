# -*- coding: utf-8 -*-
"""L4: Schrift-Integritaet ta/te/th ueber ALLE Strings.
   - U+FFFD (replacement char)
   - leere/abgeschnittene Strings
   - Anteil der Zielschrift (<30% verdaechtig, ausser json_key_*/Platzhalter-only)
   - Latin-Wort-Inseln mitten im Text
   Schreibt l4-script-report.json.
"""
import json, os, re
import xml.etree.ElementTree as ET

app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')

# Unicode-Bloecke
RANGES = {
    'ta': (0x0B80, 0x0BFF),  # Tamil
    'te': (0x0C00, 0x0C7F),  # Telugu
    'th': (0x0E00, 0x0E7F),  # Thai
}

def parse(path):
    out = {}
    tree = ET.parse(path)
    for el in tree.getroot():
        if el.tag == 'string' and el.get('name'):
            out[el.get('name')] = ''.join(el.itertext())
    return out

def script_ratio(text, lo, hi):
    """Anteil der Zeichen im Zielblock unter allen 'Buchstaben'-Zeichen
       (ignoriert Ziffern, Leerzeichen, Satzzeichen, Platzhalter)."""
    letters = 0
    target = 0
    for ch in text:
        cp = ord(ch)
        # Nur "Schrift-Zeichen" zaehlen: Zielblock ODER Latin-Buchstaben
        is_target = lo <= cp <= hi
        is_latin = (0x41 <= cp <= 0x5A) or (0x61 <= cp <= 0x7A)
        if is_target or is_latin:
            letters += 1
            if is_target:
                target += 1
    if letters == 0:
        return None, letters, target  # nur Zahlen/Symbole/Platzhalter
    return target / letters, letters, target

# Platzhalter / Format-Specifier entfernen fuer Latin-Insel-Check
PH = re.compile(r'%\d*\$?[sdfeg]|%[sdfeg]|\\n|\\t|&#?\w+;|<[^>]+>')
# Latin-Wort-Insel: >=3 zusammenhaengende Latin-Buchstaben (nach Platzhalter-Strip)
LATIN_WORD = re.compile(r'[A-Za-z]{3,}')

# Bekannte erlaubte Latin-Begriffe (Markennamen / nicht uebersetzbar)
ALLOW_LATIN = {
    'Groq', 'Gemini', 'Google', 'Drive', 'Play', 'Premium', 'AGB', 'PDF',
    'Whoop', 'Oura', 'Polar', 'Strava', 'Fit', 'KI', 'AI', 'OpenAI',
    'Lifetime', 'App', 'Entropie', 'BestJournal', 'Journal', 'TTS',
    'DSGVO', 'GDPR', 'Email', 'E', 'Mail', 'URL', 'ID', 'OK',
}

mylangs = ['ta', 'te', 'th']
report = {}

for lang in mylangs:
    path = os.path.join(app, f'values-{lang}', 'strings.xml')
    data = parse(path)
    lo, hi = RANGES[lang]
    fffd = []
    empties = []
    low_ratio = []   # nicht json_key_/rerank_/value_
    latin_islands = []
    no_script_at_all = []  # gar kein Zielschrift-Zeichen, aber Text vorhanden

    for k, v in data.items():
        if '�' in v:
            fffd.append(k)
        stripped = v.strip()
        if stripped == '':
            empties.append(k)
            continue
        # Platzhalter strippen fuer Analyse
        clean = PH.sub(' ', v)
        ratio, letters, target = script_ratio(clean, lo, hi)

        is_jsonkey = k.startswith('json_key_') or k.startswith('json_value_')
        is_promptheader = k.startswith('ai_prompt_')

        # Latin-Inseln (Woerter die NICHT in ALLOW sind)
        islands = [w for w in LATIN_WORD.findall(clean) if w not in ALLOW_LATIN]
        if islands and not is_jsonkey:
            # Nur melden wenn es eigentlich Zielschrift-Text sein sollte
            # (d.h. der String hat AUCH Zielschrift ODER ist laenger als ein Markenwort)
            if (target > 0) or (len(islands) >= 1 and len(clean.strip()) > 12 and not is_promptheader):
                latin_islands.append({'key': k, 'latinWords': islands[:8], 'value': v[:120]})

        # Niedriger Schrift-Anteil
        if ratio is not None and ratio < 0.30 and not is_jsonkey:
            low_ratio.append({'key': k, 'ratio': round(ratio, 2), 'letters': letters, 'target': target, 'value': v[:120]})
        # Gar keine Zielschrift, aber echter Text (kein json_key, nicht nur Zahlen)
        if target == 0 and letters > 0 and not is_jsonkey:
            no_script_at_all.append({'key': k, 'value': v[:120]})

    report[lang] = {
        'totalStrings': len(data),
        'fffdReplacementChar': fffd,
        'emptyStrings': empties,
        'lowScriptRatio': low_ratio,
        'noTargetScriptButHasText': no_script_at_all,
        'latinIslands': latin_islands,
    }
    print(f"=== {lang} === total={len(data)}")
    print(f"  U+FFFD: {len(fffd)}  empty: {len(empties)}  lowRatio(<30%): {len(low_ratio)}  noScript: {len(no_script_at_all)}  latinIslands: {len(latin_islands)}")

out_path = os.path.join(base, 'worker-checkpoints', 'l4-script-report.json')
tmp = out_path + '.tmp'
with open(tmp, 'w', encoding='utf-8') as f:
    json.dump(report, f, ensure_ascii=False, indent=1)
os.replace(tmp, out_path)
print("\nGeschrieben:", out_path)
