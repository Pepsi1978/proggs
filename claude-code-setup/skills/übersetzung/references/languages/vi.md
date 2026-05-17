### vi — Vietnamesisch

```
## Language-Specific Rules: Vietnamese (vi)
- Script: Latin with two-tier diacritics (tone marks + vowel modifications).
  Examples: ợ (vowel ơ + grave tone), ẫ (vowel â + creaky tone), ừ (vowel ư + grave tone).
- Register: Informal — Consumer-apps either use no pronoun ("Nhập tên" = "Enter name") or
  the neutral 2nd-person "bạn" (you, friend). NEVER formal "Quý khách" or "Ngài" — those
  are for banks/government/luxury hotels.
- Plurals: one, other (single quantity form — Vietnamese has no grammatical plural).
- Text: Vietnamese is 10-15% LONGER than German on average. Manageable.
- Vocabulary: Nhật ký (journal), Ghi chú or Bài viết (entry), Tâm trạng (mood),
  Lưu (save), Xóa (delete), Cài đặt (settings), Nhắc nhở (reminder), Tìm kiếm (search),
  Miễn phí (free), Cao cấp (premium), Đăng ký (subscription)
- Use Arabic numerals (0-9) — Vietnamese has no native numeral system in modern use.
- CRITICAL — Diacritics integrity: LLMs frequently DROP or CONFUSE tone marks.
  "tam" (three) vs "tâm" (heart/mind) vs "tám" (eight) — all different words.
  Every vowel that should have a tone mark MUST have it. Validate via grep:
  no Vietnamese word should contain unmarked vowels in normal text.
- WARNING — Tone confusion: LLMs swap grave/acute/hook tones. "mơ" (dream) vs "mở" (open).
  Stick to the established vocabulary list — DON'T improvise tone marks.
- WARNING — English code-mix: LLMs often insert English tech terms in Latin script
  ("Save", "Settings"). Vietnamese has perfectly good native equivalents — use them.
- Vietnamese addresses formal hierarchy heavily in person-to-person speech but apps
  bypass this with implicit 2nd-person or "bạn". Keep it implicit when possible.
- WARNING — Telex/VNI input artifacts: LLMs sometimes produce "aa" or "a^" instead of
  proper "â". Verify the file is rendered as proper Unicode, not Telex/VNI input codes.
```
