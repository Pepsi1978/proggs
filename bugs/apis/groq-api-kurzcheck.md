# Groq API (Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
| 6 | Whisper-Upload >24 MB ⭐ | Chunken VOR dem Senden (16 kHz mono: Limit ab ~13 Min!) + WAV bei Fehlschlag retten statt löschen | §4 |
| 7 | Whisper halluziniert bei Stille | `verbose_json` + `no_speech_prob`/`avg_logprob`-Filter | §4 |
| 8 | Tool-Calling / Folge-Request | Striktes OpenAI-Schema; `reasoning_content` vorher strippen | §5 |
