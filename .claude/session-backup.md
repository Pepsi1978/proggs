# Session Handoff — 2026-06-15 (spaet)

## Ziel (1-3 Saetze)
Harness-/Bug-Almanach-/Intelligenz-System verbessern (Welle 3 + Folgeaufgaben). ALLE beauftragten
Aufgaben dieser Session sind ERLEDIGT + committed (#46802-46812). Offen ist NUR der geplante Endpunkt:
die 2 NEUEN Update-Skills (Almanach-Welle + Best-Practices-Welle, Cowork-tauglich) bauen.

## Aktueller Status — ALLES committed auf origin/main (#46802-46812)
- #46802 Aufgabe A: claude-hooks-Almanach + best-practices/01-hooks auf Claude Code v2.1.177 re-recherchiert
  (7 Researcher, Issue-Status HART per gh verifiziert). Restliche 4 Welle-3-Almanache (kotlin/compose/gradle/
  firebase-billing) NICHT manuell gemacht -> umgewidmet zu den Update-Skills.
- #46803 Spec fuer die 2 Update-Skills gesichert: bugs/UPDATE-SKILLS-SPEC.md (komplette Bauanleitung).
- #46804/#46805 Aufgabe B: 6 check-coupling-Luecken geschlossen + Folge-Luecke (guard-coverage cowork-git-push) gefixt.
- #46806 Aufgabe C: BOM-Root-Cause = session-guard.ps1 schrieb settings.json mit [System.Text.Encoding]::UTF8
  (=UTF8Encoding(true)=BOM); alle 4 Stellen + redact-settings-reference.ps1 -> (New-Object System.Text.UTF8Encoding $false).
  Empirisch bewiesen, in claude-hooks-Almanach 12.1 + bug-cases dokumentiert.
- #46807 Aufgabe D: generisches Dedup in whiteboard-insert.{ps1,sh} (timestamp-invarianter Key, KEIN top-level exit, beide getestet).
- #46808 W3-1: check-version-anchor.py (in health.py) + strukturiertes `> **Anker:** <label>=<version>`-Feld in 10 software-Almanachen + SYSTEM.md §7.
- #46809 W3-2: semantischer Prompt-Trigger bug-almanac-hint (UserPromptSubmit, py-Logik + ps1/sh-Wrapper, einmalig/Bereich/Session, passiv). 3-Dateien-Settings + SYSTEM.md-Schicht 1b.
- #46810 W3-3: claude-code-setup/tools/memory-staleness.py (read-only) + Governance-Konvention (meta-memory). KEIN 114-Datei-Umbau.
- #46811 W3-4: Agent-Skills als Digest-Traeger EVALUIERT -> Empfehlung NICHT umbauen (keine Erzwingung, Budget). SYSTEM.md §10.
- #46812 W3-5: bestehende Skills bug-almanach-recherche (Schritt 6.3-6.5) + best-practices (Schritt 5) + known-bugs-Regel (Schicht 1b) + UPDATE-SKILLS-SPEC an das neue W3-System angepasst (Frank's Hinweis!).

## Relevante Dateien
- `bugs/UPDATE-SKILLS-SPEC.md` — DIE Bauanleitung fuer die 2 offenen Skills (inkl. W3-Mechanismen + gh-Pflicht).
- `bugs/health.py` — 4 Checks (coupling, guard-coverage, version-anchor, Stand-Verfall). Fuer MEINE Arbeit gruen.
- `bugs/check-version-anchor.py`, `~/.claude/hooks/bug-almanac-hint.{py,ps1,sh}`, `claude-code-setup/tools/memory-staleness.py`.

## Getroffene Entscheidungen
- Welle-3-Re-Recherche wird kuenftig per Skill in Claude COWORK gemacht (bessere Limits), NICHT manuell im CLI.
- W3-3 schlank (Staleness-Tool) statt 114-Datei-Migration (Frank-Wahl). W3-4 reine Eval.

## Fehlgeschlagene Ansaetze / WICHTIG (nicht wiederholen)
- python-Befehle via Bash mit `$HOME`/`/c/Users/...`-Pfad -> FileNotFoundError. IMMER os.path.expanduser ODER C:/Users/... .
- settings.json NICHT per Edit-Tool (triggert Formatter/BOM) und NICHT per json.dump (reformatiert TAB->Spaces).
  Stattdessen: gezielter Python-STRING-Replace (utf-8-sig lesen, utf-8 ohne BOM schreiben) + json.loads-Validierung.
- PARALLELE CVO-Session arbeitet AKTIV an cowork: hat cowork-scheduled-tasks.md (neuer Almanach) + best-practices-cowork.md
  angefasst -> 2 health-WARNs (cowork-Drift + cowork-scheduled-tasks-Luecke) gehoeren IHR. NICHT anfassen (sie macht
  ihre coupling/guard-Eintraege selbst). README.md/SYSTEM.md wurden von ihr mit-modifiziert.
- claude-config + claude-hooks + python-windows Almanach-VOLLTEXTE in dieser Session schon gelesen (Bereiche frei).

## Naechste Schritte (priorisiert)
1. Die 2 Update-Skills bauen — via skill-creator, Anleitung in bugs/UPDATE-SKILLS-SPEC.md. ZUERST der
   Almanach-Update-Skill (Prioritaet), dann der Best-Practices-Update-Skill. Sehr ausfuehrlich. Danach in
   Cowork-Logik umwandeln (cowork-git.sh push-files, ~45s-Shell-Limit, Mount-Fallen) und an Frank schicken.
2. Optional (Intelligenz-Vorschlag, Frank-Zustimmung offen): MEMORY.md-Groessenwaechter (SessionStart-Check),
   da MEMORY.md mit 25.8 KB ueber dem 24.4-KB-Ladelimit ist (nur teilweise geladen).

## Offene Fragen
- Frank-Antwort offen: Update-Skills in frischer Session (empfohlen) ODER direkt? + MEMORY.md-Waechter ja/nein?

## Anker
- Branch: main
- Letzte Commits:
  #46812 W3-5 align existing skills+rule with new W3 logic
  #46811 W3-4 evaluate Agent Skills as native digest carrier -> NOT convert
  #46810 W3-3 memory-staleness.py + governance convention
  #46809 W3-2 semantic prompt-trigger bug-almanac-hint
  #46808 W3-1 version-anchor check + Anker fields

## NACHTRAG (Frank, 2026-06-15 spaet) — Cowork/Desktop-Skill-Varianten ebenfalls anpassen
Frank nutzt in Claude COWORK (Desktop-App) GEKUERZTE Varianten derselben Skills: bug-almanach-recherche
("Bug einmal nach"), research/researcher, best-practices. Diese gekuerzten Cowork-Versionen muessen
EBENFALLS an das neue W3-Logik-System angepasst werden (Anker-Feld W3-1, check-version-anchor, bug-almanac-hint
W3-2, health.py-Self-Test — soweit in der gekuerzten Form sinnvoll/anwendbar).
ACHTUNG — Speicherort NICHT eindeutig gefunden: `~/proggs/Cowork/` enthaelt nur eine README (keine Skills);
Cowork-Skills werden per ZIP in die Desktop-App hochgeladen, die Quelle der gekuerzten Skills liegt vermutlich
woanders. FRISCHE SESSION: ZUERST Frank nach dem genauen Speicherort/Quellordner der gekuerzten Cowork-Skill-
Varianten fragen, DANN analog zu W3-5 anpassen. Gehoert thematisch zur Update-Skills-/Skill-Konsistenz-Arbeit.

## PRAEZISIERUNG (Frank, 2026-06-15 spaet) — Cowork-Skill-ZIPs in ~/proggs/Cowork/ bauen
Frank-Wunsch konkret: Die gekuerzten Cowork-Varianten als FERTIGE ZIPs in `~/proggs/Cowork/` ablegen, sodass
Frank sie direkt von dort in die Cowork-Desktop-App HOCHLADEN/EINFUEGEN kann (Cowork installiert Skills per ZIP-Upload).
TODO (frische Session, eigener grosser Block):
1. Fuer JEDEN der 3 Skills eine GEKUERZTE Cowork-Variante des `SKILL.md` erstellen (Quelle = die CLI-Skills
   `~/.claude/skills/{bug-almanach-recherche,best-practices}` + ein research/researcher-Skill). Kuerzen, weil Cowork
   engere Limits hat; description einzeilig <=200 Zeichen, `name`-Feld setzen (Cowork-Anforderung, siehe
   bugs/claude-tooling/cowork.md + best-practices-cowork.md §2 Skills/Plugins).
2. Die neuen W3-Mechanismen EINBAUEN (soweit in Cowork sinnvoll): Anker-Feld W3-1, check-version-anchor-Hinweis,
   bug-almanac-hint-Pflege, `python bugs/health.py`-Self-Test; PLUS Cowork-Git-Logik (`bash ~/proggs/cowork-git.sh
   push-files ...` statt nacktem git; ~45s-Shell-Limit; Mount-Fallen — siehe ~/.claude/rules/cowork-git-push.md).
3. Jede Variante als eigenes ZIP packen (Ordnerstruktur `<skillname>/SKILL.md` im ZIP) und in `~/proggs/Cowork/`
   ablegen. README dort aktualisieren (welche ZIPs, wie hochladen).
4. Committen+pushen (Cowork-Ordner-Inhalt gehoert ins Repo). Frank meldet, sobald hochgeladen/getestet.
Hinweis: Diese gekuerzten Cowork-Skills existierten bisher NUR in der Desktop-App (nicht im Dateisystem) — sie
werden also NEU erstellt (basierend auf den CLI-Skills), nicht editiert.

## ZUSATZ-SPEZIFIKATION (Frank, 2026-06-15 spaet) — Cowork-Skills VOLL Cowork-tauglich
Die gekuerzten Cowork-Skill-Varianten muessen DIREKT in der Claude-Cowork-Desktop-App lauffaehig sein —
nicht nur inhaltlich gekuerzt, sondern Cowork-UMGEBUNGS-korrekt:
- **Komplette neue W3-Logik anwenden** (Anker-Feld W3-1, check-version-anchor, bug-almanac-hint W3-2, health.py-Self-Test).
- **Ordner-/Speicher-Bewusstsein:** Der Skill MUSS wissen, WAS er WO speichert (in Cowork laeuft alles ueber den
  gemounteten `~/proggs/`-Ordner: Almanache `~/proggs/bugs/<kategorie>/<bereich>.md`, Best-Practices
  `~/proggs/best-practices/projekt-code/<kategorie>/`, Tools `~/proggs/bugs/*.py`, Hooks-Pflege). Er muss auch
  NEUE Ordner anlegen koennen (neue Kategorie/neuer Bereich) — explizit im Skill-Ablauf beschreiben.
- **Cowork-Schreibfallen beachten** (rules/cowork-git-push.md): Mount-Bruecke kann Edit/Write abschneiden →
  Dateiende pruefen (`tail -1`/`wc -l`) ODER git-intern bauen; Datei besser per `cat >` schreiben; ~45s-Shell-Limit.
- **Abschluss IN Cowork:** Wenn der Skill durchgelaufen ist, MUSS er committen + pushen — ueber
  `bash ~/proggs/cowork-git.sh push-files "#NNN - Text" <datei...>` (gezielt, Mount-schonend; NICHT nacktes git;
  Datenverlust-Waechter greift). Vorher `bash ~/proggs/cowork-git.sh setup` ("Push-Zugang OK" abwarten).
- Ziel bleibt: fertige ZIPs in `~/proggs/Cowork/` (Frank laedt sie per ZIP-Upload in die Desktop-App).

## VORLAGE vorhanden (Frank lieferte 2026-06-15 die EXISTIERENDE Cowork-Fassung von bug-almanach-recherche)
Frank hat die aktuelle Cowork-Fassung des `bug-almanach-recherche`-Skills + dessen `references/researcher-prompts.md`
im Chat gepostet. Sie ist die VORLAGE — die frische Session baut darauf auf (nicht bei Null anfangen). Bekannte
Cowork-DELTAS dieser Fassung ggü. der CLI-Fassung (`~/.claude/skills/bug-almanach-recherche/SKILL.md`):
- **Ablage-Ort-Abschnitt ganz oben (ZUERST LESEN):** Ergebnisse in den aktuellen COWORK-ARBEITSORDNER (verbundener
  Ordner/Projekt), NICHT in festen `~/proggs`-Pfad. Struktur RELATIV: `bugs/<kategorie>/<bereich>.md`,
  `bugs/README.md`, `best-practices/projekt-code/<kategorie>/best-practices-<software>.md`.
- **Ordner-anlegen ist Pflicht + erlaubt:** fehlt ein Ziel-/Zwischenordner → ERST anlegen (mkdir -p bzw. Datei-Werkzeug),
  dann Datei schreiben. NIE abbrechen weil Ordner fehlt. Wenn Frank anderen Basis-Ordner nennt → dorthin (gleiche Unterstruktur).
- **Schritt 3:** "falls eine Shell verfuegbar ist" gh-Pruefung (Cowork-Shell eingeschraenkt); sonst Changelog als Beleg + ehrlich markieren.
- **Schritt 7 "Sichern":** Git-Repo → committen+pushen; kein Repo → nur speichern.
- **Kurzcheck (Stufe A)** als Pflicht-Bestandteil ist schon drin; researcher-prompts.md (Phase A/B) ebenfalls.
WAS NOCH FEHLT (frische Session muss es EINBAUEN): die NEUEN W3-Mechanismen — Anker-Feld (W3-1) + Hinweis auf
check-version-anchor, bug-almanac-hint-Pflege (W3-2), `health.py`-Self-Test — UND die Cowork-Abschluss-Logik
(`bash cowork-git.sh push-files ...` statt nacktem git, ~45s-Limit, Mount-Schreibfallen). Gleiches dann fuer die
best-practices- und research-Cowork-Fassungen. Ergebnis: 3 fertige ZIPs in `~/proggs/Cowork/`.
HINWEIS: Falls Franks Cowork-Skill-Text im neuen Transkript fehlt (nach /clear weg) — Frank bitten, ihn erneut zu
posten, ODER aus der CLI-Fassung + diesen Deltas rekonstruieren (beides moeglich).

## VORLAGE 2 (Frank lieferte 2026-06-15 die EXISTIERENDE Cowork-Fassung von best-practices + Scripts)
Frank postete auch die Cowork-Fassung des `best-practices`-Skills + `scripts/update-changelog.{sh,ps1}`. VORLAGE
fuer die frische Session. Cowork-DELTAS ggü. CLI-Fassung: relativer Ablage-Ort (best-practices/ im Arbeitsordner,
NICHT ~/proggs), Ordner-anlegen-Pflicht, 12-Kategorien-Taxonomie (01-hooks..12-neues) + projekt-code/<kategorie>/,
Changelog-Archiv verbatim, "falls Shell verfuegbar" fuer die Scripts, Bug-Almanach-Rueckkopplung + Bezugstabellen.
WICHTIGES DELTA ZU FIXEN: `update-changelog.{sh,ps1}` haben noch `DataDir = ~/proggs/best-practices` als Default —
fuer die Cowork-Fassung muss der Default der COWORK-ARBEITSORDNER sein (relativ), sonst schreibt das Script am
verbundenen Ordner vorbei. EINZUBAUEN (wie bei bug-almanach-recherche): Anker-Feld W3-1 + check-version-anchor-Hinweis,
bug-almanac-hint-Pflege W3-2, health.py-Self-Test, cowork-git.sh-Abschluss.
=> 3 Cowork-ZIPs nach ~/proggs/Cowork/: bug-almanach-recherche, best-practices, research/researcher (jeweils mit
   diesen Deltas + W3-Mechanismen + Cowork-git). Beide Skill-Vorlagen hat Frank im Chat geliefert (research-Fassung
   ggf. noch erfragen). Falls Texte nach /clear weg: Frank erneut bitten ODER aus CLI-Fassungen + diesen Deltas rekonstruieren.
