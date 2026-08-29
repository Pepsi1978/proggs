# R4 — Semantik der Transkript-Felder in `serverContent` (Gemini Live API, `bidiGenerateContent`)

Stand der Recherche: 29.08.2026. Primärquelle für die Feld-Definitionen war der aktuelle Quellcode des offiziellen JS-SDK (`googleapis/js-genai`, Datei `src/types.ts`, `main`-Branch) — das ist zugleich die genaueste öffentlich einsehbare Abbildung des `v1beta`-Protos, da die reine Proto-Referenzseite (`ai.google.dev/api/live`) inhaltlich mit den SDK-Kommentaren übereinstimmt, aber weniger vollständig ist.

## 1. `inputTranscription` vs. `interimInputTranscription` — offizieller Unterschied

**BESTÄTIGT.** Wörtliches Zitat aus `googleapis/js-genai/src/types.ts` (Interface `LiveServerContent`):

```
/** Input transcription. The transcription is independent to the model
    turn which means it doesn't imply any ordering between transcription and
    model turn. */
inputTranscription?: Transcription;
/** Low latency transcription updated while the user is speaking. */
interimInputTranscription?: Transcription;
```

`interimInputTranscription` ist also explizit als *Low-Latency-Zwischenergebnis* dokumentiert, gedacht für Live-Untertitel/UI-Feedback während der Nutzer noch spricht. `inputTranscription` ist das (aus Serversicht) reguläre Transkriptions-Feld, dokumentiert als *unabhängig vom Model-Turn* — d. h. es gibt laut Spezifikation **keine garantierte Reihenfolge** zwischen Transkriptions-Nachrichten und den `modelTurn`-Content-Nachrichten desselben Austauschs. Beide Felder sind vom Typ `Transcription` (siehe unten).
Quelle: https://raw.githubusercontent.com/googleapis/js-genai/main/src/types.ts (Zeilen ~7694–7719) sowie inhaltlich deckungsgleich https://ai.google.dev/api/live und https://ai.google.dev/gemini-api/docs/live-api/live-transcribe

## 2. Kumulativ oder inkrementell?

**TEILWEISE BESTÄTIGT, TEILWEISE VERMUTUNG.** Die offizielle Doku legt explizit **nicht** fest, ob `interimInputTranscription.text` bei jedem Frame den kompletten bisherigen Text enthält (kumulativ) oder nur das neue Stück (inkrementell). Der Live-Transcribe-Guide beschreibt Interim-Ergebnisse nur als "low-latency, speculative partial hypotheses updated while the speaker is actively talking" — das deutet auf laufend überschriebene/aktualisierte Hypothesen hin, ist aber keine explizite Aussage zu "kumulativ".
Quelle: https://ai.google.dev/gemini-api/docs/live-api/live-transcribe

Der von euch **gemessene Befund** (kumulativer, wachsender Text inkl. rückwirkender Großschreibungs-Korrekturen bei `interimInputTranscription`, während `inputTranscription` genau einmal mit dem kompletten Endtext kam) deckt sich mit einem klar dokumentierten **Verhaltenswechsel neuerer Modelle**: Im Google-AI-Developer-Forum berichtet ein Nutzer, dass `inputTranscription` bei Gemini 2.5 noch inkrementell in vielen kleinen Chunks kam (z. B. `" Ho"`, `"w"`, `" are"`, `" you"`), während es bei neueren Flash-3.x-Live-Modellen **erst nach dem vollständigen Ende der Nutzer-Äußerung als ein einziges Frame mit dem Gesamttext** eintrifft. Eine offizielle Google-Stellungnahme dazu fehlt im Thread — das ist also ein **Community-Report ohne Google-Bestätigung**, deckt sich aber exakt mit eurem Messergebnis (genau ein `inputTranscription` mit vollem Text, sowohl bei 36,7 s als auch bei 64,5 s Audio).
Quelle: https://discuss.ai.google.dev/t/gemini-live-flash-3-1-api-inputtranscription-no-longer-streams-incrementally/136977

**Einordnung:** Für `gemini-3.5-transcribe-live` (euer Modell) ist am plausibelsten, dass es dieses neuere Verhalten übernimmt — `interimInputTranscription` kumulativ/laufend aktualisiert als reine Vorschau, `inputTranscription` als einmaliges finales Gesamt-Frame pro Sprecheinheit. Eine wortwörtliche Modell-spezifische Doku-Aussage dazu wurde nicht gefunden (VERMUTUNG, aber stark durch Messung + Forum-Report gestützt).

## 3. Gibt es ein `finished`-Flag? — Ja, aber es ist nachweislich kaputt

