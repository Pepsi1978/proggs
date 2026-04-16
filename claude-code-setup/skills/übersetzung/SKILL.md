---
name: übersetzung
description: Uebersetzt Android strings.xml in alle 26 Sprachen aus uebersetzung-global.md. Nutze diesen Skill IMMER wenn der Benutzer sagt "uebersetze die Strings", "Uebersetzung starten", "Strings uebersetzen", "starte den Uebersetzungsskill", "uebersetze fuer [App]", "neue Strings uebersetzen", "alle Strings uebersetzen", oder wenn eine App lokalisiert werden soll. Auch bei Varianten wie "mach die App mehrsprachig", "Lokalisierung", "i18n", "internationalisieren", "in andere Sprachen", "uebersetze das". Der Skill arbeitet Sprache fuer Sprache sequentiell, mit Verifikation nach jeder Sprache und Commit nach jedem Abschluss. Funktioniert fuer JEDE Android-App, nicht nur fuer eine bestimmte.
---

# Uebersetzungs-Skill: Android strings.xml in 26 Sprachen

Dieser Skill uebersetzt die strings.xml einer Android-App in alle 26 Sprachen, die in
`~/proggs/uebersetzung-global.md` definiert sind. Er arbeitet Sprache fuer Sprache,
verifiziert jede Uebersetzung im zweiten Durchlauf, und committet nach jeder fertigen
Sprache. Das Ziel ist professionelle App-Store-Qualitaet — nicht "gut genug", sondern
die bestmoegliche maschinelle Uebersetzung.

---

## Wann dieser Skill zum Einsatz kommt

- **Neue App lokalisieren**: "Uebersetze die Strings fuer [App-Name]"
- **Neue Strings nach Feature**: "Uebersetze nur die neuen Strings" (nach dem Hinzufuegen neuer Features)
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

### 1.4 Prompt-Referenz laden

Die Datei `~/proggs/uebersetzung-global.md` lesen. Sie enthaelt:
- **Abschnitt 1**: Den Universal-Prompt mit Platzhaltern
- **Abschnitt 2**: 26 sprach-spezifische Prompt-Bloecke

### 1.5 App-Informationen sammeln

Fuer die Platzhalter im Universal-Prompt werden diese Informationen benoetigt:

| Platzhalter | Woher | Beispiel |
|-------------|-------|---------|
| `[APP_NAME]` | Benutzer-Eingabe oder `app_name` in strings.xml | "BestJournal" |
| `[APP_DESCRIPTION]` | README.md der App oder Benutzer | "Personal journaling and diary app" |
| `[APP_TONE]` | Aus dem Stil der deutschen Strings ableiten | "Warm, encouraging, personal" |
| `[TARGET_LANGUAGE]` | Wird pro Sprache gesetzt | "French" |
| `[LOCALE_CODE]` | Aus uebersetzung-global.md | "fr-FR" |
| `[REGISTER]` | Aus dem sprach-spezifischen Prompt | "Informal tu" |

Wenn `[APP_DESCRIPTION]` oder `[APP_TONE]` nicht klar sind: kurz den Benutzer fragen
oder aus der README.md der App ableiten. Das dauert 10 Sekunden und verbessert jede
Uebersetzung — nicht ueberspringen.

### 1.6 Zielverzeichnisse pruefen

Fuer jede Sprache pruefen ob das Zielverzeichnis existiert:
```
[APP_DIR]/app/src/main/res/values-[locale]/strings.xml
```

Locale-Mapping (Android-Verzeichnisnamen):
```
en → values-en, fr → values-fr, es → values-es, pt-BR → values-pt-rBR,
it → values-it, nl → values-nl, pl → values-pl, ru → values-ru,
uk → values-uk, tr → values-tr, ja → values-ja, ko → values-ko,
zh-Hans → values-zh-rCN, zh-Hant → values-zh-rTW, ar → values-ar,
hi → values-hi, th → values-th, id → values-in, bn → values-bn,
te → values-te, mr → values-mr, ta → values-ta, ur → values-ur,
gu → values-gu, kn → values-kn, ml → values-ml
```

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
- Sprachen: 26 (en, fr, es, pt-BR, it, nl, pl, ru, uk, tr, ja, ko,
  zh-Hans, zh-Hant, ar, hi, th, id, bn, te, mr, ta, ur, gu, kn, ml)
- Vorgehen: Sprache fuer Sprache, mit Verifikation und Commit nach jeder Sprache

