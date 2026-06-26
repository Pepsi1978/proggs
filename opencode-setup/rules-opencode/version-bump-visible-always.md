Versionszähler immer hochzählen — und SICHTBAR

Jedes Mal, wenn eine Software aktualisiert/veraendert/verbessert wird: Versionsnummer hochzaehlen UND die Version MUSS im laufenden Programm SICHTBAR sein (damit man sofort sieht, dass ein Update angekommen ist). Eine intern erhoehte, aber unsichtbare Version erfuellt den Zweck NICHT. Gibt es noch keine sichtbare Anzeige -> eine einbauen.

## Wann hochzaehlen
JA: Bugfix (auch klein) = Patch; neues Feature = Minor; UI-/Verhaltens-Verbesserung = Patch/Minor; Performance = Patch; grosser Umbau/Breaking = Major.
NEIN: reine Doku/README, Harness-Datei (Regel/Hook/Skill/Memory, hat Commit-Nummern), Wegwerf-Skript.
Im Zweifel: hochzaehlen.

## SemVer (MAJOR.MINOR.PATCH)
PATCH = Bugfix/Polish; MINOR = neues Feature (abwaertskompatibel); MAJOR = Breaking. Android zusaetzlich: `versionCode` (Integer) bei JEDEM Release +1 (sonst lehnt der Play Store den Upload ab).

## Wo die Version steht + sichtbar (aus der Quelle ableiten, nicht doppelt hardcoden)
- Android: `build.gradle.kts` (versionName/versionCode) -> Settings/Ueber-Screen, aus `BuildConfig.VERSION_NAME`.
- .NET/WPF: `.csproj` `<Version>` -> Titelleiste/Ueber-Dialog.
- Swift/macOS: `Info.plist` CFBundleShortVersionString -> Ueber-Fenster.
- TypeScript/Node: `package.json` version -> CLI `--version`/Footer.
- Rust: `Cargo.toml` version -> CLI-Banner (`env!("CARGO_PKG_VERSION")`).
- Chrome-Erweiterung: `manifest.json` version -> Popup-Fuss.
Bevorzugt dauerhaft sichtbar (Titelleiste/Fusszeile/CLI-Banner), nicht nur tief versteckt.

## Reihenfolge
Code fertig -> Version-Bump in der Quelle -> sichtbare Anzeige pruefen (zeigt sie die NEUE Version?) -> commit+push -> bauen/deployen. Bei Multi-Task: EIN Bump pro ausgeliefertem Build (nicht pro Teilaufgabe).

## NIEMALS
- Software aendern und Version NICHT hochzaehlen.
- Version intern erhoehen, aber im Programm NICHT sichtbar machen.
- Eine App ohne JEDE sichtbare Versionsanzeige lassen.
- Android-`versionCode` beim Release vergessen (+1 Pflicht).
- Sichtbare Version hardcoden, die von der echten Quelle abweicht.
