# Schicht 4b — Wortlaut-Mapping pro Bereich (PFLICHT)

> **FIX AA4 (Audit 10) — Kotlin + Java:** Wortlaut-Patterns mit `--include='*.kt'` muessen bei Java-Hybrid-Apps um `--include='*.java'` ergaenzt werden (Activity/Fragment mit `getString(R.string....)`, AlertDialog.Builder mit `.setTitle(...).setMessage(...)`, Toast mit `Toast.makeText(...)`). Reine Kotlin-Apps koennen den Java-Filter weglassen.

## Warum diese Schicht existiert

Der Rechtssicherheits-Skill prueft jede Formulierung gegen UWG, EU UCPD, DSGVO, Google Play Policy und weitere Rechtsnormen. **Schon ein einziges falsches Wort kann eine Abmahnung ausloesen.** Beispiele aus der Praxis:

| Formulierung (Achtung) | Risiko | Bessere Formulierung |
|------------------------|--------|--------------------|
| "Geld zurueck" | UWG §5 — keine Bedingungen genannt | "Geld zurueck innerhalb 14 Tagen ohne Angabe von Gruenden" |
| "Unbegrenzte KI-Analysen" | UWG §5 — Code limitiert auf 150/Tag | "Bis zu 150 KI-Analysen pro Tag" |
| "Anonym" | DSGVO Art. 4 — Pseudonymisierung != Anonymisierung | "Pseudonymisierte Nutzung" |
| "Kostenlos" | UWG §5a — Pflicht-Abo nach Trial nicht erwaehnt | "7 Tage gratis testen, danach 4,99 €/Monat" |
| "Sicher" | UWG §5 — kein Beleg | "Verschluesselte Uebertragung mit TLS 1.3" |
| "Garantiert" | UWG §5 — Garantie-Bedingungen fehlen | "Geld-zurueck-Garantie unter den Bedingungen aus Punkt 4 unserer AGB" |

Deshalb sind 1:1-Wortlaute nicht optional, sondern die Grundlage des gesamten Rechtssicherheits-Audits.

**Coverage-Beitrag fuer "was steht 1:1 in der App": 100 Prozent**

## 4b.1 Goldene Regeln fuer jeden Wortlaut-Eintrag

| Regel | Pflicht-Format |
|-------|---------------|
| **Stable Area-ID** | Jeder Bereich bekommt eine eindeutige ID (z.B. `area_id: dialog_delete_entry`) fuer programmatischen Zugriff und stabile Querverweise |
| **Zitat in Anfuehrungszeichen** | `"Jetzt Premium starten"` — kein freier Fliesstext, immer `"..."` |
| **Quelle vollstaendig** | `R.string.paywall_cta_primary @ res/values/strings.xml:142` |
| **UI-Element nennen** | "Primaer-Button TopBar / TopBar-Title / Confirm-Button im Loesch-Dialog" |
| **Alle Sprachen** | DE/EN/FR/... — pro Sprache eigene Zelle ODER eigene Zeile mit Sprach-Suffix |
| **Sonderzeichen erhalten** | Umlaute, Geviertstriche, geschuetzte Leerzeichen, runde/typografische Quotes nicht "normalisieren" |
| **Plurals komplett** | `<plurals>` mit allen Quantitaeten: zero/one/two/few/many/other (sofern definiert) |
| **Format-Strings markieren** | `%1$s`, `%2$d` bleiben drin und werden als Platzhalter erklaert |

### Stable Area-ID Konvention

Jede Wortlaut-Tabelle bekommt eine eindeutige `area_id`, damit nachgelagerte Skills (Rechtssicherheits-Skill, Uebersetzungs-Skill) gezielt referenzieren koennen und der Audit-Bericht ueber mehrere Laeufe hinweg stabile Querverweise behaelt.

**Format:** `<typ>_<name>_<sub>`

