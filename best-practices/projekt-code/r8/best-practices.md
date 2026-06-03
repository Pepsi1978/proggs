# R8 — Best Practices (Stand 2026-06-03, AGP 8.7.3 / R8 8.7.x + AGP 8.10.0 / R8 8.10.x)

Die „so-macht-man-es-richtig"-Seite zum Bug-Almanach `~/proggs/bugs/r8.md`. R8 ist Androids
Code-Shrinker/Optimizer/Obfuscator (mit AGP gebündelt, Full-Mode Default seit AGP 8.0). Ziel:
R8 so einführen, dass die typischen „läuft in Debug, bricht in Release"-Bugs **gar nicht erst
entstehen**. Quellen-Rangordnung: offizielle Google/Android-Doku = Grundwahrheit; Blogs = `extern`.

---

## A — Inkrementell einführen + Diagnose-Workflow (offiziell)

- **R8 NIE komplett abschalten.** `minifyEnabled false` ist kein Fix, sondern Funktionsverlust
  (größeres/langsameres APK, kein Obfuscation-Schutz). Stattdessen Root-Cause (fehlende keep-Regel) fixen.
- **Schrittweise aktivieren** (Google „Adopt optimizations incrementally"): erst shrinking, dann
  obfuscation, dann optimization — nach jedem Schritt Release auf echtem Gerät testen.
- **Immer die R8-Output-Dateien lesen** (`app/build/outputs/mapping/release/`): `missing_rules.txt`,
  `usage.txt`, `seeds.txt`, `configuration.txt`, `resources.txt`. `-printconfiguration` für die
  gemergten Regeln. Das ist der schnellste Weg zur Ursache — nie raten.
- **Release immer auf echtem Gerät testen** — ein grüner Debug-Build beweist nichts über R8.
  *(offiziell)* [Adopt optimizations incrementally](https://developer.android.com/topic/performance/app-optimization/adopt-optimizations-incrementally)

## B — Reflection/Serialisierung: consumer-rules zuerst, eigene Modelle selbst halten

- **Aktuelle Lib-Versionen nutzen** — sie bringen R8-Full-Mode-Regeln mit: Gson **2.11.0+**,
  Retrofit **2.9.0+**, kotlinx.serialization **1.6.0+**, Room, WorkManager. Erspart eigene Regeln.
- **Moshi/kotlinx codegen statt reflective** — vermeidet die fragilsten keep-Regeln komplett
  (`@JsonClass(generateAdapter=true)` generiert Adapter zur Compile-Zeit, keine Reflection auf Modelle).
- **Eigene Modellklassen sind nie von Lib-Regeln abgedeckt.** Annotation-gesteuert halten:
  `-keepclassmembers,allowobfuscation class * { @com.google.gson.annotations.SerializedName <fields>; }`.
- **`-keepattributes Signature`** setzen, sobald irgendwo generische Reflection (Gson TypeToken,
  Retrofit-Returns) läuft. *(offiziell)* R8 compatibility FAQ.

## C — keep-Regeln eng halten

- So eng wie möglich: Member-Level statt `class lib.** { *; }`. `allowobfuscation`/`allowshrinking`/
  `allowoptimization` ergänzen, wo nur die Existenz (nicht der Name) gebraucht wird.
- **Keine Negation `!`** in keep-Regeln (invertiert die Absicht).
- Keine redundanten Regeln für Manifest-Komponenten (Activities/Services etc. sind in
  `proguard-android-optimize.txt` abgedeckt).
- `-whyareyoukeeping` / `usage.txt` nutzen, um zu breite Regeln zu erkennen.
  *(offiziell)* [Configure and troubleshoot R8 keep rules (Nov 2025)](https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html)

## D — Resource-Shrinking absichern

- `isShrinkResources = true` immer **zusammen mit** `isMinifyEnabled = true`.
- Dynamisch (per `getIdentifier()`) geladene Ressourcen in `res/raw/<package>.keep.xml` mit
  `tools:keep` + `xmlns:tools`-Namespace sichern.
- `safe`-Modus (Default) beibehalten, `strict` nur mit vollständiger `tools:keep`-Liste.
- `resConfigs`/`resourceConfigurations` nur setzen, wenn ALLE unterstützten Locales gelistet sind
  (sonst werden Übersetzungen weggeworfen).
- Vor AGP-9.0-Upgrade (automatisches `optimizedResourceShrinking`) Release gründlich testen.
  *(offiziell)* [Customize which resources to keep](https://developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep)

## E — Logging/Optimierung sicher steuern

- `-assumenosideeffects` nur mit **exakten Methoden-Signaturen**, NIE `{ *; }`-Wildcard (sonst
  werden `wait/notify/synchronized` weg-optimiert → Races). *(extern, Jake Wharton — gut belegt)*
- `proguard-android-optimize.txt` als Default verwenden (nicht `proguard-android.txt`, entfällt ab AGP 9.0).

## F — Deobfuskation & Crash-Reporting

- `mapping.txt` **pro veröffentlichter Version** sichern/hochladen (wird bei jedem Build überschrieben).
- `-keepattributes SourceFile,LineNumberTable` für eindeutige Stacktraces.
- Crashlytics-Gradle-Plugin **v3.0.0+** (lädt Mapping automatisch hoch; ältere inkompatibel mit AGP 8.1+).
  *(offiziell)* [Get deobfuscated reports](https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports)

## G — Versions-Hygiene & AGP-Upgrades

- **AGP-Patch-Versionen aktuell halten** — viele R8-Miscompilation-Bugs werden in Patch-Releases
  gefixt (siehe `bugs/r8.md` Abschnitt O). Konkret für die Projekte: **AGP 8.7.3 trägt die
  SPI/ServiceLoader-Regression #389737060** (gefixt erst 8.9.1) — bei `ServiceLoader`-Nutzung auf 8.9.1+ heben.
- Bei AGP-Upgrade beachten: missing-class = Fehler (8.0), `proguard-android-optimize.txt` (9.0),
  `-keepattributes` explizite RuntimeInvisible-Liste (9.2).
- Library-Module: kein `minifyEnabled` (ab 8.4 verboten), Regeln per `consumerProguardFiles`, keine
  globalen Opt-Flags in consumer-rules.

> **Hinweis:** Ergänzt `projekt-code/gradle/best-practices.md` (Build-System-Sicht). Diese Datei
> fokussiert R8 selbst (Shrinking/Obfuscation/Optimization + keep-Regeln).

---

### Best-Practice-Abschnitt ↔ Bug-Abschnitt (wechselseitig zu `bugs/r8.md`)
| Best-Practice (hier) | Bug-Almanach (`bugs/r8.md`) |
|----------------------|-----------------------------|
| A Inkrementell + Diagnose | A Full-Mode / B Diagnose-Workflow |
| B consumer-rules zuerst | C Library-keep-Regeln / D Kotlin |
| C keep eng halten | I keep Anti-Patterns |
| D Resource-Shrinking | G Resource-Shrinking |
| E Logging/Optimierung | H `-assumenosideeffects`-Fallen |
| F Deobfuskation | N mapping.txt / F4 Crashlytics |
| G Versions-Hygiene | O Fix-Status / J Versions-Bugs / M Upgrade-Breaking |
