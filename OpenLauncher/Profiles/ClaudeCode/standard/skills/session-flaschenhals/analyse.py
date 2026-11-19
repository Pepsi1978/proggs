#!/usr/bin/env python3
"""Session-Flaschenhals: misst einen abgeschlossenen Claude-Code-Verlauf (JSONL).

Aufruf:  python analyse.py <sitzung.jsonl | sitzungs-id | --letzte | --letzte=N> [--json] [--top N] [--nur-profil]
Ohne Pfad nimmt --letzte die juengste abgeschlossene Sitzung (>= 15 min unveraendert).
Gesucht wird in allen Geschwister-Profilen des aktiven Profils (minimal, standard, strict), denn
massnahmen.json gilt fuer alle drei; --nur-profil beschraenkt auf das aktive Profil.
Subagenten-Verlaeufe (<id>/subagents/*.jsonl): ihre Aufrufe und Fehler zaehlen bei Fehlern und
Massnahmen mit, Dauer und Anzahl stehen im Block Subagenten; Zeittoepfe und Doppelarbeit messen
nur den Hauptverlauf. --json liefert die vollen Befehle hinter Fehlern und Massnahmen-Treffern
(mit --letzte=N als Liste). Liest nur, schreibt nichts.

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

try:  # Stichtage in massnahmen.json sind Berliner Zeit, egal wo das Skript laeuft (Server evtl. UTC)
    from zoneinfo import ZoneInfo
    BERLIN = ZoneInfo("Europe/Berlin")
except Exception:  # ohne tzdata (Windows ohne Paket) oder Python < 3.9: fail-open auf Ortszeit
    BERLIN = None


def _konfig():
    """Aktiver Claude-Code-Profilordner; in Codex/OpenCode fehlt die Variable -> OpenLauncher-Profil."""
    home = os.path.expanduser("~")
    kand = [os.environ.get("CLAUDE_CONFIG_DIR", ""),
            os.path.join(home, ".openlauncher", "claude", "minimal"),  # Hostinger-Server
            os.path.join(home, "proggs", "OpenLauncher", "Profiles", "ClaudeCode", "minimal"),  # Windows
            os.path.join(home, "proggs", "OpenLauncher", "Profiles", "ClaudeCodeMac", "minimal"),  # Mac
            os.path.join(home, ".claude")]

    def hat_verlaeufe(k):
        p = os.path.join(k, "projects")
        try:
            return any(f.endswith(".jsonl") for d in os.listdir(p) if os.path.isdir(os.path.join(p, d))
                       for f in os.listdir(os.path.join(p, d)))
        except OSError:
            return False
    return next((k for k in kand if k and hat_verlaeufe(k)), kand[-1])


PLATTFORM = "windows" if sys.platform.startswith("win") else ("mac" if sys.platform == "darwin" else "linux")


KONFIG = _konfig()
PROJEKTE = os.path.join(KONFIG, "projects")
HIER = os.path.dirname(os.path.abspath(__file__))
SKILL_NAME = "session-flaschenhals"
PROFILE = ("minimal", "standard", "strict")


def _profilordner():
    """Aktives Profil plus Geschwister (minimal/standard/strict desselben Ordners), ausser --nur-profil."""
    k = os.path.normpath(KONFIG)
    if "--nur-profil" in sys.argv or os.path.basename(k) not in PROFILE:
        return [k]
    eltern = os.path.dirname(k)
    return [os.path.join(eltern, p) for p in PROFILE if os.path.isdir(os.path.join(eltern, p, "projects"))] or [k]


PROFILORDNER = _profilordner()


def profil_von(pfad):
    """<profil>/projects/<projekt>/<id>.jsonl -> Profilname."""
    return os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(pfad)))))


def alle_verlaeufe():
    """Hauptverlaeufe aller Projektordner der gelesenen Profile (keine Subagenten)."""
    erg = []
    for k in PROFILORDNER:
        p = os.path.join(k, "projects")
        if not os.path.isdir(p):
            continue
        erg += [os.path.join(p, d, f) for d in os.listdir(p) if os.path.isdir(os.path.join(p, d))
                for f in os.listdir(os.path.join(p, d)) if f.endswith(".jsonl")]
    return erg


_MASSNAHMEN = None


def massnahmen():
    """Einmal je Lauf laden (bei --letzte=40 sonst 41-mal); Einträge ohne id werden übergangen."""
    global _MASSNAHMEN
    if _MASSNAHMEN is None:
        _MASSNAHMEN = [m for m in _massnahmen_laden() if m.get("id")]
    return _MASSNAHMEN


def _massnahmen_laden():
    try:
        with open(os.path.join(HIER, "massnahmen.json"), encoding="utf-8") as f:
            liste = json.load(f).get("massnahmen", [])
        # Massnahmen gelten je Rechner: ohne "plattform" ueberall, sonst nur dort (windows/mac/linux); Text oder Liste
        def plattformen(m):
            p = m.get("plattform", [PLATTFORM])
            return [p] if isinstance(p, str) else p
        return [m for m in liste if PLATTFORM in plattformen(m)]
    except Exception as e:  # kaputte oder fehlende Liste darf die Analyse nicht stoppen
        print(f"(Hinweis: massnahmen.json nicht lesbar: {e})", file=sys.stderr)
        return []


def status_von(mn):
    """status ist Text oder je Plattform ein Objekt {"windows": .., "mac": .., "linux": ..}."""
    s = mn.get("status")
    if isinstance(s, dict):
        s = s.get(PLATTFORM)
    return s or "STATUS FEHLT"


def stichtag_von(datum):
    """datum (ISO, Vorgabe in SKILL.md, oder Profilregel-6-Format) als Berliner Zeit -> Zeitstempel."""
    for form in (None, "%d.%m.%Y %H:%M", "%d.%m.%Y, %H:%M Uhr"):
        try:
            dt = datetime.fromisoformat(datum) if form is None else datetime.strptime(datum, form)
        except ValueError:
            continue
        if dt.tzinfo is None and BERLIN is not None:
            dt = dt.replace(tzinfo=BERLIN)
        return dt.timestamp()
    return None


MENSCH_WERKZEUGE = {"AskUserQuestion", "ExitPlanMode"}
# Lese-/Suchaufrufe zaehlen nie als Fehler oder Aufruf einer Massnahme: ihr Ergebnis ist Quelltext oder Doku,
# in dem die Fehlermeldung woertlich stehen kann (cat jarvis-install.sh, Almanach lesen).
LESE_WERKZEUGE = {"Read", "Grep", "Glob", "NotebookRead", "WebFetch", "WebSearch", "ToolSearch"}
# Nur Befehle, deren Ausgabe Dateiinhalt ist; ls/find bleiben Arbeit (M35 misst ein haengendes ls -t).
# ls zählt als Lesen, nur ls -t (M35, hing am Mac) bleibt Arbeit: Sonst machte ein angehängtes "ls … | head" ein grep im
# Quelltext zum Treffer (Server 19.11.2026, M27: grep in jarvis-install.sh fand den Meldungstext).
LESE_BEFEHL = re.compile(r"(cat|bat|grep|egrep|rg|head|tail|less|more|wc|nl|awk|sed\s+-n|ls(?!\s+-\w*t)|"
                         r"get-content|gc|select-string|sls|type|"
                         r"git\s+(-C\s+\S+\s+)?(grep|log|show|diff|blame))\b", re.I)
NEBENBEFEHL = re.compile(r"(cd|set-location|sl|echo|printf|write-host|write-output|true|export)\b", re.I)
LANG_S = 60          # Werkzeuglauf ab hier = Kandidat fuer Wartezeit
MODELL_LANG_S = 120  # Modellpause ab hier = Kandidat (z. B. Advisor, langes Denken)


def ts(s):
    try:
        return datetime.fromisoformat(s.replace("Z", "+00:00")).timestamp()
    except Exception:
        return None


def nur_lesen(a):
    """Lese-Werkzeug oder Shell-Befehl, dessen Teile alle nur lesen/suchen (cat, grep, git log …)."""
    if a["name"] in LESE_WERKZEUGE:
        return True
    if a["name"] not in ("Bash", "PowerShell"):
        return False
    cmd = str(a["eingabe"].get("command", ""))
    # Text in Anführungszeichen vor dem Zerlegen ausblenden: grep "a\|b" zerfiel sonst an den | im Muster (Server, M27).
    cmd = re.sub(r'"(?:[^"\\]|\\.)*"|\'[^\']*\'', '""', cmd)
    teile = [t.strip(" \t({") for t in re.split(r"&&|\|\||[;|\n]", cmd)]
    teile = [t for t in teile if t and not NEBENBEFEHL.match(t)]
    return bool(teile) and all(LESE_BEFEHL.match(t) for t in teile)


def finde_datei(arg):
    if arg and os.path.isfile(arg):
        return arg
    if arg and arg != "--letzte":
        treffer = [p for p in alle_verlaeufe() if os.path.basename(p).startswith(arg.replace(".jsonl", ""))]
        if treffer:
            return max(treffer, key=os.path.getmtime)
        sys.exit(f"Verlauf nicht gefunden: {arg} (gesucht unter {', '.join(PROFILORDNER)})")
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
    # errors="replace": die letzte Zeile einer laufenden Sitzung kann mitten in einem Mehrbyte-Zeichen enden
    with open(pfad, encoding="utf-8", errors="replace") as f:
        for l in f:
            try:
                d = json.loads(l)
            except Exception:
                continue
            if d.get("type") in ("user", "assistant") and d.get("timestamp"):
                d["_quelle"] = quelle
                zeilen.append(d)
    return zeilen


def ergebnis_eintragen(a, b, t):
    """tool_result b in den Aufruf a uebernehmen."""
    a["ende"] = t
    txt = text_von(b.get("content"))
    a["fehler"] = bool(b.get("is_error"))  # vom Harness gesetzt: Exit-Code != 0, Edit-Fehlschlag, Ablehnung
    a["ausgabe"] = len(txt)
    a["fehlertext"] = re.sub(r"\s+", " ", txt)[:120] if a["fehler"] else ""
    a["fehlertext_lang"] = re.sub(r"\s+", " ", txt)[:600] if a["fehler"] else ""
    a["fehlerklasse"] = fehlerklasse(txt[:600]) if a["fehler"] else ""
    # Anfang und Ende: Gradle & Co. melden den eigentlichen Fehler oft weit hinten (M7, M9, M19).
    a["roh"] = txt if len(txt) <= 4000 else txt[:1000] + "\n…\n" + txt[-3000:]
    a["fehlerroh"] = a["roh"] if a["fehler"] else ""


def neuer_aufruf(b, t, quelle):
    return {"name": b.get("name"), "eingabe": b.get("input") or {}, "start": t, "ende": None,
            "fehler": False, "ausgabe": 0, "quelle": quelle}


def ist_reminder(txt):
    # Auch Meldungen fertiger Hintergrundläufe (<task-notification>) sind kein Prompt von Frank.
    return txt.startswith("<") and ("system-reminder" in txt[:40] or txt.startswith("<task-notification>"))


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
    prompt_zeiten = []
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
                if ist_reminder(txt):
                    # Kein Prompt, aber die Ruhe davor verbucht die Schleife unten als Frank/Ruhe:
                    # ab hier zaehlt erst das Modell (sonst doppelt gezaehlt).
                    letzte_zeit, letzte_art = t, "prompt"
                    continue
                if "[Request interrupted" in txt:
                    unterbrechungen += 1
                nutzer_prompts += 1
                if t is not None and not d.get("isMeta"):  # isMeta: eingespeister Skill-Text, kein Prompt von Frank
                    prompt_zeiten.append(t)
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
                ergebnis_eintragen(a, b, t)
                if a["name"] in MENSCH_WERKZEUGE:
                    mensch_s += max(0.0, t - a["start"])
                letzte_zeit, letzte_art = t, "ergebnis"
        else:
            if letzte_zeit is not None and t is not None:
                pause = t - letzte_zeit
                if pause > 0:
                    modell_s += pause
                    if pause >= MODELL_LANG_S:
                        lange_modell.append((pause, d["timestamp"]))
            for b in inhalt if isinstance(inhalt, list) else []:
                if isinstance(b, dict) and b.get("type") == "tool_use":
                    aufrufe[b["id"]] = neuer_aufruf(b, t, "haupt")
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

    # Subagenten: Aufrufe und Ergebnisse fuer Fehler und Massnahmen (nicht fuer Zeittoepfe/Doppelarbeit)
    sub_reihe = []
    for name, zeilen in subs:
        for d in zeilen:
            t = ts(d["timestamp"])
            inhalt = (d.get("message") or {}).get("content")
            if not isinstance(inhalt, list):
                continue
            for b in inhalt:
                if not isinstance(b, dict):
                    continue
                if d["type"] == "assistant" and b.get("type") == "tool_use" and b.get("id") not in aufrufe:
                    aufrufe[b["id"]] = neuer_aufruf(b, t, name)
                    sub_reihe.append(b["id"])
                elif d["type"] == "user" and b.get("type") == "tool_result":
                    a = aufrufe.get(b.get("tool_use_id"))
                    if a and a["quelle"] == name:
                        ergebnis_eintragen(a, b, t)
    alle = [aufrufe[i] for i in reihenfolge + sub_reihe]

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

    fehler = [a for a in alle if a["fehler"]]
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

    # Eigenaufwand: vom ersten Aufruf dieses Skills bis zur letzten Zeile vor dem nächsten echten Prompt (sonst Sitzungsende).
    # Ohne die Kappung zählten spätere Nachträge und Franks Pausen mit (Mac 19.11.2026: Mittel 40 min, max 172 min).
    eigen = next((a["start"] for i in reihenfolge if (a := aufrufe[i])["name"] == "Skill"
                  and str(a["eingabe"].get("skill", "")).endswith(SKILL_NAME)), None)
    eigen_s = 0
    if eigen and zeiten:
        naechster = min((p for p in prompt_zeiten if p > eigen), default=None)
        ende = max((z for z in zeiten if naechster is None or z < naechster), default=eigen)
        eigen_s = max(0.0, ende - eigen)

    # Treffer je Massnahme (fuer die Wirkungspruefung vorher/nachher). Grundsatz (Frank, 19.11.2026):
    # Praezision vor Vollstaendigkeit, im Zweifel nicht zaehlen; Fehlalarme loesen falsche Ruecknahmen aus.
    m_treffer, m_details = {}, {}
    for mn in massnahmen():
        try:
            fr = re.compile(mn["fehler_regex"]) if mn.get("fehler_regex") else None
            ar = re.compile(mn["aufruf_regex"]) if mn.get("aufruf_regex") else None
        except re.error:
            continue
        wz = mn.get("werkzeug")
        wz = set(wz) if isinstance(wz, list) else set(wz.split("|")) if wz else None  # "Bash|PowerShell" = beide

        def im_werkzeug(a):
            # werkzeug-Filter gilt immer; Lese-/Suchaufrufe zaehlen nie (Treffer im Quelltext), ausser die Massnahme
            # betrifft den Suchbefehl selbst (auch_lesebefehle, z. B. M36: grep --include=*.kt in zsh)
            return (mn.get("auch_lesebefehle") or not nur_lesen(a)) and (not wz or a["name"] in wz)

        def passt(a):
            if not ar or not im_werkzeug(a):
                return False
            roh = str(a["eingabe"].get("command") or json.dumps(a["eingabe"], ensure_ascii=False))
            return bool(ar.search(roh)) and roh.count("\n") + 1 >= mn.get("min_zeilen", 0)
        # fehler_am_aufruf: nur Fehler genau der Aufrufe zählen, die auf aufruf_regex passen. Sonst zählt z. B. jede
        # berechtigte Bump-Sperre des Hooks als Fehler gegen jede Ausnahme mit demselben Text.
        # Ohne aufruf_regex gibt es nichts zu koppeln (sonst stünde still 0 F da).
        gekoppelt = bool(mn.get("fehler_am_aufruf") and ar)
        if mn.get("auch_ohne_fehlerstatus"):
            # Zeitlimit-Meldungen tragen is_error=False: alle Ergebnisse durchsuchen, aber nur der passenden Aufrufe
            quelle = [a for a in alle if (passt(a) if ar else im_werkzeug(a))]
            feld = "roh"
        else:
            quelle = [a for a in fehler if im_werkzeug(a) and (not gekoppelt or passt(a))]
            feld = "fehlerroh"
        treffer = []
        for a in quelle:
            mt = fr.search(a.get(feld) or "") if fr else None
            if mt:
                txt = a.get(feld) or ""
                treffer.append({"quelle": a["quelle"], "werkzeug": a["name"],
                                "befehl": a["eingabe"].get("command") or a["eingabe"],
                                "auszug": re.sub(r"\s+", " ", txt[max(0, mt.start() - 120):mt.end() + 120])})
        n_a = sum(1 for a in alle if passt(a))
        m_treffer[mn["id"]] = (len(treffer), n_a)
        if treffer:
            m_details[mn["id"]] = treffer

    def fkurz(a, n=60):
        return ("" if a["quelle"] == "haupt" else "[sub] ") + kurz(a["name"], a["eingabe"], n)
    return {
        "datei": pfad, "profil": profil_von(pfad), "auftrag": erste_prompt, "gesamt_s": gesamt,
        "beginn": min(zeiten) if zeiten else 0,
        "eigen_s": eigen_s, "massnahmen": m_treffer, "massnahmen_treffer": m_details,
        "werkzeug_s": werkzeug_s, "modell_s": modell_s, "mensch_s": mensch_s,
        "aufrufe": len(reihenfolge), "sub_aufrufe": len(sub_reihe), "werkzeuge": namen.most_common(8),
        "nutzer_prompts": nutzer_prompts, "unterbrechungen": unterbrechungen,
        "lange_werkzeuge": sorted(lang, reverse=True), "lange_modell": sorted(lange_modell, reverse=True),
        "doppelt": sorted(doppelt, reverse=True), "mehrfach_gelesen": sorted(mehrfach_gelesen, reverse=True),
        "fehler": [(fkurz(a), a["fehlertext"]) for a in fehler],
        "fehler_befehle": [{"quelle": a["quelle"], "werkzeug": a["name"],
                            "befehl": a["eingabe"].get("command") or a["eingabe"],
                            "fehler": a["fehlertext_lang"]} for a in fehler],
        "fehlerklassen": fehlerklassen.most_common(), "langsam_klassen": dict(langsam),
        "doppelt_klassen": [kurz(v[0]["name"], v[0]["eingabe"], 60) for v in gruppen.values() if len(v) >= 2],
        "grosse_ausgaben": sorted(((a["ausgabe"], kurz(a["name"], a["eingabe"], 60)) for i in reihenfolge
                                  if (a := aufrufe[i])["ausgabe"] > 15000), reverse=True),
        "subagenten": sub_info,
    }


def m(s):
    return f"{s/60:.1f} min" if s >= 90 else f"{s:.0f} s"


def bericht(r, top):
    z = [f"# Sitzung {os.path.basename(r['datei'])[:-6][:8]}  ({m(r['gesamt_s'])} gesamt, Profil {r['profil']})",
         f"Auftrag: {r['auftrag']}",
         f"Zeit: Werkzeug {m(r['werkzeug_s'])} · Modell {m(r['modell_s'])} · Frank/Ruhe {m(r['mensch_s'])}",
         f"Aufrufe: {r['aufrufe']} ({', '.join(f'{n} {c}' for n, c in r['werkzeuge'])})"
         + (f" + {r['sub_aufrufe']} in Subagenten" if r["sub_aufrufe"] else "")
         + f"; Prompts {r['nutzer_prompts']}, Unterbrechungen {r['unterbrechungen']}"]
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
    profile = Counter(r["profil"] for r in berichte)
    z = [f"# Vergleich der {n} jüngsten abgeschlossenen Sitzungen "
         f"(Profile: {', '.join(f'{p} {k}' for p, k in profile.most_common())}; gelesen: "
         f"{', '.join(os.path.basename(p) for p in PROFILORDNER)})"]
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
    if BERLIN is None and PLATTFORM != "windows":  # Windows-PC laeuft ohnehin in Berliner Zeit
        z.append("- (Hinweis: zoneinfo/tzdata fehlt, Stichtage als Ortszeit gelesen)")
    liste = massnahmen()
    for mn in liste:
        titel = str(mn.get("titel", ""))[:50]
        status = status_von(mn)
        if mn.get("messbar") is False:
            z.append(f"- {mn.get('id', '?')} [{status}] nicht automatisch messbar  | {titel}")
            continue
        datum = str(mn.get("datum", "")).strip()
        stichtag = stichtag_von(datum)
        if stichtag is None:
            z.append(f"- {mn.get('id', '?')} DATUM UNLESBAR ({datum!r}): nicht gemessen, in massnahmen.json berichtigen")
            continue
        def summe(teil):
            t = [r["massnahmen"].get(mn["id"], (0, 0)) for r in teil]
            return f"{sum(1 for x in t if any(x))}/{len(teil)} Sitz., {sum(x[0] for x in t)} F, {sum(x[1] for x in t)} A"
        vor = [r for r in berichte if r["beginn"] < stichtag]
        nach = [r for r in berichte if r["beginn"] >= stichtag]
        hinweis = "  ← status in massnahmen.json nachtragen" if status == "STATUS FEHLT" else ""
        z.append(f"- {mn.get('id', '?')} [{status}] vorher {summe(vor)} · nachher {summe(nach)}  | {titel}{hinweis}")
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
    args = []
    argv = sys.argv[1:]
    i = 0
    while i < len(argv):
        a = argv[i]
        if a.startswith("--top="):
            top = int(a.split("=", 1)[1])
        elif a == "--top" and i + 1 < len(argv) and argv[i + 1].isdigit():
            top = int(argv[i + 1])
            i += 1
        elif a.startswith("--letzte="):
            mehrere = int(a.split("=", 1)[1])
        elif a == "--letzte" and i + 1 < len(argv) and argv[i + 1].isdigit():
            mehrere = int(argv[i + 1])
            i += 1
        elif a not in ("--alle-profile", "--nur-profil"):  # --alle-profile ist Standard
            args.append(a)
        i += 1
    als_json = "--json" in args
    if mehrere:
        berichte = [analysiere(p) for p in abgeschlossene(mehrere)]
        if als_json:
            print(json.dumps(berichte, ensure_ascii=False, indent=1, default=str))
            sys.exit(0)
        txt = vergleich(berichte, top)
    else:
        ziel = next((a for a in args if a != "--json"), "--letzte")
        r = analysiere(finde_datei(ziel))
        if als_json:
            print(json.dumps(r, ensure_ascii=False, indent=1, default=str))
            sys.exit(0)
        txt = bericht(r, top)
    print(txt)
    print(f"\n(Eigenaufwand: {time.time() - t0:.1f} s Laufzeit, {len(txt)} Zeichen ≈ {len(txt)//4} Tokens)")
