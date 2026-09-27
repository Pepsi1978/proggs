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
| B4 | Bot-Reviews (Codex) erreichen die Sitzung nicht | Kommentare aktiv per `gh api` abfragen | §B4 |
| B5 | Merge per API vom Auto-Modus-Klassifizierer blockiert | melden, nicht umgehen | §B5 |
| B6 | PR-Titel vom ältesten Commit / Stop-Hook meldet Phantom-„unpushed commits“ | Titel selbst setzen; `git fetch --prune` | §B6 |
| B7 | Repo-Hooks ignoriert bei 2+ Repos | genau ein Repo pro Sitzung | §B7 |
| B8 | Plugins aus Repo-Settings nie registriert | nicht auf Repo-Plugins bauen | §B8 |
| B9 | Android: `dl.google.com` 403 | Custom-Netzwerk + Domain, oder CI bauen lassen | §B9 |
| B10 | Maven-Central DNS-Fehler | CI bauen lassen | §B10 |
| B11 | Proxy: Bun, TLS, Allowlist „alle Domains“ ignoriert | npm/pnpm; Domains einzeln | §B11 |
| B12 | Teleport bringt nur den ersten Turn | vorher pushen, lokal `git pull` | §B12 |
| B13 | Remote Control 0 Events / Amnesie nach Daemon-Neustart | Zwischenstände committen | §B13 |
| B14 | Browser/Playwright in der Cloud geht nicht | lokale Sitzung / Remote Control | §B14 |

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
- **Fix:** Nicht auf Ereignisse warten, sondern pollen:
  `gh api repos/O/R/issues/N/comments` (Codex-Sammelkommentar „Codex Review Summary … Completed“),
  `gh api repos/O/R/pulls/N/comments` (Zeilen-Befunde P1/P2), `gh api repos/O/R/issues/N/reactions` (👍 = keine Befunde).
- **Quelle:** https://github.com/anthropics/claude-code/issues/62977

## §B5 Merge per API vom Auto-Modus-Klassifizierer blockiert
- **Symptom:** `PUT /pulls/N/merge` → `[Auto-Mode Bypass]`, später auch PR-Erstellung. Gemeldet mit 2.1.275 + internem Plugin.
- **Status:** offen seit 23.09.2026. **Umgang:** Blockade nicht umgehen; Frank melden, dass der PR offen ist.
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

## Weitere, schwächer belegte Einträge
- SKILL.md im System-Prompt angekündigt, aber nicht gemountet (#26254, offen).
- Vorhandene Skills/Subagents werden vom Modell ignoriert (#90182) → Skill ausdrücklich nennen.
- PreToolUse-Hook-Deadlock in Cowork-Cloud-Sitzungen (#85581) → PreToolUse nie pauschal auf alle Tools matchen.
- „execution error“ auf jede Nachricht (#72602 → #54528) → neue Sitzung, Statusseite prüfen.
- Hooks hören nach ca. 2,5 h auf (#16047, ungeprüft ob aktuell).
- Vor 2.1.282: GitHub-Token lief nach ca. 8 h ab (jetzt Auto-Erneuerung, CHANGELOG 2.1.282).
