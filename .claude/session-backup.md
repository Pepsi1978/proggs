# Session Handoff — 2026-06-18, ~14:50

## Ziel (1-3 Saetze)
Konzeptionelles Gespraech (KEINE Code-Aufgabe): Frank will OpenCode ein praktisch
"unendliches" externes Gedaechtnis geben — Wissen extern lagern, nur bei Bedarf gezielt +
verdichtet ins CLI laden (Token sparen, Context Rot vermeiden). Themen: MCP-Server bauen,
Vektordatenbanken verstehen, "Hermes Agent" (und ANDERE Agent-Services) als moegliche fertige
Bausteine. Ein TVO-Prompt ist bereits im Chat ausgegeben. Frank will exakt an diesem
Gespraechsstand weitermachen.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)
- **Welche Aufgabe lief gerade:** Reines Konzept-/Beratungsgespraech. Frank hatte eine fruehere
  Session verloren und den alten Prompt + meine alte Antwort reinkopiert. Ich habe (1) einen
  ausfuehrlichen TVO-Wiedereinstiegs-Prompt geschrieben, (2) meine ehrliche Meinung zur Idee
  gegeben, (3) sein MCP-Verstaendnis korrigiert, (4) "Hermes Agent" per WebSearch verifiziert.
  KEIN uncommitteter Code, KEINE Datei geaendert.
- **Wo genau unterbrochen — der allerletzte Schritt:** Nach meiner Hermes-auf-Hostinger-Antwort
  hat Frank "session backup" verlangt und DANN per Nachricht den Scope erweitert (siehe unten,
  "Franks NEUER Wunsch").
- **Schon erledigter Teil:** TVO-Prompt v1 ist im Chat (Frank speichert ihn selbst im Terminal
  Voice Overlay). Meinung + MCP-Korrektur + Hermes-Verifikation sind raus.
- **Noch offener Teil / Franks NEUER Wunsch (WICHTIG — so weitermachen):**
  Frank will am konzeptionellen Gespraech genau hier weitermachen und im NACHHINEIN:
  1. Genau herausfinden, WAS Hermes ist und WIE Hermes funktioniert (tiefer als bisher).
  2. Pruefen, ob es BESSERE Agent-Moeglichkeiten gibt als Hermes. Hostinger bietet nicht nur
     Hermes als Ein-Klick-Docker an, sondern auch ANDERE Services, die dieselbe Arbeit machen
     koennen.
  3. Also: SAEMTLICHE Ein-Klick-Docker-Agent-Systeme von Hostinger durchgehen/vergleichen und
     den RICHTIGEN auswaehlen — Auswahlkriterium: am besten geeignet fuer eine GROSSE Datenbank
     im Hintergrund, in der ALLES (Wissen ueber Frank, Rules, Bug-Almanache, Best Practices,
     Memories) gespeichert wird.
- **So geht es EXAKT weiter (allererste Aktion der neuen Session):** Das Gespraech fortsetzen mit
  einer RECHERCHE (WebSearch, ggf. 3-5 researcher parallel): (a) tiefer erklaeren was Hermes
  Agent genau ist + wie es funktioniert (Memory-Mechanik!), (b) den vollstaendigen Hostinger
  Application-Catalog / Docker-Templates nach Agent-/Memory-/AI-Systemen durchsuchen, (c) die
  Kandidaten vergleichen mit Fokus "grosse Hintergrund-DB als Gedaechtnis" und sie Frank
  gegenueberstellen, damit er den richtigen auswaehlt. Nicht raten — recherchieren und
  strukturiert (Tabelle) praesentieren.
- **Was dafuer alles vorhanden sein muss:** Inhaltlicher Stand steht unten (Recherche-Ergebnisse,
  Entscheidungen, Pruefpunkte). Research-Persistenz beachten: taugliche Ergebnisse in
  best-practices/ + bugs/ einarbeiten.
- **Uncommitteter Arbeitsstand:** KEINER (reines Gespraech, nichts editiert).
- **Danach:** Wenn ein Service gewaehlt ist UND Frank bauen will -> brainstorming-Skill.

## Aktueller Status
- Erledigt: TVO-Prompt v1 ausgegeben; Meinung abgegeben; MCP-Missverstaendnis korrigiert;
  "Hermes Agent" per WebSearch als reales Produkt (Nous Research, via Hostinger VPS) bestaetigt.
