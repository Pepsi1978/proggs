# Bekannte Bugs & Fallen: Claude Code in der Cloud (claude.ai/code, Handy-App, `--cloud`)

> **PFLICHT-LESEN vor Arbeit an Cloud-Abläufen:** Skills/Hooks für Cloud-Sitzungen, `apk-update-cloud`,
> `.github/workflows/android-cloud-build.yml`, Setup-Skripte/Environments, Teleport/Remote Control.
>
> **Stand:** 27.09.2026 · CLI 2.1.283 · 7 parallele Researcher. Fast alle Einträge sind Community-Issues in
> `github.com/anthropics/claude-code` ohne offizielle Fix-Bestätigung; die Statusseite zeigt dazu keine Incidents.
> **Rohberichte:** `best-practices/claude-tooling/claude-code-cloud-recherche-2026-09-27/r7-bugs.md` (+ r1–r6).
> **Gegenseite (Best Practices):** `best-practices/claude-tooling/claude-code-cloud.md`

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Falle | Gegenmittel | Volltext |
|---|---|---|---|
| B1 | Symlink-Skill-Ordner wird nicht gefunden | echter Ordner mit Verweis-SKILL.md | §B1 |
| B2 | Projekt-Skills fehlen im `/`-Menü der Remote-Sitzung | Skill beim Namen nennen; Auslöser per SessionStart-Hook einblenden | §B2 |
| B3 | Push 403 / leeres „authorized repository set“ | Repo beim Start als Quelle wählen, neue Sitzung | §B3 |
| B4 | Bot-Reviews (Codex) erreichen die Sitzung nicht | Kommentare aktiv abfragen (Warte-Skript, curl oder GitHub-MCP) | §B4 |
| B5 | Merge und danach das Bau-Warten vom Auto-Modus blockiert | melden, nicht umgehen; Modus „Änderungen akzeptieren“ | §B5 |
| B6 | PR-Titel vom ältesten Commit / Stop-Hook meldet Phantom-„unpushed commits“ | Titel selbst setzen; `git fetch --prune` | §B6 |
| B7 | Repo-Hooks ignoriert bei 2+ Repos | genau ein Repo pro Sitzung | §B7 |
| B8 | Plugins aus Repo-Settings nie registriert | nicht auf Repo-Plugins bauen | §B8 |
| B9 | Android: `dl.google.com` 403 | Custom-Netzwerk + Domain, oder CI bauen lassen | §B9 |
| B10 | Maven-Central DNS-Fehler | CI bauen lassen | §B10 |
| B11 | Proxy: Bun, TLS, Allowlist „alle Domains“ ignoriert | npm/pnpm; Domains einzeln | §B11 |
| B12 | Teleport bringt nur den ersten Turn | vorher pushen, lokal `git pull` | §B12 |
| B13 | Remote Control 0 Events / Amnesie nach Daemon-Neustart | Zwischenstände committen | §B13 |
| B14 | Browser/Playwright in der Cloud geht nicht | lokale Sitzung / Remote Control | §B14 |
| B15 | `gh` fehlt: Warte-Skripte melden still `timeout`/`kein-lauf` | Skripte ab 27.09.2026 nutzen curl + jq; sonst GitHub-MCP | §B15 |

---

## §B1 Symlink-Skill-Ordner in `.claude/skills/` wird nicht erkannt
- **Symptom:** `.claude/skills/<name>` als Symlink (lokal oder als Git-Symlink Mode 120000) fehlt in `/skills` und
  im Autocomplete; teils „Unknown skill“ beim Aufruf. Laut Doku sind Symlinks eigentlich erlaubt.
- **Ursache:** Scanner nutzt `readdirSync(withFileTypes)` + `Dirent.isDirectory()`, das ist für Symlinks immer `false`.
- **Versionen:** seit 2.0.73 gemeldet, auf 2.1.212 diagnostiziert, Stand 2.1.283 **offen**.
- **Fix (funktionserhaltend):** echter Ordner mit kleiner `SKILL.md` (gleiches `name`/`description`), deren Text auf die
  zentrale Anleitung verweist. Umgesetzt für `apk-update-cloud` am 27.09.2026.
- **Quelle:** https://github.com/anthropics/claude-code/issues/14836 (verwandt #36659, #25367). Separater Linux-Sandbox-
  Startfehler bei symlinktem `.claude/skills` wurde in **2.1.178 gefixt** (CHANGELOG).

## §B2 Projekt-Skills fehlen im `/`-Menü einer Remote-Sitzung
- **Symptom:** In Remote-Sitzungen zeigt `/reload-skills` weniger Skills als lokal; Autocomplete fehlt.
- **Status:** offen (gemeldet 17.09.2026). **Workaround:** Skill beim vollen Namen nennen oder in Worten anfordern;
  Pflicht-Abläufe zusätzlich per SessionStart-Hinweis einblenden. **Quelle:** #95105

