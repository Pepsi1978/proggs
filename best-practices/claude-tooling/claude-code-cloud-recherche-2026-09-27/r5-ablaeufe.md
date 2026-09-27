# Researcher 5 — Claude Code on the web: Arbeitsabläufe, Möglichkeiten und Grenzen

Stand der Recherche: 27.09.2026, lokale CLI-Version 2.1.283.

## Befunde

### Zugänge / Start einer Cloud-Session
- Cloud-Sessions können gestartet werden über: Browser (claude.ai/code), die **Code**-Registerkarte der Claude-App (iOS/Android), die Desktop-App (Auswahl **Cloud** statt **Local**), das Terminal (`claude --cloud "Aufgabe"`), sowie automatisch durch **Routines** (geplante/getriggerte Läufe). [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Verfügbarkeit: Pro-, Max- und Team-Pläne, sowie Enterprise-Nutzer mit Premium-Seats oder „Chat + Claude Code"-Seats. Reguläre Enterprise-Standard-Seats sind laut Doku NICHT explizit genannt (offen, siehe unten). [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- GitHub-Anbindung ist Pflicht für Cloud-Sessions: entweder über die Claude GitHub App (Browser-Onboarding) oder über `/web-setup` (sendet den lokalen `gh`-CLI-Token ans Claude-Konto). Ohne GitHub-Konto zeigt die Seite nur einen Login-Button. [offiziell] https://code.claude.com/docs/en/web-quickstart
- Organisationen mit Zero Data Retention können `/web-setup` und andere Cloud-Session-Features nicht nutzen. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web

### Terminal ↔ Cloud: `--cloud`, `--teleport`, `/teleport`
- `claude --cloud "Aufgabe"` erstellt eine neue Cloud-Session für das aktuelle Repository; die Cloud-VM klont den GitHub-Remote-Branch (NICHT den lokalen Checkout) — vorher pushen! [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- `claude -p "Nachricht" --cloud <session-id>` schickt eine Folgenachricht an eine laufende Cloud-Session, von jeder Maschine mit `claude auth login`, ohne lokalen Session-State zu senden — auch aus CI-Skripten nutzbar. Antwortet nicht auf die Antwort, postet nur. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- **Handoff ist von der CLI aus einseitig**: `--teleport` zieht eine Cloud-Session ins Terminal, aber man kann keine bestehende Terminal-Session in die Cloud „pushen" — dafür bietet nur die Desktop-App ein **Continue in**-Menü. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Teleport-Varianten: `claude --teleport` (interaktive Auswahl), `claude --teleport <session-id>`, `/teleport` bzw. `/tp` innerhalb einer Session, `/tasks` → Taste `t`, oder „Open in > Terminal" auf claude.ai/code. Voraussetzungen: sauberer Git-Status (sonst Stash-Aufforderung), gleiches Repository (kein Fork), gepushter Branch, gleiches claude.ai-Konto. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Nach dem Teleport ist die Terminal-Session eine eigene Kopie: neue Arbeit dort erscheint NICHT mehr in der Cloud-Session. Um vom Handy aus weiterzusteuern, muss danach `/remote-control` gestartet werden. [offiziell] ebenda
- `--teleport` erfordert claude.ai-Abo-Authentifizierung (kein API-Key, kein Bedrock/Vertex/Foundry). [offiziell] ebenda

### Paralleles Arbeiten
- Jeder `--cloud`-Befehl erzeugt eine eigene, unabhängige Session — mehrere Aufgaben können gleichzeitig in getrennten Sessions/Branches laufen, ohne Worktree-Verwaltung. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Tipp „Plan lokal, ausführen in der Cloud": erst `claude --permission-mode plan` lokal zum gemeinsamen Planen, Plan committen/pushen, dann `claude --cloud "Führe den Plan in docs/... aus"`. [offiziell] ebenda
- Projekte (`/docs/en/claude-projects`) koordinieren mehrere parallele Cloud-Sessions für ein Arbeitsgebiet zentral. [offiziell] ebenda

### Wiederaufnahme / Sessions verwalten
- Sessions laufen weiter, auch wenn man den Tab schließt oder das Laptop zuklappt — das ist gewolltes Verhalten. [offiziell] https://code.claude.com/docs/en/web-quickstart
- Sessions lassen sich archivieren (aus der Liste ausblenden) oder endgültig löschen (unwiderruflich, mit Bestätigung). [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Bei Umgebungs-Timeout (`environment expired`, nach Inaktivität) wird die Session beim erneuten Öffnen mit frischer VM + wiederhergestellter Konversation neu gestartet; **Hintergrundarbeit wie Subagents und laufende Shell-Befehle wird NICHT wiederhergestellt**. Eine Session zählt bereits als inaktiv, während sie auf eine MCP-Connector-Genehmigung wartet. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- `--resume` ist etwas anderes als `--teleport`: `--resume` lädt lokale Konversationshistorie, listet keine Cloud-Sessions. [offiziell] ebenda

### Benachrichtigungen (Mobile Push)
- Push-Benachrichtigungen funktionieren über Remote Control (nicht spezifisch Cloud-Sessions): Claude entscheidet selbst, wann gepusht wird (typischerweise bei Abschluss einer langen Aufgabe oder wenn eine Entscheidung nötig ist); man kann es im Prompt auch explizit verlangen ("notify me when the tests finish"). Einrichtung: Claude-App installieren, mit demselben Account anmelden, Benachrichtigungen erlauben, dann in `/config` **Push when Claude decides** und/oder **Push when actions required** aktivieren. [offiziell] https://code.claude.com/docs/en/remote-control
- Fehlerbehebung: „No mobile registered" → App einmal öffnen; iOS Fokus-Modi/Zusammenfassungen können Pushes unterdrücken; Android-Akku-Optimierung kann Zustellung verzögern. Über `CLAUDE_CLIENT_PRESENCE_FILE` lässt sich das Unterdrücken von Pushes bei Anwesenheit auf jedes Fenster ausweiten (nicht nur das fokussierte Terminal). [offiziell] ebenda

### Sitzungen teilen
- Team/Enterprise: Sichtbarkeit **Privat** oder **Team** (sichtbar für Org-Mitglieder); Claude-in-Slack-Sessions sind automatisch „Team"-sichtbar. Repository-Zugriffsprüfung standardmäßig aktiv.
- Max/Pro: Sichtbarkeit **Privat** oder **Öffentlich** (sichtbar für alle claude.ai-Nutzer!) — Repository-Zugriffsprüfung standardmäßig AUS. Warnung der Doku: vor dem Teilen auf sensible Inhalte/Credentials prüfen. Einstellbar unter Settings > Claude Code > Sharing settings. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web

### Subagents, Plan-Modus, Modelle in der Cloud
- Subagents funktionieren wie lokal — Claude kann sie über das Agent-Tool spawnen, aus `.claude/agents/` im Repo werden sie automatisch erkannt. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Agent Teams sind standardmäßig AUS, aktivierbar über Umgebungsvariable `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`. [offiziell] ebenda
- Berechtigungsmodi in Cloud-Sessions: nur **Auto** (Klassifikator statt Rückfrage, wenn Org es erlaubt), **Accept edits**, **Plan** — es gibt **kein Manual/Default- und kein Bypass-Modus** wie lokal. [offiziell] https://code.claude.com/docs/en/web-quickstart
- Modellwahl per `/model sonnet` (Argument statt Terminal-Picker), ebenso `/effort`, `/color`, `/rename` (erfordern Claude Code ≥ v2.1.205 in der Session-Umgebung). `/fast` schaltet Fast Mode um (≥ v2.1.271). [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Kontext-Management: `/compact` und `/context` funktionieren, `/clear` NICHT (stattdessen neue Session aus der Sidebar starten). Auto-Compaction wird durch die Session selbst über `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` gesteuert — eigene Umgebungsvariable dafür wird überschrieben. [offiziell] ebenda

### Was NICHT geht — kein GUI/Emulator/lokales Netz
- Cloud-Sessions laufen headless in Anthropic-verwalteten VMs, standardmäßig ohne Zugriff auf lokales Netz/Gerät/adb. Netzwerk ist je Environment konfigurierbar (Trusted/Custom/Full/None), Standard „Trusted" erlaubt nur eine Allowlist (Paketregistries, Cloud-APIs etc.). [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- **Kein Browser/keine visuelle Frontend-Verifikation in Cloud-Sessions**: laut einem offenen (als „not planned" geschlossenen) GitHub-Issue können Cloud-Sessions keine Screenshots machen, keine UI-Flows durchklicken und keine Konsolenfehler einer gerenderten Seite lesen. Drei technische Blocker werden genannt: Playwright/Puppeteer scheitern (Proxy unterstützt kein HTTPS-CONNECT), Chromium-Binary-Downloads scheitern (CDN nicht in der Allowlist), headless Chromium bekommt trotz vollem Netzwerkzugriff `ERR_CONNECTION_RESET`. [extern, UNSICHER bzgl. Datum/aktuellem Status] https://github.com/anthropics/claude-code/issues/75632 — Hinweis: Der Fetch-Assistent gab ein zweifelhaftes Datum aus („8. Juli 2026, Zukunftsdatum in der Simulation"); das Issue selbst und sein Inhalt wirken aber plausibel und passen zur offiziellen Architektur-Doku (headless VM, kein Browser erwähnt). Nicht als hartes Datum verwenden.
- Für Aufgaben, die lokale Config, Tools, Emulatoren oder Umgebung brauchen, empfiehlt die offizielle Doku ausdrücklich **lokale Session** oder **Remote Control** statt Cloud-Session. [offiziell] https://code.claude.com/docs/en/web-quickstart
- Cloud-Sessions benötigen zwingend GitHub für Klonen/PR — GitLab/Bitbucket/sonstige Repos können nur als lokales Bundle hochgeladen werden (`CCR_FORCE_BUNDLE=1`), können dann aber NICHT zurückgepusht werden. Selbstgehostete GitHub-Enterprise-Server werden nur für Team/Enterprise unterstützt. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web

### Remote Control (Unterschied zu Cloud-Sessions)
- Remote Control lässt eine **lokale** Session vom Handy/Web aus steuern — Code-Ausführung und Dateizugriff bleiben auf der eigenen Maschine; Web/Mobile sind nur ein „Fenster" hinein. Voraussetzung: Pro/Max/Team/Enterprise (keine API-Keys), lokaler `claude`-Prozess muss laufen bleiben (bei SSH ggf. in `tmux`/`screen`). [offiziell] https://code.claude.com/docs/en/remote-control
- Start: `claude remote-control` (Server-Modus, mehrere gleichzeitige Sessions bis `--capacity` Standard 32), `claude --remote-control`/`--rc` (interaktive Session), `/remote-control`/`/rc` (aus laufender Session heraus), auch aus Desktop-App/VS-Code. [offiziell] ebenda
- Trusted Devices (Beta): bindet Remote-Control-Zugriff an ein verifiziertes Gerät + Anmeldung < 18 Std, biometrische Bestätigung (Face ID/Touch ID/Windows Hello/Passkey); für Team/Enterprise durch Owner aktivierbar, für Pro/Max selbst aktivierbar. [offiziell] ebenda
- Vergleichstabelle „Dispatch / Remote Control / Channels / Slack / Self-hosted environments / Scheduled tasks" zeigt unterschiedliche Trigger, Ausführungsort und Einsatzzweck. [offiziell] ebenda

### Geplante/zeitgesteuerte Aufgaben — Routines
- Eine **Routine** ist eine gespeicherte Konfiguration (Prompt + Repos + Connectors), läuft auf Anthropic-Cloud-Infrastruktur (oder selbstgehostet), Erstellung/Verwaltung unter claude.ai/code/routines, in Desktop-App oder per `/schedule` (Alias `/routines`) in der CLI. Verfügbar für Pro, Max, Team, Enterprise. **In Research Preview** — Verhalten/Limits können sich ändern. [offiziell] https://code.claude.com/docs/en/routines
- Drei Trigger-Typen, auch kombinierbar: **Scheduled** (wiederkehrend: stündlich/täglich/werktags/wöchentlich, oder einmalig zu festem Zeitpunkt), **API** (POST an Routine-eigene URL mit Bearer-Token), **GitHub** (Events wie Pull Request/Release, mit Filtern). [offiziell] ebenda
- **Granularität**: Mindestintervall ist eine Stunde; feinere Cron-Ausdrücke werden abgelehnt. Zeitplanung ist lokale Zeitzone → automatisch nach UTC konvertiert. Ganze-Stunde-Termine (z. B. 9:00) können mehrere Minuten verspätet starten — Empfehlung: z. B. 9:07 wählen. [offiziell] ebenda
- Einmalige Läufe (One-off, z. B. `/schedule tomorrow at 9am, ...`) zählen NICHT gegen das tägliche Routine-Run-Kontingent, deaktivieren sich nach dem Lauf automatisch. [offiziell] ebenda
- Routines laufen völlig autonom als vollwertige Cloud-Sessions — **kein Permission-Mode-Picker**, keine Rückfrage außer bei bestimmten Artifact-Aktionen. Wichtig: Prompt muss selbsterklärend/vollständig sein, da die Session ihn als zugewiesene Aufgabe behandelt (nicht als untrusted Content). [offiziell] ebenda
- Routines gehören zum **individuellen** Konto, werden NICHT mit Teammitgliedern geteilt, zählen gegen das eigene tägliche Kontingent. Aktionen über GitHub/Connectors erscheinen als der Ersteller (Commits/PRs unter dessen GitHub-User). [offiziell] ebenda
- GitHub-Trigger: Claude GitHub App muss installiert sein; unterstützte Events sind Pull Request (opened/closed/assigned/labeled/synchronized/…) und Release; Filter nach Author, Title, Body, Base/Head Branch, Labels, Is draft, Is merged, mit Operatoren (equals/contains/starts with/is one of/matches regex). Webhook-Events unterliegen während der Preview stündlichen Caps pro Routine/Account. [offiziell] ebenda
- API-Trigger: `/fire`-Endpoint unter `experimental-cc-routine-2026-04-01` Beta-Header, Token wird nur einmal angezeigt; übermittelter `text` kommt als klar als „untrusted" markierter `<routine-fire-payload>`-Block an — die gespeicherte Routine-Prompt muss explizit darauf verweisen, sonst wird der Text ignoriert. [offiziell] ebenda
- Bei fehlendem/abgelaufenem GitHub-Zugriff pausiert eine Routine bis zu 72 Std., danach schaltet sie sich selbst ab. [offiziell] ebenda
- Nutzung/Limits: Routines ziehen normales Abo-Kontingent wie interaktive Sessions, PLUS ein tägliches Cap an Routine-Läufen pro Konto (genaue Zahl nicht in der Doku genannt, einsehbar unter claude.ai/code/routines). Bei erreichtem Cap/Limit: mit aktivierten „Usage Credits" läuft es gegen Metered Overage weiter, sonst werden weitere Läufe bis zum Reset abgelehnt. [offiziell] ebenda
- Alternativen zu Cloud-Routines: `/loop` + Cron-Tools für session-gebundenes Polling (verfällt beim Beenden, wiederkehrende Aufgaben löschen sich nach 3 Tagen selbst), Desktop Scheduled Tasks (persistent, laufen lokal solange die App offen ist). [extern, aus Sekundärquellen zusammengefasst] https://makerkit.dev/blog/tutorials/claude-code-routines-guide sowie offizielle Verweise auf `/docs/en/scheduled-tasks` und `/docs/en/desktop-scheduled-tasks`

### Nutzungslimits und Kosten
- **Cloud-Sessions teilen sich das Ratenlimit mit der gesamten übrigen Claude-/Claude-Code-Nutzung im Account.** Parallele Aufgaben verbrauchen das Kontingent proportional stärker. **Es gibt keine separate Abrechnung für die Cloud-VM selbst** — nur die Token-Nutzung zählt. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Zeitlimits: Setup-Skripte werden nur gecacht, wenn sie in ca. 5 Minuten fertig sind; Befehle/SessionStart-Hooks haben änderbare Standard-Timeouts. [offiziell] ebenda
- Pläne: Pro ($20/Monat, $17 bei Jahreszahlung), Max ($100 für 5×, $200 für 20× Pro-Nutzung), Team ($20/Nutzer/Monat Standard-Seat bei Jahreszahlung, Premium-Seat $100/Nutzer/Monat mit 5× Standard-Nutzung), Enterprise nach Vertrag. [extern, Sekundärquellen — Preise können sich geändert haben] https://www.cloudzero.com/blog/claude-code-pricing/ — für Aktualität siehe claude.com/pricing (offiziell verwiesen, aber nicht direkt abgerufen)
- Für Teams/Enterprise: Nutzung zieht aus einem Pro-Sitzplatz-Kontingent (rollierendes 5-Stunden- + Wochenfenster), geteilt mit Claude Chat/Cowork; „Usage Credits" erlauben Weiterarbeit gegen Metered Overage nach Limit-Erreichen. [offiziell] https://code.claude.com/docs/en/costs
- Durchschnittliche Enterprise-Kosten: ca. 13 $/Entwickler/aktiver Tag, 150–250 $/Entwickler/Monat, unter 30 $/Tag für 90 % der Nutzer. [offiziell] https://code.claude.com/docs/en/costs
- Organisations-IP-Allowlisting: Wenn aktiv, schlagen ALLE Anthropic-gehosteten Cloud-Sessions (auch Code Review und cloud-geroutete Routines) mit Auth-Fehler fehl, da sie von Anthropic-Infrastruktur, nicht dem eigenen Netz aus telefonieren — Ausnahme via Anthropic-Support nötig. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web

### Sicherheit / Isolation
- Jede Cloud-Session läuft in einer isolierten, Anthropic-verwalteten VM (oder auf selbstgehosteter Infrastruktur der Organisation). Netzwerkzugriff ist standardmäßig eingeschränkt und konfigurierbar; auch bei deaktiviertem Netzwerkzugriff kann die Session weiterhin mit der Anthropic-API kommunizieren (potenzieller Datenabfluss-Pfad). Git-Credentials/Signierschlüssel bleiben außerhalb der Sandbox, ein Proxy authentifiziert stellvertretend. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web

### Auto-Fix für Pull Requests
- Claude kann eine PR überwachen und automatisch auf CI-Fehler und Review-Kommentare reagieren (erfordert installierte Claude GitHub App). Aktivierbar über claude.ai/code (CI-Statusleiste), `/autofix-pr` im Terminal, per Zuruf in der Mobile-App, oder durch Einfügen der PR-URL. Bei mehrdeutigen Review-Kommentaren fragt Claude nach, statt selbstständig zu handeln; bei Merge-Konflikten (kein GitHub-Webhook dafür) reagiert Auto-Fix NICHT automatisch — Rebase muss man manuell anstoßen. **Warnung**: Auto-Fix antwortet unter dem eigenen GitHub-Account auf Kommentare, was bei kommentar-getriggerter Automation (Atlantis, Terraform Cloud, Custom Actions) ungewollte Workflows auslösen kann. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web

### Offizielle Best Practices (aus Blog/Doku)
- Cloud-Sessions eignen sich laut offiziellem Blog gut für: Fragen zum Projektaufbau/Repo-Struktur, Bugfixes und routinemäßige, gut abgegrenzte Aufgaben, Backend-Änderungen mit testgetriebener Verifikation. [offiziell] https://claude.com/blog/claude-code-on-the-web
- Aus dem Quickstart: Cloud-Sessions passen gut zu parallelen unabhängigen Aufgaben, Repos die man nicht lokal hat, Aufgaben die keine häufige Steuerung brauchen, sowie Code-Fragen/Exploration ohne lokalen Checkout. Für Arbeit, die lokale Config/Tools/Umgebung braucht: lieber lokal oder Remote Control. [offiziell] https://code.claude.com/docs/en/web-quickstart
- Aufgaben möglichst präzise formulieren: konkrete Datei/Funktion nennen, Fehlerausgabe einfügen, erwartetes Verhalten statt nur Symptom beschreiben. [offiziell] ebenda
- Review-Workflow: Diff-Ansicht (Vergleich gegen Basis-Branch, umstellbar), Inline-Kommentare sammeln sich und werden mit der nächsten Nachricht gebündelt gesendet, danach PR erstellen (voll/Draft/GitHub-Compose). Session bleibt nach PR-Erstellung aktiv für weitere Iteration. [offiziell] ebenda
- URL-Prefill für Integrationen: `claude.ai/code?prompt=...&repositories=owner/repo&environment=...` — nützlich z. B. für einen Button im Issue-Tracker. [offiziell] ebenda
- CLAUDE.md wird bei jedem Session-Start geladen — Empfehlung, sie unter ~200 Zeilen zu halten und detaillierte/spezialisierte Workflow-Anweisungen stattdessen in Skills auszulagern (spart Kontext/Kosten). [offiziell] https://code.claude.com/docs/en/costs

## BEST-PRACTICES-KANDIDATEN:
- Vor `claude --cloud` immer erst lokale Commits pushen — die Cloud-VM klont den GitHub-Remote-Branch, nicht den lokalen Checkout. Quelle: https://code.claude.com/docs/en/claude-code-on-the-web
- Für komplexe Aufgaben zuerst lokal im Plan-Modus (`claude --permission-mode plan`) das Vorgehen abstimmen, Plan committen/pushen, dann autonom per `--cloud` ausführen lassen. Quelle: ebenda
- Aufgaben für Cloud-Sessions möglichst eng abgrenzen (konkrete Datei/Funktion, Fehlerausgabe, erwartetes Verhalten) statt vage Prompts — spart Kontext/Iterationen. Quelle: https://code.claude.com/docs/en/web-quickstart
- CLAUDE.md kurz halten (< 200 Zeilen), spezialisierte Workflows in Skills auslagern, da CLAUDE.md bei jedem (Cloud-)Session-Start voll in den Kontext geladen wird. Quelle: https://code.claude.com/docs/en/costs
- Für Frontend-/GUI-lastige Aufgaben, die visuelle Verifikation brauchen (Screenshots, Klick-Flows, Konsole), keine Cloud-Session nutzen, sondern lokale Session oder Remote Control — Cloud-Sessions haben keinen Browser. Quelle: https://code.claude.com/docs/en/web-quickstart + GitHub-Issue #75632
- Bei Routine-Prompts, die auf `text` aus API-Trigger reagieren sollen, den `<routine-fire-payload>`-Block im Prompt explizit referenzieren, sonst wird der übergebene Text ignoriert (Sicherheitsmechanismus gegen Prompt-Injection über geleakte Tokens). Quelle: https://code.claude.com/docs/en/routines
- Bei geplanten Routines Startzeit leicht nach der vollen Stunde legen (z. B. 9:07 statt 9:00), da Termine „auf die Minute" verspätet starten können. Quelle: ebenda

## BUG-KANDIDATEN:
- Symptom: In Cloud-Sessions (claude.ai/code, headless VM) sind Browser-basierte Verifikationen von Frontend-Änderungen nicht möglich — kein Screenshot, keine Klick-Flows, keine Konsolenfehler aus gerenderter Seite. Zusätzlich scheitern eigene Versuche, Browser-Automatisierung selbst zu installieren/nutzen: Playwright/Puppeteer scheitern am Proxy (kein HTTPS-CONNECT-Support), Chromium-Binary-Downloads scheitern (CDN nicht in Netzwerk-Allowlist), headless Chromium erhält trotz vollem Netzwerkzugriff `ERR_CONNECTION_RESET`.
  Ursache: Architektur-Entscheidung (headless VM, eingeschränkter Netzwerk-Proxy ohne CONNECT-Tunneling, restriktive Default-Allowlist).
  Version: gemeldet unter Claude Code / Cloud-Sessions, genaues Meldedatum unsicher (siehe Hinweis oben).
  Workaround: Für visuelle Frontend-Prüfung lokale Session oder Remote Control nutzen statt Cloud-Session; ggf. Netzwerk-Environment auf „Full" statt „Trusted" umstellen, falls das den Chromium-Download/-Start ermöglicht (in der Doku nicht als offizieller Fix bestätigt).
  Status: Als „not planned" geschlossenes GitHub-Issue — d. h. laut Repo-Maintainern aktuell keine Änderung geplant.
  Quelle: [extern] https://github.com/anthropics/claude-code/issues/75632 (Inhalt plausibel und konsistent mit offizieller Architektur-Doku, aber Datum/Aktualität der Fetch-Zusammenfassung unsicher — bei Bedarf Issue direkt im Browser gegenprüfen)

## OFFEN/UNSICHER:
- Ob reguläre (Nicht-Premium) Enterprise-Standard-Seats Cloud-Sessions nutzen können, bleibt laut Doku unklar — genannt werden nur „Premium-Seats oder Chat + Claude Code-Seats". Nicht abschließend verifiziert.
- Die genaue Zahl des täglichen Routine-Run-Caps pro Account wird in der offiziellen Doku nicht beziffert (nur „siehe claude.ai/code/routines" bzw. „siehe claude.ai/settings/usage"). Nicht direkt abrufbar ohne eingeloggten Account.
- Aktuelle, exakte Preise für Pro/Max/Team/Enterprise stammen teils aus Sekundärquellen (cloudzero.com, Stand vermutlich 2026) und wurden nicht direkt von claude.com/pricing verifiziert — können sich geändert haben.
- Das GitHub-Issue #75632 zu fehlendem Browser in Cloud-Sessions: Datum und aktueller Bearbeitungsstatus über den WebFetch-Tool-Zusammenfassungspfad unsicher wiedergegeben (Tool nannte ein möglicherweise fehlerhaftes „Simulationsdatum"). Für belastbare Aussagen das Issue direkt im Browser/GitHub-CLI gegenprüfen.
- Keine offizielle, explizite Aussage gefunden, dass Cloud-Sessions grundsätzlich „kein GUI/keine Emulatoren/kein lokales Netz/Gerät/adb" unterstützen — dies ergibt sich indirekt aus der Architektur (headless VM, konfigurierbarer aber grundsätzlich eingeschränkter Netzwerkzugriff, keine Erwähnung von Display/Emulator-Unterstützung) plus dem externen GitHub-Issue, nicht aus einer einzelnen offiziellen Zusammenfassungsseite.
