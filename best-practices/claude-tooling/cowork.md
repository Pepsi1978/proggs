# Cowork (Desktop-App) — Best Practices (Stand 2026-06-13, Quellenstand April–Juni 2026)

> Best-Practices für den **Cowork-Modus der Claude-Desktop-App** (macOS/Windows). Cowork bringt die
> agentische Architektur von Claude Code ohne Terminal in die Desktop-App — für nicht-programmierende
> Wissensarbeit. Diese Datei ist die "richtige Seite der Medaille": *wie man Cowork von vornherein
> richtig nutzt*. Das Gegenstück im Bug-Almanach existiert inzwischen:
> [`bugs/claude-tooling/cowork.md`](../../bugs/claude-tooling/cowork.md) (allgemein) und
> [`bugs/claude-tooling/cowork-scheduled-tasks.md`](../../bugs/claude-tooling/cowork-scheduled-tasks.md)
> (geplante/wiederkehrende Aufgaben — Gegenseite zu §5 dieser Datei).
>
> **Status:** Cowork startete Januar 2026 als Research Preview; die Produktseite nennt es inzwischen
> "generally available", einzelne Teilfunktionen (Computer-Use, Handy/Dispatch) bleiben Research Preview.
> Offizielle Quellen sind hier leicht uneinheitlich.
>
> **Versions-Anker:** kein Software-Changelog wie bei Kotlin/Swift — Anker ist der Stand der offiziellen
> Anthropic-Support-/Doku-Seiten (April–Juni 2026), live recherchiert am 2026-06-13 mit 7 parallelen Researchern.
>
> **Quellen-Rangordnung:** offiziell (support.claude.com, claude.com/docs, code.claude.com,
> anthropic.com/engineering) = Grundwahrheit. Community/Presse als `extern` gelabelt (sekundär,
> überstimmt nie das Offizielle). Jeder Eintrag trägt Quelle + Datum + `offiziell`/`extern`-Flag.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Voraussetzung | Nur Desktop (macOS/Windows), bezahltes Abo; App offen + Rechner wach, sonst stoppt die Aufgabe | §1 |
| 2 | Einrichtung | `/setup-cowork`; Connectors aktivieren; Arbeitsort wählen (Ordner ODER Projekt) | §1 |
| 3 | Ordner vs. Projekt | Memory nur in **Projekten**, nicht über Standalone-Sessions | §1 |
| 4 | Sicherheitsmodus | "Ask before acting" als Standard; vor endgültigem Löschen fragt Claude immer | §1 |
| 5 | Skill-Beschreibung | Claude.ai-Limit **200 Zeichen** — Trigger knapp & keyword-stark | §2 |
| 6 | Eigene Skills | Customize > Skills > ZIP-Upload (Ordnername = name-Feld), dann Toggle aktivieren | §2 |
| 7 | Plugins | Nur in Cowork/Code, **nicht in Chat**; eigene Git-Repos als Marketplace nutzbar | §2 |
| 8 | Connectors | Claude erbt deine Quellsystem-Rechte; Gmail liest/Entwürfe, **kein Versand** | §3 |
| 9 | MCP in Cowork | Remote-Connectors laufen über Anthropics Cloud → eigene Server: Anthropic-IPs allowlisten | §3 |
| 10 | Datei-Arbeit | Dedizierter Arbeitsordner; sensible Ordner nicht verbinden; Mount `read-write-no-delete` als Schutz | §4 |
| 11 | Scheduled Tasks | Prompt **selbst-enthaltend**; Catch-up-Fallstrick → Zeit-Guardrails in den Prompt | §5 |
| 12 | Live-Artefakte | Nutzen Connectors **ohne Rückfrage**; lokal, (noch) nicht teilbar | §5 |
| 13 | Tool-Hierarchie | Connector → Claude in Chrome → Computer Use (in dieser Reihenfolge) | §6 |
| 14 | Computer Use | Nur Pro/Max, **keine Sandbox**; Links aus Mail/Doku nie per Computer-Use klicken | §6 |
| 15 | Grenzen | Compliance-Blindspot (nicht in Audit-Logs); deutlich höherer Usage-Verbrauch als Chat | §7 |
| 16 | Git push aus Cowork (dauerhaft) | Kein nativer Push-Weg/Secret-Store/Startup-Hook → Token in `.git/credentials` (im Mount, nie committet, relativer Pfad) + `credential.helper store` lokal; Remote NICHT auf SSH/Token-URL ändern (geteilte `.git/config`) | §3a |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

