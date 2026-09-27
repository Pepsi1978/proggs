# Researcher 3 — Git und GitHub in Cloud-Sitzungen (Claude Code on the web)

Stand der Recherche: 27.09.2026, lokale CLI-Version 2.1.283.

## Befunde

### GitHub-Zugriff: zwei Verbindungswege
Cloud-Sitzungen brauchen Zugriff auf GitHub-Repos zum Klonen und Pushen. Zwei Wege stehen offen:

| Methode | Wie verbunden | Erreichbare Repos |
|---|---|---|
| **Claude GitHub App** | Autorisierung im Web-Onboarding | Alle öffentlichen Repos + private Repos, auf denen die App installiert ist |
| **`/web-setup`** | Sendet den lokalen `gh`-CLI-Token ans Claude-Konto | Alle Repos, die der `gh`-Token erreicht — App-Installation nicht nötig |

Die Installation der Claude GitHub App auf einem Repo aktiviert zusätzlich Auto-Fix. Threads in einem "Project" brauchen die App installiert, egal welcher Verbindungsweg genutzt wurde.
Quelle: https://code.claude.com/docs/en/claude-code-on-the-web [offiziell]; https://code.claude.com/docs/en/web-quickstart [offiziell]

### Credential-Schutz: Der GitHub-Proxy
In Anthropic-gehosteten Umgebungen bleiben die echten GitHub-Credentials verschlüsselt auf Anthropics Servern und erreichen die Sitzungs-VM nie. Alle GitHub-Operationen laufen über einen dedizierten Proxy, unabhängig vom Netzwerk-Zugriffslevel der Umgebung. Der Proxy leistet:
- **Git-Credentials**: Der Git-Client in der VM nutzt ein scoped Credential, das der Proxy prüft und gegen den echten GitHub-Token tauscht.
- **API-Requests**: Requests der eingebauten GitHub-Tools und von `gh` (über den Platzhalter `proxy-injected`) gehen mit den echten Credentials raus.
- **Push-Schutz**: „`git push` funktioniert nur gegen den aktuellen Arbeits-Branch der Sitzung; Klonen, Fetchen und PR-Operationen funktionieren normal." (wörtliches Zitat)
- **Repository-Scope**: GitHub-API- und Release-Asset-Requests erreichen nur an die Sitzung angehängte Repos — ein Setup-Skript, das Release-Assets aus einem nicht angehängten Repo lädt, bekommt 403.
- **GraphQL-Restriktionen**: Der Proxy bedient nur eine festgelegte Menge an GraphQL-Operationen für PR-Workflows. Alles andere wird mit 403 „This GraphQL query is not enabled for this session" abgelehnt, mit Verweis auf den REST-Fallback `gh api repos/{owner}/{repo}/...`. Gilt für JEDEN Request über den Proxy, unabhängig von mitgelieferten Credentials — auch ein selbst gesetzter `GH_TOKEN` bekommt denselben 403. Claude kann daher GitHub-APIs, die nur als GraphQL existieren (z. B. Projects v2), über den Proxy nicht erreichen.

Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt „GitHub proxy") [offiziell]

### Branch-Beschränkung: Warum nur `claude/...`-Branches (bei Routinen)
Für Routinen (`/schedule`, geplante/getriggerte Cloud-Sitzungen) explizit dokumentiert: „Claude pushes its work to branches prefixed with `claude/`, which are always accepted." Wenn der Prompt Claude anweist, auf einen anderen Branch zu pushen, prüft Claude Code den Push zuerst und lehnt ihn ab, wenn:
- der Branch auf GitHub geschützt (protected) ist,
- jemand anderes einen offenen PR von diesem Branch hat,
- der Branch Commits trägt, die von jemand anderem als dir selbst autorisiert wurden.

