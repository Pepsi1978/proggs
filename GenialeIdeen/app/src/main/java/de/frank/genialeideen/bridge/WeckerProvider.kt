package de.frank.genialeideen.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.content.Intent
import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.settings.SecureSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Ausschließlich die Schwester-App darf offene Ideen lesen; kein Schreibzugriff auf die Datenbank. */
class WeckerProvider : ContentProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { GenialeIdeenDatabase.getInstance(requireNotNull(context)) }
    override fun onCreate(): Boolean {
        scope.launch {
            database.ideenDao().alle().map { rows -> rows.filter { it.status == "OFFEN" }.map { listOf(it.id, it.titel, it.text, it.reihenfolge) } }
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
        return database.openHelper.readableDatabase.query(
            "SELECT id AS _id, titel, text, reihenfolge, geaendertAm FROM ideen WHERE status = 'OFFEN' ORDER BY reihenfolge ASC, angelegtAm DESC, id ASC",
        ).also { it.setNotificationUri(requireNotNull(context).contentResolver, URI) }
    }
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
        } }
    }
    override fun getType(uri: Uri) = "vnd.android.cursor.dir/vnd.frank.offene-ideen"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("Nur Lesen")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Nur Lesen")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Nur Lesen")
    companion object { val URI: Uri = Uri.parse("content://de.frank.genialeideen.wecker/offen") }
}