> Zweite Seite der Medaille: Der Bug-Almanach sagt *was schiefgeht und wie man es umgeht*, diese
> Datei *wie man Cowork von vornherein richtig nutzt*. Gegenstücke:
> [`bugs/claude-tooling/cowork.md`](../../bugs/claude-tooling/cowork.md) (allgemein) und
> [`bugs/claude-tooling/cowork-scheduled-tasks.md`](../../bugs/claude-tooling/cowork-scheduled-tasks.md) (geplante Aufgaben).

| Best-Practice (diese Datei) | Bug-Gegenpart im Almanach |
|---|---|
| §1 Überblick & Einrichtung | `cowork.md` — VM-Architektur, Berechtigungen, Mount-Verhalten |
| §2 Skills & Plugins | `cowork.md` — Skill-/Plugin-Fallen, 200-Zeichen-Limit |
| §3 Connectors & MCP | `cowork.md` — Connector-/MCP-Fallen über Anthropics Cloud |
| §3a Git-Push aus Cowork | `cowork.md` §10a — Mount-Locks, fileMode, LFS, `cowork-git.sh` |
| §4 Datei-Arbeit & Ergebnis-Dokumente | `cowork.md` — Mount-Modi, Lösch-/Truncation-Fallen |
| §5 Geplante Aufgaben & Live-Artefakte | `cowork-scheduled-tasks.md` — Catch-up, Cron, Boot-Loop |
| §6 Computer-Steuerung & Browser | `cowork.md` — Computer-Use-/Chrome-Fallen |
| §7 Grenzen, Datenschutz, Sicherheit | `cowork.md` — Compliance-Blindspot, Usage |

---

## 1. Überblick & Einrichtung

- **Was Cowork ist:** Claude plant mehrstufige Aufgaben, zerlegt sie in Sub-Agenten und liefert fertige Outputs direkt ins Dateisystem — kein Terminal nötig. Quelle: support.claude.com/en/articles/13345190 · 2026-06 · `[offiziell]`
- **Voraussetzungen:** nur Claude-Desktop-App für **macOS oder Windows** (kein Web, kein eigenständiges Mobile), bezahltes Abo (Pro/Max/Team/Enterprise), durchgehende Internetverbindung. App muss offen bleiben und der Rechner wach, sonst stoppt die Aufgabe. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Best Practice Einrichtung (4 Schritte):** 1) Cowork-Tab öffnen/App aktualisieren; 2) unter "Customize" die täglich genutzten Connectors aktivieren (+ optional Claude in Chrome); 3) **Arbeitsort** wählen — lokaler Ordner ODER Projekt; 4) Aufgabe stellen. `/setup-cowork` startet die geführte Einrichtung. Quelle: claude.com/resources/tutorials/get-started-in-claude-cowork-in-three-steps · 2026-04-27 · `[offiziell]`
- **Ordner vs. Projekt:** Ordner = Claude liest/schreibt dort, eng oder breit zuschneidbar. Projekt = Workspace mit eigenen Dateien, Instruktionen und **Memory**, das über Sessions bleibt. Memory gibt es NUR in Projekten, nicht über Standalone-Sessions. Quelle: support.claude.com/en/articles/14116274 · 2026-04-09 · `[offiziell]`
- **Globale + Ordner-Instruktionen:** Settings > Cowork = globale Anweisungen (Tonfall, Format, Rolle) für jede Session; "Folder instructions" ergänzen projektspezifischen Kontext und kann Claude während der Session selbst aktualisieren. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Architektur (Sicherheits-Kern):** Agent-Loop + Dateioperationen laufen **nativ** auf dem Gerät (Permission-System auf App-Ebene); nur Shell/Code läuft in einer **isolierten Linux-VM** (Apple Virtualization.framework / Hyper-V). Fällt die VM aus, laufen Datei-/Web-Tools weiter, nur Shell/Code meldet "workspace unavailable". Quelle: support.claude.com/en/articles/14479288 · 2026-04-24 · `[offiziell]`
- **Berechtigungsmodi:** "Ask before acting" (empfohlen, fragt vor jeder Aktion) vs. "Act without asking" (schneller, riskanter, nur unter Aufsicht + vertrauten Quellen). In BEIDEN Modi fragt Claude immer vor endgültigem **Löschen**. Quelle: support.claude.com/en/articles/13345190 + /13364135 · `[offiziell]`

