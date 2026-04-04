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

| "ACE starten", "Regeln verbessern", "Selbstverbesserung der Regeln" | `ace-curator` Agent | Analysiert Session-Daten (Scores, Bugs, Feedback) und schlaegt konkrete Regelverbesserungen vor. Basierend auf Stanford ACE-Paper. Schuetzt Begruessung und Direktiven ABSOLUT. |

**NICHT verwechseln**: `claude-reflect` = langfristig lernen (CLAUDE.md). `reflexion:reflect` = aktuelle Antwort bewerten. `ace-curator` = datenbasierte Regeloptimierung (Session-Scores + Bug-DB).

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
| "erstelle einen Hook", "neuer Hook", "Hook bauen" | `hook-forge` → dann `hookify:hookify` | **hook-forge ZUERST** (Template + Checkliste + exit-0-Pflicht), dann hookify fuer Konversationsanalyse. hook-forge stellt sicher dass der Hook resilient ist (3 Direktiven) |
| "Hook fixen", "Hook reparieren" (grundlegender Rewrite) | `hook-forge` | Nur bei grundlegendem Rewrite, nicht bei kleinen Edits |
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

## 14. Cross-Platform & Cross-CLI Sync (Universal Mirror Bridge)

**Zwei Agenten die zusammen eine universelle "Spiegelung" zwischen allen Plattformen
und CLIs ermoeglichen. Der `export` Agent schreibt, der `import` Agent liest und baut ein.**

| Deutsche Phrase | Agent | Was er WIRKLICH tut |
|----------------|-------|---------------------|
| "starte den export Agenten", "exportiere", "Aenderungen spiegeln", "export" | `export` | Scannt ALLE Session-Aenderungen und schreibt sie ausfuehrlich ins mirror-ledger.md — mit Kontext, Portierungs-Anweisungen, vollstaendigen Datei-Inhalten fuer beide Plattformen |
| "starte den import Agenten", "importiere", "hol Neuerungen", "was ist neu", "import" | `import` | Liest mirror-ledger.md, zeigt Triage-Tabelle, portiert ausstehende Aenderungen automatisch auf die aktuelle Plattform/CLI |

**NICHT verwechseln mit alten Agenten:** `mirror-export` und `mirror-import` sind die alten
Agenten (erste Version). Die neuen heissen `export` und `import` (kuerzere Namen, mehr Funktionen).

**Wann welchen Agent:**
- **Session beendet, Aenderungen gemacht** → `export` (schreibt alles ins Ledger)
- **Plattform gewechselt, moechte aufholen** → `import` (holt alles vom Ledger)
- **Neues CLI gestartet (Codex/Gemini)** → `import` (portiert Aenderungen von Claude Code)

**Automatische Benachrichtigung:** Bei SessionStart zeigt `mirror-check` Hook an wenn Eintraege ausstehen.

**Technischer Hinweis (Custom Agents starten):** Custom Agents aus `~/.claude/agents/` sind
KEINE registrierten `subagent_type`-Werte. Sie werden mit `subagent_type: "general-purpose"`
gestartet und der Agent-Prompt wird manuell im `prompt`-Parameter uebergeben. NIEMALS
`subagent_type: "export"` oder `subagent_type: "import"` verwenden — das schlaegt fehl.
<!-- Updated from MIRROR-2026-03-25-MAC-004 by import agent on 2026-03-26 -->

---

## 16. Browser-Automatisierung (browser-use CLI)

**KEIN MCP-Server — direkte CLI-Aufrufe per Bash (spart Token).**
Vollstaendige Regel: `~/.claude/rules/browser-use-cli.md`

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "oeffne im Browser", "geh auf die Seite", "oeffne die URL" | `browser-use --headed --profile open URL` | Oeffnet Chrome mit echtem Profil (alle Logins aktiv) |
| "browser use", "Browser Use", "mit dem Browser" | Browser-Use CLI aktivieren | Sichtbarer Chrome mit Profil |
| "pruefe das auf der Webseite", "ueberprüf das selber" | open + state + screenshot | Seite oeffnen, Zustand lesen, Screenshot |
| "trag das auf Firebase ein", "gib das dort ein" | open + navigate + input | Auf Seite navigieren und Wert eintragen |
| "klick da drauf", "klick auf den Link" | `browser-use click [INDEX]` | Element anklicken (Index aus state) |
| "was steht auf der Seite?", "schau dir die Seite an" | `browser-use --json state` | Seiteninhalt und klickbare Elemente auslesen |
| "mach einen Screenshot" | `browser-use screenshot` | Screenshot der aktuellen Seite |
| "schliess den Browser" | `browser-use close` | Browser sauber beenden |
| "oeffne meinen Chrome", "mein Chrome" | `browser-use --headed --profile open` | Chrome mit Profil oeffnen |

