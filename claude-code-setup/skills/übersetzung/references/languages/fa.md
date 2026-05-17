### fa — Persisch (Farsi)

```
## Language-Specific Rules: Persian / Farsi (fa)
- Script: Persian (uses Arabic-derived script with 4 additional letters: پ چ ژ گ).
  NOT identical to Arabic — different letterforms, different letters, different rules.
  RIGHT-TO-LEFT (RTL) language.
- Register: Consumer-apps standardize on "شما" (shomâ, formal 2nd-person, but used as
  default polite address in modern Iranian Persian). NOT "تو" (tu, intimate). Persian
  culture leans formal in written/UI contexts.
- Plurals: one, other (two forms — simpler than Arabic's six). Persian forms plurals via
  suffix -ها (-hâ) for most nouns, but UI strings often skip the suffix in counted contexts.
- Text: Persian is 20-30% LONGER than German on average (RTL languages tend to expand).
- Vocabulary: یادداشت (yâdâsht, note/entry), خاطره (khâtere, journal/memory),
  حال و هوا or حال (mood — حال is more colloquial), ذخیره (save), حذف (delete),
  تنظیمات (settings), یادآور (reminder), جستجو (search), رایگان (free),
  پیشرفته (premium), اشتراک (subscription)
- Use Arabic numerals (0-9) in UI — NOT Persian/Indic digits (۰۱۲۳۴۵۶۷۸۹).
  Modern Iranian apps consistently display Arabic 0-9 in interface elements.
  (Run scripts/validators/check_native_digits.py --locale fa --path values-fa/strings.xml.)
- CRITICAL — Persian vs Arabic vocabulary contamination: LLMs trained heavily on Arabic
  insert Arabic loanwords ("مَحفوظ" instead of native "ذخیره"). Validate every term —
  if a Persian native equivalent exists, prefer it over the Arabic loanword.
- CRITICAL — Letter confusion: Some Arabic letters look similar to Persian but are
  different code points. Persian "ی" (U+06CC, Farsi Yeh) vs Arabic "ي" (U+064A, Arabic Yeh).
  Persian "ک" (U+06A9, Keheh) vs Arabic "ك" (U+0643, Kaf). LLMs mix these up.
- WARNING — Ezâfe construction: Persian uses ـِ (kasre) between connected words
  ("ساعتِ ۸" = "8 o'clock"). In UI it's often omitted but should be present in formal text.
- WARNING — Dari/Tajik divergence: This rule set is for Iranian Persian (fa-IR).
  Dari (Afghan Persian, fa-AF) and Tajik (uses Cyrillic) need separate translations
  if those markets are targets.
- RTL string safety (same as Arabic/Urdu):
  - NEVER use arrow characters (← → ↑ ↓) — they do NOT mirror in RTL layouts
  - Use numbered placeholders (%1$s, %2$d) — translators may reorder arguments
  - Brand name at START of Persian string: add ‏ (RTL Mark) before it
  - Use … (…) for ellipsis, not three dots (...)
```
