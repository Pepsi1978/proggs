# Semikolon-Trenner für mehrere Aufgaben in einem Prompt (KRITISCH)

> **Diese Regel gilt AUTOMATISCH in JEDER Session, bei JEDEM Benutzer-Prompt.**
> Wenn in einem Prompt die exakte Zeichenfolge ` ; ` (Leerzeichen-Semikolon-Leerzeichen)
> vorkommt, signalisiert der Benutzer damit, dass der Prompt MEHRERE eigenständige
> Aufgaben enthält, die NACHEINANDER und VOLLSTÄNDIG abgearbeitet werden müssen.

---

## Ursprung

Der Benutzer nutzt das Voice Terminal Overlay zum Einsprechen von Prompts.
Nach jedem Mikrofon-Insert hängt das Overlay automatisch ` ; ` an den Text an.
Spricht der Benutzer mehrfach hintereinander, entsteht so ein Prompt der Form:

```
Aufgabe eins ; Aufgabe zwei ; Aufgabe drei ;
```

Claude MUSS diese Trennung erkennen und JEDE der Teilaufgaben erledigen, ohne eine zu vergessen.

---

## DER MULTI-TASK-VORGANG (durchgängige Pipeline — KEINE Fragmentierung)

> **Das ist das Herzstück dieser Regel.** Aufgabenerkennung ist nur der erste Schritt
> einer durchgehenden Kette. Alle Folgeschritte hängen FEST an der Erkennung dran —
> sie liegen NICHT in irgendwelchen Memories und werden NICHT "teilweise" genutzt.
> Sobald ein ` ; `-Prompt erkannt wird, läuft der gesamte Vorgang als EIN
> zusammenhängender Prozess ab. Die Brücke zum jeweils nächsten Schritt ist immer gelegt.

### Die 7 Schritte der Pipeline (in dieser festen Reihenfolge)

```
1. ERKENNEN        →  am ` ; ` splitten, leere Teile verwerfen, Marker (Pre/Post) aussortieren
        ↓
2. SORTIEREN       →  Pre-Flight: Aufgaben gruppieren, Abhängigkeiten erkennen,
   (Pre-Flight)        optimale Reihenfolge bestimmen (NICHT Einsprech-Reihenfolge)
        ↓
3. ANZEIGEN        →  ab 2 Aufgaben: Übersicht + TaskCreate-Liste sichtbar machen
        ↓
4. ABARBEITEN      →  jede Aufgabe SICHTBAR, eine nach der anderen, KEINE Subagents.
   (pro Aufgabe)       Pro Aufgabe: in_progress → umsetzen → committen+pushen → Marker → abhaken
        ↓
5. BAUEN           →  NUR EINMAL nach der letzten Aufgabe: Build erzeugen
   (einmal am Ende)
        ↓
6. INSTALLIEREN    →  NUR EINMAL nach dem Build: aufs Handy installieren + App starten
   (einmal am Ende)
        ↓
7. VERIFIZIEREN    →  Original-Liste durchgehen: sind WIRKLICH alle erledigt?
   (End-Check)         Untergegangenes nachholen, dann erst Status-Meldung
```

**Die Kette darf nie abreißen.** Wenn Schritt 1 ausgelöst wurde, werden die Schritte
garantiert durchlaufen — kein Schritt wird vergessen.

**Wann Schritt 5+6 (Bauen + Installieren) greifen:** NUR wenn die Aufgaben eine baubare,
installierbare App betreffen (z.B. Android). Bei reinen Regel-, Doku-, Config-, Skript-
oder Web-Aufgaben gibt es nichts zu bauen — dann werden Schritt 5+6 übersprungen und es
geht direkt von Schritt 4 zu Schritt 7. Das ist die einzige erlaubte Auslassung. Alle
anderen Schritte (1–4 und 7) laufen IMMER.

---

## Schritt 1 — ERKENNEN

> **Faustregel:** Anzahl Aufgaben = Anzahl der NICHT-LEEREN Teile nach dem Split. Ein
> abschließendes ` ; ` (ohne Text danach) erzeugt nur einen leeren Teil und zählt NICHT mit.

