"""Erzeugt einen 30-Woerter-Satz in Voicebox und misst die Audiolaenge."""
import json
import time
import urllib.request
import wave
from pathlib import Path

API = "http://127.0.0.1:17493"
PROFILE = "57af6748-db9f-4821-a1e0-0a37c0c8ba52"
SATZ = (
    "Wenn du heute Abend in Ruhe auf diesen Tag zurückblickst und dich ehrlich "
    "fragst, was dir wirklich gutgetan hat, welcher einzelne Moment kommt dir "
    "dann als Erstes in den Sinn?"
)

print(f"Woerter: {len(SATZ.split())}")

body = json.dumps(
    {"text": SATZ, "profile_id": PROFILE, "language": "de", "model_size": "0.6B"}
).encode("utf-8")
req = urllib.request.Request(
    f"{API}/generate", data=body, headers={"Content-Type": "application/json"}
)

started = time.perf_counter()
try:
    with urllib.request.urlopen(req, timeout=60) as response:
        created = json.load(response)
except urllib.error.HTTPError as error:
    print("HTTP-Fehler:", error.code)
    print(error.read().decode("utf-8", "replace")[:1500])
    raise SystemExit(1)

gen_id = created["id"]
print(f"Auftrag: {gen_id} | Modell: {created.get('model_size')}")

audio = Path.home() / "AppData/Roaming/sh.voicebox.app/generations" / f"{gen_id}.wav"
for _ in range(240):
    time.sleep(2)
    with urllib.request.urlopen(f"{API}/history/{gen_id}", timeout=15) as response:
        state = json.load(response)
    if state.get("status") in {"completed", "failed", "error"}:
        break

elapsed = time.perf_counter() - started
print(f"Status: {state.get('status')} nach {elapsed:.1f} s")
if state.get("error"):
    print("Fehler:", state["error"])

if audio.exists():
    with wave.open(str(audio), "rb") as handle:
        seconds = handle.getnframes() / handle.getframerate()
    print(f"Audiolaenge: {seconds:.2f} s  ({handle.getframerate()} Hz)")
    print(f"=> Talker-Schritte bei 12 Hz: {round(seconds * 12)}")
    print(f"=> Echtzeitfaktor dieser PC-Generierung: {seconds / elapsed:.2f} x")
else:
    print("Keine Audiodatei gefunden:", audio)
