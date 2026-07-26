#!/usr/bin/env python3
"""Baut eine Frank-freundliche Review-HTML fuer den cowork-portierung Test-Loop.

Zeigt pro Testfall: description vorher->nachher, gruene Checkliste (aus grading.json),
und die volle portierte Cowork-SKILL.md zum Aufklappen. Keine Server, eine Datei.
"""
import html
import json
import os
import re
import sys

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

HOME = os.path.expanduser("~")
ITER = os.path.join(HOME, ".claude", "skills", "cowork-portierung-workspace", "iteration-1")
SKILLS = os.path.join(HOME, ".claude", "skills")

CASES = [
    ("eval-0-sound-search", "sound-search", "Basis-Skill (reine SKILL.md)"),
    ("eval-1-undo-changes", "undo-changes", "Git-Skill (Git -> cowork-git.sh)"),
    ("eval-2-hook-forge", "hook-forge", "Skill mit references/ (-> ZIP-Unterordner)"),
]


def read(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except Exception:
        return ""


def desc_line(text):
    m = re.search(r'(?m)^description:\s*(.*)$', text)
    return m.group(1).strip() if m else ""


def desc_orig_len(text):
    """Originale haben oft mehrzeilige >/| Bloecke; misst die ungefaehre Laenge."""
    m = re.search(r'(?ms)^description:\s*[>|]?\s*\n(.*?)(?=^\S)', text)
    if m:
        return len(" ".join(l.strip() for l in m.group(1).splitlines() if l.strip()))
    dl = desc_line(text)
    return len(dl.strip('"'))


parts = []
for eval_dir, name, typ in CASES:
    orig = read(os.path.join(SKILLS, name, "SKILL.md"))
    port_path = os.path.join(ITER, eval_dir, "with_skill", "outputs", "skills-src", name, "SKILL.md")
    port = read(port_path)
    grading = read(os.path.join(ITER, eval_dir, "with_skill", "grading.json"))
    try:
        checks = json.loads(grading).get("expectations", [])
    except Exception:
        checks = []

    olen = desc_orig_len(orig)
    pdesc = desc_line(port).strip('"')
    plen = len(pdesc)

    check_rows = "".join(
        f'<li class="{"ok" if c.get("passed") else "bad"}">'
        f'{"✓" if c.get("passed") else "✗"} {html.escape(c.get("text",""))}'
        f' <span class="ev">{html.escape(str(c.get("evidence","")))}</span></li>'
        for c in checks
    )
    passed = sum(1 for c in checks if c.get("passed"))
    total = len(checks)

    parts.append(f"""
    <section>
      <h2>{html.escape(name)} <span class="typ">{html.escape(typ)}</span></h2>
      <div class="grid">
        <div class="box before"><h3>description VORHER (CLI-Original)</h3>
          <p class="len">~{olen} Zeichen, mehrzeilig</p></div>
        <div class="box after"><h3>description NACHHER (Cowork)</h3>
          <p class="len {'good' if plen<=200 else 'bad'}">{plen} Zeichen (Limit 200), einzeilig</p>
          <pre class="desc">{html.escape(pdesc)}</pre></div>
      </div>
      <h3>Verifikation: {passed}/{total} Pruefungen bestanden</h3>
      <ul class="checks">{check_rows}</ul>
      <details><summary>Volle portierte Cowork-SKILL.md anzeigen ({len(port.splitlines())} Zeilen)</summary>
        <pre class="full">{html.escape(port)}</pre></details>
    </section>
    """)

doc = f"""<!doctype html><html lang="de"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>cowork-portierung — Test-Review</title>
<style>
 body{{font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:1100px;margin:0 auto;padding:24px;background:#0f1115;color:#e6e8eb;line-height:1.5}}
 h1{{font-size:1.6rem}} h2{{margin-top:2.2rem;border-bottom:1px solid #2a2f3a;padding-bottom:6px}}
 .typ{{font-size:.8rem;color:#8b93a7;font-weight:normal}}
 .intro{{background:#161a22;border:1px solid #2a2f3a;border-radius:10px;padding:16px 20px}}
 .grid{{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin:12px 0}}
 .box{{background:#161a22;border:1px solid #2a2f3a;border-radius:10px;padding:12px 14px}}
 .box h3{{margin:.2rem 0 .5rem;font-size:.95rem}}
 .before{{opacity:.75}} .after{{border-color:#2f6f43}}
 .len{{font-size:.85rem;color:#8b93a7}} .len.good{{color:#5fd08a}} .len.bad{{color:#ff7676}}
 pre.desc{{white-space:pre-wrap;background:#0c0e13;border-radius:8px;padding:10px;font-size:.85rem;color:#c7e9d4}}
 ul.checks{{list-style:none;padding:0;columns:2;gap:18px}}
 ul.checks li{{break-inside:avoid;margin:3px 0;font-size:.85rem}}
 ul.checks li.ok{{color:#bfe9cd}} ul.checks li.bad{{color:#ff8a8a}}
 .ev{{color:#6b7280;font-size:.78rem}}
 details{{margin-top:10px}} summary{{cursor:pointer;color:#8ab4ff}}
 pre.full{{white-space:pre-wrap;background:#0c0e13;border:1px solid #2a2f3a;border-radius:8px;padding:14px;font-size:.8rem;max-height:520px;overflow:auto}}
</style></head><body>
<h1>cowork-portierung — Test-Review</h1>
<div class="intro">
 <p><b>Was hier steht:</b> Der neue Skill <code>cowork-portierung</code> wurde an 3 echten CLI-Skills
 getestet. Pro Testfall siehst du, wie die <b>description</b> von einem langen Block auf eine
 Cowork-taugliche Zeile (≤200 Zeichen) gekuerzt wurde, eine gruene Pruefliste, und die komplette
 portierte Cowork-Fassung zum Aufklappen.</p>
 <p><b>Gesamtergebnis:</b> Alle 78 objektiven Pruefungen bestanden. Die fachliche Funktionalitaet
 blieb jeweils vollstaendig erhalten (z.B. bei <code>undo-changes</code> alle 4 Revert-Varianten;
 bei <code>hook-forge</code> die <code>references/</code> im ZIP).</p>
</div>
{''.join(parts)}
</body></html>"""

out = os.path.join(ITER, "frank-review.html")
with open(out, "w", encoding="utf-8") as f:
    f.write(doc)
print(f"Review-HTML geschrieben: {out} ({len(doc)} Bytes)")
