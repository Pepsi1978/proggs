# Neues / Horizont-Scan — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Kategorie 12: Ganz neue Claude-Code-Fähigkeiten der letzten ~6 Monate.
> Quellen: Ausschließlich offizielle Anthropic-Dokumentation (code.claude.com/docs/en/whats-new)
> sofern nicht anders angegeben.

---

## Auto Mode (v2.1.83, März 2026)

- **Was:** Classifier-basiertes Permission-System als Mittelweg zwischen manuellem Review und `--dangerously-skip-permissions`. Sichere Aktionen laufen durch, riskante werden blockiert. Zwei Schichten: Server-seitiger Prompt-Injection-Probe (Input) + Transcript-Classifier mit Sonnet 4.6 (Output). Stage 1: schnelle Ja/Nein-Entscheidung. Stage 2: Chain-of-Thought nur für markierte Aktionen.
- **Best Practice:** Mit Default-Einstellungen beginnen (konservative Konfiguration). `autoMode.allow`, `soft_deny`, und `environment` anpassen. `"$defaults"` in die Listen aufnehmen, um eigene Regeln zusätzlich zu den eingebauten hinzuzufügen (nicht zu ersetzen). `autoMode.hardDeny` nutzen für absolut verbotene Aktionen unabhängig von Ausnahmen. Für Max-Abonnenten auf Opus 4.7 ist Auto Mode ohne `--enable-auto-mode`-Flag verfügbar. 0,4% False-Positive-Rate auf echtem Traffic; ~17% der unsicheren Aktionen werden nicht erkannt — kein Ersatz für eigene Urteilsfähigkeit.
- **Quelle:** [https://www.anthropic.com/engineering/claude-code-auto-mode](https://www.anthropic.com/engineering/claude-code-auto-mode) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w13](https://code.claude.com/docs/en/whats-new/2026-w13) (offiziell)
- **Stand:** März 2026 (Research Preview → ab April 2026 für Max ohne Flag)

---

## Computer Use im CLI (v2.1.86, März/April 2026)

- **Was:** Claude kann native Apps öffnen, durch UI klicken, eigene Änderungen testen und Fehler beheben — alles aus dem Terminal heraus. Zuvor nur im Desktop-App verfügbar. Ermöglicht Verifikationsschleifen für Apps ohne API (iOS-Simulator, native macOS-Apps, GUI-Tools). Research Preview, noch mit rauen Kanten.
- **Best Practice:** Über `/mcp` aktivieren → `computer-use` einschalten. Nur für GUI-only-Verifikation einsetzen, wo keine API existiert. Nicht für einfache Code-Tasks verwenden, die per Bash lösbar sind — Computer Use ist langsamer und teurer. Ideal für End-to-End-Tests nativer Apps nach Code-Änderungen. Beispiel: "Open the iOS simulator, tap through onboarding, and screenshot each step."
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w14](https://code.claude.com/docs/en/whats-new/2026-w14) (offiziell), [https://code.claude.com/docs/en/computer-use](https://code.claude.com/docs/en/computer-use) (offiziell)
- **Stand:** März/April 2026 (Research Preview)

---

## /ultrareview — Cloud-basierter Multi-Agent-Review (v2.1.114, April 2026)

- **Was:** Fleet von Bug-hunting-Agents in der Cloud analysiert den aktuellen Branch oder einen bestimmten PR. Befunde landen automatisch in CLI oder Desktop. Umfasst parallele Multi-Agent-Analyse und einen adversarial Critique-Pass. Kein lokales Rechnen nötig.
- **Best Practice:** Vor Merges kritischer Änderungen einsetzen (Auth, Datenmigration, Security-Code). Aktuellen Branch: `/ultrareview`. Bestimmten PR: `/ultrareview 1234`. Auch aus CI und Scripts via `claude ultrareview` (ohne Slash, ab v2.1.120) aufrufbar. Nicht für triviale Änderungen — der Ressourcenaufwand ist erheblich. Ergänzt, ersetzt nicht den lokalen Code-Reviewer-Agent.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w17](https://code.claude.com/docs/en/whats-new/2026-w17) (offiziell), [https://code.claude.com/docs/en/ultrareview](https://code.claude.com/docs/en/ultrareview) (offiziell)
- **Stand:** April 2026 (Research Preview → ab Woche 18 auch in CI)

---

## Routines — Geplante Cloud-Agents (v2.1.105–113, April 2026)

- **Was:** Templated Cloud-Agents die auf Zeitplan, GitHub-Event oder API-Call feuern. Definition über Claude Code Web: Prompt + erlaubte Repos + Connectors. Trigger: schedule, PR-opened, release-published, eigene Webhooks. Jede Routine bekommt einen tokenisierten `/fire`-Endpunkt für externe Systeme.
- **Best Practice:** Tägliche PR-Reviews automatisieren: `/schedule daily PR review at 9am`. Repetitive Aufgaben (Nightly-Tests, Auto-Triage von Issues) als Routine statt manueller Ausführung. Auf Claude Code Web erstellen oder via CLI scaffolden. Rechte der Routine auf die minimal nötigen Repos beschränken. GitHub-Event-Filter nutzen, um unnötige Auslösungen zu vermeiden.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell), [https://code.claude.com/docs/en/routines](https://code.claude.com/docs/en/routines) (offiziell)
- **Stand:** April 2026

---

## Native Binaries (v2.1.113, April 2026)

- **Was:** Das `claude`-CLI verwendet jetzt pro Plattform eine native Binary statt gebündeltem JavaScript. Node.js wird beim Aufruf nicht mehr involviert. Das npm-Paket lädt die passende Binary via optionale Abhängigkeit (z. B. `@anthropic-ai/claude-code-darwin-arm64`). Schnellerer Start, geringerer Overhead.
- **Best Practice:** `claude update` ausführen und mit `claude --version` verifizieren. Kein Änderungsbedarf am Install-Befehl. Auf Windows gilt: PowerShell ist nun der Shell-Fallback wenn Git Bash fehlt (ab v2.1.120 kein Git for Windows mehr erforderlich). MCP-Server-Configs müssen weiterhin absolute Pfade verwenden.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026 (v2.1.113)

---

## Ultraplan — Cloud-basierte Planungsphase (v2.1.92–v2.1.101, April 2026)

- **Was:** Planentwurf in der Cloud, Review und Kommentierung im Web-Editor, dann Ausführung remote oder lokal. Erster Run erstellt automatisch eine Cloud-Umgebung. Trennt Planungsphase von Ausführung.
- **Best Practice:** Für komplexe Architektur-Änderungen oder mehrstufige Migrationen einsetzen (nicht für einfache Tasks). Beispiel: `/ultraplan migrate the auth service from sessions to JWTs`. Plan im Browser überprüfen und kommentieren bevor Ausführung startet. Remote-Ausführung für lang laufende Migrationen wählen, um lokale Ressourcen zu schonen.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w15](https://code.claude.com/docs/en/whats-new/2026-w15) (offiziell)
- **Stand:** April 2026 (Early Preview)

---

## Agent View — Zentrales Sessions-Dashboard (v2.1.139, Mai 2026)

- **Was:** `claude agents` öffnet einen einzigen Bildschirm für alle laufenden Claude-Code-Sessions: was läuft, was wartet auf Input, was ist fertig. An jede Zeile anknüpfen für das vollständige Gespräch, `←` zurück zur Liste. Hintergrund-Sessions laufen ohne angehängtes Terminal weiter.
- **Best Practice:** Mehrere parallele Sessions starten (Bug-Fix, PR-Review, Test-Untersuchung) und über `claude agents` steuern. Dispatch-Flags mitgeben: `--add-dir`, `--settings`, `--model`, `--effort`, `--permission-mode`. `--cwd <path>` für projektbezogene Session-Ansicht nutzen. Nur bei Bedarf in eine Session einsteigen — kein ständiges Überwachen nötig. Gut kombinierbar mit `/goal` für autonome Background-Tasks.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell), [https://code.claude.com/docs/en/agent-view](https://code.claude.com/docs/en/agent-view) (offiziell)
- **Stand:** Mai 2026 (Research Preview, v2.1.139)

---

## /goal — Autonome Zielverfolgung (v2.1.139, Mai 2026)

- **Was:** Setzt eine Completion-Bedingung; Claude arbeitet über mehrere Turns selbstständig weiter bis sie erfüllt ist. Nach jedem Turn prüft ein schnelles Modell ob die Bedingung gilt. Funktioniert im interaktiven Modus, `-p` und Remote Control. Ziel wird nach Erfüllung automatisch gelöscht.
- **Best Practice:** Verifiable End States definieren (nicht vage Ziele): `/goal all tests in test/auth pass and the lint step is clean`. Für umfangreiche Arbeit mit klarer Fertigstellungsbedingung einsetzen (Modul-Migration, Refactoring bis alle Call-Sites kompilieren). Mit `claude agents` kombinieren: `/goal` in einer Background-Session starten, selbst weiterarbeiten. Zu breite Ziele vermeiden — Claude braucht eine prüfbare Bedingung.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell), [https://code.claude.com/docs/en/goal](https://code.claude.com/docs/en/goal) (offiziell)
- **Stand:** Mai 2026 (v2.1.139)

---

## Fast Mode auf Opus 4.7 (Research Preview, Mai 2026)

- **Was:** `/fast` läuft jetzt auf Opus 4.7 statt Opus 4.6. Gleiche Modellqualität bei ~2,5× Geschwindigkeit gegen höhere Token-Kosten. Preis unverändert ($30/$150 pro MTok wie Opus 4.6 Fast Mode). Für schnelle Iteration und Live-Debugging gedacht.
- **Best Practice:** `/fast` für Rapid-Iteration-Phasen und interaktives Debugging nutzen. Für finale, qualitätskritische Arbeit wieder auf Standard-Modus zurückschalten. Pin auf Opus 4.6: `CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE=1` setzen. Nicht als Dauereinstellung — Fast Mode kostet mehr pro Token.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell), [https://code.claude.com/docs/en/fast-mode](https://code.claude.com/docs/en/fast-mode) (offiziell)
- **Stand:** Mai 2026 (Research Preview)

---

## Checkpoints & /rewind (v2.0, Ende 2025 — erweitert Mai 2026)

- **Was:** Automatische Snapshots des Code-Zustands vor jeder Änderung. Drei Wiederherstellungs-Modi: Chat only (Chat zurückspulen, Code behalten), Code only (Dateiänderungen revertieren, Chat behalten), Both (vollständige Wiederherstellung). Checkpoints 30 Tage aufbewahrt. Rewind-Menü bietet seit Mai 2026 "Summarize up to here" für Kontext-Kompression älterer Turns. `/undo` ist Alias für `/rewind`.
- **Best Practice:** Vor riskanten Operationen (Refactoring, Migrations) auf Checkpoint verlassen. `Esc Esc` oder `/rewind` bei Fehlschlag. "Summarize up to here" nutzen wenn ältere Turns Platz im Kontextfenster verbrauchen aber nicht mehr relevant sind. "Code only"-Modus für Experimente: Code revertieren ohne Gesprächskontext zu verlieren.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell)
- **Stand:** Ende 2025 (v2.0), erweitert Mai 2026