Für interaktive Cloud-Sitzungen (nicht-Routine) ist die dokumentierte Regel einfacher: „git push funktioniert nur gegen den aktuellen Arbeits-Branch der Sitzung" — Claude kann also grundsätzlich nur auf den eigenen Session-Branch pushen, nicht direkt auf beliebige fremde Branches.
Quelle: https://code.claude.com/docs/en/routines (Abschnitt „Repositories and branch permissions") [offiziell]; https://code.claude.com/docs/en/cloud-environments [offiziell]

**Unsicher/abweichend**: Mehrere externe Quellen (Blogs, keine Anthropic-Docs) sprechen von einem UI-Schalter „Allow unrestricted branch pushes" pro Repo/Routine, mit dem man den `claude/`-Zwang aufheben könne. In den offiziell gecrawlten Docs (cloud-environments, routines, claude-code-on-the-web) taucht dieser Schalter-Name so NICHT auf — die Doku beschreibt stattdessen die oben genannte Regelprüfung (protected/offener PR/fremde Commits) ohne benannten Schalter. Möglich, dass der Schalter existiert, aber (noch) nicht in den öffentlichen Docs beschrieben ist, oder umbenannt/entfernt wurde. Mehrere GitHub-Issues bestätigen aber, dass ein solcher Schalter in der Praxis existiert bzw. existierte (siehe Bug-Kandidaten).
Quellen [extern]: https://israynotarray.com/en/ai/2026/05/25/claude-code-routines-schedule-and-branch-push-fix/ ; https://www.verdent.ai/guides/what-is-claude-code-routines ; GitHub-Issues #44949, #58141 (s.u.)

### Kann eine Sitzung PRs selbst erstellen UND mergen?
**Erstellen: ja, dokumentiert.** Im Diff-View gibt es „Create PR" mit drei Optionen: als vollständigen PR, als Draft, oder Sprung zu GitHubs Compose-Seite mit vorausgefülltem Titel/Beschreibung.
Quelle: https://code.claude.com/docs/en/web-quickstart (Abschnitt „Review and iterate") [offiziell]

