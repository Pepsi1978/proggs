# Brille `performance` — Last-Checkliste, Dimensionen und Loop-Stufen

## Schritt 1 — Funktions- und Lastprofil (vollständige Checkliste)

**Funktionale Identität:**
- **Zweck und Verantwortung:** Welches konkrete Problem löst dieses Modul?
- **Funktionstyp:** Berechnung, Transformation, Persistenz, Kommunikation,
  Zustandsverwaltung, Rendering, Caching, Scheduling, IO, Parsing, Event-Handling,
  Streaming, Orchestrierung … (mehrere möglich).
- **Inputs und Outputs:** Welche Daten/Ereignisse fließen rein/raus, in welchen
  Wertebereichen und realistischen Größenordnungen?

**Lastprofil und Skalierungsverhalten:**
- **Häufigkeit und Hot-Path-Charakter:** Wie oft läuft dieser Code unter realistischer
  Nutzung — pro Sekunde, pro User-Aktion, pro Frame, pro Request, einmalig beim Start,
  sporadisch?
- **Skalierungsdimensionen:** Womit wächst die Arbeit — Eingabegröße N, Anzahl Items,
  Nutzer, Datenvolumen, Frequenz, Parallelität, Zustandsgröße? Wachstumstyp (linear,
  mehrdimensional, burst-artig)?
- **Kritische Metrik:** Latenz, Durchsatz, Reaktionszeit, Frame-Time, Boot-Dauer,
  Energieverbrauch oder Speicher-Footprint — was dominiert, was ist sekundär?
- **Ressourcenprofil:** CPU, Memory/Allocation, IO, Netzwerk, GPU, Energie, Threads,
  Locks, Connections — was wird am stärksten beansprucht?

**Kontext:**
- **Lebenszyklus:** einmalig, wiederholt, parallel, asynchron, ereignisgetrieben,
  batchweise, kontinuierlich?
- **Externe Wechselwirkungen:** welche Systeme, mit welcher erwartbaren Latenz/Frequenz?
- **Implizite Kontextannahmen:** Datenverteilungen, Cache-Zustand, typische Last,
  Hardware, Umgebung, Parallelitäts-Annahmen.

## Schritt 2 — Performance-Dimensionen

Unterschiedliche Funktions-/Lasttypen haben fundamental unterschiedliche Engpass-Profile —
Auswahl und Priorisierung müssen zum konkreten Modul passen, nicht zu einer Standardliste.
Mindestens diese Dimensionen auf Anwendbarkeit prüfen, priorisieren, pro Dimension kurz
begründen; nicht anwendbare mit Begründung überspringen:

1. **Algorithmische Komplexität** — Zeit-/Raumkomplexität in den relevanten
   Skalierungsdimensionen, vermeidbare Wiederholungsarbeit, redundante Berechnungen,
   fehlende Memoization (Beispiel Real-Lauf: `lastIndexOf` über den ganzen Puffer pro
   SSE-Delta = O(n²)).
2. **Hot-Path-Effizienz** — was passiert tatsächlich auf dem häufig durchlaufenen Pfad?
   Unnötige Arbeit, überflüssige Indirektion, vermeidbare Validierungen
   (Beispiel: Regex-Neukompilierung pro Aufruf, Wortzählung pro Recomposition).
3. **Speicher- und Allocation-Verhalten** — Allocation-Druck auf Hot Paths, kurzlebige
   Objekte in Schleifen, unnötige Kopien, große temporäre Strukturen, GC-Last,
   Wachstum über Zeit.
4. **Datenstruktur-Eignung** — passen die Strukturen zum Zugriffsmuster? Lineare Suche
   statt Map, falsche Kollektion, schlechte Cache-Locality.
5. **IO- und Round-Trip-Verhalten** — Anzahl/Größe/Sequenzialität von IO-/Netz-/DB-Calls;
   N+1-Muster, fehlendes Batching/Streaming, unnötige Synchronität
   (Beispiel: zwei unabhängige Server-Calls SERIELL über den langsamen Tunnel —
   parallelisieren halbiert die Latenz).
