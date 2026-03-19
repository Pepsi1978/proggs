# Procedural Memory — Bewährte Workflows

> Wiederverwendbare Schritt-für-Schritt-Abläufe für häufige Aufgaben.
> Jedes Verfahren wurde mindestens 2x erfolgreich angewendet.
> Updated by: all agents after repeated successful workflows
> Read by: ALL agents before starting work

## Wie dieses Dokument funktioniert

Agents sollen dieses Dokument VOR der Arbeit lesen und prüfen, ob ein passender Workflow existiert.
Wenn ein Agent eine Aufgabe 3x ähnlich löst, soll er den Ablauf hier als neuen Workflow eintragen.

---

## QuizVerse: Neue Quiz-Fragen hinzufügen

**Wann**: Wenn neue Fragen für eine Kategorie/Schwierigkeit erstellt werden sollen
**Schritte**:
1. Bestehende Dateien prüfen: `QuizVerse/app/src/main/java/com/quizverse/app/data/prepopulate/questions/`
2. Nächste fortlaufende Nummer für die Datei ermitteln (z.B. FilmQuestionsEasy6.kt)
3. 50 Fragen pro Datei erstellen, Format: `Question(text=, options=listOf(), correctAnswer=, ...)`
4. **WICHTIG**: Jede faktische Aussage mit Web-Recherche verifizieren (Lesson aus FAILURES.md)
5. In `QuestionSeeder.kt` importieren UND in der `seedQuestions()`-Funktion registrieren
6. `questionCount` für die Kategorie aktualisieren
7. Build prüfen (Kotlin-Compiler fängt Syntaxfehler)
8. Committen mit `#NNN - Add [N] [difficulty] [category] questions`

**Bekannte Fallstricke**:
- Vergessen, den Import UND die Registrierung in QuestionSeeder.kt hinzuzufügen
- questionCount nicht aktualisiert → App zeigt falsche Fortschrittsanzeige
- Fakten nicht geprüft → falsche Antworten in der App
- **ABSTURZ-GEFAHR**: Mehr als 50 Fragen auf einmal generieren → Agent-Crash durch Kontext-/Output-Überlauf! Immer in Chunks von max 50 arbeiten (siehe "Bulk Content Generation" Workflow unten)

---

## Neues Projekt anlegen (Cross-Platform)

**Wann**: Wenn ein neues Projekt in ~/proggs/ erstellt werden soll
**Schritte**:
1. Ordner unter `~/proggs/` erstellen (z.B. `~/proggs/MeinProjekt/`)
2. **NIEMALS** ein neues GitHub-Repo erstellen — alles geht in `Pepsi1978/proggs`
3. Architektur planen: macOS (Swift/AppKit) + Windows (C#/WPF) parallel
4. README.md erstellen mit Programmbeschreibung + Installation (DE, für Anfänger)
5. Build-System einrichten (Xcode Projekt, .sln, package.json, Cargo.toml)
6. quality-gate Agent starten nach erstem Feature
7. Committen und Pushen

**Bekannte Fallstricke**:
- Neues Repo erstellt statt Unterordner → CLAUDE.md-Verstoß
- Python für GUI verwendet → User will das nicht

---

## Self-Improve Durchlauf

**Wann**: Bei `/self-improve` oder expliziter Anfrage
**Schritte**:
1. Platform erkennen (Darwin/MINGW/Linux)
2. Stufe 0: session-scores.jsonl Plausibility Check + evolution-analyst
3. Stufe 1: env-checker Agent (full/quick)
4. Stufe 2: Researchers parallel (R1-R8; R1+R5+R6+R8 immer, R2-R4 wenn Cache >7 Tage)
5. Stufe 3: Challenger Gate (Verbesserungen prüfen) → Implementieren → Report → Meta-Improve
6. Stufe 4: Creative Research (6 Linsen + Benchmark)
7. Stufe 5: Super Intelligenz (R8-Ergebnisse umsetzen)
8. Stufe 6: Fehler-Dauerhaftigkeit (3 parallele Scans + Fixes)
9. Agents auf Robustness-Protocol prüfen (maxTurns, Tool-Fehler, Kontext-Schutz)
10. Cross-Platform-Sync: Alle Änderungen nach ~/proggs/claude-code-setup/ kopieren
11. Shell-Updates als ALLERLETZTER Schritt (nach Benutzer-Bestätigung)
12. Committen und Pushen

**Bekannte Fallstricke**:
- settings-reference.json vergessen zu aktualisieren bei Hook-Änderungen
- Git push ohne vorheriges pull --rebase → Merge-Konflikte
- Shell-Updates (PowerShell, Git, Node.js, ADB) während des Laufs zerstören Terminals
- Session-Scorer kann bei Claude-Code-Updates neues Transcript-Format nicht parsen
- Turn-Counter wird nicht zurückgesetzt → Intent-Drift-Schutz unwirksam in neuer Session

---

## Bulk Content Generation (Anti-Crash Chunking)

**Wann**: Wenn grosse Mengen aehnlicher Inhalte generiert werden sollen (Quiz-Fragen, Test-Daten, Seed-Daten, Uebersetzungen, etc.)
**Schwelle**: Ab 50+ Items MUSS gechukt werden. Unter 50: direkt generieren ist OK.

**Schritte**:
1. Gesamtmenge bestimmen (z.B. 200 Fragen)
2. In Chunks von max 50 aufteilen (200 → 4 Chunks à 50)
3. **Fuer sequentielle Ausfuehrung**: Chunk 1 generieren → in Datei schreiben → Chunk 2 generieren → anhaengen → ...
4. **Fuer parallele Ausfuehrung (BEVORZUGT)**: Mehrere coder-Agents spawnen, jeder generiert einen Chunk:
   - Coder 1: Items 1-50 → schreibt in Datei A
   - Coder 2: Items 51-100 → schreibt in Datei B
   - Coder 3: Items 101-150 → schreibt in Datei C
   - Coder 4: Items 151-200 → schreibt in Datei D
5. Am Ende: Alle Teildateien zusammenfuehren oder einzeln registrieren

**Warum**:
- 200+ Items auf einmal → Agent-Kontext und Output-Buffer ueberlaufen → interner Fehler → Totalverlust
- 50 Items bleiben sicher innerhalb der Token-Limits
- Parallel: 4x schneller als sequentiell

**Bekannte Fallstricke**:
- Chunk-Grenzen koennen zu Duplikaten fuehren → nach dem Zusammenfuehren auf Duplikate pruefen
- Nummerierung/IDs muessen chunk-uebergreifend konsistent sein
- Bei Fakten-basierten Inhalten: Jeder Chunk braucht eigene Web-Recherche-Validierung

---

## Tampermonkey-Skript aktualisieren

**Wann**: Bei Änderungen an Userscripts in ~/proggs/Tampermonkey/
**Schritte**:
1. Datei bearbeiten
2. **PFLICHT**: @version im UserScript-Header erhöhen (x.y → x.y+1)
3. Committen mit `#NNN - Update [Skriptname] to v[Version]`
4. Pushen

**Bekannte Fallstricke**:
- Version nicht erhöht → Tampermonkey erkennt kein Update
