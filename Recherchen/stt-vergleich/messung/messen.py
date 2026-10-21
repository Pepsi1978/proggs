"""Messplatz der STT-Studie: schickt alle Testaufnahmen an jedes erreichbare STT-Modell.

Aufruf: python -I messung/messen.py [anbieter ...]   (ohne Argument: alle)
Schlüssel kommen zur Laufzeit aus ~/SK, nichts davon landet in Dateien.
Ergebnis: messung/ergebnisse.json  ({anbieter: {datei: {text, sekunden, fehler}}})
"""
import base64, json, pathlib, subprocess, sys, tempfile, threading, time, urllib.request, urllib.error, uuid
from concurrent.futures import ThreadPoolExecutor

BASIS = pathlib.Path(__file__).resolve().parent
SK = pathlib.Path.home() / "SK"
TTS_PROBEN = BASIS.parent.parent / "tts-vergleich" / "proben"
EIGENE = BASIS / "audio"
ZIEL = BASIS / "ergebnisse.json"
UA = "curl/8.0"  # Groq blockt den Python-Standard-User-Agent mit 403

VOKABULAR = ["Jarvis", "OpenLauncher", "Sonnet-Schwarm", "Groq", "Whisper", "Logcat", "Harness",
             "Bug-Almanach", "UpdateStation", "Galaxy Fold 8", "WLAN", "APK", "HRV"]
PROMPT_VOKABULAR = "Fachbegriffe: " + ", ".join(VOKABULAR) + "."


def schluessel():
    env = {}
    for zeile in (SK / "VoiceOverlays" / ".env").read_text(encoding="utf-8-sig").splitlines():
        if "=" in zeile and not zeile.startswith("#"):
            a, b = zeile.split("=", 1)
            env[a.strip()] = b.strip()
    k = json.loads((SK / "VoiceAgent" / "keys.json").read_text(encoding="utf-8-sig"))
    return {"groq": env["GROQ_API_KEY"], "gemini": env.get("GEMINI_TRANSCRIBE_API_KEY") or k["gemini"],
            "google": k["google"], "gemini2": k["gemini"]}  # zweiter Schlüssel (Textkorrektur), eigenes Kontingent


def aufnahmen():
    """Testtext A aus den TTS-Hörproben (ohne -b) plus eigene Aufnahmen (Eigennamen, Rauschen, Stille)."""
    dateien = [p for p in sorted(TTS_PROBEN.glob("*.mp3")) if not p.name.endswith("-b.mp3")]
    dateien += sorted(EIGENE.glob("*.mp3")) + sorted(EIGENE.glob("*.wav"))
    return dateien


def als_wav(datei, ordner):
    ziel = pathlib.Path(ordner) / (datei.stem + ".wav")
    if not ziel.exists():
        subprocess.run(["ffmpeg", "-v", "quiet", "-y", "-i", str(datei), "-ar", "16000", "-ac", "1", str(ziel)], check=True)
    return ziel.read_bytes()


def post(url, daten, kopf, zeit=120):
    anfrage = urllib.request.Request(url, data=daten, headers={"User-Agent": UA, **kopf}, method="POST")
    try:
        with urllib.request.urlopen(anfrage, timeout=zeit) as antwort:
            return json.loads(antwort.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"HTTP {e.code}: {e.read().decode('utf-8', 'replace')[:300]}")


def multipart(felder, wav):
    grenze = uuid.uuid4().hex
    teile = []
    for name, wert in felder.items():
        teile.append(f"--{grenze}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n{wert}\r\n".encode())
    teile.append(f"--{grenze}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.wav\"\r\n"
                 "Content-Type: audio/wav\r\n\r\n".encode() + wav + b"\r\n")
    teile.append(f"--{grenze}--\r\n".encode())
    return b"".join(teile), f"multipart/form-data; boundary={grenze}"


def groq(modell, mit_prompt):
    def lauf(wav, k):
        felder = {"model": modell, "language": "de", "response_format": "json", "temperature": "0"}
        if mit_prompt:
            felder["prompt"] = PROMPT_VOKABULAR
        daten, typ = multipart(felder, wav)
        return post("https://api.groq.com/openai/v1/audio/transcriptions", daten,
                    {"Authorization": "Bearer " + k["groq"], "Content-Type": typ})["text"]
    return lauf


def gemini_transcribe(mit_vokabular):
    # Free Tier: 3 Anfragen/Min. und 25/Tag (429-Meldung vom 21.10.2026); erster Lauf mit dem Transcribe-Schlüssel,
    # Rest nach Tageslimit mit dem zweiten Schlüssel
    def lauf(wav, k):
        konfig = {"mode": {"type": "verbatim"}}
        if mit_vokabular:
            konfig["custom_vocabulary"] = VOKABULAR
        nutzlast = {"model": "gemini-3.5-transcribe",
                    "input": [{"type": "audio", "mime_type": "audio/wav", "data": base64.b64encode(wav).decode()}],
                    "generation_config": {"transcription_config": konfig}}
        antwort = post("https://generativelanguage.googleapis.com/v1beta/interactions", json.dumps(nutzlast).encode(),
                       {"x-goog-api-key": k["gemini2"], "Content-Type": "application/json"})
        texte = [t.get("text", "") for s in antwort.get("steps", []) for t in s.get("content", []) if t.get("type", "text") == "text"]
        return " ".join(x for x in texte if x).strip()
    return lauf


