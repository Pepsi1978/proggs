package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsMedium(): List<Question> = listOf(

    // ── MEDIUM (difficulty = 2) ── 50 questions ──────────────────────────────

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Sri Lanka?",
        answerA = "Colombo",
        answerB = "Kandy",
        answerC = "Galle",
        answerD = "Sri Jayawardenepura Kotte",
        correctAnswer = 3, // D
        explanation = "Sri Jayawardenepura Kotte ist die offizielle Hauptstadt Sri Lankas, während Colombo die größte Stadt und wirtschaftliches Zentrum des Landes ist.",
        difficulty = 2,
        funFact = "Der Name der Hauptstadt ist mit 27 Buchstaben eine der längsten Hauptstadtbezeichnungen der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Durch wie viele Länder fließt der Amazonas?",
        answerA = "Nur durch Brasilien",
        answerB = "Durch 2 Länder",
        answerC = "Durch 9 Länder",
        answerD = "Durch 3 Länder",
        correctAnswer = 2, // C
        explanation = "Das Amazonasbecken erstreckt sich über 9 Länder: Brasilien, Peru, Kolumbien, Venezuela, Ecuador, Bolivien, Guyana, Suriname und Französisch-Guayana.",
        difficulty = 2,
        funFact = "Der Amazonas trägt etwa 20 % des gesamten Süßwassers bei, das weltweit in die Ozeane fließt."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Myanmar (Birma)?",
        answerA = "Rangun",
        answerB = "Mandalay",
        answerC = "Naypyidaw",
        answerD = "Bagan",
        correctAnswer = 2, // C
        explanation = "Naypyidaw ist seit 2006 die offizielle Hauptstadt Myanmars. Die Stadt wurde speziell als neue Hauptstadt geplant und gebaut.",
        difficulty = 2,
        funFact = "Naypyidaw gilt als eine der am dünnsten besiedelten Hauptstädte der Welt – trotz riesiger Infrastruktur sind die Straßen oft fast menschenleer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean ist der tiefste der Welt?",
        answerA = "Atlantischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Arktischer Ozean",
        answerD = "Pazifischer Ozean",
        correctAnswer = 3, // D
        explanation = "Der Pazifische Ozean ist der tiefste Ozean – der Marianengraben liegt dort mit einer Tiefe von etwa 11.034 Metern (Challengertief) als tiefster Punkt der Erde.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Nachbarländer?",
        answerA = "Russland",
        answerB = "Brasilien",
        answerC = "China",
        answerD = "Deutschland",
        correctAnswer = 0, // A
        explanation = "Russland grenzt an 14 verschiedene Länder und hat damit die meisten Nachbarländer aller Staaten der Welt.",
        difficulty = 2,
        funFact = "China teilt sich mit Russland den zweiten Platz – es grenzt ebenfalls an 14 Länder."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Paraguay?",
        answerA = "Santa Cruz",
        answerB = "Asunción",
        answerC = "Montevideo",
        answerD = "Sucre",
        correctAnswer = 1, // B
        explanation = "Asunción ist die Hauptstadt und mit über 500.000 Einwohnern die größte Stadt Paraguays. Sie liegt am Río Paraguay.",
        difficulty = 2,
        funFact = "Asunción ist eine der ältesten dauerhaft bewohnten Städte Südamerikas, gegründet im Jahr 1537."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die natürliche Grenze zwischen Europa und Asien?",
        answerA = "Kaukasus",
        answerB = "Karpaten",
        answerC = "Ural",
        answerD = "Alpen",
        correctAnswer = 2, // C
        explanation = "Der Ural gilt traditionell als geografische Grenze zwischen Europa und Asien. Er erstreckt sich über etwa 2.500 km von Nord nach Süd durch Russland.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Titicacasee?",
        answerA = "Nur in Peru",
        answerB = "In Chile und Argentinien",
        answerC = "In Peru und Bolivien",
        answerD = "Nur in Bolivien",
        correctAnswer = 2, // C
        explanation = "Der Titicacasee liegt auf der Grenze zwischen Peru und Bolivien auf einer Höhe von etwa 3.812 Metern und ist der höchstgelegene schiffbare See der Welt.",
        difficulty = 2,
        funFact = "Der Titicacasee ist so groß wie Puerto Rico und enthält schwimmende Inseln aus Schilf, auf denen die Uros-Gemeinschaft lebt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge verbindet das Schwarze Meer mit dem Mittelmeer?",
        answerA = "Straße von Gibraltar",
        answerB = "Bosporus und Dardanellen",
        answerC = "Straße von Hormus",
        answerD = "Straße von Messina",
        correctAnswer = 1, // B
        explanation = "Das Schwarze Meer ist über den Bosporus mit dem Marmarameer verbunden, das seinerseits über die Dardanellen in das Ägäische Meer und damit ins Mittelmeer mündet.",
        difficulty = 2,
        funFact = "Die Meerenge des Bosporus teilt Istanbul in einen europäischen und einen asiatischen Teil."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das flächenmäßig größte Land Südamerikas?",
        answerA = "Argentinien",
        answerB = "Peru",
        answerC = "Kolumbien",
        answerD = "Brasilien",
        correctAnswer = 3, // D
        explanation = "Brasilien ist mit einer Fläche von 8,5 Millionen km² das größte Land Südamerikas und das fünftgrößte Land der Welt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Klimazone gehört die Sahara an?",
        answerA = "Tropisches Klima",
        answerB = "Subtropisches Wüstenklima",
        answerC = "Steppenklima",
        answerD = "Mediterranes Klima",
        correctAnswer = 1, // B
        explanation = "Die Sahara gehört zum subtropischen Wüstenklima (Koppen-Klassifikation: BWh), das durch extreme Trockenheit, hohe Temperaturen und geringe Niederschläge gekennzeichnet ist.",
        difficulty = 2,
        funFact = "Obwohl die Sahara als heiße Wüste bekannt ist, kann es dort im Winter nachts unter den Gefrierpunkt kalt werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kasachstan?",
        answerA = "Almaty",
        answerB = "Taschkent",
        answerC = "Astana",
        answerD = "Bischkek",
        correctAnswer = 2, // C
        explanation = "Astana (früher Nur-Sultan und davor Akmola) ist seit 1997 die Hauptstadt Kasachstans. Die Stadt wurde speziell als neue Hauptstadt entwickelt.",
        difficulty = 2,
        funFact = "Kasachstan ist das größte Binnenland der Welt und das neuntgrößte Land insgesamt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss ist der längste in Europa?",
        answerA = "Rhein",
        answerB = "Donau",
        answerC = "Wolga",
        answerD = "Dnjepr",
        correctAnswer = 2, // C
        explanation = "Die Wolga ist mit etwa 3.530 km der längste Fluss Europas. Sie fließt vollständig durch Russland und mündet ins Kaspische Meer.",
        difficulty = 2,
        funFact = "Die Wolga entwässert etwa 40 % des europäischen Teils Russlands und gilt als Nationalfluss der Russen."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Victoriasee?",
        answerA = "Nur in Kenia",
        answerB = "In Uganda, Tansania und Kenia",
        answerC = "In Äthiopien und Sudan",
        answerD = "Nur in Tansania",
        correctAnswer = 1, // B
        explanation = "Der Victoriasee liegt an den Grenzen von Uganda, Tansania und Kenia. Er ist der größte Süßwassersee Afrikas und der zweitgrößte der Welt.",
        difficulty = 2,
        funFact = "Der Victoriasee ist eine wichtige Quelle des Nils und versorgt Millionen von Menschen mit Fisch und Trinkwasser."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge ist das längste der Welt?",
        answerA = "Himalaja",
        answerB = "Anden",
        answerC = "Rockies",
        answerD = "Alpen",
        correctAnswer = 1, // B
        explanation = "Die Anden sind mit einer Länge von etwa 7.500 km das längste Gebirge der Welt. Sie erstrecken sich entlang der gesamten Westküste Südamerikas.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Straße trennt Europa von Afrika?",
        answerA = "Straße von Messina",
        answerB = "Straße von Korsika",
        answerC = "Straße von Gibraltar",
        answerD = "Straße von Sizilien",
        correctAnswer = 2, // C
        explanation = "Die Straße von Gibraltar ist die Meerenge zwischen Spanien (Europa) und Marokko (Afrika). An ihrer schmalsten Stelle ist sie nur etwa 14 km breit.",
        difficulty = 2,
        funFact = "Die Straße von Gibraltar ist eine der meistbefahrenen Wasserstraßen der Welt – täglich passieren sie rund 300 Schiffe."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Afrikas?",
        answerA = "Äthiopien",
        answerB = "Ägypten",
        answerC = "Demokratische Republik Kongo",
        answerD = "Nigeria",
        correctAnswer = 3, // D
        explanation = "Nigeria ist mit über 220 Millionen Einwohnern das bevölkerungsreichste Land Afrikas und eines der bevölkerungsreichsten Länder der Welt.",
        difficulty = 2,
        funFact = "Schätzungen zufolge könnte Nigeria bis 2050 das drittbevölkerungsreichste Land der Welt werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Usbekistan?",
        answerA = "Samarkand",
        answerB = "Buchara",
        answerC = "Taschkent",
        answerD = "Andijon",
        correctAnswer = 2, // C
        explanation = "Taschkent ist die Hauptstadt und mit fast 3 Millionen Einwohnern die größte Stadt Usbekistans sowie eine der größten Städte Zentralasiens.",
        difficulty = 2,
        funFact = "Taschkent wurde 1966 von einem schweren Erdbeben fast vollständig zerstört und danach von der Sowjetunion neu aufgebaut."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Wüste ist die kälteste der Welt?",
        answerA = "Sahara",
        answerB = "Gobi",
        answerC = "Atacama",
        answerD = "Antarktis",
        correctAnswer = 3, // D
        explanation = "Die Antarktis gilt als größte und kälteste Wüste der Welt. Sie ist eine Eiswüste mit weniger als 200 mm Niederschlag pro Jahr.",
        difficulty = 2,
        funFact = "Die tiefste je gemessene Temperatur auf der Erde wurde in der Antarktis gemessen: -89,2 °C an der sowjetischen Wostok-Station."
    ),

    Question(
        categoryId = 1,
        questionText = "An welchem Meer liegt das Land Jordanien?",
        answerA = "Nur am Mittelmeer",
        answerB = "Am Roten Meer und Toten Meer",
        answerC = "Nur am Toten Meer",
        answerD = "Am Persischen Golf",
        correctAnswer = 1, // B
        explanation = "Jordanien hat Zugang zum Roten Meer (Golf von Akaba) und liegt am Toten Meer, das es sich mit Israel und dem Westjordanland teilt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das kleinste Land Südamerikas?",
        answerA = "Uruguay",
        answerB = "Paraguay",
        answerC = "Suriname",
        answerD = "Guyana",
        correctAnswer = 2, // C
        explanation = "Suriname ist mit einer Fläche von etwa 163.820 km² das kleinste souveräne Land Südamerikas.",
        difficulty = 2,
        funFact = "Suriname ist das einzige Land Südamerikas, in dem Niederländisch die Amtssprache ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Durch welches Land fließt der Mekong hauptsächlich?",
        answerA = "Vietnam",
        answerB = "Thailand",
        answerC = "Kambodscha",
        answerD = "Myanmar",
        correctAnswer = 0, // A
        explanation = "Der Mekong fließt durch 6 Länder: China, Myanmar, Laos, Thailand, Kambodscha und Vietnam. Das längste Stück fließt durch Laos, er mündet aber in Vietnam ins Meer.",
        difficulty = 2,
        funFact = "Der Mekong ist einer der artenreichsten Flüsse der Welt – nur der Amazonas hat mehr Fischarten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen Italien und der Balkanhalbinsel?",
        answerA = "Tyrrhenisches Meer",
        answerB = "Ligurisches Meer",
        answerC = "Ionisches Meer",
        answerD = "Adriatisches Meer",
        correctAnswer = 3, // D
        explanation = "Das Adriatische Meer liegt zwischen der italienischen Halbinsel im Westen und der Balkanhalbinsel (Slowenien, Kroatien, Bosnien, Montenegro, Albanien) im Osten.",
        difficulty = 2,
        funFact = "Venedig liegt direkt an der Adriaküste und ist durch Kanäle mit dem Meer verbunden – die Stadt versinkt jährlich um etwa 2 mm."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Mosambik?",
        answerA = "Beira",
        answerB = "Maputo",
        answerC = "Nampula",
        answerD = "Chimoio",
        correctAnswer = 1, // B
        explanation = "Maputo (früher Lourenço Marques) ist die Hauptstadt und größte Stadt Mosambiks. Sie liegt im südlichsten Teil des Landes an der Maputo-Bucht.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat den höchsten Anteil an Wüstenfläche?",
        answerA = "Afrika",
        answerB = "Asien",
        answerC = "Australien",
        answerD = "Südamerika",
        correctAnswer = 2, // C
        explanation = "Australien hat den höchsten Anteil an Wüstenfläche aller Kontinente: Etwa 35 % der Gesamtfläche sind Wüste oder Halbwüste.",
        difficulty = 2,
        funFact = "Australiens Wüsten sind nicht heiß-sandig wie die Sahara, sondern meist kiesige Steinwüsten mit niedrigem Strauchwerk."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Fluss Nordamerikas?",
        answerA = "Mississippi",
        answerB = "Rio Grande",
        answerC = "Missouri",
        answerD = "Mackenzie",
        correctAnswer = 2, // C
        explanation = "Der Missouri River ist mit etwa 3.767 km der längste Fluss Nordamerikas. Er mündet in den Mississippi, weshalb das Mississippi-Missouri-System zusammen als längster Flusslauf gilt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land grenzt im Norden an Afghanistan?",
        answerA = "Iran",
        answerB = "Pakistan",
        answerC = "Tadschikistan, Usbekistan und Turkmenistan",
        answerD = "Kirgistan und Kasachstan",
        correctAnswer = 2, // C
        explanation = "Im Norden grenzt Afghanistan an Tadschikistan, Usbekistan und Turkmenistan (alle drei ehemalige Sowjetrepubliken). Im Osten liegt Pakistan und China, im Westen der Iran.",
        difficulty = 2,
        funFact = "Afghanistan hat einen sehr kleinen Grenzabschnitt mit China im äußersten Nordosten – den sogenannten Wakhan-Korridor."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Papua-Neuguinea?",
        answerA = "Suva",
        answerB = "Honiara",
        answerC = "Port Moresby",
        answerD = "Nuku'alofa",
        correctAnswer = 2, // C
        explanation = "Port Moresby ist die Hauptstadt und größte Stadt Papua-Neuguineas. Sie liegt an der Südküste der Insel Neuguinea.",
        difficulty = 2,
        funFact = "Papua-Neuguinea ist eines der sprachlich vielfältigsten Länder der Welt – dort werden über 800 verschiedene Sprachen gesprochen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher der folgenden Seen ist der tiefste der Welt?",
        answerA = "Kaspisches Meer",
        answerB = "Tanganjikasee",
        answerC = "Baikalsee",
        answerD = "Oberer See",
        correctAnswer = 2, // C
        explanation = "Der Baikalsee in Sibirien ist mit einer maximalen Tiefe von 1.642 Metern der tiefste See der Welt und enthält etwa 20 % des gesamten Süßwassers der Erde.",
        difficulty = 2,
        funFact = "Der Baikalsee ist auch der älteste See der Welt – er entstand vor etwa 25 bis 30 Millionen Jahren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die längste Küstenlinie der Welt?",
        answerA = "Russland",
        answerB = "Australien",
        answerC = "Norwegen",
        answerD = "Kanada",
        correctAnswer = 3, // D
        explanation = "Kanada hat mit etwa 202.080 km die längste Küstenlinie der Welt, da das Land von drei Ozeanen umgeben ist und tausende Inseln besitzt.",
        difficulty = 2,
        funFact = "Kanadas Küstenlinie ist so lang, dass man sie nicht in einem Leben ablaufen könnte – selbst bei 30 km pro Tag würde es fast 19 Jahre dauern."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Biom liegt das größte zusammenhängende Waldsystem der Erde?",
        answerA = "Tropischer Regenwald (Amazonas)",
        answerB = "Taiga (borealer Nadelwald)",
        answerC = "Gemäßigter Laubwald",
        answerD = "Mangrovenwald",
        correctAnswer = 1, // B
        explanation = "Die Taiga (borealer Nadelwald) ist das größte zusammenhängende Biom der Erde und erstreckt sich über Kanada, Skandinavien und Sibirien.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Äthiopien?",
        answerA = "Nairobi",
        answerB = "Addis Abeba",
        answerC = "Dakar",
        answerD = "Khartum",
        correctAnswer = 1, // B
        explanation = "Addis Abeba ist die Hauptstadt Äthiopiens und mit über 5 Millionen Einwohnern die größte Stadt des Landes. Sie ist auch Sitz der Afrikanischen Union.",
        difficulty = 2,
        funFact = "Addis Abeba bedeutet auf Amharisch 'neue Blume' und liegt auf einer Höhe von etwa 2.355 Metern – damit ist sie eine der höchstgelegenen Hauptstädte der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Golf liegt zwischen der Arabischen Halbinsel und dem Iran?",
        answerA = "Golf von Bengalen",
        answerB = "Golf von Oman",
        answerC = "Persischer Golf",
        answerD = "Golf von Aden",
        correctAnswer = 2, // C
        explanation = "Der Persische Golf liegt zwischen der Arabischen Halbinsel im Südwesten und dem Iran im Norden und Nordosten. Er ist über die Straße von Hormus mit dem Golf von Oman verbunden.",
        difficulty = 2,
        funFact = "Im Persischen Golf befinden sich einige der größten Erdölreserven der Welt – die Anrainerstaaten fördern täglich Millionen Barrel Rohöl."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Europas (ohne Russland)?",
        answerA = "Frankreich",
        answerB = "Italien",
        answerC = "Spanien",
        answerD = "Deutschland",
        correctAnswer = 3, // D
        explanation = "Deutschland ist mit etwa 84 Millionen Einwohnern das bevölkerungsreichste Land der Europäischen Union und das bevölkerungsreichste Land Europas ohne Russland.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel ist die bevölkerungsreichste der Welt?",
        answerA = "Borneo",
        answerB = "Honshu",
        answerC = "Java",
        answerD = "Sumatra",
        correctAnswer = 2, // C
        explanation = "Java ist mit über 150 Millionen Einwohnern die bevölkerungsreichste Insel der Welt. Dort liegt auch die indonesische Hauptstadt Jakarta.",
        difficulty = 2,
        funFact = "Obwohl Java nur 7 % der Gesamtfläche Indonesiens ausmacht, leben dort über 56 % der indonesischen Bevölkerung."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo liegt der Atacama-Graben, einer der tiefsten Meeresgraben der Welt?",
        answerA = "Im Atlantischen Ozean vor Westafrika",
        answerB = "Im Indischen Ozean vor Indien",
        answerC = "Im Pazifik vor der Küste Perus und Chiles",
        answerD = "Im Arktischen Ozean",
        correctAnswer = 2, // C
        explanation = "Der Peru-Chile-Graben (auch Atacama-Graben) liegt im Pazifischen Ozean vor der Westküste Südamerikas und erreicht eine Tiefe von bis zu 8.065 Metern.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Weißrussland (Belarus)?",
        answerA = "Lwiw",
        answerB = "Minsk",
        answerC = "Riga",
        answerD = "Grodno",
        correctAnswer = 1, // B
        explanation = "Minsk ist die Hauptstadt und größte Stadt von Belarus (Weißrussland). Sie wurde im Zweiten Weltkrieg fast vollständig zerstört und danach im sowjetischen Stil wiederaufgebaut.",
        difficulty = 2,
        funFact = "Minsk ist eine der am gründlichsten im sozialistischen Realismus-Stil erbauten Hauptstädte – breite Prachtboulevards und monumentale Gebäude prägen das Stadtbild."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches europäische Land hat die meisten aktiven Vulkane?",
        answerA = "Griechenland",
        answerB = "Italien",
        answerC = "Island",
        answerD = "Portugal",
        correctAnswer = 2, // C
        explanation = "Island sitzt auf dem Mittelatlantischen Rücken und hat mit über 130 aktiven Vulkanen die meisten Europas. Es gibt dort durchschnittlich einen Vulkanausbruch alle 4-5 Jahre.",
        difficulty = 2,
        funFact = "Island bezieht nahezu seine gesamte Energie aus erneuerbaren Quellen – hauptsächlich aus Geothermie und Wasserkraft, dank seiner vulkanischen Aktivität."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge trennt die Inseln Sumatra und Java?",
        answerA = "Malakka-Straße",
        answerB = "Sunda-Straße",
        answerC = "Lombok-Straße",
        answerD = "Banda-Straße",
        correctAnswer = 1, // B
        explanation = "Die Sunda-Straße liegt zwischen den indonesischen Inseln Sumatra und Java. Hier versank 1883 Krakatau bei einem gewaltigen Vulkanausbruch im Meer.",
        difficulty = 2,
        funFact = "Der Krakatau-Ausbruch von 1883 war so laut, dass er noch in 5.000 km Entfernung zu hören war – eine der lautesten Explosionen in der aufgezeichneten Geschichte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die höchste Bevölkerungsdichte in Westafrika?",
        answerA = "Ghana",
        answerB = "Senegal",
        answerC = "Côte d'Ivoire",
        answerD = "Nigeria",
        correctAnswer = 3, // D
        explanation = "Nigeria hat mit über 220 Einwohnern pro km² die höchste Bevölkerungsdichte unter den großen westafrikanischen Ländern und ist absolut das bevölkerungsreichste Land Afrikas.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss bildet die Grenze zwischen den USA und Mexiko?",
        answerA = "Colorado River",
        answerB = "Rio Grande",
        answerC = "Arkansas River",
        answerD = "Gila River",
        correctAnswer = 1, // B
        explanation = "Der Rio Grande (in Mexiko: Río Bravo del Norte) bildet auf einer Länge von etwa 2.018 km die Grenze zwischen den USA und Mexiko.",
        difficulty = 2,
        funFact = "Der Rio Grande gilt in manchen Abschnitten als einer der am stärksten belasteten Flüsse der Welt – intensive Landwirtschaft und Wasserentnahmen haben seinen Wasserstand stark sinken lassen."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Angola?",
        answerA = "Luanda",
        answerB = "Lusaka",
        answerC = "Kinshasa",
        answerD = "Libreville",
        correctAnswer = 0, // A
        explanation = "Luanda ist die Hauptstadt und mit über 8 Millionen Einwohnern die größte Stadt Angolas. Sie liegt direkt am Atlantischen Ozean.",
        difficulty = 2,
        funFact = "Luanda war lange eine der teuersten Städte der Welt für Expats, vor allem wegen der Ölindustrie und der damit verbundenen hohen Lebenshaltungskosten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Hochplateau gilt als 'Dach der Welt'?",
        answerA = "Dekkanplateau",
        answerB = "Anatolisches Plateau",
        answerC = "Tibetisches Plateau",
        answerD = "Mongolisches Plateau",
        correctAnswer = 2, // C
        explanation = "Das Tibetische Plateau (auch Tibetplateau genannt) wird als 'Dach der Welt' bezeichnet. Mit einer durchschnittlichen Höhe von über 4.500 m ist es das höchste und größte Hochplateau der Erde.",
        difficulty = 2,
        funFact = "Auf dem Tibetischen Plateau entspringen einige der wichtigsten Flüsse Asiens: der Yangtze, der Mekong, der Indus und der Brahmaputra."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt im Norden Australiens?",
        answerA = "Korallenmeer",
        answerB = "Tasmanische See",
        answerC = "Timorsee",
        answerD = "Arabisches Meer",
        correctAnswer = 2, // C
        explanation = "Im Norden Australiens liegt die Timorsee, die Australien von der Insel Timor trennt. Östlich davon liegt die Araforasee.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten UNESCO-Welterbestätten?",
        answerA = "China",
        answerB = "Spanien",
        answerC = "Frankreich",
        answerD = "Italien",
        correctAnswer = 3, // D
        explanation = "Italien führt die Liste der UNESCO-Welterbestätten an und hat mit 58 eingetragenen Stätten die meisten weltweit (Stand 2024).",
        difficulty = 2,
        funFact = "China liegt dicht hinter Italien mit 57 Welterbestätten auf Platz zwei."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge liegt zwischen Frankreich und Spanien?",
        answerA = "Alpen",
        answerB = "Apenninen",
        answerC = "Pyrenäen",
        answerD = "Cevennen",
        correctAnswer = 2, // C
        explanation = "Die Pyrenäen bilden die natürliche Grenze zwischen Frankreich und Spanien und erstrecken sich vom Atlantik bis zum Mittelmeer über etwa 430 km.",
        difficulty = 2,
        funFact = "Im Herzen der Pyrenäen liegt das winzige Fürstentum Andorra – ein Kleinstaat der weder zu Frankreich noch zu Spanien gehört."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche der folgenden Städte liegt am nächsten zum Äquator?",
        answerA = "Nairobi",
        answerB = "Lagos",
        answerC = "Dakar",
        answerD = "Addis Abeba",
        correctAnswer = 0, // A
        explanation = "Nairobi liegt auf einem Breitengrad von etwa 1,3° Süd und ist damit die einzige der genannten Städte, die fast direkt auf dem Äquator liegt.",
        difficulty = 2,
        funFact = "Nairobi ist die Hauptstadt Kenias und trotz seiner Lage nahe dem Äquator dank seiner Höhe von 1.795 m über dem Meeresspiegel relativ kühl."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land ist bekannt dafür, dass es sowohl an den Atlantik als auch an den Pazifik grenzt?",
        answerA = "Mexiko",
        answerB = "Kolumbien",
        answerC = "Panama",
        answerD = "Nicaragua",
        correctAnswer = 1, // B
        explanation = "Kolumbien ist das einzige Land Südamerikas, das sowohl Küsten am Atlantischen Ozean (Karibik) als auch am Pazifischen Ozean hat.",
        difficulty = 2,
        funFact = "Kolumbien ist eines der artenreichsten Länder der Welt – es hat mehr Vogelarten als jedes andere Land auf der Erde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte Binnenland der Welt (kein Zugang zum Meer)?",
        answerA = "Mongolei",
        answerB = "Niger",
        answerC = "Tschad",
        answerD = "Kasachstan",
        correctAnswer = 3, // D
        explanation = "Kasachstan ist mit einer Fläche von 2,7 Millionen km² das größte Binnenland der Welt. Es grenzt an Russland, China, Kirgisistan, Usbekistan und Turkmenistan.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wo liegt der höchste Wasserfall der Welt?",
        answerA = "Brasilien",
        answerB = "Venezuela",
        answerC = "Sambia",
        answerD = "Norwegen",
        correctAnswer = 1, // B
        explanation = "Der Salto Ángel (Angel Falls) in Venezuela ist mit einer Fallhöhe von 979 Metern der höchste Wasserfall der Welt. Er liegt im Canaima-Nationalpark.",
        difficulty = 2,
        funFact = "Der Salto Ángel war bis zu seiner 'Entdeckung' durch den amerikanischen Piloten Jimmy Angel 1933 außerhalb Venezuelas kaum bekannt."
    ),

    // ── MEDIUM (difficulty = 2) ── questions 51–100 ──────────────────────────

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Bhutan?",
        answerA = "Kathmandu",
        answerB = "Thimphu",
        answerC = "Paro",
        answerD = "Punakha",
        correctAnswer = 1, // B
        explanation = "Thimphu ist die Hauptstadt und größte Stadt Bhutans. Es ist eine der wenigen Hauptstädte der Welt ohne Ampeln – der Verkehr wird von Polizisten per Hand geregelt.",
        difficulty = 2,
        funFact = "Bhutan ist das einzige Land der Welt, das sein Bruttonationalglück offiziell misst und als Leitgröße der Wirtschaftspolitik verwendet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer trennt die Arabische Halbinsel von Afrika?",
        answerA = "Arabisches Meer",
        answerB = "Rotes Meer",
        answerC = "Golf von Aden",
        answerD = "Indischer Ozean",
        correctAnswer = 1, // B
        explanation = "Das Rote Meer liegt zwischen der Arabischen Halbinsel im Osten und dem afrikanischen Kontinent (Ägypten, Sudan, Eritrea, Dschibuti) im Westen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat sowohl Küsten am Atlantik als auch am Indischen Ozean?",
        answerA = "Mosambik",
        answerB = "Tansania",
        answerC = "Südafrika",
        answerD = "Namibia",
        correctAnswer = 2, // C
        explanation = "Südafrika ist das einzige afrikanische Land, das sowohl am Atlantischen Ozean (Westküste) als auch am Indischen Ozean (Ostküste) liegt.",
        difficulty = 2,
        funFact = "Am Kap der Guten Hoffnung treffen die Strömungen beider Ozeane aufeinander – nicht wie oft angenommen am Kap Agulhas, dem südlichsten Punkt Afrikas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge erstreckt sich durch Marokko, Algerien und Tunesien?",
        answerA = "Hoggar-Gebirge",
        answerB = "Atlasgebirge",
        answerC = "Rif-Gebirge",
        answerD = "Ahaggar",
        correctAnswer = 1, // B
        explanation = "Das Atlasgebirge erstreckt sich über rund 2.500 km durch Marokko, Algerien und Tunesien und erreicht im Toubkal (Marokko) eine Höhe von 4.167 m.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Golf befindet sich zwischen der Skandinavischen Halbinsel und Finnland?",
        answerA = "Golf von Bottnien",
        answerB = "Golf von Finnland",
        answerC = "Rigaer Meerbusen",
        answerD = "Dänische Straße",
        correctAnswer = 0, // A
        explanation = "Der Golf von Bottnien liegt zwischen Schweden im Westen und Finnland im Osten und ist der nördlichste Teil der Ostsee.",
        difficulty = 2,
        funFact = "Der Golf von Bottnien friert im Winter fast vollständig zu – Eisbrecher sind dort im Winter unverzichtbar."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Turkmenistan?",
        answerA = "Bischkek",
        answerB = "Duschanbe",
        answerC = "Aschgabat",
        answerD = "Nukus",
        correctAnswer = 2, // C
        explanation = "Aschgabat ist die Hauptstadt und größte Stadt Turkmenistans. Die Stadt ist bekannt für ihre weißmarmornen Gebäude und gigantischen Goldstatuen des ehemaligen Diktators Niyazov.",
        difficulty = 2,
        funFact = "Aschgabat hält mehrere Guinness-Weltrekorde, u. a. für die höchste Dichte weißer Marmorgebäude in einer Stadt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge verbindet den Atlantischen Ozean mit dem Pazifischen Ozean im Süden Südamerikas?",
        answerA = "Drakepassage",
        answerB = "Magellanstraße",
        answerC = "Beagle-Kanal",
        answerD = "Falklandstraße",
        correctAnswer = 1, // B
        explanation = "Die Magellanstraße liegt zwischen dem südlichen Zipfel Südamerikas und der Insel Feuerland und verbindet Atlantik und Pazifik. Sie wurde 1520 von Ferdinand Magellan durchquert.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Großteil des Hindukusch-Gebirges?",
        answerA = "Pakistan und Indien",
        answerB = "Afghanistan und Pakistan",
        answerC = "Tadschikistan und Kirgistan",
        answerD = "Iran und Irak",
        correctAnswer = 1, // B
        explanation = "Der Hindukusch erstreckt sich hauptsächlich durch Afghanistan und Pakistan und bildet eine wichtige geografische Trennlinie zwischen Zentral- und Südasien.",
        difficulty = 2,
        funFact = "Der Name Hindukusch bedeutet in persischer Überlieferung 'Töter der Inder' – möglicherweise eine Anspielung auf die gefährlichen Übergänge für Händler und Soldaten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die größte Anzahl an Inseln weltweit?",
        answerA = "Indonesien",
        answerB = "Philippinen",
        answerC = "Schweden",
        answerD = "Norwegen",
        correctAnswer = 2, // C
        explanation = "Schweden hat offiziell über 220.000 Inseln, von denen jedoch nur etwa 1.000 dauerhaft bewohnt sind. Damit übertrifft Schweden sogar Indonesien in der reinen Inselanzahl.",
        difficulty = 2,
        funFact = "Indonesien hingegen hat die meisten bewohnten Inseln und die flächenmäßig größte Inselgruppe der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Klimaphänomen ist verantwortlich für die extreme Trockenheit der Atacama-Wüste?",
        answerA = "El Niño und der Äquatorialgürtel",
        answerB = "Der Humboldt-Strom und das Regenschattenphänomen der Anden",
        answerC = "Das subtropische Hochdruckgebiet allein",
        answerD = "Monsunwinde aus dem Pazifik",
        correctAnswer = 1, // B
        explanation = "Der kalte Humboldt-Strom kühlt die aufsteigende Luft und verhindert Niederschläge. Die Anden blockieren zusätzlich Feuchtigkeit aus dem Osten – zusammen machen sie die Atacama zur trockensten Wüste der Welt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Laos?",
        answerA = "Phnom Penh",
        answerB = "Vientiane",
        answerC = "Luang Prabang",
        answerD = "Pakse",
        correctAnswer = 1, // B
        explanation = "Vientiane ist die Hauptstadt und größte Stadt von Laos. Sie liegt am Mekong an der Grenze zu Thailand und ist eine der kleinsten und ruhigsten Hauptstädte Südostasiens.",
        difficulty = 2,
        funFact = "Laos ist das einzige Binnenland Südostasiens und wird auch als 'Land der Millionen Elefanten' bezeichnet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge liegt in der Mitte der Iberischen Halbinsel?",
        answerA = "Sierra Nevada",
        answerB = "Kantabrisches Gebirge",
        answerC = "Zentralkordillere (Cordillera Central)",
        answerD = "Montes de Toledo",
        correctAnswer = 2, // C
        explanation = "Die Cordillera Central (Zentralkordillere) teilt die Iberische Halbinsel in Richtung West-Ost und trennt das Meseta Norte von der Meseta Sur. Sie enthält den höchsten Gipfel Kontinentalspaniens.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Südostasiens?",
        answerA = "Vietnam",
        answerB = "Thailand",
        answerC = "Philippinen",
        answerD = "Indonesien",
        correctAnswer = 3, // D
        explanation = "Indonesien ist mit über 275 Millionen Einwohnern das bevölkerungsreichste Land Südostasiens und das viertbevölkerungsreichste Land der Welt.",
        difficulty = 2,
        funFact = "Indonesien besteht aus mehr als 17.000 Inseln und ist damit der größte Inselstaat der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kanal verbindet den Atlantik mit dem Pazifik in Mittelamerika?",
        answerA = "Suezkanal",
        answerB = "Kaledoniaknal",
        answerC = "Panamakanal",
        answerD = "Nicaraguakanal",
        correctAnswer = 2, // C
        explanation = "Der Panamakanal in Panama verbindet den Atlantischen Ozean (Karibik) mit dem Pazifischen Ozean. Er wurde 1914 eröffnet und ist eine der wichtigsten Schiffahrtsrouten der Welt.",
        difficulty = 2,
        funFact = "Schiffe, die den Panamakanal durchqueren, fahren – entgegen der Intuition – von Ost nach West auf dem Atlantik- und von West nach Ost auf dem Pazifik-Teil."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das höchste Gebirge Afrikas?",
        answerA = "Drakensberge",
        answerB = "Rwenzori-Gebirge",
        answerC = "Äthiopisches Hochland",
        answerD = "Kilimanjaro-Massiv",
        correctAnswer = 3, // D
        explanation = "Das Kilimanjaro-Massiv in Tansania enthält mit dem Kibo-Gipfel (5.895 m) den höchsten Punkt Afrikas. Es ist ein Vulkan, kein Gebirge im klassischen Sinne, aber die höchste Erhebung Afrikas.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Nordzypern (offiziell nicht anerkannt)?",
        answerA = "Limassol",
        answerB = "Larnaka",
        answerC = "Lefkosia (Nikosia-Nord)",
        answerD = "Kyrenia",
        correctAnswer = 2, // C
        explanation = "Die Türkische Republik Nordzypern, die nur von der Türkei anerkannt wird, hat Lefkosia (Nordzyperns Teil von Nikosia) als Hauptstadt. Nikosia ist damit die letzte geteilte Hauptstadt Europas.",
        difficulty = 2,
        funFact = "Nikosia ist die einzige geteilte Hauptstadt der Welt – eine UN-Pufferzone trennt den griechisch-zypriotischen Süden vom türkisch-zypriotischen Norden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Klimazone dominiert in Nordkanada und Nordrussland?",
        answerA = "Kontinentales Klima",
        answerB = "Subarktisches Klima (Taiga)",
        answerC = "Polares Tundraklima",
        answerD = "Arktisches Eisklima",
        correctAnswer = 1, // B
        explanation = "Das subarktische Klima (auch boreales Klima) dominiert in den großen Nadelwaldgürteln Nordkanadas und Nordrusslands. Es zeichnet sich durch sehr kalte Winter und kurze kühle Sommer aus.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land grenzt im Osten an Ungarn?",
        answerA = "Slowakei",
        answerB = "Kroatien",
        answerC = "Rumänien",
        answerD = "Serbien",
        correctAnswer = 2, // C
        explanation = "Rumänien grenzt im Osten und Nordosten an Ungarn. Ungarn hat insgesamt sieben Nachbarländer: Österreich, Slowakei, Ukraine, Rumänien, Serbien, Kroatien und Slowenien.",
        difficulty = 2,
        funFact = "Zwischen Ungarn und Rumänien liegt Siebenbürgen (Transsilvanien) – eine historische Region, die jahrhundertelang zu Ungarn gehörte und heute Teil Rumäniens ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss mündet in das Kaspische Meer?",
        answerA = "Don",
        answerB = "Wolga",
        answerC = "Ob",
        answerD = "Ural",
        correctAnswer = 1, // B
        explanation = "Die Wolga, der längste Fluss Europas, mündet in das Kaspische Meer. Das Kaspische Meer hat keinen natürlichen Abfluss zum Ozean – es ist ein abflussloser Salzsee.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das am dichtesten besiedelte Land der Welt (unter den großen Ländern über 1 Mio. Einwohner)?",
        answerA = "Indien",
        answerB = "Bangladesh",
        answerC = "Japan",
        answerD = "Niederlande",
        correctAnswer = 1, // B
        explanation = "Bangladesh hat mit über 1.250 Einwohnern pro km² die höchste Bevölkerungsdichte unter allen bevölkerungsreichen Ländern der Welt und ist damit dicht besiedelter als Indien oder Japan.",
        difficulty = 2,
        funFact = "Bangladesh hat eine ähnliche Einwohnerzahl wie Russland, ist aber fast 120-mal kleiner."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Berg gilt als heiligster Berg Japans?",
        answerA = "Mount Kita",
        answerB = "Mount Yari",
        answerC = "Mount Fuji",
        answerD = "Mount Ontake",
        correctAnswer = 2, // C
        explanation = "Der Fuji (3.776 m) ist der höchste Berg Japans und ein heiliger Schreinberg der Shinto-Religion. Er ist seit 2013 UNESCO-Weltkulturerbe.",
        difficulty = 2,
        funFact = "Der Fuji ist ein potenziell aktiver Vulkan – sein letzter Ausbruch war 1707. Jährlich besteigen ihn etwa 300.000 Menschen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Wüste erstreckt sich über den Norden Chinas und der Mongolei?",
        answerA = "Taklamakan",
        answerB = "Karakum",
        answerC = "Lut-Wüste",
        answerD = "Gobi",
        correctAnswer = 3, // D
        explanation = "Die Gobi ist eine Kaltewüste, die sich über weite Teile der Mongolei und Nordchinas erstreckt. Sie ist mit etwa 1,3 Millionen km² die größte Wüste Asiens.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegen die Karakoram-Berge mit dem K2?",
        answerA = "Indien und Nepal",
        answerB = "Pakistan und China",
        answerC = "Afghanistan und Tadschikistan",
        answerD = "Nur in Pakistan",
        correctAnswer = 1, // B
        explanation = "Das Karakoram-Gebirge liegt hauptsächlich in Pakistan und China (Xinjiang) und enthält mit dem K2 (8.611 m) den zweithöchsten Berg der Erde.",
        difficulty = 2,
        funFact = "Der K2 gilt als schwieriger zu besteigen als der Mount Everest – er hat eine deutlich höhere Todesrate und wurde im Winter erst 2021 erstmals bezwungen."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Mauretanien?",
        answerA = "Dakar",
        answerB = "Bamako",
        answerC = "Conakry",
        answerD = "Nouakchott",
        correctAnswer = 3, // D
        explanation = "Nouakchott ist die Hauptstadt und größte Stadt Mauretaniens. Sie liegt direkt am Atlantik und wurde erst nach der Unabhängigkeit 1960 zur Hauptstadt ausgebaut.",
        difficulty = 2,
        funFact = "Nouakchott ist eine der am schnellsten wachsenden Städte Afrikas – Klimawandel und Desertifikation treiben immer mehr Nomaden aus der Sahara in die Stadt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean ist der kleinste der Welt?",
        answerA = "Südlicher Ozean",
        answerB = "Arktischer Ozean",
        answerC = "Atlantischer Ozean",
        answerD = "Indischer Ozean",
        correctAnswer = 1, // B
        explanation = "Der Arktische Ozean (Nordpolarmeer) ist mit einer Fläche von etwa 14,06 Millionen km² der kleinste der fünf Weltmeere.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land liegt vollständig zwischen Frankreich und Spanien?",
        answerA = "Monaco",
        answerB = "San Marino",
        answerC = "Andorra",
        answerD = "Liechtenstein",
        correctAnswer = 2, // C
        explanation = "Andorra ist ein Kleinstaat, der vollständig von Frankreich und Spanien umgeben ist und im Herzen der Pyrenäen liegt. Es ist eines der wenigen Länder Europas ohne reguläre Armee.",
        difficulty = 2,
        funFact = "Andorra ist offiziell ein Fürstentum mit zwei gleichrangigen Fürsten: dem Bischof von Urgell (Spanien) und dem Präsidenten Frankreichs."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt östlich der Arabischen Halbinsel?",
        answerA = "Rotes Meer",
        answerB = "Mittelmeer",
        answerC = "Arabisches Meer",
        answerD = "Golf von Aden",
        correctAnswer = 2, // C
        explanation = "Das Arabische Meer liegt im Nordwestteil des Indischen Ozeans, östlich der Arabischen Halbinsel und westlich der indischen Halbinsel.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge liegt zwischen Alaska und Russland?",
        answerA = "Beringstraße",
        answerB = "Davisstraße",
        answerC = "Hudsonstraße",
        answerD = "Cookstraße",
        correctAnswer = 0, // A
        explanation = "Die Beringstraße trennt Alaska (USA) von der russischen Tschuktschen-Halbinsel. An ihrer schmalsten Stelle ist sie nur etwa 85 km breit.",
        difficulty = 2,
        funFact = "Während der letzten Eiszeit war die Beringstraße trocken und bildete die sogenannte Beringia-Landbrücke, über die Menschen aus Asien nach Amerika einwanderten."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Guinea-Bissau?",
        answerA = "Conakry",
        answerB = "Freetown",
        answerC = "Monrovia",
        answerD = "Bissau",
        correctAnswer = 3, // D
        explanation = "Bissau ist die Hauptstadt und größte Stadt von Guinea-Bissau, einem der kleinsten und ärmsten Länder Westafrikas.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches europäische Land hat keine Landesgrenze zu einem anderen Land?",
        answerA = "Dänemark",
        answerB = "Schweden",
        answerC = "Irland",
        answerD = "Norwegen",
        correctAnswer = 2, // C
        explanation = "Irland teilt seine einzige Landgrenze mit Nordirland (Teil des Vereinigten Königreichs). Ansonsten ist es vollständig vom Atlantischen Ozean und der Irischen See umgeben.",
        difficulty = 2,
        funFact = "Island hingegen ist vollständig von Wasser umgeben und hat überhaupt keine Landgrenze – es ist damit eine echte Insel ohne Landnachbarn."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge liegt zwischen dem Schwarzen Meer und dem Kaspischen Meer?",
        answerA = "Ural",
        answerB = "Kaukasus",
        answerC = "Zagros",
        answerD = "Pontisches Gebirge",
        correctAnswer = 1, // B
        explanation = "Der Kaukasus liegt zwischen dem Schwarzen Meer im Westen und dem Kaspischen Meer im Osten und gilt oft als geografische Grenze zwischen Europa und Asien.",
        difficulty = 2,
        funFact = "Der Elbrus im Kaukasus (5.642 m) ist der höchste Berg Europas – wenn man den Kaukasus zu Europa zählt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das flächenmäßig größte Land der Karibik?",
        answerA = "Jamaika",
        answerB = "Haiti",
        answerC = "Puerto Rico",
        answerD = "Kuba",
        correctAnswer = 3, // D
        explanation = "Kuba ist mit einer Fläche von etwa 109.884 km² das größte Land der Karibik und der größte Inselstaat des Karibischen Meeres.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher See ist der größte in Nordamerika nach Fläche?",
        answerA = "Michigansee",
        answerB = "Huron-See",
        answerC = "Oberer See",
        answerD = "Eriesee",
        correctAnswer = 2, // C
        explanation = "Der Obere See (Lake Superior) ist mit einer Fläche von 82.414 km² der flächenmäßig größte Süßwassersee der Welt und der größte der Großen Seen Nordamerikas.",
        difficulty = 2,
        funFact = "Der Obere See enthält genug Wasser, um ganz Nordamerika und Südamerika 30 cm tief zu bedecken."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt die Wüste Rub al-Chali?",
        answerA = "Irak und Iran",
        answerB = "Saudi-Arabien, Jemen, Oman und VAE",
        answerC = "Nur in Saudi-Arabien",
        answerD = "Ägypten und Libyen",
        correctAnswer = 1, // B
        explanation = "Das Rub al-Chali (Leeres Viertel) erstreckt sich über den Süden Saudi-Arabiens sowie Teile des Jemen, Omans und der Vereinigten Arabischen Emirate. Es ist die größte zusammenhängende Sandwüste der Welt.",
        difficulty = 2,
        funFact = "Das Rub al-Chali ist so groß wie Frankreich und besteht zu einem Drittel aus riesigen Sanddünen, manche über 250 Meter hoch."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Tadschikistan?",
        answerA = "Samarkand",
        answerB = "Duschanbe",
        answerC = "Chorog",
        answerD = "Chudschand",
        correctAnswer = 1, // B
        explanation = "Duschanbe ist die Hauptstadt und größte Stadt Tadschikistans. Der Name bedeutet auf Tadschikisch 'Montag', da die Stadt aus einem Wochenmarkt hervorging.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet den Rücken der Apennin-Halbinsel (Italien)?",
        answerA = "Dolomiten",
        answerB = "Apenninen",
        answerC = "Kalabrische Apenninen",
        answerD = "Ligurische Alpen",
        correctAnswer = 1, // B
        explanation = "Die Apenninen (Appennino) sind ein Gebirgszug, der die Apenninhalbinsel von Nord nach Süd durchzieht. Sie bilden das 'Rückgrat' Italiens über eine Länge von etwa 1.200 km.",
        difficulty = 2,
        funFact = "In den Apenninen entspringen mehrere wichtige Flüsse Italiens, darunter der Arno (Florenz) und der Tiber (Rom)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Binnenseen der Welt?",
        answerA = "Russland",
        answerB = "Finnland",
        answerC = "Kanada",
        answerD = "Schweden",
        correctAnswer = 2, // C
        explanation = "Kanada hat mehr Seen als alle anderen Länder zusammen – schätzungsweise über 2 Millionen natürliche Seen, was etwa 60 % aller Süßwasserseen der Welt ausmacht.",
        difficulty = 2,
        funFact = "Finnland wird zwar als 'Land der tausend Seen' bezeichnet, hat aber tatsächlich über 188.000 Seen – noch immer weit weniger als Kanada."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Straße verbindet das Rote Meer mit dem Golf von Aden?",
        answerA = "Straße von Hormuz",
        answerB = "Straße von Bab el-Mandeb",
        answerC = "Straße von Malakka",
        answerD = "Straße von Tiran",
        correctAnswer = 1, // B
        explanation = "Die Straße von Bab el-Mandeb (arabisch: 'Tor der Tränen') trennt die Halbinsel Arabien von Afrika und verbindet das Rote Meer mit dem Golf von Aden.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Klimazone herrscht in Großteilen Brasiliens vor?",
        answerA = "Wüstenklima",
        answerB = "Mediterranes Klima",
        answerC = "Tropisches Regenwaldklima",
        answerD = "Subarktisches Klima",
        correctAnswer = 2, // C
        explanation = "Große Teile Brasiliens, vor allem das Amazonasbecken, gehören zum tropischen Regenwaldklima (Af) nach Köppen, mit ganzjährig hohen Temperaturen und Niederschlägen.",
        difficulty = 2,
        funFact = "Der Amazonas-Regenwald produziert etwa 20 % des gesamten Sauerstoffs der Erde – daher wird er auch als 'Lunge der Erde' bezeichnet."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Eritrea?",
        answerA = "Mogadischu",
        answerB = "Dschibuti",
        answerC = "Asmara",
        answerD = "Hargeisa",
        correctAnswer = 2, // C
        explanation = "Asmara ist die Hauptstadt Eritreas und liegt auf etwa 2.325 m Höhe auf dem Hochland Ostafrikas. Die Stadt ist bekannt für ihre außergewöhnliche modernistische Architektur aus der italienischen Kolonialzeit.",
        difficulty = 2,
        funFact = "Asmara wurde 2017 als UNESCO-Welterbe eingetragen – als eine der am besten erhaltenen modernistischen Städte der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge liegt auf der Grenze zwischen Indien und China/Nepal?",
        answerA = "Karakoram",
        answerB = "Hindukusch",
        answerC = "Himalaja",
        answerD = "Vindhyagebirge",
        correctAnswer = 2, // C
        explanation = "Der Himalaja erstreckt sich über Nepal, Bhutan, Indien, China und Pakistan und enthält alle 14 Achttausender der Welt, darunter den Mount Everest.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Zentralasiens?",
        answerA = "Kasachstan",
        answerB = "Kirgistan",
        answerC = "Turkmenistan",
        answerD = "Usbekistan",
        correctAnswer = 3, // D
        explanation = "Usbekistan ist mit etwa 35 Millionen Einwohnern das bevölkerungsreichste Land Zentralasiens, obwohl Kasachstan flächenmäßig fast siebenmal so groß ist.",
        difficulty = 2,
        funFact = "Usbekistan war das Herzstück der antiken Seidenstraße – Städte wie Samarkand und Buchara waren einst die reichsten Handelszentren der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Golf liegt zwischen der Türkei und Griechenland?",
        answerA = "Golf von Korinth",
        answerB = "Ionisches Meer",
        answerC = "Ägäisches Meer",
        answerD = "Adriatisches Meer",
        correctAnswer = 2, // C
        explanation = "Das Ägäische Meer liegt zwischen Griechenland im Westen und der Türkei im Osten. Es ist ein Randmeer des Mittelmeers und enthält Tausende griechischer Inseln.",
        difficulty = 2,
        funFact = "Das Ägäische Meer ist nach dem mythischen König Aigeus benannt, der sich ins Meer gestürzt haben soll, als er dachte, sein Sohn Theseus sei im Kampf gegen den Minotaurus gefallen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land ist das einzige in Südamerika, das kein Spanisch oder Portugiesisch als Amtssprache hat?",
        answerA = "Guyana",
        answerB = "Suriname",
        answerC = "Französisch-Guayana",
        answerD = "Alle drei",
        correctAnswer = 3, // D
        explanation = "Guyana (Englisch), Suriname (Niederländisch) und Französisch-Guayana (Französisch, Überseedepartement Frankreichs) haben alle nicht Spanisch oder Portugiesisch als Amtssprache.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wo liegt das Totes Meer, der tiefstgelegene Punkt der Erdoberfläche?",
        answerA = "Zwischen Ägypten und der Sinai-Halbinsel",
        answerB = "Zwischen Israel, Palästina und Jordanien",
        answerC = "Im Norden Saudi-Arabiens",
        answerD = "Im Süden des Libanons",
        correctAnswer = 1, // B
        explanation = "Das Tote Meer liegt zwischen Israel und dem Westjordanland im Westen und Jordanien im Osten. Seine Oberfläche liegt etwa 430 Meter unter dem Meeresspiegel.",
        difficulty = 2,
        funFact = "Das Tote Meer ist so salzig (ca. 28-35%), dass kein Lebewesen darin überleben kann – daher der Name. Menschen können darin mühelos schwimmen, ohne zu sinken."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher ist der längste Fluss Australiens?",
        answerA = "Darling River",
        answerB = "Murrumbidgee River",
        answerC = "Murray River",
        answerD = "Cooper Creek",
        correctAnswer = 2, // C
        explanation = "Der Murray (Murraay auf Ngarrindjeri) ist mit etwa 2.508 km der längste Fluss Australiens. Zusammen mit dem Darling bildet er das Murray-Darling-Flusssystem, das wichtigste Australiens.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kiribati?",
        answerA = "Suva",
        answerB = "Funafuti",
        answerC = "South Tarawa",
        answerD = "Apia",
        correctAnswer = 2, // C
        explanation = "South Tarawa (offiziell: Tarawa) ist die Hauptstadt des Inselstaates Kiribati im Zentralpazifik. Kiribati liegt auf dem Äquator und erstreckt sich über mehrere Zeitzonen.",
        difficulty = 2,
        funFact = "Kiribati ist das erste Land, das offiziell den Klimawandel als existenzielle Bedrohung ausgerufen hat – viele seiner Inseln drohen durch steigende Meeresspiegel zu versinken."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die Grenze zwischen Russland und Georgien?",
        answerA = "Ural",
        answerB = "Karpaten",
        answerC = "Großer Kaukasus",
        answerD = "Pontisches Gebirge",
        correctAnswer = 2, // C
        explanation = "Der Große Kaukasus (Hauptkamm) bildet die natürliche Grenze zwischen Russland im Norden und Georgien sowie Aserbaidschan im Süden.",
        difficulty = 2,
        funFact = "Der Große Kaukasus enthält mit dem Elbrus (5.642 m) den höchsten Gipfel Europas – sofern man den Kaukasus als europäisch-asiatische Grenze betrachtet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land liegt vollständig innerhalb Südafrikas?",
        answerA = "Swasiland (Eswatini)",
        answerB = "Swasiland und Lesotho",
        answerC = "Lesotho",
        answerD = "Botswana",
        correctAnswer = 2, // C
        explanation = "Lesotho ist vollständig von Südafrika umschlossen (ein sogenannter Enklavenstaat). Es liegt im Drakensberggebirge auf einer durchschnittlichen Höhe von 2.500 m.",
        difficulty = 2,
        funFact = "Lesotho ist neben dem Vatikan und San Marino eines von nur drei Ländern der Welt, die vollständig von einem einzigen anderen Land umschlossen sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher ist der größte Fluss Afrikas nach Abflussmenge?",
        answerA = "Nil",
        answerB = "Niger",
        answerC = "Sambesi",
        answerD = "Kongo",
        correctAnswer = 3, // D
        explanation = "Der Kongo ist nach dem Amazonas der wasserreichste Fluss der Welt. Er hat eine durchschnittliche Abflussmenge von etwa 41.000 m³/s – weit mehr als der Nil.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Osttimor (Timor-Leste)?",
        answerA = "Dili",
        answerB = "Baucau",
        answerC = "Suai",
        answerD = "Maliana",
        correctAnswer = 0, // A
        explanation = "Dili ist die Hauptstadt und größte Stadt von Timor-Leste (Osttimor). Das Land erlangte erst 2002 die Unabhängigkeit und ist eines der jüngsten Länder der Welt.",
        difficulty = 2,
        funFact = "Timor-Leste ist eines der wenigen mehrheitlich christlichen Länder Asiens – ein Erbe der portugiesischen Kolonialherrschaft."
    ),

    // ── MEDIUM (difficulty = 2) ── questions 101–150 ─────────────────────────

    Question(
        categoryId = 1,
        questionText = "Welche zwei Länder sind doppelt eingeschlossen (von Binnenländern umgeben)?",
        answerA = "Bolivien und Paraguay",
        answerB = "Usbekistan und Liechtenstein",
        answerC = "Nepal und Bhutan",
        answerD = "Luxemburg und Österreich",
        correctAnswer = 1, // B
        explanation = "Usbekistan und Liechtenstein sind die einzigen zwei doppelt eingeschlossenen Länder der Welt (doubly landlocked): Sie sind von Binnenländern umgeben, sodass man zwei Staatsgrenzen überqueren muss, bevor man das Meer erreicht.",
        difficulty = 2,
        funFact = "Usbekistan grenzt an fünf Länder – alle sind selbst Binnenländer (Kasachstan, Kirgistan, Tadschikistan, Afghanistan, Turkmenistan)."
    ),

    Question(
        categoryId = 1,
        questionText = "Woher stammt der Name 'Kasachstan'?",
        answerA = "Vom persischen Wort für 'Steppenland'",
        answerB = "Vom Türkischen für 'freier Mensch' (Kazakh) plus 'Land' (stan)",
        answerC = "Vom mongolischen Wort für 'weites Land'",
        answerD = "Nach dem Fluss Kasach benannt",
        correctAnswer = 1, // B
        explanation = "Der Name Kasachstan leitet sich vom Türkisch-Mongolischen 'Kazakh' ab, was in etwa 'freier Mensch' oder 'Nomade' bedeutet, kombiniert mit dem persischen Suffix '-stan' (Land/Heimat).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "An welcher Plattengrenze liegen Japan und Indonesien?",
        answerA = "An einer Divergenzzone (auseinanderdriftende Platten)",
        answerB = "An einer Transformstörung (seitlich verschiebende Platten)",
        answerC = "An einer Subduktionszone (eintauchende Platten)",
        answerD = "Im Zentrum einer tektonischen Platte",
        correctAnswer = 2, // C
        explanation = "Japan und Indonesien liegen an aktiven Subduktionszonen, wo ozeanische Platten unter kontinentale Platten tauchen. Dies erklärt den 'Pazifischen Feuerring' mit seinen zahlreichen Vulkanen und Erdbeben.",
        difficulty = 2,
        funFact = "Indonesien hat mehr als 130 aktive Vulkane – mehr als jedes andere Land der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat das größte Mekong-Delta?",
        answerA = "Thailand",
        answerB = "Kambodscha",
        answerC = "Vietnam",
        answerD = "Laos",
        correctAnswer = 2, // C
        explanation = "Das Mekong-Delta liegt in Südvietnam und erstreckt sich über rund 40.500 km². Es ist einer der fruchtbarsten Reisanbaugebiete Asiens und wird als 'Reisschüssel Vietnams' bezeichnet.",
        difficulty = 2,
        funFact = "Das Mekong-Delta beherbergt über 17 Millionen Menschen und produziert die Hälfte des gesamten vietnamesischen Reises."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches europäische Land hat die längsten und tiefsten Fjorde?",
        answerA = "Island",
        answerB = "Schottland",
        answerC = "Schweden",
        answerD = "Norwegen",
        correctAnswer = 3, // D
        explanation = "Norwegen ist weltweit für seine tiefen Fjorde bekannt. Der Sognefjord ist mit 204 km der längste und mit 1.308 m der tiefste Fjord der Welt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land trägt seinen Namen nach dem Silber (argentum)?",
        answerA = "Chile",
        answerB = "Argentinien",
        answerC = "Bolivien",
        answerD = "Peru",
        correctAnswer = 1, // B
        explanation = "Der Name Argentinien leitet sich vom lateinischen 'argentum' (Silber) ab. Spanische Conquistadoren hofften, dort riesige Silbervorkommen zu finden – tatsächlich war das Rio de la Plata-Gebiet aber nicht reich an Silber.",
        difficulty = 2,
        funFact = "Trotz seines silbrigen Namens war Argentiniens Reichtum historisch hauptsächlich auf Rindervieh und Getreide zurückzuführen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo befindet sich der größte Permafrost-Gürtel der Welt?",
        answerA = "In der Antarktis",
        answerB = "In Kanada und Grönland",
        answerC = "In Sibirien (Russland)",
        answerD = "In Skandinavien",
        correctAnswer = 2, // C
        explanation = "Sibirien beherbergt den größten zusammenhängenden Permafrostbereich der Erde. Etwa 65 % der russischen Landfläche ist von Permafrost bedeckt, was enormen Auswirkungen auf den Klimawandel hat.",
        difficulty = 2,
        funFact = "Im Permafrost Sibiriens wurden Mammutreste gefunden, die über 40.000 Jahre alt sind und noch gut erhalten sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land wurde nach dem Entdecker Amerigo Vespucci benannt?",
        answerA = "Nur die USA",
        answerB = "Venezuela",
        answerC = "Nord- und Südamerika",
        answerD = "Brasilien",
        correctAnswer = 2, // C
        explanation = "Beide amerikanische Kontinente wurden nach dem italienischen Entdecker und Kartografen Amerigo Vespucci benannt, der als Erster erkannte, dass es sich um einen eigenständigen Kontinent und nicht um Indien handelte.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches afrikanische Land war nie eine Kolonie einer europäischen Macht?",
        answerA = "Äthiopien",
        answerB = "Liberia und Äthiopien",
        answerC = "Somalia",
        answerD = "Sudan",
        correctAnswer = 1, // B
        explanation = "Äthiopien und Liberia gelten als die einzigen afrikanischen Länder, die nie dauerhaft unter europäischer Kolonialherrschaft standen. Äthiopien besiegte Italien 1896 in der Ersten Italo-Äthiopischen Krieg.",
        difficulty = 2,
        funFact = "Liberia wurde 1847 von befreiten amerikanischen Sklaven gegründet und ist einer der ältesten Republiken Afrikas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Monsun versorgt den indischen Subkontinent hauptsächlich mit Regen?",
        answerA = "Der Nordost-Monsun (Winter)",
        answerB = "Der Südwest-Monsun (Sommer)",
        answerC = "Der Äquatorialmonsun",
        answerD = "Der Indische Ozean-Monsun (Herbst)",
        correctAnswer = 1, // B
        explanation = "Der Südwest-Monsun (Juni–September) bringt feuchte Luft vom Indischen Ozean über den Subkontinent und ist verantwortlich für 70–90 % der jährlichen Niederschläge in Indien.",
        difficulty = 2,
        funFact = "In Mawsynram (Indien) fällt mit über 11.000 mm pro Jahr der meiste Regen der Welt – dank des Südwest-Monsuns."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresregion besitzt den breitesten Kontinentalschelf der Welt?",
        answerA = "Das Südchinesische Meer",
        answerB = "Das Sibirische Schelf im Arktischen Ozean",
        answerC = "Der Nordatlantik vor Nordamerika",
        answerD = "Das Arabische Meer",
        correctAnswer = 1, // B
        explanation = "Das Sibirische Schelf (Laptewsee, Ostsibirische See, Karasee) im Arktischen Ozean ist mit einer Breite von bis zu 1.500 km der breiteste Kontinentalschelf der Welt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt das Ganges-Delta (Sundarban)?",
        answerA = "Nur in Indien",
        answerB = "In Bangladesh und Indien",
        answerC = "In Pakistan und Indien",
        answerD = "Nur in Bangladesh",
        correctAnswer = 1, // B
        explanation = "Das Ganges-Brahmaputra-Delta (Sundarban) erstreckt sich über Bangladesh und den indischen Bundesstaat Westbengalen. Es ist das größte Mündungsdelta der Welt.",
        difficulty = 2,
        funFact = "Das Sundarban ist das größte Mangrovenwaldgebiet der Welt und Heimat des Bengalischen Tigers."
    ),

    Question(
        categoryId = 1,
        questionText = "Nach wem wurde 'Neuseeland' benannt?",
        answerA = "Nach einem britischen Admiral namens Zealand",
        answerB = "Nach der niederländischen Provinz Zeeland",
        answerC = "Nach dem deutschen Wort für 'neues Seeland'",
        answerD = "Es war ein Maori-Begriff, den die Europäer übernahmen",
        correctAnswer = 1, // B
        explanation = "Neuseeland wurde vom niederländischen Entdecker Abel Tasman nach der niederländischen Provinz Zeeland benannt. Der lateinisierte Name 'Nova Zeelandia' wurde später zu 'New Zealand'.",
        difficulty = 2,
        funFact = "Die Maori nennen Neuseeland 'Aotearoa', was 'Land der langen weißen Wolke' bedeutet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Plattengrenze verläuft mitten durch Island?",
        answerA = "Die nordamerikanisch-eurasische Divergenzgrenze (Mittelatlantischer Rücken)",
        answerB = "Die pazifisch-nordamerikanische Subduktionszone",
        answerC = "Die afrikanisch-eurasische Kollisionszone",
        answerD = "Die arabisch-eurasische Transformstörung",
        correctAnswer = 0, // A
        explanation = "Der Mittelatlantische Rücken verläuft mitten durch Island und trennt die nordamerikanische von der eurasischen Platte. Island wächst dadurch jährlich um etwa 2 cm in der Breite.",
        difficulty = 2,
        funFact = "In Island kann man buchstäblich zwischen zwei Kontinentalplatten stehen und beide berühren – im Þingvellir-Nationalpark."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land wurde nach einem portugiesischen Wort für 'tief' oder 'tiefes Land' benannt?",
        answerA = "Tansania",
        answerB = "Angola",
        answerC = "Mosambik",
        answerD = "Nigeria",
        correctAnswer = 1, // B
        explanation = "Angola leitet seinen Namen vom Titel 'Ngola' der Könige des Ndongo-Reiches ab, den die Portugiesen aufgriffen. Es ist kein direktes portugiesisches Wort, aber die Portugiesen prägten den Namen im Kolonialzeitalter.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Fjordregion liegt in Neuseeland?",
        answerA = "Milford Sound in Fiordland",
        answerB = "Queen Charlotte Sound",
        answerC = "Marlborough Sounds",
        answerD = "Hawke's Bay",
        correctAnswer = 0, // A
        explanation = "Milford Sound im Fiordland-Nationalpark auf der Südinsel Neuseelands ist einer der bekanntesten Fjorde weltweit. Fiordland wurde durch Gletschereis während der Eiszeiten geformt.",
        difficulty = 2,
        funFact = "Milford Sound wird manchmal als 'achtes Weltwunder' bezeichnet – Rudyard Kipling beschrieb es als 'das achte Weltwunder'."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land führt seinen Namen auf das arabische Wort für 'zwei Meere' zurück?",
        answerA = "Kuwait",
        answerB = "Bahrain",
        answerC = "Katar",
        answerD = "Oman",
        correctAnswer = 1, // B
        explanation = "Der Name Bahrain (arabisch: البحرين, al-Baḥrayn) bedeutet 'zwei Meere' – wahrscheinlich eine Anspielung auf die Süßwasserquellen unter dem Meeresgrund und das Salzwasser des Persischen Golfs.",
        difficulty = 2,
        funFact = "Bahrain ist der kleinste Staat des arabischen Raums und besteht aus über 50 Inseln."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches südamerikanische Land ist vollständig von anderen Ländern eingeschlossen (Binnenland)?",
        answerA = "Nur Bolivien",
        answerB = "Bolivien und Paraguay",
        answerC = "Nur Paraguay",
        answerD = "Bolivien, Paraguay und Uruguay",
        correctAnswer = 1, // B
        explanation = "Bolivien und Paraguay sind die einzigen Binnenländer Südamerikas. Bolivien verlor seinen Zugang zum Pazifik im Salpeterkrieg (1879–1884) an Chile.",
        difficulty = 2,
        funFact = "Bolivien unterhält trotzdem eine Marine – die bolivianische Kriegsmarine patrouilliert den Titicacasee und wird symbolisch als Zeichen des Anspruchs auf einen Meeresausgang gehalten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Delta entsteht am Nil in Nordägypten?",
        answerA = "Das Okavango-Delta",
        answerB = "Das Suez-Delta",
        answerC = "Das Nildelta",
        answerD = "Das Alexandria-Delta",
        correctAnswer = 2, // C
        explanation = "Das Nildelta liegt im Norden Ägyptens, wo der Nil in das Mittelmeer mündet. Es ist eines der ältesten und bevölkerungsreichsten Deltas der Welt und hat die Form des griechischen Buchstabens Delta (Δ).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land war die erste europäische Kolonialmacht auf dem afrikanischen Kontinent?",
        answerA = "Spanien",
        answerB = "Portugal",
        answerC = "England",
        answerD = "Niederlande",
        correctAnswer = 1, // B
        explanation = "Portugal war die erste europäische Kolonialmacht, die systematisch die afrikanische Küste erkundete und Handelsstützpunkte errichtete – beginnend im 15. Jahrhundert unter Heinrich dem Seefahrer.",
        difficulty = 2,
        funFact = "Portugal hielt mit Mosambik und Angola die letzten großen Kolonien in Afrika bis 1975."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher der folgenden Seen hat keinen oberirdischen Abfluss (endorheischer See)?",
        answerA = "Baikalsee",
        answerB = "Kaspisches Meer",
        answerC = "Viktoriasee",
        answerD = "Michigansee",
        correctAnswer = 1, // B
        explanation = "Das Kaspische Meer ist endorheisch – es hat keinen natürlichen Abfluss zum Weltmeer. Es ist technisch gesehen der größte See der Welt und verliert Wasser nur durch Verdunstung.",
        difficulty = 2,
        funFact = "Der Aralsee war ebenfalls endorheisch, ist aber durch massive Wasserentnahmen für Bewässerung auf einen Bruchteil seiner ursprünglichen Größe geschrumpft."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Monsun-System ist verantwortlich für die Regenzeit in Westafrika?",
        answerA = "Der West-Afrikanische Monsun",
        answerB = "Der Indische Ozean-Monsun",
        answerC = "Der Atlantische Passatwind",
        answerD = "Der ITCZ-Tropenzyklus",
        correctAnswer = 0, // A
        explanation = "Der West-Afrikanische Monsun (WAM) bringt feuchte Luft vom Atlantik über die Sahelzone und das Kongobecken und ist für die Regenzeit in Westafrika (Juli–September) verantwortlich.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Warum heißt der Pazifische Ozean 'Pazifik'?",
        answerA = "Nach dem portugiesischen Wort für 'groß'",
        answerB = "Nach dem lateinischen 'pacificus' (friedlich), weil Magellan ruhiges Wetter antraf",
        answerC = "Nach dem griechischen Wort für 'blaues Wasser'",
        answerD = "Nach dem Stamm der Pazifik-Indianer",
        correctAnswer = 1, // B
        explanation = "Ferdinand Magellan nannte den Ozean 'Mar Pacífico' (friedliches Meer), weil er nach der stürmischen Passage durch die Magellanstraße auf ruhiges Wetter traf.",
        difficulty = 2,
        funFact = "Der Pazifik ist größer als alle Kontinente zusammen – er bedeckt etwa 165 Millionen km²."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt die Lena, einer der großen sibirischen Ströme?",
        answerA = "Kasachstan",
        answerB = "Mongolei",
        answerC = "China",
        answerD = "Russland",
        correctAnswer = 3, // D
        explanation = "Die Lena fließt vollständig durch Russland (Sibirien) und ist mit etwa 4.400 km einer der längsten Flüsse der Welt. Sie mündet in das Laptewmeer im Arktischen Ozean.",
        difficulty = 2,
        funFact = "Das Lena-Delta ist eines der größten Flussdeltas der Welt und ein wichtiger Brutplatz für Zugvögel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Inselgruppe im Atlantik gehört zu Spanien, obwohl sie vor der afrikanischen Küste liegt?",
        answerA = "Azoren",
        answerB = "Madeira",
        answerC = "Kapverden",
        answerD = "Kanarische Inseln",
        correctAnswer = 3, // D
        explanation = "Die Kanarischen Inseln sind eine autonome Gemeinschaft Spaniens, liegen aber nur etwa 100 km vor der Küste Marokkos. Ihr Name geht möglicherweise auf das lateinische 'Canariae Insulae' (Inseln der Hunde) zurück.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Binnenland in Afrika liegt vollständig innerhalb des Sahara-Gürtels?",
        answerA = "Mali",
        answerB = "Niger",
        answerC = "Tschad",
        answerD = "Burkina Faso",
        correctAnswer = 1, // B
        explanation = "Niger ist eines der am stärksten von der Sahara geprägten Binnenländer Afrikas. Etwa 80 % des Landes sind Wüste oder Halbwüste (Sahara und Sahel), was Niger zu einem der ärmsten Länder der Welt macht.",
        difficulty = 2,
        funFact = "Niger ist namengebend für den Niger-Fluss, obwohl der Fluss nur einen kleinen Teil des Landes berührt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche tektonische Platte trägt den indischen Subkontinent?",
        answerA = "Australische Platte",
        answerB = "Eurasische Platte",
        answerC = "Indische Platte",
        answerD = "Arabische Platte",
        correctAnswer = 2, // C
        explanation = "Der indische Subkontinent liegt auf der Indischen Platte (manchmal auch Indo-Australische Platte genannt). Diese kollidiert mit der Eurasischen Platte und hat so den Himalaja aufgefaltet.",
        difficulty = 2,
        funFact = "Indien bewegt sich noch heute mit etwa 5 cm pro Jahr nach Norden und lässt den Himalaja weiter wachsen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo liegt das Okavango-Delta, eines der einzigartigsten Deltas der Welt?",
        answerA = "In Namibia",
        answerB = "In Zimbabwe",
        answerC = "In Botswana",
        answerD = "In Sambia",
        correctAnswer = 2, // C
        explanation = "Das Okavango-Delta liegt in Botswana und ist einzigartig: Der Okavango-Fluss mündet nicht ins Meer, sondern in die Kalahari-Wüste und bildet ein riesiges Binnendelta, das bis zu 15.000 km² groß werden kann.",
        difficulty = 2,
        funFact = "Das Okavango-Delta ist ein UNESCO-Welterbe und beherbergt eine der letzten großen Wildtier-Konzentrationen Afrikas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige zentralasiatische Binnenland, das keinen Zugang zum Kaspischen Meer hat?",
        answerA = "Usbekistan",
        answerB = "Kirgistan",
        answerC = "Tadschikistan",
        answerD = "Alle drei genannten",
        correctAnswer = 3, // D
        explanation = "Kirgistan, Tadschikistan und Usbekistan haben keinen Zugang zum Kaspischen Meer. Das Kaspische Meer grenzt nur an Kasachstan und Turkmenistan unter den zentralasiatischen Staaten.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt das größte Süßwasserdelta der Welt?",
        answerA = "Mississippi-Delta",
        answerB = "Amazonas-Mündung",
        answerC = "Ganges-Brahmaputra-Delta (Sundarban)",
        answerD = "Ob-Delta",
        correctAnswer = 2, // C
        explanation = "Das Ganges-Brahmaputra-Delta im Bereich des Sundarban in Bangladesh und Indien ist das größte Mündungsdelta der Welt mit einer Fläche von etwa 105.000 km².",
        difficulty = 2,
        funFact = "Das Sundarban ist gleichzeitig der größte Mangrovenwald der Welt und ein Lebensraum für den gefährdeten Bengalischen Tiger."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land nennt sich selbst 'Magyarország' und woher kommt dieser Name?",
        answerA = "Rumänien – von 'magna Roma' (Groß-Rom)",
        answerB = "Ungarn – von 'Magyar' (Name des Volkes) plus 'ország' (Land)",
        answerC = "Österreich – von 'Ostarrichi' (östliches Reich)",
        answerD = "Slowakei – vom slawischen 'Slav-Land'",
        correctAnswer = 1, // B
        explanation = "Ungarn nennt sich selbst 'Magyarország'. 'Magyar' ist der Eigenname des ungarischen Volkes, und 'ország' bedeutet 'Land' oder 'Reich'. Die Herkunft von 'Magyar' ist umstritten, könnte aber 'Sprecher der Mansi-Sprache' bedeuten.",
        difficulty = 2,
        funFact = "Der Name 'Hungary' in westeuropäischen Sprachen geht auf die Hunnen zurück, obwohl die Ungarn (Magyar) keine direkten Nachfahren der Hunnen sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches afrikanische Land war eine britische Kolonie und ist nach einer Einheimischen Bezeichnung für den Niger-Fluss benannt?",
        answerA = "Ghana",
        answerB = "Nigeria",
        answerC = "Kenia",
        answerD = "Tansania",
        correctAnswer = 1, // B
        explanation = "Nigeria ist nach dem Niger-Fluss benannt, der das Land durchfließt. Der Name wurde 1897 von der britischen Journalistin Flora Shaw geprägt und 1914 offiziell von der britischen Kolonialverwaltung übernommen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Region in Kanada hat den größten Permafrostanteil?",
        answerA = "British Columbia",
        answerB = "Ontario",
        answerC = "Yukon und die Nordwest-Territorien",
        answerD = "Quebec",
        correctAnswer = 2, // C
        explanation = "Yukon und die Nordwest-Territorien im Norden Kanadas haben den größten Permafrostanteil. Etwa 50 % der kanadischen Landfläche liegt auf Permafrostboden.",
        difficulty = 2,
        funFact = "Der Permafrost in Kanada enthält gefrorene organische Substanz, die beim Auftauen Treibhausgase freisetzt und den Klimawandel verstärkt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land trägt seinen Namen von einem einheimischen Stamm, der 'tapfere Krieger' bedeutete?",
        answerA = "Mosambik",
        answerB = "Simbabwe",
        answerC = "Kenia",
        answerD = "Tansania",
        correctAnswer = 2, // C
        explanation = "Kenia ist nach dem Berg Kenia (Kirinyaga auf Kikuyu, Mount Kenya) benannt. Der Name der Kikuyu bedeutet in der Sprache der lokalen Bevölkerung so viel wie 'Gipfel des Berges'.",
        difficulty = 2,
        funFact = "Der Berg Kenia ist der zweithöchste Berg Afrikas und ein erloschener Vulkan."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Konvergenzzone ist Hauptursache des asiatischen Sommermonsuns?",
        answerA = "Der sibirische Hochdruckgürtel",
        answerB = "Die Innertropische Konvergenzzone (ITCZ)",
        answerC = "Das arktische Frontalsystem",
        answerD = "Der subtropische Jetstream",
        correctAnswer = 1, // B
        explanation = "Die Innertropische Konvergenzzone (ITCZ) ist ein zentraler Antrieb des asiatischen Monsuns. Sie verlagert sich im Sommer nach Norden, zieht Feuchtigkeit vom Indischen Ozean an und sorgt für intensive Niederschläge.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches skandinavische Land hat keine Fjorde?",
        answerA = "Dänemark",
        answerB = "Schweden",
        answerC = "Finnland",
        answerD = "Sowohl Dänemark als auch Finnland",
        correctAnswer = 3, // D
        explanation = "Sowohl Dänemark als auch Finnland wurden nicht durch dieselben tiefen glazialen Einschnitte wie Norwegen geprägt. Fjorde entstanden durch Gletscher, die tiefe Täler gruben – das war in Dänemark und Finnland weniger ausgeprägt.",
        difficulty = 2,
        funFact = "Schweden hat einige fjordähnliche Einschnitte (Fjärdar), aber echte Fjorde sind ein Merkmal der norwegischen Westküste."
    ),

    Question(
        categoryId = 1,
        questionText = "Woher kommt der Name 'Brasilien'?",
        answerA = "Von dem portugiesischen König Dom Brasil",
        answerB = "Vom Pau-Brasil-Baum (Brasilholz), einem roten Farbholz",
        answerC = "Vom indigenen Wort 'Brasiga' für 'großes Land'",
        answerD = "Von der Stadt São Brás",
        correctAnswer = 1, // B
        explanation = "Brasilien leitet seinen Namen vom Pau-Brasil (Caesalpinia echinata) ab, einem Rotholzbaum, der für seinen roten Farbstoff wertvoll war. Der Handel mit diesem Holz war das erste wichtige Exportgeschäft der portugiesischen Kolonie.",
        difficulty = 2,
        funFact = "Der Pau-Brasil-Baum steht heute auf der Roten Liste gefährdeter Arten – rücksichtslose Abholzung im 16.–17. Jahrhundert hat ihn fast ausgerottet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land besitzt den größten Teil der Sonoran-Wüste?",
        answerA = "USA",
        answerB = "Mexiko",
        answerC = "USA und Mexiko zu gleichen Teilen",
        answerD = "Mexiko hat den größeren Teil",
        correctAnswer = 3, // D
        explanation = "Die Sonoran-Wüste liegt zum größten Teil in Mexiko (Sonora und Baja California) und zu einem kleineren Teil in den US-Bundesstaaten Arizona und Kalifornien.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist das Besondere an den Fjorden der Chiloe-Insel-Region in Chile?",
        answerA = "Sie sind die tiefsten Fjorde der Südhalbkugel",
        answerB = "Sie sind die einzigen Fjorde Südamerikas",
        answerC = "Sie befinden sich im tropischen Klimabereich",
        answerD = "Sie entstanden durch Vulkanismus statt durch Gletscher",
        correctAnswer = 1, // B
        explanation = "Die chilenischen Fjorde in Patagonien und der Chiloe-Region sind die einzigen Fjorde Südamerikas. Sie entstanden wie norwegische Fjorde durch glaziale Erosion und erstrecken sich über tausende Kilometer.",
        difficulty = 2,
        funFact = "Die chilenischen Fjorde sind eines der wasserreichsten Gebiete der Erde und gehören zu den letzten fast unberührten Wildnisregionen weltweit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat den breitesten Kontinentalschelf vor seiner Küste?",
        answerA = "Europa (Nordsee-Schelf)",
        answerB = "Nordamerika (Atlantikküste)",
        answerC = "Asien (Sibirischer Arktisschelf)",
        answerD = "Australien (Nordküste)",
        correctAnswer = 2, // C
        explanation = "Der Sibirische Arktisschelf (Eurasischer Arktisschelf) ist der ausgedehnteste Kontinentalschelf der Welt und erstreckt sich über eine Breite von bis zu 1.500 km unter dem Arktischen Ozean.",
        difficulty = 2,
        funFact = "Unter dem Arktischen Schelf werden riesige Erdöl- und Erdgasvorkommen vermutet, was zu Territorialstreitigkeiten zwischen Russland, Kanada, Norwegen und Dänemark führt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige vollständig von Wasser umgebene Binnenland der Welt?",
        answerA = "Es gibt kein solches Land",
        answerB = "Liechtenstein (von Österreich und Schweiz umgeben, beide Binnenländer)",
        answerC = "Luxemburg",
        answerD = "San Marino",
        correctAnswer = 1, // B
        explanation = "Liechtenstein und Usbekistan sind die einzigen doppelt eingeschlossenen Länder – sie sind von Ländern umgeben, die selbst keinen Meereszugang haben. Liechtenstein grenzt an die Schweiz und Österreich, beide Binnenländer.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat seinen Namen vom arabischen Begriff für 'südlich des Sahara-Sandes'?",
        answerA = "Libyen",
        answerB = "Sudan",
        answerC = "Algerien",
        answerD = "Mauretanien",
        correctAnswer = 1, // B
        explanation = "Der Name Sudan leitet sich vom arabischen 'Bilad as-Sudan' ab, was 'Land der Schwarzen' oder 'Land südlich der Sahara' bedeutet. Es war der historische arabische Begriff für Subsahara-Afrika.",
        difficulty = 2,
        funFact = "Sudan hatte bis 2011 die größte Landfläche Afrikas, bevor der Südsudan unabhängig wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo liegt der größte Kontinentalschelf Europas?",
        answerA = "Vor der Küste Irlands im Atlantik",
        answerB = "Die Nordsee (Europäischer Nordseeschelf)",
        answerC = "Das Mittelmeer vor Spanien",
        answerD = "Vor der norwegischen Küste",
        correctAnswer = 1, // B
        explanation = "Die Nordsee liegt vollständig auf dem Europäischen Nordseeschelf, einem der flachsten und breitesten Kontinentalsockel der Welt. Die durchschnittliche Tiefe beträgt nur etwa 95 Meter.",
        difficulty = 2,
        funFact = "Die Nordsee war während der letzten Eiszeit vollständig trocken – Mensch und Tier konnten von England nach Dänemark zu Fuß gehen (Doggerland)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Monsun beeinflusst das Klima der Ostküste Australiens?",
        answerA = "Der australische Nordwest-Monsun (Sommer)",
        answerB = "Der indo-pazifische Sommermonsun",
        answerC = "Der El-Niño-Monsun",
        answerD = "Der südostasiatische Monsun",
        correctAnswer = 0, // A
        explanation = "Der australische Nordwest-Monsun bringt im australischen Sommer (November–April) feuchte Luft aus dem Indischen Ozean in den Norden Australiens. Die Ostküste wird stärker von Passatwinden und dem La-Niña/El-Niño-System beeinflusst.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land trägt seinen Namen nach dem lateinischen Wort für 'reich an Silber' und hat eine Küste?",
        answerA = "Argentinien",
        answerB = "Costa Rica",
        answerC = "Venezuela",
        answerD = "Peru",
        correctAnswer = 0, // A
        explanation = "Argentinien (von lat. 'argentum' = Silber) hat eine lange Atlantikküste von etwa 4.989 km. Der Name bezog sich auf erhoffte Silbervorkommen, obwohl das Land tatsächlich nicht reich an Silber war.",
        difficulty = 2,
        funFact = "Argentiniens Silber-Name geht auf die Mündung des Río de la Plata zurück – 'La Plata' bedeutet ebenfalls 'Silber' auf Spanisch."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Kolonialmacht gründete die meisten heutigen afrikanischen Hauptstädte?",
        answerA = "England (Großbritannien)",
        answerB = "Frankreich",
        answerC = "Portugal",
        answerD = "Deutschland",
        correctAnswer = 1, // B
        explanation = "Frankreich hatte das größte Kolonialreich in Afrika (Französisch-Westafrika, Äquatorialafrika, Maghreb) und gründete viele der heutigen afrikanischen Hauptstädte oder baute sie aus, darunter Dakar, Abidjan, Bamako und Brazzaville.",
        difficulty = 2,
        funFact = "Frankreich war die Kolonialmacht über mehr als 20 heutige afrikanische Länder – mehr als jede andere europäische Nation."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge hat die aktivste Subduktionszone mit häufigen Mega-Erdbeben?",
        answerA = "Die Kaskaden (Nordamerika)",
        answerB = "Die Anden (Südamerika)",
        answerC = "Der Himalaja (Asien)",
        answerD = "Der Ural (Russland)",
        correctAnswer = 1, // B
        explanation = "Die Anden entstanden und entstehen durch die Subduktion der Nazca-Platte unter die Südamerikanische Platte. Diese Zone ist eine der seismisch aktivsten der Welt mit regelmäßigen Mega-Erdbeben der Stärke 8+.",
        difficulty = 2,
        funFact = "Das stärkste je gemessene Erdbeben (Magnitude 9,5) ereignete sich 1960 in Chile – genau an der andinen Subduktionszone."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Binnenland in Asien, das vollständig von Russland und China umgeben ist?",
        answerA = "Kirgistan",
        answerB = "Mongolei",
        answerC = "Tadschikistan",
        answerD = "Kasachstan",
        correctAnswer = 1, // B
        explanation = "Die Mongolei ist vollständig von Russland im Norden und China im Süden, Osten und Westen umschlossen. Sie ist das größte vollständig von Binnenländern umgebene Land der Welt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Delta-System ist am stärksten von Monsun-Überschwemmungen bedroht?",
        answerA = "Das Mississippi-Delta",
        answerB = "Das Nil-Delta",
        answerC = "Das Ganges-Brahmaputra-Delta in Bangladesh",
        answerD = "Das Niger-Delta",
        correctAnswer = 2, // C
        explanation = "Das Ganges-Brahmaputra-Delta in Bangladesh ist jedes Jahr von Monsun-Überschwemmungen betroffen. Die kombinierten Fluten des Ganges, Brahmaputra und Meghna überfluten regelmäßig 20–30 % des Landes.",
        difficulty = 2,
        funFact = "Bangladesh ist eines der am dichtesten besiedelten Länder der Welt und gleichzeitig eines der am stärksten vom Klimawandel und steigendem Meeresspiegel bedrohten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land im Nahen Osten hat das größte Binnengewässer – den Urmia-See?",
        answerA = "Irak",
        answerB = "Iran",
        answerC = "Türkei",
        answerD = "Syrien",
        correctAnswer = 1, // B
        explanation = "Der Urmia-See im Nordwesten des Iran ist das größte Gewässer des Landes und einer der größten Salzseen der Welt. Allerdings hat er seit den 1970er Jahren durch Wasserentnahme über 90 % seines Volumens verloren.",
        difficulty = 2,
        funFact = "Der Urmia-See war einst so groß wie das Bundesland Bayern – heute ist er auf einen kleinen Bruchteil seiner einstigen Größe geschrumpft."
    ),

    Question(
        categoryId = 1,
        questionText = "Woher stammt der Name 'Mexiko'?",
        answerA = "Vom spanischen König Mexicus",
        answerB = "Vom Nahuatl 'Mēxihco', der Hauptstadt der Azteken",
        answerC = "Vom Maya-Wort für 'großes Land'",
        answerD = "Von einem spanischen Geographen namens Mexo",
        correctAnswer = 1, // B
        explanation = "Der Name Mexiko leitet sich vom Nahuatl 'Mēxihco' ab, dem Namen der aztekischen Hauptstadt Tenochtitlan (heute Mexico-Stadt). Die genaue Bedeutung ist umstritten, möglicherweise 'Ort des Mexitli' (aztekischer Kriegsgott).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Fjorde nach Norwegen?",
        answerA = "Neuseeland",
        answerB = "Island",
        answerC = "Kanada",
        answerD = "Chile",
        correctAnswer = 3, // D
        explanation = "Chile hat nach Norwegen die meisten und längsten Fjorde der Welt. Die patagonischen Fjorde erstrecken sich über mehr als 1.500 km entlang der chilenischen Westküste.",
        difficulty = 2,
        funFact = "Die chilenischen Fjorde wurden durch Gletscher geformt, die sich während der letzten Eiszeit bis fast zum Meer erstreckten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher tektonische Prozess ist für die Entstehung des Ostafrikanischen Grabens verantwortlich?",
        answerA = "Subduktion – eine Platte taucht unter eine andere",
        answerB = "Kollision – zwei Kontinentalplatten prallen aufeinander",
        answerC = "Rifting – Auseinanderdriften von Platten (Divergenz)",
        answerD = "Transformstörung – Platten gleiten aneinander vorbei",
        correctAnswer = 2, // C
        explanation = "Der Ostafrikanische Graben entsteht durch Rifting – die afrikanische Platte reißt auseinander. Der Ostafrikanische Rift erstreckt sich über 6.000 km und wird in einigen Millionen Jahren zu einem neuen Ozean führen.",
        difficulty = 2,
        funFact = "Im Ostafrikanischen Graben entstanden die tiefsten Seen Afrikas: Tanganjikasee (1.470 m) und Malawisee (700 m)."
    ),

    // ── MEDIUM (difficulty = 2) ── questions 158–207 ─────────────────────────

    // --- Rain shadow effect ---

    Question(
        categoryId = 1,
        questionText = "Was versteht man unter dem geografischen Begriff 'Regenschatten'?",
        answerA = "Eine Region, die immer im Schatten von Wolken liegt",
        answerB = "Eine trockene Zone auf der windabgewandten Seite eines Gebirges",
        answerC = "Das Gebiet unterhalb eines Wasserfalles",
        answerD = "Ein Tiefdruckgebiet hinter einem Gebirge",
        correctAnswer = 1, // B
        explanation = "Der Regenschatten entsteht, wenn feuchte Luft ein Gebirge übersteigt. An der Luvseite regnet es stark, die Luft trocknet ab. Auf der Leeseite (Regenschatten) sinkt trockene Luft herab und es fällt kaum Niederschlag.",
        difficulty = 2,
        funFact = "Die Atacama-Wüste profitiert gleich von zwei Regenschatten: den Anden im Osten und dem kalten Humboldtstrom im Westen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Wüste entstand hauptsächlich durch den Regenschatteneffekt der Sierra Nevada?",
        answerA = "Mojave-Wüste",
        answerB = "Sonoran-Wüste",
        answerC = "Chihuahua-Wüste",
        answerD = "Great Basin-Wüste",
        correctAnswer = 3, // D
        explanation = "Die Great Basin-Wüste im Westen der USA liegt im Regenschatten der Sierra Nevada. Feuchte Luft vom Pazifik wird abgefangen und die östliche Seite bleibt extrem trocken.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt das Thar-Wüstengebiet, das teilweise durch den Regenschatten des Westghats entsteht?",
        answerA = "Pakistan und Iran",
        answerB = "Indien und Pakistan",
        answerC = "Nur in Indien",
        answerD = "Bangladesh und Indien",
        correctAnswer = 1, // B
        explanation = "Die Thar-Wüste erstreckt sich über Nordwestindien (Rajasthan) und Pakistan (Sindh, Punjab). Die Westghats halten einen Teil des Monsunregens zurück, aber die Entfernung vom Meer ist der Hauptfaktor für die Trockenheit.",
        difficulty = 2,
        funFact = "Die Thar ist eine der am dichtesten besiedelten Wüsten der Welt – trotz der extremen Trockenheit leben dort über 80 Millionen Menschen."
    ),

    // --- Island arcs ---

    Question(
        categoryId = 1,
        questionText = "Was ist ein 'Inselbogen' (island arc) in der Plattentektonik?",
        answerA = "Eine Reihe von Vulkaninseln, die durch Subduktion einer ozeanischen Platte entstehen",
        answerB = "Eine kreisförmige Atoll-Kette im Pazifik",
        answerC = "Eine durch Erosion geformte Halbmondbucht",
        answerD = "Ein durch tektonische Hebung entstandenes Gebirge unter Wasser",
        correctAnswer = 0, // A
        explanation = "Ein Inselbogen entsteht, wenn eine ozeanische Platte unter eine andere taucht (Subduktion). Dabei schmelzen Gesteine, Magma steigt auf und bildet eine Reihe von Vulkanen – oft als Inselkette sichtbar.",
        difficulty = 2,
        funFact = "Die bekanntesten Inselbögen der Welt sind die japanischen Inseln, die Aleuten und die Kurilen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Inselbogen trennt das Karibische Meer vom Atlantischen Ozean?",
        answerA = "Die Großen Antillen",
        answerB = "Die Kleinen Antillen",
        answerC = "Die Bahamas",
        answerD = "Die Turks-und-Caicos-Inseln",
        correctAnswer = 1, // B
        explanation = "Die Kleinen Antillen (Lesser Antilles) bilden einen Inselbogen, der das Karibische Meer vom Atlantischen Ozean trennt. Sie entstanden durch Subduktion der Karibischen Platte.",
        difficulty = 2,
        funFact = "Auf den Kleinen Antillen liegt der aktive Vulkan Soufrière Hills auf Montserrat, der 1995 die Hauptstadt Plymouth unter Asche begrub."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Inselbogen liegt zwischen der Banda-See und dem Indischen Ozean östlich von Bali?",
        answerA = "Philippinischer Inselbogen",
        answerB = "Sunda-Inselbogen (Java bis Timor)",
        answerC = "Marianen-Inselbogen",
        answerD = "Tonga-Inselbogen",
        correctAnswer = 1, // B
        explanation = "Der Sunda-Inselbogen erstreckt sich von Sumatra über Java, Bali, Lombok bis Timor. Er entsteht durch Subduktion der Australischen Platte unter die Eurasische Platte und ist durch zahlreiche Vulkane bekannt.",
        difficulty = 2,
        funFact = null
    ),

    // --- Mediterranean geography details ---

    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Insel im Mittelmeer?",
        answerA = "Sardinien",
        answerB = "Zypern",
        answerC = "Sizilien",
        answerD = "Korsika",
        correctAnswer = 2, // C
        explanation = "Sizilien ist mit einer Fläche von 25.711 km² die größte Insel im Mittelmeer. Sie gehört zu Italien und liegt nur wenige Kilometer von der süditalienischen Halbinsel entfernt.",
        difficulty = 2,
        funFact = "Auf Sizilien liegt der Ätna – der höchste und aktivste Vulkan Europas mit einer Höhe von etwa 3.357 Metern."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen Griechenland, der Türkei und dem Nahen Osten?",
        answerA = "Ionisches Meer",
        answerB = "Mittelmeer",
        answerC = "Levantinisches Becken",
        answerD = "Adriatisches Meer",
        correctAnswer = 2, // C
        explanation = "Das Levantinische Becken ist der östlichste Teil des Mittelmeers, umgeben von Griechenland, der Türkei, Syrien, dem Libanon, Israel und Ägypten.",
        difficulty = 2,
        funFact = "Das Levantinische Becken ist eines der salzreichsten Teile des Mittelmeers, da die Verdunstung die Niederschläge übersteigt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Mittelmeerland hat die meisten Inseln?",
        answerA = "Italien",
        answerB = "Spanien",
        answerC = "Türkei",
        answerD = "Griechenland",
        correctAnswer = 3, // D
        explanation = "Griechenland hat mit über 6.000 Inseln und Inselchen die meisten Inseln aller Mittelmeerländer, von denen etwa 230 dauerhaft bewohnt sind.",
        difficulty = 2,
        funFact = null
    ),

    // --- Caucasus region ---

    Question(
        categoryId = 1,
        questionText = "Welche drei Länder bilden den 'Südkaukasus' (Transkaukasien)?",
        answerA = "Russland, Georgien, Armenien",
        answerB = "Georgien, Armenien, Aserbaidschan",
        answerC = "Armenien, Aserbaidschan, Iran",
        answerD = "Tschetschenien, Dagestan, Inguschetien",
        correctAnswer = 1, // B
        explanation = "Der Südkaukasus (Transkaukasien) umfasst die drei unabhängigen Staaten Georgien, Armenien und Aserbaidschan, die südlich des Großen Kaukasusgebirges liegen.",
        difficulty = 2,
        funFact = "Armenien gilt als der älteste christliche Staat der Welt – es nahm 301 n. Chr. das Christentum als Staatsreligion an, noch vor dem Römischen Reich."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt ist die Hauptstadt Aserbaidschans und liegt am Kaspischen Meer?",
        answerA = "Tiflis",
        answerB = "Jerewan",
        answerC = "Baku",
        answerD = "Sumgait",
        correctAnswer = 2, // C
        explanation = "Baku ist die Hauptstadt Aserbaidschans und liegt am Westufer des Kaspischen Meeres. Die Stadt ist das wirtschaftliche und kulturelle Zentrum des Landes und bekannt für ihre Öl- und Gasvorkommen.",
        difficulty = 2,
        funFact = "Baku liegt unterhalb des Meeresspiegels – die Stadtmitte befindet sich etwa 28 Meter unter dem Meeresspiegel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge trennt den Großen Kaukasus vom Kleinen Kaukasus?",
        answerA = "Die Kolchische Tiefebene und das Rioni-Tal",
        answerB = "Das Talyschgebirge",
        answerC = "Das Pontische Gebirge",
        answerD = "Der Zangezur-Rücken",
        correctAnswer = 0, // A
        explanation = "Der Große Kaukasus (Hauptkamm) und der Kleine Kaukasus sind durch das Rioni-Tal in Georgien und die Kura-Araxes-Senke getrennt. Dazwischen liegen die Tiefebenen Kolchis und Kura-Araxes.",
        difficulty = 2,
        funFact = null
    ),

    // --- Central Asian steppes ---

    Question(
        categoryId = 1,
        questionText = "Wie heißt die riesige Steppenlandschaft, die sich von der Ukraine bis zur Mongolei erstreckt?",
        answerA = "Sibirische Taiga",
        answerB = "Eurasische Steppe (Pontisch-Kasachische Steppe)",
        answerC = "Anatolisches Plateau",
        answerD = "Tibetisches Hochplateau",
        correctAnswer = 1, // B
        explanation = "Die Eurasische Steppe (auch Pontisch-Kasachische Steppe) erstreckt sich über etwa 8.000 km von der Ukraine und Russland über Kasachstan bis in die Mongolei und China. Sie ist die größte Steppenlandschaft der Welt.",
        difficulty = 2,
        funFact = "Die eurasische Steppe war die 'Autobahn' der Geschichte – Nomadenvölker wie die Mongolen nutzten sie für ihre Feldzüge quer durch Eurasien."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die Südgrenze der zentralasiatischen Steppe Kasachstans?",
        answerA = "Pamir",
        answerB = "Tianshan",
        answerC = "Hindukusch",
        answerD = "Zagros",
        correctAnswer = 1, // B
        explanation = "Der Tianshan (Himmelsgebirge) bildet die südliche Grenze der kasachischen Steppe und trennt Zentralasien von Xinjiang (China). Er ist über 2.500 km lang und erreicht Höhen über 7.000 m.",
        difficulty = 2,
        funFact = "Im Tianshan liegt der Issyk-Kul, einer der größten Gebirgsseen der Welt – er friert trotz seiner Höhe von 1.606 m nie zu."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer schrumpfte dramatisch durch Bewässerungsprojekte in der zentralasiatischen Steppe?",
        answerA = "Kaspisches Meer",
        answerB = "Aralsee",
        answerC = "Balchaschsee",
        answerD = "Issyk-Kul",
        correctAnswer = 1, // B
        explanation = "Der Aralsee in Kasachstan und Usbekistan schrumpfte seit den 1960er Jahren durch sowjetische Bewässerungsprojekte auf etwa 10 % seiner ursprünglichen Fläche. Er gilt als eine der größten Umweltkatastrophen der Menschheitsgeschichte.",
        difficulty = 2,
        funFact = "Im einst fruchtbaren Aralsee-Becken liegen heute verrostete Schiffe mitten in der Wüste – Symbole einer Katastrophe, die ganze Fischerdörfer zerstörte."
    ),

    // --- Sub-Saharan African geography ---

    Question(
        categoryId = 1,
        questionText = "Welches Becken beherbergt den zweitgrößten Regenwald der Erde nach dem Amazonas?",
        answerA = "Das Nigerdelata-Becken",
        answerB = "Das Kongobecken",
        answerC = "Das Sambesi-Becken",
        answerD = "Das Seengebiet Ostafrikas",
        correctAnswer = 1, // B
        explanation = "Das Kongobecken in Zentralafrika beherbergt den zweitgrößten tropischen Regenwald der Welt. Er erstreckt sich über die Demokratische Republik Kongo und angrenzende Länder.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land in Subsahara-Afrika hat die größte Fläche?",
        answerA = "Demokratische Republik Kongo",
        answerB = "Sudan",
        answerC = "Angola",
        answerD = "Tansania",
        correctAnswer = 0, // A
        explanation = "Die Demokratische Republik Kongo (DRK) ist mit einer Fläche von 2,34 Millionen km² das größte Land in Subsahara-Afrika und das zweitgrößte Afrikas insgesamt (nach Algerien).",
        difficulty = 2,
        funFact = "Die DRK hat trotz riesiger Mineralvorkommen (Kobalt, Koltan, Diamanten) eine der niedrigsten Lebenserwartungen weltweit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches geografische Merkmal teilt Subsahara-Afrika in eine semiaride Zone und einen feuchten Süden?",
        answerA = "Das Atlasgebirge",
        answerB = "Die Sahelzone",
        answerC = "Das Ostafrikanische Hochland",
        answerD = "Die Großen Seen Afrikas",
        correctAnswer = 1, // B
        explanation = "Die Sahelzone ist ein semiarides Übergangsgebiet zwischen der Sahara im Norden und den feuchten Savannen im Süden. Sie erstreckt sich von Senegal im Westen bis Eritrea im Osten.",
        difficulty = 2,
        funFact = "Schätzungen zufolge könnte bis 2100 ein Drittel der Sahelzone durch Desertifikation zur Wüste werden – was Hunderte Millionen Menschen zur Flucht zwingen könnte."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt die größte Savanne Afrikas?",
        answerA = "Kalahari",
        answerB = "Serengeti",
        answerC = "Miombo-Waldland",
        answerD = "Sudano-Guineische Savanne",
        correctAnswer = 3, // D
        explanation = "Die Sudano-Guineische Savanne ist die größte Savannenzone Afrikas und erstreckt sich von der Westküste bis Ostafrika. Die Serengeti ist nur ein bekanntes Teilgebiet der ostafrikanischen Savannen.",
        difficulty = 2,
        funFact = null
    ),

    // --- Polynesian triangle ---

    Question(
        categoryId = 1,
        questionText = "Welche drei Inseln oder Inselgruppen markieren die Eckpunkte des Polynesischen Dreiecks?",
        answerA = "Fidschi, Tonga und Samoa",
        answerB = "Hawaii, Neuseeland und Osterinsel",
        answerC = "Tahiti, Guam und Palau",
        answerD = "Cook-Inseln, Tuvalu und Kiribati",
        correctAnswer = 1, // B
        explanation = "Das Polynesische Dreieck wird durch Hawaii im Norden, die Osterinsel (Rapa Nui) im Osten und Neuseeland (Aotearoa) im Süden begrenzt. Es umfasst einen Großteil des zentralen und südlichen Pazifiks.",
        difficulty = 2,
        funFact = "Die polynesischen Vorfahren bewältigten diese immensen Entfernungen mit bloßen Kanus, Sternennavigation und dem Wissen über Strömungen – eine der beeindruckendsten Leistungen der Menschheitsgeschichte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Volk besiedelte als erstes die Inseln des Polynesischen Dreiecks?",
        answerA = "Die Melanesier von Papua-Neuguinea",
        answerB = "Die Lapita-Kultur, Vorfahren der Polynesier",
        answerC = "Seefahrer aus dem alten China",
        answerD = "Einwanderer aus Südamerika",
        correctAnswer = 1, // B
        explanation = "Die Lapita-Kultur gilt als direkte Vorfahrenkultur der Polynesier. Sie entstand vor etwa 3.500 Jahren in Melanesien und breitete sich von dort über den Pazifik aus.",
        difficulty = 2,
        funFact = "Genetische Studien belegen, dass polynesische Vorfahren tatsächlich auch Kontakt zu südamerikanischen Indigenen hatten – eine bemerkenswerte Leistung der alten Seefahrt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die flächenmäßig größte Inselgruppe innerhalb des Polynesischen Dreiecks?",
        answerA = "Hawaii",
        answerB = "Französisch-Polynesien",
        answerC = "Tonga",
        answerD = "Samoa",
        correctAnswer = 1, // B
        explanation = "Französisch-Polynesien umfasst 118 Inseln und Atolle mit einer Meereszone von ca. 5,5 Millionen km² – das ist fast so groß wie Europa. Die Landfläche selbst beträgt aber nur 4.167 km².",
        difficulty = 2,
        funFact = null
    ),

    // --- Arctic geography ---

    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen Russland und der Nordpolregion und ist für seinen breiten Kontinentalschelf bekannt?",
        answerA = "Karasee",
        answerB = "Laptewsee",
        answerC = "Ostsibirische See",
        answerD = "Barentssee",
        correctAnswer = 3, // D
        explanation = "Die Barentssee liegt nördlich von Norwegen und Russland und hat einen sehr breiten, flachen Kontinentalschelf. Sie ist fischreich und war strategisch wichtig im Kalten Krieg.",
        difficulty = 2,
        funFact = "Die Barentssee ist nach dem niederländischen Entdecker Willem Barents benannt, der 1596 bei seiner Suche nach der Nordostpassage dort überwinterte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige arktische Territorium, das zu Dänemark gehört?",
        answerA = "Spitzbergen",
        answerB = "Jan Mayen",
        answerC = "Grönland",
        answerD = "Bären-Insel",
        correctAnswer = 2, // C
        explanation = "Grönland ist ein autonomes Territorium Dänemarks und die größte Insel der Welt (2,17 Millionen km²). Es liegt größtenteils innerhalb des Arktischen Kreises.",
        difficulty = 2,
        funFact = "Etwa 80 % Grönlands sind von einem Eisschild bedeckt. Würde dieser vollständig schmelzen, würde der Meeresspiegel weltweit um etwa 7 Meter steigen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches arktisches Inselarchipel gehört zu Norwegen und beherbergt den Svalbard-Vertrag-Stützpunkt?",
        answerA = "Franz-Josef-Land",
        answerB = "Nowaja Semlja",
        answerC = "Spitzbergen (Svalbard)",
        answerD = "Severnaya Zemlya",
        correctAnswer = 2, // C
        explanation = "Svalbard (Spitzbergen) ist ein norwegisches Archipel in der Arktis. Aufgrund des Svalbard-Vertrages von 1920 dürfen Bürger aller Unterzeichnerstaaten dort ohne Visum leben und arbeiten.",
        difficulty = 2,
        funFact = "In der Stadt Longyearbyen auf Svalbard ist es per Gesetz verboten zu sterben – das klingt absurd, hat aber einen praktischen Grund: Leichen können im Permafrost nicht verrotten."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Nordwestpassage?",
        answerA = "Ein Kanal durch den Panamakanal nördlich der Hauptschleuse",
        answerB = "Eine Schifffahrtsroute durch den Arktischen Ozean nördlich Kanadas",
        answerC = "Die Beringstraße zwischen Alaska und Russland",
        answerD = "Eine Eisbrecher-Route durch den Nordpol",
        correctAnswer = 1, // B
        explanation = "Die Nordwestpassage ist eine Schifffahrtsroute durch den Arktischen Ozean entlang der Nordküste Kanadas und Alaskas. Sie verbindet den Atlantik mit dem Pazifik. Durch den Klimawandel wird sie immer schiffbarer.",
        difficulty = 2,
        funFact = null
    ),

    // --- Antarctic research stations ---

    Question(
        categoryId = 1,
        questionText = "Welches Land betreibt die Amundsen-Scott-Südpolstation direkt am geografischen Südpol?",
        answerA = "Norwegen",
        answerB = "Argentinien",
        answerC = "USA",
        answerD = "Australien",
        correctAnswer = 2, // C
        explanation = "Die Amundsen-Scott-Südpolstation wird von den USA (NSF – National Science Foundation) betrieben und liegt exakt am geografischen Südpol auf dem antarktischen Eisschild.",
        difficulty = 2,
        funFact = "Die Station ist nach Roald Amundsen (Norwegen) und Robert Falcon Scott (Großbritannien) benannt – den ersten zwei Männern, die den Südpol erreichten (1911 und 1912)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche deutsche Forschungsstation liegt in der Antarktis?",
        answerA = "Neumayer-Station III",
        answerB = "Concordia-Station",
        answerC = "Troll-Station",
        answerD = "Esperanza-Base",
        correctAnswer = 0, // A
        explanation = "Die Neumayer-Station III ist die deutsche Antarktis-Forschungsstation, betrieben vom Alfred-Wegener-Institut. Sie steht auf dem Ekström-Eisschelf in der Atka-Bucht.",
        difficulty = 2,
        funFact = "Die Neumayer-Station wird regelmäßig auf Stelzen angehoben, um das jährliche Einsinken durch Schneefälle auszugleichen – ein technisches Meisterstück."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vertrag regelt die internationale Nutzung der Antarktis und verbietet Militärpräsenz?",
        answerA = "Genfer Antarktis-Abkommen (1950)",
        answerB = "Antarktisvertrag (1959, in Kraft 1961)",
        answerC = "Kyoto-Protokoll für Polargebiete (1997)",
        answerD = "UN-Seerechtskonvention Antarktis-Zusatz (1982)",
        correctAnswer = 1, // B
        explanation = "Der Antarktisvertrag wurde 1959 unterzeichnet und trat 1961 in Kraft. Er erklärt die Antarktis zur militärfreien Zone, schützt sie für wissenschaftliche Forschung und friert alle Gebietsansprüche ein.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Forschungsstation liegt am magnetischen Südpol und wird von Australien betrieben?",
        answerA = "Mawson-Station",
        answerB = "Davis-Station",
        answerC = "Casey-Station",
        answerD = "Macquarie-Island-Station",
        correctAnswer = 0, // A
        explanation = "Die australische Mawson-Station liegt in der Antarktis und war lange dem magnetischen Südpol am nächsten (der sich ständig verschiebt). Sie wurde 1954 eröffnet und ist eine der ältesten noch aktiven Stationen.",
        difficulty = 2,
        funFact = "Der magnetische Südpol wandert jedes Jahr – heute liegt er im Indischen Ozean nördlich der Antarktis und entfernt sich zunehmend vom geografischen Südpol."
    ),

    // --- River confluence points ---

    Question(
        categoryId = 1,
        questionText = "In welcher Stadt in Ägypten teilt sich der Nil in den Weißen und den Blauen Nil?",
        answerA = "Kairo",
        answerB = "Khartum (Sudan)",
        answerC = "Assuan",
        answerD = "Luxor",
        correctAnswer = 1, // B
        explanation = "In Khartum (Sudan) fließen der Weiße Nil (aus Uganda) und der Blaue Nil (aus Äthiopien) zusammen. Danach fließt der vereinte Nil durch Ägypten ins Mittelmeer.",
        difficulty = 2,
        funFact = "Der Blaue Nil liefert trotz seiner kürzeren Länge etwa 80 % des Nilwassers – besonders nach dem äthiopischen Monsun im Sommer."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo mündet der Missouri in den Mississippi?",
        answerA = "Bei New Orleans, Louisiana",
        answerB = "Bei St. Louis, Missouri",
        answerC = "Bei Memphis, Tennessee",
        answerD = "Bei Cairo, Illinois",
        correctAnswer = 1, // B
        explanation = "Der Missouri mündet kurz nördlich von St. Louis in den Mississippi. Diese Zusammenfluss-Stelle macht St. Louis zu einem historisch wichtigen Knotenpunkt der amerikanischen Flussschifffahrt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Zusammenfluss von Euphrat und Tigris, der den Schatt al-Arab bildet?",
        answerA = "Iran",
        answerB = "Syrien",
        answerC = "Türkei",
        answerD = "Irak",
        correctAnswer = 3, // D
        explanation = "Euphrat und Tigris vereinigen sich im südlichen Irak bei Qurna zum Schatt al-Arab, der dann in den Persischen Golf mündet. Diese Region gilt als mögliche Lage des biblischen Garten Eden.",
        difficulty = 2,
        funFact = "Der Name Mesopotamien bedeutet auf Griechisch 'Land zwischen den Flüssen' (Euphrat und Tigris) – die Wiege der ersten Hochkulturen der Menschheit."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo mündet der Rio Negro in den Amazonas in Brasilien?",
        answerA = "Bei Manaus",
        answerB = "Bei Belém",
        answerC = "Bei Iquitos (Peru)",
        answerD = "Bei Santarém",
        correctAnswer = 0, // A
        explanation = "Der Rio Negro mündet bei Manaus in den Amazonas. Die Zusammenfluss-Stelle ist für das Phänomen 'Encontro das Águas' (Begegnung der Gewässer) bekannt – die schwarzen und weißen Wässer fließen kilometerlang nebeneinander ohne sich zu vermischen.",
        difficulty = 2,
        funFact = "Das Phänomen entsteht durch unterschiedliche Temperatur, Dichte und Fließgeschwindigkeit der beiden Flüsse – ein beliebtes Touristenziel in Amazonien."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Zusammenfluss von Donau und Save, wo die heutige Hauptstadt liegt?",
        answerA = "Rumänien (Bukarest)",
        answerB = "Serbien (Belgrad)",
        answerC = "Kroatien (Zagreb)",
        answerD = "Bulgarien (Sofia)",
        correctAnswer = 1, // B
        explanation = "Belgrad liegt am Zusammenfluss von Donau und Save. Die Lage an diesem strategisch wichtigen Punkt machte die Stadt zu einer der am häufigsten belagerten und zerstörten Städte Europas.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss entsteht durch den Zusammenfluss von Rhône und Saône in Frankreich?",
        answerA = "Die Rhône setzt sich allein fort",
        answerB = "Der Loire",
        answerC = "Der Rhein",
        answerD = "Die Durance",
        correctAnswer = 0, // A
        explanation = "Bei Lyon mündet die Saône in die Rhône. Nach dem Zusammenfluss setzt sich die Rhône als vereinter Strom fort und fließt durch das Rhônetal zum Mittelmeer. Die Saône ist ein linker Nebenfluss der Rhône.",
        difficulty = 2,
        funFact = "Lyon liegt genau an diesem Zusammenfluss und war daher schon in der Antike (als Lugdunum) ein wichtiger römischer Knotenpunkt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo ensteht der Nil durch den Zusammenfluss zweier Quellflüsse – welche sind es?",
        answerA = "Weißer Nil und Blauer Nil",
        answerB = "Albert-Nil und Victoria-Nil",
        answerC = "Kagera und Luvironza",
        answerD = "Atbara und Weißer Nil",
        correctAnswer = 0, // A
        explanation = "Der Nil entsteht in Khartum durch den Zusammenfluss des Weißen Nils (aus Uganda/Viktoriasee-Region) und des Blauen Nils (aus dem Tana-See in Äthiopien). Beide tragen unterschiedliche Wassermengen bei.",
        difficulty = 2,
        funFact = "Der längste Zufluss des Nils ist technisch gesehen der Kagera-Fluss in Ruanda/Uganda, der in den Viktoriasee mündet – manche Geografen zählen ihn als 'eigentliche Quelle' des Nils."
    ),

    // --- Additional mixed topics from the specified themes ---

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge erzeugt den Regenschatten, der die Patagonische Steppe in Argentinien austrocknet?",
        answerA = "Die Kordilleren Chiles",
        answerB = "Die Anden",
        answerC = "Die Küstenkordillere Chiles",
        answerD = "Die Drakensberge",
        correctAnswer = 1, // B
        explanation = "Die Anden blockieren die feuchten Westwinde vom Pazifik. Westlich (Chile) regnet es stark, östlich (Patagonien/Argentinien) liegt der Regenschatten – es ist trocken und windig.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie nennt man den südlichsten Punkt des Polynesischen Dreiecks?",
        answerA = "Samoa",
        answerB = "Tonga",
        answerC = "Neuseeland (Aotearoa)",
        answerD = "Cookinseln",
        correctAnswer = 2, // C
        explanation = "Der südlichste Eckpunkt des Polynesischen Dreiecks ist Neuseeland (Aotearoa), das von den Maori zwischen ca. 1250 und 1300 n. Chr. besiedelt wurde – als letztes großes Landgebiet der Erde.",
        difficulty = 2,
        funFact = "Die Maori navigierten über 3.000 km Ozean von der östlichen Polynesien nach Neuseeland – ohne GPS oder Instrumente, nur mit Sternen, Wellen und Vögeln."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen dem Kaukasus im Norden und dem Iran im Süden?",
        answerA = "Schwarzes Meer",
        answerB = "Asowsches Meer",
        answerC = "Kaspisches Meer",
        answerD = "Aralsee",
        correctAnswer = 2, // C
        explanation = "Das Kaspische Meer liegt zwischen Russland und Aserbaidschan im Westen/Norden, Kasachstan und Turkmenistan im Osten sowie dem Iran im Süden. Es ist der größte abflusslose See der Welt.",
        difficulty = 2,
        funFact = "Das Kaspische Meer war erdgeschichtlich ein Teil des Urmeeres Tethys – deshalb nennen wir es noch immer 'Meer', obwohl es technisch ein See ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land liegt vollständig auf dem Kaukasus-Inselbogen zwischen Schwarzem und Kaspischem Meer und ist seit 1991 unabhängig?",
        answerA = "Tschetschenien",
        answerB = "Dagestan",
        answerC = "Georgien",
        answerD = "Abchasien",
        correctAnswer = 2, // C
        explanation = "Georgien liegt im Südkaukasus, grenzt an Russland, die Türkei, Armenien und Aserbaidschan und wurde 1991 nach dem Zerfall der Sowjetunion unabhängig.",
        difficulty = 2,
        funFact = "Georgien ist eine der ältesten Weinbauregionen der Welt – dort wird seit über 8.000 Jahren Wein in Amphoren (Qvevri) hergestellt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Forschungsstation liegt auf dem höchsten Punkt des Antarktischen Eisschilds?",
        answerA = "McMurdo-Station (USA)",
        answerB = "Concordia-Station (Frankreich/Italien)",
        answerC = "Vostok-Station (Russland)",
        answerD = "Halley-Station (Großbritannien)",
        correctAnswer = 2, // C
        explanation = "Die russische Wostok-Station (Vostok) liegt auf dem Antarktischen Eisschild und ist für die tiefste jemals auf der Erde gemessene Temperatur bekannt: -89,2 °C (1983).",
        difficulty = 2,
        funFact = "Unter der Wostok-Station verbirgt sich der Wostok-See – ein riesiger Süßwassersee, der seit Millionen von Jahren unter dem Eis eingeschlossen ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Strom trägt im Arktischen Ozean warmes Atlantikwasser nach Norden?",
        answerA = "Labradorstrom",
        answerB = "Nordatlantikstrom / Golfstrom-Ausläufer",
        answerC = "Kanarenstrom",
        answerD = "Humboldtstrom",
        correctAnswer = 1, // B
        explanation = "Der Nordatlantikstrom (Ausläufer des Golfstroms) trägt warmes Wasser in den Arktischen Ozean und beeinflusst das Klima Europas und der arktischen Randseen erheblich.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches subsaharische Land ist bekannt als 'das Land der tausend Hügel'?",
        answerA = "Uganda",
        answerB = "Burundi",
        answerC = "Ruanda",
        answerD = "Kamerun",
        correctAnswer = 2, // C
        explanation = "Ruanda wird oft als 'Pays des mille collines' (Land der tausend Hügel) bezeichnet. Das kleine, dicht besiedelte Land ist von grünen Hügelketten durchzogen und liegt im Herzen Afrikas.",
        difficulty = 2,
        funFact = "Ruanda hat heute weltweit den höchsten Frauenanteil im nationalen Parlament – über 60 % der Sitze sind von Frauen besetzt."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land fließen Euphrat und Tigris zusammen und münden dann als Schatt al-Arab in den Persischen Golf?",
        answerA = "Kuwait",
        answerB = "Syrien",
        answerC = "Iran",
        answerD = "Irak",
        correctAnswer = 3, // D
        explanation = "Im südlichen Irak, bei der Stadt al-Qurna, vereinigen sich Euphrat und Tigris zum Schatt al-Arab. Dieser fließt etwa 190 km bis zum Persischen Golf und bildet die Grenze zwischen Irak und Iran.",
        difficulty = 2,
        funFact = "Der Schatt al-Arab war Schauplatz des Iran-Irak-Krieges (1980–1988), der u. a. über die Kontrolle dieser strategischen Wasserstraße geführt wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte Inselreich im Polynesischen Dreieck nach Bevölkerung?",
        answerA = "Tonga",
        answerB = "Samoa",
        answerC = "Französisch-Polynesien",
        answerD = "Hawaii (USA)",
        correctAnswer = 3, // D
        explanation = "Hawaii ist mit über 1,4 Millionen Einwohnern die bevölkerungsreichste Inselgruppe im Polynesischen Dreieck, auch wenn es als US-Bundesstaat nicht als eigenständiges Reich gilt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss bildet die Grenze zwischen Sambia und Simbabwe und beherbergt die Victoriafälle?",
        answerA = "Kongo",
        answerB = "Sambesi",
        answerC = "Limpopo",
        answerD = "Okavango",
        correctAnswer = 1, // B
        explanation = "Der Sambesi bildet die Grenze zwischen Sambia und Simbabwe und stürzt bei den Victoriafällen (Mosi-oa-Tunya) 108 Meter in die Tiefe. Die Fälle sind mit einer Breite von 1.708 m die breitesten der Welt.",
        difficulty = 2,
        funFact = "Die Victoriafälle erzeugen eine Wasserwolke, die bei ruhigem Wetter bis zu 30 km weit zu sehen ist – daher der Name 'Mosi-oa-Tunya' auf Kololo: 'Rauch, der donnert'."
    ),

    // ── MEDIUM (difficulty = 2) ── questions 206–255 (world trade & transport geography) ─────

    // --- Suez Canal ---

    Question(
        categoryId = 1,
        questionText = "Wie lang ist der Suezkanal ungefähr?",
        answerA = "80 km",
        answerB = "193 km",
        answerC = "320 km",
        answerD = "450 km",
        correctAnswer = 1, // B
        explanation = "Der Suezkanal ist etwa 193 km lang und verbindet das Mittelmeer (Port Said) mit dem Roten Meer (Suez). Er wurde 1869 eröffnet und ist eine der wichtigsten Schifffahrtsrouten der Welt.",
        difficulty = 2,
        funFact = "Der Suezkanal wird täglich von durchschnittlich 50 Schiffen durchquert und verkürzt den Seeweg von Europa nach Asien um bis zu 7.000 km."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief ist der Suezkanal (Fahrwassertiefe nach aktueller Erweiterung)?",
        answerA = "12 Meter",
        answerB = "20 Meter",
        answerC = "24 Meter",
        answerD = "30 Meter",
        correctAnswer = 2, // C
        explanation = "Nach den Erweiterungsarbeiten (zuletzt 2015) hat der Suezkanal eine Fahrwassertiefe von etwa 24 Metern und eine Breite von bis zu 215 Metern. Er hat keine Schleusen, da der Meeresspiegel auf beiden Seiten nahezu gleich ist.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Ereignis blockierte den Suezkanal im März 2021 für fast eine Woche?",
        answerA = "Ein schwerer Sturm zerstörte das Kanalufer",
        answerB = "Das Containerschiff Ever Given lief quer",
        answerC = "Ein Erdbeben beschädigte die Kanalwände",
        answerD = "Militärische Übungen sperrten den Kanal",
        correctAnswer = 1, // B
        explanation = "Im März 2021 blockierte das Containerschiff Ever Given (399 m lang) den Suezkanal, nachdem es sich quergestellt hatte. Die Blockade dauerte sechs Tage und kostete den Welthandel schätzungsweise 9 Milliarden Dollar täglich.",
        difficulty = 2,
        funFact = "Das Schiff war so groß, dass es fast jeden Zentimeter des Kanals ausfüllte – Rettungsschlepper benötigten Tage, um es zu befreien."
    ),

    // --- Panama Canal ---

    Question(
        categoryId = 1,
        questionText = "Wie viele Schleusenstufen hat der Panamakanal auf jeder Seite?",
        answerA = "Zwei Schleusenstufen",
        answerB = "Drei Schleusenstufen",
        answerC = "Vier Schleusenstufen",
        answerD = "Keine – er hat keine Schleusen",
        correctAnswer = 1, // B
        explanation = "Der Panamakanal hebt und senkt Schiffe über drei Schleusenstufen auf jeder Seite: auf der Atlantikseite die Gatún-Schleusen (+26 m), auf der Pazifikseite die Pedro-Miguel- und Miraflores-Schleusen.",
        difficulty = 2,
        funFact = "Der Panamakanal wurde 2016 um einen dritten Schleusenkanal erweitert, der auch Post-Panamax-Schiffe aufnehmen kann."
    ),

    Question(
        categoryId = 1,
        questionText = "Warum benötigt der Panamakanal Schleusen, der Suezkanal aber nicht?",
        answerA = "Weil der Pazifik tiefer ist als das Mittelmeer",
        answerB = "Weil das Gelände Panamas hügelig ist und der Kanal Seen auf 26 m Höhe überquert",
        answerC = "Weil der Atlantik und der Pazifik unterschiedliche Wasserhöhen haben",
        answerD = "Aus historischen Gründen der ursprünglichen Planung",
        correctAnswer = 1, // B
        explanation = "Der Panamakanal führt durch Mittelamerika, dessen Gelände deutlich höher als der Meeresspiegel liegt. Der Gatún-See liegt auf 26 Metern Höhe. Schiffe müssen durch Schleusen auf dieses Niveau angehoben und wieder abgesenkt werden.",
        difficulty = 2,
        funFact = null
    ),

    // --- Major shipping lanes ---

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge ist die meistbefahrene der Welt nach Schiffszahlen?",
        answerA = "Straße von Gibraltar",
        answerB = "Straße von Malakka",
        answerC = "Straße von Hormus",
        answerD = "Kanal von Dover",
        correctAnswer = 3, // D
        explanation = "Der Kanal von Dover (zwischen England und Frankreich) gilt als eine der meistbefahrenen Wasserstraßen der Welt – täglich passieren ihn etwa 500 Schiffe. Die Straße von Malakka ist jedoch für den Frachttonnenkilometer bedeutender.",
        difficulty = 2,
        funFact = "Die Straße von Malakka ist für den globalen Handel entscheidend: Rund 40 % des Welthandels und ein Drittel des weltweiten Öls passieren sie täglich."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Schifffahrtsroute verbindet Europa direkt mit Ostasien um das Kap der Guten Hoffnung?",
        answerA = "Die Nordostpassage",
        answerB = "Die Kap-Route (Cape Route)",
        answerC = "Die Suezroute",
        answerD = "Die Drakepassage-Route",
        correctAnswer = 1, // B
        explanation = "Die Kap-Route führt von Europa um das Kap der Guten Hoffnung (Südafrika) nach Asien. Vor dem Bau des Suezkanals 1869 war dies die einzige Seeroute zwischen Europa und dem Indischen Ozean.",
        difficulty = 2,
        funFact = "Die Kap-Route ist zwar länger als der Suezkanal, wird aber von Supertankern genutzt, die für den Kanal zu groß sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Durch welche Meerenge fließt etwa 40 % des weltweit verschifften Erdöls?",
        answerA = "Straße von Gibraltar",
        answerB = "Bab el-Mandeb",
        answerC = "Straße von Malakka",
        answerD = "Straße von Hormus",
        correctAnswer = 3, // D
        explanation = "Die Straße von Hormus zwischen dem Iran und der Arabischen Halbinsel ist das wichtigste Nadelöhr für den weltweiten Öltransport. Rund 20 Millionen Barrel Öl passieren sie täglich.",
        difficulty = 2,
        funFact = null
    ),

    // --- World's longest tunnels ---

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Eisenbahntunnel der Welt (Stand 2024)?",
        answerA = "Eurotunnel (Ärmelkanal)",
        answerB = "Gotthard-Basistunnel (Schweiz)",
        answerC = "Seikan-Tunnel (Japan)",
        answerD = "Lötschberg-Basistunnel (Schweiz)",
        correctAnswer = 1, // B
        explanation = "Der Gotthard-Basistunnel in der Schweiz ist mit 57,1 km der längste Eisenbahntunnel der Welt. Er wurde 2016 eröffnet und unterquert die Alpen auf einer Höhe von 550 m über dem Meeresspiegel.",
        difficulty = 2,
        funFact = "Der Bau des Gotthard-Basistunnels dauerte 17 Jahre. Er ermöglicht es, die Alpen in nur 20 Minuten zu unterqueren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land betreibt den Seikan-Tunnel – den längsten Unterwassertunnel der Welt?",
        answerA = "China",
        answerB = "Südkorea",
        answerC = "Japan",
        answerD = "Norwegen",
        correctAnswer = 2, // C
        explanation = "Der Seikan-Tunnel in Japan ist 53,9 km lang und verbindet die Inseln Honshu und Hokkaido unter der Tsugaru-Meerenge. Er ist der längste Eisenbahntunnel unter Wasser.",
        difficulty = 2,
        funFact = "Der Seikan-Tunnel hat zwei Stationen unter dem Meeresspiegel, die als Notfallunterkünfte dienen – ein einzigartiges Merkmal weltweit."
    ),

    Question(
        categoryId = 1,
        questionText = "Unter welchem Meeresarm verläuft der Eurotunnel (Channel Tunnel)?",
        answerA = "Irische See",
        answerB = "Nordsee",
        answerC = "Ärmelkanal",
        answerD = "Biskayabucht",
        correctAnswer = 2, // C
        explanation = "Der Eurotunnel (Channel Tunnel) verbindet Folkestone (England) mit Coquelles bei Calais (Frankreich) unter dem Ärmelkanal. Er besteht aus drei Tunneln: zwei Eisenbahntunnel und ein Servicetunnel.",
        difficulty = 2,
        funFact = null
    ),

    // --- Famous railway routes ---

    Question(
        categoryId = 1,
        questionText = "Welche Eisenbahnstrecke gilt als die längste der Welt auf einer einzigen Linie?",
        answerA = "Trans-Kanada-Bahn",
        answerB = "Transsibirische Eisenbahn",
        answerC = "Indian Pacific (Australien)",
        answerD = "Orient-Express",
        correctAnswer = 1, // B
        explanation = "Die Transsibirische Eisenbahn verbindet Moskau mit Wladiwostok über 9.289 km. Die Fahrt dauert etwa 7 Tage und die Strecke durchquert 8 Zeitzonen.",
        difficulty = 2,
        funFact = "Die Transsibirische Eisenbahn wurde zwischen 1891 und 1916 gebaut und ist bis heute eine wichtige Fracht- und Passagierroute durch Sibirien."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Bahnstrecke in den USA verbindet die Ost- mit der Westküste und hat historische Bedeutung?",
        answerA = "Amtrak Sunset Limited",
        answerB = "First Transcontinental Railroad",
        answerC = "California Zephyr",
        answerD = "Empire Builder",
        correctAnswer = 1, // B
        explanation = "Die First Transcontinental Railroad (1869) war die erste Eisenbahnstrecke, die die USA von der Ostküste bis zur Westküste (Sacramento, Kalifornien) verband. Sie revolutionierte die Erschließung des amerikanischen Westens.",
        difficulty = 2,
        funFact = "Beim Bau arbeiteten vorwiegend Einwanderer: irische im Osten, chinesische im Westen – über 10.000 chinesische Arbeiter legten die gefährlichsten Abschnitte der Sierra Nevada.",
        ),

    Question(
        categoryId = 1,
        questionText = "Welche Hochgebirgs-Eisenbahnstrecke überquert die Andenpässe in Peru und gilt als eine der spektakulärsten der Welt?",
        answerA = "Cusco–Puno-Bahn",
        answerB = "Ferrocarril Central Andino (Lima–Huancayo)",
        answerC = "Tren a las Nubes (Argentinien)",
        answerD = "Machu-Picchu-Bahn",
        correctAnswer = 1, // B
        explanation = "Der Ferrocarril Central Andino (Lima–Huancayo) erreicht auf 4.829 m über dem Meeresspiegel eine der höchsten Eisenbahnstrecken der Welt. Die Strecke überwindet einen Höhenunterschied von 4.780 m auf 335 km.",
        difficulty = 2,
        funFact = null
    ),

    // --- Trans-continental highways ---

    Question(
        categoryId = 1,
        questionText = "Welche Fernstraße verbindet Alaska (USA) mit dem südlichsten Zipfel Südamerikas?",
        answerA = "Route 66",
        answerB = "Pan-American Highway",
        answerC = "Carretera Austral",
        answerD = "Trans-Amazonian Highway",
        correctAnswer = 1, // B
        explanation = "Der Pan-American Highway erstreckt sich von Prudhoe Bay (Alaska) bis Ushuaia (Argentinien) über rund 30.000 km. Er verbindet 17 Länder – mit einer Unterbrechung im Darién-Gap zwischen Panama und Kolumbien.",
        difficulty = 2,
        funFact = "Das Darién-Gap (ca. 87 km) ist das einzige fehlende Stück des Pan-American Highways – unwegsamer Dschungel und politische Unstimmigkeiten verhindern den Bau."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche transkontinentale Straße verbindet Portugal mit dem russischen Fernen Osten?",
        answerA = "E40 (Europäische Fernstraße 40)",
        answerB = "Seidenstraßen-Autobahn",
        answerC = "Trans-Sibirische Autostraße",
        answerD = "Asiatische Fernstraße AH1",
        correctAnswer = 0, // A
        explanation = "Die E40 ist mit etwa 8.500 km eine der längsten Fernstraßen Europas und Teilen Asiens. Sie beginnt in Calais (Frankreich) und reicht bis nach Ridder (Kasachstan), verläuft also von West- nach Zentralasien.",
        difficulty = 2,
        funFact = null
    ),

    // --- Urbanization and megacities ---

    Question(
        categoryId = 1,
        questionText = "Ab wie vielen Einwohnern wird eine Stadt offiziell als 'Megastadt' definiert?",
        answerA = "1 Million",
        answerB = "5 Millionen",
        answerC = "10 Millionen",
        answerD = "20 Millionen",
        correctAnswer = 2, // C
        explanation = "Eine Megastadt (Megacity) ist per UN-Definition eine städtische Agglomeration mit mehr als 10 Millionen Einwohnern. Weltweit gibt es etwa 35 solcher Städte (Stand 2024).",
        difficulty = 2,
        funFact = "1950 gab es weltweit nur zwei Megastädte: New York und Tokio. Heute dominieren Asien und Lateinamerika die Liste."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt ist aktuell die bevölkerungsreichste Megastadt der Welt?",
        answerA = "Delhi",
        answerB = "Shanghai",
        answerC = "Tokyo",
        answerD = "São Paulo",
        correctAnswer = 2, // C
        explanation = "Tokio (Tokyo) ist mit der Metropolregion von über 37 Millionen Einwohnern nach wie vor die bevölkerungsreichste Megastadt der Welt, gefolgt von Delhi mit rund 33 Millionen.",
        difficulty = 2,
        funFact = "Delhi wächst so schnell, dass es laut UN-Prognosen Tokio bis 2028 als weltgrößte Stadt überholen könnte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat die höchste Urbanisierungsrate (Anteil der Stadtbevölkerung)?",
        answerA = "Nordamerika",
        answerB = "Europa",
        answerC = "Lateinamerika",
        answerD = "Ozeanien",
        correctAnswer = 3, // D
        explanation = "Ozeanien (vor allem Australien und Neuseeland) hat mit über 85 % die höchste Urbanisierungsrate aller Kontinente. Nordamerika und Lateinamerika folgen mit etwa 82 % bzw. 81 %.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent erlebt aktuell die schnellste Urbanisierung weltweit?",
        answerA = "Asien",
        answerB = "Südamerika",
        answerC = "Afrika",
        answerD = "Europa",
        correctAnswer = 2, // C
        explanation = "Afrika erlebt die schnellste Urbanisierungsdynamik weltweit. Der Anteil der Stadtbevölkerung wächst schneller als in jedem anderen Kontinent – bis 2050 wird mehr als die Hälfte der Afrikaner in Städten leben.",
        difficulty = 2,
        funFact = "Lagos in Nigeria wächst täglich um Tausende Menschen und könnte bis 2100 zur größten Stadt der Welt werden."
    ),

    // --- World trade routes ---

    Question(
        categoryId = 1,
        questionText = "Was war die historische Bedeutung der Seidenstraße als Handelsnetz?",
        answerA = "Eine Schifffahrtsroute zwischen China und Europa",
        answerB = "Ein Netz von Landrouten, das China mit dem Mittelmeerraum verband",
        answerC = "Eine Eisenbahnlinie durch Zentralasien",
        answerD = "Eine Handelsroute ausschließlich für Seide und Gewürze",
        correctAnswer = 1, // B
        explanation = "Die Seidenstraße war ein Netz von Karawanenrouten, das seit dem 2. Jahrhundert v. Chr. China über Zentralasien, Persien und den Nahen Osten mit dem Mittelmeerraum verband. Neben Seide wurden Gewürze, Porzellan, Gold und Ideen ausgetauscht.",
        difficulty = 2,
        funFact = "Der Begriff 'Seidenstraße' wurde erst 1877 vom deutschen Geographen Ferdinand von Richthofen geprägt – die Händler selbst kannten diesen Namen nicht."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Programm Chinas soll die Handelswege der alten Seidenstraße im 21. Jahrhundert erneuern?",
        answerA = "Trans-Asian Railway Network",
        answerB = "Belt and Road Initiative (BRI)",
        answerC = "Asian Infrastructure Investment Bank",
        answerD = "Shanghai Cooperation Organisation",
        correctAnswer = 1, // B
        explanation = "Die Belt and Road Initiative (BRI, auch 'Neue Seidenstraße') wurde 2013 von China initiiert und umfasst Infrastrukturprojekte in über 140 Ländern: Häfen, Eisenbahnen, Straßen und Energie-Pipelines.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Hafen ist nach Containerumschlag der größte der Welt?",
        answerA = "Rotterdam",
        answerB = "Singapur",
        answerC = "Shanghai",
        answerD = "Hongkong",
        correctAnswer = 2, // C
        explanation = "Shanghai ist seit 2010 der größte Containerhafen der Welt und verarbeitete 2023 über 49 Millionen TEU (Standardcontainereinheiten). Er liegt am Yangtze-Delta und ist ein zentraler Knotenpunkt des globalen Handels.",
        difficulty = 2,
        funFact = "Der Hafen Shanghai ist so groß, dass seine Terminals eine Gesamtlänge von über 20 km umfassen."
    ),

    // --- International airports geography ---

    Question(
        categoryId = 1,
        questionText = "Welches ist das flächenmäßig größte Flughafengelände der Welt?",
        answerA = "Dubai International Airport",
        answerB = "King Fahd International Airport (Saudi-Arabien)",
        answerC = "Denver International Airport",
        answerD = "Dallas/Fort Worth International Airport",
        correctAnswer = 1, // B
        explanation = "Der King Fahd International Airport in Dammam (Saudi-Arabien) ist mit einer Fläche von 776 km² das flächenmäßig größte Flughafengelände der Welt – größer als das Stadtgebiet von Bahrain.",
        difficulty = 2,
        funFact = "Trotz seiner riesigen Fläche zählt der King Fahd Airport nicht zu den verkehrsreichsten Flughäfen – Dubai und Heathrow haben weitaus mehr Passagiere."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher internationale Flughafen ist der verkehrsreichste der Welt nach Passagierzahlen (Stand 2023)?",
        answerA = "London Heathrow",
        answerB = "Hartsfield–Jackson Atlanta",
        answerC = "Dubai International",
        answerD = "Beijing Capital",
        correctAnswer = 2, // C
        explanation = "Der Dubai International Airport überholte 2022/23 den Hartsfield-Jackson Atlanta Airport als meistfrequentierten Flughafen der Welt (nach internationalen Passagieren) und verzeichnete über 86 Millionen Passagiere.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Flughafen hat die längste Start- und Landebahn der Welt?",
        answerA = "Qamdo Bamda Airport (Tibet, China)",
        answerB = "Hartsfield-Jackson Atlanta",
        answerC = "Riyad King Khalid International",
        answerD = "Denver International Airport",
        correctAnswer = 0, // A
        explanation = "Der Qamdo Bamda Airport in Tibet, China, hat mit einer Startbahn von 5.500 Metern die längste Landebahn der Welt. Die extreme Höhe (4.334 m ü. NN) erfordert eine sehr lange Bahn zum Starten und Landen.",
        difficulty = 2,
        funFact = "Auf dieser Höhe ist die Luft so dünn, dass Flugzeuge eine viel höhere Geschwindigkeit beim Start benötigen als auf Meereshöhe."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Inselgruppe liegt der Flughafen Adolfo Suárez Madrid-Barajas nicht, obwohl er zu Spanien gehört?",
        answerA = "Diese Frage ist falsch – Barajas liegt auf dem spanischen Festland",
        answerB = "Auf den Balearen",
        answerC = "Auf den Kanarischen Inseln",
        answerD = "Auf den Kanarischen Inseln und Balearen",
        correctAnswer = 0, // A
        explanation = "Madrid-Barajas liegt auf dem spanischen Festland in der Hauptstadt Madrid. Spaniens größte Inselflughäfen sind Gran Canaria und Mallorca – beide ebenfalls stark frequentiert.",
        difficulty = 2,
        funFact = null
    ),

    // --- Canal systems additional ---

    Question(
        categoryId = 1,
        questionText = "Welches Kanalsystem in den Niederlanden gilt als Weltkulturerbe und prägte das Land grundlegend?",
        answerA = "Rhein-Schelde-Kanal",
        answerB = "Amsterdamer Grachtengürtel",
        answerC = "Noord-Hollands Kanaal",
        answerD = "Markermeer-Poldersystem",
        correctAnswer = 1, // B
        explanation = "Der Amsterdamer Grachtengürtel aus dem 17. Jahrhundert ist UNESCO-Weltkulturerbe. Die Grachten wurden für Handel, Transport und Entwässerung gebaut und prägten die Stadtentwicklung grundlegend.",
        difficulty = 2,
        funFact = "Amsterdam hat über 100 km Grachten, mehr als 1.500 Brücken und rund 2.500 Hausboote – mehr als jede andere Stadt der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kanal verbindet die Nordsee mit der Ostsee durch Schleswig-Holstein?",
        answerA = "Rhein-Herne-Kanal",
        answerB = "Mittellandkanal",
        answerC = "Nord-Ostsee-Kanal (Kiel-Kanal)",
        answerD = "Elbe-Lübeck-Kanal",
        correctAnswer = 2, // C
        explanation = "Der Nord-Ostsee-Kanal (Kiel-Kanal) ist 98 km lang und verbindet Brunsbüttel an der Elbe mit Kiel an der Ostsee. Er ist nach Passagierzahlen eine der meistbefahrenen Wasserstraßen der Welt.",
        difficulty = 2,
        funFact = "Der Kiel-Kanal wurde 1895 eröffnet und spart Schiffen die gefährliche Umfahrung der Jütländischen Halbinsel um bis zu 900 km."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Canal du Midi, der Atlantik und Mittelmeer verbindet?",
        answerA = "Spanien",
        answerB = "Italien",
        answerC = "Frankreich",
        answerD = "Portugal",
        correctAnswer = 2, // C
        explanation = "Der Canal du Midi in Südfrankreich verbindet Toulouse mit dem Mittelmeer und ist damit Teil der Wasserstraße zwischen Atlantik (via Garonne) und Mittelmeer. Er wurde 1681 fertiggestellt und ist UNESCO-Welterbe.",
        difficulty = 2,
        funFact = null
    ),

    // --- World's longest tunnels (road) ---

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Straßentunnel der Welt?",
        answerA = "Eurotunnel (England–Frankreich)",
        answerB = "Laerdal-Tunnel (Norwegen)",
        answerC = "Gotthard-Straßentunnel (Schweiz)",
        answerD = "Zhongnanshan-Tunnel (China)",
        correctAnswer = 1, // B
        explanation = "Der Laerdal-Tunnel in Norwegen ist mit 24,5 km der längste Straßentunnel der Welt. Er verbindet Laerdal und Aurland und wurde 2000 eröffnet. Innen leuchtet er in drei Beleuchtungszonen, um Fahrern das Gefühl langer Strecken zu mildern.",
        difficulty = 2,
        funFact = "Im Laerdal-Tunnel gibt es drei spezielle Beleuchtungsinseln mit warmem Licht, die Autofahrern helfen, die monotone 20-minütige Fahrt zu überstehen."
    ),

    // --- More megacities and urbanization ---

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Megastädte (Städte mit über 10 Mio. Einwohnern)?",
        answerA = "USA",
        answerB = "Brasilien",
        answerC = "China",
        answerD = "Indien",
        correctAnswer = 2, // C
        explanation = "China hat die meisten Megastädte weltweit: Shanghai, Beijing, Chongqing, Guangzhou und weitere überschreiten die 10-Millionen-Grenze in ihrer Agglomeration. Indien holt aber schnell auf.",
        difficulty = 2,
        funFact = "Chongqing im Inneren Chinas ist möglicherweise die bevölkerungsreichste Verwaltungseinheit der Welt mit über 32 Millionen Einwohnern im Verwaltungsbezirk."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die 'Verstädterungsrate' und welches afrikanische Land hat sie am höchsten?",
        answerA = "Die Wachstumsrate der Stadtbevölkerung; Niger führt Afrika",
        answerB = "Der Anteil der Stadtbevölkerung an der Gesamtbevölkerung; Gabun führt Afrika",
        answerC = "Die Bevölkerungsdichte in Städten; Lagos führt",
        answerD = "Die Anzahl der Großstädte pro km²; Ruanda führt",
        correctAnswer = 1, // B
        explanation = "Die Verstädterungsrate bezeichnet den Anteil der in Städten lebenden Bevölkerung. In Afrika hat Gabun mit über 90 % eine der höchsten Urbanisierungsraten – hauptsächlich wegen der Ölwirtschaft, die Bevölkerung in die Städte zieht.",
        difficulty = 2,
        funFact = null
    ),

    // --- Additional trade & transport ---

    Question(
        categoryId = 1,
        questionText = "Welches Meeresgebiet ist für den Welthandel ein strategisches Nadelöhr zwischen Afrika und Asien?",
        answerA = "Golf von Bengalen",
        answerB = "Arabisches Meer",
        answerC = "Golf von Aden und Bab el-Mandeb",
        answerD = "Straße von Mosambik",
        correctAnswer = 2, // C
        explanation = "Der Golf von Aden und die Meerenge Bab el-Mandeb sind das Nadelöhr zwischen dem Roten Meer und dem Indischen Ozean. Jährlich passieren hier über 20.000 Schiffe, darunter ein Drittel aller europäisch-asiatischen Containerfrachter.",
        difficulty = 2,
        funFact = "Somalische Piraten machten den Golf von Aden um 2008–2012 zu einer der gefährlichsten Schifffahrtsrouten der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel ist ein wichtiger Umschlaghafen und liegt an der Kreuzung der Schifffahrtsrouten zwischen Indischem und Pazifischem Ozean?",
        answerA = "Sri Lanka",
        answerB = "Madagaskar",
        answerC = "Singapur",
        answerD = "Borneo",
        correctAnswer = 2, // C
        explanation = "Singapur liegt an der Straße von Malakka, dem wichtigsten Schifffahrtskorridor zwischen dem Indischen Ozean und dem Südchinesischen Meer/Pazifik. Der Stadtstaat ist der zweitbeschäftigtste Containerhafen der Welt.",
        difficulty = 2,
        funFact = "Singapurs Hafen verarbeitete 2023 über 38 Millionen TEU. Ohne den Suezkanal würden alle Schiffe um Kap der Guten Hoffnung umgeleitet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge erschwert den Bau einer durchgehenden Verbindungsstraße zwischen Nord- und Südamerika im Darién-Gap?",
        answerA = "Die zentralamerikanische Kordillere",
        answerB = "Der Darién-Regenwald und das Atrato-Sumpfgebiet",
        answerC = "Das kolumbianische Hochland",
        answerD = "Die Sierras de Baudó",
        correctAnswer = 1, // B
        explanation = "Das Darién-Gap (87 km) zwischen Panama und Kolumbien ist kein Gebirge, sondern ein Dschungel- und Sumpfgebiet. Die Kombination aus dichtem Regenwald, Sumpfland und fehlender Infrastruktur macht einen Straßenbau extrem schwierig.",
        difficulty = 2,
        funFact = null
    ),

    // --- Railway routes ---

    Question(
        categoryId = 1,
        questionText = "Welche australische Transkontinentalbahn verbindet Adelaide mit Darwin von Süd nach Nord?",
        answerA = "Indian Pacific",
        answerB = "The Ghan",
        answerC = "XPT New South Wales",
        answerD = "Overland Melbourne–Adelaide",
        correctAnswer = 1, // B
        explanation = "The Ghan verbindet Adelaide (Südaustralien) mit Darwin (Northern Territory) über 2.979 km. Der Name erinnert an die afghánischen Kamelführer des 19. Jahrhunderts, die das Innere Australiens erschlossen.",
        difficulty = 2,
        funFact = "The Ghan fährt durch das Herz Australiens – durch Wüsten, Salzseen und Buschland, weitgehend von jeder Zivilisation abgeschnitten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gebirgspass in der Schweiz wurde durch den Gotthard-Basistunnel für den Schienenverkehr überflüssig?",
        answerA = "Simplonpass",
        answerB = "Berninapass",
        answerC = "Gotthardpass",
        answerD = "Sustenpass",
        correctAnswer = 2, // C
        explanation = "Der Gotthard-Basistunnel (2016) ersetzte den alten Gotthard-Bahntunnel (1882) und die direkte Alpenüberquerung über den Gotthardpass für den Hochgeschwindigkeitsverkehr. Der alte Tunnel wird weiterhin für Regionalzüge genutzt.",
        difficulty = 2,
        funFact = null
    ),

    // --- More airport geography ---

    Question(
        categoryId = 1,
        questionText = "Welcher Flughafen liegt am höchsten über dem Meeresspiegel und ist für regulären Linienverkehr zugänglich?",
        answerA = "La Paz El Alto (Bolivien)",
        answerB = "Lhasa Gonggar (Tibet)",
        answerC = "Qamdo Bamda (Tibet, China)",
        answerD = "Cusco Alejandro Velasco Astete (Peru)",
        correctAnswer = 2, // C
        explanation = "Der Qamdo Bamda Airport in Tibet liegt auf 4.334 m über dem Meeresspiegel und ist damit der höchstgelegene zivile Flughafen mit regulärem Linienverkehr. La Paz El Alto liegt auf 4.061 m.",
        difficulty = 2,
        funFact = "In diesen Höhen benötigen Flugzeuge sehr viel längere Startbahnen, da die dünne Luft weniger Auftrieb erzeugt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher europäische Flughafen ist nach Passagieren der meistfrequentierte (Stand 2023)?",
        answerA = "Frankfurt am Main",
        answerB = "Amsterdam Schiphol",
        answerC = "Paris Charles de Gaulle",
        answerD = "London Heathrow",
        correctAnswer = 3, // D
        explanation = "London Heathrow ist mit über 79 Millionen Passagieren im Jahr 2023 der meistfrequentierte Flughafen Europas und einer der wichtigsten Luftverkehrsknotenpunkte weltweit.",
        difficulty = 2,
        funFact = "Heathrow ist der einzige der größten europäischen Flughäfen, der nur zwei Start- und Landebahnen hat – ein Kapazitätsproblem, das seit Jahrzehnten politisch umstritten ist."
    ),

    // --- World trade routes additional ---

    Question(
        categoryId = 1,
        questionText = "Welches Meergebiet bildet den Hauptkorridor des Handels zwischen Europa, Nordafrika und Asien?",
        answerA = "Schwarzes Meer",
        answerB = "Rotes Meer",
        answerC = "Mittelmeer",
        answerD = "Golf von Bengalen",
        correctAnswer = 2, // C
        explanation = "Das Mittelmeer ist seit der Antike der Haupthandelsweg zwischen Europa, Nordafrika und Asien. Heute verbindet es den Atlantik (via Gibraltar) mit dem Suezkanal und dem Indischen Ozean.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Schifffahrtsroute durch die Arktis könnte durch den Klimawandel immer wichtiger werden?",
        answerA = "Nordwestpassage (Kanada)",
        answerB = "Nordostpassage (Russland)",
        answerC = "Beide arktischen Routen gleichzeitig",
        answerD = "Polarer Direktweg über den Nordpol",
        correctAnswer = 1, // B
        explanation = "Die Nordostpassage entlang der russischen Arktisküste wird durch das Abschmelzen des Meereises zunehmend schiffbar. Sie würde die Route zwischen Europa und Ostasien um etwa 40 % gegenüber dem Suezkanal verkürzen.",
        difficulty = 2,
        funFact = "Russland verlangt Gebühren und Pilotdienste für Schiffe, die die Nordostpassage (Nördliche Seeroute) nutzen – ein geopolitisch umstrittenes Thema."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Ozean befindet sich der 'Garbage Patch', ein riesiges Müllgebiet, das durch Meeresströmungen entstand?",
        answerA = "Atlantischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Pazifischer Ozean",
        answerD = "Arktischer Ozean",
        correctAnswer = 2, // C
        explanation = "Der Great Pacific Garbage Patch (Großer Pazifischer Müllfleck) ist eine riesige Ansammlung von Plastikmüll im Nordpazifik, gehalten von dem nordpazifischen Strömungswirbel (Gyre). Er ist etwa 1,6 Millionen km² groß.",
        difficulty = 2,
        funFact = "Das Great Pacific Garbage Patch ist kein fester Teppich, sondern eine unsichtbare Suppe aus Mikroplastik – kaum zu fotografieren, aber verheerend für Meerestiere."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Welthandelsorganisation (WTO) hat ihren Sitz in welcher Stadt?",
        answerA = "New York",
        answerB = "Brüssel",
        answerC = "Wien",
        answerD = "Genf",
        correctAnswer = 3, // D
        explanation = "Die Welthandelsorganisation (WTO) hat ihren Hauptsitz in Genf, Schweiz. Sie wurde 1995 gegründet und hat 164 Mitgliedsstaaten, die zusammen über 98 % des weltweiten Handels ausmachen.",
        difficulty = 2,
        funFact = null
    ),

    // --- Canal systems additional ---

    Question(
        categoryId = 1,
        questionText = "Welcher Kanal verbindet in China den Yangtze mit dem Huang He (Gelber Fluss) und gilt als ältester langer Kanal der Welt?",
        answerA = "Jinghang-Kanal (Großer Kanal)",
        answerB = "Ling-Kanal",
        answerC = "Li-Bai-Kanal",
        answerD = "Guangzhou-Schifffahrtskanal",
        correctAnswer = 0, // A
        explanation = "Der Große Kanal Chinas (Jinghang-Kanal) ist mit etwa 1.776 km der längste künstliche Kanal der Welt und verbindet Peking mit Hangzhou. Begonnen im 5. Jahrhundert v. Chr., war er jahrhundertelang Chinas wichtigste Handelsader.",
        difficulty = 2,
        funFact = "Der Große Kanal Chinas ist UNESCO-Welterbe und war neben der Seidenstraße die zweite große Handelsachse des chinesischen Reiches."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kanal in Kanada verbindet den Sankt-Lorenz-Strom mit dem Ontariosee und ermöglicht Seeschiffen das Binnenland zu erreichen?",
        answerA = "Rideau-Kanal",
        answerB = "Welland-Kanal",
        answerC = "Trent-Severn-Wasserstraße",
        answerD = "Lachine-Kanal",
        correctAnswer = 1, // B
        explanation = "Der Welland-Kanal (45 km) umgeht die Niagarafälle und verbindet den Eriesee mit dem Ontariosee. Er ist Teil des St.-Lorenz-Seewegs, der Hochseeschiffen ermöglicht, bis in das Herz Nordamerikas zu fahren.",
        difficulty = 2,
        funFact = "Ohne den Welland-Kanal würden die Niagarafälle den gesamten Schiffsverkehr zwischen den Großen Seen und dem Atlantik blockieren."
    ),

)


