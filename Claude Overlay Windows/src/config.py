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
    grok_api_key: str
    grok_whisper_model: str
    grok_whisper_url: str
    gemini_api_key: str
    gemini_model: str
    audio_sample_rate: int
    audio_channels: int
    claude_process_names: List[str]

    @classmethod
    def load(cls, env_path: Path | None = None) -> "Settings":
        if env_path is None:
            env_path = Path(__file__).resolve().parents[1] / ENV_FILENAME

        _load_dotenv_file(env_path)

        process_names_raw = os.getenv(
            "CLAUDE_PROCESS_NAMES", "Claude.exe,Claude Desktop.exe,claude.exe"
        )
        process_names = [
            item.strip().lower()
            for item in process_names_raw.split(",")
            if item.strip()
        ]

        return cls(
            grok_api_key=os.getenv("GROK_API_KEY", ""),
            grok_whisper_model=os.getenv("GROK_WHISPER_MODEL", "grok-2-whisper-1"),
            grok_whisper_url=os.getenv(
                "GROK_WHISPER_URL", "https://api.x.ai/v1/audio/transcriptions"
            ),
            gemini_api_key=os.getenv("GEMINI_API_KEY", ""),
            gemini_model=os.getenv("GEMINI_MODEL", "gemini-2.0-flash"),
            audio_sample_rate=int(os.getenv("AUDIO_SAMPLE_RATE", "16000")),
            audio_channels=int(os.getenv("AUDIO_CHANNELS", "1")),
            claude_process_names=process_names,
        )

    def validate(self) -> None:
        missing = []
        if not self.grok_api_key:
            missing.append("GROK_API_KEY")
        if not self.gemini_api_key:
            missing.append("GEMINI_API_KEY")

        if missing:
            raise ValueError(
                "Fehlende Umgebungsvariablen: " + ", ".join(missing)
            )
