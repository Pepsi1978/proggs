### ur — Urdu

```
## Language-Specific Rules: Urdu (ur)
- Script: Arabic script (اردو) — RIGHT-TO-LEFT (RTL) language.
  Same RTL rules as Arabic: numbers stay LTR, English terms stay LTR.
- Register: Polite "آپ" (aap). Urdu culture strongly favors respect.
- Plurals: one, other
- Text: 20-30% LONGER than German.
- Vocabulary: ڈائری or جریدہ (journal), اندراج (entry), مزاج or موڈ (mood),
  یاد دہانی (reminder)
- Use Arabic-Persian vocabulary: "kitaab" (كتاب) NOT Hindi "pustak",
  "waqt" (وقت) NOT Hindi "samay". Urdu prefers Arabic-Persian loanwords.
- CRITICAL — Hindi vocabulary leakage: Urdu is grammatically close to Hindi but uses
  Arabic script and different vocabulary. LLMs trained on Hindi data insert Hindi/Sanskrit
  words into Urdu output. Native speakers immediately notice this.
- Do NOT just copy Hindi translation and change script — they are different languages.
- English tech terms (PDF, Premium, Export) stay in English — commonly understood.
- Neutral vocabulary for both Pakistan (ur-PK) and India (ur-IN).
- RTL string safety (same rules as Arabic):
  - NEVER use arrow characters (← → ↑ ↓) — they do NOT mirror in RTL layouts
  - Use numbered placeholders (%1$s, %2$d) — Urdu may reorder arguments
  - Brand name at START of Urdu string: add \u200F (RTL Mark) before it
  - Use \u2026 (…) for ellipsis, not three dots (...)
```
