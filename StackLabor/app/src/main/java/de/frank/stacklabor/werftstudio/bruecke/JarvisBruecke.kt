package de.frank.stacklabor.werftstudio.bruecke

import android.content.ContentProvider
import android.os.Process
import android.os.Binder
import android.content.pm.PackageManager
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import de.frank.stacklabor.werftstudio.StackLaborApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis): Stacks und Mittel lesen UND bearbeiten. Geschützt durch die
 * Signatur-Erlaubnis `de.frank.jarvis.permission.BRUECKE`, also nur für Apps mit demselben Signierschlüssel.
 *
 * Aufruf: `ContentResolver.call(content://de.frank.stacklabor.werftstudio.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück.
 *
 * Lesend: `status`, `stacks`, `stack`, `mittel`, `ziele`. Schreibend: `eintrag`, `stack_speichern`, `mittel_speichern`,
 * `loeschen`. Die Fachlogik steht in [JarvisStackDienst]; jede Datenänderung meldet die App über den Room-Änderungsmelder
 * an Jarvis (`notifyChange` auf diese Adresse, siehe StackLaborApplication).
 */
class JarvisBruecke : ContentProvider() {
    /** Abgleich mit Franks anderem Gerät (Handy ↔ Tablet) über Jarvis, siehe [GeraeteAbgleich]. */
    private val abgleich by lazy { GeraeteAbgleich(context!!.applicationContext, listOf("stacklabor.db")) }

    // Ein bereitliegender Stand des anderen Geräts wird hier eingespielt, bevor irgendetwas die Datenbank öffnet.
    override fun onCreate(): Boolean { context?.let { GeraeteAbgleich.wendeAn(it.applicationContext) }; return true }

    override fun openFile(uri: Uri, mode: String): android.os.ParcelFileDescriptor {
        nurEigeneSignatur()
        return abgleich.oeffne(uri, mode)
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        nurEigeneSignatur()
        abgleich.rufe(method, extras)?.let { return it }
        val antwort = runCatching {
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            val app = context?.applicationContext as? StackLaborApplication ?: return@runCatching JSONObject().put("fehler", "App nicht bereit.")
            runBlocking(Dispatchers.IO) { app.container.jarvisDienst.verarbeite(method, anfrage) }
        }.getOrElse { fehler -> JSONObject().put("fehler", "Stack-Labor: " + (fehler.message ?: fehler.javaClass.simpleName)) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

/**
 * Nur Apps mit Franks Signierschlüssel kommen durch. Die Prüfung steht hier im Code und nicht als `android:permission`
 * am Provider: Die geteilte Erlaubnis `de.frank.jarvis.permission.BRUECKE` gehört im System der App, die sie zuerst
 * deklariert hat, und verschwindet mit deren Deinstallation; dann wären alle Brücken zu (11.11.2026, Entropy Reducer).
 */
private fun ContentProvider.nurEigeneSignatur() {
    val pm = context?.packageManager ?: throw SecurityException("App nicht bereit.")
    if (pm.checkSignatures(Binder.getCallingUid(), Process.myUid()) != PackageManager.SIGNATURE_MATCH)
        throw SecurityException("Nur Apps mit demselben Signierschlüssel.")
}
