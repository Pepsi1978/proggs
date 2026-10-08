package de.frank.jarvis.ablage

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** Was übertragen werden soll. Die [url] bleibt im privaten Speicher und erscheint nie in Texten oder Protokollen. */
data class DownloadAuftrag(
    val url: String,
    val eintragTitel: String,
    val name: String = "",
    val mime: String = "",
    val beschreibung: String = "",
    val kennung: String = "",
    val titel: String = "",
    val herkunft: String = "Download",
)

enum class UebertragungsZustand { LAEUFT, FEHLER }

/** Sichtbarer Stand einer Übertragung: nur der Server-Name, nie die Adresse mit ihren Parametern. */
data class Uebertragung(
    val id: String,
    val eintragTitel: String,
    val name: String,
    val host: String,
    val geladen: Long,
    val gesamt: Long?,
    val zustand: UebertragungsZustand,
    val fehler: String? = null,
    val seit: Long = System.currentTimeMillis(),
) {
    val prozent: Int? get() = gesamt?.takeIf { it > 0 }?.let { ((geladen * 100) / it).toInt().coerceIn(0, 100) }
}

/**
 * Lädt Dateien von einer Adresse (zum Beispiel einem kurzlebigen Ergebnis-Link) vollständig in den privaten Speicher
 * und übergibt sie erst danach der Ablage. Fortschritt, Fehler und Wiederholung (mit Fortsetzen, wenn der Server
 * Teilbereiche kann) laufen hier. Offene Aufträge stehen in einer privaten Datei, damit sie einen Neustart überstehen.
 */
class Uebertragungen(private val speicher: AblageSpeicher, private val auftragsDatei: File, private val client: OkHttpClient = STANDARD_CLIENT) {
    private val sperre = Any()
    private val _liste = MutableStateFlow<List<Uebertragung>>(emptyList())
    val liste: StateFlow<List<Uebertragung>> = _liste.asStateFlow()

    /** id → Auftrag und Teil-Datei; nur im privaten Speicher. */
    private val auftraege = LinkedHashMap<String, Pair<DownloadAuftrag, String>>()

