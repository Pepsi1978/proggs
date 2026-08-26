# Escape-Formen fuer deutsche Umlaute und Sonderzeichen

> Diese Referenz dokumentiert alle Escape-Formen unter denen deutsche Umlaute
> in Quellcode "versteckt" sein koennen. Die Standard-Grep-Muster aus Phase 1
> finden diese nicht, weil sie nach echten Umlauten (`Ü`, `ä`) suchen — die
> Escape-Form (`Ü`, `&Uuml;`) ist aber ASCII-Text und rutscht durch.
>
> **Hintergrund:** Vorfall 2026-04-16 — das Wort `"Überblick"` war als
> `"Überblick"` (Unicode-Escape) in DashboardScreen.kt hardcoded. Die
> 9 Standard-Muster haben es nicht gefunden.

---

## 1. Unicode-Escape (PFLICHT-Pruefung in jedem Durchlauf)

Diese Form taucht am haeufigsten auf — entweder durch Auto-Format-Tools,
durch Copy-Paste aus alten Code-Snippets oder durch Encoding-Konflikte.

### Pflicht-Muster — alle 7 deutschen Umlaute

| Code | Zeichen | Code | Zeichen |
|------|---------|------|---------|
| `Ü` | Ü | `ü` | ü |
| `Ö` | Ö | `ö` | ö |
| `Ä` | Ä | `ä` | ä |
| `ß` | ß | (kein KB-eq.) | — |

### Breitnetz fuer alle Unicode-Escapes

```
Pattern: \\u[0-9a-fA-F]{4}
Path:    app/src/main/java/
Glob:    *.kt
```

### Gezielte Pflicht-Suche (deutsche Umlaute)

```
Pattern: \\u00(dc|fc|d6|f6|c4|e4|df)
Path:    app/src/main/java/
Glob:    *.kt
```

### Beispiele aus echten Bugs

- `"Überblick"` als `"Überblick"`
- `"Müll"` als `"Müll"`
- `"grüßen"` als `"grüßen"`
- `"Straße"` als `"Straße"`

### Naming-Hinweis bei Treffern

Wenn ein Treffer entdeckt wird, dem Benutzer den DECODIERTEN Text zeigen:

```
Zeile 666: "Überblick" (= "Überblick")
```

So versteht der Benutzer sofort was hinter dem Escape steckt.

### Ausnahmen (technisch, KEINE Extraktion noetig)

| Ausnahme | Beispiel | Warum |
|----------|----------|-------|
| Regex-Konstanten | `Regex("\\u0020+")` | Technisch, nicht UI |
| Unicode-Konstanten in Parser-Code | `const val BOM = "﻿"` | Spezial-Zeichen |
| Kommentare | `// Ü = U-Umlaut` | Informativ, nicht im Code |
| Test-Dateien | `src/test/`, `src/androidTest/` | Nicht in Produktion |
| Test-Daten | `"User Überblick Test"` in Mock | OK fuer Tests |

---

## 2. HTML-Entities (seltener, aber moeglich)

Diese Form taucht auf wenn HTML-Inhalte (z.B. aus einer Datenbank, einer API
oder aus AnnotatedString-Parsing) verarbeitet werden.

### Pflicht-Muster

```
Pattern: &(Uuml|uuml|Ouml|ouml|Auml|auml|szlig);
Path:    app/src/main/java/, app/src/main/res/values/
Glob:    *.kt, *.xml
```

### Beispiele

| HTML-Entity | Zeichen |
|-------------|---------|
| `&Uuml;` | Ü |
| `&uuml;` | ü |
| `&Ouml;` | Ö |
| `&ouml;` | ö |
| `&Auml;` | Ä |
| `&auml;` | ä |
| `&szlig;` | ß |

### Wo zu erwarten

- AnnotatedString mit HTML-Parsing (`HtmlCompat.fromHtml(...)`)
- Strings die direkt in WebViews angezeigt werden
- Daten aus REST-APIs die HTML-encoded sind
- Strings die als HTML in TextView gesetzt werden

### Behandlung

- **In sichtbaren UI-Strings:** Zu echten Umlauten umwandeln
- **In HTML-Vorlagen die spaeter geparst werden:** Bleiben als Entities
- **Im strings.xml:** Entities verwenden ist OK, aber echte Umlaute lesbarer

---

## 3. XML-Entities (selten)

In strings.xml direkt kommen XML-Entities fast nicht vor — Android-Tools
schreiben echte Umlaute. Aber bei manuell editierten Dateien moeglich.

### Pflicht-Muster

```
Pattern: &#(220|252|214|246|196|228|223);
Path:    app/src/main/res/values/
Glob:    strings.xml
```

### Beispiele

| XML-Entity | Zeichen |
|-------------|---------|
| `&#220;` | Ü |
| `&#252;` | ü |
| `&#214;` | Ö |
| `&#246;` | ö |
| `&#196;` | Ä |
| `&#228;` | ä |
| `&#223;` | ß |

### Behandlung

In `strings.xml` IMMER zu echten Umlauten umwandeln. Android-Tools tun das
automatisch, aber bei manuell editierten Dateien pruefen.

---

## 4. JavaScript-Escape (sehr selten)

Nur wenn JavaScript-Code im Kotlin eingebettet ist (z.B. WebView-Injection).

### Muster

```
Pattern: \\x\{00(dc|fc|d6|f6|c4|e4|df)\}
Path:    app/src/main/java/
Glob:    *.kt
```

### Behandlung

- In JS-Code: Bleiben als Escapes (JS-Konvention)
- In Kotlin-String-Konstanten die JS enthalten: Zu echten Umlauten umwandeln

---

## 5. URL-Encoding (technisch, NICHT extrahieren)

URL-encoded Umlaute in URLs sind technisch und gehoeren NICHT in strings.xml
extrahiert. Beispiele:

- `%C3%9C` = Ü (UTF-8 percent-encoded)
- `%C3%BC` = ü
- `%C3%9F` = ß

Diese bleiben in URL-Strings stehen — sie sind Teil der URL-Struktur.

---

## 6. Pre-Flight-Check vor Phase 1

Bevor Phase 1 (SCAN) startet, einmal das Breitnetz-Pattern auf das ganze Projekt
anwenden:

```bash
grep -rn '\\u[0-9a-fA-F]\{4\}' app/src/main/java/ --include="*.kt" | \
    grep -v '^Binary\|/test/\|/androidTest/' | \
    head -50
```

Wenn Treffer mit deutschen Umlaut-Escapes (00dc, 00fc, etc.) erscheinen,
beim Phase-1-Scan auf "viele versteckte Umlaute" einstellen und besonders
sorgfaeltig vorgehen.

---

## 7. Warum echte Umlaute besser sind als Escapes

| Aspekt | Echter Umlaut `Ü` | Escape `Ü` |
|--------|-------------------|-----------------|
| Lesbarkeit im Code | Sehr gut | Schlecht |
| Grep-Auffindbarkeit | Findet jeder Standard-Such-Befehl | Braucht spezielle Patterns |
| IDE-Highlighting | Korrekt | Manchmal als ASCII falsch interpretiert |
| Code-Review | Schnell verstaendlich | Muss decoded werden |
| Risiko bei Refactoring | Niedrig | Auto-Format kann es brechen |

**Regel:** Im Kotlin/Java-Code IMMER echte Umlaute verwenden. Escapes nur fuer:
- Spezial-Unicode-Zeichen (BOM, ZWJ, etc.)
- Regex-Patterns wo der Umlaut ein Steuerzeichen ist
- Tests die Encoding-Verhalten pruefen
