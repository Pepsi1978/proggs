## Arbeitsweise
- [Commit](commit-nur-mit-pfaden.md): fremde Änderungen in der Datei → eigenen Teil vormerken, `git commit` ohne Pfade
- [Parallele Sitzungen](parallele-sitzung-nimmt-dateien-mit.md): neue Pakete zuerst committen; Löschungen danach prüfen
- [Lange Skripte](lange-skripte-per-write.md): >30 Zeilen nie als Bash-Heredoc, per Write in den Scratchpad; PowerShell: „“ brechen jeden Befehl → Edit/Write
- [Rückfrage](jarvis-rueckfrage-nie-stehen-bleiben.md): PC + Jarvis je 5 min, dann selbst entscheiden (auch Review)
- [HTML](html-immer-in-chrome-oeffnen.md): sofort per Start-Process chrome öffnen
- [Fehler an Anthropic](fehler-an-anthropic-als-github-issue.md): `gh issue create` in anthropics/claude-code, /feedback gesperrt
- [HTTP-Timeout](http-timeouts-fallback-wartezeit.md): scheitert genau am Limit → curl -4 gegen -6, IPv4 zuerst
- [Agenten-Pipe](gestartetes-programm-erbt-agenten-pipe.md): „started“, Tool wartet bis Timeout = Handle-Vererbung
- [Fenster-Aktivierung](aktivierung-erfolg-nur-mit-isiconic.md): Erfolg nur mit IsIconic prüfen

## Claude Code, Launcher, Konten
- [Config-Ordner](aktiver-config-ordner-openlauncher-profil.md): OpenLauncher-Profil, nicht ~/.claude
- [Regeln](profilregeln-aus-sources-generiert.md): kommen aus sources/; fehlt eine → Repo veraltet
- [OpenLauncher](openlauncher-deploy-update-skript.md): nur Update-Skript (Regel 13), nie dotnet build
- [advisorModel](advisormodel-alias-statt-modell-id.md): Alias, keine Modell-ID (sonst still aus)
- [Synced-Plugins](synced-plugins-abschalten.md): "name@synced": false in enabledPlugins
- [Claude Desktop](claude-desktop-store-pfade.md): Store-Version, Pfade nicht unter %APPDATA%\Claude
- [Anthropic-Schlüssel](anthropic-schluessel-praefix.md): beginnt mit sk-ant-user, nie Präfix prüfen
- [ChatGPT-Abo](chatgpt-abo-100-euro.md): 100 €, kein Pro 500 → kein Ultrafast per OAuth
- [Codex-Mails](github-codex-mails-participating.md): kommen über Participating (author), dort aus
- [KI-Briefing](ki-briefing-push-im-prompt.md): Prompt ruft PushNotification selbst; nur über claude.ai bearbeiten

## Handy, Android
- [adb-WLAN](adb-wlan-selbstheilend.md): Ausfall → Log + Wachhund-Aufgabe; TLS-Port nie speichern
- [Tunnel langsam](tunnel-install-langsam-heim-wlan.md): 10.8.0.7 hängt = Heim-WLAN; im Hotspot per LAN-IP
- [Jarvis Tablet](jarvis-tablet-zweites-geraet.md): Tab S9 = zweites Gerät; geraete.json, auf beide installieren, Abgleich
- [Installation sichtbar](installation-immer-im-vordergrund.md): nie run_in_background, immer Vordergrund, Geräte nacheinander
- [Fernsteuern](handy-nicht-blind-fernsteuern.md): vor adb-Eingaben Vordergrund-App prüfen
- [Link zu Windows](link-zu-windows-explorer.md): nie force-stoppen, Start per StartUpActivity
- [Cloud-Bau](android-cloud-bau-rclone.md): rclone-Konfig in SK\rclone, eigener OAuth-Client
- [Android immer Debug](android-immer-debug.md): apk-update-Standard assembleDebug, keine Release-/schnell-Ausnahmen (außer BestJournal Play)
- [Gradle-Cache](gradle-config-cache-versionslog.md): apk-update „Build veraltet?“ → .gradle/configuration-cache löschen
- [apk-update](apk-update-nicht-aus-git-bash.md): nicht aus Git Bash starten (sonst „origin/main nicht verfügbar“), PowerShell-Tool nehmen
- [Overlay-Version](overlay-informationalversion-mitziehen.md): TVO/CVO-csproj nur Version bumpen, InformationalVersion nie von Hand
- [Hostinger](hostinger-server-rolle.md): nur Rückfall ohne Rechner; Android bauen + Tunnel-Install

## Jarvis, Rechner, Projekte
- [Relay + ChatGPT-Plugin](jarvis-relay-und-chatgpt-plugin.md): Hostinger-Relay; Plugin als benutzerdefinierter MCP-Server
- [PC-Brücke](jarvis-pc-bruecke-neustart.md): nie nur Stop-ScheduledTask, Skript nutzen
- [Fernwartung](windows-fernwartung-zugeklappt.md): kein Bild = Anzeige aus; Notweg Parsec VDD
- [Biomarker alt](biomarker-abgleich-stand-je-quelle.md): Diagnose-Protokoll per adb; Whoop neu anmelden
- [Azure](azure-speech-schluessel.md): „baue Azure ein“ → SK\Azure\azure-speech.json
- [Dieser Rechner](dieser-rechner-installationswege.md): npm-CLIs, LM Studio, Stream Deck per winget
- [Explorer blinkt](explorer-blinkschutz.md): Wächter-Log; leerer UserChoice-Schlüssel einer Store-App
- Grafik: [DLSS-5-Mod](dlss5-mod-installiert.md), [MW2-Override](mw2-dlss-override-eingerichtet.md) (nach Treiber-Clean-Install neu)
- Angefangen: [KimiKompass](kimikompass-angefangen.md), [Epigenetik](epigenetik-angefangen.md), [KI-Agenten-Kurs](ki-agenten-kurs-angefangen.md)
- Nur bei Bedarf: apps-nie-aus-claude-shell-starten, bash-commit-heredoc-syntax, bauweise-ohne-spec-pipeline, claude-profile-architektur-launcher, compose-fussleiste-verdraengt, deploy-guard-port-unterscheidung, gedankenspeicher-logikfehler-jagd, kompass-doku-luecke-zweite-quelle, modul-bibliothek, opencode-fast-priority-tier-wirkungslos, opus5-leitplanken, overlay-tooltips-app-weit-gesperrt, perfectmoment-version-bump-pflicht, repo-branch-und-sync-setup, vto-mikrofon-fehlt-stummer-klick
