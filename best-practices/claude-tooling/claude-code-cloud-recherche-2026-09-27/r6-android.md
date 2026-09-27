# Researcher 6: Android-/Gradle-Entwicklung in Claude-Code-Cloud-Sitzungen

Stand der Recherche: 27.09.2026

## Befunde

### 1. Java/Gradle ist bereits vorinstalliert, Android SDK nicht
In Anthropic-gehosteten Cloud-Umgebungen läuft jede Sitzung in einer frischen Ubuntu-24.04-VM (x86_64) mit vorinstallierten Toolchains. Für Java: **OpenJDK 21 mit Maven und Gradle** sind vorinstalliert. Toolchains außerhalb der Liste (z. B. .NET SDK) sind NICHT vorinstalliert, auch wenn ihre Package-Registries auf der Standard-Allowlist stehen — das gilt analog für das Android SDK: es ist nicht Teil des Basisimages und muss per Setup-Skript installiert werden.
Quelle: https://code.claude.com/docs/en/cloud-environments [offiziell]

### 2. Netzwerk-Allowlist ("Trusted") enthält die JVM/Gradle-Domains, aber NICHT dl.google.com
Die offizielle Standard-Allowlist ("Trusted"-Stufe) listet unter "JVM package managers" explizit: `maven.org, repo.maven.org, central.maven.org, repo1.maven.org, repo.maven.apache.org, maven.google.com, jcenter.bintray.com, gradle.org, services.gradle.org, plugins.gradle.org, plugins-artifacts.gradle.org, kotlinlang.org, spring.io, repo.spring.io`. Unter "Development tools and platforms" steht zusätzlich `developer.android.com`. **`dl.google.com` taucht in der offiziellen Domain-Liste nicht auf** — das ist aber genau die Domain, über die `sdkmanager` SDK-Pakete (Plattformen, Build-Tools, cmdline-tools-Bootstrap) herunterlädt.
Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt "Default allowed domains") [offiziell]

### 3. Praxisbeleg: Google-Maven-Domains werden trotz Allowlist teils blockiert
In einem realen Android-Projekt (GitHub Issue) scheitern Cloud-Sessions mit **403-CONNECT-Fehlern für `dl.google.com` UND `maven.google.com`**, obwohl `services.gradle.org` erreichbar ist. Folge: Android Gradle Plugin und androidx-Artefakte lassen sich nicht auflösen, Gradle-Builds sind in der Cloud-Sitzung komplett unmöglich. Das Team nutzt deshalb CI als einzigen echten Compiler und schreibt in der Cloud-Sitzung "blind" (Ergebnis: 24 Commits für ein Feature, davon ~6 echte Arbeit, mehrere Commits mit trivialen Compile-Fehlern, die lokal sofort auffallen würden, ca. 10 Minuten CI-Durchlauf pro Versuch).
Quelle: https://github.com/LocNgu/YAPT-Yet-Another-Plant-Tracker/issues/419 [extern]
Hinweis zur Einordnung: unklar, ob das betroffene Environment "Trusted" oder ein "Custom"-Netzwerkzugriff ohne aktivierte "Also include default list"-Checkbox war (dann wäre es Nutzerkonfiguration statt Anthropic-Bug); da `maven.google.com` laut Doku eigentlich in "Trusted" enthalten ist, deutet der Fund aber auf ein echtes Proxy-/Allowlist-Problem hin (Muster wie bei Maven-Central/crates.io, siehe Befund 4).

