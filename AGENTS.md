# AGENTS.md — Agent-Optimized Context for Claude Code Agents

> This file is for AGENTS (coder, tester, reviewer, optimizer), not humans.
> It provides the minimum context needed for efficient autonomous work.
> Source: arXiv 2601.20404 — structured agent context reduces tool calls by 15-30%.

## Repository Structure

```
~/proggs/                          # Monorepo: Pepsi1978/proggs
  BestJournalAndroid/              # Kotlin/Compose journal app (Google Play)
  BestJournalFrank/                # Kotlin/Compose journal app (clone, no Firebase, no Premium)
  claude-code-setup/               # Claude Code config sync (hooks, rules, agents, settings)
  ClaudeCodexVoiceOverlay-macOS/   # Swift voice overlay
  ClaudeVoiceOverlay-Windows/      # C#/WPF voice overlay
  QuizVerse/                       # Kotlin quiz app
  mcp-code-search/                 # Local semantic search server
  tools/                           # Shared utilities
```

## Build Commands

| Project | Build | Test | Lint |
|---------|-------|------|------|
| BestJournalAndroid | `cd BestJournalAndroid && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| BestJournalFrank | `cd BestJournalFrank && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| C#/WPF | `dotnet build -c Release` | `dotnet test` | `dotnet format analyzers` |

## Files Agents Must NEVER Modify

- `~/.claude/rules/superintelligence.md` — Directive #1 (protected)
- `~/.claude/rules/self-observation.md` — Directive #2 (protected)
- `~/.claude/rules/resilient-bugfixing.md` — Directive #3 (protected)
- `~/.claude/rules/bypass-permissions-permanent.md` — Security (protected)
- `~/.claude/settings.json` permissions section — bypassPermissions must stay
- `.mcp.json` — Platform-specific, never auto-modify
- Any file in `~/Codex/` — Forbidden directory

## File Ownership Rules

- Two agents must NEVER edit the same file simultaneously
- Each agent gets assigned files in its prompt — stay within those files
- If you need to read a file another agent owns: READ only, never WRITE

## Android-Specific Conventions (BestJournal apps)

- Package: `com.bestjournal.app` (Android) / `com.entropyjournal` (Frank)
- Architecture: MVVM + Hilt DI + Room DB + Coroutines/Flow
- UI: Jetpack Compose, Material3, Dark Mode with 3 profiles
- Frank app: NO Firebase, NO Premium features, NO analytics
- Both apps must maintain feature parity (see feedback_frank_porting_pattern.md)
- Settings buttons: `wrapContentSize` + `Row(Center)`, never `fillMaxWidth`
- DB migrations: Always export schema JSON to `app/schemas/`
- After ADB install: Always auto-launch the app

## Commit Format

`#NNN - Description` (English). Number auto-incremented from last commit.

## Quality Requirements

- Every feature must pass `quality-gate` agent before commit
- Build must be green before commit (no broken builds pushed)
- Cross-platform: .ps1 hooks need .sh counterpart (and vice versa)
- Do not leave project files untracked. Anything an agent creates or edits in the repo must be committed and pushed to GitHub, except secrets/local/generated files which must be explicitly ignored or replaced with a safe template.

## Secrets: zentrale SK-Ablage (KRITISCH)

