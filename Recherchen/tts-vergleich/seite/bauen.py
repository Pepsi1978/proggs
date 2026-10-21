"""Erzeugt seite/berichte.js aus den Berichten r1–r10, den Hörproben und den Aussprache-Abgleichen.

Aufruf: python -I seite/bauen.py   (aus dem Ordner Recherchen/tts-vergleich oder mit absolutem Pfad)
"""
import json, pathlib, re, subprocess

BASIS = pathlib.Path(__file__).resolve().parent.parent
PROBEN = BASIS / "proben"

# Prüfstellen: Suchmuster im kleingeschriebenen Transkript, die eine korrekte Aussprache belegen
PRUEFSTELLEN = [
    ("21.10.2026", r"einundzwanzigsten (oktober|zehnten) zweitausendsechsundzwanzig"),
    ("18:47 Uhr", r"achtzehn uhr siebenundvierzig"),
    ("30,5 ms", r"dreißig komma fünf millisekunden"),
    ("2,3 %", r"zwei komma drei prozent"),
    ("1.249,99 €", r"(ein)?tausendzweihundertneunundvierzig euro (und )?neunundneunzig"),
    ("3. Nov.", r"dritten november"),
    ("ca.", r"(zirka|circa)"),
    ("12,5 km", r"zwölf komma fünf kilometer"),
    ("14 °C", r"vierzehn grad celsius"),
    ("Dr.", r"doktor"),
    ("z. B.", r"zum beispiel"),
]


def dauer(datei):
    out = subprocess.run(["ffprobe", "-v", "quiet", "-show_entries", "format=duration", "-of", "csv=p=0", str(datei)],
                         capture_output=True, text=True).stdout.strip()
    return round(float(out), 1) if out else None


def lade(name):
    p = BASIS / name
    return json.loads(p.read_text(encoding="utf-8")) if p.exists() else {}


def main():
    berichte = {}
    for f in sorted(BASIS.glob("r*.md"), key=lambda p: int(re.match(r"r(\d+)", p.name).group(1))):
        nummer = re.match(r"(r\d+)", f.name).group(1)
        text = f.read_text(encoding="utf-8")
        titel = text.splitlines()[0].lstrip("# ").strip()
        berichte[nummer] = {"datei": f.name, "titel": titel, "text": text}

    pruefer = lade("aussprache-abgleich.json")
    transkript_b = lade("aussprache-transkripte.json")
    transkript_c = lade("aussprache-transkripte-2.json")

    proben = []
    for f in sorted(PROBEN.glob("*.mp3")):
        tb = (transkript_b.get(f.name) or {}).get("transkript", "")
        tc = (transkript_c.get(f.name) or {}).get("transkript", "")
        stellen = []
        for stelle, muster in PRUEFSTELLEN:
            treffer = [bool(re.search(muster, t.lower())) for t in (tb, tc) if t]
            stellen.append({"stelle": stelle, "ok": sum(treffer), "von": len(treffer)})
        p = pruefer.get(f.name) or {}
        punkte = p.get("pruefpunkte") or []
        proben.append({
            "datei": f.name, "dauer": dauer(f),
            "transkriptB": tb, "transkriptC": tc, "stellen": stellen,
            "prueferA": {"korrekt": sum(1 for x in punkte if x.get("korrekt")), "von": len(punkte),
                         "auffaelligkeiten": p.get("auffaelligkeiten", ""),
                         "fehler": [x for x in punkte if not x.get("korrekt")]} if punkte else None,
        })

    testtext = (BASIS / "testtext.md").read_text(encoding="utf-8")
    ziel = BASIS / "seite" / "berichte.js"
    ziel.write_text("window.BERICHTE = " + json.dumps(berichte, ensure_ascii=False) + ";\n"
                    + "window.PROBEN = " + json.dumps(proben, ensure_ascii=False) + ";\n"
                    + "window.TESTTEXT = " + json.dumps(testtext, ensure_ascii=False) + ";\n", encoding="utf-8")
    print(f"{ziel}: {len(berichte)} Berichte, {len(proben)} Proben")


main()
