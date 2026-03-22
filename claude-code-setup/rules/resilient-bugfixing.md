# Resilient Bugfixing & Proaktive Intelligenz-Steigerung (KRITISCH)

## Regel: Jeder Bug wird zum permanenten System-Upgrade

Wenn ein Fehler gefunden und gefixt wird, ist der Fix NICHT fertig, bis er **zukunftssicher** ist.
Ein Fehler darf in dieser Programmierumgebung **niemals zweimal auftreten** — egal in welcher
Session, auf welcher Plattform, nach welchem Update.

## Proaktive Intelligenz-Vorschlaege (PFLICHT in jeder Session)

Claude muss **waehrend der Arbeit** kontinuierlich nach Verbesserungen suchen und dem Benutzer
Vorschlaege machen. Nicht nur wenn Fehler auftreten, sondern IMMER:

### Wann und wo Vorschlaege machen?
- **Nur AM ENDE der Aufgabe** — NIEMALS mittendrin waehrend der Arbeit.
- Der Vorschlag kommt NACH der Status-Meldung ("Committed, gepusht und plattformuebergreifend.").
- **Nur wenn es einen ECHTEN Mehrwert gibt** — keinen Vorschlag erzwingen wenn nichts auffaellt.
- Kein Vorschlag ist besser als ein banaler Vorschlag.

### Wann ist ein Vorschlag sinnvoll?
- Zwischenfehler in der Session die spaeter gefixt wurden → Praevention vorschlagen
- Wiederkehrende manuelle Schritte → Automatisierung vorschlagen
- Fehlende Absicherung (keine Tests, kein Retry, kein Fallback) → Haertung vorschlagen
- Ineffizienzen (zu viele Versuche, zu lange Wartezeit) → Optimierung vorschlagen
- Neue Muster erkannt die das System schlauer machen koennten

### Format
Kurz, klar, am Ende der Antwort, maximal 2-3 Saetze:
```
💡 **Intelligenz-Vorschlag**: [Was verbessert werden kann]
   → [Konkreter Vorschlag] — Soll ich das umsetzen?
```

## Pflicht-Ablauf bei JEDEM Bugfix

### 1. Root Cause finden (nicht nur Symptom fixen)
- Den eigentlichen Ausloeser identifizieren, nicht nur das sichtbare Problem
- Frage: "Was ist die TIEFSTE Ursache?" — mindestens 3x "Warum?" fragen
- Beispiel: Hook-Fehler → Symptom ist der Fehler, Root Cause ist die Race Condition

### 2. Verwandte Fehlerquellen suchen (PFLICHT)
- **Gleiche Klasse**: Kann der gleiche Fehlertyp an anderer Stelle auftreten?
  (z.B. Race Condition bei claude-mem → gibt es andere Plugins mit dem gleichen Muster?)
- **Gleiche Komponente**: Kann die betroffene Komponente auf andere Weise versagen?
  (z.B. Port-Konflikt → was passiert bei Timeout, bei Crash, bei Update?)
- **Gleiche Abhaengigkeit**: Welche anderen Teile haengen von der gleichen Sache ab?
  (z.B. Worker-Service → wer braucht den noch? Was wenn er spaeter crasht?)

### 3. Zukunftssicheren Fix implementieren
Der Fix muss diese Eigenschaften haben:

| Eigenschaft | Bedeutung |
|-------------|-----------|
| **Self-Healing** | Repariert sich selbst nach Updates, Neustarts, Plattformwechsel |
| **Defensiv** | Faengt nicht nur den bekannten Fehler ab, sondern die ganze Fehlerklasse |
| **Ueberlebbar** | Uebersteht Plugin-Updates, Config-Aenderungen, System-Upgrades |
| **Erweiterbar** | Kann fuer zukuenftige aehnliche Faelle erweitert werden (z.B. Service Registry) |
| **Dokumentiert** | Memory-Eintrag erklaert Ursache, Fix und wie man neue Faelle hinzufuegt |
| **Schadensfrei** | Der Fix selbst darf KEINE neuen Fehler einfuehren (siehe Schritt 3b) |

### 3b. Fix-Induced-Failure-Pruefung (PFLICHT — VOR dem Commit)
**Ein Fix der neue Probleme verursacht ist SCHLIMMER als kein Fix.**
Vor JEDEM Commit eines Bugfixes MUSS geprueft werden:

