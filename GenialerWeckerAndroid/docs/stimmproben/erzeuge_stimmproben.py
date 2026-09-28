#!/usr/bin/env python3
"""Erzeugt die Hörproben der Stimmauswahl vorab (40 Rechenschritte) als OGG nach app/src/main/assets/stimmproben/.

Aufruf (aus GenialerWeckerAndroid, nach einem Build, der app/tts-modelle geladen hat):
    python3 -m pip install sherpa-onnx numpy soundfile
    python3 docs/stimmproben/erzeuge_stimmproben.py

Die Texte müssen genau Sprachen.probe() in LokaleStimme.kt entsprechen; die Dateinamen ModellStimmen.probeAsset().
"""
import os, sys, time
import numpy as np, soundfile as sf, sherpa_onnx

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODELLE = os.path.join(WURZEL, "app", "tts-modelle", "tts")
ZIEL = os.path.join(WURZEL, "app", "src", "main", "assets", "stimmproben")
SCHRITTE = 40
PROBE = {
    "de": "Guten Morgen! Es ist sieben Uhr. Draußen wird es hell – ein guter Moment, um aufzustehen.",
    "en": "Good morning! It's seven o'clock. The day is getting brighter – a good moment to get up.",
    "fr": "Bonjour ! Il est sept heures. Le jour se lève – c'est le bon moment pour se lever.",
    "es": "¡Buenos días! Son las siete. Ya amanece: es un buen momento para levantarse.",
    "pt": "Bom dia! São sete horas. Lá fora já está a clarear – um bom momento para se levantar.",
}

def speichern(pfad, samples, rate):
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    sf.write(pfad, np.asarray(samples, dtype=np.float32), rate, format="OGG", subtype="VORBIS")

def supertonic():
    d = os.path.join(MODELLE, "supertonic")
    tts = sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(
        supertonic=sherpa_onnx.OfflineTtsSupertonicModelConfig(
            duration_predictor=f"{d}/duration_predictor.onnx", text_encoder=f"{d}/text_encoder.onnx",
            vector_estimator=f"{d}/vector_estimator.onnx", vocoder=f"{d}/vocoder.onnx", tts_json=f"{d}/tts.json",
            unicode_indexer=f"{d}/unicode_indexer.bin", voice_style=f"{d}/voice.bin"), num_threads=8)))
    for sprache, text in PROBE.items():
        for sid in range(10):
            g = sherpa_onnx.GenerationConfig(); g.sid = sid; g.num_steps = SCHRITTE; g.extra = {"lang": sprache}
            t = time.time(); a = tts.generate(text, g)
            speichern(os.path.join(ZIEL, sprache, f"supertonic-{sid}.ogg"), a.samples, a.sample_rate)
            print(f"supertonic {sprache} {sid}: {time.time() - t:.1f} s", flush=True)

def pocket():
    d = os.path.join(MODELLE, "pocket")
    tts = sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(
        pocket=sherpa_onnx.OfflineTtsPocketModelConfig(
            lm_flow=f"{d}/lm_flow.int8.onnx", lm_main=f"{d}/lm_main.int8.onnx", encoder=f"{d}/encoder.onnx",
            decoder=f"{d}/decoder.int8.onnx", text_conditioner=f"{d}/text_conditioner.onnx",
            vocab_json=f"{d}/vocab.json", token_scores_json=f"{d}/token_scores.json"), num_threads=8)))
    for stimme in ["bria", "loona"]:
        ref, rate = sf.read(f"{d}/test_wavs/{stimme}.wav", dtype="float32")
        if ref.ndim > 1: ref = ref.mean(axis=1)
        g = sherpa_onnx.GenerationConfig(); g.reference_audio = ref.tolist(); g.reference_sample_rate = rate
        g.num_steps = 5; g.extra = {"temperature": "0.7", "chunk_size": "15"}
        a = tts.generate(PROBE["en"], g)
        speichern(os.path.join(ZIEL, "en", f"pocket-{stimme}.ogg"), a.samples, a.sample_rate)
        print(f"pocket {stimme} fertig", flush=True)

if __name__ == "__main__":
    supertonic()
    pocket()
