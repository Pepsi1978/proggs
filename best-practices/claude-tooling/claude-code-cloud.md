# Best Practices: Claude Code in der Cloud (Cloud-Sitzungen, claude.ai/code, Handy-App)

> **Stand:** 27.09.2026, 09:30 Uhr · recherchiert mit 7 parallelen Researchern (Sonnet 5)
> **Versions-Anker:** lokale CLI 2.1.283; Cloud-Sitzungen laufen serverseitig aktuell (Changelog-Einträge
> bis 2.1.282 berücksichtigt). Cloud-Umgebung: Anthropic-VM, Ubuntu 24.04.
> **Quellen-Rangordnung:** `offiziell` = code.claude.com / docs.claude.com / anthropics/claude-code CHANGELOG ·
> `extern` = GitHub-Issues, Blogs, Community.
> **Rohberichte (lossless, mit allen Zitaten und URLs):** `claude-code-cloud-recherche-2026-09-27/r1…r7.md`
> **Gegenseite (Bugs):** `bugs/claude-tooling/claude-code-cloud.md` · **Kurzcheck:** `claude-code-cloud-kurzcheck.md`

---

## §1 Was eine Cloud-Sitzung ist

- Jede Sitzung bekommt eine **frische VM** (Ubuntu 24.04, x86_64, ca. 4 vCPU, 16 GB RAM, 30 GB Disk), in die das
  Repo von **GitHub** geklont wird. Nicht der lokale Stand: **vorher pushen**. `offiziell`
  https://code.claude.com/docs/en/cloud-environments
- Vorinstalliert: Python, Node 20/21/22, Java 21, Go, Rust, Ruby, PHP, Docker, PostgreSQL 16, Redis, **gh CLI**.
  Nicht vorinstalliert: **Android SDK**, .NET. Übersicht in der Sitzung: `check-tools`. `offiziell`