| Muster | Anzahl Aufgaben |
|--------|-----------------|
| Kein ` ; ` im Prompt | 1 Aufgabe — normal bearbeiten |
| Ein ` ; ` **zwischen** zwei Texten | 2 Aufgaben — beide nacheinander abarbeiten |
| Ein ` ; ` nur **am Ende** (kein Text danach) | 1 Aufgabe — der leere Teil zählt nicht |
| N Trenner **zwischen** Texten | N+1 Aufgaben |
| N Trenner zwischen Texten **plus** ` ; ` am Ende | N+1 Aufgaben (das End-` ; ` zählt nicht extra) |

Beispiel: `Mach X ;` = **1 Aufgabe** (ein End-Trenner). `Mach X ; Mach Y ;` = **2 Aufgaben**
(ein Trenner dazwischen, einer am Ende der nicht zählt).

**Wichtig:** Die Zeichenfolge muss EXAKT `Leerzeichen + Semikolon + Leerzeichen` sein.
Semikola ohne umgebende Leerzeichen (z.B. in Code-Snippets, TypeScript-Statements,
SQL-Queries) sind KEINE Aufgaben-Trenner — dort bleibt das Semikolon Teil des Codes.

Schritte beim Erkennen:
1. **Prompt am ` ; `-Muster splitten** — jede resultierende Teilzeichenkette ist eine eigenständige Aufgabe.
2. **Leere Teile verwerfen** (z.B. wenn der Prompt mit ` ; ` endet).
3. **Marker aussortieren** — Blöcke mit `Pre-Prompt:` / `Post-Prompt:` sind KEINE Aufgaben (siehe eigene Sektion unten).

---

## Schritt 2 — SORTIEREN (Pre-Flight, vor der ersten Aufgabe)

> Die Aufgaben werden NICHT stur in Einsprech-Reihenfolge abgearbeitet. Vorher wird
> kurz sortiert — damit sich die Aufgaben nicht gegenseitig behindern oder überschreiben.

Vor der ersten Code-Änderung diese drei Fragen durchgehen (dauert ~10 Sekunden, kein langer Plan):

| Frage | Was zu tun ist |
|-------|---------------|
| **Gruppieren** | Gehören mehrere Aufgaben zur selben Datei oder zum selben Feature? → zusammen abarbeiten, damit sie sich nicht überschreiben |
| **Abhängigkeiten** | Baut Aufgabe B auf Aufgabe A auf? → A zuerst. Widerspricht Aufgabe C einer früheren? → dem Benutzer melden, nicht blind beides bauen |
| **Reihenfolge** | Optimale Reihenfolge festlegen: erst die Grundlagen, dann was darauf aufbaut, riskante/große Änderungen bewusst einordnen |