## 2. Skills & Plugins

- **Skill = Verzeichnis + SKILL.md**, dynamisch geladen über **Progressive Disclosure** (Stufe 1: nur Name+Beschreibung ~100 Token; Stufe 2: voller SKILL.md; Stufe 3: References/Skripte bei Bedarf). Quelle: claude.com/docs/skills/overview · `[offiziell]`
- **Voraussetzung:** "Code execution and file creation" muss aktiv sein (Settings > Capabilities; Team/Enterprise: Owner unter Organization settings > Skills). Quelle: support.claude.com/en/articles/12512180 · `[offiziell]`
- **SKILL.md-Pflichtfelder:** YAML-Frontmatter `name` (nur a-z/0-9/Bindestrich, = Verzeichnisname) + `description`. **Beschreibungs-Limit in Claude.ai = 200 Zeichen** (Agent-Skills-Standard erlaubt 1024) — Trigger-Beschreibungen knapp und keyword-stark halten. Struktur mit `scripts/`, `references/`, `assets/`; SKILL.md < 500 Zeilen, Detail auslagern und in SKILL.md erwähnen. Quelle: claude.com/docs/skills/how-to · `[offiziell]`
- **Eigene Skills hochladen:** Customize > Skills > "+" > "Upload a skill" als **ZIP** (Ordnername = name-Feld, Skill-Ordner muss im ZIP enthalten sein), dann per Toggle aktivieren. Custom Skills sind privat zum Account. Quelle: support.claude.com/en/articles/12512180 · `[offiziell]`
- **Plugin = Paket** aus Skills + MCP-Connectors + Sub-Agents + Slash-Commands + Hooks; file-based (Markdown + JSON, kein Build). Plugins laufen in Cowork und Code, **nicht in Chat** (dort Hooks/Sub-Agents ausgegraut, Skills funktionieren). Quelle: claude.com/docs/cowork/guide/plugins + github.com/anthropics/knowledge-work-plugins · `[offiziell]`
- **Marketplaces:** Default "Knowledge Work" vorinstalliert; weitere Anthropic-Marktplätze (Financial Services, Legal, Life Sciences) und eigene **Git-Repos** als Marketplace (`owner/repo` oder URL) hinzufügbar. Limits: Plugin ≤200 MB/5.000 Dateien, ≤500 Plugins/Marketplace, ≤25 Marketplaces. Quelle: claude.com/docs/cowork/guide/plugins · `[offiziell]`
- **Plugins/Skills in Cowork selbst bauen:** Plugin "Plugin Create" / `cowork-plugin-management` führt durch Discovery→Planung→Design→Implementierung→Packaging und liefert eine installierbare `.plugin`-Datei. "Customize" an einem Plugin öffnet eine neue Cowork-Session, in der Claude die Skills auf die tatsächlich genutzten Tools umschreibt. **Nur Desktop** (bearbeitet lokale Dateien). Quelle: claude.com/resources/tutorials/how-to-customize-plugins-in-cowork · `[offiziell]`

## 3. Connectors & MCP

