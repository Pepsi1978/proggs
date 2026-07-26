---
name: almanach-trigger-auswertung
description: "Wertet die Almanach-Trigger-Sonde (~/.claude/state/bug-almanac-triggers.jsonl) nach den 3 Hauptdirektiven aus, klassifiziert jede Guard-Unterbrechung automatisch nach Trivialitaet und fuehrt danach eine gefuehrte Fragerunde (Brainstorming, KEIN Plan-Modus), um gemeinsam zu entscheiden, welche unnoetigen Auslöser abgeschaltet werden und was am Auslöse-System generell besser wird. Nutze diesen Skill IMMER wenn der Benutzer sagt: 'werte die Almanach-Trigger aus', 'Almanach-Auswertung', 'Almanach-Trigger-Statistik', 'analysiere die Almanach-Sonde', 'welche Almanach-Unterbrechungen waren unnoetig', 'welche Almanach-Blocks kann ich ausschliessen', 'wie oft hat der Almanach geblockt', 'Almanach-Watcher auswerten'. Liest, fragt und schlaegt vor — aendert den Guard NIE ohne Freigabe."
---

# Almanach-Trigger-Auswertung

## Wozu dieser Skill

Der `bug-almanac-guard` (PreToolUse-Hook) unterbricht die Arbeit, bis fuer den betroffenen
Bereich der Bug-Almanach (+ Best-Practices) gelesen wurde. Eine **Sonde** im Guard zeichnet
jede Unterbrechung (`block`) UND jede Freigabe (`pass`) als JSON-Zeile auf nach:

```
~/.claude/state/bug-almanac-triggers.jsonl
```

Das Almanach-/Best-Practices-System ist ein **Intelligenzsystem**: Es macht Claude vor jeder
Arbeit klueger, indem es bekanntes Bug-Wissen erzwingt. Aber ein Intelligenzsystem, das bei
**unnoetigen** Dingen bremst (z.B. wenn nur eine Versionsnummer hochgezaehlt wird), erzeugt
Reibung statt Nutzen — es kostet Tokens, unterbricht den Fluss und stumpft die Aufmerksamkeit
fuer die ECHTEN Warnungen ab. Dieser Skill ist das Selbstbeobachtungs-Werkzeug, das genau diese
Reibung sichtbar macht und Schritt fuer Schritt herausnimmt, damit das System schaerfer wird.

Dieser Skill hat **zwei Aufgaben** fuer dasselbe Intelligenzsystem:
1. **Reibung herausnehmen** — unnoetige Auslöser (z.B. reiner Version-Bump) finden und abschalten.
2. **Luecken schliessen** — pruefen, ob WIRKLICH jeder vorhandene Bug-Almanach UND jede zugehoerige
   Best-Practices-Datei vom Start-Hook (`bug-almanac-guard`) ueberhaupt getriggert wird. Ein Almanach,
   den der Hook nie ansteuert, ist **totes Wissen**: abgespeichert, aber wirkungslos — es bringt nichts,
   Bug-Wissen zu pflegen, das nie ausgeloest wird. Reihenfolge im Hook: erst der Almanach, dann seine
   Best Practices.

Beide Aufgaben dienen demselben Ziel: Das System soll bei den RICHTIGEN Dingen ausloesen und bei den
falschen schweigen.

**Wichtig:** Dieser Skill liest, wertet aus, fragt und schlaegt vor. Er aendert den Guard NIE
von selbst — jede Abschaltung ist eine bewusste, in der Fragerunde gemeinsam getroffene
Entscheidung.

## Der Rahmen: die 3 Hauptdirektiven

Die gesamte Auswertung laeuft entlang der drei Direktiven — das ist die Brille, durch die jeder
Auslöser betrachtet wird:

| Direktive | Rolle in dieser Auswertung | Leitfrage |
|-----------|----------------------------|-----------|
| **#2 Selbstbeobachtung** | Die Sonde misst das eigene Auslöse-Verhalten; die Klassifikation erkennt Muster; die Fragerunde zieht Lehren. | "Wann bremst der Guard — und war es berechtigt?" |
| **#1 Superintelligenz** | Jeder unnoetige Auslöser ist Reibung im Harness. Ihn zu entfernen schaerft das System (weniger Rauschen, mehr Signal fuer echte Warnungen). | "Macht uns dieser Auslöser klueger oder kostet er nur?" |
| **#3 Resilient Bugfixing** | Abschalten ist funktionserhaltend: NIE einen echten Bug-Trigger opfern. Root-Cause statt Symptom (warum loest der Guard unnoetig aus?). Poka-Yoke: den Fehlauslöser konzeptionell unmoeglich machen. | "Wie schalten wir ab, ohne je echtes Bug-Wissen zu verlieren?" |

## Datenformat (eine JSON-Zeile pro Ereignis)

| Feld | Bedeutung |
|------|-----------|
| `ts` | Zeitstempel |
| `event` | `block` (Unterbrechung) oder `pass` (Freigabe) |
| `block_type` | `almanach-ungelesen` · `volltext-c` · `best-practices` · `kein-almanach` · `already-read` · `disabled` · `ack` |
| `slug` / `area` | Bereich technisch + Klarname |
| `tool` | Edit / Write / MultiEdit |
| `file` | betroffene Datei |
| `change_excerpt` | kurzer Auszug der Aenderung (Secrets maskiert) — der Schluessel zum Erkennen trivialer Auslöser |
| `high_risk` | Stufe-C-Bereich? |
| `session` | Session-ID |

---

## Ablauf

### Schritt 1 — Aggregieren & klassifizieren (Direktive #2: beobachten)

Das mitgelieferte Script liest die `.jsonl` (inkl. rotierter `.jsonl.1`), klassifiziert jeden
Block automatisch nach Trivialitaet und gibt eine kompakte Auswertung aus. Die Rohdatei wird
NICHT in den Kontext geladen (Lossless-Prinzip — Details bleiben per Pfad erreichbar):

```bash
python3 ~/.claude/skills/almanach-trigger-auswertung/scripts/aggregate.py
```

Das Script liefert: Verhaeltnis block/pass · Blocks nach Bereich+Typ · **Abschalt-Bilanz**
(wie viel % sicher/verdaechtig/grenzwertig/berechtigt) · **Abschalt-Kandidaten** (gruppiert
nach Bereich x Art, mit echten Beispielen) · **berechtigte Blocks** (echte Logik, zur Kontrolle).

Die maschinelle Klassifikation kennt fuenf Klassen und ist **konservativ** (im Zweifel
"berechtigt", damit nie ein echter Bug-Trigger faelschlich als wegwerfbar erscheint):

| Klasse | Bedeutung | Stufe |
|--------|-----------|-------|
| `version-bump` | nur `versionCode`/`versionName` geaendert | **SICHER abschaltbar** |
| `string-only` | nur Text-/String-Ressource | VERDACHT (pruefen) |
| `comment-whitespace` | nur Kommentar/Leerzeile | VERDACHT (pruefen) |
| `import-only` | nur Import-/Package-Kopf sichtbar | GRENZFALL (meist folgt Logik → eher behalten) |
| `logic` | echte Funktions-/Logik-Aenderung | BERECHTIGT (nicht anfassen) |

Gibt es keine Daten, meldet das Script das und der Skill endet mit dem Hinweis, dass die Sonde
noch nichts aufgezeichnet hat.

### Schritt 1b — Coverage-Check: ist jeder Almanach + jede Best-Practices im Hook registriert? (Direktive #1: Vollstaendigkeit)

Die zweite Aufgabe. Pruefen, ob der Start-Hook (`bug-almanac-guard`) wirklich JEDEN vorhandenen
Bug-Almanach UND die zugehoerigen Best-Practices ansteuert. Dafuer das autoritative Coverage-Script
laufen lassen (rein lesend, vergleicht `bugs/**` und `best-practices/**` gegen die Datei-Mappings im Guard):

