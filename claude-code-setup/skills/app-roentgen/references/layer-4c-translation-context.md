# Schicht 4c — Translation-Context (Grundlage fuer den Uebersetzungs-Skill)

> **Pfad-Annahme (Multi-Module-Hinweis):** Die Bash-Snippets in diesem Layer benutzen aus Lesbarkeitsgruenden `app/src/main/res/values/strings.xml`. Bei Multi-Module-Apps (Feature-Module mit eigenen `src/main/res/values/`) projektweit suchen:
> ```bash
> find . -path '*/src/main/res/values*/strings.xml' -not -path '*/build/*'
> ```
> Das `feature-scan.sh` Skript hat dafuer `find_default_strings_xml`, `find_translated_strings_xml` und `find_locale_dirs` und nutzt sie automatisch. Manuelle Audits muessen die Pfad-Anpassung selbst vornehmen.

## Warum diese Schicht existiert

Der Uebersetzungs-Skill braucht mehr als nur den Original-Wortlaut. Er braucht den **Kontext** unter dem der String erscheint, damit eine korrekte Uebersetzung moeglich ist:

| Ohne Context | Mit Context |
|--------------|-------------|
| "Bank" → bank (Geld) oder bench (Sitzbank)? | "Bank" mit Slot=Settings-Item, Kategorie=Finanzen → bank |
| "Save" → speichern oder retten? | "Save" mit Slot=Button in EntryEditor → speichern |
| "%1$s hat %2$d Eintraege" | Kommentar: "%1$s = Benutzername, %2$d = Eintragszahl" |
| Plural-String fehlt | Audit: "ru/ar brauchen `few` und `many`" |

Layer 4c liefert all das. Sie ist die Bruecke zwischen Roentgen-Audit und Uebersetzungs-Skill.

**Coverage-Beitrag fuer "uebersetzbarer Kontext": 100 Prozent**

## 4c.1 Laengenbeschraenkungen pro UI-Slot (PFLICHT)

Jeder String hat eine implizite Maximallaenge basierend auf seinem UI-Slot. Werden diese ueberschritten, schneidet die UI ab oder zerlegt das Layout — typische Folge nach schlechter Uebersetzung.

### Standard-Maximallaengen (Erfahrungswerte fuer mobile UI)

| UI-Slot | Empfohlene Max-Laenge (DE) | Toleranz (EN-Original × Faktor) | Begruendung |
|---------|---------------------------|----------------------------------|-------------|
| Primaer-Button-Label | 14 Zeichen | × 1.3 | Touch-Target 48dp + Padding |
| Sekundaer-Button-Label | 18 Zeichen | × 1.3 | Etwas mehr Platz |
| TopBar-Title | 25 Zeichen | × 1.4 | Wuerde sonst mit Icons kollidieren |
| Bottom-Navigation-Item-Label | 10 Zeichen | × 1.3 | Sehr eng |
| Tab-Label | 12 Zeichen | × 1.3 | Mehrere Tabs in einer Zeile |
| Settings-Item-Label | 30 Zeichen | × 1.4 | Eine Zeile in der Liste |
| Settings-Item-Summary | 60 Zeichen | × 1.4 | Zwei Zeilen + Ellipsis |
| Dialog-Title | 50 Zeichen | × 1.4 | Eine Zeile typisch |
| Dialog-Body | 200 Zeichen | × 1.5 | Mehrere Zeilen erlaubt |
| Dialog-Button | 12 Zeichen | × 1.3 | Side-by-side Layout |
| Snackbar-Message | 80 Zeichen | × 1.4 | Eine Zeile, ggf. zwei |
| Snackbar-Action | 10 Zeichen | × 1.3 | "Rueckgaengig" ist Grenzfall |
| Toast | 60 Zeichen | × 1.4 | Eine Zeile, kurz |
| Push-Notification-Title | 65 Zeichen | × 1.4 | Android-Cap auf Lockscreen |
| Push-Notification-Body | 240 Zeichen | × 1.5 | Big-Text-Style |
| Push-Notification-Action | 20 Zeichen | × 1.3 | Inline-Button |
| TextField-Label | 25 Zeichen | × 1.4 | Floating-Label |
| TextField-Placeholder | 40 Zeichen | × 1.4 | Schwacher Hinweistext |
| TextField-Helper | 80 Zeichen | × 1.5 | Zwei Zeilen unter Feld |
| TextField-Error | 80 Zeichen | × 1.5 | Zwei Zeilen unter Feld |
| Chip-Label | 16 Zeichen | × 1.3 | Kompakt |
| Tooltip-Plain | 50 Zeichen | × 1.4 | Eine Zeile |
| Tooltip-Rich-Title | 30 Zeichen | × 1.4 | Eine Zeile |
| Tooltip-Rich-Text | 200 Zeichen | × 1.5 | Mehrere Zeilen |
| Empty-State-Headline | 30 Zeichen | × 1.4 | Eine Zeile |
| Empty-State-Body | 120 Zeichen | × 1.5 | Zwei bis drei Zeilen |
| Error-State-Headline | 40 Zeichen | × 1.4 | Eine Zeile |
| Error-State-Body | 200 Zeichen | × 1.5 | Mehrere Zeilen |
| ContentDescription (a11y) | 100 Zeichen | × 1.5 | Screenreader-Text |
| App-Name (`app_name`) | 30 Zeichen | × 1.0 | Wird oft nicht uebersetzt |
| Play-Store Short-Description | 80 Zeichen | × 1.0 | Hard-Cap |
| Play-Store Long-Description | 4000 Zeichen | × 1.0 | Hard-Cap |

