package de.frank.updatestation

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AnmeldungNoetig : Exception("Google-Drive-Anmeldung nötig")

/** Woher die Updates kommen: Google Drive direkt oder ein lokal synchronisierter Ordner. */
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

/** Ordner, den eine Sync-App (z. B. Autosync) aus Google Drive aufs Handy spiegelt. */
class OrdnerQuelle(private val context: Context, private val baum: Uri) : UpdateQuelle {
    override suspend fun suche(): List<Fund> = withContext(Dispatchers.IO) {
        val wurzel = DocumentFile.fromTreeUri(context, baum) ?: throw IOException("Ordner nicht lesbar.")
        if (!wurzel.canRead()) throw IOException("Kein Zugriff mehr auf den Ordner. Bitte neu auswählen.")
        wurzel.listFiles().filter { it.isDirectory }.mapNotNull { ordner ->
            val dateien = ordner.listFiles()
            val manifestDatei = dateien.firstOrNull { it.name == "update.json" } ?: return@mapNotNull null
            val text = context.contentResolver.openInputStream(manifestDatei.uri)?.use { it.bufferedReader().readText() }
                ?: return@mapNotNull null
            val m = runCatching { UpdateManifest.ausJson(text) }.getOrNull() ?: return@mapNotNull null
            Fund(m, dateien.firstOrNull { it.name == m.apk }?.uri?.toString())
        }
    }

    override suspend fun oeffne(fund: Fund): InputStream = withContext(Dispatchers.IO) {
        val ref = fund.apkRef ?: throw IOException("APK ist noch nicht im Ordner angekommen.")
        context.contentResolver.openInputStream(Uri.parse(ref)) ?: throw IOException("APK nicht lesbar.")
    }
}
