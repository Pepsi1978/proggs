# STT-Provider (Speech-to-Text, Deutsch) — Bug-Almanach

Stand: 21.10.2026 (Systemzeit 22:20) · Quelle: Studie `Recherchen/stt-vergleich` (Berichte r1–r10, Messplatz `messung/`).
Verwandt: `bugs/desktop/groq-transkription.md`, `bugs/desktop/whisper-stt-lokal.md`, `best-practices/apis/stt-provider.md`.

## S1 — Gemini 3.5 Transcribe: Free Tier nur 25 Anfragen/Tag
- **Symptom:** HTTP 429 `Rate limit exceeded for model gemini-3.5-transcribe (limit: 3 requests per minute on Free Tier)` bzw. `(limit: 25 requests per day on Free Tier). Please retry in 10h49m…`.
- **Ursache:** Free-Tier-Limits 3 RPM / 25 RPD (in der Doku nicht veröffentlicht, nur im AI-Studio-Dashboard).
- **Betroffen:** `gemini-3.5-transcribe`, Interactions API, Stand 21.10.2026. TVO/CVO mit Engine `gemini` fallen nach 25 Diktaten still auf Groq zurück.
- **Fix:** bezahlter Schlüssel mit Prepay + Limit, oder bewusst als Erstversuch mit Groq-Rückfall; Limits unter https://aistudio.google.com/rate-limit prüfen.
- **Quelle:** eigener Messplatz 21.10.2026 (`messung/grenzen.json`).

## S2 — Groq: Modell auf Organisationsebene gesperrt
- **Symptom:** 403 `The model whisper-large-v3 is blocked at the organization level … code: model_permission_blocked_org`.
- **Fix:** https://console.groq.com/settings/limits → Modell freischalten. Funktionserhaltend: Turbo bleibt als Rückfall.
- **Quelle:** Messplatz 21.10.2026.

## S3 — Groq blockt Python-urllib-User-Agent
- **Symptom:** 403 Forbidden auf `GET /openai/v1/models` bzw. Transkription aus Python `urllib` ohne eigenen UA.
- **Fix:** Header `User-Agent` setzen (z. B. `curl/8.0`).
- **Quelle:** Messplatz 21.10.2026.

## S4 — Whisper gibt bei Stille den Prompt aus
- **Symptom:** Bei 12 s leisem Rauschen liefert Groq Turbo mit `prompt="Fachbegriffe: …"` den Text „Fachbegriffe, WLAN, APK, HRV.“ (ohne Prompt nur „...“).
- **Betroffen:** `whisper-large-v3-turbo` (Groq), ebenso gpt-4o-*-transcribe laut r9.
- **Fix:** VAD-Sperre vor dem Upload; Ausgabe, die überwiegend aus Prompt-Wörtern besteht, verwerfen (WhisperHallucinationFilter erweitern).
- **Quelle:** Messplatz; r9.

## S5 — Gemini-LLM als STT erfindet Text bei Stille
- **Symptom:** `gemini-3.8-flash` per `generateContent` mit Audio und Anweisung „wortgetreu, bei Stille leer“ liefert bei Rauschen ohne Sprache ganze Sätze („Guten Tag, Herr Schneider. Ich wollte mich kurz … bedanken.“; mit Vokabelliste ein Satz über den Bug-Almanach).
- **Betroffen:** `gemini-3.8-flash`, 21.10.2026; `gemini-3.5-flash-lite` und `gemini-3.5-transcribe` blieben leer.
- **Fix:** dedizierte Transcribe-Modelle nutzen; LLM-STT nur hinter VAD-Sperre.
- **Quelle:** Messplatz.

## S6 — Gemini 3.8 Flash: 503 und schnell erschöpftes Free-Kontingent
- **Symptom:** 503 „This model is currently experiencing high demand“, danach 429 „You exceeded your current quota“ nach wenigen Aufrufen.
- **Fix:** Backoff (15–25 s), anderer Schlüssel/Paid.
- **Quelle:** Messplatz.

## S7 — Deepgram `keyterm` mit Komma: stiller Ausfall
- **Symptom:** Begriffe werden nicht verstärkt, kein Fehler.
- **Fix:** je Begriff ein eigener `keyterm=`-Parameter, ≤ 500 Tokens, keine Kommas/Gewichte.
- **Quelle:** https://developers.deepgram.com/docs/keyterm (Nova-3, 21.10.2026) via r5.

## S8 — Gemini Transcribe: smart lässt Wörter weg, 200 ohne Text
- **Fix:** `mode.type = "verbatim"`; leere 200-Antwort als „keine Sprache“ bzw. Rückfall behandeln.
- **Quelle:** TVO `GeminiBatchTranscribeClient.cs`; r9.

## S9 — Google Cloud STT: API-Key gesperrt / v2 braucht OAuth
- **Symptom:** 403 `Requests to this API speech.googleapis.com method google.cloud.speech.v1.Speech.Recognize are blocked` (API-Key-Einschränkung); v2 `recognize` akzeptiert keinen API-Key.
- **Fix:** v1 nur mit freigegebenem Key; v2 mit OAuth-Scope `cloud-platform`. Die 60 Freiminuten gelten nur für v1.
- **Quelle:** Messplatz; r3.

## S10 — OpenAI: Abschaltung alter Transkriptionsmodelle 26.02.2027
- `whisper-1`, `gpt-4o-transcribe`, `gpt-4o-mini-transcribe`, `gpt-4o-transcribe-diarize`, `/v1/audio/translations` → Ersatz `gpt-transcribe` (0,27 $/h) / `gpt-live-transcribe` (1,02 $/h). Jarvis Live führt `gpt-4o-mini-transcribe` noch als Wahl.
- **Quelle:** r2 (Ankündigung 26.08.2026).

## S11 — Messartefakt: WER-Zahlen nicht quer vergleichbar
- HF-Normalizer entfernt Umlaute und kennt keine deutschen Zahlen; dasselbe Modell streut je Quelle bis 2,7 Punkte; Hoster-Abweichung beim selben Modell bis ~6 Punkte.
- **Fix:** nur innerhalb einer Quelle vergleichen; eigene Messung mit eigener Normalisierung.
- **Quelle:** r8.
