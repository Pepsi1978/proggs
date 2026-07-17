package de.frank.karteikartenlernen.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import de.frank.karteikartenlernen.text.ArticleBlock
import de.frank.karteikartenlernen.text.parseResearchArticle
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

internal data class SpeechUnit(val text: String, val pauseAfterMs: Long)
internal data class ManualSpeechPlan(val units: List<SpeechUnit>, val continuationIndex: Int)

class EdgeTtsPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private data class PreparedAudio(val index: Int, val file: File)

    private class PlaybackSession(
        val generation: Long,
        val units: MutableList<SpeechUnit>,
        val voice: String,
        val speechRate: Float,
        val onPlaybackStart: (() -> Unit)?,
        val onComplete: () -> Unit,
        val manualContinuationIndex: Int? = null,
        val queued: Boolean = false,
    ) {
        val completed = AtomicBoolean(false)
        val files = mutableSetOf<File>()
        var prepared: PreparedAudio? = null
        var preparingIndex: Int? = null
        var waitingIndex: Int? = null
        var pauseElapsed = false
        var preparationFailed = false
        var manualContinuationRequested = false
        var playingIndex: Int? = null
    }

    private var mediaPlayer: MediaPlayer? = null
    private var webSocket: WebSocket? = null
    private var watchdogJob: Job? = null
    private var watchdogOwner: String? = null
    private var pauseJob: Job? = null
    @Volatile private var currentOutputStream: FileOutputStream? = null
    @Volatile private var generation = 0L
    private var activeSession: PlaybackSession? = null

    fun speak(
        text: String,
        voice: String = TtsVoiceRegistry.DEFAULT_VOICE_ID,
        speechRate: Float = 1f,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit = {},
    ) {
        val units = buildSpeechUnits(text)
        startSession(units, voice, speechRate, onPlaybackStart, onComplete)
    }

    fun speakWithPreparedContinuation(
        text: String,
        continuationText: String,
        voice: String = TtsVoiceRegistry.DEFAULT_VOICE_ID,
        speechRate: Float = 1f,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit = {},
    ) {
        val plan = buildManualSpeechPlan(text, continuationText)
        startSession(
            units = plan.units,
            voice = voice,
            speechRate = speechRate,
            onPlaybackStart = onPlaybackStart,
            onComplete = onComplete,
            manualContinuationIndex = plan.continuationIndex,
        )
    }

    fun continuePreparedSpeech() {
        val session = activeSession ?: return
        val continuationIndex = session.manualContinuationIndex ?: return
        session.manualContinuationRequested = true
        if (session.waitingIndex != continuationIndex) return
        session.pauseElapsed = true
        val prepared = session.prepared
        when {
            prepared?.index == continuationIndex -> playPrepared(session, prepared)
            session.preparationFailed -> {
                session.preparationFailed = false
                prepareUnit(session, continuationIndex)
            }
        }
    }

    fun startSpeechQueue(
        voice: String = TtsVoiceRegistry.DEFAULT_VOICE_ID,
        speechRate: Float = 1f,
    ) {
        startSession(
            units = emptyList(),
            voice = voice,
            speechRate = speechRate,
            onPlaybackStart = null,
            onComplete = {},
            queued = true,
        )
    }

    fun enqueueSpeech(text: String) {
        val session = activeSession?.takeIf { it.queued } ?: return
        val additions = buildSpeechUnits(text)
        if (additions.isEmpty()) return
        val firstNewIndex = session.units.size
        if (firstNewIndex > 0) {
            session.units[firstNewIndex - 1] = session.units[firstNewIndex - 1].copy(pauseAfterMs = SECTION_PAUSE_MS)
        }
        session.units += additions
        val idle = session.playingIndex == null && session.preparingIndex == null && session.prepared == null
        if (idle) {
            if (firstNewIndex > 0) {
                session.waitingIndex = firstNewIndex
                session.pauseElapsed = true
            }
            session.preparationFailed = false
            prepareUnit(session, firstNewIndex)
        } else if (session.playingIndex != null && session.preparingIndex == null && session.prepared == null) {
            prepareUnit(session, firstNewIndex)
        }
    }

    private fun startSession(
        units: List<SpeechUnit>,
        voice: String,
        speechRate: Float,
        onPlaybackStart: (() -> Unit)?,
        onComplete: () -> Unit,
        manualContinuationIndex: Int? = null,
        queued: Boolean = false,
    ) {
        stop()
        if (units.isEmpty() && !queued) {
            onComplete()
            return
        }
        val session = PlaybackSession(
            generation = generation,
            units = units.toMutableList(),
            voice = TtsVoiceRegistry.resolveVoiceId(voice),
            speechRate = speechRate,
            onPlaybackStart = onPlaybackStart,
            onComplete = onComplete,
            manualContinuationIndex = manualContinuationIndex,
            queued = queued,
        )
        activeSession = session
        prepareUnit(session, 0)
    }

    private fun prepareUnit(session: PlaybackSession, index: Int) {
        if (!isActive(session) || index !in session.units.indices || session.preparingIndex != null) return
        session.preparingIndex = index
        val text = session.units[index].text
        val connectionId = randomId()
        val requestId = randomId()
        val terminal = AtomicBoolean(false)
        val request = Request.Builder()
            .url(
                "$EDGE_TTS_URL?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                    "&Sec-MS-GEC=${generateSecMsGec()}" +
                    "&Sec-MS-GEC-Version=1-$CHROMIUM_FULL_VERSION" +
                    "&ConnectionId=$connectionId",
            )
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36 " +
                    "Edg/$CHROMIUM_MAJOR_VERSION.0.0.0",
            )
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Cookie", "muid=${generateMuid()};")
            .build()

        val audioFile = File(appContext.cacheDir, "tts_${session.generation}_${index}_${randomId()}.mp3")
        session.files += audioFile
        val outputStream = try {
            FileOutputStream(audioFile).also { currentOutputStream = it }
        } catch (error: Exception) {
            Log.e(TAG, "Could not open TTS cache file", error)
            preparationFailed(session, index, audioFile)
            return
        }

        fun fail(socket: WebSocket, error: Throwable) {
            if (!terminal.compareAndSet(false, true)) return
            Log.e(TAG, "Edge TTS preparation failed: ${error.message}", error)
            cancelWatchdog(requestId)
            socket.cancel()
            closeOutputStream(outputStream)
            audioFile.delete()
            if (webSocket === socket) webSocket = null
            scope.launch { preparationFailed(session, index, audioFile) }
        }

        try {
            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (!isActive(session)) return
                        startWatchdog(webSocket, WATCHDOG_INITIAL_MS, session.generation, requestId, terminal) {
                            fail(webSocket, IllegalStateException("Edge TTS antwortet nicht."))
                        }
                        webSocket.send(SPEECH_CONFIG)
                        webSocket.send(buildSsmlFrame(requestId, text, session.voice, session.speechRate))
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        if (!isActive(session)) return
                        val data = bytes.toByteArray()
                        if (data.size <= 2) return
                        val headerLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                        val audioStart = headerLength + 2
                        if (data.size > audioStart) {
                            try {
                                outputStream.write(data, audioStart, data.size - audioStart)
                            } catch (error: Exception) {
                                fail(webSocket, error)
                                return
                            }
                            startWatchdog(webSocket, WATCHDOG_IDLE_MS, session.generation, requestId, terminal) {
                                fail(webSocket, IllegalStateException("Edge TTS liefert keine weiteren Audiodaten."))
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (!isActive(session) || !text.contains("Path:turn.end") || !terminal.compareAndSet(false, true)) return
                        cancelWatchdog(requestId)
                        closeOutputStream(outputStream)
                        webSocket.close(1000, "turn.end")
                        if (this@EdgeTtsPlayer.webSocket === webSocket) this@EdgeTtsPlayer.webSocket = null
                        scope.launch { prepared(session, PreparedAudio(index, audioFile)) }
                    }

                    override fun onFailure(webSocket: WebSocket, error: Throwable, response: Response?) {
                        if (isActive(session)) fail(webSocket, error)
                    }
                },
            )
        } catch (error: Exception) {
            closeOutputStream(outputStream)
            audioFile.delete()
            Log.e(TAG, "Creating Edge TTS WebSocket failed", error)
            preparationFailed(session, index, audioFile)
        }
    }

    private fun prepared(session: PlaybackSession, audio: PreparedAudio) {
        if (!isActive(session)) {
            audio.file.delete()
            return
        }
        if (session.preparingIndex == audio.index) session.preparingIndex = null
        session.prepared = audio
        val waitingForManualFlip = audio.index == session.manualContinuationIndex && !session.manualContinuationRequested
        if (!waitingForManualFlip && (audio.index == 0 || session.waitingIndex == audio.index && session.pauseElapsed)) {
            playPrepared(session, audio)
        }
    }

    private fun preparationFailed(session: PlaybackSession, index: Int, file: File) {
        session.files -= file
        file.delete()
        if (!isActive(session)) return
        if (session.preparingIndex == index) session.preparingIndex = null
        session.preparationFailed = true
        if (index == session.manualContinuationIndex && !session.manualContinuationRequested) return
        if (index == 0 || session.waitingIndex == index && session.pauseElapsed) finishSession(session)
    }

    private fun playPrepared(session: PlaybackSession, audio: PreparedAudio) {
        if (!isActive(session) || session.prepared != audio) return
        session.prepared = null
        session.waitingIndex = null
        session.pauseElapsed = false
        session.playingIndex = audio.index
        playFile(
            file = audio.file,
            requestGeneration = session.generation,
            onPlaybackStart = {
                if (audio.index == 0) session.onPlaybackStart?.invoke()
                if (audio.index + 1 < session.units.size) prepareUnit(session, audio.index + 1)
            },
        ) {
            session.files -= audio.file
            unitFinished(session, audio.index)
        }
    }

    private fun unitFinished(session: PlaybackSession, index: Int) {
        if (!isActive(session)) return
        session.playingIndex = null
        val nextIndex = index + 1
        if (nextIndex !in session.units.indices) {
            if (!session.queued) finishSession(session)
            return
        }
        session.waitingIndex = nextIndex
        session.pauseElapsed = false
        if (session.prepared?.index != nextIndex && session.preparingIndex != nextIndex && !session.preparationFailed) {
            prepareUnit(session, nextIndex)
        }
        if (nextIndex == session.manualContinuationIndex && !session.manualContinuationRequested) {
            session.pauseElapsed = true
            return
        }
        pauseJob?.cancel()
        pauseJob = scope.launch {
            delay(session.units[index].pauseAfterMs)
            if (!isActive(session) || session.waitingIndex != nextIndex) return@launch
            session.pauseElapsed = true
            val next = session.prepared
            when {
                next?.index == nextIndex -> playPrepared(session, next)
                session.preparationFailed -> finishSession(session)
            }
        }
    }

    private fun finishSession(session: PlaybackSession) {
        if (!session.completed.compareAndSet(false, true)) return
        if (activeSession === session) activeSession = null
        pauseJob?.cancel()
        pauseJob = null
        session.files.forEach(File::delete)
        session.files.clear()
        session.onComplete()
    }

    fun stop() {
        generation++
        cancelWatchdog()
        pauseJob?.cancel()
        pauseJob = null
        webSocket?.cancel()
        webSocket = null
        closeOutputStream(currentOutputStream)
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        activeSession?.files?.forEach(File::delete)
        activeSession?.files?.clear()
        activeSession = null
    }

    fun shutdown() {
        stop()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun startWatchdog(
        webSocket: WebSocket,
        timeoutMs: Long,
        requestGeneration: Long,
        owner: String,
        terminal: AtomicBoolean,
        onTimeout: () -> Unit,
    ) {
        cancelWatchdog(owner)
        watchdogOwner = owner
        watchdogJob = scope.launch {
            delay(timeoutMs)
            if (requestGeneration != generation || watchdogOwner != owner || terminal.get()) return@launch
            Log.w(TAG, "No Edge TTS audio received for ${timeoutMs}ms; aborting")
            if (this@EdgeTtsPlayer.webSocket === webSocket) this@EdgeTtsPlayer.webSocket = null
            watchdogOwner = null
            watchdogJob = null
            onTimeout()
        }
    }

    private fun cancelWatchdog(owner: String? = null) {
        if (owner != null && watchdogOwner != owner) return
        watchdogJob?.cancel()
        watchdogJob = null
        watchdogOwner = null
    }

    private fun playFile(
        file: File,
        requestGeneration: Long,
        onPlaybackStart: (() -> Unit)?,
        onComplete: () -> Unit,
    ) {
        mediaPlayer?.release()
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { completedPlayer ->
                    completedPlayer.release()
                    if (mediaPlayer === completedPlayer) mediaPlayer = null
                    file.delete()
                    if (requestGeneration == generation) onComplete()
                }
                setOnErrorListener { failedPlayer, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    failedPlayer.release()
                    if (mediaPlayer === failedPlayer) mediaPlayer = null
                    file.delete()
                    if (requestGeneration == generation) onComplete()
                    true
                }
                prepare()
                start()
            }
            onPlaybackStart?.invoke()
        } catch (error: Exception) {
            Log.e(TAG, "Could not play Edge TTS audio", error)
            player.release()
            if (mediaPlayer === player) mediaPlayer = null
            file.delete()
            if (requestGeneration == generation) onComplete()
        }
    }

    private fun closeOutputStream(stream: FileOutputStream?) {
        runCatching { stream?.close() }
        if (currentOutputStream === stream) currentOutputStream = null
    }

    private fun isActive(session: PlaybackSession): Boolean =
        activeSession === session && session.generation == generation && !session.completed.get()

    companion object {
        private const val TAG = "EdgeTTS"
        private const val EDGE_TTS_URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        private const val CHROMIUM_MAJOR_VERSION = "143"
        private const val WINDOWS_EPOCH_SECONDS = 11644473600L
        private const val WATCHDOG_INITIAL_MS = 30_000L
        private const val WATCHDOG_IDLE_MS = 30_000L
        private const val SECTION_PAUSE_MS = 1_000L
        private const val SPEECH_CONFIG =
            "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                "{\"context\":{\"synthesis\":{\"audio\":{\"metadataOptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-96kbitrate-mono-mp3\"}}}}"

        internal fun buildSsmlFrame(requestId: String, text: String, voice: String, speechRate: Float): String {
            val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val percent = ((speechRate.coerceIn(0.6f, 1.6f) - 1f) * 100).roundToInt()
            val rate = if (percent >= 0) "+$percent%" else "$percent%"
            val language = TtsVoiceRegistry.extractLocale(voice)
            return "X-RequestId:$requestId\r\n" +
                "Content-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$language'>" +
                "<voice name='$voice'><prosody rate='$rate'>$escaped</prosody></voice></speak>"
        }

        internal fun buildSpeechUnits(text: String, maxUtf8Bytes: Int = 3_800): List<SpeechUnit> {
            val logicalSegments = mutableListOf<String>()
            var pendingHeading: String? = null
            var skipSources = false
            parseResearchArticle(text).forEach { block ->
                when (block) {
                    is ArticleBlock.Heading -> {
                        pendingHeading?.let(logicalSegments::add)
                        skipSources = block.text.equals("Quellen", ignoreCase = true)
                        pendingHeading = block.text.takeUnless { skipSources }
                    }
                    is ArticleBlock.Paragraph -> if (!skipSources) {
                        logicalSegments += pendingHeading?.let { "$it. ${block.text}" } ?: block.text
                        pendingHeading = null
                    }
                    is ArticleBlock.Source -> Unit
                }
            }
            pendingHeading?.let(logicalSegments::add)
            val units = logicalSegments.flatMapIndexed { segmentIndex, segment ->
                val chunks = splitForTts(segment, maxUtf8Bytes)
                chunks.mapIndexed { chunkIndex, chunk ->
                    val isLastChunk = chunkIndex == chunks.lastIndex
                    val hasNextSegment = segmentIndex < logicalSegments.lastIndex
                    SpeechUnit(chunk, if (isLastChunk && hasNextSegment) SECTION_PAUSE_MS else 0L)
                }
            }
            return units.filter { it.text.isNotBlank() }
        }

        internal fun buildManualSpeechPlan(
            text: String,
            continuationText: String,
            maxUtf8Bytes: Int = 3_800,
        ): ManualSpeechPlan {
            val initial = buildSpeechUnits(text, maxUtf8Bytes).map { it.copy(pauseAfterMs = 0L) }
            val continuation = buildSpeechUnits(continuationText, maxUtf8Bytes)
            return ManualSpeechPlan(initial + continuation, initial.size)
        }

        internal fun splitForTts(text: String, maxUtf8Bytes: Int = 3_800): List<String> {
            require(maxUtf8Bytes > 0)
            val chunks = mutableListOf<String>()
            var current = StringBuilder()

            fun flush() {
                if (current.isNotEmpty()) chunks += current.toString()
                current = StringBuilder()
            }

            text.trim().split(Regex("\\s+")).filter(String::isNotEmpty).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (candidate.toByteArray(Charsets.UTF_8).size <= maxUtf8Bytes) {
                    current = StringBuilder(candidate)
                } else {
                    flush()
                    if (word.toByteArray(Charsets.UTF_8).size <= maxUtf8Bytes) {
                        current.append(word)
                    } else {
                        val codePoints = word.codePoints().iterator()
                        while (codePoints.hasNext()) {
                            val character = String(Character.toChars(codePoints.nextInt()))
                            if ((current.toString() + character).toByteArray(Charsets.UTF_8).size > maxUtf8Bytes) flush()
                            current.append(character)
                        }
                    }
                }
            }
            flush()
            return chunks
        }

        private fun generateSecMsGec(): String {
            var ticks = (System.currentTimeMillis() / 1000.0) + WINDOWS_EPOCH_SECONDS
            ticks -= ticks % 300
            ticks *= 1e7
            val bytes = "${ticks.toLong()}$TRUSTED_CLIENT_TOKEN".toByteArray(Charsets.US_ASCII)
            return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        }

        private fun randomId(): String = UUID.randomUUID().toString().replace("-", "")
        private fun generateMuid(): String = randomId().uppercase()
    }
}
