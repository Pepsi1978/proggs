# Claude Code (OpenRouter)

Startet dein **komplettes Claude Code** (mit allen Regeln, Hooks, Skills, Agenten, MCP-Servern)
— aber mit einem **Modell deiner Wahl von OpenRouter** statt deinem Opus-Abo.

Dein **normales Claude Code bleibt unberuehrt.** Die OpenRouter-Einstellungen gelten nur im
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
   - **E** = eigenen Modell-Slug eintippen (z.B. `deepseek/deepseek-chat-v3.1`)
   - **Enter** = Standard: Claude Opus (laeuft wie gewohnt, rock-solid)
3. Claude Code startet in `~/proggs` mit dem gewaehlten Modell. Fertig.

---

## Die Spalten in den Listen

| Spalte | Bedeutung |
|--------|-----------|
| **Ktx** | Kontext-Groesse (wie viel passt rein), z.B. 200K, 1M |
| **Tools** | Kann das Modell Werkzeuge nutzen? **Ohne "ja" funktioniert Claude Code kaum.** |
| **Agt** | Agenten-Benchmark (Tool-/Terminal-Arbeit) — hoeher = besser fuer deinen Stil. Live von OpenRouter. |
| **Cod** | Coding-Benchmark — hoeher = besser beim Programmieren. Live von OpenRouter. |
| **Eig** | Eignung fuer Claude Code (A+ = perfekt wie Opus, C = experimentell). Erfahrungswert. |
| **$/M** | Preis pro 1 Mio Eingabe-Token (`frei` = kostenlos). |

---

## Wichtig zu wissen

- **Claude-Modelle** (A+) laufen rock-solid — wie dein Opus-Abo, nur anders abgerechnet.
- **Fremdmodelle** (GPT, Gemini, DeepSeek, Kimi …) funktionieren mit robustem Werkzeug-Verhalten
  (`enhancetool` ist aktiv), aber **nie ganz so rund wie Claude** — Claude Code ist fuer Claude gebaut.
- **Neue Modelle** (z.B. Kimi 3.0) erscheinen automatisch in den Listen, sobald OpenRouter sie listet.
- **Kosten:** Du zahlst pro Nutzung. Bei Fremdmodellen entfaellt ein Spar-Mechanismus (Prompt-Caching) →
  lange Sessions koennen teurer sein als dein Abo. Behalt das Activity-Dashboard von OpenRouter im Blick.

---

## Wenn etwas klemmt

| Problem | Loesung |
|---------|---------|
| "Schluessel noch nicht eingetragen" | Schluessel in `~/SK/ClaudeCodeOpenRouter/openrouter.key` eintragen (oeffnet sich automatisch). |
| Modell-Liste laedt nicht | Internet pruefen; das Skript nutzt sonst die letzte gespeicherte Liste. |
| Modell zeigt "Tools: NEIN" | Dieses Modell taugt nicht fuer Claude Code — ein anderes mit "ja" waehlen. |
| Fremdmodell bricht bei Werkzeugen ab | Auf ein Claude-Modell (A+) wechseln — das ist die stabilste Wahl. |
| Zurueck zum normalen Claude | Einfach deine gewohnte Claude-Verknuepfung nutzen — die ist voellig getrennt. |

Logs jeder Sitzung liegen in `logs/openrouter-launcher-<datum>.jsonl` (zum Nachschauen/Debuggen).

---

## Wie es technisch funktioniert (kurz)

Claude Code spricht das Anthropic-Format. OpenRouter hat dafuer einen passenden Eingang, aber
fuer **robustes Werkzeug-Verhalten bei Fremdmodellen** laeuft alles ueber den lokalen
**claude-code-router (`ccr`)** mit dem `enhancetool`-Transformer. Das Startmenue
(`Start-ClaudeCode-OpenRouter.ps1`) schreibt bei jeder Modellwahl die passende `ccr`-Config
(`~/.claude-code-router/config.json`) und startet `ccr code` in `~/proggs`.

### Dateien

| Datei | Zweck |
|-------|-------|
| `Start-ClaudeCode-OpenRouter.ps1` | Das Startmenue (Herzstueck). |
| `coding-models.json` | Eignungs-Einschaetzung + Kategorie-Texte (anpassbar). |
| `config.example.json` | Referenz-Vorlage der ccr-Config (nicht die aktive Datei). |
| `~/SK/ClaudeCodeOpenRouter/openrouter.key` | Dein Schluessel (ausserhalb Repo). |
| `~/.claude-code-router/config.json` | Aktive ccr-Config (vom Startmenue erzeugt). |