---

## Monitor-Tool — Echtzeit-Event-Streaming (v2.1.98, April 2026)

- **Was:** Hintergrund-Watcher streamt Events als Transcript-Messages in die Konversation. Claude reagiert sofort auf jedes Event ohne Sleep-Loops. Für Log-Tailing, CI-Überwachung, Dev-Server-Crashes geeignet.
- **Best Practice:** Für Szenarien nutzen wo Claude auf externe Ereignisse reagieren soll: "Tail server.log in the background and tell me the moment a 5xx shows up." Plugins können Hintergrund-Watcher via `monitors`-Schlüssel im Manifest definieren (automatisch beim Session-Start aktiviert). Mit `/loop` kombinieren wenn periodische Checks nötig sind; Monitor bevorzugen wenn Push-Events verfügbar sind.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w15](https://code.claude.com/docs/en/whats-new/2026-w15) (offiziell)
- **Stand:** April 2026 (v2.1.98)

---

## /recap — Session-Zusammenfassung (April 2026)

- **Was:** Zeigt eine einzeilige Zusammenfassung dessen was passierte, während ein Terminal nicht fokussiert war. Nützlich beim Betreiben mehrerer paralleler Sessions. Automatisch beim Zurückkehren zur Session; manuell per `/recap`; in `/config` deaktivierbar.
- **Best Practice:** Nicht deaktivieren wenn man mit mehreren Sessions arbeitet — spart Zeit beim Wiedereinstieg. `/recap` manuell aufrufen nach Unterbrechungen. Bei sehr langen Sessions: `/recap` gibt Überblick vor dem Vertiefen in Details.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w17](https://code.claude.com/docs/en/whats-new/2026-w17) (offiziell)
- **Stand:** April 2026

---

## /usage — Verbrauchs-Analyse (v2.1.105–113, April 2026)

- **Was:** Zeigt was die Limits antreibt: parallele Sessions, Subagents, Cache-Misses, langer Kontext — jeweils mit Prozentzahl der letzten 24h und Optimierungstipp. `d`/`w` für Tag/Woche-Ansicht. Vereinigt die alten `/cost` und `/stats` Befehle (alte Namen noch als Shortcuts verfügbar).
- **Best Practice:** Regelmäßig prüfen wenn Limits spürbar werden. Cache-Miss-Prozentsatz beobachten — hohe Werte deuten auf suboptimalen Prompt-Aufbau hin. Parallelisierungs-Overhead gegen Zeitgewinn abwägen. Wochenansicht für Trend-Erkennung nutzen.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026

---

## Mobile Push Notifications (v2.1.105–113, April 2026)

- **Was:** Mit Remote Control verbunden sendet Claude Push-Benachrichtigungen ans Handy wenn ein langer Task endet oder eine Entscheidung benötigt wird. In `/config` aktivieren: "Push when Claude decides". Auch per Prompt-Anweisung: "notify me when the tests pass."
- **Best Practice:** Für lang laufende Agent-Runs aktivieren und vom Terminal weggehen — kein manuelles Polling nötig. Remote Control muss eingerichtet sein. Nicht bei kurzen Tasks aktivieren (unnötiger Lärm). Gut kombinierbar mit `claude agents` + `/goal` für vollständig autonome Workflows.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026

---

## Windows ohne Git Bash (v2.1.120, April/Mai 2026)

- **Was:** Git for Windows ist nicht mehr erforderlich. Claude Code nutzt PowerShell als Shell-Tool wenn Bash fehlt. Erleichtert Windows-Setup erheblich. Native PowerShell-Tool seit Woche 13 verfügbar.
- **Best Practice:** Bestehende Windows-Installationen können Git Bash behalten — kein Breaking Change. Für Neu-Installationen: kein Git for Windows mehr nötig. PowerShell-Hooks (`.ps1`) funktionieren nun als primäre Shells. MCP-Server-Configs weiterhin mit absoluten Pfaden konfigurieren.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w18](https://code.claude.com/docs/en/whats-new/2026-w18) (offiziell)
- **Stand:** April/Mai 2026 (v2.1.120)

---

## /powerup — Interaktive In-App-Lernlektionen (v2.1.90, April 2026)

- **Was:** Animierte Demos zu Claude-Code-Features direkt im Terminal. Verhindert das Verpassen von Features bei den häufigen Releases.
- **Best Practice:** Nach größeren Updates ausführen: `/powerup`. Besonders für neue Teammitglieder geeignet. Ergänzend zu `/team-onboarding` nutzen (erzeugt replaybare Onboarding-Guides aus eigenem Usage-Muster für das Team).
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w14](https://code.claude.com/docs/en/whats-new/2026-w14) (offiziell)
- **Stand:** April 2026 (v2.1.90)

---

## Conditional Hooks & Hook-Neuerungen (Woche 13–20, 2026)

- **Was:** `if`-Hooks feuern nur unter Bedingungen. `continueOnBlock` bei `PostToolUse`: Ablehnungsgrund wird Claude mitgeteilt und der Turn weitergeführt statt abzubrechen. `terminalSequence`-Feld in Hook-JSON für Desktop-Benachrichtigungen ohne Terminal. `args: string[]` exec-Form spawnt Befehle direkt ohne Shell (kein Quoting-Problem bei Pfad-Platzhaltern). `PreCompact`-Hooks können Kompaktierung via exit 2 oder `{"decision":"block"}` blockieren. Hooks können nun MCP-Tools direkt via `type: "mcp_tool"` aufrufen.
- **Best Practice:** `if`-Bedingungen nutzen um Hook-Overhead zu reduzieren (nur bei relevanten Tool-Calls feuern). `continueOnBlock` für Hooks die Claude auf Alternativen hinweisen sollen statt den Turn hart abzubrechen. `type: "mcp_tool"` für Hooks die auf bereits verbundene MCP-Server zugreifen müssen — effizienter als Prozess-Spawn. `PreCompact`-Block nur einsetzen wenn wichtige In-Progress-Daten gesichert werden müssen. `args: string[]`-Form bevorzugen bei Pfaden mit Leerzeichen.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w13](https://code.claude.com/docs/en/whats-new/2026-w13) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w17](https://code.claude.com/docs/en/whats-new/2026-w17) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell)
- **Stand:** März–Mai 2026

---

## Claude Opus 4.7 + xhigh Effort Level (v2.1.105, April 2026)

- **Was:** Stärkstes Coding-Modell (Default auf Max und Team Premium). Neuer `xhigh` Effort-Level zwischen `high` und `max` — empfohlen für die meisten Coding- und Agentic-Tasks. `/effort` öffnet interaktiven Slider ohne Argumente. Default-Effort für Pro/Max-Abonnenten jetzt `high` (war `medium`). Opus 4.7 nutzt nativ 1M-Kontextfenster (behebt fehlerhafte `/context`-Prozentzahlen früherer Versionen).
- **Best Practice:** `/model opus` dann `/effort xhigh` für anspruchsvolle Tasks. Interaktiven Slider mit `/effort` (ohne Argumente) nutzen. `max`-Level für Aufgaben sparen wo Qualität absolut kritisch ist. Fast Mode für Rapid-Iteration; Standard für Produktion. Hinweis: `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE`-Schwellwert wurde durch Opus 4.7's 1M-Fenster faktisch neu kalibriert.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026 (v2.1.105)

---

## Plugin-Neuerungen: ZIP-Archive, URLs, bin/-Verzeichnis (Woche 16–19, 2026)

- **Was:** `--plugin-dir` akzeptiert `.zip`-Dateien; `--plugin-url` lädt Plugin-Archive von einer URL für die aktuelle Session. Executables im `bin/`-Verzeichnis eines Plugins werden automatisch zum Bash-`PATH` der Session hinzugefügt. `claude plugin tag` erstellt Release-Tags mit Versions-Validierung. `claude plugin details <name>` zeigt Komponenten-Inventar und Token-Kosten-Schätzung. Plugins mit Root-Level `SKILL.md` ohne `skills/`-Unterverzeichnis werden als Skill erkannt.
- **Best Practice:** Plugins als ZIP-Archive verteilen für einfachere Installation ohne Repo-Zugriff. `bin/`-Verzeichnis nutzen um CLI-Helfer direkt neben Hooks/Commands zu paketieren — kein absoluter Pfad oder Wrapper-Script nötig. `claude plugin details <name>` vor Aktivierung prüfen um Token-Kosten zu kennen. Plugin-Releases mit `claude plugin tag` versionieren.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w19](https://code.claude.com/docs/en/whats-new/2026-w19) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w14](https://code.claude.com/docs/en/whats-new/2026-w14) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell)
- **Stand:** April/Mai 2026

---

## /autofix-pr, /team-onboarding, /loop (v2.1.98–101, April 2026)

- **Was:** `/autofix-pr` — leitet aus dem aktuellen Branch den offenen PR ab, aktiviert Auto-Fix auf Claude Code Web, überwacht CI und pusht Fixes bis grün. `/team-onboarding` — generiert einen Ramp-up-Guide aus eigenen Usage-Mustern, den Teammates replizieren können. `/loop` — selbstpacend wenn Intervall weggelassen; `/proactive` ist Alias.
- **Best Practice:** `/autofix-pr` nach `git push` ausführen und vom Terminal weggehen — Claude übernimmt CI-Reparaturen. `/team-onboarding` in gut bekannten Projekten ausführen für standardisierte Dokumentation. `/loop` ohne Intervall-Parameter für intelligente Auto-Pacing-Nutzung; Monitor-Tool bevorzugen wenn Push-Events verfügbar.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w15](https://code.claude.com/docs/en/whats-new/2026-w15) (offiziell)
- **Stand:** April 2026

---

*Recherchiert 2026-05-25 · 8 WebFetches aus code.claude.com/docs/en/whats-new (Wochen 13–20, 2026) + anthropic.com/engineering/claude-code-auto-mode · Alle Angaben offiziell bestätigt.*
