# Recherche: Bekannte Bugs, Fallen und Workarounds von Claude-Code-Cloud-Sitzungen

Stand der Recherche: 27.09.2026. Quelle primär: GitHub Issues in `anthropics/claude-code` (per `gh search issues` / `gh issue view`), ergänzt durch Status-Seite und Web-Suche. Alle Angaben [extern] = Community-Meldung (nicht von Anthropic bestätigt), [offiziell] = aus Anthropic-Doku/Repo-Labels selbst. Da GitHub-Issues von Nutzern stammen, sind auch [offiziell]-markierte Punkte primär Nutzerberichte gegen das offizielle Repo — als "offiziell" zählt hier nur, was aus Anthropic-eigenen Quellen (Status-Seite, Docs) stammt.

---

## BUG-LISTE

### 1. Push/PR — Cloud-Sitzung kann gar nicht pushen: leeres "authorized repository set"
- **Symptom:** `git push` schlägt in jeder Cloud-/Cowork-Sitzung konsequent mit 403 fehl: `access denied by the git proxy: <owner>/<repo> is not in this session's authorized repository set`. Auch REST-Aufrufe an `api.github.com` werden vom Proxy mit 403 abgefangen. `git clone`/`git fetch` funktionieren, nur Schreiben ist blockiert.
- **Ursache:** Der sitzungsinterne Git-Credential-Proxy führt eine eigene "authorized repository set"-Liste, die unabhängig von GitHub-App-Installation, PAT-Scope oder Account-Settings ist. Der in der Fehlermeldung genannte Fix-Weg (`add_repo`-Tool) existiert in vielen Sitzungstypen gar nicht.
- **Betroffen:** Laufend, mehrfach reproduziert bis 2026-09 (z.B. mit CC 2.1.x). Ein Bericht datiert den Rollout auf ca. 10.07.2026 (Feature-Flag `CCR_TEST_GITPROXY=1`).
- **Status:** Offen (mehrere Duplikate/Varianten offen).
- **Workaround:** Keiner zuverlässig gefunden. In manchen Sitzungstypen hilft ein `add_repo`-Tool (falls vorhanden) mit `access:"push"`; wo es fehlt, keine Selbsthilfe möglich außer neue Sitzung/Session-Typ probieren oder Repo explizit als "Source" beim Sitzungsstart hinzufügen (nicht nachträglich).
- **URL:** [#92188](https://github.com/anthropics/claude-code/issues/92188), [#76248](https://github.com/anthropics/claude-code/issues/76248), [#96075](https://github.com/anthropics/claude-code/issues/96075), [#77482](https://github.com/anthropics/claude-code/issues/77482), verwandt: [#57009](https://github.com/anthropics/claude-code/issues/57009) (GitHub App nur OAuth-autorisiert, nicht "installiert" → nur Lesezugriff)

### 2. Push/PR — Handoff-Sitzung kann pushen, aber keine PR öffnen
- **Symptom:** Eine per "Claude Design → Claude Code"-Handoff erzeugte Cloud-Sitzung kann Branch pushen, hat aber keine GitHub-MCP-Tools (`create_pull_request` fehlt komplett) — die letzte Aktion (PR öffnen) muss manuell in einer normalen claude.ai/code-Sitzung nachgeholt werden.
- **Ursache:** Handoff-Sitzungstyp bekommt kein GitHub-MCP-Toolset, nur rohen Git-Zugriff.
- **Betroffen:** claude.ai/code, Handoff-Sitzungen (Tag `omelette-handoff`).
- **Status:** Offen.
- **Workaround:** Nach Push manuell eine zweite, normale claude.ai/code-Sitzung öffnen und dort PR erstellen lassen.
- **URL:** [#90238](https://github.com/anthropics/claude-code/issues/90238)

### 3. PR-Inhalt — "Create PR" nimmt Titel/Body vom ÄLTESTEN statt neuesten Commit
- **Symptom:** Bei mehreren Commits auf dem Branch übernimmt die "Create PR"-Funktion Titel und Beschreibung vom ältesten Commit, nicht vom neuesten oder einer Zusammenfassung des ganzen Branches. Der Diff-Kopf (SHA) ist korrekt, nur Titel/Body sind irreführend unvollständig. Zusätzlich landen `Co-Authored-By:`/`Claude-Session:`-Trailer aus der Commit-Message fälschlich im PR-Body.
- **Ursache:** Unbekannt (aktive Auswahl des ältesten statt neuesten Commits, kein GitHub-Standardverhalten).
- **Betroffen:** Claude Code on the Web (Remote-Sitzung).
- **Status:** Offen.
- **Workaround:** PR-Titel/Body nach dem Erstellen manuell prüfen und überschreiben, bevor gemergt wird — besonders bei Mehr-Commit-Branches.
- **URL:** [#93852](https://github.com/anthropics/claude-code/issues/93852)

### 4. Stop-Hook — Phantom "unpushed commits" nach squash-gemergtem PR (Merge-Loop-Falle)
- **Symptom:** Der eingebaute `stop-hook-git-check.sh` vergleicht `HEAD` gegen `origin/<branch>`. Wenn GitHub nach einem Squash-Merge den Feature-Branch server-seitig automatisch löscht, aber der lokale `origin/<branch>`-Tracking-Ref noch existiert (nicht geprunt), meldet der Hook bei JEDEM Stop fälschlich "1 unpushed commit" — mit Exit-Code 2, der die Sitzung blockiert. Der vorgeschlagene "Push"-Weg funktioniert nicht, weil der Branch remote nicht mehr existiert.
- **Ursache:** Bug in `stop-hook-git-check.sh` (falsche Ref-Wahl: `git rev-parse origin/$branch` statt Prüfung, ob der Branch remote noch existiert).
- **Betroffen:** Alle Sitzungen (lokal und Cloud) mit dem eingebauten Stop-Hook, besonders relevant für den Ablauf "PR erstellen → selbst mergen", da genau danach der Hook feuert.
- **Status:** Offen. Verwandtes Duplikat: [#95524](https://github.com/anthropics/claude-code/issues/95524) (unpushed-commit-Check versagt bei Branches ohne Remote-Ref, false-positive nach gemergtem PR). Auch [#89684](https://github.com/anthropics/claude-code/issues/89684) (Stop-Hook rät zum Rewrite veröffentlichter History bei shallow clone + signierter GitHub-Merge-Commit "Unverified") und [#83490](https://github.com/anthropics/claude-code/issues/83490) ähnlich.
- **Workaround:** `git fetch --prune` bzw. `git remote prune origin` ausführen, um veraltete Tracking-Refs zu entfernen, bevor der Stop-Hook läuft; oder den lokalen Branch nach dem Merge explizit auf `origin/main` zurücksetzen (`git checkout -B <branch> origin/main`).
- **URL:** [#92338](https://github.com/anthropics/claude-code/issues/92338)

### 5. Hooks — `.claude/settings.json` des Repos wird in Web-Sitzungen bei ZWEI angehängten Repos ignoriert
- **Symptom:** In einer Claude-Code-on-the-Web-Sitzung mit zwei angehängten Repos ist das Projekt-Root der GEMEINSAME Elternordner statt des jeweiligen Checkouts. Dadurch wird `<repo>/.claude/settings.json` nie geladen, `SessionStart`-Hooks feuern nie, `$CLAUDE_PROJECT_DIR` ist leer — ohne jede Warnung. `CLAUDE.md` wird dagegen korrekt geladen, was die Fehlersuche erschwert (sieht aus wie ein normal laufender, aber wirkungsloser Hook).
- **Ursache:** Projekt-Root-Auflösung bei Mehrfach-Repo-Sitzungen fehlerhaft.
- **Betroffen:** Claude Code on the Web, Sitzungen mit mehr als einem angehängten Repository.
- **Status:** Offen.
- **Workaround:** Nur ein Repo pro Cloud-Sitzung anhängen, wenn Repo-Hooks (z.B. für Dependency-Installation) kritisch sind; sonst Setup-Schritte in einen expliziten Setup-Script statt SessionStart-Hook auslagern.
- **URL:** [#89215](https://github.com/anthropics/claude-code/issues/89215)

### 6. Plugins — `extraKnownMarketplaces`/`enabledPlugins` aus Repo-`.claude/settings.json` werden in Cloud-Sandboxen nie verarbeitet
- **Symptom:** Ein Team-Plugin, das per `.claude/settings.json` (`extraKnownMarketplaces` + `enabledPlugins`) im Repo deklariert ist, wird in Cloud-Sitzungen NICHT registriert — `known_marketplaces.json` bleibt leer, Plugin-Skills tauchen nie auf, `/myplugin:...`-Commands sind unbekannt. Hooks aus derselben Datei funktionieren dagegen einwandfrei — nur der Marketplace/Plugin-Teil ist betroffen.
- **Ursache:** Unbekannt, vermutlich fehlender Consent-Gate-Mechanismus für Marketplace-Registrierung in unattended Cloud-Sandboxes.
- **Betroffen:** claude.ai/code Cloud-Sessions, CC 2.1.211 bestätigt.
- **Status:** Offen.
- **Workaround:** Kein zuverlässiger gefunden; nur manuelle CLI-Registrierung (`claude plugin marketplace add`) funktioniert, ist aber pro Sitzung nicht praktikabel in unattended Cloud-Runs.
- **URL:** [#78119](https://github.com/anthropics/claude-code/issues/78119)

### 7. Skills — Projekt-Skills aus `.claude/skills/` fehlen im `/`-Menü einer Remote-Sitzung (nur Autocomplete betroffen)
- **Symptom:** Im Desktop-App-`/`-Menü einer Remote-Cloud-Sitzung tauchen Projekt-Skills aus `.claude/skills/` nicht auf (nur Built-in/Account/Plugin-Skills), obwohl dieselben Skill-Verzeichnisse im Checkout vorhanden und gültig sind. `/reload-skills` zeigt in der Remote-Sitzung deutlich weniger Skills (87) als in einer lokalen Sitzung desselben Repos (164).
- **Ursache:** Client-seitiges Autocomplete lädt Projekt-Skills bei Remote-Sitzungen nicht mit.
- **Betroffen:** Claude Code Desktop App, Code-Tab, Remote-Sitzungen.
- **Status:** Offen.
- **Workaround:** Skill-Namen vollständig ausschreiben (`/verify-cockpit`) oder in normaler Sprache anfordern ("nutze den verify-cockpit-Skill") — der Skill selbst funktioniert, nur die Autovervollständigung nicht.
- **URL:** [#95105](https://github.com/anthropics/claude-code/issues/95105)

### 8. Skills — SKILL.md-Dateien im System-Prompt angekündigt, aber nicht im Container gemountet
- **Symptom:** Nutzer-/Organisations-Skills erscheinen korrekt im `<available_skills>`-Block des System-Prompts, aber `/mnt/skills/user/` existiert im Container gar nicht bzw. einzelne Organisations-Skills fehlen auf der Platte. Claude versucht, die SKILL.md zu lesen, und scheitert lautlos.
- **Ursache:** Server-seitiger Mount-Fehler, spezifisch für User-/Organisations-Skills (Public/Example-Skills sind stets korrekt gemountet).
- **Betroffen:** claude.ai Web-Chat, Claude Code und Cowork, gemeldet Feb. 2026 — noch offen im Sept. 2026 gemäß verwandten Skill-Ladebugs.
- **Status:** Offen.
- **Workaround:** Keiner bekannt außer Skill neu hochladen/erneut aktivieren und hoffen, dass der Mount beim nächsten Sitzungsstart klappt.
- **URL:** [#26254](https://github.com/anthropics/claude-code/issues/26254)

### 9. Skills/Subagents — vorhandene Skills/Subagents werden vom Modell ignoriert, Arbeit läuft teuer inline
- **Symptom:** Obwohl Projekt-Instruktionen (CLAUDE.md/AGENTS.md) explizit eine Router-Tabelle mit Skill-Zuordnung und definierten Subagenten enthalten, arbeitet Claude die Aufgaben inline im Hauptkontext ab, statt den passenden Skill zu laden oder den Subagenten zu delegieren — mit höheren Kosten UND schlechterer Qualität (wiederholte Fehler, die der Skill dokumentiert hätte).
- **Ursache:** Unbekannt, Modellverhalten (Label "MODEL").
- **Betroffen:** allgemein, nicht cloud-spezifisch, aber besonders relevant für Cloud-Sessions ohne Interaktionsmöglichkeit zum Nachsteuern.
- **Status:** Offen.
- **Workaround:** Explizit und wiederholt zur Nutzung des Skills/Subagenten auffordern; hilft laut Melder nur kurzfristig für die nächste Aktion.
- **URL:** [#90182](https://github.com/anthropics/claude-code/issues/90182)

### 10. PreToolUse-Hook-Deadlock ohne Recovery in Cowork-Cloud-Sitzungen
- **Symptom:** Ein `PreToolUse`-Hook, der pauschal alle Tools blockiert (inkl. `Task`/`Agent` für Subagenten), sperrt die gesamte Sitzung permanent — auch die Werkzeuge, die zum Entfernen des blockierenden Zustands nötig wären. Ein neuer Chat verwendet denselben Container weiter, sodass der Zustand überlebt. Nur ein komplett frischer Sandbox-Neustart hilft.
- **Ursache:** Fehlende Eskalations-/Recovery-Route, wenn ein Hook alle Tool-Aufrufe (inkl. Subagenten-Dispatch) blockiert; kein Dateisystemzugriff außerhalb der gesperrten Tools.
- **Betroffen:** Cowork Cloud-Sandbox.
- **Status:** Offen.
- **Workaround:** Neue, komplett frische Sitzung/Environment starten (nicht nur neuer Chat im selben Container); vorbeugend: PreToolUse-Hooks nie pauschal (Catch-all-Regex) auf alle Tools inkl. `Task|Agent` matchen lassen.
- **URL:** [#85581](https://github.com/anthropics/claude-code/issues/85581)

### 11. Governed Git: verschachtelter `claude`-Unterprozess kapert die Git/gh-Proxy-Config der Session
- **Symptom:** Wenn ein Tool innerhalb der Sitzung selbst einen `claude`-Prozess startet (z.B. Vercel CLI ruft beim Env-Pull `claude plugins list --json` auf), startet dieser Kindprozess einen eigenen Agent-Proxy-Relay, überschreibt Git-Config und `gh`-Shim mit seinem eigenen (kurzlebigen) Port und beendet sich dann selbst. Ab diesem Moment zeigen `git`/`gh` auf einen toten Port — jeder weitere `git push`/`fetch`/`gh`-Aufruf schlägt mit "connection refused" fehl, bis die Sitzung endet.
- **Ursache:** Fehlende Wiederverwendung/Schutz der Parent-Session-Proxy-Konfiguration gegen Kindprozess-Überschreibung.
- **Betroffen:** Self-hosted-Runner mit `--use-anthropic-git-proxy`, CC 2.1.281, bestätigt bei Vercel-CLI-Interaktion; potenziell jedes Tool, das intern `claude` aufruft.
- **Status:** Offen.
- **Workaround:** Verhindern, dass Drittwerkzeuge in der Sitzung `claude` aufrufen (z.B. bei Vercel CLI `AI_AGENT` auf einen Nicht-Claude-Wert setzen oder `pluginDeclined: true` in dessen Preferences-Datei setzen).
- **URL:** [#96856](https://github.com/anthropics/claude-code/issues/96856)

### 12. Teleport — nur der erste Turn kommt an, spätere Nachrichten/Branch fehlen
- **Symptom:** `claude --teleport` einer Web-Sitzung holt nur den ersten Assistenten-Turn; ein zweiter User-Prompt samt Antwort (auf claude.ai sichtbar) kommt lokal nicht an. Zusätzlich schlägt der Branch-Checkout fehl ("Session resumed without branch"), ohne dass der Nutzer erkennt, dass der Transkript unvollständig ist.
- **Ursache:** Unklar — Timing (Snapshot vor dem zweiten Turn) oder echter Cutoff.
- **Betroffen:** Claude Code 2.1.273, native Linux-Installation, Web-Sitzung → lokaler Teleport.
- **Status:** Offen. Verwandt: [#74277](https://github.com/anthropics/claude-code/issues/74277) (closed als inaktiv, nicht gefixt — "teleport restored no history"), [#92734](https://github.com/anthropics/claude-code/issues/92734) (Teleport web→local droppt Prompt-History, Memory referenziert falschen Projekt-Key).
- **Workaround:** Vor dem Teleport in der Web-UI die Seite neu laden und prüfen, ob wirklich alle Turns angezeigt werden; nach dem Teleport das lokale Transkript gegen claude.ai manuell abgleichen, bevor man weiterarbeitet.
- **URL:** [#94836](https://github.com/anthropics/claude-code/issues/94836)

### 13. Teleport — Remote-Control-Sitzungen liefern seit 15.09.2026 0 Events (Regression)
- **Symptom:** Zwischen 26.08. und 11.09.2026 konnte eine auf Rechner A gestartete Remote-Control-Sitzung auf Rechner B per Teleport mit vollem Verlauf geöffnet werden. Seit 15.09.2026 liefert Teleport 0 Events — die Aktion meldet "Erfolg", öffnet aber eine leere neue Konversation. Nicht an eine Client-Version gebunden (auch neue Sitzungen mit alten CLI-Versionen betroffen), server-seitiger Store ist schlicht leer für neue Sitzungen.
- **Ursache:** Server-seitige Regression, Zeitpunkt eingegrenzt auf 12.–15.09.2026.
- **Betroffen:** VS-Code-Extension Remote-Control-Sitzungen, CC 2.1.266–2.1.278, Windows 11.
- **Status:** Offen (server-seitig, per Client-Update nicht behebbar).
- **Workaround:** Keiner — wichtige Zwischenstände sollten im Zweifel direkt in der Web-Oberfläche gesichert/exportiert werden, nicht nur über Teleport verlassen.
- **URL:** [#95873](https://github.com/anthropics/claude-code/issues/95873), verwandt [#93892](https://github.com/anthropics/claude-code/issues/93892), älterer Vorfall [#79417](https://github.com/anthropics/claude-code/issues/79417) (closed, "0 log entries" im Juli 2026)

### 14. Cross-Device-Sitzung führt Aktionen still auf dem URSPRUNGS-Rechner aus (Sicherheitsrisiko)
- **Symptom:** Öffnet man auf Rechner B eine Sitzung, die eigentlich auf Rechner A läuft, sieht die UI wie eine normale lokale Sitzung aus — es gibt keine Kennzeichnung. Tatsächlich laufen alle Datei-/Shell-Operationen weiterhin auf Rechner A. Ein Nutzer, der auf Rechner B "hier" löschen/ändern lässt, trifft in Wahrheit Rechner A.
- **Ursache:** UI zeigt keinen Hinweis auf abweichenden Ausführungsort.
- **Betroffen:** Claude Desktop App, Cross-Device-Session-Continuation (gleicher Account, verschiedene physische Maschinen).
- **Status:** Offen. Ein Kommentar meldet den "umgekehrten" Fall ebenfalls als Bug.
- **Workaround:** Vor Aktionen in einer wiedereröffneten Sitzung immer den Working-Directory-Pfad/Hostnamen explizit per `pwd`/`hostname` abfragen, statt der UI blind zu vertrauen.
- **URL:** [#92144](https://github.com/anthropics/claude-code/issues/92144)

### 15. Remote-Control-Daemon-Neustart lässt Sitzungen ohne `--resume` neu starten → Kontextverlust ("Amnesie")
- **Symptom:** Startet der Remote-Control-Daemon (systemd-Service) neu (Crash, Update, Reboot), verliert er die Zuordnung zu laufenden Session-Host-Prozessen. Öffnet man danach eine bestehende Konversation aus der App, startet der Daemon einen NEUEN Host-Prozess OHNE `--resume` — die App zeigt die volle History (kommt aus der API), aber das Modell selbst ist "leer" und antwortet, als sei der Chat neu.
- **Ursache:** Fehlender `--resume`/`--fork-session`-Parameter beim Neu-Spawnen verwaister Sessions.
- **Betroffen:** `claude remote-control` als always-on Daemon (z.B. systemd), genutzt via Mobile App/claude.ai/code, CC 2.1.220.
- **Status:** Offen. Ein funktionierender Community-Fix (Wrapper-Binary, die `--resume <uuid> --fork-session` injiziert) wurde beschrieben, greift aber laut Folgekommentar NICHT bei Daemon-initiierten Spawns (argv[0] zeigt auf den echten Binary-Pfad, kein LD_PRELOAD möglich bei statisch gelinktem Binary).
- **Workaround:** Kein vollständiger — der beschriebene Wrapper-Ansatz deckt nur manuell gestartete Prozesse ab, nicht Daemon-Auto-Respawns. Praktisch: Daemon möglichst nicht neu starten/updaten während offener Sitzungen; wichtige Zwischenergebnisse vor bekannten Neustart-Fenstern committen.
- **URL:** [#84468](https://github.com/anthropics/claude-code/issues/84468)

### 16. Netzwerk/Proxy — Java/Maven-Builds scheitern an DNS-Fehler für Maven Central
- **Symptom:** `./mvnw`/Gradle-Builds in Claude Code Web scheitern mit "Temporary failure in name resolution" für `repo.maven.apache.org`, obwohl die Doku "JVM: Maven Central, Gradle services" ausdrücklich als erlaubte Domain listet.
- **Ursache:** DNS-Auflösung im Cloud-Sandbox-Egress-Proxy fehlerhaft trotz dokumentierter Allowlist.
- **Betroffen:** Claude Code Web Cloud-Sandbox, Java/Maven/Gradle-Projekte.
- **Status:** Offen (Issue seit Dez. 2025, im Sept. 2026 laut Suche weiter offen — Stand-Verfall möglich, im Zweifel nachprüfen).
- **Workaround:** Keiner im Issue dokumentiert außer Build außerhalb der Cloud-Sandbox laufen zu lassen (z.B. GitHub Actions statt In-Session-Build).
- **URL:** [#13372](https://github.com/anthropics/claude-code/issues/13372)

### 17. Netzwerk/Proxy — TLS/Zertifikatsprobleme in der Cloud-Sandbox
- **Symptom (a):** Cloud-Sandbox-Proxy setzt Chromiums TLS-1.3-Handshake zurück (WebKit nicht betroffen). **(b)** Der `agent-proxy` signiert mit einer Staging-CA ohne X.509v3-Extensions, was JEDEN Python-3.13+-Client bricht (strengere Zertifikatsprüfung dort).
- **Ursache:** Fehlkonfiguration der TLS-Interception im Cloud-Egress-Proxy.
- **Betroffen:** Cloud-Sandboxen, browserbasierte bzw. Python-3.13+-Workloads.
- **Status:** Offen.
- **Workaround:** Bei Python 3.13+: Zertifikatsprüfung nicht global deaktivieren, sondern gezielt die Sandbox-CA ins Trust-Store einspielen falls möglich, oder Python-Version <3.13 im Sandbox-Setup-Script fixieren.
- **URL:** [#87378](https://github.com/anthropics/claude-code/issues/87378), [#90521](https://github.com/anthropics/claude-code/issues/90521)

### 18. Netzwerk/Proxy — Egress-Allowlist "Alle Domains" wird ignoriert / kollabiert
- **Symptom:** Trotz Account-Einstellung "All domains" blockiert der Cloud-Egress-Proxy einzelne Domains mit 403 (z.B. `api.telegram.org`), oder die Allowlist kollabiert auf ca. 5 Hosts unabhängig von der Einstellung.
- **Ursache:** Server-seitiger Bug in der Allowlist-Durchsetzung, mehrfach reproduziert auf Windows und macOS.
- **Betroffen:** Cowork/Cloud-Sandbox, Sept. 2026.
- **Status:** Ein Duplikat (#93525) wurde geschlossen, ein weiteres (#93677) ist weiterhin offen — Fix-Status uneinheitlich, ggf. nur teilweise behoben.
- **Workaround:** Betroffene Domain testweise einzeln zur Allowlist hinzufügen statt "All domains" zu verwenden; bei Fehlschlag neue Sitzung starten (manchmal hilft laut Berichten ein Session-Neustart kurzfristig).
- **URL:** [#93561](https://github.com/anthropics/claude-code/issues/93561), [#93677](https://github.com/anthropics/claude-code/issues/93677), geschlossen: [#93525](https://github.com/anthropics/claude-code/issues/93525)

### 19. Sitzung stirbt sofort/mit Error auf jede Nachricht ("execution error")
- **Symptom:** Cloud-Sitzungen und Routinen scheitern zu 100% mit "An error occurred while executing Claude Code" bei JEDER Nachricht, sogar bei "hi" — noch bevor irgendeine User-Aktion verarbeitet wird. CLI und claude.ai-Chat funktionieren auf demselben Account einwandfrei.
- **Ursache:** Server-seitiger Fehler in der Cloud-Ausführungsschicht (mehrere Duplikate über Monate hinweg, unterschiedliche Root Causes vermutlich).
- **Betroffen:** claude.ai/code Cloud-Execution-Layer, wiederkehrend seit mind. Mitte 2026.
- **Status:** Issue als Duplikat von #54528 geschlossen — Kern-Symptom aber laut Duplikat-Häufung wiederkehrend, nicht dauerhaft gefixt.
- **Workaround:** Neue Sitzung starten, abwarten (oft transientes Server-Problem); falls dauerhaft: Status-Seite prüfen (siehe unten), Feedback-ID im Issue mitschicken.
- **URL:** [#72602](https://github.com/anthropics/claude-code/issues/72602) (Duplikat von #54528)

### 20. Stop-Hook feuert auch bei lokalen (nicht-Cloud) Sitzungen, aber besonders tückisch nach Cloud→Merge-Workflow — Hooks stoppen nach ~2,5h
- **Symptom (separater Bug):** Hooks laufen bei Sitzungsstart normal, stoppen aber nach ca. 2,5 Stunden lautlos, ohne Fehler oder Warnung — keine weiteren Logeinträge.
- **Ursache:** Unbekannt (Session-Lifecycle-Bug, [extern] gemeldet für Windows/Git Bash, nicht cloud-spezifisch bestätigt, aber relevant für lange laufende Cloud-Sitzungen/Routinen).
- **Betroffen:** Windows 11, Git Bash, Stand Anfang 2026 — Aktualität für Sept. 2026 ungeprüft.
- **Status:** [extern] gemeldet, Fix-Status unklar (kein GitHub-Issue-Link direkt bestätigt, nur über Web-Suche gefunden — mit Vorsicht behandeln).
- **Workaround:** Lange Sitzungen (>2h) periodisch neu starten/kompaktieren, wenn Hook-basierte Automatisierung kritisch ist.
- **URL:** [Bug: Hooks stop executing after ~2.5 hours in session](https://github.com/anthropics/claude-code/issues/16047) [extern, ungeprüft ob noch aktuell]

---

## BEST-PRACTICES-KANDIDATEN:

1. **Vor dem Verlassen einer Cloud-Sitzung immer selbst pushen und den Push verifizieren** (nicht nur auf "Erfolg" der UI verlassen) — Push-Fehler sind der mit Abstand größte Bug-Cluster (Bugs 1, 2, 11, 17-19) und sind teils lautlos/inkonsistent.
2. **Nach jedem Squash-Merge eines eigenen PRs sofort `git fetch --prune` bzw. den lokalen Branch auf `origin/main` zurücksetzen**, bevor der Stop-Hook läuft — verhindert die Phantom-"unpushed commits"-Falle (Bug 4), die exakt beim Ablauf "PR erstellen → selbst mergen" zuschlägt.
3. **Vor dem Merge PR-Titel/Body von Claude-generierten PRs immer manuell gegenlesen**, besonders bei Mehr-Commit-Branches — die automatische PR-Beschreibung kann vom falschen (ältesten) Commit stammen und wichtige Änderungen unterschlagen (Bug 3).
4. **Bei Cloud-Sitzungen mit Repo-Hooks/Setup-Skripten nur EIN Repo pro Sitzung anhängen**, sonst werden `.claude/settings.json`-Hooks und `$CLAUDE_PROJECT_DIR` stillschweigend übersprungen (Bug 5).
5. **Keine Drittwerkzeuge (z.B. Vercel CLI) in einer governed-git-Cloud-Sitzung `claude` selbst aufrufen lassen** — das kapert den Session-Git-Proxy-Port und legt Git/gh lahm bis Sitzungsende (Bug 11).
6. **Wichtige Zwischenstände in langlaufenden/Daemon-basierten Remote-Control-Sitzungen frühzeitig committen**, da Daemon-Neustarts und Teleport-Regressionen Kontext/Verlauf verlustig verursachen können, ohne dass die UI das anzeigt (Bugs 12, 13, 15).

## OFFEN/UNSICHER:

- Für die meisten hier gelisteten Bugs (v.a. 1, 2, 4-11, 16-19) fehlt eine offizielle Anthropic-Bestätigung oder ein dokumentierter Fix in einer bestimmten Version — alle stammen aus offenen (unbeantworteten oder unbestätigten) GitHub-Issues. Die Angst-Priorisierung basiert auf Plausibilität/Reproduktionsqualität der Melder, nicht auf offiziellem Anthropic-Statement.
- Bug 16 (Maven-DNS) ist als Issue seit Dezember 2025 offen — möglich, dass er in der Zwischenzeit gefixt wurde und das Issue nur nicht geschlossen ist. Vor Verlass auf diese Info im aktuellen Stand (Sept. 2026) selbst nachtesten.
- Bug 18 (Egress-Allowlist) zeigt ein geschlossenes UND ein offenes Duplikat gleichzeitig — Fix-Status inkonsistent, könnte teilweise/für bestimmte Plattformen behoben sein.
- Bug 20 (Hooks stoppen nach 2,5h) wurde nur über allgemeine Websuche gefunden (kein direktes GitHub-Issue mit Nummer im Repo `anthropics/claude-code` verifiziert im Rahmen dieser Recherche) — Herkunft/Aktualität unsicher, als Hinweis, nicht als belastbaren Fakt behandeln.
- Die Anthropic-Statusseite (status.claude.com) zeigt für den Recherchezeitraum keine offiziell bestätigten Vorfälle speziell zu Claude Code Web/Cloud/GitHub-Integration — die hier gelisteten Probleme sind also (noch) nicht als System-Incidents anerkannt, sondern laufen ausschließlich über Community-Issues.
- Reddit r/ClaudeAI und Hacker News wurden nur über allgemeine Websuche gestreift, nicht mit gezielter Subreddit-/HN-Suche durchsucht — für eine tiefere Reddit/HN-Abdeckung wäre eine eigene Suchrunde nötig.
