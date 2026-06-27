# Session Handoff — 2026-06-27 (nachmittags)

## Ziel (1-3 Saetze)
Anti-Halluzinations-Wissen fuer OpenCode nutzbar machen: (a) Kurzcheck-Dateien aus allen Almanachen/
Best-Practices erzeugen (fuers Cortex), und (b) OpenCode so einrichten, dass es Almanache/BP
just-in-time (nur der gerade bearbeitete Bereich) aus dem Cortex liest. Es sind noch ZWEI Aufgaben offen.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)
Es lief KEIN halbfertiger Edit — alles ist committed/gepusht. Offen sind ZWEI klar definierte Aufgaben.
Frank macht jetzt /clear + restore und will direkt weiterarbeiten.

### OFFENE AUFGABE A — die 6 restlichen BP-Kurzchecks generieren
- **Was:** Fuer 6 grosse Best-Practices OHNE eigene Kurzcheck-Sektion je eine `-kurzcheck.md` schreiben
  (handgeschrieben aus dem Volltext ZUSAMMENFASSEN + gegenpruefen — NICHT extrahieren, die haben keine
  Kurzcheck-Sektion). Anti-Halluzination: jede BP voll lesen, nicht aus Ueberschriften raten.
- **Die 6 Dateien (alle best-practices/opencode/):**
  1. agents-md-memory.md (261 Z, 13 Abschnitte)
  2. agents-modes.md (294 Z, 10)
  3. grundlagen-installation.md (354 Z, 9)
  4. konfiguration.md (343 Z, 15)
  5. plugins-mcp-skills.md (362 Z, 11)
  6. token-effizienz.md (303 Z, 9)
- **KONVENTION (exakt wie die 187 bereits erzeugten):**
  - Dateiname: `<original>-kurzcheck.md` im SELBEN Ordner.
  - Erste Zeile: `# <derive_title(original)> Kurzcheck` — derive_title = h1 ohne "# ", dann entfernen:
    " — Best Practices...", "(Best Practices...", Praefix "Best Practices:"/"Bekannte Bugs:"/"Bug-Almanach:",
    Suffix " — Almanach", " (Stand...)". Das ergibt via cortex_sync.py den eindeutigen Cortex-Titel
    "<basis> Kurzcheck (Best Practices)" — KEINE doc_id-Kollision mit dem Original.
  - Dann Verweis-Blockquote (1:1 kopieren aus einer Vorlage):
    "> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
    > diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
    > Titel ohne Kurzcheck), nicht nur diese Kurzfassung."
  - Dann "## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)" + Tabelle | # | Situation | Best Practice (Kurzform) | Volltext |.
  - **VORLAGEN ansehen** (3 schon fertige generierte): best-practices/opencode/openrouter-kurzcheck.md,
    best-practices/opencode/command-palette-kurzcheck.md, best-practices/claude-tooling/openrouter-claude-code-kurzcheck.md.
