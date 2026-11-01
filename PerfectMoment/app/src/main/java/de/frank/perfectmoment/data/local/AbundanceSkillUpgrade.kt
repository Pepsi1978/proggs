package de.frank.perfectmoment.data.local

/**
 * Hebt die vorinstallierten Skills auf die Fülle-Regel: Jede Frage setzt das Ziel als erreicht voraus.
 *
 * Eine einzige Quelle für drei Wege: Neuinstallation (PreinstalledContent), Datenbank-Migration und
 * das Zurückholen alter Sicherungen. Eigene Änderungen in einem Skill bleiben erhalten, denn ersetzt
 * werden nur die genannten Stellen. Mehrfaches Anwenden ändert nichts.
 */
internal object AbundanceSkillUpgrade {
    const val RULE_HEADING = "FÜLLE-REGEL"

    val ruleText = """
$RULE_HEADING (geht allen anderen Stellen dieses Skills vor)

Jede einzelne Frage kommt aus der Fülle. Der gewünschte Zustand ist schon da, das Ziel ist schon erreicht. Gefragt wird, warum es so ist und warum es erreicht wurde, nie, wie man es irgendwann in Zukunft erreichen könnte. Das ist der große Unterschied. Keine Frage hebt einen Mangel hervor, keine sucht einen Weg.

Die stärksten Fragewörter sind Warum, Weshalb und Wieso. Bevorzuge sie: Mindestens die Hälfte aller Fragen beginnt damit.
„Warum habe ich einen sehr erholsamen Schlaf?“
„Weshalb schlafe ich generell sehr gut?“
„Warum schlafe ich an jedem Ort gut?“
„Weshalb fühle ich mich voller Frieden?“

Wie-Fragen stehen immer rückblickend in der Vergangenheitsform und setzen einen bereits erreichten Erfolg voraus. Andere Wie-Fragen gibt es nicht.
„Wie habe ich es geschafft, dass es mir sehr gut geht?“
„Wie habe ich genug Geld zusammenbekommen, um mir das neue Auto zu kaufen?“

Auch jedes andere Fragewort drückt die Fülle aus, im Präsens oder im Rückblick auf das schon Erreichte.
„Wodurch ist meine Fitness so stark gestiegen?“
„Was hat dazu geführt, dass ich heute so tief schlafe?“
„Woran merke ich heute, wie erholt ich bin?“
„Wann habe ich gemerkt, dass ich überall gut schlafe?“
„Wer freut sich mit mir darüber, dass ich so fit bin?“

Verboten sind suchende Zielfragen wie „Wie erreiche ich, dass ich einen guten Schlaf habe?“ oder „Was kann ich für besseren Schlaf tun?“. Ebenso verboten sind Wie-Fragen im Präsens und alles, was das Ziel in die Zukunft legt, als Möglichkeit behandelt oder als noch laufenden Weg beschreibt.

Die Beispiele zeigen nur die Form und werden nicht wörtlich übernommen. Sie stehen in der Ich-Form; bei einer anderen Perspektive gilt dieselbe Regel.

Letzte Prüfung jeder einzelnen Frage: Könnte sie auch jemand stellen, dem das Ziel noch fehlt und der es erst sucht? Dann wird sie ersetzt.
""".trim()

