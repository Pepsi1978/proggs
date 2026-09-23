package de.frank.updatestation

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AnmeldungNoetig : Exception("Google-Drive-Anmeldung nötig")

/** Woher die Updates kommen: Google Drive per Anmeldung oder ein per Ordnerauswahl freigegebener Ordner. */
interface UpdateQuelle {
    suspend fun suche(): List<Fund>
    suspend fun oeffne(fund: Fund): InputStream
}

object Quellen {
    fun aktuelle(context: Context): UpdateQuelle? {
        val e = Einstellungen(context)
        return when (e.quelle) {
            "drive" -> DriveQuelle(context, e)
            "ordner" -> e.ordnerUri?.let { OrdnerQuelle(context, Uri.parse(it)) }
            else -> null
        }
    }
}

object DriveAuth {
    private val SCOPE = Scope("https://www.googleapis.com/auth/drive.readonly")
    private fun anfrage() = AuthorizationRequest.builder().setRequestedScopes(listOf(SCOPE)).build()

    /** Für die Oberfläche: liefert das Ergebnis, das ggf. eine Bestätigung (pendingIntent) braucht. */
    suspend fun autorisiere(activity: Activity): AuthorizationResult =
        Identity.getAuthorizationClient(activity).authorize(anfrage()).await()

    /** Für den Hintergrund: Token ohne Rückfrage oder null, wenn der Nutzer erst bestätigen muss. */
    suspend fun token(context: Context): String? {
        val r = Identity.getAuthorizationClient(context).authorize(anfrage()).await()
        return if (r.hasResolution()) null else r.accessToken
    }

    fun verwerfe(context: Context, token: String) {
        runCatching { GoogleAuthUtil.clearToken(context, token) }
    }
}

class DriveQuelle(private val context: Context, private val einst: Einstellungen) : UpdateQuelle {
    private var token: String? = null

    private suspend fun holeToken(neu: Boolean = false): String {
        if (neu) token?.let { DriveAuth.verwerfe(context, it) }
        if (neu || token == null) token = DriveAuth.token(context) ?: throw AnmeldungNoetig()
        return token!!
    }

