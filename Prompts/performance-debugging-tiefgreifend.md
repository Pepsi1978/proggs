Führe ein tiefgreifendes, iteratives Performance-Debugging der letzten Implementierung durch – ohne bestehende Funktionalität, Verhalten, Schnittstellen oder Design zu verändern oder zu beeinträchtigen. Nutze dabei AUSNAHMSLOS das vorhandene Bug-Wissen (Bug-Almanach, Bug-Fall-Datenbank) und die Best Practices der betroffenen Bereiche – sowohl präventiv (vorher nachschlagen, besonders bekannte Performance-Fallen), als auch reaktiv (bei jedem Engpass matchen), als auch lernend (neue Funde zurückschreiben). Gehe strikt nach folgendem Protokoll vor:

0. Wissens-Grundierung: Bug-Almanach + Best Practices ZUERST (Pflicht, vor jeder Analyse)
Bevor irgendetwas charakterisiert, analysiert oder optimiert wird, beschaffe dir das vorhandene, kuratierte Bug- und Best-Practices-Wissen für genau die Technologie-Bereiche, um die es geht. Das ist Prävention: bekannte Performance-Fallen werden gar nicht erst neu gesucht, und der dokumentierte Performance-Sollzustand steht von Anfang an als Referenz bereit.

a) Bereiche erkennen: Bestimme grob, welche technischen Bereiche die letzte Implementierung berührt (z. B. Kotlin, Jetpack Compose, Gradle, R8, Room, Coil, Media3/ExoPlayer, WorkManager, Firebase, Swift/AppKit, .NET/WPF, TypeScript, Chrome-Erweiterungen, MCP, Python, APIs …). Mehrere Bereiche sind möglich – dann alle behandeln.
b) Index prüfen: Schlage in ~/proggs/bugs/README.md nach, ob für die Bereiche ein Almanach existiert.
c) Almanach lesen (Digest-Modell, in dieser Reihenfolge erst Almanach, dann Best Practices) – mit Performance-Brille:
   - Normaler Bereich (Stufe A): NUR den Kurzcheck lesen – Read auf ~/proggs/bugs/<kategorie>/<bereich>.md mit limit=80 (Erkennungssignale + Sofort-Regeln). Achte gezielt auf dokumentierte Performance-Fallen (z. B. unnötige Re-Renders/Re-Layouts, Allocation-Druck auf Hot Paths, N+1-IO, blockierende Aufrufe auf UI-/Event-Loop-Threads, teures Logging unter Last, Cold-Start-Kosten).
   - Hochrisiko-Bereich (Stufe C: r8, firebase-billing, claude-hooks, claude-config): den VOLLTEXT lesen (Read ohne limit), inkl. Versions-Abgleich pro Bug.
   - Versions-Anker beachten: Arbeite ich mit einer neueren Version als im Almanach dokumentiert, vermerke das und behandle die Lage als potenziell abweichend.
d) Best Practices lesen: Direkt danach die zugehörige ~/proggs/best-practices/<kategorie>/<bereich>.md (Kurzcheck, limit=80 genügt; bei Hochrisiko ausführlicher). Der Almanach sagt, WAS performt schlecht und warum; die Best Practices sagen, wie es von vornherein PERFORMANT gemacht wird – das ist der Performance-Sollzustand, gegen den geprüft wird.
e) Kein Almanach für einen relevanten Bereich vorhanden? Das ist ein Signal, kein Freibrief: notiere es als Lücke (wird in Schritt 7 zum Anlegen vorgeschlagen) und arbeite mit erhöhter Vorsicht weiter.

Ergebnis von Schritt 0: eine Liste bereichsspezifisch bekannter Performance-Fallen und Anti-Pattern PLUS der Best-Practice-Performance-Sollzustand – beides fließt verbindlich in die Schritte 2 und 5 ein.

1. Funktions- und Lastcharakterisierung (Pflicht-Research vor der Code-Analyse)
Charakterisiere eigenständig und präzise, was die letzte Implementierung fachlich-funktional ist und wie sie typischerweise belastet wird:

Funktionale Identität:
- Zweck und Verantwortung: Welches konkrete Problem löst dieses Modul?
- Funktionstyp: Zu welcher Klasse gehört es (z. B. Berechnung, Transformation, Persistenz, Kommunikation, Zustandsverwaltung, Rendering, Caching, Scheduling, IO, Parsing, Event-Handling, Streaming, Orchestrierung …)? Mehrere Typen möglich.
- Inputs und Outputs: Welche Daten/Ereignisse fließen rein und raus, in welchen Wertebereichen und realistischen Größenordnungen?

