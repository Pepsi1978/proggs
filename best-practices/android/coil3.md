# Coil 3 (Bild-/Video-Laden in Compose) — Best Practices (Stand 2026-06-14, Version 3.0.4)

> WIE man Coil 3 von vornherein richtig einsetzt (idiomatisch, Do's & Don'ts). Recherchiert mit 6 Researchern
> parallel, **offizielle Quellen zuerst** (coil-kt.github.io/coil, github.com/coil-kt/coil CHANGELOG). Jeder
> Eintrag mit Quelle + `offiziell`/`extern`-Flag.
> **Versions-Anker (live aus BestJournalAndroid):** **coil 3.0.4** (`io.coil-kt.coil3`) mit `coil-compose` + `coil-video`,
> Kotlin 2.1.0, Compose, AGP 8.x (R8 full mode). **Achtung:** `coil-network-okhttp` ist aktuell NICHT eingebunden
> (siehe §2.1). Aktuellste Version: **3.5.0** (Upgrade-Empfehlung §8).
>
> **Gegenstück (was schiefgeht):** [`bugs/android/coil3.md`](../../bugs/android/coil3.md) — dort die Bug-Seite + Fix-Status.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Loader anlegen | EINEN `ImageLoader` app-weit teilen (Singleton); nur EIN Init-Weg | §1.1 |
| 2 | Custom-Config | einmal bauen, via Hilt `@Singleton` (`coil-core`) bereitstellen | §1.2 |
| 3 | Application-Factory | `ImageLoader.Builder(this)` = Application-Context (Leak-Schutz) | §1.3 |
| 4 | Netzwerkbilder | `coil-network-okhttp` ZWINGEND zusätzlich einbinden | §2.1 |
| 5 | Versionen | alle coil3-Artefakte gleich, am besten `coil-bom` | §2.2 |
| 6 | Multiplatform | `coil-network-ktor3` + Engine pro Plattform; kein OkHttp | §2.3 |
| 7 | Bild anzeigen | `AsyncImage` als Default (leitet Größe ab) | §3.1 |
| 8 | Größe | `Modifier.size(...)` + `contentScale` immer setzen | §3.2 |
| 9 | Zustände | `placeholder`/`error`/`fallback` als Painter; `placeholderMemoryCacheKey` | §3.3 |
| 10 | Crossfade | `crossfade(true)` einmal am Loader; Custom-Transition geht nicht | §3.4 |
| 11 | Stabiler model | URL/Data direkt; `ImageRequest` per `remember(url)`; Callbacks via AsyncImage | §3.5 |
| 12 | rememberAsyncImagePainter | nur mit `rememberConstraintsSizeResolver()` | §3.6 |
| 13 | SubcomposeAsyncImage | nicht in LazyList (Subcomposition langsam) | §3.7 |
| 14 | Memory-Cache | `MemoryCache.Builder().maxSizePercent(context, 0.25)` | §4.1 |
| 15 | Disk-Cache | eigenes Dir + `maxSizePercent(0.02)`/`maxSizeBytes`; EIN Cache pro Dir | §4.2 |
| 16 | Cache-Policy | `ENABLED` lassen; `WRITE_ONLY` für Force-Refresh | §4.3 |
| 17 | Cache-Header | Coil 3 ignoriert Cache-Control → `coil-network-cache-control` falls nötig | §4.4 |
| 18 | Force-Refresh | `remove(key)`/`WRITE_ONLY` (gleiche URL, neuer Inhalt) | §4.5 |
| 19 | Preloading | `ImageRequest` ohne Target + `enqueue` (ggf. `BlackholeDecoder`) | §4.6 |
| 20 | OOM-Schutz | Zielgröße ableiten lassen; `maxBitmapSize` 4096 nicht abschalten | §5.1 |
| 21 | Hardware-Bitmaps | `allowHardware(true)` lassen; lokal `false` nur bei Pixel-Zugriff | §5.2 |
| 22 | Speicher senken | `RGB_565` für deckende Fotos ohne Alpha | §5.3 |
| 23 | kein BitmapPool | Coil 3 hat keinen Pool → Zielgröße + Cache statt Pooling | §5.4 |
| 24 | Video-Thumbnail | `VideoFrameDecoder.Factory()` MANUELL registrieren | §6.1 |
| 25 | Frame-Wahl | `videoFramePercent(0.5)` statt schwarzem 1. Frame | §6.2 |
| 26 | SVG | `SvgDecoder` (Auto); `useViewBoundsAsIntrinsicSize` bewusst | §6.3 |
| 27 | GIF | ≥API28 `AnimatedImageDecoder` sonst `GifDecoder` | §6.4 |
| 28 | Custom components | bei eigenem Loader ALLE Decoder + Fetcher explizit | §6.5 |
| 29 | Compose-Preview | `LocalAsyncImagePreviewHandler` + `ColorImage` | §7.1 |
| 30 | R8/Release | R8 nutzen, keine Coil-Keeps; in 3.0.4 `-dontwarn coil3.PlatformContext` | §7.2 |
| 31 | Logging | `DebugLogger()` nur in Debug-Builds | §7.3 |
| 32 | Upgrade | 3.0.4 → 3.5.0 (R8-Fix, Perf, Caching-Fixes; Java 11 ab 3.2.0) | §8.1 |

