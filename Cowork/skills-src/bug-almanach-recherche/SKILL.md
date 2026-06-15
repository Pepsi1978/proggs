---
name: bug-almanach-recherche
description: "Recherchiert bekannte Bugs und Workarounds einer Software und baut einen versionsbewussten Bug-Almanach im Arbeitsordner. Trigger: Bug-Almanach anlegen, Bugs fuer X recherchieren."
---

# Bug-Almanach-Recherche (Cowork-Fassung)

Diese Cowork-Fassung erzeugt einen kuratierten, versionsbewussten Bug-Almanach fuer einen
technischen Bereich (Kotlin, Gradle, Swift, .NET, TypeScript, Chrome-Erweiterungen …). Sie
laeuft in der **Claude-Cowork-Desktop-App** und ist auf deren Umgebung zugeschnitten.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Alle Ergebnisse werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert** (üblicherweise
der gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad. Struktur relativ
zum Arbeitsordner:

| Was | Relativer Pfad |
|-----|----------------|
| Bug-Almanach | `bugs/<kategorie>/<bereich>.md` |
| Almanach-Index | `bugs/README.md` |
| Best-Practices-Gegenseite | `best-practices/projekt-code/<kategorie>/best-practices-<software>.md` |
| Self-Test / Werkzeuge | `bugs/health.py`, `bugs/check-version-anchor.py` |

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt ein Ziel- oder Zwischenordner (neue Kategorie,
neuer Bereich) → ERST anlegen (Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfügbar), DANN
schreiben. NIEMALS abbrechen, weil ein Ordner fehlt. Nennt der Benutzer einen anderen Basis-Ordner,
dort ablegen — mit gleicher Unterstruktur.

Kategorien (wie im Bug-Almanach): `android`, `android-build`, `desktop`, `web`, `peripherie`,
`apis`, `assets`, `agents`, `claude-tooling`. Passt nichts → neue Kategorie anlegen.

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Cowork-Mount-Brücke kann beim Schreiben grosser Dateien das **Dateiende
  abschneiden**. Nach JEDEM Schreiben das Dateiende prüfen (`tail -1`, `wc -l`) — nicht nur den Anfang.
  Bei wichtigen Dateien lieber git-intern bauen statt über den Mount.
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf läuft max ~45 Sekunden; Hintergrundprozesse überleben
  den Wechsel zwischen Aufrufen NICHT. Researcher laufen als **Agenten** (nicht als Shell-Hintergrund),
  das ist unkritisch. Aber jeder Git-/Schreib-Schritt muss in EINEM Aufruf durchlaufen.
- **Git NIEMALS nackt:** Aus Cowork wird IMMER über `bash ~/proggs/cowork-git.sh` committet/gepusht
  (fängt Mount-Fallen ab: Lock auf VM-Platte, `core.fileMode false`, LFS-Zeiger, Build-Berge,
  Datenverlust-Wächter). NIE direktes `git commit`/`git push`.

---

## Vor dem Spawnen: Zeiterwartung ansagen

Kurz sagen, wie lange es dauert: "N Researcher parallel, je ~5–10 Minuten. Ich melde sofort, falls
einer abstürzt." Bei einem Crash die anderen NICHT abbrechen, Ausfall sofort melden, am Ende
zusammenfassen, welche erfolgreich waren.

## Der Workflow (7 Schritte)

### Schritt 1 — Bereich + Version(en) der JEWEILIGEN Software LIVE ermitteln
Bereich + kurzes Slug festlegen (`kotlin`, `gradle`, `swift-appkit`, `chrome-extensions` …). Die
relevante(n) Version(en) live abfragen — nie raten, nie aus einer veraltbaren Datei nehmen:

| Software / Bereich | Versions-Befehl (falls Shell verfügbar) |
|--------------------|------------------------------------------|
| Claude Code / Hooks | `claude --version` |
| Kotlin | `kotlinc -version`; Plugin-Version aus `build.gradle.kts` |
| Gradle | `./gradlew --version` |
| Android (AGP/SDK) | AGP aus `build.gradle`; `compileSdk`/`minSdk` |
| Jetpack Compose | Compose-BOM aus `build.gradle.kts` |
| Swift / macOS | `swift --version`; `xcodebuild -version` |
| .NET / WPF | `dotnet --version`; TargetFramework aus `.csproj` |
| TypeScript / Node | `tsc --version`; `node --version`; `package.json` |
| Chrome-Erweiterung | `chrome --version` |

Ist in Cowork keine Shell verfügbar, die Version aus Manifest-/Lock-/Projektdateien lesen und das
ehrlich als "aus Datei, nicht live" markieren. Mehrere Versionen pro Projekt sind normal → alle in
den Stand-Header.

### Schritt 2 — Researcher-Schwarm (breite Bug-Suche, offizielle Quellen zuerst)
**Direkt 7 Researcher GLEICHZEITIG starten, dann Continuous-Spawning:** Bei genug Teilbereichen mit
7 auf einmal beginnen (nicht erst 4, dann 3). Wird einer fertig (nur noch 6 laufen) → SOFORT den
nächsten fürs nächste Thema starten, bis ALLE Themen abgedeckt sind. Konstant 7 parallel. Empirisch:
5 sicher, 7 läuft, ab ~12 RPM-Abstürze. Teilbereiche (Vorlagen in `references/researcher-prompts.md`):

1. **Offizielle Doku + Hersteller-Hilfen** (Vorrang vor Foren) — Anthropic/JetBrains/Gradle/Apple/Microsoft/Chrome.
2. **Issue-Tracker** — offen + kürzlich geschlossen.
3. **Community / Praxis** — Reddit, dev.to, Medium, SO, HN.
4. **Plattform-Fallen** — Windows UND macOS/Linux (Encoding, Pfade, Permissions, Shell).
5. **Mechanik / bereichsspezifisch** — typische konzeptionelle Fehler.

Pro Researcher: max 15 Web-Fetches, max 10 Min. **KEIN Eintrags-Cap** — alle gefundenen Bugs
dokumentieren (Opus 1M; Kappen wäre lossy). Sehr viele Funde → verlustfrei in Datei + kompakte
Summary. Pro Bug zurückgeben: **Titel · Symptom · Ursache · Lösung (funktionserhaltend!) · betroffene
Versionen · Quelle (URL)**. Den bestehenden Almanach-Stand mitgeben → Researcher suchen gezielt NUR
NEUES seit Stand X. Researcher laufen auf dem hoechsten Opus-Modell (`opts.model` NICHT setzen).

### Schritt 3 — Fix-Status-Recherche (was ist schon gefixt?)
SEPARATE, gezielte Recherche: Welche gefundenen Bugs sind bis zur installierten Version bereits behoben?
- **Researcher (2–3, nur WebFetch):** Changelog/Release-Notes durchgehen + Sekundärquellen als
  Gegenprobe. Sie liefern die **Issue-Nummern/URLs**, NICHT den harten Status.
- **Hauptagent — gh-Status HART prüfen (falls Shell + gh verfügbar):**
  `gh issue view <nr> --repo <org>/<repo> --json number,state,title,closedAt,stateReason`
  → `NOT_PLANNED` = won't fix (Workaround bleibt DAUERHAFT), `COMPLETED` = gefixt, `DUPLICATE` = gebündelt.
  Researcher-Web-Snippets sind bei Versions-/Status-Angaben unzuverlässig — **gh ist die Grundwahrheit.**
  Ist in der Cowork-Shell kein `gh` verfügbar, das Changelog als Beleg nehmen und unklare Fälle ehrlich
  als "Status unklar" markieren statt zu raten.

**Ehrlichkeits-Pflicht:** strikt trennen zwischen *belegt gefixt* und *Status unklar*. Nie "gefixt"
ohne Beleg — im Zweifel bleibt der Bug "noch offen".

### Schritt 4 — Best-Practices-Abgleich (beide Richtungen: lesen UND schreiben)
- **4a LESEN:** Jeden Bug gegen `best-practices/` abgleichen (`grep -ri "<stichwort>" best-practices/`).
  Steht dort schon eine Lösung → in den **FIX**-Bereich des Almanach-Eintrags aufnehmen (Verweis).
- **4b SCHREIBEN:** Hat ein Bug eine allgemeingültige Prävention, diese AUCH nach best-practices
  eintragen — Projekt-Code → `best-practices/projekt-code/<kategorie>/best-practices-<software>.md`,
  Harness → `best-practices/<NN-kategorie>/best-practices.md`. Mit Quelle + Datum + `offiziell`/`extern`.
- **4c Bezugs-Tabellen synchron halten:** Existieren BEIDE Dateien (Almanach + Best-Practice), in jeder
  eine wechselseitige Abschnitts-Bezugs-Tabelle „Bug-Abschnitt ↔ Best-Practice-Abschnitt" pflegen.

### Schritt 5 — Kuratieren in `bugs/<kategorie>/<bereich>.md`
Researcher-Ergebnisse DEDUPLIZIEREN (gleicher Bug von mehreren → EIN Eintrag) und thematisch gruppieren.
Format pro Eintrag:
```
## N. <Bug-Titel>   [⭐ HAEUFIG falls oft genannt]
**Symptom:** Was man sieht.
**Ursache:** Der wahre Grund.
**Versionen:** betrifft V1–V3, gefixt ab V4 — oder "per Design" / "unabhaengig".
**FIX:** Beste funktionserhaltende Loesung (NIE "Feature weglassen"). Offizielle Loesung zuerst.
**Quelle:** URL / eigener Vorfall / best-practices/<datei>.
```
Pflicht-Bestandteile der Datei:
- **Header** mit Pflicht-Lese-Hinweis + **Stand**-Vermerk ("recherchiert am DATUM für <Software> Version V").
- **Kurzcheck (Stufe A)**-Tabelle ganz oben (Erkennungssignale + Sofort-Regeln, innerhalb der ersten 80 Zeilen).
- **TL;DR** der 3–5 wichtigsten Regeln.
- Bug-Einträge, thematisch gruppiert; Plattform-Unterschiede je eigene Sektion.
- **Fix-Status-Sektion** (Schritt 3): Tabelle "Früherer Bug | gefixt ab | Bezug" + Liste "noch NICHT
  gefixt" + Methodik-/Ehrlichkeits-Hinweis (gh-hart wo geprüft).
- **Pflicht-Checkliste** am Ende.

Echte deutsche Umlaute — AUSSER in Strings, die als Hook-stdout auf Windows ausgegeben werden (ASCII wegen cp1252).

### Schritt 6 — Ins System einhängen (inkl. neue W3-Mechanismen)
0. **Kategorie wählen** (siehe Tabelle §0). Best-Practices-Gegenseite in dieselbe Kategorie.
1. **`bugs/README.md`:** Bereich aus "ohne Almanach" nach "Vorhandene Almanache" verschieben (Stand,
   Bug-Anzahl, Erkennungs-Trigger).
2. **Versions-Anker (W3-1, NUR software-gebundene Almanache):** Direkt unter dem `Stand:`-Header ein
   maschinenlesbares Feld setzen:
   `> **Anker:** <label>=<version>` (z. B. `> **Anker:** kotlin=2.1.0`).
   Ist die INSTALLIERTE Version == der relevanten (claude-code, python, node …): zusätzlich Eintrag in
   `bugs/check-version-anchor.py` → `ANCHORS` mit `live`-Tupel `(cmd, regex)`. Bei PROJEKT-gebundenen
   Bereichen (Gradle/.csproj pinnt die Version) `live: None` (nur Anker-Vollständigkeit, kein Live-Abgleich).
3. **Semantischer Prompt-Trigger (W3-2):** Hat der Bereich eindeutige Stichwörter, ihn in
   `bug-almanac-hint.py` (Hook) → `AREAS` aufnehmen:
   `"<kategorie>/<bereich>": ("<Anzeigename>", [eindeutige Mehrwort-Stichwoerter, lowercase])`.
   Stichwörter spezifisch (Mehrwort) halten, keine Fehlalarme. (Hook-Pflege nur, wenn der Arbeitsordner
   die Hooks enthält; sonst als TODO an Frank melden.)
4. **Self-Test (PFLICHT vor Commit, falls Shell + Python verfügbar):** `python bugs/health.py` —
   alle VIER Checks (coupling, guard-coverage, **version-anchor**, Stand-Verfall) müssen grün sein.
   Ein Almanach ohne Bezugs-Tabelle, ohne Guard-Mapping oder (software-gebunden) ohne Anker-Feld fällt
   hier sofort auf. Kein Python in Cowork? → Anker-Feld + Bezugs-Tabelle manuell gegenprüfen, ehrlich vermerken.

### Schritt 7 — Sichern (Cowork-Git)
Git-Repo verbunden → committen + pushen über das Cowork-Skript:
```bash
bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push-files "#NNN - bug-almanach <bereich>: recherchiert + W3" \
  bugs/<kategorie>/<bereich>.md bugs/README.md \
  best-practices/projekt-code/<kategorie>/best-practices-<software>.md
```
`push-files` committet GEZIELT nur diese Pfade (Mount-schonend) + Datenverlust-Wächter (bricht bei
verdächtiger Schrumpfung/Phantom-Löschung ab). Kein Git-Repo → nur speichern und dem Benutzer den
Ablage-Pfad nennen.

---

## Was NIEMALS passieren darf
- Aus Cowork mit nacktem `git commit`/`git push` arbeiten (immer `cowork-git.sh`).
- Eine grosse Datei schreiben, ohne danach das Dateiende zu prüfen (Mount-Truncation).
- Nur Claudes Version prüfen, obwohl an anderer Software gearbeitet wird (Schritt 1).
- Einen Bug als "gefixt" markieren ohne Beleg; Fix-Status- oder Best-Practices-Schritt überspringen.
- Eine Lösung notieren, die Funktionalität entfernt (funktionserhaltend!).
- Researcher ohne Fetch-/Zeit-Limits spawnen oder echte Funde an einem künstlichen Cap abschneiden.
- Beim Einhängen das Anker-Feld (W3-1) oder den `health.py`-Self-Test vergessen.

## Referenzen
- `references/researcher-prompts.md` — fertige Prompt-Vorlagen (Phase A breite Suche + Phase B Fix-Status).
- `bugs/SYSTEM.md`, `bugs/README.md` — das Almanach-System im Arbeitsordner.
- `bugs/claude-tooling/cowork-git-push.md` — die Cowork-Git-/Mount-Regeln im Detail.
