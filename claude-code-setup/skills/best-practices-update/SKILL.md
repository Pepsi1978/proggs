---
name: best-practices-update
description: >
  Aktualisiert BESTEHENDE Best-Practices unter ~/proggs/best-practices/ als BATCH/WELLE auf den
  neuesten Stand — Harness-Themen (claude-tooling/<thema>.md gegen den Claude-Code-Changelog) UND
  Projekt-Code (best-practices/<kategorie>/<software>.md gegen den eigenen Software-Changelog).
  Geht viele Dateien nacheinander durch, gleicht jede gegen die passende offizielle Quelle ab,
  hebt den Stand-Header und koppelt gefundene Bugs in die Almanache (bugs/) zurueck. Nutze diesen
  Skill IMMER wenn der Benutzer sagt: "Best-Practices aktualisieren", "Best-Practices-Update",
  "Best-Practices-Welle", "hebe die Best-Practices auf die aktuelle Version", "aktualisiere mein
  Harness-Wissen als Welle", "sind meine Best-Practices noch aktuell", "Best-Practices-Refresh",
  "nur die Kotlin-Best-Practices aktualisieren", "alle Best-Practices neu recherchieren". Auch bei
  "die Best-Practices sind veraltet" oder "Stand-Verfall der Best-Practices beheben". Optional auf
  eine Kategorie/Datei fokussierbar ("nur claude-tooling", "nur android"). Schwester-Skill zum
  almanach-update (zwei Seiten der Medaille). Primaer fuer regelmaessige Wellen in Claude Cowork
  gedacht, laeuft aber auch im CLI.
invocation: user
---

<!-- delegation-research-skill -->
> **Web-Recherche laeuft ueber den zentralen `research`-Skill (Delegation, seit 2026-06-21).**
> Nach Frage 1 (Policy `research-strategy.md`: Empfehlung + A/B/C/D) die Recherche NICHT selbst
> orchestrieren — den `research`-Skill laden und ihm diesen Research-Auftrag uebergeben (verlustfreie
> Bruecke; ALLE Felder ausfuellen, nichts erzaehlen):
> - **zweck:** best_practice · **rueckgabe_schema:** `best_practice` · **zerlegungs_modus:** `feste_liste`
> - **unterthemen[]:** pro BP-Datei: was ist seit der letzten Version neu/veraltet (je 2-3 Saetze praezise — werden 1:1 an die Researcher gereicht)
> - **version_anker:** PFLICHT — LIVE-Version + Stand je Datei
> - **engine:** A→C · **anzahl/wellen/cap:** 7, Continuous-Spawning, KEIN Eintrags-Cap, Checkpoint je Datei
> - **persistenz_ziel:** `dieselben best-practices/-Dateien (Kurzcheck+Volltext)` · **dup_quelle:** bestehender BP-Stand je Datei
> - **nacharbeit_aufrufer:** Bugs in bugs/ zurueckkoppeln
> Der research-Skill uebernimmt sichtbare beschriftete Researcher + Continuous-Spawning + Zwischenfazit
> pro Researcher + ruhige Auswertung und gibt das Ergebnis im `best_practice`-Schema zurueck; damit
> hier weiterarbeiten. (Die A/B/C-Engine-Details unten bleiben als Referenz, werden aber vom research-Skill ausgefuehrt.)


# Best-Practices-Update — Best-Practices als Welle auf den neuesten Stand heben

## Zweck

Die positive Seite der Medaille (`~/proggs/best-practices/`) veraltet genauso wie die Almanache.
Dieser Skill geht die BESTEHENDEN Best-Practices-Dateien systematisch durch und hebt jede per
Re-Recherche auf die aktuelle Version — als wiederholbare **Welle**. Er ist die Batch-Version des
Skills `best-practices`: pro Datei derselbe Ablauf, aber ueber viele Dateien mit Auswahl-Logik
und Wellen-Disziplin.

Struktur seit 2026-06-16 **flach 1:1 wie `bugs/`** (siehe `best-practices/SYSTEM.md`):
- **Harness:** `best-practices/claude-tooling/<thema>.md` (`hooks.md`, `skills.md`, `mcp.md`,
  `settings.md`, … `neues.md`). Quelle = **Claude-Code-Changelog** (via `update-changelog`-Script
  des `best-practices`-Skills). Versions-Anker = installierte Claude-Code-Version.
- **Projekt-Code:** `best-practices/<kategorie>/<software>.md` (`android/kotlin.md`,
  `desktop/swift-appkit.md`, …). Quelle = **eigener Changelog** der Software. Versions-Anker =
  LIVE ermittelte installierte Version.

## Schritt 0 — Auswahl: welche Dateien sind dran?

| Modus | Auswahl |
|-------|---------|
| **Default (Stand-Verfall)** | Aelteste nach Stand-Datum zuerst; Hochrisiko bei jedem Versionssprung. |
| **Harness vs. Projekt-Code** | "nur claude-tooling" (Harness gegen Claude-Code-Changelog) ODER "nur android" (Projekt-Code). |
| **Datei/Liste** | "nur kotlin + gradle". |
| **Alle** | komplett (gross — vorher Umfang melden). |

