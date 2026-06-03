---
name: bug-almanach-recherche
description: >-
  Recherchiert gruendlich die oeffentlich bekannten Bugs, Fallen und Workarounds eines
  technischen Bereichs und erstellt daraus einen kuratierten Bug-Almanach in
  ~/proggs/bugs/<bereich>.md. Teil des proaktiven Bug-Almanach-Systems (siehe
  ~/proggs/bugs/SYSTEM.md). Nutze diesen Skill IMMER wenn der Benutzer einen neuen
  Bug-Almanach anlegen will oder die bekannten Bugs einer beliebigen Software/Technologie
  recherchieren will — egal welche: Claude-Hooks, Kotlin, Gradle, Jetpack Compose, Swift,
  .NET/WPF, TypeScript, Chrome-Erweiterungen, Tampermonkey, Rust, Go oder was auch immer.
  Auch dann, wenn das known-bugs-before-coding-System einen neuen Bereich OHNE Almanach
  erkennt und der Benutzer sein OK zur Recherche gibt. Trigger-Phrasen (immer auch mit
  konkretem Software-Namen): "recherchiere Bugs fuer/nach Swift", "Bugs fuer Kotlin",
  "recherchiere Gradle-Bugs", "Bugs fuer die aktuelle Swift-Version", "Bugs fuer
  Kotlin-Versionen generell", "Bugs fuer die Version die ich gerade benutze", "neuen
  Bug-Almanach anlegen", "Bug-Recherche fuer X", "Almanach erstellen", "bekannte Bugs
  recherchieren", "welche Bugs gibt es bei X". Der Skill kann jederzeit MANUELL gestartet
  werden. Er ermittelt die aktuell installierte Version der JEWEILIGEN Software LIVE
  (nicht nur Claude), spawnt einen parallelen Researcher-Schwarm inkl. offizieller
  Hersteller-Quellen, prueft SEPARAT welche gefundenen Bugs in neueren Versionen bereits
  gefixt wurden, und gleicht jeden Bug gegen den lokalen best-practices/-Ordner ab, ob
  dort schon eine Loesung steht. Alle Loesungen sind funktionserhaltend und stehen
  vollstaendig in der Almanach-Datei.
---

# Bug-Almanach-Recherche

Dieser Skill erzeugt einen kuratierten, versionsbewussten Bug-Almanach fuer einen
technischen Bereich. Er kodifiziert den erprobten Recherche-Workflow des
Bug-Almanach-Systems (`~/proggs/bugs/SYSTEM.md`).

## Warum es diesen Skill gibt

Bekannte Bugs eines Bereichs VOR der Arbeit nachzuschlagen spart Stunden Debugging
(Poka-Yoke Stufe 3). Eine gute Recherche hat aber drei Tuecken, die leicht vergessen
werden: **(a)** sie muss breit sein (Doku, offizielle Quellen, Issues, Community,
Plattform, Mechanik), **(b)** sie muss versionsbewusst sein — fuer die richtige Software
UND die richtige Version, sonst jagt man Geister, und **(c)** sie soll bereits bekanntes
Wissen wiederverwenden (der lokale `best-practices/`-Ordner enthaelt oft schon die
Loesung). Dieser Skill macht alle drei zuverlaessig, jedes Mal gleich.

## Wann der Skill laeuft

- Der Benutzer bittet explizit um eine Bug-Recherche / einen neuen Almanach (mit oder
  ohne konkreten Software-Namen). Manuell jederzeit startbar.
- Das `known-bugs-before-coding`-System trifft auf einen Bereich ohne Almanach und der
  Benutzer gibt sein **OK** (die "erst OK"-Regel — die Recherche kostet Zeit/Tokens,
  also nie ungefragt starten).

## Voraussetzung: Zeiterwartung ansagen

Vor dem Spawnen dem Benutzer kurz sagen, wie lange es dauert (Agent-Zuverlaessigkeit):
"N Researcher parallel, je ~5-10 Minuten. Ich melde sofort, falls einer abstuerzt."
Bei einem Researcher-Crash die anderen NICHT abbrechen, sondern den Ausfall sofort
melden und am Ende zusammenfassen, welche erfolgreich waren.

