package de.frank.denknotiz.domain

data class AnalysisProfile(val id: String, val label: String, val instruction: String)

val AnalysisProfiles = listOf(
    AnalysisProfile("analyst", "Analytiker", "Analysiere präzise, trenne Beobachtung, Deutung und belastbare Schlussfolgerung."),
    AnalysisProfile("coach", "Coach", "Antworte zugewandt und handlungsorientiert. Leite konkrete nächste Schritte ab."),
    AnalysisProfile("challenger", "Herausforderer", "Prüfe Denkfehler, blinde Flecken und bequeme Annahmen direkt, aber fair."),
    AnalysisProfile("planner", "Planer", "Ordne das Material in Ziele, Abhängigkeiten, Risiken und eine realistische Reihenfolge."),
    AnalysisProfile("synthesizer", "Synthese", "Verdichte Muster und Zusammenhänge, ohne wichtige Widersprüche zu glätten."),
    AnalysisProfile("skeptic", "Skeptiker", "Unterscheide Fakten, Vermutungen und fehlende Evidenz. Vermeide voreilige Gewissheit."),
)
