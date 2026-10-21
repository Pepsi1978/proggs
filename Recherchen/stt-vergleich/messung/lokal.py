"""Lokaler Teil des Messplatzes: offene Modelle über onnx-asr auf der CPU des Laptops.

Aufruf (in einer venv mit `pip install "onnx-asr[cpu,hub]" soundfile`):
  python -I messung/lokal.py
Ergebnis: messung/ergebnisse-lokal.json, gleiches Format wie ergebnisse.json.
"""
import json, pathlib, subprocess, tempfile, time

import onnx_asr

BASIS = pathlib.Path(__file__).resolve().parent
TTS_PROBEN = BASIS.parent.parent / "tts-vergleich" / "proben"
ZIEL = BASIS / "ergebnisse-lokal.json"
MODELLE = {"lokal-parakeet-tdt-0.6b-v3": ("nemo-parakeet-tdt-0.6b-v3", "int8"),
           "lokal-canary-1b-v2": ("nemo-canary-1b-v2", "int8")}


def aufnahmen():
    dateien = [p for p in sorted(TTS_PROBEN.glob("*.mp3")) if not p.name.endswith("-b.mp3")]
    return dateien + sorted((BASIS / "audio").glob("*.mp3")) + sorted((BASIS / "audio").glob("*.wav"))


def main():
    ergebnisse = {}
    with tempfile.TemporaryDirectory() as tmp:
        for name, (kennung, quant) in MODELLE.items():
            try:
                modell = onnx_asr.load_model(kennung, quantization=quant)
            except Exception as e:
                print(name, "nicht ladbar:", e)
                continue
            ergebnisse[name] = {}
            for datei in aufnahmen():
                wav = pathlib.Path(tmp) / (datei.stem + ".wav")
                if not wav.exists():
                    subprocess.run(["ffmpeg", "-v", "quiet", "-y", "-i", str(datei), "-ar", "16000", "-ac", "1", str(wav)], check=True)
                start = time.perf_counter()
                try:
                    text, fehler = modell.recognize(str(wav), language="de") if "canary" in kennung else modell.recognize(str(wav)), None
                except Exception as e:
                    text, fehler = "", str(e)[:300]
                ergebnisse[name][datei.name] = {"text": text, "sekunden": round(time.perf_counter() - start, 2), "fehler": fehler}
                print(f"{name:30} {datei.name:50} {fehler or text[:70]}", flush=True)
            ZIEL.write_text(json.dumps(ergebnisse, ensure_ascii=False, indent=1), encoding="utf-8")


main()
