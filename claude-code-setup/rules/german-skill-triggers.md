# Deutsche Trigger-Map fuer Skills und Plugins (KRITISCH)

## Warum diese Regel existiert

Der Benutzer spricht Deutsch (oft via Whisper Speech-to-Text). Die meisten Skill-
und Plugin-Beschreibungen sind auf Englisch. Diese Regel ist die Uebersetzungsschicht
zwischen deutschen Anfragen und englischen Skills.

**PFLICHT**: Bei JEDER Benutzer-Anfrage diese Map mental durchgehen und pruefen ob
ein Skill oder Plugin passt. Auch bei 1% Wahrscheinlichkeit den Skill aufrufen.

**WICHTIG**: Jede Zuordnung wurde gegen die tatsaechliche Skill-Beschreibung verifiziert.
Die Trigger zeigen auf den Skill der WIRKLICH das tut was der Benutzer will — nicht
auf den Skill der aehnlich klingt.

---

## 1. Committen, Pushen & Git

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "committe", "mach ein Commit" | `commit-commands:commit` | NUR lokaler Commit, kein Push, kein PR |
| "committe und pushe", "ab damit", "PR machen" | `commit-commands:commit-push-pr` | Commit + Push + GitHub-PR in einem Zug |
| "mach das rückgängig", "undo", "revert" | `undo-changes` | Revert per `git revert` (nie force-push), lokal UND auf GitHub |
| "Branches aufräumen", "alte Branches löschen" | `commit-commands:clean_gone` | Loescht lokale Branches die remote schon geloescht sind |
| "Worktree", "isoliert arbeiten", "paralleler Branch" | `git:worktrees` | Git-Worktree erstellen/verwalten fuer isolierte Parallel-Arbeit |
| "Issue analysieren", "GitHub Issue anschauen" | `git:analyze-issue` | GitHub Issue lesen und technische Spezifikation erstellen |

**NICHT verwechseln**: "pushe das" (ohne PR) → kein Skill noetig, direkt `git push` ausfuehren.

## 2. Code Review (3 verschiedene Tools — richtig waehlen!)

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "reviewe den PR", "PR Review" | `code-review:code-review` | Analysiert einen **GitHub-PR** mit 5 parallelen Agenten + Scoring (nur Issues ≥80/100 werden als GitHub-Kommentar gepostet) |
| "prüfe meine Änderungen", "schau dir das an" | `pr-review-toolkit:review-pr` | Prueft **lokale Aenderungen** (kein PR noetig) mit 6 Spezialisten (Kommentare, Tests, stille Fehler, Typen, Code-Qualitaet, Vereinfachung) |
| "CodeRabbit", "externer Review" | `coderabbit:review` | Ruft den **externen CodeRabbit-Cloud-Dienst** auf — die Analyse macht NICHT Claude sondern CodeRabbit.ai |
| "zweite Meinung", "was sagt Codex?", "Gemini fragen" | `second-opinion:second-opinion` | Holt Review von einem **anderen LLM** (OpenAI Codex oder Google Gemini CLI) — nicht von Claude |

**Entscheidungshilfe**: PR auf GitHub? → `code-review`. Lokale Aenderungen? → `pr-review-toolkit`. Externes Tool? → `coderabbit` oder `second-opinion`.

## 3. Debugging & Fehler

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "finde den Bug", "warum geht das nicht?", "funktioniert nicht" | `superpowers:systematic-debugging` | 4-Phasen-Methode: Root Cause → Pattern → Hypothese → Fix. Eisernes Gesetz: KEIN Fix ohne vorherige Root-Cause-Analyse |
| "Root Cause", "Ursache finden", "5x Warum?" | `kaizen:why` | Iterative 5-Whys-Analyse die von Symptom zur fundamentalen Ursache bohrt |
| "Ursache-Wirkungs-Analyse", "Fishbone", "Ishikawa" | `kaizen:cause-and-effect` | Fishbone-Diagramm ueber 6 Kategorien |
| "was ist kaputt?", "Fehler suchen", "Analyse" | `tool-check` | Systematischer Code-Scanner: 4 Analyse-Schleifen (Oberflaeche → Tief → Architektur → UI), MANUELL getriggert |

**NICHT verwechseln**: "Fehler fixen" bei bekanntem Bug → `systematic-debugging`. "Code prüfen" ohne bekannten Bug → `tool-check`.

