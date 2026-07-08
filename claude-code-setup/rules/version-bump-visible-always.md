# Versionszaehler immer hochzaehlen — und SICHTBAR anzeigen (KRITISCH)

> Gilt fuer ALLE Software-Projekte/Sprachen (Android, .NET/WPF, Swift, TS/Node, Rust, Go,
> Chrome-Erweiterungen, CLIs). Zweck: Frank will beim Benutzen sofort erkennen, DASS ein Update angekommen ist.

## Grundprinzip

Bei jeder Aenderung an einer Software: Version hochzaehlen, Erhoehungszeitpunkt speichern, UND im
laufenden Programm sichtbar machen. Pflichtformat: `Version 1.2.3 (27.06.2026, 14.35 Uhr)` —
Versionsnummer, dann direkt dahinter Datum + Uhrzeit der Erhoehung in Klammern (echte Uhr, nie schaetzen).

## Wann hochzaehlen

JA: Bugfix (Patch), neues Feature (Minor), UI/Verhalten (Patch/Minor), Performance (Patch), grosser
Umbau/Breaking (Major). NEIN: reine Doku/README, Harness-Datei (hat `#NNN`-Commits), Wegwerf-Skript.
Im Zweifel hochzaehlen. Android zusaetzlich: `versionCode` (Integer) bei JEDEM Release +1 (sonst Play-Store-Reject).

## Wo die Version steht + sichtbar ist

Quelle der Wahrheit = sichtbare Anzeige aus der Quelle ableiten (nicht doppelt hardcoden):

| Plattform | Quelle | Sichtbar aus |
|-----------|--------|--------------|
| Android | `build.gradle.kts` versionName/Code | `BuildConfig.VERSION_NAME` (Settings/Ueber/Fuss) |
| .NET/WPF | `.csproj` `<Version>` | `Assembly…Version` (Titelleiste/Ueber) |
| Swift/macOS | `Info.plist` CFBundleShortVersionString | Bundle (Ueber/Settings) |
| TS/Node | `package.json` version | `--version`/Footer |
| Rust | `Cargo.toml` version | `env!("CARGO_PKG_VERSION")` |
| Chrome-Ext | `manifest.json` version | `chrome.runtime.getManifest().version` |

Bevorzugt dauerhaft sichtbar (Titelleiste/Fusszeile/CLI-Banner). Bei Multi-Task: EIN Bump pro
ausgeliefertem Build (nicht pro Mini-Edit). Beim Start Version einmal ins Log (`version=<x.y.z>`).

## Was NIEMALS passieren darf

- Software aendern und Version NICHT hochzaehlen · Version intern erhoehen aber nicht sichtbar machen
- Datum/Uhrzeit weglassen oder nicht im Format `(27.06.2026, 14.35 Uhr)` · App ohne jede sichtbare Version
- Android-`versionCode` beim Release vergessen · sichtbare Version hardcoden die von der Quelle abweicht
