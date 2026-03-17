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
)
