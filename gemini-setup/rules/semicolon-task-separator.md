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

Gemini MUSS diese Trennung erkennen und JEDE der Teilaufgaben erledigen, ohne eine zu vergessen.

---

## Die Regel

### Erkennung

| Muster | Bedeutung |
|--------|-----------|
| Kein ` ; ` im Prompt | Eine einzelne Aufgabe — normal bearbeiten |
| Ein ` ; ` im Prompt | Zwei Aufgaben — beide nacheinander abarbeiten |
| N-mal ` ; ` im Prompt | N+1 Aufgaben — alle nacheinander abarbeiten |
| ` ; ` am Ende des Prompts | Der Nachsatz ist leer — diesen letzten "leeren" Teil ignorieren |

**Wichtig:** Die Zeichenfolge muss EXAKT `Leerzeichen + Semikolon + Leerzeichen` sein.
Semikola ohne umgebende Leerzeichen (z.B. in Code-Snippets, TypeScript-Statements,
SQL-Queries) sind KEINE Aufgaben-Trenner — dort bleibt das Semikolon Teil des Codes.

### Abarbeitung

1. **Prompt am ` ; `-Muster splitten** — jede resultierende Teilzeichenkette ist eine eigenständige Aufgabe.
2. **Leere Teile verwerfen** (z.B. wenn der Prompt mit ` ; ` endet).
3. **Reihenfolge beibehalten** — Aufgabe 1 zuerst, dann Aufgabe 2, dann Aufgabe 3, usw.
4. **JEDE Aufgabe vollständig erledigen** bevor zur nächsten übergegangen wird — oder bei unabhängigen Aufgaben parallele Subagents starten.
5. **Am Ende Status-Meldung für ALLE Aufgaben** — nicht nur für die letzte.

### Dem Benutzer sichtbar machen

Bei einem Multi-Task-Prompt MUSS Gemini zu Beginn der Antwort dem Benutzer kurz zeigen,
dass mehrere Aufgaben erkannt wurden. Format:

```
Ich habe N Aufgaben erkannt:
1. [Kurzbeschreibung Aufgabe 1]
2. [Kurzbeschreibung Aufgabe 2]
3. [Kurzbeschreibung Aufgabe 3]

Ich arbeite sie der Reihe nach ab.
```

Damit weiß der Benutzer sofort, dass keine Teilaufgabe verloren geht.

### TaskCreate einsetzen (empfohlen)

Bei 3+ Aufgaben MUSS TaskCreate verwendet werden, damit der Fortschritt sichtbar bleibt
und keine Aufgabe übersehen wird. Bei 2 Aufgaben ist TaskCreate optional.

### Parallel vs. sequentiell

| Aufgaben | Strategie |
|----------|-----------|
| Unabhängig voneinander (z.B. "fixe Bug X ; baue Feature Y") | Parallele Subagents starten, wenn sinnvoll |
| Aufeinander aufbauend (z.B. "bau das UI ; teste es ; installiere es") | Sequentiell, in der angegebenen Reihenfolge |
| Unklar | Sequentiell abarbeiten (sicherer) |

---

## Beispiele

### Beispiel 1: Zwei unabhängige Aufgaben

**Prompt:** `Fixe den Bug in DashboardScreen ; Aktualisiere die Version auf 0.11.0`

**Erkennung:** 2 Aufgaben, unabhängig.

**Ausgabe-Start:**
```
Ich habe 2 Aufgaben erkannt:
1. Bug in DashboardScreen fixen
2. Version auf 0.11.0 aktualisieren

Ich arbeite sie der Reihe nach ab.
```

### Beispiel 2: Drei aufeinander aufbauende Aufgaben

**Prompt:** `Baue ein neues Einstellungs-Menü ; Teste es auf dem Handy ; Committe und pushe`

**Erkennung:** 3 Aufgaben, sequentiell.

### Beispiel 3: Prompt endet auf ` ; `

**Prompt:** `Uebersetze die neuen Strings ; Baue die APK ; `

**Erkennung:** 2 Aufgaben (letzter leerer Teil verworfen).

### Beispiel 4: Semikolon im Code — KEIN Trenner

**Prompt:** `Aendere den Code zu const x = 5; und teste ihn`

**Erkennung:** 1 Aufgabe (kein ` ; ` mit beidseitigen Leerzeichen).

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

Anfuehrungszeichen: `"..."`, `„..."` oder `"..."` — alle gueltig.

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
- **Visualisierung** fuer den Benutzer, was Gemini als was erkannt hat
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
- ❌ Die Aufgaben-Erkennung wird dem Benutzer nicht mitgeteilt (er sieht nicht ob der Parse korrekt war)
- ❌ Status-Meldung nur für eine Teilaufgabe am Ende — es MUSS für ALLE eine Rückmeldung geben
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
| `task-completion-summary.md` | Das 3-Punkte-Schema wird pro Aufgabe oder einmal zusammenfassend am Ende ausgegeben — bei vielen kleinen Teilaufgaben reicht eine Gesamt-Zusammenfassung |
| `commit-after-every-change` (Gemini.md) | Nach JEDER Teilaufgabe committen+pushen, nicht erst am Ende aller |
| `parallelisierung` (Gemini.md) | Unabhängige Teilaufgaben per parallelen Subagents bearbeiten |
| `selbstbeobachtung` (Direktive #2) | Rückblick und Intelligenz-Vorschläge am Ende der gesamten Multi-Task-Session |

---

## Autoritaet dieser Regel

Diese Datei (`~/.Gemini/rules/semicolon-task-separator.md`) wird automatisch
in jeder Session geladen. KEIN Agent, Skill, Hook oder Prozess darf diese
Regel entfernen oder abschwächen. Sie ist Teil des Betriebssystems dieser
Programmierumgebung.