- In Arbeit: Service-Auswahl — Hermes vs. andere Hostinger Ein-Klick-Docker-Agent-Systeme
  (Franks neuer Wunsch, NOCH NICHT recherchiert).
- Blockiert: nichts.

## Relevante Dateien
- (Keine geaenderten Dateien.) Bestands-Kontext fuers Thema:
  `~/.config/opencode/opencode.jsonc`, `~/.config/opencode/AGENTS.md`,
  `~/.cache/opencode/models.json` — Franks echtes OpenCode-Setup (fuer spaeteres Bauen lesen).
- Bug-Almanach (Bezug): `bugs/opencode/opencode-cli.md`, `bugs/claude-tooling/mcp-server.md`.

## Getroffene Entscheidungen (damit die neue Session nicht zurueckrudert)
- Kernprinzip ist RICHTIG: just-in-time retrieval / Agentic RAG / progressive disclosure.
  Franks Bug-Almanach-Digest ist im Kleinen schon genau dieses Muster.
- Ehrliche Bewertung: Idee zu ~80% stark, ~20% Falle. Konkret:
  * Vektor-DB ist NICHT automatisch noetig — bei exakten Namen/Versionen/Pfaden ist Grep besser
    (deckt sich mit Regel semantic-search-before-agents). Hybrid (Datei+Grep fuers Exakte,
    semantisch nur fuers Unscharfe) oft besser.
  * Der "rund um die Uhr mitschneidende Agent" (Push) ist die schwaechste Idee — teuer + kann
    Kontext verschmutzen. "Pull" (Agent fragt bei Bedarf) schlaegt "Push".
  * Eigentliche harte Arbeit ist das CHUNKING (wie Wissen zerschnitten wird), nicht die DB-Wahl.
- MCP-Verstaendnis-Korrektur (wichtig): Ein MCP-Server ist KEINE KI. Er ist ein "dummes" Programm,
  das Tools bereitstellt. Die KI sitzt im CLI (OpenCode-Modell) und formuliert die Anfrage.
  Such-Intelligenz kommt entweder aus mechanischer Suche (kein 2. Modell) ODER aus einem bewusst
  dazugebauten kleinen Modell (Sub-Agent), das zusammenfasst.
- Empfohlene Bau-Reihenfolge: erst schlanker Memory-MCP-Server mit On-Demand-Tool (Variante A),
  Push-Dauer-Agent zunaechst WEGLASSEN. Lokal starten statt sofort Internet (Datenschutz).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- Keine echten Fehlversuche im Gespraech. ABER Denkfallen, die NICHT wiederholt werden duerfen:
  * NICHT bestaetigen, dass "MCP-Server = KI" ist (haeufiges Missverstaendnis, bereits korrigiert).
  * NICHT Owl Alpha als Memory-/Sub-Agent-Modell vorschlagen — es loggt Prompts/Completions fuers
    Training -> NICHT fuer private/sensible Daten. Owl Alpha nur fuer harmlose Wegwerf-Aufgaben.
  * NICHT zum sofortigen VPS-Kauf raten, bevor die 3 Pruefpunkte geklaert sind UND der beste
    Service ausgewaehlt ist.
- TECHNISCH (dieser Backup-Lauf): Bash-Heredoc mit Apostrophen im Text ("Hermes' ...") scheiterte
  mit "unexpected EOF while looking for matching '". Loesung: Write-Tool statt Heredoc verwenden.

## Wichtige Recherche-Ergebnisse
- "Hermes Agent" (Nous Research): selbst-verbessernder KI-Agent mit PERSISTENTEM Gedaechtnis,
  das mit der Zeit waechst. Genau Franks "Hermes"-Idee, aber als fertige Software.
- Hostinger bietet ihn per Ein-Klick-Docker (Application Catalog / Docker Manager): Template
  waehlen, LLM-API-Key einfuegen, deployen -> in <15 Min live auf oeffentlicher IP. End-to-end
  ~35 Min laut Test.
- Unterstuetzt OpenRouter, OpenAI, Anthropic, custom LLM endpoints. Gateway-Modus zu Telegram,
  Discord, Slack, WhatsApp, Signal, E-Mail. Web-Browsing, Code-Ausfuehrung, Multi-Agent.