def gemini_llm(modell, mit_vokabular):
    def lauf(wav, k):
        anweisung = ("Transkribiere diese deutsche Sprachaufnahme wortgetreu. Gib nur den gesprochenen Text aus, "
                     "ohne Kommentar, ohne Zusammenfassung. Ist nichts gesprochen, gib eine leere Antwort.")
        if mit_vokabular:
            anweisung += " " + PROMPT_VOKABULAR
        nutzlast = {"contents": [{"parts": [{"text": anweisung},
                                            {"inline_data": {"mime_type": "audio/wav", "data": base64.b64encode(wav).decode()}}]}],
                    "generationConfig": {"temperature": 0}}
        antwort = post(f"https://generativelanguage.googleapis.com/v1beta/models/{modell}:generateContent",
                       json.dumps(nutzlast).encode(), {"x-goog-api-key": k["gemini2"], "Content-Type": "application/json"})
        teile = antwort.get("candidates", [{}])[0].get("content", {}).get("parts", [])
        return " ".join(p.get("text", "") for p in teile if not p.get("thought")).strip()
    return lauf


def google_cloud(modell):
    def lauf(wav, k):
        nutzlast = {"config": {"languageCode": "de-DE", "model": modell, "enableAutomaticPunctuation": True,
                               "sampleRateHertz": 16000, "encoding": "LINEAR16"},
                    "audio": {"content": base64.b64encode(wav[44:]).decode()}}
        antwort = post("https://speech.googleapis.com/v1/speech:recognize?key=" + k["google"], json.dumps(nutzlast).encode(),
                       {"Content-Type": "application/json"})
        return " ".join(r["alternatives"][0]["transcript"] for r in antwort.get("results", [])).strip()
    return lauf


ANBIETER = {
    "groq-turbo": groq("whisper-large-v3-turbo", False),
    "groq-turbo+prompt": groq("whisper-large-v3-turbo", True),
    "groq-large-v3": groq("whisper-large-v3", False),
    "groq-large-v3+prompt": groq("whisper-large-v3", True),
    "gemini-3.5-transcribe": gemini_transcribe(False),
    "gemini-3.5-transcribe+vokabular": gemini_transcribe(True),
    "gemini-3.8-flash": gemini_llm("gemini-3.8-flash", False),
    "gemini-3.8-flash+vokabular": gemini_llm("gemini-3.8-flash", True),
    "gemini-3.5-flash-lite": gemini_llm("gemini-3.5-flash-lite", False),
    "google-cloud-latest_long": google_cloud("latest_long"),
}


GRENZEN = BASIS / "grenzen.json"  # erste Ratenlimit-Meldung je Anbieter, als Beleg fürs Freikontingent


def mit_wiederholung(name, lauf, wav, k, grenzen):
    """Bei 429/503 warten und erneut versuchen; gezählt wird nur die Zeit des erfolgreichen Aufrufs."""
    for versuch in range(10):
        start = time.perf_counter()
        try:
            return lauf(wav, k), None, round(time.perf_counter() - start, 2)
        except Exception as e:
            fehler = str(e)[:400]
            if "HTTP 429" in fehler or "HTTP 503" in fehler:
                grenzen.setdefault(name, fehler)
                time.sleep(25 if "429" in fehler else 15)
                continue
            return "", fehler, round(time.perf_counter() - start, 2)
    return "", fehler, None


def main():
    wahl = sys.argv[1:] or list(ANBIETER)
    k = schluessel()
    ergebnisse = json.loads(ZIEL.read_text(encoding="utf-8")) if ZIEL.exists() else {}
    grenzen = json.loads(GRENZEN.read_text(encoding="utf-8")) if GRENZEN.exists() else {}
    sperre = threading.Lock()

    def anbieter_lauf(name, tmp):
        ergebnisse.setdefault(name, {})
        for datei in aufnahmen():
            alt = ergebnisse[name].get(datei.name)
            if alt and not alt["fehler"]:
                continue  # schon erfolgreich gemessen
            wav = als_wav(datei, tmp)
            text, fehler, sekunden = mit_wiederholung(name, ANBIETER[name], wav, k, grenzen)
            with sperre:
                ergebnisse[name][datei.name] = {"text": text, "sekunden": sekunden, "fehler": fehler}
                ZIEL.write_text(json.dumps(ergebnisse, ensure_ascii=False, indent=1), encoding="utf-8")
                GRENZEN.write_text(json.dumps(grenzen, ensure_ascii=False, indent=1), encoding="utf-8")
            print(f"{name:34} {datei.name:50} {'FEHLER ' + fehler[:80] if fehler else text[:70]}", flush=True)
            if fehler and ("HTTP 403" in fehler or "HTTP 401" in fehler or "HTTP 404" in fehler):
                break  # Zugang fehlt: Rest dieses Anbieters überspringen

    with tempfile.TemporaryDirectory() as tmp:
        for datei in aufnahmen():
            als_wav(datei, tmp)  # einmal vorab umwandeln, damit die Threads nicht gleichzeitig schreiben
        # Anbieter parallel (eigene Limits), Aufnahmen je Anbieter nacheinander
        with ThreadPoolExecutor(max_workers=len(wahl)) as pool:
            list(pool.map(lambda n: anbieter_lauf(n, tmp), wahl))


main()
