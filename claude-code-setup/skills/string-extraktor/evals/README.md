# Evals fuer den string-extraktor Skill

Dieses Verzeichnis enthaelt automatisierte Tests fuer den Skill. Sie pruefen
dass aenderungen an der SKILL.md oder den References nicht versehentlich
funktionierende Faelle kaputt machen (Regressions-Tests).

## Was ist hier drin

- `eval-set.json` — 3 Test-Szenarien (Simple-Screen, Plurals+Platzhalter, Enum-Trap)
- `README.md` — Diese Datei

## Wann die Evals laufen lassen

**Pflicht-Faelle:**
- Nach JEDER Aenderung an SKILL.md
- Nach Aenderungen an einer der References (`deutsche-sprache.md`,
  `string-best-practices.md`, `funktions-check.md`, `praevention-setup.md`,
  `escape-formen.md`, `ast-scanning.md`)
- Nach Aenderungen am `scan-strings.sh` oder `validate_extracted.py`

**Optionale Faelle:**
- Vor groesseren Modell-Upgrades (z.B. Opus 4.6 -> 4.7) — zeigt ob das Modell
  den Skill noch gleich gut versteht
- Beim Onboarding neuer Beitragender — als Qualitaets-Baseline

## Wie laufen lassen

### Manuell (einfacher)

Pro Eval einen Skill-Aufruf mit den gemockten Dateien:

```bash
# Im Test-Verzeichnis die Eingabe-Dateien anlegen
mkdir -p /tmp/eval-test/app/src/main/java/com/example
mkdir -p /tmp/eval-test/app/src/main/res/values
# (Dateien aus eval-set.json kopieren)

# Dann Claude mit dem Skill aufrufen
cd /tmp/eval-test
claude -p "Extrahiere alle hardcodierten Strings aus dem SettingsScreen..."

# Resultat gegen erwartetes Verhalten pruefen
```

### Automatisiert (skill-creator)

Wenn das skill-creator Eval-Tool installiert ist:

```bash
python -m scripts.run_loop \
  --eval-set ~/.claude/skills/string-extraktor/evals/eval-set.json \
  --skill-path ~/.claude/skills/string-extraktor \
  --model claude-opus-4-7 \
  --max-iterations 1 \
  --verbose
```

Output landet als HTML-Report im Browser.

## Die 3 Test-Szenarien

### Eval 1: simple-compose-screen
Ein typischer SettingsScreen mit 5 hardcodierten Strings.
**Was getestet wird:**
- Erkennt der Skill alle 5 Strings?
- Erstellt er korrekt snake_case-Keys?
- Halt er die Du-Form aus dem Sprach-Kontext ein?
- Markiert er das Email-Beispiel als `translatable="false"`?

### Eval 2: plurals-and-placeholders
JournalScreen mit if/else-Konstrukt fuer Plurals und Template-Strings.
**Was getestet wird:**
- Erkennt der Skill das if/else als Plural-Kandidat?
- Erstellt er korrekte `<plurals>` mit `one` + `other` (CLDR-Deutsch)?
- Nummeriert er Platzhalter (`%1$s` nicht `%s`)?
- Setzt er XLIFF-Tags mit `id` + `example`?
- Wechselt der Code zu `pluralStringResource()`?

### Eval 3: enum-displayname-trap
Mood-Enum dessen `displayName` sowohl fuer UI als auch fuer DB verwendet wird.
**Was getestet wird:**
- Erkennt der Skill das Phase-5.2-Risiko (Daten-Verlust durch Lokalisierung)?
- Schlaegt er die `@StringRes`-Refactoring vor?
- Warnt er vor `enum.name` statt `displayName` fuer DB-Speicherung?
- Fuehrt er die Phase-5-Pruefung automatisch durch?

## Verhalten bei Eval-Fehlschlag

Wenn ein Eval fehlschlaegt:

1. **Erst manuell pruefen** ob der Eval selbst noch valide ist (z.B. Compose-API hat sich geaendert)
2. **Skill-Aenderung rueckgaengig machen** (wenn ein Regressions-Fall ist)
3. **Skill anpassen** wenn der Eval ein bisher uebersehenes Verhalten verlangt
4. **Eval anpassen** wenn der Skill jetzt einen besseren Weg geht (selten)

## Erweiterung der Eval-Suite

Wenn beim normalen Skill-Einsatz ein Bug oder Edge-Case entdeckt wird der
nicht abgedeckt ist:

1. Neuen Eval-Eintrag in `eval-set.json` anlegen (id hochzaehlen)
2. Eingabe-Dateien minimal halten (nur was den Fall isoliert testet)
3. `expected_behavior` praezise formulieren
4. `assertions` quantitativ formulieren (pattern_exists, count_gte, absence, manual_check)
5. Einmal lokal laufen lassen, dann committen

So waechst der Skill bei jedem neuen Erkenntnisgewinn an Qualitaets-Sicherung.
