"""Zeigt alle bei Alibaba registrierten Stimmen des Kontos.

Ersatz fuer die Konsole: Alibaba dokumentiert die Verwaltung geklonter Stimmen nur ueber die
API, eine Seite in der Weboberflaeche ist nirgends beschrieben.

Aufruf:
    python tools/qwen_voices.py
"""
import sys

from qwen_voice_test import log, post, read_credentials


def main() -> None:
    key, base = read_credentials()
    try:
        result = post(
            f"{base}/services/audio/tts/customization",
            {"model": "qwen-voice-enrollment", "input": {"action": "list"}},
            key,
            timeout=45,
        )
    except RuntimeError as error:
        log("FEHLER beim Abruf:")
        log(str(error)[:600])
        sys.exit(1)

    output = result.get("output", {})
    voices = output.get("voice_list", [])
    log("")
    log(f"{output.get('total_count', len(voices))} Stimmen im Konto")
    log("─" * 74)
    for voice in voices:
        voice_id = voice.get("voice", "")
        name = voice_id.split("qwen-tts-vc-")[-1].split("-voice-")[0] if voice_id else "?"
        log(f"{name:14} {voice.get('gmt_create', '')}   Sprache: {voice.get('language', '?')}")
        log(f"{'':14} {voice_id}")
    log("")
    log("In der App wählbar unter: Einstellungen > Stimme")


if __name__ == "__main__":
    main()
