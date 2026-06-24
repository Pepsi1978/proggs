# Android-Build-System (Gradle / AGP / R8 / KSP) — Best Practices

> **Zweite Seite der Medaille zum Bug-Almanach** `~/proggs/bugs/android-build/gradle.md`: dort steht *was
> schiefgeht und wie man es umgeht*, hier *wie man es von vornherein richtig macht, damit der
> Bug gar nicht erst entsteht*. Jeder Abschnitt verweist auf seinen Bug-Gegenpart (Bezugs-Tabelle).
>
> **Stand:** recherchiert 2026-06-02 (offizielle Gradle-/Android-/KSP-Quellen).
>
> | | benutzt (live) | aktuell verfuegbar (2026-06-02) |
> |---|---|---|
> | Gradle (Wrapper) | **8.11.1** (beide Projekte) | **9.5.1** (letztes 8.x: 8.14.5) |
> | AGP | BestJournal **8.7.3** / Entropie **8.10.0** | **9.2.0** |
> | Kotlin / KSP | 2.1.0 / **2.1.0-1.0.29 (KSP1)** | KSP2 Default seit KSP 2.0.0 |
> | JDK (Daemon) | 21 | — |
> | Compose BOM | BestJournal **2025.01.01** / Entropie 2026.03.00 | **2026.05.00** |
> | Firebase BOM | 34.11.0 (BestJournal) | 34.13.0 |
>
> **Wichtig (Policy):** BestJournalAndroid geht in den **Play Store** → Upgrade-Pfad relevant.
> EntropieReductor ist eine **private App** (Memory `project_entropie_reductor_private_app`) →
> Build-Tool-Upgrades NICHT ungefragt; bleibt bewusst auf Kotlin 2.1 / AGP 8.10.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | `org.gradle.jvmargs` setzen | IMMER `-XX:MaxMetaspaceSize=1g` mit drin | §2.1 |
| 2 | Windows-Encoding | `options.encoding="UTF-8"` pro JavaCompile, nicht nur `-Dfile.encoding` | §2.5 |
| 3 | JDK festnageln | Java-Toolchain `jvmToolchain(21)` statt `JAVA_HOME` | §1.2 |
| 4 | AGP-Bump | Matrix VOR Bump, erst Gradle-Wrapper, dann AGP | §1.1 |
| 5 | Configuration Cache + Datei-Read | `providers.fileContents().asText` statt `readText()` | §3.2 |
| 6 | R8-Basis | `proguard-android-optimize.txt`, fullMode an | §4.1 |
| 7 | keep-Regel | so eng wie moeglich, Ctor/Member explizit | §4.2, §4.3 |
| 8 | Dynamisch geladene Ressourcen | per `res/raw/keep.xml` schuetzen (AGP 9 Default) | §4.5 |
| 9 | Release testen | R8-Crashes nur im Release sichtbar — real testen | §4.8 |
| 10 | KSP-Suffix | an Kotlin-Version koppeln, beim Bump mitbumpen | §5.2 |
| 11 | BOM | via `platform(...)`, gemanagte Libs ohne Version, auf JEDER Config | §6.2 |
| 12 | Defender (Windows) | Echtzeit-Ausnahme `build/` + `.gradle/`, `gradlew --stop` vor clean | §8.2 |
| 13 | AGP 9 / Kotlin 2.3 / KSP2 | als EIN gebuendelter Block planen | §9.1, §9.2 |
| 14 | Private App (Entropie) | KEINE Build-Tool-Upgrades ungefragt (Policy) | §9.3 |

---

## Quellen-Rangordnung

