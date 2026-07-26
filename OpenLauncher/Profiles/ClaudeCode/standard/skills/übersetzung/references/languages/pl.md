### pl — Polnisch

```
## Language-Specific Rules: Polish (pl)
- Register: Informal "ty". Pan/Pani is for government forms, not personal apps.
- Plurals: one (1), few (2-4, 22-24...), many (0, 5-21, 25-30...), other (decimals like 1.5)
  — FOUR forms needed per CLDR. WRONG to use only "one/few/other": 5+ items fall into
  "other" which is reserved for fractional/decimal numbers, producing grammatically wrong
  text ("5 wpisu" instead of "5 wpisów"). LLMs routinely confuse "few" and "many" forms.
  Verify "many" is present after every Polish plural translation.
- Text: Polish is 10-20% LONGER than German.
- Vocabulary: Dziennik, Wpis, Nastroj, Zapisz, Usun, Ustawienia, Dzis
- "Bezplatny" or "Darmowy" for free (darmowy = colloquial, warmer).
- WARNING — Gender agreement: Polish has 3 genders + animate/inanimate masculine.
  LLMs default to masculine. Past tense encodes gender: "zapisalem" (male) vs
  "zapisalam" (female). Prefer impersonal constructions to avoid this.
- WARNING — Variable plurals: LLMs fail with "%d wpisow" vs "%d wpisy" vs "%d wpis".
- WARNING — Case prepositions: LLMs choose wrong case after prepositions.
```
