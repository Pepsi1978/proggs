# Analyse-Dimensionen — Brille `bugs` (Logik-/Codefehler)

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
