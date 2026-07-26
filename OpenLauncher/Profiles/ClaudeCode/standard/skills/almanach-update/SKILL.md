---
name: almanach-update
description: >
  Aktualisiert BESTEHENDE Bug-Almanache unter ~/proggs/bugs/<kategorie>/<bereich>.md als
  BATCH/WELLE auf die jeweils aktuelle Software-Version (Re-Recherche, kein neuer Almanach).
  Geht viele Almanache nacheinander durch, hebt jeden mit dem erprobten 7-Schritte-Ablauf
  des bug-almanach-recherche-Skills auf den neuesten Stand und koppelt gefundene Bugs in die
  Best-Practices zurueck. Nutze diesen Skill IMMER wenn der Benutzer sagt: "Almanache
  aktualisieren", "Almanach-Update", "Re-Recherche-Welle", "Welle starten", "hebe die
  Almanache auf die aktuelle Version", "aktualisiere alle Bugs", "aktualisiere die aeltesten
  Almanache", "Almanach-Welle", "nur android-Almanache aktualisieren", "Almanach-Refresh",
  "Bug-Almanache auf neuesten Stand bringen". Auch bei Varianten wie "die alten Almanache sind
  veraltet, recherchier sie neu" oder "Stand-Verfall beheben". Optional fokussierbar auf eine
  Kategorie oder Liste ("nur android", "nur kotlin + gradle"). Lege NIE einen neuen Almanach an
  (dafuer: bug-almanach-recherche) — dieser Skill AKTUALISIERT nur Bestehendes. Primaer fuer
  regelmaessige Wellen in Claude Cowork gedacht (bessere Limits), laeuft aber auch im CLI.
invocation: user
---

<!-- delegation-research-skill -->
> **Web-Recherche laeuft ueber den zentralen `research`-Skill (Delegation, seit 2026-06-21).**
> Nach Frage 1 (Policy `research-strategy.md`: Empfehlung + A/B/C/D) die Recherche NICHT selbst
> orchestrieren — den `research`-Skill laden und ihm diesen Research-Auftrag uebergeben (verlustfreie
> Bruecke; ALLE Felder ausfuellen, nichts erzaehlen):
> - **zweck:** bug · **rueckgabe_schema:** `bug` · **zerlegungs_modus:** `feste_liste`
> - **unterthemen[]:** die 7-Schritte-Aspekte je Almanach-Datei (je 2-3 Saetze praezise — werden 1:1 an die Researcher gereicht)
> - **version_anker:** PFLICHT — LIVE-Version + bestehender Almanach-Stand
> - **engine:** A→C · **anzahl/wellen/cap:** 7, Continuous-Spawning, KEIN Eintrags-Cap
> - **persistenz_ziel:** `die bestehenden bugs/-Almanach-Dateien (Kurzcheck+Volltext)` · **dup_quelle:** bestehender Almanach-Stand
> - **nacharbeit_aufrufer:** gh OPEN/CLOSED-Pruefung + Fix-Status je Bug; veraltete Eintraege markieren
> Der research-Skill uebernimmt sichtbare beschriftete Researcher + Continuous-Spawning + Zwischenfazit
> pro Researcher + ruhige Auswertung und gibt das Ergebnis im `bug`-Schema zurueck; damit
> hier weiterarbeiten. (Die A/B/C-Engine-Details unten bleiben als Referenz, werden aber vom research-Skill ausgefuehrt.)


# Almanach-Update — Bug-Almanache als Welle auf den neuesten Stand heben

## Zweck

Bug-Almanache veralten: Software entwickelt sich weiter, Fixes erscheinen, neue Fallen kommen
dazu. Dieser Skill geht die BESTEHENDEN Almanache (`~/proggs/bugs/<kategorie>/<bereich>.md`)
systematisch durch und hebt jeden per Re-Recherche auf die aktuelle Software-Version — als
wiederholbare **Welle**, nicht als Einzel-Recherche.

Er ist die Batch-Version von `bug-almanach-recherche`: pro Almanach laeuft derselbe erprobte
7-Schritte-Ablauf, aber ueber viele Almanache mit Auswahl-Logik und Wellen-Disziplin. **Er legt
NIE einen neuen Almanach an** (das macht `bug-almanach-recherche` mit Franks OK) — er
aktualisiert nur, was schon existiert.