- Erkennen in Hooks/Skripten: **`CLAUDE_CODE_REMOTE=true`** (zusätzlich `CLAUDE_CODE_REMOTE_SESSION_ID`). `offiziell`
- Kein Browser, kein Emulator, kein Zugriff auf das lokale Netz, kein adb. Für GUI-/Geräte-Arbeit: lokale Sitzung
  oder **Remote Control** (lokale Sitzung vom Handy fernsteuern). `offiziell` + `extern` (#75632)
- Kosten: keine eigene VM-Abrechnung, aber **gemeinsames Nutzungslimit** mit allen anderen Claude-Code-Sitzungen. `offiziell`

## §2 Was geladen wird (und was nicht)

| Wird geladen (weil im Repo committet) | Wird NICHT geladen |
|---|---|
| `CLAUDE.md` (Wurzel + verschachtelt); fehlt sie, **`AGENTS.md`** (Fallback seit 2.1.277) | alles aus `~/.claude/*` (persönliche Skills, persönliche CLAUDE.md, Settings) |
| `.claude/settings.json` inkl. **Hooks** und Permissions | `.claude/settings.local.json` (meist gitignored) |
| `.claude/skills/`, `.claude/agents/`, `.claude/commands/` | OpenLauncher-Profile, deren Regeln und Skills |
| `.mcp.json` (Projekt-Scope-MCP-Server) | lokal (User-Scope) angelegte MCP-Server, lokale Plugins |

- **Mehrere Repos in einer Sitzung:** Dann wird aus `.claude/settings.json` **nur** `enabledPlugins` /
  `extraKnownMarketplaces` gelesen: keine Hooks, keine Permissions, kein `.mcp.json`. `offiziell`
  → Für Frank: **Cloud-Sitzungen immer mit genau einem Repo (proggs)** starten.
- **Skills als Link:** Offiziell darf `.claude/skills/<name>` ein Symlink auf einen Ordner sein. Praktisch filtert der
  Skill-Scanner Symlink-Einträge aber heraus (`Dirent.isDirectory()` ist bei Links `false`), Bug #14836, offen.
  **Folge:** Links nicht verwenden. Stattdessen einen **echten Ordner mit einer kleinen `SKILL.md`**, die auf die
  zentrale Anleitung verweist (so umgesetzt für `apk-update-cloud`, 27.09.2026).
- Projekt-Skills können in Remote-Sitzungen im `/`-Menü fehlen (#95105). Sie funktionieren aber bei Nennung des Namens.
  **Auslöser deshalb zusätzlich über einen SessionStart-Hook einblenden** (Muster `cloud-sitzung-hinweis.md`).

## §3 Umgebung einrichten: Setup-Skript vs. SessionStart-Hook

| | Setup-Skript (im Environment-Dialog auf claude.ai) | SessionStart-Hook (`.claude/settings.json`) |
|---|---|---|
| Läuft | einmal als root **vor** dem Start, Ergebnis wird als **Snapshot gecacht** (ca. 7 Tage) | **bei jedem** Sitzungsstart, lokal UND in der Cloud |
| Gut für | große Toolchains (Android SDK, apt-Pakete) | leichte Schritte: Env-Variablen, `local.properties`, Hinweise einblenden |
| Grenzen | **unter ca. 5 Minuten** bleiben, sonst kein Cache; mit Exit 0 enden (`|| true` an Unkritisches) | Timeout 600 s; immer mit `[ "$CLAUDE_CODE_REMOTE" = "true" ] || exit 0` beginnen |

- Umgebungsvariablen dauerhaft setzen: in den Hook schreiben nach **`$CLAUDE_ENV_FILE`**. `offiziell`
- **Secrets nie in die „Environment variables“** des Environments: jeder Nutzer der Umgebung kann sie lesen.
  Auf Pro/Max stattdessen **„API credentials“** (werden serverseitig vom Proxy angehängt, Claude sieht sie nie). `offiziell`

## §4 Netzwerk

- Stufen: **None / Trusted (Standard) / Full / Custom**. Trusted erlaubt u. a. `maven.google.com`,
  `repo.maven.apache.org`, `services.gradle.org`, `pypi.org`, `registry.npmjs.org`, **aber nicht `dl.google.com`**
  (Android-SDK-Downloads). Für SDK-Installation: **Custom** + `dl.google.com` ergänzen und „default list“ anhaken. `offiziell`
- Aller Verkehr läuft über einen Security-Proxy. Bun ist damit offiziell inkompatibel → npm/pnpm nehmen.
  `NODE_EXTRA_CA_CERTS` und Client-Zertifikate werden in Anthropic-Umgebungen ignoriert. `offiziell`

## §5 Git und GitHub

- GitHub-Zugriff über die Claude-GitHub-App bzw. `/web-setup`. Das echte Token bleibt in einem **Credential-Proxy
  außerhalb der VM**. Seit 2.1.282 erneuert es sich nach ca. 8 h selbst. `offiziell`
- **`git push` nur auf den eigenen Sitzungs-Branch (`claude/…`).** Direkt auf `main` geht nicht. Das ist gewollt
  und passt zum Muster „PR → Merge → CI baut“. `offiziell`
- **`gh` ist vorinstalliert und bekommt die Zugangsdaten automatisch.** Der GraphQL-Endpunkt ist nur für einen festen
  Satz PR-Operationen offen. Für alles andere **REST: `gh api repos/{owner}/{repo}/…`**. `offiziell`
- PRs erstellen (auch als Entwurf) ist dokumentiert. **Selbst mergen** ist kein beschriebenes Standardverhalten,
  geht aber über `gh pr merge` bzw. `gh api -X PUT …/pulls/{n}/merge`. Im Auto-Modus kann der Klassifizierer den
  Merge blockieren (#96257) → dann melden, nicht umgehen. `extern`
- **Bot-Kommentare** (Codex-Reviewer, Copilot) werden an Auto-Fix-Sitzungen **nicht als Ereignis zugestellt** (#62977).
  Wer auf ein Review wartet, muss die Kommentare **selbst abfragen**:
  `gh api repos/O/R/pulls/N/comments` (Zeilenkommentare), `…/pulls/N/reviews`, `…/issues/N/comments`
  (Sammelkommentar), `…/issues/N/reactions` (👍 = keine Befunde).
- Workflow-Dateien (`.github/workflows/`) pushen: Token braucht den `workflow`-Scope (`gh auth refresh -s workflow`
  bei `/web-setup`). `offiziell`
- PR-Titel/-Text: „Create PR“ nimmt den **ältesten** Commit (#93852) → Titel und Text vor dem Merge selbst setzen.

## §6 Arbeitsabläufe

- Start: Handy-App, Browser, Desktop-App oder Terminal (`claude --cloud "…"`). Folgenachricht ohne Teleport:
  `claude -p --cloud <id> "…"`. `offiziell`
- **Übergabe nur in eine Richtung:** Cloud → Terminal mit `/teleport` bzw. `claude --teleport`. Teleport hat
  offene Bugs (nur erster Turn, #94836) → Ergebnis vorher committen/pushen, dann lokal `git pull` statt Teleport.
- Aufgaben **eng abgrenzen** (Datei, Fehlermeldung, erwartetes Verhalten). Komplexes zuerst lokal im Plan-Modus
  planen, Plan committen, dann in der Cloud ausführen lassen. `offiziell`
- **Routinen** (geplante Cloud-Aufgaben): mindestens stündlich, eigenes Tageslimit; Startzeit nicht auf die volle
  Stunde legen (z. B. 9:07). Prompt muss für sich allein verständlich sein. `offiziell`
- Remote Control: lokale Sitzung (mit adb, SK-Ordner, Emulator) vom Handy aus steuern, mit Push-Benachrichtigungen.
  Das ist die Wahl, wenn etwas nur am PC geht. `offiziell`

## §7 Android in der Cloud

- **Empfohlen (so umgesetzt): Cloud programmiert, GitHub Actions baut und signiert nach dem Merge.** Genau dafür ist
  der GitHub-Proxy gedacht. Emulator- und Instrumented-Tests gehen in der Cloud ohnehin nicht. `offiziell`
- Wer trotzdem in der Cloud kompilieren will (Unit-Tests, `assembleDebug` zur Prüfung):
  1. Environment auf **Custom** mit `dl.google.com` (+ Standardliste).
  2. **Setup-Skript:** cmdline-tools + `platforms;android-XX` + `build-tools;XX` per `sdkmanager` (Lizenzen per
     Heredoc mit mehreren `y` statt `yes |`, das ist robuster), unter 5 Minuten halten.
  3. **SessionStart-Hook** (nur Cloud): `ANDROID_HOME`/`PATH` über `$CLAUDE_ENV_FILE` setzen, `local.properties` schreiben.
  4. Optional: Gradle-Aufwärmen im Hintergrund (`nohup ./gradlew help > /tmp/gradle-warmup.log &`).
- Maven-Central-DNS-Fehler kommen vor (#13372) → im Zweifel nicht in der Cloud bauen, CI bauen lassen.
- **Nie** Keystore oder Secrets in die Cloud-VM holen. Signieren nur in GitHub Actions (Environment `android-signing`,
  nur `main`).

## §8 Umsetzung in Franks Repo (Stand 27.09.2026)

| Baustein | Ort |
|---|---|
| Veröffentlichungs-Ablauf Cloud | Skill `apk-update-cloud`: zentral `OpenLauncher/Profiles/ClaudeCode/standard/skills/apk-update-cloud/SKILL.md`, in der Cloud über den echten Ordner `.claude/skills/apk-update-cloud/` (Verweis-SKILL.md, kein Link) |
| Start-Hinweis nur in der Cloud | `.claude/settings.json` SessionStart → `.claude/hooks/cloud-sitzung-hinweis.md` (Guard `CLAUDE_CODE_REMOTE`) |
| Regeln in der Cloud | Wurzel-`AGENTS.md` (wird als Fallback gelesen, solange keine Wurzel-`CLAUDE.md` existiert) |
| Bauen + Signieren | `.github/workflows/android-cloud-build.yml`, nur PR-Merges auf `main` |
| Codex-Review vor dem Merge | PR nicht als Entwurf öffnen → Codex läuft; Befunde per `gh api` abfragen (§5) |

## Wechselseitige Bezüge

| Thema | Best Practice (hier) | Bug-Almanach |
|---|---|---|
| Skill-Links | §2 | `bugs/claude-tooling/claude-code-cloud.md` §B1 |
| Push / PR / Merge | §5 | §B3–§B6 |
| Hooks / Mehr-Repo | §2, §3 | §B7 |
| Netzwerk / Android | §4, §7 | §B9–§B11 |
| Teleport / Remote Control | §6 | §B12–§B14 |
