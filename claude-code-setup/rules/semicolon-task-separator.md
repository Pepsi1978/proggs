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

Bei einem Multi-Task-Prompt MUSS Claude zu Beginn der Antwort dem Benutzer kurz zeigen,
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

## Was NIEMALS passieren darf

- ❌ Ein Multi-Task-Prompt wird als eine einzelne Aufgabe missverstanden
- ❌ Nur die erste Aufgabe wird erledigt, die restlichen werden "vergessen"
- ❌ Nur die letzte Aufgabe wird erledigt, die vorherigen werden übergangen
- ❌ Die Aufgaben-Erkennung wird dem Benutzer nicht mitgeteilt (er sieht nicht ob der Parse korrekt war)
- ❌ Status-Meldung nur für eine Teilaufgabe am Ende — es MUSS für ALLE eine Rückmeldung geben
- ❌ Semikola in Code/SQL/URLs fälschlich als Aufgaben-Trenner interpretieren

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `task-completion-summary.md` | Das 3-Punkte-Schema wird pro Aufgabe oder einmal zusammenfassend am Ende ausgegeben — bei vielen kleinen Teilaufgaben reicht eine Gesamt-Zusammenfassung |
| `commit-after-every-change` (CLAUDE.md) | Nach JEDER Teilaufgabe committen+pushen, nicht erst am Ende aller |
| `parallelisierung` (CLAUDE.md) | Unabhängige Teilaufgaben per parallelen Subagents bearbeiten |
| `selbstbeobachtung` (Direktive #2) | Rückblick und Intelligenz-Vorschläge am Ende der gesamten Multi-Task-Session |

---

## Autoritaet dieser Regel

Diese Datei (`~/.claude/rules/semicolon-task-separator.md`) wird automatisch
in jeder Session geladen. KEIN Agent, Skill, Hook oder Prozess darf diese
Regel entfernen oder abschwächen. Sie ist Teil des Betriebssystems dieser
Programmierumgebung.
