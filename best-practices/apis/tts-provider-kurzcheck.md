# TTS-Provider (Edge-TTS, Google Chirp 3 HD) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Default-Stimme deutsch, kostenlos | Edge-TTS `de-DE-KatjaNeural`/`de-DE-ConradNeural`; Bibliothek/Token aktuell halten | §1, §3 |
| 2 | Premium deutsch, natürlichste Stimme | Google `de-DE-Chirp3-HD-<Name>` (z. B. `Kore`, `Charon`); Free-Tier 1 Mio. Zeichen/Monat | §2, §4 |
| 3 | Edge plötzlich 403 / „No audio received" | Bibliothek updaten (Sec-MS-GEC), Systemuhr per NTP genau, **keine** Datacenter-IP | §3, Bugs B1–B4 |
| 4 | Chirp 3 HD Streaming, aber MP3 gewünscht | Streaming liefert **kein MP3** → OGG_OPUS/PCM nehmen, oder Batch `text:synthesize` für MP3 | §4, Bugs G1 |
| 5 | SSML an Chirp 3 HD | Nur **synchron** (Preview), **nicht** im Streaming; Pausen via `markup`-Feld `[pause]` | §4, Bugs G2 |
| 6 | Langer Text (Briefing/Wochenrückblick) | Pro Absatz/Satz chunken; Google-Limit **5.000 Bytes**/Request, dt. Umlaute = 2 Bytes | §4, §6, Bugs G3 |
| 7 | Latenz beim ersten Ton wichtig | Streaming-Synthese nutzen; sonst pro Absatz vorab erzeugen + Pipeline (wie `speakAbsatzAt`) | §5 |
| 8 | Offline / Cloud fällt aus | Android-native `TextToSpeech` als Fallback, `de_DE`-Verfügbarkeit + Daten prüfen | §7 |
| 9 | Gleiche Texte wiederholt (Briefing) | Audio **cachen**: Key = hash(provider+model+voice+settings+text), MP3/OGG in cacheDir, LRU | §8 |
| 10 | Fehler 429/5xx/Netz | Exponentielles Backoff + Jitter, `Retry-After` lesen; 4xx (INVALID_ARGUMENT) **nicht** retryen | §9 |
| 11 | API-Key Google | Niemals in URL/Repo; verschlüsselt (EncryptedSharedPreferences), besser OAuth/Service-Account | §10 |
| 12 | Provider-Wahl im UI | Eine Quelle der Wahrheit (selektierter Provider), klarer Fallback-Pfad, kein stilles Mischen | §11 |
| 13 | Lokale Stimme in Verkaufs-App | System-TTS + `synthesizeToFile` vorab; sherpa-onnx/Piper zieht espeak-ng (GPL) mit | §12 |
| 14 | Piper-Stimme wählen | Lizenz je MODEL_CARD: thorsten/kristin/siwis/davefx ok, lessac/amy/tom meiden | §12 |
| 15 | Lokales Diktat | On-Device-Recognizer prüfen + Fallback; whisper.cpp base/small q5_1 nachladen | §12 |
| 16 | Edge in Verkaufs-App | Inoffiziell, ToS-Risiko; nur Opt-in „online/inoffiziell“, sonst Azure Paid + Token-Broker | §13 |
| 17 | „Klingt wie Edge, aber lokal“ gefordert | Gibt es 2026 nicht fertig. Stufen: Pre-Rendering (Cloud) → lokales Modell ohne espeak → Cloud per Broker → System-TTS | §15 |
| 18 | Lokales Modell für Deutsch wählen | Prüfraster: Lizenz von Code und Gewichten, G2P ohne GPL, DE nativ, RTF auf dem Zielgerät. Favoriten: Pocket TTS (CC-BY), Supertonic 3 (OpenRAIL-M), Kokoro-German-Martin + Lexikon | §16 |
| 19 | Piper/Kokoro ohne espeak in der APK | Lexikon-Muster: espeak nur beim Bauen, Wort→Phonem-Tabelle ausliefern, OOV per MIT-G2P (CharsiuG2P/DeepPhonemizer) | §16 |
| 20 | Kommerzielles On-Device-SDK | Azure Embedded (Limited-Access-Antrag), Picovoice Orca (Self-Service, 6.000 $/J), Acapela (% vom Verkaufspreis) | §15 |
| 21 | Lizenzierte Cloud-Stimme DE | Spitze: Cartesia Sonic 3.6, Inworld TTS-2. Key nur im Broker, Audio cachen (Google/ElevenLabs erlaubt), KI-Stimme kennzeichnen (AI Act Art. 50) | §17 |
| 22 | Lokale Synthese Deutsch | Textnormalisierung vorschalten (Zahlen/Datum/Euro/Abkürzungen), satzweise streamen, Warm-up, eigener RTF-Benchmark | §16 |
