---
name: übersetzung
description: Uebersetzt Android strings.xml in alle 30 Locales (29 Sprachen — Portugiesisch zaehlt als pt-BR und pt-PT) aus der mitgelieferten Referenz. Nutze diesen Skill IMMER wenn der Benutzer sagt "uebersetze die Strings", "Uebersetzung starten", "Strings uebersetzen", "starte den Uebersetzungsskill", "uebersetze fuer [App]", "neue Strings uebersetzen", "alle Strings uebersetzen", oder wenn eine App lokalisiert werden soll. Auch bei Varianten wie "mach die App mehrsprachig", "Lokalisierung", "i18n", "internationalisieren", "in andere Sprachen", "uebersetze das". Der Skill arbeitet mit 15 parallelen Workern (Continuous-Spawning nach FIN-023): sobald 1 Worker fertig ist, startet SOFORT der naechste fuer die naechste Sprache — keine Wellengruppen. Verifikation und Commit nach jedem Worker-Abschluss. Funktioniert fuer JEDE Android-App, nicht nur fuer eine bestimmte.
---

# Uebersetzungs-Skill: Android strings.xml in 30 Locales (29 Sprachen)

Dieser Skill uebersetzt die strings.xml einer Android-App in alle 30 Locales — 29 Sprachen,
wobei Portugiesisch als zwei eigenstaendige Varianten gepflegt wird (pt-BR fuer Brasilien
und pt-PT fuer Portugal). Er arbeitet Sprache fuer Sprache, verifiziert jede Uebersetzung
mit fuenf automatischen Validatoren, und committet nach jeder fertigen Sprache. Das Ziel
ist professionelle App-Store-Qualitaet — nicht "gut genug", sondern die bestmoegliche
maschinelle Uebersetzung.

## Skill-Struktur (Progressive Disclosure, Anthropic Best Practice)

Der Skill folgt dem Drei-Ebenen-Pattern: Metadaten (immer geladen) → Body (bei Trigger)
→ Resources (nur bei Bedarf).

```
übersetzung/
├── SKILL.md                          ← Dieses Dokument: Workflow + Phasen + Aufrufe
├── übersetzung-global.md             ← Universal-Prompt + Inhaltsverzeichnis
├── references/
│   └── languages/                    ← Eine Datei pro Locale (27 Stueck)
│       ├── en.md, fr.md, es.md, ...
└── scripts/
    └── validators/                   ← Auto-Fix-Scripts (5 Stueck)
        ├── check_native_digits.py
        ├── check_cjk_punctuation.py
        ├── check_pt_variants.py
        ├── check_apostrophes.py
        ├── check_length_pacing.py
        └── README.md
```

**Warum diese Struktur:** Frueher (vor Refactoring-Stufe-B) lagen alle 27 Sprach-Prompts
in einer Datei (842 Zeilen) und alle Validator-Scripts inline in dieser SKILL.md
(888 Zeilen). Bei jedem Skill-Trigger wurden ~7.000 Token in den Kontext geladen.
Mit der neuen Struktur sind es ~3.000 Token im Standard-Fall — und pro Sprach-Wechsel
laedt der Skill nur die eine Datei die er gerade braucht.

---

## Referenzdatei finden

Die Universal-Referenz `übersetzung-global.md` liegt direkt im Skill-Ordner. Die
sprach-spezifischen Prompts liegen je in `references/languages/[code].md`. Pfade:

```
~/.claude/skills/übersetzung/übersetzung-global.md
~/.claude/skills/übersetzung/references/languages/[code].md
~/.claude/skills/übersetzung/scripts/validators/check_*.py
```

(Auf macOS: `/Users/frank/.claude/skills/...`, auf Windows: `%USERPROFILE%/.claude/skills/...`)

---

## Wann dieser Skill zum Einsatz kommt

- **Neue App lokalisieren**: "Uebersetze die Strings fuer [App-Name]"
- **Neue Strings nach Feature**: "Uebersetze nur die neuen Strings" (nach Feature-Bau)
- **Komplette Neuuebersetzung**: "Uebersetze alle Strings komplett neu"
- **Einzelne Sprache**: "Uebersetze die Strings auf Franzoesisch" (nur eine Sprache)

---

## Phase 1: Erkundung — App und Strings finden

Bevor eine einzige Zeile uebersetzt wird, muss die Umgebung vollstaendig verstanden werden.

### 1.1 App-Verzeichnis finden

Der Benutzer nennt den App-Namen (z.B. "BestJournal Android"). Finde das Verzeichnis:

```
Glob: ~/proggs/[AppName]*/**/strings.xml
```

Wenn mehrere Treffer: den Benutzer fragen welches Projekt gemeint ist.
Das Hauptverzeichnis der App ist das Elternverzeichnis von `app/src/main/res/`.

### 1.2 Quell-Strings lesen

Die Quell-Strings liegen in:
```
[APP_DIR]/app/src/main/res/values/strings.xml
```

Diese Datei KOMPLETT lesen. Sie ist die Quelle fuer ALLE Uebersetzungen.

### 1.3 Umfang bestimmen — Was wird uebersetzt?

| Benutzer sagt | Aktion |
|---------------|--------|
| "nur die neuen Strings" | `git diff` auf strings.xml ausfuehren, nur NEUE `<string>`-Eintraege extrahieren |
| "alle Strings" / "komplette Uebersetzung" / neue App | Gesamte strings.xml uebersetzen |
| "nur [Sprache]" | Nur die genannte Sprache uebersetzen, dann fertig |
| Nichts Spezifisches | Aus dem Kontext ableiten. Wenn gerade neue Strings hinzugefuegt wurden: nur die neuen. Sonst nachfragen. |

