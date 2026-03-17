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

)
