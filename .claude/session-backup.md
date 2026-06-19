# Session Handoff — 2026-06-19

## Ziel (1-3 Saetze)
Frank evaluiert OpenCode-Plugins fuer sein Setup. Es ging darum: (1) wie man Plugins bei OpenCode
installiert, (2) wo es seriose Plugin-/MCP-Verzeichnisse gibt, (3) eine kuratierte, fuer SEIN Profil
passende Plugin-Auswahl mit verstaendlichen Erklaerungen in LEICHTER SPRACHE (keine Fremdwoerter).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. ALLE 11 Aufgaben der letzten
Multi-Task-Anfrage sind fertig, dokumentiert und committed (#46931 docs opencode, Hash 02aa6fb12).
Es gibt KEINEN eigenen uncommitteten Arbeitsstand (die Working-Tree-Eintraege sind fremde
Parallel-Session-/Hook-Auto-Dateien: MEMORY.md, active-tasks.jsonl, experience-store.jsonl,
trajectories.jsonl, diverse ?? — NICHT anfassen).

## Aktueller Status
- Erledigt: 10 Plugin-Recherchen (Researcher-Schwarm) + worktree-Frage + Persistierung in
  best-practices/ + bugs/ (committed #46931 / 02aa6fb12, gepusht).
- Frueher in dieser Session: #46925 (erste OpenCode-Plugin-Doku: Discovery + 2 Almanach-Fallen).
- In Arbeit: nichts.
- Blockiert: nichts.

## Relevante Dateien
- `best-practices/opencode/plugins-mcp-skills.md` — §8 (neu): Plugin-Trigger-Modell + recherchierte
  Plugin-Auswahl-Tabelle + Self-hosted-Memory-Bauplan. Die zentrale Wissensdatei zu OpenCode-Plugins.
- `bugs/opencode/opencode-cli.md` — §55a/55b/55c (neu): kein plugin-add-CLI, open-code.ai-Domain-Falle,
  Plugin-spezifische Fallen-Tabelle (notify/sentry/plannotator/ocx/skillful/micode/firecrawl/supermemory/worktree).

## Getroffene Entscheidungen / Kern-Ergebnisse (damit die naechste Session nicht neu recherchiert)
- OpenCode-Plugins werden EVENT-getriggert, NICHT ueber Triggerwoerter; sie laufen NICHT bei jeder
  Anfrage. Commands=Slash/manuell, Skills=on-demand (Modell waehlt nach Beschreibung), Agents=Modus.
- EMPFOHLEN fuer Frank: opencode-notifier (mohak34 — Ton bei permission+complete+error, Windows+Mac),
  opencode-openai-codex-auth (Abo statt API), opencode-dynamic-context-pruning (Token sparen),
  firecrawl (Recherche, braucht API-Key).
- STREICHEN: opencode-worktree (erzwingt eigene Branches → unvereinbar mit Franks "alles auf main");
  opencode-skillful (durch native Skills ueberholt).
- NUR mit Self-Host vertretbar (Geheimhaltung): sentry-monitor (Default sendet Prompts/Code!),
  supermemory.
- micode/plannotator: KEIN Zwang bei jeder Frage (Frank hatte das befuerchtet) — laufen nur auf Abruf.
- ocx: erst ab vielen Plugins/Profilen sinnvoll; OpenWork: Alpha, Windows schwach, spaeter neu bewerten.

## Fehlgeschlagene Ansaetze (WICHTIG)
- Researcher-Schwarm mit 7 gleichzeitig + je ~12 Web-Fetches -> Server-Rate-Limit ("temporarily
  limiting requests"), 1 Researcher (notify) musste neu. Lehre: kuenftig ~8 Fetches/Researcher oder
  5-6 gleichzeitig, gestaffelt nachziehen.
- WebFetch auf github.com wird blockiert (verlangt gh CLI). Researcher mussten auf sourcepulse.org,
  npmjs.com, opencode.ai/docs, WebSearch ausweichen. -> bei kuenftigen Repo-Recherchen direkt so.

## Wichtige Recherche-Ergebnisse (Frank-Highlight: eigenes Memory)
- Frank will einen Server mieten (~2 Jahre) und ein eigenes Memory fuer OpenCode UND Claude Code bauen.
  MACHBAR: Supermemory ist Open Source (MIT, ~27k Stars), self-hostbar (`npx supermemory local`,
  Port 6767, Postgres+pgvector, Ollama-Embeddings), bringt OpenCode-Plugin + MCP fuer Claude Code mit.
  "Ein Server, beide CLIs" via gemeinsamem MCP-Endpunkt. Alternativen: mem0 (Claude-Code-erprobt,
  Docker), Hindsight (schlank, MCP-first). Bauplan steht in plugins-mcp-skills.md §8.

## Naechste Schritte (priorisiert) — auf diese wartet Frank evtl.
1. ZWEI Intelligenz-Vorschlaege sind noch UNBEANTWORTET: (a) Researcher-Schwarm kuenftig entzerren
   (~8 Fetches statt 12); (b) GitHub-Abruf-Falle in bugs/claude-tooling/ festhalten. Frank fragen
   ob umsetzen.
2. Falls Frank will: Start-`opencode.json` bauen (notifier + codex-auth + dynamic-context-pruning),
   passende WAV-Toene ueber den sound-search-Skill suchen.
3. Vor Installation: Quellcode der Top-Plugins auf Sicherheit + Windows-Tauglichkeit pruefen
   (CLAUDE.md-Pflicht fuer externen Code; v.a. die *-auth-Plugins + vibeguard).
4. Self-hosted Memory-Server planen (Supermemory vs. mem0), sobald Frank den Server gemietet hat.
5. Globale `~/.config/opencode/AGENTS.md` aus den 3 Direktiven ableiten (OpenCode liest CLAUDE.md
   nur als Fallback).
6. Festplatte aufraeumen — beim Session-Start kam Warnung: nur ~19 GB frei, 98% belegt.

## Offene Fragen
- Frank hat auf die 2 Intelligenz-Vorschlaege noch nicht geantwortet (siehe Naechste Schritte 1).
- Welche der 5 vorgeschlagenen OpenCode-Aufgaben er als naechstes will (Starter-config? Memory-Server?).

## Anker
- Branch: main
- Letzte Commits:
b23951e14 #46931 - fix(EntropieReductor): Loop-Instanz-Prio propagiert rueckwaerts ins Template + Geschwister v0.16.4
f120c17a2 #46933 - feat(TVO/CVO macOS): personal vocabulary dictionary for Gemini correction
e348eeba3 #46932 - feat(TVO/CVO Windows): personal vocabulary dictionary for Gemini correction
02aa6fb12 #46931 - docs(opencode): Plugin-Trigger-Modell + Plugin-Auswahl + self-hosted-Memory-Bauplan + Plugin-Fallen-Tabelle  <-- DIESE SESSION
ec02dbcb4 #46930 - fix(EntropieReductor): manuelle Instanz-Prio schlaegt Loop/Template-Prio v0.16.3