### 4. Bekanntes Muster: Trusted-Domain in Doku gelistet, DNS/Proxy blockiert trotzdem
Für Maven Central selbst gibt es einen offenen, bestätigten Bug: `./mvnw` schlägt mit `Temporary failure in name resolution` für `repo.maven.apache.org` fehl, **obwohl** die Domain in der Doku als "Trusted" gelistet ist. Es gibt ein Präzedenz-/Vergleichsproblem für crates.io (Issue #10307), das laut Community-Analyse durch Aufnahme der Domain in `NO_PROXY` gefixt wurde. Für Maven Central ist das offenbar noch offen (Stand Recherche: keine Anthropic-Antwort im Issue).
Quelle: https://github.com/anthropics/claude-code/issues/13372 [extern/GitHub-Issue]

### 5. Setup-Skripte für Android SDK in Cloud-Sitzungen existieren bereits (Community)
Es gibt ein fertiges, wiederverwendbares Setup-Skript ("One-liner Android SDK setup for Claude Code remote environments"): lädt Android-Commandline-Tools, ermittelt automatisch neueste SDK-Plattform/Build-Tools, akzeptiert Lizenzen via `sdkmanager`, installiert platform-tools (adb). Konfigurierbar über `SDK_PLATFORM`, `BUILD_TOOLS`, `ANDROID_HOME` (Default `~/android-sdk`), `SHELL_RC`. **Fallback-Mechanismus eingebaut**: Wenn `sdkmanager`-Download fehlschlägt ("typisch bei Proxy-Einschränkungen wie in Claude Code Remote-Containern"), lädt das Skript automatisch direkt per curl.
Quelle: https://gist.github.com/warting/d2e33954309fdd22afc905f9cdf33e14 [extern, Community-Skript]

### 6. Zwei-Ebenen-Architektur: einmaliges Setup-Skript + SessionStart-Hook
Community-Beispiele (mehrere PRs) verwenden ein Muster mit zwei Teilen:
- **Setup-Skript** (im Environment-Dialog konfiguriert): läuft einmalig vor Sessionstart, installiert Android-Commandline-Tools, compileSdk-Plattform, platform-tools. Wird bei Erfolg innerhalb von ~5 Minuten als Filesystem-Snapshot gecacht, spätere Sessions starten daraus ohne erneute Installation.
- **SessionStart-Hook** (`.claude/hooks/...`, in `.claude/settings.json` registriert, aus dem Repo committet): läuft bei jedem Sessionstart/Resume, setzt `ANDROID_HOME`/`ANDROID_SDK_ROOT`/`PATH` über `$CLAUDE_ENV_FILE`, schreibt `local.properties`. Guard über `if [ "$CLAUDE_CODE_REMOTE" != "true" ]; then exit 0; fi`, damit der Hook lokal nicht läuft.
Quellen: https://code.claude.com/docs/en/cloud-environments (Abschnitt "Setup scripts vs. SessionStart hooks") [offiziell]; Beispiel-PRs: https://github.com/fmheim/green-griffin/pull/4, https://github.com/aveshev/TPMS-advanced-NE/pull/26 [extern]

### 7. JDK-Version anpassen (z. B. JDK 25 statt vorinstalliertem JDK 21)
Ein SessionStart-Hook kann `apt-get install -y openjdk-25-jdk` ausführen (~20 Sekunden), wenn ein Projekt eine neuere Java-Version pinnt als das vorinstallierte OpenJDK 21. Gradle-Toolchain-Erkennung findet die zusätzliche JVM automatisch unter `/usr/lib/jvm`. Es gibt außerdem offene Feature-Requests, JDK 25 direkt ins Standardimage aufzunehmen.
Quellen: https://github.com/sdobie/space-colony/pull/7 [extern]; https://github.com/anthropics/claude-code/issues/45527, https://github.com/anthropics/claude-code/issues/96769 [extern, offene Feature-Requests]

### 8. Gradle-Warmup gegen langsame Cold-Builds
Community-Pattern: ein Hook startet im Hintergrund `nohup ./gradlew <task> --console=plain > /tmp/gradle-warmup.log &`, um Abhängigkeiten/Compiler während des Sessionstarts vorzuladen, ohne den Start zu blockieren (Rückkehr nach wenigen ms). Offizielle Doku bestätigt separat: das erste `./gradlew help` in einem frischen Container lädt Gradle selbst und baut buildSrc; spätere Aufrufe sind durch den Environment-Cache sehr schnell.
Quellen: https://github.com/uiopak/lst-crc/pull/83 [extern]; https://code.claude.com/docs/en/cloud-environments [offiziell, Environment-Caching-Abschnitt]

### 9. Ressourcen- und Zeitlimits (offiziell, gelten für Gradle-Builds mit)
- **Ressourcen:** 4 vCPUs, 16 GB RAM, 30 GB Disk pro Session-VM. "Die VM kann Tasks stoppen, die deutlich mehr Speicher brauchen, wie große Build-Jobs oder speicherintensive Tests" — das betrifft potenziell den Gradle-Daemon bei großen Android-Projekten.
- **Befehls-Timeout:** Bash-Tool-Standard 2 Minuten, auf Anfrage bis 10 Minuten; bei Timeout wird der Befehl in den Hintergrund verschoben statt gestoppt (außer bei `sleep`-Befehlen). Für Environments lässt sich per `BASH_DEFAULT_TIMEOUT_MS`/`BASH_MAX_TIMEOUT_MS` (Millisekunden) global erhöhen.
- **SessionStart-Hook-Timeout:** 600 Sekunden Standard, per `timeout`-Feld überschreibbar; mit `async: true` kein Timeout.
- **Setup-Skript-Timeout:** muss unter ca. 5 Minuten bleiben, sonst wird das Environment NICHT gecacht (jede neue Session führt es dann erneut komplett aus — bei Android-SDK-Downloads ein reales Risiko).
Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitte "Resource limits", "Time limits", "Script requirements") [offiziell]