Starte jetzt mit Englisch...
```

---

## Phase 2: Uebersetzungs-Schleife — Sprache fuer Sprache

Diese Phase ist das Herzstuck des Skills. Fuer JEDE der 26 Sprachen werden drei
Schritte ausgefuehrt: Uebersetzen, Verifizieren, Speichern.

Die Reihenfolge der Sprachen folgt dem Inhaltsverzeichnis in uebersetzung-global.md:
en → fr → es → pt-BR → it → nl → pl → ru → uk → tr → ja → ko → zh-Hans → zh-Hant
→ ar → hi → th → id → bn → te → mr → ta → ur → gu → kn → ml

### Schritt A — Uebersetzen (erster Durchlauf)

1. **Prompt-Block extrahieren**: In uebersetzung-global.md nach `### [code] —` suchen
   und den Code-Block zwischen den ``` Markierungen extrahieren.

2. **Prompt zusammenbauen**: Universal-Prompt + sprach-spezifischer Block.
   Alle Platzhalter befuellen. Den REGISTER-Wert aus dem sprach-spezifischen Block
   uebernehmen (z.B. "Informal tu" fuer Franzoesisch).

3. **Uebersetzen**: Die Quell-Strings mit dem zusammengebauten Prompt uebersetzen.
   Dabei den gesamten Prompt als Kontext im Kopf behalten — jede Warnung, jeder
   Vokabel-Hinweis, jede Plural-Regel ist wichtig.

4. **Ergebnis schreiben**: Die uebersetzten Strings in die Zieldatei schreiben:
   `values-[locale]/strings.xml`

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

1. **Prompt erneut laden**: Den sprach-spezifischen Prompt-Block NOCHMAL lesen.
   Nicht aus dem Gedaechtnis arbeiten — frisch laden, damit keine Warnung vergessen wird.

2. **Systematische Pruefung (6 Checks):**

   | # | Check | Was geprueft wird | Wie pruefen |
   |---|-------|------------------|-------------|
   | 1 | Vollstaendigkeit | Jeder Quell-String hat eine Uebersetzung | Anzahl Strings vergleichen |
   | 2 | Platzhalter | %s, %d, %1$s exakt wie im Original | Grep nach `%` in Quelle und Ziel |
   | 3 | XML-Struktur | Tags korrekt geoeffnet/geschlossen, Escaping (\', \") | XML validieren |
   | 4 | Plural-Formen | Alle erforderlichen `quantity`-Formen vorhanden | Gegen Prompt-Vorgabe pruefen |
   | 5 | Sprach-Warnungen | Spezifische LLM-Pitfalls aus dem Prompt | Gegen Warnung-Liste pruefen |
   | 6 | Konsistenz | Gleiche Begriffe fuer gleiche Konzepte | Stichprobe der Kern-Vokabeln |

3. **Check 5 im Detail — Sprach-spezifische Warnungen:**
   Das ist der wichtigste Check. Fuer jede Sprache gibt es spezifische Gefahren:

   | Sprache | Was im zweiten Durchlauf BESONDERS pruefen |
   |---------|-------------------------------------------|
   | fr | Leerzeichen vor : ; ! ? vorhanden? Guillemets statt Anfuehrungszeichen? |
   | es | Kein Voseo? Kein Usted? Keine Castilian-Begriffe? |
   | pt-BR | Keine PT-PT-Woerter? (utilizador, aplicacao, guardar, definicoes) |
   | nl | Keine German→Dutch False Friends? (wie≠how, mogen≠moegen, meer≠Meer) |
   | uk | KEINE russischen Woerter? (Сохранить→Зберегти, Настройки→Налаштування) |
   | zh-Hans | Keine Traditional-Zeichen? Keine Taiwan-Vokabeln? |
   | zh-Hant | Keine Simplified-Zeichen? Keine Mainland-Vokabeln? |
   | ja | Konsistente Hoeflichkeitsstufe (です/ます)? Kein Keigo-Mix? |
   | ko | Konsistente Sprechebene (해요체)? Keine 해라체? |
   | ar | Alle 6 Plural-Formen? Gender-Agreement? Keine Dialekt-Woerter? |
   | tr | Vokalharmonie korrekt? Keine Plural-Suffixe nach Zahlen? |
   | ru | Richtiger Aspekt (Сохранить, nicht Сохранять)? Alle 4 Plurale? |
   | hi | Kein unkontrolliertes Hinglish? Register konsistent (आप)? |
   | th | Keine Hoeflichkeitspartikel (ครับ/ค่ะ) in Buttons? |
   | ta | Keine Sanskrit-Lehnwoerter? Natives Tamil fuer persoenliche Begriffe? |
   | mr | Keine Hindi-Woerter? Drei Genera korrekt? |
   | bn | Keine Hindi-Leakage? Keine Devanagari-Zeichen? |
   | ml | Vereinfachte Orthographie? Suffix-Ketten korrekt? |

4. **Korrekturen anwenden**: Wenn Probleme gefunden werden, die betroffenen Strings
   korrigieren. Dabei den vollstaendigen Prompt-Kontext beruecksichtigen.

5. **Verbesserungen melden**: Dem Benutzer kurz berichten was im zweiten Durchlauf
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
   ✓ [Sprache] ist jetzt fertig. ([N]/26)
   Naechste Sprache: [naechste Sprache]
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

Gesamt: [N] Strings in 26 Sprachen uebersetzt.
Commits: #[erste] bis #[letzte]
```

### 3.2 Bekannte Einschraenkungen nennen

Wenn bestimmte Sprachen wahrscheinlich eine menschliche Pruefung brauchen,
das explizit nennen:
- Ukrainisch (Russismus-Risiko)
- Malayalam (komplexe Morphologie, niedrige LLM-Qualitaet)
- Kannada (schwache LLM-Performance)
- Thai (Wort-Segmentierung unsichtbar)

---

## Sonderfaelle

### Nur eine einzelne Sprache uebersetzen

Wenn der Benutzer sagt "uebersetze nur auf Franzoesisch":
- Nur diese eine Sprache durchlaufen (Schritt A + B + C)
- Dann fertig, nicht alle 26 machen

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

---

## Qualitaets-Prinzipien

Diese Prinzipien erklaeren WARUM der Skill so arbeitet wie er arbeitet:

### Warum sequentiell statt parallel?

Uebersetzungsqualitaet braucht vollen Kontext. Wenn 26 Sprachen parallel uebersetzt
werden, bekommt jede nur einen Bruchteil der Aufmerksamkeit. Sequentiell bedeutet:
jede Sprache bekommt den vollstaendigen Prompt-Kontext, die volle Verifikation, und
das Ergebnis wird sofort committed — ein Rettungspunkt nach jeder Sprache.

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