    private val replacements: Map<String, List<Pair<String, String>>> = mapOf(
        PreinstalledContent.RESEARCH_TEAM_SKILL_NAME to listOf(
            "  - Die Frage setzt voraus, dass Ressourcen, Fortschritte oder Lösungen bereits\n    teilweise vorhanden sind.\n" to
                "  - Die Frage setzt voraus, dass der gewünschte Zustand oder das Ziel bereits\n    vollständig erreicht ist.\n",
            "so formuliert, als sei er bereits im Entstehen oder\n    teilweise Realität." to
                "so formuliert, als sei er bereits erreicht und\n    selbstverständliche Realität.",
            "„Welche deiner aktuellen Routinen helfen dir bereits jetzt, deinen Körper\n    langfristig belastbar und jung zu halten?\"" to
                "„Warum ist dein Körper heute so belastbar und jung?\"",
            "„An welchen Momenten in deinem Schichtsystem spürst du schon jetzt am\n    deutlichsten, dass dein Schlaf etwas stabiler geworden ist?\"" to
                "„Weshalb schläfst du in jeder Phase deines Schichtsystems so tief?\"",
        ),
        PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME to listOf(
            "Immer Präsens. Der Zustand gilt jetzt. Niemals Zukunft, niemals „werde\", „bald\", „irgendwann\". Auch kein Fortschritt und keine Entwicklung — das Ziel ist erreicht, es wächst nicht erst." to
                "Präsens oder Rückblick. Der Zustand gilt jetzt, oder die Frage blickt in der Vergangenheitsform auf den schon erreichten Erfolg zurück. Niemals Zukunft, niemals „werde\", „bald\", „irgendwann\". Auch kein laufender Fortschritt — das Ziel ist erreicht, es wächst nicht erst.",
            "Warum / Wieso — „Warum ist mein Leben schön?\", „Wieso bleibe ich in jeder Situation entspannt?\"" to
                "Warum / Weshalb / Wieso (bevorzugt) — „Warum ist mein Leben schön?\", „Wieso bleibe ich in jeder Situation entspannt?\"",
            "Wie fühlt sich … an? — zielt auf das körperliche Gefühl statt auf Gedanken" to
                "Wie habe ich … geschafft? — blickt in der Vergangenheitsform auf den erreichten Erfolg zurück\nWeshalb fühle ich mich …? — zielt auf das körperliche Gefühl statt auf Gedanken",
            "🫁 Wie fühlt sich mein Atem an, wenn er von ganz allein langsamer wird?" to
                "🫁 Weshalb fließt mein Atem von ganz allein so langsam?",
            "🫀 Wie fühlt sich mein Herzschlag an, wenn er nach der Anstrengung so schnell wieder ruhig wird?" to
                "🫀 Wie habe ich es geschafft, dass mein Herz nach der Anstrengung so schnell ruhig schlägt?",
        ),
        PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME to listOf(
            "„Wie fühlt sich deine kraftvolle Fitness gerade in deinem Körper an?“" to
                "„Warum fühlt sich dein Körper gerade so kraftvoll an?“",
            "„Wie prägt deine starke körperliche Verfassung gerade dein Selbstbild?“" to
                "„Wie hast du es geschafft, dass dein Körper heute so stark ist?“",
            "- wie sie sich jetzt zeigt\n- wie sie sich jetzt anfühlt\n- wo sie im Körper wahrnehmbar ist" to
                "- warum sie sich jetzt so deutlich zeigt\n- weshalb sie sich jetzt so gut anfühlt\n- wie sie rückblickend erreicht wurde\n- wo sie im Körper wahrnehmbar ist",
            "- wie sie das Selbstbild prägt\n- wie sie die Wahrnehmung verändert\n- wie sie sich im Alltag ausdrückt\n- wie sie Beziehungen und Verbundenheit bereichert" to
                "- warum sie das Selbstbild prägt\n- weshalb sie die Wahrnehmung verändert\n- woran sie sich im Alltag zeigt\n- wodurch sie Beziehungen und Verbundenheit bereichert",
            "- ausschließlich in der Gegenwart\n" to
                "- in der Gegenwart oder im Rückblick auf den schon erreichten Erfolg\n",
            "- Warum …\n- Wieso …\n- Welche …" to
                "- Warum … (bevorzugt)\n- Weshalb … (bevorzugt)\n- Wieso … (bevorzugt)\n- Wodurch …\n- Welche …",
            "- Woran erkennst du …\n- Wie fühlt sich …\n- Wo spürst du …" to
                "- Woran erkennst du …\n- Wie hast du es geschafft, dass …\n- Wo spürst du …",
            "- Wie zeigt sich …\n" to "- Woran zeigt sich …\n",
            "- Wie verbinden sich …" to "- Was verbindet …",
        ),
        PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME to listOf(
            "8. Bevorzuge kurze, klare Frageanfänge: Warum, Weshalb, Woran, Was, Wo. Vermeide „Wodurch“, „Inwiefern“, „In welcher Weise“." to
                "8. Bevorzuge Warum, Weshalb und Wieso. Daneben passen Woran, Wodurch, Was und Wo. Vermeide „Inwiefern“, „In welcher Weise“.",
            "Schwach: „Wie fühlt es sich an, frei zu sein?“\nStark: „Warum ist das Gefühl von Freiheit so tief in mir?“\nStark: „Wie fühlt es sich an, dass ich ein freier Mensch bin?“" to
                "Schwach: „Wie fühlt es sich an, frei zu sein?“\nStark: „Warum ist das Gefühl von Freiheit so tief in mir?“\nStark: „Weshalb fühle ich mich als so freier Mensch?“",
            "Eine Frage nach der Art und Weise unterstellt leicht, dass der Zustand noch erreicht werden muss. Deshalb sind nur drei Bauformen erlaubt.\n\n" +
                "a) Gradfrage — sie fragt nach dem Ausmaß eines vorhandenen Zustands:\n„Wie tief ist meine Ruhe gerade?“\n„Wie deutlich spüre ich meine Kraft jetzt?“\n\n" +
                "b) Rückblickfrage in der Vergangenheitsform — der Zustand ist schon erreicht, gesucht wird der bereits gegangene Weg:\n„Wie habe ich es erreicht, dass ich mich frei fühle?“\n„Welchen Weg bin ich gegangen, damit ich heute so ruhig bin?“\n\n" +
                "c) Gefühlsfrage mit dass-Satz — das Sein steht im Nebensatz und gilt damit als Tatsache:\n„Wie fühlt es sich an, dass ich stark bin?“\n„Was für ein Gefühl ist es, dass mein Körper mich trägt?“" to
                "Eine Frage nach der Art und Weise unterstellt leicht, dass der Zustand noch erreicht werden muss. Deshalb ist nur eine Bauform erlaubt.\n\n" +
                "Rückblickfrage in der Vergangenheitsform — der Zustand ist schon erreicht, gesucht wird der bereits gegangene Weg:\n„Wie habe ich es erreicht, dass ich mich frei fühle?“\n„Wie habe ich es geschafft, dass ich heute so ruhig bin?“",
            "- Wieso …\n- Woran merke ich …" to "- Wieso …\n- Wodurch …\n- Woran merke ich …",
            "- Wie tief / wie stark / wie deutlich ist …\n- Wie habe ich es erreicht, dass …\n- Welchen Weg bin ich gegangen, damit …\n- Wie fühlt es sich an, dass ich … bin" to
                "- Wie habe ich es erreicht, dass …\n- Wie habe ich es geschafft, dass …\n- Welchen Weg bin ich gegangen, damit …",
            "10. Beginnt höchstens jede fünfte Frage mit „Wie“, und ausschließlich in einer der drei erlaubten Bauformen?" to
                "10. Beginnt höchstens jede fünfte Frage mit „Wie“, und ausschließlich als Rückblick in der Vergangenheitsform?",
        ),
        PreinstalledContent.ASSUMPTION_BOOST_2_SKILL_NAME to listOf(
            "8. Gefühls- und Gradfrage — Wie fühlt es sich an, dass ich … bin / Wie tief ist …\nNur in dieser Bauform. Das Sein steht im Nebensatz und gilt damit als Tatsache.\n„Wie fühlt es sich an, dass mein Körper so stark ist?“\n„Wie tief ist die Ruhe, die gerade in mir liegt?“" to
                "8. Gefühlsfrage — Warum fühle ich mich … / Weshalb fühle ich mich …\nDas Gefühl gilt als Tatsache, gesucht wird nur noch der Grund.\n„Warum fühlt sich mein Körper heute so stark an?“\n„Weshalb liegt gerade so viel Ruhe in mir?“",
            "Dazu erlaubt, sparsam: die Rückblickfrage in der Vergangenheitsform.\n„Wie habe ich es erreicht, dass ich heute so ruhig bin?“" to
                "Wie-Fragen gibt es nur als Rückblickfrage in der Vergangenheitsform.\n„Wie habe ich es erreicht, dass ich heute so ruhig bin?“",
            "Höchstens jede fünfte Frage beginnt mit „Wie“, und nur in den Formen aus Punkt 8 oder als Rückblick." to
                "Höchstens jede fünfte Frage beginnt mit „Wie“, und nur als Rückblick.",
            "Ebenso verboten: „Wie fühlt es sich an, … zu sein“ ohne dass-Satz. Diese Form beschreibt eine Möglichkeit statt meines Zustands." to
                "Ebenso verboten: jede Wie-Frage im Präsens, auch „Wie fühlt es sich an, …“ und „Wie tief ist …“.",
            "11. Höchstens jede fünfte Frage beginnt mit „Wie“, nur in erlaubter Bauform?" to
                "11. Höchstens jede fünfte Frage beginnt mit „Wie“, nur als Rückblick in der Vergangenheitsform?",
            "🌊 Wie tief ist die Freude, die ich gerade in mir spüre?" to
                "🌊 Warum ist die Freude in mir gerade so tief?",
            "❤️ Wie fühlt es sich an, dass ich ein so schönes Leben habe?" to
                "❤️ Wie habe ich es geschafft, dass ich ein so schönes Leben habe?",
        ),
    )

