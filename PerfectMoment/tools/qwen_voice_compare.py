"""Stellt mehrere Referenzaufnahmen gegeneinander, damit die beste per Gehoer gewinnt.

Registriert jede uebergebene Audiodatei als eigene Stimme bei Alibaba Model Studio und laesst
alle Varianten dieselben Saetze sprechen. Die bereits eingerichtete Stimme tritt automatisch
mit an, sodass eine neue Aufnahme immer gegen den aktuellen Stand antritt.

Aufruf:
    python tools/qwen_voice_compare.py <audio1.wav> [audio2.wav ...]

Die Hoerproben landen in tools/qwen-voice-test-output/ und heissen <variante>_<satz>.wav.
Am Ende steht, welche voice_id zu welcher Variante gehoert — die gewuenschte traegt man
anschliessend in der App unter Einstellungen > Stimme > Stimm-Kennung ein.
"""
import pathlib
import sys
import time

from qwen_voice_test import (
    LANGUAGE,
    OUT_DIR,
    PREFERRED_NAME,
    TARGET_MODEL,
    VOICE_ID_FILE,
    audio_suffix,
    enroll_voice_from,
    log,
    preflight,
    read_credentials,
    synthesize,
)

SAETZE = [
    ("frage", "Was war heute der schönste Moment für dich?"),
    ("lang", "Und wenn du heute Abend in Ruhe auf diesen Tag zurückblickst, "
             "welcher einzelne Augenblick kommt dir dann als Erstes in den Sinn?"),
]


def main() -> None:
    dateien = [pathlib.Path(a) for a in sys.argv[1:]]
    if not dateien:
        log("Aufruf: python tools/qwen_voice_compare.py <audio1.wav> [audio2.wav ...]")
        sys.exit(1)
    fehlend = [d for d in dateien if not d.exists()]
    if fehlend:
        for d in fehlend:
            log(f"FEHLER: Datei nicht gefunden: {d}")
        sys.exit(1)

    key, base = read_credentials()
    preflight(key, base)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    varianten: list[tuple[str, str]] = []

    # Die bereits eingerichtete Stimme tritt als Vergleichsmassstab mit an.
    if VOICE_ID_FILE.exists():
        bestehend = VOICE_ID_FILE.read_text(encoding="utf-8").strip()
        if bestehend:
            varianten.append(("bisher", bestehend))
            log(f"bisher      -> {bestehend}")

    for datei in dateien:
        name = datei.stem
        # Alibaba lehnt alles ab, was nicht alphanumerisch ist - "Frank-HD" gibt HTTP 400.
        # Der Dateiname bestimmt die Kennung, denn aus ihr baut die App den Anzeigenamen.
        kennung = "".join(c for c in name if c.isalnum()) or PREFERRED_NAME
        log(f"Registriere '{name}' aus {datei.name} …")
        started = time.perf_counter()
        voice_id = enroll_voice_from(datei, key, base, preferred_name=kennung)
        log(f"{name:11} -> {voice_id}  ({time.perf_counter() - started:.1f} s)")
        varianten.append((name, voice_id))

    log("")
    log(f"Hörproben — {len(varianten)} Varianten x {len(SAETZE)} Sätze")
    for name, voice_id in varianten:
        for satz_id, satz in SAETZE:
            audio, _, _ = synthesize(satz, voice_id, key, base, f"{name:11} {satz_id}")
            (OUT_DIR / f"{name}_{satz_id}{audio_suffix(audio)}").write_bytes(audio)

    log("")
    log("─" * 64)
    log("Variante    voice_id")
    for name, voice_id in varianten:
        log(f"{name:11} {voice_id}")
    log("")
    log(f"Hörproben liegen in: {OUT_DIR}")
    log("Die gewünschte Kennung in der App unter Einstellungen > Stimme eintragen.")


if __name__ == "__main__":
    main()
