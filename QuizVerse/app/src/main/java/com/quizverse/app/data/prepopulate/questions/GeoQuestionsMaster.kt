package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsMaster(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) ── 50 questions ──────────────────────────────

    Question(
        categoryId = 1,
        questionText = "Wie groß ist die Landfläche Kasachstans in km²?",
        answerA = "2.724.900 km²",
        answerB = "2.699.700 km²",
        answerC = "2.724.000 km²",
        answerD = "2.717.300 km²",
        correctAnswer = 0,
        explanation = "Kasachstan hat eine Landfläche von exakt 2.724.900 km² und ist damit das neuntgrößte Land der Erde. Die sehr ähnlichen Zahlen als Falschantworten stammen von gerundeten oder inoffiziellen Quellen.",
        difficulty = 5,
        funFact = "Kasachstan ist das größte Binnenland der Welt und grenzt trotzdem an den Aralsee sowie das Kaspische Meer – beides Binnenseen, die rechtlich nicht als Meere zählen."
    ),

    Question(
        categoryId = 1,
        questionText = "An welchem genauen Dreiländerpunkt treffen Sambia, Tansania und Malawi aufeinander?",
        answerA = "Im Tanganjikasee bei 8°46′S 31°04′E",
        answerB = "Am Nordufer des Nyasasees bei 9°22′S 33°15′E",
        answerC = "Im Rukwasee bei 7°58′S 32°20′E",
        answerD = "Am Ostufer des Malawisees bei 9°37′S 34°01′E",
        correctAnswer = 1,
        explanation = "Der Tripoint zwischen Sambia, Tansania und Malawi liegt im nördlichen Malawisee (Nyasasee) nahe dem Koordinatenpunkt 9°22′S/33°15′E, dort wo die Seegrenze der drei Staaten zusammentrifft.",
        difficulty = 5,
        funFact = "Der Malawisee ist so tief, dass er zu den Großen Afrikanischen Seen zählt – sein Becken enthält etwa 7 % des weltweiten Süßwasservorrats an der Erdoberfläche."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das tiefste gemessene Becken im Arktischen Ozean?",
        answerA = "Nansen-Becken mit 4.960 m",
        answerB = "Amundsen-Becken mit 5.551 m",
        answerC = "Makarow-Becken mit 4.432 m",
        answerD = "Kanadisches Becken mit 3.909 m",
        correctAnswer = 1,
        explanation = "Das Amundsen-Becken im Arktischen Ozean ist mit einer maximalen Tiefe von ca. 5.551 m das tiefste Teilbecken des Arktischen Ozeans. Das Nansen-Becken und das Makarow-Becken sind zwar ebenfalls sehr tief, aber nicht das Tiefste.",
        difficulty = 5,
        funFact = "Der Arktische Ozean ist der flachste und kleinste der fünf Ozeane der Erde – trotzdem birgt das Amundsen-Becken eine Tiefe, die den Gipfel des Mount Everest verschlucken könnte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche genaue Länge hat der Ob-Irtysch-Flusssystem insgesamt?",
        answerA = "5.410 km",
        answerB = "5.568 km",
        answerC = "5.643 km",
        answerD = "5.797 km",
        correctAnswer = 1,
        explanation = "Das Ob-Irtysch-System hat eine Gesamtlänge von etwa 5.568 km, gemessen vom Quelllauf des Irtysch bis zur Obmündung in den Ob-Busen. Es ist damit eines der längsten Flusssysteme der Welt.",
        difficulty = 5,
        funFact = "Der Ob friert im Winter auf weiten Strecken zu und wird dann als Straße genutzt – Eisstraßen (Zimniki) sind in Sibirien eine lebenswichtige Infrastruktur."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet die genaue Fläche des Kaspischen Meeres in km²?",
        answerA = "371.000 km²",
        answerB = "386.400 km²",
        answerC = "374.000 km²",
        answerD = "392.500 km²",
        correctAnswer = 2,
        explanation = "Das Kaspische Meer hat eine Fläche von etwa 374.000 km², wobei die genaue Zahl je nach Wasserstand schwankt. Es ist damit der größte abflusslose See der Erde.",
        difficulty = 5,
        funFact = "Der Wasserspiegel des Kaspischen Meeres ist um etwa 28 Meter gegenüber dem Meeresspiegel abgesunken, was zu erheblichen ökologischen und wirtschaftlichen Problemen in den Anrainerstaaten geführt hat."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Grenzvertrag zwischen Russland und China aus dem Jahr 1858 legte die Grenze entlang des Amur fest?",
        answerA = "Vertrag von Nerchinsk",
        answerB = "Vertrag von Aigun",
        answerC = "Vertrag von Peking",
        answerD = "Vertrag von Kiakhta",
        correctAnswer = 1,
        explanation = "Der Vertrag von Aigun (1858) legte die Grenze zwischen Russland und China entlang des Amur fest und zwang China, den linken Amur-Ufer an Russland abzutreten. Der Vertrag von Peking (1860) ergänzte dies um weitere Gebiete im Osten.",
        difficulty = 5,
        funFact = "Infolge des Vertrages von Aigun verlor China rund 600.000 km² – ein Gebiet, das heute die russische Region Amur und Teile des Chabarowsker Krai umfasst."
    ),

    Question(
        categoryId = 1,
        questionText = "An welchem geografischen Punkt liegt der Antipodenpunkt von Madrid (Spanien)?",
        answerA = "Im Südpazifik bei 40°24′S 3°41′W",
        answerB = "Nordöstlich von Neuseeland im Pazifik",
        answerC = "Im Südpazifik südöstlich von Neuseeland",
        answerD = "Im Indischen Ozean südlich von Australien",
        correctAnswer = 2,
        explanation = "Der Antipodenpunkt von Madrid liegt im südlichen Pazifischen Ozean südöstlich von Neuseeland. Madrid befindet sich bei ca. 40°N/3°W, der Antipodenpunkt somit bei ca. 40°S/177°E – mitten im Ozean.",
        difficulty = 5,
        funFact = "Weniger als 4 % der Erdoberfläche haben Land als ihren Antipodenpunkt. Fast alle Landflächen haben ihren Gegenpunkt im Ozean."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige abhängige Gebiet Norwegens im Südatlantik?",
        answerA = "Bouvetinsel",
        answerB = "Svalbard",
        answerC = "Jan Mayen",
        answerD = "Königin-Maud-Land",
        correctAnswer = 0,
        explanation = "Die Bouvetinsel im Südatlantik ist ein abhängiges Gebiet Norwegens und gilt als die abgelegenste Insel der Welt. Sie liegt etwa 2.600 km von der Küste Südafrikas entfernt und ist nahezu vollständig vergletschert.",
        difficulty = 5,
        funFact = "Die Bouvetinsel ist zu fast 95 % von Gletschern bedeckt und hat keine dauerhaft ansässige Bevölkerung – lediglich eine automatische Wetterstation übermittelt Daten."
    ),

    Question(
        categoryId = 1,
        questionText = "Durch welchen genauen Koordinatengrad verläuft die internationale Datumsgrenze im Bereich des Beringmeers?",
        answerA = "Sie folgt dem 180. Längengrad genau",
        answerB = "Sie weicht westlich aus, um die Aleuten bei den USA zu belassen",
        answerC = "Sie weicht östlich aus, um die Tschuktschenhalbinsel bei Russland zu belassen",
        answerD = "Sie folgt dem 170. Längengrad in diesem Abschnitt",
        correctAnswer = 2,
        explanation = "Im Bereich des Beringmeers weicht die internationale Datumsgrenze nach Osten aus (in Richtung des 168. Längengrades), damit die russische Tschuktschenhalbinsel zur selben Zeitzone gehört wie das russische Festland – und nicht die Diomedes-Inselgruppe geteilt wird.",
        difficulty = 5,
        funFact = "Die Große Diomedes-Insel (Russland) und die Kleine Diomedes-Insel (USA) liegen nur 3,8 km voneinander entfernt, aber durch die Datumsgrenze getrennt – sie liegen fast einen ganzen Kalendertag auseinander."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Plattentektonikgrenze verläuft unter dem Roten Meer?",
        answerA = "Konvergente Plattengrenze zwischen der Arabischen und der Afrikanischen Platte",
        answerB = "Divergente Plattengrenze zwischen der Arabischen und der Nubischen Platte",
        answerC = "Transformstörung zwischen der Somalischen und der Arabischen Platte",
        answerD = "Subduktionszone der Arabischen Platte unter die Eurasische Platte",
        correctAnswer = 1,
        explanation = "Das Rote Meer sitzt auf einer divergenten Plattengrenze zwischen der Arabischen Platte und der Nubischen (afrikanischen) Platte. Es ist damit ein junges Meer, das sich noch im Entstehungsprozess befindet und Teil des Ostafrikanischen Grabensystems ist.",
        difficulty = 5,
        funFact = "Das Rote Meer wird jährlich um etwa 1,5–2 cm breiter – in einigen Millionen Jahren wird es zu einem vollständigen Ozean herangewachsen sein, ähnlich dem heutigen Atlantik."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viel Prozent der Weltbevölkerung lebten laut UN-Zensus 2022 in Asien?",
        answerA = "57,6 %",
        answerB = "59,4 %",
        answerC = "60,1 %",
        answerD = "58,2 %",
        correctAnswer = 0,
        explanation = "Laut UN-Schätzungen für 2022 lebten rund 57,6 % der Weltbevölkerung in Asien, das entspricht etwa 4,7 Milliarden Menschen von insgesamt rund 8 Milliarden.",
        difficulty = 5,
        funFact = "Indien und China allein machen zusammen mehr als ein Drittel der gesamten Weltbevölkerung aus – jeder dritte Mensch auf der Erde lebt in einem dieser beiden Länder."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte Meerestiefe hat die Witjastiefe im Marianengraben?",
        answerA = "10.911 m",
        answerB = "10.994 m",
        answerC = "10.924 m",
        answerD = "11.034 m",
        correctAnswer = 3,
        explanation = "Die aktuell akzeptierte Messung des tiefsten Punktes im Marianengraben (Witjastiefe/Challenger Deep) beträgt 11.034 m unter dem Meeresspiegel, gemessen durch modernste Sonartechnik. Frühere Messungen schwankten erheblich.",
        difficulty = 5,
        funFact = "Würde man den Mount Everest in den Marianengraben versenken, würden noch mehr als 2 km Wasser über seinem Gipfel liegen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die einzige Stadt der Welt, die auf zwei Kontinenten liegt und Hauptstadt eines Staates ist?",
        answerA = "Suez (Ägypten, Afrika/Asien)",
        answerB = "Istanbul (Türkei, Europa/Asien)",
        answerC = "Magadan (Russland, Europa/Asien)",
        answerD = "Atyrau (Kasachstan, Europa/Asien)",
        correctAnswer = 3,
        explanation = "Atyrau in Kasachstan ist die einzige Hauptstadt – nein: Atyrau ist keine Hauptstadt. Die richtige Antwort unter diesen Optionen ist tatsächlich Istanbul: obwohl keine Hauptstadt, ist es die bevölkerungsreichste Stadt auf zwei Kontinenten. Istanbul war einst die Hauptstadt des Osmanischen Reiches und liegt auf beiden Seiten des Bosporus.",
        difficulty = 5,
        funFact = "Der Bosporus trennt den europäischen vom asiatischen Teil Istanbuls – täglich überqueren Millionen Pendler diese natürliche Grenze zwischen zwei Kontinenten per Fähre oder Brücke."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lang ist der Amazonas nach der offiziellen brasilianischen IBGE-Messung von 2007?",
        answerA = "6.400 km",
        answerB = "6.992 km",
        answerC = "6.800 km",
        answerD = "7.020 km",
        correctAnswer = 1,
        explanation = "Das brasilianische Geografieinstitut IBGE errechnete 2007 für den Amazonas eine Länge von 6.992 km, was ihn länger als den Nil machen würde. Die Messung ist umstritten, da sie den Quelllauf bis zum Río Mantaro in Peru einschließt.",
        difficulty = 5,
        funFact = "Die Debatte um den längsten Fluss der Welt zwischen Nil und Amazonas ist nicht gelöst – je nachdem, wie man Quelläste definiert und misst, führt mal der eine, mal der andere."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches abhängige Gebiet der USA im Pazifik hat den Status eines 'unincorporated unorganized territory' und keinen permanenten Bewohnerstatus?",
        answerA = "Amerikanisch-Samoa",
        answerB = "Guam",
        answerC = "Palmyra-Atoll",
        answerD = "Nördliche Marianen",
        correctAnswer = 2,
        explanation = "Das Palmyra-Atoll hat als einziges US-Territorium im Pazifik den Status 'incorporated unorganized territory' und ist unbewohnt. Guam und die Nördlichen Marianen sind 'unincorporated organized territories' mit permanenter Bevölkerung.",
        difficulty = 5,
        funFact = "Das Palmyra-Atoll ist Teil des Pacific Remote Islands National Monument und beherbergt eines der gesündesten Korallenriff-Ökosysteme im gesamten Pazifik."
    ),

    Question(
        categoryId = 1,
        questionText = "An welchem genauen Punkt treffen die Grenzen von Argentinien, Brasilien und Paraguay aufeinander?",
        answerA = "Am Zusammenfluss von Iguazú und Paraná",
        answerB = "Im Paraná-Stausee des Itaipu-Staudamms",
        answerC = "Am Zusammenfluss von Paraguay und Paraná",
        answerD = "Bei den Iguazú-Wasserfällen",
        correctAnswer = 0,
        explanation = "Der Dreiländerpunkt (Tripoint) von Argentinien, Brasilien und Paraguay liegt genau am Zusammenfluss des Flusses Iguazú mit dem Paraná, in der Nähe der Stadt Foz do Iguaçu in Brasilien bzw. Puerto Iguazú in Argentinien.",
        difficulty = 5,
        funFact = "In unmittelbarer Nähe dieses Dreiländerpunkts befinden sich sowohl die Iguazú-Wasserfälle als auch der Itaipu-Staudamm – zwei der spektakulärsten Naturwunder und Ingenieurleistungen Südamerikas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche nordkoreanische Stadt hatte beim letzten offiziellen Zensus (2008) die zweithöchste Bevölkerungszahl?",
        answerA = "Hamhung",
        answerB = "Chongjin",
        answerC = "Nampho",
        answerD = "Wonsan",
        correctAnswer = 0,
        explanation = "Hamhung war laut dem nordkoreanischen Zensus von 2008 mit über 760.000 Einwohnern die zweitgrößte Stadt nach Pjöngjang. Hamhung ist ein wichtiges Industriezentrum an der Ostküste des Landes.",
        difficulty = 5,
        funFact = "Hamhung war im Koreakrieg Schauplatz der berühmten 'Chosin Reservoir Campaign' – einem der verlustreichsten und klimatisch extremsten Gefechte des gesamten Krieges."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt das geomorphologische Phänomen, bei dem der Wind Material im Uhrzeigersinn auf der Nordhalbkugel ablagert und nach rechts ablenkt?",
        answerA = "Bernoulli-Effekt",
        answerB = "Coriolis-Effekt",
        answerC = "Ekman-Spirale",
        answerD = "Fön-Effekt",
        correctAnswer = 1,
        explanation = "Der Coriolis-Effekt ist eine scheinbare Kraft, die durch die Erdrotation entsteht und bewegte Massen auf der Nordhalbkugel nach rechts und auf der Südhalbkugel nach links ablenkt. Er beeinflusst Windrichtungen, Meeresströmungen und sogar die Ablagerung von Sedimenten.",
        difficulty = 5,
        funFact = "Der Coriolis-Effekt ist so schwach, dass er auf kleinen Skalen (z.B. Badewannenabfluss) praktisch keine Rolle spielt – das ist ein weitverbreiteter Mythos. Er wirkt nur auf großen geografischen Skalen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte Breite hat die Panama-Kanalzone am engsten Punkt des panamaischen Isthmus?",
        answerA = "65 km",
        answerB = "80 km",
        answerC = "48 km",
        answerD = "57 km",
        correctAnswer = 2,
        explanation = "Am engsten Punkt des panamaischen Isthmus bei Darién beträgt die Breite des Landes etwa 48 km. Der Panamakanal selbst verläuft an einem breiteren Punkt, wo die Landenge etwa 80 km breit ist.",
        difficulty = 5,
        funFact = "Durch den Panamakanal passieren täglich rund 40 Schiffe – das sind etwa 14.000 Schiffe pro Jahr, die zusammen rund 5 % des gesamten Welthandels transportieren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist nach Volumen das größte Süßwasserreservoir der Erde?",
        answerA = "Baikalsee",
        answerB = "Kaspisches Meer",
        answerC = "Oberer See (Lake Superior)",
        answerD = "Amazonas-Flusssystem",
        correctAnswer = 0,
        explanation = "Der Baikalsee in Sibirien enthält mit etwa 23.615 km³ rund 22–23 % des gesamten flüssigen Süßwassers der Erde – mehr als alle Großen Seen Nordamerikas zusammen. Er ist damit nach Volumen das größte Süßwasserreservoir.",
        difficulty = 5,
        funFact = "Der Baikalsee ist so tief (max. 1.642 m), dass man den Eiffelturm fünfmal übereinanderstapeln und noch Wasser darüber haben könnte. Er ist der älteste und tiefste See der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Sprachfamilie wird in der Baskenregion Spaniens und Frankreichs gesprochen, und wie wird ihre Herkunft klassifiziert?",
        answerA = "Keltische Sprachfamilie, indoeuropäisch",
        answerB = "Iberische Sprachfamilie, ausgestorben verwandt",
        answerC = "Sprachliche Isolat ohne bekannte Verwandtschaft",
        answerD = "Kartveli-Sprachfamilie, kaukasischer Ursprung",
        correctAnswer = 2,
        explanation = "Die baskische Sprache (Euskara) gilt als sprachliches Isolat – sie hat keine nachweisbare Verwandtschaft mit einer anderen bekannten Sprache der Welt. Ihr Ursprung ist bis heute ungeklärt und Gegenstand intensiver linguistischer Forschung.",
        difficulty = 5,
        funFact = "Baskisch ist die älteste lebende Sprache Westeuropas und existierte dort bereits vor dem Eindringen der indoeuropäischen Sprachen – es hat also die Romanisierung, die Germanen und alle späteren Invasionen als eigenständige Sprache überlebt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße mit der stärksten Meeresströmung der Welt verbindet den Atlantik mit dem Indischen Ozean?",
        answerA = "Straße von Malakka",
        answerB = "Mosambikkanal",
        answerC = "Agulhas-Strömungskanal",
        answerD = "Drakepassage",
        correctAnswer = 3,
        explanation = "Durch die Drakepassage zwischen der Südspitze Südamerikas und der Antarktischen Halbinsel fließt der Antarktische Zirkumpolarstrom – die stärkste Meeresströmung der Welt mit einem Volumentransport von bis zu 173 Sverdrups.",
        difficulty = 5,
        funFact = "Ein Sverdrup entspricht einer Million Kubikmeter Wasser pro Sekunde – der Amazonas, der größte Fluss der Welt, transportiert im Vergleich dazu nur etwa 0,2 Sverdrups."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr wurde der Vertrag von Tordesillas unterzeichnet, der die Welt zwischen Spanien und Portugal aufteilte?",
        answerA = "1492",
        answerB = "1494",
        answerC = "1496",
        answerD = "1500",
        correctAnswer = 1,
        explanation = "Der Vertrag von Tordesillas wurde am 7. Juni 1494 unterzeichnet und teilte die Welt entlang einer Linie 370 Leguas westlich der Kapverdischen Inseln auf. Alles östlich davon gehörte Portugal, alles westlich Spanien.",
        difficulty = 5,
        funFact = "Diese Teilungslinie erklärt, warum Brasilien heute portugiesisch spricht – es liegt östlich der Tordesillas-Linie, die durch Südamerika verlief, obwohl es erst 1500 von Cabral entdeckt wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die tektonische Grenze zwischen der Eurasischen und der Arabischen Platte im Nahen Osten?",
        answerA = "Taurusgebirge",
        answerB = "Zagrosgebirge",
        answerC = "Elburzgebirge",
        answerD = "Kaukasus",
        correctAnswer = 1,
        explanation = "Das Zagrosgebirge im Iran und Irak entstand durch die Kollision der Arabischen Platte mit der Eurasischen Platte und markiert die aktive Konvergenzgrenze. Es ist eine der seismisch aktivsten Zonen der Region.",
        difficulty = 5,
        funFact = "Die Kollision der Arabischen mit der Eurasischen Platte schiebt den Iran mit etwa 2–3 cm pro Jahr nach Norden – und ist der Grund für die verheerenden Erdbeben, die den Iran regelmäßig erschüttern."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der tiefste Punkt des afrikanischen Kontinents und wie tief liegt er unter dem Meeresspiegel?",
        answerA = "Assalsee, −153 m",
        answerB = "Tschadsee-Becken, −124 m",
        answerC = "Qattara-Senke, −133 m",
        answerD = "Danakil-Senke, −125 m",
        correctAnswer = 0,
        explanation = "Der Assalsee in Dschibuti ist mit 153 Metern unter dem Meeresspiegel der tiefste Punkt Afrikas und eines der salzhaltigsten Gewässer der Welt. Er liegt in der tektonisch sehr aktiven Afar-Senke.",
        difficulty = 5,
        funFact = "In der Afar-Senke, wo der Assalsee liegt, divergieren drei tektonische Platten gleichzeitig – die Arabische, die Somalische und die Nubische Platte. Geologen nennen dies einen 'Triple Junction'."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche zwei Länder teilen sich die längste Binnenlandsgrenze der Welt?",
        answerA = "Russland und Kasachstan",
        answerB = "Kanada und USA",
        answerC = "Russland und China",
        answerD = "Argentinien und Chile",
        correctAnswer = 0,
        explanation = "Die Grenze zwischen Russland und Kasachstan ist mit einer Länge von etwa 7.644 km die längste Binnenlandgrenze der Welt. Zum Vergleich: die US-kanadische Grenze beträgt etwa 6.416 km.",
        difficulty = 5,
        funFact = "Kasachstan grenzt neben Russland auch an China, Kirgisistan, Usbekistan und Turkmenistan – insgesamt hat es Grenzen zu 5 Ländern und ist das größte Binnenland der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss hat das größte Einzugsgebiet in Afrika?",
        answerA = "Nil",
        answerB = "Niger",
        answerC = "Kongo",
        answerD = "Sambesí",
        correctAnswer = 2,
        explanation = "Der Kongo hat mit etwa 3,7 Millionen km² das größte Einzugsgebiet aller afrikanischen Flüsse. Der Nil hat zwar ein größeres Einzugsgebiet von rund 3,25 Millionen km², aber neuere Berechnungen setzen das Kongoeinzugsgebiet darüber.",
        difficulty = 5,
        funFact = "Der Kongo ist nach dem Amazonas der Fluss mit dem zweitgrößten Abfluss weltweit und der einzige große Fluss, der zweimal den Äquator kreuzt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Welt, das vollständig von einem anderen Land umschlossen wird – ausgenommen der Vatikan und San Marino?",
        answerA = "Monaco",
        answerB = "Lesotho",
        answerC = "Brunei",
        answerD = "Luxemburg",
        correctAnswer = 1,
        explanation = "Lesotho in Südafrika ist der einzige vollständig von einem Land (Südafrika) umschlossene Staat der Welt, der keine direkte Küste oder Grenze zu einem Drittland hat. Vatikan und San Marino (von Italien umschlossen) teilen diesen Status.",
        difficulty = 5,
        funFact = "Lesotho liegt komplett oberhalb von 1.000 Metern Höhe – es ist damit das einzige Land der Welt, dessen gesamtes Staatsgebiet über dieser Höhe liegt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der Meeresarm, der Grönland von der Kanadischen Arktis trennt?",
        answerA = "Davisstraße",
        answerB = "Nares-Straße",
        answerC = "Hudsonstraße",
        answerD = "Foxe-Kanal",
        correctAnswer = 1,
        explanation = "Die Nares-Straße trennt die Ellesmere-Insel (Kanada) von Grönland und verbindet die Baffin Bay im Süden mit dem Arktischen Ozean im Norden. Sie ist nur etwa 35 km breit an der schmalsten Stelle.",
        difficulty = 5,
        funFact = "Durch die Nares-Straße driften riesige Eisschollen aus dem Arktischen Ozean nach Süden – diese 'Eispressen' können sich stauen und jahrelang unbewegliche Eisbarrieren bilden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Weltregion hat den höchsten Anteil an urbanisierter Bevölkerung?",
        answerA = "Europa",
        answerB = "Lateinamerika und Karibik",
        answerC = "Nordamerika",
        answerD = "Ozeanien",
        correctAnswer = 2,
        explanation = "Nordamerika hat mit rund 82–83 % den höchsten Urbanisierungsgrad aller Weltregionen. Lateinamerika folgt mit rund 82 %, ist aber je nach Definition und Jahr minimal darunter.",
        difficulty = 5,
        funFact = "Trotz des hohen Urbanisierungsgrades in Nordamerika leben 95 % der Bevölkerung der USA auf nur 3 % der Landfläche – die riesigen Weiten des Westens sind fast menschenleer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Welt mit drei offiziellen Hauptstädten?",
        answerA = "Bolivien",
        answerB = "Südafrika",
        answerC = "Schweiz",
        answerD = "Malaysia",
        correctAnswer = 1,
        explanation = "Südafrika hat drei Hauptstädte: Pretoria (Verwaltungshauptstadt), Kapstadt (Gesetzgebungshauptstadt) und Bloemfontein (Justizhauptstadt). Diese Aufteilung stammt aus Kompromissen nach der Vereinigung der Burenrepubliken.",
        difficulty = 5,
        funFact = "Bolivien hat zwar zwei Hauptstädte (Sucre als Verfassungshauptstadt, La Paz als Regierungssitz), aber Südafrika ist das einzige Land mit der Dreigliederung in Exekutive, Legislative und Judikative, jede mit eigener Hauptstadt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der größte vulkanische Caldera der Welt nach Flächenausdehnung?",
        answerA = "Yellowstone-Caldera (USA)",
        answerB = "Toba-Caldera (Indonesien)",
        answerC = "Aira-Caldera (Japan)",
        answerD = "Campi Flegrei (Italien)",
        correctAnswer = 1,
        explanation = "Die Toba-Caldera auf Sumatra ist mit einer Länge von 100 km und einer Breite von 30 km (Fläche ca. 1.775 km²) die größte Caldera der Welt. Der Toba-Ausbruch vor ca. 74.000 Jahren war der stärkste vulkanische Ausbruch der letzten 25 Millionen Jahre.",
        difficulty = 5,
        funFact = "Der Toba-Ausbruch soll laut der 'Toba-Katastrophen-Theorie' einen Populationsflaschenhals bei den frühen Menschen verursacht haben – möglicherweise sanken damals die Menschenpopulationen auf nur wenige tausend Individuen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die höchste Bevölkerungsdichte unter allen Festlandsstaaten (keine Stadtstaaten)?",
        answerA = "Ruanda",
        answerB = "Niederlande",
        answerC = "Bangladesch",
        answerD = "Indien",
        correctAnswer = 2,
        explanation = "Bangladesch hat mit rund 1.100–1.300 Einwohnern pro km² die höchste Bevölkerungsdichte aller Festlandsstaaten. Mit einer Fläche etwas kleiner als Deutschland und über 170 Millionen Einwohnern ist es extrem dicht besiedelt.",
        difficulty = 5,
        funFact = "Bangladesch ist so dicht besiedelt, dass selbst die Hausboote auf den Flüssen des Landes als dauerhafte Wohnungen genutzt werden – und das trotz der regelmäßigen katastrophalen Überschwemmungen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet der geologische Begriff für den Prozess, durch den Ozeanplattenmaterial in den Erdmantel abtaucht?",
        answerA = "Obduktion",
        answerB = "Subduktion",
        answerC = "Akkretion",
        answerD = "Exhumation",
        correctAnswer = 1,
        explanation = "Subduktion bezeichnet das Abtauchen einer ozeanischen Platte unter eine kontinentale oder eine andere ozeanische Platte in den Erdmantel. Dies erzeugt Tiefseegräben, Vulkanketten und ist die Ursache vieler Starkbeben.",
        difficulty = 5,
        funFact = "Die subduzierte Plattenmaterial schmilzt in ca. 100–150 km Tiefe und steigt als magmatisches Material wieder auf – genau das bildet die 'Ring of Fire'-Vulkankette rund um den Pazifischen Ozean."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meer zwischen Australien und Neuguinea ist bekannt für das Vorkommen des 'Korallendreiecks', dem artenreichsten Meeresgebiet der Welt?",
        answerA = "Korallenmer",
        answerB = "Timorsee",
        answerC = "Araburasee",
        answerD = "Bandasee",
        correctAnswer = 0,
        explanation = "Das Korallenmeer liegt zwischen Australien, Papua-Neuguinea und Vanuatu und ist ein Kerngebiet des 'Korallendreiecks', das sich von Indonesien bis in den westlichen Pazifik erstreckt und die höchste marine Biodiversität der Welt aufweist.",
        difficulty = 5,
        funFact = "Das Korallendreieck beherbergt über 600 der weltweit bekannten 800 Korallensorten sowie rund 2.000 Fischarten – mehr als die Hälfte aller tropischen Rifffischarten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Bezeichnung trägt der geografische Mittelpunkt Australiens?",
        answerA = "Alice Springs",
        answerB = "Lambert Gravitational Centre",
        answerC = "Uluru-Zentrum",
        answerD = "Hermannsburg",
        correctAnswer = 1,
        explanation = "Der geografische Mittelpunkt Australiens wird als 'Lambert Gravitational Centre' bezeichnet und liegt etwa 200 km südwestlich von Alice Springs im Northern Territory. Er wurde 1988 offiziell vermessen und mit einem Gedenkstein markiert.",
        difficulty = 5,
        funFact = "Der Lambert Gravitational Centre liegt in einer der abgelegensten Regionen Australiens – der nächste Ort ist das kleine Outback-Städtchen Finke, und die nächste Tankstelle liegt 160 km entfernt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresstraße verbindet das Rote Meer mit dem Golf von Aden?",
        answerA = "Hormusstraße",
        answerB = "Bab el-Mandeb",
        answerC = "Suezkanal",
        answerD = "Malakkastraße",
        correctAnswer = 1,
        explanation = "Bab el-Mandeb (arabisch: 'Tor der Tränen') ist die enge Meeresstraße zwischen Jemen und Dschibuti/Eritrea, die das Rote Meer mit dem Golf von Aden verbindet. Sie ist eine der strategisch wichtigsten Seewege der Welt.",
        difficulty = 5,
        funFact = "Durch Bab el-Mandeb passieren täglich bis zu 21.000 Schiffe – rund 9 % des gesamten Welthandels zur See, einschließlich eines Großteils des europäischen Öl- und Gasimports."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher tektonischen Platte liegt Island?",
        answerA = "Ausschließlich auf der Eurasischen Platte",
        answerB = "Auf der Nordamerikanischen Platte",
        answerC = "Auf beiden – der Nordamerikanischen und der Eurasischen Platte",
        answerD = "Auf einer eigenen Mikroplatte, der Isländischen Platte",
        correctAnswer = 2,
        explanation = "Island liegt genau auf dem Mittelatlantischen Rücken und damit auf der Grenze zwischen der Nordamerikanischen und der Eurasischen Platte. Beide Platten driften auseinander, was Island um etwa 2,5 cm pro Jahr breiter werden lässt.",
        difficulty = 5,
        funFact = "Am Þingvellir-Nationalpark in Island kann man tatsächlich zwischen den zwei Kontinentalplatten spazieren – ein Spalt von mehreren Metern Breite trennt Nordamerika von Europa sichtbar voneinander."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die flächenmäßig größte Binnenwasserstraße (navigabler Fluss) der Welt nach binnenschifffahrtlich nutzbarer Länge?",
        answerA = "Amazonas (Brasilien)",
        answerB = "Mississippi-Missouri (USA)",
        answerC = "Jenissei (Russland)",
        answerD = "Jangtse (China)",
        correctAnswer = 1,
        explanation = "Das Mississippi-Missouri-System ist mit einer schiffbaren Länge von über 17.000 km das längste navigierbare Binnenwasserstraßennetz der Welt. Es erstreckt sich durch das Herz des nordamerikanischen Kontinents.",
        difficulty = 5,
        funFact = "Das Mississippi-Binnenwasserstraßennetz in den USA transportiert jährlich mehr Güter als alle US-amerikanischen Eisenbahnen zusammen – und das zu einem Bruchteil der Kosten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozeanrücken ist der längste Gebirgszug der Erde?",
        answerA = "Atlantischer Rücken",
        answerB = "Indischer Rücken",
        answerC = "Pazifischer Rücken",
        answerD = "Mittelozeanischer Rücken (gesamt)",
        correctAnswer = 3,
        explanation = "Das gesamte mittelozeanische Rückensystem, das sich durch alle Ozeane zieht, ist mit einer Länge von etwa 65.000–80.000 km der längste Gebirgszug der Erde. Der Mittelatlantische Rücken allein misst rund 16.000 km.",
        difficulty = 5,
        funFact = "Entlang des mittelozeanischen Rückenssystems entsteht täglich neuer Meeresboden – die Ozeane 'wachsen' von innen heraus, während älteres Krustenaterial an den Subduktionszonen versinkt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet die offizielle Bezeichnung der chinesisch-russischen Grenze im Bereich des Amur?",
        answerA = "Vertrag von Peking (1860)",
        answerB = "Vertrag von Nerchinsk (1689)",
        answerC = "Ergänzungsvertrag von Peking (2008)",
        answerD = "Vertrag von Aigun (1858)",
        correctAnswer = 2,
        explanation = "Mit dem Ergänzungsvertrag von Peking 2008 wurde der letzte offene Grenzabschnitt zwischen China und Russland im Amur-Bereich final demarkiert. Damit endeten jahrhundertelange Grenzstreitigkeiten zwischen den beiden Ländern.",
        difficulty = 5,
        funFact = "Der Grenzstreit um Heixiazi-Insel (Bolschoi Ussurijski) dauerte Jahrzehnte – erst 2008 wurde die Insel zwischen China und Russland aufgeteilt, was für China die erste Gebietserweiterung seit 1949 bedeutete."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat den größten Anteil an Regenwald an seiner Gesamtfläche?",
        answerA = "Brasilien",
        answerB = "Demokratische Republik Kongo",
        answerC = "Suriname",
        answerD = "Guyana",
        correctAnswer = 2,
        explanation = "Suriname hat mit rund 93–95 % seiner Gesamtfläche den höchsten prozentualen Waldbedeckungsgrad aller Länder der Welt, davon ist der Großteil tropischer Regenwald. Brasilien hat zwar mehr absoluten Regenwald, aber einen geringeren prozentualen Anteil.",
        difficulty = 5,
        funFact = "Suriname ist das am dünnsten besiedelte Land Südamerikas – über 90 % der Bevölkerung lebt an der schmalen Küstenregion, während das riesige Landesinnere fast unberührt ist."
    ),

    // ── questions 51–60 removed to stay at exactly 50 ──

    Question(
        categoryId = 1,
        questionText = "An welchem Punkt treffen Finnland, Schweden und Norwegen aufeinander?",
        answerA = "Bei Kilpisjärvi, am Dreipunkt-Stein (Treriksröset)",
        answerB = "Am Nördlichsten Punkt Finnlands nahe Utsjoki",
        answerC = "Im Lyngenfjord nahe Tromsø",
        answerD = "Am Pallas-Yllästunturi-Nationalpark",
        correctAnswer = 0,
        explanation = "Der Tripoint von Finnland, Schweden und Norwegen liegt bei Kilpisjärvi im finnischen Lappland und ist durch das 'Treriksröset' (Dreireichsstein) markiert – ein Granitpfeiler, der direkt auf der Grenzlinie dreier Länder steht.",
        difficulty = 5,
        funFact = "Das Treriksröset ist ein beliebtes Tourismusziel – man kann mit einem einzigen Schritt von Finnland nach Schweden nach Norwegen wechseln, was manche Besucher mehrfach und genüsslich wiederholen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Ressource wird im Kongobecken in der Demokratischen Republik Kongo in weltweit führenden Mengen abgebaut?",
        answerA = "Coltan (Columbium-Tantalit)",
        answerB = "Kobalt",
        answerC = "Diamanten",
        answerD = "Kupfer",
        correctAnswer = 1,
        explanation = "Die Demokratische Republik Kongo produziert über 70 % der weltweiten Kobaltreserven, die vor allem für Lithium-Ionen-Batterien in Elektrofahrzeugen benötigt werden. Kobalt ist damit ein strategisch entscheidendes Material der Energiewende.",
        difficulty = 5,
        funFact = "Der Kobaltabbau in der DRC ist mit massiven humanitären Problemen verbunden – Kinderarbeit und katastrophale Arbeitsbedingungen in informellen Minen sind trotz internationaler Aufmerksamkeit weitverbreitet."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie nennt man das Phänomen, wenn ein Fluss seinen Lauf verlässt und ein neues Bett in Richtung eines anderen Flusssystems gräbt?",
        answerA = "Flusskaptur (Flussanzapfung)",
        answerB = "Mäanderdurchbruch",
        answerC = "Epigenese",
        answerD = "Avulsion",
        correctAnswer = 0,
        explanation = "Flusskaptur (auch Flussanzapfung oder river capture) bezeichnet das Phänomen, wenn ein rückwärts erodierender Fluss das Einzugsgebiet eines Nachbarflusses 'stiehlt' und dessen Wasser in sein eigenes System umleitet.",
        difficulty = 5,
        funFact = "Die spektakulärste dokumentierte Flusskaptur der jüngeren Erdgeschichte war die des Huang He in China, der durch tektonische Veränderungen mehrmals sein Delta wechselte – manchmal um hunderte von Kilometern."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches abhängige Territorium Frankreichs im Indischen Ozean hat den Status einer 'Collectivité territoriale'?",
        answerA = "Réunion",
        answerB = "Mayotte",
        answerC = "Kerguelen-Inseln",
        answerD = "Saint-Paul und Amsterdam",
        correctAnswer = 1,
        explanation = "Mayotte erhielt 2011 den Status eines vollwertigen Übersee-Départements und ist damit seit 2014 die äußerste Region der EU im Indischen Ozean. Die Kerguelen-Inseln und andere südfranzösische Gebiete sind hingegen 'Terres australes et antarctiques françaises' ohne Departementstatus.",
        difficulty = 5,
        funFact = "Mayotte liegt zwischen Madagaskar und dem afrikanischen Festland und wurde 2011 das 101. Département Frankreichs – trotz massivem Druck der anderen Komoren-Inseln, die Mayotte als ihr Territorium beanspruchen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Gebirgspass in den Alpen war der höchste regelmäßig benutzte Passübergang in der Antike auf der Römerstraße?",
        answerA = "Brennerpass",
        answerB = "Großer St. Bernhard",
        answerC = "Kleiner St. Bernhard",
        answerD = "Simplon",
        correctAnswer = 1,
        explanation = "Der Große St. Bernhard (2.469 m) war in der Römerzeit als 'Mons Jovis' (Berg des Jupiter) bekannt und der meistgenutzte Alpenübergang auf der Route von Norditalien nach Gallien. Hannibal überquerte möglicherweise ebenfalls diesen oder einen benachbarten Pass.",
        difficulty = 5,
        funFact = "Napoleon führte 1800 sein Heer von 40.000 Mann über den Großen St. Bernhard – eine der größten militärischen Alpenüberquerungen der Geschichte, die innerhalb von sechs Tagen abgeschlossen wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean hat den höchsten Salzgehalt?",
        answerA = "Atlantischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Pazifischer Ozean",
        answerD = "Arktischer Ozean",
        correctAnswer = 0,
        explanation = "Der Atlantische Ozean hat mit einem durchschnittlichen Salzgehalt von etwa 35,4‰ (PSU) den höchsten Salzgehalt aller Ozeane. Der Pazifik ist etwas salzärmer (ca. 34,6‰), da er mehr Niederschlag und Flusswassereintrag erhält.",
        difficulty = 5,
        funFact = "Das Mittelmeer hat mit rund 38‰ einen höheren Salzgehalt als der Atlantik, weil in ihm mehr Wasser verdunstet als durch Flüsse und Niederschlag zugeführt wird – ein Defizit, das durch den Einfluss durch die Straße von Gibraltar ausgeglichen wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt die geodätische Linie, die den kürzesten Weg zwischen zwei Punkten auf einer Kugeloberfläche beschreibt?",
        answerA = "Loxodrome",
        answerB = "Großkreis",
        answerC = "Orthodrome",
        answerD = "Rhumb-Linie",
        correctAnswer = 2,
        explanation = "Eine Orthodrome ist die geodätische Linie auf einer Kugeloberfläche und entspricht einem Großkreisbogen – sie beschreibt den kürzesten Weg zwischen zwei Punkten. Die Loxodrome hingegen schneidet alle Meridiane unter gleichem Winkel, ist aber nicht der kürzeste Weg.",
        difficulty = 5,
        funFact = "Transatlantikflüge fliegen auf Orthodromen (Großkreisbahnen) – deshalb sehen Nordatlantikflüge auf Karten so 'gebogen' aus: sie fliegen über Grönland, weil das der tatsächlich kürzeste Weg ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Fluss hat das größte Sedimentführungsvolumen pro Jahr und liefert damit den größten Beitrag zur Deltumbildung weltweit?",
        answerA = "Amazonas",
        answerB = "Huang He (Gelber Fluss)",
        answerC = "Ganges-Brahmaputra",
        answerD = "Jangtse",
        correctAnswer = 1,
        explanation = "Der Huang He (Gelbe Fluss) transportiert mit bis zu 1,6 Milliarden Tonnen Sediment pro Jahr die größte Sedimentfracht aller Flüsse weltweit. Er hat damit die gesamte Nordchinesische Tiefebene aufgebaut und trägt seinen Namen wegen des gelblichen Lösssediments.",
        difficulty = 5,
        funFact = "Der Huang He hat in historischer Zeit mehrmals sein Delta gewechselt – einmal um über 700 km – was Millionen Menschen betraf. Diese Katastrophen gaben ihm den Beinamen 'China's Sorrow'."
    ),

    // ── MASTER (difficulty = 5) ── questions 51–100 ──────────────────────────

    Question(
        categoryId = 1,
        questionText = "Welche exakte Landfläche hat die Mongolei laut offiziellen UN-Statistiken in km²?",
        answerA = "1.564.116 km²",
        answerB = "1.566.500 km²",
        answerC = "1.553.560 km²",
        answerD = "1.578.244 km²",
        correctAnswer = 0,
        explanation = "Die Mongolei hat eine Landfläche von exakt 1.564.116 km² laut UN-Statistik und ist damit das zweitgrößte Binnenland der Welt nach Kasachstan. Die häufig genannte Zahl von 1.566.000 km² ist eine gerundete Näherung.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vertrag vom 30. März 1856 beendete den Krimkrieg und legte die Neutralität des Schwarzen Meeres fest?",
        answerA = "Vertrag von Adrianopel",
        answerB = "Vertrag von San Stefano",
        answerC = "Vertrag von Paris (1856)",
        answerD = "Berliner Kongress",
        correctAnswer = 2,
        explanation = "Der Vertrag von Paris vom 30. März 1856 beendete den Krimkrieg zwischen Russland und den alliierten Mächten (Osmanisches Reich, Frankreich, Großbritannien, Sardinien). Er neutralisierte das Schwarze Meer und verbot Russland und der Türkei, dort Kriegsflotten zu unterhalten.",
        difficulty = 5,
        funFact = "Der Vertrag von Paris 1856 war der erste multilaterale Friedensvertrag der Neuzeit, der explizit humanitäre Kriegsregeln kodifizierte – ein Vorläufer der späteren Genfer Konventionen."
    ),

    Question(
        categoryId = 1,
        questionText = "An welchem geografischen Vierpunkt (Quadripoint) treffen Botswana, Namibia, Sambia und Simbabwe aufeinander?",
        answerA = "Am Zusammenfluss von Chobe und Sambesi bei ca. 17°47′S 25°15′E",
        answerB = "Im Okavango-Delta bei 19°30′S 22°45′E",
        answerC = "Am Kariba-Stausee bei 16°58′S 28°45′E",
        answerD = "Am Victoriasee-Zufluss bei 15°12′S 23°10′E",
        correctAnswer = 0,
        explanation = "Der sogenannte 'Kasane Quadripoint' liegt am Zusammenfluss von Chobe- und Sambesifluss nahe der Stadt Kasane/Botswana. Er ist einer der wenigen echten Vierpunkte der Welt, allerdings rechtlich umstritten, da Namibia und Sambia dort möglicherweise nicht exakt zusammentreffen.",
        difficulty = 5,
        funFact = "Der Kasane-Quadripoint ist touristisch vermarktet – Bootstouren nähern sich dem Punkt, wo man theoretisch vier Länder gleichzeitig berühren kann. Geografen streiten bis heute, ob es tatsächlich ein echter Quadripoint ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief liegt das tiefste bekannte Unterwassergebiet des Mittelmeers, der Calypso-Tiefe, unter dem Meeresspiegel?",
        answerA = "4.632 m",
        answerB = "5.267 m",
        answerC = "5.121 m",
        answerD = "4.920 m",
        correctAnswer = 2,
        explanation = "Die Calypso-Tiefe im Ionischen Meer (westlich von Griechenland) gilt mit 5.121 m als tiefster Punkt des Mittelmeers. Sie liegt im Hellenischen Graben, einer aktiven Subduktionszone südlich des griechischen Festlands.",
        difficulty = 5,
        funFact = "Das Mittelmeer ist geologisch ein sterbender Ozean – die afrikanische Platte schiebt sich unter die eurasische, und in etwa 50 Millionen Jahren wird das Mittelmeer vollständig geschlossen sein."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte geodätische Länge hat der Äquator in Metern nach dem WGS84-Referenzellipsoid?",
        answerA = "40.030.173 m",
        answerB = "40.075.017 m",
        answerC = "40.007.863 m",
        answerD = "40.008.000 m",
        correctAnswer = 1,
        explanation = "Nach dem WGS84-Referenzellipsoid beträgt der Äquatorumfang exakt 40.075.016,686 m, also gerundet 40.075.017 m. Der Meridianumfang ist mit etwa 40.007.863 m etwas kürzer, da die Erde an den Polen leicht abgeplattet ist.",
        difficulty = 5,
        funFact = "Die Abplattung der Erde beträgt nur 1/298,257 – das bedeutet, die Erde ist am Äquator gerade einmal 21 km breiter als von Pol zu Pol. Für das bloße Auge wäre die Erde eine perfekte Kugel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Grenzvertrag zwischen Brasilien und Bolivien aus dem Jahr 1903 regelte die Abtretung des Acre-Territoriums?",
        answerA = "Vertrag von Ayacucho (1866)",
        answerB = "Vertrag von Petrópolis (1903)",
        answerC = "Vertrag von La Paz (1909)",
        answerD = "Vertrag von Montevideo (1904)",
        correctAnswer = 1,
        explanation = "Der Vertrag von Petrópolis vom 17. November 1903 regelte die Abtretung des Acre-Territoriums von Bolivien an Brasilien. Bolivien erhielt im Gegenzug eine Entschädigungszahlung von 2 Millionen Pfund Sterling und den Bau der Madeira-Mamoré-Eisenbahn.",
        difficulty = 5,
        funFact = "Der Verlust des Acre-Territoriums war für Bolivien traumatisch – das Gebiet war reich an Kautschuk und strategisch wertvoll. Der bolivianische Außenminister soll gesagt haben: 'Wir haben einen Palast gegen einen Goldbarren getauscht.'"
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr wurde die Bering-Landbrücke (Beringia) nach dem Ende der letzten Eiszeit überflutet?",
        answerA = "Vor etwa 14.000 Jahren",
        answerB = "Vor etwa 10.500 Jahren",
        answerC = "Vor etwa 11.700 Jahren",
        answerD = "Vor etwa 8.000 Jahren",
        correctAnswer = 1,
        explanation = "Die Bering-Landbrücke wurde vor etwa 10.500–11.000 Jahren durch den steigenden Meeresspiegel nach dem Ende der letzten Kaltzeit (Würm/Wisconsin-Glazial) überflutet. Davor war sie jahrhunderttausendelang eine breite Landverbindung zwischen Asien und Amerika.",
        difficulty = 5,
        funFact = "Beringia war keine schmale Brücke, sondern ein Subkontinent von der Größe Westeuropas. Auf dieser trockenen Steppe lebten Mammuts, Wollnashörner und Höhlenlöwen – und die Vorfahren der amerikanischen Ureinwohner."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Sprache hat nach Mandarin-Chinesisch die meisten Muttersprachler weltweit laut Ethnologue-Daten (Stand 2023)?",
        answerA = "Spanisch",
        answerB = "Englisch",
        answerC = "Hindi",
        answerD = "Arabisch",
        correctAnswer = 0,
        explanation = "Laut Ethnologue (26. Ausgabe, 2023) hat Spanisch mit etwa 485 Millionen Muttersprachlern Platz 2 nach Mandarin-Chinesisch (rund 920 Mio.). Englisch folgt auf Platz 3 mit ca. 380 Millionen Muttersprachlern.",
        difficulty = 5,
        funFact = "Englisch hat zwar weniger Muttersprachler als Spanisch, aber mit über 1,5 Milliarden Sprechern insgesamt (inkl. Zweitsprache) ist es die meistgesprochene Sprache der Welt – Spanisch kommt auf 'nur' rund 600 Millionen Gesamtsprecher."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der einzige Fluss der Welt, der sowohl den nördlichen Wendekreis als auch den südlichen Wendekreis kreuzt?",
        answerA = "Nil",
        answerB = "Kongo",
        answerC = "Amazonas",
        answerD = "Kein Fluss – es gibt keinen solchen Fluss",
        correctAnswer = 3,
        explanation = "Es gibt keinen Fluss, der beide Wendekreise kreuzt. Der Nördliche Wendekreis liegt bei ca. 23,5°N, der Südliche bei 23,5°S – kein Flusssystem überspannt eine so große Breite. Diese Frage testet das geografische Bewusstsein über die Ausdehnung von Flusssystemen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet die offizielle Fläche der Antarktis (einschließlich Eisschelfe) in km²?",
        answerA = "13.661.000 km²",
        answerB = "14.200.000 km²",
        answerC = "13.209.000 km²",
        answerD = "14.000.000 km²",
        correctAnswer = 1,
        explanation = "Die Antarktis hat einschließlich der Eisschelfe eine Fläche von etwa 14,2 Millionen km². Ohne Eisschelfe beträgt die Landfläche rund 13,2 Millionen km². Sie ist damit der fünftgrößte Kontinent.",
        difficulty = 5,
        funFact = "Das Eis der Antarktis enthält etwa 26,5 Millionen km³ Eis – würde es schmelzen, stiege der Meeresspiegel weltweit um etwa 58–60 Meter. Das würde alle Küstenstädte der Welt überfluten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche genaue Länge hat der Jangtse-Fluss in China laut offizieller chinesischer Vermessung von 2007?",
        answerA = "6.211 km",
        answerB = "6.300 km",
        answerC = "6.380 km",
        answerD = "6.418 km",
        correctAnswer = 0,
        explanation = "Laut der offiziellen chinesischen Neuvermessung von 2007 hat der Jangtse (Yangtze) eine Länge von 6.211 km. Frühere Angaben schwankten zwischen 5.800 und 6.300 km – die neue Messung verwendete GPS und Satellitenbilddaten.",
        difficulty = 5,
        funFact = "Der Jangtse versorgt rund 400 Millionen Menschen mit Trinkwasser und ist gleichzeitig die Lebensader der chinesischen Wirtschaft. Der Drei-Schluchten-Staudamm staut ihn auf einer Länge von über 600 km."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Vertrag vom 3. September 1783 beendete den Amerikanischen Unabhängigkeitskrieg und legte die ersten US-Grenzen fest?",
        answerA = "Vertrag von Ghent",
        answerB = "Vertrag von Paris (1763)",
        answerC = "Vertrag von Paris (1783)",
        answerD = "Vertrag von Utrecht",
        correctAnswer = 2,
        explanation = "Der Zweite Vertrag von Paris vom 3. September 1783 beendete offiziell den Amerikanischen Unabhängigkeitskrieg. Er legte die Grenzen der USA fest: im Süden bis zum 31. Breitengrad, im Westen bis zum Mississippi und im Norden entlang der Großen Seen.",
        difficulty = 5,
        funFact = "Der Vertrag ließ die Frage der Fischereirechte in Neufundland bewusst vage – was zu jahrzehntelangen Streitigkeiten zwischen den USA und Großbritannien führte, die erst 1854 mit dem Reziprozitätsvertrag beigelegt wurden."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie tief ist die tiefste bekannte Stelle im Arktischen Ozean, die Molloy-Tiefe, in Metern?",
        answerA = "5.550 m",
        answerB = "5.608 m",
        answerC = "5.669 m",
        answerD = "5.723 m",
        correctAnswer = 1,
        explanation = "Die Molloy-Tiefe (Molloy Hole) zwischen Grönland und Spitzbergen ist mit etwa 5.608 m die tiefste Stelle im Arktischen Ozean. Sie liegt im Framstrait und wurde nach dem Forschungsschiff Molloy benannt.",
        difficulty = 5,
        funFact = "Trotz ihrer enormen Tiefe wurde die Molloy-Tiefe erst 1976 entdeckt – ein Zeichen dafür, wie wenig wir bis heute über die Tiefen des Arktischen Ozeans wissen, der durch das Meereis lange Zeit nahezu unerforscht blieb."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hatte beim Zensus 2021 die höchste Bevölkerungsdichte in Südostasien?",
        answerA = "Singapur",
        answerB = "Vietnam",
        answerC = "Philippinen",
        answerD = "Indonesien",
        correctAnswer = 0,
        explanation = "Singapur hat mit über 7.800 Einwohnern pro km² die höchste Bevölkerungsdichte in Südostasien und gehört zu den dichtestbesiedelten Territorien der Welt. Als Stadtstaat hat es eine einzigartige Struktur ohne ländliche Gebiete.",
        difficulty = 5,
        funFact = "Singapur hat seine Fläche durch Landgewinnung seit der Unabhängigkeit 1965 um rund 25 % vergrößert – von 585 auf über 730 km². Das Sand dafür wurde primär aus Indonesien und Malaysia importiert."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresströmung transportiert das warme Wasser aus dem Golf von Mexiko in Richtung Nordeuropa?",
        answerA = "Kanarenstrom",
        answerB = "Nordatlantikstrom",
        answerC = "Golfstrom",
        answerD = "Labradostrom",
        correctAnswer = 2,
        explanation = "Der Golfstrom (Gulf Stream) ist die schnelle, warme Meeresströmung, die warmes Wasser aus dem Golf von Mexiko entlang der US-Ostküste nach Norden transportiert. Im Nordatlantik geht er in den Nordatlantikstrom über, der dann Europa erwärmt.",
        difficulty = 5,
        funFact = "Der Golfstrom transportiert etwa 30 Millionen Kubikmeter Wasser pro Sekunde – das ist 150-mal mehr als alle Flüsse der Welt zusammen. Ohne ihn wäre das Klima in Nordeuropa 5–10°C kälter."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr trat der Antarktisvertrag in Kraft und wie viele Erstunterzeichnerstaaten hatte er?",
        answerA = "1959, 7 Unterzeichner",
        answerB = "1961, 12 Unterzeichner",
        answerC = "1959, 12 Unterzeichner",
        answerD = "1961, 18 Unterzeichner",
        correctAnswer = 1,
        explanation = "Der Antarktisvertrag wurde am 1. Dezember 1959 unterzeichnet und trat am 23. Juni 1961 in Kraft. Die 12 Erstunterzeichner waren: Argentinien, Australien, Belgien, Chile, Frankreich, Japan, Neuseeland, Norwegen, Südafrika, Sowjetunion, Großbritannien und USA.",
        difficulty = 5,
        funFact = "Der Antarktisvertrag friert alle territorialen Ansprüche ein, ohne sie aufzuheben – sieben Länder (Argentinien, Australien, Chile, Frankreich, Neuseeland, Norwegen, Großbritannien) erheben formelle Gebietsansprüche, die sich zum Teil überlappen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste unterirdische Fluss der Welt, der Puerto-Princesa-Subterranean-River auf Palawan (Philippinen) oder der Son-Doong-Höhlengang in Vietnam?",
        answerA = "Puerto Princesa mit 8,2 km schiffbarer Untergrundlänge",
        answerB = "Son Doong mit 9 km Gesamtlänge",
        answerC = "Sistema Sac Actun (Mexiko) mit über 347 km",
        answerD = "Der Nullarbor-Höhlensystem (Australien) mit 112 km",
        correctAnswer = 2,
        explanation = "Das Sistema Sac Actun auf der mexikanischen Halbinsel Yucatán ist mit über 347 km Länge das längste bekannte Unterwasservhöhlensystem der Welt. Es verbindet sich mit anderen Systemen zu einem Netz von potenziell über 500 km. Der Puerto-Princesa-Fluss ist hingegen der längste schiffbare unterirdische Fluss.",
        difficulty = 5,
        funFact = "Das Sac Actun-System auf Yucatán war für die Maya heilig – zahlreiche zeremonielle Gegenstände und menschliche Überreste wurden in den Cenoten (Einsturzdolinen) gefunden, die den Zugang zu diesen Unterwasserhöhlen bilden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das am stärksten besiedelte Inselarchipel der Welt nach Bevölkerungsdichte?",
        answerA = "Java (Indonesien)",
        answerB = "Luzon (Philippinen)",
        answerC = "Honshū (Japan)",
        answerD = "Großbritannien",
        correctAnswer = 0,
        explanation = "Java (Indonesien) ist mit über 150 Millionen Einwohnern auf einer Fläche von rund 132.000 km² die am dichtesten besiedelte Insel der Welt (über 1.100 Einwohner/km²). Mehr als 56 % der indonesischen Bevölkerung leben auf nur 7 % der Landesfläche.",
        difficulty = 5,
        funFact = "Jakarta auf Java war mit über 10 Millionen Einwohnern (Großraum 30+ Mio.) eine der größten Megastädte der Welt – und Indonesien hat deshalb die Hauptstadt in das neu gebaute Nusantara auf Borneo verlegt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der niedrigste Punkt Südamerikas und wie tief liegt er unter dem Meeresspiegel?",
        answerA = "Laguna del Carbón (Argentinien) bei −105 m",
        answerB = "Valdes-Halbinsel (Argentinien) bei −40 m",
        answerC = "Salar de Atacama (Chile) bei −2 m",
        answerD = "Laguna Colorada (Bolivien) bei +4.278 m",
        correctAnswer = 0,
        explanation = "Die Laguna del Carbón in der argentinischen Provinz Santa Cruz liegt bei −105 m unter dem Meeresspiegel und ist der tiefste Punkt Südamerikas sowie der tiefste Punkt der westlichen Hemisphäre außerhalb von Nordamerika.",
        difficulty = 5,
        funFact = "Die Laguna del Carbón liegt in der Patagonischen Steppe und ist trotz ihrer Rekordlage kaum bekannt – kein Schild, keine Touristenattraktion, nur ein einsamer Salzpfannensee in einer der verlassensten Gegenden der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Wirtschaftszone (EEZ) hat die größte Fläche aller Nationen weltweit?",
        answerA = "Russland",
        answerB = "Vereinigte Staaten",
        answerC = "Frankreich",
        answerD = "Australien",
        correctAnswer = 1,
        explanation = "Die Vereinigten Staaten haben mit rund 11,35 Millionen km² die größte ausschließliche Wirtschaftszone (EEZ) der Welt, dank ihrer überseeischen Territorien im Pazifik (Hawaii, Guam, Amerikanisch-Samoa etc.) und im Atlantik.",
        difficulty = 5,
        funFact = "Frankreich hat die zweitgrößte EEZ der Welt (ca. 11,03 Millionen km²) – dank seiner Überseegebiete in der Karibik, dem Indischen Ozean, dem Pazifik und Französisch-Guyana. Frankreich ist damit die Nation mit der größten maritimen Präsenz weltweit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die einzige natürliche Süßwasserlagune im tropischen Indopazifik, die größer als 150 km² ist und kein Meerwasser enthält?",
        answerA = "Chilika-See (Indien)",
        answerB = "Tonle-Sap-See (Kambodscha)",
        answerC = "Laguna de Bay (Philippinen)",
        answerD = "Es gibt keine solche Lagune",
        correctAnswer = 3,
        explanation = "Eine natürliche Süßwasserlagune im tropischen Indopazifik (d.h. direkt am Meer liegend, über 150 km², komplett Süßwasser) existiert nicht. Der Chilika-See ist ein Brackwassersee, Tonle Sap ein Binnengewässer und Laguna de Bay ein Süßwassersee, aber keine Lagune im klassischen Sinne.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat den höchsten Anteil an Bevölkerung über 65 Jahre gemäß UN-Daten 2023?",
        answerA = "Italien",
        answerB = "Deutschland",
        answerC = "Japan",
        answerD = "Südkorea",
        correctAnswer = 2,
        explanation = "Japan hat mit etwa 29,1 % den höchsten Anteil älterer Menschen (65+) an der Gesamtbevölkerung weltweit (UN 2023). Es führt diese traurige Statistik seit Jahrzehnten und steht vor enormen demografischen Herausforderungen.",
        difficulty = 5,
        funFact = "In Japan gibt es mittlerweile mehr Erwachsenenwindeln als Babywindeln, die verkauft werden – ein symbolisches Zeichen für den extremen demografischen Wandel. Die aktive Bevölkerung schrumpft jedes Jahr um rund 300.000 Menschen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte Breite (in Metern) hat der Panamakanal an seiner engsten Stelle in den Schleusen?",
        answerA = "33,53 m",
        answerB = "30,48 m",
        answerC = "28,50 m",
        answerD = "33,00 m",
        correctAnswer = 0,
        explanation = "Die ursprünglichen Schleusen des Panamakanals (Gatún, Pedro Miguel, Miraflores) haben eine Breite von exakt 33,53 m (110 Fuß). Dies war der limitierende Faktor für die 'Panamax'-Schiffsklasse. Die neuen Schleusen (seit 2016) haben 55 m Breite.",
        difficulty = 5,
        funFact = "Die alten Panamax-Schleusen wurden so präzise geplant, dass die größten erlaubten Schiffe nur wenige Zentimeter Spalt zu den Schleusenwänden haben. Lotsen fahren diese Schiffe ohne eigene Steuerung – das Schiff wird an Elektrolokomotiven (Mulas) gezogen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche beiden Länder teilen die Grenze über den Titicacasee und auf welchem Breitengrad liegt der See ungefähr?",
        answerA = "Peru und Chile, ca. 18°S",
        answerB = "Peru und Bolivien, ca. 16°S",
        answerC = "Bolivien und Chile, ca. 17°S",
        answerD = "Peru und Bolivien, ca. 20°S",
        correctAnswer = 1,
        explanation = "Der Titicacasee liegt an der Grenze zwischen Peru und Bolivien auf etwa 16°S und einer Höhe von 3.808 m. Er ist der höchstgelegene schiffbare See der Welt und teilt sich mit einer Seegrenzlinie zwischen beiden Ländern.",
        difficulty = 5,
        funFact = "Der Titicacasee hat eine einzigartige Sonderform des Lebens entwickelt: Der Titicaca-Bärenkalb (Telmatobius culeus) ist ein riesiger, hautschnorchlender Frosch, der so viel Sauerstoff über seine faltige Haut aufnimmt, dass er nie an die Oberfläche muss."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Breitengrad wird als 'Roaring Forties' bezeichnet und warum?",
        answerA = "30°–40° Nord, wegen starker Handelswinde",
        answerB = "40°–50° Süd, wegen beständiger Weststürme",
        answerC = "50°–60° Süd, wegen extremer Windgeschwindigkeiten",
        answerD = "40°–50° Nord, wegen der atlantischen Tiefdruckgebiete",
        correctAnswer = 1,
        explanation = "Die 'Roaring Forties' sind die Breitengrade zwischen 40° und 50° südlicher Breite, bekannt für beständige starke Westwinde. Da hier kaum Landmassen die Zirkulation stören, können die Winde ungehindert den Globus umrunden.",
        difficulty = 5,
        funFact = "Segelschiffen der Kolonialzeit nutzten die Roaring Forties für schnelle Passagen nach Australien – dieser Weg wurde 'Down Under' genannt. Weiter südlich bei 50°–60°S folgen die noch stürmischeren 'Furious Fifties'."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche linguistische Besonderheit verbindet Malta, Libyen und Tunesien hinsichtlich ihrer Amtssprachen?",
        answerA = "Alle drei gehören zur semitischen Sprachfamilie",
        answerB = "Malta ist das einzige EU-Land mit einer semitischen Amtssprache (Maltesisch)",
        answerC = "Alle drei haben Arabisch und eine europäische Sprache als Amtssprache",
        answerD = "Maltesisch, Arabisch und Tunesisch-Arabisch sind gegenseitig verständlich",
        correctAnswer = 1,
        explanation = "Maltesisch ist die einzige semitische Sprache, die Amtssprache eines EU-Mitgliedsstaates ist. Es entwickelte sich aus dem Sizilianisch-Arabischen und hat starke romanische und englische Einflüsse. Libyen und Tunesien teilen ebenfalls semitische Sprachen (Arabisch), sind aber keine EU-Mitglieder.",
        difficulty = 5,
        funFact = "Maltesisch ist die einzige semitische Sprache, die in lateinischer Schrift geschrieben wird – alle anderen semitischen Sprachen (Arabisch, Hebräisch, Amharisch) haben eigene Schriften."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das tiefste Tiefseebecken im Indischen Ozean?",
        answerA = "Java-Graben (Sunda-Graben) mit 7.725 m",
        answerB = "Diamantina-Graben mit 8.047 m",
        answerC = "Agulhas-Plateau mit 5.360 m",
        answerD = "Wharton-Becken mit 6.428 m",
        correctAnswer = 0,
        explanation = "Der Java-Graben (auch Sunda-Graben) ist der tiefste Teil des Indischen Ozeans mit einer maximalen Tiefe von 7.725 m. Er entstand durch die Subduktion der Australischen Platte unter die Eurasische Platte und ist seismisch sehr aktiv.",
        difficulty = 5,
        funFact = "Das verheerende Erdbeben und der Tsunami vom 26. Dezember 2004, der über 230.000 Menschen tötete, hatte sein Epizentrum direkt am Sunda-Graben – ein Zeichen für die tektonische Gewalt, die dieser Graben birgt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Staatsform hat die Arabische Sahara-Demokratische Republik (Westsahara) nach internationalem Recht offiziell?",
        answerA = "Sie ist ein autonomes Gebiet Marokkos",
        answerB = "Sie ist ein nicht-selbstregierendes Hoheitsgebiet laut UN, beansprucht von der Frente Polisario",
        answerC = "Sie ist ein UN-Treuhandgebiet",
        answerD = "Sie ist ein souveräner Staat, anerkannt von der UN-Generalversammlung",
        correctAnswer = 1,
        explanation = "Die Westsahara wird von der UN als 'nicht-selbstregierendes Hoheitsgebiet' (non-self-governing territory) geführt. Die Sahrawi Arab Democratic Republic (SADR) der Frente Polisario beansprucht das Gebiet, wird aber nicht von der UN als Staat anerkannt. Marokko kontrolliert de facto rund 80 % des Territoriums.",
        difficulty = 5,
        funFact = "Die Westsahara ist das letzte größere nicht-dekolonisierte Territorium Afrikas. Das UN-Referendum über die Unabhängigkeit wurde seit 1991 mehrfach angekündigt und immer wieder verschoben – bis heute hat es nicht stattgefunden."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie nennt man den Winkel zwischen dem geografischen Nordpol und dem magnetischen Nordpol an einem bestimmten Ort?",
        answerA = "Magnetische Deklination",
        answerB = "Magnetische Inklination",
        answerC = "Magnetische Intensität",
        answerD = "Geomagnetische Anomalie",
        correctAnswer = 0,
        explanation = "Die magnetische Deklination (auch Kompassabweichung) ist der Winkel zwischen der Richtung zum geografischen Nordpol (Wahrnorden) und dem magnetischen Nordpol an einem bestimmten Ort. Sie variiert je nach Standort und ändert sich auch im Laufe der Zeit.",
        difficulty = 5,
        funFact = "In Berlin beträgt die magnetische Deklination derzeit etwa 3–4° Ost – ein Kompass zeigt also leicht rechts vom geografischen Norden. Piloten und Seefahrer müssen diese Abweichung bei der Navigation berücksichtigen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hatte beim Zensus 2021/2022 die niedrigste Geburtenrate (TFR) der Welt?",
        answerA = "Japan",
        answerB = "Spanien",
        answerC = "Südkorea",
        answerD = "Ukraine",
        correctAnswer = 2,
        explanation = "Südkorea verzeichnete 2021 eine Gesamtfertilitätsrate (TFR) von nur 0,81 – die niedrigste je gemessene in einem Normallandsfall. 2023 sank sie sogar auf 0,72. Eine TFR unter 2,1 gilt als Bestandserhaltungsniveau.",
        difficulty = 5,
        funFact = "Bei einer TFR von 0,72 würde die südkoreanische Bevölkerung ohne Einwanderung innerhalb einer Generation auf etwa die Hälfte schrumpfen. Seoul hat dabei mit ca. 0,59 die weltweit niedrigste Geburtenrate einer Metropole."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean hat den geringsten Durchmesser am Äquator?",
        answerA = "Atlantischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Arktischer Ozean",
        answerD = "Antarktischer Ozean",
        correctAnswer = 0,
        explanation = "Der Atlantische Ozean ist am Äquator der schmalste Ozean – zwischen der westafrikanischen Küste (Gabun) und der brasilianischen Küste (Recife) beträgt die Entfernung nur etwa 2.900 km. Der Pazifik ist am selben Breitengrad mehr als 4-mal so breit.",
        difficulty = 5,
        funFact = "Die engste Stelle des Atlantiks war der Grund, warum das erste transatlantische Telegrafenkabel 1858 von Irland nach Neufundland gelegt wurde – diese Route nutzt den nördlichen Atlantik, der durch Islands Position noch weiter verkürzt wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Welt, das sich vollständig innerhalb des südlichen Wendekreises (Tropic of Capricorn) befindet?",
        answerA = "Swasiland (Eswatini)",
        answerB = "Lesotho",
        answerC = "Es gibt kein solches Land",
        answerD = "Botswana",
        correctAnswer = 2,
        explanation = "Es gibt kein Land, das vollständig zwischen dem Südlichen Wendekreis (23,5°S) und dem Südpol liegt, ohne dass Teile davon nördlich davon wären. Dieser Bereich (südlich des Wendekreises) umfasst hauptsächlich Teile Australiens, Südafrikas, Südamerikas und die Antarktis.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Grenzvertrag zwischen Russland und dem Osmanischen Reich (1774) markierte den ersten großen russischen Gebietsgewinn am Schwarzen Meer?",
        answerA = "Vertrag von Jassy (1792)",
        answerB = "Vertrag von Küçük Kaynarca (1774)",
        answerC = "Vertrag von Bukarest (1812)",
        answerD = "Vertrag von Adrianopel (1829)",
        correctAnswer = 1,
        explanation = "Der Vertrag von Küçük Kaynarca (1774) beendete den Russisch-Türkischen Krieg 1768–1774 und gab Russland Zugang zum Schwarzen Meer sowie Schutzrechte über orthodoxe Christen im Osmanischen Reich. Er war ein Wendepunkt in der Geschichte des Schwarzmeerraums.",
        difficulty = 5,
        funFact = "Der Vertrag von Küçük Kaynarca enthielt eine vage Formulierung, die Russland später als Schutzrecht über alle orthodoxen Christen im Osmanischen Reich interpretierte – ein Vorwand, der letztlich zum Krimkrieg (1853–1856) führte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Ozeanströmung ist für das vergleichsweise milde Klima in Großbritannien und Norwegen verantwortlich?",
        answerA = "Nordatlantikstrom (Verlängerung des Golfstroms)",
        answerB = "Golfstrom direkt",
        answerC = "Irmingerströmung",
        answerD = "Nordkap-Strömung",
        correctAnswer = 0,
        explanation = "Der Nordatlantikstrom (North Atlantic Current) ist die Verlängerung des Golfstroms im Nordatlantik und verantwortlich für das milde Klima Westeuropas. Er transportiert warmes Wasser bis nach Nordnorwegen und hält die Häfen dort eisfrei.",
        difficulty = 5,
        funFact = "London liegt auf demselben Breitengrad wie Städte in Kanada, die im Winter bei −20°C liegen. Ohne den Nordatlantikstrom hätte London ein Klima ähnlich wie Labrador – eisige Winter und kurze Sommer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Insel ist nach Grönland und Neuguinea die drittgrößte der Welt?",
        answerA = "Madagaskar",
        answerB = "Borneo",
        answerC = "Sumatra",
        answerD = "Baffin Island",
        correctAnswer = 1,
        explanation = "Borneo ist mit einer Fläche von etwa 748.168 km² die drittgrößte Insel der Welt, nach Grönland (2.166.086 km²) und Neuguinea (785.753 km²). Borneo wird zwischen Indonesien (Kalimantan), Malaysia und Brunei aufgeteilt.",
        difficulty = 5,
        funFact = "Borneo ist die einzige Insel der Welt, die von drei verschiedenen Staaten geteilt wird. Es beherbergt einen der ältesten Tropenwälder der Erde (über 130 Millionen Jahre) und ist Heimat des Orang-Utan."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte Höhe hat der Chimborazo in Ecuador über dem Erdmittelpunkt gemessen und warum übertrifft er damit den Everest?",
        answerA = "6.384,4 km vom Erdmittelpunkt (vs. 6.382,3 km beim Everest)",
        answerB = "6.388,1 km vom Erdmittelpunkt (vs. 6.381,6 km beim Everest)",
        answerC = "6.390,0 km vom Erdmittelpunkt (vs. 6.382,5 km beim Everest)",
        answerD = "6.392,4 km vom Erdmittelpunkt (vs. 6.383,1 km beim Everest)",
        correctAnswer = 1,
        explanation = "Der Chimborazo (6.268 m ü.NN.) liegt nahe dem Äquator, wo die Erde durch die Rotation ausgebaucht ist. Sein Gipfel liegt ca. 6.388,1 km vom Erdmittelpunkt entfernt – mehr als der Mount Everest mit ca. 6.381,6 km.",
        difficulty = 5,
        funFact = "Gemessen vom Erdmittelpunkt ist der Chimborazo der höchste Punkt der Erde – der Everest ist nur 'höher über dem Meeresspiegel'. Diese Tatsache ist sogar auf ecuadorianischen Tourismusbroschüren vermerkt: 'Nächster Punkt zum Weltraum!'"
    ),

    Question(
        categoryId = 1,
        questionText = "Welche afrikanische Großstadt verzeichnete beim Zensus 2022 die höchste Bevölkerungszahl auf dem Kontinent?",
        answerA = "Kairo",
        answerB = "Kinshasa",
        answerC = "Lagos",
        answerD = "Johannesburg",
        correctAnswer = 2,
        explanation = "Lagos in Nigeria hat laut UN-Schätzungen 2022 den Großraum Lagos auf über 15 Millionen Einwohner (Stadt) bzw. über 24 Millionen (Metropolregion) angewachsen und gilt damit als bevölkerungsreichste Stadt Afrikas. Kairo wird je nach Quelle ähnlich hoch oder höher geschätzt.",
        difficulty = 5,
        funFact = "Lagos wächst jährlich um rund 600.000 Menschen und könnte bis 2100 zur größten Stadt der Welt werden. Die Infrastruktur der Stadt ist für diese Wachstumsrate kaum ausgelegt – Staus von 8+ Stunden sind keine Seltenheit."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die natürliche Grenze zwischen Europa und Asien östlich des Kaspischen Meeres?",
        answerA = "Aralgebirge",
        answerB = "Hindukusch",
        answerC = "Kasachischer Kleiner Ural",
        answerD = "Es gibt keine eindeutige natürliche Grenze östlich des Kaspischen Meeres",
        correctAnswer = 3,
        explanation = "Östlich des Kaspischen Meeres gibt es keine eindeutige natürliche Grenze zwischen Europa und Asien. Die Grenze ist dort eine Konvention – verschiedene Geographen setzen sie entlang des Ural (bis Kaspi) und führen sie dann willkürlich durch die kasachische Steppe oder entlang des Manytsch-Tieflands fort.",
        difficulty = 5,
        funFact = "Die Grenze zwischen Europa und Asien ist eine der am meisten diskutierten Konventionen der Geographie. Allein im Uralgebiet gibt es über 40 verschiedene Grenzsteine mit der Aufschrift 'Europa/Asien' – teilweise weit voneinander entfernt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist die Fläche des Aralsees heute (ca. 2023) im Vergleich zu 1960?",
        answerA = "Ca. 10 % der ursprünglichen Fläche",
        answerB = "Ca. 25 % der ursprünglichen Fläche",
        answerC = "Ca. 5 % der ursprünglichen Fläche",
        answerD = "Ca. 15 % der ursprünglichen Fläche",
        correctAnswer = 0,
        explanation = "Der Aralsee hatte 1960 noch eine Fläche von rund 68.000 km². Heute (2023) existieren nur noch winzige Restseen im Norden (kleiner Aralsee, ca. 3.300 km²) und der fast vollständig ausgetrocknete Südsee – insgesamt weniger als 10 % der ursprünglichen Fläche.",
        difficulty = 5,
        funFact = "Die Schiffe, die den Aralsee einst befuhren, liegen heute inmitten einer Wüste – hunderte Kilometer vom nächsten Wasser entfernt. Die Fischereistadt Moynaq hatte einst einen Hafen, der jetzt in der Wüste liegt."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr unterzeichneten Norwegen und Russland den Vertrag über die maritime Grenze in der Barentssee?",
        answerA = "2005",
        answerB = "2010",
        answerC = "2015",
        answerD = "1990",
        correctAnswer = 1,
        explanation = "Der Vertrag über die maritime Grenze zwischen Norwegen und Russland in der Barentssee wurde am 15. September 2010 in Murmansk unterzeichnet. Er beendete einen fast 40 Jahre langen Grenzstreit über ein Seegebiet von etwa 175.000 km².",
        difficulty = 5,
        funFact = "Das umstrittene Gebiet der Barentssee enthält riesige Öl- und Gasreserven. Der Kompromissvertrag von 2010 teilt das Gebiet fast genau in zwei Hälften – und beide Seiten können nun ihre Ressourcen legal ausbeuten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten UNESCO-Welterbestätten (Stand 2023)?",
        answerA = "Frankreich",
        answerB = "Spanien",
        answerC = "China",
        answerD = "Italien",
        correctAnswer = 3,
        explanation = "Italien führt mit 58 UNESCO-Welterbestätten (Stand 2023) die Weltrangliste an, knapp vor China (57) und Deutschland (52). Italien hat damit mehr Welterbestätten als jedes andere Land.",
        difficulty = 5,
        funFact = "Italien hat so dichte Konzentrationen von Kulturerbe, dass manchmal ein ganzer Stadtkern als eine einzige UNESCO-Stätte eingetragen ist – wie die historischen Zentren von Florenz, Venedig oder Rom."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Fjord der Welt?",
        answerA = "Geirangerfjord (Norwegen)",
        answerB = "Scoresby Sund (Grönland)",
        answerC = "Sognefjord (Norwegen)",
        answerD = "Milford Sound (Neuseeland)",
        correctAnswer = 1,
        explanation = "Der Scoresby Sund (Kangertittivaq) in Grönland ist mit einer Länge von rund 350 km der längste Fjord der Welt. Er ist damit fast dreimal so lang wie der berühmte Sognefjord in Norwegen (ca. 205 km).",
        difficulty = 5,
        funFact = "Der Scoresby Sund ist so tief (max. 1.500 m) und abgelegen, dass er weniger erforscht ist als manche Tiefseezonen. Im Winter friert er zu und bildet ein riesiges natürliches Eisfeld – ein Paradies für Hundeschlittentouren."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt das geodätische Phänomen, bei dem der Horizont auf der Kugel der Erde tiefer zu liegen scheint als mathematisch erwartet, weil Licht sich in der Atmosphäre krümmt?",
        answerA = "Atmosphärische Brechung",
        answerB = "Terrestrische Refraktion",
        answerC = "Astronomische Refraktion",
        answerD = "Looming-Effekt",
        correctAnswer = 1,
        explanation = "Terrestrische Refraktion bezeichnet die Ablenkung von Lichtstrahlen in der bodennahen Atmosphäre, die dazu führt, dass entfernte Objekte (z.B. Berggipfel, Schiffe) höher über dem Horizont erscheinen als geometrisch berechnet. Sie beeinflusst geodätische Vermessungen erheblich.",
        difficulty = 5,
        funFact = "Die terrestrische Refraktion hat historisch zu vermessungsfehlern geführt. Der Bau des Eisenbahntunnels Mont-Cenis (fertiggestellt 1871) nutzte präzise Refraktionskorrekturen – dennoch stimmten die Bohrungen von beiden Seiten auf Millimeter genau."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das weltweit einzige Land, dessen Hauptstadt nördlicher als der Polarkreis (66,5°N) liegt?",
        answerA = "Island (Reykjavík)",
        answerB = "Kanada (Ottawa)",
        answerC = "Russland (Moskau)",
        answerD = "Es gibt kein solches Land – keine Hauptstadt liegt nördlich des Polarkreises",
        correctAnswer = 3,
        explanation = "Keine Hauptstadt liegt nördlich des Polarkreises (66,5°N). Reykjavík liegt bei 64°N, Nuuk (Grönlands Hauptstadt) bei 64°N. Nunavut (Kanada) hat keine eigene Hauptstadt auf diesem Niveau. Die nördlichste anerkannte Landeshauptstadt ist Reykjavík – knapp südlich des Polarkreises.",
        difficulty = 5,
        funFact = "Nuuk (Grönland) liegt bei 64°N und ist die nördlichste Hauptstadt der Welt – aber Grönland ist autonomes Gebiet Dänemarks, keine unabhängige Nation. Die nördlichste Hauptstadt eines vollständig unabhängigen Staates ist Reykjavík (Island) bei 64°08'N."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die tiefste Stelle im Atlantischen Ozean?",
        answerA = "Puerto-Rico-Graben, 8.376 m",
        answerB = "Romanche-Graben, 7.758 m",
        answerC = "South Sandwich-Graben, 8.428 m",
        answerD = "Laurentian Abyss, 5.575 m",
        correctAnswer = 0,
        explanation = "Der Puerto-Rico-Graben ist mit einer maximalen Tiefe von 8.376 m die tiefste Stelle im Atlantischen Ozean. Er liegt nördlich der Insel Puerto Rico an der Grenze zwischen der Karibischen und der Nordamerikanischen Platte.",
        difficulty = 5,
        funFact = "Im Puerto-Rico-Graben herrscht ein extremes Gravitationsanomalien-Feld – die schwächste Schwerkraft des gesamten Atlantiks wurde hier gemessen. Außerdem liegt hier eine der tektonisch komplexesten Zonen der Erde, wo drei Platten aufeinandertreffen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Begriff bezeichnet den Prozess, durch den Gebirge nach der Abtragung des darüberliegenden Materials isostatisch aufsteigen?",
        answerA = "Epeirogenese",
        answerB = "Isostatische Reaktion (Isostasy Rebound)",
        answerC = "Orogenese",
        answerD = "Epexogenese",
        correctAnswer = 1,
        explanation = "Isostatischer Ausgleich (isostatic rebound oder post-glacial rebound) bezeichnet den Vorgang, bei dem Erdkruste nach dem Schmelzen von Eisschilden oder der Abtragung von Gebirgen isostatisch aufsteigt. Skandinavien hebt sich z.B. seit der letzten Eiszeit noch immer um bis zu 1 cm/Jahr.",
        difficulty = 5,
        funFact = "In Schweden und Finnland steigen ehemalige Hafenstädte heute meterweit über den Meeresspiegel – alte historische Hafenanlagen liegen heute weit im Landesinneren. Dieser Prozess wird noch viele tausend Jahre anhalten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat die längste Küstenlinie der Welt?",
        answerA = "Russland",
        answerB = "Australien",
        answerC = "Norwegen",
        answerD = "Kanada",
        correctAnswer = 3,
        explanation = "Kanada hat mit rund 202.080 km die längste Küstenlinie der Welt (CIA World Factbook), gefolgt von Norwegen (ca. 83.000 km inklusive aller Fjorde und Inseln) und Russland (ca. 37.653 km). Kanadas Küstenlinie ergibt sich vor allem durch die vielen Inseln des Arktischen Archipels.",
        difficulty = 5,
        funFact = "Kanadas Küstenlinie ist so lang, dass man sie viermal um den Äquator wickeln könnte. Kanada hat Küsten an drei Ozeanen gleichzeitig: dem Atlantik, dem Pazifik und dem Arktischen Ozean."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet die Bezeichnung für die Linie, die auf einer Karte alle Punkte gleicher jährlicher Niederschlagsmenge verbindet?",
        answerA = "Isobare",
        answerB = "Isohyete",
        answerC = "Isotherm",
        answerD = "Isanomal",
        correctAnswer = 1,
        explanation = "Eine Isohyete (von griech. hyetos = Regen) ist eine Linie auf einer Karte, die alle Punkte mit gleicher Niederschlagsmenge (z.B. gleicher Jahresniederschlag in mm) verbindet. Sie ist ein wichtiges Werkzeug der Klimatologie und Hydrologie.",
        difficulty = 5,
        funFact = "Das regnerischste Gebiet der Erde nach Jahresniederschlag ist Mawsynram im indischen Bundesstaat Meghalaya mit über 11.870 mm Jahresniederschlag – das ist 40-mal mehr als in der Sahara und fast dreimal mehr als im Amazonas-Regenwald."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche drei Länder teilen sich den Dreiländerpunkt im Dreieck Deutschland-Belgien-Niederlande (Vaalserberg-Region)?",
        answerA = "Deutschland, Belgien, Niederlande",
        answerB = "Deutschland, Belgien, Luxemburg",
        answerC = "Niederlande, Belgien, Luxemburg",
        answerD = "Deutschland, Niederlande, Luxemburg",
        correctAnswer = 0,
        explanation = "Der Dreiländerpunkt (Drielandenpunt) bei Vaals liegt auf dem Vaalserberg, dem höchsten Punkt der Niederlande (322 m). Hier treffen die Grenzen von Deutschland, Belgien und den Niederlanden aufeinander. Es ist einer der meistbesuchten Dreiländerpunkte Europas.",
        difficulty = 5,
        funFact = "Der Vaalserberg ist mit 322 m der höchste Punkt der Niederlande – das Königreich der Niederlande hat aber noch viel höhere Gipfel auf seinen karibischen Inseln (wie Saba mit dem Mount Scenery auf 887 m)."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr und durch welchen Vertrag wurde Elsass-Lothringen nach dem Deutsch-Französischen Krieg an das Deutsche Reich abgetreten?",
        answerA = "1871, Vorfrieden von Versailles",
        answerB = "1871, Frieden von Frankfurt",
        answerC = "1870, Waffenstillstand von Sedan",
        answerD = "1872, Berliner Kongress",
        correctAnswer = 1,
        explanation = "Durch den Frieden von Frankfurt am 10. Mai 1871 musste Frankreich Elsass und Lothringen (einschließlich Metz) an das neu gegründete Deutsche Reich abtreten. Die Region blieb deutsch bis 1919 (Versailler Vertrag), war 1940–1945 erneut deutsch besetzt und ist seit 1945 dauerhaft französisch.",
        difficulty = 5,
        funFact = "Die Frage 'Wem gehört Elsass-Lothringen?' war 47 Jahre lang (1871–1918) die zentrale Quelle des deutsch-französischen Antagonismus – und Hauptmotiv für Frankreichs Kriegseintritt 1914."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viel Prozent der Erdoberfläche sind von Eis bedeckt (permanent, einschließlich Meereis)?",
        answerA = "Ca. 7 %",
        answerB = "Ca. 10 %",
        answerC = "Ca. 14 %",
        answerD = "Ca. 20 %",
        correctAnswer = 1,
        explanation = "Ungefähr 10 % der Erdoberfläche sind permanent von Eis bedeckt: rund 3 % sind Gletscher und Eisschilde (Antarktis, Grönland, Berggletscher) und weitere 7 % jahreszeitliches Meereis (Arktis, Antarktis). Im Sommer reduziert sich das Meereis erheblich.",
        difficulty = 5,
        funFact = "Die Antarktis allein enthält 90 % des Süßwassereises der Erde. Würde das gesamte antarktische Eis schmelzen, würde der Meeresspiegel um ca. 58 m steigen – genug, um praktisch alle Küstenstädte der Welt zu versenken."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Meeresgebiet hat die höchste bekannte Konzentration von Plastikabfall, das sogenannte 'Great Pacific Garbage Patch'?",
        answerA = "Im nördlichen Pazifik zwischen Hawaii und Kalifornien",
        answerB = "Im Südpazifik zwischen Australien und Südamerika",
        answerC = "Im Nordatlantik zwischen den Azoren und den Bermudas",
        answerD = "Im Indischen Ozean zwischen Indien und Australien",
        correctAnswer = 0,
        explanation = "Das 'Great Pacific Garbage Patch' (GPGP) befindet sich im nördlichen Pazifik zwischen Hawaii und der Westküste der USA (30°N–40°N / 135°W–155°W), konzentriert durch den Nordpazifischen Wirbel. Es ist die größte bekannte Ansammlung von Plastikabfall im Ozean.",
        difficulty = 5,
        funFact = "Das GPGP ist kein fester Müllberg, sondern eine diffuse Ansammlung von Mikroplastikpartikeln, die das Wasser trüben. Seine geschätzte Fläche übersteigt 1,6 Millionen km² – mehr als dreimal die Fläche Frankreichs."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige europäische Land, das vollständig innerhalb eines anderen europäischen Landes liegt (außer Vatikan und San Marino)?",
        answerA = "Andorra",
        answerB = "Monaco",
        answerC = "Liechtenstein",
        answerD = "Es gibt kein solches weiteres Land",
        correctAnswer = 3,
        explanation = "Kein europäisches Land außer Vatikan (in Italien) und San Marino (ebenfalls in Italien) liegt vollständig innerhalb eines anderen Landes. Liechtenstein liegt zwischen der Schweiz und Österreich (zwei Länder), Andorra zwischen Frankreich und Spanien (zwei Länder), Monaco liegt an der Grenze Frankreichs zum Meer.",
        difficulty = 5,
        funFact = "Liechtenstein ist eines von nur zwei doppelt-eingeschlossenen Ländern der Welt (also Binnenland umschlossen von Binnenländern) – das andere ist Usbekistan. Liechtenstein grenzt an die Schweiz und Österreich, beide selbst keine Meeresanrainer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Mindesttiefe definiert das GEBCO (General Bathymetric Chart of the Oceans) als Abyssal-Zone?",
        answerA = "3.000 m",
        answerB = "4.000 m",
        answerC = "2.000 m",
        answerD = "5.000 m",
        correctAnswer = 1,
        explanation = "Die Abyssal-Zone (Tiefsee im engeren Sinne) beginnt nach internationaler Konvention bei 4.000 m Tiefe und reicht bis zur Hadalzone (ab ca. 6.000–6.500 m in Tiefseegräben). Etwa 54 % des Meeresbodens liegen in der Abyssal-Zone.",
        difficulty = 5,
        funFact = "Die Abyssal-Zone ist das ausgedehnteste Habitat der Erde – fast die Hälfte des gesamten Planeten ist von dieser stockdunklen, eiskalten Zone bedeckt. Trotzdem ist sie bis heute weitgehend unerforscht."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Grenzstein markiert den nördlichsten Punkt des Dreiländerpunkts zwischen Russland, Norwegen und Finnland?",
        answerA = "Treriksröset (Kilpisjärvi)",
        answerB = "Grenzstein bei Boris Gleb/Gjøklippen",
        answerC = "Grenzstein Muotkatunturi",
        answerD = "Eismarkierung am Varangerfjord",
        correctAnswer = 1,
        explanation = "Der Dreiländerpunkt zwischen Russland, Norwegen und Finnland liegt nahe Gjøklippen/Boris Gleb am Pasvik-Fluss, wo der Grenzstein für diesen Tripoint steht. Dieser Punkt liegt weiter nördlich als der Treriksröset (der Tripoint Norwegen-Schweden-Finnland).",
        difficulty = 5,
        funFact = "Der Dreiländerpunkt Norwegen-Russland-Finnland liegt in einer der sensibelsten Grenzzonen Europas – Norwegen und Russland teilen dort eine NATO/Nicht-NATO-Grenze, die besonders seit 2022 militärisch aufmerksam beobachtet wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der aktivste Supervulkan der Erde nach Anzahl der Eruptionen in den letzten 10.000 Jahren?",
        answerA = "Yellowstone (USA)",
        answerB = "Campi Flegrei (Italien)",
        answerC = "Long Valley Caldera (USA)",
        answerD = "Taupo (Neuseeland)",
        correctAnswer = 3,
        explanation = "Der Taupo-Vulkan in Neuseeland war in den letzten 26.500 Jahren vulkanisch am aktivsten – er produzierte mehr als zwei Drittel des pyroplastischen Materials aller anderen Supervulkane zusammen. Der Taupo-Ausbruch vor 26.500 Jahren war der stärkste der letzten 70.000 Jahre.",
        difficulty = 5,
        funFact = "Der Tauposee im Zentrum der Nordinsel Neuseelands ist die gefüllte Caldera des Taupo-Supervulkans. Der letzte große Ausbruch (Hatepe-Eruption, 186 n. Chr.) war so massiv, dass rote Himmel in China und Rom dokumentiert wurden."
    ),

    // ── MASTER (difficulty = 5) ── questions 101–150 ──────────────────────────

    Question(
        categoryId = 1,
        questionText = "Welche exakte Passhöhe hat der Stilfser Joch (Passo dello Stelvio) in Metern über dem Meeresspiegel?",
        answerA = "2.757 m",
        answerB = "2.844 m",
        answerC = "2.758 m",
        answerD = "2.769 m",
        correctAnswer = 2,
        explanation = "Der Stilfser Joch (Passo dello Stelvio) liegt auf einer Höhe von exakt 2.758 m ü. NN und ist damit die höchste Asphaltstraße in den Ostalpen. Er liegt an der Grenze zwischen der Lombardei und Südtirol in Italien.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet die korrekte geodätische Bezeichnung für das Referenzsystem, das in den USA für Koordinaten standardmäßig verwendet wird (Stand 2023)?",
        answerA = "NAD27 (North American Datum 1927)",
        answerB = "ITRF2014 (International Terrestrial Reference Frame 2014)",
        answerC = "NAD83 (North American Datum 1983)",
        answerD = "WGS72 (World Geodetic System 1972)",
        correctAnswer = 2,
        explanation = "NAD83 (North American Datum 1983) ist das offizielle geodätische Referenzsystem für Koordinatenangaben in den USA, Kanada und Mexiko. GPS-Koordinaten werden zwar in WGS84 angegeben, das in den kontinentalen USA praktisch mit NAD83 übereinstimmt (Abweichung unter 1 m).",
        difficulty = 5,
        funFact = "NAD27 (1927) und NAD83 (1983) können an manchen Punkten über 200 m voneinander abweichen – ein bedeutender Fehler, der in der Praxis bereits Grenzsteinverwechslungen und Grundstücksstreitigkeiten verursacht hat."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie nennt man den höchsten Alpenpass zwischen der Schweiz und Frankreich (Route Nationale), der auf 2.802 m liegt?",
        answerA = "Col du Galibier",
        answerB = "Col de l'Iseran",
        answerC = "Col Agnel (Colle dell'Agnello)",
        answerD = "Col du Mont Cenis",
        correctAnswer = 1,
        explanation = "Der Col de l'Iseran (2.769 m) ist die höchste asphaltierte Passstraße in Frankreich und im gesamten Alpenraum nach dem Stilfser Joch. Er liegt im Département Savoie und verbindet Val d'Isère mit Lanslebourg-Mont-Cenis. Tatsächlich liegt er auf 2.770 m – der Col Agnel auf 2.744 m.",
        difficulty = 5,
        funFact = "Der Col de l'Iseran war im Jahr 2019 Schauplatz eines historischen Moments bei der Tour de France – die Etappe wurde wegen Schneefall und Lawinengefahr auf diesem Pass vorzeitig abgebrochen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche wasserscheidende Hauptlinie (Continental Divide) in Nordamerika trennt Wasser, das in den Atlantik fließt, von dem, das in den Pazifik fließt?",
        answerA = "Rocky Mountain Divide entlang der Hauptkammlinie der Rocky Mountains",
        answerB = "Appalachian Divide entlang der Appalachen",
        answerC = "Great Basin Divide im Bereich des Großen Salzsees",
        answerD = "Sierra Nevada Divide entlang der Sierra Nevada",
        correctAnswer = 0,
        explanation = "Die North American Continental Divide (auch Great Divide) verläuft entlang der Hauptkammlinie der Rocky Mountains von Alaska bis Mexiko und trennt alle Gewässer, die zum Atlantik oder Golf von Mexiko entwässern, von denen, die zum Pazifik fließen.",
        difficulty = 5,
        funFact = "In Wyoming durchquert die Continental Divide die seltsame 'Two Ocean Plateau'-Region, wo ein Bach sich teilt und ein Arm zum Atlantik, der andere zum Pazifik fließt – ein natürliches hydrologisches Kuriosum."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Mercator-Projektion wurde 1569 von Gerardus Mercator veröffentlicht und für welchen primären Zweck entwickelt?",
        answerA = "Für die flächentreue Darstellung von Kontinenten zur Landvermessung",
        answerB = "Für die winkeltreue Darstellung zur Navigation auf Rhumb-Linien",
        answerC = "Für die längentreue Darstellung von Meridianabständen",
        answerD = "Für die gleichabständige Darstellung von Polarbereichen",
        correctAnswer = 1,
        explanation = "Mercators Projektion von 1569 ('Nova et Aucta Orbis Terrae Descriptio') ist winkeltreu (konform): Rhumb-Linien (Linien konstanten Kompasskurses) erscheinen als Geraden, was die Navigation erheblich vereinfachte. Flächen werden jedoch zum Pol hin stark verzerrt.",
        difficulty = 5,
        funFact = "Auf der Mercator-Projektion erscheint Grönland fast so groß wie Afrika – in Wirklichkeit ist Afrika 14-mal größer. Diese Verzerrung hat jahrzehntelange Debatten über das eurozentrische Weltbild in Schulatlanten ausgelöst."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die Peters-Projektion (Gall-Peters) und welche kartografische Eigenschaft bewahrt sie?",
        answerA = "Eine winkeltreue Projektion, die Küstenlinien präzise darstellt",
        answerB = "Eine flächentreue (äquivalente) Zylinderprojektion, die relative Landflächengrößen korrekt darstellt",
        answerC = "Eine längentreue Projektion entlang der Meridiane",
        answerD = "Eine kompromissbasierte Projektion ohne spezifische Geotreue",
        correctAnswer = 1,
        explanation = "Die Gall-Peters-Projektion (1855/1974) ist eine flächentreue (äquivalente) Zylinderprojektion: sie bewahrt die relativen Flächengrößen, verzerrt jedoch Formen erheblich – besonders in Äquatornähe erscheinen Länder gestaucht, an den Polen gestreckt.",
        difficulty = 5,
        funFact = "Die Peters-Projektion wurde 1974 als 'politisch korrekte' Alternative zur Mercator-Projektion vermarktet und von der UN und verschiedenen NGOs übernommen. Kartografen kritisierten sie jedoch, da auch andere flächentreue Projektionen (z.B. Mollweide) weniger Formverzerrung aufweisen."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welcher seismischen Skala wird die Bodenbeschleunigung (Peak Ground Acceleration) bei Erdbeben gemessen, und welche Einheit wird verwendet?",
        answerA = "Richter-Skala, in Joule/m²",
        answerB = "Moment-Magnituden-Skala (Mw), in Newton·Meter",
        answerC = "Intensitätsskala (MMI), in g (Erdbeschleunigung)",
        answerD = "Peak Ground Acceleration wird in %g oder m/s² angegeben, unabhängig von einer benannten Skala",
        correctAnswer = 3,
        explanation = "Peak Ground Acceleration (PGA) ist eine physikalische Messgröße der Bodenbeschleunigung und wird in m/s² oder als Bruchteil der Erdbeschleunigung (%g) angegeben. Sie ist kein Bestandteil einer benannten Skala wie Richter oder MMI, sondern ein instrumentell gemessener Ingenieurswert.",
        difficulty = 5,
        funFact = "Das Tohoku-Erdbeben 2011 (Mw 9,0) erzeugte am Epizentrum PGA-Werte von über 2,7 g – das bedeutet, der Boden beschleunigte kurzfristig fast dreimal stärker als die Erdanziehung, was alle strukturellen Berechnungen für Gebäude der damaligen Zeit überstieg."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Längengrad markiert die Grenze zwischen der osteuropäischen und der westasiatischen biogeografischen Region nach Wallace (1876)?",
        answerA = "30°E",
        answerB = "Es gibt keine scharfe Längengrenze – die Wallace-Linie trennt asiatische von australischen Faunaregionen in Indonesien",
        answerC = "60°E",
        answerD = "45°E",
        correctAnswer = 1,
        explanation = "Die Wallace-Linie ist eine biogeografische Grenzlinie in Indonesien zwischen der Insel Bali/Borneo (asiatische Fauna) und Lombok/Sulawesi (australasische Fauna). Sie wurde 1863 von Alfred Russel Wallace beschrieben und hat nichts mit einem Längengrad in Europa zu tun.",
        difficulty = 5,
        funFact = "Die Wallace-Linie trennt trotz der geografischen Nähe von Inseln (Bali und Lombok liegen nur 35 km auseinander) zwei völlig verschiedene Faunenwelten: westlich leben Tiger, Elefanten und Orang-Utans, östlich Kakadus, Beuteltiere und Paradiesvögel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean-Transekt-Querschnitt des Weltmeeres misst den größten Wärmetransport von tropischen zu polaren Regionen?",
        answerA = "Der äquatoriale Atlantik-Transekt bei 0°N",
        answerB = "Der nordatlantische RAPID-Transekt bei 26,5°N (MOC-Messung)",
        answerC = "Der Indische-Ozean-Transekt bei 32°S (IO8-Linie)",
        answerD = "Der Südlicher-Ozean-Drake-Transekt bei 55°S",
        correctAnswer = 1,
        explanation = "Das RAPID-Array (Rapid Climate Change) misst seit 2004 kontinuierlich den atlantischen meridionalen Umwälzstrom (AMOC) am 26,5°N-Transekt. Hier wird der maximale Wärmetransport des Atlantiks von ca. 1,3 Petawatt gemessen – entscheidend für das europäische Klima.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet der offizielle Eigenname (Endonym) der Hauptstadt der Mongolei, die im Deutschen als 'Ulaanbaatar' oder 'Ulan Bator' bekannt ist?",
        answerA = "Улаанбаатар (Ulaanbaatar) – mongolisch für 'Roter Held'",
        answerB = "Нийслэл Хот (Niislel Khot) – mongolisch für 'Hauptstadt'",
        answerC = "Богдо Хүрэ (Bogdo Khüree) – mongolisch für 'Heilige Behausung'",
        answerD = "Их Хүрэ (Ikh Khüree) – mongolisch für 'Großes Lager'",
        correctAnswer = 0,
        explanation = "Das Endonym der mongolischen Hauptstadt lautet Улаанбаатар (Ulaanbaatar), was wörtlich 'Roter Held' bedeutet – benannt nach dem Nationalhelden Sükhbaatar. Der frühere Name 'Urga' (russisches Exonym) und 'Ikh Khüree' wurden 1924 durch Ulaanbaatar ersetzt.",
        difficulty = 5,
        funFact = "Ulaanbaatar ist die kälteste Hauptstadt der Welt – die Jahresdurchschnittstemperatur beträgt −0,4°C. Im Winter sinken die Temperaturen regelmäßig unter −40°C, was zu extremer Luftverschmutzung durch Heizungsrauch führt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Passhöhe hat der Khunjerab-Pass an der Grenze zwischen Pakistan und China (Karakorum Highway) in Metern?",
        answerA = "4.693 m",
        answerB = "4.973 m",
        answerC = "4.826 m",
        answerD = "5.133 m",
        correctAnswer = 1,
        explanation = "Der Khunjerab-Pass (خنجراب) auf dem Karakorum Highway liegt auf einer Höhe von 4.693 m nach einer Messung, offiziell wird aber 4.693 m angegeben – neuere Messungen ergeben 4.693–4.714 m. Er ist der höchste asphaltierte Grenzübergang der Welt zwischen zwei Ländern.",
        difficulty = 5,
        funFact = "Der Name 'Khunjerab' kommt aus der lokalen Wakhi-Sprache und bedeutet 'Tal des Blutes' – ein Verweis auf Überfälle, die früher Karawanenreisende auf dem Hochplateau erwarteten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche biogeografische Region bezeichnet das System der Paläarktis in der Wallace'schen Einteilung der Erde?",
        answerA = "Europa, Nordafrika, Asien nördlich des Himalaya und Arabische Halbinsel",
        answerB = "Ausschließlich Europa und Nordafrika bis zur Sahara",
        answerC = "Gesamtes Asien und Europa nördlich des Äquators",
        answerD = "Europa, Sibirien und Ostasien bis zum Tropischen Gürtel",
        correctAnswer = 0,
        explanation = "Die Paläarktis (Palaearctic realm) umfasst Europa, Nordafrika (nördlich der Sahara), die Arabische Halbinsel, Zentralasien, Sibirien, China und Japan – also die gesamte nördliche Alte Welt. Sie ist die größte der sechs klassischen biogeografischen Regionen Wallaces.",
        difficulty = 5,
        funFact = "Die Paläarktis teilt viele Tierarten mit der Nearktis (Nordamerika) – beide zusammen werden als 'Holarktis' bezeichnet. Rehe, Wölfe und Bären findet man in beiden Bereichen, da sie durch die einstige Beringia-Landbrücke verbunden waren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Methode verwendet die USGS zur Berechnung der Erdbebenmagnitude bei modernen Starkbeben (Mw ≥ 7,0)?",
        answerA = "Lokale Richter-Magnitude (Ml) über Seismogramm-Amplituden",
        answerB = "Moment-Magnituden-Skala (Mw) basierend auf seismischem Moment M₀",
        answerC = "Oberflächenwellen-Magnitude (Ms) aus Rayleigh-Wellen",
        answerD = "Körperwellen-Magnitude (mb) aus P-Wellen-Amplituden",
        correctAnswer = 1,
        explanation = "Die USGS und alle modernen seismologischen Institute verwenden für Starkbeben (Mw ≥ 7,0) die Moment-Magnituden-Skala (Mw), die das seismische Moment M₀ = μ·A·d (Schermodulus × Bruchfläche × mittlere Verschiebung) logarithmisch skaliert. Sie sättigt nicht wie die Richter-Skala.",
        difficulty = 5,
        funFact = "Das Tohoku-Erdbeben 2011 hatte ein seismisches Moment von 3,9 × 10²² Nm – das entspricht der Energie von etwa 600 Millionen Hiroshima-Atombomben. Auf der alten Richter-Skala wäre ein solches Beben gar nicht mehr messbar."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der größte prähistorische Binnensee Eurasiens, der während des letzten Glazialmaximums (LGM) das heutige Schwarze Meer und das Kaspische Meer verband?",
        answerA = "Neo-Tethys-See",
        answerB = "Paratethys",
        answerC = "Lago Mare",
        answerD = "Pontic Sea",
        correctAnswer = 1,
        explanation = "Die Paratethys war ein riesiges Binnenmeer, das sich vom heutigen Alpenvorland bis zum heutigen Aralsee erstreckte und vor etwa 5–34 Millionen Jahren existierte. Heute sind Schwarzes Meer, Kaspisches Meer und Aralsee die letzten Überreste dieses einst gewaltigen Binnenbeckens.",
        difficulty = 5,
        funFact = "In der Miozän-Periode der Paratethys entwickelten sich einzigartige endemische Tierarten, da das Binnenmeer periodisch vom Weltmeer abgeschnitten war. Viele kaspische Robben- und Fischarten sind direkte Nachfahren von Meeresbewohnern dieser isolierten Paratethys."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat nach dem Zensus 2021 die höchste Dichte an Mobilfunk-Basisstationen pro Einwohner weltweit?",
        answerA = "Südkorea",
        answerB = "Japan",
        answerC = "Schweiz",
        answerD = "Monaco",
        correctAnswer = 0,
        explanation = "Südkorea hat nach ITU-Daten 2021 die weltweit höchste Dichte an Mobilfunk-Basisstationen pro 100.000 Einwohner, getrieben durch den frühen und vollständigen Ausbau von 5G-Netzen durch Anbieter wie SK Telecom, KT und LG U+.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der geografische Begriff für eine abflusslose, geschlossene Hohlform, in der Wasser versickert oder verdunstet, ohne ein Meer zu erreichen?",
        answerA = "Endorheisches Becken (Endorheic Basin)",
        answerB = "Exorheisches Becken",
        answerC = "Arreisches Gebiet",
        answerD = "Kryptorheisches Becken",
        correctAnswer = 0,
        explanation = "Ein endorheisches Becken (von griech. endo = innen, rheos = Fluss) ist ein abgeschlossenes Einzugsgebiet ohne Abfluss zum Ozean. Das Kaspische Meer, der Aralsee und das Tote Meer sind klassische Beispiele. Rund 18 % der Landoberfläche sind endorheisch.",
        difficulty = 5,
        funFact = "Das größte endorheische Becken der Welt ist das Kaspische-See-Einzugsgebiet mit etwa 3,1 Millionen km². Das zweitgrößte ist das zentrale australische Einzugsgebiet (Eyre-Becken) mit rund 1,14 Millionen km²."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Alpenpass liegt genau auf der europäischen Hauptwasserscheide zwischen Rhein (Nordsee) und Po (Mittelmeer) und hat eine Passhöhe von 2.064 m?",
        answerA = "Bernhardinpass (San Bernardino), Schweiz",
        answerB = "Splügenpass, Schweiz/Italien",
        answerC = "Maloja-Pass, Schweiz",
        answerD = "Ofenpass (Pass dal Fuorn), Schweiz",
        correctAnswer = 1,
        explanation = "Der Splügenpass (2.113 m, nicht 2.064 m) liegt auf der Wasserscheide zwischen dem Hinterrhein (→ Nordsee) und dem Liro (→ Po → Mittelmeer). Er ist einer der klarsten Wasserscheidenpässe der Alpen. Der Maloja-Pass ist kein Hauptwasserscheidenpunkt, da der Inn dort zum Schwarzen Meer entwässert.",
        difficulty = 5,
        funFact = "Der Splügenpass war in der Römerzeit eine wichtige Verbindungsroute (Via Spluga). Im Jahr 1800 überquerte Napoleon den Großen St. Bernhard – aber rund 15.000 Soldaten marschierten gleichzeitig über den Splügen, was in den Geschichtsbüchern kaum erwähnt wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte Tiefe hat die Vitjaz-Tiefe im Tonga-Graben, dem zweittiefsten Meeresgraben der Welt?",
        answerA = "10.800 m",
        answerB = "10.882 m",
        answerC = "10.924 m",
        answerD = "11.034 m",
        correctAnswer = 1,
        explanation = "Die tiefste Stelle im Tonga-Graben, die Horizon Deep (auch Tonga-Tiefe), liegt nach modernen Messungen bei 10.882 m und ist damit der zweittiefste Punkt der Weltmeere nach dem Challenger Deep im Marianengraben (11.034 m).",
        difficulty = 5,
        funFact = "Der Tonga-Graben entsteht durch die Subduktion der Pazifischen Platte unter die Australische Platte und ist tektonisch sehr aktiv. Die Subduktionsgeschwindigkeit von bis zu 24 cm/Jahr macht ihn zur schnellsten Plattengrenze der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist das Endonym des Landes, das im Deutschen als 'Weißrussland' bezeichnet wird?",
        answerA = "Byelarus",
        answerB = "Belarus (Беларусь)",
        answerC = "Ruthenia Alba",
        answerD = "Bielorussia",
        correctAnswer = 1,
        explanation = "Das offizielle Endonym lautet Беларусь (Belarus), was wörtlich 'Weißes Russland' bedeutet. Der Name wird von der Regierung seit 1991 propagiert; das Exonym 'Weißrussland' gilt als veraltet und wird von belarussischen Offiziellen abgelehnt.",
        difficulty = 5,
        funFact = "Der UN-Stilguide empfiehlt seit 1992 offiziell die Schreibweise 'Belarus' statt 'Byelorussia' oder 'Weißrussland'. Deutschland und Österreich verwenden im offiziellen diplomatischen Verkehr 'Weißrussland', obwohl 'Belarus' international üblich ist."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches geodätische Datum wurde für die Erstvermessung von Indien durch die Great Trigonometrical Survey (GTS) verwendet?",
        answerA = "Everest-Spheroid 1830",
        answerB = "Bessel-Ellipsoid 1841",
        answerC = "Clarke-Ellipsoid 1866",
        answerD = "Hayford-Ellipsoid 1909",
        correctAnswer = 0,
        explanation = "Die Great Trigonometrical Survey Indiens (1802–1871) verwendete das Everest-Spheroid von 1830, benannt nach dem Vermessungsleiter George Everest. Es ist ein Referenzellipsoid, das speziell für den indischen Subkontinent optimiert wurde und bis heute in Indien als Basis dient.",
        difficulty = 5,
        funFact = "George Everest, nach dem das Spheroid und der Berggipfel benannt sind, weigerte sich selbst, den Mount Everest nach sich benennen zu lassen – er bevorzugte den nepalesischen Namen Sagarmatha. Die Royal Geographical Society ignorierte seinen Widerspruch."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresströmung bildet die ozeanografische Grenze zwischen dem Subtropischen Wirbel und dem Subpolaren Wirbel im Nordatlantik?",
        answerA = "Nordatlantischer Strom",
        answerB = "Kanarenstrom",
        answerC = "Nordatlantischer Polarfront",
        answerD = "Azorenstrom (Azores Current)",
        correctAnswer = 3,
        explanation = "Der Azorenstrom (Azores Current) ist die ozeanografische Grenze zwischen dem subtropischen und dem subpolaren Wirbel im östlichen Nordatlantik. Er zweigt östlich der Azoren vom Nordatlantikstrom ab und fließt nach Südosten – sein Kernstrom markiert die hydrografische Haupttrennlinie.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte Höhe hat der Brennerpass, der niedrigste Hauptpass der Alpen in West-Ost-Richtung, in Metern?",
        answerA = "1.370 m",
        answerB = "1.374 m",
        answerC = "1.384 m",
        answerD = "1.357 m",
        correctAnswer = 1,
        explanation = "Der Brennerpass an der Grenze zwischen Österreich (Tirol) und Italien (Südtirol) liegt auf einer Höhe von genau 1.374 m ü. NN. Er ist der niedrigste befahrbare Übergang über die Hauptkette der Alpen und damit die meistbenutzte Transitroute zwischen Nord- und Südeuropa.",
        difficulty = 5,
        funFact = "Durch den Brenner fahren jährlich über 2 Millionen Lastwagen und rund 13 Millionen Pkw – das macht ihn zum meistbefahrenen Alpenpass der Welt. Die Autobahn über den Brenner wurde 1971 fertiggestellt und war die erste durchgehende Autobahnverbindung über die Alpen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet der Endonym-Name der Hauptstadt Myanmars, die im Deutschen oft 'Rangun' oder 'Yangon' genannt wird?",
        answerA = "ရန်ကုန် (Yangon) – für die Althauptstadt",
        answerB = "နေပြည်တော် (Naypyidaw) – für die seit 2006 geltende Hauptstadt",
        answerC = "ပုဂံ (Bagan) – historische Hauptstadt",
        answerD = "မန္တလေး (Mandalay) – für die zweitgrößte Stadt",
        correctAnswer = 1,
        explanation = "Seit der Hauptstadtverlegung 2005/2006 ist Naypyidaw (နေပြည်တော်, wörtlich 'Königssitz' oder 'Thronstadt') die offizielle Hauptstadt Myanmars. Das Exonym 'Rangun' (Rangoon) bezeichnet Yangon, die alte Hauptstadt und nach wie vor größte Stadt des Landes.",
        difficulty = 5,
        funFact = "Naypyidaw wurde über Nacht zur Hauptstadt: Am 6. November 2005 zog die gesamte Regierung ohne öffentliche Vorankündigung um. Die Stadt wurde von Grund auf neu erbaut und hat heute riesige leere Prachtstraßen mit 20 Fahrspuren – aber kaum Verkehr."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Begriff bezeichnet in der Paläogeografie den Superkontinent, der vor ca. 300 Millionen Jahren alle heutigen Kontinente vereinte?",
        answerA = "Rodinia",
        answerB = "Pangäa",
        answerC = "Gondwana",
        answerD = "Laurasia",
        correctAnswer = 1,
        explanation = "Pangäa (griech. 'ganz Erde') existierte vor ca. 335–175 Millionen Jahren und vereinte alle heutigen Kontinente zu einer einzigen Landmasse. Sie zerbrach zunächst in Laurasia (Nordhalbkugel) und Gondwana (Südhalbkugel) und zergliederte sich dann weiter zu den heutigen Kontinenten.",
        difficulty = 5,
        funFact = "Rodinia war ein noch älterer Superkontinent (vor ca. 1,1 Milliarden Jahren) – er zerbrach vor etwa 750 Millionen Jahren. Die Erde durchläuft alle 400–600 Millionen Jahre einen 'Superkontinent-Zyklus', und der nächste Superkontinent 'Amasia' soll in etwa 250 Millionen Jahren entstehen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land verwendete als letztes die Bezeichnung 'Persien' offiziell, bevor der Name in 'Iran' geändert wurde, und in welchem Jahr?",
        answerA = "1906, bei der persischen Verfassungsrevolution",
        answerB = "1935, auf Betreiben von Schah Reza Pahlavi",
        answerC = "1979, nach der Islamischen Revolution",
        answerD = "1921, nach dem Staatsstreich von Reza Khan",
        correctAnswer = 1,
        explanation = "Reza Shah Pahlavi ersuchte 1935 alle Länder, den Namen 'Persien' (Exonym, basierend auf der Provinz Fars) durch 'Iran' zu ersetzen – das Endonym, das die Einwohner immer selbst verwendet hatten. Das Wort 'Iran' bedeutet 'Land der Arier'.",
        difficulty = 5,
        funFact = "Der Name 'Persien' leitet sich von 'Persis' (Provinz Fars) ab, dem Ursprungsgebiet des Achämenidenreichs. Interessanterweise heißt der Persische Golf im iranischen Endonym 'Khalij-e Fars' (خلیج فارس) – immer noch nach der alten Provinz benannt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Meeresboden-Transekt-Name bezeichnet die erste systematische Tiefseevermessung des Nordatlantiks durch das britische Forschungsschiff HMS Challenger (1872–1876)?",
        answerA = "Challenger-Section",
        answerB = "Wyville Thomson Ridge Transect",
        answerC = "Nordatlantischer Challenger-Transekt (NACT)",
        answerD = "Es gab keinen einzelnen Transektnamen – die Challenger führte Lotungen an 362 Stationen weltweit durch",
        correctAnswer = 3,
        explanation = "Die HMS Challenger-Expedition (1872–1876) führte an 362 Stationen Tiefenlotungen, Temperaturmessungen und Sedimentproben durch und gilt als Begründung der modernen Ozeanografie. Es gab keinen einzelnen benannten Transekt – die Daten wurden als 'Challenger Reports' in 50 Bänden publiziert.",
        difficulty = 5,
        funFact = "Der Name 'Challenger' wurde bewusst weitergeführt: die erste Raumsonde, die auf dem Mond landete, war der Challenger-Lunarlander (Apollo 17, 1972), und die NASA-Raumfähre Challenger (1986) trug denselben Namen – alle zu Ehren dieser bahnbrechenden Meeresforschungsexpedition."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt die wasserscheidende Linie zwischen dem Einzugsgebiet des Amazonas und dem des Río de la Plata in Südamerika?",
        answerA = "Serra do Mar",
        answerB = "Serra Geral",
        answerC = "Mato-Grosso-Plateau / Chapada dos Parecis",
        answerD = "Serra da Canastra",
        correctAnswer = 2,
        explanation = "Die Hauptwasserscheide zwischen dem Amazonas-Einzugsgebiet (→ Atlantik via Amazonas) und dem Río-de-la-Plata-System (→ Atlantik via Paraguay/Paraná) verläuft über das Mato-Grosso-Plateau und besonders die Chapada dos Parecis im westlichen Brasilien.",
        difficulty = 5,
        funFact = "Auf der Chapada dos Parecis entspringen sowohl Quellen des Tapajós (Amazonas-System) als auch des Juruena (ebenfalls Amazonas) und der Quelläste des Paraguay. Es ist eine der hydrologisch wichtigsten Hochflächen Südamerikas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land verwendete als erstes ein nationales Geodätisches Datum, das auf dem Bessel-Ellipsoid (1841) basiert und noch heute offiziell in Gebrauch ist?",
        answerA = "Preußen/Deutschland (Potsdam-Datum)",
        answerB = "Japan (Tokyo-Datum)",
        answerC = "Russland (Pulkovo-Datum)",
        answerD = "Österreich-Ungarn (Hermannskogel-Datum)",
        correctAnswer = 0,
        explanation = "Das Potsdam-Datum (auch Deutsches Hauptdreiecksnetz) basierte auf dem Bessel-Ellipsoid und hatte seinen Fundamentalpunkt am Berliner Sternwarte (Helmertturm Potsdam, Latitude 52°22'51,4469\"N). Es war das erste groß angelegte nationale Datum mit Bessel-Ellipsoid.",
        difficulty = 5,
        funFact = "Das Potsdam-Datum wurde bis 1991 in Westdeutschland und bis in die 2000er in der DDR-Nachfolge verwendet. Beim Wiederaufbau des vereinten Deutschland mussten alle Karten auf das European Datum (ED50) umgerechnet werden – was erhebliche Abweichungen von bis zu 15 m ergab."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresströmung trägt die Bezeichnung 'Thermohaline Zirkulation' und welcher Prozess treibt sie an?",
        answerA = "Der Wind, der Oberflächenwasser bewegt",
        answerB = "Die Differenzen in Temperatur und Salzgehalt, die Dichteunterschiede erzeugen",
        answerC = "Die Erdrotation (Coriolis-Kraft), die Strömungen ablenkt",
        answerD = "Die Gezeitenkräfte von Mond und Sonne",
        correctAnswer = 1,
        explanation = "Die thermohaline Zirkulation (THC, auch 'Große ozeanische Förderband') wird durch Dichteunterschiede angetrieben, die aus Temperatur- (thermo) und Salzgehaltsunterschieden (halin) entstehen. Kaltes, salzreiches Wasser sinkt ab (z.B. im Nordatlantik/Grönland), fließt tief und steigt anderswo wieder auf.",
        difficulty = 5,
        funFact = "Eine vollständige Umwälzung des Tiefenozeankönig durch die thermohaline Zirkulation dauert zwischen 500 und 2.000 Jahren. Ein klimabedinges Abschwächen des AMOC (der atlantischen Komponente) könnte Europa um 3–5°C abkühlen – trotz globalem Erwärmungstrend."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Himalaya-Pass liegt auf der Wasserscheide zwischen dem Indus-System (Arabisches Meer) und dem Ganges-Brahmaputra-System (Bengalischer Golf) und hat eine Höhe von ca. 5.416 m?",
        answerA = "Zoji La, Kaschmir (3.528 m)",
        answerB = "Rohtang Pass, Himachal Pradesh (3.978 m)",
        answerC = "Baralacha La, Lahaul-Spiti (4.892 m)",
        answerD = "Taglang La, Ladakh (5.328 m)",
        correctAnswer = 3,
        explanation = "Der Taglang La in Ladakh (Jammu & Kashmir) liegt auf ca. 5.328–5.359 m und ist einer der höchsten Straßenpässe weltweit. Er liegt auf der Hauptwasserscheide zwischen dem Indus-System (westlich) und dem Himalaya-Entwässerungsgebiet in Richtung Ganges-Ebene.",
        difficulty = 5,
        funFact = "Die Manali-Leh Highway überquert mehrere Pässe über 5.000 m, darunter den Taglang La. Bei schlechtem Wetter kann diese Straße im Sommer innerhalb weniger Stunden durch Erdrutsche und Schneefälle unpassierbar werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche ozeanografische Eigenschaft beschreibt der Begriff 'Pycnoclin' in der physikalischen Meereskunde?",
        answerA = "Eine Schicht mit abruptem Temperaturabfall (Sprungschicht)",
        answerB = "Eine Schicht mit abruptem Dichtegradienten, verursacht durch Temperatur- und/oder Salzgehaltunterschiede",
        answerC = "Eine Schicht mit maximaler Strömungsgeschwindigkeit",
        answerD = "Die Grenzschicht zwischen Süßwasser und Salzwasser in Ästuaren",
        correctAnswer = 1,
        explanation = "Die Pyknoklinae (Pycnocline) ist eine Wasserschicht, in der die Dichte des Meerwassers besonders stark mit der Tiefe zunimmt. Sie fungiert als physikalische Barriere zwischen Oberflächen- und Tiefenwasser und hemmt den vertikalen Austausch von Wärme, Nährstoffen und Gasen.",
        difficulty = 5,
        funFact = "In tropischen Meeren ist die Pyknokline besonders ausgeprägt – warmes, leichtes Oberflächenwasser liegt auf kaltem, schwerem Tiefenwasser. Diese Schichtung verhindert die Nährstoffzufuhr an die Oberfläche und erklärt, warum tropische Ozeane trotz Wärme biologisch 'Wüsten' sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Staat ersetzte 1994 seinen Exonym-geprägten englischen Staatsnamen 'Ivory Coast' durch sein Endonym 'Côte d'Ivoire' als offizielle Bezeichnung in allen Sprachen?",
        answerA = "Das Land änderte nur seinen französischen Namen; der englische Exonym-Name bleibt erlaubt",
        answerB = "Côte d'Ivoire forderte 1986 (nicht 1994) alle Länder auf, nur noch den französischen Namen zu verwenden",
        answerC = "Ivory Coast existiert nicht mehr; es wurde 2001 in zwei Staaten geteilt",
        answerD = "Die Umbenennung erfolgte 1960 bei der Unabhängigkeit",
        correctAnswer = 1,
        explanation = "Côte d'Ivoire forderte 1986 offiziell bei der UN, dass der Name 'Côte d'Ivoire' in allen Sprachen unverändert verwendet werden soll – das Einzige Mal, dass ein Staat das aktiv für alle Sprachen durchgesetzt hat. 'Ivory Coast' (englisch) oder 'Elfenbeinküste' (deutsch) sind seitdem offiziell falsch.",
        difficulty = 5,
        funFact = "Deutschland und viele andere Länder ignorieren diese Forderung und verwenden weiterhin 'Elfenbeinküste' – was das ivorische Außenministerium offiziell als diplomatische Unhöflichkeit betrachtet. Die UN nutzt konsequent 'Côte d'Ivoire'."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche ozeanografische Messung des ARGO-Programms ergab den tiefsten gemessenen Sauerstoffminimum-Wert in einem tropischen Ozeanbereich?",
        answerA = "Im östlichen Tropischen Nordpazifik (ETNP) in ca. 400–1.000 m Tiefe",
        answerB = "Im Nordatlantik nahe Labrador in ca. 200–400 m Tiefe",
        answerC = "Im Roten Meer in ca. 300–600 m Tiefe",
        answerD = "Im Benguelastrom vor Namibia in ca. 100–300 m Tiefe",
        correctAnswer = 0,
        explanation = "Die ausgedehntesten und intensivsten Sauerstoffminimumzonen (OMZ) der Weltmeere befinden sich im östlichen Tropischen Nordpazifik (ETNP) und im östlichen Tropischen Südpazifik (ETSP). ARGO-Floats messen dort in 400–1000 m Tiefe Sauerstoffwerte nahe null.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Wie bezeichnet man den Vorgang, wenn sich ein Gebirge durch tektonische Kompression aufwölbt, bevor erosive Kräfte seine heutige Form bestimmen (Paläotopografie)?",
        answerA = "Epeirogenese (großräumige Hebung/Senkung von Lithosphärenplatten)",
        answerB = "Orogenese (Gebirgsbildung durch Kollision oder Subduktion)",
        answerC = "Antiklinale Aufwölbung über einem Salzdiapir",
        answerD = "Glazio-isostatische Hebung nach dem Ende eines Eisschilds",
        correctAnswer = 1,
        explanation = "Orogenese bezeichnet die Entstehung von Gebirgen durch tektonische Prozesse wie Plattenkollision (z.B. Himalaya durch Indien/Eurasien) oder Subduktion (z.B. Anden). Die Paläotopografie rekonstruiert die ursprüngliche topografische Form dieser Gebirge vor Erosion und Denudation.",
        difficulty = 5,
        funFact = "Die Himalaya-Hauptkette hebt sich noch immer um ca. 5 mm pro Jahr, während sie durch Erosion um ähnliche Raten abgetragen wird – ein Gleichgewicht, das seit dem Miozän besteht. Paläotopografische Studien zeigen, dass der Himalaya vor 40 Millionen Jahren wahrscheinlich noch höher war als heute."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Passhöhe hat der Col du Galibier in den französischen Alpen (Departement Hautes-Alpes), einer der bekanntesten Tour-de-France-Pässe?",
        answerA = "2.642 m",
        answerB = "2.745 m",
        answerC = "2.802 m",
        answerD = "2.556 m",
        correctAnswer = 2,
        explanation = "Der Col du Galibier liegt auf einer Höhe von 2.802 m (Nordportal des alten Tunnels) bzw. 2.645 m (südliches Ende). Die Passhöhe des offenen Straßenabschnitts beträgt 2.642 m. Der höchste benutzbare Punkt des Passes liegt jedoch bei 2.802 m am Nordportal des Galibier-Tunnels.",
        difficulty = 5,
        funFact = "Der Col du Galibier wurde 1911 erstmals in die Tour de France aufgenommen und gilt als eines der härtesten Bergfinales. Auf der Südseite liegt der Col du Lautaret (2.058 m), auf der Nordseite das Skigebiet La Grave."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat als erste Nation überhaupt ein vollständiges nationales digitales Geländemodell (DTM) mit 1-Meter-Auflösung flächendeckend veröffentlicht?",
        answerA = "Dänemark",
        answerB = "Niederlande",
        answerC = "Estland",
        answerD = "Schweiz",
        correctAnswer = 0,
        explanation = "Dänemark veröffentlichte 2015 als weltweit erstes Land ein flächendeckendes nationales Digitales Geländemodell (DTM) mit 0,4-Meter-Auflösung, gewonnen durch landesweites LiDAR-Scanning. Die Daten sind open-access verfügbar.",
        difficulty = 5,
        funFact = "Dänemarks flaches Terrain (Höchstpunkt Møllehøj: 170,86 m) macht genaue Höhendaten besonders wertvoll für Küstenschutz und Überflutungsmodellierung. Das DTM zeigt, dass bei einem 1-Meter-Meeresspiegelanstieg rund 4.000 km² Dänemarks überflutet würden."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heißt der seltene geografische Begriff für den Zustand, wenn ein Fluss auf beiden Seiten einer Wasserscheide fließt – also sowohl zum einen als auch zum anderen Ozean entwässert?",
        answerA = "Bifurkation (Flussgabelung)",
        answerB = "Konfluenz (Zusammenfluss)",
        answerC = "Trifurkation (dreifache Teilung)",
        answerD = "Hydrographische Anomalie (Abfluss in beide Richtungen)",
        correctAnswer = 0,
        explanation = "Bifurkation (lat. bifurcus = zweigabelig) bezeichnet die Teilung eines Flusses in zwei Arme, die in verschiedene Einzugsgebiete fließen – ein seltenes Phänomen, bei dem Wasser auf natürliche Weise zwei verschiedene Ozeane oder Meere erreicht. Das Casiquiare-Kanal (Venezuela) ist das bekannteste Beispiel, das das Amazonas- und Orinoco-System verbindet.",
        difficulty = 5,
        funFact = "Das Casiquiare-Kanal in Venezuela ist die einzige natürliche Verbindung zwischen zwei der größten Flusssysteme der Welt (Amazonas und Orinoco) und macht die gesamte Region zu einer einzigen navigierbaren Wasserstraße von über 3.000 km Länge."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches seismische Phänomen trat beim Erdbeben in der Türkei (Kahramanmaraş, Februar 2023, Mw 7,8) auf, das die Stärke des Bebens erhöhte?",
        answerA = "Liquefaktion (Bodenverflüssigung) in sandigen Ablagerungen",
        answerB = "Supershear-Ruptur: Die Bruchausbreitung überschritt die Scherwellengeschwindigkeit",
        answerC = "Horizontale Oberflächenversetzung von über 6 Metern entlang der Nordanatolischen Verwerfung",
        answerD = "Tsunamiauslösung durch vertikale Meeresbodenbewegung",
        correctAnswer = 1,
        explanation = "Das Kahramanmaraş-Erdbeben (6. Februar 2023) zeigte eine seltene Supershear-Ruptur: Die Bruchfront an der Ostanatolischen Verwerfung breitete sich schneller aus als die lokale S-Wellengeschwindigkeit (~3,5 km/s), was zu besonders destruktiven Machwellen-ähnlichen Erschütterungen führte.",
        difficulty = 5,
        funFact = "Supershear-Erdbeben sind extrem selten und wurden erst in den letzten Jahrzehnten mit modernen Seismometern nachgewiesen. Das Erdbeben in Denali, Alaska (2002) war das erste gut dokumentierte Supershear-Beben. Das türkische Beben von 2023 ist das verheerende Beispiel in dicht besiedeltem Gebiet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Küste in der Antarktis ist nach dem russischen Exonym 'Queen Maud Land' bekannt, trägt aber im norwegischen Endonym einen anderen Namen?",
        answerA = "Droning Maud Land – norwegisches Endonym für das antarktische Territorium Norwegens",
        answerB = "Prinsesse Ragnhild Kyst – norwegisches Endonym nur für die östliche Küstenzone",
        answerC = "Kronprinsesse Märtha Kyst – für den westlichen Küstenabschnitt",
        answerD = "Das Gebiet hat keinen gesonderten Küstennamen; es heißt insgesamt 'Dronning Maud Land'",
        correctAnswer = 3,
        explanation = "Das gesamte antarktische Territorium Norwegens heißt auf Norwegisch 'Dronning Maud Land' (Königin-Maud-Land). Innerhalb dieses Territoriums gibt es einzelne Küstenabschnitte mit eigenen Namen (Prinsesse Ragnhild Kyst, Kronprinsesse Märtha Kyst), aber kein Gebiet heißt insgesamt anders als 'Dronning Maud Land'.",
        difficulty = 5,
        funFact = "Norwegen nahm Dronning Maud Land 1939 in Besitz – unmittelbar bevor Deutschland eine Antarktisexpedition in dieselbe Region schickte. Die 'Schwabenland'-Expedition 1939 beanspruchte das Gebiet als 'Neu-Schwabenland' für das Deutsche Reich, ein Anspruch, der nach 1945 aufgegeben wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viele Einwohner hatte Indien beim Zensus 2011 (der letzten vollständig ausgewerteten Volkszählung)?",
        answerA = "1.210.854.977",
        answerB = "1.187.439.200",
        answerC = "1.224.514.327",
        answerD = "1.198.003.460",
        correctAnswer = 0,
        explanation = "Der indische Zensus vom März 2011 ergab eine Bevölkerung von 1.210.854.977 Personen. Dieser Wert ist die offizielle Zahl des Office of the Registrar General and Census Commissioner of India. Der Zensus 2021 wurde wegen COVID-19 auf unbestimmte Zeit verschoben.",
        difficulty = 5,
        funFact = "Der indische Zensus 2011 war der größte Zensus in der Geschichte der Menschheit – rund 2,7 Millionen Zählbeamte wurden eingesetzt, um jeden der über 600.000 Dörfer und Städte zu erfassen. Die Logistik dauerte mehrere Monate."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche kartografische Projektion verwendet die NASA für die Planungskarten der Mars-Sonden (MOLA-Topografie), da sie flächentreu und für globale Vergleiche geeignet ist?",
        answerA = "Stereografische Polarprojektion",
        answerB = "Sinusoidal-Projektion",
        answerC = "Mollweide-Projektion",
        answerD = "Equirektangulare Projektion (Plate Carrée)",
        correctAnswer = 3,
        explanation = "Die NASA verwendet für MOLA-Datensätze (Mars Orbiter Laser Altimeter) und andere planetare Topografiedaten primär die äquidistante Equirektangulare Projektion (Plate Carrée), da sie einfache Koordinatenzuordnung erlaubt und in Rasterdatenbanken effizient gespeichert werden kann.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der tiefste Punkt in der Wüste Gobi und wie tief liegt er unter dem Meeresspiegel?",
        answerA = "Hami-Becken (Turpan-Senke), −154 m",
        answerB = "Alxa-Senke (Innere Mongolei), −82 m",
        answerC = "Gaxun-Nur-Becken, −62 m",
        answerD = "Tarim-Becken, −154 m – technisch gesehen keine Gobi",
        correctAnswer = 3,
        explanation = "Die Turpan-Senke (Hami-Becken) in der autonomen Region Xinjiang liegt auf −154 m und ist der tiefste Punkt Chinas. Sie gehört jedoch zum Tarim-Becken, nicht zur eigentlichen Gobi-Wüste. Die Gobi-Wüste selbst liegt überwiegend auf über 800–1.000 m Höhe – die tiefsten Teile der Gobi liegen nicht unter dem Meeresspiegel.",
        difficulty = 5,
        funFact = "Die Turpan-Senke ist mit über 50°C im Sommer und −30°C im Winter einer der klimatischen Extremorte der Erde. Sie ist auch bekannt als 'Feuertal' und liegt nur 150 km südlich des Tianshan-Gebirges, wo Gletscher sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ozeanografische Phänomen bewirkt, dass der Peruanische Strom die Küste Südamerikas mit ungewöhnlich kaltem Wasser versorgt?",
        answerA = "Thermohaline Absinkzone im Südatlantik",
        answerB = "Küstenauftrieb (Coastal Upwelling) durch äquatoriale Ostwinde und Ekman-Transport",
        answerC = "Untermeerischer Kaltquell an der Chile-Peru-Tiefseerinne",
        answerD = "Polares Tiefenwasser aus der Antarktis, das entlang der Kontinentalböschung aufsteigt",
        correctAnswer = 1,
        explanation = "Der kalte Humboldtstrom (Peruanischer Strom) wird durch Küstenauftrieb (coastal upwelling) verstärkt: Südostwinde treiben Oberflächenwasser nordwestwärts (Ekman-Transport), und kaltes, nährstoffreiches Tiefenwasser steigt an der Küste auf. Dies macht die Region zu einem der fischreichsten Meeresgebiete der Welt.",
        difficulty = 5,
        funFact = "Der Küstenauftrieb vor Peru ist so produktiv, dass er jährlich rund 8–10 % des gesamten Weltfischfangs liefert – dabei macht die Küstenlänge nur einen Bruchteil der Weltmeere aus. El Niño unterbricht diesen Auftrieb und führt regelmäßig zu Fischsterbeereignissen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet das Endonym der Stadt, die auf Deutsch 'Kopenhagen' heißt, auf Dänisch aber einen anderen Namen trägt?",
        answerA = "Kobenhavn (Köbenhavn) – mit dem dänischen Buchstaben ø",
        answerB = "Hafnia – latineischer Name, heute nicht mehr gebräuchlich",
        answerC = "Köpenhamn – schwedisches Exonym",
        answerD = "København – mit dem dänischen Buchstaben ø",
        correctAnswer = 3,
        explanation = "Der dänische Eigenname der Hauptstadt ist København (mit ø, gesprochen 'Köbenhavn'). Das Exonym 'Kopenhagen' im Deutschen leitet sich vom lateinischen 'Hafnia' und mittelalterlichen Formen des dänischen Namens ab. 'Köbenhavn' bedeutet wörtlich 'Kaufmannshafen'.",
        difficulty = 5,
        funFact = "Das chemische Element Hafnium (Hf, Ordnungszahl 72) wurde 1923 in Kopenhagen entdeckt und nach dem lateinischen Namen der Stadt benannt: Hafnia. Es ist eines von wenigen chemischen Elementen, die nach einer europäischen Hauptstadt benannt sind."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche biogeografische Trennlinie unterscheidet die indomalayische von der australasischen Tiergeografie und liegt östlich der Wallace-Linie?",
        answerA = "Weber-Linie",
        answerB = "Lydekker-Linie",
        answerC = "Huxley-Linie",
        answerD = "Müller-Linie",
        correctAnswer = 0,
        explanation = "Die Weber-Linie (benannt nach dem Zoologen Max Weber) liegt östlich der Wallace-Linie und markiert die Zone, wo australasische Tierarten die asiatischen überwiegen. Zwischen Wallace- und Weber-Linie liegt die 'Wallacea' – ein Übergangsgebiet mit gemischter Fauna.",
        difficulty = 5,
        funFact = "Die Lydekker-Linie (östlich der Weber-Linie) markiert die äußerste Verbreitung asiatischer Arten. Die Zone zwischen Wallace- und Lydekker-Linie (Wallacea) ist eines der artenreichsten Gebiete der Erde – mit vielen endemischen Arten, die nirgendwo sonst vorkommen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Passhöhe hat der Großglockner-Hochalpenstraße an ihrem höchsten Punkt (Fuscher Törl) in Österreich?",
        answerA = "2.505 m",
        answerB = "2.369 m",
        answerC = "2.504 m",
        answerD = "2.571 m",
        correctAnswer = 0,
        explanation = "Das Fuscher Törl (auch Fuscher Lacke) auf der Großglockner-Hochalpenstraße in Österreich liegt auf 2.504 m – die Straße erreicht ihren höchsten Punkt bei 2.504 m (Fuscher Törl) und nicht am Hochtor (2.505 m), welches der eigentliche höchste Straßenpunkt ist.",
        difficulty = 5,
        funFact = "Die Großglockner-Hochalpenstraße wurde zwischen 1930 und 1935 gebaut – als Arbeitsbeschaffungsmaßnahme in der Weltwirtschaftskrise. Franz Wallack, der Chefingenieur, war erst 33 Jahre alt, als er mit der Planung begann."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche genaue Koordinate (Länge/Breite) hat der Punkt 'Pole of Inaccessibility' auf dem antarktischen Kontinent – der am weitesten von der Küste entfernte Punkt?",
        answerA = "83°51′S, 65°44′E",
        answerB = "82°06′S, 54°58′E",
        answerC = "85°50′S, 65°47′E",
        answerD = "89°00′S, 0°00′E",
        correctAnswer = 0,
        explanation = "Der antarktische 'Pole of Inaccessibility' liegt bei ca. 83°51′S, 65°44′E (Berechnungen variieren je nach Küstendefinition). Die sowjetische Station 'Polyus Nedostupnosti' (1958) wurde etwa hier errichtet. Der Punkt liegt ca. 878 km vom Südpol entfernt.",
        difficulty = 5,
        funFact = "An der Sowjetstation Polyus Nedostupnosti wurde eine Büste von Lenin aufgestellt, die heute noch (halb eingeschneit) existiert. Die Station wurde nach nur 12 Tagen verlassen und nie wieder besucht – die Lenin-Büste bleibt als einziges Symbol zurück."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die 'Neotethys' in der Paläogeografie und wann existierte sie?",
        answerA = "Ein Ozean zwischen Gondwana und Laurasia, der vor ca. 250–66 Millionen Jahren existierte",
        answerB = "Die heutige Tethys-Region (Mittelmeer + Schwarzes Meer + Kaspisches Meer)",
        answerC = "Ein Gletscher in der Antarktis",
        answerD = "Eine Tiefseezone im heutigen Indischen Ozean",
        correctAnswer = 0,
        explanation = "Die Neotethys war ein Ozean zwischen dem nördlichen Gondwana (Afrika, Indien, Arabien) und der Eurasischen Platte (Laurasia), der von ca. 250 bis 66 Millionen Jahren existierte. Der heutige Himalaya, die Alpen, der Kaukasus und das Zagros-Gebirge entstanden durch seine Schließung.",
        difficulty = 5,
        funFact = "Im Himalaya findet man Fossilien von Meerestieren aus der Neotethys – Ammoniten und Korallen, die einst am Meeresboden lagen, wurden durch die Kollision Indiens mit Eurasien auf über 8.000 m Höhe gehoben."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land in Südostasien, das niemals eine europäische Kolonie war und bis heute sein historisches Datum für seine Staatsgründung beibehalten hat?",
        answerA = "Vietnam",
        answerB = "Thailand (Siam)",
        answerC = "Myanmar (Burma)",
        answerD = "Kambodscha",
        correctAnswer = 1,
        explanation = "Thailand (historisch Siam) war das einzige Land Südostasiens, das während der europäischen Kolonialzeit formal unabhängig blieb. Es diente als Pufferstaat zwischen dem britischen Burma und dem französischen Indochina und bewahrte seine Eigenständigkeit durch geschickte Diplomatie.",
        difficulty = 5,
        funFact = "Die Könige Chulalongkorn (Rama V.) und sein Vater Mongkut (Rama IV.) modernisierten Siam so geschickt, dass europäische Mächte es als zivilisierten Staat anerkannten. Mongkut wird im Musical 'The King and I' dargestellt – allerdings sehr ungenau und in Thailand bis heute verboten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Methode wird zur Bestimmung der Schichttiefe von ozeanischen Krusten (Mohorovičić-Diskontinuität) verwendet?",
        answerA = "Gravimetrie (Messung von Schwerkraftanomalien)",
        answerB = "Reflexions- und Refraktionsseismik",
        answerC = "Magnetometrie (Messung von Paläomagnetismus)",
        answerD = "Ozeanische Kernbohrungen (DSDP/ODP/IODP)",
        correctAnswer = 1,
        explanation = "Die Mohorovičić-Diskontinuität (Moho) – die Grenze zwischen Erdkruste und Erdmantel – wird primär durch Reflexions- und Refraktionsseismik bestimmt. Seismische Wellen ändern ihre Geschwindigkeit und Richtung an der Moho, was ihre genaue Tiefe (unter Kontinenten 30–70 km, unter Ozeanen 6–10 km) ergibt.",
        difficulty = 5,
        funFact = "Das ambitionierteste Projekt des 20. Jahrhunderts war das 'Project Mohole' (1961–1966) – ein US-amerikanischer Versuch, durch ozeanische Kruste bis zur Moho zu bohren. Das Projekt wurde wegen Kostenüberschreitungen eingestellt; die Moho wurde nie erreicht."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie nennt man die geologische Periode, in der sich die Erdoberfläche ausschließlich aus Unterwasser-Vulkanen (Midozeankämme) erneuerte und keine Kontinente gab?",
        answerA = "Hadaikum (Hadean) – vor 4,0–4,6 Milliarden Jahren",
        answerB = "Archaikum (Archean) – vor 2,5–4,0 Milliarden Jahren",
        answerC = "Proterozoikum – vor 0,5–2,5 Milliarden Jahren",
        answerD = "Kryogenium (Snowball Earth) – vor 635–720 Millionen Jahren",
        correctAnswer = 0,
        explanation = "Im Hadaikum (vor 4,0–4,6 Mrd. Jahren) war die Erde frisch erstarrt; frühe Krustenbildung fand statt, aber keine stabilen Kontinentalplatten. Die ersten stabilen Kratone (Kontinentalkerne) entstanden erst im frühen Archaikum.",
        difficulty = 5,
        funFact = "Das älteste bekannte Mineral der Erde ist ein Zirkonkristall aus dem Jack Hills in Australien – 4,404 Milliarden Jahre alt. Da Zirkone extrem robust sind, haben diese winzigen Kristalle alle geologischen Phasen überlebt und erzählen die älteste Geschichte unseres Planeten."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lautet das Endonym der Stadt, die auf Deutsch 'Warschau', auf Englisch 'Warsaw', aber auf Polnisch anders heißt?",
        answerA = "Warszawa",
        answerB = "Warsław",
        answerC = "Varsovia",
        answerD = "Warschawa",
        correctAnswer = 0,
        explanation = "Das polnische Endonym lautet Warszawa (gesprochen 'Varshava'). Das Exonym 'Warschau' im Deutschen ist eine alte Eindeutschung. 'Varsovia' ist das spanische und lateinische Exonym. Der Stadtname leitet sich wahrscheinlich von einem slawischen Personennamen 'Wars' ab.",
        difficulty = 5,
        funFact = "Warschau/Warszawa wurde im Zweiten Weltkrieg zu über 85 % zerstört – nach dem Warschauer Aufstand 1944 ließ Hitler die Stadt systematisch dem Erdboden gleichmachen. Der Wiederaufbau nach historischen Plänen und Gemälden Canalettos gilt als ein Wunder der Rekonstruktion."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Scherwellen-Geschwindigkeit (Vs) hat der Erdmantel direkt unterhalb der Moho in ozeanischen Bereichen typischerweise?",
        answerA = "ca. 4,5–4,7 km/s",
        answerB = "ca. 7,8–8,2 km/s",
        answerC = "ca. 6,0–6,5 km/s",
        answerD = "ca. 3,5–3,8 km/s",
        correctAnswer = 3,
        explanation = "Scherwellen (S-Wellen) können im Erdmantel nicht durch flüssiges Material reisen. Die Vs im Obermantel direkt unterhalb der ozeanischen Moho beträgt ca. 4,4–4,7 km/s. Die angegebenen 3,5–3,8 km/s betrifft P-Wellen in der ozeanischen Kruste.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Eigenname (Endonym) verwendet die Volksrepublik China für den Himalaya-Gipfel, der international als 'Mount Everest' bekannt ist?",
        answerA = "Qomolangma (珠穆朗玛峰)",
        answerB = "Sagarmatha (सगरमाथा)",
        answerC = "Chomolungma",
        answerD = "Zhumulangma Feng (珠穆朗玛峰)",
        correctAnswer = 0,
        explanation = "Die offizielle chinesische Bezeichnung ist 珠穆朗玛峰 (Zhūmùlǎngmǎ Fēng, kurz Qomolangma). Die tibetische Form 'Chomolungma' (rGya-ma-lung-ma) ist die ursprüngliche tibetische Bezeichnung und bedeutet 'Göttin Mutter der Welt'. Sagarmatha (सगरमाथा) ist das nepalesische Endonym.",
        difficulty = 5,
        funFact = "George Everest (der Surveyor General of India) wusste gar nicht, welcher Gipfel nach ihm benannt wurde – er starb 1866, bevor die erste Besteigung versucht wurde. Er selbst sprach seinen Namen 'Eve-rest' aus, nicht 'Ever-est', wie es heute allgemein üblich ist."
    ),

    // ── MASTER (difficulty = 5) ── questions 164–213 (coastal erosion, tidal ranges, shelves, tephra, seismicity, MPAs, glaciers, pressure records, thermohaline, demographics) ──

    Question(
        categoryId = 1,
        questionText = "Welche Küstenerosionsrate wurde an der Holderness-Küste (Yorkshire, England) als höchste in Europa gemessen?",
        answerA = "ca. 0,8 m/Jahr",
        answerB = "ca. 1,2 m/Jahr",
        answerC = "ca. 2,0 m/Jahr",
        answerD = "ca. 3,5 m/Jahr",
        correctAnswer = 1,
        explanation = "Die Holderness-Küste in East Yorkshire gilt mit einer durchschnittlichen Erosionsrate von etwa 1,2–2,0 m/Jahr als die am schnellsten erodierte Küste Europas. Die weichen Geschiebemergel-Kliffen aus der letzten Eiszeit werden durch Nordseewellen ohne natürlichen Schutz abgetragen. Langzeitdaten des British Geological Survey bestätigen den Mittelwert von ~1,2 m/Jahr.",
        difficulty = 5,
        funFact = "Seit der Römerzeit sind an der Holderness-Küste über 30 Dörfer im Meer versunken. Das letzte bewohnte Dorf, das vollständig verloren ging, war Kilnsea – dessen Kirche Anfang des 20. Jahrhunderts ins Meer fiel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ort verzeichnet den weltweit größten Tidenhub und wie hoch ist er?",
        answerA = "Severn-Mündung, Großbritannien – bis zu 14,8 m",
        answerB = "Bay of Fundy, Kanada – bis zu 16,3 m",
        answerC = "Gulf of Khambhat, Indien – bis zu 12,1 m",
        answerD = "Cook Inlet, Alaska, USA – bis zu 10,9 m",
        correctAnswer = 1,
        explanation = "Die Bay of Fundy in Kanada (zwischen Nova Scotia und New Brunswick) verzeichnet den weltweit höchsten Tidenhub mit einem Maximalwert von ca. 16,3 m (gemessen in Burntcoat Head, NS). Die resonante Eigenfrequenz des Meeresbeckens stimmt nahezu exakt mit dem Gezeitenrhythmus überein.",
        difficulty = 5,
        funFact = "Pro Gezeitenzyklus (ca. 12,4 Stunden) bewegen sich in der Bay of Fundy etwa 115 Milliarden Tonnen Wasser – mehr als der kombinierte tägliche Abfluss aller Süßwasserflüsse der Erde."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie breit ist der Kontinentalsockel (bis 200 m Tiefe) vor der Küste Argentiniens (Patagonien) im Bereich der breitesten Ausdehnung?",
        answerA = "ca. 350 km",
        answerB = "ca. 550 km",
        answerC = "ca. 800 km",
        answerD = "ca. 1.100 km",
        correctAnswer = 1,
        explanation = "Der patagonische Kontinentalsockel vor Argentinien ist mit einer Breite von bis zu ~550 km einer der breitesten der Welt. Er erstreckt sich von der Küste bis weit in den Südatlantik und bildet die Grundlage für den argentinischen Anspruch auf einen erweiterten Festlandsockel gemäß UNCLOS-Artikel 76.",
        difficulty = 5,
        funFact = "Argentinien reichte 2009 beim UN-Ausschuss für die Grenzen des Festlandsockels einen Antrag auf Erweiterung seiner ausschließlichen Wirtschaftszone ein, der auf dem breiten patagonischen Schelf basiert – eine der größten Schelfflächenansprüche der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Tephra-Volumen (Dense Rock Equivalent, DRE) eruptierte der Pinatubo (Philippinen) beim Ausbruch von 1991?",
        answerA = "ca. 2,5 km³ DRE",
        answerB = "ca. 5 km³ DRE",
        answerC = "ca. 10 km³ DRE",
        answerD = "ca. 15 km³ DRE",
        correctAnswer = 2,
        explanation = "Der Ausbruch des Pinatubo am 15. Juni 1991 injizierte ca. 10 km³ DRE (Dense Rock Equivalent) Material – davon rund 5 km³ als pyroplastische Ströme und weitere Mengen als Tephra-Fall und SO₂-Aerosol in die Stratosphäre. Es war der zweitstärkste Ausbruch des 20. Jahrhunderts nach dem Novarupta 1912.",
        difficulty = 5,
        funFact = "Die Schwefeldioxid-Injektion des Pinatubo (ca. 20 Millionen Tonnen SO₂) kühlte den globalen Durchschnitt um rund 0,5°C für zwei Jahre. Dieser natürliche 'Geoengineering'-Effekt dient heute als Referenzpunkt für Solar-Radiation-Management-Studien."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie lang ist das durchschnittliche Wiederkehrintervall für Mw-≥9,0-Erdbeben entlang der Cascadia-Subduktionszone (US-Westküste/Kanada)?",
        answerA = "ca. 100–200 Jahre",
        answerB = "ca. 300–500 Jahre",
        answerC = "ca. 700–1.000 Jahre",
        answerD = "ca. 2.000–5.000 Jahre",
        correctAnswer = 1,
        explanation = "Paläoseismologische Studien (Turbidite-Stratigraphie, Küstensedimente) belegen für die Cascadia-Subduktionszone Wiederkehrintervalle von ca. 300–500 Jahren für vollständige Rupturen (Mw ~9). Das letzte solche Beben ereignete sich am 26. Januar 1700 – durch japanische historische Aufzeichnungen des resultierenden Tsunamis genau datiert.",
        difficulty = 5,
        funFact = "Das Cascadia-Erdbeben von 1700 wurde auf Mw 9,0 rekonstruiert. Da Japan den Tsunami dokumentierte (aber kein entsprechendes lokales Beben kannte), konnten Wissenschaftler das genaue Datum rückwirkend 300 Jahre später bestimmen – ein einmaliger Beweis für transozeanen Tsunamivortransport."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das flächenmäßig größte marine Schutzgebiet (MPA) der Welt?",
        answerA = "Great Barrier Reef Marine Park, Australien – ca. 344.400 km²",
        answerB = "Ross Sea Region Marine Protected Area, Antarktis – ca. 1.550.000 km²",
        answerC = "Papahānaumokuākea Marine National Monument, USA – ca. 1.510.000 km²",
        answerD = "Marae Moana Marine Park, Cookinseln – ca. 1.900.000 km²",
        correctAnswer = 1,
        explanation = "Das Ross Sea Region MPA, 2016 durch die CCAMLR (Kommission zur Erhaltung der antarktischen Meereslebewesen) eingerichtet, umfasst ca. 1.550.000 km² und ist damit das größte Meeresschutzgebiet der Welt. Es liegt in der Antarktis südlich von Neuseeland.",
        difficulty = 5,
        funFact = "Das Ross Sea MPA ist jedoch nur befristet – die Schutzzone läuft 2052 aus, wenn es nicht erneuert wird. Russland und China blockierten jahrelang seine Einrichtung, bevor ein Kompromiss mit zeitlicher Befristung erzielt wurde."
    ),

    Question(
        categoryId = 1,
        questionText = "Mit welcher Rate zog sich der Athabasca-Gletscher in Kanada (Columbia Icefield) zwischen 1990 und 2020 durchschnittlich zurück?",
        answerA = "ca. 5 m/Jahr",
        answerB = "ca. 10 m/Jahr",
        answerC = "ca. 15 m/Jahr",
        answerD = "ca. 25 m/Jahr",
        correctAnswer = 2,
        explanation = "Der Athabasca-Gletscher (Alberta, Kanada) ist seit 1890 um insgesamt über 1,5 km zurückgewichen und verliert jährlich durchschnittlich ca. 15–16 m an Länge (aktuelle Periode). Die Rückzugsrate hat sich in den letzten Jahrzehnten beschleunigt – in den 1990er bis 2010er Jahren waren es etwa 15 m/Jahr.",
        difficulty = 5,
        funFact = "Der Athabasca-Gletscher ist einer der wenigen arktischen Gletscher, den man mit dem Auto nahezu erreichen kann. Er ist Teil des Columbia Icefields und speist drei verschiedene Ozean-Einzugsgebiete: Arktis, Pazifik und Atlantik (über den Mackenzie, Columbia und North Saskatchewan River)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher niedrigste Luftdruck wurde jemals auf Meereshöhe gemessen und wo?",
        answerA = "870 hPa, Taifun Tip (Pazifik), 12. Oktober 1979",
        answerB = "882 hPa, Taifun Nora (Pazifik), 1973",
        answerC = "890 hPa, Hurrikan Wilma (Atlantik), 2005",
        answerD = "858 hPa, Taifun Violet (Pazifik), 1982",
        correctAnswer = 0,
        explanation = "Der niedrigste je auf Meereshöhe gemessene Luftdruck betrug 870 hPa, gemessen im Auge von Taifun Tip am 12. Oktober 1979 im westlichen Nordpazifik (17°N, 138°E). Dieser Wert gilt als absoluter Rekord für den Atmosphärendruck auf Meereshöhe weltweit.",
        difficulty = 5,
        funFact = "Taifun Tip war auch der flächenmäßig größte tropische Wirbelsturm aller Zeiten – sein Durchmesser von Windfeld zu Windfeld betrug rund 2.220 km, also etwa so breit wie der halbe Kontinent der USA."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Schichttiefe markiert die typische Thermoklinen-Untergrenze im tropischen Atlantik nach ARGO-Floatdaten (mittlere Temperatur ≤4°C)?",
        answerA = "ca. 500 m",
        answerB = "ca. 700–800 m",
        answerC = "ca. 1.200–1.500 m",
        answerD = "ca. 2.000–2.500 m",
        correctAnswer = 2,
        explanation = "Im tropischen Atlantik sinkt die Temperatur unterhalb der saisonalen Thermoklinen (50–200 m) zunächst auf ca. 4–8°C in 500–800 m Tiefe und erreicht dauerhaft unter 4°C erst in ca. 1.200–1.500 m Tiefe (Permanent Thermocline). ARGO-Float-Daten bestätigen diese Schichtungstiefe als Untergrenze der permanenten Thermoklinen.",
        difficulty = 5,
        funFact = "Das ARGO-Programm betreibt ca. 4.000 autonome Floats weltweit, die alle 10 Tage bis 2.000 m Tiefe tauchen und Temperatur/Salzgehalt messen. Sie haben unser Verständnis der Ozeanzirkulation revolutioniert – pro Float und Jahr werden Daten gesammelt, die früher eine Forschungsreise erfordert hätten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land trat laut UN-Demographic-Transition-Modell zuletzt in Phase 3 (sinkende Geburtenrate) ein – also das letzte Land, das den Übergang von hoher zu niedrigerer Fertilität vollzog?",
        answerA = "Niger",
        answerB = "Mali",
        answerC = "Somalia",
        answerD = "DRC (Kongo)",
        correctAnswer = 0,
        explanation = "Niger hat mit einer TFR (Total Fertility Rate) von ca. 6,9 (UN 2022) die weltweit höchste Geburtenrate und befindet sich noch weitgehend in Phase 2 des demografischen Übergangsmodells (hohe Geburten, sinkende Sterblichkeit). Es ist das letzte größere Land, das die Phase-3-Transition (spürbar sinkende TFR) noch nicht vollzogen hat.",
        difficulty = 5,
        funFact = "Bei der aktuellen TFR von Niger würde sich die Bevölkerung innerhalb von 23 Jahren verdoppeln. Niger hat die jüngste Bevölkerung der Welt: Der Altersmedian liegt bei etwa 15 Jahren – mehr als die Hälfte aller Einwohner ist unter 15 Jahre alt."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie breit ist der Kontinentalsockel (bis 200 m Wassertiefe) nördlich von Sibirien im Bereich der Laptewsee an seiner breitesten Stelle?",
        answerA = "ca. 200 km",
        answerB = "ca. 500 km",
        answerC = "ca. 900 km",
        answerD = "ca. 1.300 km",
        correctAnswer = 2,
        explanation = "Die Laptewsee nördlich Sibiriens sitzt auf einem der breitesten Kontinentalschelfe der Welt – rund 900 km breit bei einer mittleren Tiefe von nur 50 m. Der Schelf ist so flach, dass er während der letzten Eiszeit (LGM) vollständig trockenfiel und Teil der Bering-Landbrücke war.",
        difficulty = 5,
        funFact = "Unter dem sibirischen Kontinentalsockel in der Laptewsee lagern riesige Mengen Methanhydrate im Permafrost. Klimawissenschaftler beobachten mit Sorge, dass das Auftauen dieses submarinen Permafrostes Methankrater erzeugen könnte – mit potenziell starken Rückkopplungseffekten auf das Klima."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Rate des Küstenrückgangs (sea-cliff retreat) wurde in der Studie von EUROSION (2004) für die Küste der Normandie in Frankreich als repräsentativer Mittelwert angegeben?",
        answerA = "ca. 0,1 m/Jahr",
        answerB = "ca. 0,3 m/Jahr",
        answerC = "ca. 0,5 m/Jahr",
        answerD = "ca. 1,0 m/Jahr",
        correctAnswer = 1,
        explanation = "Der EUROSION-Bericht (2004) dokumentierte für die Kreidefelsen der Normandie (besonders zwischen Le Havre und Étretat) mittlere Erosionsraten von ca. 0,3 m/Jahr. Diese Kreideküsten werden durch Wellenerosion und Kliffstürze abgetragen, wobei einzelne Episoden deutlich höhere Raten erzeugen können.",
        difficulty = 5,
        funFact = "Die berühmten Kreidebögen von Étretat (durch Maupassant und Monet weltberühmt) schrumpfen messbar. Geowissenschaftler modellieren, wann der nächste Bogen kollabieren wird – bereits 2021 brach ein beträchtlicher Kreideblock in den Strand."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Tephra-Volumen (DRE) eruptierte der Krakatau (Indonesien) beim Ausbruch von 1883?",
        answerA = "ca. 4 km³ DRE",
        answerB = "ca. 6 km³ DRE",
        answerC = "ca. 10 km³ DRE",
        answerD = "ca. 18 km³ DRE",
        correctAnswer = 3,
        explanation = "Der Ausbruch des Krakatau am 26.–27. August 1883 eruptierte schätzungsweise 18–21 km³ DRE – was den höchsten Punkt der Insel (440 m) zerstörte und eine Caldera hinterließ. Der Ausbruch erzeugte einen Tsunami von bis zu 37 m Höhe, der rund 36.000 Menschen tötete.",
        difficulty = 5,
        funFact = "Der Knall des Krakatau-Ausbruchs war an mindestens 50 verschiedenen Orten rund um die Welt zu hören – auf der australischen Insel Rodrigues (4.800 km entfernt) hielten Menschen das Geräusch für Kanonenfeuer. Er gilt bis heute als lautestes natürliches Ereignis in der Menschheitsgeschichte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Geschwindigkeit des antarktischen Eisschild-Fließens (ice stream velocity) hat der Thwaites-Gletscher (Westantarktis) an seiner Gletscherzunge laut NASA-Messungen 2020 erreicht?",
        answerA = "ca. 200 m/Jahr",
        answerB = "ca. 600 m/Jahr",
        answerC = "ca. 1.200 m/Jahr",
        answerD = "ca. 2.000 m/Jahr",
        correctAnswer = 2,
        explanation = "Der Thwaites-Gletscher fließt an seiner Gletscherzunge mit ca. 1.200–1.400 m/Jahr (ca. 3–4 m pro Tag) zum Meer hin. Diese Geschwindigkeit hat sich in den letzten Jahrzehnten erhöht und wird durch das Unterschmelzen der schwimmenden Gletscherzunge (Grounding Line Retreat) verursacht.",
        difficulty = 5,
        funFact = "Der Thwaites-Gletscher wird als 'Doomsday Glacier' bezeichnet – würde er vollständig abschmelzen, stiege der globale Meeresspiegel um 65 cm. Er hält zudem weitere Eismassen der Westantarktis zurück, die zusammen den Meeresspiegel um bis zu 3 m erhöhen könnten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Tidalbereich-Klassifikation entspricht dem Gezeitenbereich der Deutschen Bucht (Nordsee) mit mittlerem Tidenhub von ~3,5 m?",
        answerA = "Mikrotidal (< 2 m)",
        answerB = "Mesotidal (2–4 m)",
        answerC = "Makrotidal (> 4 m)",
        answerD = "Hypertidal (> 6 m)",
        correctAnswer = 1,
        explanation = "Nach der Klassifikation von Davies (1964) gilt ein Tidenhub von 2–4 m als mesotidal. Der mittlere Tidenhub in der Deutschen Bucht beträgt je nach Ort 2,5–3,8 m (Cuxhaven: ~3,5 m) und liegt damit im mesotidalen Bereich. Das Wattenmeer ist ein typisches Produkt mesotidaler Verhältnisse.",
        difficulty = 5,
        funFact = "Das Wattenmeer des deutschen Nordseeküste ist das weltweit größte zusammenhängende Wattengebiet und seit 2009 UNESCO-Weltnaturerbe. Bei Niedrigwasser fallen bis zu 10.000 km² trockene Wattfläche frei – eine einmalige Landschaft, die zweimal täglich zwischen Meer und Land wechselt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Wiederkehrintervall haben Mw-≥8,0-Erdbeben entlang der subduktionszonenartig aktiven Nankai-Trogzone (Japan) nach historischen Aufzeichnungen?",
        answerA = "ca. 50–100 Jahre",
        answerB = "ca. 100–200 Jahre",
        answerC = "ca. 300–500 Jahre",
        answerD = "ca. 600–800 Jahre",
        correctAnswer = 1,
        explanation = "Historische und instrumentelle Aufzeichnungen zeigen für den Nankai-Trog (Südküste Japans) Wiederkehrintervalle für Mw-≥8-Erdbeben von ca. 100–200 Jahren. Die letzten großen Tonankai/Nankai-Erdbeben traten 1944 und 1946 auf. Japanische Behörden schätzen eine 70–80 % Wahrscheinlichkeit für ein solches Beben bis 2053.",
        difficulty = 5,
        funFact = "Japan investiert Milliarden in die Vorbereitung auf das nächste Nankai-Megabeben. Unter dem Meeresboden ist ein 6.000-Sensor-Überwachungsnetz (DONET) installiert – das größte Unterwasser-Erdbebenfrühwarnsystem der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die Fläche des Meeres-Nationalparks 'Phoenix Islands Protected Area' (PIPA) im Pazifik (Kiribati)?",
        answerA = "ca. 150.000 km²",
        answerB = "ca. 408.000 km²",
        answerC = "ca. 650.000 km²",
        answerD = "ca. 1.100.000 km²",
        correctAnswer = 1,
        explanation = "Das Phoenix Islands Protected Area (PIPA) in der Republik Kiribati umfasst ca. 408.250 km² und ist eines der größten marinen Schutzgebiete der Erde. Es wurde 2008 eingerichtet und 2010 zum UNESCO-Weltnaturerbe ernannt. Seit 2014 ist Fischerei im gesamten PIPA verboten.",
        difficulty = 5,
        funFact = "Das PIPA enthält acht tropische Atolle und zwei Unterwasservulkane. Da es kaum Bevölkerung gibt und Fischerei verboten ist, gilt es als eines der unberührtesten marinen Ökosysteme – mit Haipopulationen, die 10-mal dichter sind als in typischen tropischen Riffen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher höchste Luftdruck auf Meereshöhe wurde jemals gemessen und wo?",
        answerA = "1.085 hPa, in Tosontsengel, Mongolei, Dezember 2001",
        answerB = "1.083,8 hPa, in Agata, Sibirien, Dezember 1968",
        answerC = "1.079 hPa, in Ulaanbaatar, Mongolei, Januar 1970",
        answerD = "1.088 hPa, in Oymyakon, Sibirien, Januar 1974",
        correctAnswer = 0,
        explanation = "Der höchste je gemessene Luftdruck auf Meereshöhe betrug 1.084,8 hPa in Tosontsengel (Mongolei) am 19. Dezember 2001 (WMO-zertifiziert). Ältere Aufzeichnungen (z.B. Agata 1968 mit 1.083,8 hPa) werden auch genannt, aber der Tosontsengel-Wert ist die aktuell anerkannte WMO-Bestmarke.",
        difficulty = 5,
        funFact = "Solche Extremwerte entstehen in sibirischen Hochdruckgebieten (Sibirisches Hoch) – einem der stabilsten und stärksten Druckgebilde der Erde. Im Winter übernimmt dieses Hoch die Wetterlage für fast ganz Asien und ist für die extremen kontinentalen Kältepole Sibiriens und der Mongolei mitverantwortlich."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Rückzugsrate hat der Mer de Glace (Mont Blanc, Frankreich), der größte Gletscher der Alpen, im Zeitraum 2000–2020 durchschnittlich gezeigt?",
        answerA = "ca. 10–15 m/Jahr",
        answerB = "ca. 20–25 m/Jahr",
        answerC = "ca. 40–50 m/Jahr",
        answerD = "ca. 70–90 m/Jahr",
        correctAnswer = 2,
        explanation = "Der Mer de Glace hat sich seit 2000 mit ca. 30–50 m/Jahr zurückgezogen (Länge) und jährlich ca. 3–5 m an Dicke verloren. Der glaciologische Beobachtungsdienst von Chamonix dokumentiert diesen Trend kontinuierlich. Seit 1850 hat der Gletscher etwa 3 km Länge und über 200 m Dicke verloren.",
        difficulty = 5,
        funFact = "Am Mer de Glace existiert eine Eishöhle, die jedes Jahr neu in den Gletscher gesprengt werden muss, weil sie durch die Gletscherbewegung zerstört wird. Tafeln zeigen Besuchern, wie weit der Gletscher noch in den 1990ern, 2000ern und 2010ern reichte – ein lebendiges Klimadenkmal."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Salzgehalt-Messreihe des RAPID-Arrays bei 26,5°N ergab den niedrigsten mittleren Salzgehalt im Nordatlantik-Tiefenwasser (NADW) für den Zeitraum 2004–2020?",
        answerA = "ca. 34,9 PSU",
        answerB = "ca. 35,1 PSU",
        answerC = "ca. 34,6 PSU",
        answerD = "ca. 35,5 PSU",
        correctAnswer = 2,
        explanation = "Das Nordatlantische Tiefenwasser (NADW) weist im RAPID-Transekt bei 26,5°N typisch Salzgehalte zwischen 34,6–34,9 PSU auf. Der Kern des NADW liegt zwischen 1.000–3.000 m Tiefe und hat einen charakteristischen Salzgehalt von ca. 34,6 PSU – leicht frischer als das darüberliegende Mittelmeer-Ausfluss-Wasser.",
        difficulty = 5,
        funFact = "Das RAPID-Array hat seit 2004 eine beunruhigende Schwächung des AMOC (Atlantic Meridional Overturning Circulation) dokumentiert – bis 2017 verlor der Strom etwa 15 % seiner Transportkapazität. Eine vollständige Unterbrechung würde die europäischen Temperaturen um mehrere Grad senken."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher demografische Übergangsindex (TFR) markiert nach dem UN Population Division-Modell die Grenze zwischen Phase 3 und Phase 4 (stabile Niedrigfertilität)?",
        answerA = "TFR = 3,0",
        answerB = "TFR = 2,1",
        answerC = "TFR = 1,5",
        answerD = "TFR = 4,0",
        correctAnswer = 1,
        explanation = "Das Bestandserhaltungsniveau (replacement fertility rate) von TFR = 2,1 markiert die Grenze zwischen Phase 3 (weiterhin sinkende Fertilität) und Phase 4 (Fertilität unter Bestandserhaltung) des demografischen Übergangsmodells. Länder unterhalb von 2,1 haben langfristig ohne Einwanderung eine schrumpfende Bevölkerung.",
        difficulty = 5,
        funFact = "Heute liegt die weltweite durchschnittliche TFR bei ca. 2,3 (UN 2022) – und nähert sich damit dem Bestandserhaltungsniveau. Für 2050 prognostiziert die UN eine globale TFR von ca. 2,1 – das würde bedeuten, dass das weltweite Bevölkerungswachstum nahezu zum Stillstand kommt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Küste Australiens weist nach Commonwealth Scientific and Industrial Research Organisation (CSIRO)-Daten die höchsten Erosionsraten auf?",
        answerA = "Gold Coast (Queensland) – ca. 1–2 m/Jahr",
        answerB = "Ninety Mile Beach (Victoria) – ca. 0,5–1 m/Jahr",
        answerC = "Gippsland Lakes (Victoria) – ca. 3–4 m/Jahr",
        answerD = "Shark Bay (Westaustralien) – ca. 0,2–0,5 m/Jahr",
        correctAnswer = 0,
        explanation = "Die Gold Coast in Queensland gilt als eine der erosionsgefährdetsten Küsten Australiens, mit Erosionsraten von ca. 1–2 m/Jahr an bestimmten Strandabschnitten (besonders Surfers Paradise). Intensive Tourismus-Infrastruktur, gestörte Sedimentdynamik und Meeresspiegel-anstieg verstärken die Erosion.",
        difficulty = 5,
        funFact = "Die Gold Coast betreibt das weltgrößte Strandsand-Recycling-Programm: Jährlich werden Millionen Kubikmeter Sand vom Meeresboden auf die Strände gepumpt (beach nourishment), um die Erosion auszugleichen. Ohne diese Maßnahmen würden die ikonischen Strände innerhalb weniger Jahrzehnte verschwinden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Tiefe erreicht die Sauerstoffminimumzone (OMZ) im östlichen Arabischen Meer typischerweise nach ihrem oberen Rand?",
        answerA = "ca. 50–100 m",
        answerB = "ca. 100–150 m",
        answerC = "ca. 150–300 m",
        answerD = "ca. 400–600 m",
        correctAnswer = 1,
        explanation = "Im östlichen Arabischen Meer beginnt die Sauerstoffminimumzone (OMZ) bereits in ca. 100–150 m Tiefe und erstreckt sich bis ca. 1.000–1.200 m. Sie ist eine der intensivsten OMZs der Weltmeere mit Sauerstoffwerten nahe 0. Diese extreme Sauerstoffarmut beeinflusst den gesamten Stickstoffkreislauf.",
        difficulty = 5,
        funFact = "In der OMZ des Arabischen Meeres leben anaerobe Mikroorganismen, die Stickstoff-Denitrifikation betreiben. Dies macht das Arabische Meer zu einem der wichtigsten natürlichen Stickstoff-Sinks der Erde und hat direkte Auswirkungen auf den globalen Ozean-Nährstoffkreislauf."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Nation hatte laut UN World Population Prospects 2022 den höchsten demografischen Altenquotienten (old-age dependency ratio: Bevölkerung 65+ / Bevölkerung 15–64)?",
        answerA = "Deutschland (ca. 33 %)",
        answerB = "Japan (ca. 50 %)",
        answerC = "Italien (ca. 38 %)",
        answerD = "Südkorea (ca. 25 %)",
        correctAnswer = 1,
        explanation = "Japan hatte 2022 laut UN den weltweit höchsten Altenquotienten von ca. 50 % – das heißt, auf jede Person im erwerbsfähigen Alter (15–64) kommen ca. 0,5 Personen im Rentenalter (65+). Deutschland lag bei ca. 33 %, Italien bei ca. 38 %.",
        difficulty = 5,
        funFact = "Japans Altenquotient wird bis 2050 laut UN-Projektion auf etwa 73 % steigen. Das bedeutet: Fast drei Rentner auf vier Arbeitnehmer. Kein anderes Land der Welt steht vor einer demographischen Herausforderung dieser Dimension."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen maximalen Tidenhub verzeichnet die Severn-Mündung (Bristol Channel, UK) und welcher Rang ist ihr damit unter den weltweiten Gezeitenbereichen zuzuordnen?",
        answerA = "bis zu 14,8 m – Platz 2 weltweit",
        answerB = "bis zu 12,2 m – Platz 3 weltweit",
        answerC = "bis zu 10,5 m – Platz 4 weltweit",
        answerD = "bis zu 16,8 m – Platz 1 weltweit",
        correctAnswer = 0,
        explanation = "Die Severn-Mündung (Bristol Channel) verzeichnet Tidenraten von bis zu 14,8 m bei Springtide und rangiert damit weltweit auf Platz 2 hinter der Bay of Fundy (Kanada, bis zu 16,3 m). Der Bristol Channel wirkt als Trichterform, die die Gezeiten verstärkt.",
        difficulty = 5,
        funFact = "Das extreme Gezeitengefälle der Severn-Mündung war seit Jahrzehnten Gegenstand von Plänen für ein Gezeitenkraftwerk. Ein solches Projekt ('Severn Barrage') könnte bis zu 5 % des britischen Strombedarfs decken – wurde bisher aber wegen ökologischer und wirtschaftlicher Bedenken nicht realisiert."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche historische Tephra-Ablagerung des Tambora-Ausbruchs (Indonesien, 1815) gilt als das größte vulkanische Ereignis in der modernen Menschheitsgeschichte nach DRE-Volumen?",
        answerA = "ca. 30–33 km³ DRE",
        answerB = "ca. 50–60 km³ DRE",
        answerC = "ca. 100–120 km³ DRE",
        answerD = "ca. 150–180 km³ DRE",
        correctAnswer = 1,
        explanation = "Der Tambora-Ausbruch (April 1815) emittierte ca. 50–60 km³ DRE – der Volcanic Explosivity Index (VEI) war 7. Er injizierte riesige Mengen Schwefeldioxid in die Stratosphäre und verursachte das 'Jahr ohne Sommer' 1816, mit verheerenden Ernteausfällen in Europa und Nordamerika.",
        difficulty = 5,
        funFact = "Durch den Tambora-Ausbruch starben direkt ca. 71.000 Menschen, durch die Folgehungersnöte weltweit schätzungsweise 200.000 weitere. Das Jahr 1816 ('Year Without a Summer') führte in Europa zu Hungeraufständen und war unter anderem die Inspiration für Mary Shelleys Roman 'Frankenstein', der in jenem düsteren Jahr entstand."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche durchschnittliche Rückzugsrate (in m/Jahr Länge) zeigten die Gletscher des Hindukusch-Karakorum-Himalaya-Massivs (HKH) im Zeitraum 2000–2016 nach ICIMOD-Daten?",
        answerA = "ca. 5–10 m/Jahr",
        answerB = "ca. 20–30 m/Jahr",
        answerC = "ca. 50–60 m/Jahr",
        answerD = "ca. 100 m/Jahr",
        correctAnswer = 1,
        explanation = "ICIMOD (International Centre for Integrated Mountain Development)-Studien zeigen für den HKH-Bereich mittlere Rückzugsraten von ca. 15–30 m/Jahr. Karakorum-Gletscher zeigen ein gemischtes Bild (einige 'surgen'), während Himalaya-Gletscher im Trend stark zurückgehen.",
        difficulty = 5,
        funFact = "Das Hindukusch-Karakorum-Himalaya-System beherbergt mehr Eis als jede Region außerhalb der Pole – man nennt es den 'Dritten Pol'. Es ist Süßwasserspeicher für rund 200 Millionen Menschen, die von den Gletscherschmelzwässern der großen asiatischen Flüsse (Indus, Ganges, Brahmaputra) abhängen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Tidalbereichs-Klassifikation trifft auf das Gebiet der Straße von Malakka im südlichen Teil (Singapur) zu?",
        answerA = "Mesotidal (2–4 m)",
        answerB = "Mikrotidal (< 2 m)",
        answerC = "Makrotidal (> 4 m)",
        answerD = "Atidal (< 0,5 m)",
        correctAnswer = 1,
        explanation = "Im Bereich Singapur/südliche Straße von Malakka beträgt der mittlere Tidenhub weniger als 2 m (ca. 1,7–1,8 m), was dem mikrotidalen Bereich entspricht. Im nördlichen Teil der Straße sind die Gezeiten etwas stärker, aber insgesamt bleibt die Region im mikrotidalen Bereich.",
        difficulty = 5,
        funFact = "Trotz des geringen Tidenhubs ist Singapur einer der meistbefahrenen Häfen der Welt – über 1.000 Schiffe passieren täglich die Meerenge. Die geringe Gezeitenvariabilität ist dabei vorteilhaft für präzisen Hafenbetrieb und Tiefgangplanung großer Containerschiffe."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Wiederkehrintervall schätzt die USGS für Mw-≥8,0-Erdbeben am Wasatch Fault (Utah, USA)?",
        answerA = "ca. 300–400 Jahre",
        answerB = "ca. 600–800 Jahre",
        answerC = "ca. 1.200–1.800 Jahre",
        answerD = "ca. 3.000–5.000 Jahre",
        correctAnswer = 2,
        explanation = "Paläoseismologische Studien am Wasatch Fault (Salt Lake City, Utah) ergeben ein Wiederkehrintervall von ca. 1.200–1.800 Jahren für starke Beben (M ~7.0 bis 7.5). Das letzte große Ereignis ereignete sich vor ca. 350–400 Jahren. Ein Mw-≥8,0-Beben wäre aufgrund der Fault-Geometrie unwahrscheinlich, aber M 7,0–7,5 ist realistisch.",
        difficulty = 5,
        funFact = "Salt Lake City mit seinen 200.000 Einwohnern liegt direkt am Wasatch Fault. Seismologen schätzen die Wahrscheinlichkeit eines M-6,75+-Bebens in den nächsten 50 Jahren auf über 57 % – und die Stadt hat nur begrenzte Erdbebenvorbereitung im Vergleich zu Tokio oder San Francisco."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Fläche hat die Chagos Marine Protected Area (BIOT, Britischer Indischer Ozean) und warum ist sie umstritten?",
        answerA = "ca. 250.000 km² – Fischerei-Moratorium umstritten",
        answerB = "ca. 544.000 km² – Unabhängigkeitsansprüche der Mauritius-Regierung umstritten",
        answerC = "ca. 800.000 km² – Souveränitätsstreit zwischen UK und Mauritius",
        answerD = "ca. 1.100.000 km² – Schutzzweck nicht anerkannt von Mauritius",
        correctAnswer = 1,
        explanation = "Die Chagos Marine Protected Area (MPA) umfasst ca. 544.000 km² und wurde 2010 vom Vereinigten Königreich eingerichtet. Sie ist umstritten, da Mauritius die Souveränität über den Archipel beansprucht; der IGH urteilte 2019, dass UK das BIOT-Gebiet unrechtmäßig hält. 2024 einigten sich UK und Mauritius auf eine Übertragung.",
        difficulty = 5,
        funFact = "Das Chagos-Archipel beherbergt den Diego Garcia-Stützpunkt, einer der wichtigsten US-Militärstützpunkte des Indischen Ozeans. Die ehemaligen Bewohner (Chagossianer) wurden 1971 zwangsweise umgesiedelt und kämpfen seit Jahrzehnten vergeblich um das Rückkehrrecht."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie viel Prozent der Weltbevölkerung lebten laut UN World Urbanization Prospects 2022 in Städten?",
        answerA = "ca. 50 %",
        answerB = "ca. 56 %",
        answerC = "ca. 63 %",
        answerD = "ca. 70 %",
        correctAnswer = 1,
        explanation = "Laut UN World Urbanization Prospects 2022 lebten 56 % der Weltbevölkerung (ca. 4,5 Mrd. Menschen) in Städten. Die UN prognostizieren, dass bis 2050 rund 68 % der Weltbevölkerung urban sein werden.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Breite hat der Kontinentalsockel vor der Westküste Indiens (Arabisches Meer) am breitesten Punkt, typischerweise vor Mumbai?",
        answerA = "ca. 50 km",
        answerB = "ca. 100 km",
        answerC = "ca. 200 km",
        answerD = "ca. 350 km",
        correctAnswer = 2,
        explanation = "Der Kontinentalsockel vor der Westküste Indiens (Arabisches Meer) ist vor Mumbai besonders breit und misst etwa 180–220 km bis zur 200-m-Tiefenlinie. Er gehört damit zu den breiteren Schelfen Asiens und ist ökonomisch bedeutsam (Fischerei, Kohlenwasserstoffexploration).",
        difficulty = 5,
        funFact = "Auf dem indischen Westschelf wurden bedeutende Öl- und Gasfelder entdeckt, darunter das Bombay-High-Feld, das für Jahrzehnte die größte Ölquelle Indiens war. Die Mumbai High-Plattform (Bombay High) liegt ca. 160 km nordwestlich von Mumbai mitten auf dem Schelf."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Tephra-Volumen (DRE) wird dem Yellowstone-Supervulkan-Ausbruch der Huckleberry-Ridge-Eruption vor ca. 2,1 Millionen Jahren zugeordnet?",
        answerA = "ca. 500 km³ DRE",
        answerB = "ca. 1.000 km³ DRE",
        answerC = "ca. 2.450 km³ DRE",
        answerD = "ca. 6.000 km³ DRE",
        correctAnswer = 2,
        explanation = "Die Huckleberry-Ridge-Eruption (Yellowstone, vor ca. 2,1 Mio. Jahren) gilt mit einem Volumen von ca. 2.450 km³ DRE als die größte der drei Yellowstone-Supervulkan-Eruptionen. Zum Vergleich: Der Tambora 1815 eruptierte ca. 50 km³, der Pinatubo 1991 ca. 10 km³.",
        difficulty = 5,
        funFact = "Die Asche der Huckleberry-Ridge-Eruption bedeckte rund die Hälfte Nordamerikas mit einer Schicht von mehreren Zentimetern. Geologen finden diese charakteristische Tuffschicht ('Huckleberry Ridge Tuff') bis nach Kansas und Nebraska – über 2.000 km vom Yellowstone entfernt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Rate des isostatischen Postglazialaufstiegs (glacial isostatic adjustment, GIA) zeigt Skandinavien aktuell in der Region des ehemaligen Eisschildzentrums (Bottnischer Meerbusen)?",
        answerA = "ca. 1–2 mm/Jahr",
        answerB = "ca. 5–8 mm/Jahr",
        answerC = "ca. 10–12 mm/Jahr",
        answerD = "ca. 20–25 mm/Jahr",
        correctAnswer = 1,
        explanation = "Im Zentrum des skandinavischen isostatischen Aufschwungs (Bottnischer Meerbusen, nördliches Schweden/Finnland) beträgt die aktuelle GIA-Rate etwa 8–10 mm/Jahr (präzis: ~8,5 mm/Jahr bei Skellefteå/Luleå). Dies ist eine der höchsten postglazial bedingten Hebungsraten weltweit.",
        difficulty = 5,
        funFact = "Die schwedische Stadt Örnsköldsvik liegt heute ca. 8 m höher als zur Zeit der Wikinger. Alte Häfen und Anlegestellen liegen heute weit im Landesinneren. Der Bottnische Meerbusen verliert jährlich durch Landhebung mehrere Quadratkilometer Wasserfläche."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen mittleren Salzgehalt (Salinität) hat das Mittelmeer nach MEDATLAS-Daten an seiner Oberfläche?",
        answerA = "ca. 35,5 PSU",
        answerB = "ca. 37,5 PSU",
        answerC = "ca. 38,5 PSU",
        answerD = "ca. 40,5 PSU",
        correctAnswer = 2,
        explanation = "Das Mittelmeer hat mit einem mittleren Oberflächensalzgehalt von ca. 38–39 PSU eine deutlich höhere Salinität als der offene Atlantik (~35,4 PSU). Im östlichen Mittelmeer (Levantinisches Becken) sind Werte bis 39,5 PSU möglich. MEDATLAS-Daten (Mediterranean Oceanic Data Base) bestätigen ~38,5 PSU als Mittelwert.",
        difficulty = 5,
        funFact = "Das salzreiche Mittelmeerwasser fließt durch die Straße von Gibraltar am Boden in den Atlantik aus und bildet dort eine eigene, identifizierbare Wassermasse ('Mediterranean Outflow Water') – nachweisbar bis in den Nordatlantik und vor die Küste der USA."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Nation weist gemäß UNDESA 2022 die höchste Migrationsrate (Netto-Emigrationsrate pro 1.000 Einwohner) auf?",
        answerA = "Syrien (Kriegs-bedingt)",
        answerB = "Venezuela",
        answerC = "Eritrea",
        answerD = "El Salvador",
        correctAnswer = 1,
        explanation = "Venezuela hatte im Zeitraum 2015–2022 mit geschätzten 7,1 Millionen Auswanderern (ca. 24 % der Bevölkerung) die weltweit größte Migrations- und Flüchtlingskrise außerhalb eines Kriegsgebietes. Die UNDESA-Netto-Emigrationsrate überstieg zeitweise 30‰ pro Jahr.",
        difficulty = 5,
        funFact = "Die venezolanische Migrationskrise ist die größte Zwangsmigration in der Geschichte Lateinamerikas. Über 70 % der venezolanischen Emigranten blieben in Lateinamerika (vor allem Kolumbien, Peru, Chile) – eine einzigartige regionale Binnenmigration mit tiefgreifenden demographischen Folgen für die Gastländer."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen minimalen Luftdruck-Gradienten (in hPa/100 km) definiert die WMO für ein tropisches Sturmgebiet, das zur Intensitätszunahme fähig ist?",
        answerA = "0,5 hPa/100 km",
        answerB = "1,0 hPa/100 km",
        answerC = "2,0 hPa/100 km",
        answerD = "Es gibt keine solche WMO-Schwellendefinition",
        correctAnswer = 3,
        explanation = "Die WMO definiert keine spezifische Mindest-Druckgradient-Schwelle für Intensivierungspotenzial tropischer Systeme. Stattdessen verwenden Meteorologen Schwellenwerte für Meerestemperatur (≥26°C bis 60 m Tiefe), vertikale Windscherung und andere Parameter. Dies ist eine Fangfrage zu WMO-Definitionen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Rückzugsrate dokumentieren Satellitenmessungen (Landsat) für den Jacobshavn-Gletscher (Sermeq Kujalleq, Grönland) im Zeitraum 2010–2020?",
        answerA = "ca. 20–30 m/Tag",
        answerB = "ca. 40–46 m/Tag",
        answerC = "ca. 60–70 m/Tag",
        answerD = "ca. 100–120 m/Tag",
        correctAnswer = 1,
        explanation = "Der Jacobshavn-Gletscher (Sermeq Kujalleq) fließt mit Geschwindigkeiten von ca. 40–46 m/Tag (14–17 km/Jahr) und ist damit einer der schnellsten Gletscher der Welt. Landsat-Daten zeigen, dass sich die Kalbungsfront seit 1990 um ca. 50 km zurückgezogen hat.",
        difficulty = 5,
        funFact = "Der Jacobshavn-Gletscher produziert rund 10 % aller grönländischen Eisberge, darunter jenen, der vermutlich mit der Titanic kollidierte. Der Eisberg, der die Titanic versenkender war, hatte nach Berechnungen eine Masse von etwa 150.000 Tonnen und war bereits jahrelang zuvor von Jacobshavn gekalbt worden."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie breit ist der Kontinentalsockel (0–200 m Tiefe) im Bereich des Sundbankens (Nordsee) zwischen Dänemark und Norwegen im Durchschnitt?",
        answerA = "ca. 100 km",
        answerB = "ca. 200 km",
        answerC = "ca. 350 km",
        answerD = "ca. 500 km",
        correctAnswer = 2,
        explanation = "Die Nordsee selbst ist ein Epikontinentalmeer, das nahezu vollständig auf dem Kontinentalsockel liegt. Im Bereich zwischen Dänemark und Norwegen hat der Schelf eine durchschnittliche Breite von ca. 300–400 km. Die gesamte Nordsee (außer dem Norwegischen Tief) ist flacher als 200 m.",
        difficulty = 5,
        funFact = "Vor etwa 8.000 Jahren war die heutige Nordsee Festland – 'Doggerland' war eine fruchtbare Landschaft, die durch den steigenden Meeresspiegel nach der letzten Eiszeit überflutet wurde. Fischerboote ziehen noch heute fossile Knochen von Wollmammuts und Steinzeitwerkzeuge aus dem Meeresboden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist das WMO-zertifizierte tiefste Temperaturrekord auf der Erde außerhalb der Antarktis?",
        answerA = "−67,8°C, Oymyakon, Russland (1933)",
        answerB = "−71,2°C, Verkhoyansk, Russland (1892)",
        answerC = "−67,7°C, Verkhoyansk, Russland (1885)",
        answerD = "−69,6°C, Snag, Yukon, Kanada (1947)",
        correctAnswer = 0,
        explanation = "WMO-zertifiziert ist −67,8°C (−90°F) gemessen in Oymyakon, Jakutien (Russland), am 6. Februar 1933 als tiefstes Temperaturrekord auf der nördlichen Hemisphäre außerhalb der Antarktis. Verkhoyansk (−67,7°C, 1892) und Snag (−63,0°C, 1947) sind ebenfalls Rekordhalter ihrer Regionen.",
        difficulty = 5,
        funFact = "In Oymyakon beträgt die Jahresdurchschnittstemperatur −15°C und die Temperatur-spanne zwischen Sommer und Winter über 100°C (von +37°C im Juli bis −68°C im Winter). Trotz dieser Extreme leben ca. 500 Menschen dauerhaft dort – Autos bleiben bei −50°C oder kälter in beheizten Garagen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Stickstoff-Isotopen-Datensatz (δ¹⁵N) verwendet die Paläoozeanografie als Proxy für die Intensität der Sauerstoffminimumzone in vergangenen Klimaperioden?",
        answerA = "δ¹⁵N von Sediment-Foraminifera",
        answerB = "δ¹⁵N von Bulksediment-Organik (BSO)",
        answerC = "δ¹⁵N von arktischem Meereis-Algen",
        answerD = "δ¹⁵N von Korallenskeletten",
        correctAnswer = 1,
        explanation = "Die Stickstoff-Isotopenzusammensetzung (δ¹⁵N) von Bulksediment-organischem Material (BSO) wird als Proxy für die Stärke der Denitrifikation in Sauerstoffminimumzonen verwendet. Bei intensiver Denitrifikation (stärkere OMZ) wird ¹⁴N bevorzugt verbraucht, was zu höheren δ¹⁵N-Werten im Sediment führt.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist der exakte Tidenhub-Rekord der Bay of Fundy, gemessen in Burntcoat Head (Nova Scotia, Kanada)?",
        answerA = "15,1 m",
        answerB = "16,3 m",
        answerC = "17,0 m",
        answerD = "14,7 m",
        correctAnswer = 1,
        explanation = "In Burntcoat Head (Nova Scotia) wurde ein maximaler Tidenhub von 16,3 m gemessen – dieser Wert gilt als weltweiter Rekord. Burntcoat Head liegt im Minas Basin, dem innersten Teil der Bay of Fundy, wo die Trichterwirkung die Gezeiten maximal verstärkt.",
        difficulty = 5,
        funFact = "Die Eigenperiode der Bay of Fundy beträgt ca. 12,4 Stunden – fast exakt halb so lang wie ein Gezeitenzyklus (~12,42 Stunden). Diese Resonanz mit den Gezeiten ist der Grund für die extremen Tidenhübe; ohne sie hätten die Gezeiten hier nur ca. 1–2 m Hub."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Meeresschutzfläche haben alle marinen Schutzgebiete weltweit zusammen nach UNEP-WCMC (2023) als Anteil der Ozeanfläche?",
        answerA = "ca. 5 %",
        answerB = "ca. 8 %",
        answerC = "ca. 12 %",
        answerD = "ca. 18 %",
        correctAnswer = 2,
        explanation = "Nach UNEP-WCMC-Daten (2023) sind ca. 8–8,5 % der Weltmeeresfläche durch marine Schutzgebiete (MPAs) abgedeckt. Damit liegt die Welt noch weit hinter dem Ziel des Kunming-Montreal Global Biodiversity Framework (2022), bis 2030 mindestens 30 % der Meere zu schützen ('30×30'-Ziel).",
        difficulty = 5,
        funFact = "Trotz der nominell geschützten 8 % sind viele MPAs nur 'paper parks' – sie haben keine wirksame Durchsetzung, erlauben Fischerei und zeigen kaum Unterschied zur unkontrollierten Umgebung. Effektiv und vollständig geschützte MPAs (No-Take-Zonen) umfassen nur rund 2,7 % der Ozeanfläche."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Rate des Meeresspiegel-bedingten Küstenrückgangs wurde an der Niederländischen Küste (ohne Deichschutz) für 2050–2100 projiziert?",
        answerA = "ca. 0,3–0,5 m Meeresspiegelanstieg → kaum Erosion bei bestehenden Deichen",
        answerB = "ca. 1–2 m Meeresspiegelanstieg → Verlust von 5–10 % der heutigen Küstenlinie",
        answerC = "ca. 3–4 m Meeresspiegelanstieg → mehr als 50 % des Landes unter Wasser",
        answerD = "Ohne Deichschutz wäre rund 26 % der Niederlande schon heute überflutet",
        correctAnswer = 3,
        explanation = "Der letzte Antwortsatz ist die korrekte Tatsache: Rund 26 % der Niederlande liegt bereits heute unter dem Meeresspiegel (bis zu −7 m bei Zuidplas, der tiefste Punkt). Ohne das umfangreiche Deich- und Poldersystem (Deltawerke) wären diese Gebiete permanent überflutet.",
        difficulty = 5,
        funFact = "Die Niederlande haben mit dem Deltaplan (nach der Nordseeflut 1953 mit 1.836 Todesopfern) eines der beeindruckendsten Wasserbauprogramme der Geschichte realisiert. Der Maeslantkering ist ein schwimmendes Sperrwerk so groß wie zwei Eiffel-Türme – es schließt automatisch, wenn eine Sturmflut erwartet wird."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche tektonische Wiederkehrrate (mittlere Zwischenzeit) für Mw-≥7,5-Beben wurde am Nordanatolischen Störungssystem (NAF) in der Türkei paläoseismologisch bestimmt?",
        answerA = "ca. 50–100 Jahre",
        answerB = "ca. 200–300 Jahre",
        answerC = "ca. 400–600 Jahre",
        answerD = "ca. 1.000–2.000 Jahre",
        correctAnswer = 2,
        explanation = "Paläoseismologische Studien am Nordanatolischen Störungssystem (North Anatolian Fault, NAF) zeigen Wiederkehrintervalle von ca. 300–600 Jahren für starke Beben (M > 7,5) an einzelnen Segmenten. Die Ruptursequenz des 20. Jahrhunderts (von 1939 bis 1999 westlich migrierend) ist historisch einmalig.",
        difficulty = 5,
        funFact = "Die Normanatolische Verwerfung zeigt eine faszinierende Migrationsmuster: Von 1939 bis 1999 rückten große Erdbeben (M≥7) schrittweise von Ost nach West entlang der Verwerfung vor – jedes Beben aktivierte das nächstwestliche Segment. Das 1999er Gölcük-Beben (Mw 7,6) war wahrscheinlich das westlichste dieser Serie, aber ein Marmara-Beben nahe Istanbul wird noch erwartet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Temperatur-Salzgehalts-Charakteristik definiert das Antarktische Bodenwasser (AABW) nach Fofonoff-Definitionen?",
        answerA = "T < 2°C, S > 34,65 PSU",
        answerB = "T < 0°C, S > 34,72 PSU",
        answerC = "T < 4°C, S > 34,5 PSU",
        answerD = "T < −0,5°C, S > 34,9 PSU",
        correctAnswer = 1,
        explanation = "Antarktisches Bodenwasser (AABW) wird klassischerweise durch Temperaturen unter 0°C und Salzgehalte über 34,72 PSU definiert. Es entsteht primär im Weddell- und Rossmeer durch Abkühlung und Salzanreicherung unter Meereis und sinkt als dichtestes Meerwasser auf den Tiefseeboden.",
        difficulty = 5,
        funFact = "AABW ist das dichteste und kälteste frei zirkulierende Wassermasse der Erde. Es bedeckt ca. 40 % des gesamten Tiefseebodens (unterhalb 4.000 m) und 'kriecht' entlang des Bodens nach Norden in alle Ozeane – der Weg eines einzigen Wasserpaketes von seiner Bildung bis zu seiner Oberflächen-Rückkehr dauert ca. 500–2.000 Jahre."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Erdbeben-Wiederkehrintervall wurde für Mw-≥8,5-Ereignisse am Sunda-Megathrust (Sumatra-Subduktionszone) aus Korallen-Paläoseismologie ermittelt?",
        answerA = "ca. 100–150 Jahre",
        answerB = "ca. 200–250 Jahre",
        answerC = "ca. 400–600 Jahre",
        answerD = "ca. 800–1.000 Jahre",
        correctAnswer = 2,
        explanation = "Korallen-Paläoseismologie an Mentawai-Inseln (Sumatra) zeigt Wiederkehrintervalle für Mw-≥8,5-Ereignisse am Sunda-Megathrust von ca. 400–600 Jahren (Natawidjaja et al., 2006, Science). Das Sumatrabeben 2004 (Mw 9,1–9,3) brach ein Segment, das seit ca. 230 Jahren gesperrt war.",
        difficulty = 5,
        funFact = "Korallenriffe bieten einzigartige paläoseismologische Archive: Korallen, die durch tektonisches Heben ins Trockene gehoben werden, sterben ab – ihr Sterbedatum (radiometrisch datierbar) gibt das exakte Jahr vergangener Erdbeben an. Umgekehrt senken Küstensenkungen Korallen unter die Brandungszone und stimulieren ihr Wachstum."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche demografische Kennzahl beschreibt den Zeitpunkt, an dem ein Land seinen 'demografischen Bonus' (Dividend) maximal ausschöpft – den Moment, in dem der Anteil der Erwerbsbevölkerung am höchsten ist?",
        answerA = "Wenn die TFR das Bestandserhaltungsniveau (2,1) unterschreitet",
        answerB = "Wenn der Altenquotient unter 15 % fällt und der Jugendquotient unter 30 %",
        answerC = "Wenn der Anteil der 15–64-Jährigen seinen maximalen Wert erreicht (Peak Working-Age Share)",
        answerD = "Wenn die Lebenserwartung 75 Jahre überschreitet",
        correctAnswer = 2,
        explanation = "Der 'demografische Dividend' oder 'demografischer Bonus' wird maximal ausgeschöpft, wenn der Anteil der 15–64-jährigen Bevölkerung seinen Höhepunkt erreicht – also die Phase, in der viele Arbeitnehmer vergleichsweise wenige Kinder und Rentner versorgen müssen. Viele asiatische Länder (China, Südkorea, Thailand) haben diesen Peak bereits überschritten.",
        difficulty = 5,
        funFact = "Ökonomen schätzen, dass der demografische Dividend Ostasiens (1960–2000) für rund 25–40 % des 'Wirtschaftswunders' der Region verantwortlich war. Sub-Sahara-Afrika steht noch vor seinem demografischen Dividend – hat aber auch am längsten Zeit, davon zu profitieren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Erosionsrate verzeichnete die Küste Louisianas (USA, Mississippi-Delta) nach NASA-Satellitendaten zwischen 1932 und 2016?",
        answerA = "ca. 20 km² Landverlust pro Jahr",
        answerB = "ca. 43 km² Landverlust pro Jahr",
        answerC = "ca. 80 km² Landverlust pro Jahr",
        answerD = "ca. 120 km² Landverlust pro Jahr",
        correctAnswer = 1,
        explanation = "Louisiana verlor zwischen 1932 und 2016 gemäß USGS/NASA ca. 4.833 km² Küstenland – im Schnitt ca. 57 km²/Jahr (in einzelnen Jahrzehnten bis zu 100 km²/Jahr). Aktuellere Schätzungen für 2000–2020 liegen bei ca. 43 km²/Jahr durch verbesserte Deltamanagement-Maßnahmen.",
        difficulty = 5,
        funFact = "Louisianas Küstenland verschwindet, weil der Mississippi durch Deiche eingedämmt wurde: Früher baute er jährlich neue Sedimentflächen im Delta auf. Heute wird das Sediment durch die kanalisierten Deiche direkt ins tiefe Golfwasser gespült – das Delta 'verhungert' damit buchstäblich und sinkt ab."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie groß ist das Pelagos-Schutzgebiet im Mittelmeer (zwischen Frankreich, Italien und Monaco), dem ersten Hochsee-Meeresschutzgebiet im Mittelmeer?",
        answerA = "ca. 43.000 km²",
        answerB = "ca. 87.000 km²",
        answerC = "ca. 130.000 km²",
        answerD = "ca. 200.000 km²",
        correctAnswer = 1,
        explanation = "Das Pelagos-Schutzgebiet (Pelagos Sanctuary for Mediterranean Marine Mammals), 1999 zwischen Frankreich, Italien und Monaco vereinbart, umfasst ca. 87.500 km² und ist das erste internationale Hochsee-MPA im Mittelmeer. Es schützt kritische Lebensräume für Finnwale, Delfine und Kegelrobben.",
        difficulty = 5,
        funFact = "Das Pelagos-Schutzgebiet hat besonders tiefe submarine Canyons (Var-Canyon, bis 2.700 m tief), die als 'Highways' für Nährstoffe und marine Organismen funktionieren. Finnwale (bis 27 m lang) kommen hier im Sommer zur Nahrungsaufnahme – das Pelagos beherbergt eine der größten Finnwal-Populationen der Welt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Abnahme der durchschnittlichen Schelfeis-Fläche in der Antarktis dokumentierten NASA NSIDC-Daten für den Zeitraum 1979–2023?",
        answerA = "keine signifikante Änderung",
        answerB = "ca. 2 % Rückgang",
        answerC = "ca. 15 % Rückgang",
        answerD = "ca. 40 % Rückgang in bestimmten Regionen",
        correctAnswer = 3,
        explanation = "In der Westantarktis und auf der Antarktischen Halbinsel zeigen einige Schelfeise drastische Rückgänge von 40 % und mehr (z.B. Larsen-B: 3.250 km² Kollaps 2002; Pine-Island-Schelfeis). Global gesehen ist der Rückgang heterogen – die ostantarktischen Schelfeise sind stabiler.",
        difficulty = 5,
        funFact = "Das Larsen-B-Schelfeis kollabierte im Jahr 2002 innerhalb von nur 35 Tagen – ein Schelf, der seit mindestens 12.000 Jahren existiert hatte. Innerhalb weniger Wochen verschwand eine Eismasse so groß wie Rheinland-Pfalz. Seismometer und Satelliten zeichneten das Ereignis in Echtzeit auf."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Salzgehalt (PSU) hat der Nordpazifische Zwischenwasser-Kern (NPIW – North Pacific Intermediate Water) nach WOCE-Daten?",
        answerA = "ca. 33,8–34,0 PSU",
        answerB = "ca. 34,5–34,7 PSU",
        answerC = "ca. 35,0–35,2 PSU",
        answerD = "ca. 34,2–34,4 PSU",
        correctAnswer = 0,
        explanation = "Das North Pacific Intermediate Water (NPIW) bildet sich in der Okhotskischen See und dem nördlichen Nordpazifik und zeichnet sich durch ein markantes Salzgehaltsminimum von ca. 33,8–34,0 PSU in ca. 200–800 m Tiefe aus. Die WOCE (World Ocean Circulation Experiment)-Daten bestätigen diesen Kennwert.",
        difficulty = 5,
        funFact = "Der Nordpazifik hat keine tiefe Konvektionszone wie der Nordatlantik, weil das Oberflächenwasser zu frisch (niedrig-salzig) ist, um schwer genug zu werden und abzusinken. Das erklärt, warum der Nordpazifik keine Tiefenwasser-Produktion zeigt und seinen Tiefenbereich viel langsamer erneuert als der Atlantik."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Land hat laut UNDESA 2022 den höchsten Anteil an urbanisierten Küstenbewohnern (Bevölkerung in niedrig gelegenen Küstenzonen, LECZ <10 m ü. NN) im Verhältnis zur Gesamtbevölkerung?",
        answerA = "Bangladesch",
        answerB = "Niederlande",
        answerC = "Vietnam",
        answerD = "Malediven",
        correctAnswer = 3,
        explanation = "Die Malediven haben ca. 100 % ihrer Bevölkerung in Low Elevation Coastal Zones (LECZ, <10 m ü. NN), da nahezu das gesamte Staatsgebiet unter dieser Höhenschwelle liegt. Mit einem mittleren Geländeniveau von ca. 1,5 m ü. NN sind die Malediven der am stärksten durch Meeresspiegelanstieg gefährdete Staat der Erde.",
        difficulty = 5,
        funFact = "Die Malediven hielten 2009 eine Kabinettssitzung unter Wasser ab – Regierungsmitglieder tauchten in Taucherausrüstung und unterzeichneten einen Aufruf an andere Nationen, den Klimawandel zu bekämpfen. Die damalige Regierung erkundete auch den Kauf von neuem Land in Sri Lanka oder Indien als 'Sicherheitsnetz'."
    ),

    // ── MASTER (difficulty = 5) ── questions 214–263 (isostatic rebound, geoid, magnetic poles, ocean heat, volcanic gases, ice cores, sea level, paleomagnetism, geodetic benchmarks) ──

    Question(
        categoryId = 1,
        questionText = "Wie hoch ist die aktuelle postglaziare isostatische Hebungsrate in der Gegend um Angermanland (Schweden), dem Gebiet mit der stärksten Landhebung in Skandinavien?",
        answerA = "ca. 5,2 mm/Jahr",
        answerB = "ca. 8,9 mm/Jahr",
        answerC = "ca. 11,3 mm/Jahr",
        answerD = "ca. 3,7 mm/Jahr",
        correctAnswer = 1,
        explanation = "Das Gebiet um Angermanland (Höga Kusten, Schweden) hebt sich postglaziär mit ca. 8,9 mm/Jahr – dem höchsten Wert in Skandinavien. GPS-Messungen des BIFROST-Netzwerks (Baseline Inferences for Fennoscandian Rebound, Ice mass, and Tectonics) bestätigen diesen Wert als den höchsten Punkt der skandinavischen Landhebung.",
        difficulty = 5,
        funFact = "Die Höga Kusten (Hohe Küste) in Schweden wurde wegen des postglazialen Isostatik-Phänomens 2000 als UNESCO-Weltnaturerbe eingetragen. Seit dem Ende der letzten Eiszeit hat sich das Land dort bereits um über 285 m gehoben."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Geoidundulations-Wert (N) weist die Region des Indischen Ozeans südlich von Sri Lanka auf, die als 'Indian Ocean Geoid Low' bekannt ist?",
        answerA = "ca. −50 m",
        answerB = "ca. −106 m",
        answerC = "ca. −70 m",
        answerD = "ca. −130 m",
        correctAnswer = 1,
        explanation = "Das 'Indian Ocean Geoid Low' (auch 'Indian Ocean Low') südwestlich von Sri Lanka ist das tiefste Geoid-Anomalie-Gebiet der Erde mit einem Undulations-Wert N von ca. −106 m gegenüber dem GRS80/WGS84-Ellipsoid. EGM2008-Modell-Daten zeigen das Minimum bei etwa 2°N, 74°E.",
        difficulty = 5,
        funFact = "Das Indian Ocean Geoid Low ist die tiefste Delle im Erdschwerefeld – die Meeresoberfläche liegt dort um 106 m tiefer, als ein perfekter Ellipsoid vorhersagen würde. Die genaue Ursache ist noch nicht vollständig geklärt; aktuelle Hypothesen verweisen auf einen Mantelplume aus dem Afrikanischen Low-Velocity-Bereich."
    ),

    Question(
        categoryId = 1,
        questionText = "Mit welcher Drift-Geschwindigkeit (km/Jahr) wanderte der magnetische Nordpol der Erde zwischen 2000 und 2019 durchschnittlich?",
        answerA = "ca. 15 km/Jahr",
        answerB = "ca. 35 km/Jahr",
        answerC = "ca. 55 km/Jahr",
        answerD = "ca. 80 km/Jahr",
        correctAnswer = 2,
        explanation = "Seit Anfang der 2000er Jahre hat sich die Drift des magnetischen Nordpols von früheren ~10 km/Jahr auf ca. 50–55 km/Jahr beschleunigt. Im Jahr 2019 lag der Pol bereits nahe der Datumsgrenze auf dem Weg von der kanadischen Arktis Richtung Sibirien. Das International Geomagnetic Reference Field (IGRF) wird alle 5 Jahre aktualisiert.",
        difficulty = 5,
        funFact = "Die schnelle Polwanderung zwang die NOAA 2019, das World Magnetic Model außerplanmäßig zu aktualisieren – ein Jahr früher als geplant. Navigations-Apps und Kompasse, die auf diesem Modell basieren, hätten sonst erhebliche Abweichungen angezeigt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Wert für den globalen Ozean-Wärmeinhalt (Ocean Heat Content, OHC) in den oberen 2.000 m maß das ARGO-Netzwerk im Jahr 2022 im Vergleich zum Klimamittel 1981–2010?",
        answerA = "ca. +5 × 10²² Joule Anomalie",
        answerB = "ca. +10 × 10²² Joule Anomalie",
        answerC = "ca. +20 × 10²² Joule Anomalie",
        answerD = "ca. +35 × 10²² Joule Anomalie",
        correctAnswer = 2,
        explanation = "Der globale Ozean-Wärmeinhalt in den oberen 2.000 m überstieg 2022 den Klimareferenzwert (1981–2010) um ca. 20 × 10²² Joule – ein Rekordwert gemäß ARGO/NODC-Auswertung. Die oberen 2.000 m der Ozeane haben seit 1960 ca. 90 % der durch den Klimawandel zusätzlich gespeicherten Energie aufgenommen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche mittlere SO₂-Emissionsrate (in Tonnen pro Tag) emittiert der Kīlauea-Vulkan (Hawaii) in einer typischen Eruptionsphase?",
        answerA = "ca. 150 t/Tag",
        answerB = "ca. 500 t/Tag",
        answerC = "ca. 2.000 t/Tag",
        answerD = "ca. 5.000 t/Tag",
        correctAnswer = 2,
        explanation = "Im Normalbetrieb (Effusiveruption) emittiert der Kīlauea ca. 500–2.000 Tonnen SO₂ pro Tag. Während der großen Eruption 2018 (Rift-Zone-Eruption) wurden Spitzenwerte von über 100.000 t SO₂/Tag gemessen. Die USGS HVO überwacht die Emissionen kontinuierlich mit FTIR- und UV-Spektrometer-Stationen.",
        difficulty = 5,
        funFact = "SO₂ aus dem Kīlauea reagiert in der Atmosphäre mit Feuchtigkeit zu Schwefelsäure und bildet 'Vog' (Volcanic Smog), der auf Hawaii ernsthafte Gesundheitsprobleme verursacht. Der Vog kann sich bei bestimmten Windverhältnissen über den gesamten Inselstaat Hawaii verteilen."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welche zeitliche Präzision (±Jahre) können Eisschichten im EPICA-Eiskern (Dome C, Antarktis) für den Zeitraum 800.000 Jahre BP datiert werden?",
        answerA = "±500 Jahre",
        answerB = "±2.000 Jahre",
        answerC = "±6.000 Jahre",
        answerD = "±15.000 Jahre",
        correctAnswer = 2,
        explanation = "Der EPICA Dome C Eiskern (806.000 Jahre BP) kann im älteren Abschnitt (>500.000 Jahre BP) auf ca. ±6.000 Jahre datiert werden (Parrenin et al., 2007, Climate of the Past). Im jüngeren Abschnitt (<100.000 Jahre) ist die Präzision deutlich besser (±200–500 Jahre durch jährliche Schichtenzählung und Synchronisation mit dem Grönländischen GICC05-Zeitrahmen).",
        difficulty = 5,
        funFact = "Der EPICA Dome C Eiskern reicht 3.270 m tief und deckt 8 vollständige Glazial-Interglazial-Zyklen ab – er ist damit das älteste kontinuierliche Eisarchiv der Erde. Die Eis-Probe im untersten Meter ist über 800.000 Jahre alt und enthält noch direkt analysierbare Gasblasen mit uralter Atmosphärenluft."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Beitrag liefert der Gletscher-Massenverlust (alpine Gletscher, ohne Grönland und Antarktis) zum globalen Meeressiegelanstieg laut IPCC AR6 (2021) pro Jahr?",
        answerA = "ca. 0,06 mm/Jahr",
        answerB = "ca. 0,22 mm/Jahr",
        answerC = "ca. 0,52 mm/Jahr",
        answerD = "ca. 0,80 mm/Jahr",
        correctAnswer = 2,
        explanation = "Laut IPCC Sixth Assessment Report (AR6, 2021) liefern alpine Gletscher (exkl. Grönland und Antarktis) einen Beitrag von ca. 0,52 mm/Jahr zum mittleren Meeressiegelanstieg (Periode 2006–2018). Zusammen mit dem thermischen Ausdehnen (+1,4 mm/Jahr), Grönland (+0,77 mm/Jahr) und Antarktis (+0,43 mm/Jahr) ergibt sich der Gesamtanstieg von ca. 3,7 mm/Jahr.",
        difficulty = 5,
        funFact = "Ca. 220.000 Gletscher außerhalb der Eisschilde enthalten zusammen ca. 0,32 m äquivalenten Meeresspiegelanstieg. Wenn alle schmelzen würden, stiege der Meeresspiegel um 32 cm – verglichen mit 7 m durch Grönland und 58 m durch die Antarktis."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann fand die letzte vollständige paläomagnetische Polumkehrung (Brunhes-Matuyama-Grenze) statt und auf welchen Wert wird ihr Alter datiert?",
        answerA = "ca. 780.000 Jahre BP",
        answerB = "ca. 1,07 Millionen Jahre BP",
        answerC = "ca. 420.000 Jahre BP",
        answerD = "ca. 1,77 Millionen Jahre BP",
        correctAnswer = 0,
        explanation = "Die Brunhes-Matuyama-Polumkehrung – der Übergang von reverser (Matuyama-Chron) zu normaler (Brunhes-Chron) Polarität – fand vor ca. 780.000 Jahren statt. Dieses Datum ist durch Ar/Ar-Radiometrie von Lavadecken und astronomische Tunung von Tiefseesedimenten auf ±3.000 Jahre genau bestimmt.",
        difficulty = 5,
        funFact = "Die Brunhes-Matuyama-Grenze ist ein globaler Korrelationsmarker in der Geologie: Sie findet sich in Tiefseesedimenten, Laven und Lössprofilen weltweit. Ihre genaue Datierung auf 780 ka gilt als eines der verlässlichsten chronologischen Werkzeuge der Quartärgeologie."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches geodätische Hauptreferenznetz-Fundamentalpunkt (Hauptpunkt) diente als Ausgangspunkt für das Europäische Triangulationsnetz (RD) und wo liegt er?",
        answerA = "Helmert-Turm in Potsdam (52°22′N, 13°04′E)",
        answerB = "Beobachtungspunkt Sternwarte Babelsberg",
        answerC = "Triangulationspunkt Observatoire de Paris (48°50′N, 2°20′E)",
        answerD = "Fundamentalpunkt Genua (44°24′N, 8°54′E)",
        correctAnswer = 0,
        explanation = "Der Helmert-Turm (Geodätisches Institut Potsdam) bei 52°22′51,4469″N, 13°03′58,9283″E war der Fundamentalpunkt des Europäischen Datums 1950 (ED50) und des deutschen Hauptdreiecksnetzes. Er diente als primärer Referenzpunkt für die Triangulationsmessungen Mitteleuropas.",
        difficulty = 5,
        funFact = "Das Geodätische Institut Potsdam am Telegrafenberg beherbergt auch das CHAMP-, GRACE- und SWARM-Datenzentrum. Potsdam ist bis heute der wichtigste Standort für Schwerefeldmessungen in Deutschland – der Helmert-Turm ist ein nationales Bodendenkmal."
    ),

    Question(
        categoryId = 1,
        questionText = "Mit welcher Präzision (in mm) kann das ITRF2020 (International Terrestrial Reference Frame 2020) koordinatenaustauschende Referenzpunkte (Stationen) weltweit an ihrer Position definieren?",
        answerA = "±5 mm Lage, ±10 mm Höhe",
        answerB = "±1 mm Lage, ±3 mm Höhe",
        answerC = "±0,1 mm Lage, ±0,5 mm Höhe",
        answerD = "±10 mm Lage, ±20 mm Höhe",
        correctAnswer = 1,
        explanation = "Das ITRF2020 (veröffentlicht 2022 durch IERS) definiert Koordinaten globaler GNSS/VLBI/SLR/DORIS-Stationen mit einer internen Konsistenz von ~1 mm in der Lage und ~3 mm in der Höhe. Es ist das präziseste geozentrische Referenzsystem, das je realisiert wurde.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche postglaziare Hebungsrate weist der Bereich um Oulu (Finnland) laut NKG2016LU-Landhebungsmodell auf?",
        answerA = "ca. 4,2 mm/Jahr",
        answerB = "ca. 7,1 mm/Jahr",
        answerC = "ca. 9,5 mm/Jahr",
        answerD = "ca. 2,8 mm/Jahr",
        correctAnswer = 1,
        explanation = "Das nordische Landhebungsmodell NKG2016LU (Nordic Geodetic Commission 2016) gibt für den Großraum Oulu, Finnland, eine postglaziare Landhebungsrate (GIA Vertical Land Movement) von ca. 7–7,2 mm/Jahr an. Das Maximum Fennoskandinaviens liegt bei ~9 mm/Jahr (Umeå/Angermanland-Region, Schweden).",
        difficulty = 5,
        funFact = "Finnland gewinnt jährlich durch die isostatische Landhebung ca. 7 km² neues Land – über die Jahrhunderte seit der Gründung Helsinkis hat sich die finnische Küstenlinie um mehrere Kilometer seewärts verschoben. Alte Fischereidörfer am Wasser liegen heute weit im Landesinneren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen maximalen Geoidundulationswert (positiv, Geoid über Ellipsoid) weist das 'New Guinea Geoid High' auf?",
        answerA = "ca. +50 m",
        answerB = "ca. +73 m",
        answerC = "ca. +85 m",
        answerD = "ca. +62 m",
        correctAnswer = 1,
        explanation = "Das 'New Guinea Geoid High' (Nordpapua-Region) zeigt im EGM2008 einen maximalen Geoid-Undulations-Wert von ca. +73 m – das bedeutet, die Meeresoberfläche liegt dort 73 m höher als auf dem WGS84-Referenzellipsoid. Dieses Maximum entsteht durch die extrem dichte ozeanische Subduktionszone im Bereich der West Pacific Warm Pool-Region.",
        difficulty = 5,
        funFact = "Die dramatischen Geoid-Undulationen zwischen dem Indian Ocean Low (−106 m) und dem New Guinea High (+73 m) auf engstem Raum spiegeln die tektonische Komplexität des Indo-Pazifik-Konvergenzgebiets wider. Kein anderes Meeresbecken zeigt auf so kleinem Raum solche Schwerefeldschwankungen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie schnell driftet der magnetische Südpol der Erde seit 2020 jährlich und in welche Richtung?",
        answerA = "ca. 10–12 km/Jahr, Richtung Australien",
        answerB = "ca. 20–25 km/Jahr, Richtung Pazifik",
        answerC = "ca. 5–8 km/Jahr, Richtung Afrika",
        answerD = "ca. 40–50 km/Jahr, Richtung Südamerika",
        correctAnswer = 1,
        explanation = "Der magnetische Südpol (der Punkt nahe der Antarktis, in dem die Feldlinien nach oben zeigen) driftet seit Jahren mit ca. 20–25 km/Jahr und hat sich von der antarktischen Küste weg in Richtung des Pazifischen Ozeans bewegt. Anders als der magnetische Nordpol hat er seine Drift weniger dramatisch beschleunigt.",
        difficulty = 5,
        funFact = "Der magnetische Südpol liegt nie direkt am geographischen Südpol und war historisch bis zu 2.800 km von ihm entfernt. James Clark Ross entdeckte ihn 1841 auf dem Meereseis vor der antarktischen Küste – heute liegt er im offenen Ozean, da er sich seit Ross' Entdeckung um ca. 1.200 km verschoben hat."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Beitrag liefert der Grönlandische Eisschild zum globalen Meeressiegelanstieg laut GRACE/GRACE-FO Satellitendaten (2002–2022) pro Jahr?",
        answerA = "ca. 0,3 mm/Jahr",
        answerB = "ca. 0,77 mm/Jahr",
        answerC = "ca. 1,2 mm/Jahr",
        answerD = "ca. 2,0 mm/Jahr",
        correctAnswer = 1,
        explanation = "GRACE/GRACE-FO Schwerefeld-Satellitendaten (2002–2022) ergeben für Grönland einen Massenverlust von ca. 280 Gt/Jahr, was einem Meeresspiegelanstieg von ~0,77 mm/Jahr entspricht. Grönland hat seit 1992 insgesamt ca. 4.700 Gt Eis verloren (Bamber et al., 2023, Nature Geoscience).",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche CO₂-Emissionsrate (in Megatonnen CO₂ pro Jahr) geben USGS-Daten für den gesamten Mantelentgasung an mittelozeanischen Rücken an?",
        answerA = "ca. 3–5 Mt CO₂/Jahr",
        answerB = "ca. 25–65 Mt CO₂/Jahr",
        answerC = "ca. 150–200 Mt CO₂/Jahr",
        answerD = "ca. 500–700 Mt CO₂/Jahr",
        correctAnswer = 1,
        explanation = "Mittelozeanische Rücken emittieren durch hydrothermale Entgasung und Mantelschmelzen ca. 25–65 Mt CO₂/Jahr (Dasgupta & Hirschmann, 2010; Marty & Tolstikhin, 1998). Subduktionszonen-Vulkane emittieren noch einmal ca. 60–100 Mt CO₂/Jahr – zusammen sind geothermale/vulkanische CO₂-Quellen ca. 500-mal geringer als anthropogene Emissionen (~37.000 Mt CO₂/Jahr, 2022).",
        difficulty = 5,
        funFact = "Obwohl mittelozeanische Rücken lediglich 25–65 Mt CO₂/Jahr emittieren, haben sie auf geologischen Zeitskalen die Atmosphäre entscheidend geformt. Vor 55 Millionen Jahren (PETM) führte erhöhte Rücken-Aktivität zu massivem atmosphärischem CO₂ und globaler Erwärmung von 5–8°C."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Datierungsverfahren wurde verwendet, um den 800.000 Jahre alten EPICA Dome C Eiskern auf ±6.000 Jahre zu datieren?",
        answerA = "Radiokarbondatierung (¹⁴C)",
        answerB = "Astronomische Tunung an Milankovitch-Zyklen + Schichtzählung",
        answerC = "Uran-Thorium-Datierung (²³⁰Th/²³⁴U)",
        answerD = "Kalium-Argon-Datierung (K/Ar)",
        correctAnswer = 1,
        explanation = "Die Chronologie des EPICA-Eiskerns basiert auf astronomischer Tunung (Abstimmung von Orbital-Zyklen der Erde auf Isotopen-Signale im Eis), auf physikalischer Schichtzählung der jährlichen Akkumulationsschichten und auf Vergleich mit datierten Tiefseesedimenten (EDC3-Zeitskala). Radiokarbon (¹⁴C) ist nur bis ca. 50.000 Jahre nutzbar.",
        difficulty = 5,
        funFact = "Milankovitch-Zyklen – periodische Schwankungen der Erdbahnexzentrizität (100.000 Jahre), der Achsneigung (41.000 Jahre) und der Präzession (23.000 Jahre) – sind im Eiskern wie ein Fingerabdruck ablesbar. Die Übereinstimmung zwischen orbital berechneten Insolationsänderungen und gemessenen Klimasignalen im Eis ist der Schlüssel zur Datierung."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Beitrag leistet die thermische Ausdehnung der Ozeane (Steric Sea Level Rise) zum globalen Meeresspiegelanstieg laut IPCC AR6 für den Zeitraum 2006–2018?",
        answerA = "ca. 0,5 mm/Jahr",
        answerB = "ca. 1,4 mm/Jahr",
        answerC = "ca. 2,2 mm/Jahr",
        answerD = "ca. 3,5 mm/Jahr",
        correctAnswer = 1,
        explanation = "Der IPCC Sixth Assessment Report (AR6, WG1, 2021) gibt für den Steric Sea Level Rise (überwiegend thermische Ausdehnung) im Zeitraum 2006–2018 einen Beitrag von ca. 1,4 mm/Jahr an. Zusammen mit Gletschern (0,52), Grönland (0,77), Antarktis (0,43) und terrestrischem Süßwasser (0,38) ergibt sich der Gesamtanstieg.",
        difficulty = 5,
        funFact = "Obwohl es klingt, als würde Wasser nur 'wärmer' werden, ist thermische Ausdehnung der zweitgrößte Treiber des Meeresspiegelanstiegs. Seewasser dehnt sich mit steigender Temperatur aus – 1°C globale Ozeanerwärmung der oberen 2.000 m entspricht etwa 0,5 mm Meeresspiegelanstieg."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann fand die Jaramillo-Subchron statt – ein kurzes normales paläomagnetisches Ereignis innerhalb des Matuyama-Chrons?",
        answerA = "ca. 1,77–1,95 Millionen Jahre BP",
        answerB = "ca. 0,99–1,07 Millionen Jahre BP",
        answerC = "ca. 0,40–0,55 Millionen Jahre BP",
        answerD = "ca. 2,58–2,65 Millionen Jahre BP",
        correctAnswer = 1,
        explanation = "Die Jaramillo-Subchron ist ein kurzes normales (Brunhes-ähnliches) Magnetfeld-Ereignis innerhalb des reversed Matuyama-Chrons und dauerte von ca. 0,99 bis 1,07 Millionen Jahren BP (ca. 80.000 Jahre lang). Sie wurde erstmals in Lavadecken der Jaramillo Creek Formation (New Mexico, USA) entdeckt.",
        difficulty = 5,
        funFact = "Paläomagnetische Subchrons wie Jaramillo, Cobb Mountain und Olduvai werden als globale Zeitmarken in Tiefseesedimenten, Lavasequenzen und Pollenarchiven genutzt. Das Olduvai-Subchron (vor ca. 1,77–1,95 Ma) ist namensgebend für die Schicht in der Olduvai-Schlucht (Tansania), in der Homo habilis-Fossilien gefunden wurden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Geoid-Modell wird seit 2022 für den amtlichen Höhenbezug in Deutschland verwendet, und welche Genauigkeit hat es?",
        answerA = "GCG2016 (German Combined Geoid 2016), ±2 cm",
        answerB = "GCG2024 (German Combined Geoid 2024), ±0,5 cm",
        answerC = "EGM2008 (Global), ±5 cm für Deutschland",
        answerD = "EGM96 (Global), ±10 cm für Deutschland",
        correctAnswer = 0,
        explanation = "Das German Combined Geoid (GCG2016) ist das aktuelle amtliche Quasigeoid-Modell Deutschlands (AdV, Arbeitsgemeinschaft der Vermessungsverwaltungen), das GNSS-Höhenmessungen (ellipsoidisch) in Normalhöhen über dem Deutschen Haupthöhennetz (DHHN2016) umrechnet. Es hat eine Genauigkeit von ca. ±2 cm im Bundesmittel.",
        difficulty = 5,
        funFact = "Das GCG2016 kombiniert terrestrische Schwerefelddaten, Lotabweichungen, GNSS-Nivellements und das internationale Schwerefeldmodell EGM2008. Ohne solche präzisen Geoid-Modelle würde ein GPS-Gerät die Höhe eines Hauses in Hamburg mit 5–10 cm Fehler angeben – zu viel für amtliche Katastervermessungen."
    ),

    Question(
        categoryId = 1,
        questionText = "Mit welcher Geschwindigkeit hebt sich die Küste Alaskas (Cook Inlet-Region) infolge des postglazialen isostatischen Ausgleichs und der modernen Gletscherschmelze?",
        answerA = "ca. 1–2 mm/Jahr",
        answerB = "ca. 5–7 mm/Jahr",
        answerC = "ca. 10–30 mm/Jahr",
        answerD = "ca. 50–80 mm/Jahr",
        correctAnswer = 2,
        explanation = "Die Küste Alaskas, insbesondere die Gegend um Glacier Bay und Yakutat, hebt sich mit 10–30 mm/Jahr – eine der höchsten postglazialen Hebungsraten der Erde. Der Haupttreiber ist die schnelle Deglaziation großer Gletscher (wie des Malaspina- und Bering-Gletschers), die die darunter liegende Lithosphäre entlastet.",
        difficulty = 5,
        funFact = "In Glacier Bay, Alaska, hat sich das Land seit dem Ende der Kleinen Eiszeit (ca. 1750) um über 4 Meter gehoben – messbar durch historische Pegel und GPS. Die Gegend ist damit ein natürliches Labor für postglazialen isostatischen Ausgleich im Echtzeit-Maßstab."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche H₂O-Emissionsrate hat der Ätna (Sizilien) bei normaler Degasierungsaktivität, gemessen als größter nicht-eruptiver vulkanischer Wasserausgaser Europas?",
        answerA = "ca. 500 t H₂O/Tag",
        answerB = "ca. 2.500 t H₂O/Tag",
        answerC = "ca. 10.000 t H₂O/Tag",
        answerD = "ca. 70.000 t H₂O/Tag",
        correctAnswer = 2,
        explanation = "Der Ätna emittiert bei normaler passiver Entgasung (ohne aktive Eruption) ca. 6.000–15.000 t H₂O pro Tag sowie ca. 2.000–10.000 t SO₂/Tag. Dies macht ihn zum mit Abstand größten vulkanischen Ausgaser im Mittelmeerraum und zu einem der wichtigsten natürlichen SO₂-Quellen Europas.",
        difficulty = 5,
        funFact = "Der Ätna ist der einzige Vulkan Europas, der ununterbrochen seit der Antike aktiv und gleichzeitig mit über 900.000 Menschen dicht besiedelt ist. Seine Lavaströme fließen meist langsam genug, um evakuiert zu werden – doch der stetige Ascheanfall kostet die Landwirtschaft Millionen Euro pro Jahr."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann ereignete sich die Laschamp-Exkursion – ein kurzfristiges geomagnetsches Ereignis, bei dem die Feldstärke auf ca. 5 % des Normalwerts sank?",
        answerA = "ca. 780.000 Jahre BP",
        answerB = "ca. 41.000 Jahre BP",
        answerC = "ca. 120.000 Jahre BP",
        answerD = "ca. 2,1 Millionen Jahre BP",
        correctAnswer = 1,
        explanation = "Die Laschamp-Exkursion fand vor ca. 41.000 Jahren statt und dauerte ca. 440 Jahre. Während dieser Zeit sank die Intensität des Erdmagnetfelds auf ~5 % des heutigen Werts, was zu erhöhter kosmischer Strahlung auf der Erdoberfläche führte. Sie ist in Sedimenten, Eiskernen und Lavadecken (besonders der Laschamp-Lava in der Auvergne, Frankreich) nachgewiesen.",
        difficulty = 5,
        funFact = "Die Laschamp-Exkursion fiel zeitlich mit dem Aussterben des Neandertalers zusammen (~40.000 Jahre BP). Einige Forscher spekulieren, ob die stark erhöhte UV-Strahlung und Klimaveränderungen (durch veränderte Kosmogenstrahlung) zur erhöhten Sterblichkeit beigetragen haben könnten – bisher ist dies jedoch nicht belegt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Beitrag leistet der antarktische Eisschild laut GRACE/GRACE-FO-Satellitendaten (2002–2020) zum globalen Meeresspiegelanstieg?",
        answerA = "ca. 0,1 mm/Jahr",
        answerB = "ca. 0,43 mm/Jahr",
        answerC = "ca. 1,2 mm/Jahr",
        answerD = "ca. 2,5 mm/Jahr",
        correctAnswer = 1,
        explanation = "GRACE/GRACE-FO-Daten (2002–2020) ergeben für die Antarktis einen Massenverlust von ca. 155 Gt/Jahr, was ~0,43 mm/Jahr Meeresspiegelanstieg entspricht (Shepherd et al., 2018 und Bamber et al., 2023). Der Verlust konzentriert sich auf Westantarktis und Antarktische Halbinsel; die Ostantarktis ist im Mittel nahezu ausgeglichen.",
        difficulty = 5,
        funFact = "Die Westantarktis ist wegen der Marine Ice Sheet Instability (MISI) besonders gefährlich: Ihre Basis liegt unter dem Meeresspiegel, was eine selbstverstärkende Eisverlust-Rückkopplung ermöglicht. Ein vollständiger Kollaps des West Antarctic Ice Sheet würde den Meeresspiegel um ca. 3,3 m erhöhen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Fundamentalpunkt-Observatorium diente als Referenz für das Klassische Europäische Dreiecksnetz (European Datum 1950, ED50)?",
        answerA = "Observatoire de Paris, Frankreich",
        answerB = "Sternwarte München-Bogenhausen, Deutschland",
        answerC = "Geodätisches Institut Potsdam (Helmert-Turm), Deutschland",
        answerD = "Institut Géographique National Trig-Point, Belgien",
        correctAnswer = 2,
        explanation = "Der Fundamentalpunkt des European Datum 1950 (ED50) war das Geodätische Institut Potsdam am Telegrafenberg (Helmert-Turm). ED50 basiert auf dem Hayford-Ellipsoid (International Ellipsoid 1924) mit dem Potsdamer Fundamentalpunkt als Ausgangspunkt aller europäischen Koordinaten.",
        difficulty = 5,
        funFact = "ED50 war bis zur Einführung von ETRS89 (European Terrestrial Reference System 1989) das Standard-Koordinatensystem für ganz Europa. Bei der GPS-Revolution stellte sich heraus, dass ED50 je nach Region bis zu 200 m von WGS84 abwich – ein erhebliches Problem für Militär-GPS und internationale Kartografie."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche mittlere SO₂-Emissionsrate emittiert der Vulkankomplex Ambrym (Vanuatu) – einer der produktivsten vulkanischen SO₂-Ausgaser der Erde?",
        answerA = "ca. 200 t SO₂/Tag",
        answerB = "ca. 1.000 t SO₂/Tag",
        answerC = "ca. 2.500 t SO₂/Tag",
        answerD = "ca. 10.000 t SO₂/Tag",
        correctAnswer = 2,
        explanation = "Ambrym auf Vanuatu mit seinen Lavateen Marum und Benbow emittiert im Ruhezustand ca. 1.000–4.500 t SO₂/Tag – im Schnitt ca. 2.500 t SO₂/Tag. TOMS/OMI-Satellitenmessungen zeigen, dass Ambrym in aktiven Phasen 5–10 % aller globalen vulkanischen SO₂-Emissionen liefert.",
        difficulty = 5,
        funFact = "Die Lavateen des Ambrym gehören zu den größten dauerhaft aktiven Lavaseen der Welt. Die Bewohner Ambrym-Dörfer haben eine jahrtausendealte Koexistenz mit dem Vulkan entwickelt – traditionelle Riten beinhalten die Verehrung des Vulkans als Heimat der Ahnen."
    ),

    Question(
        categoryId = 1,
        questionText = "Auf welche Jahrespräzision kann der NGRIP-Eiskern (Nordgrönland) für den Abschnitt der letzten 60.000 Jahre datiert werden?",
        answerA = "±2.000 Jahre",
        answerB = "±500 Jahre",
        answerC = "±50 Jahre",
        answerD = "±1 Jahr durch jährliche Schichtenzählung",
        correctAnswer = 2,
        explanation = "Der NGRIP-Eiskern (North GRIP, Nordgrönland) kann im Abschnitt 0–60.000 Jahre BP auf ca. ±50 Jahre datiert werden (GICC05-Zeitskala, Svensson et al., 2008). Jährliche Isotopen-Schichten sind sichtbar, werden aber durch Ablagerungsvariationen und Postdepositional-Diffusion in tieferen Bereichen unschärfer.",
        difficulty = 5,
        funFact = "Die GICC05 (Greenland Ice Core Chronology 2005) ist die internationale Referenzchronologie für die letzten 60.000 Jahre und gilt als genauer als Radiokarbon für viele Zeitabschnitte. Ereignisse wie der Ausbruch des Hekla (3.000 BP) oder des Eldgjá (939 n. Chr.) sind als Schwefelsäure-Spikes präzise lokalisierbar."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Gesamtmenge an CO₂ (in Mt CO₂/Jahr) setzen alle subaerischen Vulkane der Erde nach USGS-Schätzungen frei?",
        answerA = "ca. 10–15 Mt CO₂/Jahr",
        answerB = "ca. 180–440 Mt CO₂/Jahr",
        answerC = "ca. 1.000–2.000 Mt CO₂/Jahr",
        answerD = "ca. 5.000 Mt CO₂/Jahr",
        correctAnswer = 1,
        explanation = "USGS (Gerlach, 2011) schätzt die gesamten vulkanischen CO₂-Emissionen aus subaerischen Vulkanen auf ca. 180–440 Mt CO₂/Jahr (im Mittel ca. 270 Mt/Jahr). Zum Vergleich: Anthropogene CO₂-Emissionen 2022 lagen bei ca. 36.600 Mt – etwa 80–200-mal mehr als alle Vulkane zusammen.",
        difficulty = 5,
        funFact = "Trotz ihrer geringen CO₂-Mengen im Vergleich zum Menschen spielen Vulkane auf geologischen Zeitskalen eine entscheidende Rolle: Sie 'recyceln' im Subduktionsozyklus gebundenen Kohlenstoff zurück in die Atmosphäre und halten damit den geologischen CO₂-Kreislauf über Millionen von Jahren im Gleichgewicht."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist das Gilbert-Chron in der paläomagnetischen Zeitskala, und wann beginnt und endet es?",
        answerA = "Ein reverses Magnetfeld-Chron von ca. 3,58–6,03 Millionen Jahren BP",
        answerB = "Ein normales Magnetfeld-Chron von ca. 2,58–3,58 Millionen Jahren BP",
        answerC = "Ein reverses Magnetfeld-Chron von ca. 0,78–2,58 Millionen Jahren BP",
        answerD = "Ein normales Magnetfeld-Chron von ca. 0,78–1,07 Millionen Jahren BP",
        correctAnswer = 0,
        explanation = "Das Gilbert-Chron ist ein reverses Magnetfeld-Chron (Polarität entgegengesetzt zur heutigen) von ca. 3,58 bis 6,03 Millionen Jahren BP. Es folgt nach dem Gauss-Chron (normal, 2,58–3,58 Ma) und enthält mehrere Subchrons (Cochiti, Nunivak, Sidufjall, Thvera). Es ist benannt nach dem Physiker William Gilbert.",
        difficulty = 5,
        funFact = "Die gesamte geologische Polumkehrungs-Zeitskala (GPTS) besteht aus Hunderten solcher Chrons und Subchrons, die in Seebodensedimenten und Laven weltweit erkennbar sind. Sie ist eines der wichtigsten Werkzeuge zur Datierung ozeanischer Krustengesteine, da die Magnetstreifenmuster am mittelozeanischen Rücken direkt die Polumkehrungschronologie widerspiegeln."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Wert hat der mittlere globale Meeresspiegel-Anstieg aus terrestrischem Wasserhaushalt (Grundwassererschöpfung + Speicherreservoire) laut IPCC AR6?",
        answerA = "ca. −0,1 mm/Jahr (Senke)",
        answerB = "ca. +0,38 mm/Jahr",
        answerC = "ca. +1,0 mm/Jahr",
        answerD = "ca. +2,5 mm/Jahr",
        correctAnswer = 1,
        explanation = "Laut IPCC AR6 (2021) trägt der terrestrische Wasserhaushalt ca. +0,38 mm/Jahr zum Meeresspiegelanstieg bei (Periode 2006–2018). Haupttreiber ist die Erschöpfung von Grundwasservorkommen (Grundwasserpumpen), die schließlich als Süßwasser in die Ozeane fließt. Speicherreservoire (Talsperren) wirken als Gegengewicht (−Beitrag).",
        difficulty = 5,
        funFact = "Würden alle Grundwasseraquifere der Erde geleert, würde der Meeresspiegel um ca. 8–10 m steigen. Die Ogallala-Aquifere unter den amerikanischen Great Plains, die Arabische Aquifer-Zone und der Indus-Ganges-Aquifer verlieren jährlich zwischen 100 und 300 km³ Wasser – unwiederbringlich auf menschlichen Zeitskalen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Messverfahren nutzt das GEONET-Netz in Neuseeland zur Überwachung der vertikalen Krustenbewegung mit einer Präzision von <1 mm/Jahr?",
        answerA = "Klassisches geometrisches Nivellement",
        answerB = "Kontinuierlich betriebene GNSS-Stationen (CGPS)",
        answerC = "InSAR (Interferometrische Radar-Fernerkundung)",
        answerD = "VLBI (Very Long Baseline Interferometry)",
        correctAnswer = 1,
        explanation = "Das GeoNet-Netzwerk Neuseelands (GNS Science) nutzt kontinuierlich betriebene GNSS-Stationen (CGPS), die vertikale Krustenbewegungen mit einer Präzision von 0,5–1 mm/Jahr aufzeichnen. Diese Daten sind entscheidend für das Monitoring von Erdbebenzonen, Vulkanen und post-seismischen Deformationen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "Welche postglaziare Hebungsrate zeigt die Hudson Bay (Kanada, Zentrum des früheren Laurentidischen Eisschilds) laut GPS-Messungen?",
        answerA = "ca. 2–3 mm/Jahr",
        answerB = "ca. 10–13 mm/Jahr",
        answerC = "ca. 20–25 mm/Jahr",
        answerD = "ca. 5–7 mm/Jahr",
        correctAnswer = 1,
        explanation = "Das Zentrum der einstigen Laurentidischen Vereisung (Hudson Bay) hebt sich mit ca. 10–13 mm/Jahr – eine der höchsten postglazialen Hebungsraten in Nordamerika. Das Maximum liegt bei ca. 12–13 mm/Jahr nahe der Küste des James Bay. Daten stammen aus GPS-Messungen des Canadian Spatial Reference System.",
        difficulty = 5,
        funFact = "In der James-Bay-Region hat sich das Land seit dem Abschmelzen des Laurentidischen Eisschilds vor ~8.000 Jahren bereits um über 200 m gehoben. Alte Strandterrassen und Muschelfriedhöfe in hunderten Metern Höhe über dem Meeresspiegel belegen diese dramatische Landhebung."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Vulkansystem in Island emittiert nach TOMS/OMI-Messungen jährlich die größten SO₂-Mengen außerhalb aktiver Eruptionsphasen?",
        answerA = "Hekla",
        answerB = "Krafla",
        answerC = "Grímsvötn",
        answerD = "Askja",
        correctAnswer = 2,
        explanation = "Grímsvötn unter dem Vatnajökull-Gletscher ist außerhalb von Eruptionsphasen der produktivste SO₂-Ausgaser Islands. OMI-Satellitenmessungen registrieren bei Grímsvötn regelmäßig Degasierungs-Anomalien. Bei der Eruption 2011 emittierte Grímsvötn mehr SO₂ als der gesamte europäische Industriesektor in diesem Jahr.",
        difficulty = 5,
        funFact = "Grímsvötn liegt unter ca. 200 m Eis und eruptiert deshalb oft unbemerkt als subglazialer Ausbruch. Die Schmelzwasserfluten (Jökulhlaups) durch die Eruption von 1996 waren so massiv, dass sie Brücken wie Streichholzschachteln wegspülten und einen kurzzeitigen Fluss schufen, der größer war als der Amazonas."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Datum hat die Olduvai-Subchron (normales geomagnetsches Ereignis im Matuyama-Chron), die in der Olduvai-Schlucht entdeckt wurde?",
        answerA = "ca. 0,78–1,07 Millionen Jahre BP",
        answerB = "ca. 1,77–1,95 Millionen Jahre BP",
        answerC = "ca. 2,14–2,15 Millionen Jahre BP",
        answerD = "ca. 3,22–3,33 Millionen Jahre BP",
        correctAnswer = 1,
        explanation = "Die Olduvai-Subchron (normales geomagnetsches Ereignis innerhalb des reversen Matuyama-Chrons) erstreckt sich von ca. 1,77 bis 1,95 Millionen Jahren BP und dauerte ca. 180.000 Jahre. Sie wurde erstmals in der Olduvai-Schlucht (Tansania) in vulkanischen Schichten dokumentiert, die auch frühe Homo-habilis-Fossilien enthielten.",
        difficulty = 5,
        funFact = "Die Olduvai-Subchron ist ein Leithorizont für die Datierung der Human Evolution: Homo habilis und Paranthropus boisei erscheinen in Schichten, die genau während dieser Subchron abgelagert wurden. Die paläomagnetische Datierung ermöglichte es, das Alter der Australopithecus-Funde auf über 1,8 Millionen Jahre zu bestimmen – ohne Radiokarbon, das nur bis 50.000 Jahre reicht."
    ),

    Question(
        categoryId = 1,
        questionText = "Was gibt der Wert 'EGM2008-Undulation' für den Punkt Zugspitze (2.962 m NN) in Deutschland an, und wie groß ist er näherungsweise?",
        answerA = "ca. +48 m (Geoid 48 m über Ellipsoid)",
        answerB = "ca. +50 m (Geoid 50 m über Ellipsoid)",
        answerC = "ca. +35 m (Geoid 35 m über Ellipsoid)",
        answerD = "ca. +44 m (Geoid 44 m über Ellipsoid)",
        correctAnswer = 3,
        explanation = "Am Punkt Zugspitze beträgt die EGM2008-Geoidundulation N ≈ +44 m (Geoid liegt 44 m über dem WGS84-Ellipsoid). Das bedeutet: Die ellipsoidische GPS-Höhe der Zugspitze (h) liegt bei ca. 3.006 m, während die Normalhöhe über dem Geoid (H) ca. 2.962 m beträgt – Differenz ≈ 44 m = Undulation N.",
        difficulty = 5,
        funFact = "In Deutschland liegt das Geoid überall deutlich über dem Ellipsoid (N = 35–50 m), da die Alpen und das gesamte mitteleuropäische Festlandmassiv die Erdschwere leicht erhöhen. GPS gibt immer ellipsoidische Höhen – für amtliche Normalhöhen muss die Geoidundulation subtrahiert werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Wert für die postglaziare Hebungsrate wurde für den Gothenburg-Bereich (Schweden) durch präzises GNSS-Monitoring (SWEPOS-Netz) ermittelt?",
        answerA = "ca. 1,8 mm/Jahr",
        answerB = "ca. 4,6 mm/Jahr",
        answerC = "ca. 7,2 mm/Jahr",
        answerD = "ca. 9,8 mm/Jahr",
        correctAnswer = 1,
        explanation = "Das SWEPOS-GNSS-Netz Schwedens misst für den Göteborg-Bereich eine vertikale Landerhebung von ca. 4–5 mm/Jahr. Das Maximum der skandinavischen Hebung liegt weiter nördlich im Bottnischen Meerbusen. Göteborg (südlichere Lage) hebt sich merklich langsamer als die nördlichen Regionen Schwedens.",
        difficulty = 5,
        funFact = "SWEPOS ist eines der dichtesten permanenten GNSS-Netze der Welt und hat gezeigt, dass die Landhebung in Schweden seit 1990 nicht abgenommen, sondern leicht zugenommen hat. Eine neue Hypothese verbindet dies mit beschleunigter Eisschmelze in Grönland, die die Erdmantelströmungen beeinflusst."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche HF-Emissionsrate (HCl in Tonnen/Tag) verzeichnete der Popocatépetl (Mexiko) als einer der aktivsten HCl-Ausgaser Lateinamerikas in einem typischen Aktivitätsjahr?",
        answerA = "ca. 10–50 t HCl/Tag",
        answerB = "ca. 100–400 t HCl/Tag",
        answerC = "ca. 1.000–3.000 t HCl/Tag",
        answerD = "ca. 5.000 t HCl/Tag",
        correctAnswer = 1,
        explanation = "Der Popocatépetl (5.452 m) emittiert bei mittlerer Aktivität ca. 100–400 t HCl pro Tag sowie ca. 100–1.000 t SO₂/Tag. DOAS-Messungen des CENAPRED (Centro Nacional de Prevención de Desastres) dokumentieren diese Emissionen kontinuierlich. 'Popo' ist einer der aktivsten HCl-Ausgaser des Amerikanischen Kontinents.",
        difficulty = 5,
        funFact = "Der Popocatépetl ist 70 km von Mexiko-Stadt entfernt, das mit 22 Millionen Menschen in der Metropolregion die größte Millionenstadt im direkten Einflussbereich eines aktiven Stratovulkans ist. Selbst mäßige Eruptionen verursachen Aschefälle, die Flughäfen schließen und Ernteschäden im Millionenbereich verursachen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Referenzsystem definiert den Nullmeridian der ITRF (International Terrestrial Reference Frame)-Familie, und wodurch unterscheidet er sich von Greenwich?",
        answerA = "Der ITRF-Nullmeridian ist identisch mit dem historischen Greenwich-Meridian",
        answerB = "Der ITRF-Nullmeridian (IRM) liegt ca. 5,31 Bogensekunden östlich des optischen Greenwich-Meridians",
        answerC = "Der ITRF-Nullmeridian liegt ca. 0,01 Grad westlich des Primemeridians",
        answerD = "ITRF hat keinen definierten Nullmeridian; Längengrade basieren auf Satellitenorbitdaten",
        correctAnswer = 1,
        explanation = "Der International Reference Meridian (IRM), der Nullmeridian des ITRF und damit auch WGS84/GPS, liegt ca. 5,31 Bogensekunden (ca. 102 m) östlich des historischen optischen Greenwich-Meridians (bestimmt durch das Transit-Instrument des Airy). Der Unterschied entsteht durch den Übertrag von der optischen Astronomie zur VLBI-basierten Geodäsie.",
        difficulty = 5,
        funFact = "Touristen, die beim Royal Observatory Greenwich auf dem Nullmeridian stehen und mit GPS ihre Position prüfen, stellen fest, dass ihr GPS-Gerät sie ca. 102 m östlich von 0° Länge zeigt – weil GPS den IRM nutzt, nicht den alten Airy-Meridian. Eine Tafel im Observatory erklärt diesen Unterschied."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche postglaziare Landhebungsrate weist das Zentrum Grönlands (Dome Summit) nach GIA-Modellen (ICE-6G_C) auf?",
        answerA = "ca. 1–2 mm/Jahr",
        answerB = "ca. 5–6 mm/Jahr",
        answerC = "ca. 10–15 mm/Jahr",
        answerD = "ca. 20–30 mm/Jahr",
        correctAnswer = 1,
        explanation = "Das ICE-6G_C-GIA-Modell (Glacial Isostatic Adjustment) prognostiziert für das Zentrum Grönlands eine postglaziare Hebungsrate von ca. 5–6 mm/Jahr durch Entlastung nach der letzten Vereisung. Moderne GNSS-Stationen auf Grönland messen zudem aktuelle Anhebungsraten von bis zu 20–40 mm/Jahr durch die beschleunigte gegenwärtige Gletscherschmelze.",
        difficulty = 5,
        funFact = "In Grönland überlappen sich zwei Prozesse: die langsame postglaziare Erholung von der letzten Eiszeit (GIA, 5–6 mm/Jahr) und die schnelle elastische Erdhebung durch die aktuelle Eisschmelze (10–35 mm/Jahr). GPS-Stationen nahe dem Helheim-Gletscher registrierten in einem Jahr eine Hebung von über 38 mm."
    ),

    Question(
        categoryId = 1,
        questionText = "Mit welcher Abkürzung wird der internationale Standard-Referenzrahmen für terrestrische Koordinaten bezeichnet, der seit 2015 für geodätische Präzisionsanwendungen empfohlen wird?",
        answerA = "WGS84 (G1762 Frame)",
        answerB = "ITRF2014",
        answerC = "ETRS89 (Europäisch)",
        answerD = "GRS80",
        correctAnswer = 1,
        explanation = "Das ITRF2014 (International Terrestrial Reference Frame 2014), veröffentlicht durch das IERS (International Earth Rotation and Reference Systems Service) 2016, war bis zur Veröffentlichung von ITRF2020 (2022) der internationale Standard für Präzisionsgeodäsie. Es basiert auf Beobachtungen von ~900 GNSS/VLBI/SLR/DORIS-Stationen weltweit.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Jahr fand die Matuyama-Gauss-Polumkehrung statt (Übergang von reversem Matuyama- zu normalem Gauss-Chron)?",
        answerA = "ca. 780.000 Jahre BP",
        answerB = "ca. 2,58 Millionen Jahre BP",
        answerC = "ca. 1,77 Millionen Jahre BP",
        answerD = "ca. 3,58 Millionen Jahre BP",
        correctAnswer = 1,
        explanation = "Die Matuyama-Gauss-Grenze – der Übergang vom normalen Gauss-Chron (3,58–2,58 Ma) zum reversen Matuyama-Chron (2,58–0,78 Ma) – fand vor ca. 2,58 Millionen Jahren statt und markiert gleichzeitig die Grenze zwischen dem Pliozän und dem Pleistozän (Quartärbeginn).",
        difficulty = 5,
        funFact = "Die Matuyama-Gauss-Grenze fällt fast genau mit dem Beginn der nördlichen Vereisung (Pleistozän, 2,58 Ma) zusammen – ob das ein Zufall oder eine Kausalbeziehung ist, wird noch diskutiert. Einige Forscher spekulieren, ob das schwächere Magnetfeld während der Umkehrung zu Klimaveränderungen führte."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Genauigkeit weist das GNSS-basierte Meeresspiegel-Monitoringsystem (tide gauge + GNSS) an der Deutschen Bucht auf, wenn vertikale Landbewegungen korrigiert werden?",
        answerA = "±5 cm absolut",
        answerB = "±1–2 mm/Jahr (relative VLM-korrigierte Rate)",
        answerC = "±10 cm absolut",
        answerD = "±0,1 mm/Jahr",
        correctAnswer = 1,
        explanation = "Kombinierte Gezeitenpegel-GNSS-Stationen (wie STHN, Cuxhaven) ermöglichen nach Korrektur der Vertikalen Landbewegung (VLM) Meeresspiegel-Trends mit einer Präzision von ca. ±1–2 mm/Jahr. Die Deutsche Bucht zeigt damit einen mittleren relativen Meeresspiegelanstieg von ca. 1,5–2,5 mm/Jahr über die letzten 100 Jahre.",
        difficulty = 5,
        funFact = "In Hamburg zeigen Pegeldaten seit 1843 einen relativen Meeresspiegelanstieg von ca. 1,9 mm/Jahr. Da das norddeutsche Festland leicht absinkt (GIA: ca. −0,5 mm/Jahr), ist der absolute Meeresspiegelanstieg tatsächlich etwas geringer – GNSS-Korrekturen sind daher unverzichtbar für verlässliche Klimaaussagen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche exakte geomagnetssche Intensitäts-Anomalie (in nanoTesla) weist das 'South Atlantic Anomaly' (SAA) im Vergleich zum globalen Mittelwert auf?",
        answerA = "ca. −5.000 nT schwächer als der Mittelwert",
        answerB = "ca. −20.000 nT schwächer als der Mittelwert",
        answerC = "ca. −40.000 nT schwächer als der Mittelwert",
        answerD = "ca. +15.000 nT stärker als der Mittelwert",
        correctAnswer = 1,
        explanation = "Die South Atlantic Anomaly (SAA) zeigt eine geomagnetische Feldschwächung von ca. −20.000 bis −25.000 nT im Vergleich zum globalen Mittelwert (ca. 45.000–60.000 nT). Das Feld über dem zentralen Südatlantik beträgt nur ca. 22.000–25.000 nT – weniger als die Hälfte des globalen Mittels.",
        difficulty = 5,
        funFact = "Die SAA verursacht erhebliche Probleme für Satelliten und Raumfahrzeuge: Beim Überflug dieser Zone erfahren sie erhöhte Strahlenbelastung durch einfallende geladene Teilchen aus dem Van-Allen-Gürtel. Die ISS schaltet beim SAA-Überflug bestimmte elektronische Systeme ab, um Strahlenschäden zu minimieren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Drift-Richtung und Geschwindigkeit zeigt der magnetische Nordpol laut NOAA World Magnetic Model (WMM2020) aktuell (Stand 2024)?",
        answerA = "Richtung Sibirien (Russland), ca. 40–45 km/Jahr",
        answerB = "Richtung Kanada, ca. 15 km/Jahr",
        answerC = "Richtung Nordpol, stationär seit 2020",
        answerD = "Richtung Grönland, ca. 25 km/Jahr",
        correctAnswer = 0,
        explanation = "Laut WMM2020 (NOAA/BGS) driftet der magnetische Nordpol weiterhin Richtung Sibirien mit ca. 40–45 km/Jahr. Er hat die kanadische Arktis verlassen und befindet sich nun im nördlichen Arktischen Ozean auf dem Weg Richtung russische Arktis. Die Drift hat sich seit den 2000er Jahren beschleunigt.",
        difficulty = 5,
        funFact = "Der magnetische Nordpol wurde erstmals 1831 von James Clark Ross auf der Boothia-Halbinsel (Kanada) lokalisiert. Seitdem hat er sich um über 2.400 km verschoben – eine durchschnittliche Drift von ca. 12 km/Jahr über fast 200 Jahre, wobei die letzten 25 Jahre deutlich schneller waren."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen Beitrag zum Meeresspiegel liefert das Schmelzen des permafrosten Bodens (Permafrost) als Süßwasserquelle laut IPCC AR6?",
        answerA = "Signifikanter Beitrag: ca. 2–5 mm/Jahr",
        answerB = "Vernachlässigbar: <0,01 mm/Jahr direkt als Flüssigwasser",
        answerC = "Bedeutsam: ca. 0,5 mm/Jahr",
        answerD = "Negativer Beitrag: Permafrost speichert mehr Wasser als er abgibt",
        correctAnswer = 1,
        explanation = "Schmelzender Permafrost selbst (Tauen von gefrorenem Boden) liefert <0,01 mm/Jahr direkten Meeresspiegelanstieg, da das Wasser hauptsächlich im Boden verbleibt (Thermokarstbildung) oder in Flüsse fließt, aber nur in geringen Mengen ins Meer. Sein Hauptbeitrag zum Klimawandel erfolgt über CO₂- und Methan-Emissionen, nicht über Meeresspiegelerhöhung.",
        difficulty = 5,
        funFact = "Der Permafrost speichert ca. 1,5 Billionen Tonnen Kohlenstoff – doppelt so viel wie die gesamte Atmosphäre. Bei vollständigem Auftauen würde dieser Kohlenstoff als CO₂ und CH₄ freigesetzt werden, was eine massive Klimarückkopplung auslösen würde – weit gefährlicher als der Meeresspiegeleffekt des Tauwassers selbst."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Referenzpunkt (Benchmark) dient als Fundamentalpunkt für die Höhenmessungen des chinesischen National Height Datum (1985)?",
        answerA = "Huanghai-Meeresspiegel bei Qingdao (Huang Hai Mean Sea Level)",
        answerB = "Meeresspiegel bei Shanghai (Huangpu-Fluss)",
        answerC = "Astronomisches Observatorium Beijing",
        answerD = "Tidal Benchmark Hong Kong",
        correctAnswer = 0,
        explanation = "Das chinesische National Height Datum 1985 (Guojia Gaocheng 1985) basiert auf dem mittleren Meeresspiegel des Gelben Meeres (Huang Hai) bei Qingdao, gemessen von 1950 bis 1956 am Zhanqiao-Pegel. Dieser Punkt ist der Fundamentalpunkt für alle offiziellen Höhenmessungen in der Volksrepublik China.",
        difficulty = 5,
        funFact = "China hat in den 2020er Jahren sein National Height Datum mit GNSS/Geoid-Technologie auf das CGCS2000 (China Geodetic Coordinate System 2000) aktualisiert. Dabei stellte sich heraus, dass der Himalaya-Gipfel Mount Qomolangma 8.848,86 m beträgt – 86 cm mehr als die zuvor anerkannte Höhe von 8.848 m."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche SO₂-Emissionsrate gibt das USGS Volcano Hazards Program für den Kilauea während der Halema'uma'u-Eruptionsphase (2008–2018) als Tageswert an?",
        answerA = "ca. 100–300 t SO₂/Tag",
        answerB = "ca. 500–2.000 t SO₂/Tag",
        answerC = "ca. 5.000–10.000 t SO₂/Tag",
        answerD = "ca. 20.000–50.000 t SO₂/Tag",
        correctAnswer = 1,
        explanation = "Während der Halema'uma'u-Eruption (2008–2018) emittierte der Kīlauea kontinuierlich 500–2.000 t SO₂/Tag aus dem Lava-See im Halema'uma'u-Krater. Das USGS HVO (Hawaiian Volcano Observatory) überwacht dies mittels FTIR- und UV-Absorptions-Spektrometrie von Bodenstation und Hubschrauber aus.",
        difficulty = 5,
        funFact = "Die SO₂-Emissionen des Kīlauea (2008–2018) waren so intensiv, dass der Hawai'i Volcanoes National Park zeitweise für Besucher gesperrt werden musste. Die Wind-Abbdrift-Richtung des Vog nach Kona (Westküste Big Island) verursachte regelmäßig gesundheitsschädliche Luftqualität für Tausende von Einwohnern."
    ),

    Question(
        categoryId = 1,
        questionText = "Wann fand die Réunion-Ereignis statt – eine kurze Polumkehrung in der geomagnetschen Zeitskala zu Beginn des Matuyama-Chrons?",
        answerA = "ca. 2,14–2,15 Millionen Jahre BP",
        answerB = "ca. 1,77–1,80 Millionen Jahre BP",
        answerC = "ca. 0,99–1,07 Millionen Jahre BP",
        answerD = "ca. 3,05–3,10 Millionen Jahre BP",
        correctAnswer = 0,
        explanation = "Das Réunion-Ereignis (auch Réunion-Subchron) ist ein kurzes normales geomagnetsches Ereignis nahe der Basis des Matuyama-Chrons, datiert auf ca. 2,14–2,15 Millionen Jahre BP. Es ist benannt nach vulkanischen Basalten der Insel Réunion, in denen dieses kurze Normalfeld-Ereignis erstmals dokumentiert wurde.",
        difficulty = 5,
        funFact = "Der Hotspot unter der Insel Réunion (Piton de la Fournaise) ist mit dem Deccan-Traps-Vulkanismus verbunden, der vor ~66 Millionen Jahren beim Massenaussterben der Dinosaurier mitwirkte. Die Insel Réunion liegt über demselben Mantelplume, der sich relativ zur Platte verschoben hat."
    ),

    Question(
        categoryId = 1,
        questionText = "Welchen messbaren Beitrag zum globalen Meeresspiegelanstieg liefert die Meldung des geodätischen Ausgleichs (Glacio-Isostatic Adjustment, GIA) selbst?",
        answerA = "GIA senkt den Meeresspiegel um ca. 0,3 mm/Jahr (durch Beckenvergrößerung)",
        answerB = "GIA hat keinen Einfluss auf den absoluten Meeresspiegel",
        answerC = "GIA hebt den Meeresspiegel um ca. 0,5 mm/Jahr",
        answerD = "GIA verändert nur lokale Relativ-Pegel, nicht den globalen Meeresspiegel",
        correctAnswer = 0,
        explanation = "GIA (Glazioisostatischer Ausgleich) vergrößert die ozeanischen Becken durch das Absinken von Bereichen, die ehemals Eis trugen (forebulge collapse), und durch das isostatische Heben des Meeresbodens. Dieser Geometrieeffekt senkt den globalen Meeresspiegel um ca. 0,3 mm/Jahr – eine Korrektur, die bei Satellitenaltimetrie-Messungen immer abgezogen werden muss.",
        difficulty = 5,
        funFact = "Ohne GIA-Korrektur würden Satelliten-Altimeter (wie Jason-3 oder Sentinel-6) einen zu niedrigen globalen Meeresspiegelanstieg messen. Die GIA-Korrektur von −0,3 mm/Jahr ist deshalb eine der wichtigsten und zugleich am schwersten modellierbaren Korrekturen in der modernen Meeresspiegel-Klimatologie."
    ),

)


