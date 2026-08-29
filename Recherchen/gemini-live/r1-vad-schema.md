# R1 — Voice-Activity-Detection (VAD): vollständiges JSON-Schema und Parameter

## Struktur

`setup.realtimeInputConfig` ist eine `RealtimeInputConfig`-Nachricht mit drei Feldern:

- `automaticActivityDetection` (Nachricht `AutomaticActivityDetection`)
- `activityHandling` (Enum `ActivityHandling`)
- `turnCoverage` (Enum `TurnCoverage`)

Quelle: offizielle REST/WebSocket-Referenz. **BESTÄTIGT**
https://ai.google.dev/api/live

## Felder von `AutomaticActivityDetection` (BESTÄTIGT)

Aus der offiziellen API-Referenz (`RealtimeInputConfig.AutomaticActivityDetection`):

| Feld | Typ | Beschreibung (Original) |
|---|---|---|
| `disabled` | `bool` | "If enabled (the default), detected voice and text input count as activity. If disabled, the client must send activity signals." |
| `startOfSpeechSensitivity` | Enum `StartSensitivity` | "Determines how likely speech is to be detected." |
| `prefixPaddingMs` | `int32` | "The required duration of detected speech before start-of-speech is committed." |
| `endOfSpeechSensitivity` | Enum `EndSensitivity` | "Determines how likely detected speech is ended." |
| `silenceDurationMs` | `int32` | "The required duration of detected non-speech (e.g. silence) before end-of-speech is committed." |

Ergänzende Formulierung aus dem Capabilities-Guide (identischer Wortlaut zur Proto-Doku):
- `prefixPaddingMs`: "The lower this value, the more sensitive the start-of-speech detection is and shorter speech can be recognized."
- `silenceDurationMs`: "The larger this value, the longer speech gaps can be without interrupting the user's activity."

Quellen: https://ai.google.dev/api/live , https://ai.google.dev/api/live.md.txt , https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1beta/generative_service.proto

## Offizielles Beispiel-JSON mit den Default-Werten (BESTÄTIGT)

Aus dem Live-API-Capabilities-Guide, Python- und JS-Beispiel (Kommentar `# default` steht im Original direkt bei `disabled`, die anderen Werte sind die im Beispiel gezeigten Defaults):

```
"automaticActivityDetection": {
  "disabled": false,                                            // default
  "startOfSpeechSensitivity": "START_SENSITIVITY_LOW",
  "endOfSpeechSensitivity": "END_SENSITIVITY_LOW",
  "prefixPaddingMs": 20,
  "silenceDurationMs": 100
}
```

Quelle: https://ai.google.dev/gemini-api/docs/live-api/capabilities (Rohtext via `.md.txt`-Variante gegengeprüft)

Das heißt: **Der Server-Default für `silenceDurationMs` liegt bei 100 ms** — deutlich kürzer, als man bei einem 12-Sekunden-Pausen-Fall vermuten würde. Das erklärt zwanglos, warum bei Standard-VAD nach 3,4 s Sprechpause bereits `generationComplete` kam.

Achtung: Eine per WebSearch gefundene Drittquelle (Blog/Community, nicht verifizierbar zitierfähig) behauptete einen internen Default von "~800 ms" und einen "empfohlenen Bereich 500–800 ms". Das steht im Widerspruch zum offiziellen Beispiel-JSON (100 ms) und konnte an keiner Primärquelle bestätigt werden — **als VERMUTUNG/unbelegt einstufen, nicht dem 100-ms-Wert aus der offiziellen Doku vorziehen.**

## Sensitivity-Enums (BESTÄTIGT)

Aus dem Proto (`generative_service.proto`, Nachrichten `StartSensitivity` / `EndSensitivity` innerhalb `AutomaticActivityDetection`):

