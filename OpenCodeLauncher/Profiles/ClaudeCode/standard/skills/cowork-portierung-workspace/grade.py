#!/usr/bin/env python3
"""Objektive Verifikation der portierten Cowork-Skills (cowork-portierung Test-Loop).

Prueft jeden Output (skills-src/<name>/SKILL.md + <name>.zip) gegen die Cowork-Assertions
und schreibt pro Run eine grading.json (Viewer-Schema: text/passed/evidence).
"""
import json
import os
import re
import sys
import zipfile

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

ITER = os.path.join(os.path.expanduser("~"), ".claude", "skills",
                    "cowork-portierung-workspace", "iteration-1")

RUNS = [
    ("eval-0-sound-search", "sound-search"),
    ("eval-1-undo-changes", "undo-changes"),
    ("eval-2-hook-forge", "hook-forge"),
]
CONFIGS = ["with_skill", "without_skill"]


def desc_value(skill_md_text):
    """Liefert (string, einzeilig?) der description aus dem Frontmatter."""
    m = re.search(r'(?m)^description:\s*(.*)$', skill_md_text)
    if not m:
        return None, False
    raw = m.group(1).strip()
    one_line = raw.startswith('"') and raw.rstrip().endswith('"')
    val = raw.strip('"') if one_line else raw
    return val, one_line


def grade_run(eval_dir, name):
    out = os.path.join(ITER, eval_dir)
    results = {}
    for cfg in CONFIGS:
        base = os.path.join(out, cfg, "outputs")
        skill_md = os.path.join(base, "skills-src", name, "SKILL.md")
        zip_path = os.path.join(base, f"{name}.zip")
        checks = []

        def add(text, passed, evidence):
            checks.append({"text": text, "passed": bool(passed), "evidence": str(evidence)})

        if not os.path.isfile(skill_md):
            add("SKILL.md existiert", False, f"fehlt: {skill_md}")
            results[cfg] = {"expectations": checks}
            continue
        with open(skill_md, "r", encoding="utf-8") as fh:
            text = fh.read()

        add("SKILL.md existiert", True, skill_md)

        val, one_line = desc_value(text)
        add("description einzeilig in Quotes", one_line,
            "ja" if one_line else f"mehrzeilig/keine Quotes: {repr((val or '')[:40])}")
        dlen = len(val) if val else 999
        add("description <= 200 Zeichen", val is not None and dlen <= 200, f"{dlen} Zeichen")
        add("description ohne spitze Klammern", val is not None and "<" not in val and ">" not in val,
            "ok" if val and "<" not in val and ">" not in val else "enthaelt <...>")
        add("description ohne URL", val is not None and "http" not in (val or "").lower(),
            "ok" if val and "http" not in val.lower() else "enthaelt URL")

        add("Titel '(Cowork-Fassung)'", "(Cowork-Fassung)" in text,
            "vorhanden" if "(Cowork-Fassung)" in text else "fehlt")
        has0 = re.search(r'(?m)^##\s*0\.', text) is not None
        has0a = re.search(r'(?m)^##\s*0a\.', text) is not None
        add("Block ## 0. (Ablage relativ)", has0, "vorhanden" if has0 else "fehlt")
        add("Block ## 0a. (Mount/Git-Fallen)", has0a, "vorhanden" if has0a else "fehlt")

        # Echter fester Pfad = ~/proggs/<x> MIT Slash; cowork-git.sh ist die erlaubte Ausnahme.
        # Blosse Erwaehnungen (`~/proggs` ohne folgenden /pfad, z.B. "nicht in ~/proggs schreiben")
        # sind erklaerende Prosa und kein echter Pfad.
        proggs_paths = re.findall(r'~/proggs/([A-Za-z0-9_.\-/]+)', text)
        bad_proggs = [p for p in proggs_paths if not p.startswith("cowork-git.sh")]
        add("keine festen ~/proggs-Pfade (ausser cowork-git.sh)", not bad_proggs,
            "ok" if not bad_proggs else f"{len(bad_proggs)} echte Pfade: {bad_proggs[:3]}")

        add("Sichern via cowork-git.sh", "cowork-git.sh" in text,
            "vorhanden" if "cowork-git.sh" in text else "fehlt")

        # ZIP
        if not os.path.isfile(zip_path):
            add("ZIP existiert", False, f"fehlt: {zip_path}")
        else:
            add("ZIP existiert", True, zip_path)
            with zipfile.ZipFile(zip_path) as zf:
                names = zf.namelist()
            add("ZIP-Wurzel = <name>/, SKILL.md enthalten", f"{name}/SKILL.md" in names,
                "ok" if f"{name}/SKILL.md" in names else f"namelist={names[:3]}")
            add("ZIP forward-slash (kein Backslash)", all("\\" not in n for n in names),
                "ok" if all("\\" not in n for n in names) else "Backslash gefunden")
            if name == "hook-forge":
                refs = [n for n in names if n.startswith(f"{name}/references/")]
                add("references/ im ZIP (hook-forge)", len(refs) >= 1,
                    f"{len(refs)} references-Eintraege: {refs}")

        results[cfg] = {"expectations": checks}
        # grading.json pro Run schreiben
        gpath = os.path.join(out, cfg, "grading.json")
        with open(gpath, "w", encoding="utf-8") as fh:
            json.dump({"expectations": checks}, fh, ensure_ascii=False, indent=2)
    return results


print(f"{'RUN':<22} {'CONFIG':<16} {'PASS':>5}/{'TOTAL':<5}  FAILED")
print("-" * 78)
summary = {}
for eval_dir, name in RUNS:
    res = grade_run(eval_dir, name)
    for cfg in CONFIGS:
        checks = res[cfg]["expectations"]
        passed = sum(1 for c in checks if c["passed"])
        total = len(checks)
        failed = [c["text"] for c in checks if not c["passed"]]
        summary[f"{eval_dir}/{cfg}"] = (passed, total)
        fail_str = "" if not failed else " | ".join(failed)
        print(f"{eval_dir:<22} {cfg:<16} {passed:>5}/{total:<5}  {fail_str}")

print("-" * 78)
all_pass = all(p == t for p, t in summary.values())
print("ALLE CHECKS BESTANDEN" if all_pass else "ES GIBT FEHLSCHLAGENDE CHECKS (siehe oben)")
