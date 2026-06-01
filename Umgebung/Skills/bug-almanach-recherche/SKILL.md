---
name: bug-almanach-recherche
description: >-
  Recherchiert gruendlich die oeffentlich bekannten Bugs, Fallen und Workarounds eines
  technischen Bereichs und erstellt daraus einen kuratierten Bug-Almanach in
  ~/proggs/bugs/<bereich>.md. Teil des proaktiven Bug-Almanach-Systems (siehe
  ~/proggs/bugs/SYSTEM.md). Nutze diesen Skill IMMER wenn der Benutzer einen neuen
  Bug-Almanach anlegen will, die bekannten Bugs eines Bereichs recherchieren will
  (Chrome-Erweiterungen, Android/Compose, WPF, Swift, TypeScript, Tampermonkey,
  Claude-Hooks, Gradle, oder jede andere Technologie), oder wenn das
  known-bugs-before-coding-System einen neuen Bereich OHNE Almanach erkennt und der
  Benutzer sein OK zur Recherche gibt. Trigger-Phrasen: "recherchiere Bugs fuer X",
  "neuen Bug-Almanach anlegen", "Bug-Recherche fuer X", "Almanach erstellen",
  "bekannte Bugs recherchieren", "welche Bugs gibt es bei X". Der Skill ermittelt die
  aktuell installierte Version LIVE, spawnt einen parallelen Researcher-Schwarm und
  prueft SEPARAT, welche gefundenen Bugs in neueren Versionen bereits gefixt wurden —
  damit nie ein laengst behobener Bug als aktiv behandelt wird. Loesungen sind immer
  funktionserhaltend.
---

# Bug-Almanach-Recherche

Dieser Skill erzeugt einen kuratierten, versionsbewussten Bug-Almanach fuer einen
technischen Bereich. Er kodifiziert den erprobten Recherche-Workflow des
Bug-Almanach-Systems (`~/proggs/bugs/SYSTEM.md`).

## Warum es diesen Skill gibt

Bekannte Bugs eines Bereichs VOR der Arbeit nachzuschlagen spart Stunden Debugging
(Poka-Yoke Stufe 3). Aber eine gute Recherche hat zwei Tuecken, die leicht vergessen
werden: **(a)** sie muss breit sein (Doku, Issues, Community, Plattform, Mechanik) und
**(b)** sie muss versionsbewusst sein — ein Bug, der vor drei Versionen gefixt wurde,
darf nicht mehr als aktiv gelten, sonst jagt man Geister. Dieser Skill macht beides
zuverlaessig, jedes Mal gleich.

## Wann der Skill laeuft

- Der Benutzer bittet explizit um eine Bug-Recherche / einen neuen Almanach.
- Das `known-bugs-before-coding`-System trifft auf einen Bereich ohne Almanach und der
  Benutzer gibt sein **OK** (die "erst OK"-Regel — die Recherche kostet Zeit/Tokens,
  also nie ungefragt starten).

## Voraussetzung: Zeiterwartung ansagen

Vor dem Spawnen dem Benutzer kurz sagen, wie lange es dauert (Agent-Zuverlaessigkeit):
"N Researcher parallel, je ~5-10 Minuten. Ich melde sofort, falls einer abstuerzt."
Und: bei einem Researcher-Crash die anderen NICHT abbrechen, sondern den Ausfall
sofort melden und am Ende zusammenfassen, welche erfolgreich waren.

---

## Der Workflow (6 Schritte)

### Schritt 1 — Bereich + Version LIVE ermitteln

Den Bereich benennen und ein kurzes Slug festlegen (z.B. `chrome-extensions`,
`android-compose`, `claude-hooks`, `wpf-csharp`, `gradle`). Dann die **aktuell
installierte Version live** ermitteln — nie raten, nie aus einer Datei nehmen, die
veralten kann:

