from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import List


ENV_FILENAME = ".env"


def _load_dotenv_file(path: Path) -> None:
    """Laedt Schluessel-Wert-Paare aus einer .env-Datei in os.environ."""
    if not path.exists():
        return

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


@dataclass(frozen=True)
class Settings:
    groq_api_key: str
    whisper_model: str
    whisper_url: str
    whisper_lang: str
    gemini_api_key: str
    gemini_model: str
    gemini_thinking_level: str
    audio_sample_rate: int
    audio_channels: int
    claude_process_names: List[str]

    @classmethod
    def load(cls, env_path: Path | None = None) -> "Settings":
        if env_path is None:
            env_path = Path(__file__).resolve().parents[1] / ENV_FILENAME

        _load_dotenv_file(env_path)

        process_names_raw = os.getenv(
            "CLAUDE_PROCESS_NAMES", "Claude"
        )
        process_names = [
            item.strip().lower()
            for item in process_names_raw.split(",")
            if item.strip()
        ]

        return cls(
            groq_api_key=os.getenv("GROQ_API_KEY", ""),
            whisper_model=os.getenv("WHISPER_MODEL", "whisper-large-v3"),
            whisper_url=os.getenv(
                "WHISPER_URL", "https://api.groq.com/openai/v1/audio/transcriptions"
            ),
            whisper_lang=os.getenv("WHISPER_LANG", "de"),
            gemini_api_key=os.getenv("GEMINI_API_KEY", ""),
            gemini_model=os.getenv("GEMINI_MODEL", ""),
            gemini_thinking_level=os.getenv("GEMINI_THINKING_LEVEL", ""),
            audio_sample_rate=int(os.getenv("AUDIO_SAMPLE_RATE", "16000")),
            audio_channels=int(os.getenv("AUDIO_CHANNELS", "1")),
            claude_process_names=process_names,
        )

    @property
    def gemini_available(self) -> bool:
        """Prueft ob Gemini vollstaendig konfiguriert ist (API-Key und Modell)."""
        return bool(self.gemini_api_key and self.gemini_model)

    def validate(self) -> None:
        missing = []
        if not self.groq_api_key:
            missing.append("GROQ_API_KEY")

        if missing:
            raise ValueError(
                "Fehlende Umgebungsvariablen: " + ", ".join(missing)
            )