**Mergen: nicht in der offiziellen Doku als Standard-Fähigkeit beschrieben.** Es gibt einen dokumentierten „Auto-merge"-Mechanismus (aus der CI-Statusleiste aktivierbar, siehe GitHub-Issues zu Auto-merge/GHE-Hardcoding), aber die offizielle Doku, die ich fand, beschreibt PR-**Erstellung** und Auto-Fix ausführlich, nicht aber einen eigenständigen „merge PR"-Schritt als Standardteil des Workflows. Es existiert aber ein GitHub-Issue (#96257, offen, 23.09.2026), das zeigt, dass PR-Merges via GitHub-REST-API (`PUT .../pulls/:number/merge`) in der Praxis technisch möglich sind/waren — bis ein Update des „Auto mode classifier" (Auto-Permission-Modus) genau das blockierte. Das deutet darauf hin, dass Mergen grundsätzlich technisch machbar ist (über `gh`/API), aber im Auto-Modus von einem Sicherheits-Klassifizierer geprüft/blockiert werden kann.
Zusätzlicher Kontext (nicht offiziell, projekt-intern): Im eigenen Repo dieses Nutzers gibt es bereits einen produktiven Workflow „apk-update-cloud", der laut Skill-Beschreibung eine Cloud-Sitzung PRs öffnen, auf Codex-Review warten, Befunde fixen und **selbst mergen** lässt — das belegt, dass Self-Merge in der Praxis funktioniert (zumindest mit `gh pr merge` als Tool-Aufruf), ist aber projektspezifische Konfiguration, keine offizielle Anthropic-Dokumentation dieses Verhaltens als Default.
Quellen: https://github.com/anthropics/claude-code/issues/96257 [extern/GitHub-Issue]; https://github.com/anthropics/claude-code/issues/96003, #96002, #95997 (Auto-merge hardcoded auf github.com, nicht GHE-fähig) [extern]

### Verfügbare GitHub-Werkzeuge in der Cloud
„Cloud sessions include built-in GitHub tools that let Claude read issues, list pull requests, fetch diffs, and post comments without any setup." Diese Tools authentifizieren über den GitHub-Proxy.
Zusätzlich: `GH_TOKEN`/`GITHUB_TOKEN` sind entweder selbst gesetzt (dann normale Env-Var, für alle Nutzer der Umgebung lesbar) oder beide lesen als Platzhalter `proxy-injected`, während der Proxy die echten Credentials serverseitig einsetzt. `gh` funktioniert dann ohne eigenen Token (`gh auth login` nicht nötig).
Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt „Work with GitHub issues and pull requests") [offiziell]

Der geprüfte Auszug nennt keine konkrete Liste einzelner MCP-Funktionsnamen wie `create_pull_request`, `merge_pull_request`, `pull_request_read` als Bestandteil der Cloud-Session-Tools — das ist eher Terminologie der **separaten** GitHub-MCP-Integration für GitHub Actions (`claude-code-action`) bzw. der **Managed-Agents-API** (platform.claude.com), die einen eigenen GitHub-MCP-Server (`https://api.githubcopilot.com/mcp/`) anbindet. Das ist ein anderes Produkt als „Claude Code on the web"/claude.ai/code-Cloud-Sitzungen — dort mountet man Repos per API mit `authorization_token` und verbindet den offiziellen GitHub-Copilot-MCP-Server, nicht den Cloud-Session-GitHub-Proxy.
**Wichtig, Verwechslungsgefahr**: platform.claude.com/docs/en/managed-agents/github beschreibt NICHT claude.ai/code, sondern die separate "Managed Agents"-API (Sessions via REST-API, eigene Repos mounten). Für das eigentliche Unterthema (claude.ai/code Cloud-Sitzungen) sind die „built-in GitHub tools" (Issues lesen, PRs listen, Diffs holen, Kommentare posten) plus `gh` maßgeblich, nicht die MCP-Tool-Namen aus der Managed-Agents-Doku.
Quelle: https://code.claude.com/docs/en/cloud-environments [offiziell]; https://platform.claude.com/docs/en/managed-agents/github [offiziell, aber anderes Produkt]

### GitHub-Actions-Läufe/Job-Logs lesen
Die CI-Statusleiste in der Sitzung zeigt Check-Status; „Auto-fix" reagiert auf CI-Fehler. Details zum Lesen einzelner Job-Logs wurden in den geprüften Seiten nicht explizit mit Tool-Namen dokumentiert (nur implizit über „Claude investigates" bei CI-Fehlern im Auto-Fix-Abschnitt). `gh` ist vorinstalliert, wodurch `gh run view`/`gh run view --log` grundsätzlich nutzbar sein sollte, sofern der Proxy die zugehörigen REST-Endpunkte durchlässt (die GraphQL-Restriktion gilt nur für den GraphQL-Endpunkt).
Quelle: https://code.claude.com/docs/en/cloud-environments [offiziell] — Rest als Ableitung markiert, nicht wörtlich bestätigt.

### `gh` CLI: funktioniert
„GitHub's `gh` CLI is pre-installed. If you need a `gh` command the built-in tools don't cover, like `gh release` or `gh workflow run`, ask Claude to run it. `gh` reads `GH_TOKEN` automatically, so you don't need to run `gh auth login`." Auch in der generellen Toolliste der VM aufgeführt (`git, gh, jq, yq, ripgrep, tmux, vim, nano`).
Quelle: https://code.claude.com/docs/en/cloud-environments [offiziell]

