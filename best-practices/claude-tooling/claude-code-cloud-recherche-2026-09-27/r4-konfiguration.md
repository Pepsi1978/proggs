# Researcher 4: Welche Konfiguration lädt eine Cloud-Sitzung (Claude Code on the web)

Stand der Recherche: 27.09.2026, lokale CLI-Version 2.1.283. Primärquellen: code.claude.com/docs (offiziell), CHANGELOG.md im Repo anthropics/claude-code (offiziell), GitHub Issues in anthropics/claude-code (extern, per `gh issue view` gegen die Live-API verifiziert, keine Web-Halluzinationen).

## Befunde

### 1. Grundprinzip: Cloud-Sitzung = frischer Git-Klon, kein Zugriff auf die lokale Maschine

> "Cloud sessions start from a fresh clone of your repository. Anything you commit to the repo is available. Anything you've installed or configured only on your own machine isn't available in the session."
[offiziell] https://code.claude.com/docs/en/cloud-environments (Abschnitt "What carries over from your setup")

### 2. CLAUDE.md / AGENTS.md

- Repo-`CLAUDE.md`: **Ja**, Teil des Klons, wird geladen wie lokal. [offiziell] https://code.claude.com/docs/en/cloud-environments
- `~/.claude/CLAUDE.md` (Benutzer): **Nein**, liegt nur auf der eigenen Maschine, nicht im Repo. [offiziell] https://code.claude.com/docs/en/cloud-environments
- **AGENTS.md wird gelesen**: Seit Claude Code v2.1.277 liest Claude Code `AGENTS.md` nativ, wenn kein `CLAUDE.md`/`.claude/CLAUDE.md`/`CLAUDE.local.md` im Arbeitsverzeichnis oder darüber liegt (Default-Modus `claude-md-or-agents-md`). Es gibt auch `claude-md-and-agents-md` (beide werden geladen, CLAUDE.md zuerst) und `managed-only`. Konfigurierbar über `pluginConfigs` des eingebauten Plugins `agents-md@builtin`, aber **nicht** in Projekt-/Local-Settings ("Claude Code ignoriert es in project and local settings files"). [offiziell] https://code.claude.com/docs/en/memory (Abschnitt "AGENTS.md")
  - Wichtige Einschränkung: **Vor v2.1.281** lasen manche Sitzungen (u. a. Amazon Bedrock oder mit deaktivierter Telemetrie) `AGENTS.md` **nicht**. Da lokale CLI-Version 2.1.283 und Cloud-Sitzungen i. d. R. aktuelle Server-Versionen nutzen, sollte das heute kein Problem mehr sein — aber es zeigt, dass AGENTS.md-Unterstützung eine relativ junge, versionsabhängige Funktion ist. [offiziell] ebd. (Abschnitt "When AGENTS.md support is unavailable")
  - Gegendarstellung eines externen Blogs/Gist ("Claude Code reads AGENTS.md as a fallback' claim is wrong") ist laut den offiziellen Docs **veraltet/falsch** — die Fallback-Logik ist offiziell dokumentiert und aktiv. [extern, widerlegt] https://gist.github.com/yurukusa/d36197848911f025add142abefcde685 vs. [offiziell] https://code.claude.com/docs/en/memory
  - Für Cloud-Sitzungen selbst ist kein Sonderverhalten dokumentiert; da AGENTS.md wie CLAUDE.md "Teil des Klons" ist, ist naheliegend, dass es cloud-seitig identisch funktioniert — das ist aber **nicht explizit** in der Cloud-Tabelle bestätigt (nur CLAUDE.md wird dort namentlich aufgeführt). Als abgeleitet, nicht wörtlich belegt markieren.

### 3. @-Imports und verschachtelte CLAUDE.md

- Imports über `@pfad/datei` werden beim Start expandiert, max. **4 Hops** Rekursionstiefe, relative Pfade lösen relativ zur importierenden Datei auf (nicht zum Arbeitsverzeichnis). Imports in Codeblöcken/Backticks werden übersprungen. [offiziell] https://code.claude.com/docs/en/memory (Abschnitt "Import additional files")
- Import einer Datei **außerhalb** des Arbeitsverzeichnisses gilt als "external import" und löst beim ersten Mal einen Bestätigungsdialog aus — außer bei User-Scope-Dateien (`~/.claude/CLAUDE.md`, `~/.claude/rules/`), die als vertrauenswürdig gelten. **Ausnahme: In Cowork-Sitzungen** werden externe Imports aus User-Scope-Dateien übersprungen, wenn sie außerhalb des Sitzungsordners liegen. [offiziell] ebd.

