"""Registriert Franks geklonte Stimme bei Alibaba Model Studio und misst die Sprachausgabe.

Prueft drei Dinge, bevor die App umgebaut wird:
  1. Klingt die geklonte Stimme ueber die Cloud so gut wie lokal in Voicebox?
  2. Wie lange dauert ein Satz wirklich (Antwortzeit inklusive Netzwerk)?
  3. Klingt derselbe Satz bei Wiederholung anders, oder ist die Ausgabe deterministisch?

Aufruf:
    python tools/qwen_voice_test.py

Der API-Schluessel wird aus $HOME/SK/PerfectMoment/dashscope.key gelesen (Singapur-Region).
Die einmal erzeugte voice_id wird daneben in qwen-voice-id.txt zwischengespeichert, damit
nicht bei jedem Lauf eine neue Stimme registriert wird.
"""
import base64
import hashlib
import json
import mimetypes
import pathlib
import sys
import time
import urllib.error
import urllib.request

REGION_BASE = "https://dashscope-intl.aliyuncs.com/api/v1"
ENROLL_URL = f"{REGION_BASE}/services/audio/tts/customization"
SYNTH_URL = f"{REGION_BASE}/services/aigc/multimodal-generation/generation"

# Voice cloning and synthesis must use the exact same model, otherwise synthesis fails.
TARGET_MODEL = "qwen3-tts-vc-2026-01-22"
PREFERRED_NAME = "frank"
LANGUAGE = "German"

SK_DIR = pathlib.Path.home() / "SK" / "PerfectMoment"
KEY_FILE = SK_DIR / "dashscope.key"
VOICE_ID_FILE = SK_DIR / "qwen-voice-id.txt"

# The reference recording Frank made in Voicebox: 20.8 s, 24 kHz, mono, 16 bit.
REFERENCE_AUDIO = (
    pathlib.Path.home()
    / "AppData/Roaming/sh.voicebox.app/profiles"
    / "57af6748-db9f-4821-a1e0-0a37c0c8ba52"
    / "158c06e4-9c7d-4ed4-a816-c29c8d9154e5.wav"
)

OUT_DIR = pathlib.Path(__file__).resolve().parent / "qwen-voice-test-output"

SAETZE = [
    "Was war heute der schönste Moment für dich?",
    "Wenn du heute Abend in Ruhe zurückblickst, welcher Augenblick kommt dir als Erstes in den Sinn?",
    "Wofür bist du gerade dankbar?",
]


def log(msg: str) -> None:
    print(msg, flush=True)


# The Windows console defaults to cp1252, which mangles German umlauts.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")


def read_key() -> str:
    if not KEY_FILE.exists():
        log(f"FEHLER: Kein Schlüssel gefunden unter {KEY_FILE}")
        log("Lege dort deinen Model-Studio-Schlüssel der Singapur-Region ab (beginnt mit sk-).")
        sys.exit(1)
    key = KEY_FILE.read_text(encoding="utf-8").strip()
    if not key:
        log(f"FEHLER: {KEY_FILE} ist leer.")
        sys.exit(1)
    return key


def post(url: str, payload: dict, key: str, timeout: int = 120) -> dict:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")
        raise RuntimeError(f"HTTP {error.code}: {body[:1200]}") from error


