# Agent-Verhalten: Commit/Push-Disziplin gegen eingebaute Vorbehalte — Best Practices

> Gegenstück (was schiefgeht): `bugs/opencode/opencode-cli.md` #48a — dort steht der konkrete
> Vorfall (GPT-5.5 verweigert Commit/Push mit erfundener "nur auf ausdrückliche Anweisung"-Begründung).
> Diese Datei ist die proaktive Seite: wie man von vornherein verhindert, dass ein eingebauter
> System-Prompt eine eigene "committe immer"-Regel aussticht.
>
> **Stand:** recherchiert 2026-07-01 (5-Researcher-Schwarm, Sonnet-5-Schwarm, quellentreu).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | ⭐ Agent committet/pusht nicht, obwohl Regel es verlangt | Viele CLI-Tools (auch OpenCode) haben einen **hardcodierten** System-Prompt-Vorbehalt ("nur auf explizite Anweisung"). Eine reine Erwähnung der Pflicht in AGENTS.md reicht NICHT — die Regel muss den eingebauten Vorbehalt aktiv als überschrieben markieren | §1 |
| 2 | Wie den eingebauten Vorbehalt entkräften | Formulierungsmuster: "Diese Regel HIER ist die explizite Anweisung, für JEDE Aufgabe, ohne Wiederholung" — nicht nur "bitte committen", sondern das Autorisierungs-Wort selbst liefern | §2 |
| 3 | Zuverlässigster Mechanismus (Goldstandard) | Code-Ebene statt Prompt-Ebene: Commit als deterministische Nebenwirkung eines Event-Handlers (Aider: nach jedem Edit; Cursor+GitButler: bei Task-Ende), NIE als "Entscheidung" des LLM | §3 |
| 4 | Bei Multi-Session-Setups | `git add -A` in Auto-Commit-Snippets NICHT übernehmen — reisst fremde Dateien anderer Sessions mit. Nur eigene/getrackte Dateien committen | §3, §4 |
| 5 | OpenCode konkret | Plugin bei `session.idle` (Alert-Ebene, sofort umsetzbar) oder `tool.execute.after`-Datei-Tracking + gezielter Commit (Ausbaustufe, datei-genau) | §4 |

---

## 1. Das Problem: eingebauter System-Prompt vs. eigene Regel

