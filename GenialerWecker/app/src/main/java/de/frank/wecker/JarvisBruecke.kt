package de.frank.wecker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis): Wecker lesen UND stellen. Geschützt durch die Signatur-Erlaubnis
 * `de.frank.jarvis.permission.BRUECKE` — nur Apps mit demselben Signierschlüssel kommen durch.
 *
 * Aufruf: `ContentResolver.call(content://de.frank.genialerwecker.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück.
 *
 * Methoden: `lesen`, `optionen`, `stellen` (neu oder, mit `id`, ändern), `schalten`, `auslassen`, `loeschen`.
 *
 * Gespeichert und geplant wird über den [AlarmScheduler] — derselbe Weg wie der Speichern-Knopf der App,
 * mit denselben Prüfungen. Braucht der Weckablauf Sprache (Aufgaben, Ideen, eigener Text), stößt die Brücke
 * danach die Sprachvorbereitung im Hintergrund an.
 */
class JarvisBruecke : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val antwort = runCatching {
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            verarbeite(method, anfrage)
        }.getOrElse { fehler -> JSONObject().put("fehler", fehler.message ?: fehler.javaClass.simpleName) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    private fun verarbeite(methode: String, a: JSONObject): JSONObject {
        val ctx = context?.applicationContext ?: return fehler("App nicht bereit.")
        val store = AlarmStore.get(ctx)
        val planer = AlarmScheduler(ctx)
        return when (methode) {
            "lesen" -> {
                val alle = store.all().sortedWith(compareBy({ !it.enabled }, { if (it.nextAt > 0) it.nextAt else Long.MAX_VALUE }, { it.hour * 60 + it.minute }))
                JSONObject().put("jetzt", Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime().toString().take(16))
                    .put("wecker", JSONArray(alle.map { alsJson(it, store) }))
            }

            "optionen" -> JSONObject()
                .put("ablauf", JSONArray(SCHRITTE.map { (kurz, schritt) -> JSONObject().put("id", kurz).put("name", schritt.title).put("beschreibung", BESCHREIBUNG.getValue(schritt)) }))
                .put("toene", JSONArray(Tones.names.map { (id, name) -> JSONObject().put("id", id).put("name", name) }))
                .put("wiederholung", JSONArray(listOf("einmalig", "taeglich", "wochentage (Liste)", "alle X Tage ab einem Startdatum", "monatlich", "jaehrlich")))
                .put("vorgabe_ablauf", JSONArray(listOf("ton", "aufgaben")))

            "stellen" -> {
                val alt = a.optString("id").takeIf { it.isNotBlank() }?.let { store.get(it) ?: return fehler("Keinen Wecker mit dieser id gefunden.") }
                val basis = alt ?: Alarm(steps = listOf(Step.TONE, Step.TASKS))
                var w = basis

                if (a.has("name")) w = w.copy(name = a.optString("name").trim().take(60).ifEmpty { basis.name })
                if (a.has("uhrzeit")) {
                    val treffer = Regex("^\\s*([01]?\\d|2[0-3])[:.]([0-5]\\d)").find(a.optString("uhrzeit")) ?: return fehler("Uhrzeit nicht verstanden: ${a.optString("uhrzeit")}. Format HH:MM.")
                    w = w.copy(hour = treffer.groupValues[1].toInt(), minute = treffer.groupValues[2].toInt())
                } else if (alt == null) return fehler("Für einen neuen Wecker fehlt die Uhrzeit.")

                // Wiederholung: Wird eine der Angaben gemacht, ersetzt sie den bisherigen Rhythmus vollständig.
                if (a.has("wiederholung") || a.has("wochentage") || a.has("datum") || a.has("intervall_tage")) {
                    val art = a.optString("wiederholung", if (a.has("wochentage")) "wochentage" else if (a.has("intervall_tage")) "intervall" else "einmalig").lowercase(Locale.GERMAN)
                    val datum = if (a.has("datum")) (datumAus(a.optString("datum")) ?: return fehler("Datum nicht verstanden: ${a.optString("datum")}.")) else null
                    w = w.copy(days = emptySet(), startDate = "", endDate = "", intervalDays = 0, repeatUnit = "", repeatEvery = 0)
                    w = when (art) {
                        "einmalig", "einmal", "" -> w.copy(startDate = datum?.toString().orEmpty())
                        "taeglich", "täglich" -> w.copy(days = (1..7).toSet())
                        "werktags" -> w.copy(days = (1..5).toSet())
                        "wochenende" -> w.copy(days = setOf(6, 7))
                        "wochentage" -> w.copy(days = tageAus(a.optJSONArray("wochentage")).ifEmpty { return fehler("Für wiederholung=wochentage fehlt die Liste wochentage.") })
                        "intervall" -> w.copy(intervalDays = a.optInt("intervall_tage", 0).takeIf { it in 1..365 } ?: return fehler("intervall_tage muss zwischen 1 und 365 liegen."), startDate = (datum ?: LocalDate.now()).toString())
                        "monatlich" -> w.copy(repeatUnit = Alarm.MONTHLY, repeatEvery = 1, startDate = (datum ?: return fehler("Für monatlich fehlt das Startdatum (datum).")).toString())
                        "jaehrlich", "jährlich" -> w.copy(repeatUnit = Alarm.YEARLY, repeatEvery = 1, startDate = (datum ?: return fehler("Für jährlich fehlt das Startdatum (datum).")).toString())
                        else -> return fehler("Unbekannte Wiederholung: $art.")
                    }
                }

                if (a.has("ablauf")) {
                    val liste = a.optJSONArray("ablauf") ?: JSONArray()
                    val schritte = (0 until liste.length()).map { n ->
                        val kurz = liste.optString(n).trim().lowercase(Locale.GERMAN)
                        SCHRITTE[kurz] ?: return fehler("Unbekannter Weckschritt: $kurz. Möglich: ${SCHRITTE.keys.joinToString()}.")
                    }.distinct()
                    if (schritte.isEmpty()) return fehler("Der Weckablauf braucht mindestens einen Schritt.")
                    w = w.copy(steps = schritte)
                }
                if (a.has("text")) w = w.copy(text = a.optString("text").trim(), originalText = "")
                if (Step.TEXT in w.steps && w.text.isBlank()) return fehler("Für den Schritt „Eigener Text“ fehlt der Text, den der Wecker vorlesen soll.")
                if (a.has("ton")) w = w.copy(tone = a.optString("ton").takeIf { it in Tones.names } ?: return fehler("Unbekannter Ton. Möglich: ${Tones.names.keys.joinToString()}."))
                if (a.has("lautstaerke")) w = w.copy(volume = a.optInt("lautstaerke", w.volume).coerceIn(1, 100))
                if (a.has("vibration")) w = w.copy(vibrate = a.optBoolean("vibration"))
                if (a.has("schlummer_minuten")) w = w.copy(snoozeMinutes = a.optInt("schlummer_minuten", w.snoozeMinutes).coerceIn(1, 60))
                // Ein neuer Wecker ist an. Beim Ändern bleibt der Zustand, außer aktiv wird ausdrücklich genannt.
                w = w.copy(enabled = if (a.has("aktiv")) a.optBoolean("aktiv") else alt?.enabled ?: true)

                val geplant = planer.save(w, create = alt == null)
                val gespeichert = store.get(w.id) ?: return fehler("Speichern fehlgeschlagen.")
                if (gespeichert.needsSpeech) runCatching { PreparationWorker.enqueue(ctx) }
                JSONObject().put("wecker", alsJson(gespeichert, store)).put("geplant", geplant)
                    .put("hinweis", if (gespeichert.needsSpeech) "Die Sprachfassungen für den Weckablauf werden im Hintergrund vorbereitet." else "")
            }

            "schalten" -> {
                val id = a.optString("id")
                store.get(id) ?: return fehler("Keinen Wecker mit dieser id gefunden.")
                val geplant = planer.setEnabled(id, a.optBoolean("aktiv", true))
                store.get(id)?.takeIf { it.enabled && it.needsSpeech }?.let { runCatching { PreparationWorker.enqueue(ctx) } }
                JSONObject().put("wecker", alsJson(store.get(id)!!, store)).put("geplant", geplant)
            }

            "auslassen" -> {
                val (neu, geplant) = planer.skipNext(a.optString("id"))
                JSONObject().put("wecker", alsJson(neu, store)).put("geplant", geplant)
            }

            "loeschen" -> {
                val wecker = store.get(a.optString("id")) ?: return fehler("Keinen Wecker mit dieser id gefunden.")
                require(wecker.id !in store.ringing()) { "Dieser Wecker klingelt gerade. Beende ihn zuerst." }
                // Erst aus dem Speicher, dann die Planung: wie beim Löschen in der App.
                store.delete(wecker.id); planer.cancel(wecker.id)
                SnoozeNotice.cancel(ctx, wecker.id)
                JSONObject().put("geloescht", alsJson(wecker, store))
            }

            else -> fehler("Unbekannte Methode: $methode")
        }
    }

    private fun alsJson(w: Alarm, store: AlarmStore): JSONObject = JSONObject()
        .put("id", w.id)
        .put("name", w.name)
        .put("uhrzeit", w.timeLabel)
        .put("aktiv", w.enabled)
        .put("naechstes_klingeln", if (w.enabled && w.nextAt > 0) Instant.ofEpochMilli(w.nextAt).atZone(ZoneId.systemDefault()).toLocalDateTime().toString().take(16) else JSONObject.NULL)
        .put("wiederholung", wiederholung(w))
        .put("ablauf", JSONArray(w.steps.map { it.title }))
        .put("text", w.text)
        .put("ton", Tones.names[w.tone] ?: w.tone)
        .put("musik", if (Step.MUSIC in w.steps) w.musicName else "")
        .put("lautstaerke", w.volume)
        .put("vibration", w.vibrate)
        .put("schlummern", "${w.snoozeMinutes} Minuten, höchstens ${w.snoozeLimit}-mal")
        .put("ausgelassen_bis", w.skippedThrough)
        .put("hinweis", store.issues.value[w.id].orEmpty().ifEmpty { w.preparationError })

    private fun wiederholung(w: Alarm): String = when {
        w.repeatUnit == Alarm.MONTHLY -> "jeden ${if (w.repeatEvery > 1) "${w.repeatEvery}. " else ""}Monat ab ${w.startDate}"
        w.repeatUnit == Alarm.YEARLY -> "jedes ${if (w.repeatEvery > 1) "${w.repeatEvery}. " else ""}Jahr ab ${w.startDate}"
        w.intervalDays > 0 -> "alle ${w.intervalDays} Tage ab ${w.startDate}"
        w.days.size == 7 -> "täglich"
        w.days == (1..5).toSet() -> "werktags (Montag bis Freitag)"
        w.days.isNotEmpty() -> w.days.sorted().joinToString(", ") { TAGE[it - 1] }
        w.startDate.isNotBlank() -> "einmalig am ${w.startDate}"
        else -> "einmalig"
    } + if (w.endDate.isNotBlank()) ", bis ${w.endDate}" else ""

    private fun datumAus(wert: String): LocalDate? = when (val w = wert.trim().lowercase(Locale.GERMAN)) {
        "heute" -> LocalDate.now()
        "morgen" -> LocalDate.now().plusDays(1)
        "uebermorgen", "übermorgen" -> LocalDate.now().plusDays(2)
        else -> runCatching { LocalDate.parse(w.take(10)) }.getOrNull()
    }

    /** Wochentage als Zahlen 1 (Montag) bis 7 (Sonntag) oder als Namen bzw. Kürzel. */
    private fun tageAus(liste: JSONArray?): Set<Int> {
        liste ?: return emptySet()
        return (0 until liste.length()).mapNotNull { n ->
            val roh = liste.opt(n)?.toString()?.trim()?.lowercase(Locale.GERMAN).orEmpty()
            roh.toIntOrNull()?.takeIf { it in 1..7 } ?: TAGE.indexOfFirst { it.lowercase(Locale.GERMAN).startsWith(roh.take(2)) && roh.length >= 2 }.takeIf { it >= 0 }?.plus(1)
        }.toSet()
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        val TAGE = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

        /** Kurzname für Jarvis → Weckschritt. Die Reihenfolge der Liste ist die Reihenfolge im Weckablauf. */
        val SCHRITTE = linkedMapOf("ton" to Step.TONE, "aufgaben" to Step.TASKS, "ideen" to Step.IDEAS, "text" to Step.TEXT, "musik" to Step.MUSIC)
        val BESCHREIBUNG = mapOf(
            Step.TONE to "kurzes Klingelzeichen (Standard-Piepton) zum Wachwerden",
            Step.TASKS to "liest die Aufgaben des Tages aus Geniale Aufgaben vor",
            Step.IDEAS to "liest die offenen Ideen aus Geniale Ideen vor",
            Step.TEXT to "liest einen eigenen Text vor (Feld text nötig)",
            Step.MUSIC to "spielt den Weckton bzw. die in der App gewählte MP3",
        )
    }
}
