package de.frank.epigenetik.data

/** In welche Richtung ein Schalter für die Gesundheit günstig ist. */
enum class Richtung(val anzeige: String, val pfeil: String) {
    AKTIVIEREN("aktivieren", "▲"),
    HEMMEN("hemmen", "▼"),
    STABILISIEREN("im Gleichgewicht halten", "◆");

    companion object {
        fun von(s: String?): Richtung {
            val t = s.orEmpty().lowercase()
            return when {
                t.startsWith("aktiv") || t.startsWith("an") || t.startsWith("steig") || t.startsWith("hoch") || t.startsWith("erh") -> AKTIVIEREN
                t.startsWith("hemm") || t.startsWith("senk") || t.startsWith("aus") || t.startsWith("runter") || t.startsWith("unterdr") -> HEMMEN
                else -> STABILISIEREN
            }
        }
    }
}

enum class Gruppe(val anzeige: String, val emoji: String) {
    SIRTUINE_NAD("Sirtuine & NAD+", "⚡"),
    ENERGIE_SENSOREN("Energie-Sensoren", "🔋"),
    ENTGIFTUNG("Entgiftung & Zellschutz", "🛡️"),
    ENTZUENDUNG("Entzündung", "🔥"),
    DNA_METHYLIERUNG("DNA-Methylierung", "🧬"),
    HISTON_MIRNA("Histone & miRNA", "🧶"),
    AUTOPHAGIE("Zellerneuerung & Autophagie", "♻️"),
    TUMORSUPPRESSION("Tumorschutz", "🎗️"),
    TELOMERE_ALTERUNG("Telomere & Alterung", "⏳"),
    STOFFWECHSEL("Stoffwechsel & Insulin", "🍯"),
    STRESS_HORMONE("Stress, Hormone & Rhythmus", "🌙"),
    MITOCHONDRIEN("Mitochondrien", "🔆"),
}

/** Ein epigenetischer Schalter aus dem festen Vokabular. */
data class Schalter(val id: String, val name: String, val gruppe: Gruppe, val guenstig: Richtung, val info: String)

/**
 * Das feste Vokabular. Die KI muss ihre Wirkungen diesen IDs zuordnen, damit Rückwärtssuche, Atlas
 * und Stack-Abdeckung ohne neue Recherche aus den vorhandenen Ergebnissen funktionieren.
 */