---

## §1 ImageLoader & Singleton

### §1.1 EINEN ImageLoader app-weit teilen, nur EIN Init-Weg
- **Empfehlung:** Genau einen `ImageLoader` erstellen und app-weit teilen. Auf Android `SingletonImageLoader.Factory` an der `Application` (idiomatisch) ODER `SingletonImageLoader.setSafe { }` in `Application.onCreate` — exakt EINEN der drei Wege (Factory / `setSafe` / `setSingletonImageLoaderFactory`).
- **Begründung (offiziell):** „Coil performs best when you create a single `ImageLoader` and share it throughout your app. This is because each `ImageLoader` has its own memory cache, disk cache, and `OkHttpClient`." Setup „as soon as possible … inside `Application.onCreate`".
- **DO:** `class App : Application(), SingletonImageLoader.Factory { override fun newImageLoader(ctx) = ImageLoader.Builder(this).crossfade(true).build() }`.
- **DON'T:** pro Screen/ViewModel einen neuen Loader bauen; mehrere Init-Wege mischen; `setUnsafe` außerhalb Tests.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/image_loaders/ · https://coil-kt.github.io/coil/getting_started/

### §1.2 Custom-Config einmal via Hilt `@Singleton` (coil-core)
- **Empfehlung:** Bei DI den `ImageLoader` einmal bauen und als `@Singleton` bereitstellen; dafür an `io.coil-kt.coil3:coil-core` (ohne globalen Singleton) hängen.
- **Begründung (offiziell):** „If you have a larger app or want to manage your own `ImageLoaders` you can depend on `io.coil-kt.coil3:coil-core` … This route makes scoping … and testing easier."
- **DO:** `@Provides @Singleton fun provideImageLoader(@ApplicationContext ctx: Context) = ImageLoader.Builder(ctx).crossfade(true).build()`; in `AsyncImage(..., imageLoader = injected)`.
- **DON'T:** bei DI zusätzlich den globalen Singleton setzen (zwei Quellen der Wahrheit); Activity-Context injizieren.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/image_loaders/#dependency-injection

### §1.3 Application-Context verwenden, nicht den durchgereichten (Leak-Schutz)
- **Empfehlung:** In `newImageLoader(context: PlatformContext)` an der Application `ImageLoader.Builder(this)` nutzen (Application), nicht den Parameter-`context`.
- **Begründung (offiziell, Issue):** der durchgereichte `context` stammt von der ersten Nutzungsstelle (oft Activity); lazy Builder-Blöcke (z.B. DiskCache) halten ihn fest → Activity-Leak.
- **DO:** `ImageLoader.Builder(this)`. **DON'T:** Parameter-Context in lazy-Blöcken festhalten.
- `offiziell` · 2026-06-14 · https://github.com/coil-kt/coil/issues/3213

### §1.4 Libraries: NIE den Singleton setzen/lesen
- **Empfehlung:** In einer Library `coil-core` nutzen, eigenen `ImageLoader` bauen und durchreichen — nicht den globalen Singleton.
- **Begründung (offiziell):** „If you set the singleton `ImageLoader` in your library you could be overwriting the `ImageLoader` set by the app."
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/getting_started/#configuring-the-singleton-imageloader

---

## §2 Netzwerk-Laden