Stand-Daten: `grep -rl '\*\*Stand:\*\*' best-practices/` + Datum je Datei; Liste nach Prioritaet
sortieren und als TaskCreate-Liste sichtbar machen.

## Schritt 1 — Pro Datei: der Ablauf aus dem `best-practices`-Skill

> **Recherche-Weg (Regel `research-strategy.md`):** Vor JEDER Web-Recherche zuerst eine kurze Empfehlung
> geben und per `AskUserQuestion` Frage 1 (A/B/C/D) stellen. **A/B** laufen ueber `mm-research.py`/`or-research.py`
> (Firecrawl bei A max 2 parallel); der hier beschriebene **7-Opus-Researcher-Schwarm ist Option C** (nur auf
> explizite Wahl). Bei A nach Abschluss Frage 2 (Eskalation?).

Fuer JEDE Datei der Arbeitsliste den Ablauf des Skills **`best-practices`** anwenden (diesen Skill
als Referenz lesen, NICHT duplizieren). Mechanik-Unterschied beachten:

- **Harness (`claude-tooling/<thema>.md`):** Changelog-Quelle = Claude-Code-Changelog (das
  `update-changelog.ps1`/`.sh`-Script des `best-practices`-Skills holt ihn verbatim). Delta gegen
  `_state.json.last_version` bestimmen, nur neue Versionen aufrollen. Versions-Anker = Claude-Code-Version.
- **Projekt-Code (`<kategorie>/<software>.md`):** Changelog-Quelle = eigener Software-Changelog
  (Kotlin-Releases bei JetBrains, Swift bei Apple, Gradle-Releases …). Versions-Anker = LIVE
  ermittelte installierte Version (`kotlinc -version`, `swift --version`, `./gradlew --version`, …).

Pro Datei:
1. **Version(en) LIVE ermitteln** (Harness: `claude --version`; Projekt-Code: die jeweilige Software).
2. **7 Researcher GLEICHZEITIG** (Continuous-Spawning, **NIE Workflow**, max 7). Den BESTEHENDEN Stand
   mitgeben → nur NEUES seit Stand X. Max 15 Fetches / 10 Min je Researcher, kein Eintrags-Cap.
3. **Fix-Status / Versions-Claims HART per `gh`** gegenpruefen (Hauptagent) — Researcher-Web-Snippets
   sind bei Versions-/Status-Angaben unzuverlaessig.
4. **Bug-Rueckkopplung (PFLICHT):** foerdert die Recherche einen echten BUG/eine Falle zutage, gehoert er
   in den passenden Almanach `bugs/<kategorie>/<bereich>.md` ZURUECK (existiert kein Almanach → dem Benutzer
   melden + `bug-almanach-recherche` vorschlagen, NICHT im Vorbeigehen einen halben anlegen). Bezugs-Tabellen
   (🔗) beidseitig synchron halten.
5. **Kuratieren + Stand heben:** Eintraege mit Quelle + Datum + `offiziell`/`extern`-Flag, **Kurzcheck-Tabelle
   UND Volltext** pflegen, Stand-Header + Anker aktualisieren. Header-Format erzwingen
   (`# <Thema/Software> — Best Practices (Stand JJJJ-MM-TT, Version V)`).
6. **Self-Test + Commit:** `python bugs/health.py` (alle Checks gruen) → committen+pushen pro Datei
   (eigene Pfade namentlich). Bei Harness-Laeufen `_state.json` + `_changelog-archiv.md` ueber das
   `update-changelog`-Script pflegen.

## Schritt 2 — Wellen-Disziplin & Auswertung

- **Eine Datei = ein Rettungspunkt** (committen+pushen vor der naechsten); TaskCreate-Fortschritt sichtbar.
- **Researcher-Strom konstant 7** (Continuous-Spawning, 429-Backoff, Continuation bei Crash).
- **Auswertung am Ende** wie im `best-practices`-Skill (Teil A je Kategorie, Teil B Kurz-Header, Teil C
  umsetzbare Vorschlaege mit Umgebungs-Gegencheck) — nur fuer die Dateien der Welle.

## Cowork (wenn der Skill in Claude Cowork laeuft)

Identisch zum `almanach-update`-Skill: Git NIE nackt → IMMER `bash ~/proggs/cowork-git.sh push-files …`
(zuerst `setup`, "Push-Zugang OK" abwarten); ~45s pro Shell-Aufruf, Arbeit in Haeppchen; nach dem Schreiben
Dateiende pruefen (`tail -1`/`wc -l`) ODER git-intern bauen; Datenverlust-Waechter nutzen. Volltext:
`~/.claude/rules/cowork-git-push.md`.

## Abgrenzung

- **Bugs/Almanache aktualisieren** (statt Best-Practices) → Schwester-Skill `almanach-update`.
- **Einzelne Ad-hoc-Best-Practices-Recherche** → `best-practices`-Skill direkt.
- **Neue Best-Practices-Datei fuer einen Bereich ohne Pendant** → entsteht beim normalen `best-practices`-Lauf.