| Typ-Praefix | Bedeutung | Beispiel |
|------------|-----------|----------|
| `screen_` | Bildschirm | `screen_dashboard`, `screen_entry_editor` |
| `dialog_` | Dialog | `dialog_delete_entry`, `dialog_logout_confirm` |
| `sheet_` | Bottom-Sheet | `sheet_share_options`, `sheet_filter_entries` |
| `menu_` | Menue | `menu_topbar_dashboard`, `menu_overflow_settings` |
| `setting_` | Settings-Item (Hierarchie mit `>`) | `setting_account_profile_displayname` |
| `snack_` | Snackbar | `snack_entry_saved`, `snack_network_error` |
| `toast_` | Toast | `toast_invalid_input` |
| `notif_` | Push-Notification-Template | `notif_daily_reminder`, `notif_trial_end` |
| `error_` | Error-State | `error_offline`, `error_billing_unavailable` |
| `empty_` | Empty-State | `empty_dashboard_no_entries` |
| `loading_` | Loading-State | `loading_paywall_purchase` |
| `tip_` | Tooltip / Banner | `tip_onboarding_step3_save` |
| `field_` | TextField | `field_login_email`, `field_entry_title` |
| `chip_` | Chip | `chip_filter_emotional`, `chip_filter_recent` |
| `search_` | SearchBar | `search_entries`, `search_help` |
| `a11y_` | semantics-Block | `a11y_fab_new_entry`, `a11y_paywall_close` |
| `slider_` | Slider | `slider_reminder_time` |
| `picker_` | Date/Time-Picker | `picker_reminder_time`, `picker_entry_date` |
| `link_` | AnnotatedString Inline-Link | `link_terms_in_onboarding`, `link_privacy_in_paywall` |
| `paywall_` | Paywall-Bildschirm | `paywall_main`, `paywall_onboarding`, `paywall_winback` |
| `state_` | Subscription-State-Banner | `state_paused`, `state_grace_period`, `state_on_hold` |
| `churn_` | Cancel-/Churn-Flow | `churn_survey`, `churn_confirm` |
| `legal_` | Rechtstext (Schicht 4d) | `legal_terms_link`, `legal_privacy_link`, `legal_consent_banner` |
| `perm_` | Permission-Rationale (Schicht 4d) | `perm_camera_rationale`, `perm_notifications_rationale` |
| `external_` | Externe Inhalte (Schicht 4e) | `external_store_listing_de`, `external_email_welcome_de` |

### Regeln fuer Area-IDs

| Regel | Begruendung |
|-------|-------------|
| **snake_case** | Kompatibel mit JSON-Keys, URLs, CLI-Tools |
| **stabil ueber Audit-Laeufe** | Selbe ID auch wenn String-Key sich aendert |
| **eindeutig pro Bericht** | Keine zwei Bereiche duerfen die gleiche `area_id` haben |
| **kurz aber sprechend** | `dialog_delete_entry` statt `dialog_delete_journal_entry_confirmation_with_undo_button` |
| **Hierarchien mit `_`** | Settings-Pfade: `setting_account_profile_displayname` |
| **Sub-Komponenten mit `__` (doppelter Underscore)** | `dialog_delete_entry__confirm_button` fuer einzelne Slots |

## 4b.2 Die 20 Bereichstypen — fuer jeden eine Pflicht-Tabelle

Jede dieser Kategorien bekommt im finalen Bericht einen eigenen Unterabschnitt mit einer Wortlaut-Tabelle. Wenn ein Bereichstyp in der App nicht existiert, wird das explizit vermerkt ("Keine Snackbar-Texte gefunden — geprueft mit Patterns X").

| # | Bereichstyp | Was zaehlt dazu |
|---|------------|-----------------|
| 1 | **Bildschirme** (Screens) | TopBar-Title, Inhalts-Header, beschreibende Texte, Footer |
| 2 | **Dialoge** | Title + Body + Confirm-Button + Dismiss-Button + Neutral-Button |
| 3 | **Bottom-Sheets** | Title + Inhalt + alle enthaltenen Buttons/Optionen |
| 4 | **Menues** (Top, Bottom, Navigation Drawer, Overflow) | Menu-Item-Labels |
| 5 | **Settings-Items** | Label + Beschreibung + Switch-Werte + Dropdown-Optionen + Slider-Labels |
| 6 | **Snackbars** | Message + Action-Button-Label |
| 7 | **Toasts** | Message |
| 8 | **Push-Notifications** | Channel-Name + Title + Body + Action-Button-Labels |
| 9 | **Error-States** | Fehlermeldungen, "Erneut versuchen"-Buttons |
| 10 | **Empty-States** | Leerer-Zustand-Texte ("Noch keine Eintraege") + Call-to-Action |
| 11 | **Loading-States** | Lade-Texte, Progress-Beschreibungen |
| 12 | **Onboarding/Tooltips/Banner** | Schritt-Texte, Tooltip-Bodies, In-App-Banner |
| 13 | **TextField-Slots** | label, placeholder, supportingText, prefix, suffix, errorText, leadingIcon-cd, trailingIcon-cd |
| 14 | **Chips** | FilterChip-Label, AssistChip-Label, InputChip-Label, SuggestionChip-Label |
| 15 | **Material 3 Tooltips** | PlainTooltip-Text, RichTooltip-Title/Text/Action |
| 16 | **SearchBar / DockedSearchBar** | placeholder, leadingIcon-cd, trailingIcon-cd, suggestion-headers |
| 17 | **semantics-Block** | contentDescription, stateDescription, liveRegion, paneTitle |
| 18 | **Slider mit Labels** | thumb-Text, Min/Max-Labels, Step-Labels, Value-Format |
| 19 | **Date/Time-Picker** | Title, OK-Button, Cancel-Button, Headline-Format |
| 20 | **AnnotatedString / Markdown** | Inline-Hyperlinks, fette/kursive Spans, eingebettete Klick-Texte |

