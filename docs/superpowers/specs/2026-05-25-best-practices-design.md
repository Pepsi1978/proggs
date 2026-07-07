# Best-Practices Skill — Design-Spec (Teil 1)

- **Datum:** 2026-05-25
- **Status:** Entwurf zur Freigabe
- **Skill-Slug:** `best-practices`

---

## Zweck

Eine laufend gepflegte Wissensbasis über die **offiziell sich weiterentwickelnden
Claude-Code-Werkzeuge** (den "Harness") und — wichtiger noch — **wie man sie am besten
anwendet** (Best Practices, nicht nur "was ist neu").

Kerngedanke: Mein Wissen über meine eigenen Werkzeuge ist auf den Trainingsstand
eingefroren, Claude Code entwickelt sich aber ständig weiter. Dieser Skill schließt
die Lücke — reaktive Recherche wird zu proaktivem, persistentem Wissen, das beim
nächsten Mal schon bereitliegt (Compound Intelligence Effect, Direktive #1).

---

## Gesamtkontext: Zweiteilung

| Teil | Form | Aufgabe | Status |
|------|------|---------|--------|
| **Teil 1** | Skill `best-practices` | Wissen **sammeln + strukturieren** + Auswertung mit direkten Verbesserungsvorschlägen | **dieses Dokument** |
| **Teil 2** | späteres Plugin "Harness-Intelligenz" | **Nachdenken über** das Wissen: Meta-Analyse über den ganzen Ordner, Mustererkennung, Zukunftsprojektion, Werkzeug-Audit + Umsetzung (nur als Vorschlag) | später, eigene Spec |

Teil 1 **produziert** die Wissensbasis, Teil 2 **konsumiert** sie. Teil 2 ist der
riskante Teil (fasst eigene Werkzeuge an) — wird erst gebaut, wenn Teil 1 sich bewährt
hat, und immer nur als Vorschlag (ACE-Schutzzonen).

---

## Festgelegte Entscheidungen (aus dem Brainstorming)

| Thema | Entscheidung |
|-------|--------------|
| Name | `best-practices` |
| Trigger | **Rein manuell** (`/best-practices`). Kein Cron, kein Hintergrundlauf. |
| Delta-Anker | Merkdatei `_state.json` ("zuletzt geprüft: Version X / Datum Y") → jeder Lauf recherchiert nur den Zuwachs. Option für erzwungenen Volllauf. |
| Quellen | **Offiziell zuerst** (Anthropic = Grundwahrheit), extern als Ergänzung (gelabelt, nur als abwägbare Alternative, nie überstimmend). |
| Ausgabe | Wissen speichern + **strukturierte Auswertung mit direkten Verbesserungsvorschlägen**, inkl. leichtem Flag, wenn ein eigenes Werkzeug dadurch veraltet wirkt. |
| Researcher | Claude Sonnet 4.6, Effort X-High, **max 130k Token je Researcher**, Continuation statt Absturz. |

---

## Taxonomie (11 Kategorien, selbst-erweiternd)

| # | Kategorie | Inhalt |
|---|-----------|--------|
| 1 | Hooks | Events, Schema, Exit-Codes, Neuerungen |
| 2 | Skills | SKILL.md-Format, Trigger, References, skill-creator |
| 3 | Subagents / Agents | Agent-Tool, Definitionen, Modellwahl, Parallelisierung |
| 4 | Plugins | plugin.json, Struktur, Marketplace, Komponenten |
| 5 | MCP-Server | .mcp.json, Konfiguration, lohnende Server, Resources/Tools |
| 6 | Slash-Commands | Command-Format, Argumente |
| 7 | Settings & Konfig | settings.json, Permissions, Env-Vars, Statusline, Output-Styles |
| 8 | Kontext-Management | Compaction, Microcompact, Autocompact-Schwellen, Memory |
| 9 | Token- & Kosten-Effizienz | Caching, Modellwahl, Effort-Levels, Parallelisierungs-Ökonomie |
| 10 | Arbeitsweise / Verhalten | Wie man Fragen angeht, Multi-Task, Planung, TDD |
| 11 | **Neues / Horizont-Scan** | Auffangzone für alles, was in 1–10 nicht passt. Wächst zu eigenen Kategorien (12, 13, 14…) heran, wenn es sich als wichtig erweist. |

Kategorien ab 12 werden bei Bedarf vom Skill selbst angelegt (selbst-erweiternde Taxonomie).

---

## Wissensbasis-Struktur (im Repo, plattformübergreifend)

```
best-practices/
├── README.md              ← Inhaltsverzeichnis, verlinkt jede Kategorie (Schnellzugriff)
├── _state.json            ← Delta-Anker: "zuletzt geprüft: Version X, Datum Y"
├── _changelog-archiv.md   ← ALLE offiziellen Changelog-Einträge, chronologisch (Master-Zeitleiste)
├── 01-hooks/
│   └── best-practices.md  ← aktuelle Best Practices für Hooks
├── 02-skills/
├── 03-agents/
│   … (04–10) …
├── 11-neues/
└── 12-… / 13-…            ← bei Bedarf neu angelegt
```

**Jeder Eintrag trägt: Quelle (URL) + Datum + Flag `offiziell`/`extern`.** Damit ist
die Aktualität jederzeit abwägbar. Regel: Nichts Neueres gefunden → letzter Eintrag
gilt weiter als aktuell.

Liegt bewusst **im Repo** (`~/proggs/best-practices/`), damit er nach macOS mit-synct.

---

## Ablauf eines Laufs (`/best-practices`)

1. `_state.json` + `README.md` lesen → "was kenne ich schon, welche Version zuletzt?"
2. Offizielles Claude-Code-Changelog holen → **Delta seit letztem Lauf** berechnen.
3. **Nichts Relevantes neu?** → "Nichts Neues seit Version X" melden, fertig (billig).
4. **Delta vorhanden?** → parallele `researcher`-Agenten (max ~5 gleichzeitig). Jeder pro
   Kategorie/Bereich: *WAS* hat sich geändert (offizielles Changelog) + *WIE* wendet man
   es am besten an (Anthropic-Docs/Blog) + externe Alternativen (sekundär, gelabelt).
   Unpassendes → Kategorie 11.
5. Ergebnisse destillieren → Kategorie-`best-practices.md` aktualisieren, `_changelog-archiv.md`
   chronologisch ergänzen, `README.md` + `_state.json` aktualisieren.
6. **Strukturierte Auswertung** an Frank ausgeben (siehe unten).

---

## Researcher-Regeln (KRITISCH)

- Modell: **Claude Sonnet 4.6**, Effort **X-High**.
- **Max 130.000 Token pro Researcher.** Kein Researcher darf darüber hinaus laufen.
- **Scope klein schneiden:** Jeder Researcher bekommt einen klar begrenzten Bereich,
  der deutlich unter 130k bleibt.
- **Checkpoint / Continuation (Absturz-Schutz):** Der Researcher schreibt seinen Fortschritt
  **inkrementell** in die Kategorie-Datei und endet mit einem Checkpoint-Marker. Ist er nicht
  fertig, startet der Orchestrator (der Skill) einen **Continuation-Researcher am Checkpoint** —
  so geht kein Fortschritt verloren und nichts stürzt ab.
- Zusätzlich bestehende Researcher-Regel: max 50 Ergebnisse / 15 Web-Fetches / 10 Min je Lauf.

**Offener Punkt für die Planung:** Der genaue Mechanismus, X-High-Effort und das 130k-Limit
*pro Subagent* technisch durchzusetzen, muss in der Implementierungsplanung verifiziert werden
(Subagents messen ihren Token-Verbrauch nicht live selbst — die Durchsetzung erfolgt über
Scope-Begrenzung + Checkpoint, nicht über Selbstmessung).

---

## Verlässlichkeits-Leitplanken

- Offiziell = Grundwahrheit; extern nur als `extern`-gelabelte Alternative, nie überstimmend.
- Jede Behauptung mit Quelle + Datum (Halluzinations-Schutz, Direktive #3).
- **Teil 1 fasst NIE ein Werkzeug an** — liest/schreibt nur den `best-practices/`-Ordner
  und schlägt vor.

---

## Auswertung / Ausgabeformat (Schritt 6)

Eine gut strukturierte, übersichtliche Auswertung am Ende des Laufs:

- **Was hat sich geändert** (seit letztem Lauf), gruppiert nach Kategorie.
- **Direkte Verbesserungsvorschläge** — konkret benannt, sofort umsetzbar.
- **Leichtes Flag eigener Werkzeuge**, die durch eine Neuerung veraltet wirken
  ("Hook X könnte überholt sein" — nur Hinweis, keine Analyse/Änderung; die gehört in Teil 2).
- Ausgegeben im bestehenden **Intelligenz-Vorschlags-Format**.

---

## Abgrenzung — bewusst NICHT in Teil 1

- Tiefe Meta-Analyse über den ganzen Ordner, Mustererkennung, Zukunftsprojektion → Teil 2.
- Werkzeug-Audit + Umsetzung von Verbesserungen → Teil 2 (Teil 1 darf nur leicht flaggen).
- Cron / Scheduling → entfällt (manuell).

---

## Plattform

Skill = Markdown (SKILL.md), Wissensbasis = Markdown im Repo. **Keine Hooks, kein Cron**
→ automatisch plattformübergreifend (macOS + Windows), ohne `.ps1`/`.sh`-Gegenstücke.

---

## Wiederverwendung (nicht doppelt bauen)

- Nutzt den vorhandenen `researcher`-Agenten (Sonnet) für parallele Web-Recherche.
- Der Skill selbst wird via `skill-creator` gebaut (CLAUDE.md-Pflicht).
- Grenzt sich klar ab von `claude-delta-scanner` (scannt *eigene* Repo-Änderungen für Codex),
  `direktiven-recherche` (Direktiven, nicht Werkzeuge) und `/self-improve` (Umgebungs-Audit
  ohne externe CLI-Best-Practices).

---

## Offene Punkte für die Implementierungsplanung

1. X-High-Effort + 130k-Token-Limit pro Subagent — technische Durchsetzung verifizieren.
2. Genaue Changelog-Quelle festnageln (code.claude.com/docs vs. GitHub-Releases vs. In-Produkt-Changelog).
3. Format des Checkpoint-Markers für die Continuation-Logik.