---

## Der Workflow (7 Schritte)

### Schritt 1 — Bereich + Version(en) der JEWEILIGEN Software LIVE ermitteln

> Die Version ist der Anker fuer den Fix-Status (Schritt 3). Sie wird IMMER live
> ermittelt — fuer die Software DIESES Bereichs, NICHT pauschal fuer Claude. Arbeite
> ich an Kotlin, zaehlt die Kotlin-Version; an Gradle die Gradle-Version; an Swift die
> Swift-Version; an einer Chrome-Erweiterung die Chrome-Version usw. Oft sind MEHRERE
> Versionen gleichzeitig relevant (ein Android-Projekt hat Kotlin + Gradle + AGP +
> compileSdk) — dann alle ermitteln und alle in den Stand-Header schreiben.

Bereich benennen + kurzes Slug festlegen (z.B. `kotlin`, `gradle`, `swift-appkit`,
`claude-hooks`, `chrome-extensions`, `wpf-csharp`). Dann die relevante(n) Version(en)
live abfragen — nie raten, nie aus einer Datei nehmen, die veralten kann:

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

Software nicht in der Liste? Den passenden `--version`-Befehl bzw. die Projekt-Manifest-/
Lock-Datei nutzen. Wenn der Benutzer "fuer die aktuelle Version" sagt, ist die hier
ermittelte gemeint; sagt er "fuer Versionen generell", zusaetzlich aeltere Versionen
mitnehmen (aber die aktuell installierte bleibt der Hauptanker).

### Schritt 2 — Researcher-Schwarm (breite Bug-Suche, offizielle Quellen zuerst)

**Direkt 7 Researcher GLEICHZEITIG starten, dann CONTINUOUS-SPAWNING** (Frank 2026-06-02 + 2026-06-03):
Bei genug Teilbereichen IMMER mit **7 auf einmal** beginnen (in EINEM Antwortblock) — NICHT erst 4 und
danach nochmal 3 (das ist Zeitverschwendung). 7 gleichzeitig funktionieren einwandfrei.
Gibt es MEHR als 7 Themen, dann gilt Continuous-Spawning: sobald EINER fertig wird (es laufen also nur
noch 6), SOFORT den naechsten Researcher fuers naechste Thema hinterher starten, damit wieder 7 laufen —
und so weiter, bis ALLE Themen abgedeckt sind. **NIEMALS** warten, bis die ersten 7 alle fertig sind, und
dann erst die naechste Welle (z.B. 3) nachschieben (das wuerden in der Spitze 10 → RPM-Risiko und langsamer).
So bleibt die Parallelitaet konstant bei 7 und der RPM-Strom gleichmaessig (kein Burst). Empirisch:
5 sicher, 7 laeuft einwandfrei; ab ~12 RPM-Abstuerze. Reicht der Bereich nicht fuer 7 Teilbereiche,
mehr Researcher mit GLEICHEM Teilbereich aber unterschiedlichem Fokus (verschiedene Quellen-Typen,
Versionen, Unterthemen) spawnen — Duplikate bestaetigen den Bug, kosten aber nichts.
Fertige Prompt-Vorlagen: `references/researcher-prompts.md` — `[BEREICH]`/`[VERSION]`
einsetzen. Standard-Aufteilung (bei 7 die Liste um Unterthemen erweitern):

1. **Offizielle Doku + Hersteller-Hilfen** — die offizielle Anleitung, das Changelog
   UND gezielt offizielle Empfehlungen/Workarounds/Fixes zum konkreten Bug. Bei
   Hersteller-eigenen Bugs direkt beim Hersteller schauen: Anthropic-Docs bei Claude,
   JetBrains bei Kotlin, Gradle-Docs bei Gradle, Apple-Developer bei Swift, Microsoft-
   Learn bei .NET, Chrome-Developer bei Erweiterungen.
