# Android-Networking — Retrofit + OkHttp + Moshi Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
