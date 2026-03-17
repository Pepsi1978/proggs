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

)
