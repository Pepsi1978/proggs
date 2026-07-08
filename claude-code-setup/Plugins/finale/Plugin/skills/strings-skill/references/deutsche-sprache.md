# Deutsche Sprache für App-Strings — Vollständige Referenz

**Zweck:** Diese Datei ist die autoritative Regel-Quelle für alle deutschen
String-Ressourcen in Android-Apps. Sie wird vom `string-extraktor`-Skill in
Phase 0 geladen und danach bei jeder Erstellungs- und Verifikations-Aktion
konsultiert.

**Primärsprache:** Alle Apps werden zuerst auf **Deutsch** entwickelt. Deutsche
Strings sind der Master — aus ihnen werden alle anderen Sprachen übersetzt.
Qualität der deutschen Originale entscheidet über Qualität aller Übersetzungen.

**Gliederung:**
- **Teil A:** Orthographie & Typografie (Umlaute, ß, Anführungszeichen, Striche, Substantive, Rechtschreibung, Zeichensetzung, Abkürzungen)
- **Teil B:** UX & Stil (Du/Sie, Gendern, Kompositum-Länge, System-Strings, Button-Labels, Fehlermeldungen, Anglizismen)
- **Teil C:** Zahlen, Datum, Zeit, Währung, Einheiten, Plurals, Listen, Regional (DE/AT/CH)
- **Teil D:** Kotlin-Utility & Kombinierte Checkliste

---

# Teil A: Orthographie & Typografie

## A.1 Umlaute und ß

### Grundregel: ß vs. ss

Die Rechtschreibreform 1996 hat die Regel vereinfacht und strikt gemacht:

| Kontext | Schreibung | Beispiele |
|---|---|---|
| Nach **langem Vokal** | **ß** | Straße, Fuß, Maß, reißen, beißen |
| Nach **kurzem Vokal** | **ss** | Fluss, Hass, Riss, dass, muss |
| Nach **Diphthong** (au, ei, eu…) | **ß** | draußen, heiß, Strauß |
| **Schweiz & Liechtenstein** | immer **ss** | Strasse, Fuss, Masse |

**Merksatz:** Klingt der Vokal kurz → ss. Klingt er lang → ß.

```xml
<!-- DO -->
<string name="label_street">Straße</string>
<string name="hint_that">dass du…</string>
<string name="msg_must">Du musst…</string>

<!-- DON'T (alte Schreibung vor 1996) -->
<string name="hint_that_old">daß du…</string>     <!-- veraltet -->
<string name="msg_must_old">Du mußt…</string>     <!-- veraltet -->
```

### Großbuchstaben: Ä/Ö/Ü und ẞ

- **Ä/Ö/Ü** im Fließtext und Überschriften: immer direkt — **nicht** AE/OE/UE
- **Ausnahme AE/OE/UE:** nur in technischen Identifikatoren ohne Umlaut-Unterstützung (URLs, Dateinamen, Login-Namen) — nie in sichtbaren App-Strings
- **Großes ẞ (U+1E9E):** seit 29. Juni 2017 offiziell. Verwendung z. B. in STRAẞE. `toUpperCase()` handhabt das ab API 26 automatisch

### Android strings.xml: Direkt oder Escape?

| Zeichen | Empfehlung | Begründung |
|---|---|---|
| ä, ö, ü, ß | **Direkt** in UTF-8 | Keine XML-Sonderzeichen; `encoding="utf-8"` ist Standard |
| &, <, > | Escape: `&amp;` `&lt;` `&gt;` | XML-reserviert |
| ' (Apostroph) | `\'` oder String in `"…"` | Android-Pflicht |
| " (gerades Zitat) | `\"` | Android-Pflicht |
| Typogr. Zeichen | `\u201E` etc. oder direkt | Beide Methoden korrekt |

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Umlaute direkt — kein Escape nötig -->
    <string name="title_settings">Einstellungen öffnen</string>
    <string name="msg_save">Änderungen gespeichert</string>
    <!-- Apostroph-Escape -->
    <string name="hint_user">Das ist Hans\' Profil</string>
</resources>
```

**KRITISCH:** NIEMALS Unicode-Escapes wie `\u00dc` statt `Ü` im Kotlin-Code verwenden. Das ist eine verbotene Workaround-Form, wenn im Code versucht wird Umlaute ohne UTF-8-Konfiguration zu verwenden. Solche Escapes müssen IMMER als String-Ressource extrahiert werden (siehe Phase 1.3b im Haupt-Skill).

## A.2 Deutsche Anführungszeichen und Typografie

### Anführungszeichen-Typen

| Typ | Zeichen | Unicode | Verwendung |
|---|---|---|---|
| **Deutsch (korrekt)** | „…" | U+201E / U+201C | Standard für alle deutschen App-Texte |
| Einfach deutsch | ‚…' | U+201A / U+2018 | Zitat im Zitat |
| Amerikanisch (falsch in DE) | "…" | U+0022 | Nur in Code/Englisch |
| Französisch (Guillemets) | «…» / »…« | — | Nicht für Standard-Deutsch |

**Eselsbrücke:** „99 unten, 66 oben" — das öffnende Zeichen sitzt **unten**, das schließende **oben**.

```xml
<!-- DO: typografisch korrektes Zitat -->
<string name="msg_quote">Er sagte: „Willkommen zurück."</string>

<!-- DON'T: Schreibmaschinen-Anführungszeichen -->
<string name="msg_quote_bad">Er sagte: "Willkommen zurück."</string>
```

### Striche: Gedankenstrich vs. Bindestrich

| Strich | Zeichen | Unicode | Leerzeichen | Verwendung |
|---|---|---|---|---|
| **Bindestrich** | - | U+002D | keins | Komposita: E-Mail, Co-Pilot, Vor- und Nachname |
| **Gedankenstrich** | – | U+2013 | **davor und danach** | Einschübe: „Er – ihr großes Vorbild – kam." |
| **Bis-Strich** | – | U+2013 | **keins** | Spannen: 9–17 Uhr, 100–200 €, Mo.–Fr. |

```xml
<!-- Gedankenstrich als Einschub (mit Leerzeichen) -->
<string name="msg_thought">Das System – sehr einfach – startet jetzt.</string>

<!-- Bis-Strich ohne Leerzeichen -->
<string name="label_hours">9–17 Uhr</string>
<string name="label_range">Montag–Freitag</string>

