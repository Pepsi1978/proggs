# BestJournal Android: Lokalisierungs-Leitfaden & Uebersetzungs-Prompts

> Erstellt am 2026-04-15 basierend auf Recherche zu Best Practices der erfolgreichsten Apps weltweit
> (Spotify, TikTok, Airbnb, Duolingo, WhatsApp, Instagram).

---

## Inhaltsverzeichnis

1. [Wie die Grossen es machen](#1-wie-die-grossen-es-machen)
2. [Empfohlener Workflow fuer BestJournal](#2-empfohlener-workflow-fuer-bestjournal)
3. [Universal-Prompt fuer alle Sprachen](#3-universal-prompt-fuer-alle-sprachen)
4. [Sprach-spezifische Prompt-Ergaenzungen](#4-sprach-spezifische-prompt-ergaenzungen)
5. [Haeufige Fehler und Fallstricke](#5-haeufige-fehler-und-fallstricke)
6. [Qualitaetssicherung nach der Uebersetzung](#6-qualitaetssicherung-nach-der-uebersetzung)
7. [Welche Sprachen zuerst?](#7-welche-sprachen-zuerst)
8. [Tools und Plattformen](#8-tools-und-plattformen)
9. [Technische Voraussetzungen in der App](#9-technische-voraussetzungen-in-der-app)
10. [Beruechtigte Uebersetzungsfehler grosser Unternehmen](#10-beruechtigte-uebersetzungsfehler-grosser-unternehmen)

---

## 1. Wie die Grossen es machen

### Uebersicht der Top-Apps

| App | Sprachen | Maerkte | Methode |
|-----|----------|---------|---------|
| **Spotify** | 78 Sprachen | 184 Maerkte | Interne Linguisten + Agenturen, kulturelle Anpassung (nicht nur Text) |
| **TikTok** | 75 Sprachen | 150+ Laender | Profis via Alconost, Translation Memory, Glossare, monatliche QA-Zyklen |
| **Airbnb** | 100+ Varianten | 190+ Laender | Kulturelle Adaptation: lokale Fotografie, UI-Elemente, marktspezifisches Framing |
| **Duolingo** | ~40 Sprachen | Global | Radikales Crowdsourcing: 30-40 Nutzer uebersetzen denselben Text, Algorithmus waehlt Beste |
| **WhatsApp** | 60+ Sprachen | ~180 Laender | Professionelle Uebersetzer + Community-Pruefung |
| **Instagram** | 36+ Sprachen | Weltweit | Facebook/Meta internes Lokalisierungsteam |

### Der Industrie-Standard 2025/2026

**Hybridmodell: KI uebersetzt, Mensch reviewt.**

- KI (GPT-4, Claude, DeepL, Gemini) macht die Erst-Uebersetzung
- Translation Memory (TM) sorgt fuer Konsistenz mit frueheren Uebersetzungen
- RAG (Retrieval Augmented Generation) erdet das LLM mit Marken-Terminologie und Glossar
- Muttersprachler reviewen und korrigieren
- Die KI-Erst-Uebersetzung spart laut Branchendaten **60-75% der Nachbearbeitungszeit**
- Uebersetzer werden zu "AI Content Reviewers" und "Cultural Consultants"

### Faustregel

Die ersten 5-10 Sprachen (Englisch, Spanisch, Portugiesisch, Franzoesisch, Deutsch,
Japanisch, Koreanisch, Chinesisch vereinfacht, Arabisch, Hindi) decken **~80% der
globalen App-Downloads** ab.

---

## 2. Empfohlener Workflow fuer BestJournal

### 3-Stufen-Ansatz (Indie-/Solo-Entwickler)

```
Stufe 1: KI-Erst-Uebersetzung
    → Universal-Prompt (siehe Abschnitt 3) mit Claude oder GPT-4 verwenden
    → strings.xml als Input, uebersetzte strings.xml als Output
    → Pro Sprache ein Durchlauf

Stufe 2: Automatische Validierung
    → Platzhalter-Check: Sind alle %1$s, %d etc. noch vorhanden?
    → Laengencheck: Uebersetzungen die >150% der Originallaenge haben, markieren
    → Plural-Vollstaendigkeit: Alle quantity-Werte fuer die Zielsprache vorhanden?
    → XML-Validierung: Ist das Ergebnis gueltige XML?

Stufe 3: Muttersprachler-Review (optional, aber empfohlen)
    → Fuer die Top-5-Sprachen (EN, ES, FR, JA, PT-BR) einen nativen Speaker draufschauen lassen
    → Plattformen: Fiverr, Upwork, oder Crowdin Community
    → Fokus: Natuerlichkeit, Ton, kulturelle Angemessenheit
```

### Verzeichnisstruktur in Android

```
app/src/main/res/
    values/             ← Deutsch (Quellsprache)
        strings.xml
    values-en/          ← Englisch
        strings.xml
    values-es/          ← Spanisch
        strings.xml
    values-fr/          ← Franzoesisch
        strings.xml
    values-pt-rBR/      ← Portugiesisch (Brasilien)
        strings.xml
    values-ja/          ← Japanisch
        strings.xml
    values-ko/          ← Koreanisch
        strings.xml
    values-zh-rCN/      ← Chinesisch (vereinfacht)
        strings.xml
    values-hi/          ← Hindi
        strings.xml
    values-ar/          ← Arabisch
        strings.xml
    values-tr/          ← Tuerkisch
        strings.xml
```

---

## 3. Universal-Prompt fuer alle Sprachen

Dieser Prompt funktioniert mit Claude, GPT-4, GPT-4o und anderen LLMs.
Er basiert auf Best Practices von Custom.MT, Crowdin, Lokalise und den
Workflows von TikTok und Spotify.

### Anleitung

1. Den Prompt unten kopieren
2. `[TARGET LANGUAGE]` und `[LOCALE CODE]` ersetzen (z.B. "English" und "en")
3. `[INFORMAL/FORMAL]` ersetzen (siehe Abschnitt 4 fuer die richtige Wahl pro Sprache)
4. Falls die Sprache spezielle Regeln braucht: Ergaenzung aus Abschnitt 4 anhaengen
5. Die strings.xml am Ende einsetzen
6. Ergebnis speichern als `values-[locale]/strings.xml`

### Der Prompt

```
You are a professional Android app localization specialist with 10+ years of experience
translating mobile apps for the Google Play Store.

## App Context
- App name: "BestJournal" (NEVER translate this brand name)
- App type: Personal journaling and diary app for Android
- Target audience: Adults who want to write daily journal entries, track moods, and reflect
  on their lives
- Tone: Warm, encouraging, personal — like a supportive friend, not a corporate tool
- Register: [INFORMAL/FORMAL — see language-specific instruction below]

## Task
Translate the following Android strings.xml entries from German to [TARGET LANGUAGE]
([LOCALE CODE, e.g. fr-FR]).

## Critical Rules (MUST follow — violation = unusable translation)

### 1. NEVER modify these elements:
- Format placeholders: %s, %d, %1$s, %2$d, %1$d, etc.
- String resource names (the "name" attribute in XML tags)
- XML tags: <string>, <plurals>, <item>, <string-array>, <xliff:g>
- HTML formatting tags: <b>, <i>, <u>, <br>, <a href="...">
- CDATA blocks
- Escaped characters: \n, \', \"
- URLs, email addresses, file paths
- The attribute translatable="false" — skip these strings entirely

### 2. Brand names — keep in original form:
- "BestJournal", "Premium", "Google", "Android", "Groq", "Whisper"
- Any other proper nouns or product names

### 3. Plurals (CRITICAL for grammatical correctness):
- If the source has <plurals>, generate ALL quantity forms required by [TARGET LANGUAGE]
- Android plural quantities: zero, one, two, few, many, other
- Different languages need different quantities:
  - English/German: one, other
  - French/Italian/Portuguese: one, many, other
  - Arabic: zero, one, two, few, many, other (ALL SIX)
  - Russian/Polish/Czech: one, few, many, other
  - Japanese/Chinese/Korean/Thai: other (only)
  - Turkish: one, other

### 4. Text length awareness:
- UI space is limited on mobile screens
- If your translation is significantly longer than the original (>40% expansion),
  provide a shorter alternative as an XML comment above the string:
  <!-- SHORTER: [alternative] -->
- Button labels should stay under 20 characters where possible
- Dialog titles should stay under 30 characters where possible

### 5. Consistency:
- Use the same translation for the same term throughout
- Key terms glossary (fill in consistently for this language):
  - Eintrag/Entry = [consistent term in target language]
  - Tagebuch/Journal = [consistent term in target language]
  - Stimmung/Mood = [consistent term in target language]
  - Erinnerung/Reminder = [consistent term in target language]
  - Einstellungen/Settings = [consistent term in target language]
  - Zusammenfassung/Summary = [consistent term in target language]
  - Transkription/Transcription = [consistent term in target language]
  - Erfolg/Achievement = [consistent term in target language]
  - Streak = [consistent term or keep "Streak" in target language]
  - Premium = Premium (keep as-is in all languages)
  - Export/PDF = keep as-is

### 6. Cultural adaptation:
- Date formats: Use the format natural to [TARGET LANGUAGE] speakers
- Do NOT use American cultural references — keep it universal
- Emojis in strings: keep exactly as-is, do not replace or add

## Output Format
- Return ONLY the translated XML content, ready to paste into values-[locale]/strings.xml
- Do NOT add explanations, comments about your choices, or markdown formatting
  around the XML
- Preserve the EXACT XML structure of the input (same order, same nesting)
- If you are uncertain about a translation, add an XML comment: <!-- REVIEW: reason -->
- Start your output with <?xml version="1.0" encoding="utf-8"?>

## Quality Checklist (verify internally before outputting):
- [ ] All %s, %d, %1$s placeholders are preserved exactly as in the source
- [ ] All XML tags are intact and properly closed
- [ ] Brand names are NOT translated
- [ ] Plural forms match target language requirements
- [ ] Tone is warm and personal, not robotic or corporate
- [ ] No literal/word-for-word translations — natural phrasing in target language
- [ ] No strings with translatable="false" were translated
- [ ] Escaped characters (\n, \', \") are preserved

It is very important that you preserve all format placeholders exactly as they appear
in the source. A missing or modified placeholder will crash the app at runtime.

---

## strings.xml to translate:

[PASTE YOUR strings.xml CONTENT HERE]
```

---

## 4. Sprach-spezifische Prompt-Ergaenzungen

Fuer bestimmte Sprachen muss dem Universal-Prompt eine zusaetzliche Sektion
angehaengt werden. Diese Ergaenzungen adressieren grammatische, kulturelle
und technische Besonderheiten der jeweiligen Sprache.

### 4.1 Franzoesisch (fr) — Frankreich

```
## Language-Specific Rules: French (fr-FR)
- Script: Latin — no special font requirements
- Register: Use informal "tu/te/toi" form, NOT "vous" — this is a personal journal app.
  French tech apps (Spotify, Apple, Google) universally use "tu". "Vous" feels cold and
  corporate for a diary product. LLMs default to "vous" when not prompted, so ALWAYS
  specify "informal tu form" explicitly.
- Plurals: one, other
- Text expansion: French is typically 10-20% longer than German — watch button labels.
  German compound nouns get split into multi-word phrases in French.
- "Journal intime" for "Tagebuch/Journal", "Entrée" for "Eintrag/Entry"
- "Humeur" for "Stimmung/Mood", "Paramètres" for "Einstellungen/Settings"
- "Enregistrer" for "Speichern/Save", "Supprimer" for "Löschen/Delete"
- Gender: Use masculine as default unless context clearly indicates otherwise.
  Prefer gender-neutral formulations where possible.

CRITICAL LLM WARNING — French Punctuation:
- French requires a space BEFORE : ; ! ? and inside « » guillemets.
  Specifically: thin non-breaking space (\u202F) before ! and ?,
  regular non-breaking space (\u00A0) before : and ;.
  LLMs ROUTINELY OMIT these spaces — they output "Comment vas-tu?" instead of
  "Comment vas-tu\u202F?" This silently breaks French typographic standards.
- Use guillemets «\u00A0...\u00A0» for quotation marks, NOT "..."
- LLMs sometimes produce English-style "..." instead of French «...» — always check.

CRITICAL LLM WARNING — False Friends:
- ~60% of English vocabulary derives from French/Latin, creating deceptive similarities.
  Key false friends: "actuellement" = currently (NOT "aktuell"), "sensible" = sensitive
  (NOT sensible), "large" = wide (NOT large/big).
- LLMs sometimes produce inconsistent gender agreement in longer strings with pronouns.

Regional variant:
- Use France French (fr-FR) as default — the neutral international standard understood
  by all 29 French-speaking countries. Quebec French differs significantly in vocabulary
  and grammar. Add fr-CA as separate locale only if specifically targeting Canada.
- Key Quebec differences: "courriel" (email) vs "e-mail", "fin de semaine" vs "week-end"

LLM-specific note:
- Claude 3.5 Sonnet leads for French (WMT24 first place, 78% "good" in Lokalise blind study).
- DeepL specialized LLM preferred 1.7x more often than GPT-4 by translators for French.
- German→French is a high-resource pair — all major LLMs perform at near-human level.

Font rendering:
- Standard Roboto/Noto Sans handles all French diacritics (é, è, ê, à, â, û, ç, œ, æ)
- Test thin non-breaking space \u202F on API 21+ — some older Android versions render it
  as zero-width space.
```

### 4.2 Spanisch (es) — Lateinamerika neutral

```
## Language-Specific Rules: Spanish (es-419)
- Script: Latin — no special font requirements
- Register: Use informal "tú" (tuteo) throughout, NOT "usted" and NEVER voseo.
  Personal journal apps are intimate products — "usted" would feel bureaucratic.
  Explicitly specify "tuteo" — LLMs sometimes default to "usted" for app strings.
- Plurals: one, other
- Text expansion: Spanish is typically 15-25% longer than German — design UI with ~20% extra space
- "Diario" for "Tagebuch/Journal", "Entrada" for "Eintrag/Entry"
- "Estado de ánimo" for "Stimmung/Mood", "Configuración" for "Einstellungen/Settings"
- "Guardar" for "Speichern/Save", "Eliminar" for "Löschen/Delete"
- "Buscar" for "Suchen/Search", "Escribir" for "Schreiben/Write"

CRITICAL LLM WARNING — Voseo Leakage:
- LLMs trained on large Argentine/Uruguayan web data will sometimes leak voseo verb forms
  ("vos tenés", "vos podés") even when prompted for neutral Spanish. This is
  unintelligible/jarring to ~90% of Spanish speakers outside Argentina/Uruguay.
  ALWAYS add "no uses voseo" to prompts.
- Without explicit instruction, LLMs mix "tú" and "usted" forms within the same
  translation set — inconsistent register across strings sounds broken.

CRITICAL LLM WARNING — Spanglish:
- LLMs inject English-influenced vocabulary ("deletear" instead of "eliminar",
  "printear" instead of "imprimir"). Watch for anglicisms in tech UI terms.
- False friends: "sensible" = sensitive (NOT sensible), "actual" = current (NOT actual)

Regional variant:
- Use Neutral Latin American Spanish (es-419) as default. Reasoning:
  Latin America has ~470M Spanish speakers vs ~47M in Spain.
  Spain users accept LATAM neutral; LATAM users do NOT accept Castilian vocabulary.
- AVOID Castilian-specific terms: "ordenador" → "computadora", "móvil" → "celular",
  "vosotros" → "ustedes"
- Add es-ES as separate locale only if Spain is a specific target market.

LLM-specific note:
- Claude 3.5 Sonnet and GPT-4o both competitive for Spanish — no significant quality gap.
- German→Spanish is a high-resource pair — all major LLMs perform well.

Font rendering:
- Standard Roboto/Noto handles all Spanish characters (á, é, í, ó, ú, ñ, ü, ¿, ¡)
- No special rendering concerns. Inverted ¿ and ¡ render correctly.
```

### 4.3 Portugiesisch Brasilianisch (pt-BR) — Brasilien

```
## Language-Specific Rules: Brazilian Portuguese (pt-BR)
- Script: Latin — no special font requirements
- Register: Use "você" throughout — the standard second-person pronoun in Brazilian Portuguese.
  "Tu" is used in some regions (Rio Grande do Sul, Northeast) but "você" is the safe default.
  Explicitly instruct: "use você for all second-person references, not tu."
- Plurals: one, other
- Text expansion: PT-BR is typically 15-30% longer than German — design with ~20% extra space
- "Diário" for "Tagebuch/Journal", "Entrada" for "Eintrag/Entry"
- "Humor" for "Stimmung/Mood", "Configurações" for "Einstellungen/Settings"
- "Salvar" for "Speichern/Save", "Excluir" for "Löschen/Delete"
- "Pesquisar" for "Suchen/Search", "Escrever" for "Schreiben/Write"

CRITICAL LLM WARNING — PT-BR / PT-PT Vocabulary Mixing:
- This is the SINGLE BIGGEST LLM failure for Portuguese. LLMs trained on mixed data produce
  hybrid outputs. Critical UI term pairs where mixing occurs:
  - "usuário" (BR) vs "utilizador" (PT) — "user"
  - "aplicativo" (BR) vs "aplicação" (PT) — "app"
  - "celular" (BR) vs "telemóvel" (PT) — "mobile phone"
  - "salvar" (BR) vs "guardar" (PT) — "save"
  - "configurações" (BR) vs "definições" (PT) — "settings"
  - "senha" (BR) vs "palavra-passe" (PT) — "password"
  - "baixar" (BR) vs "transferir" (PT) — "download"
- ALWAYS state "use Brazilian Portuguese (pt-BR), not European Portuguese" in every prompt.
  Even this sometimes fails — post-validate key terms against the list above.

CRITICAL LLM WARNING — Spelling Reform:
- Brazil follows the 1990 Orthographic Agreement differently from Portugal.
  LLMs sometimes use pre-reform Brazilian spelling. Check for outdated spellings.

Regional variant:
- This locale is specifically pt-BR (Brazilian Portuguese). European Portuguese (pt-PT)
  requires a completely separate translation with different vocabulary, not just spelling.
  Do NOT generate one and derive the other — they must be translated independently.

Cultural specifics:
- Brazilians expect warm, informal tone — cold formal language reads as bureaucratic.
- Emoji usage is culturally high in Brazil — emoji in strings fits the market.
- "Retrospective" / reflection features resonate strongly with Brazilian self-improvement culture.
- Journal/diary ("diário") is a well-understood concept in Brazil.

LLM-specific note:
- Gemini 2.5 Pro/Flash rank among the best for PT-BR specifically (Intento 2025).
- Claude 3.5 is strong generally but Gemini has an edge for Brazilian Portuguese.
- Training data is overwhelmingly PT-BR (Brazil has 10x more internet content than Portugal).

Font rendering:
- Standard Roboto/Noto handles all Portuguese characters (á, â, ã, à, ç, é, ê, í, ó, ô, õ, ú)
- No special rendering concerns.
```

### 4.4 Japanisch (ja)

```
## Language-Specific Rules: Japanese (ja)
- Script: Kanji + Hiragana + Katakana (mixed script system)
- Register: Use teineigo (丁寧語, polite form) — 〜です/ます throughout.
  Do NOT use sonkeigo (尊敬語, honorific) or kenjougo (謙譲語, humble) — they feel
  like a corporate HR system in a personal journal app.
  Do NOT use plain form (〜だ/〜である) — too casual for app UI.
  Buttons: plain verb form is OK (保存する = save).
  Messages: teineigo (記録が保存されました。)
  AVOID: honorific prefix ご/お before app-specific nouns (ご記録 = over-formal)
- Plurals: Use ONLY the "other" quantity — Japanese has no grammatical plural
- Text will typically be 20-30% SHORTER than German — this is normal.
  German compound words (Tagebucheintrag) become tight compound kanji (日記) saving space.
  UI layouts may look empty — verify padding doesn't look excessive.
- "日記" (にっき, nikki) for "Tagebuch/Journal" — native word preferred over ジャーナル
- "記録" (きろく, kiroku) for "Eintrag/Entry" — more natural than エントリー for a diary
- "保存する" for "Speichern/Save", "設定" for "Einstellungen/Settings"
- "無料" for "Kostenlos/Free", "プレミアム" for "Premium" (katakana OK here)
- "振り返り" for "Rückblick/Retrospective"
- Numbers: Use half-width Arabic numerals (1, 2, 3), not full-width
- Use full-width punctuation: 。，！？（）「」

CRITICAL LLM WARNING — Keigo Level Contamination:
- LLMs default to over-formal sonkeigo or kenjougo when teineigo is sufficient.
  Journal app text with wrong keigo feels like a government portal, not a personal diary.
- LLMs mix 〜です/ます (polite) with 〜だ/である (plain) within the SAME screen —
  fatal for UI consistency. Check every string for consistent sentence endings.
- LLMs over-use katakana loanwords (ジャーナル, メモ) where native Japanese terms
  (日記, 記録) feel more natural in a personal diary context.

CRITICAL — Han Unification Bug (Android):
- Unicode assigns shared codepoints to CJK characters used differently in Japanese, Chinese,
  and Korean. Without explicit xml:lang="ja" tagging, Android may render Japanese characters
  using CHINESE glyph variants. Example: 直, 骨, 角 look different in ja vs zh fonts.
  ALWAYS set android:fontFamily to a Japan-specific font or use locale config.
- Noto Sans JP is the recommended Android font (~16MB). Specify fallback chain:
  Noto Sans JP → system CJK → sans-serif.

Line-breaking (禁則処理, kinsoku shori):
- JIS X 4051 line-break rules: Characters like 、。」』) must NEVER start a line.
  Characters like 「『( must NEVER end a line.
  Use android:breakStrategy="high_quality" and test explicitly.
- Font size: 12sp in Japanese is harder to read than 12sp in Latin.
  Recommended minimum: 14sp for body text.

LLM-specific note:
- Claude 3.5 Sonnet ranked first at WMT24 including ja↔zh pairs.
- DeepL next-gen specialized model is strong for ja↔de translation.
- Qwen-MT also strong on CJK with human-eval approval.
```

### 4.5 Koreanisch (ko)

```
## Language-Specific Rules: Korean (ko)
- Script: Hangul (한글) — syllabic alphabet composed of jamo (consonants/vowels)
- Register: Use 해요체 (haeyoche) — polite conversational level (〜해요, 〜이에요, 〜세요).
  This is the standard for consumer apps in Korea (Naver, Kakao use this).
  Button labels: imperative form OK → "저장하기" (to save), "작성하기" (to write)
  Messages: 해요체 past "기록이 저장되었어요."
  NEVER use 해라체 (해라, 하라) — sounds like a military order, rude in app UI.
  AVOID 합쇼체 (〜합니다/입니다) — too formal, feels like a government form.
- Plurals: Use ONLY the "other" quantity — Korean has no grammatical plural.
  A 들 suffix exists but is rarely used in app UI text.
- Text will be typically 10-20% SHORTER than German.
- "일기" for "Tagebuch/Journal", "기록" for "Eintrag/Entry"
- "저장하기" for "Speichern/Save" (button form), "설정" for "Einstellungen/Settings"
- "무료" for "Kostenlos/Free", "프리미엄" for "Premium" (loanword OK)
- "구독" for "Abonnement/Subscription" (native word preferred over loanword)
- "돌아보기" or "회고" for "Rückblick/Retrospective"

CRITICAL LLM WARNING — Speech Level Mixing:
- LLMs frequently mix 합쇼체 (formal written), 해요체 (polite spoken), and 해라체
  (plain/imperative) within the same translation set. This sounds completely inconsistent
  to native speakers and makes the app feel broken.
- Google Translate is specifically known to fail at honorific consistency in Korean.
  Human post-editing is always recommended.

CRITICAL LLM WARNING — Particle Errors:
- Korean particles (subject/object markers) change based on vowel/consonant ending
  of the preceding word. LLMs get this wrong ~15-20% of the time:
  은/는 (topic), 이/가 (subject), 을/를 (object), 으로/로 (direction).
  A wrong particle is immediately noticeable to native speakers.

Font rendering:
- Noto Sans KR recommended. Android ships with system Korean fonts but they vary by
  manufacturer (Samsung adds its own). Test on stock Android + Samsung One UI.
- Jamo rendering: Hangul composed of jamo in syllable blocks. Incomplete jamo sequences
  during input can render as standalone characters. Ensure TextInputLayout handles
  composition correctly.
- Use android:breakStrategy="balanced" for Korean text blocks — word-breaking at compound
  nouns can look unnatural with default strategy.

LLM-specific note:
- Claude Opus 4 and Sonnet 3.7 rank among top for Korean (Intento 2025).
- Qwen-MT also strong with human evaluation approval for Korean.
```

### 4.6 Chinesisch Vereinfacht (zh-Hans) — Festland-China

```
## Language-Specific Rules: Simplified Chinese (zh-Hans)
- Script: Simplified Chinese characters (简体中文) — for mainland China ONLY
- Register: Use conversational Mandarin (口语化) — natural, modern, informal-but-clean.
  Reference: WeChat, Douyin (TikTok), Alipay UI language.
  Avoid classical/literary structures (之, 其, 乃).
  Use common internet-era vocabulary (已经 not 已然, 设置 not 设定).
  Do NOT use 您 (nin, overly formal) — use 你 or impersonal constructions.
- Plurals: Use ONLY the "other" quantity — Chinese has no grammatical plural
- Text will be 30-40% SHORTER than German — the most dramatic compression of all languages.
  Example: "Tageseintrag wurde gespeichert" (34 chars) → "日记已保存" (5 chars)
  UI layouts may have significant empty space — do NOT stretch strings to fill width.
- "日记" for "Tagebuch/Journal", "条目" or "记录" for "Eintrag/Entry"
- "保存" for "Speichern/Save", "设置" for "Einstellungen/Settings" (NOT 设定)
- "免费" for "Kostenlos/Free", "高级版" or "专业版" for "Premium" (NOT 付费版 — negative framing)
- "订阅" for "Abonnement/Subscription", "回顾" for "Rückblick/Retrospective"
- "应用" or "应用程序" for "App" (NOT 應用程式 — that is Traditional/Taiwan)
- Numbers: Use half-width Arabic numerals
- Use full-width punctuation: 。，！？（）""『』

CRITICAL LLM WARNING — Traditional Character Leakage:
- LLMs trained on mixed CJK data frequently insert Traditional Chinese characters into
  Simplified output. Example: 軟體 (Traditional, Taiwan) instead of 软件 (Simplified).
  A SINGLE Traditional character in zh-Hans output is immediately visible to mainland users
  and signals poor quality. Post-validate every string.
- Even when generating Simplified characters, LLMs use Taiwan vocabulary (應用程式 for "app"
  instead of mainland 应用程序 or 应用).

CRITICAL LLM WARNING — Over-Formal Register:
- LLMs default to written/formal Chinese that sounds like a news broadcast.
  Consumer apps in China use conversational register — check for unnaturally stiff phrasing.

CRITICAL — Han Unification Bug (Android):
- Without explicit xml:lang="zh-Hans" or Locale config, Android may render Chinese characters
  using Japanese glyph variants. Always set locale via LocaleList in Android 7.0+.
- Font: Noto Sans SC (Simplified Chinese) recommended. Specify locale explicitly.
- Minimum font size: 12sp is acceptable for Chinese (denser info packing than Latin).
- Punctuation: LLMs sometimes insert half-width punctuation (,!) — must use fullwidth (，！)

Words/topics to avoid in mainland China context:
- References to VPN as a positive feature
- Politically sensitive geographic references — use neutral framing for all geo content.

LLM-specific note:
- Gemini 2.5 Pro leads for Chinese specifically (Intento 2025).
- Qwen-MT dominates CJK benchmarks with human evaluation approval.
- GPT-4 excels in high-resource Chinese pairs. Even GPT-4 makes errors in 28% of Chinese
  idiom (成语) translations — avoid idiom-heavy source strings.
```

### 4.7 Chinesisch Traditionell (zh-Hant) — Taiwan & Hongkong

```
## Language-Specific Rules: Traditional Chinese (zh-Hant)
- Script: Traditional Chinese characters (繁體中文) — for Taiwan AND Hong Kong
- This is a COMPLETELY SEPARATE translation from Simplified Chinese, NOT character conversion.
  Vocabulary, expressions, and register differ significantly from mainland China.
- Register: Use formal written standard (書面語) with Taiwan vocabulary.
  Polite but not over-formal. Reference: LINE Taiwan, Apple Taiwan UI.
- Plurals: Use ONLY the "other" quantity — no grammatical plural
- Text will be 30-40% SHORTER than German — same compression as zh-Hans.
- "日記" for "Tagebuch/Journal", "記錄" or "條目" for "Eintrag/Entry"
- "儲存" or "保存" for "Speichern/Save", "設定" for "Einstellungen/Settings"
- "免費" for "Kostenlos/Free", "進階版" or "專業版" for "Premium"
- "訂閱" for "Abonnement/Subscription", "回顧" for "Rückblick/Retrospective"
- Numbers: Use half-width Arabic numerals
- Use full-width punctuation: 。，！？（）「」

CRITICAL LLM WARNING — Simplified Character Leakage:
- LLMs trained predominantly on Simplified data insert Simplified characters into Traditional
  output. Example: 软件 (Simplified) instead of 軟體 (TW) or 軟件 (HK).
  This is immediately obvious to native readers and signals very poor quality.
- LLMs often generate Traditional-script text but with MAINLAND vocabulary and sentence
  structures — technically correct characters but wrong regional vocabulary.
- LLMs show measurable bias toward Simplified Chinese (arXiv 2505.22645) — even
  Traditional-oriented models underperform on Traditional-specific regional terms.

Taiwan (zh-TW) vs Hong Kong (zh-HK) — Key Differences:
- Both use Traditional characters, but Taiwan speaks Mandarin while HK speaks Cantonese.
- Key vocabulary differences:
  | Concept     | Taiwan zh-TW | Hong Kong zh-HK | Mainland zh-CN |
  | Software    | 軟體         | 軟件            | 软件           |
  | App         | 應用程式     | 應用程式        | 应用程序       |
  | Taxi        | 計程車       | 的士            | 出租车         |
  | Click       | 點選/按      | 撳/按           | 点击           |
  | Settings    | 設定         | 設定/設置       | 设置           |
- Practical recommendation: Ship separate zh-TW and zh-HK resource files.
  The delta is ~15-25% of strings. Use zh-TW as the primary Traditional locale.

CRITICAL — Han Unification Bug (Android):
- Specify xml:lang="zh-Hant" or zh-TW/zh-HK explicitly. Without it, system may render
  characters with wrong glyph variants (Japanese or Simplified Chinese).
- Font: Noto Sans TC (Traditional Chinese) for zh-TW. Noto Sans HK available for zh-HK.

LLM-specific note:
- Claude 3.5/4 strong for Traditional Chinese but needs explicit prompting.
- Qwen-1.5 anomaly: answers correctly when prompted in Simplified but incorrectly when
  prompted in Traditional for the same question — avoid using Qwen for zh-Hant without testing.
```

### 4.8 Hindi (hi) — Indien

```
## Language-Specific Rules: Hindi (hi)
- Script: Devanagari (देवनागरी)
- Register: Use "आप" (aap) — the formal/polite second person. "तुम" (tum) is too casual
  for app UI; "तू" (tu) is intimate/rude in non-personal contexts.
  Standard Android/iOS apps in Hindi use "आप" universally.
  For a personal journal app, lean toward SEMI-FORMAL HINGLISH — use Hindi grammar but
  allow common English tech terms (Settings, Account, Premium) rather than forcing
  Sanskrit equivalents. This is how Hindi speakers actually use apps.
- Plurals: one, other (same as English — very simple)
- Text length: Hindi is typically 10-30% SHORTER than German in character count but
  visually similar in width due to Devanagari glyph width.
- "डायरी" (diary, English loanword) for "Tagebuch/Journal" — standard term
- "प्रविष्टि" (pravishti) or "एंट्री" (Entry, Hinglish) for "Eintrag/Entry"
- "सेटिंग्स" (Settings, English) for "Einstellungen" — universally used
- "सहेजें" for "Speichern/Save", "हटाएं" for "Löschen/Delete"
- "मूड" (mood, English loanword) for "Stimmung/Mood" — natural in Hindi
- Numbers: Use Arabic numerals (0-9), NOT Devanagari numerals (०-९).
  Most Hindi speakers use Arabic numerals in digital contexts.

CRITICAL LLM WARNING — English Word Leakage / Hinglish:
- LLMs trained on web data have absorbed massive amounts of Hinglish (mixed Hindi-English).
  They produce outputs like "आपका account activate हो गया है" when pure Hindi would say
  "आपका खाता सक्रिय हो गया है". For app UI, some Hinglish is DESIRED (see Register),
  but LLMs do it INCONSISTENTLY — sometimes pure Hindi, sometimes Hinglish, with no
  predictable pattern. Explicitly specify which English terms to keep and which to translate.

CRITICAL LLM WARNING — Sanskrit-Heavy Formal Register:
- LLMs default to Sanskritized vocabulary (शब्दावली like "सुरक्षित करें" for "save") that
  feels overly formal for a personal journal app. Prefer colloquial Hindi where possible.
- The Sanskritization vs Persianization debate: for app UI, use the word most commonly
  used in everyday speech, regardless of etymology.

Cultural specifics:
- Diary/journal culture (डायरी लेखन) is well-established in India — part of school curriculum.
- Privacy is important — emphasize that entries are personal and not shared.
- Avoid religious/caste references in example content.

Font rendering:
- Noto Sans Devanagari is bundled on all Android 5.0+ devices. No custom font needed.
- Devanagari conjuncts (two-consonant combinations like क्ष, त्र, ज्ञ) are rendered as single
  glyphs. Unicode segmentation can break these wrongly. Android's HarfBuzz engine (API 23+)
  handles this correctly.
- Matras (vowel marks) render above, below, before, or after consonants — never use
  fixed-width character assumptions.
- Line height: Devanagari needs ~20-25% more line height than Latin due to the horizontal
  bar (शिरोरेखा) and descending vowel marks. Use lineSpacingMultiplier="1.3" minimum.

LLM-specific note:
- IndicTrans2 (AI4Bharat) outperforms all commercial MT by 4-8 BLEU/chrF++ for EN→Hindi.
- GPT-4 / Claude both handle formal Hindi well. For German→Hindi, use English pivot
  (DE→EN→HI) which outperforms direct German→Hindi in practice.
- Hindi is high-resource — all major LLMs have substantial Hindi training data.
```

### 4.9 Arabisch (ar) — Modernes Hocharabisch

```
## Language-Specific Rules: Arabic (ar)
- Script: Arabic (العربية) — RIGHT-TO-LEFT (RTL) language
- This is RTL: All layout attributes MUST use start/end instead of left/right.
  android:supportsRtl="true" in AndroidManifest.xml is MANDATORY.
- Use Modern Standard Arabic (MSA / فصحى) — the ONLY variant universally understood
  across all 22+ Arab countries. Dialectal Arabic (Egyptian, Levantine, Gulf) would
  exclude ~70% of potential users who speak a different dialect.
- Register: Semi-formal, warm tone. Privacy messaging is important — emphasize
  "only for you" / "لك فقط" in onboarding.
- CRITICAL — Plurals: Arabic requires ALL SIX quantity forms:
  - zero (للصفر)
  - one (للواحد)
  - two (للاثنين)
  - few (للقليل — 3-10)
  - many (للكثير — 11-99)
  - other (لغير ذلك — 100+)
  Generate ALL SIX for every <plurals> element. This is the MOST COMPLEX plural system
  of all major languages. Missing quantities WILL crash or display wrong text.
- Text expansion: Arabic is typically +25% longer than German. Use wrap_content height
  and avoid fixed-height text containers.
- "مذكرات" (mudhakaraat) for "Tagebuch/Journal" — standard term
- "تسجيل" for "Eintrag/Entry" (feels more natural than "إدخال" for journal context)
- "الإعدادات" for "Einstellungen/Settings", "حفظ" for "Speichern/Save"
- "حذف" for "Löschen/Delete", "المزاج" for "Stimmung/Mood"
- "اشتراك" for "Abonnement/Subscription"
- Tashkeel (diacritical marks): Do NOT include in app UI. Tashkeel is for religious texts
  and children's books. Modern adult readers find them patronizing.
- Arabic question mark is ؟ (U+061F), NOT ? — LLMs sometimes forget this.

CRITICAL LLM WARNING — Gender Agreement Errors:
- Arabic has grammatical gender for ALL nouns (masculine/feminine), and every adjective,
  verb, and pronoun must agree. LLMs routinely use masculine defaults for gender-neutral
  German sentences. ~60% of all Arabic LLM translation errors are gender-related.
- Example: "Dein Eintrag" → correct Arabic requires knowing if "your" refers to male/female.
  Use masculine default and gender-neutral formulations where possible.

CRITICAL LLM WARNING — Dialect Mixing:
- LLMs drift from MSA into Egyptian or Levantine mid-text without warning,
  producing inconsistent output. Always verify MSA consistency across all strings.

Bidirectional text (BiDi):
- Numbers stay LTR within RTL text. Android handles this via Unicode BiDi algorithm,
  but mixed strings (e.g., "سجل 5 ملاحظات") need textDirection="locale" or explicit
  BiDi markers (U+200F RIGHT-TO-LEFT MARK).
- English brand names and tech terms embedded in Arabic text must maintain LTR direction.
- Use Unicode BiDi control characters where needed for mixed-direction text.

Font rendering:
- Arabic letters have 4 forms (isolated, initial, medial, final). The system font
  (Noto Naskh Arabic) handles this automatically — never use bitmap fonts.
- Arabic ligatures are handled by the HarfBuzz text engine on Android 5.0+.

Cultural specifics:
- Journaling is culturally acceptable but often private — emphasize privacy.
- Emotional expression in Arabic apps tends to be more formal than Western apps.
- Religious references (Alhamdulillah, Inshallah) appear organically in user content.
- Avoid alcohol, relationship, or religious-criticism topics in example prompts.

LLM-specific note:
- Claude 3.5 Sonnet confirmed as top performer for Arabic (WMT24, cultural nuance tests).
- GPT-4 strong for MSA, struggles with dialects and informal register.
- DeepL added Arabic support mid-2024 — still behind Claude/GPT-4 for Arabic specifically.
```

### 4.10 Tuerkisch (tr)

```
## Language-Specific Rules: Turkish (tr)
- Script: Latin with special characters (ç, ğ, ı, İ, ö, ş, ü)
- Register: Use informal "sen" form, NOT formal "siz". Turkish mobile apps (especially
  lifestyle/personal) use "sen" almost universally. "Siz" is for government portals and
  banks. "Senin günlüğün" (your diary) feels intimate — "Sizin günlüğünüz" sounds corporate.
- Plurals: one, other. IMPORTANT: Turkish nouns do NOT change form after numbers.
  "5 giriş" NOT "5 girişler" — the plural suffix (-ler/-lar) is NOT used after numbers.
  LLMs sometimes incorrectly add the plural suffix after numbers.
- Text expansion: +30-40% for description texts vs German. Short verb strings (buttons)
  are often similar length. Plan for flex layouts for longer texts.
- "Günlük" for "Tagebuch/Journal" (literally "daily" — perfect word)
- "Giriş" or "Kayıt" for "Eintrag/Entry", "Ruh hali" for "Stimmung/Mood"
- "Kaydet" for "Speichern/Save" (imperative, correct for button), "Sil" for "Löschen/Delete"
- "Ayarlar" for "Einstellungen/Settings", "Hatırlatıcı" for "Erinnerung/Reminder"
- "Abonelik" for "Abonnement/Subscription", "Ücretsiz" for "Kostenlos/Free"

CRITICAL — THE İ/I CATASTROPHE (Android/Kotlin):
- Turkish has 4 "I" letters: i (dotted lowercase), İ (dotted uppercase),
  ı (dotless lowercase), I (dotless uppercase)
- Java/Kotlin String.toUpperCase() WITHOUT explicit Locale turns i → İ in Turkish locale
- Java/Kotlin String.toLowerCase() WITHOUT explicit Locale turns I → ı in Turkish locale
- RESULT: "title".toUpperCase() in Turkish locale = "TİTLE" NOT "TITLE" —
  causes comparison failures, routing bugs, CRASHES
- FIX: ALWAYS use toUpperCase(Locale.ROOT) or toUpperCase(Locale.ENGLISH) for
  non-display strings. This bug famously broke the Kotlin compiler itself.
- This is NOT a translation issue but a CODE issue triggered by Turkish locale!

CRITICAL LLM WARNING — Vowel Harmony Violations:
- Turkish suffix vowels MUST harmonize with the root vowel (back/front, rounded/unrounded).
  Example: "kitap" (book) → "kitapta" (in the book) NOT "kitepte".
  LLMs generate vowel harmony violations especially with compound words or loanwords.
  This sounds COMPLETELY WRONG to native speakers — like broken grammar.

CRITICAL LLM WARNING — Agglutination Errors:
- Turkish expresses in suffixes what German expresses in prepositions + articles.
  "ev-de" (at home), "ev-den" (from home), "ev-e" (to home).
  LLMs sometimes stack wrong suffixes or omit required ones.
- Foreign loanwords need correct suffix attachment via buffer vowels.
  "App-i" (accusative of app) — LLMs sometimes skip the buffer vowel.
- LLMs mix "sen" and "siz" within the same output block — register inconsistency.

Font rendering:
- Noto Sans fully covers all Turkish special characters: ç ğ ı İ ö ş ü
- CRITICAL: The dotless ı (U+0131) and dotted İ (U+0130) — ensure fonts support both.
- Some older custom fonts treat ğ as "g with breve" and render it as a question mark.
  Test explicitly with the default system font.

LLM-specific note:
- GPT-4 class models perform best for Turkish (TurkishMMLU, TurkBench 2025).
- Turkish tokenization is highly inefficient in standard subword tokenizers — Turkish words
  require substantially more subword tokens than English/German, which degrades quality
  and increases API cost.
- Claude 3.5 is also strong. Gemini less tested for Turkish specifically.
```

### 4.11 Russisch (ru)

```
## Language-Specific Rules: Russian (ru)
- Script: Cyrillic (Кириллица)
- Register: Use formal "Вы" (capitalized) for a personal journal app targeting adults 20+.
  Easier to maintain consistency, prevents gendered language issues (ты forms often assume
  gender), and is the safe default. Major Russian apps (Yandex, VK) started with Вы.
  Exception: teen/children apps use ты.
- Plurals: Russian requires FOUR quantity forms:
  - one (1, 21, 31...)    → "запись"
  - few (2-4, 22-24...)   → "записи"
  - many (5-20, 25-30...) → "записей"
  - other (decimals)      → "записи"
  Generate ALL FOUR for every <plurals> element.
  LLMs frequently forget "many" and collapse to 3 forms — produces wrong output for 5-20.
- Text expansion: Russian is ~15-20% longer than German. Russian tends to be verbose for
  compound concepts Germans express in one word ("Tagebucheintrag" = "запись в дневнике").
- "Дневник" for "Tagebuch/Journal", "Запись" for "Eintrag/Entry"
- "Сохранить" for "Speichern/Save" (perfective infinitive — CORRECT for buttons)
- "Удалить" for "Löschen/Delete", "Настройки" for "Einstellungen/Settings"
- "Настроение" for "Stimmung/Mood", "Напоминание" for "Erinnerung/Reminder"
- "Подписка" for "Abonnement/Subscription" (NOT "абонемент"!)
- "Премиум" for "Premium" (loanword, unchanged)

CRITICAL LLM WARNING — Aspect System Blindness:
- Russian has perfective/imperfective aspect for EVERY verb. LLMs routinely use the wrong
  aspect. "Save" button = "Сохранить" (perfective) NOT "Сохранять" (imperfective).
  Wrong aspect in UI buttons sounds completely unnatural.
- Verb form in buttons: Use INFINITIVE (сохранить) in Russian UI — LLMs sometimes produce
  imperative (сохрани/сохраните) which sounds commanding/rude.

CRITICAL LLM WARNING — Case Endings with Variables:
- Strings with embedded numbers/variables break Russian grammar.
  "%d entries" — the word "запись" declines differently at 1/2/5/21.
  LLMs often just use nominative everywhere — produces grammatically wrong output.

CRITICAL LLM WARNING — Ё vs Е:
- The letter "ё" is often omitted (written as "е"), which changes meaning in some words.
  LLMs frequently strip ё. Decide upfront: include ё for correctness (recommended).

Cultural specifics:
- Diary/journal writing has strong cultural tradition in Russia (literary heritage).
- Privacy very important — emphasize "только для вас" / "никто не увидит".
- Dates should use Cyrillic month names for warmth: "16 апреля" (not just numbers).

Font rendering:
- Noto Sans CIS (Cyrillic) fully covers all Russian characters including ё and ъ.
- Roboto Cyrillic is the default Android system font — renders perfectly.
- Watch: Some custom fonts lack ё glyph — falls back to different font, creating mixed rendering.

LLM-specific note:
- Claude 3.5 Sonnet ranked #1 in 9/11 language pairs at WMT24 including Russian.
- Russian (despite being high-resource) shows measurably lower LLM scores than German/French.
- GPT-4o is the most stable fallback for Russian.
```

### 4.12 Polnisch (pl)

```
## Language-Specific Rules: Polish (pl)
- Script: Latin with special diacritics (ą, ę, ć, ł, ń, ó, ś, ź, ż)
- Register: Use informal "ty" form. Polish mobile apps (especially wellness/personal)
  consistently use "ty". "Twój dziennik" feels right — "Pański dziennik" feels like
  a tax office. Young/millennial Polish users find Pan/Pani in apps condescending.
- Plurals: Polish requires THREE quantity forms (CLDR):
  - one (1)                          → "wpis"
  - few (2-4, 22-24, 32-34...)      → "wpisy"
  - other (0, 5-21, 25-31...)       → "wpisów"
  Polish "few" rule: numbers ending in 2-4 (EXCEPT 12-14) use genitive singular.
  LLMs routinely confuse "few" and "other" forms.
- Text expansion: Polish is 10-20% longer than German. Compound German words become
  multi-word Polish phrases: "Tagebucheintrag" = "wpis w dzienniku".
- "Dziennik" for "Tagebuch/Journal", "Wpis" for "Eintrag/Entry"
- "Zapisz" for "Speichern/Save" (imperative, correct for button), "Usuń" for "Löschen/Delete"
- "Ustawienia" for "Einstellungen/Settings", "Nastrój" for "Stimmung/Mood"
- "Przypomnienie" for "Erinnerung/Reminder", "Subskrypcja" for "Abonnement/Subscription"
- "Bezpłatny" or "Darmowy" for "Kostenlos/Free" (darmowy = colloquial, warmer)
- "Dziś" or "Dzisiaj" for "Heute/Today" (dziś slightly more colloquial)

CRITICAL LLM WARNING — Gender Agreement Failure:
- Polish has 3 grammatical genders + animate/inanimate masculine distinction.
  LLMs default to masculine when gender is ambiguous. Wrong gender agreement sounds
  native-speaker-wrong immediately. Example: "Twój wpis" (masc) vs "Twoja notatka" (fem).
- Past-tense gender in verbs: "zapisałem" (I/male saved) vs "zapisałam" (I/female saved).
  LLMs often default to masculine, offending female users.
  Prefer impersonal constructions where possible to avoid this.

CRITICAL LLM WARNING — Variable Plural Strings:
- "Almost every LLM" tested had issues with variables in plural contexts (Localazy study).
  "%d wpisów" vs "%d wpisy" vs "%d wpis" — models pick the wrong form.

CRITICAL LLM WARNING — Case Prepositions:
- Prepositions in Polish govern specific cases. LLMs frequently choose wrong case after
  prepositions (e.g., "o" + locative, not accusative).
- Literal translation of idioms: "daily reflection" → "codzienna refleksja" sounds clinical;
  native = "codzienne przemyślenia".

Font rendering:
- Noto Sans fully covers all Polish diacritics: ą ę ć ł ń ó ś ź ż
- Roboto (Android default) fully supports Polish characters.
- No special font needed; all modern Android fonts support Polish.

LLM-specific note:
- DeepSeek-V3 and Gemini 2.0 scored 22/30 in comparative Polish tests (Localazy).
- Claude 3.5 Sonnet also rates highly. ChatGPT syntax error rate: 35% of all errors in
  Polish are syntactic — specifically compound agreement structures.
- Polish classified as "medium-resource" (ACL 2024) — 10-15% quality gap vs French/German.
```

### 4.13 Thai (th)

```
## Language-Specific Rules: Thai (th)
- Script: Thai script (ไทย) — NO spaces between words, NO uppercase/lowercase distinction
- Register: Use neutral/informal register WITHOUT politeness particles in UI text.
  Thai app UI convention: buttons and labels use short, direct forms without ครับ/ค่ะ
  (these are spoken-language particles, NOT written UI text).
  Correct button: "บันทึก" (save), NOT "บันทึกครับ".
  For descriptive text: polite neutral — formal enough to be respectful, informal enough
  to feel friendly. คุณ (khun) is the standard second-person pronoun for app UI.
- Plurals: Use ONLY the "other" quantity — Thai has no grammatical plural.
  Numbers simply precede the noun: "5 รายการ" — same form as "1 รายการ".
- Text length: Thai is typically 20-40% SHORTER than German in visual width.
- "สมุดบันทึก" for "Tagebuch/Journal", "บันทึก" for "Eintrag/Entry" (also = "save"!)
- "การตั้งค่า" for "Einstellungen/Settings", "บันทึก" for "Speichern/Save"
- "ลบ" for "Löschen/Delete", "อารมณ์" for "Stimmung/Mood"
- "ทบทวน" for "Rückblick/Retrospective"
- Use Arabic numerals (0-9), NOT Thai numerals (๐-๙). Most Thai users prefer Arabic.

CRITICAL LLM WARNING — Word Segmentation:
- Thai has NO spaces between words. Word boundaries are implied from context.
  LLMs sometimes produce text with incorrect segmentation — words run together wrongly
  or are split at wrong points. This is INVISIBLE in the output but causes issues with:
  (1) text selection/copy, (2) line-breaking on Android, (3) screen reader pronunciation.
- Android's built-in Thai word-breaking (via ICU library) works but can fail on unusual words.
  Test with long compound words.

CRITICAL LLM WARNING — Wrong Politeness Particles:
- Thai sentences end with ครับ (khrap, masculine) or ค่ะ/ค้ะ (kha, feminine).
  LLMs often include these in UI text where they are INAPPROPRIATE (buttons, labels
  should be neutral). Remove all ครับ/ค่ะ from UI strings.

CRITICAL LLM WARNING — Formal/Archaic Vocabulary:
- LLMs default to written formal Thai that sounds like GOVERNMENT DOCUMENTS rather than
  friendly app UI. Check for unnaturally stiff phrasing.

CRITICAL — Font Rendering / Line Height:
- Thai has diacritics ABOVE and BELOW the baseline — vowels can stack 2-3 levels above
  consonants, and tone marks add another level.
- Default Android line height WILL cause CLIPPING of Thai diacritics.
  Use lineSpacingMultiplier="1.4" minimum, or lineSpacingExtra="4dp".
- Noto Sans Thai is bundled on Android 5.0+. Thai rendering requires OpenType GSUB/GPOS
  tables — do not use simple bitmap fonts.

Cultural specifics:
- Journaling associated with self-improvement and mindfulness — positive framing in Thailand.
- Buddhist concepts of reflection resonate strongly for journal apps.
- Privacy matters greatly; Thais are generally reserved about emotional expression publicly.
- "Face" (หน้า) concept — avoid embarrassing prompts or examples.
- Monarchy references: extreme caution (lese-majeste laws).

LLM-specific note:
- Thai is a mid-resource language. LLM quality is moderate — below English/German.
- Claude 3.5 Sonnet best overall multilingual performance for Thai.
- No specialized German→Thai model exists — all models route through English internally.
- Human review strongly recommended.
```

### 4.14 Indonesisch (id)

```
## Language-Specific Rules: Indonesian (id)
- Script: Latin — no special font requirements (minimal Android complexity)
- Register: Use "kamu" for a personal journal app. "Anda" is for airport announcements
  and product manuals — too formal for a diary app. Mozilla's Indonesian localization
  guidelines and app store conventions lean toward "kamu" for consumer apps.
  NOTE: "Anda" is ALWAYS capitalized when used — a detail LLMs sometimes get wrong.
- Plurals: Use ONLY the "other" quantity — Indonesian has no grammatical plural.
  Plurality is expressed by context or reduplication (buku-buku = books).
- Text length: Indonesian is typically 10-20% SHORTER than German. Compound German words
  become phrase constructions in Indonesian (catatan harian) — usually shorter.
- "Jurnal" or "catatan harian" for "Tagebuch/Journal" ("jurnal" increasingly common)
- "Catatan" for "Eintrag/Entry" (= note, more natural than "entri")
- "Simpan" for "Speichern/Save", "Hapus" for "Löschen/Delete"
- "Pengaturan" for "Einstellungen/Settings" (standard Android term)
- "Suasana hati" or "mood" for "Stimmung/Mood" (English loanword natural)
- "Kilas balik" for "Rückblick/Retrospective" (kilas=flash, balik=back)
- "Langganan" for "Abonnement/Subscription"

CRITICAL LLM WARNING — Affix System Errors:
- Indonesian uses a complex morphological system: prefix me- (active voice),
  di- (passive voice), ber- (stative/habitual), ke-...an (nominalization),
  pe-...an (process noun). LLMs confuse voice: "Anda dapat menyimpan" (active)
  vs "dapat disimpan" (passive). In app UI, active voice with me- prefix is preferred
  but LLMs sometimes produce passive di- constructions that feel awkward.

CRITICAL LLM WARNING — Register Inconsistency:
- LLMs mix formal "Anda" and informal "kamu" within the same translation output.
  Explicitly specify "use kamu throughout, never Anda" in prompts.

Indonesian vs Malay (ms) — Key Differences:
- If also targeting Malaysia, be aware:
  | Concept     | Indonesian (id) | Malay (ms)  |
  | delete      | hapus           | padam       |
  | settings    | pengaturan      | tetapan     |
  | account     | akun            | akaun       |
  | save        | simpan          | simpan      |
- Indonesian and Malay share ~80% vocabulary but diverge on key app terms.
  They require separate translations.

Dutch loanwords in Indonesian:
- Indonesian has ~3,000-5,000 Dutch loanwords (polisi=politie, gratis=gratis,
  kantor=kantoor, kualitas=kwaliteit). No cultural sensitivity around these.

Cultural specifics:
- Indonesia has a strong journaling tradition in school culture (menulis jurnal/diary).
- Privacy valued — emphasize data security in onboarding.
- Predominantly Muslim — avoid alcohol/pork references in example content.
- Emoji culture is very strong among Indonesian users.

Font rendering:
- Latin script — no special font requirements. Standard Roboto/Noto works perfectly.

LLM-specific note:
- Indonesian is well-represented in training data (large web presence).
- Claude 3.5 Sonnet and GPT-4 both perform well. DeepL supports Indonesian.
- Quality is good — better than Thai, slightly below Hindi.
```

### 4.15 Italienisch (it)

```
## Language-Specific Rules: Italian (it)
- Script: Latin — no special font requirements
- Register: Use informal "tu/tuo/tua", NOT formal "Lei". Italian tech apps (Apple, Google,
  Spotify) all use "tu". "Lei" would be bizarre for a diary product.
  Explicitly specify "usa il tu informale, non il Lei formale" in prompts.
  LLMs sometimes default to Lei for software strings.
- Plurals: one, other
- Text expansion: Italian is 10-20% longer than German. Uses more articles and prepositions.
  Design with ~15% extra space.
- "Diario" for "Tagebuch/Journal", "Voce" for "Eintrag/Entry"
- "Salva" for "Speichern/Save", "Elimina" for "Löschen/Delete"
- "Impostazioni" for "Einstellungen/Settings", "Umore" for "Stimmung/Mood"
- "Cerca" for "Suchen/Search", "Scrivi" for "Schreiben/Write"
- "Oggi" for "Heute/Today"

CRITICAL LLM WARNING — Lei Capitalization:
- When using formal register (which we avoid, but LLMs may produce it anyway), Italian
  requires Lei (CAPITALIZED) to mean formal "you" — lowercase "lei" means "she".
  LLMs frequently forget to capitalize it, causing grammatical errors that native
  speakers notice immediately.

CRITICAL LLM WARNING — Register Inconsistency:
- Italian formal Lei requires THIRD-PERSON SINGULAR verb conjugation (Lei vuole, not
  tu vuoi). LLMs sometimes mix tu/Lei conjugations in the same string set — sounds illiterate.
- False gender assignments: Italian grammatical gender confuses LLMs producing adjective-noun
  agreement errors in longer strings.
- LLMs underuse the Italian subjunctive (che tu abbia vs che tu hai) — produces slightly
  unnatural but not incomprehensible text.

Regional variations:
- Standard Italian (italiano standard) is universal. Regional dialects (Neapolitan,
  Venetian, Sicilian) are not used in app UI. No regional split needed.

Font rendering:
- Standard Roboto/Noto handles all Italian characters (à, è, é, ì, ò, ù).
- No special rendering concerns — Italian is the safest of all languages for Android rendering.

LLM-specific note:
- Claude 3.5 Sonnet and GPT-4o both perform at near-human level for Italian.
- DeepL specialized LLM shows particularly strong results for Italian.
- High-resource European language — all major LLMs perform well.
```

### 4.16 Niederlaendisch (nl) — Niederlande

```
## Language-Specific Rules: Dutch (nl-NL)
- Script: Latin — no special font requirements
- Register: Use informal "je/jij" (Netherlands Dutch). "U" feels government-bureaucratic
  in NL context. Netherlands digital apps universally use informal register.
  NOTE: For Belgian Flemish (nl-BE), use "u" — Flemish users expect formal register.
  Recommendation: Target nl-NL with "je" as default. A personal journal app is inherently
  informal. Flemish users can be served with separate nl-BE locale if Belgium is a market.
- Plurals: one, other
- Text expansion: ~0% to +5% vs German — Dutch and German are structurally very similar.
  This is positive: Dutch strings rarely overflow German UI layouts.
- "Dagboek" for "Tagebuch/Journal", "Inschrijving" or "Notitie" for "Eintrag/Entry"
- "Opslaan" for "Speichern/Save", "Verwijderen" for "Löschen/Delete"
- "Instellingen" for "Einstellungen/Settings", "Stemming" for "Stimmung/Mood"
- "Zoeken" for "Suchen/Search", "Schrijven" for "Schreiben/Write"
- "Vandaag" for "Heute/Today"

CRITICAL LLM WARNING — German→Dutch is the HIGHEST-RISK pair due to extreme similarity:
- False friends (valse vrienden) — the most dangerous LLM mistakes:
  | German        | LLM produces (WRONG Dutch) | Correct Dutch  | Why wrong                          |
  | wie (how)     | wie                        | hoe            | Dutch "wie" = "who", not "how"     |
  | mögen (like)  | mogen                      | houden van     | Dutch "mogen" = "allowed to"       |
  | Meer (sea)    | meer                       | zee            | Dutch "meer" = "lake", not "sea"   |
  | Tafel (board) | tafel                      | bord           | Dutch "tafel" = "table"            |
  | verstehen     | verstaan                   | begrijpen      | Dutch "verstaan" = "hear/spoken"   |
- LLMs exploit German surface similarity and produce WRONG Dutch words that look right.
  These errors are subtle — a non-native reviewer will NOT catch them.

CRITICAL LLM WARNING — English Interference:
- 16% of ALL annotated linguistic errors in Dutch LLM output have a clear link to
  English interference (HumEval 2024). LLMs produce "Dutch" text that is actually
  English sentence structure in Dutch words. Worse for NL than for Romance languages
  because English and Dutch are closely related.

CRITICAL LLM WARNING — Separable Verbs:
- Dutch separable verbs (opslaan, verwijderen, instellen) get merged or split incorrectly
  by LLMs — same issue as German separable verbs but with different rules.

Belgian Dutch (Flemish) vs Netherlands Dutch:
- nl-NL: "je/jij" standard, direct/casual tone
- nl-BE: "u" dominant, more formal overall, some vocabulary differences
  (schoonbroer vs zwager), archaic "ge/gij" still used
- For BestJournal: nl-NL as default. Separate nl-BE locale optional.

Font rendering:
- Standard Roboto/Noto handles all Dutch characters (ij, é, ë, ï).
- The digraph "ij" (treated as a single letter in Dutch) renders correctly.
- No special rendering concerns.

LLM-specific note:
- Claude 3.5 and GPT-4o both perform well for Dutch.
- Fietje (2024): open-source Dutch-focused LLM available for comparison.
- German→Dutch requires EXTRA vigilance — the language similarity makes errors harder to spot.
```

### 4.17 Ukrainisch (uk)

```
## Language-Specific Rules: Ukrainian (uk)
- Script: Cyrillic (Кирилиця) — Ukrainian Cyrillic, NOT Russian
- Register: Use formal "ви" for a journal app. Ukrainian digital norms favor "ви" in app UIs
  as respectful default. Post-2022 context: Ukrainian users are particularly sensitive to
  anything that "feels Russian" — "ви" is the safer, more universally accepted choice.
- Plurals: Ukrainian requires FOUR quantity forms (same structure as Russian, DIFFERENT endings):
  - one (1, 21, 31...)    → "запис"
  - few (2-4, 22-24...)   → "записи"
  - many (5-20, 25-30...) → "записів"
  - other (decimals)      → "записи"
  Generate ALL FOUR for every <plurals> element.
- Text expansion: ~10-20% longer than German (similar to Polish).
- "Щоденник" for "Tagebuch/Journal" (щоденник = daily, from щодня)
- "Запис" for "Eintrag/Entry" (NOT "Запись" — that is RUSSIAN!)
- "Зберегти" for "Speichern/Save" (NOT "Сохранить" — RUSSIAN!)
- "Видалити" for "Löschen/Delete" (NOT "Удалить" — RUSSIAN!)
- "Налаштування" for "Einstellungen/Settings" (NOT "Настройки" — RUSSIAN!)
- "Настрій" for "Stimmung/Mood" (NOT "Настроение" — RUSSIAN!)
- "Нагадування" for "Erinnerung/Reminder" (NOT "Напоминание" — RUSSIAN!)
- "Сьогодні" for "Heute/Today" (NOT "Сегодня" — RUSSIAN!)
- "Підписка" for "Abonnement/Subscription" (NOT "Подписка" — similar but different ending!)
- "Безкоштовно" for "Kostenlos/Free" (NOT "Бесплатно" — RUSSIAN!)

CRITICAL LLM WARNING — POLITICAL SENSITIVITY — Russian Vocabulary Leakage (Russisms):
- This is the MOST CRITICAL WARNING of ALL 25 languages.
  LLMs spontaneously mix Ukrainian and Russian due to training data imbalance.
  Models generate Surzhyk — "conjugating words from one language according to the rules
  of another." For Ukrainian users post-2022, Russian vocabulary in a Ukrainian app
  can feel DEEPLY OFFENSIVE. This is not just a linguistic error — it is a political one.
- Specific Russisms to watch for:
  | WRONG (Russian)   | CORRECT (Ukrainian)  | Meaning          |
  | Конечно           | Звичайно             | of course        |
  | Понятно           | Зрозуміло            | understood       |
  | Сохранить         | Зберегти             | save             |
  | Настройки         | Налаштування         | settings         |
  | Поиск             | Пошук                | search           |
  | Бесплатно         | Безкоштовно          | free             |
- LLMs produce sentences that are grammatically Ukrainian but with Russian lexical roots —
  hard to catch without native review.
- Grammar contamination: Ukrainian has different case endings from Russian. LLMs trained
  on Russian-heavy data introduce Russian endings into Ukrainian text.
- Add explicit instruction: "Use only authentic Ukrainian vocabulary. Avoid all Russisms.
  If in doubt, prefer the Ukrainian-origin word over any Russian/Soviet-era loanword."

MANDATORY: Human native review for Ukrainian — more critical than any other language.

Cultural specifics — Post-War Context:
- NEVER use Soviet-era vocabulary or anything associated with "Soviet" culture branding.
- Diary/journal tradition strong in Ukraine (Shevchenko's diary is iconic).
- Date format: DD.MM.YYYY, Cyrillic month names preferred (Квітень for April).

Font rendering:
- Noto Sans Cyrillic covers all Ukrainian characters including і, ї, є, ґ.
- Ukrainian uses letters NOT found in Russian: і, ї, є, ґ — ensure font coverage.
- Roboto Cyrillic is the default Android system font — renders correctly.

LLM-specific note:
- Gemini 2.5 Pro ranks BEST specifically for Ukrainian (Intento 2025) — notable, unlike
  Russian where Claude leads.
- Ukrainian lags significantly behind Russian in LLM quality due to Russian internet
  domination in training data.
- Open-source gap: Fine-tuned Gemma-7B scored only 33.5/100 on Ukrainian open-ended tasks
  vs GPT-4's 85 — the gap is enormous.
- Use LanguageTool with Ukrainian Russism detection rules for automated validation.
```

### 4.18 Bengali (bn) — Indien & Bangladesch

```
## Language-Specific Rules: Bengali (bn)
- Script: Bengali/Bangla script (বাংলা) — NOT Devanagari
- Register: Use informal "তুমি" (tumi) form — personal and warm, not ultra-formal "আপনি" (apni)
  and not ultra-casual "তুই" (tui). Journals are private and personal, তুমি is correct.
- Plurals: one, other
- Text expansion: Bengali is typically 20-35% longer than German — watch button labels closely
- "দিনলিপি" (dinlipi) or "জার্নাল" (journal, borrowed) for "Tagebuch/Journal"
- "এন্ট্রি" (entry, borrowed) or "লেখা" (lekha, native) for "Eintrag/Entry"
- "মেজাজ" (mejaj) for "Stimmung/Mood"
- "স্মরণিকা" (smaranika) or "রিমাইন্ডার" (reminder, borrowed) for "Erinnerung/Reminder"
- Numbers: Use standard Arabic numerals (0-9), NOT Bengali numerals (০১২৩৪৫৬৭৮৯)

CRITICAL LLM WARNING — Hindi-Leakage:
- LLMs frequently leak Hindi vocabulary and Devanagari-influenced structures into Bengali output.
  Bengali and Hindi are DIFFERENT languages with different scripts. If you find yourself using
  a word that looks like it belongs in Hindi, check if there is a native Bengali alternative.
- Avoid Sanskrit loanwords where native Bengali equivalents exist.
- Urban Bengali users mix English naturally: "Save করুন" (Save korun) is authentic code-mixing.
  Keep English tech terms (App, Settings, Notification, Update, Profile) as-is.

Bengali variants:
- West Bengali (India, bn-IN): More Sanskrit loanwords, Indian date format (DD/MM/YYYY)
- Bangladeshi Bengali (bn-BD): More Arabic-Persian loanwords, different vocabulary for some terms
- Use standard/neutral Bengali that works for both regions.

Font rendering:
- Noto Sans Bengali is included since Android 5.0 — most devices render correctly
- Conjunct characters (যুক্তাক্ষর / juktakkhor) are ligatures combining multiple consonants
- WARNING: Some cheap OEM devices (Huawei, Oppo budget models) may not include Bengali fonts,
  showing empty boxes ("tofu") instead. Consider bundling a fallback font.
```

### 4.19 Telugu (te) — Indien

```
## Language-Specific Rules: Telugu (te)
- Script: Telugu script (తెలుగు) — unique to Telugu, NOT shared with other languages
- Register: Use polite "మీరు" (miiru) form — respectful but warm.
  Do NOT use నువ్వు (nuvvu) in app UI — it is too familiar for adults.
  Users writing journal entries themselves will naturally use నువ్వు, but the APP
  addressing the USER should use మీరు.
- Plurals: one, other
- Text expansion: Telugu is typically 15-30% longer than German

CRITICAL — Agglutination:
- Telugu is highly agglutinative. Words grow by chaining suffixes. A single wrong suffix
  completely changes the meaning. "Save" = "సేవ్ చేయండి" (3x longer in display).
- Avoid compound words exceeding 15 syllables — they will overflow UI buttons.
- Always provide <!-- SHORTER: ... --> alternatives for translations exceeding 40% expansion.

Vocabulary:
- "డైరీ" (diary, borrowed) or "జర్నల్" (journal, borrowed) for "Tagebuch/Journal"
- "ఎంట్రీ" (entry, borrowed) or "నమోదు" (namodu, native) for "Eintrag/Entry"
- "మూడ్" (mood, borrowed) or "మనోభావం" (manobhavam, native) for "Stimmung/Mood"
- "రిమైండర్" (reminder, borrowed) for "Erinnerung/Reminder"
- Numbers: Use standard Arabic numerals (0-9), NOT Telugu numerals (౦౧౨౩౪౫౬౭౮౯)

LLM-specific note:
- Telugu shows the BEST improvement from few-shot prompting among all Indian languages.
  If quality is insufficient, try adding 2-3 example translations in the prompt.
- Gemini performs slightly better than GPT-4o for Telugu (0.726 vs 0.704 semantic similarity).
- English loanwords in Telugu script are natural and preferred for tech UI terms
  (Settings, Save, App, Notification) — the Hyderabad IT audience expects them.

Font rendering:
- Noto Sans Telugu is included since Android 5.1
- Telugu has wide characters with vowel extensions above and below the baseline.
  Set lineSpacingMultiplier to at least 1.2 in TextViews to prevent clipping.
- Andhra Pradesh and Telangana have minor vocabulary differences — use standard Telugu.
```

### 4.20 Marathi (mr) — Indien

```
## Language-Specific Rules: Marathi (mr)
- Script: Devanagari (देवनागरी) — same script as Hindi, but DIFFERENT language

CRITICAL — Hindi-Verwechslung (haeufigster LLM-Fehler!):
- DO NOT confuse Marathi with Hindi. They share the Devanagari script but have different
  vocabulary, grammar, and expressions. A Hindi translation is NOT acceptable for Marathi.
- Marathi speakers are VERY sensitive to Hindi-leakage ("Ghaati"-Komplex).
  A native Marathi speaker will IMMEDIATELY notice Hindi words in Marathi text.
- Key grammatical difference: Marathi has THREE genders (masculine, feminine, NEUTER)
  unlike Hindi's two. LLMs frequently make gender agreement errors.
- The letter ळ (ḷa) is common in Marathi but ABSENT in Hindi. Use it correctly.
- Marathi prefers Sanskrit and Portuguese loanwords (Goa influence),
  Hindi prefers Arabic-Persian loanwords (Mughal influence). Do NOT mix these.

Register: Use "तुम्ही" (tumhi) form — warm and respectful.
  This is the right level for a personal journal app: not too formal (आपण),
  not too casual (तू). For notification/reminder texts, तुम्ही is also appropriate.
- Plurals: one, other
- Text expansion: Marathi is typically 20-35% longer than German

Vocabulary:
- "दैनंदिनी" (dainandini) or "जर्नल" (journal, borrowed) for "Tagebuch/Journal"
- "नोंद" (nond) for "Eintrag/Entry"
- "मनस्थिती" (manasthiti) for "Stimmung/Mood"
- "स्मरणपत्र" (smaranpatra) or "रिमाइंडर" (reminder, borrowed) for "Erinnerung/Reminder"
- Numbers: Use standard Arabic numerals (0-9), NOT Devanagari numerals
- Technical terms (PDF, Export, Premium) stay in English — commonly understood
- Marathi speakers are predominantly in Maharashtra (Mumbai, Pune) —
  urban audience is very comfortable with English tech terms

Font rendering:
- Noto Sans Devanagari covers Marathi fully. No separate font needed.
- Marathi-specific OpenType features are less demanding than Dravidian scripts.
```

### 4.21 Tamil (ta) — Indien & Sri Lanka

```
## Language-Specific Rules: Tamil (ta)
- Script: Tamil script (தமிழ்) — one of the oldest scripts still in use (2000+ years)
- Register: Use polite "நீங்கள்" (neengal) form — Tamil culture values politeness
  even in informal contexts. Do NOT use ultra-casual "நீ" (nee) for a journal app.
- Plurals: one, other
- Text expansion: Tamil is typically 25-40% longer than German — this is SIGNIFICANT.
  Always provide <!-- SHORTER: ... --> alternatives for labels exceeding 30% expansion.

CRITICAL — Cultural Identity & Language Purism:
- Tamil speakers have the STRONGEST language purism of all Indian languages.
  Tamil is one of the world's oldest literary languages and speakers are very proud of it.
- BAD Tamil translations will get ACTIVELY negative reviews in the Play Store.
- DO NOT use Sanskrit loanwords. Tamil does NOT borrow from Hindi or Sanskrit for everyday words.
  Use Dravidian roots or classical Tamil vocabulary.
- For personal/emotional content: ALWAYS use native Tamil words.
  "நாட்குறிப்பு" (naatkurippu) for Journal, "மனநிலை" (mananilai) for Mood.
- For UI tech elements: English loanwords ARE acceptable for Settings, Save, Export, App.
  This is the mixed approach that works best — native for heart, English for tech.

CRITICAL LLM WARNING — Code-Mixing (Tanglish):
- LLMs frequently produce "Tanglish" (Tamil + English mixed) even when pure Tamil is requested.
  While Tanglish is linguistically authentic for urban Chennai speakers, it is BAD
  localization practice. Check each string for unnecessary English mixing.
- Tamil has only 18 consonants (versus 35+ in Hindi/Bengali). LLMs sometimes confuse
  consonants that look visually similar in Tamil script.

Vocabulary:
- "நாட்குறிப்பு" (naatkurippu) for "Tagebuch/Journal"
- "உள்ளீடு" (ulleedu) or "பதிவு" (pathivu) for "Eintrag/Entry"
- "மனநிலை" (mananilai) for "Stimmung/Mood"
- "நினைவூட்டல்" (ninaivoottal) for "Erinnerung/Reminder"
- Numbers: Use standard Arabic numerals (0-9), NOT Tamil numerals (௦௧௨௩௪௫௬௭௮௯)

Font rendering:
- Noto Sans Tamil is included since Android 5.0
- Tamil has 247 characters (12 vowels x 18 consonants + specials) — orthogonally complex
  but less rendering issues than Malayalam or Kannada
- Tamil Nadu and Sri Lankan Tamil have minor differences — use standard Tamil
```

### 4.22 Urdu (ur) — Pakistan & Indien

```
## Language-Specific Rules: Urdu (ur)
- Script: Arabic script (اردو) — this is a RIGHT-TO-LEFT (RTL) language
- Register: Use "آپ" (aap) form — Urdu culture strongly favors politeness and respect.
  آپ is the standard for all apps addressing users.
- Plurals: one, other
- Text expansion: Urdu is typically 20-30% longer than German

CRITICAL — Urdu vs Hindi:
- Urdu is grammatically almost identical to Hindi (~80% shared vocabulary), but uses
  Arabic script and prefers Arabic-Persian loanwords where Hindi uses Sanskrit.
- Do NOT just copy the Hindi translation and change the script.
- Use Arabic-Persian vocabulary: "kitaab" (كتاب) NOT "pustak", "waqt" (وقت) NOT "samay".
- LLMs trained on large amounts of Hindi data frequently insert Hindi/Sanskrit vocabulary
  into Urdu output. A native Urdu speaker will immediately notice this.

Nastaliq vs Naskh script — The critical decision:
- Nastaliq (traditional Urdu calligraphy, hanging diagonal): Culturally preferred by Urdu speakers.
  But has known CLIPPING PROBLEMS on Android when scaled below 80%.
- Naskh (linear, like Arabic): Works out-of-the-box on Android. Technically simpler.
  Urdu speakers accept it for digital UI but find it "less authentic".
- RECOMMENDATION: Use Naskh (Noto Sans Arabic) for UI. This is the pragmatic choice.
- Noto Nastaliq Urdu is available but requires lineSpacingMultiplier of ~2.0 due to
  the hanging character style.

Vocabulary:
- "ڈائری" (diary, borrowed) or "جریدہ" (jarida, native) for "Tagebuch/Journal"
- "اندراج" (indiraaj) for "Eintrag/Entry"
- "موڈ" (mood, borrowed) or "مزاج" (mizaaj, native) for "Stimmung/Mood"
- "یاد دہانی" (yaad-dehaani) for "Erinnerung/Reminder"

RTL RULES (same as Arabic):
- Numbers, English brand names, and technical terms must remain left-to-right (LTR)
- Use Unicode BiDi control characters where needed for mixed-direction text
- App needs android:supportsRtl="true" in AndroidManifest
- Use start/end instead of left/right in ALL layouts
- Icons showing direction (back arrow, next) must be mirrored (use ldrtl qualifier)
- Urdu is spoken in both Pakistan (ur-PK) and India (ur-IN) — use neutral vocabulary
- Technical terms (PDF, Export, Premium) are commonly kept in English
```

### 4.23 Gujarati (gu) — Indien

```
## Language-Specific Rules: Gujarati (gu)
- Script: Gujarati script (ગુજરાતી) — derived from Devanagari but WITHOUT the horizontal
  top line (shirorekha). This makes it visually softer than Hindi/Marathi.
- Register: Use polite-informal "તમે" (tame) form — warm but respectful.
  This is the correct level for a personal journal app.
- Plurals: one, other
- Text expansion: Gujarati is typically 15-25% longer than German

LLM WARNING — Script Fallback:
- LLMs sometimes fall back to Devanagari Unicode characters when Gujarati glyphs are missing
  in their training data. The scripts look similar but ARE different.
  If a character has the horizontal top line (shirorekha), it is Devanagari, NOT Gujarati.
- GPT-4o scores only 41.77% on IndicMMLU-Pro for Gujarati — one of the weaker Indian languages
  for LLMs. Extra review is recommended.

Vocabulary:
- "ડાયરી" (diary, borrowed) or "જર્નલ" (journal, borrowed) for "Tagebuch/Journal"
- "એન્ટ્રી" (entry, borrowed) or "નોંધ" (nondh, native) for "Eintrag/Entry"
- "મૂડ" (mood, borrowed) or "મનોસ્થિતિ" (manosthiti, native) for "Stimmung/Mood"
- "રિમાઇન્ડર" (reminder, borrowed) for "Erinnerung/Reminder"
- Numbers: Use standard Arabic numerals (0-9), NOT Gujarati numerals (૦૧૨૩૪૫૬૭૮૯)

Audience context:
- Gujarati-speaking community is strongly business-oriented (trade, commerce).
  English tech terms and anglicisms are very naturally accepted.
- "Business", "Payment", "Settings", "Save" = keep in English.
- Technical terms stay in English: PDF, Export, Premium, Streak

Font rendering:
- Noto Sans Gujarati was originally only in Nexus devices. Broadly available since Android 6.0.
- WARNING: Some OEM devices (Motorola, Oppo budget models) may NOT include Gujarati fonts
  in their AOSP build. Consider bundling a fallback font for maximum compatibility.
```

### 4.24 Kannada (kn) — Indien

```
## Language-Specific Rules: Kannada (kn)
- Script: Kannada script (ಕನ್ನಡ) — round, distinctive script used in Karnataka
- Register: Use polite "ನೀವು" (neevu) form — respectful but not stiff.
  Do NOT use casual "ನೀನು" (neenu) in app UI.
- Plurals: one, other
- Text expansion: Kannada is typically 20-35% longer than German

LLM WARNING — Script Confusion with Telugu:
- Kannada and Telugu scripts have visual similarities (both use many circular shapes).
  LLMs occasionally mix Telugu characters into Kannada text. Verify that output uses
  ONLY Kannada Unicode range (U+0C80-U+0CFF), not Telugu (U+0C00-U+0C7F).
- GPT-4o scores 38.97% on IndicMMLU-Pro for Kannada — the weakest of all 4 South Indian
  languages. Native speaker review is strongly recommended.

Vocabulary:
- "ಡೈರಿ" (diary, borrowed) or "ಜರ್ನಲ್" (journal, borrowed) for "Tagebuch/Journal"
- "ಎಂಟ್ರಿ" (entry, borrowed) or "ನಮೂದು" (namoodu, native) for "Eintrag/Entry"
- "ಮೂಡ್" (mood, borrowed) or "ಮನಸ್ಥಿತಿ" (manasthiti, native) for "Stimmung/Mood"
- "ರಿಮೈಂಡರ್" (reminder, borrowed) for "Erinnerung/Reminder"
- Numbers: Use standard Arabic numerals (0-9), NOT Kannada numerals (೦೧೨೩೪೫೬೭೮೯)

Audience context:
- Karnataka and especially Bengaluru (Bangalore) is India's tech hub.
  The audience is VERY English-comfortable. English loanwords in Kannada script
  are perfectly natural for app UI.
- Older users outside Bengaluru prefer more native Kannada vocabulary.
- Use mixed approach: English for tech (Settings, Save, App), native for personal content.
- Technical terms stay in English: PDF, Export, Premium, Streak

Font rendering:
- Noto Sans Kannada is included since Android 5.0
- Conjunct-ligature rendering is correct on Android 4.3+ with HarfBuzz-ng
- WARNING: Some Android Go (budget) devices may render complex Kannada ligatures incorrectly.
  Test on actual budget devices if targeting the Indian mass market.
```

### 4.25 Malayalam (ml) — Indien

```
## Language-Specific Rules: Malayalam (ml)
- Script: Malayalam script (മലയാളം) — highly curved, distinctive script used in Kerala.
  THE most complex conjunct character system of ALL Indian scripts.
- Register: Use polite "നിങ്ങൾ" (ningal) form — Malayalam culture values respect.
  Do NOT use casual "നീ" (nee) in app UI.
- Plurals: one, other
- Text expansion: Malayalam is typically 25-40% longer than German — THE MOST EXTREME
  expansion of all Indian languages. Single grammatically correct words can represent
  an entire sentence due to agglutinative suffix chaining.

CRITICAL — Orthography Decision:
- Malayalam has TWO orthographies:
  - Simplified (1971 reform): Fewer ligatures, easier to render. Android system font
    (Noto Sans Malayalam) uses this. Compatible with 99% of devices. RECOMMENDED.
  - Traditional (Thaali/Old): More ligatures, culturally valued by older users and literati.
    Requires special fonts, may not render on all devices.
- For a journal app: USE SIMPLIFIED ORTHOGRAPHY. It works on all devices.

CRITICAL LLM WARNING — The Hardest Indian Language for LLMs:
- Malayalam has the LEAST training data among major Indian languages combined with
  the MOST complex morphology. LLM output quality is notably lower than Bengali/Hindi.
- LLMs produce Malayalam text that is Unicode-correct but often ORTHOGRAPHICALLY wrong
  (wrong ligature choices, incorrect suffix combinations).
- Morphology errors: Malayalam has complex agglutinative suffix chains. LLMs frequently
  cut them incorrectly, producing ungrammatical compounds.
- NATIVE SPEAKER REVIEW IS STRONGLY RECOMMENDED for Malayalam. Do not ship without review.

Vocabulary:
- "ഡയറി" (diary, borrowed) or "ജേണൽ" (journal, borrowed) for "Tagebuch/Journal"
- "എൻട്രി" (entry, borrowed) or "രേഖ" (rekha, native) for "Eintrag/Entry"
- "മൂഡ്" (mood, borrowed) or "മാനസികാവസ്ഥ" (maanasikavastha, native) for "Stimmung/Mood"
- "ഓർമ്മപ്പെടുത്തൽ" (ormmappeduttal) for "Erinnerung/Reminder"
- Numbers: Use standard Arabic numerals (0-9), NOT Malayalam numerals (൦൧൨൩൪൫൬൭൮൯)

Audience context:
- Kerala has 96% literacy rate — the HIGHEST in India. Audience expects
  grammatically PERFECT text. Poor translations will be noticed and criticized.
- English loanwords for tech terms are completely normal (Kerala IT sector is large).
- Technical terms (PDF, Export, Premium, Settings) stay in English — widely understood.
- Malayalam does NOT borrow from Hindi/Sanskrit for everyday words — use Dravidian roots.

Font rendering:
- Google acknowledged: "All non-browser apps have ligature rendering issues for Malayalam"
  This was fixed via HarfBuzz-ng with Swathanthra Malayalam Computing collaboration.
- Android 4.3+ renders correctly, but ligature extent depends on the font.
- Noto Sans Malayalam renders simplified orthography correctly.
- ALWAYS set lineSpacingMultiplier to at least 1.3 — Malayalam glyphs extend above and below.
- Button layouts MUST use wrap_content. Fixed widths are UNUSABLE with Malayalam text.
```

---

### Zusammenfassung: Indische Sprachen auf einen Blick

| Sprache | Locale | Schrift | Sprecher | RTL? | Plurale | Besonderheit |
|---------|--------|---------|----------|------|---------|-------------|
| **Hindi** | hi | Devanagari | 600 Mio. | Nein | one, other | Groesste indische Sprache, aber nur ~40% Indiens |
| **Bengali** | bn | Bengalisch | 270 Mio. | Nein | one, other | Indien + Bangladesch, komplexe Ligaturen |
| **Telugu** | te | Telugu | 96 Mio. | Nein | one, other | Agglutination (lange Woerter), English-Borrowings beliebt |
| **Marathi** | mr | Devanagari | 95 Mio. | Nein | one, other | Gleiche Schrift wie Hindi, ANDERE Sprache! |
| **Tamil** | ta | Tamil | 85 Mio. | Nein | one, other | Bevorzugt reine Tamil-Woerter, starke kulturelle Identitaet |
| **Urdu** | ur | Arabisch (Nastaliq) | 70 Mio. | **JA (RTL!)** | one, other | Grammatisch ~Hindi, aber arabische Schrift, braucht RTL-Support |
| **Gujarati** | gu | Gujarati | 60 Mio. | Nein | one, other | Aehnlich Devanagari ohne Oberlinie, English-Borrowings natuerlich |
| **Kannada** | kn | Kannada | 50 Mio. | Nein | one, other | Bangalore = Tech-Hub, sehr English-komfortabel |
| **Malayalam** | ml | Malayalam | 38 Mio. | Nein | one, other | Komplexeste Ligaturen aller indischen Schriften, lange Woerter |

**Wichtig fuer den indischen Markt:**
- Hindi + Bengali + Telugu + Marathi + Tamil decken ~80% der indischen Bevoelkerung ab
- Urdu erfordert RTL-Support (wie Arabisch) — als separates Projekt mit Arabisch zusammen planen
- Fast alle indischen Sprachen verwenden arabische Ziffern (0-9) im digitalen Kontext
- Technische Begriffe (PDF, Premium, Export, Streak) koennen in ALLEN indischen Sprachen
  auf Englisch bleiben — sie sind im App-Kontext allgemein verstanden
- Jede indische Sprache hat eine eigene Schrift — "Hindi uebersetzen und Schrift aendern"
  funktioniert NICHT (ausser bei Urdu, das grammatisch aehnlich ist)

### LLM-Qualitaet fuer indische Sprachen (Recherche-Ergebnis)

LLMs sind bei indischen Sprachen **30-50% schlechter** als bei europaeischen Sprachen.
Die drei haeufigsten Fehlerklassen:

1. **Code-Mixing**: LLM mischt Hindi/Englisch in die Zielsprache (besonders bei Bengali, Marathi)
2. **Halluzinationen bei figurativer Sprache**: Redewendungen werden woertlich uebersetzt
3. **Falsche Wortbedeutung bei Polysemie**: Mehrdeutige Woerter erhalten die falsche Bedeutung

| Sprache | LLM-Qualitaet (GPT-4o) | Haeufigster Fehler | Review-Prioritaet |
|---------|------------------------|--------------------|--------------------|
| **Hindi** | Gut (~50% IndicMMLU) | Englisch-Mixing | Mittel |
| **Bengali** | Gut | Hindi-Leakage (Devanagari-Einfluss) | Mittel |
| **Telugu** | Gut (beste Gemini-Performance) | Agglutinations-Fehler | Mittel |
| **Marathi** | Mittel | Hindi-Verwechslung (gleiche Schrift!) | **Hoch** |
| **Tamil** | Mittel | Tanglish Code-Mixing, Sanskrit-Einstreuung | **Hoch** |
| **Urdu** | Mittel | Hindi/Sanskrit-Vokabeln statt Persisch/Arabisch | **Hoch** |
| **Gujarati** | Schwach (~42% IndicMMLU) | Devanagari-Fallback statt Gujarati-Schrift | **Sehr hoch** |
| **Kannada** | Schwach (~39% IndicMMLU) | Telugu-Zeichen-Verwechslung | **Sehr hoch** |
| **Malayalam** | Sehr schwach | Ligatur-Fehler, Morphologie-Fehler | **Kritisch** |

**Empfehlung:** Fuer Gujarati, Kannada und Malayalam UNBEDINGT Muttersprachler-Review
einplanen. Die LLM-Qualitaet allein reicht hier nicht fuer einen professionellen Launch.

---

## 5. Haeufige Fehler und Fallstricke

### 5.1 Positionelle Platzhalter (KRITISCH)

```xml
<!-- FALSCH — Reihenfolge kann in anderen Sprachen anders sein -->
<string name="welcome">Hallo %s, du hast %d Nachrichten</string>

<!-- RICHTIG — Positionsangabe, Uebersetzer kann Reihenfolge aendern -->
<string name="welcome">Hallo %1$s, du hast %2$d Nachrichten</string>
```

**Warum:** Im Deutschen steht der Name vielleicht vor der Zahl, im Japanischen
koennte es anders sein. Mit `%1$s` und `%2$d` kann der Uebersetzer die Reihenfolge
aendern ohne die Zuordnung zu verlieren.

**VOR der Uebersetzung pruefen:** Alle `%s` und `%d` in der Quell-strings.xml
muessen positionelle Specifier haben (`%1$s`, `%2$d` etc.) wenn der String
mehr als einen Platzhalter enthaelt.

### 5.2 Pluralformen (HAEUFIGSTE CRASH-QUELLE)

```xml
<!-- FALSCH — crasht bei Arabisch, Russisch, Polnisch -->
<plurals name="entries_count">
    <item quantity="other">%d Eintraege</item>
</plurals>

<!-- RICHTIG fuer Arabisch — ALLE 6 Formen -->
<plurals name="entries_count">
    <item quantity="zero">لا توجد إدخالات</item>
    <item quantity="one">إدخال واحد</item>
    <item quantity="two">إدخالان</item>
    <item quantity="few">%d إدخالات</item>
    <item quantity="many">%d إدخالاً</item>
    <item quantity="other">%d إدخال</item>
</plurals>
```

### Plural-Anforderungen pro Sprache

| Sprache | Benoetigte Quantities |
|---------|----------------------|
| Englisch, Deutsch, Niederlaendisch, Tuerkisch | one, other |
| Hindi, Bengali, Telugu, Marathi, Tamil, Urdu, Gujarati, Kannada, Malayalam | one, other |
| Franzoesisch, Italienisch, Portugiesisch | one, many, other |
| Russisch, Ukrainisch, Polnisch, Tschechisch | one, few, many, other |
| **Arabisch** | **zero, one, two, few, many, other (ALLE 6!)** |
| Japanisch, Chinesisch, Koreanisch, Thai, Indonesisch | other (nur) |

### 5.3 Text-Expansion nach Sprache

| Sprache | Expansion vs. Deutsch | Auswirkung |
|---------|----------------------|------------|
| Franzoesisch | +15-20% | Buttons koennen ueberlaufen |
| Spanisch | +15-25% | Buttons koennen ueberlaufen |
| Portugiesisch | +15-25% | Buttons koennen ueberlaufen |
| Polnisch | +20-30% | Erheblich, Agglutination |
| Hindi | +20-40% | Devanagari braucht mehr Platz |
| Bengali | +20-35% | Komplexe Ligaturen, laengere Woerter |
| Telugu | +15-30% | Agglutination, lange Woerter moeglich |
| Marathi | +20-35% | Aehnlich wie Hindi, Devanagari-Schrift |
| Tamil | +25-40% | Sehr lange Woerter, SHORTER-Alternativen Pflicht |
| Urdu | +20-30% | RTL-Schrift (wie Arabisch), braucht RTL-Support |
| Gujarati | +15-25% | Moderate Expansion |
| Kannada | +20-35% | Komplexe Konjunkte, laengere Woerter |
| Malayalam | +25-40% | Laengste Woerter aller indischen Sprachen, SHORTER-Pflicht |
| Arabisch | +20-30% | Plus RTL-Layout noetig |
| Tuerkisch | +20-50% | Agglutination, lange Woerter |
| Russisch | +15-25% | Kyrillisch etwas breiter |
| Japanisch | -20-40% | Kuerzer durch Ideogramme |
| Chinesisch | -30-50% | Deutlich kuerzer |
| Koreanisch | -10-30% | Etwas kuerzer |

### 5.4 Nicht-uebersetzbare Elemente schuetzen

```xml
<!-- xliff:g schuetzt Teile vor Uebersetzung durch TMS-Tools -->
<string name="share_text">
    Geschrieben mit <xliff:g id="app_name">BestJournal</xliff:g>
</string>

<!-- translatable="false" fuer Strings die nie uebersetzt werden sollen -->
<string name="app_package" translatable="false">com.bestjournal.app</string>
```

### 5.5 RTL-Sprachen (Arabisch, Hebraeisch, Persisch, Urdu)

Nicht nur ein Prompt-Problem — die App braucht auch technische Anpassungen:

- `android:supportsRtl="true"` im AndroidManifest.xml
- `start/end` statt `left/right` in allen Layouts
- Zahlen und englische Markennamen bleiben LTR innerhalb von RTL-Text
- Pseudolocale `ar-XB` im Emulator zum Testen verwenden

### 5.6 Tuerkisches I-Problem

Tuerkisch hat zwei verschiedene i-Buchstaben:
- `İ/i` (mit Punkt) — wie im Englischen
- `I/ı` (ohne Punkt) — nur im Tuerkischen

`"Settings".toLowerCase()` ergibt auf tuerkischen Systemen `"settıngs"` (mit punktlosem i).
Das ist ein bekannter Bug in vielen Apps. Die Uebersetzung muss die richtigen Buchstaben verwenden.

---

## 6. Qualitaetssicherung nach der Uebersetzung

### 6.1 Automatischer Platzhalter-Check (Python-Script)

```python
"""
Prueft ob alle Platzhalter aus der Quell-strings.xml auch in der
uebersetzten Version vorhanden sind.
"""
import re
import xml.etree.ElementTree as ET
import sys

def extract_placeholders(text):
    """Findet alle %s, %d, %1$s, %2$d etc. in einem String."""
    return sorted(re.findall(r'%\d*\$?[sdf]', text or ''))

def check_translation(source_path, translated_path):
    source = ET.parse(source_path)
    translated = ET.parse(translated_path)

    source_strings = {s.get('name'): s.text for s in source.findall('.//string')}
    trans_strings = {s.get('name'): s.text for s in translated.findall('.//string')}

    errors = []
    for name, source_text in source_strings.items():
        if name not in trans_strings:
            continue  # fehlende Strings sind ein anderes Problem
        source_ph = extract_placeholders(source_text)
        trans_ph = extract_placeholders(trans_strings[name])
        if source_ph != trans_ph:
            errors.append(f"FEHLER: '{name}' — Quelle hat {source_ph}, Uebersetzung hat {trans_ph}")

    if errors:
        print(f"\n{len(errors)} Platzhalter-Fehler gefunden in {translated_path}:")
        for e in errors:
            print(f"  {e}")
    else:
        print(f"OK: Alle Platzhalter korrekt in {translated_path}")

    return len(errors)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python check_placeholders.py <source_strings.xml> <translated_strings.xml>")
        sys.exit(1)
    errors = check_translation(sys.argv[1], sys.argv[2])
    sys.exit(1 if errors > 0 else 0)
```

### 6.2 Laengencheck

```python
"""
Findet Uebersetzungen die deutlich laenger als das Original sind.
"""
import xml.etree.ElementTree as ET

def check_length(source_path, translated_path, threshold=1.5):
    source = ET.parse(source_path)
    translated = ET.parse(translated_path)

    source_strings = {s.get('name'): s.text for s in source.findall('.//string') if s.text}
    trans_strings = {s.get('name'): s.text for s in translated.findall('.//string') if s.text}

    warnings = []
    for name in source_strings:
        if name not in trans_strings:
            continue
        src_len = len(source_strings[name])
        trans_len = len(trans_strings[name])
        if src_len > 0 and trans_len / src_len > threshold:
            ratio = trans_len / src_len
            warnings.append(f"WARNUNG: '{name}' — {ratio:.1f}x laenger "
                          f"({src_len} → {trans_len} Zeichen)")

    if warnings:
        print(f"\n{len(warnings)} Laengen-Warnungen (>{threshold:.0%} Expansion):")
        for w in warnings:
            print(f"  {w}")
    else:
        print("OK: Keine problematischen Laengenunterschiede")

if __name__ == '__main__':
    import sys
    check_length(sys.argv[1], sys.argv[2])
```

### 6.3 Pseudolocale-Test in Android

Android bietet zwei eingebaute Test-Sprachen:

| Pseudolocale | Locale-Code | Was es tut | Wofuer |
|-------------|-------------|-----------|--------|
| **Accented English** | `en-XA` | Erweitert Text um ~30% mit Akzenten: `[Šéttîñgš one two]` | Findet Layout-Probleme durch Text-Expansion |
| **Mirrored** | `ar-XB` | Spiegelt das gesamte Layout (RTL-Simulation) | Findet RTL-Probleme ohne echte arabische Uebersetzung |

**Aktivierung:**
1. In `build.gradle`:
   ```groovy
   android {
       buildTypes {
           debug {
               pseudoLocalesEnabled true
           }
       }
   }
   ```
2. Auf dem Geraet/Emulator: Einstellungen → Sprache → "XA" oder "XB" auswaehlen

### 6.4 LLM-Selbst-Review (Zweiter Durchlauf)

Nach der Erst-Uebersetzung einen zweiten Prompt-Durchlauf machen:

```
You are a senior localization QA specialist. Review the following [TARGET LANGUAGE]
translation of an Android app (BestJournal, a personal journaling app).

Check for:
1. ACCURACY: Does each translation correctly convey the meaning of the source?
2. FLUENCY: Does the translation sound natural to a native speaker?
3. TERMINOLOGY: Is the same term translated consistently throughout?
4. PLACEHOLDERS: Are all %1$s, %d etc. preserved exactly?
5. TONE: Is the tone warm and personal (not corporate or robotic)?
6. CULTURAL: Are there any culturally inappropriate translations?

Rate each issue as:
- CRITICAL: Will crash the app or is completely wrong
- MAJOR: Grammatically wrong or unnatural
- MINOR: Could be improved but is acceptable

Output format: For each issue found, write:
Line: [string name]
Severity: [CRITICAL/MAJOR/MINOR]
Issue: [what is wrong]
Suggestion: [how to fix it]

If the translation is perfect, write: "NO ISSUES FOUND"

Source (German):
[SOURCE strings.xml]

Translation ([TARGET LANGUAGE]):
[TRANSLATED strings.xml]
```

---

## 7. Welche Sprachen zuerst?

### Empfohlene Reihenfolge nach Marktpotenzial

| Prioritaet | Sprache | Locale-Code | Sprecher | Warum wichtig |
|------------|---------|-------------|----------|---------------|
| **1** | Englisch | en | 1.5 Mrd. | Basis, groesste Reichweite |
| **2** | Spanisch | es | 580 Mio. | Zweitgroesste Muttersprache weltweit |
| **3** | Portugiesisch (BR) | pt-rBR | 260 Mio. | Riesiger Android-Markt in Brasilien |
| **4** | Franzoesisch | fr | 320 Mio. | Europa + wachsender afrikanischer Markt |
| **5** | Japanisch | ja | 125 Mio. | Hoechste Zahlungsbereitschaft fuer Apps |
| **6** | Koreanisch | ko | 80 Mio. | Sehr hohe Zahlungsbereitschaft |
| **7** | Chinesisch (vereinf.) | zh-rCN | 1.1 Mrd. | Groesster Markt (Play Store eingeschraenkt) |
| **8** | Hindi | hi | 600 Mio. | Groesste indische Sprache, schnellstwachsender Smartphone-Markt |
| **9** | Bengali | bn | 270 Mio. | Zweitgroesste indische Sprache, Indien + Bangladesch |
| **10** | Arabisch | ar | 400 Mio. | 22 Laender, RTL-Support noetig |
| **11** | Tuerkisch | tr | 85 Mio. | Starker Android-Anteil |
| 12 | Telugu | te | 96 Mio. | Drittgroesste indische Sprache, Andhra Pradesh + Telangana |
| 13 | Marathi | mr | 95 Mio. | Maharashtra (Mumbai, Pune), zahlungskraeftiger Markt |
| 14 | Tamil | ta | 85 Mio. | Tamil Nadu + Sri Lanka, starke kulturelle Identitaet |
| 15 | Russisch | ru | 250 Mio. | Grosser Markt, eigene App-Stores |
| 16 | Indonesisch | id | 270 Mio. | Riesiger Android-Markt, einfache Sprache |
| 17 | Urdu | ur | 70 Mio. | Pakistan + Indien, braucht RTL-Support (wie Arabisch) |
| 18 | Gujarati | gu | 60 Mio. | Gujarat, starke Business-Community |
| 19 | Kannada | kn | 50 Mio. | Karnataka (Bangalore = Indiens Tech-Hub) |
| 20 | Malayalam | ml | 38 Mio. | Kerala, hoechste Alphabetisierungsrate Indiens (96%) |
| 21 | Italienisch | it | 67 Mio. | Europa, zahlungskraeftig |
| 22 | Niederlaendisch | nl | 25 Mio. | Europa, zahlungskraeftig |
| 23 | Polnisch | pl | 45 Mio. | Starker Android-Markt in Europa |
| 24 | Thai | th | 70 Mio. | Wachsender suedostasiatischer Markt |
| 25 | Ukrainisch | uk | 45 Mio. | Eigene Sprache, nicht Russisch verwenden |

### Schnellstart-Empfehlung

Fuer den ersten Launch reichen **Prioritaet 1-7** (Englisch, Spanisch, Portugiesisch,
Franzoesisch, Japanisch, Koreanisch, Chinesisch). Das deckt die zahlungskraeftigsten Maerkte ab.

### Indien-Strategie (Prioritaet 8-9, 12-14, 17-20)

Indien hat 1,4 Milliarden Menschen aber KEINE einheitliche Sprache. Die optimale Reihenfolge:

| Phase | Sprachen | Abdeckung Indien |
|-------|----------|-----------------|
| **Phase 1** | Hindi + Bengali | ~55% der indischen Bevoelkerung |
| **Phase 2** | + Telugu + Marathi + Tamil | ~80% der indischen Bevoelkerung |
| **Phase 3** | + Urdu + Gujarati + Kannada + Malayalam | ~95% der indischen Bevoelkerung |

**Wichtig:** Urdu (Phase 3) erfordert RTL-Support — zusammen mit Arabisch planen.

### RTL-Sprachen (separates Projekt)

Arabisch (Prioritaet 10) und Urdu (Prioritaet 17) erfordern technische App-Aenderungen
(RTL-Support: Layout-Spiegelung, BiDi-Text), daher als separates Projekt planen.

---

## 8. Tools und Plattformen

### Kostenlose Optionen

| Tool | Was es tut | Fuer wen |
|------|-----------|---------|
| **Google Play Console Gemini Translation** | Automatische Uebersetzung bei jedem App-Bundle-Upload | Schnellster Weg, wenig Kontrolle |
| **Crowdin (Free Tier)** | TMS mit GitHub-Integration, AI-Voruebersetzung | Open-Source-Projekte, Indie-Entwickler |
| **lingo.dev** (Open Source) | CLI-Tool, eigenes LLM mitbringen, strings.xml nativ | Entwickler die Kontrolle wollen |

### Bezahlte Optionen

| Tool | Preis-Modell | Staerke |
|------|-------------|---------|
| **Crowdin** | Per-Wort oder Seat-based | GitHub-Action, Android-Studio-Plugin, 700+ Integrationen |
| **Lokalise** | Seat-based | AI-Orchestrator (routet zum besten LLM pro Content-Typ), RAG |
| **Phrase** | Enterprise | Reifeste Plattform, XLIFF 2.2 Support |
| **POEditor** | Per-Sprache | Guenstig, einfach, gut fuer Indie-Entwickler |

### Android Studio Plugins

| Plugin | LLM-Support | Kosten |
|--------|------------|--------|
| **android-AILocalizationPlugin** | ChatGPT, Gemini, DeepL, Google Translate, AWS | Kostenlos (Open Source) |
| **Crowdin Plugin** | Multi-LLM ueber Crowdin | Crowdin-Account noetig |

### LLM-Empfehlung fuer Uebersetzungen

| LLM | Staerke | Schwaeche |
|-----|---------|-----------|
| **Claude (Opus/Sonnet)** | Bester Ton und Nuancen, folgt komplexen Regeln zuverlaessig | Teurer |
| **GPT-4o** | Schnell, gute Platzhalter-Treue, grosse Sprachvielfalt | Manchmal zu woertlich |
| **DeepL** | Hochvolumen, guenstig, natuerliche europaeische Sprachen | Weniger Kontrolle ueber Ton |
| **Gemini (Play Console)** | Kostenlos, direkt integriert | Keine Prompt-Anpassung moeglich |

---

## 9. Technische Voraussetzungen in der App

### Checkliste VOR der Uebersetzung

- [ ] **Alle Strings in strings.xml** — kein Hardcoding im Kotlin-Code
- [ ] **Positionelle Platzhalter** — `%1$s` statt `%s` bei mehreren Parametern
- [ ] **`xliff:g` Tags** fuer nicht-uebersetzbare Teile (URLs, Markennamen)
- [ ] **`translatable="false"`** fuer technische Strings (Package-Namen, Keys)
- [ ] **Kommentare** bei mehrdeutigen Strings (Kontext fuer den Uebersetzer)
- [ ] **Flexible Layouts** — `wrap_content` statt feste Breiten fuer Textelemente
- [ ] **`start/end` statt `left/right`** in Layouts (fuer RTL-Support)
- [ ] **`android:supportsRtl="true"`** im AndroidManifest (fuer Arabisch/Hebraeisch)

### Checkliste NACH der Uebersetzung

- [ ] **Platzhalter-Check** (Script aus Abschnitt 6.1)
- [ ] **Laengencheck** (Script aus Abschnitt 6.2)
- [ ] **XML-Validierung** — jede uebersetzte strings.xml muss gueltig sein
- [ ] **Build erfolgreich** — App kompiliert ohne Fehler mit allen Sprachen
- [ ] **Pseudolocale-Test** — `en-XA` und `ar-XB` im Emulator
- [ ] **Visueller Check** — wichtigste Screens in jeder Sprache einmal durchklicken
- [ ] **Lint-Check** — `./gradlew lint` findet fehlende Uebersetzungen

### Kontext-Kommentare in strings.xml (Best Practice)

```xml
<!-- Max 20 chars, shown on main screen button -->
<string name="btn_new_entry">Neuer Eintrag</string>

<!-- %1$s = user name, %2$d = number of entries -->
<string name="welcome_message">Hallo %1$s, du hast %2$d Eintraege</string>

<!-- Dialog title, max 30 chars -->
<string name="delete_confirm_title">Eintrag loeschen?</string>
```

Diese Kommentare helfen sowohl dem LLM als auch menschlichen Uebersetzern,
den Kontext zu verstehen und passende Uebersetzungen zu liefern.

---

## 10. Beruechtigte Uebersetzungsfehler grosser Unternehmen

Diese Beispiele zeigen, warum Qualitaetssicherung UNVERZICHTBAR ist:

| Unternehmen | Fehler | Folge |
|-------------|--------|-------|
| **HSBC** | "Assume Nothing" → "Do Nothing" (weltweit) | 10 Mio. $ Rebranding-Kampagne |
| **Mercedes-Benz** (China) | Erster Markenname 奔死 = "Renn in den Tod" | Komplettes Rebranding zu 奔驰 ("Galoppierender Stern") |
| **Amazon** (Schweden 2020) | Rohe Maschinenuebersetzung ohne Review | Obszoene Produktbeschreibungen, falsche Nationalflaggen |
| **Facebook** | Arabischer Gruss auto-uebersetzt als "Greift sie an!" auf Hebraeisch | Falsche Verhaftung eines Palaestinensers in Israel |
| **Honda** | Modellname "Fitta" = vulgaeres Wort in Nordeuropa | Umbenennung zu "Jazz" in Skandinavien |
| **IKEA** | Produkt "Fartfull" (Schwedisch fuer "schnell") | Internationale Spottnachrichten |
| **Vicks** (Deutschland) | "V" klingt im Deutschen wie "F" — ergab obszoenen Klang | Umbenennung zu "Wick" fuer den deutschen Markt |
| **Medizintechnik** (DE, 2006) | "non-modular cemented" → "non-cemented" | 47 Patienten brauchten Re-Operationen |

**Wichtigste Lektion:** Maschinenuebersetzung ohne menschliche Pruefung kann katastrophal
schiefgehen — besonders bei Namen, kulturellen Referenzen und mehrdeutigen Begriffen.
Fuer eine Journal-App ist das Risiko geringer als bei Medizintechnik, aber schlechte
Uebersetzungen fuehren direkt zu 1-Stern-Bewertungen im Play Store.

---

## Quellen

### Offizielle Android-Dokumentation
- [Localize your app — Android Developers](https://developer.android.com/guide/topics/resources/localization)
- [Support different languages — Android Developers](https://developer.android.com/training/basics/supporting-devices/languages)
- [Translation services — Google Play Console](https://support.google.com/googleplay/android-developer/answer/9844778)

### Best Practices und Guides
- [The Ultimate Guide to Android Localization — Phrase](https://phrase.com/blog/posts/best-practices-for-android-localization-revisited-and-expanded/)
- [Mobile App Localization Guide — Crowdin](https://crowdin.com/blog/mobile-app-localization-guide)
- [Android String Resources Complete Guide — SimpleLocalize](https://simplelocalize.io/blog/posts/android-strings-localization/)

### LLM-Uebersetzung und Prompts
- [AI Prompt Engineering for Localization 2024 — Custom.MT](https://custom.mt/ai-prompt-engineering-for-localization-2024-techniques/)
- [AI Prompts for Quality Translation — Crowdin Blog](https://crowdin.com/blog/ai-prompts-for-quality-translation)
- [10 Translation Prompts for ChatGPT and Claude — ChatsControl](https://chatscontrol.com/blog/prompt-engineering-translation-chatgpt-claude-prompts)
- [35 ChatGPT Prompts for High-Quality Translation — Pairaphrase](https://www.pairaphrase.com/blog/chatgpt-prompts-translation)

### Industrie-Trends und Fallstudien
- [Localization Case Studies: How Top Brands Get Global Right — Aspect Journal](https://aspectusjournal.com/2025/04/18/localization-case-studies-how-top-brands-get-global-right/)
- [A Behind-the-Scenes Look at TikTok Localization Strategy — Alconost](https://alconost.com/en/blog/tiktok)
- [Spotify's Model: Localizing for User Belonging — Weglot](https://www.weglot.com/blog/spotify-localization)
- [Best AI Translation Tools for Enterprise Localization 2026 — XTM](https://xtm.ai/blog/ai-translation-tools)
- [13 Translation Industry Trends: 2026 Outlook — Pairaphrase](https://www.pairaphrase.com/blog/translation-industry-trends)

### Tools
- [lingo.dev — Open-Source AI i18n Toolkit](https://github.com/lingodotdev/lingo.dev)
- [android-AILocalizationPlugin — GitHub](https://github.com/zigzagyc/android-AILocalizationPlugin)
- [Crowdin GitHub Action](https://github.com/crowdin/github-action)
