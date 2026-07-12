# Groq-Transkription (Whisper large-v3 / turbo) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

> **⭐ PFLICHT-KANON:** Jedes Groq/Whisper-large-v3(-turbo)-Diktat baut IMMER **alle vier**
> Anti-Halluzinations-Schichten ein (1 Vorfilter · 2 Confidence-Gate · 3 Audio-Abgleich · 4 Floskel-
> Blocklist), nie eine Teilmenge — alle funktionserhaltend. Kanonische Referenz: TVO
> `GroqWhisperClient.cs` (C#) bzw. CortexAndroid `SpeechAnalyzer.kt`+`WhisperHallucinationFilter.kt`
> (Kotlin). Details: Volltext-Abschnitt „Der 4-Schichten-Standard-Kanon".

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
