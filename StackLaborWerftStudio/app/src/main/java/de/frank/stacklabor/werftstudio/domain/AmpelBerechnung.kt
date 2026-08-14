package de.frank.stacklabor.werftstudio.domain

import de.frank.stacklabor.werftstudio.domain.model.Ampel
import de.frank.stacklabor.werftstudio.domain.model.Bewertungszelle
import de.frank.stacklabor.werftstudio.domain.model.StackZiel
import de.frank.stacklabor.werftstudio.domain.model.Wirkung

object AmpelBerechnung {
    fun zielgewicht(rang: Int): Int {
        require(rang >= 1) { "Der Zielrang muss mindestens 1 sein." }
        return when (rang) {
            in 1..3 -> 3
            in 4..7 -> 2
            else -> 1
        }
    }

    fun mittelAmpel(
        mittelId: String,
        mittelAktiv: Boolean,
        stackZiele: List<StackZiel>,
        zellen: List<Bewertungszelle>,
    ): Ampel {
        if (!mittelAktiv) return Ampel.GRAU
        val rangNachZiel = stackZiele.associate { it.zielId to it.rang }
        val maximaleStoerung = zellen.asSequence()
            .filter { it.mittelId == mittelId && it.wirkung == Wirkung.STOERT }
            .mapNotNull { zelle -> rangNachZiel[zelle.zielId]?.let { zielgewicht(it) * zelle.staerke } }
            .maxOrNull()
            ?: 0

        return when {
            maximaleStoerung >= 6 -> Ampel.ROT
            maximaleStoerung >= 2 -> Ampel.GELB
            else -> Ampel.GRUEN
        }
    }

    fun zielAmpel(
        zielId: String,
        aktiveMittelIds: Set<String>,
        zellen: List<Bewertungszelle>,
    ): Ampel {
        val relevanteZellen = zellen.filter {
            it.zielId == zielId && it.mittelId in aktiveMittelIds
        }
        val stuetzende = relevanteZellen.filter { it.wirkung == Wirkung.STUETZT }
        if (stuetzende.isEmpty()) return Ampel.GRAU

        val stoerende = relevanteZellen.filter { it.wirkung == Wirkung.STOERT }
        val saldo = stuetzende.sumOf { it.staerke } - stoerende.sumOf { it.staerke }
        return when {
            stoerende.any { it.staerke == 3 } || saldo <= -1 -> Ampel.ROT
            saldo in 0..2 -> Ampel.GELB
            saldo >= 3 && stoerende.none { it.staerke >= 2 } -> Ampel.GRUEN
            else -> Ampel.GELB
        }
    }

    fun sammelAmpel(zielAmpeln: Collection<Ampel>, hatBewertung: Boolean): Ampel {
        if (!hatBewertung || zielAmpeln.isEmpty()) return Ampel.GRAU
        return when {
            Ampel.ROT in zielAmpeln -> Ampel.ROT
            Ampel.GELB in zielAmpeln || Ampel.GRAU in zielAmpeln -> Ampel.GELB
            else -> Ampel.GRUEN
        }
    }
}