## 4b.3 Suchstrategie — wie finde ich die Wortlaute?

### A) Compose: stringResource und Verwandte

```bash
# Direkte String-Resource-Aufrufe in Compose
grep -rn 'stringResource(\s*R\.string\.' --include='*.kt' . -A 0

# Plurals
grep -rn 'pluralStringResource(\s*R\.plurals\.' --include='*.kt' .

# Format-Strings mit Argumenten
grep -rn 'stringResource(\s*R\.string\.[a-zA-Z_]\+\s*,' --include='*.kt' .

# Array-Resources (typisch fuer Settings-Dropdowns)
grep -rn 'stringArrayResource(\s*R\.array\.' --include='*.kt' .
grep -rn 'integerArrayResource(\s*R\.array\.' --include='*.kt' .
```

### B) Klassisches Android-View / Activity / Fragment

```bash
# getString und getQuantityString
grep -rn 'getString(\s*R\.string\.\|context\.getString\|requireContext()\.getString\|resources\.getString' --include='*.kt' --include='*.java' .
grep -rn 'getQuantityString(\s*R\.plurals\.' --include='*.kt' --include='*.java' .

# XML-Layouts mit android:text
grep -rn 'android:text=' --include='*.xml' res/
grep -rn 'android:hint=' --include='*.xml' res/
grep -rn 'android:contentDescription=' --include='*.xml' res/

# Preference XML
grep -rn 'android:title=\|android:summary=\|android:entries=\|android:entryValues=' --include='*.xml' res/xml/
```

### C) Dialoge — Title + Body + Buttons

```bash
# AlertDialog Builder Pattern
grep -rn 'AlertDialog\.Builder\|MaterialAlertDialogBuilder' --include='*.kt' --include='*.java' . -A 20

# Compose AlertDialog
grep -rn 'AlertDialog(' --include='*.kt' . -A 30

# Material 3 AlertDialog Slots: title, text, confirmButton, dismissButton
grep -rn 'title = {\s*Text\|text = {\s*Text\|confirmButton = {\|dismissButton = {' --include='*.kt' .

# setTitle / setMessage / setPositiveButton
grep -rn '\.setTitle(\|\.setMessage(\|\.setPositiveButton(\|\.setNegativeButton(\|\.setNeutralButton(' --include='*.kt' --include='*.java' .
```

### D) Menues — alle Tiefen rekursiv

```bash
# XML-Menues (Action Bar, Bottom Navigation, Drawer)
find . -path '*/res/menu/*.xml' -not -path '*/build/*' | sort
grep -rn '<item\b' --include='*.xml' res/menu/ -A 3

# Compose DropdownMenu / DropdownMenuItem
grep -rn 'DropdownMenu(\|DropdownMenuItem(' --include='*.kt' . -A 5

# NavigationBarItem (Bottom-Nav) und NavigationDrawerItem
grep -rn 'NavigationBarItem(\|NavigationRailItem(\|NavigationDrawerItem(' --include='*.kt' . -A 8

# TopAppBar mit Actions
grep -rn 'TopAppBar(\|CenterAlignedTopAppBar(\|MediumTopAppBar(\|LargeTopAppBar(' --include='*.kt' . -A 15

# Tabs
grep -rn 'Tab(\|LeadingIconTab(\|ScrollableTabRow' --include='*.kt' . -A 5
```

### E) Settings/Preferences — komplette Hierarchie

