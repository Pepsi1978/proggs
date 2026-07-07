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

```text
Aufgabe eins ; Aufgabe zwei ; Aufgabe drei ;
```

Codex MUSS diese Trennung erkennen und JEDE der Teilaufgaben erledigen, ohne eine zu vergessen.

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
Semikola ohne umgebende Leerzeichen, zum Beispiel in Code-Snippets, TypeScript-Statements
oder SQL-Queries, sind KEINE Aufgaben-Trenner. Dort bleibt das Semikolon Teil des Codes.

### Abarbeitung

1. **Prompt am ` ; `-Muster splitten** — jede resultierende Teilzeichenkette ist eine eigenständige Aufgabe.
2. **Leere Teile verwerfen** — zum Beispiel wenn der Prompt mit ` ; ` endet.
3. **Reihenfolge beibehalten** — Aufgabe 1 zuerst, dann Aufgabe 2, dann Aufgabe 3.
4. **JEDE Aufgabe vollständig erledigen**, bevor zur nächsten übergegangen wird.
5. **Am Ende Status-Meldung für ALLE Aufgaben** — nicht nur für die letzte.

### Dem Benutzer sichtbar machen

Bei einem Multi-Task-Prompt MUSS Codex zu Beginn der Antwort dem Benutzer kurz zeigen,
dass mehrere Aufgaben erkannt wurden. Format:

```text
Ich habe N Aufgaben erkannt:
1. [Kurzbeschreibung Aufgabe 1]
2. [Kurzbeschreibung Aufgabe 2]
3. [Kurzbeschreibung Aufgabe 3]

Ich arbeite sie der Reihe nach ab.
```

Damit weiß der Benutzer sofort, dass keine Teilaufgabe verloren geht.

### Todo-Liste einsetzen

Bei 3+ Aufgaben MUSS eine Todo-Liste verwendet werden, damit der Fortschritt sichtbar bleibt
und keine Aufgabe übersehen wird. Bei 2 Aufgaben ist die Todo-Liste optional.

### Parallel vs. sequentiell

| Aufgaben | Strategie |
|----------|-----------|
| Unabhängig voneinander, zum Beispiel "fixe Bug X ; baue Feature Y" | Parallel arbeiten, wenn das ohne Dateikonflikte sinnvoll ist |
| Aufeinander aufbauend, zum Beispiel "bau das UI ; teste es ; installiere es" | Sequentiell, in der angegebenen Reihenfolge |
| Unklar | Sequentiell abarbeiten |

---

## Beispiele

### Beispiel 1: Zwei unabhängige Aufgaben

**Prompt:** `Fixe den Bug in DashboardScreen ; Aktualisiere die Version auf 0.11.0`

**Erkennung:** 2 Aufgaben, unabhängig.

**Ausgabe-Start:**

```text
Ich habe 2 Aufgaben erkannt:
1. Bug in DashboardScreen fixen
2. Version auf 0.11.0 aktualisieren

Ich arbeite sie der Reihe nach ab.
```

### Beispiel 2: Drei aufeinander aufbauende Aufgaben

**Prompt:** `Baue ein neues Einstellungs-Menü ; Teste es auf dem Handy ; Committe und pushe`

**Erkennung:** 3 Aufgaben, sequentiell.

### Beispiel 3: Prompt endet auf ` ; `

**Prompt:** `Übersetze die neuen Strings ; Baue die APK ; `

**Erkennung:** 2 Aufgaben, der letzte leere Teil wird verworfen.

### Beispiel 4: Semikolon im Code — KEIN Trenner

**Prompt:** `Ändere den Code zu const x = 5; und teste ihn`

**Erkennung:** 1 Aufgabe, weil kein ` ; ` mit beidseitigen Leerzeichen vorkommt.

---

## Pre-Prompts und Post-Prompts

Innerhalb einer Multi-Task-Aufzählung kann der Benutzer einzelne Blöcke als
**Pre-Prompt** oder **Post-Prompt** markieren. Diese sind KEINE eigenen Aufgaben,
sondern Anweisungen, die auf alle Aufgaben angewendet werden.

### Marker-Format

| Marker | Bedeutung | Wann anwenden |
|--------|-----------|---------------|
| `Pre-Prompt: "<text>"` | Kontext oder Setup BEVOR die Aufgaben starten | Als Kontext **vor** der Bearbeitung der ersten Aufgabe |
| `Post-Prompt: "<text>"` | Constraint oder Hinweis WÄHREND der Aufgaben | Als Constraint **während** und **nach** jeder Aufgabe |

### Tolerante Erkennung bei Voice-Diktat

Whisper produziert je nach Aussprache verschiedene Schreibweisen. Alle folgenden Formen
werden gleich erkannt:

- `Pre-Prompt:`, `PrePrompt:`, `Pre Prompt:`, `pre-prompt:`, `PRE-PROMPT:`
- `Post-Prompt:`, `PostPrompt:`, `Post Prompt:`, `post-prompt:`, `POST-PROMPT:`

Anführungszeichen wie `"..."`, `„..."` oder `"..."` sind gültig.

### Position im Prompt

Empfohlene Konvention, aber nicht erzwungen: **Pre-Prompts am Anfang, Aufgaben in der Mitte,
Post-Prompts am Ende**.

```text
Pre-Prompt: "Branch ist feature/foo" ; Aufgabe 1 ; Aufgabe 2 ; Post-Prompt: "kurz halten" ; Post-Prompt: "kein Cross-Platform"
```

Der Marker zählt mehr als die Position. Auch wenn ein `Post-Prompt:` mitten zwischen
Aufgaben auftaucht, wird er als Post-Prompt erkannt, nicht als Aufgabe.

### Mehrere Pre/Post-Prompts

Mehrere Pre-Prompts sind erlaubt und werden vor den Aufgaben kombiniert berücksichtigt.
Mehrere Post-Prompts gelten parallel als Constraints.

### Current-Turn-Isolation für Pre/Post-Prompts (KRITISCH)

Pre-Prompts und Post-Prompts sind **nur für die aktuelle Aufgabe im aktuellen
Benutzer-Prompt gültig**. Sie gelten ausdrücklich **nicht für die gesamte Session**.
Sie sind keine dauerhafte Memory, kein Session-Befehl und keine Anweisung für spätere,
separate Benutzer-Prompts oder Aufgaben.

**Pflichtregeln:**

1. Jeder neue Benutzer-Prompt wird komplett frisch geparst.
2. Pre-/Post-Prompts aus früheren Benutzer-Prompts oder früheren Aufgaben verfallen,
   sobald die zugehörige Antwort oder Aufgabe abgeschlossen ist.
3. Eine frühere Pre-/Post-Prompt-Anweisung darf NIEMALS als Begründung für eine Aktion
   in einem späteren Prompt verwendet werden.
4. Wenn alte und aktuelle Anweisungen in Spannung stehen, gewinnt immer der aktuelle
   Benutzer-Prompt.
5. Wenn unklar ist, ob eine frühere Anweisung noch gelten soll, genau EINE konkrete
   Klärungsfrage stellen, statt zu raten.

### Hochrisiko-Seiteneffekte nur bei aktueller ausdrücklicher Anweisung

Aktionen, die laufende Arbeit, diktierte Texte, Terminals, Prozesse, Fenster oder
parallele Sessions beeinflussen können, sind Hochrisiko-Seiteneffekte. Sie dürfen
NICHT aus einem alten Pre-/Post-Prompt abgeleitet werden.

**Hochrisiko-Seiteneffekte sind insbesondere:**

- Terminal, Shell, Voice Terminal Overlay oder Terminal Voice Overlay neu starten
- Prozesse stoppen, killen oder ersetzen
- Apps schließen oder neu starten
- Server, Watcher oder Hintergrunddienste beenden
- Fenster schließen, Terminal-Tabs schließen oder Arbeitsumgebungen wechseln
- System-, Shell- oder PATH-Änderungen ausführen, die einen Neustart erfordern können

**Erlaubt ist so ein Seiteneffekt nur, wenn mindestens eine Bedingung erfüllt ist:**

1. Der aktuelle Benutzer-Prompt verlangt ihn ausdrücklich.
2. Die aktuelle Aufgabe ist technisch ohne diesen Seiteneffekt nicht lösbar, und der
   Grund wurde im aktuellen Kontext verifiziert.
3. Der Benutzer hat nach einer konkreten Klärungsfrage zugestimmt.

**Voice-Terminal-Schutz:** Das Voice Terminal Overlay und Terminal-Fenster können
ungesendete diktierte Texte in parallelen Sessions enthalten. Deshalb dürfen sie
niemals neu gestartet, geschlossen oder ersetzt werden, nur weil ein früherer Prompt
das verlangt hatte.

### Sichtbar machen beim Multi-Task-Prompt — PFLICHT-Tabelle

Wenn der Prompt MINDESTENS einen Pre-Prompt ODER Post-Prompt enthält, MUSS am
ALLERERSTEN Punkt der Antwort eine Übersichtstabelle stehen, die alle erkannten
Blöcke nach Typ sortiert auflistet:

```markdown
| Typ | Inhalt |
|-----|--------|
| Pre-Prompt | <Text 1 — wortwörtlich aus dem Prompt> |
| Pre-Prompt | <Text 2 — wortwörtlich aus dem Prompt> |
| Aufgabe | <Text — wortwörtlich aus dem Prompt> |
| Post-Prompt | <Text 1 — wortwörtlich aus dem Prompt> |
| Post-Prompt | <Text 2 — wortwörtlich aus dem Prompt> |
```