- **Grundprinzip:** Claude **erbt pro Person die Rechte des Quellsystems** — kein Zugriff dort = kein Zugriff hier. Quelle: support.claude.com/en/articles/11176164 · `[offiziell]`
- **Zwei Typen:** Remote-Connectors (Standard, überall inkl. Cowork, für Cloud/SaaS) vs. Desktop-Extensions (lokal, nur Claude Desktop + Code, NICHT Cowork-Web/claude.ai). Quelle: support.claude.com/en/articles/11725091 · 2026-04-15 · `[offiziell]`
- **Wichtig für Cowork:** Remote-Connectors laufen über **Anthropics Cloud**, nicht über das lokale Netz — ein eigener MCP-Server muss aus Anthropics IP-Ranges über das öffentliche Internet erreichbar sein (sonst Anthropic-IPs in der Firewall allowlisten). Quelle: support.claude.com/en/articles/11175166 · 2026-04-02 · `[offiziell]`
- **Gmail/Drive/Calendar Funktionsumfang:** Gmail = lesen/suchen, Entwürfe erstellen (**kein Versand durch Claude**), Labels/Threads; nur Anhang-Metadaten. Calendar = Events sehen/anlegen/ändern/löschen. Drive = Docs/Sheets/Slides/PDFs/Office lesen, Ordner/Upload; nur Textextraktion. Jede Aktion braucht Freigabe. Quelle: support.claude.com/en/articles/10166901 · `[offiziell]`
- **Auth:** delegiertes Pro-Nutzer-OAuth, keine Service-Accounts; org-weite Aktivierung macht den Connector nur verfügbar, jeder Nutzer authentifiziert sich selbst. Tokens verschlüsselt, pro Nutzer gescoped. Quelle: support.claude.com/en/articles/14503689 · 2026-06 · `[offiziell]`
- **Admin-Kontrollen (Team/Enterprise):** pro Connector "Always allow / Needs approval / Blocked" je Aktionskategorie, org-weit erzwungen. Bei 10+ aktiven Connectors "On demand"-Tool-Access nutzen, um Kontext zu sparen. Quelle: support.claude.com/en/articles/11176164 · `[offiziell]`

## 3a. Git / GitHub-Push aus Cowork dauerhaft einrichten

> Recherchiert 2026-06-15 (7 Researcher). Bug-Gegenseite: `bugs/claude-tooling/cowork.md` §10a.
> Schritt-Anleitung für Frank: `~/proggs/COWORK-GIT-PUSH-SETUP.md`.

- **Zuverlässig aus der VM pushen (Windows-Lock-Problem gelöst):** Auf dem gemounteten `.git` kann die
  Cowork-VM ihre `.lock`-Dateien nicht löschen → `commit`/`push` aus der VM hängt. Lösung: das git-dir
  auf die **VM-eigene Platte** legen (`GIT_DIR` auf ext4, `GIT_WORK_TREE` = der Mount-Ordner), Quelle der
  Wahrheit bleibt `origin/main`. Im Repo gekapselt als `cowork-git.sh` (`bash cowork-git.sh push "msg"`).
  Verifiziert 2026-06-15 (separates git-dir hält ALLE Locks vom Mount fern; Mount-`.git` + Host-Terminal
  bleiben unberührt). Bug-Gegenseite: `bugs/claude-tooling/cowork.md` §10a.5/§10a.6/§10a.7.
- **`git add -A` aus Cowork — vier weitere Mount-Artefakte (alle in `cowork-git.sh` gelöst):** unlesbare
  Symlinks (readlink-I/O → `skip-worktree`), Datei-Modus immer 0755 (`core.fileMode false`), untrackte
  Build-Bäume (`**/build/`, `**/.gradle/`, `**/node_modules/` in `.gitignore`), LFS-Dateien als Vollinhalt
  (LFS-Muster per `skip-worktree` ausnehmen, sonst 100-MB-Push-Ablehnung). Langer Push muss in EINEM
  VM-Aufruf laufen (Hintergrundprozesse überleben den Sandbox-Wechsel nicht). Details: Almanach §10a.7.