- SELF-HOSTED = API-Keys, Verlauf, Kontext bleiben auf Franks eigener Infrastruktur
  (loest die Owl-Alpha-Datenschutzsorge).
- Kosten: Hostinger KVM ab ~4.99 USD/Monat (24-Mon-Term), Verlaengerung +140-230% -> real
  ~11-15 USD/Monat. Always-on kostet laufend.
- Es gibt weitere Hostinger Ein-Klick-Docker-Vorlagen rund um Hermes (in der Suche gesehen:
  "Hermes Workspace", "Hermes WebUI") UND generell andere AI-Agent-Templates -> GENAU DIE noch
  systematisch vergleichen (Franks neuer Wunsch).
- Quellen: hostinger.com/tutorials/what-is-hermes-agent ;
  hostinger.com/applications/hermes-agent ; hostinger.com/applications/hermes-workspace ;
  hostinger.com/vps/docker/hermes-webui ;
  hostinger.com/support/how-to-get-started-with-hermes-agent-on-hostinger-vps ;
  hostadvice.com Hostinger Hermes Agent VPS Review 2026 ;
  xcloud.host/best-hermes-agent-hosting-providers (Provider-Vergleich).
- 3 OFFENE PRUEFPUNKTE (vor VPS-Kauf klaeren, gelten fuer JEDEN gewaehlten Service):
  1. Wie laedt man Franks FERTIGES Wissen (Rules, Almanache, Best Practices, Notizen) in das
     Gedaechtnis des Agenten? (Hermes lernt primaer aus eigener Erfahrung — externes Wissen
     einspielen ist die offene Frage. Genau das braucht Frank aber.)
  2. Ist der Agent per MCP/API von OpenCode aus ansprechbar? (Hermes ist primaer fuer Messenger
     gebaut — MCP-Bruecke muss verifiziert werden.)
  3. Kosten/Datenschutz bewusst akzeptieren (monatlich; self-hosted-Vorteil nutzen).

## Naechste Schritte (priorisiert)
1. RECHERCHE starten (WebSearch / mehrere researcher parallel):
   (a) Hermes genauer: was ist es, wie funktioniert das Memory wirklich?
   (b) Vollstaendige Liste der Hostinger Ein-Klick-Docker-Agent-/AI-/Memory-Systeme.
   (c) Vergleich der Kandidaten mit Fokus "grosse Hintergrund-DB als Gedaechtnis" + Eignung,
       externes Wissen einzuspielen + MCP/API-Ansprechbarkeit.
2. Ergebnis als Vergleichstabelle praesentieren, damit Frank den richtigen Service auswaehlt.
3. Optional: "Was ist eine Vektordatenbank + Datenbank-Typen" einfach erklaeren (Franks
   genannte Wissensluecke).
4. Falls Service gewaehlt + bauen: brainstorming-Skill starten (was kommt ins Gedaechtnis?
   Datei vs Vektor? welches guenstige NICHT-loggende Modell? Variante A zuerst).
5. Research-Persistenz: taugliche Funde in best-practices/ + Bugs in bugs/ einarbeiten.

## Offene Fragen
- Welcher Hostinger Ein-Klick-Docker-Service ist der beste fuer eine grosse Hintergrund-DB als
  Memory? (Hermes vs. Alternativen — noch zu recherchieren.)
- Hermes/Hostinger-Fertigservice ODER kompletter Eigenbau (Variante A)? Haengt an den 3 Pruefpunkten.
- Will Frank zusaetzlich die Vektordatenbank-Grundlagen erklaert bekommen?

## Anker
- Branch: main
- Letzte Commits:
2b04ed83f #46893 - resilient-bugfixing skill: load full directive text on activation instead of short version
03e3085a4 #NNN - bugs: OpenRouter/OpenCode — correct stealth-model fix (status:alpha is filtered; needs own provider)
e4bd9a8cc #NNN - bugs: OpenRouter/OpenCode §40 — manually register stealth models (owl-alpha)
39720a3f5 #46892 - bug-almanach opencode-cli: researched + curated (94 entries, OpenCode v1.17.8)
27b04b3e2 UI: Lautsprecher in Orange, Kalender-Buttons entfernt
