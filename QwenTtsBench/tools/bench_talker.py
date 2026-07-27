"""Benchmark des Qwen3-TTS Talkers (autoregressiver Decode-Schritt).

12 Talker-Schritte entsprechen 1 Sekunde gesprochener Sprache (12-Hz-Tokenizer).
"""
import sys
import time

import numpy as np
import onnxruntime as ort

MODEL = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\barwa\Downloads\qwen3tts-onnx\talker_decode_q.onnx"
PROMPT_LEN = int(sys.argv[2]) if len(sys.argv) > 2 else 100   # Text + Sprecher-Embedding
STEPS = int(sys.argv[3]) if len(sys.argv) > 3 else 60          # 60 Schritte = 5 s Audio

LAYERS, KV_HEADS, HEAD_DIM, HIDDEN = 28, 8, 128, 1024

opts = ort.SessionOptions()
opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL

t0 = time.perf_counter()
sess = ort.InferenceSession(MODEL, opts, providers=["CPUExecutionProvider"])
load_s = time.perf_counter() - t0
print(f"Modell geladen in {load_s:.1f} s")
print(f"Threads: intra={opts.intra_op_num_threads or 'auto'}")

past = {}
for i in range(LAYERS):
    past[f"past_key_{i}"] = np.zeros((1, KV_HEADS, PROMPT_LEN, HEAD_DIM), dtype=np.float32)
    past[f"past_value_{i}"] = np.zeros((1, KV_HEADS, PROMPT_LEN, HEAD_DIM), dtype=np.float32)

timings = []
for step in range(STEPS):
    past_len = PROMPT_LEN + step
    feeds = {
        "inputs_embeds": np.zeros((1, 1, HIDDEN), dtype=np.float32),
        "attention_mask": np.ones((1, past_len + 1), dtype=np.int64),
    }
    feeds.update(past)

    t = time.perf_counter()
    outs = sess.run(None, feeds)
    timings.append(time.perf_counter() - t)

    names = [o.name for o in sess.get_outputs()]
    for i in range(LAYERS):
        past[f"past_key_{i}"] = outs[names.index(f"present_key_{i}")]
        past[f"past_value_{i}"] = outs[names.index(f"present_value_{i}")]

warm = timings[3:] if len(timings) > 6 else timings
avg_ms = sum(warm) / len(warm) * 1000
tok_s = 1000 / avg_ms

print()
print(f"Schritte gemessen:      {len(timings)}")
print(f"Erster Schritt:         {timings[0] * 1000:.0f} ms")
print(f"Mittel (eingelaufen):   {avg_ms:.0f} ms/Schritt")
print(f"Tempo:                  {tok_s:.1f} Schritte/s")
print(f"Echtzeitfaktor:         {tok_s / 12:.2f} x   (12 Schritte = 1 s Audio)")
print()
sec4 = 4 * 12 / tok_s
print(f"Ein typischer Fragesatz (4 s Sprache) braucht: {sec4:.1f} s Rechenzeit")
