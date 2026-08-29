package de.frank.genialeideen.ui

import de.frank.genialeideen.tts.ClonedVoice
import de.frank.genialeideen.tts.TtsCatalog
import de.frank.genialeideen.tts.TtsProvider
import de.frank.genialeideen.tts.VoiceGender

/**
 * Ein Eintrag der **einen** Stimmenauswahl aus Kapitel 4.6.
 *
 * Ich wähle die Stimme, nicht die Engine — mit der Stimme wird die zugehörige Engine
 * automatisch mitgeschaltet.
 */
data class StimmenEintrag(
    val id: String,
    val name: String,
    val anbieter: TtsProvider,
    val gruppe: String,
    /** Woher die Stimme kommt, als kurzes Kennzeichen: „Meine", „Alibaba", „Google", „Edge". */
    val herkunft: String,
    val geschlecht: VoiceGender? = null,
    val zusatz: String = "",
    /** Nicht nutzbar heisst ausgegraut mit Klartext-Grund — nie stilles Weglassen. */
    val nutzbar: Boolean = true,
    val grund: String = "",
)

/** Die feste Gruppenreihenfolge aus Kapitel 4.6. */
object Stimmenliste {
    const val GRUPPE_FAVORITEN = "Favoriten"
    const val GRUPPE_MEINE = "Meine Stimmen"
    const val GRUPPE_ALIBABA = "Alibaba-Stimmen"
    const val GRUPPE_GOOGLE = "Google Chirp 3 HD"
    const val GRUPPE_EDGE = "Edge-Stimmen"

    /** Der Platzhalter, wenn es noch keine eigene Stimme gibt — die Gruppe verschwindet nie. */
    const val ID_STIMME_AUFNEHMEN = "__aufnehmen__"

    /**
     * Baut die vollständige Liste in fester Reihenfolge:
     * Favoriten → Meine Stimmen → Alibaba → Google → Edge.
     *
     * @param eigeneFehler Grund, warum die eigenen Stimmen gerade nicht geladen werden konnten.
     */
    fun baue(
        eigene: List<ClonedVoice>,
        eigeneNamen: Map<String, String>,
        favoriten: Set<String>,
        alibabaSchluessel: Boolean,
        googleSchluessel: Boolean,
        eigeneFehler: String? = null,
    ): List<StimmenEintrag> {
        val meine = eigene.map { stimme ->
            StimmenEintrag(
                id = stimme.id,
                name = eigeneNamen[stimme.id] ?: stimme.name,
                anbieter = TtsProvider.QWEN_CLONE,
                gruppe = GRUPPE_MEINE,
                herkunft = TtsProvider.QWEN_CLONE.kurz,
                zusatz = stimme.createdAt,
                nutzbar = alibabaSchluessel,
                grund = if (alibabaSchluessel) "" else "Alibaba-Schlüssel fehlt",
            )
        }.ifEmpty {
            listOf(
                StimmenEintrag(
                    id = ID_STIMME_AUFNEHMEN,
                    name = eigeneFehler?.let { "Konnte nicht geladen werden — erneut versuchen" }
                        ?: "Eigene Stimme aufnehmen …",
                    anbieter = TtsProvider.QWEN_CLONE,
                    gruppe = GRUPPE_MEINE,
                    herkunft = TtsProvider.QWEN_CLONE.kurz,
                    zusatz = "",
                ),
            )
        }

        val alibaba = TtsCatalog.qwenVoices.map { stimme ->
            StimmenEintrag(
                id = stimme.id,
                name = stimme.name,
                anbieter = TtsProvider.QWEN,
                gruppe = GRUPPE_ALIBABA,
                herkunft = TtsProvider.QWEN.kurz,
                geschlecht = stimme.gender,
                nutzbar = alibabaSchluessel,
                grund = if (alibabaSchluessel) "" else "Alibaba-Schlüssel fehlt",
            )
        }

        val google = TtsCatalog.googleVoices.map { stimme ->
            StimmenEintrag(
                id = stimme.id,
                name = stimme.name,
                anbieter = TtsProvider.GOOGLE_CLOUD,
                gruppe = GRUPPE_GOOGLE,
                herkunft = TtsProvider.GOOGLE_CLOUD.kurz,
                geschlecht = stimme.gender,
                nutzbar = googleSchluessel,
                grund = if (googleSchluessel) "" else "Google-Schlüssel fehlt",
            )
        }

        // Edge braucht keinen Schlüssel und ist deshalb immer wählbar (Kapitel 4.6).
        val edge = TtsCatalog.edgeVoices.map { stimme ->
            StimmenEintrag(
                id = stimme.id,
                name = stimme.name,
                anbieter = TtsProvider.EDGE,
                gruppe = GRUPPE_EDGE,
                herkunft = TtsProvider.EDGE.kurz,
                geschlecht = stimme.gender,
            )
        }

        val alle = meine + sortiereNachGeschlecht(alibaba) +
            sortiereNachGeschlecht(google) + sortiereNachGeschlecht(edge)

        // Markierte Stimmen erscheinen zusätzlich ganz oben, noch vor „Meine Stimmen".
        val markierte = alle
            .filter { it.id in favoriten }
            .map { it.copy(gruppe = GRUPPE_FAVORITEN) }

        return markierte + alle
    }

    private fun sortiereNachGeschlecht(liste: List<StimmenEintrag>): List<StimmenEintrag> =
        liste.sortedWith(compareBy({ it.geschlecht != VoiceGender.FEMALE }, { it.name }))
}