### §2.1 `coil-network-okhttp` ZWINGEND zusätzlich einbinden ⭐
- **Empfehlung:** Für Netzwerkbilder neben `coil-compose` immer genau EIN Network-Artefakt — auf Android `coil-network-okhttp`. INTERNET-Permission im Manifest.
- **Begründung (offiziell):** „By default, Coil 3.x does not include support for loading images from the network." Mit OkHttp: „Once imported, network URLs … will automatically be supported."
- **DO:** `implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")` + `<uses-permission android:name="android.permission.INTERNET"/>`.
- **DON'T:** annehmen, `coil-compose` allein lade URLs; mehrere Network-Engines gleichzeitig.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/network/ · https://coil-kt.github.io/coil/getting_started/

### §2.2 Alle coil3-Artefakte gleiche Version — `coil-bom`
- **Empfehlung:** Alle `io.coil-kt.coil3:*` auf derselben Version; idiomatisch via BOM, dann Artefakte ohne Versionsnummer.
- **Begründung (offiziell):** „Importing `coil-bom` allows you to depend on other Coil artifacts without specifying a version." Verhindert Mischen (Binär-Inkompatibilität).
- **DO:** `implementation(platform("io.coil-kt.coil3:coil-bom:3.0.4"))` + Artefakte ohne Version.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/getting_started/#artifacts

### §2.3 Multiplatform: `coil-network-ktor3` + Engine pro Plattform
- **Empfehlung:** In KMP `coil-network-ktor3` (nicht OkHttp, Android/JVM-only) + je Plattform eine Ktor-Engine (`ktor-client-android`/`-darwin`/`-java`); Init über `setSingletonImageLoaderFactory`.
- **Begründung (offiziell):** „If you use Compose Multiplatform, you'll need to use Ktor instead of OkHttp"; „import a Ktor engine for each platform (except Javascript)."
- **DON'T:** `coil-network-okhttp` in `commonMain`; Ktor-Artefakt ohne Engine.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/network/#ktor-network-engines

---

## §3 AsyncImage richtig nutzen

### §3.1 `AsyncImage` als Default-Composable
- **Empfehlung:** Für normale Bildanzeige (auch in Listen) `AsyncImage` — nicht `SubcomposeAsyncImage`, nicht manuell `rememberAsyncImagePainter`.
- **Begründung (offiziell):** „Prefer using `AsyncImage` in most cases. It correctly determines the size your image should be loaded at based on the constraints of the composable and the provided `ContentScale`."
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/

### §3.2 Größe begrenzen + `contentScale` setzen; `contentDescription` für Accessibility
- **Empfehlung:** `Modifier.size(...)`/Layout-Constraints + `contentScale` (z.B. `Crop`) explizit; `contentDescription` sinnvoll füllen (nur dekorativ `null`).
- **Begründung (offiziell):** begrenzte Constraints → kleinere, passend skalierte Bitmaps (Kern-OOM-Schutz). `contentDescription` „sets the text used by accessibility services".
- **DON'T:** unbegrenzte Constraints für große Remote-Bilder; gedankenlos überall `null`.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/

### §3.3 `placeholder`/`error`/`fallback` als Painter; `placeholderMemoryCacheKey`
- **Empfehlung:** Zustände über die Painter-Argumente (`painterResource(...)`) abdecken; für nahtlose List→Detail-Übergänge `placeholderMemoryCacheKey` (Key des Listenbilds).
- **Begründung (offiziell):** „Painters are … faster as Coil doesn't need to use subcomposition." Memory-Key als Placeholder → „smooth transition with no white flashes if the image is in the memory cache." `fallback` greift bei `data == null`, sonst `error`.
- **DON'T:** für simple Platzhalter `SubcomposeAsyncImage`-Slots öffnen.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/ · https://coil-kt.github.io/coil/recipes/

### §3.4 `crossfade(true)` einmal global am Loader
- **Empfehlung:** `crossfade(true)` am `ImageLoader.Builder` (App-weiter Default); pro Request nur überschreiben.
- **Begründung (offiziell):** offizielles Singleton-Muster setzt `crossfade(true)` am Loader. Custom-`Transition`s funktionieren NICHT mit `AsyncImage`/`rememberAsyncImagePainter` (brauchen `View`) — nur `CrossfadeTransition`. Bei Memory-Cache-Hit wird der Fade übersprungen (by-design); eigene Animation über `painter.state` und `dataSource != MEMORY_CACHE`.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/#transitions · https://coil-kt.github.io/coil/image_loaders/

