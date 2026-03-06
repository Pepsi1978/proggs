from __future__ import annotations

import json
import logging
import re
import tempfile
import time
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional

import numpy as np
import requests
import soundfile as sf

from config import Settings

log = logging.getLogger(__name__)

# Groq Whisper max. Dateigroesse: 25 MB.  Wir bleiben bei 20 MB fuer Sicherheit.
_MAX_CHUNK_BYTES = 20 * 1024 * 1024

# Retry-faehige HTTP-Statuscodes
_RETRYABLE_STATUS_CODES = {429, 500, 503}


def _split_audio(audio_path: Path, max_bytes: int = _MAX_CHUNK_BYTES) -> List[Path]:
    """Teilt eine WAV-Datei in Chunks auf, die jeweils unter max_bytes liegen."""
    file_size = audio_path.stat().st_size
    if file_size <= max_bytes:
        return [audio_path]

    data, sr = sf.read(str(audio_path), dtype="float32")
    # Bytes pro Sample: float32 = 4 Bytes * Kanaele + WAV-Header-Overhead
    channels = 1 if data.ndim == 1 else data.shape[1]
    bytes_per_sample = 4 * channels
    # WAV-Header ~44 Bytes, etwas Puffer
    samples_per_chunk = (max_bytes - 1024) // bytes_per_sample

    chunks: List[Path] = []
    total_samples = len(data)
    offset = 0

    while offset < total_samples:
        end = min(offset + samples_per_chunk, total_samples)
        chunk_data = data[offset:end]

        tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
        tmp_path = Path(tmp.name)
        tmp.close()
        sf.write(str(tmp_path), chunk_data, sr)
        chunks.append(tmp_path)
        offset = end

    log.info("Audio aufgeteilt: %d Chunks (%.1f MB gesamt)", len(chunks), file_size / 1024 / 1024)
    return chunks


_HALLUCINATION_PATTERNS = [
    "vielen dank",
    "danke fuers zuschauen",
    "danke fürs zuschauen",
    "untertitel von",
    "untertitel der amara",
    "copyright",
    "thank you",
    "thanks for watching",
    "subtitles by",
    "bis zum naechsten mal",
    "bis zum nächsten mal",
    "tschuess",
    "tschüss",
]


def _is_hallucination(text: str) -> bool:
    """Prueft ob der Text eine bekannte Whisper-Halluzination ist."""
    if not text:
        return True
    lower = text.lower().strip(" .!,")
    # Nur kurze Texte pruefen (echte Halluzinationen sind typischerweise kurz)
    if len(lower) > 60:
        return False
    return any(lower.startswith(p) or lower == p for p in _HALLUCINATION_PATTERNS)


def _transcribe_single(audio_path: Path, settings: Settings) -> str:
    """Transkribiert eine einzelne WAV-Datei (muss unter 25 MB sein)."""
    headers = {
        "Authorization": f"Bearer {settings.groq_api_key}",
    }
    data = {
        "model": settings.whisper_model,
        "language": settings.whisper_lang,
        "response_format": "json",
        "temperature": "0",
    }

    max_retries = 3
    delays = [2, 4, 8]

    for attempt in range(max_retries + 1):
        with audio_path.open("rb") as audio_file:
            files = {
                "file": (audio_path.name, audio_file, "audio/wav"),
            }
            response = requests.post(
                settings.whisper_url,
                headers=headers,
                files=files,
                data=data,
                timeout=180,
            )

        if response.ok:
            break

        if response.status_code in _RETRYABLE_STATUS_CODES and attempt < max_retries:
            wait = delays[attempt]
            log.info(
                "Groq %d - Versuch %d/%d, warte %ds...",
                response.status_code, attempt + 1, max_retries, wait,
            )
            time.sleep(wait)
            continue

        raise RuntimeError(
            f"Groq API Fehler {response.status_code}: {response.text}"
        )

    payload = response.json()

    text = payload.get("text") or payload.get("transcript") or ""
    text = text.strip()

    # Bekannte Whisper-Halluzinationen herausfiltern
    if _is_hallucination(text):
        return ""

    if not text:
        raise RuntimeError(f"Unerwartete Grok-Whisper-Antwort: {payload}")

    return text


def transcribe_with_grok(audio_path: Path, settings: Settings) -> str:
    """Sendet eine WAV-Datei an die Grok Whisper API und gibt den transkribierten Text zurueck.

    Grosse Dateien (>20 MB) werden automatisch in Chunks aufgeteilt.
    """
    chunks = _split_audio(audio_path)
    try:
        parts = [_transcribe_single(chunk, settings) for chunk in chunks]
    finally:
        # Temp-Chunks aufraeumen (nicht die Original-Datei)
        for chunk in chunks:
            if chunk != audio_path:
                try:
                    chunk.unlink(missing_ok=True)
                except Exception:
                    pass

    # Leere Teile (gefilterte Halluzinationen) entfernen
    parts = [p for p in parts if p]
    return " ".join(parts).strip()


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


def improve_text_with_gemini(
    transcript: str,
    settings: Settings,
    status_callback: Optional[Callable[[str], None]] = None,
) -> Dict[str, Any]:
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

    generation_config: Dict[str, Any] = {
        "maxOutputTokens": 2048,
    }

    # thinkingConfig nur fuer Gemini 3.x Modelle hinzufuegen
    if "gemini-3" in settings.gemini_model:
        generation_config["thinkingConfig"] = {
            "thinkingLevel": settings.gemini_thinking_level,
        }
    else:
        generation_config["temperature"] = 0.2

    payload = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": prompt}],
            }
        ],
        "generationConfig": generation_config,
    }

    max_retries = 5
    delays = [2, 4, 8, 16, 32]
    last_status = 0

    for attempt in range(max_retries + 1):
        response = requests.post(url, params=params, json=payload, timeout=120)

        if response.ok:
            break

        last_status = response.status_code

        if last_status in _RETRYABLE_STATUS_CODES and attempt < max_retries:
            wait = delays[attempt]
            if status_callback:
                status_callback(f"API Retry {attempt + 1}/{max_retries} ({wait}s)")
            log.info(
                "Gemini %d - Versuch %d/%d, warte %ds...",
                last_status, attempt + 1, max_retries, wait,
            )
            time.sleep(wait)
            continue

        # Nicht retrybar oder alle Versuche aufgebraucht
        if last_status == 429:
            raise RuntimeError("Gemini API ueberlastet (Rate Limit) - alle Versuche fehlgeschlagen.")
        elif last_status == 503:
            raise RuntimeError("Gemini API nicht erreichbar - alle Versuche fehlgeschlagen.")
        elif last_status == 500:
            raise RuntimeError("Gemini API Serverfehler - alle Versuche fehlgeschlagen.")
        else:
            raise RuntimeError(f"Gemini API Fehler {last_status}")

    data = response.json()
    try:
        parts = data["candidates"][0]["content"]["parts"]
        # Thinking-Parts ueberspringen (thought: true), nur normalen Text extrahieren
        text = ""
        for part in parts:
            if part.get("thought"):
                continue
            if "text" in part:
                text = part["text"]
                break
        if not text:
            raise ValueError("Kein Text-Part in der Antwort")
    except (KeyError, IndexError, TypeError, ValueError) as exc:
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
