# Retrofit + OkHttp + Moshi Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
