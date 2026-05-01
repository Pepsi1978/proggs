# Master-Index der Wissensbibliothek

Zentrale Übersicht aller Einträge — chronologisch und nach Kategorie sortiert.

Bei jeder neuen Datei in der Bibliothek **muss** dieser Index ergänzt werden.

## Suchen

- **Per Kategorie**: Tabelle weiter unten durchgehen
- **Per Tag**: `grep -r "tags:.*<TAG>" wissensbibliothek/`
- **Per Titel**: `grep -r "^title:" wissensbibliothek/`
- **Per Volltext**: `grep -ri "<begriff>" wissensbibliothek/`

## Alle Einträge (chronologisch absteigend)

| Datum | Titel | Kategorie | Tags |
|-------|-------|-----------|------|
| 2026-05-01 | [Overlay über LazyColumn — Performance-Architektur](android-compose/energy-effects/2026-05-01-overlay-lazycolumn-performance.md) | android-compose/energy-effects | compose, lazycolumn, layoutinfo, withframenanos, performance |
| 2026-05-01 | [Multi-Layer-Glow & Bloom — Profitechniken in Compose Canvas](android-compose/energy-effects/2026-05-01-multi-layer-glow-bloom.md) | android-compose/energy-effects | compose, canvas, glow, bloom, blendmode, blurmaskfilter, vfx |
| 2026-05-01 | [Energy-/Electric-UI — Inspirationen und DNA-Analyse](android-compose/energy-effects/2026-05-01-energy-ui-inspirationen.md) | android-compose/energy-effects | inspiration, ui-design, cyberpunk, iron-man, tesla, lichtenberg, biolumineszenz |
| 2026-05-01 | [Particle-Spark-Systeme für Compose Canvas](android-compose/energy-effects/2026-05-01-particle-funken-systeme.md) | android-compose/energy-effects | compose, canvas, particles, withframemillis, snapshotstatelist, object-pool |
| 2026-05-01 | [Stromfluss in Compose Canvas — DashPath, AGSL, Gradient](android-compose/energy-effects/2026-05-01-stromfluss-canvas-shader.md) | android-compose/energy-effects | compose, canvas, agsl, pathEffect, brush, animation |

## Kategorien-Übersicht

### android-compose/

[Kategorie-README](android-compose/README.md)

| Cluster | Eintragszahl | Zuletzt aktualisiert |
|---------|--------------|---------------------|
| [energy-effects](android-compose/energy-effects/INDEX.md) | 5 | 2026-05-01 |

### recht/

[Kategorie-README](recht/README.md) · [Vollständiger Index](recht/INDEX.md)

Bündelt alle rechtlichen Dokumente die im Repo verteilt liegen — verlinkt
auf Originale ohne sie zu kopieren. Enthält:

| Quelle | Inhalt |
|--------|--------|
| `rechtssicherheit.md` (Repo-Root) | Zentrale Wissensbasis für rechtliche Anforderungen |
| `BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-*.md` | 8 iterative Compliance-Audits, neueste v7 (2026-04-28) |
| `BestJournalAndroid/app-roentgen-AUDIT-2026-05-01.md` | App-Röntgen-Compliance-Audit |
| `BestJournalAndroid/app-roentgen-WERBEAUSSAGEN-FIX-2026-05-01.md` | UWG §5 Werbeaussagen-Audit (führte zu Commit #1950) |

### Noch keine Einträge

- ios-swift/
- web-frontend/
- desktop/
- ki-llms/
- tooling/
- algorithmen-konzepte/
- produkt-design/

Diese Kategorien existieren als Konzept (siehe `README.md`) und werden mit dem
ersten Eintrag automatisch angelegt.

## Tag-Wolke (häufige Tags)

`compose` (5) · `canvas` (4) · `android` (5) · `performance` (3) ·
`animation` (3) · `inspiration` (1) · `vfx` (1) · `glow` (1) ·
`particles` (1) · `agsl` (1) · `recht` (verlinkt)

Aktualisiert bei jedem neuen Eintrag.
