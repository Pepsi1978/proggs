package de.frank.stacklabor.werftstudio.bruecke

import de.frank.stacklabor.werftstudio.data.repository.StackLaborRepository
import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.EigeneFrage
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackEintragDosis
import de.frank.stacklabor.werftstudio.domain.model.StackZiel
import de.frank.stacklabor.werftstudio.domain.model.Ziel
import de.frank.stacklabor.werftstudio.domain.model.anzeige
import de.frank.stacklabor.werftstudio.domain.model.darreichungsformAnzeige
import de.frank.stacklabor.werftstudio.domain.model.einheitAnzeige
import de.frank.stacklabor.werftstudio.domain.model.frequenzAnzeige
import de.frank.stacklabor.werftstudio.domain.model.toDarreichungsformOrNull
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die Fachlogik der Jarvis-Brücke: liest und ändert Stacks und Mittel über das Repository der App (dieselben Prüfungen,
 * Transaktionen und Änderungssignale wie die Oberfläche). Jede Antwort ist JSON mit einem fertigen deutschen `text`;
 * fachliche Fehler kommen als `{"fehler": …}`, eine nötige Rückfrage zusätzlich mit `rueckfrage: true`.
 *
 * Regeln:
 * - Jede Lesung geht frisch an die Datenbank, auch direkt vor jeder Änderung.
 * - Alle schreibenden Methoden laufen nacheinander ([schreibSperre]): Jarvis ruft parallel auf, und ein Mittel darf je Stack
 *   nur einmal vorkommen.
 * - Wiederholte Aufrufe legen nichts doppelt an (gleicher Name = gleiches Mittel, gleicher Eintrag = keine Änderung).
 * - Gesperrte Stacks (Schloss in der App) bleiben unverändert; entsperren kann nur Frank in der App.
 * - Löschen nur mit `bestaetigt: true`; ohne kommt die Rückfrage mit allem, was verloren ginge.
 * - Nach jeder Änderung wird nachgelesen; die Antwort zeigt den gespeicherten Stand, nicht die Eingabe.
 */