    private val insertBefore = mapOf(
        PreinstalledContent.RESEARCH_TEAM_SKILL_NAME to "UMGANG MIT DIREKTEN ANTWORTEN",
        PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME to "Ausgabeformat (hart)",
        PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME to "AUSGABE\n\nGib ausschließlich",
        PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME to "PRÜFUNG VOR DER AUSGABE",
        PreinstalledContent.ASSUMPTION_BOOST_2_SKILL_NAME to "PRÜFUNG VOR DER AUSGABE",
    )

    val skillNames: Set<String> get() = replacements.keys

    internal fun replacedPassages(skillName: String): List<String> =
        replacements[skillName].orEmpty().map { it.first }

    /** Skills unter anderem Namen sind Franks eigene und bleiben unverändert. */
    fun apply(skillName: String, text: String): String {
        val edits = replacements[skillName] ?: return text
        val replaced = edits.fold(text.replace("\r\n", "\n")) { current, (old, new) ->
            current.replace(old, new)
        }
        if (replaced.contains(RULE_HEADING)) return replaced
        val anchor = replaced.indexOf(insertBefore.getValue(skillName))
        return if (anchor >= 0) {
            replaced.substring(0, anchor) + ruleText + "\n\n" + replaced.substring(anchor)
        } else {
            replaced.trimEnd() + "\n\n" + ruleText
        }
    }
}
