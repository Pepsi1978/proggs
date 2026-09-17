package de.frank.wecker

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.genialeideen.audio.*
import de.frank.genialeideen.auth.*
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.tts.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

class WeckerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    val store = AlarmStore.get(application)
    val settings = SecureSettings(application)
    val alarms = store.alarms
    val theme = settings.themeFlow
    val scheduler = AlarmScheduler(application)
    val auth = CodexAuthManager(application)
    private val recorder = MicRecorder(application)
    private var preview: MediaPlayer? = null
    private val _draft = MutableStateFlow(store.prefs.getString("draft", null)?.let { runCatching { Alarm.from(JSONObject(it)) }.getOrNull() })
    private var draftIsNew = _draft.value?.let { store.get(it.id) == null } ?: true
    val isNewDraft: Boolean get() = draftIsNew
    val draft = _draft.asStateFlow()
    val busy = MutableStateFlow("")
    val audioBusy = MutableStateFlow("")
    private val preparationJobs = mutableMapOf<String, Job>()
    private val preparationProgress = mutableMapOf<String, String>()
    val message = MutableStateFlow("")
    val recording = MutableStateFlow(false)
    val voiceRecording = MutableStateFlow(false)
    val clonedVoices = MutableStateFlow<List<ClonedVoice>>(emptyList())
    val voicesLoading = MutableStateFlow(false)
    val voiceLoadError = MutableStateFlow("")
    private var voiceJob: Job? = null
    private var requestedVoiceAccount = ""
    private var voiceGeneration = 0
    val settingsRevision = MutableStateFlow(0)
    val loginCode = MutableStateFlow<DeviceAuthInfo?>(null)
    val ideas = MutableStateFlow(IdeasBridge(application).cached())
    private var voiceSample: ByteArray? = null
    val hasVoiceSample = MutableStateFlow(false)
    private var actionJob: Job? = null

    init {
        if (settings.ttsProvider == TtsProvider.QWEN.id) settings.ttsProvider = TtsProvider.QWEN_CLONE.id
        restoreVoiceCache()
        loadVoices()
        scheduler.restore()
        viewModelScope.launch(Dispatchers.IO) { Tones.names.keys.forEach { Tones.file(store.files, it) } }
        PreparationWorker.enqueue(app)
    }
    fun newAlarm() = edit(Alarm(id = UUID.randomUUID().toString()))
    fun edit(alarm: Alarm) {
        stopPreview()
        draftIsNew = store.get(alarm.id) == null
        _draft.value = alarm
        persistDraft(alarm)
    }
    fun change(alarm: Alarm) {
        if (_draft.value?.id != alarm.id) {
            android.util.Log.w("WeckerEditor", "Veraltetes Bearbeitungsereignis verworfen")
            return
        }
        _draft.value = alarm
        persistDraft(alarm)
    }
    private fun persistDraft(alarm: Alarm) { store.prefs.edit().putString("draft", alarm.json().toString()).putBoolean("draft_is_new", draftIsNew).apply() }
    fun closeEditor() { _draft.value = null; store.prefs.edit().remove("draft").remove("draft_is_new").apply() }
    fun runAction(label: String, silent: Boolean = false, action: suspend () -> Unit) {
        if (actionJob?.isActive == true) { message.value = "Bitte warte auf den laufenden Vorgang."; return }
        actionJob = viewModelScope.launch {
            if (!silent) busy.value = label
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message.value = e.message ?: "Der Vorgang ist fehlgeschlagen." }
            finally { busy.value = "" }
        }
    }
    fun cancelAction() {
        if (actionJob?.isActive == true) actionJob?.cancel() else preparationJobs.values.toList().forEach { it.cancel() }
        auth.cancelChat(); auth.cancelLogin()
    }
    fun save(done: () -> Unit) {
        val source = _draft.value ?: return
        val alarm = source.copy(enabled = true)
        val create = draftIsNew
        runAction("Wecker speichern …") {
            withContext(Dispatchers.IO) { scheduler.save(alarm, create = create) }
            if (_draft.value == source) { closeEditor(); done() }
            else if (_draft.value?.id == alarm.id && create) {
                draftIsNew = false
                _draft.value?.let(::persistDraft)
            }
            message.value = "${alarm.name} gespeichert und aktiviert."
            if (alarm.needsSpeech) {
                prepare(store.get(alarm.id)!!)
            }
        }
    }
    fun toggle(alarm: Alarm, enabled: Boolean) = runAction("Weckzeit ändern …", silent = true) {
        withContext(Dispatchers.IO) { scheduler.save(alarm.copy(enabled = enabled)) }
        if (enabled && alarm.needsSpeech) PreparationWorker.enqueue(app)
    }
    fun delete(alarm: Alarm) = runAction("Wecker löschen …") {
        preparationJobs[alarm.id]?.cancelAndJoin()
        withContext(Dispatchers.IO) {
            require(alarm.id !in store.ringing()) { "Stoppe zuerst den klingelnden Wecker." }
            scheduler.cancel(alarm.id); store.delete(alarm.id)
        }
    }
    fun skip(alarm: Alarm) = runAction("Nächste Weckzeit auslassen …") {
        require(alarm.repeats) { "Einmalige Wecker kannst du ausschalten." }
        withContext(Dispatchers.IO) {
            val updated = alarm.copy(nextAt = AlarmTime.next(alarm, java.time.Instant.ofEpochMilli(alarm.nextAt + 1000)))
            store.put(updated); scheduler.schedule(updated)
        }
    }
    fun prepare(alarm: Alarm) {
        val previous = preparationJobs[alarm.id]
        preparationJobs[alarm.id] = viewModelScope.launch {
            fun progress(text: String?) {
                if (text == null) preparationProgress.remove(alarm.id) else preparationProgress[alarm.id] = text
                audioBusy.value = preparationProgress.values.firstOrNull().orEmpty()
            }
            try {
                previous?.cancelAndJoin()
                progress("${alarm.name}: Audio-Vorbereitung …")
                SpeechPreparation(app, settings).prepare(alarm) { text -> withContext(Dispatchers.Main) { progress("${alarm.name}: $text") } }
                ideas.value = IdeasBridge(app).cached()
                if (store.get(alarm.id)?.let { it.sameSpeechAs(alarm) && it.voiceVariants.size == VoiceVariations.COUNT } == true)
                    message.value = "${alarm.name}: Die Sprachvarianten sind offline bereit."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message.value = "${alarm.name}: ${e.message ?: "Audio-Vorbereitung fehlgeschlagen"}" }
            finally {
                if (preparationJobs[alarm.id] === coroutineContext[Job]) {
                    progress(null); preparationJobs.remove(alarm.id)
                }
            }
        }
    }
    fun syncIdeas() = runAction("Offene Ideen lesen …") {
        ideas.value = IdeasBridge(app).refresh()
        message.value = "${ideas.value.size} offene Ideen in Originalreihenfolge übernommen."
        PreparationWorker.enqueue(app)
    }
    fun importSettings() = runAction("Spracheinstellungen übernehmen …") {
        IdeasBridge(app).copySettings(settings)
        settingsChanged()
        message.value = "Stimme, Sprechtempo und Sprachschlüssel aus Geniale Ideen übernommen."
    }
    fun settingsChanged() {
        if (settings.ttsProvider == TtsProvider.QWEN.id) settings.ttsProvider = TtsProvider.QWEN_CLONE.id
        settingsRevision.value++
        if (requestedVoiceAccount != voiceAccount()) { restoreVoiceCache(); loadVoices(force = true) }
        PreparationWorker.enqueue(app)
    }
    fun importMusic(uri: Uri) = runAction("Song vollständig auf dem Gerät speichern …") {
        val alarmId = _draft.value?.id ?: return@runAction
        val result = withContext(Dispatchers.IO) {
            val name = app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: "Eigener Song"
            val file = File(store.files, "music_${UUID.randomUUID()}")
            try {
                app.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    ?: error("Der Song konnte nicht geöffnet werden.")
                require(file.length() > 0) { "Die Musikdatei ist leer." }
                val metadata = MediaMetadataRetriever()
                try {
                    metadata.setDataSource(file.absolutePath)
                    require(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "Diese Datei enthält keine abspielbare Audiospur." }
                } finally { metadata.release() }
                file.absolutePath to name
            } catch (e: Exception) { file.delete(); throw e }
        }
        _draft.value?.takeIf { it.id == alarmId }?.let { change(it.copy(music = result.first, musicName = result.second)) }
    }
    fun referencePhoto(file: File) = runAction("Referenzfoto prüfen …") {
        val alarm = _draft.value ?: return@runAction
        val saved = withContext(Dispatchers.IO) {
            val bitmap = PhotoCheck.bitmap(file)
            val saved = File(store.files, "photo_${UUID.randomUUID()}.jpg")
            saved.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            val result = PhotoCheck.check(saved, alarm.copy(reference = saved.absolutePath, photoTolerance = 75, color = "none", minBrightness = 0))
            require(result.accepted) { "Wähle ein gut beleuchtetes Motiv mit erkennbaren Strukturen statt einer leeren Wand." }
            saved
        }
        _draft.value?.takeIf { it.id == alarm.id }?.let { change(it.copy(reference = saved.absolutePath, photoRequired = true)) }
    }
    fun startRecording(forVoice: Boolean = false) {
        if (recording.value) return
        if (recorder.start(viewModelScope)) { recording.value = true; voiceRecording.value = forVoice }
        else message.value = "Das Mikrofon konnte nicht gestartet werden. Prüfe die Berechtigung."
    }
    fun stopRecording() {
        if (!recording.value) return
        val forVoice = voiceRecording.value
        recording.value = false
        val alarmId = _draft.value?.id
        runAction(if (forVoice) "Stimmprobe speichern …" else "Diktat mit Groq transkribieren …") {
            val bytes = recorder.stop() ?: error("Die Aufnahme war leer.")
            if (forVoice) { voiceSample = bytes; hasVoiceSample.value = true; message.value = "Stimmprobe aufgenommen." }
            else {
                withContext(Dispatchers.IO) { File(app.filesDir, "letztes_diktat.wav").writeBytes(bytes) }
                val transcriber = GroqTranscriber(settings.groqApiKey, filterStille = settings.filterStilleVorabAn,
                    filterMetriken = settings.filterSegmentmetrikenAn, filterZeitstempel = settings.filterZeitstempelAn,
                    filterFloskeln = settings.filterFloskelnAn)
                val result = try { Diktat(transcriber).transkribiere(bytes) } finally { transcriber.shutdown() }
                _draft.value?.takeIf { it.id == alarmId }?.let { change(it.copy(text = (it.text + "\n" + result.text).trim())) }
                message.value = if (result.teileFehlend == 0) "Diktat eingefügt." else "${result.teileFehlend} Abschnitte fehlen. Die Originalaufnahme bleibt gespeichert."
            }
        }
    }
    fun improve() {
        val source = _draft.value ?: return
        if (source.text.isBlank()) { message.value = "Gib zuerst einen Text ein."; return }
        runAction("Text verbessern …") {
            val result = auth.streamChat(
                "Bring den folgenden diktierten Text in gutes Deutsch: Füllwörter raus, Satzzeichen und Absätze rein, Versprecher bereinigen. Verändere den Inhalt nicht, erfinde nichts hinzu und lass nichts weg. Gib nur den geglätteten Text zurück, ohne Vorrede. Nutze echte Umlaute.",
                listOf(ChatTurn("user", source.text)), CodexModel.fromLabel(settings.model), ReasoningEffort.fromLabel(settings.reasoning))
            _draft.value?.takeIf { it.id == source.id && it.text == source.text }?.let {
                change(it.copy(text = result, originalText = source.text))
            }
        }
    }
    fun login(activity: ComponentActivity) = runAction("ChatGPT-Anmeldung …") {
        try {
            auth.login(activity) { loginCode.value = it }
            message.value = "ChatGPT ist verbunden."
            settingsRevision.value++
        } finally { loginCode.value = null }
    }
    private fun voiceAccount() = SpeechPreparation.hash(settings.qwenTtsApiKey.filterNot(Char::isWhitespace))
    private fun restoreVoiceCache() {
        clonedVoices.value = runCatching {
            val cache = JSONObject(settings.clonedVoiceCache)
            if (cache.optString("account") != voiceAccount()) emptyList() else {
                val array = cache.getJSONArray("voices")
                (0 until array.length()).map { array.getJSONObject(it).let { j -> ClonedVoice(j.getString("id"), j.getString("name"), j.optString("created")) } }
            }
        }.getOrDefault(emptyList())
    }
    fun loadVoices(force: Boolean = false) {
        if (settings.qwenTtsApiKey.isBlank()) {
            voiceLoadError.value = "Trage deinen Alibaba-Schlüssel ein, um deine hochgeladenen Stimmen zu laden."
            return
        }
        if (voiceJob?.isActive == true && !force) return
        val generation = ++voiceGeneration
        requestedVoiceAccount = voiceAccount()
        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            voicesLoading.value = true; voiceLoadError.value = ""
            try { loadVoicesNow() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                voiceLoadError.value = e.message ?: "Eigene Stimmen konnten nicht geladen werden."
                android.util.Log.w("WeckerVoices", "Stimmenliste nicht erreichbar", e)
            } finally { if (generation == voiceGeneration) voicesLoading.value = false }
        }
    }
    private suspend fun loadVoicesNow() {
        val key = settings.qwenTtsApiKey
        val account = voiceAccount()
        val directory = QwenVoiceDirectory()
        try {
            val voices = directory.list(key)
            if (key != settings.qwenTtsApiKey) return
            clonedVoices.value = voices
            settings.clonedVoiceCache = JSONObject().put("account", account).put("voices", org.json.JSONArray(voices.map {
                JSONObject().put("id", it.id).put("name", it.name).put("created", it.createdAt)
            })).toString()
            if (voices.isEmpty()) voiceLoadError.value = "In diesem Alibaba-Konto wurden keine eigenen Stimmen gefunden."
            settingsRevision.value++
        } finally { directory.shutdown() }
    }
    fun createVoice(name: String) = runAction("Eigene Stimme anlegen …") {
        require(name.isNotBlank()) { "Gib der Stimme einen Namen." }
        val sample = voiceSample ?: error("Nimm zuerst die Stimmprobe auf.")
        val enrollment = QwenVoiceEnrollment()
        val id = try { enrollment.create(settings.qwenTtsApiKey, name, sample) } finally { enrollment.shutdown() }
        settings.qwenVoiceNames = settings.qwenVoiceNames + (id to name)
        settings.qwenTtsVoiceId = id; settings.ttsProvider = TtsProvider.QWEN_CLONE.id
        settingsChanged(); loadVoicesNow(); message.value = "Deine Stimme ist angelegt."
    }
    fun removeVoice(id: String) = runAction("Eigene Stimme löschen …") {
        val enrollment = QwenVoiceEnrollment()
        try { enrollment.delete(settings.qwenTtsApiKey, id) } finally { enrollment.shutdown() }
        if (settings.qwenTtsVoiceId == id) { settings.qwenTtsVoiceId = ""; settings.ttsProvider = TtsProvider.EDGE.id }
        settingsChanged(); loadVoicesNow()
    }
    fun previewVoice() = runAction("Stimmprobe vorbereiten …") {
        val prep = SpeechPreparation(app, settings)
        val file = prep.audio("Guten Morgen! Es ist Zeit für deine genialen Ideen. Dein Wecker ist bereit.")
        playPreview(file, prep.playbackSpeed)
    }
    fun playPreview(file: File, speed: Float = 1f) {
        stopPreview()
        preview = MediaPlayer().apply {
            setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
            setDataSource(file.absolutePath)
            setOnPreparedListener { it.playbackParams = android.media.PlaybackParams().setSpeed(speed); it.start() }
            setOnCompletionListener { stopPreview() }
            setOnErrorListener { _, _, _ -> message.value = "Die Vorschau konnte nicht abgespielt werden."; stopPreview(); true }
            prepareAsync()
        }
    }
    fun stopPreview() { preview?.release(); preview = null }
    fun test(alarm: Alarm) {
        stopPreview()
        app.startForegroundService(Intent(app, AlarmService::class.java).setAction("TEST").putExtra("id", alarm.id))
    }
    override fun onCleared() {
        recorder.release(); stopPreview(); settings.close(); auth.cancelLogin(); auth.cancelChat()
        super.onCleared()
    }
}
