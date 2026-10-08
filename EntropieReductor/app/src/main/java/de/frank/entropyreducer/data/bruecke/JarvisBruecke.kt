package de.frank.entropyreducer.data.bruecke

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.domain.usecase.ForegroundSyncManager
import de.frank.entropyreducer.presentation.amazfit.findRestingHrForWorkoutDay
import de.frank.entropyreducer.presentation.dashboard4.computeVo2MaxOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.TreeMap
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis): NUR LESEND, der gesamte Biomarker-Bereich.
 * Geschützt durch die Signatur-Erlaubnis `de.frank.jarvis.permission.BRUECKE` — nur Apps mit demselben
 * Signierschlüssel kommen durch.
 *
 * Aufruf: `ContentResolver.call(content://<applicationId>.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück.
 *
 * Methoden:
 *  - `katalog`     — alle Messgrößen mit Einheit, Quelle, Zeitraum und Anzahl der Tage
 *  - `tag`         — alle Werte eines Tages (`datum`, Vorgabe: neuester Tag mit Daten) samt Trainings
 *  - `verlauf`     — Zeitreihen (`metriken`, `von`, `bis`, `aufloesung` tag|woche|monat)
 *  - `auswertung`  — aktueller Wert gegen die Durchschnitte der letzten 7/30/90 Tage, Abweichung, Trend
 *  - `trainings`   — Trainingsliste (`von`, `bis`, `limit`, `sport`)
 *  - `training`    — ein Training mit Kilometer-Abschnitten (`id`)
 *  - `abgleich`    — holt vorher frische Daten von Whoop, Oura, Waage und Health Connect (derselbe Abgleich wie
 *                    beim Öffnen des Biomarker-Reiters); wartet höchstens 90 Sekunden. Ändert keine Nutzerdaten.
 *
 * Die Auswertung rechnet hier und nicht in Jarvis: So gehen nur wenige Kennzahlen über die Leitung statt
 * der ganzen Historie, und eine Antwort im Sprachmodus kommt ohne Wartezeit.
 */
class JarvisBruecke : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Zugang {
        fun datenbank(): AppDatabase

        fun abgleich(): ForegroundSyncManager
    }

    /** Eine Messgröße. [richtung]: +1 = höher ist besser, -1 = niedriger ist besser, 0 = ohne Wertung. */
    private class Metrik(val id: String, val name: String, val einheit: String, val quelle: String, val richtung: Int, val stellen: Int = 0)

    private val metriken = listOf(
        Metrik("erholung", "Erholung (Recovery)", "%", "Whoop", 1),
        Metrik("hrv", "HRV", "ms", "Whoop", 1, 1),
        Metrik("ruhepuls", "Ruhepuls", "Schläge/min", "Whoop", -1),
        Metrik("schlafperformance", "Schlafperformance", "%", "Whoop", 1),
        Metrik("schlafdauer", "Schlafzeit", "Stunden", "Whoop", 1, 2),
        Metrik("tiefschlaf_min", "Tiefschlaf", "min", "Whoop", 1),
        Metrik("rem_min", "REM-Schlaf", "min", "Whoop", 1),
        Metrik("leichtschlaf_min", "Leichtschlaf", "min", "Whoop", 0),
        Metrik("wach_min", "Wachzeit im Bett", "min", "Whoop", -1),
        Metrik("tiefschlaf_prozent", "Tiefschlaf-Anteil", "%", "Whoop", 1),
        Metrik("rem_prozent", "REM-Anteil", "%", "Whoop", 1),
        Metrik("leichtschlaf_prozent", "Leichtschlaf-Anteil", "%", "Whoop", 0),
        Metrik("wach_prozent", "Wach-Anteil", "%", "Whoop", -1),
        Metrik("erholsamer_schlaf_prozent", "Erholsamer Schlaf (Tief + REM)", "%", "Whoop", 1),
        Metrik("schlafeffizienz", "Schlafeffizienz", "%", "Whoop", 1),
        Metrik("schlafkonsistenz", "Schlafregelmäßigkeit", "%", "Whoop", 1),
        Metrik("schlafbedarf", "Schlafbedarf", "Stunden", "Whoop", 0, 2),
        Metrik("schlafdefizit_min", "Schlafdefizit", "min", "Whoop", -1),
        Metrik("schlafstoerungen", "Schlafstörungen", "Anzahl", "Whoop", -1),
        Metrik("schlafzyklen", "Schlafzyklen", "Anzahl", "Whoop", 0),
        Metrik("atemfrequenz", "Atemfrequenz", "Atemzüge/min", "Whoop", 0, 1),
        Metrik("spo2", "Sauerstoffsättigung", "%", "Whoop", 1, 1),
        Metrik("hauttemperatur", "Hauttemperatur", "°C", "Whoop", 0, 1),
        Metrik("tagesbelastung", "Tagesbelastung (Strain)", "0–21", "Whoop", 0, 1),
        Metrik("energie_kcal", "Energieumsatz", "kcal", "Whoop", 0),
        Metrik("oura_readiness", "Readiness", "Punkte", "Oura", 1),
        Metrik("oura_schlafscore", "Schlafscore", "Punkte", "Oura", 1),
        Metrik("oura_resilienz", "Resilienz (1 = begrenzt … 5 = außergewöhnlich)", "Stufe", "Oura", 1),
        Metrik("oura_aktivitaet", "Aktivitätsscore", "Punkte", "Oura", 1),
        Metrik("schritte", "Schritte", "Anzahl", "Oura", 1),
        Metrik("oura_temperaturabweichung", "Temperaturabweichung", "°C", "Oura", 0, 2),
        Metrik("vo2max", "VO2max", "ml/kg/min", "Training", 1, 1),
        Metrik("gewicht", "Gewicht", "kg", "Waage", 0, 1),
        Metrik("koerperfett", "Körperfett", "%", "Waage", 0, 1),
        Metrik("muskelmasse", "Muskelmasse", "kg", "Waage", 0, 1),
        Metrik("skelettmuskel", "Skelettmuskelmasse", "kg", "Waage", 0, 1),
        Metrik("magermasse", "Magermasse", "kg", "Waage", 0, 1),
        Metrik("knochenmasse", "Knochenmasse", "kg", "Waage", 0, 2),
        Metrik("viszeralfett", "Viszerales Fett", "Stufe", "Waage", -1, 1),
        Metrik("bmi", "BMI", "kg/m²", "Waage", 0, 1),
        Metrik("koerperwasser", "Körperwasser", "kg", "Waage", 0, 1),
        Metrik("wasseranteil", "Wasseranteil", "%", "Waage", 0, 1),
        Metrik("eiweiss", "Eiweiß", "%", "Waage", 0, 1),
    )
    private val nachId = metriken.associateBy { it.id }

    /** Kennung in der Tabelle der Körperwerte → Messgröße hier. */
    private val waage = mapOf(
        "weight" to "gewicht", "body_fat" to "koerperfett", "muscle_mass" to "muskelmasse",
        "skeletal_muscle_mass" to "skelettmuskel", "lean_body_mass" to "magermasse", "bone_mass" to "knochenmasse",
        "visceral_fat" to "viszeralfett", "bmi" to "bmi", "body_water" to "koerperwasser",
        "body_water_percent" to "wasseranteil", "protein_percent" to "eiweiss",
    )
    private val resilienzStufen = listOf("limited", "adequate", "solid", "strong", "exceptional")
    private val resilienzDeutsch = listOf("begrenzt", "ausreichend", "solide", "stark", "außergewöhnlich")

    /** Was die Auswertung ohne Angabe betrachtet: die Werte, an denen man den Tag einschätzt. */
    private val kern = listOf(
        "erholung", "hrv", "ruhepuls", "schlafdauer", "schlafperformance", "erholsamer_schlaf_prozent",
        "tiefschlaf_min", "rem_min", "oura_readiness", "oura_schlafscore", "oura_resilienz",
        "hauttemperatur", "atemfrequenz", "tagesbelastung", "gewicht", "koerperfett", "vo2max",
    )

    /** Werte, die sich über den Tag aufsummieren und erst am Tagesende feststehen. */
    private val TAGESSUMMEN = listOf("tagesbelastung", "energie_kcal", "schritte", "oura_aktivitaet")

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val antwort = runCatching {
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            runBlocking(Dispatchers.IO) { verarbeite(method, anfrage) }
        }.getOrElse { fehler -> JSONObject().put("fehler", fehler.message ?: fehler.javaClass.simpleName) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    private suspend fun verarbeite(methode: String, a: JSONObject): JSONObject {
        val ctx = context?.applicationContext ?: return fehler("App nicht bereit.")
        val db = EntryPointAccessors.fromApplication(ctx, Zugang::class.java).datenbank()
        val heute = LocalDate.now()
        return when (methode) {
            "abgleich" -> {
                val start = System.currentTimeMillis()
                val fertig = withTimeoutOrNull(90_000) {
                    EntryPointAccessors.fromApplication(ctx, Zugang::class.java).abgleich().syncApisNow("Jarvis")
                    true
                } ?: false
                JSONObject().put("fertig", fertig).put("dauer_ms", System.currentTimeMillis() - start)
            }

            "katalog" -> {
                val reihen = ladeReihen(db, ohneLaufendenTag = false)
                JSONObject().put("heute", heute.toString()).put("metriken", JSONArray(metriken.map { m ->
                    val r = reihen[m.id]
                    JSONObject().put("id", m.id).put("name", m.name).put("einheit", m.einheit).put("quelle", m.quelle)
                        .put("tage", r?.size ?: 0)
                        .put("von", r?.firstKey()?.toString() ?: JSONObject.NULL)
                        .put("bis", r?.lastKey()?.toString() ?: JSONObject.NULL)
                }))
            }

            "tag" -> {
                val reihen = ladeReihen(db, ohneLaufendenTag = false)
                val whoop = db.biomarkerSnapshotDao().getAll().first()
                val tag = datum(a.optString("datum")) ?: reihen.values.mapNotNull { it.lastKey() }.maxOrNull() ?: heute
                val werte = JSONArray()
                metriken.forEach { m ->
                    reihen[m.id]?.get(tag)?.let { wert -> werte.put(wertJson(m, wert)) }
                }
                val trainings = db.amazfitWorkoutDao().observeByDateKey(tag.toString()).first()
                JSONObject().put("heute", heute.toString()).put("datum", tag.toString()).put("werte", werte)
                    .put("trainings", JSONArray(trainings.map { trainingJson(it, false, whoop) }))
                    .apply { if (tag == heute) put("hinweis", "Der Tag läuft noch: Tagesbelastung, Energieumsatz, Schritte und Aktivität sind Zwischenstände.") }
            }

            "verlauf" -> {
                val reihen = ladeReihen(db, ohneLaufendenTag = true)
                val gewuenscht = idListe(a.optJSONArray("metriken")).ifEmpty { return fehler("Es fehlt die Liste der metriken.") }
                gewuenscht.firstOrNull { it !in nachId }?.let { return fehler("Unbekannte Messgröße: $it. Die gültigen ids nennt der Biomarker-Katalog.") }
                val bis = datum(a.optString("bis")) ?: heute
                val von = datum(a.optString("von")) ?: bis.minusDays(29)
                val aufloesung = a.optString("aufloesung", "tag")
                JSONObject().put("heute", heute.toString()).put("von", von.toString()).put("bis", bis.toString()).put("aufloesung", aufloesung)
                    .put("reihen", JSONArray(gewuenscht.map { id ->
                        val m = nachId.getValue(id)
                        val teil = reihen[id]?.subMap(von, true, bis, true) ?: TreeMap()
                        val punkte = when (aufloesung) {
                            "woche" -> teil.entries.groupBy { "${it.key.get(IsoFields.WEEK_BASED_YEAR)}-KW${"%02d".format(it.key.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))}" }
                            "monat" -> teil.entries.groupBy { it.key.toString().take(7) }
                            else -> teil.entries.groupBy { it.key.toString() }
                        }.map { (schluessel, eintraege) -> JSONArray().put(schluessel).put(runde(eintraege.map { it.value }.average(), m.stellen)) }
                        JSONObject().put("id", id).put("name", m.name).put("einheit", m.einheit).put("quelle", m.quelle)
                            .put("anzahl_tage", teil.size).put("punkte", JSONArray(punkte.takeLast(400)))
                    }))
            }

            "auswertung" -> {
                val reihen = ladeReihen(db, ohneLaufendenTag = true)
                val gewuenscht = idListe(a.optJSONArray("metriken")).ifEmpty { kern }
                gewuenscht.firstOrNull { it !in nachId }?.let { return fehler("Unbekannte Messgröße: $it. Die gültigen ids nennt der Biomarker-Katalog.") }
                val bezug = datum(a.optString("datum")) ?: heute
                val fenster = a.optInt("tage", 30).coerceIn(7, 730)
                JSONObject().put("heute", heute.toString()).put("bezugstag", bezug.toString()).put("vergleich_tage", fenster)
                    .put("metriken", JSONArray(gewuenscht.mapNotNull { id -> auswertung(nachId.getValue(id), reihen[id], bezug, fenster) }))
            }

            "trainings" -> {
                val bis = datum(a.optString("bis")) ?: heute
                val von = datum(a.optString("von")) ?: bis.minusDays(89)
                val sport = a.optString("sport").trim().lowercase()
                val zone = ZoneId.systemDefault()
                var liste = db.amazfitWorkoutDao().observeRange(
                    von.atStartOfDay(zone).toInstant().toEpochMilli(),
                    bis.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
                ).first().sortedByDescending { it.startMs }
                if (sport.isNotEmpty()) liste = liste.filter { sport in it.sportName.orEmpty().lowercase() }
                val grenze = a.optInt("limit", 30).coerceIn(1, 200)
                val whoop = db.biomarkerSnapshotDao().getAll().first()
                val laeufe = liste.filter { (it.distanceMeters ?: 0.0) > 0 }
                JSONObject().put("heute", heute.toString()).put("von", von.toString()).put("bis", bis.toString()).put("anzahl", liste.size)
                    .put("summe_km", runde(laeufe.sumOf { it.distanceMeters ?: 0.0 } / 1000, 1))
                    .put("summe_stunden", runde(liste.sumOf { it.durationSeconds ?: 0L } / 3600.0, 1))
                    .put("trainings", JSONArray(liste.take(grenze).map { trainingJson(it, false, whoop) }))
            }

            "training" -> {
                val training = db.amazfitWorkoutDao().getById(a.optString("id")) ?: return fehler("Kein Training mit dieser id.")
                JSONObject().put("training", trainingJson(training, true, db.biomarkerSnapshotDao().getAll().first()))
            }

            else -> fehler("Unbekannte Methode: $methode")
        }
    }

    // ---------------------------------------------------------------- Daten laden

    /** Alle Messgrößen als Tagesreihen. Gibt es an einem Tag mehrere Messungen, zählt die letzte. */
    private suspend fun ladeReihen(db: AppDatabase, ohneLaufendenTag: Boolean): Map<String, TreeMap<LocalDate, Double>> {
        val reihen = HashMap<String, TreeMap<LocalDate, Double>>()
        fun setze(id: String, tag: LocalDate, wert: Number?) {
            if (wert != null) reihen.getOrPut(id) { TreeMap() }[tag] = wert.toDouble()
        }
        val zone = ZoneId.systemDefault()
        fun tagVon(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

        // Whoop: aufsteigend nach Zeit, der spätere Stand eines Tages überschreibt den früheren.
        val whoop = db.biomarkerSnapshotDao().getAll().first()
        whoop.forEach { s ->
            val tag = tagVon(s.capturedAt)
            setze("erholung", tag, s.recoveryScore)
            setze("hrv", tag, s.hrvMs)
            setze("ruhepuls", tag, s.restingHeartRate)
            setze("schlafperformance", tag, s.sleepPerformance)
            setze("schlafdauer", tag, s.sleepTotalMinutes?.let { it / 60.0 })
            setze("tiefschlaf_min", tag, s.sleepDeepMinutes)
            setze("rem_min", tag, s.sleepRemMinutes)
            setze("leichtschlaf_min", tag, s.sleepLightMinutes)
            setze("wach_min", tag, s.sleepAwakeMinutes)
            val tief = s.sleepDeepMinutes
            val rem = s.sleepRemMinutes
            val leicht = s.sleepLightMinutes
            if (tief != null && rem != null && leicht != null) {
                val wach = s.sleepAwakeMinutes ?: 0
                val imBett = (tief + rem + leicht + wach).toDouble()
                if (imBett > 0) {
                    setze("tiefschlaf_prozent", tag, tief * 100 / imBett)
                    setze("rem_prozent", tag, rem * 100 / imBett)
                    setze("leichtschlaf_prozent", tag, leicht * 100 / imBett)
                    setze("wach_prozent", tag, wach * 100 / imBett)
                }
                val schlaf = (tief + rem + leicht).toDouble()
                if (schlaf > 0) setze("erholsamer_schlaf_prozent", tag, (tief + rem) * 100 / schlaf)
            }
            setze("schlafeffizienz", tag, s.sleepEfficiencyPercent)
            setze("schlafkonsistenz", tag, s.sleepConsistencyPercent)
            setze("schlafbedarf", tag, s.sleepNeedMinutes?.let { it / 60.0 })
            setze("schlafdefizit_min", tag, s.sleepDebtMinutes)
            setze("schlafstoerungen", tag, s.sleepDisturbances)
            setze("schlafzyklen", tag, s.sleepCycleCount)
            setze("atemfrequenz", tag, s.respiratoryRate)
            setze("spo2", tag, s.spo2Percent)
            setze("hauttemperatur", tag, s.skinTempCelsius)
            setze("tagesbelastung", tag, s.dayStrain)
            setze("energie_kcal", tag, s.dayKilojoules?.let { it / 4.184 })
        }

        fun tagAus(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
        db.ouraReadinessDao().getAll().first().forEach { r ->
            val tag = tagAus(r.day) ?: return@forEach
            setze("oura_readiness", tag, r.score)
            setze("oura_temperaturabweichung", tag, r.temperatureDeviation)
        }
        db.ouraDailySleepDao().getAll().first().forEach { r -> tagAus(r.day)?.let { setze("oura_schlafscore", it, r.score) } }
        db.ouraResilienceDao().getAll().first().forEach { r ->
            val stufe = resilienzStufen.indexOf(r.level?.lowercase())
            if (stufe >= 0) tagAus(r.day)?.let { setze("oura_resilienz", it, stufe + 1) }
        }
        db.ouraActivityDao().getAll().first().forEach { r ->
            val tag = tagAus(r.day) ?: return@forEach
            setze("oura_aktivitaet", tag, r.score)
            setze("schritte", tag, r.steps)
        }

        // Körperwerte: direkt aus Zepp (Kennung "zepp_…") hat Vorrang vor dem älteren Health-Connect-Stand.
        val koerper = db.healthConnectValueDao().getAll()
        for (direkt in listOf(false, true)) {
            koerper.forEach { zeile ->
                val istDirekt = zeile.metric.startsWith("zepp_")
                if (istDirekt != direkt) return@forEach
                waage[zeile.metric.removePrefix("zepp_")]?.let { id -> setze(id, tagVon(zeile.timestampMs), zeile.value) }
            }
        }

        // VO2max: dieselbe Rechnung wie die Biomarker-Seite (aus Lauf, Puls und Ruhepuls des Tages).
        // Pro Tag zählt das letzte Training.
        db.amazfitWorkoutDao().observeAll().first().sortedBy { it.startMs }.forEach { t ->
            val wert = vo2max(t, whoop) ?: return@forEach
            setze("vo2max", tagAus(t.dateKey) ?: tagVon(t.startMs), wert)
        }

        // Tagessummen wachsen über den Tag. Für Vergleiche zählt deshalb nur, was abgeschlossen ist.
        if (ohneLaufendenTag) {
            val heute = LocalDate.now()
            TAGESSUMMEN.forEach { reihen[it]?.remove(heute) }
        }
        return reihen
    }

    // ---------------------------------------------------------------- Auswertung

    private fun auswertung(m: Metrik, reihe: TreeMap<LocalDate, Double>?, bezug: LocalDate, fenster: Int): JSONObject? {
        val bisBezug = reihe?.headMap(bezug, true)?.takeIf { it.isNotEmpty() } ?: return null
        val letzter = bisBezug.lastEntry() ?: return null
        val alter = ChronoUnit.DAYS.between(letzter.key, bezug)
        fun mittel(von: LocalDate, bis: LocalDate): Double? =
            reihe.subMap(von, true, bis, true).values.takeIf { it.isNotEmpty() }?.average()

        // Verglichen wird der aktuelle Wert mit den Tagen DAVOR, sonst zöge er seinen eigenen Schnitt mit.
        val vorher = reihe.subMap(letzter.key.minusDays(fenster.toLong()), true, letzter.key.minusDays(1), true).values.toList()
        val schnitt = vorher.takeIf { it.isNotEmpty() }?.average()
        val streuung = if (vorher.size >= 5 && schnitt != null) sqrt(vorher.sumOf { (it - schnitt) * (it - schnitt) } / vorher.size) else null
        val abweichung = if (schnitt != null) letzter.value - schnitt else null
        val alleDavor = reihe.headMap(letzter.key, false).values
        val z = if (abweichung != null && streuung != null && streuung > 0) abweichung / streuung else null

        // Trend: die letzten 7 Tage gegen die 7 Tage davor.
        val neu7 = mittel(letzter.key.minusDays(6), letzter.key)
        val alt7 = mittel(letzter.key.minusDays(13), letzter.key.minusDays(7))

        val einordnung = when {
            z == null -> "zu wenige Vergleichstage"
            abs(z) < 0.75 -> "im üblichen Bereich"
            m.richtung == 0 -> if (z > 0) (if (z >= 1.5) "deutlich höher als üblich" else "etwas höher als üblich") else (if (z <= -1.5) "deutlich niedriger als üblich" else "etwas niedriger als üblich")
            z * m.richtung > 0 -> if (abs(z) >= 1.5) "deutlich besser als üblich" else "etwas besser als üblich"
            else -> if (abs(z) >= 1.5) "deutlich schlechter als üblich" else "etwas schlechter als üblich"
        }
        return JSONObject()
            .put("id", m.id).put("name", m.name).put("einheit", m.einheit).put("quelle", m.quelle)
            .put("aktuell", runde(letzter.value, m.stellen))
            .apply { if (m.id == "oura_resilienz") put("aktuell_text", resilienzDeutsch.getOrNull(letzter.value.roundToInt() - 1) ?: "") }
            .put("datum", letzter.key.toString())
            .put("alter_tage", alter)
            .put("schnitt_vergleich", schnitt?.let { runde(it, m.stellen + 1) } ?: JSONObject.NULL)
            .put("abweichung", abweichung?.let { runde(it, m.stellen + 1) } ?: JSONObject.NULL)
            .put("abweichung_prozent", if (abweichung != null && schnitt != null && schnitt != 0.0) runde(abweichung * 100 / abs(schnitt), 0) else JSONObject.NULL)
            .put("einordnung", einordnung)
            .put("schnitt_7", mittel(letzter.key.minusDays(6), letzter.key)?.let { runde(it, m.stellen + 1) } ?: JSONObject.NULL)
            .put("schnitt_30", mittel(letzter.key.minusDays(29), letzter.key)?.let { runde(it, m.stellen + 1) } ?: JSONObject.NULL)
            .put("schnitt_90", mittel(letzter.key.minusDays(89), letzter.key)?.let { runde(it, m.stellen + 1) } ?: JSONObject.NULL)
            .put("schnitt_gesamt", alleDavor.takeIf { it.isNotEmpty() }?.average()?.let { runde(it, m.stellen + 1) } ?: JSONObject.NULL)
            .put("tage_gesamt", alleDavor.size)
            .put("min_vergleich", vorher.minOrNull()?.let { runde(it, m.stellen) } ?: JSONObject.NULL)
            .put("max_vergleich", vorher.maxOrNull()?.let { runde(it, m.stellen) } ?: JSONObject.NULL)
            .put("vergleichstage", vorher.size)
            .put("trend_7_tage", if (neu7 != null && alt7 != null) runde(neu7 - alt7, m.stellen + 1) else JSONObject.NULL)
            .put("hoeher_ist", when (m.richtung) { 1 -> "besser"; -1 -> "schlechter"; else -> "neutral" })
    }

    // ---------------------------------------------------------------- Ausgabe

    private fun wertJson(m: Metrik, wert: Double): JSONObject = JSONObject()
        .put("id", m.id).put("name", m.name).put("wert", runde(wert, m.stellen)).put("einheit", m.einheit).put("quelle", m.quelle)
        .apply { if (m.id == "oura_resilienz") put("text", resilienzDeutsch.getOrNull(wert.roundToInt() - 1) ?: "") }

    private fun vo2max(t: AmazfitWorkoutEntity, whoop: List<BiomarkerSnapshotEntity>): Double? =
        computeVo2MaxOrNull(t, findRestingHrForWorkoutDay(whoop, t.startMs))

    private fun trainingJson(t: AmazfitWorkoutEntity, ausfuehrlich: Boolean, whoop: List<BiomarkerSnapshotEntity>): JSONObject = JSONObject()
        .put("id", t.trackId)
        .put("datum", t.dateKey)
        .put("start", Instant.ofEpochMilli(t.startMs).atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5))
        .put("sport", t.sportName ?: "Training")
        .put("dauer_min", t.durationSeconds?.let { runde(it / 60.0, 0) } ?: JSONObject.NULL)
        .put("distanz_km", t.distanceMeters?.let { runde(it / 1000, 2) } ?: JSONObject.NULL)
        .put("pace_schnitt", pace(t.avgPaceSecPerKm))
        .put("pace_max", pace(t.maxPaceSecPerKm))
        .put("puls_schnitt", t.avgHeartRate ?: JSONObject.NULL)
        .put("puls_max", t.maxHeartRate ?: JSONObject.NULL)
        .put("kalorien", t.calories?.let { runde(it, 0) } ?: JSONObject.NULL)
        .put("vo2max", vo2max(t, whoop)?.let { runde(it, 1) } ?: JSONObject.NULL)
        .apply {
            if (!ausfuehrlich) return@apply
            put("hoehenmeter_auf", t.altitudeGainMeters?.let { runde(it, 0) } ?: JSONObject.NULL)
            put("hoehenmeter_ab", t.altitudeLossMeters?.let { runde(it, 0) } ?: JSONObject.NULL)
            put("trainingseffekt_aerob", t.trainingEffectAerobic ?: JSONObject.NULL)
            put("trainingseffekt_anaerob", t.trainingEffectAnaerobic ?: JSONObject.NULL)
            put("schrittfrequenz", t.cadence ?: JSONObject.NULL)
            put("schrittlaenge_cm", t.strideLengthCm ?: JSONObject.NULL)
            put("erholungszeit_stunden", t.recoveryTimeHours ?: JSONObject.NULL)
            put("ort", t.city ?: JSONObject.NULL)
            put("wetter", listOfNotNull(t.weatherCondition, t.weatherTempCelsius?.let { "$it °C" }).joinToString(", ").ifEmpty { null } ?: JSONObject.NULL)
            // Kilometer-Abschnitte so, wie die App sie gespeichert hat (höchstens 60, damit die Antwort klein bleibt).
            t.splitsJson?.takeIf { it.isNotBlank() }?.let { roh ->
                runCatching { JSONArray(roh) }.getOrNull()?.let { alle ->
                    put("abschnitte", JSONArray((0 until minOf(alle.length(), 60)).map { alle.get(it) }))
                }
            }
        }

    /** Sekunden je Kilometer als „5:42“. */
    private fun pace(sekunden: Double?): Any {
        val s = sekunden?.takeIf { it > 0 && it < 3600 }?.roundToInt() ?: return JSONObject.NULL
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun runde(wert: Double, stellen: Int): Any {
        val faktor = Math.pow(10.0, stellen.toDouble())
        val gerundet = Math.round(wert * faktor) / faktor
        return if (stellen == 0) gerundet.toLong() else gerundet
    }

    private fun datum(text: String): LocalDate? = when (text.trim().lowercase()) {
        "" -> null
        "heute" -> LocalDate.now()
        "gestern" -> LocalDate.now().minusDays(1)
        "vorgestern" -> LocalDate.now().minusDays(2)
        else -> runCatching { LocalDate.parse(text.trim().take(10)) }.getOrNull()
    }

    private fun idListe(array: JSONArray?): List<String> =
        if (array == null) emptyList() else (0 until array.length()).map { array.optString(it).trim().lowercase() }.filter { it.isNotEmpty() }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
