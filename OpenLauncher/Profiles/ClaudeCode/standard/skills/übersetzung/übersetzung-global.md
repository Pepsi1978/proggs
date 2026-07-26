# Uebersetzungs-Prompts — Skill-Referenz

> Diese Datei ist die zentrale Referenz fuer den Uebersetzungs-Skill.
> Sie enthaelt:
> - Den **Universal-Prompt** (gleicher Rahmen fuer ALLE Sprachen)
> - Das **Inhaltsverzeichnis** der sprach-spezifischen Prompts
>
> Die sprach-spezifischen Prompts liegen seit der Refactoring-Stufe-B in
> `references/languages/[code].md` — eine Datei pro Locale. Das spart Kontext
> beim Trigger: der Skill laedt nur die Prompts der Sprachen die er gerade
> braucht, statt aller 27 auf einmal.

---

## Inhaltsverzeichnis — 30 Locales

| # | Code | Sprache | Referenz-Datei |
|---|------|---------|----------------|
| 1 | en | Englisch | `references/languages/en.md` |
| 2 | fr | Franzoesisch | `references/languages/fr.md` |
| 3 | es | Spanisch | `references/languages/es.md` |
| 4 | pt-BR | Portugiesisch Brasilianisch | `references/languages/pt-BR.md` |
| 5 | pt-PT | Portugiesisch Europa | `references/languages/pt-PT.md` |
| 6 | it | Italienisch | `references/languages/it.md` |
| 7 | nl | Niederlaendisch | `references/languages/nl.md` |
| 8 | pl | Polnisch | `references/languages/pl.md` |
| 9 | ru | Russisch | `references/languages/ru.md` |
| 10 | uk | Ukrainisch | `references/languages/uk.md` |
| 11 | tr | Tuerkisch | `references/languages/tr.md` |
| 12 | ja | Japanisch | `references/languages/ja.md` |
| 13 | ko | Koreanisch | `references/languages/ko.md` |
| 14 | zh-Hans | Chinesisch Vereinfacht | `references/languages/zh-Hans.md` |
| 15 | zh-Hant | Chinesisch Traditionell | `references/languages/zh-Hant.md` |
| 16 | ar | Arabisch | `references/languages/ar.md` |
| 17 | he | Hebraeisch | `references/languages/he.md` |
| 18 | fa | Persisch (Farsi) | `references/languages/fa.md` |
| 19 | hi | Hindi | `references/languages/hi.md` |
| 20 | th | Thai | `references/languages/th.md` |
| 21 | id | Indonesisch | `references/languages/id.md` |
| 22 | vi | Vietnamesisch | `references/languages/vi.md` |
| 23 | bn | Bengali | `references/languages/bn.md` |
| 24 | te | Telugu | `references/languages/te.md` |
| 25 | mr | Marathi | `references/languages/mr.md` |
| 26 | ta | Tamil | `references/languages/ta.md` |
| 27 | ur | Urdu | `references/languages/ur.md` |
| 28 | gu | Gujarati | `references/languages/gu.md` |
| 29 | kn | Kannada | `references/languages/kn.md` |
| 30 | ml | Malayalam | `references/languages/ml.md` |

Hinweis: 29 Sprachen, Portugiesisch wird als 2 eigenstaendige Varianten gepflegt
(pt-BR fuer Brasilien, pt-PT fuer Portugal — siehe Check 9 PT-Varianten-Trennung).

---

## 1. Universal-Prompt

Dieser Prompt wird fuer JEDE Sprache als Basis verwendet. Die Platzhalter werden
vom Skill automatisch befuellt. Der sprach-spezifische Block aus
`references/languages/[code].md` ersetzt den Platzhalter `[LANGUAGE_SPECIFIC_RULES]`
am Ende.

