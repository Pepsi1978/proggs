# -*- coding: utf-8 -*-
import json, os, random, re
import xml.etree.ElementTree as ET

base = os.path.expanduser('~/proggs/BestJournalAndroid')
res = os.path.join(base, 'app/src/main/res')
shield = os.path.join(base, '.android-shield')

with open(os.path.join(shield, 'legal-keys-2026-06-10.json'), 'r', encoding='utf-8') as f:
    legal_raw = json.load(f)['legalCriticalKeys']
legal_set = set()
for entry in legal_raw:
    for p in entry.split('/'):
        legal_set.add(p.strip())

def parse_strings(path):
    if not os.path.exists(path): return {}
    out = {}
    tree = ET.parse(path); root = tree.getroot()
    for el in root:
        if el.tag != 'string': continue
        name = el.get('name')
        if name is None: continue
        inner = el.text or ''
        for child in el:
            tag_local = child.tag.split('}')[-1]
            attrs = ''.join(f' {k.split("}")[-1]}="{v}"' for k, v in child.attrib.items())
            inner += f'<{tag_local}{attrs}>{child.text or ""}</{tag_local}>'
            if child.tail: inner += child.tail
        out[name] = inner
    return out

de = parse_strings(os.path.join(res, 'values', 'strings.xml'))

# Exclude legal keys, json_key/value structural keys, dev_seed, ai_prompt
def is_sampleable(k):
    if k in legal_set: return False
    if k.startswith('json_key') or k.startswith('json_value'): return False
    if k.startswith('dev_seed'): return False
    if k.startswith('ai_prompt'): return False
    if k.startswith('settings_about_version'): return False
    return True

# Hindi (Devanagari) range to detect Hindi-in-Urdu mix
DEVANAGARI = re.compile(r'[ऀ-ॿ]')
# Latin word detection (3+ consecutive latin letters) — to find untranslated German leakage
LATIN_WORD = re.compile(r'[A-Za-zÄÖÜäöüß]{4,}')
# Known acceptable latin brand/tech tokens
ALLOW_LATIN = {'Best','Journal','Google','Play','Drive','Premium','Firebase','Gemini','Groq',
               'Microsoft','Bing','Speech','Whisper','Large','Turbo','PDF','Analytics','Data',
               'Privacy','Framework','BGB','CCPA','CPRA','DSGVO','GDPR','EU','US','USA','ABD',
               'KI','YZ','AI','TTS','Lifetime','Inc','SCCs','Settings','Strava','Video','Kamera',
               'whisper','large','turbo','EU-US','Md','Art','Standard','Contractual','Clauses','AB-ABD',
               'KVKK','Mountain','View','Whoop','Oura','Polar','Apple','Health'}

for lang in ['ar', 'ur', 'tr']:
    loc = parse_strings(os.path.join(res, f'values-{lang}', 'strings.xml'))
    candidates = [k for k in loc if is_sampleable(k) and de.get(k) is not None
                  and len(loc[k]) > 15]  # skip very short
    random.seed(42 + ord(lang[0]))  # deterministic per lang
    sample = random.sample(candidates, min(15, len(candidates)))
    print(f'\n##### {lang} SAMPLE ({len(sample)} of {len(candidates)} candidates) #####')
    for k in sorted(sample):
        v = loc[k]
        flags = []
        if lang == 'ur' and DEVANAGARI.search(v):
            flags.append('HINDI-DEVANAGARI-DETECTED')
        # latin leakage: latin words not in allowlist
        latin_words = [w for w in LATIN_WORD.findall(v) if w not in ALLOW_LATIN]
        if latin_words:
            flags.append(f'LATIN-WORDS:{latin_words}')
        flagstr = ('  <<' + ' | '.join(flags) + '>>') if flags else ''
        print(f'[{k}]{flagstr}')
        print(f'  DE : {de[k][:130]}')
        print(f'  {lang.upper()}: {v[:160]}')