**Neue Strings per git diff erkennen:**
```bash
git diff HEAD~1 -- [APP_DIR]/app/src/main/res/values/strings.xml | grep "^+" | grep "<string\|<plurals\|<string-array"
```
Wenn kein sinnvoller Diff: die letzten 1-3 Commits pruefen oder den Benutzer fragen.

### 1.4 Universal-Prompt jetzt laden (Sprach-Prompts spaeter)

**JETZT lesen:** Die Datei `~/.claude/skills/übersetzung/übersetzung-global.md` mit
dem Read-Tool oeffnen. Abschnitt 1 (Universal-Prompt) im Gedaechtnis behalten — er
wird in Phase 2 fuer JEDE Sprache als Basis benutzt.

**JETZT NICHT lesen:** Die sprach-spezifischen Dateien in
`~/.claude/skills/übersetzung/references/languages/[code].md`. Sie werden **erst in
Phase 2 Schritt A geladen** — pro Sprache nur die eine die gerade dran ist. Das spart
ca. 95% Kontext-Verbrauch im Vergleich zu "alle 30 vorab".

Wenn du also gerade Franzoesisch uebersetzt, liest du `fr.md`. Wenn du danach
Persisch uebersetzt, liest du `fa.md` (und vergisst `fr.md` aus deinem Arbeitskontext).

### 1.5 App-Informationen sammeln

Fuer die Platzhalter im Universal-Prompt werden diese Informationen benoetigt:

| Platzhalter | Woher | Beispiel |
|-------------|-------|---------|
| `[APP_NAME]` | Benutzer-Eingabe oder `app_name` in strings.xml | "BestJournal" |
| `[APP_DESCRIPTION]` | README.md der App oder Benutzer | "Personal journaling and diary app" |
| `[APP_TONE]` | Aus dem Stil der deutschen Strings ableiten | "Warm, encouraging, personal" |
| `[TARGET_LANGUAGE]` | Wird pro Sprache gesetzt | "French" |
| `[LOCALE_CODE]` | Aus dem Inhaltsverzeichnis | "fr-FR" |
| `[REGISTER]` | Aus dem sprach-spezifischen Prompt | "Informal tu" |

Wenn `[APP_DESCRIPTION]` oder `[APP_TONE]` nicht klar sind: kurz den Benutzer fragen
oder aus der README.md der App ableiten. Das dauert 10 Sekunden und verbessert jede
Uebersetzung — nicht ueberspringen.

### 1.6 Zielverzeichnisse pruefen

Fuer jede Sprache pruefen ob das Zielverzeichnis existiert:
```
[APP_DIR]/app/src/main/res/values-[android-locale]/strings.xml
```

Locale-Mapping (Sprach-Datei-Code → Android-Verzeichnis):
```
en        → values-en        | fr        → values-fr        | es        → values-es
pt-BR     → values-pt-rBR    | pt-PT     → values-pt-rPT    | it        → values-it
nl        → values-nl        | pl        → values-pl        | ru        → values-ru
uk        → values-uk        | tr        → values-tr        | ja        → values-ja
ko        → values-ko        | zh-Hans   → values-zh-rCN    | zh-Hant   → values-zh-rTW
ar        → values-ar        | he        → values-iw        | fa        → values-fa
hi        → values-hi        | th        → values-th        | id        → values-in
vi        → values-vi        | bn        → values-bn        | te        → values-te
mr        → values-mr        | ta        → values-ta        | ur        → values-ur
gu        → values-gu        | kn        → values-kn        | ml        → values-ml
```

Hinweis: Hebraeisch verwendet auf Android das alte Locale-Kuerzel `iw` (Legacy-ISO-Code
von 1989, nicht das aktuelle `he` aus ISO 639-1). Android folgt hier dem Java-Standard.

Fehlende Verzeichnisse erstellen. Bei bestehenden Dateien: nur die zu uebersetzenden
Strings einfuegen/aktualisieren, nicht die gesamte Datei ueberschreiben (es sei denn,
es ist eine Komplettuebersetzung).

### 1.7 Dem Benutzer den Plan zeigen

Bevor die Uebersetzung beginnt, eine kurze Zusammenfassung ausgeben:

```
Uebersetzungsplan:
- App: [APP_NAME] in [APP_DIR]
- Quell-Strings: [Anzahl] Strings aus values/strings.xml
- Umfang: [alle / nur neue (N Stueck)]
- Locales: 30 (en, fr, es, pt-BR, pt-PT, it, nl, pl, ru, uk, tr, ja, ko,
  zh-Hans, zh-Hant, ar, he, fa, hi, th, id, vi, bn, te, mr, ta, ur, gu, kn, ml)
  Hinweis: 29 Sprachen, Portugiesisch zaehlt als 2 eigenstaendige Varianten.
- Vorgehen: Sprache fuer Sprache, mit Verifikation und Commit nach jeder Sprache

Starte jetzt mit Englisch...
```

---

## Continuous-Spawning-Pattern fuer Uebersetzungen (FIN-023 Frank-Direktive 2026-05-18)

PFLICHT-Architektur fuer Multi-Sprachen-Uebersetzungen:

1. **15 Worker IMMER parallel** (nicht 10, nicht sequentiell)
2. **Continuous-Spawning**: sobald 1 Worker fertig → SOFORT naechster Worker
   fuer die naechste Sprache. NIE auf alle 15 warten bis eine neue Welle startet.
3. **Token-Cap 100k pro Worker** (FIN-004)
4. **Vordergrund-Sichtbarkeit** (FIN-028):
   - Beim Start der ersten 15 Worker ein klarer Status-Block ausgeben:
     ```
     Welle 1 (15 Workers): ar, bn, en, es, fr, gu, hi, it, ja, kn, ko, ml,
     mr, nl, pl — alle gestartet
     ```
   - Pro Worker: descriptive `description`-Feld
     z.B. `"Translate: ar — 31 Strings × Devanagari-Validierung"`
   - Live-Stats nach jeder Fertig-Notification:
     `"X von 30 fertig, Y noch laufend, naechste Sprache: [code]"`