> **Warum als eigener Skill / fuer Cowork:** Das regelmaessige Re-Recherchieren soll kuenftig in
> **Claude Cowork** laufen (bessere Limits, schont Franks CLI-Token). Die Wellen-Logik ist hier
> gekapselt und Cowork-tauglich (siehe Abschnitt „Cowork").

## Schritt 0 — Auswahl: welche Almanache sind dran?

Zuerst die Arbeitsliste bestimmen (dem Benutzer kurz zeigen, bevor es losgeht):

| Modus | Auswahl |
|-------|---------|
| **Default (Stand-Verfall)** | Die AELTESTEN nach Stand-Datum zuerst (`> **Stand:** … JJJJ-MM-TT` im Kopf), bzw. alle mit Verfall (Richtwert > ~14 Tage; Hochrisiko-Bereiche `r8`/`firebase-billing`/`claude-hooks`/`claude-config` bei JEDEM Software-Versionssprung). |
| **Kategorie** | "nur android" → alle `bugs/android/*.md`. |
| **Liste** | "nur kotlin + gradle" → genau diese. |
| **Alle** | komplette Re-Recherche aller Almanache (gross — vorher Umfang melden). |

Stand-Daten schnell sammeln: `python bugs/health.py` zeigt den Stand-Verfall; ODER
`grep -rl '\*\*Stand:\*\*' bugs/` + Datum je Datei. Die Liste nach Prioritaet sortieren
(aeltester/hoechstes Risiko zuerst) und als TaskCreate-Liste sichtbar machen.

## Schritt 1 — Pro Almanach: der erprobte 7-Schritte-Ablauf

> **Recherche-Weg (Regel `research-strategy.md`):** Vor JEDER Web-Recherche zuerst eine kurze Empfehlung
> geben und per `AskUserQuestion` Frage 1 (A/B/C/D) stellen. **A/B** laufen ueber `mm-research.py`/`or-research.py`
> (Firecrawl bei A max 2 parallel); der hier beschriebene **7-Opus-Researcher-Schwarm ist Option C** (nur auf
> explizite Wahl). Bei A nach Abschluss Frage 2 (Eskalation?).

Fuer JEDEN Almanach der Arbeitsliste den Ablauf aus dem Skill **`bug-almanach-recherche`**
(Schritte 1-7) anwenden — diesen Skill als Referenz lesen, NICHT die Logik hier duplizieren.
Wellen-spezifische Praezisierungen:

1. **Version(en) LIVE ermitteln** (nicht raten): je nach Software
   `claude --version` · `kotlinc -version` · Plugin-Version aus `build.gradle.kts`/`libs.versions.toml` ·
   `./gradlew --version` · AGP+compileSdk · Compose-BOM · Billing-Dep-Version. Mehrere Versionen pro
   Android-Projekt sind normal → ALLE in den Stand-Header.
2. **7 Researcher GLEICHZEITIG** (Continuous-Spawning, **NIE Workflow**, max 7 — ab ~12 RPM-Absturz).
   Teilbereiche: offizielle Doku/Changelog · Issue-Tracker · Community/Praxis · Plattform-Fallen
   (Windows+macOS) · Mechanik · Fix-Status-Changelog · neue Features. Jeder: max 15 Fetches, max 10 Min,
   **KEIN Eintrags-Cap**. **WICHTIG:** den Researchern den BESTEHENDEN Almanach-Stand mitgeben → sie
   suchen gezielt NUR NEUES seit Stand X (sonst doppelte Arbeit; bestehende Almanache sind oft schon umfangreich).
3. **Fix-Status HART per `gh`** (HAUPTAGENT — Researcher haben kein Bash). Issue-Nummern von den
   Researchern sammeln lassen, dann selbst verifizieren:
   `gh issue view <nr> --repo <org>/<repo> --json number,state,title,closedAt,stateReason`
   → `NOT_PLANNED` = won't fix (Workaround bleibt DAUERHAFT), `COMPLETED` = echt gefixt, `DUPLICATE` = gebuendelt.
   **Pflicht-Gegenprobe:** Researcher-Web-Snippets sind bei Versions-/Status-Angaben unzuverlaessig — `gh` ist die Grundwahrheit.
4. **Best-Practices-Abgleich BEIDE Richtungen:** lesen (`grep best-practices/` → bekannte Loesung in den
   FIX aufnehmen) UND schreiben (allgemeingueltige Praevention nach `best-practices/<kategorie>/<software>.md`
   zurueckspeisen). Bezugs-Tabellen (🔗) synchron halten, wenn beide Dateien existieren. (Struktur seit
   2026-06-16 flach 1:1 wie bugs/ — siehe `best-practices/SYSTEM.md`.)
5. **Kuratieren:** deduplizieren, thematisch gruppieren, Format pro Bug (Symptom/Ursache/Versionen/FIX/Quelle),
   **Kurzcheck-Tabelle UND Volltext** pflegen, Fix-Status-Sektion mit gh-Status, Methodik-Hinweis. Stand-Header
   + `> **Anker:** <label>=<version>` (SYSTEM.md §7) aktualisieren.
6. **Ins System einhaengen:** `bugs/README.md` (Stand-Datum + Bug-Anzahl), `check-version-anchor.py`-`ANCHORS`
   bei Live-Software, `bug-almanac-hint.py`-`AREAS` nur bei neuen Stichwoertern, Hook-Mapping nur bei neuem Dateimuster.
7. **Self-Test + Commit:** `python bugs/health.py` — alle Checks gruen (coupling, guard-coverage, version-anchor,
   dead-paths, Stand-Verfall) —, DANN committen+pushen pro Almanach (eigene Pfade namentlich).

## Schritt 2 — Wellen-Disziplin

- **Ein Almanach = ein Rettungspunkt:** nach jedem aktualisierten Almanach committen+pushen, bevor der naechste startet.
- **Sichtbarer Fortschritt:** TaskCreate-Liste pro Almanach abhaken; nach jedem ein kurzer Stand ("3/8 fertig").
- **Researcher-Strom konstant 7:** wird einer fertig, SOFORT den naechsten fuers naechste Teilthema/den naechsten
  Almanach starten (kein Wellen-Warten). Nie >7 gleichzeitig (RPM).
- **Continuation bei Crash:** stuerzt ein Researcher ab → 429-Backoff, neu starten; kommt er nicht durch →
  Continuation-Researcher am Checkpoint. Nie still aufgeben.

## Cowork (wenn der Skill in Claude Cowork laeuft)

Siehe `~/.claude/rules/cowork-git-push.md`. Kernpunkte:
- **Git NIE nackt:** IMMER `bash ~/proggs/cowork-git.sh push-files "#NNN - Text" <datei...>` (gezielt) bzw.
  zuerst `bash ~/proggs/cowork-git.sh setup` ("Push-Zugang OK" abwarten). Das Skript faengt die Mount-Fallen
  (Lock/BOM/LFS/Symlink/Build-Berge) + Datenverlust-Waechter ab.
- **~45s pro Cowork-Shell-Aufruf, Hintergrundprozesse ueberleben den Wechsel NICHT** → die Arbeit pro Almanach
  in tragfaehige Haeppchen pro Aufruf schneiden. (Researcher laufen als Agenten, nicht als Shell-Hintergrund — das ist ok.)
- **Mount-Schreiben kann abgeschnitten sein** → nach dem Schreiben Dateiende pruefen (`tail -1`, `wc -l`) ODER
  git-intern bauen. Den Datenverlust-Waechter im `cowork-git.sh` nutzen.

## Abgrenzung

- **Neuer Almanach noetig** (Bereich hat noch keinen) → NICHT dieser Skill, sondern `bug-almanach-recherche` (mit Franks OK).
- **Best-Practices-Dateien aktualisieren** (statt Bugs) → Schwester-Skill `best-practices-update`.
- **Einzelne Ad-hoc-Recherche** → `bug-almanach-recherche` direkt.

## Offene Almanache aus Welle 3 (Stand der Spec, falls noch nicht nachgeholt)
kotlin · jetpack-compose · gradle · firebase-billing (jeweils Stand 2026-06-02) — beim Default-Lauf
„aelteste zuerst" kommen diese zuerst dran.