**Wenn die optimale Reihenfolge von der Einsprech-Reihenfolge abweicht:** dem Benutzer in
EINEM Satz mitteilen, warum umsortiert wird (z.B. "Ich mache Aufgabe 3 zuerst, weil
Aufgabe 1 darauf aufbaut").

Bei nur 2 unabhängigen, kleinen Aufgaben kann die Sortierung trivial sein — dann reicht
die Standard-Reihenfolge. Die Prüfung selbst (die drei Fragen) wird trotzdem immer gemacht.

### Konflikt-Warnung — bei gegensätzlichen Aufgaben VORHER nachfragen (PFLICHT)

> Wenn zwei Aufgaben dieselbe Stelle gegensätzlich verändern würden, wird NICHT blind
> beides nacheinander gebaut. Sonst überschreibt die spätere Aufgabe die frühere, und
> der Benutzer bekommt am Ende nur ein halbes, widersprüchliches Ergebnis.

Während der Pre-Flight-Prüfung wird aktiv nach solchen Kollisionen gesucht. Ein Konflikt
liegt vor, wenn zwei (oder mehr) Aufgaben **dieselbe Datei, Funktion, dasselbe UI-Element
oder dieselbe Einstellung gegensätzlich** anfassen. Typische Fälle:

| Konflikt-Typ | Beispiel |
|--------------|----------|
| Gegensätzlicher Wert | Aufgabe 2: "Header blau machen" — Aufgabe 9: "Header grün machen" |
| Hinzufügen vs. Entfernen | Aufgabe 3: "Button einbauen" — Aufgabe 11: "diesen Button entfernen" |
| Doppelte, abweichende Änderung am selben Code | Zwei Aufgaben schreiben dieselbe Funktion unterschiedlich um |

**Pflicht-Ablauf bei erkanntem Konflikt:**

1. **STOP** — nicht mit der Abarbeitung beginnen.
2. Dem Benutzer den Konflikt in 1–2 Sätzen zeigen: welche zwei Aufgaben, welche Stelle, was sich widerspricht.
3. **Kurz nachfragen**, welche Variante gewünscht ist (oder ob beide in einer bestimmten Reihenfolge gemeint sind).
4. Erst nach der Antwort weiterarbeiten.

Bei diktierten Multi-Task-Prompts passiert das leicht — der Benutzer spricht spontan ein
und merkt selbst nicht immer, dass sich zwei Wünsche widersprechen. Genau dafür ist diese
Warnung da. Lieber 10 Sekunden nachfragen als am Ende das Falsche gebaut zu haben.

---

## Schritt 3 — ANZEIGEN (Übersicht + TaskCreate-Liste)

### Übersicht im Text (ab 2 Aufgaben)

Bei einem Multi-Task-Prompt MUSS Claude zu Beginn der Antwort dem Benutzer kurz zeigen,
dass mehrere Aufgaben erkannt wurden — in der Reihenfolge, in der sie abgearbeitet werden:

```
Ich habe N Aufgaben erkannt:
1. [Kurzbeschreibung Aufgabe 1]
2. [Kurzbeschreibung Aufgabe 2]
3. [Kurzbeschreibung Aufgabe 3]

Ich arbeite sie der Reihe nach ab.
```

Damit weiß der Benutzer sofort, dass keine Teilaufgabe verloren geht.

### TaskCreate-Liste (PFLICHT ab 2 Aufgaben)

| Anzahl Aufgaben | TaskCreate-Liste? |
|-----------------|-------------------|
| 1 Aufgabe | NEIN — direkt arbeiten, keine Liste nötig |
| 2 oder mehr Aufgaben | **JA — PFLICHT.** Sichtbare Liste anlegen, damit der Fortschritt nachvollziehbar bleibt und keine Aufgabe übersehen wird |

Die Liste bildet die sortierte Reihenfolge aus Schritt 2 ab (nicht zwingend die Einsprech-Reihenfolge).

---

## Schritt 4 — ABARBEITEN (sichtbar, sequenziell, KEINE Subagents)

> **Der Benutzer will jede Aufgabe in Echtzeit sehen.** Es wird IMMER sichtbar im
> Hauptchat gearbeitet, eine Aufgabe nach der anderen. KEINE Subagents für die
> Abarbeitung der Teilaufgaben — der Benutzer würde dann nicht sehen, woran gearbeitet wird.

### Der geschlossene Zyklus pro Aufgabe

Jede einzelne Aufgabe durchläuft denselben kleinen Kreislauf, bevor die nächste startet:

```
1. Task auf in_progress setzen (sichtbar)
2. Kurz ansagen, was jetzt passiert
3. Aufgabe umsetzen (Code-Änderung, sichtbar)
4. committen + pushen (Code ist sicher im Repo, bevor die nächste Aufgabe startet)
5. SICHTBAREN Commit-Marker ausgeben (siehe unten) — damit der Benutzer den Rettungspunkt sieht
6. Task erst JETZT als completed abhaken (Echtzeit — erst wenn committed+gepusht, nicht am Ende stapeln)
7. → nächste Aufgabe
```

**Echtzeit-Abhaken:** Abgehakt wird SOFORT pro Aufgabe — aber erst NACH
committen+pushen (eine Aufgabe gilt erst als erledigt, wenn sie sicher im Repo liegt),
niemals erst am Ende gesammelt. So sieht der Benutzer live, wo der Vorgang steht, und
nichts geht in der Mitte einer langen Liste unter. (Bei reinen Erklärungs-/Frage-Aufgaben
ohne Code-Änderung gibt es kein Commit und keinen Marker — dann wird direkt nach der
Antwort abgehakt.)

**Commit+Push pro Aufgabe:** Jede abgeschlossene Aufgabe wird committed und gepusht, bevor
die nächste beginnt. Das sind die Rettungspunkte. (Der BUILD kommt erst später — siehe Schritt 5.)

**NUR die eigenen Dateien stagen (KRITISCH bei parallelen Sessions):** Beim Committen werden
AUSSCHLIESSLICH die Dateien gestaged, die DIESE Aufgabe selbst geändert oder erstellt hat —
namentlich, einzeln. Andere Sessions (weitere Claude-Fenster, Codex, Gemini) arbeiten oft
gleichzeitig am selben Repo und hinterlassen dort viele halbfertige Dateien. Diese fremden
Dateien werden IGNORIERT — niemals mitcommittet, niemals weggeräumt.

| Regel | Detail |
|-------|--------|
| Stagen | NUR `git add <pfad1> <pfad2>` mit den eigenen Dateinamen — NIEMALS `git add -A` oder `git add .` |
| Fremde Dateien | Liegen lassen wie sie sind. Sie gehören einer anderen Session und werden von ihr selbst committed |
| Pre-Push-Check | Vor dem Push einmal `git status --short` — jede Zeile bewusst zuordnen: eigene Datei (committen) vs. fremd (ignorieren) |
| Meldung | Wenn fremde Dateien im Working Tree liegen: dem Benutzer in 1 Zeile sagen "X Dateien liegen unstaged, gehören nicht zu dieser Aufgabe" |

Details siehe `~/.claude/rules/parallel-sessions-git.md`. Kurzfassung: nur was man selbst
gerade gebaut hat, geht in den Commit — alles andere bleibt unberührt.

### Sichtbarer Commit-Marker pro Aufgabe (PFLICHT)

Direkt nach dem Commit+Push JEDER Aufgabe wird ein klar abgegrenzter Marker ausgegeben,
damit der Benutzer auf einen Blick sieht: "An diesem Punkt wurde committed und gepusht."

**Genaues Format** — Linie darüber, Marker-Zeile, Linie darunter:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 Aufgabe N: [ganz kurze Beschreibung in leichtem Deutsch, max. 1 Zeile] — committed und gepusht
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Bausteine (alle Pflicht):**

| Baustein | Regel |
|----------|-------|
| Linie darüber | Genau 80 × `━` (U+2501), alleine in der Zeile |
| Disketten-Symbol | 💾 ganz vorne in der Marker-Zeile |
| `Aufgabe N:` | Die Nummer aus der TaskCreate-Liste (Aufgabe 1, Aufgabe 2, …) |
| Kurzbeschreibung | Ganz kurz, leichtes Deutsch, **maximal eine Zeile** |
| `— committed und gepusht` | Wörtlich am Ende der Marker-Zeile |
| Linie darunter | Genau 80 × `━`, alleine in der Zeile |

**Wann:** Nach jeder Aufgabe, DIE ETWAS COMMITTET, sofort nach ihrem Commit+Push — nicht
gesammelt am Ende. Der Marker erscheint mitten in der Abarbeitung, zwischen den Aufgaben,
als sichtbarer Rettungspunkt.

**Wann NICHT:** Reine Erklärungs-, Frage- oder Recherche-Aufgaben ohne Datei-Änderung haben
keinen Commit — und damit auch keinen Marker. Sie werden im Text beantwortet und abgehakt.
In der finalen ERLEDIGTE-AUFGABEN-Box erscheinen sie trotzdem (mit Status-Symbol, siehe
`task-completion-summary.md`).

**Beispiel im Ablauf:**

```
(Arbeit an Aufgabe 1 sichtbar …)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 Aufgabe 1: Header-Farbe auf Blau geändert — committed und gepusht
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
(Arbeit an Aufgabe 2 sichtbar …)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 Aufgabe 2: Button vergrößert — committed und gepusht
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

So bleibt der Bezug klar: Jede Linie-Marker-Linie-Einheit ist genau ein gespeicherter Punkt.

### Was hier VERBOTEN ist

- ❌ Subagents für die Abarbeitung der Teilaufgaben starten (Benutzer sieht die Arbeit nicht)
- ❌ Mehrere Aufgaben "im Stillen" abarbeiten und erst am Ende zeigen
- ❌ Alle Tasks erst ganz am Schluss gemeinsam abhaken
- ❌ Mehrere unabhängige Aufgaben sammeln und in EINEM Commit zusammenwerfen

---

## Schritt 5 — BAUEN (nur einmal, ganz am Ende)

> Bei mehreren Aufgaben an derselben App wird NICHT nach jeder Aufgabe gebaut. Das wäre
> Zeitverschwendung (12 Aufgaben = 12 Builds). Stattdessen: alle Code-Aufgaben fertig,
> jede einzeln committed+gepusht — und DANN EINMAL bauen.

- Der Build (`./gradlew assembleDebug`, `bundleRelease`, etc.) läuft **nach der letzten Aufgabe**, ein einziges Mal.
- Reihenfolge bleibt eingehalten: alle Commits+Pushes sind durch, BEVOR gebaut wird (entspricht der "Commit+Push vor Build"-Regel).
- Schlägt der Build fehl: Fehler fixen, erneut committen+pushen, dann erneut bauen. Der Code aller Aufgaben ist bereits sicher im Repo.

**Ausnahme:** Wenn eine spätere Aufgabe in der Liste explizit "und teste/installiere DAS jetzt"
verlangt (Zwischen-Build mitten in der Liste gewünscht), dann diesen einen Zwischenschritt machen.
Standard ist aber: ein Build am Ende.

---

## Schritt 6 — INSTALLIEREN (nur einmal, nach dem Build)

- Nach dem erfolgreichen Build EINMAL aufs Handy installieren (`adb install`) und die App automatisch starten.
- Nicht nach jeder Aufgabe installieren — nur einmal am Ende, mit dem fertigen Build aller Aufgaben.

---

## Schritt 7 — VERIFIZIEREN (End-Check)

> Bevor die finale Status-Meldung kommt: prüfen, ob WIRKLICH jede Aufgabe erledigt wurde —
> besonders die in der Mitte einer langen Liste, die am ehesten untergehen.

Pflicht-Ablauf am Ende:
1. **Original-Liste durchgehen** — jede der N erkannten Aufgaben einzeln gegen das Ergebnis abgleichen.
2. **Untergegangenes finden** — ist eine Aufgabe (z.B. Nummer 7 von 14) übersprungen oder nur halb erledigt?
3. **Nachholen** — falls ja: die offene Aufgabe JETZT noch fertig machen (umsetzen → committen+pushen → abhaken), bevor abgeschlossen wird.
4. **Erst dann** die Status-Meldung und die Abschluss-Boxen ausgeben (Boxen gemäß
   `task-completion-summary.md` — wenn ausschließlich Fragen beantwortet und gar nichts
   umgesetzt/committet wurde, entfallen die Boxen).

Die TaskCreate-Liste hilft hier: stehen alle Tasks auf `completed`? Wenn nicht → der Vorgang ist nicht fertig.

---

## Beispiele

### Beispiel 1: Zwei unabhängige Aufgaben

**Prompt:** `Fixe den Bug in DashboardScreen ; Aktualisiere die Version auf 0.11.0`

**Erkennung:** 2 Aufgaben → TaskCreate-Liste Pflicht.

**Ausgabe-Start:**
```
Ich habe 2 Aufgaben erkannt:
1. Bug in DashboardScreen fixen
2. Version auf 0.11.0 aktualisieren

Ich arbeite sie der Reihe nach ab.
```

### Beispiel 2: Drei aufeinander aufbauende Aufgaben

**Prompt:** `Baue ein neues Einstellungs-Menü ; Teste es auf dem Handy ; Committe und pushe`

**Erkennung:** 3 Aufgaben, sequentiell. Pre-Flight erkennt: "Teste auf dem Handy" gehört
in Schritt 5/6 (Build+Install am Ende), nicht als eigener Zwischen-Build.

### Beispiel 3: Prompt endet auf ` ; `

**Prompt:** `Uebersetze die neuen Strings ; Baue die APK ; `

**Erkennung:** 2 Aufgaben (letzter leerer Teil verworfen).

### Beispiel 4: Semikolon im Code — KEIN Trenner

**Prompt:** `Aendere den Code zu const x = 5; und teste ihn`

**Erkennung:** 1 Aufgabe (kein ` ; ` mit beidseitigen Leerzeichen) → keine TaskCreate-Liste, direkt arbeiten.

### Beispiel 5: Viele Aufgaben an einer App (12+)

**Prompt:** `Mach den Button größer ; ändere die Farbe des Headers ; ... ` (12 Aufgaben am Entropie Reductor)

**Ablauf:** Erkennen (12) → Sortieren (zusammengehörige UI-Änderungen gruppieren, Abhängigkeiten
prüfen) → TaskCreate-Liste mit 12 Einträgen → jede Aufgabe sichtbar nacheinander umsetzen +
committen+pushen + Marker + abhaken → nach der 12. Aufgabe EINMAL bauen → EINMAL aufs Handy
installieren → verifizieren dass alle 12 wirklich erledigt sind → Status-Meldung.

---

## Pre-Prompts und Post-Prompts (Marker-basiert)

Innerhalb einer Multi-Task-Aufzaehlung kann der Benutzer einzelne Bloecke als
**Pre-Prompt** oder **Post-Prompt** markieren. Diese sind KEINE eigenen Aufgaben,
sondern Anweisungen die auf alle Aufgaben angewendet werden.

### Marker-Format

| Marker | Bedeutung | Wann anwenden |
|--------|-----------|---------------|
| `Pre-Prompt: "<text>"` | Kontext/Setup BEVOR die Aufgaben starten | Als Kontext **vor** der Bearbeitung der ersten Aufgabe |
| `Post-Prompt: "<text>"` | Constraint/Hinweis WAEHREND der Aufgaben | Als Constraint **waehrend** und **nach** jeder Aufgabe (Antwortgestaltung, Code, Status) |

### Tolerante Erkennung (Voice-Diktat)

Whisper produziert je nach Aussprache verschiedene Schreibweisen — alle werden gleich erkannt:

- `Pre-Prompt:`, `PrePrompt:`, `Pre Prompt:`, `pre-prompt:`, `PRE-PROMPT:`
- `Post-Prompt:`, `PostPrompt:`, `Post Prompt:`, `post-prompt:`, `POST-PROMPT:`

Anfuehrungszeichen: `"..."` (gerade), `„...“` (deutsch) oder `“...”` (typografisch) — alle gueltig.

### Position im Prompt

Empfohlene Konvention (nicht erzwungen): **Pre-Prompts am Anfang, Aufgaben in der Mitte,
Post-Prompts am Ende**. Beispiel:

```
Pre-Prompt: "Branch ist feature/foo" ; Aufgabe 1 ; Aufgabe 2 ; Post-Prompt: "kurz halten" ; Post-Prompt: "kein Cross-Platform"
```

Der Marker zaehlt mehr als die Position — auch wenn ein `Post-Prompt:` mitten zwischen
Aufgaben auftaucht, wird er als Post-Prompt erkannt, nicht als Aufgabe.

### Mehrere Pre/Post-Prompts in einem Prompt

Voll erlaubt. Mehrere Pre-Prompts werden vor den Aufgaben kombiniert beruecksichtigt,
mehrere Post-Prompts gelten parallel als Constraints.

### Sichtbar machen beim Multi-Task-Prompt — PFLICHT-Tabelle

Wenn der Prompt MINDESTENS einen Pre-Prompt ODER Post-Prompt enthaelt, MUSS am
ALLERERSTEN Punkt der Antwort eine Uebersichts-Tabelle stehen, die alle
erkannten Bloecke nach Typ sortiert auflistet:

```markdown
| Typ | Inhalt |
|-----|--------|
| Pre-Prompt | <Text 1 — wortwoertlich aus dem Prompt> |
| Pre-Prompt | <Text 2 — wortwoertlich aus dem Prompt> |
| Aufgabe | <Text — wortwoertlich aus dem Prompt> |
| Post-Prompt | <Text 1 — wortwoertlich aus dem Prompt> |
| Post-Prompt | <Text 2 — wortwoertlich aus dem Prompt> |
```

#### WORTWOERTLICH 1:1 — keine Zusammenfassung, kein Umschreiben

Der Inhalt JEDER Zelle MUSS exakt das sein, was der Benutzer im Prompt geschrieben
oder diktiert hat — wortwoertlich, ohne Kuerzung, ohne "sinngemaess uebernommen",
ohne eigene Umformulierung, ohne Zusatzkommentare.

Wenn die Aufgabe lang ist, wird sie eben lang dargestellt — die Tabellen-Zelle
wird groesser, aber der Inhalt bleibt der Original-Wortlaut. Der Benutzer muss
in der Tabelle EXAKT die Worte wiederfinden, die er gesprochen oder getippt hat.

Anfuehrungszeichen um die Originaltexte sind erlaubt und sogar empfohlen, weil
sie die Zitat-Natur unterstreichen. Pre/Post-Prompts haben sie ohnehin im
Markup (z.B. `Post-Prompt: "..."`), Aufgaben werden zur Konsistenz auch in
Anfuehrungszeichen gesetzt.

#### Reihenfolge in der Tabelle

ZUERST alle Pre-Prompts, DANN alle Aufgaben, DANN alle Post-Prompts —
unabhaengig von ihrer tatsaechlichen Position im Original-Prompt. So sieht
der Benutzer auf einen Blick was der Kontext (Pre), was die eigentliche
Aufgabe und was die Constraints (Post) sind.

#### Wann die Tabelle ENTFAELLT

Nur wenn der Prompt KEINEN einzigen Pre-Prompt oder Post-Prompt enthaelt
(also nur reine Aufgabe(n) ohne Marker). Dann waere die Tabelle reine
Redundanz.

#### Was die Tabelle leistet

- **Sofortige Bestaetigung** dass der Prompt korrekt geparst wurde
- **Visualisierung** fuer den Benutzer, was Claude als was erkannt hat
- **Schutz vor Fehlklassifikation** — wenn der Benutzer in der Tabelle sieht
  dass eine Aufgabe als Post-Prompt eingestuft wurde (oder umgekehrt), kann
  er sofort korrigieren bevor Code geaendert wird

Die Tabelle erscheint VOR jeder anderen Antwort-Aktivitaet — also vor
Tool-Calls, Code-Edits, Plaenen oder Erklaerungen.

### Stiller Modus bei nicht passenden Post-Prompts

Manche Post-Prompts sind plattform- oder kontext-spezifisch (z.B. `i18n-Pflicht (Android-Apps)`
greift nur bei Android-Aufgaben). Wenn der aktuelle Kontext nicht zur Bedingung passt,
schlummert der Post-Prompt — er feuert keine ueberfluessigen Hinweise und nennt sich
nicht als "aktiv". Sobald eine passende Aufgabe kommt, greift er wieder.

---

## Was NIEMALS passieren darf

- ❌ Ein Multi-Task-Prompt wird als eine einzelne Aufgabe missverstanden
- ❌ Nur die erste Aufgabe wird erledigt, die restlichen werden "vergessen"
- ❌ Nur die letzte Aufgabe wird erledigt, die vorherigen werden übergangen
- ❌ Eine Aufgabe in der MITTE einer langen Liste wird übersprungen oder nur halb erledigt — der End-Check (Schritt 7) muss das fangen
- ❌ Die Aufgaben-Erkennung wird dem Benutzer nicht mitgeteilt (er sieht nicht ob der Parse korrekt war)
- ❌ Bei 2+ Aufgaben KEINE TaskCreate-Liste anlegen
- ❌ Subagents für die Abarbeitung der Teilaufgaben starten — der Benutzer will jede Aufgabe sichtbar im Hauptchat sehen
- ❌ Aufgaben stur in Einsprech-Reihenfolge abarbeiten ohne die Pre-Flight-Sortierung (Schritt 2)
- ❌ Zwei gegensätzliche Aufgaben (gleiche Stelle, widersprüchliche Änderung) blind nacheinander bauen, statt vorher nachzufragen
- ❌ Tasks erst am Ende gesammelt abhaken statt in Echtzeit pro Aufgabe
- ❌ Den sichtbaren Commit-Marker (💾 + Aufgabe N + Beschreibung + "committed und gepusht", eingerahmt) nach einer Aufgabe weglassen oder gesammelt am Ende ausgeben
- ❌ Den Commit-Marker ohne Disketten-Symbol, ohne Nummer oder ohne die zwei Rahmen-Linien ausgeben
- ❌ `git add -A` / `git add .` verwenden und damit fremde Dateien anderer Sessions mitcommitten
- ❌ Fremde, halbfertige Dateien anderer Sessions wegräumen, überschreiben oder "aufräumen" — nur die eigenen Dateien anfassen
- ❌ Nach JEDER Aufgabe neu bauen und installieren — Build+Install kommt nur EINMAL am Ende
- ❌ Build ausführen bevor alle Aufgaben committed+gepusht sind
- ❌ Status-Meldung nur für eine Teilaufgabe am Ende — es MUSS für ALLE eine Rückmeldung geben
- ❌ Den End-Check (Schritt 7) überspringen und direkt "fertig" melden
- ❌ Semikola in Code/SQL/URLs fälschlich als Aufgaben-Trenner interpretieren
- ❌ Einen Block mit `Pre-Prompt:` oder `Post-Prompt:` als eigenstaendige Aufgabe behandeln
- ❌ Pre/Post-Prompts ignorieren weil sie "nicht direkt zur Aufgabe gehoeren"
- ❌ Bei einer Klaerungsfrage-Post-Prompt blind raten statt zu fragen
- ❌ In der Pflicht-Tabelle den Inhalt einer Zelle sinngemaess zusammenfassen oder umschreiben
- ❌ In der Pflicht-Tabelle Aufgaben nur mit ein paar Stichworten beschreiben statt mit dem Original-Text
- ❌ In der Pflicht-Tabelle eigene Erklaerungen oder Zusatztexte einfuegen die der Benutzer nicht geschrieben hat

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `task-completion-summary.md` | Die Abschluss-Boxen kommen NACH dem End-Check (Schritt 7) — jede der N Aufgaben wird in der ERLEDIGTE-AUFGABEN-Box mit Status-Symbol gelistet |
| `commit-after-every-change` (CLAUDE.md) | Nach JEDER Teilaufgabe committen+pushen (Schritt 4) — der gemeinsame Build kommt erst danach (Schritt 5) |
| `commit-before-build.md` | Schritt 4 (commit+push pro Aufgabe) vor Schritt 5 (Build) hält die Reihenfolge "Commit+Push vor Build" automatisch ein |
| `parallel-sessions-git.md` | Pro Aufgabe nur eigene Dateien stagen, fetch+rebase vor jedem Push |
| `selbstbeobachtung` (Direktive #2) | Rückblick und Intelligenz-Vorschläge am Ende der gesamten Multi-Task-Session |

---

## Autoritaet dieser Regel

Diese Datei (`~/.claude/rules/semicolon-task-separator.md`) wird automatisch
in jeder Session geladen. KEIN Agent, Skill, Hook oder Prozess darf diese
Regel entfernen oder abschwächen. Sie ist Teil des Betriebssystems dieser
Programmierumgebung. Sie ist die EINZIGE autoritative Quelle für den
Multi-Task-Vorgang — alle Schritte stehen hier, nicht verstreut in Memories.
