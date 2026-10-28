# Bekannte Bugs: OpenAI Live- und Realtime-Sprach-API (GPT-Live, gpt-realtime-2.1, Android-Client)

> PFLICHT-LESEN vor Arbeit an einer Sprachgespräch-Anbindung an OpenAI (`v1/live/sessions`, `v1/realtime`),
> an Delegation (Backend-Modell hinter GPT-Live) oder an Android-Audio (AudioRecord/AudioTrack/AEC) dafür.
> Stand: 2026-10-16 (recherchiert 16.10.2026, 12:15; research-Skill, 9 parallele Researcher).
> **Versions-Anker:** OpenAI API Stand 16.10.2026, gpt-live-1 GA 10.09.2026, gpt-realtime-2.1 06.07.2026,
> openai-python Stand 16.10.2026.
> **Anker:** openai-api=2026-10-16
> Ergänzt 28.10.2026, 15:35: A13 um die 9 Sprachstimmen und den SDK-Rückstand erweitert, A17 (ChatGPT-Stimmennamen als voice-ID).
> Zweite Seite (wie macht man es richtig): `best-practices/apis/openai-live-realtime.md`.
> Allgemeine OpenAI-Fehler (Responses/Chat, 429, Structured Outputs): `bugs/apis/openai-api.md`.
>
> Markierung: **[offiziell]** OpenAI-/Google-Doku, Changelog oder SDK-Quelltext · **[extern]** Drittanbieter-Doku ·
> **[Community]** Forum-/Nutzerbericht · **[unbestätigt]** nicht reproduziert oder widersprüchlich.
> Zu gpt-live-1 gibt es **keine offiziell bestätigten Bugs** (Changelog listet keine Fixes); die A/B-Einträge sind
> dokumentierte Fallen und Drittquellen-Eigenheiten.

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
| 16 | GPT-Live hackt (Wortanfang, Lücken im Satz) / „spricht" hängt | Live liefert Stille in Echtzeit: Sprechen nur per Pegel; Füll-Stille nur nach Füllstand; Polster nur hinter Stille in Pausen; Logcat-Prüfung Best Practice §14.6 | F13, F15, F16, F17 |
| 17 | Telefonklang / Hinweiston über Live-Stimme | Medienweg statt Telefonmodus; Hinweise im Gespräch `setSilent` | F18, F19 |
| 18 | Stimme „Miko“/„Sonna“ (ChatGPT) gewünscht / SDK meckert über `nuri` | App-Namen sind keine API-IDs: für Deutsch `nuri`/`juni`/`flitz` als freien String senden | A13, A17 |

---

## A) GPT-Live-Protokoll (`wss://api.openai.com/v1/live/sessions`)

### 1. `immutable_field_update` bei `session.update` ⭐
- **Symptom:** Fehler `immutable_field_update`, Änderung von Stimme, Modell, Instructions, Audioformat, Startverlauf oder Delegationstyp greift nicht; auch `session.update` mit `delegation: null` bei laufender Responses-Delegation scheitert.
- **Ursache:** Nach `session.start` sind `model`, `instructions`, `audio` (Format + Stimme), `input` und `delegation.type` unveränderlich; änderbar sind nur `delegation.responses`-Inhalte und `triggers`.
- **Versionen:** gpt-live-1 seit GA 10.09.2026 — per Design.
- **FIX:** Alles vor dem Start festlegen. Für neue Stimme/neues Modell: neue Sitzung, ggf. per Fork (`.../live/sessions/{id}/fork`, Overrides nur store, Responses-Delegation, audio.format) bzw. mit Textverlauf in `input`. Instructions ergänzen per `session.instructions.append` (≤ 500 Tokens).
- **Quelle:** https://developers.openai.com/api/docs/guides/live-conversations · https://developers.openai.com/api/docs/guides/live-delegation [offiziell]

### 2. Befehle vor `session.started` / falsche erste Nachricht
- **Symptom:** Fehler oder keine Reaktion direkt nach dem Verbindungsaufbau; `session.started` kommt nie.
- **Ursache:** `session.start` muss die erste Nachricht sein; Audio/Befehle erst nach `session.started`. Ein Startfehler verhindert `session.started`, ein späterer Befehlsfehler beendet die Sitzung nicht zwingend.
- **Versionen:** gpt-live-1.
- **FIX:** Zustandsmaschine: verbinden → `session.start` → auf `session.started` warten → erst dann `session.input_audio.append`. Bei WebRTC `session.start` gar nicht über den Data-Channel senden.
- **Quelle:** https://developers.openai.com/api/reference/resources/live/primary-websocket · https://developers.openai.com/api/docs/guides/voice-webrtc [offiziell]

### 3. Assistent spricht nach Unterbrechung weiter / Transkript zu vollständig ⭐
- **Symptom:** Nutzer unterbricht, App spielt die alte Antwort weiter; Transkript führt unterbrochene Antworten als vollständig.
- **Ursache:** GPT-Live hat kein `speech_started`, kein Cancel-/Truncate-Kommando und kein `output_audio.done`. Der Server hört nur auf zu senden (Lücken in `start_ms`/`end_ms`); bereits gepuffertes Audio spielt die App weiter.
- **Versionen:** gpt-live-1 — per Design; Transkript-Eigenheit laut Pydantic AI.
- **FIX:** Eigene Wiedergabe-Queue; Sprechanzeige und „fertig gesprochen" aus dem Player ableiten; wenn die App selbst abbricht, Queue leeren + `AudioTrack.flush()`. Unterbrochene Antworten im Chat markieren.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-migration [offiziell] · https://pydantic.dev/docs/ai/realtime/openai-live/ [extern]

### 4. Sitzung hängt / kostet nach Ende der Audioquelle weiter
- **Symptom:** Nach Ende des Mikrofonstreams oder Schließen des Sockets läuft die Sitzung (und die Minutenabrechnung) weiter bzw. die Nutzung bleibt unbestätigt.
- **Ursache:** EOF beendet eine Live-Sitzung nicht.
- **Versionen:** gpt-live-1.
- **FIX:** `session.close` senden und weiterlesen bis `session.closed` (bis 15 s warten), erst dann Socket schließen.
- **Quelle:** https://developers.openai.com/api/docs/guides/voice-latency-cost · https://developers.openai.com/api/docs/guides/realtime-websockets [offiziell]

### 5. `usage.seconds` aufsummiert → doppelte Kosten / Endwert fehlt
- **Symptom:** Berechnete Sprachminuten weit zu hoch; oder Endabrechnung fehlt.
- **Ursache:** `session.usage.updated` (ca. 1×/Min) ist **kumulativ**; Endwert steht in `session.closed`. Bei Verbindungsabbruch vor `session.closed` ist die Nutzung „unbestätigt". Die finale Summe kann verspätet eintreffen (Pydantic AI wartet bis 2 s).
- **Versionen:** gpt-live-1.
- **FIX:** Immer nur den letzten Wert übernehmen, Endwert aus `session.closed`; nach `session.close` kurz warten; Abbrüche als „unbestätigt" markieren.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-conversations [offiziell] · https://pydantic.dev/docs/ai/realtime/openai-live/ [extern]

### 6. `invalid_audio`
- **Symptom:** Fehler `invalid_audio` bei `session.input_audio.append`.
- **Ursache:** PCM16-Block mit ungerader Byteanzahl (halbes Sample) oder falsches Format (WAV-Header, Stereo, falsche Rate).
- **Versionen:** gpt-live-1.
- **FIX:** Roh-PCM16 little-endian mono, 24 kHz (oder 16 kHz laut Startkonfig), gerade Bytezahl; Restbyte in den nächsten Block übernehmen; gleichmäßig 100-ms-Blöcke (4.800 Byte bei 24 kHz).
- **Quelle:** https://developers.openai.com/api/docs/guides/voice-websockets · https://developers.openai.com/api/reference/resources/live/primary-websocket [offiziell]

