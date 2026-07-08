# Wie fuege ich eine neue Sprache zum Uebersetzungs-Skill hinzu?

Diese Anleitung listet alle 11 Stellen die geaendert werden muessen, wenn der
Skill um eine zusaetzliche Sprache erweitert wird. Ohne diese Checkliste werden
typischerweise 1-3 Stellen vergessen — was dazu fuehrt dass der Skill die neue
Sprache aufrufen will, aber nicht alle Verzeichnisse/Validatoren synchron sind.

Beispiel-Sprache fuer diese Anleitung: **Malaiisch (ms)** — 290M Sprecher in
Malaysia, Brunei, Singapur. Lateinisch, einfache Grammatik, keine RTL, keine
nativen Ziffern, keine CJK-Punctuation.

---

## Vorbereitung — Sprach-Eigenheiten klaeren

Bevor du die 11 Stellen anfasst, klaer diese 6 Fragen zur neuen Sprache:

| Frage | Beispiel Malaiisch (ms) |
|-------|------------------------|
| 1. Sprach-Datei-Code (ISO 639-1) | `ms` |
| 2. Android-Verzeichnis-Code | `ms` (gleich) |
| 3. RTL? | Nein |
| 4. Plural-Kategorien (CLDR) | `other` (1 Form) |
| 5. Native Ziffern? | Nein (nutzt 0-9 wie Latein) |
| 6. CJK-Punctuation? | Nein |

Bei RTL: zusaetzlich BiDi-Marker erwaehnen. Bei Plural > 2: in plurals-Regel ausfuehrlicher.
Bei nativen Ziffern: `scripts/validators/check_native_digits.py` erweitern. Bei CJK:
`scripts/validators/check_cjk_punctuation.py` erweitern.

---

## Die 11 Stellen — Checkliste

### Stelle 1: `references/languages/[code].md` erstellen

Pfad: `~/.claude/skills/übersetzung/references/languages/ms.md`

Inhalt (Template — passe `Language`, `Register`, `Plurals`, `Vocabulary` etc. an):

```markdown
### ms — Malaiisch

\`\`\`
## Language-Specific Rules: Malay (ms)
- Script: Latin (Rumi script). Some informal contexts use Jawi (Arabic script)
  but Consumer-Apps universally use Rumi.
- Register: Informal "kamu" or "anda" — modern Consumer-Apps use "anda" as polite
  default. Avoid "engkau" (intimate, like Indonesian "kau") and overly formal
  "tuan/puan".
- Plurals: other (one form — Malay has no grammatical plural, reduplication for emphasis).
- Text: Malay is ~10-15% LONGER than German on average. Compact words but few abbreviations.
- Vocabulary: Jurnal (journal), Catatan (entry), Mood/Emosi (mood), Simpan (save),
  Padam (delete — Malay) NOT Hapus (Indonesian), Tetapan (settings — Malay) NOT
  Pengaturan (Indonesian), Cari (search), Percuma (free), Premium (loanword OK),
  Langganan (subscription)
- Use Arabic numerals (0-9).
- CRITICAL — Malay vs Indonesian: Malay (ms-MY) and Indonesian (id-ID) are
  closely related but DIFFERENT languages with subtle vocabulary divergence.
  LLMs trained on Indonesian data leak Indonesian words into Malay output:
  "hapus" (id) vs "padam" (ms) for delete, "pengaturan" (id) vs "tetapan" (ms)
  for settings, "akun" (id) vs "akaun" (ms) for account. Validate every term.
- WARNING — Jawi script: LLMs occasionally produce Jawi (Arabic script) for
  formal-sounding text. Modern Malaysian apps universally use Rumi (Latin).
\`\`\`
```

### Stelle 2: `übersetzung-global.md` Inhaltsverzeichnis

Pfad: `~/.claude/skills/übersetzung/übersetzung-global.md`

Zwei Aenderungen:
1. Tabellen-Ueberschrift: "30 Locales" → "31 Locales"
2. Neue Zeile in der Tabelle einfuegen (typisch nach `id` bei Suedost-Asien):