### PR-Kommentare/Reviews von Bots lesen (z. B. Codex-Reviewer) — mit bekanntem Bug
Auto-Fix beschreibt: „Claude subscribes to GitHub activity on the PR, and when a check fails or a reviewer leaves a comment, Claude investigates and pushes a fix if one is clear." Das umfasst laut Doku grundsätzlich auch Reviewer-Kommentare allgemein, nicht nur menschliche.
**ABER**: GitHub-Issue #62977 (geschlossen als Duplikat, Stand 27.05.2026) dokumentiert einen konkreten Bug: Autofix-Sitzungen erhalten `<github-webhook-activity>`-Events für **menschliche** Kommentare und CI-Checks zuverlässig, aber Bot-verfasste Reviews/Kommentare (GitHub Copilot, Codex, Vercel, Supabase) werden **stillschweigend gefiltert** — vermutlich weil ein Anti-Feedback-Loop-Filter (soll verhindern, dass die Sitzung auf ihre eigenen Bot-Kommentare reagiert) zu breit scoped ist und alle Bot/App-Autoren trifft, nicht nur die eigene Session-Identität. Siehe Bug-Kandidaten unten.
Quelle: https://code.claude.com/docs/en/claude-code-on-the-web (Abschnitt „Auto-fix pull requests") [offiziell]; https://github.com/anthropics/claude-code/issues/62977 [extern]

### „PR-Aktivität abonnieren" / Auto-Fix bei CI-Fehlern und Review-Kommentaren
Vollständig dokumentiertes Feature „Auto-fix pull requests":
- Aktivierbar: (1) aus einer Cloud-Sitzung heraus über die CI-Statusleiste → „Auto-fix"; (2) vom Terminal mit `/autofix-pr` auf dem PR-Branch (Claude Code erkennt den offenen PR über `gh`, startet eine Cloud-Sitzung und aktiviert Auto-Fix in einem Schritt); (3) aus der mobilen App per Anweisung; (4) für jeden bestehenden PR durch Einfügen der PR-URL.
- Erfordert die installierte Claude GitHub App.
- Reaktionslogik: „Clear fixes" → Claude macht die Änderung, pusht und erklärt es; „Ambiguous requests" → Claude fragt vorher nach; „Duplicate or no-action events" → Claude notiert es und macht weiter.
- **Einschränkung**: „GitHub does not emit a webhook when the base branch advances and creates a merge conflict, so auto-fix can't react to conflicts on its own." — bei Merge-Konflikten muss man selbst um Rebase bitten.
- Claude kann auf Review-Kommentar-Threads auf GitHub antworten (unter dem Nutzer-GitHub-Account, aber als „Claude Code" markiert). **Warnung der Doku**: Bei kommentar-getriggerter Automation (Atlantis, Terraform Cloud, custom Actions auf `issue_comment`) kann Claudes automatische Antwort diese Workflows auslösen — Doku rät, vor Aktivierung von Auto-Fix die Repo-Automation zu prüfen und Auto-Fix ggf. für Repos mit privilegierten Comment-Trigger-Aktionen zu deaktivieren.
- Abschalten: Auto-Fix-Toggle in der CI-Statusleiste leeren, oder Claude bitten, das Beobachten zu beenden.
Quelle: https://code.claude.com/docs/en/claude-code-on-the-web (Abschnitt „Auto-fix pull requests") [offiziell]

### GitHub-Trigger für Routinen (verwandt, aber kein Auto-Fix)
Routinen können zusätzlich auf GitHub-Events selbst reagieren: unterstützte Event-Kategorien sind „Pull request" (opened, closed, assigned, labeled, synchronized, ...) und „Release" (created, published, edited, deleted). Filterbar u. a. nach Autor, Titel, Body, Base-/Head-Branch, Labels, „Is draft", „Is merged". Erfordert ebenfalls die installierte Claude GitHub App; `/web-setup` allein installiert die App NICHT und aktiviert keine Webhook-Zustellung.
Quelle: https://code.claude.com/docs/en/routines (Abschnitt „Add a GitHub trigger") [offiziell]

### Draft-PR-Verhalten
Beim „Create PR"-Schritt im Diff-View kann man explizit „als Draft" öffnen — eine von drei Optionen (voller PR / Draft / GitHub-Compose-Seite). Für Routinen-GitHub-Trigger gibt es zusätzlich den Filter „Is draft" (true/false), z. B. um Draft-PRs von einer Review-Routine auszuschließen.
Quelle: https://code.claude.com/docs/en/web-quickstart [offiziell]; https://code.claude.com/docs/en/routines [offiziell]

### Mehrere Repos pro Sitzung
Interaktive Sitzungen: „You can add multiple repositories to work across them in one session." (Schritt „Select a repository and branch" im Quickstart). Für Sitzungen mit mehreren Repos gilt aber eine Einschränkung: Repo-eigene `.claude/settings.json` (Hooks/Permissions) und `.mcp.json` werden NUR in Sitzungen mit **genau einem** Repository gelesen — bei mehreren Repos (inkl. Project-Threads) starten Sitzungen „above the clones" ohne diese Dateien zu lesen. Ebenso: SessionStart-Hooks aus `.claude/settings.json` laufen bei mehreren Repos nicht (stattdessen Setup-Skript der Umgebung nutzen).
Routinen: „Add one or more GitHub repositories for Claude to work in."
Quelle: https://code.claude.com/docs/en/web-quickstart [offiziell]; https://code.claude.com/docs/en/cloud-environments (Tabelle „What carries over from your setup" + Abschnitt „Limitations in cloud sessions") [offiziell]; https://code.claude.com/docs/en/routines [offiziell]