### §3.5 `model` stabil halten; Callbacks über AsyncImage-Parameter
- **Empfehlung:** Als `model` einfache URL/Data; braucht man `ImageRequest`, per `remember(url)` cachen. Lade-Ausgang über `onSuccess`/`onError`/`onLoading` von `AsyncImage`, nicht als Lambda im Request.
- **Begründung (offiziell):** `model` darf `ImageRequest.data` ODER der `ImageRequest` selbst sein; ein bei jeder Recomposition neu gebauter Request ist ungleich → unnötige Reloads (`remember`-Stabilisierung = `extern` Compose-Prinzip). Callbacks von `AsyncImage` gehen nicht in die `model`-Equality.
- `offiziell` (model-Formen) / `extern` (remember) · 2026-06-14 · https://coil-kt.github.io/coil/compose/

### §3.6 `rememberAsyncImagePainter` nur mit `SizeResolver`
- **Empfehlung:** Nur wenn man einen `Painter` braucht; dann `rememberConstraintsSizeResolver()` an `ImageRequest.size(...)` + `Modifier.then(sizeResolver)`.
- **Begründung (offiziell):** „it does not detect the size … and always loads the image with its original dimensions" (OOM-Falle); `state` ist im ersten Frame `Empty`.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/#rememberasyncimagepainter

### §3.7 `SubcomposeAsyncImage` nicht in LazyList
- **Empfehlung:** Nur einsetzen, wenn man Slot-APIs braucht UND der `state` sofort korrekt sein muss; nicht in `LazyColumn`/`LazyRow`/`LazyGrid`.
- **Begründung (offiziell):** „Subcomposition is slower than regular composition so this composable may not be suitable for performance-critical parts of your UI (e.g. `LazyList`)."
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/#subcomposeasyncimage

### §3.8 Zustände beobachten: `AsyncImagePainter.state` (StateFlow)
- **Empfehlung:** Für eigene Loading/Error-UI `painter.state.collectAsState()`.
- **Begründung (offiziell):** `state` ist in 3.x ein `StateFlow`; „observed using `val state = painter.state.collectAsState()`."
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/

---

## §4 Cache-Policy (Memory + Disk)

### §4.1 Memory-Cache: `maxSizePercent(context, 0.25)`
- **Empfehlung:** Memory-Cache am Loader explizit; `MemoryCache.Builder().maxSizePercent(context, 0.25).build()` (prozentual = geräteadaptiv).
- **Begründung (offiziell):** LRU-Cache zuletzt dekodierter Bitmaps; prozentual passt sich an Heap-Größe an (`context` nötig).
- **DON'T:** Memory-Cache pauschal abschalten (erzwingt teure Re-Decodes); fixer Byte-Wert.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/image_loaders/

### §4.2 Disk-Cache: eigenes Dir + Größe; EIN Cache pro Verzeichnis
- **Empfehlung:** `DiskCache.Builder().directory(context.cacheDir.resolve("image_cache")).maxSizePercent(0.02)` (oder `maxSizeBytes`). Bei mehreren Loadern: geteilte DiskCache-Instanz ODER getrennte Verzeichnisse.
- **Begründung (offiziell):** Disk-Cache hält Netzwerkbilder; zwei aktive Caches auf demselben Ordner korrumpieren das Journal (`IllegalStateException`).
- **DON'T:** `maxSizeBytes` + `min/maximumMaxSizeBytes` mischen (letztere greifen dann nicht).
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/image_loaders/ · https://github.com/coil-kt/coil/discussions/2737

### §4.3 CachePolicy bewusst (ENABLED/READ_ONLY/WRITE_ONLY/DISABLED)
- **Empfehlung:** Default `ENABLED` lassen; gezielt: `WRITE_ONLY` (frisch laden, neu cachen), `READ_ONLY` (Offline), `DISABLED` (sensible One-Shot-Bilder).
- **Begründung (offiziell):** `memoryCachePolicy`/`diskCachePolicy`/`networkCachePolicy` steuern Lesen/Schreiben je Ebene.
- **DON'T:** pauschal `networkCachePolicy(DISABLED)` oder `memoryCachePolicy(DISABLED)` global (killt Scroll-Recycling).
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/api/coil-core/coil3.request/-image-request/-builder/