```bash
# Preference-XML (klassisch)
find . -path '*/res/xml/*.xml' -not -path '*/build/*'
grep -rn '<PreferenceScreen\|<PreferenceCategory\|<SwitchPreferenceCompat\|<ListPreference\|<EditTextPreference\|<SeekBarPreference\|<MultiSelectListPreference' --include='*.xml' res/xml/

# Compose-Settings (kein offizielles Pattern — manuell pro App pruefen)
grep -rn 'SettingsScreen\|PreferenceScreen\|SettingsRow\|SettingsItem' --include='*.kt' .
```

### F) Snackbars, Toasts, Notifications

```bash
# Snackbar (Compose und View)
grep -rn 'snackbarHostState\.showSnackbar\|Snackbar\.make\|SnackbarHostState' --include='*.kt' --include='*.java' . -A 3

# Toast
grep -rn 'Toast\.makeText\(' --include='*.kt' --include='*.java' . -A 1

# NotificationCompat.Builder Inhalte
grep -rn '\.setContentTitle(\|\.setContentText(\|\.setSubText(\|\.setTicker(\|\.setBigContentTitle(\|\.setSummaryText(' --include='*.kt' --include='*.java' .

# Notification Channels (Name + Description)
grep -rn 'NotificationChannel(\|notificationManager\.createNotificationChannel' --include='*.kt' --include='*.java' . -A 5
```

### G) Error-, Empty-, Loading-States

```bash
# Typische Komponenten-Namen
grep -rn 'ErrorScreen\|ErrorState\|ErrorMessage\|EmptyScreen\|EmptyState\|LoadingScreen\|LoadingState\|RetryButton' --include='*.kt' . -A 10

# sealed UiState mit Error/Empty/Loading
grep -rn 'sealed.*UiState\|object Loading\|object Empty\|data class Error' --include='*.kt' . -A 5
```

### H) Hardcoded Strings (DARF eigentlich nicht sein, muss aber geprueft werden)

```bash
# Compose: Text("...") mit direktem Literal
grep -rn 'Text(\s*"[A-Za-zÄÖÜäöü0-9]' --include='*.kt' . | grep -v '/test/' | grep -v 'Text(text ='

# Compose: Text(text = "literal")
grep -rn 'Text(\s*text\s*=\s*"[A-Za-zÄÖÜäöü0-9]' --include='*.kt' . | grep -v '/test/'

# View: setText("...")
grep -rn '\.setText("[A-Za-zÄÖÜäöü0-9][^"]\{3,\}"' --include='*.kt' --include='*.java' .

# Toast.makeText mit String-Literal
grep -rn 'Toast\.makeText([^,]*,\s*"' --include='*.kt' --include='*.java' .
```

Hardcoded Texte werden im Bericht als **eigene Tabelle "Nicht-internationalisierte Wortlaute"** aufgelistet — sie sind oft Quellen versteckter Werbeaussagen, welche die Uebersetzungs-Pipeline nicht erfasst hat.

## 4b.4 Rekursive Menue-Aufloesung (PFLICHT — egal wie tief)

Diese Regel ist nicht verhandelbar: JEDES Menue, JEDES Untermenue, JEDE weitere Verschachtelung wird vollstaendig aufgeloest. Kein "und so weiter", kein "siehe Code", kein "(Standard-Untermenue)".

### Pfad-Notation

Jedes Menue-Item bekommt einen Breadcrumb-Pfad als Praefix:

```
Settings > Konto > Sicherheit > 2FA > Backup-Codes neu generieren
```

Der Pfad zeigt die komplette Hierarchie. Jede Ebene wird als eigener Eintrag in der Tabelle gefuehrt, AUCH wenn sie selbst keine eigenen Texte hat ausser dem Label — denn dieses Label IST der zu zitierende Wortlaut.

### Beispiel-Tabelle fuer eine Settings-Hierarchie (Auszug)