5. **Bei 30 Locales total:**
   - Start: 15 Worker gleichzeitig
   - Nach erster Fertig-Notification: 1 neuer Worker (16. Locale) SOFORT
   - Nach zweiter Fertig-Notification: 1 neuer Worker (17. Locale) SOFORT
   - ... bis alle 30 gestartet
   - Sobald alle 30 gestartet: nur noch warten + commit pro Worker-Notification

Frank-Originalwortlaut 2026-05-18:
"Du laesst immer 15 Subagents gleichzeitig laufen. Das beim Roentgenskill,
das gleiche beim Strings-Creator-Skill und das gleiche beim Uebersetzer-Skill.
Sobald 1 Worker fertig: SOFORT naechster fuer die naechste Sprache."

KEIN sequentielles "alle 15 fertig, dann Welle 2" — das ist Verschwendung.

---

## Phase 2: Uebersetzungs-Schleife — 15 parallele Worker (Continuous-Spawning)

Diese Phase ist das Herzstück des Skills. Alle 30 Locales werden über 15 parallele
Worker abgearbeitet — mit sofortigem Nachrücken sobald ein Worker fertig ist.

Die Reihenfolge der Locales folgt dem Inhaltsverzeichnis in `übersetzung-global.md`:

```
en → fr → es → pt-BR → pt-PT → it → nl → pl → ru → uk → tr → ja → ko → zh-Hans →
zh-Hant → ar → he → fa → hi → th → id → vi → bn → te → mr → ta → ur → gu → kn → ml
```

Geografisch sortiert: Europa-West → Europa-Ost → Ost-Asien → Naher Osten/RTL (ar, he, fa) →
Indien (hi, ur, bn, te, mr, ta, gu, kn, ml) → Suedost-Asien (th, id, vi).

**WICHTIG:** pt-BR und pt-PT sind separate Locales und MUESSEN beide uebersetzt werden.
pt-PT wird haeufig vergessen — der bidirektionale PT-Varianten-Check (Validator 3)
faengt Kreuz-Kontaminationen, aber er ersetzt nicht die Pflege von pt-PT als
eigenstaendige Sprache.

### Schritt A — Uebersetzen (erster Durchlauf)

1. **Sprach-Prompt laden**: Die Datei `references/languages/[code].md` lesen.
   Sie enthaelt den Code-Block mit den sprach-spezifischen Regeln zwischen
   ` ``` `-Markierungen. Diesen Code-Block extrahieren.

2. **Prompt zusammenbauen**: Universal-Prompt (aus `übersetzung-global.md`) +
   extrahierter sprach-spezifischer Block. Alle Platzhalter befuellen. Den
   `REGISTER`-Wert aus dem sprach-spezifischen Block uebernehmen (z.B.
   "Informal tu" fuer Franzoesisch).

3. **Uebersetzen**: Die Quell-Strings mit dem zusammengebauten Prompt uebersetzen.
   Dabei den gesamten Prompt als Kontext im Kopf behalten — jede Warnung, jeder
   Vokabel-Hinweis, jede Plural-Regel ist wichtig.

4. **Ergebnis schreiben**: Die uebersetzten Strings in die Zieldatei schreiben:
   `values-[android-locale]/strings.xml` (siehe Locale-Mapping in 1.6).

   Bei Teiluebersetzung (nur neue Strings): Die neuen Strings in die bestehende
   Datei einfuegen, an der gleichen Position wie in der Quelldatei. Die bestehenden
   Uebersetzungen NICHT veraendern.

   Bei Komplettuebersetzung: Die gesamte Datei schreiben mit XML-Header:
   `<?xml version="1.0" encoding="utf-8"?>`

**Wichtig waehrend der Uebersetzung — mentale Checkliste:**
- Behalte die Textlaenge im Blick. Wenn die Uebersetzung >40% laenger wird als das
  Original: `<!-- SHORTER: [Alternative] -->` hinzufuegen.
- Benutze konsistent die gleichen Begriffe fuer die gleichen Konzepte.
- Beachte die Plural-Regeln der Zielsprache — fehlende Formen crashen die App!
- Bei RTL-Sprachen (Arabisch, Urdu): BiDi-Kontrolle beachten.
- Alle Platzhalter (%s, %d, %1$s) muessen EXAKT erhalten bleiben.

### Schritt B — Verifizieren (zweiter Durchlauf, PFLICHT)

Der zweite Durchlauf ist keine Option — er ist Pflicht. Er faengt echte Fehler,
die im ersten Durchlauf entstehen.

1. **Sprach-Prompt erneut laden**: Die Datei `references/languages/[code].md`
   NOCHMAL lesen. Nicht aus dem Gedaechtnis arbeiten — frisch laden, damit keine
   Warnung vergessen wird.

2. **11 systematische Checks**:

   | # | Check | Was geprueft wird | Wie pruefen |
   |---|-------|------------------|-------------|
   | 1 | Vollstaendigkeit | Jeder Quell-String hat eine Uebersetzung | Anzahl Strings vergleichen |
   | 2 | Platzhalter | %s, %d, %1$s exakt wie im Original | Grep nach `%` in Quelle und Ziel |
   | 3 | XML-Struktur | Tags korrekt geoeffnet/geschlossen, Escaping (\', \") | XML validieren |
   | 4 | Plural-Formen | Alle erforderlichen `quantity`-Formen vorhanden | Gegen Prompt-Vorgabe pruefen |
   | 5 | Sprach-Warnungen | Spezifische LLM-Pitfalls aus dem Prompt | Gegen Warnung-Liste pruefen |
   | 6 | Konsistenz | Gleiche Begriffe fuer gleiche Konzepte | Stichprobe der Kern-Vokabeln |
   | **7** | **Native-Ziffern** (indische Sprachen) | Keine bengalischen/devanagari/tamilischen etc. Ziffern | `scripts/validators/check_native_digits.py` |
   | **8** | **Full-width Punctuation** (CJK-Sprachen) | Keine half-width `, . ! ? : ; ( )` nach CJK-Zeichen | `scripts/validators/check_cjk_punctuation.py` |
   | **9** | **PT-Varianten-Trennung** (pt-BR ↔ pt-PT) | Keine PT-BR-Vokabeln in pt-PT und umgekehrt | `scripts/validators/check_pt_variants.py` |
   | **10** | **Apostroph-Escape** (FR/IT/PT-BR/PT-PT/EN) | Unescaped `'` in `<string>`-Werten — bricht den AAPT-Build | `scripts/validators/check_apostrophes.py` |
   | **11** | **Laengen-Pacing** (alle Sprachen) | Uebersetzung >+40% laenger als Source ohne SHORTER-Alternative | `scripts/validators/check_length_pacing.py` |

