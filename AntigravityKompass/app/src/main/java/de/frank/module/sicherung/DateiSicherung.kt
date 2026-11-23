// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v7
// Quelle: Module/Android/M1.1-Sicherung/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.1 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Eine Sicherungsdatei im gewählten Ordner. */
data class Sicherungsdatei(val uri: Uri, val name: String, val geaendertAm: Long)

/**
 * Sicherung als Datei in einen selbst gewählten Ordner — in der Praxis ein Google-Drive-Ordner,
 * weil die Drive-App sich Android als Speicherort anbietet.
 *
 * Der Weg braucht kein Cloud-Projekt, keine Client-ID und keine Anmeldung: Android fragt einmal
 * nach dem Ordner, die App behält ihn dauerhaft und schreibt danach ohne Rückfrage dorthin. Auf
 * einem zweiten Gerät wird derselbe Ordner gewählt und die Sicherung eingelesen.
 *
 * Der frühere Weg über die Drive-API scheiterte an dem, was er zusätzlich verlangte: eine in der
 * Google-Cloud-Console registrierte OAuth-Client-ID. Ohne sie antwortet Google mit
 * „Fehler 888 / unregistered on API console“ — daran lässt sich in der App nichts ändern.
 *
 * Es liegen immer höchstens [BEHALTEN] Sicherungen im Ordner: die aktuelle und die davor.
 * Ältere räumt jede Sicherung selbst weg, damit sich dort nichts ansammelt.
 */
