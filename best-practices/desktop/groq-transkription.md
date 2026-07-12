# Groq-Transkription (Whisper large-v3 / turbo) — Best Practices

**Stand:** 2026-07-02. Zwei Laeufe: (a) bug-fokussiert aus dem Almanach-Lauf #46624,
(b) breiter Best-Practices-Lauf (7 Researcher parallel, offizielle Groq-/Microsoft-Learn-Doku zuerst).
Software: **Groq Speech-to-Text API** (`whisper-large-v3`, `whisper-large-v3-turbo`), Cloud.
Client-Kontext: Always-On-Voice-App VoiceAgent (.NET 10, WPF, NAudio, 16 kHz mono, statischer HttpClient).

> **Update 2026-07-02:** Re-Recherche fand keine neuen belegten Groq-Whisper-STT-Regeln seit 2026-06-08; die bestehende mehrschichtige Stille-/Halluzinationsabwehr bleibt unveraendert.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/desktop/groq-transkription.md`](../../bugs/desktop/groq-transkription.md)):
> der Almanach sagt *was schiefgeht*, diese Datei sagt *wie man die Pipeline von vornherein richtig baut*.
> Quellen-Flag: `offiziell` (Groq/OpenAI/Microsoft-Learn-Doku, arXiv) vs. `extern` (Community/Praxis).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Clip an Groq senden | Nie rohe Stille — Sprachgehalt-/VAD-Vorfilter VOR dem Request | §1 |
| 2 | Response-Format | `response_format=verbose_json` (Confidence-Felder, kein `word`-Timestamp) | §2 |
| 3 | Nachfilter Confidence | UND: `no_speech_prob>0.6` UND `avg_logprob<-1.0`; `compression_ratio>2.4` | §3 |
| 4 | Letzter Filter | Mehrsprachige Floskel-Blocklist nur bei kurz + Stille-Kontext | §3 |
| 5 | Modell waehlen | `whisper-large-v3-turbo` als Default; `v3` nur fuer max. Genauigkeit/Translation | §2 |
| 6 | Request-Params | `language="de"` (ISO-639-1), `temperature=0`, `prompt` nur Eigennamen | §2 |
| 7 | Audio aufnehmen | 16 kHz mono PCM16, KEIN Denoise/AGC/Normalisierung; WAV=Latenz | §1 |
| 8 | .NET HTTP-Resilienz | Statischer `HttpClient`+`SocketsHttpHandler`; Upload-POST NICHT retryen; `retry-after` lesen | §5 |
| 9 | JSON-DTOs | System.Text.Json Source-Gen, snake_case via `[JsonPropertyName]` | §6 |
| 10 | Kosten senken | VAD-Vorfilter (groesster Hebel), kurze Clips buendeln (Min-Billing 10 s); Batch API 50 % guenstiger | §7 |

---

## ⭐ Der 4-Schichten-Standard-Kanon (PFLICHT bei JEDEM Groq/Whisper-large-v3-Projekt)

> **Verbindliche Referenz (Frank-Wunsch 2026-07-12):** Whisper (Groq `large-v3` UND `large-v3-turbo`)
> halluziniert bei Stille Floskeln ("Vielen Dank", "Untertitel des ZDF") und haengt nach End-Pausen
> Woerter an ("Ja") — beides MIT hoher Confidence. `temperature=0` allein reicht NICHT. Jede App mit
> Groq-Whisper-Diktat baut daher IMMER **alle vier** Schichten ein, nie eine Teilmenge. Alle vier sind
> **funktionserhaltend**: echte (auch leise/kurze) Sprache wird NIE verworfen.

| Schicht | Was | Kernregel |
|---------|-----|-----------|
| **1 — Sprachgehalt-Vorfilter** | VOR dem Senden: Voiced-Zeit der Aufnahme messen (RMS pro 20-ms-Frame, `rms ≥ 0.015`) | `< 150 ms` laute Zeit → gar nicht erst an Groq senden (nur absolute Zeit, keine Ratio — Denkpausen erlaubt) |
| **2 — Confidence-Gate** | pro Segment aus `verbose_json` verwerfen | `no_speech_prob>0.6` **UND** `avg_logprob<-1.0` (nie ODER!); `compression_ratio>2.4`; Mini-Noise (`dauer<0.4s` UND `no_speech>0.6`). Text aus den BEHALTENEN Segmenten NEU zusammensetzen — nie den Roh-Text durchreichen |
| **3 — Segment-Audio-Abgleich** | Segment-Zeitfenster gegen die eigene Voiced-Timeline | `< 10 %` laute Frames im Fenster = Trailing-Halluzination. **Drift-Sicherung:** wuerde Schicht 3 ALLES verwerfen, die Schicht-2-Ergebnisse behalten (Whisper-Timestamps driften) |
| **4 — Floskel-Blocklist** | letzter Filter an ALLEN Ausgaengen | Floskel NUR verwerfen bei DREI Signalen zugleich: kurz (`≤6 Woerter`/`≤64 Zeichen`) + normalisierter EXAKTER Match (`==`, nicht contains) + Stille-Kontext (`voicedMs<600`). Ohne Voiced-Timeline NICHT verwerfen |

**Kanonische Referenz-Implementierungen (kopieren statt neu erfinden):**
- **C# / .NET (Vollversion, alle 4):** `TerminalVoiceOverlay-Windows/Services/GroqWhisperClient.cs` (+ macOS/CVO-Pendants).
- **Kotlin / Android (Vollversion, alle 4):** `CortexAndroid/.../audio/SpeechAnalyzer.kt` + `WhisperHallucinationFilter.kt` (WAV-PCM16 direkt, kein MediaCodec).
- **Kotlin / Android (3-Schichten-Variante, M4A via MediaCodec):** `EntropieReductor/.../TranscribeAudioUseCase.kt`, `BestJournalFrank/.../TranscriptionRepository.kt` — bei neuen Projekten die **4-Schichten**-Fassung bevorzugen (Blocklist ergaenzen).

Gegenstueck (Fehlermodi jeder Schicht): [`bugs/desktop/groq-transkription.md`](../../bugs/desktop/groq-transkription.md) §2.

---

## 1. Aufnahme & Audio-Preprocessing (Client) — die wirksamste Schicht

- **16 kHz mono PCM16** direkt aufnehmen (Groq + Whisper downsamplen ohnehin darauf; hoehere Rate/
  Bittiefe bringt KEINE bessere Genauigkeit, nur groessere Dateien). `offiziell`
- **Sprachgehalt messen, nicht nur Dauer:** Aufnahme-Trigger ≠ Sprache. Beim Finalisieren die
  **Voiced-Dauer** (Summe Frames mit `rms ≥ Schwelle`) bzw. Voiced-Ratio bestimmen; sprach-arme Clips
  gar nicht senden. Konservativ (echte kurze Befehle nicht abschneiden). `extern`
- **Adaptiver Noise-Floor** statt fester Schwelle: `thr ≈ 3× noise`, `noise = 0.95·noise + 0.05·cur`
  (Update nur in Stille) — robust gegen Luefter/Hintergrund. `extern`
- **Optional Silero-VAD (ONNX)** fuer hoehere Genauigkeit: .NET via `VadSharp`/`ManySpeech.SileroVad`;
  threshold 0.5 (noisy 0.7), `min_speech_duration_ms≈250`, `speech_pad_ms≈300`. RMS-Gate reicht als erster Schritt. `extern`
- **KEIN aggressives Preprocessing vor Whisper** (KRITISCH): Denoise/RNNoise, harte AGC/Kompression,
  starke Hoch-/Tiefpassfilter, Pre-Emphasis, Upsampling „fuer Qualitaet" **verschlechtern** Whisper
  messbar — das Modell ist auf rohes, diverses Audio trainiert und reinigt selbst. „Clear audio beats
  perfect format specs": gute Mic-Platzierung schlaegt jede Filterkette. Nur VAD + leichtes Padding
  sind sichere Stufen. `extern` (arXiv „When Denoising Hinders", openai/whisper #2125)
- **Resampling (falls Geraet ≠ 16 kHz):** Windows `MediaFoundationResampler` (Quality bis 60, kann
  Downmix+Resample in einem Schritt); cross-platform/managed `WdlResamplingSampleProvider`. `offiziell`(NAudio)
- **Clipping vermeiden** (Peaks < 0 dBFS, irreversibel) ist wichtiger als Lautheit angleichen;
  DC-Offset entfernen; falls Normalisierung, sanft mit Peak-Limiter. `extern`
- **Pre-/Post-Roll-Padding** (300–500 ms, bei abgeschnittenen Anlauten bis ~1000 ms) gegen verschluckte
  erste/letzte Silben — aber nicht doppelt stapeln, wenn der VAD-Filter schon padded. `offiziell`+`extern`
- **Format-Wahl:** WAV PCM16 = geringste Latenz (kein Encode/Decode); FLAC = verlustfrei + ~50 %
  kleiner fuer grosse Dateien; Genauigkeit WAV == FLAC. Opus/MP3 (lossy) meiden, wenn Genauigkeit zaehlt. `offiziell`

## 2. Request-Konfiguration (Groq)

- **`response_format=verbose_json`** (ohne `word`-Granularitaet → die ist OPEN-buggy bei Groq und
  kostet Latenz). Liefert pro Segment `no_speech_prob`, `avg_logprob`, `compression_ratio`. `offiziell`
- **Modellwahl:**

  | Modell | Preis/h | WER | Speed | Hinweis |
  |--------|--------:|----:|------:|---------|
  | `whisper-large-v3-turbo` | $0.04 | ~12 % | 216x | **Default Diktat**, nur Transkription (keine Translation) |
  | `whisper-large-v3` | $0.111 | ~10,3 % | 164x | max. Genauigkeit + Translation (→ EN) |
  | `distil-whisper-large-v3-en` | guenstig | — | sehr schnell | **nur Englisch** (fuer DE ungeeignet) |
  `offiziell`
- **`temperature=0`** als Basis (verhindert Stille-Halluzination NICHT allein, aber sinnvoll). `offiziell`
- **`language="de"`** (ISO-639-1, nicht `de-DE`/`german`) — bessere Accuracy UND Latenz (kein Auto-Detect-Overhead). `offiziell`
- **`prompt`** (max 224 Tokens) NUR fuer Eigennamen/Schreibweisen, keine Anweisungen (sonst Leakage).
  Beispiel (DE): `"Transkript eines Entwickler-Diktats. Eigennamen: Frank, Claude Code, BestJournal, Entropie Reductor, Groq, Whisper, WPF."` `offiziell`
- **Nur dokumentierte Parameter** senden — Whisper-interne Schwellen (`no_speech_threshold`,
  `condition_on_previous_text` …) gibt es bei Groq NICHT → client-seitig ueber `verbose_json` filtern (§3). `offiziell`

## 3. Nachfilter (funktionserhaltend) — Reihenfolge

1. **Confidence-Gate (UND):** Segment verwerfen wenn `no_speech_prob>0.6 AND avg_logprob<-1.0`;
   zusaetzlich `compression_ratio>2.4` (Repetition); `dauer<0.4s AND no_speech_prob>0.6` (Mini-Noise).
   Zu aggressiv? `no_speech`-Schwelle auf 0.7 anheben, NICHT `avg_logprob` lockern. `offiziell`
   - **Kalibrier-Hinweis:** Das Groq-Cookbook nennt `avg_logprob <= -0.5` als „deutet auf Probleme" —
     das ist eine **Flag**-Schwelle (markieren/monitoren), nicht hart verwerfen. Zum *Verwerfen* bleibt
     die konservativere Whisper-Default-Kombi (-1.0 + AND) richtig, damit echte Sprache nicht wegfaellt.
     `compression_ratio ~1.66` ist normal fuer fluessige Sprache. `offiziell`(Cookbook)
2. **Floskel-Blocklist:** verwerfen nur bei (kurz ≤~6 Woerter) UND (normalisierter exakter Match,
   `==` nicht `contains`) UND (Stille-Kontext). Quelle: HF-Dataset `sachaarbonel/whisper-hallucinations`
   (7.890 Phrasen). `extern`
3. **Leere/sprach-arme Antwort** still verwerfen, NICHT als Fehler werfen (Always-On normal). `extern`

## 4. Performance & Latenz

- **WAV fuer Latenz** (kein Server-Decode), FLAC nur wenn Upload-Groesse zaehlt. Clientseitig auf
  16 kHz mono konvertieren schrumpft den Upload (Stereo 44.1 kHz → mono 16 kHz ≈ Faktor 5–6). `offiziell`
- **Kein echtes STT-Streaming bei Groq** (REST/file-basiert; SSE gibt es nur fuer Chat-LLMs). Latenz-
  aermste Alternative = **VAD-getriggerte kurze Chunks** (1–5 s) sofort einzeln senden. `offiziell`+`extern`
- **Connection-Reuse:** persistenter `HttpClient`/keep-alive (kein TLS-Handshake pro Clip), HTTP/2,
  beim Lesen `HttpCompletionOption.ResponseHeadersRead`. Praxis-Pipeline ~80 ms STT (turbo). `extern`
- **Chunking langer Aufnahmen** (>25 MB / lange Files): Cookbook-Default Chunk 600 s, **Overlap 10 s**,
  Merge per Longest-Common-Sequence an Wortgrenzen. Fuer Always-On „kuerzere Chunks 30–60 s besser bei
  wechselnden Sprechern/Rauschen". Groq chunkt NICHT selbst → ueberlange Files sonst still uebersprungen. `offiziell`(Cookbook)
- **Durchsatz vs. Limit:** free **20 RPM** org-weit → Client-Token-Bucket ~1 Request/3 s + lokale Queue;
  parallel erlaubt, solange das gleitende 60-s-Fenster ≤20 bleibt. Zweiter Engpass: **7.200 Audio-Sek/h**
  → nur echte Sprache (VAD) senden. `offiziell`

## 5. .NET HTTP-Resilienz (`Microsoft.Extensions.Http.Resilience` / Polly v8)

- **Upload-POST NICHT automatisch retryen (KRITISCH):** ein Retry kann dieselbe Datei doppelt
  transkribieren/abrechnen. `options.Retry.DisableForUnsafeHttpMethods()` (deaktiviert Retry fuer
  POST/PUT/PATCH/DELETE) — sicherste Variante: Upload-Fehler an den Aufrufer melden statt blind wiederholen. `offiziell`
- **`retry-after` auswerten** (Groq liefert ihn bei 429) via `DelayGenerator`, der
  `response.Headers.RetryAfter.Delta`/`.Date` liest; `return null` → Fallback auf Exponential+Jitter. `offiziell`(Polly)
- **AttemptTimeout/TotalTimeout fuer Audio hochsetzen** — Defaults (10 s/30 s) sind fuer multipart-
  Upload zu kurz; `HttpClient.Timeout = InfiniteTimeSpan`, Timeout-Kontrolle der Pipeline ueberlassen. `offiziell`
- **Ohne DI (WPF):** `ResiliencePipelineBuilder<HttpResponseMessage>` → `ResilienceHandler { InnerHandler =
  SocketsHttpHandler { PooledConnectionLifetime = 2–15 min } }` → in den statischen `HttpClient`. `offiziell`
- **Circuit Breaker:** Default `MinimumThroughput=100` ist fuer eine Single-User-Desktop-App zu hoch
  (loest faktisch nie aus) → z. B. 3 setzen; `FailureRatio` 0.2. `BackoffType=Exponential`, `UseJitter=true`. `offiziell`
- **`PooledConnectionLifetime`** zwingend bei langlebigem static Client (sonst stale DNS, ignoriert TTL). `offiziell`
- Hinweis Ist-Zustand VoiceAgent: aktueller `GroqWhisperClient` retryt 429/500/503 mit festen Delays
  `[2,4,8]s` und liest `retry-after` NICHT → Kandidat fuer Umstellung auf obiges Muster.

## 6. .NET Client-Architektur

- **Statischer `HttpClient` + `SocketsHttpHandler`** statt `IHttpClientFactory` in einer reinen WPF-App
  ohne DI-Host (festes Ziel Groq, Bearer-Token) — schlankste MS-konforme Loesung. `offiziell`
- **System.Text.Json Source-Generator** (`JsonSerializerContext` + `[JsonSerializable]`) fuer die
  `verbose_json`-DTOs → reflection-frei, schnell, trim/AOT-fest, erfuellt CA1869 automatisch. Groq liefert
  **snake_case** → pro Property `[JsonPropertyName("no_speech_prob")]` (oder SnakeCaseLower-Policy). `offiziell`
  ```csharp
  public record GroqTranscription(string Text, IReadOnlyList<GroqSegment> Segments);
  public record GroqSegment(
      [property: JsonPropertyName("no_speech_prob")]   double NoSpeechProb,
      [property: JsonPropertyName("avg_logprob")]      double AvgLogProb,
      [property: JsonPropertyName("compression_ratio")]double CompressionRatio,
      string Text);
  [JsonSerializable(typeof(GroqTranscription))]
  internal partial class GroqJsonContext : JsonSerializerContext { }
  ```
- **`CancellationToken` als letzter Parameter** (CA1068), Timeout + User-Cancel per
  `CancellationTokenSource.CreateLinkedTokenSource` zusammenfuehren; Token an alle Aufrufe weiterreichen (CA2016). `offiziell`
- **Multipart-Upload:** `MultipartFormDataContent` mit `StreamContent` (streamt, kein Vollpuffer) bzw.
  `ByteArrayContent` bei kleinen Buffern; in Lib-Code jeden `await` mit `ConfigureAwait(false)`. `offiziell`
- **Fehlerbehandlung:** `StatusCode` explizit auf typisierte Exceptions mappen (429→RateLimit mit
  `RetryAfter`, 401→Auth, sonst generisch) statt nur `EnsureSuccessStatusCode()` — Body zur Diagnose behalten. `offiziell`

## 7. Kosten

- **Min-Abrechnung 10 s pro Request** (Floor: alles <10 s kostet wie 10 s; darueber sekundengenau). `offiziell`
- **Modell als Hebel:** turbo $0.04/h vs v3 $0.111/h = **64 % guenstiger**. `offiziell`
- **VAD-Vorfilter** = groesster Hebel: nur echte Sprache senden (1 h Audio enthaelt oft 10–15 min Sprache → 75 %+ weniger Audio-Sekunden). `extern`
- **Kurze Clips buendeln** auf ~10 s+ statt jede 2–3-s-Aussage einzeln (sonst zahlt jede die 10-s-Mindestgebuehr). `offiziell`(Regel)/`extern`(Strategie)
- **Batch API** (`/v1/audio/transcriptions`, async, URL statt Direct-Upload, max 50.000 Zeilen/200 MB):
  **50 % guenstiger**, eigene Rate-Limits (zaehlt nicht gegen die synchronen). turbo via Batch ≈ $0.02/h.
  Fuer Live ungeeignet (Latenz Stunden) — ideal fuer nachgelagerte Volltranskripte/Logs. `offiziell`
- **Clip-Dedup** lohnt bei Live-Mic kaum (nie byte-identisch) — nur bei Re-Processing identischer Dateien. `extern`

## 8. Integration, Observability & Fallback

- **API-Key nie hardcoden:** aus SK-Ordner/Env laden (Windows DPAPI / `secrets-in-sk-folder`-Regel) —
  Key konzeptionell nie im Repo (Poka-Yoke Stufe 3). `extern`
- **Idempotenz:** kein Blind-Retry auf POST; bei Wunsch deterministischen Idempotency-Key senden;
  `x-ratelimit-remaining-*` proaktiv ueberwachen. `offiziell`
- **Observability (passt zu observability-first):** pro Transkription `no_speech_prob`, `avg_logprob`,
  `compression_ratio`, `duration`, Modell, language, Latenz strukturiert als JSON-Lines loggen; Segmente
  ausserhalb der Schwellen als WARN. Macht schlechte Aufnahmebedingungen/Mic-Probleme sichtbar. `offiziell`(Cookbook)
- **Health/Fallback bei Groq-Ausfall:** Groq primaer; bei 429/Timeout/Outage Fallback auf zweiten
  Cloud-Provider (OpenAI-Whisper) ODER lokales `whisper.cpp`/`faster-whisper` (offline, 0 Kosten, langsamer).
  Outages sind real → Fallback nicht optional. `extern`
- **Kanonischer Endpoint** `https://api.groq.com/openai/v1/audio/transcriptions`. `offiziell`