**BESTÄTIGT.** Der `Transcription`-Typ selbst hat laut Proto/SDK-Schema ein `finished`-Feld:

```
/** Audio transcription in Server Content. */
export declare interface Transcription {
  /** Optional. Transcription text. */
  text?: string;
  /** Optional. The bool indicates the end of the transcription. */
  finished?: boolean;
  /** The BCP-47 language code of the transcription. */
  languageCode?: string;
  /** A label identifying the speaker of this audio segment (e.g. "spk_1", "spk_2"). */
  speakerLabel?: string;
  /** Detailed word-level transcriptions and timing details. */
  words?: WordInfo[];
}
```
Quelle: https://raw.githubusercontent.com/googleapis/js-genai/main/src/types.ts (Zeilen ~1936–1947)

Das `finished`-Feld ist also konzeptionell genau das gesuchte "Ende-des-Segments"-Merkmal — **wird aber vom Server in der Praxis nicht befüllt.** Zwei unabhängige, noch offene bzw. bewusst nicht behobene Bug-Reports der offiziellen SDKs bestätigen das:

- `googleapis/python-genai#1504` ("Input and Output Transcription Always Returning 'Finished' as Null"): `finished` liefert bei sowohl `input_transcription` als auch `output_transcription` durchgängig `None` statt `true` am letzten Chunk. Status: **geschlossen als "not planned"**, Priorität P3. Kein offizieller Fix zugesagt.
  Quelle: https://github.com/googleapis/python-genai/issues/1504
- `googleapis/js-genai#1429` ("Transcription finished flag never updates in Javascript SDK"), getestet mit `gemini-2.5-flash-native-audio-preview-12-2025`, SDK `@google/genai` v1.44.0: `finished` erscheint **nie**, weder bei `inputTranscription` noch bei `outputTranscription`. Der Melder dokumentiert als Workaround genau das, was ihr auch beobachtet habt: Input-Text-Fragmente akkumulieren, bis die erste Output-Transkription eintrifft, und den Output-Puffer bei `turnComplete: true` leeren. Status: **offen**, Priorität P2, Stand 23.03.2026.
  Quelle: https://github.com/googleapis/js-genai/issues/1429

**Das erklärt direkt euren Messbefund:** Dass bei euch kein `finished`-Flag auftauchte, ist kein Messfehler, sondern der aktuell (Stand 29.08.2026) bekannte, unbehobene Zustand der API — `finished` existiert im Schema, ist aber serverseitig funktionslos. Als Abschluss-Signal taugt es aktuell nicht.

## 4. `turnComplete` vs. `generationComplete` — Unterschied und Zuverlässigkeit

**BESTÄTIGT**, wörtliches Zitat aus `LiveServerContent` in `types.ts`:

```
/** If true, indicates that the model is done generating. Generation will
    only start in response to additional client messages. Can be set
    alongside `content`, indicating that the `content` is the last in
    the turn. */
turnComplete?: boolean;

/** If true, indicates that the model is done generating. When model is
    interrupted while generating there will be no generation_complete message
    in interrupted turn, it will go through interrupted > turn_complete.
    When model assumes realtime playback there will be delay between
    generation_complete and turn_complete that is caused by model
    waiting for playback to finish. If true, indicates that the model
    has finished generating all content. This is a signal to the client
    that it can stop sending messages. */
generationComplete?: boolean;
```
Quelle: https://raw.githubusercontent.com/googleapis/js-genai/main/src/types.ts (Zeilen ~7690–7707)

Kurz: `generationComplete` = das Modell ist fertig mit dem **Erzeugen** des Inhalts (reines Content-Generierungs-Ende). `turnComplete` = der gesamte **Turn** ist abgeschlossen und der Server erwartet erst wieder Client-Input, bevor eine neue Generierung startet. Beide Ereignisse fallen **nicht zwingend zusammen**: Bei aktivierter Realtime-Playback-Steuerung wartet `turnComplete` zusätzlich, bis die (simulierte) Wiedergabe fertig ist — es kann also eine Verzögerung zwischen beiden geben. Wird die Generierung durch den Nutzer unterbrochen, entfällt `generationComplete` komplett; der Ablauf ist dann `interrupted` → `turnComplete`, ohne `generationComplete` dazwischen.

Zur **Reihenfolge relativ zur Output-Transkription** gibt die Proto-Referenzseite (ai.google.dev/api/live) explizit an: *"The last output transcription of this turn is sent before either `generationComplete` or `interrupted`, which in turn are followed by `turnComplete`."* Für `outputTranscription` gibt es also eine dokumentierte Ordnungsgarantie relativ zu den Abschluss-Flags. Für `inputTranscription` existiert **keine** vergleichbare explizite Garantie in der Doku — dort steht nur die allgemeine Aussage, dass Input-Transkription unabhängig vom Model-Turn ist.
Quelle: https://ai.google.dev/api/live

