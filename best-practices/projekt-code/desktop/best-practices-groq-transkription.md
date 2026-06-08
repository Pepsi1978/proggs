# Groq-Transkription (Whisper large-v3 / turbo) — Best Practices

**Stand:** 2026-06-08 (7 Researcher parallel, offizielle Groq-Doku + peer-reviewed Paper zuerst).
Software: **Groq Speech-to-Text API** (`whisper-large-v3`, `whisper-large-v3-turbo`), Cloud.
Kontext: Always-On-Voice-Apps (VoiceAgent, .NET 10/NAudio, 16 kHz mono).

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/desktop/groq-transkription.md`](../../../bugs/desktop/groq-transkription.md)):
> der Almanach sagt *was schiefgeht*, diese Datei sagt *wie man die Pipeline von vornherein richtig baut*.
> Quellen-Flag: `offiziell` (Groq/OpenAI-Doku, arXiv) vs. `extern` (Community/Praxis).

---

## ⚡ TL;DR — die Defaults, die man einmal richtig setzt

1. **Nie rohe Stille an Groq senden.** Sprachgehalt-/VAD-Vorfilter VOR dem Request — verhindert
   Halluzinationen UND spart Geld (Min-Billing 10 s/Clip). `offiziell`+`extern`
2. **`response_format=verbose_json`** statt `text` — bei Groq ohne Mehrlatenz/-kosten, liefert die
   Confidence-Felder zum Filtern. `offiziell`
3. **Confidence-Gate mit UND:** `no_speech_prob > 0.6` UND `avg_logprob < -1.0` verwerfen;
   `compression_ratio > 2.4` gegen Repetition. AND schuetzt leise Sprache. `offiziell`
4. **Mehrsprachige Floskel-Blocklist** als letzter Filter — nur bei kurz + Stille-Kontext greifen
   lassen (echte kurze Aussage bleibt). `extern`
5. **`language="de"` (ISO-639-1), `temperature=0`**, nur dokumentierte Params, kanonischer Endpoint.
   `retry-after` bei 429 auswerten. `offiziell`

---

## 1. Aufnahme & Vorfilter (Client) — die wirksamste Schicht

- **16 kHz mono WAV** direkt aufnehmen (Groq-Empfehlung, geringste Latenz, kein Server-Resampling). `offiziell`
- **Sprachgehalt messen, nicht nur Dauer:** Aufnahme-Trigger ≠ Sprache. Beim Finalisieren die
  **Voiced-Dauer** (Summe Frames mit `rms ≥ Schwelle`) bzw. Voiced-Ratio bestimmen; sprach-arme Clips
  gar nicht senden. Konservativ (echte kurze Befehle nicht abschneiden). `extern`
- **Adaptiver Noise-Floor** statt fester Schwelle: `thr ≈ 3× noise`, `noise = 0.95·noise + 0.05·cur`
  (Update nur in Stille) — robust gegen Luefter/Hintergrund. `extern`
- **Optional Silero-VAD (ONNX)** fuer hoehere Genauigkeit: .NET via `VadSharp` oder
  `ManySpeech.SileroVad`; threshold 0.5 (noisy 0.7), `min_speech_duration_ms≈250`,
  `speech_pad_ms≈300`. Reines RMS-Gate reicht als erster Schritt. `extern`
- **Pre-/Post-Padding** (200–500 ms) gegen abgeschnittene Wortanlaute/-enden. `extern`

## 2. Request-Konfiguration (Groq)

- **`response_format=verbose_json`** (ohne `word`-Granularitaet → die ist OPEN-buggy bei Groq und
  kostet Latenz). Liefert pro Segment `no_speech_prob`, `avg_logprob`, `compression_ratio`. `offiziell`
- **`temperature=0`** als Basis (verhindert Stille-Halluzination NICHT allein, aber sinnvoll). `offiziell`
- **`language="de"`** (ISO-639-1, nicht `de-DE`/`german`) — bessere Accuracy/Latenz. `offiziell`
- **`prompt`** (max 224 Tokens) NUR fuer Eigennamen/Schreibweisen, keine Anweisungen (sonst Leakage). `offiziell`
- **Modell:** `whisper-large-v3-turbo` fuer reines Diktat (guenstig, schnell, kein Translate);
  `whisper-large-v3` wenn Translation gebraucht wird. `offiziell`
- **Nur dokumentierte Parameter** senden — Whisper-interne Schwellen (`no_speech_threshold`,
  `condition_on_previous_text` …) gibt es bei Groq NICHT → client-seitig ueber `verbose_json` filtern. `offiziell`

## 3. Nachfilter (funktionserhaltend) — Reihenfolge

1. **Confidence-Gate (UND):** Segment verwerfen wenn `no_speech_prob>0.6 AND avg_logprob<-1.0`;
   zusaetzlich `compression_ratio>2.4` (Repetition); `dauer<0.4s AND no_speech_prob>0.6` (Mini-Noise).
   Zu aggressiv? `no_speech`-Schwelle auf 0.7 anheben, NICHT `avg_logprob` lockern. `offiziell`
2. **Floskel-Blocklist:** verwerfen nur bei (kurz ≤~6 Woerter) UND (normalisierter exakter Match,
   `==` nicht `contains`) UND (Stille-Kontext). Quelle: HF-Dataset `sachaarbonel/whisper-hallucinations`
   (7.890 Phrasen). `extern`
3. **Leere/sprach-arme Antwort** still verwerfen, NICHT als Fehler werfen (Always-On normal). `extern`

## 4. Resilienz / Kosten

- **Statischer `HttpClient`** mit `SocketsHttpHandler.PooledConnectionLifetime` (kein `new` pro Call). `offiziell`
- **429:** `retry-after`-Header auswerten + exponential backoff (+Jitter); `x-ratelimit-remaining-*`
  ueberwachen. 413/422 NICHT retryen, nur 429/500/503. `offiziell`
- **Min-Billing 10 s/Clip + RPM 20 (free)** beachten — Vorfilter (Abschnitt 1) reduziert beides. `offiziell`
- **Kanonischer Endpoint** `https://api.groq.com/openai/v1/audio/transcriptions`. `offiziell`

---

## Kopplung zum Bug-Almanach

| Best-Practice (hier) | verhindert Bug in `bugs/desktop/groq-transkription.md` |
|----------------------|--------------------------------------------------------|
| 1 Aufnahme & Vorfilter | §1.1 Stille-Halluzination, §2.1/2.2 Vorfilter, §4.1 Min-Billing |
| 2 Request-Konfiguration | §1.2 temperature, §1.3 prompt, §3.1–3.6 Format/Params |
| 3 Nachfilter | §1.1/§1.4 Halluzination, §2.3 Confidence-Gate, §2.4 Blocklist |
| 4 Resilienz/Kosten | §4.1–4.6 Rate-Limits/Fehlercodes/Endpoint |

## Quellen
- [Groq Speech-to-Text Docs](https://console.groq.com/docs/speech-to-text) · [API Reference](https://console.groq.com/docs/api-reference) · [Rate Limits](https://console.groq.com/docs/errors)
- [arXiv 2501.11378 — Whisper Hallucinations (VAD+BoH)](https://arxiv.org/html/2501.11378v1) · [arXiv 2402.08021](https://arxiv.org/html/2402.08021v2)
- [snakers4/silero-vad](https://github.com/snakers4/silero-vad) · [HF whisper-hallucinations](https://huggingface.co/datasets/sachaarbonel/whisper-hallucinations)
