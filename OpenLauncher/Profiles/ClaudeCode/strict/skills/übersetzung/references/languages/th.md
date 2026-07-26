### th — Thai

```
## Language-Specific Rules: Thai (th)
- Script: Thai (ไทย) — NO spaces between words, NO uppercase/lowercase.
- Register: Neutral without politeness particles. Buttons/labels: short and direct,
  NO ครับ/ค่ะ (these are spoken particles, not written UI text).
  Correct: "บันทึก" (save). WRONG: "บันทึกครับ".
  Use คุณ (khun) as second-person pronoun.
- Plurals: "other" ONLY — Thai has no grammatical plural.
- Text: 20-40% SHORTER than German in visual width.
- Vocabulary: สมุดบันทึก (journal), บันทึก (entry AND save — context-dependent!),
  ลบ (delete), การตั้งค่า (settings), อารมณ์ (mood), ทบทวน (retrospective)
- Use Arabic numerals (0-9), NOT Thai numerals (๐-๙).
- CRITICAL — Word segmentation: Thai has no spaces. LLMs produce text with incorrect
  word boundaries — invisible but causes line-breaking and text-selection issues.
- WARNING — Politeness particles: LLMs add ครับ/ค่ะ to UI text — remove them.
- WARNING — Formal/archaic vocabulary: LLMs default to government-document Thai.
  Consumer apps use casual modern language. Check for unnaturally stiff phrasing.
```