Zur **Zuverlässigkeit**: Ein offener SDK-Issue (`googleapis/js-genai#707`) berichtet, dass `turnComplete` bei manchen Sessions mit `gemini-2.5-flash-preview-native-audio-dialog` **vorzeitig** eintrifft, obwohl der Inhalt inhaltlich unvollständig wirkt (ohne begleitendes `interrupted`-Flag) — ein Hinweis, dass die Abschluss-Signale in Audio-Response-Szenarien nicht absolut robust sind. Für Transkriptions-Ordnungsfragen ist das nur mittelbar relevant, zeigt aber, dass man sich bei zeitkritischer Logik nicht blind auf ein einzelnes Flag verlassen sollte.
Quelle: https://github.com/googleapis/js-genai/issues/707

**Zusatzfund (nicht explizit angefragt, aber relevant):** Der aktuelle Schema-Stand enthält inzwischen weitere, neuere Felder in `LiveServerContent`, die bei der Diagnose von Abschlussverhalten helfen können: `turnCompleteReason` (Enum: u. a. `MALFORMED_FUNCTION_CALL`, `RESPONSE_REJECTED`, "needs more input from user" …), `waitingForInput` (Modell wartet auf weiteren Nutzer-Input, ohne dass die Generierung tatsächlich fertig ist) und `interactionStatus` ("Always sent alongside `turn_complete`"). Diese sind über die Standard-Live-Transcribe-Doku hinaus dokumentiert nur im SDK-Quellcode.
Quelle: https://raw.githubusercontent.com/googleapis/js-genai/main/src/types.ts (Zeilen ~7719–7727, Enum ab Zeile ~1526)

## 5. Mehrere aufeinanderfolgende `inputTranscription`-Segmente bei langer Aufnahme?

**VERMUTUNG, mit Vorsicht zu behandeln.** Offizielle Doku sagt dazu nichts Explizites (weder "genau eins pro Turn" noch "mehrere möglich"). Zwei indirekte Hinweise:

- Euer eigener Messbefund (36,7 s → 1× `inputTranscription`; 64,5 s → 1× `inputTranscription`) spricht dafür, dass bei `gemini-3.5-transcribe-live` **ein finales Segment pro zusammenhängender Sprechphase** (bis zur nächsten VAD-Pause bzw. bis `turnComplete`) normal ist — nicht pro festem Zeitfenster.
- Das offizielle Modellblatt zu `gemini-3.5-transcribe-live` nennt aber eine **Sessiondauer-Obergrenze von 10 Minuten pro Session**. Für Aufnahmen über 10 Minuten muss also ohnehin die Verbindung neu aufgebaut werden — dort entstehen zwangsläufig mehrere, über Session-Grenzen hinweg getrennte `inputTranscription`-Blöcke, die man anwendungsseitig aneinanderhängen muss.
  Quelle: https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe

Ein **Warnsignal gegen die Annahme "immer genau ein sauberes finales Frame"** liefert `google-gemini/cookbook#1197` (getestet mit `gemini-3.1-flash-live-preview`, nicht dem Transcribe-Modell, aber gleiche Server-Infrastruktur): Bei Unterbrechungen mitten im Satz wird `outputTranscription` **wortweise über mehrere `serverContent`-Nachrichten fragmentiert** (Beispiel: "Take care." kommt zerstückelt als u. a. `"car"`, `"e"`). Zusätzlich berichtet derselbe Issue, dass `inputTranscription` und `outputTranscription` bei Unterbrechungen **im selben `serverContent`-Frame, nur ~10 ms auseinander**, eintreffen können — was die Zuordnung "wer hat wann gesprochen" erschwert. Übertragen auf Input-Transkription bedeutet das: **Auch wenn "ein finales Frame pro Segment" der Normalfall ist, darf man sich darauf nicht als Garantie verlassen** — insbesondere in Interrupt-/Overlap-Szenarien ist Fragmentierung über mehrere Nachrichten dokumentiert real.
Quelle: https://github.com/google-gemini/cookbook/issues/1197

