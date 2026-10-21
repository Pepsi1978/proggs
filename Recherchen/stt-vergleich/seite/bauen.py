"""Erzeugt seite/berichte.js aus den Berichten r1–r10 und den Messergebnissen des Messplatzes.

Aufruf: python -I seite/bauen.py
Wortfehlerrate (WER) nach einheitlicher Normalisierung: Zahlen, Einheiten und Abkürzungen werden in
Referenz und Erkennung auf dieselbe Kanonform gebracht, sodass „18:47 Uhr“ und „achtzehn Uhr
siebenundvierzig“ gleich zählen. Gezählt werden also echte Hörfehler, nicht Schreibweisen.
"""
import json, pathlib, re

BASIS = pathlib.Path(__file__).resolve().parent.parent
MESSUNG = BASIS / "messung"

TESTTEXT_A = ("Guten Abend, Frank. Am Mittwoch, dem 21.10.2026, um 18:47 Uhr lag deine HRV bei 30,5 ms – das sind "
              "2,3 % weniger als am Vortag. Die Rechnung über 1.249,99 € ist bis zum 3. Nov. fällig. Die Strecke ist "
              "ca. 12,5 km lang, bei 14 °C. Dr. Müller empfiehlt z. B. ein Update der App per WLAN, nicht per USB. "
              "Hast du das Meeting im Office gecancelt? Ja! Das war's.")
EIGENNAMEN = (MESSUNG / "eigennamen.txt").read_text(encoding="utf-8").strip()

# (Muster, Kanonform) – Reihenfolge zählt, Längeres zuerst
KANON = [
    (r"(21\.\s?10\.\s?2026|21\.\s?oktober\s?2026|einundzwanzigste[nrsm]?\s+(oktober|zehnte[nrsm]?)\s+(zweitausendsechsundzwanzig|2026))", " DATUM "),
    (r"(18[:.]\s?47|achtzehn uhr siebenundvierzig)( uhr)?", " ZEIT "),
    (r"(30,5|dreißig komma fünf)\s*(ms|millisekunden)", " HRVWERT "),
    (r"(\b30|dreißig)\s*(ms|millisekunden)", " HRVWERT2 "),
    (r"(2,3|zwei komma drei)\s*(%|prozent)", " PROZENT "),
    (r"(1\.?249,99\s*(€|euro)|(ein)?tausendzweihundertneunundvierzig (komma )?neunundneunzig euro"
     r"|(ein)?tausendzweihundertneunundvierzig euro (und )?neunundneunzig( cent)?)", " GELD "),
    (r"(\b3\.|dritten)\s*nov(ember|\.)?", " NOV3 "),
    (r"(12,5|zwölf komma fünf)\s*(km|kilometer)", " STRECKE "),
    (r"(\b14|vierzehn)\s*(°\s*c\b|grad celsius|°|grad)", " TEMP "),
    (r"\bca\.|\bcirca\b|\bzirka\b|\bca\b", " ca "),
    (r"\bz\.\s?b\.|\bzb\b|zum beispiel", " zb "),
    (r"\bdr\.|\bdoktor\b|\bdr\b", " dr "),
    (r"\bw-?lan\b|\bweh-?lan\b", " wlan "),
    (r"\bh-?r-?v\b|\bh r v\b", " hrv "),
    (r"\bu-?s-?b\b", " usb "),
    (r"gecancel(t|led|ed)\b|gecanceled", " gecancelt "),
    (r"war['’`´]?s\b|war es\b", " wars "),
    (r"open[- ]?launcher", " openlauncher "),
    (r"update[- ]?station", " updatestation "),
    (r"\blog[- ]?cat\b", " logcat "),
    (r"\bfold\s*(8|acht)\b", " fold8 "),
    (r"\ba[- ]?p[- ]?k\b", " apk "),
    # Zusammen- oder Getrenntschreibung von Komposita ist kein Hörfehler
    (r"\bsonnet[- ]?schwarm", " sonnetschwarm "),
    (r"\bbug[- ]?almanach", " bugalmanach "),
    (r"\bapk\s*-?\s*update", " apkupdate "),  # nach der apk-Regel steht hier „ apk -update“
]

PRUEF_A = [("21.10.2026", "DATUM"), ("18:47 Uhr", "ZEIT"), ("30,5 ms", "HRVWERT"), ("2,3 %", "PROZENT"),
           ("1.249,99 €", "GELD"), ("3. Nov.", "NOV3"), ("ca.", "ca"), ("12,5 km", "STRECKE"), ("14 °C", "TEMP"),
           ("Dr. Müller", "dr müller"), ("z. B.", "zb"), ("HRV", "hrv"), ("WLAN", "wlan"), ("USB", "usb"),
           ("Meeting", "meeting"), ("Office", "office"), ("gecancelt", "gecancelt")]
