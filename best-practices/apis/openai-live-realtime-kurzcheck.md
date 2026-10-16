# OpenAI Live- und Realtime-Sprach-API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.
> Stand: 2026-10-16 (§14 Praxis-Erkenntnisse Jarvis 19:23) · Volltext: `best-practices/apis/openai-live-realtime.md`

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
| 11 | API-Schlüssel | Standard-Key gehört auf einen Server; in einer verteilten App nur Ephemeral-Key — private Eigen-App mit eigenem Key ist bewusstes Restrisiko | §12 |
| 12 | GPT-Live flüssig abspielen | Live liefert auch in Pausen Stille-Audio in Echtzeit → „spricht" nur per Pegel erkennen; Puffer wächst nie von selbst → Polster **in Pausen hinter Stille** aufbauen, Füll-Stille nur bei fast leerem Puffer, nie mitten in die Rede; Medienton statt Telefonmodus; Rückfall-Prüfung per Logcat-Tag `JarvisLiveRouting` | §14 |
| 13 | Live vor Hintergrundarbeit | Ton-/Mikro-Faden `URGENT_AUDIO`, Socket-Lesefaden `AUDIO`; eigene Hinweise im Gespräch stumm (`GespraechsFokus.stilleHinweise`); nichts Blockierendes zwischen Füllstandsmessung und `write()` | §14.2 Nr. 9 |
