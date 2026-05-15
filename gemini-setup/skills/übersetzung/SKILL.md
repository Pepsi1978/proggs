---
name: übersetzung
description: Uebersetzt Android strings.xml in alle 26 Sprachen aus der mitgelieferten Referenzdatei übersetzung-global.md (im Skill-Ordner). Nutze diesen Skill IMMER wenn der Benutzer sagt "uebersetze die Strings", "Uebersetzung starten", "Strings uebersetzen", "starte den Uebersetzungsskill", "uebersetze fuer [App]", "neue Strings uebersetzen", "alle Strings uebersetzen", oder wenn eine App lokalisiert werden soll. Auch bei Varianten wie "mach die App mehrsprachig", "Lokalisierung", "i18n", "internationalisieren", "in andere Sprachen", "uebersetze das". Der Skill arbeitet Sprache fuer Sprache sequentiell, mit Verifikation nach jeder Sprache und Commit nach jedem Abschluss. Funktioniert fuer JEDE Android-App, nicht nur fuer eine bestimmte.
---

# Uebersetzungs-Skill: Android strings.xml in 26 Sprachen

Dieser Skill uebersetzt die strings.xml einer Android-App in alle 26 Sprachen, die in
der mitgelieferten Referenzdatei `übersetzung-global.md` definiert sind. Er arbeitet
Sprache fuer Sprache, verifiziert jede Uebersetzung im zweiten Durchlauf, und committet
nach jeder fertigen Sprache. Das Ziel ist professionelle App-Store-Qualitaet — nicht
"gut genug", sondern die bestmoegliche maschinelle Uebersetzung.

## Referenzdatei finden (KRITISCH — immer dieser Pfad)

Die Referenzdatei `übersetzung-global.md` (mit Umlaut!) liegt direkt im Skill-Ordner:

```
/Users/frank/.Gemini/skills/übersetzung/übersetzung-global.md   (macOS)
$env:USERPROFILE/.Gemini/skills/übersetzung/übersetzung-global.md   (Windows)
```

**NIEMALS** nach `~/proggs/uebersetzung-global.md` (ohne Umlaut) suchen — dieser Pfad
existiert nicht. Die korrekte Datei heisst mit Umlaut und ist Teil des Skills. Fallback
bei Nicht-Auffinden: `~/proggs/übersetzung-global.md` — das ist die Master-Kopie.

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

Die Datei `~/.Gemini/skills/übersetzung/übersetzung-global.md` lesen (mit Umlaut!).
Sie wird mit dem Skill mitgeliefert und ist garantiert vorhanden. Sie enthaelt:
- **Abschnitt 1**: Den Universal-Prompt mit Platzhaltern
- **Abschnitt 2**: 26 sprach-spezifische Prompt-Bloecke

Wenn die Datei im Skill-Ordner aus irgendeinem Grund fehlt, ist der Fallback
`~/proggs/übersetzung-global.md` (Master-Kopie im Repo).

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

