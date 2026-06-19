# Session Handoff — 2026-06-19

## Ziel (1-3 Saetze)
Frank evaluiert OpenCode-Plugins fuer sein Setup. Es ging darum: (1) wie man Plugins bei OpenCode
installiert, (2) wo es seriose Plugin-/MCP-Verzeichnisse gibt, (3) eine kuratierte, fuer SEIN Profil
passende Plugin-Auswahl mit verstaendlichen Erklaerungen in LEICHTER SPRACHE (keine Fremdwoerter).

## >>> TOOLS, DIE FRANK AUF JEDEN FALL INSTALLIEREN + EINRICHTEN WILL (feste Entscheidung 2026-06-19) <<<
Diese sind GESETZT — Frank hat sie ausdruecklich bestaetigt. In der naechsten Session umsetzen
(nach kurzem Quellcode-Sicherheitscheck, CLAUDE.md-Pflicht fuer externen Code):
1. **opencode-notifier** (mohak34) — INSTALLIEREN + einrichten. Ton bei permission (Bestaetigung) +
   complete (fertig) + error; eigene WAV-Toene pro Event auf Windows+Mac. `"plugin":["@mohak34/opencode-notifier@latest"]`,
   dann WAV pro Event in `opencode-notifier.json`. Passende Toene ggf. ueber sound-search-Skill suchen.
2. **opencode-firecrawl** — INSTALLIEREN + einrichten. Web-Recherche-Werkzeug; braucht Firecrawl-API-Key
   (Free-Tier 1000 Seiten/Monat). Cloud (Daten USA) oder self-host. Wird vom Agent bei Bedarf aufgerufen.
3. **plannotator** (@plannotator/opencode) — INSTALLIEREN. Frank WILL es definitiv (auch wenn vorher als
   "optional" eingestuft — das ist seine Entscheidung). WICHTIG zum Einrichten: `workflow:"manual"` (nur
   auf Abruf per Slash-Command) ODER Default `workflow:"plan-agent"` (greift nur im Plan-Modus), damit es
   NICHT bei jeder trivialen Frage eine Plan-Bestaetigung erzwingt.
4. **supermemory** — Frank WILL es AUCH, aber NICHT einfach jetzt installieren: separater, GROESSERER
   Rechercheschritt EXTRA. Frank hat dafuer schon ein eigenes Formular/Form vorbereitet. Also: eigenes
   Mini-Projekt "Supermemory tief + Eigenbau" — siehe Recherche-Ergebnisse unten + Bauplan in
   best-practices/opencode/plugins-mcp-skills.md §8. NICHT vorschnell installieren, auf Franks Formular warten.

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
- Blockiert: supermemory-Umsetzung wartet auf Franks vorbereitetes Formular (groessere Recherche extra).

## Relevante Dateien
- `best-practices/opencode/plugins-mcp-skills.md` — §8 (neu): Plugin-Trigger-Modell + recherchierte
  Plugin-Auswahl-Tabelle + Self-hosted-Memory-Bauplan. Die zentrale Wissensdatei zu OpenCode-Plugins.
- `bugs/opencode/opencode-cli.md` — §55a/55b/55c (neu): kein plugin-add-CLI, open-code.ai-Domain-Falle,
  Plugin-spezifische Fallen-Tabelle (notify/sentry/plannotator/ocx/skillful/micode/firecrawl/supermemory/worktree).

## Getroffene Entscheidungen / Kern-Ergebnisse (damit die naechste Session nicht neu recherchiert)
- OpenCode-Plugins werden EVENT-getriggert, NICHT ueber Triggerwoerter; sie laufen NICHT bei jeder
  Anfrage. Commands=Slash/manuell, Skills=on-demand (Modell waehlt nach Beschreibung), Agents=Modus.
- FEST GEWOLLT (siehe Abschnitt oben): notifier, firecrawl, plannotator + (extra) supermemory.
- Zusaetzlich sinnvoll waeren: opencode-openai-codex-auth (Abo statt API), opencode-dynamic-context-pruning
  (Token sparen) — Frank hat sie (noch) nicht explizit bestaetigt.
- STREICHEN: opencode-worktree (erzwingt eigene Branches -> unvereinbar mit Franks "alles auf main");
  opencode-skillful (durch native Skills ueberholt).