| Pfad | Label-Wortlaut | Beschreibung-Wortlaut | Werte-Wortlaut(e) | Quelle |
|------|---------------|----------------------|-------------------|--------|
| Settings | "Einstellungen" | — | — | `R.string.title_settings @ values/strings.xml:12` |
| Settings > Konto | "Konto" | "Anmeldung, Profil, Loeschung" | — | `R.string.settings_account_title`, `R.string.settings_account_summary` |
| Settings > Konto > Profil | "Profil" | "Name, Avatar, Bio" | — | `R.string.settings_profile_title`, `R.string.settings_profile_summary` |
| Settings > Konto > Profil > Anzeigename | "Anzeigename" | "So sehen dich andere Nutzer" | (frei) | `R.string.settings_profile_displayname_*` |
| Settings > Konto > Profil > Avatar aendern | "Avatar aendern" | "Aus Galerie oder Kamera" | — | `R.string.settings_profile_avatar_*` |
| Settings > Konto > Sicherheit | "Sicherheit" | "Zwei-Faktor, App-Sperre" | — | `R.string.settings_security_*` |
| Settings > Konto > Sicherheit > Zwei-Faktor | "Zwei-Faktor-Authentifizierung" | "Schuetzt dein Konto mit einem zweiten Faktor" | aus/ein | `R.string.settings_2fa_*` |
| Settings > Konto > Sicherheit > Zwei-Faktor > Backup-Codes | "Backup-Codes verwalten" | "10 Codes fuer den Notfall" | — | `R.string.settings_2fa_backup_*` |
| Settings > Konto > Sicherheit > Zwei-Faktor > Backup-Codes > Neu generieren | "Neue Codes erzeugen" | "Alte Codes werden ungueltig" | — | `R.string.settings_2fa_regen_*` |
| Settings > Konto > Sicherheit > Zwei-Faktor > Backup-Codes > Neu generieren > Bestaetigungs-Dialog | "Wirklich neu erzeugen?" (Title), "Alle bisherigen Codes verlieren ihre Gueltigkeit. Diese Aktion kann nicht rueckgaengig gemacht werden." (Body), "Neu erzeugen" (Confirm), "Abbrechen" (Dismiss) | — | `R.string.settings_2fa_regen_confirm_*` |

Diese Tiefe wird IMMER ausgerollt, egal wie viele Ebenen die App hat. Wenn ein Menue-Item zu einer Activity oder einem neuen Screen springt, beginnt dort die naechste Wortlaut-Tabelle (Bildschirm-Tabelle), und der Pfad-Prefix bleibt erhalten.

### Wie tief ist "zu tief"?

Es gibt keine "zu tief"-Grenze. Wenn die App 9 Ebenen tief verschachtelt — alle 9 Ebenen werden zitiert. Sollte der Bericht dadurch lang werden, ist das gewollt: der Rechtssicherheits-Skill muss alles sehen.

## 4b.5 Ausgabeformate — eine Tabelle pro Bereich

### Standard-Tabelle pro Screen

```markdown
### Wortlaute im Screen: <Name> (`DashboardScreen.kt:42`)

**Area-ID:** `screen_dashboard`

| UI-Element | Sub-Area-ID | String-Key | Wortlaut (DE) | Wortlaut (EN) | Wortlaut (FR) | ... |
|-----------|------------|-----------|---------------|--------------|---------------|-----|
| TopBar-Title | `screen_dashboard__topbar_title` | `dashboard_title` | "Mein Tagebuch" | "My Journal" | "Mon journal" | ... |
| FAB-Label (a11y) | `screen_dashboard__fab_cd` | `dashboard_fab_new_entry_cd` | "Neuen Eintrag erstellen" | "Create new entry" | ... | ... |
| Empty-State-Headline | `screen_dashboard__empty_title` | `dashboard_empty_title` | "Noch keine Eintraege" | "No entries yet" | ... | ... |
| Empty-State-Body | `screen_dashboard__empty_body` | `dashboard_empty_body` | "Tippe auf das Plus-Symbol, um deinen ersten Eintrag zu erstellen." | ... | ... | ... |
| Empty-State-CTA | `screen_dashboard__empty_cta` | `dashboard_empty_cta` | "Ersten Eintrag erstellen" | ... | ... | ... |
```

### Standard-Tabelle pro Dialog

```markdown
### Dialog: Eintrag loeschen (`DeleteEntryDialog.kt:18`)

| Slot | String-Key | Wortlaut (DE) | Wortlaut (EN) | ... |
|------|-----------|---------------|--------------|-----|
| Title | `delete_entry_title` | "Eintrag loeschen?" | "Delete entry?" | ... |
| Body | `delete_entry_body` | "Dieser Eintrag wird unwiderruflich geloescht. Diese Aktion kann nicht rueckgaengig gemacht werden." | "This entry will be permanently deleted. This action cannot be undone." | ... |
| Confirm-Button | `delete_entry_confirm` | "Loeschen" | "Delete" | ... |
| Dismiss-Button | `delete_entry_cancel` | "Abbrechen" | "Cancel" | ... |
```