PRUEF_E = [("Jarvis", "jarvis"), ("OpenLauncher", "openlauncher"), ("Sonnet-Schwarm", "sonnetschwarm"),
           ("Groq", "groq"), ("Whisper", "whisper"), ("Logcat", "logcat"), ("Harness", "harness"),
           ("Bug-Almanach", "bugalmanach"), ("APK-Update", "apkupdate"), ("UpdateStation", "updatestation"),
           ("WLAN", "wlan"), ("Galaxy Fold 8", "galaxy fold8"), ("HRV", "hrv"), ("30 Millisekunden", "HRVWERT2")]


def kanon(text):
    t = " " + text.lower().replace(" ", " ") + " "
    for muster, ersatz in KANON:
        t = re.sub(muster, ersatz, t)
    t = t.replace("-", " ").replace("–", " ").replace("—", " ")
    t = re.sub(r"[^\w\s]", " ", t)
    return t.split()


def wer(ref, hyp):
    r, h = kanon(ref), kanon(hyp)
    d = list(range(len(h) + 1))
    for i in range(1, len(r) + 1):
        vor, d[0] = d[0], i
        for j in range(1, len(h) + 1):
            alt = d[j]
            d[j] = min(d[j] + 1, d[j - 1] + 1, vor + (r[i - 1] != h[j - 1]))
            vor = alt
    return d[len(h)] / max(len(r), 1)


def enthaelt(hyp, ziel):
    return f" {ziel} " in " " + " ".join(kanon(hyp)) + " "


def art(datei):
    if datei.startswith("eigennamen"):
        return "eigennamen"
    if datei.startswith("stille"):
        return "stille"
    if datei.startswith("rauschen"):
        return "rauschen"
    return "sauber"


def main():
    berichte = {}
    for f in sorted(BASIS.glob("r*.md"), key=lambda p: int(re.match(r"r(\d+)", p.name).group(1))):
        nummer = re.match(r"(r\d+)", f.name).group(1)
        text = f.read_text(encoding="utf-8")
        berichte[nummer] = {"datei": f.name, "titel": text.splitlines()[0].lstrip("# ").strip(), "text": text}

    roh = {}
    for name in ("ergebnisse.json", "ergebnisse-lokal.json"):
        p = MESSUNG / name
        if p.exists():
            roh.update(json.loads(p.read_text(encoding="utf-8")))

    modelle = []
    for modell, dateien in roh.items():
        zeilen, summe = [], {}
        for datei, e in dateien.items():
            a = art(datei)
            z = {"datei": datei, "art": a, "text": e["text"], "sekunden": e["sekunden"], "fehler": e["fehler"]}
            if not e["fehler"]:
                if a == "stille":
                    z["halluziniert"] = bool(kanon(e["text"]))  # „...“ allein zählt nicht als Text
                else:
                    ref = EIGENNAMEN if a == "eigennamen" else TESTTEXT_A
                    z["wer"] = round(wer(ref, e["text"]), 4)
                    pruef = PRUEF_E if a == "eigennamen" else PRUEF_A
                    z["stellen"] = [{"stelle": s, "ok": enthaelt(e["text"], k)} for s, k in pruef]
                    summe.setdefault(a, []).append(z["wer"])
            zeilen.append(z)
        ok = [z for z in zeilen if not z["fehler"]]
        modelle.append({
            "modell": modell, "proben": zeilen,
            "werMittel": {a: round(sum(v) / len(v), 4) for a, v in summe.items()},
            "sekundenMedian": sorted(z["sekunden"] for z in ok)[len(ok) // 2] if ok else None,
            "fehlerAnzahl": sum(1 for z in zeilen if z["fehler"]),
        })
    modelle.sort(key=lambda m: m["werMittel"].get("sauber", 9))

    ziel = BASIS / "seite" / "berichte.js"
    ziel.write_text("window.BERICHTE = " + json.dumps(berichte, ensure_ascii=False) + ";\n"
                    + "window.MESSUNG = " + json.dumps({"modelle": modelle, "testtextA": TESTTEXT_A, "eigennamen": EIGENNAMEN},
                                                       ensure_ascii=False) + ";\n", encoding="utf-8")
    print(f"{ziel}: {len(berichte)} Berichte, {len(modelle)} Modelle")
    for m in modelle:
        print(f"  {m['modell']:34} {m['werMittel']}  median {m['sekundenMedian']} s  Fehler {m['fehlerAnzahl']}")


main()
