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
import de.frank.genialeideen.data.settings.SecureSettings
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One saved-and-closed editor; [generation] makes every save a distinct event. */
data class SavedEvent(val id: String, val generation: Long)

class WeckerViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        /** Beschriftung, solange eine Stimmprobe entsteht; die Oberfläche zeigt dann den Fortschritt. */
        const val PROBE_VORBEREITEN = "Stimmprobe vorbereiten …"
    }
    private val app = application
    val store = AlarmStore.get(application)
    val settings = SecureSettings(application)
    val alarms = store.alarms
    val theme = settings.themeFlow
    val scheduler = AlarmScheduler(application)
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
    val settingsRevision = MutableStateFlow(0)
    /** Testphase vorbei und nicht gekauft: Hinweis mit Freischaltung zeigen. */
    val freischaltungNoetig = MutableStateFlow(false)
    /**
     * Einzige Sperre der Testphase: Anlegen, Bearbeiten, Duplizieren und Einschalten. Klingeln, Schlummern,
     * Stopp, Ausschalten, Löschen, Auslassen und die Vorbereitung bestehender Wecker bleiben immer frei.
     */
    fun darfBearbeiten(): Boolean {
        Freischaltung.pruefeTest()
        if (Freischaltung.zustand.value.darfBearbeiten) return true
        freischaltungNoetig.value = true
        return false
    }

    private var actionJob: Job? = null
    /** Owner of the busy banner; a cancelled older action must not clear the banner of a newer one. */
    private var busyOwner: Any? = null
    /** Incremented by every stop or new preview; late preparations, callbacks and errors of older generations are dropped. */
    private var previewGeneration = 0L
    /** Only the preview's own preparation job, never a foreign action job. */
    private var previewJob: Job? = null
    /** Confirmation shown on the saved card instead of a success banner. */
    val lastSaved = MutableStateFlow<SavedEvent?>(null)
    private var savedGeneration = 0L
    /** Changes whenever an editor is opened or closed; the same alarm id reopened is a new editor session. */
    private var editorGeneration = 0L


    init {
        // Einmalig (1.0.6): Wecker mit fest gewählter Gerätestimme bekommen wieder die Vorgabe (heute die lokale Modellstimme).
        if (!store.prefs.getBoolean("premium_migration1", false)) {
            store.all().filter { LokaleStimmen.istEigeneEngine(it.voiceProvider) && it.voiceId.isNotBlank() }
                .forEach { a -> store.update(a.id) { it.copy(voiceProvider = "", voiceId = "") } }
            store.prefs.edit().putBoolean("premium_migration1", true).apply()
        }
        scheduler.restore()
        // Gerätestimmen werden nicht mehr beim Start abgefragt (spart das Binden der TTS-Engine); die Vorbereitung fragt sie selbst.
        viewModelScope.launch(Dispatchers.IO) { Tones.names.keys.forEach { Tones.file(store.files, it) } }
        PreparationWorker.enqueue(app)
    }
    /** Neue Wecker bekommen EINMAL die Gerätesprache als festen Wert; ein späterer Locale-Wechsel ändert ihn nicht. */
    fun newAlarm() { if (darfBearbeiten()) edit(Alarm(id = UUID.randomUUID().toString(), sprache = geraeteSprache())) }
    /** Sprache des Handys (de/en/fr/es) – bestimmt, welche Stimmen überhaupt angeboten werden. */
    fun geraeteSprache(): String {
        val locale = app.resources.configuration.locales[0]
        return DiktatLogik.spracheFuer(locale.language, locale.country).first.code
    }
    val geraeteRegion: String get() = app.resources.configuration.locales[0].country
    fun edit(alarm: Alarm) {
        stopPreview()
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
    fun closeEditor() { editorGeneration++; _draft.value = null; draftBaseline = null; store.prefs.edit().remove("draft").remove("draft_is_new").remove("draft_base").apply() }
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
    }
    fun save(notificationsDenied: Boolean = false, done: () -> Unit) {
        val source = _draft.value ?: return
        if (!darfBearbeiten()) return
        speichern(source, notificationsDenied, done)
    }
    private fun speichern(source: Alarm, notificationsDenied: Boolean, done: () -> Unit) {
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
                if (closed && planned && !notificationsDenied) message.value = "${alarm.name} gespeichert · Die Weckstimmen werden erzeugt …"
            }
        }
    }
    /** Clears only the event of this generation; an older confirmation ending never removes a newer one. */
    fun consumeSaved(generation: Long) { if (lastSaved.value?.generation == generation) lastSaved.value = null }
    fun toggle(alarm: Alarm, enabled: Boolean) {
        // Ausschalten ist immer erlaubt; nur das Einschalten fällt unter die Testphase.
        if (enabled && !darfBearbeiten()) return
        toggleNow(alarm, enabled)
    }
    private fun toggleNow(alarm: Alarm, enabled: Boolean) = runAction("Weckzeit ändern …", silent = true) {
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
    /** Weckstimmen erzeugen: im Hintergrund-Worker (Vordergrund-Benachrichtigung), damit es auch bei geschlossener App weiterläuft. */
    fun prepare(alarm: Alarm) {
        if (alarm.needsSpeech) PreparationWorker.sofort(app)
    }
    fun settingsChanged() {
        settingsRevision.value++
        PreparationWorker.enqueue(app)
    }
    fun importMusic(uri: Uri, quelle: String = "datei") = runAction("Song vollständig auf dem Gerät speichern …") {
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
                    // Geräte-Wecktöne (oft OGG) melden „hat Audio“ teils gar nicht; nur ein ausdrückliches Nein weist ab.
                    require(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != "no") { "Diese Datei enthält keine abspielbare Audiospur." }
                } finally { metadata.release() }
                file.absolutePath to name
            } catch (e: Exception) { file.delete(); throw e }
        }
        _draft.value?.takeIf { it.id == alarmId }?.let { change(it.copy(music = result.first, musicName = result.second, musicQuelle = quelle)) }
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
    /**
     * Hört die Stimme ab. Ohne [alarm] gilt der globale Standard, mit [alarm] dessen eigene Auswahl
     * samt eigenem Tempo. Die globalen Einstellungen werden dabei nie verändert.
     */
    fun previewVoice(alarm: Alarm? = null, text: String? = null, sprache: String? = null, stimme: String? = null) {
        // Ein Schnappschuss für den gesamten Vorgang: Audio und Abspieltempo stammen garantiert aus
        // derselben Stimme, auch wenn die Einstellungen währenddessen geändert werden.
        // Dieselbe Auflösung wie beim Wecken: Sprache des Weckers bzw. die gewählte Sprache der Einstellungen.
        val voice = SyntheseStimme(settings).let { defaults -> alarm?.resolveVoice(defaults) ?: sprache?.let { defaults.fuerSprache(it) } ?: defaults }
            .let { v -> stimme?.let { v.copy(stimme = it) } ?: v }
        vorhoeren(voice, alarm, text)
    }
    private fun vorhoeren(voice: SyntheseStimme, alarm: Alarm?, text: String?) {
        stopPreview()
        val generation = previewGeneration
        runAction(PROBE_VORBEREITEN) {
            // The job is captured inside the action, so a refused runAction can never register a foreign job.
            val job = currentCoroutineContext()[Job]
            if (generation != previewGeneration) return@runAction
            previewJob = job
            try {
                // Stimmauswahl: die fertige 40-Schritte-Probe aus der APK sofort abspielen, im eingestellten Tempo.
                if (text == null) ModellStimmen.probeDatei(app, voice)?.let { probe ->
                    if (generation != previewGeneration) return@runAction
                    if (previewJob === job) previewJob = null
                    startPlayer(probe, voice.ttsSpeechRate.coerceIn(.5f, 2f), generation)
                    return@runAction
                }
                val prep = SpeechPreparation(app, settings) { voice }
                val file = prep.audio(text?.takeIf { it.isNotBlank() }?.take(1500)
                    ?: Sprachen.probe(voice.sprache))
                if (generation != previewGeneration) return@runAction
                // Hand over without stopPreview(): that would cancel this very job.
                if (previewJob === job) previewJob = null
                // Mit eigenem Text: genau so laut wie beim echten Wecken (Alarm-Audiostrom, Wecklautstärke).
                startPlayer(file, prep.playbackSpeed, generation, weckLautstaerke = if (text != null) alarm?.volume else null)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // An error of an outdated preview is dropped instead of overwriting newer messages.
                if (generation == previewGeneration) throw e
            } finally { if (previewJob === job) previewJob = null }
        }
    }
    /** Plays a built-in tone; creating the file runs on IO, playback on Main, both bound to the current generation. */
    fun playTone(id: String, weckLautstaerke: Int? = null, anschwellSekunden: Int = 0) {
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
                startPlayer(file, 1f, generation, weckLautstaerke, anschwellSekunden)
            } finally {
                // Released only by its own identity, also after file errors or cancellation.
                if (previewJob === job) previewJob = null
            }
        }
    }
    /** Spielt die gewählte eigene Musikdatei (MP3 oder Geräte-Weckton) zur Probe ab. */
    fun playMusic(path: String, weckLautstaerke: Int? = null, anschwellSekunden: Int = 0) {
        stopPreview()
        val file = File(path)
        if (!file.exists()) { message.value = "Die Audiodatei ist nicht mehr vorhanden."; return }
        startPlayer(file, 1f, previewGeneration, weckLautstaerke, anschwellSekunden)
    }
    /** Local player until it is prepared successfully; every failure releases it and never throws into the UI. */
    private fun startPlayer(file: File, speed: Float, generation: Long, weckLautstaerke: Int? = null, anschwellSekunden: Int = 0) {
        // Die Markierung des laufenden Anhören-Knopfs bleibt erhalten: Nur der alte Player wird
        // freigegeben, die Vorschau selbst läuft ja gerade an (sonst zeigte der Knopf nie „Stopp“).
        val laufend = vorschau.value
        releasePlayer()
        vorschau.value = laufend
        if (weckLautstaerke != null) {
            // Wie AlarmService.setVolume: Alarmstrom auf den Anteil der Wecklautstärke, danach zurück.
            val audio = app.getSystemService(android.media.AudioManager::class.java)
            runCatching { vorherAlarmLautstaerke = audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM) }
            vorschauZiel = weckLautstaerke
            vorschauAnschwellen = anschwellSekunden
            vorschauStart = System.currentTimeMillis()
            alarmstromSetzen(anschwellAnteil())
            // Dieselbe Rampe wie beim echten Wecken (AlarmService): alle 500 ms nachziehen, ab 5 %.
            anschwellJob = viewModelScope.launch {
                while (isActive) { delay(500); alarmstromSetzen(anschwellAnteil()) }
            }
        }
        // Construction can fail natively; that must not escape as an unhandled coroutine exception.
        val player = try { MediaPlayer() } catch (e: Exception) {
            android.util.Log.w("WeckerPreview", "MediaPlayer konnte nicht erzeugt werden", e)
            if (generation == previewGeneration) message.value = "Die Vorschau konnte nicht abgespielt werden."
            return
        }
        try {
            player.setAudioAttributes(android.media.AudioAttributes.Builder()
                .setUsage(if (weckLautstaerke != null) android.media.AudioAttributes.USAGE_ALARM else android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
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
    private fun releasePlayer() {
        val player = preview; preview = null; player?.release(); vorschau.value = null
        anschwellJob?.cancel(); anschwellJob = null
        // Eine für die Testvorlesung gesetzte Alarmlautstärke wird wiederhergestellt.
        if (vorherAlarmLautstaerke >= 0) {
            runCatching { app.getSystemService(android.media.AudioManager::class.java)
                .setStreamVolume(android.media.AudioManager.STREAM_ALARM, vorherAlarmLautstaerke, 0) }
            vorherAlarmLautstaerke = -1
        }
    }
    private var vorherAlarmLautstaerke = -1
    /**
     * Wecklautstärke während des Anhörens live nachziehen – derselbe Weg wie beim echten Wecken
     * (Alarmstrom auf den Anteil), nur solange eine Vorschau auf dem Alarmstrom läuft.
     */
    fun vorschauLautstaerke(weckLautstaerke: Int) {
        if (preview == null || vorherAlarmLautstaerke < 0) return
        vorschauZiel = weckLautstaerke
        alarmstromSetzen(anschwellAnteil())
    }
    /** Anschwelldauer während des Anhörens ändern; die laufende Rampe rechnet ab ihrem Start neu. */
    fun vorschauAnschwellen(sekunden: Int) {
        if (preview == null || vorherAlarmLautstaerke < 0) return
        vorschauAnschwellen = sekunden
        alarmstromSetzen(anschwellAnteil())
    }
    private var vorschauZiel = 100
    private var vorschauAnschwellen = 0
    private var vorschauStart = 0L
    private var anschwellJob: Job? = null
    private fun anschwellAnteil(): Float = if (vorschauAnschwellen == 0) 1f else
        ((System.currentTimeMillis() - vorschauStart).toFloat() / (vorschauAnschwellen * 1000)).coerceIn(.05f, 1f)
    private fun alarmstromSetzen(anteil: Float) = runCatching {
        val audio = app.getSystemService(android.media.AudioManager::class.java)
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
        val prozent = (vorschauZiel * anteil).roundToInt().coerceAtLeast(1)
        audio.setStreamVolume(android.media.AudioManager.STREAM_ALARM, (max * prozent / 100f).toInt().coerceIn(1, max), 0)
    }
    /** Was gerade zur Probe läuft (Schlüssel des Anhören-Knopfs), sonst null. */
    val vorschau = MutableStateFlow<String?>(null)
    /** Stops playback and cancels only the preview's own preparation; every later result of it is discarded. */
    fun stopPreview() {
        previewGeneration++
        previewJob?.let { job -> previewJob = null; job.cancel() }
        releasePlayer()
    }
    fun test(alarm: Alarm) {
        stopPreview()
        app.startForegroundService(Intent(app, AlarmService::class.java).setAction("TEST").putExtra("id", alarm.id).putExtra("quiet", true))
        // The app is in the foreground: open the shared alarm screen directly instead of relying on a full-screen notification.
        app.startActivity(Intent(app, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun onCleared() {
        stopPreview(); settings.close()
        super.onCleared()
    }
}
