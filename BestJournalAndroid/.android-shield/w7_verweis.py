import os, re, html
base = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/assets/legal')

# samples: short ones (fr, ur, zh-CN, gu) + the two suspicious "full?" ones (ja, pt-BR)
samples = ['fr', 'ja', 'ur', 'pt-BR', 'zh-CN', 'gu']

def textof(p):
    t = open(p, encoding='utf-8').read()
    # strip tags for plain-text scan but keep hrefs separately
    return t

for loc in samples:
    fp = os.path.join(base, loc, 'PRIVACY.html')
    if not os.path.exists(fp):
        print("=== %s : MISSING ===" % loc); continue
    raw = textof(fp)
    size = os.path.getsize(fp)
    # find links
    hrefs = re.findall(r'href="([^"]+)"', raw)
    # find references to de/en/ko or "full version" cues across languages
    cues = []
    for kw in ['vollständ', 'voll', 'full', 'complete', 'verbindlich', 'maßgeb', 'prevail',
               'authoritative', 'German', 'English', 'Korean', 'Deutsch', 'Englisch',
               'deutsch', 'anglais', 'allemand', 'coréen', '完全', '正式', '독일어', '영어',
               'Widerspruch', 'conflict', 'discrepan', 'divergenc', 'em caso', 'en cas',
               'complète', 'completa', 'versão completa', 'version complète']:
        if kw.lower() in raw.lower():
            cues.append(kw)
    # detect language of the reference clause: look for a sentence with 'full'/'voll' near a link
    print("=== %s (PRIVACY.html, %d bytes) ===" % (loc, size))
    print("  hrefs:", hrefs[:8])
    print("  cue-keywords present:", cues)
    # extract first ~400 visible chars (tags stripped) to judge language
    plain = re.sub(r'<[^>]+>', ' ', raw)
    plain = html.unescape(re.sub(r'\s+', ' ', plain)).strip()
    print("  head:", plain[:240])
    print("  tail:", plain[-240:])
    print()
