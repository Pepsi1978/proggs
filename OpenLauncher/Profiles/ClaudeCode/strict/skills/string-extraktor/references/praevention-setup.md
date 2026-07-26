# Praevention-Setup — Neue hardcodierte Strings dauerhaft verhindern

> Diese Referenz ist die ausgelagerte Detail-Version von **Phase 6** des
> string-extraktor-Skills. Wird einmalig pro Projekt eingerichtet, NICHT bei
> jedem Skill-Lauf. Bei weiteren Skill-Laeufen pruefen ob die Guards noch
> aktiv sind.
>
> **Warum diese Phase existiert:** Eine perfekt extrahierte strings.xml ist
> nichts wert wenn beim naechsten Feature wieder hardcodierte Strings
> dazukommen. Diese Phase richtet automatische Guards ein, die Rueckfaelle
> verhindern. Lokalise und Crowdin empfehlen das uebereinstimmend als
> CI/CD-Standard 2025/2026.

---

## Inhalt

- 6.1 Pre-Commit-Hook (Git)
- 6.2 CI-Check (GitHub Actions)
- 6.3 Lint verschaerfen in `build.gradle.kts`
- 6.4 String-Freeze vor Releases
- 6.5 donottranslate.xml — Trennung uebersetzbar vs. nicht-uebersetzbar
- 6.6 Optional: Tolgee / Lokalise / Crowdin / Phrase Integration
- 6.7 Setup-Report-Format

---

## 6.1 Pre-Commit-Hook (Git)

Der eingebaute Hook im Skill ist eine Basisversion. Fuer voll erweiterte
Coverage das Skript `scripts/pre-commit-hook.sh` aus dem Skill-Verzeichnis
verwenden — es deckt alle 9 Patterns aus Phase 1.3 ab.

**Basisversion (Inline-Hook in `.git/hooks/pre-commit`):**

```bash
#!/bin/bash
# .git/hooks/pre-commit
STAGED=$(git diff --cached --name-only --diff-filter=ACM | grep '\.kt$')
ERR=0
for F in $STAGED; do
    HITS=$(git diff --cached "$F" | grep "^+" \
        | grep -E 'Text\("[A-Za-zAEOEUEaeoeuess ]{3,}"\)|title\s*=\s*"[A-Za-zAEOEUEaeoeuess ]{3,}"' \
        | grep -v '//\|stringResource\|@Suppress\|@Preview')
    if [ -n "$HITS" ]; then
        echo "Hardcoded strings in $F:"; echo "$HITS"; ERR=$((ERR+1))
    fi
done
[ $ERR -gt 0 ] && { echo "Bitte stringResource() verwenden."; exit 1; } || exit 0
```

**Vollversion** (deckt Toast, Snackbar, contentDescription, placeholder,
TopAppBar, error-Calls, Enum-Labels ab): siehe `scripts/pre-commit-hook.sh`.

**Hook aktivieren:**
```bash
cp scripts/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

---

## 6.2 CI-Check (GitHub Actions)

```yaml
name: i18n String Check
on: [pull_request]
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - run: |
          git diff origin/main...HEAD --name-only --diff-filter=ACM \
            | grep '\.kt$' \
            | xargs -I{} grep -nH 'Text(\s*"[^"]\{3,\}"' {} 2>/dev/null \
            | grep -v 'stringResource\|@Preview' | tee /tmp/hits.txt
          [ -s /tmp/hits.txt ] && exit 1 || exit 0
```

**Erweiterte CI-Version** (deckt alle 9 Patterns ab):

```yaml
- name: Extended hardcoded string check
  run: |
    bash scripts/pre-commit-hook.sh \
      --files "$(git diff origin/main...HEAD --name-only --diff-filter=ACM | grep '\.kt$')"
```

---

## 6.3 Lint verschaerfen in `build.gradle.kts`

```kotlin
android {
    lint {
        error("MissingTranslation")
        error("HardcodedText")
        warning("MissingQuantity")
        warning("ImpliedQuantity")
        warning("TypographyDashes")
        warning("TypographyQuotes")
        abortOnError = true
    }
}
```

**Wichtig — Compose-Limitation:** Der eingebaute `HardcodedText`-Lint findet
NUR XML-Layouts, nicht Kotlin/Compose. Fuer Compose-Coverage zusaetzlich
**Detekt** mit Custom-Rule einrichten — siehe `references/ast-scanning.md`.

---

## 6.4 String-Freeze vor Releases (wie Mozilla/Brave/Nextcloud)

1-2 Wochen vor Release: PRs die `strings.xml` aendern werden blockiert.
Uebersetzer bekommen garantierte Zeit. Das verhindert "neue Strings tropfen
in letzter Minute rein und sind dann nicht uebersetzt".

**Umsetzung als CI-Check:**
```yaml
- name: String freeze check
  if: github.base_ref == 'release' && env.IN_FREEZE == 'true'
  run: |
    if git diff origin/release...HEAD --name-only | grep -q 'strings.xml$'; then
      echo "ERROR: strings.xml changes blocked during string freeze"
      exit 1
    fi