Mehrere CLI-Coding-Tools liefern einen **hardcodierten** Standard-Vorbehalt für git-Aktionen aus
(z.B. OpenCode: sinngemäß "NEVER commit changes unless the user explicitly asks you to" / "DO NOT
push... unless explicitly asked"). Eine eigene Regel ("committe nach jeder Aufgabe") KONKURRIERT
mit diesem eingebauten Text — und der eingebaute Vorbehalt kann gewinnen, wenn die eigene Regel ihn
nicht aktiv für ungültig erklärt, sondern nur eine zusätzliche Erwartung formuliert.

Verwandtes Muster auf Anbieter-Ebene (nicht OpenCode-spezifisch): Anthropics eigener Engineering-Blog
zum "Claude Code Auto Mode" beschreibt einen Permission-Classifier, der Aktionen nach Reversibilität
und "Blast Radius" bewertet — Kategorien wie "Bypass review or affect others: pushing directly to
main" werden dort explizit als erhöhtes Risiko eingestuft. Die Zurückhaltung bei `git push` ist bei
mehreren Anbietern bewusst eingebaut, nicht nur eine zufällige Trainings-Nebenwirkung.
Quelle: anthropic.com/engineering/claude-code-auto-mode.

## 2. Wie man den eingebauten Vorbehalt aktiv überschreibt

Der Unterschied zwischen einer wirkungslosen und einer wirksamen Regel ist die FORMULIERUNG:

| Wirkungslos (nur Erwähnung) | Wirksam (aktive Überschreibung) |
|------------------------------|----------------------------------|
| "Bitte committe nach jeder Aufgabe." | "Diese Regel HIER ist die explizite Anweisung dafür — für JEDE Aufgabe, ohne dass sie je wiederholt werden muss." |
| "Commit+Push ist wichtig." | "Es gibt KEINE Ausnahme 'nur auf ausdrückliche Anweisung' — dieser Gedanke ist FALSCH, sollte er auftauchen." |
| Regel nur in einer selten geladenen Datei | Regel als eigenständiger, IMMER geladener Kern-Punkt (nicht nur verschachtelt in einem Spezialfall wie Multi-Task) |

Wichtig: Die Regel muss dem eingebauten Prompt explizit widersprechen ("ich weiss dass du einen
eingebauten Vorbehalt hast — DAS HIER ist die Erlaubnis"), nicht nur eine eigene Erwartung daneben
stellen. Sonst gewinnt oft der zuerst/tiefer verankerte eingebaute Text.

## 3. Der Goldstandard: Commit als Code-Nebenwirkung, nicht als LLM-Entscheidung

Recherche über mehrere AI-Coding-Tools zeigt ein gemeinsames Muster bei den zuverlässigsten Lösungen:

- **Aider** committet automatisch NACH JEDEM Edit — als Tool-Nebenwirkung im Edit-Zyklus, das LLM
  wird dabei nicht gefragt. Die Commit-Message kommt aus einem separaten (günstigen) Modell-Call,
  aber die AUSLÖSUNG ist reiner Code. Quelle: aider.chat/docs/git.html.
- **Cursor** hat keinen eingebauten Auto-Commit, löst es aber zuverlässig über externe
  Lifecycle-Hooks (z.B. GitButler-Integration: jeder Chat bekommt einen eigenen Branch, Commit
  automatisch bei Task-Ende). Quelle: blog.gitbutler.com/cursor-hooks-integration.
- **Devin/Windsurf/Copilot Workspace** kapseln die Arbeit strukturell in Branch+PR — die
  Zuverlässigkeit kommt aus der Workflow-Struktur, nicht aus einer LLM-Entscheidung.
- **Claude Code** selbst: nur LLM-Disziplin + optionale Hooks (Stop/PostToolUse) — genau die
  Schwachstelle, die dieses Dokument adressiert.

**Gemeinsamer Nenner:** Der Commit kommt aus einem DETERMINISTISCHEN Event-Handler (Code), nie aus
einer "Entscheidung" des Sprachmodells. Ein Stop-Hook (Claude Code) bzw. ein `session.idle`-Plugin
(OpenCode) ist die jeweilige Entsprechung — Poka-Yoke Stufe 3: der Agent müsste den Mechanismus
aktiv entfernen, um NICHT zu committen.

## 4. Multi-Session-Einschränkung — bewusst NICHT 1:1 übernehmen

Alle recherchierten Auto-Commit-Vorbilder (inkl. das gängige Claude-Code-Stop-Hook-Snippet) nutzen
`git add -A`. Das ist bei einem geteilten Multi-Session-Repo (mehrere CLI-Fenster am selben Ordner,
siehe `parallel-sessions-git.md`) gefährlich — es reisst fremde, unfertige Dateien anderer Sessions
mit. Zwei sichere Varianten für diesen Fall:

1. **Alert-only (sofort umsetzbar, aktuell in `git-dirty-watchdog.js` implementiert):** Ein
   `session.idle`-Plugin prüft `git status --porcelain`; ist das Repo dirty, wird das per Ton
   (`error.wav`) + Log-Eintrag sofort unübersehbar gemacht. Committet NICHT automatisch — der
   Mensch oder das Modell muss noch selbst committen, aber das Vergessen ist unmöglich zu übersehen.
2. **Datei-basiertes Auto-Commit (Ausbaustufe, recherchiert aber noch nicht umgesetzt):** Ein Plugin
   trackt per `tool.execute.after`, welche Dateien DIESE Session selbst editiert hat (analog zum
   Tracking-Muster in `tool-first-guard.js`), und committet bei `session.idle` NUR genau diese
   Dateien namentlich — nie `git add -A`. Kommt dem Aider-Pattern nahe, ohne das Multi-Session-Risiko.

## 5. Bezugstabelle: Best Practice ↔ Bug-Almanach

| Best-Practice-Abschnitt (diese Datei) | Bug-Gegenpart (`bugs/opencode/opencode-cli.md`) |
|----------------------------------------|--------------------------------------------------|
| §1 Eingebauter Vorbehalt | #48a (konkreter Vorfall + Root Cause) |
| §2 Aktive Überschreibung | #48a FIX-Abschnitt (AGENTS.md-Formulierung) |
| §3 Code statt Prompt | #48a FIX (git-dirty-watchdog.js) |
| §4 Multi-Session | `parallel-sessions-git.md` |

## Quellen (Stand 2026-07-01)

- anthropic.com/engineering/claude-code-auto-mode — Permission-Classifier, Reversibilität/Blast-Radius
- aider.chat/docs/git.html, aider.chat/docs/faq.html — Auto-Commit als Tool-Nebenwirkung
- blog.gitbutler.com/cursor-hooks-integration — Cursor + externe Lifecycle-Hooks
- github.com/anomalyco/opencode/issues/14923, /3099, /11534, /11732 — verwandte OpenCode-Issues
- arxiv.org/pdf/2605.07769 ("Coding Agents Don't Know When to Act"), arxiv.org/html/2407.18418v1
  (Abstention-Survey) — allgemeine Over-Caution-Literatur
