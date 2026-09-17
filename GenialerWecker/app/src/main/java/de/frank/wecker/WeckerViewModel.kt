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

enum class RecordingKind { DIKTAT, STIMMPROBE }
data class RecordingSession(val kind: RecordingKind, val draftId: String?, val draftName: String, val editorGeneration: Long, val startedAt: Long)
/** One saved-and-closed editor; [generation] makes every save a distinct event. */
data class SavedEvent(val id: String, val generation: Long)

data class OpenDictation(val draftId: String, val draftName: String, val text: String, val missing: Int, val createdAt: Long = 0) {
    fun json(): String = JSONObject().put("v", 1).put("draftId", draftId).put("draftName", draftName)
        .put("text", text).put("missing", missing).put("createdAt", createdAt).toString()
    companion object {
        const val KEY = "open_dictation"
        const val UNREADABLE_KEY = "open_dictation_unreadable"
        /** Strict: anything unexpected throws, so a damaged entry is kept aside instead of being half-loaded. */
        fun parse(raw: String): OpenDictation {
            val j = JSONObject(raw)
            require(j.getInt("v") == 1) { "Unbekannte Version" }
            val value = OpenDictation(j.getString("draftId"), j.getString("draftName"), j.getString("text"), j.getInt("missing"), j.getLong("createdAt"))
            require(value.draftId.isNotBlank() && value.text.isNotBlank() && value.missing >= 0) { "Ungültiges offenes Diktat" }
            return value
        }
    }
}

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
    /** Stand beim Öffnen des Editors; nur echte Änderungen lösen die Rückfrage beim Verlassen aus. */
    private var draftBaseline = store.prefs.getString("draft_base", null)
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
    /** Time of the last successful sync; separate from [ideas], which suppresses identical lists. */
    val ideasAt = MutableStateFlow(store.prefs.getLong("ideasAt", 0))
    private var voiceSample: ByteArray? = null
    val hasVoiceSample = MutableStateFlow(false)
    private var actionJob: Job? = null
    /** Owner of the busy banner; a cancelled older action must not clear the banner of a newer one. */
    private var busyOwner: Any? = null
    /** Incremented by every stop or new preview; late preparations, callbacks and errors of older generations are dropped. */
    private var previewGeneration = 0L
    /** Only the preview's own preparation job, never a foreign action job. */
    private var previewJob: Job? = null
    /** Recording currently running; bound to kind and original editor session at start. */
    val recordingSession = MutableStateFlow<RecordingSession?>(null)
    /** Processing text after a recording, e.g. while stopping or transcribing. */
    val recordingStatus = MutableStateFlow("")
    val openDictation = MutableStateFlow<OpenDictation?>(null)
    /** Confirmation shown on the saved card instead of a success banner. */
    val lastSaved = MutableStateFlow<SavedEvent?>(null)
    private var savedGeneration = 0L
    val recordingLevel = recorder.pegel
    private var stopping = false
    private var dictationJob: Job? = null
    /** Increments per transcription and on cancel; guards against late results of an older job. */
    private var dictationGeneration = 0L
    /** Whether the WAV of the current transcription was saved successfully. */
    private var dictationSaved = false
    /** Changes whenever an editor is opened or closed; the same alarm id reopened is a new editor session. */
    private var editorGeneration = 0L

    init {
        if (settings.ttsProvider == TtsProvider.QWEN.id) settings.ttsProvider = TtsProvider.QWEN_CLONE.id
        restoreVoiceCache()
        loadVoices()
        scheduler.restore()
        restoreOpenDictation()
        viewModelScope.launch(Dispatchers.IO) { Tones.names.keys.forEach { Tones.file(store.files, it) } }
        PreparationWorker.enqueue(app)
    }
    fun newAlarm() = edit(Alarm(id = UUID.randomUUID().toString()))
    fun edit(alarm: Alarm) {
        stopPreview()
        stopDictationForEditorChange()
        editorGeneration++
        draftIsNew = store.get(alarm.id) == null
        _draft.value = alarm
        draftBaseline = if (draftIsNew && alarm.name.endsWith("– Kopie")) null else alarm.json().toString()
        store.prefs.edit().putString("draft_base", draftBaseline).apply()
        persistDraft(alarm)
    }
    /**
     * New copies always count as unsaved (no baseline); otherwise any difference from the opened state counts.
     * Compared as objects, so fields added by an update (e.g. sleepMinutes) do not mark an untouched draft as changed.
     * A missing or unreadable baseline counts conservatively as changed.
     */
    fun draftChanged(): Boolean {
        val current = _draft.value ?: return false
        val baseline = draftBaseline?.let { runCatching { Alarm.from(JSONObject(it)) }.getOrNull() } ?: return true
        return current != baseline
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
    private fun stopDictationForEditorChange() { if (recordingSession.value?.kind == RecordingKind.DIKTAT) stopRecording() }
    fun closeEditor() { stopDictationForEditorChange(); editorGeneration++; _draft.value = null; draftBaseline = null; store.prefs.edit().remove("draft").remove("draft_is_new").remove("draft_base").apply() }
    fun runAction(label: String, silent: Boolean = false, action: suspend () -> Unit) {
        if (actionJob?.isActive == true) { message.value = "Bitte warte auf den laufenden Vorgang."; return }
        val owner = Any()
        busyOwner = owner
        // Owner is set before launch, so even a synchronously finishing action (Main.immediate) clears only its own banner.
        actionJob = viewModelScope.launch {
            // A silent action also clears a stale banner left by a cancelled older action.
            busy.value = if (silent) "" else label
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message.value = e.message ?: "Der Vorgang ist fehlgeschlagen." }
            finally { if (busyOwner === owner) { busy.value = ""; busyOwner = null } }
        }
    }
    fun cancelAction() {
        if (actionJob?.isActive == true) actionJob?.cancel() else preparationJobs.values.toList().forEach { it.cancel() }
        auth.cancelChat(); auth.cancelLogin()
    }
    fun save(notificationsDenied: Boolean = false, done: () -> Unit) {
        val source = _draft.value ?: return
        val alarm = source.copy(enabled = true)
        val create = draftIsNew
        runAction("Wecker speichern …") {
            val planned = try { withContext(Dispatchers.IO) { scheduler.save(alarm, create = create) } }
            finally {
                // Once stored, a retry must update the entry instead of failing on the existing id.
                if (create && store.get(alarm.id) != null && _draft.value?.id == alarm.id) { draftIsNew = false; _draft.value?.let(::persistDraft) }
            }
            val closed = _draft.value == source
            if (closed) { closeEditor(); done() }
            val nextAt = store.get(alarm.id)?.nextAt ?: 0
            when {
                // Real warnings stay visible as messages.
                !planned -> message.value = "${alarm.name} gespeichert, aber nicht geplant: ${store.issues.value[alarm.id].orEmpty()}"
                notificationsDenied -> message.value = "${alarm.name} gespeichert. Ohne Benachrichtigungen fehlen Vollbild und Sperrbildschirm-Tasten – bitte in den Einstellungen erlauben."
                // Success with the unchanged source draft closed: the list confirms on the card itself, no banner.
                closed -> lastSaved.value = SavedEvent(alarm.id, ++savedGeneration)
                // Editor stays open because the draft changed meanwhile: there is no card to show, so keep the message.
                nextAt > 0 -> message.value = "${alarm.name} gespeichert · klingelt ${formatAt(nextAt)} (in ${remaining(nextAt - System.currentTimeMillis())})."
                else -> message.value = "${alarm.name} gespeichert und aktiviert."
            }
            if (alarm.needsSpeech) {
                prepare(store.get(alarm.id)!!)
            }
        }
    }
    /** Clears only the event of this generation; an older confirmation ending never removes a newer one. */
    fun consumeSaved(generation: Long) { if (lastSaved.value?.generation == generation) lastSaved.value = null }
    fun toggle(alarm: Alarm, enabled: Boolean) = runAction("Weckzeit ändern …", silent = true) {
        val planned = withContext(Dispatchers.IO) { scheduler.setEnabled(alarm.id, enabled) }
        // Success stays silent: the next-alarm card already shows date, time and remaining time.
        if (!planned) message.value = "${alarm.name}: ${store.issues.value[alarm.id] ?: "Die Weckzeit konnte nicht geplant werden."}"
        if (enabled && alarm.needsSpeech) PreparationWorker.enqueue(app)
    }
    fun delete(alarm: Alarm) = runAction("Wecker löschen …") {
        preparationJobs[alarm.id]?.cancelAndJoin()
        withContext(Dispatchers.IO) {
            require(alarm.id !in store.ringing()) { "Stoppe zuerst den klingelnden Wecker." }
            // Delete from the store first: a parallel reminder sync then sees the alarm as gone and cannot plan it again.
            store.delete(alarm.id); scheduler.cancel(alarm.id)
            SnoozeNotice.cancel(app, alarm.id)
        }
        if (_draft.value?.id == alarm.id) closeEditor()
        message.value = "„${alarm.name}“ gelöscht."
    }
    fun skip(alarm: Alarm) = runAction("Nächste Weckzeit auslassen …") {
        val (updated, planned) = withContext(Dispatchers.IO) { scheduler.skipNext(alarm.id) }
        message.value = if (planned) "Nächster Termin ausgelassen. ${updated.name} klingelt wieder ${formatAt(updated.nextAt)}."
            else "${updated.name}: ${store.issues.value[alarm.id] ?: "Die Weckzeit konnte nicht geplant werden."}"
    }
    fun unskip(alarm: Alarm) = runAction("Auslassen rückgängig machen …") {
        val (updated, planned) = withContext(Dispatchers.IO) { scheduler.unskip(alarm.id) }
        message.value = if (planned) "${updated.name} klingelt wieder ${formatAt(updated.nextAt)}."
            else "${updated.name}: ${store.issues.value[alarm.id] ?: "Die Weckzeit konnte nicht geplant werden."}"
    }
    fun endSnooze(alarm: Alarm) = runAction("Schlummerpause beenden …", silent = true) {
        withContext(Dispatchers.IO) { scheduler.endSnooze(alarm.id) }
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
                // Preparation may have synced the ideas as well.
                ideasAt.value = store.prefs.getLong("ideasAt", 0)
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
        ideasAt.value = store.prefs.getLong("ideasAt", 0)
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
        val kind = if (forVoice) RecordingKind.STIMMPROBE else RecordingKind.DIKTAT
        // Start stays blocked while anything of an earlier recording is unfinished, so no result is overwritten silently.
        when {
            recordingSession.value != null || stopping -> return
            actionJob?.isActive == true -> { message.value = "Bitte warte auf den laufenden Vorgang."; return }
            dictationJob?.isActive == true -> { message.value = "Das letzte Diktat wird noch transkribiert."; return }
            kind == RecordingKind.DIKTAT && openDictation.value != null ->
                { message.value = "Füge zuerst das offene Diktat ein oder verwirf es."; return }
        }
        val draft = _draft.value
        if (kind == RecordingKind.DIKTAT && draft == null) return
        // The speaker must not be recorded: end our own audio preview before the microphone starts.
        stopPreview()
        if (!recorder.start(viewModelScope)) { message.value = "Das Mikrofon konnte nicht gestartet werden. Prüfe die Berechtigung."; return }
        // Kind and original draft (including the editor session) are bound at start, never read again at stop.
        recordingSession.value = RecordingSession(kind, draft?.id, draft?.name.orEmpty(), editorGeneration, System.currentTimeMillis())
        recording.value = true; voiceRecording.value = forVoice
    }

    /** Hardware stop independent of any other action. A second call while stopping does nothing. */
    fun stopRecording() {
        val session = recordingSession.value ?: return
        if (stopping) return
        stopping = true
        recordingStatus.value = "Aufnahme wird beendet …"
        // UNDISPATCHED: recorder.stop() releases the AudioRecord before its first suspension, i.e. within this call.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var bytes: ByteArray? = null
            try { bytes = recorder.stop() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { android.util.Log.w("WeckerRecording", "Aufnahme nicht sauber beendet", e) }
            finally {
                // Only now the recording is really over; the state never claims recording while it is not.
                recordingSession.value = null
                recording.value = false; voiceRecording.value = false
                stopping = false
                if (recordingStatus.value == "Aufnahme wird beendet …") recordingStatus.value = ""
            }
            val data = bytes
            when {
                data == null -> message.value = "Die Aufnahme war leer."
                session.kind == RecordingKind.STIMMPROBE -> { voiceSample = data; hasVoiceSample.value = true; message.value = "Stimmprobe aufgenommen." }
                else -> transcribe(session, data)
            }
        }
    }

    /**
     * Own job, parallel to other actions; only [cancelDictation] cancels it. The WAV is saved first and not cancellable,
     * so every later message can state truthfully whether the recording is kept.
     */
    private fun transcribe(session: RecordingSession, bytes: ByteArray) {
        val generation = ++dictationGeneration
        dictationSaved = false
        recordingStatus.value = "Diktat wird gesichert …"
        dictationJob = viewModelScope.launch {
            val job = currentCoroutineContext()[Job]
            // Owner check: after cancelDictation or a newer dictation, this job may neither deliver nor report.
            fun owns() = dictationGeneration == generation && dictationJob === job
            val saved = try {
                withContext(NonCancellable + Dispatchers.IO) { File(app.filesDir, "letztes_diktat.wav").writeBytes(bytes) }
                true
            } catch (e: Exception) {
                android.util.Log.w("WeckerRecording", "Diktat-WAV nicht gespeichert", e)
                false
            }
            if (owns()) { dictationSaved = saved; recordingStatus.value = "Diktat wird transkribiert …" }
            val kept = if (saved) " Die Aufnahme liegt als letztes Diktat vor." else " Die Aufnahme konnte nicht gesichert werden."
            try {
                currentCoroutineContext().ensureActive()
                val transcriber = GroqTranscriber(settings.groqApiKey, filterStille = settings.filterStilleVorabAn,
                    filterMetriken = settings.filterSegmentmetrikenAn, filterZeitstempel = settings.filterZeitstempelAn,
                    filterFloskeln = settings.filterFloskelnAn)
                val result = try { Diktat(transcriber).transkribiere(bytes) } finally { transcriber.shutdown() }
                // A late, non-cooperative network result of a cancelled or replaced job is dropped silently.
                currentCoroutineContext().ensureActive()
                if (!owns()) return@launch
                deliverDictation(session, result.text, result.teileFehlend, saved)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (owns()) message.value = "Das Diktat konnte nicht transkribiert werden (${e.message ?: e.javaClass.simpleName}).$kept"
            } finally {
                if (owns()) { dictationJob = null; recordingStatus.value = "" }
            }
        }
    }

    fun cancelDictation() {
        val job = dictationJob ?: return
        dictationGeneration++
        dictationJob = null
        recordingStatus.value = ""
        job.cancel()
        message.value = "Transkription abgebrochen." + if (dictationSaved) " Die Aufnahme liegt als letztes Diktat vor." else " Die Aufnahme konnte nicht gesichert werden."
    }

    /**
     * Automatic insert only into the very editor session it was recorded in, and only while the text step is still
     * active; otherwise it waits as an open dictation for a conscious insert.
     */
    private fun deliverDictation(session: RecordingSession, text: String, missing: Int, saved: Boolean) {
        if (text.isBlank()) { message.value = "Im Diktat wurde kein Text erkannt." + if (saved) " Die Aufnahme liegt als letztes Diktat vor." else ""; return }
        val incomplete = if (missing > 0) " $missing Abschnitte fehlen." + if (saved) " Die Originalaufnahme bleibt gespeichert." else "" else ""
        val draft = _draft.value
        val sameEditor = draft != null && draft.id == session.draftId && editorGeneration == session.editorGeneration
        if (sameEditor && draft != null && Step.TEXT in draft.steps) {
            change(draft.copy(text = (draft.text + "\n" + text).trim()))
            message.value = "Diktat eingefügt.$incomplete"
            return
        }
        val saved = persistOpenDictation(OpenDictation(session.draftId.orEmpty(), session.draftName, text, missing, System.currentTimeMillis()))
        message.value = (if (saved) "" else "Das offene Diktat konnte nicht dauerhaft gesichert werden. ") + (if (sameEditor) "Der Baustein „Eigener Text“ ist nicht mehr aktiv. Das Diktat wartet oben im Editor zum bewussten Einfügen."
            else "Das Diktat für „${session.draftName}“ wurde nicht eingefügt, weil dieser Entwurf nicht mehr offen ist. Öffne den Wecker, um es bewusst einzufügen.") + incomplete
    }

    /** Keeps the open dictation in memory in any case; returns whether it was also stored durably. */
    private fun persistOpenDictation(value: OpenDictation): Boolean {
        openDictation.value = value
        return runCatching { store.prefs.edit().putString(OpenDictation.KEY, value.json()).commit() }
            .onFailure { android.util.Log.w("WeckerRecording", "Offenes Diktat nicht gesichert", it) }.getOrDefault(false)
    }

    /** Restores a stored open dictation for a conscious insert; a damaged entry is moved aside, never deleted. */
    private fun restoreOpenDictation() {
        val raw = store.prefs.getString(OpenDictation.KEY, null) ?: return
        try { openDictation.value = OpenDictation.parse(raw) }
        catch (e: Exception) {
            android.util.Log.w("WeckerRecording", "Gesichertes Diktat unlesbar; wird aufbewahrt", e)
            val edit = store.prefs.edit().remove(OpenDictation.KEY)
            if (!store.prefs.contains(OpenDictation.UNREADABLE_KEY)) edit.putString(OpenDictation.UNREADABLE_KEY, raw)
            else edit.putString(OpenDictation.UNREADABLE_KEY + "_" + System.currentTimeMillis(), raw)
            val moved = runCatching { edit.commit() }.getOrDefault(false)
            message.value = if (moved) "Ein gesichertes Diktat war unlesbar. Es wurde zur Prüfung aufbewahrt, nicht gelöscht."
                else "Ein gesichertes Diktat ist unlesbar und bleibt unverändert gespeichert."
        }
    }

    /**
     * Conscious insert into the matching open draft; adds the text step so the text is actually used.
     * Draft and removal of the open dictation are written in ONE commit; only then both flows change.
     */
    fun insertOpenDictation() {
        val open = openDictation.value ?: return
        val draft = _draft.value?.takeIf { it.id == open.draftId } ?: return
        val updated = draft.copy(text = (draft.text + "\n" + open.text).trim(), steps = if (Step.TEXT in draft.steps) draft.steps else draft.steps + Step.TEXT)
        val written = runCatching {
            store.prefs.edit().putString("draft", updated.json().toString()).putBoolean("draft_is_new", draftIsNew)
                .remove(OpenDictation.KEY).commit()
        }.getOrDefault(false)
        if (!written) { message.value = "Das Diktat konnte nicht eingefügt werden, weil der Speicher nicht geschrieben werden konnte. Es bleibt offen."; return }
        _draft.value = updated
        openDictation.value = null
        message.value = if (open.missing > 0) "Diktat eingefügt, aber unvollständig: ${open.missing} Abschnitte fehlen." else "Diktat eingefügt."
    }

    /** Removes the open dictation only after the removal was stored. */
    fun discardOpenDictation() {
        if (openDictation.value == null) return
        val removed = runCatching { store.prefs.edit().remove(OpenDictation.KEY).commit() }.getOrDefault(false)
        if (!removed) { message.value = "Das offene Diktat konnte nicht verworfen werden. Es bleibt erhalten."; return }
        openDictation.value = null
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
    /** No preview while the microphone records; nothing else is cancelled for that. */
    private fun rejectPreviewWhileRecording(): Boolean {
        if (recordingSession.value == null && !stopping) return false
        message.value = "Während einer Aufnahme ist keine Vorschau möglich."
        return true
    }
    fun previewVoice() {
        if (rejectPreviewWhileRecording()) return
        stopPreview()
        val generation = previewGeneration
        runAction("Stimmprobe vorbereiten …") {
            // The job is captured inside the action, so a refused runAction can never register a foreign job.
            val job = currentCoroutineContext()[Job]
            if (generation != previewGeneration) return@runAction
            previewJob = job
            try {
                val prep = SpeechPreparation(app, settings)
                val file = prep.audio("Guten Morgen! Es ist Zeit für deine genialen Ideen. Dein Wecker ist bereit.")
                if (generation != previewGeneration) return@runAction
                // Hand over without stopPreview(): that would cancel this very job.
                if (previewJob === job) previewJob = null
                startPlayer(file, prep.playbackSpeed, generation)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // An error of an outdated preview is dropped instead of overwriting newer messages.
                if (generation == previewGeneration) throw e
            } finally { if (previewJob === job) previewJob = null }
        }
    }
    /** Plays a built-in tone; creating the file runs on IO, playback on Main, both bound to the current generation. */
    fun playTone(id: String) {
        if (rejectPreviewWhileRecording()) return
        stopPreview()
        val generation = previewGeneration
        previewJob = viewModelScope.launch {
            val job = currentCoroutineContext()[Job]
            try {
                val file = try { withContext(Dispatchers.IO) { Tones.file(store.files, id) } }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    android.util.Log.w("WeckerPreview", "Ton konnte nicht vorbereitet werden", e)
                    if (generation == previewGeneration) message.value = "Der Ton konnte nicht vorbereitet werden."
                    return@launch
                }
                if (generation != previewGeneration) return@launch
                if (previewJob === job) previewJob = null
                startPlayer(file, 1f, generation)
            } finally {
                // Released only by its own identity, also after file errors or cancellation.
                if (previewJob === job) previewJob = null
            }
        }
    }
    /** Local player until it is prepared successfully; every failure releases it and never throws into the UI. */
    private fun startPlayer(file: File, speed: Float, generation: Long) {
        releasePlayer()
        // Construction can fail natively; that must not escape as an unhandled coroutine exception.
        val player = try { MediaPlayer() } catch (e: Exception) {
            android.util.Log.w("WeckerPreview", "MediaPlayer konnte nicht erzeugt werden", e)
            if (generation == previewGeneration) message.value = "Die Vorschau konnte nicht abgespielt werden."
            return
        }
        try {
            player.setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { prepared ->
                if (preview !== prepared || generation != previewGeneration) return@setOnPreparedListener
                // Tempo is optional: if the device rejects it, play at normal speed.
                try { prepared.playbackParams = android.media.PlaybackParams().setSpeed(speed) }
                catch (e: Exception) { android.util.Log.w("WeckerPreview", "Tempo nicht unterstützt", e) }
                // No second start after a real start failure.
                try { if (!prepared.isPlaying) prepared.start() } catch (e: Exception) { failPlayer(prepared, generation, e) }
            }
            player.setOnCompletionListener { done -> if (preview === done) releasePlayer() }
            player.setOnErrorListener { failed, what, extra ->
                // A stale callback neither stops the current player nor overwrites newer messages.
                if (preview === failed) failPlayer(failed, generation, IllegalStateException("MediaPlayer-Fehler $what/$extra"))
                true
            }
            preview = player
            player.prepareAsync()
        } catch (e: Exception) {
            if (preview === player) preview = null
            player.release()
            android.util.Log.w("WeckerPreview", "Vorschau konnte nicht gestartet werden", e)
            if (generation == previewGeneration) message.value = "Die Vorschau konnte nicht abgespielt werden."
        }
    }
    private fun failPlayer(player: MediaPlayer, generation: Long, error: Exception) {
        android.util.Log.w("WeckerPreview", "Vorschau abgebrochen", error)
        if (preview === player) preview = null
        player.release()
        if (generation == previewGeneration) message.value = "Die Vorschau konnte nicht abgespielt werden."
    }
    private fun releasePlayer() { val player = preview; preview = null; player?.release() }
    /** Stops playback and cancels only the preview's own preparation; every later result of it is discarded. */
    fun stopPreview() {
        previewGeneration++
        previewJob?.let { job -> previewJob = null; job.cancel() }
        releasePlayer()
    }
    fun test(alarm: Alarm) {
        stopPreview()
        stopRecording()
        app.startForegroundService(Intent(app, AlarmService::class.java).setAction("TEST").putExtra("id", alarm.id).putExtra("quiet", true))
        // The app is in the foreground: open the shared alarm screen directly instead of relying on a full-screen notification.
        app.startActivity(Intent(app, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun onCleared() {
        recorder.release(); stopPreview(); settings.close(); auth.cancelLogin(); auth.cancelChat()
        super.onCleared()
    }
}