### §4.4 Cache-Header: Coil 3 ignoriert `Cache-Control` (default)
- **Empfehlung:** Bewusst sein, dass Coil 3 `Cache-Control` standardmäßig ignoriert und immer auf Disk cached. Server-Cache-Semantik nötig → `coil-network-cache-control` + `CacheControlCacheStrategy`.
- **Begründung (offiziell):** „By default, Coil 3.x does not respect `Cache-Control` headers and always saves a response to its disk cache." (API ≤ 25: `coreLibraryDesugaring`.)
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/network/#cache-control-support

### §4.5 Force-Refresh: gleiche URL, neuer Inhalt
- **Empfehlung:** Da Coil cached, reicht erneutes Laden nicht. Entweder Eintrag invalidieren (`memoryCache?.remove(key)` + Disk-Key), `WRITE_ONLY`-Policy, oder inhaltsbewussten Key (URL mit Hash/Version).
- **Begründung (offiziell):** `respectCacheHeaders` aus → expliziter Eingriff; `MemoryCache` bietet `remove`/`clear`.
- **DON'T:** `.data(sameUrl)` erneut laden und auf Aktualität hoffen.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/api/coil-core/coil3.memory/-memory-cache/

### §4.6 Preloading: `ImageRequest` ohne Target + `enqueue`
- **Empfehlung:** Bilder vorab cachen via `ImageRequest` ohne `target` + `imageLoader.enqueue(request)`. Nur Disk-Cache füllen: `memoryCachePolicy(DISABLED)` + `BlackholeDecoder.Factory()` (überspringt Decode).
- **Begründung (offiziell):** `enqueue` „Enqueue the request to be executed asynchronously."; FAQ-Preload-Rezept.
- **DON'T:** Dummy-Target/echte View zum Vorladen missbrauchen.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/faq/#how-do-i-preload-an-image

---

## §5 Speicher / OOM / Sampling

### §5.1 Zielgröße ableiten lassen; `maxBitmapSize` (4096²) nicht abschalten
- **Empfehlung:** `AsyncImage` + Constraints (§3.2) dekodiert in Anzeigegröße; bei `rememberAsyncImagePainter` `SizeResolver` (§3.6); Nicht-Compose: `ImageRequest.size(w,h)`. Die 4096²-Grenze als OOM-Sicherheitsnetz behalten.
- **Begründung (offiziell):** Zielgröße = zentraler OOM-Schutz; „Output image dimensions are now enforced to be less than 4096x4096 to guard against accidental OOMs." `precision(INEXACT)` (AsyncImage-Default) spart RAM + erhöht Cache-Hits.
- **DON'T:** `maxBitmapSize(Size.ORIGINAL)` (entfernt die Bremse); `precision(EXACT)` pauschal.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/upgrading_to_coil3/#general · https://coil-kt.github.io/coil/compose/

### §5.2 `allowHardware(true)` lassen, lokal `false` nur bei Pixel-Zugriff
- **Empfehlung:** Hardware-Bitmaps (GPU-effizient, Default) global an; `allowHardware(false)` nur pro Request bei Palette, `toBitmap()`/`drawToBitmap`, Shared-Element-Transitions.
- **Begründung (offiziell):** Software-Zugriff auf Hardware-Bitmaps wirft `IllegalArgumentException: Software rendering doesn't support hardware bitmaps`.
- **DON'T:** Hardware-Bitmaps global deaktivieren (kostet RAM + GPU-Effizienz).
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/recipes/#palette · https://coil-kt.github.io/coil/recipes/#shared-element-transitions

### §5.3 `RGB_565` für deckende Fotos ohne Alpha
- **Empfehlung:** `bitmapConfig(Bitmap.Config.RGB_565)` (oder `allowRgb565(true)`) für große, deckende Fotos → halbiert RAM (2 statt 4 Byte/Pixel).
- **Begründung (offiziell):** `allowRgb565` „when an image is guaranteed to not have alpha." Trade-off: kein Alpha, Banding in Verläufen — nur für deckende Fotos.
- **DON'T:** RGB_565 für Bilder mit Transparenz/feinen Verläufen.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/api/coil-core/coil3.request/allow-rgb565.html

