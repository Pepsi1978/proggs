import os, json

DATA = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/ptbr-data.json')
with open(DATA, 'r', encoding='utf-8') as f:
    raw = f.read()

BS = chr(92)   # backslash
Q = chr(34)    # double quote

# Current broken sequence in file: backslash backslash quote  (\\")
# JSON sees \\ -> one backslash, then " -> string terminator. Broken.
# We WANT the decoded Python string to contain: backslash quote (\")  -> Android-escaped quote in XML.
# To encode  \"  in JSON we need:  \\\"  (\\ -> \, \" -> ")  = backslash backslash backslash quote.
broken = BS + BS + Q          # \\"  (3 chars)
fixed = BS + BS + BS + Q      # \\\" (4 chars)

count = raw.count(broken)
new = raw.replace(broken, fixed)

with open(DATA, 'w', encoding='utf-8', newline='\n') as f:
    f.write(new)

# Validate
try:
    d = json.loads(new)
    print('FIXED', count, 'occurrences. JSON now valid. strings=' + str(len(d['strings'])) + ' plurals=' + str(len(d['plurals'])))
    # spot-check one value decodes with backslash-quote
    sample = d['strings']['ai_prompt_custom_intro']
    needle = BS + Q + 'Metas' + BS + Q
    print('Sample contains  \\"Metas\\"  (backslash-quote form)?:', needle in sample)
    print('Sample raw-newline check (should be False):', '\n' in sample)
except json.JSONDecodeError as e:
    print('STILL BROKEN at line', e.lineno, 'col', e.colno)
    s = max(0, e.pos - 50); en = min(len(new), e.pos + 50)
    print('CTX:', repr(new[s:en]))
