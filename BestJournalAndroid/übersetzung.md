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

### 4.1 Franzoesisch (fr)

```
## Language-Specific Rules: French (fr-FR)
- Register: Use informal "tu" form, not "vous" — this is a personal journal app
- Use French punctuation rules: space before ; : ! ? (thin non-breaking space)
- Text expansion: French is typically 15-20% longer than German — watch button labels
- Gender: When referring to the user's actions (e.g., "selected"), use masculine as default
  unless the context clearly indicates otherwise. Prefer gender-neutral formulations
  where possible.
- "Journal" can stay as "Journal" in French (same word)
```

### 4.2 Spanisch (es)

```
## Language-Specific Rules: Spanish (es-419)
- Register: Use informal "tu" form, not "usted"
- Use Latin American Spanish (es-419) as the default — it reaches more users than
  Castilian Spanish (es-ES)
- "Entrada" for "Eintrag/Entry", "Diario" for "Tagebuch/Journal"
- Text expansion: Spanish is 15-25% longer than German — watch UI space
- Avoid region-specific slang — use neutral Latin American Spanish
```

### 4.3 Portugiesisch Brasilianisch (pt-BR)

```
## Language-Specific Rules: Brazilian Portuguese (pt-BR)
- Register: Use informal "voce" form
- Use Brazilian Portuguese, NOT European Portuguese — very different vocabulary and grammar
- "Entrada" for "Eintrag/Entry", "Diario" for "Tagebuch/Journal"
- Brazilians prefer "celular" (not "telemovel"), "tela" (not "ecra")
- Text expansion: Similar to Spanish, 15-25% longer
```

### 4.4 Japanisch (ja)

```
## Language-Specific Rules: Japanese (ja)
- Register: Use polite form (desu/masu). NOT casual plain form, NOT ultra-formal keigo.
- Use full-width punctuation: periods, commas, brackets, exclamation/question marks
- Plurals: Use ONLY the "other" quantity — Japanese has no grammatical plural
- Text will typically be 20-40% SHORTER than German — this is normal
- Keep "Journal" as "ジャーナル" (katakana) — commonly understood in app context
- For "Entry": use "エントリー" (katakana) or "記録" (native) — be consistent
- Numbers: Use half-width Arabic numerals (1, 2, 3), not full-width
```

### 4.5 Koreanisch (ko)

```
## Language-Specific Rules: Korean (ko)
- Register: Use polite speech level 해요체 (haeyo-che) — respectful but not stiff
- Plurals: Use ONLY the "other" quantity — Korean has no grammatical plural
- Text will be similar length or shorter than German
- Use native Korean words (순 한국어) where they sound natural,
  Sino-Korean for formal/technical terms
- "일기" for "Tagebuch/Journal", "기록" for "Eintrag/Entry"
```

### 4.6 Chinesisch Vereinfacht (zh-Hans)

```
## Language-Specific Rules: Simplified Chinese (zh-Hans)
- Use Simplified Chinese (简体中文) for mainland China — NOT Traditional Chinese
- Plurals: Use ONLY the "other" quantity — Chinese has no grammatical plural
- Text will be significantly shorter than German (30-50%) — this is normal
- Use full-width punctuation: 。，！？（）
- "日记" for "Tagebuch/Journal", "日记条目" for "Eintrag/Entry"
- Numbers: Use half-width Arabic numerals
```

### 4.7 Chinesisch Traditionell (zh-Hant)

```
## Language-Specific Rules: Traditional Chinese (zh-Hant)
- Use Traditional Chinese (繁體中文) for Taiwan/Hong Kong — NOT Simplified Chinese
- This is a SEPARATE translation from Simplified Chinese, not just character conversion
- Vocabulary and expressions differ from mainland China
- Plurals: Use ONLY the "other" quantity
- "日記" for "Tagebuch/Journal"
```

### 4.8 Hindi (hi)

```
## Language-Specific Rules: Hindi (hi)
- Use Devanagari script (देवनागरी)
- Register: Use informal "तुम" form (not formal "आप") — personal journal app
- Plurals: one, other
- Text expansion: Hindi translations can be 20-40% longer than German — watch UI space
- Technical terms (PDF, Premium, Export) can stay in English — commonly understood
- "डायरी" (diary) for "Tagebuch/Journal", "प्रविष्टि" for "Eintrag/Entry"
```

### 4.9 Arabisch (ar)

```
## Language-Specific Rules: Arabic (ar)
- This is a RIGHT-TO-LEFT (RTL) language
- Numbers, English brand names, and technical terms must remain left-to-right (LTR)
- Use Modern Standard Arabic (MSA / فصحى) — understood across all Arab countries
- Register: Semi-formal, warm tone
- CRITICAL — Plurals: Arabic requires ALL SIX quantity forms:
  - zero (للصفر)
  - one (للواحد)
  - two (للاثنين)
  - few (للقليل — 3-10)
  - many (للكثير — 11-99)
  - other (لغير ذلك — 100+)
  Generate ALL SIX for every <plurals> element. Missing quantities WILL crash the app.
- Text expansion: Arabic is typically 20-30% longer than German
- "يوميات" for "Tagebuch/Journal", "إدخال" for "Eintrag/Entry"
- Use Unicode BiDi control characters where needed for mixed-direction text
  (e.g., a sentence with an English brand name in the middle)
```

### 4.10 Tuerkisch (tr)