**PFLICHT**: Jeder Aufruf braucht `PYTHONIOENCODING=utf-8` Prefix (Windows-Encoding-Fix).
**PFLICHT**: `--headed` immer setzen (Sichtbarkeitsregel). `--profile` immer wenn Login noetig.
**ACHTUNG**: Chrome muss geschlossen sein fuer `--profile`, sonst Profil-Lock-Fehler.

## 17. NotebookLM CLI (Google NotebookLM Automatisierung)

**KEIN MCP-Server — direkte CLI-Aufrufe per Bash (spart Token).**
Vollstaendige Regel: `~/.claude/rules/notebooklm-cli.md`

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "NotebookLM", "Notebook LM", "neues Notebook" | NotebookLM CLI aktivieren | Pre-Flight-Check (Auth + Notebook), dann Befehl |
| "erstell ein NotebookLM zu [Thema]" | `create` + `use` + ggf. `source add-research` | Neues Notebook mit Quellen erstellen |
| "fueg das als Quelle hinzu", "Quelle hinzufuegen" | `source add [URL/Datei/YouTube]` | Quelle zum aktiven Notebook hinzufuegen |
| "recherchier das in NotebookLM" | `source add-research "Suchbegriff"` | Web-Recherche und automatisch als Quellen hinzufuegen |
| "mach einen Podcast", "Deep Dive", "generier Audio" | `generate audio` + `download audio` | NotebookLM-Podcast generieren und herunterladen |
| "mach ein Video daraus" | `generate video` + `download video` | Video generieren und herunterladen |
| "erstell ein Quiz", "Quiz machen" | `generate quiz` + `download quiz` | Quiz aus Notebook-Quellen erstellen |
| "Lernkarten", "Flashcards erstellen" | `generate flashcards` + `download flashcards` | Lernkarten generieren |
| "Mind-Map erstellen" | `generate mind-map` + `download mind-map` | Mind-Map aus Quellen generieren |
| "mach eine Praesentation", "Slides" | `generate slide-deck` + `download slide-deck` | Praesentation erstellen |
| "fass das Notebook zusammen" | `summary` | KI-Zusammenfassung aller Quellen |
| "frag das Notebook", "was sagen die Quellen" | `ask "Frage"` | Frage an NotebookLM stellen |
| "Infografik erstellen" | `generate infographic` + `download infographic` | Infografik generieren |
| "schreib einen Report", "Blog-Post" | `generate report` + `download report` | Report/Blog/Study-Guide erstellen |
| "YouTube-Video als Quelle" | `source add --type youtube URL` | YouTube-Transkript als Quelle |
| "PDF als Quelle" | `source add --type file pfad.pdf` | PDF-Datei als Quelle |

**PFLICHT**: Jeder Aufruf braucht `PYTHONIOENCODING=utf-8` Prefix (Windows-Encoding-Fix).
**PFLICHT**: Pre-Flight-Check (Auth + aktives Notebook) vor dem ersten Befehl jeder Session.
**ACHTUNG**: Login ist interaktiv — Benutzer muss `! notebooklm login` selbst ausfuehren.

## 18. CLI Dev-Tools (Tier 1+2+3 — installiert 2026-04-04)

> **13 CLI-Tools fuer Codebase-Analyse, Git-Workflow, Security und Benchmarking.**
> **Alle via `~/bin/` Symlinks im PATH. Kein Shell-Neustart noetig.**
> **Bei fehlendem Tool: `pwsh ~/.claude/hooks/path-verify.ps1 -Fix` repariert automatisch.**