class DateiSicherung(
    private val context: Context,
    private val namen: SicherungsNamen,
    private val protokoll: SicherungsProtokoll = StillesProtokoll,
) {

    // Aus den Namen abgeleitet. Früher standen diese Muster im companion object und wurden aus
    // einem fest verdrahteten App-Profil gebaut — als Modul müssen sie zur Instanz gehören,
    // weil jede App ihr eigenes Präfix mitbringt.

    /** `20-03-2026-1346-ocode-kompass.json` — Zeitpunkt vorn, App-Name hinten. */
    private val HEUTIGES_MUSTER = muster(namen.dateiPraefix)

    /**
     * Das heutige Muster und die unter früheren App-Namen geschriebenen.
     *
     * Eine Umbenennung der App darf keine Sicherung entwerten: Wer gestern unter dem alten
     * Namen gesichert hat, muss die Datei heute noch finden und einspielen können.
     */
    private val MUSTER = listOf(HEUTIGES_MUSTER) + namen.fruehere.map(::muster)

    /** Das älteste Muster `ocode-kompass-2026-03-20-1346Z.json`, nur noch zum Lesen. */
    private val ALTE_PRAEFIXE =
        (listOf(namen.dateiPraefix) + namen.fruehere).map { "$it-" }

    private val prefs
        get() = context.applicationContext.getSharedPreferences(namen.einstellungenDatei, Context.MODE_PRIVATE)

    /**
     * Der geparste Ordner — einmal gelesen, danach gemerkt.
     *
     * `null` heisst „noch nicht nachgesehen", nicht „kein Ordner"; das steht im Wert darin.
     * Die selbsttätige Sicherung fragt bei JEDER Datenbankänderung nach dem Ordner, und jede
     * Frage hiess vorher: Ablage aufschlagen, Zeichenkette holen, Adresse neu zergliedern.
     */
    @Volatile
    private var gemerkterOrdner: Optional? = null

    /** Ein gemerkter Wert, der auch „nichts" sein kann — `null` steht schon für „ungelesen". */
    private class Optional(val wert: Uri?)

    /** Der gemerkte Sicherungsordner, oder null solange keiner gewählt wurde. */
    val ordner: Uri?
        get() = (
            gemerkterOrdner
                ?: Optional(prefs.getString(namen.ordnerSchluessel, null)?.let(Uri::parse))
                    .also { gemerkterOrdner = it }
            ).wert

    /**
     * Der zuletzt ermittelte Ordnername.
     *
     * [ordnerName] fragt den Speicheranbieter — bei einem Drive-Ordner also die Drive-App —
     * über den ContentResolver. Der Einstellungs-Bildschirm liest seinen Zustand bei JEDER
     * Änderung neu ein, vom Schieberegler bis zum Schalter, und tut das auf dem Hauptfaden.
     * Eine solche Abfrage dort kostet im ungünstigen Fall hunderte Millisekunden und lässt die
     * Oberfläche stocken. Der Name ändert sich nur, wenn ein anderer Ordner gewählt wird —
     * genau dann wird er verworfen.
     */
    @Volatile
    private var gemerkterName: String? = null

    /** Ordner merken und die Schreibberechtigung über Neustarts hinweg behalten. */
    fun merkeOrdner(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit().putString(namen.ordnerSchluessel, uri.toString()).apply()
        gemerkterOrdner = null
        gemerkterName = null
    }

    fun vergissOrdner() {
        ordner?.let { alt ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    alt,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        prefs.edit().remove(namen.ordnerSchluessel).apply()
        gemerkterOrdner = null
        gemerkterName = null
    }

    /** Der Name des Ordners, wie ihn der Dateiwähler zeigt — für die Anzeige in den Einstellungen. */
    fun ordnerName(): String? {
        val baum = ordner ?: return null
        gemerkterName?.let { return it }
        val kennung = runCatching { DocumentsContract.getTreeDocumentId(baum) }.getOrNull()
            ?: return baum.lastPathSegment
        val dokument = DocumentsContract.buildDocumentUriUsingTree(baum, kennung)
        val name = spalte(dokument, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            ?: kennung.substringAfterLast(':').substringAfterLast('/')
        gemerkterName = name
        return name
    }

    /**
     * Schreibt eine neue Sicherung und räumt danach auf: Es bleiben nur die aktuelle und die
     * eine davor stehen.
     */
    /**
     * Schreibt eine neue Sicherung. Aufgeräumt wird erst später, über [raeumeAlteWeg].
     *
     * Die Trennung ist wichtig: Aufzuräumen, bevor die frische Datei einmal fehlerfrei gelesen
     * wurde, hiesse eine gute Sicherung gegen eine ungeprüfte einzutauschen. Geht beim
     * Zurücklesen etwas schief, stehen so wenigstens noch die beiden alten Stände da.
     */
    suspend fun schreibe(
        fuelle: suspend (java.io.Writer) -> Unit,
    ): Pair<Sicherungsdatei, List<Sicherungsdatei>> = withContext(Dispatchers.IO) {
        val baum = ordner ?: error("Es ist noch kein Sicherungsordner gewählt.")
        val bisherige = listeAuf(baum, benenneAlteUm = true)
        val name = neuerName(bisherige)
        val datei = DocumentsContract.createDocument(
            context.contentResolver,
            DocumentsContract.buildDocumentUriUsingTree(baum, DocumentsContract.getTreeDocumentId(baum)),
            "application/json",
            name,
        ) ?: error("Im Sicherungsordner liess sich keine Datei anlegen. Wähl ihn neu aus.")

        check(bisherige.none {
            DocumentsContract.getDocumentId(it.uri) == DocumentsContract.getDocumentId(datei)
        }) { "Der Speicheranbieter hat keine neue Datei angelegt. Die bestehende Sicherung bleibt unverändert." }

        try {
            // Gepuffert und satzweise: Der Inhalt wird nie als eine grosse Zeichenkette gebaut.
            context.contentResolver.openOutputStream(datei, "wt")?.use { strom ->
                strom.bufferedWriter(Charsets.UTF_8).use { schreiber -> fuelle(schreiber) }
            } ?: error("Die Sicherung ließ sich nicht schreiben. Wähl den Ordner neu aus.")
        } catch (fehler: Exception) {
            runCatching {
                check(DocumentsContract.deleteDocument(context.contentResolver, datei))
            }.onFailure {
                protokoll.warn("DateiSicherung", "schreibe", "Unvollständige Datei blieb liegen",
                    mapOf("art" to it.javaClass.simpleName))
            }
            throw fehler
        }

        Sicherungsdatei(datei, name, System.currentTimeMillis()) to bisherige
    }

    /**
     * Wirft eine frisch geschriebene Datei weg, die sich nicht zurücklesen liess.
     *
     * Sie bliebe sonst liegen — und weil ihr Name den jüngsten Zeitpunkt trägt, gälte sie beim
     * nächsten Mal als „die aktuelle". Der nächste geglückte Lauf räumte dann die letzte gute
     * Sicherung weg und behielte die unlesbare als Rückfallebene. Bis dahin böte das
     * Wiederherstellen sie als neueste an, und der Benutzer bekäme „beschädigt" zu lesen,
     * während eine heile Datei danebenliegt.
     *
     * Geht das Löschen nicht, bleibt sie liegen; ein zweiter Fehlschlag hintereinander ändert
     * nichts daran, dass die Sicherung davor unangetastet dasteht.
     */
    suspend fun verwirf(datei: Sicherungsdatei) = withContext(Dispatchers.IO) {
        runCatching {
            check(DocumentsContract.deleteDocument(context.contentResolver, datei.uri))
        }.onFailure {
            protokoll.warn(
                "DateiSicherung",
                "verwirf",
                "Ungeprüfte Sicherung blieb liegen",
                mapOf("name" to datei.name, "art" to it.javaClass.simpleName),
            )
        }
        Unit
    }

    /**
     * Räumt alles bis auf die [BEHALTEN] jüngsten weg — aufzurufen, NACHDEM die frische
     * Sicherung geprüft ist.
     *
     * [vorherige] ist der Stand von vor dem Schreiben; die frisch geschriebene Datei kommt
     * dazu. Damit entfällt eine zweite Abfrage des Ordners — beim Speicheranbieter eines
     * Cloud-Dienstes ist das keine billige Auskunft. Nichts kann sich dazwischen geändert
     * haben: Der ganze Vorgang läuft unter demselben Riegel.
     */
    suspend fun raeumeAlteWeg(vorherige: List<Sicherungsdatei>) = withContext(Dispatchers.IO) {
        // Die frisch geschriebene zählt als die jüngste — vom Rest bleiben BEHALTEN-1 stehen.
        raeumeAuf(vorherige.drop(BEHALTEN - 1))
    }

    /** Alle Sicherungen im Ordner, die jüngste zuerst. */
    suspend fun sicherungen(): List<Sicherungsdatei> = withContext(Dispatchers.IO) {
        val baum = ordner ?: return@withContext emptyList()
        listeAuf(baum, benenneAlteUm = true)
    }

    /**
     * Öffnet die Datei und reicht sie satzweise an [verarbeite] weiter.
     *
     * Bewusst kein `String` als Rückgabe: Eine Sicherung von über einem Megabyte als eine
     * Zeichenkette im Speicher zu halten, nur um sie gleich darauf zu zerlegen, ist genau die
     * Stelle, die bei einem grossen Bestand als erste kippt.
     */
    suspend fun <T> lies(quelle: Uri, verarbeite: suspend (java.io.Reader) -> T): T = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(quelle)?.use { strom ->
            strom.bufferedReader(Charsets.UTF_8).use { leser -> verarbeite(leser) }
        } ?: error("Die Sicherung konnte nicht gelesen werden. Wähl den Ordner neu aus.")
    }

    private fun kinderUri(baum: Uri): Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
        baum,
        DocumentsContract.getTreeDocumentId(baum),
    )

    /**
     * Nur die eigenen Sicherungen zählen — fremde Dateien im Ordner bleiben unangetastet.
     *
     * Mit [benenneAlteUm] bekommt dabei jede Datei aus dem früheren Namensmuster ihren heutigen
     * Namen. Das geschieht beim Sichern und beim Auflisten, nicht als eigener Knopf: Die
     * Umbenennung ist reine Formsache und soll niemandem auffallen müssen.
     */
    private fun listeAuf(baum: Uri, benenneAlteUm: Boolean = false): List<Sicherungsdatei> {
        val gefunden = mutableListOf<Sicherungsdatei>()
        context.contentResolver.query(
                kinderUri(baum),
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE,
                ),
                null,
                null,
                null,
            )?.use { zeiger ->
                while (zeiger.moveToNext()) {
                    val name = zeiger.getString(1) ?: continue
                    if (!gehoertDazu(name)) continue
                    // Eine leer gebliebene Datei (Schreiben abgebrochen) ist keine Sicherung —
                    // sonst gälte sie als „die davor“ und die gute würde weggeräumt.
                    if (!zeiger.isNull(3) && zeiger.getLong(3) == 0L) continue
                    val adresse = DocumentsContract.buildDocumentUriUsingTree(baum, zeiger.getString(0))
                    val gefundene = Sicherungsdatei(
                        uri = adresse,
                        name = name,
                        geaendertAm = zeiger.getLong(2),
                    )
                    gefunden += if (benenneAlteUm) benenneUm(gefundene) else gefundene
                }
            } ?: error("Der Sicherungsordner konnte nicht aufgelistet werden. Bestehende Sicherungen bleiben erhalten.")
        // Verglichen wird der Zeitpunkt aus dem Namen, nie der Name selbst: Beim heutigen
        // Muster steht der Tag vorn, also ordnet die Schreibweise nichts — der 01.10. käme vor
        // dem 20.03. Erst die gelesene Zeit bringt die Reihenfolge, und nur die entscheidet,
        // welche Sicherung als "die davor" stehen bleibt.
        // Den Zeitpunkt je Datei EINMAL lesen. Als Vergleichsschlüssel würde er bei jedem
        // Vergleich neu aus dem Namen geparst — samt frisch gebautem Datumsformat.
        return gefunden
            .map { datei -> datei to (zeitpunktAusNamen(datei.name) ?: datei.geaendertAm) }
            .sortedWith(
                compareByDescending<Pair<Sicherungsdatei, Long>> { it.second }
                    .thenByDescending { it.first.geaendertAm },
            )
            .map { it.first }
    }

    /**
     * Gehört die Datei zu DIESER App?
     *
     * Zwei Muster: das heutige `20-03-2026-1346-opencode-kompass.json` und das frühere
     * `opencode-kompass-2026-03-20-1346Z.json`. Das alte bleibt lesbar, damit eine vor der
     * Umstellung geschriebene Sicherung weiter gefunden und wiederhergestellt werden kann.
     * Fremde Dateien im Ordner bleiben in beiden Fällen unangetastet.
     */
    private fun gehoertDazu(name: String): Boolean =
        name.endsWith(".json") &&
            (MUSTER.any { it.containsMatchIn(name) } || ALTE_PRAEFIXE.any(name::startsWith))

    /**
     * SimpleDateFormat ist nicht threadsicher — deshalb je Aufruf eine eigene Instanz statt
     * einer geteilten. Die Aufrufe kommen aus der Oberfläche und aus der selbsttätigen
     * Sicherung, also aus verschiedenen Fäden; eine geteilte Instanz liefert dann stillschweigend
     * falsche Zeiten.
     */
    private fun zeitpunktAusNamen(name: String): Long? {
        MUSTER.firstNotNullOfOrNull { it.find(name) }?.let { treffer ->
            val format = SimpleDateFormat(ZEIT_MUSTER, Locale.GERMANY).apply { isLenient = false }
            return runCatching { format.parse(treffer.groupValues[1])?.time }.getOrNull()
        }
        val treffer = ALTES_MUSTER.find(name) ?: return null
        val ziffern = treffer.groupValues[1].replace("-", "").padEnd(17, '0')
        val format = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.GERMANY).apply {
            if (treffer.groupValues[2] == "Z") timeZone = UTC
            isLenient = false
        }
        return runCatching { format.parse(ziffern)?.time }.getOrNull()
    }

    /**
     * Hebt eine Sicherung aus dem früheren Namensmuster auf das heutige.
     *
     * Aus `opencode-kompass-2026-03-20-1346Z.json` wird `20-03-2026-1446-opencode-kompass.json`
     * — der alte Name stand in UTC, der neue in Ortszeit, deshalb wird der Zeitpunkt gelesen
     * und neu geschrieben statt die Ziffern umzusortieren. Sonst sähe eine umbenannte Sicherung
     * eine oder zwei Stunden älter aus, als sie ist.
     *
     * Geht das Umbenennen nicht — schreibgeschützter Anbieter, Name schon vergeben —, bleibt
     * die Datei unter ihrem alten Namen liegen. Sie wird weiterhin erkannt und
     * wiederhergestellt; ein misslungener Schreibvorgang darf keine Sicherung verlieren.
     */
    private fun benenneUm(datei: Sicherungsdatei): Sicherungsdatei {
        // Schon im heutigen Muster UND unter dem heutigen Namen? Dann ist nichts zu tun.
        if (HEUTIGES_MUSTER.containsMatchIn(datei.name)) return datei
        val zeitpunkt = zeitpunktAusNamen(datei.name) ?: datei.geaendertAm
        val zeit = SimpleDateFormat(ZEIT_MUSTER, Locale.GERMANY).format(Date(zeitpunkt))
        val neuerName = "$zeit-${namen.dateiPraefix}.json"
        if (neuerName == datei.name) return datei
        return runCatching {
            val adresse = DocumentsContract.renameDocument(context.contentResolver, datei.uri, neuerName)
                ?: return@runCatching datei
            protokoll.info(
                "DateiSicherung",
                "benenneUm",
                "Sicherung auf das heutige Namensmuster gehoben",
                mapOf("vorher" to datei.name, "nachher" to neuerName),
            )
            datei.copy(uri = adresse, name = neuerName)
        }.getOrElse { fehler ->
            protokoll.warn(
                "DateiSicherung",
                "benenneUm",
                "Sicherung behielt ihren alten Namen",
                mapOf("name" to datei.name, "art" to fehler.javaClass.simpleName),
            )
            datei
        }
    }

    private fun raeumeAuf(zuLoeschen: List<Sicherungsdatei>) {
        zuLoeschen.forEach { datei ->
            runCatching {
                check(DocumentsContract.deleteDocument(context.contentResolver, datei.uri)) {
                    "Die ältere Sicherung konnte nicht gelöscht werden. Die neue Datei ist gespeichert, aber es liegen noch mehr als zwei Sicherungen im Ordner."
                }
            }
                .onFailure {
                    // Die neue Sicherung ist geschrieben — eine liegen gebliebene alte ist kein Fehlschlag.
                    protokoll.warn(
                        "DateiSicherung",
                        "raeumeAuf",
                        "Alte Sicherung blieb liegen",
                        mapOf("art" to it.javaClass.simpleName),
                    )
                }
        }
        if (zuLoeschen.isNotEmpty()) {
            protokoll.info(
                "DateiSicherung",
                "raeumeAuf",
                "Alte Sicherungen entfernt",
                mapOf("anzahl" to zuLoeschen.size),
            )
        }
    }

    private fun spalte(dokument: Uri, name: String): String? = runCatching {
        context.contentResolver.query(dokument, arrayOf(name), null, null, null)?.use { zeiger ->
            if (zeiger.moveToFirst()) zeiger.getString(0) else null
        }
    }.getOrNull()

    /**
     * Der Name einer neuen Sicherung: `20-03-2026-1346-opencode-kompass.json`.
     *
     * Zeitpunkt in Ortszeit und in der Reihenfolge, in der man ein Datum liest — der Name soll
     * im Dateiwähler ohne Umrechnen zu erkennen sein. Dass sich damit nicht mehr alphabetisch
     * sortieren lässt, kostet nichts: Die Reihenfolge kommt aus dem gelesenen Zeitpunkt.
     * Der App-Name steht hinten und trennt die Sicherungen der drei Kompass-Apps im selben
     * Ordner voneinander.
     */
    private fun neuerName(bisherige: List<Sicherungsdatei>): String {
        val zeit = SimpleDateFormat(ZEIT_MUSTER, Locale.GERMANY).format(Date())
        val basis = "$zeit-${namen.dateiPraefix}"
        val namen = bisherige.map { it.name }.toSet()
        var name = "$basis.json"
        var nummer = 2
        while (name in namen) name = "$basis (${nummer++}).json"
        return name
    }

    companion object {
        /** Die aktuelle Sicherung und die eine davor — mehr sammelt sich nie an. */
        const val BEHALTEN = 2

        /** Der Zeitpunkt im Dateinamen, in Ortszeit und in Lesereihenfolge. */
        private const val ZEIT_MUSTER = "dd-MM-yyyy-HHmm"

        private fun muster(praefix: String) =
            Regex("^(\\d{2}-\\d{2}-\\d{4}-\\d{4})-${Regex.escape(praefix)}\\b")

        private val ALTES_MUSTER = Regex("(\\d{4}-\\d{2}-\\d{2}-\\d{4}(?:\\d{2}-\\d{3})?)(Z?)")
        private val UTC = java.util.TimeZone.getTimeZone("UTC")
    }
}