| Pruefung | Frage | Beispiel |
|----------|-------|----------|
| **Abhaengigkeiten** | Was haengt vom geaenderten Code ab? | Launcher-Script aendern → launchd-plist testen |
| **Fehlszenarien** | Was passiert wenn der Fix-Code selbst fehlschlaegt? | Health-Check crasht → blockiert er die Session? (Muss exit 0 sein!) |
| **Zustandsaenderungen** | Aendert der Fix einen Systemzustand dauerhaft? | launchd-Agent → was wenn Plugin deinstalliert wird? (Crash-Loop!) |
| **Race Conditions** | Kann der Fix mit anderem Code kollidieren? | Guard + Plugin-Hook starten Worker → doppelter Start? |
| **Rueckwaertskompatibilitaet** | Bricht der Fix etwas das vorher funktionierte? | Neue Imports in index.ts → existieren die Module? |
| **Plattform-Effekte** | Funktioniert der Fix auf macOS UND Windows? | .sh-Hook → braucht es ein .ps1-Gegenstueck? |
| **Update-Resistenz** | Ueberlebt der Fix das naechste Plugin/CLI/OS-Update? | Hardcoded Pfade → dynamisch aufloesen |
| **Graceful Degradation** | Was wenn eine Voraussetzung fehlt? | Bun nicht installiert → auf Node.js zurueckfallen, nicht crashen |

**Faustregel**: Der Fix muss die Frage bestehen: "Was ist das Schlimmste das passieren kann
wenn ich diesen Fix deploye und dann 6 Monate lang nicht hinschaue?"

### 4. Mehrere Absicherungsschichten (Defense in Depth)
Nie nur EINE Absicherung. Immer mindestens 2-3 Schichten:
- Schicht 1: Praeventiv (Problem verhindern bevor es auftritt)
- Schicht 2: Reaktiv (Problem abfangen wenn es doch auftritt)
- Schicht 3: Selbstheilend (Fix automatisch wiederherstellen nach Updates)
- Schicht 4: Upstream (Bug beim Verursacher melden fuer permanenten Fix)

### 5. Memory speichern
- Feedback-Memory mit: Was war der Fehler, was war die Root Cause, was ist der Fix
- Muster-Erkennung: "Wenn ich in Zukunft [Muster X] sehe, muss ich [Check Y] machen"

## Beispiel: claude-mem Hook-Fehler (2026-03-22)

| Schritt | Was gemacht wurde |
|---------|-------------------|
| Root Cause | Race Condition: MCP-Server und Hook starten Worker gleichzeitig |
| Verwandte Fehler | Alle Plugins mit Daemon/Worker/Server-Start geprueft |
| Self-Healing | Auto-Patcher repariert hooks.json nach jedem Plugin-Update |
| Defensiv | Service Registry fuer ALLE zukuenftigen Service-Plugins |
| Mehrere Schichten | Guard-Hook + Auto-Patcher + Plugin-Patch + Upstream-Issue |
| Memory | Feedback-Memory mit Muster-Erkennung gespeichert |

## Beispiel: Fix-Induced-Failure vermieden (2026-03-22)

| Pruefung | Problem erkannt | Massnahme |
|----------|----------------|-----------|
| Zustandsaenderung | launchd-Agent wuerde bei Plugin-Deinstallation Crash-Loop ausloesen | Launcher wartet geduldig statt exit 1, schlaeft 5min vor Retry |
| Graceful Degradation | Bun koennte fehlen oder Pfad sich aendern | Multi-Path-Suche + Fallback auf Node.js |
| Fehlszenario | Health-Check-Hook koennte crashen und Session blockieren | Immer exit 0, set +e, Fallback-Logger |
| Race Condition | Worker bereits laufend → doppelter Start | Pre-Flight-Check via Health-Endpoint |
| Rueckwaertskompatibilitaet | Neue Module (db-state.ts) muessen committed sein | Sofort ins Repo committed, nie nur lokal |

## Was NIEMALS passieren darf
- ❌ Nur das Symptom fixen ohne Root Cause zu verstehen
- ❌ Fix der bei naechstem Plugin-Update oder Neustart kaputt geht
- ❌ Fix nur fuer den einen konkreten Fall, ohne aehnliche Faelle zu pruefen
- ❌ Fix ohne Memory-Eintrag (Wissen geht verloren zwischen Sessions)
- ❌ "Funktioniert jetzt" sagen ohne zu pruefen ob es in 2 Wochen noch funktioniert
- ❌ Zwischenfehler in der Session ignorieren nur weil sie "danach" gefixt wurden
- ❌ Session beenden ohne Rueckblick auf Verbesserungsmoeglichkeiten
- ❌ Stumm arbeiten ohne proaktive Vorschlaege zur Intelligenz-Steigerung
- ❌ Fix deployen der selbst neue Fehler einfuehrt (Fix-Induced Failure)
- ❌ Fix ohne die 8-Punkte-Pruefung aus Schritt 3b durchzugehen
- ❌ Einen Fehler ein ZWEITES Mal machen — jeder Fehler wird genau EINMAL gemacht
