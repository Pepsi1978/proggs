from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any, Dict

import requests

from config import Settings


def transcribe_with_grok(audio_path: Path, settings: Settings) -> str:
    """Sendet eine WAV-Datei an die Grok Whisper API und gibt den transkribierten Text zurueck."""
    headers = {
        "Authorization": f"Bearer {settings.groq_api_key}",
    }

    with audio_path.open("rb") as audio_file:
        files = {
            "file": (audio_path.name, audio_file, "audio/wav"),
        }
        data = {
            "model": settings.whisper_model,
            "language": settings.whisper_lang,
            "response_format": "json",
            "temperature": "0",
        }

        response = requests.post(
            settings.whisper_url,
            headers=headers,
            files=files,
            data=data,
            timeout=120,
        )

    if not response.ok:
        raise RuntimeError(
            f"Groq API Fehler {response.status_code}: {response.text}"
        )
    payload = response.json()

    text = payload.get("text") or payload.get("transcript") or ""
    if not text.strip():
        raise RuntimeError(f"Unerwartete Grok-Whisper-Antwort: {payload}")

    return text.strip()


def _extract_json_block(text: str) -> Dict[str, Any]:
    """Extrahiert ein JSON-Objekt aus einem Text (direkt, fenced oder innerhalb von Klammern)."""
    direct = text.strip()
    try:
        return json.loads(direct)
    except json.JSONDecodeError:
        pass

    fenced = re.search(r"```json\s*(\{.*?\})\s*```", text, re.DOTALL)
    if fenced:
        return json.loads(fenced.group(1))

    brace = re.search(r"(\{.*\})", text, re.DOTALL)
    if brace:
        return json.loads(brace.group(1))

    raise ValueError("Keine JSON-Struktur in Gemini-Antwort gefunden.")


def improve_text_with_gemini(transcript: str, settings: Settings) -> Dict[str, Any]:
    """Sendet den transkribierten Text an die Gemini API zur Intentionserkennung und Textverbesserung."""
    prompt = (
        "Du bist ein Sprachassistent fuer Claude Code. "
        "Analysiere die Intentionen in der Spracheingabe und formuliere daraus "
        "eine klare, natuerlich klingende, hochwertige deutsche Eingabe, die alle "
        "Intentionen vollstaendig und praezise transportiert. "
        "Gib ausschliesslich JSON im Format "
        '{"intentionen": ["..."], "verbesserter_text": "..."}.\n\n'
        f"Spracheingabe:\n{transcript}"
    )

    url = (
        "https://generativelanguage.googleapis.com/v1beta/"
        f"models/{settings.gemini_model}:generateContent"
    )
    params = {"key": settings.gemini_api_key}
    payload = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": prompt}],
            }
        ],
        "generationConfig": {
            "temperature": 0.2,
            "maxOutputTokens": 800,
        },
    }

    response = requests.post(url, params=params, json=payload, timeout=120)

    if not response.ok:
        status = response.status_code
        if status == 429:
            raise RuntimeError("Gemini API ueberlastet (Rate Limit). Bitte kurz warten.")
        elif status == 503:
            raise RuntimeError("Gemini API voruebergehend nicht erreichbar. Bitte spaeter versuchen.")
        elif status == 500:
            raise RuntimeError("Gemini API interner Serverfehler. Bitte erneut versuchen.")
        else:
            raise RuntimeError(f"Gemini API Fehler {status}")

    data = response.json()
    try:
        text = data["candidates"][0]["content"]["parts"][0]["text"]
    except (KeyError, IndexError, TypeError) as exc:
        raise RuntimeError(f"Unerwartete Gemini-Antwort: {data}") from exc

    result = _extract_json_block(text)
    improved = str(result.get("verbesserter_text", "")).strip()
    intentions = result.get("intentionen", [])

    if not improved:
        raise RuntimeError(f"Gemini lieferte keinen verbesserten Text: {result}")

    return {
        "intentionen": intentions if isinstance(intentions, list) else [],
        "verbesserter_text": improved,
        "roh_transkript": transcript,
    }