3. **Validator-Scripts aufrufen** (Checks 7-11):

   Die Validatoren sind eigenstaendige Python-Scripts in `~/.claude/skills/übersetzung/scripts/validators/`.
   Sie folgen einer einheitlichen Aufruf-Konvention: `--locale` (was die Sprache ist)
   und `--path` (Zielpfad zur strings.xml). Exit-Code 0 = OK, 1 = Probleme die manuelle
   Korrektur brauchen, 2 = Fehler. Alle haben Auto-Fix wo das sinnvoll ist
   (Native-Ziffern, CJK-Punctuation, Apostroph), nur Bericht wo Kontext-Wissen
   gebraucht wird (PT-Varianten, Laengen-Pacing).

   **Einheitliche CLI-Konvention:** Alle 5 Scripts nutzen `--locale <sprach-datei-code>`
   (also `fr`, `pt-BR`, `zh-Hans`, `fa`, `he`, `vi`). Intern wandeln sie den Code falls
   noetig in Android-Verzeichnis-Format um (z.B. `zh-Hans` → `values-zh-rCN`).
   Rueckwaerts-kompatibel: alte Aufrufe mit `--locale zh-rCN` oder `--variant pt-PT`
   funktionieren weiter.

   **Welcher Validator fuer welche Sprache:**

   ```bash
   # Check 7 — Native-Ziffern: bn, hi, mr, te, ta, gu, kn, ml, ur, fa
   python3 ~/.claude/skills/übersetzung/scripts/validators/check_native_digits.py \
       --locale bn --path [APP_DIR]/app/src/main/res/values-bn/strings.xml

   # Check 8 — CJK-Punctuation: zh-Hans, zh-Hant, ja
   python3 ~/.claude/skills/übersetzung/scripts/validators/check_cjk_punctuation.py \
       --locale ja --path [APP_DIR]/app/src/main/res/values-ja/strings.xml

   # Check 9 — PT-Varianten: pt-PT und pt-BR (BEIDE!)
   python3 ~/.claude/skills/übersetzung/scripts/validators/check_pt_variants.py \
       --locale pt-PT --path [APP_DIR]/app/src/main/res/values-pt-rPT/strings.xml

   # Check 10 — Apostroph: fr, it, pt-BR, pt-PT, en
   python3 ~/.claude/skills/übersetzung/scripts/validators/check_apostrophes.py \
       --locale fr --path [APP_DIR]/app/src/main/res/values-fr/strings.xml

   # Check 11 — Laengen-Pacing: ALLE Sprachen (besonders empfohlen fuer pl, ru, uk, fr, es,
   #                                          pt-BR, pt-PT, it, tr, fa, bn, te, mr, ta, gu, kn, ml)
   python3 ~/.claude/skills/übersetzung/scripts/validators/check_length_pacing.py \
       --source [APP_DIR]/app/src/main/res/values/strings.xml \
       --target [APP_DIR]/app/src/main/res/values-pl/strings.xml \
       --locale pl
   ```

   Bei Exit-Code 1 (Probleme gefunden): Den Bericht lesen und die genannten Strings
   im LLM-Verbesserungs-Pass nachkorrigieren. Bei Exit-Code 0: weiter zu Schritt C.

   **Defense in Depth** pro Validator:
   - Schicht 1 (praeventiv): Universal-Prompt warnt im Vorfeld
   - Schicht 2 (reaktiv): Script-Aufruf prueft objektiv
   - Schicht 3 (selbstheilend): Auto-Fix wo machbar (atomares Schreiben via tempfile)

   Details zu jedem Script: `scripts/validators/README.md`.

