#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cortex Update — synchronisiert Repo-Wissensdateien ins zweite Gehirn (Cortex / Qdrant brain-api).

Drei Subcommands (kontext-schonend: Volltexte laufen NIE durch den LLM-Kontext, nur kompakte JSON-Berichte):

  scan   [--focus all|almanache|best-practices|rules|<bereich>]
         Liest die Quell-Ordner per Datei-IO, leitet Titel + Kategorie ab, holt /list vom Brain,
         klassifiziert jede Datei als aktuell|geaendert|neu. Gibt einen JSON-Bericht aus UND schreibt
         ihn nach <state>/scan.json. Aendert NICHTS im Brain.

  upload --plan <plan.json>
         Legt die im Plan stehenden (vom Benutzer bestaetigten) Dateien per POST /store ab.
         Waechter: bei status 'neu' + replaced=true ALARM (Titel-Kollision) -> diese Datei NICHT ablegen.
         Schreibt nach <state>/uploaded.json (fuer verify).

  verify [--plan <plan.json>] [--sample N]
         Stichprobe (die N groessten zuletzt hochgeladenen Dateien): holt den vollen Text per
         /by-title ZURUECK aus dem Brain und vergleicht ihn 1:1 mit der Originaldatei (faengt
         Truncation / kaputten Chunk-Zusammenbau bei grossen Dateien). Prueft zusaetzlich Kategorie
         und semantische Auffindbarkeit (/search).

