# Android-Build-System (Gradle / AGP / R8 / KSP) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
