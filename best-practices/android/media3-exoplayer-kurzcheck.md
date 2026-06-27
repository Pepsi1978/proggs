# Media3 / ExoPlayer Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Gradle | nur benötigte media3-Module; ALLE auf GLEICHER Version (Version-Catalog) | §1.1 |
| 2 | Kotlin | optional `media3-common-ktx` (`Player.listen`) | §1.3 |
| 3 | Lifecycle | `DefaultLifecycleObserver`; API ≥ 24 onStart/onStop, ≤ 23 onResume/onPause | §2.1 |
| 4 | Freigabe | `player.release()` + Referenz `null`, deterministisch (onCleared/onDispose) | §2.2 |
| 5 | Config-Change | Player im (Android)ViewModel, Release in `onCleared()` | §2.3 |
| 6 | Vordergrund vs Hintergrund | Vordergrund: in onStop releasen; Hintergrund: `MediaSessionService` | §2.4, §6 |
| 7 | Threading | Player auf EINEM (Main-)Thread erstellen+bedienen; aus Coroutine `withContext(Main)` | §3.1 |
| 8 | Listener-Thread | Callbacks feuern auf `applicationLooper` — UI direkt setzen ok | §3.2 |
| 9 | Audio-Focus | `setAudioAttributes(attrs, handleAudioFocus = true)`; kein eigenes Focus-Handling | §4.1 |
| 10 | TTS-Attribute | `USAGE_MEDIA` + `AUDIO_CONTENT_TYPE_SPEECH` | §4.2 |
| 11 | Kopfhörer ab | `setHandleAudioBecomingNoisy(true)` | §4.3 |
| 12 | Status-UI | an `isPlaying`/`onIsPlayingChanged` koppeln, nicht `playWhenReady` | §4.4 |
| 13 | Player-Anzahl | EINE Instanz über viele Clips wiederverwenden, nicht pro Clip neu | §5.1 |
| 14 | Mehrere Clips | Playlist-API (`setMediaItems`/`addMediaItem`); `ConcatenatingMediaSource` deprecated | §5.2 |
| 15 | Reihenfolge | `setMediaItem(s) → prepare() → play()`; `pause()` statt `stop()`; Replay `seekTo(0)+play()` | §5.3 |
| 16 | Clip-Zuordnung | `setMediaId`/`setTag` + `onMediaItemTransition`/`onEvents` | §5.4 |
| 17 | kurze Clips | `DefaultLoadControl` `bufferForPlaybackMs ≈ 250–1000` (1.5.1-Default 2500) | §5.5 |
| 18 | TTS-Bytes | `ByteArrayDataSource`-Factory + `MediaItem.setMimeType(...)` | §5.6 |
| 19 | Hintergrund | Player+`MediaSession` in `MediaSessionService`; UI via `MediaController` | §6.1, §6.4 |
| 20 | Service-Manifest | `foregroundServiceType="mediaPlayback"` + FGS-Permissions + Intent-Filter | §6.2 |
| 21 | Screen-off-Streaming | `setWakeMode(C.WAKE_MODE_NETWORK)` (+ WAKE_LOCK-Permission) | §6.3 |
| 22 | Notification | media3 erzeugt sie; nur `MediaMetadata` liefern (ab API 33 nicht via Provider biegen) | §6.5 |
| 23 | Media-Buttons/Resume | media3 handhabt Buttons; Resume = MediaButtonReceiver + `onPlaybackResumption` | §6.6 |
| 24 | Compose-Player | im ViewModel; sonst `remember` + `DisposableEffect{ onDispose{ release() } }` | §7.1, §7.2 |
| 25 | Compose-Lifecycle | `LifecycleEventObserver` in `DisposableEffect(lifecycleOwner)` | §7.3 |
| 26 | Compose-Clipwechsel | `LaunchedEffect(clipKey)`, Player-Calls auf Main | §7.4 |
| 27 | Compose-State | Player = Single Source of Truth; State-Holder via `Player.listen` (Holder ab 1.6.0) | §7.5 |
| 28 | reines Audio | KEIN PlayerView/PlayerSurface/SurfaceView nötig | §7.6 |
| 29 | Fehler | `onPlayerError` + `prepare()`-Retry; ggf. `LoadErrorHandlingPolicy` | §8.1 |
| 30 | DI | EINE Instanz `@Singleton`/Service-scoped bereitstellen | §8.2 |
| 31 | R8 | media3 bringt Keep-Regeln mit; aktuelles AGP; Release-Build testen | §8.3 |
| 32 | Diagnose | `player.addAnalyticsListener(EventLogger())` im Debug; `onEvents` für gebündelte Updates | §8.4 |