```markdown
| 22 | vi | Vietnamesisch | `references/languages/vi.md` |
| 23 | ms | Malaiisch | `references/languages/ms.md` |   ← NEU
| 24 | bn | Bengali | `references/languages/bn.md` |
...
```

3. Hinweis-Zeile am Ende: "29 Sprachen" → "30 Sprachen"

### Stelle 3: `SKILL.md` Frontmatter description

Pfad: `~/.claude/skills/übersetzung/SKILL.md` Zeile 3

```yaml
description: Uebersetzt Android strings.xml in alle 31 Locales (30 Sprachen — ...)
```

(Sowohl die Zahl der Locales als auch der Sprachen anpassen.)

### Stelle 4: `SKILL.md` Titel und Einleitung

Pfad: `~/.claude/skills/übersetzung/SKILL.md` Zeile 6-13

```markdown
# Uebersetzungs-Skill: Android strings.xml in 31 Locales (30 Sprachen)

Dieser Skill uebersetzt die strings.xml einer Android-App in alle 31 Locales — 30 Sprachen, ...
```

### Stelle 5: `SKILL.md` Skill-Struktur-Diagramm

```markdown
│   └── languages/                    ← Eine Datei pro Locale (31 Stueck)
│       ├── en.md, fr.md, es.md, ar.md, he.md, fa.md, vi.md, ms.md, ...
```

### Stelle 6: `SKILL.md` Phase 1.6 Locale-Mapping

```
id        → values-in        | vi        → values-vi        | ms        → values-ms   ← NEU
```

### Stelle 7: `SKILL.md` Phase 1.7 Plan-Ausgabe

```
- Locales: 31 (en, fr, es, pt-BR, pt-PT, it, nl, pl, ru, uk, tr, ja, ko,
  zh-Hans, zh-Hant, ar, he, fa, hi, th, id, vi, ms, bn, te, mr, ta, ur, gu, kn, ml)
  Hinweis: 30 Sprachen, Portugiesisch zaehlt als 2 eigenstaendige Varianten.
```

### Stelle 8: `SKILL.md` Phase 2 Reihenfolge

```
en → fr → es → pt-BR → pt-PT → it → nl → pl → ru → uk → tr → ja → ko → zh-Hans →
zh-Hant → ar → he → fa → hi → th → id → vi → ms → bn → te → mr → ta → ur → gu → kn → ml
```

### Stelle 9: `SKILL.md` Phase 2 Schritt B — Check 5 Tabelle

Eine Zeile fuer die neue Sprache ergaenzen (vor `ml`):

```markdown
| ms | Keine Indonesisch-Woerter (hapus/pengaturan/akun)? Tetapan/Padam/Akaun korrekt? Kein Jawi-Script? |
```

### Stelle 10: `SKILL.md` Phase 2 Schritt C Status-Meldung

```
✓ [Locale] ist jetzt fertig. ([N]/31)
```

### Stelle 11: `SKILL.md` Phase 3 Abschluss-Zusammenfassung + Qualitaets-Prinzipien

```
Gesamt: [N] Strings in 31 Locales (30 Sprachen) uebersetzt.
```

und

```
Uebersetzungsqualitaet braucht vollen Kontext. Wenn 31 Locales parallel uebersetzt
```

### Stelle 12 (BONUS): Bekannte Einschraenkungen in Phase 3.2

Falls die neue Sprache bekannte LLM-Schwaechen hat (z.B. Diakritika-Verlust,
Skript-Verwechslung, Gender-Inkonsistenz), als Zeile in Phase 3.2 ergaenzen:

```
- [Sprache] ([kurze Beschreibung des LLM-Problems])
```

Diese Stelle wird haeufig vergessen. Frank's Phase 3.2 hatte nach der vi/fa/he-
Erweiterung in Stufe 3 die neuen Sprachen NICHT in der Risiko-Liste — wurde
erst beim 5. Review-Pass entdeckt. Diese Stelle 12 wurde als Resultat ergaenzt.

---

## Optionale Erweiterungen je nach Sprach-Typ

### Wenn Sprache native Ziffern hat (z.B. Lao ລາວ ojb)

Erweiterung in `scripts/validators/check_native_digits.py`:

```python
LANG_DIGITS = {
    ...
    "ms": (...)  # NICHT noetig fuer ms (nutzt 0-9)
    "lo": ("໐໑໒໓໔໕໖໗໘໙", "Lao"),  # Beispiel
}
```

Plus: In SKILL.md Phase 2 Schritt B die Liste der Sprachen erweitern:
```
# Check 7 — Native-Ziffern: bn, hi, mr, te, ta, gu, kn, ml, ur, fa, lo
```

### Wenn Sprache CJK ist (z.B. Yue Chinesisch yue)

Erweiterung in `scripts/validators/check_cjk_punctuation.py`:

```python
LOCALE_CONFIG = {
    ...
    "yue": ("，", "Yue/Cantonese", KANJI),  # Beispiel
}
```

### Wenn Sprache RTL ist (z.B. Sindhi sd)

Im sprach-spezifischen Prompt (`references/languages/sd.md`) den RTL-Safety-Block kopieren:

```
- RTL string safety (same as Arabic/Persian/Hebrew):
  - NEVER use arrow characters (← → ↑ ↓)
  - Use numbered placeholders (%1$s, %2$d)
  - Brand name at START: add ‏ (RTL Mark) before it
  - Use … (…) for ellipsis
```

Plus: Apostroph-Validator-Liste erweitern wenn die Sprache Apostrophe nutzt.

### Wenn Sprache eine Plural-Variante > 2 hat (z.B. Maltesisch mt mit zero/one/two/few/many/other)

Im sprach-spezifischen Prompt explizit ALLE Plural-Kategorien auflisten mit CLDR-Regeln.
Sonst produzieren LLMs nur `one/other` was zu grammatisch falschem Output fuehrt.

---

## Verifikation nach den 11 Stellen

Nach allen Aenderungen diese 4 Checks:

```bash
# 1. Sprach-Datei existiert und hat korrektes Format
ls ~/.claude/skills/übersetzung/references/languages/ms.md
head -3 ~/.claude/skills/übersetzung/references/languages/ms.md  # sollte "### ms — Malaiisch" beginnen

# 2. evals.json ist noch Schema-konform (nicht zwingend, aber sicherheitshalber)
python3 -c "import json; json.load(open(r'C:\\Users\\barwa\\.claude\\skills\\übersetzung\\evals\\evals.json', 'r', encoding='utf-8')); print('Schema OK')"

# 3. Alle Sprach-Anzahlen synchron in SKILL.md
grep -c "31 Locales\|31 Stueck" ~/.claude/skills/übersetzung/SKILL.md  # sollte >0 sein
grep -c "30 Locales\|30 Stueck" ~/.claude/skills/übersetzung/SKILL.md  # sollte 0 sein (alle umgestellt)

# 4. Locale-Mapping enthaelt neue Sprache
grep "values-ms" ~/.claude/skills/übersetzung/SKILL.md
```

Plus: Setup-Repo-Mirror synchronisieren (`~/proggs/claude-code-setup/skills/übersetzung/`).

---

## Realer Erweiterungs-Aufwand

| Sprache | Aufwand | Komplikation |
|---------|---------|--------------|
| Einfache Lateinschrift (ms, sk, hr, ro) | ~20 Min | Keine |
| Mit native Ziffern (lo, my, km) | ~30 Min | Validator-Liste erweitern |
| CJK (yue) | ~30 Min | Validator + Sprach-Block ausfuehrlicher |
| RTL (sd, ps, dv) | ~35 Min | BiDi-Safety + ggf. Native-Digits |
| Komplexe Plurale (mt, ar-Variante, gd) | ~40 Min | Plural-Block ausfuehrlich |

---

## Wenn Sprachen entfernt werden sollen

Reziprok zur Erweiterung:
1. Sprach-Datei `references/languages/[code].md` loeschen
2. Eintrag aus `übersetzung-global.md` TOC entfernen
3. SKILL.md 11 Stellen anpassen (alle Zahlen reduzieren, Sprache aus Listen entfernen)
4. Validator-Listen anpassen (wenn betroffen)

Tipp: Statt loeschen kann eine Sprache auch "stillgelegt" werden indem sie aus der
Reihenfolge entfernt aber die Sprach-Datei behalten wird. So bleibt sie reaktivierbar.
