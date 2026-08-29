# R7 — Bestätigte Bugs, Fallstricke und offene Issues bei der Gemini-Live-Transkription

Recherche-Fokus: WebSocket-`bidiGenerateContent` (v1beta), speziell rund um `gemini-3.5-transcribe-live` sowie die eng verwandten `*-native-audio`-Live-Modelle, deren Transkriptionsprobleme dieselbe Server-Pipeline betreffen. Durchsucht: GitHub-Issues (`googleapis/js-genai`, `googleapis/python-genai`, `google-gemini/cookbook`, `livekit/agents`), Google AI Developers Forum (discuss.ai.google.dev), offizielle Doku.

Einordnung je Fund: **BESTÄTIGT** (mit URL, Status offen/geschlossen) oder **VERMUTUNG** (aus verwandten Funden abgeleitet, kein direkter Beleg für exakt dieses Szenario).

---

## 1. Transkriptionsfelder bleiben komplett leer, obwohl Audio korrekt verarbeitet wird

**BESTÄTIGT.** [googleapis/python-genai#1279](https://github.com/googleapis/python-genai/issues/1279) — Status: **geschlossen** (gemeldet 18.08.2025). Modell: `gemini-live-2.5-flash-preview-native-audio` (auch mit `gemini-2.0-flash-live-preview-04-09` und `gemini-live-2.5-flash` reproduziert). Trotz exakt dokumentierter Konfiguration (`output_audio_transcription: {}`, `input_audio_transcription: {}`, `response_modalities: ["AUDIO"]`) blieben `input_transcription`/`output_transcription` in der Server-Antwort dauerhaft leer — sowohl über Vertex- als auch über Dev-Endpoint. Ein Google-Mitarbeiter (`shivvaam0001`) verwies als Fix auf ein neues Modell (`gemini-2.5-flash-native-audio-preview-09-2025`). Kein Workaround außer Modellwechsel dokumentiert.

Das ident gelagerte Problem trat später erneut im Node-SDK auf: [googleapis/js-genai#1212](https://github.com/googleapis/js-genai/issues/1212) — Status: **geschlossen** (gemeldet 23.12.2025, `@google/genai@^1.34.0`, Modell `gemini-2.5-flash-native-audio-preview-09-2025`). Zwei Szenarien: (a) `responseModalities: [AUDIO, TEXT]` gemäß Doku-Empfehlung für Transkription → sofortiger Verbindungsabbruch mit **Code 1007 „Request contains an invalid argument"**; (b) nur `AUDIO` als Modalität → Verbindung stabil, Audio läuft, aber `inputAudioTranscription`/`outputAudioTranscription` erscheinen nie im `onmessage`-Callback. Der Reporter verweist explizit auf die Ähnlichkeit zu #1279.

Älterer, noch grundlegenderer Fall: [googleapis/js-genai#478](https://github.com/googleapis/js-genai/issues/478) „Live inputTranscription server messages are not sent" — Status: **geschlossen** (gemeldet 21.04.2025, js-genai 0.9.0, Modell `gemini-2.0-flash-live-001`), Re-Open von #454. Mehrere Nutzer bestätigen das gleiche Verhalten; ein Kommentator (`laishere`) merkt an, dass anders als bei OpenAIs Realtime API keine VAD-Start/End-Events und keine Verknüpfung Transkript↔Audio-Segment (`itemId`) existieren, weshalb man auf externe STT ausweichen musste.

→ Fazit: Leere/fehlende Transkriptionsfelder sind ein **wiederkehrendes, über mehrere Modellgenerationen und beide SDKs (Python/JS) bestätigtes Muster**, jeweils nur durch Modell- oder SDK-Versionswechsel behoben — nicht strukturell gelöst.

---

## 2. Transkription kommt nur wortweise/buchstabenweise, nie als zusammenhängender Satz

**BESTÄTIGT.** [google-gemini/cookbook#951](https://github.com/google-gemini/cookbook/issues/951) „Audio transcript in Gemini Live API not really working" — Status: **geschlossen** (gemeldet 21.09.2025). Modell-Transkript kam fragmentiert an: `Ca` / `n I` / ` pl` / `eas` / `e h` / `ave` / … statt `Can I please have your account number`. Keine dokumentierte Lösung im Thread, nur die Bug-Meldung selbst.

Verwandt: [googleapis/js-genai#1429](https://github.com/googleapis/js-genai/issues/1429) „Transcription finished flag never updates" (js-genai 1.44.0, Modell `gemini-2.5-flash-native-audio-preview-12-2025`, v1alpha, Ephemeral-Token-Auth). Laut Doku/TS-Typen sollte das letzte Transkriptions-Fragment eines Turns `finished: true` tragen — dieses Feld fehlt in der Praxis **immer**, sowohl bei Input- als auch Output-Transkription. Einziges verlässliches Turn-Ende-Signal ist `serverContent.turnComplete === true`, das aber auf einer separaten Nachricht ohne Transkriptions-Payload kommt und nur das *Modell*-Turn-Ende markiert, nicht das des Nutzers. Workaround laut Issue: Fragmente selbst puffern und beim Eintreffen des ersten Output-Fragments bzw. bei `turnComplete` flushen — man muss die Signalfolge selbst reverse-engineeren.

---

## 3. Vorzeitiger `turnComplete` schneidet Audio/Text mitten im Satz ab (server-seitig, nicht Echo/VAD)

**BESTÄTIGT, weiterhin offen.** [googleapis/python-genai#2117](https://github.com/googleapis/python-genai/issues/2117) — Status: **OFFEN** (gemeldet 27.02.2026). Modell: `gemini-2.5-flash-native-audio-preview-12-2025`, SDK `google-genai 1.64.0` via `google-adk 1.25.1`, Dev-API (nicht Vertex). Nach 1–3 Sätzen (teils mitten im Wort) kommt `turnComplete` **ohne** `interrupted: true` — der Server beendet den Turn selbständig, der Rest wird nie ausgeliefert. Der Reporter hat fünf clientseitige Gegenmaßnahmen implementiert (Hardware-AEC, Echo-Gating, SileroVAD-Bestätigung, `NOINTERRUPTION`-Modus, deaktivierte automatische Aktivitätserkennung) — das Problem besteht trotzdem weiter, was laut Issue eindeutig auf eine **serverseitige** Ursache hindeutet, nicht auf Echo oder Client-VAD. Google-Doku empfiehlt hier bezeichnenderweise nur „Kopfhörer benutzen", was der Reporter als unzureichend zurückweist. Verstärkende Faktoren laut Issue: Tool-Calls/Function-Calling, wachsender Kontext, nicht-englische Sprachen (Chinesisch/Japanisch schlimmer), `enable_affective_dialog`, `context_window_compression`. Das Issue verweist auf ~40 Entwickler-Bestätigungen über 8 Monate hinweg, u. a. verwandt mit [js-genai#707](https://github.com/googleapis/js-genai/issues/707) „responses cut off prematurely with turnComplete despite incomplete content".

Ergänzend: [livekit/agents#5742](https://github.com/livekit/agents/issues/5742) „trailing audio truncated when a tool call ends the turn" — Audio endet exakt beim letzten Nicht-Stille-Sample ohne Trailing-Silence, was zu Wort-/Silben-Abschnitten führt, wenn ein Tool-Call mittendrin feuert, bevor die letzten `modelTurn.parts` geflusht wurden.

Diese Funde betreffen primär die *Output*-Seite (Modell-Antwort), nicht direkt `input_transcription` — aber die gleiche Turn-Verwaltungs-Pipeline wird auch für die Input-Transkriptions-Turns genutzt, weshalb das Risiko auf reine Transkriptionsmodelle mit übertragbar ist (siehe Punkt 5).

---

## 4. Halluzinierte Transkriptionen bei Stille / ohne echten Spracheingang

**BESTÄTIGT.** Google AI Developers Forum: [„Gemini Live API models 'inputTranscription' hallucinations"](https://discuss.ai.google.dev/t/gemini-live-api-models-inputtranscription-hallucinations/107899) (21.10.2025, kein Vertex-Custom-Live-Modell-Detail im Titel, aber im Body als benutzerdefiniertes Vertex-Live-Setup beschrieben). Beobachtung: Ohne jede tatsächliche Sprachäußerung sendet das Modell `inputTranscription`-Events — zwei Transkriptionen mit „buchstäblich zufälligem" unleserlichem Inhalt, gefolgt von einer Transkription mit Inhalt `None`. Das Modell generiert daraufhin unpassende Antworten wie „Usted disculpe, no le he entendido muy bien", obwohl niemand gesprochen hat. Keine Google-Antwort, kein Workaround im Thread dokumentiert.

Verwandt (indirekte Bestätigung des Grundphänomens „VAD/Modell reagiert auf Nicht-Sprache"): [google-gemini/cookbook#1262](https://github.com/google-gemini/cookbook/issues/1262) — Status **offen** (gemeldet 09.06.2026, Label „awaiting response"), Modell `gemini-3.1-flash-live-preview`: VAD-„Turn-Thrashing" mit 60–160 Turn-Events/Minute statt erwarteter 5–15, `turn_complete` löst teils <500 ms nach Sprachbeginn aus und unterbricht den Nutzer mitten im Satz. Zusätzlich falsche Spracherkennung: trotz explizitem `languageCode: "es-MX"` erscheinen zufällig portugiesische Tokens in spanischen Sitzungen, reproduzierbar über 5 verschiedene Mandanten hinweg — vermutetes serverseitiges Modell-Update als Ursache, kein bestätigter Workaround außer Rückstufung auf `gemini-2.5-flash-native-audio-latest`.

---

## 5. Setup mit `systemInstruction` wird angenommen, aber stillschweigend ignoriert (verwandtes Modell, nicht `transcribe-live` selbst)

**BESTÄTIGT für `gemini-3.5-live-translate-preview`, NICHT direkt für `gemini-3.5-transcribe-live` belegt.** Forum-Post: [„Gemini-3.5-live-translate-preview: systemInstruction is accepted and silently ignored, plus polarity reversals on long sentences"](https://discuss.ai.google.dev/t/gemini-3-5-live-translate-preview-systeminstruction-is-accepted-and-silently-ignored-plus-polarity-reversals-on-long-sentences/179912). Das Setup wird angenommen, `setupComplete` kommt normal zurück — die `systemInstruction` (Test: jeder Satz soll mit „ZEBRA" beginnen) erscheint aber **null Mal** in der Ausgabe. Es gibt **keinen** Hinweis auf Verbindungsabbruch oder verweigertes `setupComplete` — im Gegenteil, das Setup läuft scheinbar fehlerfrei durch, das Feld wird nur nicht angewendet. Zusätzlich dokumentiert: „Polarity Reversals" bei langen Sätzen (Bedeutungsumkehr, z. B. „nicht" → „sicher") — 4,1 Fälle pro 100 Sätze auf Deutsch, 10,2 auf Persisch. Kein Google-Kommentar, kein Workaround im Thread.

**Zu deiner konkreten Frage (Setup wird stumm abgelehnt / Verbindung schließt ohne Fehlertext bei `speechConfig.languageCode` + `systemInstruction` an ein `*-transcribe-live`-Modell):** Dafür wurde **kein direkter Beleg** gefunden. Zwei Indizien deuten aber in unterschiedliche, jeweils unangenehme Richtungen: (a) beim strukturell verwandten `-live-translate-preview`-Modell wird `systemInstruction` nicht mit einem Fehler abgelehnt, sondern **kommentarlos ignoriert** — die Verbindung bleibt bestehen; (b) bei den `*-native-audio`-Modellen führte die Kombination bestimmter Felder (z. B. `TEXT` in `responseModalities` zusammen mit Transkriptions-Flags) zu einem **expliziten** 1007-Fehler mit Textreason, nicht zu stillem Verbindungsabbruch (#1212 oben). Die offizielle Doku zu `live-transcribe` listet `systemInstruction`/`speechConfig` nicht explizit als unterstützte Felder für Transkriptions-Modelle und weist allgemein darauf hin, dass „unsupported fields" zu „silent failures or connection issues" führen können — das ist aber eine allgemeine Doku-Formulierung, keine spezifisch bestätigte Fehlermeldung für dieses Modell. **Empfehlung:** vor dem produktiven Verlassen auf ein bestimmtes Fehlverhalten selbst gezielt testen (Setup mit vs. ohne `languageCode`/`systemInstruction`, jeweils mit Logging auf WebSocket-Close-Code und -Reason).

---

## 6. Automatische Sprechpausen-Erkennung beendet Turn vorzeitig bei schnell/vorab gesendetem Audio

**VERMUTUNG — kein direkter Beleg für exakt dieses Szenario (vorab aufgenommenes Audio schneller als Echtzeit gesendet).** Was bestätigt ist: Automatische VAD arbeitet primär timing-basiert (Stille-Erkennung über `silence_duration_ms`/`end_of_speech_sensitivity`/`prefix_padding_ms`); ein Google-Cloud-Best-Practices-Dokument empfiehlt für manuelle VAD eine Stille-Schwelle von **mindestens 500 ms**, da niedrigere Schwellen (100–200 ms) natürliche Sprechpausen fälschlich als Turn-Ende werten und Äußerungen zerstückeln. Das oben genannte Cookbook-Issue #1262 bestätigt genau dieses Symptom in freier Wildbahn (`turn_complete` <500 ms nach Sprechbeginn, 60–160 statt 5–15 Turns/Minute) — allerdings bei normalem (nicht beschleunigtem) Audio-Stream. Es gibt außerdem einen dokumentierten Forum-Hinweis zu Konflikten in der `prefixPaddingMs`-Doku selbst ([„Conflicting definitions of prefixPaddingMs"](https://discuss.ai.google.dev/t/conflicting-definitions-of-prefixpaddingms-in-the-gemini-live-api-documentation/179129)), was zusätzliche Unsicherheit bei der Konfiguration nahelegt.

Die Doku erwähnt explizit: Wird der Audio-Stream für über eine Sekunde pausiert (z. B. Mikro aus), wird ein `AudioStreamEnd`-Event gesendet, um gepufferte Audiodaten zu flushen — das deutet indirekt darauf hin, dass die VAD-Logik auf **Wanduhr-Timing**, nicht auf Audio-interne Zeitstempel reagiert. Wird vorab aufgenommenes Audio schneller als Echtzeit in den Socket geschrieben, kommen die Bytes schneller an, als die reale Sprechzeit vergehen würde — die serverseitige VAD hätte dann faktisch weniger „Bedenkzeit" zwischen den echten (aber komprimiert gesendeten) Sprechpausen, was laut der oben zitierten 500-ms-Empfehlung plausibel zu vorzeitigen Turn-Enden führen könnte. **Das ist aber eine Ableitung aus VAD-Timing-Verhalten allgemein, kein dokumentierter Bug-Report zu genau diesem Szenario.** Empfehlung: Audio in Echtzeit-Tempo (bzw. mit realistischem Pacing/Chunking) senden, oder `automatic_activity_detection.disabled = true` setzen und Turn-Grenzen selbst per `ActivityStart`/`ActivityEnd` steuern (dokumentiertes, unterstütztes Pattern laut Live-API-Referenz).

---

## 7. Verbindungsabbrüche (WebSocket 1007/1008/1011) im Transkriptions-Kontext

**BESTÄTIGT für 1007** (siehe Punkt 1, js-genai#1212: `Modality.TEXT` + Transkriptions-Flags → sofortiger 1007-Abbruch).

**BESTÄTIGT für 1011, aber primär allgemein/nicht transkriptionsspezifisch.** Forum: [„gemini-2.5-flash-native-audio-preview-12-2025 returns code=1011 mid-turn at ~80% rate (started 2026-05-27)"](https://discuss.ai.google.dev/t/gemini-live-api-gemini-2-5-flash-native-audio-preview-12-2025-returns-code-1011-mid-turn-at-80-rate-started-2026-05-27/167186) — beschreibt einen plötzlichen Anstieg der 1011-Abbruchrate auf ~80 % ab einem konkreten Stichtag, was auf eine serverseitige Regression zu einem bestimmten Zeitpunkt hindeutet (kein Nutzerfehler). Weitere 1011-Berichte: [google/adk-python#3918](https://github.com/google/adk-python/issues/3918) (1011 während Tool-Ausführung) und [googleapis/python-genai#2238](https://github.com/googleapis/python-genai/issues/2238) — `response_modalities=[TEXT]` bei `gemini-3.1-flash-live-preview` löst **sofort** 1011 aus; dokumentierter Workaround dort: `AUDIO`-Modalität mit `output_audio_transcription` verwenden, um Text zu erhalten (suboptimal für reine Text-Use-Cases, aber funktionsfähig). Rate-Limit-bezogene Fehler laufen laut Doku separat über HTTP 429 `RESOURCE_EXHAUSTED`, nicht über WebSocket-1011 — 1011 wird als generischer „Internal Error" beschrieben, dessen Ursache (Quota, Ressourcenerschöpfung, nicht unterstütztes Modell) serverseitig meist nicht näher spezifiziert wird.

**Zu deutscher Sprache speziell:** kein eigenständiger Bug-Report zu 1007/1011 in Kombination mit `languageCode: de-DE` gefunden — nur die oben (Punkt 5) genannte Polarity-Reversal-Rate von 4,1/100 Sätzen auf Deutsch beim verwandten Translate-Modell, was eher ein Qualitäts- als ein Verbindungsproblem ist.

---

## Offen / unbelegt

- Kein direkter Beleg für: stumme Ablehnung des Setups (kein `setupComplete`, Verbindungsschluss ohne Fehlertext) speziell bei `speechConfig.languageCode` + `systemInstruction` an `*-transcribe-live`-Modelle. Nächstliegende Befunde deuten eher auf „wird ignoriert, Verbindung bleibt offen" (Punkt 5) oder „expliziter 1007-Fehler mit Reason-Text" (Punkt 1/7) hin — beides widerspricht der Erwartung eines *stillen* Abbruchs.
- Kein direkter Beleg für: automatische VAD beendet Turn vorzeitig speziell bei vorab aufgenommenem, schneller-als-Echtzeit gesendetem Audio. Nur indirekt über allgemeines VAD-Timing-Verhalten und die 500-ms-Schwellenempfehlung plus das Turn-Thrashing-Issue #1262 gestützt (Punkt 6).
- Keine dokumentierten Free-Tier-spezifischen Rate-Limit-Fehlerberichte für Live-Transkription gefunden (nur allgemeine 429/1011-Diskussionen ohne Tier-Bezug).
- Rechtschreibqualität/Genauigkeit speziell für deutsche Live-Transkription (`gemini-3.5-transcribe-live`) wurde in keinem gefundenen Issue explizit thematisiert — nur die Polarity-Reversal-Rate beim verwandten Translate-Modell.

## Quellenliste

- https://github.com/googleapis/python-genai/issues/1279
- https://github.com/googleapis/js-genai/issues/1212
- https://github.com/googleapis/js-genai/issues/478
- https://github.com/google-gemini/cookbook/issues/951
- https://github.com/googleapis/js-genai/issues/1429
- https://github.com/googleapis/python-genai/issues/2117
- https://github.com/googleapis/js-genai/issues/707
- https://github.com/livekit/agents/issues/5742
- https://discuss.ai.google.dev/t/gemini-live-api-models-inputtranscription-hallucinations/107899
- https://github.com/google-gemini/cookbook/issues/1262
- https://discuss.ai.google.dev/t/gemini-3-5-live-translate-preview-systeminstruction-is-accepted-and-silently-ignored-plus-polarity-reversals-on-long-sentences/179912
- https://discuss.ai.google.dev/t/gemini-live-api-gemini-2-5-flash-native-audio-preview-12-2025-returns-code-1011-mid-turn-at-80-rate-started-2026-05-27/167186
- https://github.com/google/adk-python/issues/3918
- https://github.com/googleapis/python-genai/issues/2238
- https://discuss.ai.google.dev/t/conflicting-definitions-of-prefixpaddingms-in-the-gemini-live-api-documentation/179129
- https://ai.google.dev/gemini-api/docs/live-api/live-transcribe