### GitLab/andere Hosts
„**Platform restrictions**: repository cloning and pull request creation require GitHub. Self-hosted [GitHub Enterprise Server](/docs/en/github-enterprise-server) instances are supported for Team and Enterprise plans. You can send a GitLab, Bitbucket, or other non-GitHub repository to a cloud session as a local bundle by setting `CCR_FORCE_BUNDLE=1`, but the session can't push results back to that remote." (wörtliches Zitat aus dem Abschnitt „Limitations")
Kurz: GitLab/Bitbucket lassen sich als lokales Bundle **hochladen** (voller Git-Verlauf, alle Branches, plus uncommittete Änderungen an getrackten Dateien), aber die Sitzung kann NICHT zu diesem Remote zurückpushen. Nur GitHub (inkl. selbstgehostetem GitHub Enterprise Server für Team/Enterprise) unterstützt den vollen Zyklus inkl. PR-Erstellung.
Quelle: https://code.claude.com/docs/en/claude-code-on-the-web (Abschnitt „Limitations", „Send local repositories without GitHub") [offiziell]

### Sicherheitsrelevante Details zum Bundle-Upload (Nebenbefund)
Beim Bundle-Upload (kein GitHub-Remote oder App nicht installiert) lässt Claude Code auf macOS/Linux/WSL uncommittete Änderungen an Dateien wie `.env`, Terraform `*.tfvars` und Schlüsseldateien (`id_rsa`, `*.pem`) automatisch aus dem Upload heraus und nennt die ausgelassenen Dateien. Relevant, falls man `--cloud` mal ohne GitHub-Anbindung aus einem lokalen Repo heraus nutzt.
Quelle: https://code.claude.com/docs/en/claude-code-on-the-web [offiziell]

## BEST-PRACTICES-KANDIDATEN:
- Für Repos mit CI-Actions-Workflow-Dateien (`.github/workflows/`): Vor dem ersten Cloud-Push aus `/web-setup` prüfen, ob der `gh`-Token den `workflow`-Scope hat (`gh auth refresh -s workflow`), sonst können Pushes, die Workflow-Dateien ändern, von GitHub abgelehnt werden. Offiziell dokumentierter Troubleshooting-Hinweis.
- Bevor Auto-Fix auf einem Repo aktiviert wird, das kommentar-getriggerte Automation nutzt (Atlantis, Terraform Cloud, eigene `issue_comment`-Actions): prüfen, ob Claudes automatische Antworten auf Review-Threads (die unter dem eigenen GitHub-Account gepostet werden) versehentlich privilegierte Workflows auslösen können.
- Bei Sitzungen mit mehreren Repos (Projects) NICHT auf reposeigene `.claude/settings.json`-Hooks oder `.mcp.json` verlassen — diese werden nur bei genau einem Repo gelesen. MCP-Server für Multi-Repo-Sitzungen stattdessen über claude.ai-Connectors einbinden.
- Für Merge-Konflikte nach Auto-Fix-Aktivierung: Kein automatisches Reagieren möglich (GitHub sendet dafür keinen Webhook) — aktiv „rebase" anfordern, wenn der Base-Branch weitergewandert ist.
- Der GraphQL-Endpunkt des GitHub-Proxys ist nur für einen festen Satz PR-Operationen freigegeben; für alles andere (z. B. GitHub Projects v2) den REST-Fallback `gh api repos/{owner}/{repo}/...` nutzen, da GraphQL-Queries mit 403 abgelehnt werden — auch mit selbst gesetztem `GH_TOKEN`.

