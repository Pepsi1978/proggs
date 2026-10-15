package de.frank.epigenetik.data

import org.json.JSONObject

/**
 * Macht aus einem Recherche-Ergebnis einen gut lesbaren (und vorlesbaren) Text.
 * Wird für das Vorlesen und für Jarvis benutzt, damit beide dieselbe vollständige Auswertung sehen.
 */
object Bericht {

    fun text(r: Recherche, wirkungen: List<SchalterWirkung> = emptyList(), kurzfassung: Boolean = false): String {
        if (!r.fertig || r.json.isBlank()) return when (r.statusEnum) {
            Status.FEHLER -> "${r.name}: Die Recherche ist gescheitert (${r.fehler})."
            else -> "${r.name}: Die Recherche läuft noch."
        }
        val o = runCatching { JSONObject(r.json) }.getOrNull() ?: return r.kurz
        return when (r.typEnum) {
            Typ.MITTEL -> mittel(r, o, wirkungen, kurzfassung)
            Typ.WIRKUNG, Typ.ZIEL -> wirkungOderZiel(r, o, kurzfassung)
            Typ.STACK -> stack(o, kurzfassung)
            Typ.FRAGE -> frage(o, kurzfassung)
        }
    }

    private fun mittel(r: Recherche, o: JSONObject, w: List<SchalterWirkung>, kurz: Boolean): String = buildString {
        append(r.name).append(" (").append(r.kategorieEnum.anzeige)
        o.optString("klasse").takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
        append("). Epigenetisches Potenzial ").append(r.potenzial).append(" von 100, Evidenz ").append(r.evidenz.ifBlank { "unklar" }).append(".\n")
        append(o.optString("kurz")).append('\n')
        val schalter = o.optJSONArray("schalter").objekte()
        if (schalter.isNotEmpty()) {
            append("\nEpigenetische Schalter:\n")
            schalter.take(if (kurz) 5 else 20).forEach { s ->
                val sch = Schalterkatalog.zu(s.optString("id"))
                append("• ").append(sch?.name ?: s.optString("id")).append(" ").append(s.optString("richtung"))
                append(" (Stärke ").append(s.optInt("staerke")).append(", ").append(s.optString("evidenz")).append(")")
                if (!kurz) {
                    s.optString("mechanismus").takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
                    s.optString("dosis_bezug").takeIf { it.isNotBlank() }?.let { append(" Dosis: ").append(it) }
                }
                append('\n')
            }
        } else if (w.isNotEmpty()) {
            append("\nSchalter: ").append(w.joinToString { "${Schalterkatalog.zu(it.schalter)?.name ?: it.schalter} ${it.richtung}" }).append('\n')
        }
        if (!kurz) {
            o.optJSONArray("sonstige_schalter").objekte().takeIf { it.isNotEmpty() }?.let { l ->
                append("Weitere Gene und Wege: ").append(l.joinToString { "${it.optString("name")} ${it.optString("richtung")}" }).append('\n')
            }
            o.optJSONArray("mechanismen").strings().takeIf { it.isNotEmpty() }?.let { append("\nMechanismen:\n").append(it.joinToString("\n") { "• $it" }).append('\n') }
        }
        o.optJSONObject("dosierung")?.let { d ->
            append("\nDosierung: ").append(listOfNotNull(
                d.optString("empfohlen").takeIf { it.isNotBlank() }?.let { "empfohlen $it" },
                d.optString("studien").takeIf { it.isNotBlank() && !kurz }?.let { "in Studien $it" },
                d.optString("obergrenze").takeIf { it.isNotBlank() }?.let { "Obergrenze $it" },
            ).joinToString("; ")).append('\n')
        }
        o.optJSONObject("einnahme")?.let { ei ->
            val teile = listOfNotNull(
                ei.optString("timing").takeIf { it.isNotBlank() },
                if (ei.optBoolean("mit_fett")) "mit Fett" else null,
                if (ei.optBoolean("nuechtern")) "nüchtern" else null,
                ei.optString("zyklisch").takeIf { it.isNotBlank() }?.let { "Zyklus: $it" },
                ei.optString("hinweise").takeIf { it.isNotBlank() && !kurz },
            )
            if (teile.isNotEmpty()) append("Einnahme: ").append(teile.joinToString("; ")).append('\n')
        }
        if (!kurz) {
            o.optJSONObject("bioverfuegbarkeit")?.let { b ->
                append("Bioverfügbarkeit: ").append(b.optString("bewertung"))
                b.optString("beste_form").takeIf { it.isNotBlank() }?.let { append(", beste Form: ").append(it) }
                b.optJSONArray("verstaerker").strings().takeIf { it.isNotEmpty() }?.let { append(", Verstärker: ").append(it.joinToString()) }
                append('\n')
            }
            o.optJSONArray("synergien").objekte().takeIf { it.isNotEmpty() }?.let { l ->
                append("Synergien: ").append(l.joinToString("; ") { "${it.optString("mittel")} (${it.optString("begruendung")})" }).append('\n')
            }
        }
        o.optJSONArray("risiken").strings().takeIf { it.isNotEmpty() }?.let { append("Risiken: ").append(it.joinToString("; ")).append('\n') }
        o.optJSONArray("wechselwirkungen").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            append("Wechselwirkungen: ").append(l.joinToString("; ") { "${it.optString("mit")}: ${it.optString("art")} (${it.optString("schwere")})" }).append('\n')
        }
        if (!kurz) {
            o.optJSONArray("kontroversen").strings().takeIf { it.isNotEmpty() }?.let { append("Widersprüche: ").append(it.joinToString("; ")).append('\n') }
            o.optJSONArray("offene_fragen").strings().takeIf { it.isNotEmpty() }?.let { append("Offene Fragen: ").append(it.joinToString("; ")).append('\n') }
        }
        append("\nFazit: ").append(o.optString("fazit"))
        if (!kurz) quellen(o)?.let { append("\n\nQuellen:\n").append(it) }
    }

    private fun wirkungOderZiel(r: Recherche, o: JSONObject, kurz: Boolean): String = buildString {
        append(o.optString("titel").ifBlank { r.name }).append('\n')
        o.optString("erklaerung").ifBlank { o.optString("beschreibung") }.takeIf { it.isNotBlank() }?.let { append(it).append('\n') }
        o.optJSONArray("schalter").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            append("\nWichtige Schalter: ").append(l.joinToString { "${Schalterkatalog.zu(it.optString("id"))?.name ?: it.optString("id")} ${it.optString("richtung")}" }).append('\n')
        }
        o.optJSONArray("schalter_ids").strings().takeIf { it.isNotEmpty() }?.let { l ->
            append("\nSchalter: ").append(l.joinToString { Schalterkatalog.zu(it)?.name ?: it }).append('\n')
        }
        liste("Nahrungsergänzungsmittel", o, "supplements", if (kurz) 5 else 12)
        liste("Nahrungsmittel", o, "nahrung", if (kurz) 4 else 10)
        liste("Verhalten", o, "verhalten", if (kurz) 3 else 8)
        o.optJSONArray("stack_vorschlag").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            append("\nStack-Vorschlag:\n").append(l.joinToString("\n") { "• ${it.optString("name")}: ${it.optString("dosis")}, ${it.optString("timing")}" }).append('\n')
        }
        o.optString("kombination").takeIf { it.isNotBlank() }?.let { append("\nBeste Kombination: ").append(it).append('\n') }
        if (!kurz) {
            o.optJSONArray("zu_meiden").objekte().takeIf { it.isNotEmpty() }?.let { l ->
                append("\nZu meiden: ").append(l.joinToString("; ") { "${it.optString("name")} (${it.optString("warum")})" }).append('\n')
            }
            o.optJSONArray("messbar").objekte().takeIf { it.isNotEmpty() }?.let { l ->
                append("Messbar über: ").append(l.joinToString("; ") { it.optString("marker") }).append('\n')
            }
        }
        append("\nFazit: ").append(o.optString("fazit"))
        if (!kurz) quellen(o)?.let { append("\n\nQuellen:\n").append(it) }
    }

    private fun StringBuilder.liste(titel: String, o: JSONObject, feld: String, max: Int) {
        val l = o.optJSONArray(feld).objekte()
        if (l.isEmpty()) return
        append('\n').append(titel).append(":\n")
        l.take(max).forEachIndexed { i, it ->
            append(i + 1).append(". ").append(it.optString("name")).append(" (Stärke ").append(it.optInt("staerke")).append(", ").append(it.optString("evidenz")).append(")")
            it.optString("mechanismus").takeIf { m -> m.isNotBlank() }?.let { m -> append(": ").append(m) }
            it.optString("dosis_hinweis").takeIf { d -> d.isNotBlank() }?.let { d -> append(" Dosis: ").append(d) }
            append('\n')
        }
    }

    private fun stack(o: JSONObject, kurz: Boolean): String = buildString {
        append("Stack-Bewertung ").append(o.optInt("bewertung")).append(" von 100. ").append(o.optString("zusammenfassung")).append('\n')
        o.optJSONArray("synergien").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            append("\nSynergien:\n").append(l.joinToString("\n") { "• ${it.optJSONArray("mittel").strings().joinToString(" + ")}: ${it.optString("warum")}" }).append('\n')
        }
        o.optJSONArray("luecken").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            append("\nLücken:\n").append(l.joinToString("\n") { "• ${Schalterkatalog.zu(it.optString("schalter"))?.name ?: it.optString("schalter")}: ${it.optString("warum")} Vorschläge: ${it.optJSONArray("vorschlaege").strings().joinToString()}" }).append('\n')
        }
        o.optJSONArray("doppelungen").objekte().takeIf { it.isNotEmpty() && !kurz }?.let { l ->
            append("\nDoppelungen:\n").append(l.joinToString("\n") { "• ${it.optJSONArray("mittel").strings().joinToString(" + ")}: ${it.optString("hinweis")}" }).append('\n')
        }
        o.optJSONArray("risiken").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            append("\nRisiken:\n").append(l.joinToString("\n") { "• ${it.optJSONArray("mittel").strings().joinToString(" + ")}: ${it.optString("hinweis")} (${it.optString("schwere")})" }).append('\n')
        }
        o.optJSONArray("tagesplan").objekte().takeIf { it.isNotEmpty() && !kurz }?.let { l ->
            append("\nTagesplan:\n").append(l.joinToString("\n") { "• ${it.optString("zeit")}: ${it.optJSONArray("mittel").strings().joinToString()}" + it.optString("hinweis").let { h -> if (h.isBlank()) "" else " – $h" } }).append('\n')
        }
        append("\nFazit: ").append(o.optString("fazit"))
    }

    private fun frage(o: JSONObject, kurz: Boolean): String = buildString {
        append(o.optString("titel")).append('\n')
        append(if (kurz) o.optString("kurz") else o.optString("antwort"))
        o.optJSONArray("empfehlungen").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            append("\n\nEmpfehlungen:\n").append(l.joinToString("\n") { "• ${it.optString("name")}: ${it.optString("warum")}" })
        }
        if (!kurz) quellen(o)?.let { append("\n\nQuellen:\n").append(it) }
    }

    private fun quellen(o: JSONObject): String? = o.optJSONArray("quellen").objekte().takeIf { it.isNotEmpty() }?.joinToString("\n") {
        "• ${it.optString("titel")}" + (it.optInt("jahr", 0).takeIf { j -> j > 0 }?.let { j -> " ($j)" } ?: "") +
            (it.optString("url").takeIf { u -> u.isNotBlank() }?.let { u -> " $u" } ?: "")
    }

    /** Für das Vorlesen: ohne Quellen-Links, Aufzählungszeichen weg. */
    fun zumVorlesen(r: Recherche): String = text(r).substringBefore("\n\nQuellen:").replace("• ", "").replace("▲", "").replace("▼", "")
}