<!-- NICHT Bindestrich als Gedankenstrich missbrauchen -->
<string name="label_bad">Montag - Freitag</string>    <!-- DON'T -->
```

### Weitere typografische Zeichen

| Zeichen | Unicode | Verwendung | strings.xml |
|---|---|---|---|
| Auslassungspunkte … | U+2026 | Ladevorgang, abgebrochener Text | `\u2026` oder direkt |
| Typogr. Apostroph ' | U+2019 | Hans' Profil, 90er-Jahre | `\u2019` oder direkt |
| Geschütztes Leerzeichen | U+00A0 | Vor Einheiten, in Abkürzungen | `\u00A0` oder `&#160;` |
| Bedingter Trennstrich | U+00AD | Lange Komposita | `\u00AD` oder `&#173;` |

```xml
<!-- Auslassungspunkte: 1 Zeichen, nicht 3 Punkte -->
<string name="status_loading">Wird geladen\u2026</string>   <!-- DO -->
<string name="status_loading_bad">Wird geladen...</string>  <!-- DON'T -->

<!-- Apostroph typografisch -->
<string name="label_genitive">Hans\u2019 Profil</string>    <!-- DO -->

<!-- Geschütztes Leerzeichen vor Einheit -->
<string name="label_size">100\u00A0MB</string>              <!-- DO: kein Umbruch -->
```

## A.3 Substantiv-Großschreibung

### Die Kernregel

**Alle Substantive groß — immer, auch in Überschriften und Buttons.**
Im Deutschen gilt **KEINE Title Case** wie im Englischen. Adjektive und Verben
bleiben in Überschriften **klein**.

| Korrekt (DE) | Falsch (engl. Title Case) |
|---|---|
| Einstellungen speichern | Einstellungen Speichern |
| Konto verwalten | Konto Verwalten |
| Neue Nachricht erstellen | Neue Nachricht Erstellen |
| Das Speichern der Daten | Das Speichern Der Daten |

### Substantivierte Infinitive

Wenn ein Verb durch Artikel oder Kontext zum Substantiv wird: **groß**.

```xml
<!-- DO: Substantivierte Infinitive groß -->
<string name="label_save_action">das Speichern</string>
<string name="dialog_delete_title">Löschen bestätigen</string>
<!-- "Löschen" = Substantiv (groß), "bestätigen" = Verb (klein) -->

<!-- Adjektive in Überschriften: klein -->
<string name="section_header">Neue Beiträge</string>
<!-- "Neue" = Adjektiv (klein), "Beiträge" = Substantiv (groß) -->
```

### Höflichkeitsanrede (Sie-Form)

**Sie / Ihnen / Ihr** — immer groß, auch in App-Dialogen.

```xml
<!-- DO -->
<string name="dialog_confirm">Möchten Sie fortfahren?</string>
<string name="label_your_account">Ihr Konto</string>

<!-- DON'T -->
<string name="dialog_confirm_bad">Möchten sie fortfahren?</string>
```

## A.4 Neue Deutsche Rechtschreibung (seit 1996/2006)

| Alt (vor 1996) | Neu (ab 1996/seit 2006) | Regel |
|---|---|---|
| daß | **dass** | Konjunktion: kurzer Vokal → ss |
| muß, mußt | **muss, musst** | kurzer Vokal → ss |
| Haß, Fluß | **Hass, Fluss** | kurzer Vokal → ss |
| Schiffahrt | **Schifffahrt** | Dreifachkonsonant bleibt |
| Tip | **Tipp** | Anpassung |

Weitere Liberalisierung bei Getrennt-/Zusammenschreibung ("kennenlernen" oder
"kennen lernen" — beide gültig, aber **Konsistenz innerhalb der App ist Pflicht**).

## A.5 Zeichensetzung

### Punkt am Ende: Wann ja, wann nein?

| Element | Punkt? | Beispiel |
|---|---|---|
| Vollständiger Satz in Dialog/Toast | **JA** | „Ihre Daten wurden gespeichert." |
| Button-Beschriftung | **NEIN** | „Speichern" |
| Menü-Label / Navigation | **NEIN** | „Einstellungen" |
| Überschrift / Titel | **NEIN** | „Konto verwalten" |
| Hilfetext / Erklärung (mehrsätzig) | **JA** | „Hier findest du deine Nachrichten. Tippe auf eine, um sie zu öffnen." |
| Placeholder / Hint | **NEIN** | „Benutzername eingeben" |

### Ausrufezeichen

Sparsam einsetzen — Ausrufezeichen wirken in Apps **aggressiv oder aufdringlich**.

```xml
<!-- Nur bei echten Warnungen / Erfolgen -->
<string name="success_saved">Gespeichert!</string>       <!-- akzeptabel, selten -->
<string name="warning_delete">Achtung: Löschen kann nicht rückgängig gemacht werden.</string>

<!-- DON'T: Ausrufezeichen inflationär -->
<string name="loading_bad">Einen Moment!</string>
```

### Leerzeichen-Regeln

| Regel | DO | DON'T |
|---|---|---|
| Kein Leerzeichen VOR Satzzeichen | `Möchten Sie löschen?` | `Möchten Sie löschen ?` |
| Kein Leerzeichen INNERHALB Klammern | `(optional)` | `( optional )` |
| Kein Leerzeichen vor Doppelpunkt | `Status: Aktiv` | `Status : Aktiv` |
| Leerzeichen um Gedankenstrich (Einschub) | `Text – Einschub – Rest` | `Text–Einschub–Rest` |
| Kein Leerzeichen um Bis-Strich | `9–17 Uhr` | `9 – 17 Uhr` |

## A.6 Abkürzungen in Apps

### Wann erlaubt, wann nie?

| Kontext | Abkürzungen? | Begründung |
|---|---|---|
| Button-Text | **NIE** | Muss ohne Nachdenken verständlich sein |
| Dialog-Titel | **NIE** | Zu wichtig für Mehrdeutigkeit |
| Kurze Label (Platz!) | **mit Vorsicht** | Nur Standard-Kürzel, immer mit Tooltip |
| Statusleiste / Chips | **erlaubt** | Platzmangel ist begründet |
| Tooltips / Erklärtext | **erlaubt** | Platz vorhanden, trotzdem sparsam |

### Standard-Abkürzungen (Duden-konform)

