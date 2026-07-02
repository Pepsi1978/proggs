# Brille `bugs` — Profil-Checkliste, Dimensionen und Loop-Stufen

## Schritt 1 — Funktionsprofil (vollständige Checkliste)

Eigenständig und präzise charakterisieren, was die Implementierung fachlich-funktional ist:
- **Zweck und Verantwortung:** Welches konkrete Problem löst dieses Modul? Welche fachliche Rolle?
- **Funktionstyp:** Zu welcher Klasse gehört es (Berechnung, Transformation, Persistenz,
  Kommunikation, Zustandsverwaltung, Validierung, Orchestrierung, Rendering, Caching,
  Scheduling, Sicherheit, IO, Parsing, Synchronisation, Event-Handling …)? Mehrere möglich —
  dann alle benennen.
- **Inputs und Outputs:** Welche Daten/Signale/Ereignisse fließen rein/raus? Realistische
  Wertebereiche, Typen, Häufigkeiten, Quellen?
- **Fachliche Invarianten und Korrektheitskriterien:** Was muss jederzeit gelten,
  unabhängig von der Code-Form?
- **Lebenszyklus und Aufrufmuster:** Einmalig, wiederholt, parallel, asynchron,
  ereignisgetrieben, batchweise, persistent, transient?
- **Externe Wechselwirkungen:** Mit welchen Komponenten/Systemen interagiert es,
  welche Annahmen macht es über diese?
- **Implizite Kontextabhängigkeiten:** Welche Umgebungs-, Zeit-, Konfigurations- oder
  Zustandsannahmen sind nicht ausgesprochen, aber vorausgesetzt?

## Schritt 2 — Debugging-Dimensionen

Mindestens diese generischen Dimensionen auf Anwendbarkeit prüfen, nach Relevanz für den
ermittelten Funktionstyp priorisieren, pro Dimension kurz begründen warum wichtig
(oder nicht anwendbar):

1. **Logische Korrektheit** — Bedingungen, Verzweigungen, Negationen, Vergleiche,
   Off-by-One, Schleifenabbruch.
2. **Ablauf- und Sequenzkorrektheit** — Reihenfolge der Schritte, Vorbedingungen,
   korrekte Häufigkeit, korrekte Phasenübergänge.
3. **Datenflussintegrität** — Initialisierung, Mutation, Konsistenz zwischen Schritten,
   Lebensdauer und Sichtbarkeit von Werten.
4. **Rand- und Sonderfälle** — leere/maximale/ungültige Eingaben, Grenzwerte,
   Null/Undefined, unerwartete Typen, Encodings, Zeitpunkte
   (Beispiel Real-Lauf: `weight(0f)` crasht bei Kategorie mit 0 Einträgen;
   `Int.MIN_VALUE`-Hash → negativer Index).
5. **Fehler- und Ausfallpfade** — geschluckte Fehler, falsche Defaults, fehlende
   Rollbacks/Cleanups, partielle Erfolge, ungeprüfte Rückgaben
   (Beispiel: Fehlerpfad ersetzte funktionierende UI-Liste durch `emptyList()`).
6. **Nebenläufigkeit und Timing** — Race Conditions, Reentry, Idempotenz,
   Reihenfolgeannahmen, zeitabhängige Pfade
   (Kotlin-Pflichtdimension: `CancellationException` nie in `catch(Exception)`
   verschlucken — Almanach kotlin §2.1; der häufigste Fund im Real-Lauf, 29 Stellen).
7. **Ressourcen- und Lifecycle-Verhalten** — Leaks, doppelte Freigabe, hängige
   Subscriptions/Handles/Listener, fehlerhafte Initialisierung/Teardown
   (Beispiele: ProcessLifecycleOwner-Observer nie entfernt; InputStream ohne `use{}`;
   AudioTrack ohne try/finally; Log ohne Rotation).
8. **Implizite Annahmen** — über Daten, Aufrufer, Umgebung, Konfiguration, externe
   Systeme, Zeit, Reihenfolge
   (Beispiel: `by lazy`-Retrofit friert baseUrl ein, Settings sind aber veränderlich).
9. **Vertragskonsistenz** — Schnittstelle vs. tatsächliches Verhalten, Doku vs. Code,
   deklarierte Typen vs. reale Werte (auch: UTC vs. lokale Zeit in der Anzeige).
10. **Fachliche Korrektheit** — gegen die Domänenregeln des Moduls (Funktionsprofil).
11. **Sicherheits- und Integritätsrisiken** — innerhalb des Funktionstyps
    (Secrets im Log, ungeschützte Persistenz, Injection-Flächen).
12. **BEREICHSSPEZIFISCHE Almanach-Dimensionen** — jede dokumentierte Falle des Bereichs
    aus Schritt 0 (höchste Priorität, empirisch belegt).
13. **BEST-PRACTICE-Abweichungen** — jede Stelle, an der die Implementierung vom
    dokumentierten Sollzustand abweicht.

## Schritt 5 — Loop-Tiefenstufen (Brille bugs)

- **Loop 1:** offensichtliche logische Fehler im Hauptpfad + direkte Treffer aus dem Almanach.
- **Loop 2:** Randfälle, implizite Annahmen, dokumentierte Versions-/Umgebungs-Fallen.
- **Loop 3:** Wechselwirkungen, Reihenfolgeprobleme, subtile Zustandskorruption.
- **Loop 4+:** nicht offensichtliche Inkonsistenzen, fachliche Edge Cases entlang der
  Domänenregeln aus dem Funktionsprofil und der bereichsspezifischen Almanach-Dimensionen.
