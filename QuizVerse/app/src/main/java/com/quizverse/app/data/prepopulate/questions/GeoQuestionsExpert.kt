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

)
