# Bekannte Bugs: Bild-/Video-Laden mit Coil 3 (`io.coil-kt.coil3`) in Jetpack Compose

> PFLICHT-LESEN vor Arbeit an Bild-/Medien-Laden mit Coil in BestJournalAndroid (`AsyncImage`, `ImageLoader`,
> `coil-video`). Stand: recherchiert am 2026-06-14 mit **7 Researchern parallel** (offizielle Quellen zuerst:
> coil-kt.github.io/coil, github.com/coil-kt/coil Issues + CHANGELOG + mitgelieferte shrinker-rules) +
> Fix-Status-Lauf (R8-`PlatformContext`-Regel/3.2.0 und aktuelle Version direkt verifiziert). ~60 Einträge in 8 Sektionen.
> **Versions-Anker (live aus BestJournalAndroid):** **coil 3.0.4** (`io.coil-kt.coil3`) mit `coil-compose` + `coil-video` ·
> Kotlin **2.1.0** · Jetpack Compose · AGP 8.x (R8 full mode). **WICHTIG:** `coil-network-okhttp` ist aktuell
> **NICHT** eingebunden (→ Netzwerk-Bilder laden nicht, siehe N1). Aktuellste Coil-Version: **3.5.0** (3.0.4 ist mehrere Minors zurück).
>
> **Abgrenzung (was steht woanders):** generelle Compose-Recomposition/State → [`jetpack-compose.md`](jetpack-compose.md).
> Reine Kotlin-/Coroutinen-Themen → [`kotlin.md`](kotlin.md). Allgemeine R8-Shrinker-Themen →
> [`../android-build/r8.md`](../android-build/r8.md). Hier geht es NUR um das Bild-/Medien-Laden mit Coil 3.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **M** Migration 2→3 · **N** Netzwerk-Laden · **AI** AsyncImage/Compose · **P** Preview ·
> **C** Cache · **OOM** Speicher/Bitmap · **D** Decoder (Video/SVG/GIF) · **R** R8/Setup/Singleton/Lifecycle.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Netzwerk-Bilder laden NICHT (kein Crash) | `io.coil-kt.coil3:coil-network-okhttp` ergänzen | N1 |
| 2 | `Unable to create a fetcher that supports: https://…` | Network-Artefakt fehlt → coil-network-okhttp | N1 |
| 3 | `Unresolved reference: coil` nach Upgrade | Imports `coil.*` → `coil3.*`; Koordinaten `io.coil-kt.coil3` | M1 |
| 4 | `Coil.setImageLoader`/`ImageLoaderFactory` weg | `SingletonImageLoader.Factory` / `setSafe { }` | M3 |
| 5 | Override `newImageLoader(Context)` greift nicht | Signatur `PlatformContext` | M4 |
| 6 | `Drawable`-Typ-Mismatch / `asCoilImage` weg | `coil3.Image`; `asImage()`/`asDrawable(resources)` | M5 |
| 7 | `painter.state` `when(...)` bricht | `state` ist `StateFlow` → `collectAsState()` | M6 |
| 8 | `componentRegistry`/`bitmapConfig` weg | `components { }`; `allowHardware`/`maxBitmapSize` | M8 |
| 9 | Default-placeholder/error fehlt in AsyncImage | placeholder/error/fallback am Composable setzen | M7 |
| 10 | `NoSuchMethodError ... NetworkFetcher$Factory` | alpha-coil3-Artefakt im Baum → alle auf 3.0.4 (BOM/force) | N2 |
| 11 | ktor: `NoClassDefFoundError: HttpTimeout` | Engine zur ktor2/ktor3-Variante passend; Android → okhttp | N3 |
| 12 | Netzwerk lädt nur im Debug, nicht Release | ServiceLoader-Strip → Keep-Regeln / Fetcher explizit `add()` | R3 |
| 13 | HTTP-URL lädt nicht / „Cleartext not permitted" | `INTERNET`-Permission; network_security_config | N6 |
| 14 | `AsyncImage` flackert/Reload beim Scrollen | `ImageRequest` per `remember(url)`; Callbacks via AsyncImage-Parameter | AI1 |
| 15 | Flicker nach Upgrade auf 3.1.0 | `ImageRequest.size(...)` explizit (3.0.4 selbst nicht betroffen) | AI2 |
| 16 | crossfade fehlt bei Cache-Hit | by-design; eigene Transition via `painter.state` | AI3 |
| 17 | Custom-Fetcher → kein Memory-Cache/Flackern | passenden `Keyer` registrieren | AI4 |
| 18 | `SubcomposeAsyncImage` ruckelt in LazyList | `AsyncImage`/`rememberAsyncImagePainter` statt Subcompose | AI5 |
| 19 | Bild riesig/0px | `Modifier.size(...)` + `contentScale` (nicht `None`) | AI6 |
| 20 | Preview zeigt nichts | `LocalAsyncImagePreviewHandler` + `AsyncImagePreviewHandler` | P1 |
| 21 | Preview hängt in Loading (rememberAsyncImagePainter) | im `LocalInspectionMode` direkt `Image(painter)` | P2 |
| 22 | mehrere ImageLoader → kein Cache-Sharing | EIN Singleton (`SingletonImageLoader.setSafe`) | C1 |
| 23 | `IllegalStateException` zwei DiskCaches gleiches Dir | geteilte DiskCache-Instanz ODER getrennte Verzeichnisse | C2 |
| 24 | `OutOfMemoryError` große Bilder | Zielgröße: `AsyncImage`+Constraints bzw. `.size(...)` | OOM1 |
| 25 | `Software rendering doesn't support hardware bitmaps` | `allowHardware(false)` (lokal, bei Pixel-Zugriff) | OOM2 |
| 26 | mehr GC/OOM seit Migration | BitmapPool entfernt → Zielgröße + Cache-Tuning + RGB_565 | OOM3 |
| 27 | Video lädt nicht (lokal/Netz) | `add(VideoFrameDecoder.Factory())` MANUELL registrieren | D1 |
| 28 | Video aus URL lädt nicht | Network-Fetcher UND VideoFrameDecoder zusammen | D3 |
| 29 | GIF zeigt nur 1. Frame | `coil-gif`; ≥API28 `AnimatedImageDecoder` sonst `GifDecoder` | D5 |
| 30 | Custom `components { }` → SVG/GIF tot | alle Decoder explizit registrieren | D6 |
| 31 | SVG verzerrt/falsche Größe | `SvgDecoder.Factory(useViewBoundsAsIntrinsicSize=false)` | D9 |
| 32 | Release-Build R8 `Missing class coil3.PlatformContext` | `-dontwarn coil3.PlatformContext` (in 3.0.4 selbst setzen, Lib erst ab 3.2.0) | R1 |
| 33 | `onSuccess`/`onError` feuern nie | meist Cancellation (Screen verlassen) — Network/Logger prüfen | R5 |

---

## M) Migration Coil 2 → Coil 3

> Fast alles hier ist **by-design** (gewollte Breaking Changes der KMP-Umstellung), kein Library-Bug.
> FIXes sind funktionserhaltend — nur die API-Verdrahtung ändert sich, alle Features bleiben.

