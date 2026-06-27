# R8 — Bug-Almanach (Android Code-Shrinker / Optimizer / Obfuscator) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): **Hochrisiko-Bereich (Stufe C)** — vor echter
> Arbeit hier ist der VOLLTEXT Pflicht (`Read` ohne `limit`); dieser Kurzcheck dient nur der
> Schnell-Orientierung. Bei JEDEM Fehler im Bereich gilt ebenfalls Volltext-Pflicht (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Crash NUR im Release, Debug ok | Output-Dateien lesen, nicht raten (`mapping/release/`) | §B |
| 2 | Fix-Versuch | Gezielte `keep`-Regel — NIE `minifyEnabled false` | TL;DR/#2 |
| 3 | Gson-Modell wird null/leer | `-keepattributes Signature` + Felder/TypeToken halten | §C1 |
| 4 | "Serializer for class X not found" | `-keepattributes InnerClasses` + Companion/serializer | §C3 |
| 5 | Retrofit-Service crasht (`create()`) | Retrofit aktuell halten (bringt `retrofit2.pro`) | §C2 |
| 6 | Enum `valueOf()`/`values()`-Crash | `values()`+`valueOf()` keepen | §D1 |
| 7 | `@HiltViewModel` NoSuchMethod (OFFEN) | keep-Regel für Ctor manuell ergänzen | §F1 |
| 8 | Per Name geladene Ressource fehlt | `res/raw/<pkg>.keep.xml` mit `tools`-Namespace | §G1, §G2 |
| 9 | `-assumenosideeffects` | NIE `*`-Wildcard, exakte Signaturen | §H2 |
| 10 | keep-Regel zu breit | Member-Level, kein `{ *; }`, keine `!`-Negation | §I |
| 11 | AGP 8.7.3 + ServiceLoader/META-INF/services | Miscompilation #389737060 → 8.9.1+ erwägen | §J, §O |
| 12 | AGP-9-Upgrade | `proguard-android-optimize.txt`, strictFullMode, optimizedResShrinking | §M, §G6 |
| 13 | Parcelable/JNI/`@JavascriptInterface` | CREATOR/native/JS-Bridge gezielt keepen | §E1–§E3 |
| 14 | Vor jedem Release | Pflicht-Checkliste durchgehen | §P |
