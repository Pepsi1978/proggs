# Gemini Live API – Manuelle Aktivitätssteuerung (activityStart/activityEnd) und der offizielle Weg für vorab aufgenommenes Audio

Recherche von R2 im Rahmen des Gemini-Live-API-Recherche-Schwarms (bidiGenerateContent, v1beta, Fokus `gemini-3.5-transcribe-live`).

## Kernbefund: activityStart/activityEnd und audioStreamEnd schließen sich laut offizieller Referenz gegenseitig aus

Die offizielle WebSocket-API-Referenz definiert die drei Felder von `BidiGenerateContentRealtimeInput` wörtlich so:

- `activityStart`: „Optional. Marks the start of user activity. **This can only be sent if automatic (i.e. server-side) activity detection is disabled.**"
- `activityEnd`: „Optional. Marks the end of user activity. **This can only be sent if automatic (i.e. server-side) activity detection is disabled.**"
- `audioStreamEnd`: „Optional. Indicates that the audio stream has ended, e.g. because the microphone was turned off. **This should only be sent when automatic activity detection is enabled (the default).**"

**BESTÄTIGT** — Quelle: [Live API – WebSockets API reference](https://ai.google.dev/api/live)

Diese drei Sätze sind die zentrale Antwort auf die Frage im Auftrag: `activityStart`/`activityEnd` sind explizit an **deaktivierte** automatische (serverseitige) Aktivitätserkennung gebunden; `audioStreamEnd` ist explizit an **aktivierte** (Standard-)Aktivitätserkennung gebunden. Das sind laut Referenztext zwei getrennte, sich gegenseitig ausschließende Steuerungsmodi — kein kombinierter Ablauf.

Diese Trennung wird durch eine zweite, unabhängig formulierte Textstelle aus der Live-API-Doku bestätigt: „When automatic VAD is disabled, the client is responsible for detecting user speech and sending `activityStart` and `activityEnd` messages at the appropriate times. **An `audioStreamEnd` isn't sent in this configuration.** Instead, any interruption of the stream is marked by an `activityEnd` message."

**BESTÄTIGT** — Quelle: [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities) (deckungsgleich mit Formulierungen im Live API Guide, https://ai.google.dev/gemini-api/docs/live-guide)

### Einordnung des gemessenen Kontexts (R1/Team-Praxis: `activityEnd` gefolgt von `audioStreamEnd`)

Nach dieser Quellenlage ist das Senden von `audioStreamEnd` **zusätzlich** zu `activityEnd` bei deaktivierter automatischer VAD **nicht der von Google dokumentierte Weg** — die Referenz sagt ausdrücklich, `audioStreamEnd` solle *nur* bei aktivierter automatischer Erkennung gesendet werden, und der Capabilities-Guide sagt ausdrücklich, bei deaktivierter VAD werde *kein* `audioStreamEnd` gesendet. Dass es in der Praxis (36,7 s Audio, 12 s Pause) den vollständigen Text und zuverlässig `generationComplete` gebracht hat, während es ohne die Deaktivierung nur 61 Zeichen waren, spricht **nicht** dagegen, dass die Deaktivierung selbst (das eigentliche `automaticActivityDetection.disabled=true` + `activityStart`/`activityEnd`) die Ursache der Verbesserung ist — das zusätzliche `audioStreamEnd` danach ist laut Dokumentation vermutlich redundant bzw. für einen nicht vorgesehenen Zustand, aber es gibt **keine Quelle**, die ein Fehlverhalten oder einen Fehlercode durch diese Kombination explizit belegt oder ausschließt (siehe „Offen / unbelegt"). Empfehlung aus der Doku-Lage: `audioStreamEnd` bei deaktivierter VAD weglassen und sich auf `activityEnd` allein verlassen, das laut Doku „immediate finalization" auslöst.

## Offizielles Codebeispiel für manuelle Aktivitätssteuerung

Der Capabilities-Guide enthält ein direktes Python- und JavaScript-Beispiel für den manuellen Modus:

```python
config = {
    "realtime_input_config": {"automatic_activity_detection": {"disabled": True}},
}
await session.send_realtime_input(activity_start=types.ActivityStart())
await session.send_realtime_input(
    audio=types.Blob(data=audio_bytes, mime_type="audio/pcm;rate=16000")
)
await session.send_realtime_input(activity_end=types.ActivityEnd())
```

```javascript
session.sendRealtimeInput({ activityStart: {} })
session.sendRealtimeInput({ audio: {...} })
session.sendRealtimeInput({ activityEnd: {} })
```

**BESTÄTIGT** — Quelle: [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities)

In beiden Beispielen taucht `audioStreamEnd` nicht auf — ein weiterer indirekter Beleg dafür, dass es im manuellen Modus nicht vorgesehen ist.

## audioStreamEnd im (einzig dafür vorgesehenen) automatischen VAD-Modus

Bei aktivierter automatischer VAD dient `audioStreamEnd` dazu, gepufferte Audiodaten zu erzwingen zu verarbeiten, wenn der Stream länger als eine Sekunde pausiert (z. B. Mikrofon aus): „an `audioStreamEnd` event should be sent to flush any cached audio." Es überspringt dabei laut Doku die serverseitige Stille-Verzögerung: „treats the `audio_stream_end` signal as an immediate finalization prompt, bypassing the default server-side silence detection delay."

**BESTÄTIGT** — Quelle: [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities) / [Live API Guide](https://ai.google.dev/gemini-api/docs/live-guide)

## Kein offizielles Cookbook-Beispiel für „vorhandene Audiodatei per Live-API transkribieren"

Das offizielle Gemini-Cookbook-Quickstart-Skript `Get_started_LiveAPI.py` wurde vollständig nach `activity_start`, `activity_end`, `automatic_activity_detection` und `audio_stream_end` durchsucht — **keiner dieser Begriffe kommt vor**. Das Skript sendet ausschließlich Live-Mikrofon-Audio kontinuierlich über `send_realtime_input(audio=blob)`; es enthält keine Verarbeitung einer bereits vorhandenen Datei und keine manuelle Aktivitätssteuerung.

**BESTÄTIGT** (Negativbefund durch Volltextprüfung) — Quelle: [cookbook/quickstarts/Get_started_LiveAPI.py](https://github.com/google-gemini/cookbook/blob/main/quickstarts/Get_started_LiveAPI.py)

Auch der Google-eigene `gemini-live-api-dev`-Skill (für Coding-Agenten gedacht) nennt `activityStart`/`activityEnd` nicht und erwähnt `audioStreamEnd` nur im Kontext „send `audioStreamEnd` when the mic is paused to flush cached audio" — kein Beispiel für die Verarbeitung einer fertigen Datei.

**BESTÄTIGT** — Quelle: [google-gemini/gemini-skills – SKILL.md](https://github.com/google-gemini/gemini-skills/blob/main/skills/gemini-live-api-dev/SKILL.md)

Eine gezielte Suche nach Cookbook-Issues, Google-Cloud-Best-Practices- und Troubleshooting-Seiten sowie Foren-Treffern ergab **kein** offizielles Beispiel mit dem Titel/Inhalt „bestehende/vorab aufgenommene Audiodatei über die Live-API senden". Die Google-Cloud-Best-Practices-Seite zur Live API (Gemini Enterprise Agent Platform) konnte inhaltlich nicht sauber abgerufen werden (nur Navigationsgerüst ohne Fließtext) und liefert daher keinen verwertbaren Beleg in beide Richtungen.

**Offen/unbelegt** — kein positiver oder negativer Beleg aus `docs.cloud.google.com/gemini-enterprise-agent-platform/models/live-api/best-practices`.

## Der von Google vorgesehene Weg für bereits fertige Aufnahmen: Gemini Transcribe (nicht die Live-API)

Der Live-Transcribe-Guide selbst grenzt sich explizit von Datei-Verarbeitung ab und verweist im Abschnitt „What's next" aktiv weg von der Live-API: „Read the Gemini Transcribe documentation for non-streaming audio files." Das ist die klarste, direkteste offizielle Aussage zum zweiten Teil der Frage: Für fertige Aufnahmen ist **nicht** die Live-API vorgesehen, sondern eine eigene synchrone/non-streaming Transkriptions-API.

**BESTÄTIGT** — Quelle: [Live transcription with Gemini Live API](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe)

Die Zielseite (`Audio transcription`) beschreibt diesen Weg so: „Synchronous transcription transcribes complete, pre-recorded audio files in a single request" mit dem Modell `gemini-3.5-transcribe` (Preview-Variante `gemini-3.5-transcribe-preview` wird an anderer Stelle ebenfalls genannt). Ablauf laut Doku:

1. Datei-Upload über die Files API: `client.files.upload(file="path/to/sample.mp3")`
2. Die zurückgegebene URI wird als `input`-Element vom Typ `"audio"` mit MIME-Type an das Transkriptions-Modell übergeben — nicht als Live-WebSocket-Stream, sondern als ein einzelner (synchroner) Request.

**BESTÄTIGT** — Quelle: [Audio transcription | Gemini API](https://ai.google.dev/gemini-api/docs/transcribe)

Das deckt sich mit der allgemeinen Abgrenzung aus der Live-API-Übersicht: Live-Transkription (`gemini-3.5-transcribe-live`) ist für „a dedicated, low-latency speech recognition pipeline" bei echtem Streaming gedacht, während bereits vorliegende Dateien über die separate, nicht-streamende Transcribe-API laufen sollen.

## Offen / unbelegt

- Kein offizieller Beleg (Cookbook, SDK-Doku, Google-Forum) für ein konkretes End-to-End-Beispiel „lies eine .wav-Datei ein und schicke sie mit `activityStart`/`activityEnd` über die Live-API" — dieses Pattern lässt sich aus der Referenz-Doku nur ableiten (manueller VAD-Codeblock + eigene Chunking-Schleife über eine Datei statt Mikrofon), ist aber nirgends als offizielles „So transkribiert man eine fertige Datei per Live-API"-Beispiel dokumentiert.
- Kein expliziter Beleg dafür, dass das gleichzeitige Senden von `activityEnd` **und** `audioStreamEnd` bei deaktivierter VAD einen Fehler, eine Warnung oder unerwünschtes Serververhalten auslöst — nur der indirekte Schluss aus den sich gegenseitig ausschließenden Feldbeschreibungen der Referenz.
- GitHub-Issue [google/adk-python#2887](https://github.com/google/adk-python/issues/2887) bestätigt nur, dass `audioStreamEnd` in der ADK-Bibliothek als separates Feature neben bereits unterstütztem `ActivityStart`/`ActivityEnd` nachgerüstet wurde (Issue ist geschlossen); die eigentliche Diskussion/Begründung im Issue war beim Abruf nicht als Volltext-Kommentare verfügbar, nur Metadaten.
- Der Google-Cloud-Best-Practices-Text zur Live API (Gemini Enterprise Agent Platform) ließ sich nicht inhaltlich prüfen (nur Navigationsstruktur abrufbar) — möglicher blinder Fleck für weitere Praxishinweise.

## Quellenliste

- [Live API – WebSockets API reference](https://ai.google.dev/api/live) — Feldreferenz `activityStart`/`activityEnd`/`audioStreamEnd`
- [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities) — manuelle VAD, Codebeispiele, audioStreamEnd-Verhalten
- [Live API Guide](https://ai.google.dev/gemini-api/docs/live-guide) — deckungsgleiche Formulierungen zu VAD/audioStreamEnd
- [Live transcription with Gemini Live API](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe) — Verweis weg von Live-API für non-streaming Dateien
- [Audio transcription | Gemini API](https://ai.google.dev/gemini-api/docs/transcribe) — offizieller Weg für fertige Aufnahmen (`gemini-3.5-transcribe`, Files API)
- [cookbook/quickstarts/Get_started_LiveAPI.py](https://github.com/google-gemini/cookbook/blob/main/quickstarts/Get_started_LiveAPI.py) — Negativbefund: kein activity_start/end, kein Datei-Beispiel
- [google-gemini/gemini-skills – SKILL.md](https://github.com/google-gemini/gemini-skills/blob/main/skills/gemini-live-api-dev/SKILL.md) — kein Datei-Beispiel
- [google/adk-python Issue #2887](https://github.com/google/adk-python/issues/2887) — audioStreamEnd als separates Feature in ADK
- [Gemini Enterprise Agent Platform – Live API best practices](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/live-api/best-practices) — Inhalt nicht verwertbar abrufbar (offen)
