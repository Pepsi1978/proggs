### tr — Tuerkisch

```
## Language-Specific Rules: Turkish (tr)
- Register: Informal "sen", NOT "siz". Personal apps use "sen" universally.
- Plurals: one, other. IMPORTANT: Turkish nouns do NOT change after numbers.
  "5 giris" NOT "5 girisleri" — the suffix -ler/-lar is NOT used after numbers.
- Text: +30-40% for descriptions vs German. Short buttons are similar length.
  Always provide <!-- SHORTER: ... --> for strings exceeding 30% expansion.
- Vocabulary: Gunluk (journal), Giris/Kayit (entry), Ruh hali (mood),
  Kaydet (save), Sil (delete), Ayarlar (settings), Hatirlatici (reminder),
  Abonelik (subscription), Ucretsiz (free)
- Special characters: c, g, i (dotless), I (dotted uppercase), o, s, u — use correctly.
- CRITICAL — Dotted/Dotless-I Java/Android Bug: In Turkish locale (Locale("tr")),
  "I".toLowerCase() produces "ı" (dotless), NOT "i", and "i".toUpperCase() produces
  "İ" (dotted capital), NOT "I". This is a well-known Android/JVM trap that breaks
  case-insensitive string comparisons, JSON keys, URL parsing, and file extension
  checks. App code that calls .toLowerCase() / .toUpperCase() WITHOUT an explicit
  Locale parameter (Locale.ROOT or Locale.ENGLISH) will silently misbehave for
  Turkish users. This is NOT a translation problem per se — but every translator
  should flag any string that depends on case conversion (e.g. "JSON", "URL", "PDF",
  file paths) for the dev team to audit case handling. Recommended: report findings
  as <!-- LOCALE-AUDIT: case-conversion risk --> if such terms appear in UI strings.
- CRITICAL — Vowel harmony: Turkish suffixes MUST harmonize with root vowels.
  "kitap" → "kitapta" NOT "kitepte". Violations sound like broken grammar.
- WARNING — Agglutination: Suffixes change meaning completely. LLMs stack wrong suffixes
  or skip buffer vowels on loanwords ("App-i" not "App").
- WARNING — Register mixing: LLMs mix "sen" and "siz" in same output.
```
