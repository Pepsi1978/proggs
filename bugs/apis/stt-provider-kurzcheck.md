# STT-Provider (Speech-to-Text, Deutsch) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

Stand: 21.10.2026

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Gemini Transcribe 429 „25 requests per day on Free Tier“ | Free Tier ist 3 RPM/25 RPD → Rückfall Groq oder bezahlter Schlüssel | S1 |
| 2 | Groq 403 `model_permission_blocked_org` | Modell in der Groq-Konsole (Settings › Limits) für die Org freischalten | S2 |
| 3 | Groq 403 aus Python/urllib | Standard-User-Agent wird geblockt → eigenen UA setzen | S3 |
| 4 | Whisper gibt bei Stille den Prompt aus | VAD-Sperre vor Upload; Ausgabe = Prompt-Text verwerfen | S4 |
| 5 | Gemini-LLM (generateContent) erfindet Sätze bei Stille | Nur mit Stille-Sperre; dedizierte Transcribe-Modelle nutzen | S5 |
| 6 | Gemini 3.8 Flash 503 „high demand“ / 429 „exceeded your current quota“ | Backoff; Free-Kontingent ist klein → zweiter Schlüssel/Paid | S6 |
| 7 | Deepgram `keyterm` wirkt nicht | Je Begriff eigener Parameter, kein Komma (stiller Ausfall) | S7 |
| 8 | Gemini Transcribe smart lässt Wörter weg / 200 ohne Text | verbatim setzen; leere 200 = keine Sprache bzw. Rückfall | S8 |
| 9 | Google Cloud STT 403 „API_KEY … blocked“ / v2 ohne API-Key | v2 braucht OAuth; Key-Einschränkung prüfen | S9 |
| 10 | OpenAI whisper-1 / gpt-4o-(mini-)transcribe | Abschaltung 26.02.2027 → gpt-transcribe | S10 |
| 11 | WER-Vergleich Turbo vs. andere „passt nicht“ | Normalisierung (Umlaute, Zahlen) je Quelle verschieden; nur innerhalb einer Quelle vergleichen | S11 |
