package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsEasy(): List<Question> = listOf(

    // Question 1 - Ancient Egypt
    Question(
        categoryId = 3,
        questionText = "Wer war der berühmteste Pharao des alten Ägyptens, der als Knabenkönig bekannt ist?",
        answerA = "Ramses II.",
        answerB = "Tutanchamun",
        answerC = "Kleopatra",
        answerD = "Nofretete",
        correctAnswer = 1, // B
        explanation = "Tutanchamun bestieg mit etwa 9 Jahren den Thron und regierte ca. von 1332–1323 v. Chr. Er starb jung mit etwa 19 Jahren. Sein Grab wurde 1922 fast unberührt entdeckt.",
        funFact = "Als Howard Carter Tutanchamuns Grab öffnete, soll er gefragt worden sein, ob er etwas sehe. Er antwortete: 'Wunderbare Dinge!'"
    ),

    // Question 2 - Ancient Egypt
    Question(
        categoryId = 3,
        questionText = "Welcher Fluss ist der wichtigste für das alte Ägypten gewesen?",
        answerA = "Tigris",
        answerB = "Euphrat",
        answerC = "Nil",
        answerD = "Jordan",
        correctAnswer = 2, // C
        explanation = "Der Nil war die Lebensader des alten Ägyptens. Seine jährlichen Überschwemmungen düngten die Felder und machten das Land fruchtbar, mitten in der Wüste.",
        funFact = "Mit ca. 6.650 km ist der Nil der längste Fluss der Welt – er fließt von Zentralafrika bis zum Mittelmeer."
    ),

    // Question 3 - Ancient Egypt
    Question(
        categoryId = 3,
        questionText = "Wie nennt man die monumentalen Grabbauten der ägyptischen Pharaonen?",
        answerA = "Obelisken",
        answerB = "Sphinxe",
        answerC = "Tempel",
        answerD = "Pyramiden",
        correctAnswer = 3, // D
        explanation = "Die Pyramiden wurden als Grabstätten für die Pharaonen erbaut. Die bekanntesten stehen in Gizeh bei Kairo. Die Große Pyramide des Cheops ist eines der sieben Weltwunder der Antike.",
        funFact = "Die Große Pyramide besteht aus ca. 2,3 Millionen Steinblöcken, von denen jeder durchschnittlich 2,5 Tonnen wiegt."
    ),

    // Question 4 - Ancient Greece
    Question(
        categoryId = 3,
        questionText = "In welcher Stadt des antiken Griechenlands entstand die Demokratie?",
        answerA = "Sparta",
        answerB = "Korinth",
        answerC = "Athen",
        answerD = "Theben",
        correctAnswer = 2, // C
        explanation = "Athen gilt als Geburtsort der Demokratie. Um 508 v. Chr. führte Kleisthenes grundlegende demokratische Reformen ein, die den Bürgern politische Mitsprache ermöglichten.",
        funFact = "Das Wort 'Demokratie' kommt aus dem Griechischen: 'demos' (Volk) und 'kratos' (Herrschaft) – also Volksherrschaft."
    ),

    // Question 5 - Ancient Greece
    Question(
        categoryId = 3,
        questionText = "Welcher griechische Held soll mit einem trojanischen Pferd die Stadt Troja besiegt haben?",
        answerA = "Achilles",
        answerB = "Odysseus",
        answerC = "Herakles",
        answerD = "Perseus",
        correctAnswer = 1, // B
        explanation = "Odysseus soll laut dem Epos 'Ilias' und 'Odyssee' von Homer die Idee des hölzernen Pferds gehabt haben, mit dem die Griechen heimlich in Troja eindringen konnten.",
        funFact = "Homers 'Odyssee' beschreibt die zehnjährige Heimreise des Odysseus nach dem Trojanischen Krieg – voller Abenteuer und Hindernisse."
    ),

    // Question 6 - Ancient Greece
    Question(
        categoryId = 3,
        questionText = "Wer war der berühmte griechische Philosoph, der Sokrates als Schüler hatte?",
        answerA = "Aristoteles",
        answerB = "Platon",
        answerC = "Pythagoras",
        answerD = "Epikur",
        correctAnswer = 1, // B
        explanation = "Platon war ein Schüler des Sokrates und später Lehrer des Aristoteles. Er gründete die Akademie in Athen und schrieb berühmte philosophische Dialoge wie die 'Politeia'.",
        funFact = "Platon war ursprünglich Ringkämpfer – sein eigentlicher Name soll 'Aristokles' gewesen sein, 'Platon' (breite Schultern) war ein Spitzname."
    ),

    // Question 7 - Ancient Greece
    Question(
        categoryId = 3,
        questionText = "Welche sportlichen Wettkämpfe wurden im antiken Griechenland zu Ehren der Götter veranstaltet?",
        answerA = "Die Gladiatorenkämpfe",
        answerB = "Die Olympischen Spiele",
        answerC = "Die Ritterspiele",
        answerD = "Die Turniere",
        correctAnswer = 1, // B
        explanation = "Die Olympischen Spiele fanden erstmals 776 v. Chr. in Olympia statt und wurden alle vier Jahre zu Ehren des Gottes Zeus abgehalten. Sie waren das wichtigste sportliche Ereignis der Antike.",
        funFact = "Während der Olympischen Spiele in der Antike galt ein Waffenstillstand – Kriege wurden unterbrochen, damit Athleten sicher reisen konnten."
    ),

    // Question 8 - Ancient Rome
    Question(
        categoryId = 3,
        questionText = "Welches Symbol war das Wahrzeichen der Römischen Republik und des Kaiserreichs?",
        answerA = "Der Adler",
        answerB = "Der Löwe",
        answerC = "Der Wolf",
        answerD = "Der Stier",
        correctAnswer = 0, // A
        explanation = "Der Adler (Aquila) war das wichtigste Symbol Roms und wurde auf den Feldzeichen der Legionen getragen. Er symbolisierte Stärke, Macht und die Verbindung zu Jupiter.",
        funFact = "Jede römische Legion hatte einen Adler aus vergoldetem Silber, den sogenannten 'Aquilifer' getragen hat – der Verlust des Adlers war die größte Schande für eine Legion."
    ),

    // Question 9 - Ancient Rome
    Question(
        categoryId = 3,
        questionText = "Wie nannte man die Volksversammlungen und den Senat im alten Rom zusammen?",
        answerA = "Die Kurie",
        answerB = "Das Forum Romanum",
        answerC = "Der Senat",
        answerD = "Die Republik",
        correctAnswer = 3, // D
        explanation = "Die Römische Republik (lat. 'res publica' = öffentliche Sache) war das politische System Roms von ca. 509 v. Chr. bis 27 v. Chr., mit Senat, Volksversammlung und gewählten Konsuln.",
        funFact = "Das Wort 'Republik' kommt vom lateinischen 'res publica' – 'die öffentliche Angelegenheit'. Es wird bis heute für demokratische Staatsformen verwendet."
    ),

    // Question 10 - Ancient Rome
    Question(
        categoryId = 3,
        questionText = "Welches Bauwerk bauten die Römer in England, um das Reich vor den Pikten zu schützen?",
        answerA = "Der Limes",
        answerB = "Die Hadriansmauer",
        answerC = "Die Trajanssäule",
        answerD = "Das Kolosseum",
        correctAnswer = 1, // B
        explanation = "Kaiser Hadrian ließ ab 122 n. Chr. eine Mauer quer durch Nordengland bauen, um das römische Britannien vor Überfällen der Pikten zu schützen. Sie war ca. 117 km lang.",
        funFact = "Die Hadriansmauer ist heute ein UNESCO-Weltkulturerbe und zieht jährlich Hunderttausende Touristen an."
    ),

    // Question 11 - Ancient Rome
    Question(
        categoryId = 3,
        questionText = "Wie hieß die Hauptstadt des Weströmischen Reiches?",
        answerA = "Konstantinopel",
        answerB = "Karthago",
        answerC = "Ravenna",
        answerD = "Rom",
        correctAnswer = 3, // D
        explanation = "Rom war die Hauptstadt des Römischen Reiches für Jahrhunderte. Im 5. Jahrhundert wurde die Hauptstadt zeitweise nach Ravenna verlegt, aber Rom blieb das symbolische Zentrum.",
        funFact = "'Alle Wege führen nach Rom' – im Mittelalter war das fast wörtlich wahr, da das gesamte Straßennetz des Imperiums auf die Hauptstadt ausgerichtet war."
    ),

    // Question 12 - Medieval Europe
    Question(
        categoryId = 3,
        questionText = "Wann wurde Karl der Große zum Kaiser des Abendlandes gekrönt?",
        answerA = "800 n. Chr.",
        answerB = "768 n. Chr.",
        answerC = "912 n. Chr.",
        answerD = "1066 n. Chr.",
        correctAnswer = 0, // A
        explanation = "Karl der Große wurde am 25. Dezember 800 n. Chr. von Papst Leo III. in Rom zum Kaiser gekrönt. Er herrschte über weite Teile Europas und gilt als 'Vater Europas'.",
        funFact = "Karl der Große konnte trotz seiner Bildungsbemühungen sein ganzes Leben lang nicht richtig schreiben – er soll nachts unter seinem Kopfkissen Wachstafeln gehabt haben, um das Schreiben zu üben."
    ),

    // Question 13 - Medieval Europe
    Question(
        categoryId = 3,
        questionText = "Wie nannte man die Kriege, in denen europäische Ritter im Mittelalter nach Jerusalem zogen?",
        answerA = "Die Wikingerzüge",
        answerB = "Die Kreuzzüge",
        answerC = "Die Völkerwanderung",
        answerD = "Die Hundertjährigen Kriege",
        correctAnswer = 1, // B
        explanation = "Die Kreuzzüge (1095–1291) waren militärische Expeditionen europäischer Christen, um das Heilige Land und Jerusalem von muslimischer Herrschaft zu befreien. Es gab insgesamt sieben Hauptkreuzzüge.",
        funFact = "Beim Ersten Kreuzzug (1096–1099) zog zunächst ein chaotischer 'Volkskreuzzug' voraus, der fast vollständig aufgerieben wurde."
    ),

    // Question 14 - Medieval Europe
    Question(
        categoryId = 3,
        questionText = "Welche Seuche wütete im 14. Jahrhundert in Europa und tötete etwa ein Drittel der Bevölkerung?",
        answerA = "Die Cholera",
        answerB = "Die Pocken",
        answerC = "Der Schwarze Tod",
        answerD = "Die Influenza",
        correctAnswer = 2, // C
        explanation = "Der Schwarze Tod (Beulenpest) breitete sich von 1347 bis 1353 in Europa aus und tötete schätzungsweise 25–30 Millionen Menschen, ein Drittel bis die Hälfte der europäischen Bevölkerung.",
        funFact = "Der Schwarze Tod veränderte die europäische Gesellschaft grundlegend: Arbeitskräftemangel führte zum Ende des Feudalsystems und gab Bauern mehr Verhandlungsmacht."
    ),

    // Question 15 - Medieval Europe
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde England von den Normannen unter Wilhelm dem Eroberer besetzt?",
        answerA = "1066",
        answerB = "1099",
        answerC = "1215",
        answerD = "1348",
        correctAnswer = 0, // A
        explanation = "In der Schlacht von Hastings am 14. Oktober 1066 besiegte Wilhelm der Eroberer den englischen König Harald II. und wurde König von England. Dies veränderte die englische Sprache und Kultur dauerhaft.",
        funFact = "Durch die normannische Eroberung kamen tausende französische Wörter ins Englische. Deshalb haben viele englische Wörter heute sowohl germanische als auch französische Varianten."
    ),

    // Question 16 - Age of Exploration
    Question(
        categoryId = 3,
        questionText = "Wer war der erste Europäer, der den Seeweg nach Indien um Afrika herum entdeckte?",
        answerA = "Amerigo Vespucci",
        answerB = "Ferdinand Magellan",
        answerC = "Vasco da Gama",
        answerD = "Bartolomeu Dias",
        correctAnswer = 2, // C
        explanation = "Vasco da Gama umrundete 1497–1498 als erster Europäer Afrika (das Kap der Guten Hoffnung) und erreichte Indien. Dies eröffnete den Portugiesen den direkten Seehandelsweg nach Asien.",
        funFact = "Das Kap der Guten Hoffnung heißt so, weil man hoffte, nun den Weg nach Indien gefunden zu haben. Bartolomeu Dias, der es 1488 als erster umsegelte, nannte es ursprünglich 'Kap der Stürme'."
    ),

    // Question 17 - Age of Exploration
    Question(
        categoryId = 3,
        questionText = "Welcher Forscher segelte als erster um die gesamte Erde?",
        answerA = "James Cook",
        answerB = "Ferdinand Magellan",
        answerC = "Amerigo Vespucci",
        answerD = "Francis Drake",
        correctAnswer = 1, // B
        explanation = "Ferdinand Magellan brach 1519 mit einer spanischen Flotte auf. Er starb 1521 auf den Philippinen, doch sein Schiff 'Victoria' kehrte 1522 zurück – die erste Weltumsegelung war vollbracht.",
        funFact = "Von Magellans ursprünglich 270 Besatzungsmitgliedern und 5 Schiffen überlebte nur eines mit 18 Mann die Weltumsegelung. Die Reise dauerte knapp drei Jahre."
    ),

    // Question 18 - Age of Exploration
    Question(
        categoryId = 3,
        questionText = "Nach wem wurde Amerika benannt?",
        answerA = "Amerigo Vespucci",
        answerB = "Christoph Kolumbus",
        answerC = "John Cabot",
        answerD = "Hernán Cortés",
        correctAnswer = 0, // A
        explanation = "Amerika wurde nach dem florentinischen Seefahrer Amerigo Vespucci benannt, da er erkannte, dass die entdeckten Länder keine Teile Asiens, sondern ein neuer Kontinent waren.",
        funFact = "Der Kartograf Martin Waldseemüller schlug 1507 vor, den neuen Kontinent 'America' zu nennen – nach Amerigo Vespucci. Dieser Name setzte sich durch."
    ),

    // Question 19 - French Revolution
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr begann die Französische Revolution?",
        answerA = "1776",
        answerB = "1789",
        answerC = "1804",
        answerD = "1815",
        correctAnswer = 1, // B
        explanation = "Die Französische Revolution begann 1789 mit dem Sturm auf die Bastille am 14. Juli. Sie beendete die Monarchie und führte zur Ausrufung der Republik.",
        funFact = "Der 14. Juli (Bastilletag) ist bis heute der französische Nationalfeiertag. Bei der Erstürmung der Bastille befanden sich tatsächlich nur 7 Gefangene in der Festung."
    ),

    // Question 20 - French Revolution
    Question(
        categoryId = 3,
        questionText = "Welche Losung wurde zum Symbol der Französischen Revolution?",
        answerA = "Gott, König und Vaterland",
        answerB = "Freiheit, Gleichheit, Brüderlichkeit",
        answerC = "Einigkeit, Recht und Freiheit",
        answerD = "Arbeit, Ordnung, Fortschritt",
        correctAnswer = 1, // B
        explanation = "'Liberté, Égalité, Fraternité' (Freiheit, Gleichheit, Brüderlichkeit) war die Losung der Französischen Revolution und ist bis heute der offizielle Wahlspruch Frankreichs.",
        funFact = "Diese drei Werte inspirierten nicht nur Frankreich, sondern wurden zur Grundlage moderner demokratischer Verfassungen weltweit."
    ),

    // Question 21 - French Revolution / Napoleon
    Question(
        categoryId = 3,
        questionText = "Wer war Napoleon Bonaparte?",
        answerA = "Ein französischer König",
        answerB = "Ein englischer Admiral",
        answerC = "Ein französischer Kaiser und Feldherr",
        answerD = "Ein preußischer General",
        correctAnswer = 2, // C
        explanation = "Napoleon Bonaparte war ein französischer General, der sich 1804 selbst zum Kaiser der Franzosen krönte. Er eroberte weite Teile Europas, wurde aber 1815 bei Waterloo besiegt.",
        funFact = "Das Gerücht, Napoleon sei sehr klein gewesen, ist ein Mythos. Er war mit ca. 1,68–1,70 m für seine Zeit durchschnittlich groß. Die Verwechslung entstand durch einen Übersetzungsfehler mit dem schottischen Zoll-Maß."
    ),

    // Question 22 - Industrial Revolution
    Question(
        categoryId = 3,
        questionText = "In welchem Land begann die Industrielle Revolution?",
        answerA = "Frankreich",
        answerB = "Deutschland",
        answerC = "England",
        answerD = "USA",
        correctAnswer = 2, // C
        explanation = "Die Industrielle Revolution begann in der zweiten Hälfte des 18. Jahrhunderts in England. Die Erfindung der Dampfmaschine durch James Watt und neue Textilmaschinen revolutionierten die Produktion.",
        funFact = "Die erste Dampflokomotive, die Passagiere transportierte, war 'The Rocket' von George Stephenson – sie fuhr 1829 auf der Strecke zwischen Manchester und Liverpool."
    ),

    // Question 23 - Industrial Revolution
    Question(
        categoryId = 3,
        questionText = "Wer erfand die praktische Dampfmaschine, die die Industrialisierung antrieb?",
        answerA = "Thomas Edison",
        answerB = "James Watt",
        answerC = "George Stephenson",
        answerD = "Nikola Tesla",
        correctAnswer = 1, // B
        explanation = "James Watt verbesserte ab 1769 die Dampfmaschine so grundlegend, dass sie für industrielle Zwecke nutzbar wurde. Die Einheit der Leistung 'Watt' wurde ihm zu Ehren benannt.",
        funFact = "James Watt führte den Begriff 'Pferdestärke' (PS) ein, um die Leistung seiner Dampfmaschinen vergleichbar zu machen – eine Einheit, die wir beim Auto noch heute kennen."
    ),

    // Question 24 - Industrial Revolution
    Question(
        categoryId = 3,
        questionText = "Was war das wichtigste neue Transportmittel der Industriellen Revolution?",
        answerA = "Das Dampfschiff",
        answerB = "Die Kutsche",
        answerC = "Das Fahrrad",
        answerD = "Die Eisenbahn",
        correctAnswer = 3, // D
        explanation = "Die Eisenbahn revolutionierte den Transport von Waren und Menschen im 19. Jahrhundert. Sie ermöglichte schnelle, günstige Langstreckentransporte und verband Städte und Industriegebiete.",
        funFact = "Die erste Eisenbahn in Deutschland fuhr 1835 zwischen Nürnberg und Fürth – eine Strecke von gerade einmal 7 km, die die Lokomotive 'Adler' in ca. 9 Minuten zurücklegte."
    ),

    // Question 25 - World War Basics
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr endete der Zweite Weltkrieg in Europa?",
        answerA = "1943",
        answerB = "1944",
        answerC = "1945",
        answerD = "1946",
        correctAnswer = 2, // C
        explanation = "Der Zweite Weltkrieg in Europa endete am 8. Mai 1945 mit der bedingungslosen Kapitulation der deutschen Wehrmacht. Dieser Tag wird als 'Tag der Befreiung' oder 'V-E Day' (Victory in Europe Day) gefeiert.",
        funFact = "In Russland wird der 9. Mai als Tag des Sieges gefeiert, weil die Kapitulationsurkunde nach Moskauer Zeit in der Nacht vom 8. auf den 9. Mai unterzeichnet wurde."
    ),

    // Question 26 - World War Basics
    Question(
        categoryId = 3,
        questionText = "Wer war der Diktator Deutschlands während des Zweiten Weltkriegs?",
        answerA = "Heinrich Himmler",
        answerB = "Hermann Göring",
        answerC = "Joseph Goebbels",
        answerD = "Adolf Hitler",
        correctAnswer = 3, // D
        explanation = "Adolf Hitler war von 1933 bis 1945 Diktator Deutschlands. Er führte das Land in den Zweiten Weltkrieg und verantwortete den Holocaust, in dem sechs Millionen Juden ermordet wurden.",
        funFact = null
    ),

    // Question 27 - World War Basics
    Question(
        categoryId = 3,
        questionText = "Was war der Name des alliierten Angriffs auf die Normandie am 6. Juni 1944?",
        answerA = "Operation Barbarossa",
        answerB = "Operation Market Garden",
        answerC = "Operation Overlord",
        answerD = "Operation Sea Lion",
        correctAnswer = 2, // C
        explanation = "Die Landung in der Normandie am 6. Juni 1944 (D-Day) war Teil der Operation Overlord. Sie war die größte amphibische Militäroperation der Geschichte und leitete die Befreiung Westeuropas ein.",
        funFact = "Über 156.000 alliierte Soldaten landeten am D-Day an fünf verschiedenen Stränden der Normandie. Es war die größte Seeinvasion der Weltgeschichte."
    ),

    // Question 28 - World War Basics
    Question(
        categoryId = 3,
        questionText = "Welches Ereignis brachte die USA in den Zweiten Weltkrieg?",
        answerA = "Der Angriff auf Pearl Harbor",
        answerB = "Die Invasion der Normandie",
        answerC = "Die Bombardierung Londons",
        answerD = "Die Atlantikcharta",
        correctAnswer = 0, // A
        explanation = "Am 7. Dezember 1941 griffen japanische Streitkräfte den amerikanischen Marinestützpunkt Pearl Harbor auf Hawaii an. Tags darauf erklärten die USA Japan den Krieg und traten in den Zweiten Weltkrieg ein.",
        funFact = "Präsident Roosevelt bezeichnete den 7. Dezember 1941 als 'a date which will live in infamy' (ein Datum, das in Schande weiterleben wird). Über 2.400 Amerikaner starben beim Angriff."
    ),

    // Question 29 - Cold War Basics
    Question(
        categoryId = 3,
        questionText = "Welche zwei Supermächte standen sich im Kalten Krieg gegenüber?",
        answerA = "USA und China",
        answerB = "USA und UdSSR",
        answerC = "England und Russland",
        answerD = "Deutschland und Amerika",
        correctAnswer = 1, // B
        explanation = "Der Kalte Krieg (1947–1991) war ein politischer und militärischer Konflikt zwischen den USA und der Sowjetunion (UdSSR). Er prägte die Weltpolitik für über 40 Jahre.",
        funFact = "Der Begriff 'Kalter Krieg' wurde erstmals 1945 vom Schriftsteller George Orwell verwendet – als Bezeichnung für eine Welt, in der zwei Supermächte sich mit Atomwaffen gegenseitig in Schach halten."
    ),

    // Question 30 - Cold War Basics
    Question(
        categoryId = 3,
        questionText = "Welches sowjetische Raumfahrtprogramm schickte 1957 den ersten Satelliten ins All?",
        answerA = "Explorer",
        answerB = "Luna",
        answerC = "Sputnik",
        answerD = "Vostok",
        correctAnswer = 2, // C
        explanation = "Sputnik 1 war der erste von Menschen gebaute Satellit, der am 4. Oktober 1957 von der Sowjetunion ins All geschossen wurde. Er löste das 'Sputnik-Schock' genannte Wettrüsten im Weltraum aus.",
        funFact = "Sputnik war nur etwa so groß wie ein Wasserball (58 cm Durchmesser) und übertrug lediglich ein einfaches Funksignal – aber es reichte aus, die USA in tiefe Unruhe zu versetzen."
    ),

    // Question 31 - Cold War Basics
    Question(
        categoryId = 3,
        questionText = "Wer war der erste Mensch auf dem Mond?",
        answerA = "Buzz Aldrin",
        answerB = "Juri Gagarin",
        answerC = "Neil Armstrong",
        answerD = "Michael Collins",
        correctAnswer = 2, // C
        explanation = "Neil Armstrong betrat am 20. Juli 1969 als erster Mensch den Mond. Er sagte die berühmten Worte: 'Das ist ein kleiner Schritt für einen Menschen, aber ein großer Sprung für die Menschheit.'",
        funFact = "Buzz Aldrin war der zweite Mensch auf dem Mond, nur wenige Minuten nach Armstrong. Michael Collins blieb im Mondorbit und umkreiste den Mond, während seine Kollegen die Oberfläche erkundeten."
    ),

    // Question 32 - Cold War Basics
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr begann die Kuba-Krise, bei der die Welt kurz vor einem Atomkrieg stand?",
        answerA = "1956",
        answerB = "1961",
        answerC = "1962",
        answerD = "1968",
        correctAnswer = 2, // C
        explanation = "Die Kubakrise fand im Oktober 1962 statt, als die USA entdeckten, dass die Sowjetunion Atomraketen auf Kuba stationierte. Für 13 Tage drohte ein nuklearer Krieg, bevor eine diplomatische Lösung gefunden wurde.",
        funFact = "Während der Kubakrise erhielt US-Präsident Kennedy angeblich täglich genaue Berichte über die Positionierung sowjetischer Schiffe – die Welt war dem Atomkrieg so nah wie nie zuvor oder danach."
    ),

    // Question 33 - German History
    Question(
        categoryId = 3,
        questionText = "Wann wurde die Bundesrepublik Deutschland gegründet?",
        answerA = "1945",
        answerB = "1947",
        answerC = "1949",
        answerD = "1955",
        correctAnswer = 2, // C
        explanation = "Die Bundesrepublik Deutschland (BRD) wurde am 23. Mai 1949 mit dem Inkrafttreten des Grundgesetzes gegründet. Die DDR (Ostdeutschland) entstand am 7. Oktober 1949.",
        funFact = "Das Grundgesetz war ursprünglich nur als vorläufige Verfassung gedacht, bis Deutschland wiedervereinigt sein würde. Aus diesem Grund hieß es 'Grundgesetz' und nicht 'Verfassung'."
    ),

    // Question 34 - German History
    Question(
        categoryId = 3,
        questionText = "Wann wurde Deutschland nach dem Zweiten Weltkrieg wiedervereinigt?",
        answerA = "9. November 1989",
        answerB = "3. Oktober 1990",
        answerC = "1. Januar 1991",
        answerD = "12. September 1990",
        correctAnswer = 1, // B
        explanation = "Die Deutsche Wiedervereinigung fand am 3. Oktober 1990 statt. Der 3. Oktober ist seitdem der Tag der Deutschen Einheit und wird als Nationalfeiertag begangen.",
        funFact = "Deutschland wurde genau 45 Jahre nach dem Ende des Zweiten Weltkriegs wiedervereinigt – ein historisches Datum, das weltweit Beachtung fand."
    ),

    // Question 35 - German History
    Question(
        categoryId = 3,
        questionText = "Wer war der erste Bundeskanzler der Bundesrepublik Deutschland?",
        answerA = "Willy Brandt",
        answerB = "Ludwig Erhard",
        answerC = "Konrad Adenauer",
        answerD = "Helmut Schmidt",
        correctAnswer = 2, // C
        explanation = "Konrad Adenauer war von 1949 bis 1963 der erste Bundeskanzler der BRD. Er führte Deutschland nach dem Krieg in die westliche Gemeinschaft und legte den Grundstein für das Wirtschaftswunder.",
        funFact = "Adenauer war bei seiner Wahl zum Bundeskanzler 73 Jahre alt – er ist damit der älteste Regierungschef, den Deutschland je hatte. Er regierte bis ins hohe Alter von 87 Jahren."
    ),

    // Question 36 - German History
    Question(
        categoryId = 3,
        questionText = "Wie hieß die Unterdrückungsbehörde der DDR, die die Bevölkerung überwachte?",
        answerA = "Gestapo",
        answerB = "Stasi",
        answerC = "SS",
        answerD = "Abwehr",
        correctAnswer = 1, // B
        explanation = "Die Stasi (Ministerium für Staatssicherheit) war der Geheimdienst der DDR. Sie überwachte die Bevölkerung massiv, hatte zeitweise ca. 90.000 hauptamtliche Mitarbeiter und 170.000 inoffizielle Informanten.",
        funFact = "Auf je 63 DDR-Bürger kam ein Stasi-Spitzel – eine der höchsten Überwachungsdichten in der Geschichte. Die Stasi führte Akten zu etwa 6 Millionen Menschen."
    ),

    // Question 37 - Famous Historical Figures
    Question(
        categoryId = 3,
        questionText = "Wer war Kleopatra?",
        answerA = "Eine römische Kaiserin",
        answerB = "Eine griechische Göttin",
        answerC = "Die letzte Pharaonin Ägyptens",
        answerD = "Eine persische Königin",
        correctAnswer = 2, // C
        explanation = "Kleopatra VII. war die letzte Herrscherin des ptolemäischen Ägyptens. Sie war bekannt für ihre Intelligenz und ihre politischen Allianzen mit Julius Caesar und Marcus Antonius.",
        funFact = "Kleopatra war ursprünglich griechischer Abstammung (Ptolemäer-Dynastie) und war die erste ihrer Herrscherfamilie, die überhaupt Ägyptisch sprach – neben mehreren anderen Sprachen."
    ),

    // Question 38 - Famous Historical Figures
    Question(
        categoryId = 3,
        questionText = "Wofür ist Galileo Galilei bekannt?",
        answerA = "Er entdeckte Amerika",
        answerB = "Er erfand den Buchdruck",
        answerC = "Er verbesserte das Teleskop und bestätigte das heliozentrische Weltbild",
        answerD = "Er schrieb die Bibel",
        correctAnswer = 2, // C
        explanation = "Galileo Galilei verbesserte das Teleskop und nutzte es für astronomische Beobachtungen, die das heliozentrische Weltbild (Erde dreht sich um die Sonne) bestätigten. Die Kirche verurteilte ihn dafür.",
        funFact = "Die Legende, Galilei habe nach seinem Widerruf geflüstert 'Und sie bewegt sich doch!' ist vermutlich erfunden – sie taucht erst über 100 Jahre nach seinem Tod in Quellen auf."
    ),

    // Question 39 - Famous Historical Figures
    Question(
        categoryId = 3,
        questionText = "Wer war Martin Luther und wofür ist er bekannt?",
        answerA = "Ein englischer König, der die Kirche von England gründete",
        answerB = "Ein deutscher Mönch, der die Reformation einleitete",
        answerC = "Ein Papst, der die Bibel übersetzte",
        answerD = "Ein Kreuzritter und Erzbischof von Köln",
        correctAnswer = 1, // B
        explanation = "Martin Luther (1483–1546) war ein deutscher Theologe und Augustinermönch, der 1517 seine 95 Thesen veröffentlichte und damit die Reformation der Kirche einleitete. Er übersetzte auch die Bibel ins Deutsche.",
        funFact = "Luthers Übersetzung der Bibel ins Deutsche war wegweisend für die Entstehung einer gemeinsamen deutschen Schriftsprache – er benutzte eine Sprache, die von möglichst vielen Deutschen verstanden werden konnte."
    ),

    // Question 40 - Famous Historical Figures
    Question(
        categoryId = 3,
        questionText = "Wer war Wolfgang Amadeus Mozart?",
        answerA = "Ein österreichischer Maler des Barock",
        answerB = "Ein österreichisches Musikgenie der Klassik",
        answerC = "Ein preußischer Militärkomponist",
        answerD = "Ein venezianischer Opernkomponist",
        correctAnswer = 1, // B
        explanation = "Wolfgang Amadeus Mozart (1756–1791) war ein österreichisches Musikgenie, das bereits im Kindesalter komponierte. Er schrieb über 600 Werke, darunter Symphonien, Opern und Kammermusik.",
        funFact = "Mozart spielte bereits mit 3 Jahren Klavier, komponierte mit 5 Jahren erste Stücke und trat mit 6 Jahren am Kaiserlichen Hof in Wien auf. Sein Vater Leopold reiste mit ihm durch ganz Europa."
    ),

    // Question 41 - Ancient History general
    Question(
        categoryId = 3,
        questionText = "Was ist die Magna Carta und wann wurde sie unterzeichnet?",
        answerA = "Ein Friedensvertrag zwischen England und Frankreich, 1215",
        answerB = "Eine englische Königsurkunde, die Grundrechte garantierte, 1215",
        answerC = "Die erste englische Verfassung, 1688",
        answerD = "Eine päpstliche Bulle, die die Kreuzzüge erklärte, 1096",
        correctAnswer = 1, // B
        explanation = "Die Magna Carta (Große Urkunde der Freiheiten) wurde 1215 von König Johann von England unterzeichnet. Sie begrenzte die königliche Macht und gilt als ein Vorläufer moderner Verfassungen.",
        funFact = "Von der Magna Carta existieren heute noch vier Originalexemplare aus dem Jahr 1215. Sie werden in der British Library und in den Kathedralen von Salisbury und Lincoln aufbewahrt."
    ),

    // Question 42 - Medieval Europe
    Question(
        categoryId = 3,
        questionText = "Was waren die Wikinger?",
        answerA = "Arabische Händler des Mittelalters",
        answerB = "Skandinavische Krieger und Seefahrer",
        answerC = "Britische Piraten der Nordsee",
        answerD = "Germanische Stämme aus dem Osten",
        correctAnswer = 1, // B
        explanation = "Die Wikinger waren skandinavische Seefahrer, Händler und Krieger, die vom 8. bis 11. Jahrhundert Europa erkundeten, handelten und überfielen. Sie reichten von Nordamerika bis nach Konstantinopel.",
        funFact = "Die Wikinger erreichten Nordamerika (Vinland) etwa 500 Jahre vor Kolumbus. Leif Eriksson gründete um 1000 n. Chr. eine Siedlung in Neufundland, Kanada."
    ),

    // Question 43 - Ancient Rome
    Question(
        categoryId = 3,
        questionText = "Wie nannte man die Amphitheater-Kämpfer im alten Rom?",
        answerA = "Legionäre",
        answerB = "Zenturionen",
        answerC = "Gladiatoren",
        answerD = "Prätorianer",
        correctAnswer = 2, // C
        explanation = "Gladiatoren waren Kämpfer, die in römischen Amphitheatern wie dem Kolosseum gegeneinander oder gegen Tiere kämpften. Die Kämpfe dienten der Unterhaltung des Volkes.",
        funFact = "Nicht alle Gladiatoren waren Sklaven. Manche freie Männer wurden freiwillig Gladiatoren, weil es Ruhm und Geld brachte. Erfolgreiche Gladiatoren waren echte Popstars der Antike."
    ),

    // Question 44 - Famous Historical Figures
    Question(
        categoryId = 3,
        questionText = "Wofür ist Abraham Lincoln bekannt?",
        answerA = "Er war der erste Präsident der USA",
        answerB = "Er erklärte die Unabhängigkeit der USA",
        answerC = "Er schaffte die Sklaverei in den USA ab",
        answerD = "Er kaufte Louisiana von Frankreich",
        correctAnswer = 2, // C
        explanation = "Abraham Lincoln (1809–1865) war der 16. Präsident der USA. Er führte das Land durch den Amerikanischen Bürgerkrieg und schaffte 1863 mit der Emanzipationsproklamation die Sklaverei ab.",
        funFact = "Lincoln wurde am 14. April 1865 im Ford's Theatre in Washington D.C. erschossen – wenige Tage nachdem der Bürgerkrieg mit dem Sieg des Nordens geendet hatte."
    ),

    // Question 45 - French Revolution / Napoleon
    Question(
        categoryId = 3,
        questionText = "Was ist die Bastille und warum wurde sie 1789 gestürmt?",
        answerA = "Ein Schloss des Königs – gestürmt wegen Brotnot",
        answerB = "Ein Staatsgefängnis – gestürmt als Symbol der Tyrannei",
        answerC = "Ein Waffenlager – gestürmt von Soldaten",
        answerD = "Eine Kathedrale – gestürmt wegen Kirchenkritik",
        correctAnswer = 1, // B
        explanation = "Die Bastille war ein königliches Staatsgefängnis in Paris, das als Symbol der königlichen Tyrannei galt. Am 14. Juli 1789 stürmte eine Pariser Volksmenge die Festung – der Beginn der Französischen Revolution.",
        funFact = "Bei der Erstürmung der Bastille befanden sich nur 7 Gefangene darin – vier Fälscher, zwei Verrückte und ein Adliger. Die Symbolwirkung war jedoch enorm."
    ),

    // Question 46 - Industrial Revolution
    Question(
        categoryId = 3,
        questionText = "Wer schrieb das Kommunistische Manifest, das während der Industrialisierung entstand?",
        answerA = "Friedrich Engels allein",
        answerB = "Karl Marx allein",
        answerC = "Karl Marx und Friedrich Engels gemeinsam",
        answerD = "Vladimir Lenin und Karl Marx",
        correctAnswer = 2, // C
        explanation = "Karl Marx und Friedrich Engels verfassten das 'Manifest der Kommunistischen Partei' 1848. Es analysierte die Industriegesellschaft und forderte die Arbeiterklasse auf, gegen das Bürgertum zu kämpfen.",
        funFact = "Das Kommunistische Manifest beginnt mit dem berühmten Satz: 'Ein Gespenst geht um in Europa – das Gespenst des Kommunismus.' Es ist eine der meistverbreiteten politischen Schriften der Geschichte."
    ),

    // Question 47 - World War Basics
    Question(
        categoryId = 3,
        questionText = "Wie hieß der Angriff Deutschlands auf die Sowjetunion im Zweiten Weltkrieg?",
        answerA = "Operation Overlord",
        answerB = "Operation Barbarossa",
        answerC = "Operation Sea Lion",
        answerD = "Operation Dynamo",
        correctAnswer = 1, // B
        explanation = "Operation Barbarossa war der Deckname für den deutschen Angriff auf die Sowjetunion, der am 22. Juni 1941 begann. Es war die größte Militäroperation der Geschichte und kostete Millionen von Menschenleben.",
        funFact = "Hitler ließ die Operation nach Kaiser Friedrich Barbarossa benennen – einem mittelalterlichen deutschen Kaiser. Es war ein fataler Fehler: Die UdSSR mobilisierte gewaltigen Widerstand."
    ),

    // Question 48 - German History
    Question(
        categoryId = 3,
        questionText = "Was war der Reichstag in der deutschen Geschichte?",
        answerA = "Ein kaiserliches Schloss in Berlin",
        answerB = "Das Parlament des Deutschen Reiches",
        answerC = "Ein Militärhauptquartier",
        answerD = "Ein Gerichtshof des Kaisers",
        correctAnswer = 1, // B
        explanation = "Der Reichstag war das Parlament des Deutschen Reiches. Das Gebäude in Berlin, in dem der Reichstag tagte, wurde 1933 durch Brand beschädigt. Heute tagt dort der Deutsche Bundestag.",
        funFact = "1995 ließen die Künstler Christo und Jeanne-Claude den Reichstag mit silbernem Stoff verhüllen – über 5 Millionen Besucher kamen, um das spektakuläre Kunstwerk zu sehen."
    ),

    // Question 49 - Cold War Basics
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr löste sich die Sowjetunion auf?",
        answerA = "1989",
        answerB = "1990",
        answerC = "1991",
        answerD = "1993",
        correctAnswer = 2, // C
        explanation = "Die Sowjetunion löste sich am 25. Dezember 1991 offiziell auf. Michail Gorbatschow trat zurück und 15 unabhängige Staaten entstanden, darunter Russland, Ukraine und die baltischen Staaten.",
        funFact = "Am Tag der Auflösung der Sowjetunion wurde über dem Kreml die russische Trikolore gehisst und die rote Sowjetflagge eingeholt. Es war das Ende des Kalten Krieges."
    ),

    // Question 50 - Famous Historical Figures
    Question(
        categoryId = 3,
        questionText = "Wer war Nelson Mandela?",
        answerA = "Ein kenianischer Staatspräsident und Revolutionär",
        answerB = "Ein südafrikanischer Politiker, der gegen Apartheid kämpfte",
        answerC = "Ein nigerianischer Schriftsteller und Friedensnobelpreisträger",
        answerD = "Ein ghanaischer Anführer der Unabhängigkeitsbewegung",
        correctAnswer = 1, // B
        explanation = "Nelson Mandela (1918–2013) war ein südafrikanischer Freiheitskämpfer, der 27 Jahre im Gefängnis saß. Nach seiner Freilassung wurde er 1994 zum ersten demokratisch gewählten Präsidenten Südafrikas.",
        funFact = "Mandela erhielt 1993 den Friedensnobelpreis, zusammen mit dem damaligen Präsidenten Frederik de Klerk. Er ist einer der wenigen Menschen, die gleichzeitig von Freunden und ehemaligen Feinden verehrt werden."
    ),

)