### Pflicht-Output

Pro Wortlaut wird die effektive Laenge gegen die Slot-Beschraenkung geprueft:

```markdown
| String-Key | Wortlaut (DE) | Slot | Laenge (DE) | Max | Status |
|-----------|---------------|------|-------------|-----|--------|
| `paywall_cta_primary` | "Jetzt Premium starten" | Primaer-Button | 21 | 14 × 1.3 = 18 | ⚠ ZU LANG |
| `dashboard_title` | "Mein Tagebuch" | TopBar-Title | 13 | 25 × 1.4 = 35 | OK |
| `notif_daily_title` | "Wie war dein Tag?" | Push-Title | 17 | 65 × 1.4 = 91 | OK |
```

Strings mit ⚠ werden im Audit-Bericht als "Risiko fuer Uebersetzung" markiert — sie sind im Original schon zu lang und werden in der Uebersetzung noch laenger.

## 4c.2 `translatable="false"` Erfassung (PFLICHT)

Strings die NICHT uebersetzt werden duerfen muessen explizit `translatable="false"` markiert sein:

```xml
<string name="app_brand" translatable="false">BestJournal</string>
<string name="instagram_url" translatable="false">https://instagram.com/...</string>
<string name="copyright_year" translatable="false">2026</string>
```

```bash
# Alle nicht-uebersetzbaren Strings finden
grep -oE '<string name="[^"]+" translatable="false"' app/src/main/res/values/strings.xml | sed 's/<string name="//' | sed 's/" translatable.*//' | sort -u

# Alternative: arrays mit translatable=false
grep -oE '<string-array name="[^"]+" translatable="false"\|<integer-array name="[^"]+" translatable="false"' app/src/main/res/values/strings.xml
```

### Pflicht-Output

```markdown
### 4c.2 Nicht-uebersetzbare Strings (`translatable="false"`)

| Key | Wortlaut | Begruendung (vermutet) |
|-----|----------|----------------------|
| `app_brand` | "BestJournal" | Markenname |
| `version_string` | "v0.20.21" | Versionsnummer |
| `copyright_year` | "2026" | Jahr |
| `support_email` | "support@bestjournal.app" | E-Mail-Adresse |
| `social_twitter` | "@BestJournalApp" | Social-Handle |

Audit: <N> Strings als translatable=false markiert. Erwartete Kandidaten die fehlen (vom Skill geschaetzt):
- `app_url` — sollte translatable=false sein, ist es aber nicht (Pruefen!)
- ...
```

## 4c.3 `xliff:g`-Tags (PFLICHT, falls vorhanden)

Das `xliff:g` Tag markiert Inline-Teile in Strings die nicht uebersetzt werden duerfen:

```xml
<string name="welcome_user">Willkommen, <xliff:g id="username" example="Frank">%1$s</xliff:g>!</string>
<string name="price_display">Preis: <xliff:g id="amount" example="4,99 €">%1$s</xliff:g> pro Monat</string>
```

