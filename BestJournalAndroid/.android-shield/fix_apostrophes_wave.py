# Find and escape unescaped apostrophes in string VALUES (en/nl/uk) - aapt build blockers.
# Only lines whose value contains an unescaped ' and is not fully double-quoted.
import os, re

app = os.path.expanduser("~/proggs/BestJournalAndroid")

for loc in ("en", "nl", "uk"):
    p = os.path.join(app, "app/src/main/res/values-" + loc, "strings.xml")
    raw = open(p, encoding="utf-8").read()
    lines = raw.split("\n")
    fixed = 0
    report = []
    for i, line in enumerate(lines):
        m = re.match(r'^(\s*<(?:string|item)\b[^>]*>)(.*)(</(?:string|item)>\s*)$', line)
        if not m:
            continue
        head, val, tail = m.groups()
        # Skip values fully wrapped in double quotes (Android allows ' inside "...").
        if val.startswith('"') and val.endswith('"'):
            continue
        if re.search(r"(?<!\\)'", val):
            newval = re.sub(r"(?<!\\)'", lambda mm: "\\'", val)
            lines[i] = head + newval + tail
            fixed += 1
            key = re.search(r'name="([^"]+)"', head)
            report.append((key.group(1) if key else "?", val[:60]))
    if fixed:
        out = "\n".join(lines)
        tmp = p + ".tmp"
        with open(tmp, "w", encoding="utf-8", newline="\n") as f:
            f.write(out)
        os.replace(tmp, p)
    print(f"{loc}: {fixed} Zeilen escaped")
    for k, v in report[:25]:
        print(f"   {k}: {v}")

# Validate all three after fix
import xml.etree.ElementTree as ET
for loc in ("en", "nl", "uk"):
    p = os.path.join(app, "app/src/main/res/values-" + loc, "strings.xml")
    ET.parse(p)
    raw = open(p, encoding="utf-8").read()
    no_c = re.sub(r"<!--.*?-->", "", raw, flags=re.S)
    vals = re.findall(r'<(?:string|item)\b[^>]*>(.*?)</(?:string|item)>', no_c, flags=re.S)
    bad = sum(1 for v in vals if not (v.startswith('"') and v.endswith('"')) and re.search(r"(?<!\\)'", v))
    print(f"{loc}: XML valide, verbleibende unescapte Apostrophe in Werten: {bad}")