- **Wichtig:** Die Tools (check-coupling/guard-coverage/dead-paths/health.py + bug-almanac-index-Hook)
  schliessen `*-kurzcheck.md` BEREITS aus (#47280) — NICHTS daran aendern. NACH dem Schreiben
  `python bugs/health.py` laufen: die 2 WARN (firecrawl-Drift + guard-coverage-Luecke) sind ALT/fremd und
  muessen IDENTISCH bleiben (keine neuen durch Kurzchecks).
- **Schreiben per Write-Tool** (Inhalt ist handgeschrieben). Danach committen+pushen (#NNN).

### OFFENE AUFGABE B — Recall-Modell in OpenCode-AGENTS.md einarbeiten (Franks ausdruecklicher Wunsch)
- **Was:** Damit OpenCode die Kurzchecks/Almanache/BP just-in-time aus dem Cortex liest, eine neue
  KERN-REGEL (z.B. Regel 8) in `~/.config/opencode/AGENTS.md` einbauen + Spiegel `opencode-setup/AGENTS-global.md`.
- **Hintergrund — das dreistufige Modell, das ich vorgeschlagen habe** (Frank waehlte den MVP = recall-Ebene + AGENTS.md-Eintrag,
  bewusst KEINE Plugin-Hooks, weil die in OpenCode experimentell/instabil sind):
  - Regeln = Push (alle beim Start geladen). Almanache/BP = Pull (zu viele/zu gross fuer Vorladen → Context Rot).
  - Schicht 1 Awareness (Hint) · Schicht 2 just-in-time `recall` (semantisch, nur der relevante Bereich) · Schicht 3 Plugin-Guard (Durchsetzung).
  - **MVP jetzt = AUFGABE B:** AGENTS.md-Regel im Sinne von: "BEVOR du in einem technischen Bereich arbeitest,
    rufe das second-brain-Werkzeug recall mit dem Bereich + Kurzcheck auf und lies den KURZCHECK (Stufe A).
    Bei einem FEHLER im Bereich den VOLLTEXT lesen (gleicher Titel ohne Kurzcheck, Stufe B). Nur den gerade
    bearbeiteten Bereich, nicht alles." Balance betonen (greift bei Fakten-/Bereichsarbeit, nicht beim blossen Denken/Planen).
- **Voraussetzung:** Kurzchecks + Volltexte muessen im Cortex liegen. Frank macht das per cortex-update-Skill
  (parallele Session hat cortex-update in #47279 schon um die Kurzcheck-Aufnahme erweitert). cortex-update NICHT
  selbst anfassen (Frank/parallele Session).
- **Format-Vorbild:** Kern-Regel 7 (Anti-Halluzination) steht schon in derselben AGENTS.md → gleiche Knappheit/Stil.
  AGENTS.md kurz halten (< ~150 Zeilen, OpenCode befolgt kurze AGENTS.md zuverlaessiger). Nach dem Edit Spiegel 1:1 nachziehen (cp global → opencode-setup/AGENTS-global.md), committen+pushen.
- **Danach:** mit "Naechste Schritte" weiter; A und B sind unabhaengig, Reihenfolge nach Franks Wahl.

## Aktueller Status
- **Erledigt + gepusht in dieser Session:**
  - #47267 best-practices/agents/anti-halluzination-regeln.md (Recherche 9 Researcher) + bugs/claude-config §1.1 Rueckkopplung
  - #47268 OpenCode-Plugin tool-first-guard.js (warnt bei edit/patch ohne vorheriges read; OPENCODE_TOOL_FIRST_ENFORCE=1 blockt)
  - #47269 AGENTS.md Kern-Regel 7 (Anti-Halluzination, 5 Kern-Regeln) global + Spiegel
  - #47270/#47271 opencode-setup Cross-Platform-Installer (install.sh + install.ps1 + sounds + README; dry-run getestet)
  - #47272 opencode-setup/rules-opencode/anti-halluzination.md (15. Programmierung/Rules-Regel; Frank speichert sie selbst ins Cortex)
  - #47280 Kurzcheck-System Teil 1: 184 -kurzcheck.md extrahiert + 8 Harness-Tools schliessen *-kurzcheck.md aus (health.py = Baseline)
  - #47281 3 von 9 generierten BP-Kurzchecks (openrouter-claude-code, command-palette, openrouter)
- **In Arbeit:** Aufgabe A (3/9 generierte fertig, 6 offen) + Aufgabe B (noch nicht begonnen).
- **Blockiert:** nichts.

## Relevante Dateien
- best-practices/opencode/{agents-md-memory,agents-modes,grundlagen-installation,konfiguration,plugins-mcp-skills,token-effizienz}.md — die 6 BP, die noch Kurzchecks brauchen (Aufgabe A)
- best-practices/opencode/openrouter-kurzcheck.md u.a. — Format-VORLAGEN fuer die generierten Kurzchecks
- ~/.config/opencode/AGENTS.md (+ Spiegel opencode-setup/AGENTS-global.md) — hier kommt die Recall-Regel rein (Aufgabe B); Kern-Regel 7 als Stil-Vorbild
- ~/.claude/skills/cortex-update/scripts/cortex_sync.py — derive_title-Logik (Z.118-138) + Titel-Suffix; NICHT anfassen (parallele Session)
- best-practices/agents/anti-halluzination-regeln.md — die Forschungsbasis (5 Kern-Regeln, Grounding, Hebel)

## Getroffene Entscheidungen
- Kurzcheck-Dateien liegen NEBEN dem Original (`-kurzcheck.md`), NICHT in eigenem Ordner (Frank-Entscheidung) → dafuer 8 Tools angepasst.
- Fuer BP ohne Kurzcheck-Sektion wird ein Kurzcheck GENERIERT (Frank-Entscheidung) — nur fuer echte BP, NICHT fuer Rohergebnisse/Plaene/Prompt-Listen (z.B. *-rohergebnisse, UMSETZUNGSPLAN, OFFENE-ALMANACHE-PROMPTS).
- OpenCode-Almanach/BP-Einbindung: MVP = recall + AGENTS.md-Regel (gegen Plugin-Hooks, weil experimentell/instabil).
- Skills-Mitbau-Pflicht (best-practices/bug-almanach-recherche/research bauen kuenftig automatisch Kurzchecks mit): Frank hat das im Skill-Core-Text schon eingebaut — NICHT nochmal machen.

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- KEINE echten Fehlschlaege. Wichtige Hinweise:
  - `git commit`+`push` mit 192 Dateien timete einmal nach 2 Min aus (parallele Session haelt Index-Lock) — der COMMIT war aber durch, nur der PUSH fehlte. Lehre: nach Timeout NICHT neu committen (Duplikat-Gefahr), erst `git log -1` pruefen, dann nur pushen.
  - Session-Backup-Heredoc scheiterte an Single-Quotes im Inhalt (recall('...')) → Write-Tool statt Bash-Heredoc nutzen, wenn der Inhalt Single-Quotes hat.
  - Firecrawl-Suche braucht KURZE keyword-Queries; lange Queries mit eingebetteten Anfuehrungszeichen → 0 Treffer.

## Wichtige Recherche-Ergebnisse
- doc_id im Cortex ist GLOBAL titel-basiert (sha1 aus "frank::"+title.lower(), Kategorie zaehlt NICHT) → Titel muss global eindeutig sein. Darum haengt cortex_sync.py " (Almanach)"/" (Best Practices)" an; Kurzcheck-Titel = "<basis> Kurzcheck" → eindeutig gegen das Original.
- Anti-Halluzination Kern: Grounding ist staerkster Hebel; Schema-Zwang OHNE Grounding macht es schlimmer; niedrige Temp fuer Fakten; "Rules in prompts are requests, hooks in code are laws" (4/5 Modelle ignorieren AGENTS.md → Jaroslawicz 2025).
- OpenCode laedt Regeln aus Cortex dynamisch per get_category_item bis N (N kommt vom Server) → neue Regeln werden automatisch erkannt, AGENTS.md braucht keine feste Zahl.

## Naechste Schritte (priorisiert)
1. Frank entscheiden lassen, ob zuerst Aufgabe A (6 Kurzchecks) oder Aufgabe B (Recall-Regel in AGENTS.md) — beide unabhaengig.
2. AUFGABE A: die 6 BP je voll lesen → -kurzcheck.md schreiben (Konvention + Vorlagen oben) → health.py pruefen (WARN identisch zur Baseline) → committen+pushen.
3. AUFGABE B: Recall-Kern-Regel in ~/.config/opencode/AGENTS.md (+ Spiegel) einbauen (MVP: recall vor Bereichsarbeit, Kurzcheck Stufe A / Volltext bei Fehler Stufe B) → committen+pushen.
4. Danach: Frank kann cortex-update laufen lassen, damit alle 193 Kurzchecks + die neue Regel im Cortex landen.

## Offene Fragen
- Reihenfolge A vs B (Frank entscheidet). Sonst keine.

## Anker
- Branch: main
- Letzte Commits:
57da5acc1 #47282 - Dashboard-Version 0.25.0 (Drawer-Buttons) [parallele Session]
117a1f87a #47281 - Kurzcheck-System: 3 von 9 generierten BP-Kurzchecks
c850504fd #47281 - Drawer-Buttons [parallele Session, Doppelnummer]
de767f711 #47280 - Kurzcheck-System Teil 1: 184 -kurzcheck.md + 8 Tools
6653f36ba #47279 - cortex-update: add CodeCheck/Kurzcheck files