### Standard-Tabelle pro Menue (mit Tiefenpfad)

```markdown
### Menue: Settings — komplette Hierarchie

(siehe Beispiel-Tabelle in 4b.4)
```

### Standard-Tabelle pro Push-Notification

```markdown
### Push-Notification: Tagesreminder (`DailyReminderWorker.kt:62`)

| Slot | String-Key | Wortlaut (DE) | Wortlaut (EN) | ... |
|------|-----------|---------------|--------------|-----|
| Channel-Name | `notif_channel_daily_name` | "Tagesreminder" | "Daily reminder" | ... |
| Channel-Description | `notif_channel_daily_desc` | "Erinnert dich abends an deinen Tagebuch-Eintrag" | "Reminds you in the evening to write your journal entry" | ... |
| Title | `notif_daily_title` | "Wie war dein Tag?" | "How was your day?" | ... |
| Body (Plural?) | `notif_daily_body` | "Halte deinen Tag in einem kurzen Eintrag fest." | ... | ... |
| Action 1 | `notif_daily_action_now` | "Jetzt schreiben" | "Write now" | ... |
| Action 2 | `notif_daily_action_later` | "Spaeter erinnern" | "Remind later" | ... |
```

### Standard-Tabelle pro Settings-Item (mit Switch/Dropdown-Werten)

```markdown
### Settings-Item: Theme (`SettingsScreen.kt:128`)

| Slot | String-Key | Wortlaut (DE) | Wortlaut (EN) | ... |
|------|-----------|---------------|--------------|-----|
| Item-Label | `setting_theme_label` | "Design" | "Theme" | ... |
| Item-Beschreibung | `setting_theme_summary` | "Hell, Dunkel oder Systemvorgabe" | "Light, dark or system default" | ... |
| Dropdown-Option 1 | `setting_theme_value_system` | "Systemvorgabe" | "System default" | ... |
| Dropdown-Option 2 | `setting_theme_value_light` | "Hell" | "Light" | ... |
| Dropdown-Option 3 | `setting_theme_value_dark` | "Dunkel" | "Dark" | ... |
| Dialog-Title | `setting_theme_dialog_title` | "Design waehlen" | "Choose theme" | ... |
```

### Standard-Tabelle pro TextField

```markdown
### TextField: <Name/Zweck> (`<Datei.kt:Zeile>`)

| Slot | String-Key | Wortlaut (DE 1:1) | Wortlaut (EN 1:1) | Weitere Sprachen |
|------|-----------|-------------------|-------------------|------------------|
| label | | | | |
| placeholder | | | | |
| supportingText | | | | |
| prefix | | | | |
| suffix | | | | |
| errorText (bei isError = true) | | | | |
| leadingIcon contentDescription | | | | |
| trailingIcon contentDescription | | | | |
```

### Standard-Tabelle pro Chip-Gruppe

```markdown
### Chip-Gruppe: <Name/Zweck> (`<Datei.kt:Zeile>`)

| Chip-Typ | Slot | String-Key | Wortlaut (DE 1:1) | Wortlaut (EN 1:1) |
|---------|------|-----------|-------------------|-------------------|
| FilterChip 1 | label | | | |
| FilterChip 2 | label | | | |
| AssistChip 1 | label | | | |
| ... | ... | ... | ... | ... |
```

### Standard-Tabelle pro Tooltip

```markdown
### Tooltip: <Name/Zweck> (`<Datei.kt:Zeile>`)

Tooltip-Typ: PlainTooltip / RichTooltip

| Slot | String-Key | Wortlaut (DE 1:1) | Wortlaut (EN 1:1) |
|------|-----------|-------------------|-------------------|
| Title (nur Rich) | | | |
| Text / Body | | | |
| Action-Button (nur Rich) | | | |
```

### Standard-Tabelle pro SearchBar

```markdown
### SearchBar: <Name> (`<Datei.kt:Zeile>`)

| Slot | String-Key | Wortlaut (DE 1:1) | Wortlaut (EN 1:1) |
|------|-----------|-------------------|-------------------|
| placeholder | | | |
| leadingIcon contentDescription | | | |
| trailingIcon contentDescription | | | |
| Empty-Suggestions-Header | | | |
| No-Results-State | | | |
```

### Standard-Tabelle pro semantics-Block (Accessibility)

