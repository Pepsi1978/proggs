---
name: almanach-update
description: "Hebt BESTEHENDE Bug-Almanache als Batch/Welle per Re-Recherche auf die aktuelle Software-Version (7-Schritte-Ablauf, kein neuer Almanach). Trigger: Almanache aktualisieren, Almanach-Update, Welle."
---

# Almanach-Update (Cowork-Fassung) — Bug-Almanache als Welle auf den neuesten Stand heben

Diese Cowork-Fassung geht die BESTEHENDEN Bug-Almanache (`bugs/<kategorie>/<bereich>.md` im
Arbeitsordner) systematisch durch und hebt jeden per Re-Recherche auf die aktuelle Software-Version
— als wiederholbare **Welle**, nicht als Einzel-Recherche. Sie ist die Batch-Version von
`bug-almanach-recherche`: pro Almanach derselbe erprobte 7-Schritte-Ablauf, aber über viele Almanache
mit Auswahl-Logik und Wellen-Disziplin. **Sie legt NIE einen neuen Almanach an** (das macht
`bug-almanach-recherche` mit Franks OK) — sie aktualisiert nur Bestehendes. Läuft in der
**Claude-Cowork-Desktop-App** (bessere Limits, schont CLI-Token).

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Alle Almanache liegen RELATIV im aktuell verbundenen Arbeitsordner** (üblicherweise der gemountete
`proggs`-Ordner) — NICHT in einem fest verdrahteten `~/proggs`-Pfad. Struktur relativ zum Arbeitsordner:

```
bugs/
├── README.md                         ← Index (Stand-Datum + Bug-Anzahl je Almanach)
├── SYSTEM.md                         ← Almanach-System (Format, Anker §7, Hook-Mapping)
├── health.py                         ← Self-Test (coupling, guard-coverage, version-anchor, dead-paths, Stand-Verfall)
├── check-version-anchor.py           ← ANCHORS bei Live-Software
└── <kategorie>/<bereich>.md          ← die zu aktualisierenden Almanache (android, web, claude-tooling, …)

best-practices/<kategorie>/<software>.md   ← Rückkopplungs-Ziel (flach 1:1 wie bugs/, seit 2026-06-16)
```

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt eine Best-Practices-Datei zum Zurückspeisen → ERST
anlegen (Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfügbar), DANN schreiben. NIEMALS abbrechen,
weil ein Ordner/eine Datei fehlt. Nennt der Benutzer einen anderen Basis-Ordner, dort hinein.

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Cowork-Mount-Brücke kann das **Dateiende abschneiden**. Nach JEDEM
  Schreiben das Dateiende prüfen (`tail -1`, `wc -l`) ODER git-intern bauen. Besonders kritisch bei
  langen Almanachen — den Datenverlust-Wächter im `cowork-git.sh` nutzen.
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf läuft max ~45 Sekunden; Hintergrundprozesse überleben
  den Wechsel zwischen Aufrufen NICHT. Die Arbeit pro Almanach in tragfähige Häppchen pro Aufruf
  schneiden; jeder Git-/Script-Schritt muss in EINEM Aufruf durchlaufen. Researcher laufen als
  **Agenten** (unkritisch, vom ~45s-Limit unberührt).
- **Git NIEMALS nackt:** Aus Cowork IMMER über `bash ~/proggs/cowork-git.sh` committen/pushen (fängt
  Mount-Fallen + Datenverlust-Wächter ab). NIE direktes `git commit`/`git push`.

---

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
   dead-paths, Stand-Verfall) —, DANN committen+pushen pro Almanach (eigene Pfade namentlich, über `cowork-git.sh`).

## Schritt 2 — Wellen-Disziplin

- **Ein Almanach = ein Rettungspunkt:** nach jedem aktualisierten Almanach committen+pushen, bevor der naechste startet.
- **Sichtbarer Fortschritt:** TaskCreate-Liste pro Almanach abhaken; nach jedem ein kurzer Stand ("3/8 fertig").
- **Researcher-Strom konstant 7:** wird einer fertig, SOFORT den naechsten fuers naechste Teilthema/den naechsten
  Almanach starten (kein Wellen-Warten). Nie >7 gleichzeitig (RPM).
- **Continuation bei Crash:** stuerzt ein Researcher ab → 429-Backoff, neu starten; kommt er nicht durch →
  Continuation-Researcher am Checkpoint. Nie still aufgeben.

## Abgrenzung

- **Neuer Almanach noetig** (Bereich hat noch keinen) → NICHT dieser Skill, sondern `bug-almanach-recherche` (mit Franks OK).
- **Best-Practices-Dateien aktualisieren** (statt Bugs) → Schwester-Skill `best-practices-update`.
- **Einzelne Ad-hoc-Recherche** → `bug-almanach-recherche` direkt.

## Offene Almanache aus Welle 3 (Stand der Spec, falls noch nicht nachgeholt)
kotlin · jetpack-compose · gradle · firebase-billing (jeweils Stand 2026-06-02) — beim Default-Lauf
„aelteste zuerst" kommen diese zuerst dran.

## Sichern (Cowork-Git)
Git-Repo verbunden → committen + pushen über das Cowork-Skript (nur die eigenen Pfade namentlich):
```bash
bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push-files "#NNN - almanach-update <bereich>: re-recherchiert auf Stand JJJJ-MM-TT" \
  bugs/<kategorie>/<bereich>.md bugs/README.md best-practices/<kategorie>/<software>.md
```
Kein Git-Repo → nur speichern und dem Benutzer den Ablage-Pfad nennen.

## Was NIEMALS passieren darf
- Einen NEUEN Almanach anlegen (das ist `bug-almanach-recherche` mit Franks OK) — dieser Skill aktualisiert nur Bestehendes.
- Den Fix-Status NUR aus Researcher-Web-Snippets übernehmen, statt ihn HART per `gh` zu verifizieren.
- Die Best-Practices-Rückkopplung oder die Bezugs-Tabellen (🔗) vergessen.
- Kurzcheck-Tabelle ODER Volltext eines Bugs weglassen — IMMER beide pflegen.
- Mehr als 7 Researcher gleichzeitig starten (RPM-Absturz ab ~12); Researcher per Workflow statt Agent-Tool laufen lassen.
- Findings an einem künstlichen Cap abschneiden (lossy) — alle Funde dokumentieren.
- Aus Cowork mit nacktem `git commit`/`git push` arbeiten (immer `cowork-git.sh`).
- Nach dem Schreiben das Dateiende NICHT prüfen (Mount-Truncation übersehen).

## Referenzen
- Skill `bug-almanach-recherche` — der erprobte 7-Schritte-Ablauf pro Almanach (NICHT duplizieren, referenzieren).
- Schwester-Skill `best-practices-update` — aktualisiert Best-Practices statt Bugs.
- `bugs/SYSTEM.md`, `bugs/health.py`, `bugs/README.md` — Almanach-System + Self-Test + Index im Arbeitsordner.
- `best-practices/SYSTEM.md` — flache 1:1-Struktur der Best-Practices (Rückkopplungs-Ziel).
- Cowork-Regeln: `bugs/claude-tooling/cowork.md`, `bugs/claude-tooling/cowork-git-push.md`.
