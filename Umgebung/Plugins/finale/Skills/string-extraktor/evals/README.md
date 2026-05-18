# Evals fuer den string-extraktor Skill

Dieses Verzeichnis enthaelt automatisierte Tests fuer den Skill. Sie pruefen
dass Aenderungen an der SKILL.md oder den References nicht versehentlich
funktionierende Faelle kaputt machen (Regressions-Tests).

## Was ist hier drin

- `eval-set.json` — 6 Test-Szenarien, decken die Phasen 0, 1, 2, 3, 5 ab
- `README.md` — Diese Datei

## Assertion-Types

Das Eval-Schema nutzt einheitlich diese 5 Types:

| Type | Bedeutung |
|------|-----------|
| `pattern_exists` | Pattern muss mindestens 1x vorkommen |
| `pattern_count_gte` | Pattern muss mindestens N-mal vorkommen (Feld `target`) |
| `pattern_all` | ALLE Matches muessen valide sein (z.B. snake_case fuer alle Keys) |
| `absence` | Pattern darf NICHT vorkommen (z.B. unnummerierte `%s`) |
| `manual_check` | Subjektive Bewertung — Feld `criterion` beschreibt was erwartet wird |

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

## Die 6 Test-Szenarien

### Eval 1: simple-compose-screen (Phase 3-CREATE)
SettingsScreen mit 5 hardcodierten Strings. Testet ob Skill alle Strings findet,
snake_case-Keys erstellt, Du-Form einhaelt und Email-Beispiel als `translatable="false"` markiert.

### Eval 2: plurals-and-placeholders (Phase 3-CREATE)
JournalScreen mit if/else-Plural-Konstrukt und Template-Strings. Testet Plural-
Erkennung, CLDR-konformes `one`/`other`, Platzhalter-Nummerierung und XLIFF-Tags.

### Eval 3: enum-displayname-trap (Phase 5-FUNKTIONS-CHECK)
Mood-Enum mit displayName fuer UI UND Room. Testet ob Skill das Phase-5.2-Risiko
erkennt und @StringRes-Refactoring + enum.name fuer DB-Speicherung vorschlaegt.

### Eval 4: unicode-escape-detection (Phase 1-SCAN)
DashboardScreen mit 4 Unicode-Escape-Umlauten (`Ü`, `Müll`, `Grüße`, `Straße`).
Testet ob Skill diese versteckten Umlaute findet und in echte ä/ö/ü umwandelt.

### Eval 5: existing-xml-audit (Phase 2-AUDIT)
Bestehende strings.xml mit 5 Qualitaets-Issues: unnummerierte Platzhalter, URL
ohne translatable=false, veraltete Rechtschreibung (muß/daß), Duplikate, Denglisch.
Testet automatische Fixes und Benutzer-Meldung fuer subjektive Entscheidungen.

### Eval 6: language-context-detection (Phase 0-SPRACH-KONTEXT)
strings.xml mit Du/Sie-Mix und Partizip+Neutral-Gender-Strategie. Testet ob
Skill die Inkonsistenz erkennt UND die Gender-Strategie korrekt klassifiziert.

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