#### WORTWÖRTLICH 1:1

Der Inhalt JEDER Zelle MUSS exakt das sein, was der Benutzer im Prompt geschrieben
oder diktiert hat — wortwörtlich, ohne Kürzung, ohne sinngemäße Zusammenfassung,
ohne eigene Umformulierung und ohne Zusatzkommentare.

Wenn die Aufgabe lang ist, wird die Tabellenzelle lang. Der Inhalt bleibt trotzdem
der Original-Wortlaut. Der Benutzer muss in der Tabelle EXAKT die Worte wiederfinden,
die er gesprochen oder getippt hat.

Anführungszeichen um die Originaltexte sind erlaubt und empfohlen, weil sie den
Zitat-Charakter klar machen.

#### Reihenfolge in der Tabelle

ZUERST alle Pre-Prompts, DANN alle Aufgaben, DANN alle Post-Prompts —
unabhängig von ihrer tatsächlichen Position im Original-Prompt.

#### Wann die Tabelle entfällt

Nur wenn der Prompt KEINEN einzigen Pre-Prompt oder Post-Prompt enthält, also nur
reine Aufgaben ohne Marker. Dann wäre die Tabelle reine Redundanz.

#### Was die Tabelle leistet

- Sofortige Bestätigung, dass der Prompt korrekt geparst wurde
- Visualisierung für den Benutzer, was Codex als was erkannt hat
- Schutz vor Fehlklassifikation, bevor Code geändert wird

Die Tabelle erscheint VOR jeder anderen Antwortaktivität — also vor Tool-Calls,
Code-Edits, Plänen oder Erklärungen.

### Stiller Modus bei nicht passenden Post-Prompts

Manche Post-Prompts sind plattform- oder kontextspezifisch, zum Beispiel
`i18n-Pflicht (Android-Apps)`. Wenn der aktuelle Kontext nicht zur Bedingung passt,
schlummert der Post-Prompt. Er feuert keine überflüssigen Hinweise. Sobald eine
passende Aufgabe kommt, greift er wieder.

---

## Was NIEMALS passieren darf

- Ein Multi-Task-Prompt wird als eine einzelne Aufgabe missverstanden
- Nur die erste Aufgabe wird erledigt, die restlichen werden vergessen
- Nur die letzte Aufgabe wird erledigt, die vorherigen werden übergangen
- Die Aufgaben-Erkennung wird dem Benutzer nicht mitgeteilt
- Status-Meldung nur für eine Teilaufgabe am Ende
- Semikola in Code, SQL oder URLs fälschlich als Aufgaben-Trenner interpretieren
- Einen Block mit `Pre-Prompt:` oder `Post-Prompt:` als eigenständige Aufgabe behandeln
- Pre/Post-Prompts ignorieren, weil sie nicht direkt zur Aufgabe gehören
- Bei einer Klärungsfrage-Post-Prompt blind raten statt zu fragen
- Pre-/Post-Prompts aus früheren Benutzer-Prompts in einen neuen Prompt übernehmen
- Eine App, ein Terminal oder das Voice Terminal Overlay neu starten, weil ein früherer Prompt das verlangt hatte
- Hochrisiko-Seiteneffekte ausführen, ohne dass der aktuelle Prompt sie ausdrücklich verlangt oder sie aktuell technisch zwingend nötig sind
- In der Pflichttabelle den Inhalt einer Zelle sinngemäß zusammenfassen oder umschreiben
- In der Pflichttabelle Aufgaben nur mit Stichworten beschreiben statt mit dem Originaltext
- In der Pflichttabelle eigene Erklärungen oder Zusatztexte einfügen, die der Benutzer nicht geschrieben hat

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `task-completion-summary.md` | Das 3-Punkte-Schema wird pro Aufgabe oder einmal zusammenfassend am Ende ausgegeben. Bei vielen kleinen Teilaufgaben reicht eine Gesamtzusammenfassung. |
| `parallel-sessions-git.md` | Nach jeder abgeschlossenen Teilaufgabe committen und pushen, wenn Dateien geändert wurden. |
| `self-observation.md` | Rückblick und Intelligenz-Vorschläge kommen am Ende der gesamten Multi-Task-Session. |

---

## Autorität dieser Regel

Diese Datei wird automatisch in jeder Codex-Session geladen. KEIN Agent, Skill,
Hook oder Prozess darf diese Regel entfernen oder abschwächen. Sie ist Teil des
Betriebssystems dieser Codex-Programmierumgebung.
