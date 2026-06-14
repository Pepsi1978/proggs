# Bekannte Bugs: TTS-Provider (Edge-TTS, Google Chirp 3 HD, Android-native)

> PFLICHT-LESEN vor Arbeit an der Vorlese-/TTS-Anbindung (BestJournal Android, `vorlese-overlay-v2`).
> Stand: zuletzt recherchiert am 2026-06-14. Versions-Anker: `rany2/edge-tts` 7.2.8 (22.03.2026);
> Google Cloud Text-to-Speech API v1, Chirp 3: HD (de-DE GA), SSML-Preview (Doku 09.06.2026),
> Quotas 01.06.2026; Android `TextToSpeech` Platform-SDK.
> Fokus deutsche Stimmen. **ElevenLabs bewusst ausgelassen (zu teuer).**
> Zweite Seite (wie macht man es richtig):
> [`best-practices/projekt-code/apis/best-practices-tts-provider.md`](../../best-practices/projekt-code/apis/best-practices-tts-provider.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Dieser Kurzcheck ist Vorab-Pflicht (`Read` mit `limit=80`). Der Volltext darunter
> ist Pflicht bei JEDEM Fehler in diesem Bereich. Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Edge plötzlich `403` Invalid status | Bibliothek/Token-Logik updaten (Sec-MS-GEC seit 08/2024 Pflicht) | E1 |
| 2 | Edge `403` trotz Update | Systemuhr per NTP genau stellen (Token = Zeit-Hash, Clock-Skew) | E2 |
| 3 | Edge „No audio received" | Nicht von Datacenter-IP; echten, nicht-leeren Text senden | E3 |
| 4 | Edge nach Chrome-Update tot | `Sec-MS-GEC-Version` = `1-<aktuelle Chromium-Version>` nachziehen | E4 |
| 5 | Edge `Invalid SSML` | Kein Custom-SSML; nur rate/volume/pitch | E5 |
| 6 | Chirp Streaming + MP3 → Fehler/Stille | Streaming kann kein MP3 → OGG_OPUS/PCM, oder Batch für MP3 | G1 |
| 7 | Chirp SSML-Tags werden vorgelesen/ignoriert | SSML nur synchron (Preview), nicht im Streaming | G2 |
| 8 | Chirp `INVALID_ARGUMENT` bei langem dt. Text | 5.000-Byte-Limit; Umlaute/ß = 2 Bytes → enger chunken | G3 |
| 9 | Chirp `[pause]` wirkt nicht | Pause nur im `markup`-Feld, nicht in `text` | G4 |
| 10 | Chirp Tonhöhe lässt sich nicht ändern | Pitch wird nicht unterstützt; nur `speaking_rate` | G5 |
| 11 | Native TTS spielt falsche/keine Stimme | Engine nicht garantiert; `de_DE`-Verfügbarkeit + Daten prüfen | N1, N2 |
| 12 | Falsche Audiodatei aus Cache | Cache-Key muss provider+model+voice+settings+ssml+text umfassen | C1 |
| 13 | Endlos-Retries / Kosten | 4xx/INVALID_ARGUMENT nicht retryen; Backoff+Jitter nur bei transient | C2 |
| 14 | API-Key geleakt | Key nie in URL/Repo; verschlüsselt + API-Restriction | S1 |

---

## E) Edge-TTS (Microsoft Read-Aloud, frei)

### E1. `403 Invalid response status` — Sec-MS-GEC-Token fehlt ⭐ HAEUFIG
- **Symptom:** WebSocket/HTTP `403`, früher funktionierender Edge-TTS-Code liefert nichts mehr.
- **Ursache:** Microsoft verlangt seit **August 2024** den Anti-Abuse-Parameter `Sec-MS-GEC`
  (+ `Sec-MS-GEC-Version`) am Synthese-Endpunkt. Alter Code/alte Bibliothek ohne Token → 403.
- **Versionen:** alle edge-tts-Clients vor der Sec-MS-GEC-Unterstützung; tritt periodisch erneut auf.
- **FIX:** `rany2/edge-tts` auf aktuelle Version (≥ 7.x, hier 7.2.8) updaten bzw. im eigenen
  WebSocket-Code den `Sec-MS-GEC`-Token erzeugen (SHA-256 aus 5-min-gerundeter Windows-Zeit +
  TrustedClientToken) und `Sec-MS-GEC-Version=1-<Chromium-Version>` mitsenden.
- **Quelle:** https://github.com/rany2/edge-tts/issues/290 (extern, 2026)

### E2. `403` trotz aktueller Bibliothek — Systemuhr falsch (Clock-Skew) ⭐ HAEUFIG
- **Symptom:** 403 obwohl Bibliothek aktuell; auf anderem Gerät geht es.
- **Ursache:** Der `Sec-MS-GEC`-Wert ist ein **Hash der aktuellen Uhrzeit** (auf 5 min gerundet).
  Geht die Geräteuhr mehr als wenige Minuten falsch, ist der Token ungültig → 403.
- **Versionen:** geräteabhängig, jederzeit.
- **FIX:** Systemzeit automatisch (NTP) stellen lassen; auf dem Server/Container Zeitsync prüfen.
  Token immer frisch pro Request erzeugen (nicht cachen über 5 min hinaus).
- **Quelle:** https://github.com/rany2/edge-tts/issues/290 · https://pkg.go.dev/github.com/wujunwei928/edge-tts-go/edge_tts (extern)

### E3. „No audio received" — Datacenter-IP gefiltert oder leerer Text ⭐ HAEUFIG
- **Symptom:** WebSocket öffnet, aber es kommen **keine Audio-Frames** (`NoAudioReceived`).
  Besonders gemeldet für **deutsche** Stimmen aus Cloud-Umgebungen.
- **Ursache:** (a) Microsoft filtert **Datacenter-/Cloud-IP-Ranges** (Colab, HF Spaces, Server) →
  Verbindung akzeptiert, aber keine Audioframes. (b) Eingabe ist **leer / nur Satzzeichen /
  Whitespace**. (c) Voice-Name falsch geschrieben.
- **Versionen:** seit Anti-Abuse-Verschärfung 2024/2025; weiterhin aktuell.
- **FIX:** Vom **Heim-/Mobil-IP** senden (Frank: Handy ist ok). Auf Servern: Azure Speech TTS
  (offizielle API) nehmen ODER lokalen Relay über Residential-IP. Text vor dem Senden bereinigen,
  leere/punktuierte Chunks **überspringen**. Voice-ID exakt aus der `--list-voices`-Liste.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/5653188 (offiziell-Forum, 2026)

### E4. Nach Chrome/Chromium-Update wieder 403 — Versions-Anker veraltet
- **Symptom:** Lief lange, dann plötzlich wieder 403 nach einiger Zeit.
- **Ursache:** `Sec-MS-GEC-Version` koppelt an die Chromium-Vollversion (`1-<Version>`). Microsoft
  kann veraltete Client-Versionen abweisen.
- **Versionen:** rollierend; Bibliotheks-Maintainer ziehen die Version nach.
- **FIX:** Bibliothek aktualisieren (sie pflegt die Versionsnummer); im Eigenbau die Chromium-
  Version regelmäßig nachziehen.
- **Quelle:** https://github.com/travisvn/edge-tts-universal/issues/19 (extern, 2026)

### E5. `Invalid SSML` — eigenes SSML wird abgelehnt
- **Symptom:** Fehler beim Senden von eigenem SSML mit mehreren Tags/Stimmen.
- **Ursache:** Microsoft erlaubt am Read-Aloud-Endpunkt **nur** das vom Edge-Browser selbst
  erzeugbare SSML: **ein** `<voice>` mit **einem** `<prosody>`. Mehr → Ablehnung. Darum hat
  `rany2/edge-tts` Custom-SSML **entfernt**.
- **Versionen:** per Design, dauerhaft.
- **FIX:** Kein eigenes SSML bauen. Nur die Parameter **rate / volume / pitch** nutzen
  (z. B. `rate=-10%`, `pitch=-2Hz`).
- **Quelle:** https://github.com/rany2/edge-tts (extern, README „Custom SSML", 22.03.2026)

---

## G) Google Chirp 3 HD

### G1. Streaming liefert **kein MP3** → Fehler oder unspielbares Audio ⭐ HAEUFIG
- **Symptom:** `streamingSynthesize` mit `MP3` schlägt fehl bzw. App kann das Audio nicht abspielen.
- **Ursache:** Chirp-3-HD-**Streaming** unterstützt nur **ALAW, MULAW, OGG_OPUS, PCM** — **kein MP3**.
  MP3 gibt es nur im **Batch** (`text:synthesize`).
- **Versionen:** Chirp 3: HD, Stand 06/2026 — per Design.
- **FIX:** Im Streaming **OGG_OPUS** oder **PCM/LINEAR16** anfordern und passend dekodieren; wenn
  zwingend MP3 → **Batch**-Endpunkt nutzen (kostet etwas Latenz).
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (offiziell, 09.06.2026)

### G2. SSML-Tags werden vorgelesen oder ignoriert (Streaming) ⭐ HAEUFIG
- **Symptom:** `<break>`/`<prosody>` etc. erscheinen als gesprochener Text oder wirken nicht.
- **Ursache:** SSML wird von Chirp 3: HD **nur für synchrone** Requests unterstützt (Preview),
  **nicht im Streaming**. Ältere Doku behauptete sogar „gar kein SSML" — das stimmt nicht mehr,
  aber Streaming bleibt ohne SSML. Nicht-unterstützte Tags werden ignoriert.
- **Versionen:** Chirp 3: HD, SSML = Preview (06/2026).
- **FIX:** Für SSML synchron synthetisieren; im Streaming Pausen über das **`markup`-Feld**
  (`[pause]`) und Tempo über `speaking_rate` steuern statt SSML.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (offiziell, 09.06.2026)

### G3. `INVALID_ARGUMENT` bei langem deutschem Text — 5.000-Byte-Limit ⭐ HAEUFIG
- **Symptom:** Lange Briefings/Wochenrückblicke werfen `400 INVALID_ARGUMENT` / „input too long".
- **Ursache:** Hartes Content-Limit **5.000 Bytes pro Request** (nicht erhöhbar). **Deutsche
  Umlaute (ä, ö, ü) und ß zählen als 2 Bytes** (UTF-8) → der Text ist früher „voll" als die
  Zeichenzahl vermuten lässt.
- **Versionen:** Cloud TTS v1, dauerhaft.
- **FIX:** Vor dem Senden in Absätze/Sätze chunken, Sicherheitsmarge (~4.000 Bytes) wegen Umlauten;
  Byte-Länge prüfen, nicht Zeichen-Länge. Pro Chunk ein Request, in der Absatz-Pipeline abspielen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/quotas (offiziell, 01.06.2026)

### G4. `[pause]` ohne Wirkung — falsches Eingabefeld
- **Symptom:** Pausen-Tags `[pause]`/`[pause long]` ändern nichts.
- **Ursache:** Pause-Tags wirken **nur im `markup`-Feld**, **nicht** im `text`-Feld. Bei
  Positionierung an unnatürlicher Stelle ignoriert das Modell sie zudem manchmal.
- **Versionen:** Chirp 3: HD Voice-Controls (Preview, 06/2026).
- **FIX:** Text als `markup` senden, Tags an natürliche Satzgrenzen setzen; nicht übermäßig stapeln.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (offiziell, 09.06.2026)

### G5. Tonhöhe (pitch) lässt sich nicht steuern
- **Symptom:** `pitch`-Parameter zeigt keine Wirkung / wird abgelehnt.
- **Ursache:** Chirp 3: HD bietet **keine freie Pitch-Steuerung** (anders als ältere
  Standard/WaveNet-Stimmen). Nur **`speaking_rate`** (0.25–2.0) ist verfügbar. Hinweis: Ältere
  Doku-Stellen listen Chirp 3 HD pauschal als „kein speaking_rate/pitch" — die aktuelle
  Chirp3-HD-Seite zeigt jedoch **Tempo (pace) als unterstützt**; nur Pitch bleibt außen vor.
- **Versionen:** Chirp 3: HD, 06/2026 (Doku in Bewegung — Tempo ja, Pitch nein).
- **FIX:** Tempo über `speaking_rate` regeln; Tonhöhe nicht erwarten. Wer Pitch braucht: andere
  Stimmfamilie (Standard/WaveNet) — kostet aber Natürlichkeit.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (offiziell, 09.06.2026) ·
  https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types (offiziell)

### G6. Stimme im Streaming/Region nicht gefunden
- **Symptom:** `INVALID_ARGUMENT`/„voice not found" obwohl Name korrekt aussieht.
- **Ursache:** Voice-Name-Schema `<locale>-Chirp3-HD-<Name>` muss exakt passen; nicht jede Stimme
  ist in jeder Region/jedem Pfad gleich verfügbar; hartkodierte Listen veralten.
- **Versionen:** laufend (Google ergänzt Stimmen).
- **FIX:** Stimmen zur Laufzeit über `GET /v1/voices?languageCode=de-DE` abrufen + cachen; exakten
  Namen verwenden; hartkodierte Liste nur als Fallback.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (offiziell, 09.06.2026)

---

## N) Android-native TextToSpeech (Offline-Fallback)

### N1. Vorgegebene Engine wird nicht geladen
- **Symptom:** Andere Stimme/Engine spricht als erwartet, oder gar keine.
- **Ursache:** Auch mit angegebenem Engine-Namen lädt das System die **Default-Engine**, wenn die
  gewünschte fehlt/deaktiviert ist. Keine Garantie.
- **Versionen:** Android-Platform, dauerhaft.
- **FIX:** `getEngines()` prüfen, Verfügbarkeit testen, im UI klarmachen, welche Offline-Stimme
  läuft; nicht blind auf Google-Engine setzen.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech.OnInitListener (offiziell)

### N2. `LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED` für `de_DE`
- **Symptom:** `setLanguage(Locale.GERMAN)` schlägt fehl, keine Sprache.
- **Ursache:** Deutsche Sprachdaten der TTS-Engine sind nicht installiert bzw. die Engine kann
  de_DE nicht.
- **Versionen:** geräteabhängig.
- **FIX:** Rückgabewert von `setLanguage` prüfen; bei `LANG_MISSING_DATA` per
  `ACTION_INSTALL_TTS_DATA`-Intent zur Installation führen; vorher `isLanguageAvailable` testen.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech (offiziell)

---

## C) Querschnitt (Caching, Retry)

### C1. Cache liefert falsche Audiodatei — unvollständiger Key ⭐ HAEUFIG
- **Symptom:** Nach Stimmen-/Tempo-/Provider-Wechsel kommt die **alte** Audioausgabe.
- **Ursache:** Cache-Key umfasst nicht alle ausgabe-relevanten Felder (nur Text gehasht).
- **Versionen:** eigener Code.
- **FIX:** Key = `sha256(provider | model | voiceId | speakingRate | rate/volume/pitch | ssml? |
  normalisierterText)`. Bei Änderung des Aufbereitungs-Algorithmus Cache-Version hochzählen.
- **Quelle:** Eigenes Muster (siehe Best-Practices §8).

### C2. Endlose Retries / unnötige Kosten bei 4xx
- **Symptom:** App wiederholt fehlschlagende Requests, Google-Zeichen-Kosten/Quota steigen, oder
  Edge-403 wird stumpf wiederholt.
- **Ursache:** Retry-Logik unterscheidet nicht zwischen transient (429/5xx/Netz) und permanent
  (400/INVALID_ARGUMENT/401/403-Auth/Längen-Limit).
- **Versionen:** eigener Code.
- **FIX:** Nur transient retryen, exponentielles Backoff + Jitter, `Retry-After` respektieren;
  Edge-403 zuerst Token/Uhr erneuern; bei permanent sofort Fallback (anderer Provider / native TTS).
- **Quelle:** Best-Practices §9 + `apis/api-integration-general.md`.

---

## S) Sicherheit

### S1. Google-API-Key in URL oder Repo
- **Symptom:** Key in Logs/Proxy-Caches/Git-Historie auffindbar; Missbrauch/Kosten.
- **Ursache:** Key als `?key=...` in der Request-URL oder hartkodiert eingecheckt.
- **Versionen:** eigener Code.
- **FIX:** Key nie in URL (Header/Body) und nie im Repo; verschlüsselt ablegen
  (EncryptedSharedPreferences, so im Projekt) oder besser OAuth/Service-Account; in der
  Cloud-Console die API-Restriction auf TTS setzen.
- **Quelle:** https://cloud.google.com/docs/authentication (offiziell) + Best-Practices §10.

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-tts-provider.md`) |
|---------------|-----------------------------------------------------------|
| E1–E5 | §1, §3 (Edge Auth/Streaming/SSML/Stimmen) |
| G1–G6 | §4 (Chirp Format/SSML/Limit/Controls/Stimmenliste) |
| N1–N2 | §7 (Offline-Fallback Android-native) |
| C1 | §8 (Caching) |
| C2 | §9 (Retry/Backoff/Fallback-Kette) |
| S1 | §10 (Secrets) |