| Bereich | Versions-Befehl |
|---------|-----------------|
| Claude Code / Hooks | `claude --version` |
| Android / Gradle | `./gradlew --version`, AGP aus `build.gradle`, `compileSdk` |
| Chrome-Erweiterung | `chrome --version` / `msedge --version` |
| TypeScript/Node | `node --version`, `cat package.json` (Dep-Versionen) |
| .NET / WPF | `dotnet --version`, `.csproj` TargetFramework |
| Swift | `swift --version`, `xcodebuild -version` |

Die ermittelte Version ist der Anker fuer Schritt 3 (Fix-Status) und kommt in den
Stand-Header des Almanachs.

### Schritt 2 — Researcher-Schwarm (breite Bug-Suche)

3-5 Researcher PARALLEL spawnen (in EINEM Antwortblock), jeder mit einem eigenen
Teilbereich, damit sie sich nicht ueberschneiden. Die fertigen Prompt-Vorlagen stehen
in `references/researcher-prompts.md` — von dort kopieren und `[BEREICH]`/`[VERSION]`
einsetzen. Standard-Aufteilung:

1. **Offizielle Doku + Changelog** — dokumentierte Bugs, Einschraenkungen, Workarounds.
2. **GitHub-Issues / Bugtracker** — gemeldete Bugs (offen + kuerzlich geschlossen).
3. **Community / Praxis** — Reddit, dev.to, Medium, Blogs, Stack Overflow, HN.
4. **Plattform-Fallen** — Windows UND macOS/Linux (Encoding, Pfade, Permissions, Shell).
5. **Mechanik / bereichsspezifisch** — die typischen konzeptionellen Fehler des Bereichs.

Bei kleinen/eng umrissenen Bereichen reichen 3 Researcher (Doku, Issues, Community).
Bei breiten Bereichen 5.

**Pflicht-Limits pro Researcher** (gegen Absturz, siehe `agent-and-researcher-rules.md`
+ `subagent-crash-proofing.md`): max 15 Web-Fetches, max 10 Minuten, max ~40 Eintraege,
KOMPAKTE strukturierte Rueckgabe (~1500-2000 Token, kein Rohdaten-Dump). Subagenten
laufen automatisch auf dem hoechsten Opus-Modell (Modell-Policy) — `opts.model` nicht
setzen.

Jeder Researcher gibt pro Bug zurueck: **Titel · Symptom · Ursache · Loesung
(funktionserhaltend!) · betroffene Versionen · Quelle (URL)**.

### Schritt 3 — Fix-Status-Recherche (der Kern: was ist schon gefixt?)

Das ist der Schritt, der am leichtesten vergessen wird und am wichtigsten ist. NACH der
breiten Suche eine SEPARATE, gezielte Recherche, die fragt: **Welche der gefundenen
Bugs sind in neueren Versionen (bis zur in Schritt 1 ermittelten installierten Version)
bereits behoben?**

2-3 Researcher parallel:
- **Changelog/Release-Notes** der Versionen seit dem aeltesten relevanten Bug bis zur
  installierten Version durchgehen — welche Bug-Fixes betreffen den Bereich?
- **Issue-Status** der konkreten gefundenen Bug-Tickets pruefen: geschlossen? gefixt in
  welcher Version? (Falls der Bugtracker per WebFetch blockiert ist — z.B. GitHub —
  ehrlich vermerken und nur Such-Snippets/Changelog als Beleg nehmen.)
- **Sekundaerquellen** als Gegenprobe (Release-Zusammenfassungen, Blogs).

**Ehrlichkeits-Pflicht:** Strikt trennen zwischen *Changelog-belegt gefixt* (zuverlaessig)
und *Status unklar / kein Fix gefunden* (vorsichtig behandeln). Niemals einen Bug als
"gefixt" markieren, ohne Beleg — im Zweifel bleibt er "noch offen".

### Schritt 4 — Kuratieren in `~/proggs/bugs/<bereich>.md`