```bash
# Suche nach xliff:g
grep -n '<xliff:g' app/src/main/res/values/strings.xml | head -50

# Pruefen ob xmlns:xliff deklariert ist
grep -n 'xmlns:xliff' app/src/main/res/values/strings.xml
```

### Pflicht-Output

```markdown
### 4c.3 xliff:g-Tags (Inline-Schutz)

xmlns:xliff deklariert: JA / NEIN

| String-Key | Wortlaut mit xliff:g | id | example |
|-----------|----------------------|------|---------|
| `welcome_user` | "Willkommen, <xliff:g id='username' example='Frank'>%1$s</xliff:g>!" | username | Frank |

Audit: <N> xliff:g-Tags gefunden. Format-Strings ohne xliff:g (Kandidaten):
- `paywall_yearly_price` enthaelt "%1$s €/Jahr" ohne xliff:g — sollte gewrappt werden
```

Wenn `xliff:g` GAR NICHT verwendet wird, im Audit explizit empfehlen: "Strings mit Format-Argumenten sollten `xliff:g` nutzen, sonst koennen Uebersetzer den Platzhalter falsch positionieren oder loeschen."

## 4c.4 XML-Kommentare als Uebersetzer-Notizen (PFLICHT)

`<!-- ... -->`-Kommentare in `strings.xml` sind die direkte Kommunikation an den Uebersetzer:

```xml
<!-- Title fuer Tagesreminder, wird oben in der Push-Notification angezeigt -->
<string name="notif_daily_title">Wie war dein Tag?</string>

<!-- %1$s = Benutzername aus Profil, %2$d = Anzahl der Eintraege heute -->
<string name="dashboard_greeting">Hallo %1$s, du hast heute %2$d Eintraege geschrieben</string>
```

```bash
# Kommentare vor jedem String-Tag extrahieren (5 Zeilen Kontext)
grep -B 1 '<string name=' app/src/main/res/values/strings.xml | grep -A 1 '<!--' | head -100

# Strings OHNE vorangestellten Kommentar zaehlen
awk '/<!--/{c=1; next} /<string name=/{if(!c) print; c=0} /^[^!]/{c=0}' app/src/main/res/values/strings.xml | wc -l
```

### Pflicht-Output

```markdown
### 4c.4 Uebersetzer-Notizen (XML-Kommentare)

Strings mit Kommentar: <N> / <Gesamt> (<X>%)

Beispiele:
| Key | Wortlaut | Kommentar |
|-----|----------|-----------|
| `dashboard_greeting` | "Hallo %1$s, du hast heute %2$d Eintraege" | "%1$s = Benutzername, %2$d = Anzahl Eintraege heute" |

Empfehlung: Strings mit Format-Argumenten ohne Kommentar identifizieren (`%1$s` / `%2$d` ohne erklaerenden `<!--`):
- `paywall_yearly_price` mit `%1$s` — Argument-Bedeutung fehlt
- ...
```

## 4c.5 CLDR-Plural-Vollstaendigkeit pro Sprache (PFLICHT — wichtigster Audit-Punkt fuer Uebersetzung)

Verschiedene Sprachen haben unterschiedliche Plural-Quantitaeten:

| Sprache | Benoetigte Quantitaeten |
|---------|------------------------|
| Deutsch, Englisch, Spanisch, Italienisch, Niederlaendisch, Portugiesisch, Tuerkisch, Chinesisch, Japanisch, Koreanisch | `one`, `other` |
| Franzoesisch, Brasilianisches Portugiesisch | `one`, `many`, `other` |
| Russisch, Ukrainisch, Polnisch, Tschechisch, Slowakisch | `one`, `few`, `many`, `other` |
| Arabisch | `zero`, `one`, `two`, `few`, `many`, `other` |
| Hebraeisch | `one`, `two`, `many`, `other` |
| Walisisch | `zero`, `one`, `two`, `few`, `many`, `other` |
| Irisch | `one`, `two`, `few`, `many`, `other` |
| Slowenisch | `one`, `two`, `few`, `other` |

### Pflicht-Pruefung

Fuer JEDES Plural-Resource in allen Sprachen pruefen ob alle benoetigten Quantitaeten existieren:

```bash
# Alle Plural-Keys extrahieren
PLURAL_KEYS=$(grep -oE '<plurals name="[^"]+"' app/src/main/res/values/strings.xml | sed 's/<plurals name="//' | sed 's/"$//' | sort -u)

# Pro Sprache pruefen ob alle Quantitaeten da sind
for lang_dir in app/src/main/res/values-*/; do
  lang=$(basename "$lang_dir" | sed 's/values-//')
  for key in $PLURAL_KEYS; do
    if [ -f "$lang_dir/strings.xml" ]; then
      QUANTITIES=$(awk "/<plurals name=\"$key\"/,/<\/plurals>/" "$lang_dir/strings.xml" | grep -oE 'quantity="[^"]+"' | sort -u | tr '\n' ',')
      echo "$lang | $key | $QUANTITIES"
    fi
  done
done | head -100
```

### Pflicht-Output

```markdown
### 4c.5 CLDR-Plural-Audit

| Plural-Key | DE | EN | RU | AR | ZH | ... | Status |
|-----------|-----|-----|-----|-----|-----|-----|--------|
| `plural_entries_count` | one,other ✓ | one,other ✓ | one,few,many,other ✓ | zero,one,two,few,many,other ✓ | other ✓ | ... | OK |
| `plural_days_left` | one,other ✓ | one,other ✓ | one,other ⚠ FEHLT few,many | one,other ⚠ FEHLT zero,two,few,many | other ✓ | ... | KRIT RU+AR |

Audit: <N> Plurals geprueft. <X> Sprachen mit fehlenden Quantitaeten — KRITISCH fuer korrekte Anzeige.
```

## 4c.6 HTML/CDATA in Strings (PFLICHT)

HTML-Tags in Strings muessen bei der Uebersetzung erhalten bleiben:

```xml
<string name="terms_intro">Mit der Nutzung akzeptierst du unsere <b>AGB</b>.</string>
<string name="welcome_rich"><![CDATA[<b>Willkommen!</b><br/>Starte mit einem <a href="...">Tutorial</a>.]]></string>
```

```bash
# HTML-Tags in Strings finden
grep -E '<string name="[^"]+">[^<]*<(b|i|u|br|a|font|strong|em|span|p|ol|ul|li|tt)\b' app/src/main/res/values/strings.xml

# CDATA-Bereiche
grep -E '<!\[CDATA\[' app/src/main/res/values/strings.xml

# Escapte HTML-Entities (&lt;, &gt;, &amp;)
grep -E '&(lt|gt|amp|quot|apos|#[0-9]+);' app/src/main/res/values/strings.xml
```

### Pflicht-Output

```markdown
### 4c.6 HTML- und CDATA-Inhalte

| Key | Wortlaut | HTML-Tags | CDATA |
|-----|----------|-----------|-------|
| `terms_intro` | "Mit der Nutzung akzeptierst du unsere <b>AGB</b>." | `<b>` | Nein |
| `welcome_rich` | "[CDATA] <b>Willkommen!</b><br/><a href=...>Tutorial</a>" | `<b>`, `<br/>`, `<a>` | Ja |

Audit: <N> Strings mit HTML, <M> Strings mit CDATA. Empfehlung an Uebersetzer: HTML-Tags wortgenau erhalten, NICHT lokalisieren.
```

## 4c.7 Format-Argument-Semantik (PFLICHT)

Aus dem Roentgen-Audit muss hervorgehen WAS jeder Platzhalter bedeutet — sonst weiss der Uebersetzer nicht ob `%1$d` Eintraege, Tage oder eine Versionsnummer ist.

### Format-Argument-Tabelle (pro Format-String)

```markdown
| Key | Wortlaut (DE) | Argumente | Argument-Bedeutung (aus Code/Kommentar) | Beispiel-Render |
|-----|---------------|-----------|----------------------------------------|----------------|
| `dashboard_greeting` | "Hallo %1$s, du hast heute %2$d Eintraege" | `%1$s`, `%2$d` | %1$s = Benutzername (String), %2$d = Eintragszahl heute (Int) | "Hallo Frank, du hast heute 5 Eintraege" |
| `subscription_until` | "Premium aktiv bis %1$s" | `%1$s` | %1$s = Datum im Format "dd.MM.yyyy" | "Premium aktiv bis 31.12.2026" |
| `plural_days_left` (one) | "Noch %d Tag" | `%d` | %d = verbleibende Tage (Int) | "Noch 1 Tag" |
```

