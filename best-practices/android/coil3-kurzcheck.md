# Coil 3 (Bild-/Video-Laden in Compose) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
