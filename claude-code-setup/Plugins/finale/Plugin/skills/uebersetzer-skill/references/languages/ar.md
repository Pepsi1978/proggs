### ar — Arabisch

```
## Language-Specific Rules: Arabic (ar)
- Script: Arabic — RIGHT-TO-LEFT (RTL) language.
- Register: Semi-formal MSA (Modern Standard Arabic / فصحى). The ONLY variant understood
  across all 22+ Arab countries. Do NOT use dialectal Arabic.
- Plurals: ALL SIX forms required: zero, one, two, few (3-10), many (11-99), other (100+).
  This is the MOST COMPLEX plural system. Missing quantities = wrong text or crash.
- Text: ~25% LONGER than German. Use flexible layouts.
- Vocabulary: مذكرات (journal), تسجيل (entry), المزاج (mood), حفظ (save), حذف (delete),
  الإعدادات (settings), اشتراك (subscription)
- No tashkeel (diacritical marks) in app UI — they are for religious texts/children's books.
- Arabic question mark is ؟ (U+061F), NOT ?
- Numbers and English brand names stay LTR within RTL text.
- CRITICAL — Gender agreement: 60% of Arabic LLM errors are gender-related. Every adjective,
  verb, and pronoun must agree with noun gender. Use masculine default where gender unknown.
- WARNING — Dialect mixing: LLMs drift from MSA into Egyptian/Levantine mid-text.
  Verify MSA consistency across all strings.
- BiDi text: Embedded English terms and numbers maintain LTR direction automatically,
  but verify mixed-direction strings render correctly.
- RTL string safety:
  - NEVER use arrow characters (← → ↑ ↓) — they do NOT mirror in RTL layouts
  - Use numbered placeholders (%1$s, %2$d) — Arabic may reorder arguments
  - Brand name at START of Arabic string: add \u200F (RTL Mark) before it
  - Brand name at END: add \u200E (LTR Mark) after it or use BidiFormatter in code
  - Parentheses in strings render correctly but test with embedded LTR content
  - Use \u2026 (…) for ellipsis, not three dots (...)
```
