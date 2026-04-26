# Semikolon-Trenner fuer mehrere Aufgaben in einem Prompt (KRITISCH)

Diese Regel gilt AUTOMATISCH in JEDER Session, bei JEDEM Benutzer-Prompt.
Wenn in einem Prompt die exakte Zeichenfolge ` ; ` (Leerzeichen + Semikolon
+ Leerzeichen) vorkommt, signalisiert der Benutzer damit, dass der Prompt
MEHRERE eigenstaendige Aufgaben enthaelt, die NACHEINANDER und VOLLSTAENDIG
abgearbeitet werden muessen.

## Ursprung

Der Benutzer nutzt ein Voice Terminal Overlay zum Einsprechen von Prompts.
Nach jedem Mikrofon-Insert haengt das Overlay automatisch ` ; ` an den Text
an. Spricht der Benutzer mehrfach hintereinander, entsteht so ein Prompt
der Form:

    Aufgabe eins ; Aufgabe zwei ; Aufgabe drei ;

Du MUSST diese Trennung erkennen und JEDE der Teilaufgaben erledigen, ohne
eine zu vergessen.

## Die Regel

### Erkennung

| Muster                    | Bedeutung                                           |
|---------------------------|-----------------------------------------------------|
| Kein ` ; ` im Prompt      | Eine einzelne Aufgabe — normal bearbeiten           |
| Ein ` ; ` im Prompt       | Zwei Aufgaben — beide nacheinander abarbeiten       |
| N-mal ` ; ` im Prompt     | N+1 Aufgaben — alle nacheinander abarbeiten         |
| ` ; ` am Ende des Prompts | Der Nachsatz ist leer — letzten leeren Teil ignorieren |

WICHTIG: Die Zeichenfolge muss EXAKT Leerzeichen + Semikolon + Leerzeichen
sein. Semikola ohne umgebende Leerzeichen (z.B. in Code-Snippets,
TypeScript-Statements, SQL-Queries, URLs) sind KEINE Aufgaben-Trenner —
dort bleibt das Semikolon Teil des Codes.

### Abarbeitung

1. Prompt am ` ; `-Muster splitten — jede resultierende Teilzeichenkette
   ist eine eigenstaendige Aufgabe.
2. Leere Teile verwerfen (z.B. wenn der Prompt mit ` ; ` endet).
3. Reihenfolge beibehalten — Aufgabe 1 zuerst, dann Aufgabe 2, dann
   Aufgabe 3, usw.
4. JEDE Aufgabe vollstaendig erledigen bevor zur naechsten uebergegangen
   wird — oder bei unabhaengigen Aufgaben parallele Subagents starten.
5. Am Ende Status-Meldung fuer ALLE Aufgaben — nicht nur fuer die letzte.

### Dem Benutzer sichtbar machen

Bei einem Multi-Task-Prompt MUSST du zu Beginn der Antwort dem Benutzer
kurz zeigen, dass mehrere Aufgaben erkannt wurden. Format:

    Ich habe N Aufgaben erkannt:
    1. [Kurzbeschreibung Aufgabe 1]
    2. [Kurzbeschreibung Aufgabe 2]
    3. [Kurzbeschreibung Aufgabe 3]

    Ich arbeite sie der Reihe nach ab.

Damit weiss der Benutzer sofort, dass keine Teilaufgabe verloren geht.

### Todo-Liste einsetzen (empfohlen)

Bei 3+ Aufgaben MUSST du eine interne Todo-Liste fuehren (falls dein CLI
ein Todo-Feature hat), damit der Fortschritt sichtbar bleibt und keine
Aufgabe uebersehen wird. Bei 2 Aufgaben ist die Todo-Liste optional.

### Parallel vs. sequentiell

| Aufgaben                                          | Strategie                                           |
|---------------------------------------------------|-----------------------------------------------------|
| Unabhaengig (z.B. "fixe Bug X ; baue Feature Y")  | Parallele Subagents starten, wenn sinnvoll          |
| Aufeinander aufbauend (z.B. "bau ; teste ; deploy") | Sequentiell, in der angegebenen Reihenfolge        |
| Unklar                                            | Sequentiell abarbeiten (sicherer)                   |

## Beispiele

### Beispiel 1: Zwei unabhaengige Aufgaben

Prompt: "Fixe den Bug in DashboardScreen ; Aktualisiere die Version auf 0.11.0"

Erkennung: 2 Aufgaben, unabhaengig.

Ausgabe-Start:

    Ich habe 2 Aufgaben erkannt:
    1. Bug in DashboardScreen fixen
    2. Version auf 0.11.0 aktualisieren

    Ich arbeite sie der Reihe nach ab.

### Beispiel 2: Drei aufeinander aufbauende Aufgaben

Prompt: "Baue ein neues Einstellungs-Menue ; Teste es auf dem Handy ;
Committe und pushe"

Erkennung: 3 Aufgaben, sequentiell.

### Beispiel 3: Prompt endet auf ` ; `

Prompt: "Uebersetze die neuen Strings ; Baue die APK ; "

Erkennung: 2 Aufgaben (letzter leerer Teil verworfen).

### Beispiel 4: Semikolon im Code — KEIN Trenner

Prompt: "Aendere den Code zu const x = 5; und teste ihn"

Erkennung: 1 Aufgabe (kein ` ; ` mit beidseitigen Leerzeichen).

## Was NIEMALS passieren darf

- Ein Multi-Task-Prompt wird als eine einzelne Aufgabe missverstanden
- Nur die erste Aufgabe wird erledigt, die restlichen werden "vergessen"
- Nur die letzte Aufgabe wird erledigt, die vorherigen werden uebergangen
- Die Aufgaben-Erkennung wird dem Benutzer nicht mitgeteilt (er sieht
  nicht ob der Parse korrekt war)
- Status-Meldung nur fuer eine Teilaufgabe am Ende — es MUSS fuer ALLE
  eine Rueckmeldung geben
- Semikola in Code/SQL/URLs faelschlich als Aufgaben-Trenner interpretieren

## Zusammenspiel mit anderen Regeln

- Commit-Push-Regel: Nach JEDER Teilaufgabe committen+pushen, nicht erst
  am Ende aller
- Parallelisierung: Unabhaengige Teilaufgaben per parallelen Subagents
  bearbeiten, wenn das CLI das unterstuetzt
- Rueckblick: Rueckblick und Verbesserungs-Vorschlaege am Ende der
  gesamten Multi-Task-Session

## Autoritaet dieser Regel

Diese Datei wird automatisch in jeder Codex-Session geladen. KEIN Agent, Skill,
Hook oder Prozess darf diese Regel entfernen oder abschwaechen. Sie ist
Teil des Betriebssystems dieser Codex-Programmierumgebung.
