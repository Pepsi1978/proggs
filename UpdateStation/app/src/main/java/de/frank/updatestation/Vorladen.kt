package de.frank.updatestation

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Lädt gefundene Updates schon im Hintergrund herunter, damit beim Tippen auf „Aktualisieren“ nur noch
 * die Übergabe an Android bleibt. Die Dateien liegen in filesDir (cacheDir räumt Android selbst leer)
 * unter `<paket>-vc<versionCode>.apk`, jeweils erst nach geprüfter Prüfsumme umbenannt.
 */
object Vorladen {
    private val sperre = Mutex()

    private fun ordner(context: Context) = File(context.filesDir, "vorgeladen").apply { mkdirs() }

    private fun datei(context: Context, m: UpdateManifest) = File(ordner(context), "${m.paket}-vc${m.versionCode}.apk")

    /** Die vorgeladene APK für genau diese Version, wenn sie vollständig da ist (Größe laut update.json). */
    fun vorhanden(context: Context, m: UpdateManifest): File? {
        if (!PAKET_MUSTER.matches(m.paket)) return null
        return datei(context, m).takeIf { it.isFile && (m.groesse <= 0 || it.length() == m.groesse) }
    }

    fun istVorgeladen(context: Context, datei: File) = datei.parentFile == ordner(context)

    /** Nach erfolgreicher Installation: alle vorgeladenen Fassungen des Pakets löschen. */
    fun entferne(context: Context, paket: String) {
        ordner(context).listFiles()?.filter { it.name.startsWith("$paket-vc") }?.forEach { it.delete() }
    }

    /**
     * Lädt jedes Update (Status UPDATE) vor, das noch nicht da ist, und räumt überholte Dateien weg.
     * Liefert die Einträge, die in diesem Lauf neu fertig geworden sind.
     */
    suspend fun lade(context: Context, eintraege: List<AppEintrag>): List<AppEintrag> = sperre.withLock {
        withContext(Dispatchers.IO) {
            val updates = eintraege.filter { it.status == Status.UPDATE && it.fund.apkRef != null && PAKET_MUSTER.matches(it.paket) }
            val behalten = updates.map { datei(context, it.fund.manifest).name }.toSet()
            ordner(context).listFiles()?.filter { it.name !in behalten }?.forEach { it.delete() }
            val quelle = Quellen.aktuelle(context) ?: return@withContext emptyList()
            val neu = mutableListOf<AppEintrag>()
            for (e in updates) {
                val m = e.fund.manifest
                if (vorhanden(context, m) != null || Installierer.laeuft(m.paket)) continue
                val ziel = datei(context, m)
                val teil = File(ziel.path + ".part")
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    quelle.oeffne(e.fund).use { ein ->
                        teil.outputStream().use { aus ->
                            val puffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = ein.read(puffer)
                                if (n < 0) break
                                aus.write(puffer, 0, n)
                                digest.update(puffer, 0, n)
                            }
                        }
                    }
                    val hash = digest.digest().joinToString("") { "%02x".format(it) }
                    if (hash != m.sha256) {
                        // Meist ist Drive noch beim Hochladen: beim nächsten Signal oder Prüflauf erneut.
                        Log.i(TAG, "${m.paket}: Vorladen – Prüfsumme passt noch nicht")
                        Diagnose.ereignis(context, Phase.DOWNLOAD, "VORLADEN_HASH", "paket" to m.paket, "vc" to m.versionCode)
                        continue
                    }
                    if (!teil.renameTo(ziel)) continue
                    Log.i(TAG, "${m.paket}: vc ${m.versionCode} vorgeladen")
                    Diagnose.ereignis(context, Phase.DOWNLOAD, "VORGELADEN", "paket" to m.paket, "vc" to m.versionCode)
                    neu += e.copy(vorgeladen = true)
                } catch (c: CancellationException) {
                    throw c
                } catch (f: Exception) {
                    Log.w(TAG, "${m.paket}: Vorladen fehlgeschlagen", f)
                    Diagnose.ereignis(context, Phase.DOWNLOAD, "VORLADEN_FEHLER", "paket" to m.paket, "klasse" to Diagnose.klasse(f))
                } finally {
                    teil.delete()
                }
            }
            neu
        }
    }

    /** Lässt Jarvis die fertigen Updates ansagen (Jarvis prüft die Signatur über die Erlaubnis BRUECKE). */
    fun sageAn(context: Context, fertig: List<AppEintrag>) {
        if (fertig.isEmpty()) return
        val namen = fertig.map { it.label }
        val text = if (namen.size == 1) "Ein Update von ${namen[0]} ist bereit in UpdateStation."
            else "Updates von ${namen.dropLast(1).joinToString(", ")} und ${namen.last()} sind bereit in UpdateStation."
        val ansage = Intent("de.frank.jarvis.ANSAGE").setPackage("de.frank.jarvis")
            .putExtra("titel", "UpdateStation")
            .putExtra("text", text)
        runCatching { context.sendBroadcast(ansage) }.onFailure { Log.w(TAG, "Ansage an Jarvis nicht möglich", it) }
    }
}