### §5.4 Kein BitmapPool mehr → Zielgröße + Cache statt Pooling
- **Empfehlung:** Coil-2-Pooling-Denken loslassen; Speicher steuert man über Zielgröße, `maxBitmapSize`, Bitmap-Config und Cache.
- **Begründung (offiziell):** Bitmap-Pool in Coil 3 entfernt (Multiplatform/Skia); Upgrade-Doc listet nur Größen-/Cache-Mechanismen.
- **DON'T:** Code aus Coil-2-Anleitungen übernehmen, der Pooling voraussetzt.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/upgrading_to_coil3/

### §5.5 LazyGrid: feste Zellgrößen + `ContentScale.Crop`
- **Empfehlung:** In Grids/Listen jeder Zelle feste Größe + `ContentScale.Crop` → viele kleine statt großer Bitmaps; `SubcomposeAsyncImage` meiden (§3.7).
- **Begründung (offiziell):** folgt aus der Größenbestimmung von `AsyncImage` (§3.1/§5.1).
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/

### §5.6 `transformations` auf bereits heruntergerechneten Bildern
- **Empfehlung:** Zielgröße + Transformation kombinieren, nie Transformation als Ersatz für Größe.
- **Begründung (offiziell):** Transformationen laufen auf dem Output-Bitmap (Software); `allowConversionToBitmap` nötig. Auf großem Bild verdoppelt sich kurz der Bedarf.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/api/coil-core/coil3.request/transformations.html

---

## §6 Decoder (Video / SVG / GIF)

### §6.1 `VideoFrameDecoder.Factory()` MANUELL registrieren ⭐
- **Empfehlung:** Beim Bauen des `ImageLoader` `components { add(VideoFrameDecoder.Factory()) }`; Import `coil3.video.VideoFrameDecoder`.
- **Begründung (offiziell):** Video-Doc verlangt explizit, den Decoder zur Component-Registry hinzuzufügen — anders als SVG/GIF (Auto-Erkennung) passiert nichts automatisch.
- **DON'T:** nur `coil-video` als Dependency einbinden und Auto-Laden erwarten.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/videos/

### §6.2 Frame-Position bewusst wählen
- **Empfehlung:** `videoFramePercent(0.5)` (robust, ohne Längenkenntnis) statt Default; `videoFrameMillis(...)` für feste Zeit; `videoFrameIndex` (API 28+); `videoFrameOption(OPTION_CLOSEST_SYNC)` für schnelle Thumbnails.
- **Begründung (offiziell):** „If no frame position is specified, the first frame … will be decoded" (oft schwarz/Vorspann).
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/videos/

### §6.3 SVG: Auto-Erkennung + `useViewBoundsAsIntrinsicSize` bewusst
- **Empfehlung:** `coil-svg` reicht meist (Auto-Erkennung am `<svg`-Marker). `SvgDecoder.Factory(useViewBoundsAsIntrinsicSize = true)` = viewBox als Größe (korrektes Verhältnis ohne width/height); `false` = width/height-Attribute. `renderToBitmap` (Default true) = sofort rastern (schnell, mehr RAM).
- **Begründung (offiziell, API):** falsche intrinsische Größe = Hauptursache für verzerrte/unscharfe SVGs; echte Zielgröße im Layout vorgeben.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/svgs/ · https://coil-kt.github.io/coil/api/coil-svg/coil3.svg/-svg-decoder/

### §6.4 GIF: ≥API28 `AnimatedImageDecoder`, sonst `GifDecoder`
- **Empfehlung:** `coil-gif` einbinden (Auto). Bei Custom-Loader: `if (SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())`.
- **Begründung (offiziell):** `AnimatedImageDecoder` ist schneller und kann animiertes WebP/HEIF (API 28+); `GifDecoder` ist langsamer, aber alle API-Level.
- **DON'T:** `AnimatedImageDecoder` ohne API-Guard < 28.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/gifs/

