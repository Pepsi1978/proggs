#!/usr/bin/env python3
"""Session-Flaschenhals: misst einen abgeschlossenen Claude-Code-Verlauf (JSONL).

Aufruf:  python analyse.py <sitzung.jsonl | sitzungs-id | --letzte> [--json] [--top N]
Ohne Pfad nimmt --letzte die juengste abgeschlossene Sitzung (>= 15 min unveraendert)
im Projektordner des aktiven Profils. Subagenten-Verlaeufe (<id>/subagents/*.jsonl)
werden mitgezaehlt. Liest nur, schreibt nichts.

Zeit wird in drei Toepfe getrennt, damit Franks Antwortzeit nie dem Agenten angelastet wird:
  werkzeug = tool_use -> tool_result      (Laufzeit von Befehlen, Builds, Uploads)
  modell   = tool_result/Prompt -> naechste Assistant-Zeile (Denken + Schreiben)
  mensch   = Wartezeit auf Frank (AskUserQuestion, neuer Prompt, Berechtigungsdialog)
"""
import hashlib
import json
import os
import re
import sys
import time
from collections import Counter, defaultdict
from datetime import datetime

def _konfig():
    """Aktiver Claude-Code-Profilordner; in Codex/OpenCode fehlt die Variable -> OpenLauncher-Profil."""
    home = os.path.expanduser("~")
    kand = [os.environ.get("CLAUDE_CONFIG_DIR", ""),
            os.path.join(home, "proggs", "OpenLauncher", "Profiles", "ClaudeCode", "minimal"),
            os.path.join(home, "proggs", "OpenLauncher", "Profiles", "ClaudeCodeMac", "minimal"),
            os.path.join(home, ".claude")]
    return next((k for k in kand if k and os.path.isdir(os.path.join(k, "projects"))), kand[-1])


KONFIG = _konfig()
PROJEKTE = os.path.join(KONFIG, "projects")
HIER = os.path.dirname(os.path.abspath(__file__))
SKILL_NAME = "session-flaschenhals"


def alle_verlaeufe():
    """Hauptverlaeufe aller Projektordner des aktiven Profils (keine Subagenten)."""
    if not os.path.isdir(PROJEKTE):
        return []
    return [os.path.join(PROJEKTE, d, f) for d in os.listdir(PROJEKTE)
            if os.path.isdir(os.path.join(PROJEKTE, d))
            for f in os.listdir(os.path.join(PROJEKTE, d)) if f.endswith(".jsonl")]


def massnahmen():
    try:
        with open(os.path.join(HIER, "massnahmen.json"), encoding="utf-8") as f:
            return json.load(f).get("massnahmen", [])
    except Exception as e:  # kaputte oder fehlende Liste darf die Analyse nicht stoppen
        print(f"(Hinweis: massnahmen.json nicht lesbar: {e})", file=sys.stderr)
        return []
MENSCH_WERKZEUGE = {"AskUserQuestion", "ExitPlanMode"}
LANG_S = 60          # Werkzeuglauf ab hier = Kandidat fuer Wartezeit
MODELL_LANG_S = 120  # Modellpause ab hier = Kandidat (z. B. Advisor, langes Denken)
def ts(s):
    try:
        return datetime.fromisoformat(s.replace("Z", "+00:00")).timestamp()
    except Exception:
        return None


def finde_datei(arg):
    if arg and os.path.isfile(arg):
        return arg
    if arg and arg != "--letzte":
        treffer = [p for p in alle_verlaeufe() if os.path.basename(p).startswith(arg.replace(".jsonl", ""))]
        if treffer:
            return max(treffer, key=os.path.getmtime)
        sys.exit(f"Verlauf nicht gefunden: {arg} (gesucht unter {PROJEKTE})")
    return abgeschlossene(1)[0]