```
enum StartSensitivity {
  START_SENSITIVITY_UNSPECIFIED = 0;  // Default ist START_SENSITIVITY_HIGH
  START_SENSITIVITY_HIGH = 1;         // erkennt Sprechbeginn öfter
  START_SENSITIVITY_LOW = 2;          // erkennt Sprechbeginn seltener
}

enum EndSensitivity {
  END_SENSITIVITY_UNSPECIFIED = 0;    // Default ist END_SENSITIVITY_HIGH
  END_SENSITIVITY_HIGH = 1;           // beendet Sprache öfter
  END_SENSITIVITY_LOW = 2;            // beendet Sprache seltener
}
```

Es gibt **nur zwei echte Stufen (HIGH/LOW) plus UNSPECIFIED** — **kein** `MEDIUM`. Eine WebSearch-Zusammenfassung behauptete fälschlich einen dritten Wert `START_SENSITIVITY_MEDIUM` — das ist in Proto, offizieller API-Referenz und dem python-genai-Quellcode (Docstring bestätigt nur HIGH/LOW) **nicht vorhanden**. Diese Behauptung wird hiermit als falsch/unbelegt zurückgewiesen.

Wichtig zum Default-Widerspruch: Proto-Kommentar sagt UNSPECIFIED-Default = HIGH; der python-genai-Docstring differenziert nach Plattform: "START_SENSITIVITY_LOW für Gemini Enterprise Agent Platform und START_SENSITIVITY_HIGH für Gemini Live" (analog bei EndSensitivity umgekehrt: LOW für Enterprise, HIGH für Live). Das oben zitierte Beispiel-JSON aus dem Capabilities-Guide setzt beide explizit auf LOW — das ist dort ein demonstrierter Beispielwert, nicht zwangsläufig der aktive Default bei weggelassenem Feld.

Quellen: https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1beta/generative_service.proto , https://github.com/googleapis/python-genai/blob/main/google/genai/types.py

## `activityHandling` (BESTÄTIGT)

```
enum ActivityHandling {
  ACTIVITY_HANDLING_UNSPECIFIED = 0;    // Default-Verhalten = START_OF_ACTIVITY_INTERRUPTS
  START_OF_ACTIVITY_INTERRUPTS = 1;     // Sprechbeginn unterbricht die Modellantwort (Barge-in)
  NO_INTERRUPTION = 2;                  // Modellantwort wird nicht unterbrochen
}
```

Quelle: https://ai.google.dev/api/live

## `turnCoverage` (BESTÄTIGT)

```
enum TurnCoverage {
  TURN_COVERAGE_UNSPECIFIED = 0;                    // Default hängt vom Modell ab
  TURN_INCLUDES_ONLY_ACTIVITY = 1;                  // nur Aktivität seit letztem Turn, Stille ausgeschlossen
  TURN_INCLUDES_ALL_INPUT = 2;                      // alles inkl. Stille
  TURN_INCLUDES_AUDIO_ACTIVITY_AND_ALL_VIDEO = 3;   // Audio-Aktivität + alles Video
}
```

python-genai-Docstring präzisiert den modellabhängigen Default: **Gemini 2.5 → `TURN_INCLUDES_ONLY_ACTIVITY`**, **Gemini 3.1 → `TURN_INCLUDES_AUDIO_ACTIVITY_AND_ALL_VIDEO`** (Default). Für `gemini-3.5-transcribe-live` selbst wurde der konkrete Default nirgends explizit dokumentiert gefunden — **unbelegt für dieses spezielle Modell**, nur die generelle 2.5-vs-3.1-Regel ist belegt.

Quellen: https://ai.google.dev/api/live , https://github.com/googleapis/python-genai/blob/main/google/genai/types.py

## Manuelle Aktivitätssignale bei deaktivierter VAD (BESTÄTIGT)