4. **Check 5 im Detail — Sprach-spezifische Warnungen:**
   Das ist der wichtigste manuelle Check. Fuer jede Sprache gibt es spezifische Gefahren:

   | Sprache | Was im zweiten Durchlauf BESONDERS pruefen |
   |---------|-------------------------------------------|
   | fr | Leerzeichen vor : ; ! ? vorhanden? Guillemets statt Anfuehrungszeichen? |
   | es | Kein Voseo? Kein Usted? Keine Castilian-Begriffe? |
   | pt-BR | Keine PT-PT-Woerter? (utilizador, aplicacao, guardar, definicoes) — Check 9 deckt das ab |
   | pt-PT | Keine PT-BR-Woerter? Gerundium "estar a" statt "-ando"? — Check 9 deckt das ab |
   | nl | Keine German→Dutch False Friends? (wie≠how, mogen≠moegen, meer≠Meer) |
   | uk | KEINE russischen Woerter? (Сохранить→Зберегти, Настройки→Налаштування) |
   | zh-Hans | Keine Traditional-Zeichen? Keine Taiwan-Vokabeln? |
   | zh-Hant | Keine Simplified-Zeichen? Keine Mainland-Vokabeln? |
   | ja | Konsistente Hoeflichkeitsstufe (です/ます)? Kein Keigo-Mix? |
   | ko | Konsistente Sprechebene (해요체)? Keine 해라체? |
   | ar | Alle 6 Plural-Formen? Gender-Agreement? Keine Dialekt-Woerter? |
   | he | Niqqud weg? Gender-Agreement (masculine default)? RTL-Marker korrekt? |
   | fa | Keine Arabisch-Vokabeln eingestreut? Persische Buchstaben (پ چ ژ گ) korrekt? Arabic-numerals 0-9 statt ۰-۹? |
   | tr | Vokalharmonie korrekt? Keine Plural-Suffixe nach Zahlen? |
   | ru | Richtiger Aspekt (Сохранить, nicht Сохранять)? Alle 4 Plurale? |
   | hi | Kein unkontrolliertes Hinglish? Register konsistent (आप)? |
   | th | Keine Hoeflichkeitspartikel (ครับ/ค่ะ) in Buttons? |
   | vi | Alle Tonzeichen (`ợ ẫ ừ`) korrekt? Keine Telex-Artefakte (`aa` statt `â`)? |
   | ta | Keine Sanskrit-Lehnwoerter? Natives Tamil fuer persoenliche Begriffe? |
   | mr | Keine Hindi-Woerter? Drei Genera korrekt? |
   | bn | Keine Hindi-Leakage? Keine Devanagari-Zeichen? |
   | te | Suffix-Ketten zu lang (>15 Silben)? Sanskrit-Mix? SHORTER-Alternativen bei langen Begriffen? |
   | gu | Korrekte Gujarati-Unicode-Range (U+0A80–U+0AFF)? Keine Devanagari-Zeichen (mit shirorekha = horizontale Linie oben) eingestreut? |
   | kn | Korrekte Kannada-Unicode-Range (U+0C80–U+0CFF)? Kein Telugu-Mix (U+0C00–U+0C7F sieht aehnlich aus)? |
   | pl | Alle 4 Plural-Formen (one/few/many/other) vorhanden? "many" nicht mit "other" verwechselt? Geschlecht in Vergangenheit (zapisalem/zapisalam) vermieden? |
   | ml | Vereinfachte Orthographie? Suffix-Ketten korrekt? |

5. **Korrekturen anwenden**: Wenn Probleme gefunden werden, die betroffenen Strings
   korrigieren. Dabei den vollstaendigen Prompt-Kontext beruecksichtigen.

6. **Verbesserungen melden**: Dem Benutzer kurz berichten was im zweiten Durchlauf
   verbessert wurde. Format:
   ```
   Verifikation [Sprache]: [N] Verbesserungen
   - [Was verbessert wurde, z.B. "2 fehlende Plural-Formen ergaenzt"]
   - [z.B. "1 Russismus in ukrainischer Uebersetzung korrigiert"]
   ```
   Wenn nichts gefunden wurde: "Verifikation [Sprache]: Alles korrekt, keine Aenderungen."

### Schritt C — Speichern und Weiter

1. **Commit erstellen**:
   ```bash
   git add [APP_DIR]/app/src/main/res/values-[locale]/strings.xml
   git commit -m "#NNN - Translate strings to [Language] ([locale])"
   ```
   Die Commit-Nummer ermitteln wie in CLAUDE.md beschrieben (fortlaufend).

2. **Pushen**:
   ```bash
   git fetch origin && git rebase origin/main && git push
   ```
   Bei unstaged Changes: `git stash` vor Rebase, danach `git stash pop`.

3. **Status melden**:
   ```
   ✓ [Locale] ist jetzt fertig. ([N]/30)
   Naechstes Locale: [naechstes Locale]
   ```

4. **Zur naechsten Sprache** — zurueck zu Schritt A.

---

## Phase 3: Abschluss

Nachdem alle Sprachen fertig sind:

### 3.1 Zusammenfassung ausgeben

```
Uebersetzung abgeschlossen!

| # | Sprache | Code | Strings | Verifikation | Status |
|---|---------|------|---------|-------------|--------|
| 1 | Englisch | en | [N] | [N] Fixes | Fertig |
| 2 | Franzoesisch | fr | [N] | Alles OK | Fertig |
| ... | ... | ... | ... | ... | ... |

Gesamt: [N] Strings in 30 Locales (29 Sprachen) uebersetzt.
Commits: #[erste] bis #[letzte]
```

### 3.2 Bekannte Einschraenkungen nennen

Wenn bestimmte Sprachen wahrscheinlich eine menschliche Pruefung brauchen,
das explizit nennen:
- Ukrainisch (Russismus-Risiko — LLMs streuen russische Vokabeln ein)
- Malayalam (komplexe Morphologie, niedrige LLM-Qualitaet)
- Kannada (schwache LLM-Performance)
- Thai (Wort-Segmentierung unsichtbar — keine Leerzeichen zwischen Woertern)
- Vietnamesisch (Diakritika-Verlust — LLMs lassen Tonzeichen weg, z.B. "tam" vs "tâm" vs "tám")
- Persisch (Arabisch-Verwechslung — LLMs nutzen arabische Buchstaben ي/ك statt persisches ی/ک)
- Hebraeisch (Gender-Konsistenz — Verben aendern Form je nach Geschlecht des Subjekts)

---

## Sonderfaelle

### Nur eine einzelne Sprache uebersetzen

Wenn der Benutzer sagt "uebersetze nur auf Franzoesisch":
- Nur diese eine Sprache durchlaufen (Schritt A + B + C)
- Dann fertig, nicht alle 30 machen

### Bestehende Uebersetzungen aktualisieren (nur neue Strings)