Eine sichere Methode, um "neuer Abschnitt" von "Fortschreibung desselben Abschnitts" zu unterscheiden, ist in keiner Quelle explizit dokumentiert (kein Sequenz-Zähler, keine Segment-ID am `Transcription`-Typ — er hat nur `text`, `finished` (kaputt), `languageCode`, `speakerLabel`, `words`). Am ehesten geeignet als Trenner sind demnach ausschließlich die **Nachrichtenfolge selbst**: ein neues `inputTranscription`-Frame nach einem vorherigen `inputTranscription`-Frame (nicht nach `interimInputTranscription`) markiert praktisch einen neuen finalen Block, weil `inputTranscription` laut bisheriger Beobachtung immer den kompletten Text des jeweiligen Segments trägt und nicht fortlaufend wächst.

## 6. `outputTranscription` — wofür

**BESTÄTIGT.** `outputTranscription` ist die Transkription der vom Modell **gesprochenen Audio-Antwort** (nicht der Nutzereingabe) — Teil des "Generation output" des Servers, thematisch unabhängig vom `inputTranscription`-Feld. Für sie gilt (siehe Abschnitt 4) die dokumentierte Ordnungsgarantie: das letzte `outputTranscription`-Frame eines Turns kommt vor `generationComplete`/`interrupted`, gefolgt von `turnComplete`.
Quelle: https://ai.google.dev/api/live, https://raw.githubusercontent.com/googleapis/js-genai/main/src/types.ts

## Empfehlung zum verlustfreien Zusammensetzen des Gesamttexts (belegt + eigene Ableitung)

Basierend auf den obigen Belegen — insbesondere dem kaputten `finished`-Flag (#1504, #1429) und der dokumentierten Fragmentierungsgefahr bei Interrupts (#1197) — ist folgender Ansatz am robustesten:

1. **`interimInputTranscription` niemals aufsummieren/anhängen** — nur als Wegwerf-Vorschau für Live-UI verwenden (jedes Frame ersetzt die Anzeige komplett), da der Text laut Messung kumulativ und rückwirkend korrigiert ankommt.
2. **Nur `inputTranscription`-Frames zum Endergebnis konkatenieren**, in Empfangsreihenfolge, jeweils mit Leerzeichen-Trennung falls nötig — nicht auf `finished` warten (es kommt nicht zuverlässig).
3. **Abschluss der Aufnahme/Session** als Stopp-Signal nehmen (z. B. `turnComplete`, Session-Ende oder euer eigenes `generationComplete`-Signal wie in eurem Log beobachtet), nicht ein Transkriptions-internes Flag.
4. Für Aufnahmen über ~10 Minuten: Session-Neuaufbau einplanen und die Teil-Transkripte über Sessions hinweg aneinanderhängen (siehe Sessiondauer-Limit oben).
5. Defensiv bleiben: In Interrupt-nahen Szenarien mit mehreren schnell aufeinanderfolgenden `inputTranscription`/`outputTranscription`-Frames im selben Zeitfenster rechnen (Beleg #1197) — nicht blind annehmen, dass nach dem ersten `inputTranscription` garantiert Ruhe ist, bevor `turnComplete`/`generationComplete` kommt.

## Offen / unbelegt

- Ob `interimInputTranscription.text` *offiziell dokumentiert* kumulativ ist (nur indirekt über Formulierung "partial hypotheses" und eure Messung gestützt).
- Ob `gemini-3.5-transcribe-live` explizit denselben "ein-Frame-am-Ende"-Wechsel vollzogen hat wie die in Abschnitt 2 zitierten Flash-3.1-Native-Audio-Modelle (keine modell-spezifische Doku-Aussage gefunden, nur Analogieschluss).
- Ob es bei `gemini-3.5-transcribe-live` (reines Transkriptions-Modell ohne Audio-Antwort) überhaupt `outputTranscription`/`turnComplete`/`generationComplete` im klassischen Sinn gibt, oder ob dort andere Abschluss-Semantik gilt (die Modellseite nennt kein spezifisches Abschluss-Event für reine Transkription).
- Kein Beleg für ein explizites Sequenz-/Segment-Identifikationsfeld am `Transcription`-Typ (existiert nicht laut Schema).

## Quellenliste

- https://raw.githubusercontent.com/googleapis/js-genai/main/src/types.ts (Primärquelle: SDK-Typdefinitionen, deckt Proto ab)
- https://ai.google.dev/api/live
- https://ai.google.dev/gemini-api/docs/live-api/live-transcribe
- https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe
- https://discuss.ai.google.dev/t/gemini-live-flash-3-1-api-inputtranscription-no-longer-streams-incrementally/136977
- https://github.com/googleapis/python-genai/issues/1504
- https://github.com/googleapis/js-genai/issues/1429
- https://github.com/googleapis/js-genai/issues/707
- https://github.com/google-gemini/cookbook/issues/1197
- https://docs.livekit.io/agents/models/stt/gemini/ (ergänzend, keine protokollrelevanten Neuigkeiten)
