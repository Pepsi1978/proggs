# R6 — Gemini Live API: Audio schneller als Echtzeit einspeisen, Chunk-Größe und Sendetakt

Untersuchtes Modell/Protokoll: `gemini-3.5-transcribe-live` über WebSocket `bidiGenerateContent` (v1beta), Nachrichtenfeld `realtimeInput`.

## Kernfrage: Darf man schneller als Echtzeit senden?

**BESTÄTIGT (indirekt, kein explizites Verbot, aber auch keine explizite Erlaubnis):** Keine der offiziell erreichbaren Google-Dokumentationsseiten (live-translate, live-transcribe, get-started-websocket, capabilities, `ai.google.dev/api/live`, Firebase-Limits, Google-Cloud-Troubleshooting/Best-Practices) enthält einen Satz, der das Senden vorab aufgenommener Audiodateien "am Stück" bzw. schneller als Echtzeit ausdrücklich erlaubt oder verbietet. Die Doku ist durchgängig auf den Live-Mikrofon-Anwendungsfall zugeschnitten und beschreibt nicht den Fall "ganze Datei vorab vorhanden, wird gepusht". Quellen: [Live translation](https://ai.google.dev/gemini-api/docs/live-api/live-translate), [Live transcription](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe), [Get started (WebSocket)](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket), [Capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities), [API-Referenz `BidiGenerateContentRealtimeInput`](https://ai.google.dev/api/live).

**BESTÄTIGT, starkes Indiz:** Das offizielle Beispiel `google-gemini/cookbook/quickstarts/Get_started_LiveAPI.py` sendet Audio-Chunks **ohne jedes künstliche `asyncio.sleep()`** — der Sende-Task liest einfach so schnell aus einer Queue, wie Daten verfügbar sind:
```python
async def send_realtime(self):
    while True:
        msg = await self.out_queue.get()
        blob = types.Blob(data=msg["data"], mime_type=msg["mime_type"])
        if msg["mime_type"].startswith("audio/"):
            await self.session.send_realtime_input(audio=blob)
```
Es gibt sogar einen Kommentar zur Latenzoptimierung, der besagt, dass bei voller Queue der älteste Eintrag verworfen wird statt zu warten — also ein Design, das eher "schneller senden" als "künstlich bremsen" bevorzugt. Quelle: [Get_started_LiveAPI.py](https://github.com/google-gemini/cookbook/blob/main/quickstarts/Get_started_LiveAPI.py). Einschränkend: Im Mikrofon-Fall entsteht die Realtime-Taktung natürlich dadurch, dass die Queue nur so schnell befüllt wird, wie das Mikrofon Daten liefert — der Code beweist also nicht positiv, dass ein bewusst schneller Bulk-Push einer fertigen Datei ebenso unproblematisch ist, zeigt aber, dass Google selbst keine Sleep-basierte künstliche Drosselung als Pflicht vorsieht.

**VERMUTUNG (nicht offiziell, Drittquelle):** Der unabhängige Blogartikel von Didier P. Martin behauptet explizit, man solle "im Echtzeit-Tempo" senden ("pace at real-time rate"), und warnt vor "Pitch-Shift und verstümmelten Antworten" bei falschem Tempo. Bei genauerem Lesen bezieht sich diese Warnung dort aber erkennbar auf eine **falsche Abtastrate** (Sample-Rate-Mismatch), nicht auf reine Sende-Geschwindigkeit — die Formulierung vermischt beides. Da keine offizielle Google-Quelle diese Pacing-Pflicht bestätigt, ist dieser Punkt als unbelegte Einzelmeinung zu werten. Quelle: [didierphmartin.com — Gemini WebSocket Guide](https://didierphmartin.com/articles/gemini-websocket-guide.html).

## Bestätigte Berichte zu Problemen beim Streaming (aber anderer Natur als "zu schnell")

**BESTÄTIGT:** GitHub-Issue `googleapis/python-genai#1859` — Nutzer berichtet von Latenz-Akkumulation und "verstümmelten"/unsinnigen Transkripten bei **langen (20 s+), kontinuierlichen Echtzeit-Streams** (Mikrofon-Fall, nicht Bulk-Datei), 16-kHz-PCM-Int16 mit 1024- bzw. 4096-Byte-Chunks, `gemini-2.0-flash-exp`. Das Issue ist geschlossen, **ohne** sichtbare Stellungnahme von Google oder bestätigten Workaround. Das ist ein anderes Szenario als "fertige Datei am Stück senden" — hier wächst die Latenz über die Zeit bei durchgehendem Echtzeit-Tempo, nicht durch bewusst schnelles Senden. Relevanz für deinen Fall: gering bis mittel, da dein Test mit 64,5 s Audio in 15 s ein vollständiges Ergebnis lieferte, also gerade **keine** Latenz-Akkumulation zeigte. Quelle: [python-genai Issue #1859](https://github.com/googleapis/python-genai/issues/1859).

**BESTÄTIGT:** Weitere verwandte, aber nicht deckungsgleiche Issues zu Audio-Qualität/Abschneiden im selben Repo: `#872` (Chunks werden übersprungen, Silben gehen verloren — betrifft Audio-**Ausgabe**, nicht Transkriptions-Eingabe), `#2117` und `#1224` (Turn-Complete- bzw. Session-Hänger, ebenfalls Ausgabe-/Antwortseite). Diese beziehen sich überwiegend auf die Sprachausgabe des Modells oder Session-Stabilität, nicht direkt auf das Verwerfen von zu schnell gesendeten Eingabe-Audio-Chunks. Quellen: [#872](https://github.com/googleapis/python-genai/issues/872), [#2117](https://github.com/googleapis/python-genai/issues/2117), [#1224](https://github.com/googleapis/python-genai/issues/1224).

**VERMUTUNG (Drittquelle, kein Google-Post):** Ein Blogartikel (technetexperts.com) beschreibt ein reproduzierbares VAD-Problem: **erratisches, nicht-kontinuierliches** Senden (z. B. 10 ms Audio, dann 2 s Pause, dann wieder 10 ms) soll den serverseitigen VAD-Puffer sättigen oder aus dem Takt bringen. Das ist explizit ein Muster mit **großen Lücken zwischen winzigen Chunks**, nicht "viele Chunks direkt hintereinander ohne Pause" (dein Muster). Der Artikel empfiehlt stattdessen größere, zusammenhängende Chunks (2–3 s) oder durchgehende Frames — das spricht eher **für** dein Vorgehen (durchgehende 32-KB-Blöcke ohne Lücken) als dagegen. Quelle: [technetexperts.com — Fix Gemini Live API Audio Stops](https://www.technetexperts.com/fix-gemini-live-api-audio-stops/) — **nicht** von Google, ohne Primärquellen-Beleg, mit Vorsicht zu behandeln.

## Empfohlene Chunk-Größe und Format

**BESTÄTIGT:** Mehrere offizielle Google-Seiten geben übereinstimmend **100 ms pro Chunk** als Empfehlung an, konkretisiert als **1024 bis 2048 Frames**. Quellen: [Live translation](https://ai.google.dev/gemini-api/docs/live-api/live-translate) ("Send audio in chunks of 100ms"), [Live transcription](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe) ("Send audio in chunks of 100ms (1,024 to 2,048 frames)").

**BESTÄTIGT:** Das offizielle Cookbook-Beispiel verwendet `CHUNK_SIZE = 1024` (Bytes bzw. Samples, je nach Kontext ≈ 64 ms bei 16 kHz/16 bit mono). Quelle: [Get_started_LiveAPI.py](https://github.com/google-gemini/cookbook/blob/main/quickstarts/Get_started_LiveAPI.py).

Dein Wert von 32 KB pro Block (≈ 1 s Audio) liegt damit deutlich **über** der von Google in den Doku-Beispielen genutzten Chunk-Größe (100 ms ≈ 3,2 KB bei 16 kHz/16 bit/mono). Das ist nirgends als Fehlerquelle dokumentiert — es gibt keine Aussage zu einer Obergrenze der Chunk-Größe außer der impliziten WebSocket-Frame-Grenze (siehe unten) — aber es weicht spürbar vom in allen Beispielen gezeigten Muster ab.

**BESTÄTIGT — exaktes Audioformat:** `audio/pcm;rate=16000`, roh, 16 Bit, little-endian, mono, für Eingabe. Ausgabe (falls relevant): 24 kHz, ebenfalls 16 Bit little-endian PCM. Quellen: [Live translation](https://ai.google.dev/gemini-api/docs/live-api/live-translate), [Get started (WebSocket)](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket), [Firebase Limits & Specs](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs).

**BESTÄTIGT — abweichende Abtastrate:** Die API akzeptiert laut Doku grundsätzlich andere Abtastraten, sofern die tatsächliche Rate korrekt im `mimeType` jedes Blobs angegeben wird (z. B. `audio/pcm;rate=8000`) — die API resampelt dann intern auf 16 kHz. Fehlt diese Angabe oder ist sie falsch, ist mit Tonhöhenverschiebung / fehlerhafter Erkennung zu rechnen (Pitch-Shift, "garbled responses"), da die Rohbytes dann mit falscher Rate interpretiert werden. Quelle: [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities) (per Web-Suche zusammengefasst — Originalzitat konnte per WebFetch nicht wörtlich extrahiert werden, da die Seite beim direkten Abruf nur Navigationsinhalte lieferte; als **BESTÄTIGT mit Einschränkung** eingestuft, da die Aussage in mehreren unabhängigen Suchergebnis-Zusammenfassungen konsistent auftaucht, aber nicht wörtlich verifiziert werden konnte).

## `audioStreamEnd` / `activityEnd` — Hinweis auf serverseitiges Puffern/Caching

**BESTÄTIGT:** Offizielle Doku und ein GitHub-Issue-Titel bestätigen übereinstimmend: `audioStreamEnd: true` dient dazu, bei einer Sende-Pause (z. B. Mikrofon länger als ~1 s stumm) **gecachtes Audio zu flushen** ("send audioStreamEnd when the mic is paused to flush cached audio"). Das bestätigt indirekt, dass der Server eingehendes Audio serverseitig zwischenspeichert/puffert, bevor es final verarbeitet wird — ein Hinweis darauf, dass sehr schnelles Nachliefern von Chunks grundsätzlich mit dem Pufferkonzept der API vereinbar ist, solange am Ende `audioStreamEnd` bzw. bei manuellem VAD `activityEnd` gesetzt wird, damit der Puffer final geleert/abgeschlossen wird. Quellen: [Get started (WebSocket)](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket), [ADK-Python Issue #2887 zu `audioStreamEnd`](https://github.com/google/adk-python/issues/2887).

Dein Ablauf (Blöcke → `activityEnd` → `audioStreamEnd`) folgt exakt diesem dokumentierten Muster.

## Maximale WebSocket-Frame-Größe

**Offen / unbelegt:** In keiner der geprüften offiziellen Quellen (API-Referenz `ai.google.dev/api/live`, Firebase Limits & Specs, Google-Cloud-Referenzdoku) findet sich eine explizite Zahl für eine maximale Nachrichten-/Frame-Größe bei `realtimeInput`. Es wird nur allgemein auf Session-/Verbindungsdauer-Limits eingegangen (siehe nächster Abschnitt), nicht auf Byte-Grenzen pro Nachricht.

## Session- und sonstige Limits (Kontext, nicht direkt zur Kernfrage)

**BESTÄTIGT:** Laut Firebase-AI-Logic-Dokumentation: Audio-only-Sessions ca. 15 Minuten begrenzt, Audio+Video ca. 2 Minuten, WebSocket-Verbindung an sich ca. 10 Minuten mit 60-Sekunden-Vorwarnung vor Beendigung, Kontextfenster bis 128k Token. Bei 64,5 s Audio bist du weit innerhalb dieser Grenzen. Quelle: [Firebase — Limits and specifications of the Live API](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs).

## Einordnung für deinen konkreten Fall

Zusammengefasst gibt es **keinen bestätigten Bericht**, der dein exaktes Muster (durchgehende, lückenlose 32-KB-Blöcke ohne Pause, danach `activityEnd`/`audioStreamEnd`) als Fehlerquelle benennt. Die dokumentierten Problemfälle betreffen entweder (a) erratisches Senden mit großen Lücken zwischen winzigen Chunks, (b) sehr lange durchgehende Echtzeit-Live-Streams mit Latenz-Akkumulation über die Zeit, oder (c) Probleme auf der Ausgabeseite (Modell-Sprachantwort), nicht der Transkriptions-Eingabeseite. Dass dein 64,5-s-Test nach 15 s ein vollständiges Ergebnis lieferte, ist eher ein Zeichen dafür, dass die API das schnelle Einspeisen problemlos verarbeitet und mit ihrem eigenen Tempo (schneller als Audio-Echtzeit) durchrechnet, als ein Warnsignal.

## Offen / unbelegt

- Keine offizielle, wörtlich zitierbare Aussage, ob bewusst schnelleres-als-Echtzeit-Senden einer kompletten Datei explizit unterstützt oder abgeraten wird — die Doku behandelt nur den Mikrofon-Fall.
- Keine belegte maximale WebSocket-Frame-/Nachrichtengröße für `realtimeInput`.
- Die Resampling-Aussage zur Live-API-Capabilities-Seite konnte nicht wörtlich verifiziert werden (nur über Suchmaschinen-Zusammenfassung), gilt daher als bestätigt mit Einschränkung.
- Kein bestätigter Fall (mit Repro-Schritten und Google-Antwort) von "Chunks werden verworfen, weil zu schnell gesendet" für den Transkriptions-Eingabepfad gefunden.

## Quellenliste

- [Live translation with Gemini Live API](https://ai.google.dev/gemini-api/docs/live-api/live-translate)
- [Live transcription with Gemini Live API](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe)
- [Get started with Gemini Live API using WebSockets](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket)
- [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities)
- [Live API — WebSockets API reference](https://ai.google.dev/api/live)
- [Firebase AI Logic — Limits and specifications of the Live API](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs)
- [google-gemini/cookbook — Get_started_LiveAPI.py](https://github.com/google-gemini/cookbook/blob/main/quickstarts/Get_started_LiveAPI.py)
- [googleapis/python-genai Issue #1859 — Real-time Audio Streaming Accumulates Significant Latency & Scrambles Audio](https://github.com/googleapis/python-genai/issues/1859)
- [googleapis/python-genai Issue #872 — Live API Audio Quality bad](https://github.com/googleapis/python-genai/issues/872)
- [googleapis/python-genai Issue #2117 — Premature turnComplete Causes Mid-Sentence Audio Truncation](https://github.com/googleapis/python-genai/issues/2117)
- [googleapis/python-genai Issue #1224 — Session stops responding after first turn_complete](https://github.com/googleapis/python-genai/issues/1224)
- [google/adk-python Issue #2887 — Support gemini audioStreamEnd for realtimeInput](https://github.com/google/adk-python/issues/2887)
- [didierphmartin.com — Gemini Live API: The WebSocket Interface for Text & Voice](https://didierphmartin.com/articles/gemini-websocket-guide.html) (Drittquelle, nicht offiziell)
- [technetexperts.com — Fix Gemini Live API Audio Stream Silently Stops](https://www.technetexperts.com/fix-gemini-live-api-audio-stops/) (Drittquelle, nicht offiziell)
- Nicht ergiebig / nur Navigationsinhalt beim Abruf: `docs.cloud.google.com/gemini-enterprise-agent-platform/models/live-api/send-audio-video-streams`, `.../best-practices`, `.../troubleshooting`