- Ist `disabled: true` gesetzt, muss der Client `activityStart` und `activityEnd` senden. Referenztext: "This can only be sent if automatic (i.e. server-side) activity detection is disabled."
- Umgekehrt gilt für `audioStreamEnd`: "This should only be sent when automatic activity detection is enabled (which is the default)." — bei deaktivierter VAD ist `audioStreamEnd` also **nicht** vorgesehen/nicht nötig. Das deckt sich exakt mit dem beschriebenen Ansatz (disabled VAD + `activityStart`/`activityEnd`, ohne `audioStreamEnd`).

Quelle: https://ai.google.dev/api/live.md.txt

## Gibt es eine Obergrenze für `silenceDurationMs`? (Kernfrage)

**Nirgends dokumentiert.** Weder Proto-Kommentare noch die REST-Referenz noch der Capabilities-Guide nennen einen Maximalwert. Feldtyp ist `int32`, technisch also bis rechnerisch ca. 2,1 Mrd. ms zulässig — aber das ist reine Typgrenze, keine von Google dokumentierte/garantierte Obergrenze. **Als unbelegt/offen einstufen.**

## Wäre ein hohes `silenceDurationMs` (z. B. 30000 ms) die bessere Alternative zum Deaktivieren der VAD? (Kernfrage, teils widerlegt)

**Nein, nicht empfehlenswert — und laut mehreren Community-Berichten in der aktuellen `-live-preview`-Modellfamilie vermutlich gar nicht wirksam.** Zwei einschlägige, aktuelle GitHub-Issues (beide 2026, beide gegen die Live-Preview-Modelle der 3.x-Reihe, die technisch nah an `gemini-3.5-transcribe-live` liegt):

1. **googleapis/js-genai #1467** ("`silenceDurationMs` in `automaticActivityDetection` is ignored on `gemini-3.1-flash-live-preview`"): Bei 2 s Stille im Audio und konfiguriertem 5-Sekunden-Schwellwert wird trotzdem bei der 2-Sekunden-Lücke geschnitten. **`gemini-2.5-flash-native-audio-latest` funktioniert dagegen korrekt** mit demselben Parameter. Kein Workaround im Issue dokumentiert, keine Google-Antwort sichtbar.
   https://github.com/googleapis/js-genai/issues/1467

2. **google-gemini/cookbook #1263** ("Gemini 3.1 Flash Live Preview - STT, VAD, Silence Duration, and Tool Calling Issues"): `silenceDurationMs = 6000` konfiguriert, aber der Turn endet trotzdem nach ca. 2000 ms — der Server scheint einen internen Default statt des konfigurierten Werts anzuwenden. Status im Issue: "awaiting response", keine Lösung.
   https://github.com/google-gemini/cookbook/issues/1263

3. **google-gemini/cookbook #1262** ("VAD turn thrashing"): Selbst mit `silenceDurationMs: 3000`, `prefixPaddingMs: 500` und beiden Sensitivitäten auf LOW traten 60–160 Turn-Events/Minute statt der erwarteten 5–15 auf, inkl. Unterbrechungen mitten im Satz. Auch hier kein bestätigter Workaround, keine Google-Antwort im einsehbaren Verlauf.
   https://github.com/google-gemini/cookbook/issues/1262

**Fazit zur Kernfrage:** Diese drei Issues betreffen zwar nicht direkt `gemini-3.5-transcribe-live`, sondern die `-live-preview`-Modelle der 3.x-Generation — aber sie zeigen ein wiederkehrendes Muster: Bei den neueren Live-Modellen wird `silenceDurationMs` teils schlicht ignoriert bzw. es wird ein interner, deutlich kürzerer Default angewendet, unabhängig vom konfigurierten Wert. Ein hoch gesetztes `silenceDurationMs` (z. B. 30 s) wäre damit **kein verlässlicher Ersatz** für das vollständige Deaktivieren der VAD — es könnte schlicht wirkungslos verpuffen, genau wie bei den gemeldeten Bugs. Der bereits erprobte Weg des Nutzers (VAD komplett deaktivieren + manuelle `activityStart`/`activityEnd`-Signale) umgeht dieses Risiko strukturell, weil dabei gar keine serverseitige Stille-Uhr mehr zum Tragen kommt. Eine direkte Bestätigung, dass exakt `gemini-3.5-transcribe-live` denselben `silenceDurationMs`-Bug hat, konnte nicht gefunden werden — das bleibt **unbelegt**, ist aber angesichts der Modellfamilien-Nähe naheliegend.

