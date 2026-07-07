---
name: android-audio
description: >
  Professional Android audio and sound effect integration. Use when: adding sound effects
  to Android apps, implementing audio playback, working with Oboe/SoundPool/Media3,
  choosing audio APIs, setting up low-latency audio, managing audio focus, creating
  audio visualizations, or asking "which audio API should I use?".
---

# Android Audio Skill — Professional Sound Integration

## Step 1: Choose the Right API

Ask the user what kind of audio they need, then recommend:

| Need | API | Kotlin/C++ | Latency |
|------|-----|------------|---------|
| Short UI sounds (clicks, alerts, game effects < 5s) | **SoundPool** | Pure Kotlin | ~50ms |
| Music, podcasts, streaming, long audio | **Jetpack Media3** | Pure Kotlin | ~100ms |
| Real-time audio, instruments, games requiring < 20ms | **Oboe** | C++ (NDK) + Kotlin JNI | **< 20ms** |
| Procedural/generated sounds (synthesis, DSP) | **TarsosDSP** or **MWEngine** | Kotlin / C++ | Variable |
| 3D spatial audio (headphones, XR) | **Spatializer API** (API 32+) | Pure Kotlin | ~50ms |
| Audio synced with haptic vibration | **HapticGenerator** (API 31+) | Pure Kotlin | Synced |

## Step 2: Generate Dependencies

Based on the chosen API, add these to `build.gradle.kts`:

### SoundPool (built-in, no dependency needed)
```kotlin
// SoundPool is part of Android SDK — no extra dependency
// Just use: import android.media.SoundPool
```

### Jetpack Media3
```kotlin
val media3Version = "1.6.0"
dependencies {
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common-ktx:$media3Version") // Kotlin Coroutines
}
```

### Oboe (C++ NDK)
```kotlin
// build.gradle.kts
android {
    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_static")
                cppFlags("-std=c++17")
            }
        }
    }
    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }
}
dependencies {
    implementation("com.google.oboe:oboe:1.9.0")
}
```

### TarsosDSP
```kotlin
repositories { maven { url = uri("https://jitpack.io") } }
dependencies {
    implementation("com.github.AkshayChordiya:TarsosDSP-Android:master-SNAPSHOT")
}
```

## Step 3: Generate Boilerplate Code

### SoundPool Manager (for short effects)
```kotlin
class SoundEffectManager(private val context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val sounds = mutableMapOf<String, Int>()

    fun load(name: String, resId: Int) {
        sounds[name] = soundPool.load(context, resId, 1)
    }

    fun play(name: String, volume: Float = 1.0f) {
        sounds[name]?.let { soundPool.play(it, volume, volume, 1, 0, 1.0f) }
    }

    fun release() = soundPool.release()
}
```

### Media3 Player (for music/streaming)
```kotlin
@Composable
fun AudioPlayer(uri: String) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (player.isPlaying) player.pause() else player.play()
        }) {
            Icon(
                if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (player.isPlaying) "Pause" else "Play"
            )
        }
    }
}
```

## Step 4: Audio Format Recommendations

| Format | Best for | Size | Quality | Android since |
|--------|----------|------|---------|---------------|
| **OGG Vorbis** | Effects + Music | Small | Excellent | API 1 |
| **OGG Opus** | Speech/Streaming | Very small | Excellent low-bitrate | API 21 |
| **WAV** | Development only | Large | Lossless | API 1 |
| **FLAC** | Hi-fi music apps | Medium | Lossless | API 12 |

**Recommendation:** Use OGG Vorbis at 128-192 kbps for everything. Better than MP3 at same bitrate.

## Step 5: Audio Focus (MANDATORY for playback apps)

```kotlin
val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
    .setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build())
    .setOnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> player.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.setVolume(0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> { player.setVolume(1.0f); player.play() }
        }
    }.build()
audioManager.requestAudioFocus(focusRequest)
```

## Step 6: Free Sound Resources

| Source | License | Best for |
|--------|---------|----------|
| [Zapsplat](https://www.zapsplat.com/) | Royalty-free | 160K+ sounds, app/game ready |
| [Freesound](https://freesound.org/) | CC (varies) | OGG downloads, community |
| [SONNISS GDC](https://sonniss.com/gameaudiogdc/) | Royalty-free, no attribution | Annual pro pack |
| [Mixkit](https://mixkit.co/free-sound-effects/) | Royalty-free | Clean game sounds |

## Sound Preview

To preview sounds on macOS before adding to your app:
```bash
# Play any audio file
afplay path/to/sound.ogg
afplay path/to/sound.wav
afplay path/to/sound.mp3

# Play with volume adjustment (0.0 to 1.0)
afplay -v 0.5 path/to/sound.ogg

# Play at different speed
afplay -r 1.5 path/to/sound.ogg  # 1.5x speed
```

For visual preview with waveform, open in QuickTime Player or use `ffplay`:
```bash
ffplay -nodisp path/to/sound.ogg  # Audio only, no window
ffplay -showmode 1 path/to/sound.ogg  # With waveform visualization
```

## Proactive Sound Suggestions (MANDATORY)

When working on ANY Android app with UI interactions, ALWAYS proactively suggest sounds:

> **Sound-Moeglichkeiten fuer dein Projekt:**
>
> | Interaktion | Sound-Typ | Empfehlung |
> |-------------|-----------|------------|
> | Button-Taps | Kurzer Click (50-100ms) | SoundPool, OGG Vorbis |
> | Richtige Antwort | Aufsteigende Tonfolge | SoundPool + HapticFeedback |
> | Falsche Antwort | Kurzer Buzz/absteigend | SoundPool + VibrationEffect |
> | Timer/Countdown | Tick-Tock, Tempo steigernd | SoundPool mit Pitch-Variation |
> | Seitenwechsel | Sanfter Whoosh | SoundPool |
> | Achievement/Level-Up | Fanfare (1-2s) | Media3 oder SoundPool |
> | Hintergrundmusik | Loop, dezent | Media3 mit AudioFocus |
>
> Soll ich passende Sounds auf Freesound suchen? Sag z.B. "suche einen Erfolgs-Sound"

After adding any sound to the project, ALWAYS offer to preview it:
> Soll ich dir den Sound vorspielen? (`afplay [path]`)

Use the `sound-search` skill for Freesound API integration when the user wants to search.
