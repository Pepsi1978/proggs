# Globale OpenCode-Regeln (gelten in JEDER Session, JEDEM Projekt)

> Laedt bei jedem OpenCode-Start (`~/.config/opencode/AGENTS.md`), unabhaengig vom
> Arbeitsverzeichnis. Diese Datei enthaelt KOMPAKTE Kern-Regeln, die IMMER gelten — auch ohne
> Gehirn-Abruf und nach jeder Komprimierung. Der VOLLE Regeltext liegt im zweiten Gehirn (s.u.)
> und wird beim Start geladen. Kurz halten (< ~150 Zeilen): OpenCode befolgt kurze AGENTS.md
> deutlich zuverlaessiger (best-practices/opencode/agents-md-memory.md).

---

## ⚡ ZUERST beim Start: Arbeitsregeln aus dem zweiten Gehirn laden — per NUMMER (PFLICHT)

**ERSTE Handlung jeder Session, VOR jeder Arbeit. So einfach, dass es JEDES Modell schafft —
einfach von 1 bis zur Gesamtzahl durchzaehlen, KEINEN Titel tippen:**

1. Rufe das `second-brain`-Werkzeug **`get_category_item('Programmierung/Rules', 1)`** auf.
   Die Antwort zeigt **"Eintrag 1 von N"** (N = Gesamtzahl der Regeln) und am Ende den naechsten Aufruf.
2. Rufe es dann mit **2**, **3**, **4** … auf — `get_category_item('Programmierung/Rules', 2)`,
   `get_category_item('Programmierung/Rules', 3)` … **bis du bei N angekommen bist.** Die Antwort sagt
   dir jedes Mal selbst, welche Nummer als naechstes kommt und wie viele noch offen sind.
3. Jeder Aufruf liefert GENAU EINE Regel im vollen Wortlaut (klein, wird nie abgeschnitten). Lies sie.
4. Bestaetige in EINER Zeile: **"N Regeln aus dem zweiten Gehirn eingelesen."** (N = die Gesamtzahl
   aus Schritt 1 — und nur so viele, wie du wirklich abgerufen hast).

> **Warum per Nummer statt per Titel?** Du musst keinen Titel und keine Kategorie-Liste mehr richtig
> abtippen — eine Zahl (1, 2, 3 …) kann kein Modell falsch raten. Das ist der zuverlaessigste Weg,
> ausnahmslos jede Regel wirklich zu lesen. (Frueher: `list_memories` + `get_by_title` mit exaktem
> Titel — schwache Modelle scheiterten am Titel-Tippen.)

> ❌ **NIEMALS `get_by_category('Programmierung/Rules')`** zum Laden nutzen: das gibt alle Regeln
> auf einmal zurueck (~125 KB / ~32k Token) und wird vom System **abgeschnitten (truncated) bzw.
> abgelehnt** — die Regeln waeren dann NICHT gelesen, obwohl der Aufruf "erfolgreich" aussieht.
> **Immer `get_category_item` per Nummer** — das ist der einzige verlaessliche Weg.

**Diese beiden Punkte sind PFLICHT, nicht optional:**
- **(P1) Der Start-Abruf ist eine Muss-Aufgabe** — die ALLERERSTE Handlung jeder Session, bevor auf
  irgendetwas anderes reagiert wird. Kein "vielleicht schaue ich rein": die Regeln werden wirklich
  geladen (Nummer 1 bis N), sonst gilt die Session als nicht arbeitsbereit.
- **(P2) MUSS-Regel fuer JEDEN Second-Brain-Abruf (nicht nur die Regeln):** Daten vom Second-Brain
  werden **immer EINZELN** geholt — bei einer ganzen Kategorie per `get_category_item` Nummer fuer
  Nummer, bei einem bekannten einzelnen Eintrag per `get_by_title` — und **niemals als ganze grosse
  Kategorie (`get_by_category`) zusammengeschmissen**. So kann nichts truncaten und jeder Eintrag wird
  wirklich gelesen.

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

### 7. Anti-Halluzination: nichts erfinden (KRITISCH — gerade fuer guenstige Modelle)
Gerade schwache/guenstige Modelle (GLM-Flash/Air, DeepSeek V4 Pro u.a.) erfinden bei Nichtwissen.
Darum gelten diese 5 Regeln IMMER:
1. **"Ich weiss es nicht"** statt raten — bei Unsicherheit zugeben, nicht erfinden.
2. **Tool-first statt Memory-first** — bevor du ueber Datei/Funktion/API/Config/Projektzustand redest,
   ZUERST mit Tool pruefen (`read`/`grep`/`list`). Die Datei hat recht, dein Gedaechtnis oft nicht.
3. **Kein Ketten-Raten** — nach EINER unverifizierten Vermutung stoppen, nichts darauf aufbauen.
4. **Sofort zurueckziehen** — merkst du, dass du falsch liegst, brich ab statt selbstsicher-falsch weiterzureden.
5. **Quelle nennen** — sag aus welcher Datei/Zeile/Tool-Ausgabe ein Fakt stammt. Kein Beleg = keine
   Behauptung. Erfinde NIE Funktionen, Imports, Paketnamen, Config-Keys oder API-Methoden — verifiziere sie.

> Durchsetzung im Code (nicht nur Bitte): das lokale Plugin `tool-first-guard` warnt bei `edit`/`patch`
> ohne vorheriges `read` (mit `OPENCODE_TOOL_FIRST_ENFORCE=1` blockt es hart). Volltext + Belege:
> `best-practices/agents/anti-halluzination-regeln.md`.