def abgeschlossene(n):
    """Die n juengsten abgeschlossenen Hauptsitzungen (>= 15 min unveraendert, > 20 kB)."""
    jetzt = time.time()
    kand = [p for p in alle_verlaeufe() if jetzt - os.path.getmtime(p) > 900 and os.path.getsize(p) > 20000]
    if not kand:
        sys.exit("Keine abgeschlossene Sitzung gefunden (alle < 15 min alt oder zu klein).")
    return sorted(kand, key=os.path.getmtime, reverse=True)[:n]


def befehlsklasse(name, eingabe):
    """Grobe Klasse eines Aufrufs fuer den Sitzungsvergleich: Werkzeug + erstes Programm."""
    cmd = str((eingabe or {}).get("command", "")).strip()
    if not cmd:
        return name
    cmd = re.sub(r"^((cd|set-location|sl)\s+\S+\s*(;|&&)\s*|\$\w+\s*=\s*('[^']*'|\"[^\"]*\"|\S+)\s*;\s*)+", "", cmd, flags=re.I)
    teile = [os.path.basename(x.strip("\"'&()")).lower() for x in re.split(r"[\s;|]+", cmd)[:4]]
    teile = [x for x in teile if x and not x.startswith("-")]
    if not teile:
        return name
    erstes = teile[0]
    if erstes in ("powershell", "pwsh", "python", "python3", "bash", "sh", "node", "&") and len(teile) > 1:
        erstes += " " + teile[1]
    return f"{name}:{erstes}"


def fehlerklasse(txt):
    zeile = next((z for z in txt.splitlines() if z.strip() and not z.startswith("Exit code")), txt)
    return re.sub(r"[0-9a-f]{6,}|\d+|[A-Z]:\\\S+|/\S+/\S+", "#", zeile.strip())[:70]


def text_von(inhalt):
    if isinstance(inhalt, str):
        return inhalt
    if isinstance(inhalt, list):
        return " ".join(b.get("text", "") for b in inhalt if isinstance(b, dict))
    return ""


def schluessel(name, eingabe):
    """Normierte Eingabe fuer Doppelarbeits-Erkennung (Beschreibungen ignorieren)."""
    e = {k: v for k, v in (eingabe or {}).items()
         if k not in ("description", "timeout", "run_in_background")}
    roh = name + json.dumps(e, sort_keys=True, ensure_ascii=False)
    return hashlib.sha1(roh.encode("utf-8")).hexdigest()[:12]


def kurz(name, eingabe, n=90):
    e = eingabe or {}
    for k in ("command", "file_path", "pattern", "skill", "query", "prompt", "url"):
        if k in e:
            return f"{name}: {str(e[k]).strip().splitlines()[0][:n]}" if str(e[k]).strip() else name
    return name


def lies(pfad, quelle):
    zeilen = []
    with open(pfad, encoding="utf-8") as f:
        for l in f:
            try:
                d = json.loads(l)
            except Exception:
                continue
            if d.get("type") in ("user", "assistant") and d.get("timestamp"):
                d["_quelle"] = quelle
                zeilen.append(d)
    return zeilen