### 4. Rules-Dateien (`.claude/rules/`) — Symlinks explizit dokumentiert und unterstützt

> "The `.claude/rules/` directory supports symlinks, so you can maintain a shared set of rules and link them into multiple projects. Circular symlinks are detected and handled gracefully."
[offiziell] https://code.claude.com/docs/en/memory (Abschnitt "Share rules across projects with symlinks")
- Ein Symlink, dessen Ziel außerhalb des Arbeitsverzeichnisses liegt, wird wie ein External Import behandelt (Bestätigungsdialog, danach laden nur Rules ohne `paths`-Feld).
- Seit **v2.1.198** funktioniert Pfad-Matching für `paths`-gescopte Rules auch, wenn die Datei über einen symlinkten Checkout-Pfad erreicht wird. [offiziell] CHANGELOG.md Zeile 2987 ("Fixed `.claude/rules/` conditional rules not loading when the target file is reached via a symlinked path")
- Seit **v2.1.239**: `claudeMdExcludes`-Pattern matchen jetzt sowohl den Link-Pfad als auch das Link-Ziel (vorher nur das Ziel). [offiziell] CHANGELOG.md Zeile 1927 + memory-Doku Zeile 331
- Symlink auf einen Netzwerkpfad (UNC `\\server\share`, `/net`, `/Network`) wird **nicht** aufgelöst (Sicherheitsmaßnahme). [offiziell] https://code.claude.com/docs/en/memory
- Fix in **v2.1.282**: CLAUDE.md/Rules-Lesen beim Start über einen Repo-Symlink, der via `..` oder `/.vol`-Pfad auf macOS `/Network` zeigt, konnte fälschlich gelistet werden — jetzt gefixt. [offiziell] CHANGELOG.md Zeile 118

### 5. Skills — BESONDERS WICHTIG: Symlink-Frage

**Offizielle Dokumentationslage (aktuell, Stand heute):**

> "Symlinked folders: a `<skill-name>` entry in the enterprise, personal, or project location can be a symlink to a directory elsewhere on disk. Claude Code reads `SKILL.md` from the target and loads the skill once even if several locations point at the same target. Plugin skills handle symlinks differently."
[offiziell] https://code.claude.com/docs/en/skills (Abschnitt "Skill folders also follow these rules")

Das heißt: **Laut Dokumentation ist ein `<skill-name>`-Ordner in `.claude/skills/<name>` (Projekt-Ebene), der ein Symlink auf ein anderes Verzeichnis ist, offiziell unterstützt** — inklusive eines Git-getrackten Symlinks (Mode 120000), der beim Checkout in der Cloud-VM (Linux) als echter Filesystem-Symlink erscheint. Es wird **nicht** zwischen "OS-Symlink lokal erzeugt" und "Git-Symlink aus dem Repo ausgecheckt" unterschieden — beide sind für Claude Code technisch identisch (ein Verzeichniseintrag vom Typ Symlink).