- **Ausgangslage:** Cowork hat **keinen** nativen `git push`-Weg, **keinen** Secret-Store und
  **keinen** Startup-Hook. Die VM startet pro Session frisch → VM-Home (`~/.git-credentials`)
  ist nie persistent. Persistent ist nur der gemountete Ordner. Quelle: support.claude.com/.../10167454
  + code.claude.com/docs/en/claude-code-on-the-web + Architektur-Doku · 2026 · `[offiziell]`
- **Richtiger Weg (dauerhaft):** Token in `.git/credentials` ablegen + `git config
  credential.helper 'store --file=.git/credentials'` **lokal** (nicht `--global`). `.git/` liegt
  im persistenten Mount und wird nie committet; **relativer** Pfad umgeht die nicht-deterministischen
  Mount-Pfade. Quelle: git-scm.com/docs/git-credential-store · `[offiziell]`
- **Remote-URL NICHT ändern:** `.git/config` ist mit dem Host-Terminal **geteilt** — eine
  Umstellung auf SSH oder Token-in-URL beschädigt das funktionierende Terminal-Setup.
  `credential.helper store` ist **additiv** und die sichere Wahl. Quelle: Recherche 2026-06-15.
- **Token-Hygiene:** Fine-grained PAT, nur Ziel-Repo, **Contents: Read and write** + **Metadata: Read**
  (sonst `403`); kurze Laufzeit + Rotation (max 366 Tage). Bei **privatem** Repo gibt es **kein**
  Auto-Revoke bei Leak → manuell widerrufen. Quelle: docs.github.com (PAT-Permissions/Expiration/
  Secret-Scanning) · `[offiziell]`
- **Alternative SSH-Deploy-Key** (liefe nie ab) bewusst NICHT empfohlen, solange `.git/config`
  zwischen VM und Host geteilt ist (würde das Terminal-Remote auf SSH zwingen). Quelle: docs.github.com/.../managing-deploy-keys · `[offiziell]`

## 4. Datei-Arbeit & Ergebnis-Dokumente

- **Direktzugriff:** liest/schreibt in verbundenen Ordnern ohne Up-/Download; Ergebnisse landen direkt im Dateisystem. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Drei Mount-Modi pro Ordner:** read-only, read-write, read-write-no-delete — "Blast Radius" granular begrenzen. Quelle: anthropic.com/engineering/how-we-contain-claude · `[offiziell]`
- **Erzeugbare Formate:** Excel (.xlsx mit echten Formeln/VLOOKUP/bedingter Formatierung/mehreren Tabs), PowerPoint (.pptx), Word (.docx), PDF, PNG-Visualisierungen, Python-Skripte. Konvertierung und Multi-Step-Pipelines (CSV→Modell→Memo→Deck). Quelle: support.claude.com/en/articles/12111783 · 2026-04-29 · `[offiziell]`
- **Markdown "Edit with Claude":** Text markieren → gezielt an der Stelle editieren lassen, ohne die Passage im Thread zu beschreiben. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Best Practice:** dediziertes Arbeitsverzeichnis statt breitem Zugriff; sensible Dateien (Finanzen, Zugangsdaten, Personenakten) NICHT verbinden; wichtige Dateien sichern. Datei-Limit bei Erstellung 30 MB. Quelle: support.claude.com/en/articles/13364135 + /12111783 · `[offiziell]`

## 5. Geplante Aufgaben & Live-Artefakte

> **Tiefen-Almanach (Gegenseite, Bezugstabelle dort am Ende):**
> [`bugs/claude-tooling/cowork-scheduled-tasks.md`](../../bugs/claude-tooling/cowork-scheduled-tasks.md)
> (Stand 2026-06-15). Kernregeln: System bewusst wählen (Cloud-Routine für Zuverlässigkeit ohne wachen
> PC, Local Task für lokale Dateien, `/loop` nur in offener Session) · Zeit-Guardrails gegen Catch-up ·
> keine High-Frequency-Cron (Boot-Loop-Risiko) · nach Anlegen „Run now" + „always allow" · ersten
> MCP-Call per Subagent absetzen (Warm-up) · keine sensiblen Connectors an unbeaufsichtigte Tasks.