### §6.5 Custom-Loader: ALLE Decoder + Fetcher explizit; Reihenfolge spezifisch-zuerst
- **Empfehlung:** Sobald man `components { }` selbst anfasst, jeden gebrauchten Decoder + Network-Fetcher (+ ggf. `Keyer` für Custom-Datentyp) explizit registrieren; spezifische Decoder zuerst.
- **Begründung (offiziell):** Custom-Loader bekommt nur, was man addiert; „first match wins" in der Pipeline. Fehlt der `Keyer`, ist das Ergebnis nicht memory-cacheable.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/image_pipeline/

### §6.6 `coil-video`/`coil-gif` sind Android-only (KMP)
- **Empfehlung:** In KMP `coil-video`/`coil-gif` nur in `androidMain`; `coil-svg` ist multiplatform.
- **Begründung (offiziell):** coil-video/coil-gif stützen sich auf Android-Decoder.
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/upgrading_to_coil3/

---

## §7 Preview / R8 / Lifecycle / Logging

### §7.1 Compose-Preview über `LocalAsyncImagePreviewHandler`
- **Empfehlung:** Im `@Preview` einen `AsyncImagePreviewHandler { ColorImage(...) }` via `CompositionLocalProvider(LocalAsyncImagePreviewHandler provides ...)` bereitstellen; gilt auch für Screenshot-Tests.
- **Begründung (offiziell):** „Network access is disabled in the preview environment so network URLs will always fail." (`ColorImage` seit 3.1.0, ab 3.2.0 stabil.)
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/

### §7.2 R8 nutzen, keine Coil-Keeps; in 3.0.4 `-dontwarn coil3.PlatformContext`
- **Empfehlung:** R8 (Default) verwenden — Coil bringt Consumer-Regeln automatisch mit. Auf dem Anker **3.0.4** zusätzlich selbst `-dontwarn coil3.PlatformContext` setzen (Lib-Regel erst ab 3.2.0). Bei ProGuard die FAQ-Keep-Regeln für ServiceLoader-Targets. Release-Build mit echtem Bild-Load testen.
- **Begründung (offiziell):** „You do not need to add any custom rules for Coil if you use R8 … added automatically." CHANGELOG 3.2.0: „Fix warning for missing `PlatformContext` when building with R8."
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/faq/ · https://coil-kt.github.io/coil/changelog/ (3.2.0)

### §7.3 `DebugLogger()` nur in Debug-Builds
- **Empfehlung:** `if (BuildConfig.DEBUG) logger(DebugLogger())` am Loader.
- **Begründung (offiziell):** FAQ: „`DebugLogger` should only be used in debug builds."
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/faq/#how-do-i-enable-logging

### §7.4 Multiplatform: `LocalPlatformContext` + `Res.getUri(...)`
- **Empfehlung:** In gemeinsamem Code `ImageRequest.Builder(LocalPlatformContext.current)`; Ressourcen über `Res.getUri("drawable/x")` (nicht `Res.drawable.x`).
- **Begründung (offiziell):** `PlatformContext` ersetzt `Context`; „`Res.drawable.image` … are not supported … use `Res.getUri(...)`."
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/ · https://coil-kt.github.io/coil/upgrading_to_coil3/

### §7.5 Lifecycle: Requests werden automatisch gecancelt
- **Empfehlung:** Kein manuelles Cancellation-Handling; Coil bindet Requests an Composition/Lifecycle. Fehler über `error`-Painter/`onError`/`state` sichtbar machen (Cancellation beim Verlassen ist erwartet, kein Bug).
- **Begründung (offiziell):** Compose-APIs „restartable and skippable"; beim Verlassen automatische Cancellation (`onSuccess`/`onError` laufen dann nicht).
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/compose/

---

## §8 Upgrade

### §8.1 3.0.4 → 3.5.0 empfohlen
- **Empfehlung:** Auf aktuelle 3.5.0 upgraden (alle coil3-Artefakte gleich, via BOM).
- **Begründung (offiziell, CHANGELOG):** R8-`PlatformContext`-Regel mitgeliefert (3.2.0) → kein eigener `-dontwarn` mehr; `AsyncImage`-Runtime +25–40 % schneller, weniger Allokationen (3.1.0); Caching-Fixes (404/500-Handling, `DeDupeConcurrentRequestStrategy` 3.4.0); `memoryCacheMaxSizePercentWhileInBackground` (3.3.0/3.5.0); `maxBitmapSize`-Edge-Case-Fix (3.4.0).
- **Breaking-Changes:** ab **3.2.0** brauchen `coil-compose`/`-core` **Java-11-Bytecode** (`jvmTarget = "11"`); **3.5.0** hebt **minSdk auf 23** und entfernt `iosX64`/`macosX64`.
- **DO:** Upgrade + Java-11-Compile-Options + minSdk/Targets prüfen. **DON'T:** auf 3.0.4 bleiben (verpasst R8-Fix/Perf/Caching).
- `offiziell` · 2026-06-14 · https://coil-kt.github.io/coil/changelog/

