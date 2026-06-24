# Bekannte Bugs: Android-Build-System (Gradle / AGP / R8·ProGuard / KSP)

> **PFLICHT-LESEN vor Arbeit an Build-Dateien** (`build.gradle(.kts)`, `settings.gradle(.kts)`,
> `gradle.properties`, `gradle/*`, `gradle-wrapper.properties`, Version-Catalog `libs.versions.toml`).
> Es geht hier um die **Build-Infrastruktur** — NICHT um die Kotlin-Sprache.
>
> **Stand:** recherchiert am 2026-06-02, **re-recherchiert am 2026-06-24** (Engine A: Firecrawl+MiniMax)
> fuer den real benutzten Stand der beiden Android-Projekte
> **Anker:** gradle=8.11.1  <!-- maschinenlesbar fuer check-version-anchor.py -->
> (live ermittelt):
>
> | | BestJournalAndroid (Play Store) | EntropieReductor (privat) |
> |---|---|---|
> | Gradle (Wrapper) | **8.11.1** | **8.11.1** |
> | AGP | **8.7.3** | **8.10.0** |
> | Kotlin / KSP | 2.1.0 / **2.1.0-1.0.29** | 2.1.0 / **2.1.0-1.0.29** |
> | JDK (Daemon) | **21** (Temurin 21.0.10) | 21 |
> | compileSdk / minSdk / targetSdk | 36 / 26 / 36 | 36 / 28 / 36 |
> | R8 (minify+shrink) Release | an | an |
> | gradle.properties Build-Flags | minimal | `parallel`, `caching`, `configuration-cache=false` (bewusst), `ksp.incremental=true` |
>
> **Upgrade-Pfad im Blick behalten:** AGP 9.0/9.1 + Gradle 9.x + Kotlin 2.3 (Memory
> `project_agp9_kotlin23_upgrade`). Viele Eintraege unten sind genau fuer diesen Sprung relevant.
> Bei einem Versionssprung der benutzten Tools: kurzer Re-Check (mit Franks OK).
>
> **Versions-Horizont (Stand 2026-06-24, Re-Recherche):** Die Toolchain ist inzwischen weit ueber
> euren gepinnten Stand hinaus: **Gradle 9.6.0** (released 2026-06-20), **AGP 9.2.0** (April 2026),
> **KSP 2.3.9** (2026-05-26, 2.3.10 in Vorbereitung), Kotlin-Linie bei 2.3/2.4. Euer Pin (Gradle
> 8.11.1, AGP 8.7.3/8.10.0, Kotlin/KSP 2.1.0) bleibt gueltig — der Anker ist projekt-gepinnt,
> kein Live-Abgleich. Die §2/§4/§7/§8-Eintraege zum 9er-Sprung sind damit voll aktuell relevant.

---

## ⚠️ Abgrenzung zu `kotlin.md` (NICHT verwechseln)

Beide Almanache beruehren das Build-System. Die Trennung:

| Gehoert in **`gradle.md`** (diese Datei) | Gehoert in **`kotlin.md`** (§10) |
|------------------------------------------|----------------------------------|
| Gradle-Core, Daemon, Config-/Build-Cache, Wrapper | `kotlinOptions {}` → `compilerOptions {}` (Sprach-DSL) |
| AGP-Versionsmatrix + AGP-9.0-Breaking-Changes | KSP-Suffix **an die Kotlin-Version** koppeln (`2.1.0-1.0.29`) |
| R8/ProGuard-**Mechanik** (fullMode, mapping, shrinking, dexing) | R8-keep-Rules speziell fuer **kotlinx.serialization** |
| KSP als **Gradle-Plugin** (Apply-Order, useKSP2, incremental, Cache) | KAPT-K2-Modus (`kapt.use.k2`) |
| Dependency-Resolution, Version-Catalogs, BOM-Mechanik | Compose-Compiler-Plugin als Sprach-Feature |