| Abkürzung | Ausgeschrieben | Leerzeichen-Regel |
|---|---|---|
| z. B. | zum Beispiel | Leerzeichen (geschützt) zwischen z. und B. |
| d. h. | das heißt | Leerzeichen (geschützt) zwischen d. und h. |
| u. a. | unter anderem | Leerzeichen zwischen u. und a. |
| evtl. | eventuell | kein inneres Leerzeichen |
| ca. | circa | kein inneres Leerzeichen |
| usw. | und so weiter | kein inneres Leerzeichen |
| bzw. | beziehungsweise | kein inneres Leerzeichen |

```xml
<!-- Geschütztes Leerzeichen in Abkürzungen verhindert Zeilenumbruch -->
<string name="hint_example">z.\u00A0B. einen Benutzernamen</string>
```

### Einheiten: Leerzeichen und Schreibweise

Zwischen Zahl und Einheit kommt immer ein **geschütztes Leerzeichen**:

```xml
<string name="label_size_mb">100\u00A0MB</string>
<string name="label_size_gb">1,5\u00A0GB</string>
<string name="label_percent">75\u00A0%</string>
<string name="label_temp">22\u00A0°C</string>
```

### Technische Abkürzungen: immer Großbuchstaben

```xml
<string name="label_url">URL eingeben</string>       <!-- nicht "Url" -->
<string name="label_api">API-Schlüssel</string>      <!-- nicht "Api" -->
<string name="label_pdf">Als PDF exportieren</string> <!-- nicht "als Pdf" -->
<string name="label_id">Benutzer-ID</string>         <!-- nicht "Benutzer-Id" -->
```

---

# Teil B: UX & Stil

## B.1 Du vs. Sie — die wichtigste Entscheidung

Die Wahl der Anredeform ist die **erste und verbindlichste** Entscheidung.
Sie gilt für die gesamte App, ausnahmslos.

### Entscheidungsmatrix

| Kontext | Anrede | Beispiele |
|---|---|---|
| Social Media, Messaging | **Du** | WhatsApp, Instagram, Threads |
| Streaming, Entertainment | **Du** | Spotify, Netflix, YouTube Music |
| Fitness, Sport, Health | **Du** | Nike, Strava, Freeletics |
| Spiele | **Du** | Immer Du — kein Spiel siezt den Spieler |
| Lern-Apps (Jugendliche) | **Du** | Duolingo, Khan Academy DE |
| E-Commerce (modern) | **Du** | Zalando, About You |
| Reise (Consumer) | **Du** | HRS, Booking.com DE |
| Banking, Finanzen | **Sie** | Sparkasse, Commerzbank, ING |
| Versicherung | **Sie** | Allianz, HUK, Signal Iduna |
| Medizin, Gesundheit (offiziell) | **Sie** | Arzt-Apps, Krankenhaus-Portale |
| Behörden, Verwaltung | **Sie** | ELSTER, Bürgerdienste |
| B2B, Enterprise | **Sie** | SAP, Datev, Business-Tools |
| Ältere Zielgruppen (60+) | **Sie** | Auch Consumer, oder Option |
| Öffentlicher Verkehr | **Sie** | DB Navigator |

### Goldene Regeln

**Sie wird IMMER großgeschrieben** — auch mitten im Satz:

```xml
<!-- RICHTIG -->
<string name="onboarding_welcome">Willkommen! Hier verwalten Sie Ihr Konto.</string>
<string name="error_login">Ihre Anmeldedaten sind ungültig.</string>

<!-- FALSCH — "sie" klein ist 3. Person Plural, nicht Anrede! -->
<string name="error_login_bad">ihre Anmeldedaten sind ungültig.</string>
```

**Mischung ist TABU** — nie mal Du und mal Sie in derselben App.

**Default für moderne Apps:** Wenn unklar → **Du** ist der moderne Standard
(2020er). Sie nur bei nachweislich konservativer Zielgruppe oder
branchengegebener Formalität.

## B.2 Gendergerechte Sprache — Strategie-Rangfolge

### Strategie 1: Partizip-Substantive (EMPFOHLEN — sauberste Lösung)

Keine Sonderzeichen, barrierefrei, Screenreader-kompatibel:

```xml
<string name="role_learner">Lernende</string>
<string name="role_teacher">Lehrende</string>
<string name="role_user">Nutzende</string>        <!-- statt "Nutzer" -->
<string name="role_participant">Teilnehmende</string>
<string name="role_admin">Administrierende</string>
<string name="label_author">Verfassende</string>
```

### Strategie 2: Geschlechtsneutrale Substantive (EMPFOHLEN)

```xml
<string name="label_account">Konto</string>         <!-- statt "Benutzerkonto" -->
<string name="label_person">Person</string>
<string name="label_team">Team</string>
<string name="label_contact">Kontakt</string>
<string name="label_member">Mitglied</string>
<string name="label_staff">Personal</string>
```

### Strategie 3: Doppelpunkt-Notation (nur Fließtext, nie Button-Labels)

Akzeptiert von German UPA, Screenreader macht kurze Pause (barrierefrei):

```xml
<!-- OK in beschreibenden Texten -->
<string name="empty_state_users">Noch keine Nutzer:innen registriert.</string>

<!-- NICHT in Button-Labels (zu lang, Sonderzeichen) -->
<string name="btn_add">Person hinzufügen</string>    <!-- statt "Nutzer:in hinzufügen" -->
```

### Strategie 4: Gender-Stern (nur wenn Zielgruppe das explizit erwartet)

```xml
<string name="label_users">Nutzer*innen</string>
<!-- Problem: Screenreader liest "*" als "Stern" — schlechte Barrierefreiheit -->
```

### Barrierefreiheits-Hinweis (Blinden- und Sehbehindertenverband — BSV)

> **Wichtig fuer Accessibility:** Der Blinden- und Sehbehindertenverband
> empfiehlt **Vollformen** (`Fahrerinnen und Fahrer`) als die screenreader-
> kompatibelste Variante — werden von TalkBack/VoiceOver korrekt vorgelesen.
> Sonderzeichen (`*`, `:`, `_`) erzeugen Sprachpausen oder werden falsch
> ausgesprochen ("Stern", "Doppelpunkt").

**Prioritaeten-Reihenfolge fuer maximale Barrierefreiheit:**

1. **Vollformen** — screenreader-perfekt, aber **2-3x laenger** → nicht fuer Buttons/Tabs
2. **Partizip-Substantive** (`Nutzende`, `Lernende`) — screenreader-perfekt UND kurz → **bevorzugt fuer App-Strings**
3. **Geschlechtsneutrale Substantive** (`Person`, `Mitglied`) — screenreader-perfekt
4. Doppelpunkt-Notation — akzeptable Kompromiss-Loesung im Fliesstext
5. Gender-Stern — nur wenn Zielgruppe das explizit erwartet, Accessibility-Kompromiss

