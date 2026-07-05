# Session Handoff — 2026-07-05, 03:14 Uhr

## Ziel (1-3 Saetze)
Cortex/Second-Brain-Ausbau. Diese Nacht-Session hat den NACHTSCHICHT-BIBLIOTHEKAR komplett gebaut
(Plan Gruppe B, Bereiche 11-18 + Nachzuegler-Bonus + eigene Aufgaben) und in derselben Nacht auf
Franks Zuruf stark verfeinert. Alles ist deployt, verifiziert und committed (#47496-#47507).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Franks Fahrplan fuer die naechste
Session steht unter "Naechste Schritte"; vollstaendiger Projekt-Kontext liegt in der Memory
project_nachtschicht_bibliothekar.md (wird automatisch geladen).

## Aktueller Status
- Live auf dem VPS (alles healthy): librarian 0.6.0 · agent 0.52.0 · brain-api 1.22.0 ·
  dashboard 0.43.1. Commits #47496-#47507, alle gepusht.
- Bibliothekar komplett: eigener Container sb-librarian (10.8.0.1:8004), Nachtlauf 04:10
  Europe/Berlin (NTP-verifiziert, wartet aufs Host-Backup), Bereiche 11-18 + Nachzuegler-Lauf,
  striktes Vorschlags-Prinzip (Loeschen nur via Papierkorb).
- Verfeinerungen derselben Nacht: gpt-5.5 als Nachtmodell + Thinking high (via agent POST /llm =
  ChatGPT-OAuth-Durchgriff, keine Auth-Duplikation); Ohne-Begrenzung-Modus (Default AN, Notbremse
  LIB_LLM_BACKSTOP=5000); Nacht-Bilanz (jede Aufgabe meldet auch "nichts gefunden"/"ausgeschaltet");
  Merge-Steuerung (Volltext-Editor + "Aenderung uebernehmen" + Kategorien-Union als abwaehlbare
  Chips -> Multi-Category); Lernen-Knopf an jedem Fund (Dialog -> finale Regel bestaetigen ->
  lernregeln.json -> Pflicht-Block in allen 10 Nacht-Urteils-Prompts) + Karte "Gelernte Regeln"
  (Kurzfassung, Bearbeiten-Editor, "KI fragen"-Selbstpruefung, an/aus, Papierkorb);
  Bibliothekar-Einstellungen mit AUTO-SAVE (kein Speichern-Knopf mehr); Einstellungen-Karte heisst
  "Agenten" (Modell+Thinking je Rolle nebeneinander); Uebersicht-Vitals 3s-Poll nur bei offener
  Uebersicht; Feature-Chronik auf 39 Eintraege nachgezogen.
- Erster manueller Testlauf (00:34-00:55): 716 Eintraege, 149 LLM-Calls, 0 Fehler, 60
  nachverknuepft, 15 OFFENE Funde im Tages-Report 2026-07-05 (8 Dubletten, 2 Widersprueche,
  5 Kategorien) — noch ohne Bilanz/Kategorien-Union (alte Version).
- NEUE Systemregel (Frank): Timestamps NIEMALS schaetzen — immer echte Uhr (date/Get-Date,
  Europe/Berlin). Verankert in ~/.claude/rules/timestamps-niemals-schaetzen.md + Repo-Spiegel +
  Memory + Second Brain (Programmierung/Rules).
- Projekt-Briefing fuer fremde KIs im Gehirn: Titel "Second Brain (Cortex) — Projektbeschreibung
  fuer KI-Modelle" [Programmierung/Projekte/Second Brain] — bei grossen Ausbauten mit gleichem
  Titel neu speichern.
- In Arbeit: nichts. Blockiert: nichts.

## Relevante Dateien
- second-brain-server/librarian/app.py — der komplette Nachtschicht-Dienst (Scheduler, Tasks, Reports, Lernregeln)
- second-brain-server/dashboard/static/index.html — Bibliothekar-Tab (Karten: Report -> Abarbeiten -> Gelernte Regeln -> Einstellungen -> Eigene Aufgaben)
- second-brain-server/dashboard/app.py — Proxy /api/lib/* (Whitelist inkl. learn), /api/vitals, VERSION
- second-brain-server/agent/app.py — POST /llm (LLM-Durchgriff fuer den Bibliothekar)
- second-brain-server/LEVEL2-FEATURES-PLAN.md — restliche Plan-Punkte: Gruppe A (1-10) + Gruppe C (19-26)
- second-brain-server/DEPLOY.md — Deploy-Weg (scp + compose --build, librarian in Diensttabelle, chown uid 1000)

## Getroffene Entscheidungen
- Eigener Container "librarian" (Franks Wahl) statt Nacht-Thread im Agenten.
- GPT via agent-/llm-Durchgriff statt OAuth-Logik zu duplizieren (Token-Refresh lebt nur im Agenten).
- Ohne-Begrenzung default AN; nur stille 5000-Call-Notbremse (ai-agent-Almanach 2.1 Enforcement).
- Ja-Knopf fuehrt IMMER exakt die angezeigte Empfehlung aus; Nein merkt sich der Fund 120 Tage.
- Gelernte Regeln als Pflicht-Block in ALLE Nacht-Urteile (nicht selektiv pro Task).
- Bibliothekar-Einstellungen speichern sich automatisch (Frank-Logik: Vergessen unmoeglich).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- Versions-Timestamps HANDGESCHAETZT -> liefen ~30 min voraus, Frank hat es sofort gesehen.
  NIE WIEDER: vor jedem Zeit-Eintrag date/Get-Date abfragen (neue Systemregel, immer geladen).
- features.json-Pflegepflicht bei schnellen Deploy-Serien fast vergessen — Sammel-Nachtrag am
  Session-Ende ist Pflicht (Frank musste erinnern).
- Nach einem EXTERNEN Python-Rewrite einer Datei verlangt das Edit-Tool erst ein frisches Read.
- Langlebiger SSH-Poll (Monitor ueber ~1h) bricht mit "Connection reset by peer" — kurze Einzel-
  Polls statt einer langen SSH-Session nutzen.
- SEHR grosse Bash-/Python-Heredocs (~9 KB) fuer das Backup brachen 2x mit Bash-Quoting-Fehler
  ("unexpected EOF while looking for matching quote") — grosse Inhalte per Read+Write-Tool
  schreiben (dieser Weg hier), NICHT per Heredoc durch die Shell schieben.
- Aus der Vorsession weiter gueltig: neue Laufzeit-Datei MUSS in die Dockerfile-COPY-Liste;
  git commit -- pfad scheitert bei NEUEN Dateien (erst git add).

## Wichtige Recherche-Ergebnisse
- memory-evolution-2026-Kurzcheck bleibt der Leitfaden: Konsolidierung asynchron (#2),
  Selbstorganisation nur als Vorschlag mit Bestaetigung (#13) — beides strikt eingebaut.

## Naechste Schritte (priorisiert)
1. Ersten AUTOMATISCHEN 04:10-Lauf pruefen: ssh -i ~/SK/second-brain/id_ed25519
   root@168.231.83.205 "docker logs sb-librarian --since 8h" + Morgen-Report/Bilanz im Dashboard
   (erster unbegrenzter gpt-5.5-Lauf; auch Codex-Token-Verbrauch grob einordnen).
2. Die 15 offenen Funde mit Frank abarbeiten = Live-Test von Starten/Rueckfragen/Merge-Editor/
   Lernen-Knopf. Franks angekuendigte erste Lernregel: "Kurzcheck + Vollversion NIEMALS
   zusammenfuehren" (am Fund 'Code-Check + Langversion', wartet offen).
3. Franks Ansage "die anderen Punkte alle noch einarbeiten": restliche Plan-Punkte aus
   LEVEL2-FEATURES-PLAN.md — Gruppe A Nr. 1-10 (Kern-Bloecke, Kurzzeit-Schicht, bi-temporale
   Fakten, ADD/UPDATE/NOOP, Recall-Verstaerkung, Decay, Provenance) + Gruppe C Nr. 19-26
   (Tagesbriefing, Wochenrueckblick, Kontext-Injektion, ntfy-Push). ERST Frank den Umfang/die
   Reihenfolge waehlen lassen (ggf. wieder erst erklaeren, er kennt die Punkte noch nicht im Detail).
4. Morgen-Report-Karte in die Cortex-Android-App (Server-API steht: 10.8.0.1:8004 /reports).
5. Offene, unbeantwortete Intelligenz-Vorschlaege: Token-Verbrauch in der Bilanz; Auto-Save auch
   fuer die "Agenten"-Karte; Lernregeln zusaetzlich ins Gehirn spiegeln; Teil-Bilanz bei
   Lauf-Abbruch; Poll-Pause im Bibliothekar-Tab; Stempel-Waechter-Hook; Chronik-Sammelnachtrag-
   Regel in DEPLOY.md; Projekt-Briefing nach Ausbauten aktualisieren.

## Offene Fragen
- Welchen Umfang will Frank fuer die naechste Ausbaustufe (Gruppe A 1-10 vs. Gruppe C 19-26 vs.
  Auswahl)? Erst erklaeren, dann waehlen lassen.
- Die offenen Intelligenz-Vorschlaege (siehe Schritt 5) hat Frank noch nicht beantwortet.

## Anker
- Branch: main
- Letzte Commits (vor dem Backup-Commit):
9d279004b #47507 - dashboard 0.43.1: feature chronicle caught up (6 neue System-Info-Eintraege)
d2c9d3365 #47506 - librarian 0.6.0 + dashboard 0.43.0: Gelernte Regeln editierbar + KI fragen, Karte zwischen Abarbeiten und Einstellungen
4a1ab94dc #47505 - librarian 0.5.0 + dashboard 0.42.0: Lernen-Knopf an jedem Fund, Regeln in alle Nacht-Urteile
06be93c1c #47504 - librarian 0.4.0 + dashboard 0.41.0: Merge-Text-Editor + Kategorien-Chips
8c97fcdc5 #47503 - librarian 0.3.0 + dashboard 0.40.1: Nacht-Bilanz (auch 'nichts gefunden')
