# Translation Validators

Diese fuenf Python-Validatoren werden vom Uebersetzungs-Skill (`../../SKILL.md`)
nach jeder Sprach-Uebersetzung aufgerufen. Sie sind ausgelagert aus der SKILL.md
um Progressive Disclosure (Anthropic Best Practice) zu erfuellen.

## Uebersicht

| Script | Sprachen | Auto-Fix? | Exit-Code 0 wenn... |
|--------|----------|-----------|---------------------|
| `check_native_digits.py` | bn, hi, mr, te, ta, gu, kn, ml, ur | Ja | OK oder gefixt |
| `check_cjk_punctuation.py` | zh-rCN, zh-rTW, ja | Ja | OK oder gefixt |
| `check_pt_variants.py` | pt-PT, pt-BR | Nein (Bericht) | Saubere Variante |
| `check_apostrophes.py` | fr, it, pt-rBR, pt-rPT, en | Ja | OK oder gefixt |
| `check_length_pacing.py` | alle Sprachen | Nein (Warnung) | Im Rahmen |

## Aufruf-Konvention

Alle Scripts nutzen `argparse` mit klaren CLI-Args. Beispiele:

```bash
# Native-Ziffern (Auto-Fix)
python3 check_native_digits.py --locale bn --path /repo/app/src/main/res/values-bn/strings.xml

# CJK-Punctuation (Auto-Fix)
python3 check_cjk_punctuation.py --locale ja --path /repo/app/src/main/res/values-ja/strings.xml

# PT-Varianten (Bericht, kein Auto-Fix)
python3 check_pt_variants.py --variant pt-PT --path /repo/app/src/main/res/values-pt-rPT/strings.xml

# Apostroph (Auto-Fix)
python3 check_apostrophes.py --locale fr --path /repo/app/src/main/res/values-fr/strings.xml

# Laengen-Pacing (Bericht, kein Auto-Fix)
python3 check_length_pacing.py --source /repo/app/src/main/res/values/strings.xml \
    --target /repo/app/src/main/res/values-pl/strings.xml --locale pl
```

## Exit-Code-Konvention

| Code | Bedeutung |
|------|-----------|
| 0 | Alles OK (oder erfolgreich auto-gefixt) |
| 1 | Probleme gefunden die manuelle Korrektur brauchen |
| 2 | Fehler (Datei nicht lesbar, falsches Locale, etc.) |

## Engineering-Prinzipien

Alle Validatoren erfuellen:

- **Atomares Schreiben**: tempfile + os.replace verhindert kaputte Dateien bei Crash
- **UTF-8 explizit**: encoding="utf-8" bei jedem open() (Windows-Pflicht)
- **Argparse-Validierung**: Locale-Choices, Pfad-Existenz vor Verarbeitung
- **Klare Exit-Codes**: 0=OK, 1=Problem, 2=Fehler — Skill kann darauf reagieren
- **Standalone**: keine geteilten Helper-Module — jedes Script funktioniert allein

## Testen

Die Scripts werden vom Uebersetzungs-Skill nach jeder Sprache aufgerufen.
Manuelle Tests sind via Test-strings.xml moeglich (siehe SKILL.md "Phase 2 Schritt B").