Lastprofil und Skalierungsverhalten:
- Häufigkeit und Hot-Path-Charakter: Wie oft wird dieser Code unter realistischer Nutzung aufgerufen – pro Sekunde, pro User-Aktion, pro Frame, pro Request, einmalig beim Start, sporadisch?
- Skalierungsdimensionen: Mit was wächst die Arbeit – Eingabegröße N, Anzahl Items, Anzahl Nutzer, Datenvolumen, Frequenz, Parallelität, Zustandsgröße? Welcher Wachstumstyp ist realistisch (linear, mehrdimensional, burst-artig)?
- Kritische Metrik: Ist Latenz, Durchsatz, Reaktionszeit, Frame-Time, Boot-Dauer, Energieverbrauch oder Speicher-Footprint die dominierende Größe für diesen Funktionstyp? Was ist sekundär?
- Ressourcenprofil: Welche Ressourcen werden voraussichtlich am stärksten beansprucht (CPU, Memory/Allocation, IO, Netzwerk, GPU, Energie, Threads, Locks, Connections)?

Kontext:
- Lebenszyklus: Einmalig, wiederholt, parallel, asynchron, ereignisgetrieben, batchweise, kontinuierlich?
- Externe Wechselwirkungen: Welche Komponenten/Systeme werden aufgerufen, mit welcher erwartbaren Latenz und Frequenz?
- Implizite Kontextannahmen: Über Datenverteilungen, Cache-Zustand, typische Last, Hardware, Umgebung, Parallelitäts-Annahmen.

Ergebnis: ein präzises Funktions- und Lastprofil. Dieses Profil – nicht der Code-Ort – ist die Grundlage aller folgenden Schritte.

2. Ableitung der relevanten Performance-Dimensionen (Profil + Almanach + Best Practices)
Leite ab, welche Performance-Klassen für genau diese Art von Funktionalität und dieses Lastprofil typisch und kritisch sind. Verschmelze dabei DREI Quellen:
(i) das Funktions- und Lastprofil aus Schritt 1,
(ii) die bereichsspezifisch bekannten Performance-Fallen aus dem Bug-Almanach (Schritt 0) – jede dokumentierte Performance-Falle des Bereichs, die auf dieses Modul anwendbar ist, wird zu einer PFLICHT-Dimension mit hoher Priorität,
(iii) den Best-Practice-Performance-Sollzustand des Bereichs – Abweichungen davon sind eigenständige Prüfpunkte.

Unterschiedliche Funktions-/Lasttypen haben fundamental unterschiedliche Engpass-Profile – die Auswahl und Priorisierung muss zum konkreten Modul passen, nicht zu einer Standardliste. Prüfe mindestens die folgenden Dimensionen auf Anwendbarkeit, priorisiere sie nach Relevanz, und begründe pro Dimension kurz, warum sie für genau dieses Modul wichtig (oder nicht anwendbar) ist:
- Algorithmische Komplexität: Zeit-/Raumkomplexität in den relevanten Skalierungsdimensionen, vermeidbare Wiederholungsarbeit, redundante Berechnungen, fehlende Memoization.
- Hot-Path-Effizienz: Was passiert tatsächlich auf dem häufig durchlaufenen Pfad? Unnötige Arbeit, überflüssige Indirektion, vermeidbare Validierungen, sinnlose Funktionsaufrufe.
- Speicher- und Allocation-Verhalten: Allocation-Druck auf Hot Paths, kurzlebige Objekte in Schleifen, unnötige Kopien, große temporäre Strukturen, GC-/Memory-Manager-Belastung, Wachstum über Zeit.
- Datenstruktur-Eignung: Passen die Datenstrukturen zum tatsächlichen Zugriffsmuster (Lookup, Insert, Iteration, Sortierung)? Lineare Suche statt Map, falsche Kollektion, schlechte Cache-Locality.
- IO- und Round-Trip-Verhalten: Anzahl, Größe, Sequenzialität, Parallelisierbarkeit von IO-/Netzwerk-/DB-/IPC-Calls; N+1-Muster, fehlendes Batching, fehlendes Streaming, unnötige Synchronität.
- Caching und Wiederverwendung: Wo wäre Caching effektiv (Ergebnis, Berechnung, Verbindung, Ressource), wo ist vorhandenes Caching wirkungslos, Hit-Raten-Annahmen, Stale-Risiko vs. Recompute-Kosten.
- Lazy vs. Eager Auswertung: Wird Arbeit gemacht, deren Ergebnis selten benötigt wird? Wird umgekehrt mehrfach lazy berechnet, was einmal eager günstiger wäre?
- Nebenläufigkeit und Synchronisation: Lock-Contention, übergroße kritische Abschnitte, falsche Sperr-Granularität, ungenutzte Parallelität, serialisierte unabhängige Arbeit, False Sharing, Thread-Hopping-Overhead.
- Asynchronität und Backpressure: Blockierende Aufrufe auf zeitkritischen Pfaden (UI-Thread, Event-Loop), fehlende Async-Nutzung, fehlendes Debouncing/Throttling, unkontrollierte Queue-Größen.
- Render- und UI-Performance (falls relevant): Frame-Time-Stabilität, unnötige Re-Renders/Re-Layouts, Layout-Thrashing, Off-Main-Thread-Auslagerung, Bild-/Asset-Kosten.
- Startup- und Initialisierungskosten: Cold-Start-Zeit, eagerer Aufbau von selten Genutztem, vermeidbare frühe Arbeit, Initialisierungsreihenfolge.
- Logging-, Observability- und Debug-Overhead: Kosten von Tracing/Logging auf Hot Paths, teure Format-Operationen vor Filter, ungenutzte Tiefen-Logs.
- Serialisierung und Datenformate: Konvertierungen, Mehrfach-Parsen, ineffiziente Formate, unnötige Zwischenrepräsentationen.
- Energie- und Ruhezustand-Verhalten (falls mobil/embedded relevant): Wake-Frequenz, Polling, Hintergrundarbeit, Sensor-/Netzaktivität.
- BEREICHSSPEZIFISCHE Almanach-Performance-Dimensionen: jede bekannte Performance-Falle/jedes dokumentierte Anti-Pattern des Bereichs aus Schritt 0 (höchste Priorität, weil empirisch belegt schon einmal aufgetreten).
- BEST-PRACTICE-Abweichungen: jede Stelle, an der die Implementierung vom dokumentierten Performance-Sollzustand abweicht.

