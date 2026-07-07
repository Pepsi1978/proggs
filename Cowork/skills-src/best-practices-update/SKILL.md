---
name: best-practices-update
description: "Hebt bestehende Best-Practices als Welle auf den neuesten Stand (Harness + Projekt-Code gegen die Changelogs) und koppelt Bugs zurueck. Trigger: Best-Practices aktualisieren, Best-Practices-Welle."
---

# Best-Practices-Update (Cowork-Fassung) — Best-Practices als Welle aktuell halten

Diese Cowork-Fassung geht die BESTEHENDEN Best-Practices-Dateien im Arbeitsordner systematisch durch
und hebt jede per Re-Recherche auf die aktuelle Version — als wiederholbare **Welle**. Sie ist die
Batch-/Wellen-Version des Skills `best-practices` (pro Datei derselbe Ablauf, aber über viele Dateien
mit Auswahl-Logik und Wellen-Disziplin) und der Schwester-Skill zu `almanach-update` (zwei Seiten der
Medaille). Sie läuft in der **Claude-Cowork-Desktop-App** und ändert nie ein Werkzeug — sie
recherchiert, speichert und schlägt vor.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Alle Ergebnisse werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert** (üblicherweise der
gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad. Struktur seit
2026-06-16 **flach 1:1 wie `bugs/`** (siehe `best-practices/SYSTEM.md`), relativ zum Arbeitsordner:

```
best-practices/
├── README.md              ← Inhaltsverzeichnis
├── _state.json            ← Harness: {"last_version": …, "last_checked": …}
├── _changelog-archiv.md   ← VERBATIM Claude-Code-Changelog (Harness-Quelle)
├── claude-tooling/<thema>.md           ← Harness (hooks, skills, mcp, settings, … neues)
└── <kategorie>/<software>.md            ← Projekt-Software (android/kotlin.md, desktop/swift-appkit.md, …)
```

KEINE alten Muster: kein `01-hooks…12-neues`, keine `projekt-code/`-Ebene, kein `best-practices-`-Präfix.

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt die Struktur oder ein Kategorie-Ordner → ERST anlegen
(Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfügbar), DANN schreiben. NIEMALS abbrechen, weil ein
Ordner fehlt. Nennt der Benutzer einen anderen Basis-Ordner, dort hinein (gleiche Struktur).

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Cowork-Mount-Brücke kann das **Dateiende abschneiden**. Nach JEDEM Schreiben
  das Dateiende prüfen (`tail -1`, `wc -l`) ODER git-intern bauen. Besonders kritisch für das große
  `_changelog-archiv.md` — lieber das Script (siehe `best-practices`-Skill) nutzen statt von Hand zu schreiben.
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf läuft max ~45 Sekunden; Hintergrundprozesse überleben den
  Wechsel zwischen Aufrufen NICHT. Researcher laufen als **Agenten** (unkritisch); jeder Git-/Script-Schritt
  muss in EINEM Aufruf durchlaufen.
- **Git NIEMALS nackt:** Aus Cowork IMMER über `bash ~/proggs/cowork-git.sh` committen/pushen (fängt
  Mount-Fallen + Datenverlust-Wächter ab). NIE direktes `git commit`/`git push`.

---

## Zwei Bereiche, ein Ordner

- **Harness:** `best-practices/claude-tooling/<thema>.md` (`hooks.md`, `skills.md`, `mcp.md`, `settings.md`,
  … `neues.md`). Quelle = **Claude-Code-Changelog** (via `update-changelog`-Script des `best-practices`-Skills,
  Cowork-DataDir-Default relativ). Versions-Anker = installierte Claude-Code-Version.
- **Projekt-Code:** `best-practices/<kategorie>/<software>.md` (`android/kotlin.md`, `desktop/swift-appkit.md`,
  …). Quelle = **eigener Changelog** der Software (Kotlin bei JetBrains, Swift bei Apple, Gradle …). Versions-Anker
  = LIVE ermittelte installierte Version.

## Schritt 0 — Auswahl: welche Dateien sind dran?

| Modus | Auswahl |
|-------|---------|
| **Default (Stand-Verfall)** | Älteste nach Stand-Datum zuerst; Hochrisiko bei jedem Versionssprung. |
| **Harness vs. Projekt-Code** | "nur claude-tooling" (Harness gegen Claude-Code-Changelog) ODER "nur android" (Projekt-Code). |
| **Datei/Liste** | "nur kotlin + gradle". |
| **Alle** | komplett (groß — vorher Umfang melden). |

Stand-Daten: `grep -rl '\*\*Stand:\*\*' best-practices/` + Datum je Datei; Liste nach Priorität sortieren und
als TaskCreate-Liste sichtbar machen.

## Schritt 1 — Pro Datei: der Ablauf aus dem `best-practices`-Skill

Für JEDE Datei der Arbeitsliste den Ablauf des Skills **`best-practices`** anwenden (diesen Skill als Referenz
lesen, NICHT duplizieren). Mechanik-Unterschied:

- **Harness (`claude-tooling/<thema>.md`):** Changelog-Quelle = Claude-Code-Changelog (das
  `update-changelog`-Script des `best-practices`-Skills holt ihn verbatim). Delta gegen `_state.json.last_version`
  bestimmen, nur neue Versionen aufrollen. Versions-Anker = Claude-Code-Version.
- **Projekt-Code (`<kategorie>/<software>.md`):** Changelog-Quelle = eigener Software-Changelog. Versions-Anker
  = LIVE ermittelte installierte Version (`kotlinc -version`, `swift --version`, `./gradlew --version`, …).

Pro Datei:
1. **Version(en) LIVE ermitteln** (Harness: `claude --version`; Projekt-Code: die jeweilige Software).
2. **7 Researcher GLEICHZEITIG** (Continuous-Spawning, **NIE Workflow**, max 7; Agent-Typ `researcher`, höchstes
   Opus 1M, `opts.model` NICHT setzen — als Agent vom ~45s-Cowork-Limit unberührt). Den BESTEHENDEN Stand
   mitgeben → nur NEUES seit Stand X. Max ~15 Fetches / ~10 Min je Researcher, **kein Findings-Cap** (alle Funde
   dokumentieren — Kappen wäre lossy; sehr viele → verlustfrei in Datei + kompakte Summary). **429-Backoff
   (PFLICHT):** bei Rate-Limit sofort melden + exponential backoff (`retry-after`), nie still aufgeben.
3. **Fix-Status / Versions-Claims HART per `gh`** gegenprüfen (Hauptagent) — Researcher-Web-Snippets sind bei
   Versions-/Status-Angaben unzuverlässig.
4. **Bug-Rückkopplung (PFLICHT):** fördert die Recherche einen echten BUG/eine Falle zutage, gehört er in den
   passenden Almanach `bugs/<kategorie>/<bereich>.md` ZURÜCK (Titel/Symptom/Ursache/Versionen/FIX/Quelle,
   dedupliziert). Existiert KEIN Almanach → dem Benutzer melden + `bug-almanach-recherche` vorschlagen (NICHT im
   Vorbeigehen einen halben anlegen). Bezugs-Tabellen (🔗) beidseitig synchron halten.
5. **Kuratieren + Stand heben:** Einträge mit Quelle + Datum + `offiziell`/`extern`-Flag (Offizielles überstimmt
   Externes immer), **Kurzcheck-Tabelle UND Volltext** pflegen, Stand-Header + Versions-Anker aktualisieren.
   Header-Format erzwingen (`# <Thema/Software> — Best Practices (Stand JJJJ-MM-TT, Version V)`); ein
   software-gebundener Almanach trägt zusätzlich `> **Anker:** <label>=<version>`.
6. **Self-Test + Commit:** `python bugs/health.py` (alle fünf Checks grün — coupling, guard-coverage,
   version-anchor, dead-paths, Stand-Verfall) → committen+pushen pro Datei (eigene Pfade namentlich). Bei
   Harness-Läufen `_state.json` + `_changelog-archiv.md` über das `update-changelog`-Script pflegen, danach das
   Dateiende prüfen. Kein Python in Cowork → Bezugs-Tabellen + Header manuell gegenprüfen, ehrlich vermerken.

## Schritt 2 — Wellen-Disziplin & Auswertung

- **Eine Datei = ein Rettungspunkt** (committen+pushen vor der nächsten); TaskCreate-Fortschritt sichtbar.
- **Researcher-Strom konstant 7** (Continuous-Spawning: wird einer fertig → sofort den nächsten starten;
  429-Backoff; Continuation bei Crash). Empirisch: 5 sicher, 7 läuft, 12 → Absturz.
- **Auswertung am Ende** wie im `best-practices`-Skill, nur für die Dateien der Welle:
  Teil A (je Kategorie: Neuerungen + Quelle · Best Practices · Betrifft eigene Werkzeuge?),
  Teil B (Kurz-Header `## Best-Practices-Lauf — [Datum] | Version [alt]→[neu] | Kategorien: N | Quellen: N`),
  Teil C (umsetzbare Vorschläge — nummeriert, research-gestützt mit belegtem Vorteil + Quelle, je
  `Aktion · Vorteil (belegt) · Betrifft/Aufwand/Risiko`; **Umgebungs-Gegencheck VOR dem Listen:** existiert die
  Datei? nutzt das Werkzeug das Feature? echter Nutzen genau hier? — nur was alle drei besteht; lieber 3 starke
  als 10 schwache). Danach auf die Auswahl warten und NUR Bestätigtes umsetzen.

## Abgrenzung

- **Bugs/Almanache aktualisieren** (statt Best-Practices) → Schwester-Skill `almanach-update`.
- **Einzelne Ad-hoc-Best-Practices-Recherche** → `best-practices`-Skill direkt.
- **Neue Best-Practices-Datei für einen Bereich ohne Pendant** → entsteht beim normalen `best-practices`-Lauf.

## Sichern (Cowork-Git)

Git-Repo verbunden → committen + pushen über das Cowork-Skript (nur die eigenen Pfade namentlich, pro Datei):
```bash
bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push-files "#NNN - best-practices-update <bereich>: Welle auf aktuellen Stand" \
  best-practices/<kategorie>/<software>.md best-practices/_state.json best-practices/_changelog-archiv.md
```
Kein Git-Repo verbunden → nur speichern und dem Benutzer den Ablage-Pfad nennen.

## Was NIEMALS passieren darf

- Aus Cowork mit nacktem `git commit`/`git push` arbeiten (immer `cowork-git.sh`).
- Nach dem Schreiben das Dateiende NICHT prüfen (Mount-Truncation) — besonders bei `_changelog-archiv.md`.
- Mehr als 7 Researcher gleichzeitig (RPM-Absturz) oder als Workflow statt als Agenten starten.
- Echte Funde an einem künstlichen Findings-Cap abschneiden (alle dokumentieren — bei Menge lossless auslagern).
- `_changelog-archiv.md` per WebFetch/Researcher zusammenfassen lassen (zerstört die Vollständigkeit) — Script nutzen.
- Eine externe Behauptung als offiziell darstellen; Vorschläge ohne belegten Vorteil listen (Teil C).
- Alte Best-Practices-Pfadmuster erzeugen (`01-hooks…`, `projekt-code/`, `best-practices-<x>.md`-Präfix).
- Bei Projekt-Code die Bug-Almanach-Rückkopplung oder die Bezugs-Tabellen vergessen.
- Ein eigenes Hook/Skill/Agent/MCP/Setting ändern (dieser Skill schreibt nur Wissensdateien + Almanach-Rückkopplung).

## Referenzen

- Skill `best-practices` — der Pro-Datei-Ablauf + `scripts/update-changelog` (Cowork-DataDir-Default relativ).
- Schwester-Skill `almanach-update` — dieselbe Wellen-Logik für die Bug-Almanache.
- `best-practices/SYSTEM.md`, `bugs/SYSTEM.md`, `bugs/health.py` — Struktur + Self-Test im Arbeitsordner.
- Cowork-Regeln: `bugs/claude-tooling/cowork.md` (§6 Skills), `bugs/claude-tooling/cowork-git-push.md`.
