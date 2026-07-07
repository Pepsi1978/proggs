# Eval-Suite fuer den Uebersetzungs-Skill

Dieser Ordner enthaelt 6 realistische Test-Prompts zum Verifizieren dass der
Uebersetzungs-Skill nach Aenderungen weiterhin korrekt arbeitet. Schema-konform
nach `skill-creator/references/schemas.md` (Anthropic-Standard).

## Aufbau

```
evals/
├── evals.json                       ← 6 Test-Prompts mit Expectations
├── files/
│   └── sample_strings_de.xml        ← 12 typische deutsche Test-Strings
└── README.md                        ← diese Datei
```

## Wofuer ist das gut?

Wenn du den Skill aenderst (neue Sprache hinzufuegen, Validator anpassen,
Workflow umbauen), willst du nicht jedes Mal manuell 30 Sprachen durchprobieren.
Diese Test-Prompts decken die 6 wichtigsten Use-Cases ab — wenn die alle
funktionieren, funktioniert der Skill in der Praxis.

## Die 6 Test-Faelle

| # | Name | Use-Case |
|---|------|----------|
| 1 | Franzoesisch nur neue Strings | Der haeufigste Alltagsfall |
| 2 | Persisch (RTL + neue fa-Sprache) | RTL-Layout + Native-Digits-Validator fuer fa |
| 3 | Japanisch (CJK + full-width Punctuation) | CJK-Punctuation-Validator |
| 4 | Tamil mit Laengen-Warnung | Indische Sprache + Length-Pacing-Validator |
| 5 | Portugiesisch beide Varianten | Bidirektionaler PT-Varianten-Check |
| 6 | Komplettlauf 30 Locales | Stresstest des gesamten Workflows |

## Test-Eingabe-Datei

Alle 6 Tests nutzen `files/sample_strings_de.xml` als Quell-Strings. Diese Datei
enthaelt 12 typische deutsche Strings:

- Brand-Name (`BestJournal`) mit `translatable="false"` — soll NICHT uebersetzt werden
- Kurze Button-Texte (`Speichern`, `OK`) — testen Laengen-Pacing
- Apostroph-Risiko-Strings (`Willkommen zurueck`, `Schreibe heute...`) — bei FR-Uebersetzung
  entstehen Apostrophe (`l'application`) die der Apostroph-Validator escapen muss
- Format-Platzhalter (`%1$s`, `%2$d`) — muessen EXAKT erhalten bleiben
- Plural-Quantities — testen CLDR-Plural-Formen
- XML-Sonderzeichen (`&amp;`) — testen korrektes Escaping

## Schema-Konformitaet

Die `evals.json` folgt dem offiziellen Skill-Creator-Schema:

```json
{
  "skill_name": "übersetzung",
  "evals": [{
    "id": 1,
    "prompt": "...",
    "expected_output": "...",
    "files": ["evals/files/sample_strings_de.xml"],
    "expectations": [
      "Statement 1 das vom Grader verifiziert werden kann",
      "Statement 2 das vom Grader verifiziert werden kann"
    ]
  }]
}
```

Wichtig:
- `expectations` sind einfache Strings (nicht Objekte) — der Grader gibt sie zurueck
  mit zusaetzlichem `passed`/`evidence`, aber INPUT-seitig sind sie Strings
- `files` ist eine Liste relativer Pfade ab Skill-Root
- Pro Test 4-6 Expectations — discriminating (passen NICHT bei kaputter Skill-Version)

## Wie ausfuehren?

### Manuell (Vibe-Check)

Lies einen Test-Prompt aus `evals.json`, kopiere ihn in Claude Code, und schau ob
der Skill korrekt anspringt und die Expectations erfuellt.

### Automatisiert (skill-creator)

Der `skill-creator:skill-creator` Skill von Anthropic kann diese evals.json
einlesen und alle 6 Tests als Subagents parallel laufen lassen mit Vorher/Nachher-
Vergleich. Workflow:

```
1. /skill-creator:skill-creator aufrufen
2. Skill auf "übersetzung" zeigen lassen
3. evals/evals.json wird automatisch eingelesen
4. Subagents laufen parallel, Outputs werden in einem Browser-Viewer verglichen
5. Du markierst pro Test PASS/FAIL und schreibst Feedback
6. Bei FAIL: skill-creator schlaegt Verbesserungen vor
```

### Schema-Validierung

Pruef das Format der evals.json vor Commits:

```bash
python3 -c "import json; d=json.load(open('evals/evals.json',encoding='utf-8')); \
  assert 'skill_name' in d; \
  assert 'evals' in d and len(d['evals']) > 0; \
  [assert isinstance(e['expectations'], list) and all(isinstance(s, str) for s in e['expectations']) for e in d['evals']]; \
  print('Schema OK')"
```

## Wann ausfuehren?

- **Vor groesseren Skill-Aenderungen**: Baseline-Lauf
- **Nach Skill-Aenderungen**: Vergleichs-Lauf gegen Baseline
- **Vor Plugin-Updates** (claude-mem, skill-creator, etc.): Sanity-Check dass
  externe Aenderungen den Skill nicht brechen
- **Bei Verdacht auf Regression**: Schnell-Check welche Tests jetzt fehlschlagen

## Was die Tests NICHT pruefen

- **Uebersetzungs-Qualitaet selbst** — ob "salvar" wirklich besser ist als "guardar"
  fuer einen brasilianischen User. Das ist subjektiv und nur durch Native-Speaker
  pruefbar. Die Tests pruefen ob der Skill die richtigen Werkzeuge benutzt, nicht
  ob das LLM perfekt uebersetzt.
- **Performance** — wie schnell der Skill ist. Dafuer braucht es eigene Benchmarks.
- **Edge-Cases** — z.B. strings.xml mit 5000 Strings oder mit CDATA-Bloecken.
  Wenn das wichtig wird: weitere Tests in evals.json ergaenzen.

## Aktualisierung

Wenn du eine neue Sprache hinzufuegst oder einen Validator aenderst:
1. Pruefe ob ein bestehender Test betroffen ist
2. Ergaenze ggf. einen neuen Test (z.B. `vietnamese-tone-marks`)
3. Erhoehe die `id` fortlaufend
4. Lass die `sample_strings_de.xml` falls moeglich unangetastet (Baseline-Stabilitaet)
