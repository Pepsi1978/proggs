### he — Hebräisch

```
## Language-Specific Rules: Hebrew (he)
- Script: Hebrew alphabet (א-ת). RIGHT-TO-LEFT (RTL) language.
- Register: Informal — Consumer-apps use 2nd-person singular. BUT: Hebrew has strong
  gender distinctions in 2nd-person, so the default is usually masculine ("אתה" - atah)
  with gender-neutral phrasing where possible. Modern apps increasingly use "you" without
  gendered verb forms.
- Plurals: one, two, other (THREE forms — Hebrew has a DUAL form for exactly 2 items
  in some words, especially body parts and certain time units like "יומיים" = two days).
  CLDR specifies one/two/many/other but Android typically uses one/two/other.
- Text: Hebrew is 5-15% SHORTER than German on average. Compact script.
- Vocabulary: יומן (yoman, journal), רישום or כניסה (entry), מצב רוח (matsav ruakh, mood),
  שמור (save), מחק (delete), הגדרות (settings), תזכורת (reminder), חיפוש (search),
  חינם (free), פרימיום (premium, loanword OK), מנוי (subscription)
- Use Arabic numerals (0-9) — Hebrew gematria letters are NEVER used as numbers in UI.
- CRITICAL — Niqqud (vowel pointing): Modern Hebrew UI does NOT use niqqud (the dots and
  marks under letters). Niqqud is reserved for religious texts, children's books, and
  language learning material. Plain consonant text is the standard.
- CRITICAL — Gender agreement: Hebrew verbs, adjectives, and pronouns all change for
  gender. "אתה רוצה" (you-masc want) vs "את רוצה" (you-fem want). Default to masculine
  in generic UI, but prefer gender-neutral imperative ("הוסף" = add) when possible.
- WARNING — Verb forms: Hebrew has 7 binyanim (verb stems). LLMs sometimes pick the
  wrong stem. "להעלות" (le-ha'alot, hif'il = to upload) vs "לעלות" (la'alot, qal = to go up).
  Validate that tech terms use the right stem.
- WARNING — Loanword adoption: Hebrew academy promotes native equivalents but consumer
  apps often use familiar English loanwords ("אפליקציה" = application, "אימייל" = email).
  Use whichever Israeli users actually say — usually the loanword for tech terms.
- WARNING — Construct state (smikhut): Hebrew uses construct state for compound nouns
  ("יומן רישומים" = journal of entries). Different from English possessives. LLMs sometimes
  produce ungrammatical literal translations.
- RTL string safety (same as Arabic/Persian):
  - NEVER use arrow characters (← → ↑ ↓) — they do NOT mirror in RTL layouts
  - Use numbered placeholders (%1$s, %2$d) — translators may reorder arguments
  - Brand name at START of Hebrew string: add ‏ (RTL Mark) before it
  - Numbers and English brand names stay LTR within RTL text
  - Use … (…) for ellipsis, not three dots (...)
```