### 7. Parser stürzt bei `session.output_audio.delta` ab
- **Symptom:** NullPointer/Parse-Fehler, weil `start_ms`/`end_ms` fehlen.
- **Ursache:** Referenz: Zeitfelder „bei Sideband erforderlich, auf primärer WebSocket ausgelassen"; Guide und Azure-Spiegel zeigen sie trotzdem.
- **Versionen:** gpt-live-1, Doku-Stand 16.10.2026 [unbestätigt, welche Variante gilt].
- **FIX:** Felder optional parsen (`?: null`), nie für die Wiedergabe-Logik voraussetzen.
- **Quelle:** https://developers.openai.com/api/reference/resources/live/primary-websocket [offiziell] · https://learn.microsoft.com/en-us/azure/foundry/openai/gpt-live-reference [offiziell, Azure]

### 8. Keine Turn-Grenzen / Turn endet zu früh
- **Symptom:** Chatverlauf zerfasert; Nutzer- und Assistenten-Fragmente überlagern sich; lange Denkpause beendet den Turn zu früh.
- **Ursache:** Transkript-Deltas ohne Done-Event, ohne Item-IDs, ohne Turn-Grenzen.
- **Versionen:** gpt-live-1.
- **FIX:** Fragmente in Lieferreihenfolge je Sprecher anhängen, Turn aus Stille ableiten (Pydantic AI: 2.000 ms, ggf. länger), Zeitstempel erhalten; im Prompt „Keep listening while the user pauses to think".
- **Quelle:** https://developers.openai.com/api/docs/guides/live-migration [offiziell] · https://pydantic.dev/docs/ai/realtime/openai-live/ [extern]

### 9. Stiller Kontext wird laut gesprochen
- **Symptom:** Text, der nur Kontext sein sollte (z. B. per `session.thinking.append` oder `input`), wird vorgelesen.
- **Ursache:** Text in der Sitzung gilt als Kontext, nicht als Nutzeräußerung, kann aber trotzdem gesprochen werden.
- **Versionen:** gpt-live-1 [extern].
- **FIX:** Kontexttexte als Fakten formulieren, im Voice-Prompt „Lies Hintergrundinformationen nicht vor"; Gesprochenes nur über `session.commentary.append`.
- **Quelle:** https://pydantic.dev/docs/ai/realtime/openai-live/ [extern]