- **Scheduled Tasks** via `/schedule` oder Seitenleiste "Scheduled" > "+ New task". Kadenzen: stündlich, täglich, wöchentlich, werktags, manuell; Sonstiges (z.B. alle 6 h, Monatserster, einmaliger Lauf) per natürlicher Sprache. Cron in **lokaler Zeitzone**. Jeder Lauf startet **frisch ohne Erinnerung** → Prompt muss selbst-enthaltend sein (Connectors, Format, Präferenzen). Quelle: support.claude.com/en/articles/13854387 · 2026-04-09 + code.claude.com/docs/en/desktop-scheduled-tasks · `[offiziell]`
- **Catch-up-Fallstrick:** Beim Aufwachen/App-Start wird **genau ein** verpasster Lauf nachgeholt (zuletzt verpasster Zeitpunkt der letzten 7 Tage), ältere verworfen → eine 9-Uhr-Aufgabe kann um 23 Uhr laufen. Gegenmittel: Zeit-Guardrails in den Prompt ("nur heutige Daten; nach 17 Uhr überspringen"). Quelle: code.claude.com/docs/en/desktop-scheduled-tasks · `[offiziell]`
- **Live-Artefakte:** persistente, interaktive HTML-Seiten (Tracker, Dashboard), die sich beim Öffnen mit frischen Connector-/Datei-Daten aktualisieren; eigener "Live artifacts"-Tab mit Versionshistorie. Self-contained HTML, nur Chart.js/Grid.js/Mermaid per CDN erlaubt; Daten über `window.cowork.callMcpTool()`. Quelle: support.claude.com/en/articles/14729249 · 2026-04-24 · `[offiziell]`
- **Artefakt-Vorsicht:** Artefakte nutzen freigegebene Connectors **ohne erneute Rückfrage** → bei datenverändernden Connectors aufpassen. Artefakte sind lokal, (noch) nicht teilbar. Quelle: support.claude.com/en/articles/14729249 · `[offiziell]`

## 6. Computer-Steuerung & Browser

- **Tool-Hierarchie (Best Practice):** 1) dedizierter **Connector** (schnell, präzise, API) → 2) **Claude in Chrome** (Web-App ohne Connector, DOM-bewusst) → 3) **Computer Use** (native Desktop-Apps, App-übergreifend; breitestes, langsamstes Mittel). Quelle: support.claude.com/en/articles/14128542 · 2026-04-24 · `[offiziell]`
- **Computer Use:** Research Preview, **nur Pro/Max** (Team/Enterprise kein Zugriff). Aktivierung: Settings > General > "Computer use". **Tier-Modell:** Browser/Trading = "read" (nur sehen), Terminals/IDEs = "click" (nur klicken, kein Tippen), alles andere = "full". Keine Sandbox — läuft auf dem echten Desktop. Quelle: support.claude.com/en/articles/14128542 + code.claude.com/docs/en/computer-use · `[offiziell]`
- **Claude in Chrome:** Beta, alle Bezahlpläne, **nur Google Chrome**. In Cowork: Settings > Connectors > Claude in Chrome > Configure; pro Konversation manuell aktivieren. JavaScript-Ausführung braucht separate Pro-Domain-Freigabe (zentrale Schutzschicht). Pro: nur Haiku 4.5. Quelle: support.claude.com/en/articles/12012173 + /12902446 · `[offiziell]`
- **Link-Sicherheit (kritisch):** Web-Links in E-Mails/Nachrichten/Dokumenten gelten als verdächtig. **Niemals Links mit Computer-Use anklicken** — URL über die Chrome-MCP öffnen; volle URL vorher prüfen. Quelle: support.claude.com/en/articles/14128542 + MCP-Instruktionen · `[offiziell]`
- **Keine Finanztransaktionen:** Claude führt nie Trades/Orders/Überweisungen aus; Trading-/Krypto-Apps standardmäßig blockiert. Quelle: support.claude.com/en/articles/14128542 + /12902446 · `[offiziell]`

## 7. Grenzen, Datenschutz, Sicherheit

- **Compliance-Blindspot:** Cowor