    init {
        // Nach einem Neustart: was lief, gilt als unterbrochen und kann fortgesetzt werden.
        runCatching {
            if (auftragsDatei.isFile) {
                val a = JSONArray(auftragsDatei.readText())
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val auftrag = DownloadAuftrag(o.getString("url"), o.getString("eintrag"), o.optString("name"), o.optString("mime"), o.optString("beschreibung"), o.optString("kennung"), o.optString("titel"), o.optString("herkunft", "Download"))
                    val id = o.getString("id")
                    auftraege[id] = auftrag to o.getString("teil")
                    val teil = File(speicher.teilOrdner, o.getString("teil"))
                    _liste.value = _liste.value + Uebertragung(id, auftrag.eintragTitel, anzeigeName(auftrag), host(auftrag.url), if (teil.isFile) teil.length() else 0, o.optLong("gesamt").takeIf { it > 0 }, UebertragungsZustand.FEHLER, o.optString("fehler").ifEmpty { "Unterbrochen (App oder Handy wurde neu gestartet)." })
                }
            }
        }
    }

    /** Teil-Dateien, die noch zu offenen Aufträgen gehören (das Aufräumen lässt sie stehen). */
    fun offeneTeile(): Set<String> = synchronized(sperre) { auftraege.values.map { it.second }.toSet() }

    fun laeuft(kennung: String): Uebertragung? = synchronized(sperre) {
        if (kennung.isBlank()) return null
        val id = auftraege.entries.firstOrNull { it.value.first.kennung == kennung }?.key ?: return null
        _liste.value.firstOrNull { it.id == id && it.zustand == UebertragungsZustand.LAEUFT }
    }

    /** Legt einen Auftrag an, ohne ihn zu starten. Rückgabe: seine Kennung. Prüft die Adresse vorab. */
    fun anlegen(auftrag: DownloadAuftrag): String {
        pruefeAdresse(auftrag.url)
        synchronized(sperre) {
            val id = AblageSpeicher.neueId()
            auftraege[id] = auftrag to "$id.teil"
            _liste.value = _liste.value + Uebertragung(id, auftrag.eintragTitel, anzeigeName(auftrag), host(auftrag.url), 0, null, UebertragungsZustand.LAEUFT)
            sichern()
            return id
        }
    }

    /**
     * Führt die Übertragung [id] aus (auch als Wiederholung). Erfolg: Die Datei liegt in der Ablage, der Auftrag
     * ist erledigt. Fehler: Der Auftrag bleibt mit Meldung stehen und lässt sich wiederholen; geworfen wird ein
     * [AblageFehler] mit verständlichem Text ohne Adresse.
     */
    suspend fun ausfuehren(id: String): Uebernahme = withContext(Dispatchers.IO) {
        val (auftrag, teilName) = synchronized(sperre) { auftraege[id] } ?: throw AblageFehler("Diese Übertragung gibt es nicht mehr.")
        val teil = File(speicher.teilOrdner, teilName)
        aktualisiere(id) { it.copy(zustand = UebertragungsZustand.LAEUFT, fehler = null, geladen = if (teil.isFile) teil.length() else 0) }
        try {
            val (name, mime) = lade(auftrag, teil, id)
            val ergebnis = speicher.uebernimm(
                eintragTitel = auftrag.eintragTitel, quelle = teil, name = name, mime = mime, herkunft = auftrag.herkunft + " (" + host(auftrag.url) + ")",
                beschreibung = auftrag.beschreibung, kennung = auftrag.kennung.ifBlank { null }, titel = auftrag.titel.ifBlank { null },
            )
            synchronized(sperre) { auftraege.remove(id); _liste.value = _liste.value.filter { it.id != id }; sichern() }
            ergebnis
        } catch (e: kotlinx.coroutines.CancellationException) {
            aktualisiere(id) { it.copy(zustand = UebertragungsZustand.FEHLER, fehler = "Abgebrochen.") }
            throw e
        } catch (e: Exception) {
            val text = when (e) {
                is AblageFehler -> e.message.orEmpty()
                is java.net.UnknownHostException -> "Der Server ${host(auftrag.url)} ist nicht erreichbar (kein Netz oder falsche Adresse)."
                is java.net.SocketTimeoutException -> "Zeitüberschreitung beim Laden von ${host(auftrag.url)}."
                is IOException -> "Die Verbindung zu ${host(auftrag.url)} brach ab."
                else -> "Übertragung fehlgeschlagen (${e.javaClass.simpleName})."
            }
            aktualisiere(id) { it.copy(zustand = UebertragungsZustand.FEHLER, fehler = text) }
            synchronized(sperre) { sichern() }
            throw AblageFehler(text)
        }
    }

    /** Verwirft einen fehlgeschlagenen Auftrag samt Teil-Datei. */
    fun verwerfen(id: String) = synchronized(sperre) {
        auftraege.remove(id)?.let { File(speicher.teilOrdner, it.second).delete() }
        _liste.value = _liste.value.filter { it.id != id }
        sichern()
    }

    private suspend fun lade(auftrag: DownloadAuftrag, teil: File, id: String): Pair<String, String?> {
        val url = auftrag.url.toHttpUrlOrNull() ?: throw AblageFehler("Die Adresse ist ungültig.")
        val vorhanden = if (teil.isFile) teil.length() else 0L
        val anfrage = Request.Builder().url(url).header("Accept-Encoding", "identity").apply { if (vorhanden > 0) header("Range", "bytes=$vorhanden-") }.build()
        client.newCall(anfrage).execute().use { antwort ->
            val code = antwort.code
            if (code == 416 && vorhanden > 0) { teil.delete(); throw AblageFehler("Der Server lehnte das Fortsetzen ab. Bitte erneut versuchen, dann lädt Jarvis von vorn.") }
            if (!antwort.isSuccessful) throw AblageFehler(httpText(code, host(auftrag.url)))
            val fortsetzen = code == 206 && vorhanden > 0
            val koerper = antwort.body ?: throw AblageFehler("Der Server lieferte keinen Inhalt.")
            val laenge = koerper.contentLength().takeIf { it >= 0 }
            val gesamt = laenge?.let { if (fortsetzen) it + vorhanden else it }
            if (gesamt != null && gesamt > MAX_GROESSE) throw AblageFehler("Die Datei ist mit ${Dateityp.groesse(gesamt)} zu groß (höchstens ${Dateityp.groesse(MAX_GROESSE)}).")
            val name = auftrag.name.ifBlank { nameAus(antwort.header("Content-Disposition"), url) }
            val mime = auftrag.mime.ifBlank { koerper.contentType()?.let { "${it.type}/${it.subtype}" }.orEmpty() }.ifBlank { null }
            var geladen = if (fortsetzen) vorhanden else 0L
            aktualisiere(id) { it.copy(name = name, gesamt = gesamt, geladen = geladen) }
            var zuletzt = 0L
            FileOutputStream(teil, fortsetzen).use { aus ->
                koerper.byteStream().use { ein ->
                    val puffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = ein.read(puffer)
                        if (n < 0) break
                        aus.write(puffer, 0, n)
                        geladen += n
                        if (geladen > MAX_GROESSE) throw AblageFehler("Die Datei überschreitet ${Dateityp.groesse(MAX_GROESSE)}; abgebrochen.")
                        val jetzt = System.currentTimeMillis()
                        if (jetzt - zuletzt > 300) { zuletzt = jetzt; val g = geladen; aktualisiere(id) { it.copy(geladen = g) } }
                    }
                }
                aus.fd.sync()
            }
            // Unvollständig? Dann bleibt die Teil-Datei zum Fortsetzen liegen und nichts erscheint in der Ablage.
            if (gesamt != null && teil.length() != gesamt) throw AblageFehler("Unvollständig übertragen (${Dateityp.groesse(teil.length())} von ${Dateityp.groesse(gesamt)}). Bitte erneut versuchen.")
            aktualisiere(id) { it.copy(geladen = teil.length()) }
            return name to mime
        }
    }

    private fun aktualisiere(id: String, aenderung: (Uebertragung) -> Uebertragung) = synchronized(sperre) {
        _liste.value = _liste.value.map { if (it.id == id) aenderung(it) else it }
    }

    private fun sichern() {
        val a = JSONArray()
        auftraege.forEach { (id, paar) ->
            val (auftrag, teil) = paar
            val stand = _liste.value.firstOrNull { it.id == id }
            a.put(JSONObject().put("id", id).put("url", auftrag.url).put("eintrag", auftrag.eintragTitel).put("name", auftrag.name).put("mime", auftrag.mime)
                .put("beschreibung", auftrag.beschreibung).put("kennung", auftrag.kennung).put("titel", auftrag.titel).put("herkunft", auftrag.herkunft)
                .put("teil", teil).put("gesamt", stand?.gesamt ?: 0).put("fehler", stand?.fehler.orEmpty()))
        }
        runCatching { AblageSpeicher.schreibeAtomar(auftragsDatei, a.toString().toByteArray()) }
    }

    companion object {
        const val MAX_GROESSE = 1_000_000_000L

        val STANDARD_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(false)
            .build()

        /** Nur der Server-Name: so darf eine Quelle in Texten, Protokollen und der Oberfläche erscheinen. */
        fun host(url: String): String = url.toHttpUrlOrNull()?.host ?: "unbekannter Server"

        /**
         * Nur HTTPS (in Tests auch http zu localhost über [erlaubeLokal]), keine Adressen im eigenen Netz:
         * ein fremder Link soll das Handy nicht dazu bringen, Geräte im WLAN abzufragen.
         */
        @Volatile var erlaubeLokal = false

        fun pruefeAdresse(adresse: String) {
            val url: HttpUrl = adresse.trim().toHttpUrlOrNull() ?: throw AblageFehler("Das ist keine gültige Web-Adresse (nur https://… wird übernommen).")
            if (erlaubeLokal) return
            if (url.scheme != "https") throw AblageFehler("Nur https-Adressen werden übernommen.")
            val h = url.host.lowercase(Locale.ROOT)
            val lokal = h == "localhost" || h.endsWith(".local") || h.endsWith(".lan") ||
                runCatching { if (h.any { it == ':' } || h.all { it.isDigit() || it == '.' }) InetAddress.getByName(h).let { it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress || it.isAnyLocalAddress } else false }.getOrDefault(true)
            if (lokal) throw AblageFehler("Adressen im eigenen Netz werden aus Sicherheitsgründen nicht geladen.")
        }

        fun anzeigeName(a: DownloadAuftrag): String = a.name.ifBlank { a.url.toHttpUrlOrNull()?.pathSegments?.lastOrNull { it.isNotBlank() } ?: "Datei" }

        fun nameAus(disposition: String?, url: HttpUrl): String {
            disposition?.let { d ->
                Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(d)?.groupValues?.get(1)?.let { return runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
                Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(d)?.groupValues?.get(1)?.let { return it.trim() }
            }
            return url.pathSegments.lastOrNull { it.isNotBlank() } ?: "Datei"
        }

        fun httpText(code: Int, host: String): String = when (code) {
            401, 403 -> "Der Server $host verweigert den Zugriff (Fehler $code). Der Link ist vermutlich abgelaufen oder braucht eine Anmeldung."
            404, 410 -> "Die Datei gibt es auf $host nicht (mehr) (Fehler $code). Temporäre Links verfallen oft nach kurzer Zeit."
            429 -> "Der Server $host bremst gerade (zu viele Anfragen). Bitte später erneut versuchen."
            in 500..599 -> "Der Server $host hat ein Problem (Fehler $code). Bitte später erneut versuchen."
            else -> "Der Server $host antwortete mit Fehler $code."
        }
    }
}