---

## Bezug: Best-Practice-Abschnitt ↔ Bug-Abschnitt

> Wechselseitig mit [`bugs/android/coil3.md`](../../bugs/android/coil3.md) (dort die Spiegel-Tabelle).

| Best-Practice (hier) | Verwandter Bug-Abschnitt (Almanach) |
|----------------------|-------------------------------------|
| §1.1 EIN Loader / EIN Init-Weg | C1 mehrere Loader, R5 Mehrfach-Init |
| §1.2 Hilt @Singleton (coil-core) | C1 Cache-Sharing |
| §1.3 Application-Context | R5 Context-Leak |
| §1.4 Library kein Singleton | M3 (Singleton-Setup) |
| §2.1 coil-network-okhttp | N1 kein Default-Fetcher, N6 INTERNET/Cleartext |
| §2.2 coil-bom / gleiche Version | N2 alpha-Artefakt NoSuchMethodError, M1 Koordinaten |
| §2.3 Multiplatform ktor3 | N3 ktor-Engine-Mismatch, N8 KMP-Artefakt |
| §3.1 AsyncImage Default | AI8 rememberAsyncImagePainter Empty/ORIGINAL |
| §3.2 Größe + contentScale | AI6 fehlendes size/contentScale |
| §3.3 placeholder/Key | M7 Default-Placeholder fehlt, AI7 placeholderMemoryCacheKey |
| §3.4 crossfade global | AI3 crossfade bei Cache-Hit |
| §3.5 model stabil | AI1 instabiler ImageRequest, AI4 Custom-Fetcher/Keyer |
| §3.6 rememberAsyncImagePainter | AI8 Empty/ORIGINAL, AI2 3.1.0-Flicker |
| §3.7 SubcomposeAsyncImage | AI5 Subcompose-Perf |
| §3.8 state StateFlow | M6 state ist StateFlow |
| §4.1 Memory-Cache | C5 Cache-Größe, OOM4 |
| §4.2 Disk-Cache / EIN Dir | C2 zwei DiskCaches, C5, C6 cacheDir-NPE |
| §4.3 CachePolicy | C4 Größe im Key |
| §4.4 Cache-Control aus | N7/C4 Cache-Control ignoriert |
| §4.5 Force-Refresh | C3 DiskCache-Formatwechsel |
| §4.6 Preloading | (Performance) |
| §5.1 Zielgröße/maxBitmapSize | OOM1 Size.ORIGINAL, OOM7 maxBitmapSize |
| §5.2 allowHardware | OOM2 Hardware-Bitmap-Crash |
| §5.3 RGB_565 | OOM4 bitmapConfig (Alpha-Bug) |
| §5.4 kein BitmapPool | OOM3 BitmapPool entfernt |
| §5.5 LazyGrid | OOM5/OOM-Listen |
| §5.6 transformations | OOM6 transformations+EXACT |
| §6.1 VideoFrameDecoder | D1 manuell registrieren, D4 ohne Extension |
| §6.2 Frame-Position | (Setup) |
| §6.3 SVG | D9 viewBox-Größe, D10 SVG-String |
| §6.4 GIF | D5 GIF-Decoder API-Level |
| §6.5 Custom-Loader Decoder | D6 components killt Defaults, D8 Reihenfolge |
| §6.6 Android-only | D7 coil-video Android-only |
| §7.1 Preview | P1 Preview leer, P2/P3 Preview-Loading |
| §7.2 R8 | R1 PlatformContext-Build, R2 Runtime, R3 ServiceLoader, R4 Release-Crash |
| §7.4 Multiplatform | M4 PlatformContext, N8 |
| §7.5 Lifecycle | R6 onSuccess/onError feuern nie |
| §8.1 Upgrade 3.5.0 | Fix-Status-Tabelle (Almanach) |
