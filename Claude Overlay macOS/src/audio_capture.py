from __future__ import annotations

import tempfile
import threading
from pathlib import Path
from typing import List

import numpy as np
import sounddevice as sd
import soundfile as sf


class AudioRecorder:
    """Nimmt Audio vom Standard-Mikrofon auf und speichert es als WAV-Datei."""

    def __init__(self, sample_rate: int = 16000, channels: int = 1) -> None:
        self.sample_rate = sample_rate
        self.channels = channels
        self._stream: sd.InputStream | None = None
        self._frames: List[np.ndarray] = []
        self._lock = threading.Lock()
        self._recording = False

    @property
    def is_recording(self) -> bool:
        return self._recording

    def start(self) -> None:
        if self._recording:
            return

        self._frames = []

        def callback(
            indata: np.ndarray,
            _frames: int,
            _time: object,
            status: sd.CallbackFlags,
        ) -> None:
            if status:
                pass
            with self._lock:
                self._frames.append(indata.copy())

        self._stream = sd.InputStream(
            samplerate=self.sample_rate,
            channels=self.channels,
            dtype="float32",
            callback=callback,
        )
        self._stream.start()
        self._recording = True

    def stop(self) -> Path:
        """Stoppt die Aufnahme und gibt den Pfad zur WAV-Datei zurueck."""
        if not self._recording or self._stream is None:
            raise RuntimeError("Es laeuft gerade keine Aufnahme.")

        self._stream.stop()
        self._stream.close()
        self._stream = None
        self._recording = False

        with self._lock:
            if not self._frames:
                raise RuntimeError("Keine Audiodaten aufgenommen.")
            audio = np.concatenate(self._frames, axis=0)

        tmp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
        tmp_path = Path(tmp_file.name)
        tmp_file.close()
        sf.write(str(tmp_path), audio, self.sample_rate)
        return tmp_path
