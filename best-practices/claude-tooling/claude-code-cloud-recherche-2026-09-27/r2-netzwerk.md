# Researcher 2: Netzwerk und Sicherheit — Claude Code on the web

Stand der Recherche: 27.09.2026, bezogen auf CLI-Version 2.1.283 / aktuelle Cloud-Doku.

## Befunde

### 1. Vier Netzwerkzugriffs-Stufen pro Cloud-Environment
Jede Cloud-Umgebung (environment) legt genau eine Netzwerkzugriffs-Stufe fest, die die ausgehenden Verbindungen ihrer Sitzungen steuert:

| Stufe | Ausgehende Verbindungen |
| --- | --- |
| **None** (kein Zugriff) | Kein ausgehender Netzwerkzugriff über das Netzwerk der Sitzung |
| **Trusted** (vertrauenswürdig, Standard) | Nur [Allowlist-Domains](#3-vollständige-liste-der-standardmäßig-erlaubten-domains-trusted): Paket-Registries, GitHub, Cloud-SDKs |
| **Full** (voll) | Jede Domain |
| **Custom** (eigene Allowlist) | Eigene Domainliste, optional zusätzlich zu den Standard-Domains |

Quelle: [offiziell] https://code.claude.com/docs/en/cloud-environments#access-levels

Auch bei „None" erreichen Sitzungen laut Doku wörtlich weiterhin vier Dinge, weil diese NICHT über die Allowlist der Sitzung laufen:
- GitHub, über den separaten GitHub-Proxy
- Aktivierte MCP-Connectors (Traffic läuft über Anthropic-Server)
- Die Hosts, die in den environment-eigenen API-Credentials gelistet sind (außer den Hosts, die nie das Credential bekommen)
- Die Anthropic-API selbst, für Claude Codes eigene Requests — **auch bei „None"**

Zitat: „When running with network access disabled, Claude Code can still communicate with the Anthropic API, which may allow data to exit the VM." [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web#security-and-isolation

Beim Standard-Onboarding wird automatisch eine „Default"-Umgebung mit **Trusted**-Zugriff angelegt. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web#cloud-environments

### 2. Eigene Allowlist (Custom) einrichten
Unter „Custom" wird eine eigene Domainliste (eine Domain pro Zeile, führendes `*.` erlaubt Subdomains) eingetragen. Checkbox „Also include default list of common package managers" fügt optional die Trusted-Liste hinzu. Es gibt **keine organisationsweite Allowlist**, die Admins zentral auf alle Member-Environments pushen könnten — jede Umgebung hat ihre eigene Liste; für ein Team-Standard braucht es ein „organization-shared environment" mit Custom-Zugriff. [offiziell] https://code.claude.com/docs/en/cloud-environments#allow-specific-domains

### 3. Vollständige Liste der standardmäßig erlaubten Domains (Trusted)
Wörtlich aus der offiziellen Doku übernommen, gruppiert wie dort dargestellt (`*` = Wildcard-Subdomain-Matching):

- **Anthropic-Dienste:** api.anthropic.com, docs.claude.com, platform.claude.com, code.claude.com, claude.ai
- **Versionskontrolle:** github.com, www.github.com, api.github.com, npm.pkg.github.com, raw.githubusercontent.com, pkg-npm.githubusercontent.com, objects.githubusercontent.com, release-assets.githubusercontent.com, codeload.github.com, avatars.githubusercontent.com, camo.githubusercontent.com, gist.github.com, gitlab.com, www.gitlab.com, registry.gitlab.com, bitbucket.org, www.bitbucket.org, api.bitbucket.org
- **Container-Registries:** registry-1.docker.io, auth.docker.io, index.docker.io, hub.docker.com, www.docker.com, production.cloudflare.docker.com, download.docker.com, gcr.io, *.gcr.io, ghcr.io, mcr.microsoft.com, *.data.mcr.microsoft.com, public.ecr.aws
- **Cloud-Plattformen:** cloud.google.com, accounts.google.com, gcloud.google.com, *.googleapis.com, storage.googleapis.com, compute.googleapis.com, container.googleapis.com, azure.com, portal.azure.com, microsoft.com, www.microsoft.com, *.microsoftonline.com, packages.microsoft.com, dotnet.microsoft.com, dot.net, visualstudio.com, dev.azure.com, *.amazonaws.com, *.api.aws, oracle.com, www.oracle.com, java.com, www.java.com, java.net, www.java.net, download.oracle.com, yum.oracle.com, *.r2.cloudflarestorage.com
- **JavaScript/Node-Paketmanager:** registry.npmjs.org, www.npmjs.com, www.npmjs.org, npmjs.com, npmjs.org, yarnpkg.com, registry.yarnpkg.com, jsr.io, npm.jsr.io
- **Python-Paketmanager:** pypi.org, www.pypi.org, files.pythonhosted.org, pythonhosted.org, test.pypi.org, pypi.python.org, pypa.io, www.pypa.io
- **Ruby-Paketmanager:** rubygems.org, www.rubygems.org, api.rubygems.org, index.rubygems.org, ruby-lang.org, www.ruby-lang.org, rubyforge.org, www.rubyforge.org, rubyonrails.org, www.rubyonrails.org, rvm.io, get.rvm.io
- **Rust-Paketmanager:** crates.io, www.crates.io, index.crates.io, static.crates.io, rustup.rs, static.rust-lang.org, www.rust-lang.org
- **Go-Paketmanager:** proxy.golang.org, sum.golang.org, index.golang.org, golang.org, www.golang.org, goproxy.io, pkg.go.dev
- **JVM-Paketmanager:** maven.org, repo.maven.org, central.maven.org, repo1.maven.org, **repo.maven.apache.org**, **maven.google.com**, jcenter.bintray.com, gradle.org, www.gradle.org, **services.gradle.org**, plugins.gradle.org, plugins-artifacts.gradle.org, kotlinlang.org, www.kotlinlang.org, spring.io, repo.spring.io
- **Weitere Paketmanager:** packagist.org/www.packagist.org/repo.packagist.org (PHP Composer), nuget.org/www.nuget.org/api.nuget.org (.NET NuGet), pub.dev/api.pub.dev (Dart/Flutter), hex.pm/www.hex.pm (Elixir/Erlang), cpan.org/www.cpan.org/metacpan.org/www.metacpan.org/api.metacpan.org (Perl CPAN), cocoapods.org/www.cocoapods.org/cdn.cocoapods.org (iOS/macOS), haskell.org/www.haskell.org/hackage.haskell.org, swift.org/www.swift.org
- **Linux-Distributionen:** archive.ubuntu.com, security.ubuntu.com, ubuntu.com, www.ubuntu.com, *.ubuntu.com, ppa.launchpad.net, launchpad.net, www.launchpad.net, *.nixos.org
- **Entwicklungstools/Plattformen:** dl.k8s.io, pkgs.k8s.io, k8s.io, www.k8s.io, releases.hashicorp.com, apt.releases.hashicorp.com, rpm.releases.hashicorp.com, archive.releases.hashicorp.com, hashicorp.com, www.hashicorp.com, repo.anaconda.com, conda.anaconda.org, anaconda.org, www.anaconda.com, anaconda.com, continuum.io, apache.org, www.apache.org, archive.apache.org, downloads.apache.org, eclipse.org, www.eclipse.org, download.eclipse.org, nodejs.org, www.nodejs.org, developer.apple.com, developer.android.com, pkg.stainless.com, binaries.prisma.sh
- **Cloud-Dienste/Monitoring:** http-intake.logs.datadoghq.com, *.datadoghq.com, *.datadoghq.eu, api.honeycomb.io
- **CDN/Mirrors:** sourceforge.net, *.sourceforge.net, packagecloud.io, *.packagecloud.io, fonts.googleapis.com, fonts.gstatic.com
- **Schema/Konfiguration:** json-schema.org, www.json-schema.org, json.schemastore.org, www.schemastore.org
- **MCP:** *.modelcontextprotocol.io

Quelle: [offiziell] https://code.claude.com/docs/en/cloud-environments#default-allowed-domains

**Wichtiger Befund zur Nutzerfrage:** `repo.maven.apache.org`, `maven.google.com` und `services.gradle.org` sind in der Trusted-Liste enthalten. **`dl.google.com` ist NICHT in der Liste enthalten** — für Android/Gradle-Builds, die `dl.google.com` direkt ansprechen (z. B. manche Legacy-Repository-Deklarationen), müsste die Domain explizit über „Custom" ergänzt werden. `pypi.org` und `registry.npmjs.org` sind Teil der Trusted-Liste.

### 4. Traffic, der IMMER die Allowlist umgeht (unabhängig von der gewählten Stufe)
- GitHub, über den [GitHub-Proxy](#5-git-zugangsdaten--credential-proxy)
- MCP-Connector-Traffic (läuft über Anthropic-Server)
- Hosts der environment-eigenen API-Credentials (außer Ausnahmen, s. u.)
- Anthropic-API selbst (auch bei „None")

Quelle: [offiziell] https://code.claude.com/docs/en/cloud-environments#access-levels

### 5. Git-Zugangsdaten / Credential-Proxy
In Anthropic-gehosteten Umgebungen bleiben GitHub-Zugangsdaten **verschlüsselt auf Anthropics Servern und gelangen nie in die Sitzungs-VM**. Alle GitHub-Operationen aus der VM laufen über einen dedizierten GitHub-Proxy:
- „Git credentials: the git client inside the VM uses a scoped credential, which the proxy verifies and swaps for your actual GitHub token."
- „API requests: requests from the built-in GitHub tools, and from `gh` under the proxy-injected placeholder, go out with your real credentials substituted."
- „Push protection: `git push` works only against the session's current working branch; cloning, fetching, and PR operations work normally."
- „Repository scope: GitHub API and release-asset requests reach only repositories attached to the session" — ein Setup-Skript, das Release-Assets eines nicht angehängten Repos lädt, bekommt einen 403.
- „GraphQL restrictions: the proxy serves only a pinned set of GraphQL operations for pull-request workflows." Alles andere wird mit 403 „This GraphQL query is not enabled for this session" abgelehnt (z. B. GitHub Projects v2 nicht erreichbar), **auch wenn man selbst ein `GH_TOKEN` setzt**.

`GH_TOKEN`/`GITHUB_TOKEN`: setzt man selbst keinen, liest der Container den Platzhalter-String `proxy-injected`; der Proxy ersetzt ihn serverseitig bei ausgehenden GitHub-Requests. Ein selbst gesetztes Token wird dagegen unverändert durchgereicht (und ist dann als normale Umgebungsvariable für jeden lesbar, der die Umgebung nutzt).

Quelle: [offiziell] https://code.claude.com/docs/en/cloud-environments#github-proxy und https://code.claude.com/docs/en/cloud-environments#work-with-github-issues-and-pull-requests

Für selbst gehostete Umgebungen (self-hosted environments) liefert die eigene Infrastruktur die Git-Zugangsdaten; optional kann man sich für den „Anthropic git proxy" entscheiden, der dann von Anthropics Seite aus holt. [offiziell] https://code.claude.com/docs/en/network-config#github-allow-lists-and-firewalls

### 6. API-Credentials (Secrets-Handling für Drittanbieter-APIs) — nur Pro/Max
Auf Pro- und Max-Plänen lassen sich „API credentials" (Bearer-Token o. Ä.) pro Environment hinterlegen. Der „agent proxy" hängt den Schlüssel serverseitig an Requests für gelistete Hosts an, **nachdem** der Request die VM verlassen hat — „The key never reaches Claude, the commands it runs, or the session's environment variables." Diese Credentials sind auf Team/Enterprise-Plänen (noch) nicht verfügbar.

Ausnahmen, die NIE ein solches Credential bekommen:
- GitHub (läuft über den GitHub-Proxy)
- `api.anthropic.com`, `registry.npmjs.org`, `jsr.io`, `npm.jsr.io`, `pypi.org`, `files.pythonhosted.org`, `index.crates.io`, `proxy.golang.org`
- Requests des Setup-Skripts (das läuft, bevor Claude Code sich mit dem Agent-Proxy verbindet)

Quelle: [offiziell] https://code.claude.com/docs/en/cloud-environments#add-api-credentials und #requests-that-never-get-the-credential

### 7. Umgang mit Secrets/Environment-Variablen (klare Warnung von Anthropic)
Environment-Variablen (`.env`-Format) werden bei Sitzungsstart einmalig in gewöhnliche Umgebungsvariablen kopiert. Zitat: „Anyone who uses the environment can read the values." Die Doku warnt ausdrücklich: „don't add secrets or credentials" — stattdessen API-Credentials (s. o.) nutzen. [offiziell] https://code.claude.com/docs/en/cloud-environments#set-environment-variables

Beim `--cloud`-Bundle-Upload lokaler Repos ohne GitHub-Remote: Auf macOS/Linux/WSL werden unversionierte Änderungen an Dateien, die wie Credentials/Keys aussehen (`.env`-Dateien, Terraform `*.tfvars`, `id_rsa`, `*.pem`), automatisch **vom Upload ausgeschlossen** und die ausgeschlossenen Dateien benannt. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web#send-local-repositories-without-github

### 8. Security-Proxy / Sandbox-Isolation der VM
Jede Sitzung läuft in einer isolierten, Anthropic-verwalteten VM (Ubuntu 24.04, x86_64). Aller ausgehender Internetverkehr aus einer Anthropic-gehosteten Sitzung läuft durch einen HTTP/HTTPS-Sicherheitsproxy, der laut Doku bietet:
- Schutz vor bösartigen Requests
- Rate-Limiting und Missbrauchsprävention
- Content-Filtering
- „A DNS-level audit trail of requested hostnames"

In selbst gehosteten Umgebungen (self-hosted environments) verlässt der Traffic stattdessen die eigene Netzwerkgrenze; Isolation ist dort Verantwortung des eigenen Deployments.

Quelle: [offiziell] https://code.claude.com/docs/en/cloud-environments#security-proxy und https://code.claude.com/docs/en/claude-code-on-the-web#security-and-isolation

### 9. Proxy-Fallen: Tools, die den Proxy nicht sauber respektieren
Offiziell dokumentiert: **Bun** hat „known proxy compatibility issues" beim Paket-Fetching im Sicherheitsproxy der Cloud-Sitzungen — Bun ist zwar vorinstalliert, funktioniert aber nicht zuverlässig mit dem Proxy. Zitat: „some package managers don't work correctly with it; Bun is a known example." [offiziell] https://code.claude.com/docs/en/cloud-environments#installed-tools und #limitations-in-cloud-sessions

### 10. TLS/Zertifikate in Cloud-Sitzungen — wichtiger Unterschied zur lokalen CLI
In Cloud-Sitzungen verwaltet die Hosting-Umgebung die Verbindung zur API. Deshalb **ignoriert Claude Code dort folgende Variablen**, selbst wenn sie in einem Settings-`env`-Block gesetzt sind:
- `CLAUDE_CODE_CLIENT_CERT`, `CLAUDE_CODE_CLIENT_KEY`, `CLAUDE_CODE_CLIENT_KEY_PASSPHRASE` (mTLS)
- `NODE_EXTRA_CA_CERTS`, `NODE_TLS_REJECT_UNAUTHORIZED`
- `CLAUDE_CODE_OAUTH_SCOPES`

Jede ignorierte Variable wird im Debug-Log der Sitzung vermerkt. Diese Regel gilt für Cloud-Sitzungen „wherever you start them" (Browser, Terminal `--cloud`, Mobile, Desktop-Cloud-Modus). [offiziell] https://code.claude.com/docs/en/network-config#mtls-authentication

Ebenfalls ignoriert/nicht übernommen aus dem Repo-`.claude/settings.json` `env`-Block: Transport-Variablen wie `NODE_EXTRA_CA_CERTS` und die mTLS-Client-Zertifikat-Variablen — „The hosting environment manages the session's API connection, so Claude Code ignores these keys and notes each ignored key in the session's debug log." [offiziell] https://code.claude.com/docs/en/cloud-environments (Tabelle „What carries over from your setup")

Folge: Eigene TLS-Interception-Proxys/Custom-CAs lassen sich in Anthropic-gehosteten Cloud-Sitzungen **nicht** über diese Variablen konfigurieren — das ist nur für lokale/self-hosted Sitzungen relevant.

### 11. Setup-Skripte und Netzwerk
Setup-Skripte (laufen vor Sitzungsstart als root auf Ubuntu 24.04) brauchen bei „None"-Netzwerkzugriff keinen Zugang zu Registries → Installationen schlagen dann fehl. Bei „Trusted" sind npm, PyPI, RubyGems, crates.io etc. abgedeckt. Skripte müssen mit Exit-Code 0 enden und innerhalb von ca. 5 Minuten fertig sein, sonst wird das Environment-Caching übersprungen. [offiziell] https://code.claude.com/docs/en/cloud-environments#script-requirements

### 12. IP-Allowlisting der Organisation kollidiert mit Cloud-Sessions
Cloud-Sitzungen rufen die Anthropic-API von Anthropic-verwalteter Infrastruktur auf, nicht aus dem eigenen Firmennetz. Ist beim Kunden Organisations-IP-Allowlisting aktiv, **scheitert jede Anthropic-gehostete Cloud-Sitzung mit einem Authentifizierungsfehler** (ebenso Code Review und Routines auf Anthropic-Infrastruktur). Workaround: Anthropic-Support kontaktieren, um Anthropic-gehostete Dienste von der Firmen-IP-Allowlist auszunehmen, oder auf self-hosted environments/Routing ausweichen. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web#limitations

### 13. GitHub-Proxy und IP-Allowlists bei GitHub Enterprise
Für GitHub Enterprise Cloud mit IP-Restriktionen: „IP allow list inheritance for installed GitHub Apps" aktivieren UND Anthropics ausgehende IP-Adressen zusätzlich in die Allowlist eintragen (Inheritance deckt nur App-Installation-Requests, nicht die im Namen der Nutzer gemachten Requests ab). Für self-hosted GitHub Enterprise Server (GHES) hinter Firewall: Anthropics ausgehende IPs allowlisten, damit Anthropic-Infrastruktur den GHES-Host zum Klonen/Kommentieren erreichen kann — außer bei self-hosted environments, dort kommt der Traffic aus dem eigenen Netz. [offiziell] https://code.claude.com/docs/en/network-config#github-allow-lists-and-firewalls

### 14. Sandbox/Isolation — lokaler Bash-Sandbox-Mechanismus (Kontext, nicht cloud-spezifisch, aber relevant für „Netzwerk und Sicherheit")
Die separate „Sandboxed Bash tool"-Funktion (lokale CLI, macOS/Linux/WSL2) arbeitet mit einem Proxy-Server außerhalb der Sandbox:
- Standardmäßig sind **keine Domains vorab erlaubt**; bei Bedarf fragt Claude Code nach, oder im Auto-Modus benennt Claude die benötigten Hosts direkt am Kommando.
- „The built-in proxy enforces the allowlist based on the requested hostname and, by default, does not terminate or inspect TLS traffic." — d. h. kein TLS-Interception standardmäßig; optional per `network.tlsTerminate` aktivierbar (u. a. Voraussetzung für Credential-Masking).
- Für Firmenproxys: `HTTPS_PROXY`/`HTTP_PROXY`/`NO_PROXY` im `env`-Block der Settings setzen; Claude Code erzwingt zuerst die eigene Domain-Allowlist und tunnelt erlaubte Verbindungen dann durch den vorgelagerten Firmenproxy.
- Credential-Masking-Feature: Umgebungsvariablen/Dateien mit Secrets können per `mode: mask` durch einen Platzhalter ersetzt werden, den der Sandbox-Proxy erst beim Verlassen der Sandbox gegen den echten Wert austauscht (nur für erlaubte `injectHosts`).

Quelle: [offiziell] https://code.claude.com/docs/en/sandboxing#network-isolation — dies ist die **lokale** CLI-Sandbox, nicht die Cloud-Session-Netzwerkarchitektur; beide Mechanismen sind getrennt zu betrachten (Cloud-Sessions nutzen den in Befund 8 beschriebenen Security-Proxy).

### 15. Netzwerk-Anforderungen generell (Basis-Domains für den Betrieb der CLI selbst)
Für den Betrieb von Claude Code (auch außerhalb von Cloud-Sessions) müssen u. a. folgende Hosts erreichbar sein: `api.anthropic.com`, `claude.ai`, `claude.com`, `platform.claude.com`, `mcp-proxy.anthropic.com`, `downloads.claude.ai`, `storage.googleapis.com`, `registry.npmjs.org`, `bridge.claudeusercontent.com`, `*.frame.claudeusercontent.com`, `github.com`, `raw.githubusercontent.com`, Datadog-Telemetrie-Hosts, `formulae.brew.sh`, `code.claude.com`. Vollständige Tabelle mit Zweck je Host: [offiziell] https://code.claude.com/docs/en/network-config#network-access-requirements

## BEST-PRACTICES-KANDIDATEN:

1. Für Android/Gradle-Cloud-Builds (z. B. android-cloud-bau-Skill) prüfen, ob `dl.google.com` benötigt wird — es steht NICHT auf der Trusted-Default-Liste (anders als `maven.google.com`, `repo.maven.apache.org`, `services.gradle.org`). Falls ein Build `dl.google.com` direkt referenziert, muss die Cloud-Umgebung auf „Custom" gestellt und die Domain manuell ergänzt werden, sonst schlägt der Abruf mit 403 fehl.
2. Keine Secrets/API-Keys in die „Environment variables" eines Cloud-Environments eintragen — sie sind für jeden lesbar, der die Umgebung nutzt (auch bei „organization-shared environments"). Auf Pro/Max-Plänen stattdessen „API credentials" nutzen (serverseitig injiziert, nie für Claude/Commands sichtbar).
3. Bun in Cloud-Sitzungen meiden bzw. vorsichtig behandeln, wenn Paketinstallationen über den Security-Proxy nötig sind — offiziell als bekanntes Proxy-Kompatibilitätsproblem dokumentiert. npm/yarn/pnpm sind die verlässlicheren Alternativen.
4. Bei eigenen mTLS-/Custom-CA-Anforderungen (z. B. Firmen-TLS-Interception-Proxy) funktioniert das in Anthropic-gehosteten Cloud-Sessions NICHT über `NODE_EXTRA_CA_CERTS`/`CLAUDE_CODE_CLIENT_CERT` — diese werden dort ignoriert. Für solche Anforderungen ist ein self-hosted environment nötig.
5. Bei Organisationen mit IP-Allowlisting: Cloud-Sessions vorab mit Anthropic-Support klären (Ausnahme für Anthropic-Hosting-IPs), sonst schlagen alle Cloud-Sessions mit Auth-Fehler fehl.

## BUG-KANDIDATEN:

1. **Symptom:** In „Custom"/„Additional allowed domains" eingetragene Domains werden im Netzwerk-Egress von Claude-Code-Cloud-Containern nicht berücksichtigt; Requests scheitern mit `403 Forbidden`, Header `x-deny-reason: host_not_allowed`.
   **Ursache (vermutet, laut Issue):** Der dem Container ausgestellte JWT-Token enthält die benutzerdefinierten Domains nicht im `allowed_hosts`-Claim; nur die Standard-„Package managers only"-Domains werden berücksichtigt.
   **Version:** Claude Code on the Web, betroffen mit Claude-Opus-Modell (Zeitpunkt/Build unklar).
   **Workaround:** Keiner dokumentiert.
   **Status:** Als Duplikat von Issue #11897 geschlossen.
   **URL:** [extern] https://github.com/anthropics/claude-code/issues/19087

2. **Symptom:** Der offizielle `slide-deck-designer`-Skill scheitert im Cowork-/Sandbox-Kontext, weil `registry.npmjs.org` (und `unpkg.com`, `jsdelivr.net`, `cdnjs.cloudflare.com`, `github.com`) vom Proxy mit 403 blockiert werden; nötige npm-Pakete (`pptxgenjs`, `react`, `react-dom`, `react-icons`, `sharp`) können nicht installiert werden.
   **Ursache:** Die Sandbox-/Proxy-Allowlist war für Python-Tooling (pypi.org) konfiguriert, aber nicht durchgängig für Node/npm-Tooling geöffnet.
   **Version:** Cowork-Sandbox (nicht identisch mit claude.ai/code-Cloud-Environments, aber gleiche Proxy-Architektur-Klasse).
   **Workaround:** Ersatz von `pptxgenjs` durch bereits vorinstalliertes `python-pptx` (schlechtere Ausgabequalität).
   **Status:** Als Duplikat geschlossen (verweist auf #43334).
   **URL:** [extern] https://github.com/anthropics/claude-code/issues/43334

3. **Symptom (offiziell dokumentiert, kein GitHub-Issue nötig):** Bun-Paketinstallationen funktionieren im Security-Proxy von Cloud-Sitzungen nicht zuverlässig.
   **Ursache:** Bun respektiert/verhält sich inkompatibel mit dem verpflichtenden HTTP/HTTPS-Security-Proxy Anthropic-gehosteter Sitzungen.
   **Version:** aktuelle Cloud-Sessions (Node 20/21/22 vorinstalliert, Bun als Fußnote „bun¹").
   **Workaround:** npm/yarn/pnpm statt Bun verwenden, oder Pakete per Setup-Skript vorinstallieren/cachen.
   **URL:** [offiziell] https://code.claude.com/docs/en/cloud-environments#installed-tools

## OFFEN/UNSICHER:

- Ob die beiden geschlossenen GitHub-Issues (#19087, #43334) inzwischen durch das jeweils referenzierte Duplikat-Issue (#11897 bzw. das Ziel von #43334) tatsächlich gefixt wurden, ließ sich in dieser Recherche nicht verifizieren — die verlinkten „Original"-Issues wurden nicht separat abgerufen.
- Unklar, ob `dl.google.com` bewusst von der Trusted-Liste ausgeschlossen wurde (weil `maven.google.com` für Gradle/Android-Zwecke als ausreichend gilt) oder ob es sich um eine Lücke handelt — die Doku äußert sich dazu nicht explizit.
- Keine primäre Anthropic-Quelle mit einer vollständigen, aktuellen Liste bekannter „Tools, die den Proxy nicht respektieren" gefunden außer der Bun-Erwähnung; weitere Fälle wurden nur in Community-/Issue-Quellen angedeutet, nicht offiziell bestätigt.
- Nicht verifiziert, ob `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC` auch in Cloud-Sessions wirkt (Dokumentation dazu bezieht sich primär auf lokale/self-hosted Nutzung).