Dimensionen, die nachweislich nicht anwendbar sind, werden mit kurzer Begründung übersprungen. Ergebnis: ein modulspezifisches Performance-Profil, das festlegt, welche Dimensionen mit welcher Priorität und Tiefe untersucht werden – bekannte Performance-Fallen des Bereichs zuerst.

3. Scope-Lokalisierung (untergeordnet)
Identifiziere die Code-Stellen, an denen die Implementierung lebt – Dateien, Funktionen, Hot Paths, Aufrufer/Konsumenten. Dies dient nur als Karte für die spätere Optimierungsarbeit, nicht als Filter für die fachliche Analyse aus Schritt 1/2.

4. Baseline-Fixierung (Verhaltens- und Designneutralität präzise definieren)
Erfasse das intendierte funktionale Verhalten und die Design-/Oberflächen-Charakteristik. Definiere diese Baseline als invariant. Unterscheide explizit:
- intendiertes funktionales Verhalten (bleibt semantisch äquivalent – gleiche Ergebnisse für gleiche Eingaben),
- intendierte Design-/UX-Charakteristik (Animationen, Sichtbarkeitsreihenfolge, Übergänge, visuelle Erscheinung – bleibt unverändert),
- intendierte Schnittstellen (API-Form, Rückgaben, Fehlertypen, Fehlerzeitpunkte – bleiben unverändert),
- beobachtbare Reihenfolgen von Seiteneffekten (Logs, Events, Side-Effects in definierter Sequenz – bleiben unverändert),
- Grauzonen (im Zweifel als intendiert behandeln und in der Abschlussliste als „klärungsbedürftig" markieren).

Warnung Performance-spezifisch: Optimierungen können Verhalten subtil verändern – Lazy/Eager-Wechsel verschieben Fehlerzeitpunkte, Caching kann Stale-Daten erzeugen, Parallelisierung ändert Reihenfolge von Seiteneffekten, Batching ändert Wahrnehmung von Latenz, Off-Main-Thread-Auslagerung kann Synchronisierungs-Annahmen aufweichen. Eine Optimierung ist nur erlaubt, wenn nachweislich verhaltens- und designneutral. Wo der Almanach für eine geplante Optimierung bereits eine dokumentierte Falle kennt (z. B. „Caching hier erzeugt Stale-Daten"), gilt diese Warnung verbindlich.

5. Iterative Loops mit ansteigender Tiefe (mit CBR-Lookup bei jedem Engpass)
- Analysiere entlang der in Schritt 2 abgeleiteten Performance-Dimensionen, in der dort festgelegten Priorisierung (bereichsspezifische Almanach-Performance-Dimensionen zuerst).
- Bei JEDEM gefundenen Engpass/Verdacht ZUERST das vorhandene Wissen abgleichen (Case-Based Reasoning, 4 Phasen):
  • RETRIEVE: In ~/proggs/.claude/agent-memory/shared/bug-cases.jsonl nach Symptom/Muster suchen UND – da jetzt im Bereich ein konkretes Problem vorliegt – den VOLLTEXT des zugehörigen Almanachs lesen (Stufe B: Read ohne limit; ab dem ersten Problem reicht der Kurzcheck nicht mehr).
  • REUSE: Ist es eine bekannte Performance-Falle, die dokumentierte, verhaltens- und designneutrale Optimierung als ERSTEN Lösungsansatz anwenden (schnellster, sicherster Pfad).
  • REVISE: Passt der bekannte Fix nicht 1:1, gezielt anpassen.
  • (RETAIN folgt in Schritt 7.)
- Behebe gefundene Engpässe unmittelbar – ausschließlich durch verhaltens- und designneutrale Änderungen, die dem Best-Practice-Performance-Sollzustand des Bereichs entsprechen.
- Bei jedem Fix dokumentiere kurz, warum die Änderung funktional und visuell äquivalent ist (insbesondere Sichtbarkeitsreihenfolge, Fehlerverhalten, beobachtbare Seiteneffekte, Threading-Annahmen); nenne ggf. den genutzten Almanach-/Best-Practice-Eintrag.
- Loop-Tiefenstufen:
  Loop 1: offensichtliche Engpässe auf dem Hot Path (überflüssige Arbeit, falsche Datenstruktur, N+1-Muster) + direkte Treffer aus dem Almanach.
  Loop 2: Allocation-/Memory-Druck, Cache-Wirksamkeit, Sequenz vs. Parallelisierbarkeit, einfache Lazy/Eager-Korrekturen, dokumentierte Versions-/Umgebungs-Fallen.
  Loop 3: subtile Wechselwirkungen – Logging-Overhead unter Last, versteckte Synchronisation, schlechte Cache-Locality, indirekte Re-Renders, Kaskadeneffekte zwischen Modulen.
  Loop 4+: nicht offensichtliche Engpässe entlang der modulspezifischen Lastdimensionen und der bereichsspezifischen Almanach-Performance-Dimensionen, Mikro-Optimierungen mit nachweisbarer Wirkung, Skalierungs-Edge-Cases an den Grenzen des realistischen Lastprofils.
- Engpässe, deren Behebung Verhalten oder Design verändern würde: nicht eigenmächtig ausführen, sondern in Schritt 7, Liste (b) mit Trade-off als Vorschlag auflisten (z. B. „spart ~X% Zeit, verschiebt aber Fehlerzeitpunkt / ändert Sichtbarkeitsreihenfolge / erhöht Speicher um Y").
- Fixe immer getreu Direktive #3 (Resilient Bugfixing): Root Cause statt Symptom, verwandte Engpass-Quellen prüfen, Funktionalität niemals entfernen/auskommentieren/schlucken.

6. Abbruchbedingung
Wiederhole, bis in zwei aufeinanderfolgenden Loops keine verhaltens- und designneutralen Engpässe mehr gefunden werden.

7. Abschluss-Verifikation + Wissens-Rückschreibung (Compound Intelligence)
Zuerst drei getrennte Ergebnis-Listen:
(a) Behobene Engpässe – was war ineffizient, wie optimiert, mit kurzer Begründung der Verhaltens- und Designäquivalenz; bei bekannten Fallen: welcher Almanach-/bug-cases-Eintrag genutzt wurde.
(b) Nicht eigenmächtig optimiert – Engpässe, deren Fix Verhalten oder Design berührt hätte; mit Trade-off und Empfehlung.
(c) Klärungsbedürftige Grauzonen – Verhalten oder Performance-Aspekte, deren Äquivalenz-Status unklar war.

Danach die Wissens-Rückschreibung (RETAIN – Pflicht, damit Recherche/Funde nicht „verkommen"):
- Jede NEU erlebte Performance-Falle (die noch nicht im Almanach stand) in den passenden ~/proggs/bugs/<kategorie>/<bereich>.md eintragen (Symptom/Muster, Root Cause, betroffene Versionen, verhaltensneutrale Optimierung, Quelle) – Kurzcheck-Sektion UND Volltext; Stand-Header aktualisieren.
- Jeden Fall zusätzlich als Zeile in ~/proggs/.claude/agent-memory/shared/bug-cases.jsonl anhängen (nur appenden).
- Fehlte für einen Bereich ein Almanach (Schritt 0e): das Anlegen vorschlagen (Skill bug-almanach-recherche), nicht ad hoc selbst recherchieren.
- Lieferten die Funde verallgemeinerbare Performance-Erkenntnisse: die zugehörige ~/proggs/best-practices/<kategorie>/<bereich>.md ergänzen (Kurzcheck + Volltext).

Sei allumfassend und kreativ; achte besonders auf subtile, nicht offensichtliche Engpässe entlang der modulspezifischen UND der bereichsspezifischen (Almanach-)Performance-Dimensionen – niemals auf Kosten bestehender Funktionalität oder des Designs.
