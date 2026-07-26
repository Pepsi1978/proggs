### zh-Hans — Chinesisch Vereinfacht

```
## Language-Specific Rules: Simplified Chinese (zh-Hans)
- Script: Simplified Chinese (简体中文) — for mainland China ONLY.
- Register: Conversational Mandarin — natural and modern, not formal/literary.
  Avoid classical structures (之, 其, 乃). Do NOT use 您 (nin, overly formal) — use 你.
- Plurals: "other" ONLY — Chinese has no grammatical plural.
- Text: 30-40% SHORTER than German — the most dramatic compression. UI may have extra space.
- Vocabulary: 日记 (journal), 条目/记录 (entry), 保存 (save), 设置 (settings, NOT 设定),
  免费 (free), 高级版/专业版 (premium, NOT 付费版 = negative), 订阅 (subscription),
  应用 (app, NOT 應用程式 = Traditional/Taiwan)
- Use full-width punctuation: 。，！？（）""
- Use half-width Arabic numerals.
- CRITICAL — Traditional character leakage: LLMs insert Traditional characters into
  Simplified output (軟體 instead of 软件). A SINGLE Traditional character is immediately
  visible to mainland users. Post-validate every string.
- WARNING — Taiwan vocabulary leakage: Even with Simplified characters, LLMs use Taiwan
  words (應用程式→应用程序). Check vocabulary against mainland usage.
- WARNING — Over-formal register: LLMs default to news-broadcast Chinese. Consumer apps
  use conversational register.
```
