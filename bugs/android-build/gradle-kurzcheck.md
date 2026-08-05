# Android-Build-System (Gradle / AGP / R8·ProGuard / KSP) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
| 16 | Samsung-Geraet fehlt komplett in `adb devices`, USB-Debugging ausgegraut | Auto Blocker („Automatische Sperre") aus — kein Treiber-/Kabelproblem | §13.2 |
