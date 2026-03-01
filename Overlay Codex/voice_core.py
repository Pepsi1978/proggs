"""
Codex Desktop Voice Input - Shared Core Module
Spracheingabe-zu-Text Transkription mit Whisper (lokal).
"""

import io
import wave
import threading
import numpy as np

# Audio-Konfiguration
SAMPLE_RATE = 16000
CHANNELS = 1
DTYPE = np.int16

# Whisper model cache
_whisper_model = None
_model_lock = threading.Lock()


def get_whisper_model(model_size="base"):
    """Lädt das Whisper-Modell (cached, thread-safe).

    Versucht zuerst faster-whisper (schneller), fällt auf openai-whisper zurück.
    Modell-Größen: tiny, base, small, medium, large
    - tiny:   ~1GB RAM, schnellste Transkription, geringste Qualität
    - base:   ~1GB RAM, guter Kompromiss für Deutsch
    - small:  ~2GB RAM, deutlich bessere Qualität
    - medium: ~5GB RAM, sehr gute Qualität
    - large:  ~10GB RAM, beste Qualität
    """
    global _whisper_model

    with _model_lock:
        if _whisper_model is not None:
            return _whisper_model

        # Versuche faster-whisper (empfohlen)
        try:
            from faster_whisper import WhisperModel
            print(f"Lade faster-whisper Modell '{model_size}'...")
            _whisper_model = ("faster", WhisperModel(
                model_size,
                device="cpu",
                compute_type="int8"
            ))
            print("faster-whisper geladen.")
            return _whisper_model
        except ImportError:
            pass

        # Fallback: openai-whisper
        try:
            import whisper
            print(f"Lade openai-whisper Modell '{model_size}'...")
            _whisper_model = ("openai", whisper.load_model(model_size))
            print("openai-whisper geladen.")
            return _whisper_model
        except ImportError:
            pass

        raise RuntimeError(
            "Weder 'faster-whisper' noch 'openai-whisper' installiert.\n"
            "Installiere eines davon:\n"
            "  pip install faster-whisper\n"
            "  pip install openai-whisper"
        )


def transcribe_audio(audio_data, language="de", model_size="base"):
    """Transkribiert Audio-Daten (numpy int16 array) zu Text.

    Args:
        audio_data: numpy array mit int16 Audio-Samples (16kHz, mono)
        language: Sprachcode (z.B. "de", "en")
        model_size: Whisper Modell-Größe

    Returns:
        Transkribierter Text als String
    """
    if len(audio_data) == 0:
        return ""

    engine, model = get_whisper_model(model_size)

    if engine == "faster":
        # faster-whisper erwartet float32
        audio_float = audio_data.astype(np.float32) / 32768.0
        segments, _ = model.transcribe(
            audio_float,
            language=language,
            beam_size=5,
            vad_filter=True,
        )
        text = " ".join(seg.text.strip() for seg in segments)
    else:
        # openai-whisper erwartet float32
        audio_float = audio_data.astype(np.float32) / 32768.0
        result = model.transcribe(
            audio_float,
            language=language,
            fp16=False,
        )
        text = result["text"].strip()

    return text


def audio_to_wav_bytes(audio_data):
    """Konvertiert numpy int16 array zu WAV-Bytes (für Debug/Export)."""
    buf = io.BytesIO()
    with wave.open(buf, "wb") as wf:
        wf.setnchannels(CHANNELS)
        wf.setsampwidth(2)  # int16 = 2 bytes
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(audio_data.tobytes())
    return buf.getvalue()


class AudioRecorder:
    """Nimmt Audio vom Mikrofon auf (thread-safe start/stop)."""

    def __init__(self):
        self.is_recording = False
        self._frames = []
        self._stream = None
        self._audio_interface = None
        self._lock = threading.Lock()

    def start(self):
        """Startet die Aufnahme."""
        import sounddevice as sd

        with self._lock:
            if self.is_recording:
                return
            self._frames = []
            self.is_recording = True

        def callback(indata, frames, time_info, status):
            if self.is_recording:
                self._frames.append(indata.copy())

        self._stream = sd.InputStream(
            samplerate=SAMPLE_RATE,
            channels=CHANNELS,
            dtype="int16",
            callback=callback,
            blocksize=1024,
        )
        self._stream.start()

    def stop(self):
        """Stoppt die Aufnahme und gibt die Audio-Daten zurück.

        Returns:
            numpy array mit int16 Audio-Samples
        """
        with self._lock:
            self.is_recording = False

        if self._stream is not None:
            self._stream.stop()
            self._stream.close()
            self._stream = None

        if not self._frames:
            return np.array([], dtype=DTYPE)

        audio = np.concatenate(self._frames, axis=0).flatten()
        self._frames = []
        return audio