object Schalterkatalog {
    val alle = listOf(
        Schalter("SIRT1", "Sirtuin 1", Gruppe.SIRTUINE_NAD, Richtung.AKTIVIEREN, "Langlebigkeits-Enzym, entfernt Acetylgruppen von Histonen und schaltet Stress- und Reparaturprogramme an."),
        Schalter("SIRT3", "Sirtuin 3", Gruppe.SIRTUINE_NAD, Richtung.AKTIVIEREN, "Sirtuin der Mitochondrien, steuert Energiegewinnung und oxidativen Schutz."),
        Schalter("SIRT6", "Sirtuin 6", Gruppe.SIRTUINE_NAD, Richtung.AKTIVIEREN, "Hält das Genom stabil, fördert DNA-Reparatur und dämpft Entzündungsgene."),
        Schalter("NAD", "NAD+-Spiegel", Gruppe.SIRTUINE_NAD, Richtung.AKTIVIEREN, "Treibstoff der Sirtuine und PARP-Reparaturenzyme; sinkt mit dem Alter."),
        Schalter("CD38", "CD38 (NAD+-Verbraucher)", Gruppe.SIRTUINE_NAD, Richtung.HEMMEN, "Enzym, das im Alter NAD+ abbaut; Hemmung hält NAD+ hoch."),
        Schalter("AMPK", "AMPK", Gruppe.ENERGIE_SENSOREN, Richtung.AKTIVIEREN, "Energiesensor der Zelle; aktiviert bei Energiemangel Fettverbrennung, Autophagie und Mitochondrienbildung."),
        Schalter("MTOR", "mTOR", Gruppe.ENERGIE_SENSOREN, Richtung.HEMMEN, "Wachstumsschalter; dauerhaft aktiv beschleunigt er das Altern, zeitweise braucht ihn der Muskelaufbau."),
        Schalter("IGF1", "IGF-1-Achse", Gruppe.ENERGIE_SENSOREN, Richtung.HEMMEN, "Wachstumssignal; dauerhaft hohe Werte hängen mit Krebs und kürzerer Lebensspanne zusammen."),
        Schalter("FOXO3", "FOXO3", Gruppe.ENERGIE_SENSOREN, Richtung.AKTIVIEREN, "Langlebigkeits-Gen (Hundertjährige), schaltet Stressabwehr und Zellreinigung an."),
        Schalter("NRF2", "NRF2", Gruppe.ENTGIFTUNG, Richtung.AKTIVIEREN, "Hauptschalter für über 200 Schutzgene gegen oxidativen Stress und Giftstoffe."),
        Schalter("HO1", "Hämoxygenase-1", Gruppe.ENTGIFTUNG, Richtung.AKTIVIEREN, "Schutz-Enzym gegen oxidativen Stress und Entzündung, von NRF2 gesteuert."),
        Schalter("GSH", "Glutathion-System", Gruppe.ENTGIFTUNG, Richtung.AKTIVIEREN, "Wichtigstes körpereigenes Antioxidans und Entgiftungssystem."),
        Schalter("SOD2", "SOD2", Gruppe.ENTGIFTUNG, Richtung.AKTIVIEREN, "Entschärft Superoxid-Radikale in den Mitochondrien."),
        Schalter("PHASE2", "Phase-II-Enzyme", Gruppe.ENTGIFTUNG, Richtung.AKTIVIEREN, "GST, NQO1, UGT: machen Schadstoffe und Krebsauslöser ausscheidbar."),
        Schalter("NFKB", "NF-κB", Gruppe.ENTZUENDUNG, Richtung.HEMMEN, "Hauptschalter der Entzündung; chronisch aktiv treibt er Altern und Krebs."),
        Schalter("NLRP3", "NLRP3-Inflammasom", Gruppe.ENTZUENDUNG, Richtung.HEMMEN, "Entzündungsmaschine, die im Alter überaktiv wird (Inflammaging)."),
        Schalter("IL6", "IL-6/TNF-α", Gruppe.ENTZUENDUNG, Richtung.HEMMEN, "Entzündungsbotenstoffe, Marker stiller Entzündung."),
        Schalter("COX2", "COX-2", Gruppe.ENTZUENDUNG, Richtung.HEMMEN, "Entzündungsenzym, bildet Prostaglandine; in Tumoren oft hochreguliert."),
        Schalter("DNMT", "DNA-Methyltransferasen", Gruppe.DNA_METHYLIERUNG, Richtung.STABILISIEREN, "Setzen Methyl-Markierungen auf die DNA; Fehlsteuerung schaltet Schutzgene stumm."),
        Schalter("TET", "TET-Enzyme", Gruppe.DNA_METHYLIERUNG, Richtung.AKTIVIEREN, "Entfernen Methyl-Markierungen wieder und reaktivieren stummgeschaltete Gene."),
        Schalter("METHYL", "Methylierungszyklus", Gruppe.DNA_METHYLIERUNG, Richtung.STABILISIEREN, "Liefert Methylgruppen (SAM) über Folat, B12, B6, Cholin, Betain."),
        Schalter("EPIUHR", "Epigenetische Uhr", Gruppe.DNA_METHYLIERUNG, Richtung.HEMMEN, "Biologisches Alter, gemessen am DNA-Methylierungsmuster; Ziel: langsamer ticken lassen."),
        Schalter("HDAC", "Histon-Deacetylasen", Gruppe.HISTON_MIRNA, Richtung.HEMMEN, "Verdichten das Chromatin; Hemmung öffnet Schutz- und Tumorsuppressorgene."),
        Schalter("P300", "p300/CBP", Gruppe.HISTON_MIRNA, Richtung.HEMMEN, "Histon-Acetyltransferasen; überaktiv fördern sie Entzündung und Wachstum."),
        Schalter("EZH2", "EZH2", Gruppe.HISTON_MIRNA, Richtung.HEMMEN, "Setzt die Sperr-Markierung H3K27me3; in vielen Tumoren überaktiv."),
        Schalter("MIR21", "miR-21", Gruppe.HISTON_MIRNA, Richtung.HEMMEN, "Krebsfördernde microRNA, bremst Tumorsuppressoren wie PTEN."),
        Schalter("MIR34A", "miR-34a", Gruppe.HISTON_MIRNA, Richtung.AKTIVIEREN, "Tumorschützende microRNA, Teil des p53-Netzwerks."),
        Schalter("AUTOPHAGIE", "Autophagie", Gruppe.AUTOPHAGIE, Richtung.AKTIVIEREN, "Zelluläre Müllabfuhr: baut beschädigte Bestandteile ab und recycelt sie."),
        Schalter("MITOPHAGIE", "Mitophagie", Gruppe.AUTOPHAGIE, Richtung.AKTIVIEREN, "Abbau defekter Mitochondrien (PINK1/Parkin)."),
        Schalter("SENOLYSE", "Abbau seneszenter Zellen", Gruppe.AUTOPHAGIE, Richtung.AKTIVIEREN, "Entfernt gealterte „Zombie-Zellen“, die Entzündung verbreiten."),
        Schalter("P53", "p53", Gruppe.TUMORSUPPRESSION, Richtung.AKTIVIEREN, "Wächter des Genoms: stoppt Zellteilung bei DNA-Schäden oder lässt kranke Zellen sterben."),
        Schalter("PTEN", "PTEN", Gruppe.TUMORSUPPRESSION, Richtung.AKTIVIEREN, "Tumorsuppressor, bremst den Wachstumsweg PI3K/Akt."),
        Schalter("WNT", "Wnt/β-Catenin", Gruppe.TUMORSUPPRESSION, Richtung.HEMMEN, "Wachstumsweg; überaktiv bei Darm- und anderen Krebsarten."),
        Schalter("VEGF", "VEGF/Angiogenese", Gruppe.TUMORSUPPRESSION, Richtung.HEMMEN, "Gefäßneubildung, mit der Tumoren sich versorgen."),
        Schalter("TERT", "Telomerase", Gruppe.TELOMERE_ALTERUNG, Richtung.STABILISIEREN, "Verlängert Telomere; gesund in Stammzellen, unerwünscht in Tumorzellen."),
        Schalter("SASP", "SASP", Gruppe.TELOMERE_ALTERUNG, Richtung.HEMMEN, "Entzündliche Botenstoffe gealterter Zellen, die Nachbarzellen altern lassen."),
        Schalter("KLOTHO", "Klotho", Gruppe.TELOMERE_ALTERUNG, Richtung.AKTIVIEREN, "Anti-Aging-Hormon; höhere Spiegel gehen mit längerem Leben und besserer Kognition einher."),
        Schalter("INSULIN", "Insulinsensitivität", Gruppe.STOFFWECHSEL, Richtung.AKTIVIEREN, "Wie gut Zellen auf Insulin reagieren; Schlüssel gegen Diabetes und Altern."),
        Schalter("PPARA", "PPAR-α", Gruppe.STOFFWECHSEL, Richtung.AKTIVIEREN, "Schaltet Fettverbrennung und Ketonbildung an."),
        Schalter("FGF21", "FGF21", Gruppe.STOFFWECHSEL, Richtung.AKTIVIEREN, "Stoffwechselhormon bei Fasten und Proteinrestriktion, verlängert im Tier die Lebensspanne."),
        Schalter("HPA", "Cortisol-Achse (HPA)", Gruppe.STRESS_HORMONE, Richtung.HEMMEN, "Stressachse; chronisch aktiv beschleunigt sie die epigenetische Uhr."),
        Schalter("BDNF", "BDNF", Gruppe.STRESS_HORMONE, Richtung.AKTIVIEREN, "Wachstumsfaktor für Nervenzellen, Lernen und Stimmung."),
        Schalter("HSP", "Hitzeschockproteine", Gruppe.STRESS_HORMONE, Richtung.AKTIVIEREN, "Reparieren falsch gefaltete Proteine; durch Hitze (Sauna) und Bewegung aktiviert."),
        Schalter("CIRCADIAN", "Innere Uhr (CLOCK/BMAL1)", Gruppe.STRESS_HORMONE, Richtung.STABILISIEREN, "Taktgeber für tausende Gene; Schichtarbeit und Licht in der Nacht verstellen ihn."),
        Schalter("PGC1A", "PGC-1α", Gruppe.MITOCHONDRIEN, Richtung.AKTIVIEREN, "Hauptschalter für neue Mitochondrien und Ausdauer."),
        Schalter("TFAM", "NRF1/TFAM", Gruppe.MITOCHONDRIEN, Richtung.AKTIVIEREN, "Bauen und kopieren die Mitochondrien-DNA."),
        Schalter("MTROS", "Mitochondriale Radikale", Gruppe.MITOCHONDRIEN, Richtung.HEMMEN, "Überschüssige Radikale aus den Kraftwerken schädigen DNA und Proteine."),
    )

