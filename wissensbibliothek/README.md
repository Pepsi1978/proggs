# Wissensbibliothek

Zentrales Archiv aller Recherchen, die in Sessions gemacht wurden.
Jede ausführliche Internet-Recherche, jeder Deep-Dive, jede systematische Auseinandersetzung
mit einem Thema landet hier — kategorisiert, durchsuchbar, dauerhaft abrufbar.

## Warum diese Bibliothek existiert

Wenn ein Researcher-Agent eine Stunde lang das Internet durchforstet hat, ist das
gewonnene Wissen wertvoll. Ohne Bibliothek geht es nach der Session verloren.
Mit Bibliothek kann es **Monate später** für ähnliche Aufgaben wiederverwendet werden —
ohne erneute Recherche, ohne Token-Kosten, ohne Wartezeit.

Das ist Compound Intelligence Effect in seiner reinsten Form: Was einmal
gelernt wurde, ist für immer verfügbar.

## Struktur

```
wissensbibliothek/
├── README.md              ← diese Datei (Anleitung)
├── INDEX.md               ← Master-Index ALLER Einträge, durchsuchbar
└── <kategorie>/
    ├── INDEX.md           ← Cluster-Index der Kategorie
    └── <unterkategorie>/
        ├── INDEX.md       ← Cluster-Index der Unterkategorie
        └── YYYY-MM-DD-titel.md  ← einzelner Eintrag
```

### Kategorien (wachsen organisch mit jedem Research)

| Kategorie | Was rein gehört |
|-----------|----------------|
| `android-compose/` | Jetpack Compose, Kotlin Android, Material Design 3, AGSL Shader, Compose-spezifische Performance |
| `ios-swift/` | Swift, AppKit, SwiftUI, iOS-/macOS-spezifische Themen |
| `web-frontend/` | TypeScript, React, CSS, Web-Animationen, Browser-APIs |
| `desktop/` | Windows WPF, .NET, WinUI, plattformübergreifende Desktop-Themen |
| `ki-llms/` | LLM-APIs (Anthropic, Gemini, Groq), Prompt-Engineering, RAG, Agent-Architektur |
| `tooling/` | CLIs, Build-Systeme, Git-Workflows, Editoren, Hooks |
| `algorithmen-konzepte/` | Plattformunabhängige algorithmische Themen, Datenstrukturen, Mathematik |
| `produkt-design/` | UX-Patterns, Marken-Identität, Marketing, Produktstrategie |

### Unterkategorien (Cluster) entstehen pro Use-Case

Innerhalb einer Kategorie gibt es Cluster nach Thema, z.B.:
- `android-compose/energy-effects/` — alles zu Strom-, Glow-, Particle-Effekten
- `android-compose/lazy-list-tricks/` — Performance-Patterns rund um LazyColumn
- `web-frontend/svg-animationen/` — SVG-spezifische Animationstechniken

## Schreibregeln für jeden Eintrag

Jede Datei ist ein eigenständiger, in sich abgeschlossener Wissens-Baustein.

### Pflicht-Frontmatter (YAML am Anfang)

```yaml
---
title: "Kurzer prägnanter Titel"
date: YYYY-MM-DD
source: research-agent | self-research | external-link | conversation
project_context: "Optional: in welchem Projekt entstand das"
tags: [tag1, tag2, tag3]
related: ["pfad/zu/anderem/eintrag.md"]
summary: "Ein bis zwei Sätze die das Kernergebnis zusammenfassen."
---
```

### Pflicht-Sektionen im Body

1. **Kontext** — was war die Ausgangsfrage, warum wurde recherchiert
2. **Kern-Ergebnisse** — die wichtigsten Erkenntnisse, ranked nach Wichtigkeit
3. **Code-Snippets/Beispiele** — wenn anwendbar, in Code-Fences
4. **Referenzen** — alle URLs als Markdown-Links am Ende

### Datei-Benennung

`YYYY-MM-DD-kebab-case-titel.md`

Beispiel: `2026-05-01-stromfluss-canvas-shader.md`

Datum vorne sorgt automatisch für chronologische Sortierung beim `ls`.

## Wie Claude diese Bibliothek nutzen soll

### Beim Session-Start

Wenn ein Thema aufkommt, das in der Bibliothek bereits behandelt sein könnte:
1. **`INDEX.md`** in passenden Kategorien lesen
2. Wenn relevanter Eintrag existiert: zuerst diesen lesen, **dann** entscheiden ob neue Recherche nötig
3. Bei verwandten Themen: alte Erkenntnisse in neue Recherche einbauen

### Nach jeder umfangreicheren Recherche

Wenn ein Researcher-Agent (oder mehrere parallel) substantielle Ergebnisse liefert:
1. Pro Researcher eine eigene Datei in der passenden Unterkategorie anlegen
2. Frontmatter ausfüllen
3. Bericht **wortgetreu** speichern (Zusammenfassungen verlieren Detail)
4. `INDEX.md` der Unterkategorie aktualisieren
5. Bei neuer Kategorie: auch Master-`INDEX.md` ergänzen

### Beim Wiederverwenden

In künftigen Sessions kann Claude direkt auf einen Eintrag verweisen:
> "Das Thema haben wir am 2026-05-01 ausführlich recherchiert, siehe
> `wissensbibliothek/android-compose/energy-effects/2026-05-01-stromfluss-canvas-shader.md`."

## Was NICHT in die Bibliothek gehört

- Triviale Lösungen die in 5 Minuten neu gefunden werden können
- Personenbezogene/projektspezifische Daten (gehören in MEMORY.md, nicht hierher)
- Quellcode-Snippets ohne Kontext (gehören in den Code selbst)
- Bug-Cases (gehören in `~/proggs/.claude/agent-memory/shared/bug-cases.jsonl`)

## Erstmal-Anlage

Die Bibliothek wurde am **2026-05-01** angelegt — Anlass war eine umfangreiche
5-Researcher-Recherche zum Energy-Board-Feature für die Frank-Tagebuch-App.
Die fünf Berichte sind die ersten Einträge und gleichzeitig der Beweis, dass
der Aufwand für die Bibliothek sich sofort lohnt.