def analysiere(pfad):
    haupt = lies(pfad, "haupt")
    sub_dir = os.path.join(pfad[:-6], "subagents")
    subs = []
    if os.path.isdir(sub_dir):
        for f in sorted(os.listdir(sub_dir)):
            if f.endswith(".jsonl"):
                subs.append((f, lies(os.path.join(sub_dir, f), f[:-6])))

    aufrufe = {}   # id -> dict
    reihenfolge = []
    erste_prompt = ""
    mensch_s = modell_s = 0.0
    lange_modell = []
    nutzer_prompts = 0
    unterbrechungen = 0
    letzte_zeit = None
    letzte_art = None  # 'mensch' wenn das letzte Ereignis auf Frank wartete

    for d in haupt:
        t = ts(d["timestamp"])
        m = d.get("message", {}) or {}
        inhalt = m.get("content")
        if d["type"] == "user":
            ist_ergebnis = isinstance(inhalt, list) and any(
                isinstance(b, dict) and b.get("type") == "tool_result" for b in inhalt)
            if not ist_ergebnis:
                txt = text_von(inhalt)
                if txt.startswith("<") and "system-reminder" in txt[:40]:
                    continue
                if "[Request interrupted" in txt:
                    unterbrechungen += 1
                nutzer_prompts += 1
                if not erste_prompt:
                    erste_prompt = re.sub(r"\s+", " ", txt)[:160]
                letzte_zeit, letzte_art = t, "prompt"
                continue
            for b in inhalt:
                if not isinstance(b, dict) or b.get("type") != "tool_result":
                    continue
                a = aufrufe.get(b.get("tool_use_id"))
                if not a:
                    continue
                a["ende"] = t
                txt = text_von(b.get("content"))
                a["fehler"] = bool(b.get("is_error"))  # vom Harness gesetzt: Exit-Code != 0, Edit-Fehlschlag, Ablehnung
                a["ausgabe"] = len(txt)
                a["fehlertext"] = re.sub(r"\s+", " ", txt)[:120] if a["fehler"] else ""
                a["fehlerklasse"] = fehlerklasse(txt[:600]) if a["fehler"] else ""
                a["fehlerroh"] = txt[:600] if a["fehler"] else ""
                if a["name"] in MENSCH_WERKZEUGE:
                    mensch_s += max(0.0, t - a["start"])
                letzte_zeit, letzte_art = t, "ergebnis"
        else:
            if letzte_zeit is not None and t is not None:
                pause = t - letzte_zeit
                if letzte_art == "prompt" and pause > 0:
                    pass  # Modell startet nach Prompt: zaehlt als modell
                if pause > 0:
                    modell_s += pause
                    if pause >= MODELL_LANG_S:
                        lange_modell.append((pause, d["timestamp"]))
            for b in inhalt if isinstance(inhalt, list) else []:
                if isinstance(b, dict) and b.get("type") == "tool_use":
                    aufrufe[b["id"]] = {"name": b.get("name"), "eingabe": b.get("input") or {},
                                        "start": t, "ende": None, "fehler": False, "ausgabe": 0}
                    reihenfolge.append(b["id"])
            letzte_zeit, letzte_art = t, "assistant"

    # Pausen zwischen Assistant-Ende und naechstem Prompt = mensch (Frank tippt / Sitzung ruht)
    prompt_pausen = 0.0
    vorher = None
    for d in haupt:
        t = ts(d["timestamp"])
        inhalt = (d.get("message") or {}).get("content")
        ist_prompt = d["type"] == "user" and not (isinstance(inhalt, list) and any(
            isinstance(b, dict) and b.get("type") == "tool_result" for b in inhalt))
        if ist_prompt and vorher is not None and t:
            prompt_pausen += max(0.0, t - vorher)
        vorher = t
    mensch_s += prompt_pausen  # Ruhe vor einem neuen Prompt: Frank tippt, Sitzung ruht oder Hintergrundmeldung

    lang = []
    namen = Counter()
    spannen = []
    for i in reihenfolge:
        a = aufrufe[i]
        namen[a["name"]] += 1
        if a["ende"] and a["start"] and a["name"] not in MENSCH_WERKZEUGE:
            dauer = a["ende"] - a["start"]
            spannen.append((a["start"], a["ende"]))
            if dauer >= LANG_S:
                lang.append((dauer, kurz(a["name"], a["eingabe"])))
    # parallele Aufrufe nicht doppelt zaehlen: Vereinigung der Zeitspannen
    werkzeug_s, ende = 0.0, None
    for s, e in sorted(spannen):
        if ende is None or s > ende:
            werkzeug_s += e - s
            ende = e
        elif e > ende:
            werkzeug_s += e - ende
            ende = e

    # Doppelarbeit
    gruppen = defaultdict(list)
    for i in reihenfolge:
        a = aufrufe[i]
        if a["name"] in ("TodoWrite", "ToolSearch"):
            continue
        gruppen[schluessel(a["name"], a["eingabe"])].append(a)
    doppelt = [(len(v), kurz(v[0]["name"], v[0]["eingabe"]), sum(1 for x in v if x["fehler"]))
               for v in gruppen.values() if len(v) >= 2]
    gelesen = Counter(a["eingabe"].get("file_path") for i in reihenfolge
                      if (a := aufrufe[i])["name"] == "Read")
    mehrfach_gelesen = [(n, p) for p, n in gelesen.items() if p and n >= 3]

    fehler = [a for i in reihenfolge if (a := aufrufe[i])["fehler"]]
    fehlerklassen = Counter(f"{a['name']}: {a['fehlerklasse']}" for a in fehler)
    langsam = Counter()
    for i in reihenfolge:
        a = aufrufe[i]
        if a["ende"] and a["start"] and a["name"] not in MENSCH_WERKZEUGE and a["ende"] - a["start"] >= LANG_S:
            langsam[befehlsklasse(a["name"], a["eingabe"])] += a["ende"] - a["start"]

    # Subagenten
    sub_info = []
    for name, zeilen in subs:
        tz = [ts(z["timestamp"]) for z in zeilen if ts(z["timestamp"])]
        n_tools = sum(1 for z in zeilen if z["type"] == "assistant" for b in
                      ((z.get("message") or {}).get("content") or []) if isinstance(b, dict) and b.get("type") == "tool_use")
        sub_info.append((name, (max(tz) - min(tz)) if tz else 0, n_tools))

    zeiten = [ts(d["timestamp"]) for d in haupt if ts(d["timestamp"])]
    gesamt = (max(zeiten) - min(zeiten)) if zeiten else 0

    # Eigenaufwand: vom ersten Aufruf dieses Skills bis zum Sitzungsende
    eigen = next((a["start"] for i in reihenfolge if (a := aufrufe[i])["name"] == "Skill"
                  and str(a["eingabe"].get("skill", "")).endswith(SKILL_NAME)), None)
    eigen_s = (max(zeiten) - eigen) if eigen and zeiten else 0

    # Treffer je Massnahme (fuer die Wirkungspruefung vorher/nachher)
    m_treffer = {}
    for mn in massnahmen():
        try:
            fr = re.compile(mn["fehler_regex"]) if mn.get("fehler_regex") else None
            ar = re.compile(mn["aufruf_regex"]) if mn.get("aufruf_regex") else None
        except re.error:
            continue
        n_f = sum(1 for a in fehler if fr and fr.search(a.get("fehlerroh", "")))
        n_a = 0
        for i in reihenfolge:
            a = aufrufe[i]
            if not ar or (mn.get("werkzeug") and a["name"] != mn["werkzeug"]):
                continue
            roh = str(a["eingabe"].get("command") or json.dumps(a["eingabe"], ensure_ascii=False))
            if ar.search(roh) and roh.count("\n") + 1 >= mn.get("min_zeilen", 0):
                n_a += 1
        m_treffer[mn["id"]] = (n_f, n_a)
    return {
        "datei": pfad, "auftrag": erste_prompt, "gesamt_s": gesamt, "beginn": min(zeiten) if zeiten else 0,
        "eigen_s": eigen_s, "massnahmen": m_treffer,
        "werkzeug_s": werkzeug_s, "modell_s": modell_s, "mensch_s": mensch_s,
        "aufrufe": len(reihenfolge), "werkzeuge": namen.most_common(8),
        "nutzer_prompts": nutzer_prompts, "unterbrechungen": unterbrechungen,
        "lange_werkzeuge": sorted(lang, reverse=True), "lange_modell": sorted(lange_modell, reverse=True),
        "doppelt": sorted(doppelt, reverse=True), "mehrfach_gelesen": sorted(mehrfach_gelesen, reverse=True),
        "fehler": [(kurz(a["name"], a["eingabe"], 60), a["fehlertext"]) for a in fehler],
        "fehlerklassen": fehlerklassen.most_common(), "langsam_klassen": dict(langsam),
        "doppelt_klassen": [kurz(v[0]["name"], v[0]["eingabe"], 60) for v in gruppen.values() if len(v) >= 2],
        "grosse_ausgaben": sorted(((a["ausgabe"], kurz(a["name"], a["eingabe"], 60)) for i in reihenfolge
                                  if (a := aufrufe[i])["ausgabe"] > 15000), reverse=True),
        "subagenten": sub_info,
    }


