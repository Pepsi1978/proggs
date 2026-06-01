# 🐛 INHALTSVERZEICHNIS BUGS

> **Der zentrale Bug-Almanach pro Technologie-Bereich.**
> Hier liegt fuer jeden Bereich, an dem gearbeitet wird, eine eigene `.md`-Datei mit
> den oeffentlich bekannten Bugs/Fallen **und ihren bewaehrten, funktionserhaltenden
> Loesungen**. Ziel: Bekannte Fehler werden VOR der Arbeit nachgeschlagen, statt
> hinterher teuer debuggt (Poka-Yoke Stufe 3). Das vollstaendige Systemverhalten
> steht in [`SYSTEM.md`](SYSTEM.md).

---

## So funktioniert es (Kurzfassung)

1. **Vor** echter Arbeit an einem technischen Bereich: pruefen, ob es hier einen
   Almanach fuer den Bereich gibt (diese Liste).
2. **Almanach vorhanden** → komplett lesen, Versionen abgleichen, DANN arbeiten.
3. **Kein Almanach** → Frank Bescheid geben, auf sein **OK** warten, dann die
   bekannten Bugs des Bereichs recherchieren und hier einen neuen Almanach anlegen.
4. **Neuen Bug erlebt** → in den passenden Almanach eintragen (Bug + Loesung + Version).

Drei Automatik-Schichten sorgen dafuer, dass das in **jeder** Session laeuft:
Session-Hook (`bug-almanac-index`) blendet diese Liste beim Start ein · Datei-Hook
(`bug-almanac-guard`) erinnert beim Anfassen bereichstypischer Dateien an den Almanach ·
Regel `known-bugs-before-coding.md` als Verhaltensschicht.

---

## ✅ Vorhandene Almanache

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Browser-Erweiterungen** (Chrome/Edge, MV3) | [`chrome-extensions.md`](chrome-extensions.md) | 2026-06-01 | 9 | `manifest.json` (mit `manifest_version`), `background.js`, `service-worker.js`, `*/overlays/*`, `chrome.*`-APIs · „Erweiterung", „Extension", „Overlay" |

---

## ⬜ Bereiche ohne Almanach (bei erster echter Arbeit: recherchieren — erst Franks OK)

> Diese Liste ist die Landkarte der erwarteten Bereiche. Sobald an einem davon
> echte Arbeit beginnt und noch kein Almanach existiert, wird er angelegt.

| Bereich | (geplante Datei) | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|------------------|-------------------------------------------|
| **Android — Kotlin / Compose** | `android-compose.md` | `*.kt`, `*.kts`, `AndroidManifest.xml`, `build.gradle*` · „Compose", „Android", „Jetpack" |
| **Windows-Desktop — C# / WPF / WinUI** | `wpf-csharp.md` | `*.xaml`, `*.cs`, `*.csproj` · „WPF", „WinUI", „.NET" |
| **macOS-Desktop — Swift / AppKit** | `swift-appkit.md` | `*.swift`, `*.xcodeproj`, `Info.plist` · „Swift", „AppKit", „SwiftUI" |
| **Web — TypeScript / Node** | `typescript-node.md` | `*.ts`, `*.tsx`, `package.json`, `tsconfig.json` · „TypeScript", „Node", „npm" |
| **Tampermonkey / Userscripts** | `tampermonkey.md` | `*.user.js` · „Tampermonkey", „Userscript", „Greasemonkey" |
| **Claude-Harness — Hooks (PowerShell/Bash)** | `claude-hooks.md` | `~/.claude/hooks/*.ps1`, `*.sh` · „Hook", „SessionStart", „PreToolUse" |
| **Build — Gradle** | `gradle.md` | `build.gradle*`, `settings.gradle*`, `gradle/*` · „Gradle", „AGP", „R8" |

(Liste waechst mit. Neue Bereiche hier ergaenzen, sobald sie auftauchen — und das
Pfad-Mapping im `bug-almanac-guard`-Hook entsprechend nachziehen.)

---

## Aufbau jeder Almanach-Datei (Format-Vorlage)

```
# Bekannte Bugs: <Thema>
> PFLICHT-LESEN vor Arbeit an <Thema>.
> Stand: zuletzt recherchiert am <Datum> fuer Version <X>.

## N. <Bug-Titel>   [⭐ HAEUFIG falls oft]
Symptom:    Was man sieht
Ursache:    Der wahre Grund
Versionen:  betrifft V1-V3, gefixt ab V4   (oder „per Design / unabhaengig")
FIX:        Beste funktionserhaltende Loesung (NIE „Feature weg")
Quelle:     Link / eigener Vorfall
```
