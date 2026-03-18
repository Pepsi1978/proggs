package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsExpert(): List<Question> = listOf(

    // ── EXPERT (difficulty = 4) ── 50 questions ─────────────────────────────

    Question(
        categoryId = 1,
        questionText = "Welches Gebiet beansprucht weder Ägypten noch der Sudan, obwohl es von beiden Ländern verwaltet werden könnte?",
        answerA = "Hala'ib-Dreieck",
        answerB = "Bir Tawil",
        answerC = "Wadi Halfa",
        answerD = "Gebel Uweinat",
        correctAnswer = 1, // B
        explanation = "Bir Tawil ist eines der wenigen Gebiete weltweit, das von keinem Staat beansprucht wird. Ägypten erkennt die Grenze von 1899 an (wodurch Bir Tawil zu Sudan gehört), Sudan erkennt die Grenze von 1902 an (wodurch es zu Ägypten gehört) – beide bevorzugen das wertvollere Hala'ib-Dreieck.",
        difficulty = 4,
        funFact = "Ein amerikanischer Bauer erklärte Bir Tawil 2014 zum 'Königreich Norden', um seiner Tochter den Titel einer Prinzessin zu verschaffen – international anerkannt wird dies natürlich nicht."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchem Breitengrad liegt der geografische Mittelpunkt Südamerikas?",
        answerA = "12° Süd",
        answerB = "8° Süd",
        answerC = "15° Süd",
        answerD = "20° Süd",
        correctAnswer = 2, // C
        explanation = "Der geografische Mittelpunkt Südamerikas liegt bei etwa 15° Süd und 57° West, im brasilianischen Bundesstaat Mato Grosso, nahe der Stadt Cuiabá. Diese Koordinate markiert den Schwerpunkt der kontinentalen Landmasse.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist die ausschließliche Wirtschaftszone (AWZ) Frankreichs, die weltweit größte?",
        answerA = "9,5 Millionen km²",
        answerB = "11,0 Millionen km²",
        answerC = "7,8 Millionen km²",
        answerD = "13,2 Millionen km²",
        correctAnswer = 1, // B
        explanation = "Frankreich besitzt mit rund 11,0 Millionen km² die größte ausschließliche Wirtschaftszone der Welt, deutlich vor den USA (ca. 11,3 Mio. – je nach Berechnungsmethode eng beieinander) und Australien (ca. 8,1 Mio. km²). Diese enorme AWZ entsteht durch Frankreichs Überseegebiete in allen Weltmeeren.",
        difficulty = 4,
        funFact = "Frankreich besitzt Territorien in drei Ozeanen: Atlantik (Martinique, Guadeloupe), Indischer Ozean (Réunion, Mayotte) und Pazifik (Französisch-Polynesien, Neukaledonien)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss hat den weltweit größten Abfluss (mittlere Wassermenge) pro Sekunde?",
        answerA = "Nil",
        answerB = "Jangtse",
        answerC = "Amazonas",
        answerD = "Kongo",
        correctAnswer = 2, // C
        explanation = "Der Amazonas transportiert mit durchschnittlich etwa 209.000 m³/s den mit Abstand größten Wasserabfluss aller Flüsse der Erde. Dies entspricht rund 20 % des gesamten Süßwasserabflusses der Erde in die Ozeane.",
        difficulty = 4,
        funFact = "Der Amazonas gibt so viel Süßwasser ins Atlantikgebiet ab, dass das Salzgehalt des Atlantiks noch 160 km vor der Flussmündung messbar verdünnt ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die längste Küstenlinie der Welt?",
        answerA = "Russland",
        answerB = "Australien",
        answerC = "Norwegen",
        answerD = "Kanada",
        correctAnswer = 3, // D
        explanation = "Kanada hat mit über 202.080 km die längste Küstenlinie der Welt – mehr als doppelt so lang wie die Russlands (ca. 37.653 km) oder Norwegens (ca. 83.281 km mit Fjorden). Die kanadische Küste umfasst tausende Inseln, Fjorde und arktische Buchten.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Svalbard-Vertrag von 1920 gewährt Signatarstaaten besondere Rechte?",
        answerA = "Fischereirechte im Nordpolarmeer",
        answerB = "Bergbaurechte auf Spitzbergen",
        answerC = "Transitrechte durch die Nordostpassage",
        answerD = "Forschungsrechte auf dem Nordpol",
        correctAnswer = 1, // B
        explanation = "Der Svalbard-Vertrag von 1920 erkennt Norwegens Souveränität über den Archipel an, gewährt aber den über 40 Signatarstaaten das gleichberechtigte Recht zur wirtschaftlichen Nutzung – insbesondere Bergbau. Russland betreibt bis heute die Bergbausiedlung Barentsburg auf Svalbard.",
        difficulty = 4,
        funFact = "Svalbard hat keine Einreisebeschränkungen – theoretisch kann jeder Staatsbürger eines Signaturstaates ohne Visum einreisen und eine Firma gründen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozeangraben ist der tiefste bekannte Punkt der Erde, und wie tief ist er?",
        answerA = "Tonga-Graben, 10.820 m",
        answerB = "Marianengraben, 10.994 m",
        answerC = "Philippinengraben, 10.540 m",
        answerD = "Kermadec-Graben, 10.047 m",
        correctAnswer = 1, // B
        explanation = "Der Challenger Deep im Marianengraben im westlichen Pazifik ist mit einer Tiefe von ca. 10.994 m (neueste Messungen) der tiefste bekannte Punkt der Erde. Er liegt westlich von Guam und wurde 1960 von Piccard und Walsh erstmals von Menschen besucht.",
        difficulty = 4,
        funFact = "Der Marianengraben ist so tief, dass der Mount Everest (8.849 m) darin versinken würde und noch über 2 km Wasser darüber stünde."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr entdeckte Abel Tasman als erster Europäer Neuseeland?",
        answerA = "1642",
        answerB = "1619",
        answerC = "1688",
        answerD = "1665",
        correctAnswer = 0, // A
        explanation = "Der niederländische Entdecker Abel Tasman sichtete Neuseeland am 13. Dezember 1642 als erster Europäer. Er benannte es 'Staten Landt' – der Name 'Nieuw Zeeland' (nach der niederländischen Provinz Zeeland) wurde später von Kartografen vergeben.",
        difficulty = 4,
        funFact = "Tasmanien in Australien ist nach Abel Tasman benannt, der es 1642 ebenfalls als erster Europäer besuchte – noch bevor er Neuseeland erreichte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Afrikas (Stand 2024)?",
        answerA = "Äthiopien",
        answerB = "Demokratische Republik Kongo",
        answerC = "Nigeria",
        answerD = "Ägypten",
        correctAnswer = 2, // C
        explanation = "Nigeria ist mit über 220 Millionen Einwohnern (2024) das bevölkerungsreichste Land Afrikas und steht weltweit auf Rang 6. Es wird erwartet, dass Nigeria bis 2050 zu den drei bevölkerungsreichsten Ländern der Welt gehören wird.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele Länder haben keine Meeresküste (Binnenländer) in Afrika?",
        answerA = "14",
        answerB = "16",
        answerC = "12",
        answerD = "18",
        correctAnswer = 0, // A
        explanation = "Afrika hat 16 Binnenländer ohne Meeresküste – das sind mehr als auf jedem anderen Kontinent. Zu ihnen gehören u.a. Äthiopien (bis 1993 hatte es Zugang zum Meer), Mali, Niger, Tschad, Simbabwe und Uganda.",
        difficulty = 4,
        funFact = "Äthiopien und Eritrea sind ein besonderer Fall: Nachdem Eritrea 1993 unabhängig wurde, verlor Äthiopien seinen Zugang zum Roten Meer und wurde zum größten Binnenland Afrikas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Berg ist der am weitesten vom Erdmittelpunkt entfernte Punkt der Erdoberfläche?",
        answerA = "Mount Everest",
        answerB = "Chimborazo",
        answerC = "Kilimandscharo",
        answerD = "Mauna Kea",
        correctAnswer = 1, // B
        explanation = "Der Chimborazo in Ecuador (6.263 m) liegt am Äquator, wo die Erde durch die Abplattung am breitesten ist. Obwohl der Everest höher über dem Meeresspiegel liegt, ist der Gipfel des Chimborazo ca. 6.384 km vom Erdmittelpunkt entfernt – rund 2.100 km weiter als der Everest.",
        difficulty = 4,
        funFact = "Der Chimborazo war im 19. Jahrhundert der höchste bekannte Berg der Welt – Humboldt und Bonpland versuchten 1802 ihn zu besteigen und erreichten einen Höhenrekord von ca. 5.875 m."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Mikrostaaten-Gebiet ist eine Enklave in der Enklave San Marino?",
        answerA = "Serravalle",
        answerB = "Es gibt keine Enklave in San Marino",
        answerC = "Poggio di Chiesanuova",
        answerD = "Faetano",
        correctAnswer = 1, // B
        explanation = "San Marino selbst ist eine Enklave vollständig innerhalb Italiens, enthält jedoch keine eigene Enklave. San Marino ist eine der ältesten Republiken der Welt und der einzige Staat, der vollständig innerhalb eines anderen Staates liegt (außer dem Vatikan, der in Rom liegt).",
        difficulty = 4,
        funFact = "San Marino mit nur 61 km² ist nach dem Vatikan und Monaco der drittkleinste Staat Europas und beansprucht, die älteste Republik der Welt zu sein – gegründet angeblich 301 n. Chr."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss mündet in das Kaspische Meer und hat das größte Einzugsgebiet?",
        answerA = "Ural",
        answerB = "Kura",
        answerC = "Wolga",
        answerD = "Terek",
        correctAnswer = 2, // C
        explanation = "Die Wolga ist mit rund 3.530 km der längste Fluss Europas und entwässert ein Einzugsgebiet von ca. 1,38 Millionen km² in das Kaspische Meer. Sie trägt etwa 80 % des gesamten Süßwasserzuflusses ins Kaspische Meer bei.",
        difficulty = 4,
        funFact = "Das Kaspische Meer ist trotz des Namens ein See – der größte See der Welt mit ca. 371.000 km² Oberfläche. Sein Wasserspiegel liegt etwa 28 Meter unter dem Meeresspiegel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Inselgruppe liegt geografisch am weitesten von jedem Festland entfernt?",
        answerA = "Tristan da Cunha",
        answerB = "Heard Island",
        answerC = "Bouvetøya",
        answerD = "Kerguelen",
        correctAnswer = 0, // A
        explanation = "Tristan da Cunha im Südatlantik gilt als die abgelegenste bewohnte Inselgruppe der Welt. Die Hauptinsel liegt 2.787 km von der nächsten Insel (St. Helena) und 2.437 km von Südafrika entfernt. Die ca. 250 Bewohner sind britische Staatsbürger.",
        difficulty = 4,
        funFact = "Tristan da Cunha hat keine Flugverbindung – man kann die Insel nur per Schiff erreichen, und es gibt nur wenige Versorgungsfahrten pro Jahr von Südafrika aus."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land besitzt die meisten UNESCO-Welterbestätten (Stand 2024)?",
        answerA = "China",
        answerB = "Spanien",
        answerC = "Deutschland",
        answerD = "Italien",
        correctAnswer = 3, // D
        explanation = "Italien führt die Liste der UNESCO-Welterbestätten mit 59 Stätten an (Stand 2024), knapp vor China (57 Stätten). Beide Länder konkurrieren seit Jahren um den ersten Platz und fügen regelmäßig neue Stätten zur Liste hinzu.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meerenge ist die schmalste internationale Schifffahrtsstraße der Welt?",
        answerA = "Straße von Gibraltar",
        answerB = "Straße von Malakka",
        answerC = "Straße von Hormus",
        answerD = "Straße von Tiran",
        correctAnswer = 2, // C
        explanation = "Die Straße von Hormus zwischen dem Oman und dem Iran ist an ihrer engsten Stelle nur etwa 34 km breit, aber die gesamte nutzbare Schifffahrtsrinne ist faktisch noch enger. Sie ist die wichtigste Öl-Transitroute der Welt: ca. 20 % des weltweiten Ölhandels passieren sie täglich.",
        difficulty = 4,
        funFact = "Etwa 17 Millionen Barrel Erdöl werden täglich durch die Straße von Hormus transportiert – mehr als durch den Suezkanal und den Panamakanal zusammen."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Höhe liegt der Titicacasee, der höchstgelegene schiffbare See der Welt?",
        answerA = "3.812 m ü.M.",
        answerB = "4.135 m ü.M.",
        answerC = "3.456 m ü.M.",
        answerD = "4.320 m ü.M.",
        correctAnswer = 0, // A
        explanation = "Der Titicacasee liegt auf 3.812 m über dem Meeresspiegel an der Grenze zwischen Peru und Bolivien. Mit einer Fläche von ca. 8.372 km² ist er der größte Süßwassersee Südamerikas und für kommerzielle Schifffahrt nutzbar.",
        difficulty = 4,
        funFact = "Auf dem Titicacasee leben die Uru – ein Volk, das seit Jahrhunderten auf künstlich angelegten schwimmenden Inseln aus Totora-Schilf lebt, die ständig erneuert werden müssen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele Zeitzonen erstreckt sich Russland über?",
        answerA = "9",
        answerB = "13",
        answerC = "11",
        answerD = "15",
        correctAnswer = 2, // C
        explanation = "Russland überspannt 11 Zeitzonen – von UTC+2 (Kaliningrad) bis UTC+12 (Kamtschatka und Tschukotka). Damit ist Russland das Land mit den meisten Zeitzonen der Welt, obwohl es früher 12 hatte, bevor einige Regionen zusammengelegt wurden.",
        difficulty = 4,
        funFact = "Wenn es in Kaliningrad (dem westlichsten Teil Russlands) 6 Uhr morgens ist, ist es auf Kamtschatka (dem östlichsten Teil) bereits 17 Uhr am selben Tag."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land, das zwei verschiedene Kontinente als Teil seines Kerngebiets (nicht Überseegebiete) umfasst?",
        answerA = "Russland",
        answerB = "Ägypten",
        answerC = "Kasachstan",
        answerD = "Türkei",
        correctAnswer = 3, // D
        explanation = "Die Türkei ist das einzige Land, dessen Kernterritorium auf zwei Kontinenten liegt: Anatolien (Asien) und Thrakien (Europa) sind durch den Bosporus, das Marmarameer und die Dardanellen getrennt. Russland und Kasachstan erstrecken sich ebenfalls über Europa und Asien, aber weniger ausgewogen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Punkt der Erde ist der am weitesten von jeglichem Ozeanwasser entfernte Ort?",
        answerA = "Ürümqi, China",
        answerB = "Pol der Unzugänglichkeit, Xinjiang",
        answerC = "Ulaanbaatar, Mongolei",
        answerD = "Almaty, Kasachstan",
        correctAnswer = 1, // B
        explanation = "Der eurasische Pol der Unzugänglichkeit liegt bei ca. 46°17'N 86°40'E in der Nähe von Ürümqi in der chinesischen Region Xinjiang. Dieser Punkt ist von jedem Ozean rund 2.645 km entfernt und gilt als der kontinentalste Punkt der Erde.",
        difficulty = 4,
        funFact = "Es gibt mehrere 'Pole der Unzugänglichkeit' je nach Definition – einen für den weitesten Punkt von Küsten, einen für den Ozean (Punkt Nemo im Südpazifik, 2.688 km von der nächsten Küste)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das kleinste Land der Welt nach Fläche, das eine eigene Armee hat?",
        answerA = "Liechtenstein",
        answerB = "San Marino",
        answerC = "Monaco",
        answerD = "Vatikanstadt",
        correctAnswer = 1, // B
        explanation = "San Marino unterhält mit dem 'Corpo Militare' eine der ältesten noch aktiven Armeen der Welt – obwohl sie eher zeremonieller Natur ist. Monaco und Liechtenstein haben keine eigene Armee (Frankreich bzw. Schweiz garantieren ihre Sicherheit). Der Vatikan hat die Schweizergarde.",
        difficulty = 4,
        funFact = "Liechtenstein hat seine Armee 1868 nach dem Preußisch-Österreichischen Krieg abgeschafft – als das Land feststellte, dass seine 80 Soldaten heil zurückgekehrt waren (und sogar einen Freund mitgebracht hatten)."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Meer liegt die Insel Sokotra, bekannt für ihre einzigartige Flora?",
        answerA = "Rotes Meer",
        answerB = "Persischer Golf",
        answerC = "Arabisches Meer",
        answerD = "Indischer Ozean",
        correctAnswer = 2, // C
        explanation = "Sokotra liegt im Arabischen Meer nahe dem Golf von Aden und gehört zu Jemen. Die Insel ist für ihre bizarre Flora bekannt, besonders den Drachenblutsbaum (Dracaena cinnabari). Über ein Drittel aller Pflanzenarten der Insel sind endemisch – nirgendwo sonst auf der Erde zu finden.",
        difficulty = 4,
        funFact = "Sokotra wird oft als 'Galápagos des Indischen Ozeans' bezeichnet. Der Drachenblutsbaum mit seinem pilzförmigen Blätterdach und rotem Harz war in der Antike ein begehrtes Handelsgut."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat mit dem Vättern den tiefsten See Skandinaviens?",
        answerA = "Norwegen",
        answerB = "Finnland",
        answerC = "Schweden",
        answerD = "Dänemark",
        correctAnswer = 2, // C
        explanation = "Der Vättern in Schweden ist mit einer maximalen Tiefe von 128 m der tiefste See Skandinaviens und der zweitgrößte See Schwedens. Er liegt auf der Hochebene Schmalensee und ist für sein außergewöhnlich klares Wasser bekannt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die bevölkerungsreichste Stadt der Welt, die NICHT Hauptstadt ihres Landes ist?",
        answerA = "Mumbai",
        answerB = "São Paulo",
        answerC = "Shanghai",
        answerD = "Karatschi",
        correctAnswer = 2, // C
        explanation = "Shanghai ist mit über 26 Millionen Einwohnern (Stadtgebiet) die bevölkerungsreichste Stadt Chinas, aber Peking ist die Hauptstadt. São Paulo (keine Hauptstadt Brasiliens – das ist Brasília) und Karatschi (keine Hauptstadt Pakistans – das ist Islamabad) sind ebenfalls große Nicht-Hauptstädte.",
        difficulty = 4,
        funFact = "Viele bedeutende Wirtschaftszentren sind bewusst nicht Hauptstädte: Australien hat Canberra statt Sydney, Kanada Ottawa statt Toronto, die USA Washington D.C. statt New York."
    ),

    Question(
        categoryId = 1,
        questionText = "Durch wie viele Länder fließt die Donau?",
        answerA = "8",
        answerB = "12",
        answerC = "10",
        answerD = "14",
        correctAnswer = 2, // C
        explanation = "Die Donau fließt durch 10 Länder: Deutschland, Österreich, Slowakei, Ungarn, Kroatien, Serbien, Bulgarien, Rumänien, Moldau (berührt sie kaum) und die Ukraine. Sie ist damit der Fluss, der die meisten Länder durchquert, und mündet ins Schwarze Meer.",
        difficulty = 4,
        funFact = "Die Donau fließt durch vier Hauptstädte: Wien (Österreich), Bratislava (Slowakei), Budapest (Ungarn) und Belgrad (Serbien) – kein anderer Fluss der Welt durchfließt so viele Hauptstädte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das höchstgelegene Hauptstadt der Welt?",
        answerA = "Quito",
        answerB = "Thimphu",
        answerC = "La Paz",
        answerD = "Bogotá",
        correctAnswer = 2, // C
        explanation = "La Paz in Bolivien liegt auf etwa 3.640 m Höhe und ist die höchstgelegene Regierungssitz der Welt (de facto Hauptstadt, obwohl Sucre die verfassungsmäßige Hauptstadt ist). Quito liegt auf 2.850 m, Thimphu auf 2.334 m und Bogotá auf 2.600 m.",
        difficulty = 4,
        funFact = "In La Paz kocht Wasser bei nur etwa 88°C statt 100°C, was das Kochen von Lebensmitteln länger dauern lässt. Spitzenköche müssen ihre Rezepte für diese Höhe anpassen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat hat die längste Landgrenze mit China?",
        answerA = "Indien",
        answerB = "Kasachstan",
        answerC = "Russland",
        answerD = "Mongolei",
        correctAnswer = 2, // C
        explanation = "Russland teilt mit China eine Landgrenze von etwa 4.209 km – die längste Grenze Chinas mit einem einzelnen Staat. Die Mongolei hat eine Grenze von ca. 4.677 km, macht aber Kasachstan (1.765 km) und Indien (umstritten, ca. 3.488 km) wett. Tatsächlich ist die Mongolei die längste.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die einzige Nation, die sowohl den Äquator als auch den Wendekreis des Steinbocks überquert?",
        answerA = "Peru",
        answerB = "Bolivien",
        answerC = "Brasilien",
        answerD = "Ecuador",
        correctAnswer = 2, // C
        explanation = "Brasilien ist das einzige Land der Welt, das sowohl vom Äquator (0°) als auch vom Wendekreis des Steinbocks (23,5° Süd) durchquert wird. Das Land erstreckt sich von ca. 5° Nord bis 34° Süd und umfasst damit tropische, subtropische und gemäßigte Klimazonen.",
        difficulty = 4,
        funFact = "Brasilien ist das fünftgrößte Land der Welt und grenzt an alle südamerikanischen Staaten außer Chile und Ecuador – ein geografisches Paradoxon für ein so zentrales Land."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der tiefste Punkt Afrikas?",
        answerA = "Äthiopien",
        answerB = "Kenia",
        answerC = "Dschibuti",
        answerD = "Tansania",
        correctAnswer = 2, // C
        explanation = "Das Assal-Senke (Lac Assal) in Dschibuti liegt auf −153 m unter dem Meeresspiegel – dies ist der tiefste Punkt Afrikas und der dritttiefste Landpunkt der Erde nach dem Toten Meer (−430 m) und dem Ufer-Tiefpunkt des Kaspischen Meeres.",
        difficulty = 4,
        funFact = "Der Assal-See ist auch einer der salzhaltigsten Seen der Welt mit einem Salzgehalt von rund 35 % – fast zehnmal höher als das Meerwasser und damit in der Nähe des Toten Meeres."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche zwei Länder teilen sich die Halbinsel Iberia vollständig?",
        answerA = "Spanien und Portugal",
        answerB = "Spanien, Portugal und Andorra",
        answerC = "Spanien, Portugal, Andorra und Gibraltar",
        answerD = "Spanien und Portugal (Andorra liegt auf der Halbinsel, ist aber kein Iberer)",
        correctAnswer = 0, // A
        explanation = "Die Iberische Halbinsel wird geografisch von Spanien und Portugal eingenommen. Andorra liegt zwar auf der Halbinsel (in den Pyrenäen), wird aber nicht zur iberischen Staatenwelt gezählt. Gibraltar ist ein britisches Überseegebiet an der Südspitze der Halbinsel.",
        difficulty = 4,
        funFact = "Gibraltar ist mit nur 6,7 km² das drittkleinste britische Überseegebiet, hat aber eine strategisch enorme Bedeutung als 'Schlüssel zum Mittelmeer' und war seit 1704 britisch."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt das geografische Phänomen, bei dem zwei Ozeane aufeinandertreffen, aber ihr Wasser sich nicht sofort vermischt?",
        answerA = "Ozeanische Thermokline",
        answerB = "Halocline-Effekt",
        answerC = "Ozeanische Divergenz",
        answerD = "Ozeanische Konvergenzlinie",
        correctAnswer = 1, // B
        explanation = "Die Halocline (Halokline) ist eine Sprungschicht im Ozean, an der sich zwei Wassermassen mit unterschiedlichem Salzgehalt und damit unterschiedlicher Dichte treffen. Sichtbar ist dieses Phänomen z.B. am Kap der Guten Hoffnung und an der Straße von Molukken, wo Wasser unterschiedlicher Herkunft aufeinandertrifft.",
        difficulty = 4,
        funFact = "Am Kap Hornos in Chile treffen Pazifik und Atlantik aufeinander – dieses Gebiet ist als 'Friedhof der Schiffe' bekannt, weil die kollidierende Strömungen extrem gefährliche See erzeugen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Nachbarländer (meiste gemeinsame Landesgrenzen)?",
        answerA = "China",
        answerB = "Russland",
        answerC = "Brasilien",
        answerD = "Deutschland",
        correctAnswer = 1, // B
        explanation = "Russland grenzt an 14 verschiedene Länder: Norwegen, Finnland, Estland, Lettland, Litauen, Polen, Belarus, Ukraine, Georgien, Aserbaidschan, Kasachstan, China, Mongolei und Nordkorea. China kommt auf 14 Nachbarn (wenn man alle Beziehungen zählt), aber Russland liegt leicht vorn.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean hat die durchschnittlich größte Tiefe?",
        answerA = "Pazifik",
        answerB = "Arktischer Ozean",
        answerC = "Indischer Ozean",
        answerD = "Atlantik",
        correctAnswer = 0, // A
        explanation = "Der Pazifische Ozean hat mit einer Durchschnittstiefe von etwa 4.280 m die größte mittlere Tiefe aller Ozeane. Der Atlantik hat ca. 3.332 m, der Indische Ozean ca. 3.897 m. Der Pazifik ist auch flächenmäßig der größte Ozean und enthält mehr als die Hälfte des Meerwassers der Erde.",
        difficulty = 4,
        funFact = "Der Pazifik ist so groß, dass alle Kontinente der Erde darin Platz hätten – mit noch verbleibendem Platz. Seine Fläche beträgt ca. 165 Millionen km²."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige UN-Mitgliedsland, das keinen offiziellen Hauptstadt-Status in seiner Verfassung hat?",
        answerA = "Nauru",
        answerB = "Tuvalu",
        answerC = "Palau",
        answerD = "Kiribati",
        correctAnswer = 0, // A
        explanation = "Nauru, der drittkleinste Staat der Welt (21 km²), hat keine offiziell erklärte Hauptstadt. Die Regierungsgebäude befinden sich in Yaren, das als inoffizielle Hauptstadt gilt. Nauru ist auch das einzige Land ohne eine offizielle Hauptstadt in seiner Verfassung.",
        difficulty = 4,
        funFact = "Nauru war einst dank Phosphat-Vorkommen (Vogelkot über Millionen von Jahren) das reichste Land pro Kopf der Welt. Nach der Erschöpfung der Ressourcen ist es nun eines der ärmsten – ein warnendes Beispiel für Ressourcen-Fluch."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt liegt exakt auf dem Nullmeridian (0° Länge)?",
        answerA = "London",
        answerB = "Greenwich",
        answerC = "Paris",
        answerD = "Madrid",
        correctAnswer = 1, // B
        explanation = "Der Nullmeridian, auch Primemeridian genannt, verläuft durch das Royal Observatory in Greenwich (heute ein Stadtteil Londons, aber historisch eigenständig). Im engeren Sinne ist es das Greenwich-Observatorium, das auf dem Nullmeridian liegt, nicht die City of London.",
        difficulty = 4,
        funFact = "Bis 1884 hatte jedes Land seinen eigenen Nullmeridian – Frankreich nutzte Paris, Amerika Washington D.C. Erst die Internationale Meridian-Konferenz 1884 einigten sich die meisten Länder auf Greenwich."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat pro Einwohner die größte Landfläche der Welt?",
        answerA = "Russland",
        answerB = "Australien",
        answerC = "Grönland (Dänemark)",
        answerD = "Mongolei",
        correctAnswer = 2, // C
        explanation = "Grönland (autonomes Territorium Dänemarks) hat mit ca. 2,17 Millionen km² und nur etwa 56.000 Einwohnern die größte Pro-Kopf-Landfläche – rund 38 km² pro Person. Mongolei (ca. 4,7 km² p.P.), Australien (ca. 3,2 km² p.P.) und Russland (ca. 11,5 km² p.P.) folgen.",
        difficulty = 4,
        funFact = "Obwohl Grönland 80 % von Eis bedeckt ist, wäre es nach Fläche das zwölftgrößte Land der Welt, wenn es unabhängig wäre – noch vor Peru oder Libyen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie nennt man die geografische Linie, die die Übergangszone zwischen asiatischen und australasischen Tierarten markiert?",
        answerA = "Humboldt-Linie",
        answerB = "Weber-Linie",
        answerC = "Wallace-Linie",
        answerD = "Darwin-Linie",
        correctAnswer = 2, // C
        explanation = "Die Wallace-Linie ist eine biogeografische Grenzlinie, die 1859 von Alfred Russel Wallace entdeckt wurde. Sie verläuft zwischen Borneo und Sulawesi sowie zwischen Bali und Lombok und trennt die orientalische (asiatische) von der australasischen Tierregion. Östlich der Linie dominieren Beuteltiere und Kakadus.",
        difficulty = 4,
        funFact = "Zwischen der Wallace-Linie und der Weber-Linie liegt die 'Wallacea' – eine Übergangszone mit einzigartiger Mischfauna. Sulawesi, die Maluku-Inseln und Lombok gehören zu dieser biologischen Zwischenzone."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche vier Länder teilen sich den Aralsee?",
        answerA = "Kasachstan und Usbekistan",
        answerB = "Kasachstan, Usbekistan und Turkmenistan",
        answerC = "Kasachstan, Usbekistan, Kirgisistan und Tadschikistan",
        answerD = "Nur Kasachstan (der See ist fast vollständig ausgetrocknet)",
        correctAnswer = 0, // A
        explanation = "Der Aralsee war ursprünglich zwischen Kasachstan (Norden) und Usbekistan (Süden) aufgeteilt. Durch massive Bewässerungsprojekte seit den 1960er Jahren ist er auf unter 10 % seiner ursprünglichen Größe geschrumpft. Der nördliche Teil (in Kasachstan) wurde durch einen Damm stabilisiert.",
        difficulty = 4,
        funFact = "Der Aralsee war einst der viertgrößte See der Welt. In den ehemaligen Hafenstädten wie Muynak (Usbekistan) rosten heute Schiffe im Sand – das Meer ist 150 km entfernt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel gehört zu Dänemark, obwohl sie geografisch nahe der kanadischen Küste liegt?",
        answerA = "Ellesmere Island",
        answerB = "Grönland",
        answerC = "Hans Island",
        answerD = "Baffin Island",
        correctAnswer = 2, // C
        explanation = "Hans Island (Inuit: Tartupaluk) war bis 2022 Gegenstand eines territorialen Streits zwischen Kanada und Dänemark. Die winzige, unbewohnte Insel (1,3 km²) in der Kennedy-Kanal liegt genau auf der Mittellinie der Meerenge. 2022 einigten sich beide Länder auf eine gemeinsame Grenzlinie durch die Insel.",
        difficulty = 4,
        funFact = "Der 'Whiskey-Krieg' zwischen Kanada und Dänemark um Hans Island war einer der friedlichsten Territorialkonflikte: Beide Seiten hinterließen beim Besuch der Insel abwechselnd Whiskey- bzw. Schnaps-Flaschen und einen Brief 'Willkommen in Kanada/Dänemark'."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gebirgspass verbindet Pakistan und China auf über 4.700 m Höhe?",
        answerA = "Khyber-Pass",
        answerB = "Karakorum-Pass",
        answerC = "Kunjerab-Pass",
        answerD = "Bolan-Pass",
        correctAnswer = 2, // C
        explanation = "Der Kunjerab-Pass (4.733 m) liegt am Karakorum Highway und ist der offiziell höchste asphaltierte Grenzübergang der Welt zwischen Pakistan und China. Der Karakorum-Pass (4.693 m) ist höher, aber kein offizieller Grenzübergang mehr.",
        difficulty = 4,
        funFact = "Der Karakorum Highway gilt als eines der erstaunlichsten Bauprojekte der Menschheit. Die Straße wurde zwischen 1959 und 1979 gebaut – Hunderte von Arbeitern starben beim Bau durch Erdrutsche, Überschwemmungen und extreme Kälte."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt die einzige Stadt auf dem Gebiet zweier Kontinente?",
        answerA = "Astana",
        answerB = "Istanbul",
        answerC = "Athen",
        answerD = "Tiflis",
        correctAnswer = 1, // B
        explanation = "Istanbul liegt auf zwei Kontinenten: Der europäische Teil (Thrakien) und der asiatische Teil (Anatolien) sind durch den Bosporus getrennt. Die Stadt ist einzigartig, da die Stadtgrenzen selbst auf beiden Kontinenten liegen – verbunden durch drei Brücken und einen Unterwassertunnel.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land Lateinamerikas, dessen Amtssprache Portugiesisch ist?",
        answerA = "Belize",
        answerB = "Suriname",
        answerC = "Brasilien",
        answerD = "Guyana",
        correctAnswer = 2, // C
        explanation = "Brasilien ist das einzige Land Lateinamerikas mit Portugiesisch als Amtssprache – ein Ergebnis des Vertrags von Tordesillas (1494), der die Neue Welt zwischen Spanien und Portugal aufteilte. Belize hat Englisch, Suriname Niederländisch und Guyana Englisch als Amtssprachen.",
        difficulty = 4,
        funFact = "Brasilien ist das einzige Land der Welt, wo Portugiesisch von über 200 Millionen Menschen als Muttersprache gesprochen wird – mehr als in Portugal selbst (ca. 10 Millionen)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Gebirgskette bildet die natürliche Grenze zwischen Europa und Asien im Ural-Bereich?",
        answerA = "Kaukasus",
        answerB = "Altai",
        answerC = "Ural",
        answerD = "Hindukusch",
        correctAnswer = 2, // C
        explanation = "Das Uralgebirge erstreckt sich über ca. 2.500 km von Nord nach Süd und bildet die traditionelle geografische Grenze zwischen Europa und Asien. Die höchste Erhebung ist der Narodnaja (1.895 m). Im Süden setzt sich die Grenze entlang des Uralflusses und des Kaspischen Meeres fort.",
        difficulty = 4,
        funFact = "Jekaterinburg, die viertgrößte Stadt Russlands, liegt am östlichen Fuß des Urals in Asien – obwohl sie von manchen Karten als 'europäisch' eingestuft wird, liegt sie geographisch in Asien."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Tiefland ist das größte zusammenhängende Tiefland der Erde?",
        answerA = "Amazonasbecken",
        answerB = "Westsibirisches Tiefland",
        answerC = "Großes Becken (USA)",
        answerD = "Hindustan-Ebene",
        correctAnswer = 1, // B
        explanation = "Das Westsibirische Tiefland in Russland ist mit ca. 2,6 Millionen km² das größte zusammenhängende Tiefland der Erde. Es erstreckt sich vom Ural bis zum Jenissej und ist durchschnittlich nur 100 m über dem Meeresspiegel. Ein Großteil ist Sumpf- und Moorland.",
        difficulty = 4,
        funFact = "Das westsibirische Tiefland enthält das größte Torfmoor-Gebiet der Welt (Vasyugan-Moor), das riesige Mengen Kohlenstoff speichert – ein kritischer Klimapuffer, der durch den Klimawandel bedroht ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land weltweit, das offiziell keinen Kalender nach gregorianischem Standard verwendet?",
        answerA = "Saudi-Arabien",
        answerB = "Iran",
        answerC = "Afghanistan",
        answerD = "Äthiopien",
        correctAnswer = 1, // B
        explanation = "Der Iran verwendet den persischen Kalender (Solar-Hijri) als offiziellen Staatskalender. Das Jahr beginnt zur Frühlings-Tagundnachtgleiche (Nowruz, ca. 21. März) und umfasst 12 Monate. Das Jahr 2024 entspricht dem persischen Jahr 1403.",
        difficulty = 4,
        funFact = "Äthiopien nutzt ebenfalls einen eigenen Kalender (koptisch, mit 13 Monaten), der ca. 7-8 Jahre hinter dem gregorianischen zurückliegt. Das Jahr 2000 wurde in Äthiopien erst 2007/2008 gefeiert."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat keine aktiven Vulkane?",
        answerA = "Australien",
        answerB = "Afrika",
        answerC = "Nordamerika",
        answerD = "Europa",
        correctAnswer = 0, // A
        explanation = "Australien ist der einzige Kontinent ohne aktive Vulkane. Zwar gibt es geologische Beweise für frühere vulkanische Aktivität (z.B. der Mount Gambier in Südaustralien, zuletzt aktiv vor 5.000 Jahren), aber derzeit keine aktiven Vulkane. Dies liegt daran, dass Australien weit von plattentektonischen Grenzen entfernt liegt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Territorium hat die niedrigste Bevölkerungsdichte der Welt (unter bewohnten Gebieten)?",
        answerA = "Alaska",
        answerB = "Mongolei",
        answerC = "Grönland",
        answerD = "Namibia",
        correctAnswer = 2, // C
        explanation = "Grönland hat mit ca. 0,026 Einwohnern pro km² die niedrigste Bevölkerungsdichte aller bewohnten Territorien der Welt. Die Mongolei liegt bei ca. 2 Personen pro km², Alaska bei ca. 0,5. Grönlands 56.000 Einwohner leben auf 2,17 Millionen km² – größtenteils an der Westküste.",
        difficulty = 4,
        funFact = "Die Mongolei ist das bevölkerungsärmste Land der Welt (nach Bevölkerungsdichte), das in internationalen Ranglisten als eigenständiger Staat geführt wird. Ein nomadischer Lebensstil ist dort bis heute verbreitet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel ist gleichzeitig der am stärksten bewohnte Ort und die kleinste Insel der Welt nach Fläche (unter dauerhaft bewohnten Inseln)?",
        answerA = "Funafuti-Atoll",
        answerB = "Santa Cruz del Islote",
        answerC = "Migingo-Insel",
        answerD = "Norderney",
        correctAnswer = 1, // B
        explanation = "Santa Cruz del Islote vor der kolumbianischen Karibikküste gilt als am dichtesten besiedelte Insel der Welt: Auf nur 0,012 km² (1,2 Hektar) leben über 1.200 Menschen in etwa 97 Häusern. Die Bevölkerungsdichte übertrifft Hongkong und Manhattan um ein Vielfaches.",
        difficulty = 4,
        funFact = "Auf Santa Cruz del Islote gibt es keine Krankenhäuser, keine Schulen und keine Polizei. Die Einwohner ruderten jahrzehntelang täglich zum Festland für Schule und Arbeit – inzwischen gibt es eine Fährverbindung."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat den größten Anteil seiner Fläche unter dem Meeresspiegel?",
        answerA = "Niederlande",
        answerB = "Bangladesh",
        answerC = "Dänemark",
        answerD = "Belgien",
        correctAnswer = 0, // A
        explanation = "Die Niederlande haben rund 26 % ihrer Landfläche unterhalb des Meeresspiegels liegen – die tiefste Stelle liegt bei −6,76 m (Nieuwerkerk aan den IJssel). Über Jahrhunderte wurde durch Deiche, Polderland und Pumpwerke Land aus dem Meer zurückgewonnen (Landgewinnung).",
        difficulty = 4,
        funFact = "Fast die Hälfte der niederländischen Fläche liegt weniger als 1 Meter über dem Meeresspiegel. Das Land gibt jährlich Milliarden Euro für Küstenschutz aus – der Klimawandel stellt eine existenzielle Bedrohung dar."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Berg gilt als der am schwierigsten zu besteigende aller 8000er-Gipfel (höchste Todesrate pro Besteigungsversuch)?",
        answerA = "K2",
        answerB = "Annapurna",
        answerC = "Nanga Parbat",
        answerD = "Kangchendzönga",
        correctAnswer = 1, // B
        explanation = "Die Annapurna I in Nepal (8.091 m) hat historisch die höchste Todesrate unter den 14 Achttausendern: Auf jeden erfolgreichen Gipfeler kommen statistisch 0,3-0,5 Todesfälle. Obwohl der K2 als 'Savage Mountain' bekannt ist, liegt die Annapurna in manchen Statistiken noch höher.",
        difficulty = 4,
        funFact = "Die Annapurna war 1950 der erste Achttausender, der je von Menschen bestiegen wurde – durch die Franzosen Maurice Herzog und Louis Lachenal. Beide verloren dabei Finger und Zehen durch schwere Erfrierungen."
    ),

    // ── EXPERT BATCH 2 (difficulty = 4) ── 50 additional questions ───────────

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Inseln weltweit (nach offizieller Zählung)?",
        answerA = "Norwegen",
        answerB = "Schweden",
        answerC = "Indonesien",
        answerD = "Philippinen",
        correctAnswer = 1, // B
        explanation = "Schweden hat laut offizieller schwedischer Vermessungsbehörde über 221.800 Inseln – mehr als jedes andere Land der Welt. Davon sind jedoch nur etwa 1.000 dauerhaft bewohnt. Indonesien hat offiziell ca. 17.508 Inseln, die Philippinen ca. 7.641.",
        difficulty = 4,
        funFact = "Rund 1.000 schwedische Inseln sind dauerhaft bewohnt, aber im Sommer verdoppelt sich die Bevölkerung auf vielen Schäreninseln durch Ferienhäuser – Schweden hat eine der lebendigsten Schärenkulturen der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss bildet die längste internationale Grenze der Erde?",
        answerA = "Rio Grande",
        answerB = "Sambesi",
        answerC = "Mekong",
        answerD = "Kongo",
        correctAnswer = 0, // A
        explanation = "Der Rio Grande (Rio Bravo del Norte) bildet auf ca. 2.019 km die Grenze zwischen den USA und Mexiko und ist damit die längste Flussgrenze der Welt zwischen zwei Staaten. Er entspringt in Colorado und mündet im Golf von Mexiko.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt das Territorium, das gleichzeitig der südlichste Punkt Europas ist?",
        answerA = "Ceuta",
        answerB = "Punta de Tarifa",
        answerC = "Gibraltar",
        answerD = "Melilla",
        correctAnswer = 1, // B
        explanation = "Punta de Tarifa auf der Halbinsel Tarifa in Spanien (36°00'N) ist der südlichste Punkt des europäischen Festlandes. Gibraltar liegt geografisch nördlicher. Die Punta liegt nur 14 km vom afrikanischen Kontinent (Marokko) entfernt.",
        difficulty = 4,
        funFact = "Vom Leuchtturm auf Punta de Tarifa aus kann man bei klarem Wetter die afrikanische Küste mit bloßem Auge sehen. Die Straße von Gibraltar ist an dieser Stelle nur ca. 14,4 km breit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land besitzt die meisten Zeitzonen weltweit (inkl. Überseegebiete)?",
        answerA = "Russland",
        answerB = "USA",
        answerC = "Großbritannien",
        answerD = "Frankreich",
        correctAnswer = 3, // D
        explanation = "Frankreich erstreckt sich dank seiner Überseegebiete auf der ganzen Welt über 12 verschiedene Zeitzonen – von UTC-10 (Französisch-Polynesien) bis UTC+12 (Wallis und Futuna). Damit übertrifft Frankreich sogar Russland mit 11 Zeitzonen.",
        difficulty = 4,
        funFact = "Durch Frankreichs Überseegebiete ist das Land immer gleichzeitig an mindestens zwei verschiedenen Tagen – wenn es in Paris Mitternacht ist, ist es auf den Marquesas-Inseln noch der Vortag."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der geografische Nordpol der Erde?",
        answerA = "Kanada",
        answerB = "Norwegen (Svalbard)",
        answerC = "Dänemark (Grönland)",
        answerD = "In internationalen Gewässern / niemandem",
        correctAnswer = 3, // D
        explanation = "Der geografische Nordpol liegt mitten im Arktischen Ozean auf einer Eisdecke und gehört keinem Staat. Er ist nicht Teil irgendeines Landes, obwohl Russland, Kanada, Norwegen, Dänemark und die USA territoriale Ansprüche auf arktischen Meeresschelf erheben.",
        difficulty = 4,
        funFact = "Russland platzierte 2007 eine Titanflagge auf dem Meeresboden am Nordpol in 4.261 m Tiefe – ein symbolischer Akt ohne rechtliche Bedeutung, der aber international für Aufsehen sorgte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vulkan auf Island hat 2010 den europäischen Luftraum lahmgelegt?",
        answerA = "Hekla",
        answerB = "Katla",
        answerC = "Eyjafjallajökull",
        answerD = "Bárðarbunga",
        correctAnswer = 2, // C
        explanation = "Der Eyjafjallajökull (Eyjafjallagletscher) auf Island brach im April 2010 aus und speite eine riesige Aschewolke aus, die über sechs Tage lang den europäischen Luftraum sperrte. Über 100.000 Flüge wurden gestrichen – einer der größten Luftfahrtausfälle seit dem Zweiten Weltkrieg.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der einzige Fluss der Welt, der sowohl in den Atlantischen als auch in den Pazifischen Ozean entwässert?",
        answerA = "Rio de la Plata",
        answerB = "Parná",
        answerC = "Es gibt keinen solchen Fluss",
        answerD = "Casiquiare-Kanal",
        correctAnswer = 3, // D
        explanation = "Der Casiquiare-Kanal ist ein natürlicher Wasserweg in Venezuela, der den Orinoko (Atlantik) mit dem Rio Negro (Amazonas → Atlantik) verbindet – doch er ist Teil des Orinoko-Amazonas-Systems. Streng genommen gibt es keinen Fluss, der selbst in beide Ozeane mündet; der Casiquiare verbindet zwei Atlantik-Systeme. Es gibt jedoch Wasserscheiden-Phänomene.",
        difficulty = 4,
        funFact = "Der Casiquiare-Kanal wurde von Alexander von Humboldt 1800 kartografiert. Er ist ca. 335 km lang und eine der wenigen natürlichen Verbindungen zwischen zwei großen Flusssystemen weltweit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land Südamerikas, das keine Grenze zu Brasilien hat?",
        answerA = "Uruguay",
        answerB = "Ecuador",
        answerC = "Paraguay",
        answerD = "Guyana",
        correctAnswer = 1, // B
        explanation = "Ecuador und Chile sind die einzigen südamerikanischen Länder ohne gemeinsame Grenze mit Brasilien. Ecuador grenzt an Kolumbien und Peru. Brasilien grenzt an alle anderen südamerikanischen Länder außer Ecuador und Chile.",
        difficulty = 4,
        funFact = "Brasilien ist so zentral in Südamerika, dass es an 10 der 12 anderen südamerikanischen Länder grenzt. Nur Ecuador (durch Peru getrennt) und Chile (durch Argentinien/Bolivien getrennt) fehlen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher See gilt als der älteste See der Welt (nach Entstehungsalter)?",
        answerA = "Kaspisches Meer",
        answerB = "Baikalsee",
        answerC = "Tanganjikasee",
        answerD = "Victoriasee",
        correctAnswer = 1, // B
        explanation = "Der Baikalsee in Sibirien ist mit einem geschätzten Alter von 25–30 Millionen Jahren der älteste See der Welt. Er ist auch der tiefste (1.642 m) und enthält mit ca. 23.615 km³ rund 20 % des weltweiten Süßwasservorrats.",
        difficulty = 4,
        funFact = "Der Baikalsee beherbergt über 1.700 endemische Tier- und Pflanzenarten, die nirgendwo sonst auf der Erde vorkommen – darunter die Baikal-Robbe (Nerpa), die einzige Süßwasserrobbe der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebiet beanspruchen Argentinien, Großbritannien und Chile gleichzeitig in der Antarktis?",
        answerA = "Rossabhängigkeit",
        answerB = "Weddellmeer-Sektor",
        answerC = "Antarktische Halbinsel",
        answerD = "Queen Maud Land",
        correctAnswer = 2, // C
        explanation = "Die Antarktische Halbinsel ist das einzige Gebiet der Erde, auf das drei Länder gleichzeitig territoriale Ansprüche erheben: Argentinien (seit 1943), Chile (seit 1940) und Großbritannien (seit 1908). Alle drei Ansprüche überlappen sich. Der Antarktisvertrag von 1959 friert diese Ansprüche ein.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die bevölkerungsreichste Stadt Afrikas (Stand 2024)?",
        answerA = "Kairo",
        answerB = "Lagos",
        answerC = "Kinshasa",
        answerD = "Johannesburg",
        correctAnswer = 1, // B
        explanation = "Lagos in Nigeria ist mit über 15–20 Millionen Einwohnern (je nach Berechnungsmethode der Metropolregion) die bevölkerungsreichste Stadt Afrikas – vor Kairo (ca. 22 Mio. im Großraum) und Kinshasa (ca. 17 Mio.). Lagos wächst so schnell, dass genaue Zahlen schwer zu ermitteln sind.",
        difficulty = 4,
        funFact = "Lagos wächst mit ca. 85 neuen Einwohnern pro Stunde und könnte bis 2050 zur größten Stadt der Welt werden – vor Tokio, Delhi und Shanghai. Die Stadt liegt auf einem Archipel mit über 200 Inseln und Halbinseln."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt die kürzeste internationale Grenze der Welt (zwischen zwei souveränen Staaten)?",
        answerA = "Gibraltar–Spanien",
        answerB = "Monaco–Frankreich",
        answerC = "Vatikan–Italien",
        answerD = "Peñón de Vélez–Marokko",
        correctAnswer = 3, // D
        explanation = "Peñón de Vélez de la Gomera, eine spanische Exklave an der Nordküste Marokkos, ist durch eine ca. 85 m lange Sandbank mit dem marokkanischen Festland verbunden – diese Landbrücke ist die kürzeste Staatsgrenze der Welt. Gibraltar (1,2 km) und Monaco–Frankreich (4,4 km) sind länger.",
        difficulty = 4,
        funFact = "Peñón de Vélez de la Gomera war historisch eine Insel, aber eine Sandbank hat sie im Laufe der Zeit mit der afrikanischen Küste verbunden. Heute beherbergt sie eine spanische Militärgarnison."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige asiatische Land, das nicht Teil der Kontinentalplatte Eurasien ist?",
        answerA = "Japan",
        answerB = "Taiwan",
        answerC = "Philippinen",
        answerD = "Indonesien",
        correctAnswer = 2, // C
        explanation = "Die Philippinen liegen auf der Philippinischen Meeresplatte und der Eurasischen Platte, sind aber größtenteils nicht Teil der eurasischen Kontinentalplatte. Das macht sie geologisch eigenständig – ein Inselarchipel, der durch Subduktionsprozesse entstanden ist.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land, das vollständig von einem anderen Binnenland umschlossen ist (doppelte Enklave)?",
        answerA = "San Marino",
        answerB = "Vatikan",
        answerC = "Lesotho",
        answerD = "Swasiland",
        correctAnswer = 2, // C
        explanation = "Lesotho ist das einzige Land der Welt, das vollständig von einem einzigen anderen Staat umschlossen wird – von Südafrika. Es ist damit eine sogenannte 'Enklave'. Der Vatikan und San Marino sind ebenfalls Enklaven, aber sie liegen in Italien. Lesotho liegt komplett innerhalb Südafrikas.",
        difficulty = 4,
        funFact = "Lesotho liegt vollständig über 1.400 m Höhe – es ist damit das einzige Land der Welt, das komplett oberhalb von 1.000 m liegt. Die tiefste Stelle des Landes ist noch höher als viele Berge Europas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat die höchste durchschnittliche Höhe über dem Meeresspiegel?",
        answerA = "Asien",
        answerB = "Südamerika",
        answerC = "Nordamerika",
        answerD = "Antarktis",
        correctAnswer = 3, // D
        explanation = "Die Antarktis hat mit einer Durchschnittshöhe von ca. 2.300 m den höchsten Mittelpunkt aller Kontinente – weit vor Asien (ca. 960 m) und Südamerika (ca. 590 m). Dies liegt am massiven Eisschild, der teilweise über 4.000 m dick ist.",
        difficulty = 4,
        funFact = "Ohne das Eis wäre die Antarktis großteils unter dem Meeresspiegel, da das Gewicht des Eises die Erdkruste um Hunderte von Metern eingedrückt hat – ein Phänomen namens Isostatische Absenkung."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Binnenmeer ist das salzhaltigste der Erde?",
        answerA = "Totes Meer",
        answerB = "Don Juan Pond (Antarktis)",
        answerC = "Assal-See (Dschibuti)",
        answerD = "Großer Salzsee (USA)",
        correctAnswer = 1, // B
        explanation = "Der Don Juan Pond in der Wright Valley (Antarktis) hat einen Salzgehalt von bis zu 44 % – er ist das salzhaltigste natürliche Gewässer der Erde und friert selbst bei −53 °C nicht ein. Das Tote Meer liegt bei ca. 30–34 %. Der Don Juan Pond ist allerdings winzig.",
        difficulty = 4,
        funFact = "Trotz des extremen Salzgehalts wurden im Don Juan Pond vereinzelt salzliebende Mikroorganismen (Halophile) nachgewiesen – ein Hinweis darauf, dass Leben auch unter extremsten Bedingungen möglich ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße trennt Australien von Papua-Neuguinea?",
        answerA = "Timorsee",
        answerB = "Torresstraße",
        answerC = "Arafurasee",
        answerD = "Karpentaria-Golf",
        correctAnswer = 1, // B
        explanation = "Die Torresstraße liegt zwischen dem australischen Bundesstaat Queensland (Cape York Peninsula) und Papua-Neuguinea und ist an ihrer engsten Stelle nur ca. 150 km breit. Sie ist seicht und mit Riffen durchsetzt – für die Navigation anspruchsvoll.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das jüngste international anerkannte Land der Welt (Stand 2024)?",
        answerA = "Kosovo",
        answerB = "Montenegro",
        answerC = "Südsudan",
        answerD = "Timor-Leste",
        correctAnswer = 2, // C
        explanation = "Südsudan erklärte am 9. Juli 2011 seine Unabhängigkeit vom Sudan und ist damit das jüngste UN-Mitglied und das jüngste international anerkannte Land der Welt (Stand 2024). Es wurde nach einem langen Bürgerkrieg und einem Referendum gegründet.",
        difficulty = 4,
        funFact = "Südsudan hat eine der niedrigsten Alphabetisierungsraten der Welt (ca. 27 %) und kämpft seit der Unabhängigkeit mit schwerem internen Bürgerkrieg. Trotzdem erkannten ihn innerhalb von Stunden nach der Unabhängigkeitserklärung zahlreiche Länder an."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss entwässert das größte Einzugsgebiet in Australien?",
        answerA = "Murray",
        answerB = "Darling",
        answerC = "Murray-Darling-System",
        answerD = "Cooper Creek",
        correctAnswer = 2, // C
        explanation = "Das Murray-Darling-Flusssystem entwässert ca. 1,06 Millionen km² – das größte Einzugsgebiet Australiens (etwa 14 % der Landesfläche). Der Murray und der Darling sind die beiden größten Flüsse Australiens und vereinigen sich in New South Wales.",
        difficulty = 4,
        funFact = "Trotz seines riesigen Einzugsgebiets ist der Murray-Darling eines der wasserärmsten Flusssysteme der Welt im Verhältnis zur Fläche – Australien ist der trockenste bewohnte Kontinent der Erde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches britische Überseegebiet liegt am weitesten von Großbritannien entfernt?",
        answerA = "Falklandinseln",
        answerB = "Britisches Antarktis-Territorium",
        answerC = "Pitcairn-Inseln",
        answerD = "Südgeorgien",
        correctAnswer = 2, // C
        explanation = "Die Pitcairn-Inseln im Südpazifik liegen ca. 17.000 km von London entfernt und sind damit das am weitesten entfernte britische Überseegebiet. Die Inseln haben nur etwa 50 Einwohner – Nachfahren der Meuterer der HMS Bounty.",
        difficulty = 4,
        funFact = "Pitcairn ist das bevölkerungsärmste Territorium der Erde. Die Inseln sind so abgelegen, dass Schiffe nur wenige Male im Jahr anlegen. Die Einwohner verdienen ihren Lebensunterhalt u.a. durch den Verkauf von Briefmarken an Sammler."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat eine Hauptstadt, die an der Mündung zweier Flüsse liegt und deren Name übersetzt 'Guter Wind' bedeutet?",
        answerA = "Montevideo, Uruguay",
        answerB = "Asunción, Paraguay",
        answerC = "Buenos Aires, Argentinien",
        answerD = "Santiago, Chile",
        correctAnswer = 2, // C
        explanation = "Buenos Aires bedeutet auf Spanisch 'gute Lüfte/Winde'. Die argentinische Hauptstadt liegt am Rio de la Plata (dem breitesten Fluss der Welt an seiner Mündung), der seinerseits aus dem Zusammenfluss von Paraná und Uruguay entsteht.",
        difficulty = 4,
        funFact = "Der Rio de la Plata ist an seiner Mündung 290 km breit – das macht ihn zum breitesten Fluss der Welt. Auf der argentinischen Seite liegt Buenos Aires, auf der uruguayischen Seite Montevideo."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer hat den höchsten durchschnittlichen Salzgehalt aller offenen Meere?",
        answerA = "Arabisches Meer",
        answerB = "Rotes Meer",
        answerC = "Mittelmeer",
        answerD = "Persischer Golf",
        correctAnswer = 1, // B
        explanation = "Das Rote Meer hat mit einem durchschnittlichen Salzgehalt von ca. 40–41 ‰ (Promille) den höchsten Salzgehalt aller offenen Meere der Welt. Dies liegt an hoher Verdunstung, geringen Niederschlägen und kaum Süßwasserzufluss. Das Mittelmeer liegt bei ca. 38 ‰.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land weltweit, das die Todesstrafe ausschließlich für Landesverrat (nicht für Mord) in seiner Verfassung hat?",
        answerA = "Israel",
        answerB = "Norwegen",
        answerC = "Mexiko",
        answerD = "Finnland",
        correctAnswer = 0, // A
        explanation = "Israel hat die Todesstrafe nur für NS-Kriegsverbrechen und Verrat in Kriegszeiten in seinem Rechtssystem – sie wurde nur einmal angewendet: 1962 an Adolf Eichmann. Für gewöhnliche Straftaten wie Mord gibt es in Israel keine Todesstrafe.",
        difficulty = 4,
        funFact = "Adolf Eichmann, der Organisator des Holocaust, wurde 1960 von israelischen Agenten in Argentinien entführt, nach Israel gebracht, 1961 verurteilt und 1962 hingerichtet – die einzige Hinrichtung in der Geschichte des Staates Israel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die höchste Küstenlinie im Verhältnis zu seiner Landfläche?",
        answerA = "Griechenland",
        answerB = "Norwegen",
        answerC = "Kroatien",
        answerD = "Indonesien",
        correctAnswer = 1, // B
        explanation = "Norwegen hat im Verhältnis zu seiner Landfläche eine der längsten Küstenlinien der Welt – ca. 83.281 km (mit Fjorden und Inseln) bei einer Landfläche von 385.000 km². Das Verhältnis von ca. 0,22 km Küste pro km² Land ist weltweit unter Großstaaten einzigartig.",
        difficulty = 4,
        funFact = "Würde man die norwegischen Fjordküsten gerade strecken, würden sie zweieinhalbmal um die Erde reichen. Der Sognefjord, der längste Fjord Norwegen, ist 204 km lang und bis zu 1.308 m tief."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge trennt Europa von Afrika in Marokko?",
        answerA = "Rif-Gebirge",
        answerB = "Hoher Atlas",
        answerC = "Anti-Atlas",
        answerD = "Sahara-Atlas",
        correctAnswer = 0, // A
        explanation = "Das Rif-Gebirge im Norden Marokkos verläuft parallel zur Mittelmeerküste und trennt das Mittelmeer-Klima Nordmarokkos vom Rest des Landes. Es liegt der iberischen Halbinsel gegenüber und ist geologisch mit der Betischen Kordillere Spaniens verwandt – beide entstanden durch dieselbe alpidische Gebirgsbildung.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Schelf gilt als der breiteste kontinentale Schelf der Welt?",
        answerA = "Sibirischer Arktisschelf",
        answerB = "Nordseeschelf",
        answerC = "Sahul-Schelf (Australien-Neuguinea)",
        answerD = "Patagonischer Schelf",
        correctAnswer = 0, // A
        explanation = "Der sibirische Arktisschelf – bestehend aus dem Karischen Meer, der Laptewsee und der Ostsibirischen See – ist der breiteste und flachste Kontinentalschelf der Welt. Er erstreckt sich auf über 1.500 km Breite und ist meist weniger als 50 m tief.",
        difficulty = 4,
        funFact = "Der sibirische Arktisschelf enthält enorme Mengen an Methanhydrat (gefrorenem Methan). Wenn der Permafrost durch den Klimawandel taut, könnte dieses Methan freigesetzt werden – ein möglicher Kipppunkt für das Erdklima."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat als erstes den Freihandel auf einer ganzen Insel eingeführt und gilt als Geburtsstätte moderner Freihäfen?",
        answerA = "Singapur",
        answerB = "Großbritannien (Gibraltar)",
        answerC = "Niederlande",
        answerD = "Venedig (Historische Republik)",
        correctAnswer = 1, // B
        explanation = "Gibraltar wurde 1704 von Großbritannien eingenommen und bereits 1705 als Freihafen erklärt – einer der frühesten modernen Freihäfen der Welt. Heute ist Gibraltar für seinen Duty-Free-Status bekannt und zieht Millionen Tagestouristen aus Spanien an.",
        difficulty = 4,
        funFact = "Gibraltar hat mehr registrierte Unternehmen als Einwohner (ca. 30.000 Menschen, aber über 75.000 registrierte Firmen) – viele nutzen den günstigen Steuerstatus des britischen Überseegebiets."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land Asiens, das nie kolonisiert wurde (im modernen Sinne)?",
        answerA = "China",
        answerB = "Japan",
        answerC = "Thailand",
        answerD = "Korea",
        correctAnswer = 2, // C
        explanation = "Thailand (historisch: Siam) ist das einzige südostasiatische Land, das nie zur Kolonie einer europäischen Macht wurde. König Chulalongkorn (Rama V.) navigierte geschickt zwischen dem britischen Burma und dem französischen Indochina und bewahrte die Unabhängigkeit durch Diplomatie und Reformen.",
        difficulty = 4,
        funFact = "Der Name 'Thailand' bedeutet wörtlich 'Land der Freien' (Thai = frei). Dies ist kein Zufall – die Thais sind stolz darauf, nie kolonisiert worden zu sein, und dieser Aspekt ist Teil der nationalen Identität."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Gebirgswall des Meeresbodens der Erde?",
        answerA = "Mittelatlantischer Rücken",
        answerB = "Pazifisch-Antarktischer Rücken",
        answerC = "Mittelozeanischer Rücken (gesamtes System)",
        answerD = "Indischer Ozeanrücken",
        correctAnswer = 2, // C
        explanation = "Das gesamte System der mittelozeanischen Rücken erstreckt sich über ca. 65.000 km durch alle Ozeane – es ist die längste Gebirgskette der Erde, aber fast vollständig unter Wasser. Der Mittelatlantische Rücken allein ist ca. 16.000 km lang.",
        difficulty = 4,
        funFact = "An mittelozeanischen Rücken entstehen täglich ca. 2 km² neuer Ozeanboden durch Lavaaustritte. An diesen hydrothermalen Schloten (Black Smokers) leben einzigartige Ökosysteme, die ohne Sonnenlicht funktionieren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt liegt am nördlichsten unter den Hauptstädten der Welt mit über 1 Million Einwohnern?",
        answerA = "Oslo",
        answerB = "Helsinki",
        answerC = "Moskau",
        answerD = "Stockholm",
        correctAnswer = 2, // C
        explanation = "Moskau liegt auf 55°45'N und ist die nördlichste Millionenstadt der Welt. Oslo liegt auf 59°55'N, Helsinki auf 60°10'N und Stockholm auf 59°20'N – aber keine dieser Städte hat über 1 Million Einwohner im Stadtgebiet im Vergleich zu Moskaus ca. 12 Millionen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meeresgebiet gilt als das Bermudadreieck der See – mit unerklärlich vielen Schiffs- und Flugzeugverlusten im Pazifik?",
        answerA = "Drachengdreieck (Devil's Sea)",
        answerB = "Formosa-Kanal",
        answerC = "Philippinensee",
        answerD = "Sulusee",
        correctAnswer = 0, // A
        explanation = "Das 'Teufels-Meer' (Devil's Sea / Dragon's Triangle) östlich von Japan und südlich von Tokio auf dem Philippinischen Meer gilt als pazifisches Pendant zum Bermudadreieck. Japanische Behörden erklärten das Gebiet in den 1950er Jahren vorübergehend zur Gefahrenzone.",
        difficulty = 4,
        funFact = "Das Drachengdreieck liegt am Rande der Pazifischen Feuerring-Zone mit besonders aktiver vulkanischer und seismischer Aktivität – was manche der historischen Schiffsverluste erklären könnte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Welt, dessen Name ein Adjektiv ist?",
        answerA = "Vereinigte Arabische Emirate",
        answerB = "Dominikanische Republik",
        answerC = "Zentralafrikanische Republik",
        answerD = "Westliche Sahara",
        correctAnswer = 1, // B
        explanation = "Die Dominikanische Republik ist das einzige Land der Welt, dessen offizieller kurzgefasster Name ein Adjektiv (dominikanisch) enthält, da der Vollname 'República Dominicana' kein Substantiv für die Nation selbst liefert – 'Dominikanische' ist das zentrale Namenselement.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Durch welchen Ozean verläuft die Datumsgrenze (International Date Line) hauptsächlich?",
        answerA = "Indischer Ozean",
        answerB = "Atlantik",
        answerC = "Arktischer Ozean",
        answerD = "Pazifik",
        correctAnswer = 3, // D
        explanation = "Die internationale Datumsgrenze verläuft hauptsächlich durch den Pazifischen Ozean entlang des 180°-Meridians, macht aber mehrere Ausnahmen, um Länder nicht zu teilen (z.B. Kiribati, Samoa, Fidschi). Westlich der Linie ist ein Tag früher als östlich davon.",
        difficulty = 4,
        funFact = "Kiribati verschob 1995 die Datumsgrenze östlich um den Linienkomplex, sodass der ganze Staat auf der gleichen Seite liegt. Damit liegt Kiribati als erstes Land täglich im neuen Tag – Silvester 1999/2000 wurde in Kiribati zuerst auf der Welt gefeiert."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie nennt man das Phänomen, bei dem ein Fluss plötzlich sein Bett ändert und in eine andere Richtung fließt?",
        answerA = "Avulsion",
        answerB = "Meandrierung",
        answerC = "Braided River",
        answerD = "Fluviale Transgression",
        correctAnswer = 0, // A
        explanation = "Avulsion bezeichnet das plötzliche Verlegen eines Flussbettes, bei dem der Fluss ein neues Hauptgerinne schafft und das alte verlässt. Dies kann durch Überschwemmungen, tektonische Verschiebungen oder Sedimentaufschüttung ausgelöst werden. Der Mississippi hat so seinen Lauf mehrfach verändert.",
        difficulty = 4,
        funFact = "Der Huang He (Gelber Fluss) in China hat sein Flussdelta in der Geschichte mindestens 26 Mal dramatisch verschoben – mal mündete er nördlich, mal südlich der Shandong-Halbinsel. Diese Avulsionen verursachten katastrophale Überschwemmungen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Archipel liegt im Zentrum des 'Pacific Ring of Fire' und hat die höchste vulkanische Dichte?",
        answerA = "Hawaii",
        answerB = "Kamtschatka",
        answerC = "Vanuatu",
        answerD = "Kurilen",
        correctAnswer = 2, // C
        explanation = "Vanuatu im Südpazifik hat eine der höchsten vulkanischen Dichten weltweit – auf dem Archipel mit 83 Inseln befinden sich 9 aktive Vulkane. Das Land ist eines der tektonisch aktivsten der Erde und liegt an der Kollisionszone des Pazifischen und Australischen Platte.",
        difficulty = 4,
        funFact = "Auf der Vanuatu-Insel Tanna befindet sich der Yasur, einer der zugänglichsten aktiven Vulkane der Welt. Touristen können bis an den Kraterkrater herantreten – trotz regelmäßiger Ausbrüche."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Binnenland ist am stärksten von Meeren umgeben (muss am meisten fremde Staatsgebiete passieren, um den Ozean zu erreichen)?",
        answerA = "Mongolei",
        answerB = "Kasachstan",
        answerC = "Usbekistan",
        answerD = "Liechtenstein",
        correctAnswer = 2, // C
        explanation = "Usbekistan ist das einzige 'doppelt eingeschlossene' Land der Welt: Es ist ein Binnenland, das vollständig von anderen Binnenländern umgeben ist. Um den Ozean zu erreichen, muss man mindestens zwei Landesgrenzen überqueren. Liechtenstein teilt diesen Status mit Usbekistan.",
        difficulty = 4,
        funFact = "Liechtenstein und Usbekistan sind die einzigen beiden 'doubly landlocked countries' – Länder, die von Binnenländern umgeben sind. Liechtenstein liegt zwischen der Schweiz und Österreich (beide keine Meeresanrainer)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die am dichtesten besiedelte Insel der Welt (nach Einwohnern pro km², mit über 1 Mio. Einwohnern)?",
        answerA = "Singapur",
        answerB = "Bali",
        answerC = "Honshū",
        answerD = "Dhaka (Bangladesch-Insel im Delta)",
        correctAnswer = 0, // A
        explanation = "Singapur (als Stadtstaat-Insel) hat mit ca. 7.900 Einwohnern/km² eine der höchsten Bevölkerungsdichten aller selbständigen Inseln weltweit, die zugleich souveräne Staaten sind. Die Insel hat ca. 728 km² und über 5,9 Millionen Einwohner.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der geografische Begriff für eine Halbinsel innerhalb einer Halbinsel?",
        answerA = "Sub-Halbinsel",
        answerB = "Chersonesos",
        answerC = "Landenge",
        answerD = "Es gibt keinen speziellen Begriff",
        correctAnswer = 3, // D
        explanation = "Es gibt keinen allgemein anerkannten geografischen Fachbegriff für eine Halbinsel innerhalb einer Halbinsel. Das Phänomen existiert (z.B. Cape Cod auf der bereits halbinselartigen östlichen US-Küste), aber die Geografen haben dafür keine standardisierte Bezeichnung. 'Chersonesos' ist das griechische Wort für Halbinsel generell.",
        difficulty = 4,
        funFact = "Die Krim-Halbinsel ist ihrerseits eine Halbinsel auf der größeren Krim-Region im Schwarzen Meer. Dieses Phänomen nennen Geomorphologen manchmal informell 'nested peninsula', aber ein offizieller Fachbegriff fehlt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der östlichste Punkt des europäischen Kontinents (ohne Russland)?",
        answerA = "Kap Hoornos (Spanien)",
        answerB = "Kap Dezhnev (Russland)",
        answerC = "Bosporus-Brücke (Türkei)",
        answerD = "Kap Kanin Nos (Russland)",
        correctAnswer = 2, // C
        explanation = "Der östlichste Punkt Europas (ohne russisches Territorium) liegt in der Türkei an der asiatisch-europäischen Grenze. Der Bosporus markiert die Ostgrenze des europäischen Teils der Türkei. Kap Dezhnev ist der östlichste Punkt Russlands (und damit Europas/Asiens), aber liegt in Asien.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Bundesland Deutschlands grenzt an die meisten anderen Bundesländer?",
        answerA = "Bayern",
        answerB = "Hessen",
        answerC = "Nordrhein-Westfalen",
        answerD = "Thüringen",
        correctAnswer = 3, // D
        explanation = "Thüringen grenzt an sechs andere Bundesländer: Bayern, Hessen, Niedersachsen, Sachsen-Anhalt, Sachsen und Nordrhein-Westfalen (über eine winzige Grenze). Damit teilt Thüringen den Rekord mit Hessen (ebenfalls sechs Nachbarn). Der geografisch zentrale Lage macht Thüringen zur Mitte Deutschlands.",
        difficulty = 4,
        funFact = "Thüringen wird als das 'grüne Herz Deutschlands' bezeichnet – 34 % der Fläche sind Wald. Der Thüringer Wald, das Schiefergebirge und das Kyffhäusergebirge prägen das Landschaftsbild."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher See ist der größte tropische See der Welt?",
        answerA = "Victoriasee",
        answerB = "Tanganjikasee",
        answerC = "Malawisee",
        answerD = "Tschadsee",
        correctAnswer = 0, // A
        explanation = "Der Victoriasee in Ostafrika (geteilt zwischen Uganda, Tansania und Kenia) ist mit ca. 68.870 km² der größte tropische See und der zweitgrößte Süßwassersee der Welt nach dem Oberen See. Er liegt zwischen dem Äquator und 3°S auf 1.135 m Höhe.",
        difficulty = 4,
        funFact = "Der Victoriasee ist eine der Quellen des Nil. Der Weißen Nil verlässt den See an den Ripon Falls in Uganda und ist der längere Quellfluss des Nils – obwohl der Blaue Nil (aus Äthiopien) mehr Wasser führt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die einzige Stadt der Welt, die als Welterbe insgesamt eingetragen ist (komplette Stadt)?",
        answerA = "Venedig",
        answerB = "Rom",
        answerC = "Valletta (Malta)",
        answerD = "Dubrovnik",
        correctAnswer = 2, // C
        explanation = "Valletta auf Malta wurde 1980 als erste Hauptstadt vollständig in die UNESCO-Welterbeliste aufgenommen. Die gesamte Stadt innerhalb der historischen Mauern ist Welterbe. Dubrovnik hat seine Altstadt als Welterbe, aber Valletta war die erste vollständig eingetragene Hauptstadt.",
        difficulty = 4,
        funFact = "Valletta ist die kleinste Hauptstadt der EU mit nur ca. 6.000 Einwohnern innerhalb der Stadtmauern. Sie wurde 1566 von den Johannitern (Malteserrittern) nach deren Großmeister Jean de Valette benannt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat Ozeaniens hat die meisten Einwohner?",
        answerA = "Fidschi",
        answerB = "Papua-Neuguinea",
        answerC = "Neuseeland",
        answerD = "Vanuatu",
        correctAnswer = 1, // B
        explanation = "Papua-Neuguinea ist mit ca. 10 Millionen Einwohnern das bevölkerungsreichste Land Ozeaniens (wenn Australien separat betrachtet wird). Neuseeland hat ca. 5 Millionen Einwohner, Fidschi ca. 900.000. PNG ist auch das flächenmäßig größte Inselland Ozeaniens.",
        difficulty = 4,
        funFact = "Papua-Neuguinea hat über 800 lebende Sprachen – das sind mehr als in jedem anderen Land der Welt, etwa 12 % aller Sprachen der Menschheit. Dies spiegelt die extreme kulturelle und geografische Isolation der Bergregionen wider."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Hochplateau gilt als 'Dach der Welt'?",
        answerA = "Deosai-Hochplateau (Pakistan)",
        answerB = "Tibetisches Hochplateau",
        answerC = "Altiplano (Südamerika)",
        answerD = "Deccan-Plateau (Indien)",
        correctAnswer = 1, // B
        explanation = "Das Tibetische Hochplateau in Zentralasien hat eine Durchschnittshöhe von über 4.500 m über dem Meeresspiegel und wird als 'Dach der Welt' bezeichnet. Es erstreckt sich über ca. 2,5 Millionen km² – etwa viermal die Fläche Frankreichs – zwischen dem Himalaya im Süden und dem Kunlun im Norden.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel wurde durch einen Vulkanausbruch im Jahr 1963 neu geschaffen?",
        answerA = "Ferdinandea (Mittelmeer)",
        answerB = "Surtsey (Island)",
        answerC = "Hunga Tonga (Pazifik)",
        answerD = "Nishinoshima (Japan)",
        correctAnswer = 1, // B
        explanation = "Surtsey vor der Südküste Islands entstand zwischen November 1963 und Juni 1967 durch einen unterseeischen Vulkanausbruch. Die Insel wurde 2008 zum UNESCO-Weltnaturerbe erklärt, da sie ein einzigartiges natürliches Laboratorium für die Besiedlung neu entstandenen Landes bietet.",
        difficulty = 4,
        funFact = "Surtsey ist für die Öffentlichkeit gesperrt – nur wenige Wissenschaftler dürfen die Insel besuchen, um zu untersuchen, wie sich Leben auf einem neuen Stück Erde ausbreitet. Bereits innerhalb von Jahren siedelten sich Vögel, Pflanzen und Insekten an."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Wüste der Erde (nach Fläche)?",
        answerA = "Sahara",
        answerB = "Arabische Wüste",
        answerC = "Gobi",
        answerD = "Antarktis",
        correctAnswer = 3, // D
        explanation = "Die Antarktis ist mit ca. 14,2 Millionen km² die größte Wüste der Erde – eine Kältewüste mit weniger als 200 mm Niederschlag pro Jahr (als Schnee). Die Sahara mit ca. 9 Millionen km² ist die größte Hitzewüste. Wüsten werden nach Niederschlag definiert, nicht nach Temperatur.",
        difficulty = 4,
        funFact = "Die Antarktis ist so trocken, dass die 'trockenen Täler' (McMurdo Dry Valleys) als trockenste Orte der Erde gelten – sie sind seit Millionen von Jahren eisfrei und fast ohne jegliche Feuchtigkeit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Erde, das zwei separate, nicht zusammenhängende Landteile hat, die beide als souveränes Kernterritorium gelten (ohne Exklaven)?",
        answerA = "Bangladesch",
        answerB = "Pakistan (West und Ost, bis 1971)",
        answerC = "Malaysia (Malaiische Halbinsel + Borneo)",
        answerD = "USA (Alaska + Kontinentalstaaten)",
        correctAnswer = 2, // C
        explanation = "Malaysia besteht aus zwei nicht zusammenhängenden Gebieten: Westmalaysia auf der Malaiischen Halbinsel und Ostmalaysia (Sabah und Sarawak) auf Borneo – getrennt durch das Südchinesische Meer. Beide Teile gehören zum souveränen Kernterritorium, nicht zu Überseegebieten.",
        difficulty = 4,
        funFact = "Zwischen Westmalaysia (Kuala Lumpur) und Ostmalaysia liegen ca. 1.400 km Meer. Malaysische Staatsbürger benötigen zwar keinen Reisepass, aber einen Personalausweis für die Reise zwischen beiden Landesteilen – Einwanderungskontrollen sind vorhanden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der einzige Fluss der Welt, der in zwei verschiedene Ozeane mündet?",
        answerA = "Kein Fluss mündet in zwei Ozeane",
        answerB = "Orinoko",
        answerC = "Paranaíba",
        answerD = "Okavango",
        correctAnswer = 0, // A
        explanation = "Kein einzelner Fluss mündet direkt in zwei verschiedene Ozeane – das widerspricht der hydrografischen Definition. Wasserscheiden trennen die Einzugsgebiete verschiedener Ozeane. Der Casiquiare verbindet zwei Atlantik-Flusssysteme (Orinoko und Amazonas), aber beide münden in den Atlantik.",
        difficulty = 4,
        funFact = "Die kontinentale Hauptwasserscheide (Continental Divide) in Nordamerika trennt Wasser, das in den Atlantik fließt, von Wasser, das in den Pazifik fließt. Sie verläuft durch die Rocky Mountains und ist an manchen Stellen kaum erkennbar."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße hat den weltweit dichtesten Schiffsverkehr?",
        answerA = "Straße von Malakka",
        answerB = "Ärmelkanal",
        answerC = "Straße von Gibraltar",
        answerD = "Straße von Hormus",
        correctAnswer = 1, // B
        explanation = "Der Ärmelkanal (La Manche) zwischen England und Frankreich hat den dichtesten Schiffsverkehr weltweit – über 500 Schiffe pro Tag passieren ihn. Die Straße von Malakka hat zwar mehr Gesamtvolumen an Gütern, aber der Ärmelkanal hat die höchste Schiffsdichte und ein komplexes Verkehrsleitsystem.",
        difficulty = 4,
        funFact = "Der Ärmelkanal wird durch Verkehrstrennungsgebiete (TSS) gesteuert – ähnlich wie Autobahnen: Schiffe müssen je nach Fahrtrichtung verschiedene Spuren nutzen. An der schmalsten Stelle (Pas-de-Calais) sind es nur 33 km."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Meer liegt die Insel Zanzibar?",
        answerA = "Golf von Aden",
        answerB = "Arabisches Meer",
        answerC = "Indischer Ozean (nahe Tansanias Küste)",
        answerD = "Mosambikkanal",
        correctAnswer = 2, // C
        explanation = "Zanzibar liegt im Indischen Ozean, ca. 35 km vor der Küste des tansanischen Festlandes. Die Inselgruppe (Hauptinsel Unguja und Pemba) ist Teil der Vereinigten Republik Tansania, hat aber eine weitgehende Autonomie und eine eigene Regierung.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Welt, das in seinem offiziellen Namen das Wort 'demokratisch' führt, aber eine Monarchie ist?",
        answerA = "Demokratische Volksrepublik Korea",
        answerB = "Kambodscha",
        answerC = "Demokratisches Königreich Bhutan",
        answerD = "Lesotho",
        correctAnswer = 1, // B
        explanation = "Kambodscha heißt offiziell 'Königreich Kambodscha' (Preăh Réachéanachâk Kâmpŭchéa) – kein 'demokratisch' im Namen. Bhutan ist eine konstitutionelle Monarchie ohne 'demokratisch' im offiziellen Namen. Nordkorea nennt sich 'Demokratische Volksrepublik', ist aber eine Diktatur. Diese Frage zeigt: Kambodscha ist konstitutionelle Monarchie mit Parlamentswahlen.",
        difficulty = 4,
        funFact = "Das Königreich Kambodscha kehrte 1993 zur Monarchie zurück, nachdem es seit 1970 Republik war. König Norodom Sihanouk wurde restauriert. Sein Sohn Sihamoni ist seit 2004 König."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge hat den größten Unterschied zwischen Gipfelhöhe und Talsohle (relative Höhe)?",
        answerA = "Himalaya",
        answerB = "Mauna Kea (Hawaii)",
        answerC = "Olympus Mons (Mars)",
        answerD = "Kilimandscharo",
        correctAnswer = 1, // B
        explanation = "Der Mauna Kea auf Hawaii hat eine Gesamthöhe von ca. 10.210 m vom Meeresgrund bis zum Gipfel – obwohl er nur 4.205 m über dem Meeresspiegel liegt. Er ist damit der höchste Berg der Erde, wenn man vom Fuß des Berges misst, und überragt den Everest (8.849 m ü.M.) um über 1.300 m.",
        difficulty = 4,
        funFact = "Auf dem Gipfel des Mauna Kea stehen einige der bedeutendsten Observatorien der Welt – die trockene Luft und das klare Wetter in über 4.000 m Höhe machen ihn zum idealen Standort für astronomische Beobachtungen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land Europas, das eine Landgrenze mit Russland, aber keine Seegrenze mit der Ostsee hat?",
        answerA = "Weißrussland",
        answerB = "Ukraine",
        answerC = "Georgien",
        answerD = "Aserbaidschan",
        correctAnswer = 0, // A
        explanation = "Weißrussland (Belarus) grenzt im Nordosten an Russland, hat aber keinen Meereszugang. Es ist ein vollständiges Binnenland, das zwischen Russland, Ukraine, Polen, Litauen und Lettland liegt – damit ist es das größte Binnenland Europas nach Kasachstan (je nach Definition).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt vollständig innerhalb des Tropengürtels zwischen den Wendekreisen?",
        answerA = "Karibisches Meer",
        answerB = "Arabisches Meer",
        answerC = "Südchinesisches Meer",
        answerD = "Banda-See",
        correctAnswer = 0, // A
        explanation = "Das Karibische Meer liegt vollständig zwischen dem Wendekreis des Krebses (23,5°N) und dem Äquator, also im tropischen Bereich. Es wird von den Großen Antillen, Kleinen Antillen und dem zentralamerikanischen sowie südamerikanischen Festland begrenzt.",
        difficulty = 4,
        funFact = "Das Karibische Meer hat eine Fläche von ca. 2,75 Millionen km² und eine maximale Tiefe von 7.686 m (Kaimangraben). Es ist eines der produktivsten Meeresgebiete mit enorimer Korallenriff-Biodiversität."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt die schmale Landverbindung zwischen Nord- und Südamerika?",
        answerA = "Landenge von Darien",
        answerB = "Isthmus von Panama",
        answerC = "Tehuantepec-Isthmus",
        answerD = "Yucatán-Halbinsel",
        correctAnswer = 1, // B
        explanation = "Der Isthmus von Panama (Landenge von Panama) ist die schmalste Verbindung zwischen Nord- und Südamerika – an seiner engsten Stelle nur ca. 80 km breit. Hier wurde 1914 der Panamakanal gebaut, der Atlantik und Pazifik verbindet.",
        difficulty = 4,
        funFact = "Der Isthmus von Panama entstand vor ca. 3 Millionen Jahren, als sich Nord- und Südamerika verbanden. Dies veränderte die Meeresströmungen grundlegend und trug möglicherweise zur Eiszeit bei – und ermöglichte die Wanderung von Tieren zwischen den Kontinenten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das am höchsten gelegene Hauptstadtgebiet (Metropolregion) Europas?",
        answerA = "Madrid",
        answerB = "Bern",
        answerC = "Andorra la Vella",
        answerD = "Innsbruck",
        correctAnswer = 2, // C
        explanation = "Andorra la Vella, die Hauptstadt des Fürstentums Andorra, liegt auf 1.023 m über dem Meeresspiegel – damit ist sie die höchstgelegene Hauptstadt Europas. Madrid liegt auf ca. 650 m, Bern auf ca. 540 m. Andorra liegt in den Pyrenäen zwischen Frankreich und Spanien.",
        difficulty = 4,
        funFact = "Andorra hat kein eigenes Militär und keine eigene Nationalbank, aber es ist kein EU-Mitglied – obwohl es den Euro als Währung benutzt. Das Fürstentum wird seit 1278 von zwei Fürsten gemeinsam regiert: dem Bischof von Urgell und dem Präsidenten Frankreichs."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat die meisten Länder?",
        answerA = "Asien",
        answerB = "Amerika (Nord + Süd)",
        answerC = "Afrika",
        answerD = "Europa",
        correctAnswer = 2, // C
        explanation = "Afrika hat mit 54 anerkannten Staaten die meisten Länder aller Kontinente. Asien hat 49, Europa 44 (je nach Zählweise) und die Amerikas zusammen 35 Länder. Dies ist das Ergebnis der kolonialen Dekolonisierung nach dem Zweiten Weltkrieg.",
        difficulty = 4,
        funFact = "Die meisten afrikanischen Staatsgrenzen wurden 1884–1885 auf der Berliner Konferenz von europäischen Kolonialmächten gezogen – ohne Rücksicht auf ethnische, sprachliche oder kulturelle Grenzen. Diese künstlichen Grenzen sind bis heute eine Ursache vieler Konflikte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer ist das flachste der Welt (geringste Durchschnittstiefe)?",
        answerA = "Nordsee",
        answerB = "Asowsches Meer",
        answerC = "Ostsee",
        answerD = "Kaspisches Meer",
        correctAnswer = 1, // B
        explanation = "Das Asowsche Meer im Nordosten des Schwarzen Meeres ist mit einer Durchschnittstiefe von nur ca. 7 m das flachste Meer der Welt. Die maximale Tiefe beträgt nur 14 m. Es liegt zwischen der Ukraine (Krim-Halbinsel) und Russland (Kuban-Region).",
        difficulty = 4,
        funFact = "Das Asowsche Meer wird regelmäßig im Winter vollständig zugefroren – das einzige Meer Europas, das dies tut. Im Sommer dagegen erwärmt es sich aufgrund der geringen Tiefe sehr schnell auf bis zu 30°C."
    ),

    // ── EXPERT BATCH 3 (difficulty = 4) ── 50 additional questions ───────────

    Question(
        categoryId = 1,
        questionText = "Auf welchen exakten GPS-Koordinaten liegt die Kuppe des Machu Picchu (Huayna Picchu-Gipfel)?",
        answerA = "13°09'23\"S, 72°32'44\"W",
        answerB = "13°09'47\"S, 72°34'38\"W",
        answerC = "12°58'22\"S, 73°10'11\"W",
        answerD = "13°31'09\"S, 71°58'32\"W",
        correctAnswer = 1, // B
        explanation = "Der Gipfel des Huayna Picchu, der imposante Felsturm über der Inka-Stätte Machu Picchu, liegt bei ca. 13°09'47\"S, 72°34'38\"W auf 2.693 m Höhe. Machu Picchu selbst (die Ruinenanlage) liegt bei etwa 13°09'47\"S, 72°32'44\"W. Die Koordinaten sind ein wichtiger Referenzpunkt für Archäologen und GPS-Vermessungen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche zwei Länder streiten sich um die ausschließliche Wirtschaftszone (AWZ) rund um die Falklandinseln?",
        answerA = "Argentinien und Brasilien",
        answerB = "Argentinien und Großbritannien",
        answerC = "Chile und Großbritannien",
        answerD = "Argentinien, Chile und Großbritannien",
        correctAnswer = 1, // B
        explanation = "Die ausschließliche Wirtschaftszone rund um die Falklandinseln (Malvinas) ist Gegenstand eines langjährigen Streits zwischen Argentinien und Großbritannien. Großbritannien beansprucht eine 200-Seemeilen-AWZ um die Falklands; Argentinien erkennt dies nicht an und besteht auf seiner Souveränität über die Inseln. Die AWZ enthält potenzielle Öl- und Gasvorkommen.",
        difficulty = 4,
        funFact = "Im Falklandkrieg 1982 kämpften Argentinien und Großbritannien um die Inseln. Seitdem unterhält Großbritannien eine bedeutende Militärgarnison dort – die permanente britische Präsenz sichert de facto die AWZ-Kontrolle."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen mittleren Abfluss (m³/s) hat der Kongo – der wasserreichste Fluss Afrikas?",
        answerA = "ca. 12.000 m³/s",
        answerB = "ca. 21.000 m³/s",
        answerC = "ca. 41.000 m³/s",
        answerD = "ca. 57.000 m³/s",
        correctAnswer = 2, // C
        explanation = "Der Kongo hat einen mittleren Abfluss von ca. 41.000 m³/s und ist damit der wasserreichste Fluss Afrikas und der zweitgrößte der Welt nach dem Amazonas (ca. 209.000 m³/s). Seine Wasserführung ist wegen der symmetrischen Lage um den Äquator relativ gleichmäßig – beide Flusshälften haben gegenläufige Regenzeiten.",
        difficulty = 4,
        funFact = "Der Kongograben (Congo Canyon) vor der Küste Kongos ist einer der tiefsten Unterwassercanyon der Erde und wird direkt durch den enormen Sedimenteintrag des Flusses geformt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte Eisfeld außerhalb der Polargebiete (Patagonien)?",
        answerA = "Perito-Moreno-Gletscher",
        answerB = "Campo de Hielo Sur (Südpatagonisches Eisfeld)",
        answerC = "Hubbard-Gletscher (Alaska)",
        answerD = "Vatnajökull (Island)",
        correctAnswer = 1, // B
        explanation = "Das Campo de Hielo Sur (Südpatagonisches Eisfeld) an der Grenze zwischen Argentinien und Chile umfasst ca. 12.363 km² und ist das größte zusammenhängende Eisfeld außerhalb der Polargebiete. Es speist zahlreiche Gletscher, darunter den berühmten Perito Moreno.",
        difficulty = 4,
        funFact = "Das südpatagonische Eisfeld schmilzt messbar: Satellitendaten zeigen einen Massenverlust von etwa 24 Gigatonnen pro Jahr. Es gilt als einer der empfindlichsten Klimaindikatoren der südlichen Hemisphäre."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchem Rang liegt die Tokio-Metropolregion (Greater Tokyo Area) in der weltweiten Rangliste der Stadtregionen nach Einwohnerzahl (Stand 2024)?",
        answerA = "Rang 2",
        answerB = "Rang 3",
        answerC = "Rang 1",
        answerD = "Rang 4",
        correctAnswer = 2, // C
        explanation = "Die Greater Tokyo Area (Tokio, Yokohama, Saitama, Chiba etc.) ist mit ca. 37–38 Millionen Einwohnern nach wie vor die größte Stadtregion (Urban Agglomeration) der Welt. Delhi (ca. 33 Mio.) liegt auf Rang 2, gefolgt von Shanghai. Tokio hält diesen Spitzenwert seit Jahrzehnten.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie alt ist das Skandinavische Kaledonische Gebirge (Gebirgsbildung) in Millionen Jahren?",
        answerA = "ca. 65 Millionen Jahre",
        answerB = "ca. 490–390 Millionen Jahre",
        answerC = "ca. 250 Millionen Jahre",
        answerD = "ca. 1.000 Millionen Jahre",
        correctAnswer = 1, // B
        explanation = "Die kaledonische Gebirgsbildung in Skandinavien und Britischen Inseln fand vor ca. 490–390 Millionen Jahren (Ordovizium bis Devon) statt. Die heutigen skandinavischen Berge sind stark erodierte Reste dieses einst himalayahohen Gebirges. Das Appalachische Gebirge in Nordamerika entstand in der gleichen Kollision.",
        difficulty = 4,
        funFact = "Die Appalachians in Nordamerika und das Skandinavische Gebirge waren einst ein einziger Gebirgszug – bevor der Atlantik durch die Kontinentaldrift auseinanderbrach. Gleiche Fossilien auf beiden Seiten belegen dies."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Vulkanexplosivitätsindex (VEI) hatte der Ausbruch des Krakatau im Jahr 1883?",
        answerA = "VEI 5",
        answerB = "VEI 7",
        answerC = "VEI 6",
        answerD = "VEI 8",
        correctAnswer = 2, // C
        explanation = "Der Krakatau-Ausbruch von 1883 erreichte VEI 6 (colossal). Er schleuderte ca. 21 km³ Material aus und verursachte einen Tsunamie, der über 36.000 Menschen tötete. Die Druckwelle umrundete die Erde mehrfach. Der Tambora-Ausbruch 1815 (VEI 7) war jedoch deutlich größer.",
        difficulty = 4,
        funFact = "Der Krakatau-Ausbruch war so laut, dass er 4.800 km entfernt auf der Insel Rodrigues (Mauritius) gehört wurde – das lauteste je von Menschen aufgezeichnete Ereignis. Die Druckwelle wurde weltweit von Barometern registriert."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der größte Stausee der Welt nach Wasservolumen (Speicherkapazität)?",
        answerA = "Drei-Schluchten-Staudamm (China)",
        answerB = "Kariba-Stausee (Sambia/Simbabwe)",
        answerC = "Owen-Falls-Stausee (Uganda)",
        answerD = "Bratsk-Stausee (Russland)",
        correctAnswer = 1, // B
        explanation = "Der Kariba-Stausee an der Grenze zwischen Sambia und Simbabwe hat eine Speicherkapazität von ca. 180 km³ und ist damit der größte Stausee der Welt nach Volumen. Er wurde durch den Kariba-Damm am Sambesi geschaffen (1959). Der Drei-Schluchten-Damm hat ca. 39 km³.",
        difficulty = 4,
        funFact = "Bei der Flutung des Kariba-Stausees in den späten 1950ern mussten ca. 57.000 Tonga-Menschen umgesiedelt werden – eine der größten Zwangsumsiedlungen im subsaharischen Afrika des 20. Jahrhunderts."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebiet erzeugt eine 'Datumsgrenz-Anomalie', weil Samoa 2011 die Datumsgrenze wechselte?",
        answerA = "Samoa liegt nun westlich der Datumsgrenze, obwohl es östlich davon liegt",
        answerB = "Samoa liegt nun östlich der Datumsgrenze, obwohl es geografisch westlich liegt",
        answerC = "Amerikanisch-Samoa und Samoa teilen nun die gleiche Zeitzone",
        answerD = "Samoa und Tonga liegen auf verschiedenen Seiten der Datumsgrenze",
        correctAnswer = 0, // A
        explanation = "Samoa wechselte am 29. Dezember 2011 von östlich nach westlich der Datumsgrenze – indem es den 30. Dezember 2011 übersprungen wurde. Geografisch liegt Samoa östlich von 180°, aber politisch-wirtschaftlich (Handel mit Australien und Neuseeland) erschien es sinnvoll, auf deren Seite zu sein. Amerikanisch-Samoa liegt nun auf der entgegengesetzten Seite.",
        difficulty = 4,
        funFact = "Durch den Zeitzonenwechsel erlebten die Samoaner Silvester 2011 zuerst (noch vor Neuseeland und Australien), obwohl sie ihn zuvor als eine der letzten Nationen gefeiert hatten. Das kleine Samoa lag nun zeitlich auf dem neuesten Stand."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen exakten Koordinaten liegt der Punkt Nemo – der ozeanische Pol der Unzugänglichkeit?",
        answerA = "45°52'S, 123°23'W",
        answerB = "48°52'S, 123°23'W",
        answerC = "49°27'S, 119°42'W",
        answerD = "51°12'S, 131°15'W",
        correctAnswer = 1, // B
        explanation = "Punkt Nemo liegt bei ca. 48°52.6'S, 123°23.6'W im Südpazifik und ist der am weitesten von jeder Küste entfernte Punkt im Ozean – ca. 2.688 km von der nächsten Küste entfernt (Inseln Ducie, Motu Nui und die Antarktis). Er liegt in der South Pacific Gyre, einem fast leblosen Meeresbereich.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen mittleren Abfluss hat der Ganges an seiner Mündung ins Bengalische Meer?",
        answerA = "ca. 11.000 m³/s",
        answerB = "ca. 35.000 m³/s",
        answerC = "ca. 5.500 m³/s",
        answerD = "ca. 19.000 m³/s",
        correctAnswer = 0, // A
        explanation = "Der Ganges hat an seiner Mündung (gemeinsam mit dem Brahmaputra als Ganges-Brahmaputra-System) einen kombinierten Abfluss von ca. 38.000 m³/s. Der Ganges allein hat etwa 11.000–12.000 m³/s mittleren Abfluss. Das gemeinsame Delta ist das größte der Welt (Sundarbans).",
        difficulty = 4,
        funFact = "Das Ganges-Brahmaputra-Delta (Sundarbans) ist das größte Mangrovenwaldgebiet der Erde und Heimat des Bengal-Tigers. Jährlich lagert das System ca. 1 Milliarde Tonnen Sediment im Bengalischen Golf ab."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das geologisch älteste Gebirge Deutschlands?",
        answerA = "Schwarzwald (ca. 340 Mio. Jahre)",
        answerB = "Erzgebirge (ca. 380 Mio. Jahre)",
        answerC = "Harz (ca. 350 Mio. Jahre)",
        answerD = "Bayerischer Wald / Böhmerwald (ca. 500–600 Mio. Jahre)",
        correctAnswer = 3, // D
        explanation = "Der Bayerische Wald und der angrenzende Böhmerwald gehören zum Böhmischen Massiv, dessen kristallines Grundgebirge aus dem Proterozoikum stammt und ca. 500–600 Millionen Jahre alt ist. Das Erzgebirge (variszische Faltung, ca. 380 Mio. Jahre) ist jünger. Diese alten Granite und Gneise bilden eines der ältesten Gebirgsreste Mitteleuropas.",
        difficulty = 4,
        funFact = "Obwohl der Bayerische Wald geologisch uralt ist, sind seine heutigen Erhebungen (max. 1.456 m) bescheiden – nach Hunderten Millionen Jahren Erosion. Einst war er wohl himalayahoch."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen VEI (Vulkanexplosivitätsindex) hatte der Ausbruch des Tambora (1815) – der stärkste in der aufgezeichneten Geschichte?",
        answerA = "VEI 6",
        answerB = "VEI 8",
        answerC = "VEI 7",
        answerD = "VEI 9",
        correctAnswer = 2, // C
        explanation = "Der Tambora-Ausbruch auf Sumbawa (Indonesien) am 10./11. April 1815 erreichte VEI 7 (super-colossal) und schleuderte ca. 160 km³ Material aus. Er tötete direkt ca. 71.000 Menschen und verursachte das 'Jahr ohne Sommer' 1816, das weltweit zu Ernteausfällen und Hungersnöten führte.",
        difficulty = 4,
        funFact = "Das 'Jahr ohne Sommer' 1816 durch den Tambora-Ausschuss trieb Mary Shelley dazu, an einem regnerischen, kalten Sommer am Genfer See das Buch 'Frankenstein' zu schreiben – ein direktes kulturelles Erbe eines Vulkanausbruchs."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie hoch ist die Staumauer des Drei-Schluchten-Damms am Jangtse (China)?",
        answerA = "128 m",
        answerB = "185 m",
        answerC = "221 m",
        answerD = "163 m",
        correctAnswer = 1, // B
        explanation = "Die Staumauer des Drei-Schluchten-Damms ist 185 m hoch und 2.335 m lang. Mit einer installierten Leistung von ca. 22.500 MW ist er das größte Wasserkraftwerk der Welt nach installierter Leistung. Der Stausee streckt sich ca. 660 km entlang des Jangtse.",
        difficulty = 4,
        funFact = "Für den Drei-Schluchten-Stausee mussten ca. 1,3 Millionen Menschen umgesiedelt werden – die größte Zwangsumsiedlung durch ein Bauprojekt in der Geschichte. Über 1.300 archäologische Stätten wurden geflutet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land streitet mit Japan um die AWZ-Kontrolle der Senkaku/Diaoyu-Inseln im Ostchinesischen Meer?",
        answerA = "Nordkorea und China",
        answerB = "China und Taiwan (beide beanspruchen die Inseln)",
        answerC = "Südkorea und China",
        answerD = "China, Taiwan und Südkorea",
        correctAnswer = 1, // B
        explanation = "Sowohl China (Volksrepublik) als auch Taiwan (Republik China) beanspruchen die Senkaku/Diaoyu-Inseln, die von Japan verwaltet werden. Der Streit betrifft nicht nur die unbewohnten Inseln, sondern vor allem die dadurch entstehenden AWZ-Ansprüche im ressourcenreichen Ostchinesischen Meer.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gletscher ist der schnellste der Welt (höchste Fließgeschwindigkeit)?",
        answerA = "Jakobshavn-Gletscher (Grönland)",
        answerB = "Lambert-Gletscher (Antarktis)",
        answerC = "Hubbard-Gletscher (Alaska)",
        answerD = "Fedtschenko-Gletscher (Tadschikistan)",
        correctAnswer = 0, // A
        explanation = "Der Jakobshavn-Gletscher (Sermeq Kujalleq) in Westgrönland gilt als der schnellste Gletscher der Welt mit einer Fließgeschwindigkeit von über 40–46 m pro Tag (ca. 17 km pro Jahr). Er ist auch einer der Hauptverursacher des grönländischen Massenverlusts und eines der schnellsten Kälber für Eisberge.",
        difficulty = 4,
        funFact = "Der Eisberg, der 1912 die Titanic versenkte, stammete wahrscheinlich vom Jakobshavn-Gletscher. Die Reise eines solchen Eisbergs von Grönland in den Nordatlantik dauert typischerweise zwei bis drei Jahre."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchem Breitengrad liegt der nördlichste Punkt des afrikanischen Kontinents?",
        answerA = "38°47'N (Kap Blanc, Tunesien)",
        answerB = "37°21'N (Ras ben Sakka, Tunesien)",
        answerC = "35°55'N (Kap Serrat, Tunesien)",
        answerD = "40°04'N (Kap Bon, Tunesien)",
        correctAnswer = 1, // B
        explanation = "Ras ben Sakka (auch Ras Angela) in Tunesien bei 37°21'N ist der nördlichste Punkt des afrikanischen Festlandes. Er liegt auf der Halbinsel Cap Blanc, nur ca. 137 km von der sizilianischen Küste entfernt. Viele Quellen nennen fälschlicherweise Kap Blanc (Ras Enghela) als nördlichsten Punkt.",
        difficulty = 4,
        funFact = "An der engsten Stelle zwischen Afrika und Europa – zwischen Tunesien und Sizilien – liegt das Kap Bon Peninsula. Diese Route nutzten Millionen afrikanischer Migranten und historisch die Armeen Hannibals und der Vandalen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der drittgrößte Stausee der Welt nach Oberfläche (Wasserfläche)?",
        answerA = "Aswan-Stausee (Nassersee, Ägypten/Sudan)",
        answerB = "Manicouagan-Stausee (Kanada)",
        answerC = "Volta-Stausee (Ghana)",
        answerD = "Superior-See (Nordamerika – Naturgewässer, kein Stausee)",
        correctAnswer = 2, // C
        explanation = "Der Volta-Stausee (Lake Volta) in Ghana, entstanden durch den Akosombo-Damm (1965), hat eine Oberfläche von ca. 8.502 km² und ist der drittgrößte Stausee nach Fläche. Nach dem Nassersee (ca. 5.248 km²) und dem Kainji-Stausee folgt er in manchen Rankings noch vor dem Bratsk-Stausee.",
        difficulty = 4,
        funFact = "Der Volta-Stausee überflutete ca. 15 % der Landesfläche Ghanas und zwang über 78.000 Menschen zur Umsiedlung. Der Akosombo-Damm liefert noch heute den Großteil von Ghanas Strom."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Inseln westlich von Kiribati erlebten durch die Datumsgrenzverschiebung 1995 einen 'verlorenen Tag'?",
        answerA = "Die Line Islands und Phoenix Islands von Kiribati",
        answerB = "Tuvalu und Tokelau",
        answerC = "Amerikanisch-Samoa",
        answerD = "Jarvis Island und Howland Island",
        correctAnswer = 0, // A
        explanation = "Als Kiribati 1995 die Datumsgrenze östlich um seine Inselgruppen herumführte, sprangen die Line Islands (Caroline Island etc.) und Phoenix Islands über eine Datumsgrenze. Für die Bewohner dieser Inseln existierte ein bestimmter Tag praktisch nicht – sie wachten an einem Datum auf und gingen am übernächsten zu Bett.",
        difficulty = 4,
        funFact = "Kiribati erstreckt sich nun über fast alle 24 Zeitzonen und ist das Land mit der größten Ost-West-Ausdehnung in der Zeit (UTC+12 bis UTC+14 in den östlichsten Atolls). Caroline Island wurde in 'Millennium Island' umbenannt, um als erste den Jahrtausendwechsel 2000 zu begrüßen."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist der mittlere jährliche Abfluss des Mississippi an seiner Mündung in den Golf von Mexiko?",
        answerA = "ca. 5.100 m³/s",
        answerB = "ca. 16.800 m³/s",
        answerC = "ca. 9.800 m³/s",
        answerD = "ca. 24.500 m³/s",
        correctAnswer = 1, // B
        explanation = "Der Mississippi hat an seiner Mündung einen mittleren Abfluss von ca. 16.800 m³/s. Er ist der wasserreichste Fluss Nordamerikas und drainiert ca. 3,2 Millionen km² – etwa 41 % der kontinentalen USA. Im Vergleich dazu: der Amazonas hat ca. 209.000 m³/s.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land beansprucht die Exclusive Economic Zone (AWZ) rund um Rockall – einen einsamen Felsen im Nordatlantik?",
        answerA = "Irland allein",
        answerB = "Großbritannien, Irland, Dänemark (Färöer) und Island streiten alle darum",
        answerC = "Großbritannien (de facto Kontrolle) und Irland",
        answerD = "Nur Großbritannien unbestritten",
        correctAnswer = 1, // B
        explanation = "Rockall ist ein 21 m hoher Granitfels im Nordatlantik, auf den Großbritannien, Irland, Island und Dänemark (für die Färöer) AWZ-Ansprüche anmelden. Großbritannien annektierte Rockall 1955, aber die anderen Länder erkennen daraus resultierende AWZ-Ansprüche nicht an. Kern des Streits sind Fischereirechte und mögliche Erdöl-/Gasvorkommen.",
        difficulty = 4,
        funFact = "Rockall ist so klein und felsig, dass es nie dauerhaft bewohnt war. Greenpeace-Aktivisten 'besetzten' die Insel 1997, um gegen Ölbohrungen zu protestieren – eine der kuriosesten Besetzungen in der Geschichte territorialer Streitigkeiten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge Europas hat das geologisch jüngste Entstehungsalter?",
        answerA = "Alpen (ca. 34–5 Mio. Jahre)",
        answerB = "Karpaten (ca. 25 Mio. Jahre)",
        answerC = "Kaukasus (ca. 25–5 Mio. Jahre)",
        answerD = "Dinariden (ca. 70 Mio. Jahre)",
        correctAnswer = 0, // A
        explanation = "Die Alpen sind eines der geologisch jüngsten Hochgebirge Europas – ihre Haupthebungsphase fand vor etwa 34–5 Millionen Jahren statt (Eozän bis Pliozän). Teile des Himalaya sind ähnlich jung. Die heutigen Alpengipfel entstanden durch Kollision der Afrikanischen und Eurasischen Platte und kontinuierliche Erosion.",
        difficulty = 4,
        funFact = "Die Alpen steigen geologisch gesehen noch immer leicht an – die isostatische Hebung durch Erosionsentlastung beträgt etwa 1 mm pro Jahr, was durch den Klimawandel und erhöhte Schmelzwasserabfuhr sich verändert."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist die installierte Leistung des Itaipu-Staudamms zwischen Brasilien und Paraguay?",
        answerA = "8.370 MW",
        answerB = "14.000 MW",
        answerC = "22.500 MW",
        answerD = "6.960 MW",
        correctAnswer = 1, // B
        explanation = "Der Itaipu-Damm am Paraná zwischen Brasilien und Paraguay hat eine installierte Leistung von 14.000 MW (nach einer Erweiterung auf 14.000 MW, mit 20 Generatoren à 700 MW). Er war lange das größte Wasserkraftwerk der Welt nach Erzeugung, bis der Drei-Schluchten-Damm 2012 mehr produzierte.",
        difficulty = 4,
        funFact = "Itaipu deckt ca. 15 % des brasilianischen Strombedarfs und ca. 90 % des paraguayischen Stroms. Da Paraguay so wenig Strom verbraucht, verkauft es den Großteil seiner Quote an Brasilien – ein bedeutendes Einnahmemodell für das kleine Binnenland."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das am dichtesten besiedelte Stadtgebiet (Urban Agglomeration) Südamerikas (Stand 2024)?",
        answerA = "Buenos Aires",
        answerB = "Rio de Janeiro",
        answerC = "São Paulo",
        answerD = "Lima",
        correctAnswer = 2, // C
        explanation = "Die Greater São Paulo Area hat ca. 22–23 Millionen Einwohner und ist damit die größte Stadtregion Südamerikas. Buenos Aires folgt mit ca. 16 Millionen. São Paulo ist zugleich die größte Stadt der südlichen Hemisphäre und eines der wichtigsten Wirtschaftszentren der Welt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen VEI hatte der Ausbruch des Pinatubo auf den Philippinen (1991)?",
        answerA = "VEI 4",
        answerB = "VEI 6",
        answerC = "VEI 5",
        answerD = "VEI 7",
        correctAnswer = 1, // B
        explanation = "Der Pinatubo-Ausbruch vom 15. Juni 1991 erreichte VEI 6 und war der zweitstärkste vulkanische Eruption des 20. Jahrhunderts. Er schleuderte ca. 10 km³ Material aus, tötete ca. 800 Menschen direkt und senkte die globale Durchschnittstemperatur durch Schwefeldioxide um ca. 0,5 °C für zwei Jahre.",
        difficulty = 4,
        funFact = "Die NASA nutzte die Pinatubo-Eruption als 'natürliches Experiment', um Klimamodelle zu testen: Der durch den Vulkan verursachte Temperaturabfall bestätigte die Modellvorhersagen und stärkte das Vertrauen in Klimamodelle erheblich."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen GPS-Koordinaten liegt der geografische Mittelpunkt Australiens?",
        answerA = "25°36'S, 134°21'E (Lambert Center)",
        answerB = "23°42'S, 133°53'E (in der Nähe von Alice Springs)",
        answerC = "27°00'S, 133°00'E (in South Australia)",
        answerD = "20°15'S, 131°45'E (Northern Territory Mitte)",
        correctAnswer = 0, // A
        explanation = "Der Lambert Centre of Australia liegt bei 25°36'30\"S, 134°21'17\"E in der Nähe von Finke im Northern Territory – er wurde durch den australischen Vermessungsdienst Geoscience Australia berechnet. Ein Denkmal markiert diesen Mittelpunkt. Alice Springs (23°42'S) liegt etwas nördlicher.",
        difficulty = 4,
        funFact = "Der Lambert Centre liegt so abgelegen, dass es keine befestigte Straße dorthin gibt. Touristen, die den exakten Mittelpunkt Australiens besuchen wollen, brauchen ein 4WD-Fahrzeug und GPS-Navigation durch unmarkiertes Buschland."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Gletscher außerhalb der Polargebiete?",
        answerA = "Fedtschenko-Gletscher (Tadschikistan, 77 km)",
        answerB = "Siachen-Gletscher (Pakistan/Indien, 76 km)",
        answerC = "Baltoro-Gletscher (Pakistan, 62 km)",
        answerD = "Biafo-Gletscher (Pakistan, 63 km)",
        correctAnswer = 0, // A
        explanation = "Der Fedtschenko-Gletscher im Pamir-Gebirge (Tadschikistan) ist mit ca. 77 km Länge der längste Gletscher außerhalb der Polargebiete. Er liegt auf über 4.000 m Höhe und hat eine Fläche von ca. 700 km². Der Siachen-Gletscher (76 km, Karakorum) ist der zweitlängste und der höchstgelegene Kriegsschauplatz der Welt.",
        difficulty = 4,
        funFact = "Der Siachen-Gletscher ist seit 1984 Schauplatz eines militärischen Konflikts zwischen Indien und Pakistan um Kontrolle des Gletschers in über 6.000 m Höhe – der höchstgelegene militärische Konfliktort der Welt. Die extreme Kälte tötet mehr Soldaten als der Feind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher AWZ-Streit betrifft das Südchinesische Meer am direktesten zwischen den meisten Ländern gleichzeitig?",
        answerA = "China, Vietnam, Philippinen, Malaysia, Brunei und Taiwan",
        answerB = "China, Vietnam und Philippinen",
        answerC = "China, Indonesien, Malaysia und Singapur",
        answerD = "China und Vietnam allein (Paracel Islands)",
        correctAnswer = 0, // A
        explanation = "Das Südchinesische Meer ist Schauplatz der komplexesten AWZ-Streitigkeiten der Welt: China, Vietnam, die Philippinen, Malaysia, Brunei und Taiwan erheben überlappende Ansprüche auf die Spratly-Inseln und die umliegenden Gewässer. China beansprucht mit seiner 'Neun-Striche-Linie' ca. 90 % des Meeres.",
        difficulty = 4,
        funFact = "Der Internationale Schiedsgerichtshof (Haag) entschied 2016, dass Chinas 'Neun-Striche-Linie' keine rechtliche Grundlage im internationalen Seerecht hat. China erkannte das Urteil nicht an und setzt seine Gebietsbeanspruchungen fort."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie alt ist das Gebirge Appalachen (USA) nach seiner Hauptfaltungsphase in Millionen Jahren?",
        answerA = "ca. 65 Mio. Jahre (Kreidezeit)",
        answerB = "ca. 480–300 Mio. Jahre (Ordovizium–Karbon)",
        answerC = "ca. 150 Mio. Jahre (Jura)",
        answerD = "ca. 1.000 Mio. Jahre (Proterozoikum)",
        correctAnswer = 1, // B
        explanation = "Die Appalachen entstanden in drei Hauptphasen: der Taconic-Orogenese (ca. 480–440 Mio. Jahre), der Acadischen Orogenese (ca. 375–325 Mio. Jahre) und der Alleghenischen Orogenese (ca. 325–300 Mio. Jahre). Sie sind daher deutlich älter als die Alpen oder der Himalaya und entsprechen dem skandinavischen Kaledonischen Gebirge.",
        difficulty = 4,
        funFact = "Die Appalachen waren in ihrer Blütezeit wahrscheinlich so hoch wie die heutigen Alpen oder der Himalaya – über 400 Millionen Jahre Erosion haben sie auf ihre heutigen bescheidenen Höhen (max. 2.037 m, Mount Mitchell) reduziert."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viel Kubikmeter Wasser pro Sekunde hat der Iguazú-Wasserfall in der Regenzeit durchschnittlich?",
        answerA = "ca. 1.200 m³/s (Normalbetrieb)",
        answerB = "ca. 6.500 m³/s (Hochwasser) bis 12.870 m³/s (Rekord)",
        answerC = "ca. 350 m³/s (Jahresdurchschnitt)",
        answerD = "ca. 2.700 m³/s (mittlerer Jahresdurchschnitt)",
        correctAnswer = 3, // D
        explanation = "Der Iguazú hat einen mittleren Abfluss von ca. 1.500–2.700 m³/s, wobei 2.700 m³/s oft als repräsentativer Jahreswert gilt. Im Hochwasser kann er auf über 12.000 m³/s ansteigen; der Rekord lag bei ca. 12.870 m³/s (1992). Die Fälle sind 2,7 km breit und bestehen aus ca. 275 einzelnen Wasserfällen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt der magnetische Nordpol im Jahr 2024 ungefähr?",
        answerA = "ca. 86°N, 162°W (auf arktischem Ozean, Richtung Russland)",
        answerB = "ca. 80°N, 110°W (Nunavut, Kanada)",
        answerC = "ca. 90°N (fällt mit dem geografischen Nordpol zusammen)",
        answerD = "ca. 75°N, 100°W (Queen Elizabeth Islands)",
        correctAnswer = 0, // A
        explanation = "Der magnetische Nordpol wandert kontinuierlich. Er befand sich um 2024 bei ca. 86°N, 162°W – also weit nördlich, bereits über dem Arktischen Ozean in Richtung Sibirien. Anfang des 20. Jahrhunderts lag er noch über Kanada. Die Wanderungsgeschwindigkeit betrug zuletzt ca. 50–60 km pro Jahr.",
        difficulty = 4,
        funFact = "Der magnetische Nordpol wandert seit seiner ersten Messung 1831 über 2.300 km von der Küste Kanadas in Richtung Sibirien. In den letzten Jahrzehnten hat sich seine Wanderungsgeschwindigkeit stark erhöht – von ca. 10 km/Jahr auf über 50 km/Jahr."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Stadt (nach Stadtgebiet-Fläche) der Erde?",
        answerA = "Tokio",
        answerB = "New York",
        answerC = "Hulunbuir (China)",
        answerD = "Chongqing (China)",
        correctAnswer = 3, // D
        explanation = "Chongqing in Südwestchina gilt nach manchen Definitionen als die flächenmäßig größte Stadt der Welt mit über 82.000 km² Verwaltungsfläche – allerdings umfasst diese Fläche auch ländliche Bezirke und ist eher ein stadtähnlicher Verwaltungsraum. Das Stadtgebiet (urban area) von Chongqing ist deutlich kleiner; Tokio und New York haben größere urbane Zentren.",
        difficulty = 4,
        funFact = "Chongqing ist die einzige Großstadt Chinas ohne eigenständige Provinz – sie ist direkt der Zentralregierung unterstellt, wie Peking und Shanghai. Die Stadt wuchs durch den Drei-Schluchten-Damm stark, da viele Umgesiedelte hier angesiedelt wurden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der größte Gletscher Nordamerikas nach Fläche?",
        answerA = "Hubbard-Gletscher (Alaska)",
        answerB = "Bering-Gletscher (Alaska)",
        answerC = "Columbia-Gletscher (Alaska)",
        answerD = "Malaspina-Gletscher (Alaska)",
        correctAnswer = 1, // B
        explanation = "Der Bering-Gletscher in Alaska ist mit ca. 5.175 km² der größte Gletscher Nordamerikas und einer der größten temperate Gletscher der Welt. Er ist bekannt für periodische 'Surges' – plötzliche Beschleunigungsphasen, bei denen er sich deutlich vorwärtsbewegt. Der Malaspina-Gletscher (ca. 5.000 km²) liegt knapp dahinter.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vulkan hat den höchsten VEI-Wert in der aufgezeichneten Geschichte der letzten 10.000 Jahre (Holozän)?",
        answerA = "Toba (Indonesien, ca. 74.000 v. Chr.) – VEI 8",
        answerB = "Tambora 1815 – VEI 7",
        answerC = "Yellowstone (letzte Eruption ca. 640.000 v. Chr.) – VEI 8",
        answerD = "Pinatubo 1991 – VEI 6",
        correctAnswer = 1, // B
        explanation = "In der aufgezeichneten Geschichte (historische Quellen) ist Tambora 1815 mit VEI 7 der stärkste Ausbruch. Der Toba-Ausbruch (VEI 8) ist zwar geologisch dokumentiert, aber liegt ca. 74.000 Jahre zurück – weit außerhalb der historischen Aufzeichnungen. Gelológisch betrachtet ist Toba der stärkste bekannte Ausbruch der letzten 2 Millionen Jahre.",
        difficulty = 4,
        funFact = "Der Toba-Superausbruch vor ca. 74.000 Jahren war so massiv, dass er möglicherweise einen 'vulkanischen Winter' von mehreren Jahren auslöste und die menschliche Weltbevölkerung auf einige Tausend Individuen reduzierte – bekannt als die 'Toba-Katastrophentheorie', die aber umstritten ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen mittleren Abfluss hat der Jenissej (Sibirien) an seiner Mündung ins Karasee?",
        answerA = "ca. 9.000 m³/s",
        answerB = "ca. 19.600 m³/s",
        answerC = "ca. 29.000 m³/s",
        answerD = "ca. 36.500 m³/s",
        correctAnswer = 1, // B
        explanation = "Der Jenissej hat einen mittleren Abfluss von ca. 19.600 m³/s und ist damit einer der wasserreichsten Flüsse der Welt (Platz 5 weltweit). Zusammen mit seinem Quellfluss Selenga ist er ca. 5.539 km lang. Er ist der größte Flusses, der in den Arktischen Ozean mündet.",
        difficulty = 4,
        funFact = "Der Jenissej entwässert ein riesiges Gebiet, das von der Mongolei bis zum Arktischen Ozean reicht. Das Einzugsgebiet von ca. 2,58 Millionen km² enthält ausgedehnte Taiga und Permafrostböden, die bei Klimaerwärmung erhebliche Mengen Kohlenstoff freisetzen könnten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel liegt direkt auf der internationalen Datumsgrenze und wird dadurch in zwei verschiedene Tage gespalten?",
        answerA = "Diomedes-Inseln (Groß-Diomedes und Klein-Diomedes)",
        answerB = "Fanning Island (Kiribati)",
        answerC = "Wakeninsel (USA)",
        answerD = "Jarvis Island (USA)",
        correctAnswer = 0, // A
        explanation = "Die Diomedes-Inseln in der Beringstraße – Groß-Diomedes (Russland) und Klein-Diomedes (USA) – liegen nur 3,8 km voneinander entfernt, aber auf verschiedenen Seiten der internationalen Datumsgrenze. Bei Groß-Diomedes (Russland) ist es einen Tag später als bei Klein-Diomedes (USA) – sie werden manchmal 'Gestern-Insel' und 'Morgen-Insel' genannt.",
        difficulty = 4,
        funFact = "Während des Kalten Krieges war die Wasserstraße zwischen den Diomedes-Inseln eine der wenigen Stellen, an denen US-Staatsbürger und sowjetische Staatsbürger sich gegenüberstanden – sie wurde als 'Ice Curtain' (Eiserner Vorhang im Eis) bezeichnet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der Standort des höchsten dauerhaft bewohnten Ortes der Welt?",
        answerA = "La Rinconada (Peru), ca. 5.100 m",
        answerB = "Wenzhuan (Tibet), ca. 5.200 m",
        answerC = "El Alto (Bolivien), ca. 4.150 m",
        answerD = "Lhasa (Tibet), ca. 3.650 m",
        correctAnswer = 0, // A
        explanation = "La Rinconada in den peruanischen Anden liegt auf ca. 5.100 m über dem Meeresspiegel und ist die höchstgelegene dauerhaft bewohnte Siedlung der Welt. Die Goldminenstadt hat ca. 50.000–80.000 Einwohner und bietet extreme Lebensbedingungen durch Sauerstoffmangel und arktische Temperaturen.",
        difficulty = 4,
        funFact = "In La Rinconada funktioniert ein altes Lohnsystem: Arbeiter erhalten keinen regulären Lohn, sondern dürfen einmal im Monat so viel Erz mit nach Hause nehmen, wie sie tragen können – das 'Cachorreo'-System. Was sie ausschmilzen, gehört ihnen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie alt sind die Appalachian Mountains im Vergleich zu den Alpen (Entstehungsalter)?",
        answerA = "Gleich alt – beide entstanden vor ca. 60 Mio. Jahren",
        answerB = "Appalachen sind ca. 5–7 mal älter als die Alpen",
        answerC = "Alpen sind älter – sie entstanden vor den Appalachen",
        answerD = "Appalachen sind doppelt so alt wie die Alpen",
        correctAnswer = 1, // B
        explanation = "Die Appalachen entstanden hauptsächlich vor ca. 480–300 Millionen Jahren, die Alpen vor ca. 34–5 Millionen Jahren. Die Appalachen sind damit etwa 5–7 Mal älter. Dieser Unterschied erklärt, warum die Appalachen trotz ursprünglicher Himalaya-Höhe heute nur noch ca. 2.000 m hoch sind.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Unterwassertunnel der Welt (Bahntunnel unter Wasser)?",
        answerA = "Ärmelkanaltunnel (Eurotunnel), 50 km",
        answerB = "Seikan-Tunnel (Japan), 53,9 km",
        answerC = "Marmaray (Istanbul), 13,6 km",
        answerD = "Øresundbrücke-Tunnel (Dänemark-Schweden), 8 km",
        correctAnswer = 1, // B
        explanation = "Der Seikan-Tunnel in Japan verbindet die Inseln Honshū und Hokkaidō unter der Tsugaru-Straße und ist mit 53,9 km Gesamtlänge (davon 23,3 km unter Wasser) der längste Bahntunnel der Welt nach Gesamtlänge. Der Ärmelkanaltunnel (50,5 km, davon 38 km unter Wasser) hat den längsten Unterwasserabschnitt.",
        difficulty = 4,
        funFact = "Der Bau des Seikan-Tunnels (1971–1988) kostete das Leben von 34 Arbeitern und dauerte 17 Jahre. Die Tsugaru-Straße ist für ihre extremen Gezeiten und Strömungen bekannt – beim Bau kam es wiederholt zu Überschwemmungen durch eindringendes Meerwasser."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche zwei ozeanischen AWZ-Gebiete überlappen sich im Bereich des Arktischen Ozeans und sind Gegenstand aktueller Diplomatie?",
        answerA = "USA (Alaska) und Russland",
        answerB = "Kanada und Dänemark (Grönland)",
        answerC = "Russland, Kanada, Norwegen, Dänemark und USA (alle fünf arktischen Nationen)",
        answerD = "Nur Russland und Norwegen (Barentssee)",
        correctAnswer = 2, // C
        explanation = "Alle fünf arktischen Küstenstaaten – Russland, Kanada, Norwegen, Dänemark (via Grönland) und die USA – erheben AWZ-Ansprüche auf Teile des Arktischen Ozeans und seines Meeresbodens. Da viel Arktis über 200 Seemeilen von jeder Küste entfernt ist, sind erweiterte Kontinentalschelf-Ansprüche nach UNCLOS-Artikel 76 der eigentliche Streitpunkt.",
        difficulty = 4,
        funFact = "Russland hat einen U-Bootagenten 2007 eine Titanflagge am Nordpol verankern lassen – ein symbolischer Akt für seinen Anspruch auf den arktischen Schelf. Kanada und Dänemark protestierten. Heute reichen alle fünf Arktis-Nationen formale Ansprüche bei der UN-Kommission ein."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der größte Staudamm der Welt nach der Höhe (Dammhöhe)?",
        answerA = "Nurek-Damm (Tadschikistan), 300 m",
        answerB = "Drei-Schluchten-Damm (China), 185 m",
        answerC = "Rogun-Damm (Tadschikistan, im Bau), 335 m",
        answerD = "Grand Coulee (USA), 168 m",
        correctAnswer = 2, // C
        explanation = "Der Rogun-Damm in Tadschikistan (am Fluss Wachsh) ist nach seiner geplanten Fertigstellung mit 335 m Höhe der höchste Staudamm der Welt. Derzeit (2024) ist der Nurek-Damm (ebenfalls in Tadschikistan, 300 m) der höchste fertiggestellte Erdschüttdamm der Welt. Der Rogun-Damm befindet sich noch im Bau.",
        difficulty = 4,
        funFact = "Tadschikistan besitzt die zwei höchsten Staudämme der Welt – Nurek und Rogun – beide am gleichen Fluss Wachsh. Das kleine Binnenland in Zentralasien ist wasserkraftreich und hofft, durch den Rogun-Damm zu einem bedeutenden Stromexporteur zu werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt die Stadt mit dem weltweit höchsten Bevölkerungswachstum unter den Megastädten (Stand 2024)?",
        answerA = "Asien (Dhaka, Bangladesch)",
        answerB = "Afrika (Kinshasa, DR Kongo)",
        answerC = "Asien (Karatschi, Pakistan)",
        answerD = "Afrika (Lagos, Nigeria)",
        correctAnswer = 1, // B
        explanation = "Kinshasa in der Demokratischen Republik Kongo gehört laut UN-Prognosen zu den am schnellsten wachsenden Megastädten der Welt und könnte bis 2050 zu den drei bevölkerungsreichsten Städten gehören. Das Wachstum liegt bei über 4–5 % jährlich – ein Rekordwert unter Städten über 10 Millionen Einwohner.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Archipel liegt auf der Datumsgrenze und gehört daher teilweise zum 'heutigen' und teilweise zum 'gestrigen' Tag?",
        answerA = "Tonga-Inseln",
        answerB = "Fidschi-Inseln",
        answerC = "Chatham-Inseln (Neuseeland)",
        answerD = "Fiji und Tonga (die Datumsgrenze verläuft zwischen beiden)",
        correctAnswer = 3, // D
        explanation = "Die internationale Datumsgrenze verläuft zwischen den Fidschi-Inseln und Tonga – sie macht eine Ausbuchtung, um beide Inselgruppen auf der gleichen Seite zu halten. Historisch führte dies dazu, dass Fidschi und Tonga zeitlich 'einen Tag auseinander' lagen, obwohl sie eng benachbart sind. Beide liegen jetzt westlich der Linie.",
        difficulty = 4,
        funFact = "Tonga war das erste Land, das das Jahr 2000 feierte, weil es westlich der Datumsgrenze liegt. Samoa war damals noch östlich (und feierte als eines der letzten) – bevor es 2011 die Seiten wechselte."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viel Prozent des gesamten Süßwassers der Erde sind in Gletschern und Eisschilden gespeichert?",
        answerA = "ca. 20 %",
        answerB = "ca. 50 %",
        answerC = "ca. 69 %",
        answerD = "ca. 85 %",
        correctAnswer = 2, // C
        explanation = "Ca. 68–69 % des gesamten Süßwassers der Erde sind in Gletschern und Eisschilden (v.a. Antarktis und Grönland) gebunden. Grundwasser macht etwa 30 % aus. Seen, Flüsse und der Atmosphäre enthalten zusammen nur ca. 1 % des Süßwassers. Der globale Gletscherschwund verändert dieses Gleichgewicht.",
        difficulty = 4,
        funFact = "Würde das gesamte Eis der Antarktis schmelzen, würde der Meeresspiegel um ca. 58 Meter steigen – genug, um die meisten Küstenstädte der Welt zu überfluten. Allein das grönländische Eisschild würde den Meeresspiegel um ca. 7 Meter erhöhen."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen exakten Koordinaten liegt die Basis der Christusstatue (Cristo Redentor) in Rio de Janeiro?",
        answerA = "22°57'06\"S, 43°12'38\"W",
        answerB = "22°55'00\"S, 43°14'00\"W",
        answerC = "23°00'32\"S, 43°10'15\"W",
        answerD = "22°53'18\"S, 43°09'44\"W",
        correctAnswer = 0, // A
        explanation = "Die Christusstatue auf dem Corcovado-Berg in Rio de Janeiro steht bei 22°57'06\"S, 43°12'38\"W auf 710 m Höhe. Sie ist 38 m hoch (ohne Sockel) und öffnet ihre Arme 28 m weit. Die Statue wurde 1931 eingeweiht und ist eine der bekanntesten Landmarken der Welt.",
        difficulty = 4,
        funFact = "Der Corcovado ist ein 710 m hoher Granitfels, der aus dem Atlantischen Regenwald (Mata Atlântica) aufragt. Der Wald um den Berg ist Teil des Tijuca-Nationalparks – dem größten Stadtwald der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Stausee hat die größte Oberfläche (Fläche) in Afrika?",
        answerA = "Nassersee / Nubischer See (Ägypten/Sudan)",
        answerB = "Volta-Stausee (Ghana)",
        answerC = "Kariba-Stausee (Sambia/Simbabwe)",
        answerD = "Tanganjikasee (kein Stausee – Natursee)",
        correctAnswer = 1, // B
        explanation = "Der Volta-Stausee (Lake Volta) in Ghana mit ca. 8.502 km² ist der flächenmäßig größte künstliche Stausee Afrikas und einer der größten weltweit. Der Nassersee ist mit ca. 5.248 km² kleiner, aber hat ein erheblich größeres Volumen. Kariba hat die größte Kapazität nach Volumen (ca. 180 km³).",
        difficulty = 4,
        funFact = "Der Volta-Stausee entstand durch den Bau des Akosombo-Damms 1961–1965. Der erste Präsident Ghanas, Kwame Nkrumah, initiierte das Projekt mit Weltbank-Finanzierung, um Energie für die aufstrebende Aluminiumindustrie zu liefern."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge ist das geologisch jüngste auf dem Festland Australiens?",
        answerA = "Great Dividing Range (ca. 300 Mio. Jahre)",
        answerB = "Flinders Ranges (ca. 500 Mio. Jahre)",
        answerC = "MacDonnell Ranges (ca. 300 Mio. Jahre)",
        answerD = "Australian Alps (Snowy Mountains, ca. 90–80 Mio. Jahre)",
        correctAnswer = 3, // D
        explanation = "Die Snowy Mountains (Teil der Australian Alps) sind mit einer Haupthebungsphase vor ca. 90–80 Millionen Jahren (Kreide) die geologisch jüngsten Hochlagen Australiens. Die Great Dividing Range begann vor ca. 300 Millionen Jahren. Die Flinders und MacDonnell Ranges sind über 500 Millionen Jahre alt.",
        difficulty = 4,
        funFact = "Australiens höchster Gipfel, der Mount Kosciuszko (2.228 m), liegt in den Snowy Mountains. Trotz des bescheidenen Namens 'Snowy' lieferten diese Berge die Grundlage für das Snowy Mountains Scheme – eines der größten Wasserkraft- und Bewässerungsprojekte der südlichen Hemisphäre."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist der mittlere Abfluss des Orinoko an seiner Mündung (Venezuela)?",
        answerA = "ca. 3.200 m³/s",
        answerB = "ca. 30.000 m³/s",
        answerC = "ca. 10.500 m³/s",
        answerD = "ca. 16.000 m³/s",
        correctAnswer = 1, // B
        explanation = "Der Orinoko hat einen mittleren Abfluss von ca. 30.000 m³/s an seiner Mündung im Atlantik – er ist der drittgrößte Fluss Südamerikas nach dem Amazonas und dem Paraná/Río de la Plata-System. Er entwässert ca. 880.000 km² und ist für Venezuela das wichtigste Süßwassersystem.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Ranking liegt Mumbai (Bombay) unter den weltgrößten Stadtregionen nach Einwohnern (Stand 2024)?",
        answerA = "Rang 3–4",
        answerB = "Rang 7–8",
        answerC = "Rang 5–6",
        answerD = "Rang 10–12",
        correctAnswer = 2, // C
        explanation = "Die Großregion Mumbai liegt nach UN-Schätzungen 2024 auf ca. Rang 5–6 weltweit mit ca. 21–22 Millionen Einwohnern (Urban Agglomeration). Vor Mumbai liegen Tokio, Delhi, Shanghai und São Paulo/Dhaka. Die Rangliste schwankt je nach Berechnungsmethode, aber Mumbai gehört stabil zur Top-10.",
        difficulty = 4,
        funFact = "Mumbai ist die wirtschaftlich bedeutendste Stadt Indiens – sie erwirtschaftet ca. 5 % des indischen BIP und ist Sitz der Börse, Bollywood und der größten Konzerne. Gleichzeitig hat die Stadt mit Dharavi eines der größten Slums Asiens."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches VEI hatte der Ausbruch des Hunga Tonga-Hunga Ha'apai 2022, der einen Tsunamie ausgelöst hat?",
        answerA = "VEI 4",
        answerB = "VEI 6",
        answerC = "VEI 5",
        answerD = "VEI 7",
        correctAnswer = 2, // C
        explanation = "Der Hunga Tonga-Hunga Ha'apai-Ausbruch vom 15. Januar 2022 erreichte vorläufige Schätzungen von VEI 5–5,8. Er war der stärkste Vulkanausbruch seit dem Pinatubo 1991 und erzeugte einen Tsunami sowie eine globale Druckwelle, die mehrfach um die Erde wanderte. Satelliten registrierten eine Aschewolke bis ca. 58 km Höhe.",
        difficulty = 4,
        funFact = "Der Hunga Tonga-Ausbruch katapultierte Wasser in die Stratosphäre – ca. 150 Millionen Tonnen Wasserdampf, mehr als jeder andere bekannte Vulkanausbruch. Dies erhöhte den Wasserdampfgehalt der Stratosphäre um ca. 10 % und könnte kurzfristig den Klimaeffekt beeinflussen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land betreibt den Aswan-Hochdamm und wie viel Strom produziert er jährlich?",
        answerA = "Ägypten; ca. 10–12 Milliarden kWh/Jahr",
        answerB = "Ägypten und Sudan gemeinsam; ca. 25 Milliarden kWh/Jahr",
        answerC = "Ägypten allein; ca. 50 Milliarden kWh/Jahr",
        answerD = "Ägypten; ca. 2,6 Milliarden kWh/Jahr",
        correctAnswer = 0, // A
        explanation = "Der Aswan-Hochdamm (Sadd el-Aali) in Ägypten erzeugt mit 12 Turbinen eine installierte Leistung von ca. 2.100 MW und produziert ca. 10–12 Milliarden kWh pro Jahr. Er deckte in den 1970er Jahren über 50 % des ägyptischen Stroms, heute (bei stark gewachsenem Bedarf) noch ca. 10–15 %.",
        difficulty = 4,
        funFact = "Beim Bau des Aswan-Hochdamms (1960–1970) mussten ca. 100.000 nubische Menschen umgesiedelt werden, und über 20 antike ägyptische Tempel wurden abgetragen und an sichere Standorte versetzt – darunter der berühmte Tempel von Abu Simbel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Stadt hat nach absoluter Einwohnerzahl des Stadtgebiets den Rang als größte Stadt in Ozeanien (inkl. Australien)?",
        answerA = "Auckland",
        answerB = "Brisbane",
        answerC = "Sydney",
        answerD = "Melbourne",
        correctAnswer = 2, // C
        explanation = "Sydney ist mit ca. 5,3 Millionen Einwohnern (Greater Sydney, 2024) die einwohnerstärkste Stadt Australiens und Ozeaniens. Melbourne (ca. 5,1 Mio.) liegt knapp dahinter. Beide Städte wechseln in den Rankings je nach Berechnungsmethode die Plätze, mit Sydney geringfügig vorn.",
        difficulty = 4,
        funFact = "Trotz seiner Dominanz als größte Stadt ist Sydney nicht die Hauptstadt Australiens – das ist Canberra. Canberra wurde bewusst als Kompromisshauptstadt zwischen Sydney und Melbourne gewählt, nachdem beide Städte im Wettbewerb um den Hauptstadtstatus lagen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie dick ist das antarktische Eisschild an seiner dicksten bekannten Stelle (Astrolabe-Subglaziales Becken)?",
        answerA = "ca. 3.000 m",
        answerB = "ca. 4.776 m",
        answerC = "ca. 2.500 m",
        answerD = "ca. 6.200 m",
        correctAnswer = 1, // B
        explanation = "Das antarktische Eisschild erreicht an seiner dicksten Stelle im Bereich der subglazialen Becken Dicken von bis zu 4.776 m. Diese Messung stammt aus Radar-Tiefenmessungen im Bereich des Astrolabe-Subglazialen Beckens/Wilkes-Becken. Die durchschnittliche Dicke des antarktischen Eisschildes beträgt ca. 2.160 m.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die weltweit größte ausschließliche Wirtschaftszone pro km² Landfläche (prozentual)?",
        answerA = "Kiribati",
        answerB = "Nauru",
        answerC = "Palau",
        answerD = "Marshallinseln",
        correctAnswer = 0, // A
        explanation = "Kiribati hat eine Landfläche von nur ca. 811 km², aber eine AWZ von ca. 3,44 Millionen km² – das ergibt ein AWZ-zu-Land-Verhältnis von über 4.000:1. Kein anderes Land hat eine so dramatisch überproportionale Meereszone im Verhältnis zu seiner Landfläche. Die AWZ enthält wichtige Fischereiressourcen.",
        difficulty = 4,
        funFact = "Kiribati ist durch den Klimawandel existenziell bedroht – der Meeresspiegel steigt schneller als die Inseln sich erholen können. Kiribati hat bereits Teile von Fidschi gekauft, um seiner Bevölkerung eine Umsiedlungsoption zu geben – der erste Staat, der 'klimatisches Territorium' im Ausland erwarb."
    ),

    // ── EXPERT BATCH 4 (difficulty = 4) ── 50 additional questions ───────────
    // Topics: island populations, UN geographic features, hydrographic basins,
    // continental shelf claims, volcanic eruption dates, geyser fields,
    // submarine canyons, remote island coordinates, permafrost thickness, ocean salinity gradients

    Question(
        categoryId = 1,
        questionText = "Wie viele ständige Einwohner hat Tristan da Cunha, die entlegenste bewohnte Insel der Welt?",
        answerA = "ca. 50",
        answerB = "ca. 250",
        answerC = "ca. 800",
        answerD = "ca. 1.500",
        correctAnswer = 1, // B
        explanation = "Tristan da Cunha im südlichen Atlantik hat ca. 250 ständige Einwohner, die alle auf die gleichen sieben Familien zurückgehen, die die Insel im frühen 19. Jahrhundert besiedelten. Die Insel liegt ca. 2.800 km von der nächsten Küste (St. Helena) entfernt und ist nur per Schiff erreichbar.",
        difficulty = 4,
        funFact = "Alle 250 Bewohner von Tristan da Cunha tragen einen von nur acht Familiennamen. Wegen der Isolation kämpft die Bevölkerung mit bestimmten Erbkrankheiten – Wissenschaftler untersuchen das genetische Profil der Gemeinschaft intensiv."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen genauen Koordinaten liegt die Insel Bouvet (Bouvetøya), die entlegenste Insel Norwegens?",
        answerA = "51°24'S, 3°21'E",
        answerB = "54°26'S, 3°24'E",
        answerC = "48°38'S, 1°57'E",
        answerD = "56°18'S, 5°42'E",
        correctAnswer = 1, // B
        explanation = "Bouvetøya liegt bei 54°26'S, 3°24'E im Südatlantik und ist norwegisches Territorium. Die unbewohnte Vulkaninsel ist die entlegenste Insel der Welt – der nächste bewohnte Ort (Tristan da Cunha) liegt ca. 1.600 km entfernt. Sie ist zu 93 % von Gletschern bedeckt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief ist die Permafrostschicht im jakutischen Tiefland (Ostsibirien) an ihrer tiefsten gemessenen Stelle?",
        answerA = "ca. 400 m",
        answerB = "ca. 1.500 m",
        answerC = "ca. 700 m",
        answerD = "ca. 250 m",
        correctAnswer = 1, // B
        explanation = "Im jakutischen Becken (Sibirien) reicht der Permafrost bis in Tiefen von ca. 1.500 m – das ist die weltweit tiefste gemessene Permafrostschicht. Im Durchschnitt ist die sibirische Permafrostschicht ca. 300–600 m tief. Diese Tiefe erklärt sich durch jahrtausendelange arktische Kälteeinstrahlung ohne Unterbrechung durch Vereisung.",
        difficulty = 4,
        funFact = "In den tiefsten Schichten des jakutischen Permafrostes wurden Mammut-Reste gefunden, die so gut erhalten waren, dass Wissenschaftler noch Fleisch und Blut entnehmen konnten – ein natürlicher Tiefkühlschrank seit 30.000 Jahren."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann genau (Datum) begann die explosive Phase des Eyjafjallajökull, die 2010 den europäischen Luftraum lahmlegte?",
        answerA = "20. März 2010",
        answerB = "14. April 2010",
        answerC = "2. April 2010",
        answerD = "28. März 2010",
        correctAnswer = 1, // B
        explanation = "Die explosive eruptive Phase des Eyjafjallajökull, die den europäischen Luftraum lahmlegte, begann am 14. April 2010 nach einem ersten Ausbruch am 20. März. Die Aschewolke vom 14. April führte zwischen dem 15. und 21. April 2010 zur größten Luftraumsperrung in Europa seit dem Zweiten Weltkrieg.",
        difficulty = 4,
        funFact = "Trotz der dramatischen Auswirkungen auf den Luftverkehr gab es beim Eyjafjallajökull 2010 keine direkten Todesopfer durch die Eruption. Der Vulkan trägt seinen komplizierten Namen nach dem isländischen Gletscher, der seinen Gipfel bedeckt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Geyserfeld gilt als das aktivste der Welt mit über 500 aktiven Geysiren?",
        answerA = "Yellowstone-Nationalpark (USA)",
        answerB = "Kamtschatka-Halbinsel (Russland, Tal der Geysire)",
        answerC = "El Tatio (Chile)",
        answerD = "Wai-O-Tapu (Neuseeland)",
        correctAnswer = 0, // A
        explanation = "Der Yellowstone-Nationalpark in Wyoming (USA) beherbergt über 500 aktive Geysire – mehr als die Hälfte aller Geysire der Welt. Das bekannteste ist Old Faithful, das im Abstand von 60–110 Minuten ausbricht. Das gesamte Yellowstone-Gebiet liegt über einem gigantischen Magmareservoir (Supervolkano).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Unterwassercanyon gilt als der längste der Nordatlantikküste Nordamerikas?",
        answerA = "Georges-Bank-Canyon",
        answerB = "Hudson-Canyon",
        answerC = "Laurentian-Canyon",
        answerD = "Baltimore-Canyon",
        correctAnswer = 1, // B
        explanation = "Der Hudson-Canyon ist mit einer Länge von über 750 km (vom Hudson-Fluss bis in die Tiefsee) einer der längsten Unterwassercanyons der Welt. Er beginnt am Rand des kontinentalen Schelfs vor New York und reicht bis in Tiefen von über 3.500 m. Er ist das direkte Unterwasser-Fortführungssystem des Hudson-Flusses.",
        difficulty = 4,
        funFact = "Der Hudson-Canyon entstand, als der Meeresspiegel während der letzten Eiszeit um ca. 120 m absank und der Hudson-Fluss seinen Lauf über den jetzt überfluteten Schelf bis an die Tiefseekante fortsetzte."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt der Pol der Unzugänglichkeit des eurasischen Kontinents (Dsunggarei, China)?",
        answerA = "ca. 46°17'N, 86°40'E",
        answerB = "ca. 40°00'N, 65°00'E",
        answerC = "ca. 55°24'N, 84°00'E",
        answerD = "ca. 50°00'N, 92°00'E",
        correctAnswer = 0, // A
        explanation = "Der eurasische Pol der Unzugänglichkeit liegt bei ca. 46°17'N, 86°40'E in der Dsunggarei (Xinjiang, China), ca. 2.645 km von der nächsten Küste entfernt. Er ist der am weitesten von jedem Ozean entfernte Punkt auf dem eurasischen Festland.",
        difficulty = 4,
        funFact = "Der Eurasische Pol der Unzugänglichkeit zeigt extreme Kontinentalklimaschwankungen von über 70 °C zwischen Sommer (+40°C) und Winter (−30°C). Niederschläge sind minimal – oft unter 200 mm/Jahr."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie ist der Salinitätsgradient zwischen Mittelmeer und Atlantik an der Straße von Gibraltar?",
        answerA = "Mittelmeer ca. 38 ‰, Atlantik ca. 35 ‰ (Mittelmeer salziger)",
        answerB = "Atlantik salziger als Mittelmeer (ca. 40 ‰ vs. 36 ‰)",
        answerC = "Beide haben identischen Salzgehalt von ca. 35 ‰",
        answerD = "Mittelmeer ca. 30 ‰, Atlantik ca. 35 ‰ (Atlantik salziger)",
        correctAnswer = 0, // A
        explanation = "Das Mittelmeer hat durch starke Verdunstung und geringen Süßwasserzufluss einen Salzgehalt von ca. 38–39 ‰, während der angrenzende Atlantik ca. 35–36 ‰ aufweist. An der Straße von Gibraltar entsteht eine zweischichtige Strömung: Atlantikwasser fließt oben ein, dichtes Mittelmeerwasser fließt tief unten heraus.",
        difficulty = 4,
        funFact = "Das ausströmende Mittelmeerwasser (Mediterranean Outflow Water) bleibt als separate Wassermasse im Atlantik erkennbar und beeinflusst die Tiefenzirkulation des gesamten Nordatlantiks bis in Tiefen von 1.000 m."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat mehrfach erweiterte Kontinentalschelf-Ansprüche nach UNCLOS Artikel 76 eingereicht, die über 1 Million km² arktischen Meeresbodens umfassen?",
        answerA = "Australien",
        answerB = "Russland",
        answerC = "Brasilien",
        answerD = "Neuseeland",
        correctAnswer = 1, // B
        explanation = "Russland hat mehrfach erweiterte Kontinentalschelf-Ansprüche bei der CLCS eingereicht – der umfassendste umfasst über 1,2 Millionen km² des Arktischen Meeresbodens. Russland beansprucht damit, dass der Lomonosov-Rücken eine Verlängerung des russischen Kontinentalsockels ist.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt Heard Island (Australien) mit dem aktiven Vulkan Big Ben im südlichen Indischen Ozean?",
        answerA = "53°06'S, 73°31'E",
        answerB = "49°31'S, 69°30'E",
        answerC = "60°12'S, 78°45'E",
        answerD = "44°55'S, 68°22'E",
        correctAnswer = 0, // A
        explanation = "Heard Island liegt bei ca. 53°06'S, 73°31'E im südlichen Indischen Ozean. Der aktive Vulkan Big Ben (Mawson Peak, 2.745 m) ist der höchste Punkt australischen Territoriums. Die Insel ist UNESCO-Weltnaturerbe und nahezu unberührt von menschlichem Einfluss.",
        difficulty = 4,
        funFact = "Heard Island ist so abgelegen, dass es nahezu unberührt von menschlichem Einfluss ist – trotzdem haben Satelliten mehrfach aktive Lavaströme dokumentiert. Rund 80 % der Insel sind von Gletschern bedeckt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das hydrografische Einzugsgebiet des Ob-Irtysch-Systems (Sibirien)?",
        answerA = "ca. 1,1 Millionen km²",
        answerB = "ca. 2,99 Millionen km²",
        answerC = "ca. 1,78 Millionen km²",
        answerD = "ca. 4,5 Millionen km²",
        correctAnswer = 1, // B
        explanation = "Das Ob-Irtysch-Einzugsgebiet umfasst ca. 2,99 Millionen km² und ist damit eines der größten der Welt (Platz 5). Es erstreckt sich von den kasachischen Steppen bis zum Arktischen Ozean und entwässert das westsibirische Tiefland. Der Ob ist ca. 3.650 km lang, das Gesamtsystem mit dem Irtysch ca. 5.410 km.",
        difficulty = 4,
        funFact = "Das Ob-Einzugsgebiet enthält das Vasyugan-Moor – das größte Torfmoor der Welt (ca. 53.000 km²). Es speichert mehr Kohlenstoff als ganz Deutschland und ist ein kritischer Klimapuffer, der durch Erdöl-Erschließung und Klimawandel bedroht ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele Einwohner hat Saint Helena (britisches Überseegebiet) im Südatlantik (Stand 2024)?",
        answerA = "ca. 500",
        answerB = "ca. 4.500",
        answerC = "ca. 12.000",
        answerD = "ca. 25.000",
        correctAnswer = 1, // B
        explanation = "Saint Helena hat ca. 4.500 ständige Einwohner (Stand 2021/2024). Die Insel im südlichen Atlantik ist vor allem als Verbannungsort Napoleon Bonapartes (1815–1821) bekannt. Seit 2016 gibt es einen Flughafen, der die Insel nach Jahrzehnten ausschließlicher Schiffsverbindung öffnete.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Geyserfeld in Island ist Heimat des Namengebers aller Geysire weltweit?",
        answerA = "Geysir-Geothermalgebiet (Haukadalur)",
        answerB = "Hengill-Vulkansystem",
        answerC = "Krafla-Geothermalfeld",
        answerD = "Nesjavellir-Geothermalgebiet",
        correctAnswer = 0, // A
        explanation = "Das Haukadalur-Geothermalgebiet in Südisland ist Heimat des 'Geysir' – dem Namensgeber aller Geysire weltweit. Das Feld hat mehrere aktive Geysire, darunter Strokkur (bricht alle 5–10 Minuten aus) und den historischen Great Geysir (heute seltener aktiv).",
        difficulty = 4,
        funFact = "Das englische Wort 'geyser' kommt direkt vom isländischen Geysir, der wiederum vom altnordischen 'gjósa' (hervorbrechen, sprudeln) abstammt. Der große Geysir war zu seiner Blütezeit (1890er Jahre) der berühmteste Geysir der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Unterwassercanyon an der afrikanischen Westküste wird direkt durch den Kongoflusseintrag erodiert?",
        answerA = "Kongo-Canyon (Congo Canyon)",
        answerB = "Niger-Canyon",
        answerC = "Senegal-Canyon",
        answerD = "Volta-Canyon",
        correctAnswer = 0, // A
        explanation = "Der Kongo-Canyon vor der Mündung des Kongo-Flusses ist einer der größten und tiefsten Unterwassercanyons der Welt. Er beginnt direkt an der Flussmündung, erstreckt sich ca. 750 km ins Tiefsee-Abyssal und reicht bis in Tiefen von über 3.000 m. Er wird aktiv durch die enorme Sedimentlast des Kongo-Flusses erodiert.",
        difficulty = 4,
        funFact = "Der Kongo-Canyon wird noch heute aktiv durch turbide Strömungen erodiert – die gewaltige Wasserführung des Kongo treibt selbst in der Tiefsee noch Erosionsprozesse an. Wissenschaftler nutzen Unterwasserkabel in diesem Canyon zur seismischen Messung."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie dick ist der Permafrost im Bereich von Jakutsk (Sacha/Jakutien) durchschnittlich?",
        answerA = "ca. 100–200 m",
        answerB = "ca. 300–500 m",
        answerC = "ca. 1.000–1.500 m",
        answerD = "ca. 50–80 m",
        correctAnswer = 1, // B
        explanation = "Im Bereich von Jakutsk (die kälteste Großstadt der Welt) beträgt die Permafrosttiefe durchschnittlich ca. 300–500 m. Jakutsk liegt vollständig auf Permafrostboden – alle Gebäude müssen auf Pfählen errichtet werden, da die Wärme der Häuser den Boden auftauen würde.",
        difficulty = 4,
        funFact = "In Jakutsk werden im Winter regulär Temperaturen von −40 bis −50 °C gemessen. Das gesamte Stadtleben (Rohrsysteme, Fundamente, Straßen) muss an den dauerhaft gefrorenen Untergrund angepasst sein – ein einzigartiges ingenieurtechnisches Umfeld."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt Ascension Island (britisches Überseegebiet) im Südatlantik?",
        answerA = "7°56'S, 14°22'W",
        answerB = "15°56'S, 5°44'W",
        answerC = "1°30'S, 9°30'E",
        answerD = "11°32'N, 8°20'W",
        correctAnswer = 0, // A
        explanation = "Ascension Island liegt bei 7°56'S, 14°22'W im Südatlantik, ca. 1.600 km nordwestlich von Saint Helena. Die Insel ist strategisch wichtig (britische Militärbasis, NASA-Tracking-Station) und hat ca. 600–800 Einwohner, überwiegend Militär und Verwaltung.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches UN-Koordinatensystem hat seit 1984 den historischen Greenwicher Nullmeridian als globalen Standard ersetzt?",
        answerA = "ED50 (European Datum 1950)",
        answerB = "IERS-Referenzmeridian (WGS84, ca. 102 m östlich von Greenwich)",
        answerC = "Greenwicher Nullmeridian (0° historisches Observatorium)",
        answerD = "GRS80 (Geodetic Reference System 1980)",
        correctAnswer = 1, // B
        explanation = "Seit 1984 definiert das IERS den offiziellen Nullmeridian beim IERS-Referenzmeridian (WGS84), der ca. 102 m östlich des historischen Greenwicher Nullmeridians liegt. GPS-Geräte und alle modernen Koordinatensysteme nutzen diesen Standard. Wer in Greenwich auf der historischen Linie steht, befindet sich laut GPS leicht östlich des modernen Nullmeridians.",
        difficulty = 4,
        funFact = "Der Unterschied von ca. 102 m entsteht, weil das historische Greenwich-Observatorium die lokale Vertikale nutzte (beeinflusst durch lokale Schwerkraftanomalien), während WGS84 ein erdzentriertes Ellipsoidmodell verwendet."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das Einzugsgebiet des Amazonas nach Fläche (das größte der Welt)?",
        answerA = "ca. 4,8 Millionen km²",
        answerB = "ca. 7,05 Millionen km²",
        answerC = "ca. 5,9 Millionen km²",
        answerD = "ca. 3,2 Millionen km²",
        correctAnswer = 1, // B
        explanation = "Das Amazonas-Einzugsgebiet umfasst ca. 7,05 Millionen km² – das größte Flusseinzugsgebiet der Welt. Es erstreckt sich über neun südamerikanische Länder und macht ca. 40 % der Fläche Südamerikas aus. Das Einzugsgebiet entspricht ca. 20 % des globalen Süßwasserabflusses.",
        difficulty = 4,
        funFact = "Das Amazonas-Einzugsgebiet beherbergt ca. 10 % aller auf der Erde lebenden Arten. Ein einziger Hektar Amazonas-Regenwald kann mehr Baumarten enthalten als ganz Europa. Der Regenwald erzeugt durch Transpiration seinen eigenen Wasserkreislauf – 'fliegende Flüsse' aus Wasserdampf."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann genau fand die Hauptexplosion des Pinatubo (Philippinen) 1991 statt?",
        answerA = "3. Juni 1991",
        answerB = "15. Juni 1991",
        answerC = "2. April 1991",
        answerD = "28. Mai 1991",
        correctAnswer = 1, // B
        explanation = "Die Hauptexplosion des Pinatubo fand am 15. Juni 1991 statt – der stärkste Ausbruch seit dem Novarupta 1912. Vorläuferausbrüche begannen am 9. und 12. Juni. Der Hauptausbruch am 15. Juni produzierte eine Aschesäule von ca. 34 km Höhe und schleuderte ca. 10 km³ Material aus.",
        difficulty = 4,
        funFact = "Der Pinatubo-Ausbruch traf zeitlich mit dem Taifun Yunya zusammen – die Kombination aus Vulkanasche und Regen erzeugte schwere Lahars (Schlammlawinen), die mehr Todesopfer forderten als die Eruption selbst. Das US-amerikanische Clark Air Base wurde dauerhaft aufgegeben."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele Einwohner hat das Cocos-(Keeling-)Inseln-Territorium (Australien) im Indischen Ozean?",
        answerA = "ca. 100",
        answerB = "ca. 600",
        answerC = "ca. 3.500",
        answerD = "ca. 15.000",
        correctAnswer = 1, // B
        explanation = "Die Cocos (Keeling) Islands haben ca. 550–600 ständige Einwohner auf zwei bewohnten Inseln (Home Island und West Island). Die Bevölkerung setzt sich aus malaiisch-stämmigen Cocos-Malaiern und australischen Beamten zusammen. Das Territorium liegt ca. 2.750 km nordwestlich von Perth.",
        difficulty = 4,
        funFact = "Die Cocos (Keeling) Islands wurden von Charles Darwin 1836 während der Beagle-Reise besucht – er entwickelte hier seine Theorie zur Entstehung von Atollen durch das langsame Absinken vulkanischer Inseln."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Salzgehalt hat das Tote Meer (Israel/Jordanien) an seiner Oberfläche?",
        answerA = "ca. 15 ‰",
        answerB = "ca. 33 ‰",
        answerC = "ca. 340 ‰ (ca. 34 %)",
        answerD = "ca. 120 ‰ (ca. 12 %)",
        correctAnswer = 2, // C
        explanation = "Das Tote Meer hat einen Salzgehalt von ca. 330–342 ‰ (ca. 34 %), was etwa zehnmal höher ist als der durchschnittliche Meeressalzgehalt (35 ‰). Dieser extrem hohe Salzgehalt macht das Baden ohne Ertrinken möglich (erhöhter Auftrieb) und verhindert jegliches tierische Leben.",
        difficulty = 4,
        funFact = "Das Tote Meer sinkt jährlich um ca. 1 Meter ab – durch massive Wasserentnahme aus dem Jordan für Bewässerung und Trinkwasser sowie hohe Verdunstung. Seit 1930 hat es bereits ca. ein Drittel seiner Fläche verloren."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt Christmas Island (Weihnachtsinsel, Australien) im Indischen Ozean?",
        answerA = "10°29'S, 105°38'E",
        answerB = "14°15'S, 89°12'E",
        answerC = "8°04'S, 115°55'E",
        answerD = "17°22'S, 102°18'E",
        correctAnswer = 0, // A
        explanation = "Christmas Island liegt bei 10°29'S, 105°38'E im Indischen Ozean, ca. 360 km südlich von Java (Indonesien). Die australische Überseegebiet hat ca. 1.500–2.000 Einwohner und ist vor allem für die jährliche Roten Krabben-Wanderung bekannt (ca. 120 Millionen Krabben ziehen vom Inneren zur Küste).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Geyserfeld auf Kamtschatka (Russland) wurde 2007 durch eine Schlammlawine teilweise zerstört?",
        answerA = "Uzon-Caldera-Geothermalfeld",
        answerB = "Tal der Geysire (Dolina Geizerov)",
        answerC = "Mutnovsky-Geothermalanlage",
        answerD = "Pauzhetka-Geothermalfeld",
        correctAnswer = 1, // B
        explanation = "Das 'Tal der Geysire' (Dolina Geizerov) auf Kamtschatka mit über 200 Geysiren wurde am 3. Juni 2007 durch einen massiven Erdrutsch mit Schlammlawine teilweise verschüttet. Etwa zwei Drittel des Tals wurden begraben. Das Tal ist UNESCO-Weltnaturerbe.",
        difficulty = 4,
        funFact = "Kamtschatkas Tal der Geysire ist nur per Hubschrauber zugänglich. Nach dem Erdrutsch 2007 regenerierte sich das Tal überraschend schnell – viele Geysire wurden freigelegt oder öffneten neue Kanäle."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das Einzugsgebiet des Kongo-Flusses?",
        answerA = "ca. 1,2 Millionen km²",
        answerB = "ca. 3,69 Millionen km²",
        answerC = "ca. 2,1 Millionen km²",
        answerD = "ca. 4,8 Millionen km²",
        correctAnswer = 1, // B
        explanation = "Das Einzugsgebiet des Kongo umfasst ca. 3,69 Millionen km² – das zweitgrößte Einzugsgebiet Afrikas und das fünftgrößte der Welt. Es erstreckt sich über neun Länder und umfasst das größte tropische Regenwaldgebiet Afrikas (Kongobecken). Der Fluss überquert zweimal den Äquator.",
        difficulty = 4,
        funFact = "Das Kongobecken beherbergt ca. 70 % aller afrikanischen tropischen Regenwälder und ist nach dem Amazonasbecken das zweitwichtigste Kohlenstoffsenke der Erde."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann fand die Hauptexplosion des Krakatau statt, die 1883 den drittlautesten Knall der aufgezeichneten Geschichte erzeugte?",
        answerA = "27. August 1883",
        answerB = "15. September 1883",
        answerC = "3. Juli 1883",
        answerD = "12. November 1883",
        correctAnswer = 0, // A
        explanation = "Der Haupt-Paroxysmus des Krakatau-Ausbruchs fand am 27. August 1883 statt. Die stärkste Explosion um 10:02 Uhr Ortszeit wurde 4.800 km entfernt auf Rodrigues (Mauritius) als 'Kanonendonner' beschrieben. Die Druckwelle umrundete die Erde mehrfach.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der größte aktive Geysir der Welt nach Ausbruchshöhe?",
        answerA = "Old Faithful (Yellowstone, ca. 55 m)",
        answerB = "Steamboat Geyser (Yellowstone, bis 90 m)",
        answerC = "Grand Geysir (Island, ca. 70 m historisch)",
        answerD = "Strokkur (Island, ca. 30 m)",
        correctAnswer = 1, // B
        explanation = "Der Steamboat Geyser im Yellowstone-Nationalpark ist der größte aktive Geysir der Welt – bei Hauptausbrüchen kann er Wasser bis zu 90–120 m hoch schleudern. Er bricht jedoch unregelmäßig aus. Old Faithful (ca. 55 m) ist regelmäßiger, aber kleiner.",
        difficulty = 4,
        funFact = "Der Steamboat Geyser hatte zwischen 1911 und 1961 eine 50-jährige Ruhephase. Nach jahrelanger Inaktivität begann er 2018 ungewöhnlich häufig auszubrechen – Wissenschaftler diskutieren, ob dies ein Zeichen erhöhter geothermaler Aktivität unter Yellowstone ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher asiatische Salzsee hat nach dem Don Juan Pond einen der höchsten Salzgehalte weltweit?",
        answerA = "Assal-See (Dschibuti)",
        answerB = "Totes Meer (Israel/Jordanien)",
        answerC = "Garabogazköl (Turkmenistan)",
        answerD = "Tuz Gölü (Türkei)",
        correctAnswer = 2, // C
        explanation = "Der Garabogazköl an der Westküste des Kaspischen Meeres in Turkmenistan hat einen Salzgehalt von bis zu 350 ‰ (ca. 35 %) in Trockenzeiten – er gilt nach dem Don Juan Pond als eines der salzhaltigsten natürlichen Gewässer Asiens. Das Kaspische Meer selbst hat nur ca. 13 ‰.",
        difficulty = 4,
        funFact = "Der Garabogazköl war von 1980–1992 durch einen Damm vom Kaspischen Meer getrennt und trocknete fast vollständig aus. Nach Öffnung des Dammes füllte er sich wieder. Er ist eine wichtige Quelle für Natriumsulfat (Glaubersalz) für die Chemieindustrie."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Flusssystem hat das größte Einzugsgebiet in Asien?",
        answerA = "Ob-Irtysch (ca. 3 Mio. km²)",
        answerB = "Jangtse (ca. 1,8 Mio. km²)",
        answerC = "Jenissej-Angara (ca. 2,58 Mio. km²)",
        answerD = "Lena (ca. 2,49 Mio. km²)",
        correctAnswer = 0, // A
        explanation = "Das Ob-Irtysch-System hat das größte Einzugsgebiet in Asien mit ca. 2,99 Millionen km². Es übertrifft das Jenissej-Angara-System (ca. 2,58 Mio. km²) und das Lena-System (ca. 2,49 Mio. km²). Alle drei großen sibirischen Flusssysteme enden im Arktischen Ozean.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Salinitätsunterschied hat der Persische Golf gegenüber dem Arabischen Meer?",
        answerA = "Persischer Golf ca. 40 ‰, Arabisches Meer ca. 36 ‰ (Differenz ca. 4 ‰)",
        answerB = "Persischer Golf ca. 55 ‰, Arabisches Meer ca. 35 ‰ (Differenz 20 ‰)",
        answerC = "Beide haben identischen Salzgehalt von ca. 38 ‰",
        answerD = "Persischer Golf ca. 70 ‰, Arabisches Meer ca. 40 ‰ (Differenz 30 ‰)",
        correctAnswer = 0, // A
        explanation = "Der Persische Golf hat durch extreme Verdunstung einen Salzgehalt von ca. 38–42 ‰ (teilweise bis 45 ‰ in flachen Bereichen), das Arabische Meer liegt bei ca. 36–37 ‰. Der Gradient von ca. 4–5 ‰ erzeugt an der Straße von Hormuz eine dichte Ausströmung von Golfwasser in den Ozean.",
        difficulty = 4,
        funFact = "Das aus dem Persischen Golf ausströmende hypersaline Wasser taucht im Arabischen Meer ab und bildet eine eigene Wassermasse (Persian Gulf Water), die über Tausende von Kilometern durch den Indischen Ozean verfolgt werden kann."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher ist der größte Unterwassercanyon im Pazifischen Ozean nach Länge?",
        answerA = "Monterey-Canyon (Kalifornien, ca. 153 km)",
        answerB = "Bering-Canyon (Beringmeer, ca. 400 km)",
        answerC = "Cascadia-Channel (Oregon/Washington)",
        answerD = "Suruga-Canyon (Japan)",
        correctAnswer = 1, // B
        explanation = "Der Bering-Canyon im Beringmeer (Alaska) ist mit einer Länge von ca. 400 km und Tiefen bis 4.000 m einer der größten Unterwassercanyons des Pazifiks. Der Monterey Canyon vor Kalifornien (ca. 153 km) ist tiefer als der Grand Canyon relativ zur umliegenden Fläche, aber kürzer.",
        difficulty = 4,
        funFact = "Der Monterey Canyon vor der Küste Kaliforniens ist so zugänglich, dass er eines der meistuntersuchten Unterwasserökosysteme der Welt ist. Das Monterey Bay Aquarium Research Institute (MBARI) betreibt dort seit Jahrzehnten Unterwasserroboter."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt Henderson Island (UNESCO-Weltnaturerbe, britisches Pitcairn-Territorium)?",
        answerA = "24°22'S, 128°19'W",
        answerB = "31°12'S, 141°45'W",
        answerC = "18°48'S, 151°20'W",
        answerD = "27°54'S, 135°08'W",
        correctAnswer = 0, // A
        explanation = "Henderson Island liegt bei 24°22'S, 128°19'W im südlichen Pazifik und gehört zum britischen Pitcairn-Territorium. Die unbewohnte Insel ist UNESCO-Weltnaturerbe (seit 1988) wegen ihrer einzigartigen Fauna (4 endemische Vogelarten). Sie liegt ca. 5.000 km von Neuseeland und Südamerika entfernt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wann begann der stärkste Vulkanausbruch des 20. Jahrhunderts – der Novarupta in Alaska?",
        answerA = "1.–9. Juni 1912",
        answerB = "August 1900",
        answerC = "15. März 1923",
        answerD = "22. Oktober 1930",
        correctAnswer = 0, // A
        explanation = "Der Novarupta-Katmai-Ausbruch fand vom 1.–9. Juni 1912 auf der Alaska-Halbinsel statt und war der stärkste Vulkanausbruch des 20. Jahrhunderts mit VEI 6. Er schleuderte ca. 12,5 km³ Material aus und schuf das 'Tal der 10.000 Rauche' (Valley of Ten Thousand Smokes).",
        difficulty = 4,
        funFact = "Obwohl der Novarupta-Ausbruch stärker war als der Pinatubo 1991 und der Krakatau 1883, ist er wenig bekannt – weil er in einer völlig unbewohnten Region Alaskas auftrat. Keine Großstadt lag in der Nähe, und die Nachrichtenkommunikation war 1912 noch sehr beschränkt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das Einzugsgebiet des Mississippi (USA) und welchen Weltrang hat es?",
        answerA = "ca. 2,1 Mio. km², Rang 8",
        answerB = "ca. 3,2 Mio. km², Rang 4",
        answerC = "ca. 1,2 Mio. km², Rang 12",
        answerD = "ca. 4,5 Mio. km², Rang 3",
        correctAnswer = 1, // B
        explanation = "Das Mississippi-Einzugsgebiet umfasst ca. 3,2 Millionen km² – das viertgrößte der Welt. Es entwässert ca. 41 % der kontinentalen USA, von den Rocky Mountains bis zu den Appalachen, und umfasst 31 Bundesstaaten.",
        difficulty = 4,
        funFact = "Das Mississippi-Delta ist aktiv wachsend, sinkt aber durch Bodenverdichtung und Sedimentmangel (durch Dämme) schneller als es wächst. New Orleans liegt bereits unter dem Meeresspiegel und ist von Deichen abhängig."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie dick ist der grönländische Eisschild an seiner dicksten Stelle im Zentralbereich?",
        answerA = "ca. 1.500 m",
        answerB = "ca. 3.400 m",
        answerC = "ca. 2.100 m",
        answerD = "ca. 4.800 m",
        correctAnswer = 1, // B
        explanation = "Der grönländische Eisschild erreicht im Zentralbereich Dicken von ca. 3.400–3.500 m. Die durchschnittliche Eisdicke beträgt ca. 1.790 m. Unter dem Gewicht des Eises liegt ein Großteil des Grundgesteins unter dem Meeresspiegel.",
        difficulty = 4,
        funFact = "Die Schmelzrate des grönländischen Eisschildes hat sich seit den 1990er Jahren verdreifacht. Zwischen 1992 und 2020 verlor Grönland ca. 4.890 Gigatonnen Eis – genug, um den globalen Meeresspiegel um ca. 13,5 mm zu erhöhen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches UN-Verfahren regelt die Beantragung von erweitertem Festlandsockel über die 200-Seemeilen-AWZ hinaus?",
        answerA = "Bathymetrische Zonierung nach IOC/UNESCO",
        answerB = "Artikel-76-Verfahren der UN-Kommission für die Grenzen des Festlandsockels (CLCS)",
        answerC = "ICES-Schelfkartierung (International Council for the Exploration of the Sea)",
        answerD = "IMO-Schiffahrtszonenrecht",
        correctAnswer = 1, // B
        explanation = "Artikel 76 des UN-Seerechtsübereinkommens (UNCLOS, 1982) regelt, wie Küstenstaaten ihren Festlandsockel über die 200-Seemeilen-AWZ hinaus beanspruchen können. Die UN-Kommission für die Grenzen des Festlandsockels (CLCS) prüft wissenschaftliche Belege. Bis 2024 haben über 80 Länder Anträge eingereicht.",
        difficulty = 4,
        funFact = "Der Artikel-76-Prozess ist einer der kompliziertesten Aspekte des Seerechts: Länder müssen detaillierte bathymetrische Messungen, seismische Profile und Sedimentdaten vorlegen, um zu beweisen, dass ein Unterwassergebiet eine 'natürliche Verlängerung' ihres Landes ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie ist der Salinitätsgradient zwischen dem Arktischen Ozean und dem Nordatlantik?",
        answerA = "Arktis ca. 32 ‰, Nordatlantik ca. 35 ‰ (Arktis salzärmer)",
        answerB = "Arktis ca. 38 ‰, Nordatlantik ca. 35 ‰ (Arktis salzreicher)",
        answerC = "Arktis ca. 25 ‰, Nordatlantik ca. 35 ‰ (Arktis deutlich salzärmer)",
        answerD = "Kein wesentlicher Unterschied (beide ca. 35 ‰)",
        correctAnswer = 0, // A
        explanation = "Der Arktische Ozean hat durch starken Süßwasserzufluss (sibirische und nordamerikanische Flüsse) und Schmelzwasser einen Oberflächensalzgehalt von ca. 28–32 ‰ – deutlich niedriger als der Atlantik (ca. 35 ‰). Diese Süßwasserschicht wirkt als Isolationsschicht für den Wärmeaustausch.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Atoll hat die höchste Bevölkerungsdichte unter den Inselgruppen der Marshallinseln?",
        answerA = "Funafuti-Atoll (Tuvalu), ca. 3.000 Einwohner/km²",
        answerB = "Tarawa-Atoll (Kiribati), ca. 4.000 Einwohner/km²",
        answerC = "Majuro (Marshallinseln), ca. 2.500 Einwohner/km²",
        answerD = "Ebeye (Kwajalein-Atoll, Marshallinseln), ca. 40.000 Einwohner/km²",
        correctAnswer = 3, // D
        explanation = "Ebeye im Kwajalein-Atoll (Marshallinseln) hat mit ca. 15.000 Menschen auf 0,36 km² eine Bevölkerungsdichte von über 40.000 Einwohnern/km² – eine der höchsten der Welt. Funafuti (Tuvalu) liegt bei ca. 2.000 Einwohner/km², Tarawa Südatoll (Kiribati) bei ca. 3.000–4.000.",
        difficulty = 4,
        funFact = "Ebeye ist die 'Schattenseite' der Marshall-Inseln: Während das nahe Kwajalein-Atoll von der US-Armee als Raketen-Testgelände genutzt wird, leben tausende Marshallesen auf dem winzigen Ebeye in extremer Dichte – das Produkt historischer Umsiedlungen durch US-Atomtests."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Salzgehalt hat das Schwarze Meer an der Oberfläche im Vergleich zum Mittelmeer?",
        answerA = "Schwarzes Meer ca. 17–18 ‰, Mittelmeer ca. 38 ‰",
        answerB = "Beide identisch bei ca. 35 ‰",
        answerC = "Schwarzes Meer ca. 30 ‰, Mittelmeer ca. 38 ‰",
        answerD = "Schwarzes Meer ca. 40 ‰ (salziger als Mittelmeer)",
        correctAnswer = 0, // A
        explanation = "Das Schwarze Meer hat durch starken Süßwasserzufluss (Donau, Dnjepr, Don) einen Oberflächensalzgehalt von nur ca. 17–18 ‰ – weit unter dem Mittelmeer (ca. 38 ‰). An der Straße von Bosporus entsteht eine zweischichtige Strömung: salzarmes Schwarzmeerwasser fließt oben aus, salzhaltiges Mittelmeerwasser strömt unten ein.",
        difficulty = 4,
        funFact = "Unterhalb von ca. 150 m Tiefe ist das Schwarze Meer komplett sauerstofffrei (anoxisch). Dies bewahrt gesunkene Schiffe jahrhundertelang nahezu unversehrt – Archäologen haben dort Holzschiffe aus der Antike in perfektem Erhaltungszustand entdeckt."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt Jan Mayen (Norwegen) mit dem nördlichsten aktiven Vulkan der Erde?",
        answerA = "71°00'N, 8°00'W",
        answerB = "69°30'N, 1°00'W",
        answerC = "64°00'N, 4°30'W",
        answerD = "75°15'N, 20°00'W",
        correctAnswer = 0, // A
        explanation = "Jan Mayen liegt bei ca. 71°00'N, 8°00'W im Nordatlantik/Arktischen Ozean, ca. 1.000 km westlich von Norwegen. Die Insel ist Heimat des Beerenberg (2.277 m), dem nördlichsten aktiven Vulkan der Erde. Nur eine kleine norwegische Militär- und Wetterstation besteht dort.",
        difficulty = 4,
        funFact = "Der Beerenberg auf Jan Mayen brach zuletzt 1985 aus. Die Insel liegt an der Grenze zwischen Europäischer und Amerikanischer Platte, genau auf dem Jan-Mayen-Rücken – einem mittelozeanischen Rücken."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das hydrografische Einzugsgebiet des Nils?",
        answerA = "ca. 1,9 Millionen km²",
        answerB = "ca. 3,4 Millionen km²",
        answerC = "ca. 2,6 Millionen km²",
        answerD = "ca. 4,2 Millionen km²",
        correctAnswer = 1, // B
        explanation = "Das Nil-Einzugsgebiet umfasst ca. 3,4 Millionen km² und erstreckt sich über elf Länder, von Ruanda und Burundi bis zur Nilmündung in Ägypten. Der Nil selbst ist ca. 6.650 km lang und hat an seiner Mündung nur ca. 2.800 m³/s mittleren Abfluss – stark reduziert durch den Aswan-Damm.",
        difficulty = 4,
        funFact = "Obwohl der Nil das bekannteste Flusssystem Afrikas ist, hat er weit weniger Abfluss als der Kongo (ca. 41.000 m³/s). Der Nil ist vor allem durch seine extreme Länge und historische Bedeutung für die Menschheitsgeschichte weltberühmt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Unterwassercanyon an der portugiesischen Küste erzeugt die größten surfbaren Wellen der Welt?",
        answerA = "Tejo-Canyon",
        answerB = "Nazaré-Canyon",
        answerC = "Douro-Canyon",
        answerD = "Setúbal-Canyon",
        correctAnswer = 1, // B
        explanation = "Der Nazaré-Canyon vor der portugiesischen Küste ist einer der tiefsten Unterwassercanyons Europas – ca. 170 km lang und bis 5.000 m tief. Er erzeugt durch seine Geometrie außergewöhnlich große Wellen vor dem Ort Nazaré. Wellenreiter-Weltrekorde mit Wellen über 30 m wurden hier aufgestellt.",
        difficulty = 4,
        funFact = "Der Nazaré-Canyon wirkt wie ein Wellenamplifizator: Atlantikswells laufen in den Canyon ein, werden gebündelt und an der Oberfläche zu Riesenwellen verstärkt. 2020 surften Wellenreiter Wellen von über 30 m Höhe – ein Weltrekord."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele ständige Einwohner hat Niue (Pazifik, assoziiert mit Neuseeland) auf der Insel?",
        answerA = "ca. 200",
        answerB = "ca. 1.500",
        answerC = "ca. 8.000",
        answerD = "ca. 20.000",
        correctAnswer = 1, // B
        explanation = "Niue hat ca. 1.500–1.600 ständige Einwohner auf der Insel – nach einem Bevölkerungshöchststand von ca. 5.000 in den 1960ern hat die Abwanderung nach Neuseeland die Bevölkerung stark reduziert. Ca. 30.000 Niueans leben in Neuseeland mit neuseeländischer Staatsbürgerschaft.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Geyserfeld in Neuseeland hat die meisten aktiven Geysire des Landes, darunter den größten der südlichen Hemisphäre?",
        answerA = "Rotorua-Geothermalgebiet (allgemein)",
        answerB = "Wai-O-Tapu",
        answerC = "Whakarewarewa (Te Pūia)",
        answerD = "Orakei Korako",
        correctAnswer = 2, // C
        explanation = "Whakarewarewa (Te Pūia) bei Rotorua hat ca. 65 aktive Geysire – darunter Pōhutu, den größten aktiven Geysir der südlichen Hemisphäre (bricht bis 30 m hoch aus). Das Gebiet liegt auf der Taupo Volcanic Zone, einer der heißesten Geothermalzonen der Erde.",
        difficulty = 4,
        funFact = "Pōhutu in Whakarewarewa kann bis zu 20 Mal pro Tag ausbrechen und bis 30 m Höhe erreichen. Er ist verlässlicher als viele andere Geysire der Welt und deshalb eine Hauptattraktion Neuseelands."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welchen Koordinaten liegt Kerguelen (Desolation Islands, Frankreich) im südlichen Indischen Ozean?",
        answerA = "49°21'S, 70°12'E",
        answerB = "54°18'S, 68°42'E",
        answerC = "46°15'S, 72°30'E",
        answerD = "57°40'S, 61°55'E",
        correctAnswer = 0, // A
        explanation = "Kerguelen liegt bei ca. 49°21'S, 70°12'E im südlichen Indischen Ozean, ca. 3.400 km von der nächsten Küste entfernt. Die Inselgruppe gehört zum französischen Überseegebiet TAAF und hat eine permanente Forschungsstation mit ca. 45–110 Personen (saisonal).",
        difficulty = 4,
        funFact = "Kerguelen wird auch 'Desolation Islands' genannt – vom britischen Entdecker Cook 1776 so beschrieben wegen seiner extremen Kargheit. Die endemische Pflanze 'Kerguelen-Kohl' (Pringlea antiscorbutica) half Seeleuten gegen Skorbut."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Salinitätsgradienten hat die Ostsee von der Nordseeverbindung (Kattegat) bis zum nördlichsten Bottnischen Meerbusen?",
        answerA = "Kattegat ca. 25 ‰, Bottnischer Meerbusen ca. 3 ‰ (starker Gradient)",
        answerB = "Kattegat ca. 15 ‰, Bottnischer Meerbusen ca. 30 ‰ (Norden salzreicher)",
        answerC = "Homogener Salzgehalt von ca. 10 ‰ in der gesamten Ostsee",
        answerD = "Kattegat ca. 35 ‰, Bottnischer Meerbusen ca. 8 ‰ (Differenz 27 ‰)",
        correctAnswer = 0, // A
        explanation = "In der Ostsee existiert ein ausgeprägter Salinitätsgradient: Im Kattegat ca. 20–25 ‰, in der zentralen Ostsee ca. 8–10 ‰ und im nördlichsten Bottnischen Meerbusen nur ca. 2–3 ‰. Dieser Gradient entsteht durch starken Süßwasserzufluss (ca. 500 km³/Jahr) und den begrenzten Wasseraustausch mit der Nordsee.",
        difficulty = 4,
        funFact = "Durch den geringen Salzgehalt der Ostsee gefriert sie bei wesentlich höheren Temperaturen als normale Meeresgebiete. Im Winter ist der nördliche Bottnische Meerbusen regelmäßig mehrere Monate zugefroren – Schiffe benötigen Eisbrecher für die Navigation."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie dick ist der Permafrost in der Hudson-Bay-Tiefebene (Kanada) durchschnittlich?",
        answerA = "ca. 20–50 m",
        answerB = "ca. 300–600 m",
        answerC = "ca. 100–200 m",
        answerD = "ca. 5–10 m (nur dünner saisonaler Permafrost)",
        correctAnswer = 2, // C
        explanation = "In der Hudson-Bay-Tiefebene Kanadas ist der Permafrost ca. 100–200 m tief – im nördlicheren Kanada (Baffin Island, Banks Island) kann er 400–600 m erreichen. Die Hudson-Bay-Region liegt im Bereich des diskontinuierlichen Permafrosts, der nicht überall vorhanden ist und langsam auftaut.",
        difficulty = 4,
        funFact = "Die Hudson-Bay-Tiefebene enthält eines der größten Moorflächen Kanadas – ca. 320.000 km² Torfmoor (Muskeg). Der Permafrost hält enorme Mengen organischen Kohlenstoffs gespeichert, der bei Auftauen als CO₂ und Methan freigesetzt wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das hydrografische Einzugsgebiet des Niger-Flusses (Westafrika)?",
        answerA = "ca. 600.000 km²",
        answerB = "ca. 2,09 Millionen km²",
        answerC = "ca. 1,1 Millionen km²",
        answerD = "ca. 3,8 Millionen km²",
        correctAnswer = 1, // B
        explanation = "Das Niger-Einzugsgebiet umfasst ca. 2,09 Millionen km² und ist damit das drittgrößte in Afrika (nach dem Kongo mit 3,69 Mio. km² und dem Nil mit 3,4 Mio. km²). Es erstreckt sich von Guinea über Mali, Niger, Benin bis Nigeria. Der Niger ist ca. 4.184 km lang.",
        difficulty = 4,
        funFact = "Das Niger-Binnendelta bei Timbuktu ist ein einzigartiges Binnenfeuchtgebiet im Übergang zur Sahara – während der Regenzeit überflutet der Niger ein Gebiet von bis zu 40.000 km² und beherbergt Hunderttausende von Wasservögeln."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann begann die Holuhraun-Spaltenrupption des Bárðarbunga-Vulkansystems in Island?",
        answerA = "29. August 2014",
        answerB = "3. März 2010",
        answerC = "15. September 2018",
        answerD = "2. Juli 2021",
        correctAnswer = 0, // A
        explanation = "Die Holuhraun-Spaltenrupption begann am 29. August 2014 nach einem Erdbebenschwarm unter dem Bárðarbunga. Die Eruption dauerte bis März 2015 und war die größte Lavaausbruch in Island seit 200 Jahren (ca. 1,6 km³ Lava). Sie produzierte erhebliche SO₂-Emissionen, die über Europa nachgewiesen wurden.",
        difficulty = 4,
        funFact = "Die Holuhraun-Eruption 2014–2015 erzeugte ein neues Lavafeld von ca. 85 km² auf dem isländischen Hochland – größer als Manhattan. Die SO₂-Emissionen waren an manchen Tagen so hoch, dass die Luftqualität in weiten Teilen Islands als gefährlich eingestuft wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der größte Unterwasser-Sedimentfächer des Indischen Ozeans?",
        answerA = "Indus-Canyon (Arabisches Meer)",
        answerB = "Bengal-Fächer (Bengalischer Golf)",
        answerC = "Mozambique-Canyon",
        answerD = "Sunda-Canyon (Indonesien)",
        correctAnswer = 1, // B
        explanation = "Der Bengal-Fächer (Bengal Fan) im Bengalischen Golf ist das größte Unterwasser-Sedimentfächersystem der Welt mit einer Fläche von ca. 3 Millionen km² und einem Sedimentvolumen von über 12 Millionen km³ – aufgebaut durch den Himalaya-Erosionstransport via Ganges-Brahmaputra.",
        difficulty = 4,
        funFact = "Der Bengal-Fächer reicht bis zum Südteil des Indischen Ozeans und hat eine Länge von ca. 2.800 km. Wissenschaftler können aus seinen Schichten die vollständige Hebungsgeschichte des Himalayas über die letzten 50 Millionen Jahre rekonstruieren."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das Einzugsgebiet des Mekong (Südostasien)?",
        answerA = "ca. 250.000 km²",
        answerB = "ca. 795.000 km²",
        answerC = "ca. 1,4 Millionen km²",
        answerD = "ca. 450.000 km²",
        correctAnswer = 1, // B
        explanation = "Das Mekong-Einzugsgebiet umfasst ca. 795.000 km² und erstreckt sich über sechs Länder (China, Myanmar, Laos, Thailand, Kambodscha, Vietnam). Der Fluss ist ca. 4.909 km lang und entwässert vom tibetischen Hochplateau durch den regenreichen Indochina-Gürtel.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele ständige Einwohner hat Svalbard (Spitzbergen, Norwegen) im Arktischen Ozean?",
        answerA = "ca. 150",
        answerB = "ca. 2.600",
        answerC = "ca. 8.000",
        answerD = "ca. 500",
        correctAnswer = 1, // B
        explanation = "Svalbard hat ca. 2.400–2.700 ständige Einwohner, davon ca. 2.000–2.200 in der Hauptsiedlung Longyearbyen und ca. 400–500 in der russischen Siedlung Barentsburg. Der Svalbard-Vertrag (1920) erlaubt Bürgern aller Signatarstaaten die Niederlassung.",
        difficulty = 4,
        funFact = "Auf Svalbard ist das Sterben regulatorisch eingeschränkt: Schwerkranke und Sterbende werden auf das norwegische Festland ausgeflogen, da der Permafrostboden Leichen nicht verwesen lässt. Svalbard hat keinen aktiven Friedhof für Neubeerdigungen seit Jahrzehnten."
    ),

    // ── EXPERT BATCH 5 (difficulty = 4) ── 50 additional questions ───────────
    // Topics: magnetic declination, soil liquefaction, subsidence rates, karst aquifers,
    // snowline altitudes, paleo-lake systems, loess deposits, river meander wavelengths,
    // periglacial landforms, cryosphere data

    Question(
        categoryId = 1,
        questionText = "Welchen Wert der magnetischen Deklination hat London (Greenwich) aktuell (ca. 2024)?",
        answerA = "ca. −5,5° (westliche Deklination)",
        answerB = "ca. +2,0° (östliche Deklination)",
        answerC = "0° (kein Unterschied)",
        answerD = "ca. −12° (starke westliche Deklination)",
        correctAnswer = 1, // B
        explanation = "In London beträgt die magnetische Deklination ca. 0° bis +1,5° (schwach östlich, Stand 2024) – sie ist dort nach Jahrhunderten westlicher Deklination in den letzten Jahrzehnten auf nahezu Null zurückgegangen und wechselt langsam in den östlichen Bereich. Der Magnetpol wandert Richtung Sibirien und zieht die Deklinationslinien mit.",
        difficulty = 4,
        funFact = "Im Jahr 1800 betrug die magnetische Deklination in London noch ca. −24° (westlich). Dieser dramatische Rückgang über 200 Jahre zeigt, wie dynamisch das Erdmagnetfeld ist – Schifffahrtsnavigation musste ständig neu kalibriert werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Wert der magnetischen Deklination wurde 2024 in Tokio gemessen?",
        answerA = "ca. +7° (östliche Deklination)",
        answerB = "ca. −3° (westliche Deklination)",
        answerC = "ca. +15° (starke östliche Deklination)",
        answerD = "ca. −10° (westliche Deklination)",
        correctAnswer = 0, // A
        explanation = "In Tokio beträgt die magnetische Deklination ca. +7°–8° (östlich, 2024). Das bedeutet, dass ein Kompass dort etwa 7° östlich vom geografischen Norden zeigt. Im westlichen Pazifik ist die östliche Deklination typisch für diesen Bereich der Erde.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Gebiet Neuseelands trat beim Erdbeben von Christchurch (2011) die stärkste Bodenverflüssigung auf?",
        answerA = "Westufer des Lake Ellesmere",
        answerB = "Stadtbezirke Bexley, Avonside und Richmond entlang des Avon-Flusses",
        answerC = "Gebirgszone der Southern Alps",
        answerD = "Küstengebiet von Banks Peninsula",
        correctAnswer = 1, // B
        explanation = "Beim Christchurch-Erdbeben (22. Februar 2011, M 6,3) trat die schwerste Bodenverflüssigung in den flussnahen Wohngebieten entlang des Avon-Flusses auf – besonders in Bexley, Avonside und Richmond. Die wassergesättigten Alluvialsande verloren ihre Festigkeit, tausende Häuser mussten abgerissen werden. Die gesamte 'Red Zone' wurde stillgelegt.",
        difficulty = 4,
        funFact = "Nach dem Christchurch-Erdbeben wurde ein ca. 600 Hektar großes Stadtgebiet ('Red Zone') dauerhaft aufgegeben und in einen Grünkorridor umgewandelt. Das Ereignis ist das wichtigste Lehrbeispiel weltweit für städtische Bodenverflüssigung in modernen Zeiten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche japanische Stadt verzeichnet jährliche Bodensubsidenz von über 10 mm/Jahr und liegt dadurch teilweise unter dem Meeresspiegel?",
        answerA = "Tokio (Tokyo)",
        answerB = "Osaka",
        answerC = "Niigata",
        answerD = "Nagoya",
        correctAnswer = 0, // A
        explanation = "Tokio hat im 20. Jahrhundert durch exzessive Grundwasserentnahme eine kumulative Subsidenz von bis zu 4–5 Metern erfahren. Teile des Kōtō-Viertels liegen heute über 2 m unter dem Meeresspiegel. Seit Regulierung der Grundwasserentnahme in den 1960er Jahren hat sich die Absenkungsrate auf unter 1 mm/Jahr verringert.",
        difficulty = 4,
        funFact = "In Tokio wurden in den 1950er–60er Jahren Gebiete mit über 20 cm/Jahr Subsidenz gemessen – eine industriell verursachte Absinkkrise. Heute schützt ein Netz aus unterirdischen Speicherbecken und Deichen die tief liegenden Stadtteile."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Küstenstadt versinkt weltweit am schnellsten durch anthropogene Subsidenz (Stand 2024)?",
        answerA = "Jakarta (Indonesien), teils >25 cm/Jahr",
        answerB = "Dhaka (Bangladesch), ca. 10 cm/Jahr",
        answerC = "Mumbai (Indien), ca. 5 cm/Jahr",
        answerD = "Shanghai (China), ca. 3 cm/Jahr",
        correctAnswer = 0, // A
        explanation = "Jakarta gilt als die am schnellsten sinkende Großstadt der Welt – in einigen Stadtteilen (z. B. Nordjakarta) wurden Subsidenzraten von über 25 cm/Jahr gemessen, verursacht durch massiven Grundwasserentzug. Bis 2050 könnte ein Drittel der Stadt unter Wasser liegen. Indonesien verlegt deshalb die Hauptstadt nach Nusantara auf Borneo.",
        difficulty = 4,
        funFact = "Indonesien beschloss 2019, Jakarta als Hauptstadt aufzugeben und auf Borneo die neue Hauptstadt Nusantara zu bauen – ein beispielloser Hauptstadtumzug direkt wegen Subsidenz und Überschwemmungsrisiken."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Karstaquifer-System gilt als das größte zusammenhängende der Welt?",
        answerA = "Floridan Aquifer System (USA, Südosten)",
        answerB = "Großes Artesisches Becken (Australien)",
        answerC = "Dinarischer Karst (Balkanhalbinsel)",
        answerD = "Yucatán-Karst-Aquifer (Mexiko)",
        correctAnswer = 0, // A
        explanation = "Das Floridan Aquifer System im Südosten der USA (Florida, Georgia, Alabama, South Carolina) gilt als eines der produktivsten und ausgedehntesten Karstaquifer-Systeme der Welt mit einer Fläche von ca. 260.000 km². Es ist ein mehrschichtiges Karbonatgesteins-Aquifer und liefert Trinkwasser für Millionen Menschen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Karstgebiet befindet sich das weltgrößte bekannte unterirdische Höhlensystem (nach erkundetem Volumen)?",
        answerA = "Mammoth Cave System (Kentucky, USA)",
        answerB = "Dinarischer Karst (Kroatien/Bosnien)",
        answerC = "Sistema Ox Bel Ha (Yucatán, Mexiko)",
        answerD = "Nullarbor-Karst (Australien)",
        correctAnswer = 0, // A
        explanation = "Das Mammoth Cave System in Kentucky ist mit über 675 km erkundeten Gängen das längste bekannte Höhlensystem der Welt (Stand 2024). Es liegt im Mississippi Valley Karbonatgestein und ist UNESCO-Weltnaturerbe. Das Yucatán-System (Sistema Ox Bel Ha) ist das längste Unterwasserhöhlensystem mit ca. 290 km.",
        difficulty = 4,
        funFact = "Das Mammoth Cave System wächst noch immer – jährlich werden neue Tunnel entdeckt und kartiert. Die Gesamtlänge könnte laut Schätzungen noch Hunderte weiterer Kilometer betragen."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Höhe liegt die mittlere Schneegrenze (Equilibrium Line Altitude) in den Zentralalpen (Schweiz/Österreich) heute?",
        answerA = "ca. 2.400–2.600 m",
        answerB = "ca. 3.000–3.200 m",
        answerC = "ca. 1.800–2.000 m",
        answerD = "ca. 3.800–4.200 m",
        correctAnswer = 1, // B
        explanation = "Die Gleichgewichtslinie (ELA) der Zentralalpen liegt heute bei ca. 3.000–3.200 m über dem Meeresspiegel. Sie ist seit dem Hochstand der Kleinen Eiszeit (ca. 1850) um über 200–300 m angestiegen. Die ELA ist der Höhenbereich, wo Akkumulation und Ablation eines Gletschers gleich sind.",
        difficulty = 4,
        funFact = "In den letzten 30 Jahren ist die ELA in den Alpen um ca. 50–100 m gestiegen – ein direktes Maß für den Klimawandel. Gletscher, deren ELA die Gipfelhöhe überschreitet, sterben innerhalb weniger Jahrzehnte ab."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Höhe liegt die Schneegrenze (ELA) am Kilimandscharo (Tansania) auf der feuchteren Südseite?",
        answerA = "ca. 4.200–4.400 m",
        answerB = "ca. 5.000–5.200 m",
        answerC = "ca. 3.600–3.800 m",
        answerD = "ca. 5.500–5.700 m",
        correctAnswer = 0, // A
        explanation = "Am Kilimandscharo liegt die Gleichgewichtslinie (ELA) auf der niederschlagsreicheren Südseite bei ca. 4.200–4.400 m, auf der trockeneren Nordseite bis zu 5.000 m. Durch den Klimawandel ist die ELA um mehrere Hundert Meter gestiegen und der Gletscher hat seit 1912 über 85 % seiner Fläche verloren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist der paläo-See Agassiz und wo befand er sich?",
        answerA = "Paläo-See im heutigen Sahara, ca. 12.000–6.000 BP",
        answerB = "Eiszeitlicher Gletschersee im heutigen Zentralkanada/Nordminesota, ca. 11.000–8.000 BP",
        answerC = "Paläo-See im heutigen Tibetischen Hochplateau, ca. 8.000–4.000 BP",
        answerD = "Eiszeitlicher See in Nordrussland, ca. 15.000–10.000 BP",
        correctAnswer = 1, // B
        explanation = "Der glaziale Lake Agassiz war der größte Gletschersee der Erdgeschichte – er erstreckte sich vor ca. 11.000–8.000 Jahren über Teile des heutigen Manitoba, Saskatchewan, Ontario (Kanada) und Minnesota/North Dakota (USA). Auf seinem Höhepunkt hatte er eine Fläche von über 440.000 km² – größer als alle heutigen Großen Seen zusammen.",
        difficulty = 4,
        funFact = "Als Lake Agassiz ca. 8.200 v. Chr. durch einen Norddurchbruch kollabierte, strömten Hunderte von Kubikkilometern Süßwasser in den Nordatlantik. Dies störte die thermohaline Zirkulation so stark, dass eine kurze globale Abkühlung (8,2-ka-Ereignis) folgte – ein möglicher Auslöser früher Klimaveränderungen."
    ),

    Question(
        categoryId = 1,
        questionText = "Was war der paläo-See Mega-Tschad und wie groß war er auf seinem Maximum?",
        answerA = "Eiszeitlicher See im Sahel, max. ca. 400.000 km² (fast so groß wie das Kaspische Meer)",
        answerB = "Kleiner Stausee am Tschadbecken, max. 50.000 km²",
        answerC = "Paläo-See in Nordafrika, max. 100.000 km²",
        answerD = "Unterirdischer fossiler Aquifer, kein offener See",
        correctAnswer = 0, // A
        explanation = "Der Paläo-Lake Mega-Tschad existierte während des 'Afrikanischen Feuchten Zeitraums' (Holocene African Humid Period, ca. 10.000–5.000 BP) und hatte auf seinem Maximum eine Ausdehnung von ca. 400.000 km² – vergleichbar mit dem Kaspischen Meer. Er bedeckte Teile des heutigen Tschad, Niger, Nigeria und Kamerun. Der heutige Tschadsee ist ein winziger Rest.",
        difficulty = 4,
        funFact = "Der heutige Tschadsee schrumpft dramatisch – von ca. 25.000 km² in den 1960ern auf heute unter 1.500 km². Er ist ein direktes Symbol des Klimawandels und der Übernutzung von Zuflüssen in der Sahelzone."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist das größte bekannte Löss-Plateau der Erde und wo liegt es?",
        answerA = "Pampas-Löss (Argentinien), ca. 600.000 km²",
        answerB = "Chinesisches Lössplateau (Loess Plateau, Nordchina), ca. 640.000 km²",
        answerC = "Europäischer Lössgürtel (von Frankreich bis Ukraine), ca. 900.000 km²",
        answerD = "Mississippi-Löss (USA, Mittlerer Westen), ca. 300.000 km²",
        correctAnswer = 1, // B
        explanation = "Das Chinesische Lössplateau (Huang Tu Gaoyuan) in Nordchina ist mit ca. 640.000 km² das größte zusammenhängende Lössgebiet der Erde. Die Lössschicht ist dort bis zu 200–300 m tief und wurde während des Pleistozäns durch äolischen (Wind-)Transport aus der Wüste Gobi abgelagert.",
        difficulty = 4,
        funFact = "Das chinesische Lössplateau ist eine der am stärksten erodierten Landschaften der Erde – der Huang He (Gelber Fluss) trägt pro Jahr ca. 1,6 Milliarden Tonnen Löss-Sediment ins Meer und ist der schlammigste Fluss der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebiet Europas hat die mächtigsten Lössablagerungen, die bis zu 50 m tief sind?",
        answerA = "Burgundy (Frankreich)",
        answerB = "Niederrheinische Bucht (Deutschland)",
        answerC = "Vojvodina (Serbien) und ungarische Tiefebene",
        answerD = "Polnische Tiefebene (Masowien)",
        correctAnswer = 2, // C
        explanation = "Die Vojvodina in Serbien und die ungarische Tiefebene (Alföld) haben mit stellenweise über 40–50 m die mächtigsten Lössablagerungen Europas. Dieser Löss wurde während des Pleistozäns vom Wind abgelagert und bildet heute extrem fruchtbare Böden (Schwarzerden). Der Titel-Cliff-Profil Batajnica bei Belgrad ist ein Referenz-Lössprofil der Paläoklimaforschung.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie ist die typische Wellenlänge eines ausgewachsenen Mäanders des Mississippi im Unterläufer?",
        answerA = "ca. 0,5–1 km",
        answerB = "ca. 5–12 km",
        answerC = "ca. 30–50 km",
        answerD = "ca. 2–3 km",
        correctAnswer = 1, // B
        explanation = "Die typische Mäanderwellenlänge des unteren Mississippi beträgt ca. 5–12 km, mit Schlingenmäandern von 3–6 km Breite. Die Wellenlänge eines Mäanders ist empirisch etwa 10–14 Mal die Flussbreite. Bei einem Fluss von 500–800 m Breite ergibt sich rechnerisch eine Wellenlänge von ca. 6–10 km.",
        difficulty = 4,
        funFact = "Mäanderwellenlängen folgen einer universellen Skalierungsregel: Sie entsprechen ca. 10× bis 14× der mittleren Flussbreite – egal ob kleiner Bach oder riesiger Fluss. Diese Leopold-Wolman-Gleichung ist eine der wichtigsten empirischen Geomorphologie-Gesetze."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Mäanderwellenlänge hat der Amazonas im Mittelläufer (Manaus-Bereich)?",
        answerA = "ca. 2–5 km",
        answerB = "ca. 50–100 km",
        answerC = "ca. 20–40 km",
        answerD = "ca. 5–10 km",
        correctAnswer = 2, // C
        explanation = "Im Mittel- und Unterläufer des Amazonas, wo der Fluss mehrere Kilometer breit ist (2–4 km), beträgt die Mäanderwellenlänge ca. 20–40 km nach der Skalierungsregel. Die Mäander des Amazonas sind so groß, dass sie von Satelliten als charakteristisches Muster erkennbar sind.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist ein Pingo und in welchen Breiten kommt er vor?",
        answerA = "Ein eiszeitlicher See in der Arktis",
        answerB = "Ein kegelförmiger Eishügel mit Eiskern im Permafrostgebiet (Arktis und Subarktis)",
        answerC = "Eine periglaziale Schutthaldenform in den Alpen",
        answerD = "Ein flaches Hochmoor im borealen Wald",
        correctAnswer = 1, // B
        explanation = "Ein Pingo ist ein kegelförmiger Hügel mit einem Eiskern, der durch hydrostatischen oder hydrologischen Druck im Permafrostgebiet entsteht. Pingos können bis zu 70 m hoch und 600 m breit werden. Sie kommen vor allem in der Arktis (Kanada, Sibirien, Alaska, Grönland) vor, gelegentlich auch in subarktischen Gebieten.",
        difficulty = 4,
        funFact = "Das Mackenzie-Delta in Kanada hat über 1.350 bekannte Pingos – das größte Pingo-Vorkommen der Erde. Der Name kommt aus der Inuktitut-Sprache der Inuit."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist eine Thermokarstsee und wie entstehen sie?",
        answerA = "Trichterförmige Senke durch Auftauen von Permafrost-Eislinsen",
        answerB = "Karsthohlform in tropischen Kalksteinen",
        answerC = "Salzsee in ariden Regionen ohne Abfluss",
        answerD = "Glazialer Übertiefungssee in Moränentälern",
        correctAnswer = 0, // A
        explanation = "Thermokarstseen entstehen durch das Auftauen von Eis im Permafrost (Eiskeilen, Eislinsen). Wenn das Eis schmilzt, bricht der Boden ein und füllt sich mit Wasser. In Sibirien und Alaska entstehen so Tausende von Seen. Mit dem Klimawandel nehmen Thermokarstseen zu – sie setzen große Mengen CO₂ und Methan frei.",
        difficulty = 4,
        funFact = "In der arktischen Tundra Sibiriens gibt es mehr als eine Million Thermokarstseen. Manche dieser Seen blasen Methan-Blasen auf – sogenannte 'Methanlöcher', die bei Entzündung mit einer Flamme brennen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche periglaziale Landform entsteht durch Frostsortierung von Steinen zu geometrischen Mustern?",
        answerA = "Solifluktionslappen",
        answerB = "Strukturböden (Steinringe, Steinpolygone)",
        answerC = "Thermokarst-Depression",
        answerD = "Pingo",
        correctAnswer = 1, // B
        explanation = "Strukturböden (auch Musterböden oder Frostmusterböden) entstehen durch wiederholtes Gefrieren und Auftauen, das größere Steine nach außen transportiert und so geometrische Muster erzeugt: Steinringe, Steinpolygone, Steinstreifen. Sie kommen im Tundra- und Hochgebirgsbereich vor.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist Solifluktion und in welchem Umfeld tritt sie auf?",
        answerA = "Salzausblühung in Wüsten durch kapillare Wasserbewegung",
        answerB = "Langsames, schichtweises Fließen aufgetauten Bodens über gefrorenem Untergrund",
        answerC = "Tektonisch verursachtes Bodengleiten in aktiven Verwerfungszonen",
        answerD = "Schnelle Massenrutschung durch Starkregenereignisse",
        correctAnswer = 1, // B
        explanation = "Solifluktion (auch Bodenfließen) bezeichnet das langsame Fließen des wassergesättigten Auftauhorizonts (Aktive Schicht) über dem dauerhaft gefrorenen Permafrost. Sie tritt im periglazialen Bereich auf und bewegt Erdmaterial hangabwärts mit Geschwindigkeiten von wenigen Zentimetern bis Dezimetern pro Jahr.",
        difficulty = 4,
        funFact = "Solifluktionslappen und -bögen sind typische Landschaftsformen der subarktischen Tundra. In isländischen und skandinavischen Bergen kann Solifluktion Infrastruktur (Straßen, Leitungen) ernsthaft beschädigen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Anteil am gesamten globalen Meeresspiegel-Beitrag machte die Gletscherschmelze außerhalb der Polargebiete (Berggletscher) zwischen 2000 und 2019 aus?",
        answerA = "ca. 5 %",
        answerB = "ca. 21 %",
        answerC = "ca. 40 %",
        answerD = "ca. 60 %",
        correctAnswer = 1, // B
        explanation = "Laut IPCC AR6 (2021) trugen die Berggletscher (außerhalb Grönland und Antarktis) ca. 21 % zum Meeresspiegelanstieg zwischen 2000 und 2019 bei. Grönland und Antarktis trugen zusammen ca. 36 %, thermische Ausdehnung der Ozeane ca. 38 %. Obwohl Berggletscher wenig Masse haben, schmelzen sie proportional am schnellsten.",
        difficulty = 4,
        funFact = "Die Berggletscher außerhalb der Polargebiete enthalten nur ca. 1 % des globalen Süßwasservorrats, sind aber für Milliarden von Menschen als saisonale Wasserreservoirs unverzichtbar – besonders in Zentralasien, dem Hindukusch und den Anden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Masseverlust hat der Jakobshavn-Gletscher (Sermeq Kujalleq) in Grönland durchschnittlich pro Jahr?",
        answerA = "ca. 20 Gigatonnen/Jahr",
        answerB = "ca. 46 Gigatonnen/Jahr",
        answerC = "ca. 5 Gigatonnen/Jahr",
        answerD = "ca. 100 Gigatonnen/Jahr",
        correctAnswer = 1, // B
        explanation = "Der Jakobshavn-Gletscher verliert durch Kalbung und Schmelze ca. 44–48 Gigatonnen Eis pro Jahr – er allein trägt zu ca. 4–5 % des grönländischen Gesamtmassenverlustes bei. Er ist der schnellste Gletscher der Erde und einer der produktivsten Eisbergproduzenten.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie dick ist das Meereis in der Arktis im winterlichen Durchschnitt (mehrjähriges Eis, 2024)?",
        answerA = "ca. 0,5–1 m (hauptsächlich einjähriges Eis)",
        answerB = "ca. 2–3 m (mehrjähriges Eis)",
        answerC = "ca. 5–7 m (altes Packeis)",
        answerD = "ca. 8–10 m (Presseis)",
        correctAnswer = 1, // B
        explanation = "Mehrjähriges arktisches Meereis hat eine typische Dicke von ca. 2–3 m, während einjähriges Meereis meist 1–2 m erreicht. Durch die Klimaerwärmung ist der Anteil mehrjährigen Eises drastisch zurückgegangen – 2024 dominiert einjähriges, dünnes Eis den Arktischen Ozean, was das Eis im Sommer früher schmelzen lässt.",
        difficulty = 4,
        funFact = "Das arktische Meereis hat seit 1979 (Start der Satellitenmessungen) im Sommer über 40 % seiner Ausdehnung verloren. Das Septemberminimum 2012 war mit ca. 3,4 Millionen km² das kleinste je gemessene."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie hoch ist die mittlere Schneegrenze (ELA) im Karakorum-Gebirge (Zentral-Karakorum) heute?",
        answerA = "ca. 4.000–4.400 m",
        answerB = "ca. 5.500–5.900 m",
        answerC = "ca. 3.200–3.500 m",
        answerD = "ca. 6.200–6.500 m",
        correctAnswer = 1, // B
        explanation = "Im zentralen Karakorum liegt die Gleichgewichtslinie (ELA) bei ca. 5.500–5.900 m – eine der höchsten der Welt, bedingt durch das trockene Kontinentalklima und die extreme Höhe. Trotz hoher ELA gibt es dort mächtige Gletscher (Baltoro, Biafo), weil genügend Niederschlag als Schnee akkumuliert wird.",
        difficulty = 4,
        funFact = "Der Karakorum gilt als das einzige Hochgebirge der Erde, in dem die Gletscher in der ersten Hälfte des 20. Jahrhunderts eher stabil blieben oder wuchsen – bekannt als 'Karakorum-Anomalie'. Die Ursachen (veränderte Windmuster, erhöhte Winterniederschläge) sind noch nicht vollständig geklärt."
    ),

    Question(
        categoryId = 1,
        questionText = "Was war der Paläo-See Bonneville und wo befand er sich?",
        answerA = "Paläo-See im heutigen Turkestan, Zentralasien, vor ca. 20.000 Jahren",
        answerB = "Eiszeitlicher See im Großen Becken (Utah/Idaho/Nevada, USA), max. ca. 51.700 km²",
        answerC = "Pleistozäner See in der australischen Nullarbor-Ebene",
        answerD = "Holozäner Paläo-See in der patagonischen Steppe",
        correctAnswer = 1, // B
        explanation = "Lake Bonneville war ein eiszeitlicher Pluvialsee im Großen Becken der USA (hauptsächlich Utah), der auf seinem Maximum (ca. 18.000–14.500 BP) ca. 51.700 km² bedeckte und bis zu 305 m tief war. Das Bonneville Salt Flats und der Great Salt Lake sind seine heutigen Überreste.",
        difficulty = 4,
        funFact = "Das Bonneville Salt Flats in Utah ist weltberühmt als Ort für Landgeschwindigkeitsrekorde – die spiegelglatte Salzkruste bietet mehrere Kilometer langen Freiraum für Rekordversuche. Bis 2020 wurden dort über 700 Weltrekorde aufgestellt."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Gebiet der Erde treten Yedoma-Ablagerungen auf und was sind sie?",
        answerA = "Warme, calciumreiche Sedimentfolgen in Karst-Tälern der Tropen",
        answerB = "Eisreiche Permafrostsedimente in Sibirien/Alaska mit Eiskeilnetzwerken, bis zu 50 m mächtig",
        answerC = "Vulkanische Tuffe in der isländischen Hochlandtundra",
        answerD = "Fluviale Sedimentabfolgen im Amazonasbecken",
        correctAnswer = 1, // B
        explanation = "Yedoma sind eisreiche, spätpleistozäne Lössablagerungen in Sibirien und Alaska mit enormen Eiskeilen (bis 40–50 m mächtig, Eisanteil bis 80 % des Volumens). Sie wurden während der letzten Eiszeit in einer steppenartigen 'Mammutsteppen'-Landschaft abgelagert. Bei Tauung setzen sie riesige Mengen organischen Kohlenstoffs frei.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist ein Alas (geografischer Begriff) im Kontext der Permafrostgeomorphologie?",
        answerA = "Ein Tundra-Geomorphologietyp, bei dem Thermokarst-Depression ausgetrocknet ist und eine flache, grasbewachsene Mulde bildet",
        answerB = "Ein glazialer Übertiefungssee in Skandinavien",
        answerC = "Eine Küstendünenform im arktischen Schelfgebiet",
        answerD = "Ein Ringwall aus Kies um einen Pingo",
        correctAnswer = 0, // A
        explanation = "Ein Alas (jakutisch für 'Mulde') ist eine flache, ovale Hohlform in der jakutischen Tundra, die durch die Ausdrocknung oder langsame Verlandung ehemaliger Thermokarstseen entsteht. Die Mulde wird dann von Gras und Seggen besiedelt. Die jakutische Landschaft ist geprägt von Millionen solcher Alas-Täler.",
        difficulty = 4,
        funFact = "Alas-Landschaften in Jakutien sind wichtige Weidegründe für die sibirische Pferdehaltung. Die flachen, fruchtbaren Mulden mit hohem Grundwasserstand sind in der sonst trockenen Kältetundra produktive Oasen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Karstaquifer liefert den Großteil des Trinkwassers für die Hauptstädte Wien, Graz und zahlreiche weitere österreichische Städte?",
        answerA = "Dachstein-Karstaquifer",
        answerB = "Hochschwab-Karstaquifer",
        answerC = "Ötztaler-Alpen-Grundwasser",
        answerD = "Donau-Schottteraquifer bei Krems",
        correctAnswer = 1, // B
        explanation = "Der Hochschwab-Karstaquifer im steirischen Salzkammergut ist der wichtigste Trinkwasserlieferant für Wien und andere österreichische Städte. Über die 1. und 2. Wiener Hochquellenwasserleitung (Bau 1873 und 1910) fließt Karstquellwasser aus ca. 100 km Entfernung nach Wien.",
        difficulty = 4,
        funFact = "Wien ist eine der wenigen Großstädte der Welt, die ihr Trinkwasser praktisch unbehandelt aus natürlichen Karstquellen bezieht – ohne Chlorierung. Die 1. Wiener Hochquellenleitung war bei ihrer Eröffnung 1873 die längste Hochdruckleitung der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebiet Westafrikas hat durch saisonale Überschwemmungen ein internes Binnendelta mit wechselnden Mäandermustern gebildet?",
        answerA = "Niger-Binnendelta (Mali, Mopti-Region) – ein saisonales 'Inland Niger Delta'",
        answerB = "Senegal-Mündungsgebiet bei Saint-Louis",
        answerC = "Volta-Stausee-Rückstaugebiet in Ghana",
        answerD = "Kamerun-Hochland-Becken",
        correctAnswer = 0, // A
        explanation = "Das Innere Niger-Delta (Inland Niger Delta) in Mali erstreckt sich auf ca. 40.000 km² und ist eines der größten Binnenfeuchtgebiete der Erde in einer semiariden Region. Jährlich überflutet der Niger ab Juli/August bis zu 40.000 km² – das Flussnetz wechselt sein Mäandermuster saisonal komplett.",
        difficulty = 4,
        funFact = "Das Innere Niger-Delta ernährt Millionen von Menschen durch Fischerei, Reisanbau und saisonale Weide. Es ist nach dem Okavango-Delta das wichtigste Binnenfeuchtgebiet Afrikas und UNESCO-Kulturerbe für die Dogon-Civilization."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist das Verhältnis zwischen Mäanderwellenlänge und Flussbreite (Leopold-Wolman-Regel)?",
        answerA = "Wellenlänge ≈ 3–5 × Flussbreite",
        answerB = "Wellenlänge ≈ 10–14 × Flussbreite",
        answerC = "Wellenlänge ≈ 50–100 × Flussbreite",
        answerD = "Keine stabile Relation – hängt nur von Gefälle ab",
        correctAnswer = 1, // B
        explanation = "Nach Leopold und Wolman (1957) beträgt die Mäanderwellenlänge ca. 10–14 Mal die mittlere Flussbreite. Diese Beziehung gilt über mehrere Größenordnungen – vom kleinen Bach bis zum Amazonas. Sie ist eine der bemerkenswertesten universellen Skalierungsgesetze der Geomorphologie.",
        difficulty = 4,
        funFact = "Dieses Verhältnis gilt sogar für Mäander in der Stratosphäre (Jet-Stream), in Unterwasserströmungen und für gefrorene Flüsse unter Gletschern. Es scheint eine fundamentale physikalische Gesetzmäßigkeit von Strömungen zu sein."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Region hat die größten Lössablagerungen in Nordamerika?",
        answerA = "Nebraska Sandhills (USA)",
        answerB = "Mississippi-Tal-Lössgürtel (Mississippi Loess Bluffs)",
        answerC = "Great Plains Löss (Nebraska, Kansas, Iowa)",
        answerD = "Columbia-Plateau-Löss (Washington State)",
        correctAnswer = 2, // C
        explanation = "Die Great Plains (Nebraska, Kansas, Iowa, South Dakota) haben die ausgedehntesten Lössablagerungen Nordamerikas, die während des Pleistozäns vom Wind aus glazialen Outwash-Ebenen abgelagert wurden. Der Palouse-Lössgürtel in Washington State ist lokal mächtiger (bis 75 m), aber weniger ausgedehnt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie hoch ist die mittlere Schneegrenze in den Centralanden (Altiplano-Region, Bolivien/Peru) bei 16–20°S?",
        answerA = "ca. 4.000–4.500 m",
        answerB = "ca. 5.000–5.500 m",
        answerC = "ca. 3.200–3.600 m",
        answerD = "ca. 6.000–6.500 m",
        correctAnswer = 1, // B
        explanation = "In den zentralen Anden zwischen 16° und 20° Süd liegt die Gleichgewichtslinie (ELA) der Gletscher bei ca. 5.000–5.500 m. Das relativ trockene Klima des Altiplano (Kontinentaleffekt) hält die ELA hoch. Durch den Klimawandel steigt die ELA und die tropischen Gletscher der Anden verschwinden schnell.",
        difficulty = 4,
        funFact = "Die tropischen Gletscher der Andes (Quelccaya, Huascarán) sind einige der am schnellsten schmelzenden Gletscher der Welt. Sie dienen als wichtige saisonale Wasserspeicher für Millionen Menschen und als Klimaarchive (Eisbohrkerne bis 500.000 Jahre alt)."
    ),

    Question(
        categoryId = 1,
        questionText = "Was versteht man unter 'aktiver Permafrostschicht' (Active Layer) und wie tief ist sie typischerweise in der arktischen Tundra?",
        answerA = "Die gesamte Permafrostmächtigkeit; ca. 300–500 m tief",
        answerB = "Der saisonal auftauende Oberflächenbereich über dem permanenten Eis; ca. 30–200 cm tief",
        answerC = "Eine tiefe Grundwasserschicht unter dem Permafrost; ca. 10–50 m tief",
        answerD = "Der saisonale Schneedeckenhorizont; ca. 0,5–2 m",
        correctAnswer = 1, // B
        explanation = "Die 'Aktive Schicht' (Active Layer) ist die oberste Bodenschicht über dem dauerhaft gefrorenen Permafrost, die im Sommer auftaut. In der arktischen Tundra beträgt sie typischerweise 30–80 cm, in wärmeren subarktischen Bereichen bis 200 cm. Ihre Tiefe nimmt mit dem Klimawandel zu und destabilisiert Infrastruktur.",
        difficulty = 4,
        funFact = "Die Zunahme der Aktiven Schicht durch Klimaerwärmung ist ein globales Problem: Straßen, Pipelines und Gebäude in Permafrostgebieten werden durch das tiefere Auftauen beschädigt. Russland meldet bereits Schäden an Gebäuden in den Milliardenhöhe."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche magnetische Deklination hat San Francisco (Kalifornien) ca. 2024?",
        answerA = "ca. +13° (östlich)",
        answerB = "ca. −2° (westlich)",
        answerC = "ca. +5° (östlich)",
        answerD = "ca. −20° (westlich)",
        correctAnswer = 0, // A
        explanation = "In San Francisco beträgt die magnetische Deklination ca. +13–14° (östlich, Stand 2024). An der US-Westküste ist die östliche Deklination stark ausgeprägt und variiert je nach Breitengrad. Diese Deklination muss bei Kompassnavigation und Luftfahrt berücksichtigt werden.",
        difficulty = 4,
        funFact = "Die magnetische Deklination variiert stark über die USA: An der Ostküste (New York) beträgt sie ca. −12° (westlich), in San Francisco ca. +13° (östlich). Die Agone-Null-Linie (0° Deklination) verläuft quer durch die USA von Michigan bis Florida."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist der paläo-See Caspian (Paratethys) und wann existierte er?",
        answerA = "Ein messinischer Eindampfungssee im Mittelmeer, ca. 5,3 Mio. Jahre alt",
        answerB = "Das pleistozäne Vorgänger-Mega-Meer des Kaspischen, Schwarzen und Aralsees, Rückzug ca. 10.000 BP",
        answerC = "Ein eiszeitlicher Gletschersee in Kasachstan, ca. 18.000 BP",
        answerD = "Der fossile Lake Aral, nur ca. 2.000 Jahre alt",
        correctAnswer = 1, // B
        explanation = "Die Paratethys war ein riesiges Binnenmeer, das im Tertiär (vor ca. 34–5 Mio. Jahren) von Mitteleuropa bis Zentralasien reichte. Im Pleistozän führten Klimaschwankungen zu riesigen Vorgängerversionen des Kaspischen Meeres (Caspian Mega-Lake), Schwarzen Meeres und Aralsees, die phasenweise miteinander verbunden waren und sich dann zurückzogen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist die durchschnittliche Bodensenkungsrate in Mexiko-Stadt durch Grundwasserentnahme auf dem ehemaligen Seegrund?",
        answerA = "ca. 2 cm/Jahr",
        answerB = "ca. 5–9 cm/Jahr (bis zu 50 cm lokal)",
        answerC = "ca. 1 mm/Jahr",
        answerD = "ca. 30–50 cm/Jahr (stabile Rate)",
        correctAnswer = 1, // B
        explanation = "Mexiko-Stadt sinkt durch Grundwasserentnahme aus dem tonreichen Untergrund des ehemaligen Texcoco-Sees um durchschnittlich 5–9 cm/Jahr, lokal bis zu 30–50 cm/Jahr. Die kumulative Senkung seit Beginn der Industrialisierung beträgt bis zu 10–15 Meter in manchen Stadtteilen. Die historische Kathedrale neigt sich sichtbar.",
        difficulty = 4,
        funFact = "Die aztekische Hauptstadt Tenochtitlan wurde auf Inseln im Texcoco-See gebaut. Als die Spanier die Stadt unter den See aufschütteten, legten sie den Grundstein für Jahrhunderte von Subsidenz. Mexiko-Stadt ist ein Lehrbeispiel für die Konsequenzen des Bauens auf ehemaligem Seegrund."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Karstaquifer-System versorgt weite Teile des Mittelmeerraums und hat Quellen unter dem Meer?",
        answerA = "Dinarischer Karst (Kroatien/Bosnien)",
        answerB = "Vaucluse-Fontaine (Frankreich, Rhône-Becken)",
        answerC = "Almería-Aquifer (Spanien)",
        answerD = "Peneios-Aquifer (Griechenland)",
        correctAnswer = 0, // A
        explanation = "Der dinarische Karstaquifer (Dinarides) ist mit einer Fläche von ca. 60.000 km² eines der größten Karstaquifere Europas. Er hat zahlreiche submarine Karstquellen (Vrulje) auf dem Meeresboden der Adria – Süßwasser quillt direkt am Meeresboden aus. Diese Quellen waren historisch für Seefahrer als Trinkwasserquelle bekannt.",
        difficulty = 4,
        funFact = "Der dinarische Karst gab der Wissenschaft den Begriff 'Karst' selbst – der Name kommt vom Kras-Plateau (dt. Karst) in Slowenien/Kroatien, wo Geomorphologen im 19. Jahrhundert diese Landschaftsform erstmals systematisch beschrieben."
    ),

    Question(
        categoryId = 1,
        questionText = "Was sind Eiszapfenrippen (Eiskeil-Pseudomorphosen) und was verraten sie über das Klima?",
        answerA = "Rezente Eisformationen in aktiven Permafrostböden der Arktis",
        answerB = "Fossile Keilstrukturen im Boden, wo Eiskeilnetze waren – Beweis für ehemaligen Permafrost",
        answerC = "Glaziale Kristallisationsstrukturen in Eisschilden",
        answerD = "Eisformationen in Gebirgsquellen bei Temperaturstürzen",
        correctAnswer = 1, // B
        explanation = "Eiskeil-Pseudomorphosen (Eiszapfenrippen) sind fossile Bodenmuster, die in gemäßigten Breiten die Lage ehemaliger Eiskeile aus der letzten Eiszeit markieren. Sie beweisen, dass Gebiete wie Norddeutschland, England oder die Niederlande einst von Permafrost bedeckt waren – wertvolle Paläoklima-Indikatoren.",
        difficulty = 4,
        funFact = "Eiskeil-Pseudomorphosen sind in vielen Teilen Mitteleuropas bei Bauarbeiten zu finden. Im norddeutschen Tiefland durchzieht ein dichtes Netz solcher Fossile den Boden – stille Zeugen einer arktischen Periode vor ca. 18.000–12.000 Jahren."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher Höhe liegt die mittlere Schneegrenze in Island heute (nordseitige Gletscher)?",
        answerA = "ca. 500–700 m",
        answerB = "ca. 1.100–1.300 m",
        answerC = "ca. 200–300 m",
        answerD = "ca. 1.800–2.200 m",
        correctAnswer = 0, // A
        explanation = "In Island liegt die Gleichgewichtslinie (ELA) der Gletscher bei ca. 500–700 m auf der niederschlagsreichen (ozeanischen) Seite und ca. 1.000–1.200 m auf der trockeneren Lee-Seite. Islands Gletscher (Vatnajökull, Langjökull) sind bei relativ niedrigen Höhen aktiv, weil der hohe Niederschlag die ELA tief hält.",
        difficulty = 4,
        funFact = "Der Vatnajökull in Island ist mit ca. 7.900 km² der größte Gletscher Europas und das größte Eisvolumen Europas außerhalb der Arktis. Er bedeckt mehrere aktive Vulkane – bei Ausbrüchen entstehen Jökulhlaup (Gletscherlauffluten)."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das Gebiet der Lössablagerungen in Argentinien (Pampa-Löss) und seit wann wurden sie abgelagert?",
        answerA = "ca. 50.000 km², im Holozän (letzte 10.000 Jahre)",
        answerB = "ca. 600.000 km², hauptsächlich im späten Pleistozän (ca. 150.000–10.000 BP)",
        answerC = "ca. 200.000 km², im Miozän (ca. 23–5 Mio. Jahre)",
        answerD = "ca. 100.000 km², im letzten Glazialen Maximum (ca. 22.000–18.000 BP)",
        correctAnswer = 1, // B
        explanation = "Der Pampa-Löss in Argentinien bedeckt ca. 600.000 km² (Provinzen Buenos Aires, Córdoba, Santa Fe, Entre Ríos) und wurde hauptsächlich im späten Pleistozän (ca. 150.000–10.000 BP) durch Wind aus der Patagonischen Steppe transportiert. Er bildet die Grundlage der fruchtbaren Pampa-Böden.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist ein Drumlins und in welchem Prozess entstehen diese Landformen?",
        answerA = "Windgeformte Sanddünen in ariden Periglazialgebieten",
        answerB = "Längliche, eiförmige Hügel aus Moränenmaterial, die subglazial durch Eisbewegung geformt werden",
        answerC = "Kettenförmige Moränenrücken durch Gletschervorstöße",
        answerD = "Subaquatische Sedimentmuster in arktischen Fjorden",
        correctAnswer = 1, // B
        explanation = "Drumlins sind längliche, stromlinienförmige Hügel aus glazialem Till, die durch strömenden Gletscher über älteres Moränenmaterial geformt wurden. Sie zeigen die Fließrichtung des Eises an (ihre Längsachse ist parallel zur Eisflussrichtung) und kommen in Schwärmen vor. Irland, Schottland und die Great Lakes-Region (Kanada/USA) haben klassische Drumlin-Felder.",
        difficulty = 4,
        funFact = "Irland hat eines der dichtesten Drumlin-Felder der Welt – das 'Drumlin Belt' durch Cavan, Monaghan und Armagh mit tausenden eng gepackten Drumlins. Der Name kommt aus dem Irischen: 'droimnín' = kleiner Bergrücken."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche typische Fließgeschwindigkeit hat Solifluktion in der arktischen Tundra (Jahresdurchschnitt)?",
        answerA = "ca. 10–50 m/Jahr",
        answerB = "ca. 0,5–5 cm/Jahr",
        answerC = "ca. 50–200 cm/Jahr",
        answerD = "ca. 1–3 m/Jahr",
        correctAnswer = 1, // B
        explanation = "Solifluktion bewegt sich mit typischerweise 0,5–5 cm/Jahr – sehr langsam, aber kontinuierlich. Im subarktischen Hochland kann sie bis zu 10–20 cm/Jahr erreichen. Diese scheinbar geringe Rate führt über Jahrtausende zu erheblicher Massenverschiebung und charakteristischen Solifluktionslappen am Hang.",
        difficulty = 4,
        funFact = "Solifluktionsterrasschen (kleine Stufenformen am Hang) sind ein typisches Merkmal subarktischer Tundra-Landschaften. In alpinen Lagen Mitteleuropas finden sich noch aktive Solifluktionszonen über der Waldgrenze."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher See Zentralasiens gilt als Überrest des Paläo-Sees Balkhash-Aral-Kaspi-Systems?",
        answerA = "Issyk-Kul (Kirgisistan)",
        answerB = "Kaspisches Meer",
        answerC = "Aralsee",
        answerD = "Balkhaschsee",
        correctAnswer = 3, // D
        explanation = "Der Balkhaschsee in Kasachstan (ca. 16.400 km², stark schwankend) ist ein Überrest eines größeren pleistozänen Seesystems. Er ist einzigartig: Die westliche Hälfte enthält Süßwasser (Fluss Ili mündet dort), die östliche salziges Wasser – getrennt durch eine schmale Durchfahrt. Frühere Megaseen verbanden ihn zeitweise mit Kaspi und Aral.",
        difficulty = 4,
        funFact = "Der Balkhaschsee kämpft heute ums Überleben: Bewässerungsprojekte und Staudämme am Fluss Ili (kommt aus China) haben den Zufluss stark reduziert. Wissenschaftler warnen, er könnte das Schicksal des Aralsees erleiden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Subsidenzrate zeigen GPS-Messungen für das Mekong-Delta (Vietnam) heute?",
        answerA = "ca. 1–2 mm/Jahr",
        answerB = "ca. 1–2 cm/Jahr (lokal bis 4 cm/Jahr)",
        answerC = "ca. 10–15 cm/Jahr",
        answerD = "Keine messbare Subsidenz",
        correctAnswer = 1, // B
        explanation = "Das Mekong-Delta in Vietnam sinkt durch Grundwasserentnahme und Sedimentmangel (durch Staudämme am Mekong) um ca. 1–2 cm/Jahr, lokal bis 4 cm/Jahr. Kombiniert mit dem Meeresspiegelanstieg von ca. 3 mm/Jahr droht dem Delta eine existentielle Überflutungskrise bis 2100.",
        difficulty = 4,
        funFact = "Das Mekong-Delta ernährt ca. 18 Millionen Menschen in Vietnam und produziert die Hälfte des vietnamesischen Reises. Durch Subsidenz und verringerten Sedimentnachschub (ca. 50 % weniger seit dem Staudammbau am oberen Mekong in China) erodiert die Küste jährlich um Hunderte von Metern."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Wert hat die magnetische Deklination in Melbourne (Australien) ca. 2024?",
        answerA = "ca. +11–12° (östlich)",
        answerB = "ca. −5° (westlich)",
        answerC = "ca. +1° (minimal östlich)",
        answerD = "ca. +25° (stark östlich)",
        correctAnswer = 0, // A
        explanation = "In Melbourne (Australien) beträgt die magnetische Deklination ca. +11–12° (östlich, Stand 2024). In der südlichen Hemisphäre ist die Deklination über Australien generell östlich und variiert von ca. 5° im Westen bis ca. 13° im Osten des Kontinents.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist ein Talar und wie unterscheidet er sich von einem anderen Periglazialphänomen?",
        answerA = "Ein Schuttkegel aus periglazialem Frostschutt, der sich durch Frostsprengung und Solifluktion bildet – im Gegensatz zum Pingo, der durch Eislinsen entsteht",
        answerB = "Ein Salzmutterlauge-Pool in arktischen Fjorden",
        answerC = "Eine Windschliff-Fläche (Deflationsfläche) in Kältewüsten",
        answerD = "Eine Auftausenke (Thermokarst) in Sibirien",
        correctAnswer = 0, // A
        explanation = "Ein Talus (Talar) ist ein Schuttkegel oder Schutthang aus Felstrümmern, die durch Frostsprengung (periglaziale Verwitterung) von Steilwänden abgespalten werden und sich am Hangfuß aufhäufen. Im Unterschied zum Pingo (durch Eisdruck) entsteht er durch mechanische Verwitterung und Gravitation. In Gebirgen und arktischen Steilküsten ubiquitär.",
        difficulty = 4,
        funFact = "In manchen alpinen Gebirgen wachsen Schuttkegel (Talus) noch aktiv – besonders wenn Permafrost in den Felswänden auftaut und die Klüfte öffnet. Der Klimawandel erhöht die Steinschlaghäufigkeit in vielen Alpengebieten."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie dick ist das Eisschild auf dem Südpol (Amundsen-Scott-Südpol-Station) ungefähr?",
        answerA = "ca. 500 m",
        answerB = "ca. 2.850 m",
        answerC = "ca. 1.200 m",
        answerD = "ca. 4.500 m",
        correctAnswer = 1, // B
        explanation = "Am geographischen Südpol beträgt die Eisdicke ca. 2.835–2.850 m. Die Amundsen-Scott-Station liegt auf 2.835 m Höhe, das darunterliegende Felsgestein liegt jedoch nahe dem Meeresspiegel. Die durchschnittliche Eisdicke des gesamten antarktischen Eisschildes beträgt ca. 2.160 m.",
        difficulty = 4,
        funFact = "Die Amundsen-Scott-Station bewegt sich jährlich ca. 10 m mit dem fließenden Eis vom geografischen Südpol weg – die Station muss regelmäßig wieder auf den exakten Polpunkt zentriert werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist der Paläo-See Lahontan und wo befand er sich?",
        answerA = "Eiszeitlicher Pluvialsee im Großen Becken (Nevada/Kalifornien), max. ca. 22.300 km²",
        answerB = "Holozäner See in der Atacama-Wüste",
        answerC = "Pleistozäner See auf dem Tibetischen Hochplateau",
        answerD = "Eiszeitlicher See im Tschad-Becken",
        correctAnswer = 0, // A
        explanation = "Lake Lahontan war ein eiszeitlicher Pluvialsee im westlichen Großen Becken (hauptsächlich Nevada, teils Kalifornien und Oregon). Auf seinem Maximum (ca. 14.000 BP) hatte er eine Fläche von ca. 22.300 km² und war bis zu 280 m tief. Der Pyramid Lake in Nevada ist sein direkter Überrest.",
        difficulty = 4,
        funFact = "Der Paläo-See Lahontan und Lake Bonneville waren die größten eiszeitlichen Seen der USA. Als das Klima wärmer und trockener wurde, schrumpften sie auf heutige Größe. Die Salzseen und Salzebenen (Playas) des heutigen Nevada sind ihre eingetrockneten Überreste."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Subsidenzrate zeigen GPS-Messungen für Rotterdam (Niederlande) durch Torfsetzung und Grundwasserentnahme?",
        answerA = "ca. 0,1 mm/Jahr (geologisch stabil)",
        answerB = "ca. 2–3 mm/Jahr (kombiniert glazioisostatisch und anthropogen)",
        answerC = "ca. 10–20 mm/Jahr",
        answerD = "Rotterdam hebt sich durch isostatische Hebung an",
        correctAnswer = 1, // B
        explanation = "Rotterdam sinkt durch die Kombination aus glazioisostatischer Subsidenz (ca. 0,4 mm/Jahr) und Torfverdichtung/Grundwasserabsenkung um insgesamt ca. 1,5–3 mm/Jahr. Kombiniert mit Meeresspiegelanstieg (ca. 3–4 mm/Jahr) steigt die relative Überflutungsgefahr um ca. 4–7 mm/Jahr.",
        difficulty = 4,
        funFact = "Die Niederlande haben weltweit führende Expertise im Küstenschutz entwickelt – notgedrungen, da Amsterdam, Rotterdam und The Hague alle unter dem Meeresspiegel liegen. Das 'Delta Programme' ist ein Milliarden-Investitionsprogramm für Klimaanpassung."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche typische Tiefe hat ein Karstgrundwasserspiegel im dinarischen Karst (Balkanhalbinsel)?",
        answerA = "Karstgrundwasser ist immer an der Erdoberfläche (Quellen)",
        answerB = "Typisch 50–500 m unter der Karstoberfläche, stark variabel nach Niederschlag",
        answerC = "Immer über 1.000 m tief (artesisches System)",
        answerD = "Nur 1–5 m tief wie normales Grundwasser",
        correctAnswer = 1, // B
        explanation = "Im dinarischen Karst variiert der Karstgrundwasserspiegel stark saisonal und regional: In hohen Karstplateaus kann er 50–500 m unter der Oberfläche liegen. In tiefer liegenden Zonen und Küstenbereichen tritt er als submarine oder Küstenquelle aus. Die extremen Schwankungen sind charakteristisch für Karstsysteme.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Mäanderwellenlänge hat der Río Beni (bolivianisch-amazonischer Tiefland) mit einer Breite von ca. 300 m?",
        answerA = "ca. 100–200 m",
        answerB = "ca. 3.000–4.200 m (nach Leopold-Wolman-Regel 10–14× Breite)",
        answerC = "ca. 15–20 km",
        answerD = "ca. 500–800 m",
        correctAnswer = 1, // B
        explanation = "Nach der Leopold-Wolman-Skalierungsregel (Wellenlänge ≈ 10–14× Breite) hätte ein 300 m breiter Fluss wie der Río Beni typische Mäanderwellenlängen von ca. 3.000–4.200 m (3–4 km). Dies deckt sich mit Satellitenmessungen tropischer Tieflandflüsse dieser Größenordnung.",
        difficulty = 4,
        funFact = "Der Río Beni in Bolivien ist für seine aktiven Mäander bekannt – Mäander werden durch Seitenerosion ständig verschoben, schneiden ab (Mäanderdurchbrüche) und bilden Altwässer (Oxbow Lakes). Das bolivianische Amazoniasgebiet ist ein Lehrbuchbeispiel für aktive Mäanderdynamik."
    ),

    Question(
        categoryId = 1,
        questionText = "Was versteht man unter 'glazialer Isostatischer Anpassung' (GIA) und wo hebt sich Europa noch heute?",
        answerA = "Tektonische Plattenhebung durch Magmaeinstrom unter Isostasiezonen",
        answerB = "Langsame Aufwärtsbewegung der Erdkruste nach Entlastung vom Gletschergewicht – Skandinavien hebt sich noch um ca. 1 cm/Jahr",
        answerC = "Küstenerosion durch wellenbedingte Unterschneidung",
        answerD = "Setzung des Meeresbodens nach dem letzten Meeresspiegelmaximum",
        correctAnswer = 1, // B
        explanation = "Glaziale Isostatische Anpassung (GIA) ist die Wiederaufwärtsbewegung der Erdkruste nach dem Abschmelzen der eiszeitlichen Eisschilde (vor ca. 10.000 Jahren). Skandinavien (insbesondere der Bottnische Meerbusen) hebt sich noch um ca. 7–10 mm/Jahr. Dies führt zum Entstehen neuer Küstengebiete aus dem Meer.",
        difficulty = 4,
        funFact = "Durch die isostatische Hebung Skandinaviens erwächst jährlich neues Land aus dem Bottnischen Meerbusen – finnische Bauern haben ihren Landbesitz durch diese natürliche Küstenhebung seit Jahrhunderten vergrößert. In 1.000 Jahren könnten weite Teile des nördlichen Bottnischen Meerbusen trocken sein."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das größte Lössgebiet der Welt nach ausgedehntester Fläche?",
        answerA = "Eurasischer Lössgürtel (von Westeuropa bis China), ca. 2–3 Millionen km² insgesamt",
        answerB = "Chinesisches Lössplateau allein",
        answerC = "Pampa-Löss Südamerikas",
        answerD = "North American Loess Belt (Great Plains + Mississippi)",
        correctAnswer = 0, // A
        explanation = "Der eurasische Lössgürtel – ein Kontinuum von Lössablagerungen von Frankreich über Deutschland, Polen, die Ukraine bis nach China – ist die größte zusammenhängende (aber nicht überall lückenlose) Lösszone der Erde. Er entstand durch äolischen Transport von periglazialem Feinstaub während der Eiszeiten und ist für die Landwirtschaft Europas und Asiens fundamental.",
        difficulty = 4,
        funFact = "Der europäische Lössgürtel ist die Kornkammer Europas: Die fruchtbaren Schwarzerden (Chernozem) der Ukraine, die deutschen Börden und die pannonischen Böden sind alle auf Löss-Unterlagen entstanden – und damit Erben der letzten Eiszeit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Subsidenzrate hat das Niledelta (Ägypten) durch Sedimentmangel und Grundwasserentnahme (Stand 2024)?",
        answerA = "Keine messbare Subsidenz",
        answerB = "ca. 0,1–0,5 mm/Jahr",
        answerC = "ca. 3–8 mm/Jahr (lokal bis 12 mm/Jahr)",
        answerD = "ca. 50 mm/Jahr",
        correctAnswer = 2, // C
        explanation = "Das Niledelta sinkt durch Sedimentmangel (Aswan-Damm hält Sediment zurück), Grundwasserentnahme und Kompaktion um ca. 3–8 mm/Jahr, lokal bis 12 mm/Jahr. Kombiniert mit dem Meeresspiegelanstieg ergibt sich eine relative Überflutungsrate von ca. 6–11 mm/Jahr. Über 30 % des Nildelta könnten bis 2100 unter Wasser liegen.",
        difficulty = 4,
        funFact = "Seit dem Bau des Aswan-Hochdamms (1970) werden ca. 98 % des Nilsediments im Nassersee zurückgehalten – das Niledelta erhält kaum noch Sediment. Früher bauten die jährlichen Nilüberschwemmungen das Delta auf; heute erodiert die Küste jährlich um Meter."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist ein Periglazialklima und in welchen geographischen Breiten herrscht es typischerweise?",
        answerA = "Polares Eis-Klima mit dauerhaftem Meereis (70°N–90°N)",
        answerB = "Klima rund um die Eisränder ohne permanente Eisbedeckung, mit starkem Frostwechsel – subarktisch (60°–70° N/S) und hochalpin",
        answerC = "Ozeanisch-gemäßigtes Klima der britischen Inseln",
        answerD = "Wüstenklima mit extremen Tagesschwankungen (Sahara, Atacama)",
        correctAnswer = 1, // B
        explanation = "Das periglaziale Klima herrscht in Gebieten randlich der Gletscher und Eisschilde – charakteristisch sind viele Frostwechsel pro Jahr (häufiges Auftauen-Gefrieren), langer Bodenfrost, geringe Sommerniederschläge und starke Winde. Es kommt vor in der Subarktis (60°–70° N/S) und in Hochgebirgen (alpin-periglazial). Periglaziale Prozesse haben Mitteleuropa während der Eiszeiten geprägt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die Hauptbodenverflüssigungszone Japans, die beim Tōhoku-Erdbeben 2011 stark betroffen war?",
        answerA = "Bergregionen der japanischen Alpen (Hida-Gebirge)",
        answerB = "Küstenebenen und aufgeschüttete Neulandgebiete rund um die Tokio-Bucht und in der Sendai-Ebene",
        answerC = "Felsküsten der Izu-Halbinsel",
        answerD = "Volcanic Ash Zones rund um den Fuji",
        correctAnswer = 1, // B
        explanation = "Beim Tōhoku-Erdbeben 2011 (M 9,0) trat massivste Bodenverflüssigung in aufgefüllten Küstengebieten und Neulandarealen rund um die Tokio-Bucht auf (besonders in Urayasu, Chiba) sowie in der Sendai-Ebene. Aufgeschüttetes Land (Landfill) und lockere maritime Ablagerungen sind weltweit die gefährlichsten Zonen für Verflüssigung.",
        difficulty = 4,
        funFact = "Die Stadt Urayasu bei Tokio (Disney-Resort-Standort) erlebte 2011 auf über 30 % ihrer Fläche Bodenverflüssigung. Hunderte Häuser kippten, Straßen wurden zerstört. Urayasu liegt fast vollständig auf aufgeschüttetem Boden aus den 1970er Jahren."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief ist der Permafrost auf der Tibetischen Hochfläche (Qinghai-Tibet-Plateau) durchschnittlich?",
        answerA = "ca. 10–30 m",
        answerB = "ca. 50–120 m",
        answerC = "ca. 300–600 m",
        answerD = "ca. 1–5 m (nur saisonaler Frost)",
        correctAnswer = 1, // B
        explanation = "Auf dem Tibetischen Hochplateau (durchschnittliche Höhe ca. 4.500 m) beträgt die Permafrosttiefe ca. 50–120 m im Mittel, mit Maxima bis 150 m in den kältesten Bereichen. Das Plateau hat das größte Permafrostgebiet mittlerer Breiten der Erde (ca. 1,06 Millionen km² Permafrost).",
        difficulty = 4,
        funFact = "Die Qinghai-Tibet-Eisenbahn (Lhasa-Bahn) musste auf Permafrostboden gebaut werden – Ingenieure entwickelten spezielle Kühlanker und Belüftungsdämme, um das Auftauen des Permafrosts durch die Eisenbahnwärme zu verhindern."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Wert hat die magnetische Deklination in Moskau (Russland) ungefähr (Stand 2024)?",
        answerA = "ca. +12° (östlich)",
        answerB = "ca. +6° (östlich)",
        answerC = "ca. −3° (westlich)",
        answerD = "ca. +20° (stark östlich)",
        correctAnswer = 1, // B
        explanation = "In Moskau beträgt die magnetische Deklination ca. +6–7° (östlich, Stand 2024). In ganz Russland nimmt die östliche Deklination von West nach Ost zu – im Westen Russlands noch ca. 5–8°, in Ostsibirien können es über 20° sein. Die starke Wanderung des Magnetpols nach Sibirien beeinflusst diese Werte laufend.",
        difficulty = 4,
        funFact = "In manchen russischen Gebieten Ostsibiriens beträgt die magnetische Deklination über 20° – ein Kompasstand wäre dort ohne Kalibrierung stark irreführend. Für arktische Expeditionen ist die genaue Kenntnis der Deklination überlebenswichtig."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Bodenverflüssigungsgebiet in der San Francisco Bay Area (USA) ist historisch am gefährlichsten eingestuft?",
        answerA = "Granit-Grundgestein der Peninsula (San Francisco Halbinsel)",
        answerB = "Aufgeschüttete Küstenbereiche der Bay (Marina District, SOMA, East Bay Fill)",
        answerC = "Vulkangestein des Mount Tamalpais",
        answerD = "Sandsteinformationen von Marin County",
        correctAnswer = 1, // B
        explanation = "Die höchste Bodenverflüssigungsgefahr in der Bay Area liegt in aufgefüllten Gezeitenbereichen der San Francisco Bay – wie dem Marina District (der beim Loma-Prieta-Erdbeben 1989 stark beschädigt wurde) und großen Teilen der East Bay (Oakland, Berkeley Flatlands). Diese Gebiete wurden im 19./20. Jahrhundert durch Aufschüttung für Bebauung gewonnen.",
        difficulty = 4,
        funFact = "Beim Loma-Prieta-Erdbeben 1989 (M 6,9) war der Marina District in San Francisco, der auf aufgeschüttetem Boden (teils Trümmer des Erdbebens 1906) steht, am schwersten betroffen – trotz größerer Entfernung zum Epizentrum als stabilere Bereiche."
    ),

)


