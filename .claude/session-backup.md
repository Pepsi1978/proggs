# Session Handoff — 2026-06-26 ~14:50 (Europe/Berlin)

## Ziel (1-3 Saetze)
Langer Arbeitstag am Second-Brain-Server (Cortex) + Harness-/Regel-Umstrukturierung. Zwei grosse
Straenge: (1) brain-api Bugfixes (OOM-Loop + Grossdatei-Abruf), (2) Regel-System fuer OpenCode
umgebaut (AGENTS.md schlank, Regeln on-demand aus dem Gehirn, plattformuebergreifend ins Repo).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen und committet (#47255). ABER: es stehen
mehrere OFFENE ENTSCHEIDUNGS-FRAGEN an den Benutzer (siehe "Offene Fragen") — die naechste Session
sollte mit diesen weitermachen, nicht neu raten.

## Aktueller Status
- Erledigt heute (alle committet+gepusht+deployt):
  - brain-api OOM-Loop-Fix v1.13.1/1.13.2 (category-counts + /list metadaten-only, _scroll-Parameter)
  - brain-api Grossdatei-Abruf-Fix v1.14.0 (by-title limit=1, _scroll paginiert, text_len-Feld +
    Backfill 251/251, by-category/by-parent chunk_index=0) — 1,4M-Doc jetzt vollstaendig abrufbar
  - Regel `bugfix-to-second-brain` erstellt (Claude ~/.claude/rules/ + AGENTS.md fuer OpenCode):
    funktionierende Bugfixes ins Gehirn unter `bugfixes/<unterkat>`, Titel mit Datum+Uhrzeit
  - SICA-Utility-Metric-Regel komplett geloescht (Ballast) + toten Verweis bereinigt
  - AGENTS.md aufgeraeumt 497->126 Zeilen (#47254): Secrets/Parallele-Sessions/Semikolon RAUS,
    Abruf-Anweisung rein ("zu Session-Start get_by_category('Programmierung/Rules') laden + 'N Regeln
    eingelesen' bestaetigen"). Grund: Best-Practice <150 Zeilen, ab 200-400 degradiert die Befolgung.
  - opencode-setup/ im Repo angelegt (#47255): opencode.jsonc + AGENTS-global.md gespiegelt +
    README (macOS/Windows-Einrichtung). Fuer 1:1 gleiche OpenCode-Umgebung auf macOS.
  - AGENTS.md Kategorie-Klarstellung: `Programmierung/Rules` (Unterkat heisst Rules, NICHT Regeln)
- Parallele Session (Codex/andere) hat ebenfalls am Second-Brain gearbeitet (#47254 Ladefenster-
  Schutz brain-api v1.15.0 + mcp v1.2.0, #47255 timestamp Cortex chat saves) — NICHT meine Arbeit.
- Im Gehirn liegen jetzt: 14 Regeln unter `Programmierung/Rules`, 3 Direktiven unter
  `Programmierung/Direktiven`, Bugfix-Faelle unter `bugfixes/cortex-dashboard` + `bugfixes/cortex-brain-api`.

## Relevante Dateien
- `~/proggs/AGENTS.md` (126 Z.) — schlank, mit Gehirn-Abruf-Anweisung + Bugfix-Regel
- `~/proggs/opencode-setup/` — opencode.jsonc, AGENTS-global.md, README (NEU)
- `~/.config/opencode/AGENTS.md` (globale OpenCode-Regeln) + opencode.jsonc — die LOKALEN Originale
- `~/proggs/second-brain-server/brain-api/app.py` (v1.14.0, evtl. v1.15.0 durch Parallel-Session)
- `~/.claude/rules/bugfix-to-second-brain.md` (+ Repo-Spiegel claude-code-setup/rules/)
- `~/proggs/bugs/server/qdrant.md` §8 (scroll-with_payload-OOM dokumentiert)

## Getroffene Entscheidungen
- CLAUDE.md ist TABU (Franks Kerndatei) — nur AGENTS.md wird fuer OpenCode bearbeitet.
- Regeln zentral im Gehirn (eine Quelle), AGENTS.md verweist nur darauf (Token sparen + Befolgung).
- "Eine Regel = ein Ort" gegen Doppelung: wandert eine Regel ins Gehirn, muss sie aus AGENTS.md raus.
- Secrets aus AGENTS.md raus (kommen via Rules-Abruf); Fallback-Satz "keine Secrets ins Repo" bleibt.

## Fehlgeschlagene Ansaetze (WICHTIG)
- OOM-Fix per Container-Memory-Limit erhoehen (1G->2G) hat NICHT geholfen — die Ladelast waechst
  unbeschraenkt mit dem Bestand. NUR der Code-Fix (with_payload-Feldliste / limit=1 / chunk_index=0)
  loest es. Nicht nochmal das Limit hochsetzen.
- category-counts-Fix allein stoppte den Loop NICHT — der periodische Trigger war /list (via
  /api/overview alle 20s, index.html setInterval `|| true`).

## Wichtige Recherche-Ergebnisse
- OpenCode AGENTS.md-Faustregel: < ~150 Zeilen; ab 200-400 degradiert die Befolgung
  (best-practices/opencode/agents-md-memory.md:223).
- Qdrant scroll(with_payload=True) laedt ALLE Payload-Felder ALLER Punkte in den CLIENT-RAM;
  full_text haengt 1:1 in JEDEM Chunk -> Grossdaten-OOM. Fix dokumentiert in bugs/server/qdrant.md §8.
- second-brain MCP: get_by_category(category) holt eine ganze Kategorie; remember/forget/recall/list_memories.

## Naechste Schritte (priorisiert) = die offenen Entscheidungs-Fragen
1. DOPPELUNG aufloesen: Semikolon-Trenner UND Direktive #3 stehen FEST in der globalen
   `~/.config/opencode/AGENTS.md` (Abschnitt 2 + 4) UND liegen im Gehirn. Frank soll entscheiden:
   A = aus globaler AGENTS.md raus (nur Gehirn), B = fest lassen + aus Gehirn-Abruf ausnehmen.
   Mein Rat: Semikolon -> B (fest, sofort noetig), Direktive #3 -> bleibt Skill-Verweis.
2. Sollen die 3 DIREKTIVEN (`Programmierung/Direktiven`) auch per Abruf in AGENTS.md geladen werden?
   (Aktuell holt die Anweisung nur `Programmierung/Rules` = 14 Regeln, nicht die Direktiven.)
3. `search`/`recall` im brain-api gegen Grossdatei-OOM absichern (letzte Stelle, die noch Volltexte
   mitlaedt; begrenzte overfetch, geringeres Risiko) — offen seit dem 1.14.0-Fix.

## Offene Fragen
- Siehe "Naechste Schritte" 1+2 — beides sind direkte Ja/Nein- bzw. A/B-Entscheidungen an Frank.
- Hinweis: Auf macOS muss fuer den Gehirn-Abruf WireGuard eingerichtet sein (10.8.0.1).

## Anker
- Branch: main
- Letzte Commits:
a010866ae #47255 - feat(opencode-setup): OpenCode-Umgebung plattformuebergreifend im Repo
95ad8c182 #47254 - chore(AGENTS.md): aufgeraeumt 497->126 Zeilen
7411c9b6b #47255 - rule(bugfix-to-second-brain): Bugfix-Titel mit Datum UND Uhrzeit
06a05d9f3 #47255 - feat(second-brain): timestamp Cortex chat saves (Parallel-Session)
17d234fff #47254 - fix(second-brain): Ladefenster-Schutz brain-api v1.15.0 (Parallel-Session)