Die Bedeutung wird ermittelt aus:
1. XML-Kommentar (vorzugsweise)
2. Aufruf-Stelle im Code (`stringResource(R.string.x, userName, entryCount)` → erste Argument = userName)
3. Wenn unbekannt: explizit als "UNKLAR — manuell pruefen" markieren

## 4c.8 Glossar-Auto-Erkennung (PFLICHT)

Bestimmte Begriffe muessen ueber die ganze App konsistent uebersetzt werden — der Skill erkennt sie automatisch durch Haeufigkeitsanalyse:

```bash
# Haeufigste 50 substantivierte Begriffe aus strings.xml (englisch-basiert)
grep -oE '<string name="[^"]+">[^<]+</string>' app/src/main/res/values-en/strings.xml | \
  sed 's/<[^>]*>//g' | tr ' ' '\n' | tr -cd '[:alpha:]\n' | \
  awk '{ print tolower($0) }' | sort | uniq -c | sort -rn | head -50

# Haeufigste deutsche Substantive (mit Grossbuchstaben am Anfang)
grep -oE '<string name="[^"]+">[^<]+</string>' app/src/main/res/values/strings.xml | \
  sed 's/<[^>]*>//g' | grep -oE '\b[A-ZÄÖÜ][a-zäöüß]+\b' | sort | uniq -c | sort -rn | head -50
```

### Pflicht-Output

```markdown
### 4c.8 Glossar (Top-30 Begriffe — konsistent uebersetzen)

| Begriff (DE) | Vorkommen | Aktuelle Uebersetzung EN | Vorschlag konsistent | Anmerkung |
|-------------|-----------|--------------------------|---------------------|-----------|
| Eintrag | 142 | "entry" 121x, "post" 12x, "note" 9x | "entry" (Standard) | INKONSISTENT — pruefen |
| Premium | 89 | "premium" 89x | "premium" (Markenname) | Konsistent, sollte translatable=false |
| Tagebuch | 67 | "journal" 65x, "diary" 2x | "journal" (Standard) | Fast konsistent |
| Stimmung | 45 | "mood" 45x | "mood" | OK |
| Einstellungen | 38 | "settings" 38x | "settings" | OK |
| Analyse | 35 | "analysis" 20x, "analytics" 15x | "analysis" (Standard) | INKONSISTENT — pruefen |
```

Strings die ein Glossar-Begriff verwenden bekommen im Bericht ein Marker `[glossar: eintrag]`.

## 4c.9 Region-Differenzen-Audit (pt-rBR vs pt-rPT etc.)

Wenn die App mehrere Regional-Varianten einer Sprache hat, MUSS geprueft werden ob sie inhaltlich unterscheidbar sind:

```bash
# Beispiel: pt-rBR vs pt-rPT vergleichen
diff <(grep -oE '<string name="[^"]+">[^<]+' app/src/main/res/values-pt-rBR/strings.xml | sort) \
     <(grep -oE '<string name="[^"]+">[^<]+' app/src/main/res/values-pt-rPT/strings.xml | sort) | head -30

# Zhcn vs Zhtw
diff <(grep -oE '<string name="[^"]+">[^<]+' app/src/main/res/values-zh-rCN/strings.xml | sort) \
     <(grep -oE '<string name="[^"]+">[^<]+' app/src/main/res/values-zh-rTW/strings.xml | sort) | head -30
```

### Pflicht-Output

```markdown
### 4c.9 Region-Differenzen

| Sprach-Paar | Differenzen | Status |
|-------------|------------|--------|
| pt-rBR vs pt-rPT | 142 Strings identisch | ⚠ Verdacht: pt-rBR ist nur Kopie von pt — keine echte BR-Variante |
| zh-rCN vs zh-rTW | 89 Strings unterschiedlich | OK (Simplified vs Traditional, plus Wortwahl-Unterschiede) |
| es vs es-rMX | nicht vorhanden | OK (nur eine spanische Variante) |
```

Wenn Regional-Varianten identisch zur Default-Variante sind, ist das ein **Indikator fuer fehlende Lokalisierung** — sollte im Audit als Empfehlung erscheinen.

## 4c.10 Du/Sie-Konsistenz-Check (deutsche Sprache, kritisch)

Die deutsche App-Sprache hat einen einzigen heimlichen Killer: Mischanrede ("Du" und "Sie" gemischt). Das ist sofort als unprofessionell erkennbar.

