# OpenAI Live- und Realtime-Sprach-API — Best Practices (Stand 2026-10-16)

> Gegenstück zu `bugs/apis/openai-live-realtime.md`. Allgemeine OpenAI-Regeln (Responses API,
> Structured Outputs, Retry) stehen in `best-practices/apis/openai-api.md`; Android-Audio-Grundlagen
> ergänzend im Skill `android-audio`.
> Recherche 16.10.2026, 12:15 (research-Skill, 9 parallele Researcher, Quellen:
> developers.openai.com, openai-python-Typdefinitionen auf GitHub, Android-Entwicklerdoku, Community-Forum,
> Pydantic AI, LiveKit, Pipecat, Azure-Spiegel).
> **Versions-Anker:** OpenAI API Stand 16.10.2026, gpt-live-1 GA 10.09.2026, gpt-realtime-2.1 06.07.2026,
> openai-python Stand 16.10.2026.
> **Anker:** openai-api=2026-10-16
> Ergänzt 28.10.2026, 15:35 (research-Skill, Sonnet-Schwarm, 3 Researcher): Modellstand (kein Nachfolger von gpt-live-1 in der API),
> Sprachzuordnung der 9 Stimmen vom 16.10.2026, ChatGPT-Stimmen Miko/Sonna (GPT-Live-1.1) nur in der App.
> Ergänzt 16.10.2026, 19:23: **§14 Praxis-Erkenntnisse aus Jarvis 1.8.100–1.8.108** (eigener Code, Commits, Logcat-Messung
> eines Live-Tests) mit eigener Belegkennzeichnung, Ursachen der Abhacker, bewährten Werten und Rückfall-Prüfung.
>
> Markierung je Aussage: **[offiziell]** = OpenAI-/Google-Doku oder SDK-Quelltext · **[extern]** = Drittanbieter-Doku/Blog ·
> **[Community]** = Forum-/Nutzerbericht · **[unbestätigt]** = widersprüchlich oder nicht verifiziert · **[Schluss]** = eigene Folgerung.
> Hinweis zur Quellenlage: Die OpenAI-Seiten wurden per WebFetch (zusammenfassend) gelesen; wörtlich geprüft sind nur die
> Python-SDK-Typen. JSON-Beispiele sind aus Feldnamen der Referenz zusammengesetzt, nicht gegen die API getestet.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Produkt wählen | Bestehender Text-Agent + Sprache, planbare Kosten → **GPT-Live** (`gpt-live-1`); ein Modell soll alles in einer Sitzung → **Realtime** (`gpt-realtime-2.1`/`-mini`) | §1 |
| 2 | Modell-IDs | Nie neu auf `gpt-realtime`/`gpt-realtime-mini` (Abschaltung 20.01.2027) oder `gpt-4o-*-transcribe`/`whisper-1` (26.02.2027) bauen | §2 |
| 3 | Live verbinden | `wss://api.openai.com/v1/live/sessions`, Modell nur im Body; zuerst `session.start`, auf `session.started` warten | §3, §4 |
| 4 | Live-Konfiguration | Modell, Instructions, Stimme, Audioformat, Delegationstyp sind nach Start **unveränderlich** — vorher festlegen | §4 |
| 5 | Delegation responses | Funktionsaufruf aus verschachteltem `response.event` lesen → `response.item.create` (`function_call_output`) → **immer** `response.create` | §5 |
| 6 | Delegation client | `session.delegation.created` enthält keinen Auftragstext → aus Transkript-Deltas ableiten; Ergebnis per `session.commentary.append` (≤ 500 Tokens) | §5 |
| 7 | Barge-in | Live: kein Cancel/Truncate, Wiedergabepuffer selbst leeren · Realtime-WebSocket: Wiedergabe stoppen + `conversation.item.truncate` mit **gehörter** `audio_end_ms` | §7 |
| 8 | Kosten | Live 0,05 USD/Min läuft auch bei Stille/Mute → `session.close` + auf `session.closed` warten; Realtime wächst überlinear mit der Gesprächslänge | §8, §9 |
| 9 | Android-Audio | `VOICE_COMMUNICATION`, PCM16 mono 24 kHz, 100-ms-Blöcke (4.800 Byte), AEC prüfen, `setCommunicationDevice`, FGS-Typ `microphone` aus sichtbarer Activity | §11 |
| 10 | Optionslisten | Stimmen/Effort/Backend-Modelle nicht fest verdrahten (SDK, Doku und Modellseiten weichen ab) | §13 |
| 11 | Deutsche Stimme | Live: `nuri`, `juni` (w), `flitz` (m) sind offiziell Deutsch; es gibt kein Sprach-/Locale-Feld, Sprache hängt an der Stimme. ChatGPT-Namen (Miko, Sonna) sind **keine** API-IDs | §13 |
| 12 | Neuestes Modell (28.10.2026) | API: weiter nur `gpt-live-1` und `gpt-realtime-2.1`/`-mini`; GPT-Live-1.1 nur in ChatGPT, für die API „coming soon“ | §2 |
| 11 | API-Schlüssel | Standard-Key gehört auf einen Server; in einer verteilten App nur Ephemeral-Key — private Eigen-App mit eigenem Key ist bewusstes Restrisiko | §12 |
| 12 | GPT-Live flüssig abspielen | Live liefert auch in Pausen Stille-Audio in Echtzeit → „spricht" nur per Pegel erkennen; Puffer wächst nie von selbst → Polster **in Pausen hinter Stille** aufbauen, Füll-Stille nur bei fast leerem Puffer, nie mitten in die Rede; Medienton statt Telefonmodus; Rückfall-Prüfung per Logcat-Tag `JarvisLiveRouting` | §14 |
| 13 | Live vor Hintergrundarbeit | Ton-/Mikro-Faden `URGENT_AUDIO`, Socket-Lesefaden `AUDIO`; eigene Hinweise im Gespräch stumm (`GespraechsFokus.stilleHinweise`); nichts Blockierendes zwischen Füllstandsmessung und `write()` | §14.2 Nr. 9 |

---

## 1. Produktwahl: GPT-Live vs. Realtime vs. Kette

- **GPT-Live** (`gpt-live-1`, Endpunkt `v1/live/sessions`): Vollduplex, hört und spricht gleichzeitig; Denken und Tools laufen in einem **getrennten Backend** (eigener Agent = `client`-Delegation oder ein von OpenAI betriebenes Responses-Modell = `responses`-Delegation). Wählen, wenn ein bestehender Text-Agent sprechfähig werden soll und der Nutzer während längerer Backend-Arbeit weiterreden soll. Die Audio-Seite empfiehlt GPT-Live für neue Gesprächs-Sprachanwendungen. [offiziell] https://developers.openai.com/api/docs/guides/voice-agents · https://developers.openai.com/api/docs/guides/audio
- **Realtime API** (`gpt-realtime-2.1`, `-mini`): ein einziges Modell übernimmt Zuhören, Entscheiden, Tools und Sprechen in einer Sitzung; volle Steuerung (VAD, Rauschfilter, Transkriptionsmodell, Truncation, `output_modalities`, `max_output_tokens`, Bild-Eingabe am Sprachmodell). Wählen, wenn direkte Tool-Calls/Bild-Eingabe am Sprachmodell nötig sind. [offiziell] https://developers.openai.com/api/docs/guides/voice-agents · https://developers.openai.com/api/docs/guides/realtime
- **Kette** (STT → Text-Modell → TTS): wenn jede Stufe sichtbar oder austauschbar sein muss. STT `gpt-transcribe` / `gpt-live-transcribe`, TTS `gpt-4o-mini-tts` (Streaming, `pcm`/`wav`, Stil per `instructions`). Agents-SDK-`VoicePipeline` hat kein eingebautes Barge-in. [offiziell] https://developers.openai.com/api/docs/guides/voice-agents · https://openai.github.io/openai-agents-python/voice/pipeline/
- Realtime kann **kein** fremdes Textmodell anhängen (`model` listet nur Audio-/Realtime-Modelle); dafür ist GPT-Live-Delegation gedacht. Das frühere Chat-Supervisor-Muster (`openai-realtime-agents`) ist damit abgelöst. [offiziell] https://developers.openai.com/api/reference/resources/realtime/subresources/client_secrets/methods/create · https://github.com/openai/openai-realtime-agents
- Latenz (Herstellerangabe, Forum-Spiegel der Ankündigung): Turn-Taking 0,798 s (Live mit GPT-6-Astra-Backend) gegenüber 1,41 s (gpt-realtime-2.1). Keine Garantie. [Community] https://community.openai.com/t/introducing-gpt-live-1-in-the-api/1396471
- Migration Realtime → Live: https://developers.openai.com/api/docs/guides/live-migration [offiziell]

## 2. Exakte Modell-IDs und Abkündigungen