class JarvisStackDienst(
    private val repository: StackLaborRepository,
    private val neueId: () -> String = { UUID.randomUUID().toString() },
    /** Nach dem Anlegen eines Mittels ohne Löslichkeit: die App bestimmt sie wie in der Oberfläche mit ihrer KI. */
    private val nachNeuemMittel: (Mittel) -> Unit = {},
) {
    private val schreibSperre = Mutex()

    suspend fun verarbeite(methode: String, a: JSONObject): JSONObject = when (methode) {
        "status" -> JSONObject().put("stacks", repository.beobachteStacks().first().size).put("text", "Stack-Labor ist erreichbar.")
        "stacks" -> stacks(a.optBoolean("voll", false))
        "stack" -> stack(a)
        "mittel" -> mittel(a)
        "ziele" -> ziele()
        "eintrag" -> schreibSperre.withLock { eintrag(a) }
        "stack_speichern" -> schreibSperre.withLock { stackSpeichern(a) }
        "mittel_speichern" -> schreibSperre.withLock { mittelSpeichern(a) }
        "loeschen" -> schreibSperre.withLock { loeschen(a) }
        else -> fehler("Unbekannte Methode: $methode")
    }

    // ------------------------------------------------------------------ Lesen

    /** Alles auf einmal frisch gelesen: wenige Abfragen, egal wie viele Einträge. */
    private class Bestand(
        val stacks: List<Stack>,
        val mittel: Map<String, Mittel>,
        val eintraege: List<StackEintrag>,
        val ziele: Map<String, Ziel>,
        val stackZiele: List<StackZiel>,
    ) {
        fun eintraegeVon(stackId: String) = eintraege.filter { it.stackId == stackId }.sortedWith(compareBy({ it.reihenfolge }, { it.id }))
        fun zieleVon(stackId: String) = stackZiele.filter { it.stackId == stackId }.sortedBy { it.rang }
        fun position(stack: Stack) = stacks.indexOfFirst { it.id == stack.id } + 1
    }

    private suspend fun bestand(): Bestand = Bestand(
        stacks = repository.beobachteStacks().first().sortedWith(compareBy({ it.sortierung }, { it.id })),
        mittel = repository.beobachteMittel().first().associateBy { it.id },
        eintraege = repository.beobachteAlleEintraege().first(),
        ziele = repository.beobachteZiele().first().associateBy { it.id },
        stackZiele = repository.beobachteAlleStackZiele().first(),
    )

    private suspend fun stacks(voll: Boolean): JSONObject {
        val b = bestand()
        val text = buildString {
            if (b.stacks.isEmpty()) { append("In Stack-Labor gibt es noch keine Stacks."); return@buildString }
            append(b.stacks.size).append(" Stacks in Stack-Labor, in der Reihenfolge der App. ")
            append("Das ist Franks geplante Einnahmeroutine, kein Nachweis einer Einnahme; nur aktive (angehakte) Mittel gehören dazu.\n")
            if (voll) b.stacks.forEach { append('\n').append(stackText(b, it, mitFragen = emptyList())) }
            else b.stacks.forEach { s ->
                val e = b.eintraegeVon(s.id)
                append(b.position(s)).append(". ").append(s.name).append(" [id ").append(s.id).append("]")
                beschreibung(s)?.let { append(" — ").append(it) }
                append(" · ").append(e.size).append(" Mittel, davon ").append(e.count { it.aktiv }).append(" aktiv")
                if (s.gesperrt) append(" · gesperrt")
                append('\n')
            }
        }.trim()
        return JSONObject().put("text", text).put("stacks", JSONArray(b.stacks.map { s ->
            val e = b.eintraegeVon(s.id)
            JSONObject().put("id", s.id).put("name", s.name).put("position", b.position(s)).put("zeitpunkt", s.zeitpunkt)
                .put("einnahme_hinweis", s.einnahmeHinweis).put("gesperrt", s.gesperrt).put("mittel", e.size).put("aktiv", e.count { it.aktiv })
        }))
    }

    private suspend fun stack(a: JSONObject): JSONObject {
        val b = bestand()
        val s = when (val r = findeStack(a.optString("stack"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
        val fragen = repository.beobachteFragen(s.id).first()
        val text = StringBuilder(stackText(b, s, fragen))
        if (a.optBoolean("auswertung", false)) {
            val bewertung = repository.beobachteNeuesteBewertung(s.id).first()
            text.append("\n\nLetzte gespeicherte Auswertung in Stack-Labor: ")
            if (bewertung == null) text.append("keine.")
            else {
                text.append(datum(bewertung.bewertung.zeitpunktEpochMillis)).append('\n').append(bewertung.bewertung.gesamt.trim().let { if (it.length > 4_000) it.take(4_000) + " …" else it })
                bewertung.bewertung.hinweise.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { text.append("\nHinweise: ").append(it.joinToString("; ")) }
            }
        }
        return JSONObject().put("text", text.toString().trim()).put("stack", JSONObject().put("id", s.id).put("name", s.name))
    }

    private suspend fun mittel(a: JSONObject): JSONObject {
        val b = bestand()
        val suche = a.optString("mittel").trim()
        if (suche.isEmpty()) {
            val text = buildString {
                append(b.mittel.size).append(" Mittel im Katalog von Stack-Labor:\n")
                b.mittel.values.sortedBy { it.name.lowercase(Locale.GERMAN) }.forEach { m ->
                    append("- ").append(m.name).append(" (").append(m.darreichungsformAnzeige()).append(", ").append(m.loeslichkeit.anzeige()).append(")")
                    val wo = b.eintraege.filter { it.mittelId == m.id }
                    append(if (wo.isEmpty()) " — in keinem Stack" else " — in: " + wo.joinToString(", ") { e -> stackName(b, e.stackId) + if (e.aktiv) "" else " (inaktiv)" })
                    append('\n')
                }
            }.trim()
            return JSONObject().put("text", text)
        }
        val m = when (val r = findeMittel(suche, b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
        return JSONObject().put("text", mittelText(b, m)).put("mittel", JSONObject().put("id", m.id).put("name", m.name))
    }

    private suspend fun ziele(): JSONObject {
        val b = bestand()
        val text = buildString {
            append(b.ziele.size).append(" Ziele im Ziel-Katalog:\n")
            b.ziele.values.sortedBy { it.text.lowercase(Locale.GERMAN) }.forEach { z ->
                val wo = b.stackZiele.filter { it.zielId == z.id }
                append("- ").append(z.text).append(if (wo.isEmpty()) " (in keinem Stack)" else " — in: " + wo.joinToString(", ") { stackName(b, it.stackId) + " (Rang ${it.rang})" }).append('\n')
            }
        }.trim()
        return JSONObject().put("text", text)
    }

    // ------------------------------------------------------------------ Mittel in einem Stack

    private suspend fun eintrag(a: JSONObject): JSONObject {
        val b = bestand()
        val s = when (val r = findeStack(a.optString("stack"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
        val aktion = a.optString("aktion", "hinzufuegen").ifBlank { "hinzufuegen" }
        if (s.gesperrt) return gesperrt(s)
        val mittelName = a.optString("mittel").trim()
        if (mittelName.isEmpty()) return fehler("Welches Mittel? Der Name fehlt.", rueckfrage = true)

        // Das Mittel zuerst unter den Einträgen dieses Stacks suchen (so trifft „Ashwagandha“ den Eintrag im Stack, auch wenn
        // der Katalog ein zweites, ähnlich benanntes Mittel hat), danach im ganzen Katalog.
        val imStack = b.eintraegeVon(s.id).mapNotNull { e -> b.mittel[e.mittelId]?.let { Kandidat(it.id, it.name) } }
        val ausStack = Namensaufloesung.mittel(mittelName, imStack)
        if (ausStack is Aufloesung.Mehrdeutig) return mehrdeutig("Mittel in ${s.name}", mittelName, ausStack.kandidaten)
        val mittel: Mittel? = (ausStack as? Aufloesung.Eindeutig)?.let { b.mittel[it.kandidat.id] } ?: when (val r = Namensaufloesung.mittel(mittelName, b.mittel.values.map { Kandidat(it.id, it.name) })) {
            is Aufloesung.Eindeutig -> b.mittel[r.kandidat.id]
            is Aufloesung.Mehrdeutig -> return mehrdeutig("Mittel", mittelName, r.kandidaten)
            is Aufloesung.Keiner -> null
        }
        val vorhanden = mittel?.let { m -> b.eintraegeVon(s.id).firstOrNull { it.mittelId == m.id } }

        return when (aktion) {
            "hinzufuegen" -> hinzufuegen(a, b, s, mittel, mittelName, vorhanden)
            "aendern" -> if (vorhanden == null) nichtImStack(s, mittelName, mittel) else aendern(a, b, s, mittel!!, vorhanden)
            "aktivieren", "deaktivieren" -> {
                if (vorhanden == null) return nichtImStack(s, mittelName, mittel)
                val soll = aktion == "aktivieren"
                if (vorhanden.aktiv == soll) return ok("${mittel!!.name} in ${s.name} war schon ${if (soll) "aktiv" else "inaktiv"}; nichts geändert.\n" + eintragZeile(b, frisch(s.id, mittel.id) ?: vorhanden))
                repository.setzeEintragAktiv(vorhanden.id, soll)
                val neu = frisch(s.id, mittel!!.id) ?: return fehler("Nach dem Speichern nicht mehr lesbar; bitte in der App prüfen.")
                if (neu.aktiv != soll) return fehler("Stack-Labor hat die Änderung nicht übernommen.")
                ok("Gespeichert und nachgelesen: ${mittel.name} in ${s.name} ist jetzt ${if (soll) "aktiv" else "inaktiv (ausgeschaltet, gehört nicht mehr zur geplanten Einnahme)"}.\n" + eintragZeile(bestand(), neu))
            }
            "kopieren", "verschieben" -> {
                if (vorhanden == null) return nichtImStack(s, mittelName, mittel)
                val ziel = when (val r = findeStack(a.optString("ziel_stack"), b, "Ziel-Stack")) { is Gefunden -> r.wert; is Antwort -> return r.json }
                if (ziel.id == s.id) return fehler("Quell- und Ziel-Stack sind gleich.", rueckfrage = true)
                if (ziel.gesperrt) return gesperrt(ziel)
                val schonDa = b.eintraegeVon(ziel.id).firstOrNull { it.mittelId == mittel!!.id }
                if (schonDa != null) return ok("${mittel!!.name} ist schon in ${ziel.name}; nichts ${if (aktion == "kopieren") "kopiert" else "verschoben"}.\n" + eintragZeile(b, schonDa))
                repository.uebertrageMittel(s.id, ziel.id, mittel!!.id, verschieben = aktion == "verschieben")
                val neu = frisch(ziel.id, mittel.id) ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
                ok("Gespeichert und nachgelesen: ${mittel.name} ${if (aktion == "kopieren") "zusätzlich in" else "verschoben nach"} ${ziel.name}.\n" + eintragZeile(bestand(), neu))
            }
            else -> fehler("Unbekannte Aktion „$aktion“. Erlaubt: hinzufuegen, aendern, aktivieren, deaktivieren, kopieren, verschieben.")
        }
    }

    private suspend fun hinzufuegen(a: JSONObject, b: Bestand, s: Stack, gefunden: Mittel?, name: String, vorhanden: StackEintrag?): JSONObject {
        val ohneDosis = a.optBoolean("ohne_dosis", false)
        val dosisAngaben = hatDosis(a)
        val dosis = if (dosisAngaben) when (val d = DosisEingabe.lies(a.str("menge"), a.str("menge_je_stueck"), a.str("stueckzahl"), a.str("einheit"))) {
            is DosisEingabe.Ergebnis.Gut -> d.dosis
            is DosisEingabe.Ergebnis.Fehlt -> return fehler(d.frage, rueckfrage = true)
        } else null

        if (vorhanden != null) {
            val m = gefunden!!
            val variante = if (a.optString("variante") == "dienst") DosisVariante.DIENST else DosisVariante.FREI
            val bisher = vorhanden.dosen.firstOrNull { it.variante == variante }
            if (dosis != null && !gleicheDosis(bisher, dosis)) {
                return fehler(
                    "${m.name} ist schon in ${s.name} (${dosisText(m, bisher)}). Nichts geändert. Soll die Dosis auf ${dosisText(m, dosis)} geändert werden? " +
                        "Dann nach Franks Bestätigung mit aktion „aendern“ aufrufen.",
                    rueckfrage = true,
                )
            }
            if (!vorhanden.aktiv && a.optBoolean("aktiv", true)) {
                repository.setzeEintragAktiv(vorhanden.id, true)
                val neu = frisch(s.id, m.id) ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
                return ok("${m.name} war schon in ${s.name}, aber ausgeschaltet. Jetzt wieder aktiv (gespeichert und nachgelesen).\n" + eintragZeile(bestand(), neu))
            }
            return ok("${m.name} ist bereits genau so in ${s.name}; nichts doppelt angelegt.\n" + eintragZeile(b, vorhanden))
        }

        if (dosis == null && !ohneDosis) {
            return fehler(
                "Für ${gefunden?.name ?: name} in ${s.name} fehlt die Dosis. Bitte Frank nach Menge mit Einheit und gegebenenfalls Kapselanzahl fragen; " +
                    "nur wenn er ausdrücklich ohne Dosis eintragen will, ohne_dosis setzen.",
                rueckfrage = true,
            )
        }

        // Mittel aus dem Katalog nehmen oder (nur auf ausdrücklichen Wunsch) neu anlegen.
        var neuAngelegt = false
        val mittel = gefunden ?: run {
            val katalog = b.mittel.values.map { Kandidat(it.id, it.name) }
            if (!a.optBoolean("neues_mittel", false)) {
                val aehnlich = (Namensaufloesung.aehnlich(name, katalog) + (Namensaufloesung.mittel(name, katalog) as? Aufloesung.Keiner)?.aehnlich.orEmpty()).distinctBy { it.id }
                return fehler(
                    "„$name“ steht noch nicht im Mittel-Katalog von Stack-Labor." +
                        (if (aehnlich.isNotEmpty()) " Ähnlich: " + aehnlich.take(5).joinToString(", ") { it.name } + "." else "") +
                        " Frage Frank, ob eines davon gemeint ist oder ob „$name“ neu angelegt werden soll (dann neues_mittel: true).",
                    rueckfrage = true,
                )
            }
            when (val m = neuesMittel(name, a, b)) { is Gefunden -> m.wert.also { neuAngelegt = m.neu }; is Antwort -> return m.json }
        }

        val id = neueId()
        val dosen = dosenFuer(id, a.optString("variante", "frei"), dosis ?: Dosis(BigDecimal.ONE, null, null), alt = emptyList())
            ?: return fehler("Variante „${a.optString("variante")}“ geht so nicht: erlaubt sind frei, dienst und beide; für „dienst“ allein fehlt die Dosis an freien Tagen.", rueckfrage = true)
        val frequenz = frequenz(a) ?: return fehler("Die Frequenz „${a.optString("frequenz")}“ verstehe ich nicht (täglich oder alle N Tage).", rueckfrage = true)
        val eintraege = b.eintraegeVon(s.id)
        repository.speichereEintrag(
            StackEintrag(
                id = id,
                stackId = s.id,
                mittelId = mittel.id,
                frequenzTyp = frequenz.first,
                alleNTage = frequenz.second,
                frequenzText = frequenz.third,
                aktiv = a.optBoolean("aktiv", true),
                reihenfolge = (eintraege.maxOfOrNull { it.reihenfolge } ?: 0) + 1,
                zusatztext = a.textOderLeer("notiz").orEmpty(),
                dosen = dosen,
            ),
        )
        a.position()?.let { verschiebe(s.id, id, it) }
        val neu = frisch(s.id, mittel.id) ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
        return ok(
            (if (neuAngelegt) "Neues Mittel „${mittel.name}“ im Katalog angelegt. " else "") +
                "Gespeichert und nachgelesen: ${mittel.name} ist jetzt in ${s.name}.\n" + eintragZeile(bestand(), neu),
        )
    }

    private suspend fun aendern(a: JSONObject, b: Bestand, s: Stack, m: Mittel, alt: StackEintrag): JSONObject {
        var dosen = alt.dosen
        if (hatDosis(a)) {
            val variante = a.optString("variante", "frei").ifBlank { "frei" }
            val bezug = alt.dosen.firstOrNull { it.variante == if (variante == "dienst") DosisVariante.DIENST else DosisVariante.FREI }
                ?: alt.dosen.firstOrNull { it.variante == DosisVariante.FREI }
            // Nur Stückzahl oder nur Menge genannt: Was mit dem anderen Teil passiert, wäre geraten. Dann nachfragen.
            if (a.str("stueckzahl") != null && a.str("menge") == null && a.str("menge_je_stueck") == null && bezug?.mengeJeStueck != null && !a.optBoolean("menge_je_stueck_beibehalten", false)) {
                return fehler(
                    "Bisher: ${dosisText(m, bezug)}. Soll es bei ${DosisEingabe.schoen(bezug.mengeJeStueck)} ${bezug.einheitAnzeige()} je Stück bleiben (dann menge_je_stueck_beibehalten: true) " +
                        "oder ändert sich die Gesamtmenge (dann menge mit Einheit mitgeben)? Bitte bei Frank nachfragen.",
                    rueckfrage = true,
                )
            }
            val stueck = a.str("stueckzahl") ?: if (a.str("menge") != null && bezug != null && bezug.stueckzahl.compareTo(BigDecimal.ONE) != 0) {
                return fehler("Bisher: ${dosisText(m, bezug)}. Bleibt es bei ${DosisEingabe.schoen(bezug.stueckzahl)} Stück? Bitte die Stückzahl mitgeben.", rueckfrage = true)
            } else null
            val jeStueck = a.str("menge_je_stueck") ?: if (a.optBoolean("menge_je_stueck_beibehalten", false)) bezug?.mengeJeStueck?.toPlainString() else null
            val einheit = a.str("einheit") ?: if (jeStueck != null && a.str("menge_je_stueck") == null) bezug?.einheit?.let(DosisEingabe::anzeige) else null
            val d = when (val r = DosisEingabe.lies(a.str("menge"), jeStueck, stueck, einheit)) {
                is DosisEingabe.Ergebnis.Gut -> r.dosis
                is DosisEingabe.Ergebnis.Fehlt -> return fehler(r.frage, rueckfrage = true)
            }
            dosen = dosenFuer(alt.id, variante, d, alt.dosen) ?: return fehler("Unbekannte Variante „$variante“ (frei, dienst oder beide).", rueckfrage = true)
        }
        val frequenz = if (a.gesetzt("frequenz")) frequenz(a) ?: return fehler("Die Frequenz „${a.optString("frequenz")}“ verstehe ich nicht (täglich oder alle N Tage).", rueckfrage = true)
        else Triple(alt.frequenzTyp, alt.alleNTage, alt.frequenzText)
        val neu = alt.copy(
            dosen = dosen,
            frequenzTyp = frequenz.first,
            alleNTage = frequenz.second,
            frequenzText = frequenz.third,
            // Ein- und Ausschalten nur über die Aktionen aktivieren/deaktivieren, nie nebenbei beim Ändern.
            zusatztext = a.textOderLeer("notiz") ?: alt.zusatztext,
        )
        val position = a.position()
        val vorherPosition = b.eintraegeVon(s.id).indexOfFirst { it.id == alt.id } + 1
        if (neu == alt && (position == null || position == vorherPosition)) return ok("${m.name} in ${s.name} ist bereits so gespeichert; nichts geändert.\n" + eintragZeile(b, alt))
        if (neu != alt) repository.speichereEintrag(neu)
        if (position != null && position != vorherPosition) verschiebe(s.id, alt.id, position)
        val gelesen = frisch(s.id, m.id) ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
        return ok("Gespeichert und nachgelesen: ${m.name} in ${s.name} geändert.\nVorher: " + eintragZeile(b, alt) + "\nJetzt: " + eintragZeile(bestand(), gelesen))
    }

    /** Setzt die Dosen einer Variante; die übrigen bleiben. null = Variante unbekannt oder freie Dosis fehlt. */
    private fun dosenFuer(eintragId: String, variante: String, d: Dosis, alt: List<StackEintragDosis>): List<StackEintragDosis>? {
        fun dosis(v: DosisVariante) = StackEintragDosis(eintragId, v, d.stueckzahl, d.mengeJeStueck, d.einheit, null)
        val ziel = when (variante.ifBlank { "frei" }) {
            "frei" -> listOf(DosisVariante.FREI)
            "dienst" -> listOf(DosisVariante.DIENST)
            "beide" -> listOf(DosisVariante.FREI, DosisVariante.DIENST)
            else -> return null
        }
        val neu = alt.filter { it.variante !in ziel } + ziel.map(::dosis)
        if (neu.none { it.variante == DosisVariante.FREI }) return null
        return neu.sortedBy { it.variante.code }
    }

    private fun hatDosis(a: JSONObject) = listOf("menge", "menge_je_stueck", "stueckzahl", "einheit").any { a.str(it) != null }

    /** Gleiche Dosis? Stückzahl gleich und Gesamtmenge gleich (g und mg werden dafür verglichen, nicht gespeichert). */
    private fun gleicheDosis(alt: StackEintragDosis?, neu: Dosis): Boolean {
        if (alt == null) return false
        if (alt.stueckzahl.compareTo(neu.stueckzahl) != 0) return false
        if (alt.mengeJeStueck == null || neu.mengeJeStueck == null) return alt.mengeJeStueck == null && neu.mengeJeStueck == null
        val a = DosisEingabe.inMilligramm(alt.mengeJeStueck, alt.einheit)
        val n = DosisEingabe.inMilligramm(neu.mengeJeStueck, neu.einheit)
        return if (a != null && n != null) a.compareTo(n) == 0 else alt.einheit == neu.einheit && alt.mengeJeStueck.compareTo(neu.mengeJeStueck) == 0
    }

    private fun frequenz(a: JSONObject): Triple<FrequenzTyp, Int?, String?>? {
        val roh = a.optString("frequenz").trim()
        if (roh.isEmpty() || roh.lowercase(Locale.GERMAN) in setOf("täglich", "taeglich", "jeden tag", "1x täglich")) return Triple(FrequenzTyp.TAEGLICH, null, null)
        val tage = Regex("\\d+").find(roh)?.value?.toIntOrNull() ?: DosisEingabe.zahl(roh.lowercase(Locale.GERMAN).removePrefix("alle").removeSuffix("tage").trim())?.toInt()
        if (roh.contains("alle", true) && tage != null && tage >= 2) return Triple(FrequenzTyp.ALLE_N_TAGE, tage, "Alle $tage Tage")
        return if (roh.length in 1..60) Triple(FrequenzTyp.TAEGLICH, null, roh) else null
    }

    private suspend fun verschiebe(stackId: String, eintragId: String, position: Int) {
        val liste = repository.beobachteAlleEintraege().first().filter { it.stackId == stackId }.sortedWith(compareBy({ it.reihenfolge }, { it.id })).toMutableList()
        val e = liste.firstOrNull { it.id == eintragId } ?: return
        liste.remove(e)
        liste.add((position - 1).coerceIn(0, liste.size), e)
        repository.sortiereEintraege(stackId, liste.map { it.id })
    }

    // ------------------------------------------------------------------ Stacks anlegen und ändern

    private suspend fun stackSpeichern(a: JSONObject): JSONObject {
        val b = bestand()
        val name = a.optString("stack").trim()
        if (name.isEmpty()) return fehler("Welcher Stack? Der Name fehlt.", rueckfrage = true)
        if (a.optBoolean("neu", false)) {
            b.stacks.firstOrNull { Namensaufloesung.gleich(it.name, name) }?.let { return ok("Den Stack „${it.name}“ gibt es schon; nichts doppelt angelegt.\n" + stackText(b, it, emptyList())) }
            val aehnlich = Namensaufloesung.aehnlich(name, b.stacks.map { Kandidat(it.id, it.name) })
            if (aehnlich.isNotEmpty() && !a.optBoolean("trotzdem_neu", false)) {
                return fehler("Es gibt schon ähnlich benannte Stacks: ${aehnlich.joinToString(", ") { it.name }}. Ist einer davon gemeint? Sonst mit trotzdem_neu: true anlegen.", rueckfrage = true)
            }
            if (name.length > 60) return fehler("Der Stackname darf höchstens 60 Zeichen haben.")
            val zeitpunkt = a.optString("zeitpunkt").trim()
            val hinweis = a.optString("einnahme_hinweis").trim()
            if (zeitpunkt.length > 120 || hinweis.length > 120) return fehler("Zeitpunkt und Einnahmehinweis dürfen je höchstens 120 Zeichen haben.")
            val s = Stack(neueId(), name, zeitpunkt, hinweis, (b.stacks.maxOfOrNull { it.sortierung } ?: 0) + 1)
            repository.speichereStack(s)
            a.position()?.let { stackPosition(s.id, it) }
            val nb = bestand()
            val gelesen = nb.stacks.firstOrNull { it.id == s.id } ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
            return ok("Gespeichert und nachgelesen: neuer Stack angelegt.\n" + stackText(nb, gelesen, emptyList()))
        }

        val s = when (val r = findeStack(name, b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
        val aenderungen = mutableListOf<String>()
        val stammdaten = listOf("neuer_name", "zeitpunkt", "einnahme_hinweis").any { a.gesetzt(it) } || a.position() != null || (a.optJSONArray("mittel_reihenfolge")?.length() ?: 0) > 0
        if (stammdaten && s.gesperrt) return gesperrt(s)

        var neu = s
        if (a.gesetzt("neuer_name")) {
            val nn = a.optString("neuer_name").trim()
            if (nn.length > 60) return fehler("Der Stackname darf höchstens 60 Zeichen haben.")
            b.stacks.firstOrNull { it.id != s.id && Namensaufloesung.gleich(it.name, nn) }?.let { return fehler("Den Namen „${it.name}“ hat schon ein anderer Stack.", rueckfrage = true) }
            neu = neu.copy(name = nn)
        }
        a.textOderLeer("zeitpunkt")?.let { neu = neu.copy(zeitpunkt = it) }
        a.textOderLeer("einnahme_hinweis")?.let { neu = neu.copy(einnahmeHinweis = it) }
        if (neu.zeitpunkt.length > 120 || neu.einnahmeHinweis.length > 120) return fehler("Zeitpunkt und Einnahmehinweis dürfen je höchstens 120 Zeichen haben.")
        if (neu != s) { repository.speichereStack(neu); aenderungen += "Angaben" }
        a.position()?.takeIf { it != b.position(s) }?.let { stackPosition(s.id, it); aenderungen += "Position" }

        a.optJSONArray("mittel_reihenfolge")?.let { liste ->
            val eintraege = b.eintraegeVon(s.id)
            val kandidaten = eintraege.mapNotNull { e -> b.mittel[e.mittelId]?.let { Kandidat(e.id, it.name) } }
            val gewuenscht = mutableListOf<String>()
            for (i in 0 until liste.length()) {
                val wort = liste.optString(i)
                when (val r = Namensaufloesung.mittel(wort, kandidaten)) {
                    is Aufloesung.Eindeutig -> if (r.kandidat.id !in gewuenscht) gewuenscht += r.kandidat.id
                    is Aufloesung.Mehrdeutig -> return mehrdeutig("Mittel in ${s.name}", wort, r.kandidaten)
                    is Aufloesung.Keiner -> return fehler("„$wort“ ist nicht in ${s.name}. Enthalten: ${kandidaten.joinToString(", ") { it.name }}.", rueckfrage = true)
                }
            }
            // Was nicht genannt wurde, bleibt in bisheriger Reihenfolge hinten.
            val reihenfolge = gewuenscht + eintraege.map { it.id }.filter { it !in gewuenscht }
            if (reihenfolge != eintraege.map { it.id }) { repository.sortiereEintraege(s.id, reihenfolge); aenderungen += "Reihenfolge der Mittel" }
        }

        if (a.gesetzt("ziel_hinzufuegen")) {
            val text = a.optString("ziel_hinzufuegen").trim()
            val ziel = b.ziele.values.firstOrNull { it.text.equals(text, ignoreCase = true) } ?: Ziel(neueId(), text).also { repository.speichereZiel(it) }
            val ziele = b.zieleVon(s.id)
            if (ziele.none { it.zielId == ziel.id }) {
                repository.setzeStackZiel(StackZiel(s.id, ziel.id, ziele.size + 1))
                aenderungen += "Ziel „${ziel.text}“"
            }
            if (a.has("ziel_rang") && !a.isNull("ziel_rang")) {
                val ids = repository.beobachteStackZiele(s.id).first().sortedBy { it.rang }.map { it.zielId }.toMutableList()
                ids.remove(ziel.id)
                ids.add((a.optInt("ziel_rang") - 1).coerceIn(0, ids.size), ziel.id)
                repository.sortiereZiele(s.id, ids)
                aenderungen += "Zielrang"
            }
        }
        if (a.gesetzt("frage_hinzufuegen")) {
            val text = a.optString("frage_hinzufuegen").trim()
            if (text.length > 300) return fehler("Eine eigene Frage darf höchstens 300 Zeichen haben.")
            if (repository.beobachteFragen(s.id).first().none { it.text.equals(text, ignoreCase = true) }) {
                repository.speichereFrage(EigeneFrage(neueId(), s.id, text))
                aenderungen += "eigene Frage"
            }
        }

        val nb = bestand()
        val gelesen = nb.stacks.firstOrNull { it.id == s.id } ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
        val fragen = repository.beobachteFragen(s.id).first()
        return if (aenderungen.isEmpty()) ok("Nichts geändert, ${s.name} war schon so.\n" + stackText(nb, gelesen, fragen))
        else ok("Gespeichert und nachgelesen (${aenderungen.joinToString(", ")}):\n" + stackText(nb, gelesen, fragen))
    }

    private suspend fun stackPosition(stackId: String, position: Int) {
        val ids = repository.beobachteStacks().first().sortedWith(compareBy({ it.sortierung }, { it.id })).map { it.id }.toMutableList()
        ids.remove(stackId)
        ids.add((position - 1).coerceIn(0, ids.size), stackId)
        repository.sortiereStacks(ids)
    }

    // ------------------------------------------------------------------ Mittel-Katalog

    private suspend fun mittelSpeichern(a: JSONObject): JSONObject {
        val b = bestand()
        val name = a.optString("mittel").trim()
        if (name.isEmpty()) return fehler("Welches Mittel? Der Name fehlt.", rueckfrage = true)
        if (a.optBoolean("neu", false)) {
            return when (val r = neuesMittel(name, a, b)) {
                is Antwort -> r.json
                is Gefunden -> {
                    val m = repository.beobachteMittel().first().firstOrNull { it.id == r.wert.id } ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
                    ok((if (r.neu) "Gespeichert und nachgelesen: neues Mittel angelegt.\n" else "Das Mittel gibt es schon; nichts doppelt angelegt.\n") + mittelText(bestand(), m))
                }
            }
        }
        val m = when (val r = findeMittel(name, b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
        var neu = m
        if (a.gesetzt("neuer_name")) {
            val nn = a.optString("neuer_name").trim()
            b.mittel.values.firstOrNull { it.id != m.id && Namensaufloesung.gleich(it.name, nn) }?.let { return fehler("Den Namen „${it.name}“ hat schon ein anderes Mittel.", rueckfrage = true) }
            neu = neu.copy(name = nn, loeslichkeitKiErmittelt = false)
        }
        if (a.gesetzt("darreichungsform")) {
            val text = a.optString("darreichungsform").trim()
            val form = text.toDarreichungsformOrNull()
            neu = neu.copy(darreichungsform = form ?: Darreichungsform.SONSTIGE, darreichungsformText = form?.anzeige() ?: text)
        }
        if (a.gesetzt("loeslichkeit")) neu = neu.copy(loeslichkeit = loeslichkeit(a.optString("loeslichkeit")) ?: return fehler("Löslichkeit: wasser, fett oder beides."), loeslichkeitKiErmittelt = false)
        a.textOderLeer("hersteller")?.let { neu = neu.copy(hersteller = it) }
        a.textOderLeer("beistoffe")?.let { neu = neu.copy(beistoffe = it) }
        if (a.has("durchfallrisiko")) neu = neu.copy(durchfallrisiko = a.optBoolean("durchfallrisiko"))
        if (neu == m) return ok("Nichts geändert, ${m.name} war schon so.\n" + mittelText(b, m))
        repository.speichereMittel(neu)
        val gelesen = repository.beobachteMittel().first().firstOrNull { it.id == m.id } ?: return fehler("Nach dem Speichern nicht lesbar; bitte in der App prüfen.")
        return ok("Gespeichert und nachgelesen:\n" + mittelText(bestand(), gelesen))
    }

    /** Legt ein Mittel an, wenn es wirklich neu ist. Gleicher Name = vorhandenes Mittel; sehr ähnlich = Rückfrage. */
    private suspend fun neuesMittel(name: String, a: JSONObject, b: Bestand): Ergebnis<Mittel> {
        b.mittel.values.firstOrNull { Namensaufloesung.gleich(it.name, name) }?.let { return Gefunden(it, neu = false) }
        val aehnlich = Namensaufloesung.aehnlich(name, b.mittel.values.map { Kandidat(it.id, it.name) })
        if (aehnlich.isNotEmpty() && !a.optBoolean("trotzdem_neu", false)) {
            return Antwort(fehler("Im Katalog gibt es schon ${aehnlich.joinToString(", ") { "„${it.name}“" }}. Ist das gemeint? Sonst mit trotzdem_neu: true neu anlegen.", rueckfrage = true))
        }
        if (name.length > 80) return Antwort(fehler("Der Mittelname darf höchstens 80 Zeichen haben."))
        val formText = a.optString("darreichungsform").trim()
        val form = formText.toDarreichungsformOrNull() ?: if (formText.isEmpty()) null else Darreichungsform.SONSTIGE
        val loeslich = a.optString("loeslichkeit").trim().takeIf { it.isNotEmpty() }?.let { loeslichkeit(it) ?: return Antwort(fehler("Löslichkeit: wasser, fett oder beides.")) }
        val m = Mittel(
            id = neueId(),
            name = name,
            loeslichkeit = loeslich ?: Loeslichkeit.WASSER,
            // Ohne Angabe nichts erfinden: „Sonstige“, Frank kann es in der App oder über Jarvis nachtragen.
            darreichungsform = form ?: Darreichungsform.SONSTIGE,
            darreichungsformText = form?.let { if (it == Darreichungsform.SONSTIGE) formText.ifEmpty { null } else it.anzeige() },
            hersteller = a.optString("hersteller").trim(),
            beistoffe = a.optString("beistoffe").trim(),
            durchfallrisiko = a.optBoolean("durchfallrisiko", false),
        )
        repository.speichereMittel(m)
        if (loeslich == null) nachNeuemMittel(m)
        return Gefunden(m, neu = true)
    }

    private fun loeslichkeit(text: String): Loeslichkeit? = when (text.trim().lowercase(Locale.GERMAN)) {
        "wasser", "wasserlöslich", "wasserloeslich" -> Loeslichkeit.WASSER
        "fett", "fettlöslich", "fettloeslich" -> Loeslichkeit.FETT
        "beides", "beide", "wasser- und fettlöslich" -> Loeslichkeit.BEIDES
        else -> null
    }

    // ------------------------------------------------------------------ Löschen (nur bestätigt)

    private suspend fun loeschen(a: JSONObject): JSONObject {
        val b = bestand()
        val was = a.optString("was").trim()
        val bestaetigt = a.optBoolean("bestaetigt", false)
        return when (was) {
            "mittel_aus_stack" -> {
                val s = when (val r = findeStack(a.optString("stack"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
                if (s.gesperrt) return gesperrt(s)
                val kandidaten = b.eintraegeVon(s.id).mapNotNull { e -> b.mittel[e.mittelId]?.let { Kandidat(e.id, it.name) } }
                val e = when (val r = Namensaufloesung.mittel(a.optString("mittel"), kandidaten)) {
                    is Aufloesung.Eindeutig -> b.eintraege.first { it.id == r.kandidat.id }
                    is Aufloesung.Mehrdeutig -> return mehrdeutig("Mittel in ${s.name}", a.optString("mittel"), r.kandidaten)
                    is Aufloesung.Keiner -> return fehler("„${a.optString("mittel")}“ ist nicht in ${s.name}. Enthalten: ${kandidaten.joinToString(", ") { it.name }}.", rueckfrage = true)
                }
                val zeile = eintragZeile(b, e)
                if (!bestaetigt) return bestaetigen("${b.mittel[e.mittelId]?.name} aus ${s.name} entfernen? Dabei gehen Dosis und Notiz dieses Eintrags verloren: $zeile")
                repository.loescheEintrag(e.id)
                if (repository.beobachteAlleEintraege().first().any { it.id == e.id }) return fehler("Stack-Labor hat den Eintrag nicht entfernt.")
                ok("Entfernt und nachgelesen: ${b.mittel[e.mittelId]?.name} ist nicht mehr in ${s.name}. Zum Wiederherstellen: $zeile")
            }
            "stack" -> {
                val s = when (val r = findeStack(a.optString("stack"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
                if (s.gesperrt) return gesperrt(s)
                val text = stackText(b, s, repository.beobachteFragen(s.id).first())
                if (!bestaetigt) return bestaetigen("Den ganzen Stack „${s.name}“ mit ${b.eintraegeVon(s.id).size} Mitteln, Zielen, Fragen und Auswertungen löschen? Die Mittel bleiben im Katalog.\n$text")
                repository.loescheStack(s.id)
                if (repository.beobachteStacks().first().any { it.id == s.id }) return fehler("Stack-Labor hat den Stack nicht gelöscht.")
                ok("Gelöscht und nachgelesen: Stack „${s.name}“. Bisheriger Inhalt zum Wiederherstellen:\n$text")
            }
            "mittel_aus_katalog" -> {
                val m = when (val r = findeMittel(a.optString("mittel"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
                val wo = b.eintraege.filter { it.mittelId == m.id }
                b.stacks.firstOrNull { s -> s.gesperrt && wo.any { it.stackId == s.id } }?.let { return gesperrt(it) }
                val text = mittelText(b, m)
                if (!bestaetigt) return bestaetigen("„${m.name}“ endgültig aus dem Katalog löschen?" + (if (wo.isNotEmpty()) " Es fällt damit auch aus ${wo.size} Stack(s) heraus: ${wo.joinToString(", ") { stackName(b, it.stackId) }}." else "") + "\n$text")
                repository.loescheMittel(m.id, bestaetigt = true)
                if (repository.beobachteMittel().first().any { it.id == m.id }) return fehler("Stack-Labor hat das Mittel nicht gelöscht.")
                ok("Gelöscht und nachgelesen: „${m.name}“. Bisherige Angaben zum Wiederherstellen:\n$text")
            }
            "mittel_zusammenfuehren" -> {
                val weg = when (val r = findeMittel(a.optString("mittel"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
                val bleibt = when (val r = findeMittel(a.optString("behalten"), b, "Mittel, das bleibt")) { is Gefunden -> r.wert; is Antwort -> return r.json }
                if (weg.id == bleibt.id) return fehler("Das ist dasselbe Mittel.", rueckfrage = true)
                val betroffen = b.eintraege.filter { it.mittelId == weg.id || it.mittelId == bleibt.id }.map { it.stackId }.toSet()
                b.stacks.firstOrNull { it.gesperrt && it.id in betroffen }?.let { return gesperrt(it) }
                if (!bestaetigt) return bestaetigen("„${weg.name}“ in „${bleibt.name}“ zusammenführen? „${weg.name}“ verschwindet aus dem Katalog; wo beide im selben Stack stehen, bleibt nur der Eintrag von „${bleibt.name}“.\n" + mittelText(b, weg))
                repository.fuehreMittelZusammen(bleibt.id, weg.id)
                val nb = bestand()
                if (nb.mittel.containsKey(weg.id)) return fehler("Stack-Labor hat die Mittel nicht zusammengeführt.")
                ok("Zusammengeführt und nachgelesen:\n" + mittelText(nb, nb.mittel.getValue(bleibt.id)))
            }
            "ziel_aus_stack" -> {
                val s = when (val r = findeStack(a.optString("stack"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
                val ziele = b.zieleVon(s.id).mapNotNull { sz -> b.ziele[sz.zielId]?.let { Kandidat(it.id, it.text) } }
                val z = when (val r = Namensaufloesung.mittel(a.optString("ziel"), ziele)) {
                    is Aufloesung.Eindeutig -> r.kandidat
                    is Aufloesung.Mehrdeutig -> return mehrdeutig("Ziel in ${s.name}", a.optString("ziel"), r.kandidaten)
                    is Aufloesung.Keiner -> return fehler("Dieses Ziel hat ${s.name} nicht. Ziele: ${ziele.joinToString("; ") { it.name }}.", rueckfrage = true)
                }
                if (!bestaetigt) return bestaetigen("Ziel „${z.name}“ aus ${s.name} entfernen? (Es bleibt im Ziel-Katalog.)")
                repository.entferneStackZiel(s.id, z.id)
                ok("Entfernt und nachgelesen: Ziel „${z.name}“ gehört nicht mehr zu ${s.name}.")
            }
            "frage" -> {
                val s = when (val r = findeStack(a.optString("stack"), b)) { is Gefunden -> r.wert; is Antwort -> return r.json }
                val fragen = repository.beobachteFragen(s.id).first()
                val f = when (val r = Namensaufloesung.mittel(a.optString("frage"), fragen.map { Kandidat(it.id, it.text) })) {
                    is Aufloesung.Eindeutig -> r.kandidat
                    is Aufloesung.Mehrdeutig -> return mehrdeutig("Frage in ${s.name}", a.optString("frage"), r.kandidaten)
                    is Aufloesung.Keiner -> return fehler("Diese Frage hat ${s.name} nicht.", rueckfrage = true)
                }
                if (!bestaetigt) return bestaetigen("Eigene Frage „${f.name}“ aus ${s.name} löschen?")
                repository.loescheFrage(f.id)
                ok("Gelöscht und nachgelesen: Frage „${f.name}“.")
            }
            else -> fehler("Was soll gelöscht werden? Erlaubt: mittel_aus_stack, stack, mittel_aus_katalog, mittel_zusammenfuehren, ziel_aus_stack, frage.", rueckfrage = true)
        }
    }

    // ------------------------------------------------------------------ Texte

    /** Zeitpunkt und Einnahmehinweis: die Beschreibung des Stacks, verbindlich für Zeitpunkt und Reihenfolge. */
    private fun beschreibung(s: Stack): String? = listOfNotNull(
        s.zeitpunkt.trim().takeIf { it.isNotEmpty() }?.let { "Zeitpunkt: $it" },
        s.einnahmeHinweis.trim().takeIf { it.isNotEmpty() }?.let { "Einnahmehinweis: $it" },
    ).joinToString(" · ").ifEmpty { null }

    private fun stackText(b: Bestand, s: Stack, mitFragen: List<EigeneFrage>): String = buildString {
        val e = b.eintraegeVon(s.id)
        append("### ").append(b.position(s)).append(". ").append(s.name).append(" [id ").append(s.id).append("]\n")
        append(beschreibung(s) ?: "Keine Beschreibung (Zeitpunkt, Einnahmehinweis) hinterlegt.").append('\n')
        if (s.gesperrt) append("Gesperrt in Stack-Labor (Schloss): Änderungen erst, wenn Frank ihn in der App entsperrt.\n")
        val ziele = b.zieleVon(s.id).mapNotNull { sz -> b.ziele[sz.zielId]?.let { "${sz.rang}. ${it.text}" } }
        if (ziele.isNotEmpty()) append("Ziele: ").append(ziele.joinToString("; ")).append('\n')
        if (e.isEmpty()) append("Noch keine Mittel.\n")
        else {
            append("Mittel in Einnahme-Reihenfolge (").append(e.size).append(", davon ").append(e.count { it.aktiv }).append(" aktiv):\n")
            e.forEach { append(eintragZeile(b, it)).append('\n') }
        }
        if (mitFragen.isNotEmpty()) append("Eigene Fragen: ").append(mitFragen.joinToString("; ") { it.text }).append('\n')
    }.trim()

    /** Eine Zeile je Eintrag; `[id …]` lässt Jarvis' Gedächtnis Änderungen am selben Eintrag wiedererkennen. */
    private fun eintragZeile(b: Bestand, e: StackEintrag): String = buildString {
        val m = b.mittel[e.mittelId]
        val nr = b.eintraegeVon(e.stackId).indexOfFirst { it.id == e.id } + 1
        append("- [id ").append(e.id).append("] ")
        if (nr > 0) append(nr).append(". ")
        append(m?.name ?: "Unbekanntes Mittel").append(" — ")
        val frei = e.dosen.firstOrNull { it.variante == DosisVariante.FREI }
        val dienst = e.dosen.firstOrNull { it.variante == DosisVariante.DIENST }
        append(if (m != null) dosisText(m, frei) else "Dosis unbekannt")
        if (dienst != null && m != null) append(" (an freien Tagen); an Diensttagen ").append(dosisText(m, dienst))
        append(" · ").append(e.frequenzAnzeige())
        append(if (e.aktiv) " · aktiv" else " · INAKTIV (ausgeschaltet, nicht Teil der geplanten Einnahme)")
        if (e.zusatztext.isNotBlank()) append(" · Notiz: ").append(e.zusatztext.trim().replace(Regex("\\s+"), " "))
        if (e.gruppeId != null) {
            val mit = b.eintraegeVon(e.stackId).filter { it.gruppeId == e.gruppeId && it.id != e.id }.mapNotNull { b.mittel[it.mittelId]?.name }
            if (mit.isNotEmpty()) append(" · zusammen mit ").append(mit.joinToString(", "))
        }
        if (e.alternationsPartnerMittelIds.isNotEmpty()) append(" · wechselt sich ab mit ").append(e.alternationsPartnerMittelIds.joinToString(", ") { b.mittel[it]?.name ?: it })
        e.offenerHinweis?.takeIf { it.isNotBlank() }?.let { append(" · Hinweis der App: ").append(it.trim()) }
    }

    private fun dosisText(m: Mittel, d: StackEintragDosis?): String =
        if (d == null) "ohne Dosis" else dosisText(m, Dosis(d.stueckzahl, d.mengeJeStueck, d.einheit), d.einheitAnzeige())

    /**
     * „2 Kapseln à 1 g = 2 g“, „6 g“, „2 Kapseln“. Zählbare Formen (Kapsel, Tablette …) nennen die Stückzahl, sonst nur
     * die Menge. Die Kapselanzahl steht nur da, wenn sie hinterlegt ist.
     */
    private fun dosisText(m: Mittel, d: Dosis, einheit: String = DosisEingabe.anzeige(d.einheit)): String {
        val zaehlbar = m.darreichungsform in ZAEHLBAR || (m.darreichungsform == Darreichungsform.SONSTIGE && !m.darreichungsformText.isNullOrBlank())
        val stueck = DosisEingabe.schoen(d.stueckzahl)
        val form = m.darreichungsformAnzeige(d.stueckzahl)
        val je = d.mengeJeStueck ?: return if (zaehlbar) "$stueck $form" else "$stueck Stück"
        val gesamt = DosisEingabe.schoen(je.multiply(d.stueckzahl))
        val eins = d.stueckzahl.compareTo(BigDecimal.ONE) == 0
        return when {
            eins && zaehlbar -> "$gesamt $einheit (1 $form)"
            eins -> "$gesamt $einheit"
            zaehlbar -> "$stueck $form à ${DosisEingabe.schoen(je)} $einheit = $gesamt $einheit"
            else -> "$stueck × ${DosisEingabe.schoen(je)} $einheit = $gesamt $einheit"
        }
    }

    private fun mittelText(b: Bestand, m: Mittel): String = buildString {
        append(m.name).append(" [id ").append(m.id).append("]\n")
        append("Darreichung: ").append(m.darreichungsformAnzeige()).append(" · ").append(m.loeslichkeit.anzeige())
        if (m.loeslichkeitKiErmittelt) append(" (von der KI bestimmt)")
        append('\n')
        if (m.hersteller.isNotBlank()) append("Hersteller: ").append(m.hersteller).append('\n')
        if (m.beistoffe.isNotBlank()) append("Beistoffe: ").append(m.beistoffe).append('\n')
        if (m.durchfallrisiko) append("Durchfallrisiko: ja\n")
        if (m.wirkstoffkomponenten.isNotEmpty()) append("Wirkstoffe: ").append(m.wirkstoffkomponenten.joinToString(", ") { k ->
            listOfNotNull(k.name, k.menge?.let { "${DosisEingabe.schoen(it)} ${DosisEingabe.anzeige(k.einheit)}".trim() }).joinToString(" ")
        }).append('\n')
        val wo = b.eintraege.filter { it.mittelId == m.id }.sortedBy { e -> b.stacks.indexOfFirst { it.id == e.stackId } }
        if (wo.isEmpty()) append("In keinem Stack.")
        else {
            append("In ").append(wo.size).append(" Stack(s):\n")
            wo.forEach { e -> append(stackName(b, e.stackId)).append(": ").append(eintragZeile(b, e).substringAfter("] ")).append('\n') }
        }
    }.trim()

    private fun stackName(b: Bestand, id: String) = b.stacks.firstOrNull { it.id == id }?.name ?: id

    private fun datum(ms: Long): String =
        DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

    // ------------------------------------------------------------------ Suchen und Antworten

    private sealed interface Ergebnis<out T>
    private data class Gefunden<T>(val wert: T, val neu: Boolean = false) : Ergebnis<T>
    private data class Antwort(val json: JSONObject) : Ergebnis<Nothing>

    private fun findeStack(name: String, b: Bestand, rolle: String = "Stack"): Ergebnis<Stack> {
        if (name.isBlank()) return Antwort(fehler("Welcher $rolle? Der Name fehlt. Vorhanden: ${b.stacks.joinToString(", ") { it.name }}.", rueckfrage = true))
        return when (val r = Namensaufloesung.stack(name, b.stacks.map { Kandidat(it.id, it.name) })) {
            is Aufloesung.Eindeutig -> Gefunden(b.stacks.first { it.id == r.kandidat.id })
            is Aufloesung.Mehrdeutig -> Antwort(mehrdeutig(rolle, name, r.kandidaten))
            is Aufloesung.Keiner -> Antwort(fehler("Keinen $rolle gefunden, der zu „$name“ passt. Vorhanden: ${b.stacks.joinToString(", ") { it.name }}.", rueckfrage = true))
        }
    }

    private fun findeMittel(name: String, b: Bestand, rolle: String = "Mittel"): Ergebnis<Mittel> {
        if (name.isBlank()) return Antwort(fehler("Welches $rolle? Der Name fehlt.", rueckfrage = true))
        return when (val r = Namensaufloesung.mittel(name, b.mittel.values.map { Kandidat(it.id, it.name) })) {
            is Aufloesung.Eindeutig -> Gefunden(b.mittel.getValue(r.kandidat.id))
            is Aufloesung.Mehrdeutig -> Antwort(mehrdeutig(rolle, name, r.kandidaten))
            is Aufloesung.Keiner -> Antwort(fehler("Kein Mittel im Katalog passt zu „$name“." + (if (r.aehnlich.isNotEmpty()) " Ähnlich: ${r.aehnlich.joinToString(", ") { it.name }}." else ""), rueckfrage = true))
        }
    }

    private suspend fun frisch(stackId: String, mittelId: String): StackEintrag? =
        repository.beobachteAlleEintraege().first().firstOrNull { it.stackId == stackId && it.mittelId == mittelId }

    private fun nichtImStack(s: Stack, name: String, m: Mittel?): JSONObject =
        fehler("${m?.name ?: "„$name“"} ist nicht in ${s.name}. Zum Hinzufügen aktion „hinzufuegen“ mit Dosis verwenden.", rueckfrage = true)

    private fun gesperrt(s: Stack): JSONObject =
        fehler("„${s.name}“ ist in Stack-Labor gesperrt (Schloss). Nichts geändert. Frank muss den Stack zuerst in der App entsperren; Jarvis entsperrt nie selbst.")

    private fun mehrdeutig(rolle: String, anfrage: String, kandidaten: List<Kandidat>): JSONObject =
        fehler("Mehrere ${rolle}-Namen passen zu „$anfrage“: ${kandidaten.joinToString(", ") { "„${it.name}“" }}. Frage Frank, welcher gemeint ist, und rufe dann mit dem genauen Namen auf.", rueckfrage = true)
            .put("kandidaten", JSONArray(kandidaten.map { it.name }))

    private fun bestaetigen(frage: String): JSONObject =
        fehler("NICHT GELÖSCHT, Bestätigung nötig. $frage Erst wenn Frank ausdrücklich zustimmt, mit bestaetigt: true erneut aufrufen.", rueckfrage = true).put("bestaetigung_noetig", true)

    private fun ok(text: String) = JSONObject().put("text", text.trim())

    private fun fehler(text: String, rueckfrage: Boolean = false) = JSONObject().put("fehler", text).apply { if (rueckfrage) put("rueckfrage", true) }

    private fun JSONObject.str(name: String): String? = if (has(name) && !isNull(name)) optString(name).trim().takeIf { it.isNotEmpty() } else null

    private fun JSONObject.gesetzt(name: String): Boolean = str(name) != null

    /** Ein leeres Feld heißt „nicht angegeben“ (manche Modelle füllen alle Felder); „leer“ oder „-“ löscht den Text ausdrücklich. */
    private fun JSONObject.textOderLeer(name: String): String? = str(name)?.let { if (it.lowercase(Locale.GERMAN) in setOf("leer", "-", "löschen", "keine", "keiner")) "" else it }

    /** Position ab 1; fehlend, null oder kleiner 1 = nicht angegeben. */
    private fun JSONObject.position(): Int? = if (has("position") && !isNull("position")) optInt("position").takeIf { it >= 1 } else null

    private companion object {
        val ZAEHLBAR = setOf(Darreichungsform.KAPSEL, Darreichungsform.TABLETTE, Darreichungsform.LOEFFEL, Darreichungsform.TASSE, Darreichungsform.TROPFEN)
    }
}