### M1. Alle Imports brechen: Package `coil.*` → `coil3.*`, Koordinaten `io.coil-kt.coil3` ⭐ HAEUFIG
- **Symptom:** Nach dem Versions-Bump kompiliert nichts; hunderte `Unresolved reference: coil`; Gradle findet Artefakte nicht.
- **Ursache:** Group-ID `io.coil-kt` → **`io.coil-kt.coil3`**, Package `coil` → **`coil3`**, `coil-base` → `coil-core`, `coil-compose-base` → `coil-compose-core`.
- **Versionen:** jeder 2.x→3.x-Wechsel (by-design).
- **FIX:** `implementation("io.coil-kt.coil3:coil-compose:3.0.4")`; global `import coil.` → `import coil3.` (z.B. `coil3.compose.AsyncImage`, `coil3.request.ImageRequest`, `coil3.ImageLoader`).
- **Quelle:** https://coil-kt.github.io/coil/upgrading_to_coil3/

### M3. `Coil.setImageLoader`/`ImageLoaderFactory` existieren nicht mehr
- **Symptom:** `Unresolved reference: Coil`; eigene `Application : ImageLoaderFactory` überschreibt nichts.
- **Ursache:** `Coil`-Objekt → **`SingletonImageLoader`**; `ImageLoaderFactory` → **`SingletonImageLoader.Factory`**.
- **Versionen:** 3.x (by-design).
- **FIX:** eine Variante wählen — `class App : Application(), SingletonImageLoader.Factory { override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(this).build() }` ODER `SingletonImageLoader.setSafe { ImageLoader.Builder(it).build() }`. `setSafe` (nicht `setUnsafe`).
- **Quelle:** https://coil-kt.github.io/coil/getting_started/

### M4. `Context` → `PlatformContext`: Signaturen passen nicht
- **Symptom:** Override-Fehler bei `newImageLoader(context: Context)`; `ImageRequest.Builder(context)` Typ-Mismatch.
- **Ursache:** KMP-Umbau: `android.content.Context` → **`PlatformContext`** (auf Android ein Type-Alias auf `Context`; Werte kompatibel, Override-Typen müssen `PlatformContext` heißen).
- **Versionen:** 3.x (by-design).
- **FIX:** Override-Signatur `PlatformContext`; in Compose `LocalPlatformContext.current` (statt `LocalContext.current`).
- **Quelle:** https://coil-kt.github.io/coil/upgrading_to_coil3/

### M5. `Drawable` ist nicht mehr der Ergebnistyp — `coil3.Image`
- **Symptom:** Custom `Target`/`Transition`/Decoder mit `result.drawable: Drawable` haben Type-Mismatch; `asCoilImage()` unresolved.
- **Ursache:** Zentraler Render-/Ergebnistyp ist **`coil3.Image`** (SDK-entkoppelt); Helfer `asCoilImage()` → `asImage()`.
- **Versionen:** 3.x (by-design).
- **FIX:** `image.asDrawable(context.resources)`, `drawable.asImage()`, `image.toBitmap()`/`bitmap.asImage()`, in Compose `image.asPainter()`. (Painter → Image geht nicht.)
- **Quelle:** https://coil-kt.github.io/coil/getting_started/

### M6. `AsyncImagePainter.state` ist jetzt `StateFlow`
- **Symptom:** `when (painter.state) { … }`/`painter.state.painter` Type-Mismatch; status-getriebene UI reagiert nicht.
- **Ursache:** `state` war `State`, ist jetzt **`StateFlow<AsyncImagePainter.State>`**.
- **Versionen:** 3.x (by-design).
- **FIX:** `val state by painter.state.collectAsState(); when (state) { is …Loading -> … }`. (Zusätzlich: Default-`SizeResolver` nutzt `Size.ORIGINAL`, wartet nicht mehr auf `onDraw`; `modelEqualityDelegate` über `LocalAsyncImageModelEqualityDelegate`.)
- **Quelle:** https://coil-kt.github.io/coil/upgrading_to_coil3/