## BUG-KANDIDATEN:

1. **Symptom**: Auto-Fix-Sitzungen in Claude Code on the web empfangen `<github-webhook-activity>`-Events für menschliche PR-Kommentare und CI-Checks zuverlässig, aber Bot-verfasste Reviews/Kommentare (GitHub Copilot, Codex-Reviewer, Vercel, Supabase) werden stillschweigend gefiltert und lösen keine Auto-Fix-Reaktion aus.
   **Ursache (vermutet)**: Ein zu breit gefasster Anti-Feedback-Loop-Filter nach Sender-Typ (soll verhindern, dass eine Sitzung auf ihre eigenen Bot-Kommentare reagiert) blockt pauschal alle Bot/App-Autoren statt nur die eigene Session-Identität.
   **Version**: Claude Code on the web, Regression seit ca. Mitte Mai 2026, Issue-Stand 27.05.2026.
   **Workaround**: Keiner dokumentiert im Issue.
   **Status**: Geschlossen als Duplikat (verweist auf #52474 und Feature-Request #50555).
   **URL**: https://github.com/anthropics/claude-code/issues/62977
   **Relevanz für dieses Repo**: Direkt relevant für den Skill `apk-update-cloud`, der explizit auf Codex-Review-Kommentare wartet, bevor er selbst mergt — falls Codex als Bot-Autor gilt, könnten dessen Kommentare der Auto-Fix-Sitzung möglicherweise nicht zugestellt werden. Sollte im Zweifel gegen die tatsächliche Praxis in diesem Repo verifiziert werden.

2. **Symptom**: PR-Merges via GitHub-REST-API (`PUT /repos/:owner/:repo/pulls/:number/merge`) werden mit Fehler `[Auto-Mode Bypass]` vom „Claude Code auto mode classifier" blockiert, obwohl der Session-Modus verifiziert `auto` ist und dieselbe Operation vor einem Update funktionierte. Breitet sich nach einigen Stunden auch auf PR-Erstellung aus.
   **Ursache (vermutet)**: Absichtliche Policy-Änderung oder Bug im Update des internen „engineering"-Plugins auf Version 1.2.0.
   **Version**: Claude Code 2.1.275, Engineering-Plugin 1.2.0, Plattform Anthropic API/macOS.
   **Workaround**: Nicht dokumentiert; der erwartete Workaround (`plugin:engineering:github`-Connector) funktioniert selbst nicht (zeigt `needs_auth`, Sign-in schlägt fehl).
   **Status**: Offen, Issue erst am 23.09.2026 eröffnet, keine Reaktion.
   **URL**: https://github.com/anthropics/claude-code/issues/96257
   **Einordnung**: Betrifft primär den „Auto"-Permission-Modus (Klassifizierer statt Rückfrage) und ein internes Plugin — nicht zwingend repräsentativ für reguläre Cloud-Sitzungen ohne dieses Plugin, aber ein Indiz, dass Merge-Aktionen vom Auto-Modus-Klassifizierer zusätzlich geprüft/blockiert werden können.

3. **Symptom**: Auto-Merge-Funktion (aktivierbar aus der CI-Statusleiste) ist fest auf `github.com` verdrahtet und funktioniert nicht für GitHub Enterprise Cloud mit Datenresidenz (`*.ghe.com`).
   **Ursache**: Hardcoded Host-Annahme statt host-aware Implementierung.
   **Version**: Nicht präzise angegeben in den Suchtreffern (mehrere verwandte Issues #96003, #96002, #95997).
   **Workaround**: Keiner erwähnt in den Titeln/Snippets.
   **Status**: Unbekannt (nicht im Detail geöffnet, nur Titel/Snippet ausgewertet) — nur als Hinweis, nicht vollständig verifiziert.
   **URL**: https://github.com/anthropics/claude-code/issues/96003, https://github.com/anthropics/claude-code/issues/96002, https://github.com/anthropics/claude-code/issues/95997
   **Hinweis**: Nicht im Volltext gelesen, nur aus Suchergebnis-Snippets — vor Nutzung als Bug-Eintrag ggf. gegenlesen.

## OFFEN/UNSICHER:

- **„Allow unrestricted branch pushes"-Schalter**: Mehrfach in externen Blogs/Issues erwähnt (auch als vermeintlich buggy, z. B. #58141 „Routine push to main returns HTTP 403 from git proxy despite Allow unrestricted branch pushes being enabled" und #44949 „Remote Scheduled task pushed to main even though Allow unrestricted branch pushes was not turned on"), aber in den offiziell gecrawlten Docs-Seiten (cloud-environments, routines, claude-code-on-the-web) nicht unter diesem Namen dokumentiert gefunden. Unklar, ob der Schalter aktuell existiert, wo er in der UI zu finden ist, oder ob er zwischenzeitlich umbenannt/entfernt wurde. Die Issues selbst deuten auf Inkonsistenzen zwischen dem Schalter-Zustand und dem tatsächlichen Proxy-Verhalten hin (403 trotz aktiviertem Schalter; ungewollter Push zu main trotz deaktiviertem Schalter) — falls der Schalter tatsächlich existiert, wäre das ein ernstzunehmender Sicherheits-Bug, aber ich konnte es mit den offiziellen Quellen nicht verifizieren. Empfehlung: vor produktivem Verlass auf diese Restriktion selbst in der UI nachsehen (Routine-Editor → Permissions-Bereich laut einer externen Quelle) und ggf. GitHub-Branch-Protection als zusätzliche, unabhängige Absicherung nutzen (wird auch von einer externen Quelle empfohlen).
- **Genaue MCP-Tool-Namen für Cloud-Sitzungen** (`create_pull_request`, `merge_pull_request`, `pull_request_read` etc.): Die offizielle claude.ai/code-Dokumentation beschreibt die Fähigkeiten verbal („read issues, list pull requests, fetch diffs, and post comments"), nennt aber keine konkreten Tool-/Funktionsnamen für die Cloud-Session-eigenen GitHub-Tools. Die Namen `create_pull_request`/`merge_pull_request` tauchen nur im Kontext der GitHub-Actions-Integration (`claude-code-action`, separates GitHub-MCP-Server-Ökosystem `api.githubcopilot.com/mcp/`) und der Managed-Agents-API auf — nicht bestätigt als identische Tool-Namen innerhalb von claude.ai/code-Cloud-Sitzungen selbst. Nicht abschließend geklärt, ob/wie Claude Code on the web intern MCP-Tools mit diesen Namen nutzt oder eigene, undokumentierte Tool-Namen hat.
- **Ob eine Sitzung einen PR eigenständig (ohne explizite Nutzeranweisung „merge it") proaktiv mergt**: Nicht dokumentiert gefunden. Alle gefundenen Hinweise auf Merge (Auto-Merge-Button, `gh pr merge`, API-Merge) implizieren eine explizite Aktivierung/Anweisung, kein automatisches Selbst-Mergen als Standardverhalten am Ende einer Aufgabe.
- **Wie genau GitHub-Actions-Job-Logs gelesen werden** (spezifischer Tool-Name oder nur über `gh run view --log` durch die Session ausgeführt): Nicht mit wörtlichem Zitat belegt, nur plausibel abgeleitet aus der Vorinstallation von `gh` und der allgemeinen CI-Status-Integration.
