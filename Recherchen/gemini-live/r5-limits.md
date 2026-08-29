# R5 — Längen-, Session- und Kontingent-Limits der Gemini Live API (Fokus: `gemini-3.5-transcribe-live`)

Stand der Recherche: 29.08.2026. `gemini-3.5-transcribe-live` ist ein am 26.08.2026 angekündigtes **Public-Preview-Modell** — entsprechend dünn und teils widersprüchlich ist die Sekundärquellenlage. Primärquelle ist konsequent `ai.google.dev`.

## Sitzungsdauer-Limit — hartes 10-Minuten-Limit für `gemini-3.5-transcribe-live`

**BESTÄTIGT.** Die offizielle Live-Transcribe-Doku sagt wörtlich: *"Live transcription sessions support continuous streaming for up to 10 minutes."* Quelle: https://ai.google.dev/gemini-api/docs/live-api/live-transcribe

Die Modell-Übersichtsseite bestätigt das als separate Kennzahl gegenüber dem Batch/File-Modell: *"Max Audio Duration: 10 minutes per session"* für `gemini-3.5-transcribe-live`, während das Nicht-Live-Modell `gemini-3.5-transcribe` *"Up to 1 hour per request (up to 30 minutes when speaker diarization or word-level timestamps are enabled)"* erlaubt. Quelle: https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe

Wichtig für den Vergleich zu den allgemeinen Live-API-Dialogmodellen (z. B. `gemini-3.1-flash-live-preview`, native-audio-Modelle): Dort gilt ohne Compression ein 15-Minuten-Limit für reine Audio-Sessions bzw. 2 Minuten für Audio+Video, siehe unten. **Das Transcribe-Live-Modell hat davon abweichend offenbar ein eigenes, niedrigeres, festes 10-Minuten-Limit** — die Live-Transcribe-Doku erwähnt an keiner Stelle eine Verlängerungsmöglichkeit über Context Window Compression (siehe nächster Abschnitt). Das deutet darauf hin, dass die 10 Minuten bei diesem Modell ein hartes Produkt-Limit sind, nicht (nur) eine Folge des vollgelaufenen Kontextfensters wie bei den Dialog-Modellen.

## Verhalten bei Überschreitung — GoAway-Mechanismus dokumentiert, aber nicht Transcribe-Live-spezifisch belegt

**TEILWEISE BESTÄTIGT.** Für die Live API allgemein ist dokumentiert: *"Exceeding these limits will terminate the session (and therefore, the connection)."* Vor der Terminierung schickt der Server eine `GoAway`-Nachricht mit einem `timeLeft`-Feld, das die verbleibende Zeit bis zum Abbruch angibt — der Client kann also proaktiv reagieren (z. B. Session rechtzeitig beenden oder Resumption vorbereiten). Quelle: https://ai.google.dev/gemini-session (Session-Management-Seite), https://ai.google.dev/gemini-api/docs/live-session

Ein Google-Mitarbeiter im Entwickler-Forum bestätigt für den Kontextfenster-Fall zusätzlich: *"When the 128k context window fills up, it leads to the termination of the session."* Quelle: https://discuss.ai.google.dev/t/gemini-live-api-sessions-exceeding-15-minute-limit-without-compression/114104

**VERMUTUNG (nicht Transcribe-Live-spezifisch belegt):** Die Live-Transcribe-Doku selbst nennt keine eigene Fehlermeldung oder ein spezifisches Abbruchverhalten für das 10-Minuten-Limit von `gemini-3.5-transcribe-live`. Es ist plausibel, dass derselbe generische GoAway-Mechanismus des WebSocket-Protokolls greift (harter Verbindungsabbruch, kein stiller Textverlust vorher — der Client bekommt die Vorwarnung), das ist aber für dieses konkrete Modell nicht dokumentarisch bestätigt. Für den beschriebenen Anwendungsfall (Diktate 15–90 s, gelegentlich mehrere Minuten) ist das Limit ohnehin weit entfernt: selbst bei sehr langen Gedankenpausen und Verbindungsaufbau je Aufnahme dürfte die 10-Minuten-Marke praktisch nie erreicht werden, solange nicht regelmäßig mehrminütige Diktate mit Standby-Zeiten anfallen.

## Session Resumption — für Dialog-Live-Modelle dokumentiert, für Transcribe-Live nicht erwähnt

**BESTÄTIGT für die Live API allgemein, NICHT für `gemini-3.5-transcribe-live` belegt.** Über `sessionResumption` im `BidiGenerateContentSetup` sendet der Server periodisch `SessionResumptionUpdate`-Nachrichten mit einem Handle-Token. Bei einem Verbindungsabbruch (z. B. WLAN→Mobilfunk-Wechsel) kann der Client mit diesem Token reconnecten und der Server stellt den vorherigen Kontext wieder her. *"Resumption tokens are valid for 2 hr after the last sessions termination."* Quelle: https://ai.google.dev/gemini-api/docs/live-session

Python-Konfigurationsbeispiel aus der Doku: `session_resumption=types.SessionResumptionConfig(handle=previous_session_handle)`

