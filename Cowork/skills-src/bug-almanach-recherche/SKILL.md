---
name: bug-almanach-recherche
description: "Recherchiert bekannte Bugs und Workarounds eines Bereichs (Kotlin, Swift, Gradle, Hooks) als versionsbewussten Bug-Almanach im Arbeitsordner. Trigger: recherchiere Bugs fuer X, Almanach anlegen."
---

# Bug-Almanach-Recherche (Cowork-Fassung) — versionsbewusster Bug-Almanach pro Bereich

Dieser Skill erzeugt einen kuratierten, versionsbewussten Bug-Almanach fuer einen technischen Bereich
(Kotlin, Gradle, Swift, .NET/WPF, TypeScript, Chrome-Erweiterungen, Hooks, Rust, Go …). Er spawnt einen
Researcher-Schwarm, prueft SEPARAT, was schon gefixt ist, gleicht gegen den lokalen `best-practices/`-Ordner
ab und kuratiert alles funktionserhaltend in eine Almanach-Datei. Gegenstueck zum `best-practices`-Skill
(dort *wie man es richtig macht*, hier *was schiefgeht*). Läuft in der **Claude-Cowork-Desktop-App**.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Alle Ergebnisse werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert** (üblicherweise der
gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad. Ziel-Struktur relativ
zum Arbeitsordner:

```
bugs/
├── README.md                    ← Index (Bereiche mit/ohne Almanach, Trigger)
├── SYSTEM.md                    ← das uebergeordnete System (Format, Hooks)
├── health.py                    ← Self-Test (5 Checks)
├── check-version-anchor.py      ← Live-Abgleich der Versions-Anker
└── <kategorie>/<bereich>.md     ← der Almanach (android, android-build, desktop, web,
                                    apis, peripherie, claude-tooling, …)

best-practices/
└── <kategorie>/<software>.md    ← Praevention (Projekt-Code), bzw.
    claude-tooling/<thema>.md     ← Praevention (Harness)
```

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt der Kategorie-Ordner → ERST anlegen (Datei-Werkzeug
bzw. `mkdir -p`, falls Shell verfügbar), DANN schreiben. NIEMALS abbrechen, weil ein Ordner fehlt. Nennt
der Benutzer einen anderen Basis-Ordner, dort hinein (gleiche Struktur). Die Best-Practices-Struktur ist
FLACH: `best-practices/<kategorie>/<software>.md` (Projekt-Code), `best-practices/claude-tooling/<thema>.md`
(Harness) — kein `best-practices-`-Präfix, keine `projekt-code/`-Ebene, keine nummerierten Ordner.

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Cowork-Mount-Brücke kann das **Dateiende abschneiden**. Nach JEDEM Schreiben
  das Dateiende prüfen (`tail -1`, `wc -l`) — besonders bei der großen Almanach-Datei.
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf läuft max ~45 Sekunden; Hintergrundprozesse überleben den
  Wechsel zwischen Aufrufen NICHT. Researcher laufen als **Agenten** (unkritisch); jeder Git-/`gh`-/Script-
  Schritt muss in EINEM Aufruf durchlaufen.
- **Git NIEMALS nackt:** Aus Cowork IMMER über `bash ~/proggs/cowork-git.sh` committen/pushen (fängt
  Mount-Fallen + Datenverlust-Wächter ab). NIE direktes `git commit`/`git push`.

## Wann der Skill laeuft

- Der Benutzer bittet explizit um eine Bug-Recherche / einen neuen Almanach (mit oder ohne konkreten
  Software-Namen). Manuell jederzeit startbar.
- Ein neuer Bereich ohne Almanach wird erkannt und der Benutzer gibt sein **OK** (die "erst OK"-Regel —
  die Recherche kostet Zeit/Tokens, also nie ungefragt starten).

## Voraussetzung: Zeiterwartung ansagen

Vor dem Spawnen kurz sagen, wie lange es dauert: "N Researcher parallel, je ~5-10 Minuten. Ich melde sofort,
falls einer abstuerzt." Bei einem Researcher-Crash die anderen NICHT abbrechen, sondern den Ausfall sofort
melden und am Ende zusammenfassen, welche erfolgreich waren.

---

## Der Workflow (7 Schritte)

### Schritt 1 — Bereich + Version(en) der JEWEILIGEN Software LIVE ermitteln

> Die Version ist der Anker fuer den Fix-Status (Schritt 3). IMMER live ermitteln — fuer die Software DIESES
> Bereichs, NICHT pauschal fuer Claude. Kotlin-Arbeit → Kotlin-Version; Gradle → Gradle-Version; Swift →
> Swift-Version usw. Oft sind MEHRERE Versionen gleichzeitig relevant (Android: Kotlin + Gradle + AGP +
> compileSdk) — dann alle ermitteln und alle in den Stand-Header schreiben.

Bereich benennen + kurzes Slug festlegen (z.B. `kotlin`, `gradle`, `swift-appkit`, `claude-hooks`,
`chrome-extensions`, `wpf-csharp`). Dann die relevante(n) Version(en) live abfragen — nie raten, nie aus
einer veraltbaren Datei nehmen:

| Software / Bereich | Versions-Befehl(e) (live) |
|--------------------|---------------------------|
| Claude Code / Hooks | `claude --version` |
| Kotlin | `kotlinc -version`; Kotlin-Plugin-Version aus `build.gradle.kts` |
| Gradle | `./gradlew --version` (Gradle + JVM) |
| Android (AGP/SDK) | AGP aus `build.gradle`; `compileSdk`/`minSdk` aus dem Modul |
| Jetpack Compose | Compose-BOM/Compiler-Version aus `build.gradle.kts` |
| Swift / macOS | `swift --version`; `xcodebuild -version` (Xcode) |
| .NET / WPF | `dotnet --version`; TargetFramework aus `.csproj` |
| TypeScript / Node | `tsc --version`; `node --version`; Dep-Versionen aus `package.json` |
| Chrome-Erweiterung | `chrome --version` / `msedge --version` |
| Rust / Go | `rustc --version` / `go version` |

Software nicht in der Liste? Den passenden `--version`-Befehl bzw. die Projekt-Manifest-/Lock-Datei nutzen.
"Fuer die aktuelle Version" = die hier ermittelte; "fuer Versionen generell" = zusaetzlich aeltere mitnehmen
(die installierte bleibt der Hauptanker).

### Schritt 2 — Researcher-Schwarm (breite Bug-Suche, offizielle Quellen zuerst)

**Direkt 7 Researcher GLEICHZEITIG starten, dann CONTINUOUS-SPAWNING:** Bei genug Teilbereichen IMMER mit
**7 auf einmal** beginnen (in EINEM Antwortblock) — NICHT erst 4 und dann 3. Gibt es MEHR als 7 Themen:
sobald EINER fertig wird, SOFORT den naechsten hinterher starten, sodass konstant 7 laufen, bis ALLE Themen
abgedeckt sind. NIEMALS warten, bis die ersten 7 alle fertig sind, und dann eine zweite Welle nachschieben.
Empirisch: 5 sicher, 7 läuft einwandfrei, ab ~12 RPM-Abstuerze (RPM-Limit, NICHT Kontextfenster). Reicht der
Bereich nicht fuer 7 Teilbereiche, mehr Researcher mit GLEICHEM Teilbereich aber anderem Fokus (Quellen-Typ,
Versionen, Unterthemen) spawnen — Duplikate bestaetigen den Bug, kosten aber nichts. Researcher laufen als
**Agenten** (nicht als Shell-Hintergrund → vom ~45s-Cowork-Limit unberührt). Fertige Prompt-Vorlagen:
`references/researcher-prompts.md` — `[BEREICH]`/`[VERSION]` einsetzen. Standard-Aufteilung:

1. **Offizielle Doku + Hersteller-Hilfen** — offizielle Anleitung, Changelog UND gezielt offizielle
   Empfehlungen/Workarounds/Fixes zum konkreten Bug. Bei Hersteller-eigenen Bugs direkt beim Hersteller:
   Anthropic bei Claude, JetBrains bei Kotlin, Gradle-Docs bei Gradle, Apple-Developer bei Swift,
   Microsoft-Learn bei .NET, Chrome-Developer bei Erweiterungen.
2. **Issue-Tracker** — gemeldete Bugs (offen + kuerzlich geschlossen).
3. **Community / Praxis** — Reddit, dev.to, Medium, Blogs, Stack Overflow, HN.
4. **Plattform-Fallen** — Windows UND macOS/Linux (Encoding, Pfade, Permissions, Shell).
5. **Mechanik / bereichsspezifisch** — die typischen konzeptionellen Fehler des Bereichs.

**Offizielle Quellen haben Vorrang vor Foren-Meinungen:** Nennt der Hersteller eine offizielle
Loesung/Empfehlung, ist die die erste Wahl im FIX-Feld; Foren-Tipps ergaenzen, ersetzen sie nie.

**Pflicht-Limits pro Researcher** (gegen *Haengen*): max 15 Web-Fetches, max 10 Min. **KEIN kuenstliches
Eintrags-Cap** — ALLE gefundenen Bugs dokumentieren (Opus 1M = kein Absturzrisiko; ein hartes Cap, das echte
Funde wegwirft, waere *lossy* und verboten). Findet ein Researcher sehr viele Bugs, bleibt er trotzdem
vollstaendig: bei Bedarf die Vollliste verlustfrei in eine Datei schreiben + dem Hauptagenten eine kompakte
Summary + Dateipfad zurueckgeben, statt zu kappen. Subagenten laufen auf dem hoechsten Opus-Modell —
`opts.model` NICHT setzen. Pro Bug zurueckgeben: **Titel · Symptom · Ursache · Loesung (funktionserhaltend!)
· betroffene Versionen · Quelle (URL)**. Bei 429/Rate-Limit-Absturz sofort melden + mit exponential backoff
neu starten (`retry-after` beachten), nie still aufgeben.

### Schritt 3 — Fix-Status-Recherche (was ist schon gefixt?)

Der am leichtesten vergessene und wichtigste Schritt. NACH der breiten Suche eine SEPARATE, gezielte
Recherche: **Welche der gefundenen Bugs sind in neueren Versionen (bis zur in Schritt 1 ermittelten
installierten Version) bereits behoben?**

**Arbeitsteilung:** `researcher`-Agenten haben KEIN Bash-Tool und koennen `gh` NICHT ausfuehren. Deshalb:

- **Researcher (2-3 parallel, nur WebFetch/WebSearch):** Changelog/Release-Notes der Versionen durchgehen +
  Sekundaerquellen (Blogs/Reddit/dev.to) als Gegenprobe. Sie liefern die konkreten **Issue-Nummern/URLs**,
  deren Status zu pruefen ist — aber NICHT den harten Status.
- **Hauptagent (hat Bash) — macht die harte Pruefung selbst:** die gesammelten GitHub-Issues per GitHub-CLI
  verifizieren (in EINEM Cowork-Aufruf, ~45s-Limit beachten):
  `gh issue view <nr> --repo <org>/<repo> --json number,state,title,closedAt,stateReason`
  (gh installiert + authentifiziert → echter OPEN/CLOSED-Status statt vager WebFetch-Snippets), oder mehrere:
  `gh issue list --repo <org>/<repo> --search "<stichwort>" --state all --json number,state,title`.
  Nur wenn ein Tracker NICHT ueber gh erreichbar ist (GitLab/Bugzilla/YouTrack), auf WebFetch ausweichen und
  unklare Faelle ehrlich markieren.

**Ehrlichkeits-Pflicht:** Strikt trennen zwischen *belegt gefixt* (Changelog/offizielle Quelle) und *Status
unklar / kein Fix gefunden*. Nie "gefixt" ohne Beleg — im Zweifel bleibt der Bug "noch offen".

### Schritt 4 — Best-Practices-Abgleich (beide Richtungen: lesen UND schreiben)

Bug-Almanach und `best-practices/`-Ordner sind zwei Seiten derselben Medaille. Dieser Schritt verbindet sie.

**4a — LESEN (bekanntes Wissen wiederverwenden):** Jeden gefundenen Bug gegen den lokalen Ordner abgleichen:
`grep -ri "<stichwort>" best-practices/` — passende Stelle: Harness `best-practices/claude-tooling/<thema>.md`,
oder Projekt-Code `best-practices/<kategorie>/<software>.md`. Steht dort schon eine Loesung/Empfehlung, die
den Bug adressiert oder ganz ausschliesst → mit in den **FIX-Bereich** des Almanach-Eintrags aufnehmen
(Verweis "siehe best-practices/<datei>"). So steht die beste bekannte Loesung direkt im Almanach.

**4b — SCHREIBEN (Praevention zurueckspeisen):** Hat ein Bug eine allgemeingueltige Loesung/Praevention,
diese AUCH nach best-practices eintragen — nicht nur in den Almanach:
- **Harness-Bug** (Hooks, Skills, MCP, Settings …) → `best-practices/claude-tooling/<thema>.md`
  (z. B. `hooks.md`, `mcp.md`, `settings.md`).
- **Projekt-Code-Bug** (Kotlin, Swift, Gradle …) → `best-practices/<kategorie>/<software>.md`
  (Datei `<software>.md` direkt im Kategorie-Ordner + Header `# <Software> — Best Practices (Stand DATUM,
  Version V)` anlegen falls noch nicht da; gleiche Kategorie wie der Almanach, kein `best-practices-`-Präfix,
  keine `projekt-code/`-Ebene).
Jeder Eintrag mit Quelle + Datum + `offiziell`/`extern`-Flag.

**4c — Bezugs-Tabellen synchron halten:** Existieren BEIDE Dateien (`bugs/<kategorie>/<bereich>.md` UND
`best-practices/<kategorie>/<software>.md`), in JEDER eine wechselseitige Abschnitts-Bezugs-Tabelle
„Bug-Abschnitt ↔ Best-Practice-Abschnitt" anlegen/aktuell halten, damit jede Loesung auf ihr Gegenstueck zeigt.

**Optional — breiter Best-Practices-Lauf:** Bei einem groesseren Bereich anbieten, separat den
`best-practices`-Skill fokussiert zu starten ("Best-Practices nur fuer <software>"), um die NEUESTEN Best
Practices breit aufzurollen. Nur mit OK — kostet eigene Recherche-Zeit.

(Hat ein Bereich noch keine best-practices, findet 4a nichts — dann nur 4b schreiben.)

### Schritt 5 — Kuratieren in `bugs/<kategorie>/<bereich>.md`

Researcher-Ergebnisse DEDUPLIZIEREN (gleiche Bugs von mehreren Researchern → EIN Eintrag, das bestaetigt sie)
und thematisch gruppieren. Format pro Eintrag (konsistent mit `bugs/SYSTEM.md`):

```
## N. <Bug-Titel>   [⭐ HAEUFIG falls oft genannt]
**Symptom:** Was man sieht.
**Ursache:** Der wahre Grund.
**Versionen:** betrifft V1-V3, gefixt ab V4 — oder "per Design" / "unabhaengig".
**FIX:** Beste funktionserhaltende Loesung (NIE "Feature weglassen"). Offizielle Hersteller-Loesung zuerst;
best-practices-Loesung mit aufnehmen falls vorhanden.
**Quelle:** URL / eigener Vorfall / best-practices/<datei>.
```

Pflicht-Bestandteile der Datei:
- **Header** mit Pflicht-Lese-Hinweis + **Stand**-Vermerk ("recherchiert am DATUM fuer <Software> Version V"
  — bei mehreren Versionen alle nennen).
- **TL;DR** der 3-5 wichtigsten Regeln ganz oben.
- Die Bug-Eintraege, thematisch gruppiert.
- **Eigene "Fix-Status"-Sektion** (aus Schritt 3): Tabelle "Frueherer Bug | gefixt ab | Bezug" PLUS Liste
  "noch NICHT gefixt (Workaround bleibt aktiv)" + Ehrlichkeits-Hinweis zur Methodik.
- **Pflicht-Checkliste** am Ende.

Plattform-Unterschiede (Windows vs. macOS) je eigene Sektion. Echte deutsche Umlaute (Doku-Regel) — AUSSER
in Strings, die als Hook-stdout auf Windows ausgegeben werden (dort ASCII wegen cp1252).

### Schritt 6 — Ins System einhaengen

0. **Kategorie waehlen:** Den Almanach in den passenden Kategorie-Ordner legen
   (`bugs/<kategorie>/<bereich>.md` — android, android-build, desktop, web, peripherie, claude-tooling;
   passt nichts, neue Kategorie anlegen). Die Best-Practices-Gegenseite in dieselbe Kategorie
   (`best-practices/<kategorie>/<software>.md`).
1. **`bugs/README.md`:** Bereich unter der passenden Kategorie aus "Bereiche ohne Almanach" nach "Vorhandene
   Almanache" verschieben (Stand, Bug-Anzahl, Erkennungs-Trigger).
2. **Versions-Anker setzen (NUR software-gebundene Almanache):** Direkt unter dem `Stand:`-Header ein
   maschinenlesbares Feld `> **Anker:** <label>=<version>` setzen (SYSTEM.md). Wenn die INSTALLIERTE Version
   == der fuer den Almanach relevanten ist (claude-code, python, node …): zusaetzlich einen Eintrag in
   `bugs/check-version-anchor.py` → `ANCHORS` mit `live`-Tupel `(cmd, regex)` ergaenzen. Bei
   PROJEKT-gebundenen Bereichen (Gradle/.csproj/Toolchain pinnt die Version → installiert != relevant)
   `live: None` (nur Anker-Vollstaendigkeit, kein Live-Abgleich → kein Falschalarm).
3. **Self-Test (PFLICHT vor Commit, falls Shell + Python verfügbar):** `python bugs/health.py` laufen lassen
   — alle fuenf Checks (coupling, guard-coverage, version-anchor, dead-paths, Stand-Verfall) muessen gruen
   sein. Ein neuer Almanach ohne Bezugs-Tabelle oder (software-gebunden) ohne Anker-Feld faellt hier sofort
   auf. Kein Python in Cowork? → Bezugs-Tabellen + Header manuell gegenprüfen, ehrlich vermerken.

> Hinweis Cowork: Die CLI-Hooks `bug-almanac-guard` und `bug-almanac-hint` existieren in Cowork NICHT —
> ihr Mapping/Trigger muss hier NICHT gepflegt werden. Auf der CLI-Seite gleicht sich das System ohnehin
> über die kategorie-robuste rekursive Almanach-Suche selbst ab.

### Schritt 7 — Sichern (Cowork-Git)

Almanach + `bugs/README.md` (+ ggf. best-practices-Datei) committen und pushen — siehe Abschnitt „Sichern".

---

## Nach der Recherche: Wartung

- Jeder spaeter SELBST erlebte Bug wird als Eintrag ergaenzt (Bug + funktionserhaltende Loesung + Versionen),
  Stand-Header aktualisiert.
- Bei deutlichem Versionssprung der benutzten Software: kurzer Re-Check (Schritt 1+3+4) — mit OK.

## Sichern (Cowork-Git)

Git-Repo verbunden → committen + pushen über das Cowork-Skript (nur die eigenen Pfade namentlich, in EINEM Aufruf):
```bash
bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push-files "#NNN - bug-almanach <bereich>: recherchiert + kuratiert" \
  bugs/<kategorie>/<bereich>.md bugs/README.md best-practices/<kategorie>/<software>.md
```
Kein Git-Repo verbunden → nur speichern und dem Benutzer den Ablage-Pfad nennen. Nach dem Schreiben das
Dateiende prüfen (`tail -1`, `wc -l`) — Mount-Truncation-Schutz.

## Was NIEMALS passieren darf

- Aus Cowork mit nacktem `git commit`/`git push` arbeiten (immer `cowork-git.sh`).
- Dateiende nach dem Schreiben NICHT prüfen (Mount-Truncation übersehen).
- Nur Claudes Version pruefen, obwohl an anderer Software gearbeitet wird (Schritt 1).
- Einen Bug als "gefixt" markieren ohne Beleg (Schritt 3 ehrlich halten).
- Den Fix-Status- oder den Best-Practices-Schritt ueberspringen.
- Eine Loesung notieren, die Funktionalitaet entfernt (funktionserhaltend bleiben).
- Foren-Meinung ueber eine offizielle Hersteller-Loesung stellen.
- Researcher ohne Fetch-/Zeit-Limits spawnen, mehr als 7 gleichzeitig laufen lassen, einen Crash
  verschweigen, ODER echte Funde an einem kuenstlichen Eintrags-Cap abschneiden (alle gefundenen Bugs
  dokumentieren — bei sehr vielen verlustfrei in Datei auslagern, nie kappen).
- Rohdaten-Dumps 1:1 uebernehmen statt zu deduplizieren und zu kuratieren.
- Alte best-practices-Pfadmuster verwenden (`projekt-code/`, `best-practices-<x>.md`, nummerierte Ordner) —
  die Struktur ist flach `best-practices/<kategorie>/<software>.md` bzw. `best-practices/claude-tooling/<thema>.md`.

## Referenzen

- `references/researcher-prompts.md` — fertige Prompt-Vorlagen fuer Schritt 2 + 3.
- `bugs/SYSTEM.md` — das uebergeordnete System (Ordner, Format) im Arbeitsordner.
- `best-practices/` — lokale Best-Practices, in Schritt 4 durchsucht.
- Gegenstück: Skill `best-practices` (wie man es richtig macht ↔ was schiefgeht).
