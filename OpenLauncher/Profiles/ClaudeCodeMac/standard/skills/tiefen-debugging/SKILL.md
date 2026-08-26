---
name: tiefen-debugging
description: "Nutze diesen Skill IMMER bei 'komplettes/tiefgreifendes/iteratives Debugging fuer [App/Software/Modul]', 'Tiefen-Debugging', 'debugge die App', 'mach den Skill fuer App X', 'suche alle Bugs/Logikfehler' (Brille bugs) ODER 'Performance-Debugging', 'Performance-Analyse', 'finde Engpaesse', 'optimiere die Performance' (Brille performance) — auch bei 'mach das gleiche nochmal fuer App Y'. Der Scope ist EXAKT das, was Frank ansagt (ganze App = Standard bei blossem App-Namen; nur wenn er es explizit sagt: ein Modul oder die letzte Aenderung). Fuehrt das 7-Schritte-Protokoll aus: Bug-Almanach-Grundierung ZUERST, Funktions-/Lastprofil, priorisierte Dimensionen, CBR-Loops mit steigender Tiefe, verhaltens- und designneutrale Fixes, projektgerechte Build-/Start-Verifikation, drei Ergebnis-Listen und Wissens-Rueckschreibung (RETAIN) in Almanach + bug-cases. Funktioniert fuer JEDE Software (Android, Server, Desktop, Extension …). NICHT fuer EINEN konkreten bekannten Bug (dafuer systematic-debugging)."
---

# Tiefen-Debugging — 7-Schritte-Protokoll (Brille: bugs | performance)

Ein tiefgreifender, iterativer Analyse-Durchlauf, der bestehende Funktionalität, Verhalten,
Schnittstellen und Design NIEMALS verändert oder beeinträchtigt. Zwei Brillen, EIN Gerüst:
`bugs` (Logik-/Codefehler) und `performance` (Engpässe). Vorhandenes Bug-Wissen
(Bug-Almanach, Bug-Fall-Datenbank) und Best Practices werden AUSNAHMSLOS genutzt —
präventiv (vorher nachschlagen), reaktiv (bei jedem Fund matchen) und lernend (neue Funde
zurückschreiben). Entstanden aus zwei Real-Läufen über CortexAndroid am 2026-07-02
(15 Bugfixes bzw. 8 Optimierungen).

## Parameter bestimmen (zuerst)

**Brille** (Analyse-Fokus):

| Franks Wortlaut enthält … | Brille |
|---------------------------|--------|
| Debugging, Bugs, Fehler, Logikfehler, "checke alles ab" | `bugs` |
| Performance, langsam, Engpässe, optimieren, flüssiger | `performance` |
| beides / unklar | kurz nachfragen (EINE Frage, dann loslegen) |

**Scope** (was analysiert wird) — **Franks Ansage hat IMMER Priorität.** Was er vor oder
mit dem Skill-Aufruf ansagt, gilt exakt; nichts stillschweigend verkleinern:

| Franks Ansage | Scope |
|---------------|-------|
| "für App X", "diese App", nur ein App-/Projektname | die KOMPLETTE genannte App/Software (Standard) |
| ein Modul/Ordner/Feature benannt | genau dieser Teil |
| EXPLIZIT "die letzte Implementierung/Änderung" | nur die jüngste Änderung (per git log eingrenzen) |
| kein Ziel erkennbar | kurz nachfragen (EINE Frage) |

Dann die passende Dimensionsliste laden (enthält auch die Schritt-1-Profil-Checkliste
und die brillen-spezifischen Loop-Tiefenstufen):
- `references/dimensionen-bugs.md` (Brille bugs)
- `references/dimensionen-performance.md` (Brille performance)

## Grundregeln (beide Brillen, ausnahmslos)

- **Verhaltens- und Designneutralität:** Jeder Fix muss funktional und visuell äquivalent
  sein. Würde eine Änderung Verhalten, Design, API-Form, Fehlerzeitpunkte oder beobachtbare
  Seiteneffekt-Reihenfolgen berühren → NICHT eigenmächtig ausführen, sondern in Liste (b)
  des Abschlussberichts mit Trade-off vorschlagen.
- **Direktive #3 (Resilient Bugfixing):** Root Cause statt Symptom, verwandte Quellen
  prüfen, Funktionalität niemals entfernen/auskommentieren/schlucken.
- **Sichtbar arbeiten:** TaskCreate-Liste für die Phasen, Pre-Flight-Plan vor den Fixes
  (3+ Dateien → Pflicht), Commit-Marker pro abgeschlossenem Block.

## Die 7 Schritte

### Schritt 0 — Wissens-Grundierung (PFLICHT, vor jeder Analyse)

1. Technische Bereiche des Scopes bestimmen (z.B. Kotlin, Compose, Gradle, Networking,
   Server/FastAPI, .NET/WPF, Swift, TypeScript, Chrome-Extension, Python, APIs …).
