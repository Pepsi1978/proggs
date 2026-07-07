Führe ein tiefgreifendes, iteratives Debugging der letzten Implementierung durch – ohne bestehende Funktionalität, Verhalten, Schnittstellen oder Design zu verändern oder zu beeinträchtigen. Nutze dabei AUSNAHMSLOS das vorhandene Bug-Wissen (Bug-Almanach, Bug-Fall-Datenbank) und die Best Practices der betroffenen Bereiche – sowohl präventiv (vorher nachschlagen) als auch reaktiv (bei jedem Fund matchen) als auch lernend (neue Funde zurückschreiben). Gehe strikt nach folgendem Protokoll vor:

0. Wissens-Grundierung: Bug-Almanach + Best Practices ZUERST (Pflicht, vor jeder Analyse)
Bevor irgendetwas charakterisiert, analysiert oder geändert wird, beschaffe dir das vorhandene, kuratierte Bug- und Best-Practices-Wissen für genau die Technologie-Bereiche, um die es geht. Das ist Prävention: bekannte Fehler werden gar nicht erst neu gesucht.

a) Bereiche erkennen: Bestimme grob, welche technischen Bereiche die letzte Implementierung berührt (z. B. Kotlin, Jetpack Compose, Gradle, R8, Room, Firebase-Billing, Swift/AppKit, .NET/WPF, TypeScript, Chrome-Erweiterungen, Claude-Hooks/-Config, MCP, Python, APIs …). Mehrere Bereiche sind möglich – dann alle behandeln.
b) Index prüfen: Schlage in ~/proggs/bugs/README.md nach, ob für die Bereiche ein Almanach existiert.
c) Almanach lesen (Digest-Modell, in dieser Reihenfolge erst Almanach, dann Best Practices):
   - Normaler Bereich (Stufe A): NUR den Kurzcheck lesen – Read auf ~/proggs/bugs/<kategorie>/<bereich>.md mit limit=80 (Erkennungssignale + Sofort-Regeln).
   - Hochrisiko-Bereich (Stufe C: r8, firebase-billing, claude-hooks, claude-config): den VOLLTEXT lesen (Read ohne limit), inkl. Versions-Abgleich pro Bug.
   - Versions-Anker beachten: Arbeite ich mit einer neueren Version als im Almanach dokumentiert, vermerke das und behandle die Lage als potenziell abweichend.
d) Best Practices lesen: Direkt danach die zugehörige ~/proggs/best-practices/<kategorie>/<bereich>.md (Kurzcheck, limit=80 genügt; bei Hochrisiko ausführlicher). Der Almanach sagt, WAS schiefgeht; die Best Practices sagen, wie es von vornherein RICHTIG gemacht wird – das ist der Soll-Zustand, gegen den geprüft wird.
e) Kein Almanach für einen relevanten Bereich vorhanden? Das ist ein Signal, kein Freibrief: notiere es als Lücke (wird in Schritt 7 zum Anlegen vorgeschlagen) und arbeite mit erhöhter Vorsicht weiter.

Ergebnis von Schritt 0: eine Liste bereichsspezifisch bekannter Fehlerklassen, Fallen und Workarounds PLUS der Best-Practice-Sollzustand – beides fließt verbindlich in die Schritte 2 und 5 ein.

1. Funktionscharakterisierung (Pflicht-Research vor der Code-Analyse)
Charakterisiere eigenständig und präzise, was die letzte Implementierung fachlich-funktional ist:
- Zweck und Verantwortung: Welches konkrete Problem löst dieses Modul? Welche fachliche Rolle?
- Funktionstyp: Zu welcher Klasse gehört es (Berechnung, Transformation, Persistenz, Kommunikation, Zustandsverwaltung, Validierung, Orchestrierung, Rendering, Caching, Scheduling, Sicherheit, IO, Parsing, Synchronisation, Event-Handling …)? Mehrere möglich – dann alle benennen.
- Inputs und Outputs: Welche Daten/Signale/Ereignisse fließen rein/raus? Realistische Wertebereiche, Typen, Häufigkeiten, Quellen?
- Fachliche Invarianten und Korrektheitskriterien: Was muss jederzeit gelten, unabhängig von der Code-Form?
- Lebenszyklus und Aufrufmuster: Einmalig, wiederholt, parallel, asynchron, ereignisgetrieben, batchweise, persistent, transient?
- Externe Wechselwirkungen: Mit welchen Komponenten/Systemen interagiert es, welche Annahmen macht es über diese?
- Implizite Kontextabhängigkeiten: Welche Umgebungs-, Zeit-, Konfigurations- oder Zustandsannahmen sind nicht ausgesprochen, aber vorausgesetzt?