def m(s):
    return f"{s/60:.1f} min" if s >= 90 else f"{s:.0f} s"


def bericht(r, top):
    z = [f"# Sitzung {os.path.basename(r['datei'])[:-6][:8]}  ({m(r['gesamt_s'])} gesamt)",
         f"Auftrag: {r['auftrag']}",
         f"Zeit: Werkzeug {m(r['werkzeug_s'])} · Modell {m(r['modell_s'])} · Frank/Ruhe {m(r['mensch_s'])}",
         f"Aufrufe: {r['aufrufe']} ({', '.join(f'{n} {c}' for n, c in r['werkzeuge'])}); "
         f"Prompts {r['nutzer_prompts']}, Unterbrechungen {r['unterbrechungen']}"]
    def block(titel, eintraege, fmt):
        if eintraege:
            z.append(f"\n## {titel}")
            z.extend(fmt(e) for e in eintraege[:top])
            if len(eintraege) > top:
                z.append(f"  … {len(eintraege) - top} weitere")
    block("Lange Werkzeugläufe (≥60 s)", r["lange_werkzeuge"], lambda e: f"- {m(e[0])}  {e[1]}")
    block("Lange Modellpausen (≥120 s)", r["lange_modell"], lambda e: f"- {m(e[0])}  vor {e[1][11:19]}")
    block("Fehler", r["fehler"], lambda e: f"- {e[0]}  →  {e[1]}")
    block("Wiederholte identische Aufrufe", r["doppelt"],
          lambda e: f"- {e[0]}×  {e[1]}" + (f"  ({e[2]}× Fehler)" if e[2] else ""))
    block("Dateien ≥3× gelesen", r["mehrfach_gelesen"], lambda e: f"- {e[0]}×  {e[1]}")
    block("Große Ausgaben (>15k Zeichen, Kontextlast)", r["grosse_ausgaben"], lambda e: f"- {e[0]//1000}k  {e[1]}")
    block("Subagenten", r["subagenten"], lambda e: f"- {e[0][:28]}  {m(e[1])}, {e[2]} Aufrufe")
    block("Treffer bekannter Maßnahmen (Fehler / Aufrufe)", [(k, v) for k, v in r["massnahmen"].items() if any(v)],
          lambda e: f"- {e[0]}: {e[1][0]} Fehler, {e[1][1]} Aufrufe")
    if r["eigen_s"]:
        z.append(f"\nFrüherer Lauf dieses Skills in der Sitzung: {m(r['eigen_s'])} bis Sitzungsende")
    return "\n".join(z)