    private suspend fun verbinde(url: String): HttpURLConnection {
        repeat(2) { versuch ->
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Authorization", "Bearer ${holeToken(neu = versuch > 0)}")
            }
            when (c.responseCode) {
                200 -> return c
                401 -> c.disconnect()
                404 -> { c.disconnect(); throw FileNotFoundException(url) }
                else -> {
                    val text = c.errorStream?.bufferedReader()?.readText().orEmpty()
                    c.disconnect()
                    if (c.responseCode == 403 && text.contains("insufficient", true)) throw AnmeldungNoetig()
                    throw IOException("Google Drive antwortet ${c.responseCode}: ${text.take(200)}")
                }
            }
        }
        throw AnmeldungNoetig()
    }

    private suspend fun json(url: String): JSONObject = withContext(Dispatchers.IO) {
        val c = verbinde(url)
        try { JSONObject(c.inputStream.bufferedReader().readText()) } finally { c.disconnect() }
    }

    private suspend fun dateien(q: String): List<JSONObject> {
        val ergebnis = mutableListOf<JSONObject>()
        var seite: String? = null
        do {
            val url = "https://www.googleapis.com/drive/v3/files?pageSize=1000" +
                "&fields=nextPageToken,files(id,name,mimeType,size)" +
                "&q=" + URLEncoder.encode(q, "UTF-8") +
                (seite?.let { "&pageToken=$it" } ?: "")
            val o = json(url)
            val arr = o.optJSONArray("files")
            if (arr != null) for (i in 0 until arr.length()) ergebnis += arr.getJSONObject(i)
            seite = o.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (seite != null)
        return ergebnis
    }

    private suspend fun findeOrdner(): String {
        var eltern = "root"
        for (teil in einst.drivePfad.split('/').map { it.trim() }.filter { it.isNotEmpty() }) {
            val name = teil.replace("\\", "\\\\").replace("'", "\\'")
            val treffer = dateien("name = '$name' and '$eltern' in parents and mimeType = '$ORDNER' and trashed = false")
            eltern = treffer.firstOrNull()?.getString("id")
                ?: throw IOException("Ordner \"$teil\" in Google Drive nicht gefunden (Pfad: ${einst.drivePfad}).")
        }
        return eltern
    }

    override suspend fun suche(): List<Fund> = withContext(Dispatchers.IO) {
        val updatesId = einst.driveOrdnerId ?: findeOrdner().also { einst.driveOrdnerId = it }
        val unterordner = try {
            dateien("'$updatesId' in parents and mimeType = '$ORDNER' and trashed = false")
        } catch (e: FileNotFoundException) {
            einst.driveOrdnerId = null
            val neu = findeOrdner().also { einst.driveOrdnerId = it }
            dateien("'$neu' in parents and mimeType = '$ORDNER' and trashed = false")
        }
        coroutineScope {
            unterordner.map { ordner ->
                async {
                    val inhalt = dateien("'${ordner.getString("id")}' in parents and trashed = false")
                    val manifestDatei = inhalt.firstOrNull { it.getString("name") == "update.json" } ?: return@async null
                    val text = medien(manifestDatei.getString("id")).use { it.bufferedReader().readText() }
                    val m = runCatching { UpdateManifest.ausJson(text) }.getOrNull() ?: return@async null
                    val apk = inhalt.firstOrNull { it.getString("name") == m.apk }
                    Fund(m, apk?.getString("id"))
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun medien(id: String): InputStream =
        verbinde("https://www.googleapis.com/drive/v3/files/$id?alt=media").inputStream

    override suspend fun oeffne(fund: Fund): InputStream = withContext(Dispatchers.IO) {
        medien(fund.apkRef ?: throw IOException("APK ist noch nicht in Google Drive angekommen."))
    }

    companion object {
        private const val ORDNER = "application/vnd.google-apps.folder"
    }
}

/**
 * Ein per Ordnerauswahl freigegebener Ordner – direkt aus Google Drive (Drive-Dokumentanbieter)
 * oder von einer Sync-App gespiegelt. Die Freigabe gilt für alle Unterordner.
 *
 * Der Drive-Anbieter liefert Ordnerinhalte aus seinem Zwischenspeicher. Damit "Jetzt prüfen"
 * wirklich den aktuellen Stand sieht, wird jeder Ordner vor dem Lesen aktiv neu angefordert
 * (ContentResolver.refresh) und auf das Ende des Nachladens (EXTRA_LOADING) gewartet.
 */
class OrdnerQuelle(private val context: Context, private val baum: Uri) : UpdateQuelle {
    private data class Kind(val id: String, val name: String, val ordner: Boolean)

    private val spalten = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )

    private fun neuAnfordern(docId: String, name: String) {
        val kinderUri = DocumentsContract.buildChildDocumentsUriUsingTree(baum, docId)
        val dokUri = DocumentsContract.buildDocumentUriUsingTree(baum, docId)
        val a = runCatching { context.contentResolver.refresh(kinderUri, null, null) }.getOrDefault(false)
        val b = runCatching { context.contentResolver.refresh(dokUri, null, null) }.getOrDefault(false)
        Log.i(TAG, "Ordner '$name': Neuladen angefordert (Liste=$a, Ordner=$b)")
    }

    /** Eine Abfrage der Kinder; wartet kurz, falls der Anbieter selbst "lädt noch" meldet. */
    private suspend fun abfrage(uri: Uri, name: String, versuch: Int): List<Kind> {
        val c = context.contentResolver.query(uri, spalten, null, null, null)
            ?: throw IOException("Kein Zugriff mehr auf den Ordner. Bitte neu auswählen.")
        try {
            val liste = mutableListOf<Kind>()
            while (c.moveToNext()) {
                liste += Kind(c.getString(0), c.getString(1) ?: "", c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR)
            }
            val laedt = c.extras?.getBoolean(DocumentsContract.EXTRA_LOADING) == true
            Log.i(TAG, "Ordner '$name' Abfrage $versuch: ${liste.size} Einträge, lädt=$laedt: ${liste.joinToString { it.name }.take(300)}")
            if (laedt) {
                val geaendert = CompletableDeferred<Unit>()
                val beobachter = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) { geaendert.complete(Unit) }
                }
                c.registerContentObserver(beobachter)
                withTimeoutOrNull(5_000) { geaendert.await() }
                c.unregisterContentObserver(beobachter)
            }
            return liste
        } finally {
            c.close()
        }
    }

    /**
     * Liest die Kinder eines Ordners frisch aus Google Drive: Neuladen anfordern, dann so lange
     * nachfragen, bis sich die Liste nicht mehr ändert. Der Drive-Anbieter lädt im Hintergrund
     * und meldet das meist nicht über EXTRA_LOADING – sofortiges Lesen liefert den alten Stand.
     */
    private suspend fun kinder(docId: String, name: String): List<Kind> {
        neuAnfordern(docId, name)
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(baum, docId)
        delay(ERSTE_WARTEZEIT)
        var vorher: List<String>? = null
        var liste = emptyList<Kind>()
        for (versuch in 1..8) {
            liste = abfrage(uri, name, versuch)
            val namen = liste.map { it.name }.sorted()
            if (namen == vorher) return liste
            vorher = namen
            delay(NACHFRAGE_ABSTAND)
        }
        return liste
    }

    private suspend fun leseManifest(datei: Kind, projekt: String): UpdateManifest? {
        val dokUri = DocumentsContract.buildDocumentUriUsingTree(baum, datei.id)
        runCatching { context.contentResolver.refresh(dokUri, null, null) }
        delay(NACHFRAGE_ABSTAND)
        val text = context.contentResolver.openInputStream(dokUri)?.use { it.bufferedReader().readText() } ?: return null
        return runCatching { UpdateManifest.ausJson(text) }
            .onFailure { Log.w(TAG, "Projekt '$projekt': update.json unlesbar", it) }
            .getOrNull()
    }

    private fun hoechsteApkNummer(dateien: List<Kind>): Long =
        dateien.mapNotNull { Regex("""-vc(\d+)\.apk$""").find(it.name)?.groupValues?.get(1)?.toLongOrNull() }.maxOrNull() ?: 0

    private suspend fun leseProjekt(ordner: Kind): Fund? {
        var dateien = kinder(ordner.id, ordner.name)
        var manifestDatei = dateien.firstOrNull { it.name == "update.json" }
        var runde = 0
        var m: UpdateManifest? = null
        while (runde < 3) {
            runde++
            if (manifestDatei != null) m = leseManifest(manifestDatei, ordner.name)
            val gelesen = m
            val apkDa = gelesen != null && dateien.any { it.name == gelesen.apk }
            // Stimmig: update.json vorhanden, ihre APK liegt da und keine neuere APK im Ordner.
            if (gelesen != null && apkDa && hoechsteApkNummer(dateien) <= gelesen.versionCode) break
            if (manifestDatei == null && dateien.none { it.name.endsWith(".apk") }) break
            Log.i(TAG, "Projekt '${ordner.name}': Stand noch nicht stimmig (Runde $runde), lade erneut")
            dateien = kinder(ordner.id, ordner.name)
            manifestDatei = dateien.firstOrNull { it.name == "update.json" }
        }
        val manifest = m
        if (manifest == null) {
            Log.i(TAG, "Projekt '${ordner.name}': keine update.json")
            return null
        }
        val apk = dateien.firstOrNull { it.name == manifest.apk }
        Log.i(TAG, "Projekt '${ordner.name}': ${manifest.versionName} (${manifest.versionCode}), APK ${if (apk != null) "gefunden" else "fehlt"}")
        return Fund(manifest, apk?.let { DocumentsContract.buildDocumentUriUsingTree(baum, it.id).toString() })
    }

    override suspend fun suche(): List<Fund> = withContext(Dispatchers.IO) {
        val wurzel = DocumentsContract.getTreeDocumentId(baum)
        val ordner = kinder(wurzel, "Updates").filter { it.ordner }
        Log.i(TAG, "Updates: ${ordner.size} Projektordner")
        coroutineScope {
            ordner.map { o -> async { runCatching { leseProjekt(o) }.onFailure { Log.w(TAG, "Projekt '${o.name}' fehlgeschlagen", it) }.getOrNull() } }
                .awaitAll().filterNotNull()
        }
    }

    override suspend fun oeffne(fund: Fund): InputStream = withContext(Dispatchers.IO) {
        val ref = fund.apkRef ?: throw IOException("APK ist noch nicht im Ordner angekommen.")
        context.contentResolver.openInputStream(Uri.parse(ref)) ?: throw IOException("APK nicht lesbar.")
    }
}

const val TAG = "UpdateStation"
private const val ERSTE_WARTEZEIT = 2_500L
private const val NACHFRAGE_ABSTAND = 1_500L