Bei "nur die neuen Strings":
- Die bestehende Ziel-Datei lesen
- Nur die NEUEN Strings hinzufuegen (am Ende oder an der korrekten Position)
- KEINE bestehenden Uebersetzungen veraendern
- Bei der Verifikation: nur die neuen Strings pruefen

### App hat keine strings.xml

Wenn die App keine strings.xml hat oder sie leer ist:
- Dem Benutzer sagen und fragen ob die Strings erst erstellt werden sollen
- NICHT versuchen, aus dem Code Strings zu extrahieren — das ist ein anderer Skill

### Uebersetzung einer einzelnen Sprache wiederholen

Wenn der Benutzer sagt "Franzoesisch nochmal neu":
- Die bestehende Datei komplett ueberschreiben
- Volles Programm (Uebersetzen + Verifizieren + Commit)

### Resume nach Abbruch

Wenn der Benutzer sagt "mache weiter mit der Uebersetzung" oder "geh weiter wo wir
aufgehoert haben":
- `git log --oneline -30` lesen und alle "#NNN - Translate strings to [Language]"-Commits
  auflisten
- Aus dem Inhaltsverzeichnis ableiten welche Locales noch fehlen
- Mit der naechsten fehlenden Sprache fortfahren (Phase 2 Schritt A)
- Falls eine Sprache halb fertig ist (Datei existiert aber kein Commit): den Benutzer
  fragen ob neu uebersetzen oder den vorhandenen Stand committen

### Fehlende Sprach-Datei

Wenn `references/languages/[code].md` nicht existiert (z.B. weil die Sprache aus dem
Skill entfernt wurde, oder Tippfehler im Code):
- Read-Tool gibt einen klaren Fehler — nicht still ueberspringen
- Dem Benutzer melden: "Die Sprach-Datei `[code].md` fehlt. Soll ich diese Sprache
  ueberspringen, oder soll ich eine neue Sprach-Datei mit Standard-Inhalt erstellen?"
- Bei Erstellung: Template aus `HOW_TO_ADD_LANGUAGE.md` als Startpunkt

### Validator-Fehler unterscheiden

Validator-Scripts geben drei Exit-Codes:
- **0**: Alles OK (oder erfolgreich auto-gefixt — kein Eingriff noetig)
- **1**: Probleme gefunden die manuelle Korrektur brauchen — Bericht lesen, betroffene
  Strings im LLM-Verbesserungs-Pass nachkorrigieren, dann den Validator erneut laufen
- **2**: Echter Fehler (Datei nicht lesbar, falsches Locale, Python-Crash) — Skill
  MUSS abbrechen, Benutzer informieren, NICHT die Sprache als "fertig" markieren

Bei Exit-Code 2 NIEMALS committen — die Datei ist in unbekanntem Zustand.

### Pfade mit Leerzeichen oder Sonderzeichen

Beim Aufruf der Validator-Scripts MUSS der `--path`-Wert in doppelte Anfuehrungszeichen
gesetzt werden, falls `[APP_DIR]` Leerzeichen oder Shell-Sonderzeichen enthaelt:

```bash
# Falsch (bricht bei Leerzeichen in /Users/John Doe/...):
python3 .../check_apostrophes.py --locale fr --path [APP_DIR]/app/.../values-fr/strings.xml

# Richtig:
python3 .../check_apostrophes.py --locale fr --path "[APP_DIR]/app/.../values-fr/strings.xml"
```

Praktisch: Variable `APP_DIR` immer in `"$APP_DIR"` einfuegen, nie nackt.

---

## Wartung — neue Sprache hinzufuegen