**ABER: Es gibt einen aktuell offenen, technisch klar diagnostizierten Bug**, der dieser Dokumentation widerspricht — siehe BUG-KANDIDATEN unten (Issue #14836). Die Doku beschreibt den Sollzustand; die Praxis zeigt seit Monaten (Issue offen seit 20.12.2025, letzter Kommentar mit Code-Analyse am 15.09.2026) eine Regression bei der **Skill-Discovery** (nicht beim eigentlichen Ausführen): Skills, die als Symlink vorliegen, werden vom `readdirSync`-basierten Scan nicht als Verzeichnis erkannt (`Dirent.isDirectory()` liefert für einen Symlink-Eintrag `false`), und tauchen daher weder in `/skills` noch im `/`-Autocomplete auf — der Skill-Inhalt wird aber laut mehreren Meldungen dennoch in den Modell-Kontext geladen und funktioniert bei explizitem Aufruf.

**Sandbox-Startproblem (separat, aber verwandt, GEFIXT):**
> "Fixed Linux sandbox failing to start when `.claude/skills` or `.claude/hooks` is a symlink"
[offiziell] CHANGELOG.md, Version **2.1.178**. Vor dieser Version konnte ein symlinkter `.claude/skills`- oder `.claude/hooks`-Ordner dazu führen, dass die **Linux-Sandbox gar nicht erst startete** — das ist besonders für Cloud-Sitzungen relevant, da diese in Linux-VMs laufen. Seit 2.1.178 (deutlich vor der aktuellen 2.1.283) ist das behoben, d. h. der Sandbox-Start selbst ist heute kein Blocker mehr.

**Zusätzlicher, nicht-symlink-bezogener Cloud-Bug bei Skills:**
Issue #95105 (offen, 17.09.2026): In Remote/Cloud-Sitzungen fehlen Projekt-Skills aus `.claude/skills/` im `/`-Slash-Menü, obwohl die Skills als **normale committete Verzeichnisse (explizit keine Symlinks)** vorliegen und funktionieren, wenn man sie per vollem Namen aufruft. Das zeigt: Die Skill-**Discovery/Autocomplete**-Schicht in Cloud-Sitzungen hat generell Lücken, unabhängig von Symlinks.

**Skill-Ladeverhalten in Cloud-/Cowork-Sitzungen allgemein:**

> "Cowork sessions and cloud sessions, including routines, don't read `~/.claude/skills/` on your machine. Both interactive and scheduled Cowork sessions load the skills enabled for your claude.ai account, synced at session start [...]. Cloud sessions additionally load project skills committed to the cloned repository's `.claude/skills/`."
[offiziell] https://code.claude.com/docs/en/skills (Abschnitt "Skills in Cowork and cloud sessions", sinngemäß über WebFetch extrahiert, Kernsatz stimmt mit der Tabelle in cloud-environments überein)

Tabelle (offiziell, aus /docs/en/skills):
| Ort | Pfad | Lädt in |
|---|---|---|
| Enterprise | `.claude/skills/<name>/SKILL.md` in Managed-Settings-Verzeichnis | Alle Maschinen der Organisation |
| Personal | `~/.claude/skills/<name>/SKILL.md` | Alle lokalen Projekte, **nicht** Cowork/Cloud |
| Project | `.claude/skills/<name>/SKILL.md` | Sitzungen in diesem Repo, wenn committed |
| claude.ai account | Skills, die im claude.ai-Konto aktiviert sind | Cowork-, Cloud- und Terminal-Sitzungen mit diesem Konto |

Reservierter Name: `synced` darf **nicht** als Skill-Ordnername verwendet werden — Claude Code nutzt `~/.claude/skills/synced/` intern für vom claude.ai-Konto synchronisierte Skills. [offiziell] https://code.claude.com/docs/en/skills
(Hinweis am Rande, nicht Teil der Aufgabe: Der Repo-Status des Nutzers zeigt einen untrackten Ordner `OpenLauncher/Profiles/ClaudeCode/standard/skills/synced/` — das ist vermutlich genau dieser interne Sync-Ordner, kein manuell angelegter Skill.)

**Benutzer-Skills in Routinen:**
> "If a skill exists only in `~/.claude/skills/` on your machine, Claude Code reports that the skill was not found when a routine invokes it [...]. To make a personal skill available in these sessions: enable the skill for your claude.ai account, or commit the skill to the repository's `.claude/skills/`."
[offiziell] https://code.claude.com/docs/en/skills

### 6. `.claude/settings.json` und `.claude/settings.local.json`

Kernaussage (wörtlich):

> "Shared project settings (`.claude/settings.json`): read in a session with one repository, because the file is part of the clone and the session starts inside it. Commit a setting there to apply it in those sessions. A session with several repositories starts above the clones and reads only the `enabledPlugins` and `extraKnownMarketplaces` keys from each repository's `.claude/settings.json`, not permission rules, hooks, `env`, or other keys. The marketplaces and plugins those two keys declare still don't load in a cloud session."
[offiziell] https://code.claude.com/docs/en/settings (Abschnitt "Settings in cloud sessions")

Weitere Punkte derselben Quelle:
- **Managed Settings**: Nur **server-managed settings** (aus der claude.ai-Konsole) erreichen eine Cloud-Sitzung; eine lokale `managed-settings.json`-Datei oder MDM-Profil auf dem eigenen Gerät **nicht**.
- **`/config`** öffnet im Browser (claude.ai/code) nur die claude.ai-Settings-Seite, ändert aber keinen Wert direkt. Um eine Einstellung für eine Cloud-Sitzung zu ändern: entweder Umgebungsvariable im Cloud-Environment setzen, oder (bei Single-Repo-Sitzungen) den Schlüssel in `.claude/settings.json` committen.
- **`.claude/settings.local.json`**: wird in der offiziellen Cloud-Tabelle **nicht** als übertragen aufgeführt; da diese Datei üblicherweise gitignored ist und nicht Teil des Klons wird, wird sie **nicht** gelesen. Das ist eine logische Ableitung aus "Anything you commit to the repo is available" — nicht wörtlich als eigener Punkt in der Tabelle bestätigt, aber konsistent mit allen anderen Aussagen. [abgeleitet, nicht wörtlich belegt]
- **Transport-Variablen** im `env`-Block von `.claude/settings.json` (z. B. `NODE_EXTRA_CA_CERTS`, mTLS-Client-Zertifikatsvariablen) werden in Cloud-Sitzungen **ignoriert**, da das Hosting-Environment die API-Verbindung selbst verwaltet; jeder ignorierte Key wird im Debug-Log der Sitzung vermerkt. [offiziell] https://code.claude.com/docs/en/cloud-environments

### 7. Hooks

- Hooks aus `.claude/settings.json` (Single-Repo-Sitzung): **Ja**, laufen. Bei Multi-Repo-Sitzungen: **Nein** (siehe Punkt 6). [offiziell] https://code.claude.com/docs/en/cloud-environments
- `~/.claude/settings.json`-Hooks (Benutzerebene): **werden nicht gelesen** in Cloud-Sitzungen. [offiziell] https://code.claude.com/docs/en/hooks / cloud-environments
- Die Umgebungsvariable **`CLAUDE_CODE_REMOTE`** ist in Remote-/Web-Umgebungen auf `"true"` gesetzt und in der lokalen CLI nicht gesetzt — offiziell dokumentierter Mechanismus, um Hooks (z. B. SessionStart) umgebungsabhängig zu verzweigen. [offiziell] https://code.claude.com/docs/en/env-vars (referenziert über hooks-Doku)
- Zeitlimits: Von Claude ausgeführte Kommandos und **SessionStart-Hooks** haben Standard-Timeouts (konfigurierbar); ein Setup-Skript wird nur gecacht, wenn es innerhalb von ca. 5 Minuten fertig ist. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web (Abschnitt "Limitations" → "Time limits")
- Selbst-gehostete Umgebungen lesen zusätzlich Hooks aus der `~/.claude/`-Seed-Konfiguration des Runner-Hosts und ggf. aus der Managed-Settings-Datei im Runner-Image. [offiziell] https://code.claude.com/docs/en/hooks

### 8. MCP-Server (`.mcp.json`)

> "Your repo's `.mcp.json` MCP servers | Yes, in a session with one repository | Part of the clone, found from the session's working directory"
[offiziell] https://code.claude.com/docs/en/cloud-environments
- MCP-Server, die per `claude mcp add` im **lokalen** oder **User**-Scope hinzugefügt wurden, landen in `~/.claude.json` auf der eigenen Maschine und erreichen die Cloud-Sitzung **nicht**. Für Cloud-Übertragung: `claude mcp add --scope project`, was in `.mcp.json` im Repo schreibt — das muss committed werden. [offiziell] ebd.

### 9. Plugins, `enabledPlugins`, Marketplaces

> "Plugins and marketplaces declared in your repo's `.claude/settings.json` | No | A cloud session doesn't install the plugins a repository turns on under `enabledPlugins`, including ones from the marketplaces it lists under `extraKnownMarketplaces`"
[offiziell] https://code.claude.com/docs/en/cloud-environments
- User-Scope `enabledPlugins` (aus `~/.claude/settings.json`) wird ebenfalls **nicht** übertragen.
- Bezüglich Skill-spezifischer Plugin-Symlinks: Plugin-Skills behandeln Symlinks anders als normale Skill-Ordner — verwiesen wird auf `/docs/en/plugins/host-marketplace#share-files-within-a-marketplace-with-symlinks` [offiziell, nicht im Detail abgerufen] https://code.claude.com/docs/en/skills

### 10. `.claude/agents/`, `.claude/commands/`

> "Your repo's `.claude/skills/`, `.claude/agents/`, `.claude/commands/` | Yes | Part of the clone"
[offiziell] https://code.claude.com/docs/en/cloud-environments
- `~/.claude/agents/`, `~/.claude/commands/` (Benutzerebene): **Nein**, nur auf der eigenen Maschine.
- Subagenten funktionieren in Cloud-Sitzungen wie lokal; Subagenten aus `.claude/agents/` im Repo werden automatisch erkannt. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web

### 11. Output-Styles

Kein direkter Treffer in den beiden zentralen Cloud-Dokumenten (cloud-environments, claude-code-on-the-web) zu Output-Styles explizit als eigener Tabelleneintrag. Da Output-Styles ebenfalls über Settings-Dateien konfiguriert werden, ist anzunehmen, dass sie derselben Logik wie andere `.claude/settings.json`-Werte folgen (Single-Repo-Sitzung: ja, wenn committed; Multi-Repo: nein) — **nicht explizit dokumentiert gefunden**, daher als offen markiert.

### 12. Allgemeine Cloud-Session-Architektur (Kontext)

- Jede Cloud-Sitzung läuft in einer isolierten, Anthropic-verwalteten VM (oder einer Self-Hosted-Umgebung der Organisation). [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Git-Zugangsdaten bleiben bei Anthropic-gehosteten Umgebungen außerhalb der Sandbox; ein Proxy authentifiziert im Namen der Sitzung.
- Vorinstallierte Tools: Python, Node 20/21/22, Ruby, PHP, Java/Maven/Gradle, Go, Rust, C/C++, Docker, PostgreSQL, Redis, git/gh/jq/yq/ripgrep/tmux/vim/nano. [offiziell] ebd.

## BEST-PRACTICES-KANDIDATEN:

- Für Skills, MCP-Server, Hooks und Agents, die auch in Cloud-Sitzungen verfügbar sein sollen: **immer in `.claude/settings.json`, `.mcp.json`, `.claude/skills/`, `.claude/agents/`, `.claude/commands/` committen** — nichts, was nur in `~/.claude/*` liegt, erreicht eine Cloud-Sitzung.
- Multi-Repo-Cloud-Sitzungen (z. B. Projekte mit mehreren Repos) lesen aus jedem Repo **nur** `enabledPlugins` und `extraKnownMarketplaces` aus `.claude/settings.json` — keine Hooks, keine Permission-Rules, kein `env`-Block. Wer projektübergreifende Cloud-Automatisierung plant, sollte das einkalkulieren.
- `CLAUDE_CODE_REMOTE=true` ist der offiziell dokumentierte, zuverlässige Weg, um in Hooks/Skripten zwischen lokaler und Cloud-Ausführung zu unterscheiden (deckt sich mit der bereits im Profil verwendeten Praxis).
- MCP-Server für Cloud-Nutzung immer mit `claude mcp add --scope project` anlegen (schreibt nach `.mcp.json`) statt Default-/User-Scope (`~/.claude.json`), sonst fehlen sie in der Cloud.
- AGENTS.md-Fallback ist offiziell und aktiv (seit v2.1.277, Bedrock/Telemetrie-Sonderfälle erst ab v2.1.281 gefixt) — Projekte, die sowohl mit Codex/andere Agenten als auch Claude Code arbeiten, können sich auf ein gemeinsames AGENTS.md verlassen, sofern kein CLAUDE.md danebenliegt (sonst gewinnt CLAUDE.md).
- Für projektweite Skill-Bibliotheken, die per Symlink eingebunden werden (wie im eigenen Profil-Setup unter `C:\Users\barwa\proggs\OpenLauncher\Profiles\ClaudeCode\standard\skills`): Da `.claude/rules/`-Symlinks offiziell robust unterstützt sind (inkl. Fixes bis v2.1.239), aber `.claude/skills/`-Symlinks laut offenem Bug (#14836) in der Discovery/Autocomplete-Schicht unzuverlässig sind, ist es sicherer, Skill-Inhalte für Cloud-Sitzungen **entweder real zu committen (keine Symlinks)** oder über den claude.ai-Konto-Skill-Sync bereitzustellen, statt sich auf Git-Symlinks in `.claude/skills/` zu verlassen.

## BUG-KANDIDATEN:

1. **Symptom**: `.claude/skills/<name>`, wenn als Symlink (egal ob lokal per `ln -s`/`New-Item -ItemType SymbolicLink` erzeugt oder als Git-Symlink Mode 120000 aus dem Repo ausgecheckt) angelegt, taucht **nicht** in `/skills` oder im `/`-Autocomplete auf, obwohl laut offizieller Doku (code.claude.com/docs/en/skills) Symlinks für Projekt-/Personal-/Enterprise-Skill-Ordner explizit unterstützt sein sollen.
   **Ursache** (von einem Community-Mitglied im Issue präzise diagnostiziert, Kommentar vom 15.09.2026): Der Skill-Loader listet `~/.claude/skills/*` bzw. `.claude/skills/*` via `fs.readdirSync(dir, { withFileTypes: true })` und filtert mit `dirent.isDirectory()`. Für einen Symlink-Eintrag liefert `Dirent.isDirectory()` **immer `false`** (es reflektiert den Link-Eintrag selbst, nicht das Ziel), wodurch der Eintrag beim Scan als "keine Verzeichnis" übersprungen wird.
   **Version**: Reproduziert und root-caused auf Claude Code **2.1.212** (macOS); mehrfach bestätigt über einen langen Versionszeitraum (Issue seit 20.12.2025 offen, Original-Report bereits auf 2.0.73). Der Bug ist **zum Stand 27.09.2026 (lokale Version 2.1.283) weiterhin ungefixt**, letzter Kommentar 15.09.2026 ohne Reaktion vom Anthropic-Team.
   **Workaround**: Echte Kopie statt Symlink (`cp -r`); oder Community-Vorschlag `ln -s ../.agents/skills .claude/skills` (funktioniert nur, wenn alle Skills aus einem einzigen Zielverzeichnis kommen — bei mehreren einzeln verlinkten Skill-Ordnern greift der Fix nicht).
   **URL**: https://github.com/anthropics/claude-code/issues/14836 (offen)
   Verwandte, als Duplikate geschlossene Meldungen mit denselben Symptomen: https://github.com/anthropics/claude-code/issues/36659 (nur Projekt-`.claude` als Symlink, Autocomplete + `/skill-name`-Aufruf schlagen fehl), https://github.com/anthropics/claude-code/issues/25367 (User-Skills-Symlink, "Unknown skill"-Fehler bei Aufruf, führt Skill danach trotzdem korrekt aus).

2. **Symptom**: Linux-Sandbox startet **gar nicht**, wenn `.claude/skills` oder `.claude/hooks` ein Symlink ist.
   **Ursache**: Laut Changelog-Eintrag ein Sandbox-Startfehler speziell unter Linux bei symlinktem `.claude/skills`/`.claude/hooks`.
   **Version**: Betroffen vor **2.1.178**; **behoben in 2.1.178**. Da die aktuelle Version 2.1.283 deutlich neuer ist, ist dieser konkrete Startfehler heute nicht mehr relevant — aber er erklärt, warum in älteren Cloud-Sitzungen (Linux-VM) symlinkte Skill-Ordner sogar zum Totalausfall der Sitzung führen konnten.
   **Workaround**: Update auf ≥2.1.178 (für Cloud-Sitzungen ohnehin serverseitig aktuell).
   **URL**: https://github.com/anthropics/claude-code/blob/main/CHANGELOG.md (Eintrag unter Version 2.1.178, kein separates GitHub-Issue gefunden, nur der Changelog-Eintrag selbst)

3. **Symptom**: In Remote-/Cloud-Sitzungen (Desktop-App, Code-Tab) fehlen Projekt-Skills aus `.claude/skills/` im `/`-Slash-Menü — **auch wenn es sich explizit um normale, committete Verzeichnisse ohne Symlinks handelt**. `/reload-skills` liefert in der Remote-Sitzung eine deutlich kleinere Skill-Zahl (87) als in der lokalen Sitzung auf demselben Repo (164). Skills funktionieren aber bei explizitem Aufruf per vollem Namen oder auf Zuruf.
   **Ursache**: Ungeklärt, vom Melder als reines Client-seitiges Autocomplete-Problem der Remote-Sitzung eingegrenzt (Discovery-Layer, nicht Ausführung).
   **Version**: Gemeldet auf Claude Code Desktop-App (Code-Tab), macOS 15.6, Modell Opus 5, Meldedatum 17.09.2026 — noch offen, keine Versionsangabe im Report, aber sehr aktuell (10 Tage vor dem Recherche-Stand).
   **Workaround**: Vollen Skill-Namen tippen oder auf Englisch/Deutsch im Klartext danach fragen.
   **URL**: https://github.com/anthropics/claude-code/issues/95105 (offen)

4. **Symptom (widerlegt/kein Bug)**: Issue #38051 behauptete zunächst, dass User-Level-Skills unter `~/.claude/skills` als Symlink generell nicht geladen würden (Regression seit ~2.1.69).
   **Klarstellung**: Der Melder selbst hat das Issue als **nicht symlink-bezogen** zurückgezogen ("Even with fully copied (non-symlinked) real directories under ~/.claude/skills/, user-level skills are still not loaded. The root cause is elsewhere") und als `COMPLETED`/nicht-symlink-spezifisch geschlossen (24.03.2026). Nicht als eigenständigen Symlink-Bug werten.
   **URL**: https://github.com/anthropics/claude-code/issues/38051 (geschlossen, Selbstwiderruf)

## OFFEN/UNSICHER:

- **Nicht abschließend geklärt**, ob ein spezifisch **Git-getrackter Symlink (Mode 120000)** sich in der Cloud-Sandbox exakt gleich verhält wie ein lokal per `ln -s`/PowerShell erzeugter Symlink. Alle gefundenen Bug-Reports zu `.claude/skills`-Symlinks wurden lokal (macOS/Linux-Terminal) reproduziert, nicht explizit in einer `claude.ai/code`-Cloud-Sitzung mit einem git-mode-120000-Symlink. Die technische Ursache (Node.js `Dirent.isDirectory()` bei `readdirSync`) ist aber plattform- und quellen-unabhängig (git-Symlink vs. `ln -s`-Symlink sind auf Dateisystemebene identisch), daher ist eine Übertragung auf den Cloud-Fall mit hoher Wahrscheinlichkeit, aber nicht mit einem direkten Cloud-spezifischen Beleg, anzunehmen.
- **Output-Styles in Cloud-Sitzungen**: keine explizite Dokumentation gefunden, ob/wie sie übertragen werden. Vermutlich analog zu anderen `.claude/settings.json`-Werten (Single-Repo: ja, wenn committed), aber nicht wörtlich bestätigt.
- **`.claude/settings.local.json` in Cloud-Sitzungen**: nicht explizit in der offiziellen Tabelle genannt; die Nichtübertragung ist eine logische Ableitung (Datei i. d. R. gitignored, daher kein Teil des Klons), kein wörtliches Zitat.
- **AGENTS.md cloud-spezifisch**: Die offizielle Cloud-Tabelle (cloud-environments) nennt nur "Your repo's CLAUDE.md" namentlich als übertragen — AGENTS.md wird dort nicht separat aufgeführt. Die Übertragung von AGENTS.md in Cloud-Sitzungen ist plausibel (Teil des Klons, gleiche Lade-Logik wie lokal), aber nicht wortwörtlich für den Cloud-Fall bestätigt.
- Ob der reservierte Skill-Name `synced` (interner claude.ai-Account-Sync-Mechanismus) mit dem im Nutzer-Repo beobachteten, untrackten Ordner `OpenLauncher/Profiles/ClaudeCode/standard/skills/synced/` zusammenhängt, wurde nicht vertieft geprüft — liegt außerhalb des Recherche-Auftrags, aber als Beobachtung notiert.
- Plugin-Skill-Symlinks ("handle symlinks differently") wurden nicht im Detail nachrecherchiert (Verweis-Link nicht abgerufen) — falls relevant, separater Nachrecherche-Bedarf unter /docs/en/plugins/host-marketplace#share-files-within-a-marketplace-with-symlinks.