```bash
python3 ~/proggs/bugs/check-guard-coverage.py
```

Es liefert pro Almanach `[OK]` (vom Guard erzwungen), `[BEWUSST]` (Querschnitt, per Design nicht
datei-getriggert) oder `[LUECKE]` (existiert, wird aber NIE ausgeloest) — und dieselbe Einstufung
fuer Best-Practices (`[BP-LUECKE]` = Almanach-Luecke zieht die BP mit; `[BP-ZUSATZ]` = Referenz ohne
Almanach-Paar, kein Fehler). Am Ende stehen die Gesamtzahlen + ERGEBNIS.

**So liest man das Ergebnis:**
- `[LUECKE]`: Der Almanach ist totes Wissen — er wird gepflegt, aber nie getriggert. Das ist der Kernfall,
  den dieser Check fangen soll.
- `[BP-LUECKE]`: Folgt fast immer aus einer Almanach-Luecke — sobald der Almanach erzwungen wird, greift
  auch die BP automatisch (erst Almanach, dann BP).
- `[BP-ZUSATZ]`: Eine BP ohne gleichnamigen Almanach (z.B. uebergreifende Referenz). KEIN Fehler — der
  Guard triggert BP nur als zweite Seite eines Almanachs. Nur erwaehnen, nicht als Luecke behandeln.

### Schritt 2 — Erkennen & verfeinern (Direktive #2: Muster erkennen)

Die Script-Klassifikation ist die harte Grundlage. Jetzt die Kandidaten-Beispiele kurz mit
Augenmass pruefen — vor allem die GRENZFAELLE (`import-only`) und VERDACHT-Faelle:

- Zeigt der Auszug wirklich nur Triviales (Versionszeile, Kommentar, reiner Import-Kopf)?
  → bleibt Abschalt-Kandidat.
- Schimmert echte Logik durch (neue Funktion, Bedingung, API-Aufruf, Annotation, Migration)?
  → in `logic` zuruckstufen (berechtigt). Lieber eine Unterbrechung zu viel als ein verpasster Bug.