```

`IN_FREEZE` wird manuell als Repository-Variable gesetzt wenn der Freeze beginnt.

---

## 6.5 donottranslate.xml — Trennung uebersetzbar vs. nicht-uebersetzbar

> **Warum diese Trennung existiert:** Eigennamen, URLs, technische IDs und
> Marken duerfen NICHT uebersetzt werden — sonst wird die App in Italien
> "InstaPic" zu "InstaImmagine". Profi-Workflows (Crowdin, Phrase, Lokalise)
> trennen das auf Datei-Ebene durch eine separate `donottranslate.xml`.
> Uebersetzer sehen diese Datei nicht und koennen die Werte nicht versehentlich
> anfassen.

### Was gehoert in donottranslate.xml

- App-Name (`app_name` — bleibt in jeder Sprache gleich)
- URLs (`url_privacy_policy`, `url_terms`, `url_support`)
- API-Endpunkte, technische IDs
- Marken-Namen, Eigennamen (Firmen, Personen)
- Email-Adressen, Telefonnummern
- Locale-Codes, Currency-Codes, Country-Codes
- Datenbankschluessel die im Code als Vergleich genutzt werden

### Datei-Struktur

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- res/values/donottranslate.xml -->
<resources xmlns:tools="http://schemas.android.com/tools"
           tools:ignore="MissingTranslation">

    <!-- App-Identity -->
    <string name="app_name" translatable="false">Mein Tagebuch</string>

    <!-- URLs -->
    <string name="url_privacy" translatable="false">https://example.com/privacy</string>
    <string name="url_terms" translatable="false">https://example.com/terms</string>

    <!-- Branding -->
    <string name="brand_name" translatable="false">MeinTagebuch</string>

    <!-- Technical -->
    <string name="api_base_url" translatable="false">https://api.example.com/v1</string>

</resources>
```

### Wichtig

- **`translatable="false"` ist Pflicht** pro String, auch in der Datei
- **`tools:ignore="MissingTranslation"`** im `<resources>`-Tag verhindert Lint-Warnungen
- Datei wird vom Skill-Validator (`scripts/validate_extracted.py`) explizit geprueft
- Datei NICHT in `values-en/`, `values-fr/` etc. duplizieren — Strings sind sprachunabhaengig

### Migration: Bestehende strings.xml aufteilen

Bei einem bestehenden Projekt: Diese Strings aus `strings.xml` nach
`donottranslate.xml` verschieben (keine Aenderung im Code noetig, R.string.x
funktioniert weiter):

```bash
# Cross-Platform (empfohlen):
bash scripts/validate.sh values/strings.xml --suggest-donottranslate

# Oder direkt (Plattform-abhängig):
python3 scripts/validate_extracted.py values/strings.xml --suggest-donottranslate  # macOS/Linux
python scripts/validate_extracted.py values/strings.xml --suggest-donottranslate   # Windows
```

---

## 6.6 Optional: Translation-Management-Systeme (TMS)

Fuer groessere Apps oder Teams empfehlen sich professionelle TMS.
Vergleich (Stand 2026):

| Tool | Staerke | Preis | Wann sinnvoll |
|------|---------|-------|---------------|
| **Tolgee** | Open Source, In-Context-Editing, KI-Uebersetzung | Free / SaaS | Indie + Team |
| **Lokalise** | OTA-Updates, Screenshot-Kontext | ab 140 USD/Monat | Production-Apps |
| **Crowdin** | Beste Community-Workflows | ab 50 USD/Monat | Open-Source-Apps |
| **Phrase** | Enterprise, ICU-Format, Git-Integration | ab 200 USD/Monat | Konzerne |
| **Weblate** | Self-hosted, Quality-Checks | Free / Self-host | Datenschutz-kritisch |

### Tolgee-Integration (empfohlen fuer kleine/mittlere Apps)

**Einrichtung:**
```bash
# Tolgee CLI installieren
npm install -g @tolgee/cli

# Projekt verknuepfen
tolgee login
tolgee init

# Strings hochladen
tolgee push

# Uebersetzungen herunterladen
tolgee pull --format ANDROID_XML
```

**Im string-extraktor Workflow:**
Nach Phase 4 (VERIFY) optional:
```bash
tolgee push  # Hochladen ins TMS fuer Uebersetzer
```

Nach Uebersetzungs-Skill:
```bash
tolgee pull --format ANDROID_XML --path app/src/main/res/
```

### Crowdin-Integration (fuer Open-Source-Apps)

```yaml
# crowdin.yml
project_id: "your-project-id"
api_token_env: "CROWDIN_API_TOKEN"

files:
  - source: /app/src/main/res/values/strings.xml
    translation: /app/src/main/res/values-%android_code%/strings.xml
    ignore:
      - /app/src/main/res/values/donottranslate.xml
```

---

## 6.7 Setup-Report (PFLICHT — dem Benutzer zeigen)

```
PRAEVENTION EINGERICHTET:
═══════════════════════════════════════
[X] Pre-Commit-Hook installiert (basic / extended)
[X] GitHub-Action aktiv
[X] Lint-Regeln verschaerft
[X] donottranslate.xml angelegt mit N Strings
[ ] String-Freeze-Policy definieren (vor erstem Release)
[ ] TMS-Integration (optional, nicht eingerichtet)
═══════════════════════════════════════
```

Bei bestehender Einrichtung: nur den Status zeigen, nichts ueberschreiben.

---

## Eigenstaendige Nutzung

Diese Setup-Routine kann eigenstaendig aufgerufen werden:

- "Pre-Commit-Hook fuer i18n einrichten"
- "verhindere neue hardcodierte Strings"
- "Lint fuer Strings verschaerfen"
- "donottranslate.xml einrichten"
- "Tolgee integrieren"

In dem Fall:
1. Pruefen welche Guards bereits aktiv sind (Pre-Commit, CI, Lint)
2. Fehlende Guards in der gewuenschten Reihenfolge einrichten
3. donottranslate.xml-Vorschlag aus bestehender strings.xml generieren
4. TMS-Integration nur wenn explizit gewuenscht
5. Report wie in 6.7
