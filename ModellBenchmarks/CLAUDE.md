# ModellBenchmarks

Statische Vergleichsseite für KI-Modell-Benchmarks. Live als Claude-Artifact:
https://claude.ai/artifact/6KDkbDgGsZQNjCttu4ngUm

## Dateien
- `index.html` – Gerüst und CSS (hell/dunkel über Tokens, Schalter Automatisch/Hell/Dunkel)
- `data.js` – ALLE Daten: `models`, `categories`, `sources`, `series`, `aa` (Preis-Leistungs-Diagramm)
- `erklaerungen.js` – ausklappbare Erklärung je Benchmark (Schlüssel = Feld `info` der Zeile), Niveau 10. Klasse, ca. 10 Zeilen
- `app.js` – Rendering, nur ändern, wenn sich die Darstellung ändern soll

## „Bau Modell X ein“
1. Werte über den Skill `research` holen (Frage 1 zur Engine stellen). Quellen: System Cards, Artificial Analysis, Vals.ai, arena.ai.
2. In `data.js` → `models` einen Eintrag ergänzen (id, name, short, vendor, released, apiId, Preise, context, `color: [hell, dunkel]`, `shape`). Neue Farbe mit dem dataviz-Validator gegen beide Oberflächen prüfen.
3. Werte in die passenden `series` eintragen. **Regel:** eine Serie = eine Quelle + eine Benchmark-Version + eine Messreihe. Misst eine weitere Quelle denselben Benchmark in derselben Version, bekommt sie eine eigene Serie mit gleichem `group`-Schlüssel. Die Seite zeigt dann EINE Zeile mit Quellen-Schalter. Andere Version (z. B. Terminal-Bench 2.1 statt 4.0) = eigene Gruppe. Abweichender Effort als `{ v, variant, note }`.
   **Nur seriöse Quellen:** Hersteller-System-Cards und etablierte unabhängige Testlabore (Artificial Analysis, Vals.ai, LMArena). Keine Aggregatoren (BenchLM, llm-stats o. ä.) und keine Suchmaschinen-Zusammenfassungen.
4. Neuer Benchmark → neue Zeile mit `stars` (1–5, Wichtigkeit in der Branche: 5 = Leitwert, den Hersteller und unabhängige Tester nutzen, aktuell und nicht ausgereizt; 2 = veraltet/ausgereizt; 1 = Nische) plus Erklärung in `erklaerungen.js`. Die Seite sortiert jeden Bereich automatisch nach Sternen.
5. `version` und `stand` oben in `data.js` hochzählen (Zeit per `Get-Date -Format "dd.MM.yyyy HH:mm"`).

## Deploy
Artifact-Kopie ohne `<!doctype>/<html>/<head>/<body>`-Zeilen erzeugen und mit den drei JS-Dateien als `files` an die URL oben veröffentlichen (Artifact-Tool, `url` angeben, wenn die Sitzung das Artifact nicht selbst veröffentlicht hat).

## Version
Sichtbar in der Fußzeile, Quelle ist `version` + `stand` in `data.js` (Form `2.0.0`).