## 4. Planung & Design (Reihenfolge beachten!)

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "ich will ein Feature bauen", "neue Funktion" | `superpowers:brainstorming` | Erkundet Anforderungen durch Fragen, zeigt 2-3 Ansaetze, schreibt Design-Spec. Ruft DANACH automatisch `writing-plans` auf |
| "wie sollen wir das machen?", "Ideen sammeln" | `superpowers:brainstorming` | Gleich — immer VOR der Planung, nie ueberspringen |
| "mach einen Plan", "plane das" | `superpowers:writing-plans` | Erstellt detaillierten Implementierungsplan mit TDD-Schritten (Test zuerst), exakten Dateipfaden, und 2-5min Haeppchen. NUR aufrufen wenn Design-Spec schon existiert |
| "Architektur planen", "System entwerfen" | `architect` Agent | Tiefes Reasoning ueber Systemarchitektur (Opus), bevor Code geschrieben wird |
| "Spezifikation schreiben", "Anforderungen klären" | `sdd:brainstorm` | Verfeinert rohe Ideen zu vollstaendigen Designs — aehnlich wie superpowers:brainstorming aber aus dem SDD-Framework |

**Reihenfolge**: Brainstorming → Plan → Implementation. Nie direkt zum Plan springen ohne Brainstorming.

## 5. Testen (3 verschiedene Ansaetze — richtig waehlen!)

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "schreib Tests für meinen Code", "Testabdeckung erhöhen" | `tdd:write-tests` | Schreibt **nachtraeglich** Tests fuer existierenden Code via Agenten-Schwarm (Haiku bewertet Komplexitaet, Agenten schreiben Tests parallel) |
| "TDD", "Test zuerst schreiben" | `superpowers:test-driven-development` | Erzwingt **strikte TDD-Disziplin**: erst Test schreiben, sehen wie er fehlschlaegt, dann Code. Kein Produktionscode ohne vorherigen fehlschlagenden Test |
| "Tests fixen", "kaputte Tests reparieren" | `tdd:fix-tests` | Repariert systematisch fehlschlagende Tests nach Business-Logik-Aenderungen |
| "Go testen", "Go Tests" | `everything-claude-code:go-test` | Go-spezifisch: Table-driven Tests, `go test -cover`, 80%+ Coverage |
| "Kotlin testen", "Kotlin Tests" | `everything-claude-code:kotlin-test` | Kotlin-spezifisch: Kotest + MockK + Kover Coverage |
| "E2E Test", "Browser testen", "Playwright" | `everything-claude-code:e2e` | End-to-End Tests mit Playwright, Screenshots/Videos/Traces |

**Entscheidungshilfe**: Code existiert, Tests fehlen? → `tdd:write-tests`. Neuer Code wird geschrieben? → `superpowers:tdd`. Tests sind rot nach Aenderung? → `tdd:fix-tests`.

## 6. Sicherheit (3 komplett verschiedene Scopes!)

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "ist mein Code sicher?", "Sicherheit beim Coden" | `everything-claude-code:security-review` | **Passives Wissen**: gibt Claude Sicherheitsregeln (OWASP, Secrets, Injection) als Kontext — fuehrt KEINEN Scan durch |
| "scanne meinen Code", "SAST", "Schwachstellen finden" | `aikido:scan` | **Aktiver SAST-Scanner**: scannt Quellcode-Dateien auf Vulnerabilities und Secrets, fixt automatisch (max 3 Runden) |
| "ist meine Claude-Config sicher?", "AgentShield" | `everything-claude-code:security-scan` | Scannt die **Claude-Code-Konfiguration** (~/.claude/), nicht den Quellcode — prueft CLAUDE.md, settings.json, hooks auf Fehlkonfiguration |
| "Semgrep", "statische Analyse", "Semgrep-Regeln" | `static-analysis:semgrep` | Fuehrt Semgrep-Scan durch mit parallelen Agenten pro Sprache |
| "Prompt Injection prüfen" | Parry-Scanner (automatisch via Hook) | Lauft bereits automatisch bei jedem UserPromptSubmit |

**Entscheidungshilfe**: Quellcode scannen → `aikido:scan`. Claude-Konfiguration scannen → `security-scan`. Beim Coden Sicherheit beachten → `security-review`.