2. `~/proggs/bugs/README.md` prüfen: existiert pro Bereich ein Almanach?
3. Pro Bereich lesen — Reihenfolge erst Almanach, dann Best Practices:
   - Normal (Stufe A): NUR Kurzcheck (`Read` mit `limit=80`) von
     `~/proggs/bugs/<kategorie>/<bereich>.md`, danach
     `~/proggs/best-practices/<kategorie>/<bereich>.md` (limit=80).
   - Hochrisiko (Stufe C: r8, firebase-billing, claude-hooks, claude-config): VOLLTEXT.
   - Versions-Anker abgleichen; neuere Version als dokumentiert → vermerken.
4. Bei Brille `performance`: die Kurzchecks gezielt mit Performance-Blick lesen
   (Recomposition, Hot-Path-Fallen, Logging-Kosten, N+1-IO, Blocking-Calls).
5. Fehlt ein Almanach für einen relevanten Bereich: als Lücke notieren (Schritt 7 schlägt
   `bug-almanach-recherche` vor) — NIE selbst ad hoc recherchieren.

Ergebnis: Liste bereichsspezifisch bekannter Fallen + Best-Practice-Sollzustand — beides
fließt verbindlich in Schritt 2 und 5 ein. Warum zuerst: In den Real-Läufen waren die
direkten Almanach-Treffer die schnellsten und sichersten Funde (3 von 15 sofort).

### Schritt 1 — Funktions- (und bei performance: Last-)Charakterisierung

Alle Quelldateien des Scopes selbst lesen (bei >500-Zeilen-Dateien NIE per Agent editieren
lassen). Präzises Profil nach der vollständigen **Profil-Checkliste in der geladenen
references-Datei** erstellen — Kurzfassung: Zweck/Verantwortung, Funktionstypen,
Inputs/Outputs mit realistischen Wertebereichen, fachliche Invarianten, Lebenszyklus/
Aufrufmuster, externe Wechselwirkungen, implizite Kontextabhängigkeiten. Bei `performance`
zusätzlich das Lastprofil: Häufigkeit/Hot-Path-Charakter, Skalierungsdimensionen,
kritische Metrik, Ressourcenprofil. Das Profil — nicht der Code-Ort — ist die Grundlage
aller folgenden Schritte.

### Schritt 2 — Analyse-Dimensionen ableiten

Drei Quellen verschmelzen: (i) Profil aus Schritt 1, (ii) Almanach-Fallen aus Schritt 0
(jede anwendbare dokumentierte Falle = PFLICHT-Dimension mit höchster Priorität),
(iii) Best-Practice-Abweichungen. Die generische Dimensionsliste steht in der geladenen
references-Datei — jede Dimension auf Anwendbarkeit prüfen, priorisieren, nicht anwendbare
mit kurzer Begründung überspringen.

### Schritt 3 — Scope-Lokalisierung (untergeordnet)

Dateien/Funktionen/Hot-Paths als Karte für die Fix-Arbeit — kein Filter für die Analyse.

### Schritt 4 — Baseline fixieren

Explizit unterscheiden:
- **intendiertes Verhalten** — zu erhalten, auch wenn unelegant (INVARIANT: Verhalten,
  Design/UX-Charakteristik, Schnittstellen/API-Form/Fehlertypen, beobachtbare
  Seiteneffekt-Reihenfolgen),
- **fehlerhaftes Verhalten** — DAS ist zu korrigieren (die Baseline friert keine Bugs ein),
- **Grauzonen** — im Zweifel als intendiert behandeln und in Liste (c) melden.

Best Practices verändern die Baseline NICHT eigenmächtig — sie sind Prüf-Referenz. Würde
eine Best-Practice-Angleichung Verhalten/Design ändern → Liste (b), kein stiller Fix.
Performance-Warnung: Lazy/Eager-Wechsel verschieben Fehlerzeitpunkte, Caching kann Stale-
Daten erzeugen, Parallelisierung ändert Seiteneffekt-Reihenfolgen, Batching ändert
Latenz-Wahrnehmung, Off-Main-Auslagerung weicht Synchronisierungs-Annahmen auf —
nur bei nachweisbarer Äquivalenz erlaubt; kennt der Almanach für eine geplante Optimierung
bereits eine dokumentierte Falle, gilt sie verbindlich.

### Schritt 5 — Iterative Loops mit CBR (Tiefe steigend)

Analysieren entlang der Schritt-2-Priorisierung — bereichsspezifische Almanach-Dimensionen
zuerst. Die brillen-spezifischen Loop-Tiefenstufen (Loop 1–4+) stehen in der geladenen
references-Datei.

Bei JEDEM Fund zuerst CBR:
- RETRIEVE: `~/proggs/.claude/agent-memory/shared/bug-cases.jsonl` greppen UND — ab dem
  ersten Fund im Bereich — den VOLLTEXT des zugehörigen Almanachs lesen (Stufe B).