- NUR mit Self-Host vertretbar (Geheimhaltung): sentry-monitor (Default sendet Prompts/Code!), supermemory.
- micode/plannotator: KEIN Zwang bei jeder Frage (Frank hatte das befuerchtet) — laufen nur auf Abruf.
- ocx: erst ab vielen Plugins/Profilen sinnvoll; OpenWork: Alpha, Windows schwach, spaeter neu bewerten.

## Fehlgeschlagene Ansaetze (WICHTIG)
- Researcher-Schwarm mit 7 gleichzeitig + je ~12 Web-Fetches -> Server-Rate-Limit ("temporarily
  limiting requests"), 1 Researcher (notify) musste neu. Lehre: kuenftig ~8 Fetches/Researcher oder
  5-6 gleichzeitig, gestaffelt nachziehen.
- WebFetch auf github.com wird blockiert (verlangt gh CLI). Researcher mussten auf sourcepulse.org,
  npmjs.com, opencode.ai/docs, WebSearch ausweichen. -> bei kuenftigen Repo-Recherchen direkt so.

## Wichtige Recherche-Ergebnisse (Frank-Highlight: eigenes Memory = supermemory-Extraprojekt)
- Frank will einen Server mieten (~2 Jahre) und ein eigenes Memory fuer OpenCode UND Claude Code bauen.
  MACHBAR: Supermemory ist Open Source (MIT, ~27k Stars), self-hostbar (`npx supermemory local`,
  Port 6767, Postgres+pgvector, Ollama-Embeddings), bringt OpenCode-Plugin + MCP fuer Claude Code mit.
  "Ein Server, beide CLIs" via gemeinsamem MCP-Endpunkt. Alternativen: mem0 (Claude-Code-erprobt,
  Docker), Hindsight (schlank, MCP-first). Bauplan steht in plugins-mcp-skills.md §8. Frank hat fuer
  diese tiefere Recherche schon ein eigenes Formular vorbereitet -> als eigenes Projekt behandeln.

## Naechste Schritte (priorisiert)
1. Die 3 FEST GEWOLLTEN Tools einrichten: opencode-notifier + firecrawl + plannotator (jeweils kurz
   Quellcode-Sicherheitscheck, dann opencode.json-Eintrag + Einrichtung; plannotator mit workflow
   manual/plan-agent; notifier WAV-Toene; firecrawl API-Key). Ggf. Start-`opencode.json` daraus bauen.
2. supermemory: auf Franks vorbereitetes Formular warten / danach fragen, dann GROSSE Recherche +
   Eigenbau-Plan (Server mieten). NICHT vorschnell installieren.
3. ZWEI Intelligenz-Vorschlaege noch UNBEANTWORTET: (a) Researcher-Schwarm kuenftig entzerren
   (~8 Fetches statt 12); (b) GitHub-Abruf-Falle in bugs/claude-tooling/ festhalten.
4. Optional: globale `~/.config/opencode/AGENTS.md` aus den 3 Direktiven ableiten (OpenCode liest
   CLAUDE.md nur als Fallback).
5. Festplatte aufraeumen — beim Session-Start kam Warnung: nur ~19 GB frei, 98% belegt.

## Offene Fragen
- Wo ist Franks vorbereitetes Formular fuer die supermemory-Recherche? (Beim Wiedereinstieg danach fragen.)
- Sollen codex-auth + dynamic-context-pruning auch mit rein? (Empfohlen, aber nicht explizit bestaetigt.)

## Anker
- Branch: main
- Letzte Commits:
b23951e14 #46931 - fix(EntropieReductor): Loop-Instanz-Prio propagiert rueckwaerts ins Template + Geschwister v0.16.4
f120c17a2 #46933 - feat(TVO/CVO macOS): personal vocabulary dictionary for Gemini correction
e348eeba3 #46932 - feat(TVO/CVO Windows): personal vocabulary dictionary for Gemini correction
02aa6fb12 #46931 - docs(opencode): Plugin-Trigger-Modell + Plugin-Auswahl + self-hosted-Memory-Bauplan + Plugin-Fallen-Tabelle  <-- DIESE SESSION
3f36b032c #46934 - session backup: handoff snapshot