2. **Systematische Pruefung (9 Checks):**

   | # | Check | Was geprueft wird | Wie pruefen |
   |---|-------|------------------|-------------|
   | 1 | Vollstaendigkeit | Jeder Quell-String hat eine Uebersetzung | Anzahl Strings vergleichen |
   | 2 | Platzhalter | %s, %d, %1$s exakt wie im Original | Grep nach `%` in Quelle und Ziel |
   | 3 | XML-Struktur | Tags korrekt geoeffnet/geschlossen, Escaping (\', \") | XML validieren |
   | 4 | Plural-Formen | Alle erforderlichen `quantity`-Formen vorhanden | Gegen Prompt-Vorgabe pruefen |
   | 5 | Sprach-Warnungen | Spezifische LLM-Pitfalls aus dem Prompt | Gegen Warnung-Liste pruefen |
   | 6 | Konsistenz | Gleiche Begriffe fuer gleiche Konzepte | Stichprobe der Kern-Vokabeln |
   | **7** | **Native-Ziffern (indische Sprachen PFLICHT)** | **Keine bengalischen/devanagari/tamilischen etc. Ziffern** | **Python-Regex pro Sprache (siehe unten)** |
   | **8** | **Full-width Punctuation (CJK-Sprachen PFLICHT)** | **Keine half-width `, . ! ? : ; ( )` nach CJK-Zeichen** | **Python-Regex (siehe unten)** |
   | **9** | **Portugiesisch-Varianten-Trennung (pt-BR ↔ pt-PT PFLICHT)** | **Keine PT-BR-Vokabeln in pt-PT und umgekehrt** | **Python-Regex bidirektional (siehe unten)** |

#### Check 7 — Native-Ziffern-Pflichtcheck fuer indische Sprachen

Die Referenzdatei schreibt fuer ALLE indischen Sprachen explizit vor:
**"Use Arabic numerals (0-9), NOT [native] digits."**

LLMs ignorieren diese Regel haeufig und mischen native Ziffern in die Uebersetzung.
Das ist ein systematischer Fehler — deshalb PFLICHT-Check via Python nach jeder
indischen Uebersetzung:

| Sprache | Native Ziffern (VERBOTEN) | Unicode-Range |
|---------|---------------------------|---------------|
| bn (Bengali) | ০১২৩৪৫৬৭৮৯ | U+09E6–U+09EF |
| hi (Hindi) | ०१२३४५६७८९ | U+0966–U+096F |
| mr (Marathi) | ०१२३४५६७८९ | U+0966–U+096F (Devanagari) |
| te (Telugu) | ౦౧౨౩౪౫౬౭౮౯ | U+0C66–U+0C6F |
| ta (Tamil) | ௦௧௨௩௪௫௬௭௮௯ | U+0BE6–U+0BEF |
| gu (Gujarati) | ૦૧૨૩૪૫૬૭૮૯ | U+0AE6–U+0AEF |
| kn (Kannada) | ೦೧೨೩೪೫೬೭೮೯ | U+0CE6–U+0CEF |
| ml (Malayalam) | ൦൧൨൩൪൫൬൭൮൯ | U+0D66–U+0D6F |
| ur (Urdu) | ۰۱۲۳۴۵۶۷۸۹ | U+06F0–U+06F9 (Extended Arabic-Indic) |

**Pflicht-Script (nach jeder indischen Uebersetzung ausfuehren):**

```python
import re
LANG_DIGITS = {
    "bn": ("০১২৩৪৫৬৭৮৯", "Bengali"),
    "hi": ("०१२३४५६७८९", "Devanagari"),
    "mr": ("०१२३४५६७८९", "Devanagari"),
    "te": ("౦౧౨౩౪౫౬౭౮౯", "Telugu"),
    "ta": ("௦௧௨௩௪௫௬௭௮௯", "Tamil"),
    "gu": ("૦૧૨૩૪૫૬૭૮૯", "Gujarati"),
    "kn": ("೦೧೨೩೪೫೬೭೮೯", "Kannada"),
    "ml": ("൦൧൨൩൪൫൬൭൮൯", "Malayalam"),
    "ur": ("۰۱۲۳۴۵۶۷۸۹", "Extended Arabic-Indic"),
}
locale = "bn"  # anpassen pro Sprache
native, label = LANG_DIGITS[locale]
path = f"[APP_DIR]/app/src/main/res/values-{locale}/strings.xml"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()
count = sum(1 for c in content if c in native)
if count > 0:
    print(f"FEHLER: {count} {label}-Ziffern gefunden — MUESSEN zu 0-9 ersetzt werden")
    fixed = content.translate(str.maketrans(native, "0123456789"))
    import tempfile, os
    with tempfile.NamedTemporaryFile("w", dir=os.path.dirname(path),
                                      suffix=".tmp", delete=False, encoding="utf-8") as tmp:
        tmp.write(fixed); tmp_path = tmp.name
    os.replace(tmp_path, path)
    print(f"Fixed: {count} Ziffern ersetzt")
else:
    print(f"OK: 0 {label}-Ziffern (Arabic-only)")
```

Dieser Check ist **nicht optional** fuer bn, hi, mr, te, ta, gu, kn, ml, ur. Er wird
nach Schritt A (Uebersetzen) und vor Schritt C (Commit) ausgefuehrt. Bei >0 nativen
Ziffern: automatisch ersetzen, in die Verbesserungs-Meldung aufnehmen.

#### Check 8 — Full-width Punctuation Pflichtcheck fuer CJK-Sprachen

Die Referenzdatei schreibt fuer ALLE CJK-Sprachen explizit vor:
**"Use full-width punctuation: 。，！？（）「」"**

LLMs verwenden trotzdem haeufig half-width ASCII `, . ! ? : ; ( )` nach CJK-Zeichen.
Das ist visuell sofort als unprofessionell erkennbar — besonders weil andere CJK-Apps
(Apple, Google, Native-Apps) durchgaengig full-width verwenden.

**Empirische Daten** (BestJournal-App, April 2026):
- zh-Hans: 551 half-width Vorkommen → gefixt in #1530
- zh-Hant: 446 half-width Vorkommen → gefixt in #1531/#1532
- ja: weniger betroffen, aber auch vorhanden

| Sprache | Half-width (VERBOTEN nach CJK) | Full-width (KORREKT) |
|---------|-------------------------------|---------------------|
| zh-Hans | `, . ! ? : ; ( )` | `， 。 ！ ？ ： ； （ ）` |
| zh-Hant | `, . ! ? : ; ( )` | `， 。 ！ ？ ： ； （ ）` |
| ja | `, . ! ? : ; ( )` | `、 。 ！ ？ ： ； （ ）` (Komma = `、` !) |

**WICHTIG — Ausnahmen die NICHT gefixt werden duerfen:**
- JSON-Schema-Strings (typisch in `ai_prompt_*` Keys): half-width `:` und `,` und `"`
  sind dort Pflicht-Syntax, sonst bricht das KI-Parsing
- Versionsnummern (`1.0.5`), Uhrzeiten (`12:30`) — half-width bleibt
- Format-Platzhalter `%1$s`, `%d`

**Pflicht-Script (nach jeder zh-Hans/zh-Hant/ja Uebersetzung ausfuehren):**

```python
import re, os, tempfile

LOCALE = "zh-rTW"  # anpassen: zh-rCN, zh-rTW, ja
PATH = f"[APP_DIR]/app/src/main/res/values-{LOCALE}/strings.xml"

# Komma fuer ja anders als zh-Hans/zh-Hant!
COMMA = "、" if LOCALE == "ja" else "，"

def is_json_schema(s):
    """JSON-Strings duerfen NICHT gefixt werden — sonst bricht KI-Parsing."""
    if 'JSON' in s and ('\\"' in s or '{' in s):
        return True
    if re.search(r'\\"[a-zA-Z_]+\\"\s*:', s):
        return True
    return False

def fix_string(text):
    if is_json_schema(text):
        return text, 0
    fixed = text
    n = 0
    # Komma nach CJK
    fixed, c = re.subn(r'([\u4e00-\u9fff]),', rf'\1{COMMA}', fixed); n += c
    # Punkt nach CJK (nicht in 1.0)
    fixed, c = re.subn(r'([\u4e00-\u9fff])\.(?!\d)', r'\1。', fixed); n += c
    # Ausrufezeichen, Fragezeichen
    fixed, c = re.subn(r'([\u4e00-\u9fff])!', r'\1！', fixed); n += c
    fixed, c = re.subn(r'([\u4e00-\u9fff])\?', r'\1？', fixed); n += c
    # Doppelpunkt (nicht in 12:30)
    fixed, c = re.subn(r'([\u4e00-\u9fff]):(?!\d{2})', r'\1：', fixed); n += c
    fixed, c = re.subn(r'([\u4e00-\u9fff]);', r'\1；', fixed); n += c
    # Klammern: ( neben CJK -> （
    fixed, c = re.subn(r'([\u4e00-\u9fff])\s*\(', r'\1（', fixed); n += c
    # ) Paire schliessen
    fixed, c = re.subn(r'（([^（）()]*?)\)', r'（\1）', fixed); n += c
    # Kosmetisch: kein Space nach full-width Punctuation
    fixed = re.sub(r'：[ \t]+(\S)', r'：\1', fixed)
    fixed = re.sub(r'。[ \t]+(\S)', r'。\1', fixed)
    return fixed, n

with open(PATH, "r", encoding="utf-8") as f:
    content = f.read()
original = content
total_fixes = 0
strings_changed = 0

def process(match):
    global total_fixes, strings_changed
    full = match.group(0)
    body = match.groups()[-1]
    new_body, n = fix_string(body)
    if n > 0:
        total_fixes += n
        strings_changed += 1
        return full.replace(body, new_body)
    return full

content = re.sub(r'<string name="([^"]+)"(?:[^>]*?)>(.*?)</string>',
                 process, content, flags=re.DOTALL)
content = re.sub(r'<item(?:\s+quantity="[^"]+")?>(.*?)</item>',
                 process, content, flags=re.DOTALL)

if content != original:
    d = os.path.dirname(os.path.abspath(PATH))
    with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp",
                                      delete=False, encoding="utf-8") as tmp:
        tmp.write(content); tmp_path = tmp.name
    os.replace(tmp_path, PATH)
    print(f"Fixed: {strings_changed} Strings, {total_fixes} Punctuation-Aenderungen")
else:
    print(f"OK: 0 half-width Punctuation in {LOCALE}")
```

Dieser Check ist **nicht optional** fuer zh-Hans, zh-Hant, ja. Er wird nach Schritt A
(Uebersetzen) und vor Schritt C (Commit) ausgefuehrt. Bei >0 Vorkommen: automatisch
ersetzen, in die Verbesserungs-Meldung aufnehmen.

#### Check 9 — Portugiesisch-Varianten-Trennung (pt-BR ↔ pt-PT PFLICHT)

Portugiesisch ist die Sprache mit der hoechsten LLM-Verwechslungs-Rate aller 26 Zielsprachen.
Praktisch alle Standard-LLMs defaulten auf pt-BR und streuen BR-Vokabular auch dann ein,
wenn explizit pt-PT angefragt wurde (und umgekehrt). Deshalb PFLICHT-Check via Python
nach jeder portugiesischen Uebersetzung — BIDIREKTIONAL.

**Empirische Daten** (BestJournal-App, April 2026):
- pt-PT Uebersetzung: 108 você-Vorkommen, 97 Salvar/Excluir/Compartilhar-Leakage,
  29 Retrospectiva statt Retrospetiva (AO 1990), ~580 systematische Fixes insgesamt
- pt-BR darf KEINE PT-PT-Vokabeln haben und umgekehrt. Ein einziges "utilizador"
  in pt-BR ist genauso falsch wie ein "usuário" in pt-PT.

**Marker-Woerter (die 20 wichtigsten in beide Richtungen):**

| Konzept | pt-PT (KORREKT fuer Portugal) | pt-BR (KORREKT fuer Brasilien) |
|---------|-------------------------------|--------------------------------|
| Benutzer | utilizador | usuário |
| App | aplicação (fem.) | aplicativo (masc.) |
| Speichern | guardar | salvar |
| Einstellungen | definições | configurações |
| Passwort | palavra-passe | senha |
| Herunterladen | transferir | baixar |
| Handy | telemóvel | celular |
| Datei | ficheiro | arquivo |
| Loeschen | eliminar | excluir |
| Bildschirm | ecrã | tela |
| Anmelden | iniciar sessão | fazer login / entrar |
| Teilen | partilhar | compartilhar |
| Abonnement | subscrição | assinatura |
| Kamera | câmara | câmera |
| Aufzeichnung | registo | registro |
| Du (Pronomen) | tu | você |
| Wir (ugs) | nós | a gente |
| Stress | stress | estresse |
| Retrospektive (AO 1990) | retrospetiva | retrospectiva |
| Zugreifen | aceder | acessar |
| Managen | gerir | gerenciar |
| Progressiv | "a + Infinitiv" (a guardar) | "-ando / -endo / -indo" (salvando) |

**Pflicht-Script (nach jeder pt-PT ODER pt-BR Uebersetzung ausfuehren):**

```python
import re, os, tempfile, sys

# Variante definieren: "pt-PT" fuer Portugal, "pt-BR" fuer Brasilien
TARGET_VARIANT = "pt-PT"  # oder "pt-BR"
LOCALE = "pt-rPT" if TARGET_VARIANT == "pt-PT" else "pt-rBR"
PATH = f"[APP_DIR]/app/src/main/res/values-{LOCALE}/strings.xml"

# Marker-Paare: (PT-PT-Wort, PT-BR-Wort)
MARKERS = [
    ("utilizador", "usuário"), ("aplicação", "aplicativo"),
    ("guardar", "salvar"), ("definições", "configurações"),
    ("palavra-passe", "senha"), ("transferir", "baixar"),
    ("telemóvel", "celular"), ("ficheiro", "arquivo"),
    ("eliminar", "excluir"), ("ecrã", "tela"),
    ("iniciar sessão", "fazer login"), ("partilhar", "compartilhar"),
    ("subscrição", "assinatura"), ("câmara", "câmera"),
    ("registo", "registro"), ("tu", "você"),
    ("nós", "a gente"), ("stress", "estresse"),
    ("retrospetiva", "retrospectiva"), ("aceder", "acessar"),
    ("gerir", "gerenciar"),
]

# Gerundium-Indikatoren (BR-only in Progressiv):
# In pt-PT VERBOTEN: "-ando/-endo/-indo" nach "estar/está/estou"
GERUND_PATTERNS = [
    r'\b(est[aoáãou]\w*)\s+\w+(ando|endo|indo)\b',
]

with open(PATH, "r", encoding="utf-8") as f:
    content = f.read()

# Nur user-visible Strings pruefen (keine Kommentare, keine Resource-Namen)
def extract_visible(text):
    parts = []
    for m in re.finditer(r'<string[^>]*>([^<]*(?:<(?!/string>)[^<]*)*)</string>', text, re.DOTALL):
        parts.append(m.group(1))
    for m in re.finditer(r'<item[^>]*>([^<]*(?:<(?!/item>)[^<]*)*)</item>', text, re.DOTALL):
        parts.append(m.group(1))
    return '\n'.join(parts)

visible = extract_visible(content)

# Je nach Zielvariante: den ANDEREN Marker finden
forbidden_idx = 1 if TARGET_VARIANT == "pt-PT" else 0
ok_idx = 0 if TARGET_VARIANT == "pt-PT" else 1
label_forbidden = "pt-BR" if TARGET_VARIANT == "pt-PT" else "pt-PT"

print(f"=== Check 9 — Varianten-Trennung fuer {TARGET_VARIANT} ===")
print(f"Suche nach {label_forbidden}-Woertern (sollten NICHT vorkommen):\n")

total_leakage = 0
for pt, br in MARKERS:
    forbidden = br if TARGET_VARIANT == "pt-PT" else pt
    # "tu" ist zu unspezifisch — nur mit Wortgrenzen und nicht in Anfuehrungszeichen
    if forbidden in ("tu", "nós"):
        # Nur als eigenstaendiges Pronomen
        pat = r'\b' + re.escape(forbidden) + r'\b'
    else:
        pat = r'\b' + re.escape(forbidden) + r'\b'
    matches = re.findall(pat, visible, re.IGNORECASE)
    if matches:
        total_leakage += len(matches)
        print(f"  WARN {forbidden} (sollte: {pt if TARGET_VARIANT == 'pt-PT' else br}): {len(matches)}x")

# Gerundium-Check (nur fuer pt-PT)
if TARGET_VARIANT == "pt-PT":
    gerund_matches = []
    for pat in GERUND_PATTERNS:
        for m in re.finditer(pat, visible):
            gerund_matches.append(m.group(0))
    if gerund_matches:
        total_leakage += len(gerund_matches)
        print(f"  WARN BR-Gerundium (sollte 'a + Infinitiv'): {len(gerund_matches)}x")
        for g in gerund_matches[:5]:
            print(f"    Beispiel: {g}")

if total_leakage == 0:
    print(f"OK: 0 {label_forbidden}-Kontaminationen ✓")
else:
    print(f"\nFEHLER: {total_leakage} Varianten-Verletzungen gefunden")
    print(f"MUESSEN korrigiert werden BEVOR der Commit erfolgt.")
    sys.exit(1)
```

**Wann automatisch ausfuehren:**
- Nach jeder pt-PT Uebersetzung: `TARGET_VARIANT = "pt-PT"` setzen
- Nach jeder pt-BR Uebersetzung: `TARGET_VARIANT = "pt-BR"` setzen
- Bei >0 Leakage: automatisch in die sprach-spezifische Fix-Schleife einspeisen
  (siehe Check 5 sprach-spezifische Warnungen fuer pt-BR und pt-PT)

Dieser Check ist **nicht optional** fuer pt-BR und pt-PT. Er wird nach Schritt A
(Uebersetzen) und vor Schritt C (Commit) ausgefuehrt. Bei >0 Vorkommen: automatisch
ersetzen (siehe Batch-Script-Muster in Check 5), in die Verbesserungs-Meldung aufnehmen.

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
   Die Commit-Nummer ermitteln wie in Gemini.md beschrieben (fortlaufend).

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

