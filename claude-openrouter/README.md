# Claude Code (OpenRouter)

Startet dein **komplettes Claude Code** (mit allen Regeln, Hooks, Skills, Agenten, MCP-Servern,
Werkzeugen) — aber mit einem **Modell deiner Wahl von OpenRouter** statt deinem Opus-Abo.

Dein **normales Claude Code bleibt unberuehrt.** Die OpenRouter-Einstellungen gelten nur in dem
Fenster, das ueber die Verknuepfung "Claude Code (OpenRouter)" startet.

---

## Einmalig einrichten (1 Minute)

1. Hol dir einen OpenRouter-Schluessel: <https://openrouter.ai/settings/keys> (beginnt mit `sk-or-v1-...`)
2. Trag ihn ein in:  `~/SK/ClaudeCodeOpenRouter/openrouter.key`
   (Beim ersten Doppelklick oeffnet sich die Datei automatisch, falls der Schluessel fehlt.)
3. Lade dein OpenRouter-Konto mit Guthaben auf (Tipp: >= 10 USD → bessere Limits & Geschwindigkeit).
   **Empfehlung:** im OpenRouter-Dashboard ein **Ausgabe-Limit pro Schluessel** setzen, damit nie etwas ausufert.

> Der Schluessel liegt absichtlich in `~/SK/` (ausserhalb des Repos) und wird **nie** auf GitHub hochgeladen.

---

## Taeglich nutzen

1. Doppelklick auf **"Claude Code (OpenRouter)"** (Desktop).
2. Im Menue ein Modell waehlen:
   - **1** = Programmier-Modelle, nach Staerke sortiert (empfohlen)
   - **2** = alle Modelle nach Kategorie
   - **E** = eigenen Modell-Slug eintippen (z.B. `deepseek/deepseek-v4-pro`)
   - **Enter** = Standard: Claude Opus (rock-solid)
3. Claude Code startet in `~/proggs` mit dem gewaehlten Modell. Fertig.

Du kannst auch **mehrere Fenster** gleichzeitig mit verschiedenen Modellen offen haben — jedes Fenster
ist unabhaengig.

---

## Die Spalten in den Listen

| Spalte | Bedeutung |
|--------|-----------|
| **Ktx** | Kontext-Groesse (wie viel passt rein), z.B. 200K, 1M |
| **Tools** | Kann das Modell Werkzeuge nutzen? **Ohne "ja" funktioniert Claude Code kaum** — solche Modelle filtere ich aus der Coding-Liste. |
| **Agt** | Agenten-Benchmark (Tool-/Terminal-Arbeit) — hoeher = besser fuer deinen Stil. Live von OpenRouter. |
| **Cod** | Coding-Benchmark — hoeher = besser beim Programmieren. Live von OpenRouter. |
| **Eig** | Eignung fuer Claude Code (A+ = perfekt wie Opus, C = experimentell). Erfahrungswert. |
| **$/M** | Preis pro 1 Mio Eingabe-Token (`frei` = kostenlos). |

---

## Wichtig zu wissen

- **Werkzeuge funktionieren voll** — Skills, Agenten, MCP, Hooks, Datei-Edits — mit jedem tool-faehigen
  Modell ("Tools: ja").
- **Claude-Modelle** (A+) laufen rock-solid — wie dein Opus-Abo, nur anders abgerechnet.
- **Fremdmodelle** (GPT, Gemini, DeepSeek, Kimi …) funktionieren ebenfalls, aber **nie ganz so rund wie
  Claude** — Claude Code ist fuer Claude gebaut. Starke Modelle (DeepSeek V4, GPT-5, Gemini) liefern
  sauberes Werkzeug-Format; sehr schwache/alte Modelle koennen beim Werkzeug-Aufruf mal haken.
- **Neue Modelle** (z.B. Kimi 3.0) erscheinen automatisch in den Listen, sobald OpenRouter sie listet.
- **Modell-Anzeige:** Claude Code zeigt in seinem eigenen Banner evtl. "Opus" als internen Namen an —
  das echte Modell steht gruen im Startfenster (und im Log). Das ist normal.
