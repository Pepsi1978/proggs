package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsEasy(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Italien?",
        answerA = "Mailand",
        answerB = "Rom",
        answerC = "Neapel",
        answerD = "Florenz",
        correctAnswer = 1, // B
        explanation = "Rom ist die Hauptstadt Italiens und war einst das Zentrum des Römischen Reiches. Heute leben dort etwa 2,8 Millionen Menschen.",
        difficulty = 1,
        funFact = "Rom wird auch \"die ewige Stadt\" genannt, weil es seit über 2.700 Jahren kontinuierlich bewohnt ist."
    ),

    // Question 2
    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent ist der kleinste der Erde?",
        answerA = "Europa",
        answerB = "Antarktika",
        answerC = "Australien",
        answerD = "Südamerika",
        correctAnswer = 2, // C
        explanation = "Australien ist der kleinste Kontinent der Erde mit einer Fläche von rund 7,7 Millionen km². Es ist gleichzeitig der einzige Kontinent, der von einem einzigen Land besetzt ist.",
        difficulty = 1,
        funFact = null
    ),

    // Question 3
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Brasilien?",
        answerA = "São Paulo",
        answerB = "Rio de Janeiro",
        answerC = "Salvador",
        answerD = "Brasília",
        correctAnswer = 3, // D
        explanation = "Brasília ist die Hauptstadt Brasiliens. Die Stadt wurde eigens als Hauptstadt geplant und 1960 eingeweiht, um den Regierungssitz ins Landesinnere zu verlegen.",
        difficulty = 1,
        funFact = "Brasília wurde in nur vier Jahren gebaut und gilt als Meisterwerk des modernen Städtebaus – der gesamte Stadtplan hat die Form eines Flugzeugs."
    ),

    // Question 4
    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt östlich von Saudi-Arabien?",
        answerA = "Rotes Meer",
        answerB = "Mittelmeer",
        answerC = "Arabisches Meer",
        answerD = "Persischer Golf",
        correctAnswer = 3, // D
        explanation = "Der Persische Golf liegt östlich von Saudi-Arabien und trennt die Arabische Halbinsel vom Iran. Er ist eine der wichtigsten Erdöl-Exportregionen der Welt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 5
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Australien?",
        answerA = "Sydney",
        answerB = "Melbourne",
        answerC = "Canberra",
        answerD = "Brisbane",
        correctAnswer = 2, // C
        explanation = "Canberra ist die Hauptstadt Australiens, obwohl viele Sydney oder Melbourne vermuten. Die Stadt wurde 1913 eigens als Kompromiss zwischen den beiden Rivalen gegründet.",
        difficulty = 1,
        funFact = "Der Name \"Canberra\" stammt aus der Sprache der Ngunnawal und bedeutet wahrscheinlich \"Treffpunkt\" oder \"Frau zwischen zwei Brüsten\"."
    ),

    // Question 6
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Brasilien?",
        answerA = "Nordamerika",
        answerB = "Afrika",
        answerC = "Australien",
        answerD = "Südamerika",
        correctAnswer = 3, // D
        explanation = "Brasilien liegt in Südamerika und ist mit einer Fläche von über 8,5 Millionen km² das fünftgrößte Land der Erde.",
        difficulty = 1,
        funFact = null
    ),

    // Question 7
    Question(
        categoryId = 1,
        questionText = "Welches ist der höchste Berg der Erde?",
        answerA = "K2",
        answerB = "Kangchendzönga",
        answerC = "Mount Everest",
        answerD = "Lhotse",
        correctAnswer = 2, // C
        explanation = "Der Mount Everest im Himalaya ist mit 8.849 m der höchste Berg der Erde. Er liegt an der Grenze zwischen Nepal und Tibet (China).",
        difficulty = 1,
        funFact = "Der Mount Everest heißt auf Nepalesisch \"Sagarmatha\" und auf Tibetisch \"Chomolungma\", was \"Göttin der Erde\" bedeutet."
    ),

    // Question 8
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kanada?",
        answerA = "Toronto",
        answerB = "Vancouver",
        answerC = "Montréal",
        answerD = "Ottawa",
        correctAnswer = 3, // D
        explanation = "Ottawa ist die Hauptstadt Kanadas. Viele denken, Toronto sei die Hauptstadt, weil es die größte Stadt des Landes ist – aber Ottawa wurde 1857 zur Hauptstadt ernannt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 9
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich der Eiffelturm?",
        answerA = "Belgien",
        answerB = "Schweiz",
        answerC = "Frankreich",
        answerD = "Luxemburg",
        correctAnswer = 2, // C
        explanation = "Der Eiffelturm steht in Paris, der Hauptstadt Frankreichs. Er wurde 1889 zur Weltausstellung gebaut und ist das bekannteste Wahrzeichen Frankreichs.",
        difficulty = 1,
        funFact = "Der Eiffelturm ist im Sommer etwa 15 cm höher als im Winter, weil das Metall sich bei Wärme ausdehnt."
    ),

    // Question 10
    Question(
        categoryId = 1,
        questionText = "Welcher Fluss fließt durch London?",
        answerA = "Themse",
        answerB = "Severn",
        answerC = "Mersey",
        answerD = "Avon",
        correctAnswer = 0, // A
        explanation = "Die Themse (englisch: Thames) fließt durch London und ist der längste Fluss vollständig in England mit einer Länge von rund 346 km.",
        difficulty = 1,
        funFact = "Die Themse war früher so stark verschmutzt, dass sie 1858 den berühmten \"Great Stink\" verursachte – der Geruch zwang das Parlament, die Sitzung zu unterbrechen."
    ),

    // Question 11
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Spanien?",
        answerA = "Barcelona",
        answerB = "Sevilla",
        answerC = "Madrid",
        answerD = "Valencia",
        correctAnswer = 2, // C
        explanation = "Madrid ist die Hauptstadt und bevölkerungsreichste Stadt Spaniens mit über 3,3 Millionen Einwohnern. Sie liegt im geografischen Zentrum des Landes.",
        difficulty = 1,
        funFact = null
    ),

    // Question 12
    Question(
        categoryId = 1,
        questionText = "Welches Land ist für den Kanal von Suez bekannt?",
        answerA = "Marokko",
        answerB = "Ägypten",
        answerC = "Libyen",
        answerD = "Tunesien",
        correctAnswer = 1, // B
        explanation = "Der Suezkanal liegt in Ägypten und verbindet das Mittelmeer mit dem Roten Meer. Er wurde 1869 eröffnet und ist eine der wichtigsten Wasserstraßen der Welt.",
        difficulty = 1,
        funFact = "Durch den Suezkanal werden etwa 12–15 % des weltweiten Schiffsverkehrs abgewickelt."
    ),

    // Question 13
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Südafrika?",
        answerA = "Asien",
        answerB = "Ozeanien",
        answerC = "Südamerika",
        answerD = "Afrika",
        correctAnswer = 3, // D
        explanation = "Südafrika liegt im südlichsten Teil des afrikanischen Kontinents und grenzt an den Atlantischen und den Indischen Ozean.",
        difficulty = 1,
        funFact = null
    ),

    // Question 14
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von China?",
        answerA = "Shanghai",
        answerB = "Peking",
        answerC = "Chongqing",
        answerD = "Guangzhou",
        correctAnswer = 1, // B
        explanation = "Peking (chinesisch: 北京, Běijīng) ist die Hauptstadt der Volksrepublik China und mit über 21 Millionen Einwohnern eine der größten Städte der Welt.",
        difficulty = 1,
        funFact = "\"Peking\" bedeutet auf Chinesisch \"Nördliche Hauptstadt\" – im Gegensatz zu Nanjing, was \"Südliche Hauptstadt\" bedeutet."
    ),

    // Question 15
    Question(
        categoryId = 1,
        questionText = "Welcher Ozean liegt zwischen Europa und Amerika?",
        answerA = "Pazifischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Arktischer Ozean",
        answerD = "Atlantischer Ozean",
        correctAnswer = 3, // D
        explanation = "Der Atlantische Ozean liegt zwischen Europa und Afrika im Osten sowie Nord- und Südamerika im Westen. Er ist der zweitgrößte Ozean der Erde.",
        difficulty = 1,
        funFact = "Der Atlantik wird jedes Jahr etwa 2,5 cm breiter, weil sich die Erdplatten auseinanderbewegen."
    ),

    // Question 16
    Question(
        categoryId = 1,
        questionText = "In welchem Land steht die Freiheitsstatue?",
        answerA = "Kanada",
        answerB = "Vereinigtes Königreich",
        answerC = "USA",
        answerD = "Frankreich",
        correctAnswer = 2, // C
        explanation = "Die Freiheitsstatue steht auf Liberty Island im Hafen von New York City (USA). Sie wurde 1886 von Frankreich als Geschenk übergeben.",
        difficulty = 1,
        funFact = null
    ),

    // Question 17
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Indien?",
        answerA = "Mumbai",
        answerB = "Kalkutta",
        answerC = "New Delhi",
        answerD = "Chennai",
        correctAnswer = 2, // C
        explanation = "New Delhi ist die Hauptstadt Indiens und Teil der Metropolregion Delhi. Viele verwechseln sie mit Mumbai, der größten Stadt des Landes.",
        difficulty = 1,
        funFact = "New Delhi und Old Delhi zusammen bilden das Nationale Hauptstadtterritorium Delhi mit über 30 Millionen Einwohnern."
    ),

    // Question 18
    Question(
        categoryId = 1,
        questionText = "Welcher Fluss fließt durch Paris?",
        answerA = "Rhône",
        answerB = "Loire",
        answerC = "Seine",
        answerD = "Rhein",
        correctAnswer = 2, // C
        explanation = "Die Seine fließt durch Paris und mündet bei Le Havre in den Ärmelkanal. Der Eiffelturm steht direkt an ihrem Ufer.",
        difficulty = 1,
        funFact = null
    ),

    // Question 19
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Russland?",
        answerA = "Sankt Petersburg",
        answerB = "Nowosibirsk",
        answerC = "Jekaterinburg",
        answerD = "Moskau",
        correctAnswer = 3, // D
        explanation = "Moskau ist die Hauptstadt und größte Stadt Russlands mit über 12 Millionen Einwohnern. Sie liegt im westlichen Teil des riesigen Landes.",
        difficulty = 1,
        funFact = "Der Kreml in Moskau ist eine der bekanntesten Festungen der Welt und war früher der Wohnsitz der russischen Zaren."
    ),

    // Question 20
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt die Sahara?",
        answerA = "Asien",
        answerB = "Südamerika",
        answerC = "Afrika",
        answerD = "Australien",
        correctAnswer = 2, // C
        explanation = "Die Sahara ist die größte Hitzewüste der Welt und liegt im Norden Afrikas. Sie erstreckt sich über elf Länder und ist fast so groß wie die USA.",
        difficulty = 1,
        funFact = "Die Sahara war vor etwa 6.000 Jahren grüner und feuchter als heute – dort lebten Menschen und Tiere in großer Zahl."
    ),

    // Question 21
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Argentinien?",
        answerA = "Buenos Aires",
        answerB = "Santiago",
        answerC = "Lima",
        answerD = "Bogotá",
        correctAnswer = 0, // A
        explanation = "Buenos Aires ist die Hauptstadt und größte Stadt Argentiniens. Der Name bedeutet auf Spanisch \"gute Lüfte\" und wurde von spanischen Seefahrern vergeben.",
        difficulty = 1,
        funFact = "Buenos Aires hat die höchste Dichte an Psychotherapeuten pro Einwohner weltweit – die Einwohner nennen sich selbst \"porteños\" (Hafenbewohner)."
    ),

    // Question 22
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die zweitgrößte Fläche der Erde?",
        answerA = "USA",
        answerB = "Kanada",
        answerC = "China",
        answerD = "Brasilien",
        correctAnswer = 1, // B
        explanation = "Kanada ist mit rund 10 Millionen km² das zweitgrößte Land der Erde nach Russland. Es besteht zu einem großen Teil aus unberührter Wildnis.",
        difficulty = 1,
        funFact = null
    ),

    // Question 23
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Amazonas-Regenwald hauptsächlich?",
        answerA = "Kolumbien",
        answerB = "Venezuela",
        answerC = "Peru",
        answerD = "Brasilien",
        correctAnswer = 3, // D
        explanation = "Rund 60 % des Amazonas-Regenwaldes liegen in Brasilien. Er ist der größte tropische Regenwald der Erde und wird oft als \"Lunge der Erde\" bezeichnet.",
        difficulty = 1,
        funFact = "Der Amazonas-Regenwald produziert etwa 20 % des weltweiten Sauerstoffs."
    ),

    // Question 24
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Mexiko?",
        answerA = "Guadalajara",
        answerB = "Monterrey",
        answerC = "Mexico-Stadt",
        answerD = "Tijuana",
        correctAnswer = 2, // C
        explanation = "Mexico-Stadt (Ciudad de México) ist die Hauptstadt Mexikos und mit über 9 Millionen Einwohnern (21 Millionen in der Metropolregion) eine der größten Städte der Welt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 25
    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent liegt am Südpol?",
        answerA = "Australien",
        answerB = "Südamerika",
        answerC = "Antarktika",
        answerD = "Afrika",
        correctAnswer = 2, // C
        explanation = "Die Antarktis liegt am Südpol und ist der kälteste, trockenste und windigste Kontinent. Permanente Einwohner gibt es dort nicht – nur Forscher.",
        difficulty = 1,
        funFact = "In der Antarktis liegt etwa 70 % des weltweiten Süßwassers als Eis – würde es schmelzen, stiege der Meeresspiegel um rund 60 Meter."
    ),

    // Question 26
    Question(
        categoryId = 1,
        questionText = "Welches Wahrzeichen steht in Agra, Indien?",
        answerA = "Rotes Fort",
        answerB = "Taj Mahal",
        answerC = "Qutb Minar",
        answerD = "Lotus-Tempel",
        correctAnswer = 1, // B
        explanation = "Das Taj Mahal steht in Agra, Indien. Mogulkaiser Shah Jahan ließ es im 17. Jahrhundert als Grabmal für seine verstorbene Frau Mumtaz Mahal errichten.",
        difficulty = 1,
        funFact = "Das Taj Mahal ändert seine Farbe je nach Tageszeit – bei Sonnenaufgang erscheint es rosa, tagsüber weiß und bei Mondlicht golden."
    ),

    // Question 27
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Ägypten?",
        answerA = "Alexandria",
        answerB = "Luxor",
        answerC = "Kairo",
        answerD = "Assuan",
        correctAnswer = 2, // C
        explanation = "Kairo ist die Hauptstadt Ägyptens und mit über 20 Millionen Einwohnern in der Metropolregion die größte Stadt Afrikas.",
        difficulty = 1,
        funFact = null
    ),

    // Question 28
    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Afrikas?",
        answerA = "Äthiopien",
        answerB = "Demokratische Republik Kongo",
        answerC = "Ägypten",
        answerD = "Nigeria",
        correctAnswer = 3, // D
        explanation = "Nigeria ist mit über 220 Millionen Einwohnern das bevölkerungsreichste Land Afrikas. Es liegt in Westafrika und ist auch wirtschaftlich die größte Volkswirtschaft des Kontinents.",
        difficulty = 1,
        funFact = "Nigeria hat über 500 verschiedene ethnische Gruppen und mehr als 500 Sprachen."
    ),

    // Question 29
    Question(
        categoryId = 1,
        questionText = "Durch welches Land fließt der Rhein?",
        answerA = "Nur durch Deutschland",
        answerB = "Durch Frankreich, Deutschland und Niederlande",
        answerC = "Durch Deutschland und Österreich",
        answerD = "Durch die Schweiz und Frankreich",
        correctAnswer = 1, // B
        explanation = "Der Rhein entspringt in der Schweiz und fließt durch Österreich, Liechtenstein, Frankreich, Deutschland und die Niederlande, bevor er in die Nordsee mündet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 30
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Saudi-Arabien?",
        answerA = "Mekka",
        answerB = "Medina",
        answerC = "Riad",
        answerD = "Dschidda",
        correctAnswer = 2, // C
        explanation = "Riad ist die Hauptstadt Saudi-Arabiens und liegt im Zentrum der Arabischen Halbinsel. Die Stadt hat sich in wenigen Jahrzehnten zu einer modernen Metropole entwickelt.",
        difficulty = 1,
        funFact = "In Saudi-Arabien gibt es weder eine Einkommensteuer noch eine Mehrwertsteuer auf die meisten Güter, da Öleinnahmen den Staatshaushalt finanzieren."
    ),

    // Question 31
    Question(
        categoryId = 1,
        questionText = "Welches Gebirge trennt Europa von Asien?",
        answerA = "Kaukasus",
        answerB = "Ural",
        answerC = "Karpaten",
        answerD = "Alpen",
        correctAnswer = 1, // B
        explanation = "Der Ural in Russland gilt traditionell als geografische Grenze zwischen Europa und Asien. Er erstreckt sich von der Arktis bis zu den kasachischen Steppen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 32
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich der Kilimandscharo?",
        answerA = "Kenia",
        answerB = "Äthiopien",
        answerC = "Tansania",
        answerD = "Uganda",
        correctAnswer = 2, // C
        explanation = "Der Kilimandscharo liegt in Tansania, Ostafrika, und ist mit 5.895 m der höchste Berg Afrikas. Er ist ein erloschener Vulkan.",
        difficulty = 1,
        funFact = "Obwohl der Kilimandscharo nur rund 330 km vom Äquator entfernt liegt, hat sein Gipfel ganzjährig Schnee und Eis."
    ),

    // Question 33
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Südkorea?",
        answerA = "Busan",
        answerB = "Incheon",
        answerC = "Daegu",
        answerD = "Seoul",
        correctAnswer = 3, // D
        explanation = "Seoul ist die Hauptstadt Südkoreas und eine der größten Städte Asiens mit über 10 Millionen Einwohnern. Die Stadt ist bekannt für ihre Technologie und Kultur.",
        difficulty = 1,
        funFact = "Seoul bedeutet auf Koreanisch einfach \"Hauptstadt\"."
    ),

    // Question 34
    Question(
        categoryId = 1,
        questionText = "Welches Land besteht aus mehr als 7.000 Inseln?",
        answerA = "Indonesien",
        answerB = "Philippinen",
        answerC = "Japan",
        answerD = "Malaysia",
        correctAnswer = 1, // B
        explanation = "Die Philippinen bestehen aus über 7.600 Inseln im Westpazifik. Das Land liegt in Südostasien und ist nach dem spanischen König Philipp II. benannt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 35
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt Machu Picchu?",
        answerA = "Bolivien",
        answerB = "Chile",
        answerC = "Peru",
        answerD = "Ecuador",
        correctAnswer = 2, // C
        explanation = "Machu Picchu liegt in den Anden Perus auf 2.430 m Höhe. Die Inkastadt wurde im 15. Jahrhundert erbaut und ist heute eines der bekanntesten archäologischen Weltwunder.",
        difficulty = 1,
        funFact = "Machu Picchu wurde erst 1911 vom amerikanischen Historiker Hiram Bingham der westlichen Welt bekannt gemacht – die einheimische Bevölkerung kannte es jedoch immer."
    ),

    // Question 36
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Griechenland?",
        answerA = "Thessaloniki",
        answerB = "Korfu",
        answerC = "Athen",
        answerD = "Heraklion",
        correctAnswer = 2, // C
        explanation = "Athen ist die Hauptstadt Griechenlands und eine der ältesten Städte der Welt. Die Akropolis mit dem Parthenon ist ihr bekanntestes Wahrzeichen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 37
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt der Amazonas-Fluss?",
        answerA = "Afrika",
        answerB = "Südamerika",
        answerC = "Nordamerika",
        answerD = "Asien",
        correctAnswer = 1, // B
        explanation = "Der Amazonas fließt durch Südamerika, hauptsächlich durch Brasilien. Er führt mehr Süßwasser ins Meer als jeder andere Fluss der Erde.",
        difficulty = 1,
        funFact = "Der Amazonas trägt etwa 20 % des gesamten Süßwassers, das weltweit in Meere fließt."
    ),

    // Question 38
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von den Niederlanden?",
        answerA = "Rotterdam",
        answerB = "Den Haag",
        answerC = "Utrecht",
        answerD = "Amsterdam",
        correctAnswer = 3, // D
        explanation = "Amsterdam ist die Hauptstadt der Niederlande, obwohl die Regierung und das Parlament ihren Sitz in Den Haag haben. Amsterdam ist bekannt für seine Grachten.",
        difficulty = 1,
        funFact = "Amsterdam hat mehr Fahrräder als Einwohner – über 880.000 Fahrräder für rund 870.000 Menschen."
    ),

    // Question 39
    Question(
        categoryId = 1,
        questionText = "Welches ist der größte See der Welt?",
        answerA = "Victoriasee",
        answerB = "Huronsee",
        answerC = "Kaspisches Meer",
        answerD = "Oberer See",
        correctAnswer = 2, // C
        explanation = "Das Kaspische Meer ist technisch gesehen ein Salzsee und gilt als der flächenmäßig größte See der Erde mit rund 371.000 km². Es liegt zwischen Europa und Asien.",
        difficulty = 1,
        funFact = null
    ),

    // Question 40
    Question(
        categoryId = 1,
        questionText = "Wo befinden sich die Pyramiden von Gizeh?",
        answerA = "Sudan",
        answerB = "Ägypten",
        answerC = "Libyen",
        answerD = "Marokko",
        correctAnswer = 1, // B
        explanation = "Die Pyramiden von Gizeh stehen in Ägypten, nahe der heutigen Hauptstadt Kairo. Die Große Pyramide des Pharao Cheops ist das einzige noch weitgehend erhaltene der sieben Weltwunder der Antike.",
        difficulty = 1,
        funFact = "Die Große Pyramide besteht aus schätzungsweise 2,3 Millionen Steinblöcken, von denen jeder durchschnittlich 2,5 Tonnen wiegt."
    ),

    // Question 41
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von der Türkei?",
        answerA = "Istanbul",
        answerB = "Izmir",
        answerC = "Ankara",
        answerD = "Antalya",
        correctAnswer = 2, // C
        explanation = "Ankara ist die Hauptstadt der Türkei, obwohl viele Istanbul vermuten. Ankara wurde 1923 von Mustafa Kemal Atatürk zur Hauptstadt der neuen Republik erklärt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 42
    Question(
        categoryId = 1,
        questionText = "Welches Land ist von keinem anderen Meer oder Ozean direkt erreichbar (doppelt eingeschlossen)?",
        answerA = "Schweiz",
        answerB = "Österreich",
        answerC = "Usbekistan",
        answerD = "Tschechien",
        correctAnswer = 2, // C
        explanation = "Usbekistan und Liechtenstein sind die einzigen \"doppelt eingeschlossenen\" Länder – sie sind von Ländern umgeben, die ebenfalls keinen Meereszugang haben.",
        difficulty = 1,
        funFact = "Usbekistan grenzt an Afghanistan, Kasachstan, Kirgisistan, Tadschikistan und Turkmenistan – alles Binnenstaaten ohne Meereszugang."
    ),

    // Question 43
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Norwegen?",
        answerA = "Bergen",
        answerB = "Trondheim",
        answerC = "Stavanger",
        answerD = "Oslo",
        correctAnswer = 3, // D
        explanation = "Oslo ist die Hauptstadt Norwegens und liegt am Ende des Oslofjords. Mit rund 700.000 Einwohnern ist es die größte Stadt des Landes.",
        difficulty = 1,
        funFact = "Oslo war früher unter dem Namen \"Christiania\" bekannt – der Name wurde 1925 wieder in Oslo geändert."
    ),

    // Question 44
    Question(
        categoryId = 1,
        questionText = "Auf welcher Insel liegt der Vulkan Ätna?",
        answerA = "Korsika",
        answerB = "Sardinien",
        answerC = "Sizilien",
        answerD = "Mallorca",
        correctAnswer = 2, // C
        explanation = "Der Ätna liegt auf Sizilien, der größten Insel im Mittelmeer, die zu Italien gehört. Er ist der höchste aktive Vulkan Europas mit über 3.300 m Höhe.",
        difficulty = 1,
        funFact = null
    ),

    // Question 45
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kenia?",
        answerA = "Mombasa",
        answerB = "Nairobi",
        answerC = "Kampala",
        answerD = "Dar es Salaam",
        correctAnswer = 1, // B
        explanation = "Nairobi ist die Hauptstadt Kenias und die größte Stadt Ostafrikas. Die Stadt liegt auf einer Höhe von rund 1.795 m über dem Meeresspiegel.",
        difficulty = 1,
        funFact = "Nairobi ist eine der wenigen Städte der Welt, die einen Nationalpark innerhalb der Stadtgrenzen hat."
    ),

    // Question 46
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich der Nil-Delta?",
        answerA = "Sudan",
        answerB = "Äthiopien",
        answerC = "Ägypten",
        answerD = "Uganda",
        correctAnswer = 2, // C
        explanation = "Das Nildelta liegt in Ägypten, wo der Nil ins Mittelmeer mündet. Es ist eine der fruchtbarsten und dichtest besiedelten Regionen der Welt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 47
    Question(
        categoryId = 1,
        questionText = "Welche Wüste befindet sich in der Mongolei und Nordchina?",
        answerA = "Taklamakan",
        answerB = "Karakum",
        answerC = "Gobi",
        answerD = "Namib",
        correctAnswer = 2, // C
        explanation = "Die Gobi ist eine Wüste in der Mongolei und Nordchina. Mit einer Fläche von rund 1,3 Millionen km² ist sie die fünftgrößte Wüste der Erde.",
        difficulty = 1,
        funFact = "In der Gobi werden regelmäßig Dinosaurierfossilien gefunden – besonders Eier und Skelette sind dort gut erhalten geblieben."
    ),

    // Question 48
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Portugal?",
        answerA = "Porto",
        answerB = "Faro",
        answerC = "Lissabon",
        answerD = "Braga",
        correctAnswer = 2, // C
        explanation = "Lissabon ist die Hauptstadt Portugals und liegt an der Mündung des Tejo in den Atlantik. Sie ist eine der ältesten Städte Westeuropas.",
        difficulty = 1,
        funFact = "Lissabon liegt auf sieben Hügeln und ist bekannt für seine historischen Straßenbahnen und die traurige Musik des Fado."
    ),

    // Question 49
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die Form eines Stiefels?",
        answerA = "Spanien",
        answerB = "Griechenland",
        answerC = "Italien",
        answerD = "Kroatien",
        correctAnswer = 2, // C
        explanation = "Italien hat die bekannte Stiefelform auf der Landkarte. Die Halbinsel ragt weit ins Mittelmeer hinein, und der \"Absatz\" zeigt auf die Adria.",
        difficulty = 1,
        funFact = null
    ),

    // Question 50
    Question(
        categoryId = 1,
        questionText = "Welches ist der tiefste Punkt der Erdoberfläche?",
        answerA = "Totes Meer",
        answerB = "Marianengraben",
        answerC = "Baikalsee",
        answerD = "Titicacasee",
        correctAnswer = 1, // B
        explanation = "Der Marianengraben im Pazifischen Ozean ist mit einer Tiefe von etwa 10.994 m der tiefste Punkt der Erdoberfläche. Er liegt östlich der Marianen-Inseln.",
        difficulty = 1,
        funFact = "Wenn man den Mount Everest in den Marianengraben stellen würde, wäre der Gipfel noch über 2 km unter Wasser."
    ),

    // Question 51
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Japan?",
        answerA = "Osaka",
        answerB = "Kyoto",
        answerC = "Tokio",
        answerD = "Hiroshima",
        correctAnswer = 2, // C
        explanation = "Tokio ist die Hauptstadt Japans und mit über 13 Millionen Einwohnern (37 Millionen in der Metropolregion) die bevölkerungsreichste Stadtregion der Welt.",
        difficulty = 1,
        funFact = "Tokio war früher unter dem Namen \"Edo\" bekannt und wurde erst 1869 zur Hauptstadt, als Kaiser Meiji den Regierungssitz dorthin verlegte."
    ),

    // Question 52
    Question(
        categoryId = 1,
        questionText = "Welches ist der längste Fluss der Welt?",
        answerA = "Amazonas",
        answerB = "Jangtse",
        answerC = "Nil",
        answerD = "Mississippi",
        correctAnswer = 2, // C
        explanation = "Der Nil gilt traditionell als der längste Fluss der Erde mit einer Länge von etwa 6.650 km. Er fließt von Zentralafrika nordwärts und mündet in Ägypten ins Mittelmeer.",
        difficulty = 1,
        funFact = null
    ),

    // Question 53
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Ägypten?",
        answerA = "Asien",
        answerB = "Europa",
        answerC = "Afrika",
        answerD = "Naher Osten",
        correctAnswer = 2, // C
        explanation = "Ägypten liegt auf dem afrikanischen Kontinent, im Nordosten Afrikas. Ein kleiner Teil (Sinai-Halbinsel) liegt geographisch in Asien.",
        difficulty = 1,
        funFact = null
    ),

    // Question 54
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Schweden?",
        answerA = "Göteborg",
        answerB = "Malmö",
        answerC = "Uppsala",
        answerD = "Stockholm",
        correctAnswer = 3, // D
        explanation = "Stockholm ist die Hauptstadt Schwedens und liegt auf 14 Inseln, wo der Mälarsee auf die Ostsee trifft. Mit rund 975.000 Einwohnern ist sie die größte Stadt Skandinaviens.",
        difficulty = 1,
        funFact = "Stockholm ist die einzige Hauptstadt der Welt, die auf mehreren Inseln verteilt ist – daher wird sie auch \"Venedig des Nordens\" genannt."
    ),

    // Question 55
    Question(
        categoryId = 1,
        questionText = "Welches Gebirge bildet die natürliche Grenze zwischen Frankreich und Spanien?",
        answerA = "Alpen",
        answerB = "Pyrenäen",
        answerC = "Apenninen",
        answerD = "Vogesen",
        correctAnswer = 1, // B
        explanation = "Die Pyrenäen bilden die natürliche Grenze zwischen Frankreich und Spanien und erstrecken sich vom Atlantischen Ozean bis zum Mittelmeer über rund 430 km.",
        difficulty = 1,
        funFact = null
    ),

    // Question 56
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich das Great Barrier Reef?",
        answerA = "Neuseeland",
        answerB = "Papua-Neuguinea",
        answerC = "Australien",
        answerD = "Fidschi",
        correctAnswer = 2, // C
        explanation = "Das Great Barrier Reef liegt vor der Nordostküste Australiens im Korallenmeer. Mit über 2.300 km Länge ist es das größte Korallenriff der Welt.",
        difficulty = 1,
        funFact = "Das Great Barrier Reef ist das einzige lebende Ding auf der Erde, das vom Weltall aus sichtbar ist."
    ),

    // Question 57
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Polen?",
        answerA = "Krakau",
        answerB = "Danzig",
        answerC = "Warschau",
        answerD = "Breslau",
        correctAnswer = 2, // C
        explanation = "Warschau ist die Hauptstadt Polens und liegt an der Weichsel. Nach dem Zweiten Weltkrieg, in dem die Stadt fast vollständig zerstört wurde, wurde sie komplett wiederaufgebaut.",
        difficulty = 1,
        funFact = null
    ),

    // Question 58
    Question(
        categoryId = 1,
        questionText = "Welches ist der größte Ozean der Erde?",
        answerA = "Atlantischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Arktischer Ozean",
        answerD = "Pazifischer Ozean",
        correctAnswer = 3, // D
        explanation = "Der Pazifische Ozean ist der größte und tiefste Ozean der Erde. Er bedeckt mehr als ein Drittel der Erdoberfläche und ist größer als alle Landmassen zusammen.",
        difficulty = 1,
        funFact = "Der Pazifik ist so riesig, dass alle Kontinente der Welt hineinpassen würden und noch Platz übrig wäre."
    ),

    // Question 59
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Argentinien?",
        answerA = "Córdoba",
        answerB = "Rosario",
        answerC = "Mendoza",
        answerD = "Buenos Aires",
        correctAnswer = 3, // D
        explanation = "Buenos Aires ist die Hauptstadt Argentiniens und liegt am Río de la Plata. Die Stadt ist das kulturelle und wirtschaftliche Zentrum des Landes.",
        difficulty = 1,
        funFact = "Buenos Aires hat die meisten Buchhandlungen pro Einwohner aller Städte der Welt."
    ),

    // Question 60
    Question(
        categoryId = 1,
        questionText = "Auf welcher Insel liegt Hawaii?",
        answerA = "Im Atlantischen Ozean",
        answerB = "Im Indischen Ozean",
        answerC = "Im Pazifischen Ozean",
        answerD = "Im Karibischen Meer",
        correctAnswer = 2, // C
        explanation = "Hawaii ist ein US-Bundesstaat, der aus einer Inselkette im Pazifischen Ozean besteht. Die Inseln sind vulkanischen Ursprungs und liegen etwa 3.800 km von der amerikanischen Westküste entfernt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 61
    Question(
        categoryId = 1,
        questionText = "Welche Stadt gilt als \"Tor zur Antarktis\"?",
        answerA = "Buenos Aires",
        answerB = "Montevideo",
        answerC = "Santiago",
        answerD = "Ushuaia",
        correctAnswer = 3, // D
        explanation = "Ushuaia in Argentinien gilt als die südlichste Stadt der Welt und als Ausgangspunkt für Expeditionen in die Antarktis. Sie liegt in der Provinz Feuerland.",
        difficulty = 1,
        funFact = "Ushuaia liegt am Ende der Welt – dahinter beginnt die Drakepassage, die gefährlichste Meerenge der Erde."
    ),

    // Question 62
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Österreich?",
        answerA = "Salzburg",
        answerB = "Innsbruck",
        answerC = "Wien",
        answerD = "Graz",
        correctAnswer = 2, // C
        explanation = "Wien ist die Hauptstadt Österreichs und liegt an der Donau. Die Stadt war einst das Zentrum des mächtigen Habsburger Reiches und ist bekannt für ihre klassische Musik und Kaffeehauskultur.",
        difficulty = 1,
        funFact = "Wien wurde mehrfach zur lebenswertesten Stadt der Welt gewählt und ist bekannt für seine weltberühmten Musiker wie Mozart, Beethoven und Schubert."
    ),

    // Question 63
    Question(
        categoryId = 1,
        questionText = "Welches Land ist das flächenmäßig größte der Erde?",
        answerA = "Kanada",
        answerB = "USA",
        answerC = "China",
        answerD = "Russland",
        correctAnswer = 3, // D
        explanation = "Russland ist mit einer Fläche von über 17 Millionen km² das größte Land der Erde. Es erstreckt sich über 11 Zeitzonen und umfasst fast ein Achtel der gesamten Landfläche der Erde.",
        difficulty = 1,
        funFact = null
    ),

    // Question 64
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Viktoriasee?",
        answerA = "Er liegt nur in Tansania",
        answerB = "Er liegt nur in Kenia",
        answerC = "Er liegt in Uganda, Kenia und Tansania",
        answerD = "Er liegt nur in Uganda",
        correctAnswer = 2, // C
        explanation = "Der Viktoriasee liegt an der Grenze von Uganda, Kenia und Tansania. Mit einer Fläche von rund 68.000 km² ist er der größte See Afrikas und der zweitgrößte Süßwassersee der Welt.",
        difficulty = 1,
        funFact = "Der Viktoriasee ist die Quelle des Weißen Nils, der zusammen mit dem Blauen Nil den Nil bildet."
    ),

    // Question 65
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Südafrika (Verwaltungshauptstadt)?",
        answerA = "Kapstadt",
        answerB = "Johannesburg",
        answerC = "Durban",
        answerD = "Pretoria",
        correctAnswer = 3, // D
        explanation = "Pretoria ist die Verwaltungshauptstadt Südafrikas. Das Land hat drei Hauptstädte: Pretoria (Verwaltung), Kapstadt (Parlament) und Bloemfontein (Justiz).",
        difficulty = 1,
        funFact = "Südafrika ist das einzige Land der Welt mit drei Hauptstädten – jede für einen anderen Regierungsbereich."
    ),

    // Question 66
    Question(
        categoryId = 1,
        questionText = "Welches Gebirge ist das längste der Welt?",
        answerA = "Himalaya",
        answerB = "Rocky Mountains",
        answerC = "Anden",
        answerD = "Alpen",
        correctAnswer = 2, // C
        explanation = "Die Anden in Südamerika sind mit rund 7.500 km das längste Gebirge der Welt. Sie erstrecken sich entlang der gesamten Westküste Südamerikas von Venezuela bis Patagonien.",
        difficulty = 1,
        funFact = null
    ),

    // Question 67
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Nigeria?",
        answerA = "Lagos",
        answerB = "Kano",
        answerC = "Port Harcourt",
        answerD = "Abuja",
        correctAnswer = 3, // D
        explanation = "Abuja ist die Hauptstadt Nigerias und wurde 1991 anstelle von Lagos zur Hauptstadt erklärt. Die Stadt liegt geografisch im Zentrum des Landes.",
        difficulty = 1,
        funFact = "Viele Menschen denken, Lagos sei die Hauptstadt Nigerias, weil es mit über 15 Millionen Einwohnern die größte Stadt ist – aber Abuja ist die offizielle Hauptstadt."
    ),

    // Question 68
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich die Chinesische Mauer?",
        answerA = "Mongolei",
        answerB = "China",
        answerC = "Korea",
        answerD = "Vietnam",
        correctAnswer = 1, // B
        explanation = "Die Chinesische Mauer liegt in China und wurde über Jahrhunderte gebaut, um das Reich vor Angriffen aus dem Norden zu schützen. Sie ist das längste von Menschen gebaute Bauwerk der Erde.",
        difficulty = 1,
        funFact = "Der Mythos, dass die Chinesische Mauer vom Weltraum aus sichtbar ist, stimmt nicht – sie ist zu schmal, um mit bloßem Auge aus dem Orbit erkannt zu werden."
    ),

    // Question 69
    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen Europa und Afrika?",
        answerA = "Rotes Meer",
        answerB = "Arabisches Meer",
        answerC = "Schwarzes Meer",
        answerD = "Mittelmeer",
        correctAnswer = 3, // D
        explanation = "Das Mittelmeer liegt zwischen Europa im Norden und Afrika im Süden. Es erstreckt sich vom Atlantik im Westen bis zum Suezkanal im Osten und ist eines der wichtigsten Meere der Geschichte.",
        difficulty = 1,
        funFact = null
    ),

    // Question 70
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Dänemark?",
        answerA = "Göteborg",
        answerB = "Helsinki",
        answerC = "Kopenhagen",
        answerD = "Aarhus",
        correctAnswer = 2, // C
        explanation = "Kopenhagen ist die Hauptstadt Dänemarks und liegt auf der Insel Seeland. Die Stadt ist bekannt für ihr Fahrradfreundlichkeit, die Kleine Meerjungfrau-Skulptur und den Tivoli-Freizeitpark.",
        difficulty = 1,
        funFact = "Kopenhagen bedeutet auf Dänisch \"Kaufmannshafen\" (Køber + havn) und war ursprünglich ein wichtiger Handelshafen."
    ),

    // Question 71
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Baikalsee?",
        answerA = "Kasachstan",
        answerB = "Mongolei",
        answerC = "Ukraine",
        answerD = "Russland",
        correctAnswer = 3, // D
        explanation = "Der Baikalsee liegt in Sibirien, Russland. Er ist der tiefste See der Welt (1.642 m) und enthält etwa 20 % des gesamten flüssigen Süßwassers der Erde.",
        difficulty = 1,
        funFact = "Der Baikalsee ist so tief, dass alle fünf Großen Seen Nordamerikas zusammen hineinpassen würden."
    ),

    // Question 72
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von der Schweiz?",
        answerA = "Zürich",
        answerB = "Genf",
        answerC = "Basel",
        answerD = "Bern",
        correctAnswer = 3, // D
        explanation = "Bern ist die Bundesstadt (Hauptstadt) der Schweiz. Viele denken, Zürich oder Genf seien die Hauptstadt, weil sie größer und international bekannter sind.",
        difficulty = 1,
        funFact = "Bern bedeutet auf Berndeutsch \"Bär\" – und tatsächlich gibt es in der Altstadt seit Jahrhunderten eine Bärenanlage als Wahrzeichen der Stadt."
    ),

    // Question 73
    Question(
        categoryId = 1,
        questionText = "Welches Land ist die größte Demokratie der Welt?",
        answerA = "USA",
        answerB = "Brasilien",
        answerC = "Indien",
        answerD = "Indonesien",
        correctAnswer = 2, // C
        explanation = "Indien ist mit über 1,4 Milliarden Einwohnern und regelmäßigen freien Wahlen die größte Demokratie der Welt. Bei nationalen Wahlen sind über 900 Millionen Menschen wahlberechtigt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 74
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Neuseeland?",
        answerA = "Asien",
        answerB = "Ozeanien",
        answerC = "Australien",
        answerD = "Antarktika",
        correctAnswer = 1, // B
        explanation = "Neuseeland liegt in Ozeanien, im südwestlichen Pazifischen Ozean. Das Land besteht hauptsächlich aus zwei Hauptinseln – der Nord- und der Südinsel.",
        difficulty = 1,
        funFact = "Neuseeland war das erste Land der Welt, das Frauen das Wahlrecht gewährte – im Jahr 1893."
    ),

    // Question 75
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Belgien?",
        answerA = "Antwerpen",
        answerB = "Gent",
        answerC = "Brüssel",
        answerD = "Lüttich",
        correctAnswer = 2, // C
        explanation = "Brüssel ist die Hauptstadt Belgiens und beherbergt viele wichtige EU-Institutionen, weshalb die Stadt oft als inoffizielle Hauptstadt der Europäischen Union gilt.",
        difficulty = 1,
        funFact = "In Brüssel gibt es mehr Diplomaten und internationale Beamte als in New York City."
    ),

    // Question 76
    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Wüste der Welt (inklusive Kaltüsten)?",
        answerA = "Sahara",
        answerB = "Gobi",
        answerC = "Antarktika",
        answerD = "Arabische Wüste",
        correctAnswer = 2, // C
        explanation = "Die Antarktika ist die größte Wüste der Welt – eine Wüste ist definiert durch wenig Niederschlag, nicht durch Hitze. Die Antarktis erhält kaum Niederschlag und ist damit die größte Kältwüste.",
        difficulty = 1,
        funFact = null
    ),

    // Question 77
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Finnland?",
        answerA = "Tampere",
        answerB = "Turku",
        answerC = "Helsinki",
        answerD = "Espoo",
        correctAnswer = 2, // C
        explanation = "Helsinki ist die Hauptstadt Finnlands und liegt an einem Meeresarm der Ostsee. Mit rund 650.000 Einwohnern ist es die größte Stadt des Landes.",
        difficulty = 1,
        funFact = "Helsinki und Tallinn (Estland) liegen nur 80 km voneinander entfernt und sind per Fähre verbunden – es ist eine der kürzesten Fährverbindungen zwischen zwei Hauptstädten."
    ),

    // Question 78
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich Angkor Wat?",
        answerA = "Thailand",
        answerB = "Vietnam",
        answerC = "Kambodscha",
        answerD = "Myanmar",
        correctAnswer = 2, // C
        explanation = "Angkor Wat liegt in Kambodscha und ist der größte religiöse Tempelkomplex der Welt. Er wurde im 12. Jahrhundert von König Suryavarman II. erbaut.",
        difficulty = 1,
        funFact = "Angkor Wat ist auf der Nationalflagge Kambodschas abgebildet – es ist eines der wenigen Gebäude, das auf einer Nationalflagge zu sehen ist."
    ),

    // Question 79
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Ungarn?",
        answerA = "Debrecen",
        answerB = "Pécs",
        answerC = "Budapest",
        answerD = "Győr",
        correctAnswer = 2, // C
        explanation = "Budapest ist die Hauptstadt Ungarns und liegt an der Donau. Die Stadt entstand 1873 durch die Zusammenlegung von Buda (links der Donau) und Pest (rechts der Donau).",
        difficulty = 1,
        funFact = null
    ),

    // Question 80
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Einwohner?",
        answerA = "USA",
        answerB = "Indien",
        answerC = "China",
        answerD = "Indonesien",
        correctAnswer = 1, // B
        explanation = "Indien hat China 2023 als bevölkerungsreichstes Land der Welt überholt und hat nun über 1,4 Milliarden Einwohner. China liegt knapp dahinter.",
        difficulty = 1,
        funFact = "Indien hat eine so junge Bevölkerung, dass das Durchschnittsalter nur rund 28 Jahre beträgt – in Deutschland ist es über 45 Jahre."
    ),

    // Question 81
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Yellowstone-Nationalpark?",
        answerA = "Kanada",
        answerB = "Mexiko",
        answerC = "USA",
        answerD = "Alaska",
        correctAnswer = 2, // C
        explanation = "Der Yellowstone-Nationalpark liegt hauptsächlich im US-Bundesstaat Wyoming (sowie in Montana und Idaho). Er war 1872 der erste Nationalpark der Welt.",
        difficulty = 1,
        funFact = "Yellowstone liegt auf einem riesigen Supervulkan, der sich alle paar hunderttausend Jahre entlädt – der letzte Ausbruch war vor 640.000 Jahren."
    ),

    // Question 82
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Tschechien?",
        answerA = "Brünn",
        answerB = "Prag",
        answerC = "Ostrava",
        answerD = "Pilsen",
        correctAnswer = 1, // B
        explanation = "Prag ist die Hauptstadt Tschechiens und liegt an der Moldau. Die Stadt wird wegen ihrer gut erhaltenen Altstadt oft als \"die goldene Stadt\" bezeichnet.",
        difficulty = 1,
        funFact = "Prag wurde im Zweiten Weltkrieg kaum bombardiert und hat daher noch viele historische Gebäude aus dem Mittelalter und der Barockzeit erhalten."
    ),

    // Question 83
    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen der Arabischen Halbinsel und Afrika?",
        answerA = "Arabisches Meer",
        answerB = "Rotes Meer",
        answerC = "Indischer Ozean",
        answerD = "Golf von Aden",
        correctAnswer = 1, // B
        explanation = "Das Rote Meer liegt zwischen der Arabischen Halbinsel und dem nordöstlichen Afrika. Es verbindet über den Suezkanal das Mittelmeer mit dem Indischen Ozean.",
        difficulty = 1,
        funFact = null
    ),

    // Question 84
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Iran?",
        answerA = "Isfahan",
        answerB = "Maschhad",
        answerC = "Teheran",
        answerD = "Schiras",
        correctAnswer = 2, // C
        explanation = "Teheran ist die Hauptstadt des Iran und mit über 9 Millionen Einwohnern (15 Millionen in der Metropolregion) die größte Stadt des Landes und des gesamten Nahen Ostens.",
        difficulty = 1,
        funFact = "Teheran liegt am Fuß des Elburs-Gebirges und ist eine der höchstgelegenen Hauptstädte der Welt – Teile der Stadt liegen auf über 1.800 m Höhe."
    ),

    // Question 85
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Indonesien?",
        answerA = "Ozeanien",
        answerB = "Australien",
        answerC = "Asien",
        answerD = "Pazifik",
        correctAnswer = 2, // C
        explanation = "Indonesien liegt in Südostasien und besteht aus über 17.000 Inseln. Es ist das viertbevölkerungsreichste Land der Welt mit über 270 Millionen Einwohnern.",
        difficulty = 1,
        funFact = "Indonesien ist der einzige Staat weltweit, der auf zwei Ozeanrändern liegt – dem Pazifischen und dem Indischen Ozean."
    ),

    // Question 86
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Äthiopien?",
        answerA = "Dakar",
        answerB = "Addis Abeba",
        answerC = "Asmara",
        answerD = "Dschibuti",
        correctAnswer = 1, // B
        explanation = "Addis Abeba ist die Hauptstadt Äthiopiens und der Sitz der Afrikanischen Union. Die Stadt liegt auf einer Höhe von rund 2.355 m und ist damit eine der höchstgelegenen Hauptstädte Afrikas.",
        difficulty = 1,
        funFact = null
    ),

    // Question 87
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt das Kolosseum?",
        answerA = "Griechenland",
        answerB = "Türkei",
        answerC = "Spanien",
        answerD = "Italien",
        correctAnswer = 3, // D
        explanation = "Das Kolosseum steht in Rom, der Hauptstadt Italiens. Es wurde zwischen 70 und 80 n. Chr. erbaut und fasste bis zu 70.000 Zuschauer.",
        difficulty = 1,
        funFact = "Das Kolosseum ist das größte jemals gebaute Amphitheater. Obwohl es schwer beschädigt ist, zieht es jährlich rund 7 Millionen Besucher an."
    ),

    // Question 88
    Question(
        categoryId = 1,
        questionText = "Welcher Fluss fließt durch Kairo?",
        answerA = "Kongo",
        answerB = "Niger",
        answerC = "Sambesi",
        answerD = "Nil",
        correctAnswer = 3, // D
        explanation = "Der Nil fließt durch Kairo, die Hauptstadt Ägyptens. Der Fluss war die Lebensader der altägyptischen Zivilisation und ist noch heute für die Wasserversorgung Ägyptens unverzichtbar.",
        difficulty = 1,
        funFact = "Der Nil fließt von Süd nach Nord – er ist einer der wenigen großen Flüsse der Welt, der in diese Richtung fließt."
    ),

    // Question 89
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von den Vereinigten Arabischen Emiraten?",
        answerA = "Dubai",
        answerB = "Sharjah",
        answerC = "Abu Dhabi",
        answerD = "Ajman",
        correctAnswer = 2, // C
        explanation = "Abu Dhabi ist die Hauptstadt der Vereinigten Arabischen Emirate. Viele denken, Dubai sei die Hauptstadt, weil es internationaler bekannt ist – aber Abu Dhabi ist die offizielle Hauptstadt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 90
    Question(
        categoryId = 1,
        questionText = "Welcher Berg gilt als heiliger Berg Japans?",
        answerA = "Mount Fuji",
        answerB = "Tateyama",
        answerC = "Mount Koya",
        answerD = "Hokkaido",
        correctAnswer = 0, // A
        explanation = "Der Mount Fuji ist der höchste Berg Japans (3.776 m) und gilt als heiliger Berg. Er ist ein aktiver Vulkan und eines der bekanntesten Wahrzeichen Japans.",
        difficulty = 1,
        funFact = "Der Fuji ist seit dem Jahr 781 n. Chr. etwa 16 Mal ausgebrochen – der letzte Ausbruch war 1707."
    ),

    // Question 91
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Neuseeland?",
        answerA = "Auckland",
        answerB = "Christchurch",
        answerC = "Wellington",
        answerD = "Queenstown",
        correctAnswer = 2, // C
        explanation = "Wellington ist die Hauptstadt Neuseelands und liegt an der südlichen Spitze der Nordinsel. Obwohl Auckland die größte Stadt ist, ist Wellington die Hauptstadt.",
        difficulty = 1,
        funFact = "Wellington ist die südlichste Hauptstadt der Welt und gilt als eine der windigsten Städte weltweit."
    ),

    // Question 92
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich die Nazca-Linien?",
        answerA = "Mexiko",
        answerB = "Kolumbien",
        answerC = "Bolivien",
        answerD = "Peru",
        correctAnswer = 3, // D
        explanation = "Die Nazca-Linien liegen in der Nazca-Wüste in Peru. Diese riesigen Geoglyphen wurden von der Nazca-Kultur zwischen 200 v. Chr. und 600 n. Chr. in den Wüstenboden geritzt.",
        difficulty = 1,
        funFact = "Viele der Nazca-Figuren sind so groß, dass man sie erst aus dem Flugzeug vollständig erkennen kann – trotzdem wurden sie ohne moderne Technologie erschaffen."
    ),

    // Question 93
    Question(
        categoryId = 1,
        questionText = "Welches Land liegt auf der Iberischen Halbinsel (neben Spanien)?",
        answerA = "Frankreich",
        answerB = "Portugal",
        answerC = "Andorra",
        answerD = "Gibraltar",
        correctAnswer = 1, // B
        explanation = "Portugal liegt zusammen mit Spanien auf der Iberischen Halbinsel im Südwesten Europas. Portugal grenzt im Westen und Süden an den Atlantischen Ozean.",
        difficulty = 1,
        funFact = null
    ),

    // Question 94
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Thailand?",
        answerA = "Chiang Mai",
        answerB = "Phuket",
        answerC = "Bangkok",
        answerD = "Pattaya",
        correctAnswer = 2, // C
        explanation = "Bangkok ist die Hauptstadt Thailands und mit über 10 Millionen Einwohnern die größte Stadt des Landes. Sie liegt am Fluss Chao Phraya nahe dem Golf von Thailand.",
        difficulty = 1,
        funFact = "Bangkok hat den längsten offiziellen Stadtnamen der Welt – der vollständige Thai-Name hat 169 Zeichen und gilt als Zungenbrecher."
    ),

    // Question 95
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die längste Küstenlinie der Welt?",
        answerA = "Russland",
        answerB = "Australien",
        answerC = "Norwegen",
        answerD = "Kanada",
        correctAnswer = 3, // D
        explanation = "Kanada hat mit rund 202.080 km die längste Küstenlinie aller Länder weltweit. Das liegt an der riesigen Anzahl von Inseln, Fjorden und Buchten entlang seiner Küsten.",
        difficulty = 1,
        funFact = null
    ),

    // Question 96
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt Stonehenge?",
        answerA = "Irland",
        answerB = "Schottland",
        answerC = "Frankreich",
        answerD = "England",
        correctAnswer = 3, // D
        explanation = "Stonehenge liegt in Wiltshire, England. Das prähistorische Steinkreis-Monument wurde zwischen 3000 und 1500 v. Chr. errichtet und ist eines der bekanntesten Wahrzeichen Großbritanniens.",
        difficulty = 1,
        funFact = "Die genaue Funktion von Stonehenge ist bis heute nicht vollständig geklärt – Astronomen, Archäologen und Historiker streiten noch immer über seinen Zweck."
    ),

    // Question 97
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Südkorea?",
        answerA = "Incheon",
        answerB = "Busan",
        answerC = "Seoul",
        answerD = "Daejeon",
        correctAnswer = 2, // C
        explanation = "Seoul ist die Hauptstadt Südkoreas. Die Stadt wurde bereits 1394 zur Hauptstadt erklärt und hat sich zu einem der wichtigsten Technologie- und Kulturzentren der Welt entwickelt.",
        difficulty = 1,
        funFact = "In Seoul gibt es mehr WiFi-Hotspots als in fast jeder anderen Stadt der Welt – Südkorea hat die schnellste durchschnittliche Internetverbindung weltweit."
    ),

    // Question 98
    Question(
        categoryId = 1,
        questionText = "Welches ist das kleinste Land der Welt?",
        answerA = "Monaco",
        answerB = "San Marino",
        answerC = "Liechtenstein",
        answerD = "Vatikanstadt",
        correctAnswer = 3, // D
        explanation = "Die Vatikanstadt ist mit einer Fläche von nur 0,44 km² das kleinste Land der Welt. Sie liegt vollständig innerhalb von Rom und ist der Sitz des Papstes.",
        difficulty = 1,
        funFact = "Die Vatikanstadt hat ihre eigene Post, Radiostation, Zeitung, Münzprägeanstalt und sogar eine eigene Armee (die Schweizergarde)."
    ),

    // Question 99
    Question(
        categoryId = 1,
        questionText = "Durch welche Stadt fließt die Donau?",
        answerA = "Warschau",
        answerB = "Prag",
        answerC = "Budapest",
        answerD = "Brüssel",
        correctAnswer = 2, // C
        explanation = "Die Donau fließt durch Budapest, die Hauptstadt Ungarns. Der Fluss teilt die Stadt in Buda (Hügel, Westseite) und Pest (Flachland, Ostseite).",
        difficulty = 1,
        funFact = "Die Donau fließt durch 10 Länder und ist damit der internationalste Fluss der Welt."
    ),

    // Question 100
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Senegal?",
        answerA = "Dakar",
        answerB = "Abidjan",
        answerC = "Accra",
        answerD = "Bamako",
        correctAnswer = 0, // A
        explanation = "Dakar ist die Hauptstadt Senegals und liegt auf der Kap-Verde-Halbinsel, dem westlichsten Punkt des afrikanischen Kontinents.",
        difficulty = 1,
        funFact = null
    ),

    // Question 101
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Marokko?",
        answerA = "Casablanca",
        answerB = "Marrakesch",
        answerC = "Rabat",
        answerD = "Fès",
        correctAnswer = 2, // C
        explanation = "Rabat ist die Hauptstadt Marokkos, obwohl Casablanca die größte und wirtschaftlich bedeutendste Stadt des Landes ist.",
        difficulty = 1,
        funFact = "Viele Menschen verwechseln Casablanca mit der Hauptstadt Marokkos – tatsächlich ist Rabat seit 1912 die Hauptstadt."
    ),

    // Question 102
    Question(
        categoryId = 1,
        questionText = "Welcher Kanal verbindet den Atlantischen Ozean mit dem Pazifischen Ozean?",
        answerA = "Suezkanal",
        answerB = "Panamakanal",
        answerC = "Kielkanal",
        answerD = "Corinthkanal",
        correctAnswer = 1, // B
        explanation = "Der Panamakanal in Mittelamerika verbindet den Atlantischen mit dem Pazifischen Ozean. Er wurde 1914 eröffnet und ist eine der wichtigsten Schifffahrtsrouten der Welt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 103
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Kenia?",
        answerA = "Asien",
        answerB = "Südamerika",
        answerC = "Australien",
        answerD = "Afrika",
        correctAnswer = 3, // D
        explanation = "Kenia liegt in Ostafrika direkt am Äquator. Das Land ist bekannt für seine Nationalparks, Savannen und die Massai-Mara.",
        difficulty = 1,
        funFact = "In Kenia findet jedes Jahr die \"Große Wanderung\" statt, bei der Millionen von Gnus und Zebras durch das Land ziehen."
    ),

    // Question 104
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Peru?",
        answerA = "Buenos Aires",
        answerB = "Lima",
        answerC = "Santiago",
        answerD = "Bogotá",
        correctAnswer = 1, // B
        explanation = "Lima ist die Hauptstadt und größte Stadt Perus und liegt direkt am Pazifik. Über 10 Millionen Menschen leben in der Metropolregion.",
        difficulty = 1,
        funFact = null
    ),

    // Question 105
    Question(
        categoryId = 1,
        questionText = "Welches ist die größte Insel der Welt?",
        answerA = "Madagaskar",
        answerB = "Grönland",
        answerC = "Borneo",
        answerD = "Neuguinea",
        correctAnswer = 1, // B
        explanation = "Grönland ist mit über 2,1 Millionen km² die größte Insel der Welt und gehört zum dänischen Königreich.",
        difficulty = 1,
        funFact = "Obwohl Grönland \"Grünland\" bedeutet, ist 80 % der Insel von einem Eisschild bedeckt. Der Name war eine Art Marketingstrategie von Erik dem Roten."
    ),

    // Question 106
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Iguazú-Wasserfall?",
        answerA = "Brasilien und Argentinien",
        answerB = "Chile und Peru",
        answerC = "Venezuela und Kolumbien",
        answerD = "Bolivien und Paraguay",
        correctAnswer = 0, // A
        explanation = "Die Iguazú-Wasserfälle liegen an der Grenze zwischen Brasilien und Argentinien. Sie sind breiter als die Niagarafälle und eines der beeindruckendsten Naturschauspiele der Welt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 107
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kuba?",
        answerA = "Havanna",
        answerB = "Santiago de Cuba",
        answerC = "Varadero",
        answerD = "Trinidad",
        correctAnswer = 0, // A
        explanation = "Havanna ist die Hauptstadt Kubas und liegt an der Nordküste der Insel. Die Altstadt Havannas ist UNESCO-Weltkulturerbe.",
        difficulty = 1,
        funFact = "Havanna ist bekannt für seine bunten Oldtimer aus den 1950er Jahren, die noch heute als Taxis genutzt werden."
    ),

    // Question 108
    Question(
        categoryId = 1,
        questionText = "In welcher Hemisphäre liegt Australien?",
        answerA = "Nördliche Hemisphäre",
        answerB = "Östliche Hemisphäre",
        answerC = "Südliche Hemisphäre",
        answerD = "Westliche Hemisphäre",
        correctAnswer = 2, // C
        explanation = "Australien liegt vollständig auf der südlichen Hemisphäre, also südlich des Äquators. Dort sind die Jahreszeiten im Vergleich zur Nordhalbkugel umgekehrt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 109
    Question(
        categoryId = 1,
        questionText = "Welche Farben hat die Flagge Italiens?",
        answerA = "Blau, Weiß, Rot",
        answerB = "Grün, Weiß, Rot",
        answerC = "Rot, Gelb, Grün",
        answerD = "Blau, Weiß, Gelb",
        correctAnswer = 1, // B
        explanation = "Die italienische Flagge besteht aus drei gleich breiten senkrechten Streifen in Grün, Weiß und Rot. Sie wurde von der französischen Trikolore inspiriert.",
        difficulty = 1,
        funFact = "Die Farben der italienischen Flagge symbolisieren Hoffnung (Grün), Glauben (Weiß) und Nächstenliebe (Rot)."
    ),

    // Question 110
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kolumbien?",
        answerA = "Medellín",
        answerB = "Cartagena",
        answerC = "Cali",
        answerD = "Bogotá",
        correctAnswer = 3, // D
        explanation = "Bogotá ist die Hauptstadt und größte Stadt Kolumbiens. Die Stadt liegt auf einem Hochplateau in den Anden auf etwa 2.600 Metern Höhe.",
        difficulty = 1,
        funFact = null
    ),

    // Question 111
    Question(
        categoryId = 1,
        questionText = "Welcher Ozean liegt östlich von Afrika?",
        answerA = "Atlantischer Ozean",
        answerB = "Arktischer Ozean",
        answerC = "Indischer Ozean",
        answerD = "Pazifischer Ozean",
        correctAnswer = 2, // C
        explanation = "Östlich von Afrika liegt der Indische Ozean. Er ist der drittgrößte Ozean der Welt und grenzt an Afrika, Asien, Australien und die Antarktis.",
        difficulty = 1,
        funFact = "Der Indische Ozean ist der wärmste Ozean der Welt und trägt den Namen des Subkontinents Indien."
    ),

    // Question 112
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Serengeti-Nationalpark?",
        answerA = "Kenia",
        answerB = "Tansania",
        answerC = "Uganda",
        answerD = "Südafrika",
        correctAnswer = 1, // B
        explanation = "Der Serengeti-Nationalpark liegt in Tansania und ist eines der bekanntesten Wildschutzgebiete der Welt. Er ist berühmt für seine Tierwanderungen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 113
    Question(
        categoryId = 1,
        questionText = "Welches ist der größte Regenwald der Welt?",
        answerA = "Kongobecken",
        answerB = "Daintree Regenwald",
        answerC = "Amazonas-Regenwald",
        answerD = "Borneo Regenwald",
        correctAnswer = 2, // C
        explanation = "Der Amazonas-Regenwald in Südamerika ist der größte tropische Regenwald der Welt und bedeckt über 5,5 Millionen km².",
        difficulty = 1,
        funFact = "Der Amazonas-Regenwald produziert etwa 20 % des weltweiten Sauerstoffs und beherbergt 10 % aller Tier- und Pflanzenarten der Erde."
    ),

    // Question 114
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Algerien?",
        answerA = "Oran",
        answerB = "Algier",
        answerC = "Constantine",
        answerD = "Annaba",
        correctAnswer = 1, // B
        explanation = "Algier ist die Hauptstadt und größte Stadt Algeriens. Das Land ist das flächenmäßig größte in Afrika.",
        difficulty = 1,
        funFact = null
    ),

    // Question 115
    Question(
        categoryId = 1,
        questionText = "Welche Nationalflagge zeigt einen Ahornblatt?",
        answerA = "Australien",
        answerB = "Neuseeland",
        answerC = "Kanada",
        answerD = "Japan",
        correctAnswer = 2, // C
        explanation = "Die kanadische Flagge zeigt ein rotes Ahornblatt auf weißem Hintergrund mit zwei roten Seitenstreifen. Das Ahornblatt ist das Symbol Kanadas.",
        difficulty = 1,
        funFact = "Kanada hat die meisten Ahornsyrup-Produzenten der Welt und liefert rund 70 % der weltweiten Ahornsyrup-Produktion."
    ),

    // Question 116
    Question(
        categoryId = 1,
        questionText = "In welcher Zeitzone liegt London (Normalzeit)?",
        answerA = "UTC+1",
        answerB = "UTC-1",
        answerC = "UTC+0",
        answerD = "UTC+2",
        correctAnswer = 2, // C
        explanation = "London liegt in der Zeitzone UTC+0 (Greenwich Mean Time). Der Nullmeridian verläuft durch den Stadtteil Greenwich in London.",
        difficulty = 1,
        funFact = "Der Nullmeridian (0° Längengrad) verläuft durch das Königliche Observatorium in Greenwich und dient als Referenz für alle Zeitzonen der Welt."
    ),

    // Question 117
    Question(
        categoryId = 1,
        questionText = "Welcher Fluss ist der längste in Europa?",
        answerA = "Rhein",
        answerB = "Donau",
        answerC = "Wolga",
        answerD = "Loire",
        correctAnswer = 2, // C
        explanation = "Die Wolga in Russland ist mit etwa 3.530 km der längste Fluss Europas. Sie fließt in das Kaspische Meer.",
        difficulty = 1,
        funFact = null
    ),

    // Question 118
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Vietnam?",
        answerA = "Ho-Chi-Minh-Stadt",
        answerB = "Hanoi",
        answerC = "Da Nang",
        answerD = "Hue",
        correctAnswer = 1, // B
        explanation = "Hanoi ist die Hauptstadt Vietnams und liegt im Norden des Landes. Ho-Chi-Minh-Stadt (ehemals Saigon) ist zwar größer, aber nicht die Hauptstadt.",
        difficulty = 1,
        funFact = "Viele Menschen denken, Ho-Chi-Minh-Stadt sei die Hauptstadt Vietnams, weil es die größte Stadt ist. Die eigentliche Hauptstadt ist jedoch Hanoi."
    ),

    // Question 119
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Ägypten zum größten Teil?",
        answerA = "Asien",
        answerB = "Europa",
        answerC = "Afrika",
        answerD = "Naher Osten",
        correctAnswer = 2, // C
        explanation = "Ägypten liegt hauptsächlich in Nordafrika, obwohl der Sinai-Halbinsel-Teil des Landes geographisch zu Asien gehört. Die überwiegende Fläche ist Afrika.",
        difficulty = 1,
        funFact = null
    ),

    // Question 120
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt der Ukraine?",
        answerA = "Lemberg",
        answerB = "Charkiw",
        answerC = "Kiew",
        answerD = "Odessa",
        correctAnswer = 2, // C
        explanation = "Kiew (ukrainisch: Kyiv) ist die Hauptstadt und größte Stadt der Ukraine. Die Stadt liegt am Fluss Dnjepr und hat eine über 1.500-jährige Geschichte.",
        difficulty = 1,
        funFact = "Kiew gilt als die \"Mutter aller russischen Städte\" und war das Zentrum des mittelalterlichen Kiewer Rus."
    ),

    // Question 121
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die Form eines langen, schmalen Streifens entlang der Westküste Südamerikas?",
        answerA = "Peru",
        answerB = "Ecuador",
        answerC = "Chile",
        answerD = "Bolivien",
        correctAnswer = 2, // C
        explanation = "Chile ist das längste und schmalste Land der Welt. Es erstreckt sich über 4.300 km von Nord nach Süd, ist aber im Durchschnitt nur 175 km breit.",
        difficulty = 1,
        funFact = "Chile ist so lang, dass es vom Äquator bis fast zur Antarktis reicht – das entspricht der Entfernung von Nordafrika bis nach Norwegen."
    ),

    // Question 122
    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt nördlich von Russland?",
        answerA = "Ostsee",
        answerB = "Arktischer Ozean",
        answerC = "Schwarzes Meer",
        answerD = "Kaspisches Meer",
        correctAnswer = 1, // B
        explanation = "Nördlich von Russland liegt der Arktische Ozean. Russland hat die längste Arktis-Küstenlinie aller Länder.",
        difficulty = 1,
        funFact = null
    ),

    // Question 123
    Question(
        categoryId = 1,
        questionText = "In welchem Land befindet sich der Galápagos-Archipel?",
        answerA = "Peru",
        answerB = "Kolumbien",
        answerC = "Ecuador",
        answerD = "Venezuela",
        correctAnswer = 2, // C
        explanation = "Die Galápagos-Inseln gehören zu Ecuador und liegen im Pazifischen Ozean, etwa 1.000 km von der Küste entfernt. Charles Darwin besuchte sie 1835.",
        difficulty = 1,
        funFact = "Auf den Galápagos-Inseln leben Tiere, die sonst nirgendwo auf der Welt vorkommen – wie Riesenschildkröten, Meerechsen und Blaufußtölpel."
    ),

    // Question 124
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Pakistan?",
        answerA = "Karachi",
        answerB = "Lahore",
        answerC = "Islamabad",
        answerD = "Peshawar",
        correctAnswer = 2, // C
        explanation = "Islamabad ist die Hauptstadt Pakistans, obwohl Karachi die größte Stadt ist. Islamabad wurde in den 1960er Jahren als neue Hauptstadt geplant und gebaut.",
        difficulty = 1,
        funFact = null
    ),

    // Question 125
    Question(
        categoryId = 1,
        questionText = "Welches Land liegt auf der Insel Borneo nicht?",
        answerA = "Malaysia",
        answerB = "Brunei",
        answerC = "Indonesien",
        answerD = "Philippinen",
        correctAnswer = 3, // D
        explanation = "Borneo ist zwischen drei Ländern aufgeteilt: Indonesien (größter Teil), Malaysia und Brunei. Die Philippinen liegen auf eigenen Inseln nördlich von Borneo.",
        difficulty = 1,
        funFact = "Borneo ist die drittgrößte Insel der Welt und Heimat von Orang-Utans, Zwergelefanten und dem fleischfressenden Rafflesiae-Blütenbaum."
    ),

    // Question 126
    Question(
        categoryId = 1,
        questionText = "Welcher Fluss fließt durch den Grand Canyon?",
        answerA = "Mississippi",
        answerB = "Missouri",
        answerC = "Colorado",
        answerD = "Rio Grande",
        correctAnswer = 2, // C
        explanation = "Der Colorado River hat über Millionen von Jahren den Grand Canyon in Arizona, USA ausgegraben. Der Canyon ist über 1.600 Meter tief.",
        difficulty = 1,
        funFact = "Der Grand Canyon ist nicht der tiefste oder längste Canyon der Welt, aber er ist der bekannteste aufgrund seiner farbenfrohen Gesteinsschichten."
    ),

    // Question 127
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kamerun?",
        answerA = "Douala",
        answerB = "Yaoundé",
        answerC = "Bamenda",
        answerD = "Garoua",
        correctAnswer = 1, // B
        explanation = "Yaoundé ist die Hauptstadt Kameruns, obwohl Douala die größte und wirtschaftlich bedeutendste Stadt des Landes ist.",
        difficulty = 1,
        funFact = null
    ),

    // Question 128
    Question(
        categoryId = 1,
        questionText = "Auf welchem Kontinent liegt Indonesien?",
        answerA = "Australien",
        answerB = "Asien",
        answerC = "Afrika",
        answerD = "Ozeanien",
        correctAnswer = 1, // B
        explanation = "Indonesien gehört geographisch zu Südostasien. Das Land besteht aus über 17.000 Inseln und ist das größte Inselstaat der Welt.",
        difficulty = 1,
        funFact = "Indonesien hat die viertgrößte Bevölkerung der Welt und liegt auf dem sogenannten Pazifischen Feuerring, was es sehr erdbeben- und vulkanaktiv macht."
    ),

    // Question 129
    Question(
        categoryId = 1,
        questionText = "Welches Land ist der größte Kaffeeproduzent der Welt?",
        answerA = "Kolumbien",
        answerB = "Äthiopien",
        answerC = "Vietnam",
        answerD = "Brasilien",
        correctAnswer = 3, // D
        explanation = "Brasilien ist mit Abstand der größte Kaffeeproduzent der Welt und produziert etwa ein Drittel des gesamten Weltjahresvorrats an Kaffee.",
        difficulty = 1,
        funFact = "Kaffee stammt ursprünglich aus Äthiopien, wurde aber hauptsächlich durch Brasilien zur weltweiten Massenware."
    ),

    // Question 130
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Rumänien?",
        answerA = "Cluj-Napoca",
        answerB = "Timișoara",
        answerC = "Bukarest",
        answerD = "Brașov",
        correctAnswer = 2, // C
        explanation = "Bukarest ist die Hauptstadt und größte Stadt Rumäniens. Sie liegt im Süden des Landes und wird manchmal als \"kleines Paris\" des Ostens bezeichnet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 131
    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen Griechenland und der Türkei?",
        answerA = "Adriatisches Meer",
        answerB = "Ägäisches Meer",
        answerC = "Schwarzes Meer",
        answerD = "Ionisches Meer",
        correctAnswer = 1, // B
        explanation = "Das Ägäische Meer liegt zwischen Griechenland und der Türkei. Es ist ein Teil des Mittelmeers und enthält Tausende griechischer Inseln.",
        difficulty = 1,
        funFact = "Das Ägäische Meer hat seinen Namen vermutlich vom mythologischen König Ägeus, dem Vater des Helden Theseus aus der griechischen Mythologie."
    ),

    // Question 132
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Mount Everest?",
        answerA = "China",
        answerB = "Indien",
        answerC = "Nepal und China",
        answerD = "Tibet",
        correctAnswer = 2, // C
        explanation = "Der Mount Everest liegt auf der Grenze zwischen Nepal und China (Tibet). Die Südseite gehört zu Nepal, die Nordseite zu China.",
        difficulty = 1,
        funFact = "Der Mount Everest wächst jedes Jahr um etwa 4 mm durch tektonische Kräfte, schrumpft aber auch leicht durch Erosion."
    ),

    // Question 133
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Irland?",
        answerA = "Cork",
        answerB = "Galway",
        answerC = "Limerick",
        answerD = "Dublin",
        correctAnswer = 3, // D
        explanation = "Dublin ist die Hauptstadt und größte Stadt der Republik Irland. Sie liegt an der Ostküste der Insel an der Mündung des Flusses Liffey.",
        difficulty = 1,
        funFact = "Der Name Dublin kommt vom irischen \"Dubh Linn\", was \"schwarzer Teich\" bedeutet."
    ),

    // Question 134
    Question(
        categoryId = 1,
        questionText = "Welche Insel ist die größte des Pazifischen Ozeans (außer Australien)?",
        answerA = "Borneo",
        answerB = "Neuguinea",
        answerC = "Sumatra",
        answerD = "Hokkaido",
        correctAnswer = 1, // B
        explanation = "Neuguinea ist die größte Insel im Pazifik und die zweitgrößte der Welt. Sie ist zwischen Papua-Neuguinea und Indonesien aufgeteilt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 135
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten Pyramiden der Welt?",
        answerA = "Ägypten",
        answerB = "Mexiko",
        answerC = "Sudan",
        answerD = "Peru",
        correctAnswer = 2, // C
        explanation = "Überraschenderweise hat der Sudan mehr Pyramiden als Ägypten – über 200 Pyramiden aus der Zeit des antiken Königreichs Kusch und Meroe.",
        difficulty = 1,
        funFact = "Die sudanesischen Pyramiden sind schmaler und spitzer als die ägyptischen, aber es gibt viel mehr davon."
    ),

    // Question 136
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Malaysia?",
        answerA = "Penang",
        answerB = "Johor Bahru",
        answerC = "Kuala Lumpur",
        answerD = "Ipoh",
        correctAnswer = 2, // C
        explanation = "Kuala Lumpur ist die Hauptstadt Malaysias und bekannt für die Petronas Twin Towers, die lange Zeit die höchsten Gebäude der Welt waren.",
        difficulty = 1,
        funFact = "\"Kuala Lumpur\" bedeutet auf Malaysisch \"schlammiger Zusammenfluss\" und bezieht sich auf die Stelle, wo zwei schlammige Flüsse zusammentreffen."
    ),

    // Question 137
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Yellowstone-Nationalpark?",
        answerA = "Kanada",
        answerB = "USA",
        answerC = "Mexiko",
        answerD = "Australien",
        correctAnswer = 1, // B
        explanation = "Der Yellowstone-Nationalpark liegt in den USA, hauptsächlich im Bundesstaat Wyoming. Er war 1872 der erste Nationalpark der Welt.",
        difficulty = 1,
        funFact = "Unter dem Yellowstone-Nationalpark liegt ein riesiger Supervulkan, der zuletzt vor etwa 640.000 Jahren ausbrach."
    ),

    // Question 138
    Question(
        categoryId = 1,
        questionText = "Welches Land liegt zwischen Frankreich und Spanien in den Pyrenäen?",
        answerA = "Monaco",
        answerB = "San Marino",
        answerC = "Liechtenstein",
        answerD = "Andorra",
        correctAnswer = 3, // D
        explanation = "Das Fürstentum Andorra liegt in den Pyrenäen zwischen Frankreich und Spanien. Es ist eines der kleinsten Länder Europas.",
        difficulty = 1,
        funFact = null
    ),

    // Question 139
    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat die meisten Länder?",
        answerA = "Asien",
        answerB = "Afrika",
        answerC = "Europa",
        answerD = "Südamerika",
        correctAnswer = 1, // B
        explanation = "Afrika hat mit 54 anerkannten Ländern die meisten Staaten aller Kontinente. Asien folgt mit 48 Ländern.",
        difficulty = 1,
        funFact = "Trotz seiner großen Anzahl an Ländern ist Afrika in der Weltpolitik oft unterrepräsentiert."
    ),

    // Question 140
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kroatien?",
        answerA = "Split",
        answerB = "Dubrovnik",
        answerC = "Zagreb",
        answerD = "Zadar",
        correctAnswer = 2, // C
        explanation = "Zagreb ist die Hauptstadt und größte Stadt Kroatiens. Sie liegt im Norden des Landes und ist ein wichtiges kulturelles und wirtschaftliches Zentrum.",
        difficulty = 1,
        funFact = "Dubrovnik, bekannt als \"Perle der Adria\", ist zwar touristisch bekannter als Zagreb, aber eben nicht die Hauptstadt Kroatiens."
    ),

    // Question 141
    Question(
        categoryId = 1,
        questionText = "Welches Land grenzt im Norden an Brasilien?",
        answerA = "Peru",
        answerB = "Venezuela",
        answerC = "Bolivien",
        answerD = "Chile",
        correctAnswer = 1, // B
        explanation = "Venezuela grenzt im Norden an Brasilien. Weitere Länder, die an Brasilien grenzen, sind Kolumbien, Peru, Bolivien, Paraguay, Argentinien, Uruguay, Guyana, Suriname und Französisch-Guayana.",
        difficulty = 1,
        funFact = null
    ),

    // Question 142
    Question(
        categoryId = 1,
        questionText = "Welches ist das kleinste Land Afrikas?",
        answerA = "Gambia",
        answerB = "Dschibuti",
        answerC = "Seychellen",
        answerD = "Kap Verde",
        correctAnswer = 2, // C
        explanation = "Die Seychellen sind ein Inselstaat im Indischen Ozean und das kleinste Land Afrikas. Das Land besteht aus 115 Inseln.",
        difficulty = 1,
        funFact = "Die Seychellen sind auch der bevölkerungsärmste Staat Afrikas – nur etwa 98.000 Menschen leben dort."
    ),

    // Question 143
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Kilimandscharo-Nationalpark?",
        answerA = "Kenia",
        answerB = "Ruanda",
        answerC = "Tansania",
        answerD = "Uganda",
        correctAnswer = 2, // C
        explanation = "Der Kilimandscharo liegt in Tansania, Ostafrika, und ist mit 5.895 Metern der höchste Berg Afrikas. Er ist ein Stratovulkan.",
        difficulty = 1,
        funFact = "Obwohl der Kilimandscharo nahe dem Äquator liegt, ist sein Gipfel ganzjährig mit Schnee und Eis bedeckt – aber die Eiskappe schmilzt aufgrund des Klimawandels."
    ),

    // Question 144
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Ecuador?",
        answerA = "Guayaquil",
        answerB = "Cuenca",
        answerC = "Quito",
        answerD = "Manta",
        correctAnswer = 2, // C
        explanation = "Quito ist die Hauptstadt Ecuadors und liegt in den Anden auf etwa 2.850 Metern Höhe. Der Name Ecuador kommt vom spanischen Wort für Äquator.",
        difficulty = 1,
        funFact = null
    ),

    // Question 145
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die Flagge mit dem Davidstern?",
        answerA = "Saudi-Arabien",
        answerB = "Jordanien",
        answerC = "Israel",
        answerD = "Libanon",
        correctAnswer = 2, // C
        explanation = "Die israelische Flagge zeigt den blauen Davidstern (Magen David) auf weißem Hintergrund mit zwei blauen Streifen – ein Symbol des jüdischen Volkes.",
        difficulty = 1,
        funFact = "Der Davidstern hat sechs Zacken und ist seit dem Mittelalter ein Symbol des Judentums. Er wurde 1948 ins Zentrum der israelischen Flagge aufgenommen."
    ),

    // Question 146
    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Südamerikas?",
        answerA = "Kolumbien",
        answerB = "Argentinien",
        answerC = "Venezuela",
        answerD = "Brasilien",
        correctAnswer = 3, // D
        explanation = "Brasilien ist mit über 215 Millionen Einwohnern das bevölkerungsreichste Land Südamerikas und das fünftbevölkerungsreichste Land der Welt.",
        difficulty = 1,
        funFact = "Etwa die Hälfte aller Südamerikaner sind Brasilianer, und Brasiliens Fläche macht fast die Hälfte des gesamten südamerikanischen Kontinents aus."
    ),

    // Question 147
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Irland?",
        answerA = "Belfast",
        answerB = "Limerick",
        answerC = "Dublin",
        answerD = "Cork",
        correctAnswer = 2, // C
        explanation = "Dublin ist die Hauptstadt der Republik Irland. Belfast hingegen ist die Hauptstadt von Nordirland, das zum Vereinigten Königreich gehört.",
        difficulty = 1,
        funFact = null
    ),

    // Question 148
    Question(
        categoryId = 1,
        questionText = "Welches Land liegt vollständig auf der nördlichen Hemisphäre?",
        answerA = "Brasilien",
        answerB = "Kenia",
        answerC = "Norwegen",
        answerD = "Ecuador",
        correctAnswer = 2, // C
        explanation = "Norwegen liegt vollständig auf der nördlichen Hemisphäre, weit nördlich des Äquators. Länder wie Brasilien, Kenia und Ecuador werden vom Äquator geschnitten.",
        difficulty = 1,
        funFact = "Norwegen liegt so weit im Norden, dass in Teilen des Landes im Sommer die Sonne nicht untergeht – ein Phänomen namens Mitternachtssonne."
    ),

    // Question 149
    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen Italien und der Balkanhalbinsel?",
        answerA = "Tyrrhenisches Meer",
        answerB = "Ligurisches Meer",
        answerC = "Adriatisches Meer",
        answerD = "Ionisches Meer",
        correctAnswer = 2, // C
        explanation = "Das Adriatische Meer liegt zwischen der italienischen Halbinsel und der Balkanhalbinsel. Es ist ein Teil des Mittelmeers.",
        difficulty = 1,
        funFact = "Venedig liegt am nördlichsten Ende des Adriatischen Meers und ist bekannt für seine Kanäle, die als Straßen dienen."
    ),

    // Question 150
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Slowenien?",
        answerA = "Maribor",
        answerB = "Koper",
        answerC = "Celje",
        answerD = "Ljubljana",
        correctAnswer = 3, // D
        explanation = "Ljubljana ist die Hauptstadt und größte Stadt Sloweniens. Das Land grenzt an Österreich, Italien, Ungarn und Kroatien.",
        difficulty = 1,
        funFact = null
    ),

    // Question 151
    Question(
        categoryId = 1,
        questionText = "Welche karibische Insel ist die größte der Karibik?",
        answerA = "Jamaika",
        answerB = "Puerto Rico",
        answerC = "Kuba",
        answerD = "Hispaniola",
        correctAnswer = 2, // C
        explanation = "Kuba ist die größte Insel der Karibik mit einer Fläche von rund 110.860 km². Sie liegt südlich von Florida und ist für ihre Zigarren, Musik und den Rum bekannt.",
        difficulty = 1,
        funFact = "Kuba ist das einzige sozialistische Land der westlichen Hemisphäre und wurde seit 1959 von der Familie Castro regiert."
    ),

    // Question 152
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Costa Rica?",
        answerA = "Liberia",
        answerB = "San José",
        answerC = "Limón",
        answerD = "Cartago",
        correctAnswer = 1, // B
        explanation = "San José ist die Hauptstadt und größte Stadt Costa Ricas. Das mittelamerikanische Land ist bekannt für seinen Ökotourismus und seine artenreiche Natur.",
        difficulty = 1,
        funFact = null
    ),

    // Question 153
    Question(
        categoryId = 1,
        questionText = "Auf welcher Insel liegt der Vulkan Krakatau?",
        answerA = "Borneo",
        answerB = "Java",
        answerC = "Sumatra",
        answerD = "Zwischen Java und Sumatra",
        correctAnswer = 3, // D
        explanation = "Der Krakatau liegt in der Sundastraße zwischen den Inseln Java und Sumatra in Indonesien. Er ist einer der bekanntesten aktiven Vulkane der Welt.",
        difficulty = 1,
        funFact = "Der Ausbruch des Krakatau im Jahr 1883 war so gewaltig, dass der Knall noch 5.000 km entfernt gehört wurde – es war eine der lautesten Explosionen der Menschheitsgeschichte."
    ),

    // Question 154
    Question(
        categoryId = 1,
        questionText = "Welches ist der größte See Nordamerikas?",
        answerA = "Michigansee",
        answerB = "Huronsee",
        answerC = "Oberer See",
        answerD = "Eriesee",
        correctAnswer = 2, // C
        explanation = "Der Obere See (Lake Superior) ist der größte der fünf Großen Seen Nordamerikas und der flächenmäßig größte Süßwassersee der Welt mit rund 82.100 km².",
        difficulty = 1,
        funFact = null
    ),

    // Question 155
    Question(
        categoryId = 1,
        questionText = "In welchem Land steht die Golden Gate Bridge?",
        answerA = "Kanada",
        answerB = "Mexiko",
        answerC = "USA",
        answerD = "Australien",
        correctAnswer = 2, // C
        explanation = "Die Golden Gate Bridge steht in San Francisco, USA. Sie überspannt die Golden Gate-Meerenge zwischen San Francisco Bay und dem Pazifischen Ozean.",
        difficulty = 1,
        funFact = "Die Golden Gate Bridge wurde 1937 eingeweiht und war damals die längste Hängebrücke der Welt. Ihr charakteristisches Farbe ist offiziell als \"International Orange\" bekannt."
    ),

    // Question 156
    Question(
        categoryId = 1,
        questionText = "Welcher skandinavische Staat hat keine Landesgrenze zu Russland?",
        answerA = "Norwegen",
        answerB = "Dänemark",
        answerC = "Schweden",
        answerD = "Finnland",
        correctAnswer = 1, // B
        explanation = "Dänemark hat keine gemeinsame Grenze mit Russland. Norwegen und Finnland grenzen direkt an Russland, und Schweden grenzt an Finnland, das wiederum an Russland grenzt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 157
    Question(
        categoryId = 1,
        questionText = "Welches UNESCO-Welterbe liegt in Jordanien?",
        answerA = "Angkor Wat",
        answerB = "Petra",
        answerC = "Palmyra",
        answerD = "Jerash",
        correctAnswer = 1, // B
        explanation = "Petra ist die bekannteste UNESCO-Welterbestätte Jordaniens. Diese alte Felsstadt der Nabatäer wurde in Sandstein gehauen und ist seit 1985 Weltkulturerbe.",
        difficulty = 1,
        funFact = "Petra wird auch \"Rosarote Stadt\" genannt, weil der Sandstein, aus dem sie gehauen wurde, in verschiedenen Rottönen schimmert."
    ),

    // Question 158
    Question(
        categoryId = 1,
        questionText = "Welches Land liegt auf der Arabischen Halbinsel und grenzt an den Jemen?",
        answerA = "Jordanien",
        answerB = "Kuwait",
        answerC = "Bahrain",
        answerD = "Saudi-Arabien",
        correctAnswer = 3, // D
        explanation = "Saudi-Arabien grenzt im Süden an den Jemen. Das Königreich nimmt den größten Teil der Arabischen Halbinsel ein und liegt am Roten Meer sowie am Persischen Golf.",
        difficulty = 1,
        funFact = null
    ),

    // Question 159
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Singapur?",
        answerA = "Singapur-Stadt",
        answerB = "Changi",
        answerC = "Jurong",
        answerD = "Woodlands",
        correctAnswer = 0, // A
        explanation = "Singapur ist ein Stadtstaat – das ganze Land ist gleichzeitig die Stadt. Die Hauptstadt heißt Singapur-Stadt und ist identisch mit dem Staat Singapur.",
        difficulty = 1,
        funFact = "Singapur ist einer der kleinsten Staaten der Welt, aber einer der reichsten pro Kopf. Es hat keinen Hinterland – praktisch kein Trinkwasser und keine natürlichen Ressourcen."
    ),

    // Question 160
    Question(
        categoryId = 1,
        questionText = "Welcher Vulkan liegt auf der Insel Hawaii und ist einer der aktivsten der Welt?",
        answerA = "Mauna Kea",
        answerB = "Kilauea",
        answerC = "Mauna Loa",
        answerD = "Hualalai",
        correctAnswer = 1, // B
        explanation = "Der Kilauea auf der großen Insel Hawaii gilt als einer der aktivsten Vulkane der Welt und bricht fast ununterbrochen aus. Er gehört zum Hawaii-Volcanoes-Nationalpark.",
        difficulty = 1,
        funFact = "Der Kilauea bricht seit 1983 fast ununterbrochen aus und hat dabei neues Land ins Meer hinein geschaffen – Hawaii wächst dadurch buchstäblich."
    ),

    // Question 161
    Question(
        categoryId = 1,
        questionText = "Welches mittelamerikanische Land hat keine Armee?",
        answerA = "Honduras",
        answerB = "Guatemala",
        answerC = "Costa Rica",
        answerD = "Panama",
        correctAnswer = 2, // C
        explanation = "Costa Rica hat 1948 als erstes Land der Welt sein Militär abgeschafft. Seitdem gibt es dort keine Armee – das Geld wird in Bildung und Gesundheit investiert.",
        difficulty = 1,
        funFact = null
    ),

    // Question 162
    Question(
        categoryId = 1,
        questionText = "Welche australische Stadt liegt am südlichsten?",
        answerA = "Sydney",
        answerB = "Melbourne",
        answerC = "Adelaide",
        answerD = "Hobart",
        correctAnswer = 3, // D
        explanation = "Hobart ist die Hauptstadt des australischen Bundesstaates Tasmanien und die südlichste Großstadt Australiens. Sie liegt auf der Insel Tasmanien südlich des australischen Festlands.",
        difficulty = 1,
        funFact = "Tasmanien ist von der Roaring Forties umgeben – starken Westwinden in 40-50 Grad südlicher Breite, die das Klima der Insel prägen."
    ),

    // Question 163
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Guatemala?",
        answerA = "Belize City",
        answerB = "Tegucigalpa",
        answerC = "Managua",
        answerD = "Guatemala-Stadt",
        correctAnswer = 3, // D
        explanation = "Guatemala-Stadt (Ciudad de Guatemala) ist die Hauptstadt Guatemalas und die größte Stadt Mittelamerikas mit rund 3 Millionen Einwohnern in der Metropolregion.",
        difficulty = 1,
        funFact = null
    ),

    // Question 164
    Question(
        categoryId = 1,
        questionText = "Welches ist der tiefste See der Welt?",
        answerA = "Tanganjikasee",
        answerB = "Victoriasee",
        answerC = "Baikalsee",
        answerD = "Kaspisches Meer",
        correctAnswer = 2, // C
        explanation = "Der Baikalsee in Sibirien, Russland, ist mit einer Tiefe von 1.642 Metern der tiefste See der Welt. Er enthält etwa 20 % des gesamten flüssigen Süßwassers der Erde.",
        difficulty = 1,
        funFact = "Der Baikalsee ist so tief und alt (ca. 25 Millionen Jahre), dass er eigene Tier- und Pflanzenarten beherbergt, die es nirgendwo sonst gibt."
    ),

    // Question 165
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt die Hagia Sophia?",
        answerA = "Griechenland",
        answerB = "Syrien",
        answerC = "Türkei",
        answerD = "Ägypten",
        correctAnswer = 2, // C
        explanation = "Die Hagia Sophia steht in Istanbul, Türkei. Sie wurde 537 n. Chr. als christliche Kirche erbaut, war später Moschee, dann Museum und ist seit 2020 wieder Moschee.",
        difficulty = 1,
        funFact = "Die Hagia Sophia war über tausend Jahre lang das größte Kirchengebäude der Welt, bis der Kölner Dom 1880 fertiggestellt wurde."
    ),

    // Question 166
    Question(
        categoryId = 1,
        questionText = "Welches Land liegt auf der Insel Sri Lanka direkt vor der Südspitze Indiens?",
        answerA = "Malediven",
        answerB = "Sri Lanka",
        answerC = "Lakshadweep",
        answerD = "Andamanen",
        correctAnswer = 1, // B
        explanation = "Sri Lanka ist eine Insel im Indischen Ozean, die durch die Palkstraße vom indischen Subkontinent getrennt ist. Die Hauptstadt ist Sri Jayawardenepura Kotte.",
        difficulty = 1,
        funFact = null
    ),

    // Question 167
    Question(
        categoryId = 1,
        questionText = "Welcher Fjord gilt als der längste Fjord der Welt?",
        answerA = "Hardangerfjord",
        answerB = "Geirangerfjord",
        answerC = "Scoresby-Sund",
        answerD = "Sognefjord",
        correctAnswer = 3, // D
        explanation = "Der Sognefjord in Norwegen ist mit über 200 km Länge der längste und tiefste Fjord Norwegens und der zweitlängste der Welt. Er liegt nördlich von Bergen.",
        difficulty = 1,
        funFact = "Der Sognefjord ist an einigen Stellen über 1.300 Meter tief – er ist tiefer als der Atlantische Ozean an vielen Stellen."
    ),

    // Question 168
    Question(
        categoryId = 1,
        questionText = "Welches ist die Hauptstadt der Dominikanischen Republik?",
        answerA = "Santiago de los Caballeros",
        answerB = "Santo Domingo",
        answerC = "Puerto Plata",
        answerD = "La Romana",
        correctAnswer = 1, // B
        explanation = "Santo Domingo ist die Hauptstadt der Dominikanischen Republik und war die erste dauerhaft besiedelte europäische Siedlung in Amerika, gegründet 1496.",
        difficulty = 1,
        funFact = "Die Altstadt von Santo Domingo ist UNESCO-Weltkulturerbe und enthält die älteste Kathedrale, das älteste Krankenhaus und die älteste Universität der Neuen Welt."
    ),

    // Question 169
    Question(
        categoryId = 1,
        questionText = "Welches Land in Asien ist bekannt als \"Land des Lächelns\"?",
        answerA = "Vietnam",
        answerB = "Myanmar",
        answerC = "Thailand",
        answerD = "Laos",
        correctAnswer = 2, // C
        explanation = "Thailand wird oft als \"Land des Lächelns\" bezeichnet, weil die Einheimischen für ihre freundliche und gastfreundliche Art bekannt sind. Das Land empfängt jährlich Millionen Touristen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 170
    Question(
        categoryId = 1,
        questionText = "Wo befindet sich der Uluru (Ayers Rock)?",
        answerA = "Neuseeland",
        answerB = "Australien",
        answerC = "Papua-Neuguinea",
        answerD = "Fidschi",
        correctAnswer = 1, // B
        explanation = "Der Uluru (früher Ayers Rock) ist ein riesiger Sandsteinfelsen im Roten Zentrum Australiens im Northern Territory. Er ist ein heiliger Ort für die Anangu-Aborigines.",
        difficulty = 1,
        funFact = "Der Uluru ist der größte Monolith der Welt – er ragt 348 Meter aus dem Boden, aber der sichtbare Teil ist nur ein kleiner Anteil: das Gestein erstreckt sich noch 2,5 km tief in die Erde."
    ),

    // Question 171
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Honduras?",
        answerA = "San Pedro Sula",
        answerB = "La Ceiba",
        answerC = "Tegucigalpa",
        answerD = "Choluteca",
        correctAnswer = 2, // C
        explanation = "Tegucigalpa ist die Hauptstadt Honduras und liegt im bergigen Zentrum des mittelamerikanischen Landes. Der Name stammt aus der indigenen Nahua-Sprache.",
        difficulty = 1,
        funFact = null
    ),

    // Question 172
    Question(
        categoryId = 1,
        questionText = "Welcher See liegt an der Grenze zwischen Peru und Bolivien?",
        answerA = "Maracaibo-See",
        answerB = "Titicacasee",
        answerC = "Poopósee",
        answerD = "Nicaraguasee",
        correctAnswer = 1, // B
        explanation = "Der Titicacasee liegt an der Grenze zwischen Peru und Bolivien in den Anden auf 3.812 Metern Höhe. Er gilt als der höchstgelegene schiffbare See der Welt.",
        difficulty = 1,
        funFact = "Auf dem Titicacasee leben Menschen auf schwimmenden Inseln aus Totora-Schilfrohr – die Uros-Inseln werden seit Jahrhunderten bewohnt und regelmäßig neu aufgeschichtet."
    ),

    // Question 173
    Question(
        categoryId = 1,
        questionText = "Welche Brücke verbindet Europa und Asien in Istanbul?",
        answerA = "Fatih-Sultan-Mehmet-Brücke",
        answerB = "Yavuz-Sultan-Selim-Brücke",
        answerC = "Bosporus-Brücke",
        answerD = "Galata-Brücke",
        correctAnswer = 2, // C
        explanation = "Die Bosporus-Brücke (offiziell: 15 Temmuz Şehitler Köprüsü) in Istanbul verbindet den europäischen mit dem asiatischen Teil der Stadt. Sie wurde 1973 eröffnet.",
        difficulty = 1,
        funFact = null
    ),

    // Question 174
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Tempel Borobudur?",
        answerA = "Kambodscha",
        answerB = "Myanmar",
        answerC = "Thailand",
        answerD = "Indonesien",
        correctAnswer = 3, // D
        explanation = "Borobudur liegt auf der Insel Java in Indonesien. Es ist der größte buddhistische Tempel der Welt und wurde im 8. und 9. Jahrhundert erbaut.",
        difficulty = 1,
        funFact = "Borobudur wurde nach seiner Errichtung jahrhundertelang vergessen und im 19. Jahrhundert von Sir Thomas Raffles wiederentdeckt, als der Wald um ihn herum gerodet wurde."
    ),

    // Question 175
    Question(
        categoryId = 1,
        questionText = "Welcher skandinavische Fjord ist UNESCO-Weltnaturerbe?",
        answerA = "Oslofjord",
        answerB = "Hardangerfjord",
        answerC = "Geirangerfjord",
        answerD = "Lysefjord",
        correctAnswer = 2, // C
        explanation = "Der Geirangerfjord in Norwegen ist seit 2005 UNESCO-Weltnaturerbe. Er ist bekannt für seine spektakulären Wasserfälle und die atemberaubende Landschaft.",
        difficulty = 1,
        funFact = "Die Wasserfälle am Geirangerfjord tragen poetische Namen wie \"Sieben Schwestern\" und \"Der Freier\" – laut Legende wirft der Freier seiner Liebsten eine Flasche zu."
    ),

    // Question 176
    Question(
        categoryId = 1,
        questionText = "Welches ist das kleinste Land Mittelamerikas?",
        answerA = "Costa Rica",
        answerB = "Honduras",
        answerC = "El Salvador",
        answerD = "Belize",
        correctAnswer = 2, // C
        explanation = "El Salvador ist das kleinste Land Mittelamerikas mit einer Fläche von nur rund 21.000 km². Es ist auch das einzige mittelamerikanische Land ohne Küste zum Karibischen Meer.",
        difficulty = 1,
        funFact = null
    ),

    // Question 177
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Vulkan Popocatépetl?",
        answerA = "Guatemala",
        answerB = "Costa Rica",
        answerC = "Mexiko",
        answerD = "Kolumbien",
        correctAnswer = 2, // C
        explanation = "Der Popocatépetl liegt in Mexiko, südöstlich von Mexiko-Stadt. Er ist 5.426 Meter hoch und einer der aktivsten Vulkane Nordamerikas.",
        difficulty = 1,
        funFact = "Der Name Popocatépetl kommt aus der Nahuatl-Sprache und bedeutet \"rauchender Berg\" – ein passender Name für den oft dampfenden Vulkan."
    ),

    // Question 178
    Question(
        categoryId = 1,
        questionText = "Welches ist das Wahrzeichen von Sydney, Australien?",
        answerA = "Harbour Bridge",
        answerB = "Opera House",
        answerC = "Bondi Beach",
        answerD = "Tower of Sydney",
        correctAnswer = 1, // B
        explanation = "Das Sydney Opera House ist das bekannteste Wahrzeichen Australiens und seit 2007 UNESCO-Weltkulturerbe. Die charakteristischen Schalenstrukturen wurden vom dänischen Architekten Jørn Utzon entworfen.",
        difficulty = 1,
        funFact = "Das Sydney Opera House wurde 1973 eingeweiht, obwohl die Planung 1957 begann. Die Kosten stiegen von 7 Millionen auf 102 Millionen australische Dollar."
    ),

    // Question 179
    Question(
        categoryId = 1,
        questionText = "Welches Land liegt vollständig innerhalb Südafrikas?",
        answerA = "Simbabwe",
        answerB = "Botswana",
        answerC = "Lesotho",
        answerD = "Eswatini",
        correctAnswer = 2, // C
        explanation = "Lesotho ist ein Binnenstaat, der vollständig von Südafrika umgeben ist. Es ist eines von nur drei Ländern der Welt, die vollständig von einem anderen Land umschlossen sind.",
        difficulty = 1,
        funFact = null
    ),

    // Question 180
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Myanmar (Burma)?",
        answerA = "Yangon",
        answerB = "Mandalay",
        answerC = "Naypyidaw",
        answerD = "Bagan",
        correctAnswer = 2, // C
        explanation = "Naypyidaw ist die Hauptstadt Myanmars. Sie wurde 2006 eigens als neue Hauptstadt errichtet und ersetzte Yangon (Rangun), das die frühere Hauptstadt war.",
        difficulty = 1,
        funFact = "Naypyidaw ist eine der ungewöhnlichsten Hauptstädte der Welt: Sie hat breite Straßen für 20 Spuren, aber kaum Verkehr, riesige Regierungsgebäude und fast keine Einwohner außerhalb der Beamten."
    ),

    // Question 181
    Question(
        categoryId = 1,
        questionText = "Welches Meer liegt zwischen Schweden und Finnland?",
        answerA = "Nordsee",
        answerB = "Ostsee",
        answerC = "Bottnischer Meerbusen",
        answerD = "Barents-See",
        correctAnswer = 2, // C
        explanation = "Der Bottnische Meerbusen ist der nördlichste Teil der Ostsee und liegt zwischen Schweden und Finnland. Er friert im Winter oft vollständig zu.",
        difficulty = 1,
        funFact = "Der Bottnische Meerbusen ist so flach und salzarm, dass er im Winter regelmäßig zufriert – früher nutzten die Einheimischen das Eis als Straße."
    ),

    // Question 182
    Question(
        categoryId = 1,
        questionText = "Welche Insel in der Karibik teilen sich zwei Länder?",
        answerA = "Kuba",
        answerB = "Jamaika",
        answerC = "Hispaniola",
        answerD = "Puerto Rico",
        correctAnswer = 2, // C
        explanation = "Die Insel Hispaniola ist zwischen Haiti (westlicher Teil) und der Dominikanischen Republik (östlicher Teil) aufgeteilt. Es ist eine der größten Inseln der Karibik.",
        difficulty = 1,
        funFact = null
    ),

    // Question 183
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Kambodscha?",
        answerA = "Siem Reap",
        answerB = "Battambang",
        answerC = "Phnom Penh",
        answerD = "Kompong Cham",
        correctAnswer = 2, // C
        explanation = "Phnom Penh ist die Hauptstadt und größte Stadt Kambodschas. Sie liegt am Zusammenfluss von Mekong und Tonlé Sap und ist das politische und wirtschaftliche Zentrum des Landes.",
        difficulty = 1,
        funFact = "Phnom Penh bedeutet auf Khmer \"Hügel von Penh\" – benannt nach einer Frau namens Penh, die laut Legende hier Buddhafiguren fand und einen Hügel aufschüttete."
    ),

    // Question 184
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der Tanganjikasee?",
        answerA = "Nur in Tansania",
        answerB = "In Tansania, DR Kongo, Burundi und Sambia",
        answerC = "Nur im Kongo",
        answerD = "In Kenia und Uganda",
        correctAnswer = 1, // B
        explanation = "Der Tanganjikasee liegt an den Grenzen von Tansania, der Demokratischen Republik Kongo, Burundi und Sambia. Er ist der zweittiefste See der Welt nach dem Baikalsee.",
        difficulty = 1,
        funFact = "Der Tanganjikasee ist mit 673 km der längste See der Welt – er erstreckt sich wie ein schmaler Streifen durch das Ostafrikanische Grabenbruch-System."
    ),

    // Question 185
    Question(
        categoryId = 1,
        questionText = "Welche Brücke ist die längste Hängebrücke der Welt?",
        answerA = "Akashi-Kaikyō-Brücke",
        answerB = "Golden Gate Bridge",
        answerC = "Humber Bridge",
        answerD = "Tsing-Ma-Brücke",
        correctAnswer = 0, // A
        explanation = "Die Akashi-Kaikyō-Brücke in Japan verbindet Kobe mit der Insel Awaji und hat eine Hauptspannweite von 1.991 Metern – sie ist die längste Hängebrücke der Welt.",
        difficulty = 1,
        funFact = "Die Akashi-Kaikyō-Brücke wurde erdbebensicher gebaut – sie kann Erdbeben der Stärke 8,5 und Windgeschwindigkeiten bis zu 290 km/h standhalten."
    ),

    // Question 186
    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land der Welt, das nach einem Kontinent benannt ist?",
        answerA = "China",
        answerB = "Australien",
        answerC = "Brasilien",
        answerD = "Indien",
        correctAnswer = 1, // B
        explanation = "Australien ist das einzige Land der Welt, das einen ganzen Kontinent allein ausfüllt und auch nach diesem benannt ist. Der Kontinent und das Land tragen denselben Namen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 187
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Laos?",
        answerA = "Luang Prabang",
        answerB = "Vientiane",
        answerC = "Pakse",
        answerD = "Savannakhet",
        correctAnswer = 1, // B
        explanation = "Vientiane ist die Hauptstadt und größte Stadt von Laos. Sie liegt am Mekong-Fluss an der Grenze zu Thailand und ist eine der kleinsten Hauptstädte Asiens.",
        difficulty = 1,
        funFact = "Vientiane hat einen Triumphbogen namens Patuxai, der dem Pariser Arc de Triomphe ähnelt – er wurde mit US-amerikanischem Zement gebaut, der eigentlich für einen Flughafen bestimmt war."
    ),

    // Question 188
    Question(
        categoryId = 1,
        questionText = "Welches UNESCO-Weltkulturerbe liegt in Griechenland?",
        answerA = "Parthenon auf der Akropolis",
        answerB = "Kolosseum",
        answerC = "Sagrada Família",
        answerD = "Tower of London",
        correctAnswer = 0, // A
        explanation = "Der Parthenon auf der Athener Akropolis ist Teil des UNESCO-Weltkulturerbes Griechenlands. Er wurde im 5. Jahrhundert v. Chr. gebaut und ist das bekannteste Symbol der griechischen Antike.",
        difficulty = 1,
        funFact = null
    ),

    // Question 189
    Question(
        categoryId = 1,
        questionText = "Welches ist das größte Land der Arabischen Halbinsel?",
        answerA = "Jemen",
        answerB = "Oman",
        answerC = "Kuwait",
        answerD = "Saudi-Arabien",
        correctAnswer = 3, // D
        explanation = "Saudi-Arabien ist mit über 2,1 Millionen km² das größte Land der Arabischen Halbinsel und das dreizehntgrößte der Welt. Es umfasst etwa 80 % der Halbinsel.",
        difficulty = 1,
        funFact = "Saudi-Arabien hat keinen permanenten Fluss – alle Wasserläufe sind temporäre Wadis, die nur nach Regenfällen Wasser führen."
    ),

    // Question 190
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Nepal?",
        answerA = "Pokhara",
        answerB = "Bhaktapur",
        answerC = "Kathmandu",
        answerD = "Biratnagar",
        correctAnswer = 2, // C
        explanation = "Kathmandu ist die Hauptstadt Nepals und liegt im Kathmandu-Tal auf etwa 1.400 Metern Höhe. Das Kathmandu-Tal mit seinen vielen Tempeln und Stätten ist UNESCO-Welterbe.",
        difficulty = 1,
        funFact = "Kathmandu liegt am Fuß des Himalaya und ist der Ausgangspunkt für viele Expeditionen zum Mount Everest, der nur ca. 140 km entfernt liegt."
    ),

    // Question 191
    Question(
        categoryId = 1,
        questionText = "Welcher See ist der größte in Südamerika?",
        answerA = "Titicacasee",
        answerB = "Maracaibo-See",
        answerC = "Poopósee",
        answerD = "Argentinischer See",
        correctAnswer = 1, // B
        explanation = "Der Maracaibo-See in Venezuela ist mit rund 13.000 km² der größte See Südamerikas. Er ist eigentlich eine Meeresbucht mit Verbindung zum Golf von Venezuela.",
        difficulty = 1,
        funFact = null
    ),

    // Question 192
    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Südostasiens?",
        answerA = "Vietnam",
        answerB = "Thailand",
        answerC = "Philippinen",
        answerD = "Indonesien",
        correctAnswer = 3, // D
        explanation = "Indonesien ist mit über 270 Millionen Einwohnern das bevölkerungsreichste Land Südostasiens und das viertbevölkerungsreichste Land der Welt.",
        difficulty = 1,
        funFact = "Indonesien besteht aus über 17.500 Inseln und ist damit das Land mit den meisten Inseln der Welt. Nur etwa 6.000 dieser Inseln sind bewohnt."
    ),

    // Question 193
    Question(
        categoryId = 1,
        questionText = "Welche Hauptstadt liegt am höchsten über dem Meeresspiegel?",
        answerA = "Quito",
        answerB = "Kathmandu",
        answerC = "Bogotá",
        answerD = "La Paz",
        correctAnswer = 3, // D
        explanation = "La Paz in Bolivien ist mit rund 3.650 Metern die höchstgelegene Regierungshauptstadt der Welt. (Sucre ist die Verfassungshauptstadt, La Paz die Regierungshauptstadt.)",
        difficulty = 1,
        funFact = "In La Paz kocht Wasser bereits bei 87°C statt bei 100°C, weil der Luftdruck in dieser Höhe so niedrig ist – Kochen dauert dort länger als im Flachland."
    ),

    // Question 194
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt das Schloss Neuschwanstein?",
        answerA = "Österreich",
        answerB = "Schweiz",
        answerC = "Deutschland",
        answerD = "Liechtenstein",
        correctAnswer = 2, // C
        explanation = "Das Schloss Neuschwanstein liegt in Bayern, Deutschland. Es wurde im 19. Jahrhundert von König Ludwig II. erbaut und gilt als Inspiration für das Cinderella-Schloss in Disney-Parks.",
        difficulty = 1,
        funFact = null
    ),

    // Question 195
    Question(
        categoryId = 1,
        questionText = "Welcher Vulkan brach 1980 in den USA aus und ist seither berühmt?",
        answerA = "Mount Rainier",
        answerB = "Mount Shasta",
        answerC = "Mount St. Helens",
        answerD = "Mount Hood",
        correctAnswer = 2, // C
        explanation = "Der Mount St. Helens im US-Bundesstaat Washington brach am 18. Mai 1980 aus. Der Ausbruch war der verheerendste in der Geschichte der USA und kostete 57 Menschen das Leben.",
        difficulty = 1,
        funFact = "Beim Ausbruch des Mount St. Helens im Jahr 1980 kollabierte die Nordseite des Berges, und der Gipfel sank von 2.950 auf 2.549 Meter – der Berg verlor buchstäblich seinen Kopf."
    ),

    // Question 196
    Question(
        categoryId = 1,
        questionText = "Was ist die Hauptstadt von Mongolei?",
        answerA = "Erdenet",
        answerB = "Darhan",
        answerC = "Ulaanbaatar",
        answerD = "Choibalsan",
        correctAnswer = 2, // C
        explanation = "Ulaanbaatar ist die Hauptstadt der Mongolei und mit rund 1,5 Millionen Einwohnern Heimat fast der Hälfte der gesamten mongolischen Bevölkerung.",
        difficulty = 1,
        funFact = "Ulaanbaatar ist die kälteste Hauptstadt der Welt mit einer Jahresdurchschnittstemperatur von minus 1,3 Grad Celsius."
    ),

    // Question 197
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt die alte Inka-Stadt Cusco?",
        answerA = "Bolivien",
        answerB = "Ecuador",
        answerC = "Peru",
        answerD = "Chile",
        correctAnswer = 2, // C
        explanation = "Cusco liegt in den Anden Perus auf etwa 3.400 Metern Höhe. Die Stadt war die Hauptstadt des Inka-Reichs und ist heute UNESCO-Weltkulturerbe.",
        difficulty = 1,
        funFact = null
    ),

    // Question 198
    Question(
        categoryId = 1,
        questionText = "Welches ist das einzige Land Südostasiens, das nie kolonialisiert wurde?",
        answerA = "Vietnam",
        answerB = "Malaysia",
        answerC = "Thailand",
        answerD = "Singapur",
        correctAnswer = 2, // C
        explanation = "Thailand (damals Siam) ist das einzige Land Südostasiens, das nie eine europäische Kolonie war. Geschickte Diplomatie ermöglichte dem Land seine Unabhängigkeit.",
        difficulty = 1,
        funFact = "\"Thailand\" bedeutet wörtlich \"Land der Freien\" – der Name ist ein Hinweis auf die historische Unabhängigkeit des Landes."
    ),

    // Question 199
    Question(
        categoryId = 1,
        questionText = "Welche Hauptstadt liegt auf zwei Kontinenten?",
        answerA = "Athen",
        answerB = "Istanbul",
        answerC = "Kairo",
        answerD = "Moskau",
        correctAnswer = 1, // B
        explanation = "Istanbul liegt auf zwei Kontinenten: Ein Teil der Stadt befindet sich in Europa, der andere in Asien. Der Bosporus trennt die beiden Teile der Stadt.",
        difficulty = 1,
        funFact = "Istanbul war unter verschiedenen Namen die Hauptstadt von drei Weltreichen: Konstantinopel (Byzantinisches Reich), Konstantinopel (Lateinisches Kaiserreich) und Istanbul (Osmanisches Reich)."
    ),

    // Question 200
    Question(
        categoryId = 1,
        questionText = "Welches ist das bevölkerungsreichste Land Afrikas nördlich der Sahara?",
        answerA = "Tunesien",
        answerB = "Libyen",
        answerC = "Marokko",
        answerD = "Ägypten",
        correctAnswer = 3, // D
        explanation = "Ägypten ist mit über 100 Millionen Einwohnern das bevölkerungsreichste Land Nordafrikas und das zweitbevölkerungsreichste auf dem gesamten afrikanischen Kontinent.",
        difficulty = 1,
        funFact = null
    ),

    // Question 201
    Question(
        categoryId = 1,
        questionText = "Welches ist das zweitkleinste Land der Welt (nach der Vatikanstadt)?",
        answerA = "Liechtenstein",
        answerB = "San Marino",
        answerC = "Monaco",
        answerD = "Nauru",
        correctAnswer = 2, // C
        explanation = "Monaco ist mit rund 2,02 km² das zweitkleinste Land der Welt. Es liegt an der französischen Riviera und ist bekannt für seine Casinos und den Formel-1-Grand-Prix.",
        difficulty = 1,
        funFact = "Monaco ist das bevölkerungsdichteste Land der Welt – auf einem Quadratkilometer leben dort mehr als 19.000 Menschen."
    ),

    // Question 202
    Question(
        categoryId = 1,
        questionText = "Welches winzige Land liegt vollständig innerhalb von Italien?",
        answerA = "Vatikanstadt",
        answerB = "Monaco",
        answerC = "Liechtenstein",
        answerD = "San Marino",
        correctAnswer = 3, // D
        explanation = "San Marino ist ein kleiner Staat, der vollständig von Italien umgeben ist. Es ist die älteste Republik der Welt und liegt auf dem Berg Titano.",
        difficulty = 1,
        funFact = null
    ),

    // Question 203
    Question(
        categoryId = 1,
        questionText = "Welcher Wasserfall gilt als der höchste der Welt?",
        answerA = "Iguazú-Fälle",
        answerB = "Niagarafälle",
        answerC = "Victoria-Fälle",
        answerD = "Angel-Fall",
        correctAnswer = 3, // D
        explanation = "Der Angel-Fall (Salto Ángel) in Venezuela ist mit einer Fallhöhe von 979 Metern der höchste Wasserfall der Welt. Er wurde 1933 vom Piloten Jimmy Angel entdeckt.",
        difficulty = 1,
        funFact = "Der Angel-Fall ist so hoch, dass das Wasser beim Fallen zu Nebel und Dunst zerstäubt, bevor es den Boden erreicht."
    ),

    // Question 204
    Question(
        categoryId = 1,
        questionText = "An welcher Grenze liegen die berühmten Victoriafälle?",
        answerA = "Südafrika und Mosambik",
        answerB = "Sambia und Simbabwe",
        answerC = "Kenia und Tansania",
        answerD = "Namibia und Botswana",
        correctAnswer = 1, // B
        explanation = "Die Victoriafälle liegen an der Grenze zwischen Sambia und Simbabwe am Sambesi-Fluss. Sie sind einer der größten Wasserfälle der Welt nach Breite.",
        difficulty = 1,
        funFact = "Der Name der Victoriafälle in der einheimischen Sprache Tonga lautet \"Mosi-oa-Tunya\", was \"Rauch, der donnert\" bedeutet – gut erkennbar am aufsteigenden Nebel."
    ),

    // Question 205
    Question(
        categoryId = 1,
        questionText = "Welche Stadt ist die heilige Stadt des Islam?",
        answerA = "Medina",
        answerB = "Jerusalem",
        answerC = "Mekka",
        answerD = "Riad",
        correctAnswer = 2, // C
        explanation = "Mekka in Saudi-Arabien ist die heiligste Stadt des Islams. Hier wurde der Prophet Mohammed geboren, und hier steht die Große Moschee mit der Kaaba – dem Ziel aller Pilger.",
        difficulty = 1,
        funFact = "Jedes Jahr pilgern bis zu 2,5 Millionen Muslime nach Mekka – die Hadsch ist eine der fünf Säulen des Islams."
    ),

    // Question 206
    Question(
        categoryId = 1,
        questionText = "Welche Stadt gilt als heilige Stadt für Judentum, Christentum und Islam?",
        answerA = "Rom",
        answerB = "Mekka",
        answerC = "Medina",
        answerD = "Jerusalem",
        correctAnswer = 3, // D
        explanation = "Jerusalem ist für alle drei abrahamitischen Religionen heilig: für Juden (Tempelberg), Christen (Grabeskirche) und Muslime (Al-Aqsa-Moschee und Felsendom).",
        difficulty = 1,
        funFact = null
    ),

    // Question 207
    Question(
        categoryId = 1,
        questionText = "Welche Stadt ist die heilige Stadt des Hinduismus am Ganges?",
        answerA = "Mumbai",
        answerB = "Varanasi",
        answerC = "Delhi",
        answerD = "Kolkata",
        correctAnswer = 1, // B
        explanation = "Varanasi (auch Benares oder Kashi genannt) ist eine der heiligsten Städte des Hinduismus. Hindus glauben, dass wer dort stirbt, vom Kreislauf der Wiedergeburt befreit wird.",
        difficulty = 1,
        funFact = "Varanasi ist eine der ältesten dauerhaft bewohnten Städte der Welt – sie soll seit über 3.000 Jahren kontinuierlich besiedelt sein."
    ),

    // Question 208
    Question(
        categoryId = 1,
        questionText = "Wie hieß Mumbais früherer englischer Name?",
        answerA = "Calcutta",
        answerB = "Madras",
        answerC = "Bombay",
        answerD = "Cochin",
        correctAnswer = 2, // C
        explanation = "Mumbai hieß bis 1995 offiziell Bombay. Der Name wurde in Mumbai geändert, um den historischen Marathi-Namen der Stadt wiederherzustellen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 209
    Question(
        categoryId = 1,
        questionText = "Wie hieß die heutige Stadt Ho-Chi-Minh-Stadt früher?",
        answerA = "Hanoi",
        answerB = "Pnom Penh",
        answerC = "Saigon",
        answerD = "Da Nang",
        correctAnswer = 2, // C
        explanation = "Ho-Chi-Minh-Stadt hieß bis 1975 Saigon und war die Hauptstadt Südvietnams. Nach dem Ende des Vietnamkriegs wurde sie nach Ho Chi Minh, dem Gründer Nordvietnams, umbenannt.",
        difficulty = 1,
        funFact = "Obwohl die Stadt offiziell Ho-Chi-Minh-Stadt heißt, nennen viele Einheimische sie noch immer Saigon."
    ),

    // Question 210
    Question(
        categoryId = 1,
        questionText = "Welcher Flughafen gilt als einer der verkehrsreichsten der Welt?",
        answerA = "Heathrow (London)",
        answerB = "Charles de Gaulle (Paris)",
        answerC = "Frankfurt",
        answerD = "Hartsfield-Jackson (Atlanta)",
        correctAnswer = 3, // D
        explanation = "Der Hartsfield-Jackson Atlanta International Airport in Georgia, USA, ist regelmäßig der passagierstärkste Flughafen der Welt mit über 100 Millionen Passagieren pro Jahr.",
        difficulty = 1,
        funFact = "Atlanta ist zwar nicht die bekannteste US-Stadt, aber sein Flughafen ist so zentral in den USA gelegen, dass er als perfekter Umsteigeknoten gilt – daher die enorm hohe Passagierzahl."
    ),

    // Question 211
    Question(
        categoryId = 1,
        questionText = "In welcher Stadt befindet sich der Flughafen Heathrow?",
        answerA = "Manchester",
        answerB = "Edinburgh",
        answerC = "London",
        answerD = "Birmingham",
        correctAnswer = 2, // C
        explanation = "Heathrow liegt im Westen Londons und ist der verkehrsreichste Flughafen Europas sowie einer der meistgenutzten der Welt. Er hat fünf Terminals.",
        difficulty = 1,
        funFact = null
    ),

    // Question 212
    Question(
        categoryId = 1,
        questionText = "Welcher Hafen gilt als einer der wichtigsten Containerhäfen der Welt?",
        answerA = "Hafen von Rotterdam",
        answerB = "Hafen von Hamburg",
        answerC = "Hafen von New York",
        answerD = "Hafen von Shanghai",
        correctAnswer = 3, // D
        explanation = "Der Hafen von Shanghai ist seit 2010 der größte Containerhafen der Welt. Er schlägt jährlich über 47 Millionen Standardcontainer (TEU) um.",
        difficulty = 1,
        funFact = "Im Hafen von Shanghai werden pro Stunde mehr Container umgeschlagen als manche Häfen in einem ganzen Monat."
    ),

    // Question 213
    Question(
        categoryId = 1,
        questionText = "Welcher europäische Hafen ist der größte in Europa?",
        answerA = "Hamburg",
        answerB = "Antwerpen",
        answerC = "Rotterdam",
        answerD = "Barcelona",
        correctAnswer = 2, // C
        explanation = "Der Hafen von Rotterdam in den Niederlanden ist der größte Hafen Europas. Er liegt am Rheinmündungsgebiet und ist ein wichtiges Tor für den europäischen Handel.",
        difficulty = 1,
        funFact = "Rotterdam ist die Homebase für rund 400 Schifffahrtslinien und verbindet Europa mit über 1.000 Häfen weltweit."
    ),

    // Question 214
    Question(
        categoryId = 1,
        questionText = "Wie viele Ozeane gibt es auf der Erde?",
        answerA = "Drei",
        answerB = "Vier",
        answerC = "Fünf",
        answerD = "Sieben",
        correctAnswer = 2, // C
        explanation = "Die Erde hat offiziell fünf Ozeane: den Pazifischen, den Atlantischen, den Indischen, den Arktischen und den Südlichen (Antarktischen) Ozean. Der Südliche Ozean wurde 2000 offiziell anerkannt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 215
    Question(
        categoryId = 1,
        questionText = "Welcher Ozean ist der kleinste der Erde?",
        answerA = "Indischer Ozean",
        answerB = "Südlicher Ozean",
        answerC = "Pazifischer Ozean",
        answerD = "Arktischer Ozean",
        correctAnswer = 3, // D
        explanation = "Der Arktische Ozean (Nordpolarmeer) ist der kleinste Ozean der Erde mit einer Fläche von rund 14,06 Millionen km². Er liegt am Nordpol und ist zeitweise von Eis bedeckt.",
        difficulty = 1,
        funFact = "Im Sommer schmilzt etwa die Hälfte des arktischen Meereises – seit 1980 hat die Eisfläche im Sommer um fast 40 % abgenommen."
    ),

    // Question 216
    Question(
        categoryId = 1,
        questionText = "Welches Salzwasser ist salziger als normale Meere – das Tote Meer oder das Rote Meer?",
        answerA = "Rotes Meer",
        answerB = "Beide gleich salzig",
        answerC = "Totes Meer",
        answerD = "Weder noch – beide sind Süßwasser",
        correctAnswer = 2, // C
        explanation = "Das Tote Meer hat einen Salzgehalt von rund 34 %, während normale Ozeane etwa 3,5 % haben. Es ist damit rund zehn Mal salziger als normales Meerwasser.",
        difficulty = 1,
        funFact = "Im Toten Meer kann man nicht sinken – der hohe Salzgehalt macht das Wasser so dicht, dass Menschen mühelos an der Oberfläche treiben."
    ),

    // Question 217
    Question(
        categoryId = 1,
        questionText = "Auf welchem Breitengrad liegt der Äquator?",
        answerA = "23,5° Nord",
        answerB = "0°",
        answerC = "90° Nord",
        answerD = "45° Süd",
        correctAnswer = 1, // B
        explanation = "Der Äquator liegt auf dem Breitengrad 0° und teilt die Erde in die nördliche und südliche Hemisphäre. Er hat eine Länge von rund 40.075 km.",
        difficulty = 1,
        funFact = null
    ),

    // Question 218
    Question(
        categoryId = 1,
        questionText = "Was verläuft bei 0° Längengrad durch London?",
        answerA = "Wendekreis des Krebses",
        answerB = "Äquator",
        answerC = "Nullmeridian",
        answerD = "Polarkreis",
        correctAnswer = 2, // C
        explanation = "Der Nullmeridian (0° Längengrad) verläuft durch das Königliche Observatorium in Greenwich, London. Er wurde 1884 als internationale Referenz festgelegt.",
        difficulty = 1,
        funFact = "Am Nullmeridian in Greenwich kann man buchstäblich mit einem Fuß auf der westlichen und mit dem anderen auf der östlichen Hemisphäre stehen."
    ),

    // Question 219
    Question(
        categoryId = 1,
        questionText = "Welche UNESCO-Welterbestadt liegt in der Türkei?",
        answerA = "Ephesos",
        answerB = "Pompeji",
        answerC = "Petra",
        answerD = "Angkor Wat",
        correctAnswer = 0, // A
        explanation = "Ephesos ist eine antike Stadt in der heutigen Türkei und UNESCO-Weltkulturerbe. Es war einst eine der größten Städte der antiken Welt und beherbergt den Tempel der Artemis.",
        difficulty = 1,
        funFact = "Die Bibliothek des Celsus in Ephesos gehört zu den am besten erhaltenen Fassaden der Antike und ist bis heute gut erkennbar."
    ),

    // Question 220
    Question(
        categoryId = 1,
        questionText = "Welche Stadt ist UNESCO-Welterbe und liegt in der Altstadt von Dubrovnik?",
        answerA = "Zagreb",
        answerB = "Split",
        answerC = "Dubrovnik",
        answerD = "Zadar",
        correctAnswer = 2, // C
        explanation = "Die Altstadt von Dubrovnik in Kroatien ist seit 1979 UNESCO-Weltkulturerbe. Die mittelalterlichen Stadtmauern und Gassen sind weltberühmt – Dubrovnik dient auch als Drehort für Game of Thrones.",
        difficulty = 1,
        funFact = null
    ),

    // Question 221
    Question(
        categoryId = 1,
        questionText = "Welches winzige Fürstentum liegt zwischen der Schweiz und Österreich?",
        answerA = "Monaco",
        answerB = "San Marino",
        answerC = "Liechtenstein",
        answerD = "Andorra",
        correctAnswer = 2, // C
        explanation = "Liechtenstein liegt zwischen der Schweiz und Österreich. Es ist eines der kleinsten Länder der Welt und ist doppelt eingeschlossen – keines seiner Nachbarländer hat Meeresküste.",
        difficulty = 1,
        funFact = "Liechtenstein und Usbekistan sind die einzigen zwei Länder der Welt, die von ausschließlich anderen Binnenstaaten umgeben sind – beide haben keinen Meereszugang auf direktem Weg."
    ),

    // Question 222
    Question(
        categoryId = 1,
        questionText = "Welches ist das kleinste Land auf dem amerikanischen Kontinent?",
        answerA = "El Salvador",
        answerB = "Belize",
        answerC = "St. Kitts und Nevis",
        answerD = "Barbados",
        correctAnswer = 2, // C
        explanation = "St. Kitts und Nevis ist mit einer Fläche von nur 261 km² das kleinste Land in Amerika (Nord- und Südamerika). Es ist ein Inselstaat in der Karibik.",
        difficulty = 1,
        funFact = "St. Kitts und Nevis hat eine sehr kleine Bevölkerung von rund 55.000 Menschen und ist einer der kleinsten Staaten der Welt nach Bevölkerung."
    ),

    // Question 223
    Question(
        categoryId = 1,
        questionText = "Was ist der tiefste Punkt auf dem Festland (Landmasse)?",
        answerA = "Totes Meer",
        answerB = "Kaspisches Meer",
        answerC = "Marianengraben",
        answerD = "Assal-See",
        correctAnswer = 0, // A
        explanation = "Das Tote Meer an der Grenze von Israel und Jordanien ist der tiefste Punkt auf der Erdoberfläche, der nicht von Ozean bedeckt ist. Es liegt rund 430 Meter unter dem Meeresspiegel.",
        difficulty = 1,
        funFact = "Das Tote Meer schrumpft jedes Jahr um etwa einen Meter – der Jordan-Fluss, der es speist, wird zu stark für Landwirtschaft und Industrie abgezweigt."
    ),

    // Question 224
    Question(
        categoryId = 1,
        questionText = "Welcher Flughafen in Asien ist für sein ausgezeichnetes Design und seinen Service bekannt?",
        answerA = "Incheon (Seoul)",
        answerB = "Narita (Tokio)",
        answerC = "Changi (Singapur)",
        answerD = "Suvarnabhumi (Bangkok)",
        correctAnswer = 2, // C
        explanation = "Der Changi Airport in Singapur wurde wiederholt zum besten Flughafen der Welt gewählt. Er bietet einen Swimming Pool, Gärten und sogar ein Schmetterlingsgarten für Passagiere.",
        difficulty = 1,
        funFact = null
    ),

    // Question 225
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt der berühmte Hafen von Sydney?",
        answerA = "Neuseeland",
        answerB = "Australien",
        answerC = "Papua-Neuguinea",
        answerD = "Fidschi",
        correctAnswer = 1, // B
        explanation = "Der Hafen von Sydney in Australien ist einer der schönsten Naturhäfen der Welt. Am Ufer stehen das Sydney Opera House und die Sydney Harbour Bridge.",
        difficulty = 1,
        funFact = "Der Hafen von Sydney ist einer der tiefsten Naturhäfen der Welt und kann gleichzeitig Hunderte von großen Schiffen aufnehmen."
    ),

    // Question 226
    Question(
        categoryId = 1,
        questionText = "Welche Stadt wird als \"Tor zur Seidenstraße\" bezeichnet?",
        answerA = "Teheran",
        answerB = "Kabul",
        answerC = "Samarkand",
        answerD = "Taschkent",
        correctAnswer = 2, // C
        explanation = "Samarkand in Usbekistan war einer der wichtigsten Knotenpunkte der antiken Seidenstraße. Die Stadt mit ihren blauen Kuppeln und Moscheen ist heute UNESCO-Weltkulturerbe.",
        difficulty = 1,
        funFact = "Samarkand war einmal die Hauptstadt des Timuridenreichs unter Timur (Tamerlan) und galt als eine der prächtigsten Städte der Welt."
    ),

    // Question 227
    Question(
        categoryId = 1,
        questionText = "Auf welchem Breitengrad liegt der nördliche Polarkreis?",
        answerA = "60° Nord",
        answerB = "66,5° Nord",
        answerC = "90° Nord",
        answerD = "45° Nord",
        correctAnswer = 1, // B
        explanation = "Der nördliche Polarkreis liegt bei 66,5° nördlicher Breite. Nördlich davon gibt es Tage im Jahr, an denen die Sonne nicht auf- oder untergeht (Polartag und Polarnacht).",
        difficulty = 1,
        funFact = null
    ),

    // Question 228
    Question(
        categoryId = 1,
        questionText = "Welche Stadt in Kanada ist für ihren weltberühmten Hafen und die schöne Bergkulisse bekannt?",
        answerA = "Toronto",
        answerB = "Montreal",
        answerC = "Vancouver",
        answerD = "Calgary",
        correctAnswer = 2, // C
        explanation = "Vancouver liegt an der Westküste Kanadas und ist für seinen Hafen und die Kulisse aus Meer und schneebedeckten Bergen bekannt. Es ist eine der schönsten Städte der Welt.",
        difficulty = 1,
        funFact = "Vancouver wurde mehrfach zur lebenswertesten Stadt der Welt gewählt und ist beliebt für Outdoor-Aktivitäten wie Skifahren und Segeln – beides im selben Tag möglich."
    ),

    // Question 229
    Question(
        categoryId = 1,
        questionText = "Was ist der Unterschied zwischen Breiten- und Längengraden auf einer Weltkarte?",
        answerA = "Breitengrade verlaufen von Osten nach Westen, Längengrade von Norden nach Süden",
        answerB = "Beide verlaufen von Norden nach Süden",
        answerC = "Beide verlaufen von Osten nach Westen",
        answerD = "Es gibt keinen Unterschied",
        correctAnswer = 0, // A
        explanation = "Breitengrade (Parallelen) verlaufen horizontal, also von Osten nach Westen, und messen den Abstand vom Äquator. Längengrade (Meridiane) verlaufen vertikal, von Norden nach Süden, und messen den Abstand vom Nullmeridian.",
        difficulty = 1,
        funFact = "Breitengrad = Latitude (von lat. latus = breit), Längengrad = Longitude (von lat. longus = lang) – eine gute Gedächtnisstütze ist: Latitude = Leiter (horizontal)."
    ),

    // Question 230
    Question(
        categoryId = 1,
        questionText = "Welche historische Stadt in Marokko ist UNESCO-Weltkulturerbe und für ihre engen Gassen bekannt?",
        answerA = "Casablanca",
        answerB = "Marrakesch",
        answerC = "Fès",
        answerD = "Rabat",
        correctAnswer = 2, // C
        explanation = "Die Medina von Fès ist UNESCO-Weltkulturerbe und gilt als eine der am besten erhaltenen mittelalterlichen Städte der islamischen Welt. Ihre Gassen sind so eng, dass nur Esel durchpassen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 231
    Question(
        categoryId = 1,
        questionText = "Welche Stadt in Peru ist UNESCO-Weltkulturerbe und war einst die Hauptstadt des Inka-Reichs?",
        answerA = "Lima",
        answerB = "Arequipa",
        answerC = "Cusco",
        answerD = "Trujillo",
        correctAnswer = 2, // C
        explanation = "Cusco (auch Cuzco) war die Hauptstadt des Inka-Reichs und ist heute UNESCO-Weltkulturerbe. Die Stadt liegt in den Anden auf rund 3.400 Metern Höhe.",
        difficulty = 1,
        funFact = "Die Inka bauten ihre Steinmauern so präzise, dass zwischen den Blöcken kein Messer passt – auch ohne Mörtel stehen sie nach über 500 Jahren noch."
    ),

    // Question 232
    Question(
        categoryId = 1,
        questionText = "Welches Inselland im Pazifik ist so klein, dass es das drittkleinste Land der Welt ist?",
        answerA = "Tonga",
        answerB = "Nauru",
        answerC = "Tuvalu",
        answerD = "Samoa",
        correctAnswer = 1, // B
        explanation = "Nauru ist mit einer Fläche von nur 21 km² das drittkleinste Land der Welt (nach Vatikanstadt und Monaco). Es liegt im Pazifischen Ozean und ist ein unabhängiger Inselstaat.",
        difficulty = 1,
        funFact = null
    ),

    // Question 233
    Question(
        categoryId = 1,
        questionText = "Welcher Wasserfall an der Grenze zwischen USA und Kanada ist weltberühmt?",
        answerA = "Victoria-Fälle",
        answerB = "Angel-Fall",
        answerC = "Iguazú-Fälle",
        answerD = "Niagarafälle",
        correctAnswer = 3, // D
        explanation = "Die Niagarafälle liegen an der Grenze zwischen dem US-Bundesstaat New York und der kanadischen Provinz Ontario. Sie sind zwar nicht die höchsten, aber volumenmäßig zu den stärksten Wasserfällen der Welt.",
        difficulty = 1,
        funFact = "Die Niagarafälle wandern jedes Jahr um etwa 30 cm durch Erosion zurück – in den letzten 12.000 Jahren sind sie schon rund 11 km in den Fels eingefressen."
    ),

    // Question 234
    Question(
        categoryId = 1,
        questionText = "Wie lautet der frühere Name von Peking (Hauptstadt Chinas)?",
        answerA = "Shanghai",
        answerB = "Nanjing",
        answerC = "Kanton",
        answerD = "Nanking",
        correctAnswer = 1, // B
        explanation = "Peking (Běijīng) bedeutet auf Chinesisch 'Nördliche Hauptstadt'. Nanjing (Nanjīng, 'Südliche Hauptstadt') war früher die Hauptstadt Chinas. Zwischen 1912 und 1949 war Nanjing Hauptstadt der Republik China.",
        difficulty = 1,
        funFact = "Peking war unter dem Namen \"Dadu\" die Hauptstadt des Mongolischen Reiches unter Kublai Khan im 13. Jahrhundert."
    ),

    // Question 235
    Question(
        categoryId = 1,
        questionText = "Welche Stadt in Kasachstan wurde 2019 umbenannt und heißt nun Nur-Sultan?",
        answerA = "Almaty",
        answerB = "Karaganda",
        answerC = "Astana",
        answerD = "Shymkent",
        correctAnswer = 2, // C
        explanation = "Die kasachische Hauptstadt hieß Astana und wurde 2019 zu Ehren des langjährigen Präsidenten Nursultan Nasarbajew in Nur-Sultan umbenannt. 2022 wurde der Name wieder auf Astana zurückgeändert.",
        difficulty = 1,
        funFact = null
    ),

    // Question 236
    Question(
        categoryId = 1,
        questionText = "Was ist die tiefste Stelle des Atlantischen Ozeans?",
        answerA = "Puerto-Rico-Graben",
        answerB = "Marianengraben",
        answerC = "Romanchegraben",
        answerD = "Java-Graben",
        correctAnswer = 0, // A
        explanation = "Der Puerto-Rico-Graben ist die tiefste Stelle des Atlantischen Ozeans mit rund 8.380 Metern. Er liegt nördlich von Puerto Rico an der Grenze zwischen der Karibischen und der Nordamerikanischen Platte.",
        difficulty = 1,
        funFact = "Der Puerto-Rico-Graben ist auch seismisch aktiv – die Region ist erdbebengefährdet, was Auswirkungen auf Karibikinseln haben kann."
    ),

    // Question 237
    Question(
        categoryId = 1,
        questionText = "In welchem Land liegt die Altstadt von Bern, die UNESCO-Weltkulturerbe ist?",
        answerA = "Österreich",
        answerB = "Deutschland",
        answerC = "Schweiz",
        answerD = "Liechtenstein",
        correctAnswer = 2, // C
        explanation = "Die Altstadt von Bern, der Schweizer Bundesstadt, ist seit 1983 UNESCO-Weltkulturerbe. Sie ist bekannt für ihre mittelalterlichen Laubengänge und den Zytglogge-Turm.",
        difficulty = 1,
        funFact = "In Bern gibt es über 6 km überdachte Laubengänge (Arcaden) – das ist das größte zusammenhängende Laubengangsystem der Welt."
    ),

    // Question 238
    Question(
        categoryId = 1,
        questionText = "Welcher Wendekreis liegt nördlich des Äquators?",
        answerA = "Wendekreis des Steinbocks",
        answerB = "Südlicher Polarkreis",
        answerC = "Wendekreis des Krebses",
        answerD = "Nördlicher Polarkreis",
        correctAnswer = 2, // C
        explanation = "Der Wendekreis des Krebses (Tropic of Cancer) liegt bei 23,5° nördlicher Breite. Südlich des Äquators liegt der Wendekreis des Steinbocks bei 23,5° südlicher Breite.",
        difficulty = 1,
        funFact = null
    ),

    // Question 239
    Question(
        categoryId = 1,
        questionText = "Welches kleine karibische Inselland ist das zweijüngste Land der Welt (Unabhängigkeit 2010)?",
        answerA = "Trinidad und Tobago",
        answerB = "Barbados",
        answerC = "St. Lucia",
        answerD = "Haiti",
        correctAnswer = 3, // D
        explanation = "Haiti wurde 1804 als erstes schwarzes Freiland der Welt unabhängig – es war die zweite Unabhängigkeitserklärung auf dem amerikanischen Kontinent nach den USA. Das Erdbeben 2010 war verheerend.",
        difficulty = 1,
        funFact = "Haiti war die erste Sklavenrepublik der Welt, die ihre Unabhängigkeit durch eine erfolgreiche Sklavenrevolution erkämpfte – ein einzigartiges Ereignis in der Weltgeschichte."
    ),

    // Question 240
    Question(
        categoryId = 1,
        questionText = "Welche Stadt gilt als heilige Stadt für Buddhisten und war lange Residenz des Dalai Lama?",
        answerA = "Kathmandu",
        answerB = "Lhasa",
        answerC = "Thimphu",
        answerD = "Bangkok",
        correctAnswer = 1, // B
        explanation = "Lhasa in Tibet (China) gilt als heilige Stadt des tibetischen Buddhismus. Der Potala-Palast war jahrhundertelang die Residenz des Dalai Lama und ist heute UNESCO-Welterbe.",
        difficulty = 1,
        funFact = null
    ),

    // Question 241
    Question(
        categoryId = 1,
        questionText = "Welches Land hat die meisten UNESCO-Welterbestätten?",
        answerA = "Frankreich",
        answerB = "USA",
        answerC = "China",
        answerD = "Italien",
        correctAnswer = 3, // D
        explanation = "Italien hat die meisten UNESCO-Welterbestätten der Welt mit über 58 eingetragenen Stätten (Stand 2023). Dazu zählen der Vatikan, das Kolosseum, Pompeji und viele historische Stadtzentren.",
        difficulty = 1,
        funFact = "Italien hat pro Fläche mehr Welterbestätten als jedes andere große Land der Welt – das spiegelt den enormen historischen Reichtum der Halbinsel wider."
    ),

    // Question 242
    Question(
        categoryId = 1,
        questionText = "Welches Meer ist bekannt für sein glasklares Wasser und liegt zwischen Australien und Neuguinea?",
        answerA = "Korallenmeer",
        answerB = "Timorsee",
        answerC = "Arafurasee",
        answerD = "Salomonsee",
        correctAnswer = 0, // A
        explanation = "Das Korallenmeer liegt nordöstlich von Australien zwischen dem australischen Festland, Papua-Neuguinea und den Salomonen. Das Great Barrier Reef liegt in diesem Meeresgebiet.",
        difficulty = 1,
        funFact = "Im Korallenmeer fand im Mai 1942 eine der entscheidenden Seeschlachten des Zweiten Weltkriegs statt – die erste Seeschlacht der Geschichte, bei der sich die Schiffe nie direkt sahen."
    ),

    // Question 243
    Question(
        categoryId = 1,
        questionText = "Welche europäische Hauptstadt trägt den Spitznamen \"Stadt der Wasser\" und ist berühmt für ihre Kanäle?",
        answerA = "Amsterdam",
        answerB = "Venedig",
        answerC = "Brügge",
        answerD = "Stockholm",
        correctAnswer = 1, // B
        explanation = "Venedig in Italien ist weltberühmt für seine Kanäle, die als Straßen dienen, und für seine Gondelfahrten. Die historische Stadt liegt auf 118 kleinen Inseln in einer Lagune.",
        difficulty = 1,
        funFact = null
    ),

    // Question 244
    Question(
        categoryId = 1,
        questionText = "Was bezeichnet der Begriff \"Antipode\"?",
        answerA = "Den höchsten Punkt der Erde",
        answerB = "Den tiefsten Punkt des Ozeans",
        answerC = "Den gegenüberliegenden Punkt auf der Erde",
        answerD = "Die Mitte des Äquators",
        correctAnswer = 2, // C
        explanation = "Als Antipode bezeichnet man den Punkt, der dem eigenen Standort auf der Erdkugel genau gegenüberliegt. Wenn man durch die Erde stechen würde, käme man am Antipoden heraus.",
        difficulty = 1,
        funFact = "Die Antipode von Deutschland liegt mitten im Pazifischen Ozean – würde man durch die Erde graben, landete man im Wasser."
    ),

    // Question 245
    Question(
        categoryId = 1,
        questionText = "Welcher berühmte Hafen liegt in der griechischen Hauptstadt Athen?",
        answerA = "Korinth",
        answerB = "Piräus",
        answerC = "Thessaloniki",
        answerD = "Patras",
        correctAnswer = 1, // B
        explanation = "Piräus ist der Hafen von Athen und der größte Hafen Griechenlands. Er ist auch einer der größten Passagierhäfen der Welt und dient als Ausgangspunkt für Fähren zu den griechischen Inseln.",
        difficulty = 1,
        funFact = "Piräus war in der Antike der wichtigste Hafen des Mittelmeers und der Schlüssel zur Seemacht Athens."
    ),

    // Question 246
    Question(
        categoryId = 1,
        questionText = "Welche alte Handelsstadt in Jordanien ist UNESCO-Weltkulturerbe und in Fels gehauen?",
        answerA = "Jerash",
        answerB = "Aqaba",
        answerC = "Amman",
        answerD = "Petra",
        correctAnswer = 3, // D
        explanation = "Petra in Jordanien ist eine antike Stadt der Nabatäer, die vollständig in den roten Sandstein gehauen wurde. Sie ist seit 1985 UNESCO-Weltkulturerbe und eines der Neuen Weltwunder.",
        difficulty = 1,
        funFact = null
    ),

    // Question 247
    Question(
        categoryId = 1,
        questionText = "Welches Land hat eine Hauptstadt, die nach seiner Unabhängigkeit von Portugal 1975 gegründet wurde und früher \"Lourenço Marques\" hieß?",
        answerA = "Angola",
        answerB = "Mosambik",
        answerC = "Kap Verde",
        answerD = "Guinea-Bissau",
        correctAnswer = 1, // B
        explanation = "Maputo, die Hauptstadt Mosambiks, hieß zur Kolonialzeit Lourenço Marques. Nach der Unabhängigkeit von Portugal 1975 wurde sie in Maputo umbenannt.",
        difficulty = 1,
        funFact = "Mosambik hat eine der längsten Küstenlinien Afrikas – über 2.700 km Küste am Indischen Ozean – und ist bekannt für seine wunderschönen Strände."
    ),

    // Question 248
    Question(
        categoryId = 1,
        questionText = "Welche Stadt in Kolumbien ist als UNESCO-Weltkulturerbe bekannt und liegt an der Karibikküste?",
        answerA = "Medellín",
        answerB = "Barranquilla",
        answerC = "Cartagena",
        answerD = "Cali",
        correctAnswer = 2, // C
        explanation = "Cartagena de Indias an der kolumbianischen Karibikküste ist UNESCO-Weltkulturerbe. Die gut erhaltene Kolonialstadt mit ihren bunten Häusern und starken Befestigungsmauern gilt als eine der schönsten Städte Lateinamerikas.",
        difficulty = 1,
        funFact = "Die Stadtmauern von Cartagena sind über 11 km lang und wurden im 16. und 17. Jahrhundert zum Schutz vor Piraten gebaut."
    ),

    // Question 249
    Question(
        categoryId = 1,
        questionText = "Welcher asiatische Inselstaat ist flächenmäßig einer der kleinsten der Welt, aber wirtschaftlich sehr wohlhabend?",
        answerA = "Malediven",
        answerB = "Singapur",
        answerC = "Brunei",
        answerD = "Bahrain",
        correctAnswer = 1, // B
        explanation = "Singapur ist ein Stadtstaat in Südostasien mit einer Fläche von nur rund 725 km². Trotz seiner geringen Größe ist es einer der wohlhabendsten Staaten der Welt und ein wichtiges Finanzzentrum.",
        difficulty = 1,
        funFact = null
    ),

    // Question 250
    Question(
        categoryId = 1,
        questionText = "Welche Wüste liegt im südlichen Afrika und ist eine der ältesten der Welt?",
        answerA = "Sahara",
        answerB = "Kalahari",
        answerC = "Namib",
        answerD = "Karoo",
        correctAnswer = 2, // C
        explanation = "Die Namib in Namibia ist eine der ältesten Wüsten der Welt – sie soll seit mindestens 55 Millionen Jahren existieren. Ihre riesigen orangeroten Sanddünen sind weltberühmt.",
        difficulty = 1,
        funFact = "Die Dünen der Namib sind die höchsten der Welt – die Düne \"Big Daddy\" nahe Sossusvlei erreicht etwa 325 Meter Höhe."
    )
)