Ergebnis: ein präzises Funktionsprofil. Dieses Profil – nicht der Code-Ort – ist die Grundlage aller folgenden Schritte.

2. Ableitung der relevanten Debugging-Dimensionen (Funktionsprofil + Almanach + Best Practices)
Leite ab, welche Bug-Klassen für genau diese Art von Funktionalität typisch und kritisch sind. Verschmelze dabei DREI Quellen:
(i) das Funktionsprofil aus Schritt 1,
(ii) die bereichsspezifisch bekannten Fehlerklassen aus dem Bug-Almanach (Schritt 0) – jeder dokumentierte Bug des Bereichs, der auf dieses Modul anwendbar ist, wird zu einer PFLICHT-Dimension mit hoher Priorität,
(iii) die Best Practices des Bereichs – Abweichungen vom dokumentierten Sollzustand sind eigenständige Prüfpunkte.

Prüfe mindestens die folgenden generischen Dimensionen auf Anwendbarkeit, priorisiere sie nach Relevanz für den ermittelten Funktionstyp, und begründe pro Dimension kurz, warum sie für genau dieses Modul wichtig (oder nicht anwendbar) ist:
- Logische Korrektheit: Bedingungen, Verzweigungen, Negationen, Vergleiche, Off-by-One, Schleifenabbruch.
- Ablauf- und Sequenzkorrektheit: Reihenfolge der Schritte, Vorbedingungen, korrekte Häufigkeit, korrekter Phasenübergang.
- Datenflussintegrität: Initialisierung, Mutation, Konsistenz zwischen Schritten, Lebensdauer und Sichtbarkeit von Werten.
- Rand- und Sonderfälle: leere, maximale, ungültige Eingaben; Grenzwerte; Null/Undefined; unerwartete Typen, Encodings, Zeitpunkte.
- Fehler- und Ausfallpfade: geschluckte Fehler, falsche Defaults, fehlende Rollbacks/Cleanups, partielle Erfolge, ungeprüfte Rückgaben.
- Nebenläufigkeit und Timing: Race Conditions, Reentry, Idempotenz, Reihenfolgeannahmen, zeitabhängige Pfade.
- Ressourcen- und Lifecycle-Verhalten: Lecks, doppelte Freigabe, hängige Subscriptions/Handles/Listener, fehlerhafte Initialisierung/Teardown.
- Implizite Annahmen: über Daten, Aufrufer, Umgebung, Konfiguration, externe Systeme, Zeit, Reihenfolge.
- Vertragskonsistenz: Schnittstelle vs. tatsächliches Verhalten, Doku vs. Code, deklarierte Typen vs. reale Werte.
- Fachliche Korrektheit gegen die Domänenregeln des Moduls (gemäß Funktionsprofil aus Schritt 1).
- Sicherheits- und Integritätsrisiken innerhalb des Funktionstyps.
- BEREICHSSPEZIFISCHE Almanach-Dimensionen: jede bekannte Falle/jeder dokumentierte Bug des Bereichs aus Schritt 0 (höchste Priorität, weil empirisch belegt schon einmal aufgetreten).
- BEST-PRACTICE-Abweichungen: jede Stelle, an der die Implementierung vom dokumentierten Sollzustand abweicht.

Ergebnis: ein modulspezifisches Debugging-Profil, das festlegt, welche Dimensionen mit welcher Priorität und Tiefe untersucht werden – bekannte Bugs des Bereichs zuerst.

3. Scope-Lokalisierung (untergeordnet)
Identifiziere die Code-Stellen, an denen die Implementierung tatsächlich lebt – Dateien, Funktionen, Berührungspunkte zu bestehendem Code. Dies dient nur als Karte für die Fix-Arbeit, nicht als Filter für die fachliche Analyse aus Schritt 1/2.

