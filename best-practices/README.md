# Best-Practices — Harness-Wissensbasis

Laufend gepflegtes Wissen darueber, **wie man die Claude-Code-Werkzeuge am besten benutzt**,
und was sich mit neuen Versionen geaendert hat. Gepflegt vom `best-practices`-Skill (Teil 1).

- **Quellen-Rangordnung:** Offiziell (Anthropic) = Grundwahrheit, extern = gelabelte Alternative.
- **Jeder Eintrag traegt:** Quelle (URL) + Datum + `offiziell`/`extern`-Flag.
- **Stand:** siehe `_state.json` (`last_version` / `last_checked`).
- **Master-Zeitleiste aller Aenderungen:** `_changelog-archiv.md`.

## Inhaltsverzeichnis (Kategorien)

| # | Kategorie | Datei |
|---|-----------|-------|
| 1 | Hooks | [01-hooks/best-practices.md](01-hooks/best-practices.md) |
| 2 | Skills | [02-skills/best-practices.md](02-skills/best-practices.md) |
| 3 | Agents | [03-agents/best-practices.md](03-agents/best-practices.md) |
| 4 | Plugins | [04-plugins/best-practices.md](04-plugins/best-practices.md) |
| 5 | MCP-Server | [05-mcp/best-practices.md](05-mcp/best-practices.md) |
| 6 | Slash-Commands | [06-commands/best-practices.md](06-commands/best-practices.md) |
| 7 | Settings & Konfig | [07-settings/best-practices.md](07-settings/best-practices.md) |
| 8 | Kontext-Management | [08-kontext/best-practices.md](08-kontext/best-practices.md) |
| 9 | Token- & Kosten-Effizienz | [09-token-effizienz/best-practices.md](09-token-effizienz/best-practices.md) |
| 10 | Arbeitsweise / Verhalten | [10-arbeitsweise/best-practices.md](10-arbeitsweise/best-practices.md) |
| 11 | Researcher & Internet-Recherche | [11-researcher/best-practices.md](11-researcher/best-practices.md) |
| 12 | Neues / Horizont-Scan | [12-neues/best-practices.md](12-neues/best-practices.md) |

> "Neues" (Kategorie 12) ist die Auffangzone und bleibt IMMER die letzte Kategorie. Kommt eine
> neue definierte Kategorie dazu, wird sie davor eingefuegt und "Neues" rueckt eine Nummer nach
> hinten (Ordner entsprechend umbenannt).

## Projekt-Code (zweite Seite der Medaille zum Bug-Almanach)

Neben den Harness-Kategorien oben gibt es die Sektion **[`projekt-code/`](projekt-code/README.md)**
fuer die Software/Sprachen, die in den Projekten benutzt werden (Kotlin, Swift, Gradle,
.NET/WPF, TypeScript, Rust …). Seit 2026-06-03 sind die Software-Ordner **nach Kategorie
gruppiert** (`projekt-code/<kategorie>/<software>/`, z.B. `android/kotlin/`) — dieselben
Kategorien wie der Bug-Almanach (`bugs/<kategorie>/`). Entsteht bei Bedarf.

- Quelle hier: der **eigene** Changelog der Software (nicht der Claude-Code-Changelog).
- Versions-Anker: die live ermittelte installierte Version der Software.
- Gepflegt vom `bug-almanach-recherche`-Skill (Praevention pro Bug) und vom
  `best-practices`-Skill (gezielter Lauf "nur fuer <software>").
- Gegenstueck: `~/proggs/bugs/<bereich>.md` sammelt die Bugs, hier steht, wie man sie
  von vornherein vermeidet.