def enroll_voice(key: str) -> str:
    """Registers the reference recording once and returns the voice id."""
    if VOICE_ID_FILE.exists():
        voice_id = VOICE_ID_FILE.read_text(encoding="utf-8").strip()
        if voice_id:
            log(f"Vorhandene Stimme wird benutzt: {voice_id}")
            return voice_id

    if not REFERENCE_AUDIO.exists():
        log(f"FEHLER: Referenzaufnahme nicht gefunden: {REFERENCE_AUDIO}")
        sys.exit(1)

    raw = REFERENCE_AUDIO.read_bytes()
    mime = mimetypes.guess_type(REFERENCE_AUDIO.name)[0] or "audio/wav"
    log(f"Registriere Stimme aus {REFERENCE_AUDIO.name} ({len(raw) / 1024:.0f} KB, {mime}) …")

    data_uri = f"data:{mime};base64,{base64.b64encode(raw).decode()}"
    started = time.perf_counter()
    result = post(
        ENROLL_URL,
        {
            "model": "qwen-voice-enrollment",
            "input": {
                "action": "create",
                "target_model": TARGET_MODEL,
                "preferred_name": PREFERRED_NAME,
                "audio": {"data": data_uri},
            },
        },
        key,
    )
    elapsed = time.perf_counter() - started

    voice_id = result.get("output", {}).get("voice")
    if not voice_id:
        log("Unerwartete Antwort beim Registrieren:")
        log(json.dumps(result, ensure_ascii=False, indent=2)[:1500])
        sys.exit(1)

    SK_DIR.mkdir(parents=True, exist_ok=True)
    VOICE_ID_FILE.write_text(voice_id, encoding="utf-8")
    log(f"Stimme registriert in {elapsed:.1f} s: {voice_id}")
    log(f"(gemerkt in {VOICE_ID_FILE})")
    return voice_id


def extract_audio(result: dict) -> bytes:
    """Pulls the audio out of the response, no matter whether it arrives as URL or inline."""
    audio = result.get("output", {}).get("audio")
    if isinstance(audio, dict):
        if audio.get("url"):
            with urllib.request.urlopen(audio["url"], timeout=60) as response:
                return response.read()
        if audio.get("data"):
            payload = audio["data"]
            if payload.startswith("data:"):
                payload = payload.split(",", 1)[1]
            return base64.b64decode(payload)
    raise RuntimeError(
        "Kein Audio in der Antwort:\n" + json.dumps(result, ensure_ascii=False, indent=2)[:1200]
    )


def synthesize(text: str, voice_id: str, key: str, label: str) -> tuple[bytes, float, dict]:
    started = time.perf_counter()
    result = post(
        SYNTH_URL,
        {
            "model": TARGET_MODEL,
            "input": {"text": text, "voice": voice_id, "language_type": LANGUAGE},
        },
        key,
    )
    audio = extract_audio(result)
    elapsed = time.perf_counter() - started
    log(f"  {label}: {elapsed:5.2f} s | {len(audio) / 1024:6.0f} KB | sha256={hashlib.sha256(audio).hexdigest()[:16]}")
    return audio, elapsed, result.get("usage", {})


def main() -> None:
    key = read_key()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    voice_id = enroll_voice(key)

    log("")
    log("Sprachausgabe — Antwortzeit je Satz")
    zeiten = []
    letzte_usage = {}
    for index, satz in enumerate(SAETZE, start=1):
        audio, elapsed, usage = synthesize(satz, voice_id, key, f"Satz {index}")
        (OUT_DIR / f"satz{index}.mp3").write_bytes(audio)
        zeiten.append(elapsed)
        letzte_usage = usage or letzte_usage

    log("")
    log("Variations-Test — derselbe Satz dreimal")
    hashes = []
    for lauf in range(1, 4):
        audio, _, _ = synthesize(SAETZE[0], voice_id, key, f"Lauf {lauf}")
        (OUT_DIR / f"variation{lauf}.mp3").write_bytes(audio)
        hashes.append(hashlib.sha256(audio).hexdigest())

    log("")
    log("─" * 60)
    log(f"Antwortzeit: schnellster {min(zeiten):.2f} s, langsamster {max(zeiten):.2f} s, "
        f"Mittel {sum(zeiten) / len(zeiten):.2f} s")
    if len(set(hashes)) == 1:
        log("Variation: IDENTISCH — die Cloud liest jeden Satz gleich vor.")
        log("           Lebendigkeit müsste über Instruction-Tags erzeugt werden.")
    else:
        log(f"Variation: UNTERSCHIEDLICH ({len(set(hashes))} von 3 verschieden) — "
            "jeder Satz wird neu interpretiert.")
    if letzte_usage:
        log(f"Verbrauch laut API: {json.dumps(letzte_usage, ensure_ascii=False)}")
    log(f"Audiodateien zum Anhören: {OUT_DIR}")


if __name__ == "__main__":
    main()
