# Bild-/Video-Laden mit Coil 3 (`io.coil-kt.coil3`) in Jetpack Compose Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