## 7. Selbstverbesserung & Lernen (verschiedene Zwecke!)

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "verbessere dich", "self-improve", "werde besser" | `self-improve` | Systematische Verbesserung der gesamten Programmierumgebung (10-30min, token-intensiv) |
| "was hast du gelernt?", "Learnings extrahieren" | `claudeception` | Extrahiert wiederverwendbares Wissen aus der aktuellen Session |
| "was lief gut?", "CLAUDE.md aktualisieren" | `claude-reflect:reflect` | Sammelt Korrekturen aus der Session und schreibt sie in CLAUDE.md — **langfristiges Lernen** |
| "bewerte deine Antwort", "war das gut?" | `reflexion:reflect` | Bewertet die **aktuelle Ausgabe** mit strengem Scoring (1-5) — **sofortige Qualitaetspruefung** |
| "prüfe die Umgebung", "Environment Check" | `env-checker` Agent | Prueft installierte Tools, Versionen, Hooks, Plugins, Security-Patches |
| "konfiguriere", "Settings ändern", "Hook einrichten" | `update-config` | Aendert settings.json: Permissions, Hooks, Env-Vars |
| "Tastenkürzel ändern", "Keybinding" | `keybindings-help` | Hilft bei ~/.claude/keybindings.json Anpassungen |

**NICHT verwechseln**: `claude-reflect` = langfristig lernen (CLAUDE.md). `reflexion:reflect` = aktuelle Antwort bewerten.

## 8. Suche & Erinnerung (2 verschiedene Speichersysteme!)

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "was haben wir letzte Session gemacht?" | `episodic-memory:search-conversations` | Durchsucht **archivierte Konversationen** in SQLite-Datenbank, gibt Zusammenfassung mit Quellenangaben |
| "was wissen wir über X?", "Erinnerung suchen" | `claude-mem:mem-search` | Durchsucht das **claude-mem Observations-System** (Tree-sitter-geparst, Timeline, Token-Kosten) — umfassender als episodic-memory |
| "wie haben wir das Problem gelöst?", "früherer Fix" | Beide nacheinander: erst `claude-mem:mem-search`, dann `episodic-memory` falls nichts gefunden |

## 9. Recherche & Dokumentation

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "recherchiere im Internet", "such im Web" | `researcher` Agent (Sonnet, 3-5 parallel spawnen) | Schnelles Web-Lookup ueber WebSearch/WebFetch |
| "tiefe Recherche", "umfassend recherchieren" | `everything-claude-code:deep-research` | Multi-Source Deep Research mit Firecrawl + Exa, liefert zitierten Bericht |
| "was sagt die Doku?", "offizielle Dokumentation" | context7 MCP (`resolve-library-id` → `query-docs`) | Laedt aktuelle Dokumentation fuer beliebige Bibliothek |
| "Doku schreiben", "README aktualisieren" | `docs:update-docs` | Multi-Agent-Workflow der docs/, READMEs, JSDoc, API-Doku aktualisiert |

## 10. UI, Design & Medien

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "baue eine Webseite", "HTML/CSS", "Web-UI" | `frontend-design:frontend-design` | Erstellt professionelle, Store-qualitaet Frontends mit hohem Design-Anspruch |
| "Playground bauen", "interaktiver Explorer" | `playground:playground` | Erstellt einzelne HTML-Datei mit Live-Preview und Konfigurations-Controls |
| "Präsentation bauen", "Slides erstellen" | `everything-claude-code:frontend-slides` | Animationsreiche HTML-Praesentation oder PowerPoint-Konvertierung |
| "finde ein Icon", "Icon suchen" | `better-icons` MCP (200k+ Icons) | Durchsucht 150+ Icon-Sets, gibt SVG/PNG zurueck |
| "finde einen Sound", "Sound-Effekt" | `sound-search` | Durchsucht Freesound.org (CC0), spielt Vorschau mit `afplay`, konvertiert zu OGG fuer Android |
| "Bild generieren", "Bild erstellen" | `everything-claude-code:fal-ai-media` | Text-to-Image via fal.ai (Nano Banana), auch Video und Audio |