```
## Language-Specific Rules: Turkish (tr)
- Register: Use informal "sen" form, not formal "siz"
- Plurals: one, other
- CRITICAL: Turkish uses agglutination — words can become VERY long
  If a button/label text exceeds the original by more than 50%, you MUST provide
  a shorter alternative as <!-- SHORTER: ... -->
- Special characters: ğ, ş, ı, ö, ü, ç are all standard Turkish — use them correctly
- CRITICAL I/i distinction: Turkish has TWO different i-letters:
  - İ/i (dotted) — like English
  - I/ı (dotless) — unique to Turkish
  Never substitute one for the other.
- "Günlük" for "Tagebuch/Journal", "Giriş" for "Eintrag/Entry"
```

### 4.11 Russisch (ru)

```
## Language-Specific Rules: Russian (ru)
- Register: Use informal "ты" form, not formal "Вы"
- Plurals: Russian requires FOUR quantity forms:
  - one (1, 21, 31...)
  - few (2-4, 22-24, 32-34...)
  - many (5-20, 25-30, 35-40...)
  - other (fractional numbers)
  Generate ALL FOUR for every <plurals> element.
- Text expansion: Russian is typically 15-25% longer than German
- "Дневник" for "Tagebuch/Journal", "Запись" for "Eintrag/Entry"
```

### 4.12 Polnisch (pl)

```
## Language-Specific Rules: Polish (pl)
- Register: Use informal form
- Plurals: Polish requires FOUR quantity forms:
  - one (1)
  - few (2-4, 22-24, 32-34...)
  - many (5-21, 25-31...)
  - other (fractional numbers)
  Generate ALL FOUR for every <plurals> element.
- Text expansion: Polish is 20-30% longer than German — significant UI impact
- "Dziennik" for "Tagebuch/Journal", "Wpis" for "Eintrag/Entry"
```

### 4.13 Thai (th)

```
## Language-Specific Rules: Thai (th)
- Thai has NO spaces between words — this is normal, do not add spaces
- Plurals: Use ONLY the "other" quantity — Thai has no grammatical plural
- Use Arabic numerals (0-9), not Thai numerals, unless the app explicitly uses Thai numerals
- Tone particles (ค่ะ/ครับ) are NOT needed in UI strings — keep it neutral
- Text expansion: Thai translations can be significantly longer than German
- "ไดอารี่" for "Tagebuch/Journal" (or "บันทึก" for "Aufzeichnung")
```

### 4.14 Indonesisch (id)

```
## Language-Specific Rules: Indonesian (id)
- Register: Use informal form — Indonesian is naturally less formal than Malay
- Plurals: Use ONLY the "other" quantity — Indonesian has no grammatical plural
- Indonesian is often shorter or similar length to German — no expansion issue
- "Jurnal" for "Tagebuch/Journal", "Entri" for "Eintrag/Entry"
- Very straightforward language for localization — fewest special cases
```

### 4.15 Italienisch (it)

```
## Language-Specific Rules: Italian (it)
- Register: Use informal "tu" form, not "Lei"
- Plurals: one, many, other
- Text expansion: Italian is 15-20% longer than German
- "Diario" for "Tagebuch/Journal", "Voce" for "Eintrag/Entry"
- Gender: Prefer masculine default when referring to user actions,
  or use gender-neutral formulations where possible
```

### 4.16 Niederlaendisch (nl)

```
## Language-Specific Rules: Dutch (nl)
- Register: Use informal "je/jij" form, not "u"
- Plurals: one, other
- Very similar length to German — minimal expansion issues
- "Dagboek" for "Tagebuch/Journal", "Invoer" for "Eintrag/Entry"
```

### 4.17 Ukrainisch (uk)

```
## Language-Specific Rules: Ukrainian (uk)
- Register: Use informal "ти" form
- Plurals: Ukrainian requires FOUR quantity forms (same as Russian):
  - one, few, many, other
  Generate ALL FOUR for every <plurals> element.
- Use Ukrainian language specifically — do NOT use Russian words or mixed forms
- "Щоденник" for "Tagebuch/Journal", "Запис" for "Eintrag/Entry"
```

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
| **8** | Hindi | hi | 600 Mio. | Am schnellsten wachsender Smartphone-Markt |
| **9** | Arabisch | ar | 400 Mio. | 22 Laender, RTL-Support noetig |
| **10** | Tuerkisch | tr | 85 Mio. | Starker Android-Anteil |
| 11 | Russisch | ru | 250 Mio. | Grosser Markt, eigene App-Stores |
| 12 | Indonesisch | id | 270 Mio. | Riesiger Android-Markt, einfache Sprache |
| 13 | Italienisch | it | 67 Mio. | Europa, zahlungskraeftig |
| 14 | Niederlaendisch | nl | 25 Mio. | Europa, zahlungskraeftig |
| 15 | Polnisch | pl | 45 Mio. | Starker Android-Markt in Europa |
| 16 | Thai | th | 70 Mio. | Wachsender suedostasiatischer Markt |
| 17 | Ukrainisch | uk | 45 Mio. | Eigene Sprache, nicht Russisch verwenden |

### Schnellstart-Empfehlung

Fuer den ersten Launch reichen **Prioritaet 1-6** (Englisch, Spanisch, Portugiesisch,
Franzoesisch, Japanisch, Koreanisch). Das deckt die zahlungskraeftigsten Maerkte ab.

Arabisch (Prioritaet 9) erfordert als einzige Sprache technische App-Aenderungen
(RTL-Support), daher als separates Projekt planen.

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