### bat — `cat` mit Syntax-Highlighting (Rust)

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "zeig die Datei", "zeig mir den Code" | `bat [DATEI]` | Datei mit Syntax-Highlighting und Zeilennummern anzeigen |
| "zeig die Datei mit Farben", "Syntax-Highlighting" | `bat [DATEI]` | Identisch — bat hat immer Highlighting aktiv |
| "zeig nur Zeile 10 bis 20" | `bat -r 10:20 [DATEI]` | Nur den angegebenen Zeilenbereich anzeigen |
| "welche Sprache erkennt bat?" | `bat --list-languages` | Alle unterstuetzten Sprachen auflisten |

### fd — schnelles `find` (Rust)

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "finde alle Kotlin-Dateien", "such nach .kt Dateien" | `fd -e kt` | Alle Dateien mit Endung `.kt` finden (rekursiv, schnell) |
| "finde Dateien die X heissen", "wo ist die Datei X?" | `fd "PATTERN"` | Dateien deren Name das Pattern enthaelt |
| "finde grosse Dateien", "Dateien ueber 10MB" | `fd --size +10m` | Dateien groesser als 10MB finden |
| "finde Dateien die heute geaendert wurden" | `fd --changed-within 1d` | Kuerzlich geaenderte Dateien |
| "finde und loesche alle .tmp Dateien" | `fd -e tmp -x rm {}` | Dateien finden und Befehl darauf ausfuehren |

### fzf — Fuzzy-Finder

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "fuzzy suche", "interaktiv suchen" | `fzf` | Interaktiver Fuzzy-Finder ueber stdin |
| "Datei interaktiv waehlen" | `fd \| fzf` | Datei aus dem Projekt interaktiv auswaehlen |
| "Git Branch waehlen" | `git branch \| fzf` | Branch interaktiv auswaehlen |

### delta — Git-Diff-Viewer

**Delta ist als Git-Pager konfiguriert und laeuft automatisch bei jedem `git diff` und `git log -p`.**
**Kein manueller Trigger noetig — delta ist immer aktiv.**

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "zeig den Diff", "was hat sich geaendert?" | `git diff` | Delta zeigt side-by-side Diff mit Syntax-Highlighting |
| "zeig den letzten Commit" | `git log -p -1` | Letzter Commit mit delta-formatiertem Diff |
| "Diff ohne Farben", "roher Diff" | `git --no-pager diff` | Delta umgehen, rohen Diff anzeigen |

### tokei — Code-Statistiken

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "wie viele Zeilen Code?", "Code-Statistik" | `tokei` | Zeilen, Dateien, Sprachen im aktuellen Projekt zaehlen |
| "Projekt-Ueberblick", "wie gross ist das Projekt?" | `tokei --sort lines` | Statistik sortiert nach Zeilenanzahl |
| "wie viel Kotlin?", "Sprachen-Verteilung" | `tokei` | Aufschluesselung nach Sprachen mit Code/Kommentar/Leer-Zeilen |
| "Statistik fuer einen Ordner" | `tokei [PFAD]` | Statistik fuer ein bestimmtes Unterverzeichnis |

### shellcheck — Shell-Script-Linter

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "pruefe das Shell-Script", "lint das .sh Script" | `shellcheck [DATEI.sh]` | Shell-Script auf Fehler, Warnungen und Best Practices pruefen |
| "alle Shell-Scripts pruefen" | `fd -e sh -x shellcheck {}` | ALLE .sh-Dateien im Projekt pruefen |
| "ist der Hook korrekt?", "Hook pruefen" | `shellcheck [HOOK.sh]` | Hook-Script auf Shell-Fehler pruefen (Poka-Yoke Stufe 1) |

**WICHTIG fuer Direktive #3**: shellcheck MUSS vor jedem Commit von `.sh`-Dateien laufen.
Es faengt Fehler ab die sonst erst beim Hook-Ausfuehren crashen wuerden.

### hyperfine — CLI-Benchmarking

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "wie schnell ist das?", "Benchmark", "Performance messen" | `hyperfine "BEFEHL"` | Befehl mehrfach ausfuehren und Durchschnittszeit messen |
| "vergleiche zwei Befehle", "was ist schneller?" | `hyperfine "BEFEHL_A" "BEFEHL_B"` | Zwei Befehle gegeneinander benchmarken |
| "Build-Zeit messen" | `hyperfine "./gradlew assembleDebug"` | Build-Dauer praezsie messen (mit Warmup) |