Die Quellen stehen in SOURCES und sind bewusst ERWEITERBAR: eine neue Datenart = ein neuer Eintrag.
"""
import os, re, sys, json, glob, argparse, urllib.request, urllib.parse

try:
    sys.stdout.reconfigure(encoding="utf-8")  # Windows cp1252-Print-Crash bei Umlauten/Pfeilen vermeiden
except Exception:
    pass

# ---------------------------------------------------------------------------
# Konfiguration
# ---------------------------------------------------------------------------
REPO = os.path.expanduser("~/proggs")
API = "http://10.8.0.1:8000"          # brain-api, NUR ueber WireGuard erreichbar
USER_ID = "frank"
KEY_FILE = os.path.expanduser("~/SK/second-brain/brain.env")
# Lauf-Zwischenstand (scan.json/plan.json/uploaded.json) bewusst AUSSERHALB des Skill-Ordners
# und des Repos -> wird nicht mitgespiegelt und nicht committet (geraete-lokaler State).
STATE_DIR = os.path.expanduser("~/.cortex-sync")

# Quellen — ERWEITERBAR. Neue Datenart -> hier einen Eintrag ergaenzen.
#   root            : Ordner relativ zu ~/proggs
#   category_tmpl   : Ziel-Kategorie im Brain. "{label}" wird durch das Bereichs-Label ersetzt
#                     (aus dem Unterordner). Ohne "{label}" = feste Kategorie (alle Dateien gleich).
#   title_mode      : "h1_clean" (bereinigte erste #-Ueberschrift) | "first_line" (erste Zeile 1:1)
#   layout          : "bereich" (root/<bereich>/<datei>.md) | "flach" (root/<datei>.md)
SOURCES = [
    {"name": "Almanache",      "root": "bugs",                          "category_tmpl": "Programmierung/Almanache/{label}",      "title_mode": "h1_clean",   "layout": "bereich"},
    {"name": "Best Practices", "root": "best-practices",                "category_tmpl": "Programmierung/Best Practices/{label}", "title_mode": "h1_clean",   "layout": "bereich"},
    {"name": "Rules",          "root": "opencode-setup/rules-opencode", "category_tmpl": "Programmierung/Rules",                  "title_mode": "first_line", "layout": "flach"},
]

# Ordnername -> exaktes Bereichs-Label im Brain (Sonderschreibweisen). Fallback: Title-Case.
LABELS = {
    "android": "Android", "android-build": "Android Build", "web": "Web", "server": "Server",
    "desktop": "Desktop", "apis": "API", "claude-tooling": "Claude Tooling", "opencode": "Opencode",
    "agents": "Agenten", "peripherie": "Peripherie", "assets": "Assets", "second-brain": "Second Brain",
}

# Meta-/Nicht-Inhalts-Dateien, die NIE ins Brain gehoeren.
SKIP_NAMES = {"readme.md", "system.md"}
# Almanach/BP-Inhaltsdateien heissen immer lowercase-kebab (room.md, jetpack-compose.md).
KEBAB_RE = re.compile(r"^[a-z0-9][a-z0-9.\-]*\.md$")
STOPWORDS = {"der", "die", "das", "und", "fuer", "fur", "mit", "im", "in", "the", "a", "of", "to",
             "best", "practices", "bekannte", "bugs", "almanach", "android", "ios"}


# ---------------------------------------------------------------------------
# Helfer
# ---------------------------------------------------------------------------
def load_key():
    try:
        with open(KEY_FILE, encoding="utf-8") as fh:
            for line in fh:
                if line.startswith("SB_API_KEY="):
                    return line.split("=", 1)[1].strip().strip('"').strip("'")
    except FileNotFoundError:
        die(f"Key-Datei fehlt: {KEY_FILE} (WireGuard-Tunnel aktiv? brain.env vorhanden?)")
    die("SB_API_KEY nicht in brain.env gefunden")


def http(method, path, key, payload=None, timeout=60):
    url = f"{API}{path}"
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    headers = {"Authorization": f"Bearer {key}"}
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read().decode("utf-8"))


def die(msg):
    print(f"FEHLER: {msg}", file=sys.stderr)
    raise SystemExit(2)


def write_json(path, obj):
    with open(path, "w", encoding="utf-8", newline="\n") as fh:   # newline='\n' -> kein CRLF auf Windows
        json.dump(obj, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def label_for(subdir):
    return LABELS.get(subdir, subdir.replace("-", " ").title())


def derive_title(text, mode):
    if mode == "first_line":
        for line in text.splitlines():
            if line.strip():
                return re.sub(r"^#+\s*", "", line).strip()
        return ""
    # h1_clean: erste #-Ueberschrift, gaengige Suffixe/Praefixe entfernen
    h1 = ""
    for line in text.splitlines():
        if line.startswith("# "):
            h1 = line
            break
    t = re.sub(r"^#\s+", "", h1).strip()
    t = re.split(r"\s+[—-]\s+Best Practices", t)[0]
    t = re.split(r"\s+\(Best Practices", t)[0]
    t = re.sub(r"^Best Practices:\s*", "", t)
    t = re.sub(r"^Bekannte Bugs(\s*&\s*Fallen)?:\s*", "", t)
    t = re.sub(r"^Bug-Almanach:\s*", "", t)
    t = re.sub(r"\s+[—-]\s+(Bug-)?Almanach\s*$", "", t)
    t = re.sub(r"\s*\(Stand[^)]*\)\s*$", "", t)
    return t.strip()


def tokenize(s):
    return {w for w in re.split(r"[^a-z0-9]+", s.lower()) if len(w) > 2 and w not in STOPWORDS}


def best_fuzzy(file_tokens, candidates):
    """Brain-Kandidat mit dem groessten Token-Overlap (>=1). candidates: [{title, chars, tokens}]."""
    best, score = None, 0
    for c in candidates:
        ov = len(file_tokens & c["tokens"])
        if ov > score:
            best, score = c, ov
    return best if score >= 1 else None


def collect_files(focus):
    """Liest alle (gefilterten) Quell-Dateien per Datei-IO. Volltexte bleiben im Skript, nicht im Bericht."""
    out = []
    for src in SOURCES:
        sname = src["name"].lower()
        root_abs = os.path.join(REPO, src["root"])
        if not os.path.isdir(root_abs):
            continue
        pattern = os.path.join(root_abs, "*", "*.md") if src["layout"] == "bereich" else os.path.join(root_abs, "*.md")
        for f in sorted(glob.glob(pattern)):
            name = os.path.basename(f).lower()
            if name in SKIP_NAMES or name.startswith("_"):
                continue
            if src["layout"] == "bereich":
                subdir = os.path.basename(os.path.dirname(f))
                if not KEBAB_RE.match(name):       # Meta-Dateien (README, SYSTEM, OFFENE-...) ausschliessen
                    continue
                category = src["category_tmpl"].format(label=label_for(subdir))
            else:
                subdir = ""
                category = src["category_tmpl"]
            # focus-Filter: "all" ODER Quelle (almanache/best practices/rules) ODER Bereich (android/web/...)
            if focus not in ("all", sname, src["root"], subdir):
                continue
            with open(f, encoding="utf-8") as fh:
                text = fh.read()
            title = derive_title(text, src["title_mode"])
            out.append({
                "rel": os.path.relpath(f, REPO).replace("\\", "/"),
                "source": src["name"], "category": category,
                # Der Server speichert den Text strip()-getrimmt -> chars zaehlt OHNE
                # fuehrende/abschliessende Leerzeichen. Fuer den exakten Fingerabdruck-Vergleich
                # hier dieselbe Normalisierung anwenden (sonst Off-by-one bei der finalen Newline).
                "title": title, "size": len(text.strip()), "tokens": tokenize(title + " " + name),
            })
    return out


def fetch_brain(key):
    """Holt /list und gruppiert die fuer uns relevanten Eintraege nach Kategorie."""
    res = http("GET", f"/list?{urllib.parse.urlencode({'user_id': USER_ID, 'limit': 2000})}", key)
    items = res.get("items") or res.get("memories") or []
    by_cat = {}
    for it in items:
        cat = it.get("category") or ""
        if not (cat.startswith(("Programmierung/Almanache/", "Programmierung/Best Practices/")) or cat == "Programmierung/Rules"):
            continue
        chars = it.get("chars")
        if chars is None:
            chars = it.get("text_len") or 0
        title = it.get("title") or ""
        by_cat.setdefault(cat, []).append({"title": title, "chars": chars, "tokens": tokenize(title)})
    return by_cat


def classify(fi, by_cat):
    """Sichere Zuordnung: Auto-'geaendert' NUR bei eindeutiger Zuordnung (exakter Titel oder
    eindeutige Groesse). Fuzzy ist NUR ein unverbindlicher Hinweis bei 'neu' — niemals ein
    automatisches Ueberschreiben, sonst droht stiller Datenverlust bei Fehlzuordnung.

    chars-Toleranz +-1: der Server speichert teils getrimmt (chars = gestrippte Laenge), teils
    inkl. finaler Newline (chars = +1) -- diese 1-Zeichen-Differenz ist KEINE echte Aenderung.
    """
    cands = by_cat.get(fi["category"], [])
    dtl = fi["title"].strip().lower()
    # 1. exakter Titel-Match (zuverlaessig v.a. fuer Rules: erste Zeile == Brain-Titel)
    exact = [b for b in cands if dtl and b["title"].strip().lower() == dtl]
    if exact:
        b = exact[0]
        return {"status": "aktuell" if abs(b["chars"] - fi["size"]) <= 1 else "geaendert", "brain_title": b["title"]}
    # 2. eindeutige Groessen-Uebereinstimmung (+-1) -> unveraendert (kuratierter/abweichender Titel)
    near = [b for b in cands if abs(b["chars"] - fi["size"]) <= 1]
    if len(near) == 1:
        return {"status": "aktuell", "brain_title": near[0]["title"], "note": "per Groesse zugeordnet (Titel weicht ab)"}
    if len(near) > 1:
        b = best_fuzzy(fi["tokens"], near) or near[0]
        return {"status": "aktuell", "brain_title": b["title"], "note": "mehrere gleich gross — geprueft"}
    # 3. nichts Eindeutiges -> NEU (sicher). Fuzzy nur als Hinweis, falls es doch ein Update ist.
    res = {"status": "neu", "suggested_title": fi["title"]}
    hint = best_fuzzy(fi["tokens"], cands)
    if hint:
        res["note"] = f"aehnelt bestehendem '{hint['title']}' — falls Update, Titel darauf setzen"
    return res


# ---------------------------------------------------------------------------
# Subcommands
# ---------------------------------------------------------------------------
def cmd_scan(args):
    os.makedirs(STATE_DIR, exist_ok=True)
    key = load_key()
    files = collect_files(args.focus)
    by_cat = fetch_brain(key)
    matched = set()
    rows = []
    for fi in files:
        c = classify(fi, by_cat)
        bt = c.get("brain_title")
        if bt:
            matched.add((fi["category"], bt.strip().lower()))
        rows.append({
            "rel": fi["rel"], "source": fi["source"], "category": fi["category"],
            "size": fi["size"], "status": c["status"],
            "title": bt or c.get("suggested_title") or fi["title"],
            "note": c.get("note", ""),
        })
    orphans = []
    for cat, entries in by_cat.items():
        for e in entries:
            if (cat, e["title"].strip().lower()) not in matched:
                orphans.append({"category": cat, "title": e["title"], "chars": e["chars"]})

    report = {
        "neu":       [r for r in rows if r["status"] == "neu"],
        "geaendert": [r for r in rows if r["status"] == "geaendert"],
        "aktuell":   [r for r in rows if r["status"] == "aktuell"],
        "orphans":   orphans,
        "total_files": len(files),
    }
    write_json(os.path.join(STATE_DIR, "scan.json"), report)

    print(f"Cortex-Scan: {len(files)} Dateien geprueft "
          f"(neu={len(report['neu'])}, geaendert={len(report['geaendert'])}, "
          f"aktuell={len(report['aktuell'])}, verwaist im Brain={len(orphans)})\n")
    for label in ("neu", "geaendert"):
        if report[label]:
            print(f"=== {label.upper()} ({len(report[label])}) ===")
            for r in report[label]:
                note = f"   [{r['note']}]" if r["note"] else ""
                print(f"  [{r['source'][:4]:4}] {r['rel']}")
                print(f"        Titel:     {r['title']}")
                print(f"        Kategorie: {r['category']}  ({r['size']} Zeichen){note}")
            print()
    if orphans:
        print(f"=== VERWAIST ({len(orphans)}) — im Brain, keiner Datei zugeordnet (evtl. umbenannt/geloescht) ===")
        for o in orphans:
            print(f"  {o['title']}  [{o['category']}]  ({o['chars']} Zeichen)")
        print()
    print(f"Bericht: {os.path.join(STATE_DIR, 'scan.json')}")
    return 0


def cmd_plan(args):
    """Baut aus scan.json eine plan.json (Eintraege mit rel/title/category/status). Spart das
    Abtippen vieler JSON-Objekte. Claude korrigiert danach nur einzelne Titel/Kategorien."""
    with open(os.path.join(STATE_DIR, "scan.json"), encoding="utf-8") as fh:
        r = json.load(fh)
    want = {s.strip() for s in args.status.split(",")}
    entries = []
    for grp in ("neu", "geaendert"):
        if grp not in want:
            continue
        for x in r.get(grp, []):
            if args.only and args.only not in x["rel"]:
                continue
            if args.exclude and args.exclude in x["rel"]:
                continue
            entries.append({"rel": x["rel"], "title": x["title"], "category": x["category"], "status": x["status"]})
    out = os.path.join(STATE_DIR, "plan.json")
    write_json(out, entries)
    print(f"Plan: {len(entries)} Eintraege -> {out}\n")
    for e in entries[:60]:
        print(f"  [{e['status']:9}] {e['rel']}  ->  {e['title']}")
    if len(entries) > 60:
        print(f"  ... (+{len(entries) - 60} weitere)")
    return 0


def cmd_upload(args):
    key = load_key()
    with open(args.plan, encoding="utf-8") as fh:
        plan = json.load(fh)
    entries = plan if isinstance(plan, list) else plan.get("entries", [])
    os.makedirs(STATE_DIR, exist_ok=True)
    done, alarm, fail = [], 0, 0
    for e in entries:
        path = os.path.join(REPO, e["rel"])
        try:
            with open(path, encoding="utf-8") as fh:
                text = fh.read().strip()   # 1:1 zur Server-Normalisierung -> chars stimmt sofort
            res = http("POST", "/store", key, {
                "text": text, "title": e["title"], "category": e["category"], "user_id": USER_ID,
            })
            replaced = res.get("replaced")
            if e.get("status") == "neu" and replaced:
                alarm += 1
                print(f"!! ALARM replaced=true bei NEU '{e['title']}' ({e['rel']}) — Titel kollidiert "
                      f"mit bestehendem Eintrag. NICHT als neu abgelegt; Titel anpassen.")
                continue
            done.append({"rel": e["rel"], "title": e["title"], "category": e["category"],
                         "size": len(text), "chars": res.get("chars"), "replaced": replaced})
            print(f"OK  {e['rel']}  ->  '{e['title']}'  ({res.get('chars')} Zeichen, "
                  f"{'ueberschrieben' if replaced else 'neu'})")
        except Exception as ex:
            fail += 1
            print(f"FEHLER bei {e['rel']}: {type(ex).__name__}: {ex}")
    write_json(os.path.join(STATE_DIR, "uploaded.json"), done)
    print(f"\nAblage fertig: {len(done)} gespeichert, {fail} Fehler, {alarm} Kollisions-Alarm.")
    return 0 if alarm == 0 and fail == 0 else 1


def cmd_verify(args):
    key = load_key()
    if args.plan:
        with open(args.plan, encoding="utf-8") as fh:
            data = json.load(fh)
        entries = data if isinstance(data, list) else data.get("entries", [])
    else:
        with open(os.path.join(STATE_DIR, "uploaded.json"), encoding="utf-8") as fh:
            entries = json.load(fh)
    if not entries:
        print("Nichts zu verifizieren (keine hochgeladenen Eintraege gefunden).")
        return 0
    entries = sorted(entries, key=lambda e: e.get("size", 0), reverse=True)   # groesste zuerst (Truncation-Risiko)
    sample = entries[: max(1, args.sample)]
    print(f"Vollstaendigkeits-Stichprobe ({len(sample)} groesste von {len(entries)} Dateien):\n")
    print(f"{'Datei':46} {'orig':>8} {'im Brain':>9} {'1:1':>5} {'Kat':>4} {'Vektor':>7}")
    print("-" * 86)
    all_ok = True
    for e in sample:
        with open(os.path.join(REPO, e["rel"]), encoding="utf-8") as fh:
            text = fh.read().strip()   # Server speichert getrimmt -> hier gleich normalisieren
        res = http("GET", "/by-title?" + urllib.parse.urlencode({"title": e["title"], "user_id": USER_ID}), key)
        got = res.get("text", "") or ""
        cats = res.get("categories") or ([res.get("category")] if res.get("category") else [])
        len_ok = len(text) == len(got)
        exact_ok = text == got
        cat_ok = e["category"] in cats
        try:
            s = http("POST", "/search", key, {"query": e["title"], "user_id": USER_ID, "limit": 8})
            hits = s.get("items") or s.get("results") or []
            vec = "ja" if any((h.get("title") == e["title"]) for h in hits) else "n/top8"
        except Exception:
            vec = "err"
        ok = len_ok and exact_ok and cat_ok
        all_ok = all_ok and ok
        eins = "ja" if exact_ok else ("LEN!" if not len_ok else "DIFF")
        mark = "" if ok else "  <-- PRUEFEN"
        print(f"{e['rel'][:46]:46} {len(text):>8} {len(got):>9} {eins:>5} {('ja' if cat_ok else 'NEIN'):>4} {vec:>7}{mark}")
    print("-" * 86)
    print("ALLE STICHPROBEN VOLLSTAENDIG (Text 1:1, Kategorie, auffindbar)" if all_ok
          else "!! ABWEICHUNGEN — siehe PRUEFEN-Zeilen (moegliche Truncation / falsche Kategorie)")
    return 0 if all_ok else 1


def main():
    p = argparse.ArgumentParser(description="Cortex Update — Repo-Wissen ins zweite Gehirn synchronisieren")
    sub = p.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("scan");   s.add_argument("--focus", default="all")
    pl = sub.add_parser("plan");  pl.add_argument("--status", default="neu,geaendert"); pl.add_argument("--only", default=None); pl.add_argument("--exclude", default=None)
    u = sub.add_parser("upload"); u.add_argument("--plan", required=True)
    v = sub.add_parser("verify"); v.add_argument("--plan", default=None); v.add_argument("--sample", type=int, default=2)
    args = p.parse_args()
    return {"scan": cmd_scan, "plan": cmd_plan, "upload": cmd_upload, "verify": cmd_verify}[args.cmd](args)


if __name__ == "__main__":
    raise SystemExit(main())