```bash
# Du-Indikatoren in deutscher strings.xml
grep -cE '\b(du|dein|deine|deinem|deinen|deiner|dir|dich)\b' app/src/main/res/values/strings.xml

# Sie-Indikatoren
grep -cE '\b(Sie|Ihr|Ihre|Ihrem|Ihren|Ihrer|Ihnen)\b' app/src/main/res/values/strings.xml

# Detail: Mit Beispielzeilen
grep -nE '\b(du|dein|deine)\b' app/src/main/res/values/strings.xml | head -20
grep -nE '\b(Sie|Ihr|Ihre|Ihnen)\b' app/src/main/res/values/strings.xml | head -20
```

### Pflicht-Output

```markdown
### 4c.10 Du/Sie-Konsistenz (Deutsch)

| Anrede-Form | Anzahl Treffer | Beispiele |
|------------|----------------|-----------|
| Du-Form | 287 Treffer | "Wie war dein Tag?", "Schreibe deinen ersten Eintrag" |
| Sie-Form | 4 Treffer | "Bitte aktivieren Sie...", "Ihre Daten sind sicher" |

Empfehlung: Inkonsistente Anrede gefunden. Dominanz "Du" → 4 Sie-Strings auf Du umstellen ODER bewusst "Sie" beibehalten und durchgaengig anwenden.

Die 4 Sie-Strings:
- `permission_camera_rationale` (Zeile 234): "Bitte aktivieren Sie die Kamera-Berechtigung..."
- ...
```

Diese Pruefung greift NUR fuer Deutsch — andere Sprachen haben andere Anrede-Konventionen (Englisch: nur "you", Franzoesisch: "tu" vs "vous" mit eigenem Audit, etc.).

## 4c.11 Vollstaendigkeits-Statistik am Ende von Schicht 4c

| Metrik | Wert |
|--------|------|
| Strings mit ueberschriebener Slot-Max-Laenge | N |
| Strings mit `translatable="false"` | N |
| Strings mit `xliff:g`-Tags | N |
| Strings mit XML-Kommentar (Uebersetzer-Note) | N / Gesamt (X%) |
| Plural-Keys gesamt | N |
| Plural-Sprachen mit fehlenden Quantitaeten | N (Liste) |
| Strings mit HTML-Tags | N |
| Strings mit CDATA | N |
| Format-Strings mit dokumentierter Argument-Semantik | N / Gesamt (X%) |
| Glossar-Begriffe identifiziert | N |
| Glossar-Inkonsistenzen | N |
| Regional-Varianten-Paare | N |
| Identische Regional-Varianten (Verdacht fehlende Lokalisierung) | N |
| Du/Sie-Mischanrede in Deutsch | JA/NEIN |

## 4c.12 Typische Fehlerquellen

- **App-Name in strings.xml uebersetzt**: `app_name` sollte fast immer `translatable="false"` sein — wenn nicht, wird die App in jeder Sprache anders heissen.
- **Format-Argument-Reihenfolge**: `%1$s` und `%2$s` sind POSITIONAL — wenn der Uebersetzer `%s` schreibt statt `%1$s`, bricht die Anzeige.
- **HTML-Entities verloren**: `&amp;` wird oft als `&` gespeichert — beim naechsten Build wird XML-Parser meckern.
- **Plural-Default-Quantitaet falsch**: Einige Sprachen brauchen `=0` und `=1` als spezielle Quantitaten, nicht nur `zero` und `one`.
- **`xliff:g`-Namespace nicht deklariert**: Wenn `<xliff:g>` in strings.xml aber `xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"` im `<resources>`-Tag fehlt, Build-Fehler.
- **Inkonsistente Anfuehrungszeichen**: `"..."` vs `„..."` vs `'...'` — pro Sprache typografisch korrekt? (Englisch: `"..."`, Deutsch: `„..."`, Franzoesisch: `« ... »`)

## 4c.13 Was diese Schicht NICHT macht

- Sie **uebersetzt nicht** — sie sammelt nur den Kontext den der Uebersetzungs-Skill braucht.
- Sie **wertet nicht** ob eine Uebersetzung gut ist — sie identifiziert nur was geprueft werden muss.
- Sie ueberlappt nicht mit Layer 4b — Layer 4b liefert das WAS (Wortlaut), Layer 4c liefert das WIE-UEBERSETZBAR-IST (Context).
