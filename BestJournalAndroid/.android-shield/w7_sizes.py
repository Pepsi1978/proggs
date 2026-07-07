import os, json
base = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/assets/legal')
matrix = {}
allfiles = []
for root, dirs, files in os.walk(base):
    rel = os.path.relpath(root, base)
    if rel == '.':
        continue
    loc = rel.replace(os.sep, '/')
    if '/' in loc:
        continue
    matrix.setdefault(loc, {})
    for fn in files:
        sz = os.path.getsize(os.path.join(root, fn))
        matrix[loc][fn] = sz
        allfiles.append((loc, fn, sz))
print("TOTAL FILES:", len(allfiles))
print("LOCALES:", len(matrix))
for loc in sorted(matrix):
    files = matrix[loc]
    tot = sum(files.values())
    parts = ", ".join("%s=%d" % (k, v) for k, v in sorted(files.items()))
    print("%-8s total=%6d | %s" % (loc, tot, parts))
# dump matrix for output reuse
json.dump(matrix, open(os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/w7_sizematrix.json'), 'w', encoding='utf-8'), ensure_ascii=False)