2. **Issue-Tracker** — gemeldete Bugs (offen + kuerzlich geschlossen).
3. **Community / Praxis** — Reddit, dev.to, Medium, Blogs, Stack Overflow, HN.
4. **Plattform-Fallen** — Windows UND macOS/Linux (Encoding, Pfade, Permissions, Shell).
5. **Mechanik / bereichsspezifisch** — die typischen konzeptionellen Fehler des Bereichs.

**Offizielle Quellen haben Vorrang vor Foren-Meinungen**: Wenn der Hersteller eine
offizielle Loesung/Empfehlung nennt, ist die die erste Wahl im FIX-Feld; Foren-Tipps
ergaenzen, ersetzen sie aber nicht. Sei grosszuegig: lieber 7 Researcher mit feineren
Unterthemen als 3 grobe — die Findings werden vollstaendiger und nichts wird gekappt.

**Pflicht-Limits pro Researcher** (gegen *Haengen*, siehe `agent-and-researcher-rules.md`
+ `subagent-crash-proofing.md`): max 15 Web-Fetches, max 10 Min. **KEIN kuenstliches
Eintrags-Cap** — ALLE gefundenen Bugs dokumentieren. (Mit Opus 4.8 / 1M-Kontext gibt es
kein Absturzrisiko mehr; ein hartes Cap, das echte Funde wegwirft, waere *lossy* und damit
verboten — siehe `lossless-context-principle.md`. Frank-Korrektur 2026-06-02: "wenn mehr
Bugs gefunden, dann auch alle dokumentieren".) Findet ein Researcher sehr viele Bugs, bleibt
er trotzdem vollstaendig: bei Bedarf die Vollliste verlustfrei in eine Datei schreiben
(File-as-Memory) und dem Hauptagenten eine kompakte Zusammenfassung + Dateipfad zurueckgeben,
statt zu kappen. Subagenten laufen auf dem hoechsten Opus-Modell (Modell-Policy) —
`opts.model` nicht setzen. Pro Bug zurueckgeben: **Titel · Symptom · Ursache · Loesung
(funktionserhaltend!) · betroffene Versionen · Quelle (URL)**.

### Schritt 3 — Fix-Status-Recherche (was ist schon gefixt?)

Der Schritt, der am leichtesten vergessen wird und am wichtigsten ist. NACH der breiten
Suche eine SEPARATE, gezielte Recherche: **Welche der gefundenen Bugs sind in neueren
Versionen (bis zur in Schritt 1 ermittelten installierten Version) bereits behoben?**

**Arbeitsteilung (WICHTIG — im Lauf 2026-06-02 verifiziert):** `researcher`-Agenten haben KEIN
Bash-Tool und koennen `gh` NICHT ausfuehren. Deshalb wird Schritt 3 aufgeteilt:

- **Researcher (2-3 parallel, nur WebFetch/WebSearch):** Changelog/Release-Notes der Versionen
  durchgehen + Sekundaerquellen (Blogs/Reddit/dev.to) als Gegenprobe. Sie liefern die konkreten
  **Issue-Nummern/URLs**, deren Status zu pruefen ist — aber NICHT den harten Status.
- **Hauptagent (hat Bash) — macht die harte Pruefung selbst:** die von den Researchern gesammelten
  GitHub-Issues per GitHub-CLI verifizieren:
  `gh issue view <nr> --repo <org>/<repo> --json number,state,title,closedAt,stateReason`
  (gh installiert + authentifiziert → echter OPEN/CLOSED-Status statt vager WebFetch-Snippets;
  verifiziert 2026-06-01 an #55889, 2026-06-02 an ksp/dagger/kotlinx.serialization), oder mehrere
  auf einmal: `gh issue list --repo <org>/<repo> --search "<stichwort>" --state all --json number,state,title`.
  Nur wenn ein Tracker NICHT ueber gh erreichbar ist (GitLab/Bugzilla/YouTrack), auf WebFetch
  ausweichen und unklare Faelle ehrlich markieren.

**Ehrlichkeits-Pflicht:** Strikt trennen zwischen *belegt gefixt* (Changelog/offizielle
Quelle) und *Status unklar / kein Fix gefunden*. Nie "gefixt" ohne Beleg — im Zweifel
bleibt der Bug "noch offen".

### Schritt 4 — Best-Practices-Abgleich (beide Richtungen: lesen UND schreiben)

Bug-Almanach und `best-practices/`-Ordner sind zwei Seiten derselben Medaille: der
Almanach sagt *was schiefgeht und wie man es umgeht*, best-practices sagt *wie man es von
vornherein richtig macht, damit der Bug nie entsteht*. Dieser Schritt verbindet beide.

**4a — LESEN (bekanntes Wissen wiederverwenden):**
Jeden gefundenen Bug gegen den lokalen Ordner abgleichen:
`grep -ri "<stichwort>" ~/proggs/best-practices/` — passende Stelle: Harness-Kategorien
`01-hooks/` … `12-neues/`, oder Projekt-Code `projekt-code/<software>/`. Steht dort schon
eine Loesung/Empfehlung, die den Bug adressiert oder ganz ausschliesst → mit in den
**FIX-Bereich** des Almanach-Eintrags aufnehmen (Verweis "siehe best-practices/<datei>").
So steht die beste bekannte Loesung direkt im Almanach. (Beispiel: `best-practices/01-hooks`
hat einen Hook-JSON-Bug verhindert, bevor er passierte.)

**4b — SCHREIBEN (Praevention zurueckspeisen):**
Hat ein Bug eine allgemeingueltige Loesung/Praevention ("so baut man es von vornherein
richtig"), diese AUCH nach best-practices eintragen — nicht nur in den Almanach:
- **Harness-Bug** (Hooks, Skills, MCP, Settings …) → passende Harness-Kategorie
  `best-practices/<NN-kategorie>/best-practices.md`.
- **Projekt-Code-Bug** (Kotlin, Swift, Gradle …) → `best-practices/projekt-code/<software>/best-practices.md`
  (Unterordner + Header `# <Software> — Best Practices (Stand DATUM, Version V)` anlegen falls noch nicht da).
Jeder Eintrag mit Quelle + Datum + `offiziell`/`extern`-Flag (gleiche Regeln wie der
`best-practices`-Skill). So fuellen sich beide Speicher: Bug+Workaround im Almanach,
Praevention in best-practices.

**4c — Bezugs-Tabellen synchron halten:** Existieren BEIDE Dateien (`bugs/<bereich>.md` UND
`best-practices/projekt-code/<software>/best-practices.md`), in JEDER eine wechselseitige
Abschnitts-Bezugs-Tabelle „Bug-Abschnitt ↔ Best-Practice-Abschnitt" anlegen/aktuell halten, damit
jede Loesung auf ihr Gegenstueck zeigt. (Gleiches Vorgehen wie im `best-practices`-Skill, Abschnitt
„Kopplung zum Bug-Almanach" — beide Skills pflegen dieselben zwei Tabellen.)

**Optional — breiter Best-Practices-Lauf:** Bei einem groesseren Bereich dem Benutzer
anbieten, separat den `best-practices`-Skill fokussiert zu starten ("Best-Practices nur
fuer <software>"), um die NEUESTEN Best-Practices der Software breit aufzurollen (geht
ueber die eine Bug-Loesung hinaus). Nur mit OK — kostet eigene Recherche-Zeit.

(Hat ein Bereich noch keine best-practices, findet 4a nichts — dann nur 4b schreiben.)

### Schritt 5 — Kuratieren in `~/proggs/bugs/<bereich>.md`

Researcher-Ergebnisse DEDUPLIZIEREN (gleiche Bugs von mehreren Researchern → EIN Eintrag,
aber das bestaetigt sie) und thematisch gruppieren. Format pro Eintrag (konsistent mit
`~/proggs/bugs/SYSTEM.md`):

```
## N. <Bug-Titel>   [⭐ HAEUFIG falls oft genannt]
**Symptom:** Was man sieht.
**Ursache:** Der wahre Grund.
**Versionen:** betrifft V1-V3, gefixt ab V4 — oder "per Design" / "unabhaengig".
**FIX:** Beste funktionserhaltende Loesung (NIE "Feature weglassen"). Offizielle
Hersteller-Loesung zuerst; best-practices-Loesung mit aufnehmen falls vorhanden.
**Quelle:** URL / eigener Vorfall / best-practices/<datei>.
```

Pflicht-Bestandteile der Datei:
- **Header** mit Pflicht-Lese-Hinweis + **Stand**-Vermerk ("recherchiert am DATUM fuer
  <Software> Version V" — bei mehreren Versionen alle nennen).
- **TL;DR** der 3-5 wichtigsten Regeln ganz oben.
- Die Bug-Eintraege, thematisch gruppiert.
- **Eigene "Fix-Status"-Sektion** (aus Schritt 3): Tabelle "Frueherer Bug | gefixt ab |
  Bezug" PLUS Liste "noch NICHT gefixt (Workaround bleibt aktiv)" + Ehrlichkeits-Hinweis
  zur Methodik.
- **Pflicht-Checkliste** am Ende.

Plattform-Unterschiede (Windows vs. macOS) je eigene Sektion. Echte deutsche Umlaute
(Doku-Regel) — AUSSER in Strings, die als Hook-stdout auf Windows ausgegeben werden
(dort ASCII wegen cp1252).

### Schritt 6 — Ins System einhaengen

1. **`~/proggs/bugs/README.md`**: Bereich aus "Bereiche ohne Almanach" nach "Vorhandene
   Almanache" verschieben (Stand, Bug-Anzahl, Erkennungs-Trigger).
2. **`~/.claude/hooks/bug-almanac-guard.{ps1,sh}`**: Pfad-Mapping pruefen; falls der
   Bereich fehlt, Dateimuster → `<bereich>.md` in BEIDEN Varianten ergaenzen +
   `claude-code-setup/hooks/` spiegeln.

### Schritt 7 — Committen + pushen

Almanach + README (+ ggf. Hook) committen und pushen (fortlaufende #-Nummer). Bei
Hook-Aenderung Cross-Platform-Sync nicht vergessen. Den Almanach zusaetzlich nicht noetig
zu spiegeln — er liegt schon im Repo unter `bugs/`.

---

## Nach der Recherche: Wartung

- Jeder spaeter SELBST erlebte Bug wird als Eintrag ergaenzt (Bug + funktionserhaltende
  Loesung + Versionen), Stand-Header aktualisiert.
- Bei deutlichem Versionssprung der benutzten Software: kurzer Re-Check (Schritt 1+3+4),
  ob alte Bugs noch gelten, neue dazukamen, neue best-practices existieren — mit OK.

## Was NIEMALS passieren darf

- Nur Claudes Version pruefen, obwohl an anderer Software gearbeitet wird (Schritt 1).
- Einen Bug als "gefixt" markieren ohne Beleg (Schritt 3 ehrlich halten).
- Den Fix-Status- oder den Best-Practices-Schritt ueberspringen.
- Eine Loesung notieren, die Funktionalitaet entfernt (Direktive #3 — funktionserhaltend).
- Foren-Meinung ueber eine offizielle Hersteller-Loesung stellen.
- Researcher ohne Fetch-/Zeit-Limits spawnen, einen Crash verschweigen, ODER echte Funde an einem kuenstlichen Eintrags-Cap abschneiden (alle gefundenen Bugs dokumentieren — bei sehr vielen verlustfrei in Datei auslagern, nie kappen).
- Rohdaten-Dumps 1:1 uebernehmen statt zu deduplizieren und zu kuratieren.

## Referenzen

- `references/researcher-prompts.md` — fertige Prompt-Vorlagen fuer Schritt 2 + 3.
- `~/proggs/bugs/SYSTEM.md` — das uebergeordnete System (Ordner, Hooks, Format).
- `~/proggs/best-practices/` — lokale Best-Practices, in Schritt 4 durchsucht.
- `~/.claude/rules/known-bugs-before-coding.md` — die Regel, die diesen Skill ausloest.
