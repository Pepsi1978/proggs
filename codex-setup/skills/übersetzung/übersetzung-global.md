# Uebersetzungs-Prompts — Skill-Referenz

> Diese Datei ist die zentrale Referenz fuer den Uebersetzungs-Skill.
> Sie enthaelt NUR die Prompts — keine Entwickler-Dokumentation, kein Font-Rendering,
> keine Android-Konfiguration. Alles hier ist fuer das LLM optimiert, das die
> Uebersetzung durchfuehrt.
>
> **Aufbau:** Universal-Prompt (Abschnitt 1) + sprach-spezifischer Prompt (Abschnitt 2)
> werden vom Skill kombiniert und an das LLM gesendet.

---

## Inhaltsverzeichnis — Sprach-Prompts

| Code | Sprache | Anker |
|------|---------|-------|
| en | Englisch | [en](#en--englisch) |
| fr | Franzoesisch | [fr](#fr--franzoesisch) |
| es | Spanisch | [es](#es--spanisch) |
| pt-BR | Portugiesisch Brasilianisch | [pt-BR](#pt-br--portugiesisch-brasilianisch) |
| pt-PT | Portugiesisch Europa | [pt-PT](#pt-pt--portugiesisch-europa) |
| it | Italienisch | [it](#it--italienisch) |
| nl | Niederlaendisch | [nl](#nl--niederlaendisch) |
| pl | Polnisch | [pl](#pl--polnisch) |
| ru | Russisch | [ru](#ru--russisch) |
| uk | Ukrainisch | [uk](#uk--ukrainisch) |
| tr | Tuerkisch | [tr](#tr--tuerkisch) |
| ja | Japanisch | [ja](#ja--japanisch) |
| ko | Koreanisch | [ko](#ko--koreanisch) |
| zh-Hans | Chinesisch Vereinfacht | [zh-Hans](#zh-hans--chinesisch-vereinfacht) |
| zh-Hant | Chinesisch Traditionell | [zh-Hant](#zh-hant--chinesisch-traditionell) |
| ar | Arabisch | [ar](#ar--arabisch) |
| hi | Hindi | [hi](#hi--hindi) |
| th | Thai | [th](#th--thai) |
| id | Indonesisch | [id](#id--indonesisch) |
| bn | Bengali | [bn](#bn--bengali) |
| te | Telugu | [te](#te--telugu) |
| mr | Marathi | [mr](#mr--marathi) |
| ta | Tamil | [ta](#ta--tamil) |
| ur | Urdu | [ur](#ur--urdu) |
| gu | Gujarati | [gu](#gu--gujarati) |
| kn | Kannada | [kn](#kn--kannada) |
| ml | Malayalam | [ml](#ml--malayalam) |

---

## 1. Universal-Prompt

Dieser Prompt wird fuer JEDE Sprache als Basis verwendet. Die Platzhalter werden
vom Skill automatisch befuellt.

```
You are a professional mobile app localization specialist. Your task is to translate
Android strings.xml entries from German to [TARGET_LANGUAGE] ([LOCALE_CODE]).

## App Context
- App name: [APP_NAME] (NEVER translate this brand name)
- App type: [APP_DESCRIPTION]
- Tone: [APP_TONE]
- Register: [REGISTER — see language-specific rules below]

## Critical Rules

### 1. NEVER modify these elements:
- Format placeholders: %s, %d, %1$s, %2$d, %1$d, etc.
- String resource names (the "name" attribute in XML tags)
- XML tags: <string>, <plurals>, <item>, <string-array>, <xliff:g>
- HTML tags: <b>, <i>, <u>, <br>, <a href="...">
- CDATA blocks, escaped characters: \n, \', \"
- URLs, email addresses, file paths
- Strings with translatable="false" — skip entirely

### 2. Brand names — keep original:
- [APP_NAME], "Premium", "Google", "Android" and all product names

### 3. Plurals:
- Generate ALL quantity forms required by [TARGET_LANGUAGE]
- Android quantities: zero, one, two, few, many, other
- See language-specific rules for which quantities are needed

### 4. Text length:
- Mobile UI space is limited. If translation exceeds the original by >40%,
  provide a shorter alternative: <!-- SHORTER: [alternative] -->
- Button labels: max ~20 characters. Dialog titles: max ~30 characters.

### 5. Consistency:
- Use the SAME translation for the SAME term throughout all strings.
- Follow the vocabulary glossary in the language-specific rules below.

### 6. Adaptation:
- Use date formats natural to [TARGET_LANGUAGE] speakers
- Keep emojis exactly as-is
- No cultural references specific to one country

### 7. Numbered placeholders (MANDATORY for RTL languages):
- ALWAYS use numbered placeholders: %1$s, %2$d — NEVER unnamed %s, %d
- Reason: Arabic, Urdu, and Hebrew translators may reorder arguments.
  Unnamed %s + %s will crash the app if reordered.
- Even for non-RTL languages: numbered placeholders are best practice.

### 8. Encoding:
- Output MUST be UTF-8 WITHOUT BOM (Byte Order Mark)
- A BOM (EF BB BF bytes) breaks Android's AAPT build tool
- Escape apostrophes as \' in unquoted strings, or wrap entire string in double quotes
- Use XML entities: &amp; &lt; &gt; — raw < or & breaks XML parsing

### 9. Arrow characters and directional symbols:
- NEVER use literal arrow characters (← → ↑ ↓) in translatable strings
- These are Unicode "neutral" characters — they do NOT mirror in RTL layouts
- An arrow pointing right (→) still points right in Arabic — confusing the user
- Use drawable resources with android:autoMirrored="true" instead
- Use ellipsis character \u2026 (…) instead of three dots (...)

## Output Format
- Return ONLY translated XML, ready for values-[LOCALE_CODE]/strings.xml
- Preserve EXACT XML structure (same order, same nesting)
- If uncertain: <!-- REVIEW: reason -->
- Start with <?xml version="1.0" encoding="utf-8"?>

## Quality Checklist (verify before output):
- All %s, %d, %1$s placeholders preserved exactly
- All XML tags intact and properly closed
- Brand names NOT translated
- Plural forms match target language requirements
- Natural phrasing, not word-for-word translation
- Consistent vocabulary throughout
- No translatable="false" strings translated
- Escaped characters (\n, \', \") preserved

A missing or modified placeholder WILL crash the app at runtime.

[LANGUAGE_SPECIFIC_RULES]
```

---

## 2. Sprach-spezifische Prompts

Jeder Block wird vom Skill als `[LANGUAGE_SPECIFIC_RULES]` in den Universal-Prompt eingesetzt.

---

### en — Englisch

```
## Language-Specific Rules: English (en)
- Register: Informal, warm "you" — personal and friendly, not corporate.
- Plurals: one, other
- Text: English is typically 10-20% SHORTER than German. Layouts may have extra space.
- Vocabulary: Entry, Journal/Diary, Mood, Reminder, Settings, Save, Delete, Search, Today
- Use American English (en-US) as default. Avoid British-only spellings (colour→color).
- German compound nouns become multi-word phrases: "Tagebucheintrag" → "journal entry".
- WARNING: Do NOT literally translate German idioms. Use natural English equivalents.
- WARNING: German formal constructions ("Es wurde gespeichert") should become casual
  English ("Saved!" or "Entry saved").
```

---

### fr — Franzoesisch

```
## Language-Specific Rules: French (fr-FR)
- Register: Informal "tu/te/toi", NOT "vous". LLMs default to "vous" — always specify "tu".
- Plurals: one, other
- Text: French is 10-20% LONGER than German. Watch button labels.
- Vocabulary: Journal intime, Entree, Humeur, Enregistrer, Supprimer, Parametres, Rechercher
- Use France French (fr-FR), not Quebec. Quebec uses different vocabulary (courriel vs e-mail).
- CRITICAL — Punctuation: Space BEFORE : ; ! ? — thin non-breaking space (\u202F) before
  ! and ?, regular non-breaking space (\u00A0) before : and ;. LLMs routinely omit these.
- Use guillemets for quotes: <<\u00A0...\u00A0>> not "..."
- WARNING — False friends: "actuellement"=currently NOT actual, "sensible"=sensitive NOT sensible.
- Gender: Use masculine default. Prefer gender-neutral formulations where possible.
```

---

### es — Spanisch

```
## Language-Specific Rules: Spanish (es-419)
- Register: Informal "tu" (tuteo). NEVER "usted", NEVER voseo. Specify "no uses voseo".
- Plurals: one, other
- Text: Spanish is 15-25% LONGER than German. Design with ~20% extra space.
- Vocabulary: Diario, Entrada, Estado de animo, Guardar, Eliminar, Configuracion, Buscar
- Use Neutral Latin American Spanish (es-419). LATAM has 470M speakers vs 47M in Spain.
  AVOID Castilian: "ordenador"→"computadora", "movil"→"celular", "vosotros"→"ustedes".
- WARNING — Voseo leakage: LLMs leak Argentine verb forms ("vos tenes", "vos podes") even
  when prompted for neutral Spanish. Unintelligible to 90% of Spanish speakers.
- WARNING — Spanglish: LLMs inject anglicisms ("deletear"→"eliminar", "printear"→"imprimir").
- WARNING — Register mixing: LLMs mix "tu" and "usted" within the same translation set.
```

---

### pt-BR — Portugiesisch Brasilianisch

```
## Language-Specific Rules: Brazilian Portuguese (pt-BR)
- Register: Use "voce" throughout. NOT "tu", NOT European Portuguese.
- Plurals: one, other
- Text: PT-BR is 15-30% LONGER than German. Design with ~20% extra space.
- Vocabulary: Diario, Entrada, Humor, Salvar, Excluir, Configuracoes, Pesquisar, Escrever
- CRITICAL — PT-BR vs PT-PT: This is the BIGGEST LLM failure for Portuguese.
  LLMs mix Brazilian and European vocabulary. Validate these terms:
  "usuario" (BR) NOT "utilizador" (PT) | "aplicativo" NOT "aplicacao"
  "celular" NOT "telemovel" | "salvar" NOT "guardar"
  "configuracoes" NOT "definicoes" | "senha" NOT "palavra-passe"
  "baixar" NOT "transferir" | Always state "Brazilian Portuguese, NOT European".
- WARNING — Spelling reform: LLMs sometimes use pre-reform Brazilian spelling. Check.
- Tone: Brazilians expect warm, informal language. Cold formal text reads as bureaucratic.
```

---

### pt-PT — Portugiesisch Europa

```
## Language-Specific Rules: European Portuguese (pt-PT)
- Register: Use "tu" (informal 2nd person) throughout. NOT "voce" (that is Brazilian
  neutral OR European formal — both wrong for a modern PT-PT app). NOT "Vossa Excelencia".
  Modern Portugal apps (Google PT, Microsoft PT, Apple PT) all use "tu".
  Verb conjugation follows "tu": tens, podes, queres, ves, escreves, guardas.
  Implicit addressing (no pronoun) is also natural: "Escreve aqui..." / "Guarda a entrada".

- Plurals: one, many, other (CLDR: "many" for millions without decimals).
  CRITICAL: 0 falls into "other" in pt-PT (NOT into a "zero" category). "0 entradas"
  uses the "other" form, not singular. 2–999,999 use "other". Only exactly 1 uses "one".
  1,000,000 and multiples (without decimals) use "many".

- Text: European Portuguese is 10-20% LONGER than German. Design with ~15-20% extra space.
  Button texts can be critical: "Iniciar sessao" (14 chars) vs. English "Login" (5 chars).

- ACORDO ORTOGRAFICO 1990 (MANDATORY since 2015 in Portugal — obligatory in schools,
  government, and media. Pre-reform spellings are WRONG in official contexts):
  optimo NOT "optimo" | acao NOT "accao" | eletronico NOT "electronico"
  rececao NOT "recepcao" | direcao NOT "direccao" | fator NOT "factor"
  correto NOT "correcto" | ideia NOT "ideia" (no accent) | voo NOT "voo" (no circumflex)
  para NOT "para" (no accent on 3rd person of parar) | "fim de semana" NOT "fim-de-semana"
  LLMs frequently produce pre-reform spellings — verify EVERY removed silent consonant.

- CRITICAL — PT-PT vs PT-BR vocabulary (the #1 LLM failure mode for Portuguese):
  Standard multi-provider LLMs default to Brazilian Portuguese. Every single one of
  these terms MUST use the European variant. Grep for Brazilian variants after translation.

  | German       | pt-PT (CORRECT)          | pt-BR (WRONG for PT-PT)   |
  |--------------|--------------------------|---------------------------|
  | Benutzer     | utilizador               | usuario                   |
  | App          | aplicacao (fem.)         | aplicativo (masc.)        |
  | Speichern    | guardar                  | salvar                    |
  | Einstellungen| definicoes               | configuracoes             |
  | Passwort     | palavra-passe            | senha                     |
  | Herunterladen| transferir               | baixar / fazer download   |
  | Handy        | telemovel                | celular                   |
  | Datei        | ficheiro                 | arquivo                   |
  | Loeschen     | eliminar                 | excluir                   |
  | Bildschirm   | ecra                     | tela                      |
  | Anmelden     | iniciar sessao           | fazer login / entrar      |
  | Abmelden     | terminar sessao          | sair / fazer logout       |
  | Teilen       | partilhar                | compartilhar              |
  | Abonnement   | subscricao               | assinatura                |
  | Kamera       | camara                   | camera                    |
  | Foto         | fotografia / foto        | foto                      |
  | Backup       | copia de seguranca       | backup                    |
  | Suchen       | pesquisar                | buscar / pesquisar        |
  | Aufzeichnung | registo                  | registro                  |
  | Weiter (Btn) | seguinte / continuar     | proximo / continuar       |
  | Zurueck      | voltar / anterior        | voltar                    |
  | E-Mail       | correio eletronico / e-mail | e-mail                 |

  Journaling vocabulary for BestJournal and similar apps:
  diario (Tagebuch), entrada (Eintrag), nota (Notiz), registo (Aufzeichnung — NOT registro),
  humor / disposicao (Stimmung), escrever (schreiben), guardar (speichern).

- CRITICAL — Gerundio vs. Infinitivkonstruktion:
  Portugal uses "estar a + infinitive" for progressive actions. Portugal does NOT use
  Brazilian "-ando / -endo / -indo" gerund in the progressive. This is THE most visible
  PT-PT vs PT-BR marker.

  | Context            | pt-PT (CORRECT)        | pt-BR (WRONG for PT-PT) |
  |--------------------|------------------------|-------------------------|
  | Wird gespeichert   | A guardar...           | Salvando...             |
  | Wird geladen       | A carregar...          | Carregando...           |
  | Wird synchronisiert| A sincronizar...       | Sincronizando...        |
  | Wird gesucht       | A pesquisar...         | Pesquisando...          |
  | Wird verarbeitet   | A processar...         | Processando...          |
  | Wird gesendet      | A enviar...            | Enviando...             |
  | Wird heruntergeladen| A transferir...       | Baixando...             |
  | Wird erstellt      | A criar...             | Criando...              |

  Loading screens, progress indicators and toasts MUST use "A + infinitive" form.
  Grep after translation for: "ando\.\.\." and "endo\.\.\." and "indo\.\.\." to catch
  gerund leakage. Zero matches expected.

- WARNING — Pronoun position (enclisis vs. proclisis):
  Portugal prefers ENCLISIS (pronoun after the verb, hyphenated): "Diga-me", "Mostra-me".
  Brazil prefers PROCLISIS (pronoun before the verb): "Me diga", "Me mostra".
  LLMs default to Brazilian proclisis. Check all imperatives and simple tenses.
  CORRECT: "Mostra-me as entradas" / "Envia-me lembretes"
  WRONG (BR): "Me mostra as entradas" / "Me envia lembretes"

- Typography (pt-PT standard):
  Quotation marks: PRIMARY = guillemets «...», SECONDARY (nested) = "..." (curly).
  Decimal: comma. Thousands: period. Example: 1.234,56 €
  Euro symbol AFTER the number with a space: "9,99 €" — NOT "€9.99".
  Date: DD/MM/AAAA (e.g. 18/04/2026). Time: HH:mm (24h, no AM/PM).

- WARNING — Mes-Namen (Monate) are lowercase in pt-PT after the AO 1990:
  janeiro, fevereiro, marco, abril, maio, junho, julho, agosto, setembro,
  outubro, novembro, dezembro. Weekdays also lowercase: segunda-feira, terca-feira,
  quarta-feira, quinta-feira, sexta-feira, sabado, domingo.

- Tone for journaling apps: Portuguese users expect direct but restrained tone.
  Avoid American-style enthusiasm ("Incrivel!", "Fantastico!"). Emotional prompts
  should be neutral: "Escreve o que pensas" NOT "Partilha os teus sentimentos incriveis!".
  Portuguese tone is noticeably more reserved than Brazilian Portuguese.

- WARNING — Brand and anglicism balance: Portugal accepts "login", "email", "online"
  in casual speech, but professional apps prefer the localized forms: "iniciar sessao",
  "correio eletronico"/"e-mail", "em linha". When in doubt, use the PT-PT form.

- WARNING — LLM default drift: After translating 10+ strings, LLMs frequently lapse
  back into Brazilian vocabulary mid-file. Re-check EVERY critical term in the
  verification pass, especially "salvar", "usuario", "configuracoes", "arquivo",
  "compartilhar", "voce", and any "-ando/-endo/-indo" form.
```

---

### it — Italienisch

```
## Language-Specific Rules: Italian (it)
- Register: Informal "tu/tuo/tua", NOT "Lei". Specify "usa il tu informale, non il Lei formale".
- Plurals: one, other
- Text: Italian is 10-20% LONGER than German. Design with ~15% extra space.
- Vocabulary: Diario, Voce, Umore, Salva, Elimina, Impostazioni, Cerca, Scrivi, Oggi
- Standard Italian — no regional variants needed.
- WARNING — Lei capitalization: "Lei" (capital) = formal you, "lei" (lowercase) = she.
  LLMs forget to capitalize, causing gender confusion.
- WARNING — Register mixing: Lei requires third-person verbs (Lei vuole). LLMs mix
  tu/Lei conjugations in the same string set — sounds illiterate.
- WARNING — Gender agreement: LLMs produce wrong adjective-noun gender agreement in
  longer strings. Check masculine/feminine consistency.
```

---

### nl — Niederlaendisch

```
## Language-Specific Rules: Dutch (nl-NL)
- Register: Informal "je/jij", NOT "u". Netherlands standard. (Belgian Flemish uses "u".)
- Plurals: one, other
- Text: ~0-5% longer than German. Rarely overflows German layouts.
- Vocabulary: Dagboek, Notitie, Stemming, Opslaan, Verwijderen, Instellingen, Zoeken, Vandaag
- CRITICAL — German→Dutch is the HIGHEST-RISK language pair. False friends:
  Dutch "wie"="who" (NOT "how"=hoe) | Dutch "mogen"="allowed to" (NOT "like"=houden van)
  Dutch "meer"="lake" (NOT "sea"=zee) | Dutch "tafel"="table" (NOT "board"=bord)
  Dutch "verstaan"="hear spoken" (NOT general "understand"=begrijpen)
  LLMs exploit German surface similarity and produce WRONG Dutch words that look right.
- WARNING — English interference: 16% of Dutch LLM errors come from English sentence
  structure in Dutch words. Dutch and English are closely related — LLMs mix them.
- WARNING — Separable verbs: Dutch "opslaan", "verwijderen" get split/merged incorrectly.
```

---

### pl — Polnisch

```
## Language-Specific Rules: Polish (pl)
- Register: Informal "ty". Pan/Pani is for government forms, not personal apps.
- Plurals: one (1), few (2-4, 22-24...), other (0, 5-21, 25-31...) — THREE forms needed.
  LLMs routinely confuse "few" and "other" forms.
- Text: Polish is 10-20% LONGER than German.
- Vocabulary: Dziennik, Wpis, Nastroj, Zapisz, Usun, Ustawienia, Dzis
- "Bezplatny" or "Darmowy" for free (darmowy = colloquial, warmer).
- WARNING — Gender agreement: Polish has 3 genders + animate/inanimate masculine.
  LLMs default to masculine. Past tense encodes gender: "zapisalem" (male) vs
  "zapisalam" (female). Prefer impersonal constructions to avoid this.
- WARNING — Variable plurals: LLMs fail with "%d wpisow" vs "%d wpisy" vs "%d wpis".
- WARNING — Case prepositions: LLMs choose wrong case after prepositions.
```

---

### ru — Russisch

```
## Language-Specific Rules: Russian (ru)
- Script: Cyrillic
- Register: Formal "Vy" (Вы, capitalized). Safe default for adult apps. Prevents gendered
  language issues (ty forms assume gender).
- Plurals: one (1,21,31...), few (2-4,22-24...), many (5-20,25-30...), other (decimals)
  — FOUR forms. LLMs frequently forget "many" — produces wrong output for 5-20.
- Text: ~15-20% LONGER than German.
- Vocabulary: Dnevnik (Дневник), Zapis (Запись), Nastroenie (Настроение),
  Sokhranit (Сохранить), Udalit (Удалить), Nastroyki (Настройки),
  Napominanie (Напоминание), Podpiska (Подписка) NOT "abonement"
- CRITICAL — Aspect system: "Save" = "Сохранить" (perfective) NOT "Сохранять" (imperfective).
  Wrong aspect in buttons sounds completely unnatural.
- Verb buttons: Use INFINITIVE (сохранить). LLMs sometimes produce imperative (сохрани) — rude.
- WARNING — Case endings with variables: "%d entries" requires different word forms at 1/2/5/21.
- Include "ё" (yo) — LLMs strip it, but omission changes meaning in some words.
```

---

### uk — Ukrainisch

```
## Language-Specific Rules: Ukrainian (uk)
- Script: Ukrainian Cyrillic (NOT Russian Cyrillic — different letters: і, ї, є, ґ)
- Register: Formal "ви". Safe, respectful default. Politically sensitive context since 2022.
- Plurals: one (1,21...), few (2-4,22-24...), many (5-20,25-30...), other — FOUR forms.
- Text: ~10-20% LONGER than German.
- CRITICAL — NO RUSSIAN WORDS. This is politically sensitive, not just linguistic.
  Validate EVERY term against this table:
  WRONG (Russian) → CORRECT (Ukrainian):
  Сохранить → Зберегти (save) | Удалить → Видалити (delete)
  Настройки → Налаштування (settings) | Настроение → Настрій (mood)
  Напоминание → Нагадування (reminder) | Сегодня → Сьогодні (today)
  Поиск → Пошук (search) | Бесплатно → Безкоштовно (free)
  Конечно → Звичайно (of course) | Понятно → Зрозуміло (understood)
  Запись → Запис (entry — different ending!) | Подписка → Підписка (subscription)
- Vocabulary: Щоденник (journal), Запис (entry), Настрій (mood), Зберегти (save)
- Add to prompt: "Use only authentic Ukrainian vocabulary. Avoid all Russisms.
  Prefer Ukrainian-origin words over Russian/Soviet-era loanwords."
- LLMs mix Ukrainian grammar with Russian vocabulary — hard to catch without native review.
```

---

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
- CRITICAL — Vowel harmony: Turkish suffixes MUST harmonize with root vowels.
  "kitap" → "kitapta" NOT "kitepte". Violations sound like broken grammar.
- WARNING — Agglutination: Suffixes change meaning completely. LLMs stack wrong suffixes
  or skip buffer vowels on loanwords ("App-i" not "App").
- WARNING — Register mixing: LLMs mix "sen" and "siz" in same output.
```

---

### ja — Japanisch

```
## Language-Specific Rules: Japanese (ja)
- Script: Kanji + Hiragana + Katakana (mixed). Use full-width punctuation: 。，！？「」
- Register: Teineigo (丁寧語, polite) — 〜です/ます. NOT sonkeigo (honorific), NOT plain (〜だ).
  Buttons: plain verb OK (保存する). Messages: teineigo (保存されました。).
  NEVER mix polite and plain forms in the same screen.
- Plurals: "other" ONLY — Japanese has no grammatical plural.
- Text: 20-30% SHORTER than German. Layouts may look empty — this is normal.
- Vocabulary: 日記 (nikki, journal — prefer native over ジャーナル), 記録 (kiroku, entry),
  保存する (save), 設定 (settings), 無料 (free), プレミアム (premium, katakana OK),
  振り返り (retrospective)
- Use half-width Arabic numerals (1,2,3), NOT full-width.
- WARNING — Keigo contamination: LLMs over-use honorific prefixes (ご記録 = too formal).
  Journal app should feel personal, not corporate.
- WARNING — Katakana overuse: LLMs prefer katakana loanwords. Use native kanji where natural.
- WARNING — Sentence ending inconsistency: LLMs mix です/ます with だ/である in same output.
```

---

### ko — Koreanisch

```
## Language-Specific Rules: Korean (ko)
- Script: Hangul (한글)
- Register: Haeyoche (해요체) — polite conversational (〜해요, 〜이에요, 〜세요).
  Buttons: imperative OK (저장하기). Messages: 해요체 (저장되었어요.).
  NEVER use haerache (해라체) — sounds like a military order.
  AVOID hapsyoche (합쇼체, 〜합니다) — too formal, like a government form.
- Plurals: "other" ONLY — Korean has no grammatical plural.
- Text: 10-20% SHORTER than German.
- Vocabulary: 일기 (journal), 기록 (entry), 저장하기 (save), 설정 (settings),
  무료 (free), 프리미엄 (premium, loanword OK), 구독 (subscription, native preferred)
- WARNING — Speech level mixing: LLMs mix 합쇼체, 해요체, and 해라체 in same output.
  Sounds completely inconsistent — check every string for consistent speech level.
- WARNING — Particle errors: 은/는, 이/가, 을/를 change based on preceding syllable ending.
  LLMs get this wrong ~15-20% of the time. Wrong particles are immediately noticeable.
```

---

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

---

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

---

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

---

### hi — Hindi

```
## Language-Specific Rules: Hindi (hi)
- Script: Devanagari (देवनागरी)
- Register: Polite "आप" (aap). NOT casual "तुम" or intimate "तू".
  Use semi-formal Hinglish — Hindi grammar with common English tech terms.
- Plurals: one, other (same as English — simple).
- Text: 10-30% shorter in characters than German, but visually similar width due to
  Devanagari glyph width.
- Vocabulary: डायरी (diary, loanword = standard), एंट्री or प्रविष्टि (entry),
  सेटिंग्स (Settings, English = universally used), सहेजें (save), हटाएं (delete),
  मूड (mood, English loanword = natural)
- Use Arabic numerals (0-9), NOT Devanagari (०-९).
- English tech terms are natural in Hindi: Settings, Account, Premium, Export, PDF.
- WARNING — Hinglish inconsistency: LLMs randomly switch between pure Hindi and Hinglish
  with no pattern. Specify which English terms to KEEP and which to TRANSLATE.
- WARNING — Sanskrit-heavy register: LLMs default to overly formal Sanskritized vocabulary.
  Use the word most commonly used in everyday speech, regardless of etymology.
```

---

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

---

### id — Indonesisch

```
## Language-Specific Rules: Indonesian (id)
- Script: Latin — no special characters.
- Register: Informal "kamu", NOT "Anda". "Anda" is for manuals and airports.
  Note: "Anda" is always capitalized when used — LLMs sometimes get this wrong.
- Plurals: "other" ONLY — Indonesian has no grammatical plural.
- Text: 10-20% SHORTER than German. Rarely overflows.
- Vocabulary: Jurnal or catatan harian (journal), Catatan (entry, more natural than entri),
  Simpan (save), Hapus (delete), Pengaturan (settings),
  Suasana hati or mood (mood, English loanword natural), Langganan (subscription)
- WARNING — Affix errors: Indonesian uses prefix me- (active), di- (passive), ber-, ke-an.
  LLMs confuse voice — prefer active me- for app UI.
- WARNING — Register mixing: LLMs mix "Anda" and "kamu" in same output.
  Specify "use kamu throughout, never Anda".
- Indonesian vs Malay (ms): If also targeting Malaysia, note key differences:
  delete: hapus/padam | settings: pengaturan/tetapan | account: akun/akaun.
  Separate translations required.
```

---

### bn — Bengali

```
## Language-Specific Rules: Bengali (bn)
- Script: Bengali/Bangla (বাংলা) — NOT Devanagari.
- Register: Informal "তুমি" (tumi) — warm and personal, not ultra-formal "আপনি" (apni)
  and not ultra-casual "তুই" (tui).
- Plurals: one, other
- Text: 20-35% LONGER than German — watch button labels closely.
- Vocabulary: দিনলিপি or জার্নাল (journal), এন্ট্রি or লেখা (entry), মেজাজ (mood),
  স্মরণিকা or রিমাইন্ডার (reminder)
- Use Arabic numerals (0-9), NOT Bengali (০১২৩৪৫৬৭৮৯).
- Use neutral Bengali for both India (bn-IN) and Bangladesh (bn-BD).
- English tech terms are natural: "Save করুন" is authentic code-mixing.
- CRITICAL — Hindi leakage: LLMs leak Hindi vocabulary and Devanagari structures into
  Bengali. Bengali and Hindi are DIFFERENT languages with DIFFERENT scripts.
  Check for Hindi words where native Bengali equivalents exist.
- Avoid Sanskrit loanwords where native Bengali alternatives are available.
```

---

### te — Telugu

```
## Language-Specific Rules: Telugu (te)
- Script: Telugu (తెలుగు) — unique to Telugu, not shared with other languages.
- Register: Polite "మీరు" (miiru). NOT casual నువ్వు (nuvvu) — too familiar for app UI.
- Plurals: one, other
- Text: 15-30% LONGER than German.
- Vocabulary: డైరీ or జర్నల్ (journal), ఎంట్రీ or నమోదు (entry), మూడ్ or మనోభావం (mood),
  రిమైండర్ (reminder)
- Use Arabic numerals (0-9), NOT Telugu (౦౧౨౩౪౫౬౭౮౯).
- English loanwords in Telugu script are natural for tech UI (Settings, Save, App).
- CRITICAL — Agglutination: Telugu words grow by chaining suffixes. "Save" = "సేవ్ చేయండి"
  (3x longer). Avoid compounds exceeding 15 syllables — they overflow buttons.
  Always provide <!-- SHORTER: ... --> for translations exceeding 40% expansion.
- Few-shot prompting improves Telugu quality significantly. Add 2-3 examples if needed.
```

---

### mr — Marathi

```
## Language-Specific Rules: Marathi (mr)
- Script: Devanagari (देवनागरी) — same script as Hindi but DIFFERENT language.
- Register: "तुम्ही" (tumhi) — warm and respectful, not too formal (आपण) or casual (तू).
- Plurals: one, other
- Text: 20-35% LONGER than German.
- Vocabulary: दैनंदिनी or जर्नल (journal), नोंद (entry), मनस्थिती (mood),
  स्मरणपत्र or रिमाइंडर (reminder)
- Use Arabic numerals (0-9), NOT Devanagari. English tech terms stay English.
- CRITICAL — Hindi confusion (most common LLM error!): Marathi and Hindi share Devanagari
  but have DIFFERENT vocabulary, grammar, and expressions. A Hindi translation is NOT Marathi.
  Marathi speakers IMMEDIATELY notice Hindi words — very sensitive to this ("Ghaati" complex).
- Marathi has THREE genders (masculine, feminine, NEUTER) unlike Hindi's two.
  LLMs frequently make gender agreement errors.
- The letter ळ (la) is common in Marathi but ABSENT in Hindi — use it correctly.
- Marathi prefers Sanskrit/Portuguese loanwords, Hindi prefers Arabic-Persian. Do NOT mix.
```

---

### ta — Tamil

```
## Language-Specific Rules: Tamil (ta)
- Script: Tamil (தமிழ்) — one of the oldest scripts in use (2000+ years).
- Register: Polite "நீங்கள்" (neengal). NOT casual "நீ" (nee).
- Plurals: one, other
- Text: 25-40% LONGER than German — SIGNIFICANT expansion. Always provide
  <!-- SHORTER: ... --> alternatives for labels exceeding 30% expansion.
- Vocabulary: நாட்குறிப்பு (journal, native), உள்ளீடு or பதிவு (entry),
  மனநிலை (mood, native), நினைவூட்டல் (reminder)
- Use Arabic numerals (0-9), NOT Tamil (௦-௯).
- For emotional/personal content: ALWAYS use native Tamil words, NOT Sanskrit.
  For tech UI elements: English loanwords ARE acceptable (Settings, Save, Export, App).
- CRITICAL — Language purism: Tamil speakers have the STRONGEST language pride of all
  Indian languages. BAD translations get ACTIVELY negative Play Store reviews.
  Do NOT use Sanskrit loanwords — Tamil does not borrow from Hindi/Sanskrit.
- WARNING — Tanglish: LLMs produce Tamil+English mixed text. Check each string
  for unnecessary English mixing. Pure Tamil for content, English OK for tech terms.
```

---

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

---

### gu — Gujarati

```
## Language-Specific Rules: Gujarati (gu)
- Script: Gujarati (ગુજરાતી) — derived from Devanagari but WITHOUT the horizontal top line.
- Register: Polite-informal "તમે" (tame) — warm but respectful.
- Plurals: one, other
- Text: 15-25% LONGER than German.
- Vocabulary: ડાયરી or જર્નલ (journal), એન્ટ્રી or નોંધ (entry, native),
  મૂડ or મનોસ્થિતિ (mood), રિમાઇન્ડર (reminder)
- Use Arabic numerals (0-9), NOT Gujarati (૦-૯).
- English tech terms are very naturally accepted (business-oriented community).
  Keep Settings, Save, App, Payment, Premium in English.
- WARNING — Script fallback: LLMs sometimes output Devanagari characters instead of
  Gujarati. If a character has the horizontal top line (shirorekha), it is Devanagari,
  NOT Gujarati. Verify correct Unicode range (U+0A80-U+0AFF).
- LLM quality for Gujarati is notably lower than Hindi/Bengali — extra review recommended.
```

---

### kn — Kannada

```
## Language-Specific Rules: Kannada (kn)
- Script: Kannada (ಕನ್ನಡ) — round, distinctive script.
- Register: Polite "ನೀವು" (neevu). NOT casual "ನೀನು" (neenu).
- Plurals: one, other
- Text: 20-35% LONGER than German.
- Vocabulary: ಡೈರಿ or ಜರ್ನಲ್ (journal), ಎಂಟ್ರಿ or ನಮೂದು (entry, native),
  ಮೂಡ್ or ಮನಸ್ಥಿತಿ (mood), ರಿಮೈಂಡರ್ (reminder)
- Use Arabic numerals (0-9), NOT Kannada (೦-೯).
- Bangalore = India's tech hub. English loanwords in Kannada script are perfectly natural.
  Use mixed approach: English for tech (Settings, Save), native for personal content.
- WARNING — Script confusion with Telugu: Kannada and Telugu scripts look similar (both
  use circular shapes). LLMs occasionally mix Telugu characters into Kannada output.
  Verify ONLY Kannada Unicode range (U+0C80-U+0CFF), NOT Telugu (U+0C00-U+0C7F).
- LLM quality for Kannada is the weakest of all South Indian languages — native review needed.
```

---

### ml — Malayalam

```
## Language-Specific Rules: Malayalam (ml)
- Script: Malayalam (മലയാളം) — highly curved script with the MOST COMPLEX conjunct system
  of all Indian scripts.
- Register: Polite "നിങ്ങൾ" (ningal). NOT casual "നീ" (nee).
- Plurals: one, other
- Text: 25-40% LONGER than German — THE MOST EXTREME expansion of all Indian languages.
  Single words can represent entire sentences due to agglutinative suffix chaining.
  ALWAYS provide <!-- SHORTER: ... --> alternatives. Buttons MUST use flexible widths.
- Vocabulary: ഡയറി or ജേണൽ (journal), എൻട്രി or രേഖ (entry, native),
  മൂഡ് or മാനസികാവസ്ഥ (mood), ഓർമ്മപ്പെടുത്തൽ (reminder)
- Use Arabic numerals (0-9), NOT Malayalam (൦-൯).
- Use simplified orthography (1971 reform) — works on all devices.
  Do NOT use traditional orthography (more ligatures, rendering issues).
- English tech terms (PDF, Export, Premium, Settings) stay English — widely understood.
- Malayalam does NOT borrow from Hindi/Sanskrit — use Dravidian roots.
- CRITICAL — Hardest Indian language for LLMs: Least training data + most complex morphology.
  LLM output is often Unicode-correct but orthographically wrong (wrong ligature choices,
  incorrect suffix combinations). NATIVE SPEAKER REVIEW IS MANDATORY.
- Kerala has 96% literacy — audience expects PERFECT grammar. Poor translations noticed fast.
```
