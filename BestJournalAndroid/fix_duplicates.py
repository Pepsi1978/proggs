"""Find and remove duplicate <string name="..."> entries in all values-XX folders.
Keeps the FIRST occurrence (matches default values/strings.xml semantics)."""
import os, re, glob, tempfile

BASE = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
files = sorted(glob.glob(os.path.join(BASE, "values-*", "strings.xml")))

PAT = re.compile(r'<string\s+name="([^"]+)"[^>]*>.*?</string>', re.DOTALL)

total_fixed = 0
for path in files:
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    seen = set()
    duplicates = []
    for m in PAT.finditer(content):
        name = m.group(1)
        if name in seen:
            duplicates.append((name, m.start(), m.end()))
        else:
            seen.add(name)
    if not duplicates:
        continue
    # Remove from end to start so positions stay valid
    new_content = content
    for name, start, end in reversed(duplicates):
        # Also eat the trailing newline if present
        e = end
        if e < len(new_content) and new_content[e] == "\n":
            e += 1
        # And leading whitespace on that line
        s = start
        while s > 0 and new_content[s - 1] in " \t":
            s -= 1
        new_content = new_content[:s] + new_content[e:]
    # Atomic write
    d = os.path.dirname(path)
    with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp", delete=False, encoding="utf-8") as tmp:
        tmp.write(new_content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)
    locale = os.path.basename(d)
    print(f"{locale}: removed {len(duplicates)} duplicates ({', '.join(n for n, _, _ in duplicates[:5])}{'...' if len(duplicates) > 5 else ''})")
    total_fixed += len(duplicates)

print(f"\nTotal duplicates removed: {total_fixed}")