Offizielle Quellen (docs.gradle.org, developer.android.com, blog.gradle.org, github.com/google/ksp,
android-developers.googleblog.com) = **Grundwahrheit**. Community/Blogs = `extern`-Alternative,
ueberstimmt nie das Offizielle. Jeder Eintrag traegt Quelle + Datum + `[offiziell]`/`[extern]`.

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei) | Failure-Mode im Almanach `bugs/android-build/gradle.md` |
|---|---|
| §1 Versions-Strategie & Kompatibilitaetsmatrix | §1 (1.1–1.3), §2 (AGP 9.0), §4 (Gradle 9.0) |
| §2 `gradle.properties` — empfohlene Build-Flags | §6.1 jvmargs/Metaspace, §6.4 parallel, §11.5 Encoding |
| §3 Configuration Cache richtig einfuehren | §5 (5.1–5.5) |
| §4 R8 / Shrinking / Obfuskation richtig konfigurieren | §7 (7.1–7.10), §2.3/§2.4 (AGP-9-R8-Defaults) |
| §5 KSP richtig einsetzen | §8 (8.1–8.10) |
| §6 Version-Catalogs & Dependency-Management | §9 (9.1–9.12) |
| §7 Gradle Wrapper absichern | §10.1, §11.6 (CRLF) |
| §8 Windows-Build-Hygiene | §11 (11.1–11.6) |
| §9 Upgrade-Strategie (AGP 9 / Kotlin 2.3 / KSP2) | §1, §2, §4, §8.1 + Memory `project_agp9_kotlin23_upgrade` |
| §10 Projektspezifische Ableitungen | quer ueber alle |

---

## 1. Versions-Strategie & Kompatibilitaet

**1.1 AGP diktiert Mindest-Gradle und Mindest-JDK — Matrix VOR jedem Bump pruefen** `[offiziell]`
Reihenfolge beim Upgrade IMMER: erst Gradle-Wrapper, dann AGP. Korridor (Auszug): AGP 8.7→Gradle ≥8.9,
AGP 8.10→Gradle ≥8.11.1, AGP 9.0→Gradle ≥9.1.0, AGP 9.1→Gradle ≥9.3.1; JDK 17 ist ab AGP 8.0 Pflicht.
Den **AGP Upgrade Assistant** nutzen (kennt die Matrix). Quelle: developer.android.com/build/releases/about-agp,
docs.gradle.org/current/userguide/compatibility.html (2026-05). → Bug §1.1/§1.2.

**1.2 JDK ueber Java-Toolchain festnageln statt ueber `JAVA_HOME`** `[offiziell]`
`kotlin { jvmToolchain(21) }` (bzw. `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }`)
macht den Build reproduzierbar — Gradle waehlt das JDK selbst, unabhaengig von `JAVA_HOME` und IDE-Setting.
Verhindert „JDK-Mismatch IDE vs. CLI" und doppelte Daemons. Quelle: docs.gradle.org/current/userguide/toolchains.html
(2026-05). → Bug §1.2/§11.3/§11.4.

**1.3 compileSdk nicht hoeher als die AGP-Version unterstuetzt** `[offiziell]`
compileSdk 36 braucht AGP 8.9/8.10+. Lieber AGP anheben als die Warnung mit
`android.suppressUnsupportedCompileSdk` zu unterdruecken. Quelle: developer.android.com/build/releases. → Bug §1.3.

**1.4 Latest ≠ stable beim ganzen Toolchain-Sprung** `[offiziell]`
Bei AGP/Kotlin/KSP gemeinsam erst die Kompatibilitaet ALLER beteiligten Prozessoren (KSP, Room, Hilt)
verifizieren, nicht blind die neueste Version ziehen. Quelle: kotlinlang.org/docs/compatibility-guide-23.html
(2026-01). → Bug §8.1/§8.9.

---

## 2. `gradle.properties` — empfohlene Build-Flags

**2.1 `org.gradle.jvmargs` IMMER mit `-XX:MaxMetaspaceSize`** `[offiziell]` ⭐
Sobald `org.gradle.jvmargs` gesetzt ist, verliert Gradle ALLE Default-JVM-Args (inkl.
`-XX:MaxMetaspaceSize=256m`) → unbegrenztes Metaspace-Wachstum → „Daemon disappeared". Empfehlung:
`org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8`. Quelle:
github.com/gradle/gradle/issues/19750 `[offiziell-tracker]`. → Bug §6.1 (betrifft BEIDE Projekte aktuell).