```
You are a professional mobile app localization specialist. Your task is to translate
Android strings.xml entries from German to [TARGET_LANGUAGE] ([LOCALE_CODE]).

## App Context
- App name: [APP_NAME] (NEVER translate this brand name)
- App type: [APP_DESCRIPTION]
- Tone: [APP_TONE]
- Register: [REGISTER — see language-specific rules below]

## Critical Rules

### 1. NEVER modify these elements:
- Format placeholders: %s, %d, %1$s, %2$d, %1$d, etc.
- String resource names (the "name" attribute in XML tags)
- XML tags: <string>, <plurals>, <item>, <string-array>, <xliff:g>
- HTML tags: <b>, <i>, <u>, <br>, <a href="...">
- CDATA blocks, escaped characters: \n, \', \"
- URLs, email addresses, file paths
- Strings with translatable="false" — skip entirely

### 2. Brand names — keep original:
- [APP_NAME], "Premium", "Google", "Android" and all product names

### 3. Plurals:
- Generate ALL quantity forms required by [TARGET_LANGUAGE]
- Android quantities: zero, one, two, few, many, other
- See language-specific rules for which quantities are needed

### 4. Text length:
- Mobile UI space is limited. If translation exceeds the original by >40%,
  provide a shorter alternative: <!-- SHORTER: [alternative] -->
- Button labels: max ~20 characters. Dialog titles: max ~30 characters.

### 5. Consistency:
- Use the SAME translation for the SAME term throughout all strings.
- Follow the vocabulary glossary in the language-specific rules below.

### 6. Adaptation:
- Use date formats natural to [TARGET_LANGUAGE] speakers
- Keep emojis exactly as-is
- No cultural references specific to one country

### 7. Numbered placeholders (MANDATORY for RTL languages):
- ALWAYS use numbered placeholders: %1$s, %2$d — NEVER unnamed %s, %d
- Reason: Arabic, Urdu, and Hebrew translators may reorder arguments.
  Unnamed %s + %s will crash the app if reordered.
- Even for non-RTL languages: numbered placeholders are best practice.

### 8. Encoding:
- Output MUST be UTF-8 WITHOUT BOM (Byte Order Mark)
- A BOM (EF BB BF bytes) breaks Android's AAPT build tool
- Escape apostrophes as \' in unquoted strings, or wrap entire string in double quotes
- Use XML entities: &amp; &lt; &gt; — raw < or & breaks XML parsing

### 9. Arrow characters and directional symbols:
- NEVER use literal arrow characters (← → ↑ ↓) in translatable strings
- These are Unicode "neutral" characters — they do NOT mirror in RTL layouts
- An arrow pointing right (→) still points right in Arabic — confusing the user
- Use drawable resources with android:autoMirrored="true" instead
- Use ellipsis character … (…) instead of three dots (...)

## Output Format
- Return ONLY translated XML, ready for values-[LOCALE_CODE]/strings.xml
- Preserve EXACT XML structure (same order, same nesting)
- If uncertain: <!-- REVIEW: reason -->
- Start with <?xml version="1.0" encoding="utf-8"?>

## Quality Checklist (verify before output):
- All %s, %d, %1$s placeholders preserved exactly
- All XML tags intact and properly closed
- Brand names NOT translated
- Plural forms match target language requirements
- Natural phrasing, not word-for-word translation
- Consistent vocabulary throughout
- No translatable="false" strings translated
- Escaped characters (\n, \', \") preserved

A missing or modified placeholder WILL crash the app at runtime.

[LANGUAGE_SPECIFIC_RULES]
```

---

## 2. Sprach-spezifische Prompts

Jeder Sprach-Block wird vom Skill aus `references/languages/[code].md` geladen
und als `[LANGUAGE_SPECIFIC_RULES]` in den Universal-Prompt eingesetzt. Wenn
der Skill nur eine Sprache uebersetzt, wird nur diese eine Datei geladen —
das spart ca. 95% des Kontext-Verbrauchs gegenueber der frueheren
Variante mit allen 27 Sprach-Bloecken in dieser einen Datei.

**Wie der Skill die Sprach-Datei findet:**

```
Pfad-Schema (relativ zum Skill-Ordner):
  references/languages/[code].md

Beispiele:
  references/languages/en.md          (Englisch)
  references/languages/fr.md          (Franzoesisch)
  references/languages/pt-BR.md       (Portugiesisch Brasilianisch)
  references/languages/zh-Hans.md     (Chinesisch Vereinfacht)
```

Der `code` entspricht der ersten Spalte im Inhaltsverzeichnis oben. KEINE
Umkehrung in `pt-rBR` oder `zh-rCN` — diese Android-Verzeichnis-Namen werden
nur fuer den Zielpfad in `values-[locale]/strings.xml` benutzt, NICHT fuer
die Referenz-Datei.

---

## 3. Format der sprach-spezifischen Datei

Jede `references/languages/[code].md` hat dieses Format:

```markdown
### [code] — [Sprache]

\`\`\`
## Language-Specific Rules: [Language] ([locale])
- Register: ...
- Plurals: ...
- Text: ...
- Vocabulary: ...
- WARNING — ...
- CRITICAL — ...
\`\`\`
```

Der Skill liest die Datei, extrahiert den Code-Block (zwischen den ` ``` `
Markierungen) und setzt ihn als `[LANGUAGE_SPECIFIC_RULES]` in den
Universal-Prompt ein.
