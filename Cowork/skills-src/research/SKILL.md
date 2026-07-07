---
name: research
description: "Recherchiert ein Thema mit einem Researcher-Schwarm und persistiert wiederverwendbare Funde in best-practices/ und den Bug-Almanach. Trigger: recherchiere X, Internet-Recherche, tiefe Recherche."
---

# Research (Cowork-Fassung) — gründliche Web-Recherche mit Persistenz

Diese Cowork-Fassung führt eine breite, mehrquellige Internet-Recherche zu einem Thema durch
(paralleler Researcher-Schwarm) und sorgt dafür, dass wiederverwendbare Ergebnisse **nicht
verkommen**: Sie werden in den Best-Practices-Ordner und — falls Bugs/Fallen dabei sind — in den
Bug-Almanach eingearbeitet. Läuft in der **Claude-Cowork-Desktop-App**.

> Abgrenzung: Geht es speziell um **Bugs einer Software** → `bug-almanach-recherche`. Geht es um
> **Best Practices / Changelog einer Software oder der Harness-Werkzeuge** → `best-practices`.
> Dieser Skill ist der **allgemeine** Recherche-Weg für beliebige Themen, mit derselben
> Persistenz-Disziplin.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Ergebnisse werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert** (üblicherweise der
gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad:

| Was | Relativer Pfad |
|-----|----------------|
| Best-Practices (wiederverwendbares Wissen) | `best-practices/<kategorie>/<thema>.md` bzw. `best-practices/claude-tooling/<thema>.md` (Harness) |
| Bug-Almanach (gefundene Fallen) | `bugs/<kategorie>/<bereich>.md` |
| Lange Roh-Funde (optional, verlustfrei ausgelagert) | `research/<thema>-<datum>.md` |

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt ein Ziel-/Zwischenordner → ERST anlegen
(Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfügbar), DANN schreiben. NIEMALS abbrechen, weil ein
Ordner fehlt. Anderer Basis-Ordner vom Benutzer genannt → dorthin (gleiche Struktur).

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Mount-Brücke kann das **Dateiende abschneiden**. Nach JEDEM Schreiben
  das Dateiende prüfen (`tail -1`, `wc -l`).
- **~45s-Shell-Limit:** Researcher laufen als **Agenten** (unkritisch); jeder Git-/Schreib-Schritt
  muss in EINEM Shell-Aufruf durchlaufen.
- **Git NIEMALS nackt:** IMMER über `bash ~/proggs/cowork-git.sh` (Datenverlust-Wächter). NIE direktes `git`.

---

## Ablauf

### Schritt 1 — Frage schärfen
Die Recherche-Frage präzisieren: Was genau soll beantwortet werden, für welchen Kontext/welche
Version, welche Tiefe? Bei unklarer/zu breiter Frage 1–2 Rückfragen, bevor der Schwarm startet (sonst
streuen die Researcher zu breit). Teilfragen / Unterthemen festlegen (= die Researcher-Aufträge).

### Schritt 2 — Zeiterwartung ansagen
Kurz sagen: "N Researcher parallel, je ~5–10 Minuten. Ich melde sofort, falls einer abstürzt." Bei
Crash die anderen NICHT abbrechen, Ausfall sofort melden, am Ende zusammenfassen, welche erfolgreich waren.

### Schritt 3 — Researcher-Schwarm (7 parallel, Continuous-Spawning)
**Direkt 7 Researcher GLEICHZEITIG starten** (in EINEM Antwortblock), dann Continuous-Spawning: wird
einer fertig → SOFORT den nächsten fürs nächste Unterthema starten, bis ALLE abgedeckt sind. Konstant
7 parallel. Empirisch: 5 sicher, 7 läuft, ab ~12 RPM-Abstürze.
- **Modell:** höchstes Opus (1M), `opts.model` NICHT setzen. **Agent-Typ:** `researcher`.
- **Pro Researcher:** max ~15 Web-Fetches, max ~10 Min. **KEIN Findings-Cap** — alle relevanten Funde
  zurückgeben (Opus 1M; Kappen wäre lossy). Sehr viele → verlustfrei in `research/<thema>-<datum>.md`
  auslagern + kompakte Summary/Pfad zurückgeben.
- **429-Backoff (PFLICHT):** bei Rate-Limit-Absturz sofort melden + mit exponential backoff neu
  starten (`retry-after` beachten), nie still aufgeben.
- **Quellen-Rangordnung:** offizielle/primäre Quellen = Grundwahrheit; Community/Blogs als `extern`
  gelabelt (sekundär). Jeder Fund mit **Quelle (URL) + Datum**.
- **Aufteilung (Beispiel):** offizielle Doku · Primärquellen/Standards · Community/Praxis ·
  Gegenmeinungen/Kritik · Vergleich von Alternativen · aktuelle Entwicklungen · plattform-/kontextspezifisch.

### Schritt 4 — Synthese
Funde DEDUPLIZIEREN, gegeneinander abwägen (offiziell schlägt extern), Widersprüche benennen, eine
klare, zitierte Antwort zusammenstellen. Unsicheres ehrlich als unsicher markieren — nie raten.

### Schritt 5 — Persistenz-Prüfung (PFLICHT, sonst „verkommt" die Recherche)
> Regel `research-persistence`: Nach JEDEM Recherche-Einsatz prüfen, ob die Funde über die aktuelle
> Frage hinaus wiederverwendbar sind. Wenn ja → IMMER einarbeiten (Kurzcheck UND Volltext).

| Fund | Tauglich? | Ziel |
|------|-----------|------|
| Patterns, APIs, Architektur-Empfehlungen, Tool-/Library-Vergleich mit Empfehlung | **JA** | `best-practices/<kategorie>/` bzw. Harness-Kategorie `claude-tooling/` |
| Plattform-/Policy-/Rechts-Wissen mit Dauerwert | **JA** | passende Best-Practices-Kategorie |
| Bekannte Bugs, Fallen, Workarounds, Versions-Inkompatibilitäten | **JA** | `bugs/<kategorie>/<bereich>.md` (Format: Symptom/Ursache/Versionen/FIX/Quelle) + Querverweis |
| Einmalige Faktenabfrage ohne Wiederverwendungswert | NEIN | in EINEM Satz begründen, ggf. nur im Chat |
| Rein projektspezifischer Zustand | NEIN | Projekt-Notiz statt Best Practices |

Im Zweifel: **einarbeiten.** Jeder Eintrag mit Quelle + Datum + `offiziell`/`extern`-Flag. Existieren
zu einem Bereich BEIDE Dateien (Almanach + Best-Practice), die wechselseitigen Bezugs-Tabellen pflegen.
Findet die Recherche Bugs, aber es gibt KEINEN Almanach → die `bug-almanach-recherche` vorschlagen
(erst Franks OK), die Bugs kompakt mitliefern.

### Schritt 6 — Self-Test + Sichern (Cowork-Git)
- **Self-Test (falls Shell + Python verfügbar):** Wurde ein Almanach/Best-Practice angefasst →
  `python bugs/health.py` (alle fuenf Checks grün). Kein Python in Cowork? → Bezugs-Tabellen + Header manuell prüfen.
- **Sichern:** Git-Repo verbunden → committen + pushen über das Cowork-Skript (nur eigene Pfade):
  ```bash
  bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
  bash ~/proggs/cowork-git.sh push-files "#NNN - research <thema>: Funde + Persistenz" \
    best-practices/<...> bugs/<...>
  ```
  Kein Git-Repo → nur speichern und dem Benutzer den Ablage-Pfad nennen.

---

## Was NIEMALS passieren darf
- Aus Cowork mit nacktem `git commit`/`git push` arbeiten (immer `cowork-git.sh`).
- Eine Recherche „nur im Chat" lassen, obwohl die Funde wiederverwendbar sind (research-persistence verletzt).
- Echte Funde an einem künstlichen Cap abschneiden (alle dokumentieren; sehr viele verlustfrei auslagern).
- Eine externe Behauptung als offiziell/primär darstellen; Unsicheres als sicher ausgeben.
- Mehr als ~7 Researcher gleichzeitig (RPM-Absturz) oder einen Researcher-Crash verschweigen.
- Quelle/Datum/`offiziell`/`extern`-Flag bei einem Eintrag weglassen (spätere Nachprüfbarkeit).

## Referenzen
- `best-practices/`, `bugs/` im Arbeitsordner — Ziele der Persistenz.
- Verwandte Skills: `bug-almanach-recherche` (Bugs), `best-practices` (Changelog/Best Practices).
- `bugs/claude-tooling/cowork-git-push.md` — die Cowork-Git-/Mount-Regeln.
