# Cross-lingual machine check (FIN-045 + FIN-049 + FIN-044) for all 26 locales.
# Written via Write tool (NOT heredoc) to avoid backslash mangling.
import json, os, re, glob
import xml.etree.ElementTree as ET

app = os.path.expanduser("~/proggs/BestJournalAndroid")
res = os.path.join(app, "app/src/main/res")
shield = os.path.join(app, ".android-shield")


def parse_strings(path):
    """Returns (strings_dict, plurals_dict, arrays_dict, parse_error)."""
    try:
        tree = ET.parse(path)
    except ET.ParseError as e:
        return {}, {}, {}, str(e)
    root = tree.getroot()
    strings, plurals, arrays = {}, {}, {}
    for el in root:
        name = el.get("name")
        if el.tag == "string":
            if el.get("translatable") == "false":
                continue
            strings[name] = "".join(el.itertext())
        elif el.tag == "plurals":
            plurals[name] = {i.get("quantity"): "".join(i.itertext()) for i in el}
        elif el.tag == "string-array":
            arrays[name] = ["".join(i.itertext()) for i in el]
    return strings, plurals, arrays, None


FMT = re.compile(r"%\d+\$[sdf]|%[sdf]|%%")


def naked_percent(val):
    return FMT.sub("", val).count("%")


def fmt_args(val):
    return sorted(re.findall(r"%\d+\$[sdf]|%[sdf]", val.replace("%%", "")))


de_s, de_p, de_a, _ = parse_strings(os.path.join(res, "values/strings.xml"))
de_keys = set(de_s.keys())

locales = sorted(
    os.path.basename(d).replace("values-", "")
    for d in glob.glob(os.path.join(res, "values-*"))
    if os.path.isdir(d)
)
matrix = {}
for loc in locales:
    p = os.path.join(res, "values-" + loc, "strings.xml")
    if not os.path.exists(p):
        matrix[loc] = {"error": "strings.xml fehlt"}
        continue
    s, pl, ar, err = parse_strings(p)
    if err:
        matrix[loc] = {"error": "XML-Parse-Fehler: " + err}
        continue
    keys = set(s.keys())
    missing = sorted(de_keys - keys)
    extra = sorted(keys - de_keys)
    identical = sorted(
        k
        for k in (keys & de_keys)
        if s[k].strip() == de_s[k].strip()
        and len(de_s[k].strip()) > 3
        and not de_s[k].strip().startswith(("%", "#"))
    )
    fmt_mismatch = []
    naked = []
    for k in keys & de_keys:
        if fmt_args(s[k]) != fmt_args(de_s[k]):
            fmt_mismatch.append({"key": k, "de": fmt_args(de_s[k]), "loc": fmt_args(s[k])})
        if naked_percent(s[k]) > 0 and naked_percent(de_s[k]) == 0 and fmt_args(de_s[k]):
            naked.append(k)
    # Unescaped apostrophes outside quoted segments and comments (build blocker check).
    raw = open(p, encoding="utf-8").read()
    no_comments = re.sub(r"<!--.*?-->", "", raw, flags=re.S)
    no_quoted = re.sub(r'"[^"]*"', "", no_comments)
    unesc = len(re.findall(r"(?<!\\)'", no_quoted))
    miss_pl = sorted(set(de_p.keys()) - set(pl.keys()))
    miss_ar = sorted(set(de_a.keys()) - set(ar.keys()))
    matrix[loc] = {
        "keyCount": len(keys),
        "deKeyCount": len(de_keys),
        "missingKeys": missing,
        "missingCount": len(missing),
        "extraKeys": extra[:20],
        "extraCount": len(extra),
        "identicalToDe": identical,
        "identicalCount": len(identical),
        "formatArgMismatch": fmt_mismatch,
        "nakedPercentKeys": naked,
        "unescapedApostrophes": unesc,
        "missingPlurals": miss_pl,
        "missingArrays": miss_ar,
    }

out = {
    "deTranslatableKeys": len(de_keys),
    "dePlurals": len(de_p),
    "deArrays": len(de_a),
    "locales": matrix,
}
tmp = os.path.join(shield, "cross-lingual-matrix-2026-06-10.json.tmp")
fin = os.path.join(shield, "cross-lingual-matrix-2026-06-10.json")
with open(tmp, "w", encoding="utf-8", newline="\n") as f:
    json.dump(out, f, indent=2, ensure_ascii=False)
os.replace(tmp, fin)

print(f"DE translatable: {len(de_keys)} Keys, {len(de_p)} plurals, {len(de_a)} arrays")
header = f"{'Locale':<10} {'Keys':>5} {'Fehlt':>5} {'==DE':>5} {'FmtMis':>6} {'Naked%':>6} {'Apos':>5}"
print(header)
issues = 0
for loc, m in matrix.items():
    if "error" in m:
        print(f"{loc:<10} ERROR: {m['error']}")
        issues += 1
        continue
    flag = ""
    if m["missingCount"] or m["identicalCount"] or m["formatArgMismatch"] or m["nakedPercentKeys"]:
        flag = " <<<"
        issues += 1
    print(
        f"{loc:<10} {m['keyCount']:>5} {m['missingCount']:>5} {m['identicalCount']:>5}"
        f" {len(m['formatArgMismatch']):>6} {len(m['nakedPercentKeys']):>6}"
        f" {m['unescapedApostrophes']:>5}{flag}"
    )
print(f"\nLocales mit Auffaelligkeiten: {issues}/{len(matrix)}")
