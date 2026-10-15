"""Erzeugt Audio-Testfälle (TTS-Sprache + synthetisches Fahrgeräusch), rechnet Silero VAD v5 genau wie
SileroVad.kt (64 Werte Kontext + 512, Zustand [2,1,128]) und schreibt die Wahrscheinlichkeiten je 32-ms-Rahmen
als Testdaten für die JVM-Tests von Jarvis."""
import sys, wave, os
import numpy as np
import onnxruntime as ort

ORDNER, ZIEL = sys.argv[1], sys.argv[2]
SR = 16000
rng = np.random.default_rng(42)


def lade(name):
    with wave.open(os.path.join(ORDNER, name)) as w:
        assert w.getframerate() == SR and w.getnchannels() == 1
        x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32) / 32768
    # Ränder der TTS-Datei (Stille) abschneiden, damit die Sprachzeiten exakt bekannt sind.
    idx = np.where(np.abs(x) > 0.01)[0]
    x = x[idx[0]:idx[-1]]
    return x / np.sqrt(np.mean(x ** 2)) * 0.08  # Normal laut: RMS 0,08


def tiefpass(x, alpha):
    y = np.empty_like(x); acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc); y[i] = acc
    return y


def fahrt(sek, rms=0.06):
    """Motor (Grundton 35-45 Hz mit Drehzahlschwankung, Obertöne), Abrollgeräusch (gefärbtes Rauschen),
    Wind (Hochpass-Rauschen) und Schlaglöcher (kurze laute Stöße)."""
    n = int(sek * SR); t = np.arange(n) / SR
    f0 = 40 + 5 * np.sin(2 * np.pi * 0.2 * t)
    phase = 2 * np.pi * np.cumsum(f0) / SR
    motor = sum(a * np.sin(k * phase) for k, a in [(1, 1.0), (2, 0.6), (3, 0.35), (4, 0.2), (6, 0.1)])
    weiss = rng.standard_normal(n).astype(np.float32)
    abroll = tiefpass(weiss, 0.05)
    wind = weiss - tiefpass(weiss, 0.3)
    x = motor / np.sqrt(np.mean(motor ** 2)) * 0.6 + abroll / np.sqrt(np.mean(abroll ** 2)) * 0.7 \
        + wind / np.sqrt(np.mean(wind ** 2)) * 0.25
    # Pegelschwankung (Gas geben, Gangwechsel) und Schlaglöcher.
    x *= 1 + 0.4 * np.sin(2 * np.pi * 0.15 * t + rng.uniform(0, 6))
    for s in rng.uniform(0, sek, size=int(sek / 1.5)):
        i = int(s * SR); l = int(0.08 * SR)
        x[i:i + l] += rng.standard_normal(min(l, n - i)) * 3 * np.exp(-np.arange(min(l, n - i)) / 300)
    return (x / np.sqrt(np.mean(x ** 2)) * rms).astype(np.float32)


def raum(sek):
    return (rng.standard_normal(int(sek * SR)) * 0.002).astype(np.float32)


def piep():
    t = np.arange(int(0.16 * SR)) / SR
    return (0.5 * np.sin(2 * np.pi * 1000 * t)).astype(np.float32)


def baue(teile, grund):
    """teile: Liste (art, daten) mit art in {stille, sprache}; grund: Funktion(sek) für den Hintergrund."""
    roh = np.concatenate([d for _, d in teile])
    x = grund(len(roh) / SR)[:len(roh)] + roh
    zeiten, pos = [], 0
    for art, d in teile:
        if art == "sprache": zeiten.append((pos / SR, (pos + len(d)) / SR))
        pos += len(d)
    return np.clip(x, -1, 1), zeiten


s1, s2 = lade("satz1.wav"), lade("satz2.wav")
leer = lambda sek: np.zeros(int(sek * SR), dtype=np.float32)
faelle = {
    # Sprache im Auto, danach 7 s nur Fahrgeräusch.
    "auto_sprache_dann_fahrt": baue([("piep", piep()), ("stille", leer(0.6)), ("sprache", s1), ("stille", leer(7))], fahrt),
    # Nur Fahrgeräusch nach dem Piepton, niemand spricht.
    "auto_nur_fahrt": baue([("piep", piep()), ("stille", leer(10))], fahrt),
    # Zwei Sätze mit 3 s Nachdenkpause im Auto.
    "auto_pause": baue([("piep", piep()), ("stille", leer(0.5)), ("sprache", s1), ("stille", leer(3.0)),
                        ("sprache", s2), ("stille", leer(7))], fahrt),
    # Leise Sprache (-18 dB) in ruhigem Raum.
    "ruhig_leise": baue([("piep", piep()), ("stille", leer(0.6)), ("sprache", s1 * 0.126), ("stille", leer(6))], raum),
    # Leise Sprache (-10 dB) im lauten Auto: schwierigster Fall.
    "auto_leise": baue([("piep", piep()), ("stille", leer(0.6)), ("sprache", s1 * 0.316), ("stille", leer(7))],
                       lambda sek: fahrt(sek, 0.08)),
}

sess = ort.InferenceSession(os.path.join(ORDNER, "silero_vad.onnx"))
os.makedirs(ZIEL, exist_ok=True)
for name, (x, zeiten) in faelle.items():
    zustand = np.zeros((2, 1, 128), np.float32); kontext = np.zeros(64, np.float32)
    werte, pegel = [], []
    # Wie auf dem Handy: 16-Bit-Werte, durch 32768.
    x16 = (np.round(x * 32767).astype(np.int16).astype(np.float32)) / 32768
    for i in range(0, len(x16) - 511, 512):
        r = x16[i:i + 512]
        out, zustand = sess.run(None, {"input": np.concatenate([kontext, r])[None, :], "state": zustand,
                                       "sr": np.array(16000, dtype=np.int64)})
        kontext = r[-64:]; werte.append(float(out[0, 0])); pegel.append(min(1.0, float(np.sqrt(np.sqrt(np.mean(r ** 2))) * 1.6)))
    with open(os.path.join(ZIEL, name + ".txt"), "w", encoding="utf-8") as f:
        f.write("# Silero VAD v5.1.2, je 32-ms-Rahmen: Wahrscheinlichkeit und Pegel wie MicRecorder. Sprache (s): " +
                " ".join(f"{a:.3f}-{b:.3f}" for a, b in zeiten) + "\n")
        f.write("\n".join(f"{w:.4f} {p:.4f}" for w, p in zip(werte, pegel)) + "\n")
    with wave.open(os.path.join(ORDNER, name + ".wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype(np.int16).tobytes())
    w_ = np.array(werte); t = np.arange(len(w_)) * 0.032
    in_sprache = np.zeros(len(w_), bool)
    for a, b in zeiten: in_sprache |= (t >= a) & (t < b)
    print(f"{name}: {len(werte)} Rahmen, Sprache {zeiten}, max p außerhalb Sprache {w_[~in_sprache].max():.2f}, "
          f"Anteil p>=0.5 in Sprache {np.mean(w_[in_sprache] >= 0.5) if in_sprache.any() else 0:.2f}")