## §B3 Push schlägt fehl: leeres „authorized repository set“ (403)
- **Symptom:** Sitzung kann nicht pushen, Git-Proxy lehnt ab. **Status:** offen, mehrere Varianten.
- **Workaround:** Repo beim Sitzungsstart als Quelle auswählen (nicht nachträglich), sonst neue Sitzung.
  Handoff-Sitzungen können teils pushen, aber keinen PR öffnen (#90238) → zweite normale Sitzung.
- **Quelle:** #92188, #76248, #96075, #77482

## §B4 Bot-Reviews/-Kommentare werden an die Sitzung nicht zugestellt
- **Symptom:** Auto-Fix-/PR-Ereignisse kommen für Menschen-Kommentare und CI an, **nicht** für Bots (Codex-Reviewer,
  Copilot, Vercel).
- **Ursache (vermutet):** zu breiter Anti-Schleifen-Filter nach Absender-Typ. **Status:** als Duplikat geschlossen (#52474), Feature-Wunsch #50555.
- **Fix:** Nicht auf Ereignisse warten, sondern pollen (ohne `gh` siehe §B15):
  `gh api repos/O/R/issues/N/comments` (Codex-Sammelkommentar „Codex Review Summary … Completed“),
  `gh api repos/O/R/pulls/N/comments` (Zeilen-Befunde P1/P2), `gh api repos/O/R/issues/N/reactions` (👍 = keine Befunde).
- **Quelle:** https://github.com/anthropics/claude-code/issues/62977

## §B5 Merge per API vom Auto-Modus-Klassifizierer blockiert
- **Symptom:** `PUT /pulls/N/merge` → `[Auto-Mode Bypass]`, später auch PR-Erstellung. Gemeldet mit 2.1.275 + internem Plugin.
- **Status:** offen seit 23.09.2026. **Umgang:** Blockade nicht umgehen; Frank melden, dass der PR offen ist.
- **Nachtrag 27.09.2026 (NewsKompass, PR #122):** Der Merge per GitHub-MCP (`merge_pull_request`) ging durch, aber der
  nächste Befehl danach (`warte-auf-bau.sh`) wurde mit `[Merge Without Review]` abgelehnt. Der Klassifizierer sieht die
  Freigabe in `cloud.md` nicht als Erlaubnis zum Selbst-Merge. **Gegenmittel:** Berechtigungsmodus der Sitzung auf
  „Änderungen akzeptieren“ statt Auto-Modus (Frank hat das am 27.09.2026 umgestellt). Den Bau kann man trotz Sperre
  ohne Umgehung lesen, sobald Frank ausdrücklich danach fragt; nie still als „blockiert“ stehen lassen.
- **Quelle:** #96257

## §B6 PR-Titel vom ältesten Commit / Phantom-„unpushed commits“
- „Create PR“ füllt Titel/Text aus dem **ältesten** Commit (#93852) → selbst setzen.
- Stop-Hook meldet nach einem gemergten (v. a. squash-) PR noch „unpushed commits“ und blockiert die Sitzung
  (#92338, #95524) → `git fetch --prune`, notfalls `git checkout -B <branch> origin/main`. Nie veröffentlichte
  History umschreiben (#89684).

## §B7 Repo-`.claude/settings.json` ignoriert bei zwei Repos
- Bei 2+ angehängten Repos werden Hooks, Permissions und `.mcp.json` nicht geladen (offiziell dokumentiert + #89215).
- **Fix:** genau ein Repo pro Cloud-Sitzung.

## §B8 Plugins/Marketplaces aus Repo-Settings werden in der Cloud nie registriert
- `extraKnownMarketplaces`/`enabledPlugins` greifen nicht (#78119, offen). Nur manuelles `claude plugin marketplace add`.

## §B9 Android: 403 beim Auflösen von AGP/androidx
- `dl.google.com` fehlt in der Trusted-Liste; teils auch `maven.google.com`-Fehler berichtet.
- **Fix:** Environment „Custom“ + `dl.google.com` (+ Standardliste), oder gar nicht in der Cloud bauen (CI).
- **Quelle:** Allowlist https://code.claude.com/docs/en/cloud-environments; Praxisfall LocNgu/YAPT-Yet-Another-Plant-Tracker#419

## §B10 Maven Central: `Temporary failure in name resolution`
- DNS-Fehler für `repo.maven.apache.org` trotz Trusted-Listung. Offen seit 12/2025. **Fix:** CI bauen lassen. **Quelle:** #13372

## §B11 Proxy-Fallen
- **Bun** mit dem Security-Proxy inkompatibel (offiziell) → npm/pnpm.
- TLS/Zertifikatsfehler, v. a. Python 3.13+ (#87378, #90521) → Prüfung nicht global abschalten.
- „Alle Domains“/Custom-Domains werden ignoriert, `x-deny-reason: host_not_allowed` (#93561, #93677 offen; #19087, #93525 geschlossen)
  → Domains einzeln eintragen, neue Sitzung.
- Verschachtelter `claude`-Unterprozess (z. B. von Vercel CLI) kapert die Git/gh-Proxy-Config (#96856) → Drittwerkzeuge nicht `claude` aufrufen lassen.

## §B12 Teleport: nur erster Turn / Verlauf fehlt
- #94836 offen, #92734 (Prompt-History fehlt), #74277. **Fix:** vorher committen + pushen, lokal `git pull`; Transkript gegenprüfen.

## §B13 Remote Control
- Seit 15.09.2026 teils 0 Events (#95873, serverseitig); Daemon-Neustart startet Sitzungen ohne `--resume` → Kontextverlust (#84468).
- Cross-Device-Sitzung führt Befehle auf dem Ursprungsrechner aus (#92144) → vor Aktionen `hostname`/`pwd` prüfen.

## §B14 Kein Browser in der Cloud
- Playwright/Puppeteer/Chromium scheitern (Proxy ohne CONNECT, CDN nicht erlaubt). „Not planned“ (#75632). → lokal/Remote Control.

## §B15 `gh` fehlt in der Cloud-Sitzung: Warte-Skripte liefen still ins Leere
- **Symptom:** `warte-auf-codex.sh` meldet nach 8 min `CODEX=timeout`, obwohl Codex nach 2 min fertig war;
  `warte-auf-bau.sh` meldet sofort `BAU=kein-lauf (Commit )` und `gh: command not found`.
- **Ursache:** Die Cloud-VM hat kein `gh` installiert (gesehen 27.09.2026, NewsKompass PR #122/#123). Die alten Skripte
  fingen den Fehler mit `|| echo 0` ab und zählten ihn als „noch nichts da“ — kein Hinweis, keine Fehlermeldung.
- **Vorhanden sind:** `curl`, `jq` und `GH_TOKEN`/`GITHUB_TOKEN`; `api.github.com` ist erreichbar.
- **Fix (27.09.2026):** `apk-update-cloud/github-api.sh` nimmt `gh`, wenn da, sonst `curl` mit Token. Beide Warte-Skripte
  prüfen den Zugang vorab (ohne `gh` ist ein Token Pflicht: anonym nur 60 Anfragen/Stunde) und brechen sonst sofort
  — oder nach 3 gescheiterten Abfragen in Folge — mit `ZUGANG=fehlt (…)` ab, statt still zu warten. Dann die
  GitHub-MCP-Werkzeuge nehmen: `pull_request_read` (`get_review_comments`, `get_comments`), `actions_list`
  (`list_workflow_runs`, `android-cloud-build.yml`) und `actions_get` (`get_workflow_run`).
- **Merke:** Ein Warte-Skript, das Fehler als „0“ zählt, sieht aus wie ein langsamer Dienst. Zugang immer zuerst prüfen.

## §B16 „Bypass permissions“ gibt es in der Cloud nicht
- **Symptom:** In der App stehen nur Plan, Auto und „Änderungen akzeptieren“. `defaultMode: "bypassPermissions"`
  oder `"dontAsk"` aus einer Einstellungsdatei wird still ignoriert.
- **Status:** so gewollt, dokumentiert: https://code.claude.com/docs/en/permission-modes („Bypass permissions isn't
  available“). Der Automodus lässt sich nur über `~/.claude/settings.json` oder Organisations-Einstellungen feiner
  einstellen, nicht über die Repo-Datei.
- **Umgang (27.09.2026):** Modus „Änderungen akzeptieren“ plus Freigabeliste `permissions.allow` in
  `.claude/settings.json` (alle Werkzeuge, alle MCP-Server). Ob die Cloud die Repo-Freigabeliste beachtet, ist
  nicht dokumentiert. Fragt die Sitzung trotzdem nach, hier eintragen.

## Weitere, schwächer belegte Einträge
- SKILL.md im System-Prompt angekündigt, aber nicht gemountet (#26254, offen).
- Vorhandene Skills/Subagents werden vom Modell ignoriert (#90182) → Skill ausdrücklich nennen.
- PreToolUse-Hook-Deadlock in Cowork-Cloud-Sitzungen (#85581) → PreToolUse nie pauschal auf alle Tools matchen.
- „execution error“ auf jede Nachricht (#72602 → #54528) → neue Sitzung, Statusseite prüfen.
- Hooks hören nach ca. 2,5 h auf (#16047, ungeprüft ob aktuell).
- Vor 2.1.282: GitHub-Token lief nach ca. 8 h ab (jetzt Auto-Erneuerung, CHANGELOG 2.1.282).