### M7. `AsyncImage` zeigt keine global gesetzten Default-placeholder/error/fallback mehr
- **Symptom:** am Loader/per Default gesetzte Platzhalter erscheinen in `AsyncImage` nicht.
- **Ursache:** `AsyncImage` zieht die AM COMPOSABLE übergebenen Painter; globale Defaults greifen im Compose-Pfad nicht (by-design, #2119).
- **Versionen:** 3.0.x (by-design).
- **FIX:** `placeholder=`, `error=`, `fallback=` direkt am `AsyncImage` setzen (für Views/`ImageRequest` weiter `placeholder(@DrawableRes Int)`). `fallback` greift bei `data == null`; sonst wird `error` genommen.
- **Quelle:** https://github.com/coil-kt/coil/issues/2119

### M8. `componentRegistry`→`components`; `bitmapConfig`/Bitmap-Pool-Optionen entfernt
- **Symptom:** `componentRegistry { }` unresolved; `.bitmapConfig(...)`/`.bitmapPoolPercentage(...)` weg.
- **Ursache:** `componentRegistry` → **`components`**; Bitmap-Pool komplett entfernt; `Parameters`-API → **`Extras`**.
- **Versionen:** 3.x (by-design).
- **FIX:** `ImageLoader.Builder(ctx).components { add(...) }`; Hardware-Bitmaps via `allowHardware(false)`; viele `ImageRequest.Builder`-Funktionen sind jetzt Extensions (`import coil3.request.crossfade`). Zusatz-Defaults: Ausgabe ≤ 4096×4096 (`maxBitmapSize`), `android.resource://…/drawable/name`-URIs nicht mehr unterstützt, `CoilUtils` entfernt.
- **Quelle:** https://coil-kt.github.io/coil/upgrading_to_coil3/ · https://github.com/coil-kt/coil/issues/2221

---

## N) Netzwerk-Laden (FOKUS)

### N1. Coil 3 hat KEINEN Netzwerk-Fetcher per Default — HTTP/HTTPS lädt nicht ⭐ HAEUFIG
- **Symptom:** `AsyncImage(model="https://…")` zeigt nichts, KEIN Crash (oft kein `onError`). Lokale Bilder gehen. Mit `DebugLogger`: `IllegalStateException: Unable to create a fetcher that supports: https://…`. **Genau der Projekt-Fall: BestJournalAndroid hat coil-compose+coil-video, aber NICHT coil-network-okhttp.**
- **Ursache:** `coil-core` enthält absichtlich keinen Netzwerk-Fetcher mehr (KMP/optionale Netzwerk-Lib).
- **Versionen:** alle 3.x (by-design).
- **FIX:** genau EIN Netzwerk-Artefakt ergänzen:
  ```kotlin
  implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")  // Android/JVM, registriert sich per ServiceLoader
  // KMP-Alternative: coil-network-ktor3 + pro Plattform eine Ktor-Engine
  ```
- **Quelle:** https://coil-kt.github.io/coil/network/ · https://github.com/coil-kt/coil/issues/2634

### N2. Alpha-coil3-Artefakt im Dependency-Baum → `NoSuchMethodError ... NetworkFetcher$Factory`
- **Symptom:** Netzwerkbilder laden nicht; `NoSuchMethodError: No direct method <init>(…) in class Lcoil3/network/NetworkFetcher$Factory;`.
- **Ursache:** eine transitive Lib zieht ein **alpha**-Network-Artefakt (binär inkompatibel), das per Resolution Vorrang bekommt.
- **Versionen:** wenn ein Alpha mitgezogen wird; stabile 3.x sind binär-kompatibel.
- **FIX:** `./gradlew :app:dependencies | grep coil`; alle `coil3`-Artefakte via BOM (`platform("io.coil-kt.coil3:coil-bom:3.0.4")`) oder `resolutionStrategy.force(...)` auf 3.0.4 vereinheitlichen.
- **Quelle:** https://github.com/coil-kt/coil/issues/2832

### N3. `coil-network-ktor3` mit falscher/fehlender Engine → `NoClassDefFoundError: HttpTimeout`
- **Symptom:** `NoClassDefFoundError: io/ktor/client/plugins/HttpTimeout`; oder gar keine Requests.
- **Ursache:** Versions-Mismatch: `coil-network-ktor3` braucht Ktor 3; mit Ktor-2-Engine fehlen Klassen. Ktor braucht pro Plattform eine Engine.
- **Versionen:** bei Engine-Mismatch.
- **FIX:** Engine zur Variante passend (`coil-network-ktor3` → Ktor-3-Engine je Plattform; sonst `coil-network-ktor2`). Für reines Android `coil-network-okhttp` (keine Engine-Wahl).
- **Quelle:** https://github.com/coil-kt/coil/issues/2409 · https://coil-kt.github.io/coil/network/

### N6. Fehlende `INTERNET`-Permission / Cleartext-HTTP blockiert
- **Symptom:** Network-Artefakt da, aber Bilder laden nicht; bei `http://` Log `Cleartext HTTP traffic to <host> not permitted`.
- **Ursache:** `android.permission.INTERNET` fehlt; Android 9+ blockiert Cleartext-HTTP per Default.
- **Versionen:** Plattform (Android 9+).
- **FIX:** `<uses-permission android:name="android.permission.INTERNET"/>`; HTTPS bevorzugen; für nötige HTTP-Hosts `network_security_config.xml` mit `cleartextTrafficPermitted` gezielt.
- **Quelle:** https://coil-kt.github.io/coil/network/

### N7. Cache-Control-Header werden per Default IGNORIERT → veraltete Bilder
- **Symptom:** Server liefert `no-store`/kurze TTL, Coil 3 zeigt trotzdem das alte Bild.
- **Ursache:** Coil 3 respektiert `Cache-Control` absichtlich nicht mehr und cached immer auf Disk.
- **Versionen:** alle 3.x (by-design).
- **FIX:** `io.coil-kt.coil3:coil-network-cache-control` + `OkHttpNetworkFetcherFactory(cacheStrategy = { CacheControlCacheStrategy() })` (braucht coreLibraryDesugaring ≤ API 25). Alternativ am Loader `respectCacheHeaders(true)`.
- **Quelle:** https://coil-kt.github.io/coil/network/

### N8. KMP: falsches Network-Artefakt pro Plattform
- **Symptom:** Bilder laden auf einer Plattform (iOS/Desktop/WASM) nicht, Android geht.
- **Ursache:** `coil-network-okhttp` ist nur Android/JVM; echtes KMP braucht `coil-network-ktor2/3` + Engine pro Plattform.
- **Versionen:** 3.x KMP.
- **FIX:** pro SourceSet passende Ktor-Engine; reines Android → okhttp.
- **Quelle:** https://coil-kt.github.io/coil/network/

---

## AI) AsyncImage & Compose

> Abgrenzung: generelle Compose-Recomposition steht in [`jetpack-compose.md`](jetpack-compose.md) — hier nur der Coil-Bezug.

### AI1. Instabiler `ImageRequest` (Lambda/`listener`) → Dauer-Reload/Flackern beim Scrollen ⭐ HAEUFIG
- **Symptom:** In `LazyColumn`/`LazyGrid` rekomponiert `AsyncImage` bei jedem Scroll-Tick und lädt neu (Placeholder-Flash), obwohl im Memory-Cache.
- **Ursache:** Ein frisch gebauter `ImageRequest` mit `.listener { }`-Lambda erzeugt pro Recomposition eine neue Instanz mit neuem Lambda → `equals()==false` → `model` gilt als geändert → neuer Request. Verschärft: `AsyncImage` ist unskippable (`model: Any?` instabil).
- **Versionen:** strukturell ab 2.5; in 3.0.4 relevant bei Fehlbedienung.
- **FIX:** `ImageRequest` per `remember(url)` cachen; Callbacks NICHT in den Request, sondern über `AsyncImage`-Parameter `onSuccess`/`onError`/`onLoading` (gehen nicht in die `model`-Equality). Optional `LocalAsyncImageModelEqualityDelegate`.
- **Quelle:** https://github.com/coil-kt/coil/issues/1940

### AI2. Flicker in `LazyVerticalGrid` nach Upgrade auf 3.1.0 (Size-Resolution-Regression)
- **Symptom:** Nach 3.0.x→3.1.0 flackern Bilder bei Recomposition (clickable/Theme/Click). Downgrade auf 3.0.0 behebt es.
- **Ursache:** Ab 3.1.0 wartet `AsyncImagePainter` nicht mehr auf den ersten `onDraw`; ohne explizite Größe schwankt die aufgelöste Größe → Request gilt als geändert → Reload.
- **Versionen:** Regression **ab 3.1.0**, bis ≥ 3.2.0-rc02. **In 3.0.4 NOCH NICHT betroffen** — beim Upgrade auf 3.1+ aber `ImageRequest.size(...)` explizit setzen.
- **FIX:** `ImageRequest.Builder(ctx).data(url).size(Size(w, h)).build()` ODER `rememberConstraintsSizeResolver()` + `Modifier`.
- **Quelle:** https://github.com/coil-kt/coil/issues/2936

### AI3. Crossfade läuft nicht bei Memory-Cache-Hit
- **Symptom:** Trotz `crossfade(true)` kein Übergang, wenn das Bild aus dem Memory-Cache kommt (nur beim ersten echten Laden).
- **Ursache:** by-design: Animation wird bei `DataSource.MEMORY_CACHE`/gecachtem Placeholder übersprungen (Performance); `CrossfadePainter` animiert nur einmal.
- **Versionen:** durchgehend 2.x/3.x (by-design).
- **FIX:** eigenen Crossfade über `painter.state` steuern (`rememberAsyncImagePainter` + `collectAsState`), z.B. Alpha-Transition bei `State.Success`.
- **Quelle:** https://coil-kt.github.io/coil/compose/

### AI4. Custom `Fetcher` ohne `Keyer` → kein Memory-Cache → Flackern
- **Symptom:** Eigener `Fetcher` (z.B. Bild aus DB/eigenem Typ als `model`) → bei jeder Recomposition neu gefetcht statt aus Memory-Cache → Flackern.
- **Ursache:** In Coil 3 ist `key` nicht mehr Teil von `Fetcher`. Ohne passenden `Keyer` für den `model`-Typ entsteht kein Memory-Cache-Key.
- **Versionen:** 3.x (Verhalten von 2.x).
- **FIX:** eigenen `Keyer<MyType>` registrieren (`components { add(MyFetcher.Factory()); add(MyKeyer()) }`).
- **Quelle:** https://github.com/coil-kt/coil/issues/2551

### AI5. `SubcomposeAsyncImage` als Performance-Falle in LazyList
- **Symptom:** Ruckeln/Lag beim Scrollen mit `SubcomposeAsyncImage` für Loading/Error-Slots.
- **Ursache:** Subcomposition pro Item ist teuer; offizielle Doku warnt vor Einsatz in performancekritischer UI.
- **Versionen:** alle 3.x (Architektur).
- **FIX:** `rememberAsyncImagePainter` + `painter.state` und Loading/Error selbst rendern (ohne Subcomposition).
- **Quelle:** https://coil-kt.github.io/coil/compose/

### AI6. Fehlendes `Modifier.size`/`contentScale` → Bild riesig oder 0px ⭐ HAEUFIG
- **Symptom:** Bild sprengt das Layout, bleibt 0px, oder in unbegrenztem Container wird Vollauflösung geladen.
- **Ursache:** `ConstraintsSizeResolver` greift nur bei `ContentScale != None`; bei `None` → `Size.ORIGINAL`. In unbegrenzten Layouts fehlt die Begrenzung.
- **Versionen:** 3.x (dokumentiert).
- **FIX:** `AsyncImage(..., contentScale = ContentScale.Crop, modifier = Modifier.size(96.dp))`. Bei nur zur Draw-Zeit bekannter Größe `rememberDrawScopeSizeResolver()` (experimental).
- **Quelle:** https://coil-kt.github.io/coil/compose/

### AI7. `placeholderMemoryCacheKey`: Placeholder behält Quell-Größe → UI-Sprung, kein Crossfade
- **Symptom:** List→Detail mit `placeholderMemoryCacheKey`: kleines Listenbild wird im großen Container nicht hochskaliert, UI „springt" beim Hi-Res-Eintreffen.
- **Ursache:** `AsyncImage` skaliert Placeholder nicht getrennt vom Success-Bild (`contentScale` gilt einheitlich); cached Placeholder behält Original-Pixelgröße.
- **Versionen:** seit 2.0, in 3.x unverändert.
- **FIX:** `ContentScale.Fit`; oder Placeholder aus `imageLoader.memoryCache?.get(MemoryCache.Key(key))?.image` separat voll skaliert über dem `AsyncImage` rendern (mit `AnimatedVisibility`-Fade).
- **Quelle:** https://github.com/coil-kt/coil/discussions/1152

### AI8. `rememberAsyncImagePainter`: `state` erstes Frame immer `Empty` + lädt Originalgröße
- **Symptom:** State-abhängige UI „blitzt" beim ersten Frame falsch; Bild wird in voller Auflösung geladen.
- **Ursache:** `rememberAsyncImagePainter` ermittelt die On-Screen-Größe NICHT automatisch (`Size.ORIGINAL`); State ist erst nach erster Komposition aktuell.
- **Versionen:** 3.x (by-design).
- **FIX:** `rememberConstraintsSizeResolver()` an `ImageRequest.size(...)` + `Modifier` koppeln (wie `AsyncImage` intern). Falls `Empty`-Frame inakzeptabel: `SubcomposeAsyncImage` (aber nicht in heißer Scroll-UI, AI5).
- **Quelle:** https://coil-kt.github.io/coil/compose/

---

## P) Compose-Preview

### P1. `AsyncImage` mit Netz-URL bleibt im `@Preview` leer ⭐ HAEUFIG
- **Symptom:** Im Android-Studio-Preview erscheint nichts (zur Laufzeit lädt das Bild korrekt).
- **Ursache:** by-design seit 3.0.0: der Request wird AUCH im Preview ausgeführt, aber im Inspection-Environment ist Netzzugriff deaktiviert → Netz-URLs scheitern immer.
- **Versionen:** 3.0.x (by-design; Override-API seit 3.0.0).
- **FIX:** Preview-Bild via `AsyncImagePreviewHandler` liefern:
  ```kotlin
  val ph = AsyncImagePreviewHandler { ColorImage(Color.Gray.toArgb()) }
  CompositionLocalProvider(LocalAsyncImagePreviewHandler provides ph) { AsyncImage(...) }
  ```
- **Quelle:** https://coil-kt.github.io/coil/compose/ (Previews)

### P2. `rememberAsyncImagePainter` hängt im Preview in `Loading` (auch bei mehreren `@Preview`)
- **Symptom:** Trotz PreviewHandler bleibt der Painter bei manueller `state`-Beobachtung im `Loading`; bei mehreren `@Preview`-Annotationen rendern nicht alle.
- **Ursache:** im Preview-Environment erreicht der über `state` gesteuerte Branch nicht zuverlässig `Success`; zusätzlich erstes Frame immer `Empty` (AI8).
- **Versionen:** gemeldet 3.1.0/3.2.0, gleicher Mechanismus in 3.0.4 (Workaround-Thema).
- **FIX:** im Preview-Pfad direkt zeichnen: `if (LocalInspectionMode.current) Image(painter, null) else when(state) { … }`. Wo möglich `AsyncImage` statt `rememberAsyncImagePainter` (PreviewHandler greift dort direkt).
- **Quelle:** https://github.com/coil-kt/coil/issues/2859 · https://github.com/coil-kt/coil/issues/3001

### P3. `placeholder` als einfachster Preview-Weg; KMP `Res.drawable` als `model` nicht unterstützt
- **Symptom:** im Preview wird nur der `placeholder` gerendert, nie das `model`; in KMP lädt `Res.drawable.x` als `model` nicht.
- **Ursache:** im Preview wird `model` (ggf. Netz) bewusst nicht geladen — nur der `placeholder` (Resource); `Res.drawable.x` wird nicht als `model` unterstützt.
- **Versionen:** by-design (3.x).
- **FIX:** `placeholder` nur im Inspection-Mode setzen (`if (LocalInspectionMode.current) painterResource(...) else null`); KMP: `Res.getUri("drawable/x")` statt `Res.drawable.x`.
- **Quelle:** https://github.com/coil-kt/coil/discussions/1397 · https://github.com/coil-kt/coil/issues/2812

---

## C) Cache (Memory & Disk)

### C1. Mehrere `ImageLoader`-Instanzen → doppelter Speicher, kein Cache-Sharing ⭐ HAEUFIG
- **Symptom:** hoher RAM/Disk-Verbrauch, schlechte Trefferquote; ein Bild ist in einem Screen geladen, im anderen nicht.
- **Ursache:** Jeder `ImageLoader` hat eigenen MemoryCache/DiskCache/OkHttpClient. Pro Screen ein neuer Loader teilt nichts.
- **Versionen:** alle 3.x (by-design).
- **FIX:** EINEN Singleton nutzen (`SingletonImageLoader.setSafe { ImageLoader.Builder(ctx).build() }` bzw. `SingletonImageLoader.Factory`). Varianten mit gleicher Cache über `imageLoader.newBuilder()`.
- **Quelle:** https://coil-kt.github.io/coil/image_loaders/

### C2. Zwei DiskCaches auf demselben Verzeichnis → `IllegalStateException`/Korruption
- **Symptom:** Crash beim Erzeugen des zweiten DiskCache mit gleichem `directory(...)` („directory already in use").
- **Ursache:** Coil teilt by-default EINE DiskCache-Instanz; zwei aktive Caches im selben Ordner sind verboten.
- **Versionen:** by-design (3.x).
- **FIX:** dieselbe DiskCache-Instanz an beide Loader (bzw. Singleton); ODER bewusst getrennte Verzeichnisse (`cacheDir.resolve("image_cache")` vs `"important_image_cache")`).
- **Quelle:** https://github.com/coil-kt/coil/discussions/2737

### C3. DiskCache crasht/leert nach 2→3-Upgrade (`IllegalArgumentException: Unexpected header: 10`)
- **Symptom:** nach Migration `onError` mit `Unexpected header: 10`; alte Cache-Dateien laden nicht.
- **Ursache:** DiskCache-Eintragsformat zwischen Coil 2 und 3 geändert (alte Dateien inkompatibel).
- **Versionen:** ab 3.0.0-alpha01; Auto-Clear via PR #1999 → **in 3.0.4 enthalten** (kein manuelles Eingreifen nötig).
- **FIX:** in 3.0.4 erledigt; Sonderfall: `imageLoader.diskCache?.clear()`.
- **Quelle:** https://github.com/coil-kt/coil/issues/1998

### C4. Kein Memory-Cache-Hit, weil Größe in den Cache-Key eingeht
- **Symptom:** dieselbe URL in unterschiedlich großen Composables → kein Hit, erneutes Dekodieren.
- **Ursache:** Memory-Cache-Key enthält die aufgelöste Zielgröße (gut für RAM). `Size.ORIGINAL`-Default und Extras-statt-Parameters seit 3.0.0.
- **Versionen:** by-design (3.x).
- **FIX:** wenn dasselbe Bitmap über Größen geteilt werden soll: `ImageRequest.Builder(ctx).data(url).memoryCacheKey(url).diskCacheKey(url).build()` (bewusst — verliert größenoptimierte Speicherung).
- **Quelle:** https://coil-kt.github.io/coil/image_loaders/

### C5. Cache-Größe richtig konfigurieren
- **Symptom:** Cache zu klein (häufige Re-Decodes) oder zu groß (OOM).
- **Ursache:** Fehlkonfiguration der Builder-APIs.
- **Versionen:** API gilt 3.x.
- **FIX:** `MemoryCache.Builder().maxSizePercent(context, 0.25)` (Context!); `DiskCache.Builder().directory(...).maxSizePercent(0.02)`/`maxSizeBytes(...)`.
- **Quelle:** https://coil-kt.github.io/coil/image_loaders/

### C6. DiskCache wird trotz DISABLED/Custom-Dir angesprochen → NPE auf `cacheDir`
- **Symptom:** bei `diskCachePolicy(CachePolicy.DISABLED)` und/oder Custom-Dir NPE (z.B. in Screenshot-Tests, `cacheDir` null) — file-basierte Decoder (`VideoFrameDecoder`) brauchen eine Temp-Datei.
- **Ursache:** Coil fällt für Temp-Dateien auf `context.cacheDir` zurück.
- **Versionen:** gemildert durch lazy `cacheDir` in 3.x; Reststolperstein bei file-Decodern in 3.0.4.
- **FIX:** `diskCachePolicy(CachePolicy.ENABLED)` statt DISABLED; in Tests gültiges (Fake-)FileSystem/Verzeichnis.
- **Quelle:** https://github.com/coil-kt/coil/issues/1754

---

## OOM) Speicher & Bitmap

### OOM1. OOM durch Dekodieren in Originalauflösung (`Size.ORIGINAL`) ⭐ HAEUFIG
- **Symptom:** `OutOfMemoryError: Failed to allocate … byte allocation` bei großen Bildern (8000×6000 ≈ 183 MB ARGB_8888), obwohl on-screen klein.
- **Ursache:** ohne auflösbare Zielgröße fällt Coil auf `Size.ORIGINAL` zurück. `rememberAsyncImagePainter` ermittelt keine On-Screen-Größe; `AsyncImagePainter` wartet in 3.x nicht mehr auf `onDraw`.
- **Versionen:** by-design 3.x (verschärft ggü. 2.x).
- **FIX:** `AsyncImage` mit Constraints + `ContentScale` bevorzugen; bei `rememberAsyncImagePainter` `rememberConstraintsSizeResolver()` + `.size(...)`; sonst `.size(1080,1080).precision(Precision.INEXACT)`.
- **Quelle:** https://coil-kt.github.io/coil/compose/

### OOM2. `IllegalArgumentException: Software rendering doesn't support hardware bitmaps` ⭐ HAEUFIG
- **Symptom:** Crash beim Pixel-Zugriff (Palette, `getPixels`, `drawToBitmap`, `PixelCopy`, Software-Canvas, Shared-Element-Transition).
- **Ursache:** `allowHardware` Default `true` → API ≥ 26 `Config.HARDWARE` (Pixel nur im GPU-Speicher, für CPU nicht lesbar).
- **Versionen:** by-design 2.x/3.x.
- **FIX:** lokal `allowHardware(false)` an betroffenen Requests (bei Shared-Element an BEIDEN Views); nicht global, damit andere Bilder GPU-effizient bleiben.
- **Quelle:** https://coil-kt.github.io/coil/recipes/

### OOM3. BitmapPool in Coil 3 entfernt → mehr GC-Druck/Spitzen
- **Symptom:** nach 2→3-Migration in bildintensiven Listen mehr GC/Speicherspitzen, ggf. OOM wo Coil 2 mit Pool noch lief.
- **Ursache:** `BitmapPool` (+ `inBitmap`-Reuse) in Coil 3 vollständig entfernt; immer frische immutable Bitmaps.
- **Versionen:** ab 3.0.0 (Architektur, kein Rückbau).
- **FIX:** Zielgrößen setzen (OOM1, größter Hebel), Memory-Cache dimensionieren (C5), `bitmapConfig(RGB_565)` für deckende Fotos.
- **Quelle:** https://coil-kt.github.io/coil/changelog/ · https://github.com/coil-kt/coil/discussions/1186

### OOM4. `bitmapConfig(RGB_565)` wirkt nicht (Alpha-Bug, in 3.0.4 gefixt)
- **Symptom:** global gesetztes `RGB_565` wird ignoriert, Bilder bleiben ARGB_8888 (doppelter Speicher).
- **Ursache:** Bug im `EngineInterceptor.resolveExtras` (las `request.extras` statt Defaults).
- **Versionen:** betroffen `3.0.0-alpha06`; **in 3.0.4 gefixt**.
- **FIX:** 3.0.4 enthält den Fix; zuverlässig per Request `ImageRequest.Builder(ctx).data(url).bitmapConfig(Bitmap.Config.RGB_565).build()` (nur deckende Fotos, kein Alpha; nicht mit `transformations`).
- **Quelle:** https://github.com/coil-kt/coil/issues/2221

### OOM5. Große GIFs/animierte Bilder
- **Symptom:** OOM/hoher Verbrauch bei großen/langen GIFs, mehrere gleichzeitig.
- **Ursache:** animierte Formate halten viele Frames (je eine volle Bitmap) gleichzeitig.
- **Versionen:** by-design (coil-gif).
- **FIX:** Zielgröße erzwingen; ≥ API 28 `AnimatedImageDecoder` (effizienter, auch animiertes WebP); ggf. animiertes WebP statt GIF.
- **Quelle:** https://coil-kt.github.io/coil/gifs/

### OOM6. `transformations` auf großen Bildern + `Precision.EXACT`
- **Symptom:** Speicherspitzen/OOM bei Blur/RoundedCorners auf großen Bildern.
- **Ursache:** Transformationen laufen auf Software-Bitmap + erzeugen eine zweite Bitmap; `transformations` deaktiviert die RGB_565-Optimierung.
- **Versionen:** by-design 3.x.
- **FIX:** vor der Transformation Zielgröße setzen + `precision(Precision.INEXACT)`.
- **Quelle:** https://coil-kt.github.io/coil/image_requests/

### OOM7. `maxBitmapSize`-Default (4096×4096) — Schutz, nicht deaktivieren
- **Symptom:** sehr große Vorlagen werden auf 4096² begrenzt; oder jemand setzt `maxBitmapSize(Size.ORIGINAL)` und holt OOM zurück.
- **Ursache:** Coil 3 erzwingt neu < 4096×4096 als OOM-Schutz (gab es in 2.x nicht).
- **Versionen:** ab 3.0.0 (Feature).
- **FIX:** Default belassen; bei echtem Bedarf maßvoll anheben (`maxBitmapSize(Size(8192,8192))`), nicht `Size.ORIGINAL`.
- **Quelle:** https://coil-kt.github.io/coil/upgrading_to_coil3/

---

## D) Decoder (Video / SVG / GIF)

> Relevant, weil BestJournalAndroid `coil-video` einbindet. Fast alles ist Setup/by-design.

### D1. `VideoFrameDecoder` muss MANUELL registriert werden (wird NICHT automatisch erkannt) ⭐ HAEUFIG
- **Symptom:** `AsyncImage` mit Video-URI zeigt nichts/Error; GIF/SVG gehen nach reinem Dependency-Einbinden „magisch", Video nicht.
- **Ursache:** Anders als coil-gif/coil-svg (Auto-Erkennung über Header) registriert sich `VideoFrameDecoder` NICHT selbst — ohne Eintrag kein Video-Decoder.
- **Versionen:** alle 3.x (by-design). **Projekt-relevant: coil-video ist eingebunden, aber ohne Registrierung lädt kein Video.**
- **FIX:** `ImageLoader.Builder(ctx).components { add(VideoFrameDecoder.Factory()) }.build()` (Import `coil3.video.VideoFrameDecoder`); auf dem Singleton-Pfad via `SingletonImageLoader.setSafe { }`. Frame wählen mit `videoFrameMillis(...)`/`videoFramePercent(...)`.
- **Quelle:** https://coil-kt.github.io/coil/videos/

### D2. Falsches Artefakt `io.coil-kt:coil-video:2.x` statt `io.coil-kt.coil3:coil-video`
- **Symptom:** nur `import coil.decode.VideoFrameDecoder` (Coil 2) auflösbar, inkompatibel mit Coil-3-`ImageLoader`; `coil3.video.VideoFrameDecoder` fehlt.
- **Ursache:** aus alten Snippets das Coil-2-Artefakt eingebunden.
- **Versionen:** Setup-Fehler.
- **FIX:** `implementation("io.coil-kt.coil3:coil-video:3.0.4")`, Import `coil3.video.VideoFrameDecoder`; alle coil3-Artefakte gleiche Version.
- **Quelle:** https://github.com/coil-kt/coil/discussions/2482

### D3. Video aus dem Netz braucht ZWEI Komponenten: Network-Fetcher + VideoFrameDecoder ⭐ HAEUFIG
- **Symptom:** lokales Video zeigt Frames, Video von URL lädt nicht.
- **Ursache:** Coil 3 hat keinen Netzwerk-Support default. URL-Video = Network-Fetcher (Bytes) + VideoFrameDecoder (Frame).
- **Versionen:** alle 3.x (by-design).
- **FIX:** `coil-network-okhttp` + `coil-video`; `components { add(OkHttpNetworkFetcherFactory()); add(VideoFrameDecoder.Factory()) }`.
- **Quelle:** https://coil-kt.github.io/coil/videos/ · https://coil-kt.github.io/coil/network/

### D4. Video OHNE Datei-Extension → `BitmapFactory returned a null bitmap`
- **Symptom:** Video-Datei ohne Endung (Cache-Datei/Content-URI) liefert null-Bitmap; mit Extension geht es.
- **Ursache:** Erkennung hängt am MIME aus der Endung; ohne MIME lehnt `VideoFrameDecoder.Factory.isApplicable` ab → Fallback auf Bitmap-Decoder.
- **Versionen:** seit 2.0, in 3.0.4 unverändert (by-design).
- **FIX:** Decoder erzwingen: `decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }`.
- **Quelle:** https://github.com/coil-kt/coil/issues/1510

### D5. GIF zeigt nur 1. Frame / animiert nicht (falscher/fehlender Decoder je API-Level) ⭐ HAEUFIG
- **Symptom:** GIF statisch (nur erstes Frame), oder animiert nur auf bestimmten Geräten.
- **Ursache:** GIF braucht `coil-gif`. Zwei Decoder: `AnimatedImageDecoder` (nur API ≥ 28, schneller, auch WebP/HEIF) und `GifDecoder` (alle API). Falscher/fehlender → keine Animation.
- **Versionen:** alle 3.x (by-design).
- **FIX:** `coil-gif` einbinden (Auto-Erkennung); bei Custom-Loader: `if (SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())`.
- **Quelle:** https://coil-kt.github.io/coil/gifs/

### D6. Custom `ImageLoader.Builder` + `components { }` → Default-Decoder verloren (SVG/GIF tot) ⭐ HAEUFIG
- **Symptom:** sobald man einen eigenen `ImageLoader` mit `components { }` baut, gehen GIF/SVG nicht mehr automatisch.
- **Ursache:** Auto-Registrierung (ServiceLoader) nutzt der Singleton-Loader; im Custom-Loader übernimmt man die Verantwortung selbst.
- **Versionen:** alle 3.x.
- **FIX:** im Custom-Loader ALLE benötigten Komponenten explizit: `add(OkHttpNetworkFetcherFactory())`, `add(VideoFrameDecoder.Factory())`, `add(SvgDecoder.Factory())`, GIF-Decoder (D5).
- **Quelle:** https://coil-kt.github.io/coil/image_loaders/

### D7. `coil-video` ist Android-only (KMP-Build bricht)
- **Symptom:** KMP-Build scheitert, wenn `coil-video` in `commonMain` liegt.
- **Ursache:** nutzt Androids `MediaMetadataRetriever` → nur Android.
- **Versionen:** alle 3.x (by-design).
- **FIX:** `coil-video` nur in `androidMain`, Decoder nur dort registrieren.
- **Quelle:** https://coil-kt.github.io/coil/videos/

### D8. Decoder-Reihenfolge in `components { }`: erster passender gewinnt
- **Symptom:** „falscher" Decoder verarbeitet ein Asset (z.B. animiertes WebP nur als Standbild); eigener Decoder wird nie aufgerufen.
- **Ursache:** Coil fragt Factories in Registrierungsreihenfolge, erste passende gewinnt.
- **Versionen:** alle 3.x (Architektur).
- **FIX:** spezifischere/gewünschte Decoder ZUERST registrieren (eigene vor Standard; `AnimatedImageDecoder` vor generischem Bild-Decoder).
- **Quelle:** https://coil-kt.github.io/coil/image_pipeline/

### D9. SVG falsche Größe/Aspect Ratio (viewBox als intrinsische Größe)
- **Symptom:** SVG verzerrt/zu groß; Seitenverhältnis stimmt nicht trotz korrekter `width`/`height`.
- **Ursache:** `SvgDecoder` nutzt default `viewBox`-Bounds als intrinsische Größe (`useViewBoundsAsIntrinsicSize = true`); weicht vom SVG-Spec ab.
- **Versionen:** seit 1.2.0, in 3.0.4 unverändert (won't-fix-Default, abschaltbar).
- **FIX:** `add(SvgDecoder.Factory(useViewBoundsAsIntrinsicSize = false))`; SVG sauberen `viewBox` geben; feste `Modifier.size(...)`.
- **Quelle:** https://github.com/coil-kt/coil/issues/731

### D10. SVG aus String/ByteArray → schwarzer/leerer Screen
- **Symptom:** SVG als roher String an `AsyncImage` → leer, kein Fehler.
- **Ursache:** String wird als URI/Pfad interpretiert, nicht als SVG-Quelle.
- **Versionen:** 3.x (by-design).
- **FIX:** SVG als `ByteArray` übergeben: `AsyncImage(model = svgString.toByteArray(), ...)` (Decoder erkennt `<svg`-Marker).
- **Quelle:** https://github.com/coil-kt/coil/discussions/2527

---

## R) R8 / Setup / Singleton / Lifecycle

### R1. Release-Build R8: `Missing class coil3.PlatformContext` (Build bricht ab) ⭐ HAEUFIG
- **Symptom:** `:app:minifyReleaseWithR8 FAILED` → `Missing class coil3.PlatformContext (referenced from coil3.network.…)`; `missing_rules.txt` schlägt `-dontwarn coil3.PlatformContext` vor.
- **Ursache:** `coil3.PlatformContext` ist auf der JVM ein Type-Alias auf `Context`, existiert nicht als eigene Klasse; eine Referenz aus dem OkHttp-Fetcher lässt R8 sie als fehlend melden. Die nötige `-dontwarn`-Regel war in 3.0.0–3.1.0 NICHT mitgeliefert.
- **Versionen:** betroffen 3.0.0–3.1.0; **belegt gefixt ab 3.2.0** (Lib bringt die Consumer-Regel mit). **Auf 3.0.4 selbst setzen.**
- **FIX:** in `proguard-rules.pro`: `-dontwarn coil3.PlatformContext` (behebt nur den Build-Abbruch; Runtime siehe R2). Oder Upgrade auf ≥ 3.2.0.
- **Quelle:** https://github.com/coil-kt/coil/issues/2637 · CHANGELOG 3.2.0

### R2. `NoClassDefFoundError: coil3/PlatformContext` zur LAUFZEIT (auch Debug) bei `cacheStrategy`
- **Symptom:** Bilder laden nicht (auch im Debug); `DebugLogger`: `NoClassDefFoundError: Failed resolution of: Lcoil3/PlatformContext;`.
- **Ursache:** tritt mit eigenem `OkHttpNetworkFetcherFactory(cacheStrategy = { CacheControlCacheStrategy() })` auf; in 3.0.0–3.0.1 fehlerhafte Auflösung.
- **Versionen:** Crash mit Custom-CacheStrategy gefixt **ab 3.0.2** → in 3.0.4 erledigt.
- **FIX:** 3.0.4 enthält den Fix; bei Restproblemen `connectivityChecker` explizit setzen. Cache-Control nicht weglassen.
- **Quelle:** https://github.com/coil-kt/coil/issues/2637

### R3. Decoder/Fetcher fehlen im Release (ServiceLoader) → lädt nur im Debug ⭐ HAEUFIG
- **Symptom:** mit ProGuard (nicht R8) oder `exclude("META-INF/**")` laden Bilder zur Laufzeit nicht; Fetcher/Decoder „verschwunden".
- **Ursache:** Coil 3 registriert via Java-ServiceLoader (`META-INF/services`). R8 schreibt das automatisch um; ProGuard strippt es; `META-INF`-Exclude zerstört die Service-Einträge.
- **Versionen:** alle 3.x bei ProGuard/Exclude.
- **FIX:** R8 nutzen (Default; keine Coil-Keeps nötig). Bei ProGuard die FAQ-Keep-Regeln (`-keep class * implements coil3.util.FetcherServiceLoaderTarget`, `…DecoderServiceLoaderTarget`, `ServiceLoaderComponentRegistry`). `META-INF/services` nicht excluden. Robust: Fetcher/Decoder manuell `add()`.
- **Quelle:** https://coil-kt.github.io/coil/faq/

### R4. Release-Crash `SingletonImageLoader` „missing fields" (R8 full mode)
- **Symptom:** Release crasht beim Start/ersten Bild (fehlende Felder in `RealImageLoader`/`SystemCallbacks`), Debug läuft.
- **Ursache:** aggressive R8-Optimierung (full mode, AGP-8-Default) + Regel-Kollision; projekt-spezifisch.
- **Versionen:** gemeldet 3.0.0-rc01, Reste bis 3.1.0; ab 3.2.0 (mitgelieferte `-dontwarn`) entschärft.
- **FIX:** Coil aktualisieren; zu breite eigene Keep-Regeln minimieren; als Notnetz gezielte `-keep` auf `coil3.RealImageLoader`/`coil3.util.SystemCallbacks`; Diagnose `android.enableR8.fullMode=false`.
- **Quelle:** https://github.com/coil-kt/coil/issues/2546

### R5. `SingletonImageLoader`: Mehrfach-Init & Context-Memory-Leak
- **Symptom:** mehrere Loader/kein Cache-Sharing; oder LeakCanary meldet Activity-Leak.
- **Ursache:** `setSafe` nur einmal nahe App-Entry; die drei Setup-Wege sind exklusiv. Der an `newImageLoader(context)` übergebene Context ist oft eine Activity — in lazy-Buildern festgehalten → Leak (#3213).
- **Versionen:** alle 3.x.
- **FIX:** `Application implements SingletonImageLoader.Factory` und im Builder den **Application-Context** (`this`) nutzen, nicht den durchgereichten; nur EIN Init-Weg.
- **Quelle:** https://coil-kt.github.io/coil/getting_started/ · https://github.com/coil-kt/coil/issues/3213

### R6. `onSuccess`/`onError` feuern nie — Request still abgebrochen (Cancellation)
- **Symptom:** nur `onLoading`, dann Stille; Log „request cancelled" beim Verlassen/Scrollen.
- **Ursache:** Coil bindet den Request an Lifecycle/Composition; beim Verlassen wird automatisch gecancelt (bei Cancellation laufen `onSuccess`/`onError` bewusst nicht). Verstärkt durch fehlenden Network-Loader (N1) = ewiges Loading bis Cancel.
- **Versionen:** by-design 3.x.
- **FIX:** zuerst N1 ausschließen; `DebugLogger()` zum Unterscheiden echter Fehler vs. Cancellation; gültigen Context an `ImageRequest` geben; in KMP `LocalLifecycleOwner` sicherstellen.
- **Quelle:** https://github.com/coil-kt/coil/issues/2462

---

## ✅ Fix-Status (was ist schon behoben?)

> **Ehrlichkeits-Regel:** „belegt gefixt" = Changelog/Issue/verifiziert. `gh`-CLI nicht verfügbar; die R8-`PlatformContext`-Regel
> (3.2.0) und die aktuelle Version (3.5.0) wurden per WebSearch an CHANGELOG/Maven gegengeprüft.

### Belegt gefixt (bis zum Projekt-Anker coil 3.0.4)

| Früherer Bug | gefixt ab | Beleg | Bezug |
|--------------|-----------|-------|-------|
| DiskCache-Formatwechsel 2→3 (`Unexpected header: 10`) | Auto-Clear **3.0.0** (PR #1999), in 3.0.4 | #1998 | C3 |
| `bitmapConfig(RGB_565)` ignoriert | vor Stable (Alpha-Bug), in 3.0.4 | #2221 | OOM4 |
| Snapshot-State-Crash beim Scrollen | **3.0.0-alpha04**, in 3.0.4 | #2096 | (AI) |
| `NoClassDefFoundError` mit Custom-`cacheStrategy` | **3.0.2**, in 3.0.4 | #2637 | R2 |
| `maxBitmapSize` 4096-Schutz | **3.0.0** (Feature), in 3.0.4 | Upgrade-Guide | OOM7 |

### Noch NICHT gefixt / erst NACH 3.0.4 (Workaround in 3.0.4 aktiv)

| Bug | Status | Was tun in 3.0.4 | Bezug |
|-----|--------|------------------|-------|
| R8 `Missing class coil3.PlatformContext` (Build) | Consumer-Regel erst **ab 3.2.0** | `-dontwarn coil3.PlatformContext` selbst setzen | R1 |
| `ConstraintsSizeResolver`-Cancellation/Perf | gefixt **3.2.0** | ggf. Upgrade | (AI/R6) |
| LazyGrid-Flicker durch Size-Resolution | Regression **ab 3.1.0** | 3.0.4 frei davon; bei Upgrade `.size(...)` explizit | AI2 |
| kein Default-Netzwerk-Fetcher | **by-design** | `coil-network-okhttp` ergänzen | N1 |
| VideoFrameDecoder manuell · Migration · Cache · OOM · Decoder-Setup | **by-design** | korrektes Setup, nie Feature weglassen | D1, M*, C*, OOM*, D* |

→ **Empfehlung: Upgrade von 3.0.4 auf eine aktuelle 3.x (zuletzt 3.5.0)** bringt die R8-`PlatformContext`-Regel, Perf-/Cancellation-Fixes ohne Eigencode (Breaking-Changes prüfen; ab 3.2.0 Java-11-Bytecode für coil-compose).

---

## ✅ Pflicht-Checkliste (vor dem Commit von Coil-Code mental durchgehen)

- [ ] **Netzwerk:** `coil-network-okhttp` eingebunden (sonst laden URLs NICHT); `INTERNET`-Permission; ggf. cleartext-Config. (N1, N6)
- [ ] **Migration:** Imports `coil3.*`, Koordinaten `io.coil-kt.coil3`, alle coil3-Artefakte gleiche Version (3.0.4). (M1, N2)
- [ ] **Setup:** EIN `SingletonImageLoader` (Application-Context, nicht Activity); `PlatformContext`-Signatur; nur EIN Init-Weg. (M3, M4, R5)
- [ ] **AsyncImage:** `ImageRequest` per `remember(url)`, Callbacks über AsyncImage-Parameter; `Modifier.size(...)` + `contentScale`. (AI1, AI6)
- [ ] **Cache:** EINE ImageLoader-Instanz (Sharing); DiskCache nicht doppelt auf gleichem Dir; Cache-Größen via `maxSizePercent`. (C1, C2, C5)
- [ ] **OOM:** Zielgröße setzen (`AsyncImage`+Constraints bzw. `.size(...)`); `allowHardware(false)` nur bei Pixel-Zugriff; `RGB_565` für deckende Fotos. (OOM1, OOM2, OOM3)
- [ ] **Decoder:** `VideoFrameDecoder.Factory()` manuell registrieren (coil-video!); GIF-Decoder je API-Level; bei Custom-`components` ALLE Decoder explizit. (D1, D5, D6)
- [ ] **Video aus Netz:** Network-Fetcher UND VideoFrameDecoder zusammen. (D3)
- [ ] **Preview:** `AsyncImagePreviewHandler`/`LocalInspectionMode` statt leerem Preview. (P1, P2)
- [ ] **R8/Release:** R8 (nicht ProGuard); `-dontwarn coil3.PlatformContext` in 3.0.4 gesetzt; `META-INF/services` nicht excluden; Release-Build mit echtem Bild-Load getestet. (R1, R3)

---

## Bezug: Bug-Abschnitt ↔ Best-Practices

> Gegenseite (wie macht man es richtig):
> [`best-practices/android/coil3.md`](../../best-practices/android/coil3.md)
> (dort die Spiegel-Tabelle Best-Practice-Abschnitt ↔ Bug-Abschnitt).

| Bug-Abschnitt (hier) | Verwandter Best-Practice-Abschnitt |
|----------------------|------------------------------------|
| M1 Koordinaten/Imports | §2.2 coil-bom/gleiche Version |
| M3 Coil/Factory weg | §1.1 EIN Init-Weg, §1.4 Library |
| M4 PlatformContext | §7.4 Multiplatform |
| M6 state StateFlow | §3.8 state beobachten |
| M7 Default-Placeholder | §3.3 placeholder am Composable |
| N1 kein Default-Fetcher · N6 INTERNET/Cleartext | §2.1 coil-network-okhttp |
| N2 alpha-Artefakt | §2.2 coil-bom |
| N3 ktor-Engine · N8 KMP | §2.3 Multiplatform ktor3 |
| N7 Cache-Control ignoriert | §4.4 Cache-Control aus |
| AI1 instabiler ImageRequest · AI4 Keyer | §3.5 model stabil |
| AI2 3.1.0-Flicker | §3.6 SizeResolver |
| AI3 crossfade Cache-Hit | §3.4 crossfade global |
| AI5 Subcompose-Perf | §3.7 nicht in LazyList |
| AI6 size/contentScale | §3.2 Größe + contentScale |
| AI7 placeholderMemoryCacheKey | §3.3 placeholder/Key |
| AI8 Empty/ORIGINAL | §3.1 AsyncImage, §3.6 SizeResolver |
| P1–P3 Preview | §7.1 PreviewHandler |
| C1 mehrere Loader | §1.1/§1.2 Singleton |
| C2 zwei DiskCaches · C6 cacheDir-NPE | §4.2 Disk-Cache/EIN Dir |
| C3 DiskCache-Format | §4.5 Force-Refresh |
| C4 Größe im Key | §4.3 CachePolicy |
| C5 Cache-Größe | §4.1 Memory-Cache |
| OOM1 Size.ORIGINAL · OOM7 maxBitmapSize | §5.1 Zielgröße |
| OOM2 Hardware-Bitmap | §5.2 allowHardware |
| OOM3 BitmapPool entfernt | §5.4 kein BitmapPool |
| OOM4 bitmapConfig | §5.3 RGB_565 |
| OOM5 GIF-Speicher · OOM6 transformations | §5.5/§5.6 |
| D1 VideoFrameDecoder · D4 ohne Extension | §6.1 manuell registrieren |
| D3 Video aus Netz | §2.1 + §6.1 (Fetcher + Decoder) |
| D5 GIF-Decoder | §6.4 GIF API-Level |
| D6 components killt Defaults · D8 Reihenfolge | §6.5 Custom-Loader |
| D7 coil-video Android-only | §6.6 Android-only |
| D9 SVG viewBox · D10 SVG-String | §6.3 SVG |
| R1/R2 PlatformContext · R3 ServiceLoader · R4 Release-Crash | §7.2 R8 |
| R5 Mehrfach-Init/Leak | §1.1 EIN Init-Weg, §1.3 Application-Context |
| R6 onSuccess/onError feuern nie | §7.5 Lifecycle |
| Fix-Status / Upgrade | §8.1 Upgrade 3.5.0 |