    private val nachId = alle.associateBy { it.id }

    fun zu(id: String?): Schalter? {
        val k = id.orEmpty().uppercase().replace(Regex("[^A-Z0-9]"), "")
        return nachId[k] ?: alle.firstOrNull { it.name.uppercase().replace(Regex("[^A-Z0-9]"), "") == k }
    }

    /** Kompakt für die KI: „SIRT1 = Sirtuin 1 (günstig: aktivieren)“. */
    fun fuerKi(): String = alle.joinToString("\n") { "${it.id} = ${it.name} [${it.gruppe.anzeige}] günstig: ${it.guenstig.anzeige}" }

    /** Ob die genannte Wirkungsrichtung für diesen Schalter günstig ist. */
    fun guenstig(s: Schalter, richtung: String): Boolean? {
        val r = Richtung.von(richtung)
        return when {
            s.guenstig == Richtung.STABILISIEREN || r == Richtung.STABILISIEREN -> null
            else -> s.guenstig == r
        }
    }
}

/** Vorgegebene Ziele mit ihren wichtigsten Schaltern (eigene Ziele ordnet die KI zu). */
data class ZielVorlage(val id: String, val name: String, val emoji: String, val schalter: List<String>)

object Zielkatalog {
    val alle = listOf(
        ZielVorlage("LANGLEBIGKEIT", "Langlebigkeit", "⏳", listOf("SIRT1", "NAD", "AMPK", "MTOR", "FOXO3", "AUTOPHAGIE", "EPIUHR", "SASP")),
        ZielVorlage("KREBSPRAEVENTION", "Krebsprävention", "🎗️", listOf("P53", "PTEN", "DNMT", "HDAC", "NFKB", "WNT", "MIR21", "PHASE2")),
        ZielVorlage("HERZ", "Herz & Gefäße", "❤️", listOf("NFKB", "NRF2", "SIRT1", "AMPK", "INSULIN", "IL6", "KLOTHO")),
        ZielVorlage("GEHIRN", "Gehirn & Kognition", "🧠", listOf("BDNF", "SIRT1", "NRF2", "AUTOPHAGIE", "HPA", "NLRP3", "MITOPHAGIE")),
        ZielVorlage("STOFFWECHSEL", "Blutzucker & Stoffwechsel", "🍯", listOf("INSULIN", "AMPK", "PPARA", "FGF21", "PGC1A", "SIRT3")),
        ZielVorlage("ENTZUENDUNG", "Stille Entzündung senken", "🔥", listOf("NFKB", "NLRP3", "IL6", "COX2", "SASP", "NRF2")),
        ZielVorlage("ENTGIFTUNG", "Entgiftung & Zellschutz", "🛡️", listOf("NRF2", "GSH", "PHASE2", "HO1", "SOD2")),
        ZielVorlage("FITNESS", "Fitness, HRV & VO2max", "🏃", listOf("PGC1A", "TFAM", "AMPK", "SIRT3", "HSP", "MTOR")),
        ZielVorlage("STRESS_SCHLAF", "Stress & Tiefschlaf", "🌙", listOf("HPA", "CIRCADIAN", "BDNF", "NFKB")),
        ZielVorlage("IMMUN", "Immunsystem & Seneszenz", "🦠", listOf("SENOLYSE", "SASP", "NAD", "CD38", "TET")),
        ZielVorlage("HAUT", "Haut & sichtbares Altern", "✨", listOf("NRF2", "SIRT1", "TERT", "SASP", "GSH")),
    )
}
