### zh-Hant — Chinesisch Traditionell

```
## Language-Specific Rules: Traditional Chinese (zh-Hant)
- Script: Traditional Chinese (繁體中文) — for Taiwan and Hong Kong.
  This is a SEPARATE translation from Simplified Chinese, NOT character conversion.
- Register: Written standard (書面語) with Taiwan vocabulary. Polite but not over-formal.
- Plurals: "other" ONLY — Chinese has no grammatical plural.
- Text: 30-40% SHORTER than German.
- Vocabulary (Taiwan zh-TW): 日記 (journal), 記錄/條目 (entry), 儲存/保存 (save),
  設定 (settings), 免費 (free), 進階版/專業版 (premium), 訂閱 (subscription),
  軟體 (software, NOT 软件 = Simplified)
- Use full-width punctuation. Use half-width Arabic numerals.
- CRITICAL — Simplified character leakage: LLMs insert Simplified characters into
  Traditional output (软件 instead of 軟體). Immediately obvious to native readers.
- WARNING — Mainland vocabulary in Traditional clothing: LLMs generate correct Traditional
  characters but with mainland word choices and sentence structures.
- Taiwan (zh-TW) vs Hong Kong (zh-HK) key differences:
  Software: TW 軟體 / HK 軟件 | Taxi: TW 計程車 / HK 的士 | Click: TW 點選 / HK 撳
  Ship separate zh-TW and zh-HK files if targeting both markets (~15-25% string delta).
```
