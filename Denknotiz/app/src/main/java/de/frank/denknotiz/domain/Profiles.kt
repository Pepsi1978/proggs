package de.frank.denknotiz.domain

data class AnalysisProfile(
    val id: String,
    val label: String,
    val description: String,
    val instruction: String,
    val customName: Boolean = false,
)

val AnalysisProfiles = listOf(
    AnalysisProfile(
        "short",
        "Kurz und schnell",
        "Liefert eine knappe, direkte Antwort mit den wichtigsten Erkenntnissen und nächsten Schritten.",
        "Antworte knapp und direkt. Nenne nur die wichtigsten Erkenntnisse, Zusammenhänge und höchstens drei konkrete nächste Schritte.",
    ),
    AnalysisProfile(
        "normal",
        "Normale Länge",
        "Analysiert die Notizen ausgewogen, erklärt Zusammenhänge und leitet praktische Schlussfolgerungen ab.",
        "Analysiere ausgewogen und verständlich. Erkläre die wichtigsten Muster, Widersprüche und Schlussfolgerungen in gut lesbaren Absätzen.",
    ),
    AnalysisProfile(
        "detailed",
        "Sehr ausführlich",
        "Durchdenkt das gesamte Material gründlich, berücksichtigt Nuancen und beantwortet die Fokusfrage umfassend.",
        "Analysiere sehr ausführlich und gründlich. Berücksichtige alle relevanten Details, Unsicherheiten, Gegenargumente, Zusammenhänge und konkrete Handlungsoptionen.",
    ),
    AnalysisProfile("custom1", "Eigenes Profil 1", "Verwendet genau deine selbst formulierte Auswertungsanweisung.", "", customName = true),
    AnalysisProfile("custom2", "Eigenes Profil 2", "Verwendet genau deine selbst formulierte Auswertungsanweisung.", "", customName = true),
    AnalysisProfile("custom3", "Eigenes Profil 3", "Verwendet genau deine selbst formulierte Auswertungsanweisung.", "", customName = true),
)

fun profileLabel(profile: AnalysisProfile, names: Map<String, String>): String =
    names[profile.id]?.takeIf(String::isNotBlank) ?: profile.label

fun profileInstruction(profile: AnalysisProfile, instructions: Map<String, String>): String =
    instructions[profile.id] ?: profile.instruction