```markdown
### Accessibility-Texte (semantics-Block)

| Komponente | Datei:Zeile | contentDescription | stateDescription | liveRegion | paneTitle |
|------------|-------------|-------------------|------------------|-----------|-----------|
| FAB Dashboard | DashboardScreen.kt:88 | "Neuen Eintrag erstellen" | — | — | — |
| Sync-Status-Banner | TopBar.kt:32 | "Synchronisation laeuft" | "synchronisiert" | Polite | — |
| Paywall-Bildschirm | PaywallScreen.kt:12 | — | — | — | "Premium-Kauf" |
| ... | ... | ... | ... | ... | ... |
```

### Standard-Tabelle pro Date/Time-Picker

```markdown
### Date/Time-Picker: <Name> (`<Datei.kt:Zeile>`)

| Slot | String-Key | Wortlaut (DE 1:1) | Wortlaut (EN 1:1) |
|------|-----------|-------------------|-------------------|
| Picker-Title | | | |
| Headline-Format (z.B. "%d. %s %d") | | | |
| OK-Button | | | |
| Cancel-Button | | | |
| Mode-Toggle-Label (Date/Range) | | | |
```

### Standard-Tabelle pro AnnotatedString mit Inline-Links

```markdown
### Inline-Hyperlinks (AnnotatedString)

| Datei:Zeile | Komponente | Voll-Text mit Markup (1:1) | Link-Texte (1:1) | Link-Targets |
|-------------|------------|----------------------------|------------------|--------------|
| OnboardingTerms.kt:42 | Terms-Footer | "Mit Klick auf ..weiter.. akzeptierst du unsere [AGB] und [Datenschutz]" | "AGB", "Datenschutz" | terms_url, privacy_url |
| ... | ... | ... | ... | ... |
```

### Standard-Tabelle pro Snackbar / Toast / Error

```markdown
### Snackbars und Toasts

| Trigger / Komponente | String-Key | Wortlaut (DE) | Action-Label (DE) | Quelle |
|---------------------|-----------|---------------|------------------|--------|
| EntrySaved (Erfolg) | `snackbar_entry_saved` | "Eintrag gespeichert" | "Rueckgaengig" (`snackbar_undo`) | `JournalViewModel.kt:88` |
| EntryDeleted (Erfolg) | `snackbar_entry_deleted` | "Eintrag geloescht" | "Wiederherstellen" | `JournalViewModel.kt:102` |
| NetworkError | `error_network_offline` | "Keine Internetverbindung. Bitte spaeter erneut versuchen." | "Erneut versuchen" | `SyncRepository.kt:55` |
| BillingUnavailable | `error_billing_unavailable` | "Google Play ist gerade nicht erreichbar." | — | `BillingClient.kt:140` |
```

### Standard-Tabelle pro Plural-Resource

```markdown
### Plural-Resources

| Key | Quantity | Wortlaut (DE) | Wortlaut (EN) | ... | Format-Argumente |
|-----|---------|---------------|--------------|-----|------------------|
| `plural_entries_count` | one | "%d Eintrag" | "%d entry" | ... | %d = Anzahl |
| `plural_entries_count` | other | "%d Eintraege" | "%d entries" | ... | %d = Anzahl |
| `plural_days_left` | one | "Noch %d Tag" | "%d day left" | ... | %d = Tage |
| `plural_days_left` | other | "Noch %d Tage" | "%d days left" | ... | %d = Tage |
```

## 4b.6 Sprach-Audit fuer jeden Wortlaut

Wenn die App mehrsprachig ist, MUSS pro Wortlaut geprueft werden:

1. **Gibt es eine Uebersetzung in jeder gefundenen Sprache?** (`values-en/`, `values-fr/`, `values-pt/`, ...)
2. **Sind die Uebersetzungen konsistent in der Aussage?** (Eine Uebersetzung darf nicht "anonym" sagen, wenn die Original-Aussage "pseudonymisiert" war.)
3. **Werden Format-Strings korrekt erhalten?** (`%1$s` und `%2$d` bleiben, Reihenfolge kann sich aendern.)

Pro kritische Aussage (alle Wortlaute die unter Layer 7.2 in KRITISCH/HOCH fallen): eigene Mehrsprach-Tabelle (siehe Layer 7.6).

## 4b.7 Ehrlichkeitsregel: Was tun, wenn ein Wortlaut nicht gefunden wird?

