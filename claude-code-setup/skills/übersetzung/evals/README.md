# Eval-Suite fuer den Uebersetzungs-Skill

Dieser Ordner enthaelt 6 realistische Test-Prompts zum Verifizieren dass der
Uebersetzungs-Skill nach Aenderungen weiterhin korrekt arbeitet.

## Wofuer ist das gut?

Wenn du den Skill aenderst (neue Sprache hinzufuegen, Validator anpassen,
Workflow umbauen), willst du nicht jedes Mal manuell 30 Sprachen durchprobieren.
Diese Test-Prompts decken die 6 wichtigsten Use-Cases ab — wenn die alle
funktionieren, funktioniert der Skill in der Praxis.

## Die 6 Test-Faelle

| # | Name | Use-Case |
|---|------|----------|
| 1 | `single-language-french` | Nur eine Sprache, nur neue Strings — der haeufigste Alltagsfall |
| 2 | `rtl-language-persian` | RTL-Sprache mit neuem Native-Digit-Validator (fa) |
| 3 | `cjk-japanese` | CJK-Sprache mit full-width-Konvertierung |
| 4 | `indian-tamil-with-shortening` | Indische Sprache mit Laengen-Pacing-Warnung |
| 5 | `portuguese-both-variants` | Beide PT-Varianten getrennt |
| 6 | `full-translation-30-locales` | Komplettlauf ueber alle 30 Locales |

## Wie ausfuehren?

### Manuell (Vibe-Check)

Lies einen Test-Prompt aus `evals.json`, kopiere ihn in Claude Code, und schau ob
der Skill korrekt anspringt und die Assertions erfuellt.

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

## Wann ausfuehren?

- **Vor groesseren Skill-Aenderungen**: Baseline-Lauf
- **Nach Skill-Aenderungen**: Vergleichs-Lauf gegen Baseline
- **Vor Plugin-Updates** (claude-mem, skill-creator, etc.): Sanity-Check dass
  externe Aenderungen den Skill nicht brechen
- **Bei Verdacht auf Regression**: Schnell-Check welche Tests jetzt fehlschlagen

## Assertions verstehen

Jeder Test hat 4 Assertions. Sie sind in zwei Kategorien:

- **`type: "behavior"`** — was der Skill TUT (Dateien laden, Scripts aufrufen, Commits)
- **`type: "content"`** — was im OUTPUT steht (Vokabular, Punctuation, Format)

Behavior-Assertions sind durch Beobachtung der Subagent-Logs pruefbar.
Content-Assertions sind durch Inspektion der erzeugten strings.xml pruefbar.

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