**Wichtig:** Die dedizierte Live-Transcribe-Seite (https://ai.google.dev/gemini-api/docs/live-api/live-transcribe) erwähnt `sessionResumption` an keiner Stelle. Da euer Anwendungsfall ohnehin pro Aufnahme eine frische Verbindung aufbaut und danach schließt (keine Langzeit-Session über mehrere Verbindungen), ist Session Resumption für den beschriebenen Workflow vermutlich ohne Relevanz — als Fallback bei WebSocket-Abbrüchen während einer laufenden Übertragung könnte es dennoch nützlich sein, ist aber nicht bestätigt als für dieses Modell verfügbar.

## Context Window Compression — für Transcribe-Live nicht dokumentiert, vermutlich nicht anwendbar

**BESTÄTIGT für Live-Dialogmodelle, in Live-Transcribe-Doku NICHT erwähnt.** Für die allgemeinen Live-API-Modelle gilt: *"All Gemini Live API models have a context window limit of 128k tokens"* (native-audio-Output-Modelle) bzw. *"32k tokens for other Live API models"* laut Firebase-Doku. Über `contextWindowCompression` mit `sliding_window` und `trigger_tokens` lässt sich die Session-Dauer praktisch unbegrenzt verlängern, indem älteste Turns serverseitig verworfen/zusammengefasst werden — *"Enable context window compression to extend sessions to an unlimited duration."* Quellen: https://ai.google.dev/gemini-api/docs/live-session, https://ai.google.dev/gemini-api/docs/live-api/best-practices

**VERMUTUNG:** Da die Live-Transcribe-Doku dieses Feature nicht erwähnt und das 10-Minuten-Limit dort als fixe Kennzahl ohne Verlängerungsoption dargestellt wird, ist anzunehmen, dass `gemini-3.5-transcribe-live` Context Window Compression entweder nicht unterstützt oder das 10-Minuten-Limit unabhängig davon als Produkt-Hardcap gilt. Nicht offiziell bestätigt — für euren Anwendungsfall (kurze Einzelsessions, kein Langlauf) ohnehin nicht kritisch.

## Kontextfenster-Größe und Umrechnung Audio→Token — Diskrepanz zwischen zwei offiziellen Quellen

**BESTÄTIGT, aber mit einer wichtigen Diskrepanz.** Zwei verschiedene Umrechnungsfaktoren sind offiziell dokumentiert, je nach API-Bereich:

- **Live-API allgemein (Dialogmodelle):** *"native audio tokens accumulate rapidly (approximately 25 tokens per sec of audio)"*. Quelle: https://ai.google.dev/gemini-api/docs/live-api/best-practices
- **Allgemeine Audio-Understanding-API (nicht Live, z. B. Gemini 2.5 Flash mit Datei-Upload):** 32 Tokens pro Sekunde Audio, also 1.920 Tokens pro Minute. Quelle: https://ai.google.dev/gemini-api/docs/tokens (offizielle "Understand and count tokens"-Seite)
- **Speziell für `gemini-3.5-transcribe` UND `gemini-3.5-transcribe-live` laut Pricing-Seite:** *"Estimated pricing is based on 25 audio tokens per second for input and 175 text tokens per minute for output"*. Quelle: https://ai.google.dev/gemini-api/docs/pricing

Für euer konkretes Modell gilt also die **25-Tokens/Sekunde-Rate** (nicht die 32er-Rate der allgemeinen Audio-API) — offiziell bestätigt über die Pricing-Seite. Bei `inputTokenLimit: 131072` (vom Team-Lead als vom Modell gemeldeter Wert genannt, hier nicht separat nachrecherchiert) ergibt das rechnerisch: 131.072 Tokens ÷ 25 Tokens/Sekunde ≈ 5.243 Sekunden ≈ **87 Minuten** an Audio-Kapazität rein nach Kontextfenster-Token-Budget. **Das Kontextfenster ist damit für dieses Modell in der Praxis NICHT die limitierende Größe** — das harte 10-Minuten-Session-Limit greift lange vorher. Diese Rechnung selbst (131k ÷ 25 = 87 min) ist meine eigene Ableitung, keine direkt zitierte Google-Aussage — als VERMUTUNG/Herleitung zu kennzeichnen, auch wenn beide Eingangswerte (Tokenrate, Tokenlimit) offiziell belegt sind.

## Rate Limits und Kontingente im Free Tier — für `gemini-3.5-transcribe-live` NICHT offiziell tabellarisch dokumentiert

**UNBELEGT für das konkrete Modell — wichtigste offene Lücke dieser Recherche.** Die offizielle Rate-Limits-Seite (https://ai.google.dev/gemini-api/docs/rate-limits) enthält keine öffentliche Tabelle mit RPM/TPM/RPD für `gemini-3.5-transcribe-live` oder `gemini-3.5-transcribe`. Die Seite verweist stattdessen ausdrücklich auf die Live-Ansicht: *"Rate limits depend on a variety of factors (such as your usage tier) and can be viewed in Google AI Studio"* mit Link auf https://aistudio.google.com/rate-limit (erfordert Login, für diese Recherche nicht einsehbar).

Ebenso ist nicht offiziell dokumentiert: ein Limit für **gleichzeitige Live-Sessions** (concurrent sessions) im Free Tier speziell für Transcribe-Live. Für die allgemeine Live API nennt die Firebase-Doku zwar ein Konzept von Concurrent-Session-Limits, aber ohne konkrete, für Transcribe-Live bestätigte Zahl.

**VERMUTUNG (Sekundärquellen, mehrere unabhängige SEO-/Aggregator-Blogs, keine Google-Primärquelle):** Für Text-/Standardmodelle im Free Tier kursieren Werte wie 5–15 RPM, 250.000 TPM, 100–1.000 RPD (Beispiel Gemini 2.5 Flash: 10 RPM / 250k TPM / 250 RPD) — diese Zahlen gelten aber nachweislich für andere Modelle, nicht bestätigt für `gemini-3.5-transcribe-live`. Mehrere Blogs (aireiter.com, aifreeapi.com) behaupten unisono nur, dass ein Free Tier "existiert", ohne konkrete Zahlen für dieses Modell zu nennen, und verweisen selbst auf AI Studio als einzige verlässliche Quelle.

**Empfehlung für die Praxis:** Da keine belastbare offizielle Zahl vorliegt, sollte das tatsächliche Limit direkt im eigenen AI-Studio-Projekt unter dem Rate-Limit-Dashboard (https://aistudio.google.com/rate-limit) nachgeschaut werden — dort werden projektspezifische, live gültige Werte angezeigt.

## Preis-Eckdaten (als Kontext, nicht Kern der Fragestellung)

**BESTÄTIGT.** Laut Pricing-Seite: `gemini-3.5-transcribe-live` kostet $3,50 pro Mio. Audio-Input-Tokens bzw. $0,005/Minute, und $21,00 pro Mio. Text-Output-Tokens bzw. $0,004/Minute — macht zusammen ca. **$0,009 pro Minute** Live-Transkription (blended rate). Quelle: https://ai.google.dev/gemini-api/docs/pricing

## Offen / unbelegt

- Exakte RPM/TPM/RPD-Werte für `gemini-3.5-transcribe-live` im Free Tier — nirgends offiziell tabellarisch veröffentlicht, nur individuell im AI-Studio-Dashboard einsehbar.
- Limit für gleichzeitige (concurrent) Live-Sessions im Free Tier für dieses Modell.
- Ob `sessionResumption` und `contextWindowCompression` für `gemini-3.5-transcribe-live` überhaupt technisch unterstützt werden — in der Live-Transcribe-Doku schlicht nicht erwähnt, weder bestätigend noch verneinend.
- Ob beim Erreichen des 10-Minuten-Limits vorab eine `GoAway`-Warnung mit `timeLeft` gesendet wird (für Dialogmodelle bestätigt, für Transcribe-Live nicht explizit dokumentiert).
- Ob die 25-Tokens/Sekunde-Rate exakt oder nur eine Schätzung ("Estimated pricing is based on…") für die tatsächliche Kontextfenster-Abrechnung ist — die Pricing-Seite formuliert das ausdrücklich als Schätzwert für die Preisdarstellung, nicht zwingend als exakte technische Spezifikation.

## Quellenliste

- https://ai.google.dev/gemini-api/docs/live-api/live-transcribe — Live-Transcribe-Guide (10-Min-Limit, Custom Vocabulary, keine Diarization/Timestamps live)
- https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe — Modell-Übersicht mit Limit-Tabelle Live vs. File
- https://ai.google.dev/gemini-api/docs/transcribe — Audio-Transcription-Doku (Batch-Limits: 1h / 30min mit Diarization)
- https://ai.google.dev/gemini-api/docs/pricing — Preise und Tokenraten (25 Tokens/Sek. Audio-Input für Transcribe-Modelle)
- https://ai.google.dev/gemini-api/docs/live-session — Session-Management (GoAway, sessionResumption, contextWindowCompression, 128k/32k Kontextfenster)
- https://ai.google.dev/gemini-api/docs/live-api/best-practices — 15-Min-/2-Min-Limits, 25 Tokens/Sek. für native audio, 20–40ms Audio-Chunks
- https://ai.google.dev/gemini-api/docs/live-api/capabilities — Kontextfenster 128k/32k, Session-Limits
- https://ai.google.dev/gemini-api/docs/rate-limits — Rate-Limit-Grundlagen, Verweis auf AI-Studio-Dashboard
- https://ai.google.dev/gemini-api/docs/tokens — allgemeine Audio-Token-Umrechnung (32 Tokens/Sek., abweichend von Transcribe-Modellen)
- https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs — Firebase-Doku zu Live-API-Limits (128k/32k Kontextfenster je Modelltyp)
- https://discuss.ai.google.dev/t/gemini-live-api-sessions-exceeding-15-minute-limit-without-compression/114104 — Entwickler-Forum, Google-Mitarbeiter-Kommentar zu Kontextfenster-Terminierung
- https://aireiter.com/blog/gemini-3-5-transcribe-api-guide — Sekundärquelle (Blog), Vergleichstabelle Live/Batch-Limits (26.08.2026, Preview-Status)
