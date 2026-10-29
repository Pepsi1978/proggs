package de.frank.genialeideen.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.content.Intent
import android.database.MatrixCursor
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.vorleseText
import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.settings.SecureSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Ausschließlich die Schwester-App darf Ideen lesen; kein Schreibzugriff auf die Datenbank.
 * Geliefert werden nur angenommene Ideen, und in der Spalte `text` nur deren Kurzfassung — der
 * Wecker liest genau diese Spalte vor.
 */
class WeckerProvider : ContentProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { GenialeIdeenDatabase.getInstance(requireNotNull(context)) }
    override fun onCreate(): Boolean {
        scope.launch {
            database.ideenDao().alle().map { rows -> angenommene(rows).map { listOf(it.id, it.titel, it.vorleseText(), it.reihenfolge) } }
                .distinctUntilChanged().collect {
                    context?.contentResolver?.notifyChange(URI, null)
                    context?.sendBroadcast(Intent("de.frank.genialeideen.IDEAS_CHANGED")
                        .setClassName("de.frank.genialerwecker", "de.frank.wecker.IdeasChangedReceiver"))
                }
        }
        return true
    }
    private fun authorize() {
        require(callingPackage == "de.frank.genialerwecker") { "Diese Schnittstelle ist nur für Genialer Wecker bestimmt." }
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        authorize()
        require(uri == URI) { "Unbekannte Wecker-Abfrage" }
        val ideen = runBlocking { angenommene(database.ideenDao().alleEinmal()) }
        return MatrixCursor(arrayOf("_id", "titel", "text", "reihenfolge", "geaendertAm")).apply {
            ideen.forEach { addRow(arrayOf<Any>(it.id, it.titel, it.vorleseText(), it.reihenfolge, it.geaendertAm)) }
            setNotificationUri(requireNotNull(context).contentResolver, URI)
        }
    }
    private fun angenommene(rows: List<IdeeEntity>) = rows.filter { it.status == IdeenStatus.ANGENOMMEN.name }
        .sortedWith(compareBy<IdeeEntity> { it.reihenfolge }.thenByDescending { it.angelegtAm }.thenBy { it.id })
    /** Wird nur durch den ausdrücklichen Knopf „Spracheinstellungen übernehmen“ aufgerufen. */
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        authorize()
        require(method == "speechSettings")
        return SecureSettings(requireNotNull(context)).use { settings -> Bundle().apply {
            putString("provider", settings.ttsProvider); putString("edgeVoice", settings.edgeTtsVoice)
            putString("googleVoice", settings.googleTtsVoice); putString("googleKey", settings.googleTtsApiKey)
            putString("qwenVoice", settings.qwenTtsVoiceId); putString("qwenStandardVoice", settings.qwenStandardVoice)
            putString("qwenKey", settings.qwenTtsApiKey); putString("groqKey", settings.groqApiKey)
            putFloat("rate", settings.ttsSpeechRate)
            putString("geminiFlashVoice", settings.geminiFlashTtsVoice); putString("geminiLiteVoice", settings.geminiLiteTtsVoice)
            putString("geminiKeyFrei", settings.geminiTtsKeyFrei); putString("geminiKeyBezahlt", settings.geminiTtsKeyBezahlt)
            putString("geminiSchluesselWahl", settings.geminiTtsSchluesselWahl); putBoolean("geminiChirpRueckfall", settings.geminiTtsChirpRueckfall)
            putString("azureKey", settings.azureTtsKey); putString("azureRegion", settings.azureTtsRegion); putString("azureVoice", settings.azureTtsVoice)
        } }
    }
    override fun getType(uri: Uri) = "vnd.android.cursor.dir/vnd.frank.angenommene-ideen"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("Nur Lesen")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Nur Lesen")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Nur Lesen")
    companion object { val URI: Uri = Uri.parse("content://de.frank.genialeideen.wecker/offen") }
}
