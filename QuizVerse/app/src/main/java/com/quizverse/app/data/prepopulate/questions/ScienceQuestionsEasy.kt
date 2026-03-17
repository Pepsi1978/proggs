package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsEasy(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 2,
        questionText = "Wie viele Planeten hat unser Sonnensystem?",
        answerA = "7",
        answerB = "9",
        answerC = "8",
        answerD = "10",
        correctAnswer = 2, // C
        explanation = "Unser Sonnensystem hat 8 Planeten: Merkur, Venus, Erde, Mars, Jupiter, Saturn, Uranus und Neptun. Pluto wurde 2006 zur Zwergplanet herabgestuft.",
        difficulty = 1,
        funFact = "Pluto ist seit 2006 kein Planet mehr, sondern ein Zwergplanet – eine Entscheidung, die noch heute viele Menschen überrascht."
    ),

    // Question 2
    Question(
        categoryId = 2,
        questionText = "Was ist die chemische Formel für Wasser?",
        answerA = "CO2",
        answerB = "H2O",
        answerC = "NaCl",
        answerD = "O2",
        correctAnswer = 1, // B
        explanation = "Wasser besteht aus zwei Wasserstoffatomen (H) und einem Sauerstoffatom (O), daher die Formel H2O.",
        difficulty = 1,
        funFact = null
    ),

    // Question 3
    Question(
        categoryId = 2,
        questionText = "Welches ist das häufigste Element im menschlichen Körper?",
        answerA = "Kohlenstoff",
        answerB = "Calcium",
        answerC = "Eisen",
        answerD = "Sauerstoff",
        correctAnswer = 3, // D
        explanation = "Sauerstoff ist das häufigste Element im menschlichen Körper und macht etwa 65 % der Körpermasse aus, hauptsächlich in Form von Wasser.",
        difficulty = 1,
        funFact = null
    ),

    // Question 4
    Question(
        categoryId = 2,
        questionText = "Wie nennt man die kleinste Einheit des Lebens?",
        answerA = "Atom",
        answerB = "Zelle",
        answerC = "Molekül",
        answerD = "Gen",
        correctAnswer = 1, // B
        explanation = "Die Zelle ist die kleinste lebende Einheit. Alle Lebewesen bestehen aus einer oder mehreren Zellen.",
        difficulty = 1,
        funFact = "Der menschliche Körper besteht aus etwa 37 Billionen Zellen – das sind mehr Zellen als Sterne in der Milchstraße."
    ),

    // Question 5
    Question(
        categoryId = 2,
        questionText = "Was ist das chemische Symbol für Eisen?",
        answerA = "Ei",
        answerB = "Es",
        answerC = "Fe",
        answerD = "Er",
        correctAnswer = 2, // C
        explanation = "Das Symbol Fe kommt vom lateinischen Wort Ferrum, was Eisen bedeutet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 6
    Question(
        categoryId = 2,
        questionText = "Welches Organ pumpt das Blut durch den Körper?",
        answerA = "Lunge",
        answerB = "Leber",
        answerC = "Niere",
        answerD = "Herz",
        correctAnswer = 3, // D
        explanation = "Das Herz ist ein Hohlmuskel, der das Blut durch den gesamten Körper pumpt. Es schlägt im Ruhezustand etwa 60 bis 80 Mal pro Minute.",
        difficulty = 1,
        funFact = "Das Herz schlägt im Laufe eines 70-jährigen Lebens etwa 2,5 Milliarden Mal."
    ),

    // Question 7
    Question(
        categoryId = 2,
        questionText = "Was bezeichnet man als die Einheit der elektrischen Stromstärke?",
        answerA = "Volt",
        answerB = "Watt",
        answerC = "Ampere",
        answerD = "Ohm",
        correctAnswer = 2, // C
        explanation = "Die elektrische Stromstärke wird in Ampere (A) gemessen, benannt nach dem französischen Physiker André-Marie Ampère.",
        difficulty = 1,
        funFact = null
    ),

    // Question 8
    Question(
        categoryId = 2,
        questionText = "Welcher Planet ist der größte in unserem Sonnensystem?",
        answerA = "Saturn",
        answerB = "Neptun",
        answerC = "Uranus",
        answerD = "Jupiter",
        correctAnswer = 3, // D
        explanation = "Jupiter ist der größte Planet in unserem Sonnensystem. Er ist so groß, dass alle anderen Planeten zusammen hineinpassen würden.",
        difficulty = 1,
        funFact = "Jupiter hat mindestens 95 bekannte Monde – der bekannteste ist Ganymed, der größte Mond im Sonnensystem."
    ),

    // Question 9
    Question(
        categoryId = 2,
        questionText = "Wie viele Kammern hat das menschliche Herz?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "6",
        correctAnswer = 2, // C
        explanation = "Das menschliche Herz besteht aus vier Kammern: zwei Vorhöfen (Atrien) und zwei Hauptkammern (Ventrikel).",
        difficulty = 1,
        funFact = null
    ),

    // Question 10
    Question(
        categoryId = 2,
        questionText = "Was ist die Lichtgeschwindigkeit im Vakuum ungefähr?",
        answerA = "100.000 km/s",
        answerB = "300.000 km/s",
        answerC = "500.000 km/s",
        answerD = "1.000.000 km/s",
        correctAnswer = 1, // B
        explanation = "Die Lichtgeschwindigkeit im Vakuum beträgt genau 299.792.458 m/s, also knapp 300.000 km/s.",
        difficulty = 1,
        funFact = "Licht benötigt von der Sonne zur Erde etwa 8 Minuten und 20 Sekunden."
    ),

    // Question 11
    Question(
        categoryId = 2,
        questionText = "Welches Gas atmen Menschen aus?",
        answerA = "Sauerstoff",
        answerB = "Stickstoff",
        answerC = "Kohlendioxid",
        answerD = "Methan",
        correctAnswer = 2, // C
        explanation = "Menschen atmen hauptsächlich Kohlendioxid (CO2) aus, das beim Stoffwechsel entsteht. Außerdem ist noch etwas Stickstoff und nicht verbrauchter Sauerstoff enthalten.",
        difficulty = 1,
        funFact = null
    ),

    // Question 12
    Question(
        categoryId = 2,
        questionText = "Was ist der Siedepunkt von Wasser bei normalem Luftdruck?",
        answerA = "90 °C",
        answerB = "80 °C",
        answerC = "120 °C",
        answerD = "100 °C",
        correctAnswer = 3, // D
        explanation = "Wasser siedet bei normalem Luftdruck (1013 hPa) bei genau 100 °C. In höheren Lagen mit geringerem Luftdruck siedet es früher.",
        difficulty = 1,
        funFact = "Auf dem Gipfel des Mount Everest siedet Wasser bereits bei etwa 70 °C, weil dort der Luftdruck viel geringer ist."
    ),

    // Question 13
    Question(
        categoryId = 2,
        questionText = "Wie nennt man den Prozess, bei dem Pflanzen Licht in Energie umwandeln?",
        answerA = "Atmung",
        answerB = "Gärung",
        answerC = "Fotosynthese",
        answerD = "Osmose",
        correctAnswer = 2, // C
        explanation = "Bei der Fotosynthese nutzen Pflanzen Sonnenlicht, Wasser und Kohlendioxid, um Glukose (Zucker) und Sauerstoff zu erzeugen.",
        difficulty = 1,
        funFact = "Pflanzen haben über Milliarden von Jahren die Sauerstoffatmosphäre der Erde durch Fotosynthese aufgebaut."
    ),

    // Question 14
    Question(
        categoryId = 2,
        questionText = "Was ist das chemische Symbol für Natrium?",
        answerA = "N",
        answerB = "Na",
        answerC = "Nt",
        answerD = "Nm",
        correctAnswer = 1, // B
        explanation = "Das Symbol Na kommt vom lateinischen Wort Natrium. Natrium ist ein wichtiger Bestandteil von Kochsalz (NaCl).",
        difficulty = 1,
        funFact = null
    ),

    // Question 15
    Question(
        categoryId = 2,
        questionText = "Welches ist das häufigste Gas in der Erdatmosphäre nach Stickstoff?",
        answerA = "Argon",
        answerB = "Wasserstoff",
        answerC = "Sauerstoff",
        answerD = "Kohlendioxid",
        correctAnswer = 2, // C
        explanation = "Nach Stickstoff (78 %) ist Sauerstoff mit etwa 21 % das zweithäufigste Gas in der Erdatmosphäre.",
        difficulty = 1,
        funFact = null
    ),

    // Question 16
    Question(
        categoryId = 2,
        questionText = "Wie viele Zähne hat ein erwachsener Mensch ohne Weisheitszähne?",
        answerA = "24",
        answerB = "28",
        answerC = "32",
        answerD = "30",
        correctAnswer = 1, // B
        explanation = "Ohne Weisheitszähne hat ein Erwachsener 28 Zähne. Mit Weisheitszähnen können es bis zu 32 sein.",
        difficulty = 1,
        funFact = null
    ),

    // Question 17
    Question(
        categoryId = 2,
        questionText = "Was ist die Einheit der Masse im internationalen Einheitensystem (SI)?",
        answerA = "Gramm",
        answerB = "Pfund",
        answerC = "Kilogramm",
        answerD = "Tonne",
        correctAnswer = 2, // C
        explanation = "Die SI-Basiseinheit der Masse ist das Kilogramm (kg). Alle anderen Masseneinheiten werden daraus abgeleitet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 18
    Question(
        categoryId = 2,
        questionText = "Welcher Planet wird auch der rote Planet genannt?",
        answerA = "Jupiter",
        answerB = "Venus",
        answerC = "Saturn",
        answerD = "Mars",
        correctAnswer = 3, // D
        explanation = "Mars wird als roter Planet bezeichnet, weil sein Boden eisenoxidreichen Staub (Rost) enthält, der dem Planeten seine rötliche Farbe verleiht.",
        difficulty = 1,
        funFact = "Die höchste Erhebung im Sonnensystem, der Olympus Mons, befindet sich auf dem Mars und ist etwa 22 km hoch."
    ),

    // Question 19
    Question(
        categoryId = 2,
        questionText = "Wie nennt man die Schicht der Erde, auf der wir leben?",
        answerA = "Mantel",
        answerB = "Kern",
        answerC = "Erdkruste",
        answerD = "Asthenosphäre",
        correctAnswer = 2, // C
        explanation = "Die Erdkruste ist die äußerste feste Schicht der Erde. Sie ist 5 bis 70 km dick und besteht aus Gestein.",
        difficulty = 1,
        funFact = null
    ),

    // Question 20
    Question(
        categoryId = 2,
        questionText = "Was ist die Gravitationsbeschleunigung auf der Erdoberfläche ungefähr?",
        answerA = "5 m/s²",
        answerB = "15 m/s²",
        answerC = "20 m/s²",
        answerD = "10 m/s²",
        correctAnswer = 3, // D
        explanation = "Die Gravitationsbeschleunigung auf der Erdoberfläche beträgt genau 9,81 m/s², was man häufig auf etwa 10 m/s² rundet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 21
    Question(
        categoryId = 2,
        questionText = "Welches ist das härteste natürliche Mineral?",
        answerA = "Quarz",
        answerB = "Diamant",
        answerC = "Rubin",
        answerD = "Granit",
        correctAnswer = 1, // B
        explanation = "Diamant ist das härteste natürliche Mineral und erreicht die Höchststufe 10 auf der Mohs-Härteskala.",
        difficulty = 1,
        funFact = "Diamant besteht ausschließlich aus Kohlenstoff – genau wie Graphit (Bleistift), nur in einer anderen Kristallstruktur."
    ),

    // Question 22
    Question(
        categoryId = 2,
        questionText = "Wie viele Augen hat eine Spinne typischerweise?",
        answerA = "2",
        answerB = "4",
        answerC = "6",
        answerD = "8",
        correctAnswer = 3, // D
        explanation = "Die meisten Spinnen haben 8 Augen, obwohl Anzahl und Anordnung je nach Art variieren. Einige Höhlenspinnen haben gar keine Augen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 23
    Question(
        categoryId = 2,
        questionText = "Was ist das chemische Symbol für Silber?",
        answerA = "Si",
        answerB = "Sl",
        answerC = "Ag",
        answerD = "Sv",
        correctAnswer = 2, // C
        explanation = "Das Symbol Ag kommt vom lateinischen Wort Argentum, was Silber bedeutet.",
        difficulty = 1,
        funFact = "Silber hat die höchste elektrische und thermische Leitfähigkeit aller Metalle."
    ),

    // Question 24
    Question(
        categoryId = 2,
        questionText = "Wie nennt man die Kraft, die Objekte zur Erde zieht?",
        answerA = "Magnetismus",
        answerB = "Reibung",
        answerC = "Schwerkraft",
        answerD = "Auftrieb",
        correctAnswer = 2, // C
        explanation = "Die Schwerkraft (Gravitation) ist eine der vier Grundkräfte der Natur und zieht alle Massen gegenseitig an.",
        difficulty = 1,
        funFact = null
    ),

    // Question 25
    Question(
        categoryId = 2,
        questionText = "Welcher Knochenteil bildet den Mittelpunkt des menschlichen Skeletts?",
        answerA = "Rippe",
        answerB = "Oberschenkelknochen",
        answerC = "Wirbelsäule",
        answerD = "Becken",
        correctAnswer = 2, // C
        explanation = "Die Wirbelsäule ist die zentrale Stütze des menschlichen Skeletts und besteht aus 24 beweglichen Wirbeln sowie Kreuz- und Steißbein.",
        difficulty = 1,
        funFact = null
    ),

    // Question 26
    Question(
        categoryId = 2,
        questionText = "Was ist der längste Knochen im menschlichen Körper?",
        answerA = "Humerus",
        answerB = "Tibia",
        answerC = "Fibula",
        answerD = "Oberschenkelknochen",
        correctAnswer = 3, // D
        explanation = "Der Oberschenkelknochen (Femur) ist der längste und stärkste Knochen im menschlichen Körper.",
        difficulty = 1,
        funFact = "Der Oberschenkelknochen ist stärker als Beton und kann ein Mehrfaches des Körpergewichts tragen."
    ),

    // Question 27
    Question(
        categoryId = 2,
        questionText = "Wie heißt das Organ, das für das Sehen verantwortlich ist?",
        answerA = "Ohr",
        answerB = "Auge",
        answerC = "Nase",
        answerD = "Zunge",
        correctAnswer = 1, // B
        explanation = "Das Auge ist das Sinnesorgan für das Sehen. Es wandelt Licht in elektrische Signale um, die das Gehirn verarbeitet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 28
    Question(
        categoryId = 2,
        questionText = "Welcher Aggregatzustand hat Wasser bei Raumtemperatur?",
        answerA = "Gasförmig",
        answerB = "Fest",
        answerC = "Flüssig",
        answerD = "Plasma",
        correctAnswer = 2, // C
        explanation = "Bei Raumtemperatur (ca. 20 °C) ist Wasser flüssig. Es wird erst bei 0 °C fest und bei 100 °C gasförmig.",
        difficulty = 1,
        funFact = null
    ),

    // Question 29
    Question(
        categoryId = 2,
        questionText = "Was ist das chemische Symbol für Sauerstoff?",
        answerA = "Sa",
        answerB = "So",
        answerC = "S",
        answerD = "O",
        correctAnswer = 3, // D
        explanation = "Das chemische Symbol für Sauerstoff ist O, vom englischen und lateinischen Oxygen/Oxygenium abgeleitet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 30
    Question(
        categoryId = 2,
        questionText = "Welcher Teil des menschlichen Gehirns ist für das Gleichgewicht zuständig?",
        answerA = "Großhirn",
        answerB = "Kleinhirn",
        answerC = "Hirnstamm",
        answerD = "Hippocampus",
        correctAnswer = 1, // B
        explanation = "Das Kleinhirn (Cerebellum) ist hauptsächlich für die Koordination von Bewegungen und das Gleichgewicht verantwortlich.",
        difficulty = 1,
        funFact = "Obwohl das Kleinhirn nur 10 % des Hirnvolumens ausmacht, enthält es mehr als die Hälfte aller Nervenzellen des Gehirns."
    ),

    // Question 31
    Question(
        categoryId = 2,
        questionText = "Wie nennt man die Umwandlung von Eis zu Wasser?",
        answerA = "Verdunstung",
        answerB = "Kondensation",
        answerC = "Schmelzen",
        answerD = "Sublimation",
        correctAnswer = 2, // C
        explanation = "Wenn Eis sich in Wasser verwandelt, nennt man diesen Vorgang Schmelzen. Er findet bei 0 °C statt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 32
    Question(
        categoryId = 2,
        questionText = "Was ist das Zahlzeichen der Kreiszahl Pi (π) ungefähr?",
        answerA = "2,71",
        answerB = "3,14",
        answerC = "1,41",
        answerD = "4,67",
        correctAnswer = 1, // B
        explanation = "Die Kreiszahl Pi (π) ist eine mathematische Konstante und beträgt ungefähr 3,14159... Sie beschreibt das Verhältnis von Umfang zu Durchmesser eines Kreises.",
        difficulty = 1,
        funFact = "Pi ist eine irrationale Zahl – ihre Dezimalstellen enden nie und wiederholen sich nie. Bisher wurden über 100 Billionen Stellen berechnet."
    ),

    // Question 33
    Question(
        categoryId = 2,
        questionText = "Wie heißt der Prozess, bei dem Wasser verdunstet und als Regen zurückkommt?",
        answerA = "Photosynthese",
        answerB = "Osmose",
        answerC = "Wasserkreislauf",
        answerD = "Diffusion",
        correctAnswer = 2, // C
        explanation = "Der Wasserkreislauf beschreibt den kontinuierlichen Kreislauf des Wassers zwischen Ozeanen, Atmosphäre und Landflächen durch Verdunstung, Kondensation und Niederschlag.",
        difficulty = 1,
        funFact = null
    ),

    // Question 34
    Question(
        categoryId = 2,
        questionText = "Was ist das Ergebnis von 2 hoch 10?",
        answerA = "512",
        answerB = "100",
        answerC = "20",
        answerD = "1024",
        correctAnswer = 3, // D
        explanation = "2 hoch 10 ergibt 1024, da 2 zehnmal mit sich selbst multipliziert wird: 2×2×2×2×2×2×2×2×2×2 = 1024.",
        difficulty = 1,
        funFact = "1024 Bytes sind ein Kilobyte – daher kommt die Zahl in der Informatik so häufig vor."
    ),

    // Question 35
    Question(
        categoryId = 2,
        questionText = "Welches Organ filtert das Blut und produziert Urin?",
        answerA = "Milz",
        answerB = "Niere",
        answerC = "Leber",
        answerD = "Darm",
        correctAnswer = 1, // B
        explanation = "Die Nieren filtern das Blut und scheiden Abfallstoffe sowie überschüssiges Wasser als Urin aus. Der Mensch hat zwei Nieren.",
        difficulty = 1,
        funFact = "Die Nieren filtern täglich etwa 180 Liter Blutflüssigkeit, von der aber nur 1,5 bis 2 Liter als Urin ausgeschieden werden."
    ),

    // Question 36
    Question(
        categoryId = 2,
        questionText = "Wie nennt man Tiere, die Blut saugen?",
        answerA = "Herbivoren",
        answerB = "Karnivoren",
        answerC = "Omnivoren",
        answerD = "Hämatophagen",
        correctAnswer = 3, // D
        explanation = "Tiere, die sich von Blut ernähren, nennt man Hämatophagen. Bekannte Beispiele sind Stechmücken, Blutegel und Vampire-Fledermäuse.",
        difficulty = 1,
        funFact = null
    ),

    // Question 37
    Question(
        categoryId = 2,
        questionText = "Wie viele Beine hat ein Insekt?",
        answerA = "4",
        answerB = "8",
        answerC = "6",
        answerD = "10",
        correctAnswer = 2, // C
        explanation = "Alle Insekten haben genau 6 Beine. Das ist eines der wichtigsten Merkmale, das Insekten von Spinnen (8 Beine) unterscheidet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 38
    Question(
        categoryId = 2,
        questionText = "Was ist der Gefrierpunkt von Wasser in Grad Celsius?",
        answerA = "-10 °C",
        answerB = "10 °C",
        answerC = "0 °C",
        answerD = "4 °C",
        correctAnswer = 2, // C
        explanation = "Wasser gefriert bei 0 °C zu Eis. Dieser Wert gilt bei normalem Luftdruck (1013 hPa).",
        difficulty = 1,
        funFact = "Reines Wasser kann unter idealen Bedingungen bis auf -40 °C abgekühlt werden, ohne zu gefrieren – man nennt das unterkühltes Wasser."
    ),

    // Question 39
    Question(
        categoryId = 2,
        questionText = "Welcher Planet hat ausgeprägte Ringe um sich?",
        answerA = "Mars",
        answerB = "Jupiter",
        answerC = "Saturn",
        answerD = "Neptun",
        correctAnswer = 2, // C
        explanation = "Saturn ist für sein beeindruckendes Ringsystem bekannt, das aus Milliarden von Eis- und Gesteinstrümmer besteht.",
        difficulty = 1,
        funFact = "Saturns Ringe sind zwar riesig im Durchmesser (bis zu 280.000 km), aber nur durchschnittlich 10 Meter dünn."
    ),

    // Question 40
    Question(
        categoryId = 2,
        questionText = "Wie heißt das rote Pigment, das für den Sauerstofftransport im Blut verantwortlich ist?",
        answerA = "Melanin",
        answerB = "Chlorophyll",
        answerC = "Hämoglobin",
        answerD = "Keratin",
        correctAnswer = 2, // C
        explanation = "Hämoglobin ist ein rotes Protein in den roten Blutkörperchen, das Sauerstoff in der Lunge aufnimmt und im Körper verteilt.",
        difficulty = 1,
        funFact = "Das Eisen im Hämoglobin ist der Grund, warum Blut rot ist – und warum wir Eisen in der Nahrung benötigen."
    ),

    // Question 41
    Question(
        categoryId = 2,
        questionText = "Was gibt einem Blatt seine grüne Farbe?",
        answerA = "Karotin",
        answerB = "Chlorophyll",
        answerC = "Anthocyan",
        answerD = "Melanin",
        correctAnswer = 1, // B
        explanation = "Chlorophyll ist das grüne Farbpigment in Pflanzen, das Sonnenlicht für die Fotosynthese absorbiert.",
        difficulty = 1,
        funFact = "Im Herbst bauen Pflanzen das Chlorophyll ab – daher werden Blätter gelb, orange und rot, weil andere Farbstoffe sichtbar werden."
    ),

    // Question 42
    Question(
        categoryId = 2,
        questionText = "Wie nennt man die Wissenschaft, die sich mit dem Universum befasst?",
        answerA = "Geologie",
        answerB = "Meteorologie",
        answerC = "Astronomie",
        answerD = "Astrophysik",
        correctAnswer = 2, // C
        explanation = "Die Astronomie ist die Wissenschaft von Himmelskörpern, dem Universum und den Gesetzen, die es regieren.",
        difficulty = 1,
        funFact = null
    ),

    // Question 43
    Question(
        categoryId = 2,
        questionText = "Was ist die Einheit der Temperatur im SI-System?",
        answerA = "Celsius",
        answerB = "Fahrenheit",
        answerC = "Rankine",
        answerD = "Kelvin",
        correctAnswer = 3, // D
        explanation = "Die SI-Einheit der Temperatur ist Kelvin (K). Der absolute Nullpunkt liegt bei 0 K, was -273,15 °C entspricht.",
        difficulty = 1,
        funFact = null
    ),

    // Question 44
    Question(
        categoryId = 2,
        questionText = "Wie viele Beine hat eine Spinne?",
        answerA = "6",
        answerB = "4",
        answerC = "10",
        answerD = "8",
        correctAnswer = 3, // D
        explanation = "Spinnen sind Spinnentiere (Arachnida) und haben immer 8 Beine. Das unterscheidet sie von Insekten, die 6 Beine haben.",
        difficulty = 1,
        funFact = "Spinnen produzieren Seide, die im Verhältnis zu ihrem Gewicht stärker als Stahl ist."
    ),

    // Question 45
    Question(
        categoryId = 2,
        questionText = "Was ist die Erde für eine Art von Himmelskörper?",
        answerA = "Stern",
        answerB = "Mond",
        answerC = "Planet",
        answerD = "Asteroid",
        correctAnswer = 2, // C
        explanation = "Die Erde ist ein Planet und der dritte von der Sonne aus. Sie ist der einzige bekannte Himmelskörper, auf dem Leben existiert.",
        difficulty = 1,
        funFact = "Die Erde ist nicht perfekt rund, sondern an den Polen leicht abgeflacht – man nennt diese Form Rotationsellipsoid."
    ),

    // Question 46
    Question(
        categoryId = 2,
        questionText = "Was ist das Symbol für das chemische Element Kohlenstoff?",
        answerA = "Ko",
        answerB = "K",
        answerC = "Ca",
        answerD = "C",
        correctAnswer = 3, // D
        explanation = "Das chemische Symbol für Kohlenstoff ist C, vom lateinischen Carbo (Kohle) abgeleitet.",
        difficulty = 1,
        funFact = "Kohlenstoff ist die Grundlage allen bekannten Lebens auf der Erde – alle organischen Moleküle enthalten Kohlenstoff."
    ),

    // Question 47
    Question(
        categoryId = 2,
        questionText = "Wie nennt man den Himmelskörper, der die Erde umkreist?",
        answerA = "Sonne",
        answerB = "Mond",
        answerC = "Mars",
        answerD = "Komet",
        correctAnswer = 1, // B
        explanation = "Der Mond ist der natürliche Satellit der Erde und umkreist sie in etwa 27 Tagen.",
        difficulty = 1,
        funFact = "Der Mond entfernt sich jedes Jahr um etwa 3,8 cm von der Erde – in einigen Milliarden Jahren wird er so weit weg sein, dass totale Sonnenfinsternisse nicht mehr möglich sind."
    ),

    // Question 48
    Question(
        categoryId = 2,
        questionText = "Wie heißt die äußerste Schicht der Sonne, die wir sehen können?",
        answerA = "Chromosphäre",
        answerB = "Korona",
        answerC = "Photosphäre",
        answerD = "Konvektionszone",
        correctAnswer = 2, // C
        explanation = "Die Photosphäre ist die leuchtende Oberfläche der Sonne, die wir direkt sehen können. Sie hat eine Temperatur von etwa 5.500 °C.",
        difficulty = 1,
        funFact = null
    ),

    // Question 49
    Question(
        categoryId = 2,
        questionText = "Was benötigt eine Pflanze nicht zur Fotosynthese?",
        answerA = "Sonnenlicht",
        answerB = "Sauerstoff",
        answerC = "Wasser",
        answerD = "Kohlendioxid",
        correctAnswer = 1, // B
        explanation = "Zur Fotosynthese benötigen Pflanzen Sonnenlicht, Wasser und Kohlendioxid. Sauerstoff ist das Abfallprodukt, das dabei freigesetzt wird.",
        difficulty = 1,
        funFact = "Pflanzen produzieren den gesamten Sauerstoff in unserer Atmosphäre als Nebenprodukt der Fotosynthese."
    ),

    // Question 50
    Question(
        categoryId = 2,
        questionText = "Welche Blutgruppe gilt als universeller Spender?",
        answerA = "A",
        answerB = "B",
        answerC = "AB",
        answerD = "0",
        correctAnswer = 3, // D
        explanation = "Blutgruppe 0 negativ gilt als universeller Spender, da diese Erythrozyten an Menschen aller Blutgruppen übertragen werden können.",
        difficulty = 1,
        funFact = "Nur etwa 7 % der Menschen haben die Blutgruppe 0 negativ – diese Personen sind bei Notfällen besonders wertvolle Blutspender."
    ),

)