### 10. Instrumented Tests/Emulator NICHT in der Cloud-Sitzung möglich
Es gibt keinen Android-Emulator in der Cloud-Sandbox. Community-Beleg: `./gradlew compileDebugKotlin compileDebugAndroidTestKotlin` und `./gradlew testDebugUnitTest lintDebug` laufen erfolgreich in einer frischen Cloud-Sitzung, aber "instrumented tests still require CI's emulator". Reine Unit-Tests (JVM, kein Gerät) funktionieren also, geräteabhängige Tests nicht.
Quelle: Suchergebnis-Snippet, sekundär zitiert, ursprüngliche Quelle nicht einzeln verifiziert [extern, mittlere Verlässlichkeit] — als unsicher markiert, siehe "Offen/Unsicher".

Separat, aber am Rande relevant: Anthropic arbeitet an nativer Android-Emulator-Integration für **Claude Desktop** (lokal, nicht Cloud), laut Reverse-Engineering/Leak-Berichten noch nicht freigeschaltet (Stand der zitierten Quelle).
Quelle: https://runtimewire.com/article/claude-desktop-is-ready-to-test-android-apps-whenever-anthropic-flips-the-switch [extern, spekulativ/Leak-basiert]

### 11. Etabliertes Muster: Cloud-Sitzung programmiert, CI baut/signiert (Anthropic-Doku deckt das explizit ab)
Die offizielle Doku sieht dieses Muster als Normalfall vor: Cloud-Sessions haben eingebaute GitHub-Tools (Issues, PRs lesen, Diffs holen, Kommentare posten) über einen dedizierten GitHub-Proxy, der reale Credentials nie in die Session-VM lässt. Damit ist der Workflow "Cloud-Session erstellt PR → GitHub Actions baut/signiert nach Merge" ein Standardmuster, keine Krücke. Der GitHub-Proxy erlaubt `git push` nur auf den aktuellen Arbeitsbranch der Session (Push-Protection), GraphQL ist auf einen fixen Satz PR-Operationen beschränkt.
Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt "GitHub proxy") [offiziell]

Community-Belege für dieses konkrete Muster bei Android-Apps (GitHub-Actions-Workflow baut/signiert APK nach Merge, legt sie z. B. in einen Cloud-Speicher): mehrere PRs mit "Add GitHub Actions workflow to build and publish installable APK" u. ä.
Quellen (Beispiele): https://github.com/aranyoray/anubhavandroid/pull/5, https://github.com/talibmohd0099/Crowd_Rush/pull/2, https://github.com/eugene8080/claude-usage-widget/pull/2 [extern]