- REUSE/REVISE: dokumentierten funktionserhaltenden Fix zuerst, bei Bedarf anpassen.
- RETAIN folgt in Schritt 7.

Fixes sofort umsetzen (minimal-invasiv), pro Fix kurz dokumentieren, warum er
verhaltens- und designäquivalent ist + welcher Almanach-/BP-Eintrag genutzt wurde.
Gleichartige Änderungen an 3+ Stellen: Python-Batch mit Kontext-Check statt Handarbeit
(im Real-Lauf: 29 CancellationException-Stellen in einem Batch, 0 Fehler).

### Schritt 6 — Abbruchbedingung

Wiederholen, bis zwei aufeinanderfolgende Loops keine neutral behebbaren Funde mehr
liefern. Danach die technische Verifikation (siehe Ablauf unten).

### Schritt 7 — Abschluss + Wissens-Rückschreibung (RETAIN)

Drei getrennte Ergebnis-Listen ausgeben:
- **(a) Behoben** — was, wie, warum Baseline unverändert, genutzte Almanach-Einträge.
- **(b) Nicht eigenmächtig** — Fixes, die Verhalten/Design berühren würden (auch
  Best-Practice-Angleichungen mit Verhaltensänderung), mit Trade-off und Empfehlung.
- **(c) Grauzonen** — mehrdeutiges Verhalten / unklare Äquivalenz.

Dann PFLICHT-Rückschreibung (Funde dürfen nicht verkommen):
- Jeden NEUEN verallgemeinerbaren Fund in den passenden Almanach
  (`bugs/<kategorie>/<bereich>.md`): Kurzcheck-Zeile UND Volltext-Eintrag UND
  Stand-Header-Vermerk.
- Jeden Fall als Zeile an `bug-cases.jsonl` ANHÄNGEN (nur append; Near-Miss markieren,
  wenn statisch gefunden bevor er live zuschlug).
- Verallgemeinerbare Erkenntnisse, die noch nicht in den Best Practices stehen → dort
  ergänzen. Fehlender Almanach → `bug-almanach-recherche` vorschlagen.

## Technischer Ablauf (Rahmen um die 7 Schritte)

1. TaskCreate-Liste anlegen (Phasen: Grundierung+Profil, Loops, Fixes+Verifikation+RETAIN).
2. Vor den Fixes: Pre-Flight-Plan (Dateien + was + warum + Risiko) sichtbar ausgeben.
3. Version bumpen (Patch, sichtbarer Zeitstempel nach dem `VERSION_BUMPED_AT`-Muster).
4. **Commit + Push VOR dem Build** (Regel commit-before-build; nur eigene Dateien,
   atomarer Pfad-Commit `git commit -m "#NNN - ..." -- <pfade>`).
5. **Verifikation je nach Projekttyp** — das Ziel ist immer: gebaut/gestartet + Logs
   sauber, nicht nur "kompiliert":

   | Projekttyp | Verifikation |
   |------------|--------------|
   | Android-App | `./gradlew assembleDebug` → `adb install` → App starten → Crash-Buffer + strukturiertes Log (Version korrekt, kein FATAL) |
   | Server-Dienst (VPS) | Syntax-/Import-Check (`py_compile` o.ä.) → Deploy (scp + compose up) → Container `healthy` + Versions-Log |
   | Desktop (.NET/Swift/TS) | `dotnet build` / `swift build` / `tsc --noEmit` → App starten → Log/Fenster prüfen |
   | Browser-Extension / Web | Build/Lint → laden/öffnen → Console fehlerfrei |
   | Nicht baubar/startbar | ehrlich sagen; Verifikation = statische Prüfung + Begründung |

6. Almanach-/bug-cases-Rückschreibung als eigener Commit.
7. Abschluss: drei Listen + Task-Completion-Boxen + Status-Meldung.

## Was NIEMALS passieren darf

- Analyse/Fixes ohne die Almanach-Kurzchecks aus Schritt 0 beginnen
- Einen Fund fixen, indem Funktionalität entfernt/deaktiviert/geschluckt wird
- Verhalten/Design-berührende Änderungen eigenmächtig ausführen (gehören in Liste b)
- Die Wissens-Rückschreibung weglassen oder "auf später" verschieben
- Performance-Behauptungen als Messwerte ausgeben (statisch hergeleitet ist nicht
  gemessen — ehrlich kennzeichnen, Baseline-Messung als Folgeaufgabe vorschlagen)
- Die Verifikation überspringen, wenn das Projekt baubar/startbar ist (Verifikationspflicht)
- Franks angesagten Scope stillschweigend verkleinern (z.B. nur die letzten Änderungen
  prüfen, obwohl er die ganze App genannt hat)

## Haltung

Sei allumfassend und kreativ; achte besonders auf subtile, nicht offensichtliche Funde
entlang der modulspezifischen UND der bereichsspezifischen (Almanach-)Dimensionen —
niemals auf Kosten bestehender Funktionalität oder des Designs.
