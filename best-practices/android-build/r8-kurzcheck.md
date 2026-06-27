# R8 Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | R8 bricht etwas | NIE abschalten — Root-Cause (keep-Regel) fixen | §A |
| 2 | Testen | Debug beweist nichts — minified „release-like" Build | §A |
| 3 | R8 einführen | Inkrementell: shrinking → obfuscation → optimization | §A |
| 4 | Vor Rollout | Interner/geschlossener Track + Pre-Launch + staged rollout | §B |
| 5 | Serialisierung/Reflection | consumer-rules + Codegen statt Reflection, eigene Modelle halten | §C |
| 6 | keep-Regel schreiben | so eng wie möglich, `-keepclassmembers`+`-if` statt `{ *; }` | §D |
| 7 | ProGuard-Basisdatei | `proguard-android-optimize.txt`, nie `proguard-android.txt` | §E |
| 8 | `-assumenosideeffects` | nur exakte Signaturen, NIE `*`-Wildcard | §E |
| 9 | Resource-Shrinking | minify+shrink beide true, dyn. Res in `<pkg>.keep.xml` | §F |
| 10 | Crash-Reporting | mapping.txt im AAB, Crashlytics v3+, `SourceFile,LineNumberTable` | §G |
| 11 | mapping.txt | pro Version extern archivieren (wird überschrieben) | §G |
| 12 | Baseline Profile | gegen unobfuskierten Build, <1,5 MB, in CI | §H |
| 13 | AGP 8.7.3 + ServiceLoader | #389737060 → auf ≥8.9.1 heben | §I |
| 14 | CI / Vor Release | minified Release bauen+testen, Pflicht-Checkliste | §I, §J |