**Hinweis:** Dieses Repository selbst (`barwa/proggs`) hat bereits eine dokumentierte eigene Umsetzung dieses genauen Musters — "Android-Cloud-Bau mit geteiltem Keystore" (Pull Request #118 im eigenen Repo, referenziert im Skill `apk-update-cloud`: Cloud-Sitzung committet, öffnet PR, wartet auf Codex-Review, merged selbst, GitHub Actions baut/signiert mit geteiltem Key und legt APK+update.json nach Google Drive). Das deckt sich mit dem hier gefundenen externen Best-Practice-Muster und bestätigt es als sinnvoll.
Quelle: eigenes Repo, PR #118 (Titel: "Doku: Einrichtungsauftrag für Android-Cloud-Bau mit geteiltem Keystore") [Projekt-intern, zur Einordnung erwähnt]

### 12. GitHub Copilot Coding Agent: identisches Firewall-Problem, offiziell dokumentierte Lösung
GitHub Copilots Coding-Agent-Sandbox hat ebenfalls eine Standard-Firewall/Allowlist, die bei Android-Projekten **`developer.android.com` und `dl.google.com` standardmäßig blockiert**. Symptom: Gradle-Build scheitert mit "Plugin was not found in any of the following sources". Der Agent meldet in einem PR-Kommentar selbst "Firewall rules blocked me from connecting to one or more addresses". Lösung: In den Repository-Einstellungen unter "Coding agent" → Firewall → Custom Allowlist die blockierten Domains manuell ergänzen. Das ist ein offiziell von GitHub dokumentiertes Konfigurationsfeature (nicht nur ein Workaround).
Quellen: https://docs.github.com/en/copilot/how-tos/use-copilot-agents/coding-agent/customize-the-agent-firewall [offiziell/GitHub]; https://dev.to/hossain/fixing-github-coding-agents-firewall-issue-for-android-projects-2gcn [extern, Erfahrungsbericht]; https://github.com/github/docs/issues/43216 [extern]

Ergänzend: In einem Repo (XRDOGE-XRPL/AndroidSA) hat der Copilot-Agent selbst einen PR erstellt, der Gradle-Abhängigkeiten vorab hinter der Firewall "prewarmt" — ein Community-Pattern analog zum Claude-Code-Warmup-Hook aus Befund 8.
Quelle: https://github.com/XRDOGE-XRPL/AndroidSA/pull/17 [extern]

### 13. OpenAI Codex Cloud: Netzwerk-Sandbox blockiert Gradle standardmäßig
Bei Codex CLI/Cloud ist die Standard-Sandbox netzwerkisoliert; ein Nutzerbericht beschreibt, dass Codex "Gradle nicht verwenden will", weil die Sandbox es blockiert (Issue als "enhancement"/"sandbox" gelabelt, keine dokumentierte Lösung im Issue, mittlerweile geschlossen). Im OpenAI-Community-Forum existieren funktionierende, von Nutzern gebaute Setup-Skripte für Android/Kotlin in Codex: Download von Android-Commandline-Tools per `wget`, Entpacken, `ANDROID_HOME` setzen, Lizenzen akzeptieren via `sdkmanager --licenses` mit mehrfacher `y`-Eingabe per Heredoc (als "einzige funktionierende Methode" beschrieben — einfaches `yes | sdkmanager --licenses` reicht demnach nicht zuverlässig). Genannte Zusatzprobleme: Broken-Pipe-Fehler bei Installationsbefehlen ohne `bash -c`-Wrapping; SSH-Git-Submodule scheitern mit "Network is unreachable" (Workaround: HTTPS statt SSH für Submodule verwenden).
Quellen: https://github.com/openai/codex/issues/5228 [extern]; https://community.openai.com/t/setup-script-for-codex-for-android-development/1280093 [extern, Community-Forum]

### 14. Google Jules: erstellt Android-Gradle-Projekte automatisch per PR
Beleg für Jules und Android/Gradle ist dünner als bei Claude Code/Codex/Copilot. Ein Beispiel zeigt, dass Jules (`google-labs-jules[bot]`) eigenständig einen PR mit Android-Gradle-Projekt-Setup erstellt hat. Keine belastbaren Erfahrungsberichte zu Netzwerk-/Proxy-Problemen bei Gradle/Android-SDK in Jules gefunden — hierzu keine verlässliche Aussage möglich.
Quelle: https://github.com/1337farm/iroh-android-native/pull/1 [extern, einzelnes Beispiel, keine Tiefenrecherche möglich]

## BEST-PRACTICES-KANDIDATEN:

- **Zwei-Ebenen-Setup für Android in Claude-Code-Cloud-Sessions**: Setup-Skript im Environment-Dialog installiert Android-Commandline-Tools + SDK-Plattform + Build-Tools + platform-tools einmalig (gecacht, <5 Min. Laufzeit einhalten); zusätzlich ein committeter SessionStart-Hook (`.claude/hooks/...`, per `CLAUDE_CODE_REMOTE`-Guard nur in der Cloud aktiv) setzt `ANDROID_HOME`/`ANDROID_SDK_ROOT`/`PATH` über `$CLAUDE_ENV_FILE` und schreibt `local.properties`. Quelle: https://code.claude.com/docs/en/cloud-environments
- **Gradle-Warmup-Hook** (`nohup ./gradlew <task> --console=plain > /tmp/gradle-warmup.log &`, non-blocking) reduziert gefühlte Cold-Build-Zeit in neuen Sessions. Quelle: https://github.com/uiopak/lst-crc/pull/83
- **Muster "Cloud-Session programmiert, CI baut/signiert"** ist von Anthropic selbst über den GitHub-Proxy als Normalfall vorgesehen und sollte bei Android-Projekten mit Signierungsbedarf grundsätzlich bevorzugt werden, statt zu versuchen, Release-Signierung/Emulator-Tests direkt in der Cloud-Sitzung zu erzwingen (die Sandbox unterstützt ohnehin keine Instrumented Tests/Emulatoren). Quelle: https://code.claude.com/docs/en/cloud-environments
- **SDK-Lizenzen zuverlässig automatisiert akzeptieren**: einfaches `yes | sdkmanager --licenses` ist laut Codex-Community-Erfahrung nicht zuverlässig; ein Heredoc mit mehrfacher `y`-Eingabe an `sdkmanager --sdk_root=... --licenses` funktioniert robuster. Übertragbar auf Claude-Code-Setup-Skripte. Quelle: https://community.openai.com/t/setup-script-for-codex-for-android-development/1280093
- **Bei Bedarf abweichende JDK-Version per SessionStart-Hook nachinstallieren** (`apt-get install -y openjdk-25-jdk`), statt auf Standardimage-Update zu warten; Gradle-Toolchain-Erkennung findet zusätzliche JVMs automatisch. Quelle: https://github.com/sdobie/space-colony/pull/7

## BUG-KANDIDATEN:

1. **Symptom:** Gradle-Builds in Claude-Code-Cloud-Sitzungen scheitern mit 403-CONNECT-Fehlern beim Auflösen von Android Gradle Plugin/androidx-Artefakten.
   **Ursache:** Netzwerk-Proxy blockiert `dl.google.com` und teils auch `maven.google.com`, obwohl `maven.google.com` laut offizieller Doku in der "Trusted"-Default-Allowlist steht; `dl.google.com` steht dort explizit NICHT drin (Lücke in der Dokumentation/Allowlist selbst).
   **Version:** Claude Code Web/Cloud, Stand des Issues nicht exakt datiert, im Kontext der aktuellen Recherche (27.09.2026) noch relevant, da die offizielle Domain-Liste bei Abruf am 27.09.2026 `dl.google.com` weiterhin nicht enthält.
   **Workaround:** Eigenes Environment auf "Custom" umstellen und `dl.google.com` (und sicherheitshalber `maven.google.com`) manuell zur Allowlist hinzufügen ("Also include default list of common package managers" zusätzlich aktivieren); alternativ Gradle-Cache per Setup-Skript vorpopulieren für Offline-Fähigkeit; alternativ Android-Builds komplett aus der Cloud-Sitzung heraushalten und nur über GitHub Actions/CI bauen lassen.
   **URL:** https://github.com/LocNgu/YAPT-Yet-Another-Plant-Tracker/issues/419 ; Allowlist-Referenz https://code.claude.com/docs/en/cloud-environments

2. **Symptom:** Maven/Gradle-Builds scheitern mit `repo.maven.apache.org: Temporary failure in name resolution`.
   **Ursache:** DNS-Auflösung für eine laut Doku "Trusted" gelistete Domain schlägt im Cloud-Proxy fehl; vermutetes Muster wie beim (laut Community bereits gefixten) crates.io-Problem — fehlender Eintrag in `NO_PROXY` serverseitig.
   **Version:** Claude Code Web, Issue seit 8. Dez 2025 offen, Stand Recherche (27.09.2026) laut Fetch weiterhin ohne Anthropic-Antwort im Thread.
   **Workaround:** Kein offizieller Fix bekannt; Community schlägt vor, betroffene Maven-Domains einem Custom-Network-Access explizit hinzuzufügen (was am DNS-Problem selbst evtl. nichts ändert, da es laut Bericht trotz Trusted-Listung auftritt) oder Abhängigkeiten vorab in CI aufzulösen.
   **URL:** https://github.com/anthropics/claude-code/issues/13372

## OFFEN/UNSICHER:

- Unklar, ob das 403-Problem aus Bug-Kandidat 1 aktuell (27.09.2026, CLI 2.1.283) noch vorliegt oder von Anthropic zwischenzeitlich gefixt wurde — das verlinkte Issue trägt kein Datum im gelieferten Fetch, und es gibt keine offizielle Anthropic-Stellungnahme dazu. Vor produktivem Einsatz sollte ein eigener Testlauf (`./gradlew assembleDebug` in einer frischen Cloud-Session mit Trusted-Netzwerk) diesen Punkt verifizieren.
- Ob Instrumented Tests (Android-Emulator) in Claude-Code-Cloud-Sessions grundsätzlich unmöglich sind, stützt sich nur auf ein sekundär zitiertes Suchergebnis-Snippet ohne verifizierte Primärquelle — Aussage als schwach belegt einzustufen, aber plausibel (die offizielle Doku nennt keinerlei Emulator-/Grafik-Unterstützung für Cloud-VMs, nur 4 vCPU/16 GB RAM/30 GB Disk ohne GPU/KVM-Hinweis).
- Zu Google Jules und Android/Gradle-Builds wurde nur ein einzelnes, wenig aussagekräftiges Beispiel gefunden; belastbare Erfahrungsberichte zu SDK-Setup, Netzwerk-Restriktionen oder Timeouts bei Jules fehlen in dieser Recherche komplett.
- Nicht verifiziert, ob das Setup-Skript-Zeitlimit von "roughly five minutes" in der Praxis für einen vollständigen `sdkmanager`-Download (cmdline-tools + Plattform + Build-Tools, teils mehrere hundert MB) ausreicht, insbesondere wenn zusätzlich der oben genannte dl.google.com-Bug einen Fallback auf langsameres direktes curl-Herunterladen erzwingt. Community-Skripte (Befund 5) adressieren das Problem, aber ohne belegte Zeitmessung.
- Die genaue vollständige Liste der von einem Nutzer zur GitHub-Copilot-Allowlist hinzugefügten Domains (Befund 12, Medium-Artikel) konnte nicht abgerufen werden (HTTP 403 auf Medium; dev.to-Mirror nannte nur die zwei Kern-Domains ohne vollständige Liste).