Bei Ueberlappung steht der Eintrag dort, wo seine **Ursache** liegt, und verweist auf das
Gegenstueck. Konkret referenziert: `kotlin.md` §10.1 (KSP↔Kotlin), §10.3 (R8 + serialization),
§10.7 (`proguard-android.txt`-Wegfall — dort sprachseitig, hier §2.3 build-seitig).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `org.gradle.jvmargs` gesetzt | `-XX:MaxMetaspaceSize` mitsetzen, sonst Daemon-Crash | §6.1 |
| 2 | AGP-Bump geplant | Erst Matrix pruefen: Mindest-Gradle + JDK | §1, §1.1 |
| 3 | AGP-9.0-Upgrade | Als EIN Block planen (neue DSL, ProGuard, Variant-API) | §2 |
| 4 | "ClassCastException ... BaseExtension" | Build-Logic auf Public-DSL + androidComponents | §2.1 |
| 5 | keep-Regel haelt Ctor nicht (AGP 9) | Regel praezisieren: `-keep class A { <init>(); }` | §2.4 |
| 6 | Release/Play-Store-Crash, Debug ok | R8: keep-Rules eng, dynamische Res schuetzen | §7 |
| 7 | Gson/Moshi verliert generische Typen | `-keepattributes Signature,InnerClasses,EnclosingMethod` | §7.1 |
| 8 | Per Name geladene Ressource fehlt (AGP 9) | `res/raw/keep.xml` mit `tools:keep=...` | §7.5 |
| 9 | "Missing classes ... running R8" | Regeln aus `missing_rules.txt`, KEIN `-dontwarn`-Flood | §7.4 |
| 10 | "stale generated code" / KSP-Output fehlt | Reflexartig `clean` (KSP-2.1.0-1.0.29 betroffen) | §8.2 |
| 11 | Kotlin 2.3 / AGP 9.0 Upgrade | `ksp.useKSP2=true` im selben Schnitt | §8.1 |
| 12 | BOM-Lib ohne Version unaufloesbar | `platform(...)` + BOM auf JEDER Configuration | §9.3, §9.4 |
| 13 | Windows: Umlaute werden Muell | `options.encoding = "UTF-8"` pro JavaCompile-Task | §11.5 |
| 14 | Windows: "Unable to delete" bei clean | `gradlew --stop` + Defender-Ausnahme `build/`+`.gradle/` | §11.2 |
| 15 | `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (Debug) | Keystore-Mismatch zwischen Rechnern — Fingerprints vergleichen, NIE reflexhaft deinstallieren | §13 |

---

## 1. Versions-Kompatibilitaet (AGP ↔ Gradle ↔ JDK ↔ compileSdk)

**Kompatibilitaetsmatrix** (Auszug, fuer den relevanten Korridor):

| AGP | min. Gradle | min. JDK | max compileSdk |
|-----|-------------|----------|----------------|
| 8.7 | 8.9 | 17 | 35 |
| 8.10 | 8.11.1 | 17 | 36 |
| 8.11 / 8.13 | 8.13 | 17 | 36 |
| **9.0** | **9.1.0** | 17 | 36 ("36.1") |
| 9.1 | 9.3.1 | 17 | 36.1 |

Euer Stand (Gradle 8.11.1, JDK 21) ist fuer AGP 8.7/8.10 gueltig. Fuer AGP 9.0 muss der
Gradle-Wrapper zuerst auf 9.1.0+.

### 1.1 AGP/Gradle-Versions-Mismatch  ⭐
**Symptom:** Build bricht ab, z.B. "Minimum supported Gradle version is 9.1.0. Current version is 8.11.1".
**Ursache:** Jede AGP-Version erzwingt eine Mindest-Gradle-Version (Matrix oben).
**Versionen:** per Design, alle AGP-Versionen.
**FIX:** Gradle-Wrapper passend heben (`gradle/wrapper/gradle-wrapper.properties` → `distributionUrl`),
ODER den AGP Upgrade Assistant nutzen (kennt die Matrix). Reihenfolge beim Upgrade: erst Gradle, dann AGP.
**Quelle:** developer.android.com/build/releases/about-agp

### 1.2 JDK zu alt (oder zu neu) fuer die Tool-Kombination
**Symptom:** "requires JDK 17" / Gradle-Daemon startet ueberhaupt nicht.
**Ursache:** Ab **AGP 8.0** ist **JDK 17** Pflicht zum Ausfuehren von Gradle. Umgekehrt: ein JDK, das
*neuer* ist als die Gradle-Version unterstuetzt, laesst den Daemon gar nicht starten (Gradle 8.5+ kann
unter JDK 21 laufen, Gradle 9.0 braucht min. JDK 17; JVM 27+ wird noch nirgends unterstuetzt).
**Versionen:** JDK-17-Pflicht ab AGP 8.0; JDK-21-Support ab Gradle 8.5. Euer JDK 21 ist OK.
**FIX (funktionserhaltend):** JDK-Version gegen die Gradle-Compatibility-Matrix abgleichen, BEVOR ein
JDK aktualisiert wird. Reproduzierbar: `kotlin { jvmToolchain(21) }` (Gradle waehlt das JDK selbst,
unabhaengig von `JAVA_HOME`). Bei "uninstalled JDK"-Verwirrung: `gradlew --stop`.
**Quelle:** developer.android.com/build/jdks · docs.gradle.org/current/userguide/compatibility.html

### 1.3 compileSdk neuer als die AGP-Version unterstuetzt
**Symptom:** Warnung "compileSdk version is not supported by this AGP" / "We recommend using a newer AGP".
**Ursache:** compileSdk ueberschreitet das von der AGP-Version unterstuetzte Maximum (z.B. compileSdk 36
braucht AGP 8.9/8.10+).
**Versionen:** per Design. Euer compileSdk 36 ist mit AGP 8.10 OK, mit AGP 8.7 grenzwertig.
**FIX:** AGP anheben (sauber) ODER compileSdk senken. Warnung nur im Notfall unterdruecken:
`android.suppressUnsupportedCompileSdk=<API>` in `gradle.properties` — besser die Diskrepanz beheben.
**Quelle:** developer.android.com/build/releases/past-releases/agp-8-3-0-release-notes

---

## 2. AGP 9.0 — Breaking Changes (der grosse Upgrade-Block, Jan 2026)

> Diese Sektion ist die Landkarte fuer das AGP-9.0-Upgrade. Alle Punkte als **einen** Schnitt
> abarbeiten, nicht einzeln. Viele haben ein temporaeres Opt-out-Flag (bis AGP 10.0).

### 2.1 Neue DSL als Default → `ClassCastException ... BaseExtension`  ⭐
**Symptom:** `java.lang.ClassCastException: ...ApplicationExtensionImpl$AgpDecorated_Decorated cannot be cast to ...BaseExtension`.
**Ursache:** AGP 9.0 nutzt ausschliesslich die neuen Public-DSL-Interfaces (`android.newDsl=true`); alte
`BaseExtension`-Typen sind verborgen. Trifft Build-Logic/Plugins, die alte Typen casten.
**Versionen:** Default ab AGP 9.0; Opt-out bis 10.0.
**FIX (funktionserhaltend):** Build-Logic auf Public-DSL + `androidComponents` umstellen
(`applicationVariants`→`androidComponents.onVariants`, `variantFilter`→`beforeVariants`,
`sdkDirectory`/`bootClasspath`→`androidComponents.sdkComponents`). Temporaer: `android.newDsl=false`.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.2 Built-in Kotlin als Default → externes KGP-Plugin inkompatibel
**Symptom:** `org.jetbrains.kotlin.android` Plugin kollidiert mit der neuen DSL.
**Ursache:** AGP 9.0 aktiviert built-in Kotlin per Default und haengt von KGP 2.2.10+ ab.
**Versionen:** Default ab AGP 9.0.
**FIX:** Auf built-in Kotlin migrieren ODER `android.builtInKotlin=false`. Hoehere KGP per
buildscript-classpath erzwingbar.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.3 `proguard-android.txt` als Default-Basis verboten  ⭐
**Symptom:** Build failt bei `getDefaultProguardFile("proguard-android.txt")`.
**Ursache:** Diese Default-Datei enthaelt `-dontoptimize` und wird in AGP 9.0 gesperrt
(`android.r8.proguardAndroidTxt.disallowed=true`). Nur noch `proguard-android-optimize.txt` ist Basis.
**Versionen:** Default ab AGP 9.0. **Beide Projekte nutzen bereits `proguard-android-optimize.txt`** →
hier safe; trotzdem beim Upgrade bewusst lassen. (Sprachseitige Sicht: `kotlin.md` §10.7.)
**FIX (funktionserhaltend):** `getDefaultProguardFile("proguard-android-optimize.txt")` verwenden. Wenn
Optimierung wirklich aus muss: `-dontoptimize` in eine eigene `custom-rules.txt`. Temp-Opt-out:
`android.r8.proguardAndroidTxt.disallowed=false`.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.4 `strictFullModeForKeepRules` Default → keep-Regel haelt den Ctor nicht mehr  ⭐
**Symptom:** Reflection/Default-Konstruktor zur Laufzeit weg, OBWOHL eine `-keep class A`-Regel existiert.
**Ursache:** `android.r8.strictFullModeForKeepRules=true` (Default ab 9.0): `-keep class A` haelt nicht
mehr automatisch den No-Args-Ctor.
**Versionen:** Default ab AGP 9.0.
**FIX (funktionserhaltend):** Regel praezisieren: `-keep class A { <init>(); }`. Auch
`android.r8.optimizedResourceShrinking=true` ist ab 9.0 Default → siehe §7.5.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.5 Variant-API + Property-Renames entfernt
**Symptom:** Compile-Fehler bei `applicationVariants`, `variantFilter`, `Variant.minSdkVersion`,
`targetSdkVersion`, `enable` etc.
**Ursache:** Deprecated Variant-/DSL-APIs in 9.0 entfernt; Property-Renames (`targetSdkVersion`→`targetSdk`,
`enable`→`enabled`).
**Versionen:** entfernt ab AGP 9.0.
**FIX:** Auf `androidComponents`-API + neue Property-Namen umstellen (Mapping-Tabelle in den 9.0-Notes).
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.6 Weitere Fallen — kapt, KSP-Versionen, failOnMissingFiles, KMP, gebuendelte Versionen (Recherche-Update 2026-06-19)
> Verifiziert gegen developer.android.com/build/releases/agp-9-0-0-release-notes + /build/migrate-to-built-in-kotlin.
- **`kotlin-kapt` inkompatibel mit built-in Kotlin:** das alte `kotlin-kapt`-Plugin bricht. FIX: auf
  **`com.android.legacy-kapt`** (gleiche Version wie AGP) wechseln ODER ganz auf KSP migrieren.
- **KSP1→KSP2 Versions-Schwelle:** KSP vor **2.3.1** hat keine AGP-9.0-Unterstuetzung; die deprecated
  `compilerOptions`-KGP-API wird erst in **KSP 2.3.3** sauber geloest. Built-in Kotlin zieht KSP automatisch
  auf das gebuendelte Suffix **2.2.10-2.0.2** hoch, wenn niedriger.
- **`proguard.failOnMissingFiles=true` (neuer Default ab 9.0):** fehlende/ungueltige proguard-Datei-Eintraege
  brechen jetzt den Build (statt still ignoriert). FIX: ungueltige Eintraege entfernen. Temp-Opt-out:
  `android.proguard.failOnMissingFiles=false`.
- **KMP-Projekte:** `org.jetbrains.kotlin.multiplatform` im selben Subprojekt wie `com.android.library` ist
  inkompatibel → auf das **Android Gradle Library Plugin fuer KMP** umstellen.
- **Gebuendelte Versionen in AGP 9.0:** KGP **2.2.10**, KSP **2.2.10-2.0.2**, Build-Tools **36.0.0**,
  NDK **28.2.13676358**, compileSdk max API **36.1**, Gradle min **9.1.0**, JDK min **17**.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes · /build/migrate-to-built-in-kotlin

### 2.6 buildFeatures-Defaults gekippt + entfernte globale Properties
**Symptom:** `resValues`/`shaders` ploetzlich aus; globale `android.defaults.buildfeatures.aidl/renderscript` wirkungslos.
**Ursache:** `android.defaults.buildfeatures.aidl/renderscript` entfernt; `resValues`/`shaders` Default `true→false`.
**Versionen:** ab AGP 9.0.
**FIX (funktionserhaltend):** Pro Modul aktivieren: `android { buildFeatures { aidl = true; resValues = true; shaders = true } }`
— nur was wirklich gebraucht wird.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.7 "Enforced"-Properties brechen den Build
**Symptom:** Build-Error / Upgrade-Assistant verweigert, wenn `android.r8.integratedResourceShrinking`
oder `android.enableNewResourceShrinker.preciseShrinking` noch gesetzt sind.
**Ursache:** Diese Features sind in 9.0 immer aktiv → die Property darf nicht mehr existieren.
**Versionen:** ab AGP 9.0.
**FIX:** Die Properties aus `gradle.properties` entfernen.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.8 Weitere gekippte Defaults (Stolperfallen-Sammlung)
`android.uniquePackageNames`, `android.enableAppCompileTimeRClass` (non-final R → `when`/`switch`
ueber R-IDs zu `if` refactoren), `android.sdk.defaultTargetSdkToCompileSdkIfUnset=true` (→ targetSdk
explizit setzen!), `android.onlyEnableUnitTestForTheTestedBuildType=true`, **`android.proguard.failOnMissingFiles=true`**
(Tippfehler in ProGuard-Pfaden brechen jetzt hart), Library-`consumerProguardFiles` mit globalen
Optionen (`-dontoptimize`/`-dontobfuscate`) abgelehnt (`android.r8.globalOptionsInConsumerRules.disallowed=true`).
**FIX:** Beim Upgrade die 9.0-Release-Notes Punkt fuer Punkt durchgehen; jede betroffene Property bewusst setzen/entfernen.
**Quelle:** developer.android.com/build/releases/agp-9-0-0-release-notes

### 2.9 `useAndroidX` / `enableJetifier` in AGP 9 nicht mehr akzeptiert
**Symptom:** AGP-9-Build-Fehler; Upgrade-Assistant verweigert den Lauf.
**Ursache:** Die Jetifier-Aera ist vorbei; beide Properties werden in AGP 9 nicht mehr akzeptiert (AndroidX ist Standard).
**Versionen:** ab AGP 9.0 (`enableJetifier` warnt schon unter AGP 8).
**FIX:** Vor dem AGP-9-Upgrade `android.useAndroidX` und `android.enableJetifier` aus `gradle.properties` entfernen.
(Hinweis: beide Projekte haben `android.useAndroidX=true` — beim Upgrade entfernen.)
**Quelle:** developer.android.com/build/releases/past-releases/agp-7-2-0-release-notes

---

## 3. namespace & BuildConfig (Build-Pflichten seit AGP 8.0)

### 3.1 namespace-Pflicht
**Symptom:** "Namespace not specified. Please specify a namespace in the module's build.gradle file."
**Ursache:** `package` im Manifest ist abgeschafft; `namespace` muss in `build.gradle(.kts)` stehen (schon seit AGP 8.0, nicht erst 9.0).
**Versionen:** ab AGP 8.0. **Beide Projekte setzen `namespace`** → erledigt.
**FIX:** `android { namespace = "com.example.app" }`. Upgrade-Assistant migriert automatisch.
**Quelle:** developer.android.com/build/releases/agp-8-0-0-release-notes

### 3.2 BuildConfig ist opt-in
**Symptom:** "BuildConfig class not found" / `BuildConfig`-Felder fehlen.
**Ursache:** Seit AGP 8.0 ist `BuildConfig` standardmaessig aus (Build-Performance).
**Versionen:** ab AGP 8.0. **Beide Projekte setzen `buildFeatures { buildConfig = true }`** → erledigt.
**FIX:** `android { buildFeatures { buildConfig = true } }` — nur in Modulen, die es brauchen.
**Quelle:** developer.android.com/build/releases/agp-8-0-0-release-notes

---

## 4. Gradle 9.0 — Core-Migration (unabhaengig von AGP)

### 4.1 Convention-API komplett entfernt
**Symptom:** Build-Fehler mit Gradle 9.0+, `org.gradle.api.plugins.Convention` nicht mehr vorhanden.
**Ursache:** `Convention` war seit 8.2 deprecated, in 9.0.0 entfernt. Trifft aeltere Plugins.
**Versionen:** entfernt ab Gradle 9.0.0.
**FIX (funktionserhaltend):** Plugins, die noch Convention nutzen, vor dem 9.0-Sprung aktualisieren
(Extensions-API ersetzt Convention 1:1).
**Quelle:** docs.gradle.org/current/userguide/upgrading_major_version_9.html

### 4.2 `jcenter()` endgueltig entfernt
**Symptom:** Build kann Artefakte nicht aufloesen / `jcenter()` unbekannt nach 9.0-Upgrade.
**Ursache:** `jcenter()` (laengst stillgelegt) ist in Gradle 9.0 entfernt.
**Versionen:** entfernt ab Gradle 9.0.0 (gradle#34504, COMPLETED).
**FIX:** Auf `mavenCentral()` (+ `google()`) umstellen; verbliebene `jcenter()`-Eintraege loeschen.
**Quelle:** docs.gradle.org/current/userguide/upgrading_major_version_9.html

### 4.3 Cache-Cleanup-Properties entfernt
**Symptom:** `org.gradle.cache.cleanup` bzw. `buildCache.local.removeUnusedEntriesAfterDays` wirkungslos ab 9.0.
**Ursache:** Beide Properties in 9.0.0 entfernt.
**Versionen:** entfernt ab Gradle 9.0.0.
**FIX:** Cache-Retention/-Cleanup ueber ein **init script** steuern.
**Quelle:** docs.gradle.org/current/userguide/upgrading_major_version_9.html

### 4.4 `JavaExec`/`Test` nutzen ab 9.0 die Toolchain statt der Gradle-JVM
**Symptom:** Tests/JavaExec laufen ab 9.0 mit anderer Java-Version als zuvor.
**Ursache:** Default-Wechsel: bei aktivem `java`-Plugin nutzen diese Tasks die in der `java`-Extension
konfigurierte Toolchain statt der Gradle-JVM.
**Versionen:** Verhaltensaenderung ab Gradle 9.0.0.
**FIX:** Toolchain explizit setzen (`java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }`),
damit Tests die erwartete Version nutzen.
**Quelle:** docs.gradle.org/current/userguide/upgrading_major_version_9.html

### 4.5 Wrapper-Upgrade 8.x → 9.0 stolpert ueber das Versionsschema
**Symptom:** Der uebliche `./gradlew wrapper --gradle-version`-Flow scheitert beim Sprung auf 9.0.
**Ursache:** Neues Versionsschema; die Zielversion muss exakt `9.0.0` (nicht `9.0`) sein.
**Versionen:** gradle#34968 (**OPEN**).
**FIX:** `distributionUrl` direkt auf die exakte `gradle-9.0.0-bin.zip` setzen bzw.
`--gradle-version 9.0.0` exakt angeben.
**Quelle:** github.com/gradle/gradle/issues/34968

### 4.6 Inkrementelle Breaking-Changes/Deprecations 8.13 → 9.6  (Re-Recherche 2026-06-24)
Auf dem Weg zu Gradle 9/10 kommen pro Minor-Release weitere kleine Brueche dazu — beim Wrapper-Sprung mitnehmen:
- **8.13:** `JvmTestSuite` aendert sich — Property `testType` entfernt, durch `testSuiteName` ersetzt
  (auch in Test-Report-/JaCoCo-Aggregation). `BuildLauncher.addJvmArguments` ueberschreibt nicht mehr Flags
  aus `org.gradle.jvmargs` (Tooling-API; bei Bedarf `setJvmArguments`).
- **8.14:** Der **Gradle-Wrapper ist jetzt ein ausfuehrbares JAR** (`Main-Class`-Attribut) — relevant fuer
  Security-Scanner/Reproduzierbarkeits-Pruefungen, die `gradle-wrapper.jar` inspizieren. Inkubierende
  `Settings.getDefaults()` entfernt → `Settings.defaults(Action<SharedModelDefaults>)`. Groovy-String→Enum-
  Coercion fuer `Property`-Typen deprecated (Fehler in Gradle 10).
- **9.0:** Config Cache wird **"preferred mode"** (CLI weist darauf hin, `gradle init` aktiviert es fuer neue
  Projekte; Default erst in Gradle 10), 3-stelliges SemVer-Schema (§4.5), Kotlin 2 + Groovy 4 intern.
- **9.1/9.4/9.6:** Config-Cache **precise tracking** wird ausgebaut → hoehere Cache-Hit-Raten: 9.1 fuer `-P`-
  Properties, 9.4 fuer `gradle.properties`, 9.6 fuer `org.gradle.project.*`-System-Properties + `ORG_GRADLE_PROJECT_*`-Env.
- **9.6.0 (released 2026-06-20):** impliziter Property-/Method-Lookup in Parent-Projekten **deprecated**
  (Entfernung in Gradle 10) — betrifft alte `allprojects {}`/Cross-Projekt-Konfiguration.
**FIX (funktionserhaltend):** Diese Aenderungen NICHT einzeln, sondern als Teil des geplanten 9er-Sprungs
abarbeiten (zusammen mit §2 AGP 9.0 + §4.1-4.5). Vorher das Build-Log auf die genannten Deprecation-Warnungen scannen.
**Quelle:** docs.gradle.org/current/userguide/upgrading_version_8.html · docs.gradle.org/9.6/release-notes.html

---

## 5. Configuration Cache

> EntropieReductor hat `org.gradle.configuration-cache=false` **bewusst** gesetzt (Memory
> `reference_entropie_config_cache_false`): `build.gradle.kts` liest den Maps-API-Key per
> `readText()` zur Konfigurationszeit (§5.2). NICHT blind auf `true` stellen. BestJournalAndroid
> hat CC gar nicht aktiviert.

### 5.1 `Task.project` zur Execution-Zeit  ⭐
**Symptom:** "invocation of Task.project at execution time is unsupported" bei aktivem CC.
**Ursache:** Ein Task greift in seinem `@TaskAction`-Body auf `project` zu; das Project-Objekt ist bei
CC zur Laufzeit nicht verfuegbar.
**Versionen:** per Design seit CC; haeufig bei aelteren Plugins (spotbugs#670, COMPLETED).
**FIX (funktionserhaltend):** Benoetigte Werte zur **Konfigurationszeit** in serialisierbare Felder/Provider
extrahieren (`project.layout.buildDirectory`, `providers.*`), NICHT `project` im Action-Body verwenden.
**Quelle:** docs.gradle.org/current/userguide/configuration_cache_debugging.html

### 5.2 System-Property/Datei zur Konfigurationszeit gelesen (Franks EntropieReductor-Fall)  ⭐
**Symptom:** CC-Report nennt System-Properties/Env-Vars/Datei-Reads als undeklarierte Inputs; CC wird
nicht wiederverwendet.
**Ursache:** Zugriff auf System-Property/Env/Datei waehrend der Konfigurationsphase ohne Provider-API.
Genau der EntropieReductor-Fall (`skMapsKeyFile.readText()` in `build.gradle.kts`).
**Versionen:** per Design (CC-Anforderung).
**FIX (funktionserhaltend):** Input per Provider-API deklarieren (`providers.fileContents(...)`,
`providers.systemProperty(...)`) statt direkt lesen; ODER den Task mit
`notCompatibleWithConfigurationCache("Grund")` markieren; ODER (wie EntropieReductor) bewusst
`configuration-cache=false` lassen. NICHT das Feature (Key-Einlesen) entfernen.
**Quelle:** docs.gradle.org/current/userguide/configuration_cache_debugging.html

### 5.3 CC ist sensitiv auf Aenderung von Gradle-Property-WERTEN
**Symptom:** CC wird invalidiert/neu berechnet, sobald sich ein `gradle.properties`-Wert aendert — auch
bei korrekter Provider-API-Nutzung.
**Ursache:** CC behandelt Gradle-Property-Werte als Cache-Key-Input.
**Versionen:** gradle#32219 (CLOSED als DUPLICATE 2025-02-03 — der Effekt bleibt erwartetes Verhalten).
**FIX:** Als erwartetes Verhalten akzeptieren; Property-Werte stabil halten. Kein eigener Workaround noetig.
**Quelle:** github.com/gradle/gradle/issues/32219

### 5.4 Korrupter Configuration Cache wird gespeichert (Folge-Builds scheitern garantiert)
**Symptom:** Nach einem fehlerhaften Lauf scheitert jeder weitere Build bis der CC manuell invalidiert wird.
**Ursache:** Gradle persistiert in manchen Faellen einen korrupten CC-Eintrag statt ihn zu verwerfen.
**Versionen:** gradle#23802 (CLOSED NOT_PLANNED 2023-02-24 — Workaround bleibt).
**FIX:** CC verwerfen: `.gradle/configuration-cache/` loeschen bzw. mit `--no-configuration-cache`
einmal bauen. (Greift nur, wenn CC ueberhaupt aktiv ist.)
**Quelle:** github.com/gradle/gradle/issues/23802

### 5.5 Paralleler Configuration Cache (`configuration-cache.parallel`) ist nur teilweise sicher
**Symptom:** Build bricht bei aktiviertem Parallel-CC mit Cross-Project-Zugriffsfehlern; mit Feature aus laeuft er.
**Ursache:** Paralleles Store/Load (incubating ab Gradle 8.11) verlangt, dass Build-Logik UND alle Plugins
waehrend der Konfiguration nicht auf andere Projekte zugreifen — aeltere/fremde Plugins verletzen das.
**Versionen:** Feature ab Gradle 8.11; Empfehlung Gradle 9.2.1+.
**FIX (funktionserhaltend):** Inkrementell aktivieren — zuerst nur `configuration-cache=true`, dann ggf.
`.parallel=true` testen. Bei Fehlern Feature aus lassen (kein Funktionsverlust, nur langsamer).
**Quelle:** dev.to/cdsap/gradle-811 · docs.gradle.org/current/userguide/configuration_cache.html

---

## 6. Build Cache & Gradle Daemon & Performance

### 6.1 "Gradle build daemon disappeared unexpectedly" durch jvmargs-Override  ⭐⭐ (betrifft euer Setup)
**Symptom:** Build bricht mit "Gradle build daemon disappeared unexpectedly (it may have been killed or
may have crashed)" oder `OutOfMemoryError: Metaspace` ab — oft erst bei groesseren/wiederholten Builds.
**Ursache:** Sobald `org.gradle.jvmargs` gesetzt ist, werden **ALLE** Gradle-Default-JVM-Args ueberschrieben,
u.a. `-XX:MaxMetaspaceSize=256m`. Ohne erneutes Setzen waechst der Metaspace unbegrenzt → der Daemon wird
vom OS gekillt bzw. crasht an Class-Metadata-OOM. **Beide Projekte setzen aktuell nur
`-Xmx4096m -Dfile.encoding=UTF-8`** → MaxMetaspaceSize fehlt.
**Versionen:** per Design; verschaerft seit Gradle 8 (gradle#19750 **OPEN**, #8354/#23698 COMPLETED).
**FIX (funktionserhaltend):** Metaspace explizit mitgeben:
`org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8`.
**Quelle:** github.com/gradle/gradle/issues/19750

### 6.2 Daemon crasht sofort in Low-Memory-Umgebung (JDK 21)
**Symptom:** `DaemonDisappearedException` direkt beim Start, v.a. bei wenig RAM mit JDK 21.
**Ursache:** Zu wenig Heap/RAM fuer den Daemon-Start.
**Versionen:** gradle#29958 (**OPEN**, "[8.9]").
**FIX:** `-Xmx` an verfuegbaren RAM anpassen (nicht hoeher als sinnvoll), `--no-daemon` zum Isolieren,
ggf. parallele Builds anderer Sessions stoppen.
**Quelle:** github.com/gradle/gradle/issues/29958

### 6.3 Build-Cache-Miss durch ueberlappende Output-Verzeichnisse
**Symptom:** Eigentlich cacheable Tasks werden nie aus dem Cache geladen ("not cacheable" trotz `@CacheableTask`).
**Ursache:** Zwei Tasks schreiben ins selbe Output-Verzeichnis → Gradle kann Outputs nicht zuordnen und
deaktiviert das Caching automatisch.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Output-Verzeichnisse pro Task disjunkt halten; gemeinsame Ziele ueber einen
separaten `Sync`-Task zusammenfuehren. Diagnose: `-Dorg.gradle.caching.debug=true`.
**Quelle:** docs.gradle.org/current/userguide/common_caching_problems.html

### 6.4 Parallel-Build (`org.gradle.parallel=true`) instabil bei nicht-thread-safem State
**Symptom:** Intermittierende Fehler nur mit `--parallel`, v.a. `clean` + Build kombiniert (Race beim Loeschen).
**Ursache:** `org.gradle.parallel` garantiert KEINE thread-sichere Projekt-Konfiguration; Tasks mit
gemeinsamem mutable State ueber Projektgrenzen koennen kollidieren.
**Versionen:** gradle#15163 (CLOSED NOT_PLANNED), #15938 (COMPLETED). EntropieReductor hat `parallel=true`.
**FIX (funktionserhaltend):** `clean` nicht parallel mit dem Build mischen (separater Invocation);
nicht-thread-safe Tasks ueber die Worker-API isolieren. Bei Instabilitaet `parallel` aus lassen (nur langsamer).
**Quelle:** github.com/gradle/gradle/issues/15163

---

## 7. R8 / ProGuard / Code- & Resource-Shrinking / D8-Dexing (Release-Build)

> R8 laeuft NUR im Release-Build (`isMinifyEnabled=true`). Symptome treten daher nie im Debug auf,
> sondern erst im Release/Play-Store → **Release-Variante immer real testen** (bei Billing/Paywall
> ohnehin Pflicht, Memory `feedback_billing_release_only`). fullMode ist seit AGP 8.0 Default.
> Sprachseitige keep-Rules fuer kotlinx.serialization: `kotlin.md` §10.3.

### 7.1 fullMode strippt `Signature`-Attribut → generische Typen weg  ⭐
**Symptom:** Gson/Moshi/Retrofit verlieren generische Typinfo zur Laufzeit; `TypeToken`-Deserialisierung
crasht oder liefert falsche Typen.
**Ursache:** In fullMode werden Attribute (`Signature`, Annotations) NUR fuer explizit per `-keep`
gematchte Member behalten — selbst wenn `-keepattributes` gesetzt ist.
**Versionen:** fullMode Default ab AGP 8.0 (retrofit#3005/#3751, COMPLETED — die Libs liefern heute
eigene consumer-Rules).
**FIX (funktionserhaltend):** `-keepattributes Signature,InnerClasses,EnclosingMethod` + gezielte
TypeToken-/Service-keep-Regel. Aktuelle Lib-Versionen bringen die Regeln meist selbst mit. NIE fullMode global abschalten.
**Quelle:** developer.android.com/topic/performance/app-optimization/full-mode

### 7.2 fullMode haelt den Default-Konstruktor trotz gekeepter Klasse nicht
**Symptom:** `class.getDeclaredConstructor().newInstance()` → `NoSuchMethodException`.
**Ursache:** fullMode behaelt den No-Args-Ctor NICHT implizit, auch wenn die Klasse gekeept ist.
(Verstaerkt durch `strictFullModeForKeepRules` ab AGP 9.0, §2.4.)
**Versionen:** fullMode ab AGP 8.0.
**FIX:** Ctor explizit keepen: `-keep class X { <init>(); }`.
**Quelle:** developer.android.com/topic/performance/app-optimization/full-mode

### 7.3 fullMode aendert Sichtbarkeit (private → public) → Reflection-Crash
**Symptom:** Crash bei Reflection, die auf eine bestimmte Sichtbarkeit angewiesen ist (`getDeclaredField` etc.).
**Ursache:** fullMode aendert Visibility, um Inlining zu ermoeglichen; indirekte (Reflection-)Nutzung erkennt R8 nicht.
**Versionen:** fullMode ab AGP 8.0.
**FIX:** Member explizit keepen: `-keep class X { private <fields>; }` — erhaelt Member UND Original-Sichtbarkeit.
**Quelle:** developer.android.com/topic/performance/app-optimization/full-mode

### 7.4 "Missing classes detected while running R8"
**Symptom:** Build bricht ab und verweist auf `build/outputs/mapping/<variant>/missing_rules.txt`.
**Ursache:** Referenzierte (oft transitive/optionale) Klassen fehlen im Classpath; ab AGP 8 hartes Fehlschlagen.
**Versionen:** hartes Verhalten ab AGP 8.0.
**FIX (funktionserhaltend):** Die in `missing_rules.txt` empfohlenen Regeln nach `proguard-rules.pro`
uebernehmen, iterativ rebuilden. KEIN blinder `-dontwarn`-Flood — wird die Klasse zur Laufzeit doch
gebraucht, crasht die App beim Start.
**Quelle:** developer.android.com/build/shrink-code

### 7.5 Optimized Resource Shrinking entfernt dynamisch geladene Ressourcen  ⭐ (Upgrade-relevant)
**Symptom:** Per `getIdentifier()`/String-Name geladene Ressourcen fehlen zur Laufzeit →
`Resources$NotFoundException` oder leerer Screen. Tritt nach einem AGP-Upgrade ploetzlich auf.
**Ursache:** Optimized Resource Shrinking (opt-in `android.r8.optimizedResourceShrinking=true` ab AGP 8.12,
**Default ab AGP 9.0**) nutzt strengere statische Analyse — dynamisch referenzierte Ressourcen sind unsichtbar.
**Versionen:** opt-in AGP 8.12, Default AGP 9.0.
**FIX (funktionserhaltend):** Keep-File `res/raw/keep.xml` mit `tools:keep="@drawable/foo,@layout/dyn_*"`
(Wildcards moeglich) — dynamische Ressourcen explizit schuetzen, nicht das Feature abschalten. **Vor dem
AGP-9-Upgrade pruefen, ob die Apps Ressourcen per `getIdentifier()`/Name laden.**
`tools:shrinkMode="safe"` (Default) versucht, per `getIdentifier()` referenzierte Ressourcen zu erhalten;
`strict` behaelt nur statisch referenzierte → dann sind die keep-Regeln zwingend korrekt. Diagnose:
`build/outputs/mapping/release/resources.txt` listet entfernte Ressourcen ("`raw:foo:… is not reachable`").
⚠️ **Endungs-Falle:** Im `tools:keep`-Eintrag steht der Ressourcen-Name OHNE Dateiendung —
`tools:keep="@raw/aboutlibraries"`, NICHT `@raw/aboutlibraries.json`.
**Beleg (konkreter Fall, gh-verifiziert 2026-06-24):** AboutLibraries laedt `res/raw/aboutlibraries.json`
per String-Name → `IllegalStateException` aus `Libs$Builder.build()` im Release, sobald optimized resource
shrinking aktiv ist. Unter AGP 9.0 (Default) am 2026-01-22 erneut bestaetigt; erster `tools:keep`-Versuch
MIT `.json` schlug fehl, ohne Endung wirkt er.
**Quelle:** developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep ·
github.com/mikepenz/AboutLibraries/issues/1239 (CLOSED COMPLETED 2025-10-10)

### 7.6 `shrinkResources` ohne `minifyEnabled`
**Symptom:** Build-Fehler / Resource-Shrinker laeuft nicht.
**Ursache:** Resource-Shrinking setzt Code-Shrinking voraus.
**Versionen:** per Design. (Beide Projekte setzen beides → OK.)
**FIX:** `isMinifyEnabled=true` UND `isShrinkResources=true` gemeinsam.
**Quelle:** developer.android.com/build/shrink-code

### 7.7 Zu breite `-keep`-Regeln → App schrumpft nicht
**Symptom:** APK/AAB bleibt gross trotz aktivem R8.
**Ursache:** `-keep class com.foo.** { *; }` verbietet shrink+obfuscate+optimize fuer ganze Pakete.
**Versionen:** per Design.
**FIX:** Regeln so eng wie moeglich (nur konkrete Reflection-/JNI-Entrypoints). Mit dem **R8 Configuration
Analyzer** (§7.10) pruefen, welche Regel wie viele Klassen blockiert.
**Quelle:** android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html

### 7.8 R8 strippt `@HiltViewModel`-Konstruktor → Release-Crash
**Symptom:** App crasht NUR im Release-Build nach AGP-/Hilt-Update; Hilt-ViewModel kann nicht instanziiert werden.
**Ursache:** R8 entfernt den per DI genutzten Konstruktor als "unbenutzt".
**Versionen:** dagger#4739 (**OPEN**; gemeldet mit AGP 8.9.2 + Hilt 2.56.2).
**FIX (funktionserhaltend):** Hilt/Dagger auf aktuelle Version heben; falls weiterhin betroffen, gezielte
keep-Regel fuer die `@HiltViewModel`-Konstruktoren ergaenzen. Release-Variante real testen.
**Quelle:** github.com/google/dagger/issues/4739

### 7.9 D8: "Cannot fit requested classes in a single dex file" (64K-Limit)
**Symptom:** Dexing scheitert; >65536 Methodenreferenzen in einer DEX.
**Ursache:** App + Libs ueberschreiten das 64K-Methodenlimit eines DEX.
**Versionen:** per Design.
**FIX:** `multiDexEnabled=true` (bei `minSdk ≥ 21` reicht das Flag — beide Projekte sind ≥ 26).
Aktiviertes R8/Shrinking senkt die Methodenzahl und entschaerft das oft schon.
**Quelle:** developer.android.com (multidex)

### 7.10 Werkzeuge: mapping.txt & R8 Configuration Analyzer
- **mapping.txt** deobfuskiert nur Crashes, die NACH dem Upload eingehen → fuer JEDE Version eine eigene
  `mapping.txt` hochladen (im AAB automatisch enthalten). Lokal: `retrace` / `r8 --deobfuscate-stack-trace`.
  Quelle: support.google.com/googleplay/android-developer/answer/9848633
- **R8 Configuration Analyzer** (R8 ab AGP 9.3) erzeugt `build/outputs/mapping/release/configanalyzer.html`
  und zeigt, welche keep-Regel die Optimierung blockiert. Quelle:
  developer.android.com/topic/performance/app-optimization/r8-configuration-analyzer

---

## 8. KSP als Gradle-Plugin (Build-Integration)

> KSP **2.1.0-1.0.29** ist faktisch noch **KSP1** (K1-Compiler-Plugin), solange `ksp.useKSP2` nicht
> gesetzt ist. Die Kotlin-Versions-Kopplung des Suffix steht in `kotlin.md` §10.1.

### 8.1 KSP1 stirbt → `useKSP2` ab Kotlin 2.3 / AGP 9.0 noetig  ⭐
**Symptom:** Build bricht/verhaelt sich falsch nach Upgrade auf Kotlin 2.2+/AGP 9.0; neue Sprachfeatures
werden von KSP nicht verarbeitet.
**Ursache:** KSP1 ist ein K1-Compiler-Plugin; das KGP sperrt die internen APIs ab Kotlin 2.2. KSP1 ist
mit Kotlin 2.3 inkompatibel.
**Versionen:** KSP2 Default seit 2.0.0; KSP1 deprecated ab Kotlin 2.2; Pflicht ab Kotlin 2.3 / AGP 9.0.
**FIX (funktionserhaltend):** `ksp.useKSP2=true` in `gradle.properties` — als Teil desselben Schnitts wie
das AGP-9/Kotlin-2.3-Upgrade. Rueckfall: `ksp.useKSP2=false`. (Cross-ref `kotlin.md` §10.1.)
**Quelle:** github.com/google/ksp/blob/main/docs/ksp2.md

### 8.2 Incremental bricht bei Aenderung an einer Projekt-Dependency  ⭐⭐ (trifft eure Version)
**Symptom:** Nach Aenderung an einem Modul-Dependency bekommt der KSP-Processor keine Input-Files
(`resolver.getAllFiles()` leer) → erwarteter Code wird nicht generiert → Compile-Fehler. Clean-Build behebt es.
**Ursache:** Regression eingefuehrt ab **KSP 2.1.0-1.0.28**.
**Versionen:** ksp#2252 (CLOSED **COMPLETED 2025-01-30** — der Fix kam jedoch knapp NACH dem Release von
2.1.0-1.0.29). ⚠️ **Ehrlich: eure installierte 2.1.0-1.0.29 ist mit hoher Wahrscheinlichkeit noch
betroffen** (Fix erst in der naechsten Patchversion). EntropieReductor mit `ksp.incremental=true` ist am anfaelligsten.
**FIX (funktionserhaltend):** KSP-Patchversion heben (naechste `2.1.x-1.0.30+`) ODER bei "stale generated
code"-Symptomen reflexartig `clean` ausfuehren; alternativ projektweit `ksp.incremental=false`.
**Quelle:** github.com/google/ksp/issues/2252

### 8.2a `NoSuchMethodError: KspTaskJvm.getChangedFiles(...)` — KSP/Kotlin-Versions-Mismatch  (Re-Recherche 2026-06-24)
**Symptom:** Build bricht ab mit `NoSuchMethodError` auf `KspTaskJvm.getChangedFiles(...)`.
**Ursache:** Das KSP-Gradle-Plugin und die Kotlin-Build-Tools-API passen nicht zusammen — das KSP-Suffix
ist NICHT exakt an die Kotlin-Version gekoppelt (Cross-ref `kotlin.md` §10.1). Eine "irgendeine" KSP-Version
zu einer anderen Kotlin-Version fuehrt zu fehlenden internen Methoden.
**Versionen:** versionsuebergreifend; tritt bei jeder Suffix-Fehlpaarung auf.
**FIX (funktionserhaltend):** KSP-Version exakt zum Kotlin-Suffix waehlen (`2.1.0-1.0.29` zu Kotlin 2.1.0).
Beim Kotlin-Upgrade KSP im selben Schnitt mitziehen — nie einzeln bumpen.
**Quelle:** Stack-Overflow-Faelle 2026; google/ksp Versionierungs-Doku.

### 8.2b KSP2 schlaegt Modul-Recompilation auf Windows fehl — GEFIXT in 2.3.6  (Re-Recherche 2026-06-24)
**Symptom:** Mit `ksp.useKSP2=true` scheitert auf **Windows** die inkrementelle Modul-Recompilation.
**Ursache:** KSP2-Pfad-/Recompile-Handling unter Windows (Frank-relevant: Windows-Hauptmaschine).
**Versionen:** ksp#2774 (**CLOSED COMPLETED 2026-02-17**) — gefixt in **KSP 2.3.6**. Eure KSP1-Linie
(2.1.0-1.0.29, `useKSP2` nicht gesetzt) ist NICHT betroffen; relevant erst, wenn beim Kotlin-2.3/AGP-9-Sprung
`useKSP2=true` aktiviert wird (§8.1) — dann mindestens KSP 2.3.6 verwenden.
**FIX (funktionserhaltend):** Beim KSP2-Wechsel auf KSP ≥ 2.3.6 gehen.
**Quelle:** github.com/google/ksp/issues/2774

### 8.3 Generierte Sources fehlen nach inkrementellem Build
**Symptom:** Clean-Build OK, aber inkrementeller Build → KSP-Komponenten nicht verarbeitet, generierte
Sources nicht aufloesbar.
**Ursache:** Inkrementelle Dirty-Set-Berechnung verliert generierte Outputs.
**Versionen:** ksp#2536 (**OPEN**).
**FIX:** `clean` oder `ksp.incremental=false`.
**Quelle:** github.com/google/ksp/issues/2536

### 8.4 KSP + Build-Cache restauriert geloeschte Klassen
**Symptom:** Klasse entfernt → Build mit Cache-Hit → naechste inkrementelle Aenderung restauriert die
geloeschte Klasse → Compile-Fehler.
**Ursache:** `kspCaches` (Backup generierter Klassen) wird vom Gradle-Build-Cache nicht als Output verwaltet.
**Versionen:** ksp#2042 (CLOSED COMPLETED 2024-09-05 — in aktuellen Versionen behoben).
**FIX:** Aktuelle KSP-Version; bei stale code `clean`.
**Quelle:** github.com/google/ksp/issues/2042

### 8.5 KSP + KAPT + Configuration Cache brechen zusammen
**Symptom:** CC-Serialisierung scheitert ("state could not be cached: field `__compileKotlinArgumentsContributor...`"),
v.a. wenn KSP + KAPT gleichzeitig aktiv sind.
**Ursache:** KSP/KAPT-Tasks serialisieren nicht-cachebare Objekte.
**Versionen:** ksp#1805 (CLOSED **NOT_PLANNED** 2026-04-07 — Workaround bleibt dauerhaft).
**FIX (funktionserhaltend):** KAPT komplett raus, nur `ksp(...)` nutzen. (Beide Projekte sind bereits
KAPT-frei → erledigt.)
**Quelle:** github.com/google/ksp/issues/1805

### 8.6 Room `schemaLocation` als Plain-`arg(...)` → Cache-Miss / Leerzeichen-Regex-Crash
**Symptom:** `room.schemaLocation` wird nicht als Up-to-date-Input erkannt (Cache-Probleme); ODER
`apoption does not match \S+=\S+`, wenn der Projektpfad ein **Leerzeichen** enthaelt; Schema-Verzeichnis bleibt leer.
**Ursache:** `room.schemaLocation` ist ein Verzeichnis; als simples `arg(...)` kennt Gradle es nicht fuer
Up-to-date-Checks, und Leerzeichen brechen den Regex. **Beide Projekte nutzen aktuell
`ksp { arg("room.schemaLocation", "$projectDir/schemas") }`** — funktioniert, solange der Pfad kein
Leerzeichen hat (`C:\Users\barwa\proggs\...` ist OK).
**Versionen:** nowinandroid#604.
**FIX (funktionserhaltend):** Schema via `CommandLineArgumentProvider` (`@get:InputDirectory` +
`PathSensitivity.RELATIVE`) uebergeben; Pfade ohne Leerzeichen halten. Neuere Room-Gradle-Plugins loesen
das KSP2-Schema-Setup sauber.
**Quelle:** github.com/android/nowinandroid/issues/604

### 8.7 Hilt/Dagger: `kspAndroidTest`/`kspTest` fehlt → Test-Komponenten nicht generiert
**Symptom:** Hilt-Test-Komponenten fehlen; Instrumentation-/Unit-Tests mit Hilt scheitern.
**Ursache:** Nur `ksp(...)` konfiguriert, aber KSP verarbeitet pro SourceSet getrennt (anders als KAPT gewohnt).
**Versionen:** per Design.
**FIX:** Zusaetzlich `kspAndroidTest("...hilt-compiler:X")` (Instrumentation) und `kspTest(...)` (Unit) deklarieren.
**Quelle:** dagger.dev/hilt/gradle-setup.html

### 8.8 Plugin-Apply-Reihenfolge / `ksp(...)` statt `kapt(...)`
**Symptom:** Generierte Sources werden von IDE/Compiler nicht gefunden; "annotation processing"-Fehler.
**Ursache:** Falsche Reihenfolge im `plugins {}`-Block oder noch `kapt(...)`-Konfiguration.
**Versionen:** per Design.
**FIX:** Reihenfolge `com.android.application` → `org.jetbrains.kotlin.android` → `com.google.devtools.ksp`;
Dependencies mit `ksp(...)`. `build/generated/ksp/...` wird in neueren Versionen automatisch registriert.
(Beide Projekte haben die korrekte Reihenfolge.)
**Quelle:** kotlinlang.org/docs/ksp-quickstart.html

### 8.9 KSP2: OOM, Performance-Regression, erster Build scheitert (Upgrade-relevant)
**Symptom:** (a) `OutOfMemoryError` in grossen Builds; (b) KSP2 ~20–30 % langsamer als KSP1; (c) erster
`assembleDebug` nach Code-Aenderung scheitert (IOException), zweiter klappt.
**Ursache:** (a) Kotlin-Compiler 2.3.20 spawnt Dispatcher-Threads ohne Stop; (b) KSP2-Reifegrad;
(c) korrupte inkrementelle Caches.
**Versionen:** ksp#2817 (**OPEN**, ab Kotlin 2.3.20), #2282 (**OPEN**), #1678 (**OPEN**).
**FIX:** KSP2 erst zum Pflicht-Zeitpunkt (Kotlin 2.3) aktivieren und die Regressionen einplanen; bei (a)
Gradle-Daemon neu starten; bei (c) `clean` / `ksp.incremental=false`.
**Quelle:** github.com/google/ksp/issues/2817 · /2282 · /1678

### 8.10 Hilt-Plugin inkompatibel mit `com.android.legacy-kapt`
**Symptom:** Hilt-Build bricht, sobald `com.android.legacy-kapt` aktiv ist (relevant ab AGP 8.10).
**Ursache:** Plugin-Inkompatibilitaet.
**Versionen:** dagger#4756 (**OPEN**).
**FIX (funktionserhaltend):** `com.android.legacy-kapt` vermeiden; vollstaendig auf KSP setzen (beide
Projekte sind KAPT-frei → nicht betroffen).
**Quelle:** github.com/google/dagger/issues/4756

---

## 9. Dependency-Resolution / Version-Catalogs / BOM

### 9.1 Version-Catalog: Accessor-Name-Clash
**Symptom:** "name clash" bei der Catalog-Generierung, oder ein Alias ist nicht erreichbar.
**Ursache:** Zwei Aliase erzeugen denselben Accessor — ein Alias kann nicht gleichzeitig Blatt UND
Eltern-Gruppe sein (`foo` und `foo-bar` → `libs.foo` kollidiert).
**Versionen:** per Design.
**FIX:** Aliase eindeutig benennen, ueberlappende Praefixe vermeiden.
**Quelle:** docs.gradle.org/current/userguide/how_to_fix_version_catalog_problems.html

### 9.2 Catalog wird nicht gefunden / TOML-Fehler
**Symptom:** `libs` nicht aufloesbar; oder "Undefined version reference / Undefined alias reference /
Reserved alias name / notation is not a valid dependency notation".
**Ursache:** Datei nicht exakt `gradle/libs.versions.toml`; `version.ref` ohne `[versions]`-Eintrag;
Bundle referenziert fehlende Library; Alias heisst `versions`/`bundles`/`plugins`; String-Notation ohne Version.
**Versionen:** per Design (ab Gradle 7.0).
**FIX:** Datei korrekt platzieren/benennen; jede `version.ref` in `[versions]` deklarieren; Aliase nicht
reserviert benennen; BOM-gemanagte Libs als `{ module = "group:name" }` ohne Version notieren.
**Quelle:** docs.gradle.org/current/userguide/how_to_fix_version_catalog_problems.html

### 9.3 BOM: `platform(...)` vergessen
**Symptom:** BOM setzt keine Versionen; Libs ohne Version → "Could not resolve".
**Ursache:** BOM ohne `platform(...)`-Wrapper deklariert → Gradle zieht es als normale Dependency, nicht als Constraint-Provider.
**Versionen:** per Design.
**FIX:** `implementation(platform(libs.compose.bom))` (bzw. `enforcedPlatform` zum Erzwingen).
**Quelle:** developer.android.com/develop/ui/compose/bom

### 9.4 BOM fehlt auf einer Configuration (Test-Deps unaufloesbar)
**Symptom:** `implementation`-Compose-Libs OK, aber `androidTestImplementation`-Libs ohne Version → Resolution-Fehler.
**Ursache:** BOM gilt nur fuer die Configuration, auf der es deklariert ist.
**Versionen:** per Design.
**FIX:** BOM auf JEDER relevanten Configuration deklarieren (`implementation(platform(bom))` UND
`androidTestImplementation(platform(bom))`, ggf. `testImplementation`).
**Quelle:** developer.android.com/develop/ui/compose/bom

### 9.5 Unbeabsichtigter BOM-Override durch eigene Version im Catalog
**Symptom:** Eine BOM-gemanagte Lib zieht eine andere Version als das BOM vorgibt.
**Ursache:** Der Library-Eintrag im Catalog hat eine eigene `version`/`version.ref`, obwohl die Version
vom BOM kommen soll — explizite Version uebersteuert das BOM IMMER.
**Versionen:** per Design.
**FIX:** BOM-gemanagte Libs im Catalog OHNE Version notieren; nur das BOM selbst bekommt `version.ref`.
Bewusste Overrides (z.B. Material3-Alpha) nur, wenn gewollt.
**Quelle:** github.com/gradle/gradle/issues/17117

### 9.6 Compose-Compiler ist NICHT im Compose-BOM
**Symptom:** BOM aktualisiert, aber Compose-Compiler-Mismatch bleibt.
**Ursache:** Der Compose-Compiler folgt der **Kotlin**-Version (Plugin `org.jetbrains.kotlin.plugin.compose`),
NICHT dem BOM. Das BOM steuert nur Runtime-Libraries.
**Versionen:** ab Kotlin 2.0 (cross-ref `kotlin.md` §10.2).
**FIX:** Compose-Compiler-Plugin separat an die Kotlin-Version binden — nie ueber das BOM erwarten.
**Quelle:** developer.android.com/develop/ui/compose/bom

### 9.7 Alpha/Beta-BOM bzw. Alpha-Deps in Produktion
**Symptom:** Instabile Funktionen / unerwartete Breaking Changes.
**Ursache:** Alpha/Beta-BOMs ziehen alpha-Libs. (Beide Projekte nutzen einige `-alpha`-Deps, z.B.
`credentials 1.5.0-alpha06`, EntropieReductor `healthConnect -alpha07`.)
**Versionen:** per Design.
**FIX:** In Release-Builds bevorzugt das Stable-BOM; Alpha-Deps bewusst und beobachtet einsetzen, beim
Bump die jeweiligen Changelogs pruefen.
**Quelle:** developer.android.com/develop/ui/compose/bom

### 9.8 "Duplicate class found"
**Symptom:** `Duplicate class X found in modules ...`.
**Ursache:** Zwei (oft transitive) Module liefern dieselbe Klasse.
**Versionen:** per Design.
**FIX (funktionserhaltend):** `./gradlew app:dependencies` zur Diagnose, dann gezielt
`exclude(group=..., module=...)` ODER `resolutionStrategy.force("group:name:version")` auf eine kompatible
Version. Klasse nicht entfernen.
**Quelle:** developer.android.com/build/dependency-resolution-errors

### 9.9 "newest wins" / gewollter Downgrade greift nicht
**Symptom:** Trotz deklarierter aelterer Version zieht Gradle die hoehere (transitive) Version.
**Ursache:** Gradle-Default = "highest version wins"; eine normale Deklaration ist nur eine Praeferenz.
**Versionen:** per Design.
**FIX:** Echtes Downgrade via `resolutionStrategy.force(...)` oder `version { strictly("x.y.z") }`; harter
Konflikt-Fail mit `failOnVersionConflict()`.
**Quelle:** docs.gradle.org/current/userguide/dependency_constraints_conflicts.html

### 9.10 `FAIL_ON_PROJECT_REPOS` bricht Build bei Repo im Modul
**Symptom:** Build-Fehler, sobald `repositories { }` in einem Modul-`build.gradle(.kts)` auftaucht.
**Ursache:** `dependencyResolutionManagement { repositoriesMode = FAIL_ON_PROJECT_REPOS }` (Android-Default)
verbietet projekt-lokale Repos — alle muessen in `settings.gradle(.kts)` stehen.
**Versionen:** per Design.
**FIX:** Repos nach `settings.gradle.kts → dependencyResolutionManagement { repositories { google(); mavenCentral() } }` verschieben.
**Quelle:** developer.android.com/build/remote-repositories

### 9.11 "Could not find ..." — Repository-Reihenfolge / fehlendes Repo
**Symptom:** "Could not resolve all files ... Could not find group:name:version".
**Ursache:** Artefakt liegt nur in einem nicht (oder zu spaet) gelisteten Repo; AndroidX-Artefakte nur in `google()`.
**Versionen:** per Design.
**FIX:** `google()` UND `mavenCentral()` deklarieren; Spezial-Repos mit Content-Filtering, Fallback zuletzt.
**Quelle:** developer.android.com/build/remote-repositories

### 9.12 Dependency-Verification: "dependencies were not verified"
**Symptom:** Build/CI bricht ab; Checksum/Signatur fehlt in `verification-metadata.xml`.
**Ursache:** `--write-verification-metadata` erfasst nur tatsaechlich heruntergeladene Deps; eine nur in
CI gezogene Dependency hat keinen Eintrag.
**Versionen:** per Design (nur falls Dependency-Verification aktiviert ist — aktuell bei euch nicht).
**FIX:** `./gradlew --write-verification-metadata sha256,pgp` mit leerem `GRADLE_USER_HOME`;
`*-sources.jar`/`*-javadoc.jar` per `<trusted-artifacts>` freigeben.
**Quelle:** docs.gradle.org/current/userguide/dependency_verification.html

---

## 10. Gradle Wrapper

### 10.1 Fehlende Distributions-Integritaetspruefung (`distributionSha256Sum`)
**Symptom:** Keine Verifikation der heruntergeladenen Gradle-Distribution (Korruptions-/Tampering-Risiko).
**Ursache:** `distributionSha256Sum` nicht in `gradle-wrapper.properties` gesetzt. (Beide Projekte haben
nur `validateDistributionUrl=true`, was die URL prueft, nicht den Inhalt.)
**Versionen:** Best-Practice, alle Versionen.
**FIX:** Offiziellen SHA-256 von der Gradle-Releases-Seite als `distributionSha256Sum` eintragen. Bei
Wrapper-Updates den Sum mit-aktualisieren (sonst schlaegt der naechste Lauf fehl).
**Quelle:** docs.gradle.org/current/userguide/gradle_wrapper.html

---

## 11. Windows-spezifische Build-Fallen

> Alle relevant fuer Franks Windows-11-Setup. (Plattform-getrennt, weil macOS/Linux andere Fallen haben.)

### 11.1 MAX_PATH 260 — "Filename longer than 260 characters"
**Symptom:** Build bricht (oft erst beim 2. Lauf) mit zu langem Pfad ab — typisch in `.cxx`-, KSP- oder
Transform-Cache-Verzeichnissen.
**Ursache:** Windows-MAX_PATH-Limit; tief verschachtelte generierte Pfade ueberschreiten es.
**Versionen:** gradle#1989 (**OPEN** — keine automatische Mitigation).
**FIX (in dieser Reihenfolge):** (1) Projekt naeher an den Laufwerks-Root (`C:\src\app`); (2) Long-Path-Support
aktivieren (Registry `LongPathsEnabled=1`); (3) Build-Output auf kurzen Pfad umlenken. (`C:\Users\barwa\proggs\...`
ist grenzwertig lang — bei Pfadfehlern Punkt 1 erwaegen.)
**Quelle:** github.com/gradle/gradle/issues/1989

### 11.2 File-Locking — "Unable to delete directory/file" bei `clean`/Rebuild
**Symptom:** `java.io.IOException: Unable to delete directory ... Failed to delete some children`, oft beim `clean`.
**Ursache:** Ein Prozess haelt Handles offen — Gradle-Daemon, Android Studio, oder **Windows Defender**
(scannt `build/` live). Windows erlaubt kein Loeschen offener Dateien.
**Versionen:** gradle#26912 (COMPLETED, bessere Diagnostik), generell Windows.
**FIX (funktionserhaltend):** (1) `gradlew --stop` vor `clean`; (2) **Defender-Echtzeit-Ausnahme** fuer
Projektordner + `%USERPROFILE%\.gradle` (Speedup + verhindert Locks) — NICHT Defender deaktivieren;
(3) Android Studio bei CLI-Builds schliessen.
**Quelle:** github.com/gradle/gradle/issues/26912

### 11.3 "JAVA_HOME is set to an invalid directory"
**Symptom:** `gradlew.bat` bricht beim Start ab, obwohl ein JDK installiert ist.
**Ursache:** `JAVA_HOME` mit Anfuehrungszeichen (Quotes gehoeren NICHT in die Windows-Env-Var),
trailing Backslash, oder Pfad auf ein deinstalliertes JDK.
**Versionen:** Windows, alle.
**FIX:** `JAVA_HOME` ohne Quotes, ohne trailing Backslash, auf den JDK-Root (nicht `\bin`); Terminal neu
starten. Reproduzierbarer: Java Toolchains (`kotlin { jvmToolchain(21) }`).
**Quelle:** docs.gradle.org/current/userguide/toolchains.html

### 11.4 JDK-Mismatch IDE vs. Kommandozeile
**Symptom:** Build laeuft in Android Studio, scheitert in der CLI (oder umgekehrt); doppelte Daemons, hoher RAM.
**Ursache:** Android Studio nutzt sein embedded JDK (Gradle-JVM-Setting), die CLI `JAVA_HOME`/System-Default
— verschiedene JDKs/Gradle-Versionen → je ein eigener Daemon.
**Versionen:** alle, oft Windows.
**FIX:** Gradle-JVM in Android Studio auf dasselbe JDK wie `JAVA_HOME` setzen (ideal Toolchains); bei
Verwirrung `gradlew --stop`.
**Quelle:** developer.android.com/build/jdks

### 11.5 `-Dfile.encoding=UTF-8` allein ist unzuverlaessig  ⭐ (betrifft euer Setup)
**Symptom:** Umlaute in Java-/Kotlin-Strings oder Kommentaren landen als Muellzeichen in der `.class`,
obwohl die Quelle UTF-8 ist.
**Ursache:** Der Java-Compiler nutzt den System-Default-Charset (Windows: **Cp1252**), wenn nicht explizit
gesetzt. `-Dfile.encoding=UTF-8` als Sysprop in `org.gradle.jvmargs` steuert nur die Daemon-JVM, nicht
zwingend die `JavaCompile`-Tasks. (Beide Projekte verlassen sich aktuell allein darauf.)
**Versionen:** Windows; gradle#19235.
**FIX (funktionserhaltend, zuverlaessig):** Encoding pro Task setzen —
`tasks.withType<JavaCompile> { options.encoding = "UTF-8" }`. Danach `gradlew --stop` (alter Daemon haelt
altes Encoding). Das vorhandene `-Dfile.encoding` schadet nicht, ersetzt aber `options.encoding` nicht.
**Quelle:** github.com/gradle/gradle/issues/19235

### 11.6 CRLF in `gradlew` → "bad interpreter: /bin/bash^M"
**Symptom:** Das Bash-Skript `gradlew` (nicht `.bat`) scheitert auf Linux/CI mit `bad interpreter: /bin/bash^M`.
**Ursache:** `core.autocrlf=true` auf Windows wandelt LF→CRLF; das `\r` macht den Shebang kaputt.
**Versionen:** Windows-Ursache, Linux/CI-Symptom.
**FIX:** `.gitattributes` im Repo-Root: `* text=auto eol=lf`, `gradlew text eol=lf`, `*.bat text eol=crlf`.
`gradlew` MUSS LF, `gradlew.bat` MUSS CRLF haben.
**Quelle:** docs.github.com (line endings)

---

## 12. Fix-Status (per `gh` hart geprueft am 2026-06-02, ergaenzt 2026-06-24)

> Ehrlichkeits-Hinweis: GitHub-Issues wurden per `gh issue view` (echter OPEN/CLOSED-Status) verifiziert.
> Google-IssueTracker-Eintraege (R8/AGP intern) sind ueber `gh` nicht pruefbar — fuer die gilt der
> AGP-Release-Notes-Stand. "per Design"-Eintraege sind kein Bug, sondern dokumentiertes Verhalten.

### Belegt gefixt / geschlossen

| Frueherer Bug | Status | Bezug |
|---------------|--------|-------|
| KSP incremental bricht bei Dep-Aenderung (§8.2) | CLOSED COMPLETED 2025-01-30 — **Fix erst NACH 1.0.29** → eure Version noch betroffen | ksp#2252 |
| KSP + Build-Cache restauriert geloeschte Klassen (§8.4) | CLOSED COMPLETED 2024-09-05 | ksp#2042 |
| Room Cache-Miss mit KSP2 | CLOSED COMPLETED 2025-08-04 | ksp#2467 |
| Dagger/Hilt inkompatibel mit KSP2 | CLOSED COMPLETED 2024-12-20 | dagger#4303 |
| Hilt 2.59 ComponentTreeDeps fehlt (AGP 9.0) | CLOSED COMPLETED 2026-02-12 | dagger#5099 |
| Hilt-Plugin + AGP 9.0.0-alpha | CLOSED COMPLETED 2026-01-20 | dagger#4944 |
| Hilt JavaPoet/KotlinPoet NoSuchMethodError | CLOSED COMPLETED 2025-09-23 | dagger#4976 |
| Hilt 2.56.2 ZipException | CLOSED COMPLETED 2025-07-16 | dagger#4803 |
| Gradle 8.14 bricht AGP Code-Coverage | CLOSED COMPLETED 2025-05-21 | gradle#33389 |
| jcenter() in 9.0 entfernt (§4.2) | CLOSED COMPLETED 2025-08-12 | gradle#34504 |
| Retrofit R8-fullMode keep-Rules (§7.1) | CLOSED COMPLETED (Libs liefern eigene Rules) | retrofit#3005/#3751 |
| `Task.project` Execution-Zeit (§5.1) | CLOSED COMPLETED 2023-03-22 (Plugin-seitig) | spotbugs#670 |
| R8 optimizedResourceShrinking-Crash, konkret (§7.5) | CLOSED COMPLETED 2025-10-10 | mikepenz/AboutLibraries#1239 |
| KSP2 Modul-Recompile auf Windows (§8.2b) | CLOSED COMPLETED 2026-02-17 — Fix in KSP 2.3.6 | ksp#2774 |
| KSP Kotlin-Target auf 2.3 angehoben | CLOSED COMPLETED 2026-03-18 — in KSP 2.3.7 | ksp#2821 |
| KSP `ksp.project.isolation` Default bei Isolated Projects | CLOSED COMPLETED 2026-04-23 | ksp#2866 |
| AGP 9.0: kein `onVariant`-Aequivalent fuer `mergeAssetsProvider` | **gefixt in AGP 9.2.0** (Google-Tracker, NICHT per gh pruefbar) | issuetracker#477562205 |
| AGP 9.0: `androidDeviceTest` + Manifest-Placeholders in KMP-Library | **gefixt in AGP 9.2.0** (Google-Tracker, NICHT per gh pruefbar) | issuetracker#482293927 |

### Noch NICHT gefixt — Workaround bleibt aktiv

| Aktiver Bug | Status | Bezug |
|-------------|--------|-------|
| **jvmargs-Override → "Daemon disappeared"** (§6.1) | **OPEN / per Design** — MaxMetaspaceSize selbst setzen | gradle#19750 |
| Daemon-Crash Low-Memory JDK 21 (§6.2) | **OPEN** | gradle#29958 |
| Wrapper-Upgrade 8.x→9.0 Versionsschema (§4.5) | **OPEN** | gradle#34968 |
| MAX_PATH 260 keine Auto-Mitigation (§11.1) | **OPEN** | gradle#1989 |
| KSP incremental (eure Version, §8.2) | **praktisch offen fuer 1.0.29** | ksp#2252 |
| KSP generierte Sources nach incremental fehlen (§8.3) | **OPEN** | ksp#2536 |
| KSP2 OOM (Kotlin 2.3.20) (§8.9a) | **OPEN** | ksp#2817 |
| KSP2 ~20–30 % langsamer (§8.9b) | **OPEN** | ksp#2282 |
| KSP erster assembleDebug scheitert (§8.9c) | **OPEN** | ksp#1678 |
| Hilt KSP2 PSI lifetime token | **OPEN** | ksp#2545 |
| R8 strippt @HiltViewModel-Ctor (§7.8) | **OPEN** | dagger#4739 |
| Hilt + `com.android.legacy-kapt` (§8.10) | **OPEN** | dagger#4756 |
| KSP Perf-Regression `PsiResolutionStrategy` (in 2.3.8 eingefuehrt; 2.3.x-Linie) | **OPEN** (gh-geprueft 2026-06-24) | ksp#2948 |

### "won't fix" / per Design (Workaround dauerhaft)

| Thema | Status | Bezug |
|-------|--------|-------|
| KSP + KAPT + CC (§8.5) | CLOSED NOT_PLANNED 2026-04-07 — KAPT raus | ksp#1805 |
| Korrupter CC persistiert (§5.4) | CLOSED NOT_PLANNED 2023-02-24 — CC verwerfen | gradle#23802 |
| CC sensitiv auf Property-Werte (§5.3) | CLOSED DUPLICATE 2025-02-03 — erwartetes Verhalten | gradle#32219 |
| parallel clean/build Race (§6.4) | CLOSED NOT_PLANNED — `clean` separat | gradle#15163 |

---

## 13. Signing & adb-Install (Debug-Keystore ueber mehrere Rechner)

### 13.1 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — Debug-Keystore-Mismatch zwischen Rechnern  ⭐ SELBST ERLEBT
**Symptom:** `adb install -r` schlaegt fehl: "Existing package ... signatures do not match newer version; ignoring!" — obwohl es ein normaler Debug-Build derselben App ist.
**Ursache:** Jede Maschine signiert Debug-Builds mit ihrem eigenen Keystore (Default `~/.android/debug.keystore` wird pro Maschine generiert). Ein "geteilter" Keystore in `~/SK/<projekt>/` hilft nur, wenn die DATEI auf allen Maschinen BYTE-IDENTISCH ist — eine namensgleiche, aber separat erzeugte Kopie hat einen anderen Key.
**Diagnose (2 Minuten, bevor irgendetwas geloescht wird):**
```bash
adb shell pm path <paket>            # → /data/app/.../base.apk
adb pull <pfad> /tmp/installed.apk
apksigner verify --print-certs /tmp/installed.apk     # Cert der INSTALLIERTEN App
keytool -list -keystore <kandidat>.keystore -storepass android   # Cert der Kandidaten
```
Fingerprints vergleichen — dann ist klar, WELCHER Rechner die installierte App signiert hat.
**FIX (funktionserhaltend, in dieser Reihenfolge):**
1. Den Keystore der Maschine besorgen, die die installierte App signiert hat → in `~/SK/<projekt>/` ALLER Maschinen ablegen (eine Wahrheit) → normales Update, kein Datenverlust.
2. NUR wenn der fremde Keystore unerreichbar ist UND die Daten gesichert sind (z.B. Drive-Backup): mit Nutzer-OK deinstallieren + frisch installieren, danach Backup wiederherstellen. NIEMALS reflexhaft `adb uninstall`.
**Poka-Yoke:** Nach jedem Keystore-Setup auf einer neuen Maschine: `keytool -list`-Fingerprint mit dem der anderen Maschine vergleichen (muss identisch sein). Vorfall: EntropieReductor 0.13.0, 2026-06-12 (Mac vs. Windows, Handy-App war Windows-signiert).
**Versionen:** plattformuebergreifend, zeitlos (Android-Signatur-Modell).

---

## ✅ Pflicht-Checkliste (vor & beim Build-Edit)

- [ ] Diese Datei gelesen, Stand-Datum gegen die live ermittelten Versionen (Gradle/AGP/KSP/JDK) abgeglichen?
- [ ] AGP-Bump geplant? → Kompatibilitaetsmatrix (§1) geprueft: Mindest-Gradle + JDK passen?
- [ ] AGP-9.0-Upgrade? → §2 als EIN Block (newDsl, proguard-android.txt, strictFullMode, Variant-API,
      Defaults, useAndroidX/Jetifier raus, Gradle 9.1.0) + KSP→useKSP2 (§8.1) + `kotlin.md` §10.
- [ ] `org.gradle.jvmargs` gesetzt? → `-XX:MaxMetaspaceSize` mit drin (§6.1)?
- [ ] Windows: `options.encoding = "UTF-8"` pro JavaCompile (§11.5)? Defender-Ausnahme fuer `build/`+`.gradle/` (§11.2)?
- [ ] Release-Build (R8): keep-Rules eng, dynamische Ressourcen per `keep.xml` geschuetzt, Release-Variante
      real getestet (§7)?
- [ ] KSP: bei "stale generated code" reflexartig `clean` (§8.2); EntropieReductor `ksp.incremental=true` im Blick.
- [ ] Configuration Cache: EntropieReductor bleibt bewusst `false` (§5.2) — nicht blind aktivieren.
- [ ] Version-Catalog/BOM: BOM-gemanagte Libs ohne eigene Version, `platform(...)` + BOM auf jeder
      Configuration (§9)?
- [ ] Neuen, selbst erlebten Build-Bug nach der Arbeit hier ergaenzt (Bug + funktionserhaltende Loesung + Versionen)?

---

## 🔗 Bezug: Best-Practices-Gegenstuecke

> Zweite Seite der Medaille: `~/proggs/best-practices/android-build/gradle.md` sammelt
> *wie man es von vornherein richtig macht*. Jede Bug-Sektion hier hat dort ihr praeventives Gegenstueck.

| Bug-Sektion (diese Datei) | Best-Practice-Gegenstueck (`best-practices/android-build/gradle.md`) |
|---|---|
| §1 Versions-Kompatibilitaet | §1 Versions-Strategie & Kompatibilitaet |
| §2 AGP 9.0 Breaking Changes | §9 Upgrade-Strategie (+ §4 R8-Defaults) |
| §4 Gradle 9.0 Core-Migration | §1, §9 Upgrade-Strategie |
| §5 Configuration Cache | §3 Configuration Cache richtig einfuehren |
| §6 Build Cache / Daemon / Performance | §2 `gradle.properties` Build-Flags |
| §7 R8 / ProGuard / Shrinking | §4 R8/Shrinking richtig konfigurieren |
| §8 KSP als Gradle-Plugin | §5 KSP richtig einsetzen |
| §9 Dependency-Resolution / Catalogs / BOM | §6 Version-Catalogs & Dependency-Management |
| §10 Gradle Wrapper | §7 Gradle Wrapper absichern |
| §11 Windows-Build-Fallen | §8 Windows-Build-Hygiene |