**Praktische Faustregel:** Partizip + Neutral als Default, Vollformen in
beschreibenden Texten wo Laenge nicht stoert (Onboarding, About-Texte, Hilfe).

### DO / DON'T-Tabelle

| DON'T | DO | Strategie |
|---|---|---|
| `Benutzer` | `Nutzende` oder `Person` | Partizip / Neutral |
| `Entwickler` | `Entwickelnde` | Partizip |
| `BenutzerIn` | `Nutzende` | Partizip (Binnen-I ist veraltet) |
| `Benutzer und Benutzerinnen` | `Nutzende` | Partizip (kürzer) |
| `Nutzer/-in` | `Person` oder `Mitglied` | Neutral |
| `Der Nutzer kann…` | `Nutzende können…` | Partizip Plural |

**Regel für Button-Labels:** Genderzeichen (`*`, `:`, `_`) NIE in Button-Labels —
stören TalkBack und andere Screenreader. Immer auf neutrale Formen ausweichen.

## B.3 Deutsche Kompositum-Länge — UI-Problem Nr. 1

Deutsch ist strukturell **30–40 %** wortlänger als Englisch. Kein Fehler — Sprache.

### Längenvergleich

| Englisch | Deutsch | Längenzuwachs |
|---|---|---|
| Settings | Einstellungen | +75 % |
| Notifications | Benachrichtigungen | +100 % |
| Skip | Überspringen | +125 % |
| Privacy Policy | Datenschutzerklärung | +90 % |
| Payment Methods | Zahlungsmethoden | +50 % |
| Account Settings | Kontoeinstellungen | +30 % |
| Forgot Password | Passwort vergessen | +40 % |

### Empfohlene Maximallängen (Android-Komponenten)

| Komponente | Max. Zeichen | Begründung |
|---|---|---|
| Tab-Label | 12 | Tabs wachsen sonst über den Bildschirm |
| Button (primär) | 20 | Passt in Standard-Button-Breite |
| TopAppBar-Titel | 25 | Ellipsis bei Überschreitung |
| MenuItem | 30 | Drawer-Breite ist begrenzt |
| Notification-Titel | 40 | Android kürzt danach ab |
| Chip / Badge | 15 | Kleinformat |
| SnackBar | 60 | Eine Zeile auf 360dp-Gerät |

### Strategien gegen Längenprobleme

**1. Soft-Hyphen für erzwungenen Umbruch (PFLICHT bei Komposita >18 Zeichen — siehe Beispiele unten):**

```xml
<string name="label_notifications">Benach\u00ADrichtigungen</string>
<string name="label_privacy">Daten\u00ADschutz\u00ADerklärung</string>
<!-- \u00AD wird nur sichtbar, wenn Umbruch nötig -->
```

**Pflichtkandidaten fuer Soft-Hyphen in deutschen Apps:**

| Wort | Empfohlene Trennung |
|------|--------------------|
| Datenschutzerklärung | Daten\u00ADschutz\u00ADerklärung |
| Nutzungsbedingungen | Nutzungs\u00ADbedingungen |
| Benachrichtigungseinstellungen | Benach\u00ADrichtigungs\u00ADeinstellungen |
| Zahlungsmethoden | Zahlungs\u00ADmethoden |
| Kontoeinstellungen | Konto\u00ADeinstellungen |
| Sicherheitsüberprüfung | Sicherheits\u00ADüberprüfung |
| Anmeldedaten | Anmelde\u00ADdaten |

**Setz-Regel:** Soft-Hyphen zwischen Wortbestandteilen, nicht mitten im Wort.
Sinnvolle Silbengrenze (z.B. `Benach-richtigungen`), nicht `Be-nachrichtigungen`.

**2. Kürzere Synonyme bevorzugen:**

| Zu lang | Kürzer |
|---|---|
| Benachrichtigungen | Mitteilungen |
| Benutzerkontoeinstellungen | Konto |
| Informationen | Infos |
| Datenschutzerklärung | Datenschutz |

**3. Bindestriche zur Lesbarkeit bei langen Komposita:**

```xml
<string name="label_payment">Zahlungs-Einstellungen</string>
<!-- statt "Zahlungseinstellungen" — lesbarer ab 4 Silben -->
```

**4. Compose: Ellipsis und autoSize:**

```kotlin
Text(
    text = stringResource(R.string.label_payment_method),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    style = MaterialTheme.typography.labelMedium
)
```

**5. Kein fixer Container für Text:**

```xml
<!-- NIE: android:layout_width="160dp" bei deutschen Button-Texten -->
<!-- IMMER: wrap_content mit minWidth -->
<Button
    android:layout_width="wrap_content"
    android:minWidth="88dp"
    android:text="@string/btn_confirm_payment" />
```

## B.4 Android-System-Standard-Strings

Android liefert system-seitig übersetzte Strings mit.

### Verfügbare `android.R.string`-Konstanten (DE)

| Konstante | Deutsche Übersetzung | Verwendung |
|---|---|---|
| `android.R.string.ok` | OK | Generische Bestätigung |
| `android.R.string.cancel` | Abbrechen | Dialog abbrechen |
| `android.R.string.yes` | Ja | Ja/Nein (selten empfohlen) |
| `android.R.string.no` | Nein | Ja/Nein (selten empfohlen) |
| `android.R.string.copy` | Kopieren | Kontextmenüs |
| `android.R.string.cut` | Ausschneiden | Kontextmenüs |
| `android.R.string.paste` | Einfügen | Kontextmenüs |
| `android.R.string.selectAll` | Alles auswählen | Kontextmenüs |
| `android.R.string.search_go` | Los | Suchfeld-Action |
| `android.R.string.untitled` | \<Ohne Titel\> | Namenslose Inhalte |

### Wann eigene Strings vs. System-Strings?

```kotlin
// System-String: generische UI-Elemente
AlertDialog.Builder(context)
    .setPositiveButton(android.R.string.ok) { _, _ -> confirm() }
    .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }

// Eigener String: spezifische Aktion (BESSER für Dialoge!)
AlertDialog.Builder(context)
    .setPositiveButton(R.string.btn_delete_entry) { _, _ -> delete() }
    .setNegativeButton(R.string.btn_keep_entry) { _, _ -> dismiss() }
```

