# Android String Best Practices: Der ultimative Leitfaden

> **Ziel:** Strings so erstellen, dass sie in JEDE Sprache der Welt perfekt uebersetzt werden koennen.
> **Quellen:** Google Android Docs, Meta Engineering Blog, Netflix TechBlog, Airbnb Engineering,
> Duolingo Blog, Unicode CLDR, Phrase/Lokalise/Crowdin Dokumentation, Jeroen Mols Naming Convention.
> **Stand:** April 2026

---

## Inhaltsverzeichnis

1. [Die goldenen Regeln](#1-die-goldenen-regeln)
2. [String-Naming-Konvention](#2-string-naming-konvention)
3. [Platzhalter und Format-Specifier](#3-platzhalter-und-format-specifier)
4. [XLIFF-Tags fuer nicht-uebersetzbare Inhalte](#4-xliff-tags-fuer-nicht-uebersetzbare-inhalte)
5. [Plurals — das komplexeste Thema](#5-plurals--das-komplexeste-thema)
6. [Uebersetzer-Kommentare](#6-uebersetzer-kommentare)
7. [Nicht-uebersetzbare Strings](#7-nicht-uebersetzbare-strings)
8. [String Arrays](#8-string-arrays)
9. [HTML und Styled Text in Strings](#9-html-und-styled-text-in-strings)
10. [Textexpansion und Layout-Design](#10-textexpansion-und-layout-design)
11. [RTL-Sprachen (Arabisch, Hebraeisch, Farsi, Urdu)](#11-rtl-sprachen-arabisch-hebraeisch-farsi-urdu)
12. [Problematische Sprachen und Grammatik-Fallen](#12-problematische-sprachen-und-grammatik-fallen)
13. [ICU MessageFormat fuer Gender und komplexe Muster](#13-icu-messageformat-fuer-gender-und-komplexe-muster)
14. [Jetpack Compose und i18n](#14-jetpack-compose-und-i18n)
15. [Dateiorganisation bei grossen Apps](#15-dateiorganisation-bei-grossen-apps)
16. [Anti-Patterns — was NIEMALS gemacht werden darf](#16-anti-patterns--was-niemals-gemacht-werden-darf)
17. [Automatische Qualitaetspruefung (Lint und CI)](#17-automatische-qualitaetspruefung-lint-und-ci)
18. [Tooling und Uebersetzungsplattformen](#18-tooling-und-uebersetzungsplattformen)
19. [Per-App Language Preferences (In-App Sprachwechsel)](#19-per-app-language-preferences-in-app-sprachwechsel)
20. [Checkliste vor der Uebersetzung](#20-checkliste-vor-der-uebersetzung)
21. [Vollstaendiges Beispiel: Best-Practice strings.xml](#21-vollstaendiges-beispiel-best-practice-stringsxml)

---

## 1. Die goldenen Regeln

### Regel 1: JEDER benutzersichtbare Text gehoert in strings.xml

```xml
<!-- FALSCH — Android kann das nicht uebersetzen -->
android:text="Einstellungen speichern"

<!-- RICHTIG -->
android:text="@string/settings_button_save"
```

Kein einziger sichtbarer Text darf hardcodiert im Code oder Layout stehen. Android kann nur
Strings uebersetzen die in `res/values/strings.xml` definiert sind.

### Regel 2: Die Default-Datei muss VOLLSTAENDIG sein

`res/values/strings.xml` ist der Fallback fuer ALLE Sprachen. Fehlt dort ein String,
crasht die App bei Nutzern deren Sprache nicht unterstuetzt wird.

```
res/values/strings.xml          <-- MUSS alle Strings enthalten (Fallback)
res/values-de/strings.xml       <-- Deutsche Uebersetzung
res/values-ja/strings.xml       <-- Japanische Uebersetzung
res/values-pt-rBR/strings.xml   <-- Brasilianisches Portugiesisch (r vor Region!)
```

### Regel 3: Nie Saetze zusammenbauen — immer vollstaendige Saetze als String

```kotlin
// FALSCH — bricht in JEDER nicht-englischen Sprache
val message = "Willkommen " + userName + "!"
val message = getString(R.string.welcome) + " " + userName

// RICHTIG — Uebersetzer kann Wortstellung aendern
val message = getString(R.string.welcome_user, userName)
```

```xml
<string name="welcome_user">Willkommen, %1$s!</string>
```

### Regel 4: Niemals den gleichen String in verschiedenen Kontexten wiederverwenden

Das englische Wort "Back" kann Zurueck (Navigation), Ruecken (Koerper) oder Rueckseite
(Buch) bedeuten. In den meisten Sprachen sind das komplett verschiedene Woerter.

```xml
<!-- FALSCH: Ein String, drei verschiedene Bedeutungen -->
<string name="back">Zurueck</string>

<!-- RICHTIG: Eigener String pro Kontext -->
<!-- Navigation button to return to previous screen -->
<string name="action_navigate_back">Zurueck</string>
<!-- Label for the back cover of a book in the library -->
<string name="label_book_back_cover">Rueckseite</string>
```

### Regel 5: Plural-Logik IMMER ueber das Plural-System, NIE im Code

```kotlin
// FALSCH — funktioniert nicht in Russisch, Arabisch, Polnisch...
val text = if (count == 1) "1 Eintrag" else "$count Eintraege"

// RICHTIG
val text = resources.getQuantityString(R.plurals.journal_entries, count, count)
```

---

## 2. String-Naming-Konvention

### Das bewaehrte Schema: `<WO>_<ELEMENT>_<BESCHREIBUNG>`

Basierend auf dem Jeroen Mols System, das von den meisten grossen Android-Teams verwendet wird:

```
<SCREEN/MODUL>_<ELEMENT_TYP>_<BESCHREIBUNG>
```

### Konkrete Beispiele

```xml
<!-- ══════════════════════════════════════════
     SCREEN-SPEZIFISCHE STRINGS
     Prefix = Screen-Name
     ══════════════════════════════════════════ -->

<!-- Onboarding -->
<string name="onboarding_title_welcome">Willkommen</string>
<string name="onboarding_button_next">Weiter</string>
<string name="onboarding_label_email_hint">E-Mail-Adresse</string>

<!-- Journal -->
<string name="journal_title_new_entry">Neuer Eintrag</string>
<string name="journal_button_save">Speichern</string>
<string name="journal_error_save_failed">Eintrag konnte nicht gespeichert werden</string>
<string name="journal_toast_saved">Eintrag gespeichert.</string>

<!-- Settings -->
<string name="settings_title_screen">Einstellungen</string>
<string name="settings_label_language">Sprache</string>
<string name="settings_dialog_title_logout">Abmelden?</string>
<string name="settings_dialog_message_logout">Alle lokalen Daten werden geloescht.</string>


<!-- ══════════════════════════════════════════
     APP-WEITE STRINGS (shared)
     Prefix = "all_"
     ══════════════════════════════════════════ -->

<string name="all_button_ok">OK</string>
<string name="all_button_cancel">Abbrechen</string>
<string name="all_error_network">Keine Internetverbindung</string>
<string name="all_loading">Laden\u2026</string>
```

### Element-Typ-Suffixe (Empfehlung)

| Suffix | Verwendung | Beispiel |
|--------|-----------|---------|
| `_title` | Screen-Titel, Dialog-Titel | `journal_title_entries` |
| `_label` | Beschriftungen, Felder | `settings_label_language` |
| `_hint` | Eingabefeld-Platzhalter | `onboarding_hint_email` |
| `_button` | Button-Text | `journal_button_save` |
| `_error` | Fehlermeldungen | `settings_error_save_failed` |
| `_dialog_title` | Dialog-Kopfzeile | `logout_dialog_title` |
| `_dialog_message` | Dialog-Inhalt | `logout_dialog_message` |
| `_toast` | Toast-Nachrichten | `journal_toast_saved` |
| `_snack` | Snackbar-Text | `journal_snack_undo` |
| `_desc` | Content Descriptions (Accessibility) | `mic_button_desc` |

### Warum dieses Schema skaliert

- **IDE-Autocomplete** gruppiert nach Prefix — alle `journal_`-Strings erscheinen zusammen
- **Feature-Loeschung** ist einfach: alle Strings mit dem Prefix entfernen
- **Neue Entwickler** koennen Namen vorhersagen ohne zu suchen
- **Uebersetzer** sehen sofort den Kontext durch den Screen-Prefix

### Was NIEMALS gemacht werden darf

```xml
<!-- SCHLECHT: bedeutungslos -->
<string name="text1">Speichern</string>
<string name="error7">Fehler aufgetreten</string>
<string name="button">OK</string>

<!-- SCHLECHT: zu generisch, kein Kontext fuer Uebersetzer -->
<string name="save">Speichern</string>
```

---

## 3. Platzhalter und Format-Specifier

### IMMER nummerierte Positionen verwenden

```xml
<!-- FALSCH — Reihenfolge kann nicht veraendert werden -->
<string name="user_action">%s hat %s ein Foto geliked</string>

<!-- RICHTIG — Uebersetzer kann Reihenfolge anpassen -->
<string name="user_action">%1$s hat das Foto von %2$s geliked</string>
```

**Warum:** In manchen Sprachen steht das Objekt vor dem Subjekt. Japanisch wuerde z.B.
`%2$s の写真を %1$s がいいねしました` brauchen — umgekehrte Reihenfolge.

### Vollstaendige Format-Specifier-Referenz

| Specifier | Typ | Beispiel | Hinweis |
|-----------|-----|---------|---------|
| `%1$s` | String | `%1$s hat geantwortet` | Fuer Namen, Texte |
| `%1$d` | Integer | `%1$d Nachrichten` | Fuer ganze Zahlen |
| `%1$f` | Float | `%1$f MB` | Selten direkt verwenden |
| `%1$.2f` | Float mit 2 Nachkommastellen | `%1$.2f MB` | Fuer Preise, Groessen |
| `%1$,d` | Integer mit Tausendertrenner | `%1$,d Eintraege` | Locale-abhaengig! |

### Mehrere Platzhalter — IMMER nummerieren

```xml
<!-- Immer nummeriert, auch bei nur einem Platzhalter (Konsistenz!) -->
<string name="greeting">Hallo, %1$s!</string>

<!-- Mehrere Platzhalter -->
<string name="export_summary">%1$d Eintraege am %2$s exportiert</string>

<!-- Uebersetzer in einer anderen Sprache kann umordnen: -->
<!-- Japanisch: "%2$sに%1$d件のエントリーをエクスポートしました" -->
```

---

## 4. XLIFF-Tags fuer nicht-uebersetzbare Inhalte

URLs, Markennamen, Codeschnipsel, technische Tokens und Format-Platzhalter muessen
vor versehentlicher Uebersetzung geschuetzt werden.

### Namespace deklarieren (einmal im resources-Tag)

```xml
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
```

### Beispiele

```xml
<!-- Benutzername schuetzen -->
<string name="greeting">
    Hallo, <xliff:g id="user_name" example="Maria">%1$s</xliff:g>!
</string>

<!-- URL schuetzen -->
<string name="visit_us">
    Besuche uns auf <xliff:g id="homepage" example="https://example.com">%1$s</xliff:g>
</string>

<!-- Rabattcode schuetzen -->
<string name="promo">
    Nutze "<xliff:g id="promo_code" example="SAVE20">%1$s</xliff:g>" fuer 20% Rabatt.
</string>

<!-- Zeitangabe schuetzen -->
<string name="countdown">
    Noch <xliff:g id="time_remaining" example="5 Tage">%1$s</xliff:g> bis zum Urlaub
</string>
```

### Beide Attribute sind PFLICHT

| Attribut | Zweck | Beispiel |
|----------|-------|---------|
| `id` | Erklaert dem Uebersetzer WAS das ist | `"user_name"`, nicht `"x"` |
| `example` | Zeigt WIE der Wert zur Laufzeit aussieht | `"Maria"`, `"5 Tage"` |

Ohne `xliff:g` passiert es, dass Uebersetzer `%1$s` versehentlich loeschen,
verschieben oder uebersetzen — was die App zum Crash bringt.

---

## 5. Plurals — das komplexeste Thema

### Die 6 CLDR-Kategorien

Android nutzt unter der Haube das Unicode CLDR (Common Locale Data Repository).
Es gibt genau 6 moegliche Plural-Kategorien:

| Kategorie | Bedeutung | Beispiel-Sprachen |
|-----------|-----------|-------------------|
| `zero` | Spezialregel fuer 0 | Arabisch, Lettisch |
| `one` | Singular | Deutsch (1), Englisch (1) |
| `two` | Dual (genau 2) | Arabisch (2), Walisisch |
| `few` | "Wenige" | Russisch (2-4), Polnisch, Tschechisch |
| `many` | "Viele" | Arabisch (11-99), Polnisch (5+) |
| `other` | Standard-Fallback (**PFLICHT**) | ALLE Sprachen |

### Welche Sprache braucht was?

| Sprache | Benoetigte Kategorien | Kommentar |
|---------|----------------------|-----------|
| Deutsch | `one`, `other` | Einfach: 1 = Singular, Rest = Plural |
| Englisch | `one`, `other` | Wie Deutsch |
| Franzoesisch | `one`, `other` | 0 und 1 = `one` (!) |
| Russisch | `one`, `few`, `many`, `other` | Komplex: abhaengig von letzter Ziffer + Ausnahmen |
| Polnisch | `one`, `few`, `many`, `other` | Aehnlich wie Russisch, aber andere Regeln |
| Arabisch | `zero`, `one`, `two`, `few`, `many`, `other` | ALLE 6 Kategorien! |
| Japanisch | `other` | Nur 1 Form — kein grammatischer Plural |
| Chinesisch | `other` | Nur 1 Form |
| Koreanisch | `other` | Nur 1 Form |
| Tuerkisch | `other` | Nur 1 Form |
| Hindi | `one`, `other` | 0 und 1 = `one`, alles andere = `other` |
| Finnisch | `one`, `other` | Wie Deutsch |
| Ungarisch | `one`, `other` | Wie Deutsch |

### Konkretes Beispiel: "Journal-Eintraege"

**Deutsch (2 Formen):**
```xml
<!-- values/strings.xml (Default = Deutsch) -->
<plurals name="journal_entries">
    <item quantity="one">%d Eintrag</item>
    <item quantity="other">%d Eintr\u00e4ge</item>
</plurals>
```

**Englisch (2 Formen):**
```xml
<!-- values-en/strings.xml -->
<plurals name="journal_entries">
    <item quantity="one">%d entry</item>
    <item quantity="other">%d entries</item>
</plurals>
```

**Russisch (4 Formen — komplexe Regeln!):**
```xml
<!-- values-ru/strings.xml -->
<!-- one: endet auf 1, aber NICHT auf 11 → 1, 21, 31, 41... -->
<!-- few: endet auf 2-4, aber NICHT auf 12-14 → 2, 3, 4, 22, 23... -->
<!-- many: alles andere ganzzahlige → 0, 5-20, 25-30, 11-14... -->
<!-- other: Dezimalzahlen -->
<plurals name="journal_entries">
    <item quantity="one">%d запись</item>
    <item quantity="few">%d записи</item>
    <item quantity="many">%d записей</item>
    <item quantity="other">%d записи</item>
</plurals>
```

**Arabisch (6 Formen — das Maximum!):**
```xml
<!-- values-ar/strings.xml -->
<plurals name="journal_entries">
    <item quantity="zero">لا توجد إدخالات</item>
    <item quantity="one">إدخال واحد</item>
    <item quantity="two">إدخالان</item>
    <item quantity="few">%d إدخالات</item>
    <item quantity="many">%d إدخالاً</item>
    <item quantity="other">%d إدخال</item>
</plurals>
```

**Polnisch (4 Formen — Achtung bei 12-14!):**
```xml
<!-- values-pl/strings.xml -->
<!-- ACHTUNG: 12, 13, 14 → "many", NICHT "few"! -->
<plurals name="journal_entries">
    <item quantity="one">%d wpis</item>
    <item quantity="few">%d wpisy</item>
    <item quantity="many">%d wpisów</item>
    <item quantity="other">%d wpisu</item>
</plurals>
```

**Japanisch/Chinesisch (nur 1 Form):**
```xml
<!-- values-ja/strings.xml -->
<plurals name="journal_entries">
    <item quantity="other">%d件のエントリー</item>
</plurals>
```

### Korrekter Abruf im Code

```kotlin
// KRITISCH: count wird ZWEIMAL uebergeben!
// 1. Mal → bestimmt die Plural-Kategorie (one/few/many/other)
// 2. Mal → wird in den %d-Platzhalter eingesetzt
val text = resources.getQuantityString(
    R.plurals.journal_entries,
    count,   // <-- Kategorie-Auswahl
    count    // <-- Wert fuer %d
)

// Jetpack Compose:
val text = pluralStringResource(R.plurals.journal_entries, count, count)
```

### Test-Matrix fuer Plurals

Diese Zahlen MUESSEN getestet werden, um Plural-Fehler zu finden:

| Zahl | Kritisch fuer | Warum |
|------|-------------|-------|
| 0 | Arabisch (zero), Franzoesisch (one!) | Arabisch hat eigene zero-Form |
| 1 | Fast alle (one) | Singular |
| 2 | Arabisch (two), Walisisch | Dualform |
| 4 | Russisch/Polnisch (few-Grenze) | Letzte Zahl vor dem Wechsel zu many |
| 5 | Russisch/Polnisch (many) | Erster Wert in many |
| 11 | Russisch (many, NICHT one!) | Obwohl auf 1 endend: Ausnahme! |
| 12 | Polnisch (many, NICHT few!) | Obwohl auf 2 endend: Ausnahme! |
| 21 | Russisch (one!) | %10=1 und %100≠11, also one |
| 100 | Arabisch (other) | %100=0, also other |

---

## 6. Uebersetzer-Kommentare

### Warum Kommentare entscheidend sind

Ohne Kontext koennen Uebersetzer nicht wissen:
- **Wo** der String erscheint (Kontext bestimmt Grammatik und Ton)
- **Was** die Platzhalter bedeuten
- **Wie lang** der Text sein darf
- **Ob** ein Wort mehrdeutig ist

### Kommentar-Format

Der Kommentar muss **unmittelbar VOR** dem `<string>`-Element stehen.
Alle grossen Uebersetzungsplattformen (Crowdin, Phrase, Lokalise, Transifex)
importieren diesen Kommentar als Kontext-Beschreibung.

```xml
<!-- Shown on the journal entry screen below the title. Max 60 characters. -->
<string name="journal_label_entry_count">Du hast %1$d Eintraege</string>

<!-- Button to start a new voice recording. Keep short — fits on a 100dp button. -->
<string name="journal_button_record">Aufnehmen</string>

<!-- Error shown when saving fails. %1$s = error reason from server (e.g., "Timeout") -->
<string name="journal_error_save_failed">Speichern fehlgeschlagen: %1$s</string>

<!-- Dialog title for entry deletion. %1$s = entry title (short text). -->
<string name="journal_dialog_title_delete">"%1$s" loeschen?</string>
```

### Was ein guter Kommentar enthaelt (Prioritaetsreihenfolge)

| Element | Warum wichtig | Beispiel |
|---------|--------------|---------|
| **Wo** der String erscheint | Kontext fuer Grammatik/Ton | `"Shown on the journal entry screen"` |
| **Platzhalter-Erklaerung** | `%1$s` allein ist nichtssagend | `"%1$s = username, %2$d = entry count"` |
| **Zeichenbegrenzung** | Platz variiert nach Sprache | `"Max 40 chars — fits in header bar"` |
| **Ton/Register** | Formell oder informal? | `"Use informal tone (du, not Sie)"` |
| **Mehrdeutigkeit aufklaeren** | Gleiche Woerter, andere Bedeutung | `"'book' = Buch (Nomen), nicht 'buchen' (Verb)"` |
| **Satzzeichen-Anforderungen** | Manche Sprachen vermeiden `...` | `"Keep '...' at the end"` |

### Wann KEIN Kommentar noetig ist

- `all_button_ok` → "OK" ist universell klar
- `app_name` → wird nie uebersetzt
- Langer Fliesstext der sich selbst erklaert

---

## 7. Nicht-uebersetzbare Strings

### `translatable="false"` fuer permanente Ausnahmen

```xml
<!-- App-Name: NIEMALS uebersetzen -->
<string name="app_name" translatable="false">Best Journal</string>

<!-- Firebase-Keys -->
<string name="firebase_project_id" translatable="false">bestjournal-prod</string>

<!-- URLs -->
<string name="url_privacy_policy" translatable="false">https://example.com/privacy</string>

<!-- Format-Templates die nur im Code verwendet werden -->
<string name="date_format_iso" translatable="false">yyyy-MM-dd</string>

<!-- Analytics Event Names -->
<string name="event_journal_created" translatable="false">journal_entry_created</string>
```

### Eigene Datei: `donottranslate.xml` (Google-Methode)

Google verwendet im Android Framework eine eigene Datei fuer alle nicht-uebersetzbaren Strings.
Android Lint behandelt ALLE Strings in einer Datei namens `donottranslate.xml` automatisch
als nicht uebersetzbar — ohne dass `translatable="false"` gesetzt werden muss.

```xml
<!-- res/values/donottranslate.xml -->
<resources>
    <string name="app_name">Best Journal</string>
    <string name="firebase_project_id">bestjournal-prod</string>
    <string name="support_email">support@bestjournal.app</string>
    <string name="date_format_iso">yyyy-MM-dd</string>
</resources>
```

### `tools:ignore="MissingTranslation"` vs `translatable="false"`

| Attribut | Verwendung |
|----------|-----------|
| `translatable="false"` | String wird NIEMALS uebersetzt. Erscheint nicht in Uebersetzungstools. |
| `tools:ignore="MissingTranslation"` | String existiert, fehlt aber absichtlich in manchen Sprachen. Selten noetig — deutet meist auf ein strukturelles Problem hin. |

---

## 8. String Arrays

### Wann Arrays verwenden

Arrays sind gut fuer **feste, unveraenderliche Listen** die immer zusammen auftreten:

```xml
<!-- Stimmungs-Optionen im Mood-Picker -->
<string name="mood_option_terrible">Furchtbar</string>
<string name="mood_option_bad">Schlecht</string>
<string name="mood_option_neutral">Neutral</string>
<string name="mood_option_good">Gut</string>
<string name="mood_option_great">Super</string>

<string-array name="mood_options">
    <item>@string/mood_option_terrible</item>
    <item>@string/mood_option_bad</item>
    <item>@string/mood_option_neutral</item>
    <item>@string/mood_option_good</item>
    <item>@string/mood_option_great</item>
</string-array>
```

### KRITISCH: Immer Referenzen statt direkten Text verwenden

```xml
<!-- GEFAEHRLICH — direkter Text im Array -->
<string-array name="mood_options">
    <item>Furchtbar</item>   <!-- Fehlt in values-fr/? -->
    <item>Schlecht</item>    <!-- → Kompletter Menuepunkt VERSCHWINDET! -->
    <item>Neutral</item>     <!-- Kein Fallback auf Default! -->
</string-array>

<!-- SICHER — Verweise auf einzelne Strings -->
<string-array name="mood_options">
    <item>@string/mood_option_terrible</item>  <!-- Fallback auf values/ moeglich -->
    <item>@string/mood_option_bad</item>
    <item>@string/mood_option_neutral</item>
</string-array>
```

**Warum:** Wenn ein Array-Item in einer Uebersetzung fehlt, verschwindet der gesamte
Eintrag — ohne Fallback auf die Default-Sprache. Bei Referenzen auf `@string/` greift
der normale Fallback-Mechanismus.

---

## 9. HTML und Styled Text in Strings

### Einfaches HTML

```xml
<!-- Funktioniert direkt mit setText() -->
<string name="welcome_message">Willkommen bei <b>Best Journal</b>!</string>
```

### HTML mit Format-Specifiern: CDATA verwenden

```xml
<!-- CDATA noetig wenn HTML + Platzhalter kombiniert werden -->
<string name="welcome_with_name"><![CDATA[Willkommen, <b>%1$s</b>!]]></string>
```

```kotlin
// Verwendung im Code:
val htmlText = getString(R.string.welcome_with_name, username)
val spanned = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
textView.text = spanned
```

### Annotation-Spans fuer uebersetzbare Links (fortgeschritten)

```xml
<!-- values/strings.xml -->
<string name="terms_text">
    Mit dem Fortfahren stimmst du unseren
    <annotation link="terms">Nutzungsbedingungen</annotation>
    und unserer
    <annotation link="privacy">Datenschutzerklaerung</annotation> zu.
</string>
```

```kotlin
// Annotations zur Laufzeit verarbeiten
val spanned = getText(R.string.terms_text) as SpannedString
val annotations = spanned.getSpans(0, spanned.length, Annotation::class.java)
for (ann in annotations) {
    when (ann.value) {
        "terms" -> { /* ClickableSpan setzen */ }
        "privacy" -> { /* ClickableSpan setzen */ }
    }
}
```

**Vorteil:** Uebersetzer koennen die Reihenfolge der Links aendern und den
sichtbaren Text uebersetzen — ohne HTML-Tags beschaedigen zu koennen.

### Unterstuetzte HTML-Tags in Android Strings

`<b>`, `<i>`, `<u>`, `<strike>`, `<sup>`, `<sub>`, `<big>`, `<small>`,
`<font color="#...">`, `<br>`, `<a href="...">`

---

## 10. Textexpansion und Layout-Design

### Expansionsfaktoren nach Sprache

| Sprache | Expansion vs. Englisch | Extrembeispiel |
|---------|----------------------|----------------|
| **Deutsch** | +35 bis +70% | "skip" (4) → "ueberspringen" (13) |
| **Finnisch** | +30 bis +60% | Agglutination erzeugt sehr lange Woerter |
| **Polnisch** | +25 bis +65% | Kasus-Endungen verlaengern Woerter erheblich |
| **Russisch** | +20 bis +35% | Kyrillisch braucht oft mehr Buchstaben |
| **Franzoesisch** | +15 bis +25% | Artikel und Praepositionen |
| **Spanisch** | +20 bis +25% | Aehnlich wie Franzoesisch |
| **Arabisch** | +25% | Zusaetzlich RTL-Spiegelung! |
| **Japanisch** | -30 bis -50% | Kanji kodieren viel Information in wenig Zeichen |
| **Chinesisch** | -30 bis -50% | Wie Japanisch |
| **Koreanisch** | -10 bis -20% | Hangul ist kompakt |

**Faustregel: +40% Puffer** in allen Layouts einplanen.

### Das deutsche Spezial-Problem: Komposita

"Settings" = 8 Zeichen → "Einstellungen" = 13 Zeichen
"Notification Settings" = 21 Zeichen → "Benachrichtigungseinstellungen" = 30 Zeichen

Buttons und Tab-Labels brechen regelmaessig bei deutschen Uebersetzungen.

### Layout-Regeln fuer i18n

**1. `wrap_content` statt fixer Breiten:**
```xml
<!-- FALSCH — bricht bei langen Uebersetzungen -->
<Button android:layout_width="120dp" />

<!-- RICHTIG — Button waechst mit dem Text -->
<Button
    android:layout_width="wrap_content"
    android:minWidth="120dp" />
```

**2. Hoehe nie fest setzen bei Text:**
```xml
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"   <!-- NIE feste Hoehe! -->
    android:maxLines="3"
    android:ellipsize="end" />
```

**3. Pseudolokalisierung zum Testen aktivieren:**
```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug {
            isPseudoLocalesEnabled = true
        }
    }
}
```

Dann im Emulator:
- **`English (XA)`**: Simuliert +30-40% Textexpansion, markiert hardcodierte Strings
- **`AR (XB)`**: Simuliert RTL-Layout

---

## 11. RTL-Sprachen (Arabisch, Hebraeisch, Farsi, Urdu)

### Manifest-Konfiguration (PFLICHT)

```xml
<application android:supportsRtl="true" ...>
```

Ohne dieses Flag ignoriert Android alle RTL-relevanten Layout-Attribute.

### Layout-Attribute ersetzen

Jedes `left`/`right` muss auf `start`/`end` umgestellt werden:

| Alt (bricht in RTL) | Neu (funktioniert ueberall) |
|---------------------|---------------------------|
| `android:paddingLeft` | `android:paddingStart` |
| `android:paddingRight` | `android:paddingEnd` |
| `android:layout_marginLeft` | `android:layout_marginStart` |
| `android:layout_marginRight` | `android:layout_marginEnd` |
| `android:gravity="left"` | `android:gravity="start"` |
| `android:gravity="right"` | `android:gravity="end"` |

### Drawables spiegeln (oder nicht!)

```xml
<!-- Icons die gespiegelt werden MUESSEN (Pfeile, Navigation): -->
<bitmap android:src="@drawable/arrow_back" android:autoMirrored="true" />

<!-- Icons die NICHT gespiegelt werden duerfen: -->
<!-- Logos, Mediensteuerung (Play/Pause), Warenkorb, Checkmarks -->
```

### Bidirektionaler Text (arabische Zahlen in RTL-Text)

```xml
<!-- LRM (Left-to-Right Mark) vor LTR-Inhalt in RTL-Text -->
<string name="contact_info">اتصل بنا: \u200E+49 123 456789\u200E</string>
```

| Steuerzeichen | Funktion |
|--------------|----------|
| `\u200E` (LRM) | Erzwingt LTR-Richtung fuer den folgenden Text |
| `\u200F` (RLM) | Erzwingt RTL-Richtung fuer den folgenden Text |

---

## 12. Problematische Sprachen und Grammatik-Fallen

### Tuerkisch: Vokalharmonie und Agglutination

Tuerkisch haengt grammatische Beziehungen als Suffixe an den Wortstamm.
Die Suffixe aendern sich je nach Vokalen im Wort (Vokalharmonie).

```
"In Settings" = "Ayarlar'da" (ein einziges Wort!)
"In BestJournal" = "BestJournal'da"
```

**Konsequenz:** `"In " + getString(R.string.app_name)` funktioniert auf Tuerkisch
NICHT — das "in" wird zum Suffix. Immer vollstaendige Saetze als Strings.

### Finnisch: 15 grammatische Kasus

Das gleiche Substantiv hat 15 verschiedene Formen:
- Nominativ: `kamera`
- Genitiv: `kameran`
- Inessiv (in etwas): `kamerassa`
- Elativ (aus etwas): `kamerasta`
- Illativ (zu etwas): `kameraan`

Strings wie `"Oeffne in %s"` funktionieren nicht — der Name muss im richtigen
Fall stehen, was eine sprachspezifische Endung erfordert.

### Japanisch: Zaelwoerter (Counter Words)

Japanisch hat verschiedene Zaehlsuffixe abhaengig von der Art des Objekts:
- Flache Objekte: `枚` (mai) — Fotos, Blaetter
- Lange Objekte: `本` (hon/bon/pon) — Stifte, Flaschen
- Kleine Tiere: `匹` (hiki)
- Buecher: `冊` (satsu)

`"%d Fotos"` → `"%d枚の写真"` — der Zaehler muss im String stehen.

### Koreanisch: Honorifik-System

3 Hoeflichkeitsstufen aendern die gesamte Verbendung und viele Vokabeln.
Die Stufe muss fuer die gesamte App einheitlich sein — nicht pro String variieren.

### Hindi/Bengalisch: Genus-Kongruenz

Adjektive und Verben muessen mit dem grammatischen Geschlecht des Subjekts
uebereinstimmen. `"Geloescht"` ist auf Hindi entweder `"हटाया गया"` (maskulin)
oder `"हटाई गई"` (feminin) — je nach dem Genus des geloeschten Objekts.

### Arabisch: Character Shaping

Arabische Buchstaben aendern ihre Form je nach Position im Wort
(Anfang/Mitte/Ende/isoliert). Bei manuellem Text-Rendering (Canvas)
muss eine Reshaping-Library verwendet werden.

---

## 13. ICU MessageFormat fuer Gender und komplexe Muster

### Wann ICU MessageFormat verwenden

ICU MessageFormat ist noetig wenn:
- Strings geschlechtsspezifisch formuliert werden muessen
- Plural UND Gender kombiniert werden muessen
- Ordinalzahlen (1., 2., 3.) benoetigt werden

### API-Verfuegbarkeit

| API | Ab Android Version | Dependency |
|-----|-------------------|------------|
| `android.icu.text.MessageFormat` | API 24 (Android 7.0) | Keine — eingebaut |
| `androidx.core.i18n.MessageFormat` | API 14+ | `androidx.core:core-i18n:1.0.0-alpha02` |

### Beispiele

**Gender Select:**
```kotlin
import android.icu.text.MessageFormat

val pattern = "{gender, select, female {Sie hat} male {Er hat} other {Person hat}} einen Eintrag erstellt."
val result = MessageFormat(pattern).format(mapOf("gender" to "female"))
// → "Sie hat einen Eintrag erstellt."
```

**Plural:**
```kotlin
val pattern = "Du hast {count, plural, one {# Eintrag} other {# Eintraege}}."
val result = MessageFormat(pattern, Locale.GERMAN).format(mapOf("count" to 5))
// → "Du hast 5 Eintraege."
```

**Ordinal (1., 2., 3.):**
```kotlin
val pattern = "{rank, selectordinal, one {#st} two {#nd} few {#rd} other {#th}} place"
// Englisch: 1st, 2nd, 3rd, 4th
```

**Kombiniert: Gender + Plural:**
```kotlin
val pattern = """{gender, select,
    female {{count, plural, one {Sie hat # Eintrag} other {Sie hat # Eintraege}}}
    male   {{count, plural, one {Er hat # Eintrag} other {Er hat # Eintraege}}}
    other  {{count, plural, one {# Eintrag} other {# Eintraege}}}
}"""

val result = MessageFormat(pattern, Locale.GERMAN).format(
    mapOf("gender" to "female", "count" to 3)
)
// → "Sie hat 3 Eintraege"
```

### ICU-Strings in strings.xml speichern

```xml
<!-- ICU-Pattern als normaler String, wird per MessageFormat formatiert -->
<string name="msg_user_entries">{gender, select, female {Sie hat {count, plural, one {# Eintrag} other {# Eintraege}}} male {Er hat {count, plural, one {# Eintrag} other {# Eintraege}}} other {{count, plural, one {# Eintrag} other {# Eintraege}}}}</string>
```

```kotlin
val pattern = getString(R.string.msg_user_entries)
val result = android.icu.text.MessageFormat(pattern, Locale.GERMAN)
    .format(mapOf("gender" to "female", "count" to 3))
```

---

## 14. Jetpack Compose und i18n

### `stringResource()` vs `getString()`

```kotlin
// In Composable: stringResource() — rekomposiert bei Locale-Aenderung
@Composable
fun MyScreen() {
    Text(text = stringResource(R.string.journal_title))

    // Mit Argumenten:
    Text(text = stringResource(R.string.greeting, userName))
}

// Ausserhalb von Composables: Context.getString()
val text = context.getString(R.string.journal_title)
```

### Plurals in Compose

```kotlin
// Plural MIT Formatierung (count ZWEIMAL!)
Text(
    text = pluralStringResource(R.plurals.journal_entries, count, count)
)

// Plural mit mehreren Argumenten
Text(
    text = pluralStringResource(R.plurals.cart_summary, itemCount, itemCount, totalPrice)
)
```

### RTL in Compose

Compose unterstuetzt RTL automatisch ueber `LayoutDirection`. Kein manuelles
Spiegeln noetig fuer Standard-Layouts. Voraussetzung: `android:supportsRtl="true"`
im Manifest.

---

## 15. Dateiorganisation bei grossen Apps

### Wann aufteilen?

Ab ca. 200 Strings wird eine einzelne `strings.xml` unuebersichtlich. Android
merged alle XML-Dateien in `res/values/` automatisch — die Aufteilung beeinflusst
NUR die Entwickler-Erfahrung, nicht die App.

### Empfohlene Struktur

```
res/values/
  strings.xml               <-- app_name + globale Strings
  strings_common.xml         <-- Shared Actions, Errors, A11y Labels
  strings_journal.xml        <-- Journal-Screen
  strings_settings.xml       <-- Settings-Screen
  strings_onboarding.xml     <-- Onboarding-Flow
  strings_dashboard.xml      <-- Dashboard-Screen
  strings_recording.xml      <-- Recording-Feature
  donottranslate.xml         <-- Alle nicht-uebersetzbaren Strings
```

### Zuordnungsregel

| Wohin | Welche Strings |
|-------|---------------|
| `strings.xml` | Nur `app_name` und Strings die von >3 Features genutzt werden |
| `strings_common.xml` | `all_button_ok`, `all_error_network`, `cd_back_button` |
| `strings_<feature>.xml` | Alles was einem Screen/Feature gehoert |
| `donottranslate.xml` | URLs, Firebase-IDs, Brand-Namen, Format-Templates |

### Multi-Module-Projekte

In Gradle-Multi-Module-Projekten bekommt jedes Feature-Modul seine eigene
`res/values/strings.xml`. Das Core-UI-Modul haelt die gemeinsamen Strings.

```
:core-ui/res/values/strings.xml          <-- Shared: Actions, Errors, A11y
:feature-journal/res/values/strings.xml  <-- Nur Journal
:feature-settings/res/values/strings.xml <-- Nur Settings
```

---

## 16. Anti-Patterns — was NIEMALS gemacht werden darf

### 1. String-Konkatenation (HAEUFIGSTER Fehler!)

```kotlin
// BRICHT in jeder nicht-englischen Sprache
val msg = "Willkommen " + userName + "!"           // FALSCH
val msg = getString(R.string.welcome) + " " + name // FALSCH

// RICHTIG
val msg = getString(R.string.welcome_user, userName)
```

### 2. Plural-Logik im Code

```kotlin
// FALSCH — bricht in Russisch, Arabisch, Polnisch
val text = if (count == 1) "1 Eintrag" else "$count Eintraege"

// RICHTIG
val text = resources.getQuantityString(R.plurals.entries, count, count)
```

### 3. Hardcodierte Wortstellungs-Annahmen

```kotlin
// FALSCH: Nimmt an Adjektiv kommt vor Substantiv (stimmt nicht in Hebraeisch)
"$count neue Nachrichten"

// RICHTIG: Vollstaendiger String mit Platzhaltern
getString(R.string.new_messages_count, count)
```

### 4. UI-Element-Namen im String

```xml
<!-- FALSCH: "Einstellungen" koennte in einer anderen Sprache anders heissen -->
<string name="instruction">Tippe auf den Einstellungen-Button</string>

<!-- RICHTIG: Neutral formulieren oder Platzhalter nutzen -->
<string name="instruction">Tippe auf %1$s um fortzufahren</string>
```

### 5. Datum/Zeit/Waehrung hardcodieren

```kotlin
// FALSCH: Format ist kulturspezifisch
getString(R.string.posted_on, "17.04.2026")

// RICHTIG: Locale-aware Formatierung
val formatted = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(date)
getString(R.string.posted_on, formatted)
```

### 6. Brand-Namen in strings.xml ohne Schutz

```xml
<!-- FALSCH: Uebersetzer koennten den Brand-Namen uebersetzen -->
<string name="about">Powered by Whisper AI</string>

<!-- RICHTIG: Brand in xliff:g schuetzen -->
<string name="about">
    Powered by <xliff:g id="brand" example="Whisper AI">Whisper AI</xliff:g>
</string>
```

### 7. Fehlende `other`-Pluralform

Wenn `other` in einem `<plurals>`-Block fehlt, crasht die App zur Laufzeit.
`other` ist die EINZIGE Pflicht-Kategorie.

### 8. Falsches Verzeichnis-Naming fuer Regionen

```
res/values-pt-BR/    <-- FALSCH (Android ignoriert das still!)
res/values-pt-rBR/   <-- RICHTIG (kleines r ist Pflicht)
```

### 9. Strings im ViewModel resolven

```kotlin
// FALSCH: Lifecycle-Probleme, nicht testbar
class MyViewModel {
    val text = context.getString(R.string.title) // FALSCH!
}

// RICHTIG: String-ID uebergeben, UI resolved
class MyViewModel {
    val titleResId = R.string.title // Int-ID
}
// In Compose:
Text(text = stringResource(viewModel.titleResId))
```

### 10. Accessibility-Strings vergessen

```kotlin
// FALSCH: Nicht lokalisierbar
contentDescription = "share"

// RICHTIG
contentDescription = getString(R.string.cd_share_button)
```

---

## 17. Automatische Qualitaetspruefung (Lint und CI)

### Wichtige Android Lint Checks

| Lint-ID | Bedeutung | Schwere |
|---------|-----------|---------|
| `HardcodedText` | Hardcodierter Text in XML statt `@string/` | Warning |
| `MissingTranslation` | String in values/ fehlt in values-xx/ | Error |
| `MissingQuantity` | `<plurals>` fehlt benoetigte Kategorie | Error |
| `SetTextI18n` | `textView.text = "Hallo"` direkt im Code | Warning |
| `ExtraTranslation` | Uebersetzung vorhanden, aber kein Default-String | Warning |
| `UnusedResources` | String definiert aber nie referenziert | Warning |

### Lint verschaerfen in build.gradle.kts

```kotlin
android {
    lint {
        error("MissingTranslation")
        error("HardcodedText")
        warning("MissingQuantity")
        htmlReport = true
        htmlOutput = file("build/reports/lint-results.html")
        abortOnError = true
    }
}
```

### Default-Locale im strings.xml angeben

```xml
<resources xmlns:tools="http://schemas.android.com/tools"
    xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
    tools:locale="de">
```

Das teilt Lint mit: "Deutsch ist die Basis-Sprache." `MissingTranslation` bewertet
dann relativ zu Deutsch.

### Platzhalter-Konsistenz pruefen (CI-Script)

```python
import xml.etree.ElementTree as ET
import glob, re, sys

def get_placeholders(text):
    return set(re.findall(r'%\d+\$[sd]', text or ''))

errors = []
default = {}
tree = ET.parse('app/src/main/res/values/strings.xml')
for elem in tree.findall('.//string[@name]'):
    default[elem.get('name')] = get_placeholders(elem.text)

for f in glob.glob('app/src/main/res/values-*/strings.xml'):
    locale = f.split('values-')[1].split('/')[0]
    tree = ET.parse(f)
    for elem in tree.findall('.//string[@name]'):
        name = elem.get('name')
        if name in default:
            found = get_placeholders(elem.text)
            if found != default[name]:
                errors.append(f"[{locale}] {name}: expected {default[name]}, got {found}")

if errors:
    print('\n'.join(errors))
    sys.exit(1)
else:
    print('All placeholders consistent.')
```

### resConfigs — nur unterstuetzte Sprachen einschliessen

```kotlin
android {
    defaultConfig {
        // Nur diese Sprachen ins APK bauen:
        resourceConfigurations += setOf(
            "de", "en", "fr", "es", "hi", "ar", "zh", "ja",
            "ko", "pt", "ru", "tr", "it", "nl"
        )
    }
}
```

Ohne `resConfigs` enthaelt das APK Uebersetzungen aus ALLEN Bibliotheken
(Google Play Services hat 80+ Sprachen), was die Fallback-Logik brechen kann.

---

## 18. Tooling und Uebersetzungsplattformen

### Empfohlene Plattformen

| Plattform | Staerke | Preis |
|-----------|---------|-------|
| **Crowdin** | Beste GitHub-Integration, Android Studio Plugin | Free fuer Open Source |
| **Lokalise** | Professionelle Translation Teams | Ab $120/Monat |
| **Phrase** (ehemals PhraseApp) | Enterprise mit TMS-Unterstuetzung | Ab $25/Monat |
| **Transifex** | Grosse Community-Uebersetzungen | Free Tier verfuegbar |
| **POEditor** | Einfach, guenstig | Ab $15/Monat |

### Crowdin GitHub Action (Empfehlung)

```yaml
name: Crowdin Sync
on:
  push:
    branches: [main]
    paths: ['app/src/main/res/values/strings.xml']

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: crowdin/github-action@v2
        with:
          upload_sources: true
          download_translations: true
          create_pull_request: true
          pull_request_title: 'New translations from Crowdin'
```

### Wie die Grossen es machen

| Unternehmen | Ansatz |
|-------------|--------|
| **Meta** | Eigenes FBT-Framework + Language Packs (on-demand Download) |
| **Netflix** | `app-localization.json` + AWS Lambda Transformers, Uebersetzungen kommen als PR |
| **Airbnb** | Eigene i18n-Plattform mit lokalen Caches, 62 Sprachen |
| **Google** | Android Studio Translations Editor + Partner-TMS |

---

## 19. Per-App Language Preferences (In-App Sprachwechsel)

### Konfiguration (API 33+, backward-compatible via AppCompat 1.6+)

**1. locale_config.xml erstellen:**
```xml
<!-- res/xml/locale_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="de"/>
    <locale android:name="en"/>
    <locale android:name="ja-JP"/>
    <locale android:name="pt-BR"/>
    <locale android:name="ar"/>
    <locale android:name="hi"/>
    <locale android:name="zh-Hans"/>
</locale-config>
```

**2. Im Manifest registrieren:**
```xml
<application android:localeConfig="@xml/locale_config">
```

**3. Sprache wechseln (backward-compatible):**
```kotlin
val appLocale = LocaleListCompat.forLanguageTags("de")
AppCompatDelegate.setApplicationLocales(appLocale)
// Compose rekomposiert automatisch — kein Context-Override noetig
```

---

## 20. Checkliste vor der Uebersetzung

| # | Pruefpunkt | Methode |
|---|-----------|---------|
| 1 | Alle Strings in `res/values/`? | Android Lint: `HardcodedText` |
| 2 | `tools:locale` im `<resources>`-Tag gesetzt? | Manuell pruefen |
| 3 | Alle `<plurals>` haben `other`? | Lint: `MissingQuantity` |
| 4 | Nummerierte Platzhalter (`%1$s`) ueberall? | `grep '%s' strings.xml` (ohne Zahl = Fehler) |
| 5 | `<xliff:g>` fuer alle Platzhalter mit `id` + `example`? | Manuell pruefen |
| 6 | Kommentare fuer mehrdeutige Strings? | Code Review |
| 7 | `translatable="false"` fuer URLs, Brand-Namen, Keys? | Manuell pruefen |
| 8 | Keine fixen Button-Breiten (`dp`)? | Layout Inspector |
| 9 | String Arrays nutzen Referenzen, nicht direkten Text? | `grep '<item>' strings.xml` |
| 10 | `supportsRtl="true"` im Manifest? | AndroidManifest.xml pruefen |
| 11 | `start`/`end` statt `left`/`right` in Layouts? | Lint / globale Suche |
| 12 | Pseudolocale `en-XA` getestet (Layout-Overflow)? | Emulator |
| 13 | Pseudolocale `ar-XB` getestet (RTL-Layout)? | Emulator |
| 14 | `resConfigs` auf unterstuetzte Sprachen eingeschraenkt? | build.gradle.kts |
| 15 | `isPseudoLocalesEnabled = true` in Debug-Build? | build.gradle.kts |

---

## 21. Vollstaendiges Beispiel: Best-Practice strings.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
    xmlns:tools="http://schemas.android.com/tools"
    tools:locale="de">

    <!-- ═══════════════════════════════════════════════════
         APP-WEITE STRINGS
         ═══════════════════════════════════════════════════ -->

    <!-- App name — do not translate -->
    <string name="app_name" translatable="false">Best Journal</string>

    <!-- Generic OK confirmation button -->
    <string name="all_button_ok">OK</string>
    <!-- Generic cancel button — keep short, max 10 chars -->
    <string name="all_button_cancel">Abbrechen</string>
    <!-- Generic loading indicator text -->
    <string name="all_loading">Laden\u2026</string>
    <!-- Generic network error — shown as a Snackbar -->
    <string name="all_error_network">Keine Internetverbindung.</string>


    <!-- ═══════════════════════════════════════════════════
         JOURNAL SCREEN
         ═══════════════════════════════════════════════════ -->

    <!-- Main screen title in the top app bar -->
    <string name="journal_title_screen">Mein Tagebuch</string>

    <!-- Empty state: shown when user has no entries yet. \n = line break. -->
    <string name="journal_label_empty_state">Noch keine Eintraege.\nTippe auf + um deinen ersten zu schreiben.</string>

    <!-- Entry count shown below the header.
         %1$d = total number of entries (integer).
         Keep %1$d — it will be replaced by the actual number. -->
    <string name="journal_label_entry_count">
        Du hast <xliff:g id="count" example="42">%1$d</xliff:g> Eintraege
    </string>

    <!-- Save button in the entry editor. Fits on a 100dp button. -->
    <string name="journal_button_save">Speichern</string>

    <!-- Delete confirmation dialog title.
         %1$s = entry title (a short text string, e.g. "Mein Tag"). -->
    <string name="journal_dialog_title_delete">
        \u201c<xliff:g id="entry_title" example="Mein Tag">%1$s</xliff:g>\u201d loeschen?
    </string>

    <!-- Delete confirmation dialog message — keep concise, 1-2 sentences max. -->
    <string name="journal_dialog_message_delete">Dieser Eintrag wird dauerhaft geloescht. Das kann nicht rueckgaengig gemacht werden.</string>

    <!-- Toast shown after successful save -->
    <string name="journal_toast_saved">Eintrag gespeichert.</string>

    <!-- Error shown when saving fails.
         %1$s = error reason from server (e.g., "Timeout"). -->
    <string name="journal_error_save_failed">
        Speichern fehlgeschlagen: <xliff:g id="error_reason" example="Timeout">%1$s</xliff:g>
    </string>

    <!-- Accessibility: content description for the floating action button (microphone) -->
    <string name="journal_desc_mic_button">Sprachaufnahme starten</string>


    <!-- ═══════════════════════════════════════════════════
         PLURALS
         ═══════════════════════════════════════════════════ -->

    <!-- Entry count for plural display. %d = count.
         Languages with complex plural rules (Arabic, Russian, Polish)
         need additional quantity items in their locale files.
         Always pass count twice: getQuantityString(R.plurals.x, count, count) -->
    <plurals name="journal_entries">
        <item quantity="one">%d Eintrag</item>
        <item quantity="other">%d Eintr\u00e4ge</item>
    </plurals>

    <!-- Recording duration in seconds -->
    <plurals name="recording_seconds">
        <item quantity="one">%d Sekunde</item>
        <item quantity="other">%d Sekunden</item>
    </plurals>


    <!-- ═══════════════════════════════════════════════════
         STRING ARRAYS (always use references, never inline text!)
         ═══════════════════════════════════════════════════ -->

    <!-- Mood options shown in the mood picker (order: worst to best) -->
    <string name="mood_option_terrible">Furchtbar</string>
    <string name="mood_option_bad">Schlecht</string>
    <string name="mood_option_neutral">Neutral</string>
    <string name="mood_option_good">Gut</string>
    <string name="mood_option_great">Super</string>

    <string-array name="mood_options">
        <item>@string/mood_option_terrible</item>
        <item>@string/mood_option_bad</item>
        <item>@string/mood_option_neutral</item>
        <item>@string/mood_option_good</item>
        <item>@string/mood_option_great</item>
    </string-array>

</resources>
```

---

## Quellen

### Offizielle Dokumentation
- [Android String Resources](https://developer.android.com/guide/topics/resources/string-resource)
- [Localize your app](https://developer.android.com/guide/topics/resources/localization)
- [Test with pseudolocales](https://developer.android.com/guide/topics/resources/pseudolocales)
- [Resources in Compose](https://developer.android.com/develop/ui/compose/resources)
- [Per-App Language Preferences](https://android-developers.googleblog.com/2022/11/per-app-language-preferences-part-1.html)

### Unicode & CLDR
- [CLDR Language Plural Rules](https://www.unicode.org/cldr/charts/42/supplemental/language_plural_rules.html)
- [ICU MessageFormat](https://unicode-org.github.io/icu/userguide/format_parse/messages/)

### Engineering Blogs
- [Meta: Language Packs for Android](https://engineering.fb.com/2022/05/09/android/language-packs/)
- [Airbnb: Internationalization Platform](https://medium.com/airbnb-engineering/building-airbnbs-internationalization-platform-45cf0104b63c)
- [Netflix: Streaming In Your Language](https://netflixtechblog.medium.com/now-streaming-in-your-language-the-technology-behind-netflixs-global-interface-e0c732c69b16)
- [Duolingo: Spanish Localization Inclusivity](https://blog.duolingo.com/how-duolingo-keeps-its-spanish-localization-inclusive-2/)

### Best Practice Guides
- [Phrase: Ultimate Guide to Android Localization](https://phrase.com/blog/posts/best-practices-for-android-localization-revisited-and-expanded/)
- [Phrase: Jetpack Compose i18n](https://phrase.com/blog/posts/internationalizing-jetpack-compose-android-apps/)
- [Lokalise: ICU MessageFormat Guide](https://lokalise.com/blog/complete-guide-to-icu-message-format/)
- [SimpleLocalize: Android Strings Guide](https://simplelocalize.io/blog/posts/android-strings-localization/)
- [Jeroen Mols: XML Resource Naming](https://jeroenmols.com/blog/2016/03/07/resourcenaming/)
- [Localazy: How to Provide Comments](https://localazy.com/docs/android/how-to-provide-comments-for-strings)

### Lint & Tooling
- [MissingTranslation Lint Check](https://googlesamples.github.io/android-custom-lint-rules/checks/MissingTranslation.md.html)
- [MissingQuantity Lint Check](https://googlesamples.github.io/android-custom-lint-rules/checks/MissingQuantity.md.html)
- [Crowdin GitHub Action](https://github.com/crowdin/github-action)
