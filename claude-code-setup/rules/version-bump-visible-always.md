# Versionszähler immer hochzählen — und SICHTBAR anzeigen (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-09. Gilt AUTOMATISCH in JEDER Session,
> auf ALLEN Plattformen, fuer ALLE Software-Projekte und ALLE Sprachen (Android, .NET/WPF,
> Swift, TypeScript/Node, Rust, Go, Chrome-Erweiterungen, CLIs …). Der Benutzer muss diese
> Regel NICHT erwaehnen — sie ist Standard.
>
> Repo-Spiegelung von `~/.claude/rules/version-bump-visible-always.md` (Cross-Platform-Sync).

---

## Grundprinzip

**Jedes Mal, wenn eine Software aktualisiert, veraendert oder verbessert wird, wird die
Versionsnummer hochgezaehlt — und die Version MUSS im laufenden Programm SICHTBAR sein.
Direkt hinter der Version MUSS sichtbar Datum + Uhrzeit stehen, wann diese Version erhoeht wurde.**

Der Zweck (Franks Wortlaut sinngemaess): Er will beim Benutzen der Software sofort erkennen,
**dass ein Update auch angekommen ist**. Eine intern erhoehte Version oder ein intern gespeicherter
Erhoehungszeitpunkt, die man im Programm nicht sieht, erfuellen diesen Zweck NICHT.

Zwei untrennbare Teile:

1. **Hochzaehlen** — bei jeder Code-Aenderung an einer Software die Version erhoehen.
2. **Zeitpunkt speichern** — Datum + Uhrzeit der Versionserhoehung speichern.
3. **Sichtbar machen** — die Version plus Erhoehungszeitpunkt an einer Stelle anzeigen, die der Benutzer beim normalen
   Benutzen sieht oder leicht findet. Gibt es noch KEINE sichtbare Versionsanzeige → eine einbauen.

Pflichtformat: `Version 1.2.3 (27.06.2026, 14.35 Uhr)`. Wichtig ist die Reihenfolge:
Versionsnummer, Leerzeichen, dann direkt dahinter Datum + Uhrzeit der Versionserhoehung in Klammern.

---

## Wann hochzaehlen (Geltungsbereich)

Hochzaehlen bei JEDER Aenderung an einer Software, die der Benutzer benutzt, ausliefert oder
ueber Zeit pflegt:

| Aenderung | Version hochzaehlen? |
|-----------|----------------------|
| Bugfix (auch klein) | **JA** — Patch |
| Neues Feature / neue Funktion | **JA** — Minor |
| UI-/Verhaltens-Verbesserung | **JA** — Patch oder Minor |
| Performance-Optimierung | **JA** — Patch |
| Grosse Umstrukturierung / Breaking Change | **JA** — Major |
| Reine Doku/README einer App (kein Code) | NEIN |
| Harness-Datei (Regel, Hook, Skill, Memory) | NEIN (haben Commit-Nummern #NNN) |
| Einmaliges Wegwerf-Skript | NEIN |

Im Zweifel: hochzaehlen. Lieber ein Patch zu viel als ein verpasstes "Update angekommen?".

---

## Welche Stufe (SemVer: MAJOR.MINOR.PATCH)

- **PATCH** (`x.y.Z+1`): Bugfix, kleine Verbesserung, Polish, Performance — nichts Neues sichtbar.
- **MINOR** (`x.Y+1.0`): neues Feature / neue Funktion, abwaertskompatibel.
- **MAJOR** (`X+1.0.0`): grosser Umbau, Breaking Change, neue Hauptversion.

Android zusaetzlich: `versionCode` (Integer) bei JEDEM Release **+1** — sonst lehnt der Play
Store den Upload ab. `versionName` folgt SemVer.

---

## Wo die Version steht UND wo sie SICHTBAR ist (pro Plattform)

Die "Quelle der Wahrheit" (links) muss mit der sichtbaren Anzeige (rechts) uebereinstimmen —
am besten die Anzeige aus der Quelle ableiten (BuildConfig/Assembly/Manifest), nicht doppelt
hardcoden. Der Zeitpunkt der Versionserhoehung muss zusammen mit der Version gespeichert und
direkt hinter der Version sichtbar gemacht werden.

| Plattform | Quelle der Versionsnummer | Sichtbar machen (mind. eine Stelle) |
|-----------|---------------------------|-------------------------------------|
| Android (Kotlin/Compose) | `build.gradle.kts` → `versionName` + `versionCode` | Settings/Über-Screen, App-Fuss oder Splash; aus `BuildConfig.VERSION_NAME` |
| .NET / WPF (C#) | `.csproj` → `<Version>` / `AssemblyInformationalVersion` | Titelleiste, Über-Dialog oder Settings-Fuss; aus `Assembly...Version` |
| Swift / AppKit (macOS) | `Info.plist` → `CFBundleShortVersionString` (+ `CFBundleVersion`) | Über-Fenster / Settings; aus Bundle |
| TypeScript / Node | `package.json` → `version` | CLI `--version`, UI-Footer oder About; aus `package.json` |
| Rust | `Cargo.toml` → `version` | CLI `--version` (Banner); aus `env!("CARGO_PKG_VERSION")` |
| Go | Version-Konstante / `-ldflags -X` | CLI `--version`; im Build injiziert |
| Chrome-Erweiterung | `manifest.json` → `version` | Popup-Fuss oder Optionsseite; aus `chrome.runtime.getManifest().version` |

**Bevorzugt dauerhaft sichtbar** (Titelleiste, Fusszeile, CLI-Banner) statt nur in einem tief
versteckten Dialog — so sieht Frank die Version, ohne suchen zu muessen. Anzeige immer als
Version + Erhoehungszeitpunkt direkt dahinter in Klammern, z.B. `Version 1.2.3 (27.06.2026, 14.35 Uhr)`.

---

## Reihenfolge (passt zu commit-before-build)

1. Code-Aenderung fertig.
2. **Version-Bump** in der Quelle (Schritt 2 in `commit-before-build.md`).
3. **Datum + Uhrzeit der Versionserhoehung speichern**.
4. Sichtbare Anzeige pruefen/ergaenzen (zeigt sie die NEUE Version UND den Erhoehungszeitpunkt?).
5. `git add` (eigene Dateien) → `git commit` (`#NNN`) → fetch+rebase → `git push`.
6. ERST DANN bauen → installieren/deployen.

Bei Multi-Task-Sessions (mehrere Aufgaben an einer App): **ein** Version-Bump pro
ausgeliefertem Build reicht — am Ende vor dem einen gemeinsamen Build (siehe
`semicolon-task-separator.md`, Schritt 5). Nicht pro Mini-Edit drei Stellen weiter zaehlen.

---

## Zusammenspiel mit Observability-First

Beim Programmstart die Version EINMAL ins strukturierte Log schreiben (neben dem Log-Pfad,
siehe `observability-first.md`): `version=<x.y.z>`. So ist im Log nachvollziehbar, welcher
Stand lief — die zweite Sichtbarkeits-Ebene neben der UI.

---

## Was NIEMALS passieren darf

- ❌ Eine Software aendern/verbessern und die Version NICHT hochzaehlen
- ❌ Version intern erhoehen, aber im Programm NICHT sichtbar machen (verfehlt den Zweck)
- ❌ Datum/Uhrzeit der Versionserhoehung weglassen, nicht direkt hinter der sichtbaren Version anzeigen oder nicht im Format `(27.06.2026, 14.35 Uhr)` schreiben
- ❌ Eine App ohne JEDE sichtbare Versionsanzeige lassen — dann eine einbauen
- ❌ Android-`versionCode` beim Release vergessen (+1 ist Pflicht, sonst Play-Store-Reject)
- ❌ Sichtbare Version hardcoden, die von der echten Quelle abweicht (immer ableiten)

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `commit-before-build.md` | Version-Bump ist dort Schritt 2 — diese Regel macht ihn zur Pflicht UND verlangt Sichtbarkeit |
| `git-workflow.md` (edit-then-version-bump) | Bump atomar mit dem Edit; hier zusaetzlich die sichtbare Anzeige |
| `semicolon-task-separator.md` | Bei Multi-Task: ein Bump pro Build, nicht pro Teilaufgabe |
| `observability-first.md` | Version beim Start ins Log (zweite Sichtbarkeits-Ebene) |
