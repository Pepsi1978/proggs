# Retrofit + OkHttp + Moshi — Best Practices (Stand 2026-06-14)

> **Zweck:** Wie man den Networking-/API-Layer mit Retrofit + OkHttp + Moshi in BestJournalAndroid
> von vornherein **richtig** baut — idiomatisch, offiziell belegt (Square-Docs, Now in Android,
> Android-Architektur-Guide, R8-Blog 11/2025). Die Gegenseite (was schiefgeht) steht im Bug-Almanach.
> **Versions-Anker (live aus BestJournalAndroid):** Retrofit **2.11.0** · OkHttp **4.12.0** · Moshi **1.15.1**
> (codegen) · converter-moshi/converter-scalars **2.11.0** · logging-interceptor **4.12.0** · KSP **2.1.0-1.0.29** ·
> Kotlin **2.1.0** · AGP 8.7.3 (R8 full mode). Neuer (nicht im Projekt): OkHttp 5.x (`okhttp-coroutines`,
> Happy-Eyeballs), Moshi 2.0.0-alpha (KSP2-only, kotlin-metadata). EntropieReductor nutzt kotlinx-serialization.
> **Gegenstück (was schiefgeht):** [`bugs/android/retrofit-okhttp-moshi.md`](../../bugs/android/retrofit-okhttp-moshi.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | OkHttpClient erzeugen | **EIN Singleton** pro App; Varianten via `newBuilder()` (teilt Pool/Dispatcher) | §1.1 |
| 2 | Retrofit-Instanz | EINE pro Backend; mehrere Services aus derselben Retrofit | §1.3 |
| 3 | Bereitstellung | Hilt `@Provides @Singleton` im `SingletonComponent` (core/network-Modul) | §1.4 |
| 4 | OkHttp-Init | lazy via `dagger.Lazy<Call.Factory>` + `callFactory{}` (nicht Main-Thread) | §1.5 |
| 5 | baseUrl/Endpoints | baseUrl endet auf `/`, Endpoints OHNE führenden `/` | §1.6 |
| 6 | Converter | additiv + geordnet (Scalars VOR JSON); JSON mit geteiltem Moshi-Singleton | §1.7 |
| 7 | Moshi-Adapter | **codegen** (`@JsonClass`) via KSP statt Reflection (kein kotlin-reflect, R8-fest) | §2.1 |
| 8 | Adapter-Reihenfolge | Custom `add()`, generische Factories `addLast()` | §2.3 |
| 9 | Server-Keys/null | `@Json(name=)`; optional → nullable + Default; Pflicht → non-null | §2.4 |
| 10 | Polymorphie/Enums | `PolymorphicJsonAdapterFactory.withDefaultValue`; `EnumJsonAdapter.withUnknownFallback` | §2.5 |
| 11 | DTO ↔ Domain | DTOs nur Transport; per Mapper auf Domain-Modelle, nicht ins UI | §2.7 |
| 12 | suspend-Service | `suspend fun`, kein `Call<T>`, KEIN manuelles `withContext(IO)` | §3.1 |
| 13 | Body vs Response | `T` (clean, Exception bei non-2xx) vs `Response<T>` (Status/Header/errorBody) | §3.2 |
| 14 | Result-Wrapping | sealed `NetworkResult`/`Resource` im Repository; ein `safeApiCall`-Helper | §3.3 |
| 15 | Cancellation | `catch (e: CancellationException) { throw e }` ZUERST; kein `runCatching` | §3.4 |
| 16 | Fehler unterscheiden | `HttpException` (non-2xx) vs `IOException` (Netz); errorBody EINMAL lesen | §3.5 |
| 17 | 204/leerer Body | `Response<Unit>`/`Unit` | §3.6 |
| 18 | Schichten | Repo → Domain + sealed Result; VM erzeugt Coroutine + `StateFlow<UiState>` | §3.7 |
| 19 | Token anhängen | Application-Interceptor (`header("Authorization", ...)` via `newBuilder()`) | §4.2 |
| 20 | 401-Refresh | `Authenticator` (nicht Interceptor); `synchronized` + `responseCount`-Limit | §4.3 |
| 21 | Logging-Position | als LETZTER Application-Interceptor; Level build-abhängig | §4.4 |
| 22 | Retry im Interceptor | vorige Response `close()` vor erneutem `proceed`; besser Coroutine-Ebene | §4.6 |
| 23 | Body inspizieren | `response.peekBody(≤1 MiB)` statt `body.string()` | §4.7 |
| 24 | Keep-Regeln | den **mitgelieferten** Lib-Regeln vertrauen (R8); nichts manuell kopieren | §5.1 |
| 25 | DTO-Keeps | mit codegen KEINE; nur bei Reflection + dann schmal/`@Keep` | §5.2 |
| 26 | Release-Test | `assembleRelease` + minify + echter API-Call im CI; `missing_rules.txt` prüfen | §5.5 |
| 27 | mapping.txt | pro Release archivieren (Crash-Deobfuskierung) | §5.6 |
| 28 | R8-Setup | `proguard-android-optimize.txt`; nie `-dontoptimize/-obfuscate/-shrink` in Release | §5.7 |
| 29 | KSP/Catalog | KSP statt kapt; KSP-Version = Kotlin exakt; Version-Catalog | §5.8 |
| 30 | callTimeout | `callTimeout(...)` als hartes Dach setzen (Default 0 = unendlich) | §6.1 |
| 31 | SSE/Streaming | `readTimeout(0)` + Ping/Heartbeat; KEIN callTimeout | §6.3 |
| 32 | Logging-Secrets | Debug→BODY/Release→NONE + `redactHeader("Authorization"/"Cookie")` | §6.4 |
| 33 | Cert-Pinning | nur bewusst; IMMER Backup-Pin; `**.domain`; oder network_security_config | §6.5 |
| 34 | Chat-/LLM-Hot-Path | Kein BODY-Logging im normalen Prompt-Pfad; Timing/Status/Größen statt Body | §4.4 |

---

## 1) Client-Architektur

### 1.1 Genau EIN OkHttpClient pro App
`offiziell`
- „OkHttp performs best when you create a single `OkHttpClient` instance and reuse it for all of your HTTP calls. … each client holds its own connection pool and thread pools. Reusing connections and threads reduces latency and saves memory." Varianten (anderes Timeout pro Call) via `client.newBuilder()` — teilt Connection-Pool, Thread-Pools, Konfiguration. Kein `shutdown()` nötig (idle Threads/Connections werden automatisch frei).
- **DON'T:** `OkHttpClient()` pro Request/Repository/ViewModel neu bauen.
- **Quelle:** https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/

### 1.3 EINE Retrofit-Instanz pro Backend, mehrere Services daraus
`offiziell`
- Pro `baseUrl` genau eine `Retrofit`-Instanz; `retrofit.create(XxxApi::class.java)` ist billig (Proxy) — alle Services teilen Client/Converter/CallAdapter.
- **Quelle:** https://square.github.io/retrofit/configuration/

### 1.4 Bereitstellung via Hilt: `@Provides @Singleton` im `SingletonComponent`
`offiziell` (Now in Android)
- OkHttpClient, Json/Moshi-Converter und Network-DataSource in einem `@Module @InstallIn(SingletonComponent::class)` als `@Provides @Singleton`, in einem eigenen `core/network`-Modul. Liefert app-weit dieselbe Instanz — exakt die „ein Client pro App"-Semantik ohne handgeschriebenes Singleton.
- **DON'T:** unscoped bereitstellen (Hilt erzeugt sonst pro Injektion eine neue Instanz).
- **Quelle:** https://github.com/android/nowinandroid (`core/network/.../di/NetworkModule.kt`)

### 1.5 OkHttp lazy initialisieren (nicht auf dem Main-Thread)
`offiziell` (Now in Android)
- `dagger.Lazy<Call.Factory>` injizieren und Retrofit `callFactory { lazy.get().newCall(it) }` geben — NiA-Kommentar: „to prevent initializing OkHttp on the main thread". Retrofit baut sonst (ohne `client()`/`callFactory()`) einen impliziten Default-Client.
- **Quelle:** https://github.com/android/nowinandroid (`core/network/.../retrofit/RetrofitNiaNetwork.kt`)

### 1.6 baseUrl endet auf `/`, Endpoints ohne führenden `/`
`offiziell`
- „Base URLs should always end in `/`." Ein führender `/` am Endpoint ist absolut und verwirft den Pfadanteil der baseUrl. Auflösung wie ein `<a href>`-Link.
- **Quelle:** https://square.github.io/retrofit/2.x/retrofit/retrofit2/Retrofit.Builder.html#baseUrl-okhttp3.HttpUrl-

### 1.7 Converter additiv + geordnet; geteilter Moshi-Singleton
`offiziell`
- Converter werden in Aufrufreihenfolge geprüft (erste passende gewinnt): **Scalars VOR JSON** registrieren, sonst (de)serialisiert der JSON-Converter simple Strings. JSON-Converter mit GETEILTEM Moshi-Singleton: `MoshiConverterFactory.create(moshi)` (nicht das parameterlose `.create()`, das eine eigene leere Moshi-Instanz baut → kennt eure Custom-Adapter nicht).
- **Quelle:** https://square.github.io/retrofit/configuration/

### 1.8 ConnectionPool/Dispatcher: Defaults vertrauen, gezielt anpassen
`offiziell`
- Defaults (Pool: 5 idle/5 min; Dispatcher: `maxRequestsPerHost = 5`) sind mobil sinnvoll. Nur bei nachgewiesenem Engpass `maxRequestsPerHost` erhöhen. Der Single-Client garantiert EINEN gemeinsamen Pool (Connection-Reuse, niedrigere Latenz, Akku).
- **Quelle:** https://square.github.io/okhttp/features/connections/

### 1.9 Service-Interface (API) ↔ Repository (Domain) trennen
`offiziell` (Now in Android)
- Retrofit-`@GET`-Interface gibt nur DTOs zurück (oft `internal`/`private`). Eine Repository/DataSource-Schicht kapselt Retrofit, mappt DTOs → Domain-Modelle. ViewModels/Repos sind von Retrofit entkoppelt.
- **DON'T:** Retrofit-Service direkt ins ViewModel injizieren / DTOs bis in die UI reichen.
- **Quelle:** https://github.com/android/nowinandroid

---

## 2) Moshi

### 2.1 Codegen (`@JsonClass`) via KSP statt Reflection
`offiziell`
- „Prefer codegen for better performance and to avoid the `kotlin-reflect` dependency" (Reflection zieht ~2,5 MiB). Codegen erzeugt pro Klasse einen Compile-Zeit-Adapter, ist R8-freundlich (präzise Keep-Regeln on-the-fly seit 1.10.0). Reflection nur, wenn `private`/`protected`-Properties nötig sind.
  ```kotlin
  @JsonClass(generateAdapter = true) data class User(val id: String, val name: String)
  ```
- **Quelle:** https://github.com/square/moshi (README, Kotlin/Limitations)

### 2.2 KSP statt kapt — Version an Kotlin koppeln
`offiziell`
- `ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")` (kapt deprecatet, fliegt in 2.0 raus). KSP-Version = `<Kotlin>-<KSP-Build>` (`2.1.0-1.0.29` ↔ Kotlin 2.1.0).
- **Quelle:** https://github.com/square/moshi (README, Codegen-Setup)

### 2.3 Adapter-Reihenfolge: spezifisch `add()`, generisch `addLast()`
`offiziell`
- „If a type can be matched [by] multiple adapters, the earliest one wins." Custom-Adapter mit `.add()` (vorne), `KotlinJsonAdapterFactory`/generische Factories mit `.addLast()` (hinten).
- **Quelle:** https://github.com/square/moshi (README, Precedence)

### 2.4 `@Json(name=)`, null-Safety & Defaults über das Typsystem
`offiziell`
- Property idiomatisch (camelCase), Server-Key via `@Json(name=)`; `@Json(ignore = true)` (mit Default) für ausgelassene Felder. Pflichtfelder non-null (fehlt → `JsonDataException`), optionale nullable ODER mit Default. „It understands Kotlin's non-nullable types and default parameter values." Moshi hat KEINE globale Naming-Strategy (Mapping steht am Feld).
- **Quelle:** https://github.com/square/moshi (README, Kotlin / Custom field names / Omitting fields)

### 2.5 Forward-Compatibility: Polymorphie & Enums mit Fallback
`offiziell`
- Sealed-Hierarchien: `PolymorphicJsonAdapterFactory.of(Base::class.java, "type").withSubtype(...).withDefaultValue(Unknown)` (braucht `moshi-adapters`). Enums: `@Json(name=)` + `EnumJsonAdapter.create(E::class.java).withUnknownFallback(E.UNKNOWN)`. **Wichtig:** Fallback greift NICHT bei explizitem `null`/absent/nicht-String → Enum-Property zusätzlich nullable + Default. Enums mit `@JsonClass(generateAdapter = false)` annotieren (R8-Schutz).
- **Quelle:** https://github.com/square/moshi/blob/master/moshi-adapters/src/main/java/com/squareup/moshi/adapters/EnumJsonAdapter.kt

### 2.6 Custom-Adapter (`@ToJson`/`@FromJson`) + `nullSafe()`
`offiziell`
- Plattform-/Spezialtypen (Date, BigDecimal) brauchen einen Adapter — Moshi verweigert bewusst die Serialisierung von `java.*`/`android.*` ohne nutzerseitigen Adapter. Für Date offiziell `Rfc3339DateJsonAdapter` (moshi-adapters). Adapter, die null nicht behandeln, mit `.nullSafe()` umhüllen.
  ```kotlin
  class BigDecimalAdapter { @ToJson fun toJson(v: BigDecimal)=v.toPlainString(); @FromJson fun fromJson(s:String)=BigDecimal(s) }
  ```
- **Quelle:** https://github.com/square/moshi (README, Custom Type Adapters)

### 2.7 DTO ↔ Domain trennen — DTOs nicht ins UI
`offiziell` (README „Another example")
- Moshi-annotierte Klassen sind reine Transport-DTOs (nullable Server-Felder, `@Json`). Per Mapper auf saubere, non-null Domain-Modelle überführen. Server-Eigenheiten (snake_case, null) bleiben in der DTO-Schicht; das UI behandelt nicht jedes Server-`null`.
- **Quelle:** https://github.com/square/moshi (README)

---

## 3) Coroutinen & sauberes Result-Wrapping

### 3.1 `suspend fun` statt `Call<T>` — kein manuelles `withContext(IO)`
`offiziell`
- Retrofit-Service-Methoden direkt als `suspend fun`; Retrofit führt den Request auf OkHttps eigenem Pool aus (main-safe). Ein zusätzliches `withContext(Dispatchers.IO)` ist redundant. „Interface methods support kotlin suspend functions which directly return a `Response` object, creating and asynchronously executing the call while suspending the current function."
- **Quelle:** https://square.github.io/retrofit/declarations/

### 3.2 `Response<T>` vs direkter Body `T`
`offiziell`
- Direkter Body `T` = clean (non-2xx → `HttpException`). `Response<T>` = wenn Status/Header/errorBody gebraucht (non-2xx wirft NICHT, `isSuccessful` prüfen). Nie `Response<T>` + `body()!!`.
- **Quelle:** https://square.github.io/retrofit/declarations/

### 3.3 Sealed Result-Typ im Repository
`offiziell`
- Repository gibt sealed `Result`/`Resource<T>` (Success/Error[/Loading]) zurück, nicht nackten `T` — „making the UI aware of known errors". Ein zentraler `safeApiCall`-Helper kapselt try/catch.
  ```kotlin
  sealed interface NetworkResult<out T> {
      data class Success<T>(val data: T) : NetworkResult<T>
      data class Error(val cause: Throwable) : NetworkResult<Nothing>
  }
  ```
- **Quelle:** https://developer.android.com/topic/architecture/data-layer

### 3.4 `CancellationException` ZUERST rethrowen — kein stdlib-`runCatching`
`offiziell`
- Gecancelte Coroutine wirft `CancellationException` (Abbruch-Signal, kein Fehler). „If you must catch it, rethrow it." `kotlin.runCatching` fängt `Throwable` → schluckt Cancellation → Zombie-Coroutinen. Eigenes `runSuspendCatching`:
  ```kotlin
  try { Result.success(block()) }
  catch (c: CancellationException) { throw c }   // ZUERST
  catch (e: Throwable) { Result.failure(e) }
  ```
- **Quelle:** https://kotlinlang.org/docs/cancellation-and-timeouts.html · https://developer.android.com/kotlin/coroutines/coroutines-best-practices

### 3.5 `HttpException` (non-2xx) vs `IOException` (Netz); errorBody EINMAL lesen
`offiziell`
- Im `safeApiCall` drei Fälle: `CancellationException` (rethrow), `HttpException` (`code()` + `errorBody()?.string()` GENAU einmal → Domain-Fehler), `IOException` (offline/Timeout). „Prefer catching specific exception types like `IOException`."
- **Quelle:** https://developer.android.com/kotlin/coroutines/coroutines-best-practices

### 3.6 204/leerer Body → `Response<Unit>`/`Unit`
`offiziell`
- Endpunkte ohne Body (DELETE/204) als `suspend fun … : Response<Unit>` (Status prüfbar) oder `: Unit`. Nie einen Datentyp deklarieren, wo kein Body kommt.
- **Quelle:** https://square.github.io/retrofit/declarations/

### 3.7 Schichten: Repo → Domain + Result; VM erzeugt Coroutine + `StateFlow`
`offiziell`
- „ViewModel classes should prefer creating coroutines instead of exposing suspend functions"; „Don't expose mutable types" → `val uiState: StateFlow<…>` aus privatem `MutableStateFlow`; „data and business layer should expose suspend functions and Flows". UI-State als sealed `UiState` (Loading/Success/Error).
- **Quelle:** https://developer.android.com/kotlin/coroutines/coroutines-best-practices

### 3.8 Dispatcher injizieren, `GlobalScope` meiden
`offiziell`
- Wenn `withContext` für EIGENE blockierende Arbeit nötig: Dispatcher per Konstruktor injizieren (testbar via `TestDispatcher`). `GlobalScope` nie direkt; screen-überdauernde Arbeit an injizierten `externalScope`.
- **Quelle:** https://developer.android.com/kotlin/coroutines/coroutines-best-practices

### 3.9 Optional: eigener `CallAdapter.Factory`/`NetworkResponse`
`offiziell` (Mechanismus) · `extern` (Lib)
- Für große Codebasen lohnt ein `CallAdapter.Factory`, der `NetworkResponse<S,E>` zurückgibt → non-2xx wird zu Daten statt Exception, `safeApiCall`-Boilerplate entfällt. Für kleine Apps ist der Helper (§3.5) einfacher.
- **Quelle:** https://square.github.io/retrofit/configuration/

---

## 4) Interceptors & Authentifizierung

### 4.1 Application- vs Network-Interceptor bewusst wählen
`offiziell`
- Application (`addInterceptor`): Auth/Logging/Header — einmal, auch bei Cache-Hit, sieht „original intent". Network (`addNetworkInterceptor`): Wire/gzip/Redirect-Hops, Connection-Zugriff, NICHT bei Cache-Hits.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### 4.2 Token per Application-Interceptor anhängen
`offiziell`
- Aktuellen Token via `chain.request().newBuilder().header("Authorization", "Bearer …").build()` (immutable Request, `header()` = genau eine Belegung). Nicht den Refresh hier erledigen.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### 4.3 401-Refresh im `Authenticator`, nicht im Interceptor
`offiziell` (+ `extern` für `synchronized`-Muster)
- `OkHttpClient.Builder.authenticator(...)` wird automatisch bei 401 aufgerufen + retryt mit neuem Request. Endlosschleifen-Schutz: `if (response.responseCount >= 3) return null` und `if (request.header("Authorization") != null) return null`. `synchronized` gegen Token-Stampede (drinnen prüfen, ob anderer Thread schon erneuert hat). `return null` wenn kein Refresh möglich. Refresh über separaten Client OHNE diesen Authenticator (Rekursionsschutz).
- **Quelle:** https://square.github.io/okhttp/4.x/okhttp/okhttp3/-authenticator/ · https://github.com/square/okhttp/issues/3984

### 4.4 `HttpLoggingInterceptor` als LETZTER Application-Interceptor, Level build-abhängig
`offiziell`
- Als letzter Application-Interceptor sieht das Logging den finalen Request (inkl. Auth-Header aus §4.2). Level `if (BuildConfig.DEBUG) BODY else NONE` + `redactHeader("Authorization")`/`redactHeader("Cookie")`. „interceptors are called in order."
- Performance-Regel für Chat-/LLM-/große JSON-Hot-Paths: `BODY` nur temporär zur Diagnose. In der normalen Nutzung stattdessen `BASIC`/`NONE` plus schlanker Timing-Interceptor (`method`, `path`, `status`, `elapsed_ms`, Größen). So bleibt Observability erhalten, ohne Prompt-/Antwortinhalt synchron zu serialisieren und in Logcat/Datei zu schreiben.
- **Quelle:** https://square.github.io/okhttp/4.x/logging-interceptor/okhttp3.logging/-http-logging-interceptor/

### 4.5 Interceptor-Reihenfolge ist semantisch
`offiziell`
- Erst mutieren (Header/Auth/Kompression), dann beobachten (Logging zuletzt). Reihenfolge = Ergebnis.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### 4.6 Retry im Interceptor: vorige Response schließen — oder Coroutine-Ebene
`offiziell`
- Application-Interceptor darf mehrfach `proceed()` rufen, MUSS aber jede vorige Response vor dem nächsten `proceed` `close()`en (sonst Connection-Leak). Backoff besser auf Coroutine-Ebene (`retryWhen`) — kein blockierendes `Thread.sleep` auf dem Dispatcher-Thread. `retryOnConnectionFailure` (Default true) deckt nur Verbindungsfehler ab, NICHT 5xx/429.
- **Quelle:** https://square.github.io/okhttp/features/interceptors/

### 4.7 Header via `newBuilder()`, Body zerstörungsfrei via `peekBody`
`offiziell`
- Request/Response nie in-place ändern. Zum Inspizieren `response.peekBody(1024*1024)` (lädt in Speicher → Limit setzen) statt `body.string()` (konsumiert + schließt).
- **Quelle:** https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response/peek-body/

---

## 5) R8/Keep-Regeln & Gradle/KSP

### 5.1 Den mitgelieferten Library-Keep-Regeln vertrauen
`offiziell`
- Retrofit (`retrofit2.pro` ab 2.10 mit R8-full-mode-Interface/Continuation/Signature-Keeps), OkHttp (`okhttp3.pro`) und Moshi-codegen liefern ihre Keep-Regeln automatisch mit — R8 zieht sie. Android-Blog 11/2025: „you should not have to write your own rules for these." Nur ProGuard-Nutzer kopieren manuell.
- **DON'T:** alte `-keep class retrofit2.**`-Blöcke einfügen (redundant, sabotieren Optimierung).
- **Quelle:** https://square.github.io/okhttp/features/r8_proguard/ · https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html

### 5.2 DTO-Keeps nur bei Reflection — codegen braucht keine
`offiziell`
- „The best Keep Rule is no Keep Rule." Moshi-codegen → keine DTO-Keeps. Reflection → manuelle Keeps Pflicht. Enums mit `@JsonClass(generateAdapter = false)`.
- **Quelle:** https://github.com/square/moshi#r8--proguard

### 5.3 Reflektiv genutzte DTOs: schmale `@Keep`/annotations-/ancestor-Regeln
`offiziell`
- `androidx.annotation.@Keep` an der Klasse, oder `-keepclassmembers class * implements …SerializableModel { <fields>; }` / `@MyAnnotation <fields>`. Niemals breite Paket-Wildcards; den `!`-Operator NICHT naiv nutzen (`-keep class !pkg.**` keept die GESAMTE restliche App).
- **Quelle:** https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html

### 5.4 `-dontwarn` für optionale TLS-Provider — kommt schon mit
`offiziell`
- `okhttp3.pro` enthält bereits `-dontwarn` für conscrypt/bouncycastle/animal_sniffer; `retrofit2.pro` für `javax.annotation`/`kotlin.Unit`. Eigenes `-dontwarn` nur für nicht-gebundelte Transitive (z.B. OpenJSSE).
- **Quelle:** https://github.com/square/okhttp/blob/master/okhttp/okhttp3.pro

### 5.5 Release-Build mit minify + echtem API-Call im CI testen
`offiziell`
- R8 ist statische Analyse — Proxy-/Reflection-Probleme zeigen sich erst im obfuskierten Release zur Laufzeit. Im CI `assembleRelease` (`isMinifyEnabled=true`, `isShrinkResources=true`) + Instrumented Test mit echtem Request gegen die Release-Variante. `missing_rules.txt` (`outputs/mapping/release/`) als Gate prüfen.
- **Quelle:** https://developer.android.com/topic/performance/app-optimization/test-the-optimization

### 5.6 `mapping.txt` pro Release archivieren
`offiziell`
- Ohne passende `mapping.txt` sind Produktions-Stacktraces nicht deobfuskierbar. Pro Versionsnummer archivieren / in Play Console hochladen.
- **Quelle:** https://developer.android.com/studio/build/shrink-code

### 5.7 R8 full mode + `proguard-android-optimize.txt`; keine globalen Disable-Flags
`offiziell`
- `getDefaultProguardFile("proguard-android-optimize.txt")` (nicht `proguard-android.txt`) + eigene `proguard-rules.pro`. Nie `-dontoptimize`/`-dontobfuscate`/`-dontshrink` in Release (nur temporäres Debugging).
- **Quelle:** https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html

### 5.8 KSP statt kapt; KSP-Version an Kotlin koppeln; Version-Catalog
`offiziell`
- moshi-codegen über KSP (`ksp("…:moshi-kotlin-codegen:1.15.1")`), kapt deprecatet. KSP-Version = `<Kotlin>-<KSP>` (`2.1.0-1.0.29`). Alle Versionen zentral im `gradle/libs.versions.toml` (Single Source of Truth, hält Kotlin↔KSP synchron). Keep-Regeln versioniert + kommentiert; in Multi-Modul via `consumerProguardFiles`.
- **Quelle:** https://github.com/square/moshi#codegen · https://kotlinlang.org/docs/ksp-overview.html

---

## 6) Timeouts, sicheres Logging & Certificate-Pinning

### 6.1 `callTimeout` als hartes Dach setzen (Default 0 = unendlich)
`offiziell`
- `callTimeout(...)` umfasst den GESAMTEN Call (DNS, Connect, Body, Serververarbeitung, Response, Redirects/Retries). Ohne ihn kann ein tröpfelnder Server/Redirects den Call faktisch unbegrenzt laufen lassen, obwohl jede Phase ihr 10-s-Limit hält.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-ok-http-client/-builder/call-timeout.html

### 6.2 connect/read/write bewusst setzen (Default je 10 s); Pro-Request via `newBuilder()`
`offiziell`
- Phasen-Timeouts an die reale Backend-Latenz anpassen (Mobilfunk großzügiger). `readTimeout` gilt pro Read-Operation, nicht über die ganze Antwort → braucht zusätzlich `callTimeout`. Ausreißer-Endpoint: `client.newBuilder().readTimeout(...).build()` (teilt Pool) statt neuer Client.
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-ok-http-client/index.html · https://square.github.io/okhttp/recipes/

### 6.3 SSE/Streaming: `readTimeout(0)` + Ping/Heartbeat, KEIN callTimeout
`offiziell`
- Für lange Streams `readTimeout(0)` (kein Timeout) auf einem abgeleiteten Client + `pingInterval(...)` (WebSocket/HTTP-2) bzw. Server-Heartbeat zum Erkennen toter Verbindungen. `callTimeout` für solche Streams NICHT setzen (würde den Stream kappen).
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-ok-http-client/-builder/read-timeout.html

### 6.4 Secret-sicheres Logging
`offiziell`
- `Level.BODY/HEADERS` nur in Debug (`if (BuildConfig.DEBUG) BODY else NONE`); README warnt explizit vor Leak von „Authorization"/"Cookie"-Headern + Bodies in Logcat. Zusätzlich (Defense-in-Depth) `redactHeader("Authorization")`/`redactHeader("Cookie")`. Bodies lieber per Proxy (Charles/Chucker) inspizieren als ins Logcat schreiben.
- **Quelle:** https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor

### 6.5 Certificate-Pinning: nur bewusst, IMMER mit Backup-Pin
`offiziell`
- OkHttp warnt: „Certificate Pinning is Dangerous!" — nur mit Segen des TLS-Admins + Rotationsprozess. Format `sha256/<base64-SPKI>`; IMMER ≥ 1 Backup-Pin (sonst App-Ausfall bei Cert-Rotation); `**.domain.com` für Subdomains (ein `*` matcht nur genau ein Label); bevorzugt Intermediate pinnen (überlebt Leaf-Rotation). Setup: mit absichtlich falschem Pin starten, echte Hashes aus der `SSLPeerUnverifiedException` übernehmen (auf vertrauenswürdigem Netz).
- **Quelle:** https://square.github.io/okhttp/5.x/okhttp/okhttp3/-certificate-pinner/index.html

### 6.6 Deklaratives Pinning via `network_security_config.xml`; Cleartext aus; Debug-Overrides
`offiziell`
- Alternative ohne Code: `res/xml/network_security_config.xml` mit `<pin-set>` (Live- + Backup-`<pin>`, optional `expiration`) — gilt app-weit für alle Netz-Libs. `cleartextTrafficPermitted="false"` (ab Android 9 Default). Für lokale Server/Proxy `<debug-overrides>` (nur bei `android:debuggable`, von App-Stores abgelehnt) statt Pinning-Workarounds/All-Trust-TrustManager.
- **Quelle:** https://developer.android.com/privacy-and-security/security-config

---

## Bezug: Best-Practice-Abschnitt ↔ Bug-Abschnitt

> Wechselseitig mit [`bugs/android/retrofit-okhttp-moshi.md`](../../bugs/android/retrofit-okhttp-moshi.md) (dort die Spiegel-Tabelle).

| Best-Practice (hier) | Verwandter Bug-Abschnitt (Almanach) |
|----------------------|-------------------------------------|
| §1.1 ein OkHttpClient | L6 mehrere Clients, L7 Dispatcher-Defaults |
| §1.6 baseUrl/Endpoints | A9 baseUrl/führender Slash |
| §1.7 Converter/Moshi-Singleton | M1 Adapter fehlt, A2 Body+Encoding |
| §2.1/§2.2 codegen/KSP | M1 `@JsonClass` vergessen, M2 KSP-Migration, R5/R6 R8 |
| §2.3 Adapter-Reihenfolge | M4 Custom-Adapter greift nicht |
| §2.4 `@Json`/null-Safety | D1 Non-null-null, D2 Default-null, D4 `@Json`-Mismatch |
| §2.5 Polymorphie/Enums | M5 Polymorphie, D6 Enum-Fallback |
| §2.7 DTO↔Domain | D1/D2 (null aus dem UI fernhalten) |
| §3.1 suspend | S8 suspend/`Call<T>`/`withContext`-Mythos |
| §3.2 Response vs T | S2 unterschiedliches Fehlerverhalten |
| §3.4 Cancellation | S6 CancellationException geschluckt |
| §3.5 HttpException/IOException/errorBody | S1 suspend-Crash, S5 errorBody one-shot |
| §3.6 204→Unit | S3 KotlinNullPointerException |
| §3.9 CallAdapter | S4 Result-Wrapper crasht |
| §4.2/§4.3 Token/Authenticator | I1 Header nach Redirect, I2 401-Doppel-Durchlauf |
| §4.4 Logging-Position | I3/I5 Logging-Body/Position, SEC1 Secrets |
| §4.6 Retry+close | I6 proceed-once, I7 Retry-Leak |
| §4.7 peekBody | I4 Body konsumiert |
| §5.1 mitgelieferte Regeln | R1/R2/R3 Retrofit-R8, R9 ProGuard |
| §5.2/§5.3 codegen/Reflection-Keeps | R5 codegen-Adapter, R6 Reflection-Modelle, R7 Enums |
| §5.4 dontwarn | R8 Missing class TLS-Provider |
| §5.5/§5.6 Release-Test/mapping | R10 missing_rules.txt |
| §6.1/§6.2 callTimeout | L4 callTimeout 0, L5 read vs call |
| §6.3 SSE | L5 SSE readTimeout |
| §6.4 Logging-Secrets | SEC1 Logging leakt |
| §6.5/§6.6 Pinning | SEC2/SEC3/SEC4 Pinning-Fallen |
