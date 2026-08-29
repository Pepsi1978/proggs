# Bekannte Bugs: Groq API (Integration)

> PFLICHT-LESEN vor Arbeit an einer Groq-API-Integration (Client-seitig, schnelle LPU-Inferenz).
> Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax). Versions-Anker: aktuelle Produktionsmodelle u. a.
> `llama-3.3-70b-versatile`, `llama-3.1-8b-instant`, `openai/gpt-oss-120b/-20b`, `whisper-large-v3(-turbo)`.
> Verwandt: Whisper-Halluzination siehe auch `desktop/groq-transkription.md`. Zweite Seite: noch keine
> `best-practices-groq-api.md` (Transkriptions-BP existiert: `best-practices/desktop/groq-transkription.md`).

> **Update 2026-07-02:** Neue Groq-Deprecations seit 17.06.2026: `qwen/qwen3-32b` und `meta-llama/llama-4-scout-17b-16e-instruct` Shutdown 17.07.2026; `llama-3.1-8b-instant` und `llama-3.3-70b-versatile` Shutdown 16.08.2026 (Free/Developer-Tier). Zusaetzlich beachten: Rate-Limit-Doku und Modellseiten koennen unterschiedliche Developer-Plan-Werte zeigen; zur Laufzeit Header und Dashboard als Wahrheit nutzen.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | OpenAI-Client gegen Groq | `logprobs`/`logit_bias`/`top_logprobs`/`name` weglassen, `n=1` | §1 |
| 2 | `temperature=0` gesetzt | Kleinen Float >0 setzen, kein echter Determinismus | §1 |
| 3 | 429 trotz freiem RPM ⭐ | TPM bottleneckt — token-bewusst pre-throttlen, alle 4 Header prüfen | §2 |
| 4 | Modell-ID im Code | Nie hartkodieren — dynamisch von `/models` ziehen | §3 |
| 5 | `max_tokens` setzen ⭐ | Output-Limit pro Modell, nicht ans Kontextfenster koppeln | §6 |
| 6 | Whisper-Upload >24 MB | Chunken (16 kHz mono) + `timeOffsetSec` für Timestamps | §4 |
| 7 | Whisper halluziniert bei Stille | `verbose_json` + `no_speech_prob`/`avg_logprob`-Filter | §4 |
| 8 | Tool-Calling / Folge-Request | Striktes OpenAI-Schema; `reasoning_content` vorher strippen | §5 |

---

## 1. OpenAI-Kompatibilität

### 1. Nicht unterstützte Parameter → 400
- **Symptom:** `logprobs`, `logit_bias`, `top_logprobs`, `messages[].name` → 400.
- **FIX:** diese Felder entfernen. Logprobs sind bei Groq nicht abbildbar — Feature client-seitig auslassen.
- **Quelle:** https://console.groq.com/docs/openai

### 2. `n` muss exakt 1 sein
- **FIX:** `n` weglassen/`n=1`; mehrere Varianten = N getrennte Requests (kostet RPM).

### 3. `temperature=0` wird still zu `1e-8`
- **FIX:** bewusst kleinen float > 0 setzen; echte 0-Determinismus nicht erwarten.

### 4. `response_format`/Structured Outputs von manchen Modellen ignoriert
- **Ursache:** strict Structured Outputs nur von neueren Modellen (constrained decoding); ältere ignorieren das Schema.
- **FIX:** Modell mit dokumentierter Structured-Output-Unterstützung wählen; sonst JSON-Mode + client-seitige Validierung/Repair-Loop.
- **Quelle:** https://community.groq.com/t/structured-outputs-ignored-by-openai-gpt-oss-120b/687

---

## 2. Rate-Limits (eigenes System — die häufigste Falle)

### 5. Vier Limits gleichzeitig — 429 sobald EINES feuert ⭐
- **Ursache:** RPM, RPD, TPM, TPD parallel auf Organisations-Ebene.
- **FIX:** alle vier Header überwachen (`x-ratelimit-limit/-remaining/-reset-requests`/`-tokens`), nicht nur Requests zählen.
- **Quelle:** https://console.groq.com/docs/rate-limits

