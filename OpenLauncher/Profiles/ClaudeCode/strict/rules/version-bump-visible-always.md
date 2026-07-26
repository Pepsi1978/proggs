# Versionszaehler immer hochzaehlen — und SICHTBAR anzeigen (KRITISCH)

> Fuer ALLE Software/Sprachen. Zweck: Frank soll sofort sehen, DASS ein Update ankam.

## Grundprinzip
Bei jeder Aenderung: Version hochzaehlen, Zeitpunkt speichern, im Programm sichtbar machen.
Format: `Version 1.2.3 (27.06.2026, 14.35 Uhr)` — Nummer + Datum/Uhrzeit (echte Uhr, nie schaetzen).

## Wann hochzaehlen
JA: Bugfix (Patch), Feature (Minor), UI/Verhalten (Patch/Minor), Performance (Patch), Breaking (Major).
NEIN: reine Doku/README, Harness-Datei (hat `#NNN`-Commits), Wegwerf-Skript. Im Zweifel hochzaehlen.
Android zusaetzlich: `versionCode` (Integer) bei JEDEM Release +1 (sonst Play-Store-Reject).

## Wo die Version steht + sichtbar ist
Sichtbare Anzeige aus der Quelle ableiten (nicht doppelt hardcoden): Android `build.gradle.kts` →
`BuildConfig.VERSION_NAME` · .NET `.csproj` → Titelleiste · Swift `Info.plist` → Ueber · TS
`package.json` → `--version`/Footer · Rust `Cargo.toml` → `env!("CARGO_PKG_VERSION")` · Chrome
`manifest.json` → `chrome.runtime.getManifest().version`. Bevorzugt dauerhaft sichtbar. Multi-Task:
EIN Bump pro Build. Beim Start Version ins Log.

## Was NIEMALS
- Software aendern ohne Version hochzaehlen · Version erhoehen aber nicht sichtbar machen · Datum/Uhrzeit
  weglassen oder falsches Format · Android-`versionCode` beim Release vergessen · sichtbare Version
  hardcoden die von der Quelle abweicht.