---

## Kopplung zum Bug-Almanach

| Best-Practice (hier) | verhindert Bug in `bugs/desktop/groq-transkription.md` |
|----------------------|--------------------------------------------------------|
| 1 Aufnahme & Audio-Preprocessing | §1.1 Stille-Halluzination, §2.1/2.2 Vorfilter, §3.3 Audioformat, §4.1 Min-Billing |
| 2 Request-Konfiguration | §1.2 temperature, §1.3 prompt, §3.1–3.6 Format/Params/Modell |
| 3 Nachfilter | §1.1/§1.4 Halluzination, §2.3 Confidence-Gate, §2.4 Blocklist |
| 4 Performance & Latenz | §3.5 word-granularity, §4.3 grosse Dateien/Chunking |
| 5 .NET HTTP-Resilienz | §4.2 Rate-Limit/retry-after, §4.5 Fehlercodes, §4.6 Endpoint |
| 6 .NET Client-Architektur | §3.6 nur dokumentierte Params, §4.4 leere Antwort |
| 7 Kosten | §4.1 Min-Billing, §4.2 Rate-Limits |
| 8 Integration/Observability/Fallback | §4.2 429, §4.3 stiller Fehlschlag, §4.6 Endpoint |

## Quellen
- [Groq Speech-to-Text Docs](https://console.groq.com/docs/speech-to-text) · [API Reference](https://console.groq.com/docs/api-reference) · [Rate Limits](https://console.groq.com/docs/rate-limits) · [Batch](https://console.groq.com/docs/batch) · [Pricing](https://groq.com/pricing) · [Cookbook (Speech & Audio)](https://deepwiki.com/groq/groq-api-cookbook/2-speech-and-audio-processing) `offiziell`
- [Groq Turbo-Blog](https://groq.com/blog/whisper-large-v3-turbo-now-available-on-groq-combining-speed-quality-for-speech-recognition) · [164x-Benchmark](https://groq.com/blog/groq-runs-whisper-large-v3-at-a-164x-speed-factor-according-to-new-artificial-analysis-benchmark) `offiziell`
- [MS Learn — HTTP Resilience](https://learn.microsoft.com/en-us/dotnet/core/resilience/http-resilience) · [HttpClient Guidelines](https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines) · [DisableForUnsafeHttpMethods](https://learn.microsoft.com/en-us/dotnet/api/microsoft.extensions.http.resilience.httpretrystrategyoptionsextensions.disableforunsafehttpmethods) · [Polly Retry/DelayGenerator](https://www.pollydocs.org/strategies/retry.html) `offiziell`
- [MS Learn — STJ Source-Gen](https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/source-generation) · [CA1869](https://learn.microsoft.com/en-us/dotnet/fundamentals/code-analysis/quality-rules/ca1869) · [CA1068](https://learn.microsoft.com/en-us/dotnet/fundamentals/code-analysis/quality-rules/ca1068) · [Coalesce CancellationTokens](https://learn.microsoft.com/en-us/dotnet/standard/asynchronous-programming-patterns/coalesce-cancellation-tokens-from-timeouts) `offiziell`
- [NAudio Resampling](https://github.com/naudio/NAudio/blob/master/Docs/Resampling.md) · [arXiv — When Denoising Hinders](https://arxiv.org/pdf/2603.04710) · [openai/whisper #2125 (kein Denoise)](https://github.com/openai/whisper/discussions/2125) · [#870 (16 kHz)](https://github.com/openai/whisper/discussions/870) `extern`/`offiziell`
- [arXiv 2501.11378 — Whisper Hallucinations (VAD+BoH)](https://arxiv.org/html/2501.11378v1) · [HF whisper-hallucinations](https://huggingface.co/datasets/sachaarbonel/whisper-hallucinations) · [silero-vad](https://github.com/snakers4/silero-vad) `extern`

---

## 🔗 Kopplung zum Bug-Almanach (wechselseitige Bezugstabelle)

Best-Practices (diese Datei) ↔ Bug-Almanach [`~/proggs/bugs/desktop/groq-transkription.md`](../../bugs/desktop/groq-transkription.md). Links die *Praevention*, rechts die *Falle*.

| Best-Practice-Abschnitt (hier) | Zugehoeriger Bug-Almanach-Abschnitt (`bugs/desktop/groq-transkription.md`) |
|--------------------------------|---------------------------------------------------------------------------|
| BP 1 Aufnahme/Preprocessing | §1 Stille-Halluzination, §2 Abwehr-Kette (Vorfilter) |
| BP 2 Request-Konfig | §1.2 temperature, §1.3 prompt, §3 Groq-Parameter/Format/Modell |
| BP 3 Nachfilter (Confidence + Blocklist) | §1.4 Repetition, §2 Abwehr-Kette (Confidence/Blocklist) |
| BP 4 Performance | §3 Groq-Parameter/Format/Modell |
| BP 5 .NET-Resilienz | §4 Rate-Limits/Fehlercodes |
| BP 7 Kosten | §4 Kosten |
| BP 8 Integration | §4 Endpoint |