**2.2 `org.gradle.parallel=true` + `org.gradle.caching=true` aktivieren** `[offiziell]`
Offiziell empfohlene, risikoarme Performance-Schalter. EntropieReductor hat beide; **BestJournalAndroid
hat KEINEN** → nachziehen. Einzige Vorsicht: `clean` nicht parallel mit dem Build mischen. Quelle:
docs.gradle.org/current/userguide/performance.html, developer.android.com/build/optimize-your-build (2026-05).
→ Bug §6.4.

**2.3 File-System-Watching (`org.gradle.vfs.watch=true`)** `[offiziell]`
Default an seit Gradle 7, aber explizit setzen schadet nicht; beschleunigt inkrementelle Builds spuerbar.
Quelle: docs.gradle.org/current/userguide/file_system_watching.html (2026-05).

**2.4 `ksp.incremental=true`** `[offiziell]`
Empfohlener Default fuer schnellere KSP-Laeufe. EntropieReductor hat es; BestJournal koennte es ergaenzen
(im Blick: Incremental-Bug §8.2 → bei „stale code" reflexartig `clean`). Quelle: kotlinlang.org/docs/ksp-incremental.html
(2026-05). → Bug §8.2.

**2.5 Encoding zuverlaessig pro Task setzen — NICHT nur `-Dfile.encoding`** `[offiziell]` ⭐ (Windows)
`-Dfile.encoding=UTF-8` in jvmargs steuert nur die Daemon-JVM, nicht zwingend die `JavaCompile`-Tasks
(Windows-Default: Cp1252). Zusaetzlich `tasks.withType<JavaCompile> { options.encoding = "UTF-8" }` setzen.
Quelle: github.com/gradle/gradle/issues/19235 `[offiziell-tracker]`. → Bug §11.5.

---

## 3. Configuration Cache (CC) richtig einfuehren

**3.1 Reifegrad nutzen: CC ist ab Gradle 9.0 „preferred", Default ab Gradle 10** `[offiziell]`
Inkrementell einfuehren: erst `org.gradle.configuration-cache=true`, Probleme per
`--configuration-cache-problems=warn` finden, dann erst `configuration-cache.parallel` testen. Quelle:
docs.gradle.org/current/userguide/configuration_cache.html, blog.gradle.org/road-to-configuration-cache (2026-05).
→ Bug §5.5.

**3.2 Konfigurationszeit-Inputs ueber die Provider-API lesen — nie `readText()`** `[offiziell]` ⭐
Der EntropieReductor-Fall (`skMapsKeyFile.readText()` zur Konfigurationszeit) ist genau der Grund fuer
`configuration-cache=false`. Loesung **funktionserhaltend**: Datei/Property als CC-tauglichen Input deklarieren —

```kotlin
// statt: val mapsApiKey = skMapsKeyFile.readText().trim()
val mapsApiKey: Provider<String> =
    providers.fileContents(layout.projectDirectory.file(skMapsKeyFile.absolutePath))
        .asText.map { it.trim() }
        .orElse("")
// Wert spaeter via .get() in den manifestPlaceholder
```

Alternativ eine `ValueSource`-Implementierung (fuer Logik jenseits eines reinen Datei-Reads). Damit kann
EntropieReductor CC **doch** aktivieren, ohne das Key-Einlesen zu entfernen. Quelle:
docs.gradle.org/current/userguide/configuration_cache_requirements.html,
docs.gradle.org/current/javadoc/org/gradle/api/provider/ValueSource.html (2026-05). → Bug §5.2
(ergaenzt Memory `reference_entropie_config_cache_false`: deaktiviert ist EIN gueltiger Weg — der ValueSource-Weg
ist der bessere, sobald CC-Gewinn gewuenscht ist).

**3.3 Werte nicht im `@TaskAction` ueber `project` ziehen** `[offiziell]`
Benoetigte Werte zur Konfigurationszeit in serialisierbare Provider/Felder extrahieren
(`project.layout.buildDirectory`, `providers.*`). Quelle: docs.gradle.org/current/userguide/configuration_cache_debugging.html.
→ Bug §5.1.

---

## 4. R8 / Shrinking / Obfuskation richtig konfigurieren (Release)

**4.1 `proguard-android-optimize.txt` als Basis, fullMode an lassen** `[offiziell]`
fullMode ist seit AGP 8.0 Default und soll an bleiben (kleinere/schnellere App). Basis-Datei IMMER die
`-optimize`-Variante (die schlichte `proguard-android.txt` ist ab AGP 9.0 verboten). **Beide Projekte
machen das korrekt.** Quelle: developer.android.com/build/shrink-code,
developer.android.com/topic/performance/app-optimization/full-mode (2026-05). → Bug §7.1, §2.3.

**4.2 keep-Regeln so eng wie moeglich** `[offiziell]`
`-keep class com.foo.** { *; }` verbietet shrink+obfuscate+optimize fuer ganze Pakete → App schrumpft nicht.
Nur konkrete Reflection-/JNI-Entrypoints keepen. Quelle:
android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html (2025-11). → Bug §7.7.

**4.3 Konstruktor/Member explizit keepen, nicht nur die Klasse** `[offiziell]`
fullMode behaelt den No-Args-Ctor NICHT implizit (verschaerft durch `strictFullModeForKeepRules` ab AGP 9.0):
`-keep class X { <init>(); }`. Fuer Reflection auf Felder: `-keep class X { private <fields>; }` (erhaelt
Member UND Sichtbarkeit). Quelle: developer.android.com/topic/performance/app-optimization/full-mode (2026-05).
→ Bug §7.2/§7.3/§2.4.

**4.4 `-keepattributes Signature,InnerClasses,EnclosingMethod` fuer generische (De-)Serialisierung** `[offiziell]`
Moshi/Gson/Retrofit verlieren in fullMode sonst generische Typinfo. Aktuelle Lib-Versionen liefern eigene
consumer-Rules — trotzdem bewusst pruefen. Quelle: developer.android.com/topic/performance/app-optimization/full-mode.
→ Bug §7.1.

**4.5 Dynamisch (per Name/`getIdentifier()`) geladene Ressourcen per `res/raw/keep.xml` schuetzen** `[offiziell]` ⭐
Optimized Resource Shrinking (Default ab AGP 9.0) entfernt sonst dynamisch referenzierte Ressourcen.
`<resources tools:keep="@drawable/foo,@layout/dyn_*"/>` statt das Feature abzuschalten. **Vor dem AGP-9-Upgrade
pruefen, ob die Apps Ressourcen per Name laden.** ⚠️ Im `tools:keep`-Eintrag den Namen OHNE Dateiendung
nennen (`@raw/foo`, nicht `@raw/foo.json`) — die Endung war die Falle im AboutLibraries-Fall (#1239).
`tools:shrinkMode="safe"` (Default) versucht `getIdentifier()`-Referenzen zu erhalten; was fehlt, zeigt
`build/outputs/mapping/release/resources.txt`. Quelle:
developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep ·
github.com/mikepenz/AboutLibraries/issues/1239. → Bug §7.5.

**4.6 `missing_rules.txt` gezielt uebernehmen — kein `-dontwarn`-Flood** `[offiziell]`
Die in `build/outputs/mapping/<variant>/missing_rules.txt` empfohlenen Regeln nach `proguard-rules.pro`
uebernehmen, iterativ rebuilden. Blindes `-dontwarn` kann Laufzeit-Crashes verdecken. Quelle:
developer.android.com/build/shrink-code. → Bug §7.4.

**4.7 Pro Release-Version `mapping.txt` hochladen + Retrace** `[offiziell]`
`mapping.txt` (im AAB automatisch enthalten) deobfuskiert nur Crashes NACH dem Upload — fuer JEDE Version
eigene Map. Lokal: `retrace` / `r8 --deobfuscate-stack-trace`. Ab AGP 9.3: **R8 Configuration Analyzer**
(`configanalyzer.html`) zeigt, welche keep-Regel die Optimierung blockiert. Quelle:
support.google.com/googleplay/android-developer/answer/9848633,
developer.android.com/topic/performance/app-optimization/r8-configuration-analyzer. → Bug §7.10.

**4.8 Release-Variante IMMER real testen (nie nur Debug)** `[offiziell]`
R8 laeuft nur im Release-Build → Reflection-/DI-/JNI-Crashes (z.B. `@HiltViewModel`-Ctor, sherpa-onnx JNI)
zeigen sich erst dort. Bei Billing/Paywall ohnehin Pflicht (Memory `feedback_billing_release_only`). → Bug §7.8.

---

## 5. KSP richtig einsetzen

**5.1 KSP statt KAPT — KAPT ist im Wartungsmodus** `[offiziell]`
KSP ist schneller (on-demand Typaufloesung, feineres inkrementelles Processing, KMP-tauglich). Seit Kotlin
2.2.20 nutzt KAPT default K2; echte Weiterentwicklung gibt es nur bei KSP. **Beide Projekte sind KAPT-frei** →
korrekt. Quelle: kotlinlang.org/docs/ksp-why-ksp.html, kotlinlang.org/docs/ksp-faq.html (2025-02). → Bug §8.5/§8.10.

**5.2 KSP-Suffix an die Kotlin-Version koppeln, beim Kotlin-Bump IMMER KSP mitbumpen** `[offiziell]`
Format `KotlinVer-KSPVer` (`2.1.0-1.0.29`): Der Kotlin-Prefix MUSS zur benutzten Kotlin-Version passen, sonst
„ksp-… is too old for kotlin-…". **Unsere `2.1.0-1.0.29` passt zu Kotlin 2.1.0** — korrekt. Quelle:
kotlinlang.org/docs/ksp-faq.html (2025-02-23). → Bug §8.1, kotlin.md §10.1.

**5.3 KSP2-Umstieg VOR dem Kotlin-2.3-Sprung erproben** `[offiziell]`
KSP2 ist Default seit KSP 2.0.0; KSP1 ist mit Kotlin 2.3 / AGP 9.0 inkompatibel. Schon unter Kotlin 2.1 per
`ksp.useKSP2=true` testbar (Rueckfall `=false`) → entkoppelt das Risiko vom Versionssprung. Quelle:
github.com/google/ksp/blob/main/docs/ksp2.md (2026). → Bug §8.1.

**5.4 Bei KSP2 den Heap erhoehen** `[offiziell]`
KSP2 laeuft im Gradle-Daemon (frueher Kotlin-Compiler-Daemon mit groesserem Default-Heap) → ggf.
`-Xmx4096M -XX:MaxMetaspaceSize=1024m`. Quelle: android-developers.googleblog.com/2023/12 (KSP2 Preview).
→ Bug §8.9, §6.1.

**5.5 Prozessoren pro SourceSet konfigurieren** `[offiziell]`
KSP verarbeitet pro SourceSet getrennt: neben `ksp(...)` ggf. `kspTest(...)` / `kspAndroidTest(...)` fuer
Hilt-Test-Komponenten. Quelle: dagger.dev/hilt/gradle-setup.html. → Bug §8.7.

**5.6 Plugin-Apply-Reihenfolge einhalten** `[offiziell]`
`com.android.application` → `org.jetbrains.kotlin.android` → `com.google.devtools.ksp`. **Beide Projekte
korrekt.** Quelle: kotlinlang.org/docs/ksp-quickstart.html. → Bug §8.8.

---

## 6. Version-Catalogs & Dependency-Management

**6.1 ALLE Versionen zentral im Catalog — keine inline-Versionen** `[offiziell]`
`gradle/libs.versions.toml` ist die einzige Versions-Quelle. Inline-Versionen im `build.gradle.kts`
(z.B. `play-services-auth:21.3.0` in BestJournal) in `[versions]`+`[libraries]` ziehen. Quelle:
docs.gradle.org/current/userguide/best_practices_dependencies.html (2026). → Bug §9.2.

**6.2 BOM via `platform(...)`, gemanagte Libs OHNE eigene Version** `[offiziell]`
`implementation(platform(libs.compose.bom))`; Compose/Firebase-Libs dann ohne Version. BOM auf JEDER
Configuration deklarieren (auch `androidTestImplementation`). Eigene Version im Catalog uebersteuert das BOM
immer — nur bewusst. Quelle: developer.android.com/develop/ui/compose/bom (2026-05-18). → Bug §9.3/§9.4/§9.5.

**6.3 BOMs aktuell halten** `[offiziell]`
Ein veraltetes BOM friert Bugs/fehlende Features ein. **BestJournal Compose BOM 2025.01.01 ist ~16 Monate alt**
→ in einem Schritt auf 2026.05.00, danach Build + UI-Smoke-Test + Release-R8-Test. Firebase 34.11.0→34.13.0
(kleiner Sprung). Quelle: developer.android.com/develop/ui/compose/bom. → Bug §9.7.

**6.4 Compose-Compiler folgt der Kotlin-Version, NICHT dem BOM** `[offiziell]`
Plugin `org.jetbrains.kotlin.plugin.compose` an die Kotlin-Version binden — das BOM steuert nur Runtime-Libs.
Quelle: developer.android.com/develop/ui/compose/bom. → Bug §9.6, kotlin.md §10.2.

**6.5 Repositories zentral in `settings.gradle.kts`** `[offiziell]`
`dependencyResolutionManagement { repositories { google(); mavenCentral() } }`; keine Repos in Modul-Build-Files
(`FAIL_ON_PROJECT_REPOS` ist Android-Default). Quelle: developer.android.com/build/remote-repositories. → Bug §9.10/§9.11.

**6.6 alpha-Deps in der Store-App minimieren** `[offiziell/extern]`
Beta = API-stabil (produktionstauglich), Alpha = API kann brechen. BestJournal (Store) hat `credentials
1.5.0-alpha06`, `biometric 1.2.0-alpha05` → bei Verfuegbarkeit auf stable/beta. Entropie (privat) unkritischer.
Quelle: android.googlesource.com/.../versioning.md `[offiziell]`, jakewharton.com/you-should-use-androidx-betas `[extern]`.
→ Bug §9.7.

**6.7 Dependency-Verification fuer die Store-App erwaegen** `[offiziell]`
`./gradlew --write-verification-metadata sha256,pgp` haertet gegen Supply-Chain-Angriffe. Aktuell bei keinem
Projekt aktiv. Quelle: docs.gradle.org/current/userguide/dependency_verification.html. → Bug §9.12.

**6.8 Update-Tooling, das Version-Catalogs versteht** `[extern]`
Das verbreitete `ben-manes/gradle-versions-plugin` bricht mit Gradle 9 / AGP 8.12 → stattdessen
`nl.littlerobots.version.catalog.update` (schreibt direkt in `libs.versions.toml`) oder Renovate/Dependabot
(PR-basiert). Quelle: github.com/ben-manes/gradle-versions-plugin, github.com/littlerobots/version-catalog-update-plugin `[extern]`.
→ Bug §9 (allgemein).

---

## 7. Gradle Wrapper absichern

**7.1 `distributionSha256Sum` setzen** `[offiziell]`
`validateDistributionUrl=true` prueft nur die URL, nicht den Inhalt. Offiziellen SHA-256 von der
Gradle-Releases-Seite als `distributionSha256Sum` eintragen (bei Wrapper-Updates mit-aktualisieren). Quelle:
docs.gradle.org/current/userguide/gradle_wrapper.html. → Bug §10.1.

**7.2 `gradlew` muss LF, `gradlew.bat` muss CRLF haben** `[offiziell]`
`.gitattributes`: `* text=auto eol=lf`, `gradlew text eol=lf`, `*.bat text eol=crlf`. Sonst `bad interpreter:
/bin/bash^M` auf Linux/CI. Quelle: docs.github.com (line endings). → Bug §11.6.

**7.3 Wrapper VOR AGP heben; Zielversion exakt (`9.0.0`, nicht `9.0`)** `[offiziell]`
Beim Sprung auf Gradle 9 die exakte Version angeben bzw. `distributionUrl` direkt setzen. Quelle:
docs.gradle.org/current/userguide/upgrading_major_version_9.html. → Bug §4.5.

---

## 8. Windows-Build-Hygiene

**8.1 Metaspace + Encoding** → siehe §2.1 / §2.5 (die zwei haeufigsten Windows-Build-Crashes). → Bug §6.1/§11.5.

**8.2 Windows-Defender-Echtzeit-Ausnahme fuer `build/` + `%USERPROFILE%\.gradle`** `[offiziell]`
Verhindert File-Locks beim `clean`/Rebuild UND beschleunigt den Build — NICHT Defender deaktivieren, nur
Projektordner ausnehmen. Vor `clean`: `gradlew --stop`. Quelle: github.com/gradle/gradle/issues/26912. → Bug §11.2.

**8.3 MAX_PATH 260 im Blick** `[offiziell]`
Bei „Filename longer than 260 characters": Projekt naeher an den Laufwerks-Root, Long-Path-Support
(`LongPathsEnabled=1`). `C:\Users\barwa\proggs\...` ist grenzwertig lang. Quelle:
github.com/gradle/gradle/issues/1989. → Bug §11.1.

**8.4 `JAVA_HOME` ohne Quotes/trailing Backslash, auf den JDK-Root** `[offiziell]`
Besser: Java-Toolchain (§1.2) — dann ist `JAVA_HOME` fuer den Build irrelevant. Quelle:
docs.gradle.org/current/userguide/toolchains.html. → Bug §11.3/§11.4.

---

## 9. Upgrade-Strategie (AGP 9 / Kotlin 2.3 / KSP2)

**9.1 Den Sprung als EINEN gebuendelten Block planen** `[offiziell]`
Reihenfolge: Gradle-Wrapper → 9.1.0+ ⇒ AGP → 9.x ⇒ Kotlin → 2.3 (mit KSP-2.3.x-Suffix) ⇒ KSP2
(`ksp.useKSP2=true`) ⇒ Prozessoren (Room ≥2.7, Hilt KSP2-faehig ~2.56/2.57). Deckt sich mit Memory
`project_agp9_kotlin23_upgrade`. Quelle: blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9
(2026-01), developer.android.com/build/releases/agp-9-0-0-release-notes. → Bug §2, §4, §8.1.

**9.2 AGP-9.0-Checkliste vor dem Schnitt** `[offiziell]`
`proguard-android-optimize.txt` ✓ (beide ok); `compilerOptions {}` ✓ (beide ok); `useAndroidX`/`enableJetifier`
aus `gradle.properties` entfernen; Build-Logic auf `androidComponents`-API; `strictFullModeForKeepRules` →
keep-Ctor praezisieren (§4.3); dynamische Ressourcen per `keep.xml` (§4.5); `targetSdk` explizit setzen.
`kotlin-kapt` → **`com.android.legacy-kapt`** oder KSP (built-in Kotlin bricht `kotlin-kapt`); ungueltige
proguard-Eintraege entfernen (`proguard.failOnMissingFiles=true` ab 9.0). Gebuendelte AGP-9.0-Versionen:
KGP 2.2.10, KSP 2.2.10-2.0.2 (KSP2 min **2.3.1**, deprecated `compilerOptions`-API erst in **2.3.3** geloest),
Build-Tools 36.0.0, NDK 28.2.x, compileSdk max 36.1. (Recherche-Update 2026-06-19 → Bug §2.6.)
Quelle: developer.android.com/build/releases/agp-9-0-0-release-notes (2026-01). → Bug §2.1–§2.9.

**9.3 Nur die Store-App upgraden, die private App bewusst zuruecklassen** `[Policy]`
BestJournalAndroid (Store) folgt dem Upgrade-Pfad. EntropieReductor (privat) bleibt auf Kotlin 2.1 / AGP 8.10 —
keine Upgrade-Vorschlaege ungefragt (Memory `project_entropie_reductor_private_app`).

---

## 10. Projektspezifische Ableitungen (Stand 2026-06-02)

**BestJournalAndroid (Play Store):**
1. `gradle.properties` haerten: `-XX:MaxMetaspaceSize=1g` ergaenzen (§2.1), `org.gradle.parallel=true` +
   `org.gradle.caching=true` setzen (fehlen komplett, §2.2), optional `ksp.incremental=true` (§2.4).
2. Compose BOM 2025.01.01 → 2026.05.00 anheben (§6.3) + Release-R8-Smoke-Test.
3. Inline-`play-services-auth:21.3.0` in den Catalog ziehen (§6.1).
4. `options.encoding="UTF-8"` pro JavaCompile (§2.5), `distributionSha256Sum` (§7.1).
5. Upgrade-Pfad AGP 9 / Kotlin 2.3 / KSP2 als ein Block einplanen (§9) — Store-relevant.
6. alpha-Deps (`credentials`, `biometric`) auf stable/beta beobachten (§6.6).

**EntropieReductor (privat):**
1. `-XX:MaxMetaspaceSize` in jvmargs ergaenzen (§2.1) + `options.encoding` (§2.5) — die einzigen risikoarmen
   Verbesserungen, KEINE Build-Tool-Upgrades (Policy §9.3).
2. Configuration Cache ist **doch aktivierbar**: Maps-Key per `providers.fileContents().asText` statt
   `readText()` lesen (§3.2) → `configuration-cache=true` moeglich, funktionserhaltend. Optionaler Performance-Gewinn,
   kein Muss.
3. `ksp.incremental=true` ist gesetzt → bei „stale generated code" reflexartig `clean` (Bug §8.2, anfaellig).

---

## Quellen (Auswahl, 2026-06-02)

- Gradle: docs.gradle.org/current/userguide/{performance,configuration_cache,configuration_cache_requirements,
  build_cache_performance,upgrading_major_version_9,gradle_wrapper,toolchains,dependency_verification}.html;
  gradle.org/releases; blog.gradle.org/road-to-configuration-cache; blog.gradle.org/best-practices-naming-version-catalog-entries
- Android: developer.android.com/build/{releases,shrink-code,optimize-your-build,jdks,remote-repositories};
  developer.android.com/topic/performance/app-optimization/{full-mode,customize-which-resources-to-keep,r8-configuration-analyzer};
  developer.android.com/develop/ui/compose/bom; android-developers.googleblog.com/2025/11 (R8 keep rules);
  android-developers.googleblog.com/2023/12 (KSP2 Preview)
- KSP/Kotlin: github.com/google/ksp (README, ksp2.md); kotlinlang.org/docs/{ksp-faq,ksp-why-ksp,ksp-incremental,
  ksp-quickstart,compatibility-guide-23}.html; blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9
- Tracker (offiziell): github.com/gradle/gradle/issues/{19750,19235,26912,1989}; github.com/google/dagger (Hilt/KSP2)
- extern (gelabelt): jakewharton.com/you-should-use-androidx-betas; github.com/ben-manes/gradle-versions-plugin;
  github.com/littlerobots/version-catalog-update-plugin