- **Kosten:** Du zahlst pro Nutzung. **Wichtig zu verstehen:** OpenRouter cacht NICHT — bei jeder
  Frage wird dein gesamter Kontext (Anweisungen + bisheriges Gespraech) neu berechnet. Deshalb laeuft
  diese Umgebung mit einem **schlanken Profil** (siehe unten), damit der Kontext klein und damit guenstig bleibt.

## Schlankes Profil (warum es guenstig ist)

Diese Umgebung nutzt ein eigenes, schlankes Claude-Profil unter `~/.claude-openrouter/` (via
`CLAUDE_CONFIG_DIR`). Es laedt absichtlich WENIG:

- ✅ **Deine eigenen Skills** (verlinkt aus `~/.claude/skills/`, bleiben synchron)
- ✅ **Direktive #3** (Resilient Bugfixing)
- ✅ Dein Repo + dessen `CLAUDE.md` (normal geladen)
- ❌ Die 40 grossen Regel-Dateien (~90.000 Token) — der Haupt-Kostentreiber
- ❌ claude-mem + andere Plugins, Auto-Memory

→ Statt ~200.000 Token pro Frage nur noch grob ~40.000 → ~5x guenstiger und schneller. Dein **normales
Claude Code** (`~/.claude`, voller Harness + Opus-Abo) bleibt davon **voellig unberuehrt**.

Falls das schlanke Profil mal neu aufgebaut werden muss:  `pwsh -File setup-lean-profile.ps1`

---

## Wenn etwas klemmt

| Problem | Loesung |
|---------|---------|
| "Schluessel noch nicht eingetragen" | Schluessel in `~/SK/ClaudeCodeOpenRouter/openrouter.key` eintragen (oeffnet sich automatisch). |
| Modell-Liste laedt nicht | Internet pruefen; das Skript nutzt sonst die letzte gespeicherte Liste. |
| Modell zeigt "Tools: NEIN" | Dieses Modell taugt nicht fuer Claude Code — ein anderes mit "ja" waehlen. |
| Fremdmodell hakt bei Werkzeugen | Auf ein Claude-Modell (A+) oder ein starkes Modell (DeepSeek V4, GPT-5) wechseln. |
| Zurueck zum normalen Claude | Einfach deine gewohnte Claude-Verknuepfung nutzen — die ist voellig getrennt. |

Logs jeder Sitzung liegen in `logs/openrouter-launcher-<datum>.jsonl` (zum Nachschauen/Debuggen).

---

## Wie es technisch funktioniert (kurz)

Claude Code spricht das Anthropic-Format. OpenRouter bietet dafuer eine **native "Anthropic-Skin"**
(`https://openrouter.ai/api`) — einen Endpunkt, der genau dieses Format direkt versteht.
**Es ist KEIN Proxy und KEIN Hintergrund-Dienst noetig** — das Startmenue
(`Start-ClaudeCode-OpenRouter.ps1`) setzt nur ein paar Umgebungsvariablen **in diesem einen Fenster**
(`ANTHROPIC_BASE_URL`, `ANTHROPIC_AUTH_TOKEN`, das gewaehlte Modell als `ANTHROPIC_DEFAULT_*_MODEL` +
`CLAUDE_CODE_SUBAGENT_MODEL`) und startet dann `claude` direkt in `~/proggs`. Deshalb koennen mehrere
Fenster unabhaengig laufen, und nichts kann im Hintergrund "absterben".

### Selbsttest (fuer Entwickler)

`pwsh -File Start-ClaudeCode-OpenRouter.ps1 -SelfTest "<modell-slug>"` laeuft das ganze Skript bis
kurz vor dem `claude`-Start durch (ohne Menue, ohne Claude Code zu oeffnen) — gut zum Pruefen.

### Dateien

| Datei | Zweck |
|-------|-------|
| `Start-ClaudeCode-OpenRouter.ps1` | Das Startmenue (Herzstueck). |
| `coding-models.json` | Eignungs-Einschaetzung + Kategorie-Texte (anpassbar). |
| `~/SK/ClaudeCodeOpenRouter/openrouter.key` | Dein Schluessel (ausserhalb Repo). |