| Modell | Zweck | Status | Quelle |
|---|---|---|---|
| `gpt-live-1` | Live-Sprachmodell | GA seit 10.09.2026; einzige Live-Variante in der API (ein „GPT-Live-1 mini" existiert nur in ChatGPT laut Sekundärquellen) | [offiziell] https://developers.openai.com/api/docs/models/gpt-live-1 · https://developers.openai.com/api/docs/changelog |
| (kein Nachfolger, Stand 28.10.2026) | — | Modellkatalog und Changelog nennen nur `gpt-live-1` (einziger Snapshot, Default); kein `gpt-live-1.1`, `-mini`, `-2` oder datierter Snapshot. GPT-Live-1.1 läuft seit 15.10.2026 nur in der ChatGPT-App, für die API laut OpenAI „coming soon“ ohne Datum. Seit GA nur Zubehör: Decisions-Delegation (15.10.), Prompt-to-Voice und 9 Sprachstimmen (16.10.) | [offiziell] https://developers.openai.com/api/docs/models · https://developers.openai.com/api/docs/changelog · [extern] all-ai.de, notebookcheck.net |
| `gpt-realtime-2.1` | Realtime-Flaggschiff (Reasoning) | Release 06.07.2026; bessere Zahlen-/Buchstabenerkennung, Rausch-/Stille-Handling, Unterbrechungen | [offiziell] https://developers.openai.com/api/docs/models/gpt-realtime-2.1 |
| `gpt-realtime-2.1-mini` | Realtime, günstig (Reasoning, destilliert) | Release 06.07.2026; OpenAI warnt: Instruktionstreue und Function Calling schwächer | [offiziell] https://developers.openai.com/api/docs/models/gpt-realtime-2.1-mini |
| `gpt-realtime-2` | Vorgänger von 2.1 | Release 07.05.2026 | [offiziell] https://developers.openai.com/api/docs/models/gpt-realtime-2 |
| `gpt-realtime-1.5` | ohne Reasoning, Kontext 32k | keine Abkündigung gelistet | [offiziell] https://developers.openai.com/api/docs/models/gpt-realtime-1.5 |
| `gpt-realtime` (`-2025-08-28`) | Altmodell | **Abschaltung 20.01.2027**, Ersatz `gpt-realtime-2.1` | [offiziell] https://developers.openai.com/api/docs/deprecations |
| `gpt-realtime-mini` | Altmodell | **Abschaltung 20.01.2027**, Ersatz `gpt-realtime-2.1-mini`; Snapshot `gpt-realtime-mini-2025-10-06` seit 23.07.2026 abgeschaltet | [offiziell] https://developers.openai.com/api/docs/deprecations |
| `gpt-4o-realtime-preview*` | Beta-Modelle | abgeschaltet 07.05.2026 (Beta-Header 12.05.2026) | [offiziell] https://developers.openai.com/api/docs/deprecations |
| `gpt-transcribe`, `gpt-live-transcribe` | STT neu | veröffentlicht 28.07.2026 | [offiziell] https://developers.openai.com/api/docs/models/gpt-transcribe |
| `gpt-4o-transcribe`, `gpt-4o-mini-transcribe`, `gpt-4o-transcribe-diarize`, `whisper-1` | STT alt | veraltet seit 26.08.2026, **Abschaltung 26.02.2027** | [offiziell] https://developers.openai.com/api/docs/deprecations |
| `gpt-realtime-translate` | Live-Übersetzung | 0,034 USD/Min, nicht für Gespräche | [offiziell] https://developers.openai.com/api/docs/pricing |

Backend-Modelle für Live-`responses`-Delegation: **keine Positivliste** in der Doku, `model` ist ein freier String. Genannt werden `gpt-6-luna` (Startempfehlung), `gpt-6-sol` (komplexer), `gpt-6-astra` (Referenzbeispiel, Benchmarks), `gpt-5.6-luna` (WebSocket-Guide, LiveKit-Standard), `gpt-5.6-terra` (WebRTC-Guide, Pipecat). [offiziell] https://developers.openai.com/api/docs/guides/live-delegation · https://developers.openai.com/api/docs/guides/voice-websockets · https://developers.openai.com/api/docs/guides/voice-webrtc

## 3. Endpunkte, Verbindung, Authentifizierung

- **Live WebSocket:** `wss://api.openai.com/v1/live/sessions`, **ohne Query-Parameter**; Modell gehört in `session.model`. Header `Authorization: Bearer <KEY>`; ein `OpenAI-Beta`-Header wird nicht erwähnt (nicht setzen). [offiziell] https://developers.openai.com/api/docs/guides/voice-websockets · https://developers.openai.com/api/reference/resources/live/primary-websocket
- **Live WebRTC:** Sitzung per `POST /v1/live/sessions`, Audio über Media-Track; `audio.format` weglassen und `session.start` **nicht** über den Data-Channel senden; OpenAI empfiehlt WebRTC für Browser, Mobil und Server. [offiziell] https://developers.openai.com/api/docs/guides/voice-webrtc
- **Realtime WebSocket:** `wss://api.openai.com/v1/realtime?model=gpt-realtime-2.1`, nur `Authorization`-Header, **kein** `OpenAI-Beta: realtime=v1` (Beta seit 12.05.2026 abgeschaltet). Erst `session.created` abwarten, dann `session.update` mit `session.type: "realtime"`. [offiziell] https://developers.openai.com/api/docs/guides/realtime-websocket
- **Realtime WebRTC:** Ephemeral-Key per `POST /v1/realtime/client_secrets` (`expires_after.seconds` 10–7200, Standard 600; ein Secret kann mehrere Sitzungen eröffnen, laufende Sitzung überdauert den Ablauf), SDP per `POST /v1/realtime/calls`. [offiziell] https://developers.openai.com/api/docs/guides/realtime-webrtc · https://developers.openai.com/api/reference/resources/realtime/subresources/client_secrets/methods/create
- Alle Nachrichten sind **JSON-Textframes**; Audio läuft als Base64 im JSON, nicht als Binärframe. [offiziell] https://developers.openai.com/api/docs/guides/live-migration
- Eigene `event_id`s vergeben und `error`-Events mit `client_event_id`/`event_id` loggen; unbekannte Event-Namen protokollieren statt still ignorieren (Beta→GA-Umbenennungen werfen keinen Fehler). [offiziell] https://developers.openai.com/api/reference/resources/realtime/client-events

## 4. Sitzungskonfiguration

### 4.1 Live: `session.start` (erste Nachricht, nur WebSocket)

- Pflicht: `session.model = "gpt-live-1"`. Optional: `instructions` (≤ 16.384 Tokens), `input` (Startverlauf, ≤ 128 Nachrichten / 8.192 Tokens, Rollen `developer`/`user`/`assistant`, je ein Text-Part), `audio.format` (`{type:"audio/pcm", rate:24000}` Standard, `rate:16000`, oder `audio/pcmu`/`audio/pcma` 8 kHz), `audio.output.voice` (Standard `marin` oder `{id:"…"}`), `delegation`, `store` (Standard `false`; `true` = 30 Tage Aufzeichnung, Fork, Mitschnitt-Download), `client.data_channel`, `triggers`. [offiziell] https://developers.openai.com/api/reference/typescript/resources/live/index.md · openai-python `src/openai/types/live/session_config.py`
- **Unveränderlich nach Start:** model, instructions (nur ergänzbar per `session.instructions.append`), audio (Format + Stimme), input, delegation.type. Per `session.update` änderbar: nur `delegation.responses`-Inhalt und `triggers`. [offiziell] https://developers.openai.com/api/docs/guides/live-conversations
- Konfiguration ist strikt: unbekannte Felder werden abgelehnt (`unknown_parameter`). [offiziell, Azure-Spiegel] https://learn.microsoft.com/en-us/azure/foundry/openai/gpt-live-reference
- **Kein Sprachfeld, keine Turn-Erkennung, kein VAD, kein Rauschfilter, kein Transkriptionsmodell, keine Temperatur, kein `max_output_tokens` am Sprachmodell.** Sprache (Deutsch) per Instructions in der Zielsprache plus `developer`-Eröffnungsnachricht in `input` festlegen; Deutsch ist nirgends offiziell dokumentiert → mit der gewählten Stimme testen. [offiziell] https://developers.openai.com/api/docs/guides/live-prompting · https://developers.openai.com/api/docs/guides/live-migration

Minimalbeispiel (Client-Delegation):
```json
{ "type": "session.start", "event_id": "start-1",
  "session": { "model": "gpt-live-1", "instructions": "Sprich Deutsch. Sei kurz.",
    "audio": { "format": { "type": "audio/pcm", "rate": 24000 }, "output": { "voice": "marin" } },
    "delegation": { "type": "client" } } }
```

Mit Responses-Delegation (zusammengesetzt, nicht als Ganzes getestet):
```json
{ "type": "session.start", "event_id": "start-1",
  "session": { "model": "gpt-live-1",
    "instructions": "Sprich Deutsch. Antworte kurz. Delegiere, wenn aktuelle Daten oder Aktionen nötig sind. Nenne Ergebnisse erst, wenn das Backend geantwortet hat.",
    "input": [ { "type": "message", "role": "developer",
      "content": [ { "type": "input_text", "text": "Begrüße den Nutzer kurz auf Deutsch." } ] } ],
    "audio": { "format": { "type": "audio/pcm", "rate": 24000 }, "output": { "voice": "marin" } },
    "store": false,
    "delegation": { "type": "responses", "responses": {
      "model": "gpt-6-luna", "instructions": "Backend eines Sprachassistenten. Knapp, Deutsch.",
      "max_output_tokens": 2048, "reasoning": { "effort": "low" }, "text": { "verbosity": "low" },
      "service_tier": "auto", "parallel_tool_calls": true, "tool_choice": "auto",
      "tools": [ { "type": "web_search" },
        { "type": "function", "name": "get_weather", "description": "Wetter für einen Ort.",
          "parameters": { "type": "object", "properties": { "location": { "type": "string" } },
            "required": ["location"], "additionalProperties": false } } ] } } } }
```

### 4.2 Realtime: `session.update` (nach `session.created`)

- Pflicht `session.type: "realtime"`; Audioformat als **Objekt** `{type:"audio/pcm", rate:24000}` (PCM nur 24 kHz); Stimme unter `session.audio.output.voice` (nicht Top-Level) und **vor der ersten Audioausgabe** setzen. [offiziell] https://developers.openai.com/api/reference/resources/realtime/client-events · https://developers.openai.com/api/docs/guides/realtime-conversations
- Weitere Felder: `reasoning.effort` (`minimal|low|medium|high|xhigh`, nur 2/2.1/2.1-mini), `audio.input.turn_detection` (`server_vad`: threshold 0.5, prefix_padding_ms 300, silence_duration_ms 500, idle_timeout_ms; `semantic_vad`: eagerness low/medium/high/auto; `create_response`, `interrupt_response`; `null` = manuell committen), `audio.input.noise_reduction` (`near_field` Headset / `far_field` Raum / `null`), `audio.input.transcription` (`model`, `language` z. B. `de`, `prompt`, `keywords`), `output_modalities` (`["audio"]` oder `["text"]`), `max_output_tokens` (1–4096 oder `"inf"`), `truncation`, `tools`, `tool_choice`, `parallel_tool_calls`. [offiziell] openai-python `src/openai/types/realtime/*.py` · https://developers.openai.com/api/docs/guides/realtime-vad

## 5. Delegation (GPT-Live)

- Delegationstyp wird beim Start gewählt (`client` Standard, `responses`, `decisions`); Wechsel erfordert neue Sitzung. [offiziell] https://developers.openai.com/api/docs/guides/live-delegation
- **Rollentrennung:** Persona, Sprechstil, Delegationsregeln („delegiere bei X, nicht bei Y"), Antwortlänge in `session.instructions`; Aufgabenprozeduren, Tool-Regeln, Fachwissen in den Backend-Instructions. Am besten **ein** Delegationswerkzeug statt vieler Tool-Schemas im Voice-Prompt. Berechtigungen und Bestätigungen prüft die App, nie das Sprachmodell. [offiziell] https://developers.openai.com/api/docs/guides/live-migration · https://developers.openai.com/api/docs/guides/live-prompting

### 5.1 `responses`-Delegation — Funktionsaufruf-Ablauf
1. Server: `session.delegation.created` mit `delegation.target:"responses"` und `response_id`.
2. Server: `response.event` mit verschachteltem `event.type:"response.output_item.done"`, `item.type:"function_call"` (`call_id`, `name`, `arguments`). `response.completed.output` bzw. `response.done.output` ist dabei **leer** — Funktionsaufrufe nur aus den verschachtelten `output_item.done` lesen.
3. App führt aus und sendet `response.item.create` mit `{type:"function_call_output", call_id, output}`.
4. **Danach immer `response.create`** — „Appending an item alone does not resume the backend"; sonst `tool_outputs_required` / `function_call_outputs_required`. Bei parallelen Calls ein Ergebnis je `call_id`, dann **genau ein** `response.create`.
5. MCP-Freigabe: Item `mcp_approval_request` → `response.item.create` mit `mcp_approval_response` (`approval_request_id`, `approve`).
6. Backend-Tokens aus `response.completed` (verschachtelt in `response.event`) lesen, **je Response-ID einmal** zählen.
[offiziell] https://developers.openai.com/api/docs/guides/live-delegation

```json
{ "type": "response.item.create", "event_id": "fo-1",
  "item": { "type": "function_call_output", "call_id": "call_123", "output": "{\"temperature\":17}" } }
{ "type": "response.create", "event_id": "cont-1" }
```

- Backend-Felder: `model` (Pflicht), `instructions`, `max_output_tokens` (≥ 16), `parallel_tool_calls`, `tool_choice` (`auto|none|required|{type:"function",name}|{type:"mcp",server_label,name?}`), `tools`, `text.verbosity` (`low|medium|high`, steuert nur Backend-Text), `service_tier` (`auto|default|flex|priority|ultrafast|fast_tier_temp_pilot`), `reasoning.effort`/`.summary`. [offiziell] openai-python `types/live/responses_delegation_config.py`
- MCP-Zugangsdaten (`authorization`, `headers`) werden durchgereicht, aber in der Sitzungskonfiguration **nicht zurückgegeben** → bei jedem Ersetzen von `tools` neu mitsenden. Bei `session.update` sicherheitshalber das **vollständige** Delegationsobjekt senden (Doku: „sparse", Azure-Referenz: Komplettersatz — Widerspruch). [offiziell + unbestätigt] https://developers.openai.com/api/docs/guides/live-delegation · https://learn.microsoft.com/en-us/azure/foundry/openai/gpt-live-reference
- Für die erste Migration `parallel_tool_calls:false`; später parallel für Latenz. [offiziell] https://developers.openai.com/api/docs/guides/live-migration
- Latenzhebel: niedriger Effort, `service_tier:"priority"`, kurze Ergebnisse, parallele unabhängige Tool-Calls, „Zeit bis zur ersten nützlichen gesprochenen Antwort" messen. [offiziell] https://developers.openai.com/api/docs/guides/live-delegation

### 5.2 `client`-Delegation (eigenes Backend, z. B. vorhandener Agent)
- Server sendet `session.delegation.created` mit `delegation.{id, type:"delegation", target:"client"}` und `offset_ms` — **ohne Auftragstext**. Den Wunsch aus `session.input_transcript.delta` / `session.output_transcript.delta` ableiten (Transkripte deshalb laufend mitschreiben). [offiziell] https://developers.openai.com/api/docs/guides/live-delegation
- Ergebnis zurück per `session.commentary.append` (wird paraphrasiert gesprochen) oder `session.thinking.append` (stiller Kontext), jeweils `delegation_id` + `content` (String, **≤ 500 Tokens**). Mehrere Appends mit gleicher `delegation_id` setzen fort (Zwischenstand „Ich suche noch …" möglich). Sitzungsweite Anweisungen: `session.instructions.append` mit `delegation_id: null` (kann laufende Rede unterbrechen). [offiziell]
- `session.commentary.appended` (`client_event_id`, `start_ms`, `end_ms`) bestätigt nur die **Annahme**, nicht dass gesprochen wurde. [offiziell]
```json
{ "type": "session.commentary.append", "event_id": "res-1",
  "delegation_id": "item_delegation_123", "content": "Es hat 17 Grad und regnet." }
```
- Eine laufende Delegation lässt sich **nicht abbrechen**; Backend-Arbeit läuft beim Barge-in weiter. Pro Aufgabe Operations-ID + Revisionsnummer führen, veraltete Ergebnisse verwerfen, zustandsändernde Tools mit Bestätigung. [offiziell + extern] https://developers.openai.com/api/docs/guides/live-delegation · https://pydantic.dev/docs/ai/realtime/openai-live/
- `decisions`-Delegation (seit 15.10.2026: typisierte Antworten, `session.triggers` auf Transkript-Deltas) — Preis offiziell unbelegt; für einfache Sprachassistenten nicht nötig. [offiziell] https://developers.openai.com/api/docs/changelog

## 6. Events, Turn-Bildung, Sitzungsende (Live)

- Client → Server: `session.start`, `session.update`, `session.input_audio.append` (keine Bestätigung), `session.input_audio.mute`/`.unmute`, `session.output_audio.fixture.play` (≤ 30 s Festaudio), `session.instructions.append`, `session.thinking.append`, `session.commentary.append`, `response.item.create`, `response.create`, `session.close`. [offiziell] https://developers.openai.com/api/reference/resources/live/primary-websocket
- Server → Client: `session.started` (`id`, `expires_at`, aufgelöste Konfig), `session.updated`, `session.output_audio.delta` (`delta`, `start_ms`/`end_ms` — auf primärer WebSocket evtl. ausgelassen, tolerant parsen), `session.input_transcript.delta`, `session.output_transcript.delta`, `session.input_audio.muted/unmuted`, `session.*.appended`, `session.delegation.created`, `response.event`, `session.usage.updated` (`usage.seconds` **kumulativ**, `context_window.usage_ratio`, ca. 1×/Min), `session.closed` (`reason`: `close_requested|expired|content|remote_hangup|connection_lost`), `error`, `info`. [offiziell]
- **Kein** `speech_started`, **kein** „output_audio.done", **keine** Item-IDs oder Turn-Grenzen. Turns bildet die App selbst (Fragmente in Lieferreihenfolge anhängen, Sprecher und Zeitstempel erhalten, Grenze aus Stille, Pydantic AI nutzt 2.000 ms). Sprechanzeige aus dem Player ableiten. [offiziell + extern] https://developers.openai.com/api/docs/guides/live-migration · https://pydantic.dev/docs/ai/realtime/openai-live/
- Praxis (Jarvis): Live sendet auch in Pausen durchgehend Stille-Deltas → „spricht" nur aus hörbarem Pegel ableiten, nie aus dem Eintreffen von Daten; Details §14.1/§14.2 Nr. 3.
- Transkripte asynchron und ungenau: nur finale Texte speichern, unterbrochene Antworten markieren; der Chat-/Textverlauf bleibt Wahrheitsquelle, nicht das Live-Transkript (Verlauf kann verdichtet werden). [offiziell] https://developers.openai.com/api/docs/guides/live-migration
- Kontext 128.000 Tokens; ab 90 % startet eine Ersatz-Engine mit Original-Instructions und ≤ 8.192 Tokens Verlauf. `usage_ratio` beobachten. [offiziell] https://developers.openai.com/api/docs/guides/live-conversations
- **Sitzungsende:** `session.close` senden, weiterlesen bis `session.closed` (Beispiel wartet bis 15 s), `usage.seconds` **einmal** übernehmen. EOF/Socket-Schließen beendet nicht sauber; Abbruch vor `session.closed` = Nutzung „unbestätigt". Kein automatischer Reconnect: Fortsetzen per Fork (`store:true`) oder neue Sitzung mit Textverlauf in `input`. Maximaldauer: unbelegt, `expires_at` aus `session.started` lesen. [offiziell] https://developers.openai.com/api/docs/guides/live-conversations
- Realtime: **hartes 60-Minuten-Limit** ohne Vorwarnung → bei ca. 50 Min proaktiv neu verbinden, Kontext als Zusammenfassung nachladen. [offiziell] https://developers.openai.com/api/docs/guides/realtime-conversations

## 7. Barge-in / Unterbrechung

- **Live:** Vollduplex, modellgesteuert; kein Cancel-/Truncate-Kommando. Hört der Server auf zu senden, muss die App ihre Audio-Queue selbst leeren (`AudioTrack.flush()`), sonst spielt Gepuffertes weiter. Unterbrechungsverhalten im Prompt steuern („Stop speaking when the user interrupts", „Keep listening while the user pauses to think"). Mute stoppt weder Ausgabe noch Backend. [offiziell] https://developers.openai.com/api/docs/guides/live-migration · https://developers.openai.com/api/docs/guides/live-prompting
- **Realtime per WebSocket:** Client verwaltet die Wiedergabe. Bei `input_audio_buffer.speech_started` Wiedergabe sofort stoppen, tatsächlich **abgespielte** Millisekunden ermitteln und `conversation.item.truncate` mit `item_id` (Assistenten-Item), `content_index: 0`, `audio_end_ms` senden — **auf die empfangene Audiolänge klemmen** (sonst Fehler). Erfolg: `conversation.item.truncated`. [offiziell] https://developers.openai.com/api/docs/guides/realtime-conversations · https://developers.openai.com/api/reference/resources/realtime/client-events · https://github.com/openai/openai-agents-python/pull/4122
- Praxis (Jarvis, Live + Echosperre): Eine `session.input_transcript.delta` während der eigenen Rede ist bei geschlossenem Mikrofon die **verspätete** Mitschrift des vorigen Satzes → Ausgabe dann nicht leeren; Unterbrechen per Antippen. Details §14.2 Nr. 4.
- Realtime per WebRTC/SIP: Server kennt den Wiedergabestand und kürzt automatisch; `output_audio_buffer.clear` nur dort, nach `response.cancel`. [offiziell]

## 8. Kostenmodell und Preise (Stand 16.10.2026, USD)

Quelle für alle Preise: https://developers.openai.com/api/docs/pricing [offiziell] und die jeweiligen Modellseiten (`.../api/docs/models/<id>`). `platform.openai.com/docs/pricing` leitet dorthin weiter; `openai.com/api/pricing` blockiert Abrufe (403).

### 8.1 Live
- **0,05 USD pro Minute** Sprachsitzung, sekundengenau, nicht aufgerundet; läuft auch bei Stille, Mute und während das Backend arbeitet. Backend-Modell und Tools **zusätzlich** zu deren Standardsätzen. WebRTC-Start bucht 15 s, die mit der laufenden Dauer verrechnet werden (nicht zusätzlich). Doku-Beispiel: 90 s = 0,075 USD Voice + 0,02 USD Backend = 0,095 USD. [offiziell] https://developers.openai.com/api/docs/models/gpt-live-1 · https://developers.openai.com/api/docs/guides/voice-latency-cost
- Gleichzeitige Sitzungen: Build 50, Launch 300, Grow 500, Free nicht unterstützt (Modellseite; ein Suchtreffer nennt abweichend Tier 1: 25 / Tier 2: 50 / Tier 3: 200 — [unbestätigt]). Native Transkripte: kein Extrapreis ausgewiesen.

### 8.2 Backend-Modelle (je 1 Mio. Tokens, Standard-Tier, Eingabe ≤ 272K)

| Modell | Input | Cached | Cache-Write | Output | Effort-Werte (Modellseite) |
|---|---|---|---|---|---|
| `gpt-6-luna` | 0,10 | 0,01 | 0,125 | 0,50 | none, low, medium (Std.), high, xhigh, max |
| `gpt-5.6-luna` | 0,20 | 0,02 | 0,25 | 1,20 | none … max, Std. medium |
| `gpt-5.6-terra` | 2,00 | 0,20 | 2,50 | 12,00 | none … max, Std. medium |
| `gpt-6-sol` | 2,00 | 0,20 | 2,50 | 10,00 | none … max, Std. medium |
| `gpt-6.1-sol` | 2,00 | 0,10 | 2,50 | 10,00 | low … max (**kein** none/minimal), Std. medium |
| `gpt-5.6-sol` (= `gpt-5.6`) | 4,00 | 0,40 | 5,00 | 20,00 (Aktion bis mind. 21.11.2026) | none … max, Std. medium |
| `gpt-6-astra` | 10,00 | 1,00 | 12,50 | 50,00 | low … max (**kein** none), Std. unbelegt |
| `gpt-5.5` | 5,00 | 0,50 | – | 30,00 | – |

Service-Tier-Faktoren: `flex`/Batch 0,5×, `priority` (= Fast-Modus, seit 30.07.2026 so benannt) 2×, `ultrafast` 6× (nur `gpt-6-astra` und `gpt-6.1-sol`; Astra-Ultrafast nur US-Residency). Lange Kontexte > 272K: 2× Input, 1,5× Output; Data Residency +10 %. Reasoning-Tokens werden als **Ausgabetokens** abgerechnet. Flex ist für Sprache vermutlich zu langsam [Schluss]. [offiziell] https://developers.openai.com/api/docs/pricing · https://developers.openai.com/api/docs/guides/reasoning

### 8.3 Realtime (je 1 Mio. Tokens: Input / Cached / Output)

| Modell | Audio | Text | Bild (nur Input) | Kontext / max. Ausgabe |
|---|---|---|---|---|
| `gpt-realtime-2.1` | 32 / 0,40 / 64 | 4 / 0,40 / 24 | 5 / 0,50 | 128.000 / 32.000 |
| `gpt-realtime-2.1-mini` | 10 / 0,30 / 20 | 0,60 / 0,06 / 2,40 | 0,80 / 0,08 | 128.000 / 32.000 |
| `gpt-realtime-2` | 32 / 0,40 / 64 | 4 / 0,40 / 24 | 5 / 0,50 | 128.000 / 32.000 |
| `gpt-realtime-1.5` | 32 / 0,40 / 64 | 4 / 0,40 / 16 | 5 / 0,50 | 32.000 / 4.096 |
| `gpt-realtime` (Alt) | 32 / 0,40 / 64 | 4 / 0,40 / 16 | 5 / 0,50 | 32.000 / 4.096 |
| `gpt-realtime-mini` (Alt) | 10 / 0,30 / 20 | 0,60 / 0,06 / 2,40 | 0,80 / 0,08 | 32.000 / 4.096 |

Audio-Token-Zählung: Nutzer 1 Token / 100 ms (600/Min), Assistent 1 Token / 50 ms (1.200/Min); VAD filtert leeres Audio. Die **ganze Konversation** wird bei jeder Antwort erneut als Input gezählt → ohne Cache quadratisches Wachstum. Ratenlimits 2.1: Build 400 RPM/200k TPM, Launch 10.000/4 Mio., Grow 20.000/15 Mio. [offiziell] https://developers.openai.com/api/docs/guides/realtime-costs

### 8.4 Transkription

| Modell | Preis | Hinweis |
|---|---|---|
| `gpt-transcribe` | 0,0045 USD/Min | finale Transkripte, auch Dateien |
| `gpt-live-transcribe` | 0,017 USD/Min | Streaming, niedrige Latenz |
| `gpt-realtime-whisper` | 0,017 USD/Min | Streaming |
| `gpt-4o-mini-transcribe` | ca. 0,003 USD/Min (1,25 in / 5,00 out je 1 Mio. Tokens, geschätzt) | Abschaltung 26.02.2027 |

### 8.5 Rechenbeispiel 10-Minuten-Gespräch (Annahmen, keine Doku-Zahlen)
Annahmen: 5 Min Nutzer, 5 Min Assistent, 20 Antworten, 10 Delegationen, 1.000 Tokens Instructions/Tools.
- Live 0,50 USD Voice + Backend: `gpt-6-luna` leicht 0,51 / schwer 0,54 · `gpt-6-sol` 0,63 / 1,20 · `gpt-6-astra` 1,15 / 4,00 USD (leicht = 40k In/5k Out, schwer = 200k In/30k Out). `priority` verdoppelt nur den Backend-Anteil.
- Realtime `gpt-realtime-2.1`: ca. 0,52 (gecacht) bis 3,30 USD (ungecacht); `-mini`: ca. 0,18 bis 1,02 USD; Reasoning-Tokens kommen obendrauf.
- Fazit: Live ist pro Minute fix und planbar; Realtime wächst überlinear mit der Gesprächslänge. [Schluss aus offiziellen Preisen]

## 9. Kostenhebel

- **Live:** Sitzung aktiv mit `session.close` beenden (Leerlauf-Timer in der App; Mute schließt nicht); bei langen Backend-Aufgaben Sprachsitzung schließen und Kontext später per `input` wiederherstellen; Backend nach **Kosten pro erledigter Aufgabe** wählen („a larger backend model can cost less overall if it completes the task faster"); stabile Backend-Instructions/Tools am Prompt-Anfang (Cache); `text.verbosity: low`; `max_output_tokens` als Deckel; parallele Tool-Calls sparen Sitzungsminuten. [offiziell] https://developers.openai.com/api/docs/guides/voice-latency-cost
- **Realtime:** `truncation: {type:"retention_ratio", retention_ratio: 0.8, token_limits:{post_instructions: N}}` statt ständigem Abschneiden einzelner Items; Instructions/Tools **nach Start nicht ändern** und frühen Verlauf nicht bearbeiten (Cache-Trefferquote; gecachtes Audio 0,40 statt 32 USD); kurze Audioausgabe (Ausgabe-Audio ist der teuerste Posten); erst mit dem großen Modell testen, dann auf mini wechseln; eigener Inaktivitäts-Timer statt nur `idle_timeout_ms` (jede ausgelöste Nachfrage ist eine bezahlte Antwort); `create_response:false` + manuelles `response.create` gegen ungewollte Antworten; `response.done`-Usage (cached/uncached) je Runde loggen. [offiziell] https://developers.openai.com/api/docs/guides/realtime-costs
- **Effort:** für Sprache niedrig anfangen (Backend `none`/`low`, Realtime `low`; `minimal` für einfache latenzkritische Fälle), höher nur mit messbarem Nutzen (Evals). [offiziell] https://developers.openai.com/api/docs/guides/reasoning · https://developers.openai.com/api/docs/guides/realtime-models-prompting
- **Konto-Schutz:** pro App eigenes OpenAI-Projekt, „Monthly spend limit" mit „Enforce a hard limit" plus Spend-Alerts auf niedrigerer Schwelle; Puffer wegen Durchsetzungsverzögerung, Hard Limit am Testprojekt gegenprüfen. [offiziell + Community] https://community.openai.com/t/project-limits-not-being-respected/1300634

## 10. Prompting

- Voice-Prompt minimal, benannte Kurzabschnitte (Rolle, Stil, Sprache, Unterbrechung, Delegationsregeln, Antwortlänge); Tempo/Stil/Aussprache (auch IPA-Hinweise) nur per Prompt — bei Live gibt es kein Speed-Feld. [offiziell] https://developers.openai.com/api/docs/guides/live-prompting · https://developers.openai.com/api/docs/guides/voice-prompting
- Pflichtregel: „Delegate before giving an answer that depends on backend work. Do not guess the result while waiting. Only say an action was completed after the tool call succeeds." [offiziell] https://developers.openai.com/api/docs/guides/live-prompting
- Genau **ein** kurzer, neutraler Füllsatz (Preamble) vor Tool-/Delegationsaufrufen, ohne Aussage über Erfolg; bei langer Dauer Halte-Satz mit Verzögerung, Intervall und Höchstzahl. [offiziell + extern] https://developers.openai.com/api/docs/guides/realtime-models-prompting · https://docs.livekit.io/agents/models/realtime/plugins/gpt-live/
- Gesprochene Antworten höchstens 2 Sätze, keine Listen/Markdown; kein zweiter Umformulierungsdurchlauf. Stille/Hintergrundlärm bei Realtime über ein No-Op-Tool `wait_for_user`. [offiziell] https://developers.openai.com/api/docs/guides/voice-prompting
- Kontext-Startkarte klein (Identität, Stil, Datum/Uhrzeit, wenige Zeilen Lage), beschriftet (aktueller Stand / Hintergrund / veraltet), Details per Werkzeug nachladen; dynamische Teile ans Ende. Früheren Verlauf bei Live in `session.input`, bei Realtime als `message`-Items (`input_text`/`output_text`) oder als eine Zusammenfassung in `instructions`. [offiziell + Community] https://developers.openai.com/api/docs/guides/live-conversations

## 11. Android-Umsetzung (Kotlin, WebSocket mit OkHttp)

- **Transport:** OpenAI empfiehlt WebRTC für Mobilclients (Jitterbuffer, Echo-/Paketverlust-Handling eingebaut). WebSocket mit OkHttp ist technisch erlaubt, dann übernimmt die App Wiedergabestopp, Truncate (Realtime) und Jitter-Glättung selbst. [offiziell] https://developers.openai.com/api/docs/guides/realtime-webrtc · https://ai.engineer/talks/your-realtime-ai-is-ngmi-sean-dubois-openai-kwindla-hultman-kramer-daily
- **Capture:** `AudioRecord` mit `MediaRecorder.AudioSource.VOICE_COMMUNICATION`, `CHANNEL_IN_MONO`, `ENCODING_PCM_16BIT`, 24.000 Hz; Puffer `(minBuffer * 2).coerceAtLeast(4096)`; Lesen in einer Hintergrund-Coroutine. Audio **gleichmäßig in 100-ms-Blöcken = 4.800 Byte** senden (24 kHz × 2 Byte × 0,1 s), gerade Byteanzahl, ungerade Restbytes in den nächsten Block übernehmen; kein Gate, das leise Blöcke durch Nullen ersetzt. [offiziell für Format/Blockgröße, extern für Puffer] https://developers.openai.com/api/docs/guides/voice-websockets · https://docs.smallest.ai/atoms/atoms-platform/integrate/mobile-integrations/android-kotlin.md
- **AEC:** `AcousticEchoCanceler.isAvailable()` prüfen, `AcousticEchoCanceler.create(audioRecord.audioSessionId)`, danach `getEnabled()` kontrollieren (Plattform kann AEC je nach AudioSource schon einsetzen); `NoiseSuppressor` analog. Hardware-AEC kann gerätespezifisch wirkungslos sein → Software-AEC (libwebrtc) als Rückfall. [offiziell + extern] https://developer.android.com/reference/android/media/audiofx/AcousticEchoCanceler
- **Modus und Routing:** `AudioManager.MODE_IN_COMMUNICATION` für die Gesprächsdauer, im `finally` zurücksetzen. Lautsprecher ab API 31 mit `setCommunicationDevice(device)` aus `availableCommunicationDevices`: Rückgabewert prüfen, bis zu 30 s auf Bestätigung (`AudioDeviceCallback`/Listener) warten, bei Fehler `clearCommunicationDevice()` und neu setzen, am Ende `clearCommunicationDevice()`. `setSpeakerphoneOn` ist veraltet. [offiziell] https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-manager
- **Playback:** `AudioTrack` `MODE_STREAM`, 24 kHz mono PCM16, Puffer ≥ `getMinBufferSize * 4`; eingehende Deltas in eine `LinkedBlockingQueue`, eigener Schreib-Thread (poll 50 ms). Bei Unterbrechung Queue leeren + `flush()`. `USAGE_MEDIA`/`CONTENT_TYPE_SPEECH` lässt die Lautstärke regelbar (`USAGE_VOICE_COMMUNICATION` geht auf `STREAM_VOICE_CALL`). Wiedergabe-Ende des Players ist das „fertig gesprochen"-Signal. [extern] https://docs.smallest.ai/atoms/atoms-platform/integrate/mobile-integrations/android-kotlin.md
- **Abweichung in der Praxis (Jarvis 1.8.100/1.8.105):** `MODE_IN_COMMUNICATION` + `USAGE_VOICE_COMMUNICATION` (+ Bluetooth-SCO) klingt nach Telefon. Jarvis nutzt standardmäßig den **Medienweg**: Ausgabe `USAGE_MEDIA`, kein Telefonmodus, Bluetooth als A2DP; Aufnahme weiter `VOICE_COMMUNICATION` (wegen Geräte-AEC), fest am Handy-Mikrofon, plus Echosperre. Telefonmodus nur als Rückfallweg. Details §14.2 Nr. 1.
- **Audio-Fokus:** `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` mit `USAGE_VOICE_COMMUNICATION`; bei `LOSS`/`LOSS_TRANSIENT` Gespräch beenden bzw. pausieren. [extern]
- **Hintergrund:** Foreground-Service mit `android:foregroundServiceType="microphone"`, Permission `FOREGROUND_SERVICE_MICROPHONE`, `RECORD_AUDIO` gewährt, `startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)`. **Nur aus sichtbarer Activity** (oder definierter Ausnahme: Benachrichtigung, Widget, PendingIntent aus sichtbarer App) starten — ab Android 14 sonst `SecurityException`. Reconnect aus dem Hintergrund darf den Service nicht neu starten, sondern im laufenden Service bleiben [Schluss]. `phoneCall`-Typ nur mit echter Telecom-Anbindung. [offiziell] https://developer.android.com/develop/background-work/services/fgs/service-types · https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- **Echo in der Begrüßung (Realtime):** erste Antwort nicht unterbrechbar (`interrupt_response:false`), VAD-Schwelle anheben (Nutzerbericht: threshold 0.85, prefix 300 ms, silence 500 ms), danach zurücksetzen; Kopfhörer-Hinweis. [Community] https://community.openai.com/t/realtime-api-echo-issue-when-on-speaker-phone/1370092
- **Parser:** Live-`start_ms`/`end_ms` optional parsen; unbekannte Event-Typen loggen. [offiziell]

## 12. Sicherheit: API-Schlüssel in einer Client-App

- OpenAI: „Ensure that you only use standard OpenAI API keys on the server, not in the browser." Für verteilte Apps: Ephemeral-Key per `POST /v1/realtime/client_secrets` (kurz vor dem Verbindungsaufbau, Standard 600 s) über ein Minimal-Backend, oder Server-Relay. Der `OpenAI-Safety-Identifier` wird beim Erzeugen des Secrets gebunden. [offiziell] https://developers.openai.com/api/docs/guides/realtime-webrtc
- Für Live ist kein Ephemeral-Key-Weg für WebSocket dokumentiert; WebRTC über `POST /v1/live/sessions` vom Server. [offiziell] https://developers.openai.com/api/docs/guides/voice-websockets
- Private Eigen-App mit eigenem Schlüssel (nicht verteilt): direkter WebSocket mit Standard-Key ist machbar (Pipecat nennt das „primarily for development and testing"); Schlüssel dann verschlüsselt speichern (Android Keystore / EncryptedSharedPreferences), nie in Git oder `BuildConfig`, eigenes OpenAI-Projekt mit Hard-Limit (§9). [extern + Schluss] https://docs.pipecat.ai/api-reference/client/android/transports/openai-webrtc.md
- MCP-Zugangsdaten in `delegation.responses.tools` landen bei OpenAI; nie `authorization` und `headers.Authorization` gleichzeitig setzen; `allowed_tools` eng halten, Schreibaktionen `require_approval:"always"`. [offiziell] https://developers.openai.com/api/docs/guides/realtime-mcp

## 13. Optionskataloge in Apps (Einstellungen)

- Stimmen: Live-Referenz listet 31 (alloy, ash, ballad, beacon, bossa, brise, cedar, cinder, coral, delta, echo, flitz, gleam, harema, juni, marin, meridian, nira, noeul, nuri, quartz, ripple, sage, shida, shimmer, sillage, stone, tempo, verse, vesper, willow), der Live-Guide beschreibt 12 mit Akzent (quartz en-AU w, ripple en-AU m, vesper en-GB m, willow en-IE w, stone en-IE m, gleam en-US w, meridian en-US m, bossa pt-BR w, tempo pt-BR m, beacon en-PH m, delta en-US-Süd w, cinder en-US-Süd m), das Python-SDK-Literal 23. Realtime: alloy, ash, ballad, coral, echo, sage, shimmer, verse, marin, cedar (marin/cedar empfohlen, ohne Sprachzuordnung). Kein Preisunterschied.
- **Sprachstimmen (Changelog 16.10.2026, Live-Guide mit Spalte Sprache, Stand 28.10.2026):** Deutsch `nuri` (w), `juni` (w), `flitz` (m); Französisch `brise` (w), `sillage` (m); Hindi `nira` (w); Japanisch `harema` (w), `shida` (m); Koreanisch `noeul` (w). Kein `language`-/Locale-Feld: „The only voice parameter is `audio.output.voice`“; Zielsprache zusätzlich in `session.instructions` nennen. Gleichzeitig kam Prompt-to-Voice (eigene Stimme aus Textbeschreibung, `CustomVoice`). [offiziell] https://developers.openai.com/api/docs/guides/live-conversations · https://developers.openai.com/api/docs/changelog
- **ChatGPT-App ≠ API:** Die App hat mit GPT-Live-1.1 (ab 15.10.2026) eigene Sprachstimmen, Deutsch „Sonna“ und „Miko“ (Einstellungen › Voice › German), dazu u. a. Aube/Rivage (fr), Himari/Rindō (ja). Diese Namen gibt es in der API nicht; nicht als voice-ID schicken. Ob sie klanglich `nuri`/`juni`/`flitz` entsprechen, ist unbelegt. [extern] all-ai.de · notebookcheck.net · skill-sprinters.de
- In der App (Jarvis 1.8.385) die deutschen Stimmen oben listen und Sprache im Titel nennen; Standard `marin` nicht still umstellen (gespeicherte Auswahl bleibt gültig). [Schluss] [offiziell] https://developers.openai.com/api/reference/resources/live/primary-websocket · https://developers.openai.com/api/docs/guides/live-conversations · openai-python `types/live/built_in_voice.py`
- Stimme, Effort (`max` fehlt im SDK), Backend-Modell und Service-Tier daher **konfigurierbar** halten (freier String), beim Start die aufgelöste Konfig aus `session.started` übernehmen und Fehler (`unknown_parameter`, `responses_validation_failed`) als „Option nicht verfügbar" anzeigen. Preise in der App mit Stand-Datum anzeigen. [Schluss aus offiziellen Abweichungen]
- Realtime-`audio.output.speed` 0,25–1,5 (nur zwischen Antworten änderbar; schnelleres Sprechen klingt per Prompt natürlicher). [offiziell] openai-python `realtime_audio_config_output.py`

## 14. Flüssige Wiedergabe in der Praxis (Jarvis 1.8.100–1.8.108, Android, GPT-Live über WebSocket)

> Stand 16.10.2026, 19:23. Quellen: eigener Code `Jarvis/app/src/main/java/de/frank/jarvis/live/LiveGespraech.kt`,
> `live/LiveRouting.kt`, `speech/GespraechsFokus.kt`; Commits `dc0e26d3e` (1.8.100) bis `50289b26b` (1.8.108);
> Versionslog `Jarvis/app/src/main/assets/versionslog.json`; Logcat-Mitschnitt von Franks Live-Test am 16.10.2026,
> 19:16:27–19:18:45 Uhr (Rohbeleg: `bugs/bug-cases/2026-10-16-jarvis-gpt-live-wiedergabe.md`);
> `dumpsys media.audio_flinger` vom 16.10.2026, ca. 19:22 Uhr.
>
> Markierungen in diesem Abschnitt: **[Messung]** = in Logcat/dumpsys dieses Tests belegt · **[Code-Kommentar]** =
> Messwert aus einer früheren Sitzung, nur im Quelltext-Kommentar erhalten, Rohprotokoll fehlt · **[Code]** = so
> umgesetzt, Wirkung nicht einzeln gemessen · **[Vermutung]** · **[offen]**.
>
> **Geltungsbereich der Messung:** nur Galaxy Z Fold 8 (SM-F971B), Handy-Lautsprecher, Medienweg mit Echosperre,
> GPT-Live mit Client-Delegation (Jarvis denkt selbst), ein Gespräch von gut 2 Minuten, Handy im Heim-WLAN.
> **Nicht gemessen:** Bluetooth/A2DP, Freisprechen (Telefonmodus), Realtime-Pfad, Mobilfunk.
>
> **Subjektives Feedback (Frank, Live-Test mit 1.8.108):** „deutlich besser als vorher; Klang, Sprachfluss und Aussprache
> sehr gut". Positiver Eindruck, **kein Beleg für Fehlerfreiheit**.

### 14.1 Grundbefund: So liefert GPT-Live das Audio

- GPT-Live sendet `session.output_audio.delta` **durchgehend**, auch in Sprechpausen, dann als (fast) digitale Stille.
  Beleg: „21 Stücke, 2100 ms Ton, davon 0 hörbar" in einem 2-s-Fenster ohne Rede [Messung]. Folge: Ob Jarvis spricht,
  ist nur am Pegel des Tons erkennbar, nie am Eintreffen von Daten. Es gibt kein „Antwort fertig"-Ereignis (§6).
- Die Stücke sind 100 ms lang und kommen im Mittel genau in Echtzeit (20 Stücke je 2 s) [Messung]. Der
  Wiedergabepuffer wächst deshalb **nicht von selbst**; jede Verspätung eines Stücks verbraucht Reserve.
- Ausnahme am Antwortanfang: Beim Begrüßungssatz lief der Puffer bis an seine Grenze von 1000 ms (Fenster 19:16:34:
  „Puffer 380–1000 ms, längstes Schreiben 360 ms") [Messung]. Das „längste Schreiben" ist hier `write()`, das am vollen
  Puffer wartet, kein CPU-Mangel. Deutung: Der Dienst liefert am Anfang zeitweise schneller als Echtzeit [Vermutung].
  Deshalb ist das Kappen überschüssiger Stille nötig (§14.2 Nr. 8).
- Empfangsschwankung (größte Lücke zwischen zwei Stücken je 2-s-Fenster, Sollwert 100 ms): Median 148 ms, Maximum
  386 ms; 20 von 65 Fenstern über 200 ms, 7 über 300 ms, davon nur eines während der Rede (312 ms) [Messung].
  Frühere Sitzung mit 1.8.107 im WLAN: Stücke „bis gut 200 ms zu spät" [Code-Kommentar].
- Realtime (`gpt-realtime-*`) liefert dagegen schubweise schneller als Echtzeit und grenzt Antworten selbst ab. Polster
  und Kappen sind dort abgeschaltet, der Puffer bleibt klein (`minOut * 4`), damit das Kürzen beim Dazwischenreden
  stimmt [Code].

### 14.2 Ursachen der bisherigen Abhacker und Verzögerungen und was dagegen umgesetzt ist

| Nr. | Version | Symptom | Ursache | Umsetzung | Beleglage |
|---|---|---|---|---|---|
| 1 | 1.8.100, 1.8.105 | Live klang nach Telefon (Auto und Handy-Lautsprecher) | Freisprechweg: `MODE_IN_COMMUNICATION` + `USAGE_VOICE_COMMUNICATION`, im Auto Bluetooth-SCO (schmalbandiges Telefonprofil) | **Medienweg** als Standard: Ausgabe `USAGE_MEDIA` + `CONTENT_TYPE_SPEECH`, Audiomodus unangetastet, Bluetooth als A2DP; Aufnahme weiter `VOICE_COMMUNICATION` (nur diese Quelle bekommt die Geräte-AEC), fest am Handy-Mikrofon (`setPreferredDevice(TYPE_BUILTIN_MIC)`), dazu Echosperre. Freisprechen nur als Rückfallweg, wenn gewünscht **und** ein Gerät mit eigenem Mikrofon verbunden ist (`LiveRouting.medienweg`) | [Code]; im Test „Tonweg Medien … Ton: Lautsprecher · Mikro: Handy-Mikrofon", Geräte-AEC „verfügbar=true, aktiv=true" [Messung] |
| 2 | 1.8.101 | Bluetooth-Lautsprecher verschluckt den Satzanfang | Läuft der AudioTrack leer, geht der Ausgang in Bereitschaft (Standby-Verzögerung 2 s: `Standby delay ns=2000000000` [Messung]); der A2DP-Strom wird ausgesetzt, beim Wiederanlauf fehlt der Anfang [Vermutung, am Bluetooth-Gerät nicht gemessen] | Im Medienweg in Pausen Stille nachschieben (20-ms-Blöcke, `STILLE_MS`). Füll-Stille zählt weder als Ton (Echosperre) noch als gehört (Kürzen) | [Code] |
| 3 | 1.8.103, 1.8.106 | Anzeige hing auf „Jarvis spricht", Echosperre hielt zu lange bzw. öffnete zu früh | Sperre mit fester Wartezeit bzw. nach eintreffenden Daten; GPT-Live liefert aber auch Stille (§14.1) | Hörbar-Erkennung per Effektivwert (`HOERBAR_EFFEKTIV`); Sperre an die Wiedergabeposition gekoppelt (`playbackHeadPosition` gegen `echtBisFrame`, `kopfBasis` nach `flush()`), danach Nachlauf `ECHO_NACHLAUF_MS`, harter Deckel `ECHO_SPERRE_MAX_MS` nach dem letzten hörbaren Ton. Sprechanzeige zurück, wenn kein hörbarer Ton mehr offen ist und 300 ms nichts Hörbares kam, spätestens nach `STATUS_MAX_MS`. Sperre und Anzeige entscheidet der Mikrofon-Faden, der nie an der Wiedergabe blockiert | Echosperre öffnete 591–1225 ms nach dem letzten geschriebenen hörbaren Stück; kein „Wiedergabe kommt nicht voran" [Messung] |
| 4 | 1.8.106 | Späte Mitschrift löschte die gerade beginnende Antwort | GPT-Live meldet kein „Nutzer spricht"; `session.input_transcript.delta` diente als Barge-in-Signal. Mit Echosperre bekommt der Dienst während der Rede nur Stille, eine Mitschrift ist dann die verspätete des vorigen Satzes | Mit Echosperre Ausgabe nicht leeren; Unterbrechen per Antippen der Blase (1.8.103), danach Rest der alten Antwort verwerfen (1,5 s ohne hörbaren Ton, höchstens `VERWERFEN_MAX_MS`) | Einmal im Test: „Mitschrift kommt, während Jarvis spricht: Echosperre an, Ausgabe bleibt" [Messung] |
| 5 | 1.8.107 | Stillelücken mitten in der Rede | Die Füll-Stille aus 1.8.101 wurde schon geschrieben, wenn 20 ms kein neues Stück kam, obwohl noch genug Ton im Puffer lag; ein leicht verspätetes Stück bekam Stille vorgesetzt | Füll-Stille nur nach Füllstand (`gepuffertFrames()` unter der Füllgrenze), nicht nach Zeit; Prüftakt `WARTE_MS` | [Code]; Test: Füll-Stille beim Sprechen insgesamt 60 ms in 52 Fenstern [Messung] |
| 6 | 1.8.108 | Abhacker im ersten Wort (etwa das „K" von „Klar") | Das Startpolster (1.8.107: 150 ms) kam direkt vor das erste *hörbare* Stück. Lag der leise Wortansatz noch im Stück davor (unter der Hörbar-Schwelle), riss die Polster-Stille das Wort auf [Code-Kommentar]. Außerdem reichten 150 ms nicht: 40–80 ms Füll-Stille im Satz [Code-Kommentar] | Polster 250 ms (`START_POLSTER_MS`), nur **hinter Stille** (`vorherStill`; „ganz still" = Spitze unter `STILLE_SPITZE`, etwa −44 dBFS) und nur nach mindestens `NEUER_SATZ_MS` ohne Ton; nachgefüllt, wenn der Puffer unter Polster minus einem Mischtakt liegt | Im Test vor jedem Satzanfang 220–470 ms im Puffer [Messung] |
| 7 | 1.8.108 | Strom flog aus dem Mischer | Der Lautsprecher-Mischer holt je Takt 40 ms; mit 40 ms Füllgrenze bekam er in Pausen teils weniger als einen Takt [Code-Kommentar]. Ein leer gestarteter Strom wurde nach einer Sekunde wegen Unterlauf abgeschaltet: „BUFFER TIMEOUT … underrun", danach „restartIfDisabled" [Code-Kommentar] | Füllgrenze in Pausen 80 ms (`FUELL_UNTER_MS`, zwei Takte), beim Sprechen 45 ms (`FUELL_SPRACHE_MS`, knapp über einem Takt); 250 ms Stille **vor** `play()` vorlegen | Lautsprecher-Ausgabethread mit 1920 Frames bei 48 kHz = 40 ms vorhanden [Messung]; ob genau dieser Thread den Live-Ton mischt, wurde nicht erneut zugeordnet. Start-Unterlauf **nicht vollständig behoben**, siehe §14.5 |
| 8 | 1.8.108 | Unnötige Verzögerung nach einem Empfangsschub | Ein Schub füllt den Puffer, die Reserve bleibt bis zur nächsten Antwort als Verzögerung stehen | In Pausen ab `KAPPEN_PAUSE_MS` ganz stille Stücke kappen, solange mehr als Polster plus `KAPPEN_UEBER_MS` im Puffer liegt | Gekappt bis 860 ms je Fenster, Satzanfang danach 220–470 ms [Messung] |
| 9 | 1.8.107, 1.8.108 | Risiko: Hintergrundarbeit (Recherchen, Synchronisation, Hinweise) stört Ton und Erkennung (Franks Regel 7) | — | Ton- und Mikrofon-Faden `THREAD_PRIORITY_URGENT_AUDIO`, OkHttp-Lesefaden `THREAD_PRIORITY_AUDIO`; Hinweise von Ablage, Dienst und Programmieraufträgen im Gespräch stumm (`setSilent(GespraechsFokus.stilleHinweise)`; bei Programmieraufträgen nur Zwischenschritte, Rückfrage/Fertig/Problem bleiben hörbar); `LiveGespraech.aktiv` zählt offene Gespräche | [Code]. **Dass Hintergrundlast die Wiedergabe je gestört hat, wurde nie gemessen**; die Prioritäten sind Vorsorge. Im Test „längstes Schreiben" 0–1 ms außer am vollen Puffer [Messung] |
| 10 | 1.8.108 | Realtime: Modell glaubt, Frank habe Ungehörtes gehört | `audio_end_ms` aus geschriebenen statt gehörten Bytes | `gehoertMs()` = geschrieben minus noch gepuffert; kleiner Puffer nur bei Realtime | [Code], nicht gemessen |

### 14.3 Bewährte Einstellungen (Stand 1.8.108)

Alle Werte in `Jarvis/app/src/main/java/de/frank/jarvis/live/LiveGespraech.kt` (Konstanten im `companion object` ab Zeile 1036). Werte nur mit Messung vorher und nachher ändern und den Messwert im Kommentar an der Konstante festhalten.

| Konstante / Einstellung | Wert | Zweck | Herkunft |
|---|---|---|---|
| Format | PCM16 mono 24 kHz, Senden in 100-ms-Blöcken (4.800 Byte) | OpenAI-Vorgabe (§11) | [offiziell] |
| `AudioRecord` | `VOICE_COMMUNICATION`, Puffer `max(minIn, 9600) * 2`, Geräte-AEC und `NoiseSuppressor` an | Echo-Unterdrückung | [Code] |
| `AudioTrack` (Medienweg) | `USAGE_MEDIA` + `CONTENT_TYPE_SPEECH`, `MODE_STREAM` | Medienqualität statt Telefon | [Code] |
| `AudioTrack`-Puffer | GPT-Live `max(minOut * 4, 1 s)`, Realtime `minOut * 4` (Zeile 661) | Platz für Polster und Schübe, ohne dass `write()` blockiert; die Verzögerung bestimmt der Füllstand, nicht die Größe | Test: Mindestpuffer 120 ms, Puffer 1000 ms [Messung] |
| Vorlauf vor `play()` | 250 ms Stille (Zeile 667) | Kein leerer Start | [Code-Kommentar] |
| `START_POLSTER_MS` | 250 | Reserve gegen Empfangslücken | 150 ms reichten nicht [Code-Kommentar] |
| `NEUER_SATZ_MS` | 300 | ab so viel Stille gilt Ton als Satzanfang | [Code] |
| `MISCHTAKT_MS` | 40 | Fold-8-Lautsprechertakt (1920 Frames @ 48 kHz) | [Messung, dumpsys] |
| `FUELL_UNTER_MS` | 80 | Füll-Stille in Pausen erst unter zwei Takten | 40 ms zu knapp [Code-Kommentar] |
| `FUELL_SPRACHE_MS` | 45 | Füll-Stille beim Sprechen erst knapp über einem Takt | [Code] |
| `STILLE_MS` | 20 | Länge eines Füll-Stilleblocks | [Code] |
| `WARTE_MS` | 10 | Prüftakt des Ton-Fadens im Medienweg | [Code] |
| `KAPPEN_PAUSE_MS` / `KAPPEN_UEBER_MS` | 600 / 30 | Überschuss-Stille erst in längeren Pausen kappen | [Code] |
| `STILLE_SPITZE` | 200 (etwa −44 dBFS) | „ganz still" = darf gekappt werden, Polster darf dahinter | [Code] |
| `HOERBAR_EFFEKTIV` | 120 (Effektivwert PCM16) | hörbarer Ton vs. Stille | [Code] |
| `EchoSperre.NACHLAUF_MS` (bis 1.8.193 `ECHO_NACHLAUF_MS`) | 400 | Mikrofon bleibt nach dem Kopf noch zu (Puffer, Hall) | [Code] |
| Bluetooth (ab 1.8.194, `live/EchoSperre.kt`) | + Ausgabelatenz (`getTimestamp`, mind. `BLUETOOTH_MIN_LATENZ_MS` 1000) + `BLOCK_MS` 100 | Kopf ≠ Lautsprecher: Funkstrecke und Autoradio-Puffer; Mikrofonblock ist bis 100 ms älter als die Entscheidung | [abgeleitet aus Phantomlänge, nicht gemessen], siehe `bugs/bug-cases/2026-10-20-jarvis-gemini-live-phantom-mitschrift.md` |
| `EchoSperre.SPERRE_MAX_MS` | 3000 (Bluetooth + Latenz) | spätestens dann Mikrofon offen | [Code] |
| `STATUS_MAX_MS` | 4000 | spätestens dann „Ich höre zu" | [Code] |
| `VERWERFEN_MAX_MS` | 10000 | Rest einer abgebrochenen Antwort höchstens so lange verwerfen | [Code] |
| OkHttp | `readTimeout(0)`, `pingInterval(20 s)` | Dauerverbindung | [Code] |
| Fäden | `jarvis-live-ton` und `jarvis-live-mikro` `URGENT_AUDIO`, Socket-Leser `AUDIO` | Vorrang vor Hintergrundarbeit | [Code] |

Bauregeln, die sich daraus ergeben:
- Füll-Stille nur nach Füllstand schreiben, nie nach Zeit und nie, solange noch genug Ton im Puffer liegt.
- Ein Polster nie direkt vor ein Stück legen, das hinter Ton folgt; nur hinter Stille.
- Den Sprechzustand von GPT-Live nie aus dem Datenfluss ableiten, nur aus hörbarem Pegel und der Wiedergabeposition.
- Nichts Blockierendes (auch kein Log) zwischen Füllstandsmessung und `write()`; protokolliert wird danach.
- Echosperre und Sprechanzeige im Mikrofon-Faden entscheiden, nicht im Ton-Faden (der kann in `write()` hängen).
- Neue Benachrichtigungen, die während eines Gesprächs feuern können, mit `setSilent(GespraechsFokus.stilleHinweise)`.

### 14.4 Messergebnis des Live-Tests vom 16.10.2026 (1.8.108) [Messung]

- Dauer: 19:16:27 („Verbinde …") bis 19:18:45, 14 Sprechphasen („Jarvis spricht") mit 16 Satzanfängen, eine
  Client-Delegation.
- Unterläufe laut `AudioTrack.underrunCount`: **0** in allen 52 Messfenstern.
- Füll-Stille während der Rede: **60 ms insgesamt**, in einem einzigen Fenster (19:16:46), zeitgleich mit einer
  Empfangslücke von 312 ms mitten in der Rede, bei nur 60 ms Puffer vor dem Satzanfang.
- Puffer vor einem Satzanfang: 220–470 ms bei 14 Satzanfängen; zweimal lag er darunter (60 und 180 ms) und das Polster
  füllte direkt davor auf 250 ms auf. Der Puffer am Satzanfang ist zugleich die zusätzliche Verzögerung bis zum Hören.
- Echosperre öffnete 591–1225 ms nach dem letzten geschriebenen hörbaren Stück (1225 ms beim Begrüßungssatz mit vollem
  Puffer, sonst 591–928 ms).
- „Längstes Schreiben" 0–1 ms, außer 94 ms und 360 ms in den ersten Fenstern am vollen Puffer.
- Delegation (Jarvis-Agent, Client-Delegation): „Jarvis schaut nach" 19:18:17,2 → `session.commentary.appended`
  19:18:30,7, also gut 13 s Antwortzeit des Backends. Das ist Backend-Latenz, kein Audioproblem; Hebel dafür in §5.1.

### 14.5 Offene Punkte und Vermutungen

- **[offen]** Beim Start erscheint trotz Vorlauf zweimal `restartIfDisabled … disabled due to previous underrun,
  restarting` (19:16:31,4 und 19:16:32,5), vor dem ersten hörbaren Wort; `underrunCount` blieb 0. Die
  Versionslog-Aussage von 1.8.108 „der Ton startet ohne Unterlauf" ist damit **nicht belegt**. Ob man es hört, ist
  unbekannt (Frank hat nichts bemängelt). Spur [Vermutung]: Im ersten Fenster wurden 670 ms Stille gekappt.
- **[offen]** `AudioFlinger: moveEffectChain_ll: effect chain for session 0 not on source thread` beim Start und bei
  Routing-Meldungen. Bedeutung unklar, keine hörbare Wirkung bekannt.
- **[offen]** Eine Empfangslücke von über 300 ms mitten in der Rede reicht weiterhin für kurze Füll-Stille (60 ms).
  Ein größeres Polster würde jede Antwort weiter verzögern; Abwägung Flüssigkeit gegen Reaktionszeit ist nicht gemessen.
- **[Befund 20.10.2026]** Bluetooth/A2DP im Auto (Gemini 3.8 Live, Echosperre an): Die Sperre öffnete nach Kopf + 400 ms, das
  hörbare Ende kam rund 0,9–1,2 s nach dem Kopf; die letzten Wörter jeder Antwort kamen als Franks Mitschrift zurück. Die
  Geräte-AEC hat für Ton über Bluetooth keine Referenz. Ab 1.8.194 wartet die Sperre bei Bluetooth die Ausgabelatenz ab
  (Bug-Fall `bugs/bug-cases/2026-10-20-jarvis-gemini-live-phantom-mitschrift.md`). Messung im Auto steht aus.
- **[offen]** Bluetooth/A2DP, Freisprechen, Realtime und Mobilfunk sind ohne Messprotokoll; Franks positiver Eindruck
  gilt nur für den getesteten Weg (Handy-Lautsprecher, Medienweg).
- **[offen]** Ob Hintergrundlast (Recherche, Synchronisation) die Wiedergabe je gestört hat, ist nicht gemessen. Zum
  Prüfen: während einer laufenden Jarvis-Recherche ein Live-Gespräch führen und die Werte aus §14.6 vergleichen.
- Log-Eigenheit, kein Fehler: Der erste „Satzanfang … Stille davor 1792170992339 ms" kommt vom Startwert 0 der Zeitmarke.

### 14.6 Rückfall-Prüfung (nach jeder Änderung an `live/`, Pflicht vor dem Commit)

1. Installieren, Logcat leeren (`adb -s <IP>:5555 logcat -c`), dann ein Live-Gespräch von mindestens 2 Minuten mit
   mehreren Antworten und einer Delegation führen. Für den Vorrang-Test zusätzlich eines während einer Recherche.
2. Logcat sichern:
   ```powershell
   adb -s <IP>:5555 logcat -d -v time -s JarvisLiveRouting:* AudioFlinger:W AudioTrack:W > live-logcat.txt
   ```
3. Auswerten:
   ```powershell
   $w = Select-String -Path live-logcat.txt 'Wiedergabe in'
   "Unterläufe: " + ($w | % { [int]([regex]::Match($_.Line,'Unterläufe \+(\d+)').Groups[1].Value) } | Measure -Sum).Sum
   "Füll-Stille beim Sprechen: " + ($w | % { [int]([regex]::Match($_.Line,'beim Sprechen (\d+) ms').Groups[1].Value) } | Measure -Sum).Sum + " ms"
   $g = Select-String -Path live-logcat.txt 'GPT-Live-Ausgabe in' | % { [int]([regex]::Match($_.Line,'größte Lücke (\d+) ms').Groups[1].Value) }
   "Empfangslücke max / Median: " + ($g | Measure -Max).Maximum + " / " + ($g | Sort)[[int]($g.Count/2)] + " ms"
   "restartIfDisabled: " + (Select-String -Path live-logcat.txt 'restartIfDisabled').Count
   "Wiedergabe hängt: " + (Select-String -Path live-logcat.txt 'Wiedergabe kommt nicht voran').Count
   Select-String -Path live-logcat.txt 'Satzanfang|Echosperre auf|Gesprächsbeginn|Tatsächlich' | % { $_.Line }
   ```
4. Grenzwerte (Referenz = Test 16.10.2026 mit 1.8.108):

   | Prüfwert | Soll | Referenz 1.8.108 |
   |---|---|---|
   | Unterläufe (Summe) | 0 | 0 |
   | Füll-Stille beim Sprechen | 0; einzelne ≤ 60 ms nur bei gleichzeitiger Empfangslücke über dem Polster | 60 ms (Lücke 312 ms) |
   | „Wiedergabe kommt nicht voran" | 0 | 0 |
   | Puffer am Satzanfang | etwa 200–500 ms; deutlich mehr = unnötige Verzögerung, unter 100 ms = Polster greift nicht | 220–470 ms |
   | Echosperre auf nach letztem Ton | ≤ ca. 1300 ms | 591–1225 ms |
   | `restartIfDisabled` | notieren; mehr als 2 beim Start oder welche mitten im Gespräch = Rückfall | 2 beim Start |
   | `Gesprächsbeginn` / `Tatsächlich` | erwarteter Tonweg, Ausgabe und Mikrofon | Medien · Lautsprecher · Handy-Mikrofon |

5. Hörprobe durch Frank (Satzanfänge vollständig, kein Stottern, kein Telefonklang). Ergänzend, ersetzt die Zahlen nicht.

## 🔗 Bezug zum Bug-Almanach

| Best-Practice | Bug-Abschnitt (`bugs/apis/openai-live-realtime.md`) |
|---|---|
| §2 Modell-IDs | E3, E4, E5, C1 |
| §3 Verbindung | C1–C5, A14 |
| §4 Sitzungskonfiguration | A1, A6, A12, A13, C2, C6, E1 |
| §5 Delegation | B1–B10, E2 |
| §6 Events/Ende | A3–A5, A7, A8, A15, A16, C7 |
| §7 Barge-in | A3, F1, F3, F4, D9 |
| §8/§9 Kosten | G1–G8 |
| §10 Prompting | A9, A10, D4 |
| §11 Android | F1–F12 |
| §12 Sicherheit | D6 |
| §13 Optionskataloge | A13, E1, E2 |
| §14 Praxis Jarvis (Wiedergabe) | F13–F19 |