Wenn der Benutzer eine neue Sprache zum Skill hinzufuegen will (z.B. "fuege Malaiisch
hinzu"), siehe **`HOW_TO_ADD_LANGUAGE.md`** (im Skill-Ordner). Dort steht die
11-Stellen-Checkliste mit konkretem Beispiel. Ohne diese Checkliste werden typischerweise
1-3 Stellen vergessen — was zu inkonsistentem Skill-Verhalten fuehrt.

---

## Umlaut-Pflicht (FIN-011 + FIN-027 Frank-Direktive 2026-05-18)

### Grundregel

In der **deutschen Quell-strings.xml** und in ALLEN deutschen Texten innerhalb der
App MÜSSEN echte Umlaute verwendet werden: ä ö ü Ä Ö Ü ß. NIEMALS die
ASCII-Substitution ae/oe/ue/ss. Dies gilt auch für:
- Alle Status-Meldungen die der Skill ausgibt (Deutsch)
- Alle Kommentare in XML-Dateien auf Deutsch
- Alle Audit-Berichte und Doku-Outputs des Skills
- Den Skill-Body selbst (diese Datei)

### Quell-Check vor der Übersetzung (FIN-027 NEU)

Bevor die erste Sprache gestartet wird: die deutsche Quelldatei auf
ASCII-Umlaut-Substitutionen prüfen.

```bash
# Grep auf häufige ASCII-Substitutionen in deutschen Strings
grep -n '\b\(fuer\|Groesse\|koennen\|muessen\|waere\|Aenderung\|uebersetzen\|oeffentlich\|Uebersicht\)\b' \
  "[APP_DIR]/app/src/main/res/values/strings.xml"
```

Wenn Treffer gefunden werden:
- **STOP** — Quelle ist fehlerhaft
- Dem Orchestrator (Haupt-Claude) melden: `"UMLAUT-FEHLER in Quell-strings.xml: [Zeilen]"`
- NICHT mit der Übersetzung beginnen bis die Quelle repariert ist
- Grund: Fehler in der Quelle pflanzen sich in alle 30 Locales fort

### Output-Check nach jeder Übersetzung (FIN-027 NEU)

Nach dem Schreiben jeder Zieldatei: einen abschließenden Grep auf deutsche
Teilbereiche der Ausgabe (z.B. Kommentare, Transliteration-Hinweise):

```python
import re

def check_umlaut_substitutions(path):
    """
    Prüft ob ASCII-Substitutionen (ae/oe/ue/ss) in deutschen Kommentaren
    oder Metadaten-Bereichen der Zieldatei vorhanden sind.
    Betrifft nicht den übersetzten Text (der ist in der Zielsprache).
    """
    pattern = re.compile(
        r'\b(fuer|Groesse|koennen|muessen|waere|wird|werde|uebersetzen|'
        r'oeffentlich|Aenderung|Uebersicht|Haeufig|ausserdem)\b'
    )
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    hits = [(i+1, line.strip()) for i, line in enumerate(lines)
            if pattern.search(line)]
    if hits:
        print(f'UMLAUT-WARNUNG in {path}:')
        for lineno, text in hits:
            print(f'  Zeile {lineno}: {text}')
        return False
    return True
```

Wenn Treffer in deutschen Kommentaren/Metadaten: Korrigieren, dann weiter.
Bei übersetzen Inhalten (Zielsprache ist nicht Deutsch): ignorieren — der
Pattern greift nur auf Deutsch.

### Für Zielsprachen

Für Zielsprachen gelten ihre eigenen Konventionen:
- Französisch: ç, é, è, ê, à, î, ô, ù
- Spanisch: ñ, á, é, í, ó, ú, ¿, ¡
- Polnisch: ą, ć, ę, ł, ń, ó, ś, ż, ź
- Türkisch: ç, ğ, ı, İ, ö, ş, ü
- usw. (je nach Zielsprache die sprachspezifischen Sonderzeichen verwenden)

**Pflicht-Grep nach jedem Edit auf die deutsche Quell-strings.xml:**

```bash
grep -n '\b\(fuer\|Groesse\|koennen\|muessen\|waere\|wird\|werde\)\b' \
  "[APP_DIR]/app/src/main/res/values/strings.xml"
```

Wenn Treffer gefunden werden die NICHT englische Wörter sind (z.B. `value`, `queue`,
`true`, `werden`): Den betroffenen Bereich mit echten Umlauten neu schreiben.
Vor dem Commit muss dieser Check sauber durchlaufen.

---

## XML-Validierungs-Pflicht (FIN-016)

### VOR jedem Edit auf eine Ziel-strings.xml (PFLICHT)

1. Die Zieldatei komplett lesen.
2. XML-Struktur per Python validieren:

```python
import xml.etree.ElementTree as ET
try:
    ET.parse('[PFAD_ZUR_DATEI]')
    print('XML OK')
except ET.ParseError as e:
    print(f'VORBESTEHENDER XML-FEHLER: {e}')
    # STOPP — vorher den Schaden melden, NICHT fortsetzen
```

Wenn die Datei schon vor dem Edit kaputt ist: **SOFORT stoppen und dem Benutzer
den vorbestehenden Schaden melden.** Niemals auf einer bereits kaputten Datei weiterarbeiten.

3. Insertion-Punkte für neue Strings **IMMER** zwischen vollständig geschlossenen
   `</string>`-Tags wählen. Niemals innerhalb des Werts eines anderen Tags inserieren.

### NACH jedem Edit auf eine Ziel-strings.xml (PFLICHT)

Erneut per Python validieren:

```python
import xml.etree.ElementTree as ET
try:
    ET.parse('[PFAD_ZUR_DATEI]')
    print('XML OK — Edit sauber')
except ET.ParseError as e:
    print(f'XML BESCHÄDIGT DURCH EDIT: {e}')
    # SOFORT zurückrollen — NICHT committen, NICHT fortsetzen
```

Wenn nach dem Edit ein Parse-Fehler entsteht: **Edit sofort zurückrollen (Datei auf
vorherigen Stand zurückschreiben), dann Fehler dem Benutzer melden.** Auf keinen Fall
mit einer kaputten XML-Datei committen oder zur nächsten Sprache weitergehen.

### Verbot: Tags-in-Tags (FIN-016)

Niemals ein `<string>`-Tag innerhalb des Werts eines anderen `<string>`-Tags einbauen.
Das zerstört die gesamte XML-Datei für den AAPT-Compiler.

**Pflicht-Check nach jedem Edit:**

```python
import re
with open('[PFAD_ZUR_DATEI]', 'r', encoding='utf-8') as f:
    content = f.read()
# Erkennt verschachtelte <string>-Tags
if re.search(r'<string[^>]*>[^<]*<string', content):
    print('KRITISCH: Verschachtelte <string>-Tags gefunden — sofort zurückrollen!')
```

Wenn dieser Check anschlägt: **Sofort zurückrollen.** Dies ist die häufigste
App-zerstörende Fehlklasse beim Einfügen von Übersetzungen.

---

## Apostroph-Escape-Pflicht (FIN-017 + FIN-018)

Unescapte Apostrophe (`'`) in `<string>`-Werten verursachen einen AAPT-Build-Fehler
der die gesamte App unbaubar macht. Betroffen sind besonders: fr, it, es, en,
pt-rBR, pt-rPT, uk und alle anderen Sprachen mit romanischen Lehnwörtern.

### Pflicht-Schritt nach dem Schreiben jeder Zieldatei

Nach dem Schreiben in `values-{fr,it,es,en,pt-rBR,pt-rPT,uk,...}/strings.xml`
diesen Python-Helper ausführen:

```python
import re, os

def escape_apostrophes_in_xml_strings(path):
    """
    Escaped alle unescapten Apostrophe in <string>-Werten einer Android strings.xml.
    Bereits escapte \\' werden nicht doppelt escaped.
    Gibt True zurück wenn Änderungen vorgenommen wurden, sonst False.
    """
    with open(path, 'r', encoding='utf-8') as f:
        txt = f.read()
    # Pattern: <string ...>(WERT)</string>
    pat = re.compile(r'(<string[^>]*>)([^<]*)(</string>)', re.DOTALL)
    def fix_value(m):
        opening, value, closing = m.group(1), m.group(2), m.group(3)
        new_value = []
        i = 0
        while i < len(value):
            c = value[i]
            if c == '\\' and i+1 < len(value):
                # Bereits escaptes Zeichen — unverändert übernehmen
                new_value.append(c)
                new_value.append(value[i+1])
                i += 2
            elif c == "'":
                new_value.append("\\'")
                i += 1
            else:
                new_value.append(c)
                i += 1
        return opening + ''.join(new_value) + closing
    new_txt = pat.sub(fix_value, txt)
    if new_txt != txt:
        # Atomares Schreiben: temp → rename verhindert Datei-Korruption bei Abbruch
        import tempfile
        dir_name = os.path.dirname(path)
        with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp',
                                         delete=False, encoding='utf-8',
                                         newline='\n') as tmp:
            tmp.write(new_txt)
            tmp_path = tmp.name
        os.replace(tmp_path, path)
        return True
    return False

# Aufruf-Beispiel:
# changed = escape_apostrophes_in_xml_strings('[APP_DIR]/app/src/main/res/values-fr/strings.xml')
# print('Apostrophe gefixt' if changed else 'Alles bereits korrekt escaped')
```

**Wann aufrufen:** Nach jedem Schreiben einer `strings.xml` — vor dem XML-Validierungs-Check
(Schritt NACH dem Edit, siehe oben) und vor dem Commit.

**Reihenfolge der Checks nach jedem Sprach-Edit:**
1. `escape_apostrophes_in_xml_strings(pfad)` — Apostrophe escapen
2. Tags-in-Tags-Check — Verschachtelungs-Prüfung
3. XML-Parse-Check — Strukturelle Gültigkeit
4. Erst wenn alle 3 grün: committen

---

## Qualitaets-Prinzipien

Diese Prinzipien erklaeren WARUM der Skill so arbeitet wie er arbeitet:

### Warum 15 parallele Worker statt sequentiell? (FIN-023)

15 Worker parallel maximieren den Durchsatz bei gleichbleibender Qualität. Jeder
Worker bekommt seinen eigenen vollständigen Prompt-Kontext (sprach-spezifische Datei
+ Universal-Prompt) — Parallelität geht nicht auf Kosten der Verifikation.
Continuous-Spawning stellt sicher, dass keine CPU-Zeit auf "warten bis Welle fertig"
verloren geht. Frank-Direktive 2026-05-18: "Sobald 1 Worker fertig: SOFORT nächster."

**Vorher (sequentiell, 30 Sprachen):** 30 × ~3 Min = ~90 Min Gesamtdauer
**Jetzt (15 Worker, Continuous-Spawning):** ~3 Min × ceil(30/15) = ~6-9 Min Gesamtdauer

### Warum der zweite Durchlauf?

LLMs machen systematische Fehler die sie im gleichen Kontext nicht sehen. Der zweite
Durchlauf mit frisch geladenem Prompt zwingt zur erneuten Pruefung gegen die
sprach-spezifischen Regeln. Erfahrungswerte: der zweite Durchlauf findet in ~30%
der Sprachen mindestens eine Verbesserung.

### Warum Commit nach jeder Sprache?

Wenn bei Sprache 15 etwas schiefgeht, sind die ersten 14 Sprachen sicher committed.
Ohne Zwischen-Commits waere alles verloren. Jeder Commit ist ein Rettungspunkt.

### Warum der volle Prompt fuer jede Sprache?

Jede Sprache hat einzigartige Fallstricke. Die franzoesischen Punctuation-Regeln
helfen nicht bei Koreanisch, die ukrainische Russismus-Warnung ist irrelevant fuer
Spanisch. Der volle sprach-spezifische Prompt stellt sicher, dass genau die richtigen
Warnungen aktiv sind.

### Warum die Scripts in scripts/ statt inline?

Drei Gruende: (1) Anthropic Best Practice — Progressive Disclosure haelt SKILL.md
schlank (<500 Zeilen). (2) Scripts koennen einzeln getestet, versioniert und
weiterentwickelt werden ohne die SKILL.md zu touchen. (3) Beim Trigger des Skills
laedt Claude nur die Workflow-Doku in den Kontext, nicht die ~600 Zeilen Python —
das spart spuerbar Token und Kontext-Pressure.

### Warum eine Datei pro Sprache statt eine grosse?

Wenn nur Franzoesisch uebersetzt wird, soll auch nur die Franzoesisch-Datei in den
Kontext geladen werden — nicht alle 30 Sprach-Bloecke. Bei Komplettuebersetzungen
laedt der Skill die Dateien sequentiell nacheinander, jeweils nur die gerade
gebrauchte. Empirisch spart das ~95% Kontext-Verbrauch pro Uebersetzungs-Lauf.

### Prompt-Caching-Wirkung (Performance-Hinweis)

SKILL.md und übersetzung-global.md sind statisch und werden vom Anthropic Prompt
Caching System gecacht. Pro Sprach-Wechsel wird im Idealfall NUR die kleine
sprach-spezifische Datei (~20 Zeilen) frisch geladen — die ca. 660 Zeilen
SKILL.md + übersetzung-global.md kommen aus dem Cache. Das reduziert die
Token-Last fuer eine Komplett-Uebersetzung ueber 30 Sprachen drastisch.

Damit das funktioniert: SKILL.md sollte ZWISCHEN den Sprachen nicht erneut
gelesen werden. Aus dem Gedaechtnis arbeiten. Nur die jeweils naechste Sprach-Datei
in `references/languages/[code].md` wird frisch eingelesen.