Die Researcher-Ergebnisse DEDUPLIZIEREN (gleiche Bugs erscheinen oft bei mehreren
Researchern — das bestaetigt sie, aber nur EINEN Eintrag schreiben) und nach Themen
gruppieren. Format pro Eintrag (konsistent mit `~/proggs/bugs/SYSTEM.md`):

```
## N. <Bug-Titel>   [⭐ HAEUFIG falls oft genannt]
**Symptom:** Was man sieht.
**Ursache:** Der wahre Grund.
**Versionen:** betrifft V1-V3, gefixt ab V4 — oder "per Design" / "unabhaengig".
**FIX:** Beste funktionserhaltende Loesung (NIE "Feature weglassen" als Fix).
**Quelle:** URL / eigener Vorfall.
```

Pflicht-Bestandteile der Datei:
- **Header** mit Pflicht-Lese-Hinweis + **Stand**-Vermerk ("recherchiert am DATUM fuer
  Version V").
- **TL;DR** der 3-5 wichtigsten Regeln ganz oben.
- Die Bug-Eintraege, thematisch gruppiert.
- **Eigene "Fix-Status"-Sektion** (aus Schritt 3): eine Tabelle "Frueherer Bug | gefixt
  ab | Bezug" PLUS eine Liste "noch NICHT gefixt (Workaround bleibt aktiv)". Dazu der
  Ehrlichkeits-Hinweis zur Methodik (was Changelog-belegt war, was unklar blieb).
- **Pflicht-Checkliste** am Ende.

Wenn der Bereich fehleranfaellige Plattform-Unterschiede hat (Windows vs. macOS): je
eine eigene Sektion. Echte deutsche Umlaute verwenden (Doku-Regel) — AUSSER in Strings,
die als Hook-stdout auf Windows ausgegeben werden (dort ASCII wegen cp1252).

### Schritt 5 — Ins System einhaengen

1. **`~/proggs/bugs/README.md`**: den Bereich aus "Bereiche ohne Almanach" nach
   "Vorhandene Almanache" verschieben (mit Stand, Bug-Anzahl, Erkennungs-Triggern).
2. **`~/.claude/hooks/bug-almanac-guard.{ps1,sh}`**: pruefen, ob das Pfad-Mapping den
   Bereich schon kennt; falls nicht, das Dateimuster → `<bereich>.md` ergaenzen (in
   BEIDEN Varianten, ps1 + sh). Gegenstueck auch in `claude-code-setup/hooks/` spiegeln.

### Schritt 6 — Committen + pushen

Den Almanach + README (+ ggf. die Hook-Aenderung) committen und pushen
(fortlaufende #-Nummer). Bei Hook-Aenderung: Cross-Platform-Sync nicht vergessen.

---

## Nach der Recherche: Wartung

- Jeder spaeter SELBST erlebte Bug des Bereichs wird als Eintrag ergaenzt (Bug +
  funktionserhaltende Loesung + Versionen), Stand-Header aktualisiert.
- Bei einem deutlichen Versionssprung der benutzten Software: kurzer Re-Check (Schritt
  1+3), ob alte Bugs noch gelten und neue dazukamen — mit dem OK des Benutzers.

## Was NIEMALS passieren darf

- Einen Bug als "gefixt" markieren ohne Beleg (Schritt 3 ehrlich halten).
- Den Fix-Status-Schritt ueberspringen — dann jagt man Geister auf der aktuellen Version.
- Eine Loesung notieren, die Funktionalitaet entfernt (Direktive #3 — funktionserhaltend).
- Researcher ohne Limits spawnen (Absturzgefahr) oder einen Crash verschweigen.
- Rohdaten-Dumps der Researcher 1:1 uebernehmen statt zu deduplizieren und zu kuratieren.

## Referenzen

- `references/researcher-prompts.md` — fertige Prompt-Vorlagen fuer Schritt 2 + 3.
- `~/proggs/bugs/SYSTEM.md` — das uebergeordnete System (Ordner, Hooks, Format).
- `~/.claude/rules/known-bugs-before-coding.md` — die Regel, die diesen Skill ausloest.
