package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsMedium2(): List<Question> = listOf(

    // ── GEOGRAPHISCHE BESONDERHEITEN, KLIMAZONEN & NATURWUNDER ───────────────

    // Question 1 — Klimazonen: Koeppens Klassifikation  [correct: C → correctAnswer=2]
    Question(
        categoryId = 1,
        questionText = "Welche Klimazone nach Koeppen-Geiger ist durch heisse, trockene Sommer und milde, feuchte Winter gekennzeichnet — und auf welchen Kontinenten kommt sie vor?",
        answerA = "Das Kontinentalklima (Dfb) — es kommt ausschliesslich in Nordamerika und Russland vor",
        answerB = "Das Tropische Monsunklima (Am) — es tritt in Suedostasien, Westafrika und Mittelamerika auf",
        answerC = "Das Mediterrane Klima (Csa/Csb) — es tritt an den Westkuesten aller fuenf Kontinente auf (Mittelmeer, Kalifornien, Chile, Suedafrika, Suedaustralien)",
        answerD = "Das Stepppenklima (BSk) — es kommt nur in Innerasien und Nordafrika vor",
        correctAnswer = 2, // C
        explanation = "Das Mediterrane Klima (Csa = heiss-mediterran, Csb = warm-mediterran) ist durch trockene, heisse Sommer und feuchte, milde Winter charakterisiert. Es entsteht durch saisonal wandernde Hochdruckzellen (subtropische Antizyklonen im Sommer). Typische Regionen: Mittelmeerraum, Kalifornien, Zentralchile, Suedwestspitze Afrikas (Kapregion) und Suedwestaustralien.",
        difficulty = 2,
        funFact = "Das Mediterrane Klima umfasst nur ca. 2% der Erdoberfläche, beherbergt aber ueber 20% aller Pflanzenarten der Welt — es ist eines der artenreichsten Oekosysteme ueberhaupt. Der charakteristische Macchia-Strauchwuchs ist auf Feuer und Trockenheit spezialisiert."
    ),

    // Question 2 — Naturwunder: Grosse Blaue Loch  [correct: B → correctAnswer=1]
    Question(
        categoryId = 1,
        questionText = "Das 'Grosse Blaue Loch' (Great Blue Hole) vor der Kueste Belizes ist eines der markantesten Naturwunder des Planeten. Was ist es genau, und wie hat es sich gebildet?",
        answerA = "Es ist ein submariner Vulkankrater, der durch einen Unterwasserausbruch vor 10.000 Jahren entstand und sich seitdem mit Meerwasser gefuellt hat",
        answerB = "Es ist eine riesige Blauwasser-Hoehle (Marine Blue Hole), die waehrend der letzten Eiszeit als Kalksteinhohle ueber dem Meeresspiegel entstand und nach dem Anstieg des Meeresspiegels versank",
        answerC = "Es ist eine kuenstliche Tiefbohrung, die urspruenglich zur Oelfoerderung angelegt wurde und sich nach Aufgabe mit Meerwasser gefuellt hat",
        answerD = "Es ist ein Meteoriten-Einschlagskrater auf dem Meeresboden, der durch seine runde Form und Tiefe auffaellt",
        correctAnswer = 1, // B
        explanation = "Das Grosse Blaue Loch ist eine sogenannte Marine Blue Hole — eine in Kalkstein gebohrte Unterwasserhoehle. Waehrend der Eiszeiten (vor 15.000–18.000 Jahren) lag dieses Gebiet oberhalb des Meeresspiegels. Regenwasser loeuste den Kalkstein auf und bildete ein Hoehlen- und Stalaktitensystem. Als die Eisschilde schmolzen, stieg der Meeresspiegel, das Dach der Hoehle brach ein und sie versank. Das Loch ist 300 m breit, 125 m tief und gehört seit 1996 zum UNESCO-Weltnaturerbe.",
        difficulty = 2,
        funFact = "Jacques Cousteau erforschte das Grosse Blaue Loch 1971 und erklaerte es zu einem der besten Tauchplaetze der Welt. Im Inneren sind noch Stalaktiten zu sehen — die Beweise dafuer, dass die Hoehle einst ueber dem Meeresspiegel lag und in trockener Luft entstand."
    ),

    // Question 3 — Geographische Besonderheiten: Permafrost  [correct: D → correctAnswer=3]
    Question(
        categoryId = 1,
        questionText = "Permafrost bedeckt etwa 25% der Erdoberfleche des Festlandes. Was beschreibt Permafrost korrekt und warum ist sein Auftauen fuer das Klima problematisch?",
        answerA = "Permafrost ist Boden, der oberflaechlich im Winter gefriert und im Sommer vollstaendig auftaut — er speichert keine relevanten Mengen an organischem Kohlenstoff",
        answerB = "Permafrost ist ausschliesslich in der Antarktis und Groenland zu finden und schmilzt durch den anthropogenen Klimawandel mit einer Rate von 1 Meter pro Jahr",
        answerC = "Permafrost ist Gestein, das durch vulkanische Kaelte dauerhaft unter 0°C gehalten wird und beim Auftauen Schwefel freisetzt, der sauren Regen verursacht",
        answerD = "Permafrost ist Boden oder Gestein, das mindestens zwei aufeinanderfolgende Jahre unter 0°C bleibt; beim Auftauen wird riesige Mengen gespeicherten Kohlenstoffs als CO2 und Methan freigesetzt, was die Erwaermung beschleunigt",
        correctAnswer = 3, // D
        explanation = "Permafrost ist Boden oder Gestein, das mindestens zwei Jahre lang eine Temperatur von 0°C oder darunter hat. Er kommt in Sibirien, Nordkanada, Alaska, Tibet und anderen Hochgebirgen vor. Der gefrorene Boden konserviert ueber Jahrtausende organisches Material (abgestorbene Pflanzen, Tiere). Wenn er auftaut, zersetzen Bakterien dieses Material und setzen CO2 und Methan (ein 80x staerkeres Treibhausgas als CO2) frei. Dies koennte eine unkontrollierbare Rueckkopplungsschleife ausloesen.",
        difficulty = 2,
        funFact = "In sibirischem Permafrost wurden gut erhaltene Mammutreste gefunden — darunter ganze Tiere mit Haut, Fell und sogar unverdautem Mageninhalt. Die extreme Kaelte hat sie jahrtausendelang perfekt konserviert. Wissenschaftler diskutieren, ob Mammutzellen noch lebensfaehiges genetisches Material enthalten."
    ),

)
