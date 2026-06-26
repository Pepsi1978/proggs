# Globale OpenCode-Regeln (gelten in JEDER Session, JEDEM Projekt)

> Laedt bei jedem OpenCode-Start (`~/.config/opencode/AGENTS.md`), unabhaengig vom
> Arbeitsverzeichnis. Diese Datei enthaelt KOMPAKTE Kern-Regeln, die IMMER gelten — auch ohne
> Gehirn-Abruf und nach jeder Komprimierung. Der VOLLE Regeltext liegt im zweiten Gehirn (s.u.)
> und wird beim Start geladen. Kurz halten (< ~150 Zeilen): OpenCode befolgt kurze AGENTS.md
> deutlich zuverlaessiger (best-practices/opencode/agents-md-memory.md).

---

## ⚡ ZUERST beim Start: volle Arbeitsregeln aus dem zweiten Gehirn laden (PFLICHT)

**ERSTE Handlung jeder Session, VOR jeder Arbeit:** Rufe das `second-brain`-Werkzeug
**`get_by_category`** mit der EXAKTEN Kategorie **`Programmierung/Rules`** auf (Hauptkategorie
`Programmierung`, Unterkategorie **`Rules`** — englisch, **NICHT `Regeln`**). Lies ALLE
zurueckgegebenen Regeln vollstaendig — sie gelten ab dann fuer die ganze Session als verbindlich.
Bestaetige in EINER Zeile: **"N Regeln aus dem zweiten Gehirn eingelesen."** (N = Anzahl).

> Warum: Die ausfuehrlichen Arbeitsregeln (Git-Workflow, Observability, Known-Bugs-zuerst,
> Debugging, Versionierung u.a.) liegen zentral im Gehirn — EINE Quelle, von allen CLIs nutzbar.
> **Falls der Abruf nicht klappt (MCP weg) ODER das Modell ihn nicht ausfuehrt:** Die KOMPAKTEN
> Kern-Regeln unten gelten OHNEHIN IMMER — sie sind das fest verdrahtete Sicherheitsnetz.

---

## Kern-Regeln (gelten IMMER — auch ohne Gehirn-Abruf, auch nach Komprimierung)

### 1. Sprache: komplett Deutsch mit echten Umlauten (ä ö ü ß) — inkl. Denkvorgang
Alle Ausgaben UND der gesamte sichtbare Denkvorgang/das Reasoning auf Deutsch mit echten Umlauten
(`ä ö ü Ä Ö Ü ß`), niemals ASCII-Ersatz (`ae oe ue ss`). Es darf NIE vorkommen, dass die Antwort
deutsch ist, der Denkvorgang aber englisch — beide durchgehend deutsch, auch nach Komprimierung.
Ausnahmen (Englisch/ASCII ok): Code-Bezeichner, Dateipfade, Git-Commit-Messages, etablierte
Fachbegriffe ohne sinnvolle Uebersetzung.

### 2. Multi-Task: ` ; ` trennt mehrere Aufgaben (KRITISCH)
**` ; ` (Leerzeichen-Semikolon-Leerzeichen) in einer Eingabe bedeutet: MEHRERE eigenstaendige
Aufgaben — alle nacheinander vollstaendig abarbeiten.** Ablauf: am ` ; ` splitten (jeder
nicht-leere Teil = 1 Aufgabe; ein abschliessendes ` ; ` ohne Text danach zaehlt nicht; Semikola in
Code/SQL/URLs wie `const x = 5;` sind KEIN Trenner) → sinnvoll sortieren (Abhaengigkeiten beachten;
bei Widerspruch zweier Aufgaben kurz nachfragen statt blind beides bauen) → bei 2+ Aufgaben kurz
nummeriert anzeigen → jede EINZELN & gruendlich erledigen und nach JEDER Teilaufgabe sofort
committen+pushen (nie Sammel-Commit) → Build/Install nur bei Apps EINMAL ganz am Ende → zum Schluss
pruefen, dass WIRKLICH jede Aufgabe erledigt ist (auch die in der Mitte). Voller Ablauf im Gehirn.

### 3. Git-Disziplin (parallele Sessions am selben Repo — KRITISCH)
Frank arbeitet oft mit mehreren CLIs/Sessions am selben Repo. Darum: nur EIGENE Dateien namentlich
stagen (NIEMALS `git add -A` / `git add .`). Vor JEDEM Push `git fetch origin && git rebase
origin/main`. NIE force-pushen, NIE `git reset --hard` ohne Freigabe. Fremde unstaged Dateien
anderer Sessions in Ruhe lassen — nicht mitcommitten, nicht aufraeumen.

### 4. Secrets: NIEMALS ins Repo
Keine API-Keys, Tokens, Passwoerter ins Repo — auch nicht in Code-Kommentaren, Doku oder Tests.
Secrets leben ausserhalb des Repos in `~/SK/<projekt>/`. Im Repo nur redaktierte Templates.

### 5. Nach Komprimierung / Zusammenfassung (Compact / Compress)
Diese Kern-Regeln gelten danach unveraendert weiter. Die Zusammenfassung selbst MUSS komplett auf
Deutsch mit echten Umlauten verfasst sein — eine englische Zusammenfassung laesst das Modell ins
Englische kippen. Offene, noch nicht erledigte ` ; `-Aufgaben duerfen durch die Komprimierung nicht
verloren gehen.

### 6. Bei JEDEM Bug/Fehler: Direktive #3 — Resilient Bugfixing (KRITISCH)
Bei jedem Fehler/Bug — ODER wenn Frank sinngemaess "Direktive 3" / "nach Direktive 3" sagt — den
Skill `resilient-bugfixing` laden (`skill({ name: "resilient-bugfixing" })`). Der laedt den
vollstaendigen Originaltext on-demand (token-sparend). Kernprinzip: **Root Cause** finden (nicht
Symptom zukleistern), **Funktionalitaet ERHALTEN** (ein Fix darf nie Funktionen entfernen,
auskommentieren oder mit leerem `try/catch` verschlucken — reparieren, nicht wegnehmen),
**dokumentieren**. NIEMALS eine verkuerzte Direktive als "vollstaendig" / "woertlich" ausgeben.

> Nach einem **bestaetigt funktionierenden** Bugfix zusaetzlich: den Fall ueber den
> `second-brain`-MCP (`remember`) ins Gehirn schreiben — Titel `Bugfix <App> <Bereich>
> <YYYY-MM-DD HH:MM>`, Kategorie `bugfixes/<unterkategorie>`. Nur bestaetigte Fixes. Details im
> Gehirn (Kategorie `Programmierung/Rules`).
