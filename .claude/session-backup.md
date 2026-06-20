# Session Handoff — 2026-06-20 (Abend)

## Ziel
Das Research-System (Recherche-Pipeline) so umbauen/erweitern, dass es FUER FRANK rund laeuft:
sichtbare parallele Researcher, saubere verstaendliche Auswertung, schnell, ein self-contained Skill.
MORGEN zuerst BRAINSTORMEN (kein Blind-Bau): Architektur-Entscheidung treffen + dann bauen.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
- **Welche Aufgabe lief gerade:** Frank gab ausfuehrliches Feedback, dass die erste echte Research in
  einer NEUEN Session "ueberhaupt nicht rund" lief. Wir wollten die Research-Erfahrung verbessern.
  Frank hat entschieden: NICHT heute bauen — MORGEN weitermachen, zuerst BRAINSTORMEN.
- **Kein halbfertiger Edit offen** — gesamte Research-Pipeline ist committet (bis #47023).
- **Allererste Aktion der neuen Session (MORGEN):** Mit Frank BRAINSTORMEN (superpowers:brainstorming
  oder gefuehrte Fragen) ueber die ARCHITEKTUR-KERNFRAGE (siehe "Offene Fragen"), DANN bauen. NICHT
  sofort losbauen — Frank will den besten Weg erst gemeinsam ergruenden.
- **Pending-Entscheidung (Frank per AskUserQuestion-Freitext):** "Wir machen das morgen weiter ... bau
  exakt diesen Skill, den wir generell fuer Recherchen einsetzen — ODER brainstormen nochmal, weil wir
  die anderen research skills auch noch haben (almanach-update, best-practices-update). Was passiert mit
  denen? Die muessen auch vernuenftig mit dem neuen System funktionieren. Frage: verbessern wir das
  bestehende System oder bauen wir was Neues dazu? Erst ergruenden." → MORGEN klaeren.
- **Bau-Werkzeug:** Der Skill MUSS via skill-creator-Skill gebaut werden (CLAUDE.md-Regel). Vorher
  brainstorming (Regel: "Lets build X" -> brainstorming first).

## DER SOLL-ZUSTAND (Franks Schmerzpunkte -> Pflicht-Fixes; ALLE umsetzen)
Frank will den research-Skill (oder das verbesserte System) genau so:
1. **Sichtbare parallele Researcher** — Thema in Unterthemen zerlegen, je ein Researcher klar mit seinem
   Unterthema BESCHRIFTET, Live-Fortschritt (Researcher 1: <Thema> -> laeuft… -> fertig). Mehrere
   gleichzeitig SICHTBAR und nachverfolgbar. (Heute war NICHT erkennbar, wer woran arbeitet.)
2. **Engine fest gesetzt UND angezeigt** (parallel, KEIN Exa). Heute kam "Exa statt parallel"-Text =
   Verwirrung. (Fakt: or-research.py Default ist bereits engine=parallel, Zeile 85 — es war die
   Erzaehlung, nicht die echte Engine. Trotzdem: Engine im Skill pinnen + sichtbar machen.)
3. **Kosten gedeckelt** — pro Researcher begrenzte Suchanzahl (OR_MAX_TOTAL=10), KEINE endlose agentische
   Schleife, KEIN Abbruch-Retry-Doppelpreis. Heute: Abbruch -> doppelte Kosten ~16 Cent. Laufende Kostenanzeige.
4. **Saubere, einfach erklaerte Auswertung** — KEINE "Striche hier und da". Feste Bloecke: Kurzfassung ->
   Das Wichtigste (nummeriert) -> Fuer deinen Einsatz -> Noch offen/unsicher. Optisch ruhig, gut erklaert
   (Research = neue Dinge -> klar erklaeren).
5. **Schnell** — parallele, gedeckelte Sub-Researches statt EINER langen Agentik-Schleife. (Heute sehr
   lang; die fruehen Einzeltests in der Vorsession waren "ganz schnell".)
6. **KEINE Testnachrichten/Selbsttests** ob das System funktioniert (auch nicht bei Eskalation) — System
   ist verifiziert, es wird direkt gearbeitet. Heute hat die Session "getestet ob Firecrawl geht" obwohl
   wir das vorher schon bewiesen hatten.
7. **Pfade fest eingebettet** im Skill (mm-research.py, or-research.py, Flag, Keys) — KEIN Suchen nach den
   Skripten. Heute: Session "suchte ob es ein Skript gibt". Ein Skill haette alle Adressen drin.
8. **Zwei-Stufen mit Auswertung nach JEDER Stufe:** nach Lauf 1 saubere Auswertung -> Frank entscheidet
   ueber Lauf 2 (Eskalation) -> danach wieder saubere Auswertung. Eskalation NUR nach klarer Auswertung
   + Entscheidung.

## Aktueller Status (alles committet, #47009-#47023)
- **ERLEDIGT (Pipeline gebaut + end-to-end live bewiesen):**
  - Regel ~/.claude/rules/research-strategy.md: Empfehlung VOR Frage 1 (Heuristik A/B/C + Kombi A->B),
    Frage 1 (A/B/C/D via AskUserQuestion), Frage 2 (Eskalation nach Firecrawl), §5-Tabelle aller
    Research-Skills. (#47009, #47018)
  - Hook research-approval.{ps1,sh} (PreToolUse, registriert): blockt mm/or-research + Firecrawl-MCP bis
    touch "$TEMP/research-approved.flag" (TTL 30min). Erkennt nur echte AUSFUEHRUNG (python/bash/./), nicht
    git/grep/cat/py_compile. raw-substring statt jq (#16.2), deny via permissionDecision (#16.1).
    Getestet (deny/allow/flag-roundtrip). (#47010, #47012)
  - Skripte: mm-research.py (Firecrawl /v1/search -> MiniMax M3 /messages, x-api-key, thinking
    enabled+budget=24000 — NICHT adaptive auf /messages!), or-research.py (OpenRouter web_search
    server-tool, Default engine=parallel, OR_MAX_TOTAL=10). Beide mit RESILIENTEM JSON-Parse (Keep-Alive
    ": OPENROUTER PROCESSING" ueberspringen): mm=_loads_resilient, or=_loads_or. (#47013, #47021)
  - A/B/C-Gate-Pointer in: best-practices, bug-almanach-recherche, almanach-update, best-practices-update,
    direktiven-recherche (Skill+Agent), superintelligenz, intelligence-researcher, forschungsagent,
    researcher-Agent. (Opus-Schwarm jeweils = Option C). (#47014-47016, #47018)
  - Almanach/Best-Practices: bugs+best-practices/apis/firecrawl.md + bugs/apis/openrouter-api.md
    (6 Server-Tools, #9 Keep-Alive, file-parser) + best-practices/apis/openrouter-api.md. (#47019,20,22,23)
- **Live bewiesen:** 3 echte Recherchen liefen erfolgreich durch (Firecrawl-Promo, Firecrawl-v2-BP,
  OpenRouter-Server-Tools). Credit-Beweis am 11k-Key: 11000 -> 10990 -> 10956.
- **Firecrawl-Key-Swap:** mm-research nutzt jetzt den 11k-OpenRouter-Team-Key in
  ~/SK/OpenCode/firecrawl-api-key.txt (~10.956 uebrig; Promo 10k verfaellt ~2026-09-20). Alter
  Barwandt-Key (1000/Mon) gesichert in ~/SK/OpenCode/firecrawl-api-key.barwandt.txt. fc-Keys team-scoped.
- **In Arbeit:** nichts offen. **Blockiert:** nichts.

## Relevante Dateien
- ~/.claude/rules/research-strategy.md (+ Spiegel claude-code-setup/rules/) — die Regel, Herzstueck.
- ~/proggs/mm-research.py — Firecrawl->MiniMax (Option A). ~/proggs/or-research.py — OpenRouter web_search (Option B).
- ~/.claude/hooks/research-approval.{ps1,sh} (+ Spiegel claude-code-setup/hooks/ + Umgebung/Hooks/).
- Research-Skills zum Andocken: ~/.claude/skills/{best-practices,bug-almanach-recherche,almanach-update,
  best-practices-update,direktiven-recherche}/SKILL.md + Agents {researcher,superintelligenz,
  direktiven-recherche,intelligence-researcher,forschungsagent}.md.
- bugs/apis/{firecrawl,openrouter-api}.md + best-practices/apis/{firecrawl,openrouter-api}.md.
- Evtl. Cowork-exklusiver research-Skill (laut cowork-portierung-Regel) — pruefen.

## Getroffene Entscheidungen (NICHT zurueckrudern)
- Vor JEDER Web-Recherche: Empfehlung (3-4 Zeilen, Heuristik) + Frage 1 (A/B/C/D AskUserQuestion),
  empfohlene Option zuerst + "(Empfohlen)". Nach Firecrawl-Lauf: Frage 2 (Eskalation?).
- A=Firecrawl+MiniMax M3 (max Thinking), B=MiniMax+parallel (or-research), C=Opus-Schwarm (nur explizit).
- MiniMax /messages (mm-research): thinking {type:enabled, budget_tokens:N} — adaptive NUR /chat/completions.
- or-research: engine=parallel Default, OR_MAX_TOTAL=10.
- Firecrawl key=team; 11k-Key jetzt aktiv; Promo-Ablauf ~2026-09-20 -> dann zurueck auf Barwandt-Key.
- Erledigte Tasks IMMER live abhaken (feedback_always_check_off_completed_tasks).

## Fehlgeschlagene Ansaetze / Lessons (NICHT wiederholen)
- Research-UX der neuen Session war schlecht (siehe SOLL-ZUSTAND) — alle 8 Punkte fixen.
- ": OPENROUTER PROCESSING" Keep-Alive bricht json.loads bei langer Verarbeitung -> resilient parse
  (beide Skripte haben es jetzt). Almanach openrouter-api.md #9.
- thinking {type:adaptive} auf Anthropic /messages ist FALSCH (nur /chat/completions) — /messages braucht
  enabled+budget (live-getestet). Plan-Fehler, in #47017 korrigiert.
- Hook-Erkennung darf nur AUSFUEHRUNG matchen, nicht blosse Erwaehnung (git add/grep/cat/py_compile) — gefixt #47012.
- 64-Hex-String ist KEIN Firecrawl-Key (fc-... noetig) — Keys vor Nutzung gegen /v2/team/credit-usage testen.
- Session-Backup-Heredoc bricht an Apostrophen (z.B. "Lets") wenn bash -c-gewrappt -> Write-Tool nutzen.

## Wichtige Recherche-Ergebnisse
- OpenRouter web_search: 6 Server-Tools (web_search, web_fetch[Engine openrouter=gratis], datetime,
  image_generation, apply_patch, fusion). Kosten web_search $0.005/Anfrage (bis 10 Treffer). engine=parallel.
- Firecrawl: /crawl Default-Limit 10.000 Seiten (Kostenfalle), JSON-Extraktion 5 Credits/Seite,
  Spar-Formate Question(100x weniger Token)/Highlights/monitor/lockdown. Free/Hobby = 2 concurrent.
- Firecrawl-Team-Guthaben pruefen: GET https://api.firecrawl.dev/v2/team/credit-usage (Bearer).
- urllib gegen Cloudflare: User-Agent "curl/8.5.0" setzen (sonst 403/1010).

## Naechste Schritte (priorisiert) — MORGEN
1. BRAINSTORMEN mit Frank: Architektur-Kernfrage (siehe Offene Fragen) — improve vs. neuer zentraler Skill.
2. Danach: research-Skill via skill-creator bauen, der ALLE 8 SOLL-Punkte erfuellt.
3. Andere Research-Skills sauber andocken (almanach-update, best-practices-update, best-practices,
   bug-almanach-recherche, researcher, direktiven-recherche, superintelligenz) — delegieren an neuen Kern
   oder Pointer behalten; im Brainstorming entscheiden.
4. Mockup-Optik umsetzen: sichtbare parallele Researcher + saubere Auswertungs-Bloecke.
5. Spiegeln (claude-code-setup + Umgebung) + committen.

## Offene Fragen (MORGEN zuerst klaeren — Architektur)
- **KERN:** Bestehendes System VERBESSERN (Regel + Skripte + Pointer) ODER neuen ZENTRALEN research-Skill
  bauen, an den die anderen delegieren? Wie docken almanach-update, best-practices-update, best-practices,
  bug-almanach-recherche, researcher, direktiven-recherche, superintelligenz sauber an, ohne 8x Duplikat?
- Wie viele parallele Researcher (Firecrawl=max 2 concurrent; or-research=mehr)? Themen-Zerlegung wie?
- Genaues Auswertungs-Format (Frank: einfach, optisch ruhig, gut erklaert).
- Engine final fix (parallel) + ob mm (Firecrawl, 2 parallel) oder or (OpenRouter, mehr) Default fuer Schwaerme.

## Anker
- Branch: main
- Letzte Commits:
35f64307d #47023 - best-practices(openrouter): Server-Tools 6 + file-parser-Details
66a268d24 #47022 - bugs(openrouter): 6 Server-Tools + file-parser + #9 Keep-Alive erweitert
6ddcfbf3c #47021 - fix(research): resilient JSON parse in or/mm-research.py (Keep-Alive)
9709aec3d #47020 - research-persistence: Firecrawl v2 Best-Practices+Fallen
84a03b444 #47019 - research-persistence: Firecrawl-als-OpenRouter-Engine + Promo + Credit-Oekonomie
03531222b #47018 - research-pipeline: Empfehlung VOR Frage 1 + A/B/C-Pointer in alle Research-Skills