## 11. Sprach-spezifische Skills

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "Swift bauen", "iOS/macOS App" | `apple-platform-build-tools:building-apple-platform-products` | Baut, testet, archiviert mit xcodebuild/swift — absorbiert Build-Logs |
| "Android App", "Kotlin/Compose" | `android-dev` | Produktion-qualitaet Android nach Google-Architektur und NowInAndroid |
| "Go Code reviewen" | `everything-claude-code:go-review` | Idiomatic Go, Concurrency, Error Handling Review |
| "Python Code reviewen" | `everything-claude-code:python-review` | PEP 8, Type Hints, Pythonic Idioms Review |
| "Kotlin Code reviewen" | `everything-claude-code:kotlin-review` | Null Safety, Coroutines, Compose Review |
| "Build-Fehler Go" | `everything-claude-code:go-build` | Fixt Go Build/Vet Fehler mit minimalen Aenderungen |
| "Build-Fehler Kotlin/Gradle" | `everything-claude-code:kotlin-build` | Fixt Kotlin/Gradle Build-Fehler mit minimalen Aenderungen |
| "Build-Fehler TypeScript" | `everything-claude-code:build-error-resolver` Agent | Fixt Build/Type-Errors, fokussiert auf gruenes Build |

## 12. Skill-Erstellung & System-Erweiterung

| Deutsche Phrase | Skill | Was der Skill WIRKLICH tut |
|----------------|-------|---------------------------|
| "erstelle einen Skill", "neuer Skill" | `skill-creator:skill-creator` | Gefuehrte Skill-Erstellung mit Qualitaetspruefung (PFLICHT laut CLAUDE.md) |
| "Skill verbessern", "Skill fixen" | `skill-improver:skill-improver` | Iterative Fix-Review-Schleifen bis Qualitaet stimmt |
| "erstelle ein Plugin", "neues Plugin" | `plugin-dev:create-plugin` | Gefuehrte End-to-End Plugin-Erstellung |
| "erstelle einen Hook" | `hookify:hookify` | Erstellt Hooks aus Konversationsanalyse oder expliziten Regeln |
| "CLAUDE.md verbessern", "Regeln prüfen" | `claude-md-management:claude-md-improver` | Auditiert und verbessert CLAUDE.md-Dateien |

## 13. Qualitaet & Verifikation (automatische Trigger!)

| Situation (kein expliziter Befehl noetig) | Skill | Warum automatisch |
|------------------------------------------|-------|-------------------|
| Code geschrieben, Aufgabe scheint fertig | `superpowers:verification-before-completion` | PFLICHT: Kein "fertig" ohne frischen Beweis (Build, Tests, Output pruefen) |
| Feature komplett, alle Tests gruen | `superpowers:finishing-a-development-branch` | Bietet 4 Optionen: Merge, PR, Branch behalten, verwerfen |
| PR-Feedback/Review-Kommentare erhalten | `superpowers:receiving-code-review` | Erzwingt technische Pruefung statt blindes "guter Punkt!" |
| 2+ unabhaengige Aufgaben gleichzeitig | `superpowers:dispatching-parallel-agents` | Koordiniert parallele Agenten mit Datei-Ownership |
| Jedes Feature nach Abschluss | `quality-gate` Agent | Startet tester + code-reviewer + optimizer parallel |

---

## Whisper Speech-to-Text Korrekturen

| Whisper hoert | Gemeint ist |
|---------------|------------|
| "Cloud" | **Claude** (C-L-A-U-D-E) |
| "Rapper" | **Wrapper** |
| "Claw" | **NanoClaw** REPL |
| "Self improve" | `/self-improve` Command |
| "Tool check" | `/tool-check` Command |
| "Plug-in" | Plugin |
| "Brainstorm" | `superpowers:brainstorming` |
| "Reflektion" / "Reflect" | `claude-reflect:reflect` (Lernen) oder `reflexion:reflect` (Bewertung) — nachfragen! |
| "Code Rabbit" | `coderabbit:review` |

## 14. Nemo — Kostenloser Wissens-Worker (Nemotron 3 Super 120B)

**Nemo ist ein universeller Wissens-Agent der NVIDIA Nemotron 3 Super 120B kostenlos
ueber die NIM API ansteuert. Claude denkt vorher, Nemo fuehrt aus.**