**Faustregel:** System-Strings für generisch (Kopieren, Einfügen, OK).
Eigene spezifische Strings für Aktions-Dialoge — "Löschen" + "Behalten"
ist klarer als "OK" + "Abbrechen".

### Konsistente System-Begriffe (eigene Strings, aber systemkonsistent)

Diese Begriffe kennen Nutzer aus dem Android-System:

```xml
<string name="action_back">Zurück</string>
<string name="action_next">Weiter</string>
<string name="action_done">Fertig</string>
<string name="action_edit">Bearbeiten</string>
<string name="action_share">Teilen</string>
<string name="action_delete">Löschen</string>
<string name="action_save">Speichern</string>
<string name="action_search">Suchen</string>
<string name="action_close">Schließen</string>
<string name="action_open">Öffnen</string>
<string name="action_add">Hinzufügen</string>
<string name="action_remove">Entfernen</string>  <!-- bei nicht-destruktiven Aktionen -->
```

## B.5 Button-Label-Regeln für Deutsch

### Grundregeln

| Regel | DON'T | DO |
|---|---|---|
| Infinitiv, kein Imperativ | `Speichere!` | `Speichern` |
| Aktionsorientiert | `Bestätigen` | `Zahlung bestätigen` |
| Max. 2 Wörter (primär) | `Jetzt sofort kaufen und bezahlen` | `Jetzt kaufen` |
| Kein Punkt am Ende | `Weiter.` | `Weiter` |
| Konsistenz in der App | mal `Löschen`, mal `Entfernen` | immer `Löschen` |
| Keine Navigation-Redundanz | `Weiter →` | `Weiter` |
| Keine vagen Labels | `Ja`/`Nein` im Lösch-Dialog | `Löschen`/`Behalten` |
| Kein "Hier klicken" | `Hier klicken um fortzufahren` | `Fortfahren` |

### Button-Strings-Muster

```xml
<!-- Primäre Aktionen — spezifisch und aktionsorientiert -->
<string name="btn_confirm_payment">Zahlung bestätigen</string>
<string name="btn_create_account">Konto erstellen</string>
<string name="btn_delete_entry">Eintrag löschen</string>
<string name="btn_send_message">Nachricht senden</string>
<string name="btn_upload_photo">Foto hochladen</string>
<string name="btn_start_trial">Kostenlos testen</string>

<!-- Sekundäre Aktionen — kürzer -->
<string name="btn_cancel">Abbrechen</string>
<string name="btn_skip">Überspringen</string>
<string name="btn_back">Zurück</string>
<string name="btn_next">Weiter</string>
<string name="btn_done">Fertig</string>

<!-- Dialoge — IMMER spezifisch, nie "Ja/Nein" -->
<string name="dialog_delete_confirm">Löschen</string>
<string name="dialog_delete_cancel">Behalten</string>
<string name="dialog_logout_confirm">Abmelden</string>
<string name="dialog_logout_cancel">Abbrechen</string>
<string name="dialog_discard_confirm">Verwerfen</string>
<string name="dialog_discard_cancel">Weiterbearbeiten</string>

<!-- Onboarding / CTAs -->
<string name="btn_get_started">Los geht\'s</string>
<string name="btn_sign_in">Anmelden</string>
<string name="btn_sign_up">Registrieren</string>
```

## B.6 Fehlermeldungen auf Deutsch

### Vier Pflicht-Eigenschaften (German UPA 2024)

| Eigenschaft | Bedeutung |
|---|---|
| **Nützlich** | Erkläre was passiert ist und wie es zu beheben ist |
| **Verständlich** | Sprache der Zielgruppe, kein Techniker-Jargon |
| **Knapp** | Ein Problem, eine Erklärung, eine Lösung |
| **Empathisch** | Kein Vorwurf, keine Großbuchstaben, kein Ausrufezeichen |

### DO / DON'T

| DON'T | DO |
|---|---|
| `Fehler!` | `Verbindung unterbrochen` |
| `Ein Fehler ist aufgetreten.` | `Anmeldung fehlgeschlagen.` |
| `Falsche Eingabe.` | `Bitte gib eine gültige E-Mail-Adresse ein.` |
| `Fehler 401: Unauthorized` | `Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.` |
| `Die Verbindung zum Server konnte nicht hergestellt werden.` | `Keine Internetverbindung. Bitte prüfe dein WLAN oder Mobile Daten.` |
| `PASSWORT FALSCH!` | `Das Passwort stimmt leider nicht.` |

### Fehlermeldungs-Vorlagen (Du-Form)

```xml
<!-- Netzwerk -->
<string name="error_no_internet">Keine Internetverbindung. Bitte prüfe dein WLAN oder deine mobilen Daten.</string>
<string name="error_timeout">Die Verbindung hat zu lange gedauert. Bitte versuche es erneut.</string>
<string name="error_server">Unsere Server sind gerade nicht erreichbar. Wir kümmern uns darum.</string>

<!-- Authentifizierung -->
<string name="error_login_failed">Anmeldung fehlgeschlagen. Bitte prüfe deine E-Mail-Adresse und dein Passwort.</string>
<string name="error_session_expired">Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.</string>
<string name="error_email_invalid">Bitte gib eine gültige E-Mail-Adresse ein.</string>
<string name="error_password_too_short">Das Passwort muss mindestens 8 Zeichen lang sein.</string>

<!-- Formulare -->
<string name="error_field_required">Bitte füll dieses Feld aus.</string>
<string name="error_date_invalid">Bitte gib ein gültiges Datum ein.</string>
<string name="error_phone_invalid">Bitte gib eine gültige Telefonnummer ein.</string>

<!-- Aktionen -->
<string name="error_upload_failed">Das Foto konnte nicht hochgeladen werden. Bitte versuche es erneut.</string>
<string name="error_delete_failed">Löschen fehlgeschlagen. Bitte versuche es erneut.</string>
<string name="error_save_failed">Speichern fehlgeschlagen. Deine Änderungen wurden nicht gesichert.</string>

<!-- Limits -->
<string name="error_limit_reached">Du hast das maximale Limit erreicht.</string>
<string name="error_file_too_large">Die Datei ist zu groß. Maximal %1$s MB erlaubt.</string>

<!-- Generisch (Fallback) -->
<string name="error_generic">Etwas ist schiefgelaufen. Bitte versuche es erneut.</string>
<string name="error_generic_with_retry">Etwas ist schiefgelaufen. Bitte versuche es erneut oder starte die App neu.</string>
```

### Sie-Variante (nur konsistent wenn App Sie verwendet)

