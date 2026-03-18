package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsEasy4(): List<Question> = listOf(

    // --- African History: Ancient Egypt ---

    Question(
        categoryId = 3,
        questionText = "Wie nannte man den Herrscher des alten Ägyptens?",
        answerA = "Kaiser",
        answerB = "Pharao",
        answerC = "Sultan",
        answerD = "Kalif",
        correctAnswer = 1,
        explanation = "Der Herrscher des alten Ägyptens wurde als Pharao bezeichnet. Er galt als lebende Verkörperung des Gottes Horus.",
        difficulty = 1,
        funFact = "Das Wort 'Pharao' stammt vom ägyptischen Begriff 'Per-aa', was 'Großes Haus' bedeutet."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Pharao ließ die bekannteste Pyramide von Gizeh bauen?",
        answerA = "Ramses II.",
        answerB = "Tutanchamun",
        answerC = "Cheops",
        answerD = "Amenhotep IV.",
        correctAnswer = 2,
        explanation = "Die Große Pyramide von Gizeh wurde um 2560 v. Chr. für den Pharao Cheops (auch Khufu genannt) errichtet.",
        difficulty = 1,
        funFact = "Die Große Pyramide war über 3.800 Jahre lang das höchste von Menschenhand geschaffene Bauwerk der Welt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Pharao war als Kinderkönig bekannt, dessen Grab 1922 entdeckt wurde?",
        answerA = "Nofretete",
        answerB = "Ramses II.",
        answerC = "Kleopatra",
        answerD = "Tutanchamun",
        correctAnswer = 3,
        explanation = "Tutanchamuns Grab wurde 1922 vom britischen Archäologen Howard Carter im Tal der Könige entdeckt.",
        difficulty = 1,
        funFact = "Tutanchamun bestieg im Alter von etwa neun Jahren den Thron und starb bereits mit etwa 19 Jahren."
    ),

    Question(
        categoryId = 3,
        questionText = "An welchem Fluss liegt das alte Ägypten?",
        answerA = "Tigris",
        answerB = "Euphrat",
        answerC = "Nil",
        answerD = "Kongo",
        correctAnswer = 2,
        explanation = "Das alte Ägypten entwickelte sich entlang des Nils. Die jährlichen Überschwemmungen des Nils machten das Land fruchtbar.",
        difficulty = 1,
        funFact = "Der Nil ist mit etwa 6.650 Kilometern der längste Fluss der Welt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche ägyptische Schrift verwendete Bilder als Zeichen?",
        answerA = "Keilschrift",
        answerB = "Hieroglyphen",
        answerC = "Kyrillisch",
        answerD = "Runen",
        correctAnswer = 1,
        explanation = "Hieroglyphen waren die Bilderschrift der alten Ägypter und wurden für religiöse und offizielle Texte verwendet.",
        difficulty = 1,
        funFact = "Die Hieroglyphen wurden erst 1822 von Jean-François Champollion mithilfe des Rosetta-Steins entziffert."
    ),

    // --- African History: Colonization ---

    Question(
        categoryId = 3,
        questionText = "Auf welcher Konferenz wurde Afrika 1884/1885 unter europäischen Mächten aufgeteilt?",
        answerA = "Wiener Kongress",
        answerB = "Pariser Friedenskonferenz",
        answerC = "Berliner Konferenz",
        answerD = "Genfer Konvention",
        correctAnswer = 2,
        explanation = "Auf der Berliner Konferenz (1884–1885), auch 'Kongo-Konferenz' genannt, teilten europäische Mächte Afrika unter sich auf.",
        difficulty = 1,
        funFact = "Auf der Berliner Konferenz saßen keine afrikanischen Vertreter am Tisch – ganz Afrika wurde ohne Mitsprache der Bevölkerung aufgeteilt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches afrikanische Land blieb während der Kolonialzeit weitgehend unabhängig?",
        answerA = "Kongo",
        answerB = "Äthiopien",
        answerC = "Nigeria",
        answerD = "Angola",
        correctAnswer = 1,
        explanation = "Äthiopien wehrte die italienische Invasion 1896 in der Schlacht von Adua erfolgreich ab und blieb als einziges afrikanisches Land kolonialfrei.",
        difficulty = 1,
        funFact = "Äthiopien war auch Gründungsmitglied der Vereinten Nationen und gilt als Symbol afrikanischer Unabhängigkeit."
    ),

    // --- Asian History: Silk Road ---

    Question(
        categoryId = 3,
        questionText = "Was wurde hauptsächlich auf der Seidenstraße gehandelt?",
        answerA = "Nur Seide",
        answerB = "Waren, Ideen und Kulturen zwischen Ost und West",
        answerC = "Ausschließlich Gewürze",
        answerD = "Nur Waffen",
        correctAnswer = 1,
        explanation = "Die Seidenstraße war ein Netz von Handelsrouten, auf denen Waren, Ideen, Religionen und Kulturen zwischen China, Zentralasien und Europa ausgetauscht wurden.",
        difficulty = 1,
        funFact = "Die Seidenstraße war kein einzelner Weg, sondern ein Netzwerk von Routen über Land und Wasser."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher venezianische Händler bereiste die Seidenstraße im 13. Jahrhundert und besuchte China?",
        answerA = "Christoph Kolumbus",
        answerB = "Vasco da Gama",
        answerC = "Marco Polo",
        answerD = "Ferdinand Magellan",
        correctAnswer = 2,
        explanation = "Marco Polo bereiste zwischen 1271 und 1295 die Seidenstraße und den chinesischen Kaiserhof unter Kublai Khan.",
        difficulty = 1,
        funFact = "Marco Polos Reisebericht 'Il Milione' inspirierte Generationen von Entdeckern, darunter Christoph Kolumbus."
    ),

    // --- Asian History: Great Wall of China ---

    Question(
        categoryId = 3,
        questionText = "Wozu wurde die Chinesische Mauer hauptsächlich gebaut?",
        answerA = "Als Touristenattraktion",
        answerB = "Als Handelsroute",
        answerC = "Zum Schutz vor Invasionen aus dem Norden",
        answerD = "Als religiöses Monument",
        correctAnswer = 2,
        explanation = "Die Chinesische Mauer wurde hauptsächlich zum Schutz vor Überfällen und Invasionen nordlicher Nomadenvölker errichtet.",
        difficulty = 1,
        funFact = "Die Chinesische Mauer ist mit über 21.000 Kilometern das längste Bauwerk der Welt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche chinesische Dynastie begann den Bau der Großen Mauer in ihrer heutigen Form?",
        answerA = "Han-Dynastie",
        answerB = "Ming-Dynastie",
        answerC = "Tang-Dynastie",
        answerD = "Song-Dynastie",
        correctAnswer = 1,
        explanation = "Den größten Teil der heute sichtbaren Chinesischen Mauer ließ die Ming-Dynastie (1368–1644) errichten.",
        difficulty = 1,
        funFact = "Erste Mauerabschnitte wurden bereits unter dem ersten Kaiser Qin Shi Huangdi im 3. Jahrhundert v. Chr. gebaut."
    ),

    // --- Asian History: Samurai ---

    Question(
        categoryId = 3,
        questionText = "Was waren Samurai?",
        answerA = "Japanische Händler",
        answerB = "Japanische Krieger des mittelalterlichen Adels",
        answerC = "Chinesische Mönche",
        answerD = "Koreanische Seefahrer",
        correctAnswer = 1,
        explanation = "Samurai waren Krieger des japanischen Adels, die einem strengen Ehrenkodex namens Bushido folgten.",
        difficulty = 1,
        funFact = "Das Wort 'Samurai' stammt vom japanischen Verb 'saburau', was 'jemandem dienen' bedeutet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß der Ehrenkodex der Samurai?",
        answerA = "Harakiri",
        answerB = "Katana",
        answerC = "Bushido",
        answerD = "Shogun",
        correctAnswer = 2,
        explanation = "Bushido ('Weg des Kriegers') war der moralische Kodex der Samurai, der Werte wie Ehre, Loyalität und Mut betonte.",
        difficulty = 1,
        funFact = "Bushido verlangte von einem Samurai, der seine Ehre verloren hatte, rituellen Selbstmord (Seppuku) zu begehen."
    ),

    // --- South American History: Inca ---

    Question(
        categoryId = 3,
        questionText = "Wo befand sich das Zentrum des Inka-Reiches?",
        answerA = "Buenos Aires",
        answerB = "Lima",
        answerC = "Cuzco",
        answerD = "Bogotá",
        correctAnswer = 2,
        explanation = "Cuzco (im heutigen Peru) war die Hauptstadt des Inka-Reiches und galt als 'Nabel der Welt'.",
        difficulty = 1,
        funFact = "Das Inka-Reich war das größte Reich in vorkolumbianischen Amerika und erstreckte sich über 4.000 Kilometer."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche berühmte Inka-Stadt liegt hoch in den Anden und wurde 1911 wiederentdeckt?",
        answerA = "Chichén Itzá",
        answerB = "Teotihuacán",
        answerC = "Machu Picchu",
        answerD = "Tiwanaku",
        correctAnswer = 2,
        explanation = "Machu Picchu wurde 1911 vom amerikanischen Historiker Hiram Bingham wiederentdeckt und liegt auf 2.430 Meter Höhe in Peru.",
        difficulty = 1,
        funFact = "Machu Picchu wurde vermutlich um 1450 erbaut und von den Spaniern während der Eroberung nie entdeckt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher spanische Eroberer besiegte das Inka-Reich?",
        answerA = "Hernán Cortés",
        answerB = "Francisco Pizarro",
        answerC = "Juan Ponce de León",
        answerD = "Amerigo Vespucci",
        correctAnswer = 1,
        explanation = "Francisco Pizarro führte die spanische Eroberung des Inka-Reiches an und nahm 1532 den Inka-Herrscher Atahualpa gefangen.",
        difficulty = 1,
        funFact = "Pizarro besiegte das Inka-Reich mit nur 168 Männern – hauptsächlich durch eingeschleppte Krankheiten und Bürgerkrieg geschwächt."
    ),

    // --- South American History: Aztec ---

    Question(
        categoryId = 3,
        questionText = "Wo befand sich die Hauptstadt des Aztekenreiches?",
        answerA = "Cuzco",
        answerB = "Tenochtitlán",
        answerC = "Caracas",
        answerD = "Chichén Itzá",
        correctAnswer = 1,
        explanation = "Tenochtitlán (heute Mexiko-Stadt) war die Hauptstadt des Aztekenreiches, erbaut auf einer Insel im Texcoco-See.",
        difficulty = 1,
        funFact = "Tenochtitlán hatte im 15. Jahrhundert über 200.000 Einwohner und war damit eine der größten Städte der Welt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher spanische Konquistador eroberte das Aztekenreich?",
        answerA = "Francisco Pizarro",
        answerB = "Hernán Cortés",
        answerC = "Diego de Almagro",
        answerD = "Vasco Núñez de Balboa",
        correctAnswer = 1,
        explanation = "Hernán Cortés eroberte das Aztekenreich zwischen 1519 und 1521 und zerstörte Tenochtitlán.",
        difficulty = 1,
        funFact = "Cortés nutzte Bündnisse mit anderen indigenen Völkern, die unter aztekischer Herrschaft litten, um das Reich zu besiegen."
    ),

    // --- South American History: Maya ---

    Question(
        categoryId = 3,
        questionText = "Auf welcher Halbinsel in Mexiko lebten die Maya hauptsächlich?",
        answerA = "Baja California",
        answerB = "Yucatán",
        answerC = "Florida",
        answerD = "Iberische Halbinsel",
        correctAnswer = 1,
        explanation = "Die Maya-Zivilisation entwickelte sich hauptsächlich auf der Halbinsel Yucatán in Mexiko sowie in Teilen von Guatemala, Belize und Honduras.",
        difficulty = 1,
        funFact = "Die Maya entwickelten eine der wenigen vollständigen Schriftsysteme Mesoamerikas sowie einen präzisen Kalender."
    ),

    Question(
        categoryId = 3,
        questionText = "Wofür waren die Maya besonders bekannt?",
        answerA = "Ihren Schiffbau",
        answerB = "Ihren Kalender und ihre Astronomie",
        answerC = "Ihre Eisenbearbeitung",
        answerD = "Ihre Demokratie",
        correctAnswer = 1,
        explanation = "Die Maya waren für ihren hochpräzisen Kalender, ihre Astronomie sowie ihre ausgefeilte Mathematik bekannt.",
        difficulty = 1,
        funFact = "Der Maya-Kalender war so präzise, dass er das Sonnenjahr auf 365,2420 Tage berechnete – nur 0,0002 Tage von der modernen Messung entfernt."
    ),

    // --- Australian History ---

    Question(
        categoryId = 3,
        questionText = "Wie lange leben die Aborigines schätzungsweise in Australien?",
        answerA = "Seit etwa 5.000 Jahren",
        answerB = "Seit etwa 50.000 Jahren",
        answerC = "Seit etwa 10.000 Jahren",
        answerD = "Seit etwa 100.000 Jahren",
        correctAnswer = 1,
        explanation = "Die Aborigines leben seit mindestens 50.000 Jahren in Australien und gelten damit als eine der ältesten anhaltenden Kulturen der Welt.",
        difficulty = 1,
        funFact = "Die Aborigines entwickelten eine der reichsten mündlichen Überlieferungskulturen der Menschheit, bekannt als 'Traumzeit'."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher britische Seefahrer beanspruchte Australien 1770 für Großbritannien?",
        answerA = "Francis Drake",
        answerB = "James Cook",
        answerC = "Walter Raleigh",
        answerD = "Henry Hudson",
        correctAnswer = 1,
        explanation = "James Cook erkundete 1770 die Ostküste Australiens und nahm sie im Namen der britischen Krone in Besitz.",
        difficulty = 1,
        funFact = "Cook nannte das Land 'New South Wales'. Die erste britische Siedlung wurde 1788 in der heutigen Sydney Cove gegründet."
    ),

    Question(
        categoryId = 3,
        questionText = "Als welche Art Kolonie wurde Australien von den Briten zunächst genutzt?",
        answerA = "Als Handelsposten",
        answerB = "Als Strafkolonie für Verurteilte",
        answerC = "Als Militärbasis",
        answerD = "Als Religionsfluchtstätte",
        correctAnswer = 1,
        explanation = "Australien wurde ab 1788 als britische Strafkolonie genutzt. Über 160.000 Sträflinge wurden dorthin deportiert.",
        difficulty = 1,
        funFact = "Viele der frühen weißen Australier waren Nachkommen dieser Sträflinge. Die Deportationen endeten 1868."
    ),

    // --- Treaty of Versailles ---

    Question(
        categoryId = 3,
        questionText = "Wann wurde der Versailler Vertrag unterzeichnet?",
        answerA = "1917",
        answerB = "1918",
        answerC = "1919",
        answerD = "1920",
        correctAnswer = 2,
        explanation = "Der Versailler Vertrag wurde am 28. Juni 1919 unterzeichnet und beendete offiziell den Ersten Weltkrieg.",
        difficulty = 1,
        funFact = "Er wurde genau fünf Jahre nach dem Attentat auf Franz Ferdinand unterzeichnet – am 28. Juni 1919."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Land wurde durch den Versailler Vertrag hauptsächlich für den Ersten Weltkrieg verantwortlich gemacht?",
        answerA = "Österreich-Ungarn",
        answerB = "Das Osmanische Reich",
        answerC = "Deutschland",
        answerD = "Bulgarien",
        correctAnswer = 2,
        explanation = "Artikel 231 des Versailler Vertrags, die sogenannte 'Kriegsschuldklausel', machte Deutschland für den Ersten Weltkrieg verantwortlich.",
        difficulty = 1,
        funFact = "Die Kriegsschuldklausel und die enormen Reparationszahlungen destabilisierten die Weimarer Republik und trugen zum Aufstieg Hitlers bei."
    ),

    // --- Treaty of Westphalia ---

    Question(
        categoryId = 3,
        questionText = "Welchen Krieg beendete der Westfälische Frieden von 1648?",
        answerA = "Den Siebenjährigen Krieg",
        answerB = "Den Dreißigjährigen Krieg",
        answerC = "Den Hundertjährigen Krieg",
        answerD = "Den Nordischen Krieg",
        correctAnswer = 1,
        explanation = "Der Westfälische Frieden beendete 1648 den Dreißigjährigen Krieg (1618–1648), der große Teile Europas verwüstet hatte.",
        difficulty = 1,
        funFact = "Der Westfälische Frieden gilt als Grundlage des modernen Staatensystems, da er die Souveränität der Nationalstaaten begründete."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchen Städten wurde der Westfälische Frieden 1648 unterzeichnet?",
        answerA = "Wien und Prag",
        answerB = "Berlin und Hamburg",
        answerC = "Osnabrück und Münster",
        answerD = "Paris und London",
        correctAnswer = 2,
        explanation = "Der Westfälische Frieden wurde gleichzeitig in Osnabrück (mit Schweden) und Münster (mit Frankreich) unterzeichnet.",
        difficulty = 1,
        funFact = "Die Friedensverhandlungen dauerten über fünf Jahre. Es war einer der ersten großen multilateralen Friedenskongresse der Geschichte."
    ),

    // --- Famous Speeches ---

    Question(
        categoryId = 3,
        questionText = "Wer hielt die berühmte Rede 'Ich habe einen Traum' im Jahr 1963?",
        answerA = "Malcolm X",
        answerB = "Martin Luther King Jr.",
        answerC = "John F. Kennedy",
        answerD = "Nelson Mandela",
        correctAnswer = 1,
        explanation = "Martin Luther King Jr. hielt seine legendäre Rede 'I Have a Dream' am 28. August 1963 beim Marsch auf Washington.",
        difficulty = 1,
        funFact = "Vor dem Lincoln Memorial sprach King zu über 250.000 Menschen – es war einer der größten Protests der US-Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher US-Präsident sprach die berühmten Worte 'Ich bin ein Berliner'?",
        answerA = "Dwight D. Eisenhower",
        answerB = "Richard Nixon",
        answerC = "Lyndon B. Johnson",
        answerD = "John F. Kennedy",
        correctAnswer = 3,
        explanation = "John F. Kennedy hielt am 26. Juni 1963 in West-Berlin eine Rede, in der er den berühmten Satz 'Ich bin ein Berliner' sprach.",
        difficulty = 1,
        funFact = "Die Rede war ein Symbol der Solidarität des Westens mit West-Berlin während des Kalten Krieges."
    ),

    Question(
        categoryId = 3,
        questionText = "Winston Churchill rief in einer berühmten Rede zum Widerstand auf. Welchen Ausdruck verwendete er für die nötige Ausdauer?",
        answerA = "Kein Kapitulieren, nie",
        answerB = "Blut, Schweiß und Tränen",
        answerC = "Für Gott und König",
        answerD = "Freiheit oder Tod",
        correctAnswer = 1,
        explanation = "Winston Churchill sagte in seiner Rede vom 13. Mai 1940: 'Ich habe nichts anzubieten als Blut, Mühsal, Tränen und Schweiß.'",
        difficulty = 1,
        funFact = "Churchill hielt diese Rede nur drei Tage nach seiner Ernennung zum Premierminister, während Deutschland die Niederlande und Belgien invadierte."
    ),

    // --- Nobel Prize History ---

    Question(
        categoryId = 3,
        questionText = "Nach wem ist der Nobelpreis benannt?",
        answerA = "Albert Einstein",
        answerB = "Alfred Nobel",
        answerC = "Alexander Nobel",
        answerD = "Arthur Nobel",
        correctAnswer = 1,
        explanation = "Der Nobelpreis ist nach Alfred Nobel (1833–1896) benannt, dem schwedischen Erfinder des Dynamits, der ihn in seinem Testament stiftete.",
        difficulty = 1,
        funFact = "Alfred Nobel soll die Nobelpreise gestiftet haben, nachdem er irrtümlich seinen eigenen Nachruf las, der ihn als 'Händler des Todes' bezeichnete."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurden die Nobelpreise erstmals verliehen?",
        answerA = "1895",
        answerB = "1901",
        answerC = "1910",
        answerD = "1918",
        correctAnswer = 1,
        explanation = "Die ersten Nobelpreise wurden am 10. Dezember 1901, dem fünften Todestag von Alfred Nobel, verliehen.",
        difficulty = 1,
        funFact = "Der 10. Dezember, Nobels Todestag, ist bis heute der offizielle Tag der Nobelpreisverleihung in Stockholm und Oslo."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo wird der Friedensnobelpreis verliehen?",
        answerA = "Stockholm, Schweden",
        answerB = "Genf, Schweiz",
        answerC = "Oslo, Norwegen",
        answerD = "Brüssel, Belgien",
        correctAnswer = 2,
        explanation = "Als einziger Nobelpreis wird der Friedensnobelpreis in Oslo, Norwegen, verliehen. Alle anderen Preise werden in Stockholm vergeben.",
        difficulty = 1,
        funFact = "Der Grund für Oslo ist unbekannt – Alfred Nobel erwähnte Norwegen in seinem Testament, ohne den Grund zu erklären."
    ),

    // --- Women in History: Cleopatra ---

    Question(
        categoryId = 3,
        questionText = "Welche Königin herrschte über das alte Ägypten und war für ihre Verbündeten Julius Caesar und Mark Anton bekannt?",
        answerA = "Nofretete",
        answerB = "Hatschepsut",
        answerC = "Kleopatra VII.",
        answerD = "Isis",
        correctAnswer = 2,
        explanation = "Kleopatra VII. (69–30 v. Chr.) war die letzte Herrscherin des ptolemäischen Ägyptens und unterhielt enge Beziehungen zu Julius Caesar und Mark Anton.",
        difficulty = 1,
        funFact = "Kleopatra war die erste ptolemäische Herrscherin, die Ägyptisch sprach – neben acht weiteren Sprachen."
    ),

    Question(
        categoryId = 3,
        questionText = "Aus welcher Dynastie stammte Kleopatra VII.?",
        answerA = "Aus der Ramessiden-Dynastie",
        answerB = "Aus der Ptolemäer-Dynastie",
        answerC = "Aus der Thutmosiden-Dynastie",
        answerD = "Aus der Achaemeniden-Dynastie",
        correctAnswer = 1,
        explanation = "Kleopatra VII. stammte aus der Ptolemäer-Dynastie, die von einem der Generäle Alexanders des Großen gegründet wurde.",
        difficulty = 1,
        funFact = "Obwohl Kleopatra Ägyptens letzte Pharaonin war, war sie griechischer Abstammung – die Ptolemäer stammten ursprünglich aus Mazedonien."
    ),

    // --- Women in History: Marie Curie ---

    Question(
        categoryId = 3,
        questionText = "Marie Curie war die erste Frau, die einen Nobelpreis gewann. Für welches Fachgebiet erhielt sie ihren ersten Preis?",
        answerA = "Chemie",
        answerB = "Medizin",
        answerC = "Physik",
        answerD = "Literatur",
        correctAnswer = 2,
        explanation = "Marie Curie erhielt 1903 den Nobelpreis für Physik für ihre Forschungen zur Strahlung, zusammen mit ihrem Mann Pierre Curie.",
        difficulty = 1,
        funFact = "Marie Curie ist die einzige Person, die Nobelpreise in zwei verschiedenen Wissenschaften gewann: 1903 in Physik und 1911 in Chemie."
    ),

    Question(
        categoryId = 3,
        questionText = "Aus welchem Land stammte Marie Curie ursprünglich?",
        answerA = "Frankreich",
        answerB = "Deutschland",
        answerC = "Polen",
        answerD = "Russland",
        correctAnswer = 2,
        explanation = "Marie Curie wurde 1867 als Maria Skłodowska in Warschau, Polen, geboren und emigrierte später nach Frankreich.",
        difficulty = 1,
        funFact = "Marie Curie entdeckte zusammen mit ihrem Mann Pierre Curie die Elemente Polonium (nach ihrer Heimat Polen benannt) und Radium."
    ),

    // --- Women in History: Joan of Arc ---

    Question(
        categoryId = 3,
        questionText = "Wer war Jeanne d'Arc?",
        answerA = "Eine französische Königin",
        answerB = "Eine Bäuerin, die Frankreich im Hundertjährigen Krieg anführte",
        answerC = "Eine englische Adlige",
        answerD = "Eine Klostergründerin",
        correctAnswer = 1,
        explanation = "Jeanne d'Arc (um 1412–1431) war eine Bauerntochter, die nach eigenen Angaben göttliche Visionen hatte und die französische Armee gegen England anführte.",
        difficulty = 1,
        funFact = "Jeanne d'Arc wurde von den Engländern gefangen und 1431 im Alter von etwa 19 Jahren als Ketzerin verbrannt. 1920 wurde sie heiliggesprochen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie endete das Leben von Jeanne d'Arc?",
        answerA = "Sie starb im Kampf",
        answerB = "Sie wurde auf dem Scheiterhaufen verbrannt",
        answerC = "Sie wurde Nonne",
        answerD = "Sie starb im Gefängnis",
        correctAnswer = 1,
        explanation = "Jeanne d'Arc wurde 1431 in Rouen von den Engländern als Ketzerin auf dem Scheiterhaufen verbrannt.",
        difficulty = 1,
        funFact = "25 Jahre nach ihrem Tod wurde Jeanne d'Arc rehabilitiert und für unschuldig erklärt. Heute ist sie eine Nationalheilige Frankreichs."
    ),

    // --- Mixed Topics ---

    Question(
        categoryId = 3,
        questionText = "Welche Pharaonin regierte Ägypten als erste weibliche Pharaonin über einen langen Zeitraum?",
        answerA = "Nofretete",
        answerB = "Kleopatra",
        answerC = "Hatschepsut",
        answerD = "Meritaten",
        correctAnswer = 2,
        explanation = "Hatschepsut (um 1507–1458 v. Chr.) war eine der ersten und mächtigsten weiblichen Pharaoninnen Ägyptens.",
        difficulty = 1,
        funFact = "Hatschepsut ließ sich oft mit Bart und männlicher Kleidung darstellen, um als Pharao anerkannt zu werden."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die heilige Stadt der Inka, die als 'Nabel der Welt' galt?",
        answerA = "Lima",
        answerB = "Cuzco",
        answerC = "Machu Picchu",
        answerD = "Quito",
        correctAnswer = 1,
        explanation = "Cuzco war die Hauptstadt und heilige Stadt des Inka-Reiches. Der Name bedeutet in Quechua 'Nabel' oder 'Mittelpunkt'.",
        difficulty = 1,
        funFact = "Das Inka-Reich nannte sich selbst 'Tawantinsuyu', was 'Die vier Himmelsrichtungen' bedeutet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo wurde der Nobelpreis für Literatur verliehen?",
        answerA = "Oslo",
        answerB = "Genf",
        answerC = "Stockholm",
        answerD = "Kopenhagen",
        correctAnswer = 2,
        explanation = "Der Nobelpreis für Literatur wird, wie alle Nobelpreise außer dem Friedensnobelpreis, in Stockholm, Schweden, verliehen.",
        difficulty = 1,
        funFact = "Der Literaturnobelpreis wird von der Schwedischen Akademie vergeben, die 1786 vom schwedischen König Gustav III. gegründet wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Ehrenkodex befolgten japanische Samurai?",
        answerA = "Tao",
        answerB = "Karma",
        answerC = "Zen",
        answerD = "Bushido",
        correctAnswer = 3,
        explanation = "Bushido war der moralische Kodex der Samurai, der auf Werten wie Ehre, Treue, Mut, Aufrichtigkeit und Selbstdisziplin basierte.",
        difficulty = 1,
        funFact = "Das Buch 'Hagakure' aus dem 18. Jahrhundert gilt als eines der wichtigsten Werke über den Geist des Bushido."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Kontinent wurde in der Berliner Konferenz (1884–85) unter den Kolonialmächten aufgeteilt?",
        answerA = "Asien",
        answerB = "Amerika",
        answerC = "Australien",
        answerD = "Afrika",
        correctAnswer = 3,
        explanation = "Auf der Berliner Konferenz 1884/85 teilten europäische Mächte unter Leitung von Otto von Bismarck den afrikanischen Kontinent unter sich auf.",
        difficulty = 1,
        funFact = "Bei der Konferenz war kein einziger Afrikaner anwesend. Innerhalb von 20 Jahren kontrollierten die Europäer 90% Afrikas."
    ),

    Question(
        categoryId = 3,
        questionText = "Für welche wissenschaftliche Entdeckung erhielt Marie Curie ihren zweiten Nobelpreis 1911?",
        answerA = "Entdeckung der Röntgenstrahlen",
        answerB = "Isolierung des Elements Radium",
        answerC = "Entwicklung der Kernspaltung",
        answerD = "Entdeckung der DNA",
        correctAnswer = 1,
        explanation = "Marie Curie erhielt 1911 den Nobelpreis für Chemie für die Isolierung des Elements Radium in reiner Form.",
        difficulty = 1,
        funFact = "Marie Curies Notizen aus den 1890er Jahren sind heute noch so radioaktiv, dass sie in bleigefütterten Behältern aufbewahrt werden."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Seidenstraße hauptsächlich?",
        answerA = "Ein Fluss in China",
        answerB = "Ein Netzwerk von Handelsrouten zwischen China und Europa",
        answerC = "Eine Mauer zum Schutz Chinas",
        answerD = "Eine Pilgerroute nach Jerusalem",
        correctAnswer = 1,
        explanation = "Die Seidenstraße war ein antikes Netzwerk von Handels- und Kulturrouten, das China mit Zentralasien, dem Nahen Osten und Europa verband.",
        difficulty = 1,
        funFact = "Die Seidenstraße bestand nicht aus einem einzigen Weg, sondern aus einem Netz von Routen über Land und See."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Hochkultur baute die Tempelanlage Chichén Itzá in Mexiko?",
        answerA = "Die Inka",
        answerB = "Die Azteken",
        answerC = "Die Maya",
        answerD = "Die Olmeken",
        correctAnswer = 2,
        explanation = "Chichén Itzá war eine bedeutende Maya-Stadt auf der Halbinsel Yucatán in Mexiko, bekannt für ihre Pyramiden und Tempel.",
        difficulty = 1,
        funFact = "Die Pyramide des Kukulcán in Chichén Itzá ist so konstruiert, dass bei Tag- und Nachtgleiche ein Schatten in Form einer Schlange entsteht."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann wurden in Australien die britischen Strafgefangenen-Deportationen endgültig eingestellt?",
        answerA = "1788",
        answerB = "1820",
        answerC = "1868",
        answerD = "1901",
        correctAnswer = 2,
        explanation = "Die Deportation britischer Strafgefangener nach Australien wurde 1868 endgültig eingestellt. Insgesamt wurden über 160.000 Sträflinge dorthin gebracht.",
        difficulty = 1,
        funFact = "Der erste Transportkonvoi mit Sträflingen traf am 26. Januar 1788 in der heutigen Sydneyer Cove ein – dieses Datum wird als Australia Day gefeiert."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher englische König wurde durch die Magna Carta 1215 in seiner Macht eingeschränkt?",
        answerA = "Richard Löwenherz",
        answerB = "König Johann Ohneland",
        answerC = "Eduard I.",
        answerD = "Heinrich VIII.",
        correctAnswer = 1,
        explanation = "König Johann Ohneland unterzeichnete 1215 die Magna Carta, die erstmals die Rechte der Adligen gegenüber dem König festlegte.",
        difficulty = 1,
        funFact = "Die Magna Carta gilt als eines der ältesten Dokumente der Demokratiegeschichte und beeinflusste die Verfassungen vieler moderner Staaten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Frau erhielt als erste Person überhaupt zwei Nobelpreise in unterschiedlichen Wissenschaften?",
        answerA = "Lise Meitner",
        answerB = "Rosalind Franklin",
        answerC = "Marie Curie",
        answerD = "Dorothy Hodgkin",
        correctAnswer = 2,
        explanation = "Marie Curie ist bis heute die einzige Person, die Nobelpreise in zwei verschiedenen Naturwissenschaften erhalten hat: 1903 in Physik und 1911 in Chemie.",
        difficulty = 1,
        funFact = "Marie Curies Tochter Irène Joliot-Curie gewann 1935 ebenfalls den Nobelpreis für Chemie – Mutter und Tochter sind beide Nobelpreisträgerinnen."
    ),

)