| Deutsche Phrase | Agent/Tool | Was es WIRKLICH tut |
|----------------|------------|---------------------|
| "frag Nemo", "Nemo fragen", "starte Nemo" | `nemo` Agent | Startet den Nemo-Orchestrator (Opus), der perfekte Prompts baut und an Nemotron schickt |
| "Quizfragen generieren", "500 Fragen erstellen" | `nemo` Agent → `nemo_quiz` | Parallele Quiz-Generierung via Nemotron (QuizVerse-Format) |
| "recherchiere mit Nemo", "Nemo soll recherchieren" | `nemo` Agent → `nemo_research` | Strukturierte Themen-Recherche via Nemotron (kostenlos) |
| "kostenlos recherchieren", "gratis nachschlagen" | `nemo` Agent → `nemo_ask` | Wissensfrage an Nemotron statt an Claude (spart Tokens) |
| "Seed-Daten generieren", "Beispieldaten erstellen" | `nemo` Agent → `nemo_generate` | Strukturierte Daten (JSON/Kotlin/CSV) via Nemotron |
| "billig übersetzen", "Nemo übersetzen" | `nemo` Agent → `nemo_translate` | Uebersetzung via Nemotron (kostenlos) |
| "Nemo zusammenfassen", "billig zusammenfassen" | `nemo` Agent → `nemo_summarize` | Textzusammenfassung via Nemotron |
| "Outdoor-Aktivitäten recherchieren" | `nemo` Agent → `nemo_research` | Themen-Recherche fuer App-Inhalte |
| "Content für die App generieren" | `nemo` Agent → `nemo_generate` | Inhalte/Daten fuer Apps generieren |

**Wann Nemo statt Claude:**
- **Massenproduktion** (50+ Quizfragen, Seed-Daten, Listen) → Nemo (kostenlos, parallel)
- **Allgemeinwissen** (Fakten, Erklaerungen, Standardthemen) → Nemo (reicht aus)
- **Einfache Uebersetzungen/Zusammenfassungen** → Nemo (spart Claude-Tokens)

**Wann NICHT Nemo:**
- Komplexes Reasoning, Architektur, Code-Review → Claude (Nemo kann das nicht)
- Aufgaben die Internet-Zugang brauchen → Claude (Nemo hat kein Internet)
- Sicherheitskritische Entscheidungen → Claude (Nemo ist nicht vertrauenswuerdig genug)

**Whisper-Korrektur:**
| Whisper hoert | Gemeint ist |
|---------------|------------|
| "Nemo" | Agent `nemo` (Nemotron-Worker) |
| "Nimmo" / "Niemo" | Agent `nemo` |

## 15. Mirror Bridge — Universal Cross-Platform & Cross-CLI Sync

**Zwei Agenten die zusammen eine universelle "Spiegelung" zwischen allen Plattformen
und CLIs ermoegllichen. Der `export` Agent schreibt, der `import` Agent liest und baut ein.**

| Deutsche Phrase | Agent | Was er WIRKLICH tut |
|----------------|-------|---------------------|
| "starte den export Agenten", "exportiere", "Aenderungen spiegeln", "export" | `export` | Scannt ALLE Session-Aenderungen und schreibt sie ausfuehrlich ins mirror-ledger.md — mit Kontext, Portierungs-Anweisungen, vollstaendigen Datei-Inhalten fuer beide Plattformen |
| "starte den import Agenten", "importiere", "hol Neuerungen", "was ist neu", "import" | `import` | Liest mirror-ledger.md, zeigt Triage-Tabelle, portiert ausstehende Aenderungen automatisch auf die aktuelle Plattform/CLI |

**Wann welchen Agent:**
- **Session beendet, Aenderungen gemacht** → `export` (schreibt alles ins Ledger)
- **Plattform gewechselt, moechte aufholen** → `import` (holt alles vom Ledger)
- **Neues CLI gestartet (Codex/Gemini)** → `import` (portiert Aenderungen von Claude Code)

**Automatische Benachrichtigung:** Bei SessionStart zeigt `mirror-check` Hook an wenn Eintraege ausstehen.

## Proaktive Agents (kein Trigger noetig — laufen automatisch)

Diese laufen automatisch nach Code-Aenderungen und sollen NICHT manuell getriggert werden:

| Agent | Wann er laeuft | Was er tut |
|-------|---------------|-----------|
| `code-simplifier` | Nach Edit/Write Tool-Calls | Vereinfacht geaenderten Code automatisch — KEIN manueller Skill |
| `auto-verify-iterate` | Nach jeder Coding-Aufgabe | 5-Schritt-Verifikationsschleife (Pruefen→Verifizieren→Bewerten→Verbessern→Abschliessen) |
| `auto-format` | Nach Edit/Write (async) | Formatiert Dateien automatisch |