def vergleich(berichte, top):
    """Mehrere Sitzungen: was kommt in >= 2 Sitzungen vor (wiederkehrend) und was nur einmal."""
    n = len(berichte)
    z = [f"# Vergleich der {n} jüngsten abgeschlossenen Sitzungen"]
    for r in berichte[:top + 4]:
        z.append(f"- {os.path.basename(r['datei'])[:8]}  {m(r['gesamt_s'])}  W {m(r['werkzeug_s'])} · "
                 f"M {m(r['modell_s'])} · F {m(r['mensch_s'])}  {r['aufrufe']} Aufr., "
                 f"{len(r['fehler'])} Fehler  | {r['auftrag'][:60]}")
    if n > top + 4:
        z.append(f"  … {n - top - 4} ältere Sitzungen nur in der Auswertung unten")
    def wiederkehrend(schl):
        sitz, summe = Counter(), Counter()
        for r in berichte:
            werte = r[schl]
            paare = werte.items() if isinstance(werte, dict) else (
                (k, 1) for k in werte) if werte and not isinstance(werte[0], (list, tuple)) else werte
            gesehen = set()
            for k, v in paare:
                summe[k] += v
                if k not in gesehen:
                    sitz[k] += 1
                    gesehen.add(k)
        return sorted(((sitz[k], summe[k], k) for k in sitz if sitz[k] >= 2), reverse=True)
    for titel, schl, fmt in (
            ("Fehlerklassen in ≥2 Sitzungen", "fehlerklassen", lambda s, v, k: f"- {s} Sitz., {v}×  {k}"),
            ("Langsame Befehlsklassen in ≥2 Sitzungen", "langsam_klassen", lambda s, v, k: f"- {s} Sitz., {m(v)}  {k}"),
            ("Doppelaufrufe in ≥2 Sitzungen", "doppelt_klassen", lambda s, v, k: f"- {s} Sitz.  {k}")):
        w = wiederkehrend(schl)
        z.append(f"\n## {titel}")
        z.extend(fmt(*e) for e in w[:top]) if w else z.append("- keine")
    z.append("\n## Wirkung der Maßnahmen (Sitzungen mit Treffer / Fehler / Aufrufe)")
    liste = massnahmen()
    for mn in liste:
        try:
            stichtag = datetime.fromisoformat(mn["datum"]).timestamp()
        except Exception:
            continue
        def summe(teil):
            t = [r["massnahmen"].get(mn["id"], (0, 0)) for r in teil]
            return f"{sum(1 for x in t if any(x))}/{len(teil)} Sitz., {sum(x[0] for x in t)} F, {sum(x[1] for x in t)} A"
        vor = [r for r in berichte if r["beginn"] < stichtag]
        nach = [r for r in berichte if r["beginn"] >= stichtag]
        z.append(f"- {mn['id']} [{mn.get('status', '?')}] vorher {summe(vor)} · nachher {summe(nach)}  | {mn['titel'][:50]}")
    if not liste:
        z.append("- keine Maßnahmen eingetragen")
    eigen = [r["eigen_s"] for r in berichte if r["eigen_s"]]
    if eigen:
        z.append(f"\nEigenaufwand frühere Läufe: {len(eigen)}×, Mittel {m(sum(eigen)/len(eigen))}, max {m(max(eigen))}")
    gesamt = sum(r["gesamt_s"] for r in berichte) or 1
    z.append(f"\nAnteile gesamt: Werkzeug {100*sum(r['werkzeug_s'] for r in berichte)/gesamt:.0f} % · "
             f"Modell {100*sum(r['modell_s'] for r in berichte)/gesamt:.0f} % · "
             f"Frank/Ruhe {100*sum(r['mensch_s'] for r in berichte)/gesamt:.0f} %")
    return "\n".join(z)


if __name__ == "__main__":
    t0 = time.time()
    top = 8
    mehrere = 0
    for a in sys.argv[1:]:
        if a.startswith("--top="):
            top = int(a.split("=", 1)[1])
        if a.startswith("--letzte="):
            mehrere = int(a.split("=", 1)[1])
    args = [a for a in sys.argv[1:] if not a.startswith(("--top=", "--letzte="))]
    if mehrere:
        txt = vergleich([analysiere(p) for p in abgeschlossene(mehrere)], top)
    else:
        ziel = next((a for a in args if a != "--json"), "--letzte")
        r = analysiere(finde_datei(ziel))
        if "--json" in args:
            print(json.dumps(r, ensure_ascii=False, indent=1, default=str))
            sys.exit(0)
        txt = bericht(r, top)
    print(txt)
    print(f"\n(Eigenaufwand: {time.time() - t0:.1f} s Laufzeit, {len(txt)} Zeichen ≈ {len(txt)//4} Tokens)")