### 6. TPM-Drosselung trotz freiem RPM ⭐
- **Symptom:** schnelle App bekommt 429, obwohl RPM nicht ausgeschöpft.
- **Ursache:** TPM bottleneckt High-Token-Workloads; bei großen Prompts ist TPM zuerst voll. Free-Tier z. B. ~6.000 TPM.
- **FIX:** token-bewusstes Pre-Throttling (vor dem Senden `x-ratelimit-remaining-tokens` prüfen), nicht erst auf 429 reagieren.

### 7. `retry-after` nur bei echtem 429 gesetzt
- **FIX:** `retry-after` nur im 429-Pfad auswerten; sonst Backoff + Jitter als Fallback.

### 8. Free-Tier-Limits sehr niedrig, oft missverstanden
- **Symptom:** Demo crasht in Produktion sofort an 429.
- **FIX:** Dev-Plan für höhere Limits; Limits modellspezifisch im Dashboard prüfen (nicht hartkodieren).

---

## 3. Modell-Deprecations (Groq mustert SCHNELL aus)

### 9. `model_decommissioned` (400) — Modell von heute auf morgen weg ⭐
- **Symptom:** `The model [X] has been decommissioned and is no longer supported`, code `model_decommissioned`.
- **Ursache:** Groq setzt Shutdown-Daten und entfernt hart (Mixtral, llama-3.1-70b, llama3-70b-8192, DeepSeek-R1-Distill, Kimi-K2-0905 → gpt-oss-120b u. v. m.).
- **FIX:** Modell-ID NIE hartkodieren — aus Config/`/models` beziehen; Deprecations-Seite vor Release prüfen; rechtzeitig auf Replacement migrieren.
- **Quelle:** https://console.groq.com/docs/deprecations

### 10. Stale Model-Lists in Third-Party-Tools
- **FIX:** Modell-Liste dynamisch vom `/models`-Endpoint ziehen; Defaults regelmäßig abgleichen.

---

## 4. Whisper / Speech-to-Text

### 11. Dateigrößen-Limit feuert früher als dokumentiert (413)
- **Symptom:** 413 schon bei ~37 MB auf Dev-Plan trotz „100 MB"; >30 MB teils silent fail.
- **FIX:** chunken — <~24 MB direkt, sonst in ~20-min-Segmente (32 kbps mono, 16 kHz) splitten + stitchen; alternativ `url`-Parameter.
- **Quelle:** https://console.groq.com/docs/speech-to-text
- **Eigener Vorfall 29.08.2026 (TerminalVoiceOverlay, Free-Plan):** 923 s (15,4 Min) am Stück
  diktiert → 16 kHz mono 16-bit = 32 kB/s → **29 548 784 Byte** → `413 request_too_large`.
  **Merksatz: bei 16 kHz mono ist das 25-MB-Limit nach ~13 Minuten erreicht** — jedes längere
  Diktat schlägt zu 100 % fehl. Ein 413 ist **nicht retrybar** (die Datei wird nicht kleiner),
  deshalb muss VOR dem Senden geschnitten werden, statt danach zu reagieren.
- **Zweite Lehre (kostete den kompletten Text):** Die App löschte die WAV im `finally` auch nach
  Fehlschlag → 15 Minuten Sprechen unwiederbringlich weg. **Jede fehlgeschlagene Aufnahme in einen
  Rettungsordner verschieben statt löschen** (Defense-in-Depth-Schicht hinter dem Chunking).
  Fix: `GroqWhisperClient.TranscribeInChunksAsync` (Schnitt in der längsten Sprechpause vor der
  Ziel-Größe) + `OverlayWindow.RescueFailedRecording`.

### 12. Timestamps brechen beim Chunking
- **FIX:** `timeOffsetSec` mitführen, zu jedem Segment-Timestamp addieren; `verbose_json` + `timestamp_granularities`.

