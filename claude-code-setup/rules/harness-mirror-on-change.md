# Harness-Spiegelung bei JEDER Aenderung: Skills, Plugins, Hooks, MCP & Co. ins Repo (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-16. Gilt AUTOMATISCH in JEDER Session,
> auf ALLEN Plattformen. Der Benutzer muss diese Regel NICHT erwaehnen — sie ist Standard.
> Repo-Spiegelung von `~/.claude/rules/harness-mirror-on-change.md` nach
> `~/proggs/claude-code-setup/rules/harness-mirror-on-change.md` (Cross-Platform-Sync).
>
> Konkretisiert und verschaerft fuer Skills/Plugins/Hooks/MCP/Agents/Commands das, was die
> CLAUDE.md schon allgemein fordert ("Bei Aenderungen an Regeln, Agents, Commands oder Hooks:
> Immer auch nach claude-code-setup kopieren und pushen"). Eng verwandt: Memory
> `reference_skill_mirror_locations` (Spiegelorte), `bugs/claude-tooling/claude-config.md` §2.5
> (`~/.claude/` liegt NICHT im Repo) und die Cross-Platform-Pflicht der CLAUDE.md.

---

## Grundprinzip (Franks Wortlaut sinngemaess)

**Wann immer ein Skill, Plugin, Hook, MCP-Server, Agent oder Command auf dem System neu
gebaut, aktualisiert oder verbessert wird, MUSS der aktuelle Stand SOFORT 1:1 in die
zustaendigen Repo-Ordner gespiegelt werden — damit er auf anderen Systemen uebernommen
werden kann.** Der aktive Ort (`~/.claude/`) liegt NICHT im Repo; ohne die Spiegelung hat
keine andere Plattform und kein anderer Rechner den neuen Stand.

Das gilt fuer ZWEI Spiegelorte gleichzeitig (beide bei jeder Aenderung nachziehen):

1. **Cross-Platform-Spiegel `~/proggs/claude-code-setup/`** — granular pro Komponententyp.
2. **Transportable Komplett-Umgebung `~/proggs/Umgebung/`** — das 1:1 einspielbare Paket
   fuer einen frischen Rechner. **Wird LEICHT VERGESSEN** (siehe Vorfall unten).

Die Spiegelung ist Teil der "fertig"-Definition der Aufgabe — eine Skill-/Hook-/Plugin-
Aenderung ist NICHT fertig, bevor beide Spiegelorte aktuell sind und committed+gepusht ist.

---

## Aktiver Ort → Spiegelorte (die Mapping-Tabelle)

| Komponente | Aktiv (NICHT im Repo) | Spiegel 1: `claude-code-setup/` | Spiegel 2: `Umgebung/` |
|------------|------------------------|-------------------------------|------------------------|
| **Skills** | `~/.claude/skills/<name>/` | `claude-code-setup/skills/<name>/` | `Umgebung/Skills/<name>/` |
| **Hooks** (`.ps1` + `.sh`) | `~/.claude/hooks/` | `claude-code-setup/hooks/` | `Umgebung/Hooks/` |
| **Plugins** (eigene) | `~/.claude/plugins/…` bzw. Plugin-Quelle | `claude-code-setup/Plugins/` *(falls dort gepflegt)* | `Umgebung/Plugins/` |
| **Agents** | `~/.claude/agents/<name>.md` | `claude-code-setup/agents/` | (kein eigener Ordner — via Skills/Plugins) |
| **Commands** | `~/.claude/commands/<name>.md` | `claude-code-setup/commands/` | (kein eigener Ordner) |
| **Rules** | `~/.claude/rules/<name>.md` | `claude-code-setup/rules/` | (kein eigener Ordner) |
| **MCP-Server-Config** | `~/.claude/settings.json` (`mcpServers`) bzw. `~/proggs/.mcp.json` | `claude-code-setup/mcp-windows.json` + `mcp-macos.json` | (kein eigener Ordner) |
| **Settings** | `~/.claude/settings.json` / `settings.local.json` | 3-Dateien-Regel: `settings-reference.json` + `settings.json` (macOS) + `settings.local.json` | (kein eigener Ordner) |

`Umgebung/` fokussiert bewusst auf das transportable Trio **Skills / Hooks / Plugins**.
Rules, Agents, Commands, MCP, Settings leben nur in `claude-code-setup/`.

---

## Pflicht-Ablauf bei jeder Harness-Aenderung

1. Aenderung am aktiven Ort (`~/.claude/...`) fertigstellen.
2. **Spiegel 1 (`claude-code-setup/`)** aktualisieren — den geaenderten Stand 1:1 kopieren
   (bei Hooks BEIDE Varianten `.ps1` UND `.sh`; bei Settings die 3-Dateien-Regel).
3. **Spiegel 2 (`Umgebung/`)** aktualisieren — fuer Skills/Hooks/Plugins ebenfalls 1:1.
   NICHT vergessen (die haeufigste Luecke, siehe Vorfall).
4. Pruefen, dass der Spiegel WIRKLICH 1:1 ist (`diff` bei Unsicherheit) — keine veraltete
   oder gekuerzte Kopie.
5. `git add` der eigenen, namentlich genannten Dateien → `git commit` (`#NNN`) →
   `git fetch origin && git rebase origin/main` → `git push`.
6. Status-Meldung. Da die Spiegelung plattformuebergreifendes Markdown/Code ist, lautet die
   Status-Meldung bei korrekt gepflegten Spiegeln: **"Committed, gepusht und
   plattformuebergreifend."**

---

## Beim Spiegeln IMMER beachten

- **`__pycache__` ausschliessen** — generierte Python-Caches in `*/scripts/` gehoeren nicht
  ins Repo.
- **Leeren Ordner `~/.claude/skills/learned/` ignorieren** — kein Skill.
- **LF + UTF-8 ohne BOM** halten (Windows-Falle, `claude-config.md` §3.2/§8.1) — sonst bricht
  spaeter das Edit-Tool bzw. der JSON-Parse.
- **`.mcp.json` ist plattformspezifisch** und wird NIE automatisch vereinheitlicht
  (`platform-and-paths.md` §6) — pro Plattform die passende Referenz (`mcp-windows.json` /
  `mcp-macos.json`) pflegen, nicht die jeweils andere ueberschreiben.
- **Nur eigene Dateien committen** — bei parallelen Sessions niemals `git add -A`/`git add .`.
- **Externe/installierte Plugins NICHT spiegeln/uebersetzen** — nur EIGENE Komponenten.

### KEINE Spiegel der eigenen Claude-Komponenten (separate CLI-Welten — NICHT mitpflegen)

- `~/proggs/gemini-setup/` — Gemini-CLI-Setup (eigenes Skill-/Command-Set)
- `~/proggs/codex-setup/` — Codex control plane (eigene Plugins)

Diese haben ihre eigene Logik und werden bei Claude-Skill-Aenderungen NICHT angefasst.

---

## Warum das noetig ist (Vorfall 2026-06-16)

Beim Pruefen, ob alle Skills plattformuebergreifend aktuell gespiegelt sind, fehlten in
`Umgebung/Skills/` fuenf aktive Skills (session, aufgaben-bruecke, aufgaben-visualizer,
almanach-update, best-practices-update) und vier waren veraltet — obwohl `claude-code-setup/`
gepflegt war. `Umgebung/` (und sein `Hooks/`, `Plugins/`) ist die "leicht vergessen"-Falle.
Behoben per #46859. Diese Regel macht das Nachziehen BEIDER Spiegelorte zur festen Pflicht,
damit der zweite Spiegel nie wieder hinterherhinkt.

---

## Was NIEMALS passieren darf

- ❌ Einen Skill/Hook/Plugin/Agent/Command/MCP aendern oder neu bauen, ohne BEIDE
  zustaendigen Spiegelorte nachzuziehen
- ❌ Nur `claude-code-setup/` pflegen und `Umgebung/` vergessen (oder umgekehrt)
- ❌ Bei einem Hook nur die `.ps1`- oder nur die `.sh`-Variante spiegeln
- ❌ Eine veraltete/gekuerzte Kopie spiegeln statt 1:1 des aktuellen Stands
- ❌ Die Spiegelung "auf spaeter" verschieben — sie gehoert VOR den Aufgabenabschluss
- ❌ Die Spiegelung uncommitted/ungepusht liegen lassen
- ❌ `__pycache__` oder `learned/` mit ins Repo spiegeln
- ❌ `.mcp.json` plattformuebergreifend "vereinheitlichen" (bricht die jeweils andere Plattform)
- ❌ gemini-setup/codex-setup als Claude-Spiegel mitpflegen

---

## Autoritaet dieser Regel

Diese Datei (`~/.claude/rules/harness-mirror-on-change.md`) wird automatisch in jeder Session
geladen. KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwaechen.

---

## Zusammenspiel mit anderen Regeln

| Regel / System | Zusammenspiel |
|----------------|--------------|
| CLAUDE.md "Cross-Platform & Config-Sync" | Diese Regel konkretisiert die Komponenten-Spiegelung und ergaenzt den zweiten Spiegelort `Umgebung/` |
| `cross-platform` / `git-workflow.md` | Liefert den Push-Mechanismus (fetch+rebase, nur eigene Dateien) |
| Memory `reference_skill_mirror_locations` | Operative Kurznotiz zu den Spiegelorten |
| `bugs/claude-tooling/claude-config.md` §2.5 | Begruendung: `~/.claude/` liegt nicht im Repo |
| `platform-and-paths.md` §6 | `.mcp.json` plattformspezifisch — nie automatisch vereinheitlichen |
