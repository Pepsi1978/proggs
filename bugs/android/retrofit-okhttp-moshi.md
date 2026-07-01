# Bekannte Bugs: Android-Networking — Retrofit + OkHttp + Moshi

> PFLICHT-LESEN vor Arbeit an Networking/API-Layer (Retrofit-Services, OkHttp-Client/Interceptors,
> Moshi-Modelle) in BestJournalAndroid (Moshi-Stack) und EntropieReductor (kotlinx-serialization-Stack).
> Stand: recherchiert am 2026-06-14 mit **7 Researchern parallel** (offizielle Quellen zuerst:
> square.github.io/retrofit + /okhttp, github.com/square/{retrofit,okhttp,moshi} Issues + CHANGELOG +
> die mitgelieferten proguard/r8-Regeln im Quellcode) + Fix-Status-Lauf, bei dem die **R8-Keep-Regeln
> aus `retrofit2.pro` (Tag 2.11.0) und `moshi.pro` (Tag 1.15.1) direkt verifiziert** wurden.
> **Versions-Anker (live aus BestJournalAndroid):** Retrofit **2.11.0** · OkHttp **4.12.0** · Moshi **1.15.1**
> (moshi-kotlin Reflection + moshi-kotlin-codegen) · converter-moshi/converter-scalars **2.11.0** ·
> logging-interceptor **4.12.0** · Kotlin **2.1.0** · KSP **2.1.0-1.0.29** · AGP 8.7.3 (**R8 full mode** = Default).
> Neuer (nicht im Projekt): OkHttp **5.x**, Moshi **2.0.0-alpha** (KSP2-only, kotlin-metadata statt kotlin-reflect).
>
> **Abgrenzung (was steht woanders):** generisches HTTP/LLM-API-Verhalten → [`../apis/api-integration-general.md`](../apis/api-integration-general.md).
> Allgemeine R8-/Shrinker-Regeln → [`../android-build/r8.md`](../android-build/r8.md). Hier geht es KONKRET um
> Retrofit/OkHttp/Moshi. Reine Kotlin-Coroutinen → [`kotlin.md`](kotlin.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **R** R8/Release (Fokus) · **M** Moshi-Adapter · **D** Moshi-Datenmodell · **S** Retrofit suspend/Fehler ·
> **I** Interceptors · **L** Leaks/Timeouts · **A** Annotationen · **SEC** Logging/Pinning.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Debug grün, Release crasht: `Unable to create call adapter` / `Call return type must be parameterized` | Retrofit ≥ 2.10 liefert R8-Regeln mit (2.11 ✓); bei ProGuard manuell kopieren | R1, R9 |
| 2 | Release: `Response must include generic type` | `-keepattributes Signature` + Retrofit-`.pro` (in 2.11 enthalten) | R2 |
| 3 | Release crasht nur bei `suspend`-API | `kotlin.coroutines.Continuation` keepen (in retrofit2.pro 2.11) | R3 |
| 4 | Release: gültiges JSON, aber alle Moshi-Felder null/leer | codegen (`@JsonClass`) nutzen, `moshi.generateProguardRules` an; Reflection-Modelle manuell keepen | R5, R6 |
| 5 | Build-Abbruch `Missing class org.conscrypt/bouncycastle/openjsse` | `-dontwarn` für die optionalen TLS-Provider | R8 |
| 6 | `missing_rules.txt` im Build-Output | Vorschläge übernehmen; Klasse selbst keepen (nicht nur Member) | R10 |
| 7 | `Unable to create converter ... requires explicit JsonAdapter` | `@JsonClass(generateAdapter=true)` ODER `KotlinJsonAdapterFactory` registrieren | M1 |
| 8 | Moshi-Adapter fehlt nach KSP-Migration | `kapt(...)`→`ksp(...)`, alte kapt-Zeile entfernen, clean build | M2 |
| 9 | Custom-`@ToJson`-Adapter greift nicht | `KotlinJsonAdapterFactory` mit `addLast()`, Custom mit `add()` | M4 |
| 10 | sealed/polymorpher Typ parst nicht | `PolymorphicJsonAdapterFactory` + `withDefaultValue` | M5 |
| 11 | `JsonDataException: Non-null value 'x' was null` | Property nullable + Default (Feld NICHT entfernen) | D1 |
| 12 | Server schickt `"x": null` → Default ignoriert | Backing-Property `_x?` + `?: default` im Body | D2 |
| 13 | Feld bleibt still null trotz Wert im JSON | `@Json(name="server_key")` setzen; in Debug `failOnUnknown()` | D4 |
| 14 | unbekannter Enum-Wert crasht ganze Antwort | `EnumJsonAdapter.withUnknownFallback(...)` + nullable Enum + Default | D6 |
| 15 | `suspend fun foo(): T` crasht bei HTTP 4xx/5xx | `HttpException` (non-2xx) UND `IOException` (Netz) fangen | S1 |
| 16 | 204/leerer Body → `KotlinNullPointerException` | Rückgabetyp `Response<Unit>` (oder `Unit` ab Retrofit 2.10) | S3 |
| 17 | „Netzwerkfehler" beim Screen-Verlassen / hängende UI | `catch (e: CancellationException) { throw e }` ZUERST; kein `runCatching` | S6 |
| 18 | `errorBody()` zweites Mal leer / `closed` | genau EINMAL `.string()` in Variable, dann wiederverwenden | S5 |
| 19 | eigener `Result<T>` als Retrofit-Rückgabe crasht | Custom `CallAdapter.Factory` / `NetworkResponse`; kotlin.Result nicht als Body | S4 |
| 20 | Auth-Header weg nach Redirect / doppelt | Token als **Application**-Interceptor; 401-Refresh per `Authenticator` | I1, I2 |
| 21 | `network interceptor must call proceed() exactly once` | Retry/Short-Circuit/Timeout in **Application**-Interceptor | I6 |
| 22 | Interceptor liest Body → nachfolgende sehen leer | `response.peekBody(n)` statt `body.string()` | I4 |
| 23 | Logger zeigt falsche Header / nichts bei Cache-Hit | Logger als LETZTER application- ODER als network-Interceptor (je nach Zweck) | I5, I9 |
| 24 | `A connection ... was leaked` | `response.use { }` / Body immer schließen | L1 |
| 25 | `byteStream()`/`source()` Leak / `IllegalStateException: closed` | Streaming selbst schließen; Body nur EINMAL lesen | L2 |
| 26 | `errorBody()`/`@Streaming` leakt | selbst schließen (`use{}`); Erfolgs-Body schließt Converter | L3 |
| 27 | Request hängt ewig trotz 10s-Timeouts | `callTimeout(30, SECONDS)` setzen (Default 0 = unendlich) | L4 |
| 28 | SSE/Streaming bricht nach 10s ab | `readTimeout(0)` auf abgeleitetem Client + Server-Heartbeat | L5 |
| 29 | viele idle Threads/Pools, kein Connection-Reuse | EIN `OkHttpClient`-Singleton; Varianten via `newBuilder()` | L6 |
| 30 | `@Field parameters can only be used with form encoding` | `@FormUrlEncoded` an die Methode | A1 |
| 31 | `does not contain {id}` / `Missing parameter for {id}` | `@Path`-Value exakt = `{platzhalter}`; `@Path` nie null | A4 |
| 32 | `baseUrl must end in /` / Endpoint-404 | baseUrl mit `/` enden, Endpoints OHNE führenden `/` | A9 |
| 33 | Tokens/PII in Logcat (Release) | `Level.NONE` in Release + `redactHeader("Authorization"/"Cookie")` | SEC1 |
| 34 | App offline nach Server-Cert-Wechsel | Backup-Pin setzen (oder Intermediate pinnen) | SEC3 |
| 35 | Chat/API-Antwort wirkt langsam trotz schnellem Server | `HttpLoggingInterceptor.Level.BODY` vom Hot Path nehmen; nur Timing/Status/Größen loggen | SEC4 |

---

## R) R8 / ProGuard Release-Crashes (FOKUS — „Debug grün, Release crasht")

> Grundwahrheit: **R8 full mode ist seit AGP 8.0 Default** und optimiert aggressiver. Bei `minifyEnabled true`
> liest **nur R8** die in den Lib-JARs eingebetteten `META-INF/proguard/*.pro` automatisch (ProGuard NICHT —
> siehe R9). Debug ist immer grün, weil dort nicht minifiziert wird.

### R1. Retrofit-Service-Interface zu `null` weg-optimiert ⭐ HAEUFIG
- **Symptom:** Release-Crash: `IllegalArgumentException: Unable to create call adapter for interface ...`, `Call return type must be parameterized as Call<Foo> or Call<? extends Foo>`. Im Mapping ist die Service-Klasse obfuskiert/verschwunden.
- **Ursache:** Retrofit erzeugt Service-Impls per `java.lang.reflect.Proxy`. R8 full mode sieht keinen Subtyp/keine Instanziierung des Interface und ersetzt alle Werte des Interface-Typs durch konstantes `null` (legitime Optimierung, vom R8-Team bestätigt).
- **Versionen:** betroffen sobald R8 full mode (AGP 8.0+) mit Retrofit < 2.10.0. **Belegt gefixt ab Retrofit 2.10.0** — die Regel ist in `retrofit2.pro` enthalten und **in 2.11.0 verifiziert vorhanden**. → Mit R8 ist BestJournalAndroid hier by-default geschützt.
- **FIX:** Wird automatisch mitgeliefert (nur R8). Inhalt der wirksamen Regel:
  ```proguard
  -if interface * { @retrofit2.http.* <methods>; }
  -keep,allowobfuscation interface <1>
  -if interface * { @retrofit2.http.* <methods>; }       # geerbte Services
  -keep,allowobfuscation interface * extends <1>
  ```
  Bei ProGuard (kein R8) manuell ergänzen (R9). `allowobfuscation` bleibt — umbenennen ist ok, entfernen/zu-null nicht.
- **Quelle:** https://github.com/square/retrofit/issues/3005 · verifiziert: https://raw.githubusercontent.com/square/retrofit/2.11.0/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro

### R2. Generische Rückgabesignaturen (`Response<T>`, `Call<T>`) gestrippt
- **Symptom:** Release-Crash `Response must include generic type (e.g., Response<String>)`, auch wenn das Interface gekeept ist.
- **Ursache:** R8 full mode strippt das `Signature`-Attribut nicht-gekeepter Klassen; Retrofit liest die generische Rückgabe per Reflection, um Converter/CallAdapter zu wählen.
- **Versionen:** betroffen AGP 8.0+ mit Retrofit < 2.10.0. **Gefixt ab 2.10.0, in 2.11.0 verifiziert.**
- **FIX:** in retrofit2.pro enthalten: `-keepattributes Signature, InnerClasses, EnclosingMethod` + Keep auf `retrofit2.Response` + Rückgabetyp-Keep. Kein eigener Code nötig (R8).
- **Quelle:** https://github.com/square/retrofit/issues/3751 · retrofit2.pro (verifiziert, s.o.)

### R3. Kotlin `suspend`: `Continuation`-Signatur gestrippt ⭐ HAEUFIG
- **Symptom:** Release-Crash NUR bei `suspend`-API-Methoden (`Unable to create call adapter`), während `Call<T>`-Methoden laufen.
- **Ursache:** `suspend fun x(): User` kompiliert zu einem versteckten `Continuation<? super User>`-Parameter; Retrofit liest dessen Typ-Argument. R8 strippt die Signatur von `kotlin.coroutines.Continuation`.
- **Versionen:** betroffen AGP 8.0+ mit Retrofit < 2.10.0. **Gefixt ab 2.10.0, in 2.11.0 verifiziert** (`-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation`).
- **FIX:** automatisch mitgeliefert (R8). Bei ProGuard manuell (R9).
- **Quelle:** retrofit2.pro (verifiziert, s.o.) · https://github.com/square/retrofit/issues/3751

### R4. Zusatz-CallAdapter (RxJava etc.) bringt eigene Regeln separat mit
- **Symptom:** Trotz korrekter Core-Regeln Release-Crash `Unable to create call adapter`, wenn ein RxJava2/3-/Guava-Adapter genutzt wird.
- **Ursache:** Jedes `retrofit-adapters/*`-Artefakt hat eine EIGENE `.pro`; die Core-`retrofit2.pro` deckt sie nicht ab.
- **Versionen:** mit aktuellen Adapter-Artefakten (≥ 2.10.0) automatisch via Consumer-Rules; bei älteren/ProGuard manuell.
- **FIX:** Adapter-Artefakt aktuell halten oder dessen Keep-Regeln manuell ergänzen. (BestJournal nutzt converter-moshi/-scalars, keinen CallAdapter → meist irrelevant.)
- **Quelle:** https://github.com/square/retrofit/issues/3880

### R5. Moshi-codegen-Adapter weg-minifiziert / Modellfelder umbenannt ⭐ HAEUFIG
- **Symptom:** Gültiges JSON, aber im Release alle Felder `null`/leere Objekte; oder `NoSuchMethodError`/`ClassNotFoundException` für `...JsonAdapter`.
- **Ursache:** codegen erzeugt `<Model>JsonAdapter`-Klassen, die zur Laufzeit reflektiv (Namenskonvention) gefunden werden. R8 entfernt die Adapter-Klasse oder benennt Property-Felder/JSON-Keys um.
- **Versionen:** codegen erzeugt **seit Moshi 1.10.0 präzise Keep-Regeln on-the-fly pro `@JsonClass`-Modell**; in **1.15.1 vorhanden** (Performance-Verbesserung erst 1.15.2). → Mit codegen + R8 geschützt.
- **FIX:** **codegen statt Reflection** verwenden (`@JsonClass(generateAdapter = true)`), KSP, `moshi.generateProguardRules` auf Default (`true`) lassen. Nichts weglassen.
- **Quelle:** https://github.com/square/moshi/issues/1834 · https://github.com/square/moshi/blob/master/CHANGELOG.md

### R6. Moshi-**Reflection**-Pfad: Modellklassen/Kotlin-Metadata weg-minifiziert ⭐ HAEUFIG
- **Symptom:** Mit `KotlinJsonAdapterFactory` (Reflection) im Release: `null` in Non-Null-Feldern, „No JsonAdapter", oder Moshi behandelt Kotlin-Klasse wie Java-Klasse.
- **Ursache:** Der Reflection-Adapter liest die `@Metadata`-Annotation; R8 darf sie/Modellklassen entfernen oder Felder obfuskieren. Die mitgelieferte `moshi.pro` schützt NUR Adapter-Methoden, `@JsonQualifier`, `@JsonClass`-Enums und den Kotlin-Metadata-Helfer — **NICHT die Modellklassen selbst**.
- **Versionen:** Reflection-Pfad braucht in ALLEN Versionen inkl. 1.15.1 manuelle Keep-Regeln (verifiziert an moshi.pro 1.15.1).
- **FIX:** Modellklassen explizit keepen (`@Keep` bzw. paketweite Regel) UND `kotlin-reflect` als Dependency sicherstellen — ODER (empfohlen) auf **codegen** umstellen (R5), das braucht praktisch keine eigenen Keeps.
  ```proguard
  -keep class com.example.dto.** { *; }   # nur falls Reflection genutzt wird
  ```
- **Quelle:** https://github.com/square/moshi (README R8/ProGuard) · verifiziert: https://raw.githubusercontent.com/square/moshi/1.15.1/moshi/src/main/resources/META-INF/proguard/moshi.pro

### R7. Moshi-Enums von R8 entfernt/obfuskiert
- **Symptom:** Enum-Felder parsen falsch / `JsonDataException` — nur im Release.
- **Ursache:** `EnumJsonAdapter` nutzt Enum-Feldnamen + synthetisches `values()`; R8 entfernt ungenutzte Konstanten / benennt um.
- **Versionen:** Schutzregel in moshi.pro 1.15.1 vorhanden — greift aber NUR für `@JsonClass`-annotierte Enums.
- **FIX:** Enum mit `@JsonClass(generateAdapter = false)` annotieren — dann greift die mitgelieferte Keep-Regel (`<fields>; **[] values();`).
- **Quelle:** moshi.pro 1.15.1 (verifiziert, s.o.)

### R8. OkHttp: `Missing class` (Conscrypt/BouncyCastle/OpenJSSE) bricht Release-Build ab
- **Symptom:** Build (nicht Laufzeit) bricht ab: `R8: Missing class org.conscrypt.Conscrypt` / `org.bouncycastle.jsse.*` / `org.openjsse.*`. Bei OkHttp 5.0 auch `Missing class okhttp3.internal.Util`.
- **Ursache:** OkHttp referenziert optionale TLS-Provider, die nur bei Vorhandensein per try/catch genutzt werden; R8 full mode meldet sie ohne `-dontwarn` als Fehler. `internal.Util` bei 5.0 = gemischte 4.x/5.x-Module.
- **Versionen:** durchgängig (4.12.0 + 5.x); die mitgelieferte `okhttp3.pro` deckt nicht jeden Provider ab (OpenJSSE-Lücke = Issue #9204).
- **FIX:**
  ```proguard
  -dontwarn okhttp3.internal.platform.**
  -dontwarn org.conscrypt.**
  -dontwarn org.bouncycastle.**
  -dontwarn org.openjsse.**
  ```
  Bei `Missing okhttp3.internal.Util` (5.0): NICHT dontwarn, sondern OkHttp-Versionen vereinheitlichen (BOM/Constraint; `./gradlew app:dependencies`).
- **Quelle:** https://github.com/square/okhttp/issues/9204 · https://square.github.io/okhttp/features/r8_proguard/

### R9. Reiner ProGuard (kein R8) zieht eingebettete Lib-Regeln NICHT
- **Symptom:** Mit `android.enableR8=false`/klassischem ProGuard crasht der Release trotz aktueller Versionen (`Unable to create converter`/null-Felder), obwohl es mit R8 liefe.
- **Ursache:** Nur **R8** liest die in Lib-JARs eingebetteten `META-INF/proguard/*.pro` automatisch.
- **Versionen:** strukturell, versionsunabhängig.
- **FIX:** auf R8 bleiben (Default). Bei echtem ProGuard `retrofit2.pro`, `moshi.pro`, `okhttp3.pro` + Adapter-`.pro` manuell in `proguard-rules.pro` kopieren.
- **Quelle:** https://square.github.io/retrofit/download/

### R10. `missing_rules.txt` ignoriert / nur Member statt Klasse gekeept
- **Symptom:** Release-Crash trotz scheinbarer Keep-Regeln; `app/build/outputs/mapping/release/missing_rules.txt` enthält unbeachtete Vorschläge.
- **Ursache:** R8 schreibt fehlende Regeln in `missing_rules.txt` statt hart abzubrechen; oder `-keepclassmembers` hält die Member, aber nicht die Klasse selbst (full-mode-Falle).
- **Versionen:** strukturell, AGP 8.x.
- **FIX:** `missing_rules.txt` nach jedem Release-Build prüfen + übernehmen; nicht nur Member, sondern die Klasse keepen (`-keep`/`-keepclasseswithmembers`). `mapping.txt` zum Deobfuskieren aufheben.
- **Quelle:** https://developer.android.com/build/shrink-code

---

## M) Moshi — Adapter-Erzeugung (Reflection / Codegen / KSP)

### M1. `@JsonClass(generateAdapter=true)` vergessen → Laufzeit-Crash ⭐ HAEUFIG
- **Symptom:** `IllegalArgumentException: Unable to create converter for class ...` (über Retrofit) bzw. `Cannot serialize Kotlin type ... requires explicit JsonAdapter`. Build grün, Crash zur Laufzeit.
- **Ursache:** Seit Moshi 1.9.0 brauchen Kotlin-Klassen entweder codegen (`@JsonClass`) ODER `KotlinJsonAdapterFactory` (Reflection). Reine `Moshi.Builder().build()` erkennt Kotlin-Klassen nicht.
- **Versionen:** by-design seit 1.9.0, inkl. 1.15.1.
- **FIX:** entweder pro Modell `@JsonClass(generateAdapter = true)` (codegen, empfohlen) ODER global `Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()` (+ `moshi-kotlin`-Dependency).
- **Quelle:** https://github.com/square/moshi/blob/master/CHANGELOG.md (1.9.0)

### M2. KSP-Migration unvollständig → generierter Adapter fehlt
- **Symptom:** Build-Warnung `Kapt support in Moshi ... is deprecated`; oder nach Migration fehlt `<Model>JsonAdapter` → Laufzeit-Crash wie M1.
- **Ursache:** KAPT-codegen in 1.15.0 deprecatet (in 2.x entfernt). Häufiger Fehler: KSP ergänzt, aber alte `kapt("...moshi-kotlin-codegen")`-Zeile bleibt → KSP generiert nichts. KSP-Version strikt an Kotlin gekoppelt.
- **Versionen:** KSP-Support ab Moshi 1.13.0; KAPT deprecatet 1.15.0. Anker (1.15.1 + KSP 2.1.0-1.0.29 + Kotlin 2.1.0) korrekt.
- **FIX:**
  ```kotlin
  implementation("com.squareup.moshi:moshi:1.15.1")
  ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")   // KEIN kapt(...) mehr
  ```
  danach Clean Build.
- **Quelle:** https://github.com/square/moshi/blob/master/CHANGELOG.md (1.15.0, 1.13.0)

### M3. Reflection-Pfad ohne `KotlinJsonAdapterFactory`/`moshi-kotlin`
- **Symptom:** `Cannot serialize Kotlin type ...`; Properties falsch befüllt.
- **Ursache:** Ohne Annotation MUSS `KotlinJsonAdapterFactory()` registriert UND `moshi-kotlin` vorhanden sein; Plain-Java-Reflection auf Kotlin-Klassen ist „undefined behavior". `moshi-kotlin` zieht in 1.x das große `kotlin-reflect` (~2,5 MiB).
- **Versionen:** 1.9.0–1.15.1. Ab 2.0.0-alpha nutzt die Factory `kotlin-metadata` statt `kotlin-reflect`.
- **FIX:** `implementation("com.squareup.moshi:moshi-kotlin:1.15.1")` + `addLast(KotlinJsonAdapterFactory())`. App-Größe sparen → codegen.
- **Quelle:** https://github.com/square/moshi/blob/master/README.md

### M4. Falsche Adapter-Reihenfolge → Custom-Adapter greift nicht
- **Symptom:** Eigener `@ToJson`/`@FromJson`-Adapter wird ignoriert; Standard-Repräsentation erscheint.
- **Ursache:** Moshi nimmt den ERSTEN passenden Adapter. `KotlinJsonAdapterFactory` mit `add()` (statt `addLast()`) VOR dem Custom-Adapter registriert → die generische Factory gewinnt.
- **Versionen:** `addLast()` seit 1.11.0; Falle in allen Versionen inkl. 1.15.1.
- **FIX:** Custom mit `add()` (vorne), generische Factories mit `addLast()` (hinten).
- **Quelle:** https://github.com/square/moshi/blob/master/README.md (Precedence)

### M5. Sealed/polymorphe Typen ohne `PolymorphicJsonAdapterFactory`
- **Symptom:** sealed-Hierarchie parst nicht / `JsonDataException` für unbekannten Subtyp; mit `failOnUnknown()` Crash; Fallback greift nicht, wenn der Label-Key ganz fehlt.
- **Ursache:** `@JsonClass` allein löst keine Polymorphie. Label-Feld nicht als erstes Feld → Re-Parse-Problem mit `failOnUnknown()`. Fallback bei FEHLENDEM Label-Key erst in 2.0.0-alpha gefixt.
- **Versionen:** `PolymorphicJsonAdapterFactory` ab 1.8.0, `withDefaultValue` ab 1.9.0, `withFallbackJsonAdapter` ab 1.10.0. „Label-Key fehlt ganz" in **1.15.1 noch NICHT gefixt** (erst 2.0.0-alpha.1).
- **FIX:** `PolymorphicJsonAdapterFactory.of(Base::class.java, "type").withSubtype(...).withDefaultValue(Unknown)` (braucht `moshi-adapters`); Typ-Feld als erstes Feld liefern.
- **Quelle:** https://github.com/square/moshi/issues/1512 · CHANGELOG (1.8.0–2.0.0-alpha.1)

### M6. Generics: `Types.newParameterizedType` vergessen → `LinkedHashTreeMap` statt Modell
- **Symptom:** `moshi.adapter(List::class.java)` liefert `List<LinkedHashTreeMap>` statt `List<User>`; `ClassCastException`.
- **Ursache:** Type-Erasure: `List<User>::class.java` verliert das Typargument.
- **Versionen:** by-design, alle inkl. 1.15.1; geschachtelt braucht `newParameterizedTypeWithOwner` (ab 1.3.0).
- **FIX:** `moshi.adapter<List<User>>()` (reified Extension, ab 1.11.0) ODER `Types.newParameterizedType(List::class.java, User::class.java)`.
- **Quelle:** https://github.com/square/moshi/issues/1315

---

## D) Moshi — Datenmodell & Parsing (null / Defaults / @Json / Enums)

> Fast alles hier ist **by-design** (Moshi-Verhalten, kein „Fix"). Alle FIXes sind funktionserhaltend —
> Feld bleibt erhalten, nur Nullbarkeit/Default/Adapter werden angepasst.

### D1. Non-null Kotlin-Property bekommt `null`/fehlt → `JsonDataException` ⭐ HAEUFIG
- **Symptom:** `JsonDataException: Non-null value 'a' was null at $.a` (explizites null) bzw. `Required property 'a' missing at $.a` (Feld fehlt, kein Default).
- **Ursache:** codegen liest non-nullable Properties mit `nonNull()`-Adapter; der Server hält die Non-Null-Zusage nicht ein.
- **Versionen:** by-design seit codegen (1.6.0), inkl. 1.15.1.
- **FIX (NICHT Feld entfernen):** Property nullable machen + ggf. Default: `val nickname: String? = null`, `val tags: List<String> = emptyList()`. Eigene Adapter mit `.nullSafe()` umhüllen.
- **Quelle:** https://github.com/square/moshi/blob/master/README.md · https://github.com/square/moshi/issues/1011

### D2. Default-Wert wird bei explizitem `null` ignoriert ⭐ HAEUFIG
- **Symptom:** `val name: String? = "-"`. Server lässt Feld WEG → Default „-" greift. Server schickt `"name": null` → am Ende steht `null` (Default überschrieben).
- **Ursache:** „working as designed": Moshi unterscheidet **absent** (Default greift) vs **null** (überschreibt Default). Maintainer: „Defaults and nullability are independent concerns."
- **Versionen:** by-design seit 1.6.0, inkl. 1.15.1.
- **FIX:** Backing-Property + Normalisierung im Body:
  ```kotlin
  @JsonClass(generateAdapter = true)
  data class Reference(@Json(name = "name") private val _name: String? = "-") {
      val name: String get() = _name ?: "-"
  }
  ```
- **Quelle:** https://github.com/square/moshi/issues/762 · https://www.zacsweers.dev/exploring-moshis-kotlin-code-gen/

### D3. Reflection vs codegen: Default-Verhalten unterscheidet sich
- **Symptom:** Plain-Java-Reflection setzt fehlende Felder auf `0`/`false`/`null` statt deklariertem Default; deklariertes `= -1` ignoriert.
- **Ursache:** Java-Reflection kann Feld-Defaults nur über No-Args-Konstruktor zuweisen. Kotlin-codegen/`KotlinJsonAdapterFactory` ehren Kotlin-Defaults (für abwesende Felder).
- **Versionen:** by-design, inkl. 1.15.1.
- **FIX:** für Kotlin codegen oder `KotlinJsonAdapterFactory` (`addLast()`) nutzen — beide ehren Kotlin-Defaults.
- **Quelle:** https://github.com/square/moshi/blob/master/README.md

### D4. `@Json(name=)`-Mismatch → Feld bleibt still null/Default ⭐ HAEUFIG
- **Symptom:** Feld kommt nie an, bleibt null/Default — KEINE Exception.
- **Ursache:** Moshi hat KEINE Field-Naming-Strategy (anders als Gson). JSON-Key muss exakt = Property-Name oder per `@Json(name=)` deklariert sein; unbekannte Keys werden still ignoriert.
- **Versionen:** by-design.
- **FIX:** `@Json(name = "server_key") val luckyNumber: Int`. Zum Aufspüren in Debug `adapter.failOnUnknown()`.
- **Quelle:** https://github.com/square/moshi/blob/master/README.md

### D5. Unbekannte JSON-Keys still verschluckt
- **Symptom:** Server schickt neue/falsche Felder; App parst klaglos weiter → Mapping-Fehler bleiben unbemerkt.
- **Ursache:** Default `reader.skipValue()` für unbekannte Namen (robust gegen Server-Erweiterungen, versteckt Tippfehler).
- **Versionen:** by-design; `failOnUnknown()` nennt teils falschen Feldnamen (#939, in 1.15.1 offen).
- **FIX:** in Debug/Test `adapter.failOnUnknown()`; in Release tolerant lassen.
- **Quelle:** https://github.com/square/moshi/issues/939

### D6. Unbekannter Enum-Wert crasht die ganze Antwort ⭐ HAEUFIG
- **Symptom:** `JsonDataException: Expected one of [...] but was ANCHOR at path $.suit` — ein neuer Server-Enum-Wert legt das gesamte Parsen lahm.
- **Ursache:** Standard-Enum-Adapter kennt nur deklarierte Konstanten, kein Fallback.
- **Versionen:** `EnumJsonAdapter.withUnknownFallback` ab 1.7.0; Null-Fall (#721, withUnknownFallback greift NICHT bei explizitem `null`/absent) in 1.15.1 offen.
- **FIX:** `EnumJsonAdapter.create(Suit::class.java).withUnknownFallback(Suit.UNKNOWN)` (braucht `moshi-adapters`) + Server-Schreibweisen per `@Json(name=)`; gegen den null-Fall zusätzlich `val suit: Suit? = Suit.UNKNOWN`.
- **Quelle:** https://square.github.io/moshi/1.x/moshi-adapters/...EnumJsonAdapter.html · https://github.com/square/moshi/issues/721

### D7. Listen-Nullability: `List<Foo>` lehnt null-Element/null-Liste ab
- **Symptom:** `JsonDataException: Non-null value ... was null` bei `"a": null` oder `["x", null, "y"]`.
- **Ursache:** codegen erzeugt `.nonNull()`-List-Adapter; Liste UND Elemente non-null.
- **Versionen:** korrekt seit 1.6.0 (frühes #526 war echter Bug, gefixt); in 1.15.1 korrekt.
- **FIX:** präzise modellieren: `List<String>?` (nullable Liste) vs `List<String?>` (nullable Elemente) vs `List<String?>?`.
- **Quelle:** https://github.com/square/moshi/issues/526

### D8. `@Transient` braucht Default & verträgt sich nicht mit `private`/`@Json`
- **Symptom:** `@Transient`-Property ohne Default → Compile-Fehler; `@Transient private var` → Kapt-Sichtbarkeitsfehler; mehrere `@Transient` → `duplicate option: " "` (#775).
- **Ursache:** ausgeschlossene Felder bekommen nie einen JSON-Wert → Default Pflicht; private transiente Felder sind für den generierten Adapter unsichtbar.
- **Versionen:** Default-Zwang by-design; #643/#775 in 1.15.1 relevant.
- **FIX:** Default geben, nicht `private`, kein `@Json` daneben. Modern: `@Json(ignore = true) var total: Int = 0`.
- **Quelle:** https://github.com/square/moshi/issues/643 · https://github.com/square/moshi/issues/775

### D9. `adapter.fromJson("null")` liefert `null` (überraschend, spätere NPE)
- **Symptom:** Top-Level-`"null"` → `fromJson` gibt `null`; späterer Non-null-Zugriff wirft NPE woanders.
- **Ursache:** `fromJson` ist null-tolerant für den Top-Level-Wert.
- **Versionen:** by-design.
- **FIX:** Rückgabe prüfen statt `!!`: `adapter.fromJson(body) ?: handleEmpty()`.
- **Quelle:** https://github.com/square/moshi/blob/master/README.md

### D10. Dynamische Default-Werte falsch berechnet (codegen-Edge-Case)
- **Symptom:** `data class Foo(val bar: String = "burrito", val baz: String = bar)`; JSON `{"bar":"taco"}` → `baz = "burrito"` statt „taco".
- **Ursache:** codegen instanziiert mit statischen Defaults + `copy()`; dynamischer Default `= bar` wird nicht nachgezogen.
- **Versionen:** by-design-Limit seit 1.6.0, inkl. 1.15.1.
- **FIX:** dynamische Defaults vermeiden: `private val _baz: String? = null` + `val baz get() = _baz ?: bar`.
- **Quelle:** https://www.zacsweers.dev/exploring-moshis-kotlin-code-gen/

---

## S) Retrofit suspend-Funktionen & Fehler-/Result-Behandlung

### S1. `suspend fun foo(): T` wirft bei HTTP-Fehler → unbehandelt = Crash ⭐ HAEUFIG
- **Symptom:** Bei non-2xx `retrofit2.HttpException: HTTP 400`, bei Netzproblemen `java.io.IOException` — fliegt aus der suspend-Funktion als Exception, nicht als Wert. Unbehandelt → App-Crash.
- **Ursache:** Bei direktem Body-Typ delegiert Retrofit an `KotlinExtensions.await`: non-2xx → `HttpException`, Transportfehler → `IOException`.
- **Versionen:** by-design seit 2.6.0, inkl. 2.11.0.
- **FIX:** beide Typen fangen (Reihenfolge: erst `HttpException`/`IOException`, Cancellation siehe S6):
  ```kotlin
  try { Result.success(api.user(id)) }
  catch (e: HttpException) { Result.failure(...) }   // e.code(), e.response()?.errorBody()
  catch (e: IOException) { Result.failure(...) }
  ```
- **Quelle:** https://github.com/square/retrofit/issues/3498

### S2. `Response<T>` vs `T` — unterschiedliches Fehlerverhalten
- **Symptom:** Mit `: T` wirft jeder non-2xx eine Exception. Mit `: Response<T>` KEINE Exception — `isSuccessful` prüfen, sonst `body()!!` → NPE.
- **Ursache:** `Response<…>` → `awaitResponse()` (liefert immer Response); sonst `await()` (Exception bei non-2xx).
- **Versionen:** beide seit 2.6.0.
- **FIX:** `Response<T>` wählen, wenn Status/Header/errorBody gebraucht werden; `isSuccessful` prüfen.
- **Quelle:** https://zsmb.co/retrofit-meets-coroutines/

### S3. 204/leerer Body mit non-null Typ → `KotlinNullPointerException` ⭐ HAEUFIG
- **Symptom:** Endpoint antwortet 204 No Content (logout/revoke): `KotlinNullPointerException: Response from ... was null but response body type was declared as non-null` (bestätigter Produktionsvorfall, #3595).
- **Ursache:** `OkHttpCall#parseResponse` liefert bei 204/205 hartkodiert null-Body; `await` erwartet non-null → NPE.
- **Versionen:** seit 2.6.0. `Response<Unit>` funktioniert ab 2.6.0; `Unit` als direkter Typ ab 2.10.0.
- **FIX:** Rückgabetyp `Response<Unit>` (Status prüfbar, kein NPE) oder `Unit` (ab Retrofit 2.10).
- **Quelle:** https://github.com/square/retrofit/issues/3595

### S4. Eigener `Result`/Either-Wrapper als Rückgabetyp → CallAdapter wird übersprungen
- **Symptom:** `suspend fun getUser(): Result<User>` crasht mit Converter-Fehler — Retrofit versucht, den Wrapper zu deserialisieren.
- **Ursache:** Bei suspend-Body-Typ ruft Retrofit `createCallAdapter()` nicht auf; der Wrapper landet beim Converter. `kotlin.Result` ist kein unterstützter suspend-Body-Typ.
- **Versionen:** Einschränkung seit 2.6.0, inkl. 2.11.0.
- **FIX:** eigenen `NetworkResponse<T>`-Typ + `CallAdapter.Factory` (erzeugt `Call<NetworkResponse<T>>`); oder Library `NetworkResponseAdapter`. NICHT `kotlin.Result` als Body.
- **Quelle:** https://github.com/square/retrofit/issues/3461 · https://github.com/square/retrofit/pull/2886

### S5. `errorBody()` ist one-shot — zweites Lesen leer/`closed`
- **Symptom:** Error-JSON einmal geloggt, beim zweiten Zugriff leer oder `IllegalStateException: closed`.
- **Ursache:** `errorBody()` ist ein OkHttp-`ResponseBody`, dessen Stream nur EINMAL konsumierbar ist (`.string()` liest + schließt).
- **Versionen:** OkHttp-Stream-Semantik, versionsübergreifend.
- **FIX:** genau einmal `.string()` in eine Variable, dann wiederverwenden: `val raw = resp.errorBody()?.string()`.
- **Quelle:** OkHttp `ResponseBody`-Vertrag

### S6. `CancellationException` als Fehler behandelt → kaputte strukturierte Nebenläufigkeit ⭐ HAEUFIG
- **Symptom:** Beim Screen-Verlassen (gecancelter Scope) erscheint ein „Netzwerkfehler", oder die UI hängt/leakt.
- **Ursache:** Gecancelte Coroutine wirft `CancellationException` (normales Abbruch-Signal, KEIN Fehler). Breites `catch (e: Exception)` / `runCatching {}` schluckt es → Abbruch geht verloren.
- **Versionen:** Coroutines-Semantik, alle Versionen.
- **FIX:** `CancellationException` ZUERST fangen und rethrowen:
  ```kotlin
  catch (e: CancellationException) { throw e }   // vor allen anderen catch-Zweigen
  ```
  Eigenes `runSuspendCatching` statt `kotlin.runCatching`.
- **Quelle:** https://github.com/Kotlin/kotlinx.coroutines/issues/1814 · https://kotlinlang.org/docs/cancellation-and-timeouts.html

### S7. `UndeclaredThrowableException` bei wiederholten suspend-Calls (alt, gefixt)
- **Symptom:** Zweiter/dritter gleichartiger suspend-Call crasht mit `java.lang.reflect.UndeclaredThrowableException` statt `IOException` → `catch (IOException)` greift nicht.
- **Ursache:** Zusammenspiel kotlinx.coroutines #1474 + Retrofits Proxy-Boundary; checked Exception nicht deklariert → in `UndeclaredThrowableException` verpackt.
- **Versionen:** betroffen 2.6.0; Teilfixes 2.6.1/2.6.2/2.6.3; **final robust ab 2.10.0** („capture and defer all Throwable subtypes"). **In 2.11.0 erledigt** — kein `@Throws`-Workaround mehr nötig.
- **FIX:** Retrofit ≥ 2.10 (Anker 2.11.0 erfüllt das).
- **Quelle:** https://github.com/square/retrofit/issues/3128 · CHANGELOG (2.6.1–2.10.0)

### S8. Mythos „suspend braucht `withContext(IO)`" + `suspend fun: Call<T>`-Falle
- **Symptom:** (a) unnötiges `withContext(Dispatchers.IO)` um jeden Call. (b) `suspend fun foo(): Call<User>` wird ab Retrofit 2.10 sofort als Fehler abgelehnt.
- **Ursache:** Retrofit-suspend führt I/O IMMER auf OkHttps Pool aus (hauptthread-sicher); `withContext(IO)` ist überflüssig. `suspend` + `Call<T>` mischt zwei Modelle.
- **Versionen:** Threading seit 2.6.0; Eager-Reject von `suspend ...: Call<Body>` ab 2.10.0.
- **FIX:** direkt `viewModelScope.launch { api.user(id) }`; Body-Typ direkt deklarieren (nicht `Call<T>`). Für `Call`-APIs `Call.await()`/`awaitResponse()`.
- **Quelle:** https://zsmb.co/retrofit-meets-coroutines/ · CHANGELOG 2.10.0

---

## I) OkHttp Interceptors — Reihenfolge & Fallen

> Meist **by-design** (Schicht-Modell). Application-Interceptor (`addInterceptor`) = einmal, sieht Logik-Request,
> läuft auch bei Cache-Hits. Network-Interceptor (`addNetworkInterceptor`) = pro Wire-Vorgang (Redirect/Retry mehrfach), sieht gzip/Redirects, NICHT bei Cache-Hits.

### I1. Auth-/Token-Header verschwindet nach Redirect / doppelt (falsche Schicht)
- **Symptom:** Per `addNetworkInterceptor` gesetzter `Authorization` fehlt nach Redirect oder wird bei Retries mehrfach gesetzt.
- **Ursache:** Network-Interceptor läuft pro Wire-Vorgang; Auth gehört in den Application-Layer (oder `Authenticator`).
- **Versionen:** by-design, alle.
- **FIX:** Token als **Application-Interceptor** anhängen (einmal, konsistent über Retries).
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### I2. 401-Refresh im Interceptor → doppelte Folge-Interceptor-Aufrufe ⭐ HAEUFIG
- **Symptom:** Token-Refresh per `if (code==401) chain.proceed()` zum zweiten Mal → Logging/Analytics laufen doppelt; Races bei parallelem Refresh.
- **Ursache:** Zwei `chain.proceed()`-Aufrufe durchlaufen die restliche Kette doppelt.
- **Versionen:** by-design.
- **FIX:** `Authenticator` für 401-Refresh (nur bei 401, Auto-Retry, kein Doppel-Durchlauf), Interceptor nur fürs Anhängen. `synchronized` gegen Parallel-Refresh; `return null` wenn kein Refresh möglich (sonst Endlosschleife); Versuchszähler via `responseCount`.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/ (Authenticator-API)

### I3. `Level.BODY` konsumiert one-shot Request-Body → leerer Versand
- **Symptom:** Mit `HttpLoggingInterceptor.Level.BODY` wird ein Streaming-Request-Body LEER gesendet; ohne Logging korrekt. Keine Exception.
- **Ursache:** Der Logger liest den Body; eine one-shot `RequestBody.writeTo()` ist danach erschöpft.
- **Versionen:** by-design; `isOneShot()` seit 3.13.
- **FIX:** `writeTo()` mehrfach-lesbar bauen (Quelle in `writeTo()` öffnen) ODER `isOneShot()` → `true`. Body-Inspektion sonst via Proxy (Charles/Chuck).
- **Quelle:** https://github.com/square/okhttp/issues/3269

### I4. Interceptor liest `response.body` → nachfolgende sehen leeren Body ⭐ HAEUFIG
- **Symptom:** Eigener Interceptor `response.body!!.string()`; danach leerer Body / `IllegalStateException: closed` im Converter.
- **Ursache:** Response-Body ist one-shot; `.string()` konsumiert + schließt.
- **Versionen:** by-design; `peekBody` seit 3.x.
- **FIX:** zum Inspizieren `response.peekBody(1024*1024).string()` (konsumiert nicht). Falls Body verarbeitet UND weitergegeben: neuen Body bauen (`raw.toResponseBody(contentType)`).
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### I5. `HttpLoggingInterceptor` an falscher Position → falsche Header / nichts bei Cache-Hit
- **Symptom:** Logger zeigt nicht den Wire-Inhalt (kein `Accept-Encoding`, keine Redirects) oder bei Cache-Hits gar nichts; oder Header späterer Interceptoren fehlen.
- **Ursache:** Schicht/Reihenfolge bestimmen die Sicht: letzter Application-Interceptor (Logik, läuft bei Cache-Hit) vs Network-Interceptor (Wire, nicht bei Cache-Hit).
- **Versionen:** by-design.
- **FIX:** App-Logik prüfen → als LETZTEN `addInterceptor`; Wire/gzip/Redirects → `addNetworkInterceptor`.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### I6. `network interceptor must call proceed() exactly once` ⭐ HAEUFIG
- **Symptom:** `IllegalStateException: network interceptor ... must call proceed() exactly once`.
- **Ursache:** Network-Interceptor ruft `proceed()` 0× (Short-Circuit/Mock) oder mehrfach (eigener Retry); auch `chain.withReadTimeout()/withConnectTimeout()` in einem Network-Interceptor (#6100, by-design).
- **Versionen:** by-design.
- **FIX:** Short-Circuit/Retry/Pro-Call-Timeout in einen **Application-Interceptor** verlagern (dort sind 0/mehrere `proceed()` erlaubt). Verworfene Responses beim Retry schließen (siehe I7).
- **Quelle:** https://github.com/square/okhttp/issues/6100

### I7. Retry/Backoff im Interceptor leakt Connections
- **Symptom:** „A connection ... was leaked", Pool-Erschöpfung bei eigenem Retry.
- **Ursache:** Jeder `chain.proceed()` liefert eine Response mit offenem Body; verworfene Response beim Retry nicht geschlossen.
- **Versionen:** by-design.
- **FIX:** vor jedem erneuten `proceed()` die verworfene Response `response.close()`.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### I8. Manuelles `Accept-Encoding: gzip` schaltet Auto-Dekompression ab
- **Symptom:** `response.body.string()` liefert komprimierten Müll; `Content-Encoding` verändert.
- **Ursache:** OkHttps `BridgeInterceptor` setzt `Accept-Encoding: gzip` selbst UND entpackt transparent. Setzt man den Header selbst, übernimmt man die Dekompression.
- **Versionen:** by-design.
- **FIX:** Header NICHT manuell setzen — OkHttp die Transparenz überlassen.
- **Quelle:** https://github.com/square/okhttp/issues/2132

### I9. Cache-Interceptor zwischen Application und Network → Cache-Hits umgehen Network-Interceptoren
- **Symptom:** Network-Interceptor (Logging/Metriken) läuft NICHT bei aus dem Cache bedienten Responses; Conditional-Header (`If-None-Match`) im Application-Interceptor unsichtbar.
- **Ursache:** Feste Kettenreihenfolge: Application → Retry/Bridge → **Cache** → Connect → Network → CallServer. Cache short-circuited bei Hit.
- **Versionen:** by-design, alle.
- **FIX:** „muss immer laufen" (Logging/Auth/Metriken) → Application-Interceptor; „nur echter Netzverkehr" → Network-Interceptor.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

---

## L) OkHttp Connection-/Response-Leaks & Timeouts

### L1. `ResponseBody` nicht geschlossen → Connection-Leak + Pool-Erschöpfung ⭐ HAEUFIG
- **Symptom:** `A connection to ... was leaked. Did you forget to close a response body?`; mit der Zeit hängen Requests, App wird langsam/crasht.
- **Ursache:** Jeder `ResponseBody` bindet eine Socket-Verbindung; bei `execute()`/`onResponse()` muss der Body geschlossen werden (auch im Fehlerzweig).
- **Versionen:** by-design, alle.
- **FIX:** Body immer schließen, am besten `response.use { }` (Kotlin) / try-with-resources (Java). Debug: `Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE` zeigt Leak-Allokation.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-response-body/index.html

### L2. `byteStream()`/`charStream()`/`source()` schließen NICHT automatisch; Body nur EINMAL lesbar
- **Symptom:** stiller Leak bei Streaming; oder `IllegalStateException: closed`/leerer String beim zweiten Lesen.
- **Ursache:** `string()`/`bytes()` lesen alles + schließen; die Streaming-Zugriffe NICHT. Body ist one-shot.
- **Versionen:** by-design, alle.
- **FIX:** Streaming selbst schließen (`byteStream().use { }`); mehrfach gebraucht → einmal in `String`/`ByteArray` puffern und wiederverwenden.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-response-body/index.html

### L3. Retrofit: `errorBody()` und `@Streaming` muss man SELBST schließen
- **Symptom:** Leak-Warnung trotz Retrofit; `cannot make a new request because the previous response is still open`.
- **Ursache:** Erfolgreiche Antworten mit normalem Typ schließt der Converter; `errorBody()` und `@Streaming`/`Response<ResponseBody>` NICHT.
- **Versionen:** Retrofit 2.x inkl. 2.11.0.
- **FIX:** `response.errorBody()?.use { it.string() }`; `@Streaming`-Body mit `use { }` schließen.
- **Quelle:** https://github.com/square/retrofit/issues/2950

### L4. Kein `callTimeout` (Default 0 = unendlich) → Request hängt ewig ⭐ HAEUFIG
- **Symptom:** Request hängt scheinbar unbegrenzt, obwohl connect/read/write je 10 s gesetzt sind.
- **Ursache:** OkHttp hat per Default KEINEN Gesamt-Timeout (`callTimeout = 0`). Die 10-s-Defaults gelten nur pro I/O-Phase; bei tröpfelnden Daten/Redirects greift keiner.
- **Versionen:** `callTimeout` seit 3.12; Default 0 by-design in 4.12.0/5.x.
- **FIX:** `callTimeout(30, TimeUnit.SECONDS)` setzen (harte Gesamtgrenze inkl. DNS/Connect/Redirects/Body). Pro-Request: `call.timeout().timeout(...)`.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-ok-http-client/-builder/call-timeout.html

### L5. read/connect/call-Timeout verwechselt; SSE/Streaming braucht `readTimeout(0)`
- **Symptom:** SSE/Long-Polling bricht nach 10 s mit `SocketTimeoutException: timeout` ab, obwohl nur „nichts passiert".
- **Ursache:** `readTimeout` (10 s) = max Pause zwischen Reads; bei langlebigen Streams tödlich.
- **Versionen:** unverändert 4.12.0/5.x.
- **FIX:** für SSE `readTimeout(0)` auf einem per `newBuilder()` abgeleiteten Client (normale Requests behalten 10 s). Gegen unerkannt tote Streams (#2611): Server-Heartbeat oder großzügiger statt exakt 0.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-ok-http-client/-builder/read-timeout.html · https://github.com/square/okhttp/issues/2611

### L6. Mehrere `OkHttpClient`-Instanzen statt Singleton → Pools/Threads vervielfacht ⭐ HAEUFIG
- **Symptom:** steigender Speicher/Thread-Verbrauch, viele idle `OkHttp ConnectionPool`/`Dispatcher`-Threads, kein Connection-Reuse.
- **Ursache:** Jeder Client hat eigenen ConnectionPool + Dispatcher-Threads. Pro-Request/Screen ein neuer Client = Ressourcen-Leak.
- **Versionen:** by-design, alle.
- **FIX:** EIN `OkHttpClient`-Singleton pro App; Varianten via `newBuilder()` (teilt Pool/Dispatcher). Shutdown: `dispatcher.executorService.shutdown()` + `connectionPool.evictAll()`.
- **Quelle:** https://square.github.io/okhttp/recipes/

### L7. ConnectionPool/Dispatcher-Defaults: `maxRequestsPerHost = 5` drosselt
- **Symptom:** viele parallele Requests gegen denselben Host stauen sich (nur 5 gleichzeitig).
- **Ursache:** Dispatcher-Defaults `maxRequests = 64`, `maxRequestsPerHost = 5`; Pool 5 idle / 5 min.
- **Versionen:** unverändert.
- **FIX:** `client.dispatcher.maxRequestsPerHost = 20` (wenn der Server es verträgt); `evictAll()` zum gezielten Räumen idle-Verbindungen.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-connection-pool/index.html

### L8. Coroutine-Cancellation cancelt den OkHttp-Call nicht sauber → Leak
- **Symptom:** Coroutine gecancelt, aber HTTP-Request läuft weiter; Body nie konsumiert → Leak.
- **Ursache:** Selbstgebauter `suspendCoroutine`-Wrapper / `execute()` in `withContext` ruft kein `call.cancel()`.
- **Versionen:** `okhttp-coroutines` (`call.executeAsync()`, bidirektionale Cancellation) ist Teil von **OkHttp 5.x**, NICHT in 4.12.0.
- **FIX:** mit OkHttp 4.12.0: `gildor/kotlin-coroutines-okhttp` oder eigener `suspendCancellableCoroutine` + `invokeOnCancellation { call.cancel() }`. Retrofit-suspend bindet die Cancellation bereits; `@Streaming`-Bodies trotzdem im `finally`/`use` schließen.
- **Quelle:** https://github.com/square/okhttp/blob/master/okhttp-coroutines/README.md · https://github.com/gildor/kotlin-coroutines-okhttp

---

## A) Retrofit-Annotationen (@Body/@Field/@Query/@Path/@Header)

> Alle **by-design**-Validierungen — werfen seit Retrofit 2.0 beim ERSTEN Methodenaufruf
> (`RequestFactory.parseAnnotations`), nicht beim Compile. Funktionserhaltende Fixes.

### A1. `@Field` ohne `@FormUrlEncoded` ⭐ HAEUFIG
- **Symptom:** `IllegalArgumentException: @Field parameters can only be used with form encoding. (parameter #1)` (Laufzeit).
- **Ursache:** `@Field` erzeugt ein form-urlencoded-Paar; ohne `@FormUrlEncoded` weiß Retrofit nicht, wie es kodieren soll.
- **Versionen:** by-design seit 2.0.
- **FIX:** `@FormUrlEncoded` an die Methode. (JSON gewünscht? → `@Body data class` statt `@Field`, dann KEIN `@FormUrlEncoded`.)
- **Quelle:** https://square.github.io/retrofit/2.x/retrofit/retrofit2/http/Field.html

### A2. `@Body` mit `@FormUrlEncoded`/`@Multipart` kombiniert
- **Symptom:** `IllegalArgumentException: @Body parameters cannot be used with form or multi-part encoding.`
- **Ursache:** `@Body` (ganzes Objekt) und Form/Multipart definieren konkurrierende Body-Formate.
- **Versionen:** by-design.
- **FIX:** Format entscheiden — `@Body` ohne Encoding-Annotation, ODER `@FormUrlEncoded`+`@Field`.
- **Quelle:** https://square.github.io/retrofit/2.x/retrofit/retrofit2/http/Field.html

### A3. `@Body` bei GET/HEAD/DELETE (Non-Body-Methoden)
- **Symptom:** `IllegalArgumentException: Non-body HTTP method cannot contain @Body or @TypedOutput.`
- **Ursache:** Retrofit verbietet Body bei GET/HEAD/DELETE.
- **Versionen:** by-design.
- **FIX:** GET → `@Query`/`@Path`; DELETE mit Body wirklich nötig → `@HTTP(method="DELETE", path="...", hasBody=true)`.
- **Quelle:** https://github.com/square/retrofit/issues/2598

### A4. `@Path`-Platzhalter fehlt/Tippfehler ⭐ HAEUFIG
- **Symptom:** `URL ... does not contain {id}` (Platzhalter fehlt im Pfad) bzw. `Missing parameter for {id}` (Platzhalter ohne Parameter).
- **Ursache:** Name-Mismatch zwischen `{template}` und `@Path("value")`.
- **Versionen:** by-design.
- **FIX:** Namen exakt angleichen; `@Path`-Werte dürfen NICHT null sein (`Path parameter "id" value must not be null.`).
- **Quelle:** https://square.github.io/retrofit/2.x/retrofit/retrofit2/http/Path.html

### A5. `@Path` Doppel-Encoding / Sonderzeichen (`encoded=true/false`)
- **Symptom:** stiller Logikfehler: bereits kodierte Werte werden doppelt kodiert (`%` → `%25`) → 404.
- **Ursache:** `@Path` kodiert per Default; bei schon kodiertem Wert doppelt.
- **Versionen:** by-design.
- **FIX:** Default für rohe Werte; nur bei nachweislich vorkodiertem Wert `encoded=true` (kodiert dann aber auch `/` nicht).
- **Quelle:** https://square.github.io/retrofit/2.x/retrofit/retrofit2/http/Path.html

### A6. `@Query` Encoding / `+` und Leerzeichen
- **Symptom:** `+` kommt als Leerzeichen an (oder umgekehrt) — stiller Logikfehler.
- **Ursache:** `@Query(encoded=true)` kodiert nicht; Server-Decoding mehrdeutig.
- **Versionen:** by-design.
- **FIX:** Default (`encoded=false`) lassen; `encoded=true` nur für garantiert gültige Query-Tokens.
- **Quelle:** https://square.github.io/retrofit/2.x/retrofit/retrofit2/http/Query.html

### A7. `@QueryMap`/`@FieldMap` mit null-Werten
- **Symptom:** `IllegalArgumentException: Query map contained null value for key 'xyz'.`
- **Ursache:** Doku↔Implementierung-Diskrepanz: einzelne `@Query`-null werden ignoriert, Map-null NICHT.
- **Versionen:** by-design (#2741/#1677).
- **FIX:** null-Einträge vor dem Aufruf weglassen; optionale Parameter als einzelne nullable `@Query` deklarieren.
- **Quelle:** https://github.com/square/retrofit/issues/2741

### A8. `@Header` (dynamisch) vs `@Headers` (statisch); null-Verhalten
- **Symptom:** falsche Annotation → Header fehlt/fest; `@Header`-Wert null → Header-Zeile STILL weggelassen (→ 401); gleicher Name 2× → zwei Zeilen (kein Überschreiben).
- **Ursache:** `@Header` = dynamisch (darf null = weggelassen), `@Headers` = statisch.
- **Versionen:** by-design.
- **FIX:** dynamisch `@Header("Authorization")`-Parameter; statisch `@Headers("Accept: application/json")`. Fehlen via Interceptor/Logik-Sonde aufdecken.
- **Quelle:** https://square.github.io/retrofit/2.x/retrofit/retrofit2/http/Header.html

### A9. baseUrl ohne Trailing-Slash / führender `/` im Endpoint ⭐ HAEUFIG
- **Symptom:** `IllegalArgumentException: baseUrl must end in /` (Crash) ODER 404, weil `@GET("/x")` (führender Slash) den baseUrl-Pfad ersetzt.
- **Ursache:** Retrofit löst URLs wie `<a href>` (OkHttp `HttpUrl`): führender Slash = absolut ab Host; baseUrl ohne `/` würde letztes Segment verschlucken.
- **Versionen:** by-design.
- **FIX:** baseUrl IMMER mit `/` enden (`.../v2/`), Endpoints OHNE führenden Slash (`users/me`).
- **Quelle:** https://github.com/square/retrofit/issues/2010

---

## SEC) Sicherheit: HTTP-Logging-Secrets & Certificate-Pinning

### SEC1. `HttpLoggingInterceptor` Level BODY/HEADERS in Produktion leakt Tokens/PII ⭐ HAEUFIG
- **Symptom:** Authorization/Cookies/Tokens + ganze Bodies (Passwörter/PII) im Klartext in Logcat — per `adb logcat` auslesbar. Kein Crash, reines Sicherheitsleck.
- **Ursache:** `Level.BODY`/`HEADERS` loggt ALLES ungefiltert.
- **Versionen:** by-design; `redactHeader()` seit OkHttp 3.4, in 4.12.0 vorhanden.
- **FIX:** in Release `Level.NONE`, sensible Header IMMER schwärzen:
  ```kotlin
  HttpLoggingInterceptor().apply {
      level = if (BuildConfig.DEBUG) Level.BODY else Level.NONE
      redactHeader("Authorization"); redactHeader("Cookie"); redactHeader("Set-Cookie")
  }
  ```
- **Quelle:** https://github.com/square/okhttp/blob/master/okhttp-logging-interceptor/README.md · https://github.com/square/okhttp/issues/3826

### SEC2. Falscher Pin / falsches Hash-Format → `SSLPeerUnverifiedException`
- **Symptom:** `Certificate pinning failure!`; alle TLS-Verbindungen zur Domain scheitern.
- **Ursache:** Pin passt zu keinem Chain-Zertifikat. Format MUSS `sha256/<base64-SHA256 des SubjectPublicKeyInfo>` sein (nicht der Zertifikats-Fingerprint).
- **Versionen:** by-design (Exception gewollt).
- **FIX:** korrekten Pin aus der Exception-Meldung übernehmen (OkHttp druckt die tatsächlichen Peer-Pins). `CertificatePinner.Builder().add("host", "sha256/...=")`.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-certificate-pinner/index.html

### SEC3. Fehlender Backup-Pin → App-Brick bei Cert-Rotation ⭐ HAEUFIG
- **Symptom:** App läuft monatelang, dann nach Server-Cert-Erneuerung schlagen schlagartig ALLE Requests fehl — nur ein App-Update behebt es (Nutzer alter Versionen dauerhaft offline).
- **Ursache:** Nur EIN Pin (aufs aktuelle Leaf) gesetzt; bei Rotation ändert sich der Public-Key-Hash.
- **Versionen:** konzeptionelles Risiko, versionsunabhängig.
- **FIX:** IMMER ≥ 1 Backup-Pin (Reserve-Schlüssel oder Intermediate-CA). Eine Übereinstimmung in der Chain genügt. Alternative: Intermediate pinnen (rotiert seltener) oder `network_security_config.xml` mit `<pin-set>`.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-certificate-pinner/index.html · https://developer.android.com/privacy-and-security/security-config

### SEC4. `HttpLoggingInterceptor.Level.BODY` auf Chat-/LLM-Hot-Path → Antwort wird spät sichtbar
- **Symptom:** Ein Prompt an einen Agenten/LLM fühlt sich in der App langsamer an, obwohl der Server antwortet; lange Prompts oder lange Antworten verschärfen es.
- **Ursache:** `Level.BODY` serialisiert und loggt Request- und Response-Bodies synchron im OkHttp-Interceptor. Wenn der Logger zusätzlich nach Logcat und in eine Datei schreibt, entsteht genau vor/nach dem Netzcall String-/Allocation-/File-IO-Druck auf dem Antwortpfad.
- **Versionen:** by-design bei OkHttp logging-interceptor 4.x/5.x; besonders sichtbar bei Mobile-Debug-Builds mit großen JSON-Bodies.
- **FIX:** BODY-Logging nicht auf heißen Chat-/Streaming-/LLM-Pfaden verwenden. Stattdessen `BASIC`/`NONE` plus eigenen schlanken Timing-Interceptor loggen (`method`, `path`, `status`, `elapsed_ms`, Größen), keine Prompt-/Antwortinhalte. Falls Body-Debugging nötig ist: temporär, manuell, eng begrenzt und nie für normale Nutzung.
- **Quelle:** CortexAndroid 2026-07-01: `ApiClient.authClient` loggte Agent-Chat mit `Level.BODY`; Fix: Level `BASIC` + Timing-Interceptor + gecachte Retrofit-Service-Proxies.

### SEC5. Pin-Ablauf / Subdomain-Pattern (`*` vs `**`) / Leaf vs Intermediate / Cleartext
- **Symptom:** `<pin-set expiration>` abgelaufen → Pinning still deaktiviert; `*.example.com` matcht NUR genau ein Präfix-Label (nicht `example.com`, nicht `a.b.example.com`); Leaf-Pin bricht bei jeder Cert-Erneuerung; `http://`-Downgrade umgeht Pinning ganz.
- **Ursache:** Pattern-/Lebenszeit-/Layer-Fallen; Pinning schützt nur TLS.
- **Versionen:** network_security_config ab API 24; Cleartext-Default-aus ab API 28.
- **FIX:** für breite Abdeckung `**.example.com`; Intermediate pinnen ODER Leaf + Backup; Cleartext explizit `cleartextTrafficPermitted="false"`; `expiration` bewusst wählen (Connectivity vs Schutz).
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-certificate-pinner/index.html · https://developer.android.com/privacy-and-security/security-config

---

## ✅ Fix-Status (was ist schon behoben?)

> **Ehrlichkeits-Regel:** „belegt gefixt" = Changelog/Maintainer/direkt verifizierte Datei. `gh`-CLI war in
> dieser Umgebung nicht verfügbar; die zwei wichtigsten Anker (`retrofit2.pro` 2.11.0, `moshi.pro` 1.15.1)
> wurden per direktem Datei-Fetch der getaggten Quelle verifiziert.

### Belegt gefixt (bis zum Projekt-Anker)

| Früherer Bug | gefixt ab | Beleg | Bezug |
|--------------|-----------|-------|-------|
| R8: Retrofit-Interface zu null / Signaturen / `Continuation` gestrippt | **Retrofit 2.10.0** (in 2.11.0 **verifiziert**) | retrofit2.pro@2.11.0 (Direkt-Fetch) | R1, R2, R3 |
| Moshi-codegen-Adapter durch R8 entfernt | **Moshi 1.10.0** (on-the-fly precise rules; in 1.15.1 vorhanden) | CHANGELOG + moshi.pro@1.15.1 | R5 |
| Moshi-`@JsonClass`-Enums / Adapter-Methoden / Metadata-Helfer | in moshi.pro@1.15.1 **verifiziert** | Direkt-Fetch | R7 |
| suspend `UndeclaredThrowableException` | Teilfixes 2.6.1–2.6.3, **final 2.10.0** (in 2.11.0 erledigt) | CHANGELOG | S7 |
| `Unit` als direkter suspend-Body-Typ | **Retrofit 2.10.0** (`Response<Unit>` schon 2.6.0) | CHANGELOG | S3 |
| Eager-Reject `suspend fun: Call<Body>` | **Retrofit 2.10.0** | CHANGELOG | S8 |
| Listen-Nullability (`List<String>?` fälschlich non-null, #526) | gefixt (in 1.15.1 korrekt) | #526 | D7 |

→ **BestJournalAndroid (Retrofit 2.11.0 + Moshi 1.15.1, R8) ist gegen die Kern-R8-Release-Crashes by-default geschützt** — VORAUSGESETZT Moshi-codegen (`@JsonClass`) statt Reflection (R6).

### Noch NICHT gefixt / Workaround aktiv (in 1.15.1 / 4.12.0)

| Bug | Status | Was tun | Bezug |
|-----|--------|---------|-------|
| Moshi-**Reflection**-Modelle durch R8 entfernt | by-design — moshi.pro keept Modelle NICHT | codegen nutzen ODER Modelle manuell keepen | R6 |
| Polymorphie: Fallback bei FEHLENDEM Label-Key | erst **2.0.0-alpha.1** gefixt | Label als erstes Feld; `withDefaultValue` | M5 |
| `EnumJsonAdapter.withUnknownFallback` bei explizitem `null` (#721) | in 1.15.1 offen | nullable Enum + Default kombinieren | D6 |
| `failOnUnknown()` nennt falschen Feldnamen (#939) | offen | nur als Debug-Hilfe nutzen | D5 |
| OkHttp `callTimeout` Default 0 = unendlich | by-design | `callTimeout(30, SECONDS)` selbst setzen | L4 |
| `okhttp-coroutines` (`executeAsync`) | erst **OkHttp 5.x** | mit 4.12.0 Fremd-Lib/eigener Wrapper | L8 |
| Default/Null & Annotations-Validierungen (D1–D10, A1–A9) | by-design | korrekte Modellierung/Annotation | D*, A* |

---

## ✅ Pflicht-Checkliste (vor dem Commit von Networking-Code mental durchgehen)

- [ ] **Release getestet:** `assembleRelease` mit `minifyEnabled true` baut UND startet; API-Call gegen echten Server erfolgreich. (R1–R10)
- [ ] **R8-Regeln:** R8 (nicht ProGuard) aktiv → Retrofit/Moshi-Regeln kommen automatisch; `-dontwarn` für conscrypt/bouncycastle/openjsse gesetzt; `missing_rules.txt` nach dem Build geprüft. (R8, R9, R10)
- [ ] **Moshi:** codegen (`@JsonClass(generateAdapter=true)`) statt Reflection; `ksp(...)` statt `kapt(...)`; Enums `@JsonClass(generateAdapter=false)`. (M1, M2, R6, R7)
- [ ] **Modell-null-Safety:** Felder, die der Server nullen/weglassen kann, sind `nullable` + Default; explizites-null-Verhalten via Backing-Property bedacht. (D1, D2)
- [ ] **Server-Keys:** `@Json(name=)` für abweichende Keys; unbekannte Enum-Werte mit Fallback. (D4, D6)
- [ ] **suspend-Fehler:** `HttpException` + `IOException` gefangen; `CancellationException` ZUERST rethrowen; 204 → `Response<Unit>`/`Unit`. (S1, S3, S6)
- [ ] **OkHttp-Client:** EIN Singleton (Varianten via `newBuilder()`); `callTimeout` gesetzt; Bodies via `use{}` geschlossen (inkl. `errorBody()`/`@Streaming`). (L1, L3, L4, L6)
- [ ] **Interceptors:** Token als Application-Interceptor, 401-Refresh per `Authenticator`; Retry/Timeout im Application-Layer; Logging an der richtigen Position. (I1, I2, I5, I6)
- [ ] **Sicherheit:** HTTP-Logging in Release `Level.NONE` + `redactHeader`; falls Cert-Pinning → Backup-Pin gesetzt. (SEC1, SEC3)
- [ ] **Annotationen:** `@FormUrlEncoded` bei `@Field`; baseUrl mit `/`, Endpoints ohne führenden `/`; `@Path`-Namen exakt. (A1, A4, A9)

---

## Bezug: Bug-Abschnitt ↔ Best-Practices

> Gegenseite (wie macht man es richtig):
> [`best-practices/android/retrofit-okhttp-moshi.md`](../../best-practices/android/retrofit-okhttp-moshi.md)
> (dort die Spiegel-Tabelle Best-Practice-Abschnitt ↔ Bug-Abschnitt).

| Bug-Abschnitt (hier) | Verwandter Best-Practice-Abschnitt |
|----------------------|------------------------------------|
| R1/R2/R3 Retrofit-R8 · R9 ProGuard | §5.1 mitgelieferte Keep-Regeln |
| R5 codegen-Adapter · R6 Reflection-Modelle · R7 Enums | §2.1 codegen, §5.2/§5.3 Keeps |
| R8 Missing class TLS-Provider | §5.4 dontwarn |
| R10 missing_rules.txt | §5.5/§5.6 Release-Test/mapping |
| M1 `@JsonClass` vergessen · M2 KSP-Migration | §2.1/§2.2 codegen/KSP |
| M4 Adapter-Reihenfolge | §2.3 Adapter-Reihenfolge |
| M5 Polymorphie | §2.5 Polymorphie/Enums |
| D1/D2 null/Defaults · D4 `@Json` | §2.4 `@Json`/null-Safety, §2.7 DTO↔Domain |
| D6 Enum-Fallback | §2.5 Enum-Fallback |
| S1 suspend-Crash · S5 errorBody | §3.5 HttpException/IOException/errorBody |
| S2 Response vs T | §3.2 Response vs T |
| S3 204-NPE | §3.6 204→Unit |
| S4 Result-Wrapper | §3.9 CallAdapter |
| S6 Cancellation | §3.4 Cancellation rethrow |
| S8 suspend/`Call<T>` | §3.1 suspend |
| I1/I2 Auth/Redirect/401 | §4.2/§4.3 Token/Authenticator |
| I3/I5 Logging | §4.4 Logging-Position |
| I4 Body konsumiert | §4.7 peekBody |
| I6/I7 proceed/Retry-Leak | §4.6 Retry+close |
| L4/L5 Timeouts | §6.1/§6.2/§6.3 Timeouts |
| L6/L7 Client/Pool | §1.1/§1.8 ein Client |
| SEC1 Logging-Secrets | §6.4 sicheres Logging |
| SEC2–SEC4 Pinning | §6.5/§6.6 Pinning |
| A9 baseUrl | §1.6 baseUrl/Endpoints |