Zusaetzlich auf **Bereichs-Fehlzuordnungen** achten: Wenn eine Datei im falschen Almanach-Bereich
landet (z.B. eine reine C#-Overlay-Datei unter `groq`, eine App-Klasse unter `workmanager`), ist
das KEINE Trivialitaet, aber ein eigener Verbesserungspunkt fuers Auslöse-System (Direktive #1) —
fuer die Fragerunde vormerken.

### Schritt 3 — Bericht (festes Format, nach den 3 Direktiven gegliedert)

Den Bericht in Alltagssprache schreiben (der Benutzer ist kein Programmierer und will GENAU
verstehen, was passiert ist, wo es sinnvoll war und was abgeschaltet werden kann). Festes Format:

```
# Almanach-Trigger-Auswertung

## 1. Was die Sonde gemessen hat  (Direktive #2 — Selbstbeobachtung)
Klartext: Der Guard hat <N>-mal gebremst (block) und <M>-mal sofort durchgewunken (pass,
weil der Almanach schon gelesen war). Eine Bremsung heisst: erst Almanach lesen, dann aendern.

Automatische Einstufung der <N> Bremsungen:
- <safe> SICHER unnoetig (reiner Version-Bump) — <pct>
- <suspect> wahrscheinlich unnoetig (nur Text/Kommentar) — <pct>
- <borderline> Grenzfall (nur Datei-Kopf sichtbar) — <pct>
- <legit> berechtigt (echte Logik) — <pct>

## 2. Was wir GETROST ABSCHALTEN koennen  (Direktive #1 — weniger Reibung, mehr Intelligenz)
Pro Abschalt-Kandidat ein eigener Block:

### <Nr>. <Bereich> — <Art des Auslösers>   [SICHER / VERDACHT]
- Was war der Auslöser: <Klartext, welche Datei(en), welche Art Aenderung>
- Wie oft: <n> von <N> Bremsungen (<pct>)
- Echte Beispiele: "<excerpt>", "<excerpt>"
- Warum getrost weglassbar: <Begruendung in Alltagssprache — z.B. "Eine Versionsnummer
  hochzuzaehlen aendert keine Logik; es gibt nichts, wovor ein Bug-Almanach warnen koennte."
  + Verweis: known-bugs-before-coding nennt Versions-Bump ausdruecklich als Kleinkram ohne
  Almanach-Pflicht.>
- Wie wir es abschalten: <konkreter Guard-Vorschlag, siehe Abschnitt "Wie ein Auslöser
  konkret abgeschaltet wird">

## 3. Wo das Bremsen SINNVOLL war  (Direktive #3 — Funktionserhalt, nichts opfern)
Kurz pro Bereich: <slug> hatte <n> Bremsungen mit echter Logik (z.B. neue Funktionen,
Migrationen, Annotationen). Hier ist die Pflicht berechtigt — ein Fehler in diesem Code
koennte echten Schaden anrichten, der Hook soll weiter bremsen.

## 4. Grenzfaelle (kurz pruefen, nicht blind abschalten)
<slug>/<art>: <n> Blocks, bei denen nur der Datei-Anfang sichtbar war (z.B. nur Imports).
Meist folgt echte Logik → im Zweifel bremsen lassen.

## 5. System-Verbesserungen ueber Abschalten hinaus  (Direktive #1)
Was am Auslöse-System selbst besser werden koennte, z.B.: Bereichs-Fehlzuordnungen
(<Beispiele>), ein zentraler Trivial-Filter, "pass mit Grund" statt stiller Durchwink.

## 6. Registrierungs-Luecken: totes Wissen aufdecken  (Direktive #1 — Vollstaendigkeit)
Aus Schritt 1b (check-guard-coverage.py). Almanache/Best-Practices, die existieren, aber vom
Hook NIE getriggert werden:
- Almanach-Luecken (<n>): <key>, <key> … -> gepflegt, aber nie ausgeloest (totes Wissen).
- BP-Luecken (<n>): <pfad> … (meist Folge der Almanach-Luecke; mit dem Almanach behoben).
Pro Luecke ein konkreter Weg (-> Fragerunde): Trigger-Signal im Guard ergaenzen, ODER bewusst als
Querschnitt eintragen (`INTENTIONALLY_UNMAPPED`), ODER (falls obsolet) den Almanach entfernen.
Keine Luecken -> "Alle Almanache + Best Practices sind im Hook registriert."

## 7. Unterm Strich
- Wenn wir <Vorschlag 1> abschalten: ~<n> Bremsungen (<pct>) weniger — ohne ein Stueck
  Bug-Wissen zu verlieren.
- Registrierungs-Luecken: <n> Almanache werden aktuell nie getriggert — die wichtigsten zuerst schliessen.
- Reihenfolge der Empfehlung: von sicher+haeufig (z.B. Version-Bump) zu Grenzfaellen, dann Luecken.
```

### Schritt 4 — Gefuehrte Fragerunde / Brainstorming (KEIN Plan-Modus)

Das ist der Kern-Mehrwert: Nach dem Bericht NICHT einfach auf eine Antwort warten, sondern den
Benutzer aktiv durch die Entscheidungen FUEHREN — wie ein Brainstorming, in dem du Vorschlaege
mit Alternativen machst und gemeinsam herauskristallisiert wird, was weg kann, was bleibt und
was am System generell besser wird. Ablauf:

1. **Reihenfolge nach Prioritaet — von unwichtig/leicht zu wichtig/heikel.** Beginne mit den
   klarsten, sichersten und haeufigsten unnoetigen Auslösern (z.B. Version-Bump): die bringen
   den groessten Gewinn bei null Risiko. Arbeite dich hoch zu Verdacht, dann Grenzfaellen,
   dann zu generellen System-Verbesserungen.

2. **Eine Frage pro Trigger-Gruppe, mit echten Alternativen.** Nutze dafuer das interaktive
   Frage-Werkzeug (Multiple-Choice mit Optionen + Empfehlung), in kurzen Runden (max ~4 Fragen
   pro Runde). Pro Auslöser typische Optionen:
   - **(A) Hart abschalten** via Trivial-Filter im Guard (durchwinken statt blocken). *Empfohlen,
     wenn SICHER.*
   - **(B) Nur warnen statt blocken** (sanfter Hinweis, kein Stopp) — fuer VERDACHT-Faelle.
   - **(C) So lassen** — wenn der Auslöser doch Nutzen hat.
   Formuliere jede Option in Alltagssprache mit der Folge ("dann passiert kuenftig …").

3. **Berechtigte Auslöser bestaetigen lassen.** Eine Frage, ob die als berechtigt erkannten
   Bereiche wirklich weiter bremsen sollen (meist ja) — damit der Funktionserhalt (Direktive #3)
   bewusst bestaetigt ist.

4. **Generelle System-Verbesserung erfragen** (Direktive #1): z.B. "Bereichs-Zuordnung schaerfen?",
   "zentraler Trivial-Filter fuer ALLE Bereiche?", "Transparenz: durchgewunkene Trivial-Edits als
   `pass`/`trivial-uebersprungen` mitloggen, statt still?". Mit Empfehlung.

5. **Registrierungs-Luecken durchgehen** (Direktive #1, Vollstaendigkeit): Fuer JEDE Almanach-Luecke
   aus Schritt 1b eine Frage mit Optionen — damit kein gepflegtes Bug-Wissen totes Wissen bleibt:
   - **(A) Im Guard registrieren** — ein Datei-/Inhalts-Signal im `bug-almanac-guard` (`.ps1` UND `.sh`)
     ergaenzen, sodass der Almanach (und automatisch seine Best-Practices) kuenftig getriggert wird.
     *Empfohlen, wenn es ein klares Dateimuster gibt.*
   - **(B) Bewusst als Querschnitt** — in `INTENTIONALLY_UNMAPPED` (in `check-guard-coverage.py`)
     aufnehmen, wenn es kein sauberes Dateimuster gibt und das Wissen nur ueber Index/Stichworte
     gefunden wird (wie die `apis/`-Almanache).
   - **(C) Almanach entfernen** — falls er obsolet ist.
   BP-Luecken nicht separat abfragen: sie verschwinden automatisch, sobald ihr Almanach registriert ist
   (erst Almanach, dann BP). Nur eine BP-ZUSATZ-Datei, die eigentlich ein Almanach-Paar haben sollte,
   ggf. erwaehnen.

6. **Abschluss:** Die getroffenen Entscheidungen kurz zusammenfassen (was wird abgeschaltet, welche
   Luecken werden geschlossen, was bleibt). Dann — und ERST auf ausdrueckliches OK — die Aenderungen
   umsetzen (`bug-almanac-guard.ps1` UND `.sh` bei neuen Triggern; `check-guard-coverage.py` bei
   Querschnitt-Eintraegen; Cross-Platform; danach committen+pushen). Ohne OK bleibt es beim Vorschlag.

Wenn der Benutzer abwinkt ("nur die Auswertung, keine Fragerunde"), Schritt 4 ueberspringen und
mit der Empfehlung aus Schritt 3.6 enden.

### Schritt 5 — Auswertung vermerken (PFLICHT am Ende)

Nach Bericht/Fragerunde den Zeitpunkt + die aktuelle Block-Zahl festhalten, damit der
Session-Start-Reminder (im `bug-almanac-index`-Hook) kuenftig „neue seit letzter Auswertung"
statt „seit Beginn" zeigt:

```bash
python3 - <<'PY'
import datetime
from pathlib import Path
state = Path.home() / ".claude" / "state"
total = 0
for fn in ("bug-almanac-triggers.jsonl.1", "bug-almanac-triggers.jsonl"):
    p = state / fn
    if p.exists():
        with open(p, encoding="utf-8") as f:
            total += sum(1 for line in f if '"event":"block"' in line)
state.mkdir(parents=True, exist_ok=True)
# Zeile 1 = Datum (yyyy-mm-dd), Zeile 2 = Block-Zahl zum Auswertungszeitpunkt.
with open(state / "almanach-last-review.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write(datetime.date.today().isoformat() + "\n" + str(total) + "\n")
print(f"Auswertung vermerkt: {datetime.date.today().isoformat()}, {total} Blocks gesamt")
PY
```

---

## Wie ein Auslöser konkret abgeschaltet wird (Referenz fuer die Abschalt-Vorschlaege)

Der Ausschluss passiert im `bug-almanac-guard` — in BEIDEN Dateien (`~/.claude/hooks/bug-almanac-guard.ps1`
UND `.sh`, Cross-Platform). Der Guard erkennt aus Dateiname/Inhalt den Bereich (z.B.
`build.gradle.kts` → slug `gradle`) und blockiert dann, wenn der Almanach ungelesen ist. Der
natuerliche Ausschluss-Punkt liegt **nach der Bereichserkennung und vor der Block-Entscheidung**:

> Wenn der Tool-Input (`new_string` / `content` / `edits[].new_string`) AUSSCHLIESSLICH aus
> Versions-Zeilen besteht (`versionCode`/`versionName`), dann NICHT blockieren — den Edit
> durchlassen und in der Sonde als `pass` mit `block_type: "trivial-uebersprungen"` vermerken
> (Transparenz: durchgewunken, nicht still).

Drei Auspraegungen (in der Fragerunde waehlbar):

- **Eng (empfohlen, Poka-Yoke Stufe 3):** Nur fuer den jeweils bestaetigten Auslöser, z.B. nach
  `slug = 'gradle'` pruefen, ob jede nicht-leere Zeile ein `versionCode`/`versionName` enthaelt
  → dann durchwinken. Sicher, weil ein Version-Bump ein vollstaendiger, kurzer Edit ist.
- **Zentral:** Dieselbe `classify()`-Logik aus `scripts/aggregate.py` in den Guard ziehen und bei
  Klasse `version-bump` generell (bereichsuebergreifend) durchwinken. EINE Quelle der Wahrheit
  fuer „was ist trivial".
- **Sanft:** Statt zu blocken nur einen nicht-blockierenden Hinweis ausgeben (fuer VERDACHT-Klassen,
  wo man sich der Trivialitaet nicht ganz sicher ist).

Funktionserhalt (Direktive #3): NUR die sicheren Klassen hart abschalten. String/Kommentar erst
nach Bestaetigung; Grenzfaelle (Imports) NICHT automatisch — sonst koennte echte Logik
durchrutschen. Nach der Aenderung mit einem echten Version-Bump testen (wird durchgelassen?) und
mit einer echten Logik-Aenderung gegentesten (bremst weiterhin?).

---

## Grenzen (bewusst)

- Der Skill **aendert nie** den Guard von selbst — er liefert die Entscheidungsgrundlage und
  setzt erst nach ausdruecklichem OK in der Fragerunde um.
- Die Klassifikation bewertet `change_excerpt` (max ~300 Zeichen), nicht die komplette Datei —
  das reicht fuer die Trivial-Erkennung (besonders Version-Bump), kann aber im Grenzfall einen
  Block falsch einschaetzen. Darum: im Zweifel „berechtigt", und Grenzfaelle in der Fragerunde
  ausdruecklich nachfragen statt automatisch abschalten.