6. **Caching und Wiederverwendung** — wo wäre Caching effektiv, wo wirkungslos;
   Hit-Raten-Annahmen; Stale-Risiko vs. Recompute-Kosten (Stale-Risiko → Liste b!).
7. **Lazy vs. Eager** — Arbeit, deren Ergebnis selten gebraucht wird? Mehrfach lazy
   berechnet, was einmal eager günstiger wäre?
8. **Nebenläufigkeit und Synchronisation** — Lock-Contention, übergroße kritische
   Abschnitte, ungenutzte Parallelität, serialisierte unabhängige Arbeit.
9. **Asynchronität und Backpressure** — blockierende Aufrufe auf zeitkritischen Pfaden
   (UI-Thread/Event-Loop!), fehlendes Debouncing/Throttling, unkontrollierte Queues
   (Beispiel: synchrones Datei-Logging auf dem Aufrufer-Thread pro Log-Zeile).
10. **Render- und UI-Performance** — Frame-Time-Stabilität, unnötige Re-Renders/
    Recompositions, Layout-Thrashing, Reads deferren
    (Compose: Sortierungen pro Animations-Frame → `remember`; animierten Wert in
    eigenen Recompose-Scope; `key`+`contentType` in Lazy-Listen; `derivedStateOf`).
11. **Startup- und Initialisierungskosten** — Cold-Start, eagerer Aufbau von selten
    Genutztem, Initialisierungsreihenfolge (Umbau meist Liste b — Race-Gefahr).
12. **Logging-/Observability-Overhead** — Kosten von Tracing/Logging auf Hot Paths,
    teure Formatierung vor Filter, ungenutzte Tiefen-Logs
    (dabei Live-Mitlesbarkeit erhalten — Logcat synchron lassen, nur Datei-IO auslagern).
13. **Serialisierung und Datenformate** — Konvertierungen, Mehrfach-Parsen,
    unnötige Zwischenrepräsentationen.
14. **Energie- und Ruhezustand-Verhalten** (mobil) — Wake-Frequenz, Polling,
    Hintergrundarbeit, dauerlaufende Animationen
    (Beispiel: unbedingt erzeugte `InfiniteTransition` tickt in JEDEM Zustand —
    Almanach compose §9.6).
15. **BEREICHSSPEZIFISCHE Almanach-Performance-Dimensionen** — jede dokumentierte
    Performance-Falle des Bereichs aus Schritt 0 (höchste Priorität).
16. **BEST-PRACTICE-Abweichungen** — jede Abweichung vom dokumentierten
    Performance-Sollzustand.

## Schritt 5 — Loop-Tiefenstufen (Brille performance)

- **Loop 1:** offensichtliche Engpässe auf dem Hot Path (überflüssige Arbeit, falsche
  Datenstruktur, N+1-Muster) + direkte Treffer aus dem Almanach.
- **Loop 2:** Allocation-/Memory-Druck, Cache-Wirksamkeit, Sequenz vs. Parallelisierbarkeit,
  einfache Lazy/Eager-Korrekturen, dokumentierte Versions-/Umgebungs-Fallen.
- **Loop 3:** subtile Wechselwirkungen — Logging-Overhead unter Last, versteckte
  Synchronisation, schlechte Cache-Locality, indirekte Re-Renders, Kaskadeneffekte
  zwischen Modulen.
- **Loop 4+:** nicht offensichtliche Engpässe entlang der modulspezifischen
  Lastdimensionen und der bereichsspezifischen Almanach-Dimensionen, Mikro-Optimierungen
  mit nachweisbarer Wirkung, Skalierungs-Edge-Cases an den Grenzen des realistischen
  Lastprofils.

**Trade-off-Format für Liste (b):** z. B. „spart ~X% Zeit, verschiebt aber den
Fehlerzeitpunkt / ändert die Sichtbarkeitsreihenfolge / erhöht Speicher um Y".

**Ehrlichkeits-Regel:** Alle Funde sind statisch hergeleitet, keine Messwerte. Im
Abschlussbericht klar kennzeichnen und bei Bedarf eine Baseline-Messung
(Macrobenchmark / Log-Zeitstempel) als Folgeaufgabe vorschlagen.