| Situation | Vorgehen |
|-----------|---------|
| String-Key existiert, aber kein Wert in der Sprache | `"(fehlt — fallback auf Default-Sprache)"` zitieren |
| String-Key wird per `getString` zur Laufzeit zusammengesetzt | Alle moeglichen Kombinationen zitieren oder `"UNKLAR — dynamisch zusammengebaut aus X + Y"` |
| Text kommt aus Server (Remote Config) | Default-Wert zitieren + Hinweis `"Live-Wert kann via Firebase Remote Config Schluessel X ueberschrieben werden"` |
| Text kommt aus API-Response | `"UNKLAR — Inhalt kommt von Backend, manuell pruefen"` |
| Hardcoded Text im Code | 1:1 zitieren, im Bericht als "Nicht-internationalisiert" markieren |
| Text nur in Spezialvariante (z.B. nur Premium-User sehen ihn) | Bedingung als Praefix notieren: `"[nur Premium]: 'Erweiterte Statistiken'"` |

## 4b.8 Vollstaendigkeits-Pruefung am Ende von Schicht 4b

Bevor die Schicht als abgeschlossen gilt, MUSS folgendes gezaehlt und im Bericht dokumentiert werden:

| Metrik | Wert |
|--------|------|
| Anzahl Screens mit Wortlaut-Tabelle | N |
| Anzahl Dialoge mit Wortlaut-Tabelle | N |
| Anzahl Bottom-Sheets mit Wortlaut-Tabelle | N |
| Anzahl Menue-Pfade dokumentiert (jede Ebene zaehlt einzeln) | N |
| Maximale Menue-Tiefe in der App | N |
| Anzahl Settings-Items dokumentiert | N |
| Anzahl Snackbars/Toasts/Errors dokumentiert | N |
| Anzahl Push-Notification-Templates dokumentiert | N |
| Anzahl Plural-Resources dokumentiert | N |
| Anzahl Array-Resources dokumentiert | N |
| Anzahl hardcoded Strings gefunden (sollte 0 sein!) | N |
| String-Keys im Code referenziert | N |
| String-Keys in strings.xml definiert | N |
| Differenz (tote Keys oder fehlende Keys) | N — pro Differenz Erklaerung |

Wenn die Differenz "String-Keys im Code" vs "Keys in strings.xml" nicht 0 ist, wird die Diskrepanz mit Auflistung der Keys im Bericht gezeigt.

## 4b.9 Was diese Schicht NICHT macht

- Sie pruefen NICHT, ob ein Wortlaut rechtlich problematisch ist — das macht der Rechtssicherheits-Skill und Schicht 7.
- Sie geben KEINE Verbesserungs-Vorschlaege fuer Formulierungen — sie zitieren nur 1:1.
- Sie kuerzen NICHT — wenn ein Text 2000 Zeichen lang ist (z.B. Datenschutzerklaerung-Ausschnitt), wird er vollstaendig zitiert.

## Typische Fehlerquellen

- **`stringResource(R.string.x, "arg1", "arg2")`**: Format-Argumente werden uebersehen wenn man nur den String-Key zitiert. Immer den interpolierten Beispiel-Wert mit zeigen ("Hallo %1$s, du hast %2$d Eintraege" → "Hallo Frank, du hast 12 Eintraege").
- **Compose Tooltip-Box**: `TooltipBox` mit `richTooltip` enthaelt eigene Text-Slots die leicht uebersehen werden.
- **Accessibility-Texte**: `contentDescription` und `semantics { contentDescription = "..." }` sind oft NICHT in strings.xml und werden zur Werbeaussagen-Quelle (Screenreader-Text "Premium-Funktion").
- **Konkatenierte Strings**: `"Premium" + " " + stringResource(R.string.feature_pdf)` muss als zusammengesetzter Wortlaut dokumentiert werden.
- **Conditional Texts**: `if (isPremium) "Du bist Premium" else "Werde Premium"` — beide Varianten zitieren.
- **Stringtemplate in Kotlin**: `"Willkommen, $userName"` — den Template-Rahmen zitieren mit Beispiel-Substitution.
- **HTML/Markdown in Strings**: Wenn `<b>`, `<i>`, `<u>` oder Markdown-Syntax im String steht, MUSS sie erhalten bleiben — das beeinflusst die Darstellung und kann rechtlich relevant sein (z.B. fett hervorgehobene Aussagen brauchen besondere Aufmerksamkeit).