Alle API-Keys, Signing-Keys, Tokens und vertraulichen Zugangsdaten liegen zentral in
`$HOME/SK/` (`C:\Users\barwa\SK\` auf Windows, `/Users/barwa/SK/` auf macOS), niemals im Repo.
Vor Secrets-bezogenen Aenderungen immer `$HOME/SK/README.md` lesen.

- Niemals `.env`, `google-services.json`, `google-services-*.json`, `credentials.json`,
  Keystores, `keystore.properties`, API-Keys oder Tokens committen.
- Niemals `.gitignore`-Ausnahmen wie `!app/src/debug/google-services.json`, `!*.keystore`
  oder `!.env` erstellen oder belassen.
- Android nutzt einen `syncSecretsFromSk`-Gradle-Task; C#/.NET, Swift, Python und Node
  suchen `$HOME/SK/<projekt-name>/.env` bzw. Projektdateien aus SK als erste Prioritaet.
- Ins Repo gehoeren nur `.example`-/`.template`-Dateien mit `REDACTED`-Werten.
- Release-Keystores, besonders `$HOME/SK/BestJournalAndroid/release.keystore`, sind
  unwiederbringbar und muessen extern/verschluesselt gesichert werden.

# Parallele Sessions — Commit & Push am geteilten main-Branch (KRITISCH)

Diese Regel gilt AUTOMATISCH in JEDER Session, auf ALLEN Plattformen
(Windows + macOS), unabhaengig davon welches CLI gerade laeuft (Codex,
Claude Code, Gemini CLI, Terminal). Der Benutzer arbeitet oft mit 4-5
gleichzeitig offenen Sessions am selben Repo. Alle Sessions pushen auf main.
Diese Regel stellt sicher, dass trotzdem nichts ueberschrieben wird und nichts
kaputtgeht.

## Grundprinzip

Es wird IMMER auf `main` committet und nach `origin/main` gepusht.
Keine Feature-Branches. Keine Worktrees. Kein Merge-Dance. Nur `main` —
aber mit diszipliniertem `fetch + rebase + push` vor jedem Push.

## Einmalige Git-Konfiguration (MUSS gesetzt sein)

Vor der ersten Session auf einem neuen Rechner diese drei Zeilen setzen.
Sie machen 90% der Konflikte unsichtbar:

    git config --global pull.rebase true        # Pull nutzt IMMER rebase, nie merge
    git config --global rebase.autoStash true   # Lokale Aenderungen automatisch stashen+poppen
    git config --global rerere.enabled true     # Konfliktloesungen werden gemerkt

Pflicht beim Session-Start: pruefen ob diese Werte gesetzt sind, falls nicht
einmalig setzen und kurz melden.

## Der Standard-Ablauf nach jeder Aufgabe

Nach JEDER abgeschlossenen Aufgabe wird SOFORT committet und gepusht.
Die Reihenfolge ist IMMER dieselbe — keine Abkuerzungen, keine Umwege:

    # 1. Nur eigene Dateien namentlich stagen — NIE git add -A oder git add .
    git add <datei-1> <datei-2> <datei-3>

    # 2. Commit mit fortlaufender Nummer
    git commit -m "#NNNN - Beschreibung auf Englisch"

    # 3. Vor dem Push IMMER synchronisieren
    git fetch origin
    git rebase origin/main

    # 4. Push
    git push

Die vier Schritte sind atomar. Kein Push ohne vorheriges fetch+rebase.

## Verhalten bei Push-Rejection

Eine andere Session war schneller und hat zwischenzeitlich gepusht. Die Reaktion
ist IMMER dieselbe — ruhig bleiben, nicht force-pushen:

    git fetch origin && git rebase origin/main && git push

Einfach wiederholen. Bei der zweiten Rejection wieder wiederholen.

Bei der dritten Rejection in Folge: Stop. Kurz 5-10 Sekunden warten, damit die
andere Session ihren Push-Burst fertig hat. Dann nochmal versuchen. Wenn das
immer noch fehlschlaegt: dem Benutzer melden, nicht stumm weiterprobieren.

Niemals als Ausweg: `--force`, `--force-with-lease`, `reset --hard`, neues
Repo klonen. Diese zerstoeren Arbeit der anderen Sessions.

## Umgang mit fremden Aenderungen im Working Tree

Beim Session-Start kann der Working Tree uncommittete Dateien von einer
frueheren Session oder einer parallel laufenden Session enthalten. Diese
Aenderungen duerfen NIEMALS mitcommittet werden — sie gehoeren dieser
Session nicht.

### Vor dem Commit: Nur eigene Dateien stagen

    # FALSCH — greift alles was im Working Tree liegt
    git add -A
    git add .

    # RICHTIG — nur die Dateien die DIESE Session geaendert hat
    git add pfad/zu/datei1.kt pfad/zu/datei2.xml

Falls versehentlich fremde Dateien im Index landen:

    git restore --staged <fremde-datei>

### Bei Rebase-Blockade durch fremde unstaged Aenderungen

Fehler: `error: cannot rebase: You have unstaged changes.`

Vorgehen:

    # Fremde Aenderungen kurz beiseite packen
    git stash push -u -m "hold-NNNN"

    # Jetzt laeuft Rebase + Push durch
    git fetch origin && git rebase origin/main && git push

    # Fremde Aenderungen wieder zurueckholen
    git stash pop

Die fremden Aenderungen liegen dann wieder unversioniert im Working Tree und
koennen spaeter von ihrer eigenen Session committet werden.

## Die absoluten Tabus

Diese Aktionen sind in parallelen Session-Setups VERBOTEN. Sie zerstoeren
garantiert die Arbeit anderer Sessions:

- `git push --force` — ueberschreibt Commits anderer Sessions auf origin/main
- `git push --force-with-lease` — schuetzt nur gegen lokale Konflikte, nicht
  gegen parallele Sessions
- `git reset --hard` ohne explizite Benutzer-Freigabe — zerstoert unversionierte
  Arbeit anderer Sessions
- `git add -A` / `git add .` — greift fremde In-Flight-Dateien ab
- `git commit --amend` auf bereits gepushte Commits — zwingt zum Force-Push
- `git rebase -i origin/main` (interaktiver Rebase ueber bereits gepushte
  Commits) — veraendert oeffentliche History
- Warten ohne Grund ("sleep bis der andere fertig ist") — blockiert eigene
  Arbeit ohne Nutzen

## Commit-Granularitaet

Ein Commit = ein abgeschlossener Zweck.

- 1 Datei, 1 Fix                 → 1 Commit
- 1 Feature, mehrere Dateien     → 1 Commit nach Abschluss
- Groesseres Feature (>15 Min)   → Mehrere Commits nach logischen Teilschritten

Faustregel: Lieber 5 kleine Commits als 1 grosser. Jeder kleine Commit ist ein
Rettungspunkt und reduziert die Wahrscheinlichkeit eines Rebase-Konflikts mit
anderen Sessions.

Keine Sammel-Commits mit mehreren unabhaengigen Aufgaben. Das erschwert
Revert und erhoeht Konfliktwahrscheinlichkeit.

## Unsicherheit: Was machen die anderen Sessions gerade?

Wenn unklar ist ob eine andere Session gerade dieselben Dateien bearbeitet,
diese drei Befehle nacheinander laufen lassen:

    # 1. Welche Dateien hat DIESE Session geaendert?
    git status --short

    # 2. Hat eine andere Session schon weiter gepusht?
    git fetch origin && git log HEAD..origin/main --oneline

    # 3. Gibt es neue Remote-Commits die dieselben Dateien anfassen?
    git fetch origin && git diff --name-only HEAD origin/main

Wenn die dritte Ausgabe eine Datei zeigt die auch diese Session geaendert hat:
Besonders klein committen und sofort pushen, damit der Konflikt schnell
aufgeloest wird statt zu wachsen.

## Commit-Nachrichten (Format)

Jede Commit-Nachricht beginnt mit einer fortlaufenden Nummer:

    #NNNN - Beschreibung auf Englisch

Die Nummerierung wird anhand der existierenden Commits im Repo automatisch
ermittelt (letzte Nummer + 1).

## Pre-Push-Check: Keine eigene Datei darf vergessen werden (KRITISCH)

Die Regel "nur eigene Dateien namentlich stagen" schuetzt davor, fremde Arbeit
einer parallelen Session zu klauen — hat aber eine Luecke: sie kann nicht
pruefen ob DEINE eigenen Aenderungen vollstaendig sind. Wenn eine eigene Datei
nicht gestaged wird, bleibt sie nach dem Push als unstaged liegen und geht bei
der naechsten Operation womoeglich verloren.

### Pflicht-Ablauf VOR jedem `git push`

Unmittelbar nach `git commit` und vor `git push` MUSS dieser Check laufen:

    git status --short

Jede Zeile in der Ausgabe MUSS bewusst einer dieser drei Kategorien zugeordnet
werden:

| Symbol / Praefix                         | Typ                                        | Aktion |
|------------------------------------------|--------------------------------------------|--------|
| `??`, `M `, ` M`, `MM`, `A `, `AM` — eigene Aenderung | Datei gehoert zu DIESER Aufgabe    | STOP. Erst `git add <pfad>` + neuer Commit, DANN push |
| `??`, `M ` etc. — fremde Aenderung       | Gehoert zu fremder paralleler Session      | Ignorieren, aber dem Benutzer 1 Zeile melden: "Datei X liegt unstaged, gehoert nicht zu dieser Aufgabe" |
| `??` — lokaler Muell                     | Build-Artefakte, Temp-Dateien, .env, Backups | Ignorieren oder in .gitignore eintragen, niemals committen |
| Ausgabe leer                             | Working Tree sauber                        | Push ist sauber, weitermachen |

### Was "bewusst zuordnen" bedeutet

Pro Zeile gedanklich beantworten:
1. Habe ich diese Datei geaendert? Ja → gehoert zu meiner Aufgabe, muss committed werden
2. Nein, aber sie gehoert zum Projekt? → fremde Session, ignorieren aber melden
3. Nein, und sie gehoert nicht zum Projekt? → Muell, ignorieren

Nicht einfach `git status --short` ansehen und denken "passt schon". Jede Zeile
wird einzeln klassifiziert.

### Pflicht-Meldung an den Benutzer (wenn fremde Dateien vorhanden)

Wenn nach dem Check fremde unstaged Dateien im Working Tree liegen, MUSS in der
Status-Meldung eine kurze Zeile erscheinen, z.B.:

    Hinweis: 2 Dateien liegen noch unstaged (fremde Session?):
      .claude/scheduled_tasks.lock
      some-other-file.md
    Nicht committed, weil nicht zu dieser Aufgabe gehoerend.

Damit weiss der Benutzer dass der Check gelaufen ist und die Dateien bewusst
liegengelassen wurden — nicht aus Vergesslichkeit.

### Was NIEMALS passieren darf

- `git push` ohne vorheriges `git status --short`
- Eigene Dateien unstaged lassen weil "der Push ist schon durch"
- `git status --short` ausfuehren, aber die Ausgabe nicht Zeile fuer Zeile klassifizieren
- Fremde Dateien blind mit `git add -A` mitnehmen "damit der Check sauber ist"
- Die Pflicht-Meldung an den Benutzer weglassen wenn fremde Dateien da sind

### Warum das noetig ist (Poka-Yoke Stufe 2)

Diese Regel macht Vergesslichkeit bei eigenen Dateien unmoeglich, ohne die
Sicherheit gegen parallele Sessions zu opfern. Der Benutzer sieht in jedem
Status-Bericht entweder "Push sauber" oder eine explizite Liste
liegengelassener Dateien — nie mehr "irgendwas fehlt und keiner merkt's".

## Zusammenfassung in einem Satz

Parallele Sessions sind OK solange jede Session vor dem Push
`git status --short` ausfuehrt, nur ihre eigenen Dateien namentlich staged,
`git fetch + git rebase origin/main` macht und bei Rejection einfach
`fetch + rebase + push` wiederholt — statt zu force-pushen oder zu resetten.

## Geltungsbereich

Diese Regel ist CLI-agnostisch. Sie gilt identisch fuer Codex, Claude Code,
Gemini CLI und jede andere Session die auf demselben Git-Repo arbeitet.
Jedes CLI muss sie unabhaengig kennen und befolgen, weil die Sessions nicht
miteinander kommunizieren koennen — nur der sauber disziplinierte
Git-Workflow verhindert Kollisionen.

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

Diese Datei wird automatisch in jeder Session geladen. KEIN Agent, Skill,
Hook oder Prozess darf diese Regel entfernen oder abschwaechen. Sie ist
Teil des Betriebssystems dieser Programmierumgebung — CLI-agnostisch
(gilt fuer Codex, Claude Code, Gemini CLI und jede andere Session).

---

# Funktionierende Bugfixes ins zweite Gehirn (Cortex/Second Brain) abspeichern (KRITISCH)

> Gilt fuer JEDES Modell unter OpenCode (auch schwache). Ergaenzt **Direktive #3 (Resilient
> Bugfixing)** — die VOR jedem Bugfix gilt (Root Cause finden, funktionserhaltend fixen, verifizieren).
> Diese Regel ist der Ablage-Schritt DANACH. Gesetzt 2026-06-26 (Frank). Codex hat das Format
> eingefuehrt; ab jetzt machen es ALLE CLIs gleich, damit ein gemeinsames Fehler-Gedaechtnis entsteht.

**Die Regel:** Sobald ein Bugfix **bestaetigt FUNKTIONIERT**, schreibe ihn als EINEN Eintrag ueber den
`second-brain`-MCP (`remember`) ins Gehirn — im festen Format, unter `bugfixes/<passende Unterkategorie>`.

**Wann gilt "funktioniert"? (entscheidend — nur funktionierende Fixes ablegen)**
- Hast du es OBJEKTIV verifiziert (Build gruen, Tests ok, Deploy `healthy`, Symptom reproduzierbar weg)
  → speichern.
- Kann es nur der Benutzer beurteilen (Optik/Gefuehl) ODER bist du unsicher → EINMAL kurz fragen
  **"Hat der Fix funktioniert?"** → erst bei **Ja** speichern.
- Default = NICHT speichern, bis bestaetigt. So muss nie etwas geloescht werden. (Sagt der Benutzer
  spaeter "doch nicht funktioniert": den Eintrag per `forget` entfernen, nach echtem Fix neu schreiben.)

**Titel:** `Bugfix <App> <Bereich> <YYYY-MM-DD HH:MM>` — fuer einen Menschen sofort verstaendlich.
`<HH:MM>` = aktuelle LOKALE Uhrzeit (Europe/Berlin, 24h) im Moment des Speicherns -> sofort sichtbar,
WANN genau der Fix ins Gehirn kam (nicht nur an welchem Tag); macht Fixes am selben Bereich eindeutig.
(Beispiele: `Bugfix Cortex Vorlesen Toggle Layout 2026-06-26 14:23`, `Bugfix Cortex Gehirn Kategorie Drilldown 2026-06-26 15:07`.)

**Kategorie:** `bugfixes/<unterkategorie>` — ZUERST pruefen ob es schon eine sinnvolle Unterkategorie
gibt (Kategorie-Liste / `get_by_category` ansehen) und dort einordnen; nur wenn keine passt, eine neue
sprechende anlegen (z.B. `bugfixes/cortex-dashboard`, `bugfixes/brain-api`).

**Inhalt:** `Bugfix <YYYY-MM-DD HH:MM>: <App> <Bereich>. Symptom: … Root Cause: … Fix: … Verwandte Pruefung: …
Verifikation: … Funktionalitaets-Diff: … [Poka-Yoke: …]` (dieselben Bausteine wie Direktive #3).

**Danach** dem Benutzer in einem Satz melden: "Im Gehirn dokumentiert: <Titel> [<Kategorie>]."

**Sinn:** Spaeter "hatten wir sowas schon?" → unter `bugfixes/` + Unterkategorie eingrenzen (semantische
Suche `recall`), den Fall finden und wiederverwenden. Sammeln sich aehnliche Faelle → daraus wird eine
Best Practice. Der Repo-Bug-Almanach (`bugs/`) bleibt das proaktive Tech-Wissen; das Gehirn ist die
zentrale, CLI-uebergreifende Fall-Akte.

**NIEMALS:** unbestaetigte Fixes speichern · den Doku-Schritt weglassen · abweichendes Titel-Format ·
blind neue Unterkategorie trotz passender vorhandener · kryptische Titel.