```xml
<string name="error_login_failed_sie">Anmeldung fehlgeschlagen. Bitte prüfen Sie Ihre E-Mail-Adresse und Ihr Passwort.</string>
```

## B.7 Deutsche Wörter vs. Anglizismen

### Entscheidungsregel

Verwende das **deutsche Wort**, wenn es:
- den meisten Nutzern bekannt ist
- kürzer oder gleich lang ist
- natürlich klingt (kein Amtsdeutsch)

Behalte den **Anglizismus**, wenn er:
- vollständig eingebürgert ist ("App", "Browser", "Link")
- das deutsche Wort künstlich klingt
- ein etablierter Fachbegriff ist ("Cookie", "Update")

### Referenztabelle

| Englisch | Deutsch (bevorzugt) | Verdict |
|---|---|---|
| Login / Sign In | **Anmelden** | Immer deutsch |
| Sign Up / Register | **Registrieren** | Immer deutsch |
| Sign Out / Logout | **Abmelden** | Immer deutsch |
| Settings | **Einstellungen** | Immer deutsch |
| Share | **Teilen** | Immer deutsch |
| Search | **Suchen** | Immer deutsch |
| Delete | **Löschen** | Immer deutsch |
| Save | **Speichern** | Immer deutsch |
| Download | **Herunterladen** oder Download | Beide akzeptiert |
| Upload | **Hochladen** oder Upload | Beide akzeptiert |
| Update | Update (oder „Aktualisieren") | Eingebürgert |
| App | App | Vollständig eingebürgert |
| Link | Link | Vollständig eingebürgert |
| Browser | Browser | Vollständig eingebürgert |
| Cookie | Cookie | Kein deutsches Äquivalent |
| Notification | **Benachrichtigung** / **Mitteilung** | Deutsch bevorzugen |
| Profile | **Profil** | Eingebürgert |
| Chat | Chat | Eingebürgert |
| Feed | Feed / Neuigkeiten | Beide OK |
| Story | Story (Social Media) | Eingebürgert |

### Absolutes Denglisch-TABU

```xml
<!-- VERBOTEN -->
<string name="status_shared_bad">geshared</string>
<string name="status_liked_bad">geliked</string>
<string name="status_downloaded_bad">downgeloaded</string>
<string name="action_updaten_bad">App updaten</string>
<string name="action_canceln_bad">canceln</string>
<string name="action_forwarden_bad">forwarden</string>

<!-- RICHTIG -->
<string name="status_shared">Geteilt</string>
<string name="status_downloaded">Heruntergeladen</string>
<string name="action_update">App aktualisieren</string>
<string name="action_forward">Weiterleiten</string>
```

---

# Teil C: Zahlen, Datum, Zeit, Währung, Plurals, Listen

## C.1 Zahlenformat (DIN 5008:2020-03)

### Kernregeln

| Regel | Falsch | Richtig |
|-------|--------|---------|
| Dezimaltrennzeichen | `3.14` | `3,14` |
| 4-stellige Zahl — kein Tausendertrenner | `1.000` | `1000` |
| 5+ Stellen — Leerzeichen als Trenner (DIN) | `50000` | `50 000` |
| Währung — Punkt als Trenner (Sicherheitsregel) | `10 000,00 €` | `10.000,00 €` |
| Postleitzahl — NIE Tausendertrenner | `8.0331` | `80331` |
| Jahreszahl — NIE Tausendertrenner | `2.026` | `2026` |

**Begründung:** DIN 5008 empfiehlt schmale Leerzeichen (U+202F) für normale
Zahlen ab 5 Stellen. Bei Geldbeträgen gilt der Punkt (Sicherheitsregel aus
Buchführung).

**Ausnahmen — NIE Tausendertrenner bei:**
- Postleitzahlen (`80331`)
- Jahreszahlen (`2026`)
- Geräte-IDs, Seriennummern, Telefonnummern
- Versionsnummern (`1.0.0` — hier ist Punkt Separator)

### Kotlin

```kotlin
import java.text.NumberFormat
import java.util.Locale

// Ganze Zahlen
val numFormat = NumberFormat.getNumberInstance(Locale.GERMAN)
val formatted = numFormat.format(50000)  // "50.000" (Java-API: Punkt-Trenner)

// Dezimalzahlen
val decFormat = NumberFormat.getNumberInstance(Locale.GERMAN).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
val pi = decFormat.format(3.14159)  // "3,14"
```

**Hinweis:** `Locale.GERMAN` und `Locale.GERMANY` liefern identische Formate.
Java nutzt intern den Punkt als Tausendertrenner für `de` — was dem
gängigen deutschen Software-Standard entspricht (nicht dem strengen
DIN-5008-Leerzeichen).

## C.2 Datumsformate

### Übersicht

| Stil | Format | Beispiel | Kontext |
|------|--------|---------|---------|
| DIN 5008 Standard | `TT.MM.JJJJ` | `17.04.2026` | Standard in DE/AT/CH |
| DIN 5008 Kurz | `TT.MM.JJ` | `17.04.26` | Kompaktansicht |
| Ausgeschrieben | `TT. Monat JJJJ` | `17. April 2026` | Formell, Detailseiten |
| Voll | `Wochentag, TT. Monat JJJJ` | `Freitag, 17. April 2026` | Header, Kalender |
| ISO 8601 | `JJJJ-MM-TT` | `2026-04-17` | Logs, Dateinamen, APIs |
| Kurzwochentag | — | `Mo, Di, Mi, Do, Fr, Sa, So` | Kalender-Kopfzeile |

**Wichtig:** `17. April` — **mit Leerzeichen nach dem Punkt**.
NIEMALS `17.ter April` oder `17ter April`.

### Android DateFormat (Java-API, ältere Code-Basis)

```kotlin
import java.text.DateFormat
import java.util.Date
import java.util.Locale

val date = Date()
val short = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN).format(date)
// → "17.04.26"

val medium = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(date)
// → "17.04.2026"

val long = DateFormat.getDateInstance(DateFormat.LONG, Locale.GERMAN).format(date)
// → "17. April 2026"

val full = DateFormat.getDateInstance(DateFormat.FULL, Locale.GERMAN).format(date)
// → "Freitag, 17. April 2026"
```

### DateTimeFormatter (java.time, API 26+) — bevorzugt

```kotlin
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val date = LocalDate.of(2026, 4, 17)

val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    .withLocale(Locale.GERMAN)
val result = date.format(formatter)  // "17. April 2026"

val iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)  // "2026-04-17"
```

## C.3 Zeitformate

| Regel | Falsch | Richtig |
|-------|--------|---------|
| Immer 24-Stunden-Format | `2:30 PM` | `14:30` |
| Trennzeichen | `14-30` | `14:30` |
| Mit Sekunden | — | `14:30:45` |
| Formell | — | `14:30 Uhr` |
| Kompakt (UI) | — | `14:30` |

### Layout-Erzwingung

```xml
<!-- 24h-Format in Layout erzwingen -->
<TextView
    android:format24Hour="HH:mm"
    android:format12Hour="@null" />
```

### strings.xml

```xml
<string name="time_with_uhr">%s Uhr</string>
<string name="time_range">%1$s – %2$s Uhr</string>
```

### Kotlin

```kotlin
val timeShort = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(date)
// → "14:30"
```

## C.4 Währungsformat (EUR)

| Regel | Falsch | Richtig |
|-------|--------|---------|
| Symbol kommt NACH dem Betrag | `€10,50` | `10,50 €` |
| Geschütztes Leerzeichen vor Symbol | `10,50€` | `10,50\u00A0€` |
| Immer 2 Nachkommastellen | `10 €` | `10,00 €` |
| Negativbetrag | `€-10,50` | `-10,50 €` |
| ISO-Code statt Symbol | — | `10,50 EUR` |
| Tausendertrenner | `10 000,00 €` | `10.000,00 €` |

### Kotlin

```kotlin
val currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMAN)
val result = currencyFormat.format(10.50)   // "10,50 €"
val large = currencyFormat.format(10000.0)  // "10.000,00 €"
```

### strings.xml

```xml
<string name="price_label">%1$s €</string>
<string name="price_free">Kostenlos</string>
<string name="price_from">Ab %1$s €</string>
```

## C.5 Maßeinheiten

### Regeln

| Einheit | Falsch | Richtig | Hinweis |
|---------|--------|---------|---------|
| Kilogramm | `5kg` | `5\u00A0kg` | NBSP |
| Kilometer | `100km` | `100\u00A0km` | NBSP |
| Megabyte | `200MB` | `200\u00A0MB` | NBSP |
| Temperatur | `20°C` | `20\u00A0°C` | NBSP vor °, kein Leerz. zwischen ° und C |
| Prozent (Duden) | `50%` | `50\u00A0%` | Duden empfiehlt Leerzeichen |
| Prozent (kompakt/Werbung) | — | `50%` | auch akzeptiert |

**Einheiten NICHT flektieren:** `5 kg` (nicht `5 kgs`), `3 km` (nicht `3 kms`).

### strings.xml

```xml
<string name="weight_kg">%d&#160;kg</string>
<string name="temperature_celsius">%d&#160;°C</string>
<string name="percentage">%d&#160;%%</string>
<string name="file_size_mb">%d&#160;MB</string>
<string name="distance_km">%d&#160;km</string>
```

### Kotlin (API 24+)

```kotlin
import android.icu.text.MeasureFormat
import android.icu.text.MeasureFormat.FormatWidth
import android.icu.util.Measure
import android.icu.util.MeasureUnit

val measureFormat = MeasureFormat.getInstance(Locale.GERMAN, FormatWidth.SHORT)
val weight = measureFormat.format(Measure(5, MeasureUnit.KILOGRAM))  // "5 kg"
val temp = measureFormat.format(Measure(20, MeasureUnit.CELSIUS))    // "20 °C"
```

## C.6 Plural-Formen in Android (Deutsch)

### CLDR-Regel für Deutsch

Deutsch hat genau **2 Plural-Kategorien**:

| Kategorie | Regel | Beispiele |
|-----------|-------|-----------|
| `one` | n = 1 | 1 Eintrag, 1 Tag, 1 km |
| `other` | alle anderen | 0 Einträge, 2 Tage, 21 km, 100 Einträge |

**Es gibt KEIN `zero`, `two`, `few`, `many` für Deutsch** — anders als
Arabisch (6 Formen), Russisch (4), Polnisch (4). `0 Einträge` fällt unter `other`.

**Stolperfalle:** Wer "Keine Einträge" für Fall 0 anzeigen möchte, braucht
einen eigenen **Empty-State-String** — NICHT `quantity="zero"` (wirkungslos für DE).

### strings.xml — Korrekte Plurals

```xml
<plurals name="entry_count">
    <item quantity="one">1 Eintrag</item>
    <item quantity="other">%d Einträge</item>
</plurals>

<!-- Mit Platzhalter (nummeriert) -->
<plurals name="items_selected">
    <item quantity="one">%1$d Element ausgewählt</item>
    <item quantity="other">%1$d Elemente ausgewählt</item>
</plurals>

<!-- Tage -->
<plurals name="days_remaining">
    <item quantity="one">Noch 1 Tag</item>
    <item quantity="other">Noch %d Tage</item>
</plurals>

<!-- Empty State separat — NICHT als quantity="zero" -->
<string name="entry_count_empty">Keine Einträge vorhanden</string>
```

### Kotlin

```kotlin
// count IMMER doppelt übergeben: 1x Plural-Auswahl, 1x %d
val text = resources.getQuantityString(R.plurals.entry_count, count, count)

// Compose
val text = pluralStringResource(R.plurals.entry_count, count, count)

// Beispiel mit 0:
// getQuantityString(R.plurals.entry_count, 0, 0) → "0 Einträge" (other)
// Für "Keine Einträge": getString(R.string.entry_count_empty)
```

## C.7 Listen und Aufzählungen

### Regeln

| Regel | Falsch | Richtig |
|-------|--------|---------|
| Oxford-Komma | `A, B, und C` | `A, B und C` |
| Aufzählungszeichen | `;` als Trenner | `•` oder `–` |
| Semikolon | `A; B; C` | Nur bei komplexen Teillisten |
| Satzzeichen am Ende | inkonsistent | Konsistent (alle mit Punkt oder alle ohne) |

**Oxford-Komma:** Kein Komma vor „und"/„oder". `Rot, Blau und Grün`.

### Android ListFormatter (API 30+)

```kotlin
import android.icu.text.ListFormatter

val formatter = ListFormatter.getInstance(Locale.GERMAN)
val result = formatter.format(listOf("Rot", "Blau", "Grün"))
// "Rot, Blau und Grün"
```

### strings.xml

```xml
<string name="list_separator">, </string>
<string name="list_last_separator"> und </string>
```

## C.8 Regionale Unterschiede (DE / AT / CH)

### Vergleichstabelle

| Merkmal | Deutschland (de-DE) | Österreich (de-AT) | Schweiz (de-CH) |
|---------|--------------------|--------------------|-----------------|
| Eszett | ß vorhanden | ß vorhanden | **Kein ß — immer ss** |
| Währung | EUR (€) | EUR (€) | **CHF** |
| Januar | Januar | **Jänner** (regional) | Januar |
| Datumsformat | TT.MM.JJJJ | TT.MM.JJJJ | TT.MM.JJJJ |
| Begrüßung (informell) | Hallo / Tschüss | Servus / Grüß Gott | Hoi / Tschüss |
| Tausendertrenner | `.` (Software) | `.` (Software) | `'` (Apostroph) |

### Schweiz-Besonderheiten

- `Strasse` statt `Straße`
- `Grüsse` statt `Grüße`
- `heiss` statt `heiß`
- Tausendertrenner typografisch: `1'000`
- In Software meist Punkt (konsistent mit DE)

### Android Resource-Verzeichnisse

```
res/
  values/           ← Fallback (Englisch)
  values-de/        ← Alle deutschsprachigen Regionen (DE + AT + CH)
  values-de-rDE/    ← Nur Deutschland (selten nötig)
  values-de-rAT/    ← Nur Österreich (regional: Jänner, Wortschatz)
  values-de-rCH/    ← Nur Schweiz (kein ß, CHF, andere Strings)
```

**Empfehlung:** `values-de` als Standard. `values-de-rCH` nur wenn
Schweiz-spezifische Strings nötig. `values-de-rAT` nur bei explizit
österreichischen Formulierungen.

### strings.xml — CH-Varianten

```xml
<!-- values-de/strings.xml -->
<string name="street_label">Straße</string>
<string name="currency_code">EUR</string>
<string name="greeting">Guten Tag!</string>

<!-- values-de-rCH/strings.xml (Schweiz) -->
<string name="street_label">Strasse</string>
<string name="currency_code">CHF</string>
<string name="greeting">Grüezi!</string>
```

---

# Teil D: Kotlin-Utility & Kombinierte Checkliste

## D.1 Komplette Kotlin-Formatter-Klasse

```kotlin
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit

/**
 * Zentraler Formatter fuer deutsche Locale.
 * Verwendet in der gesamten App statt manueller String-Bauerei.
 */
object GermanFormatter {
    private val locale = Locale.GERMAN

    // --- Zahlen ---
    fun number(value: Long): String =
        NumberFormat.getNumberInstance(locale).format(value)

    fun decimal(value: Double, decimals: Int = 2): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)

    // --- Waehrung ---
    fun currency(value: Double): String =
        NumberFormat.getCurrencyInstance(locale).format(value)

    // --- Datum ---
    fun dateShort(date: Date): String =
        DateFormat.getDateInstance(DateFormat.SHORT, locale).format(date)

    fun dateMedium(date: Date): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(date)

    fun dateLong(date: Date): String =
        DateFormat.getDateInstance(DateFormat.LONG, locale).format(date)

    fun dateFull(date: Date): String =
        DateFormat.getDateInstance(DateFormat.FULL, locale).format(date)

    // --- Zeit ---
    fun timeShort(date: Date): String =
        DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(date)

    // --- Masseinheiten (API 24+) ---
    fun kilogram(value: Number): String =
        MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT)
            .format(Measure(value, MeasureUnit.KILOGRAM))

    fun celsius(value: Number): String =
        MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT)
            .format(Measure(value, MeasureUnit.CELSIUS))
}
```

## D.2 Die 20 wichtigsten Regeln für deutsche App-Strings

Diese Checkliste wird in Phase 3 (CREATE) und Phase 4 (VERIFY) des
string-extraktor-Skills verwendet.

```
ORTHOGRAPHIE & TYPOGRAFIE
□ 1. ß nach langem Vokal / nach Diphthong; ss nach kurzem Vokal
     (Strasse/Straße je nach Land, dass/muss mit ss)
□ 2. Umlaute ä/ö/ü/ß direkt in UTF-8 — KEIN \u00dc-Escape im Code
□ 3. „Deutsche Anführungszeichen" (U+201E…U+201C) — nicht "..."
□ 4. Gedankenstrich – (U+2013) mit Leerzeichen für Einschübe,
     ohne für Bis-Spannen (9–17 Uhr)
□ 5. Substantive IMMER groß, auch in Überschriften
     (Einstellungen speichern, NICHT Einstellungen Speichern)
□ 6. Auslassungspunkte … als 1 Zeichen (\u2026), nicht drei Punkte

STIL & UX
□ 7. Du/Sie konsistent — Mischung ist TABU
□ 8. Sie/Ihnen/Ihr immer großschreiben (Anrede, nicht 3. Pers. Pl.)
□ 9. Gender-neutrale Formen bevorzugen (Partizip: Nutzende, Lernende)
□ 10. Keine Genderzeichen (*,:,_) in Button-Labels (Screenreader!)

BUTTONS & LABELS
□ 11. Buttons im Infinitiv, nicht Imperativ (Speichern, nicht Speichere!)
□ 12. Kein Punkt bei Buttons, Titeln, Labels, Menü-Einträgen
□ 13. Dialoge: spezifische Labels statt "Ja/Nein" (Löschen/Behalten)
□ 14. Technische Abkürzungen IMMER groß (URL, API, PDF, ID)
□ 15. Max. Längen beachten (Tab: 12, Button: 20, AppBar: 25, Menu: 30)

FORMATE
□ 16. Dezimal: Komma (3,14) — NIE Punkt
□ 17. Datum: TT.MM.JJJJ (17.04.2026) oder ausgeschrieben
□ 18. Zeit: 24h-Format (14:30), nie 12h mit PM
□ 19. Währung: "10,50 €" (Symbol NACH Betrag, mit geschütztem Leerz.)
□ 20. Plurals: NUR one + other für Deutsch, kein zero/few/many
```

---

**Quellen (konsolidiert):**
- Duden Groß- und Kleinschreibung
- Reform der deutschen Rechtschreibung 1996
- DIN 5008:2020-03
- CLDR Plural Rules (Unicode)
- Material Design 3 — Content Design
- German UPA — Leitfaden UX-Writing & Fehlermeldungen 2024
- Android Developers — String Resources, Localization
- Typefacts, Languagetool, Scribbr zu Typografie
