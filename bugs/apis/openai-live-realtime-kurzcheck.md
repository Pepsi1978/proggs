# OpenAI Live- und Realtime-Sprach-API (GPT-Live, gpt-realtime-2.1, Android-Client) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.
> Stand: 2026-10-16 · Volltext: `bugs/apis/openai-live-realtime.md`

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `immutable_field_update` bei Live-`session.update` | Nur `delegation.responses` + `triggers` änderbar; Stimme/Modell/Instructions/Format/Delegationstyp → neue Sitzung (Fork) | A1 |
| 2 | Backend antwortet nach Tool-Ergebnis nicht / `tool_outputs_required` | Nach `response.item.create` (`function_call_output`) **immer** `response.create`; jede `call_id` beantworten | B2, B3 |
| 3 | Funktionsaufruf „kommt nicht an" | `response.completed.output` ist leer — Calls aus verschachteltem `response.event` → `response.output_item.done` lesen | B1 |
| 4 | `responses_validation_failed` | Asynchron, Sitzung bleibt offen → korrigiertes `session.update` senden | B4 |
| 5 | Client-Delegation ohne Auftrag | `session.delegation.created` hat keinen Text → Transkript-Deltas mitschreiben | B6 |
| 6 | Assistent redet nach Unterbrechung weiter (Live) | Kein Cancel/Truncate/Done-Event → eigene Audio-Queue leeren | A3 |
| 7 | `conversation.item.truncate` abgelehnt (Realtime) | `audio_end_ms` auf empfangene Audiolänge klemmen, nicht Wanduhrzeit | F4 |
| 8 | Live-Kosten doppelt so hoch / Sitzung hängt | `usage.seconds` kumulativ, nie summieren; `session.close` + auf `session.closed` warten | A4, A5 |
| 9 | `invalid_audio` | PCM16 gerade Byteanzahl, 100-ms-Blöcke 4.800 Byte | A6 |
| 10 | Unknown parameter `session.audio`/`session.type` oder keine Audio-Deltas | Beta-Header `OpenAI-Beta: realtime=v1` entfernen (seit 12.05.2026 abgeschaltet), GA-Eventnamen `response.output_audio.delta` | C1, C3, C4 |
| 11 | Realtime-Stimme lässt sich nicht ändern / fällt auf alloy | `session.audio.output.voice` vor der ersten Audioausgabe setzen | C6 |
| 12 | Effort-Fehler beim Backend | `none` nicht bei `gpt-6-astra`/`gpt-6.1-sol` (dort auch kein `minimal`); `max` fehlt im SDK | E1, E2 |
| 13 | KI unterbricht sich selbst (Lautsprecher, Samsung) | AEC prüfen/Software-AEC, erste Antwort nicht unterbrechbar, VAD-Schwelle hoch | F1, F2, F10 |
| 14 | `SecurityException` beim Mikrofon-FGS | Foreground-Service `microphone` nur aus sichtbarer Activity starten (Android 14+) | F9 |
| 15 | Altmodelle | `gpt-realtime`/`-mini` Abschaltung 20.01.2027; `gpt-4o-*-transcribe`/`whisper-1` 26.02.2027 | E3, E4 |
