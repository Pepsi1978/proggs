package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsHard(): List<Question> = listOf(

    // ── HARD (difficulty = 3) ── 50 questions ────────────────────────────────

    Question(
        categoryId = 1,
        questionText = "Welcher Staat ist eine Enklave, die vollständig von Italien umgeben ist?",
        answerA = "Monaco",
        answerB = "Vatikanstadt",
        answerC = "San Marino",
        answerD = "Liechtenstein",
        correctAnswer = 2,
        explanation = "San Marino ist vollständig von italienischem Staatsgebiet umgeben und gilt damit als echter Enklavenstaat. Es ist mit 61 km² einer der kleinsten Staaten der Welt.",
        difficulty = 3,
        funFact = "San Marino beansprucht, die älteste noch existierende Republik der Welt zu sein, gegründet angeblich im Jahr 301 n. Chr."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher tektonischen Platte liegt Island vollständig?",
        answerA = "Eurasischen und Nordamerikanischen Platte",
        answerB = "Nur auf der Nordamerikanischen Platte",
        answerC = "Nur auf der Eurasischen Platte",
        answerD = "Karibischen und Nordamerikanischen Platte",
        correctAnswer = 0,
        explanation = "Island sitzt direkt auf dem Mittelatlantischen Rücken und wird von beiden Platten – der Eurasischen und der Nordamerikanischen – geteilt. Diese Spreizungszone driftet jährlich um etwa 2 cm auseinander.",
        difficulty = 3,
        funFact = "Im Thingvellir-Nationalpark kann man zwischen den beiden Kontinentalplatten hindurchlaufen – ein einzigartiges geologisches Erlebnis."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie hoch ist der Ojos del Salado, der höchste aktive Vulkan der Welt?",
        answerA = "5.822 m",
        answerB = "6.176 m",
        answerC = "6.893 m",
        answerD = "7.084 m",
        correctAnswer = 2,
        explanation = "Der Ojos del Salado an der Grenze zwischen Chile und Argentinien erreicht 6.893 m und ist damit nicht nur der höchste aktive Vulkan, sondern auch der zweithöchste Gipfel der Anden.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt war bis 1997 unter dem Namen Bombay bekannt?",
        answerA = "Chennai",
        answerB = "Kolkata",
        answerC = "Hyderabad",
        answerD = "Mumbai",
        correctAnswer = 3,
        explanation = "Die indische Metropole Mumbai hieß bis zu ihrer offiziellen Umbenennung 1995/1997 Bombay. Der Name Mumbai leitet sich von der lokalen Göttin Mumbadevi ab.",
        difficulty = 3,
        funFact = "Bombay war der englische Name, der von portugiesischen Kolonisatoren als 'Bom Bahia' (gute Bucht) geprägt wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Meeresgraben wurde 2012 der tiefste Punkt der Erde gemessen?",
        answerA = "Puerto-Rico-Graben",
        answerB = "Tonga-Graben",
        answerC = "Marianengraben",
        answerD = "Java-Graben",
        correctAnswer = 2,
        explanation = "Der Challenger Deep im Marianengraben im westlichen Pazifik ist mit etwa 10.935 m der tiefste bekannte Punkt der Erdoberfläche. James Cameron tauchte 2012 als dritter Mensch dorthin.",
        difficulty = 3,
        funFact = "Würde man den Mount Everest in den Marianengraben versenken, wäre der Gipfel noch über 2 km unter dem Meeresspiegel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebiet wird als trockenste Wüste der Welt bezeichnet?",
        answerA = "Gobi",
        answerB = "Sahara",
        answerC = "Atacama",
        answerD = "Namib",
        correctAnswer = 2,
        explanation = "Die Atacama-Wüste in Südamerika gilt als die trockenste Wüste der Welt. Einige Stationen messen durchschnittlich weniger als 1 mm Niederschlag pro Jahr.",
        difficulty = 3,
        funFact = "In Teilen der Atacama hat es möglicherweise Jahrhunderte lang nicht geregnet – dennoch lebt dort eine spezialisierte Tier- und Pflanzenwelt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat entstand 2011 als jüngster Staat der Welt?",
        answerA = "Kosovo",
        answerB = "Montenegro",
        answerC = "Südsudan",
        answerD = "Osttimor",
        correctAnswer = 2,
        explanation = "Südsudan wurde am 9. Juli 2011 nach einem Unabhängigkeitsreferendum vom Sudan unabhängig und ist damit der jüngste international anerkannte Staat der Welt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche geografische Koordinate entspricht dem südlichen Wendekreis?",
        answerA = "23° 26' südlicher Breite",
        answerB = "66° 33' südlicher Breite",
        answerC = "23° 26' nördlicher Breite",
        answerD = "30° 00' südlicher Breite",
        correctAnswer = 0,
        explanation = "Der Wendekreis des Steinbocks (Capricorn) liegt bei etwa 23° 26' südlicher Breite. Dies ist die südlichste Breite, bei der die Sonne im Zenit steht (zur Wintersonnenwende der Nordhalbkugel).",
        difficulty = 3,
        funFact = "Der genaue Wert des Wendekreises variiert leicht über Jahrzehnte aufgrund der Schwankung der Erdachsenneigung."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele Länder sind vollständig von nur einem anderen Staat umgeben (Enklaven)?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "5",
        correctAnswer = 1,
        explanation = "Weltweit gibt es drei echte Enklavenstaaten: Vatikanstadt (in Italien), San Marino (in Italien) und Lesotho (in Südafrika). Sie sind vollständig von einem einzigen anderen Staat umgeben.",
        difficulty = 3,
        funFact = "Eswatini (früher Swasiland) grenzt zwar nur an Südafrika und Mosambik, ist aber kein Enklavenstaat, da es an zwei Länder grenzt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozeangraben ist der tiefste im Atlantischen Ozean?",
        answerA = "Romanche-Graben",
        answerB = "Puerto-Rico-Graben",
        answerC = "Südatlantischer Rücken",
        answerD = "Cayman-Graben",
        correctAnswer = 1,
        explanation = "Der Puerto-Rico-Graben ist mit einer maximalen Tiefe von etwa 8.376 m (Milwaukee Deep) der tiefste Punkt des Atlantischen Ozeans und liegt nördlich von Puerto Rico.",
        difficulty = 3,
        funFact = "Der Puerto-Rico-Graben entstand an einer komplexen Subduktionszone und ist tektonisch sehr aktiv."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss bildet die Grenze zwischen Russland und China im Fernen Osten?",
        answerA = "Lena",
        answerB = "Jenissei",
        answerC = "Amur",
        answerD = "Ob",
        correctAnswer = 2,
        explanation = "Der Amur (auf Chinesisch Heilong Jiang, 'Schwarzer Drachenfluss') bildet auf einer Länge von über 3.000 km die natürliche Grenze zwischen Russland und China.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Inselgruppe liegt der kälteste dauerhaft bewohnte Ort der Welt, Oymyakon?",
        answerA = "Auf dem sibirischen Festland",
        answerB = "Auf der Insel Sachalin",
        answerC = "Auf der Neusibirischen Inselgruppe",
        answerD = "Auf der Halbinsel Kamtschatka",
        correctAnswer = 0,
        explanation = "Oymyakon liegt im russischen Jakutien auf dem sibirischen Festland, nicht auf einer Insel. Mit einer Rekordtemperatur von -67,7°C ist es der kälteste dauerhaft bewohnte Ort der Welt.",
        difficulty = 3,
        funFact = "Der Name Oymyakon bedeutet in der Jakutischen Sprache in etwa 'nicht zugefrierendes Wasser', wegen einer warmen Quelle in der Nähe."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße verbindet das Rote Meer mit dem Golf von Aden?",
        answerA = "Straße von Hormuz",
        answerB = "Straße von Malakka",
        answerC = "Bab al-Mandab",
        answerD = "Straße von Gibraltar",
        correctAnswer = 2,
        explanation = "Die Meerenge Bab al-Mandab (arabisch: 'Tor der Tränen') verbindet das Rote Meer mit dem Golf von Aden und ist eine der wichtigsten Schifffahrtsrouten der Welt.",
        difficulty = 3,
        funFact = "Täglich passieren rund 4 Millionen Barrel Öl die Meerenge Bab al-Mandab, was sie zu einem geopolitisch sensiblen Engpass macht."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Zeitzonen der Welt?",
        answerA = "Russland",
        answerB = "USA",
        answerC = "Kanada",
        answerD = "Frankreich",
        correctAnswer = 3,
        explanation = "Frankreich hat offiziell 12 Zeitzonen, da es zahlreiche Überseegebiete und Territorien weltweit besitzt. Russland hat 11 und die USA haben 11 Zeitzonen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt der geografische Mittelpunkt der Erde (gerechnet nach Landfläche)?",
        answerA = "Europa",
        answerB = "Asien",
        answerC = "Afrika",
        answerD = "Nordamerika",
        correctAnswer = 1,
        explanation = "Je nach Berechnungsmethode liegt der geografische Mittelpunkt der Landmasse der Erde in Asien, oft wird eine Region in Kasachstan oder der Türkei genannt.",
        difficulty = 3,
        funFact = "Mehrere Länder beanspruchen den 'Mittelpunkt Europas' für sich, darunter die Ukraine, Polen und Litauen – je nach Berechnungsmethode kommen verschiedene Punkte heraus."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vulkan auf der Erde hat den größten Basisdurchmesser?",
        answerA = "Mauna Loa (Hawaii)",
        answerB = "Mount Etna (Sizilien)",
        answerC = "Tamu Massif (Pazifik, Unterwasser)",
        answerD = "Kilimandscharo (Tansania)",
        correctAnswer = 2,
        explanation = "Das Tamu Massif ist ein erloschener Unterwasservulkan im Pazifik mit einem Durchmesser von etwa 650 km. Es gilt als eines der größten Vulkane im Sonnensystem.",
        difficulty = 3,
        funFact = "Trotz seiner enormen Ausmaße ist das Tamu Massif vergleichsweise flach – es ragt nur etwa 4.460 m über den Meeresboden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die geografische Grenze zwischen Europa und Asien?",
        answerA = "Kaukasus",
        answerB = "Ural",
        answerC = "Alpen",
        answerD = "Karpaten",
        correctAnswer = 1,
        explanation = "Der Ural wird traditionell als die Grenze zwischen Europa und Asien angesehen. Das Gebirge erstreckt sich über etwa 2.500 km von Nord nach Süd durch Russland.",
        difficulty = 3,
        funFact = "Die höchste Erhebung des Urals, der Narodnaja, erreicht nur 1.895 m – das Gebirge ist geologisch sehr alt und stark erodiert."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Welt, das an 14 andere Länder grenzt?",
        answerA = "Brasilien",
        answerB = "Russland",
        answerC = "China",
        answerD = "Deutschland",
        correctAnswer = 2,
        explanation = "China grenzt an 14 Staaten: Russland, Mongolei, Kasachstan, Kirgisistan, Tadschikistan, Afghanistan, Pakistan, Indien, Nepal, Bhutan, Myanmar, Laos, Vietnam und Nordkorea.",
        difficulty = 3,
        funFact = "Russland grenzt ebenfalls an 14 Länder, wenn man Nordkorea und Aserbaidschan mitzählt – beide Länder teilen sich diesen Rekord."
    ),

    Question(
        categoryId = 1,
        questionText = "Wo liegt das Danakilsenke, eine der heißesten und unwirtlichsten Regionen der Erde?",
        answerA = "In der Libyschen Wüste (Ägypten/Libyen)",
        answerB = "Im Rift Valley (Kenia/Tansania)",
        answerC = "Im Afar-Dreieck (Äthiopien/Eritrea/Dschibuti)",
        answerD = "In der Namib (Namibia/Angola)",
        correctAnswer = 2,
        explanation = "Die Danakilsenke im Afar-Dreieck liegt bis zu 116 m unter dem Meeresspiegel und verzeichnet Jahrestemperaturen von durchschnittlich über 34°C. Sie ist geologisch extrem aktiv.",
        difficulty = 3,
        funFact = "In der Danakilsenke gibt es Lava-Seen und farbige Thermalseen mit Schwefelsäure – eine der am stärksten vulkanisch geprägten Landschaften der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Inselkette wird als 'Ring of Fire'-Archipel mit über 7.000 Inseln bezeichnet?",
        answerA = "Indonesien",
        answerB = "Philippinen",
        answerC = "Japan",
        answerD = "Melanesien",
        correctAnswer = 1,
        explanation = "Die Philippinen bestehen aus 7.641 Inseln und liegen direkt im pazifischen Feuerring. Das Land ist eines der vulkanisch und seismisch aktivsten der Welt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das flächenmäßig größte Land der Welt ohne Meereszugang (Binnenstaat)?",
        answerA = "Mongolei",
        answerB = "Kasachstan",
        answerC = "Niger",
        answerD = "Tschad",
        correctAnswer = 1,
        explanation = "Kasachstan ist mit einer Fläche von etwa 2,7 Millionen km² der größte Binnenstaat der Welt, obwohl es am Kaspischen See liegt – der jedoch ein Binnensee ist.",
        difficulty = 3,
        funFact = "Kasachstan ist das neuntgrößte Land der Welt und trotzdem hat es keinen Zugang zu einem Weltmeer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Namen trug der heutige Staat Simbabwe bis 1980?",
        answerA = "Rhodesien",
        answerB = "Nyassaland",
        answerC = "Bechuanaland",
        answerD = "Basutoland",
        correctAnswer = 0,
        explanation = "Das heutige Simbabwe hieß bis zur Unabhängigkeit 1980 Rhodesien (offiziell Simbabwe Rhodesien). Der Name ehrte den britischen Kolonisator Cecil Rhodes.",
        difficulty = 3,
        funFact = "Der neue Name Simbabwe leitet sich von den Ruinen von Great Zimbabwe ab, einer mittelalterlichen Steinstadt, die als bedeutendstes archäologisches Erbe Subsahara-Afrikas gilt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Atoll gilt als flächenmäßig größtes der Welt?",
        answerA = "Bikini-Atoll (Marshallinseln)",
        answerB = "Lifou (Neukaledonien)",
        answerC = "Kiritimati / Christmas Island (Kiribati)",
        answerD = "Rangiroa (Französisch-Polynesien)",
        correctAnswer = 2,
        explanation = "Kiritimati (Christmas Island) in Kiribati ist mit einer Landfläche von etwa 388 km² das flächenmäßig größte Atoll der Welt und liegt im zentralen Pazifik.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent befindet sich der größte Fjord der Welt?",
        answerA = "Nordamerika (Kanada)",
        answerB = "Europa (Norwegen)",
        answerC = "Asien (Russland)",
        answerD = "Nordamerika (Grönland)",
        correctAnswer = 3,
        explanation = "Der Scoresby Sund in Grönland ist mit einer Länge von über 350 km der größte Fjord der Welt. Grönland gehört geographisch zu Nordamerika und politisch zu Dänemark.",
        difficulty = 3,
        funFact = "Der Sognefjord in Norwegen ist der tiefste Fjord der Welt mit über 1.300 m Tiefe, aber nicht der längste."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche tektonische Platte trägt den größten Teil des Indischen Ozeans?",
        answerA = "Pazifische Platte",
        answerB = "Antarktische Platte",
        answerC = "Indo-Australische Platte",
        answerD = "Afrikanische Platte",
        correctAnswer = 2,
        explanation = "Die Indo-Australische Platte (manchmal in Indische und Australische Platte unterteilt) trägt den größten Teil des Indischen Ozeans sowie den indischen Subkontinent und Australien.",
        difficulty = 3,
        funFact = "Die Indo-Australische Platte bewegt sich mit etwa 5–6 cm pro Jahr nordwärts – eine der schnellsten tektonischen Bewegungen überhaupt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der Gebirgszug, der Norwegen und Schweden voneinander trennt?",
        answerA = "Kjølen (Skandinavische Gebirge)",
        answerB = "Jotunheimen",
        answerC = "Dovre-Gebirge",
        answerD = "Hardangervidda",
        correctAnswer = 0,
        explanation = "Der Kjølen (Kjølen-Gebirge, auch Skandinavische Gebirge oder Kölen genannt) bildet die natürliche Grenze zwischen Norwegen und Schweden und erstreckt sich über rund 1.700 km.",
        difficulty = 3,
        funFact = "Das Kjølen-Gebirge bestimmt das Klima beider Länder stark: Die Westseite (Norwegen) erhält sehr viel Niederschlag, während Schweden im Regenschatten liegt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land ohne jeglichen Meereszugang?",
        answerA = "Äthiopien",
        answerB = "Mongolei",
        answerC = "Kasachstan",
        answerD = "Usbekistan",
        correctAnswer = 0,
        explanation = "Äthiopien hat mit über 120 Millionen Einwohnern die größte Bevölkerung unter allen Binnenstaaten. Es verlor seinen Meereszugang nach der Unabhängigkeit Eritreas 1993.",
        difficulty = 3,
        funFact = "Äthiopien ist auch eines der wenigen Länder, das ein eigenes Kalendersystem verwendet – der äthiopische Kalender ist dem gregorianischen Kalender etwa 7–8 Jahre hinterher."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gletscher ist der größte außerhalb der Polarregionen?",
        answerA = "Aletschgletscher (Schweiz)",
        answerB = "Fedtschenko-Gletscher (Tadschikistan)",
        answerC = "Siachen-Gletscher (Kaschmir)",
        answerD = "Hubbard-Gletscher (Alaska)",
        correctAnswer = 1,
        explanation = "Der Fedtschenko-Gletscher in Tadschikistan ist mit einer Länge von etwa 77 km und einer Fläche von rund 700 km² der größte Gletscher außerhalb der Polargebiete.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Titicacasee, der höchstgelegene schiffbare See der Welt?",
        answerA = "Nur in Peru",
        answerB = "Nur in Bolivien",
        answerC = "An der Grenze zwischen Peru und Bolivien",
        answerD = "An der Grenze zwischen Chile und Bolivien",
        correctAnswer = 2,
        explanation = "Der Titicacasee liegt auf einer Höhe von 3.812 m und bildet die Grenze zwischen Peru und Bolivien. Er ist der größte See Südamerikas nach Oberfläche.",
        difficulty = 3,
        funFact = "Im Titicacasee leben schwimmende Inseln aus Totora-Schilf, auf denen seit Jahrhunderten die Uros-Gemeinschaft lebt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der tiefste See der Erde?",
        answerA = "Tanganjikasee",
        answerB = "Kaspisches Meer",
        answerC = "Baikalsee",
        answerD = "Victoriasee",
        correctAnswer = 2,
        explanation = "Der Baikalsee in Sibirien ist mit einer maximalen Tiefe von 1.637 m der tiefste See der Welt. Er enthält etwa 20 % des gesamten flüssigen Süßwassers der Erde.",
        difficulty = 3,
        funFact = "Der Baikalsee ist so alt (25–30 Millionen Jahre) und so tief, dass er eine einzigartige Fauna beherbergt – über 80 % der Tierarten sind endemisch."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat keinen aktiven Vulkan auf seinem Festland?",
        answerA = "Nordamerika",
        answerB = "Australien",
        answerC = "Südamerika",
        answerD = "Afrika",
        correctAnswer = 1,
        explanation = "Australien ist der einzige Kontinent ohne aktive Vulkane auf seinem Festland. Das Land liegt mitten auf der Indo-Australischen Platte, weit entfernt von tektonischen Plattengrenzen.",
        difficulty = 3,
        funFact = "Obwohl das australische Festland vulkanisch inaktiv ist, gibt es auf dem australischen Territorium Heard Island im Indischen Ozean einen sehr aktiven Vulkan."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Halbinsel teilt sich das Rote Meer von dem Golf von Aden?",
        answerA = "Arabische Halbinsel",
        answerB = "Somali-Halbinsel (Horn von Afrika)",
        answerC = "Sinai-Halbinsel",
        answerD = "Kap-Halbinsel",
        correctAnswer = 1,
        explanation = "Das Horn von Afrika (Somali-Halbinsel) trennt den Golf von Aden im Norden vom Indischen Ozean im Süden. Der Bab al-Mandab liegt zwischen dieser Halbinsel und der arabischen Küste.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge trennt Europa von Asien im Bereich des Kaukasus?",
        answerA = "Pontisches Gebirge",
        answerB = "Taurus",
        answerC = "Großer Kaukasus",
        answerD = "Kleiner Kaukasus",
        correctAnswer = 2,
        explanation = "Der Große Kaukasus (Hauptkaukasische Gebirgskette) bildet die Grenze zwischen Europa und Asien im Kaukasusraum. Der höchste Gipfel, der Elbrus (5.642 m), ist damit auch der höchste Berg Europas.",
        difficulty = 3,
        funFact = "Die Frage, ob der Elbrus zum europäischen oder asiatischen Teil des Kaukasus gehört, ist unter Geographen noch immer umstritten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean hat die geringste durchschnittliche Tiefe?",
        answerA = "Arktischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Atlantischer Ozean",
        answerD = "Pazifischer Ozean",
        correctAnswer = 0,
        explanation = "Der Arktische Ozean hat mit einer durchschnittlichen Tiefe von etwa 1.038 m die geringste Durchschnittstiefe aller Ozeane. Er ist auch der kleinste und flachste der fünf Weltmeere.",
        difficulty = 3,
        funFact = "Im Sommer schmelzen bis zu 50 % der arktischen Meereisdecke – durch den Klimawandel nimmt diese Fläche stetig ab."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der aktive Vulkan auf dem Territorium Italiens, der als der häufigste eruptierende Vulkan Europas gilt?",
        answerA = "Vesuv",
        answerB = "Ätna",
        answerC = "Stromboli",
        answerD = "Vulcano",
        correctAnswer = 2,
        explanation = "Der Stromboli auf der gleichnamigen Liparischen Insel ist seit mindestens 2.000 Jahren nahezu ununterbrochen aktiv und wird deshalb auch 'Leuchtturm des Mittelmeers' genannt.",
        difficulty = 3,
        funFact = "Jules Verne ließ seine Figuren in 'Reise zum Mittelpunkt der Erde' durch den Stromboli aus der Erde herauskatapultieren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat ist der einzige in der Welt, der doppelt eingeschlossen ist (von einem anderen Binnenstaat umgeben)?",
        answerA = "Vatikanstadt",
        answerB = "San Marino",
        answerC = "Lesotho",
        answerD = "Liechtenstein",
        correctAnswer = 3,
        explanation = "Liechtenstein ist doppelt eingeschlossen: Es grenzt ausschließlich an die Binnenstaaten Österreich und die Schweiz, die beide keinen direkten Meereszugang haben.",
        difficulty = 3,
        funFact = "Uzbekistan ist das zweite doubly landlocked country der Welt – es grenzt nur an andere Binnenstaaten (Kasachstan, Kirgisistan, Tadschikistan, Afghanistan, Turkmenistan)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss hat das größte Einzugsgebiet der Welt?",
        answerA = "Nil",
        answerB = "Yangtse",
        answerC = "Amazonas",
        answerD = "Kongo",
        correctAnswer = 2,
        explanation = "Der Amazonas hat mit einem Einzugsgebiet von etwa 7,05 Millionen km² das größte aller Flusssysteme der Welt. Er führt mehr Wasser als die sieben nächstgrößeren Flüsse zusammen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land gibt es das Salzseeplateau Salar de Uyuni, das größte Salzsee der Welt?",
        answerA = "Chile",
        answerB = "Argentinien",
        answerC = "Bolivien",
        answerD = "Peru",
        correctAnswer = 2,
        explanation = "Der Salar de Uyuni in Bolivien ist mit einer Fläche von über 10.000 km² der größte Salzsee der Welt. Er liegt auf einer Höhe von etwa 3.656 m im Altiplano.",
        difficulty = 3,
        funFact = "Der Salar de Uyuni enthält schätzungsweise 50–70 % der weltweiten Lithiumreserven – ein entscheidender Rohstoff für Batterietechnologie."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße trennt die Kontinente Nordamerika und Asien?",
        answerA = "Drakepassage",
        answerB = "Beringsstraße",
        answerC = "Hudson Bay",
        answerD = "Lancaster Sound",
        correctAnswer = 1,
        explanation = "Die Beringsstraße zwischen Alaska (USA) und Chukotka (Russland) trennt die Kontinente Nordamerika und Asien. An ihrer schmalsten Stelle ist sie nur etwa 85 km breit.",
        difficulty = 3,
        funFact = "Während der letzten Eiszeit war die Beringsstraße trocken – die sogenannte Beringia-Landbrücke ermöglichte die Besiedlung Amerikas durch Menschen aus Asien."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge auf dem Balkan ist das höchste der Region?",
        answerA = "Pindos-Gebirge (Griechenland)",
        answerB = "Dinarisches Gebirge (Westbalkan)",
        answerC = "Rila (Bulgarien)",
        answerD = "Balkangebirge (Bulgarien)",
        correctAnswer = 2,
        explanation = "Das Rila-Gebirge in Bulgarien ist das höchste Gebirge der Balkanhalbinsel. Der Musala-Gipfel erreicht 2.925 m und ist damit der höchste Punkt Bulgariens und des gesamten Balkans.",
        difficulty = 3,
        funFact = "Im Rila-Gebirge befindet sich das berühmte Rila-Kloster, ein UNESCO-Weltkulturerbe und nationales Symbol Bulgariens."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Name wurde 1964 aus der Vereinigung von Tanganjika und Sansibar gebildet?",
        answerA = "Kenia",
        answerB = "Uganda",
        answerC = "Tansania",
        answerD = "Mosambik",
        correctAnswer = 2,
        explanation = "Am 26. April 1964 vereinigten sich die Republik Tanganjika und die Volksrepublik Sansibar zum neuen Staat Tansania – der Name ist eine Kombination aus beiden Vorgängerstaaten.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent liegt vollständig auf der südlichen Hemisphäre?",
        answerA = "Afrika",
        answerB = "Australien",
        answerC = "Südamerika",
        answerD = "Antarktis",
        correctAnswer = 3,
        explanation = "Nur die Antarktis liegt vollständig auf der südlichen Hemisphäre. Australien und Südamerika haben Teile im Norden des Äquators (z.B. der nördlichste Punkt Australiens liegt nahe dem Äquator).",
        difficulty = 3,
        funFact = "Australien gilt oft als vollständig südlich des Äquators, aber der Äquator liegt nördlich von Australiens Nordküste – formal korrekt ist Australien südlich des Äquators."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat hat die längste Landgrenze der Welt?",
        answerA = "China",
        answerB = "Brasilien",
        answerC = "USA",
        answerD = "Russland",
        correctAnswer = 3,
        explanation = "Russland hat mit einer Gesamtlandgrenzlänge von über 22.000 km die längste Landgrenze aller Staaten der Welt. Es grenzt an 14 verschiedene Länder.",
        difficulty = 3,
        funFact = "Chinas Landgrenzen sind mit etwa 22.000 km ähnlich lang und ebenfalls ein Weltrekordhalter-Kandidat – beide Länder teilen sich faktisch den ersten Platz."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Inselgruppe gehört staatsrechtlich zu Dänemark, liegt aber geografisch bei Nordamerika?",
        answerA = "Färöer-Inseln",
        answerB = "Svalbard",
        answerC = "Grönland",
        answerD = "Jan Mayen",
        correctAnswer = 2,
        explanation = "Grönland ist die weltgrößte Insel (wenn man Australien als Kontinent zählt) und gehört politisch zu Dänemark, liegt aber geografisch auf der Nordamerikanischen Platte.",
        difficulty = 3,
        funFact = "Grönland erhielt 1979 Heimregel und 2009 Selbstverwaltung von Dänemark – ein Unabhängigkeitsreferendum könnte in Zukunft folgen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gewässer trennt Europa von Afrika an seiner schmalsten Stelle?",
        answerA = "Sizilianischer Kanal",
        answerB = "Straße von Gibraltar",
        answerC = "Straße von Messina",
        answerD = "Suezkanal",
        correctAnswer = 1,
        explanation = "Die Straße von Gibraltar ist an ihrer schmalsten Stelle nur etwa 14 km breit und trennt die Iberische Halbinsel (Europa) von Marokko (Afrika).",
        difficulty = 3,
        funFact = "An der Straße von Gibraltar treffen Atlantik und Mittelmeer aufeinander – wegen unterschiedlicher Salinität und Temperatur fließen diese Wassermassen nebeneinander, ohne sich vollständig zu vermischen."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent befindet sich das größte Flussdelta der Welt?",
        answerA = "Südamerika (Amazonas-Delta)",
        answerB = "Asien (Ganges-Brahmaputra-Delta)",
        answerC = "Nordamerika (Mississippi-Delta)",
        answerD = "Afrika (Niger-Delta)",
        correctAnswer = 1,
        explanation = "Das Ganges-Brahmaputra-Delta (Sundarbans) in Bangladesch und Indien ist mit einer Fläche von über 100.000 km² das größte Flussdelta der Welt.",
        difficulty = 3,
        funFact = "Das Sundarbans-Delta beherbergt den größten Mangrovenwald der Welt und ist Heimat der Bengalischen Tiger."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vulkan in Indonesien verursachte 1883 eine der stärksten Explosionen der aufgezeichneten Geschichte?",
        answerA = "Merapi",
        answerB = "Tambora",
        answerC = "Krakatau",
        answerD = "Rinjani",
        correctAnswer = 2,
        explanation = "Der Ausbruch des Krakatau im Jahr 1883 war so gewaltig, dass der Knall noch über 4.800 km weit gehört wurde. Er verursachte einen globalen Klimaeffekt mit sinkenden Temperaturen.",
        difficulty = 3,
        funFact = "Der Vulkanausbruch des Tambora 1815 war noch stärker als der Krakatau und verursachte das 'Jahr ohne Sommer' 1816 in Europa und Nordamerika."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche geografische Besonderheit hat das Tote Meer?",
        answerA = "Es ist der salzreichste See überhaupt",
        answerB = "Es ist der tiefste Punkt der Erdlandoberfläche",
        answerC = "Es hat die höchste Wasserdichte aller Seen",
        answerD = "Es ist der heißeste dauerhaft existierende See",
        correctAnswer = 1,
        explanation = "Das Tote Meer liegt etwa 430 m unter dem Meeresspiegel und ist damit der tiefste Punkt der Erdoberfläche, der nicht unter Wasser liegt (tiefster Punkt auf Land).",
        difficulty = 3,
        funFact = "Der Salzgehalt des Toten Meeres beträgt etwa 28–35 % – etwa zehnmal höher als der normale Ozean. Menschen treiben dort mühelos ohne Schwimmen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat offiziell zwei Hauptstädte: eine administrative und eine gesetzgebende?",
        answerA = "Bolivien",
        answerB = "Niederlande",
        answerC = "Malaysia",
        answerD = "Südafrika",
        correctAnswer = 3,
        explanation = "Südafrika hat drei Hauptstädte: Pretoria (Exekutive), Kapstadt (Legislative) und Bloemfontein (Judikative). Damit ist es einzigartig in seiner Drei-Hauptstädte-Struktur.",
        difficulty = 3,
        funFact = "Bolivien hat ebenfalls zwei Hauptstädte: Sucre (formelle Hauptstadt und Sitz des Obersten Gerichts) und La Paz (Regierungssitz)."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Breitengrad liegt der Polarkreis?",
        answerA = "66° 34'",
        answerB = "64° 47'",
        answerC = "68° 12'",
        answerD = "62° 23'",
        correctAnswer = 0,
        explanation = "Der Nördliche Polarkreis liegt bei etwa 66° 33' 48'' nördlicher Breite (oft als 66° 34' angegeben). Nördlich davon gibt es Mitternachtssonne und Polarnacht.",
        difficulty = 3,
        funFact = "Der genaue Wert des Polarkreises verändert sich leicht über die Jahrhunderte, da sich die Neigung der Erdachse in einem 41.000-Jahres-Zyklus zwischen 22,1° und 24,5° verändert."
    ),

    // ── HARD (difficulty = 3) ── 50 additional questions ─────────────────────

    Question(
        categoryId = 1,
        questionText = "Welches ist der einzige Staat der Welt, der vollständig innerhalb der Hauptstadt eines anderen Staates liegt?",
        answerA = "San Marino",
        answerB = "Monaco",
        answerC = "Vatikanstadt",
        answerD = "Liechtenstein",
        correctAnswer = 2,
        explanation = "Vatikanstadt liegt vollständig innerhalb der Stadt Rom und ist damit weltweit einzigartig: Ein souveräner Staat, der sich innerhalb der Hauptstadt eines anderen Staates befindet.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief ist der Tonga-Graben, der zweittiefste Meeresgraben der Erde?",
        answerA = "9.121 m",
        answerB = "10.882 m",
        answerC = "8.376 m",
        answerD = "9.780 m",
        correctAnswer = 1,
        explanation = "Der Tonga-Graben im südwestlichen Pazifik erreicht eine Tiefe von etwa 10.882 m und ist damit nach dem Challenger Deep im Marianengraben die zweittiefste Stelle des Weltmeeres.",
        difficulty = 3,
        funFact = "Im Tonga-Graben sinkt die Pazifische Platte mit bis zu 24 cm pro Jahr unter die Australische Platte – eine der schnellsten Subduktionsraten der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche tektonische Platte kollidiert mit der Eurasischen Platte und hat dadurch den Himalaya entstehen lassen?",
        answerA = "Arabische Platte",
        answerB = "Indische Platte",
        answerC = "Afrikanische Platte",
        answerD = "Somali-Platte",
        correctAnswer = 1,
        explanation = "Die Indische Platte kollidiert seit etwa 50 Millionen Jahren mit der Eurasischen Platte. Diese Kontinent-Kontinent-Kollision schuf den Himalaya und das Tibetische Hochplateau.",
        difficulty = 3,
        funFact = "Der Himalaya wächst noch immer um etwa 5 mm pro Jahr, da die Kollision der Platten anhält."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Höhe liegt der Gipfel des Kangchendzönga, des drittgrößten Berges der Welt?",
        answerA = "8.516 m",
        answerB = "8.611 m",
        answerC = "8.586 m",
        answerD = "8.849 m",
        correctAnswer = 2,
        explanation = "Der Kangchendzönga liegt an der Grenze zwischen Nepal und Sikkim (Indien) und ist mit 8.586 m der drittgrößte Berg der Welt. Der K2 ist mit 8.611 m der zweithöchste.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land Südamerikas grenzt als einziges nicht an Brasilien?",
        answerA = "Guyana",
        answerB = "Suriname",
        answerC = "Ecuador",
        answerD = "Uruguay",
        correctAnswer = 2,
        explanation = "Ecuador und Chile sind die einzigen südamerikanischen Länder, die keine gemeinsame Grenze mit Brasilien haben. Von diesen beiden ist Ecuador das bekanntere Beispiel, da Chile im Süden liegt.",
        difficulty = 3,
        funFact = "Brasilien grenzt an zehn der zwölf südamerikanischen Länder – nur Ecuador und Chile teilen keine Grenze mit ihm."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich der Java-Graben (Sundagraben), der tiefste Punkt des Indischen Ozeans?",
        answerA = "Er liegt zwischen Indien und Sri Lanka",
        answerB = "Er liegt südlich von Java (Indonesien)",
        answerC = "Er liegt westlich von Australien",
        answerD = "Er liegt östlich von Madagaskar",
        correctAnswer = 1,
        explanation = "Der Sundagraben (Java-Graben) erstreckt sich südlich der indonesischen Insel Java und erreicht eine Tiefe von etwa 7.290 m. Er ist der tiefste Punkt des Indischen Ozeans.",
        difficulty = 3,
        funFact = "Der Sundagraben ist geologisch die Ursache für viele verheerende Erdbeben und Tsunamis in der Region, darunter den Tsunami von 2004."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das trockenste eisfreie Gebiet der Erde?",
        answerA = "Zentrale Sahara (Algerien)",
        answerB = "Atacama-Hochland (Chile)",
        answerC = "McMurdo-Trockentäler (Antarktis)",
        answerD = "Rub al-Khali (Saudi-Arabien)",
        correctAnswer = 2,
        explanation = "Die McMurdo-Trockentäler in der Antarktis gelten als trockenste eisfreie Regionen der Erde – dort fiel seit Millionen von Jahren kein nennenswerter Niederschlag. Sie sind eisfrei und extrem kalt.",
        difficulty = 3,
        funFact = "Die Trockentäler haben eine ähnliche Umgebung wie der Mars und werden von der NASA für Marsforschungen genutzt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt das umstrittene Territorium zwischen Indien und Pakistan, das Teil des ehemaligen Fürstentums ist?",
        answerA = "Aksai Chin",
        answerB = "Gilgit-Baltistan",
        answerC = "Jammu und Kaschmir",
        answerD = "Line of Control",
        correctAnswer = 2,
        explanation = "Jammu und Kaschmir ist das zentrale umstrittene Territorium zwischen Indien und Pakistan seit der Teilung Britisch-Indiens 1947. Beide Staaten beanspruchen die gesamte Region.",
        difficulty = 3,
        funFact = "China kontrolliert zusätzlich den Ostteil Aksai Chin, den Indien ebenfalls beansprucht – der Konflikt ist damit dreidimensional."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozeangraben im Südatlantik gehört zu den tiefsten der südlichen Hemisphäre?",
        answerA = "Romanche-Graben",
        answerB = "South Sandwich-Graben",
        answerC = "Meteor-Tiefe",
        answerD = "Walvis-Rücken",
        correctAnswer = 1,
        explanation = "Der South Sandwich-Graben im Südatlantik erreicht eine Tiefe von etwa 8.428 m und ist damit einer der tiefsten Gräben der südlichen Hemisphäre, entstanden durch Subduktion der Südamerikanischen unter die Sandwich-Platte.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat den höchsten Anteil bewaldeter Fläche (über 90 % Waldbedeckung)?",
        answerA = "Finnland",
        answerB = "Suriname",
        answerC = "Gabun",
        answerD = "Papua-Neuguinea",
        correctAnswer = 1,
        explanation = "Suriname in Südamerika hat mit über 93–96 % Waldbedeckung den höchsten Waldanteil aller Staaten der Welt. Die dichten Amazonas-Regenwälder bedecken nahezu das gesamte Territorium.",
        difficulty = 3,
        funFact = "Suriname ist zugleich das kleinste Land Südamerikas und eines der am dünnsten besiedelten Länder der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche zwei tektonischen Platten bewegen sich am San-Andreas-Graben in Kalifornien aneinander vorbei?",
        answerA = "Pazifische Platte und Nordamerikanische Platte",
        answerB = "Juan-de-Fuca-Platte und Nordamerikanische Platte",
        answerC = "Karibische Platte und Nordamerikanische Platte",
        answerD = "Nazca-Platte und Nordamerikanische Platte",
        correctAnswer = 0,
        explanation = "Der San-Andreas-Graben ist eine Transformstörung zwischen der Pazifischen Platte und der Nordamerikanischen Platte. Die Platten gleiten horizontal aneinander vorbei mit ca. 5–6 cm pro Jahr.",
        difficulty = 3,
        funFact = "Los Angeles liegt auf der Pazifischen Platte und bewegt sich jährlich etwa 6 cm nordwärts auf San Francisco zu – in ca. 15 Millionen Jahren werden die Städte nebeneinanderliegen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Hochplateau wird als 'Dach der Welt' bezeichnet und hat eine durchschnittliche Höhe von über 4.500 m?",
        answerA = "Dekkan-Hochland",
        answerB = "Anatolisches Plateau",
        answerC = "Tibetisches Plateau",
        answerD = "Bolivianisches Altiplano",
        correctAnswer = 2,
        explanation = "Das Tibetische Plateau in Zentralasien mit einer durchschnittlichen Höhe von über 4.500 m ü.NN ist das höchste und größte Hochplateau der Erde und wird deshalb 'Dach der Welt' genannt.",
        difficulty = 3,
        funFact = "Das Tibetische Plateau entstand durch die Kollision der Indischen mit der Eurasischen Platte und wurde in den letzten 50 Millionen Jahren um mehrere Kilometer angehoben."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche vulkanische Inselkette im Nordpazifik verbindet Alaska mit der russischen Küste?",
        answerA = "Kurilen-Inseln",
        answerB = "Aleuten",
        answerC = "Kommandeur-Inseln",
        answerD = "Pribilof-Inseln",
        correctAnswer = 1,
        explanation = "Die Aleuten sind eine vulkanische Inselkette, die sich über rund 1.900 km vom Alaska-Halbinsel bis nahe an Kamtschatka (Russland) erstreckt und einen natürlichen Bogen im Nordpazifik bildet.",
        difficulty = 3,
        funFact = "Die Aleuten bestehen aus über 300 kleinen Inseln und liegen auf dem Pazifischen Feuerring – fast jede Insel besitzt mindestens einen Vulkan."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land Afrikas, das vollständig von einem anderen afrikanischen Staat umschlossen ist?",
        answerA = "Swasiland (Eswatini)",
        answerB = "Lesotho",
        answerC = "Dschibuti",
        answerD = "Burundi",
        correctAnswer = 1,
        explanation = "Lesotho ist ein souveräner Staat, der vollständig von Südafrika umschlossen ist und damit der einzige Enklavenstaat auf dem afrikanischen Kontinent.",
        difficulty = 3,
        funFact = "Lesotho liegt vollständig über 1.000 m Höhe – kein anderes Land der Welt hat eine so hohe minimale Elevation."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie hoch ist der Uhuru Peak, der Gipfel des Kilimandscharo?",
        answerA = "5.199 m",
        answerB = "4.550 m",
        answerC = "5.895 m",
        answerD = "4.321 m",
        correctAnswer = 2,
        explanation = "Der Kilimandscharo in Tansania erreicht mit seinem Gipfel Uhuru Peak 5.895 m über dem Meeresspiegel und ist damit der höchste Berg Afrikas und zugleich der höchste freistehende Berg der Erde.",
        difficulty = 3,
        funFact = "Trotz seiner Lage nahe dem Äquator trägt der Kilimandscharo eine permanente Schnee- und Eiskappe – diese schmilzt jedoch durch den Klimawandel rapide ab."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche zwei Gewässer verbindet der Panamakanal?",
        answerA = "Atlantischer Ozean und Golf von Mexiko",
        answerB = "Karibisches Meer und Pazifischer Ozean",
        answerC = "Golf von Mexiko und Karibisches Meer",
        answerD = "Atlantischer und Indischer Ozean",
        correctAnswer = 1,
        explanation = "Der Panamakanal verbindet das Karibische Meer (Atlantikseite) mit dem Pazifischen Ozean. Er wurde 1914 eröffnet und ermöglicht Schiffen, die lange Fahrt um Kap Hoorn zu umgehen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die tiefste Landsenke der westlichen Hemisphäre?",
        answerA = "Salar de Atacama",
        answerB = "Gran Chaco",
        answerC = "Laguna del Carbón (Argentinien)",
        answerD = "Pampa",
        correctAnswer = 2,
        explanation = "Die Laguna del Carbón in der Patagonischen Steppe Argentiniens liegt 105 m unter dem Meeresspiegel und ist damit die tiefste Landsenke Südamerikas und der westlichen Hemisphäre.",
        difficulty = 3,
        funFact = "Die Laguna del Carbón ist ein Salzsee, dessen Wasser durch Verdunstung so konzentriert ist, dass der Boden weiß erscheint."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat beansprucht die Falklandinseln (Islas Malvinas) als sein Staatsgebiet?",
        answerA = "Chile",
        answerB = "Uruguay",
        answerC = "Argentinien",
        answerD = "Brasilien",
        correctAnswer = 2,
        explanation = "Argentinien beansprucht die Falklandinseln als 'Islas Malvinas' als Teil seines Staatsgebietes. 1982 führte dieser Konflikt zum Falklandkrieg zwischen Argentinien und Großbritannien.",
        difficulty = 3,
        funFact = "Die Falklandinseln befinden sich trotz ihrer Lage im Südatlantik seit 1833 unter britischer Kontrolle – ein Überbleibsel des kolonialen Zeitalters."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt die Meeresstraße, die Neuseelands Nord- und Südinsel trennt?",
        answerA = "Cook-Straße",
        answerB = "Bass-Straße",
        answerC = "Tasman-Straße",
        answerD = "Foveaux-Straße",
        correctAnswer = 0,
        explanation = "Die Cook-Straße (Cook Strait) trennt Neuseelands Nord- und Südinsel und ist an ihrer schmalsten Stelle nur ca. 22 km breit. Sie ist nach dem Entdecker James Cook benannt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt Nanga Parbat, der als 'Killsberg' bekannte Achttausender?",
        answerA = "Indien",
        answerB = "Nepal",
        answerC = "Pakistan",
        answerD = "China",
        correctAnswer = 2,
        explanation = "Der Nanga Parbat (8.126 m) liegt im pakistanischen Teil Kaschmirs im Westhimalaya. Er gilt als einer der gefährlichsten Achttausender und forderte historisch besonders viele Opfer.",
        difficulty = 3,
        funFact = "Nanga Parbat bedeutet in Sanskrit 'nackter Berg'. Die Südwand (Rupalwand) ist mit über 4.600 m Höhendifferenz die größte zusammenhängende Felswand der Erde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das am dünnsten besiedelte bewohnte Territorium der Welt?",
        answerA = "Mongolei",
        answerB = "Namibia",
        answerC = "Australien",
        answerD = "Grönland",
        correctAnswer = 3,
        explanation = "Grönland hat mit weniger als 0,03 Einwohnern pro km² die geringste Bevölkerungsdichte der Welt. Das liegt an seiner enormen Fläche (2,1 Mio km²) bei nur rund 57.000 Einwohnern.",
        difficulty = 3,
        funFact = "Die Mongolei gilt oft als das am dünnsten besiedelte unabhängige Land, aber Grönland (als autonomes Territorium Dänemarks) übertrifft sie deutlich."
    ),

    Question(
        categoryId = 1,
        questionText = "Durch welche Eckpunkte wird das Bermuda-Dreieck definiert?",
        answerA = "Bermuda, Kuba und Puerto Rico",
        answerB = "Bermuda, Miami (Florida) und Puerto Rico",
        answerC = "Bermuda, Haiti und Kuba",
        answerD = "Bermuda, Bahamas und Jamaika",
        correctAnswer = 1,
        explanation = "Das Bermuda-Dreieck wird durch die Eckpunkte Bermuda, das südliche Florida (Miami) und Puerto Rico definiert. Es liegt im Nordatlantik und hat eine Fläche von ca. 1,3 Millionen km².",
        difficulty = 3,
        funFact = "Statistisch gesehen ist das Bermuda-Dreieck nicht gefährlicher als andere stark befahrene Meeresregionen – die mysteriösen Verschwindungsfälle sind größtenteils übertrieben oder falsch dargestellt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Halbinsel der Welt nach Flächeninhalt?",
        answerA = "Skandinavische Halbinsel",
        answerB = "Arabische Halbinsel",
        answerC = "Indische Halbinsel (Dekkan)",
        answerD = "Kamtschatka-Halbinsel",
        correctAnswer = 1,
        explanation = "Die Arabische Halbinsel ist mit einer Fläche von etwa 3,2 Millionen km² die größte Halbinsel der Welt. Sie umfasst Länder wie Saudi-Arabien, Jemen, Oman, die VAE, Katar, Bahrain und Kuwait.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Mantelplume formte die Hawaiianische Inselkette durch die Bewegung der Pazifischen Platte?",
        answerA = "Yellowstone-Hotspot",
        answerB = "Kerguelen-Hotspot",
        answerC = "Hawaii-Hotspot",
        answerD = "Tristan-da-Cunha-Hotspot",
        correctAnswer = 2,
        explanation = "Der Hawaii-Hotspot ist ein Mantelplume, über den sich die Pazifische Platte hinwegbewegt. Dadurch entstanden die Hawaiianischen Inseln als eine Kette von Vulkanen von Nordwesten nach Südosten.",
        difficulty = 3,
        funFact = "Der Hotspot ist stationär, die Platte bewegt sich – ältere Inseln liegen im Nordwesten, jüngere im Südosten. Die jüngste Insel wächst noch aktiv: Lō'ihi, ein Unterwasservulkan."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land beansprucht nahezu das gesamte Südchinesische Meer mit seiner 'Neun-Striche-Linie'?",
        answerA = "Vietnam",
        answerB = "Volksrepublik China",
        answerC = "Philippinen",
        answerD = "Malaysia",
        correctAnswer = 1,
        explanation = "Die Volksrepublik China beansprucht nahezu das gesamte Südchinesische Meer einschließlich der Spratlys mit ihrer sogenannten 'Neun-Striche-Linie', was von vielen Staaten und dem Internationalen Seegerichtshof abgelehnt wird.",
        difficulty = 3,
        funFact = "China hat auf mehreren Riffen der Spratlys künstliche Inseln gebaut und militärische Anlagen errichtet – ein erheblicher geopolitischer Konfliktpunkt in Asien."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Wüste ist die größte der Erde, wenn man Kältewüsten mitzählt?",
        answerA = "Sahara",
        answerB = "Gobi",
        answerC = "Arabische Wüste",
        answerD = "Antarktische Eiswüste",
        correctAnswer = 3,
        explanation = "Die Antarktis ist als Kältewüste mit einer Fläche von 14 Millionen km² die größte Wüste der Welt. Die Sahara ist mit 9,2 Millionen km² die größte heiße Wüste.",
        difficulty = 3,
        funFact = "In der Antarktis fallen im Inneren weniger als 50 mm Niederschlag pro Jahr – technisch gesehen erfüllt sie damit die Definition einer Wüste."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Berg ist vom Meeresgrund aus gemessen der höchste der Erde?",
        answerA = "Fuji (Japan)",
        answerB = "Mauna Loa (Hawaii)",
        answerC = "Chimborazo (Ecuador)",
        answerD = "Mauna Kea (Hawaii)",
        correctAnswer = 3,
        explanation = "Mauna Kea auf Hawaii (USA) ragt 4.205 m über den Meeresspiegel, misst aber vom Meeresgrund bis zum Gipfel über 10.200 m – damit ist er der höchste Berg der Welt, gemessen von seiner Basis.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die natürliche Grenze zwischen Frankreich und Spanien?",
        answerA = "Alpen",
        answerB = "Kantabrisches Gebirge",
        answerC = "Pyrenäen",
        answerD = "Zentralmassiv",
        correctAnswer = 2,
        explanation = "Die Pyrenäen erstrecken sich über etwa 430 km vom Atlantik bis zum Mittelmeer und bilden die natürliche Landgrenze zwischen Frankreich und Spanien (sowie Andorra).",
        difficulty = 3,
        funFact = "Der höchste Gipfel der Pyrenäen, der Aneto, liegt mit 3.404 m vollständig auf spanischem Territorium."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche zwei Gewässer verbindet der Suezkanal miteinander?",
        answerA = "Atlantischer Ozean und Mittelmeer",
        answerB = "Mittelmeer und Rotes Meer",
        answerC = "Rotes Meer und Arabisches Meer",
        answerD = "Mittelmeer und Persischer Golf",
        correctAnswer = 1,
        explanation = "Der Suezkanal verbindet das Mittelmeer (bei Port Said) mit dem Roten Meer (bei Suez) und ermöglicht damit eine Schifffahrtsroute zwischen dem Atlantik und dem Indischen Ozean ohne Umrundung Afrikas.",
        difficulty = 3,
        funFact = "Der Suezkanal wurde 1869 eröffnet und ist 193 km lang. Er hat keine Schleusen, da der Höhenunterschied zwischen Mittelmeer und Rotem Meer minimal ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die längste Küstenlinie der Welt?",
        answerA = "Russland",
        answerB = "Australien",
        answerC = "USA",
        answerD = "Kanada",
        correctAnswer = 3,
        explanation = "Kanada hat mit über 202.000 km die längste Küstenlinie der Welt, einschließlich aller Inseln und der arktischen Küsten. Das sind etwa 28 % der gesamten Weltküstenlänge.",
        difficulty = 3,
        funFact = "Würde man die Küstenlinie Kanadas gerade machen, könnte man damit die Erde mehr als fünfmal umrunden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der aktivste Vulkan Europas nach Häufigkeit der Ausbrüche?",
        answerA = "Vesuv (Italien)",
        answerB = "Ätna (Sizilien, Italien)",
        answerC = "Hekla (Island)",
        answerD = "Teide (Kanarische Inseln, Spanien)",
        correctAnswer = 1,
        explanation = "Der Ätna auf Sizilien ist der aktivste Vulkan Europas und bricht mehrmals jährlich aus. Mit 3.350 m ist er auch der höchste aktive Vulkan Europas.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirgssystem ist das längste auf der Landoberfläche der Erde?",
        answerA = "Himalaya",
        answerB = "Rocky Mountains",
        answerC = "Anden",
        answerD = "Großer Dividing Range",
        correctAnswer = 2,
        explanation = "Die Anden in Südamerika sind mit einer Länge von etwa 7.000 km das längste Gebirgssystem der Welt auf dem Festland. Sie erstrecken sich entlang der gesamten Westküste Südamerikas.",
        difficulty = 3,
        funFact = "Das längste Gebirgssystem insgesamt ist der Mittelatlantische Rücken unter dem Ozean mit über 16.000 km Länge."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerespassage verbindet Atlantik und Pazifik ohne künstlichen Kanal, südlich von Südamerika?",
        answerA = "Drakepassage",
        answerB = "Magellanstraße",
        answerC = "Beagle-Kanal",
        answerD = "Lemaire-Kanal",
        correctAnswer = 0,
        explanation = "Die Drakepassage zwischen dem südlichen Südamerika und der Antarktischen Halbinsel ist die breiteste und tiefste Meeresstraße, die Atlantik und Pazifik natürlich verbindet – ohne Kanal.",
        difficulty = 3,
        funFact = "Die Drakepassage ist bekannt für extrem stürmische Gewässer und galt jahrhundertelang als eine der gefährlichsten Schiffsrouten der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das flächenmäßig kleinste Land der Welt?",
        answerA = "Monaco",
        answerB = "Nauru",
        answerC = "Vatikanstadt",
        answerD = "San Marino",
        correctAnswer = 2,
        explanation = "Vatikanstadt ist mit einer Fläche von nur 0,44 km² der kleinste Staat der Welt nach Landfläche. Monaco ist mit 2,02 km² das zweitkleinste Land.",
        difficulty = 3,
        funFact = "Vatikanstadt hat mehr UNESCO-Weltkulturerbestätten pro Fläche als jedes andere Land – eigentlich ist die gesamte Stadt ein einziges Weltkulturerbe."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher längste Fluss Afrikas durchquert elf Länder auf seinem Weg zum Mittelmeer?",
        answerA = "Kongo",
        answerB = "Niger",
        answerC = "Sambesi",
        answerD = "Nil",
        correctAnswer = 3,
        explanation = "Der Nil ist mit einer Länge von etwa 6.650 km der längste Fluss Afrikas und einer der längsten der Welt. Er entspringt im zentralafrikanischen Hochland und mündet ins Mittelmeer.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat nennt sich in seiner Landessprache 'Druk Yul' (Land des Donners)?",
        answerA = "Nepal",
        answerB = "Sikkim",
        answerC = "Bhutan",
        answerD = "Ladakh",
        correctAnswer = 2,
        explanation = "Bhutan bezeichnet sich selbst in seiner Landessprache Dzongkha als 'Druk Yul' (Land des Donners). Das kleine Königreich liegt vollständig im östlichen Himalaya.",
        difficulty = 3,
        funFact = "Bhutan misst seinen Fortschritt nicht am BIP, sondern am 'Bruttoinlandsglück' – einem einzigartigen Wohlfahrtsindikator, der neben Wirtschaft auch Kultur und Umwelt berücksichtigt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Inselgruppe im Indischen Ozean liegt im Durchschnitt nur 1,5 m über dem Meeresspiegel?",
        answerA = "Malediven",
        answerB = "Seychellen",
        answerC = "Komoren",
        answerD = "Réunion",
        correctAnswer = 0,
        explanation = "Die Malediven bestehen aus 26 Atollen mit über 1.000 Inseln, die im Durchschnitt nur 1,5 m über dem Meeresspiegel liegen. Sie sind das am stärksten vom Klimawandel und Meeresspiegelanstieg bedrohte Land.",
        difficulty = 3,
        funFact = "Die Regierung der Malediven kaufte Land in Sri Lanka und Indien als Notfallmaßnahme für den Fall, dass ihr Territorium unbewohnbar wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt liegt auf zwei Kontinenten und wird durch den Bosporus geteilt?",
        answerA = "Tiflis (Georgien)",
        answerB = "Istanbul (Türkei)",
        answerC = "Baku (Aserbaidschan)",
        answerD = "Jekaterinburg (Russland)",
        correctAnswer = 1,
        explanation = "Istanbul in der Türkei liegt beiderseits des Bosporus: der europäische Teil (Rumelia) und der asiatische Teil (Anatolia). Es ist die einzige Millionenstadt, die auf zwei Kontinenten liegt.",
        difficulty = 3,
        funFact = "Der Bosporus, der Istanbul teilt, ist nur 700 m breit an seiner engsten Stelle und wird täglich von Tausenden Schiffen passiert."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der tiefste See Afrikas?",
        answerA = "Victoriasee",
        answerB = "Kiwusee",
        answerC = "Tanganjikasee",
        answerD = "Malawisee",
        correctAnswer = 2,
        explanation = "Der Tanganjikasee mit einer maximalen Tiefe von 1.470 m ist der tiefste See Afrikas und der zweittiefste See der Welt nach dem Baikalsee. Er liegt im Ostafrikanischen Grabenbruch.",
        difficulty = 3,
        funFact = "Der Tanganjikasee ist so tief, dass sein Boden unter dem Meeresspiegel liegt. Er enthält rund 17 % des weltweit verfügbaren Süßwassers."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land grenzt als einziges an drei Ozeane?",
        answerA = "USA",
        answerB = "Russland",
        answerC = "Kanada",
        answerD = "Australien",
        correctAnswer = 2,
        explanation = "Kanada grenzt an den Atlantischen Ozean im Osten, den Pazifischen Ozean im Westen und den Arktischen Ozean im Norden – es ist das einzige Land der Welt mit Zugang zu drei Ozeanen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt das größte Unterwassergebirgssystem der Erde?",
        answerA = "Atlantischer Rücken",
        answerB = "Pazifischer Rücken",
        answerC = "Mid-Ocean Ridge System",
        answerD = "Tethys-Rücken",
        correctAnswer = 2,
        explanation = "Das Mid-Ocean Ridge System ist ein zusammenhängendes Unterwassergebirgssystem mit einer Gesamtlänge von über 65.000 km und umspannt nahezu den gesamten Erdball. Es ist das längste Gebirgssystem der Erde.",
        difficulty = 3,
        funFact = "Am Mittelatlantischen Rücken entsteht ständig neue Ozeankruste – Magma quillt auf und schiebt die Platten auseinander mit etwa 2,5 cm pro Jahr."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches weitgehend untergetauchte Territorium wurde 2017 offiziell als achter Kontinent anerkannt?",
        answerA = "Kerguelen-Plateau",
        answerB = "Zealandia",
        answerC = "Lord-Howe-Rücken",
        answerD = "Ontong-Java-Plateau",
        correctAnswer = 1,
        explanation = "Zealandia (auch Te Riu-a-Māui) ist ein weitgehend untergetauchter Kontinent östlich von Australien. Nur Neuseeland und Neukaledonien ragen über die Wasseroberfläche. 2017 wurde es offiziell als achter Kontinent anerkannt.",
        difficulty = 3,
        funFact = "94 % von Zealandia liegen unter Wasser – es sank vor etwa 60–85 Millionen Jahren ab, nachdem es sich von Australien getrennt hatte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die längste internationale Landgrenze zwischen zwei Ländern?",
        answerA = "Russland–Kasachstan",
        answerB = "USA–Kanada",
        answerC = "China–Mongolei",
        answerD = "Brasilien–Bolivien",
        correctAnswer = 1,
        explanation = "Die Grenze zwischen den USA und Kanada ist mit etwa 8.891 km (ohne Alaska) die längste internationale Grenze zwischen zwei Ländern und ist zugleich die längste unbewachte Grenze der Welt.",
        difficulty = 3,
        funFact = "Die Grenze zwischen USA und Kanada wurde 1846 im Oregonvertrag weitgehend entlang des 49. Breitengrades festgelegt."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Meeresgebiet liegt die Sargassosee – das einzige Meer der Welt ohne Landgrenzen?",
        answerA = "Karibisches Meer",
        answerB = "Nordatlantik",
        answerC = "Südatlantik",
        answerD = "Südpazifik",
        correctAnswer = 1,
        explanation = "Die Sargassosee liegt im Nordatlantik und ist einzigartig, weil sie das einzige Meer der Welt ist, das durch Meeresströmungen begrenzt wird – nicht durch Landmassen. Es ist bekannt für seine Seetangfelder.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der tiefste Punkt des afrikanischen Kontinents?",
        answerA = "Assalsee (Dschibuti) – 155 m unter dem Meeresspiegel",
        answerB = "Danakil-Senke (Äthiopien) – 116 m unter dem Meeresspiegel",
        answerC = "Qattara-Senke (Ägypten) – 133 m unter dem Meeresspiegel",
        answerD = "Chott el-Djerid (Tunesien) – 17 m unter dem Meeresspiegel",
        correctAnswer = 0,
        explanation = "Der Assalsee (Lac Assal) in Dschibuti liegt 155 m unter dem Meeresspiegel und ist damit der tiefste Punkt Afrikas. Er ist zugleich einer der salzreichsten Seen der Welt.",
        difficulty = 3,
        funFact = "Der Salzgehalt des Assalsees beträgt 34,8 % – zehnmal höher als der Durchschnitt der Weltmeere. Regen fällt dort im Schnitt nur 2–4 Tage pro Jahr."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat offiziell keine gesetzlich festgelegte Hauptstadt, obwohl Bern als Bundesstadt gilt?",
        answerA = "Österreich",
        answerB = "Schweiz",
        answerC = "Luxemburg",
        answerD = "Liechtenstein",
        correctAnswer = 1,
        explanation = "Die Schweiz hat offiziell keine Hauptstadt. Bern ist die 'Bundesstadt' (Sitz der Bundesbehörden), aber der Begriff 'Hauptstadt' ist in der Bundesverfassung nicht festgelegt.",
        difficulty = 3,
        funFact = "Israel beansprucht Jerusalem als seine Hauptstadt, was international umstritten ist – viele Staaten haben ihre Botschaften in Tel Aviv."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße ist der Hauptzugang zwischen Nordsee und Ostsee?",
        answerA = "Öresundstraße",
        answerB = "Kieler Förde",
        answerC = "Skagerrak",
        answerD = "Kattegat",
        correctAnswer = 2,
        explanation = "Das Skagerrak ist die Meeresstraße zwischen Norwegen, Schweden und Dänemark, die als Hauptzugang zwischen der Nordsee und dem Kattegat/Öresund zur Ostsee gilt.",
        difficulty = 3,
        funFact = "Der Nord-Ostsee-Kanal in Schleswig-Holstein ist eine künstliche Verbindung und verkürzt die Schifffahrt zwischen Nordsee und Ostsee erheblich."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Inseln aller Länder der Welt nach absoluter Anzahl?",
        answerA = "Indonesien",
        answerB = "Philippinen",
        answerC = "Schweden",
        answerD = "Kanada",
        correctAnswer = 2,
        explanation = "Schweden hat nach offizieller Zählung über 221.000 Inseln, von denen rund 1.000 bewohnt sind. Damit übertrifft es Indonesien (über 17.000 Inseln) und die Philippinen (über 7.000 Inseln) bei weitem.",
        difficulty = 3,
        funFact = "Die schwedischen Inseln sind überwiegend kleine Schären (Schärenküste) – viele sind kaum größer als ein Fußballfeld."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Insel Europas?",
        answerA = "Island",
        answerB = "Großbritannien",
        answerC = "Irland",
        answerD = "Sizilien",
        correctAnswer = 1,
        explanation = "Großbritannien (England, Schottland und Wales) ist mit einer Fläche von ca. 229.000 km² die größte Insel Europas, gefolgt von Island mit 103.000 km².",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche geologische Formation wurde 2017 offiziell als achter Erdkontinent anerkannt und liegt größtenteils unter Wasser?",
        answerA = "Kerguelen-Plateau",
        answerB = "Zealandia (Te Riu-a-Māui)",
        answerC = "Lord-Howe-Rücken",
        answerD = "Ontong-Java-Plateau",
        correctAnswer = 1,
        explanation = "Zealandia (Te Riu-a-Māui) ist ein weitgehend untergetauchter Kontinent östlich von Australien mit einer Fläche von ca. 4,9 Millionen km². Nur Neuseeland und Neukaledonien ragen über die Meeresoberfläche.",
        difficulty = 3,
        funFact = "Zealandia trennte sich vor etwa 83 Millionen Jahren von Australien und Antarktika. Das Absinken erfolgte über Millionen von Jahren durch Ausdehnung der Kruste."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher aktive Vulkan im Kongo (DRC) ist bekannt für seinen permanent aktiven Lavasee?",
        answerA = "Mount Cameroon",
        answerB = "Erta Ale (Äthiopien)",
        answerC = "Nyiragongo",
        answerD = "Ol Doinyo Lengai (Tansania)",
        correctAnswer = 2,
        explanation = "Der Nyiragongo liegt im östlichen Kongo (DRC) auf dem afrikanischen Festland, nahe der Stadt Goma. Er besitzt einen der aktivsten und größten Lavaseen der Welt.",
        difficulty = 3,
        funFact = "2002 ergoss sich Lava des Nyiragongo direkt durch die Stadt Goma bis in den Kivu-See. Über 100.000 Menschen mussten evakuiert werden."
    ),

    // ── HARD (difficulty = 3) ── 50 additional questions (batch 3) ───────────

    Question(
        categoryId = 1,
        questionText = "Durch welchen geologischen Prozess entstanden die Kanarischen Inseln?",
        answerA = "Subduktion der Afrikanischen Platte",
        answerB = "Spreizung des Mittelatlantischen Rückens",
        answerC = "Vulkanismus über einem Mantelplume (Hotspot)",
        answerD = "Abtrennung vom afrikanischen Festland durch Erosion",
        correctAnswer = 2,
        explanation = "Die Kanarischen Inseln entstanden durch einen Hotspot – einen stationären Mantelplume – unter der Afrikanischen Platte. Die Inseln werden durch Aufsteigen von Magma durch die Plattenkruste gebildet.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meeresströmungssystem transportiert Wärme vom tropischen Atlantik nach Nordeuropa?",
        answerA = "Benguela-Strom",
        answerB = "Humboldtstrom",
        answerC = "Atlantische Umwälzzirkulation (AMOC)",
        answerD = "Agulhasstrom",
        correctAnswer = 2,
        explanation = "Die Atlantische Umwälzzirkulation (Atlantic Meridional Overturning Circulation, AMOC) transportiert warmes Oberflächenwasser nach Norden und kaltes Tiefenwasser nach Süden. Sie ist entscheidend für das milde Klima Nordwesteuropas.",
        difficulty = 3,
        funFact = "Wissenschaftliche Studien zeigen, dass die AMOC durch den Klimawandel deutlich schwächer geworden ist – mit potenziell gravierenden Folgen für das europäische Klima."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Karstgebiet in Vietnam ist UNESCO-Weltnaturerbe und bekannt für seine Tausenden von Kalksteininseln?",
        answerA = "Ha Long Bucht",
        answerB = "Phong Nha-Ke Bang",
        answerC = "Ninh Binh",
        answerD = "Cat Ba-Archipel",
        correctAnswer = 0,
        explanation = "Die Ha Long Bucht im Golf von Tonkin, Vietnam, ist ein klassisches Karstgebiet mit über 1.600 Kalksteininseln und -felsen. Sie ist seit 1994 UNESCO-Weltnaturerbe und eines der bekanntesten Karstlandschafts-Beispiele Asiens.",
        difficulty = 3,
        funFact = "Die Kalksteinformationen der Ha Long Bucht entstanden vor ca. 500 Millionen Jahren und wurden über Jahrmillionen durch Meerwasser und Regen zu ihren heutigen Formen herausgearbeitet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches war das erste wissenschaftlich dokumentierte Beweismittel für die Kontinentaldrift-Theorie von Alfred Wegener?",
        answerA = "Identische Fossilien auf verschiedenen Kontinenten",
        answerB = "Passung der Küstenlinien von Afrika und Südamerika",
        answerC = "Gleiche Gesteinsformationen auf getrennten Kontinenten",
        answerD = "Paläomagnetische Streifen am Meeresboden",
        correctAnswer = 1,
        explanation = "Alfred Wegener nutzte als primäres Argument die auffällige geometrische Passung der Küstenlinien Afrikas und Südamerikas, die er 1912 beschrieb. Später kamen Fossilübereinstimmungen und Gesteinsanalysen hinzu.",
        difficulty = 3,
        funFact = "Wegeners Theorie der Kontinentaldrift wurde zu Lebzeiten von der Wissenschaft abgelehnt. Erst in den 1950er–60er Jahren, nach Entdeckung der paläomagnetischen Streifenmuster am Meeresboden, wurde sie akzeptiert."
    ),

    Question(
        categoryId = 1,
        questionText = "In welcher Epoche der Erdgeschichte war der Superkontinent Pangäa vollständig vereint?",
        answerA = "Ordovizium (ca. 470 Ma)",
        answerB = "Perm bis frühe Trias (ca. 300–200 Ma)",
        answerC = "Kreide (ca. 100 Ma)",
        answerD = "Jura (ca. 180 Ma)",
        correctAnswer = 1,
        explanation = "Pangäa war vor etwa 300 Millionen Jahren (Perm) vollständig vereint. Im frühen Jura (ca. 175 Ma) begann er sich zu trennen: zunächst in Laurasia (Norden) und Gondwana (Süden).",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist Paläoklimatologie und welcher Indikator liefert die detailliertesten Klimadaten der vergangenen 800.000 Jahre?",
        answerA = "Studie fossiler Meeressedimente; Foraminiferenschalen",
        answerB = "Analyse von Baumringen; Dendrochronologie-Rekonstruktionen",
        answerC = "Studie von Eisbohrkernen; CO₂- und Temperaturaufzeichnungen",
        answerD = "Analyse von Tropfsteinen; Stalagmit-Isotopenprofile",
        correctAnswer = 2,
        explanation = "Eisbohrkerne aus der Antarktis (z.B. vom Vostok-See oder dem EPICA-Projekt) enthalten eingeschlossene Luftblasen, die CO₂-Konzentration und Temperatur über die letzten 800.000 Jahre direkt messbar machen.",
        difficulty = 3,
        funFact = "Der EPICA-Bohrkern aus der Antarktis reicht 3.270 m tief und deckt acht vollständige Eis- und Warmzeiten ab – ein einzigartiges Klimaarchiv der Erde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gletscher auf Neuseeland ist einer der am schnellsten zugänglichen Gletscher der Welt, der fast bis ins Regenwaltniveau reicht?",
        answerA = "Tasman-Gletscher",
        answerB = "Fox-Gletscher",
        answerC = "Godley-Gletscher",
        answerD = "Murchison-Gletscher",
        correctAnswer = 1,
        explanation = "Der Fox-Gletscher (Te Moeka o Tuawe) auf der Südinsel Neuseelands ist für seinen steilen Abstieg von den Neuseeländischen Alpen bis auf nur 300 m über dem Meeresspiegel bekannt und liegt nahe am Regenwald.",
        difficulty = 3,
        funFact = "Der nahe gelegene Franz-Josef-Gletscher ist ähnlich berühmt. Beide Gletscher zogen sich in den letzten Jahrzehnten durch den Klimawandel erheblich zurück."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kratersee in Oregon, USA, gilt als tiefster See Nordamerikas?",
        answerA = "Crater Lake (Oregon)",
        answerB = "Lake Tahoe (Kalifornien/Nevada)",
        answerC = "Great Slave Lake (Kanada)",
        answerD = "Flathead Lake (Montana)",
        correctAnswer = 0,
        explanation = "Der Crater Lake im Bundesstaat Oregon ist mit 594 m Tiefe der tiefste See der USA und der neunttiefste See der Welt. Er entstand im Caldera-Krater des erloschenen Vulkans Mount Mazama.",
        difficulty = 3,
        funFact = "Der Crater Lake hat keinen Zu- oder Abfluss – sein Wasserstand wird ausschließlich durch Niederschlag und Verdunstung reguliert. Das Wasser ist von außerordentlicher Klarheit und Reinheit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Sodasee in Tansania ist so stark alkalisch, dass er die meisten Lebewesen tötet, aber Millionen Flamingos anzieht?",
        answerA = "Natron-See",
        answerB = "Eyasi-See",
        answerC = "Manyara-See",
        answerD = "Rukwa-See",
        correctAnswer = 0,
        explanation = "Der Natron-See in Tansania hat einen pH-Wert von bis zu 10,5 und Temperaturen über 60°C in manchen Bereichen. Der extrem hohe Sodagehalt zieht Millionen Flamingos an, für die die Cyanobakterien des Sees eine Hauptnahrungsquelle darstellen.",
        difficulty = 3,
        funFact = "Tiere, die in den Natron-See geraten und verenden, werden durch Natriumcarbonat chemisch konserviert und verkalken – ein bizarrer natürlicher Vorgang."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher der größten Untergrundseen der Welt befindet sich unter dem Antarktischen Eisschild?",
        answerA = "Lake Whillans",
        answerB = "Lake Ellsworth",
        answerC = "Vostok-See",
        answerD = "Lake CECs",
        correctAnswer = 2,
        explanation = "Der Vostok-See unter dem Antarktischen Eisschild ist mit einer Fläche von ca. 15.690 km² der größte bekannte Untersee-See unter dem Eis. Er liegt unter bis zu 4 km dickem Eis.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Phänomen beschreibt die Ausbreitung von Wüstengebieten durch Übernutzung und Klimawandel vor allem in der Sahelzone?",
        answerA = "Lateritisierung",
        answerB = "Desertifikation",
        answerC = "Deflation",
        answerD = "Erosion",
        correctAnswer = 1,
        explanation = "Desertifikation bezeichnet die Degradierung von fruchtbarem Land in Wüste oder wüstenähnliche Gebiete, verursacht durch Klimawandel, Überweidung, Abholzung und intensive Landwirtschaft. Die Sahelzone in Afrika ist besonders betroffen.",
        difficulty = 3,
        funFact = "Die Große Grüne Mauer ist ein afrikanisches Projekt, das durch Aufforstung eines 8.000 km langen Gürtels quer durch den Sahel die Desertifikation aufhalten soll."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Aral-See, der durch die Umlenkung seiner Zuflüsse fast vollständig ausgetrocknet ist?",
        answerA = "Nur in Kasachstan",
        answerB = "Nur in Usbekistan",
        answerC = "An der Grenze zwischen Kasachstan und Usbekistan",
        answerD = "In Turkmenistan",
        correctAnswer = 2,
        explanation = "Der Aral-See liegt an der Grenze zwischen Kasachstan und Usbekistan. Durch die Umlenkung der Zuflüsse Amudarja und Syrdarja für Bewässerungsprojekte schrumpfte er ab den 1960ern auf einen Bruchteil seiner ursprünglichen Größe.",
        difficulty = 3,
        funFact = "Der Aral-See war einst der viertgrößte See der Welt. Heute sind nur noch Restgewässer übrig – ein Mahnmal menschlicher Eingriffe in das natürliche Wassersystem."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozeanstrom fließt entlang der Westküste Südafrikas nordwärts und kühlt das dortige Klima erheblich ab?",
        answerA = "Agulhas-Strom",
        answerB = "Benguela-Strom",
        answerC = "Moçambique-Strom",
        answerD = "Atlantischer Nordäquatorialstrom",
        correctAnswer = 1,
        explanation = "Der Benguela-Strom ist ein kalter Meeresauftriebsstrom, der entlang der Westküste Südafrikas und Namibias nach Norden fließt. Er kühlt die Küstenregion und ist mitverantwortlich für die Entstehung der Namib-Wüste.",
        difficulty = 3,
        funFact = "Der Benguela-Strom bringt nährstoffreiches Tiefenwasser an die Oberfläche – eine der produktivsten Fischereizonen der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt gilt als bevölkerungsreichste Metropolregion der Welt (Stand 2020er)?",
        answerA = "Peking",
        answerB = "Tokio",
        answerC = "Shanghai",
        answerD = "Delhi",
        correctAnswer = 1,
        explanation = "Die Metropolregion Tokio-Yokohama ist mit über 37–38 Millionen Einwohnern die bevölkerungsreichste Stadtregion der Welt. Zum Vergleich: Delhi folgt mit ca. 32–33 Millionen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Karstphänomen sind unterirdische Höhlen und Hohlräume, die durch Auflösung von Kalkstein entstehen?",
        answerA = "Dolinen",
        answerB = "Polje",
        answerC = "Karren",
        answerD = "Schachtkarst",
        correctAnswer = 0,
        explanation = "Dolinen sind kesselförmige Hohlformen in Karstlandschaften, die durch chemische Auflösung von Kalkstein und/oder Einsturz von Hohlräumen entstehen. Sie sind eines der charakteristischsten Merkmale von Karstlandschaften.",
        difficulty = 3,
        funFact = "In Florida (USA) treten häufig plötzliche Dolinen-Einbrüche auf, die Häuser und Straßen verschlucken – ein direktes Ergebnis des Karstphänomens."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel vulkanischen Ursprungs ist die am dünnsten besiedelte Insel Europas und entstand erst 1963 durch einen Vulkanausbruch?",
        answerA = "Fayal (Azoren)",
        answerB = "Surtsey (Island)",
        answerC = "Stromboli (Italien)",
        answerD = "Pantelleria (Italien)",
        correctAnswer = 1,
        explanation = "Surtsey entstand von 1963 bis 1967 durch Unterwasservulkanismus vor der isländischen Küste. Sie ist eine der jüngsten Inseln der Welt und ein Naturreservat, das die primäre Besiedlung neuen Landes dokumentiert.",
        difficulty = 3,
        funFact = "Surtsey wurde 2008 zum UNESCO-Weltnaturerbe ernannt, weil sie einzigartige Einblicke in die Besiedlung neu entstandenen Lebensraums durch Pflanzen und Tiere bietet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher antarktische Gletscher gilt als der 'Weltuntergangsgletcher', weil sein Kollaps den Meeresspiegel um mehrere Meter anheben könnte?",
        answerA = "Lambert-Gletscher",
        answerB = "Pine-Island-Gletscher",
        answerC = "Thwaites-Gletscher",
        answerD = "Byrd-Gletscher",
        correctAnswer = 2,
        explanation = "Der Thwaites-Gletscher in der Westantarktis enthält so viel Eis, dass sein vollständiges Abschmelzen den globalen Meeresspiegel um etwa 65 cm anheben würde. Er gilt als ein wichtiger Kipppunkt im Klimasystem.",
        difficulty = 3,
        funFact = "Der Thwaites-Gletscher hat die Größe des gesamten US-Bundesstaats Florida und verliert jährlich Milliarden Tonnen Eis an das Meer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Meeresgraben im Pazifik entstand durch die Subduktion der Philippinischen Platte unter die Eurasische Platte?",
        answerA = "Marianengraben",
        answerB = "Philippinengraben",
        answerC = "Izu-Bonin-Graben",
        answerD = "Japan-Graben",
        correctAnswer = 1,
        explanation = "Der Philippinengraben (Philippine Trench) liegt östlich der Philippinen und entstand durch Subduktion der Philippinischen Platte. Er erreicht eine maximale Tiefe von etwa 10.540 m.",
        difficulty = 3,
        funFact = "Der Philippinengraben verläuft parallel zur philippinischen Küste und ist geologisch mitverantwortlich für die hohe seismische und vulkanische Aktivität des Inselarchipels."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Region gilt als der am stärksten von Desertifikation betroffene Großraum Chinas?",
        answerA = "Lösshochland (Huang-He-Becken)",
        answerB = "Innere Mongolei und Ordos-Plateau",
        answerC = "Sichuan-Becken",
        answerD = "Manchurische Ebene",
        correctAnswer = 1,
        explanation = "Die Innere Mongolei und das Ordos-Plateau in Nordchina sind am stärksten von Desertifikation betroffen. Die Ausbreitung der Gobi-Wüste durch Übernutzung und Klimawandel führt zu Sandstürmen, die bis Peking reichen.",
        difficulty = 3,
        funFact = "China hat das größte Aufforstungsprogramm der Welt gestartet ('Grüne Mauer') um die Ausbreitung der Wüste aufzuhalten – mit gemischten Ergebnissen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der weltweit größte Gletscher nach Ausdehnung, der sich in der Antarktis befindet?",
        answerA = "Thwaites-Gletscher",
        answerB = "Lambert-Gletscher",
        answerC = "Beardmore-Gletscher",
        answerD = "Mertz-Gletscher",
        correctAnswer = 1,
        explanation = "Der Lambert-Gletscher in der Ostantarktis ist mit einer Länge von ca. 400 km und einer Breite von bis zu 100 km der flächenmäßig größte Gletscher der Welt.",
        difficulty = 3,
        funFact = "Der Lambert-Gletscher mündet in das Amery-Schelfeis und transportiert jährlich riesige Eismengen in den südlichen Ozean."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche der folgenden Städte hat die höchste Bevölkerungsdichte und gilt als eine der am dichtesten besiedelten Städte der Welt?",
        answerA = "Hongkong",
        answerB = "Manila",
        answerC = "Dhaka",
        answerD = "Mumbai",
        correctAnswer = 2,
        explanation = "Dhaka, die Hauptstadt Bangladeschs, gilt als eine der am dichtesten besiedelten Städte der Welt mit über 44.500 Einwohnern pro km². Die Metropolregion wächst weiterhin rasant.",
        difficulty = 3,
        funFact = "Manila auf den Philippinen hält den Rekord für die höchste Dichte in einem einzelnen Stadtbezirk – Mandaluyong mit über 100.000 Einwohnern/km²."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Golfstrom-Ableger fließt entlang der Westküste Europas nach Norden und beeinflusst das Klima Skandinaviens?",
        answerA = "Kanarenstrom",
        answerB = "Irmingersstrom",
        answerC = "Norwegischer Strom",
        answerD = "Nordatlantikstrom",
        correctAnswer = 3,
        explanation = "Der Nordatlantikstrom (North Atlantic Current) ist der nordöstliche Ausläufer des Golfstroms. Er transportiert warmes Wasser nach Nordeuropa und macht das Klima Norwegens, Islands und Großbritanniens deutlich milder als es der Breitengrad erwarten ließe.",
        difficulty = 3,
        funFact = "Dank des Nordatlantikstroms ist der Hafen von Murmansk in Russland (nördlich des Polarkreises) das ganze Jahr eisfrei – ungewöhnlich für diese Breite."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Phänomen der Paläoklimatologie belegt, dass die Erde vor ca. 700 Millionen Jahren fast vollständig vereist war?",
        answerA = "Großes Sterben (Permian Extinction)",
        answerB = "Schneeball-Erde-Hypothese (Snowball Earth)",
        answerC = "Kambrium-Explosion",
        answerD = "Ordovizische Vereisung",
        correctAnswer = 1,
        explanation = "Die Schneeball-Erde-Hypothese besagt, dass die Erde in der Neoproterozoischen Ära (ca. 700–635 Ma) mehrmals fast vollständig bis in tropische Breiten vereist war. Beweise liefern glaziale Ablagerungen nahe dem damaligen Äquator.",
        difficulty = 3,
        funFact = "Paradoxerweise könnte die Schneeball-Erde die kambrische Explosion des Lebens ausgelöst haben – Organismen, die die extremen Bedingungen überlebten, entwickelten sich rasch zu neuen Formen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher See in der Türkei ist der größte Sodasee der Welt und liegt 1.648 m über dem Meeresspiegel?",
        answerA = "Vansee",
        answerB = "Tuz-See",
        answerC = "Beyşehirsee",
        answerD = "Eğirdursee",
        correctAnswer = 0,
        explanation = "Der Vansee (Van Gölü) in der Osttürkei ist der größte Sodasee der Welt mit einem pH-Wert von etwa 9,7–9,8. Er liegt 1.648 m über dem Meeresspiegel und hat eine Fläche von ca. 3.574 km².",
        difficulty = 3,
        funFact = "Der Vansee hat keinen Abfluss – Salze und Mineralien sammeln sich über Jahrtausende an. Trotz des hohen Sodaanteils gedeihen dort spezielle alkaliphile Organismen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Meeresströmungskreislauf transportiert Tiefenwasser global und wird als 'ozeanisches Förderband' bezeichnet?",
        answerA = "Thermohaline Zirkulation",
        answerB = "Nordpazifik-Gyre",
        answerC = "Äquatorialer Gegenstrom",
        answerD = "Antarctic Circumpolar Current",
        correctAnswer = 0,
        explanation = "Die thermohaline Zirkulation (auch 'global ocean conveyor belt') ist ein weltumspannendes Strömungssystem, das durch Dichte-Unterschiede (Temperatur und Salzgehalt) angetrieben wird und Wärme und Nährstoffe global verteilt.",
        difficulty = 3,
        funFact = "Ein vollständiger Umlauf des ozeanischen Förderbandes dauert schätzungsweise 1.000 Jahre. Es verbindet alle Ozeane miteinander."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr tauchte vor Island durch Vulkanismus die neue Insel Surtsey aus dem Meer auf?",
        answerA = "1957",
        answerB = "1963",
        answerC = "1971",
        answerD = "1946",
        correctAnswer = 1,
        explanation = "Surtsey entstand zwischen November 1963 und Juni 1967 durch Unterwasservulkanismus. Der Ausbruch begann 130 m unter dem Meeresspiegel, bevor die Insel die Wasseroberfläche durchbrach.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die extremste bekannte Desertifikations-Hotspot-Zone Afrikas mit jährlichem Flächenverlust an fruchtbarem Land?",
        answerA = "Sahel-Zone (südlich der Sahara)",
        answerB = "Namib-Küstenstreifen",
        answerC = "Kalahari-Becken",
        answerD = "Ogaden-Halbwüste (Somalia/Äthiopien)",
        correctAnswer = 0,
        explanation = "Die Sahel-Zone, die sich quer durch Afrika von Senegal bis Eritrea erstreckt, ist der gravierendste Desertifikations-Hotspot des Kontinents. Jährlich gehen Millionen Hektar fruchtbares Land durch Übernutzung, Dürre und Klimawandel verloren.",
        difficulty = 3,
        funFact = "Zwischen 1960 und 1980 trocknete der Tschadsee, der einst einer der größten Seen Afrikas war, auf weniger als 1/20 seiner ursprünglichen Fläche aus – ein Symbol der Sahel-Krise."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Küstenformation in Norwegen ist ein klassisches Beispiel für übertiefte glaziale Täler, die nach der Eiszeit vom Meer geflutet wurden?",
        answerA = "Ria",
        answerB = "Liman",
        answerC = "Fjord",
        answerD = "Calanque",
        correctAnswer = 2,
        explanation = "Fjorde sind eiszeitlich übervertiefte U-Täler, die nach dem Abschmelzen der Gletscher vom Meer überflutet wurden. Sie sind charakteristisch für Norwegen, Grönland, Island, Chile und Neuseeland.",
        difficulty = 3,
        funFact = "Der Sognefjord in Norwegen ist mit 1.308 m der tiefste Fjord der Welt – an manchen Stellen liegt sein Boden tiefer als der umliegende Meeresboden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kaltzungenstrom im Pazifik ist mit verantwortlich für die Trockenheit der Atacama-Wüste?",
        answerA = "El-Niño-Strom",
        answerB = "Humboldtstrom (Peruanischer Strom)",
        answerC = "Äquatorialer Gegenstrom",
        answerD = "Südpazifischer Gyre",
        correctAnswer = 1,
        explanation = "Der Humboldtstrom (auch Peruanischer Strom) ist ein kalter Auftriebsstrom vor der Westküste Südamerikas. Die kalte Meeresluft hemmt Wolkenbildung und Niederschlag und trägt so zur extremen Trockenheit der Atacama bei.",
        difficulty = 3,
        funFact = "Wenn El Niño den Humboldtstrom schwächt, kann es in der Atacama zu extremen Regenfällen kommen – die Wüste blüht dann binnen weniger Tage auf."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Siachen-Gletscher, über den Indien und Pakistan seit Jahrzehnten einen militärischen Grenzkonflikt führen?",
        answerA = "Nur in Indien",
        answerB = "Nur in Pakistan",
        answerC = "Im umstrittenen Kaschmir-Gebiet zwischen Indien und Pakistan",
        answerD = "In China (Tibet)",
        correctAnswer = 2,
        explanation = "Der Siachen-Gletscher liegt im nördlichen Kaschmir im Karakoram-Gebirge und ist das höchste Schlachtfeld der Welt. Sowohl Indien als auch Pakistan haben Militärposten auf dem Gletscher und beanspruchen das Gebiet.",
        difficulty = 3,
        funFact = "Der Siachen-Gletscher ist mit 76 km Länge einer der größten Nichtpolargletscher der Welt. Der Konflikt dort kostet beide Länder jährlich Milliarden Dollar."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt gilt als 'Stadt auf zwei Kontinenten' und ist Hauptstadt des Landes mit der drittgrößten Landfläche Afrikas?",
        answerA = "Khartoum (Sudan)",
        answerB = "Kairo (Ägypten)",
        answerC = "Algier (Algerien)",
        answerD = "Nairobi (Kenia)",
        correctAnswer = 2,
        explanation = "Algier ist die Hauptstadt Algeriens, dem flächenmäßig größten Land Afrikas (und dem größten der Welt, das vollständig im afrikanischen Kontinent liegt). Algerien hat eine Fläche von 2,38 Mio. km².",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozeanstrom fließt als wärmster Strom des Indischen Ozeans entlang der Ostküste Südafrikas südwärts?",
        answerA = "Mozambique-Strom",
        answerB = "Agulhas-Strom",
        answerC = "Somali-Strom",
        answerD = "West Australian Current",
        correctAnswer = 1,
        explanation = "Der Agulhas-Strom fließt südwärts entlang der Ostküste Südafrikas und ist einer der stärksten Meeresströme der Welt. An seinem Südende treffen warme und kalte Wassermassen aufeinander.",
        difficulty = 3,
        funFact = "Der Agulhas-Strom ist bekannt für gigantische Freak-Wellen, die für Schiffe extrem gefährlich sind. Diese entstehen, wenn Strömungsrichtung und Windwellen aufeinanderprallen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Hochgebirgsregion Zentralasiens wird als 'Pamir' bezeichnet und warum ist sie geologisch bedeutend?",
        answerA = "Schnittpunkt von drei tektonischen Platten (Indisch, Eurasisch, Arabisch)",
        answerB = "Kollisionspunkt von fünf tektonischen Platten (Indisch, Eurasisch, Arabisch, Iranisch, Chinesisch)",
        answerC = "Aktivste Spreizungszone Asiens",
        answerD = "Größtes Hochplateau Asiens mit aktiver Vulkantätigkeit",
        correctAnswer = 1,
        explanation = "Das Pamir-Hochland in Zentralasien ist ein einzigartiger Kollisionspunkt mehrerer tektonischer Einheiten, wo die Indische, Eurasische, Arabische, Iranische und weitere Mikro-Platten aufeinandertreffen. Dies macht es zu einer der seismisch aktivsten Regionen der Welt.",
        difficulty = 3,
        funFact = "Das Pamir wird als 'Dach der Welt' bezeichnet (gemeinsam mit Tibet) und ist der Ausgangspunkt der großen Gebirgssysteme Karakoram, Hindukusch, Kunlun und Tian Shan."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel im Indischen Ozean entstand durch Hotspot-Vulkanismus und ist bekannt für ihren aktiven Piton de la Fournaise?",
        answerA = "Mauritius",
        answerB = "Réunion",
        answerC = "Mayotte",
        answerD = "Rodrigues",
        correctAnswer = 1,
        explanation = "Réunion (Frankreich) ist eine vulkanische Insel über dem Réunion-Hotspot im Indischen Ozean. Der Piton de la Fournaise ist einer der aktivsten Vulkane der Welt mit mehreren Ausbrüchen pro Jahr.",
        difficulty = 3,
        funFact = "Die Insel Mauritius entstand aus demselben Hotspot wie Réunion – sie ist jedoch älter und geologisch inaktiv, da die Platte sich weiterbewegt hat."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte bekannte Lavafeld (Lavaplateau) auf dem europäischen Festland?",
        answerA = "Massif Central (Frankreich)",
        answerB = "Hochlandplateau Island",
        answerC = "Deccan-Traps (Indien)",
        answerD = "Eifel-Vulkanfeld (Deutschland)",
        correctAnswer = 0,
        explanation = "Das Massif Central in Frankreich ist das größte Vulkangebiet des europäischen Festlandes mit erloschenen Vulkankegeln (Puys), Lavafeldern und vulkanischen Hochplateaus. Es entstand durch Vulkanismus im Tertiär und Quartär.",
        difficulty = 3,
        funFact = "Die Auvergne-Vulkane im Massif Central – die Chaîne des Puys – wurden 2018 UNESCO-Weltnaturerbe. Der letzte Ausbruch dort ereignete sich vor nur ca. 6.000 Jahren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt in Peru gilt als höchstgelegene Großstadt der Welt mit über 100.000 Einwohnern?",
        answerA = "Cuzco",
        answerB = "La Paz (Bolivien)",
        answerC = "Puno",
        answerD = "Juliaca",
        correctAnswer = 3,
        explanation = "Juliaca in Peru liegt auf 3.824 m Höhe und ist mit über 300.000 Einwohnern eine der höchstgelegenen Großstädte der Welt. La Paz in Bolivien (Regierungssitz) liegt auf ca. 3.640 m, ist aber ein anderes Land.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches geologische Fossil gilt als 'Leitsteinarten'-Beleg für die frühere Verbindung der Kontinente Gondwana?",
        answerA = "Archaeopteryx",
        answerB = "Glossopteris (Pflanzenfossil)",
        answerC = "Ammonit",
        answerD = "Trilobit",
        correctAnswer = 1,
        explanation = "Glossopteris ist ein ausgestorbener Farnbaum, dessen Fossilien auf allen Südkontinenten (Afrika, Südamerika, Antarktis, Australien, Indien) gefunden wurden. Dies war einer der stärksten Belege für die frühere Verbindung dieser Kontinente zu Gondwana.",
        difficulty = 3,
        funFact = "Auch das Reptil Mesosaurus, dessen Fossilien nur in Südamerika und Südafrika vorkommen, gilt als Beweis für die Kontinentaldrift – es konnte den Atlantik nicht überschwimmen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozeanstrom ist der stärkste der Welt nach Volumendurchsatz?",
        answerA = "Golfstrom",
        answerB = "Kuroshio-Strom",
        answerC = "Antarctic Circumpolar Current",
        answerD = "Agulhas-Strom",
        correctAnswer = 2,
        explanation = "Der Antarctic Circumpolar Current (ACC, auch Südpolarstrom) ist mit einem Durchsatz von ca. 130–140 Sverdrup der stärkste Meeresströmung der Welt. Er umrundet die Antarktis ohne Unterbrechung durch Landmassen.",
        difficulty = 3,
        funFact = "Der ACC verbindet Atlantischen, Pazifischen und Indischen Ozean und spielt eine entscheidende Rolle bei der Regulierung des globalen Wärmehaushalts."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Salzseegebiet in Iran ist einer der größten Salzpfannen der Welt und Teil eines ausgetrockneten Binnenmeeres?",
        answerA = "Dascht-e Kawir",
        answerB = "Dascht-e Lut",
        answerC = "Hamoun-See",
        answerD = "Urmia-See",
        correctAnswer = 0,
        explanation = "Der Dascht-e Kawir (Große Salzwüste) in Iran ist eine riesige Salzpfanne im zentralen Iran, die Überrest eines ausgetrockneten Binnenmeeres ist. Er bedeckt ca. 77.000 km² und ist eine der heißesten und unwirtlichsten Regionen der Welt.",
        difficulty = 3,
        funFact = "Der benachbarte Dascht-e Lut hält den Rekord für die höchste je auf Landoberfläche gemessene Temperatur: 70,7°C wurden per Satellitenmessung registriert."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Ozean befindet sich die weltweit tiefste Tiefseesenke außerhalb des Pazifiks nach dem Marianengraben und Tonga-Graben?",
        answerA = "Philippinengraben (Pazifik)",
        answerB = "Puerto-Rico-Graben (Atlantik)",
        answerC = "South-Sandwich-Graben (Südatlantik)",
        answerD = "Java-Graben (Indischer Ozean)",
        correctAnswer = 1,
        explanation = "Der Puerto-Rico-Graben im Nordatlantik ist mit ca. 8.376 m der tiefste Punkt des Atlantischen Ozeans und insgesamt der siebttiefste bekannte Meeresgraben der Erde.",
        difficulty = 3,
        funFact = "Im Puerto-Rico-Graben wird die Karibische Platte unter die Nordamerikanische Platte subduziert. Die Region ist tektonisch aktiv und für starke Erdbeben bekannt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt hat als erste der Welt eine Bevölkerung von über einer Million Einwohnern erreicht?",
        answerA = "London (ca. 1810)",
        answerB = "Peking (ca. 1700)",
        answerC = "Rom (ca. 1. Jh. v. Chr.)",
        answerD = "Paris (ca. 1850)",
        correctAnswer = 2,
        explanation = "Rom soll zur Zeit des Augustus (1. Jh. n. Chr.) nach manchen Schätzungen bis zu einer Million Einwohner gehabt haben – womit es die erste bekannte Millionenstadt der Geschichte wäre. London überschritt die Million um ca. 1810.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das längste Gletschersystem der Welt auf einem Nicht-Polarkontinent?",
        answerA = "Fedtschenko-Gletscher (Tadschikistan, 77 km)",
        answerB = "Baltoro-Gletscher (Pakistan, 63 km)",
        answerC = "Tasman-Gletscher (Neuseeland, 29 km)",
        answerD = "Gangotri-Gletscher (Indien, 30 km)",
        correctAnswer = 0,
        explanation = "Der Fedtschenko-Gletscher im Pamirgebirge Tadschikistans ist mit ca. 77 km Länge der längste Gletscher außerhalb der Polarregionen. Er liegt auf über 3.800 m Höhe.",
        difficulty = 3,
        funFact = "Der Baltoro-Gletscher in Pakistan ist zwar kürzer, liegt aber in einem der steilsten und höchsten Gebirgsterrains der Welt – umgeben von vier der vierzehn Achttausender (K2, Broad Peak, Gasherbrum I & II)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Karstmassiv in Slowenien gilt als 'Urform' des Karst-Phänomens und gab ihm seinen Namen?",
        answerA = "Škocjan-Schlucht",
        answerB = "Karst-Hochebene (Kras)",
        answerC = "Postojna-Höhle",
        answerD = "Triglav-Massiv",
        correctAnswer = 1,
        explanation = "Das Kras-Hochplateau im Südwesten Sloweniens (und dem angrenzenden Italien) gab dem Karstphänomen seinen wissenschaftlichen Namen. Es ist eine klassische Karstlandschaft mit Dolinen, Poljen und Höhlensystemen.",
        difficulty = 3,
        funFact = "Die Postojna-Höhle im slowenischen Karst ist eines der größten Höhlensysteme Europas mit 24 km zugänglichen Gängen. Sie beherbergt den Olm (Proteus anguinus), eine völlig blinde Höhlensalamanderart."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vulkantyp entsteht typischerweise auf ozeanischen Inseln über Hotspots und bildet die charakteristischen Schildvulkane?",
        answerA = "Stratovulkan (Schichtvulkan)",
        answerB = "Schildvulkan",
        answerC = "Aschekegel",
        answerD = "Caldeira",
        correctAnswer = 1,
        explanation = "Schildvulkane entstehen durch dünnflüssige Basaltlava mit geringem Gasgehalt, die sich breit ausbreitet und flache, schildförmige Berge bildet. Hawaii ist das klassische Beispiel – Mauna Loa und Kilauea sind Schildvulkane.",
        difficulty = 3,
        funFact = "Mauna Loa ist mit einem Volumen von ca. 75.000 km³ der massereichste Vulkan der Erde – deutlich mehr als der Everest."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt in China hat die meisten Einwohner in der eigentlichen Stadtgemeinde (nicht Metropolregion)?",
        answerA = "Peking",
        answerB = "Shanghai",
        answerC = "Chongqing",
        answerD = "Guangzhou",
        correctAnswer = 2,
        explanation = "Chongqing ist nach Fläche und Verwaltungsgebiet die größte Stadtgemeinde Chinas und gilt als eine der bevölkerungsreichsten administrativen Einheiten der Welt. Die Kernstadtfläche umfasst aber auch ländliche Gebiete.",
        difficulty = 3,
        funFact = "Chongqing ist eine der vier regierungsunmittelbaren Städte Chinas (neben Peking, Shanghai und Tianjin) und wirtschaftliches Zentrum des inneren Westchinas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss bildet die größte unterirdische Flusshöhle der Welt auf den Philippinen?",
        answerA = "Puerto-Princesa-Untergrundfluss",
        answerB = "Son-Doong-Fluss (Vietnam)",
        answerC = "Cheddar-Fluss (England)",
        answerD = "Trebbia-Fluss (Italien)",
        correctAnswer = 0,
        explanation = "Der Puerto-Princesa Subterranean River National Park auf Palawan (Philippinen) enthält einen schiffbaren Untergrundfluss von ca. 8,2 km Länge. Er ist UNESCO-Weltnaturerbe und gilt als eine der 'New 7 Wonders of Nature'.",
        difficulty = 3,
        funFact = "Die Kalksteinhöhle des Puerto-Princesa-Flusses beherbergt beeindruckende Stalaktiten und Stalagmiten und mündet direkt ins Südchinesische Meer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Art von tektonischer Grenze liegt vor der Westküste Nordamerikas und hat die Cascade-Vulkankette und den Mount St. Helens gebildet?",
        answerA = "Transformstörung",
        answerB = "Kontinentale Spreizungszone",
        answerC = "Subduktionszone (ozeanisch unter kontinental)",
        answerD = "Kontinentale Kollisionszone",
        correctAnswer = 2,
        explanation = "Die Cascadia-Subduktionszone liegt vor der Nordwestküste Nordamerikas, wo die Juan-de-Fuca-Platte unter die Nordamerikanische Platte taucht. Diese Subduktion schuf die Cascade Range mit aktiven Vulkanen wie Mount St. Helens, Mount Rainier und Mount Shasta.",
        difficulty = 3,
        funFact = "Die Cascadia-Subduktionszone gilt als Quelle eines potenziell katastrophalen Megathrust-Erdbebens der Stärke 9+. Der letzte große Ausbruch ereignete sich 1700."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt ist die höchstgelegene Hauptstadt der Welt?",
        answerA = "Quito (Ecuador)",
        answerB = "Bogotá (Kolumbien)",
        answerC = "La Paz (Bolivien)",
        answerD = "Thimphu (Bhutan)",
        correctAnswer = 2,
        explanation = "La Paz in Bolivien liegt auf etwa 3.640 m (Regierungssitz) bis 4.100 m (Stadtteile El Alto) über dem Meeresspiegel und ist damit die höchstgelegene Hauptstadt der Welt. Sucre ist zwar formelle Hauptstadt Boliviens, liegt aber tiefer.",
        difficulty = 3,
        funFact = "In La Paz kocht Wasser bei nur ca. 86°C statt 100°C, weil der atmosphärische Druck auf dieser Höhe deutlich geringer ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Was sind paläomagnetische Streifen am Meeresboden und was beweisen sie?",
        answerA = "Fossile Magnetit-Ablagerungen; beweisen frühere Verschiebungen des magnetischen Nordpols",
        answerB = "Symmetrische Magnetisierungsstreifen; beweisen Meeresbodenausbreitung und Plattentektonik",
        answerC = "Radioaktive Minerale; beweisen das Alter des Meeresbodens durch Zerfall",
        answerD = "Magnetische Anomalien; beweisen unterirdische Vulkankammern",
        correctAnswer = 1,
        explanation = "Symmetrische paläomagnetische Streifen beiderseits von Mittelozeanischen Rücken zeigen, dass sich der Meeresboden ausbreitet. Magma erstarrt und zeichnet die Ausrichtung des damaligen Erdmagnetfelds auf – dieser Beweis war entscheidend für die Akzeptanz der Plattentektonik.",
        difficulty = 3,
        funFact = "Das Erdmagnetfeld hat sich in der Geschichte vielfach umgekehrt – sogenannte Polumkehrungen. Diese Umkehrungen sind als Streifen im Meeresboden 'eingefroren' und wie ein geologischer Kassettenrekorder lesbar."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss transportiert das meiste Wasser (nach Durchfluss) aller Flüsse der Welt?",
        answerA = "Nil",
        answerB = "Ganges",
        answerC = "Amazonas",
        answerD = "Kongo",
        correctAnswer = 2,
        explanation = "Der Amazonas transportiert mit einem durchschnittlichen Durchfluss von etwa 209.000 m³/s mehr Wasser als die sieben nächstgrößten Flüsse zusammen. Er macht ca. 20 % des gesamten Süßwasserabflusses aller Flüsse weltweit aus.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Phänomen erklärt die periodische Erschlaffung des Humboldtstroms, verbunden mit Erwärmung des ostpazifischen Ozeans?",
        answerA = "La Niña",
        answerB = "El Niño (ENSO-Warmphase)",
        answerC = "Pazifische Dekadische Oszillation",
        answerD = "Dipol-Effekt",
        correctAnswer = 1,
        explanation = "El Niño (ENSO: El Niño–Southern Oscillation) ist eine periodische Erwärmung des östlichen tropischen Pazifiks, verursacht durch Abschwächung der Passatwinde und des Humboldtstroms. Dies hat weltweite Auswirkungen auf Niederschlag und Temperaturen.",
        difficulty = 3,
        funFact = "Die Gegenseite von El Niño ist La Niña – eine ungewöhnliche Abkühlung des östlichen Pazifiks. Beide Phänomene wechseln sich alle 2–7 Jahre ab."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel im Südatlantik gilt als einer der entlegensten bewohnten Orte der Welt und liegt 2.816 km von der nächsten Küste entfernt?",
        answerA = "Ascension Island",
        answerB = "St. Helena",
        answerC = "Tristan da Cunha",
        answerD = "Bouvetinsel",
        correctAnswer = 2,
        explanation = "Tristan da Cunha im Südatlantik ist mit 2.816 km Entfernung zur nächsten bewohnten Küste (St. Helena) die entlegenste bewohnte Insel der Welt. Sie gehört zu Großbritannien und hat etwa 250 Einwohner.",
        difficulty = 3,
        funFact = "Tristan da Cunha ist ein aktiver Vulkan – 1961 mussten alle Bewohner evakuiert werden, kehrten aber 1963 nach Ende des Ausbruchs zurück."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt ist die am schnellsten wachsende Megastadt der Welt nach Bevölkerungszuwachs (Stand 2020er)?",
        answerA = "Lagos (Nigeria)",
        answerB = "Kinshasa (DR Kongo)",
        answerC = "Dhaka (Bangladesch)",
        answerD = "Karachi (Pakistan)",
        correctAnswer = 0,
        explanation = "Lagos in Nigeria wächst mit einem der höchsten Raten weltweit und könnte bis 2100 zur bevölkerungsreichsten Stadt der Welt werden. Der Zuzug aus ländlichen Gebieten Westafrikas ist enorm.",
        difficulty = 3,
        funFact = "Kinshasa (Kongo) und Lagos (Nigeria) liegen nur wenige hundert Kilometer voneinander entfernt und bilden zusammen das einzige Stadtpaar der Welt, dessen Bevölkerung möglicherweise beide die 100-Millionen-Marke überschreiten wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte Binnengewässer Afrikas nach Oberfläche?",
        answerA = "Tanganjikasee",
        answerB = "Tschadsee",
        answerC = "Victoriasee",
        answerD = "Malawisee",
        correctAnswer = 2,
        explanation = "Der Victoriasee mit einer Oberfläche von ca. 68.800 km² ist der größte See Afrikas und der zweitgrößte Süßwassersee der Welt nach Fläche (nach dem Huronsee in Nordamerika). Er liegt an der Grenze von Uganda, Tansania und Kenia.",
        difficulty = 3,
        funFact = "Der Victoriasee ist zwar riesig, aber mit maximal 84 m Tiefe relativ flach. Der viel kleinere Tanganjikasee enthält wegen seiner enormen Tiefe mehr Wasservolumen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Vulkaninsel entstand durch Plattensubduktion im westlichen Pazifik und bildet den Archetypus eines Inselbogen-Vulkans?",
        answerA = "Hawaii (Hotspot-Vulkanism)",
        answerB = "Krakatau (Subduktionszone, Sunda-Bogen)",
        answerC = "Réunion (Hotspot)",
        answerD = "Canary Islands (Hotspot)",
        correctAnswer = 1,
        explanation = "Krakatau liegt im Sunda-Bogen (Indonesien), wo die Indo-Australische Platte unter die Eurasische Platte subduziert. Er ist das klassische Beispiel für einen Subduktionszonen-Vulkan mit explosivem Ausbruchscharakter.",
        difficulty = 3,
        funFact = "Nach dem katastrophalen Ausbruch 1883 kollabierte Krakatau teilweise. 1927 entstand in der Caldera der Nachfolgevulkan 'Anak Krakatau' (Kind des Krakatau), der noch heute aktiv ist."
    ),

    // ── HARD (difficulty = 3) ── 50 additional questions (batch 4) ───────────

    Question(
        categoryId = 1,
        questionText = "Wie tief ist der Eurasische Becken, der tiefste Punkt des Arktischen Ozeans?",
        answerA = "3.850 m",
        answerB = "4.665 m",
        answerC = "5.450 m",
        answerD = "2.980 m",
        correctAnswer = 1,
        explanation = "Das Eurasische Becken im Arktischen Ozean erreicht eine Tiefe von etwa 4.665 m am Punkt Litke Deep. Es ist der tiefste Teil des Arktischen Ozeans und liegt nördlich von Sibirien zwischen dem Lomonossow-Rücken und der eurasischen Küste.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Flussdelta in Südostasien gilt als einer der fruchtbarsten Reisanbaugebiete der Welt und wird vom Mekong gebildet?",
        answerA = "Irrawaddy-Delta (Myanmar)",
        answerB = "Chao-Phraya-Delta (Thailand)",
        answerC = "Mekong-Delta (Vietnam)",
        answerD = "Red-River-Delta (Vietnam)",
        correctAnswer = 2,
        explanation = "Das Mekong-Delta im Süden Vietnams (Đồng bằng sông Cửu Long) ist eines der fruchtbarsten Flussdeltas der Welt. Es produziert etwa die Hälfte des vietnamesischen Reises und ein Drittel aller Agrarerzeugnisse des Landes.",
        difficulty = 3,
        funFact = "Das Mekong-Delta liegt nur knapp über dem Meeresspiegel und ist durch den Klimawandel, Meeresspiegelanstieg und Dammbauten am Oberlauf erheblich bedroht."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der Gebirgszug in den Rocky Mountains, der die nordamerikanische Kontinentalwasserscheide bildet?",
        answerA = "Sierra Nevada",
        answerB = "Cascade Range",
        answerC = "Continental Divide (Great Divide)",
        answerD = "Wasatch Range",
        correctAnswer = 2,
        explanation = "Die Continental Divide (Große Kontinentalscheide) verläuft entlang der Rocky Mountains von Alaska bis New Mexico. Wasser westlich dieser Linie fließt in den Pazifik, östlich davon in den Atlantik oder den Golf von Mexiko.",
        difficulty = 3,
        funFact = "Im Glacier National Park (Montana) gibt es einen See namens Triple Divide Peak, von dem Wasser in drei verschiedene Ozeane fließt: Pazifik, Atlantik und Arktischer Ozean."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Caldera in den USA gilt als eine der größten Supervulkan-Calderas der Welt und ist noch tektonisch aktiv?",
        answerA = "Crater Lake Caldera (Oregon)",
        answerB = "Yellowstone Caldera (Wyoming)",
        answerC = "Long Valley Caldera (Kalifornien)",
        answerD = "Valles Caldera (New Mexico)",
        correctAnswer = 1,
        explanation = "Die Yellowstone Caldera in Wyoming misst etwa 72 × 55 km und ist eine der größten aktiven Supervulkan-Calderas der Welt. Der letzte Supervulkan-Ausbruch erfolgte vor etwa 640.000 Jahren.",
        difficulty = 3,
        funFact = "Der Yellowstone-Hotspot hat sich in den letzten 17 Millionen Jahren westwärts (relativ zur Nordamerikanischen Platte) bewegt und dabei eine Spur erloschener Calderas hinterlassen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Subduktionszone erzeugte das stärkste je aufgezeichnete Erdbeben der Erde (Valdivia-Beben 1960)?",
        answerA = "Cascadia-Subduktionszone (Nordamerika)",
        answerB = "Peru-Chile-Subduktionszone (Südamerika)",
        answerC = "Izu-Bonin-Subduktionszone (Westpazifik)",
        answerD = "Sunda-Subduktionszone (Indonesien)",
        correctAnswer = 1,
        explanation = "Die Peru-Chile-Subduktionszone (auch Atacama-Graben) entstand durch das Untertauchen der Nazca-Platte unter die Südamerikanische Platte. Das Valdivia-Beben 1960 erreichte Magnitude 9,5 – das stärkste je gemessene Erdbeben.",
        difficulty = 3,
        funFact = "Das Valdivia-Beben löste einen Tsunami aus, der Hawaii, Japan und sogar die Philippinen erreichte. Die Todeswelle traf Hawaii mit über 10 m Höhe."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Grabenbruch in Ostafrika gilt als Wiege der Menschheit und teilt den Kontinent in Ost und West?",
        answerA = "Westafrikanischer Rift",
        answerB = "Ostafrikanisches Rift-System (EARS)",
        answerC = "Zentralafrikanischer Graben",
        answerD = "Kongobecken-Rift",
        correctAnswer = 1,
        explanation = "Das Ostafrikanische Rift-System (East African Rift System, EARS) ist ein aktiver Kontinentalrift, der sich von Äthiopien bis Mosambik erstreckt. Es beherbergt zahlreiche tiefe Seen (Tanganjika, Malawi) und ist geologisch die Wiege früher Hominiden.",
        difficulty = 3,
        funFact = "In ca. 10 Millionen Jahren wird Ostafrika entlang des Rifts vom Kontinent abspalten und einen neuen Ozean bilden – Somalia und Teile Kenias werden zur Insel."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie breit ist die Straße von Hormuz an ihrer engsten Stelle, durch die etwa 20 % des weltweiten Erdöls transportiert wird?",
        answerA = "54 km",
        answerB = "38 km",
        answerC = "21 km",
        answerD = "96 km",
        correctAnswer = 1,
        explanation = "Die Straße von Hormuz zwischen dem Iran und Oman/den VAE ist an ihrer schmalsten Stelle etwa 38 km breit. Sie verbindet den Persischen Golf mit dem Golf von Oman und ist eine der strategisch wichtigsten Meerengen der Welt.",
        difficulty = 3,
        funFact = "Täglich passieren rund 17–20 Millionen Barrel Öl die Straße von Hormuz, verteilt auf zwei 3 km breite Schifffahrtskorridore. Eine Blockade würde die Weltwirtschaft massiv erschüttern."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Archipel im Indischen Ozean besteht aus 115 Granitinseln und ist kein Teil des pazifischen Feuerrings?",
        answerA = "Malediven",
        answerB = "Seychellen",
        answerC = "Komoren",
        answerD = "Lakkadiven",
        correctAnswer = 1,
        explanation = "Die Seychellen bestehen aus 115 Inseln, von denen die inneren Inseln (Mahé, Praslin, La Digue) einzigartig sind: Sie sind die einzigen ozeanischen Granitinseln der Welt. Die äußeren Inseln sind Korallenatollen.",
        difficulty = 3,
        funFact = "Die Granitinseln der Seychellen sind Reste eines alten Mikrokontinents (Zealandia des Indischen Ozeans), der sich vor ca. 75 Millionen Jahren von Indien abspaltete."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gebirgspass verbindet Afghanistan mit Pakistan und war historisch die wichtigste Handels- und Invasionsroute zwischen Zentralasien und dem indischen Subkontinent?",
        answerA = "Wakhan-Korridor",
        answerB = "Khyber-Pass",
        answerC = "Bolan-Pass",
        answerD = "Lowari-Pass",
        correctAnswer = 1,
        explanation = "Der Khyber-Pass (Chaibar-Pass) im Hindukusch liegt auf 1.070 m Höhe und war seit der Antike die bedeutendste Verbindungsroute zwischen Zentralasien und dem indischen Subkontinent. Alexander der Große, die Moguln und später die Briten nutzten ihn.",
        difficulty = 3,
        funFact = "Der Khyber-Pass ist nur etwa 3–5 km breit an seiner engsten Stelle. Heute verläuft dort auch eine Eisenbahnstrecke, die als technisches Meisterwerk gilt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher geomorphologische Prozess beschreibt die seitliche Erosion eines Flusses, der ein breites Tal mit mäandrierenden Schleifen schafft?",
        answerA = "Tiefenerosion",
        answerB = "Lateralerosion (Seitenerosion)",
        answerC = "Rückwärtserosion (Rückschreitende Erosion)",
        answerD = "Akkumulation",
        correctAnswer = 1,
        explanation = "Lateralerosion (Seitenerosion) tritt auf, wenn ein Fluss in flachem Gelände nicht mehr tief erodiert, sondern seitlich mäandert. Dabei entsteht am Prallhang Erosion, am Gleithang Ablagerung – so entstehen die typischen Mäander-Schleifen.",
        difficulty = 3,
        funFact = "Wenn ein Mäander zu stark gebogen wird, kann der Fluss den Hals des Mäanders durchschneiden und eine abgetrennte ovale Rinne hinterlassen – den sogenannten Altwasser (Oxbow Lake)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das tiefste Gebiet des Mittelmeers?",
        answerA = "Adriatisches Meer – 1.230 m",
        answerB = "Tyrrhenisches Becken – 3.785 m",
        answerC = "Calypso-Tiefe (Ionisches Meer) – 5.267 m",
        answerD = "Levantinisches Becken – 4.580 m",
        correctAnswer = 2,
        explanation = "Die Calypso-Tiefe im Ionischen Meer ist mit 5.267 m der tiefste bekannte Punkt des Mittelmeeres. Sie liegt im Hellenischen Graben südlich Griechenlands, wo die Afrikanische Platte unter die Eurasische subduziert.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Amazonas-Delta-Gebiet beherbergt das Mündungsgebiet des größten Flusssystems der Erde mit der charakteristischen Ilha de Marajó?",
        answerA = "Orinoco-Delta (Venezuela)",
        answerB = "Amazonas-Ästuar (Brasilien)",
        answerC = "Rio de la Plata-Mündung (Argentinien)",
        answerD = "São Francisco-Delta (Brasilien)",
        correctAnswer = 1,
        explanation = "Das Amazonas-Ästuar in Nordbrasilien bildet mit der Insel Marajó (42.000 km² – so groß wie die Schweiz) eines der markantesten Flussmündungsgebiete der Erde. Der Amazonas mündet mit über 200 km Breite in den Atlantik.",
        difficulty = 3,
        funFact = "Das Phänomen 'Pororoca' tritt im Amazonas-Ästuar auf: Eine Gezeitenwelle (Bore Tide) läuft als riesige Welle bis zu 800 km flussaufwärts – Surfer nutzen sie für Weltrekord-Wellenritte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Subduktionszone im westlichen Pazifik hat die tiefste Subduktionsrate der Welt mit bis zu 24 cm pro Jahr?",
        answerA = "Izu-Bonin-Subduktionszone",
        answerB = "Tonga-Subduktionszone",
        answerC = "Mariana-Subduktionszone",
        answerD = "Japan-Subduktionszone",
        correctAnswer = 1,
        explanation = "Die Tonga-Subduktionszone, wo die Pazifische Platte unter die Australische (Tonga-)Platte taucht, weist mit ca. 24 cm pro Jahr die schnellste Subduktionsrate der Erde auf. Sie ist auch für extrem häufige Erdbeben bekannt.",
        difficulty = 3,
        funFact = "Die Tonga-Subduktionszone erzeugte im Januar 2022 den Ausbruch des Unterwasservulkans Hunga Tonga-Hunga Ha'apai – die stärkste Explosion in jüngster Zeit, die eine Druckwelle rund um die Erde sandte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Grabenbruchsystem liegt in Nordamerika und hat den Boden des Rio Grande geformt?",
        answerA = "Basin and Range Province",
        answerB = "Rio Grande Rift",
        answerC = "Colorado Plateau",
        answerD = "Wasatch Fault Zone",
        correctAnswer = 1,
        explanation = "Der Rio Grande Rift ist ein kontinentaler Grabenbruch, der sich von Mexiko durch New Mexico bis nach Colorado erstreckt. Entlang dieses aktiven Rifts liegt das Tal des Rio Grande; die Kruste dehnt sich hier aktiv aus.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge trennt die afrikanische Insel Madagaskar vom Festland Mosambiks und wie breit ist sie an ihrer schmalsten Stelle?",
        answerA = "Mozambique-Kanal – ca. 420 km",
        answerB = "Madagaskar-Straße – ca. 80 km",
        answerC = "Komoren-Passage – ca. 300 km",
        answerD = "Mozambique-Kanal – ca. 120 km",
        correctAnswer = 0,
        explanation = "Der Mozambique-Kanal zwischen Madagaskar und dem mosambikanischen Festland ist an seiner schmalsten Stelle etwa 420 km breit und etwa 1.700 km lang. Er ist für seinen starken Agulhas-Strom bekannt.",
        difficulty = 3,
        funFact = "Der Mozambique-Kanal war eine wichtige Handelsroute der arabischen Seefahrt und gilt als Teil der alten 'Gewürzroute' zwischen dem Indischen Ozean und dem südlichen Afrika."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Archipel im nördlichen Atlantik umfasst 18 Inseln vulkanischen Ursprungs und gehört zu Portugal?",
        answerA = "Kapverden",
        answerB = "Azoren",
        answerC = "Madeira",
        answerD = "Kanaren",
        correctAnswer = 1,
        explanation = "Die Azoren bestehen aus 9 Hauptinseln (nicht 18) und liegen auf dem Mittelatlantischen Rücken, wo drei tektonische Platten (Nordamerikanisch, Eurasisch, Afrikanisch) zusammentreffen. Sie gehören zu Portugal.",
        difficulty = 3,
        funFact = "Die Azoren sind tektonisch aktiv und liegen auf dem Azoren-Dreieck-Treffen dreier Platten – weltweit einzigartig. Surtsey-ähnliche Vulkane können jederzeit neue Inseln entstehen lassen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der Alpenpas, der die niedrigste Überquerungsmöglichkeit der Alpen auf der Schweizer-Italienischen Grenze bietet und seit der Römerzeit genutzt wird?",
        answerA = "Großer Sankt Bernhard (2.469 m)",
        answerB = "Gotthard-Pass (2.091 m)",
        answerC = "Simplon-Pass (2.005 m)",
        answerD = "Maloja-Pass (1.815 m)",
        correctAnswer = 3,
        explanation = "Der Maloja-Pass (1.815 m ü.NN) im Kanton Graubünden ist mit 1.815 m der niedrigste große Alpenpass und verbindet das Engadin in der Schweiz mit dem Bergell und der Lombardei. Allerdings ist der Brennerpass (1.374 m, Österreich-Italien) der niedrigste aller Alpenpässe.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher geomorphologische Prozess erklärt das wellenförmige Sanddünenfeld in Wüsten und wie entstehen Barchane?",
        answerA = "Fluviale Akkumulation durch saisonale Überschwemmungen",
        answerB = "Äolische (windbedingte) Sedimenttransport und Akkumulation",
        answerC = "Glaziale Ablagerung durch schmelzende Eisdecken",
        answerD = "Marine Abrasion durch Wellenbewegungen",
        correctAnswer = 1,
        explanation = "Barchane sind sichelförmige Wanderdünen, die durch äolischen (windbedingten) Sedimenttransport entstehen. Sand wird auf der Luvseite abgetragen und auf der Leeseite abgelagert – die Hörner der Sichel zeigen in Windrichtung.",
        difficulty = 3,
        funFact = "Barchane wandern mit dem Wind: In der Sahara können sie sich mehrere Meter pro Jahr bewegen und ganze Oasen verschütten. Auf dem Mars wurden ähnliche Dünenstrukturen entdeckt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief ist die Philippine Sea-Tiefe (Emden Deep), der tiefste Teil des Philippinengrabens?",
        answerA = "9.373 m",
        answerB = "10.540 m",
        answerC = "8.890 m",
        answerD = "11.034 m",
        correctAnswer = 1,
        explanation = "Der Philippinengraben erreicht am Emden Deep (Philippine Trench) eine Tiefe von ca. 10.540 m und ist damit der dritttiefste Meeresgraben der Welt nach dem Challenger Deep (Marianengraben) und dem Horizon Deep (Tongagraben).",
        difficulty = 3,
        funFact = "Der Philippinengraben verläuft entlang der Ostküste der Philippinen über mehr als 1.320 km Länge. An ihm sinkt die Philippinische Platte unter die Eurasische Platte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte aktive Caldera-System Europas, das sich im Tyrrhenischen Meer vor Neapel befindet?",
        answerA = "Vesuv-Caldera",
        answerB = "Phlegräische Felder (Campi Flegrei)",
        answerC = "Ischia-Caldera",
        answerD = "Stromboli-Krater",
        correctAnswer = 1,
        explanation = "Die Phlegräischen Felder (Campi Flegrei) westlich von Neapel sind ein riesiges aktives Caldera-System von ca. 12 km Durchmesser. Sie sind für die 'bradyseismische' Bodenhebung und -senkung bekannt und gelten als eines der gefährlichsten Vulkansysteme Europas.",
        difficulty = 3,
        funFact = "Der gesamte Boden der Phlegräischen Felder hebt und senkt sich periodisch um Meter – ein Prozess namens Bradyseismismus. Teile der historischen Römerstadt Pozzuoli stehen dadurch seit Jahrhunderten unter und über Wasser."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss bildet das Ganges-Brahmaputra-Delta, das größte Flussdelta der Welt, gemeinsam mit dem Ganges?",
        answerA = "Indus",
        answerB = "Irrawaddy",
        answerC = "Brahmaputra",
        answerD = "Jamuna",
        correctAnswer = 2,
        explanation = "Das Ganges-Brahmaputra-Delta (Sundarbans) in Bangladesch und Indien entsteht durch die Vereinigung von Ganges und Brahmaputra. Mit über 100.000 km² Fläche ist es das größte Flussdelta der Welt.",
        difficulty = 3,
        funFact = "Der Brahmaputra entspringt in Tibet als Yarlung Tsangpo, fließt durch eine der tiefsten Schluchten der Erde (Yarlung Tsangpo Grand Canyon) und ändert in Bangladesch seinen Namen zu Jamuna."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief ist der Japan-Graben, der östlich der japanischen Hauptinsel verläuft und durch Subduktion der Pazifischen Platte entstand?",
        answerA = "7.025 m",
        answerB = "8.412 m",
        answerC = "9.504 m",
        answerD = "6.780 m",
        correctAnswer = 1,
        explanation = "Der Japan-Graben verläuft östlich der japanischen Hauptinseln und erreicht eine maximale Tiefe von etwa 8.412 m. Hier taucht die Pazifische Platte unter die Nordamerikanische (und Eurasische) Platte – diese Subduktion verursachte das Tōhoku-Erdbeben 2011.",
        difficulty = 3,
        funFact = "Das Tōhoku-Erdbeben 2011 (Magnitude 9,1) verursachte den Fukushima-Tsunami. Es bewegte die japanische Hauptinsel Honshū um fast 2,4 Meter ostwärts und verschob die Erdachse um mehrere Zentimeter."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Grabenbruch-System liegt unter dem Roten Meer und erklärt dessen Entstehung als junger Ozean?",
        answerA = "Ostafrikanischer Rift",
        answerB = "Afar-Spreizungszone (Afar Triple Junction)",
        answerC = "Arabisch-Afrikanischer Kontinentalrift",
        answerD = "Levantiner Grabenbruch",
        correctAnswer = 1,
        explanation = "Das Afar-Dreieck in Äthiopien/Eritrea/Dschibuti ist eine der seltenen Stellen, wo ein Mittelozeanischer Rücken auf dem Festland liegt. Das Rote Meer und der Golf von Aden entstanden durch Spreizung – das Rote Meer ist geologisch ein junger Ozean in der Entstehung.",
        difficulty = 3,
        funFact = "Am Afar-Dreieck treffen drei tektonische Platten zusammen: Afrikanische, Arabische und Somalische Platte. Hier lässt sich die Entstehung eines Ozeans in Echtzeit beobachten."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie breit ist die Straße von Malakka an ihrer engsten Stelle, durch die täglich Tausende von Handelsschiffen fahren?",
        answerA = "65 km",
        answerB = "40 km",
        answerC = "2,8 km",
        answerD = "15 km",
        correctAnswer = 1,
        explanation = "Die Straße von Malakka zwischen der malaiischen Halbinsel und Sumatra ist an ihrer schmalsten Stelle (nahe dem Phillips Channel) etwa 2,8 km breit – aber die nutzbare Fahrrinne misst etwa 40 km in der Breite an anderen Stellen. Die Engstelle liegt bei ca. 2,8 km.",
        difficulty = 3,
        funFact = "Die Straße von Malakka ist mit täglich über 80.000 Schiffen eine der verkehrsreichsten Schifffahrtsrouten der Welt. Etwa 25 % des weltweiten Handels und 50 % des Ölhandels passieren sie."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Archipel im südwestlichen Pazifik besteht aus über 333 Inseln und ist bekannt für seine aktiven Vulkane und Tamu-Massif-ähnliche Unterwassergeologie?",
        answerA = "Tonga",
        answerB = "Vanuatu",
        answerC = "Fidschi",
        answerD = "Salomonen",
        correctAnswer = 1,
        explanation = "Vanuatu besteht aus 83 bewohnten Inseln (und rund 250 insgesamt) im südwestlichen Pazifik und liegt im Bereich der Subduktion der Australischen unter die Pazifische Platte. Das Land hat einige der aktivsten Vulkane der Welt, darunter den Yasur auf Tanna.",
        difficulty = 3,
        funFact = "Der Ambae-Vulkan in Vanuatu gilt als einer der größten aktiven Schildvulkane des Pazifiks. 2017 und 2018 musste die gesamte Inselbevölkerung mehrfach evakuiert werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher alpine Gebirgspass auf der Schweizer-Österreichischen Grenze war die wichtigste Römerstraße ('Via Claudia Augusta') über die Alpen?",
        answerA = "Reschen-Pass (1.507 m)",
        answerB = "Brenner-Pass (1.374 m)",
        answerC = "Arlberg-Pass (1.793 m)",
        answerD = "Fernpass (1.210 m)",
        correctAnswer = 1,
        explanation = "Der Brenner-Pass (1.374 m) an der Grenze zwischen Österreich (Tirol) und Italien ist nicht nur der niedrigste Alpenpass auf der Hauptalpenkette, sondern war auch die wichtigste Römerstraße. Die Via Claudia Augusta verlief durch ihn von Altinum (nahe Venedig) bis zur Donau.",
        difficulty = 3,
        funFact = "Heute führt durch den Brennerpass die meistbefahrene Alpentransversale Europas. Der Brennerbasistunnel (derzeit im Bau) wird mit 64 km der längste Eisenbahntunnel der Welt werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Prozess der Geomorphologie beschreibt das Aufschmelzen von Permafrost und die dadurch entstehenden Thermokarst-Landschaften?",
        answerA = "Periglaziale Prozesse mit Solifluktion",
        answerB = "Thermokarst-Bildung durch Permafrost-Degradation",
        answerC = "Äolische Erosion in Tundraregionen",
        answerD = "Fluviale Zerschneidung durch Schneeschmelze",
        correctAnswer = 1,
        explanation = "Thermokarst entsteht, wenn Permafrost auftaut und das Bodenmaterial kollabiert. Es entstehen Senken, Seen (Thermokarstseeen), Hügel (Pingo) und unregelmäßige Geländeformen. Durch den Klimawandel breitet sich dieses Phänomen in Sibirien, Alaska und Kanada rapide aus.",
        difficulty = 3,
        funFact = "Beim Auftauen von Permafrost wird das darin gespeicherte organische Material zu CO₂ und Methan abgebaut – ein weiterer Klimarückkopplungseffekt, der die Erderwärmung verstärkt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer hat den höchsten Salzgehalt der Welt unter den randlichen Meeren mit natürlichem Zu- und Abfluss?",
        answerA = "Rotes Meer (42–43 ‰)",
        answerB = "Persischer Golf (38–40 ‰)",
        answerC = "Arabisches Meer (36–37 ‰)",
        answerD = "Mittelmeeer (37–39 ‰)",
        correctAnswer = 0,
        explanation = "Das Rote Meer hat mit einem Salzgehalt von etwa 40–43 ‰ den höchsten Salzgehalt aller Randmeere mit Ozean-Verbindung. Dies liegt an hoher Verdunstung und geringem Süßwasserzufluss in der trockenen Klimazone.",
        difficulty = 3,
        funFact = "Das Rote Meer hat keinen nennenswerten Fluss-Zufluss. Der gesamte Wasserhaushalt hängt vom Austausch mit dem Golf von Aden über den Bab al-Mandab ab."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Flussdelta im Nigerdelta (Nigeria) ist bekannt als größtes Mündungsgebiet Afrikas und wichtigstes Ölfördergebiet?",
        answerA = "Kongodelta (DR Kongo)",
        answerB = "Nigerdelta (Nigeria)",
        answerC = "Zambezi-Delta (Mosambik)",
        answerD = "Nil-Delta (Ägypten)",
        correctAnswer = 1,
        explanation = "Das Nigerdelta im Süden Nigerias ist das größte Flussdelta Afrikas und bedeckt ca. 70.000 km². Es enthält riesige Ölreserven und ist eines der wichtigsten Erdölfördergebiete Afrikas, leidet aber unter massiver Umweltverschmutzung.",
        difficulty = 3,
        funFact = "Das Nigerdelta ist eines der artenreichsten Mangrovengebiete der Welt. Gleichzeitig ist es durch Jahrzehnte der Ölförderung eine der am stärksten verschmutzten Regionen der Erde."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der tektonische Graben, der sich vom Libanon durch Syrien, Israel/Palästina und Jordanien bis zum Roten Meer erstreckt?",
        answerA = "Anatolische Störungszone",
        answerB = "Totes Meer-Transformverwerfung (Dead Sea Transform)",
        answerC = "Euphrat-Graben",
        answerD = "Mesopotamischer Graben",
        correctAnswer = 1,
        explanation = "Die Dead Sea Transform (Totes Meer-Verwerfung) ist eine Transformstörung, die sich vom Afar-Dreieck durch das Rote Meer, den Golf von Akaba, das Tote Meer, den Jordan und weiter bis in den Libanon erstreckt. An ihr gleiten die Arabische und Afrikanische Platte aneinander vorbei.",
        difficulty = 3,
        funFact = "Die Totes Meer-Transformverwerfung bewegt sich mit ca. 6 mm pro Jahr. Der tiefste Punkt der Erde auf dem Festland (Totes Meer, −430 m) liegt direkt an dieser tektonischen Grenze."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Caldera im Pazifischen Feuerring auf Sumatra entstand durch den Superausbruch des Toba-Vulkans vor ca. 74.000 Jahren?",
        answerA = "Krakatau-Caldera",
        answerB = "Toba-Caldera (Danau Toba)",
        answerC = "Tambora-Caldera",
        answerD = "Rinjani-Caldera",
        correctAnswer = 1,
        explanation = "Der Danau Toba auf Sumatra, Indonesien, ist der größte vulkanische See der Welt (100 × 30 km) und entstand durch den Supervulkan-Ausbruch des Toba vor ca. 74.000 Jahren. Dieser Ausbruch war so massiv, dass er möglicherweise die Weltbevölkerung auf wenige tausend Menschen reduzierte.",
        difficulty = 3,
        funFact = "Der Toba-Ausbruch ist die stärkste bekannte Vulkaneruption der letzten 25 Millionen Jahre. Er stieß schätzungsweise 2.800 km³ Material aus – das 100-fache des Tambora-Ausbruchs 1815."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Archipel besteht aus über 17.000 Inseln und gilt als der größte Inselstaat der Welt nach Anzahl der Inseln?",
        answerA = "Philippinen",
        answerB = "Indonesien",
        answerC = "Japan",
        answerD = "Malediven",
        correctAnswer = 1,
        explanation = "Indonesien besteht aus über 17.000 Inseln (offiziell 17.508 vermessen), von denen ca. 6.000 bewohnt sind. Als Inselstaat nach Anzahl der Inseln ist es der größte der Welt und erstreckt sich über mehr als 5.100 km von Ost nach West.",
        difficulty = 3,
        funFact = "Indonesien liegt auf dem Pazifischen Feuerring und hat über 130 aktive Vulkane – mehr als jedes andere Land der Welt. Etwa 10 % aller aktiven Vulkane der Erde befinden sich dort."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gebirgspass in den Himalaya (5.416 m) ist der höchste befahrbare Pass der Welt, der ganzjährig geöffnet ist?",
        answerA = "Khardung La (5.359 m, Indien)",
        answerB = "Semo La (5.565 m, Tibet)",
        answerC = "Mana-Pass (5.610 m, Indien)",
        answerD = "Chang La (5.360 m, Indien)",
        correctAnswer = 0,
        explanation = "Der Khardung La in Ladakh, Indien, mit 5.359 m gilt als einer der höchstgelegenen befahrbaren Motorstraßenpässe der Welt. Er verbindet Leh mit dem Nubra-Tal und ist bei Motorradfahrern und Jeep-Reisenden berühmt.",
        difficulty = 3,
        funFact = "Früher wurde der Khardung La fälschlicherweise mit 5.602 m angegeben – GPS-Messungen ergaben jedoch eine Höhe von ca. 5.359 m. Einige noch höhere Pässe in Tibet sind aber schwerer zugänglich."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Geomorphologieprozess formt Inselberglandschaften (Inselberge) wie den Uluru in Australien und entblößt harte Gesteinskerne?",
        answerA = "Fluviale Tiefenerosion",
        answerB = "Differenzielle Verwitterung (bevorzugte Abtragung weicherer Gesteine)",
        answerC = "Glaziale Überformung",
        answerD = "Tektonische Hebung isolierter Blöcke",
        correctAnswer = 1,
        explanation = "Inselberge wie der Uluru entstehen durch differentielle Verwitterung: Weiches Gestein wird über Jahrmillionen abgetragen, während härterer Gesteinskern (hier Arkose-Sandstein) stehen bleibt und als isolierter Felsrücken in der Ebene herausragt.",
        difficulty = 3,
        funFact = "Der Uluru (Ayers Rock) ist der sichtbare Gipfel eines viel größeren unterirdischen Gesteinsblocks. Nach Schätzungen reicht er noch ca. 5–6 km tief in den Boden hinein."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße zwischen Russland und den USA hat an ihrer engsten Stelle ca. 85 km Breite und zwei Inseln in ihrer Mitte?",
        answerA = "Beringsstraße mit den Diomede-Inseln",
        answerB = "Beringsstraße ohne Inseln",
        answerC = "Alëuten-Passage",
        answerD = "Sea of Okhotsk Passage",
        correctAnswer = 0,
        explanation = "Die Beringsstraße ist an ihrer engsten Stelle ca. 85 km breit und beherbergt in ihrer Mitte die Diomede-Inseln: Große Diomede (Russland) und Kleine Diomede (USA) – die nur 3,8 km voneinander entfernt sind und die internationale Datumsgrenze liegt zwischen ihnen.",
        difficulty = 3,
        funFact = "Zwischen Großer Diomede (Russland, wo es morgen ist) und Kleiner Diomede (USA, wo es noch gestern ist) liegen nur 3,8 km – und ein Zeitunterschied von 21 Stunden wegen der Datumsgrenze."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Caldera in Europa nach Fläche, die heute von einem See gefüllt wird?",
        answerA = "Laacher See (Deutschland, 3,3 km²)",
        answerB = "Bolsena-See (Italien, 113 km²)",
        answerC = "Bracciano-See (Italien, 57 km²)",
        answerD = "Vulsini-Caldera mit dem Bolsena-See",
        correctAnswer = 3,
        explanation = "Der Bolsena-See in der Toskana, Italien, liegt in der Caldera des erloschenen Vulsini-Vulkans und ist mit 113 km² die größte Caldera Europas, die von einem See gefüllt wird. Die Vulsini-Caldera entstand durch mehrere Kollapsereignisse vor 600.000–150.000 Jahren.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Grabenbruch in Sibirien ist der tiefste kontinentale Grabenbruch der Welt und beherbergt den Baikalsee?",
        answerA = "Baikal-Rift-Zone",
        answerB = "West-Sibirisches Rift",
        answerC = "Ob-Graben",
        answerD = "Jenissei-Rift",
        correctAnswer = 0,
        explanation = "Die Baikal-Rift-Zone ist ein aktiver kontinentaler Grabenbruch, der sich über ca. 1.500 km erstreckt. Der Baikalsee, der darin liegt, ist 1.637 m tief und enthält etwa 20 % des weltweiten flüssigen Süßwassers. Der Boden des Sees liegt über 8 km unter dem umliegenden Hochland (Sedimentfüllung eingerechnet).",
        difficulty = 3,
        funFact = "Die Baikal-Rift-Zone ist 25–30 Millionen Jahre alt – damit einer der ältesten aktiven Kontinentalrifts der Welt. Der Rift dehnt sich jährlich um ca. 2 cm aus."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Schlucht in den USA gilt als tiefstes Flussdurchbruchstal der Welt, das durch fluviale Tiefen- und Rückwärtserosion entstand?",
        answerA = "Grand Canyon (Arizona)",
        answerB = "Hells Canyon (Idaho/Oregon)",
        answerC = "Snake River Canyon",
        answerD = "Bryce Canyon",
        correctAnswer = 1,
        explanation = "Hells Canyon entlang des Snake River an der Grenze von Idaho und Oregon ist mit bis zu 2.436 m Tiefe die tiefste Schlucht Nordamerikas und eine der tiefsten der Welt – noch tiefer als der Grand Canyon (1.829 m). Sie entstand durch intensive fluviale Tiefenerosion.",
        difficulty = 3,
        funFact = "Der Grand Canyon ist zwar weltberühmter, aber Hells Canyon ist tiefer. Beide entstanden durch Kombination von tektonischer Hebung des Gebirges und gleichzeitiger Tiefenerosion durch Flüsse."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Archipel im Norden Kanadas gilt als größte Inselgruppe der Welt nach Gesamtfläche?",
        answerA = "Neusibirische Inseln (Russland)",
        answerB = "Kanadischer Arktischer Archipel",
        answerC = "Svalbard-Archipel (Norwegen)",
        answerD = "Inselgruppe der Großen Antillen",
        correctAnswer = 1,
        explanation = "Der Kanadische Arktische Archipel umfasst über 36.000 Inseln mit einer Gesamtfläche von ca. 1,4 Millionen km² – damit ist er die größte Inselgruppe der Welt. Dazu gehören Baffin Island (fünftgrößte Insel der Welt), Ellesmere Island und Victoria Island.",
        difficulty = 3,
        funFact = "Der Kanadische Arktische Archipel war bis ins 20. Jahrhundert weitgehend unerforscht. Im Sommer ist die Nordwestpassage durch ihn schiffbar – durch die Klimaerwärmung wird sie zunehmend eisfrei."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der geomorphologische Prozess, bei dem ein Gletscher durch sein Eigengewicht und Temperaturunterschiede am Boden entlangfließt und dabei Felsmaterial trägt?",
        answerA = "Nivation",
        answerB = "Glaziale Exaration (Abrasion und Erosion)",
        answerC = "Periglaziale Solifluktion",
        answerD = "Fluvioglaziale Akkumulation",
        correctAnswer = 1,
        explanation = "Glaziale Exaration (Gletschererosion) umfasst die Abrasion (Schmirgeln des Untergrunds durch mitgeführte Gesteinsbruchstücke) und die Detraktion (Herausreißen von Gesteinsblöcken). Diese Prozesse schaffen U-Täler, Kare und die charakteristischen Gletscherschliffe.",
        difficulty = 3,
        funFact = "Gletscherschliffe und Rundhöcker (polierte Felsköpfe) im Alpenvorland zeigen noch heute die Fließrichtung der eiszeitlichen Gletscher an – geologische 'Wegweiser' in der Landschaft."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Subduktionszone verursacht die regelmäßigen Erdbeben und Vulkane Japans durch Untertauchen der Pazifischen Platte?",
        answerA = "Izu-Bonin-Subduktionszone und Japan-Subduktionszone",
        answerB = "Nur die Japan-Subduktionszone",
        answerC = "Philippinische Platte-Subduktionszone",
        answerD = "Sagami-Subduktionszone allein",
        correctAnswer = 0,
        explanation = "Japan liegt im Überlappungsbereich mehrerer Subduktionszonen: Im Osten taucht die Pazifische Platte unter Japan-Graben und Izu-Bonin-Graben, im Süden schiebt sich die Philippinische Platte unter die Eurasische. Zusammen machen sie Japan zu einem der seismisch aktivsten Gebiete der Erde.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Ästuar gilt als das breiteste der Welt und liegt an der Mündung des Río de la Plata in Argentinien/Uruguay?",
        answerA = "Amazonas-Ästuar (Brasilien) – 330 km",
        answerB = "Río de la Plata-Ästuar – 220 km",
        answerC = "Ob-Ästuar (Russland) – 80 km",
        answerD = "Severn-Ästuar (UK) – 15 km",
        correctAnswer = 1,
        explanation = "Der Río de la Plata (Silberfluss) bildet an seiner Mündung in den Atlantik ein Ästuar von ca. 220 km Breite und 290 km Länge – das breiteste Flussästuar der Welt. Auf seinen Ufern liegen Buenos Aires (Argentinien) und Montevideo (Uruguay).",
        difficulty = 3,
        funFact = "Obwohl der Río de la Plata wie ein Meer wirkt, ist er kein Meer, sondern ein Ästuar: Ein halbgeschlossenes Gewässer, in dem Süß- und Salzwasser sich vermischen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Hochgebirgspass in den Pyrenäen war der wichtigste mittelalterliche Jakobsweg-Übergang nach Spanien?",
        answerA = "Col d'Aubisque (1.709 m)",
        answerB = "Port de Somport (1.632 m)",
        answerC = "Port de la Bonaigua (2.072 m)",
        answerD = "Puerto de Roncesvalles / Col de Roncevaux (1.057 m)",
        correctAnswer = 3,
        explanation = "Der Roncesvalles-Pass (Roncevaux, Ibañeta-Pass) auf 1.057 m Höhe war der klassische Jakobsweg-Übergang über die Pyrenäen auf dem Camino Francés. Hier starteten Pilger aus Frankreich ihre Reise nach Santiago de Compostela.",
        difficulty = 3,
        funFact = "Das Rolandslied (Chanson de Roland) handelt von der Niederlage von Rolands Nachhut im Tal von Roncesvalles 778 n. Chr. – einem der berühmtesten Ereignisse des frühen Mittelalters in den Pyrenäen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Prozess beschreibt die Bildung von Löss-Ablagerungen als feinkörniges Windablagerungsprodukt, das fruchtbare Böden der Lösszonen Chinas bildet?",
        answerA = "Fluviale Schwemmlehm-Ablagerung",
        answerB = "Äolische Löss-Akkumulation",
        answerC = "Glaziale Grundmoränen-Ablagerung",
        answerD = "Marines Sediment-Aufstieg durch Meeresspiegelabfall",
        correctAnswer = 1,
        explanation = "Löss entsteht durch äolischen (windbedingten) Transport und Ablagerung von Schluffpartikeln (Korngröße 0,002–0,063 mm). Das chinesische Lösshochland (Huangtu Gaoyuan) ist das größte Löss-Vorkommen der Welt und wurde durch Winde aus der Gobi-Wüste aufgebaut.",
        difficulty = 3,
        funFact = "Das Lösshochland in China ist bis zu 200 m tief von Löss bedeckt. Es enthält die Terrakotta-Armee des Kaisers Qin Shihuangdi – in Lössboden eingegraben und dadurch perfekt konserviert."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge zwischen Grönland und Norwegen ist für ihre Eisbergproduktion bekannt und bildet den Zugang zum Nordatlantik?",
        answerA = "Dänemark-Straße (70 km breit)",
        answerB = "Framstraße (500 km breit)",
        answerC = "Barents-Öffnung",
        answerD = "Jan-Mayen-Fracture-Zone",
        correctAnswer = 1,
        explanation = "Die Framstraße zwischen Grönland und Svalbard (Spitzbergen) ist mit ca. 500 km Breite das tiefste und breiteste Tor zwischen Arktischem Ozean und Atlantik. Durch sie fließt der Kalte Arktische Ausstrom nach Süden – und Eisberge treiben hindurch in den Atlantik.",
        difficulty = 3,
        funFact = "Der Titanic-Eisberg stammt wahrscheinlich aus einem grönländischen Gletscher und trieb durch die Framstraße in den Nordatlantik. Die meisten Atlantik-Eisberge haben diesen Ursprung."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Inselbogen-System im westlichen Pazifik entstand durch die Subduktion der Pazifischen Platte unter die Philippinische Platte?",
        answerA = "Bonin-Inseln (Ogasawara-Inseln, Japan)",
        answerB = "Marianen-Inseln",
        answerC = "Palau-Inseln",
        answerD = "Marshall-Inseln",
        correctAnswer = 1,
        explanation = "Die Marianen-Inseln bilden einen klassischen Inselbogen, der durch Subduktion der Pazifischen Platte unter die Philippinische Platte entstanden ist. Der Marianengraben liegt direkt östlich des Bogens und ist Ergebnis dieser Subduktion.",
        difficulty = 3,
        funFact = "Die nördlichste Marianenalter hat aktiven Vulkanismus – darunter der Pagan-Vulkan und der Farallon de Pajaros (Uracas). Die südlichste Insel, Guam, ist das bevölkerungsreichste US-Territorium im Pazifik."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der geomorphologische Prozess, bei dem Wellen Kliffs rückschreitend erodieren und Plattformen (Abrasionsplattformen) zurücklassen?",
        answerA = "Marine Abrasion (Strandwall-Bildung)",
        answerB = "Marine Regression",
        answerC = "Ufererosion durch Brandung (Klifferosion und Wellenschliff)",
        answerD = "Eustasie-bedingte Küstentransformation",
        correctAnswer = 2,
        explanation = "Klifferosion durch Brandung (marine Abrasion) ist der Prozess, bei dem Wellen das Kliff an seiner Basis unterhöhlen, bis es kollabiert. Zurück bleibt eine flache Abrasionsplattform. Dieser Prozess formt klassische Kliffküsten wie die Kreideküste der Normandie oder die Weißen Klippen von Dover.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Tiefseeregion des Indischen Ozeans hat mit dem Java-Graben (Sundagraben) eine Tiefe von etwa 7.258 m und liegt südlich welcher Insel?",
        answerA = "Südlich von Sumatra",
        answerB = "Südlich von Java",
        answerC = "Südlich von Bali",
        answerD = "Südlich der Banda-Inseln",
        correctAnswer = 1,
        explanation = "Der Sundagraben (Java-Graben) verläuft in einem Bogen südlich von Sumatra und Java und erreicht südlich von Java seine größte Tiefe von ca. 7.258 m (Argo Abyssal Plain). Er ist der tiefste Punkt des Indischen Ozeans.",
        difficulty = 3,
        funFact = "Der Sundagraben ist seismisch und geologisch für den Tsunami 2004 verantwortlich (Sumatra-Andaman-Beben, M 9,1–9,3), der über 230.000 Menschen tötete – eine der verheerendsten Naturkatastrophen der Geschichte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Alpenpas auf der deutsch-österreichischen Grenze (Zugspitzmassiv) ist durch eine Seilbahn erschlossen und hat die Zugspitze als höchsten deutschen Gipfel?",
        answerA = "Zugspitzplatt (2.600 m)",
        answerB = "Scharnitz-Pass (964 m)",
        answerC = "Fernpass (1.209 m)",
        answerD = "Reutte-Pass",
        correctAnswer = 0,
        explanation = "Das Zugspitzplatt ist die Gletscherhochfläche am Fuß der Zugspitze (2.962 m), die per Zahnradbahn und Seilbahn erschlossen wird. Die Zugspitze an der deutsch-österreichischen Grenze ist der höchste Berg Deutschlands.",
        difficulty = 3,
        funFact = "Die Zugspitze hat drei Gipfelseilbahnen: von Ehrwald (Österreich), von Garmisch-Partenkirchen (Zugspitzbahn), und eine Gondel über den Grat. Auf dem Gipfel stehen sowohl deutsches als auch österreichisches Hoheitszeichen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher geomorphologische Typ von Küste entsteht, wenn ein ehemaliges Gebirgstal durch Meeresspiegelanstieg überflutet wird und eine trichterförmige Meeresbuch bildet?",
        answerA = "Haff-Küste",
        answerB = "Ria-Küste",
        answerC = "Mangroven-Küste",
        answerD = "Ausgleichsküste",
        correctAnswer = 1,
        explanation = "Eine Ria (Ría) ist eine trichterförmige Meeresbucht, die entsteht, wenn ein Flussmündungstal (im Küstenbereich) durch Meerespiegelanstieg oder tektonische Absenkung überflutet wird. Klassische Ria-Küsten finden sich in Galicien (Spanien) und in Irland.",
        difficulty = 3,
        funFact = "Die berühmten 'Rías Altas' und 'Rías Bajas' in Galicien, Spanien, sind eine der schönsten Ria-Küstenlandschaften der Welt. Sie entstanden am Ende der letzten Eiszeit durch den globalen Meeresspiegelanstieg."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Archipel im Südpazifik hat die höchste Konzentration aktiver Unterwasservulkane und liegt nördlich von Neuseeland?",
        answerA = "Kermadec-Inseln (Neuseeland)",
        answerB = "Tonga-Inseln",
        answerC = "Samoa-Inseln",
        answerD = "Cookinseln",
        correctAnswer = 0,
        explanation = "Die Kermadec-Inseln nördlich von Neuseeland liegen auf der Kermadec-Subduktionszone und haben eine außergewöhnliche Konzentration aktiver Unterwasservulkane. Der Kermadec-Graben erreicht bis zu 10.047 m Tiefe.",
        difficulty = 3,
        funFact = null
    ),

    // ── HARD (difficulty = 3) ── 50 additional questions (batch 5) ───────────

    Question(
        categoryId = 1,
        questionText = "Auf welcher Höhe liegt die alpine Baumgrenze (Waldgrenze) in den Zentralalpen der Schweiz typischerweise?",
        answerA = "ca. 1.200–1.400 m ü.NN",
        answerB = "ca. 1.600–1.800 m ü.NN",
        answerC = "ca. 2.000–2.300 m ü.NN",
        answerD = "ca. 2.500–2.800 m ü.NN",
        correctAnswer = 2,
        explanation = "In den Zentralalpen (Schweizer Alpen, Innsbruck-Region) liegt die klimatisch bedingte Baumgrenze bei etwa 2.000–2.300 m ü.NN. In Nordlagen und nach Norden hin sinkt sie, in Südlagen kann sie bis 2.400 m reichen.",
        difficulty = 3,
        funFact = "Die Baumgrenze ist keine feste Linie, sondern eine Zone: Unterhalb steht geschlossener Wald, darüber verstreute Einzelbäume ('Kampfzone'), noch höher nur Gebüsch und Matten."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Höhe liegt die Baumgrenze am Kilimandscharo in Tansania?",
        answerA = "ca. 1.800 m",
        answerB = "ca. 2.800 m",
        answerC = "ca. 3.500 m",
        answerD = "ca. 4.200 m",
        correctAnswer = 1,
        explanation = "Am Kilimandscharo liegt die Baumgrenze bei etwa 2.800 m, wo der montane Regenwald in Heideland übergeht. Darunter befindet sich ein Gürtel aus Bergnebelwald, darüber folgt die Heide- und Moorzone.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wo liegt die Baumgrenze in den tropischen Anden Perus im Vergleich zu den Alpen?",
        answerA = "Niedriger: ca. 1.500–2.000 m",
        answerB = "Ähnlich: ca. 2.000–2.200 m",
        answerC = "Höher: ca. 3.500–4.000 m",
        answerD = "Wesentlich höher: ca. 4.800–5.200 m",
        correctAnswer = 2,
        explanation = "In den tropischen Anden Perus und Boliviens liegt die Baumgrenze bei etwa 3.500–4.000 m – deutlich höher als in den Alpen (2.000–2.300 m). Der Grund ist die ganzjährig hohe Sonneneinstrahlung in Äquatornähe.",
        difficulty = 3,
        funFact = "Die Polylepis-Bäume (Queñua) wachsen in den Anden bis auf 5.200 m Höhe und sind damit die höchstgelegenen Bäume der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Bodentyp dominiert die borealen Nadelwälder (Taiga) Sibiriens und Kanadas und ist durch starke Auswaschung (Podsolierung) gekennzeichnet?",
        answerA = "Chernozem",
        answerB = "Laterit",
        answerC = "Podsol",
        answerD = "Andosol",
        correctAnswer = 2,
        explanation = "Podsole sind die typischen Böden der borealen Taiga-Zone. Durch saure Nadelstreu und hohe Niederschläge werden Nährstoffe und Eisenoxide aus dem Oberboden ausgewaschen (Eluviation) und im Unterboden angereichert (Illuviation).",
        difficulty = 3,
        funFact = "Der Name 'Podsol' kommt aus dem Russischen und bedeutet 'aschgrauer Boden' – der ausgewaschene Oberhorizont sieht tatsächlich wie Asche aus."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Bodentyp bildet die fruchtbarsten Schwarzerdeböden der osteuropäischen und nord-amerikanischen Steppen?",
        answerA = "Ferralsol",
        answerB = "Chernozem (Schwarzerde)",
        answerC = "Kastanozem",
        answerD = "Vertisol",
        correctAnswer = 1,
        explanation = "Chernozem (Schwarzerde) ist der fruchtbarste Bodentyp der Welt, geprägt durch dicke, humusreiche Oberschichten. Er dominiert die ukrainischen und russischen Steppen sowie die nordamerikanischen Prärien (Great Plains) und bildet die Grundlage der bedeutendsten Getreidekammern der Welt.",
        difficulty = 3,
        funFact = "Die Ukraine mit ihren Chernozem-Böden produziert etwa 10 % des Weltweizens. Der Begriff 'Schwarzerde' bezeichnet buchstäblich die schwarze Farbe des humusreichen Bodens."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Bodentyp dominiert die tropischen Regenwälder und ist durch starke Lateritisierung (Ferralsol) extrem nährstoffarm?",
        answerA = "Andosol",
        answerB = "Gleysol",
        answerC = "Ferralsol (Latosol/Laterit)",
        answerD = "Rendzina",
        correctAnswer = 2,
        explanation = "Ferralsole (Laterit-Böden) dominieren tropische Regenwälder. Intensive chemische Verwitterung unter hoher Wärme und Feuchtigkeit löst Silizium und Nährstoffe aus dem Boden, zurück bleiben Eisen- und Aluminiumoxide – ein paradoxes Phänomen: üppige Vegetation auf nährstoffarmen Böden.",
        difficulty = 3,
        funFact = "Wenn tropischer Regenwald abgeholzt wird, verliert der Ferralsol seinen Schutz. Nach wenigen Jahren wird er durch Austrocknung zu hartem Lateritstein, auf dem kaum noch Landwirtschaft möglich ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher biogeografische Übergang markiert die Grenze zwischen der tropischen und subtropischen Savanne und der Wüstenzone in Afrika?",
        answerA = "Der 15. Breitengrad nördlicher Breite",
        answerB = "Die isohyete 250 mm Jahresniederschlag",
        answerC = "Der Sahel (zwischen Savanne und Sahara)",
        answerD = "Der Tschadsee-Meridian",
        correctAnswer = 1,
        explanation = "Die Isohyete von 250 mm Jahresniederschlag markiert in Afrika annähernd die Grenze zwischen Sahel/Savanne und Wüste. Unterhalb dieser Niederschlagsmenge kann sich kein dauerhafter Pflanzenwuchs mehr etablieren.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Monat setzt der Südwest-Monsun typischerweise über Kerala, der Südwestküste Indiens, ein?",
        answerA = "April",
        answerB = "Juni (ca. 1. Juni)",
        answerC = "Juli",
        answerD = "August",
        correctAnswer = 1,
        explanation = "Der Südwest-Monsun trifft traditionell um den 1. Juni an der Südwestküste Indiens (Kerala) ein. Dies gilt als offizieller Monsunbeginn für den indischen Subkontinent; das Datum variiert jährlich um wenige Tage.",
        difficulty = 3,
        funFact = "Der indische Wetterdienst (IMD) gibt den 'normalen' Monsunbeginn für Kerala als 1. Juni an. Für Delhi im Norden wartet man bis Ende Juni – der Monsun braucht ca. 45 Tage, um ganz Indien zu überqueren."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Monat setzt der Ostasiatische Monsun (Meiyu/Baiu) typischerweise im Jangtse-Becken Chinas ein?",
        answerA = "März",
        answerB = "Mai",
        answerC = "Juni (Mitte Juni)",
        answerD = "September",
        correctAnswer = 2,
        explanation = "Der Meiyu-Monsun (chinesisch: Pflaumenregen) setzt im Jangtse-Becken Chinas typischerweise Mitte Juni ein und hält bis Mitte Juli an. Er ist das asiatische Äquivalent des japanischen 'Baiu' (Tsuyu) und bringt lang anhaltende Niederschläge.",
        difficulty = 3,
        funFact = "Der Name 'Meiyu' (Pflaumenregen) kommt davon, dass die Regen zur Pflaumenreifezeit einsetzt. In Japan heißt diese Regenperiode 'Tsuyu' und ist für Pilze, Schimmel und dramatische Überschwemmungen bekannt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Ophiolith-Komplex in Oman gilt als bestes erhaltenes Beispiel ozeanischer Kruste auf dem Festland?",
        answerA = "Troodos-Ophiolith (Zypern)",
        answerB = "Semail-Ophiolith (Oman)",
        answerC = "Bay-of-Islands-Ophiolith (Kanada)",
        answerD = "Vourinos-Ophiolith (Griechenland)",
        correctAnswer = 1,
        explanation = "Der Semail-Ophiolith in Oman und den VAE ist der größte und am besten erhaltene Ophiolith-Komplex der Welt. Er erstreckt sich über ca. 600 km und zeigt das vollständige Profil ozeanischer Kruste: Peridotit (Mantel), Gabbro, Pillow-Laven und ozeanische Sedimente.",
        difficulty = 3,
        funFact = "Ophiolithe sind Fragmente ozeanischer Kruste, die durch tektonische Obduktion auf Kontinente aufgeschoben wurden, anstatt in einer Subduktionszone zu versinken – ein seltenes und wichtiges geologisches Ereignis."
    ),

    Question(
        categoryId = 1,
        questionText = "Was sind Ophiolithe und welche Gesteinsabfolge ist für sie typisch?",
        answerA = "Erstarrungsgesteine aus Kontinentalkollisionen; Granit–Gneis–Marmor",
        answerB = "Fragmente ozeanischer Kruste auf Kontinenten; Peridotit–Gabbro–Basalt–Sediment",
        answerC = "Metamorphe Gesteine aus Subduktionszonen; Blauschiefer–Eklogit",
        answerD = "Vulkanische Inselbögen; Andesit–Ryolith–Ignimbrit",
        correctAnswer = 1,
        explanation = "Ophiolithe sind tektonisch auf Festland aufgeschobene Stücke ozeanischer Kruste. Die klassische Abfolge von unten nach oben: Harzburgit/Peridotit (Erdmantel) → Gabbro (untere Ozeankruste) → Sheeted Dike Complex → Pillow-Basalt (MORB) → ozeanische Sedimente.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich der Troodos-Ophiolith, ein vollständiges Musterbeispiel mesozoischer Ozeankruste?",
        answerA = "Griechenland",
        answerB = "Türkei",
        answerC = "Zypern",
        answerD = "Libanon",
        correctAnswer = 2,
        explanation = "Der Troodos-Ophiolith auf Zypern ist ein klassischer Ophiolith-Komplex aus dem Mesozoikum (ca. 90 Ma alt). Er zeigt die vollständige Abfolge ozeanischer Kruste und ist eines der meistuntersuchten geologischen Objekte weltweit.",
        difficulty = 3,
        funFact = "Zypern verdankt seinen Namen möglicherweise dem griechischen Wort 'Kupfer' (Kypros) – die Insel war in der Antike die wichtigste Kupferquelle der Mittelmeerregion, geliefert von den Gabbro-Gesteinen des Ophioliths."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Korallenriffsystem ist das größte der Welt nach Ausdehnung?",
        answerA = "Mesoamerikanisches Barriereriff (Karibik)",
        answerB = "New-Caledonia-Barriereriff (Pazifik)",
        answerC = "Great Barrier Reef (Australien)",
        answerD = "Rotes Meer-Korallenriff",
        correctAnswer = 2,
        explanation = "Das Great Barrier Reef vor der Nordostküste Australiens ist mit über 2.300 km Länge und einer Fläche von ca. 344.400 km² das größte Korallenriffsystem der Welt. Es ist das einzige lebende Gebilde, das vom Weltraum aus sichtbar ist.",
        difficulty = 3,
        funFact = "Das Great Barrier Reef besteht aus etwa 2.900 einzelnen Riffs und 900 Inseln. Es beherbergt über 1.500 Fischarten, 4.000 Weichtierarten und 240 Vogelarten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Korallenriffsystem in der Karibik ist das größte der westlichen Hemisphäre und das zweitgrößte der Welt?",
        answerA = "Mesoamerikanisches Barriereriff (Belize, Mexiko, Honduras, Guatemala)",
        answerB = "Florida Keys-Riff (USA)",
        answerC = "Bahamas-Riffsystem",
        answerD = "Kubanisches Korallenriff",
        correctAnswer = 0,
        explanation = "Das Mesoamerikanische Barriereriff (auch Maya Reef oder Great Mayan Reef) erstreckt sich über ca. 1.000 km entlang der Küsten von Mexiko (Yucatán), Belize, Honduras und Guatemala. Es ist das größte Barriereriff in der westlichen Hemisphäre.",
        difficulty = 3,
        funFact = "Das Belize-Barriereriff, Teil des Mesoamerikanischen Systems, war Charles Darwins erstes beschriebenes Barriereriff. Er bezeichnete es als 'das prächtigste Riff in Westindien'."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Korallen-Dreieck (Coral Triangle) wird als die global artenreichste Meeresregion der Welt bezeichnet?",
        answerA = "Karibisches Dreieck (Kuba–Puerto Rico–Florida)",
        answerB = "Indisches Dreieck (Malediven–Sri Lanka–Andamanen)",
        answerC = "Coral Triangle (Indonesien–Malaysia–Papua-Neuguinea–Philippinen–Salomonen–Timor-Leste)",
        answerD = "Rotes Meer-Dreieck",
        correctAnswer = 2,
        explanation = "Das Coral Triangle im südwestlichen Pazifik umfasst sechs Länder (Indonesien, Malaysia, Papua-Neuguinea, Philippinen, Salomonen, Timor-Leste) und gilt als Epizentrum der marinen Artenvielfalt: 76 % aller Korallenarten und 37 % aller Rifffische kommen dort vor.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Rotes-Meer-Korallenriff gilt als eines der nördlichsten tropischen Korallenriffe der Welt?",
        answerA = "Aqaba-Riff (Golf von Aqaba, ca. 29°N)",
        answerB = "Suez-Riff (ca. 30°N)",
        answerC = "Dahlak-Archipel-Riff (Eritrea, ca. 16°N)",
        answerD = "Farasan-Inseln-Riff (Saudi-Arabien, ca. 17°N)",
        correctAnswer = 0,
        explanation = "Das Korallenriff im Golf von Aqaba (ca. 29°N) zählt zu den nördlichsten tropischen Korallenriffen der Welt. Trotz seiner hohen Breite gedeihen dort artenreiche Riffe, begünstigt durch die warmen, klaren Gewässer des Roten Meeres.",
        difficulty = 3,
        funFact = "Das Riff von Eilat/Aqaba ist für seine außerordentliche Widerstandsfähigkeit bekannt: Trotz steigender Meerestemperaturen bleiben diese Riffe weitgehend bleichungsfrei, weil sie an hitzetolerante Algen-Symbionten angepasst sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Mangrovenregion gilt als der größte zusammenhängende Mangrovenwald der Welt?",
        answerA = "Mangroven der Florida Everglades (USA)",
        answerB = "Sundarbans (Bangladesch/Indien)",
        answerC = "Mangroven des Niger-Deltas (Nigeria)",
        answerD = "Mangroven der Guianen (Südamerika)",
        correctAnswer = 1,
        explanation = "Die Sundarbans im Ganges-Brahmaputra-Delta (Bangladesch/Indien) bilden mit ca. 10.000 km² den größten zusammenhängenden Mangrovenwald der Welt. Sie sind UNESCO-Weltnaturerbe und Heimat der größten wilden Bengaltigerpopulation.",
        difficulty = 3,
        funFact = "In den Sundarbans wurden die Bengalischen Tiger zu berüchtigten 'Menschenfressern' – bis heute werden dort mehr Menschen von Tigern getötet als irgendwo sonst auf der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat prozentual den größten Anteil seiner Küstenlänge mit Mangroven bedeckt?",
        answerA = "Südamerika",
        answerB = "Asien",
        answerC = "Afrika",
        answerD = "Australien/Ozeanien",
        correctAnswer = 0,
        explanation = "Südamerika (insbesondere Brasilien) hat den größten nationalen Mangrovenbestand der Welt. Brasilien allein besitzt ca. 26 % der globalen Mangrovenfläche. Südamerika dominiert damit den prozentualen Anteil an Küstenabschnitten mit Mangroven.",
        difficulty = 3,
        funFact = "Mangroven wachsen nur zwischen 25°N und 25°S (tropisch/subtropisch) und sind auf über 118 Länder verteilt. Sie schützen Küsten vor Tsunamis und Stürmen, binden CO₂ und sind Kinderstube für Fische."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das längste Höhlensystem der Welt nach erkundeter Ganglänge?",
        answerA = "Optimistychna-Höhle (Ukraine, ca. 261 km)",
        answerB = "Sistema Sac Actun (Mexiko, ca. 368 km)",
        answerC = "Mammoth Cave (USA, ca. 676 km)",
        answerD = "Lechuguilla Cave (USA, ca. 235 km)",
        correctAnswer = 2,
        explanation = "Das Mammoth-Cave-System in Kentucky, USA, ist mit über 676 km erkundeten Gängen das längste bekannte Höhlensystem der Welt. Es liegt in einer Kalksteinformation und wächst jedes Jahr durch neue Entdeckungen.",
        difficulty = 3,
        funFact = "Das Mammoth Cave System ist so groß, dass es noch immer neue Gänge gibt, die noch nicht kartiert wurden. Der Name kommt von der enormen Größe, nicht vom Mammut-Tier."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das zweitlängste Höhlensystem der Welt und liegt in Mexiko als Unterwasserhöhlensystem?",
        answerA = "Sistema Ox Bel Ha (Mexiko, ca. 270 km)",
        answerB = "Sistema Sac Actun (Mexiko, ca. 368 km)",
        answerC = "Jewel Cave (USA, ca. 330 km)",
        answerD = "Wind Cave (USA, ca. 248 km)",
        correctAnswer = 1,
        explanation = "Das Sistema Sac Actun auf der Yucatán-Halbinsel in Mexiko ist mit ca. 368 km das längste Unterwasserhöhlensystem (Cenote-System) der Welt und das zweitlängste Höhlensystem insgesamt. 2018 wurden Sac Actun und Dos Ojos als verbundenes System bestätigt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Höhle in Vietnam ist als größte Einzelhöhlenkammer der Welt bekannt?",
        answerA = "Phong Nha-Höhle",
        answerB = "Son Doong (Hang Sơn Đoòng)",
        answerC = "Thiên Đường-Höhle",
        answerD = "Tú Làn-Höhle",
        correctAnswer = 1,
        explanation = "Hang Sơn Đoòng in Vietnam (Phong Nha-Ke Bang Nationalpark) ist die größte Einzelhöhlenkammer der Welt: 5 km lang, bis zu 200 m hoch und 150 m breit. In ihr passen zwei Boeing 747 nebeneinander. Sie wurde 2009 erstmals erkundet.",
        difficulty = 3,
        funFact = "In Hang Sơn Đoòng gibt es ein eigenes Ökosystem mit Dschungel und Fluss im Inneren, da zwei 'Doline' (Einsturzöffnungen) Licht und Samen hereinlassen. Wolken bilden sich im Inneren der Höhle."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Kontinent-Kontinent-Kollisionszone gilt als die aktivste und jüngste der Erde und wird durch die Arabia-Eurasien-Kollision gebildet?",
        answerA = "Indien–Eurasien-Kollisionszone (Himalaya)",
        answerB = "Afrika–Eurasien-Kollisionszone (Alpen/Atlas)",
        answerC = "Arabien–Eurasien-Kollisionszone (Zagros/Alborz)",
        answerD = "Nazca–Südamerika-Kollisionszone (Anden)",
        correctAnswer = 2,
        explanation = "Die Arabien-Eurasien-Kollisionszone ist geologisch sehr aktiv und jünger als die Himalaya-Zone. Die arabische Platte schiebt sich nordwärts unter die eurasische und faltete die Zagros-Berge (Iran) und den Alborz (Nordiran). Die Kollisionsrate beträgt ca. 2–3 cm/Jahr.",
        difficulty = 3,
        funFact = "Die Zagros-Gebirgsfalten sind besonders erdölreich: Über 60 % der weltweiten konventionellen Ölreserven liegen in Faltenstrukturen entlang dieser Kollisionszone – darunter die Ölfälder Saudi-Arabiens, Irans und Iraks."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Kollisionszone zwischen der Afrikanischen und der Eurasischen Platte hat die Alpen und den Atlas geformt?",
        answerA = "Die Tethys-Subduktionszone (Mesozoikum bis Tertiär)",
        answerB = "Die aktive Adria-Eurasien-Spreizungszone",
        answerC = "Die Transformverwerfung Afrikas im westlichen Mittelmeer",
        answerD = "Die Subduktion der Afrikanischen Platte unter Sizilien",
        correctAnswer = 0,
        explanation = "Die Alpen und der Atlas entstanden durch Subduktion und schließliche Kollision der Afrikanischen mit der Eurasischen Platte, als der Tethys-Ozean im Tertiär (vor 30–10 Ma) geschlossen wurde. Diese Kollision faltete die alpinen Deckensysteme.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welcher Höhe beginnt typischerweise die subalpine Baumgrenze in den Rocky Mountains der USA (Colorado, ca. 40°N)?",
        answerA = "ca. 2.000 m",
        answerB = "ca. 2.800–3.000 m",
        answerC = "ca. 3.400–3.600 m",
        answerD = "ca. 4.000 m",
        correctAnswer = 2,
        explanation = "In den Rocky Mountains Colorados (ca. 40°N) liegt die Baumgrenze bei etwa 3.400–3.600 m ü.NN – deutlich höher als in den Alpen (2.000–2.300 m) bei ähnlichem Breitengrad. Die kontinentale Lage mit trockeneren, wärmeren Sommern erlaubt dies.",
        difficulty = 3,
        funFact = "An der Baumgrenze der Rocky Mountains wächst die Bristlecone Pine (Pinus aristata), die mit über 4.700 Jahren zu den ältesten lebenden Organismen der Erde gehört."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche spezifische Biom-Grenze trennt den südamerikanischen tropischen Regenwald (Amazonas) von der Cerrado-Savanne?",
        answerA = "Die 1.500 mm Jahresniederschlags-Isohyete",
        answerB = "Der 10. Breitengrad südlicher Breite",
        answerC = "Das São-Francisco-Flusssystem",
        answerD = "Die Caatinga-Übergangszone (gradueller Übergang, keine scharfe Grenze)",
        correctAnswer = 3,
        explanation = "Die Grenze zwischen Amazonas-Regenwald und Cerrado (brasilianische Savanne) ist keine scharfe Linie, sondern ein gradueller Übergang über die Caatinga- und Cerradão-Übergangszone. Klimatisch entscheidend ist die Länge der Trockenzeit.",
        difficulty = 3,
        funFact = "Der Cerrado ist das artenreichste Savannensystem der Welt mit ca. 12.000 Pflanzenarten. Seit 1970 wurden über 50 % des ursprünglichen Cerrado durch Landwirtschaft (vor allem Soja) zerstört."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Bodentyp ist für den landwirtschaftlichen Anbau von Reis in den Überschwemmungsebenen Asiens am charakteristischsten?",
        answerA = "Andosol",
        answerB = "Anthrosol (Paddy-Boden)",
        answerC = "Histosol",
        answerD = "Gleysol",
        correctAnswer = 1,
        explanation = "Anthrosole (speziell Paddy-Böden oder Hydragric Anthrosols) entstehen durch jahrhunderte- bis jahrtausendelange Nassreis-Kultivierung. Sie haben eine verdichtete Pflugsohle (Plinthite), die Wasser hält, und anaerobe Bedingungen, die für Nassreis ideal sind.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Monat setzt der Westafrikamonsun (WAM) typischerweise in der Guineasone Westafrikas ein?",
        answerA = "März",
        answerB = "Mai",
        answerC = "Juli",
        answerD = "Oktober",
        correctAnswer = 1,
        explanation = "Der Westafrikanische Monsun (WAM) setzt typischerweise im Mai in der Guineasone (Küstenregion Westafrikas) ein, wenn der ITCZ (Innertropische Konvergenzzone) nach Norden wandert. Im Juli erreicht er seinen nördlichsten Punkt im Sahel.",
        difficulty = 3,
        funFact = "Der WAM bestimmt Leben und Tod in der Sahel-Zone: Ein schwaches Monsunjahr wie 1972–1973 verursachte die Sahel-Hungersnot, bei der Hunderttausende Menschen starben."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das längste Höhlensystem Europas?",
        answerA = "Hölloch (Schweiz, ca. 200 km)",
        answerB = "Optimistychna-Höhle (Ukraine, ca. 261 km)",
        answerC = "Eisriesenwelt (Österreich, ca. 42 km)",
        answerD = "Sistema del Trave (Spanien, ca. 74 km)",
        correctAnswer = 1,
        explanation = "Die Optimistychna-Höhle in der Ukraine ist mit ca. 261 km erkundeten Gängen das längste Höhlensystem Europas und das viertlängste der Welt. Sie liegt in Gipshorizonten im westlichen Podolien.",
        difficulty = 3,
        funFact = "Gips-Höhlen wie die Optimistychna entstehen anders als Kalk-Höhlen: Gips (Calciumsulfat) löst sich in Wasser schneller als Kalkstein, weshalb diese Höhlen oft labyrinthisch-weitverzweigte Grundrisse haben."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Kalksteinformation in Südostasien beherbergt das Phong-Nha-Ke-Bang-Höhlensystem und liegt in welchem Land?",
        answerA = "Karsthochland von Guilin (China)",
        answerB = "Zentralvietnam (Provinz Quảng Bình, Vietnam)",
        answerC = "Karstplateau von Vang Vieng (Laos)",
        answerD = "Kambodscha (Kampot-Kalkstein)",
        correctAnswer = 1,
        explanation = "Das Phong-Nha-Ke-Bang-Höhlensystem liegt in der Provinz Quảng Bình in Zentralvietnam in einer der ältesten Kalksteinformationen Asiens (400–450 Millionen Jahre alt). Hier befindet sich auch die größte Höhlenkammer der Welt, Hang Sơn Đoòng.",
        difficulty = 3,
        funFact = "Im Phong Nha-Ke Bang Nationalpark wurden bisher über 300 Höhlen entdeckt; die meisten sind noch nicht vollständig erkundet. Das Gebiet gilt als eine der bedeutendsten Karstlandschaften der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche biogeografische Grenze trennt die orientalische von der australasischen Tiergeografieregion und verläuft durch Indonesien?",
        answerA = "Lydekker-Linie",
        answerB = "Weber-Linie",
        answerC = "Wallace-Linie",
        answerD = "Huxley-Linie",
        correctAnswer = 2,
        explanation = "Die Wallace-Linie verläuft durch Indonesien zwischen Borneo und Sulawesi sowie zwischen Bali und Lombok. Westlich davon dominieren asiatische Faunaelemente (Elefanten, Tiger), östlich australasische (Beuteltiere, Paradiesvögel).",
        difficulty = 3,
        funFact = "Alfred Russel Wallace entdeckte diese biogeografische Trennlinie, als er bemerkte, dass die Fauna zweier benachbarter Inseln (Bali und Lombok, nur 35 km voneinander entfernt) vollständig verschieden war. Diese Beobachtung trug zur Evolutionstheorie bei."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Bodentyp dominiert mediterrane Klimaregionen (z.B. Mittelmeer, Kalifornien) und zeichnet sich durch dunkelroten Farbton und hohen Eisengehalt aus?",
        answerA = "Kastanozem",
        answerB = "Vertisol",
        answerC = "Terra Rossa (Chromic Luvisol)",
        answerD = "Regosol",
        correctAnswer = 2,
        explanation = "Terra Rossa ist ein charakteristischer Rotboden mediterraner Karstlandschaften. Er entsteht durch chemische Verwitterung von Kalkgestein unter mediterranem Klima; die rote Farbe kommt von Eisenoxid (Hämatit). Er ist typisch für die Adriaküste, Sizilien und Israel.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Kontinentalkollisionszone zwischen Indien und Eurasien hebt noch immer mit einer Rate von ca. 5 mm pro Jahr an?",
        answerA = "Die Zagros-Faltenzone (Iran)",
        answerB = "Der Himalaya (Indien–Eurasien-Kollision)",
        answerC = "Der Karakorum (Kollision Indischer Mikrokontinent)",
        answerD = "Das Tibetische Plateau (Randzone des Himalaya)",
        correctAnswer = 1,
        explanation = "Der Himalaya hebt sich noch immer um ca. 5 mm pro Jahr durch die fortgesetzte Kollision der Indischen mit der Eurasischen Platte. Gleichzeitig erodieren Wind und Wetter ca. 2–3 mm ab, sodass der Nettoanstieg geringer ist.",
        difficulty = 3,
        funFact = "Die Indische Platte bewegt sich mit ca. 5 cm/Jahr nordwärts – eine der schnellsten Plattengeschwindigkeiten weltweit. Seit der Kollision vor 50 Mio. Jahren hat sie den Himalaya um über 3.000 m angehoben."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Monat setzt der ostasiatische Sommermonsun typischerweise in Tokio, Japan ('Tsuyu') ein?",
        answerA = "April",
        answerB = "Mai",
        answerC = "Juni",
        answerD = "August",
        correctAnswer = 2,
        explanation = "Der japanische Tsuyu (Regenzeit) beginnt in Tokio typischerweise Anfang bis Mitte Juni. Die Regenfront (Baiu-Front) wandert von Süden nach Norden und erreicht Tokio nach Okinawa und Kyushu.",
        difficulty = 3,
        funFact = "Das Wort 'Tsuyu' (梅雨) bedeutet 'Pflaumenregen', weil die Pflaumenernte in dieser Niederschlagsperiode stattfindet. Das Ende der Tsuyu-Zeit, der Beginn des Hochsommers, wird vom japanischen Wetterdienst offiziell ausgerufen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Korallenriff im Pazifik ist das tiefste Atoll der Erde, das durch Subsidenz eines Vulkankegels entstand (Darwinsche Atollentstehung)?",
        answerA = "Bikini-Atoll (Marshallinseln)",
        answerB = "Eniwetok-Atoll (Marshallinseln)",
        answerC = "Ningaloo Reef (Australien)",
        answerD = "Cocos-Keeling-Atoll (Australien)",
        correctAnswer = 3,
        explanation = "Das Cocos (Keeling)-Atoll ist ein klassisches Darwin-Atoll: Ein ursprünglicher Vulkankegel sank ab (Subsidenz), der Korallensaum wuchs weiter aufwärts und bildete das heutige Atoll. Darwin formulierte hier 1836 seine Atollentstehungstheorie.",
        difficulty = 3,
        funFact = "Charles Darwin besuchte die Cocos-Keeling-Inseln 1836 während seiner Beagle-Reise. Seine Beobachtungen dort bildeten die Grundlage für seine Theorie zur Entstehung von Atollen durch Absenkung und Korallenwachstum."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Mangrovenart ist die am weitesten verbreitete der Welt und reicht bis nach Japan und Neuseeland?",
        answerA = "Rhizophora apiculata (Stelzenwurzel-Mangrove)",
        answerB = "Avicennia marina (Weiße Mangrove)",
        answerC = "Bruguiera gymnorrhiza (Knierohr-Mangrove)",
        answerD = "Sonneratia caseolaris (Käsebaum-Mangrove)",
        correctAnswer = 1,
        explanation = "Avicennia marina (Weiße Mangrove) hat die größte geografische Verbreitung aller Mangrovenarten und kann Temperaturen bis 0°C tolerieren. Sie wächst von Ostafrika bis Japan und Neuseeland und ist damit die kältetolerante Mangrovenart.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches geologische Phänomen erklärt, warum Ophiolithe auf Kontinenten zu finden sind, obwohl ozeanische Kruste normalerweise subduziert wird?",
        answerA = "Hotspot-Magmatismus schiebt ozeanische Kruste über Subduktionszonen hinaus",
        answerB = "Obduktion: ozeanische Kruste wird auf Kontinentkruste aufgeschoben statt subduziert",
        answerC = "Transformation: ozeanische Kruste metamorphosiert zu kontinentaler Kruste",
        answerD = "Spreizungspause: Meeresboden bleibt ohne Subduktion zurück",
        correctAnswer = 1,
        explanation = "Obduktion ist der Prozess, bei dem ozeanische Kruste statt zu subduzieren auf benachbarte Kontinentränder aufgeschoben wird. Dies geschieht bei Kontinent-Kontinent-Kollisionen, wenn die ozeanische Kruste zwischen zwei Kontinenten eingeklemmt wird.",
        difficulty = 3,
        funFact = "Obduktion ist das genaue Gegenteil von Subduktion. Sie ist selten und erklärt, warum Ophiolithe so wertvoll sind: Sie sind fenster in den sonst unzugänglichen Ozeanboden der Vergangenheit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das tiefste Höhlensystem der Welt nach vertikaler Ausdehnung (Tiefe)?",
        answerA = "Höhle von Arabika (Georgien, ca. −2.080 m)",
        answerB = "Veryovkina-Höhle (Georgien, ca. −2.212 m)",
        answerC = "Krubera-Höhle (Georgien, ca. −2.197 m)",
        answerD = "Lamprechtsofen (Österreich, ca. −1.632 m)",
        correctAnswer = 1,
        explanation = "Die Veryovkina-Höhle im Arabika-Massiv (Westkaukasus, Georgien) ist mit einer Tiefe von ca. 2.212 m die tiefste bekannte Höhle der Welt. Sie übertraf die benachbarte Krubera-Höhle (2.197 m), die zuvor Weltrekordhalter war.",
        difficulty = 3,
        funFact = "Das Arabika-Massiv im Westkaukasus (Abchasien/Georgien) ist das tiefste Karstmassiv der Erde. Es enthält mehrere der tiefsten Höhlen weltweit. Die extreme Tiefe liegt an der hohen Reliefenergie des Kaukasus."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Biom bildet den Übergang zwischen tropischem Regenwald und tropischer Savanne und ist durch Laubbäume mit Trockenzeit-Anpassung geprägt?",
        answerA = "Tropische Bergnebelwälder",
        answerB = "Tropischer Trocken-Laubwald (Monsunwald)",
        answerC = "Galeriewälder",
        answerD = "Saisonale Feuchtgebiete",
        correctAnswer = 1,
        explanation = "Tropische Trocken-Laubwälder (Monsunwälder) sind das Übergangsbiom zwischen Regenwald und Savanne. Die Bäume werfen in der Trockenzeit ihr Laub ab. Sie bedecken Teile Indiens, Mittelamerikas und Brasiliens (Caatinga-ähnliche Übergänge).",
        difficulty = 3,
        funFact = "Tropische Trockenwälder (Dry Tropical Forests) gelten als eines der am stärksten bedrohten Biome der Erde: Weniger als 1 % sind noch in ursprünglichem Zustand – wegen ihrer fruchtbaren Böden wurden sie fast vollständig gerodet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Korallenriffsystem ist das weltgrößte im Indischen Ozean und liegt auf dem Tiefseeplateau um die Malediven und Lakkadiven?",
        answerA = "Chagos-Atoll-System (Britisches Territorium im Indischen Ozean)",
        answerB = "Lakkadiven–Malediven–Chagos-Riffkette",
        answerC = "Mascarene-Plateau-Riffsystem",
        answerD = "Andamanen-Nicobar-Riff",
        correctAnswer = 1,
        explanation = "Die Lakkadiven–Malediven–Chagos-Riffkette bildet zusammen das ausgedehnteste Korallenriffsystem des Indischen Ozeans. Es erstreckt sich über ca. 2.000 km von den Lakkadiven im Norden bis zu den Chagos-Inseln im Süden.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Škocjan-Canyon (Škocjanske jame), ein UNESCO-Weltnaturerbe mit einer der eindrucksvollsten unterirdischen Schluchten der Welt?",
        answerA = "Kroatien",
        answerB = "Österreich",
        answerC = "Slowenien",
        answerD = "Bosnien-Herzegowina",
        correctAnswer = 2,
        explanation = "Die Škocjan-Höhlen (Škocjanske jame) liegen im Karst-Hochplateau Sloweniens und sind seit 1986 UNESCO-Weltnaturerbe. Sie enthalten eine der größten unterirdischen Schluchten der Welt, durch die der Fluss Reka strömt.",
        difficulty = 3,
        funFact = "Im Škocjan-System erreicht die unterirdische Schlucht eine Höhe von über 150 m. Der Fluss Reka verschwindet in den Höhlen und taucht erst ca. 40 km entfernt bei Lipica wieder auf."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Monsun bringt im November–Januar Niederschlag an die Ostküste Indiens (Coromandel-Küste) und ist dem regulären Sommermonsun entgegengesetzt?",
        answerA = "Nordost-Monsun (Wintermonsun)",
        answerB = "Südwest-Monsun (verlängerter Rückzug)",
        answerC = "Äquatorialer Gegenstrom-Monsun",
        answerD = "Zyklonaler Bengala-Bucht-Monsun",
        correctAnswer = 0,
        explanation = "Der Nordost-Monsun (Wintermonsun) trifft von Oktober bis Dezember auf die Coromandel-Küste Südindiens und Sri Lanka. Er bringt Regen von der Bengala-Bucht – während ganz Indien während der Trockensaison ist, hat dieser Küstenstreifen seine Hauptregenzeit.",
        difficulty = 3,
        funFact = "Chennai (früher Madras) liegt an der Coromandel-Küste und hat seinen Monsun-Hauptniederschlag im Oktober–Dezember, also zur Zeit, wenn der Rest Indiens trocken ist. 2015 verursachte ein außergewöhnlich starker Nordost-Monsun eine der schlimmsten Überschwemmungen in Chennai's Geschichte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Höhlensystem in Kentucky, USA, hat einen eigenständigen ökologischen Lebensraum mit blinden, pigmentfreien Höhlenfischen entwickelt?",
        answerA = "Carlsbad Caverns (New Mexico)",
        answerB = "Mammoth Cave (Kentucky)",
        answerC = "Luray Caverns (Virginia)",
        answerD = "Ruby Falls (Tennessee)",
        correctAnswer = 1,
        explanation = "Das Mammoth Cave System in Kentucky hat über Jahrmillionen ein eigenständiges Höhlen-Ökosystem entwickelt. Der Southern Cavefish (Typhlichthys subterraneus) ist ein blinder, pigmentloser Fisch, der ausschließlich in den Gewässern dieses Höhlensystems lebt.",
        difficulty = 3,
        funFact = "Das Mammoth Cave National Park (UNESCO-Welterbe) war im 19. Jahrhundert Ort einer frühen Tourismusindustrie – und auch für die Gewinnung von Kaliumnitrat (Salpeter) für Schießpulver im Krieg von 1812."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Biom-Grenze zwischen Tundra und borealer Taiga wird durch die July 10°C Isotherm definiert?",
        answerA = "Die Waldgrenze (Baumgrenze) der nördlichen Hemisphäre",
        answerB = "Die ITCZ-Wanderungsgrenze",
        answerC = "Die Permafrost-Südgrenze",
        answerD = "Die Schneegrenze der Arktis",
        correctAnswer = 0,
        explanation = "Die nördliche Baumgrenze (polares Waldgrenzland) zwischen Tundra und Taiga folgt annähernd der July 10°C Isotherm – der Linie, wo der wärmste Monat (Juli) im Durchschnitt maximal 10°C erreicht. Unterhalb dieser Temperatur können Bäume nicht mehr ausreichend Photosynthese betreiben.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Korallenriffsystem in Neukaledonien ist das größte kohärente Lagune-Riff-System der Welt?",
        answerA = "Lord Howe Island Marine Park",
        answerB = "Neu-Kaledonien-Barriereriff (Grand Lagon Sud und Nord)",
        answerC = "Coral Sea Islands Reef",
        answerD = "Rowley Shoals (Australien)",
        correctAnswer = 1,
        explanation = "Das Barriereriff Neukaledoniens (Französisch Polynesien) ist das zweitlängste Barriereriff der Welt (1.600 km) und umschließt die größte geschlossene Lagune der Welt (ca. 24.000 km²). Es wurde 2008 UNESCO-Weltnaturerbe.",
        difficulty = 3,
        funFact = "Die Lagune Neukaledoniens gilt als ozeanische Biodiversitäts-Hotspot und enthält zahlreiche endemische Meeresarten, darunter den Dugong (Seekuh) in bedrohter Population."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche biogeografische Subregion definiert die 'Sonoran-Wüste' als ein eigenständiges Biom und in welchen Ländern liegt sie?",
        answerA = "Nur in Mexiko (Baja California und Sonora-Bundesstaat)",
        answerB = "In den USA (Arizona, Südkalifornien) und Mexiko (Sonora, Baja California)",
        answerC = "Nur in den USA (Arizona, Nevada)",
        answerD = "Im Südwesten der USA und Nordmexiko bis Guatemala",
        correctAnswer = 1,
        explanation = "Die Sonoran-Wüste erstreckt sich über Teile der US-Bundesstaaten Arizona und Südkalifornien sowie die mexikanischen Bundesstaaten Sonora und Baja California. Sie ist die heißeste Wüste Nordamerikas und zeichnet sich durch den Saguaro-Kaktus als Leitart aus.",
        difficulty = 3,
        funFact = "Die Sonoran-Wüste ist ungewöhnlich reich an Biodiversität für eine Wüste: Sie beherbergt über 2.000 Pflanzenarten, darunter den berühmten Saguaro-Kaktus, der bis 15 m hoch werden und 150 Jahre alt werden kann."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte Höhlensystem Asiens?",
        answerA = "Hang Sơn Đoòng (Vietnam, als Einzelhöhle größte Kammer)",
        answerB = "Naktong-Höhle (Südkorea)",
        answerC = "Lechuguilla Cave (Mexiko)",
        answerD = "Shuanghedong-Höhlensystem (China, ca. 257 km)",
        correctAnswer = 3,
        explanation = "Das Shuanghedong-Höhlensystem in der Provinz Guizhou, China, ist mit ca. 257 km erkundeten Gängen das längste Höhlensystem Asiens. Es liegt in einem der weltweit größten Karstgebiete, dem Guizhou-Plateau.",
        difficulty = 3,
        funFact = "Das Guizhou-Plateau in Südchina ist eine der größten und tiefsten Karstlandschaften der Welt mit tausenden unterirdischen Flüssen, riesigen Dolinen ('Tiankengs') und weitreichenden Höhlensystemen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Bodentyp entsteht durch intensive Bewässerung in ariden Regionen und kann durch Versalzung zur Degradierung führen?",
        answerA = "Solonchak (Salzboden)",
        answerB = "Kastanozem",
        answerC = "Arenosol",
        answerD = "Fluvisol",
        correctAnswer = 0,
        explanation = "Solonchake sind Salz-Böden, die in ariden und semiariden Regionen entstehen, wo Verdunstung die Niederschläge weit übersteigt. Bei intensiver Bewässerung werden Salze kapillar an die Oberfläche transportiert und akkumulieren zu unfruchtbaren Salzböden.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Kollisionszone im östlichen Mittelmeer beschreibt die Subduktion der Afrikanischen Platte unter die Ägäische Platte und erzeugt Erdbeben in Griechenland und der Türkei?",
        answerA = "Levantinische Transformzone",
        answerB = "Hellenischer Bogen (Hellenische Subduktionszone)",
        answerC = "Nordanatolische Verwerfung",
        answerD = "Anatolische Kollisionszone",
        correctAnswer = 1,
        explanation = "Der Hellenische Bogen ist eine Subduktionszone, wo die Afrikanische Platte unter die Ägäische Mikro-Platte (Griechenland) taucht. Diese Zone erzeugt regelmäßig starke Erdbeben in der Region (Kreta, Ionisches Meer) und erzeugte 365 n. Chr. ein katastrophales Erdbeben.",
        difficulty = 3,
        funFact = "Der Hellenische Bogen ist eine der aktivsten seismischen Zonen Europas. Der Santorin-Ausbruch (ca. 1600 v. Chr.) – möglicherweise Ursprung des Atlantis-Mythos – ereignete sich an einem durch diese Tektonik verursachten Supervulkan."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das wichtigste Mangrovengebiet der Pazifikküste Südamerikas?",
        answerA = "Mangroven von Ecuador und Kolumbien (Guayaquil-Golf bis Tumbes)",
        answerB = "Mangroven von Chile (Atacama-Küste)",
        answerC = "Mangroven von Peru (Paracas-Halbinsel)",
        answerD = "Mangroven von Brasilien (Rio Grande do Norte)",
        correctAnswer = 0,
        explanation = "Die Mangroven des Guayaquil-Golfs in Ecuador und die angrenzende Küste Kolumbiens und Nordperus (Tumbes) bilden das wichtigste Mangrovengebiet der Pazifikküste Südamerikas. Ecuador hat prozentual über 50 % seiner Mangroven durch Garnelenzucht verloren.",
        difficulty = 3,
        funFact = "Ecuador war einst das Land mit der höchsten Mangrovenentwaldungsrate der Welt – für Garnelenfarmen. Heute versucht das Land, Mangroven wiederherzustellen, da ihr Ökosystemwert erkannt wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die längste Höhle in Deutschland, und in welchem Mittelgebirge liegt sie?",
        answerA = "Hermannshöhle im Kyffhäuser (Thüringen)",
        answerB = "Falkensteiner Höhle (Schwäbische Alb, Baden-Württemberg)",
        answerC = "Laichinger Tiefenhöhle (Schwäbische Alb)",
        answerD = "Hüttenbläserschacht (Schwäbische Alb)",
        correctAnswer = 1,
        explanation = "Das Höhlensystem um die Falkensteiner Höhle auf der Schwäbischen Alb in Baden-Württemberg ist Teil eines weitverzweigten unterirdischen Flusssystems, das Teile der Alb entwässert. Die Schwäbische Alb hat die bedeutendsten Karsthöhlen Deutschlands.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche spezifische Grenze zwischen Wüste und Savanne in Australien folgt der 250 mm Jahresniederschlags-Isohyete?",
        answerA = "Die Mulga-Spinifex-Grenze (Übergang von Akazienbüschen zu Hartgras)",
        answerB = "Die Outback-Grenze (rein administrativ)",
        answerC = "Die Einflussgrenze des indischen Ozean Dipols",
        answerD = "Die Torres-Strait-Klimagrenze",
        correctAnswer = 0,
        explanation = "In Australien markiert die ca. 250 mm Jahresniederschlags-Isohyete annähernd den Übergang zwischen der Mulga-Acacia-Buschzone (arider Übergang) und dem zentralen Spinifex-Grasland (Wüste). Diese Grenze variiert stark mit dem ENSO-Klimazyklus.",
        difficulty = 3,
        funFact = "Australiens 'Outback' ist klimatisch keine einheitliche Wüste, sondern ein Mosaik aus ariden Busch- und Grasslandschaften, die alle durch die 250 mm-Isohyete verbunden sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Höhlensystem in der Schweiz gilt als bemerkenswertestes aktives Karstgewässer-System der Alpen?",
        answerA = "Bätselberghöhle (Uri)",
        answerB = "Siebenhengste-Höhlensystem (Berner Oberland, ca. 170 km)",
        answerC = "Märchenhöhle (Solothurn)",
        answerD = "Kristallhöhle (Wallis)",
        correctAnswer = 1,
        explanation = "Das Siebenhengste-Hohgant-Höhlensystem im Berner Oberland ist mit über 170 km Gesamtlänge das längste Höhlensystem der Schweiz und eines der längsten Europas. Es liegt im Karst des Berner Oberländer Kalkgebirges.",
        difficulty = 3,
        funFact = "Das Siebenhengste-System liegt auf bis zu 2.000 m ü.NN und reicht bis unter das Thunersee-Niveau. Teile des Systems sind mit Eis gefüllt – eine Mischung aus Eishöhle und klassischer Karsthöhle."
    ),

)