4. Baseline-Fixierung
Erfasse das intendierte funktionale Verhalten und die Design-Charakteristik. Definiere diese Baseline als invariant. Unterscheide explizit:
- intendiertes Verhalten (zu erhalten, auch wenn unelegant),
- fehlerhaftes Verhalten (zu korrigieren),
- Grauzonen (im Zweifel als intendiert behandeln und in der Abschlussliste als „klärungsbedürftig" markieren).
Hinweis: Best Practices verändern die Baseline NICHT eigenmächtig – sie sind Prüf-Referenz. Würde eine Best-Practice-Angleichung das Verhalten/Design ändern, gehört sie in Liste (b) von Schritt 7, nicht in einen stillen Fix.

5. Iterative Loops mit ansteigender Tiefe (mit CBR-Lookup bei jedem Fund)
- Analysiere entlang der in Schritt 2 abgeleiteten Dimensionen, in der dort festgelegten Priorisierung (bereichsspezifische Almanach-Dimensionen zuerst).
- Bei JEDEM gefundenen Verdacht/Bug ZUERST das vorhandene Wissen abgleichen (Case-Based Reasoning, 4 Phasen):
  • RETRIEVE: In ~/proggs/.claude/agent-memory/shared/bug-cases.jsonl nach Symptom/Fehlermeldung suchen UND – da jetzt im Bereich ein Fehler vorliegt – den VOLLTEXT des zugehörigen Almanachs lesen (Stufe B: Read ohne limit; ab dem ersten Fehler reicht der Kurzcheck nicht mehr).
  • REUSE: Ist es ein bekannter Bug, den dokumentierten, funktionserhaltenden Fix als ERSTEN Lösungsansatz anwenden (schnellster, sicherster Pfad).
  • REVISE: Passt der bekannte Fix nicht 1:1, gezielt anpassen.
  • (RETAIN folgt in Schritt 7.)
- Behebe gefundene Bugs unmittelbar – ausschließlich durch minimal-invasive, design- und funktionserhaltende Korrekturen, die dem Best-Practice-Sollzustand des Bereichs entsprechen.
- Bei jedem Fix dokumentiere kurz, warum die Änderung ausschließlich den Bug behebt und keine weiteren Verhaltens- oder Designaspekte berührt; nenne ggf. den genutzten Almanach-/Best-Practice-Eintrag.
- Loop-Tiefenstufen:
  Loop 1: offensichtliche logische Fehler im Hauptpfad + direkte Treffer aus dem Almanach.
  Loop 2: Randfälle, implizite Annahmen, dokumentierte Versions-/Umgebungs-Fallen.
  Loop 3: Wechselwirkungen, Reihenfolgeprobleme, subtile Zustandskorruption.
  Loop 4+: nicht offensichtliche Inkonsistenzen, fachliche Edge Cases entlang der Domänenregeln aus dem Funktionsprofil und der bereichsspezifischen Almanach-Dimensionen.
- Bugs, deren Fix Baseline oder Design verändern würde: nicht eigenmächtig ausführen, sondern in Schritt 7, Liste (b) mit Trade-off als Vorschlag auflisten.
- Fixe immer getreu Direktive #3 (Resilient Bugfixing): Root Cause statt Symptom, verwandte Fehlerquellen prüfen, Funktionalität niemals entfernen/auskommentieren/schlucken.

6. Abbruchbedingung
Wiederhole, bis in zwei aufeinanderfolgenden Loops keine design- und funktionsneutral behebbaren Bugs mehr gefunden werden.

7. Abschluss-Verifikation + Wissens-Rückschreibung (Compound Intelligence)
Zuerst drei getrennte Ergebnis-Listen:
(a) Behobene Bugs – was war falsch, wie behoben, warum Baseline unverändert; bei bekannten Bugs: welcher Almanach-/bug-cases-Eintrag genutzt wurde.
(b) Nicht eigenmächtig behoben – Bugs, deren Fix Baseline oder Design (oder eine Best-Practice-Angleichung mit Verhaltensänderung) berührt hätte; mit Trade-off und Empfehlung.
(c) Klärungsbedürftige Grauzonen – Verhalten, das innerhalb der Baseline mehrdeutig war.

Danach die Wissens-Rückschreibung (RETAIN – Pflicht, damit Recherche/Funde nicht „verkommen"):
- Jeden NEU erlebten Bug (der noch nicht im Almanach stand) in den passenden ~/proggs/bugs/<kategorie>/<bereich>.md eintragen (Symptom, Root Cause, betroffene Versionen, funktionserhaltender Fix, Quelle) – Kurzcheck-Sektion UND Volltext; Stand-Header aktualisieren.
- Jeden Fall zusätzlich als Zeile in ~/proggs/.claude/agent-memory/shared/bug-cases.jsonl anhängen (nur appenden).
- Fehlte für einen Bereich ein Almanach (Schritt 0e): das Anlegen vorschlagen (Skill bug-almanach-recherche), nicht ad hoc selbst recherchieren.
- Lieferten die Funde verallgemeinerbare Erkenntnisse: die zugehörige ~/proggs/best-practices/<kategorie>/<bereich>.md ergänzen (Kurzcheck + Volltext).

Sei allumfassend und kreativ; achte besonders auf subtile, nicht offensichtliche Bugs entlang der modulspezifischen UND der bereichsspezifischen (Almanach-)Debugging-Dimensionen – niemals auf Kosten bestehender Funktionalität oder des Designs.