### 13. Halluzinationen bei Stille
- **FIX:** `verbose_json`, pro Segment `no_speech_prob` (hoch=Stille) + `avg_logprob` (stark negativ=niedrige Konfidenz) prüfen, verdächtige Segmente verwerfen; Voiced-Vorfilter vorschalten. (Deckt sich mit Franks Schicht-1+2-Fix #46632/46633.)

### 14. Minimum-Billing 10 Sekunden
- **FIX:** kurze Clips client-seitig bündeln.

### 15. `prompt` ist max 224 Token & nur stilistisch
- **FIX:** Prompt knapp als Vokabular-/Stilhilfe (Voice-Dictionary-Pattern), nicht als Anweisung.

### 16. Nur erste Audiospur · kein vtt/srt · Translation nur `en`
- **FIX:** vorab auf Mono/16 kHz downmixen; nur `json`/`verbose_json`/`text`; für andere Zielsprachen separates Übersetzungsmodell.

---

## 5. Function/Tool Calling

### 17. `tool_use_failed` / 400 bei Nicht-Standard-Schema
- **Ursache:** Groq erzwingt striktes OpenAI-Tool-Schema.
- **FIX:** exakt `tools[].function.{name,description,parameters}`; `temperature` 0.0–0.5 für stabileres Tool-Calling; bei null/leer Retry.
- **Quelle:** https://console.groq.com/docs/tool-use

### 18. `reasoning_content` in assistant-Message unsupported → 400
- **FIX:** vor dem nächsten Request `reasoning_content` aus assistant-Messages strippen (nur role/content/tool_calls behalten).

### 19. Leerer assistant-`content` bei gpt-oss / Streaming
- **FIX:** leere/null-Antwort als Fehlerfall behandeln und Retry, statt leer weiterzuverarbeiten.

---

## 6. Token-/Kontext-Limits

### 20. `max_tokens` (Output) kleiner als Kontextfenster ⭐
- **Ursache:** Output-Limit modellabhängig kleiner: llama-3.3-70b = 32k, gpt-oss-120b/-20b = 65k, llama-3.1-8b = 131k — bei 131k Kontext.
- **FIX:** `max_completion_tokens` pro Modell aus Tabelle, nicht ans Kontextfenster koppeln; lange Ausgaben in Fortsetzungs-Requests.
- **Quelle:** https://console.groq.com/docs/models

---

## 7. Auth

### 21. 401 bei fehlerhaftem Bearer-Key
- **FIX:** Key aus sicherem Speicher (SK-Ordner), `gsk_`-Präfix prüfen.

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---|---|---|
| Mixtral, llama-3.1-70b, llama3-70b-8192, Kimi-K2-0905 u. a. | **decommissioned** (hart entfernt) | Bug 9 — migrieren, nicht „gefixt" |

**Noch NICHT gefixt / per Design:** nicht unterstützte OpenAI-Params (1), 4-Limit-System + TPM-Falle (5/6), Output<Kontext (20), Whisper-413/Timestamps/Stille (11–13), strict Tool-Schema (17). Modell-Set ändert sich laufend → dynamisch beziehen.

---

## Pflicht-Checkliste vor Groq-Integration

- [ ] Modell-ID dynamisch von `/models` (nicht hartkodiert)?
- [ ] Alle vier Rate-Limit-Header überwacht, TPM-bewusstes Pre-Throttling?
- [ ] `max_completion_tokens` pro Modell aus Tabelle (nicht ans Kontextfenster gekoppelt)?
- [ ] Nicht unterstützte Params (logprobs/n/name) entfernt?
- [ ] Whisper: gechunkt + Timestamp-Offset + `no_speech_prob`-Filter?
- [ ] `reasoning_content` vor Folge-Request gestrippt?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/groq-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [api-integration-general](api-integration-general.md)
- [cli-impersonation-subscription-auth](cli-impersonation-subscription-auth.md)
- [deepseek-api](deepseek-api.md)
- [google-gemini-api](google-gemini-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
- [groq-transkription](../desktop/groq-transkription.md)