## Zusätzlich gefunden: "Hybrid VAD" (BESTÄTIGT als Konzept, nicht direkt relevant für den Pausenfall)

Die offizielle Live-Transcribe-Doku beschreibt für `gemini-3.5-transcribe-live` explizit drei VAD-Strategien: automatische (Server-)VAD, "Hybrid VAD" und manuelle VAD (Push-to-Talk). Hybrid VAD kombiniert Server-VAD mit einem clientseitigen Stille-Erkenner, der proaktiv `audioStreamEnd` sendet, um die Latenz zu senken (Turn wird beendet, sobald der Client Stille erkennt, statt auf die volle `silenceDurationMs`-Wartezeit zu warten). Das ist das **Gegenteil** dessen, was der Nutzer braucht (er will lange Pausen *aushalten*, nicht früher abschneiden) — für den beschriebenen Anwendungsfall irrelevant, aber zur Vollständigkeit dokumentiert.
Quelle: https://ai.google.dev/gemini-api/docs/live-api/live-transcribe

## Offen / unbelegt

- Kein dokumentierter Maximalwert für `silenceDurationMs` (weder in Proto noch REST-Referenz noch Guides).
- Kein direkter Beleg, dass `gemini-3.5-transcribe-live` (statt der `-live-preview`-3.x-Modelle) denselben `silenceDurationMs`-Ignorier-Bug hat — nur naheliegende Vermutung aus Modellfamilien-Nähe.
- Der genaue Default von `turnCoverage` speziell für `gemini-3.5-transcribe-live` ist nicht dokumentiert.
- Die Drittquellen-Behauptung "Default ~800 ms, empfohlen 500–800 ms" für `silenceDurationMs` konnte nicht an einer Primärquelle verifiziert werden und widerspricht dem offiziellen Beispiel (100 ms) — als unbelegt/falsch einstufen.
- Die Drittquellen-Behauptung eines dritten Enum-Werts `START_SENSITIVITY_MEDIUM` ist durch Proto und SDK-Quellcode widerlegt.

## Quellenliste

- https://ai.google.dev/api/live — offizielle WebSocket-API-Referenz (BidiGenerateContentSetup, RealtimeInputConfig, AutomaticActivityDetection, Enums)
- https://ai.google.dev/api/live.md.txt — Rohtext-Variante derselben Referenz
- https://ai.google.dev/gemini-api/docs/live-api/capabilities — Capabilities-Guide mit Beispiel-JSON und Default-Werten
- https://ai.google.dev/gemini-api/docs/live-api/live-transcribe — Live-Transcribe-spezifische VAD-Strategien (inkl. Hybrid VAD)
- https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1beta/generative_service.proto — Proto-Quelldefinition
- https://github.com/googleapis/python-genai/blob/main/google/genai/types.py — Python-SDK-Typdefinitionen mit Docstrings (Plattform-abhängige Defaults)
- https://github.com/googleapis/js-genai/issues/1467 — Bug-Report: silenceDurationMs wird bei gemini-3.1-flash-live-preview ignoriert
- https://github.com/google-gemini/cookbook/issues/1263 — Bug-Report: silenceDurationMs=6000 wirkungslos, Turn endet nach ~2000ms
- https://github.com/google-gemini/cookbook/issues/1262 — Bug-Report: VAD-Turn-Thrashing trotz konservativer Settings
- https://github.com/google-gemini/gemini-skills/blob/main/skills/gemini-live-api-dev/SKILL.md — Community/Google-Skill-Datei, Erwähnung Hybrid VAD für gemini-3.5-transcribe-live