### dust — Disk-Usage-Analyse (Rust)

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "was braucht Speicherplatz?", "Speicherverbrauch" | `dust` | Verzeichnisbaum mit Groessen anzeigen (top-down) |
| "groesste Ordner finden" | `dust -d 2` | Nur 2 Ebenen tief, groesste Ordner zuerst |
| "wie gross ist das Projekt?", "Projektgroesse" | `dust [PFAD]` | Speicherverbrauch eines bestimmten Ordners |

### duf — Disk-Free-Ueberblick

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "wie viel Platz habe ich?", "Festplatte voll?" | `duf` | Alle Laufwerke mit freiem/belegtem Speicher anzeigen |
| "Speicherplatz pruefen" | `duf` | Uebersichtliche Tabelle aller Mountpoints |

### lazygit — Git-TUI

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "lazygit", "Git-UI", "Git visuell" | `lazygit` | Interaktives Git-Terminal-UI oeffnen |
| "Git-Aenderungen visuell sehen" | `lazygit` | Staging, Commits, Branches, Stash visuell verwalten |

**HINWEIS**: lazygit ist INTERAKTIV — der Benutzer muss `! lazygit` im Prompt eingeben
damit es in seiner Shell laeuft. Claude kann es nicht direkt ausfuehren.

### shfmt — Shell-Script-Formatter

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "formatiere das Shell-Script" | `shfmt -w [DATEI.sh]` | Shell-Script automatisch formatieren (in-place) |
| "alle Shell-Scripts formatieren" | `shfmt -w -l .` | Alle .sh-Dateien im Projekt formatieren |
| "Shell-Stil pruefen (ohne aendern)" | `shfmt -d [DATEI.sh]` | Diff anzeigen was geaendert wuerde (dry-run) |

**Best Practice**: `shfmt` + `shellcheck` zusammen fuer saubere Shell-Scripts:
Erst `shfmt -w` (formatieren), dann `shellcheck` (pruefen).

### trivy — Security-Scanner

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "scanne auf Sicherheitsluecken", "Vulnerability-Scan" | `trivy fs .` | Aktuelles Verzeichnis auf bekannte CVEs in Dependencies scannen |
| "sind meine Dependencies sicher?" | `trivy fs .` | Prueft package.json, Cargo.toml, go.mod, build.gradle etc. |
| "Docker-Image scannen" | `trivy image [IMAGE]` | Container-Image auf Vulnerabilities scannen |
| "geheime Schluessel suchen", "Secrets scannen" | `trivy fs --scanners secret .` | Dateien nach hartkodierten Secrets/API-Keys durchsuchen |

**Entscheidungshilfe Trivy vs Gitleaks**:
- **trivy**: Scannt Dependencies (CVEs) UND Secrets UND Fehlkonfigurationen — breiter Scope
- **gitleaks**: Scannt NUR Secrets, aber tiefer (auch Git-History) — spezialisiert

### glow — Markdown-Renderer

| Deutsche Phrase | Aktion | Was passiert |
|----------------|--------|-------------|
| "zeig die README", "README huebsch anzeigen" | `glow README.md` | Markdown-Datei gerendert im Terminal anzeigen |
| "zeig das Whiteboard", "MEMORY huebsch" | `glow .claude/agent-memory/shared/MEMORY.md` | Whiteboard gerendert anzeigen |
| "Markdown im Terminal anzeigen" | `glow [DATEI.md]` | Beliebige .md-Datei gerendert anzeigen |

---

### Zusammenspiel der CLI-Tools (Kombinations-Trigger)

