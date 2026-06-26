# OpenCode-Setup — plattformuebergreifende Umgebung (Windows + macOS)

> Zweck: Damit OpenCode auf JEDEM Rechner (Windows wie macOS) **1:1 dieselbe Umgebung**
> einliest. Die hier gespiegelten globalen Dateien liegen im echten Betrieb unter
> `~/.config/opencode/` (NICHT im Repo) — dieser Ordner haelt sie versioniert fest,
> sodass ein neuer Rechner exakt nachgezogen werden kann. Gesetzt 2026-06-26 (Frank).

---

## Was OpenCode beim Start einliest (4 Ebenen)

| Ebene | Datei | Im Repo? | Spiegelung hier |
|-------|-------|----------|-----------------|
| 1. Globale Regeln | `~/.config/opencode/AGENTS.md` | nein (lokal) | **`AGENTS-global.md`** |
| 2. Globale Config | `~/.config/opencode/opencode.jsonc` | nein (lokal) | **`opencode.jsonc`** |
| 2b. Globale Agents | `~/.config/opencode/agents/*.md` | nein (lokal) | **`agents/`** (z.B. `researcher.md` — Web-Recherche ueber die API-Pipeline) |
| 3. Projekt-Regeln | `~/proggs/AGENTS.md` | **ja** | (liegt schon im Repo) |
| 4. Projekt-CLAUDE.md | `~/proggs/CLAUDE.md` | **ja** | (liegt schon im Repo) |

**Zusaetzlich (nicht in einer Datei):** Die `~/proggs/AGENTS.md` enthaelt die Anweisung, zu
Session-Beginn EINMALIG alle Arbeitsregeln aus dem zweiten Gehirn zu laden
(`second-brain`-MCP, `get_by_category('Programmierung/Rules')`). Diese Regeln liegen also
zentral auf dem Server — auf jedem Rechner identisch, ohne dass man sie kopieren muss.

---

## Einrichtung auf einem NEUEN Rechner (macOS oder Windows)

1. **Repo klonen** (falls noch nicht): `git clone … ~/proggs` — damit sind Ebene 3+4
   (Projekt-AGENTS.md, CLAUDE.md) automatisch da.

2. **Globale Dateien an ihren Platz kopieren:**
   ```sh
   mkdir -p ~/.config/opencode/agents
   cp ~/proggs/opencode-setup/opencode.jsonc   ~/.config/opencode/opencode.jsonc
   cp ~/proggs/opencode-setup/AGENTS-global.md ~/.config/opencode/AGENTS.md
   cp ~/proggs/opencode-setup/agents/*.md      ~/.config/opencode/agents/
   ```

3. **Die EINE plattformspezifische Zeile anpassen** — in `~/.config/opencode/opencode.jsonc`:
   - **Windows:** `"shell": "pwsh"` (so wie gespiegelt — nichts tun).
   - **macOS/Linux:** `"shell"` auf `"bash"` setzen ODER die Zeile entfernen (Login-Shell).
   Alles andere (MCP-Server, Plugins, Permissions, Provider) ist 1:1 identisch.

4. **Voraussetzungen schaffen** (sonst laufen Teile ins Leere):
   - **SK-Ordner:** Die API-Recherche-Skripte (`mm-research.py` / `or-research.py`) lesen ihre Keys
     aus `~/SK/OpenCode/firecrawl-api-key.txt` + `~/SK/OpenCode/go-api-key.txt` +
     `~/SK/ClaudeCodeOpenRouter/openrouter.key`. Secrets kommen NIE aus dem Repo — siehe Regel `secrets-in-sk-folder`.
     (Firecrawl-MCP wurde am 2026-06-26 entfernt — Web-Recherche laeuft jetzt komplett ueber die API.)
   - **WireGuard aktiv:** Der `second-brain`-MCP laeuft auf `http://10.8.0.1:8001/mcp` und ist NUR
     ueber den WireGuard-Tunnel erreichbar. Ohne Tunnel kein Gehirn-Abruf (-> Regeln werden nicht
     geladen). WireGuard auf dem neuen Rechner einrichten (Almanach `bugs/server/wireguard.md`).
   - **`OPENROUTER_API_KEY`** als User-Umgebungsvariable (fuer den Owl/OpenRouter-Provider; aus auth.json).

5. **Plugins** (`@mohak34/opencode-notifier`, `@plannotator/opencode`) installiert OpenCode beim ersten
   Start automatisch aus der `plugin`-Liste — nichts manuell zu tun.

6. **Start & Selbst-Check:** OpenCode oeffnen, in `~/proggs` arbeiten. Beim ersten Prompt MUSS OpenCode
   melden: **"N Regeln aus dem zweiten Gehirn eingelesen."** — dann ist die Umgebung komplett.

---

## Pflege (wichtig — sonst laeuft es auseinander)

- Aenderst du `~/.config/opencode/opencode.jsonc` oder `~/.config/opencode/AGENTS.md`, **spiegle die
  Aenderung sofort hierher** (`opencode-setup/`) und committe — sonst hat der andere Rechner den
  alten Stand. (Gleiche Idee wie `claude-code-setup/` fuer Claude Code, nur fuer OpenCode.)
- Die eigentlichen **Arbeitsregeln** aenderst du NICHT hier, sondern zentral im **zweiten Gehirn**
  (Kategorie `Programmierung/Rules`) — von dort holt sie jeder Rechner automatisch.
- `opencode.jsonc` enthaelt **keine** Klartext-Secrets (nur `{file:}`/`{env:}`-Referenzen) — daher
  unbedenklich im Repo.
