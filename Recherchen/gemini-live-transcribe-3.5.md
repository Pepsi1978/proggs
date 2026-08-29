# Gemini Live API (bidiGenerateContent) — Sprache-zu-Text mit `gemini-3.5-transcribe-live`

**Stand:** 29.08.2026 · **Status:** Recherche laeuft (Engine B, OpenRouter `:online`, 8 parallele Researcher)
**Zweck:** Grundlage fuer die Gemini-Transkription im TerminalVoiceOverlay

> Diese Datei wird waehrend der Recherche fortlaufend ergaenzt. Abschnitte mit "(offen)" sind noch nicht befuellt.

---

## 0. Eigene Messung gegen die echte API (BESTAETIGT, selbst gemessen)

Setup: echter API-Key, WAV 16 kHz / 16 bit / mono, 36,7 s Audio mit kuenstlicher 12-Sekunden-Pause in der Mitte.

| Konfiguration | Ergebnis |
|---|---|
| Automatische Pausenerkennung (Default) | Turn nach **3,4 s** mit `generationComplete` beendet, nur **61 von 330 Zeichen** — alles nach der Pause verworfen |
| `"realtimeInputConfig":{"automaticActivityDetection":{"disabled":true}}` + `{"realtimeInput":{"activityStart":{}}}` vor und `{"realtimeInput":{"activityEnd":{}}}` nach dem Audio | **vollstaendige 330 Zeichen** ueber die Pause hinweg, `generationComplete` nach **8,9 s** |

Weitere Beobachtungen:
- `serverContent.inputTranscription` enthielt den vollstaendigen Endtext.
- `serverContent.interimInputTranscription` blieb mit 218 Zeichen zurueck und ist **kumulativ**.
- Das Modell listet als einzige unterstuetzte Methode **`bidiGenerateContent`** (kein REST-`generateContent`).

---

## 1. VAD-Schema `realtimeInputConfig.automaticActivityDetection`
(offen)

## 2. Manuelle Aktivitaetssteuerung `activityStart` / `activityEnd`
(offen)

## 3. Laengen- und Session-Limits, Session Resumption

**BESTAETIGT (offizielle Doku + Google-Engineer im Forum)**

| Limit | Wert | Quelle |
|---|---|---|
| WebSocket-Verbindungsdauer | **~10 Minuten**, danach `GoAway` und Verbindungsende | [Vertex-Doku](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/live-api/start-manage-session) |
| Session Audio-only ohne Kompression | **15 Minuten** | [Vertex-Doku](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/live-api/start-manage-session) |
| Session Audio+Video ohne Kompression | 2 Minuten | dito |
| Kontextfenster Native Audio | **128k Tokens** | [gemini-skills SKILL.md](https://github.com/google-gemini/gemini-skills/blob/main/skills/gemini-live-api-dev/SKILL.md) |
| Kontextfenster Standard | 32k Tokens | dito |
| Session-Resumption-Handle gueltig | **2 Stunden** nach Session-Ende | [ai.google.dev Session Management](https://ai.google.dev/gemini-api/docs/live-api/session-management) |

**Wichtig — die 15 Minuten sind nur eine Naeherung.** Google-Engineer Srikanta_K_N im offiziellen Forum:
> "Yes, 15 min is an approximation. It really depends on the context window size, which is, as you mentioned, 128k tokens. When the 128k context window fills up, it leads to the termination of the session."
> — [discuss.ai.google.dev](https://discuss.ai.google.dev/t/gemini-live-api-sessions-exceeding-15-minute-limit-without-compression/114104)

**Die ERSTE Grenze, die bei einem Diktat greift, ist die ~10-Minuten-Verbindungsdauer, nicht die Session-Dauer.**
Ohne Session Resumption bricht die Verbindung mitten im Diktat ab.

### Gegenmittel

1. **Session Resumption** — Feld `sessionResumption` im Setup-Config. Der Server sendet dann
   `SessionResumptionUpdate`-Nachrichten mit Tokens; der letzte Token wird als
   `SessionResumptionConfig.handle` an die Folgeverbindung uebergeben.
2. **Context Window Compression** — `contextWindowCompression: { slidingWindow: {} }` im
   `LiveConnectConfig`; die Trigger-Schwelle ist ueber `trigger_tokens` konfigurierbar.
   Erlaubt laut Doku "unlimited amount of time", verwirft aber aeltere Teile des Verlaufs.

```js
const config = {
  responseModalities: [Modality.AUDIO],
  contextWindowCompression: { slidingWindow: {} }
};
```

**NICHT dokumentiert (Luecke):**
- Ein Limit fuer die maximale zusammenhaengende Audio-Dauer eines **einzelnen Turns** — nur die
  Session-Gesamtdauer ist dokumentiert.
- Eine Token-pro-Sekunde-Rate fuer Live-Audio, mit der man aus 128k Tokens die maximale
  Audio-Dauer exakt berechnen koennte.

### Bewertung fuer ein Diktat von 5-15 Minuten

| Diktatlaenge | Greifendes Limit | Konsequenz |
|---|---|---|
| bis ~10 min | Verbindungsdauer | stabil |
| ~10-15 min | Verbindungsdauer (~10 min) zuerst | `GoAway`, Session endet ohne Resumption |
| ueber ~15 min / Token-Budget voll | 128k-Kontext | Session terminiert ohne Compression |


## 4. Schneller-als-Echtzeit-Streaming, Chunk-Groesse, Sendetakt
(offen)

## 5. Transkript-Felder: `inputTranscription` vs. `interimInputTranscription`
(offen)

## 6. Bekannte Bugs
(offen)

## 7. Modell `gemini-3.5-transcribe-live`
(offen)

## 8. Lautstaerke / Audioqualitaet
(offen)

## 9. Empfehlung fuer das TerminalVoiceOverlay
(offen)