| Deutsche Phrase | Kombination | Was passiert |
|----------------|------------|-------------|
| "Projekt-Ueberblick", "zeig mir alles" | `tokei` + `duf` + `dust -d 2` | Code-Statistik + Speicher + Verzeichnisgroessen |
| "ist das Projekt sauber?", "Qualitaetscheck" | `shellcheck` + `trivy fs .` + `gitleaks detect` | Shell-Lint + Security-Scan + Secret-Scan |
| "Shell-Scripts aufraemen" | `shfmt -w` + `shellcheck` | Erst formatieren, dann auf Fehler pruefen |
| "wie schnell baut das Projekt?" | `hyperfine "./gradlew assembleDebug"` | Build-Zeit praezsie messen |
| "finde alle grossen Dateien" | `fd --size +10m` oder `dust` | Grosse Dateien per Name oder Verzeichnisbaum |

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
| "Browser Use" / "Browser Juice" / "Browser News" | **browser-use** CLI |
| "brause use" / "Brause Juse" | **browser-use** CLI |
| "Notebook LM" / "Notebook Ellem" / "Notebook Ellen" | **notebooklm** CLI |
| "Not Book LM" / "Notbuch LM" | **notebooklm** CLI |
| "Deep Dive" / "Podcast" (im Kontext von NotebookLM) | `notebooklm generate audio` |
| "Lernkarten" / "Flashkards" | `notebooklm generate flashcards` |
| "Mindmap" / "Mind Map" (im Kontext von NotebookLM) | `notebooklm generate mind-map` |
| "Bett" / "Bat" / "Bätt" | **bat** CLI (cat mit Highlighting) |
| "FD" / "Eff-Dee" / "Äff Dee" | **fd** CLI (schnelles find) |
| "Fuzzy" / "Fuzzy Finder" / "Fuzzy Find" | **fzf** CLI |
| "Delta" / "Delle Ta" | **delta** CLI (Git-Diff-Viewer) |
| "Tokai" / "Tokäi" / "Token" (im CLI-Kontext) | **tokei** CLI (Code-Statistiken) |
| "Shell Check" / "Schell Check" / "Shell Tschek" | **shellcheck** CLI (Shell-Linter) |
| "Hyperfein" / "Hyper Fine" / "Hyperfine" | **hyperfine** CLI (Benchmarking) |
| "Dust" / "Dast" | **dust** CLI (Disk-Usage) |
| "Duff" / "Doof" / "DUF" | **duf** CLI (Disk-Free) |
| "Lazy Git" / "Leisy Git" / "Läsi Git" | **lazygit** CLI (Git-TUI) |
| "Shell Format" / "Shell FMT" / "SHFMT" | **shfmt** CLI (Shell-Formatter) |
| "Trivy" / "Triffy" / "Trivi" | **trivy** CLI (Security-Scanner) |
| "Glow" / "Gloh" | **glow** CLI (Markdown-Renderer) |

## 15. Metacognitive Analyse & Hyperagent

| Deutsche Phrase | Agent/Hook | Was er WIRKLICH tut |
|----------------|-----------|---------------------|
| "analysiere die Session", "wie war die Session?", "Metacognition" | `hyperagent` Agent (manuell spawnen) | Tiefe 5-Stufen-Analyse: Intent-Drift, Effizienz, Memory-Aktualitaet, Verbesserungsvorschlaege, Session-Score |
| "wie habe ich mich verbessert?", "Session-Trend", "Fortschritt" | Session-Scores lesen (`~/.claude/session-scores.jsonl`) | Quantitative Trend-Analyse ueber die letzten N Sessions |
| "Superintelligenz recherchieren", "Verbesserungen finden" | `superintelligenz` Agent | Iterativer Internet-Forscher der in 3 Wellen nach System-Verbesserungen sucht (alle 3 Direktiven) |

**Automatisch (kein Trigger noetig):**
- **hyperagent-stop** Hook (Stop-Event): Injiziert metacognitive Analyse-Erinnerung bei jeder Antwort (>5 Turns)
- **session-scorer** Hook (SessionEnd): Schreibt quantitative Session-Metriken in JSONL

## Proaktive Agents (kein Trigger noetig — laufen automatisch)

Diese laufen automatisch nach Code-Aenderungen und sollen NICHT manuell getriggert werden:

| Agent | Wann er laeuft | Was er tut |
|-------|---------------|-----------|
| `code-simplifier` | Nach Edit/Write Tool-Calls | Vereinfacht geaenderten Code automatisch — KEIN manueller Skill |
| `auto-verify-iterate` | Nach jeder Coding-Aufgabe | 5-Schritt-Verifikationsschleife (Pruefen→Verifizieren→Bewerten→Verbessern→Abschliessen) |
| `auto-format` | Nach Edit/Write (async) | Formatiert Dateien automatisch |
| `hyperagent-stop` | Bei jedem Stop-Event (>5 Turns) | Prompt-Injection fuer metacognitive Selbstanalyse |
| `session-scorer` | Bei SessionEnd | Schreibt Turns, Fehler, Commits, Dauer in session-scores.jsonl |