### 10. Sprachebene delegiert nicht
- **Symptom:** GPT-Live beantwortet backend-abhängige Fragen selbst (rät), oder delegiert bei Verabschiedung/Tool-Abläufen nicht.
- **Ursache:** Modellverhalten; Community-Berichte.
- **Versionen:** gpt-live-1, Sep./Okt. 2026 [Community, unbestätigt].
- **FIX:** Klare Delegationsregeln im Voice-Prompt („Delegate before giving an answer that depends on backend work. Do not guess the result while waiting."), Beispiele für „delegiere bei X / nicht bei Y".
- **Quelle:** https://community.openai.com/t/introducing-gpt-live-1-in-the-api/1396471 [Community] · https://developers.openai.com/api/docs/guides/live-prompting [offiziell]

### 11. WebRTC-Data-Channel: Befehle bei Live nicht verfügbar
- **Symptom:** `session.update`, `response.cancel`, `input_audio_buffer.clear` über den WebRTC-Data-Channel funktionieren bei Live nicht; `info` mit Code `data_channel_permissions`.
- **Ursache:** Live hat diese Realtime-Befehle nicht; der Data-Channel ist über `session.client.data_channel.allowed_client_events`/`allowed_server_events` eingeschränkt.
- **Versionen:** gpt-live-1 [Community für die konkreten Befehle, unbestätigt].
- **FIX:** Live-Eventnamen nutzen; erlaubte Events serverseitig in `client.data_channel` freigeben.
- **Quelle:** https://community.openai.com/t/introducing-gpt-live-1-in-the-api/1396471 [Community] · https://developers.openai.com/api/reference/resources/live/primary-websocket [offiziell]

### 12. Antwort nicht auf Deutsch / Akzent schwankt
- **Symptom:** Englische Antworten oder uneinheitliche Akzente bei Nicht-Englisch.
- **Ursache:** Kein Sprachfeld; unterstützte Sprachen nirgends offiziell dokumentiert; Stimmen sind regional (en-AU, en-GB, pt-BR …).
- **Versionen:** gpt-live-1 [Community für Akzente].
- **FIX:** Instructions auf Deutsch mit „Sprich immer Deutsch", `developer`-Eröffnungsnachricht in `input` auf Deutsch; Stimme mit Deutsch testen und Auswahl daran ausrichten.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-prompting [offiziell] · https://community.openai.com/t/introducing-gpt-live-1-in-the-api/1396471 [Community]

### 13. Stimme abgelehnt oder anders als erwartet
- **Symptom:** Fehler beim Start mit einer Stimme aus der Doku, oder SDK-Typprüfung meckert.
- **Ursache:** Drei verschiedene Listen: Referenz 31 Stimmen, Live-Guide 12 (Standard `marin` fehlt dort), Python-SDK-Literal 23. Ob alle 31 in allen Konten verfügbar sind, ist ungeklärt.
- **Versionen:** Doku/SDK Stand 16.10.2026 [unbestätigt].
- **FIX:** Stimme als freien String übergeben (SDK-Typ ist `Union[str, …]`); beim Fehler auf `marin` zurückfallen und dem Nutzer melden; Liste nicht fest verdrahten.
- **Nachtrag 28.10.2026:** Am 16.10.2026 kamen 9 Sprachstimmen dazu (de `nuri`/`juni`/`flitz`, fr `brise`/`sillage`, hi `nira`, ja `harema`/`shida`, ko `noeul`). openai-python 3.40.0 kennt im Literal `BuiltInVoice` nur `shida` davon; Typprüfer bemängeln die übrigen, die API nimmt sie laut Guide an. Freien String verwenden. [offiziell] https://developers.openai.com/api/docs/changelog · https://github.com/openai/openai-python/blob/main/src/openai/types/live/built_in_voice.py
- **Quelle:** https://developers.openai.com/api/reference/resources/live/primary-websocket · https://developers.openai.com/api/docs/guides/live-conversations · https://github.com/openai/openai-python/tree/main/src/openai/types/live [offiziell]

### 17. ChatGPT-Stimmennamen als voice-ID (Miko, Sonna)
- **Symptom:** Gewünschte Stimme aus der ChatGPT-App („Sonna“, „Miko“ unter German) ist in der API nicht wählbar oder wird abgelehnt.
- **Ursache:** ChatGPT nutzt seit 15.10.2026 GPT-Live-1.1 mit eigenen Stimmennamen; die API hat nur `gpt-live-1` mit anderen IDs. GPT-Live-1.1 ist für die API nur angekündigt.
- **Versionen:** gpt-live-1, Stand 28.10.2026 [offiziell für API-Liste, extern für App-Namen].
- **FIX:** Für Deutsch `nuri`, `juni` oder `flitz` nehmen und dem Nutzer offen sagen, dass Miko/Sonna nur in ChatGPT laufen; nach API-Freigabe von 1.1 Liste neu prüfen.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-conversations [offiziell] · all-ai.de · notebookcheck.net [extern]

### 14. Modell in der URL / „WebSocket geht bei Live nicht"
- **Symptom:** Verbindungsfehler mit `.../v1/live/sessions?model=gpt-live-1`; Blogs behaupten, Live laufe nur über WebRTC.
- **Ursache:** Live-Endpunkt nimmt keine Query-Parameter, Modell gehört in `session.model`. Die WebRTC-only-Behauptung (OrcaRouter-Blog) widerspricht der offiziellen Doku.
- **Versionen:** gpt-live-1.
- **FIX:** URL ohne Query; Modell im `session.start`-Body. Drittblog ignorieren.
- **Quelle:** https://developers.openai.com/api/docs/guides/voice-websockets [offiziell] · https://www.orcarouter.ai/he/blog/gpt-live-1-api-release [extern, falsch]

### 15. Audio bricht mitten in der Antwort ab, Sitzung läuft weiter
- **Symptom:** Assistenten-Audio endet abrupt für den Rest der Äußerung.
- **Ursache:** Moderation schneidet Assistenten-Audio für den Rest der aktuellen Rede ab, ohne die Sitzung zu beenden (`session.closed.reason = "content"` nur bei Sitzungsende).
- **Versionen:** gpt-live-1.
- **FIX:** Kein Fehlerzustand annehmen; Nutzer weiter sprechen lassen; bei Häufung Prompt prüfen.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-conversations [offiziell]

### 16. Modell „vergisst" in langen Gesprächen
- **Symptom:** Nach langer Sitzung fehlen frühe Details.
- **Ursache:** Ab 90 % des 128k-Kontexts startet eine Ersatz-Voice-Engine mit den Original-Instructions und nur ≤ 8.192 Tokens Verlauf; Älteres wird verdichtet.
- **Versionen:** gpt-live-1.
- **FIX:** `context_window.usage_ratio` aus `session.usage.updated` beobachten; Wichtiges über `session.thinking.append` erneut einspeisen; Live-Verlauf nie als Buchungsbeleg nutzen.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-conversations · https://developers.openai.com/api/docs/guides/live-migration [offiziell]

## B) GPT-Live-Delegation (`responses` / `client`)

### 1. Funktionsaufruf wird übersehen — `output` ist leer ⭐
- **Symptom:** `response.completed`/`response.done` hat leeres `output`; App sieht keinen Tool-Call.
- **Ursache:** Live hat keine eigenen Tools; das Backend liefert verschachtelte Events (`response.event` → `event.type:"response.output_item.done"`, `item.type:"function_call"`).
- **Versionen:** gpt-live-1 (Sep./Okt. 2026).
- **FIX:** Calls aus den verschachtelten `output_item.done` lesen (`call_id`, `name`, `arguments`), nicht aus dem Abschluss-Event.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation [offiziell] · https://community.openai.com/t/introducing-gpt-live-1-in-the-api/1396471 [Community]

### 2. Backend bleibt nach Tool-Ergebnis stumm (`tool_outputs_required`) ⭐
- **Symptom:** Nach dem Senden des Ergebnisses passiert nichts; später Fehler `tool_outputs_required`.
- **Ursache:** „Appending an item alone does not resume the backend."
- **Versionen:** gpt-live-1.
- **FIX:** `response.item.create` mit `function_call_output` (`call_id`, `output`), danach **genau ein** `response.create`.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation [offiziell]

### 3. `function_call_outputs_required` bei parallelen Calls
- **Symptom:** Fehler, obwohl ein Ergebnis gesendet wurde.
- **Ursache:** Bei `parallel_tool_calls` braucht jede `call_id` ein eigenes Ergebnis.
- **Versionen:** gpt-live-1.
- **FIX:** Alle Calls sammeln, je `call_id` ein `function_call_output`, dann ein `response.create`; für die erste Migration `parallel_tool_calls:false`.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation · https://developers.openai.com/api/docs/guides/live-migration [offiziell]

### 4. `responses_validation_failed` kommt asynchron
- **Symptom:** `session.started` kommt, später Fehler `responses_validation_failed`; Backend arbeitet nicht.
- **Ursache:** Backend-Einstellungen (Modell, Effort, Tools) werden asynchron validiert; Sitzung bleibt offen.
- **Versionen:** gpt-live-1.
- **FIX:** Fehler abfangen und korrigiertes `session.update` mit vollständigem Delegationsobjekt senden (z. B. Effort senken, MCP-Eintrag entfernen).
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation [offiziell]

### 5. Veraltete Ergebnisse nach Barge-in / Delegation nicht abbrechbar
- **Symptom:** Nutzer zieht Wunsch zurück, Backend erledigt ihn trotzdem; altes Ergebnis wird später gesprochen.
- **Ursache:** Unterbrechen der Rede lässt Backend-Arbeit weiterlaufen; eine gestartete Delegation lässt sich nicht abbrechen; fertiges Backend belegt nicht, dass der Nutzer die Antwort gehört hat.
- **Versionen:** gpt-live-1.
- **FIX:** Operations-ID + Revisionsnummer je Aufgabe, veraltete Ergebnisse verwerfen; zustandsändernde Tools nur mit Bestätigung in der App; vor Retry Status prüfen.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation [offiziell] · https://pydantic.dev/docs/ai/realtime/openai-live/ [extern]

### 6. Client-Delegation: Auftragstext fehlt
- **Symptom:** `session.delegation.created` kommt, aber die App weiß nicht, was zu tun ist.
- **Ursache:** Event enthält nur `delegation.{id, type, target:"client"}` und `offset_ms`, keinen Text.
- **Versionen:** gpt-live-1.
- **FIX:** `session.input_transcript.delta`/`session.output_transcript.delta` laufend puffern und den Auftrag aus dem Transkript bis `offset_ms` ableiten.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation [offiziell] · https://learn.microsoft.com/en-us/azure/foundry/openai/how-to/gpt-live-delegation [offiziell, Azure]

### 7. `*.appended` als „gesprochen" missverstanden
- **Symptom:** App markiert Ergebnis als mitgeteilt, Nutzer hat es nie gehört.
- **Ursache:** `session.commentary.appended`/`session.thinking.appended` bestätigen nur die Annahme.
- **Versionen:** gpt-live-1.
- **FIX:** „Gehört" nur aus Output-Transkript/Player ableiten.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation [offiziell]

### 8. Append zu lang
- **Symptom:** Fehler oder abgeschnittene Antwort bei langen Ergebnissen über `session.commentary.append`/`thinking.append`/`instructions.append`.
- **Ursache:** `content` max. 500 Tokens je Append (genaues Verhalten bei Überschreitung unbelegt).
- **Versionen:** gpt-live-1.
- **FIX:** Ergebnisse kurz zusammenfassen; bei Bedarf mehrere Appends mit derselben `delegation_id`.
- **Quelle:** https://developers.openai.com/api/reference/resources/live/primary-websocket [offiziell]

### 9. MCP-Zugangsdaten nach `session.update` weg
- **Symptom:** MCP-Tool funktioniert nach Änderung der Backend-Tools nicht mehr (401/403 vom MCP-Server).
- **Ursache:** Live reicht `authorization`/`headers` an Responses durch, gibt sie in der Sitzungskonfiguration aber nicht zurück; wer `tools` ersetzt, verliert sie.
- **Versionen:** gpt-live-1.
- **FIX:** Bei jedem Ersetzen von `delegation.responses.tools` die Zugangsdaten erneut mitsenden.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation [offiziell]

### 10. MCP in `delegation.responses.tools` / `session.update`-Semantik widersprüchlich
- **Symptom:** `responses_validation_failed` mit MCP-Eintrag; oder Teil-Update löscht andere Backend-Felder.
- **Ursache:** Prosa-Doku nennt MCP und weitere Tools als unterstützt, die TS-/Python-Typen `ResponsesDelegationConfig.tools` kennen nur `function` + `web_search`. Guide: `session.update` „sparse"; Azure-Referenz: Delegation wird als ganzes Objekt ersetzt.
- **Versionen:** Doku/SDK Stand 16.10.2026 [unbestätigt].
- **FIX:** Am echten Endpunkt testen; bei Fehler ohne MCP starten; Updates immer mit vollständigem Delegationsobjekt.
- **Quelle:** https://developers.openai.com/api/docs/guides/live-delegation · https://developers.openai.com/api/reference/typescript/resources/live/index.md · https://learn.microsoft.com/en-us/azure/foundry/openai/gpt-live-reference

## C) Realtime-Protokoll und Beta→GA-Migration (`wss://api.openai.com/v1/realtime`)

### 1. Beta-Header und Preview-Modelle abgeschaltet
- **Symptom:** Verbindung/Konfig scheitert mit `OpenAI-Beta: realtime=v1` oder `gpt-4o-realtime-preview*`.
- **Ursache:** Beta-Schnittstelle am 12.05.2026, Preview-Modelle am 07.05.2026 abgeschaltet.
- **Versionen:** alle Clients mit Beta-Code.
- **FIX:** Header entfernen, GA-Schema, `gpt-realtime-2.1`.
- **Quelle:** https://developers.openai.com/api/docs/deprecations [offiziell]

### 2. `Invalid type for 'session.audio.input.format': expected an object`
- **Symptom:** Fehlertext wie genannt; fehlendes `rate` erzeugt einen eigenen Fehler.
- **Ursache:** Format als String (`"pcm16"`) statt Objekt.
- **Versionen:** Realtime GA (Forum 2025; Referenz bestätigt Objektformat).
- **FIX:** `{ "type": "audio/pcm", "rate": 24000 }`.
- **Quelle:** https://community.openai.com/t/realtime-api-beta-realtime-api-ga-receiving-type-error-with-session-audio-input-format/1355366 [Community] · https://developers.openai.com/api/reference/resources/realtime/client-events [offiziell]

### 3. Keine Audio-Deltas, keine Fehlermeldung
- **Symptom:** Verbindung steht, aber es kommt kein Ton/Text an.
- **Ursache:** Handler hören auf Beta-Namen `response.audio.delta`, `response.audio_transcript.delta`, `response.text.delta`; unbekannte Events werfen keinen Fehler.
- **Versionen:** GA seit gpt-realtime.
- **FIX:** `response.output_audio.delta`, `response.output_audio_transcript.delta`, `response.output_text.delta`; unbekannte Eventnamen loggen.
- **Quelle:** https://developers.openai.com/api/docs/guides/realtime [offiziell]

### 4. `Unknown parameter: 'session.audio'` / `'session.type'`
- **Symptom:** `invalid_request_error` bei GA-förmigem `session.update`.
- **Ursache:** Beta-Header noch gesetzt (Beta-Schema aktiv). Seit 12.05.2026 weitgehend gegenstandslos.
- **Versionen:** Forum 2025 [unbestätigt].
- **FIX:** `OpenAI-Beta`-Header entfernen.
- **Quelle:** https://community.openai.com/t/format-bug-for-websocket-session-update-client-event/1355450 [Community]

### 5. Fehler „session.type" fehlt
- **Symptom:** `session.update` abgelehnt.
- **Ursache:** GA verlangt `session.type: "realtime"`.
- **Versionen:** Realtime GA [extern, Wortlaut unbelegt].
- **FIX:** `"type": "realtime"` in `session` setzen.
- **Quelle:** https://dev.to/flarecanary/openai-realtime-beta-disappears-may-7-your-voice-agents-audio-handlers-will-stop-firing-with-no-1fn [extern]

### 6. Stimme fällt auf `alloy` / lässt sich nicht mehr ändern ⭐
- **Symptom:** Gesetzte Stimme wirkt nicht oder Wechsel mitten im Gespräch scheitert.
- **Ursache:** `voice` im Beta-Top-Level statt `session.audio.output.voice`; Stimme ist nach der ersten Audioausgabe gesperrt.
- **Versionen:** Realtime GA.
- **FIX:** `session.audio.output.voice` im ersten `session.update` vor der ersten Antwort setzen; Stimmwechsel = neue Sitzung.
- **Quelle:** https://developers.openai.com/api/docs/guides/realtime-conversations [offiziell] · https://dev.to/flarecanary/openai-realtime-beta-disappears-may-7-your-voice-agents-audio-handlers-will-stop-firing-with-no-1fn [extern]

### 7. Verbindung bricht nach 60 Minuten ohne Vorwarnung ab
- **Symptom:** Harte Trennung nach einer Stunde.
- **Ursache:** Hartes Sitzungslimit, OpenAI warnt nicht vorher.
- **Versionen:** alle Realtime-Modelle.
- **FIX:** Bei ca. 50 Minuten proaktiv neu verbinden, Kontext als Zusammenfassung nachladen.
- **Quelle:** https://developers.openai.com/api/docs/guides/realtime-conversations [offiziell]

### 8. „Conversation already has an active response"
- **Symptom:** Fehler bei `response.create` direkt nach `function_call_output`; Folge Stille.
- **Ursache:** Zweites `response.create`, während die vorige Antwort noch läuft (oder Client-Retry).
- **Versionen:** Realtime [Community].
- **FIX:** Pro Werkzeugergebnis genau ein `response.create`, erst nach `response.done` der laufenden Antwort; keine automatischen Retries von `response.create`.
- **Quelle:** https://community.openai.com/t/frequently-getting-conversation-already-has-an-active-response-from-realtime-api/1115740 · https://community.openai.com/t/realtime-api-server-response-error-message-conversation-already-has-an-active-response/1005582 [Community]

### 9. Verlauf als Textitems → nur noch Text-Antworten
- **Symptom:** Nach Einspielen früherer Runden per `conversation.item.create` antwortet das Modell nur noch mit Text.
- **Ursache:** ungeklärt.
- **Versionen:** Forum 2024/2025, aktuelle Gültigkeit ungeprüft [Community, unbestätigt].
- **FIX:** Verlauf als eine Zusammenfassung in `instructions`; bei GPT-Live `session.input` nutzen (dafür vorgesehen).
- **Quelle:** https://community.openai.com/t/realtime-api-did-anybody-managed-to-provide-previous-conversation-transcript-history-while-keeping-audio-answers/968293 [Community]

### 10. Rückwechsel `output_modalities` Text → Audio klappt nicht
- **Symptom:** Nach Umschalten auf nur Text bleiben alle späteren Antworten Text.
- **Ursache:** ungeklärt (alter Feldname `modalities`).
- **Versionen:** ältere Threads [Community, unbestätigt].
- **FIX:** Modalität pro Sitzung fest wählen; für Wechsel neue Sitzung.
- **Quelle:** https://community.openai.com/t/realtime-api-updating-modalities/996243 · https://community.openai.com/t/realtime-api-with-webrtc-issue-when-the-modality-is-updated-to-text-audio-the-audio-output-is-missing/1254337 [Community]

### 11. `output_audio_buffer.clear` über WebSocket wirkungslos
- **Symptom:** Event wird abgelehnt/ignoriert.
- **Ursache:** Nur für WebRTC/SIP, und nur nach `response.cancel`.
- **Versionen:** Realtime GA.
- **FIX:** Bei WebSocket Wiedergabe lokal stoppen + `conversation.item.truncate` (siehe F3/F4).
- **Quelle:** https://developers.openai.com/api/reference/resources/realtime/client-events [offiziell]

### 12. Realtime-Reasoning lässt sich nicht abschalten
- **Symptom:** `reasoning.effort: "none"` abgelehnt; Nicht-Reasoning-Modelle ignorieren die Einstellung.
- **Ursache:** Realtime erlaubt nur `minimal|low|medium|high|xhigh`; nur 2/2.1/2.1-mini sind Reasoning-Modelle.
- **Versionen:** gpt-realtime-2*, Stand 16.10.2026 [extern für „nur weglassen"].
- **FIX:** `minimal` für Latenz, sonst Feld weglassen.
- **Quelle:** openai-python `realtime_reasoning_effort.py` [offiziell] · https://pydantic.dev/docs/ai/realtime/openai/ · https://community.livekit.io/t/gpt-realtime-2-set-reasoning-effort-to-none-or-very-low/1062 [extern]

## D) Werkzeuge, MCP und SDK-Wrapper

### 1. Folge-Antwort nach MCP-Calls bleibt aus (Realtime)
- **Symptom:** Nach MCP-Werkzeugaufruf schweigt das Modell.
- **Ursache:** `response.done` kommt vor Abschluss der MCP-Calls; die API erzeugt die Folge-Antwort nicht selbst.
- **Versionen:** Realtime GA — dokumentiertes Verhalten.
- **FIX:** Kein Output für MCP senden, aber nach Abschluss selbst `response.create`; nach `session.update` auf `mcp_list_tools.completed` warten.
- **Quelle:** https://developers.openai.com/api/docs/guides/realtime-mcp [offiziell]

### 2. MCP-Server liefert 10 Tools, Realtime importiert 0
- **Symptom:** Im Playground/Inspector ok, in der Realtime-Sitzung keine Tools (SSE-FastMCP).
- **Ursache:** ungelöst.
- **Versionen:** Realtime [Community, unbestätigt].
- **FIX:** Transport (Streamable HTTP statt SSE) und `allowed_tools` prüfen; Tools notfalls als Function-Tools definieren.
- **Quelle:** https://community.openai.com/t/realtime-api-mcp-server-sends-10-tools-but-api-receives-0/1367933 [Community]

### 3. `gpt-realtime-2.1-mini` ruft Function Tools nicht auf
- **Symptom:** In SIP-Bestellablauf kein Tool-Call; `gpt-realtime-mini` mit gleichem Prompt schon; `gpt-realtime-mini` ruft Tools teils falsch auf.
- **Ursache:** Modellqualität (OpenAI warnt selbst: mini schwächer bei Function Calling).
- **Versionen:** gpt-realtime-2.1-mini [Community, unbestätigt].
- **FIX:** Mit eigenen Werkzeug-Testfällen messen; bei Tool-lastigen Flows großes Modell oder GPT-Live-Delegation.
- **Quelle:** https://community.openai.com/t/model-gpt-realtime-2-1-mini-not-calling-function-tools-in-sip-realtime-while-gpt-realtime-mini-works-with-the-same-prompt-tools/1386141 · https://community.openai.com/t/gpt-realtime-mini-calls-tools-incorrectly-seems-to-be-an-error-in-the-model/1373936 [Community]

### 4. Werkzeugaufrufe langsam (~6 s) und unzuverlässig; Preambles unregelmäßig
- **Symptom:** gpt-realtime-2/2.1 brauchen bis 14 s, strukturierte Tool-Tests fallen auf 0–20 %; Preambles fehlen in etwa einem Drittel der Fälle; Zustandsmaschinen-Text leckt ins Audio.
- **Ursache:** Preambles + Reasoning (Selbstbericht).
- **Versionen:** gpt-realtime-2/2.1/2.1-mini; OpenAI-Support bestätigte nur, dass Nachfolger Anforderungen noch nicht erfüllen [Community].
- **FIX:** Effort `minimal`/`low`, Preamble-Regel „höchstens ein Satz", vor Modellwechsel eigene Werkzeug-Evals.
- **Quelle:** https://community.openai.com/t/gpt-realtime-now-shows-deprecated-impact-on-deterministic-voice-agent-architectures-please-do-not-deprecate/1387746 · https://community.openai.com/t/realtime-api-preamble-inconsistent/1361953 [Community]

### 5. Modell erfindet Function-Ergebnis
- **Symptom:** Antwort mit Inhalt eines nie gesendeten Tool-Ergebnisses.
- **Ursache:** Beta-Modell; das GA-Modell fügt stattdessen einen Platzhalter ein.
- **Versionen:** Beta (behoben in GA).
- **FIX:** GA-Modell; Prompt-Regel „Erfolg erst nach Tool-Ergebnis melden".
- **Quelle:** https://developers.openai.com/blog/realtime-api [offiziell]

### 6. MCP-Auth doppelt gesetzt
- **Symptom:** MCP-Verbindung scheitert/abgelehnt.
- **Ursache:** `authorization` und `headers.Authorization` gleichzeitig.
- **Versionen:** Realtime/Responses MCP.
- **FIX:** Nur eines von beiden setzen.
- **Quelle:** https://developers.openai.com/api/docs/guides/realtime-mcp [offiziell]

### 7. Agents SDK (JS) `RealtimeSession`: Werkzeug sieht letztes Nutzertranskript nicht
- **Symptom:** Delegationswerkzeug bekommt Verlauf ohne die gerade gesprochene Frage.
- **Ursache:** Werkzeug sieht nur einen History-Schnappschuss; Eingabetranskription läuft asynchron.
- **Versionen:** openai-agents-js, Doku Stand 16.10.2026 — dokumentiertes Verhalten.
- **FIX:** Frage als Tool-Argument übergeben lassen (wie `relevantContextFromLastUserMessage`) statt allein aus der History zu lesen.
- **Quelle:** https://openai.github.io/openai-agents-js/guides/voice-agents/build/ [offiziell]

### 8. Agents SDK `VoicePipeline`: kein Barge-in
- **Symptom:** Nutzer kann die Ausgabe nicht unterbrechen; Mikrofon-Echo löst Selbstunterbrechung aus.
- **Ursache:** „The Agents SDK currently does not provide any built-in interruption handling for StreamedAudioInput."
- **Versionen:** openai-agents-python, Stand 16.10.2026.
- **FIX:** Mikrofon über `turn_started`/`turn_ended` selbst stummschalten; für Barge-in GPT-Live oder Realtime.
- **Quelle:** https://openai.github.io/openai-agents-python/voice/pipeline/ [offiziell]

### 9. LiveKit-Plugin GPT-Live: Eigenheiten
- **Symptom:** Barge-in reagiert nicht; Modell behält den ganzen Turn; `mute_input()` pausiert die Ausgabe nicht; `generate_reply()` wird nach 10 s abgelehnt; `session.say()` spricht Skript nicht wörtlich.
- **Ursache:** Standard-VAD wird für GPT-Live entfernt, Truncation nicht unterstützt.
- **Versionen:** LiveKit Agents GPT-Live-Plugin, Okt. 2026 [extern].
- **FIX:** VAD explizit setzen; Ausgabe app-seitig stoppen; für wörtliche Sätze TTS bzw. `session.output_audio.fixture.play`.
- **Quelle:** https://docs.livekit.io/agents/models/realtime/plugins/gpt-live/ [extern]

## E) Modelle, SDK-Typen, Reasoning-Effort

### 1. SDK-Typen hinken der API hinterher (Stimmen, Effort `max`) ⭐
- **Symptom:** Typfehler/IDE-Warnung für gültige Stimmen (brise, flitz, harema, juni, nira, noeul, nuri, sillage) oder für `reasoning.effort: "max"`.
- **Ursache:** openai-python `BuiltInVoice` (Live) kennt 23 Namen, die API-Referenz 31; `RealtimeReasoningEffort` und Live-`reasoning.effort` kennen `max` nicht, die Backend-Modellseiten schon.
- **Versionen:** openai-python Stand 16.10.2026.
- **FIX:** Stimme als freien String übergeben; `max` nur mit Fehlerbehandlung anbieten (Rückfall `xhigh`).
- **Quelle:** https://github.com/openai/openai-python/tree/main/src/openai/types/live · https://developers.openai.com/api/reference/resources/live/primary-websocket [offiziell]

### 2. `reasoning.effort: none` bei `gpt-6-astra` / `gpt-6.1-sol` abgelehnt
- **Symptom:** Validierungsfehler (bei Live: `responses_validation_failed`).
- **Ursache:** `gpt-6-astra` unterstützt `none` nicht (Changelog 03.09.2026); `gpt-6.1-sol` weder `none` noch `minimal`.
- **Versionen:** gpt-6-astra, gpt-6.1-sol.
- **FIX:** `low` oder höher; Effort-Liste je Backend-Modell pflegen (`gpt-6-luna`, `gpt-6-sol`, `gpt-5.6-*`: none … max).
- **Quelle:** https://developers.openai.com/api/docs/changelog · https://developers.openai.com/api/docs/models/gpt-6.1-sol · https://developers.openai.com/api/docs/models/gpt-6-astra [offiziell]

### 3. Realtime-Altmodelle laufen aus
- **Symptom:** Ab Abschaltung `model_not_found`.
- **Ursache:** `gpt-realtime` (`-2025-08-28`) und `gpt-realtime-mini` werden am **20.01.2027** abgeschaltet; `gpt-realtime-mini-2025-10-06` ist seit 23.07.2026 abgeschaltet.
- **Versionen:** siehe Datum.
- **FIX:** Auf `gpt-realtime-2.1` bzw. `gpt-realtime-2.1-mini` umstellen und vorher mit eigenen Tool-Testfällen messen (siehe D3/D4).
- **Quelle:** https://developers.openai.com/api/docs/deprecations [offiziell]

### 4. Transkriptionsmodelle laufen aus
- **Symptom:** Ab Abschaltung Fehler bei `audio.input.transcription.model`.
- **Ursache:** `gpt-4o-transcribe`, `gpt-4o-mini-transcribe`, `gpt-4o-transcribe-diarize`, `whisper-1` seit 26.08.2026 veraltet, Abschaltung **26.02.2027**.
- **Versionen:** siehe Datum.
- **FIX:** `gpt-transcribe` (0,0045 USD/Min) oder `gpt-live-transcribe` (0,017 USD/Min, Streaming).
- **Quelle:** https://developers.openai.com/api/docs/deprecations · https://developers.openai.com/api/docs/models/gpt-transcribe [offiziell]

### 5. Modell am falschen Endpunkt / nicht existierende Variante
- **Symptom:** Fehler beim Verbinden mit `gpt-live-1` an `/v1/realtime`, `gpt-realtime-2.1` an `/v1/live/sessions`, Textmodell als Realtime-`model`, oder `gpt-live-1-mini`.
- **Ursache:** Getrennte Produkte: `gpt-live-1` nur an `v1/live/sessions`; Realtime-`model` listet nur Audio-/Realtime-Modelle; eine Mini-Variante von GPT-Live gibt es in der API nicht.
- **Versionen:** Stand 16.10.2026.
- **FIX:** Endpunkt und Modell als Paar konfigurieren; Textmodelle nur als Live-Backend (`delegation.responses.model`).
- **Quelle:** https://developers.openai.com/api/docs/models/gpt-live-1 · https://developers.openai.com/api/reference/resources/realtime/subresources/client_secrets/methods/create [offiziell]

### 6. Bilder ans Backend fehlerhaft kodiert (behoben)
- **Symptom:** Bild-Eingabe über `response.item.create` an `gpt-6-sol`/`gpt-6-luna` liefert Fehler/Unsinn.
- **Ursache:** Bild-Encoding-Fehler, laut Changelog am 25.09.2026 behoben. Live-Frontend selbst nimmt keine Bilder.
- **Versionen:** bis 25.09.2026.
- **FIX:** Erledigt; Bilder nur ans vision-fähige Backend senden.
- **Quelle:** https://developers.openai.com/api/docs/changelog [offiziell]

### 7. MCP-`connector_id` veraltet
- **Symptom:** Warnung/Fehler bei MCP-Konnektoren mit neueren Modellen.
- **Ursache:** `connector_id` ist für Modelle ab 01.09.2026 als veraltet markiert.
- **Versionen:** Backend-Modelle ab 01.09.2026.
- **FIX:** `server_url` statt `connector_id`.
- **Quelle:** https://developers.openai.com/api/reference/typescript/resources/live/index.md [offiziell]

### 8. Ratenlimit gleichzeitiger Live-Sitzungen unklar
- **Symptom:** Sitzungsstart abgelehnt, obwohl unter dem erwarteten Limit.
- **Ursache:** Modellseite: Build 50 / Launch 300 / Grow 500 (Free nicht); Suchtreffer-Zusammenfassung: Tier 1: 25 / Tier 2: 50 / Tier 3: 200 — Widerspruch.
- **Versionen:** Stand 16.10.2026 [unbestätigt].
- **FIX:** Limits des eigenen Projekts in der Konsole prüfen; nicht beendete Sitzungen (A4) zählen mit.
- **Quelle:** https://developers.openai.com/api/docs/models/gpt-live-1 [offiziell]

## F) Android-Client (Kotlin, WebSocket/WebRTC) — die 10 Fallen aus r6 plus Routing

### 1. KI unterbricht sich selbst (Lautsprecher, erste Sekunden) ⭐
- **Symptom:** `speech_started` feuert direkt zu Beginn der Antwort; Modell bricht ab, Schleife.
- **Ursache:** Mikrofon hört die Modellausgabe; AEC fehlt oder ist noch nicht eingeschwungen; die API hat keine Echo-Unterdrückung.
- **Versionen:** seit Okt. 2024 gemeldet, noch 2025/2026 (Realtime/gpt-realtime) [Community, kein offizieller Fix].
- **FIX:** Client-AEC (WebRTC oder `AcousticEchoCanceler`), erste Antwort nicht unterbrechbar (`interrupt_response:false`), VAD-Schwelle hoch (0.85), Kopfhörer, als letzte Stufe Mikrofon-Gate während Agent spricht (kostet Barge-in).
- **Quelle:** https://community.openai.com/t/issue-with-realtime-api-user-interruption/968150 · https://community.openai.com/t/realtimeapi-echo-cancellation-doesn-t-work-for-the-first-10-seconds-of-session/1252805 · https://community.openai.com/t/realtime-api-echo-issue-when-on-speaker-phone/1370092 [Community]

### 2. Samsung + `VOICE_COMMUNICATION`: KI hört sich selbst
- **Symptom:** Schleife aus `speech_started` und Abbruch auf Samsung-Gerät (WebSocket).
- **Ursache:** ungeklärt, Verdacht: Hardware-AEC greift nicht (Modell/Android-Version nicht genannt).
- **Versionen:** Meldung 18.02.2026, Thread am 19.08.2026 geschlossen [Community, kein Fix].
- **FIX:** `AcousticEchoCanceler.getEnabled()` prüfen; Software-AEC (siehe F10); Lautsprecherlautstärke senken; Test auf dem Zielgerät (z. B. Galaxy Z Fold).
- **Quelle:** https://community.openai.com/t/samsung-device-echo-self-talking-issue-with-websockets-and-voice-communication/1374490 [Community]

### 3. Nach Barge-in „weiß" das Modell nicht, was gehört wurde
- **Symptom:** Transkript enthält Ungehörtes; manchmal fehlen ganze Züge.
- **Ursache:** Modell erzeugt Audio schneller als die Wiedergabe; Truncate-Ausrichtung ungenau; von OpenAI nicht erklärt.
- **Versionen:** seit Nov. 2024, bestätigt bis März 2026 (gpt-realtime, gpt-realtime-1.5) [Community, kein Fix].
- **FIX:** `audio_end_ms` aus der tatsächlichen Player-Position (`AudioTrack.playbackHeadPosition` / Sample-Rate) berechnen; Padding half wenig; bei kritischen Abläufen Ergebnis im Textkanal bestätigen.
- **Quelle:** https://community.openai.com/t/realtime-api-interruptions-dont-properly-trim-the-transcript/1000703 · https://community.openai.com/t/openai-realtime-how-to-correctly-truncate-a-live-streaming-conversation-on-speech-interruption-twilio-media-streams/1371637 [Community]

### 4. `conversation.item.truncate` abgelehnt ⭐
- **Symptom:** Fehler beim Truncate.
- **Ursache:** `audio_end_ms` größer als die tatsächliche Audiolänge des Items (z. B. Wanduhrzeit seit erstem Delta, die bei Pausen weiterwächst).
- **Versionen:** openai-agents-python vor 0.19.x (Fix gemergt 02.08.2026); eigene Clients jederzeit.
- **FIX:** `audio_end_ms = min(gehörteMs, empfangeneAudioMs)`; `item_id` nur von Assistenten-Items, `content_index: 0`.
- **Quelle:** https://github.com/openai/openai-agents-python/pull/4122 [offiziell] · https://developers.openai.com/api/reference/resources/realtime/client-events [offiziell]

### 5. Rauschen/Knistern/Brummen in der Ausgabe
- **Symptom:** Störgeräusche (WebRTC und WebSocket/Twilio).
- **Ursache:** ungeklärt; OpenAI-Mitarbeiter vermutet Format-Mismatch nach GA-Formatänderung.
- **Versionen:** gpt-realtime GA, Sept.–Dez. 2025 [Community].
- **FIX:** Format explizit `{type:"audio/pcm", rate:24000}` für Ein- und Ausgabe, AudioTrack mit exakt 24 kHz mono PCM16; Beta-Header-Workaround ist seit 12.05.2026 nicht mehr möglich.
- **Quelle:** https://community.openai.com/t/buzzing-static-noise-from-gpt-realtime-webrtc/1357298 [Community]

### 6. Audio endet vor dem Satzende, Transkript vollständig
- **Symptom:** Letzte Wörter fehlen akustisch.
- **Ursache:** ungeklärt; OpenAI bestätigte Untersuchung am 05.11.2024.
- **Versionen:** gpt-4o-realtime-preview (Okt./Nov. 2024), Modell inzwischen abgeschaltet [Community].
- **FIX:** Bei aktuellen Modellen zuerst eigenen Player prüfen (AudioTrack nicht vor dem Leerlaufen stoppen/flushen).
- **Quelle:** https://community.openai.com/t/realtime-api-audio-is-randomly-cutting-off-at-the-end/980587 [Community]

### 7. Server sendet mitten in der Sitzung keine Events mehr
- **Symptom:** Audio wird gesendet, aber keine Server-Events.
- **Ursache:** ungeklärt; Verdacht: Client-Gate ersetzt leise Chunks durch Nullen, Server-VAD hängt.
- **Versionen:** gemeldet 18.02.2026, 13.05.2026; OpenAI-Rückfrage 30.09.2026 [Community].
- **FIX:** Kein Gate, Audio unverändert senden; Watchdog (keine Events seit X s → neu verbinden).
- **Quelle:** https://community.openai.com/t/realtime-api-bug-websocket-stops-receiving-messages-mid-session-audioworklet-sending-audio-but-no-server-events-fired/1374520 [Community]

### 8. Kotlin-WebRTC: SDP hängt 10–12 s, dann `ICE FAILED`
- **Symptom:** Verbindungsaufbau scheitert.
- **Ursache:** unbestätigt (Verdacht `turn_detection: null` oder Serverseite); am 04.04.2025 ohne Codeänderung wieder ok.
- **Versionen:** gpt-4o-mini-realtime-preview, 30.03.2025 [Community].
- **FIX:** Bei Häufung Status-Seite prüfen; Timeout + Rückfall auf WebSocket.
- **Quelle:** https://community.openai.com/t/realtime-api-kotlin-webrtc-client-stopped-working-in-past-36-hours/1157917 [Community]

### 9. Mikrofon-Foreground-Service: `SecurityException` aus dem Hintergrund ⭐
- **Symptom:** `startForeground` wirft; Logcat „Foreground service started from background can not have location/camera/microphone access".
- **Ursache:** `RECORD_AUDIO` ist while-in-use; ab Android 14 (API 34, targetSdk 34+) wird beim Start geprüft.
- **Versionen:** Android 14 und höher.
- **FIX:** `foregroundServiceType="microphone"`, `FOREGROUND_SERVICE_MICROPHONE`, Start aus sichtbarer Activity oder definierter Ausnahme (Benachrichtigung, Widget, PendingIntent aus sichtbarer App); Reconnect im laufenden Service erledigen statt neu zu starten.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start · https://developer.android.com/develop/background-work/services/fgs/service-types [offiziell]

### 10. Hardware-AEC gemeldet, aber wirkungslos (oder unterdrückt die eigene Stimme)
- **Symptom:** `isAvailable()` true, Echo bleibt (Samsung u. a.); umgekehrt unterdrückt AEC die Nutzerstimme (Tab A9+, Android 10, parallel Musik).
- **Ursache:** gerätespezifisch; libwebrtc-Apps führen Modell-Blocklisten für Hardware-AEC.
- **Versionen:** geräteabhängig [extern].
- **FIX:** Hardware-AEC abschalten und Software-AEC nutzen (libwebrtc `setUseHardwareAcousticEchoCanceler(false)`), Einstellung in der App umschaltbar.
- **Quelle:** https://www.100ms.live/docs/api-reference/android/v2/lib/live.hms.video.media.settings/-h-m-s-audio-track-settings/-builder/set-use-hardware-acoustic-echo-canceler.html · https://git.secluded.site/cheogram-android/commit/a2a7256682198e03a3e66cb61eedc35bc2019d86 · https://forum.developer.samsung.com/t/built-in-aec-cancels-human-voice-how-to-configure-correctly/41426 [extern]

### 11. Ausgabe zu leise/nicht regelbar oder im Emulator stumm
- **Symptom:** Lautstärketasten wirken nicht; Emulator ohne Ton.
- **Ursache:** `AudioTrack` mit `USAGE_VOICE_COMMUNICATION` geht auf `STREAM_VOICE_CALL` (systemgesteuert).
- **Versionen:** Android allgemein [extern, nur Emulator Pixel 9 API 35 validiert].
- **FIX:** `USAGE_MEDIA` + `CONTENT_TYPE_SPEECH` für die Wiedergabe; AEC hängt an der Capture-Quelle.
- **Quelle:** https://docs.smallest.ai/atoms/atoms-platform/integrate/mobile-integrations/android-kotlin.md [extern]

### 12. Lautsprecher-Routing greift nicht
- **Symptom:** Ausgabe bleibt auf Hörmuschel/Bluetooth; `setSpeakerphoneOn(true)` ohne Wirkung.
- **Ursache:** `setSpeakerphoneOn` seit API 31 veraltet; `setCommunicationDevice` ist asynchron und kann scheitern.
- **Versionen:** Android 12+ (API 31).
- **FIX:** `setCommunicationDevice(device)` aus `availableCommunicationDevices`, Rückgabewert prüfen, bis 30 s auf Bestätigung warten, bei Fehler `clearCommunicationDevice()` + erneut; am Ende `clearCommunicationDevice()`; `MODE_IN_COMMUNICATION` im `finally` zurücksetzen.
- **Quelle:** https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-manager [offiziell]

### 13. GPT-Live: „spricht"-Anzeige hängt, Echosperre hält zu lange ⭐
- **Symptom:** Anzeige bleibt auf „Jarvis spricht", Mikrofon bleibt nach der Antwort gesperrt oder öffnet zu früh.
- **Ursache:** GPT-Live sendet auch in Pausen durchgehend Stille-Deltas in Echtzeit; wer „spricht" am Eintreffen von Daten oder mit fester Wartezeit festmacht, liegt falsch [Messung Jarvis 16.10.2026: „21 Stücke, 2100 ms Ton, davon 0 hörbar"].
- **Versionen:** Jarvis bis 1.8.105; behoben 1.8.103/1.8.106.
- **FIX:** Sprechen nur aus hörbarem Pegel (Effektivwert) ableiten; Sperre an `playbackHeadPosition` hinter dem letzten hörbaren Frame koppeln, kurzer Nachlauf, harter Deckel nach dem letzten Ton; Entscheidung im Mikrofon-Faden, der nie an `write()` blockiert.
- **Quelle:** eigener Code `Jarvis/.../live/LiveGespraech.kt`, Commits `b0655e53a`, `2ffd66945`; Best Practice §14.2 Nr. 3.

### 14. GPT-Live + Echosperre: späte Mitschrift löscht die neue Antwort
- **Symptom:** Jarvis beginnt zu sprechen und verstummt sofort.
- **Ursache:** `session.input_transcript.delta` als Barge-in-Signal; bei geschlossenem Mikrofon ist eine Mitschrift während der Rede die verspätete des vorigen Satzes.
- **Versionen:** Jarvis bis 1.8.105; behoben 1.8.106 (im Test 16.10.2026 einmal korrekt abgefangen).
- **FIX:** Mit Echosperre Ausgabe nicht leeren; Unterbrechen per Antippen anbieten.
- **Quelle:** Commit `2ffd66945`; Best Practice §14.2 Nr. 4.

### 15. Stillelücken mitten in der Rede durch Füll-Stille ⭐
- **Symptom:** Rede stockt kurz mitten im Satz.
- **Ursache:** Füll-Stille (gegen Ausgang-Standby) wurde nach Zeit geschrieben, sobald 20 ms kein Stück kam, obwohl noch Ton im Puffer lag; das verspätete Stück bekam Stille vorgesetzt.
- **Versionen:** Jarvis 1.8.101–1.8.106; behoben 1.8.107.
- **FIX:** Füll-Stille nur nach Füllstand (unter etwa zwei Mischtakten in Pausen, knapp über einem beim Sprechen).
- **Quelle:** Commit `fb2b06d9f`; Best Practice §14.2 Nr. 5.

### 16. Abhacker im ersten Wort durch falsch platziertes Polster ⭐
- **Symptom:** Wortanfang zerrissen („K … lar").
- **Ursache:** Polster-Stille direkt vor das erste *hörbare* Stück gelegt; der leise Wortansatz lag im Stück davor unter der Hörbar-Schwelle [Code-Kommentar, Rohprotokoll nicht erhalten].
- **Versionen:** Jarvis 1.8.107; behoben 1.8.108.
- **FIX:** Polster nur hinter digitaler Stille (Spitzenwert-Schwelle) und nur in Pausen aufbauen; 250 statt 150 ms.
- **Quelle:** Commit `50289b26b`; Best Practice §14.2 Nr. 6.

### 17. Puffer unter einem Mischtakt / leerer Start → Strom fliegt aus dem Mischer
- **Symptom:** Logcat `AudioTrack: restartIfDisabled … disabled due to previous underrun, restarting`, vorher „BUFFER TIMEOUT … underrun".
- **Ursache:** Lautsprecher-Mischer (Fold 8) holt 40 ms je Takt (dumpsys: 1920 Frames @ 48 kHz); Füllgrenze 40 ms bzw. leer gestarteter Track reicht nicht.
- **Versionen:** Jarvis bis 1.8.107; teilweise behoben 1.8.108. **Offen:** Im Test 16.10.2026 trotz Vorlauf zweimal `restartIfDisabled` beim Start (vor dem ersten Wort), `underrunCount` 0.
- **FIX:** Füllgrenze ≥ zwei Mischtakte in Pausen; Stille vor `play()` vorlegen; Start-Restfall weiter beobachten (Best Practice §14.5).
- **Quelle:** Commit `50289b26b`; Rohbeleg `bugs/bug-cases/2026-10-16-jarvis-gpt-live-wiedergabe.md`.

### 18. Telefonklang im Live-Gespräch
- **Symptom:** Stimme schmalbandig wie ein Anruf, auch am Handy-Lautsprecher.
- **Ursache:** `MODE_IN_COMMUNICATION` + `USAGE_VOICE_COMMUNICATION`, im Auto Bluetooth-SCO.
- **Versionen:** Jarvis 1.8.94–1.8.104; behoben 1.8.100 (Bluetooth) und 1.8.105 (ohne Bluetooth).
- **FIX:** Medienweg: `USAGE_MEDIA`, kein Telefonmodus, A2DP; Aufnahme `VOICE_COMMUNICATION` am Handy-Mikrofon plus Echosperre; Freisprechen nur als Rückfallweg.
- **Quelle:** Commits `dc0e26d3e`, `10952d23e`; Best Practice §14.2 Nr. 1.

### 19. Hinweistöne aus Hintergrundarbeit laufen über die Live-Stimme
- **Symptom:** Benachrichtigungston (Ablage, Recherche, Programmierauftrag) mitten in Jarvis' Rede.
- **Ursache:** Hintergrundarbeit meldet sich mit Ton, ohne zu wissen, dass ein Gespräch läuft.
- **Versionen:** Jarvis bis 1.8.107; behoben 1.8.108.
- **FIX:** `setSilent(GespraechsFokus.stilleHinweise)` (true, solange Gesprächsmodus oder `LiveGespraech.aktiv`); Ton- und Mikrofon-Faden mit `URGENT_AUDIO`. Störung durch CPU-Last selbst ist nicht gemessen.
- **Quelle:** Commits `fb2b06d9f`, `50289b26b`; Best Practice §14.2 Nr. 9.

## G) Kosten und Abrechnung

### 1. Live kostet bei Stille und Mute weiter ⭐
- **Symptom:** Hohe Rechnung trotz wenig Gesprochenem.
- **Ursache:** 0,05 USD/Min sekundengenau für die ganze Sitzungsdauer, auch bei Stille, `session.input_audio.mute` und Backend-Arbeit.
- **Versionen:** gpt-live-1.
- **FIX:** App-Leerlauf-Timer → `session.close`; bei langen Backend-Aufgaben Sitzung schließen und später mit `input` fortsetzen.
- **Quelle:** https://developers.openai.com/api/docs/guides/voice-latency-cost [offiziell]

### 2. WebRTC-Start bucht 15 s
- **Symptom:** Sehr kurze Sitzungen kosten mindestens 15 s.
- **Ursache:** `POST /v1/live/sessions` berechnet 15 s, mit der laufenden Dauer verrechnet (nicht zusätzlich).
- **Versionen:** gpt-live-1 WebRTC.
- **FIX:** Keine Wegwerf-Sitzungen (z. B. für Tests/Prewarm) anlegen.
- **Quelle:** https://developers.openai.com/api/docs/guides/voice-webrtc [offiziell]

### 3. Realtime-Kosten wachsen überlinear
- **Symptom:** Späte Gesprächsrunden kosten ein Vielfaches der frühen.
- **Ursache:** Die ganze Konversation wird bei jeder Antwort erneut als Input gezählt (Audio 600 Tokens/Min Nutzer, 1.200/Min Assistent).
- **Versionen:** alle Realtime-Modelle.
- **FIX:** `truncation.retention_ratio` 0,8 + `token_limits.post_instructions`, kurze Ausgaben, mini-Modell, Sitzungs-/Zuglimit, Usage je `response.done` loggen.
- **Quelle:** https://developers.openai.com/api/docs/guides/realtime-costs [offiziell]

### 4. Cache-Treffer brechen ein
- **Symptom:** `cached_tokens` sinkt plötzlich, Kosten steigen (Audio-Input 32 statt 0,40 USD/1 Mio.).
- **Ursache:** Instructions/Tools mitten in der Sitzung geändert oder früher Verlauf bearbeitet/gelöscht.
- **Versionen:** Realtime; analog Live-Backend.
- **FIX:** Instructions/Tools nach Start fix; dynamische Infos ans Ende bzw. per Item/Append.
- **Quelle:** https://developers.openai.com/api/docs/guides/realtime-costs · https://developers.openai.com/api/docs/guides/voice-latency-cost [offiziell]

### 5. Backend-Usage doppelt gezählt
- **Symptom:** Backend-Kosten in der App zu hoch.
- **Ursache:** `response.completed` (verschachtelt in `response.event`) mehrfach verbucht; Backend wird getrennt von den Voice-Minuten abgerechnet.
- **Versionen:** gpt-live-1 `responses`-Delegation.
- **FIX:** Usage je Response-ID genau einmal zählen; Voice- und Backend-Kosten getrennt ausweisen.
- **Quelle:** https://developers.openai.com/api/docs/guides/voice-latency-cost · https://developers.openai.com/api/docs/models/gpt-live-1 [offiziell]

### 6. `idle_timeout_ms` erzeugt bezahlte Nachfragen
- **Symptom:** Modell meldet sich bei Stille selbst, Kosten steigen.
- **Ursache:** Nach Stille löst `server_vad.idle_timeout_ms` eine Antwort aus (`input_audio_buffer.timeout_triggered`) — jede ist bezahlt.
- **Versionen:** Realtime.
- **FIX:** Nur bewusst setzen (Telefonie); sonst eigener Inaktivitäts-Timer, der die Sitzung schließt.
- **Quelle:** openai-python `realtime_audio_input_turn_detection.py` [offiziell]

### 7. Auffällige Abrechnung bei Realtime-Cached-Input
- **Symptom:** Unverhältnismäßige Kosten für Audio-Input trotz Cache.
- **Ursache:** ungeklärt.
- **Versionen:** Realtime [Community, unbestätigt, nicht reproduziert].
- **FIX:** Eigene Usage-Logs je Runde gegen das Dashboard abgleichen.
- **Quelle:** https://community.openai.com/t/realtime-api-cost-anomaly-disproportionate-charges-on-audio-input/1285295 [Community]

### 8. Projekt-Budget sperrt nicht
- **Symptom:** Ausgaben über dem Projektlimit.
- **Ursache:** Ältere Meldung: Limits nur als Benachrichtigung; Durchsetzung verzögert.
- **Versionen:** ältere Meldung [Community, unbestätigt].
- **FIX:** „Enforce a hard limit" aktivieren, Spend-Alerts darunter, Puffer einplanen, am Testprojekt gegenprüfen.
- **Quelle:** https://community.openai.com/t/project-limits-not-being-respected/1300634 [Community]

---

## 🔗 Bezug: Bug-Abschnitt ↔ Best-Practice (`best-practices/apis/openai-live-realtime.md`)

| Bug-Abschnitt | Best-Practice |
|---|---|
| A1, A2, A6, A12, A13 | §4 Sitzungskonfiguration |
| A3, A8, A15, A16, A4, A5, A7 | §6 Events/Ende, §7 Barge-in |
| A9, A10 | §10 Prompting |
| A11, A14 | §3 Verbindung |
| B1–B10 | §5 Delegation |
| C1–C12 | §3, §4.2, §6 |
| D1–D9 | §5, §10, §12 |
| E1–E8 | §2 Modell-IDs, §13 Optionskataloge |
| F1–F12 | §11 Android |
| F13–F19 | §14 Praxis Jarvis (Wiedergabe, Rückfall-Prüfung) |
| G1–G8 | §8 Preise, §9 Kostenhebel |
